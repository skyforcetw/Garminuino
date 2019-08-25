package sky4s.garminhud.app;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import sky4s.garminhud.eLane;
import sky4s.garminhud.hud.HUDInterface;

public class ImageDetectListener implements ImageReader.OnImageAvailableListener {
    private static class Rect {
        public int x;
        public int y;
        public int width;
        public int height;

        public Rect(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        @Override
        public final String toString() {
            return Integer.toString(x) + "," + Integer.toString(y) + " " + Integer.toString(width) + "/" + Integer.toString(height);
        }

        public boolean valid() {
            return -1 != x && -1 != y && -1 != width && -1 != height;
        }
    }

    private enum Theme {
        DayV1, NightV1, V2, Unknow
    }

    ;

    private MainActivity activity;
    private MainActivityPostman postman;
    private static final String TAG = ImageDetectListener.class.getSimpleName();
    private HUDInterface hud;

    public ImageDetectListener(MainActivity activity) {
        this.activity = activity;
        this.hud = activity.hud;
        if (null != activity) {
            Resources resource = activity.getResources();
            if (null != resource) {
                ROAD_ROI_WIDTH_TOL = resource.getInteger(R.integer.road_roi_width_tol);
                LANE_ROI_WIDTH_TOL = resource.getInteger(R.integer.lane_roi_width_tol);
                LANE_DETECT_X_OFFSET = resource.getInteger(R.integer.lane_detect_x_offset);
                UPDATE_INTERVAL = resource.getInteger(R.integer.detect_update_interval);
            }
        }
        postman = new MainActivityPostman(activity, activity.getString(R.string.broadcast_sender_image_detect));
    }

    public final String PreImage = "myscreen_pre.png";
    public final String NowImage = "myscreen_now.png";
    public final String GmapImage = "gmap.png";
    public final String MapImage = "map.png";
    public final String LaneImage = "lane.png";

    public final int ArrowColor_Day = Color.rgb(66, 133, 244);
    public final int ArrowColor_Night = Color.rgb(223, 246, 255);
    public final int ArrowColor_Static = Color.rgb(199, 201, 201);
    public final int LaneNowWhite = Color.rgb(255, 255, 255);

    public final int RoadBgGreen_Day = Color.rgb(15, 157, 88);
    public final int RoadBgGreen_Night = Color.rgb(13, 144, 79);

    public final int LaneBgGreen_DayV1 = Color.rgb(11, 128, 67);
    public final int LaneBgGreen_NightV1 = Color.rgb(9, 113, 56);
    public final int LaneDivideWhiteV1 = Color.rgb(255, 255, 255);

    public final int RoadBgGreen_V2 = Color.rgb(11, 128, 67);
    public final int LaneBgGreen_V2 = Color.rgb(13, 101, 45);
    public final int LaneDivideWhiteV2 = Color.rgb(129, 201, 149);


    public final int OrangeTraffic_V1 = Color.rgb(255, 171, 52);
    public final int OrangeTraffic_DayV2 = Color.rgb(255, 171, 52);
    public final int OrangeTraffic_NightV2 = Color.rgb(163, 113, 55);
    public final int RedTraffic_V1 = Color.rgb(221, 25, 29);
    public final int RedTraffic_DayV2 = Color.rgb(221, 25, 29);
    public final int RedTraffic_NightV2 = Color.rgb(146, 96, 92);
    private int[] pixelsInFindColor;


    private boolean busyTrafficDetect(Bitmap map, boolean alertYellowTraffic, Theme theme) {
        final boolean isV1 = Theme.DayV1 == theme || Theme.NightV1 == theme;

        Rect orange_roi = isV1 ? getRoi(map, OrangeTraffic_V1) : getRoi(map, OrangeTraffic_DayV2, OrangeTraffic_NightV2);
        Rect roi_red = isV1 ? getRoi(map, RedTraffic_V1) : getRoi(map, RedTraffic_DayV2, RedTraffic_NightV2);
        Log.i(TAG, "busyTrafficDetect: " + "Orange" + orange_roi + " Red" + roi_red);

        boolean yellowTraffic = -1 != orange_roi.x;
        boolean redTraffic = -1 != roi_red.x;

        boolean busyTraffic = alertYellowTraffic ? yellowTraffic || redTraffic : redTraffic;
        return busyTraffic;
    }


    private int ROAD_ROI_WIDTH_TOL = 118;
    private int LANE_ROI_WIDTH_TOL = 10;
    private int LANE_DETECT_X_OFFSET = 100;
    private int ARROW_SIZE_TOL = 200;

    /**
     * detect procedure:
     * 1. road
     * 2. arrow
     * 3. lane -> lane detect
     * 4. traffic detect
     *
     * @param screen
     */
    private void screenDetection(Bitmap screen) {
        boolean road_detect_result = false;
        boolean arrow_detect_result = false;
        boolean lane_detect_result = false;
        boolean traffic_detect_result = false;

        boolean busyTraffic = false;
        Theme theme = Theme.Unknow;

        try {
            if (null == screen) {
                return;
            }

            int screen_width = screen.getWidth();
            int screen_height = screen.getHeight();
            //=====================================
            // road
            //=====================================
            Bitmap half_screen_img = Bitmap.createBitmap(screen, 0, 0, screen.getWidth(), screen_height >> 1);
            storeToPNG(half_screen_img, MainActivity.STORE_DIRECTORY + "half.png");

            Rect road_roi = getRoi(2, half_screen_img, RoadBgGreen_Day);
            theme = Theme.DayV1;

            if (!road_roi.valid() || Math.abs(road_roi.width - screen_width) > ROAD_ROI_WIDTH_TOL) {
                road_roi = getRoi(2, half_screen_img, RoadBgGreen_Night);
                theme = Theme.NightV1;
            }

            if (!road_roi.valid() || Math.abs(road_roi.width - screen_width) > ROAD_ROI_WIDTH_TOL) {
                road_roi = getRoi(2, half_screen_img, RoadBgGreen_V2);
                theme = Theme.V2;
            }

            Log.i(TAG, "Road roi: " + road_roi.toString());
            if (!road_roi.valid()) {
                return;
            }
            storeToPNG(Bitmap.createBitmap(half_screen_img, road_roi.x, road_roi.y, road_roi.width, road_roi.height), MainActivity.STORE_DIRECTORY + "road.png");

            final int gmapHeight = screen_height - road_roi.y;
            if (gmapHeight < 0) {
                return;
            }
            Bitmap gmapScreen = Bitmap.createBitmap(screen, road_roi.x, road_roi.y, road_roi.width, gmapHeight);

            // write bitmap to a file
            storeToPNG(gmapScreen, MainActivity.STORE_DIRECTORY + GmapImage);

            road_detect_result = true;
            //=====================================
            // arrow
            //=====================================
            Bitmap gmapScreenHalfDown = Bitmap.createBitmap(gmapScreen, 0, gmapScreen.getHeight() >> 1, gmapScreen.getWidth(), gmapScreen.getHeight() >> 1);
            storeToPNG(gmapScreenHalfDown, MainActivity.STORE_DIRECTORY + "gmap_half_dw.png");

            final boolean approveArrowStatic = false;
            Rect arrow_roi = null;
            if (Theme.DayV1 == theme || Theme.NightV1 == theme) {
                int ArrowColor = theme == Theme.DayV1 ? ArrowColor_Day : ArrowColor_Night;
                arrow_roi = approveArrowStatic ? getRoi(2, gmapScreenHalfDown, ArrowColor, ArrowColor_Static) :
                        getRoi(2, gmapScreenHalfDown, ArrowColor);
            } else {
                arrow_roi = approveArrowStatic ? getRoi(2, gmapScreenHalfDown, ArrowColor_Day, ArrowColor_Night, ArrowColor_Static) :
                        getRoi(2, gmapScreenHalfDown, ArrowColor_Day, ArrowColor_Night);
            }

            final boolean arrow_is_valid = arrow_roi.valid() && arrow_roi.height < ARROW_SIZE_TOL && arrow_roi.width < ARROW_SIZE_TOL;
            if (arrow_is_valid) {
                arrow_roi.y += gmapScreenHalfDown.getHeight();
                Log.i(TAG, "Found Arrow: " + arrow_roi.toString());
            } else {
                Log.i(TAG, "NoFound Arrow");
                return;
            }
            arrow_detect_result = true;
            //=====================================
            Bitmap map_roi_image = null;
            if (road_detect_result && arrow_detect_result) {
                int x = 0;//road_roi.x;
                int y = road_roi.height;
                int width = gmapScreen.getWidth();
                int height = gmapScreen.getHeight() - y - (gmapScreen.getHeight() - arrow_roi.y);

                if (width > 0 && height > 0) {
                    map_roi_image = Bitmap.createBitmap(gmapScreen, x, y, width, height);
                    // write bitmap to a file
                    storeToPNG(map_roi_image, MainActivity.STORE_DIRECTORY + MapImage);
                }
            }
            //=====================================
            // traffic
            //=====================================
            if (road_detect_result && arrow_detect_result) {
                if (arrow_detect_result) {
                    busyTraffic = busyTrafficDetect(map_roi_image, activity.alertYellowTraffic, theme);
                } else {
                    busyTraffic = false;
                }
                traffic_detect_result = true;
            }

            //=====================================
            // lane
            //=====================================
            final int lane_bg_color = theme == Theme.DayV1 ? LaneBgGreen_DayV1 :
                    theme == Theme.NightV1 ? LaneBgGreen_NightV1 :
                            theme == Theme.V2 ? LaneBgGreen_V2 : 0;
            Rect lane_roi = getRoi(2, map_roi_image, lane_bg_color);

            final boolean lane_roi_exist = lane_roi.valid() && Math.abs(lane_roi.width - road_roi.width) < LANE_ROI_WIDTH_TOL;
            if (lane_roi_exist) {
                Bitmap lane_roi_image = Bitmap.createBitmap(map_roi_image, lane_roi.x, lane_roi.y, lane_roi.width, lane_roi.height);
                storeToPNG(lane_roi_image, MainActivity.STORE_DIRECTORY + LaneImage);

                final int lane_color = theme == Theme.DayV1 || theme == Theme.NightV1 ? LaneDivideWhiteV1 :
                        theme == Theme.V2 ? LaneDivideWhiteV2 : 0;
                ArrayList<Boolean> laneDetectResult = laneDetect(lane_roi_image, lane_bg_color, lane_color);
                if (laneDetectResult.size() != 0) {
                    if (landDetectToHUD(laneDetectResult)) {

                    } else {
                        hud.SetLanes((char) 0, (char) 0);
                        storeToPNG(lane_roi_image, MainActivity.STORE_DIRECTORY + "NG_lane.png");
                    }
                } else {
                    hud.SetLanes((char) 0, (char) 0);
                    storeToPNG(lane_roi_image, MainActivity.STORE_DIRECTORY + "NG_lane.png");
                }
            } else {
                hud.SetLanes((char) 0, (char) 0);
            }
            lane_detect_result = lane_roi_exist;

        } finally {
            activity.sendBooleanExtraByBroadcast(activity.getString(R.string.broadcast_receiver_notification_monitor),
                    activity.getString(R.string.busy_traffic), busyTraffic);
            Log.i(TAG, "detect result: " +
                    Boolean.toString(road_detect_result) + " " +
                    Boolean.toString(arrow_detect_result) + " " +
                    Boolean.toString(lane_detect_result) + " " +
                    Boolean.toString(traffic_detect_result) + ":" +
                    (busyTraffic ? "Busy" : "Normal")
            );
        }
    }


    private ArrayList<Boolean> laneDetect(Bitmap lane, int bgColor, int laneColor) {
        final int height = lane.getHeight() - 1;
        ArrayList<Integer> laneDivide = findLaneDivide(lane, height, bgColor, laneColor);
        ArrayList<Boolean> result = new ArrayList<Boolean>();
        final int yOffset = -2;

        if (laneDivide.size() >= 1) {
            int halfDivideWidth = -1;
            if (laneDivide.size() == 1) {
                int divide_x = laneDivide.get(0);
                final int arrow_y = getFirstVertical(lane, divide_x, 0, laneColor, true, true);
                final int scan_y = arrow_y + yOffset;

                final int left_arrow_x0 = getFirstHorizontal(lane, LANE_DETECT_X_OFFSET, divide_x, scan_y, bgColor, true, false);
                final int left_arrow_x1 = getFirstHorizontal(lane, LANE_DETECT_X_OFFSET, divide_x, scan_y, bgColor, true, true);
                final int arrow_width = left_arrow_x1 - left_arrow_x0;
                final int left_arrow_center = left_arrow_x0 + (arrow_width >> 1);
                halfDivideWidth = divide_x - left_arrow_center;
            } else {
                final int divideWidth = laneDivide.size() >= 2 ? laneDivide.get(1) - laneDivide.get(0) : 0;
                halfDivideWidth = divideWidth >> 1;
            }

            for (int x = 0; x < laneDivide.size(); x++) {
                final int laneCenter = laneDivide.get(x) - halfDivideWidth;
                final int notBGy = getFirstVertical(lane, laneCenter, 0, bgColor, true, true);
                int pixel = lane.getPixel(laneCenter, notBGy + yOffset);
                result.add(isSameRGB(pixel, LaneNowWhite));
            }

            //last
            final int v = laneDivide.get(laneDivide.size() - 1);
            int lastLaneCenter = v + halfDivideWidth;
            final int notBGy = getFirstVertical(lane, lastLaneCenter, 0, bgColor, true, true);
            final int pixel = lane.getPixel(lastLaneCenter, notBGy + yOffset);
            result.add(isSameRGB(pixel, LaneNowWhite));
        }

        return result;
    }


    private static long lastUpdateTime = 0;
    private long UPDATE_INTERVAL = 1500;

    @Override
    public void onImageAvailable(ImageReader reader) {


//        try {
//            String path1 = activity.getApplicationContext().getExternalCacheDir().getAbsolutePath();
//            String path2 = activity.getApplicationContext().getExternalCacheDir().getCanonicalPath();
////            String path3=activity.getApplicationContext().getExternalStorageDirectory().getAbsolutePath();
//            String path3 = Environment.getExternalStorageDirectory().getAbsolutePath();
//            int a = 1;
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        Image image = null;
        FileOutputStream fos = null;
        Bitmap bitmap = null;


        try {
            image = reader.acquireLatestImage();
            if (image != null) {
                long currentTime = System.currentTimeMillis();
                long deltaTime = currentTime - lastUpdateTime;
                boolean do_detection = deltaTime > UPDATE_INTERVAL;//&& int_speed>40;

                if (do_detection) {
                    lastUpdateTime = currentTime;
                    Image.Plane[] planes = image.getPlanes();
                    ByteBuffer buffer = planes[0].getBuffer();
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * activity.mWidth;

                    // create bitmap
                    bitmap = Bitmap.createBitmap(activity.mWidth + rowPadding / pixelStride, activity.mHeight, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(buffer);

                    File nowImage = new File(MainActivity.STORE_DIRECTORY + NowImage);
                    if (nowImage.exists()) {
                        File preImage = new File(MainActivity.STORE_DIRECTORY + PreImage);
                        copy(nowImage, preImage);
                    }

                    // write bitmap to a file
                    storeToPNG(bitmap, MainActivity.STORE_DIRECTORY + NowImage);

                    final boolean loadBitmapFromFile = false;
                    if (loadBitmapFromFile) {
                        bitmap = BitmapFactory.decodeFile(MainActivity.STORE_DIRECTORY + "q.png");
                    }
                    if (!activity.is_in_navigation) {
                        return;
                    }
                    screenDetection(bitmap);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.toString());
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }

            if (bitmap != null) {
                bitmap.recycle();
            }

            if (image != null) {
                image.close();
            }
        }
    }

    private boolean isSameRGB(int color1, int color2) {
        return Color.red(color1) == Color.red(color2) &&
                Color.green(color1) == Color.green(color2) &&
                Color.blue(color1) == Color.blue(color2);
    }

    private int findColor(Bitmap image, int color, boolean vertical, boolean up, boolean left, boolean printDetail, int findWidth) {
        int width = image.getWidth();
        int height = image.getHeight();
        int totalSize = width * height;
        if (null == pixelsInFindColor || totalSize != pixelsInFindColor.length) {
            pixelsInFindColor = null;
            pixelsInFindColor = new int[width * height];
        }

        image.getPixels(pixelsInFindColor, 0, width, 0, 0, width, height);

        int inc = 1;

        int h_start = vertical ? (up ? 0 : height - findWidth) : 0;
        int h_inc = (vertical ? up ? 1 : -1 : 1) * inc;
        int h_end = vertical ? (up ? height - findWidth : 0) : height - 1;

        int w_start = vertical ? 0 : left ? 0 : width - findWidth;
        int w_inc = (vertical ? 1 : left ? 1 : -1) * inc;
        int w_end = vertical ? width - findWidth : left ? width - 1 : 0 + findWidth;

        int w0_end = vertical ? w_start + w_inc : w_end;
        int w1_end = vertical ? w_end : w_start + w_inc;

        /**
         *  w0
         *     h
         *        w1
         */

        for (int w0 = w_start; w0 != w0_end; w0 += w_inc) {
            for (int h = h_start; h != h_end; h += h_inc) {
                for (int w1 = w_start; w1 != w1_end; w1 += w_inc) {
                    int w = vertical ? w1 : w0;

                    boolean allSameColor = true;
                    for (int x = 0; x < findWidth; x++) {
                        int hh = vertical ? h : h + x;
                        int ww = vertical ? w + x : w;
                        int pixel = pixelsInFindColor[ww + hh * width];
                        allSameColor = allSameColor && isSameRGB(pixel, color);
                    }

                    boolean sameColor = allSameColor;

                    if (sameColor) {
                        if (vertical) {
                            if (printDetail)
                                Log.i(TAG, "vertical: " + h + "," + w);
                            return h;
                        } else {
                            if (printDetail)
                                Log.i(TAG, "horizontal: " + h + "," + w);
                            return w;
                        }
                    }
                }
            }
        }
        return -1;
    }

    private Rect getRoi(Bitmap image, int... colors) {
        return getRoi(1, image, colors);
    }

    private Rect getRoi(int findWidth, Bitmap image, int... colors) {
        for (int x = 0; x < colors.length; x++) {
            int color = colors[x];
            Rect rect = getRoi(findWidth, image, color, false);
            if (-1 != rect.x) {
                return rect;
            }
        }
        return new Rect(-1, -1, 0, 0);
    }


    private Rect getRoi(int findWidth, Bitmap image, int color, boolean printDetail) {
        int top = findColor(image, color, true, true, false, printDetail, findWidth);
        int bottom = findColor(image, color, true, false, false, printDetail, findWidth);
        int left = findColor(image, color, false, true, true, printDetail, findWidth);
        int right = findColor(image, color, false, true, false, printDetail, findWidth);
        return new Rect(left, top, right - left, bottom - top);
    }


    private boolean storeToPNG(Bitmap image, String filename) {
        try (FileOutputStream fos = new FileOutputStream(filename)) {
            image.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (IOException ex) {
            Log.e(TAG, ex.toString());
            return false;
        }
        return true;
    }


    private boolean landDetectToHUD(ArrayList<Boolean> laneDetectResult) {
        final int lanes = laneDetectResult.size();

            /*
                OuterRight(0x02), 6
                MiddleRight(0x04), 5,4
                InnerRight(0x08), 3,2
                InnerLeft(0x10),
                MiddleLeft(0x20),
                OuterLeft(0x40),

             */
        int x_start = 0;

        switch (lanes) {
            case 6:
            case 5:
                x_start = 0;
                break;
            case 4:
            case 3:
                x_start = 1;
                break;
            case 2:
                x_start = 2;
                break;
        }

        boolean hasDrivingLane = false;
        char nOutline = 0;
        char nArrow = 0;
        int now_lane_index = 0;
        for (int x = x_start; x < x_start + lanes; x++, now_lane_index++) {
            // the num in lane is from right to left, but laneDetectResult is from left to right, need handle it
            //final int currentLaneValue = 2 << x; //original 1
            //final int currentLaneValue = eLane.OuterLeft.value - (2 << x); //original 2
            final int currentLaneValue = eLane.OuterLeft.value >> x;

            nOutline += currentLaneValue;
            boolean drivingLane = laneDetectResult.get(now_lane_index);
            if (drivingLane) {
                nArrow += currentLaneValue;
                hasDrivingLane = true;
            }
        }
        hud.SetLanes(nArrow, nOutline);

        String msg = "";
        for (Boolean b : laneDetectResult) {
            msg += " " + (b ? "1" : "0");
        }
        msg = "lane detect: " + msg;
        postman.addStringExtra(activity.getString(R.string.notify_msg), msg);
        postman.sendIntent2MainActivity();

        msg = msg + " / " + (int) nArrow + "," + (int) nOutline;
        Log.i(TAG, msg);

        return hasDrivingLane;
    }

    private ArrayList<Integer> findLaneDivide(Bitmap lane, int y, int bgColor, int divideColor) {
        ArrayList<Integer> result = new ArrayList<Integer>();
        final int width = lane.getWidth();
        for (int x = 0; x < width - 6; x++) {
            int pixel0 = lane.getPixel(x, y);
            int pixel3 = lane.getPixel(x + 3, y);
            int pixel6 = lane.getPixel(x + 6, y);
            if (isSameRGB(pixel0, bgColor) &&
                    isSameRGB(pixel3, divideColor) &&
                    isSameRGB(pixel6, bgColor)) {
                result.add(x + 3);
                x += 6;
            }
        }
        return result;
    }

    private int getFirstVertical(Bitmap lane, int x0, int y0, int color,
                                 boolean notLogic, boolean inverse_scan) {
        final int height = lane.getHeight();
        int end = inverse_scan ? y0 : height;
        int h_step = inverse_scan ? -1 : 1;
        for (int h = inverse_scan ? height - 1 : y0; h != end; h += h_step) {
            final int pixel = lane.getPixel(x0, h);
            if (notLogic ? pixel != color : pixel == color) {
                return h;
            }
        }
        return -1;
    }

/*    private int getFirstHorizontal(Bitmap lane, int x0, int y0, int color,
                                   boolean notLogic, boolean inverse_scan) {
        final int width = lane.getWidth();
        int end = inverse_scan ? x0 : width;
        int w_step = inverse_scan ? -1 : 1;
        for (int w = inverse_scan ? width - 1 : x0; w != end; w += w_step) {
            final int pixel = lane.getPixel(w, y0);
            if (notLogic ? pixel != color : pixel == color) {
                return w;
            }
        }
        return -1;
    }*/

    private int getFirstHorizontal(Bitmap lane, int x0, int x1, int y0, int color,
                                   boolean notLogic, boolean inverse_scan) {
        int end = inverse_scan ? x0 : x1;
        int w_step = inverse_scan ? -1 : 1;
        for (int w = inverse_scan ? x1 : x0; w != end; w += w_step) {
            final int pixel = lane.getPixel(w, y0);
            if (notLogic ? pixel != color : pixel == color) {
                return w;
            }
        }
        return -1;
    }

    private static void copy(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src)) {
            try (OutputStream out = new FileOutputStream(dst)) {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }


}
