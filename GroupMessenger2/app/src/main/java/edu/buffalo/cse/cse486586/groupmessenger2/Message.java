package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.Serializable;

public class Message implements Serializable {

    public String message;
    public int priority;
    public boolean deliveryStatus;
    public int port;

    public Message(String message, int priority, boolean deliveryStatus, int port) {
        this.message = message;
        this.priority = priority;
        this.deliveryStatus = deliveryStatus;
        this.port = port;
    }
}
