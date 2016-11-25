package ch.ethz.inf.vs.a4.wdmf_api;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import java.util.ArrayList;

public class MainService extends Service {



    // TODO Make more message types for configurations

    private MessageBuffer buffer;
    // Target we publish for clients to send messages to IncomingHandler
    final Messenger receivingMessenger = new Messenger(new IncomingHandler());

    // Constructor
    public MainService() {
        // TODO Retrieve setting from preferences

        buffer = new MessageBuffer("MyNetworkName", 1000000);
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
            switch (msg.what) {
                case IPC_MSG_SEND_SINGLE_MESSAGE:
                    if( (msg.obj instanceof byte[])) {
                        buffer.addLocalMessage((byte[])msg.obj);
                        localDataChanged();
                    }
                    else {
                        Log.d("MainService", "Error: we got a command to send out a message but no message was provided.");
                    }
                    break;
                case IPC_MSG_SEND_MESSAGE_LIST:
                    if( (msg.obj instanceof ArrayList<?>)) {
                        ArrayList<byte[]> list = (ArrayList<byte[]>)msg.obj;
                        if(!list.isEmpty() && list.get(0) instanceof byte[]){
                            for(byte[] appMsg : list){
                                buffer.addLocalMessage(appMsg);
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
                default:
                    super.handleMessage(msg);
            }
        }
    }


}
