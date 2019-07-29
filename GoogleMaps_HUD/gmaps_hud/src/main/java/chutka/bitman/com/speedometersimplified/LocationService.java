package chutka.bitman.com.speedometersimplified;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import sky4s.garminhud.app.NotificationMonitor;
import sky4s.garminhud.app.R;
import sky4s.garminhud.eUnits;
import sky4s.garminhud.hud.HUDInterface;

/**
 * Created by vipul on 12/13/2015.
 */
public class LocationService extends Service implements
        LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final long INTERVAL = 200 * 2;
    private static final long FASTEST_INTERVAL = 200 * 1;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;

    public static double speed;
//    public static HUDInterface hud;

    private final IBinder mBinder = new LocalBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
//        hud = NotificationMonitor.getGarminHud();
        createLocationRequest();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();


        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //========================================================================================
        // messageer
        //========================================================================================
//        msgReceiver = new MsgReceiver();
//        IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction(getString(R.string.broadcast_receiver_location_service));
//        registerReceiver(msgReceiver, intentFilter);
        //========================================================================================
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        unregisterReceiver(msgReceiver);
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onConnected(Bundle bundle) {
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        } catch (SecurityException e) {
        }
    }


    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
//        distance = 0;
    }


    @Override
    public void onConnectionSuspended(int cause) {
        if (cause == CAUSE_NETWORK_LOST) { // not tested
//            if (null != hud) {
//                setSpeed((int) speed, false);
//            }
        }
    }

    private void sendLocationExtraByBroadcast(String key, Location location) {
        Intent intent = new Intent(getString(R.string.broadcast_receiver_main_activity));
        intent.putExtra(getString(R.string.whoami), getString(R.string.broadcast_sender_location_service));
        intent.putExtra(key, location);
        sendBroadcast(intent);
    }

    private void sendSpeedExtraByBroadcast(double speed) {
        Intent intent = new Intent(getString(R.string.broadcast_receiver_main_activity));
        intent.putExtra(getString(R.string.whoami), getString(R.string.broadcast_sender_location_service));
        intent.putExtra(getString(R.string.gps_speed), speed);
        sendBroadcast(intent);
    }

    @Override
    public void onLocationChanged(Location location) {
//        mCurrentLocation = location;
//        if (lStart == null) {
//            lStart = mCurrentLocation;
//            lEnd = mCurrentLocation;
//        } else
//            lEnd = mCurrentLocation;

        //calculating the speed with getSpeed method it returns speed in m/s so we are converting it into kmph
        if (eUnits.Kilometres == NotificationMonitor.getCurrentUnit() || eUnits.None == NotificationMonitor.getCurrentUnit()) {
            speed = location.getSpeed() * 18 / 5;
        } else if (eUnits.Miles == NotificationMonitor.getCurrentUnit()) {
            speed = location.getSpeed() * 2236 / 1000;
        }
        sendSpeedExtraByBroadcast(speed);

        //Calling the method below updates the  live values of distance and speed to the TextViews.
//        updateHUD();

        //send lat/lon for recognize history
//        sendLocationExtraByBroadcast(getString(R.string.broadcast_receiver_notification_monitor), getString(R.string.location), location);
//        final double latitude = location.getLatitude();
//        final double longitude = location.getLongitude();
//        this.sendBroadcast();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    public class LocalBinder extends Binder {

        public LocationService getService() {
            return LocationService.this;
        }


    }

    //The live feed of Distance and Speed are being set in the method below .
//    private void updateHUD() {
//        if (null == hud)
//            return;
//        if (speed >= 0.0) {
//            setSpeed((int) speed, true);
//        } else {
//            clearSpeed();
//        }

//        lStart = lEnd;
//    }


    private void setSpeed(int nSpeed, boolean bIcon) {
//        if (null != hud) {
//            if (isInNavigating) {
//                hud.SetSpeed(nSpeed, bIcon);
//            } else {
//                hud.SetDistance(nSpeed, eUnits.None);
//            }
//        }
    }

    private void clearSpeed() {
//        if (null != hud) {
//            if (isInNavigating) {
//                hud.ClearSpeedandWarning();
//            } else {
//                hud.ClearDistance();
//            }
//        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stopLocationUpdates();
        if (mGoogleApiClient.isConnected())
            mGoogleApiClient.disconnect();
//        lStart = null;
//        lEnd = null;
//        distance = 0;
        return super.onUnbind(intent);
    }

//    private MsgReceiver msgReceiver;
//    private boolean isInNavigating = false;
//    private boolean navigatingConfirmed = false;

//    private class MsgReceiver extends BroadcastReceiver {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            boolean has_in_navigation = intent.hasExtra(getString(R.string.is_in_navigation));
//            boolean prevIsInNavigating = isInNavigating;
//            isInNavigating = intent.getBooleanExtra(getString(R.string.is_in_navigation), isInNavigating);
//
//            //works only after navigation confirmed
//            if (has_in_navigation && prevIsInNavigating != isInNavigating) {
//                // Delete Speed in last line, when showing speed in distance line (when navigation finished)
////                if (null != hud) {
////                    /**
////                     * original is "not logic", different with others, curious!
////                     * change to no not logic to try.
////                     */
////                    //if (!isInNavigating) { //original
////                    if (isInNavigating) { // from no navigating to navigating
////                        hud.ClearDistance();
////                    } else { // from  navigating to no navigating
////                        hud.ClearSpeedandWarning();
////                    }
////                }
//            }
//        }
//    }
}

