package edu.buffalo.cse.cse486586.simpledht;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.util.Log;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.Map;
import java.util.TreeMap;

public class Helper {

    public String TAG = "From Helper Class";

    public String genHash(String input){
        try{
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] sha1Hash = sha1.digest(input.getBytes());
            Formatter formatter = new Formatter();
            for (byte b : sha1Hash){
                formatter.format("%02x", b);
            }
            return formatter.toString();
        }catch(NoSuchAlgorithmException e){
            Log.e(TAG,"Exception: ", e);
        }
        return "";
    }

    public Message joinNewNode(String myPort, String myPortHash){
        Message message = new Message();

        message.setSenderId(myPort);
        message.setReceiverId("5554");  //Set receiver to AVD0
        message.setMessageType(3);      //Set message type to Join
        TreeMap<String,String> data = new TreeMap<String, String>();
        data.put(myPortHash,myPort);
        message.setData(data);

        return message;
    }

    public Message finalJoin(String myPort, TreeMap<String,String> chordNodes){
        Message message = new Message();

        message.setSenderId(myPort);
        message.setData(chordNodes);
        message.setMessageType(7);      //Set message type to Join Update

        return message;
    }

    public String getInsertNodeHash(String key, TreeMap<String,String> chordNodes){
        String keyHash = genHash(key);
        String lastNodeKey = chordNodes.lastKey();
        return (keyHash.compareTo(lastNodeKey) > 0 ? chordNodes.firstKey() : chordNodes.ceilingKey(keyHash));
    }

    public TreeMap<String, String> returnData(Cursor cursor){
        TreeMap<String, String> data = new TreeMap<String, String>();
        int key = cursor.getColumnIndex(SQLContract.COLUMN_KEY);
        int value = cursor.getColumnIndex(SQLContract.COLUMN_VALUE);
        cursor.moveToFirst();
        while(!cursor.isAfterLast()){       //Iterate through the nodes in tree map
            data.put(cursor.getString(key),cursor.getString(value));
            cursor.moveToNext();
        }
        return data;
    }

    public Cursor returnCursor(TreeMap<String, String> data){
        MatrixCursor cursor = new MatrixCursor(new String[]{SQLContract.COLUMN_KEY, SQLContract.COLUMN_VALUE});
        for(Map.Entry<String, String> entry : data.entrySet()){
            cursor.addRow(new Object[]{entry.getKey(),entry.getValue()});
        }
        return cursor;
    }

    public Message insertIntoChord(String myPort, String insertHash, TreeMap<String,String> chordNodes, String key, String value){
        Message message = new Message();
        message.setSenderId(myPort);
        message.setReceiverId(chordNodes.get(insertHash));
        message.setMessageType(1);

        TreeMap<String,String> data = new TreeMap<String, String>();
        data.put(key,value);

        message.setData(data);

        return message;
    }

    public Message queryStar(String myPort, String selection){
        Message message = new Message();

        message.setReceiverId("");
        message.setSenderId(myPort);
        message.setMessageType(5);
        TreeMap<String,String > data = new TreeMap<String, String>();
        data.put(5+"",selection);
        message.setData(data);

        return message;
    }

    public Message defaultQuery(String myPort, String selection, TreeMap<String, String> chordNodes, String hashForKey){
        Message message = new Message();

        message.setSenderId(myPort);
        message.setMessageType(4);
        TreeMap<String,String > data = new TreeMap<String, String>();
        data.put(4+"",selection);
        message.setData(data);
        message.setReceiverId(chordNodes.get(hashForKey));

        return message;
    }

    public Object sendMessage(Message message){
        Object result = null;
        try{
            int port = Integer.parseInt(message.receiverId) *2;
            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),port);

            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            out.writeObject(message);
            out.flush();

            result = in.readObject();

        }catch (Exception e){
            Log.e(TAG,"Exception occurred: ",e);
        }
        return result;
    }
}
