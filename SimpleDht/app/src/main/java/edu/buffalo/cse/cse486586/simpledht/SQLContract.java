package edu.buffalo.cse.cse486586.simpledht;

public class SQLContract {

    public static final String DATABASE_NAME = "pa3";
    public static final String TABLE_NAME = "KeyValueTable";
    public static final String COLUMN_KEY = "key";
    public static final String COLUMN_VALUE = "value";
    public static final int DATABASE_VERSION = 1;
    public static final String CREATE_TABLE_SQL_QUERY = "CREATE TABLE " + TABLE_NAME +
            "( "+ COLUMN_KEY +" TEXT PRIMARY KEY NOT NULL, " + COLUMN_VALUE +" TEXT NOT NULL);";

}
