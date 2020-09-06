package sky4s.garminhud.hud;

import android.util.Log;

import sky4s.garminhud.eOutAngle;
import sky4s.garminhud.eOutType;
import sky4s.garminhud.eUnits;

public class DummyHUD extends HUDAdapter {
    private static final String TAG = DummyHUD.class.getSimpleName();

    private void log(Object object) {
        Log.i(TAG, object.toString());
    }

    @Override
    public void registerConnectionCallback(ConnectionCallback callback) {
        // dummy implementation is always connected
        callback.onConnectionStateChange(ConnectionCallback.ConnectionState.CONNECTED);
    }

    @Override
    public boolean isUpdatable() {
        return true;
    }

    @Override
    public boolean getSendResult() {
        return true;
    }

    @Override
    public void SetTime(int nH, int nM, boolean bFlag, boolean bTraffic, boolean bColon, boolean bH) {
        log("Time: " + nH + (bColon ? ":" : " ") + nM + (bH ? "h" : "") + " Flag" + bFlag + " Traffic" + bTraffic);
    }

    @Override
    public final void SetRemainTime(int nH, int nM, boolean bTraffic) {
        log("Time: " + nH + ":" + nM + " " + bTraffic);
    }

    @Override
    public void ClearTime() {
        log("Clear Time");
    }

    @Override
    public void SetDistance(float nDist, eUnits unit) {
        log("Distance: " + nDist + " " + unit);
    }

    @Override
    public void ClearDistance() {
        log("Clear Distance");
    }

    @Override
    public void SetRemainingDistance(float nDist, eUnits unit) {
        log("Remaining distance: " + nDist + " " + unit);
    }

    @Override
    public void ClearRemainingDistance() {
        log("Clear Remaining Distance");
    }

    @Override
    public void SetAlphabet(char a, char b, char c, char d) {
        log("SetAlphabet: " + a + " " + b + " " + c + " " + d);
    }

    @Override
    public void SetDirection(eOutAngle nDir, eOutType nType, eOutAngle nRoundaboutOut) {
        log("SetDirection: " + nDir + ", " + nType + ", " + nRoundaboutOut);
    }

    @Override
    public void SetLanes(char nArrow, char nOutline) {
        log("SetLanes: " + nArrow + ", " + nOutline);
    }

    @Override
    public void SetSpeed(int nSpeed, boolean bIcon) {
        log("Speed: " + nSpeed + " " + bIcon);
    }

    @Override
    public void SetSpeedWarning(int nSpeed, int nLimit, boolean bSpeeding, boolean bIcon, boolean bSlash) {
        log("Speed: " + nSpeed + " / " + nLimit + " " + bSpeeding + " " + bIcon + " " + bSlash);
    }

    @Override
    public void ClearSpeedAndWarning() {
        log("Clear Speed & Warning");
    }

    @Override
    public void SetCameraIcon(boolean visible) {
        log("SetCameraIcon: visible: " + visible);
    }

    @Override
    public void SetGpsLabel(boolean visible) {
        log("SetGpsLabel: visible: " + visible);
    }

    @Override
    public void SetAutoBrightness() {
        log("SetAutoBrightness");
    }

    @Override
    public void SetBrightness(int brightness) {
        log("SetBrightness: " + brightness);
    }

    @Override
    public void disconnect() {
        log("Disconnect");
    }
}
