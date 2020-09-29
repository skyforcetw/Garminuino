package sky4s.garminhud.app;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import sky4s.garminhud.Arrow;
import sky4s.garminhud.ArrowImage;
import sky4s.garminhud.ArrowV2;
import sky4s.garminhud.ImageUtils;
import sky4s.garminhud.eOutAngle;
import sky4s.garminhud.eOutType;
import sky4s.garminhud.eUnits;
import sky4s.garminhud.hud.HUDInterface;

public class NotificationMonitor extends NotificationListenerService {
    private static final boolean STORE_IMG = true;

    private static final String IMAGE_DIR = MainActivity.SCREENCAP_STORE_DIRECTORY;

    public static final String GOOGLE_MAPS_PACKAGE_NAME = "com.google.android.apps.maps";
    public static final String GOOGLE_MAPS_GO_PACKAGE_NAME = "com.google.android.apps.navlite";
    public static final String GOOGLE_MAPS_NOTIFICATION_GROUP_NAVIGATION = "navigation_status_notification_group";

    public static final String OSMAND_PACKAGE_NAME = "net.osmand";
    public static final String OSMAND_NOTIFICATION_GROUP_NAVIGATION = "NAVIGATION";
    public static final String SYGIC_PACKAGE_NAME = "com.sygic.aura";

    private static final String TAG = NotificationMonitor.class.getSimpleName();
    private static final int EVENT_UPDATE_CURRENT_NOS = 0;

    public static List<StatusBarNotification[]> sCurrentNotifications = new ArrayList<StatusBarNotification[]>();
    public static int sCurrentNotificationsCounts = 0;
    public static StatusBarNotification sPostedNotification;
    public static StatusBarNotification sRemovedNotification;

    static HUDInterface sHud = null;

    @SuppressLint("HandlerLeak")
    private Handler mMonitorHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_UPDATE_CURRENT_NOS:
                    updateCurrentNotifications();
                    break;
                default:
                    break;
            }
        }
    };

    private long mLastNotifyTimeMillis = 0;
    private long mNotifyPeriodTime = 0;

    private Arrow mFoundArrow = Arrow.None;
    private Arrow mLastFoundArrow = Arrow.None;
    private ArrowV2 mFoundArrowV2 = ArrowV2.None;
    private ArrowV2 mLastFoundArrowV2 = ArrowV2.None;

    private String mDistanceNum = null;
    private static String mDistanceUnit = null;
    private String mRemainingHours = null;
    private String mRemainingMinutes = null;
    private String mRemainingDistance = null;
    private String mRemainingDistanceUnits = null;
    private int mArrivalHours = -1;
    private int mArrivalMinutes = -1;
    private int mLastArrivalHours = -1;
    private int mLastArrivalMinutes = -1;

    private ExecutorService mExecutor;
    private RejectedExecutionHandler mRejectHandler = new ThreadPoolExecutor.DiscardOldestPolicy();

    private static void logi(String msg) {
        Log.i(TAG, msg);
    }

    public static StatusBarNotification[] getCurrentNotifications() {
        if (sCurrentNotifications.size() == 0) {
            logi("mCurrentNotifications size is ZERO!!");
            return null;
        }
        return sCurrentNotifications.get(0);
    }

    private MsgReceiver mMsgReceiver;

    private class MsgReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Parcelable parcelablelocation = intent.getParcelableExtra(getString(R.string.location));
            mShowETA = intent.getBooleanExtra(getString(R.string.option_show_eta), mShowETA);
            mLastArrivalMinutes = -1; // Force to switch to ETA after several toggles
            mBusyTraffic = intent.getBooleanExtra(getString(R.string.busy_traffic), mBusyTraffic);
            mArrowTypeV2 = intent.getBooleanExtra(getString(R.string.option_arrow_type), mArrowTypeV2);
        }
    }

    private boolean mShowETA = false;

    private static NotificationMonitor sStaticInstance;

    public static NotificationMonitor getInstance() {
        return sStaticInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Ensure queue notifications are around 1 second old
        final int maxQueueSize = sHud.getMaxUpdatesPerSecond();
        mExecutor = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(maxQueueSize),
                mRejectHandler);

        logi("onCreate...");
        mMonitorHandler.sendMessage(mMonitorHandler.obtainMessage(EVENT_UPDATE_CURRENT_NOS));

        //========================================================================================
        // message receiver
        //========================================================================================
        mMsgReceiver = new MsgReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(getString(R.string.broadcast_receiver_notification_monitor));

        registerReceiver(mMsgReceiver, intentFilter);

        //========================================================================================
        sStaticInstance = this;
        mPostman = MainActivityPostman.toMainActivityInstance(this, getString(R.string.broadcast_sender_notification_monitor));

        //========================================================================================

        loadArrowImagesInAssets();
    }

    private Bitmap[] mArrowBitmaps;
    private MainActivityPostman mPostman;

    private void loadArrowImagesInAssets() {
        final String dir = "arrow3";
        AssetManager assetManager = getApplicationContext().getAssets();

        try {
            String[] filePathList = assetManager.list(dir);
            mArrowBitmaps = new Bitmap[filePathList.length];

            for (int i = 0; i < filePathList.length; i++) {
                InputStream is = assetManager.open(dir + "/" + filePathList[i]);
                mArrowBitmaps[i] = BitmapFactory.decodeStream(is);
                is.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mMsgReceiver);
        mExecutor.shutdown();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        logi("onBind...");
        return super.onBind(intent);
    }


    private void processNotification(StatusBarNotification sbn) {
        // Parsing a notification may be slow, do this in the background
        mExecutor.execute(() -> {
            mPostman.addBooleanExtra(getString(R.string.notify_catched), true);
            mPostman.sendIntent2MainActivity();

            Notification notification = sbn.getNotification();
            if (null != notification) {
                String packageName = sbn.getPackageName();
                switch (packageName) {
                    case GOOGLE_MAPS_PACKAGE_NAME:
                        parseGmapsNotification(notification);
                        break;
                    /*
                    case GOOGLE_MAPS_GO_PACKAGE_NAME:
                        parseGmapsGoNotificationByReflection(notification);
                        break;
                    case OSMAND_PACKAGE_NAME:
                        parseOsmandNotification(notification);
                        break;
                    case SYGIC_PACKAGE_NAME:
                        parseSygicNotification(notification);
                        break;
                    */
                    default:
                        String notifyMessage = "No gmaps' notification found!?!?";
                        mPostman.addStringExtra(getString(R.string.notify_msg), notifyMessage);
                        mPostman.sendIntent2MainActivity();
                }
            } else {
                mPostman.addBooleanExtra(getString(R.string.notify_catched), true);
                mPostman.addBooleanExtra(getString(R.string.is_in_navigation), false);
                mPostman.sendIntent2MainActivity();

                String notifyMessage = "No notification found!?!?";
                mPostman.addStringExtra(getString(R.string.notify_msg), notifyMessage);
                mPostman.sendIntent2MainActivity();
            }
        });
    }

    private void parseSygicNotification(Notification notification) {
        long currentTime = System.currentTimeMillis();
        mNotifyPeriodTime = currentTime - mLastNotifyTimeMillis;
        mLastNotifyTimeMillis = currentTime;

        parseSygicNotificationByExtras(notification);
    }

    private void parseSygicNotificationByExtras(Notification notification) {
        if (null == notification) {
            return;
        }

        Bundle extras = notification.extras;

        if ((null != extras)) {
            Object big = extras.get(Notification.EXTRA_BIG_TEXT);

            Object titleObj = extras.get(Notification.EXTRA_TITLE);
            Object textObj = extras.get(Notification.EXTRA_TEXT);
            Object subTextObj = extras.get(Notification.EXTRA_SUB_TEXT);

            String title = null != titleObj ? titleObj.toString() : null;
            String text = null != textObj ? textObj.toString() : null;
            String subText = null != subTextObj ? subTextObj.toString() : null;
            subText = null == subText ? text : subText;

            Icon large = notification.getLargeIcon();
            Icon small = notification.getSmallIcon();
            if (null != small) {
                Drawable drawableIco = small.loadDrawable(this);
                Bitmap bitmapImage = ImageUtils.drawableToBitmap(drawableIco);

                if (null != bitmapImage) {
                    if (STORE_IMG) {
                        ImageUtils.storeBitmap(bitmapImage, IMAGE_DIR, "arrow0_sygic.png");
                    }
                    bitmapImage = ImageUtils.removeAlpha(bitmapImage);
                    if (STORE_IMG) {
                        ImageUtils.storeBitmap(bitmapImage, IMAGE_DIR, "arrow_sygic.png");
                    }
                }
            }
            // Check if subText is empty (" ·  · ") --> don't parse subText
            // Occurs for example on NagivationChanged
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void parseOsmandNotification(Notification notification) {
        long currentTime = System.currentTimeMillis();
        mNotifyPeriodTime = currentTime - mLastNotifyTimeMillis;
        mLastNotifyTimeMillis = currentTime;

        parseOsmandNotificationByExtras(notification);
    }

    private void parseGmapsNotification(Notification notification) {
        long currentTime = System.currentTimeMillis();
        mNotifyPeriodTime = currentTime - mLastNotifyTimeMillis;
        mLastNotifyTimeMillis = currentTime;

        boolean parseResult = parseGmapsNotificationByExtras(notification);
        if (!parseResult) {
            //gmap on android 6.0 need parsing by reflection
            parseResult = parseGmapsNotificationByReflection(notification);
        }

        if (!parseResult) {
            mPostman.addBooleanExtra(getString(R.string.notify_parse_failed), true);
            mPostman.addBooleanExtra(getString(R.string.gmaps_notify_catched), true);
            mPostman.addBooleanExtra(getString(R.string.is_in_navigation), false);
            mPostman.sendIntent2MainActivity();

            String notifyMessage = "Notify parsing failed.";
            mPostman.addStringExtra(getString(R.string.notify_msg), notifyMessage);
        } else {
            mPostman.addBooleanExtra(getString(R.string.notify_parse_failed), false);
            mPostman.addBooleanExtra(getString(R.string.gmaps_notify_catched), true);
            mPostman.addBooleanExtra(getString(R.string.is_in_navigation), mIsNavigating);
            mPostman.addBooleanExtra(getString(R.string.option_arrow_type), mArrowTypeV2);
        }
        mPostman.sendIntent2MainActivity();
    }

    private static boolean viewsHasActionsField(RemoteViews views) {
        try {
            Class<?> viewsClass = views.getClass();
            viewsClass.getDeclaredField("mActions");
            return true;
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            return false;
        }
    }

    private static Object getObjectProperty(Object object, String propertyName) {
        try {
            Field f = object.getClass().getDeclaredField(propertyName);
            f.setAccessible(true);
            return f.get(object);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static RemoteViews getRemoteViews(Notification notification) {
        // We have to extract the information from the view
        RemoteViews views = notification.bigContentView;
        if (!viewsHasActionsField(views)) {
            //check mActions is exist, we use it to parse notification
            views = null;
        }
        if (views == null) {
            views = notification.contentView;
        }
        if (!viewsHasActionsField(views)) {
            //check mActions again
            views = null;
        }
        return views;
    }

    private boolean parseGmapsGoNotificationByReflection(Notification notification) {
        RemoteViews views = getRemoteViews(notification);
        if (views == null) return false;

        // Use reflection to examine the m_actions member of the given RemoteViews object.
        // It's not pretty, but it works.
        try {

            Class<?> viewsClass = views.getClass();
            Field fieldActions = viewsClass.getDeclaredField("mActions");
            fieldActions.setAccessible(true);

            @SuppressWarnings("unchecked")
            ArrayList<Parcelable> actions = (ArrayList<Parcelable>) fieldActions.get(views);

            int indexOfActions = 0;

            // Find the setText() and setTime() reflection actions
            for (Parcelable action : actions) {
                Object methodName = getObjectProperty(action, "methodName");
                Object type = getObjectProperty(action, "type");
                Object value = getObjectProperty(action, "value");

                if (methodName == null) {
                    continue;
                    // Save strings
                } else if (methodName.equals("setText")) {
                    if (value instanceof String) {
                        switch (indexOfActions) {
                            case 4:
                                //distance
                                break;
                            case 6:
                                //road
                                break;
                            case 9:
                                //time
                                break;
                        }
                    }
                } else if (methodName.equals("setImageBitmap")) {
                    Object bitmapId = getObjectProperty(action, "bitmapId");
                    Object bitmapCache = getObjectProperty(views, "mBitmapCache");
                    Object bitmapsObject = getObjectProperty(bitmapCache, "mBitmaps");

                    if (bitmapId instanceof Integer && null != bitmapsObject) {
                        Integer integerBitmapId = (Integer) bitmapId;
                        ArrayList<Bitmap> bitmapList = (ArrayList<Bitmap>) bitmapsObject;

                        Bitmap bitmapImage = bitmapList.get(integerBitmapId);
                        if (STORE_IMG) {
                            ImageUtils.storeBitmap(bitmapImage, IMAGE_DIR, "arrow0.png");
                        }
                        bitmapImage = ImageUtils.removeAlpha(bitmapImage);
                        if (STORE_IMG) {
                            ImageUtils.storeBitmap(bitmapImage, IMAGE_DIR, "arrow.png");
                        }
                        ArrowImage arrowImage = new ArrowImage(bitmapImage);
                        mFoundArrow = getArrow(arrowImage);
                    }
                }
                indexOfActions++;
            }

            logParseMessage();
            return true;
        }
        // It's not usually good style to do this, but then again, neither is the use of reflection...
        catch (Exception e) {
            Log.e(TAG, e.toString());
            return false;
        }
    }

    /**
     * @param notification notification
     * @return true is successful, false if not
     */
    private boolean parseGmapsNotificationByReflection(Notification notification) {
        RemoteViews views = getRemoteViews(notification);
        if (views == null) return false;

        // Use reflection to examine the m_actions member of the given RemoteViews object.
        // It's not pretty, but it works.
        try {

            Class<?> viewsClass = views.getClass();
            Field fieldActions = viewsClass.getDeclaredField("mActions");
            fieldActions.setAccessible(true);

            @SuppressWarnings("unchecked")
            ArrayList<Parcelable> actions = (ArrayList<Parcelable>) fieldActions.get(views);

            int indexOfActions = 0;
            int updateCount = 0;
            boolean inNavigation = false;

            // Find the setText() and setTime() reflection actions
            for (Parcelable p : actions) {
                Parcel parcel = Parcel.obtain();
                if (null == p) {
                    continue;
                }
                p.writeToParcel(parcel, 0);
                parcel.setDataPosition(0);

                // The tag tells which type of action it is (2 is ReflectionAction, from the source)
                int tag = parcel.readInt();
                String simpleClassName = p.getClass().getSimpleName();

                if ((tag != 2 && tag != 12) && (!simpleClassName.equals("ReflectionAction") && !simpleClassName.equals("BitmapReflectionAction")))
                    continue;

                String methodName = parcel.readString();

                if (methodName == null) continue;
                    // Save strings
                else if (methodName.equals("setText")) {
                    // Parameter type (10 = Character Sequence)
                    parcel.readInt();

                    // Store the actual string
                    String t = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel).toString().trim();
                    switch (indexOfActions) {
                        case 2:
                            inNavigation = t.equalsIgnoreCase(getString(R.string.exit_navigation));
                            break;
                        case 3:
                            //distance to turn
                            parseDistanceToTurn(t);
                            break;
                        case 8:
                            //time, distance, arrived time
                            parseTimeAndDistanceToDest(t);
                            updateCount++;
                            break;
                        default:
                            break;
                    }
                } else if (methodName.equals("setImageBitmap")) {
                    int bitmapId = parcel.readInt();
                    Field fieldBitmapCache = views.getClass().getDeclaredField("mBitmapCache");
                    fieldBitmapCache.setAccessible(true);

                    Object bitmapCache = fieldBitmapCache.get(views);
                    Field fieldBitmaps = bitmapCache.getClass().getDeclaredField("mBitmaps");
                    fieldBitmaps.setAccessible(true);
                    Object bitmapsObject = fieldBitmaps.get(bitmapCache);

                    if (null != bitmapsObject) {
                        ArrayList<Bitmap> bitmapList = (ArrayList<Bitmap>) bitmapsObject;
                        Bitmap bitmapImage = bitmapList.get(bitmapId);
                        if (STORE_IMG) {
                            ImageUtils.storeBitmap(bitmapImage, IMAGE_DIR, "arrow0.png");
                        }
                        bitmapImage = ImageUtils.removeAlpha(bitmapImage);
                        if (STORE_IMG) {
                            ImageUtils.storeBitmap(bitmapImage, IMAGE_DIR, "arrow.png");
                        }
                        ArrowImage arrowImage = new ArrowImage(bitmapImage);
                        mFoundArrow = getArrow(arrowImage);
                        updateCount++;
                    }
                }

                parcel.recycle();
                indexOfActions++;
            }

            logParseMessage();
            //can update to garmin hud
            if (0 != updateCount && inNavigation) {
                updateHudInformation();
            }
            return true;
        }
        // It's not usually good style to do this, but then again, neither is the use of reflection...
        catch (Exception e) {
            Log.e(TAG, e.toString());
            return false;
        }
    }

    private void logParseMessage() {
        String arrowString = mArrowTypeV2 ? mFoundArrowV2.toString() : mFoundArrow.toString();
        String notifyMessage = arrowString + "(" + (mArrowTypeV2 ? "v2:" : "v1:") + sArrowMinSad + ") " +
                mDistanceNum + "/" + mDistanceUnit + " " +
                (null == mRemainingHours ? 0 : mRemainingHours) + ":" + mRemainingMinutes + " " +
                mRemainingDistance + mRemainingDistanceUnits + " " +
                mArrivalHours + ":" + mArrivalMinutes +
                " busy: " + (mBusyTraffic ? "1" : "0") +
                " (period: " + mNotifyPeriodTime + ")";
        logi(notifyMessage);

        mPostman.addStringExtra(getString(R.string.notify_msg), notifyMessage);
        mPostman.sendIntent2MainActivity();

        mPostman.addBooleanExtra(getString(R.string.is_in_navigation), mIsNavigating);
        mPostman.sendIntent2MainActivity();
    }

    private boolean mIsNavigating = false;

    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean parseOsmandNotificationByExtras(Notification notification) {
        if (null == notification) {
            return false;
        }
        Bundle extras = notification.extras;
        String group_name = notification.getGroup();

        if ((null != extras) && (null != group_name) && group_name.equals(OSMAND_NOTIFICATION_GROUP_NAVIGATION)) {
            Object remainingTime = extras.get(Notification.EXTRA_BIG_TEXT);
            Object distance = extras.get(Notification.EXTRA_TITLE);

            Icon largeIcon = notification.getLargeIcon();
            Icon smallIcon = notification.getSmallIcon();

            Object msg = extras.get(Notification.EXTRA_MESSAGES);
            Object textObj = extras.get(Notification.EXTRA_TEXT);
            Object subTextObj = extras.get(Notification.EXTRA_SUB_TEXT);

            if (null != largeIcon) {
                Drawable drawableIco = largeIcon.loadDrawable(this);
                Bitmap bitmapImage = ImageUtils.drawableToBitmap(drawableIco);

                if (null != bitmapImage) {
                    if (STORE_IMG) {
                        ImageUtils.storeBitmap(bitmapImage, IMAGE_DIR, "arrow0_osm.png");
                    }
                    bitmapImage = ImageUtils.removeAlpha(bitmapImage);
                    if (STORE_IMG) {
                        ImageUtils.storeBitmap(bitmapImage, IMAGE_DIR, "arrow_osm.png");
                    }
                }
            }
            return true;
        }
        return false;
    }

    private static String parseString(Object o) {
        return null != o ? o.toString() : null;
    }

    private boolean parseGmapsNotificationByExtras(Notification notification) {
        if (null == notification) {
            return false;
        }
        Bundle extras = notification.extras;
        String group_name = notification.getGroup();

        if ((null != extras) && (null != group_name) && group_name.equals(GOOGLE_MAPS_NOTIFICATION_GROUP_NAVIGATION)) {
            //not in navigation(chinese) of title: 參考 Google 地圖行駛
            Object titleObj = extras.get(Notification.EXTRA_TITLE);
            Object textObj = extras.get(Notification.EXTRA_TEXT);
            Object subTextObj = extras.get(Notification.EXTRA_SUB_TEXT);

            String title = parseString(titleObj);
            String text = parseString(textObj);
            String subText = parseString(subTextObj);
            subText = null == subText ? text : subText;

            // Check if subText is empty (" ·  · ") --> don't parse subText
            // Occurs for example on NavigationChanged
            boolean subTextEmpty = true;
            if (null != subText) {
                String[] split = subText.split("·");
                for (String s : split) {
                    String trimString = s.trim();
                    boolean string_empty = containsOnlyWhitespaces(trimString);
                    if (!string_empty) {
                        subTextEmpty = false;
                        break;
                    }
                }
            }

            final boolean somethingCanParse = null != subText && !subTextEmpty;
            if (somethingCanParse) {
                parseTimeAndDistanceToDest(subText);

                String[] title_str = title.split("–");
                title_str = 1 == title_str.length ? title.split("-") : title_str;
                String distance = title_str[0].trim();
                if (Character.isDigit(distance.charAt(0))) {
                    parseDistanceToTurn(distance);
                } else {
                    mDistanceNum = "-1";
                }

                Icon largeIcon = notification.getLargeIcon();
                if (null != largeIcon) {
                    Drawable drawableIco = largeIcon.loadDrawable(this);
                    Bitmap bitmapImage = ImageUtils.drawableToBitmap(drawableIco);

                    if (null != bitmapImage) {
                        if (STORE_IMG) {
//                            if (!ImageUtils.storeBitmapQ(bitmapImage, "arrow0.png")) {
                            if (!ImageUtils.storeBitmap(bitmapImage, IMAGE_DIR, "arrow0.png")) {
                                Log.d(TAG, "Store arrow bitmap failed.");
                            }
                        }

                        if (mArrowTypeV2) {
                            mFoundArrowV2 = getArrowV2(bitmapImage);
                            mLastFoundArrowV2 = mFoundArrowV2;
                        } else {
                            ArrowImage arrowImage = new ArrowImage(bitmapImage);
                            mFoundArrow = getArrow(arrowImage);
                            mLastFoundArrow = mFoundArrow;
                        }
                    }
                }
                logParseMessage();
                updateHudInformation();
                mIsNavigating = true;
                return true;
            } else {
                mIsNavigating = false;
                return false;
            }

        } else {
            return false;
        }
    }

    private static eUnits get_eUnits(String unit) {
        if (null == unit) {
            return eUnits.None;
        }

        switch (unit) {
            case "km":
                return eUnits.Kilometres;
            case "m":
                return eUnits.Metres;
            case "mi":
                return eUnits.Miles;
            case "ft":
                return eUnits.Foot;
            default:
                return eUnits.None;
        }
    }

    // Translates the units (distance and time) from local language and charset in common values
    private String translate(String local_language_string) {
        if (local_language_string.equalsIgnoreCase(getString(R.string.km))) {
            return "km";
        } else if (local_language_string.equalsIgnoreCase(getString(R.string.meter))) {
            return "m";
        } else if (local_language_string.equalsIgnoreCase(getString(R.string.feet))) {
            return "ft";
        } else if (local_language_string.equalsIgnoreCase(getString(R.string.miles))) {
            return "mi";
        } else if (local_language_string.equalsIgnoreCase(getString(R.string.minute))) {
            return "m";
        } else if (local_language_string.equalsIgnoreCase(getString(R.string.minute2))) {
            return "m";
        } else if (local_language_string.equalsIgnoreCase(getString(R.string.hour))) {
            return "h";
        } else if (local_language_string.equalsIgnoreCase(getString(R.string.hour2))) {
            return "h";
        }
        return null;
    }

    // Returns the current Unit (Kilometres or Miles) based on distanceToTurn
    public static eUnits getCurrentUnit() {
        if (get_eUnits(mDistanceUnit) == eUnits.Kilometres ||
                get_eUnits(mDistanceUnit) == eUnits.Metres) {
            return eUnits.Kilometres;
        } else if (get_eUnits(mDistanceUnit) == eUnits.Miles ||
                get_eUnits(mDistanceUnit) == eUnits.Foot) {
            return eUnits.Miles;
        }
        return eUnits.None;
    }

    private static Arrow getArrow(ArrowImage image) {
        sArrowMinSad = Integer.MAX_VALUE;
        Arrow minSadArrow = Arrow.None;

        for (Arrow a : Arrow.values()) {
            int sad = image.getSAD(a.valueLeft);
            if (sad < sArrowMinSad) {
                sArrowMinSad = sad;
                minSadArrow = a;
            }
            if (0 == sad) {
                String integerString = Long.toString(a.valueLeft);
                Log.d(TAG, "Recognize " + a.name() + " " + integerString);
                return a;

            }
        }
        Log.d(TAG, "No Recognize, minSad: " + sArrowMinSad + " arrow:" + minSadArrow);
        return minSadArrow;
    }

    static int sArrowMinSad = 0;

    private static int getGreenSAD(Bitmap image1, Bitmap image2) {
        if (null == image1 || null == image2) {
            return -1;
        }
        if (image1.getWidth() != image2.getWidth() || image1.getHeight() != image2.getHeight()) {
            return -1;
        }
        final int height = image1.getHeight();
        final int width = image1.getWidth();
        int sad = 0;
        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                int pixel1 = image1.getPixel(w, h);
                int pixel2 = image2.getPixel(w, h);
                int green1 = Color.green(pixel1);
                int green2 = Color.green(pixel2);
                sad += Math.abs(green1 - green2);
            }
        }
        return sad;
    }

    private static int getNotWhiteSAD(Bitmap image1, Bitmap image2) {
        if (null == image1 || null == image2) {
            return -1;
        }
        if (image1.getWidth() != image2.getWidth() || image1.getHeight() != image2.getHeight()) {
            return -1;
        }
        final int height = image1.getHeight();
        final int width = image1.getWidth();
        int sad = 0;
        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                int pixel1 = image1.getPixel(w, h);
                int pixel2 = image2.getPixel(w, h);
                int green1 = Color.green(pixel1);
                int green2 = Color.green(pixel2);
                if (!(green1 == green2 && green1 >= 250)) {
                    sad++;
                }
            }
        }
        return sad;
    }

    private ArrowV2 getArrowV2(Bitmap image) {
        if (null == mArrowBitmaps) {
            return null;
        }
        final ArrowV2[] arrows = ArrowV2.values();
        if (mArrowBitmaps.length + 1 != arrows.length) {
            return null;
        }

        final int targetWidth = mArrowBitmaps[0].getWidth();
        final int targetHeight = mArrowBitmaps[0].getHeight();
        Bitmap scaleImage = ImageUtils.getScaleBitmap(image, targetWidth, targetHeight);
        scaleImage = ImageUtils.removeAlpha(scaleImage);
        final int length = mArrowBitmaps.length;

        sArrowMinSad = Integer.MAX_VALUE;
        int minSADIndex = -1;
        for (int x = 0; x < length; x++) {
            int sad = getNotWhiteSAD(scaleImage, mArrowBitmaps[x]);
            if (-1 != sad && sad < sArrowMinSad) {
                sArrowMinSad = sad;
                minSADIndex = x;
            }
        }

        ArrowV2 arrow = arrows[minSADIndex];
        if (0 == sArrowMinSad) {
            Log.d(TAG, "Recognize " + arrow.name());
        } else {
            Log.d(TAG, "No Recognize, minSad: " + sArrowMinSad + " arrow:" + arrow);
        }
        return arrow;
    }

    private static ArrowV2 getArrowV2(ArrowImage image) {
        sArrowMinSad = Integer.MAX_VALUE;
        ArrowV2 minSadArrow = ArrowV2.None;

        for (ArrowV2 a : ArrowV2.values()) {
            int sad = image.getSAD(a.valueLeft);
            if (sad < sArrowMinSad) {
                sArrowMinSad = sad;
                minSadArrow = a;
            }
            if (0 == sad) {
                String integerString = Long.toString(a.valueLeft);
                Log.d(TAG, "Recognize " + a.name() + " " + integerString);
                return a;
            }
        }
        Log.d(TAG, "No Recognize, minSad: " + sArrowMinSad + " arrow:" + minSadArrow);
        return minSadArrow;
    }

    private Arrow mPreArrowV1 = Arrow.None;

    void updateArrow(Arrow arrow) {
        if (null == sHud) {
            return;
        }
        if (mPreArrowV1 != arrow) {
            mPreArrowV1 = arrow;
        }
        switch (arrow) {
            case Arrivals:
                sHud.setDirection(eOutAngle.Straight, eOutType.RightFlag, eOutAngle.AsDirection);
                break;

            case ArrivalsLeft:
                sHud.setDirection(eOutAngle.Left, eOutType.RightFlag, eOutAngle.AsDirection);
                break;

            case ArrivalsRight:
                sHud.setDirection(eOutAngle.Right, eOutType.RightFlag, eOutAngle.AsDirection);
                break;

            case EasyLeft:
            case KeepLeft:
                sHud.setDirection(eOutAngle.EasyLeft);
                break;

            case EasyRight:
            case KeepRight:
                sHud.setDirection(eOutAngle.EasyRight);
                break;

            case GoTo:
            case Straight:
                sHud.setDirection(eOutAngle.Straight);
                break;

            case LeaveRoundabout:
                //1 checked
            case LeaveRoundaboutLeft:
                //9 checked
                sHud.setDirection(eOutAngle.Left, eOutType.LeftRoundabout, eOutAngle.Left);
                break;

            case LeaveRoundaboutAsUTurn:
                sHud.setDirection(eOutAngle.Down, eOutType.LeftRoundabout, eOutAngle.Down);
                break;

            case LeaveRoundaboutAsUTurnCC:
                sHud.setDirection(eOutAngle.Down, eOutType.RightRoundabout, eOutAngle.Down);
                break;

            case LeaveRoundaboutEasyLeft:
                //4 checked
                sHud.setDirection(eOutAngle.EasyLeft, eOutType.LeftRoundabout, eOutAngle.EasyLeft);
                break;

            case LeaveRoundaboutEasyLeftCC:
                //5 checked
                sHud.setDirection(eOutAngle.EasyLeft, eOutType.RightRoundabout, eOutAngle.EasyLeft);
                break;

            case LeaveRoundaboutEasyRight:
                //6 checked
                sHud.setDirection(eOutAngle.EasyRight, eOutType.LeftRoundabout, eOutAngle.EasyRight);
                break;

            case LeaveRoundaboutEasyRightCC:
                //7 checked
                sHud.setDirection(eOutAngle.EasyRight, eOutType.RightRoundabout, eOutAngle.EasyRight);
                break;

            case LeaveRoundaboutCC:
                //8 checked
                sHud.setDirection(eOutAngle.Right, eOutType.RightRoundabout, eOutAngle.Right);
                break;

            case LeaveRoundaboutLeftCC:
                //10 checked
                sHud.setDirection(eOutAngle.Left, eOutType.RightRoundabout, eOutAngle.Left);
                break;

            case LeaveRoundaboutRight:
                //11 checked
                sHud.setDirection(eOutAngle.Right, eOutType.LeftRoundabout, eOutAngle.Right);
                break;

            case LeaveRoundaboutRightCC:
                //12 checked
                sHud.setDirection(eOutAngle.Right, eOutType.RightRoundabout, eOutAngle.Right);
                break;

            case LeaveRoundaboutSharpLeft:
                //13 checked
                sHud.setDirection(eOutAngle.SharpLeft, eOutType.LeftRoundabout, eOutAngle.SharpLeft);
                break;

            case LeaveRoundaboutSharpLeftCC:
                //14 checked
                sHud.setDirection(eOutAngle.SharpLeft, eOutType.RightRoundabout, eOutAngle.SharpLeft);
                break;

            case LeaveRoundaboutSharpRight:
                //15 checked
                sHud.setDirection(eOutAngle.SharpRight, eOutType.LeftRoundabout, eOutAngle.SharpRight);
                break;

            case LeaveRoundaboutSharpRightCC:
                //16
                sHud.setDirection(eOutAngle.SharpRight, eOutType.RightRoundabout, eOutAngle.SharpRight);
                break;

            case LeaveRoundaboutStraight:
                sHud.setDirection(eOutAngle.Straight, eOutType.LeftRoundabout, eOutAngle.Straight);
                break;

            case LeaveRoundaboutStraightCC:
                sHud.setDirection(eOutAngle.Straight, eOutType.RightRoundabout, eOutAngle.Straight);
                break;

            case Left:
                sHud.setDirection(eOutAngle.Left);
                break;

            case LeftDown:
                sHud.setDirection(eOutAngle.LeftDown);
                break;

            case LeftToLeave:
                sHud.setDirection(eOutAngle.EasyLeft, eOutType.LongerLane, eOutAngle.AsDirection);
                break;

            case Right:
                sHud.setDirection(eOutAngle.Right);
                break;

            case RightDown:
                sHud.setDirection(eOutAngle.RightDown);
                break;

            case RightToLeave:
                sHud.setDirection(eOutAngle.EasyRight, eOutType.LongerLane, eOutAngle.AsDirection);
                break;

            case SharpLeft:
                sHud.setDirection(eOutAngle.SharpLeft);
                break;

            case SharpRight:
                sHud.setDirection(eOutAngle.SharpRight);
                break;

            case Convergence:
            case None:
            default:
                sHud.setDirection(eOutAngle.AsDirection);
                break;
        }
    }

    private ArrowV2 mPreArrowV2 = ArrowV2.None;

    void updateArrow(ArrowV2 arrow) {
        if (null == sHud) {
            return;
        }
        if (mPreArrowV2 != arrow) {
            mPreArrowV2 = arrow;
        }
        switch (arrow) {
            case ArrivalsLeft:
                sHud.setDirection(eOutAngle.Left, eOutType.RightFlag, eOutAngle.AsDirection);
                break;

            case ArrivalsRight:
                sHud.setDirection(eOutAngle.Right, eOutType.RightFlag, eOutAngle.AsDirection);
                break;

            case EasyLeft:
            case KeepLeft:
                sHud.setDirection(eOutAngle.EasyLeft);
                break;

            case EasyRight:
            case KeepRight:
                sHud.setDirection(eOutAngle.EasyRight);
                break;

            case GoTo:
            case Straight:
                sHud.setDirection(eOutAngle.Straight);
                break;

            case LeaveRoundabout:
            case LeaveRoundaboutLeft:
                //9 checked
                //1 checked
                sHud.setDirection(eOutAngle.Left, eOutType.LeftRoundabout, eOutAngle.Left);
                break;

            case LeaveRoundaboutAsUTurn:
                sHud.setDirection(eOutAngle.Down, eOutType.LeftRoundabout, eOutAngle.Down);
                break;

            case LeaveRoundaboutAsUTurnCC:
                sHud.setDirection(eOutAngle.Down, eOutType.RightRoundabout, eOutAngle.Down);
                break;

            case LeaveRoundaboutEasyLeft:
                //4 checked
                sHud.setDirection(eOutAngle.EasyLeft, eOutType.LeftRoundabout, eOutAngle.EasyLeft);
                break;

            case LeaveRoundaboutEasyLeftCC:
                //5 checked
                sHud.setDirection(eOutAngle.EasyLeft, eOutType.RightRoundabout, eOutAngle.EasyLeft);
                break;

            case LeaveRoundaboutEasyRight:
                //6 checked
                sHud.setDirection(eOutAngle.EasyRight, eOutType.LeftRoundabout, eOutAngle.EasyRight);
                break;

            case LeaveRoundaboutEasyRightCC:
                //7 checked
                sHud.setDirection(eOutAngle.EasyRight, eOutType.RightRoundabout, eOutAngle.EasyRight);
                break;

            case LeaveRoundaboutCC:
                //8 checked
                sHud.setDirection(eOutAngle.Right, eOutType.RightRoundabout, eOutAngle.Right);
                break;

            case LeaveRoundaboutLeftCC:
                //10 checked
                sHud.setDirection(eOutAngle.Left, eOutType.RightRoundabout, eOutAngle.Left);
                break;

            case LeaveRoundaboutRight:
                //11 checked
                sHud.setDirection(eOutAngle.Right, eOutType.LeftRoundabout, eOutAngle.Right);
                break;

            case LeaveRoundaboutRightCC:
                //12 checked
                sHud.setDirection(eOutAngle.Right, eOutType.RightRoundabout, eOutAngle.Right);
                break;

            case LeaveRoundaboutSharpLeft:
                //13 checked
                sHud.setDirection(eOutAngle.SharpLeft, eOutType.LeftRoundabout, eOutAngle.SharpLeft);
                break;

            case LeaveRoundaboutSharpLeftCC:
                //14 checked
                sHud.setDirection(eOutAngle.SharpLeft, eOutType.RightRoundabout, eOutAngle.SharpLeft);
                break;

            case LeaveRoundaboutSharpRight:
                //15 checked
                sHud.setDirection(eOutAngle.SharpRight, eOutType.LeftRoundabout, eOutAngle.SharpRight);
                break;

            case LeaveRoundaboutSharpRightCC:
                //16
                sHud.setDirection(eOutAngle.SharpRight, eOutType.RightRoundabout, eOutAngle.SharpRight);
                break;

            case LeaveRoundaboutStraight:
                sHud.setDirection(eOutAngle.Straight, eOutType.LeftRoundabout, eOutAngle.Straight);
                break;

            case LeaveRoundaboutStraightCC:
                sHud.setDirection(eOutAngle.Straight, eOutType.RightRoundabout, eOutAngle.Straight);
                break;

            case Left:
                sHud.setDirection(eOutAngle.Left);
                break;

            case LeftDown:
                sHud.setDirection(eOutAngle.LeftDown);
                break;

            case Right:
                sHud.setDirection(eOutAngle.Right);
                break;
            case RightDown:
                sHud.setDirection(eOutAngle.RightDown);
                break;

            case SharpLeft:
                sHud.setDirection(eOutAngle.SharpLeft);
                break;
            case SharpRight:
                sHud.setDirection(eOutAngle.SharpRight);
                break;

            case Convergence:
            case None:
            default:
                sHud.setDirection(eOutAngle.AsDirection);
                break;
        }
    }

    private boolean mBusyTraffic = false;
    private boolean mArrowTypeV2 = false;

    private void updateHudInformation() {
        Log.i(TAG, "hud: " + sHud);

        //===================================================================================
        // distance
        //===================================================================================
        if (null != mDistanceNum && null != mDistanceUnit) {
            float float_distance = Float.parseFloat(mDistanceNum);
            int int_distance = (int) float_distance;
            eUnits units = get_eUnits(mDistanceUnit);

            if (null != sHud) {
                if (-1 != int_distance) {
                    sHud.setDistance(float_distance, units);
                } else {
                    sHud.clearDistance();
                }
            }
        } else {
            if (null != sHud) {
                sHud.clearDistance();
            }
        }

        final boolean distanceSendResult = null != sHud && sHud.getSendResult();
        //===================================================================================

        //===================================================================================
        // remaining distance
        //===================================================================================
        if (null != mRemainingDistance && null != mRemainingDistanceUnits) {
            float float_distance = Float.parseFloat(mRemainingDistance);
            int int_distance = (int) float_distance;
            eUnits units = get_eUnits(mRemainingDistanceUnits);

            if (null != sHud) {
                if (-1 != int_distance) {
                    sHud.setRemainingDistance(float_distance, units);
                } else {
                    sHud.clearRemainingDistance();
                }
            }
        } else {
            if (null != sHud) {
                sHud.clearRemainingDistance();
            }
        }

        final boolean remainingDistanceSendResult = null != sHud && sHud.getSendResult();
        //===================================================================================

        //===================================================================================
        // time
        //===================================================================================
        boolean timeSendResult = false;

        if (null != mRemainingMinutes) {
            if (mShowETA) {
                //show ETA
                if (mArrivalHours != -1 && mArrivalMinutes != -1) {
                    boolean sameAsLast = mArrivalHours == mLastArrivalHours && mArrivalMinutes == mLastArrivalMinutes;

                    if (!sameAsLast) {
                        if (null != sHud) {
                            sHud.setRemainTime(mArrivalHours, mArrivalMinutes, mBusyTraffic);
                        }
                        timeSendResult = (null != sHud) && sHud.getSendResult();
                        mLastArrivalMinutes = mArrivalMinutes;
                        mLastArrivalHours = mArrivalHours;
                    }
                }
            } else {
                //show remain time
                int hh = null != mRemainingHours ? Integer.parseInt(mRemainingHours) : 0;
                int mm = Integer.parseInt(mRemainingMinutes);

                if (null != sHud) {
                    sHud.setRemainTime(hh, mm, mBusyTraffic);
                }
                timeSendResult = (null != sHud) && sHud.getSendResult();
            }
        }
        //===================================================================================

        //===================================================================================
        // arrow
        // if same as last arrow, should be process, because GARMIN Hud will erase the arrow without data receive during sometime..
        //===================================================================================
        if (mArrowTypeV2) {
            updateArrow(mFoundArrowV2);
        } else {
            updateArrow(mFoundArrow);
        }
        final boolean arrowSendResult = (null != sHud) && sHud.getSendResult();
        //===================================================================================

        String sendResultInfo = "SendResult dist: " + (distanceSendResult ? '1' : '0')
                + " remaining dist: " + (remainingDistanceSendResult ? '1' : '0')
                + " time: " + (timeSendResult ? '1' : '0')
                + " arrow: " + (arrowSendResult ? '1' : '0');
        logi(sendResultInfo);
    }

    private void parseTimeAndDistanceToDest(String timeDistanceStirng) {
        String[] timeDistanceSplit = timeDistanceStirng.split("·");
        String arrivalTime;
        mRemainingHours = mRemainingMinutes = mRemainingDistance = mRemainingDistanceUnits = null;

        if (3 == timeDistanceSplit.length) {
            String timeToDest = timeDistanceSplit[0].trim();
            String distanceToDest = timeDistanceSplit[1].trim();
            String timeToArrived = timeDistanceSplit[2].trim();

            //======================================================================================
            // remain time
            //======================================================================================
            String[] timeSplit = timeToDest.split(" ");
            if (4 == timeSplit.length) {
                mRemainingHours = timeSplit[0].trim();
                mRemainingMinutes = timeSplit[2].trim();
                // Remove spaces, .trim() seems not working
                mRemainingHours = mRemainingHours.replaceAll("\u00A0", "");
                mRemainingMinutes = mRemainingMinutes.replaceAll("\u00A0", "");
            } else if (2 == timeSplit.length) {
                final int hour_index = timeToDest.indexOf(getString(R.string.hour));
                final int minute_index = timeToDest.indexOf(getString(R.string.minute));
                if (-1 != hour_index && -1 != minute_index) {
                    mRemainingHours = timeToDest.substring(0, hour_index).trim();
                    mRemainingMinutes = timeToDest.substring(hour_index + getString(R.string.hour).length(), minute_index).trim();
                    // Remove spaces, .trim() seems not working
                    mRemainingHours = mRemainingHours.replaceAll("\u00A0", "");
                    mRemainingMinutes = mRemainingMinutes.replaceAll("\u00A0", "");
                } else if (-1 != hour_index && -1 == minute_index) {
                    mRemainingHours = timeSplit[0].trim();
                    mRemainingMinutes = "0";
                } else {
                    mRemainingMinutes = timeSplit[0].trim();
                    mRemainingMinutes = mRemainingMinutes.replaceAll("\u00A0", "");
                }
            } else if (1 == timeSplit.length) {
                timeSplit = splitDigitAndNonDigit(timeToDest);
                mRemainingMinutes = 2 == timeSplit.length ? timeSplit[0] : null;
            }
            //======================================================================================

            //======================================================================================
            // remaining distance
            //======================================================================================
            String[] distSplit = distanceToDest.split("\\s");
            if (2 != distSplit.length) {
                distSplit = splitDigitAndNonDigit(distanceToDest);
            }
            if (2 == distSplit.length) {
                // Remove spaces, .trim() doesn't work
                mRemainingDistance = distSplit[0].replaceAll("\u00A0", "");
                mRemainingDistance = mRemainingDistance.replace(",", ".");
                // Remove spaces
                mRemainingDistanceUnits = distSplit[1].replaceAll("\u00A0", "");
                mRemainingDistanceUnits = translate(mRemainingDistanceUnits);
            }
            //======================================================================================

            //======================================================================================
            //ETA
            //======================================================================================
            final String ETA = getString(R.string.ETA);
            final int indexOfETA = timeToArrived.indexOf(ETA);
            String[] arrivedSplit;
            final boolean etaAtFirst = 0 == indexOfETA;
            // Separate EAT-String from leftValue
            arrivedSplit = timeToArrived.split(ETA);
            if (etaAtFirst) {
                // ETA-String first, then leftValue (chinese)
                arrivalTime = 2 == arrivedSplit.length ? arrivedSplit[1] : null;
            } else {
                // ETA-leftValue first, then string (english)
                arrivalTime = arrivedSplit[0];
            }
            arrivalTime = null != arrivalTime ? arrivalTime.trim() : arrivalTime;

            final int amIndex = arrivalTime.indexOf(getString(R.string.am));
            final int pmIndex = arrivalTime.indexOf(getString(R.string.pm));
            final boolean ampmAtFirst = 0 == amIndex || 0 == pmIndex;

            if (-1 != amIndex || -1 != pmIndex) { // 12-hour-format
                final int index = Math.max(amIndex, pmIndex);  // index of "am" or "pm"
                arrivalTime = ampmAtFirst ? arrivalTime.substring(index + 2) : arrivalTime.substring(0, index);
                arrivalTime = arrivalTime.trim();

                String[] split = arrivalTime.split(":");
                int hh = Integer.parseInt(split[0]);
                if (-1 != pmIndex && 12 != hh) {
                    hh += 12;
                }
                mArrivalHours = hh;
                mArrivalMinutes = Integer.parseInt(split[1]);
            } else { // 24-hour-format
                arrivalTime = arrivalTime.trim();

                String[] split = arrivalTime.split(":");
                if (2 == split.length) {
                    try {
                        mArrivalHours = Integer.parseInt(split[0]);
                        mArrivalMinutes = Integer.parseInt(split[1]);
                    } catch (NumberFormatException ex) {
                        mArrivalHours = mArrivalMinutes = 0;
                    }
                }
            }
            //======================================================================================
        }
    }

    private static String[] splitDigitAndAlphabetic(String str) {
        String[] result = new String[2];
        for (int x = 0; x < str.length(); x++) {
            char c = str.charAt(x);
            if (Character.isAlphabetic(c)) {
                result[0] = str.substring(0, x).replaceAll("\u00A0", ""); // Remove spaces, .trim() doesn't work
                result[1] = str.substring(x).replaceAll("\u00A0", ""); // Remove spaces
                break;
            }
        }
        return result;
    }

    private static String[] splitDigitAndNonDigit(String str) {
        String[] result = new String[2];
        for (int x = 0; x < str.length(); x++) {
            char c = str.charAt(x);
            if (!Character.isDigit(c) && '.' != c) {
                result[0] = str.substring(0, x);
                result[1] = str.substring(x);
                break;
            }
        }
        return result;
    }

    private static boolean containsOnlyWhitespaces(String str) {
        for (int x = 0; x < str.length(); x++) {
            if (!Character.isWhitespace(str.charAt(x))) {
                return false;
            }
        }

        return true;
    }

    private void parseDistanceToTurn(String distanceString) {
        final int indexOfChineseAfter = distanceString.indexOf(getString(R.string.chinese_after));
        if (-1 != indexOfChineseAfter) {
            distanceString = distanceString.substring(0, indexOfChineseAfter);
        }
        String[] splitArray = distanceString.split(" ");
        if (2 != splitArray.length) {
            splitArray = splitDigitAndAlphabetic(distanceString);
        }
        String num = null;
        String unit = null;
        if (splitArray.length == 2) {
            num = splitArray[0].replace(",", ".");
            unit = splitArray[1];
        }

        if (null != unit) {
            unit = translate(unit);
        }
        mDistanceNum = num;
        mDistanceUnit = unit;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        mMonitorHandler.post(() -> {
            updateCurrentNotifications();
            logi("onNotificationPosted...");
            logi("have " + sCurrentNotificationsCounts + " active notifications");
            sPostedNotification = sbn;
            processNotification(sbn);
        });
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        mMonitorHandler.post(() -> {
            logi("removed...");
            logi("have " + sCurrentNotificationsCounts + " active notifications");
            sRemovedNotification = sbn;

            String packageName = sbn.getPackageName();
            if (packageName.equals(GOOGLE_MAPS_PACKAGE_NAME)) {
                mPostman.addBooleanExtra(getString(R.string.gmaps_notify_catched), false);
                mPostman.addBooleanExtra(getString(R.string.is_in_navigation), mIsNavigating);
                mPostman.sendIntent2MainActivity();

                int hh = null != mRemainingHours ? Integer.parseInt(mRemainingHours) : 0;
                int mm = null != mRemainingMinutes ? Integer.parseInt(mRemainingMinutes) : -1;

                // Check if arrival is possible (don't know if mm==0 work always)
                if (hh == 0 && mm <= 5 && mm != -1) {
                    // Arrived: Delete Distance to turn
                    final boolean notArrivals = mArrowTypeV2 ? (mLastFoundArrowV2 != ArrowV2.ArrivalsLeft) && (mLastFoundArrowV2 != ArrowV2.ArrivalsRight) :
                            (mLastFoundArrow != Arrow.Arrivals) && (mLastFoundArrow != Arrow.ArrivalsLeft) && (mLastFoundArrow != Arrow.ArrivalsRight);

                    if (notArrivals) {
                        if (sHud != null) {
                            sHud.setDirection(eOutAngle.Straight, eOutType.RightFlag, eOutAngle.AsDirection);
                            sHud.clearDistance();
                        }
                    } else {
                        if (sHud != null) {
                            sHud.clearDistance();
                        }
                    }
                }
            }
        });
    }

    private void updateCurrentNotifications() {
        try {
            StatusBarNotification[] activeNos = getActiveNotifications();
            if (null == activeNos) {
                return;
            }
            if (sCurrentNotifications.size() == 0) {
                sCurrentNotifications.add(null);
            }
            sCurrentNotifications.set(0, activeNos);
            sCurrentNotificationsCounts = activeNos.length;
        } catch (Exception e) {
            Log.e(TAG, "Should not be here!!");
            e.printStackTrace();
        }
    }
}