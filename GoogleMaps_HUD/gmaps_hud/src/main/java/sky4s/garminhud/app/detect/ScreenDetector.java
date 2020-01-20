package sky4s.garminhud.app.detect;


import android.graphics.Bitmap;
import android.graphics.Color;

import sky4s.garminhud.app.MainActivity;
import sky4s.garminhud.app.MainActivityPostman;
import sky4s.garminhud.app.R;
import sky4s.garminhud.hud.HUDInterface;

class Rect {
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

public abstract class ScreenDetector {
    protected static MainActivity activity;
    protected static MainActivityPostman postman;
    protected static HUDInterface hud;
    private int[] pixelsInFindColor;

    ScreenDetector(MainActivity _activity) {
        activity = _activity;
        hud = activity.hud;
        if (null == postman) {
            postman = MainActivityPostman.toMainActivityInstance(activity, activity.getString(R.string.broadcast_sender_image_detect));
        }
    }

    abstract void screenDetection(Bitmap screen);


    protected boolean isSameRGB(int color1, int color2) {
        return Color.red(color1) == Color.red(color2) &&
                Color.green(color1) == Color.green(color2) &&
                Color.blue(color1) == Color.blue(color2);
    }


    protected boolean isSameRGB(int color1, int color2, int tolerance) {
        boolean same = Color.red(color1) == Color.red(color2) &&
                Color.green(color1) == Color.green(color2) &&
                Color.blue(color1) == Color.blue(color2);

        int deltaR = Math.abs(Color.red(color1) - Color.red(color2));
        int deltaG = Math.abs(Color.green(color1) - Color.green(color2));
        int deltaB = Math.abs(Color.blue(color1) - Color.blue(color2));
        boolean similarColor = deltaR <= tolerance && deltaG <= tolerance && deltaB <= tolerance;

        return same || similarColor;
    }

    protected int findColor(Bitmap image, int color, boolean vertical, boolean up, boolean left, boolean printDetail, int findWidth) {
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
                        final int tolerance = 1;
                        allSameColor = allSameColor && isSameRGB(pixel, color, tolerance);
                    }

                    boolean sameColor = allSameColor;

                    if (sameColor) {
                        if (vertical) {
                            if (printDetail) {
//                                Log.i(TAG, "vertical: " + h + "," + w);
                            }
                            return h;
                        } else {
                            if (printDetail) {
//                                Log.i(TAG, "horizontal: " + h + "," + w);
                            }
                            return w;
                        }
                    }
                }
            }
        }
        return -1;
    }

    protected Rect getRoi(Bitmap image, int... colors) {
        return getRoi(1, image, colors);
    }

    protected Rect getRoi(int findWidth, Bitmap image, int... colors) {
        for (int x = 0; x < colors.length; x++) {
            int color = colors[x];
            Rect rect = getRoi(findWidth, image, color, false);
            if (-1 != rect.x) {
                return rect;
            }
        }
        return new Rect(-1, -1, 0, 0);
    }


    protected Rect getRoi(int findWidth, Bitmap image, int color, boolean printDetail) {
        int top = findColor(image, color, true, true, false, printDetail, findWidth);
        int bottom = findColor(image, color, true, false, false, printDetail, findWidth);
        int left = findColor(image, color, false, true, true, printDetail, findWidth);
        int right = findColor(image, color, false, true, false, printDetail, findWidth);
        return new Rect(left, top, right - left, bottom - top);
    }
}
