package ch.ethz.inf.vs.a4.wdmf_api;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

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
    public static final int IPC_MSG_LISTEN = 5;
    // This is used to find where exactly the Service resides in the namespace
    private static final String packageName = "ch.ethz.inf.vs.a4.wdmf_api";
    private static final String serviceName = "ch.ethz.inf.vs.a4.wdmf_api.MainService";

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
    public boolean broadcastMessage(byte[] data) {
        if (!bound) {
            Log.d("WDMF_Connector", "Error: The WDMF is not bound but a message wants to be broadcasted.");
            // TODO: retry
            return false;
        }
        Log.d("WDMF_Connector", "broadcast message.");
        // Copy data
        byte[] dataCopy = Arrays.copyOfRange(data, 0, data.length);

        // Create and send a message to the service
        Message msg = Message.obtain(null, IPC_MSG_SEND_SINGLE_MESSAGE, appID, 0, dataCopy);

        try {
            sendingMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public String getNetworkTag(){
        // TODO: Implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void setNetworkTag(String networkTag){
        // TODO: Implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public long get_buffer_size(){
        // TODO: Implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void set_buffer_size(long buffersize){
        // TODO: Implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public double get_timeout() {
        // TODO: Implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void set_timeout(double timeout){
        // TODO: Implement
        throw new UnsupportedOperationException("Not implemented yet");
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
                    if( (msg.obj instanceof byte[])) {
                        onReceiveMessage((byte[])msg.obj);
                    }
                    else {
                        Log.d("WDMF Connector", "Error: we got a messenger message of type IPC_MSG_RECV_SINGLE_MESSAGE but no valid application message was contained.");
                    }
                    break;
                case WDMF_Connector.IPC_MSG_RECV_MESSAGE_LIST:
                    // type check ArraList
                    if( (msg.obj instanceof ArrayList<?>)) {
                        ArrayList<byte[]> list = (ArrayList<byte[]>)msg.obj;
                        // type check inner type of ArrayList
                        if(!list.isEmpty() && list.get(0) instanceof byte[]){

                            // send data to application

                            try {
                                // hand over as list if implmented by client
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
