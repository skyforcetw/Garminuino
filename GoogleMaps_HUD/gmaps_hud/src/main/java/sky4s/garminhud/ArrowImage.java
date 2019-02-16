package sky4s.garminhud;

import android.graphics.Bitmap;

public class ArrowImage {
    public static final int IMAGE_LEN = 5; //4x4 cannot has valid recognize
    public static final int CONTENT_LEN = IMAGE_LEN * IMAGE_LEN;
    public boolean[] content = new boolean[CONTENT_LEN];

    public ArrowImage(Bitmap bitmap) {

        final int interval = bitmap.getWidth() / IMAGE_LEN;
        for (int h0 = 0; h0 < IMAGE_LEN; h0++) {
            final int h = h0 * interval;
            for (int w0 = 0; w0 < IMAGE_LEN; w0++) {
                final int w = w0 * interval;
                int p = bitmap.getPixel(w, h);
                final int alpha = (p >> 24) & 0xff;
                final int max = Math.max(Math.max(p & 0xff, (p >> 8) & 0xff), (p >> 16) & 0xff);
                final int max_alpha = (max * alpha) >> 8;
                content[h0 * IMAGE_LEN + w0] = max_alpha < 254;
            }
        }

    }

    // Returns the "Sum of absolute differences" (SAD) between the bitcode (magicNumber) and the ArrwowImage
    public int getSAD(final int magicNumber) {
        int sad = 0;
        for (int x = 0; x < CONTENT_LEN; x++) {
            final boolean bit = 1 == ((magicNumber >> x) & 1);
            sad += content[x] != bit ? 1 : 0;
        }
        return sad;
    }

    // Returns the bitcode of the ArrowImage. The image will be divided into IMAGE_LEN x IMAGE_LEN Pixels (5x5)
    // Return-Value can be used for Arrow.java
    public int getArrowValue() {
        int value = 0;
        for (int i=0; i < CONTENT_LEN; i++)
            value += ((content[i] ? 1:0)<<i);
        return value;
    }
}
