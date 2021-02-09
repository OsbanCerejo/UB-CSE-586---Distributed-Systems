package edu.buffalo.cse.cse486586.simpledynamo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SimpleDynamoProvider extends ContentProvider {

	ReentrantLock lock = new ReentrantLock();
	SimpleDynamoDatabase simpleDynamoDatabase;
	Helper helper = new Helper();
	static final String TAG = SimpleDynamoProvider.class.getSimpleName();
	static final int SERVER_PORT = 10000;
	static int myPort, avdNumber;
	static int failedPort=-1;
	static String myHash;
	static String rows = "";
	static Queue<String> failedList = new LinkedList<String>();
	public int[] avdSequence = {11124,11112,11108,11116,11120};

	static CircularList<String> chordNodes = new CircularList<String>();

	DatabaseHelper dbHelper;

	public static MatrixCursor putDataIntoCursor(String input) {
		StringTokenizer stringTokenizer = new StringTokenizer(input, ";");
		MatrixCursor rowData = new MatrixCursor(new String[]{"key", "value"});
		String[] values = new String[2];

		while (stringTokenizer.hasMoreElements()) {
			if(stringTokenizer.hasMoreElements()){
				values[0] = stringTokenizer.nextElement().toString();
			}else{
				values[0] = null;
			}
			if(stringTokenizer.hasMoreElements()){
				values[1] = stringTokenizer.nextElement().toString();
			}else{
				values[1] = null;
			}
			rowData.addRow(values);
			values[0] = "";
			values[1] = "";
		}
		return rowData;
	}

	@Override
	public boolean onCreate() {
		dbHelper = new DatabaseHelper(getContext());
		dbHelper.getWritableDatabase();

		TelephonyManager tel = (TelephonyManager) this.getContext().getSystemService(Context.TELEPHONY_SERVICE);
		String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
		myPort = (Integer.parseInt(portStr) * 2);
		avdNumber = helper.avdTelNumber(myPort);
		myHash = helper.genHash(String.valueOf(avdNumber));
		String[] chordHashes = new String[5];
		for(int i = 0; i < avdSequence.length; i++){
			chordHashes[i] = helper.getHashFromPort(avdSequence[i]);
		}
		for (int i = 0; i < 5; i++) {
			chordNodes.add(chordHashes[i]);
		}
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(SERVER_PORT);
		} catch (IOException e) {
		}
		new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);

		broadcastMessage("7;"+myPort);
		return true;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {

		String nodeHash = null;
		if (selection.equals("*")) {
			broadcastMessage("5");
		} else if (selection.equals("@")) {
			simpleDynamoDatabase.deleteData("*");
		} else {
			nodeHash = helper.getInsertHash(selection,helper,chordNodes);
			String deleteMessage = "4;" + selection;

			int nodeNumber = -1;
			for (int i = 0; i< chordNodes.size(); i++) {
				if(nodeHash.equals(chordNodes.get(i))) {
					nodeNumber = i;
				}
			}
			sendMessage(deleteMessage,helper.getPortFromHash(chordNodes.get(nodeNumber)));
			sendMessage(deleteMessage,helper.getPortFromHash(chordNodes.get(nodeNumber+1)));
			sendMessage(deleteMessage,helper.getPortFromHash(chordNodes.get(nodeNumber+2)));
		}
		return 0;
	}

	public void callDelete(String selection) {
		simpleDynamoDatabase = new SimpleDynamoDatabase(getContext());
		simpleDynamoDatabase.getWritableDatabase();
		simpleDynamoDatabase.deleteData(selection);
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		String key = (String) values.get("key");
		String value = (String) values.get("value");
		String insertMessage = "1;" + key + ";" + value;

		String insertHash = helper.getInsertHash(key,helper,chordNodes);
		int nodeNumber = -1;
		for (int i = 0; i < chordNodes.size(); i++) {
			if(insertHash.equals(chordNodes.get(i))) {
				nodeNumber = i;
			}
		}

		if(insertHash.equals(myHash)) {
			dbHelper.insertData(values);
			new SimpleDynamoProvider.ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, insertMessage, Integer.toString(helper.getPortFromHash(chordNodes.get(nodeNumber+1))));
			new SimpleDynamoProvider.ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, insertMessage, Integer.toString(helper.getPortFromHash(chordNodes.get(nodeNumber+2))));
		} else {
			new SimpleDynamoProvider.ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, insertMessage, Integer.toString(helper.getPortFromHash(chordNodes.get(nodeNumber))));
			new SimpleDynamoProvider.ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, insertMessage, Integer.toString(helper.getPortFromHash(chordNodes.get(nodeNumber+1))));
			new SimpleDynamoProvider.ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, insertMessage, Integer.toString(helper.getPortFromHash(chordNodes.get(nodeNumber+2))));
		}
		return uri;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
						String sortOrder) {
		try {
			String fetchRows = "";
			String queryCordHash = "";
			lock.lock();
			simpleDynamoDatabase = new SimpleDynamoDatabase(getContext());
			simpleDynamoDatabase.getReadableDatabase();
			int queryType = 4;
			if (selection.equalsIgnoreCase("*")) {
				queryType = 1;
			} else if (selection.equalsIgnoreCase("@")) {
				queryType = 2;
			} else {
				queryType = 3;
			}

			if (queryType == 1) {
				for (int i = 0; i < chordNodes.size(); i++) {
					int[] ports = {11108,11112,11116,11120,11124};
					String message = "2;" + "@";
					String data = "";
					try {
						data = new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,message, Integer.toString(ports[i])).get();
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (ExecutionException e) {
						e.printStackTrace();
					}
					if (!data.equals("ack")) {
						fetchRows += ";" + data;
					}
				}
				return putDataIntoCursor(fetchRows);

			} else {
				if (queryType == 2) {
					return simpleDynamoDatabase.returnCursor(selection);
				} else {
					queryCordHash = helper.getInsertHash(selection, helper, chordNodes);
					if (queryCordHash.equals(myHash)) {
						return simpleDynamoDatabase.returnCursor(selection);
					} else {

						String requestDataFromAllAvds = "2;" + selection;

						int nodeNumber=-1;
						for (int i = 0; i< chordNodes.size(); i++) {
							if(queryCordHash.equals(chordNodes.get(i))) {
								nodeNumber = i;
							}
						}

						try {
							String requestData = new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, requestDataFromAllAvds, Integer.toString(helper.getPortFromHash(chordNodes.get(nodeNumber)))).get();
							if(requestData.equals("ack")) {
								requestData = new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, requestDataFromAllAvds, Integer.toString(helper.getPortFromHash(chordNodes.get(nodeNumber+1)))).get();
								if (requestData.equals("ack")) {
									requestData = new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, requestDataFromAllAvds, Integer.toString(helper.getPortFromHash(chordNodes.get(nodeNumber+2)))).get();
								}
							}
						} catch (InterruptedException e) {
							e.printStackTrace();
						} catch (ExecutionException e) {
							e.printStackTrace();
						}
						return putDataIntoCursor(rows);
					}
				}
			}
		} finally {
			lock.unlock();
		}
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	private class ClientTask extends AsyncTask<String, String, String> {
		@Override
		protected String doInBackground(String... msgs) {
			Log.e(TAG, "Entered the client task!");
			StringTokenizer slasher = new StringTokenizer(msgs[0], ";");
			String messageType = slasher.nextToken();
			String message = msgs[0];

			try {
				Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10,0,2,2}), Integer.parseInt(msgs[1]));
				socket.setSoTimeout(2000);

				Log.e(TAG, "Created the socket!");

				DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
				DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
				dataOutputStream.writeUTF(message);

				switch (Integer.parseInt(messageType)){
					case 1:			//Case for insert
					case 2:			//Case for query
						rows ="";
						String receivedResponse = dataInputStream.readUTF();
						if(receivedResponse!=null) {
							if (rows.equals("")) {
								rows += receivedResponse;
							} else {
								rows = "";
								rows += receivedResponse;
							}
						}
						return rows;
					case 3:			//Case for query all
						receivedResponse = dataInputStream.readUTF();
						return receivedResponse;
				}
			} catch (SocketTimeoutException e) {
				Log.e(TAG, "Socket timeout!");
			} catch (IOException e) {
				failedPort = Integer.parseInt(msgs[1]);
				if(messageType.equals("1") || messageType.equals("4")) {
					failedList.add(message);		//Add the message to the failed messages list
				}
				broadcastMessage("6;"+failedPort); // Notify all avds about the failed port
			}
			return "ack";
		}

		protected void onProgressUpdate(String... strings) {
		}
	}

	private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
		@Override
		protected Void doInBackground(ServerSocket... sockets) {
			String receivedMessage = null;

			while (true) {
				ServerSocket serverSocket = sockets[0];
				Socket socket = null;
				try {

					socket = serverSocket.accept();		//Accept the incoming connection
					DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
					DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

					receivedMessage = dataInputStream.readUTF();		//Read the message sent by the client

					StringTokenizer stringTokenizer = new StringTokenizer(receivedMessage, ";");
					String messageType = stringTokenizer.nextToken();
														//We use String Tokenizer to seperate the string or message sent by delimiters and sort of use them as list. with each seperated string acting as token

					/**
					 *  Insert : 1
					 *  Query : 2
					 *  Query All : 3
					 *  Delete : 4
					 *  Delete All : 5
					 *  Failure hadnling : 6
					 *  Recovery : 7
					 *
					 * **/

					switch (Integer.parseInt(messageType)){
						case 1:							//
							ContentValues cv = new ContentValues();
							String key = stringTokenizer.nextToken();
							String value = stringTokenizer.nextToken();
							cv.put("key", key);
							cv.put("value", value);
							dbHelper.insertData(cv);			//Insert data into the database
							dataOutputStream.writeUTF("inserted");		//Send as an acknowledgement
							break;
						case 2:
							String query = stringTokenizer.nextToken();
							String dataToReturn = "";

							SimpleDynamoDatabase sdp = new SimpleDynamoDatabase(getContext());
							Cursor cursor = sdp.returnCursor(query);

							if (cursor.moveToFirst() && cursor != null) {		//Iterate through all the queroed data and add them to the string as tokens to then dismantle at the receiving end
								do {
									for (int i = 0; i < cursor.getColumnCount(); i++) {
										if (cursor.isLast()) {
											if (i == 0) {
												dataToReturn += cursor.getString(i) + ";";
											} else {
												dataToReturn += cursor.getString(i);
											}
										} else {
											dataToReturn += cursor.getString(i) + ";";
										}
									}
								} while (cursor.moveToNext());
								dataOutputStream.writeUTF(dataToReturn);
							}
							break;
						case 3:		//Do nothing
						case 4:
							callDelete(stringTokenizer.nextToken());		//Delete
							break;
						case 5:
							callDelete("@");
							break;
						case 6:
							failedPort = Integer.parseInt(stringTokenizer.nextToken());
							break;
						case 7:
							int port = Integer.parseInt(stringTokenizer.nextToken());
							if (failedPort == port) {
								failedPort = -1;
								while(!failedList.isEmpty()) {
									String messageToSend = failedList.poll();
									StringTokenizer st = new StringTokenizer(messageToSend,";");
									if(st.nextToken().equals("1") || st.nextToken().equals("4")) {
										sendMessage(messageToSend,port);
									}
								}
							}
					}
				} catch (IOException e) {
					e.printStackTrace();
				} catch (NullPointerException e) {
					e.printStackTrace();
				}
			}
		}

		protected void onProgressUpdate(String... strings) {
		}
	}
	static class CircularList<Integer> extends ArrayList<Integer> {
		//Overriding the get function. If a value greater than the size of the list is asked for, it simply wraps around
		@Override
		public Integer get(int index) {
			return super.get(index % size());
		}
	}

	private void sendMessage(String message, int port) {
		if(port==failedPort) {			//If port to send to is failed currently then add it to list instead
			failedList.add(message);
		} else {						//If not then send the message.
			new SimpleDynamoProvider.ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message, Integer.toString(port));
		}
	}

	private void broadcastMessage(String message) {			//Send the message to all the AVDs
		int[] ports = {11108,11112,11116,11120,11124};
		for (int i = 0 ;i < ports.length ; i++) {
			sendMessage(message, ports[i]);
		}
	}
}
