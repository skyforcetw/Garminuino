package com.example.notificationlistenerdemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;


public class MainActivity extends Activity {
   public static final boolean IGNORE_BT = false;

    private static final String TAG = "SevenNLS";
    private static final String TAG_PRE = "[" + MainActivity.class.getSimpleName() + "] ";
    private static final int EVENT_SHOW_CREATE_NOS = 0;
    private static final int EVENT_LIST_CURRENT_NOS = 1;
    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";
    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
    private boolean isEnabledNLS = false;
    private TextView mTextView;
    private BluetoothSPP bt;


    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_SHOW_CREATE_NOS:
                    showCreateNotification();
                    break;
                case EVENT_LIST_CURRENT_NOS:
                    listCurrentNotification();
                    break;

                default:
                    break;
            }
        }
    };

    private AdView mAdView;


    @Override
    protected void  onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextView = (TextView) findViewById(R.id.textView);


        int versionCode = BuildConfig.VERSION_CODE;
        String versionName = BuildConfig.VERSION_NAME;

        this.setTitle(this.getTitle() + " (build " + versionCode + ")");

        startService(new Intent(this, NotificationCollectorMonitorService.class));

        if (!IGNORE_BT) {
            bt = new BluetoothSPP(this);
            if (!bt.isBluetoothAvailable()) {
                Toast.makeText(getApplicationContext()
                        , "Bluetooth is not available"
                        , Toast.LENGTH_SHORT).show();
                finish();
                //return;
            }

            bt.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {
                public void onDeviceDisconnected() {
                    mTextView.setText("Status : Not connect");
                    NotificationMonitor.bt = null;
                    logNLS("onDeviceDisconnected");
                }

                public void onDeviceConnectionFailed() {
                    mTextView.setText("Status : Connection failed");
                    NotificationMonitor.bt = null;
                    logNLS("onDeviceConnectionFailed");
                }

                public void onDeviceConnected(String name, String address) {
                    mTextView.setText("Status : Connected to " + name);
                    NotificationMonitor.bt = bt;
                    logNLS("onDeviceConnected");
                }
            });

//            bt.setAutoConnectionListener(new BluetoothSPP.AutoConnectionListener() {
//                public void onNewConnection(String name, String address) {
//                    mTextView.setText("Status : onNewConnection " + name);
//                    NotificationMonitor.bt = bt;
//                    logNLS("onNewConnection");
//                }
//
//                public void onAutoConnectionStarted() {
//                    NotificationMonitor.bt = bt;
//                    logNLS("onAutoConnectionStarted");
//                }
//            });

        }

//        AdView mAdView = (AdView) findViewById(R.id.adView);
//        AdRequest adRequest = new AdRequest.Builder().build();
//        mAdView.loadAd(adRequest);
        // Sample AdMob app ID: ca-app-pub-3940256099942544~3347511713

//        MobileAds.initialize(this, "ca-app-pub-3940256099942544~3347511713")
        String bannerAppID = getResources().getString(R.string.banner_ad_app_id);
        MobileAds.initialize(this, bannerAppID);
        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    public void onDestroy() {
        super.onDestroy();
        if (!IGNORE_BT) {
            bt.stopService();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (!IGNORE_BT) {
            if (!bt.isBluetoothEnabled()) {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, BluetoothState.REQUEST_ENABLE_BT);
            } else {
                if (!bt.isServiceAvailable()) {
                    bt.setupService();
                    bt.startService(BluetoothState.DEVICE_OTHER);
//                    bt.autoConnect("GARMIN HUD+");
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isEnabledNLS = isEnabled();
        logNLS("isEnabledNLS = " + isEnabledNLS);
        if (!isEnabledNLS) {
            showConfirmDialog();
        }
    }

    public void buttonOnClicked(View view) {
        mTextView.setTextColor(Color.BLACK);
        switch (view.getId()) {
            case R.id.btnCreateNotify:
                logNLS("Create notifications...");
                createNotification(this);
                mHandler.sendMessageDelayed(mHandler.obtainMessage(EVENT_SHOW_CREATE_NOS), 50);
                break;
            case R.id.btnClearLastNotify:
                logNLS("Clear Last notification...");
                clearLastNotification();
                mHandler.sendMessageDelayed(mHandler.obtainMessage(EVENT_LIST_CURRENT_NOS), 50);
                break;
            case R.id.btnClearAllNotify:
                logNLS("Clear All notifications...");
                clearAllNotifications();
                mHandler.sendMessageDelayed(mHandler.obtainMessage(EVENT_LIST_CURRENT_NOS), 50);
                break;
            case R.id.btnListNotify:
                logNLS("List notifications...");
                listCurrentNotification();
                break;
            case R.id.btnEnableUnEnableNotify:
                logNLS("Enable/UnEnable notification...");
                openNotificationAccess();
                break;
            case R.id.btnToggle:
                logNLS("Toogle service...");
                toggleNotificationListenerService(this);
                break;
            case R.id.btnScanBT:
                logNLS("Scan Bluetooth...");
                if (!IGNORE_BT) {
                    scanBluetooth();
                }
                break;
            default:
                break;
        }
    }

    private void scanBluetooth() {

        if (!bt.isBluetoothAvailable()) {
            Toast.makeText(getApplicationContext()
                    , "Bluetooth is not available"
                    , Toast.LENGTH_SHORT).show();
//            finish();
        } else {
//            bt.startService(BluetoothState.DEVICE_OTHER);
            bt.setDeviceTarget(BluetoothState.DEVICE_OTHER);

            Intent intent = new Intent(getApplicationContext(), DeviceList.class);
            startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);

        }
    }


    private void toggleNotificationListenerService(Context context) {

        //worked!
        //NotificationMonitor or  NotificationCollectorMonitorService??
        stopService(new Intent(this, NotificationMonitor.class));
        startService(new Intent(this, NotificationMonitor.class));

        PackageManager pm = getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName(this, NotificationMonitor.class),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(new ComponentName(this, NotificationMonitor.class),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        //useless
//        PackageManager pm = getPackageManager();
//
//        pm.setComponentEnabledSetting(
//                new ComponentName(context, NotificationCollectorMonitorService.class),
//                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
//
//        pm.setComponentEnabledSetting(
//                new ComponentName(context, NotificationCollectorMonitorService.class),
//                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }

    private boolean isEnabled() {
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(),
                ENABLED_NOTIFICATION_LISTENERS);
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (int i = 0; i < names.length; i++) {
                final ComponentName cn = ComponentName.unflattenFromString(names[i]);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void createNotification(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder ncBuilder = new NotificationCompat.Builder(context);
        ncBuilder.setContentTitle("My Notification");
        ncBuilder.setContentText("Notification Listener Service Example");
        ncBuilder.setTicker("Notification Listener Service Example");
        ncBuilder.setSmallIcon(R.mipmap.ic_launcher);
        ncBuilder.setAutoCancel(true);
        manager.notify((int) System.currentTimeMillis(), ncBuilder.build());
    }

    private void cancelNotification(Context context, boolean isCancelAll) {
        Intent intent = new Intent();
        intent.setAction(NotificationMonitor.ACTION_NLS_CONTROL);
        if (isCancelAll) {
            intent.putExtra("command", "cancel_all");
        } else {
            intent.putExtra("command", "cancel_last");
        }
        context.sendBroadcast(intent);
    }

    private String getCurrentNotificationString() {
        String listNos = "";
        StatusBarNotification[] currentNos = NotificationMonitor.getCurrentNotifications();
        if (currentNos != null) {
            for (int i = 0; i < currentNos.length; i++) {
                listNos = i + " " + currentNos[i].getPackageName() + "\n" + listNos;
            }
        }
        return listNos;
    }

    private void listCurrentNotification() {
//        GarminHUD garminHud = new GarminHUD(bt);
//        garminHud.SetDirection(eOutAngle.Right, eOutType.RightRoundabout, eOutAngle.AsDirection);
////        garminHud.SetDirection(eOutAngle.AsDirection);
//        garminHud.SetDirection((char) eOutAngle.Straight.value,
//                (char) (eOutType.LongerLane.value),
//                (char) eOutAngle.Straight.value);

        String result = "";
        if (isEnabledNLS) {
            if (NotificationMonitor.getCurrentNotifications() == null) {
                result = "mCurrentNotifications.get(0) is null";
                logNLS(result);
                mTextView.setText(result);
                return;
            }
            int n = NotificationMonitor.mCurrentNotificationsCounts;
            if (n == 0) {
                result = getResources().getString(R.string.active_notification_count_zero);
            } else {
                result = String.format(getResources().getQuantityString(R.plurals.active_notification_count_nonzero, n, n));
            }
            result = result + "\n" + getCurrentNotificationString();
            mTextView.setText(result);
        } else {
            mTextView.setTextColor(Color.RED);
            mTextView.setText("Please Enable Notification Access");
        }
    }

    private void clearLastNotification() {
        if (isEnabledNLS) {
            cancelNotification(this, false);
        } else {
            mTextView.setTextColor(Color.RED);
            mTextView.setText("Please Enable Notification Access");
        }
    }

    private void clearAllNotifications() {
        if (isEnabledNLS) {
            cancelNotification(this, true);
        } else {
            mTextView.setTextColor(Color.RED);
            mTextView.setText("Please Enable Notification Access");
        }
    }

    private void showCreateNotification() {
        if (NotificationMonitor.mPostedNotification != null) {
            String result = NotificationMonitor.mPostedNotification.getPackageName() + "\n"
                    + NotificationMonitor.mPostedNotification.getTag() + "\n"
                    + NotificationMonitor.mPostedNotification.getId() + "\n" + "\n"
                    + mTextView.getText();
            result = "Create notification:" + "\n" + result;
            mTextView.setText(result);
        }
    }

    private void openNotificationAccess() {
        startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS));
    }

    private void showConfirmDialog() {
        new AlertDialog.Builder(this)
                .setMessage("Please enable NotificationMonitor access")
                .setTitle("Notification Access")
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                openNotificationAccess();
                            }
                        })
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // do nothing
                            }
                        })
                .create().show();
    }

    private void logNLS(Object object) {
        Log.i(TAG, TAG_PRE + object);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!IGNORE_BT) {
            if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
                if (resultCode == Activity.RESULT_OK)
                    bt.connect(data);
            } else if (requestCode == BluetoothState.REQUEST_ENABLE_BT) {
                if (resultCode == Activity.RESULT_OK) {
                    bt.setupService();
                    bt.startService(BluetoothState.DEVICE_OTHER);
//                setup();
                } else {
                    Toast.makeText(getApplicationContext()
                            , "Bluetooth was not enabled."
                            , Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }
}
