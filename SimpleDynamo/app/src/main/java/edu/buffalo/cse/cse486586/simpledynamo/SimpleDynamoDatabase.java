package edu.buffalo.cse.cse486586.simpledynamo;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class SimpleDynamoDatabase extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "pa4.db";
    public static final String TABLE_NAME = "KeyValueTable";

    static final String TAG = SimpleDynamoDatabase.class.getSimpleName();

    SQLiteDatabase db;

    public SimpleDynamoDatabase(Context context) {
        super(context, DATABASE_NAME, null, 2);
        db = this.getWritableDatabase();
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " (key TEXT , value TEXT, Timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }


    public void deleteData(String key) {
        db = this.getWritableDatabase();
        if (key.equals("*")) {
            db.execSQL("DELETE FROM " + TABLE_NAME);
        }
        else if(key.equals("@")) {
            db.execSQL("DELETE FROM " + TABLE_NAME);
        }
        else {
            db.execSQL("DELETE FROM "+TABLE_NAME+" WHERE key=\""+key+"\"");
        }
    }

    public MatrixCursor returnCursor(String key) {
        Cursor cursor;
        db = this.getReadableDatabase();
        if (key.equals("@")) {
            cursor = db.rawQuery("SELECT key,value FROM " + TABLE_NAME,null);
        } else if (key.equals("*")) {
            cursor = db.rawQuery("SELECT key,value FROM " + TABLE_NAME,null);
        } else {
            cursor = db.rawQuery("SELECT key,value FROM " + TABLE_NAME + " WHERE key='" + key + "'"+" ORDER BY Timestamp DESC LIMIT 1",null);
        }
        MatrixCursor returnCursor = new MatrixCursor(new String[]{"key", "value"});
        String[] values = new String[2];
        cursor.moveToFirst();
        try {
            if (cursor != null) {
                do {
                    for (int i = 0; i < cursor.getColumnCount(); i++) {

                        if(i%2 == 0) {
                            if(cursor.getString(i) != null)
                                values[0] = cursor.getString(i);
                        } else if(i%2 != 0) {
                            values[1] = cursor.getString(i);
                            returnCursor.addRow(values);
                            values[0] = null;
                            values[1] = null;
                        }
                    }
                } while (cursor.moveToNext());
            }
        }
        catch (CursorIndexOutOfBoundsException e) {
            Log.e(TAG,e.toString());
        }

        returnCursor.moveToFirst();
        cursor.close();
        return returnCursor;
    }
}
