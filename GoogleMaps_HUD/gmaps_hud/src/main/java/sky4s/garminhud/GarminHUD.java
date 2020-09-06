package sky4s.garminhud;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import sky4s.garminhud.app.MainActivity;
import sky4s.garminhud.hud.HUDAdapter;

/**
 * Created by skyforce on 2018/8/13.
 */

public class GarminHUD extends HUDAdapter {
    //===========================================================================================
    // 不與C++共用的部分
    //===========================================================================================
    private static final int MAX_UPDATES_PER_SECOND = 6;
    private int mUpdateCount = 0;
    private long mLastUpdateClearTime = System.currentTimeMillis();
    private BluetoothSPP mBt;
    private boolean mSendResult = false;
    //===========================================================================================

    public GarminHUD(BluetoothSPP bt) {
        mBt = bt;
    }

    @Override
    public void registerConnectionCallback(ConnectionCallback callback) {
        // TODO: Move BluetoothSPP into this class and push connection state through callback
    }

    @Override
    public boolean isUpdatable() {
        final long now = System.currentTimeMillis();
        final long interval = now - mLastUpdateClearTime;
        if (interval > 1000) {
            mLastUpdateClearTime = now;
            mUpdateCount = 0;
        }

        final boolean updatable = mUpdateCount < MAX_UPDATES_PER_SECOND;
        if (!updatable) {
            mSendResult = false;
        }
        return updatable;
    }

    private char toDigit(int n) {
        n = n % 10;
        if (n == 0)
            return (char) 10;
        else
            return (char) n;
    }

    private boolean sendPacket(char[] pBuf, int length) {
        if (!isUpdatable() || null == mBt) {
            return false;
        }
        mUpdateCount++;
        if (MainActivity.IGNORE_BT_DEVICE) {
            return true;
        }
        byte[] packet = new byte[length];
        for (int x = 0; x < length; x++) {
            packet[x] = (byte) pBuf[x];
        }

        if (mBt.isServiceAvailable()) {
            //judge service exist to avoid => app.akexorcist.bluetotohspp.library.BluetoothService.getState()' on a null object reference
            mBt.send(packet, false);
        }
        return true;
    }

    @Override
    public boolean getSendResult() {
        return mSendResult;
    }

    private void sendToHud(char[] pBuf) {
        int nLen = pBuf.length;

        char[] sendBuf = new char[255];
        char len = 0;
        int stuffingCount = 0;

        sendBuf[len++] = 0x10;
        sendBuf[len++] = 0x7b;
        sendBuf[len++] = (char) (nLen + 6);
        if (nLen == 0xa) {
            sendBuf[len++] = 0x10;
            stuffingCount++;
        }
        sendBuf[len++] = (char) nLen;
        sendBuf[len++] = 0x00;
        sendBuf[len++] = 0x00;
        sendBuf[len++] = 0x00;
        sendBuf[len++] = 0x55;
        sendBuf[len++] = 0x15;

        for (char c : pBuf) {
            sendBuf[len++] = c;
            if (c == 0x10) {
                //Escape LF
                sendBuf[len++] = 0x10;
                stuffingCount++;
            }
        }

        int nCrc = 0;
        for (int i = 1; i < len; i++) {
            nCrc += sendBuf[i];
        }
        nCrc -= stuffingCount * 0x10;

        sendBuf[len++] = (char) ((-nCrc) & 0xff);
        sendBuf[len++] = 0x10;
        sendBuf[len++] = 0x03;

        mSendResult = sendPacket(sendBuf, len);
    }

    @Override
    public void setTime(int nH, int nM, boolean bFlag, boolean bTraffic, boolean bColon, boolean bH) {
        char[] arr = {(char) 0x05,
                bTraffic ? (char) 0xff : (char) 0x00,
                toDigit(nH / 10), toDigit(nH), // hour
                bColon ? (char) 0xff : (char) 0x00, // :
                toDigit(nM / 10), toDigit(nM), //minute
                bH ? (char) 0xff : (char) 0x00, // post-fix 'h'
                bFlag ? (char) 0xff : (char) 0x00};
        sendToHud(arr);
    }

    @Override
    public void setRemainTime(int nH, int nM, boolean bTraffic) {
        final boolean bH = false;
        final boolean bFlag = true;

        boolean noHour = 0 == nH;
        boolean minLessThen10 = noHour && nM < 10;
        char[] arr = {(char) 0x05,
                bTraffic ? (char) 0xff : (char) 0x00,
                noHour ? (char) 0 : toDigit(nH / 10),// hour n_
                noHour ? (char) 0 : toDigit(nH), // hour _n
                noHour ? (char) 0 : (char) 0xff, // :
                minLessThen10 ? (char) 0 : toDigit(nM / 10), //minute n_
                toDigit(nM), //minute _n
                bH ? (char) 0xff : (char) 0x00, // post-fix 'h'
                bFlag ? (char) 0xff : (char) 0x00};
        sendToHud(arr);
    }

    @Override
    public void clearTime() {
        char[] arr = {(char) 0x05,
                0x00,
                0, 0,
                0x00,
                0, 0,
                0x00,
        };
        sendToHud(arr);
    }

    @Override
    public void setDistance(float nDist, eUnits unit) {
        int distance = (int) nDist;
        boolean hasDecimal = ((eUnits.Kilometres == unit) || (eUnits.Miles == unit)) && nDist < 10;
        if (hasDecimal) {
            distance *= 10.0;
        }
        char[] arr = {(char) 0x03,
                toDigit(distance / 1000), toDigit(distance / 100), toDigit(distance / 10),
                hasDecimal ? (char) 0xff : (char) 0x00, toDigit(distance), (char) unit.value};

        if (arr[1] == 0xa) {
            arr[1] = 0;
            if (arr[2] == 0xa) {
                arr[2] = 0;
                if (arr[3] == 0xa) {
                    arr[3] = 0;
                }
            }
        }
        if (hasDecimal && ((int) nDist / 10) == 0) {
            // Show leding zero for decimals
            arr[3] = 0xa;
        }

        sendToHud(arr);
    }

    @Override
    public void clearDistance() {
        char[] arr = {(char) 0x03, 0x0, 0x0, 0x0, 0x00, 0, 0};
        sendToHud(arr);
    }

    @Override
    public void setRemainingDistance(float nDist, eUnits unit) {
        // not supported
    }

    @Override
    public void clearRemainingDistance() {
        // not supported
    }

    @Override
    public void setAlphabet(char a, char b, char c, char d) {
        eUnits unit = eUnits.None;
        final boolean bDecimal = false;
        final boolean bLeadingZero = false;

        char[] arr = {(char) 0x03, a, b, c,
                bDecimal ? (char) 0xff : (char) 0x00, d, (char) unit.value};
        if (!bLeadingZero) {
            if (arr[1] == 0xa) {
                arr[1] = 0;
                if (arr[2] == 0xa) {
                    arr[2] = 0;
                    if (arr[3] == 0xa) {
                        arr[3] = 0;
                    }
                }
            }
        }
        sendToHud(arr);
    }

    /*
    eOutType:
    Off(0x00),
    Lane(0x01),
    LongerLane(0x02),
    LeftRoundabout(0x04),
    RightRoundabout(0x08),
    ArrowOnly(0x80);

    eOutAngle:
    SharpRight(0x02),
    Right(0x04),
    EasyRight(0x08),
    Straight(0x10),
    EasyLeft(0x20),
    Left(0x40),
    SharpLeft(0x80),
    LeftDown(0x81),
    RightDown(0x82),
    AsDirection(0x00);
     */

    /*
    byte0:  header 0x01

    byte1:  Line 箭頭長度, eOutAngle
            0x00 Off
            0x01 Lane
            0x02 LongerLane
            0x04 LeftRoundabout
            0x08 RightRoundabout
            0x10 LeftDown*
            0x20 RightDown*
            0x40 RightFlag
            0x80 ArrowOnly

    byte2:  When Roundabout, eOutAngle:nRoundaboutOut or eOutType:nType

    byte3:  When not LeftDown/RightDown, 箭頭方向: eOutAngle
    */

    /**
     * @param nDir           箭頭
     * @param nType          圓環方向
     * @param nRoundaboutOut 圓環out
     */
    @Override
    public void setDirection(final eOutAngle nDir, final eOutType nType, final eOutAngle nRoundaboutOut) {
        char[] arr = {(char) 0x01,
                (nDir == eOutAngle.LeftDown) ? (char) 0x10 : ((nDir == eOutAngle.RightDown) ? (char) 0x20 : (char) nType.value),
                (nType == eOutType.RightRoundabout || nType == eOutType.LeftRoundabout) ?
                        ((char) ((nRoundaboutOut == eOutAngle.AsDirection) ? nDir.value : nRoundaboutOut.value)) : (char) 0x00,
                (nDir == eOutAngle.LeftDown || nDir == eOutAngle.RightDown) ? (char) 0x00 : (char) nDir.value};
        sendToHud(arr);
    }

    @Override
    public void setLanes(char nArrow, char nOutline) {
        char[] arr = {0x02, nOutline, nArrow};
        sendToHud(arr);
    }

    @Override
    public void setSpeed(int nSpeed, boolean bIcon) {
        final boolean bSlash = false;
        final boolean bSpeeding = false;

        char hundredsDigit, tensDigit, onesDigit;
        if (nSpeed < 10) {
            // Delete leading zeros
            hundredsDigit = (char) 0x00;
            tensDigit = (char) 0x00;
        } else {
            hundredsDigit = (char) ((nSpeed / 100) % 10);
            tensDigit = toDigit(nSpeed / 10);
        }
        onesDigit = toDigit(nSpeed);

        char[] arr = {(char) 0x06,
                (char) 0x00, (char) 0x00, (char) 0x00, bSlash ? (char) 0xff : (char) 0x00,
                hundredsDigit, tensDigit, onesDigit, bSpeeding ? (char) 0xff : (char) 0x00,
                bIcon ? (char) 0xff : (char) 0x00};

        sendToHud(arr);
    }

    @Override
    public void setSpeedWarning(int nSpeed, int nLimit, boolean bSpeeding, boolean bIcon, boolean bSlash) {
        char[] arr = {(char) 0x06,
                (char) ((nSpeed / 100) % 10), toDigit(nSpeed / 10), toDigit(nSpeed), bSlash ? (char) 0xff : (char) 0x00,
                (char) ((nLimit / 100) % 10), toDigit(nLimit / 10), toDigit(nLimit), bSpeeding ? (char) 0xff : (char) 0x00,
                bIcon ? (char) 0xff : (char) 0x00};

        sendToHud(arr);
    }

    @Override
    public void clearSpeedAndWarning() {
        char[] arr = {(char) 0x06,
                (char) 0x00, (char) 0x00, (char) 0x00, (char) 0x00,
                (char) 0x00, (char) 0x00, (char) 0x00, (char) 0x00,
                (char) 0x00};
        sendToHud(arr);
    }

    @Override
    public void setCameraIcon(boolean visible) {
        char[] arr = {0x04, (char) (visible ? 1 : 0)};
        sendToHud(arr);
    }

    @Override
    public void setGpsLabel(boolean visible) {
        char[] arr = {0x07, (char) (visible ? 1 : 0)};
        sendToHud(arr);
    }

    @Override
    public void setAutoBrightness() {
        char[] autoBrightnessCommand = {0x10, 0x7B, 0x0E, 0x08, 0x00, 0x00, 0x00, 0x56, 0x15, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x10, 0x03};
        mSendResult = sendPacket(autoBrightnessCommand, autoBrightnessCommand.length);
    }

    @Override
    public void setBrightness(int brightness) {
        char[] sendBuf = new char[8];
        int len = 0;
        int stuffing_count = 0;

        sendBuf[len++] = 0x10;
        sendBuf[len++] = 0x0f;
        sendBuf[len++] = 0x02;
        sendBuf[len++] = (char) brightness;
        sendBuf[len++] = 0x00;

        int nCrc = 0;
        for (int i = 1; i < len; i++) {
            nCrc += sendBuf[i];
        }
        nCrc -= stuffing_count * 0x10;

        sendBuf[len++] = (char) ((-(int) nCrc) & 0xff);
        sendBuf[len++] = 0x10;
        sendBuf[len++] = 0x03;

        mSendResult = sendPacket(sendBuf, sendBuf.length);
    }

    @Override
    public void disconnect() {
        // TODO: implement
    }
}