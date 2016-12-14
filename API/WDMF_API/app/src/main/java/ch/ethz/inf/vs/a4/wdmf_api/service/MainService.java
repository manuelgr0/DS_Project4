package ch.ethz.inf.vs.a4.wdmf_api.service;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ch.ethz.inf.vs.a4.wdmf_api.io.Packet;
import ch.ethz.inf.vs.a4.wdmf_api.ipc_interface.WDMF_Connector;
import ch.ethz.inf.vs.a4.wdmf_api.R;
import ch.ethz.inf.vs.a4.wdmf_api.local_data.EnumeratedMessage;
import ch.ethz.inf.vs.a4.wdmf_api.local_data.MessageBuffer;
import ch.ethz.inf.vs.a4.wdmf_api.local_data.NeighbourList;
import ch.ethz.inf.vs.a4.wdmf_api.network_protocol_data.AckTable;
import ch.ethz.inf.vs.a4.wdmf_api.network_protocol_data.LCTable;

public class MainService extends Service {
    MessageBuffer buffer;
    LCTable lcTable;
    AckTable ackTable;
    NeighbourList neighbourhood;

    volatile private boolean dataChanged = false;

    IncomingHandler handler = new IncomingHandler(this);
    // Target we publish for clients to send messages to IncomingHandler
    private final Messenger receivingMessenger = new Messenger(handler);
    // To send the messages back according to the AppID
    SparseArray<Messenger> sendingMessengers = new SparseArray<>();

    // Separate thread that connects to other devices and exchanges data
    Thread mainLoop;

    // Configuration values
    long sleepTime = 5000; //5s
    long maxNoContactTime = 60000; //1 min
    long maxNoContactTimeForeignDevices = 60000; //1 min
    String networkName = "MyNetworkName";
    String userID = "Name noe initialized";
    long maxBufferSize = 1000000;
    private volatile int timeout = 30*60; // TODO: use
    boolean prefsLocked = false;

    @Override
    public void onCreate(){
        // TODO: get correct name in network (MAC address? Something unique that is easily visible for others)

        userID = "Test Name " + new Date();
        updateFromPreferences();
        buffer = new MessageBuffer(networkName, 1000 * maxBufferSize);
        lcTable = new LCTable(userID);
        ackTable = new AckTable(userID);
        neighbourhood = new NeighbourList();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        if (intent.hasExtra("stop")){
            if(mainLoop != null && mainLoop.isAlive()) {
                mainLoop.interrupt();
            }
            else{
                Log.d("MainService", "There is nothing to stop right now.");
            }
        }
        else if (intent.hasExtra("updatePrefs")){
            updateFromPreferences();
        }
        else {
            updateFromPreferences();

            // make sure old threads are dead
            if (mainLoop != null && mainLoop.isAlive()) {
                mainLoop.interrupt();
            }

            //creating main loop thread
            mainLoop = new Thread() {
                public void run() {
                    main();
                }
            };
            // start separate thread for the main loop
            mainLoop.start();
        }
        return START_STICKY;

    }

    @Override
    public void onDestroy(){
        this.stop();
        super.onDestroy();
    }

    public void stop(){
        if(mainLoop != null) {
            mainLoop.interrupt();
        }
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
                // lock buffer because IPC messages will be handled by another thread
                synchronized (buffer) {
                    // Check if the node is in the network
                    if (lcTable.hasKey(neighbour)) {
                        // See if we have new data for the node, according to our ACK table,
                        // or otherwise check when we've had contact with him last time
                        if (buffer.hasMessagesForReceiver(neighbour, ackTable)
                                || lcTable.entryIsOlderThan(neighbour, (new Date()).getTime() - maxNoContactTime)) {
                            syncPeerToPeer(neighbour);
                        }
                    } else {
                        Date lastTry = neighbourhood.last_contact(neighbour);
                        Date threshold = new Date(System.currentTimeMillis() - maxNoContactTimeForeignDevices);
                        // Contact node if we haven't done so already recently
                        if (lastTry != null && lastTry.after(threshold)) {
                            syncPeerToPeer(neighbour);
                            neighbourhood.update_neighbour(neighbour);
                        }
                    }
                }
            }

            // Sleep before starting the loop again
            long executionTime = new Date().getTime() - start;
            Log.d("Main Loop", "Execution time for one iteration: " + executionTime + "ms");
            try{
                synchronized (buffer) {
                    // Spin in case we are woken up for wrong reasons
                    while(!dataChanged && (new Date()).getTime() < start + sleepTime){
                        buffer.wait(sleepTime - executionTime);
                    }
                    dataChanged = false;
                }
            } catch (InterruptedException e){
                Log.d("Main Service", "Stopping main Loop");
                return;
            }
        }
    }

    // Initiate connection to exchange tables and messages
    private void syncPeerToPeer(String neighbour){
        // TODO: wifiSend and wifiBlockingReceive:)
        // TODO: reorder messages and buffer them
        // TODO: detect if other nodes think we got message already (msg has been dropped)
        //       Note that the las point is quite important for the case where a node goes offline
        //       for some time and joins the network again, but also just for joining a network late,
        //       if you don't do that it will wait for seq 0 forever...


        /*   I assume the following behaviour of the Wifi IO backend:
        *
        *   void wifiSend(String node); Sends data non-blocking to the node
        *
        *   boolean wifiBlockingReceive(String node); blocks until the full data from the other node
        *    has arrived. If the connection fails, or when a timeout occurs, it will return false.
        *
        *   Of course this could be implemented differently, this is just an idea! But fot my
        *   code to work it should probably be similar to that idea, at least. But I don't know
        *   how it will look like, maybe we also have to call something like connect(neighbour)
        *   first and close() to have a persistent connection, I guess we don't want to build up
        *   the entire connection each time.
        */
        String TAG = ("ConEstbl " + neighbour).substring(23);


        // 1. Try to merge network
        //  Hidden inside of exchangeTables:
        //  1.1 Send my LC-/ACK-table and wait for answer
        //  1.2 Check if network tags match
        //  1.3. Merge LC-/Ack-table
        Log.d(TAG, "Merge network");
        if(!exchangeTables(neighbour)){
            connectionFailed(neighbour);
            Log.d(TAG, "Network merge failed");
            return;
        }

        Log.d(TAG, "Network merge successful. Exchange buffer data next.");

        // 2. Send data from buffer
        Packet myData = new Packet(
                buffer.getMessagesForReceiver(
                        neighbour,
                        ackTable
                )
        );
//UNCOMMENT when done        wifiSend(neighbour, myData.getRawData());

        // 3. Receive data, unpack it, store it and deliver it to the clients
        byte[] answerRawData = new byte[0];
//UNCOMMENT when done        byte[] answerRawData = wifiBlockingReceive(neighbour); // might fail => return in that case
        Packet theirData = new Packet(answerRawData);
        Log.d(TAG, "Buffer data exchange successful, applying data now.");
        for (byte[] message : theirData.getMessageContents()) {
            // unpack
            EnumeratedMessage emsg = new EnumeratedMessage(message);

            // store
            buffer.addRemoteMessage(neighbour, emsg.seq, emsg.msg, emsg.appId);

            // deliver to an app if one is listed for the given AppId
            Messenger client = sendingMessengers.get(emsg.appId);
            if (client != null) {
                // Note: This is an Android IPC Message, nothing to do with our custom messages that we flood
                Message delivery =  Message.obtain(null, WDMF_Connector.IPC_MSG_SEND_SINGLE_MESSAGE, emsg.appId, 0);
                Bundle b = new Bundle();
                b.putByteArray("data", emsg.msg);
                delivery.setData(b);
                try {
                    client.send(delivery);
                    // update ACK-table after successful delivery
                    ackTable.update(emsg.sender, emsg.seq);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        // 4. Update Ack-table according to what has been sent to the neighbour
        //   We can just exchange the acktables again here, in that way we even
        //   catch all errors that happened on the way, for instance RemoteExceptions in the IPC
        Log.d(TAG, "Finished applying data, exchange ACK-table again to finish the connection.");
        exchangeTables(neighbour);

    }

    // merges tables with specified neighbour
    // returns true iff network tags matched and merge was successful
    private boolean exchangeTables(String neighbour){
        String TAG = ("exchangeTables " + neighbour).substring(23);
        //  Send my LC-/ACK-table and wait for answer
        Packet myTables = new Packet(networkName, lcTable, ackTable);
//UNCOMMENT when done        wifiSend(neighbour, myTables.getRawData());

        byte[] answer = new byte[0];
//UNCOMMENT when done        byte[] answer = wifiBlockingReceive(neighbour); //return false on timeout
        Packet theirTables =  new Packet(answer);

        //  Check if network tags match
        if (!theirTables.getNetworkID().equals(networkName)){
            Log.d(TAG, "networks don't match");
            return false;
        }
        //  Merge LC-/Ack-table
        lcTable.merge(neighbour, theirTables.getLCTable());
        ackTable.merge(theirTables.getAckTable());
        return true;
    }

    // Adds the neigbour to the list of "foreign devices" <=> neighbour type 2
    // In that way we will retry after some time, but we will not try each iterations
    // even if a device stopped the app using our API.
    private void connectionFailed(String neighbour){
        neighbourhood.update_neighbour(neighbour);
    }

    public void updateFromPreferences(){

        //Read preferences
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        String lbufferSize = pref.getString(WDMF_Connector.bufferSizePK, (getResources().getString(R.string.pref_default_buffer_size)));
        String lnetworkName = pref.getString(WDMF_Connector.networkNamePK,(getResources().getString(R.string.pref_default_network_name)));
        String ltimeout = pref.getString(WDMF_Connector.timeoutPK, (getResources().getString(R.string.pref_default_timeout)));
        String lsleepTime = pref.getString(WDMF_Connector.sleepTimePK, "5");
        String lmaxNoContact = pref.getString(WDMF_Connector.maxNoContactTimePK, "60");
        String lmncForeign = pref.getString(WDMF_Connector.maxNoContactTimeForeignDevicesPK, "60");

        prefsLocked = pref.getBoolean(WDMF_Connector.lockPreferencesPK, true);

        // Apply preferences
        updateBufferSize(Long.valueOf(lbufferSize));
        updateNetworkName(lnetworkName);
        updateTimeout(Integer.valueOf(ltimeout));
        sleepTime = Integer.valueOf(lsleepTime) * 1000;
        maxNoContactTime = Integer.valueOf(lmaxNoContact) * 1000;
        maxNoContactTimeForeignDevices = Integer.valueOf(lmncForeign) * 1000;
    }

    public void updateBufferSize(long newSize){
        Log.d("MainService", "New buffer size is applied: " + newSize + "KB old size was " + maxBufferSize + "KB.");
        if (buffer != null) {
            synchronized (buffer) {
                long diff = maxBufferSize - newSize;
                if (diff < 0) {
                    buffer.decreaseBufferSize((int) -diff);
                } else if (diff > 0) {
                    buffer.increaseBufferSize(diff);
                }
            }
        }
        maxBufferSize = newSize;
    }
    // Changing the network name results in a change of the network and
    // thus in a full reset of the local buffer.
    public void updateNetworkName(String newName){
        if (!newName.equals(networkName)) {
            networkName = newName;
            if(buffer != null) {
                synchronized (buffer) {
                    buffer = new MessageBuffer(networkName, maxBufferSize);
                    lcTable = new LCTable(networkName);
                    ackTable = new AckTable(networkName);
                }
            }
        }
    }
    public void updateTimeout(int newTimeout){
        timeout = newTimeout;
    }

    /**
     * When binding to the service from a connector,
     * we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return receivingMessenger.getBinder();
    }

    void localDataChanged(){
        dataChanged = true;
        // notify mainLoop thread
        synchronized (buffer) {
            buffer.notify();
        }
    }

    /**
     * Class for direct access from our own Activitiy, not from the connector.
     * Because here we know this service always runs in the same process as the
     * single client connecting in this way, we don't need to deal with IPC here.
     *
     * We need this so we can call stop() from the MainActivity.
     */
    //public class LocalBinder extends Binder {
    //   public MainService getService() {
    //        return MainService.this;
    //    }
    //}

}
