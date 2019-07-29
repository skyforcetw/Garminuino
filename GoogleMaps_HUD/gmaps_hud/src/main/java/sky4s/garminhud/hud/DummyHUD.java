package sky4s.garminhud.hud;

import android.util.Log;

import sky4s.garminhud.eOutAngle;
import sky4s.garminhud.eOutType;
import sky4s.garminhud.eUnits;

public class DummyHUD implements HUDInterface {
    private static final String TAG = DummyHUD.class.getSimpleName();

    private void log(Object object) {
        Log.i(TAG, object.toString());
    }

    @Override
    public void setMaxUpdatePerSecond(int max) {

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
    public void SetTime(int nH, int nM, boolean bH, boolean bFlag) {
        log("Time: " + nH + ":" + nM + (bH ? "h" : "") + " " + bFlag);
    }

    @Override
    public void SetTime(int nH, int nM, boolean bH) {
        log("Time: " + nH + ":" + nM + (bH ? "h" : ""));
    }

    @Override
    public void SetTime(int nH, int nM, boolean bFlag, boolean bTraffic, boolean bColon, boolean bH) {
        log("Time: " + nH + (bColon ? ":" : " ") + nM + (bH ? "h" : "") + " " + bFlag + " " + bTraffic);
    }

    @Override
    public void SetRemainTime(int nH, int nM) {
        log("Remain Time: " + nH + ":" + nM);
    }

    @Override
    public void ClearTime() {
        log("Clear Time");
    }

    @Override
    public void SetDistance(int nDist, eUnits unit) {
        log("Distance: " + nDist + " " + unit);
    }

    @Override
    public void SetDistance(int nDist, eUnits unit, boolean bDecimal, boolean bLeadingZero) {
        log("Distance: " + nDist + " " + unit + " " + bDecimal + " " + bLeadingZero);
    }

    @Override
    public void ClearDistance() {
        log("Clear Distance");
    }

    @Override
    public void SetAlphabet(char a, char b, char c, char d) {

    }

    @Override
    public void SetDirection(eOutAngle nDir) {

    }

    @Override
    public void SetDirection(eOutAngle nDir, eOutType nType, eOutAngle nRoundaboutOut) {

    }

    @Override
    public void SetLanes(char nArrow, char nOutline) {

    }

    @Override
    public void SetSpeed(int nSpeed, boolean bIcon) {
        log("Speed: " + nSpeed + " " + bIcon);
    }

    @Override
    public void SetSpeedAndWarning(int nSpeed, int nLimit) {
        log("Speed: " + nSpeed + " / " + nLimit);
    }

    @Override
    public void SetSpeedWarning(int nSpeed, int nLimit, boolean bSpeeding, boolean bIcon, boolean bSlash) {
        log("Speed: " + nSpeed + " / " + nLimit + " " + bSpeeding + " " + bIcon + " " + bSlash);
    }

    @Override
    public void ClearSpeedandWarning() {
        log("Clear Speed & Warning");
    }

    @Override
    public void ShowCameraIcon() {

    }

    @Override
    public void SetCameraIcon(boolean visible) {

    }

    @Override
    public void ShowGpsLabel() {

    }

    @Override
    public void SetGpsLabel(boolean visible) {

    }

    @Override
    public void SetAutoBrightness() {

    }

    @Override
    public void SetBrightness(int brightness) {

    }

    @Override
    public void clear() {
        log("Clear");
    }
}
