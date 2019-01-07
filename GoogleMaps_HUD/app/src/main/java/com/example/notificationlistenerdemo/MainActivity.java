package com.example.notificationlistenerdemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;
import chutka.bitman.com.speedometersimplified.LocationService;
import sky4s.garmin.GarminHUD;
import sky4s.garmin.eOutAngle;
import sky4s.garmin.eUnits;

//import com.google.android.gms.ads.MobileAds;
//import com.google.android.gms.ads.AdRequest;
//import com.google.android.gms.ads.AdView;


public class MainActivity extends Activity {
    public static final boolean IGNORE_BT = false;

    private static final String TAG = "NLS";
    private static final String TAG_PRE = "[" + MainActivity.class.getSimpleName() + "] ";

    private static final int EVENT_SHOW_CREATE_NOS = 0;
    private static final int EVENT_LIST_CURRENT_NOS = 1;
    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";
    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";

    private boolean isEnabledNLS = false;
    private TextView mTextView;
    private BluetoothSPP bt;

    private static MainActivity selfActivity = null;

    static void updateMessage(String msg) {
        if (null != selfActivity) {
            selfActivity.mTextView.setText(msg);
//            selfActivity.findViewById()
        }
    }

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

    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextView = (TextView) findViewById(R.id.textView);

        int versionCode = BuildConfig.VERSION_CODE;
        String versionName = BuildConfig.VERSION_NAME;
        this.setTitle(this.getTitle() + " (build " + versionCode + ")");

        startService(new Intent(this, NotificationCollectorMonitorService.class));

        sharedPref = this.getPreferences(Context.MODE_PRIVATE);

        if (!IGNORE_BT) {
            bt = new BluetoothSPP(this);
            if (!bt.isBluetoothAvailable()) {
                Toast.makeText(getApplicationContext()
                        , "Bluetooth is not available"
                        , Toast.LENGTH_SHORT).show();
                finish();
                //return;
            }

//
            /*String bt_bind_name = sharedPref.getString(getString(R.string.bt_bind_name_key), null);
            if (null != bt_bind_name) {
                bt.autoConnect(bt_bind_name);
            }*/

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

                    String connected_device_name = bt.getConnectedDeviceName();
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(getString(R.string.bt_bind_name_key), connected_device_name);
                    editor.commit();
                }
            });


        }

        createNotification(this);
        selfActivity = this;
    }

    public void onDestroy() {
        super.onDestroy();
        if (!IGNORE_BT) {
            bt.stopService();
        }
        if (locationServiceStatus == true) {
            unbindService();
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
//            case R.id.btnCreateNotify:
//                logNLS("Create notifications...");
//                createNotification(this);
//                mHandler.sendMessageDelayed(mHandler.obtainMessage(EVENT_SHOW_CREATE_NOS), 50);
//                break;
//            case R.id.btnClearLastNotify:
//                logNLS("Clear Last notification...");
//                clearLastNotification();
//                mHandler.sendMessageDelayed(mHandler.obtainMessage(EVENT_LIST_CURRENT_NOS), 50);
//                break;
//            case R.id.btnClearAllNotify:
//                logNLS("Clear All notifications...");
//                clearAllNotifications();
//                mHandler.sendMessageDelayed(mHandler.obtainMessage(EVENT_LIST_CURRENT_NOS), 50);
//                break;
            case R.id.btnListNotify:
                logNLS("List notifications...");
                listCurrentNotification();
                break;
//            case R.id.btnEnableUnEnableNotify:
//                logNLS("Enable/UnEnable notification...");
//                openNotificationAccess();
//                break;
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

            case R.id.tgBtnShowSpeed:
                if (((ToggleButton) view).isChecked()) {
                    checkGps();
                    locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                    if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        return;
                    }
                    if (locationServiceStatus == false)
                        //Here, the Location Service gets bound and the GPS Speedometer gets Active.
                        bindService();
                } else {
                    if (locationServiceStatus == true)
                        unbindService();
                }


                break;
            default:
                break;
        }/**/
    }

    private void sendToGarminHUD() {
        if (null != bt) {
            GarminHUD garminHud = new GarminHUD(bt);
            garminHud.SetDistance(100, eUnits.Metres);
            garminHud.SetSpeedWarning(100, 100);
            garminHud.SetDirection(eOutAngle.Right);
        }
    }

    private void scanBluetooth() {

        if (!bt.isBluetoothAvailable()) {
            Toast.makeText(getApplicationContext()
                    , "Bluetooth is not available"
                    , Toast.LENGTH_SHORT).show();
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
        ncBuilder.setContentTitle("GoogleMaps HUD");
        ncBuilder.setContentText("GoogleMaps HUD");
//        ncBuilder.setTicker("GoogleMaps HUD");
        ncBuilder.setSmallIcon(R.mipmap.ic_launcher);
        ncBuilder.setAutoCancel(false);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, this.getIntent(), PendingIntent.FLAG_UPDATE_CURRENT);
        ncBuilder.setContentIntent(pendingIntent);

        manager.notify((int) System.currentTimeMillis(), ncBuilder.build());
    }

    private String getCurrentNotificationString() {
        String listNos = "";
        StatusBarNotification[] currentNos = NotificationMonitor.getCurrentNotifications();
        if (currentNos != null) {
            for (int i = 0; i < currentNos.length; i++) {
                listNos = Integer.toString(i + 1) + " " + currentNos[i].getPackageName() + "\n" + listNos;
            }
        }
        return listNos;
    }


    private void listCurrentNotification() {
        String result = "";
        if (isEnabledNLS) {
            if (NotificationMonitor.getCurrentNotifications() == null) {
                result = "No Notifications Capture!!!\nSometimes reboot device or re-install app can resolve this problem.";
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

    static boolean locationServiceStatus;
    LocationService locationService;
    LocationManager locationManager;
    static long startTime, endTime;

    private ServiceConnection sc = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationService.LocalBinder binder = (LocationService.LocalBinder) service;
            locationService = binder.getService();
            locationServiceStatus = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            locationServiceStatus = false;
        }
    };

    void bindService() {
        if (locationServiceStatus == true)
            return;
        Intent i = new Intent(getApplicationContext(), LocationService.class);
        bindService(i, sc, BIND_AUTO_CREATE);
        locationServiceStatus = true;
        startTime = System.currentTimeMillis();
    }

    void unbindService() {
        if (locationServiceStatus == false)
            return;
        Intent i = new Intent(getApplicationContext(), LocationService.class);
        unbindService(sc);
        locationServiceStatus = false;
    }

    //This method leads you to the alert dialog box.
    void checkGps() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {


            showGPSDisabledAlertToUser();
        }
    }

    //This method configures the Alert Dialog box.
    private void showGPSDisabledAlertToUser() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("Enable GPS to use application")
                .setCancelable(false)
                .setPositiveButton("Enable GPS",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent callGPSSettingIntent = new Intent(
                                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivity(callGPSSettingIntent);
                            }
                        });
        alertDialogBuilder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

}
