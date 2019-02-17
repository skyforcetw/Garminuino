package sky4s.garminhud;

import android.graphics.Bitmap;

public class ArrowImage {
    public static final int IMAGE_LEN = 8; //4x4 cannot has valid recognize
    public static final int CONTENT_LEN = IMAGE_LEN * IMAGE_LEN;

    public boolean[] content = new boolean[CONTENT_LEN];
    public long value;
    public Bitmap binaryImage;

    private int getGreenAlpha(int pixel) {
        final int alpha = (pixel >> 24) & 0xff;
        final int green = ((pixel >> 8) & 0xff);
        final int green_alpha = (green * alpha) >> 8;
        return green_alpha;
    }

    public ArrowImage(Bitmap bitmap) {

        for (int h = 0; h < bitmap.getHeight(); h++) {
            for (int w = 0; w < bitmap.getWidth(); w++) {
                int p = bitmap.getPixel(w, h);
                final int green_alpha = getGreenAlpha(p);
                bitmap.setPixel(w, h,green_alpha>200 ? 0xffffffff : 0);
            }
        }
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, 132, 132, false);
        binaryImage = resized;

        final int interval = resized.getWidth() / IMAGE_LEN;
        final int offset = interval / 2;
        int index = 0;
        for (int h0 = 0; h0 < IMAGE_LEN; h0++) {
            final int h = h0 * interval + offset;
            for (int w0 = 0; w0 < IMAGE_LEN; w0++) {
                final int w = w0 * interval + offset;
                int p = resized.getPixel(w, h);
                final int green_alpha = getGreenAlpha(p);
                boolean bit = green_alpha>=254;

                content[h0 * IMAGE_LEN + w0] = bit;
                long shift_value = ((bit ? 1L : 0L) << index);
                value = value | shift_value;
                index++;
            }
        }

    }

    public long getSAD(final long magicNumber) {
        long sad = 0;
        int length = 8 == IMAGE_LEN ? CONTENT_LEN - 1 : CONTENT_LEN;
        for (int x = 0; x < length; x++) {
            final boolean bit = 1 == ((magicNumber >> x) & 1);
            sad += content[x] != bit ? 1 : 0;
        }
        return sad;
    }
}
