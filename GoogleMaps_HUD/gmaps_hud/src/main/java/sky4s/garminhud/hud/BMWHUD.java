package sky4s.garminhud.hud;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

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

    private static final int DEFAULT_MAX_UPDATES = 6;

    private Context mContext;
    private BMWMessage mMsg;
    private BMWSocketConnection mSocket;
    private int mMaxUpdatesPerSecond = DEFAULT_MAX_UPDATES;
    private long mLastUpdateClearTime = 0;
    private int mUpdateCount = 0;
    private ExecutorService mExecutor;
    private FutureTask<Boolean> mSendTask;

    public BMWHUD(Context context) {
        if (DEBUG) Log.d(TAG, "Creating BMWHUD instance");
        mContext = context;
        mExecutor = Executors.newFixedThreadPool(1);
        mMsg = new BMWMessage();
        mSocket = new BMWSocketConnection(mContext);
    }

    @Override
    public void setMaxUpdatePerSecond(int max) {
        mMaxUpdatesPerSecond = max;
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
        final boolean updatable = mUpdateCount < mMaxUpdatesPerSecond && !isSending;
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
    public void SetTime(int nH, int nM, boolean bFlag, boolean bTraffic, boolean bColon, boolean bH) {
        // function is called through SetCurrentTime, and only used for showing current time when idle
        // nH is expected to be 24-hour
        mMsg.setArrivalTime(nH % 12, nM, nH < 12 ? BMWMessage.TIME_SUFFIX_AM : BMWMessage.TIME_SUFFIX_PM);

        sendMessage();
    }

    @Override
    public void SetRemainTime(int nH, int nM, boolean bTraffic) {
        // nH is expected to be 24-hour
        int suffix = 0;
        if (!isShowETAEnabled()) {
            suffix = BMWMessage.TIME_SUFFIX_HOURS;
        } else {
            suffix = nH < 12 ? BMWMessage.TIME_SUFFIX_AM : BMWMessage.TIME_SUFFIX_PM;
        }
        if (DEBUG) Log.d(TAG, "isShowETAEnabled: " + isShowETAEnabled());
        mMsg.setArrivalTime(nH % 12, nM, suffix);

        // enable separate indicator for traffic delay even though minutes delay isn't available
        mMsg.setTrafficDelay(bTraffic ? 1 : 0);

        sendMessage();
    }

    @Override
    public void ClearTime() {
        mMsg.setArrivalTime(0, 0, BMWMessage.TIME_SUFFIX_AM);

        sendMessage();
    }

    @Override
    public void SetDistance(int nDist, eUnits unit, boolean bDecimal, boolean bLeadingZero) {
        if (DEBUG) Log.d(TAG, "SetDistance: nDist: " + nDist +
                ", unit: " + unit +
                ", bDecimal: " + bDecimal +
                ", bLeadingZero: " + bLeadingZero);
        double distToTurnMiles;
        // nDist will only ever allow 1 digit after decimal, divide by 10 if bDecimal is set
        double divisor = bDecimal ? 10.0 : 1.0;
        switch (unit) {
            case Foot:
                distToTurnMiles = nDist / divisor / 5280.0;
                break;
            case Metres:
                distToTurnMiles = nDist / divisor / 1609.344;
                break;
            case Kilometres:
                distToTurnMiles = nDist / divisor / 1.609344;
                break;
            case Miles:
                distToTurnMiles = nDist / divisor;
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
    public void ClearDistance() {
        mMsg.setDistanceToTurn(0);

        sendMessage();
    }

    @Override
    public void SetAlphabet(char a, char b, char c, char d) {
        // not supported
        if (DEBUG) Log.w(TAG, "SetAlphabet: Not supported");
    }

    @Override
    public void SetDirection(final eOutAngle nDir, final eOutType nType, final eOutAngle nRoundaboutOut) {
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
                    Log.e(TAG, "SetDirection: Unhandled direction: " + nDir);
                    return;
            }
        }

        sendMessage();
    }

    @Override
    public void SetLanes(char nArrow, char nOutline) {
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
    public void SetSpeed(int nSpeed, boolean bIcon) {
        // not supported
        if (DEBUG) Log.w(TAG, "SetSpeed: Not supported");
    }

    @Override
    public void SetSpeedWarning(int nSpeed, int nLimit, boolean bSpeeding, boolean bIcon, boolean bSlash) {
        // TODO: handle isMetric parameter
        mMsg.setSpeedLimit(nLimit, false);

        sendMessage();
    }

    @Override
    public void ClearSpeedandWarning() {
        // TODO: handle isMetric parameter
        mMsg.setSpeedLimit(0, false);

        sendMessage();
    }

    @Override
    public void SetCameraIcon(boolean visible) {
        mMsg.setSpeedCameraEnabled(visible);

        sendMessage();
    }

    @Override
    public void SetGpsLabel(boolean visible) {
        // not supported
        if (DEBUG) Log.w(TAG, "SetGpsLabel: Not supported");
    }

    @Override
    public void SetAutoBrightness() {
        // TODO: decode brightness control
        if (DEBUG) Log.w(TAG, "SetAutoBrightness: Not implemented");
    }

    @Override
    public void SetBrightness(int brightness) {
        // TODO: decode brightness control
        if (DEBUG) Log.w(TAG, "SetBrightness: Not implemented");
    }

    private boolean isShowETAEnabled() {
        // Preferences are stored using Activity.getPreferences
        // which uses the classname as the preferences name
        SharedPreferences sharedPref = mContext.getSharedPreferences(
                MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
        return sharedPref.getBoolean(mContext.getString(R.string.option_show_eta), false);
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