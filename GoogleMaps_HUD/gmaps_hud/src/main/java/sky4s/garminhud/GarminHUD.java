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
    private static int maxUpdatePerSecond = 6;
    private int updateCount = 0;
    private long lastUpdateClearTime = System.currentTimeMillis();
    private BluetoothSPP bt;
    private boolean sendResult = false;

    public GarminHUD(BluetoothSPP bt) {
        this.bt = bt;
    }
    //===========================================================================================

    public final void setMaxUpdatePerSecond(int max) {
        maxUpdatePerSecond = max;
    }

    public boolean isUpdatable() {
        final long now = System.currentTimeMillis();
        final long interval = now - lastUpdateClearTime;
        if (interval > 1000) {
            lastUpdateClearTime = now;
            updateCount = 0;
        }

        final boolean updatable = updateCount < maxUpdatePerSecond;
        if (!updatable) {
            sendResult = false;
        }
        return updatable;
    }

    private char Digit(int n) {
        n = n % 10;
        if (n == 0)
            return (char) 10;
        else
            return (char) n;
    }

    private boolean SendPacket(char[] pBuf, int length_of_data) {
        if (!isUpdatable() || null == bt) {
            return false;
        }
        updateCount++;
        if (MainActivity.IGNORE_BT_DEVICE) {
            return true;
        }
        byte packet[] = new byte[length_of_data];
        for (int x = 0; x < length_of_data; x++) {
            packet[x] = (byte) pBuf[x];
        }

        if(bt.isServiceAvailable()) { //judge service exist to avoid => app.akexorcist.bluetotohspp.library.BluetoothService.getState()' on a null object reference
            bt.send(packet, false);
        }
        return true;
    }

    public boolean getSendResult() {
        return sendResult;
    }

    private final boolean FINAL_FLAG_FALSE = false;

    private void SendHud2(char[] pBuf) {
        int nLen = pBuf.length;

        char sendBuf[] = new char[255];
        char len = 0;
        //unsigned int nCrc = 0xeb + nLen + nLen;
        int stuffing_count = 0;

        sendBuf[len++] = 0x10;
        sendBuf[len++] = 0x7b;
        sendBuf[len++] = (char) (nLen + 6);
        if (nLen == 0xa) {
            sendBuf[len++] = 0x10;
            stuffing_count++;
        }
        sendBuf[len++] = (char) nLen;
        sendBuf[len++] = 0x00;
        sendBuf[len++] = 0x00;
        sendBuf[len++] = 0x00;
        sendBuf[len++] = 0x55;
        sendBuf[len++] = 0x15;

        for (int i = 0; i < nLen; i++) {
            //nCrc += pBuf[i];
            sendBuf[len++] = pBuf[i];
            if (pBuf[i] == 0x10) { //Escape LF
                sendBuf[len++] = 0x10;
                stuffing_count++;
            }
        }

        int nCrc = 0;
        for (int i = 1; i < len; i++) {
            nCrc += sendBuf[i];
        }
        nCrc -= stuffing_count * 0x10;


        sendBuf[len++] = (char) ((-(int) nCrc) & 0xff);
        sendBuf[len++] = 0x10;
        sendBuf[len++] = 0x03;

        sendResult = SendPacket(sendBuf, len);
    }

//    public void SetTime(int nH, int nM, boolean bH, boolean bFlag) {
//        SetTime(nH, nM, bFlag, false, true, bH);
//    }
//
//    public void SetTime(int nH, int nM, boolean bH) {
//        SetTime(nH, nM, true, false, true, bH);
//    }


    public void SetTime(int nH, int nM, boolean bFlag, boolean bTraffic, boolean bColon, boolean bH) {
        char arr[] = {(char) 0x05,
                bTraffic ? (char) 0xff : (char) 0x00,
                Digit(nH / 10), Digit(nH), // hour
                bColon ? (char) 0xff : (char) 0x00, // :
                Digit(nM / 10), Digit(nM), //minute
                bH ? (char) 0xff : (char) 0x00, // post-fix 'h'
                bFlag ? (char) 0xff : (char) 0x00};
        SendHud2(arr);
    }

    public void SetRemainTime(int nH, int nM, boolean bTraffic) {
//        final boolean bTraffic = false;
        final boolean bH = false;
        final boolean bFlag = true;

        boolean noHour = 0 == nH;
        boolean minLessThen10 = noHour && nM < 10;
        char arr[] = {(char) 0x05,
                bTraffic ? (char) 0xff : (char) 0x00,
                noHour ? (char) 0 : Digit(nH / 10),// hour n_
                noHour ? (char) 0 : Digit(nH), // hour _n
                noHour ? (char) 0 : (char) 0xff, // :
                minLessThen10 ? (char) 0 : Digit(nM / 10), //minute n_
                Digit(nM), //minute _n
                bH ? (char) 0xff : (char) 0x00, // post-fix 'h'
                bFlag ? (char) 0xff : (char) 0x00};
        SendHud2(arr);
    }

    public void ClearTime() {
        char arr[] = {(char) 0x05,
                0x00,
                0, 0,
                0x00,
                0, 0,
                0x00,
                // 0x00
        };
        SendHud2(arr);
    }


//    public void SetDistance(int nDist, eUnits unit) {
//        SetDistance(nDist, unit, false, false);
//    }

    public void SetDistance(int nDist, eUnits unit, boolean bDecimal, boolean bLeadingZero) {
        char arr[] = {(char) 0x03,
                Digit(nDist / 1000), Digit(nDist / 100), Digit(nDist / 10),
                bDecimal ? (char) 0xff : (char) 0x00, Digit(nDist), (char) unit.value};
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
            if (bDecimal && ((int) nDist / 10) == 0) // Show leding zero for decimals
                arr[3] = 0xa;
        }
        SendHud2(arr);
    }

    public void ClearDistance() {
        char arr[] = {(char) 0x03, 0x0, 0x0, 0x0, 0x00, 0, 0};
        SendHud2(arr);
    }

    public void SetAlphabet(char a, char b, char c, char d) {
        eUnits unit = eUnits.None;
        boolean bDecimal = false, bLeadingZero = false;


        char arr[] = {(char) 0x03, a, b, c,
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
        SendHud2(arr);
    }

//    public void SetDirection(eOutAngle nDir) {
//        SetDirection(nDir, eOutType.Lane, eOutAngle.AsDirection);
//    }

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
    public void SetDirection(final eOutAngle nDir, final eOutType nType, final eOutAngle nRoundaboutOut) {
        char arr[] = {(char) 0x01,
                (nDir == eOutAngle.LeftDown) ? (char) 0x10 : ((nDir == eOutAngle.RightDown) ? (char) 0x20 : (char) nType.value),
                (nType == eOutType.RightRoundabout || nType == eOutType.LeftRoundabout) ?
                        ((char) ((nRoundaboutOut == eOutAngle.AsDirection) ? nDir.value : nRoundaboutOut.value)) : (char) 0x00,
                (nDir == eOutAngle.LeftDown || nDir == eOutAngle.RightDown) ? (char) 0x00 : (char) nDir.value};
        SendHud2(arr);
    }

    public void SetLanes(char nArrow, char nOutline) {
        char arr[] = {0x02, nOutline, nArrow};
        SendHud2(arr);
    }

    public void SetSpeed(int nSpeed, boolean bIcon) {
        boolean bSlash = false;
        boolean bSpeeding = false;

        char hundreds_digit, tens_digit, ones_digit;
        if (nSpeed < 10) {
            // Delete leading zeros
            hundreds_digit = (char) 0x00;
            tens_digit = (char) 0x00;
        } else {
            hundreds_digit = (char) ((nSpeed / 100) % 10);
            tens_digit = Digit(nSpeed / 10);
        }
        ones_digit = Digit(nSpeed);


        char arr[] = {(char) 0x06,
                (char) 0x00, (char) 0x00, (char) 0x00, bSlash ? (char) 0xff : (char) 0x00,
                hundreds_digit, tens_digit, ones_digit, bSpeeding ? (char) 0xff : (char) 0x00,
                bIcon ? (char) 0xff : (char) 0x00};

        SendHud2(arr);
    }

//    public void SetSpeedAndWarning(int nSpeed, int nLimit) {
//        SetSpeedWarning(nSpeed, nLimit, false, true, true);
//    }

    public void SetSpeedWarning(int nSpeed, int nLimit, boolean bSpeeding, boolean bIcon, boolean bSlash) {
        char arr[] = {(char) 0x06,
                (char) ((nSpeed / 100) % 10), Digit(nSpeed / 10), Digit(nSpeed), bSlash ? (char) 0xff : (char) 0x00,
                (char) ((nLimit / 100) % 10), Digit(nLimit / 10), Digit(nLimit), bSpeeding ? (char) 0xff : (char) 0x00,
                bIcon ? (char) 0xff : (char) 0x00};

        SendHud2(arr);
    }

    public void ClearSpeedandWarning() {
        char arr[] = {(char) 0x06,
                (char) 0x00, (char) 0x00, (char) 0x00, (char) 0x00,
                (char) 0x00, (char) 0x00, (char) 0x00, (char) 0x00,
                (char) 0x00};
        SendHud2(arr);
    }

//    public void ShowCameraIcon() {
//        SetCameraIcon(true);
//    }

    public void SetCameraIcon(boolean visible) {
        char arr[] = {0x04, (char) (visible ? 1 : 0)};
        SendHud2(arr);
    }

//    public void ShowGpsLabel() {
//        SetGpsLabel(true);
//    }

    public void SetGpsLabel(boolean visible) {
        char arr[] = {0x07, (char) (visible ? 1 : 0)};
        SendHud2(arr);
    }

    public void SetAutoBrightness() {
        char command_auto_brightness[] = {0x10, 0x7B, 0x0E, 0x08, 0x00, 0x00, 0x00, 0x56, 0x15, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x10, 0x03};
        sendResult = SendPacket(command_auto_brightness, command_auto_brightness.length);
    }

    public void SetBrightness(int brightness) {
        char sendBuf[] = new char[8];
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

        sendResult = SendPacket(sendBuf, sendBuf.length);
    }

//    public void clear() {
//        SetCameraIcon(false);
//        SetGpsLabel(false);
//    }

}
