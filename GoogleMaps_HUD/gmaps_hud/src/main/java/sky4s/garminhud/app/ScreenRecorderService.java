package sky4s.garminhud.app;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.IBinder;

public class ScreenRecorderService extends Service {
    MediaProjection mediaProjection;

    private static ScreenRecorderService screenRecorderService;

    public final static ScreenRecorderService getInstance() {
        if (null == screenRecorderService) {
            screenRecorderService = new ScreenRecorderService();
        }
        return screenRecorderService;
    }

    //    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
//            createNotificationChannel();
            //Android Q 存在兼容性問題
            MediaProjectionManager mMediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            mediaProjection = mMediaProjectionManager.getMediaProjection(Activity.RESULT_OK, (Intent) intent.getParcelableExtra("data"));





//            mMediaProjection = mMediaProjectionManager.getMediaProjection(mResultCode, Objects.requireNonNull(mResultData));


//            Intent captureIntent = mMediaProjectionManager.createScreenCaptureIntent();
//            startActivityForResult(captureIntent, 1);

//            ColorPickManager.getInstance().setMediaProjection(mediaProjection);
//            if (ColorPickManager.getInstance().getColorPickerDokitView() != null) {
//                ColorPickManager.getInstance().getColorPickerDokitView().onScreenServiceReady();
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return super.onStartCommand(intent, flags, startId);
    }


    //    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}