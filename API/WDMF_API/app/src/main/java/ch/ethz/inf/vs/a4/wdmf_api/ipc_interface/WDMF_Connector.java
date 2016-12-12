package ch.ethz.inf.vs.a4.wdmf_api.ipc_interface;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Jakob on 25.11.2016.
 *
 * This abstract class will be provided to all clients. They will need to implement
 * the listener for new messages and can then use the class to communicate to our service,
 * In that way, the IPC going on in the background is completely hidden to our API clients.
 *
 */

public abstract class WDMF_Connector extends Service {
    // Application messages are byte arrays that will be sent to other applications through our API
    // Use this to send a single application message. Store it in the field with the name obj.
    public static final int IPC_MSG_SEND_SINGLE_MESSAGE = 1;
    // Use this to send multiple application messages at once.
    // Store them in a ArrayList and put it in the field with the name obj.
    public static final int IPC_MSG_SEND_MESSAGE_LIST = 2;
    // This is used to receive a single application message. It will be stored in the field with the name obj.
    public static final int IPC_MSG_RECV_SINGLE_MESSAGE = 3;
    // This is used to receive multiple application messages at once.
    // They will be stored in a ArrayList and put it in the field with the name obj.
    public static final int IPC_MSG_RECV_MESSAGE_LIST = 4;
    // Send this to the main Service to start listening to the messages with our appID
    // Make sure to write the Receiving Messenger in the replyTo field of the message
    public static final int IPC_MSG_LISTEN = 5;
    // Setting the configuration values. ACKs will come back with true / false in first argument
    public static final int IPC_MSG_SET_TAG = 6;
    public static final int IPC_MSG_SET_TAG_ACK = 7;
    public static final int IPC_MSG_SET_TIMEOUT = 8;
    public static final int IPC_MSG_SET_TIMEOUT_ACK = 9;
    public static final int IPC_MSG_SET_BUFFER_SIZE = 10;
    public static final int IPC_MSG_SET_BUFFER_SIZE_ACK = 11;
    // This is used to find where exactly the Service resides in the namespace
    private static final String packageName = "ch.ethz.inf.vs.a4.wdmf_api";
    private static final String serviceName = "ch.ethz.inf.vs.a4.wdmf_api.service.MainService";
    // Used preference key (Putting them in the string resources xml doesn't really work)
    public static final String networkNamePK = "PK network name";
    public static final String timeoutPK = "PK timeout";
    public static final String bufferSizePK = "PK buffer size";
    // And for reading the content Provider
    static final String MyPrefsUri = "content://ch.ethz.inf.vs.a4.wdmf_api.provider/any";
    // ABSTRACT PART ( MUST OVERWRITE )

    public abstract void onReceiveMessage(byte[] message);

    // OPTIONAL OVERWRITE

    // Overwrite this in client application if you want to treat a whole list of messages differently.
    // If this function is not overwritten, each message in the list will cause a onReceiveMessage invocation.
    public void onReceiveMessageList(ArrayList<byte[]> list) { throw new UnsupportedOperationException("Not implemented"); };

    // IMPLEMENTATION

    Messenger sendingMessenger = null;
    final Messenger receivingMessenger = new Messenger(new IncomingHandler());
    int appID;
    Context surroundingContext;
    boolean bound;

    //  Constructor, takes a Context as parameter since it will be used later
    //  The application tag should be consistent among all apps who want to receive messages from eachother
    public WDMF_Connector(Context c, String applicationTag){
        surroundingContext = c;

        // generate a appID dependent of the provided String tag
        appID = 7;
        for (int i = 0; i < applicationTag.length(); i++) {
            appID = appID*31 + applicationTag.charAt(i);
        }
    }

    // Call in onStart of Context
    public void connectToWDMF() {
        // Bind to the service
        Intent intentForMainService = new Intent();
        intentForMainService.setComponent(new ComponentName(packageName, serviceName));
        surroundingContext.bindService(intentForMainService, mConnection,
                Context.BIND_AUTO_CREATE);

        // BindService is asynchronous, so the rest of the setup happens in onServiceConnected
    }

    // Call in onStop of Context
    public void disconnectFromWDMF() {
        // Unbind from the service
        if (bound) {
            surroundingContext.unbindService(mConnection);
            bound = false;
        }
    }

    // Returns false in case something went wrong
    // It is in the users responsibility to send the message again if it didn't succeed
    public boolean broadcastMessage(byte[] data) {
        if (!bound) {
            Log.d("WDMF_Connector", "Error: The WDMF is not bound but a message wants to be broadcast.");
            return false;
        }
        Log.d("WDMF_Connector", "broadcast message.");
        // Copy data // unnecessary since it will be serialized anyway
        //byte[] dataCopy = Arrays.copyOfRange(data, 0, data.length);

        // Create and send a message to the service
        Message msg = Message.obtain(null, IPC_MSG_SEND_SINGLE_MESSAGE, appID, 0);
        Bundle b = new Bundle();
        b.putByteArray("data", data);
        msg.setData(b);

        try {
            sendingMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    // Returns false in case something went wrong
    // It is in the users responsibility to send the messages again if it didn't succeed
    public boolean broadcastMessages(ArrayList<byte[]> data) {
        if (!bound) {
            Log.d("WDMF_Connector", "Error: The WDMF is not bound but messages want to be broadcast.");
            return false;
        }
        Log.d("WDMF_Connector", "broadcast message list.");

        // Create and send a message to the service
        Message msg = Message.obtain(null, IPC_MSG_SEND_SINGLE_MESSAGE, appID, 0);
        Bundle b = new Bundle();
        b.putSerializable("dataList", data);
        msg.setData(b);

        try {
            sendingMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public String getNetworkTag(){
        try{
            Cursor prefsCursor = surroundingContext.getContentResolver().query(Uri.parse(MyPrefsUri), null, null, null,null);
            prefsCursor.moveToFirst();
            String result =  prefsCursor.getString(prefsCursor.getColumnIndex(bufferSizePK));
            prefsCursor.close();
            return result;
        }catch (Exception e){
            e.printStackTrace();
            return "No network found";
        }
    }

    public void setNetworkTag(String networkTag){
        if (!bound) {
            Log.d("WDMF_Connector", "Error: The WDMF is not bound but a command to change the network tag appeared.");
            return;
        }
        Log.d("WDMF_Connector", "Send IPC Message to set the network tag to " + networkTag);

        // Create and send a message to the service
        Message msg = Message.obtain(null, IPC_MSG_SET_TAG, appID, 0);
        Bundle b = new Bundle();
        b.putString(networkNamePK, networkTag);
        msg.setData(b);
        msg.replyTo = receivingMessenger;

        try {
            sendingMessenger.send(msg);
        } catch (RemoteException e) {
            Log.d("WDMF_Connector", "Error: Sending the IPC message to change the network tag failed");
            e.printStackTrace();
        }
    }

    public long get_buffer_size(){
        try{
            Cursor prefsCursor = surroundingContext.getContentResolver().query(Uri.parse(MyPrefsUri), null, null, null,null);
            prefsCursor.moveToFirst();
            long result =  prefsCursor.getLong(prefsCursor.getColumnIndex(bufferSizePK));
            prefsCursor.close();
            return result;
        }catch (Exception e){
            e.printStackTrace();
            return -2;
        }
    }

    public void set_buffer_size(long buffersize){
        if (!bound) {
            Log.d("WDMF_Connector", "Error: The WDMF is not bound but a command to change the max buffer size appeared.");
            return;
        }
        Log.d("WDMF_Connector", "Send IPC Message to set the buffer size to " + buffersize);

        // Create and send a message to the service
        Message msg = Message.obtain(null, IPC_MSG_SET_BUFFER_SIZE, appID, 0);
        Bundle b = new Bundle();
        b.putLong(bufferSizePK, buffersize);
        msg.setData(b);
        msg.replyTo = receivingMessenger;

        try {
            sendingMessenger.send(msg);
        } catch (RemoteException e) {
            Log.d("WDMF_Connector", "Error: Sending the IPC message to change the buffer size failed");
            e.printStackTrace();
        }
    }

    public int get_timeout() {
        try{
            Cursor prefsCursor = surroundingContext.getContentResolver().query(Uri.parse(MyPrefsUri), null, null, null,null);
            prefsCursor.moveToFirst();
            int result =  prefsCursor.getInt(prefsCursor.getColumnIndex(timeoutPK));
            prefsCursor.close();
            return result;
        }catch (Exception e){
            e.printStackTrace();
            return -2;
        }
    }

    public void set_timeout(int timeout){
        if (!bound) {
            Log.d("WDMF_Connector", "Error: The WDMF is not bound but a command to change the timeout appeared.");
            return;
        }
        Log.d("WDMF_Connector", "Send IPC Message to set the timeout to " + timeout);

        // Create and send a message to the service
        Message msg = Message.obtain(null, IPC_MSG_SET_TIMEOUT, appID, timeout);
        msg.replyTo = receivingMessenger;

        try {
            sendingMessenger.send(msg);
        } catch (RemoteException e) {
            Log.d("WDMF_Connector", "Error: Sending the IPC message to change the timeout failed");
            e.printStackTrace();
        }
    }




    /**
     * Class for sending messages to the main WDMF service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            sendingMessenger = new Messenger(service);
            bound = true;

            // Register to listen to messages with our appID
            Message msg = Message.obtain(null, IPC_MSG_LISTEN, appID, 0);
            msg.replyTo = receivingMessenger;
            try {
                sendingMessenger.send(msg);
            } catch (RemoteException e) {
                Log.d("WDMF_Connector", "Couldn't reach WDMF main service, please make sure it is running.");
                e.printStackTrace();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            sendingMessenger = null;
            bound = false;
        }
    };

    /**
     * Handler of incoming messages from the main WDMF service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.d("WDMF Connector", "Application side connector got a message");
            if (msg.arg1 != appID) {
                Log.w("WDMF Connector", "Received a message that was for appID " + String.valueOf(msg.arg1) + " but our app ID is " + String.valueOf(appID) + ". Going ahead anyway." );
            }
            switch (msg.what) {
                case WDMF_Connector.IPC_MSG_RECV_SINGLE_MESSAGE:
                    Bundle bnd = msg.getData();
                    byte[] data = bnd.getByteArray("data");
                    if( data != null) {
                        onReceiveMessage(data);
                    }
                    else {
                        Log.d("WDMF Connector", "Error: we got a messenger message of type IPC_MSG_RECV_SINGLE_MESSAGE but no valid application message was contained.");
                    }
                    break;
                case WDMF_Connector.IPC_MSG_RECV_MESSAGE_LIST:
                    bnd = msg.getData();
                    Serializable obj = bnd.getSerializable("dataList");
                    if( (obj instanceof ArrayList<?>)) {
                        ArrayList<byte[]> list = (ArrayList<byte[]>)obj;
                        // type check inner type of ArrayList
                        if(!list.isEmpty() && list.get(0) instanceof byte[]){
                            // send data to application
                            try {
                                // hand over as list if implemented by client
                                onReceiveMessageList(list);
                            }
                            catch (UnsupportedOperationException e) {
                                // hand over messages singly otherwise
                                for(byte[] appMsg : list){
                                    onReceiveMessage(appMsg);
                                }
                            }
                        }
                        else {
                            Log.d("WDMF Connector", "Error: we got a messenger message of type IPC_MSG_RECV_MESSAGE_LIST but the list was corrupted.");
                        }

                    }
                    else {
                        Log.d("WDMF Connector", "Error: we got a messenger message of type IPC_MSG_RECV_MESSAGE_LIST but no list of application messages was provided.");
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * The main service will have to bind to us in order to send us messages back
     */
    @Override
    public IBinder onBind(Intent intent) {
        return receivingMessenger.getBinder();
    }
}
