package ch.ethz.inf.vs.a4.wdmf_api;


import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Jakob on 24.11.2016.
 */

// TODO: Split up large messages
// TODO: Replacement policy
// TODO: Unittests

final public class MessageBuffer {
    private int seq_nr = AckTable.INIT_SEQ_NR + 1;
    private ArrayList<EnumeratedMessage> buffer;
    private String owner;
    private long memory_space;

    // Provide the name in the network associated with the local node
    // and specify the max bytes that should be buffered
    MessageBuffer(String ownerIdentifier, long bufferSize) {
        owner = ownerIdentifier;
        buffer = new ArrayList<EnumeratedMessage>();
        memory_space = bufferSize;
        if(memory_space <= 0) {
            throw new IllegalArgumentException("bufferSize must be positive");
        }
    }

    // For messages with a local origin, we don't know the seq_nr yet
    public void addLocalMessage(byte[] msg){
        EnumeratedMessage em = new EnumeratedMessage(msg, seq_nr, owner);
        if(memory_space < em.size() ) {
            makeSpace(em.size());
        }
        buffer.add(em);
        memory_space -= em.size();
        seq_nr++;
    }

    // For messages which were sent from another node
    // and therefore we should know the seq_nr already
    public void addRemoteMessages(String sender, int seq_number, byte[] msg){
        EnumeratedMessage em = new EnumeratedMessage(msg, seq_number, sender);
        if(memory_space < em.size()) {
            makeSpace(em.size());
        }
        buffer.add(em);
        memory_space -= em.size();
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

    // Kick out messages until we have at least X bytes free memory
    private void makeSpace(int bytes){
        //TODO
        throw new OutOfMemoryError();
        // Best fix: memory_space += bytes :)
    }

}

//default visibility -> visible in package only
class EnumeratedMessage{
    public int seq;
    public String sender;
    public byte[] msg;

    EnumeratedMessage(byte[] m, int s, String snd){
        byte[] msg_copy = new byte[m.length];
        for (int i = 0 ; i < m.length; i++) {
            msg_copy[i] = m[i];
        }
        seq = s;
        msg = msg_copy;
        sender = snd;
    }

    EnumeratedMessage(byte[] raw){
        seq = (((int)raw[0]) << 24)
                + (((int)raw[1]) << 16)
                + (((int)raw[2]) << 8)
                + ((int)raw[3]);
        int name_length  = (((int)raw[4]) << 24)
                + (((int)raw[5]) << 16)
                + (((int)raw[6]) << 8)
                + ((int)raw[7]);
        sender = new String(Arrays.copyOfRange(raw, 8, 8 + name_length), Charset.forName("UTF-8"));
        msg = Arrays.copyOfRange(raw, 8 + name_length, raw.length);
    }

    // Stores [Seq_Nr][Length Of Sender Name][Name of Sender][Msg] in a copy
    public byte[] raw(){
        byte[] name = sender.getBytes(Charset.forName("UTF-8"));
        int name_length = name.length;
        byte[] result = new byte[msg.length + 8 + name_length];

        result[0] = (byte) (seq >> 24);
        result[1] = (byte) (seq >> 16);
        result[2] = (byte) (seq >> 8);
        result[3] = (byte) seq ;

        result[4] = (byte) (name_length >> 24);
        result[5] = (byte) (name_length >> 16);
        result[6] = (byte) (name_length >> 8);
        result[7] = (byte) name_length;

        for(int i = 0; i < name_length; i++){
            result[i + 8] = name[i];
        }
        for(int i = 0; i < result.length; i++){
            result[i + 8 + name_length] = msg[i];
        }

        return result;
    }

    // size of stored enumerated message
    public int size(){
        return msg.length + sender.length()*2 + 8;
    }
}
