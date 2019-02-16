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
    LeaveRoundaboutSharpLeft(24817023),
    LeaveRoundaboutEasyLeft(24624831),
    LeaveRoundaboutRight(33398783),
    LeaveRoundaboutSharpRight(28696575),
    LeaveRoundaboutEasyRight(20971007),
    LeaveRoundaboutAsDirection(20577919),
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
