package sky4s.garminhud.app;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
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

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import sky4s.garminhud.Arrow;
import sky4s.garminhud.ArrowImage;
import sky4s.garminhud.GarminHUD;
import sky4s.garminhud.eOutAngle;
import sky4s.garminhud.eOutType;
import sky4s.garminhud.eUnits;


public class NotificationMonitor extends NotificationListenerService {
    private final static boolean STORE_IMG = false;
    private final static String IMAGE_DIR = "/storage/emulated/0/Pictures/";
    private final static boolean DONT_SEND_SAME = false;


    public static final String ACTION_NLS_CONTROL = "sky4s.garmin.hud.NLSCONTROL";
    public final static String GOOGLE_MAPS_PACKAGE_NAME = "com.google.android.apps.maps";
    public final static String GOOGLE_MAPS_NOTIFICATION_GROUP_NAVIGATION = "navigation_status_notification_group";
    private static final String TAG = NotificationMonitor.class.getSimpleName();
    private static final int EVENT_UPDATE_CURRENT_NOS = 0;


    public static List<StatusBarNotification[]> mCurrentNotifications = new ArrayList<StatusBarNotification[]>();
    public static int mCurrentNotificationsCounts = 0;
    public static StatusBarNotification mPostedNotification;
    public static StatusBarNotification mRemovedNotification;
    private CancelNotificationReceiver mReceiver = new CancelNotificationReceiver();

    //    public static BluetoothSPP bt = null;
    static GarminHUD garminHud = null;

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

    private class MsgReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            showETA = intent.getBooleanExtra(Integer.toString(R.id.switchShowETA), showETA);
            lastArrivalMinute = -1; // Force to switch to ETA after several toggles
        }
    }

    private boolean showETA = false;


    @Override
    public void onCreate() {
        super.onCreate();
        logi("onCreate...");
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_NLS_CONTROL);
        registerReceiver(mReceiver, filter);
        mMonitorHandler.sendMessage(mMonitorHandler.obtainMessage(EVENT_UPDATE_CURRENT_NOS));

        //========================================================================================
        // messageer
        //========================================================================================
        msgReceiver = new MsgReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(getString(R.string.broadcast_receiver_notification_monitor));
        registerReceiver(msgReceiver, intentFilter);

        //========================================================================================
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        logi("onBind...");
        return super.onBind(intent);
    }

    private void sendBooleanExtra2MainActivity(String string, boolean b) {
        Intent intent = new Intent(getString(R.string.broadcast_receiver_main_activity));
        intent.putExtra(string, b);
        sendBroadcast(intent);
    }

    private void sendStringExtra2MainActivity(String string, String message) {
        Intent intent = new Intent(getString(R.string.broadcast_receiver_main_activity));
        intent.putExtra(string, message);
        sendBroadcast(intent);
    }


    private void processGoogleMapsNotification(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        sendBooleanExtra2MainActivity(getString(R.string.notify_catched), true);

        if (packageName.equals(GOOGLE_MAPS_PACKAGE_NAME)) {
            Notification notification = sbn.getNotification();
            if (null == notification) {
                return;
            }
            processGoogleMapsNotification(notification);
        }
    }


    private void processGoogleMapsNotification(Notification notification) {
        long currentTime = System.currentTimeMillis();
        notifyPeriodTime = currentTime - lastNotifyTimeMillis;
        lastNotifyTimeMillis = currentTime;

        boolean parseResult = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            parseResult = parseNotificationByExtras(notification);
        } else {
            parseResult = parseNotificationByReflection(notification);
        }

        if (!parseResult) {
            sendBooleanExtra2MainActivity(getString(R.string.notify_parse_failed), true);
            sendBooleanExtra2MainActivity(getString(R.string.gmaps_notify_catched), false);
        } else {
            sendBooleanExtra2MainActivity(getString(R.string.gmaps_notify_catched), true);
        }
    }

    private boolean checkmActions(RemoteViews views) {
        try {
            Class viewsClass = views.getClass();
            Field fieldActions = viewsClass.getDeclaredField("mActions");
            return true;
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            return false;
        }
    }

    private boolean parseNotificationByReflection(Notification notification) {

        // We have to extract the information from the view
        RemoteViews views = notification.bigContentView;
        if (!checkmActions(views)) {
            views = null;
        }
        if (views == null) views = notification.contentView;
        if (!checkmActions(views)) {
            views = null;
        }
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
                        case 5://road
                            break;
                        case 8://time, distance, arrived time
                            parseTimeAndDistanceToDest(t);
                            updateCount++;
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
                " " + (null == remainHour ? 00 : remainHour) + ":" + remainMinute + " " + remainDistance + remainDistanceUnit + " " + arrivalTime
                + " (period: " + notifyPeriodTime + ")";
        logi(notifyMessage);
    }

    private boolean parseNotificationByExtras(Notification notification) {
        if (null == notification) {
            return false;
        }
        Bundle extras = notification.extras;
        String group_name = notification.getGroup();
        if ((null != extras) && (null != group_name) && group_name.equals(GOOGLE_MAPS_NOTIFICATION_GROUP_NAVIGATION)) {
            //not in navigation(chinese) of title: 參考 Google 地圖行駛
            Object titleObj = extras.get(Notification.EXTRA_TITLE);
            Object subTextObj = extras.get(Notification.EXTRA_SUB_TEXT);
            String title = null != titleObj ? titleObj.toString() : null;
            String subText = null != subTextObj ? subTextObj.toString() : null;

            // Check if subText is empty (" ·  · ") --> don't parse subText
            // Occurs for example on NagivationChanged
            boolean subTextEmpty = true;
            if (null != subText) {
                String[] split = subText.split("·");
                for (int i = 0; i < split.length; i++) {
                    split[i].trim();
                    boolean string_empty = containsOnlyWhitespaces(split[i]);
                    if (string_empty == false) {
                        subTextEmpty = false;
                        break;
                    }
                }
            }

            if (null != subText && !subTextEmpty) {
                parseTimeAndDistanceToDest(subText);

                String[] title_str = title.split("–");
                String distance = title_str[0].trim();
                if (Character.isDigit(distance.charAt(0)))
                    parseDistanceToTurn(distance);
                else
                    distanceNum = "-1";

                Icon largeIcon = notification.getLargeIcon();
                if (null != largeIcon) {
                    Drawable drawableIco = largeIcon.loadDrawable(this);
                    Bitmap bitmapImage = drawableToBitmap(drawableIco);

                    if (null != bitmapImage) {
                        if (STORE_IMG) {
                            storeBitmap(bitmapImage, IMAGE_DIR + "arrow0.png");
                        }
                        bitmapImage = removeAlpha(bitmapImage);
                        if (STORE_IMG) {
                            storeBitmap(bitmapImage, IMAGE_DIR + "arrow.png");
                        }

                        ArrowImage arrowImage = new ArrowImage(bitmapImage);
                        foundArrow = getArrow(arrowImage);
                    }
                }
                logParseMessage();
                updateGaminHudInformation();
            }
            return true;
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
        if(null==unit) {
            return eUnits.None;
        }else {
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
        if (local_language_string == getString(R.string.km))
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
        Arrow foundArrow = Arrow.None;
        for (Arrow a : Arrow.values()) {
            int sad = image.getSAD(a.value);
            if (0 == sad) {
                String shortString = Short.toString(a.value);
                Log.d(TAG, "Recognize " + a.name() + " " + shortString);
                foundArrow = a;
                break;
            }
        }
        return foundArrow;
    }

    private Arrow preArrow = Arrow.None;

    private void processArrow(Arrow arrow) {
        if (preArrow == arrow) {

        } else {
            preArrow = arrow;
        }
        switch (arrow) {
            case SharpRight:
                garminHud.SetDirection(eOutAngle.SharpRight);
                break;
            case Right:
                garminHud.SetDirection(eOutAngle.Right);
                break;
            case EasyRight:
            case KeepRight:
                garminHud.SetDirection(eOutAngle.EasyRight);
                break;
            case RightToLeave:
                garminHud.SetDirection(eOutAngle.EasyRight, eOutType.LongerLane, eOutAngle.AsDirection);
                break;
            case Straight:
                garminHud.SetDirection(eOutAngle.Straight);
                break;

            case GoTo:
                garminHud.SetDirection(eOutAngle.Straight);
                break;
            case EasyLeft:
            case KeepLeft:
                garminHud.SetDirection(eOutAngle.EasyLeft);
                break;
            case LeftToLeave:
                garminHud.SetDirection(eOutAngle.EasyLeft, eOutType.LongerLane, eOutAngle.AsDirection);
                break;
            case Left:
                garminHud.SetDirection(eOutAngle.Left);
                break;
            case SharpLeft:
                garminHud.SetDirection(eOutAngle.SharpLeft);
                break;
            case LeftDown:
                garminHud.SetDirection(eOutAngle.LeftDown);
                break;
            case RightDown:
                garminHud.SetDirection(eOutAngle.RightDown);
                break;
            case ArrivalsRight:
                garminHud.SetDirection(eOutAngle.Right, eOutType.RightFlag, eOutAngle.AsDirection);
                break;
            case ArrivalsLeft:
                garminHud.SetDirection(eOutAngle.Left, eOutType.RightFlag, eOutAngle.AsDirection);
                break;
            case Arrivals:
                garminHud.SetDirection(eOutAngle.Straight, eOutType.RightFlag, eOutAngle.AsDirection);
                break;

            case LeaveRoundabout:
                garminHud.SetDirection(eOutAngle.Right, eOutType.RightRoundabout, eOutAngle.Right);
                break;
            case LeaveRoundaboutUp:
                garminHud.SetDirection(eOutAngle.Straight, eOutType.RightRoundabout, eOutAngle.Straight);
                break;
            case LeaveRoundaboutLeft:
                garminHud.SetDirection(eOutAngle.Left, eOutType.RightRoundabout, eOutAngle.Left);
                break;
            case LeaveRoundaboutRight:
                garminHud.SetDirection(eOutAngle.Right, eOutType.RightRoundabout, eOutAngle.Right);
                break;

            case Convergence:
            case None:
            default:
                garminHud.SetDirection(eOutAngle.AsDirection);
                break;
        }
    }

    private String lastRemainHour = null, lastRemainMinute = null;
    private Arrow lastFoundArrow = Arrow.None;

    private void updateGaminHudInformation() {
        if (null != garminHud) {
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

                if (-1 != int_distance) {
                    garminHud.SetDistance(int_distance, units, decimal, false);
                } else {
                    garminHud.ClearDistance();
                }

            } else {
                garminHud.ClearDistance();
            }
            final boolean distanceSendResult = garminHud.getSendResult();
            //===================================================================================

            //===================================================================================
            // time
            //===================================================================================
            boolean timeSendResult = false;

            if (null != remainMinute) {
                if (showETA) {
                    if (arrivalHour != -1 && arrivalMinute != -1) {
                        boolean sameAsLast = (arrivalHour == lastArrivalHour && arrivalMinute == lastArrivalMinute) ? true : false;

                        if (!sameAsLast) {
                            garminHud.SetTime(arrivalHour, arrivalMinute, false);
                            timeSendResult = garminHud.getSendResult();
                            lastArrivalMinute = arrivalMinute;
                            lastArrivalHour = arrivalHour;
                        }
                    }
                } else {
                    int hh = null != remainHour ? Integer.parseInt(remainHour) : 0;
                    int mm = Integer.parseInt(remainMinute);

                    boolean sameAsLast = null == remainHour ?
                            remainMinute.equals(lastRemainMinute) : remainMinute.equals(lastRemainMinute) && remainHour.equals(lastRemainHour);
                    //need to verify the necessary of check same as last.
                    sameAsLast = false;
                    if (!sameAsLast) {
                        garminHud.SetTime(hh, mm, true);
                        timeSendResult = garminHud.getSendResult();
                        lastRemainMinute = remainMinute;
                        lastRemainHour = remainHour;
                    }
                }
            } else {

            }
            //===================================================================================

            //===================================================================================
            // arrow
            //===================================================================================
            final boolean sameAsLastArrow = false;//foundArrow == lastFoundArrow;
            lastFoundArrow = foundArrow;
            if (!sameAsLastArrow) {
                processArrow(foundArrow);
            }
            final boolean arrowSendResult = sameAsLastArrow ? false : garminHud.getSendResult();
            //===================================================================================

            String sendResultInfo = "dist: " + (distanceSendResult ? '1' : '0')
                    + " time: " + (timeSendResult ? '1' : '0')
                    + " arrow: " + (arrowSendResult ? '1' : '0');
            logi(sendResultInfo);

        }
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
            String[] distSplit = distanceToDest.split(" ");
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
            // Separate EAT-String from value
            if (etaAtFirst) { // ETA-String first, then value (chinese)
                arrivedSplit = timeToArrived.split(ETA);
                arrivalTime = 2 == arrivedSplit.length ? arrivedSplit[1] : null;
            } else { // ETA-value first, then string (english)
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
                arrivalHour = hh;
                arrivalMinute = Integer.parseInt(split[1]);
                if (-1 != pmIndex && 12 != hh) {
                    hh += 12;
                    arrivalTime = hh + ":" + split[1];
                }
            } else { // 24-hour-format
                arrivalTime = arrivalTime.trim();

                String[] split = arrivalTime.split(":");
                arrivalHour = Integer.parseInt(split[0]);
                arrivalMinute = Integer.parseInt(split[1]);
            }
            //======================================================================================

        }

    }

    private String[] splitDigitAndAlphabetic(String str) {
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

    private String[] splitDigitAndNonDigit(String str) {
        String[] result = new String[2];
        for (int x = 0; x < str.length(); x++) {
            char c = str.charAt(x);
            if (!Character.isDigit(c)) {
                result[0] = str.substring(0, x);
                result[1] = str.substring(x);
                break;
            }
        }
        return result;
    }

    private boolean containsOnlyWhitespaces(String str) {
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
        processGoogleMapsNotification(sbn);
    }


    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {

        logi("removed...");
        logi("have " + mCurrentNotificationsCounts + " active notifications");
        mRemovedNotification = sbn;

        String packageName = sbn.getPackageName();
        if (packageName.equals(GOOGLE_MAPS_PACKAGE_NAME)) {

            sendBooleanExtra2MainActivity(getString(R.string.gmaps_notify_catched), false);

            int hh = null != remainHour ? Integer.parseInt(remainHour) : 0;
            int mm = null != remainMinute ? Integer.parseInt(remainMinute) : -1;

            // Check if arrival is possible (don't know if mm==0 work always)
            if (hh == 0 && mm <= 5 && mm != -1) {
                // Arrived: Delete Distance to turn
                if ((lastFoundArrow != Arrow.Arrivals) && (lastFoundArrow != Arrow.ArrivalsLeft) && (lastFoundArrow != Arrow.ArrivalsRight)) {
                    if (garminHud != null) {
                        garminHud.SetDirection(eOutAngle.Straight, eOutType.RightFlag, eOutAngle.AsDirection);
                        garminHud.ClearDistance();
                    }
                } else {
                    if (garminHud != null)
                        garminHud.ClearDistance();
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

    class CancelNotificationReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action;
            if (intent != null && intent.getAction() != null) {
                action = intent.getAction();
                if (action.equals(ACTION_NLS_CONTROL)) {
                    String command = intent.getStringExtra("command");
                    if (TextUtils.equals(command, "cancel_last")) {
                        if (mCurrentNotifications != null && mCurrentNotificationsCounts >= 1) {
                            StatusBarNotification sbnn = getCurrentNotifications()[mCurrentNotificationsCounts - 1];
                            cancelNotification(sbnn.getPackageName(), sbnn.getTag(), sbnn.getId());
                        }
                    } else if (TextUtils.equals(command, "cancel_all")) {
                        cancelAllNotifications();
                    } else if (TextUtils.equals(command, "toogle")) {
                        int a = 1;
                    }
                }
            }
        }

    }

//    public static GarminHUD getGarminHud() {
//        if (garminHud != null) {
//            return garminHud;
//        } else
//            return null;
//    }


}

