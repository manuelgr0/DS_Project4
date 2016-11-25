package ch.ethz.inf.vs.a4.wdmf_api;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.Arrays;

/**
 * Created by Jakob on 25.11.2016.
 *
 * This abstract class will be provided to all clients. They will need to implement
 * the listener for new messages and can then use the class to communicate to our service,
 * In that way, the IPC going on in the background is completely hidden to our API clients.
 *
 */

//TODO: Implement server receiving Messenger

public abstract class WDMF_Connector {
    // Application messages are byte arrays that will be sent to other applications through our API
    // Use this to send a single application message. Store it in the field with the name obj.
    public static final int IPC_MSG_SEND_SINGLE_MESSAGE = 1;
    // Use this to send multiple application messages at once.
    // Store them in a ArrayList and put it in the field with the name obj.
    public static final int IPC_MSG_SEND_MESSAGE_LIST = 2;

    // ABSTRACT PART

    public abstract void onReceiveMessage();


    // IMPLEMENTATION

    Messenger sendingMessenger = null;
    /** Flag indicating whether we have called bind on the service. */
    boolean bound;

    public WDMF_Connector(){

    }

    // Call in onStart of Activity
    // TODO: Discuess how this could be improved, can we gat an Activity in another way? Extending Activity seems to be a bad idea.
    public void connectToWDMF(Activity a) {
        // Bind to the service
        a.bindService(new Intent(a, MainService.class), mConnection,
                Context.BIND_AUTO_CREATE);
    }

    // Call in onStop of Activity
    public void disconnectFromWDMF() {
        // Unbind from the service
        if (bound) {
            a.unbindService(mConnection);
            bound = false;
        }
    }

    // Returns false in case something went wrong
    public boolean broadcastMessage(byte[] data) {
        if (!bound) {
            Log.d("WDMF_Connector", "Error: The WDMF is not bound but a message wants to be broadcastet.");
            // TODO: retry
            return false;
        }

        // Copy data
        byte[] dataCopy = Arrays.copyOfRange(data, 0, data.length);

        // Create and send a message to the service
        Message msg = Message.obtain(null, IPC_MSG_SEND_SINGLE_MESSAGE, dataCopy);

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
     * Class for interacting with the main interface of the service.
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
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            sendingMessenger = null;
            bound = false;
        }
    };

}
