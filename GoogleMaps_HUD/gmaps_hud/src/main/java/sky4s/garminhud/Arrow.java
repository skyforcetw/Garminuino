package sky4s.garminhud;

public enum Arrow {
    SharpRight(30594879),
    Right(31381247),
    EasyRight(31387391),
    KeepRight(33283327),
    Straight(33554175),
    GoTo(33279999),
    EasyLeft(16224127),
    KeepLeft(33417023),
    Left(16232319),
    SharpLeft(16035327),
    LeftDown(16170623),
    RightDown(30848639),
    ArrivalsRight(18849023),
    ArrivalsLeft(3925823),

    RightToLeave(31378687),
    LeftToLeave(16240447),

    LeaveRoundabout(16506623),
    LeaveRoundaboutUp(25157247),
    LeaveRoundaboutLeft(33023167),
    LeaveRoundaboutRight(33398783),
    LeaveRoundaboutCC(31321983),
    LeaveRoundaboutUpCC(28966527),
    LeaveRoundaboutLeftCC(33286143),
    LeaveRoundaboutRightCC(33463615),

    Arrivals(33279615),
    Convergence(28966527),


    None(0);

    public final int value;

    private Arrow(int value) {
        this.value = value;
    }
}
