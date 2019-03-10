package sky4s.garminhud.app;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.location.Location;
import android.support.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;

import sky4s.garminhud.Arrow;
import sky4s.garminhud.ArrowImage;

public class RecognizeDBHelper extends SQLiteOpenHelper {
    private final static int DB_VERSION = 1; //<-- 版本
    private final static String DB_NAME = "GarminHUD.db";  //<-- db name
    private final static String TABLE_NAME = "Recognize"; //<-- table name

    // 編號表格欄位名稱，固定不變
    public static final String KEY_ID = "_id";
    // 其它表格欄位名稱
    public static final String DATETIME_COLUMN = "datetime";
    public static final String ARROW_IMG_COLUMN = "image";
    public static final String ARROW_SMALL_IMG_COLUMN = "small_image";
    public static final String ARROW_COLUMN = "arrow";

    public static final String LATITUDE_COLUMN = "latitude";
    public static final String LONGITUDE_COLUMN = "longitude";

    // 使用上面宣告的變數建立表格的SQL指令
    public static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    DATETIME_COLUMN + " INTEGER NOT NULL, " +
                    LATITUDE_COLUMN + " REAL, " +
                    LONGITUDE_COLUMN + " REAL, " +
                    ARROW_IMG_COLUMN + " BLOB, " +
                    ARROW_SMALL_IMG_COLUMN + " BLOB, " +
                    ARROW_COLUMN + " INTEGER) ";


    // 資料庫物件
    private SQLiteDatabase database;

    public RecognizeDBHelper(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        getDatabase(context);
    }

    private static byte[] bitmap2ByteArray(Bitmap arrowImage) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        arrowImage.compress(Bitmap.CompressFormat.PNG, 100, bos);
        byte[] vByteArray = bos.toByteArray();
        return vByteArray;
    }

    public boolean insert(Location location, Bitmap arrowImage, ArrowImage arrowSmallImage, Arrow arrow) {
// 1.先將 Bitmap 轉換成 byte[]


// 2.透過 byte[] 儲存到 blob 欄位
        ContentValues values = new ContentValues();
        Calendar c = Calendar.getInstance();
        values.put(DATETIME_COLUMN, c.getTimeInMillis());
        if (null != location) {
            values.put(LATITUDE_COLUMN, location.getLatitude());
            values.put(LONGITUDE_COLUMN, location.getLongitude());
        }
        values.put(ARROW_IMG_COLUMN, bitmap2ByteArray(arrowImage));
        values.put(ARROW_SMALL_IMG_COLUMN, bitmap2ByteArray(arrowImage));
        values.put(ARROW_COLUMN, arrow.valueLeft);
        // 3.db 儲存
        database.insert(TABLE_NAME, null, values);
//        read();
        return false;
    }

    public boolean read() {
        Cursor cursor = database.query(
                TABLE_NAME,   // The table to query
                null,             // The array of columns to return (pass null to get all)
                null,              // The columns for the WHERE clause
                null,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                null               // The sort order
        );

        while (cursor.moveToNext()) {
            byte[] image = cursor.getBlob(cursor.getColumnIndex(ARROW_IMG_COLUMN));
            int a = 1;
//            long itemId = cursor.getLong(
//                    cursor.getColumnIndexOrThrow(FeedEntry._ID));
//            itemIds.add(itemId);
        }

        return false;
    }


    // 需要資料庫的元件呼叫這個方法，這個方法在一般的應用都不需要修改
    private void getDatabase(Context context) {
        if (database == null || !database.isOpen()) {
            database = getWritableDatabase();
        }

//        return database;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 建立應用程式需要的表格
        // 待會再回來完成它
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 刪除原有的表格
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        // 待會再回來完成它

        // 呼叫onCreate建立新版的表格
        onCreate(db);
    }
}
