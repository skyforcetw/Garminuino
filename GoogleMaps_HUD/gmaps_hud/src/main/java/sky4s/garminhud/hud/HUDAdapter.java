package sky4s.garminhud.hud;

import sky4s.garminhud.eOutAngle;
import sky4s.garminhud.eOutType;

public abstract class HUDAdapter implements HUDInterface {
    @Override
    public final void SetCurrentTime(int nH, int nM) {
        SetTime(nH, nM, false, false, true, false);
    }

    @Override
    public final void SetDirection(eOutAngle nDir) {
        SetDirection(nDir, eOutType.Lane, eOutAngle.AsDirection);
    }

    @Override
    public final void ShowCameraIcon() {
        SetCameraIcon(true);
    }

    @Override
    public final void ShowGpsLabel() {
        SetGpsLabel(true);
    }

    @Override
    public final void clear() {
        SetCameraIcon(false);
        SetGpsLabel(false);
    }
}
