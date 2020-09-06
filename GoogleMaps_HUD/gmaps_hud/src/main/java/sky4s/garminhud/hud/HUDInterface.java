package sky4s.garminhud.hud;

import sky4s.garminhud.eOutAngle;
import sky4s.garminhud.eOutType;
import sky4s.garminhud.eUnits;

public interface HUDInterface {
    interface ConnectionCallback {
        enum ConnectionState {
            CONNECTED,
            DISCONNECTED,
        }

        void onConnectionStateChange(ConnectionState state);
    }

    void registerConnectionCallback(ConnectionCallback callback);

    boolean isUpdatable();

    boolean getSendResult();

    void SetTime(int nH, int nM, boolean bFlag, boolean bTraffic, boolean bColon, boolean bH);

    void SetCurrentTime(int nH, int nM);

    void SetRemainTime(int nH, int nM, boolean bTraffic);

    void ClearTime();

    void SetDistance(float nDist, eUnits unit);

    void ClearDistance();

    void SetRemainingDistance(float nDist, eUnits unit);

    void ClearRemainingDistance();

    void SetAlphabet(char a, char b, char c, char d);

    void SetDirection(eOutAngle nDir);

    void SetDirection(final eOutAngle nDir, final eOutType nType, final eOutAngle nRoundaboutOut);

    void SetLanes(char nArrow, char nOutline);

    void SetSpeed(int nSpeed, boolean bIcon);

    void SetSpeedWarning(int nSpeed, int nLimit, boolean bSpeeding, boolean bIcon, boolean bSlash);

    void ClearSpeedAndWarning();

    void SetCameraIcon(boolean visible);

    void SetGpsLabel(boolean visible);

    void SetAutoBrightness();

    void SetBrightness(int brightness);

    void disconnect();
}