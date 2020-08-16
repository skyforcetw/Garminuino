package sky4s.garminhud.hud;

public class BMWMessage {

    private static final int MSG_BUFFER_SIZE = 26;

    private static final int DATA_BEGIN_OFFSET = 0x02;
    private static final int UNK_OFFSET_2 = 0x02;
    private static final int SPEED_LIMIT_METRIC_OFFSET = 0x03;
    private static final int SPEED_CAMERA_OFFSET = 0x04;
    private static final int UNK_OFFSET_5 = 0x05;
    private static final int SPEED_LIMIT_OFFSET = 0x06;
    private static final int DIST_TO_TURN_0_OFFSET = 0x07;
    private static final int DIST_TO_TURN_1_OFFSET = 0x08;
    private static final int DIST_TO_TURN_2_OFFSET = 0x09;
    private static final int DIST_TO_TURN_DISABLE_OFFSET = 0x0a;
    private static final int ARROW_OFFSET = 0x0b;
    private static final int LANE_COUNT_OFFSET = 0x0c;
    private static final int LANE_INDEX_OFFSET = 0x0d;
    private static final int LANE_INDEX_DISABLE_OFFSET = 0x0e;
    private static final int ARRIVAL_TIME_HOURS_OFFSET = 0x0f;
    private static final int ARRIVAL_TIME_MINUTES_OFFSET = 0x10;
    private static final int ARRIVAL_TIME_AMPM_OFFSET = 0x11;
    private static final int REMAINING_DIST_0_OFFSET = 0x12;
    private static final int REMAINING_DIST_1_OFFSET = 0x13;
    private static final int REMAINING_DIST_2_OFFSET = 0x14;
    private static final int REMAINING_DIST_DISABLE_OFFSET = 0x15;
    private static final int TRAFFIC_DELAY_OFFSET = 0x16;
    private static final int DATA_END_OFFSET = 0x17;
    private static final int CHECKSUM_OFFSET = 0x18;

    private static final int ARROW_BEGIN = 0x00;
    public static final int ARROW_NONE = 0x00;

    // 0 is straight backwards, 180 is straight forward
    public static final int ARROW_180 = 0x01;
    public static final int ARROW_OFFRAMP_LEFT = 0x02;
    public static final int ARROW_OFFRAMP_RIGHT = 0x03;
    public static final int ARROW_RIGHT_135 = 0x04;
    public static final int ARROW_RIGHT_90 = 0x05;
    public static final int ARROW_RIGHT_45 = 0x06;
    public static final int ARROW_LEFT_135 = 0x07;
    public static final int ARROW_LEFT_90 = 0x08;
    public static final int ARROW_LEFT_45 = 0x09;

    // U turn on right side
    public static final int ARROW_RIGHT_0 = 0x0a;
    // U turn on left side
    public static final int ARROW_LEFT_0 = 0x0b;

    // 0 is straight backwards, 180 is straight forward (roundabouts)
    public static final int ARROW_ROUNDABOUT_RIGHT_180 = 0x0c;
    public static final int ARROW_ROUNDABOUT_RIGHT_135 = 0x0d;
    public static final int ARROW_ROUNDABOUT_RIGHT_90 = 0x0e;
    public static final int ARROW_ROUNDABOUT_RIGHT_45 = 0x0f;
    public static final int ARROW_ROUNDABOUT_RIGHT_225 = 0x10;
    public static final int ARROW_ROUNDABOUT_RIGHT_270 = 0x11;
    public static final int ARROW_ROUNDABOUT_RIGHT_315 = 0x12;
    public static final int ARROW_ROUNDABOUT_RIGHT_360 = 0x13;
    public static final int ARROW_ROUNDABOUT_LEFT_180 = 0x14;
    public static final int ARROW_ROUNDABOUT_LEFT_135 = 0x15;
    public static final int ARROW_ROUNDABOUT_LEFT_90 = 0x16;
    public static final int ARROW_ROUNDABOUT_LEFT_45 = 0x17;
    public static final int ARROW_ROUNDABOUT_LEFT_225 = 0x18;
    public static final int ARROW_ROUNDABOUT_LEFT_270 = 0x19;
    public static final int ARROW_ROUNDABOUT_LEFT_315 = 0x1a;
    public static final int ARROW_ROUNDABOUT_LEFT_360 = 0x1b;

    public static final int ARROW_FORK_RIGHT = 0x1c;
    public static final int ARROW_FORK_LEFT = 0x1d;

    private static final int ARROW_END = ARROW_FORK_LEFT + 1;

    public static final int TIME_SUFFIX_AM = 0x00;
    public static final int TIME_SUFFIX_PM = 0x01;
    public static final int TIME_SUFFIX_HOURS = 0x02;

    // HUD supports up to 6 lanes
    public static final int MAX_LANES = 6;

    public static final int YARDS_PER_MILE = 1760;

    private byte[] mBuffer = new byte[MSG_BUFFER_SIZE];

    public BMWMessage() {
        setHeader();
        setFooter();
    }

    private void setHeader() {
        // packet format appears to hardcode this header
        mBuffer[0] = 0x7a;
        mBuffer[1] = 0x02;
    }

    private void setFooter() {
        // packet format appears to hardcode this footer
        mBuffer[23] = 0x01;
        mBuffer[25] = 0x01;
    }

    private void updateChecksum() {
        mBuffer[CHECKSUM_OFFSET] = calculateChecksum(mBuffer);
    }

    public void setSpeedLimit(int speed, boolean isMetric) {
        mBuffer[SPEED_LIMIT_OFFSET] = (byte) (speed & 0xff);
        mBuffer[SPEED_LIMIT_METRIC_OFFSET] = (byte) (isMetric ? 1 : 0);

        updateChecksum();
    }

    public void setSpeedCameraEnabled(boolean enabled) {
        mBuffer[SPEED_CAMERA_OFFSET] = (byte) (enabled ? 1 : 0);

        updateChecksum();
    }

    public void setDistanceToTurn(double miles) {
        // TODO: map into 3 distance remaining fields
        updateChecksum();
    }

    public void setArrow(int direction) {
        if (direction < ARROW_BEGIN || direction > ARROW_END) {
            return;
        }
        mBuffer[ARROW_OFFSET] = (byte) (direction);

        updateChecksum();
    }

    public void setLaneCount(int numLanes) {
        if (numLanes < 0 || numLanes > MAX_LANES) {
            return;
        }
        mBuffer[LANE_COUNT_OFFSET] = (byte) (numLanes);

        updateChecksum();
    }

    public void setLaneIndicator(int index, boolean enable) {
        byte laneIndex = (byte) index;
        if (laneIndex > (1 << MAX_LANES) - 1) {
            // max lanes is 6, so 2^6 - 1 possible combos
            return;
        }

        // laneIndex starts from the right, index 0 is right-most lane
        if (enable) {
            mBuffer[LANE_INDEX_OFFSET] |= (1 << laneIndex);
        } else {
            mBuffer[LANE_INDEX_OFFSET] &= ~(1 << laneIndex);
        }

        updateChecksum();
    }

    public void setArrivalTime(int hours, int minutes, int suffix) {
        if (hours < 0 || hours > 24 || minutes < 0 || minutes > 59) {
            return;
        }
        mBuffer[ARRIVAL_TIME_HOURS_OFFSET] = (byte) hours;
        mBuffer[ARRIVAL_TIME_MINUTES_OFFSET] = (byte) minutes;
        mBuffer[ARRIVAL_TIME_AMPM_OFFSET] = (byte) suffix;

        updateChecksum();
    }

    private static byte getRemainingDistanceOffset0(int yards) {
        // Calculate ourselves because BMW's scaling is weird
        if (yards < 15) {
            return 10;
        } else if (yards < 25) {
            return 20;
        } else if (yards < 35) {
            return 30;
        } else if (yards < 45) {
            return 40;
        } else if (yards < 55) {
            return 50;
        } else if (yards < 65) {
            // 55 makes it display 60 for some reason
            return 55;
        } else if (yards < 75) {
            // 60 makes it display 70 for some reason
            return 60;
        } else if (yards < 85) {
            // 70 makes it display 80 for some reason
            return 70;
        } else if (yards < 95) {
            // 80 makes it display 90 for some reason
            return 80;
        } else if (yards < 150) {
            return 100;
        } else if (yards < 200) {
            return (byte) 150;
        } else if (yards < 250) {
            return (byte) 200;
        } else {
            return (byte) 231;
        }
    }

    public void setRemainingDistance(double miles) {
        // TODO: figure out how this works in metric
        double yards = miles * YARDS_PER_MILE;
        if (yards < 300) {
            // REMAINING_DIST_OFFSET_0 displays from 10-250yd
            mBuffer[REMAINING_DIST_0_OFFSET] = getRemainingDistanceOffset0((int) yards);
        } else if (miles < 37.5) {
            // REMAINING_DIST_OFFSET_1 displays fromm 300yd to 37mi
            miles = Math.round(miles / 0.16);
            // largest displayable number is 231 (37 miles)
            miles = Math.max(miles, 231);
            mBuffer[REMAINING_DIST_1_OFFSET] = (byte) miles;
        } else {
            // REMAINING_DIST_OFFSET_2 displays from 37mi+
            // TODO: Figure out the formula
        }

        updateChecksum();
    }

    public void setTrafficDelay(int minutes) {
        if (minutes < 0) {
            return;
        }

        // HUD is only capable of delaying up to 99
        minutes = Math.min(minutes, 99);

        mBuffer[TRAFFIC_DELAY_OFFSET] = (byte) minutes;

        updateChecksum();
    }

    public byte[] getBytes() {
        return mBuffer;
    }

    private static byte calculateChecksum(byte[] msg) {
        int checksum = 0;
        for (int i = DATA_BEGIN_OFFSET; i < DATA_END_OFFSET; i++) {
            checksum += msg[i];
        }
        checksum -= 0xff;
        checksum &= 0xff;

        return (byte) checksum;
    }
}
