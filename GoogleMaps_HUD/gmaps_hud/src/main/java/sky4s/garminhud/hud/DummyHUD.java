package sky4s.garminhud.hud;

import android.content.Intent;
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
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        return false;
    }

    @Override
    public void scanForHud() {
        log("scanForHud");
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
    public void setTime(int nH, int nM, boolean bFlag, boolean bTraffic, boolean bColon, boolean bH) {
        log("Time: " + nH + (bColon ? ":" : " ") + nM + (bH ? "h" : "") + " Flag" + bFlag + " Traffic" + bTraffic);
    }

    @Override
    public final void setRemainTime(int nH, int nM, boolean bTraffic) {
        log("Time: " + nH + ":" + nM + " " + bTraffic);
    }

    @Override
    public void clearTime() {
        log("Clear Time");
    }

    @Override
    public void setDistance(float nDist, eUnits unit) {
        log("Distance: " + nDist + " " + unit);
    }

    @Override
    public void clearDistance() {
        log("Clear Distance");
    }

    @Override
    public void setRemainingDistance(float nDist, eUnits unit) {
        log("Remaining distance: " + nDist + " " + unit);
    }

    @Override
    public void clearRemainingDistance() {
        log("Clear Remaining Distance");
    }

    @Override
    public void setAlphabet(char a, char b, char c, char d) {
        log("SetAlphabet: " + a + " " + b + " " + c + " " + d);
    }

    @Override
    public void setDirection(eOutAngle nDir, eOutType nType, eOutAngle nRoundaboutOut) {
        log("SetDirection: " + nDir + ", " + nType + ", " + nRoundaboutOut);
    }

    @Override
    public void setLanes(char nArrow, char nOutline) {
        log("SetLanes: " + nArrow + ", " + nOutline);
    }

    @Override
    public void setSpeed(int nSpeed, boolean bIcon) {
        log("Speed: " + nSpeed + " " + bIcon);
    }

    @Override
    public void setSpeedWarning(int nSpeed, int nLimit, boolean bSpeeding, boolean bIcon, boolean bSlash) {
        log("Speed: " + nSpeed + " / " + nLimit + " " + bSpeeding + " " + bIcon + " " + bSlash);
    }

    @Override
    public void clearSpeedAndWarning() {
        log("Clear Speed & Warning");
    }

    @Override
    public void setCameraIcon(boolean visible) {
        log("SetCameraIcon: visible: " + visible);
    }

    @Override
    public void setGpsLabel(boolean visible) {
        log("SetGpsLabel: visible: " + visible);
    }

    @Override
    public void setAutoBrightness() {
        log("SetAutoBrightness");
    }

    @Override
    public void setBrightness(int brightness) {
        log("SetBrightness: " + brightness);
    }

    @Override
    public void disconnect() {
        log("Disconnect");
    }
}
