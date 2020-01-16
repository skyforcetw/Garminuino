package sky4s.garminhud.app;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MyCrashHandler implements Thread.UncaughtExceptionHandler {

    private static MyCrashHandler crashHandler;
    private static SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
    private static Date curDate = new Date(System.currentTimeMillis());//獲取當前時間
    private static String str = formatter.format(curDate);
    private static String TAG = "MyCrashHandler";
    //系統默認的UncaughtException處理類
    private Thread.UncaughtExceptionHandler mDefaultHandler;
    //程序的Context對象
    private Context mContext;
    //用來存儲設備信息和異常信息
    private Map<String, String> infos = new HashMap<String, String>();


    @Override
    public void uncaughtException(Thread thread, Throwable ex) {

        if (!handleException(ex) && mDefaultHandler != null) {
            //如果用戶沒有處理則讓系統默認的異常處理器來處理
            mDefaultHandler.uncaughtException(thread, ex);
        } else {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Log.e(TAG, "error : ", e);
            }
            //退出程序
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }

    /**
     * 自定義錯誤處理,收集錯誤信息 發送錯誤報告等操作均在此完成.
     *
     * @param ex
     * @return true:如果處理了該異常信息;否則返回false.
     */
    private boolean handleException(Throwable ex) {
        if (ex == null) {
            return false;
        }
        //使用Toast來顯示異常信息
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();//準備發消息的MessageQueue
                Toast.makeText(mContext, "Sorry, App hang...", Toast.LENGTH_LONG).show();
                Looper.loop();
            }
        }.start();
        //收集設備參數信息
        collectDeviceInfo(mContext);
        //保存日誌文件
        saveCrashInfo2File(ex);
        return true;
    }


    /**
     * 保存錯誤信息到文件中
     *
     * @param ex
     * @return 返回文件名稱, 便於將文件傳送到服務器
     */
    private String saveCrashInfo2File(Throwable ex) {

        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, String> entry : infos.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key + "=" + value + "\n");
        }

        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        String result = writer.toString();
        sb.append(result);
        try {
            long timestamp = System.currentTimeMillis();

            String fileName = "crash-" + str + "-" + timestamp + ".log";
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                String path = Environment.getExternalStorageDirectory().getPath() + "/crash/";
                File dir = new File(path);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(path + fileName);
                fos.write(sb.toString().getBytes());
                System.out.println(sb.toString());
                fos.close();
            }
            return fileName;
        } catch (Exception e) {
            Log.e(TAG, "an error occured while writing file...", e);
        }
        return null;
    }

    /**
     * 初始化
     *
     * @param context
     */
    public void init(Context context) {
        mContext = context;
        //獲取系統默認的UncaughtException處理器
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        //設置該CrashHandler為程序的默認處理器
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    /**
     * 收集設備參數信息
     *
     * @param ctx
     */
    public void collectDeviceInfo(Context ctx) {
        try {
            PackageManager pm = ctx.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(), PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                String versionName = pi.versionName == null ? "null" : pi.versionName;
                String versionCode = ((PackageInfo) pi).versionCode + "";
                infos.put("versionName", versionName);
                infos.put("versionCode", versionCode);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "an error occured when collect package info", e);
        }
        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                infos.put(field.getName(), field.get(null).toString());
                Log.d(TAG, field.getName() + " : " + field.get(null));
            } catch (Exception e) {
                Log.e(TAG, "an error occured when collect crash info", e);
            }
        }
    }

    private MyCrashHandler() {
    }

    //單例
    public static MyCrashHandler instance() {
        if (crashHandler == null) {
            crashHandler = new MyCrashHandler();
        }
        return crashHandler;
    }
}