package edu.buffalo.cse.cse486586.simpledynamo;

import android.util.Log;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

public class Helper{

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

    public String getInsertHash(String key, Helper helper, SimpleDynamoProvider.CircularList<String> chordNodes) {
        String hashKey = helper.genHash(key);
        String insertHash = chordNodes.get(0);
        for (int i = 0; i < chordNodes.size(); i++) {
            if (hashKey.compareTo(chordNodes.get(i)) < 0) {
                insertHash = chordNodes.get(i);
                break;
            }
        }
        return insertHash;
    }

    public int avdTelNumber(int portNumber) {
        if (portNumber == 11108) {
            return 5554;
        } else if (portNumber == 11112) {
            return 5556;
        } else if (portNumber == 11116) {
            return 5558;
        } else if (portNumber == 11120) {
            return 5560;
        } else if (portNumber == 11124) {
            return 5562;
        } else {
            return -1;
        }
    }

    public int getPortFromHash(String hash) {
        if (hash.equals("33d6357cfaaf0f72991b0ecd8c56da066613c089")) {
            return 11108;
        } else if (hash.equals("208f7f72b198dadd244e61801abe1ec3a4857bc9")) {
            return 11112;
        } else if (hash.equals("abf0fd8db03e5ecb199a9b82929e9db79b909643")) {
            return 11116;
        } else if (hash.equals("c25ddd596aa7c81fa12378fa725f706d54325d12")) {
            return 11120;
        } else if (hash.equals("177ccecaec32c54b82d5aaafc18a2dadb753e3b1")) {
            return 11124;
        } else {
            return 0;
        }

    }

    public String getHashFromPort(int portNumber) {
        if (portNumber == 11108) {
            return "33d6357cfaaf0f72991b0ecd8c56da066613c089";
        } else if (portNumber == 11112) {
            return "208f7f72b198dadd244e61801abe1ec3a4857bc9";
        } else if (portNumber == 11116) {
            return "abf0fd8db03e5ecb199a9b82929e9db79b909643";
        } else if (portNumber == 11120) {
            return "c25ddd596aa7c81fa12378fa725f706d54325d12";
        } else if (portNumber == 11124) {
            return "177ccecaec32c54b82d5aaafc18a2dadb753e3b1";
        } else {
            return "";
        }

    }

}