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

    private static final int TREAT_AS_WHITE = 200;
    private static final int ALPHA_AS_WHITE = 254;
    private static final int STANDARD_IMG_SIZE = 132;

    private void toBinaryImage(Bitmap bitmap) {
        for (int h = 0; h < bitmap.getHeight(); h++) {
            for (int w = 0; w < bitmap.getWidth(); w++) {
                int p = bitmap.getPixel(w, h);
                final int green_alpha = getGreenAlpha(p);
                bitmap.setPixel(w, h, green_alpha > TREAT_AS_WHITE ? 0xffffffff : 0);
            }
        }

    }

    private Bitmap resizeImage(Bitmap bitmap, int newLength) {
        if (bitmap.getWidth() == newLength && bitmap.getHeight() == newLength) {
            return bitmap;
        }
        return Bitmap.createScaledBitmap(bitmap, newLength, newLength, false);

    }


    public ArrowImage(Bitmap bitmap) {
        toBinaryImage(bitmap);
        Bitmap resized = resizeImage(bitmap, STANDARD_IMG_SIZE);

        binaryImage = resized;

        final int interval = resized.getWidth() / IMAGE_LEN;
        int index = 0;

        for (int h0 = 0; h0 < IMAGE_LEN; h0++) {
            final int h = h0 * interval;
            for (int w0 = 0; w0 < IMAGE_LEN; w0++) {
                final int w = w0 * interval;
                int p = resized.getPixel(w, h);
                final int green_alpha = getGreenAlpha(p);
                boolean bit = green_alpha >= ALPHA_AS_WHITE;

                content[h0 * IMAGE_LEN + w0] = bit;
                long shift_value = ((bit ? 1L : 0L) << index);
                value = value | shift_value;
                index++;
            }
        }

    }


    public long getSAD(final long magicNumber) {
        long sad = 0;
        int length = 8 == IMAGE_LEN ? CONTENT_LEN - 1 : CONTENT_LEN; //when 8x8, just check 63bit, skip sign bit
        for (int x = 0; x < length; x++) {
            final boolean bit = 1 == ((magicNumber >> x) & 1);
            sad += content[x] != bit ? 1 : 0;
        }
        return sad;
    }

    // Returns the bitcode of the ArrowImage. The image will be divided into IMAGE_LEN x IMAGE_LEN Pixels (8x8)
    // Return-Value can be used for Arrow.java
    public long getArrowValue() {
        return value;
    }
}
