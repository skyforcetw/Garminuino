package sky4s.garminhud.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.location.LocationManager;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;
import chutka.bitman.com.speedometersimplified.LocationService;
import sky4s.garminhud.Arrow;
import sky4s.garminhud.GarminHUD;
import sky4s.garminhud.eOutAngle;
import sky4s.garminhud.eUnits;
import sky4s.garminhud.hud.DummyHUD;
import sky4s.garminhud.hud.HUDInterface;


public class MainActivity extends AppCompatActivity {
    //for test with virtual device which no BT device
    public final static boolean IGNORE_BT_DEVICE = (null == BluetoothAdapter.getDefaultAdapter());

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";
    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    //========================================
    // UI for Page1Fragment
    //========================================
    TextView textViewDebug;

    //status
    Switch switchHudConnected;
    Switch switchNotificationCaught;
    Switch switchGmapsNotificationCaught;

    //setting
    Switch switchShowSpeed;
    Switch switchAutoBrightness;
    SeekBar seekBarBrightness;

    Switch switchShowETA;
    Switch switchIdleShowCurrrentTime;

    //traffic
    Switch switchTrafficAndLane;
    Switch switchAlertYellowTraffic;

    //bluetooth
    Switch switchBtBindAddress;

    private boolean isEnabledNLS = false;
    private boolean showCurrentTime = false;
    private BluetoothSPP bt;
    HUDInterface hud = new DummyHUD();
    private NotificationManager notifyManager;
    private MsgReceiver msgReceiver;
    private boolean lastReallyInNavigation = false;
    boolean is_in_navigation = false;
    private BroadcastReceiver screenReceiver;
    private Timer timer = new Timer(true);
    private TimerTask currentTimeTask;
    // The ViewPager is responsible for sliding pages (fragments) in and out upon user input
    private ViewPager mViewPager;

    //    private NavigationItemSelectedListener navigationListener = new NavigationItemSelectedListener();
    private NavigationView.OnNavigationItemSelectedListener navigationListener = new NavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(MenuItem item) {
            return false;
        }
    };


    //========================================================================================
    private SharedPreferences sharedPref;
    //    private DrawerLayout mDrawerLayout;
    private BluetoothConnectionListener btConnectionListener = new BluetoothConnectionListener();
    private SeekBar.OnSeekBarChangeListener seekbarChangeListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            switchAutoBrightness.setText("Brightness " + (progress * 10) + "%");
            if (null != hud) {
                int brightness = getGammaBrightness();
                hud.SetBrightness(brightness);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    //===============================================================================================
    // location
    //===============================================================================================
    private boolean locationServiceConnected;
    private boolean useLocationService;
    private LocationService locationService;
    private LocationManager locationManager;
    private ServiceConnection locationServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationService.LocalBinder binder = (LocationService.LocalBinder) service;
            locationService = binder.getService();
            locationServiceConnected = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            locationServiceConnected = false;
        }
    };


    private boolean isInNavigation() {
        return switchGmapsNotificationCaught.isChecked();
    }

    void sendBooleanExtraByBroadcast(String receiver, String key, boolean b) {
        Intent intent = new Intent(receiver);
        intent.putExtra(key, b);
        sendBroadcast(intent);
    }

    private void setSpeed(int nSpeed, boolean bIcon) {
        if (null != hud) {
            if (is_in_navigation) {
                hud.SetSpeed(nSpeed, bIcon);
            } else {
                hud.SetDistance(nSpeed, eUnits.None);
            }
        }
    }

//    private void clearSpeed() {
//        if (null != hud) {
//            if (is_in_navigation) {
//                hud.ClearSpeedandWarning();
//            } else {
//                hud.ClearDistance();
//            }
//        }
//    }

    void loadOptions() {

        switchShowSpeed.setOnCheckedChangeListener(onCheckedChangedListener);
        switchTrafficAndLane.setOnCheckedChangeListener(onCheckedChangedListener);
        switchAlertYellowTraffic.setOnCheckedChangeListener(onCheckedChangedListener);
        switchShowETA.setOnCheckedChangeListener(onCheckedChangedListener);
        switchIdleShowCurrrentTime.setOnCheckedChangeListener(onCheckedChangedListener);
        switchBtBindAddress.setOnCheckedChangeListener(onCheckedChangedListener);

        final boolean optionShowSpeed = sharedPref.getBoolean(getString(R.string.option_show_speed), false);
        final boolean optionTrafficAndLaneDetect = sharedPref.getBoolean(getString(R.string.option_traffic_and_lane_detect), false);
        final boolean optionAlertYellowTraffic = sharedPref.getBoolean(getString(R.string.option_alert_yellow_traffic), false);
        final boolean optionShowEta = sharedPref.getBoolean(getString(R.string.option_show_eta), false);
        final boolean optionIdleShowTime = sharedPref.getBoolean(getString(R.string.option_idle_show_current_time), false);
        final boolean optionBtBindAddress = sharedPref.getBoolean(getString(R.string.option_bt_bind_address), false);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switchShowSpeed.setChecked(optionShowSpeed);
                switchTrafficAndLane.setChecked(optionTrafficAndLaneDetect);
                switchAlertYellowTraffic.setChecked(optionAlertYellowTraffic);
                switchShowETA.setChecked(optionShowEta);
                switchIdleShowCurrrentTime.setChecked(optionIdleShowTime);
                switchBtBindAddress.setChecked(optionBtBindAddress);
            }
        });
    }

    private String initBluetooth() {
        String bt_status = "";
        if (!IGNORE_BT_DEVICE) {
            if (null == bt) {
                bt = new BluetoothSPP(this);
            } else {
                bt.disconnect();
            }
            bt.setBluetoothConnectionListener(btConnectionListener);
            bt.setAutoConnectionListener(btConnectionListener);
            if (!bt.isBluetoothAvailable()) {
                Toast.makeText(getApplicationContext()
                        , "Bluetooth is not available"
                        , Toast.LENGTH_SHORT).show();
                finish();
            }
            hud = new GarminHUD(bt);

            if (!bt.isBluetoothEnabled()) { //bt cannot work
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, BluetoothState.REQUEST_ENABLE_BT);
            } else {
                if (!bt.isServiceAvailable()) {
                    bt.setupService();
                    bt.startService(BluetoothState.DEVICE_OTHER);
                }

                boolean isBindName = false;
                if (null != switchBtBindAddress) {
                    boolean isBindAddress = switchBtBindAddress.isChecked();
                    if (isBindAddress) {
                        String bindAddress = sharedPref.getString(getString(R.string.bt_bind_address_key), null);
                        if (null != bindAddress) {
                            bt.connect(bindAddress);
                        }
                    } else {
                        isBindName = true;
                    }
                } else {
                    isBindName = true;
                }

                if (isBindName) {
                    String bindName = sharedPref.getString(getString(R.string.bt_bind_name_key), null);
                    if (null != bindName) {
                        bt.autoConnect(bindName);
                    }
                }

            }


        } else {
            bt_status = "(NO BT)";
        }
        NotificationMonitor.hud = hud;

        return bt_status;
    }

    static Boolean isDebug = null;

    private static boolean isDebug() {
        return isDebug == null ? false : isDebug.booleanValue();
    }

    /**
     * Sync lib debug with app's debug value. Should be called in module Application
     *
     * @param context
     */
    private static void syncIsDebug(Context context) {
        if (isDebug == null) {
            isDebug = context.getApplicationInfo() != null &&
                    (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        syncIsDebug(getApplicationContext());

        //=======================================================================================
        // tabs
        //========================================================================================
        // Connect the ViewPager to our custom PagerAdapter. The PagerAdapter supplies the pages
        // (fragments) to the ViewPager, which the ViewPager needs to display.
        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mViewPager.setAdapter(new MyPagerAdapter(getFragmentManager()));

        // Connect the tabs with the ViewPager (the setupWithViewPager method does this for us in
        // both directions, i.e. when a new tab is selected, the ViewPager switches to this page,
        // and when the ViewPager switches to a new page, the corresponding tab is selected)
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(mViewPager);
        //========================================================================================


        startService(new Intent(this, NotificationCollectorMonitorService.class));

        sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        //========================================================================================
        // BT related
        //========================================================================================
        String bt_status = initBluetooth();
        //========================================================================================

        //=======================================================================================
        // toolbar
        //========================================================================================
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar); //when pass toolbar as actionBar, toolbar has title
        ActionBar actionBar = getSupportActionBar();
        String versionName = BuildConfig.VERSION_NAME;

        String title = actionBar.getTitle() + " v" + versionName;// + " (b" + versionCode + ")" + bt_status;
        actionBar.setTitle(title);
        //========================================================================================

        //========================================================================================
        // NavigationView
        //========================================================================================
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(navigationListener);
        //========================================================================================

        //========================================================================================
        // messageer
        //========================================================================================
        msgReceiver = new MsgReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(getString(R.string.broadcast_receiver_main_activity));
        registerReceiver(msgReceiver, intentFilter);

        // INITIALIZE RECEIVER
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                    if (useLocationService) {

                    }
                    // DO WHATEVER YOU NEED TO DO HERE
//                wasScreenOn = false;
                } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                    // AND DO WHATEVER YOU NEED TO DO HERE
//                wasScreenOn = true;
                }
            }
        };

        registerReceiver(screenReceiver, filter);
        registerReceiver(notificationSwitchReceiver, new IntentFilter(getString(R.string.broadcast_notification_switch_speed)));
        registerReceiver(notificationSwitchReceiver, new IntentFilter(getString(R.string.broadcast_notification_switch_auto_brightness)));
        registerReceiver(notificationSwitchReceiver, new IntentFilter(getString(R.string.broadcast_notification_switch_ETA)));
        registerReceiver(notificationSwitchReceiver, new IntentFilter(getString(R.string.broadcast_notification_switch_time)));
        registerReceiver(notificationSwitchReceiver, new IntentFilter(getString(R.string.broadcast_notification_switch_detect)));


        //========================================================================================

        //========================================================================================
        // MediaProjection
        //========================================================================================
        // call for the projection notifyManager
        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        // start capture handling thread
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                mProjectionHandler = new Handler();
                Looper.loop();
            }
        }.start();
        detectListener = new ImageDetectListener(this);
        //========================================================================================

        //experiment:
//        createNotification(this);
        startNotification();
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!IGNORE_BT_DEVICE) {
            if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
                if (resultCode == Activity.RESULT_OK) {
                    bt.connect(data);
                }
            } else if (requestCode == BluetoothState.REQUEST_ENABLE_BT) {
                if (resultCode == Activity.RESULT_OK) {
                    bt.setupService();
                    bt.startService(BluetoothState.DEVICE_OTHER);
                } else {
                    Toast.makeText(getApplicationContext()
                            , "Bluetooth was not enabled."
                            , Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }

        if (requestCode == SCREENCAP_REQUEST_CODE) {
            sMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);

            if (sMediaProjection != null) {

                String state = Environment.getExternalStorageState();

                if ("mounted".equals(state)) {
                    String sdcardPath = getApplicationContext().getExternalCacheDir().getAbsolutePath();
                    STORE_DIRECTORY = sdcardPath + "/screenshots/";

                    File storeDirectory = new File(STORE_DIRECTORY);
                    if (!storeDirectory.exists()) {
                        boolean success = storeDirectory.mkdirs();
                        if (!success) {
                            Log.e(TAG, "failed to create file storage directory.");
                            return;
                        }
                    }
                } else {
                    Log.e(TAG, "failed to create file storage directory, external storage is not exist.");
                    return;
                }

                // display metrics
                DisplayMetrics metrics = getResources().getDisplayMetrics();
                mDensity = metrics.densityDpi;
                mDisplay = getWindowManager().getDefaultDisplay();

                // create virtual display depending on device width / height
                createVirtualDisplay();

                // register orientation change callback
                mOrientationChangeCallback = new OrientationChangeCallback(this);
                if (mOrientationChangeCallback.canDetectOrientation()) {
                    mOrientationChangeCallback.enable();
                }

                // register media projection stop callback
                sMediaProjection.registerCallback(new MediaProjectionStopCallback(), mProjectionHandler);
            }
        }
    }

    public void onStop() {
        super.onStop();

    }

    public void onDestroy() {
        super.onDestroy();
        btTeconnectThreadEN = false;
        if (!IGNORE_BT_DEVICE) {
            bt.stopAutoConnect();
            bt.stopService();
        }
        unbindLocationService();
        if (notifyManager != null) {
            notifyManager.cancel(1);
        }
        stopProjection();

        unregisterReceiver(msgReceiver);
        unregisterReceiver(screenReceiver);
        unregisterReceiver(notificationSwitchReceiver);

        stopNotification();
    }

    @Override
    public void onStart() {
        super.onStart();
        final boolean bypassBtInitOnStart = true;
        if (!bypassBtInitOnStart) {
            if (!IGNORE_BT_DEVICE) {
                if (!bt.isBluetoothEnabled()) {
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    //there comes twice bt permission dialog
                    startActivityForResult(intent, BluetoothState.REQUEST_ENABLE_BT);
                } else {
                    if (!bt.isServiceAvailable()) {
                        bt.setupService();
                        bt.startService(BluetoothState.DEVICE_OTHER);
                    }
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isEnabledNLS = isNLSEnabled();
        log("isEnabledNLS = " + isEnabledNLS);
        if (!isEnabledNLS) {
            showConfirmDialog();
        }


    }

    @Override
    protected void onPause() {
        super.onPause();
        //move register/unregister from   onPause/onResume to onCreate/onDestroy,insure got broadcast when in background

    }

    private int getGammaBrightness() {
        final int progress = seekBarBrightness.getProgress();
        float progress_normal = progress * 1.0f / seekBarBrightness.getMax();
        final float gamma = 0.45f;
        float progress_gamma = (float) Math.pow(progress_normal, gamma);
        int gamma_brightness = Math.round(progress_gamma * seekBarBrightness.getMax());
        return gamma_brightness;
    }

    private void storeOptions(int optionID, boolean option) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(getString(optionID), option);
        editor.commit();
    }

    private class OnCheckedChangedListener implements CompoundButton.OnCheckedChangeListener {

        @Override
        public void onCheckedChanged(CompoundButton view, boolean b) {
            switch (view.getId()) {
                case R.id.switchShowSpeed:
                    final boolean canShowSpeed = showSpeed(((Switch) view).isChecked());
                    if (!canShowSpeed) {
                        ((Switch) view).setChecked(false);
                    }
                    storeOptions(R.string.option_show_speed, ((Switch) view).isChecked());
                    break;

                case R.id.switchTrafficAndLane:
                    if (((Switch) view).isChecked()) {
                        startProjection();
                    } else {
                        stopProjection();
                    }
                    storeOptions(R.string.option_traffic_and_lane_detect, ((Switch) view).isChecked());
                    break;

                case R.id.switchAlertYellowTraffic:
                    alertYellowTraffic = ((Switch) view).isChecked();
                    storeOptions(R.string.option_alert_yellow_traffic, ((Switch) view).isChecked());
                    break;

                case R.id.switchShowETA:
                    sendBooleanExtraByBroadcast(getString(R.string.broadcast_receiver_notification_monitor),
                            getString(R.string.option_show_eta), ((Switch) view).isChecked());
                    storeOptions(R.string.option_show_eta, ((Switch) view).isChecked());
                    break;

                case R.id.switchIdleShowCurrentTime:
                    showCurrentTime = ((Switch) view).isChecked();
                    if (showCurrentTime && null == currentTimeTask) {
                        currentTimeTask = new TimerTask() {
                            @Override
                            public void run() {
                                if (null != hud && !isInNavigation() && showCurrentTime) {
                                    Calendar c = Calendar.getInstance();
                                    int hour = c.get(Calendar.HOUR_OF_DAY);
                                    int minute = c.get(Calendar.MINUTE);
                                    hud.SetCurrentTime(hour, minute);
                                }
                            }
                        };

                        timer.schedule(currentTimeTask, 1000, 1000);
                    }
                    storeOptions(R.string.option_idle_show_current_time, ((Switch) view).isChecked());
                    break;

                case R.id.switchBtBindAddress:
                    final boolean isBindAddress = ((Switch) view).isChecked();
                    useBTAddressReconnectThread = isBindAddress;
                    storeOptions(R.string.option_bt_bind_address, isBindAddress);
                    break;

                default:
                    break;
            }
        }
    }

    private OnCheckedChangedListener onCheckedChangedListener = new OnCheckedChangedListener();


    public void buttonOnClicked(View view) {
        switch (view.getId()) {

            case R.id.button1:
                if (null != NotificationMonitor.getStaticInstance()) {
                    NotificationMonitor.getStaticInstance().updateArrow(Arrow.Convergence);
                }
                break;
            case R.id.button2:
                if (null != NotificationMonitor.getStaticInstance()) {
                    NotificationMonitor.getStaticInstance().updateArrow(Arrow.LeaveRoundaboutSharpRightCC);
                }
                break;
            case R.id.button3:
                if (null != NotificationMonitor.getStaticInstance()) {
                    NotificationMonitor.getStaticInstance().updateArrow(Arrow.LeaveRoundaboutSharpRight);
                }
                break;

            case R.id.btnListNotify:
                log("List notifications...");
                listCurrentNotification();
                break;

            case R.id.btnScanBT:
                log("Scan Bluetooth...");
                if (!IGNORE_BT_DEVICE) {
                    scanBluetooth();
                }
                break;

            case R.id.btnResetBT:
                log("Reset Bluetooth...");
                if (!IGNORE_BT_DEVICE) {
                    initBluetooth();
                }
                break;

            case R.id.switchAutoBrightness:
                Switch theAutoBrightness = (Switch) view;
                final boolean autoBrightness = theAutoBrightness.isChecked();

                final int progress = seekBarBrightness.getProgress();
                theAutoBrightness.setText(autoBrightness ? "Auto Brightness" : "Brightness " + (progress * 10) + "%");

                seekBarBrightness.setEnabled(!autoBrightness);
                seekBarBrightness.setOnSeekBarChangeListener(seekbarChangeListener);

                if (null != hud) {
                    if (autoBrightness) {
                        hud.SetAutoBrightness();
                    } else {
                        final int brightness = getGammaBrightness();
                        hud.SetBrightness(brightness);
                    }
                }
                break;
            default:
                break;
        }
    }

    private boolean showSpeed(final boolean doShowSpeed) {
        if (doShowSpeed) {
            if (!checkLocationPermission()) {
                return false;
            }
            if (!checkGps()) {
                return false;
            }
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                return false;
            }
            if (false == locationServiceConnected) {
                //Here, the Location Service gets bound and the GPS Speedometer gets Active.
                bindLocationService();
                useLocationService = true;
            }
        } else {
            //do not show speed
            if (true == locationServiceConnected) {
                unbindLocationService();
            }
            if (null != hud) {
                //clear according to navigate status
                if (isInNavigation()) {
                    hud.ClearSpeedandWarning();
                } else {
                    hud.ClearDistance();
                }
            }
            useLocationService = false;
        }
        return true;
    }

    private void scanBluetooth() {

        if (!bt.isBluetoothAvailable()) {
            Toast.makeText(getApplicationContext()
                    , "Bluetooth is not available"
                    , Toast.LENGTH_SHORT).show();
        } else {
            bt.setDeviceTarget(BluetoothState.DEVICE_OTHER);
            bt.setBluetoothConnectionListener(btConnectionListener);
            bt.setAutoConnectionListener(btConnectionListener);

            Intent intent = new Intent(getApplicationContext(), DeviceList.class);
            startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);

        }
    }

    /*
    Does we need restartNotificationListenerService here?
    It should be NotificaitonCollectorMonitorService's work.
     */
    private void restartNotificationListenerService(Context context) {
        //worked!
        //NotificationMonitor
        stopService(new Intent(this, NotificationMonitor.class));
        startService(new Intent(this, NotificationMonitor.class));

        PackageManager pm = getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName(this, NotificationMonitor.class),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(new ComponentName(this, NotificationMonitor.class),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

    }

    private boolean isNLSEnabled() {
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
                log(result);
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
            CharSequence text = textViewDebug.getText();
            textViewDebug.setText(result + "\n\n" + text);
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
                .setMessage("Please enable Notification Access for " + getString(R.string.app_name)
                        + ".\n\nThis app use Notification to parse Navigation Information.")
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

    private void log(Object object) {
        Log.i(TAG, object.toString());
    }


    // bind/activate LocationService
    void bindLocationService() {
        if (true == locationServiceConnected)
            return;
        Intent i = new Intent(getApplicationContext(), LocationService.class);
        bindService(i, locationServiceConnection, BIND_AUTO_CREATE);
        locationServiceConnected = true;
    }

    // unbind/deactivate LocationService
    void unbindLocationService() {
        if (false == locationServiceConnected)
            return;
        unbindService(locationServiceConnection);
        locationServiceConnected = false;
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
        alertDialogBuilder.setMessage("Enable GPS to Show Speed")
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


    private void updateTextViewDebug(String msg) {
        CharSequence orignal_text = textViewDebug.getText();
        orignal_text = orignal_text.length() > 1000 ? "" : orignal_text;
        textViewDebug.setText(msg + "\n\n" + orignal_text);
    }

    private class MsgReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String whoami = intent.getStringExtra(getString(R.string.whoami)); //for debug

            //=======================================================================
            // for debug message
            //=======================================================================
            boolean has_notify_msg = intent.hasExtra(getString(R.string.notify_msg));
            if (has_notify_msg) {
                String notify_msg = intent.getStringExtra(getString(R.string.notify_msg));
                if (null != textViewDebug) {
                    updateTextViewDebug(notify_msg);
                }
                return;
            }

            //=======================================================================
            boolean has_gps_speed = intent.hasExtra(getString(R.string.gps_speed));
            if (has_gps_speed) {
                double speed = intent.getDoubleExtra(getString(R.string.gps_speed), 0);
                int int_speed = (int) Math.round(speed);
                setSpeed(int_speed, true);

                CharSequence orignal_text = textViewDebug.getText();
                textViewDebug.setText("speed: " + int_speed + "\n\n" + orignal_text);
                return;
            }
            //=======================================================================
            // for UI usage
            //=======================================================================
            boolean notify_parse_failed = intent.getBooleanExtra(getString(R.string.notify_parse_failed), false);

            if (notify_parse_failed) {
                //when pass fail
                if (null != switchNotificationCaught && null != switchGmapsNotificationCaught) {
//                    switchNotificationCaught.setChecked(false);
                    switchGmapsNotificationCaught.setChecked(false);
                }
            } else {
                //pass success
                final boolean notify_catched = intent.getBooleanExtra(getString(R.string.notify_catched),
                        null != switchNotificationCaught ? switchNotificationCaught.isChecked() : false);
                final boolean gmaps_notify_catched = intent.getBooleanExtra(getString(R.string.gmaps_notify_catched),
                        null != switchGmapsNotificationCaught ? switchGmapsNotificationCaught.isChecked() : false);


                final boolean is_in_navigation_now = intent.getBooleanExtra(getString(R.string.is_in_navigation), is_in_navigation);

                if (null != switchNotificationCaught && null != switchGmapsNotificationCaught) {
                    if (!notify_catched) { //no notify catched
                        switchNotificationCaught.setChecked(false);
                        switchGmapsNotificationCaught.setChecked(false);
                    } else {
                        switchNotificationCaught.setChecked(notify_catched);
                        final boolean is_really_in_navigation = gmaps_notify_catched && is_in_navigation_now;
                        switchGmapsNotificationCaught.setChecked(is_really_in_navigation);

                        if (lastReallyInNavigation != is_really_in_navigation &&
                                false == is_really_in_navigation &&
                                null != hud) {
                            //exit navigation
                            hud.SetDirection(eOutAngle.AsDirection); //maybe in this line
                        }
                        is_in_navigation = is_really_in_navigation;
                        lastReallyInNavigation = is_really_in_navigation;
                    }
                }
            }

        }
    }

    //========================================================================================
    // tabs
    //========================================================================================
    // Titles of the individual pages (displayed in tabs)
    private final String[] PAGE_TITLES = new String[]{
            "Setup",
            "Debug"
    };
    // The fragments that are used as the individual pages
    private final Fragment[] PAGES = new Fragment[]{
            new Page1Fragment(),
            new Page2Fragment()
    };

    /* PagerAdapter for supplying the ViewPager with the pages (fragments) to display. */
    private class MyPagerAdapter extends FragmentPagerAdapter {

        public MyPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public Fragment getItem(int position) {
            return PAGES[position];
        }

        @Override
        public int getCount() {
            return PAGES.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return PAGE_TITLES[position];
        }

    }

    private boolean hudConnected;

    private class BluetoothConnectionListener implements BluetoothSPP.BluetoothConnectionListener, BluetoothSPP.AutoConnectionListener {
        @Override
        public void onAutoConnectionStarted() {
            int a=1;
        }

        @Override
        public void onNewConnection(String name, String address) {

        }

        /*
        talk about location service:
        only work when device connected.
        not work when device disconnected or panel off => can android send location to garmin hud when panel off?

         */

        @Override
        public void onDeviceConnected(String name, String address) {
            hudConnected = true;
            switchHudConnected.setText("'" + name + "' connected");
            switchHudConnected.setTextColor(Color.BLACK);
            switchHudConnected.setChecked(true);

            log("onDeviceConnected");

            if (useLocationService && !locationServiceConnected) {
                bindLocationService();
            }

            if (null != hud) {
                if (null != switchAutoBrightness) {
                    if (switchAutoBrightness.isChecked()) {
                        hud.SetAutoBrightness();
                    } else {
                        final int brightness = getGammaBrightness();
                        hud.SetBrightness(brightness);
                    }
                }
            }

            String connectedDeviceName = bt.getConnectedDeviceName();
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(getString(R.string.bt_bind_name_key), connectedDeviceName);

            String connectedDeviceAddress = bt.getConnectedDeviceAddress();
            editor.putString(getString(R.string.bt_bind_address_key), connectedDeviceAddress);

            editor.commit();
        }

        @Override
        public void onDeviceDisconnected() {
            hudConnected = false;
            switchHudConnected.setText("HUD disconnected");
            switchHudConnected.setTextColor(Color.RED);
            switchHudConnected.setChecked(false);

            log("onDeviceDisconnected");
            resetBT();
        }

        @Override
        public void onDeviceConnectionFailed() {
            hudConnected = false;
            switchHudConnected.setText("HUD connect failed");
            switchHudConnected.setTextColor(Color.RED);
            switchHudConnected.setChecked(false);

            log("onDeviceConnectionFailed");
            resetBT();
        }
    }


    //================================================================================
    // media proejct
    //================================================================================
    private MediaProjectionManager mProjectionManager;
    private ImageReader mImageReader;
    private Handler mProjectionHandler;
    private Display mDisplay;
    private VirtualDisplay mVirtualDisplay;
    private int mDensity;
    int mWidth;
    int mHeight;
    private int mRotation;
    private OrientationChangeCallback mOrientationChangeCallback;
    boolean alertYellowTraffic = false;

    private static final int SCREENCAP_REQUEST_CODE = 100;
    private static final String SCREENCAP_NAME = "screencap";
    private static final int VIRTUAL_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
    static String STORE_DIRECTORY;
    private static MediaProjection sMediaProjection;

    private class OrientationChangeCallback extends OrientationEventListener {

        OrientationChangeCallback(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            final int rotation = mDisplay.getRotation();
            if (rotation != mRotation) {
                mRotation = rotation;
                try {
                    // clean up
                    if (mVirtualDisplay != null) mVirtualDisplay.release();
                    if (mImageReader != null) mImageReader.setOnImageAvailableListener(null, null);

                    // re-create virtual display depending on device width / height
                    createVirtualDisplay();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private class MediaProjectionStopCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            Log.e("ScreenCapture", "stopping projection.");
            mProjectionHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mVirtualDisplay != null) mVirtualDisplay.release();
                    if (mImageReader != null) mImageReader.setOnImageAvailableListener(null, null);
                    if (mOrientationChangeCallback != null) mOrientationChangeCallback.disable();
                    sMediaProjection.unregisterCallback(MediaProjectionStopCallback.this);
                }
            });
        }
    }

    class NotificationSwitchReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            final int event = intent.getIntExtra(getString(R.string.notify_switch_event), 0);
            switch (event) {

                case 1: {
                    final boolean now = !switchShowSpeed.isChecked();
                    switchShowSpeed.setChecked(now);
                    notification = getNormalNotification("Show Speed: "
                            + (now ? "On" : "Off"));
                    mNotificationManager.notify(R.integer.notify_id, notification);
                }
                break;
                case 2: {
                    final boolean now = !switchAutoBrightness.isChecked();
                    switchAutoBrightness.setChecked(now);
                    notification = getNormalNotification("Auto Brightness: "
                            + (now ? "On" : "Off"));
                    mNotificationManager.notify(R.integer.notify_id, notification);
                }
                break;
                case 3: {
                    final boolean now = !switchShowETA.isChecked();
                    switchShowETA.setChecked(now);
                    notification = getNormalNotification("Show ETA: "
                            + (now ? "On" : "Off"));
                    mNotificationManager.notify(R.integer.notify_id, notification);
                }
                break;
                case 4: {
                    final boolean now = !switchIdleShowCurrrentTime.isChecked();
                    switchIdleShowCurrrentTime.setChecked(now);
                    notification = getNormalNotification("Show Current Time: "
                            + (now ? "On" : "Off"));
                    mNotificationManager.notify(R.integer.notify_id, notification);
                }
                break;
                case 5: {
                    final boolean now = !switchTrafficAndLane.isChecked();
                    switchTrafficAndLane.setChecked(now);
                    notification = getNormalNotification("Traffic & Lane Detection: "
                            + (now ? "On" : "Off"));
                    mNotificationManager.notify(R.integer.notify_id, notification);
                }
                break;
            }

        }
    }

    private NotificationSwitchReceiver notificationSwitchReceiver = new NotificationSwitchReceiver();

    private PendingIntent getPendingIntentForNotify(String action, int switchEvent) {
        Intent intent = new Intent(action);
        intent.putExtra(getString(R.string.notify_switch_event), switchEvent);
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }

    private Notification getNormalNotification(String contentText) {
        final Intent mainIntent = getIntent(); // 目前Activity的Intent
        int flags = PendingIntent.FLAG_CANCEL_CURRENT; // ONE_SHOT：PendingIntent只使用一次；CANCEL_CURRENT：PendingIntent執行前會先結束掉之前的；NO_CREATE：沿用先前的PendingIntent，不建立新的PendingIntent；UPDATE_CURRENT：更新先前PendingIntent所帶的額外資料，並繼續沿用
        final PendingIntent pendingMainIntent = PendingIntent.getActivity(getApplicationContext(), 0, mainIntent, flags); // 取得PendingIntent


        final PendingIntent switchSpeedPendingIntent = getPendingIntentForNotify(getString(R.string.broadcast_notification_switch_speed), 1);
        final PendingIntent switchAutoBrightnessPendingIntent = getPendingIntentForNotify(getString(R.string.broadcast_notification_switch_auto_brightness), 2);
        final PendingIntent switchETAPendingIntent = getPendingIntentForNotify(getString(R.string.broadcast_notification_switch_ETA), 3);
        final PendingIntent switchTimePendingIntent = getPendingIntentForNotify(getString(R.string.broadcast_notification_switch_time), 4);
        final PendingIntent switchDetectPendingIntent = getPendingIntentForNotify(getString(R.string.broadcast_notification_switch_detect), 5);


        final String channelID = "id";

        android.support.v4.media.app.NotificationCompat.MediaStyle style = new android.support.v4.media.app.NotificationCompat.MediaStyle();

        notification
                = new NotificationCompat.Builder(MainActivity.this, channelID)
                .setSmallIcon(R.mipmap.ic_notification_foreground)
//                .setTicker("notification on status bar.") // 設置狀態列的顯示的資訊
                .setAutoCancel(false) // 設置通知被使用者點擊後是否清除  //notification.flags = Notification.FLAG_AUTO_CANCEL;
//                .setContentTitle(getString(R.string.app_name)) // 設置下拉清單裡的標題
                .setContentText(contentText)// 設置上下文內容
                .setOngoing(true)      //true使notification變為ongoing，用戶不能手動清除// notification.flags = Notification.FLAG_ONGOING_EVENT; notification.flags = Notification.FLAG_NO_CLEAR;
                .setContentIntent(pendingMainIntent)

                .addAction(R.drawable.baseline_av_timer_24, getString(R.string.notify_switch_speed), switchSpeedPendingIntent)
                .addAction(R.drawable.baseline_brightness_auto_24, "Auto Brightness", switchAutoBrightnessPendingIntent)
                .addAction(R.drawable.baseline_drive_eta_24, getString(R.string.notify_switch_ETA), switchETAPendingIntent)
                .addAction(R.drawable.baseline_access_time_24, "Current Time", switchTimePendingIntent)
                .addAction(R.drawable.baseline_traffic_24, getString(R.string.notify_switch_detect), switchDetectPendingIntent)

                .setChannelId(channelID)
                .setStyle(style)

                .build();

        return notification;
    }

    private Notification notification;
    private NotificationManager mNotificationManager;

    private void startNotification() {
        log("startNotification");
        //Step1. 初始化NotificationManager，取得Notification服務
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notification = getNormalNotification("");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final String channelID = "id";
            NotificationChannel channel = new NotificationChannel(
                    channelID,
                    "Channel",
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
        mNotificationManager.notify(R.integer.notify_id, notification);
    }

    private void stopNotification() {
        log("stopNotification");

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll();
    }


    /****************************************** UI Widget Callbacks *******************************/
    private void startProjection() {
        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), SCREENCAP_REQUEST_CODE);
//        startNotification();

    }

    private void stopProjection() {
        mProjectionHandler.post(new Runnable() {
            @Override
            public void run() {
                if (sMediaProjection != null) {
                    sMediaProjection.stop();
                }
            }
        });
//        stopNotification();
    }

    /****************************************** Factoring Virtual Display creation ****************/
    private ImageDetectListener detectListener;//= new ImageDetectListener(this);

    private void createVirtualDisplay() {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        mWidth = dm.widthPixels;
        mHeight = dm.heightPixels;

        // start capture reader
        mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 2);
        mVirtualDisplay = sMediaProjection.createVirtualDisplay(SCREENCAP_NAME, mWidth, mHeight, mDensity, VIRTUAL_DISPLAY_FLAGS, mImageReader.getSurface(), null, mProjectionHandler);
        mImageReader.setOnImageAvailableListener(detectListener, mProjectionHandler);
    }

    //================================================================================
    // bt reconnect
    //================================================================================
    private boolean btTeconnectThreadEN = true;

    private class BTReconnectThread extends Thread {
        public void run() {
            try {
                final int interval = getResources().getInteger(R.integer.bt_reconnect_interval);
                Thread.sleep(interval);
            } catch (Exception e) {
                log(e);
            }

            if (!hudConnected && btTeconnectThreadEN) {
                String reconnectAddress = sharedPref.getString(getString(R.string.bt_bind_address_key), null);
                log("reconnect address:" + reconnectAddress);
                if (null != reconnectAddress) {
                    bt.connect(reconnectAddress);
                }
            }

        }
    }

    private boolean useBTAddressReconnectThread = false;
    private Thread btReconnectThread;

    private void resetBT() {
        if (useBTAddressReconnectThread && null != bt) {
            bt.setDeviceTarget(BluetoothState.DEVICE_OTHER);
            bt.setBluetoothConnectionListener(btConnectionListener);
            bt.setAutoConnectionListener(btConnectionListener);

            btReconnectThread = new BTReconnectThread();
            btReconnectThread.start();
        }
    }
}

class MainActivityPostman {
    private String whoami;
    private Context context;

    public MainActivityPostman(Context context, String whoami) {
        this.context = context;
        this.whoami = whoami;
    }

    private Intent intent2Main = null;

    private void checkIntentForExtra() {
        if (null == intent2Main) {
            intent2Main = new Intent(context.getString(R.string.broadcast_receiver_main_activity));
        }
    }

    public void addBooleanExtra(String key, boolean b) {
        checkIntentForExtra();
        intent2Main.putExtra(key, b);
    }

    public void addStringExtra(String key, String string) {
        checkIntentForExtra();
        intent2Main.putExtra(key, string);
    }

    public void sendIntent2MainActivity() {
        if (null != intent2Main) {
            addStringExtra(context.getString(R.string.whoami), whoami);
//            addBooleanExtra(context.getString(R.string.is_in_navigation), is_in_navigation);
            context.sendBroadcast(intent2Main);
            intent2Main = null;
        }

    }

}



