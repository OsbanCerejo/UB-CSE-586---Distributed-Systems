package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 * 
 * Please read:
 * 
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * 
 * before you start to get yourself familiarized with ContentProvider.
 * 
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 * 
 * @author stevko
 *
 */
public class GroupMessengerProvider extends ContentProvider {

    DatabaseHelper databaseHelper;
    SQLiteDatabase dbReader, dbWriter;

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /*
         * TODO: You need to implement this method. Note that values will have two columns (a key
         * column and a value column) and one row that contains the actual (key, value) pair to be
         * inserted.
         * 
         * For actual storage, you can use any option. If you know how to use SQL, then you can use
         * SQLite. But this is not a requirement. You can use other storage options, such as the
         * internal storage option that we used in PA1. If you want to use that option, please
         * take a look at the code for PA1.
         */
        long checker = dbWriter.insertWithOnConflict(KeyValueContract.SQLContract.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);

        Log.d("Inserted :", values.toString());
        Log.d("Key of inserted: ", String.valueOf(checker));
        return uri;
    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        databaseHelper = new DatabaseHelper(getContext());
        dbReader = databaseHelper.getReadableDatabase();
        dbWriter = databaseHelper.getWritableDatabase();
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        /*
         * TODO: You need to implement this method. Note that you need to return a Cursor object
         * with the right format. If the formatting is not correct, then it is not going to work.
         *
         * If you use SQLite, whatever is returned from SQLite is a Cursor object. However, you
         * still need to be careful because the formatting might still be incorrect.
         *
         * If you use a file storage option, then it is your job to build a Cursor * object. I
         * recommend building a MatrixCursor described at:
         * http://developer.android.com/reference/android/database/MatrixCursor.html
         */
        Log.d("Value sent to cursor",selection);
        String[] selection1={selection};

        Cursor cursor = dbReader.query(
                KeyValueContract.SQLContract.TABLE_NAME,    // The table to query
                null,                               // The array of columns to return (pass null to get all)
                "key=?",                            // The columns for the WHERE clause
                selection1,                                 // The values for the WHERE clause
                null,                                   // don't group the rows
                null,                               // don't filter by row groups
                sortOrder                                   // The sort order
        );

        Log.v("Value from cursor", String.valueOf(cursor.getColumnName(0)));
        Log.v("Key from cursor", String.valueOf(cursor.getColumnName(1)));
        Log.d("cursor - column count", String.valueOf(cursor.getColumnCount()));
        Log.d("Actual Cursor", DatabaseUtils.dumpCursorToString(cursor));
        return cursor;
    }
}
