package ch.ethz.inf.vs.a4.wdmf_api;

/**
 * Created by Jakob on 25.11.2016.
 */

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.*;

public class MessageBufferTest {
    @Test
    public void buildMessageBuffer(){
        // Create buffer with max size 1000B and the name Jakob for the local node
        MessageBuffer buf = new MessageBuffer("Jakob", 1000);

        // Add some messages
        buf.addLocalMessage(new byte[]{0,1,2,3,4,5,6,7,8,9});
        buf.addLocalMessage(new byte[]{10,11,12,13,14,15,16,17,18,19});
        buf.addLocalMessage(new byte[]{20,21,22,23,24,25,26,27,28,29});

        // Create ACK tables
        AckTable at1 = new AckTable("Jakob");
        AckTable at2 = new AckTable("Manuel");
        at1.merge(at2);
        at2.merge(at1);

        // Check content of buffer
        ArrayList<byte[]> toSend = buf.getMessagesForReceiver("Manuel", at1);

        assertEquals(
                "All messages returned by buffer",
                3,
                toSend.size()
        );

        for(byte[] array : toSend){
            System.out.println(Arrays.toString(array));
        }

        // Assuming the order is preserved
        // Note that the retrieved data contains a header first
        // with the message byte array following afterwards
        assertTrue(
                "Content of messages consistent",
                toSend.get(0)[toSend.get(0).length - 1] == 9 &&
                toSend.get(0)[toSend.get(0).length - 10] == 0 &&
                toSend.get(1)[toSend.get(1).length - 1] == 19 &&
                toSend.get(1)[toSend.get(1).length - 10] == 10 &&
                toSend.get(2)[toSend.get(2).length - 1] == 29 &&
                toSend.get(2)[toSend.get(2).length - 10] == 20
        );
    }

    @Test
    public void memoryLimitTest(){
        MessageBuffer buf = new MessageBuffer("Jakob", 100);

        // Create ACK tables
        AckTable at1 = new AckTable("Jakob");
        AckTable at2 = new AckTable("Manuel");
        at1.merge(at2);
        at2.merge(at1);

        byte[] msg = new byte[60];
        msg[50] = 6;

        buf.addLocalMessage(msg);
        ArrayList<byte[]> toSend1 = buf.getMessagesForReceiver("Manuel", at1);

        msg[50] = 0;
        msg[59] = 77;
        buf.addLocalMessage(msg);
        ArrayList<byte[]> toSend2 = buf.getMessagesForReceiver("Manuel", at1);

        // Check content of buffer

        assertTrue(
                "Content of the inserted message was retrieved",
                toSend1.get(0)[toSend1.get(0).length - 10] == 6
        );

        assertEquals(
                "Buffer is not overfilled",
                1,
                toSend2.size()
        );

        assertTrue(
                "Content of the newest message was retrieved",
                toSend2.get(0)[toSend2.get(0).length - 1] == 77
                        && toSend2.get(0)[toSend2.get(0).length - 10] == 0
        );
    }
}
