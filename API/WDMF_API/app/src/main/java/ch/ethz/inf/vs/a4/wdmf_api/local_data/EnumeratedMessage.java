package ch.ethz.inf.vs.a4.wdmf_api.local_data;

import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Created by Jakob on 14.12.2016.
 */

public class EnumeratedMessage{
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

    public EnumeratedMessage(byte[] raw){
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
