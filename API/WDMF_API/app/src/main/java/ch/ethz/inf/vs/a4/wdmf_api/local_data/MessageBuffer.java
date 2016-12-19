package ch.ethz.inf.vs.a4.wdmf_api.local_data;


import android.util.Log;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.PriorityQueue;
import java.util.concurrent.ThreadLocalRandom;

import ch.ethz.inf.vs.a4.wdmf_api.network_protocol_data.AckTable;
import ch.ethz.inf.vs.a4.wdmf_api.network_protocol_data.LCTable;
import ch.ethz.inf.vs.a4.wdmf_api.service.IncomingHandler;
import ch.ethz.inf.vs.a4.wdmf_api.service.MainService;

/**
 * Created by Jakob on 24.11.2016.
 */

// TODO: Split up large messages (if required by backend)

final public class MessageBuffer {
    // In the ACK-table we store the nr we have received already, so we start with init_seq_nr + 1
    private int seq_nr = AckTable.INIT_SEQ_NR + 1;
    private ArrayList<EnumeratedMessage> buffer;
    private String owner;
    private long memory_space;
    public int bufferedButNotDelivered = 0;
    // local reference to ACK-Table so we can make more sophisticated replacement policies
    private AckTable ackTable;
    private LCTable lcTable;

    // Provide the name in the network associated with the local node
    // and specify the max bytes that should be buffered
    public MessageBuffer(String ownerIdentifier, long bufferSize, AckTable at, LCTable lc) {
        owner = ownerIdentifier;
        buffer = new ArrayList<EnumeratedMessage>();
        memory_space = bufferSize;
        ackTable = at;
        lcTable = lc;
        if(memory_space <= 0 ||at == null || lc == null) {
            throw new IllegalArgumentException("bufferSize must be positive and tables initialized");
        }
    }

    // For messages with a local origin, we don't know the seq_nr yet
    public void addLocalMessage(byte[] msg, int appID){
        // Disable for JUnit test
         Log.d("Message Buffer", "Message buffer stores data:\n" + Arrays.toString(msg));

        EnumeratedMessage em = new EnumeratedMessage(msg, seq_nr, owner, appID);
        insertEnumeratedMessage(em);
        seq_nr++;
    }

    // For messages which were sent from another node
    // and therefore we should know the seq_nr already
    public void addRemoteMessage(String sender, int seq_number, byte[] msg, int appID){
        EnumeratedMessage em = new EnumeratedMessage(msg, seq_number, sender, appID);
        insertEnumeratedMessage(em);
    }

    // For messages that are already parsed to enumerated messages
    // and stored as byte array
    public void addRawEnumeratedMessage(byte[] rawMsg){
        Log.d("Message Buffer", "HURRA!!! Message buffer stores remote data");
        EnumeratedMessage em = new EnumeratedMessage(rawMsg);
        insertEnumeratedMessage(em);
    }

    // Insert to buffer
    public void insertEnumeratedMessage(EnumeratedMessage em){
        // NOTE: Expensive insert! Think about using a better data structure for the buffer...
        // Check for duplicates
        for(EnumeratedMessage stored : buffer){
            if (stored.sender.equals(em.sender) && stored.seq == em.seq) {
                return;
            }
        }

        if(memory_space < em.size()) {
            makeSpace(em.size());
        }
        buffer.add(em);
        memory_space -= em.size();
    }

    // Call this function when you no longer need a message in the buffer
    // The first occurrence of such a message will be removed from the buffer
    // Returns true when the message was found and deleted
    // Returns false when the message was not in the buffer
    public boolean removeMessage(String sender, int seq_number){
        for(int i = 0; i < buffer.size(); i++){
            if(buffer.get(i).sender.equals(sender) && buffer.get(i).seq == seq_number){
                memory_space += buffer.get(i).size();
                buffer.remove(i);
                return true;
            }
        }
        return false;
    }

    // Creates a list of raw data arrays containing all the enumerated messages
    // that has not reached the specified receiver yet
    public ArrayList<byte[]> getEnumeratedMessagesForReceiver(String receiver){
        ArrayList<byte[]> result = new ArrayList<>();
        for(EnumeratedMessage em : buffer) {
            if(ackTable.get(em.sender, receiver) <  seq_nr){
                result.add(em.raw());
            }
        }
        return result;
    }

    public void increaseBufferSize(long n){
        memory_space += n;
    }

    public void decreaseBufferSize(int n){
        makeSpace(n);
    }

    // Kick out messages until we have at least X bytes free memory
    // Throws OutOfMemoryError if the entire buffer is smaller than the required bytes
    private void makeSpace(int bytes){
        // First remove what we don't need for sure
        removeMessagesWhichReachedEveryone();

        if(bytes <= memory_space) {
            return;
        }

        ArrayList<Integer> removeOrder = randomizedImportanceOrderOfMessageIndexes();
        ArrayList<Integer> removed = new ArrayList<>();

        // Remove messages from buffer until we have enough space
        while(bytes > memory_space && !buffer.isEmpty()){

            int i = removeOrder.remove(0);
            removed.add(i);

            // Adjust index:
            for(int j : removed){
                if(j < i) { i--; }
            }

            memory_space += buffer.get(i).size();
            buffer.remove(i);

        }
        if (memory_space < bytes) {
            throw new OutOfMemoryError();
        }
    }

    // REPLACEMENT POLICIES //////////////////////////////////////////////////////////////
    // Should also be called whenever the ACK-table is updated
    public void removeMessagesWhichReachedEveryone(){
        for(int i = buffer.size() - 1; i >= 0; i--) {
            if(ackTable.reachedAll(buffer.get(i).sender, buffer.get(i).seq)){
                memory_space += buffer.get(i).size();
                buffer.remove(i);
            }
        }
    }

    // FIFO
    private int oldestMessageIndex(){
        return 0;
    }
    // Prioritize message which many nodes have gotten already to be removed, but choose randomly
    // so that not everyone removes the same messages. Also give older nodes a higher probability
    // to be removed
    // Only remove messages which are for the owner of the buffer after delivery or if it there are
    // no other messages left.
    private ArrayList<Integer> randomizedImportanceOrderOfMessageIndexes(){
        PriorityQueue<PriorityTuple> priorQ = new PriorityQueue<>(buffer.size());
        int networkSize = ackTable.width();
        for(int i = 0; i < buffer.size(); i++){
            PriorityTuple pi = new PriorityTuple(i);

            // each nodes that has not been reached yet is making the node more important
            // the longer we haven't seen the node, the smaller that importance change is
            // [0,1]
            float change =ackTable.notReached(buffer.get(i).sender, buffer.get(i).seq);
            change /= (float)networkSize; // mapped to [0,1]
            long timestamp = lcTable.getLastContactTimestamp(buffer.get(i).sender);
            long time =  timestamp - new Date().getTime();
            change *= 1.0f - ((float) time / (float)MainService.timeout);
            pi.priority += change;

            // new messages are more important
            // [0, 0.5]
            pi.priority += 0.5 * (float) i / (float)buffer.size() ;

            // add a random number to achieve asymmetric behaviour
            // [0, 0.5]
            pi.priority += 0.5 * ThreadLocalRandom.current().nextFloat();

            // most important are all messages that we have not delivered to our clients, yet
            if(ackTable.get(buffer.get(i).sender, owner) < buffer.get(i).seq){
                pi.priority += 100;
            }
            priorQ.add(pi);
        }
        ArrayList<Integer> result = new ArrayList<>(buffer.size());
        while(!priorQ.isEmpty()){
            result.add(priorQ.poll().index);
        }
        return result;
    }

    // ATTENTION: Side effect on ACK-Table
    public void skipSomeSeqNrs() {
        // Uses compare defined in EnumeratedMessage.java
        PriorityQueue<EnumeratedMessage> forApp = new PriorityQueue<>();
        for(EnumeratedMessage em : buffer) {
            if(ackTable.get(em.sender, ackTable.getOwner()) <  em.seq){
                forApp.add(em);
            }
        }

        Hashtable<String, Integer> newSeqNrs = new Hashtable<>();
        while(!forApp.isEmpty()){
            // Extract message with smallest sequence number
            EnumeratedMessage em = forApp.poll();
            if(!newSeqNrs.containsKey(em.sender)){
                // must be the smallest seq number present greater than the owners seq number
                newSeqNrs.put(em.sender, em.seq-1);
            }
        }

        for( String sender : newSeqNrs.keySet()) {
            ackTable.update(sender, newSeqNrs.get(sender));
        }
    }

    private class PriorityTuple implements Comparable<PriorityTuple> {
        public float priority = 0.0f;
        public int index;
        public PriorityTuple(int i){
            index = i;
        }
        @Override
        public int compareTo(PriorityTuple o) {
            if (this.priority < o.priority) return -1;
            if (this.priority > o.priority) return 1;
            return 0;
        }
    }

    // END REPLACEMENT POLICIES //////////////////////////////////////////////////////////


    public boolean hasMessagesForReceiver(String receiver) {
        for(EnumeratedMessage em : buffer) {
            if(ackTable.get(em.sender, receiver) <  seq_nr){
                return true;
            }
        }
        return false;
    }

    // - find the messages that can be delivered to an app of the owner of the ACK-table now
    //    by looking at the entries in the ACK-table
    // - order the messages correctly
    // - compute the new sequence numbers and directly update them in the ACK-table
    // ATTENTION: Side effect on ACK-Table
    public ArrayList<byte[]> getMessagesReadyForDelivery(int appId) {
        ArrayList<byte[]> result = new ArrayList<>();
        // Uses compare defined in EnumeratedMessage.java
        PriorityQueue<EnumeratedMessage> forApp = new PriorityQueue<>();
        for(EnumeratedMessage em : buffer) {
            if(ackTable.get(em.sender, ackTable.getOwner()) <  em.seq
                    && em.appId == appId){
                forApp.add(em);
            }
        }
        // Add enumerated messages in order if no earlier seq_nr is missing
        while(!forApp.isEmpty()){
            // Extract message with smallest sequence number
            EnumeratedMessage em = forApp.poll();
            // Exactly the message we are waiting for from this receiver
            if(ackTable.get(em.sender, ackTable.getOwner()) + 1  == em.seq ){
                result.add(em.msg);
                ackTable.update(em.sender, em.seq);
            }
            else{
                bufferedButNotDelivered += 1;
            }
        }

        return result;
    }
}

