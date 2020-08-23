package sky4s.garminhud.hud;

import android.content.Context;
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
    private Socket mSocket;
    private final InetAddress mHudAddress;

    public BMWSocketConnection(Context context) {
        mContext = context;
        InetAddress hudAddress = null;
        try {
            hudAddress = InetAddress.getByAddress(HUD_ADDRESS);
        } catch (UnknownHostException e) {
            // Should never happen
            Log.wtf(TAG, "Unable to create reference to HUD address");
        }
        mHudAddress = hudAddress;
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

    private void ensureConnected() {
        if (mSocket != null) {
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
