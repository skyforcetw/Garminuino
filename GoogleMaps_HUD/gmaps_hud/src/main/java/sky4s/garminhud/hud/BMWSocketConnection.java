package sky4s.garminhud.hud;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class BMWSocketConnection {
    private static final String TAG = BMWSocketConnection.class.getSimpleName();

    private static final byte[] HUD_ADDRESS = new byte[]{(byte) 192, (byte) 168, 0, 10};
    private static final int HUD_PORT = 50007;
    private static final int RESPONSE_BUFFER_SIZE = 1024;

    private Socket mSocket;
    private final InetAddress mHudAddress;

    public BMWSocketConnection() {
        InetAddress hudAddress = null;
        try {
            hudAddress = InetAddress.getByAddress(HUD_ADDRESS);
        } catch (UnknownHostException e) {
            // Should never happen
        }
        mHudAddress = hudAddress;
    }

    public boolean send(byte[] buffer) {
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
            // This constructor will create and connect
            mSocket = new Socket(mHudAddress, HUD_PORT);
        } catch (IOException e) {
            Log.e(TAG, "Exception connecting to HUD", e);
            mSocket = null;
        }
    }

    private static boolean isOk(byte[] response, int length) {
        if (length != 5) {
            return false;
        }

        // Server always responds with this as OK
        return response[0] == 0x7c &&
                response[1] == 0x04 &&
                response[2] == 0x01 &&
                response[3] == 0x00 &&
                response[4] == 0x00;
    }
}
