package ch.ethz.inf.vs.a4.wdmf_api.local_data;


import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.PriorityQueue;

import ch.ethz.inf.vs.a4.wdmf_api.network_protocol_data.AckTable;

/**
 * Created by Jakob on 24.11.2016.
 */

// TODO: Split up large messages
// TODO: Replacement policy

final public class MessageBuffer {
    // In the ACK-table we store the nr we have received already, so we start with init_seq_nr + 1
    private int seq_nr = AckTable.INIT_SEQ_NR + 1;
    private ArrayList<EnumeratedMessage> buffer;
    private String owner;
    private long memory_space;

    // Provide the name in the network associated with the local node
    // and specify the max bytes that should be buffered
    public MessageBuffer(String ownerIdentifier, long bufferSize) {
        owner = ownerIdentifier;
        buffer = new ArrayList<EnumeratedMessage>();
        memory_space = bufferSize;
        if(memory_space <= 0) {
            throw new IllegalArgumentException("bufferSize must be positive");
        }
    }

    // For messages with a local origin, we don't know the seq_nr yet
    public void addLocalMessage(byte[] msg, int appID){
        // Disable for JUnit test
        // Log.d("Message Buffer", "Message buffer stores data:\n" + Arrays.toString(msg));
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
    public ArrayList<byte[]> getEnumeratedMessagesForReceiver(String receiver, AckTable at){
        ArrayList<byte[]> result = new ArrayList<byte[]>();
        for(EnumeratedMessage em : buffer) {
            if(at.get(em.sender, receiver) <  seq_nr){
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
        // Remove buffer until we have enough space
        while(bytes > memory_space && !buffer.isEmpty()){
            // TODO: change replacement policy here
            // Remove oldest
            memory_space += buffer.get(0).size();
            buffer.remove(0);
        }
        if (memory_space < bytes) {
            throw new OutOfMemoryError();
        }
    }

    public boolean hasMessagesForReceiver(String receiver, AckTable ackTable) {
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
    public ArrayList<byte[]> getMessagesReadyForDelivery(int appId, AckTable ackTable) {
        ArrayList<byte[]> result = new ArrayList<>();
        // Uses compare defined in EnumeratedMessage.java
        PriorityQueue<EnumeratedMessage> forApp = new PriorityQueue<>();
        for(EnumeratedMessage em : buffer) {
            if(ackTable.get(em.sender, ackTable.getOwner()) <  em.seq){
                forApp.add(em);
            }
        }
        // Add enumerated messages in order if no earlier seq_nr is missing
        for(EnumeratedMessage em : forApp){
            // Exactly the message we are waiting for from this receiver
            if(ackTable.get(em.sender, ackTable.getOwner()) + 1  == em.seq ){
                result.add(em.msg);
                ackTable.update(em.sender, em.seq);
            }
        }

        return result;
    }
}

