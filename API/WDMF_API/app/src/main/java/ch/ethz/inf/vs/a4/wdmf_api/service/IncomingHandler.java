package ch.ethz.inf.vs.a4.wdmf_api.service;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

import ch.ethz.inf.vs.a4.wdmf_api.ipc_interface.WDMF_Connector;

/**
 * Handler of incoming messages from clients.
 */
public class IncomingHandler extends Handler {
    private MainService mainService;

    public IncomingHandler(MainService mainService) {
        this.mainService = mainService;
    }

    public MainService getService(){
        return mainService;
    }

    @Override
    public void handleMessage(Message msg) {
        Log.d("Incoming Handler", "Service got an IPC message");

        // Used in case an answer is requested and replyTo is provided
        Message answer = null;

        switch (msg.what) {
            case WDMF_Connector.IPC_MSG_SEND_SINGLE_MESSAGE:
                Bundle bnd = msg.getData();
                byte[] data = bnd.getByteArray("data");
                if( data != null) {

                    // HACK: Wait for onCreate to finish
                    long timeout = new Date().getTime() + 3000; //3s timeout
                    while(mainService.buffer == null){
                        if(timeout < new Date().getTime()){
                            Log.e("Incoming Handler", "Timeout while waiting for the local message buffer to be initialised.");
                            return;
                        }
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {e.printStackTrace();}
                    }

                    synchronized (mainService.buffer){
                        mainService.buffer.addLocalMessage(data, msg.arg1);
                    }
                    mainService.localDataChanged();
                }
                else {
                    Log.d("Incoming Handler", "Error: we got a command to send out a message but no message was provided.");
                }
                break;

            case WDMF_Connector.IPC_MSG_SEND_MESSAGE_LIST:
                bnd = msg.getData();
                Serializable obj = bnd.getSerializable("dataList");
                if( (obj instanceof ArrayList<?>)) {
                    ArrayList<byte[]> list = (ArrayList<byte[]>)obj;
                    if(!list.isEmpty() && list.get(0) instanceof byte[]){
                        for(byte[] appMsg : list){
                            mainService.buffer.addLocalMessage(appMsg, msg.arg1);
                        }
                        mainService.localDataChanged();
                    }
                    else {
                        Log.d("Incoming Handler", "Error: we got a command to send out a list of messages but the list was corrupted.");
                    }
                }
                else {
                    Log.d("Incoming Handler", "Error: we got a command to send out a list of messages but no list was provided.");
                }
                break;

            case WDMF_Connector.IPC_MSG_LISTEN:
                // store (or possibly update) messenger to send the messages back
                if(msg.replyTo != null){
                    mainService.sendingMessengers.put(msg.arg1, new Messenger(msg.replyTo.getBinder()) );
                } else {
                    Log.d("Incoming Handler", "Error: Listener could not be registered because the replyTo field of the IPC Message was null. Message data: " + msg.toString());
                }
                break;

            case WDMF_Connector.IPC_MSG_SET_TAG:
                boolean success = false;
                if(!mainService.prefsLocked) {
                    Bundle bundle = msg.getData();
                    String nt = "";
                    if(bundle != null) {nt = bundle.getString(WDMF_Connector.networkNamePK, "");}
                    if ( nt.length() > 0) {
                        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mainService);
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putString(WDMF_Connector.networkNamePK, (String) msg.obj);
                        success = editor.commit();
                        mainService.updateNetworkName(nt);
                    } else {
                        Log.d("Incoming Handler", "Got a command to set network tag but no argument was provided.");
                    }
                } else {Log.d("Incoming Handler", "Got a command to set network tag but preferences have been locked by the user, it can't be changed over the API right now.");}
                answer = Message.obtain(null, WDMF_Connector.IPC_MSG_SET_TAG_ACK, success ? 1 : 0, 0);
                break;

            case WDMF_Connector.IPC_MSG_SET_BUFFER_SIZE:
                success = false;
                if(!mainService.prefsLocked) {
                    Bundle bundle = msg.getData();
                    long bs = bundle.getLong(WDMF_Connector.bufferSizePK, -1);
                    if (bs > 0) {
                        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mainService);
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putString(WDMF_Connector.bufferSizePK, String.valueOf(bs));
                        success = editor.commit();
                        mainService.updateBufferSize(bs);
                    } else {
                        Log.d("Incoming Handler", "Got a command to set the buffer size but no argument was provided.");
                    }
                } else {Log.d("Incoming Handler", "Got a command to set the buffer size but preferences have been locked by the user, it can't be changed over the API right now.");}
                answer = Message.obtain(null, WDMF_Connector.IPC_MSG_SET_BUFFER_SIZE_ACK, success ? 1 : 0, 0);
                break;
            case WDMF_Connector.IPC_MSG_SET_TIMEOUT:
                success = false;
                if(!mainService.prefsLocked) {
                    if (msg.arg1 > 0) {
                        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mainService);
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putString(WDMF_Connector.timeoutPK, String.valueOf(msg.arg1));
                        success = editor.commit();
                        mainService.updateTimeout(msg.arg1);
                    } else {
                        Log.d("Incoming Handler", "Got a command to set the timeout but no valid argument was provided.");
                    }
                } else {Log.d("Incoming Handler", "Got a command to set the timeout but preferences have been locked by the user, it can't be changed over the API right now.");}
                answer = Message.obtain(null, WDMF_Connector.IPC_MSG_SET_TIMEOUT_ACK, success ? 1 : 0, 0);
                break;
            default:
                super.handleMessage(msg);
        }

        // Send answer
        if(answer != null){
            if (msg.replyTo == null) {
                Log.d("Incoming Handler", "Answer could not be sent because the receiver was missing in the replyTo field");
            }else {
                try {
                    msg.replyTo.send(answer);
                } catch (RemoteException e) {
                    Log.d("Incoming Handler", "Answer could not be sent because of a remote exception", e);
                }
            }
        }
    }
    public String toString(){
        return super.toString() + "   -   My Incoming Handler" ;
    }
}
