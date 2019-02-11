package sky4s.garmin;

import android.graphics.Bitmap;

public class ArrowImage {
    public static final int IMAGE_LEN = 4;
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

    public int getSAD(final int magicNumber) {
        int sad = 0;
        for (int x = 0; x < CONTENT_LEN; x++) {
            final boolean bit = 1 == ((magicNumber >> x) & 1);
            sad += content[x] != bit ? 1 : 0;
        }
        return sad;
    }
}
