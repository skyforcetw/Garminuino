package sky4s.garminhud.hud;

import sky4s.garminhud.eOutAngle;
import sky4s.garminhud.eOutType;
import sky4s.garminhud.eUnits;

public interface HUDInterface {

    void setMaxUpdatePerSecond(int max);

    boolean isUpdatable();

    boolean getSendResult();

    void SetTime(int nH, int nM, boolean bFlag, boolean bTraffic, boolean bColon, boolean bH);

//    void SetETATime(int nH, int nM,boolean bTraffic);

    void SetCurrentTime(int nH, int nM);

    void SetRemainTime(int nH, int nM, boolean bTraffic);

    void ClearTime();

    void SetDistance(int nDist, eUnits unit);

    void SetDistance(int nDist, eUnits unit, boolean bDecimal, boolean bLeadingZero);

    void ClearDistance();

    void SetAlphabet(char a, char b, char c, char d);

    void SetDirection(eOutAngle nDir);

    void SetDirection(final eOutAngle nDir, final eOutType nType, final eOutAngle nRoundaboutOut);

    void SetLanes(char nArrow, char nOutline);

    void SetSpeed(int nSpeed, boolean bIcon);

    void SetSpeedAndWarning(int nSpeed, int nLimit);

    void SetSpeedWarning(int nSpeed, int nLimit, boolean bSpeeding, boolean bIcon, boolean bSlash);

    void ClearSpeedandWarning();

    void ShowCameraIcon();

    void SetCameraIcon(boolean visible);

    void ShowGpsLabel();

    void SetGpsLabel(boolean visible);

    void SetAutoBrightness();

    void SetBrightness(int brightness);

    void clear();
}
