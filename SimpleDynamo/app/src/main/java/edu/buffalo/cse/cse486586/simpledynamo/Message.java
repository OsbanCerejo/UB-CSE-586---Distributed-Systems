package edu.buffalo.cse.cse486586.simpledynamo;

import java.io.Serializable;
import java.util.TreeMap;

public class Message implements Serializable {

    String key;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    String value;

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    String messageType;

    @Override
    public String toString() {
        return "Message{" +
                "key='" + key + '\'' +
                ", value='" + value + '\'' +
                ", messageType='" + messageType + '\'' +
                '}';
    }
}
