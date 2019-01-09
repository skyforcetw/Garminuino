package com.example.notificationlistenerdemo;

public enum Arrow {
    SharpRight(54751),
    Right(56607),
    EasyRight(56767),
    KeepRight(47999),
    Straight(48063),
    GoTo(47551),
    EasyLeft(30655),
    KeepLeft(48095),
    Left(30495),
    SharpLeft(30079),
    LeftDown(21951),
    RightDown(54719),
    ArrivalsRight(38911),
    ArrivalsLeft(15871),

    RightToLeave(56639),
    LeftToLeave(30623),

    LeaveRoundabout(32703),
    LeaveRoundaboutUp(31679),
    LeaveRoundaboutLeft(30111),
    LeaveRoundaboutRight(54143),

    Arrivals(48127),
    Convergence(56255),
    None(0);

    public final short value;

    private Arrow(int value) {
        this.value = (short) value;
    }
}