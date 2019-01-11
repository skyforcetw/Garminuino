package com.example.notificationlistenerdemo;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
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

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import sky4s.garmin.GarminHUD;
import sky4s.garmin.eOutAngle;
import sky4s.garmin.eOutType;
import sky4s.garmin.eUnits;

//import android.location.Location;
//import android.location.LocationListener;
//import static android.support.v4.app.ActivityCompatJB.startActivityForResult;


public class NotificationMonitor extends NotificationListenerService {
    private final static boolean STORE_IMG = false;
    private final static String IMAGE_DIR = "/storage/emulated/0/Pictures/";
    private final static boolean DONT_SEND_SAME = false;


    public static final String ACTION_NLS_CONTROL = "com.example.notificationlistenerdemo.NLSCONTROL";
    public final static String GOOGLE_MAPS_PACKAGE_NAME = "com.google.android.apps.maps";
    private static final String TAG = "NCM";
    private static final String TAG_PRE = "[" + NotificationMonitor.class.getSimpleName() + "] ";
    private static final int EVENT_UPDATE_CURRENT_NOS = 0;


    public static List<StatusBarNotification[]> mCurrentNotifications = new ArrayList<StatusBarNotification[]>();
    public static int mCurrentNotificationsCounts = 0;
    public static StatusBarNotification mPostedNotification;
    public static StatusBarNotification mRemovedNotification;
    private CancelNotificationReceiver mReceiver = new CancelNotificationReceiver();

    public static BluetoothSPP bt = null;
    private static GarminHUD garminHud = null;

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

//    private double speed;
//    private LocationRequest mLocationRequest;
//
//    public void onLocationChanged(Location var1) {
//
//    }

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

    public static StatusBarNotification[] getCurrentNotifications() {
        if (mCurrentNotifications.size() == 0) {
            log("mCurrentNotifications size is ZERO!!");
            return null;
        }
        return mCurrentNotifications.get(0);
    }

    private static void log(Object object) {
        Log.i(TAG, TAG_PRE + object);
    }


    @Override
    public void onCreate() {
        super.onCreate();
        log("onCreate...");
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_NLS_CONTROL);
        registerReceiver(mReceiver, filter);
        mMonitorHandler.sendMessage(mMonitorHandler.obtainMessage(EVENT_UPDATE_CURRENT_NOS));

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        log("onBind...");
        return super.onBind(intent);
    }

    private void processGoogleMapsNotification(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        if (packageName.equals(GOOGLE_MAPS_PACKAGE_NAME)) {

            Notification notification = sbn.getNotification();
            if (null == notification) {
                return;
            }
            processGoogleMapsNotification(notification);
        }
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

    private String translate(String chinese) {
        switch (chinese) {
            case "公里":
            case "킬로미터":
                return "km";
            case "公尺":
            case "미터법":
                return "m";
            case "分":
            case "分鐘":
            case "분":
                return "m";
            case "小時":
            case "時":
            case "시간":
//            case getString(R.string.hour):
                return "h";
            default:
                return chinese;
        }
    }

    // Returns the current Unit (Kilometres or Miles) based on distanceToTurn
    public static eUnits getCurrentUnit() {
        if( (get_eUnits(distanceUnit)==eUnits.Kilometres) || (get_eUnits(distanceUnit)==eUnits.Metres) )
            return eUnits.Kilometres;
        else if( (get_eUnits(distanceUnit)==eUnits.Miles) || (get_eUnits(distanceUnit)==eUnits.Foot) )
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
//            return;
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
//                garminHud.SetDirection(eOutAngle.Straight, eOutType.LeftRoundabout, eOutAngle.AsDirection);

//                garminHud.SetDirection((char) eOutAngle.Straight.value,
//                        (char) (eOutType.LeftRoundabout.value + eOutType.RightRoundabout.value),
//                        (char) eOutAngle.AsDirection.value);
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
        if (bt != null || MainActivity.IGNORE_BT) {
            if (null == garminHud) {
                garminHud = new GarminHUD(bt);
            }

            if (null != distanceNum && null != distanceUnit) {
                float float_distance = Float.parseFloat(distanceNum);
                eUnits units = get_eUnits(distanceUnit);

                int int_distance = (int) float_distance;
                boolean decimal = ((eUnits.Kilometres == units)||(eUnits.Miles == units)) && float_distance < 10;

                if (decimal) { //有小數點
                    int_distance = (int) (float_distance * 10);
                }

                if (0 != int_distance) {
//                    garminHud.SetAlphabet('A', 'b', 'C', 'd');
                    garminHud.SetDistance(int_distance, units, decimal, false);
                } else {
                    garminHud.ClearDistance();
                }

            } else {
                garminHud.ClearDistance();
            }
            final boolean distanceSendResult = garminHud.getSendResult();

            boolean timeSendResult = false;
            if (null != remainMinute) {
                int hh = null != remainHour ? Integer.parseInt(remainHour) : 0;
                int mm = Integer.parseInt(remainMinute);

                boolean sameAsLast = null == remainHour ?
                        remainMinute.equals(lastRemainMinute) : remainMinute.equals(lastRemainMinute) && remainHour.equals(lastRemainHour);
                sameAsLast = false;
                if (!sameAsLast) {
                    garminHud.SetTime(hh, mm, true);
                    timeSendResult = garminHud.getSendResult();
                    lastRemainMinute = remainMinute;
                    lastRemainHour = remainHour;
                }

            } else {

            }
            final boolean sameAsLastArrow = false;//foundArrow == lastFoundArrow;
            lastFoundArrow = foundArrow;
            if (!sameAsLastArrow) {
                processArrow(foundArrow);
            }
            final boolean arrowSendResult = sameAsLastArrow ? false : garminHud.getSendResult();

            String sendResultInfo = "dist: " + (distanceSendResult ? '1' : '0')
                    + " time: " + (timeSendResult ? '1' : '0')
                    + " arrow: " + (arrowSendResult ? '1' : '0');
            log(sendResultInfo);

        } else {
            garminHud = null;
        }
    }

    private void processGoogleMapsNotification(Notification notification) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Bundle extras = notification.extras;
            if (extras != null) {
                // 获取通知标题
                String title = extras.getString(Notification.EXTRA_TITLE, "");
                // 获取通知内容
                String content = extras.getString(Notification.EXTRA_TEXT, "");
            }
        } else {
//            List<String> textList = getText(notification);
        }

        // We have to extract the information from the view
        RemoteViews views = notification.bigContentView;
        if (views == null) views = notification.contentView;
        if (views == null) return;

        long currentTime = System.currentTimeMillis();
        notifyPeriodTime = currentTime - lastNotifyTimeMillis;
        lastNotifyTimeMillis = currentTime;

        // Use reflection to examine the m_actions member of the given RemoteViews object.
        // It's not pretty, but it works.
        try {
            Field fieldActions = views.getClass().getDeclaredField("mActions");
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
                if ( (tag != 2 && tag != 12) && (!simpleClassName.equals("ReflectionAction") && !simpleClassName.equals("BitmapReflectionAction")) )
                    continue;

                if(Build.VERSION.SDK_INT <28) {
                    // View ID
                    parcel.readInt();
                }

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
//                            String exitNavigation = getString(R.string.exit_navigation);
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
                }
                // Save times. Comment this section out if the notification time isn't important
                else if (methodName.equals("setTime")) {
                    // Parameter type (5 = Long)
//                    parcel.readInt();
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

            //can update to garmin hud
            //log("notifyPeriodTime: " + notifyPeriodTime);
            String notifyMessage = foundArrow.toString() + " " + distanceNum + distanceUnit +
                    " " + (null == remainHour ? 00 : remainHour) + ":" + remainMinute + " " + remainDistance + remainDistanceUnit + " " + arrivalTime
                    + " (period: " + notifyPeriodTime + ")";
            log(notifyMessage);
            MainActivity.updateMessage(notifyMessage);
            if (0 != updateCount && inNavigation) {
                updateGaminHudInformation();
            }

        }

        // It's not usually good style to do this, but then again, neither is the use of reflection...
        catch (Exception e) {
            Log.e("NotificationClassifier", e.toString());
        }

    }

    private void parseTimeAndDistanceToDest(String timeDistanceStirng) {
        String[] timeDistanceSplit = timeDistanceStirng.split("·");
//        remainTime = remainTimeUnit =
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
                remainDistance = remainDistance.replace(",",".");
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
            if (etaAtFirst) {//前面, 應該是中文
                arrivedSplit = timeToArrived.split(ETA);
                arrivalTime = 2 == arrivedSplit.length ? arrivedSplit[1] : null;
            } else {//後面，可能是英文
                arrivedSplit = timeToArrived.split(ETA);
                arrivalTime = arrivedSplit[0];
            }

            arrivalTime = null != arrivalTime ? arrivalTime.trim() : arrivalTime;
            final int amIndex = arrivalTime.indexOf(getString(R.string.am));
            final int pmIndex = arrivalTime.indexOf(getString(R.string.pm));
            final boolean ampmAtFirst = 0 == amIndex || 0 == pmIndex;
            if (-1 != amIndex || -1 != pmIndex) {
                final int index = Math.max(amIndex, pmIndex);
                arrivalTime = ampmAtFirst ? arrivalTime.substring(index + 2) : arrivalTime.substring(0, index);
                arrivalTime = arrivalTime.trim();

                String[] spilit = arrivalTime.split(":");
                final int hh = Integer.parseInt(spilit[0]);
                if (-1 != pmIndex && 12 != hh) {
                    arrivalTime = (hh + 12) + ":" + spilit[1];
                }
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

    private void parseDistanceToTurn(String distanceString) {
//        auto afterString = getString(R.string.
        final int indexOfHo = distanceString.indexOf("後");
        if (-1 != indexOfHo) {
            distanceString = distanceString.substring(0, indexOfHo);

        }
        String[] splitArray = distanceString.split(" ");
        if (2 != splitArray.length) {
            splitArray = splitDigitAndAlphabetic(distanceString);
        }
        String num = null;
        String unit = null;
        if (splitArray.length == 2) {
            num = splitArray[0].replace(",",".");
            unit = splitArray[1];
        }

        if (null != unit) {
            unit = translate(unit);
        }
        distanceNum = num;
        distanceUnit = unit;
//        return new String[]{num, unit};
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        updateCurrentNotifications();
        log("onNotificationPosted...");
        log("have " + mCurrentNotificationsCounts + " active notifications");
        mPostedNotification = sbn;
//        sbn.getPostTime();
        processGoogleMapsNotification(sbn);
        /*
         * Bundle extras = sbn.getNotification().extras; String
         * notificationTitle = extras.getString(Notification.EXTRA_TITLE);
         * Bitmap notificationLargeIcon = ((Bitmap)
         * extras.getParcelable(Notification.EXTRA_LARGE_ICON)); Bitmap
         * notificationSmallIcon = ((Bitmap)
         * extras.getParcelable(Notification.EXTRA_SMALL_ICON)); CharSequence
         * notificationText = extras.getCharSequence(Notification.EXTRA_TEXT);
         * CharSequence notificationSubText =
         * extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
         * Log.i("SevenNLS", "notificationTitle:"+notificationTitle);
         * Log.i("SevenNLS", "notificationText:"+notificationText);
         * Log.i("SevenNLS", "notificationSubText:"+notificationSubText);
         * Log.i("SevenNLS",
         * "notificationLargeIcon is null:"+(notificationLargeIcon == null));
         * Log.i("SevenNLS",
         * "notificationSmallIcon is null:"+(notificationSmallIcon == null));
         */
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        updateCurrentNotifications();
        log("removed...");
        log("have " + mCurrentNotificationsCounts + " active notifications");
        mRemovedNotification = sbn;
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
            log("Should not be here!!");
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
    
    public static GarminHUD getGarminHud() {
        if(garminHud!=null) {
            return garminHud;
        } else
            return null;
    }

}

class ArrowImage {

    public static final int IMAGE_LEN = 4;
    public static final int CONTENT_LEN = IMAGE_LEN * IMAGE_LEN;
    public boolean[] content = new boolean[CONTENT_LEN];

    public ArrowImage(Bitmap bitmap) {

        final int interval = bitmap.getWidth() / IMAGE_LEN;
        for (int h0 = 0; h0 < IMAGE_LEN; h0++) {
            final int h = h0 * interval;
            for (int w0 = 0; w0 < IMAGE_LEN; w0++) {
                final int w = w0 * interval;
                int p = bitmap.getPixel(w, h);
                final int alpha = (p >> 24) & 0xff;
                final int max = Math.max(Math.max(p & 0xff, (p >> 8) & 0xff), (p >> 16) & 0xff);
                final int max_alpha = (max * alpha) >> 8;
                content[h0 * IMAGE_LEN + w0] = max_alpha < 254;
            }
        }

    }

    public int getSAD(final int magicNumber) {
        int sad = 0;
        for (int x = 0; x < CONTENT_LEN; x++) {
            final boolean bit = 1 == ((magicNumber >> x) & 1);
            sad += content[x] != bit ? 1 : 0;
        }
        return sad;
    }

}
