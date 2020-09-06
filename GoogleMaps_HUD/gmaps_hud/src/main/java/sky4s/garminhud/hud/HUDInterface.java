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

    void setTime(int nH, int nM, boolean bFlag, boolean bTraffic, boolean bColon, boolean bH);

    void setCurrentTime(int nH, int nM);

    void setRemainTime(int nH, int nM, boolean bTraffic);

    void clearTime();

    void setDistance(float nDist, eUnits unit);

    void clearDistance();

    void setRemainingDistance(float nDist, eUnits unit);

    void clearRemainingDistance();

    void setAlphabet(char a, char b, char c, char d);

    void setDirection(eOutAngle nDir);

    void setDirection(final eOutAngle nDir, final eOutType nType, final eOutAngle nRoundaboutOut);

    void setLanes(char nArrow, char nOutline);

    void setSpeed(int nSpeed, boolean bIcon);

    void setSpeedWarning(int nSpeed, int nLimit, boolean bSpeeding, boolean bIcon, boolean bSlash);

    void clearSpeedAndWarning();

    void setCameraIcon(boolean visible);

    void setGpsLabel(boolean visible);

    void setAutoBrightness();

    void setBrightness(int brightness);

    void disconnect();
}