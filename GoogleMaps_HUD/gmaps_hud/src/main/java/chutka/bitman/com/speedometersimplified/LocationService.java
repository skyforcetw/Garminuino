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

import sky4s.garminhud.GarminHUD;
import sky4s.garminhud.app.NotificationMonitor;
import sky4s.garminhud.app.R;
import sky4s.garminhud.eUnits;

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
    Location mCurrentLocation, lStart, lEnd;
    static double distance = 0;
    public static double speed;
    private GarminHUD garminHud;

    public void setGarminHUD(GarminHUD hud) {
        this.garminHud = hud;
    }

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

        //========================================================================================
        // messageer
        //========================================================================================
        msgReceiver = new MsgReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(getString(R.string.broadcast_receiver_localtion_service));
        registerReceiver(msgReceiver, intentFilter);
        //========================================================================================

        return mBinder;
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
        distance = 0;
    }


    @Override
    public void onConnectionSuspended(int cause) {
        if (cause == CAUSE_NETWORK_LOST) { // not tested
            if (null != garminHud)
                setSpeed((int) speed, false);
        }
    }


    @Override
    public void onLocationChanged(Location location) {
//        MainActivity.locate.dismiss();
        mCurrentLocation = location;
        if (lStart == null) {
            lStart = mCurrentLocation;
            lEnd = mCurrentLocation;
        } else
            lEnd = mCurrentLocation;

        //calculating the speed with getSpeed method it returns speed in m/s so we are converting it into kmph
        if (eUnits.Kilometres == NotificationMonitor.getCurrentUnit() || eUnits.None == NotificationMonitor.getCurrentUnit()) {
            speed = location.getSpeed() * 18 / 5;
        } else if (eUnits.Miles == NotificationMonitor.getCurrentUnit()) {
            speed = location.getSpeed() * 2236 / 1000;
        }

        //Calling the method below updates the  live values of distance and speed to the TextViews.
        updateHUD();

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
    private void updateHUD() {
        if (null == garminHud)
            return;
        if (speed >= 0.0) {
            setSpeed((int) speed, true);
        } else
            clearSpeed();

        lStart = lEnd;
    }


    private void setSpeed(int nSpeed, boolean bIcon) {
        if (isOnNavigating) {
            garminHud.SetSpeed(nSpeed, bIcon);
        } else {
            garminHud.SetDistance(nSpeed, eUnits.None);
        }
    }

    private void clearSpeed() {
        if (isOnNavigating) {
            garminHud.ClearSpeedandWarning();
        } else {
            garminHud.ClearDistance();
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stopLocationUpdates();
        if (mGoogleApiClient.isConnected())
            mGoogleApiClient.disconnect();
        lStart = null;
        lEnd = null;
        distance = 0;
        return super.onUnbind(intent);
    }

    private MsgReceiver msgReceiver;
    private boolean isOnNavigating = false;

    private class MsgReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            isOnNavigating = intent.getBooleanExtra(getString(R.string.is_on_navigating), isOnNavigating);
            int a = 1;
//            String notify_msg = intent.getStringExtra(getString(R.string.notify_msg));
//            if (null != notify_msg) {
//                textViewDebug.setText(notify_msg);
//            } else {
//
//                boolean notify_catched = intent.getBooleanExtra(getString(R.string.notify_catched), switchNotificationCatched.isChecked());
//                boolean gmaps_notify_catched = intent.getBooleanExtra(getString(R.string.gmaps_notify_catched), switchGmapsNotificationCatched.isChecked());
//                boolean notify_parse_failed = intent.getBooleanExtra(getString(R.string.notify_parse_failed), false);
//
//                if (notify_parse_failed) {
//
//                } else {
//                    switchNotificationCatched.setChecked(notify_catched);
//                    switchGmapsNotificationCatched.setChecked(gmaps_notify_catched);
//                    sendBooleanExtra2NotificationMonitor(getString(R.string.broadcast_receiver_localtion_service), getString(R.string.is_on_navigating), isOnNavigating());
//
//                }
//            }
        }
    }
}

