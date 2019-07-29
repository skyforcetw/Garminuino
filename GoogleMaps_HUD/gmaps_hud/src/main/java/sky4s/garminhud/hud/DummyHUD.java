package sky4s.garminhud.hud;

import sky4s.garminhud.eOutAngle;
import sky4s.garminhud.eOutType;
import sky4s.garminhud.eUnits;

public class DummyHUD implements HUDInterface {
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

    }

    @Override
    public void SetTime(int nH, int nM, boolean bH) {

    }

    @Override
    public void SetTime(int nH, int nM, boolean bFlag, boolean bTraffic, boolean bColon, boolean bH) {

    }

    @Override
    public void SetRemainTime(int nH, int nM) {

    }

    @Override
    public void ClearTime() {

    }

    @Override
    public void SetDistance(int nDist, eUnits unit) {

    }

    @Override
    public void SetDistance(int nDist, eUnits unit, boolean bDecimal, boolean bLeadingZero) {

    }

    @Override
    public void ClearDistance() {

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

    }

    @Override
    public void SetSpeedAndWarning(int nSpeed, int nLimit) {

    }

    @Override
    public void SetSpeedWarning(int nSpeed, int nLimit, boolean bSpeeding, boolean bIcon, boolean bSlash) {

    }

    @Override
    public void ClearSpeedandWarning() {

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

    }
}
