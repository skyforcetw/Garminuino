package sky4s.garminhud;

public enum Arrow {
    Arrivals(1157442937613201408L),
    ArrivalsLeft(6363696276412435456L),
    ArrivalsRight(3896836750691024896L),
    Convergence(5505712186103173120L),
    EasyLeft(4629771062305364992L),
    EasyRight(289360692294742016L),
    GoTo(1157443212756910080L),
    KeepLeft(1157442799634484224L),
    KeepRight(1157442834402394112L),
    LeaveRoundabout(433484693105016832L),
    LeaveRoundaboutCC(54148887965868032L),
    LeaveRoundaboutLeft(4629806193894752256L),
    LeaveRoundaboutLeftCC(4629771630155198464L),
    LeaveRoundaboutLeftDownCC(2318857914540390448L),
    LeaveRoundaboutLeftUpCC(8682039362478542337L),
    LeaveRoundaboutRight(868082327036763648L),
    LeaveRoundaboutRightCC(868082369943306240L),
    LeaveRoundaboutRightDown(1799461538112093208L),
    LeaveRoundaboutRightUp(4070205431525982208L),
    LeaveRoundaboutUp(1732773254121656320L),
    LeaveRoundaboutUpCC(3476849762382647296L),
    Left(4665941148194045952L),
    LeftDown(19224197586223104L),
    LeftToLeave(4665799858519281664L),
    Right(289360692970790912L),
    RightDown(19356783226650624L),
    RightToLeave(289360727672619008L),
    SharpLeft(4629771114118135808L),
    SharpRight(289361105548739584L),
    Straight(1157442765411848192L),


    None(0);

    public final long value;

    Arrow(long value) {
        this.value = value;
    }
}
