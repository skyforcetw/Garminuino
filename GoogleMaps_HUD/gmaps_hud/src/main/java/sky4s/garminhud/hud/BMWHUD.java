package sky4s.garminhud.hud;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import sky4s.garminhud.app.MainActivity;
import sky4s.garminhud.app.R;
import sky4s.garminhud.eLane;
import sky4s.garminhud.eOutAngle;
import sky4s.garminhud.eOutType;
import sky4s.garminhud.eUnits;

public class BMWHUD extends HUDAdapter {
    private static final String TAG = BMWHUD.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final int MAX_UPDATES_PER_SECOND = 6;

    private static final int BMW_HUD_ACTION_WIFI_RESULT = 31337;

    private Context mContext;
    private BMWMessage mMsg;
    private BMWSocketConnection mSocket;
    private long mLastUpdateClearTime = 0;
    private int mUpdateCount = 0;
    private ExecutorService mExecutor;
    private FutureTask<Boolean> mSendTask;

    public BMWHUD(Context context) {
        if (DEBUG) Log.d(TAG, "Creating BMWHUD instance");
        mContext = context;
        mExecutor = Executors.newFixedThreadPool(1);
        mMsg = new BMWMessage();
        mSocket = BMWSocketConnection.getInstance(mContext);

        WifiManager wifiManager =
                (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLED) {
            Toast.makeText(mContext.getApplicationContext(),
                    mContext.getString(R.string.message_enable_wlan),
                    Toast.LENGTH_SHORT).show();
            // Bring up WLAN panel if not connected
            scanForHud();
        }
    }

    @Override
    public void registerConnectionCallback(ConnectionCallback callback) {
        mSocket.registerConnectionCallback(callback);
    }

    @Override
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        return requestCode == BMW_HUD_ACTION_WIFI_RESULT;
    }

    @Override
    public void scanForHud() {
        // Called when scan button is pressed, bring up WLAN panel
        Toast.makeText(mContext.getApplicationContext(),
                mContext.getString(R.string.message_connect_to_bmw_hud_network),
                Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Settings.Panel.ACTION_WIFI);
        startActivityForResult(intent, BMW_HUD_ACTION_WIFI_RESULT);
    }

    @Override
    public boolean isUpdatable() {
        final long now = System.currentTimeMillis();
        final long interval = now - mLastUpdateClearTime;
        if (interval > TimeUnit.SECONDS.toMillis(1)) {
            mLastUpdateClearTime = now;
            mUpdateCount = 0;
        }
        final boolean isSending = mSendTask != null && !mSendTask.isDone();
        if (DEBUG) Log.d(TAG, "isSending: " + isSending);
        final boolean updatable = mUpdateCount < MAX_UPDATES_PER_SECOND && !isSending;
        if (DEBUG) Log.d(TAG, "isUpdatable: " + updatable);
        return updatable;
    }

    @Override
    public boolean getSendResult() {
        if (!isUpdatable()) {
            return false;
        }

        try {
            boolean sendResult = mSendTask.get();
            if (DEBUG) Log.d(TAG, "getSendResult: result: " + sendResult);
            return sendResult;
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "getSendResult: ", e);
            return false;
        }
    }

    @Override
    public void setTime(int nH, int nM, boolean bFlag, boolean bTraffic, boolean bColon, boolean bH) {
        // function is called through SetCurrentTime, and only used for showing current time when idle
        // nH is expected to be 24-hour
        final boolean isAm = nH < 12;
        nH %= 12;
        nH = (nH == 0) ? 12 : nH;
        mMsg.setArrivalTime(nH, nM, isAm ? BMWMessage.TIME_SUFFIX_AM : BMWMessage.TIME_SUFFIX_PM);

        sendMessage();
    }

    @Override
    public void setRemainTime(int nH, int nM, boolean bTraffic) {
        // nH is expected to be 24-hour
        final boolean isAm = nH < 12;
        int suffix = 0;
        if (!isShowETAEnabled()) {
            suffix = BMWMessage.TIME_SUFFIX_HOURS;
        } else {
            nH %= 12;
            nH = (nH == 0) ? 12 : nH;
            suffix = isAm ? BMWMessage.TIME_SUFFIX_AM : BMWMessage.TIME_SUFFIX_PM;
        }
        if (DEBUG) Log.d(TAG, "isShowETAEnabled: " + isShowETAEnabled());
        mMsg.setArrivalTime(nH, nM, suffix);

        // enable separate indicator for traffic delay even though minutes delay isn't available
        mMsg.setTrafficDelay(bTraffic ? 1 : 0);

        sendMessage();
    }

    @Override
    public void clearTime() {
        mMsg.setArrivalTime(0, 0, BMWMessage.TIME_SUFFIX_AM);

        sendMessage();
    }

    @Override
    public void setDistance(float nDist, eUnits unit) {
        if (DEBUG) Log.d(TAG, "SetDistance: nDist: " + nDist +
                ", unit: " + unit);
        double distToTurnMiles;
        switch (unit) {
            case Foot:
                distToTurnMiles = nDist / 5280.0;
                break;
            case Metres:
                distToTurnMiles = nDist / 1609.344;
                break;
            case Kilometres:
                distToTurnMiles = nDist / 1.609344;
                break;
            case Miles:
                distToTurnMiles = nDist;
                break;
            default:
                // invalid input
                return;
        }
        // TODO: handle metric
        mMsg.setDistanceToTurn(distToTurnMiles);

        sendMessage();
    }

    @Override
    public void clearDistance() {
        mMsg.setDistanceToTurn(0);

        sendMessage();
    }

    @Override
    public void setRemainingDistance(float nDist, eUnits unit) {
        if (DEBUG) Log.d(TAG, "SetRemainingDistance: nDist: " + nDist +
                ", unit: " + unit);
        double distToTurnMiles;
        switch (unit) {
            case Foot:
                distToTurnMiles = nDist / 5280.0;
                break;
            case Metres:
                distToTurnMiles = nDist / 1609.344;
                break;
            case Kilometres:
                distToTurnMiles = nDist / 1.609344;
                break;
            case Miles:
                distToTurnMiles = nDist;
                break;
            default:
                // invalid input
                return;
        }
        // TODO: handle metric
        mMsg.setRemainingDistance(distToTurnMiles);

        sendMessage();
    }

    @Override
    public void clearRemainingDistance() {
        mMsg.setRemainingDistance(0);

        sendMessage();
    }

    @Override
    public void setAlphabet(char a, char b, char c, char d) {
        // not supported
        if (DEBUG) Log.w(TAG, "SetAlphabet: Not supported");
    }

    @Override
    public void setDirection(final eOutAngle nDir, final eOutType nType, final eOutAngle nRoundaboutOut) {
        if (nType == eOutType.LeftRoundabout) {
            switch (nRoundaboutOut) {
                case Down:
                case LeftDown:
                case RightDown:
                    // treat all U-turn types as a roundabout U turn
                    mMsg.setArrow(BMWMessage.ARROW_ROUNDABOUT_LEFT_360);
                    break;
                case SharpRight:
                    mMsg.setArrow(BMWMessage.ARROW_ROUNDABOUT_LEFT_315);
                    break;
                case Right:
                    mMsg.setArrow(BMWMessage.ARROW_ROUNDABOUT_LEFT_270);
                    break;
                case EasyRight:
                    mMsg.setArrow(BMWMessage.ARROW_ROUNDABOUT_LEFT_225);
                    break;
                case Straight:
                    mMsg.setArrow(BMWMessage.ARROW_ROUNDABOUT_LEFT_180);
                    break;
                case EasyLeft:
                    mMsg.setArrow(BMWMessage.ARROW_ROUNDABOUT_LEFT_135);
                    break;
                case Left:
                    mMsg.setArrow(BMWMessage.ARROW_ROUNDABOUT_LEFT_90);
                    break;
                case SharpLeft:
                    mMsg.setArrow(BMWMessage.ARROW_ROUNDABOUT_LEFT_45);
                    break;
                default:
                    Log.e(TAG, "SetDirection: Unhandled left roundabout direction: " + nRoundaboutOut);
                    break;
            }
        } else if (nType == eOutType.RightRoundabout) {
            switch (nRoundaboutOut) {
                case Down:
                case LeftDown:
                case RightDown:
                    // treat all U-turn types as a roundabout U turn
                    mMsg.setArrow(BMWMessage.ARROW_ROUNDABOUT_RIGHT_360);
                    break;
                case SharpRight:
                    mMsg.setArrow(BMWMessage.ARROW_ROUNDABOUT_RIGHT_45);
                    break;
                case Right:
                    mMsg.setArrow(BMWMessage.ARROW_ROUNDABOUT_RIGHT_90);
                    break;
                case EasyRight:
                    mMsg.setArrow(BMWMessage.ARROW_ROUNDABOUT_RIGHT_135);
                    break;
                case Straight:
                    mMsg.setArrow(BMWMessage.ARROW_ROUNDABOUT_RIGHT_180);
                    break;
                case EasyLeft:
                    mMsg.setArrow(BMWMessage.ARROW_ROUNDABOUT_RIGHT_225);
                    break;
                case Left:
                    mMsg.setArrow(BMWMessage.ARROW_ROUNDABOUT_RIGHT_270);
                    break;
                case SharpLeft:
                    mMsg.setArrow(BMWMessage.ARROW_ROUNDABOUT_RIGHT_315);
                    break;
                default:
                    Log.e(TAG, "SetDirection: Unhandled right roundabout direction: " + nRoundaboutOut);
                    break;
            }
        } else {
            // treat Lane, LongerLane, RightFlag the same
            switch (nDir) {
                case SharpRight:
                    mMsg.setArrow(BMWMessage.ARROW_RIGHT_45);
                    break;
                case Right:
                    mMsg.setArrow(BMWMessage.ARROW_RIGHT_90);
                    break;
                case EasyRight:
                    mMsg.setArrow(BMWMessage.ARROW_RIGHT_135);
                    break;
                case Straight:
                    mMsg.setArrow(BMWMessage.ARROW_180);
                    break;
                case EasyLeft:
                    mMsg.setArrow(BMWMessage.ARROW_LEFT_135);
                    break;
                case Left:
                    mMsg.setArrow(BMWMessage.ARROW_LEFT_90);
                    break;
                case SharpLeft:
                    mMsg.setArrow(BMWMessage.ARROW_LEFT_45);
                    break;
                case LeftDown:
                    mMsg.setArrow(BMWMessage.ARROW_LEFT_0);
                    break;
                case RightDown:
                    mMsg.setArrow(BMWMessage.ARROW_RIGHT_0);
                    break;
                default:
                    // Assume this is convergence
                    mMsg.setArrow(BMWMessage.ARROW_180);
                    Log.w(TAG, "SetDirection: Unhandled direction: " +
                            nDir + ", assuming convergence arrow");
                    break;
            }
        }

        sendMessage();
    }

    @Override
    public void setLanes(char nArrow, char nOutline) {
        if (nArrow == 0 && nOutline == 0) {
            // disable lane indicator if zeroes are set
            mMsg.setLaneCount(0);
            for (int i = 0; i < BMWMessage.MAX_LANES; i++) {
                mMsg.setLaneIndicator(i, false);
            }
        } else {
            int laneCount = 0;
            for (int i = 0; i < BMWMessage.MAX_LANES; i++) {
                // count the number of eLane enum bits set in nOutline to determine lane count
                if ((nOutline & (eLane.OuterRight.value << i)) != 0) {
                    laneCount++;
                }
                // parse eLane enum from nArrow in order to set indicator index
                if ((nArrow & (eLane.OuterRight.value << i)) != 0) {
                    mMsg.setLaneIndicator(i, true);
                }
            }
            mMsg.setLaneCount(laneCount);
        }
        sendMessage();
    }

    @Override
    public void setSpeed(int nSpeed, boolean bIcon) {
        // not supported
        if (DEBUG) Log.w(TAG, "SetSpeed: Not supported");
    }

    @Override
    public void setSpeedWarning(int nSpeed, int nLimit, boolean bSpeeding, boolean bIcon, boolean bSlash) {
        // TODO: handle isMetric parameter
        mMsg.setSpeedLimit(nLimit, false);

        sendMessage();
    }

    @Override
    public void clearSpeedAndWarning() {
        // TODO: handle isMetric parameter
        mMsg.setSpeedLimit(0, false);

        sendMessage();
    }

    @Override
    public void setCameraIcon(boolean visible) {
        mMsg.setSpeedCameraEnabled(visible);

        sendMessage();
    }

    @Override
    public void setGpsLabel(boolean visible) {
        // not supported
        if (DEBUG) Log.w(TAG, "SetGpsLabel: Not supported");
    }

    @Override
    public void setAutoBrightness() {
        // TODO: decode brightness control
        if (DEBUG) Log.w(TAG, "SetAutoBrightness: Not implemented");
    }

    @Override
    public void setBrightness(int brightness) {
        // TODO: decode brightness control
        if (DEBUG) Log.w(TAG, "SetBrightness: Not implemented");
    }

    @Override
    public void disconnect() {
        mSocket.disconnect();
    }

    private boolean isShowETAEnabled() {
        // Preferences are stored using Activity.getPreferences
        // which uses the classname as the preferences name
        SharedPreferences sharedPref = mContext.getSharedPreferences(
                MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
        return sharedPref.getBoolean(mContext.getString(R.string.option_show_eta), false);
    }

    private void startActivityForResult(Intent intent, int requestCode) {
        ((MainActivity) mContext).startActivityForResult(intent, requestCode);
    }

    private void sendMessage() {
        if (!isUpdatable()) {
            return;
        }
        mUpdateCount++;

        mSendTask = new FutureTask<>(() -> {
            boolean ret = mSocket.send(mMsg.getBytes());
            if (DEBUG) Log.d(TAG, "sendMessage: Sent message to HUD");
            return ret;
        });
        mExecutor.execute(mSendTask);
    }
}