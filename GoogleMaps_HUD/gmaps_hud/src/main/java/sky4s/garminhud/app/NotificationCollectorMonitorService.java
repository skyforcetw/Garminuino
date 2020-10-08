package sky4s.garminhud.app;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.List;

/**
 * Created by xinghui on 9/20/16.
 * <p>
 * calling this in your Application's onCreate
 * startService(new Intent(this, NotificationCollectorMonitorService.class));
 * <p>
 * BY THE WAY Don't Forget to Add the Service to the AndroidManifest.xml File.
 * <service android:name=".NotificationCollectorMonitorService"/>
 */
public class NotificationCollectorMonitorService extends Service {

    /**
     * {@link Log#isLoggable(String, int)}
     * <p>
     * IllegalArgumentException is thrown if the tag.length() > 23.
     */
    private static final String TAG = "NCMS";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate() called");
        ensureCollectorRunning();

        startNotification(null, null);
//        startForeground(1, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
    }

    private Notification notification;
    private NotificationManager mNotificationManager;

    private Notification getNormalNotification(String contentText, Bitmap icon) {
        final Intent mainIntent = MainActivity.sMainIntent;
        MainActivity.mNCMS = this;
        int flags = PendingIntent.FLAG_CANCEL_CURRENT; // ONE_SHOT：PendingIntent只使用一次；CANCEL_CURRENT：PendingIntent執行前會先結束掉之前的；NO_CREATE：沿用先前的PendingIntent，不建立新的PendingIntent；UPDATE_CURRENT：更新先前PendingIntent所帶的額外資料，並繼續沿用
        final PendingIntent pendingMainIntent = PendingIntent.getActivity(getApplicationContext(), 0, mainIntent, flags); // 取得PendingIntent


        final String channelID = "id";
/*
        notification
                = new NotificationCompat.Builder(this, channelID)
                .setSmallIcon(R.mipmap.ic_notification_foreground)
//                .setTicker("notification on status bar.") // 設置狀態列的顯示的資訊
                .setAutoCancel(false) // 設置通知被使用者點擊後是否清除  //notification.flags = Notification.FLAG_AUTO_CANCEL;
//                .setContentTitle(getString(R.string.app_name)) // 設置下拉清單裡的標題
                .setContentText(contentText)// 設置上下文內容
                .setOngoing(true)      //true使notification變為ongoing，用戶不能手動清除// notification.flags = Notification.FLAG_ONGOING_EVENT; notification.flags = Notification.FLAG_NO_CLEAR;
                .setContentIntent(pendingMainIntent)
//
//                .addAction(R.drawable.baseline_av_timer_24, getString(R.string.notify_switch_speed), switchSpeedPendingIntent)
//                .addAction(R.drawable.baseline_brightness_auto_24, "Auto Brightness", switchAutoBrightnessPendingIntent)
//                .addAction(R.drawable.baseline_drive_eta_24, getString(R.string.notify_switch_ETA), switchETAPendingIntent)
//                .addAction(R.drawable.baseline_access_time_24, "Current Time", switchTimePendingIntent)
//                .addAction(R.drawable.baseline_traffic_24, getString(R.string.notify_switch_detect), switchDetectPendingIntent)

                .setChannelId(channelID)
//                .setStyle(style)

                .build();*/


        NotificationCompat.Builder builder
                = new NotificationCompat.Builder(this, channelID)
                .setSmallIcon(R.mipmap.ic_notification_foreground)
                .setAutoCancel(false) // 設置通知被使用者點擊後是否清除  //notification.flags = Notification.FLAG_AUTO_CANCEL;
                .setContentText(contentText)// 設置上下文內容
                .setOngoing(true)      //true使notification變為ongoing，用戶不能手動清除// notification.flags = Notification.FLAG_ONGOING_EVENT; notification.flags = Notification.FLAG_NO_CLEAR;
                .setContentIntent(pendingMainIntent)
                .setChannelId(channelID);
//                .build();
        if (null != icon) {
            builder.setLargeIcon(icon);
        }
        notification = builder.build();
        return notification;
    }


    void startNotification(String contentText, Bitmap icon) {
//        log("startNotification");
        //Step1. 初始化NotificationManager，取得Notification服務
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notification = getNormalNotification(null == contentText ? "GMaps Notify Monitor Service" : contentText, icon);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final String channelID = "id";
            NotificationChannel channel = new NotificationChannel(
                    channelID,
                    "Notify Monitor",
                    NotificationManager.IMPORTANCE_MAX);
            channel.enableLights(false);
            //it had a bug which is vibration cannot be disabled normally.
            channel.setVibrationPattern(new long[]{0});
            channel.enableVibration(true);

            mNotificationManager.createNotificationChannel(channel);
        } else {
            notification.vibrate = new long[]{0};
        }

        // 把指定ID的通知持久的發送到狀態條上.
//        mNotificationManager.notify(R.integer.notify_id, notification);
        startForeground(1, notification);
    }

    private void stopNotification() {
//        log("stopNotification");

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void ensureCollectorRunning() {
        ComponentName collectorComponent = new ComponentName(this, /*NotificationListenerService Inheritance*/ NotificationMonitor.class);
        Log.v(TAG, "ensureCollectorRunning collectorComponent: " + collectorComponent);
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        boolean collectorRunning = false;
        List<ActivityManager.RunningServiceInfo> runningServices = manager.getRunningServices(Integer.MAX_VALUE);
        if (runningServices == null) {
            Log.w(TAG, "ensureCollectorRunning() runningServices is NULL");
            return;
        }
        for (ActivityManager.RunningServiceInfo service : runningServices) {
            if (service.service.equals(collectorComponent)) {
                Log.w(TAG, "ensureCollectorRunning service - pid: " + service.pid + ", currentPID: " + Process.myPid() + ", clientPackage: " + service.clientPackage + ", clientCount: " + service.clientCount
                        + ", clientLabel: " + ((service.clientLabel == 0) ? "0" : "(" + getResources().getString(service.clientLabel) + ")"));
                if (service.pid == Process.myPid() /*&& service.clientCount > 0 && !TextUtils.isEmpty(service.clientPackage)*/) {
                    collectorRunning = true;
                }
            }
        }
        if (collectorRunning) {
            Log.d(TAG, "ensureCollectorRunning: collector is running");
            return;
        }
        Log.d(TAG, "ensureCollectorRunning: collector not running, reviving...");
        toggleNotificationListenerService();
    }

    private void toggleNotificationListenerService() {
        Log.d(TAG, "toggleNotificationListenerService() called");
        ComponentName thisComponent = new ComponentName(this, /*getClass()*/ NotificationMonitor.class);
        PackageManager pm = getPackageManager();
        pm.setComponentEnabledSetting(thisComponent, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(thisComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

