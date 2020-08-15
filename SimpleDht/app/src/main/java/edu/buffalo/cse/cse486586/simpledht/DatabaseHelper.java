package edu.buffalo.cse.cse486586.simpledht;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    DatabaseHelper(Context context) {
        super(context, SQLContract.TABLE_NAME, null, SQLContract.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQLContract.CREATE_TABLE_SQL_QUERY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(oldVersion<newVersion)
        {
            db.execSQL("DROP TABLE IF EXISTS "+SQLContract.TABLE_NAME);
            db.execSQL(SQLContract.CREATE_TABLE_SQL_QUERY);
        }
    }


}