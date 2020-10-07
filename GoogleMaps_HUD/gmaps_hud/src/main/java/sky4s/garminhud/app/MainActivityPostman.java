package sky4s.garminhud.app;

import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;

/**
 * For other class can send message to Main Activity
 */
public class MainActivityPostman {
    private String whoami;
    private String toWho;
    private Context context;

    public static MainActivityPostman toMainActivityInstance(Context context, String whoami) {
        return new MainActivityPostman(context, whoami, context.getString(R.string.broadcast_receiver_main_activity));
    }

    private MainActivityPostman(Context context, String whoami, String toWho) {
        this.context = context;
        this.whoami = whoami;
        this.toWho = toWho;
    }

    private Intent intent2Main = null;

    private void checkIntentForExtra() {
        if (null == intent2Main) {
            intent2Main = new Intent(toWho);
        }
    }

    public void addBooleanExtra(String key, boolean b) {
        checkIntentForExtra();
        intent2Main.putExtra(key, b);
    }

    public void addStringExtra(String key, String string) {
        checkIntentForExtra();
        intent2Main.putExtra(key, string);
    }

    public void addParcelableExtra(String key, Parcelable p) {
        checkIntentForExtra();
        intent2Main.putExtra(key,p);
    }

    public void sendIntent2MainActivity() {
        if (null != intent2Main) {
            addStringExtra(context.getString(R.string.whoami), whoami);
            context.sendBroadcast(intent2Main);
            intent2Main = null;
        }
    }
}