package sky4s.garmin;

/**
 * Created by skyforce on 2018/8/13.
 */

public enum eLane {
    DotsRight(0x01),
    OuterRight(0x02),
    MiddleRight(0x04),
    InnerRight(0x08),
    InnerLeft(0x10),
    MiddleLeft(0x20),
    OuterLeft(0x40),
    DotsLeft(0x80);

    final public int value;

    eLane(int value) {
        this.value = value;
    }
}