package com.example.notificationlistenerdemo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;
import chutka.bitman.com.speedometersimplified.LocationService;
import sky4s.garmin.GarminHUD;

//import android.app.NotificationChannel;

public class MainActivity extends Activity {
    //for test with virtual device which no BT device
    public static final boolean IGNORE_BT_DEVICE = false;

    private static final String TAG = "NLS";
    private static final String TAG_PRE = "[" + MainActivity.class.getSimpleName() + "] ";

    //    private static final int EVENT_SHOW_CREATE_NOS = 0;
    private static final int EVENT_LIST_CURRENT_NOS = 1;
    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";
    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";

    private boolean isEnabledNLS = false;
    private boolean showSpeed = false;
    public static boolean showETA = false;

    private TextView textViewDebug;
    private Switch switchHudConnected;
    private   Switch switchNotificationCatched;
    private   Switch switchGmapsNotificationCatched;

    private BluetoothSPP bt;
//    private static MainActivity selfActivity = null;

    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    private MsgReceiver msgReceiver;

    public class MsgReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            boolean catched = intent.getBooleanExtra(getString(R.string.notify_catched), false);
            switchNotificationCatched.setChecked(catched);

        }

    }

//    private Handler mHandler = new Handler() {
//        @Override
//        public void handleMessage(Message msg) {
//            switch (msg.what) {
////                case EVENT_SHOW_CREATE_NOS:
////                    showCreateNotification();
////                    break;
//                case EVENT_LIST_CURRENT_NOS:
//                    listCurrentNotification();
//                    break;
//
//                default:
//                    break;
//            }
//        }
//    };

    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textViewDebug = (TextView) findViewById(R.id.textViewDebug);
        switchHudConnected = (Switch) findViewById(R.id.switchHudConnected);
        switchNotificationCatched = (Switch) findViewById(R.id.switchNotificationCatched);
        switchGmapsNotificationCatched = (Switch) findViewById(R.id.switchGmapsNotificationCatched);

        startService(new Intent(this, NotificationCollectorMonitorService.class));

        sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        String bt_status = "";
        if (!IGNORE_BT_DEVICE) {
            bt = new BluetoothSPP(this);
            if (!bt.isBluetoothAvailable()) {
                Toast.makeText(getApplicationContext()
                        , "Bluetooth is not available"
                        , Toast.LENGTH_SHORT).show();
                finish();
            }

            String bt_bind_name = sharedPref.getString(getString(R.string.bt_bind_name_key), null);

            if (null != bt_bind_name) {
                if (!bt.isBluetoothEnabled()) {
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intent, BluetoothState.REQUEST_ENABLE_BT);
                } else {
                    if (!bt.isServiceAvailable()) {
                        bt.setupService();
                        bt.startService(BluetoothState.DEVICE_OTHER);
                        bt.autoConnect(bt_bind_name);
                    }
                }
            }

            bt.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {
                public void onDeviceDisconnected() {
                    switchHudConnected.setChecked(false);
                    NotificationMonitor.bt = null;
                    logNLS("onDeviceDisconnected");
                }

                public void onDeviceConnectionFailed() {
                    switchHudConnected.setChecked(false);
                    NotificationMonitor.bt = null;
                    logNLS("onDeviceConnectionFailed");
                }

                public void onDeviceConnected(String name, String address) {
                    switchHudConnected.setText("'" + name + "' connected");
                    switchHudConnected.setChecked(true);
                    NotificationMonitor.bt = bt;
                    logNLS("onDeviceConnected");

                    if (showSpeed && !locationServiceStatus)
                        bindService();

                    String connected_device_name = bt.getConnectedDeviceName();
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(getString(R.string.bt_bind_name_key), connected_device_name);
                    editor.commit();
                }
            });

            bt.setAutoConnectionListener(new BluetoothSPP.AutoConnectionListener() {
                public void onAutoConnectionStarted() {
                    int a = 1;
                }

                public void onNewConnection(String var1, String var2) {
                    int a = 1;
                }
            });

        } else {
            bt_status = "(BYPASS BT)";
        }

        int versionCode = BuildConfig.VERSION_CODE;
        String versionName = BuildConfig.VERSION_NAME;
        this.setTitle(this.getTitle() + " v" + versionName + " (build " + versionCode + ")" + bt_status);
        createNotification(this);

        msgReceiver = new MsgReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(getString(R.string.broadcast_receiver));
        registerReceiver(msgReceiver, intentFilter);

    }

    public void onDestroy() {
        super.onDestroy();
        if (!IGNORE_BT_DEVICE) {
            bt.stopAutoConnect();
            bt.stopService();
        }
        if (locationServiceStatus == true) {
            unbindService();
        }
        unregisterReceiver(msgReceiver);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (!IGNORE_BT_DEVICE) {
            if (!bt.isBluetoothEnabled()) {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, BluetoothState.REQUEST_ENABLE_BT);
            } else {
                if (!bt.isServiceAvailable()) {
                    bt.setupService();
                    bt.startService(BluetoothState.DEVICE_OTHER);
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
        textViewDebug.setTextColor(Color.BLACK);
        switch (view.getId()) {

            case R.id.btnListNotify:
                logNLS("List notifications...");
                listCurrentNotification();
                break;

            case R.id.btnScanBT:
                logNLS("Scan Bluetooth...");
                if (!IGNORE_BT_DEVICE) {
                    scanBluetooth();
                }
                break;

            case R.id.switchShowSpeed:
                if (((Switch) view).isChecked()) {
                    if (!checkLocationPermission()) {
                        ((Switch) view).setChecked(false);
                        break;
                    }
                    if (!checkGps()) {
                        ((Switch) view).setChecked(false);
                        break;
                    }
                    locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                    if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        return;
                    }
                    if (locationServiceStatus == false) {
                        //Here, the Location Service gets bound and the GPS Speedometer gets Active.
                        if (bt != null && NotificationMonitor.getGarminHud() != null)
                            bindService();
                        showSpeed = true;
                    }
                } else {
                    if (locationServiceStatus == true)
                        unbindService();
                    GarminHUD hud = NotificationMonitor.getGarminHud();
                    if (hud != null)
                        hud.ClearSpeedandWarning();
                    showSpeed = false;
                }
                break;
            case R.id.switchShowETA:
                if(((Switch) view).isChecked())
                    showETA = true;
                else
                    showETA = false;
                break;

//            case R.id.buttonRestartService:
//                restartNotificationListenerService(this);
//                break;

//            case R.id.tgBtnShowSpeed:
//
//                if (((ToggleButton) view).isChecked()) {
//                    if (!checkLocationPermission()) {
//                        ((ToggleButton) view).setChecked(false);
//                        break;
//                    }
//                    if (!checkGps()) {
//                        ((ToggleButton) view).setChecked(false);
//                        break;
//                    }
//                    locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
//                    if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
//                        return;
//                    }
//                    if (locationServiceStatus == false) {
//                        //Here, the Location Service gets bound and the GPS Speedometer gets Active.
//                        if (bt != null && NotificationMonitor.getGarminHud() != null)
//                            bindService();
//                        showSpeed = true;
//                    }
//                } else {
//                    if (locationServiceStatus == true)
//                        unbindService();
//                    GarminHUD hud = NotificationMonitor.getGarminHud();
//                    if (hud != null)
//                        hud.ClearSpeedandWarning();
//                    showSpeed = false;
//                }
//                break;
            default:
                break;
        }/**/
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


    private void restartNotificationListenerService(Context context) {

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
        //ignore OREO NotificationChannel, because GARMINuino no need this new feature.
        String channelID = Integer.toString(0x1234);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel channelHUD = new NotificationChannel(
//                    channelID,
//                    "GoogleMaps HUD",
//                    NotificationManager.IMPORTANCE_DEFAULT);
//            channelHUD.setDescription("GoogleMaps HUD");
//            channelHUD.enableLights(false);
//            channelHUD.enableVibration(false);
//            manager.createNotificationChannel(channelHUD);
//        }

        NotificationCompat.Builder builder =
//                new NotificationCompat.Builder(this, channelID)
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("GARMINuino")
                        .setContentText("is on working");

        Notification notification = builder.build();
        manager.notify(1, notification);

    }

    private String getCurrentNotificationString() {
        String listNos = "";
        StatusBarNotification[] currentNos = NotificationMonitor.getCurrentNotifications();
        if (currentNos != null) {
            for (int i = 0; i < currentNos.length; i++) {
                listNos += Integer.toString(i + 1) + " " + currentNos[i].getPackageName() + "\n";
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
                textViewDebug.setText(result);
                return;
            }
            int n = NotificationMonitor.mCurrentNotificationsCounts;
            if (n == 0) {
                result = getResources().getString(R.string.active_notification_count_zero);
            } else {
                result = String.format(getResources().getQuantityString(R.plurals.active_notification_count_nonzero, n, n));
            }
            result = result + "\n" + getCurrentNotificationString();
            textViewDebug.setText(result);
        } else {
            textViewDebug.setTextColor(Color.RED);
            textViewDebug.setText("Please Enable Notification Access");
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
        if (!IGNORE_BT_DEVICE) {
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
//    static long startTime, endTime;

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

    // bind/activate LocationService
    void bindService() {
        if (locationServiceStatus == true)
            return;
        Intent i = new Intent(getApplicationContext(), LocationService.class);
        bindService(i, sc, BIND_AUTO_CREATE);
        locationServiceStatus = true;
//        startTime = System.currentTimeMillis();
    }

    // unbind/deactivate LocationService
    void unbindService() {
        if (locationServiceStatus == false)
            return;
//        Intent i = new Intent(getApplicationContext(), LocationService.class);
        unbindService(sc);
        locationServiceStatus = false;
    }

    // This method check if GPS is activated (and ask user for activation)
    boolean checkGps() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showGPSDisabledAlertToUser();
            return false;
        }
        return true;
    }

    //This method configures the Alert Dialog box for GPS-Activation
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

    // Check permission for location (and ask user for permission) 
    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                new AlertDialog.Builder(this)
                        .setTitle("Location Permission")
                        .setMessage("For showing speed to Garmin HUD please enable Location Permission")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();


            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

}
