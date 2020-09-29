package sky4s.garminhud.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.Service;
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
import android.widget.TextView;

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
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import chutka.bitman.com.speedometersimplified.LocationService;
import sky4s.garminhud.Arrow;
import sky4s.garminhud.ImageUtils;
import sky4s.garminhud.app.detect.ImageDetectListener;
import sky4s.garminhud.eOutAngle;
import sky4s.garminhud.eUnits;
import sky4s.garminhud.hud.BMWHUD;
import sky4s.garminhud.hud.DummyHUD;
import sky4s.garminhud.hud.GarminHUD;
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
    TextView mDebugTextView;

    //status
    SwitchCompat mHudConnectedSwitch;
    SwitchCompat mNotificationCaughtSwitch;
    SwitchCompat mGmapsNotificationCaughtSwitch;

    //setting
    SwitchCompat mShowSpeedSwitch;
    SwitchCompat mAutoBrightnessSwitch;
    SeekBar mBrightnessSeekbar;

    SwitchCompat mShowETASwitch;
    SwitchCompat mIdleShowCurrentTimeSwitch;
    SwitchCompat mTrafficAndLaneSwitch;
    //========================================
    // UI for Page3Fragment
    //========================================
    //BMW HUD
    SwitchCompat mBMWHUDEnabledSwitch;
    //arrow
    SwitchCompat mArrowTypeSwitch;
    //traffic
    SwitchCompat mAlertAnytimeSwitch;
    SeekBar mAlertSpeedSeekbar;
    SwitchCompat mAlertYellowTrafficSwitch;
    //bluetooth
    SwitchCompat mBindBtAddressSwitch;
    //notification
    SwitchCompat mShowNotifySwitch;
    //app-appearance
    SwitchCompat mDarkModeAutoSwitch;
    SwitchCompat mDarkModeManualSwitch;

    static Intent sMainIntent;

    public HUDInterface mHud = new DummyHUD();
    public boolean mIsNavigating = false;

    private boolean mIsNLSEnabled = false;
    private boolean mShowCurrentTime = false;
    private NotificationManager mNotificationManager;
    private MsgReceiver mMsgReceiver;
    private boolean mLastReallyInNavigation = false;
    private BroadcastReceiver mScreenReceiver;
    private Timer mTimer = new Timer(true);
    private TimerTask mCurrentTimerTask;

    private NavigationView.OnNavigationItemSelectedListener mNavigationListener = item -> false;

    //========================================================================================
    private SharedPreferences mSharedPrefs;
    private SeekBar.OnSeekBarChangeListener mBrightnessSeekbarChangeListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            mAutoBrightnessSwitch.setText(getString(R.string.layout_seekbar_brightness, progress * 10));
            if (null != mHud) {
                int brightness = getGammaBrightness();
                mHud.setBrightness(brightness);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    private SeekBar.OnSeekBarChangeListener mAlertSpeedSeekbarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            final boolean alertAnytime = mAlertAnytimeSwitch.isChecked();
            if (!alertAnytime) {
                mAlertAnytimeSwitch.setText(getString(R.string.layout_element_alert_speed_exceeds, progress * 10));
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
    private boolean mLocationServiceConnected = false;
    private boolean mUseLocationService;
    private LocationManager mLocationManager;
    private ServiceConnection mLocationServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mLocationServiceConnected = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mLocationServiceConnected = false;
        }
    };

    private boolean isInNavigation() {
        return mGmapsNotificationCaughtSwitch.isChecked();
    }

    public void sendBooleanExtraByBroadcast(String receiver, String key, boolean b) {
        Intent intent = new Intent(receiver);
        intent.putExtra(key, b);
        sendBroadcast(intent);
    }

    private void setSpeed(int nSpeed) {
        if (null != mHud) {
            if (mIsNavigating) {
                mHud.setSpeed(nSpeed, true);
            } else {
                mHud.setDistance(nSpeed, eUnits.None);
            }
        }
    }

    void loadOptions() {
        mShowSpeedSwitch.setOnCheckedChangeListener(mOnCheckedChangedListener);

        mTrafficAndLaneSwitch.setOnCheckedChangeListener(mOnCheckedChangedListener);
        mAlertAnytimeSwitch.setOnCheckedChangeListener(mOnCheckedChangedListener);
        mAlertYellowTrafficSwitch.setOnCheckedChangeListener(mOnCheckedChangedListener);

        mAutoBrightnessSwitch.setOnCheckedChangeListener(mOnCheckedChangedListener);
        mShowETASwitch.setOnCheckedChangeListener(mOnCheckedChangedListener);
        mIdleShowCurrentTimeSwitch.setOnCheckedChangeListener(mOnCheckedChangedListener);
        mBindBtAddressSwitch.setOnCheckedChangeListener(mOnCheckedChangedListener);
        mShowNotifySwitch.setOnCheckedChangeListener(mOnCheckedChangedListener);
        mBMWHUDEnabledSwitch.setOnCheckedChangeListener(mOnCheckedChangedListener);
        mArrowTypeSwitch.setOnCheckedChangeListener(mOnCheckedChangedListener);

        //======================================
        // default settings
        //======================================
        final boolean optionShowSpeed = mSharedPrefs.getBoolean(getString(R.string.option_show_speed), false);

        final boolean optionTrafficAndLaneDetect = mSharedPrefs.getBoolean(getString(R.string.option_traffic_and_lane_detect), false);
        final boolean optionAlertAnytime = mSharedPrefs.getBoolean(getString(R.string.option_alert_anytime), false);
        final int optionAlertSpeed = mSharedPrefs.getInt(getString(R.string.option_alert_speed), 8);
        final boolean optionAlertYellowTraffic = mSharedPrefs.getBoolean(getString(R.string.option_alert_yellow_traffic), false);

        final boolean optionShowEta = mSharedPrefs.getBoolean(getString(R.string.option_show_eta), false);
        final boolean optionIdleShowTime = mSharedPrefs.getBoolean(getString(R.string.option_idle_show_current_time), false);
        final boolean optionBtBindAddress = mSharedPrefs.getBoolean(getString(R.string.option_bt_bind_address), false);
        final boolean optionShowNotify = mSharedPrefs.getBoolean(getString(R.string.option_show_notify), false);
        final boolean optionDarkModeAuto = mSharedPrefs.getBoolean(getString(R.string.option_dark_mode_auto), false);
        final boolean optionDarkModeMan = mSharedPrefs.getBoolean(getString(R.string.option_dark_mode_man), false);

        final boolean optionBMWHUDEnabled = mSharedPrefs.getBoolean(getString(R.string.option_bmw_hud_enabled), false);
        final boolean optionArrowType = mSharedPrefs.getBoolean(getString(R.string.option_arrow_type), true);
        sendBooleanExtraByBroadcast(getString(R.string.broadcast_receiver_notification_monitor),
                getString(R.string.option_arrow_type), optionArrowType);
        //======================================

        runOnUiThread(() -> {
            mShowSpeedSwitch.setChecked(optionShowSpeed);

            mTrafficAndLaneSwitch.setChecked(optionTrafficAndLaneDetect);
            mAlertAnytimeSwitch.setChecked(optionAlertAnytime);
            mAlertSpeedSeekbar.setProgress(optionAlertSpeed);
            mAlertYellowTrafficSwitch.setChecked(optionAlertYellowTraffic);

            mShowETASwitch.setChecked(optionShowEta);
            mIdleShowCurrentTimeSwitch.setChecked(optionIdleShowTime);
            mBindBtAddressSwitch.setChecked(optionBtBindAddress);
            mShowNotifySwitch.setChecked(optionShowNotify);
            mDarkModeAutoSwitch.setChecked(optionDarkModeAuto);
            mDarkModeManualSwitch.setChecked(optionDarkModeMan);

            mBMWHUDEnabledSwitch.setChecked(optionBMWHUDEnabled);
            mArrowTypeSwitch.setChecked(optionArrowType);
        });

        // Need to be after initially setChecked to avoid loop 
        mDarkModeAutoSwitch.setOnCheckedChangeListener(mOnCheckedChangedListener);
        mDarkModeManualSwitch.setOnCheckedChangeListener(mOnCheckedChangedListener);
    }

    private HUDInterface.ConnectionCallback mHudConnectionCallback = new HUDInterface.ConnectionCallback() {
        @Override
        public void onConnectionStateChange(ConnectionState state) {
            switch (state) {
                case CONNECTED:
                    runOnUiThread(() -> {
                        if (mHudConnectedSwitch != null) {
                            String hudName = isBMW() ? "BMW HUD" : "Garmin HUD";
                            mHudConnectedSwitch.setText(getString(
                                    R.string.layout_element_hud_success_connected, hudName));
                            if (mSharedPrefs.getInt(getString(R.string.state_dark_mode),
                                    AppCompatDelegate.MODE_NIGHT_NO) == AppCompatDelegate.MODE_NIGHT_NO)
                                mHudConnectedSwitch.setTextColor(Color.BLACK);
                            mHudConnectedSwitch.setChecked(true);
                        }
                        if (mUseLocationService && !mLocationServiceConnected) {
                            bindLocationService();
                        }
                        if (mAutoBrightnessSwitch != null && mAutoBrightnessSwitch.isChecked()) {
                            mHud.setAutoBrightness();
                        } else {
                            final int brightness = getGammaBrightness();
                            mHud.setBrightness(brightness);
                        }
                    });
                    break;
                case DISCONNECTED:
                    runOnUiThread(() -> {
                        if (mHudConnectedSwitch != null) {
                            mHudConnectedSwitch.setText(getString(R.string.layout_element_hud_disconnected));
                            mHudConnectedSwitch.setTextColor(Color.RED);
                            mHudConnectedSwitch.setChecked(false);
                        }
                    });
                    break;
                case FAILED:
                    runOnUiThread(() -> {
                        if (mHudConnectedSwitch != null) {
                            mHudConnectedSwitch.setText(getString(R.string.layout_element_hud_con_failed));
                            mHudConnectedSwitch.setTextColor(Color.RED);
                            mHudConnectedSwitch.setChecked(false);
                        }
                    });
                    break;
            }
        }
    };

    private void initializeHUD() {
        if (mHud != null) {
            mHud.disconnect();
        }
        if (isBMW()) {
            mHud = new BMWHUD(this);
        } else if (!IGNORE_BT_DEVICE) {
            mHud = new GarminHUD(this);
        }
        mHud.registerConnectionCallback(mHudConnectionCallback);
        NotificationMonitor.sHud = mHud;
    }

    private boolean isBMW() {
        return mSharedPrefs.getBoolean(getString(R.string.option_bmw_hud_enabled), false);
    }

    static Boolean sIsDebug = null;

    /**
     * Sync lib debug with app's debug value. Should be called in module Application
     *
     * @param context context
     */
    private static void syncIsDebug(Context context) {
        if (sIsDebug == null) {
            sIsDebug = context.getApplicationInfo() != null &&
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
            final boolean mk_result = screenshotDir.mkdir();
        }

        mNotificationManager = getSystemService(NotificationManager.class);
        mProjectionManager = getSystemService(MediaProjectionManager.class);
        OCR_STORE_DIRECTORY = getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
        ImageUtils.context = this;

        mSharedPrefs = getPreferences(Context.MODE_PRIVATE);
        final int stateDarkMode = mSharedPrefs.getInt(getString(R.string.state_dark_mode), AppCompatDelegate.MODE_NIGHT_NO);
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
        sMainIntent = this.getIntent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, NotificationCollectorMonitorService.class));
        } else {
            startService(new Intent(this, NotificationCollectorMonitorService.class));
        }

        //========================================================================================
        // HUD connection
        //========================================================================================
        initializeHUD();
        //========================================================================================

        //=======================================================================================
        // toolbar
        //========================================================================================
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar); //when pass toolbar as actionBar, toolbar has title
        ActionBar actionBar = getSupportActionBar();
        String versionName = BuildConfig.VERSION_NAME;

        if (actionBar != null) {
            String title = actionBar.getTitle() + " v" + versionName;
            actionBar.setTitle(title);
        }
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
        navigationView.setNavigationItemSelectedListener(mNavigationListener);
        //========================================================================================

        //========================================================================================
        // message receiver
        //========================================================================================
        mMsgReceiver = new MsgReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(getString(R.string.broadcast_receiver_main_activity));
        registerReceiver(mMsgReceiver, intentFilter);

        // INITIALIZE RECEIVER
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mScreenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
            }
        };

        registerReceiver(mScreenReceiver, filter);
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
        mImageDetectListener = new ImageDetectListener(this);
        //========================================================================================
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {//捕捉返回鍵
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            //按返回鍵，則執行退出確認
            confirmExit();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void confirmExit() {
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (mHud != null && mHud.handleActivityResult(requestCode, resultCode, data)) {
            // Activity result was handled by HUD class, stop processing here
            return;
        }

        if (requestCode == SCREENCAP_REQUEST_CODE) {
            // display metrics
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            mDensity = metrics.densityDpi;
            mDisplay = getWindowManager().getDefaultDisplay();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(new Intent(this, ScreenRecorderService.class));
            } else {
//                startService(new Intent(this, NotificationCollectorMonitorService.class));
            }

//                // create virtual display depending on device width / height
                createVirtualDisplay();
//
//                // register orientation change callback
//                mOrientationChangeCallback = new OrientationChangeCallback(this);
//                if (mOrientationChangeCallback.canDetectOrientation()) {
//                    mOrientationChangeCallback.enable();
//                }
//
//                // register media projection stop callback
//                sMediaProjection.registerCallback(new MediaProjectionStopCallback(), mProjectionHandler);
//            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mHud != null) {
            mHud.disconnect();
        }
        unbindLocationService();
        if (mNotificationManager != null) {
            mNotificationManager.cancel(1);
        }
        stopProjection();

        unregisterReceiver(mMsgReceiver);
        unregisterReceiver(mScreenReceiver);

        stopService(new Intent(this, NotificationCollectorMonitorService.class));
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsNLSEnabled = isNLSEnabled();
        log("isEnabledNLS = " + mIsNLSEnabled);
        if (!mIsNLSEnabled) {
            showConfirmDialog();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private int getGammaBrightness() {
        final int progress = mBrightnessSeekbar.getProgress();
        float progress_normal = progress * 1.0f / mBrightnessSeekbar.getMax();
        final float gamma = 0.45f;
        float progress_gamma = (float) Math.pow(progress_normal, gamma);
        return Math.round(progress_gamma * mBrightnessSeekbar.getMax());
    }

    @SuppressLint("ApplySharedPref")
    private void storeOptions(int optionID, boolean option) {
        SharedPreferences.Editor editor = mSharedPrefs.edit();
        editor.putBoolean(getString(optionID), option);
        editor.commit();
    }

    @SuppressLint("ApplySharedPref")
    private void storeIntOptions(int optionID, int option) {
        SharedPreferences.Editor editor = mSharedPrefs.edit();
        editor.putInt(getString(optionID), option);
        editor.commit();
    }

    /**
     * OnCheckedChangedListener and  buttonOnClicked have similar function for UI response.
     * We recommend use "button click" with buttonOnClicked.
     * Other UI (like switch) use OnCheckedChangedListener.
     */
    private CompoundButton.OnCheckedChangeListener mOnCheckedChangedListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton view, boolean b) {
            switch (view.getId()) {
                case R.id.switchEnableBMWHUD:
                    final boolean enableBMWHUD = view.isChecked();
                    storeOptions(R.string.option_bmw_hud_enabled, enableBMWHUD);
                    view.setText(enableBMWHUD ? R.string.layout_element_bmw_hud_enabled : R.string.layout_element_bmw_hud_disabled);
                    initializeHUD();
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
                    SwitchCompat switchAlertAnytime = (SwitchCompat) view;
                    final boolean alertAnytime = switchAlertAnytime.isChecked();
                    if (!alertAnytime) { //if need speed info, must check gps status
                        showSpeed(true);
                    }

                    final int progress = mAlertSpeedSeekbar.getProgress();
                    final int kph = progress * 10;
                    mAlertSpeedExceeds = alertAnytime ? 0 : kph;
                    switchAlertAnytime.setText(alertAnytime ? getString(R.string.layout_element_alert_anytime)
                            : getString(R.string.layout_element_alert_speed_exceeds, kph));

                    mAlertSpeedSeekbar.setEnabled(!alertAnytime);
                    mAlertSpeedSeekbar.setOnSeekBarChangeListener(mAlertSpeedSeekbarChangeListener);
                    storeOptions(R.string.option_alert_anytime, alertAnytime);
                    break;

                case R.id.switchAlertYellowTraffic:
                    mAlertYellowTraffic = view.isChecked();
                    storeOptions(R.string.option_alert_yellow_traffic, view.isChecked());
                    break;

                case R.id.switchShowETA:
                    sendBooleanExtraByBroadcast(getString(R.string.broadcast_receiver_notification_monitor),
                            getString(R.string.option_show_eta), view.isChecked());
                    storeOptions(R.string.option_show_eta, view.isChecked());
                    break;

                case R.id.switchIdleShowCurrentTime:
                    mShowCurrentTime = view.isChecked();
                    if (mShowCurrentTime && null == mCurrentTimerTask) {
                        mCurrentTimerTask = new TimerTask() {
                            @Override
                            public void run() {
                                if (null != mHud && !isInNavigation() && mShowCurrentTime) {
                                    Calendar c = Calendar.getInstance();
                                    int hour = c.get(Calendar.HOUR_OF_DAY);
                                    int minute = c.get(Calendar.MINUTE);
                                    mHud.setCurrentTime(hour, minute);
                                }
                            }
                        };

                        mTimer.schedule(mCurrentTimerTask, 1000, 1000);
                    }
                    storeOptions(R.string.option_idle_show_current_time, view.isChecked());
                    break;

                case R.id.switchBtBindAddress:
                    final boolean isBindAddress = view.isChecked();
                    storeOptions(R.string.option_bt_bind_address, isBindAddress);
                    break;

                case R.id.switchShowNotify:
                    final boolean isShowNotify = view.isChecked();
                    storeOptions(R.string.option_show_notify, isShowNotify);
                    break;

                case R.id.switchDarkModeAuto:
                    final boolean isDarkModeAuto = view.isChecked();
                    mDarkModeManualSwitch.setEnabled(!isDarkModeAuto);
                    storeOptions(R.string.option_dark_mode_auto, isDarkModeAuto);
                    if (isDarkModeAuto) {
                        storeIntOptions(R.string.state_dark_mode, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    } else {
                        final boolean isDarkModeManualEnabled =
                                mSharedPrefs.getBoolean(getString(R.string.option_dark_mode_man), false);
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
                    SwitchCompat theAutoBrightness = (SwitchCompat) view;
                    final boolean autoBrightness = theAutoBrightness.isChecked();

                    final int brightnessProgress = mBrightnessSeekbar.getProgress();
                    theAutoBrightness.setText(autoBrightness ? getString(R.string.layout_element_auto_brightness)
                            : getString(R.string.layout_seekbar_brightness, brightnessProgress * 10));

                    mBrightnessSeekbar.setEnabled(!autoBrightness);
                    mBrightnessSeekbar.setOnSeekBarChangeListener(mBrightnessSeekbarChangeListener);

                    if (null != mHud) {
                        if (autoBrightness) {
                            mHud.setAutoBrightness();
                        } else {
                            final int brightness = getGammaBrightness();
                            mHud.setBrightness(brightness);
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
                if (null != NotificationMonitor.getInstance()) {
                    NotificationMonitor.getInstance().updateArrow(Arrow.Convergence);
                }
                break;

            case R.id.button2:
                if (null != NotificationMonitor.getInstance()) {
                    NotificationMonitor.getInstance().updateArrow(Arrow.LeaveRoundaboutSharpRightCC);
                }
                break;

            case R.id.button3:
                if (null != NotificationMonitor.getInstance()) {
                    NotificationMonitor.getInstance().updateArrow(Arrow.LeaveRoundaboutSharpRight);
                }
                break;

            case R.id.btnListNotify:
                log("List notifications...");
                listCurrentNotification();
                break;

            case R.id.btnScanHUD:
                log("Scan for HUD...");
                if (!IGNORE_BT_DEVICE) {
                    if (mHud != null) {
                        mHud.scanForHud();
                    }
                }
                break;

            case R.id.btnResetHUD:
                log("Reset HUD...");
                if (!IGNORE_BT_DEVICE) {
                    initializeHUD();
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
            mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                return false;
            }
            if (!mLocationServiceConnected) {
                //Here, the Location Service gets bound and the GPS Speedometer gets Active.
                bindLocationService();
                mUseLocationService = true;
            }
        } else {
            //do not show speed
            if (mLocationServiceConnected) {
                unbindLocationService();
            }
            if (null != mHud) {
                //clear according to navigate status
                if (isInNavigation()) {
                    mHud.clearSpeedAndWarning();
                } else {
                    mHud.clearDistance();
                }
            }
            mUseLocationService = false;
        }
        return true;
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
        if (mIsNLSEnabled) {
            if (NotificationMonitor.getCurrentNotifications() == null) {
                result = "No Notifications Capture!!!\nSometimes reboot device or re-install app can resolve this problem.";
                log(result);
                mDebugTextView.setText(result);
                return;
            }
            int n = NotificationMonitor.sCurrentNotificationsCounts;
            if (n == 0) {
                result = getResources().getString(R.string.active_notification_count_zero);
            } else {
                result = getResources().getQuantityString(R.plurals.active_notification_count_nonzero, n, n);
            }
            result = result + "\n" + getCurrentNotificationString();
            updateTextViewDebug(result);
        } else {
            mDebugTextView.setTextColor(Color.RED);
            mDebugTextView.setText(
                    getString(R.string.message_enable_notification_access,
                            getString(R.string.app_name)));
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
    private void bindLocationService() {
        if (mLocationServiceConnected) {
            return;
        }
        Intent i = new Intent(getApplicationContext(), LocationService.class);
        bindService(i, mLocationServiceConnection, BIND_AUTO_CREATE);
        mLocationServiceConnected = true;
    }

    // unbind/deactivate LocationService
    private void unbindLocationService() {
        if (!mLocationServiceConnected) {
            return;
        }
        unbindService(mLocationServiceConnection);
        mLocationServiceConnected = false;
    }

    // This method check if GPS is activated (and ask user for activation)
    private boolean checkGps() {
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
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
        CharSequence originalText = mDebugTextView.getText();
        if (msg.length() + originalText.length() > 1000) {
            mDebugTextView.setText("");
        }
        mDebugTextView.append(msg + "\n\n");
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
                updateTextViewDebug(notify_msg);
                return;
            }

            //=======================================================================
            // gps speed
            //=======================================================================
            boolean has_gps_speed = intent.hasExtra(getString(R.string.gps_speed));
            if (has_gps_speed) {
                double speed = intent.getDoubleExtra(getString(R.string.gps_speed), 0);
                int int_speed = (int) Math.round(speed);
                mGpsSpeed = int_speed;

                final boolean show_speed = mShowSpeedSwitch.isChecked();
                if (show_speed) {
                    setSpeed(int_speed);
                }

                updateTextViewDebug(getString(R.string.layout_debug_text_speed, int_speed));
                return;
            }

            //=======================================================================
            // for UI usage, parse notify_parse_failed first
            //=======================================================================
            boolean notify_parse_failed = intent.getBooleanExtra(getString(R.string.notify_parse_failed), false);

            if (notify_parse_failed) {
                //when pass fail
                mGmapsNotificationCaughtSwitch.setChecked(false);
                mIsNavigating = false;
            } else {
                //pass success
                final boolean notify_catched = intent.getBooleanExtra(getString(R.string.notify_catched),
                        mNotificationCaughtSwitch.isChecked());
                final boolean gmaps_notify_catched = intent.getBooleanExtra(getString(R.string.gmaps_notify_catched),
                        mGmapsNotificationCaughtSwitch.isChecked());


                final boolean is_in_navigation_in_intent = intent.getBooleanExtra(getString(R.string.is_in_navigation), mIsNavigating);

                if (!notify_catched) {
                    //no notify catched
                    mNotificationCaughtSwitch.setChecked(false);
                    mGmapsNotificationCaughtSwitch.setChecked(false);
                } else {
                    mNotificationCaughtSwitch.setChecked(true);
                    // we need two condition to confirm in navagating:
                    // 1. gmaps's notify
                    // 2. in_navigation from notify monitor
                    final boolean is_really_in_navigation = gmaps_notify_catched && is_in_navigation_in_intent;
                    mGmapsNotificationCaughtSwitch.setChecked(is_really_in_navigation);

                    if (mLastReallyInNavigation != is_really_in_navigation &&
                            !is_really_in_navigation &&
                            null != mHud) {
                        //exit navigation
                        mHud.setDirection(eOutAngle.AsDirection);
                        //maybe in this line
                    }
                    mIsNavigating = is_really_in_navigation;
                    mLastReallyInNavigation = is_really_in_navigation;
                }
            }

            //=======================================================================
            if (intent.hasExtra(getString(R.string.option_arrow_type))) {
                //re-sync arrow type between ui & notify monitor
                boolean arrowTypeV2_in_ui = mArrowTypeSwitch.isChecked();
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
    private static final String[] PAGE_TITLES = new String[]{
            "Main",
            "Setup",
            "Debug"
    };

    // The fragments that are used as the individual pages
    private static final Fragment[] PAGES = new Fragment[]{
            new Page1Fragment(),
            new Page3Fragment(),
            new Page2Fragment()
    };

    // PagerAdapter for supplying the ViewPager with the pages (fragments) to display.
    private static class MyPagerAdapter extends FragmentPagerAdapter {
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

    //================================================================================
    // media projection
    //================================================================================
    private MediaProjectionManager mProjectionManager;
    private ImageReader mImageReader;
    private Handler mProjectionHandler;
    private Display mDisplay;
    private VirtualDisplay mVirtualDisplay;
    private int mDensity;
    public int mWidth;
    public int mHeight;
    private int mRotation;
    private OrientationChangeCallback mOrientationChangeCallback;
    public int mAlertSpeedExceeds = 0;
    public int mGpsSpeed = 0;
    public boolean mAlertYellowTraffic = false;

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
    private ImageDetectListener mImageDetectListener;

    private void createVirtualDisplay() {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        mWidth = dm.widthPixels;
        mHeight = dm.heightPixels;

        // start capture reader
        mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 2);
        mVirtualDisplay = sMediaProjection.createVirtualDisplay(SCREENCAP_NAME, mWidth, mHeight, mDensity, VIRTUAL_DISPLAY_FLAGS, mImageReader.getSurface(), null, mProjectionHandler);
        mImageReader.setOnImageAvailableListener(mImageDetectListener, mProjectionHandler);
    }
}

