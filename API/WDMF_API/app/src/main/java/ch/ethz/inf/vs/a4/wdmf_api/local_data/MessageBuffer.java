package ch.ethz.inf.vs.a4.wdmf_api.local_data;


import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;

import ch.ethz.inf.vs.a4.wdmf_api.network_protocol_data.AckTable;

/**
 * Created by Jakob on 24.11.2016.
 */

// TODO: Split up large messages
// TODO: Replacement policy

final public class MessageBuffer {
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
        if(memory_space < em.size() ) {
            makeSpace(em.size());
        }
        buffer.add(em);
        memory_space -= em.size();
        seq_nr++;
    }

    // For messages which were sent from another node
    // and therefore we should know the seq_nr already
    public void addRemoteMessages(String sender, int seq_number, byte[] msg, int appID){
        EnumeratedMessage em = new EnumeratedMessage(msg, seq_number, sender, appID);
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
    public ArrayList<byte[]> getMessagesForReceiver(String receiver, AckTable at){
        ArrayList<byte[]> result = new ArrayList<byte[]>();
        for(EnumeratedMessage em : buffer) {
            //TODO: clarify whether it should be < or <= (Do we expect the seq_nr as next message or is it the last number that we received
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
            bytes -= buffer.get(0).size();
            memory_space += buffer.get(0).size();
            buffer.remove(0);
        }
        if (bytes > 0) {
            throw new OutOfMemoryError();
        }
    }

}

//default visibility -> visible in package only
class EnumeratedMessage{
    public int seq;
    public String sender;
    public byte[] msg;
    public int appId;

    EnumeratedMessage(byte[] m, int s, String snd, int appID){
        byte[] msg_copy = new byte[m.length];
        for (int i = 0 ; i < m.length; i++) {
            msg_copy[i] = m[i];
        }
        seq = s;
        msg = msg_copy;
        sender = snd;
        appId = appID;
    }

    EnumeratedMessage(byte[] raw){
        appId = (((int)raw[0]) << 24)
                + (((int)raw[1]) << 16)
                + (((int)raw[2]) << 8)
                + ((int)raw[3]);
        seq = (((int)raw[4]) << 24)
                + (((int)raw[5]) << 16)
                + (((int)raw[6]) << 8)
                + ((int)raw[7]);
        int name_length  =
                (((int)raw[8]) << 24)
                + (((int)raw[9]) << 16)
                + (((int)raw[10]) << 8)
                + ((int)raw[11]);
        sender = new String(Arrays.copyOfRange(raw, 8, 8 + name_length), Charset.forName("UTF-8"));
        msg = Arrays.copyOfRange(raw, 8 + name_length, raw.length);
    }

    // Stores [Seq_Nr][Length Of Sender Name][Name of Sender][Msg] in a copy
    public byte[] raw(){
        byte[] name = sender.getBytes(Charset.forName("UTF-8"));
        int name_length = name.length;
        byte[] result = new byte[msg.length + 8 + name_length];

        result[0] = (byte) (appId >> 24);
        result[1] = (byte) (appId >> 16);
        result[2] = (byte) (appId >> 8);
        result[3] = (byte) appId ;

        result[4] = (byte) (seq >> 24);
        result[5] = (byte) (seq >> 16);
        result[6] = (byte) (seq >> 8);
        result[7] = (byte) seq ;

        result[8] = (byte) (name_length >> 24);
        result[9] = (byte) (name_length >> 16);
        result[10] = (byte) (name_length >> 8);
        result[11] = (byte) name_length;

        for(int i = 0; i < name_length; i++){
            result[i + 8] = name[i];
        }
        for(int i = 0; i < msg.length; i++){
            result[i + 8 + name_length] = msg[i];
        }

        return result;
    }

    // size of stored enumerated message
    public int size(){
        return msg.length + sender.length()*2 + 8;
    }
}
