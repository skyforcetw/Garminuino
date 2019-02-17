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
    LeaveRoundaboutAsDirection(1732773254121656320L),
    LeaveRoundaboutAsDirectionCC(3476849762382647296L),
    LeaveRoundaboutCC(54148887965868032L),
    LeaveRoundaboutEasyLeft(3461020728995218944L),
    LeaveRoundaboutEasyLeftCC(3476920200785692160L),
    LeaveRoundaboutEasyRight(1732764373551202304L),
    LeaveRoundaboutEasyRightCC(3485856756830093312L),
    LeaveRoundaboutLeft(4629806193894752256L),
    LeaveRoundaboutLeftCC(4629771630155198464L),
    LeaveRoundaboutRight(868082327036763648L),
    LeaveRoundaboutRightCC(868082369943306240L),
    LeaveRoundaboutSharpLeft(2315461794631516160L),
    LeaveRoundaboutSharpLeftCC(2318839754340726784L),
    LeaveRoundaboutSharpRight(641983038844451840L),
    LeaveRoundaboutSharpRightCC(1772404188400058368L),
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
