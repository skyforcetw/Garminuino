package sky4s.garminhud.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
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
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.location.LocationManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
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

    private boolean isEnabledNLS = false;
    private boolean showCurrentTime = false;

    //========================================
    // UI for Page1Fragment
    //========================================
    TextView textViewDebug;
    Switch switchHudConnected;
    Switch switchNotificationCaught;
    Switch switchGmapsNotificationCaught;


    Switch switchShowSpeed;
    Switch switchAutoBrightness;
    SeekBar seekBarBrightness;

    Switch switchShowETA;

    Switch switchIdleShowCurrrentTime;
    //========================================

    private boolean isInNavigation() {
        return switchGmapsNotificationCaught.isChecked();
    }

    private BluetoothSPP bt;
    private HUDInterface hud = new DummyHUD();
    private NotificationManager manager;

    private void sendBooleanExtraByBroadcast(String receiver, String key, boolean b) {
        Intent intent = new Intent(receiver);
        intent.putExtra(key, b);
        sendBroadcast(intent);
    }

    private MsgReceiver msgReceiver;
    private boolean lastReallyInNavigation = false;
    private boolean is_in_navigation = false;

    private void setSpeed(int nSpeed, boolean bIcon) {
        if (null != hud) {
            if (is_in_navigation) {
                hud.SetSpeed(nSpeed, bIcon);
            } else {
                hud.SetDistance(nSpeed, eUnits.None);
            }
        }
    }

    private void clearSpeed() {
        if (null != hud) {
            if (is_in_navigation) {
                hud.ClearSpeedandWarning();
            } else {
                hud.ClearDistance();
            }
        }
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
                CharSequence orignal_text = textViewDebug.getText();
                orignal_text = orignal_text.length() > 1000 ? "" : orignal_text;
                textViewDebug.setText(notify_msg + "\n\n" + orignal_text);
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
                    switchNotificationCaught.setChecked(false);
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

                //for location usage
//                sendBooleanExtraByBroadcast(getString(R.string.broadcast_receiver_location_service), getString(R.string.is_in_navigation), isInNavigation());
            }

        }
    }

    private ScreenReceiver screenReceiver;

    private class ScreenReceiver extends BroadcastReceiver {

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

    }

    private Timer timer = new Timer(true);
    private CurrentTimeTask currentTimeTask;

    private class CurrentTimeTask extends TimerTask {
        public void run() {
            if (null != hud && !isInNavigation() && showCurrentTime) {
                Calendar c = Calendar.getInstance();
                int hour = c.get(Calendar.HOUR_OF_DAY);
                int minute = c.get(Calendar.MINUTE);
//                hud.SetTime(hour, minute, false, false);
                hud.SetCurrentTime(hour, minute);
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
    // The ViewPager is responsible for sliding pages (fragments) in and out upon user input
    private ViewPager mViewPager;

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

    //========================================================================================

    private NavigationItemSelectedListener navigationListener = new NavigationItemSelectedListener();

    private class NavigationItemSelectedListener implements NavigationView.OnNavigationItemSelectedListener {


        @Override
        public boolean onNavigationItemSelected(MenuItem item) {
            return false;
        }
    }


    private SharedPreferences sharedPref;

    void loadOptions() {

//        boolean optionNavigatingShowSpeed = sharedPref.getBoolean(getString(R.string.option_navigating_show_speed), false);
//        boolean optionIdleShowSpeed = sharedPref.getBoolean(getString(R.string.option_idle_show_speed), false);
//        switchNavShowSpeed.setChecked(optionNavigatingShowSpeed);
//        switchIdleShowSpeed.setChecked(optionIdleShowSpeed);
//        showSpeed(optionNavigatingShowSpeed, optionIdleShowSpeed);

        boolean optionShowEta = sharedPref.getBoolean(getString(R.string.option_show_eta), false);
        boolean optionIdleShowTime = sharedPref.getBoolean(getString(R.string.option_idle_show_current_time), false);
    }

    private DrawerLayout mDrawerLayout;
//    private ActionBarDrawerToggle mDrawerToggle;

    private String init_bt() {
        String bt_status = "";
        if (!IGNORE_BT_DEVICE) {
            bt = new BluetoothSPP(this);
            bt.setBluetoothConnectionListener(btConnectionListener);
            bt.setAutoConnectionListener(btConnectionListener);
            if (!bt.isBluetoothAvailable()) {
                Toast.makeText(getApplicationContext()
                        , "Bluetooth is not available"
                        , Toast.LENGTH_SHORT).show();
                finish();
            }
            hud = new GarminHUD(bt);

            String bt_bind_name = sharedPref.getString(getString(R.string.bt_bind_name_key), null);

            if (null != bt_bind_name) {
                if (!bt.isBluetoothEnabled()) { //bt cannot work
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


        } else {
            bt_status = "(NO BT)";
        }
        NotificationMonitor.hud = hud;

        return bt_status;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        String bt_status = init_bt();
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
        screenReceiver = new ScreenReceiver();
        registerReceiver(screenReceiver, filter);
        //========================================================================================

        //========================================================================================
        // MediaProjection
        //========================================================================================
        // call for the projection manager
        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        // start capture handling thread
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                mHandler = new Handler();
                Looper.loop();
            }
        }.start();
        //========================================================================================

        //experiment:
//        createNotification(this);
    }

    private boolean garminHudConnected;

    private class BluetoothConnectionListener implements BluetoothSPP.BluetoothConnectionListener, BluetoothSPP.AutoConnectionListener {
        @Override
        public void onAutoConnectionStarted() {
            int a = 1;
        }

        @Override
        public void onNewConnection(String name, String address) {
            int a = 1;
        }

        /*
        talk about location service:
        only work when device connected.
        not work when device disconnected or panel off => can android send location to garmin hud when panel off?

         */

        @Override
        public void onDeviceConnected(String name, String address) {
            garminHudConnected = true;
            switchHudConnected.setText("'" + name + "' connected");
            switchHudConnected.setTextColor(Color.BLACK);
            switchHudConnected.setChecked(true);

//            NotificationMonitor.hud = hud;
            log("onDeviceConnected");

            if (useLocationService && !locationServiceConnected) {
                bindLocationService();
            }

            if (null != hud) {
                if (switchAutoBrightness.isChecked()) {
                    hud.SetAutoBrightness();
                } else {
                    final int brightness = getGammaBrightness();
                    hud.SetBrightness(brightness);
                }
            }

            String connected_device_name = bt.getConnectedDeviceName();
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(getString(R.string.bt_bind_name_key), connected_device_name);
            editor.commit();
        }

        @Override
        public void onDeviceDisconnected() {
            garminHudConnected = false;
            switchHudConnected.setText("HUD disconnected");
            switchHudConnected.setTextColor(Color.RED);
            switchHudConnected.setChecked(false);
//            NotificationMonitor.hud = null;
            log("onDeviceDisconnected");
        }

        @Override
        public void onDeviceConnectionFailed() {
            switchHudConnected.setText("HUD connect failed");
            switchHudConnected.setTextColor(Color.RED);
            switchHudConnected.setChecked(false);
//            NotificationMonitor.hud = null;
            log("onDeviceConnectionFailed");
        }
    }

    private BluetoothConnectionListener btConnectionListener = new BluetoothConnectionListener();

    public void onStop() {
        super.onStop();
    }

    public void onDestroy() {
        super.onDestroy();
        if (!IGNORE_BT_DEVICE) {
            bt.stopAutoConnect();
            bt.stopService();
        }
        unbindLocationService();
        if (manager != null) {
            manager.cancel(1);
        }

        unregisterReceiver(msgReceiver);
        unregisterReceiver(screenReceiver);
    }

    @Override
    public void onStart() {
        super.onStart();

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
//        unregisterReceiver(msgReceiver);
    }

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

    private int getGammaBrightness() {
        final int progress = seekBarBrightness.getProgress();
        float progress_normal = progress * 1.0f / seekBarBrightness.getMax();
        final float gamma = 0.45f;
        float progress_gamma = (float) Math.pow(progress_normal, gamma);
        int gamma_brightness = Math.round(progress_gamma * seekBarBrightness.getMax());
        return gamma_brightness;
    }


    public void buttonOnClicked(View view) {
        switch (view.getId()) {

            case R.id.button1:
                if (null != NotificationMonitor.getStaticInstance()) {
                    NotificationMonitor.getStaticInstance().processArrow(Arrow.Convergence);
                }
                break;
            case R.id.button2:
                if (null != NotificationMonitor.getStaticInstance()) {
                    NotificationMonitor.getStaticInstance().processArrow(Arrow.LeaveRoundaboutSharpRightCC);
                }
                break;
            case R.id.button3:
                if (null != NotificationMonitor.getStaticInstance()) {
                    NotificationMonitor.getStaticInstance().processArrow(Arrow.LeaveRoundaboutSharpRight);
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
                    init_bt();
                }
                break;

            case R.id.switchShowSpeed:
                final boolean canShowSpeed = showSpeed(((Switch) view).isChecked());
                if (!canShowSpeed) {
                    ((Switch) view).setChecked(false);
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

            case R.id.switchTrafficAndLane:
                if (((Switch) view).isChecked()) {
                    startProjection();
                } else {
                    stopProjection();
                }
                break;


            case R.id.switchShowETA:
                sendBooleanExtraByBroadcast(getString(R.string.broadcast_receiver_notification_monitor),
                        getString(R.string.option_show_eta), ((Switch) view).isChecked());

//                sendBooleanExtraByBroadcast(
//                        getString(R.string.broadcast_receiver_notification_monitor),
//                        Integer.toString(R.id.switchShowETA), ((Switch) view).isChecked());
                break;


            case R.id.switchIdleShowCurrentTime:
                showCurrentTime = ((Switch) view).isChecked();
                if (showCurrentTime && null == currentTimeTask) {
                    currentTimeTask = new CurrentTimeTask();
                    timer.schedule(currentTimeTask, 1000, 1000);
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

    private void createNotification(Context context) {
        manager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        //ignore OREO NotificationChannel, because GARMINuino no need this new feature.

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("GARMINuino")
                        .setContentText("is on working")
                        .setAutoCancel(false);

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

        if (requestCode == REQUEST_CODE) {
            sMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);

            if (sMediaProjection != null) {

                File dataFilesDir = getFilesDir();
                if (dataFilesDir != null) {
                    STORE_DIRECTORY = dataFilesDir.getAbsolutePath() + "/screenshots/";
                    File storeDirectory = new File(STORE_DIRECTORY);
                    if (!storeDirectory.exists()) {
                        boolean success = storeDirectory.mkdirs();
                        if (!success) {
                            Log.e(TAG, "failed to create file storage directory.");
                            return;
                        }
                    }
                } else {
                    Log.e(TAG, "failed to create file storage directory, getFilesDir is null.");
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
                sMediaProjection.registerCallback(new MediaProjectionStopCallback(), mHandler);
            }
        }
    }

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
//            locationService.hud = hud;
            locationServiceConnected = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            locationServiceConnected = false;
        }
    };

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

    //===============================================================================================
    // screen capture
    //===============================================================================================
    private static final int REQUEST_CODE = 100;
    private static String STORE_DIRECTORY;
    private static final String SCREENCAP_NAME = "screencap";
    private static final int VIRTUAL_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
    private static MediaProjection sMediaProjection;

    private MediaProjectionManager mProjectionManager;
    private ImageReader mImageReader;
    private Handler mHandler;
    private Display mDisplay;
    private VirtualDisplay mVirtualDisplay;
    private int mDensity;
    private int mWidth;
    private int mHeight;
    private int mRotation;
    private OrientationChangeCallback mOrientationChangeCallback;

    private static long lastUpdateTime = 0;
    public final static long UpdateInterval = 1000;


    static class Rect {
        public int x;
        public int y;
        public int width;
        public int height;

        public Rect(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        @Override
        public final String toString() {
            return Integer.toString(x) + "," + Integer.toString(y) + " " + Integer.toString(width) + "/" + Integer.toString(height);
        }
    }

    private class ImageAvailableListener
            implements ImageReader.OnImageAvailableListener {
        public final String PreImage = "myscreen_pre.png";
        public final String NowImage = "myscreen_now.png";
        public final String GmapImage = "gmap.png";
        public final String MapImage = "map.png";
        public final String LaneImage = "lane.png";

        public final int ArrowColor_Day = Color.rgb(66, 133, 244);
        public final int ArrowColor_Night = Color.rgb(223, 246, 255);
        public final int ArrowColor_Static = Color.rgb(199, 201, 201);

        public final int RoadBgGreen_1 = Color.rgb(15, 157, 88);
        public final int RoadBgGreen_2 = Color.rgb(13, 144, 79);
        public final int LaneBgGreen_Day = Color.rgb(11, 128, 67);
        public final int LaneBgGreen_Night = Color.rgb(9, 113, 56);

//        public final int BlueTraffic_1 = Color.rgb(69, 151, 255);
//        public final int BlueTraffic_2 = Color.rgb(102, 157, 246);
        public final int OrangeTraffic = Color.rgb(255, 171, 52);
        public final int RedTraffic = Color.rgb(221, 25, 29);

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
                    if (deltaTime > UpdateInterval) {
                        lastUpdateTime = currentTime;
                        Image.Plane[] planes = image.getPlanes();
                        ByteBuffer buffer = planes[0].getBuffer();
                        int pixelStride = planes[0].getPixelStride();
                        int rowStride = planes[0].getRowStride();
                        int rowPadding = rowStride - pixelStride * mWidth;

                        // create bitmap
                        bitmap = Bitmap.createBitmap(mWidth + rowPadding / pixelStride, mHeight, Bitmap.Config.ARGB_8888);
                        bitmap.copyPixelsFromBuffer(buffer);
//                        Log.i(TAG, "onImageAvailable()");

                        File nowImage = new File(STORE_DIRECTORY + NowImage);
                        if (nowImage.exists()) {
                            File preImage = new File(STORE_DIRECTORY + PreImage);
                            copy(nowImage, preImage);
                        }

                        // write bitmap to a file
                        fos = new FileOutputStream(STORE_DIRECTORY + NowImage);
                        bitmap.compress(CompressFormat.PNG, 100, fos);

                        screenDetection(bitmap);
                    }
                }

            } catch (Exception e) {
//                e.printStackTrace();
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


        boolean isSameRGB(int color1, int color2) {
//            return color1 == color2;
            return Color.red(color1) == Color.red(color2) &&
                    Color.green(color1) == Color.green(color2) &&
                    Color.blue(color1) == Color.blue(color2);
        }

        int[] pixelsInFindColor;

        int findColor(Bitmap image, int color, boolean vertical, boolean up, boolean left) {
            int width = image.getWidth();
            int height = image.getHeight();
            int totalSize = width * height;
            if (null == pixelsInFindColor || totalSize != pixelsInFindColor.length) {
                pixelsInFindColor = null;
                pixelsInFindColor = new int[width * height];
            }

//            int[] pixels = new int[width*height];
            image.getPixels(pixelsInFindColor, 0, width, 0, 0, width, height);

            int inc = 1;

            int h_start = vertical ? up ? 0 : height - 1 : 0;
            int h_inc = (vertical ? up ? 1 : -1 : 1) * inc;
            int h_end = vertical ? up ? height - 1 : 0 : height - 1;

            int w_start = vertical ? 0 : left ? 0 : width - 1;
            int w_inc = (vertical ? 1 : left ? 1 : -1) * inc;
            int w_end = vertical ? width - 1 : left ? width - 1 : 0;

            int w0_end = vertical ? w_start + w_inc : w_end;
            int w1_end = vertical ? w_end : w_start + w_inc;


            for (int w0 = w_start; w0 != w0_end; w0 += w_inc) {
                for (int h = h_start; h != h_end; h += h_inc) {
                    for (int w1 = w_start; w1 != w1_end; w1 += w_inc) {
                        int w = vertical ? w1 : w0;
//                        int pixel = image.getPixel(w, h);
                        int pixel = pixelsInFindColor[w + h * width];

                        if (isSameRGB(pixel, color)) {
                            if (vertical) {
                                return h;
                            } else {
                                return w;
                            }
                        }
                    }
                }
            }
            return -1;
        }

        Rect getRoi(Bitmap image, int... colors) {
            for (int x = 0; x < colors.length; x++) {
                int color = colors[x];
                Rect rect = getRoi(image, color);
                if (-1 != rect.x) {
                    return rect;
                }
            }
            return new Rect(-1, -1, 0, 0);
        }

        Rect getRoi(Bitmap image, int color) {
            int top = findColor(image, color, true, true, false);
            int bottom = findColor(image, color, true, false, false);
            int left = findColor(image, color, false, true, true);
            int right = findColor(image, color, false, true, false);
            return new Rect(left, top, right - left, bottom - top);
        }


        private void screenDetection(Bitmap screen) {
            int screen_width = screen.getWidth();
            int screen_height = screen.getHeight();

            Bitmap halfScreen = Bitmap.createBitmap(screen, 0, 0, screen.getWidth(), screen_height >> 1);

            Rect road_roi = getRoi(halfScreen, RoadBgGreen_1);
            if (-1 == road_roi.x || road_roi.width != screen_width) {
                road_roi = getRoi(halfScreen, RoadBgGreen_2);
            }

            Log.i(TAG, "Road roi: " + road_roi.toString());
            if (-1 == road_roi.x) {
                return;
            }

            int gmapHeight = screen_height - road_roi.y;
            Bitmap gmapScreen = Bitmap.createBitmap(screen, road_roi.x, road_roi.y, road_roi.width, gmapHeight);

            // write bitmap to a file
            try (FileOutputStream fos = new FileOutputStream(STORE_DIRECTORY + GmapImage)) {
                gmapScreen.compress(CompressFormat.PNG, 100, fos);
            } catch (IOException ex) {
                Log.e(TAG, ex.toString());
            }

            Rect arrow_roi = getRoi(gmapScreen, ArrowColor_Day, ArrowColor_Night, ArrowColor_Static);
            if (-1 == arrow_roi.x) {
                Log.i(TAG, "NoFound Arrow");
                return;
            } else {
                Log.i(TAG, "Found Arrow: " + arrow_roi.toString());
            }

            int end_y = arrow_roi.y;


            Rect lane_roi = getRoi(gmapScreen, LaneBgGreen_Day);
            if (-1 != lane_roi.x && lane_roi.width != screen_width) {
                lane_roi = getRoi(gmapScreen, LaneBgGreen_Night);
            }
            final boolean lane_roi_exist = -1 != lane_roi.x;


            if (lane_roi_exist) {
                Log.i(TAG, "Lane ROI Found");

                Bitmap lane_roi_image = Bitmap.createBitmap(gmapScreen, lane_roi.x, lane_roi.y, lane_roi.width, lane_roi.height);
                try (FileOutputStream fos = new FileOutputStream(STORE_DIRECTORY + LaneImage)) {
                    lane_roi_image.compress(CompressFormat.PNG, 100, fos);
                } catch (IOException ex) {
                    Log.e(TAG, ex.toString());
                }
                laneDetect(lane_roi_image);
            } else {
                Log.i(TAG, "Lane ROI NoFound");
            }

            Bitmap map_roi_image;
            if (-1 != road_roi.x) {
                Rect target_roi = lane_roi_exist ? lane_roi : new Rect(0, 0, road_roi.width, road_roi.height);
                int map_roi_height = gmapScreen.getHeight() - (target_roi.y + target_roi.height) - (gmapScreen.getHeight() - arrow_roi.y);


                map_roi_image = Bitmap.createBitmap(gmapScreen, target_roi.x, target_roi.y + target_roi.height,
                        target_roi.width, map_roi_height);

                // write bitmap to a file
                try (FileOutputStream fos = new FileOutputStream(STORE_DIRECTORY + MapImage)) {
                    map_roi_image.compress(CompressFormat.PNG, 100, fos);
                } catch (IOException ex) {
                    Log.e(TAG, ex.toString());
                }

                boolean busyTraffic = busyTrafficDetect(map_roi_image);
                sendBooleanExtraByBroadcast(getString(R.string.broadcast_receiver_notification_monitor),
                        getString(R.string.busy_traffic), busyTraffic);
            }


        }

        private void laneDetect(Bitmap lane) {

        }

        private boolean busyTrafficDetect(Bitmap map) {
            Rect orange_roi = getRoi(map, OrangeTraffic);
            Rect roi_red = getRoi(map, RedTraffic);
            Log.i(TAG, "busyTrafficDetect: " + "Orange" + orange_roi + " Red" + roi_red);

            boolean busyTraffic = -1 != orange_roi.x || -1 != roi_red.x;
            return busyTraffic;
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
            mHandler.post(new Runnable() {
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

    /****************************************** UI Widget Callbacks *******************************/
    private void startProjection() {
        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
    }

    private void stopProjection() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (sMediaProjection != null) {
                    sMediaProjection.stop();
                }
            }
        });
    }


    /****************************************** Factoring Virtual Display creation ****************/
    private void createVirtualDisplay() {
        // get width and height
        Point size = new Point();
        mDisplay.getSize(size);
        mWidth = size.x;
        mHeight = size.y;

        // start capture reader
        mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 2);
        mVirtualDisplay = sMediaProjection.createVirtualDisplay(SCREENCAP_NAME, mWidth, mHeight, mDensity, VIRTUAL_DISPLAY_FLAGS, mImageReader.getSurface(), null, mHandler);
        mImageReader.setOnImageAvailableListener(new ImageAvailableListener(), mHandler);
    }

}



