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
import ch.ethz.inf.vs.a4.wdmf_api.io.WifiBackend;
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
    String userID = "Name not initialized";
    long maxBufferSize = 1000000;
    public static  long WIFI_RECEIVE_TIMEOUT = 2000;
    public static volatile int timeout = 30*60*1000;
    boolean prefsLocked = false;
    static int MAX_REORDERING = 8;

    @Override
    public void onCreate(){
        // TODO: get correct name in network (MAC address? Something unique that is easily visible for others)

        userID = "Test Name " + new Date();
        updateFromPreferences();
        lcTable = new LCTable(userID);
        ackTable = new AckTable(userID);
        buffer = new MessageBuffer(networkName, 1000 * maxBufferSize, ackTable, lcTable);
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

            // get currently visible devices
            List<String> deviceIdentifier = WifiBackend.getNeighbourhood();

            // Go through visible devices to contact them if necessary
            if(deviceIdentifier != null) {
                for (String neighbour : deviceIdentifier) {
                    // lock buffer because IPC messages will be handled by another thread
                    synchronized (buffer) {
                        // First check if someone is already trying to connect to us
                        while (WifiBackend.incommingConnectionRequestWaiting()) {
                            syncPeerToPeer();
                        }

                        // Check if the node is in the network
                        if (lcTable.hasKey(neighbour)) {
                            // See if we have new data for the node, according to our ACK table,
                            // or otherwise check when we've had contact with him last time
                            if (buffer.hasMessagesForReceiver(neighbour)
                                    || lcTable.entryIsOlderThan(neighbour, (new Date()).getTime() - maxNoContactTime)) {
                                try {
                                    WifiBackend.connectTo(neighbour);
                                    syncPeerToPeer();
                                } catch (Exception e) {
                                    // try next neighbour
                                    continue;
                                }
                            }
                        } else {
                            Date lastTry = neighbourhood.last_contact(neighbour);
                            Date threshold = new Date(System.currentTimeMillis() - maxNoContactTimeForeignDevices);
                            // Contact node if we haven't done so already recently
                            if (lastTry != null && lastTry.after(threshold)) {
                                try {
                                    WifiBackend.connectTo(neighbour);
                                    syncPeerToPeer();
                                } catch (Exception e) {
                                    // try next neighbour
                                    continue;
                                }
                                neighbourhood.update_neighbour(neighbour);
                            }
                        }
                    }
                }
            }

            //remove old nodes from network
            for(String node : lcTable.getNodeSet()){
                if(lcTable.entryIsOlderThan(node, timeout)){
                    ackTable.removeNode(node);
                    lcTable.removeNode(node);
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
    private void syncPeerToPeer(){
        String neighbour = WifiBackend.getOtherMacAddress();
        String TAG = ("ConEstbl " + neighbour).substring(23);


        // 1. Try to merge network
        //  Hidden inside of exchangeTables:
        //  1.1 Send my LC-/ACK-table and wait for answer
        //  1.2 Check if network tags match
        //  1.3. Merge LC-/Ack-table
        Log.d(TAG, "Merge network");
        if(!exchangeTables()){
            Log.d(TAG, "Network merge failed");
            WifiBackend.close();
            return;
        }

        Log.d(TAG, "Network merge successful. Exchange buffer data next.");

        // 2. Send data from buffer
        Packet myData = new Packet(
                buffer.getEnumeratedMessagesForReceiver(neighbour)
        );

        try {
            WifiBackend.send(myData.getRawData());
        } catch (Exception e){
            // Problem with wifi backend, abort for now and then try again in the next iteration
            WifiBackend.close();
            Log.d(TAG, "Sending my data to " + neighbour + " failed. :(");
        }

        // 3. Receive data, unpack it and store it
        byte[] answerRawData = WifiBackend.waitForReceive(neighbour);
        // might fail => return in that case and try again in the next iteration
        if(answerRawData == null) {
            WifiBackend.close();
            return;
        }
        Packet theirData = new Packet(answerRawData);
        Log.d(TAG, "Buffer data exchange successful, applying data now.");
        for (byte[] message : theirData.getMessageContents()) {
            // unpack and store hidden in MessageBuffer class
            buffer.addRawEnumeratedMessage(message);
        }
        // 4. Delivery

        // If the client-message is one we have been waiting for (EXACT sequence number),
        // only then we want to deliver it to the client apps and update the ACK-table.
        // Otherwise we have to wait with the delivery to ensure the correct order of messages
        // and the ACK-table can only store "everything before N has been received" anyway.
        // On the other hand, if we can deliver one client-message, we should also check the
        // local buffer for successor client-messages.

        buffer.bufferedButNotDelivered = 0; // This is done here so we can count the sum of all appIDs
        for(int i = 0; i < sendingMessengers.size(); i++)
        {
            int appId = sendingMessengers.keyAt(i);
            Messenger client = sendingMessengers.get(appId);

            // buffer.getMessagesReadyForDelivery does all the hard work for us to:
            // - find the messages that can be delivered now
            // - order them correctly
            // - compute the new sequence numbers and directly update them in the ACK-table
            // - count the number of buffered message we can't deliver yet
            ArrayList<byte[]> deliveryData = buffer.getMessagesReadyForDelivery(appId);

            if (deliveryData.size() > 0) {
                // Note: This is an Android IPC Message, nothing to do with our custom messages that we flood
                Message delivery = Message.obtain(null, WDMF_Connector.IPC_MSG_SEND_MESSAGE_LIST, appId, 0);
                Bundle b = new Bundle();
                b.putSerializable("dataList", deliveryData);
                delivery.setData(b);
                try {
                    client.send(delivery);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        // 5. Update Ack-table with neighbour
        //   We have to exchange the ACK-tables again here, in that way we even
        //   catch all errors that happened on the way and all cascaded ACKs
        //   from locally buffered messages with higher sequence number.

        Log.d(TAG, "Finished applying data, exchange ACK-table again to finish the connection.");

        // This migh return false, but if we fail here, we don't really care and proceed just in the same way
        exchangeTables();
        WifiBackend.close();
    }

    // merges tables with specified neighbour
    // returns true iff network tags matched and merge was successful
    private boolean exchangeTables(){
        String neighbour = WifiBackend.getOtherMacAddress();
        String TAG = ("exchangeTables " + neighbour).substring(23);
        //  Send my LC-/ACK-table and wait for answer
        Packet myTables = new Packet(networkName, lcTable, ackTable);
        try {
            WifiBackend.send(myTables.getRawData());
        } catch (Exception e){
            Log.d(TAG, "Sending tables failed :(");
            return false;
        }

        byte[] answer = WifiBackend.waitForReceive(neighbour);
        if(answer == null){ return false; }

        //return false on timeout
        if(answer == null){ return false; }

        Packet theirTables =  new Packet(answer);

        //  Check if network tags match
        if (!theirTables.getNetworkID().equals(networkName)){
            Log.d(TAG, "networks don't match");
            // make sure we don't try to connect again too soon
            addForeignDevice(neighbour);
            return false;
        }

        // Detect lost messages (in case everyone dropped it from the buffer)
        // This is crucial for the case where a node goes offline for some time and
        // joins the network again. As soon as we have more than XXX number of stored messages
        // that we could deliver, but we have to wait for the ordering, we should skip
        // some seq_numbers

        if(buffer.bufferedButNotDelivered > MAX_REORDERING) {
            buffer.skipSomeSeqNrs();
        }

        //  Merge LC-/Ack-table
        lcTable.merge(neighbour, theirTables.getLCTable());
        ackTable.merge(theirTables.getAckTable());

        // Remove unused messages from local buffer
        buffer.removeMessagesWhichReachedEveryone();

        return true;
    }

    // Adds the neighbour to the list of "foreign devices" <=> neighbour type 2
    // In that way we will retry after some time, but we will not try each iterations
    // even if a device stopped the app using our API.
    private void addForeignDevice(String neighbour){
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
                    lcTable = new LCTable(networkName);
                    ackTable = new AckTable(networkName);
                    buffer = new MessageBuffer(networkName, maxBufferSize, ackTable, lcTable);
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

}
