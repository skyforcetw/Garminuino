package sky4s.garminhud.app;

import android.Manifest;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import sky4s.garminhud.Arrow;
import sky4s.garminhud.ArrowImage;
import sky4s.garminhud.eOutAngle;
import sky4s.garminhud.eOutType;
import sky4s.garminhud.eUnits;
import sky4s.garminhud.hud.HUDInterface;


public class NotificationMonitor extends NotificationListenerService {
    private final static boolean STORE_IMG = true;
    private final static String IMAGE_DIR = "/storage/emulated/0/Pictures/";

    public final static String GOOGLE_MAPS_PACKAGE_NAME = "com.google.android.apps.maps";
    public final static String GOOGLE_MAPS_GO_PACKAGE_NAME = "com.google.android.apps.navlite";
    public final static String GOOGLE_MAPS_NOTIFICATION_GROUP_NAVIGATION = "navigation_status_notification_group";

    public final static String OSMAND_PACKAGE_NAME = "net.osmand";
    public final static String OSMAND_NOTIFICATION_GROUP_NAVIGATION = "NAVIGATION";
    public final static String SYGIC_PACKAGE_NAME = "com.sygic.aura";

    private static final String TAG = NotificationMonitor.class.getSimpleName();
    private static final int EVENT_UPDATE_CURRENT_NOS = 0;

    public static List<StatusBarNotification[]> mCurrentNotifications = new ArrayList<StatusBarNotification[]>();
    public static int mCurrentNotificationsCounts = 0;
    public static StatusBarNotification mPostedNotification;
    public static StatusBarNotification mRemovedNotification;

    static HUDInterface hud = null;

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

    private long lastNotifyTimeMillis = 0;
    private long notifyPeriodTime = 0;

    private Arrow foundArrow = Arrow.None;
    private Arrow lastFoundArrow = Arrow.None;

    private String distanceNum = null;
    private static String distanceUnit = null;
    private String remainHour = null;
    private String remainMinute = null;
    private String remainDistance = null;
    private String remainDistanceUnit = null;
    private String arrivalTime = null;
    private int arrivalHour = -1;
    private int arrivalMinute = -1;
    private int lastArrivalHour = -1;
    private int lastArrivalMinute = -1;

    private static Bitmap removeAlpha(Bitmap originalBitmap) {
        // lets create a new empty bitmap
        Bitmap newBitmap = Bitmap.createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        // create a canvas where we can draw on
        Canvas canvas = new Canvas(newBitmap);
        // create a paint instance with alpha
        Paint alphaPaint = new Paint();
        alphaPaint.setAlpha(255);
        // now lets draw using alphaPaint instance
        canvas.drawBitmap(originalBitmap, 0, 0, alphaPaint);
        return newBitmap;
    }

    private static void logi(String msg) {
        Log.i(TAG, msg);
    }

    public static StatusBarNotification[] getCurrentNotifications() {
        if (mCurrentNotifications.size() == 0) {
            logi("mCurrentNotifications size is ZERO!!");
            return null;
        }
        return mCurrentNotifications.get(0);
    }

    private MsgReceiver msgReceiver;
    private Location location;

    private class MsgReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Parcelable parcelablelocation = intent.getParcelableExtra(getString(R.string.location));
            if (null != parcelablelocation && parcelablelocation instanceof Location) {
                location = (Location) parcelablelocation;
            }
            showETA = intent.getBooleanExtra(getString(R.string.option_show_eta), showETA);
            lastArrivalMinute = -1; // Force to switch to ETA after several toggles
            busyTraffic = intent.getBooleanExtra(getString(R.string.busy_traffic), busyTraffic);
        }
    }

    private boolean showETA = false;
    private static NotificationMonitor staticInstance;

    public static NotificationMonitor getStaticInstance() {
        return staticInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        logi("onCreate...");
        mMonitorHandler.sendMessage(mMonitorHandler.obtainMessage(EVENT_UPDATE_CURRENT_NOS));

        //========================================================================================
        // messageer
        //========================================================================================
        msgReceiver = new MsgReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(getString(R.string.broadcast_receiver_notification_monitor));

        registerReceiver(msgReceiver, intentFilter);

        //========================================================================================
        staticInstance = this;
        postman = new MainActivityPostman(this, getString(R.string.broadcast_sender_notification_monitor));
    }

    private MainActivityPostman postman;


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        logi("onBind...");
        return super.onBind(intent);
    }


    private void processNotification(StatusBarNotification sbn) {
        postman.addBooleanExtra(getString(R.string.notify_catched), true);
        postman.sendIntent2MainActivity();

        Notification notification = sbn.getNotification();
        if (null != notification) {
            String packageName = sbn.getPackageName();
            switch (packageName) {
                case GOOGLE_MAPS_PACKAGE_NAME:
                    parseGmapsNotification(notification);
                    break;
//                case GOOGLE_MAPS_GO_PACKAGE_NAME:
//                    parseGmapsGoNotificationByReflection(notification);
//                    break;
//                case OSMAND_PACKAGE_NAME:
//                    parseOsmandNotification(notification);
//                    break;
//                case SYGIC_PACKAGE_NAME:
//                    parseSygicNotification(notification);
//                    break;
                default:

            }
        } else {
            postman.addBooleanExtra(getString(R.string.notify_catched), true);
            postman.addBooleanExtra(getString(R.string.is_in_navigation), is_in_navigation);
            postman.sendIntent2MainActivity();
        }
    }

    private void parseSygicNotification(Notification notification) {
        long currentTime = System.currentTimeMillis();
        notifyPeriodTime = currentTime - lastNotifyTimeMillis;
        lastNotifyTimeMillis = currentTime;

        boolean parseResult = parseSygicNotificationByExtras(notification);

    }

    private boolean parseSygicNotificationByExtras(Notification notification) {
        if (null == notification) {
            return false;
        }
        Bundle extras = notification.extras;
//        String group_name = notification.getGroup();

        if ((null != extras)) {
//            extras.get(Notification.)
            Object big = extras.get(Notification.EXTRA_BIG_TEXT);
//            Object msg = extras.get(Notification.EXTRA_MESSAGES);

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
                Bitmap bitmapImage = drawableToBitmap(drawableIco);

                if (null != bitmapImage) {
                    if (STORE_IMG) {
                        storeBitmap(bitmapImage, IMAGE_DIR + "arrow0_sygic.png");
                    }
                    bitmapImage = removeAlpha(bitmapImage);
                    if (STORE_IMG) {
                        storeBitmap(bitmapImage, IMAGE_DIR + "arrow_sygic.png");
                    }
                }
            }
            // Check if subText is empty (" ·  · ") --> don't parse subText
            // Occurs for example on NagivationChanged
            boolean subTextEmpty = true;
            return true;
        } else {
            return false;
        }
    }

    private void parseOsmandNotification(Notification notification) {
        long currentTime = System.currentTimeMillis();
        notifyPeriodTime = currentTime - lastNotifyTimeMillis;
        lastNotifyTimeMillis = currentTime;

        boolean parseResult = parseOsmandNotificationByExtras(notification);

    }


    private void parseGmapsNotification(Notification notification) {
        long currentTime = System.currentTimeMillis();
        notifyPeriodTime = currentTime - lastNotifyTimeMillis;
        lastNotifyTimeMillis = currentTime;

        boolean parseResult = parseGmapsNotificationByExtras(notification);
        if (!parseResult) { //gmap on android 6.0 need parsing by reflection
            parseResult = parseGmapsNotificationByReflection(notification);
        }

        if (!parseResult) {
            postman.addBooleanExtra(getString(R.string.notify_parse_failed), true);
            postman.addBooleanExtra(getString(R.string.gmaps_notify_catched), true);
            postman.addBooleanExtra(getString(R.string.is_in_navigation), is_in_navigation);
            postman.sendIntent2MainActivity();
        } else {
            postman.addBooleanExtra(getString(R.string.notify_parse_failed), false);
            postman.addBooleanExtra(getString(R.string.gmaps_notify_catched), true);
            postman.addBooleanExtra(getString(R.string.is_in_navigation), is_in_navigation);
            postman.sendIntent2MainActivity();
        }
    }

    private static boolean checkmActions(RemoteViews views) {
        try {
            Class viewsClass = views.getClass();
            Field fieldActions = viewsClass.getDeclaredField("mActions");
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
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static RemoteViews getRemoteViews(Notification notification) {
        // We have to extract the information from the view
        RemoteViews views = notification.bigContentView;
        if (!checkmActions(views)) { //check mActions is exist, we use it to parse notification
            views = null;
        }
        if (views == null) views = notification.contentView;
        if (!checkmActions(views)) {//check mActions again
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

            Class viewsClass = views.getClass();
            Field fieldActions = viewsClass.getDeclaredField("mActions");
            fieldActions.setAccessible(true);

            @SuppressWarnings("unchecked")
            ArrayList<Parcelable> actions = (ArrayList<Parcelable>) fieldActions.get(views);

            int indexOfActions = 0;
            int updateCount = 0;
            boolean inNavigation = false;

//            Map<Integer, String> text = new HashMap<Integer, String>();
            // Find the setText() and setTime() reflection actions
            for (Parcelable action : actions) {

                Object methodName = getObjectProperty(action, "methodName");
                Object type = getObjectProperty(action, "type");
                Object value = getObjectProperty(action, "value");


                if (methodName == null) continue;
                    // Save strings
                else if (methodName.equals("setText")) {
//                    Integer integerType = type instanceof Integer ? (Integer) type : 0;
                    if (value instanceof String) {
                        String str = (String) value;
                        switch (indexOfActions) {
                            case 4: //distance
                                break;
                            case 6://road
                                break;
                            case 9://time
                                break;
                        }
                    }
                    int a = 1;
                } else if (methodName.equals("setImageBitmap")) {
                    Object bitmapId = getObjectProperty(action, "bitmapId");
                    Object bitmapCache = getObjectProperty(views, "mBitmapCache");
                    Object bitmapsObject = getObjectProperty(bitmapCache, "mBitmaps");

                    if (bitmapId instanceof Integer && null != bitmapsObject) {
                        Integer integerBitmapId = (Integer) bitmapId;
                        ArrayList<Bitmap> bitmapList = (ArrayList<Bitmap>) bitmapsObject;

                        Bitmap bitmapImage = bitmapList.get(integerBitmapId);
                        if (STORE_IMG) {
                            storeBitmap(bitmapImage, IMAGE_DIR + "arrow0.png");
                        }
                        bitmapImage = removeAlpha(bitmapImage);
                        if (STORE_IMG) {
                            storeBitmap(bitmapImage, IMAGE_DIR + "arrow.png");
                        }
                        ArrowImage arrowImage = new ArrowImage(bitmapImage);
                        // Log.i(TAG, "Arrow-Value: "+arrowImage.getArrowValue());
                        foundArrow = getArrow(arrowImage);
                        updateCount++;
                    }

                }
                indexOfActions++;
            }

            logParseMessage();
            //can update to garmin hud
            if (0 != updateCount && inNavigation) {
                updateGaminHudInformation();
            }
            return true;
        }
        // It's not usually good style to do this, but then again, neither is the use of reflection...
        catch (Exception e) {
            Log.e(TAG, e.toString());
            return false;
        }

    }

    /**
     * @param notification
     * @return
     */
    private boolean parseGmapsNotificationByReflection(Notification notification) {
        RemoteViews views = getRemoteViews(notification);
        if (views == null) return false;

        // Use reflection to examine the m_actions member of the given RemoteViews object.
        // It's not pretty, but it works.
        try {

            Class viewsClass = views.getClass();
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
                        case 3://distance to turn
                            parseDistanceToTurn(t);
                            break;
//                        case 5://road
//                            break;
                        case 8://time, distance, arrived time
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
                            storeBitmap(bitmapImage, IMAGE_DIR + "arrow0.png");
                        }
                        bitmapImage = removeAlpha(bitmapImage);
                        if (STORE_IMG) {
                            storeBitmap(bitmapImage, IMAGE_DIR + "arrow.png");
                        }
                        ArrowImage arrowImage = new ArrowImage(bitmapImage);
                        // Log.i(TAG, "Arrow-Value: "+arrowImage.getArrowValue());
                        foundArrow = getArrow(arrowImage);
                        updateCount++;
                    }

                }

                parcel.recycle();
                indexOfActions++;
            }

            logParseMessage();
            //can update to garmin hud
            if (0 != updateCount && inNavigation) {
                updateGaminHudInformation();
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
        String notifyMessage = foundArrow.toString() + " " + distanceNum + distanceUnit +
                " " + (null == remainHour ? 00 : remainHour) + ":" + remainMinute + " " + remainDistance + remainDistanceUnit + " " + arrivalHour + ":" + arrivalMinute
                + " (period: " + notifyPeriodTime + ")";
        logi(notifyMessage);

        boolean output_parse_message_to_ui = true;
        if (output_parse_message_to_ui) {
            postman.addStringExtra(getString(R.string.notify_msg), notifyMessage);
            if (Arrow.Arrivals == foundArrow || Arrow.ArrivalsLeft == foundArrow || Arrow.ArrivalsRight == foundArrow) {
                postman.addBooleanExtra(getString(R.string.arrivals_msg), true);
            }
            postman.addBooleanExtra(getString(R.string.is_in_navigation), is_in_navigation);
            postman.sendIntent2MainActivity();
        }
    }

    private boolean is_in_navigation = false;

    private boolean parseOsmandNotificationByExtras(Notification notification) {
        if (null == notification) {
            return false;
        }
        Bundle extras = notification.extras;
        String group_name = notification.getGroup();

        if ((null != extras) && (null != group_name) && group_name.equals(OSMAND_NOTIFICATION_GROUP_NAVIGATION)) {
            Object reaminTime = extras.get(Notification.EXTRA_BIG_TEXT);
            Object distance = extras.get(Notification.EXTRA_TITLE);

            Icon largeIcon = notification.getLargeIcon();
            Icon smallIcon = notification.getSmallIcon();

            Object msg = extras.get(Notification.EXTRA_MESSAGES);
            Object textObj = extras.get(Notification.EXTRA_TEXT);
            Object subTextObj = extras.get(Notification.EXTRA_SUB_TEXT);


            if (null != largeIcon) {
                Drawable drawableIco = largeIcon.loadDrawable(this);
                Bitmap bitmapImage = drawableToBitmap(drawableIco);

                if (null != bitmapImage) {
                    if (STORE_IMG) {
                        storeBitmap(bitmapImage, IMAGE_DIR + "arrow0_osm.png");
                    }
                    bitmapImage = removeAlpha(bitmapImage);
                    if (STORE_IMG) {
                        storeBitmap(bitmapImage, IMAGE_DIR + "arrow_osm.png");
                    }
                }
            }

//            String title = null != titleObj ? titleObj.toString() : null;
//            String text = null != textObj ? textObj.toString() : null;
//            String subText = null != subTextObj ? subTextObj.toString() : null;
//            subText = null == subText ? text : subText;

            // Check if subText is empty (" ·  · ") --> don't parse subText
            // Occurs for example on NagivationChanged
            boolean subTextEmpty = true;
//            if (null != subText) {
//                String[] split = subText.split("·");
//                for (int i = 0; i < split.length; i++) {
//                    String trimString = split[i].trim();
//                    boolean string_empty = containsOnlyWhitespaces(trimString);
//                    if (string_empty == false) {
//                        subTextEmpty = false;
//                        break;
//                    }
//                }
//            }
//
//            final boolean somethingCanParse = null != subText && !subTextEmpty;
//            if (somethingCanParse) {
//                parseTimeAndDistanceToDest(subText);
//
//                String[] title_str = title.split("–");
//                title_str = 1 == title_str.length ? title.split("-") : title_str;
//                String distance = title_str[0].trim();
//                if (Character.isDigit(distance.charAt(0)))
//                    parseDistanceToTurn(distance);
//                else
//                    distanceNum = "-1";
//
//                Icon largeIcon = notification.getLargeIcon();
//                Icon smallIcon = notification.getSmallIcon();
//                if (null != largeIcon) {
//                    Drawable drawableIco = largeIcon.loadDrawable(this);
//                    Bitmap bitmapImage = drawableToBitmap(drawableIco);
//
//                    if (null != bitmapImage) {
//                        if (STORE_IMG) {
//                            storeBitmap(bitmapImage, IMAGE_DIR + "arrow0.png");
//                        }
//
//                        ArrowImage arrowImage = new ArrowImage(bitmapImage);
//
//                        if (STORE_IMG) {
//                            storeBitmap(arrowImage.binaryImage, IMAGE_DIR + "binary.png");
//                        }
//
//                        foundArrow = getArrow(arrowImage);
//                        if (lastFoundArrow != foundArrow && USE_DB && null != dbHelper) {
//                            dbHelper.insert(null, bitmapImage, arrowImage, foundArrow);
//                        }
//                        lastFoundArrow = foundArrow;
//
//                    }
//                }
//                logParseMessage();
//                updateGaminHudInformation();
//                is_in_navigation = true;
//            } else {
//                is_in_navigation = false;
//            }
            return true;
        } else {
            return false;
        }
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
            // Occurs for example on NagivationChanged
            boolean subTextEmpty = true;
            if (null != subText) {
                String[] split = subText.split("·");
                for (int i = 0; i < split.length; i++) {
                    String trimString = split[i].trim();
                    boolean string_empty = containsOnlyWhitespaces(trimString);
                    if (string_empty == false) {
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
                    distanceNum = "-1";
                }

                Icon largeIcon = notification.getLargeIcon();
                if (null != largeIcon) {
                    Drawable drawableIco = largeIcon.loadDrawable(this);
                    Bitmap bitmapImage = drawableToBitmap(drawableIco);

                    if (null != bitmapImage) {
                        if (STORE_IMG) {
                            storeBitmap(bitmapImage, IMAGE_DIR + "arrow0.png");
                        }

                        ArrowImage arrowImage = new ArrowImage(bitmapImage);

                        if (STORE_IMG) {
                            storeBitmap(arrowImage.binaryImage, IMAGE_DIR + "binary.png");
                        }

                        foundArrow = getArrow(arrowImage);
                        lastFoundArrow = foundArrow;

                    }
                }
                logParseMessage();
                updateGaminHudInformation();
                is_in_navigation = true;
                return true;
            } else {
                is_in_navigation = false;
                return false;
            }

        } else {
            return false;
        }
    }

    private static Bitmap drawableToBitmap(Drawable drawable) {
        if (null == drawable) {
            return null;
        }
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private void storeBitmap(Bitmap bmp, String filename) {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            return;
        }

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(filename);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static eUnits get_eUnits(String unit) {
        if (null == unit) {
            return eUnits.None;
        } else {
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
    }

    // Translates the units (distance and time) from local language and charset in common values
    private String translate(String local_language_string) {
        if (local_language_string.equalsIgnoreCase(getString(R.string.km)))
            return "km";
        else if (local_language_string.equalsIgnoreCase(getString(R.string.meter)))
            return "m";
        else if (local_language_string.equalsIgnoreCase(getString(R.string.feet)))
            return "ft";
        else if (local_language_string.equalsIgnoreCase(getString(R.string.miles)))
            return "mi";
        else if (local_language_string.equalsIgnoreCase(getString(R.string.minute)))
            return "m";
        else if (local_language_string.equalsIgnoreCase(getString(R.string.minute2)))
            return "m";
        else if (local_language_string.equalsIgnoreCase(getString(R.string.hour)))
            return "h";
        else if (local_language_string.equalsIgnoreCase(getString(R.string.hour2)))
            return "h";
        else
            return null;
    }

    // Returns the current Unit (Kilometres or Miles) based on distanceToTurn
    public static eUnits getCurrentUnit() {
        if ((get_eUnits(distanceUnit) == eUnits.Kilometres) || (get_eUnits(distanceUnit) == eUnits.Metres))
            return eUnits.Kilometres;
        else if ((get_eUnits(distanceUnit) == eUnits.Miles) || (get_eUnits(distanceUnit) == eUnits.Foot))
            return eUnits.Miles;
        else
            return eUnits.None;
    }

    private static Arrow getArrow(ArrowImage image) {
        long minSad = Integer.MAX_VALUE;
        Arrow minSadArrow = Arrow.None;

        int totalArrowCount = Arrow.values().length;
        long sadArray[] = new long[totalArrowCount];
        int index = 0;

        for (Arrow a : Arrow.values()) {
            long sad = image.getSAD(a.valueLeft);
            sadArray[index++] = sad;
            if (sad < minSad) {
                minSad = sad;
                minSadArrow = a;
            }
            if (0 == sad) {
                String integerString = Long.toString(a.valueLeft);
                Log.d(TAG, "Recognize " + a.name() + " " + integerString);
                return a;

            }
        }
        Log.d(TAG, "No Recognize, minSad: " + minSad + " arrow:" + minSadArrow);
        return minSadArrow;
    }

    private Arrow preArrow = Arrow.None;

    void updateArrow(Arrow arrow) {
        if (null == hud) {
            return;
        }
        if (preArrow == arrow) {

        } else {
            preArrow = arrow;
        }
        switch (arrow) {
            case Arrivals:
                hud.SetDirection(eOutAngle.Straight, eOutType.RightFlag, eOutAngle.AsDirection);
                break;
            case ArrivalsLeft:
                hud.SetDirection(eOutAngle.Left, eOutType.RightFlag, eOutAngle.AsDirection);
                break;
            case ArrivalsRight:
                hud.SetDirection(eOutAngle.Right, eOutType.RightFlag, eOutAngle.AsDirection);
                break;

            case EasyLeft:
            case KeepLeft:
                hud.SetDirection(eOutAngle.EasyLeft);
                break;

            case EasyRight:
            case KeepRight:
                hud.SetDirection(eOutAngle.EasyRight);
                break;
            case GoTo:
                hud.SetDirection(eOutAngle.Straight);
                break;

            case LeaveRoundabout://1 checked
                hud.SetDirection(eOutAngle.Left, eOutType.LeftRoundabout, eOutAngle.Left);
                break;

            case LeaveRoundaboutAsUTurn:
                hud.SetDirection(eOutAngle.Down, eOutType.LeftRoundabout, eOutAngle.Down);
                break;


            case LeaveRoundaboutAsUTurnCC:
                hud.SetDirection(eOutAngle.Down, eOutType.RightRoundabout, eOutAngle.Down);
                break;


            case LeaveRoundaboutEasyLeft://4 checked
                hud.SetDirection(eOutAngle.EasyLeft, eOutType.LeftRoundabout, eOutAngle.EasyLeft);
                break;

            case LeaveRoundaboutEasyLeftCC://5 checked
                hud.SetDirection(eOutAngle.EasyLeft, eOutType.RightRoundabout, eOutAngle.EasyLeft);
                break;

            case LeaveRoundaboutEasyRight://6 checked
                hud.SetDirection(eOutAngle.EasyRight, eOutType.LeftRoundabout, eOutAngle.EasyRight);
                break;
            case LeaveRoundaboutEasyRightCC://7 checked
                hud.SetDirection(eOutAngle.EasyRight, eOutType.RightRoundabout, eOutAngle.EasyRight);
                break;

            case LeaveRoundaboutCC://8 checked
                hud.SetDirection(eOutAngle.Right, eOutType.RightRoundabout, eOutAngle.Right);
                break;

            case LeaveRoundaboutLeft://9 checked
                hud.SetDirection(eOutAngle.Left, eOutType.LeftRoundabout, eOutAngle.Left);
                break;
            case LeaveRoundaboutLeftCC://10 checked
                hud.SetDirection(eOutAngle.Left, eOutType.RightRoundabout, eOutAngle.Left);
                break;
            case LeaveRoundaboutRight://11 checked
                hud.SetDirection(eOutAngle.Right, eOutType.LeftRoundabout, eOutAngle.Right);
                break;
            case LeaveRoundaboutRightCC://12 checked
                hud.SetDirection(eOutAngle.Right, eOutType.RightRoundabout, eOutAngle.Right);
                break;

            case LeaveRoundaboutSharpLeft://13 checked
                hud.SetDirection(eOutAngle.SharpLeft, eOutType.LeftRoundabout, eOutAngle.SharpLeft);
                break;
            case LeaveRoundaboutSharpLeftCC://14 checked
                hud.SetDirection(eOutAngle.SharpLeft, eOutType.RightRoundabout, eOutAngle.SharpLeft);
                break;

            case LeaveRoundaboutSharpRight://15 checked
                hud.SetDirection(eOutAngle.SharpRight, eOutType.LeftRoundabout, eOutAngle.SharpRight);
                break;
            case LeaveRoundaboutSharpRightCC://16
                hud.SetDirection(eOutAngle.SharpRight, eOutType.RightRoundabout, eOutAngle.SharpRight);
                break;

            case LeaveRoundaboutStraight://
                hud.SetDirection(eOutAngle.Straight, eOutType.LeftRoundabout, eOutAngle.Straight);
                break;

            case LeaveRoundaboutStraightCC://
                hud.SetDirection(eOutAngle.Straight, eOutType.RightRoundabout, eOutAngle.Straight);
                break;

            case Left:
                hud.SetDirection(eOutAngle.Left);
                break;

            case LeftDown:
                hud.SetDirection(eOutAngle.LeftDown);
                break;
            case LeftToLeave:
                hud.SetDirection(eOutAngle.EasyLeft, eOutType.LongerLane, eOutAngle.AsDirection);
                break;

            case Right:
                hud.SetDirection(eOutAngle.Right);
                break;
            case RightDown:
                hud.SetDirection(eOutAngle.RightDown);
                break;
            case RightToLeave:
                hud.SetDirection(eOutAngle.EasyRight, eOutType.LongerLane, eOutAngle.AsDirection);
                break;
            case SharpLeft:
                hud.SetDirection(eOutAngle.SharpLeft);
                break;
            case SharpRight:
                hud.SetDirection(eOutAngle.SharpRight);
                break;
            case Straight:
                hud.SetDirection(eOutAngle.Straight);
                break;

            case Convergence:
            case None:
            default:
                hud.SetDirection(eOutAngle.AsDirection);
                break;
        }
    }

    private String lastRemainHour = null, lastRemainMinute = null;
    private boolean busyTraffic = false;

    private void updateGaminHudInformation() {
        Log.i(TAG, "hud: " + hud);
        //===================================================================================
        // distance
        //===================================================================================
        if (null != distanceNum && null != distanceUnit) {
            float float_distance = Float.parseFloat(distanceNum);
            eUnits units = get_eUnits(distanceUnit);

            int int_distance = (int) float_distance;
            boolean decimal = ((eUnits.Kilometres == units) || (eUnits.Miles == units)) && float_distance < 10;

            if (decimal) { //with floating point
                int_distance = (int) (float_distance * 10);
            }

            if (null != hud) {
                if (-1 != int_distance) {
                    hud.SetDistance(int_distance, units, decimal, false);
                } else {
                    hud.ClearDistance();
                }
            }

        } else {
            if (null != hud) {
                hud.ClearDistance();
            }
        }

        final boolean distanceSendResult = null != hud ? hud.getSendResult() : false;
        //===================================================================================

        //===================================================================================
        // time
        //===================================================================================
        boolean timeSendResult = false;

        if (null != remainMinute) {
            if (showETA) { //show ETA
                if (arrivalHour != -1 && arrivalMinute != -1) {
                    boolean sameAsLast = arrivalHour == lastArrivalHour && arrivalMinute == lastArrivalMinute;

                    if (!sameAsLast) {
                        if (null != hud) {
//                            hud.SetTime(arrivalHour, arrivalMinute, false);
                            hud.SetRemainTime(arrivalHour, arrivalMinute, busyTraffic);
                        }
                        timeSendResult = (null != hud) ? hud.getSendResult() : false;
                        lastArrivalMinute = arrivalMinute;
                        lastArrivalHour = arrivalHour;
                    }
                }
            } else { //show remain time
                int hh = null != remainHour ? Integer.parseInt(remainHour) : 0;
                int mm = Integer.parseInt(remainMinute);

                boolean sameAsLast = null == remainHour ?
                        remainMinute.equals(lastRemainMinute) : remainMinute.equals(lastRemainMinute) && remainHour.equals(lastRemainHour);

                //need to verify the necessary of check same as last.
                sameAsLast = false;
                if (!sameAsLast) {
                    if (null != hud) {
                        hud.SetRemainTime(hh, mm, busyTraffic);
                    }
                    timeSendResult = (null != hud) ? hud.getSendResult() : false;
                    lastRemainMinute = remainMinute;
                    lastRemainHour = remainHour;
                }
            }
        } else {

        }
        //===================================================================================

        //===================================================================================
        // arrow
        // if same as last arrow, should be process, because GARMIN Hud will erase the arrow without data receive during sometime..
        //===================================================================================
        updateArrow(foundArrow);
        final boolean arrowSendResult = (null != hud) ? hud.getSendResult() : false;
        //===================================================================================

        String sendResultInfo = "SendResult dist: " + (distanceSendResult ? '1' : '0')
                + " time: " + (timeSendResult ? '1' : '0')
                + " arrow: " + (arrowSendResult ? '1' : '0');
        logi(sendResultInfo);

    }

    private void parseTimeAndDistanceToDest(String timeDistanceStirng) {
        String[] timeDistanceSplit = timeDistanceStirng.split("·");
        remainHour = remainMinute = remainDistance = remainDistanceUnit = arrivalTime = null;

        if (3 == timeDistanceSplit.length) {
            String timeToDest = timeDistanceSplit[0].trim();
            String distanceToDest = timeDistanceSplit[1].trim();
            String timeToArrived = timeDistanceSplit[2].trim();

            //======================================================================================
            // remain time
            //======================================================================================
            String[] timeSplit = timeToDest.split(" ");
            if (4 == timeSplit.length) {
                remainHour = timeSplit[0].trim();
                remainMinute = timeSplit[2].trim();
                remainHour = remainHour.replaceAll("\u00A0", ""); // Remove spaces, .trim() seems not working
                remainMinute = remainMinute.replaceAll("\u00A0", "");
            } else if (2 == timeSplit.length) {
                final int hour_index = timeToDest.indexOf(getString(R.string.hour));
                final int minute_index = timeToDest.indexOf(getString(R.string.minute));
                if (-1 != hour_index && -1 != minute_index) {
                    remainHour = timeToDest.substring(0, hour_index).trim();
                    remainMinute = timeToDest.substring(hour_index + getString(R.string.hour).length(), minute_index).trim();
                    remainHour = remainHour.replaceAll("\u00A0", ""); // Remove spaces, .trim() seems not working
                    remainMinute = remainMinute.replaceAll("\u00A0", "");
                } else {
                    remainMinute = timeSplit[0].trim();
                    remainMinute = remainMinute.replaceAll("\u00A0", "");

                }

            } else if (1 == timeSplit.length) {
                timeSplit = splitDigitAndNonDigit(timeToDest);
                remainMinute = 2 == timeSplit.length ? timeSplit[0] : null;
            }
            //======================================================================================


            //======================================================================================
            // remain distance
            //======================================================================================
            String[] distSplit = distanceToDest.split("\\s");
            if (2 != distSplit.length) {
                distSplit = splitDigitAndNonDigit(distanceToDest);
            }
            if (2 == distSplit.length) {
                remainDistance = distSplit[0].replaceAll("\u00A0", ""); // Remove spaces, .trim() doesn't work
                remainDistance = remainDistance.replace(",", ".");
                remainDistanceUnit = distSplit[1].replaceAll("\u00A0", ""); // Remove spaces
                remainDistanceUnit = translate(remainDistanceUnit);
            }

            //======================================================================================
            //ETA
            //======================================================================================
            final String ETA = getString(R.string.ETA);
            final int indexOfETA = timeToArrived.indexOf(ETA);
            String[] arrivedSplit = null;
            final boolean etaAtFirst = 0 == indexOfETA;
            // Separate EAT-String from value1
            if (etaAtFirst) { // ETA-String first, then value1 (chinese)
                arrivedSplit = timeToArrived.split(ETA);
                arrivalTime = 2 == arrivedSplit.length ? arrivedSplit[1] : null;
            } else { // ETA-value1 first, then string (english)
                arrivedSplit = timeToArrived.split(ETA);
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
                    arrivalTime = hh + ":" + split[1];
                }
                arrivalHour = hh;
                arrivalMinute = Integer.parseInt(split[1]);
            } else { // 24-hour-format
                arrivalTime = arrivalTime.trim();

                String[] split = arrivalTime.split(":");
                if (2 == split.length) {
                    try {
                        arrivalHour = Integer.parseInt(split[0]);
                        arrivalMinute = Integer.parseInt(split[1]);
                    } catch (NumberFormatException ex) {
                        arrivalHour = arrivalMinute = 0;
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
        boolean string_empty = true;
        for (int x = 0; x < str.length(); x++) {
            if (!Character.isWhitespace(str.charAt(x))) {
                string_empty = false;
                return string_empty;
            }
        }

        return string_empty;
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
        distanceNum = num;
        distanceUnit = unit;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        updateCurrentNotifications();
        logi("onNotificationPosted...");
        logi("have " + mCurrentNotificationsCounts + " active notifications");
        mPostedNotification = sbn;
        processNotification(sbn);
    }


    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {

        logi("removed...");
        logi("have " + mCurrentNotificationsCounts + " active notifications");
        mRemovedNotification = sbn;

        String packageName = sbn.getPackageName();
        if (packageName.equals(GOOGLE_MAPS_PACKAGE_NAME)
//                || packageName.equals(GOOGLE_MAPS_GO_PACKAGE_NAME)
        ) {

            postman.addBooleanExtra(getString(R.string.gmaps_notify_catched), false);
            postman.addBooleanExtra(getString(R.string.is_in_navigation), is_in_navigation);
            postman.sendIntent2MainActivity();


            int hh = null != remainHour ? Integer.parseInt(remainHour) : 0;
            int mm = null != remainMinute ? Integer.parseInt(remainMinute) : -1;

            // Check if arrival is possible (don't know if mm==0 work always)
            if (hh == 0 && mm <= 5 && mm != -1) {
                // Arrived: Delete Distance to turn
                if ((lastFoundArrow != Arrow.Arrivals) && (lastFoundArrow != Arrow.ArrivalsLeft) && (lastFoundArrow != Arrow.ArrivalsRight)) {
                    if (hud != null) {
                        hud.SetDirection(eOutAngle.Straight, eOutType.RightFlag, eOutAngle.AsDirection);
                        hud.ClearDistance();
                    }
                } else {
                    if (hud != null)
                        hud.ClearDistance();
                }
            } else {

            }
        }
    }

    private void updateCurrentNotifications() {
        try {
            StatusBarNotification[] activeNos = getActiveNotifications();
            if (null == activeNos) {
                return;
            }
            if (mCurrentNotifications.size() == 0) {
                mCurrentNotifications.add(null);
            }
            mCurrentNotifications.set(0, activeNos);
            mCurrentNotificationsCounts = activeNos.length;
        } catch (Exception e) {
            Log.e(TAG, "Should not be here!!");
            e.printStackTrace();
        }
    }


}

interface NotificationParserIF {

}


class GmapsNotificaitonParser implements NotificationParserIF {

    public boolean isInNavigation() {
        return false;
    }

    public boolean parse(Notification notification) {
        return false;
    }
}