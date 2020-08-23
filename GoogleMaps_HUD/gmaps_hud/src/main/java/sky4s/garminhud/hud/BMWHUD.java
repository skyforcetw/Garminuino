package sky4s.garminhud.hud;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import sky4s.garminhud.eLane;
import sky4s.garminhud.eOutAngle;
import sky4s.garminhud.eOutType;
import sky4s.garminhud.eUnits;

public class BMWHUD extends HUDAdapter {

    private static final byte[] HUD_ADDRESS = new byte[]{(byte)192, (byte)168, 0, 10};
    private static final int HUD_PORT = 50007;

    private BMWMessage mMsg;
    private Socket mSocket;
    private boolean mSendResult = false;
    private int mMaxUpdatesPerSecond = 0;
    private long mLastUpdateClearTime = 0;
    private int mUpdateCount = 0;

    public BMWHUD() {
        mMsg = new BMWMessage();

        // TODO: set up socket connection
        try {
            InetAddress hudAddress = InetAddress.getByAddress(HUD_ADDRESS);
            mSocket = new Socket(hudAddress, HUD_PORT);
        } catch (IOException e) {
            // TODO: handle errors
        }
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
        final boolean updatable = mUpdateCount < mMaxUpdatesPerSecond;
        if (!updatable) {
            mSendResult = false;
        }
        return updatable;
    }

    @Override
    public boolean getSendResult() {
        return mSendResult;
    }

    @Override
    public void SetTime(int nH, int nM, boolean bFlag, boolean bTraffic, boolean bColon, boolean bH) {
        // function is called through SetCurrentTime, and only used for showing current time when idle
        // nH is expected to be 24-hour
        mMsg.setArrivalTime(nH, nM, nH < 12 ? BMWMessage.TIME_SUFFIX_AM : BMWMessage.TIME_SUFFIX_PM);

        sendMessage();
    }

    @Override
    public void SetRemainTime(int nH, int nM, boolean bTraffic) {
        // nH is expected to be 24-hour
        // TODO: read option_show_eta to determine whether to postfix AM/PM or H
        int suffix = nH < 12 ? BMWMessage.TIME_SUFFIX_AM : BMWMessage.TIME_SUFFIX_PM;
        mMsg.setArrivalTime(nH, nM, suffix);

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
        // ignore bDecimal and bLeadingZero, BMW HUD does not handle floating point distances
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
    public void ClearDistance() {
        mMsg.setDistanceToTurn(0);

        sendMessage();
    }

    @Override
    public void SetAlphabet(char a, char b, char c, char d) {
        // not supported
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
                    // TODO: Unknown input
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
                    // TODO: Unknown input
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
                    // TODO: Unknown input
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
    }

    @Override
    public void SetAutoBrightness() {
        // TODO: decode brightness control
    }

    @Override
    public void SetBrightness(int brightness) {
        // TODO: decode brightness control
    }

    private void sendMessage() {
        if (!isUpdatable()) {
            return;
        }
        mUpdateCount++;
        try {
            byte[] resp = new byte[1024];
            OutputStream outStream = mSocket.getOutputStream();
            InputStream inStream = mSocket.getInputStream();
            outStream.write(mMsg.getBytes());
            inStream.read(resp);
            // TODO: check resp for server ACK
            mSendResult = true;
        } catch (IOException e) {
            // TODO: handle errors
        }
    }
}