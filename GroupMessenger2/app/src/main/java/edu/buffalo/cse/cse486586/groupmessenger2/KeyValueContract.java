package edu.buffalo.cse.cse486586.groupmessenger2;

import android.provider.BaseColumns;

/**Key Value Contract for Database Helper class
 *
 * @author Osban Cerejo
 *
 * */

public class KeyValueContract {

    private KeyValueContract(){}

    public static class SQLContract implements BaseColumns{

        public static final String DATABASE_NAME = "keyValue.db";
        public static final String TABLE_NAME = "keyValueTable";
        public static final String COLUMN_KEY = "[key]";
        public static final String COLUMN_VALUE = "value";
    }

    public static final String SQL_CREATE_QUERY = "CREATE TABLE "+ SQLContract.TABLE_NAME+
            " ("+ SQLContract.COLUMN_KEY+" STRING PRIMARY KEY, "+ SQLContract.COLUMN_VALUE+" STRING)";

    public static final String SQL_DROP_QUERY = "DROP TABLE IF EXISTS " + KeyValueContract.SQLContract.TABLE_NAME;

}
