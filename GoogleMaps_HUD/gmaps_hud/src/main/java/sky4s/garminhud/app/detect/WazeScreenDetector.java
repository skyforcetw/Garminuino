package sky4s.garminhud.app.detect;

import android.graphics.Bitmap;
import android.view.Window;

import com.googlecode.tesseract.android.TessBaseAPI;

import sky4s.garminhud.ImageUtils;
import sky4s.garminhud.app.MainActivity;

public class WazeScreenDetector extends ScreenDetector {

    WazeScreenDetector(MainActivity activity) {
        super(activity);
    }

    private int getStatusBarHeight() {
        android.graphics.Rect rectangle = new android.graphics.Rect();
        Window window = activity.getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
        int statusBarHeight = rectangle.top;
//        int contentViewTop =
//                window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
//        int titleBarHeight= contentViewTop - statusBarHeight;
        return statusBarHeight;//titleBarHeight;
    }

    Bitmap[] divideRoadBar(Bitmap roadBar) {
        int width = roadBar.getWidth();
        int height = roadBar.getHeight();

        Bitmap arrow_img = Bitmap.createBitmap(roadBar, 0, 0, height, height);
        Bitmap distance_img = Bitmap.createBitmap(roadBar, height, 0, width - height, height >> 1);
        Bitmap road_img = Bitmap.createBitmap(roadBar, height, height >> 1, width - height, height >> 1);

        return new Bitmap[]{arrow_img, distance_img, road_img};
    }

    Bitmap[] divideTimeBar(Bitmap timeBar) {
        int width = timeBar.getWidth();
        int height = timeBar.getHeight();
        int half_height = height >> 1;
        int double_height = height << 1;

        Bitmap eta = Bitmap.createBitmap(timeBar, height, 0, width - double_height, half_height);
        Bitmap distance = Bitmap.createBitmap(timeBar, height, half_height, width - double_height, half_height);

        return new Bitmap[]{eta, distance};
    }

    static final String TESSBASE_PATH = activity.OCR_STORE_DIRECTORY;
    static final String DEFAULT_LANGUAGE = "eng";
    static final String CHINESE_LANGUAGE = "chi_tra";

    private String ocrWithChinese(Bitmap bitmap) {
        final TessBaseAPI ocrApi = new TessBaseAPI();

        ocrApi.init(TESSBASE_PATH, CHINESE_LANGUAGE);
        ocrApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_LINE);

        ocrApi.setImage(bitmap);
        String resString = ocrApi.getUTF8Text();

        ocrApi.clear();
        ocrApi.end();
        return resString;
    }

    private String ocr(Bitmap bitmap, String language) {
        final TessBaseAPI ocrApi = new TessBaseAPI();

        ocrApi.init(TESSBASE_PATH, language);
        ocrApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_LINE);

        ocrApi.setImage(bitmap);
        String resString = ocrApi.getUTF8Text();

        ocrApi.clear();
        ocrApi.end();
        return resString;
    }


    Bitmap preProcessImage(Bitmap bitmap) {
        bitmap = ImageUtils.removeAlpha(bitmap);
        ImageUtils.toBinaryImage(bitmap, 200);

        return bitmap;
    }

    void screenDetection(Bitmap screen) {

        try {
            if (null == screen) {
                return;
            }
            int screen_width = screen.getWidth();
            int screen_height = screen.getHeight();
            int status_bar_height = getStatusBarHeight();
            int app_height = screen_height - status_bar_height;

            Bitmap eighth_screen_img = Bitmap.createBitmap(screen, 0, status_bar_height, screen.getWidth(), app_height >> 3);
            ImageUtils.storeBitmap(eighth_screen_img, MainActivity.SCREENCAP_STORE_DIRECTORY + "8_up.png");

            Bitmap[] road_images = divideRoadBar(eighth_screen_img);
            Bitmap distance_image = preProcessImage(road_images[1]);
            Bitmap roadname_image = preProcessImage(road_images[2]);

            String distance = ocrWithChinese(distance_image);
            String roadName = ocrWithChinese(roadname_image);

            Bitmap eighth_dw_screen_img = Bitmap.createBitmap(screen, 0, screen_height - (app_height / 10), screen.getWidth(), app_height / 10);
            ImageUtils.storeBitmap(eighth_dw_screen_img, MainActivity.SCREENCAP_STORE_DIRECTORY + "8_dw.png");

            Bitmap[] time_images = divideTimeBar(eighth_dw_screen_img);
            Bitmap eta_image = preProcessImage(time_images[0]);
            Bitmap time_image = preProcessImage(time_images[1]);

            String eta = ocr(eta_image,DEFAULT_LANGUAGE);
            String time = ocrWithChinese(time_image);

            ImageUtils.storeBitmap(distance_image, MainActivity.SCREENCAP_STORE_DIRECTORY + "distance.png");
            ImageUtils.storeBitmap(roadname_image, MainActivity.SCREENCAP_STORE_DIRECTORY + "road.png");
            ImageUtils.storeBitmap(eta_image, MainActivity.SCREENCAP_STORE_DIRECTORY + "eta.png");
            ImageUtils.storeBitmap(time_image, MainActivity.SCREENCAP_STORE_DIRECTORY + "time.png");

            int a = 1;

        } finally {


        }
    }
}
