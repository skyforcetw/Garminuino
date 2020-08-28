package sky4s.garminhud.hud;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class BMWSocketConnection {
    private static final String TAG = BMWSocketConnection.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final byte[] HUD_ADDRESS = new byte[]{(byte) 192, (byte) 168, 10, 1};
    private static final int HUD_PORT = 50007;
    private static final int RESPONSE_BUFFER_SIZE = 1024;

    private Context mContext;
    private boolean mWifiAvailable;
    private Socket mSocket;
    private final InetAddress mHudAddress;
    private final ConnectivityManager mConnectivityManager;

    private ConnectivityManager.NetworkCallback mNetworkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            if (DEBUG) Log.d(TAG, "onAvailable: WLAN available");
            mConnectivityManager.bindProcessToNetwork(network);
            mWifiAvailable = true;
            ensureConnected();
        }

        @Override
        public void onLosing(Network network, int maxMsToLive) {
            if (DEBUG) Log.d(TAG, "onLosing: WLAN about to be lost");
            disconnectNetwork();
        }

        @Override
        public void onLost(Network network) {
            if (DEBUG) Log.d(TAG, "onLost: WLAN lost");
            disconnectNetwork();
        }

        @Override
        public void onUnavailable() {
            if (DEBUG) Log.d(TAG, "onUnavailable: WLAN unavailable");
            disconnectNetwork();
        }

        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            // unused
        }

        @Override
        public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
            // unused
        }

        @Override
        public void onBlockedStatusChanged(Network network, boolean blocked) {
            // unused
        }

        private void disconnectNetwork() {
            if (DEBUG) Log.d(TAG, "disconnectNetwork()");
            mWifiAvailable = false;
            disconnect();
            mConnectivityManager.bindProcessToNetwork(null);
        }
    };

    public BMWSocketConnection(Context context) {
        mContext = context.getApplicationContext();
        mConnectivityManager = (ConnectivityManager) mContext.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        mWifiAvailable = false;
        InetAddress hudAddress = null;
        try {
            hudAddress = InetAddress.getByAddress(HUD_ADDRESS);
        } catch (UnknownHostException e) {
            // Should never happen
            Log.wtf(TAG, "Unable to create reference to HUD address");
        }
        mHudAddress = hudAddress;
        requestWifiNetwork();
    }

    public boolean send(byte[] buffer) {
        if (DEBUG) Log.d(TAG, "sending message to HUD");
        ensureConnected();

        if (mSocket == null) {
            Log.e(TAG, "Unable to send message, not connected");
            return false;
        }

        byte[] response = new byte[RESPONSE_BUFFER_SIZE];
        try {
            OutputStream out = mSocket.getOutputStream();
            InputStream in = mSocket.getInputStream();
            out.write(buffer);
            int read = in.read(response);
            if (!isOk(response, read)) {
                Log.e(TAG, "Server responded with error");
                return false;
            }
        } catch (IOException e) {
            Log.e(TAG, "Exception sending message", e);
        }
        return true;
    }

    private void disconnect() {
        if (DEBUG) Log.d(TAG, "disconnect()");
        if (mSocket == null) {
            return;
        }
        try {
            mSocket.close();
        } catch (IOException e) {
            // nothing to do
        }
        mSocket = null;
    }

    private void requestWifiNetwork() {
        if (DEBUG) Log.d(TAG, "requestWifiNetwork()");
        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();
        mConnectivityManager.requestNetwork(request, mNetworkCallback);
    }

    private void ensureConnected() {
        if (DEBUG) Log.d(TAG, "ensureConnected(): mWifiAvailable: " + mWifiAvailable);
        if (!mWifiAvailable) {
            return;
        }
        if (mSocket != null) {
            if (DEBUG) Log.d(TAG, "ensureConnected(): Socket already connected");
            return;
        }

        try {
            if (DEBUG) Log.d(TAG, "ensureConnected: Connecting to HUD");
            // This constructor will create, bind, and connect
            mSocket = new Socket(mHudAddress, HUD_PORT, getWifiAddress(), 0);
            if (DEBUG) Log.d(TAG, "Connected to BMW HUD");
        } catch (IOException e) {
            Log.e(TAG, "Exception connecting to HUD", e);
            mSocket = null;
        }
    }

    private InetAddress getWifiAddress() {
        WifiManager wifiManager = (WifiManager) mContext
                .getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo == null) {
            Log.e(TAG, "getWifiInterface: No connection info available");
            return null;
        }

        return intToInetAddress(wifiInfo.getIpAddress());
    }

    private static InetAddress intToInetAddress(int address) {
        byte[] bytes = new byte[4];
        for (int i = 0; i < bytes.length; i++) {
            int shift = i * 8;
            bytes[i] = (byte) ((address >> shift) & 0xff);
        }
        try {
            return InetAddress.getByAddress(bytes);
        } catch (UnknownHostException e) {
            // Should never happen
            Log.wtf(TAG, "Unable to create WLAN address object");
            return null;
        }
    }

    private static boolean isOk(byte[] response, int length) {
        if (length != 5) {
            return false;
        }

        if (DEBUG) {
            Log.d(TAG, "Received server response: " +
                    response[0] + ", " +
                    response[1] + ", " +
                    response[2] + ", " +
                    response[3] + ", " +
                    response[4]);
        }

        // Server always responds with this as OK
        return response[0] == 0x7c &&
                response[1] == 0x04 &&
                response[2] == 0x01 &&
                response[3] == 0x00 &&
                response[4] == 0x00;
    }
}
