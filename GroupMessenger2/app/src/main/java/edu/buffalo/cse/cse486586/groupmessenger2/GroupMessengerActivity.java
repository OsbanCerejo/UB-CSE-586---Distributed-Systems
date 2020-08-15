package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

import static android.content.ContentValues.TAG;

/**
 * GroupMessengerActivity is the main Activity for the assignment.(Removed Log statements)
 * 
 * @author Osban Cerejo
 *
 */

//A custom node for implementing priority queue for this project
class queueNode {
    public String message;
    public int priority;
    public boolean deliveryStatus;
    public int port;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(boolean deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}

public class GroupMessengerActivity extends Activity {

    Button send;
    DatabaseHelper databaseHelper;
    EditText editText;
    TelephonyManager tel;
    String myPort;
    Boolean someoneFailed = false;
    static final String[] REMOTE_PORTS = {"11108", "11112", "11116", "11120", "11124"};
    static final int SERVER_PORT = 10000;
    static int senderSequence = 0;
    int proposedPriority = 0;
    private static final Uri CONTENT_URI = Uri.parse("content://edu.buffalo.cse.cse486586.groupmessenger2.provider");

    //Priority queue with custom comparator to compare the priorities of messages
    static PriorityQueue<queueNode> priorityQueue = new PriorityQueue<queueNode>(10, new Comparator<queueNode>() {
    @Override
    public int compare(queueNode m1, queueNode m2) {
        return Integer.compare(m1.getPriority(), m2.getPriority());
    }
});

    String failedClientPort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        databaseHelper = new DatabaseHelper(this);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */

        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        send = (Button) findViewById(R.id.button4);
        editText = (EditText) findViewById(R.id.editText1);

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String message = editText.getText().toString();
                editText.setText("");

                Log.d(TAG, "Edit text message " + message);

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,message,myPort);
            }
        });
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void>{

        int keyCount = 0;       //Key count to be entered in the SQLite Database
        @Override
        protected Void doInBackground(ServerSocket... serverSockets) {

            //Declare the variables for server task
            ServerSocket serverSocket = serverSockets[0];
            Socket socket;
            Message message;    //Message object to accept the messages sent by client.

            while (true) {
                try {
                    socket = serverSocket.accept(); //Accept the client connection.

                    //Create object output and input streams for sending and receiving objects(Message)
                    ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
                    ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());

                    //Read the initial message sent by the client.
                    message = (Message) objectInputStream.readObject();

                    //If the message is null then the client has failed
                    if (message.message == null) {
                        //Get the failed client port so that we could use it in future.
                        failedClientPort = String.valueOf(message.port);

                        //Set the failed variable to true. This will help in identifying whether
                        // to remove the messages from queue or not
                        someoneFailed = true;
                    } else {    //Client has not failed

                        //Create a message object to send to the client with a proposal.
                        message = new Message("--*--proposal--*--",Math.max(proposedPriority,message.priority)+1,false,message.port);

                        //Write the object(message) to the output stream initialized above
                        objectOutputStream.writeObject(message);
                        objectOutputStream.flush();
                    }

                    //Read the final message sent by the client after finalizing priority.
                    message = (Message) objectInputStream.readObject();

                    //Again check if the Message is null.
                    //Since client could fail at any point of time (i.e. Before or after sending the
                    // final message)
                    //We are checking if the client is failed in both places that is when receiving
                    // initial message and after final message.

                    if (message.message == null) {
                        failedClientPort = String.valueOf(message.port);    //Get failed client

                        //Set the failed variable to true.
                        someoneFailed = true;
                    } else {

                        //If nothing went wrong the proceed with adding the message to the queue.
                        queueNode node = new queueNode();

                        node.setMessage(message.message);
                        node.setPriority(message.priority);
                        node.setPort(message.port);
                        node.setDeliveryStatus(true);

                        priorityQueue.add(node);

                    }

                    //If someone Failed then it will be true so do the following.
                    if(someoneFailed){
                        //Remove all the messages sent by the failed client from the queue.
                        Iterator nodeIterator = priorityQueue.iterator();
                        while(nodeIterator.hasNext()){
                            queueNode qnode = (queueNode) nodeIterator.next();
                            if(qnode.getPort() == Integer.parseInt(failedClientPort)){
                                priorityQueue.remove(qnode);
                            }
                        }
                    }

                    //Finally check for all the deliverable messages and publish them one at a time.
                    Iterator iterator = priorityQueue.iterator();
                    while(iterator.hasNext() && priorityQueue.peek().isDeliveryStatus()){
                        queueNode node = priorityQueue.poll();
                        publishProgress(node.getMessage(), String.valueOf(node.getPort()));
                    }

                } catch (StreamCorruptedException e) {
                    Log.e(TAG, "Server StreamCorrupted Exception");
                    e.printStackTrace();
                } catch (SocketTimeoutException e) {
                    Log.e(TAG, "Server Socket Timeout Exception");
                    e.printStackTrace();
                } catch (EOFException e) {
                    Log.e(TAG, "Server EOF Exception");
                    e.printStackTrace();
                } catch (IOException e) {
                    Log.e(TAG, "Server IO Exception");
                    e.printStackTrace();
                } catch (Exception e) {
                    Log.e(TAG, "Server Exception");
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {

            Log.d(TAG,"In onProgressUpdate");

            //Get the stuff sent by publish progress
            String strReceived = values[0].trim();
            int myPortReceived = Integer.parseInt(values[1]);

            Log.d("Received Port server :", String.valueOf(myPortReceived));

            TextView textView = (TextView) findViewById(R.id.textView1);

            //Used colors for personal debugging and understanding
            switch (myPortReceived){
                case 11108: {
                    Log.d("Case 1:", String.valueOf(myPortReceived));
                    String styledText = ""+myPortReceived+" :<font color='red'>"+strReceived+"</font>.";
                    textView.append(Html.fromHtml(styledText));
                    textView.append("\t\n\n");
                    break;
                }
                case 11112: {
                    Log.d("Case 2:", String.valueOf(myPortReceived));
                    String styledText = ""+myPortReceived+"<font color='blue'>"+strReceived+"</font>.";
                    textView.append(Html.fromHtml(styledText));
                    textView.append("\t\n\n");
                    break;
                }
                case 11116: {
                    Log.d("Case 3:", String.valueOf(myPortReceived));
                    String styledText = ""+myPortReceived+" :<font color='green'>"+strReceived+"</font>.";
                    textView.append(Html.fromHtml(styledText));
                    textView.append("\t\n\n");
                    break;
                }
                case 11120: {
                    Log.d("Case 4:", String.valueOf(myPortReceived));
                    String styledText = ""+myPortReceived+" :<font color='yellow'>"+strReceived+"</font>.";
                    textView.append(Html.fromHtml(styledText));
                    textView.append("\t\n\n");
                    break;
                }
                case 11124: {
                    Log.d("Case 5:", String.valueOf(myPortReceived));
                    String styledText = ""+myPortReceived+" :<font color='black'>"+strReceived+"</font>.";
                    textView.append(Html.fromHtml(styledText));
                    textView.append("\t\n\n");
                    break;
                }
            }

            ContentValues contentValues = new ContentValues();
            contentValues.put("key", keyCount);
            contentValues.put("value", strReceived);
            keyCount++;
            getContentResolver().insert(CONTENT_URI,contentValues);
            return;
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void>{

        @Override
        protected Void doInBackground(String... strings) {

            //Declaration and initializations of variables
            ArrayList<Integer> proposalList = new ArrayList<Integer>();
            Socket socketArray[] = new Socket[5];
            Socket socket;
            String messageToSend = strings[0];
            int p = 0;
            int finalSequenceNumber = 0;
            senderSequence++;

            //Array of streams to use them later for re-broadcasting messages.
            ObjectOutputStream outputStreamArray[] = new ObjectOutputStream[5];

            //Creating initial Message object to send to all other AVDs
            Message initialMessage = new Message(messageToSend, senderSequence, false, Integer.parseInt(myPort));

            //Multi-cast the created initial message
            for (String remotePort : REMOTE_PORTS) {
                try {
                    //Connecting to server
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remotePort));
                    //Saving the socket for later use.
                    socketArray[p] = socket;

                    //Creating the input and output streams.
                    ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());

                    //Saving the output stream since we will need it to use later. Input stream not needed
                    outputStreamArray[p] = objectOutputStream;

                    //Timeout used as mentioned in the assignment pdf to listen to proposal
                    socket.setSoTimeout(2000);

                    //Send the initial message by writing it to stream
                    objectOutputStream.writeObject(initialMessage);
                    objectOutputStream.flush();

                    //Read the proposal sent by server. This will contain a proposed priority.
                    initialMessage = (Message) objectInputStream.readObject();

                    //Add the priority to the proposal list.
                    proposalList.add(Math.max(initialMessage.priority,finalSequenceNumber));
                    p++;

                }  catch (EOFException e) {
                    Log.e(TAG, "Client initial EOF Exception");
                } catch (StreamCorruptedException e) {
                    Log.e(TAG, "Client initial StreamCorrupted Exception");
                }  catch (SocketTimeoutException e) {
                    Log.e(TAG, "Client initial Socket Timeout");
                } catch (IOException e) {
                    Log.e(TAG, "Client initial IO Exception");
                } catch (Exception e) {
                    Log.e(TAG, "Client initial Exception");
                }
            }

            //After all the 5 priorities have been received find the maximum priority.
            Collections.sort(proposalList,Collections.<Integer>reverseOrder());
            finalSequenceNumber = proposalList.get(0);

            //Create a final message with the maximum priority and message to send.
            Message finalMessage = new Message(messageToSend,finalSequenceNumber,false,Integer.parseInt(myPort));

            //Again multi-cast the message to all the clients.
            for (int i = 0; i < REMOTE_PORTS.length; i++) {
                try {
                    //Used the previously stored socket and output stream.
                    socket = socketArray[i];
                    ObjectOutputStream objectOutputStream = outputStreamArray[i];

                    //Write the final message to the stream.
                    objectOutputStream.writeObject(finalMessage);
                    objectOutputStream.flush();

                    //Close the socket as the message has now been delivered.
                    socket.close();

                } catch (EOFException e) {
                    Log.e(TAG, "Client final EOF Exception");
                } catch (StreamCorruptedException e) {
                    Log.e(TAG, "Client final StreamCorrupted Exception");
                }  catch (SocketTimeoutException e) {
                    Log.e(TAG, "Client final Socket Timeout");
                } catch (IOException e) {
                    Log.e(TAG, "Client final IO Exception");
                } catch (Exception e) {
                    Log.e(TAG, "Client final Exception");
                }
            }
            return null;
        }
    }
}
