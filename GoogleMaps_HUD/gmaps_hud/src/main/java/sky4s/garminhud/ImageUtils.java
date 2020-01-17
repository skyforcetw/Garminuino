package sky4s.garminhud;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;

import java.io.FileOutputStream;
import java.io.IOException;

public class ImageUtils {
    public static int getGreenAlpha(int pixel) {
        final int alpha = (pixel >> 24) & 0xff;
        final int green = ((pixel >> 8) & 0xff);
        final int green_alpha = (green * alpha) >> 8;
        return green_alpha;
    }


    public static void toBinaryImage(Bitmap bitmap, int treatAsWhite) {
        for (int h = 0; h < bitmap.getHeight(); h++) {
            for (int w = 0; w < bitmap.getWidth(); w++) {
                int p = bitmap.getPixel(w, h);
                final int green_alpha = getGreenAlpha(p);
                bitmap.setPixel(w, h, green_alpha > treatAsWhite ? 0xffffffff : 0);
            }
        }

    }

    public static Bitmap removeAlpha(Bitmap originalBitmap) {
        // lets create a new empty bitmap
        Bitmap newBitmap = Bitmap.createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        // create a canvas where we can draw on
        Canvas canvas = new Canvas(newBitmap);
        // create a paint instance with alpha
        Paint alphaPaint = new Paint();
        alphaPaint.setAlpha(255);
        // now lets draw using alphaPaint instance
        canvas.drawBitmap(originalBitmap, 0, 0, alphaPaint);
        return newBitmap;
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (null == drawable) {
            return null;
        }
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static Context context;

    public static boolean storeBitmap(Bitmap image, String filename) {
        return storeBitmap(image, null, filename);
    }


    public static boolean storeBitmap(Bitmap bmp, String dirname, String filename) {

//        if (null == dirname) {
//            IMAGE_DIR = MainActivity.SCREENCAP_STORE_DIRECTORY;
//            return;
//        }
        if (null == context || ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            return false;
        }

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(null != dirname ? dirname + "/" + filename : filename);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
