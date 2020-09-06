package sky4s.garminhud.hud;

import sky4s.garminhud.eOutAngle;
import sky4s.garminhud.eOutType;

public abstract class HUDAdapter implements HUDInterface {
    @Override
    public final void setCurrentTime(int nH, int nM) {
        setTime(nH, nM, false, false, true, false);
    }

    @Override
    public final void setDirection(eOutAngle nDir) {
        setDirection(nDir, eOutType.Lane, eOutAngle.AsDirection);
    }

}
