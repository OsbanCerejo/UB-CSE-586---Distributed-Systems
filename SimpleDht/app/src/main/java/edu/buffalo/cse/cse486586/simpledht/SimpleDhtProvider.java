package edu.buffalo.cse.cse486586.simpledht;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SimpleDhtProvider extends ContentProvider {

    private SQLiteDatabase db;      //Instance of SQLite database
    private Helper helper;          //Instance of Helper class containing all functions

    static final String TAG = "PA3";
    static String myPort;           //Port of the local AVD
    static String myPortHash;       //Hash of the local AVD Port
    final int SERVER_PORT = 10000;  //Server Port
    static String AVD0Port = "5554";//Leader port or default port for join

    TreeMap<String, String> chordNodes = new TreeMap<String, String>();
                                    //TreeMap Data Structure used to maintain ordering of nodes in chord

    @Override
    public boolean onCreate() {
        DatabaseHelper databaseHelper = new DatabaseHelper(getContext());
        db = databaseHelper.getWritableDatabase();  //Get Instance of Database Helper class

        helper = new Helper();                      //Get Instance of Helper class
        
        try{
            TelephonyManager telephonyManager = (TelephonyManager) this.getContext().getSystemService(Context.TELEPHONY_SERVICE);
            myPort = telephonyManager.getLine1Number().substring(telephonyManager.getLine1Number().length() - 4);
        }catch(NullPointerException e){
            Log.e(TAG, "Unable to get port number: ", e );
        }
        myPortHash = new Helper().genHash(myPort);  //Get the hash of current local port
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
                                                    //Start the server task
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket: ", e);
        }

        chordNodes.put(myPortHash,myPort);          //Add the node to the chord
        if(!myPort.equals(AVD0Port)){               //If node is not avd0 then send join request to avd0
            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, new Helper().joinNewNode(myPort,myPortHash));
        }
        return false;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        if(selection.equalsIgnoreCase("*")){           //Delete from all the avd's
            db.delete(SQLContract.TABLE_NAME,null,null);
        }else if(selection.equalsIgnoreCase("@")){      //Delete from local avd only
            db.delete(SQLContract.TABLE_NAME,null,null);
        }else{                                                 //Delete conditionally.
            db.delete(SQLContract.TABLE_NAME,"key=?", new String[]{selection});
        }
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        try{
            String key = values.getAsString(SQLContract.COLUMN_KEY);        //Get the Key to insert
            String value = values.getAsString(SQLContract.COLUMN_VALUE);    //Get the Value to insert

            String insertHash = helper.getInsertNodeHash(key,chordNodes);   //Check the edge cases and then get the final hash

            if(insertHash.equalsIgnoreCase(myPortHash)){                    //If inserting on self
                db.replace(SQLContract.TABLE_NAME, "", values);
            }else{                                                          //Send to the successor on which the insert will take place
                Message message;
                message = helper.insertIntoChord(myPort, insertHash, chordNodes, key, value);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,message).get(1000, TimeUnit.MILLISECONDS);
            }
            return uri;
        } catch (Exception e){
            Log.e(TAG,"Insert: Exception ",e);
        }
        throw new SQLException("Exception while inserting Key");
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        int queryType = 1;
        if(selection.equalsIgnoreCase("*")){        //Query from all the AVD's
            queryType = 1;
        }else if(selection.equalsIgnoreCase("@")){  //Query from the local AVD only
            queryType = 2;
        }else{                                             //Query Conditionally
            queryType = 3;
        }
        try{
            switch (queryType){                             //Cases to check what type of query it is
                case 1:                                     //Global Query
                    Message allMessage;
                    allMessage = helper.queryStar(myPort,selection);
                    return (Cursor) new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,allMessage).get();
                case 2:                                     //Local Query
                    return db.query(SQLContract.TABLE_NAME, null, null, null, null, null, null);
                case 3:                                     //Conditional Query
                    String hashForKey = helper.getInsertNodeHash(selection,chordNodes);
                    if(hashForKey.equalsIgnoreCase(myPortHash)){
                        return db.query(SQLContract.TABLE_NAME, null, SQLContract.COLUMN_KEY + "=?", new String[]{selection}, null, null, null);
                    }else{
                        Message qMessage;
                        qMessage = helper.defaultQuery(myPort,selection,chordNodes, hashForKey);
                        return (Cursor) new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,qMessage).get();
                    }
            }
        }catch (Exception e){
            Log.e(TAG,"Exception: ",e);
        }
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    private class ServerTask extends AsyncTask<ServerSocket, Message, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];         //Create a server socket

            while(true){                                    //Always Listen for incoming connections
                try {
                    Socket socket =  serverSocket.accept(); //Accept incoming connection

                    ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream objectInputStream  = new ObjectInputStream(socket.getInputStream());

                    Message recievedMessage = (Message) objectInputStream.readObject();
                                                            //Read the message sent by client.

                    /**
                     * Message type
                     *
                     * 1: Insert
                     * 2: Delete
                     * 3: Join
                     * 4: Query
                     * 5: Query *
                     * 6: Delete *
                     * 7: Join Update
                     *
                     * @return
                     */

                    switch (recievedMessage.messageType){
                        case 1:             //Insert into Database through Content Values
                            ContentValues contentValues = new ContentValues();

                            contentValues.put(SQLContract.COLUMN_KEY,recievedMessage.getData().firstEntry().getKey());
                            contentValues.put(SQLContract.COLUMN_VALUE, recievedMessage.getData().firstEntry().getValue());
                            db.replace(SQLContract.TABLE_NAME, "", contentValues);
                            break;

                        case 2:             //Delete from database
                            db.delete(SQLContract.TABLE_NAME, SQLContract.COLUMN_KEY + "=?", new String[]{recievedMessage.getData().firstEntry().getValue()});
                            break;

                        case 3:             //Join a new node to the chord
                            chordNodes.put(recievedMessage.getData().firstKey(), recievedMessage.getData().firstEntry().getValue());
                            Message msg;
                            msg = helper.finalJoin(myPort,chordNodes);
                            publishProgress(msg);   //Joined now call Client task
                            break;

                        case 4:             //Query from database.
                            String searchValue = recievedMessage.getData().firstEntry().getValue();
                            Cursor cursor = db.query(SQLContract.TABLE_NAME, null, SQLContract.COLUMN_KEY + "=?", new String[]{searchValue}, null, null, null);
                            recievedMessage.setData(helper.returnData(cursor));
                            objectOutputStream.writeObject(recievedMessage);
                            break;
                        case 5:             //Query from all the databases
                            Cursor cursorAll = db.query(SQLContract.TABLE_NAME, null, null, null, null, null, null);
                            recievedMessage.setData(helper.returnData(cursorAll));
                            objectOutputStream.writeObject(recievedMessage);
                            break;
                        case 6:             //Delete from all databases
                            db.delete(SQLContract.TABLE_NAME, null, null);
                            break;
                        case 7:             //Update the join node
                            chordNodes = recievedMessage.data;
                            break;
                        default:
                            break;
                    }
                    objectInputStream.close();
                    objectOutputStream.close();
                    socket.close();
                } catch (Exception e) {
                    Log.e(TAG, "Exception in ServerSocket: ", e);
                }
            }
        }

        protected void onProgressUpdate(Message...msgs) {
            Message message = msgs[0];
            Log.d("Message in Publish", String.valueOf(message.getData()));
            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,message);
        }
    }

    private class ClientTask extends AsyncTask<Message, Void, Object> {

        @Override
        protected Object doInBackground(Message... msgs) {
            Message message = msgs[0];

            /**
             * Message type
             *
             * 1: Insert
             * 2: Delete
             * 3: Join
             * 4: Query
             * 5: Query *
             * 6: Delete *
             * 7: Join Update
             *
             * @return
             */

            if(message.getMessageType() == 1 || message.getMessageType() == 2 || message.getMessageType() == 3){
                return helper.sendMessage(message);
            }else if( message.getMessageType() == 4){
                Message message4 = (Message) helper.sendMessage(message);
                return helper.returnCursor(message4.data);
            }else if( message.getMessageType() == 5){
                TreeMap<String, String> data = new TreeMap<String, String>();
                for(String port: chordNodes.values()){
                    message.setReceiverId(port);
                    if(port.equalsIgnoreCase(myPort)){
                        data.putAll(helper.returnData(db.query(SQLContract.TABLE_NAME,null,null,null,null,null,null)));
                    }else{
                        Message message5 = (Message) helper.sendMessage(message);
                        data.putAll(message5.data);
                    }
                }
                return helper.returnCursor(data);
            }else if( message.getMessageType() == 6){
                for(String port: chordNodes.values()){
                    message.setReceiverId(port);
                    if(port.equalsIgnoreCase(myPort)){
                        db.delete(SQLContract.TABLE_NAME,null,null);;
                    }else{
                        helper.sendMessage(message);
                    }
                }
            }else if(message.getMessageType() == 7){
                for(String port: chordNodes.values()){
                    message.setReceiverId(port);
                    helper.sendMessage(message);
                }
            }
            return null;
        }
    }
}
