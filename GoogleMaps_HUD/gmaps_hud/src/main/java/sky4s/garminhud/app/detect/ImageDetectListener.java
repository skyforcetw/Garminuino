package sky4s.garminhud.app.detect;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.media.ImageReader;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import sky4s.garminhud.ImageUtils;
import sky4s.garminhud.app.MainActivity;
import sky4s.garminhud.app.R;

public class ImageDetectListener implements ImageReader.OnImageAvailableListener {
    private MainActivity activity;
    private static final String TAG = ImageDetectListener.class.getSimpleName();
    private GmapsScreenDetector gmapsDetector;
    private WazeScreenDetector wazeDetector;

    public ImageDetectListener(MainActivity activity) {
        gmapsDetector = new GmapsScreenDetector(activity);
        wazeDetector = new WazeScreenDetector(activity);
        this.activity = activity;

        if (null != activity) {
            Resources resource = activity.getResources();
            if (null != resource) {
                UPDATE_INTERVAL = resource.getInteger(R.integer.detect_update_interval);
            }
        }
    }

    public final static String PreImage = "myscreen_pre.png";
    public final static String NowImage = "myscreen_now.png";
    private static long lastUpdateTime = 0;
    private long UPDATE_INTERVAL = 1500;

    @Override
    public void onImageAvailable(ImageReader reader) {
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

                    File nowImage = new File(MainActivity.SCREENCAP_STORE_DIRECTORY + NowImage);
                    if (nowImage.exists()) {
                        File preImage = new File(MainActivity.SCREENCAP_STORE_DIRECTORY + PreImage);
                        copy(nowImage, preImage);
                    }

                    // write bitmap to a file
                    ImageUtils.storeBitmap(bitmap, MainActivity.SCREENCAP_STORE_DIRECTORY + NowImage);

                    //=================================
                    // for debug use
                    //=================================
                    final boolean loadBitmapFromFile = false;
                    if (loadBitmapFromFile) {
                        bitmap = BitmapFactory.decodeFile(MainActivity.SCREENCAP_STORE_DIRECTORY + "q.png");
                    }
                    //=================================

                    final boolean wazeDetection = false;
                    if (wazeDetection) {
                        wazeDetector.screenDetection(bitmap);
                    }

                    if (!activity.is_in_navigation) {
                        return;
                    }
                    gmapsDetector.screenDetection(bitmap);
//                    screenDetection(bitmap);
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


    public static void copy(File src, File dst) throws IOException {
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
