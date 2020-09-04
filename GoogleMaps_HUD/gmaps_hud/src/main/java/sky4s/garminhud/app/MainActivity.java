package sky4s.garminhud.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
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
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;
import chutka.bitman.com.speedometersimplified.LocationService;
import sky4s.garminhud.Arrow;
import sky4s.garminhud.GarminHUD;
import sky4s.garminhud.ImageUtils;
import sky4s.garminhud.app.detect.ImageDetectListener;
import sky4s.garminhud.eOutAngle;
import sky4s.garminhud.eUnits;
import sky4s.garminhud.hud.BMWHUD;
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
    Switch switchIdleShowCurrentTime;
    Switch switchTrafficAndLane;
    //========================================
    // UI for Page3Fragment
    //========================================
    //BMW HUD
    Switch switchBMWHUDEnabled;
    //arrow
    Switch switchArrowType;
    //traffic
    Switch switchAlertAnytime;
    SeekBar seekBarAlertSpeed;
    Switch switchAlertYellowTraffic;
    //bluetooth
    Switch switchBtBindAddress;
    //notification
    Switch switchShowNotify;
    //app-appearance
    Switch switchDarkModeAuto;
    Switch switchDarkModeManual;

    static Intent mainIntent;

    public HUDInterface hud = new DummyHUD();
    public boolean is_in_navigation = false;

    private boolean isEnabledNLS = false;
    private boolean showCurrentTime = false;
    private BluetoothSPP bt;
    private NotificationManager notifyManager = getSystemService(NotificationManager.class);
    private MsgReceiver msgReceiver;
    private boolean lastReallyInNavigation = false;
    private BroadcastReceiver screenReceiver;
    private Timer timer = new Timer(true);
    private TimerTask currentTimeTask;

    private NavigationView.OnNavigationItemSelectedListener navigationListener = item -> false;

    //========================================================================================
    private SharedPreferences sharedPref;
    private BluetoothConnectionListener btConnectionListener = new BluetoothConnectionListener();
    private SeekBar.OnSeekBarChangeListener seekbarBrightnessChangeListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            switchAutoBrightness.setText(getString(R.string.layout_seekbar_brightness) + " " + (progress * 10) + "%");
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

    private SeekBar.OnSeekBarChangeListener seekbarAlertSpeedChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            final boolean alertAnytime = switchAlertAnytime.isChecked();
            if (!alertAnytime) {
                switchAlertAnytime.setText(getString(R.string.layout_element_alert_speed_exceeds) + " " + (progress * 10) + "kph");
                storeIntOptions(R.string.option_alert_speed, progress);
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
    private boolean locationServiceConnected = false;
    private boolean useLocationService;
    private LocationManager locationManager;
    private ServiceConnection locationServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            locationServiceConnected = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            locationServiceConnected = false;
        }
    };

    public MainActivity() {
    }


    private boolean isInNavigation() {
        return switchGmapsNotificationCaught.isChecked();
    }

    public void sendBooleanExtraByBroadcast(String receiver, String key, boolean b) {
        Intent intent = new Intent(receiver);
        intent.putExtra(key, b);
        sendBroadcast(intent);
    }

    private void setSpeed(int nSpeed) {
        if (null != hud) {
            if (is_in_navigation) {
                hud.SetSpeed(nSpeed, true);
            } else {
                hud.SetDistance(nSpeed, eUnits.None);
            }
        }
    }

    void loadOptions() {
        switchShowSpeed.setOnCheckedChangeListener(onCheckedChangedListener);

        switchTrafficAndLane.setOnCheckedChangeListener(onCheckedChangedListener);
        switchAlertAnytime.setOnCheckedChangeListener(onCheckedChangedListener);
        switchAlertYellowTraffic.setOnCheckedChangeListener(onCheckedChangedListener);

        switchAutoBrightness.setOnCheckedChangeListener(onCheckedChangedListener);
        switchShowETA.setOnCheckedChangeListener(onCheckedChangedListener);
        switchIdleShowCurrentTime.setOnCheckedChangeListener(onCheckedChangedListener);
        switchBtBindAddress.setOnCheckedChangeListener(onCheckedChangedListener);
        switchShowNotify.setOnCheckedChangeListener(onCheckedChangedListener);
        switchBMWHUDEnabled.setOnCheckedChangeListener(onCheckedChangedListener);
        switchArrowType.setOnCheckedChangeListener(onCheckedChangedListener);

        //======================================
        // default settings
        //======================================
        final boolean optionShowSpeed = sharedPref.getBoolean(getString(R.string.option_show_speed), false);

        final boolean optionTrafficAndLaneDetect = sharedPref.getBoolean(getString(R.string.option_traffic_and_lane_detect), false);
        final boolean optionAlertAnytime = sharedPref.getBoolean(getString(R.string.option_alert_anytime), false);
        final int optionAlertSpeed = sharedPref.getInt(getString(R.string.option_alert_speed), 8);
        final boolean optionAlertYellowTraffic = sharedPref.getBoolean(getString(R.string.option_alert_yellow_traffic), false);

        final boolean optionShowEta = sharedPref.getBoolean(getString(R.string.option_show_eta), false);
        final boolean optionIdleShowTime = sharedPref.getBoolean(getString(R.string.option_idle_show_current_time), false);
        final boolean optionBtBindAddress = sharedPref.getBoolean(getString(R.string.option_bt_bind_address), false);
        final boolean optionShowNotify = sharedPref.getBoolean(getString(R.string.option_show_notify), false);
        final boolean optionDarkModeAuto = sharedPref.getBoolean(getString(R.string.option_dark_mode_auto), false);
        final boolean optionDarkModeMan = sharedPref.getBoolean(getString(R.string.option_dark_mode_man), false);

        final boolean optionBMWHUDEnabled = sharedPref.getBoolean(getString(R.string.option_bmw_hud_enabled), false);
        final boolean optionArrowType = sharedPref.getBoolean(getString(R.string.option_arrow_type), true);
        sendBooleanExtraByBroadcast(getString(R.string.broadcast_receiver_notification_monitor),
                getString(R.string.option_arrow_type), optionArrowType);
        //======================================

        runOnUiThread(() -> {
            switchShowSpeed.setChecked(optionShowSpeed);

            switchTrafficAndLane.setChecked(optionTrafficAndLaneDetect);
            switchAlertAnytime.setChecked(optionAlertAnytime);
            seekBarAlertSpeed.setProgress(optionAlertSpeed);
            switchAlertYellowTraffic.setChecked(optionAlertYellowTraffic);

            switchShowETA.setChecked(optionShowEta);
            switchIdleShowCurrentTime.setChecked(optionIdleShowTime);
            switchBtBindAddress.setChecked(optionBtBindAddress);
            switchShowNotify.setChecked(optionShowNotify);
            switchDarkModeAuto.setChecked(optionDarkModeAuto);
            switchDarkModeManual.setChecked(optionDarkModeMan);

            switchBMWHUDEnabled.setChecked(optionBMWHUDEnabled);
            switchArrowType.setChecked(optionArrowType);
        });

        // Need to be after initially setChecked to avoid loop 
        switchDarkModeAuto.setOnCheckedChangeListener(onCheckedChangedListener);
        switchDarkModeManual.setOnCheckedChangeListener(onCheckedChangedListener);
    }

    private void initBluetooth() {
        if (sharedPref.getBoolean(getString(R.string.option_bmw_hud_enabled), false)) {
            if (hud != null) {
                hud.disconnect();
            }
            hud = new BMWHUD(this);
            //========================================================================================
            HUDInterface.ConnectionCallback mBMWHUDConnection = state -> {
                switch (state) {
                    case CONNECTED:
                        runOnUiThread(() -> {
                            if (switchHudConnected != null) {
                                switchHudConnected.setText(getString(
                                        R.string.layout_element_hud_success_connected, "BMW HUD"));
                                if (sharedPref != null && sharedPref.getInt(getString(R.string.state_dark_mode),
                                        AppCompatDelegate.MODE_NIGHT_NO) == AppCompatDelegate.MODE_NIGHT_NO)
                                    switchHudConnected.setTextColor(Color.BLACK);
                                switchHudConnected.setChecked(true);
                            }
                        });
                        break;
                    case DISCONNECTED:
                        runOnUiThread(() -> {
                            if (switchHudConnected != null) {
                                switchHudConnected.setText(getString(R.string.layout_element_hud_disconnected));
                                switchHudConnected.setTextColor(Color.RED);
                                switchHudConnected.setChecked(false);
                            }
                        });
                        break;
                }
            };
            hud.registerConnectionCallback(mBMWHUDConnection);
        } else if (!IGNORE_BT_DEVICE) {
            bt = new BluetoothSPP(this); //first route
            bt.setBluetoothConnectionListener(btConnectionListener);
            bt.setAutoConnectionListener(btConnectionListener);
            if (!bt.isBluetoothAvailable()) {
                Toast.makeText(getApplicationContext()
                        , getString(R.string.message_bt_not_available)
                        , Toast.LENGTH_SHORT).show();
                // Allow no BT to at least access settings to switch HUD type
                NotificationMonitor.hud = hud;
                return;
            }
            hud = new GarminHUD(bt);

            if (!bt.isBluetoothEnabled()) {
                //bt cannot work
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
                    if (isBindAddress && null != sharedPref) {
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

                if (isBindName && null != sharedPref) {
                    String bindName = sharedPref.getString(getString(R.string.bt_bind_name_key), null);
                    if (null != bindName) {
                        bt.autoConnect(bindName);
                    }
                }
            }
        }
        NotificationMonitor.hud = hud;
    }

    static Boolean isDebug = null;

    /**
     * Sync lib debug with app's debug value. Should be called in module Application
     *
     * @param context context
     */
    private static void syncIsDebug(Context context) {
        if (isDebug == null) {
            isDebug = context.getApplicationInfo() != null &&
                    (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //sdcard/Android/data/sky4s.garminhud.app/cache/screenshots
        String sdcardCachePath = getApplicationContext().getExternalCacheDir().getAbsolutePath();
        SCREENCAP_STORE_DIRECTORY = sdcardCachePath + "/screenshots/";
        File screenshotDir = new File(SCREENCAP_STORE_DIRECTORY);
        if (!screenshotDir.exists()) {
            screenshotDir.mkdir();
        }

        OCR_STORE_DIRECTORY = getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
        ImageUtils.context = this;

        sharedPref = getPreferences(Context.MODE_PRIVATE);
        final int stateDarkMode = sharedPref.getInt(getString(R.string.state_dark_mode), AppCompatDelegate.MODE_NIGHT_NO);
        AppCompatDelegate.setDefaultNightMode(stateDarkMode);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        syncIsDebug(getApplicationContext());

        //=======================================================================================
        // tabs
        //========================================================================================
        // Connect the ViewPager to our custom PagerAdapter. The PagerAdapter supplies the pages
        // (fragments) to the ViewPager, which the ViewPager needs to display.
        // The ViewPager is responsible for sliding pages (fragments) in and out upon user input
        ViewPager mViewPager = findViewById(R.id.viewpager);
        mViewPager.setAdapter(new MyPagerAdapter(getSupportFragmentManager()));

        // Connect the tabs with the ViewPager (the setupWithViewPager method does this for us in
        // both directions, i.e. when a new tab is selected, the ViewPager switches to this page,
        // and when the ViewPager switches to a new page, the corresponding tab is selected)
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(mViewPager);
        //========================================================================================
        mainIntent = this.getIntent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, NotificationCollectorMonitorService.class));
        } else {
            startService(new Intent(this, NotificationCollectorMonitorService.class));
        }

        //========================================================================================
        // BT related
        //========================================================================================
        initBluetooth();
        //========================================================================================

        //=======================================================================================
        // toolbar
        //========================================================================================
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar); //when pass toolbar as actionBar, toolbar has title
        ActionBar actionBar = getSupportActionBar();
        String versionName = BuildConfig.VERSION_NAME;

        String title = actionBar.getTitle() + " v" + versionName;
        // + " (b" + versionCode + ")" + bt_status;
        actionBar.setTitle(title);
        //========================================================================================

        //========================================================================================
        // NavigationView
        //========================================================================================
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(navigationListener);
        //========================================================================================

        //========================================================================================
        // message receiver
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
            }
        };

        registerReceiver(screenReceiver, filter);
        //========================================================================================

        //========================================================================================
        // MediaProjection
        //========================================================================================
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
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {//捕捉返回鍵
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            //按返回鍵，則執行退出確認
            ConfirmExit();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void ConfirmExit() {
        //退出確認
        AlertDialog.Builder ad = new AlertDialog.Builder(MainActivity.this);
        ad.setTitle("Exit");
        ad.setMessage("Leave GoogleMaps HUD app?");
        //退出按鈕
        ad.setPositiveButton("Yes", (dialog, i) -> {
            MainActivity.this.finish();//關閉activity
        });
        ad.setNegativeButton("No", (dialog, i) -> {
            //不退出不用執行任何操作
        });
        ad.show();//顯示對話框
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
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
                    File storeDirectory = new File(SCREENCAP_STORE_DIRECTORY);
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

        //================================================================================
        // bt reconnect
        //================================================================================
        if (!IGNORE_BT_DEVICE && null != bt) {
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

        stopService(new Intent(this, NotificationCollectorMonitorService.class));
    }

    @Override
    public void onStart() {
        super.onStart();
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
    }

    private int getGammaBrightness() {
        final int progress = seekBarBrightness.getProgress();
        float progress_normal = progress * 1.0f / seekBarBrightness.getMax();
        final float gamma = 0.45f;
        float progress_gamma = (float) Math.pow(progress_normal, gamma);
        return Math.round(progress_gamma * seekBarBrightness.getMax());
    }

    private void storeOptions(int optionID, boolean option) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(getString(optionID), option);
        editor.commit();
    }

    private void storeIntOptions(int optionID, int option) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(getString(optionID), option);
        editor.commit();
    }

    /**
     * OnCheckedChangedListener and  buttonOnClicked have similar function for UI response.
     * We recommend use "button click" with buttonOnClicked.
     * Other UI (like switch) use OnCheckedChangedListener.
     */
    private CompoundButton.OnCheckedChangeListener onCheckedChangedListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton view, boolean b) {
            switch (view.getId()) {
                case R.id.switchEnableBMWHUD:
                    final boolean enableBMWHUD = view.isChecked();
                    storeOptions(R.string.option_bmw_hud_enabled, enableBMWHUD);
                    view.setText(enableBMWHUD ? R.string.layout_element_bmw_hud_enabled : R.string.layout_element_bmw_hud_disabled);
                    break;

                case R.id.switchArrowType:
                    final boolean arrowType2 = view.isChecked();
                    sendBooleanExtraByBroadcast(getString(R.string.broadcast_receiver_notification_monitor),
                            getString(R.string.option_arrow_type), arrowType2);
                    storeOptions(R.string.option_arrow_type, arrowType2);
                    view.setText(arrowType2 ? R.string.layout_element_arrow_type_v2 : R.string.layout_element_arrow_type_v1);
                    break;

                case R.id.switchShowSpeed:
                    final boolean canShowSpeed = showSpeed(view.isChecked());
                    if (!canShowSpeed) {
                        view.setChecked(false);
                    }
                    storeOptions(R.string.option_show_speed, view.isChecked());
                    break;

                case R.id.switchTrafficAndLane:
                    if (view.isChecked()) {
                        startProjection();
                    } else {
                        stopProjection();
                    }
                    storeOptions(R.string.option_traffic_and_lane_detect, view.isChecked());
                    break;

                case R.id.switchAlertAnytime:
                    Switch switchAlertAnytime = (Switch) view;
                    final boolean alertAnytime = switchAlertAnytime.isChecked();
                    if (!alertAnytime) { //if need speed info, must check gps status
                        showSpeed(true);
                    }

                    final int progress = seekBarAlertSpeed.getProgress();
                    final int kph = progress * 10;
                    alertSpeedExceeds = alertAnytime ? 0 : kph;
                    switchAlertAnytime.setText(alertAnytime ? getString(R.string.layout_element_alert_anytime)
                            : getString(R.string.layout_element_alert_speed_exceeds) + " " + kph + "kph");

                    seekBarAlertSpeed.setEnabled(!alertAnytime);
                    seekBarAlertSpeed.setOnSeekBarChangeListener(seekbarAlertSpeedChangeListener);
                    storeOptions(R.string.option_alert_anytime, alertAnytime);
                    break;

                case R.id.switchAlertYellowTraffic:
                    alertYellowTraffic = view.isChecked();
                    storeOptions(R.string.option_alert_yellow_traffic, view.isChecked());
                    break;

                case R.id.switchShowETA:
                    sendBooleanExtraByBroadcast(getString(R.string.broadcast_receiver_notification_monitor),
                            getString(R.string.option_show_eta), view.isChecked());
                    storeOptions(R.string.option_show_eta, view.isChecked());
                    break;

                case R.id.switchIdleShowCurrentTime:
                    showCurrentTime = view.isChecked();
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
                    storeOptions(R.string.option_idle_show_current_time, view.isChecked());
                    break;

                case R.id.switchBtBindAddress:
                    final boolean isBindAddress = view.isChecked();
                    useBTAddressReconnectThread = isBindAddress;
                    storeOptions(R.string.option_bt_bind_address, isBindAddress);
                    break;

                case R.id.switchShowNotify:
                    final boolean isShowNotify = view.isChecked();
                    storeOptions(R.string.option_show_notify, isShowNotify);
                    break;

                case R.id.switchDarkModeAuto:
                    final boolean isDarkModeAuto = view.isChecked();
                    switchDarkModeManual.setEnabled(!isDarkModeAuto);
                    storeOptions(R.string.option_dark_mode_auto, isDarkModeAuto);
                    if (isDarkModeAuto) {
                        storeIntOptions(R.string.state_dark_mode, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    } else {
                        final boolean isDarkModeManualEnabled = null != sharedPref &&
                                sharedPref.getBoolean(getString(R.string.option_dark_mode_man), false);
                        storeIntOptions(R.string.state_dark_mode, isDarkModeManualEnabled ?
                                AppCompatDelegate.MODE_NIGHT_YES :
                                AppCompatDelegate.MODE_NIGHT_NO);
                        recreate();
                    }
                    break;

                case R.id.switchDarkModeMan:
                    final boolean isDarkModeManualEnabled = view.isChecked();
                    storeOptions(R.string.option_dark_mode_man, isDarkModeManualEnabled);
                    storeIntOptions(R.string.state_dark_mode, isDarkModeManualEnabled ?
                            AppCompatDelegate.MODE_NIGHT_YES :
                            AppCompatDelegate.MODE_NIGHT_NO);
                    recreate();
                    break;

                case R.id.switchAutoBrightness:
                    Switch theAutoBrightness = (Switch) view;
                    final boolean autoBrightness = theAutoBrightness.isChecked();

                    final int brightnessProgress = seekBarBrightness.getProgress();
                    theAutoBrightness.setText(autoBrightness ? getString(R.string.layout_element_auto_brightness)
                            : getString(R.string.layout_seekbar_brightness) + " " + (brightnessProgress * 10) + "%");

                    seekBarBrightness.setEnabled(!autoBrightness);
                    seekBarBrightness.setOnSeekBarChangeListener(seekbarBrightnessChangeListener);

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
    };

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
            if (!locationServiceConnected) {
                //Here, the Location Service gets bound and the GPS Speedometer gets Active.
                bindLocationService();
                useLocationService = true;
            }
        } else {
            //do not show speed
            if (locationServiceConnected) {
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
        if (null == bt || !bt.isBluetoothAvailable()) {
            Toast.makeText(getApplicationContext()
                    , getString(R.string.message_bt_not_available)
                    , Toast.LENGTH_SHORT).show();
        } else {
            bt.setDeviceTarget(BluetoothState.DEVICE_OTHER);
            bt.setBluetoothConnectionListener(btConnectionListener);
            bt.setAutoConnectionListener(btConnectionListener);

            Intent intent = new Intent(getApplicationContext(), DeviceList.class);
            startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
        }
    }

    private boolean isNLSEnabled() {
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(),
                ENABLED_NOTIFICATION_LISTENERS);
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (String name : names) {
                final ComponentName cn = ComponentName.unflattenFromString(name);
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
        StringBuilder listNos = new StringBuilder();
        StatusBarNotification[] currentNos = NotificationMonitor.getCurrentNotifications();
        if (currentNos != null) {
            for (int i = 0; i < currentNos.length; i++) {
                listNos.append(i + 1).append(" ").append(currentNos[i].getPackageName()).append("\n");
            }
        }
        return listNos.toString();
    }

    private void listCurrentNotification() {
        String result;
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
                result = getResources().getQuantityString(R.plurals.active_notification_count_nonzero, n, n);
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
        final String app_name = getString(R.string.app_name);
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.message_enable_notification_access, app_name))
                .setTitle(getString(R.string.title_enable_notification_access))
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok,
                        (dialog, id) -> openNotificationAccess())
                .setNegativeButton(android.R.string.cancel,
                        (dialog, id) -> {
                            // do nothing
                        })
                .create().show();
    }

    private void log(Object object) {
        Log.i(TAG, object.toString());
    }

    // bind/activate LocationService
    void bindLocationService() {
        if (locationServiceConnected) {
            return;
        }
        Intent i = new Intent(getApplicationContext(), LocationService.class);
        bindService(i, locationServiceConnection, BIND_AUTO_CREATE);
        locationServiceConnected = true;
    }

    // unbind/deactivate LocationService
    void unbindLocationService() {
        if (!locationServiceConnected) {
            return;
        }
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
                        (dialog, id) -> {
                            Intent callGPSSettingIntent = new Intent(
                                    Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(callGPSSettingIntent);
                        });
        alertDialogBuilder.setNegativeButton("Cancel",
                (dialog, id) -> dialog.cancel());
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    // Check permission for location (and ask user for permission)
    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                String message = getString(R.string.message_enable_location_access);
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission")
                        .setMessage(message)
                        .setPositiveButton("Ok", (dialogInterface, i) -> {
                            //Prompt the user once explanation has been shown
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_BACKGROUND_LOCATION
                                        },
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            } else {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            // Permission has already been granted
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
            //=======================================================================
            // for debug message
            //=======================================================================
            boolean has_notify_msg = intent.hasExtra(getString(R.string.notify_msg));
            if (has_notify_msg) {
                //if recv notify message,  display it then return (jump out)
                String notify_msg = intent.getStringExtra(getString(R.string.notify_msg));
                if (null != textViewDebug) {
                    updateTextViewDebug(notify_msg);
                }
                return;
            }

            //=======================================================================
            // gps speed
            //=======================================================================
            boolean has_gps_speed = intent.hasExtra(getString(R.string.gps_speed));
            if (has_gps_speed) {
                double speed = intent.getDoubleExtra(getString(R.string.gps_speed), 0);
                int int_speed = (int) Math.round(speed);
                gpsSpeed = int_speed;

                final boolean show_speed = switchShowSpeed.isChecked();
                if (show_speed) {
                    setSpeed(int_speed);
                }

                CharSequence orignal_text = textViewDebug.getText();
                textViewDebug.setText("speed: " + int_speed + "\n\n" + orignal_text);
                return;
            }

            //=======================================================================
            // for UI usage, parse notify_parse_failed first
            //=======================================================================
            boolean notify_parse_failed = intent.getBooleanExtra(getString(R.string.notify_parse_failed), false);

            if (notify_parse_failed) {
                //when pass fail
                if (null != switchNotificationCaught && null != switchGmapsNotificationCaught) {
                    switchGmapsNotificationCaught.setChecked(false);
                }
                is_in_navigation = false;
            } else {
                //pass success
                final boolean notify_catched = intent.getBooleanExtra(getString(R.string.notify_catched),
                        null != switchNotificationCaught && switchNotificationCaught.isChecked());
                final boolean gmaps_notify_catched = intent.getBooleanExtra(getString(R.string.gmaps_notify_catched),
                        null != switchGmapsNotificationCaught && switchGmapsNotificationCaught.isChecked());


                final boolean is_in_navigation_in_intent = intent.getBooleanExtra(getString(R.string.is_in_navigation), is_in_navigation);

                if (null != switchNotificationCaught && null != switchGmapsNotificationCaught) {
                    if (!notify_catched) {
                        //no notify catched
                        switchNotificationCaught.setChecked(false);
                        switchGmapsNotificationCaught.setChecked(false);
                    } else {
                        switchNotificationCaught.setChecked(true);
                        // we need two condition to confirm in navagating:
                        // 1. gmaps's notify
                        // 2. in_navigation from notify monitor
                        final boolean is_really_in_navigation = gmaps_notify_catched && is_in_navigation_in_intent;
                        switchGmapsNotificationCaught.setChecked(is_really_in_navigation);

                        if (lastReallyInNavigation != is_really_in_navigation &&
                                !is_really_in_navigation &&
                                null != hud) {
                            //exit navigation
                            hud.SetDirection(eOutAngle.AsDirection);
                            //maybe in this line
                        }
                        is_in_navigation = is_really_in_navigation;
                        lastReallyInNavigation = is_really_in_navigation;
                    }
                }
            }

            //=======================================================================
            if (intent.hasExtra(getString(R.string.option_arrow_type)) && null != switchArrowType) {
                //re-sync arrow type between ui & notify monitor
                boolean arrowTypeV2_in_ui = switchArrowType.isChecked();
                boolean arrowTypeV2_in_notify_monitor = intent.getBooleanExtra((getString(R.string.option_arrow_type)), arrowTypeV2_in_ui);

                if (arrowTypeV2_in_notify_monitor != arrowTypeV2_in_ui) {
                    sendBooleanExtraByBroadcast(getString(R.string.broadcast_receiver_notification_monitor),
                            getString(R.string.option_arrow_type), arrowTypeV2_in_ui);
                }

            }
        }
    }

    //========================================================================================
    // tabs
    //========================================================================================
    // Titles of the individual pages (displayed in tabs)
    private final String[] PAGE_TITLES = new String[]{
            "Main",
            "Setup",
            "Debug"
    };

    // The fragments that are used as the individual pages
    private final Fragment[] PAGES = new Fragment[]{
            new Page1Fragment(),
            new Page3Fragment(),
            new Page2Fragment()
    };

    // PagerAdapter for supplying the ViewPager with the pages (fragments) to display.
    private class MyPagerAdapter extends FragmentPagerAdapter {
        public MyPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public androidx.fragment.app.Fragment getItem(int position) {
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

    private class BluetoothConnectionListener implements BluetoothSPP.BluetoothConnectionListener, BluetoothSPP.AutoConnectionListener {
        @Override
        public void onAutoConnectionStarted() {

        }

        @Override
        public void onNewConnection(String name, String address) {

        }

        @Override
        public void onDeviceConnected(String name, String address) {
            if (null != switchHudConnected) {
                switchHudConnected.setText(getString(R.string.layout_element_hud_success_connected, name));
                if (null != sharedPref && sharedPref.getInt(getString(R.string.state_dark_mode)
                        , AppCompatDelegate.MODE_NIGHT_NO) == AppCompatDelegate.MODE_NIGHT_NO)
                    switchHudConnected.setTextColor(Color.BLACK);
                switchHudConnected.setChecked(true);
            }
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
            switchHudConnected.setText(getString(R.string.layout_element_hud_disconnected));
            switchHudConnected.setTextColor(Color.RED);
            switchHudConnected.setChecked(false);

            log("onDeviceDisconnected");
            resetBT();
        }

        @Override
        public void onDeviceConnectionFailed() {
            String text = getString(R.string.layout_element_hud_con_failed);
            if (null != switchHudConnected) {
                switchHudConnected.setText(text);
                switchHudConnected.setTextColor(Color.RED);
                switchHudConnected.setChecked(false);
            }

            log("onDeviceConnectionFailed");
            resetBT();
        }
    }

    //================================================================================
    // media projection
    //================================================================================
    private MediaProjectionManager mProjectionManager =
            (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    private ImageReader mImageReader;
    private Handler mProjectionHandler;
    private Display mDisplay;
    private VirtualDisplay mVirtualDisplay;
    private int mDensity;
    public int mWidth;
    public int mHeight;
    private int mRotation;
    private OrientationChangeCallback mOrientationChangeCallback;
    public int alertSpeedExceeds = 0;
    public int gpsSpeed = 0;
    public boolean alertYellowTraffic = false;

    private static final int SCREENCAP_REQUEST_CODE = 100;
    private static final String SCREENCAP_NAME = "screencap";
    private static final int VIRTUAL_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
    ///sdcard/Android/data/sky4s.garminhud.app/cache/screenshots
    public static String SCREENCAP_STORE_DIRECTORY;
    public static String OCR_STORE_DIRECTORY;
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
            mProjectionHandler.post(() -> {
                if (mVirtualDisplay != null) mVirtualDisplay.release();
                if (mImageReader != null) mImageReader.setOnImageAvailableListener(null, null);
                if (mOrientationChangeCallback != null) mOrientationChangeCallback.disable();
                sMediaProjection.unregisterCallback(MediaProjectionStopCallback.this);
            });
        }
    }

    /****************************************** UI Widget Callbacks *******************************/
    private void startProjection() {
        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), SCREENCAP_REQUEST_CODE);
    }

    private void stopProjection() {
        mProjectionHandler.post(() -> {
            if (sMediaProjection != null) {
                sMediaProjection.stop();
            }
        });
    }

    /****************************************** Factoring Virtual Display creation ****************/
    private ImageDetectListener detectListener;

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

    private boolean useBTAddressReconnectThread = false;

    private void resetBT() {
        if (useBTAddressReconnectThread && null != bt) {
            bt.setDeviceTarget(BluetoothState.DEVICE_OTHER);
            bt.setBluetoothConnectionListener(btConnectionListener);
            bt.setAutoConnectionListener(btConnectionListener);

        }
    }
}
