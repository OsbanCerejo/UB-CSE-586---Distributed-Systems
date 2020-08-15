package edu.buffalo.cse.cse486586.simpledht;

import java.io.Serializable;
import java.util.TreeMap;

public class Message implements Serializable {
    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public TreeMap<String, String> getData() {
        return data;
    }

    public void setData(TreeMap<String, String> data) {
        this.data = data;
    }


    String senderId;
    String receiverId;
    TreeMap<String,String> data;
    int messageType;


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

    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    @Override
    public String toString() {
        return "Message{" +
                "senderId='" + senderId + '\'' +
                ", receiverId='" + receiverId + '\'' +
                ", data=" + data + '\'' +
                ", type=" + messageType +
                '}';
    }
}
