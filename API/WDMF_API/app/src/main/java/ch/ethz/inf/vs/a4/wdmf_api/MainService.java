package ch.ethz.inf.vs.a4.wdmf_api;

import android.app.Service;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;

public class MainService extends Service {
    private MessageBuffer buffer;
    // Target we publish for clients to send messages to IncomingHandler
    private final Messenger receivingMessenger = new Messenger(new IncomingHandler());
    // To send the messages back according to the AppID
    private SparseArray<Messenger> sendingMessengers = new SparseArray<>();

    // Configuration values
    String networkName = "MyNetworkName";
    long maxBufferSize = 1000000;
    int timeout = 30*60;
    boolean prefsLocked = false; //TODO add to prefs

    // Constructor
    //public MainService() {
    // NOTE: Don't think about using this, there are reasons why we have onStartCommand
    //       Here, the service is not initialised and reading its context or similar stuff may lead to NPEs
    //}


    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        updateFromPreferences();
        buffer = new MessageBuffer(networkName, 1000 * maxBufferSize);
        return START_STICKY;
    }

    private void updateFromPreferences(){

        //Read preferences
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        String lbufferSize = pref.getString(WDMF_Connector.bufferSizePK, (getResources().getString(R.string.pref_default_buffer_size)));
        String lnetworkName = pref.getString(WDMF_Connector.networkNamePK,(getResources().getString(R.string.pref_default_network_name)));
        String ltimeout = pref.getString(WDMF_Connector.timeoutPK, (getResources().getString(R.string.pref_default_timeout)));

        // Apply preferences
        updateBufferSize(Long.valueOf(lbufferSize));
        updateNetworkName(lnetworkName);
        updateTimeout(timeout = Integer.valueOf(ltimeout));
    }
// TODO: Listen to IPC changes to preferences and call these functions
    private void updateBufferSize(long newSize){
        Log.d("MainService", "New buffer size is applied: " + newSize + "KB old size was " + maxBufferSize + "KB.");
        if (buffer != null){
            long diff = maxBufferSize - newSize;
            if(diff < 0){
                buffer.decreaseBufferSize((int)-diff);
            } else if (diff > 0){
                buffer.increaseBufferSize(diff);
            }
        }
        maxBufferSize = newSize;
    }
    private void updateNetworkName(String newName){
        // TODO?
        networkName = newName;
    }
    private void updateTimeout(int newTimeout){
        // TODO?
        timeout = newTimeout;
    }
    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return receivingMessenger.getBinder();
    }

    private void localDataChanged(){
        //TODO Go and send to neighbours asap
    }

    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.d("Main Service", "Service got an IPC message");

            // Used in case an answer is requested and replyTo is provided
            Message answer = null;

            switch (msg.what) {
                case WDMF_Connector.IPC_MSG_SEND_SINGLE_MESSAGE:
                    Bundle bnd = msg.getData();
                    byte[] data = bnd.getByteArray("data");
                    if( data != null) {

                        // HACK: Wait for onCreate to finish
                        while(buffer == null);

                        buffer.addLocalMessage(data, msg.arg1);
                        localDataChanged();
                    }
                    else {
                        Log.d("MainService", "Error: we got a command to send out a message but no message was provided.");
                    }
                    break;

                case WDMF_Connector.IPC_MSG_SEND_MESSAGE_LIST:
                    // TODO
                    if( (msg.obj instanceof ArrayList<?>)) {
                        ArrayList<byte[]> list = (ArrayList<byte[]>)msg.obj;
                        if(!list.isEmpty() && list.get(0) instanceof byte[]){
                            for(byte[] appMsg : list){
                                buffer.addLocalMessage(appMsg, msg.arg1);
                            }
                            localDataChanged();
                        }
                        else {
                            Log.d("MainService", "Error: we got a command to send out a list of messages but the list was corrupted.");
                        }

                    }
                    else {
                        Log.d("MainService", "Error: we got a command to send out a list of messages but no list was provided.");
                    }
                    break;

                case WDMF_Connector.IPC_MSG_LISTEN:
                    // store (or possibly update) messenger to send the messages back
                    if(msg.replyTo != null){
                        sendingMessengers.put(msg.arg1, new Messenger(msg.replyTo.getBinder()) );
                    } else {
                        Log.d("MainService", "Error: Listener could not be registered because the replyTo field of the IPC Message was null. Message data: " + msg.toString());
                    }
                    break;

                case WDMF_Connector.IPC_MSG_SET_TAG:
                    boolean success = false;
                    if(!prefsLocked) {
                        Bundle bundle = msg.getData();
                        String nt = "";
                        if(bundle != null) {nt = bundle.getString(WDMF_Connector.networkNamePK, "");}
                        if ( nt.length() > 0) {
                            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MainService.this);
                            SharedPreferences.Editor editor = pref.edit();
                            editor.putString(WDMF_Connector.networkNamePK, (String) msg.obj);
                            success = editor.commit();
                        } else {
                            Log.d("MainService", "Got a command to set network tag but no argument was provided.");
                        }
                    } else {Log.d("MainService", "Got a command to set network tag but preferences have been locked by the user, it can't be changed over the API right now.");}
                    answer = Message.obtain(null, WDMF_Connector.IPC_MSG_SET_TAG_ACK, success ? 1 : 0, 0);
                    break;

                case WDMF_Connector.IPC_MSG_SET_BUFFER_SIZE:
                    success = false;
                    if(!prefsLocked) {
                        Bundle bundle = msg.getData();
                        long bs = bundle.getLong(WDMF_Connector.bufferSizePK, -1);
                        if (bs > 0) {
                            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MainService.this);
                            SharedPreferences.Editor editor = pref.edit();
                            editor.putString(WDMF_Connector.bufferSizePK, String.valueOf(bs));
                            success = editor.commit();
                        } else {
                            Log.d("MainService", "Got a command to set the buffer size but no argument was provided.");
                        }
                    } else {Log.d("MainService", "Got a command to set the buffer size but preferences have been locked by the user, it can't be changed over the API right now.");}
                    answer = Message.obtain(null, WDMF_Connector.IPC_MSG_SET_BUFFER_SIZE_ACK, success ? 1 : 0, 0);
                    break;
                case WDMF_Connector.IPC_MSG_SET_TIMEOUT:
                    success = false;
                    if(!prefsLocked) {
                        if (msg.arg1 > 0) {
                            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MainService.this);
                            SharedPreferences.Editor editor = pref.edit();
                            editor.putString(WDMF_Connector.timeoutPK, String.valueOf(msg.arg1));
                            success = editor.commit();
                        } else {
                            Log.d("MainService", "Got a command to set the timeout but no valid argument was provided.");
                        }
                    } else {Log.d("MainService", "Got a command to set the timeout but preferences have been locked by the user, it can't be changed over the API right now.");}
                    answer = Message.obtain(null, WDMF_Connector.IPC_MSG_SET_TIMEOUT_ACK, success ? 1 : 0, 0);
                    break;
                default:
                    super.handleMessage(msg);
            }

            // Send answer
            if(answer != null){
                if (msg.replyTo == null) {
                    Log.d("MainService", "Answer could not be sent because the receiver was missing in the replyTo field");
                }else {
                    try {
                        msg.replyTo.send(answer);
                    } catch (RemoteException e) {
                        Log.d("MainService", "Answer could not be sent because of a remote exception", e);
                    }
                }
            }
        }
    }
}
