package ch.ethz.inf.vs.a4.wdmf_api.service;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ch.ethz.inf.vs.a4.wdmf_api.ipc_interface.WDMF_Connector;
import ch.ethz.inf.vs.a4.wdmf_api.R;
import ch.ethz.inf.vs.a4.wdmf_api.local_data.MessageBuffer;
import ch.ethz.inf.vs.a4.wdmf_api.local_data.NeighbourList;
import ch.ethz.inf.vs.a4.wdmf_api.network_protocol_data.AckTable;
import ch.ethz.inf.vs.a4.wdmf_api.network_protocol_data.LCTable;

public class MainService extends Service {
    MessageBuffer buffer;
    LCTable lcTable;
    AckTable ackTable;
    NeighbourList neighbourhood;

    // Target we publish for clients to send messages to IncomingHandler
    private final Messenger receivingMessenger = new Messenger(new IncomingHandler(this));
    // To send the messages back according to the AppID
    SparseArray<Messenger> sendingMessengers = new SparseArray<>();

    // Configuration values
    long sleepTime = 5000; //5s TODO? Add to preferences?
    long maxNoContactTime = 60000; //1 min TODO? Add to preferences?
    long maxNoContactTimeForeignDevices = 60000; //1 min TODO? Add to preferences?
    String networkName = "MyNetworkName";
    long maxBufferSize = 1000000;
    int timeout = 30*60;
    boolean prefsLocked = false; //TODO add to prefs

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        updateFromPreferences();
        buffer = new MessageBuffer(networkName, 1000 * maxBufferSize);
        // TODO: get correct name in network (MAC address? Something unique that is easily visible for others)
        String myName = "Test Name " + new Date();
        lcTable = new LCTable(myName);
        ackTable = new AckTable(myName);
        neighbourhood = new NeighbourList();

        //starting main loop in a separate thread
        new Thread(new Runnable() {
            public void run() {
                main();
            }
        }).start();

        return START_STICKY;
    }

    // main loop
    void main(){
        long start;
        while(true){
            start = new Date().getTime();

            // TODO
            // get currently visible devices
            List<String> deviceIdentifier = new ArrayList<>();

            // Go through visible devices to contact them if necessary
            for (String neighbour : deviceIdentifier){
                // Check if the node is in the network
                if(lcTable.hasKey(neighbour)){
                    // See if we have new data for the node, according to our ACK table,
                    // or otherwise check when we've had contact with him last time
                    if(buffer.hasMessagesForReceiver(neighbour, ackTable)
                     || lcTable.entryIsOlderThan(neighbour, (new Date()).getTime() - maxNoContactTime)) {
                        establishConnection(neighbour);
                    }
                } else {
                    Date lastTry = neighbourhood.last_contact(neighbour);
                    Date threshold = new Date(System.currentTimeMillis() - maxNoContactTimeForeignDevices);
                    // Contact node if we haven't done so already recently
                    if (lastTry != null && lastTry.after( threshold )){
                        establishConnection(neighbour);
                        neighbourhood.update_neighbour(neighbour);
                    }
                }
            }

            // Sleep before starting the loop again
            long executionTime = new Date().getTime() - start;
            Log.d("Main Loop", "Execution time for one iteration: " + executionTime + "ms");
            try{
                Thread.sleep(sleepTime - executionTime);
            } catch (InterruptedException e){
                e.printStackTrace();
                break; // TODO: can we be interrupted by a friendly thread when we have new messages to send?
            }
        }
    }

    // Initiate connection to exchange tables and messages
    private void establishConnection(String neighbour){
        // TODO :)
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

    void localDataChanged(){
        // TODO: interrupt sleeping main loop
    }

}
