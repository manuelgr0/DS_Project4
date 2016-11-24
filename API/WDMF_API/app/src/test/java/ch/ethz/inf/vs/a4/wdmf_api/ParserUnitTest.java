package ch.ethz.inf.vs.a4.wdmf_api;

/**
 * Created by Jakob on 24.11.2016.
 */

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class ParserUnitTest {

    @Test
    // Tests simple message parsing and reading them afterwards
    public void buildAndUnbuild() throws Exception {
        List<byte[]> msgs = new ArrayList<>();

        msgs.add("Message 1".getBytes());
        msgs.add("Message 2".getBytes());

        byte[] msg3 = {1,2,3,42};
        msgs.add(msg3);

        Message msg = new Message(1, "Jakob", msgs);

        List<byte[]> afterParsing = msg.getMessageContents();

        assertEquals("String message 1 is the same before and after parsing",
                "Message 1",
                new String(afterParsing.get(0))
                );
        assertEquals("String message 2 is the same before and after parsing",
                "Message 2",
                new String(afterParsing.get(1))
        );
        assertTrue("Byte message is the same before and after parsing",
                afterParsing.get(2)[0] == 1
                && afterParsing.get(2)[1] == 2
                && afterParsing.get(2)[2] == 3
                && afterParsing.get(2)[3] == 42
        );

        assertEquals(
                "sender is correct",
                "Jakob",
                msg.getSender()
        );
    }

    @Test
    // Checks data and method-call safety of the Message class
    public void internalSafety() throws Exception {

        List<byte[]> msgs = new ArrayList<>();
        msgs.add("Message 1".getBytes());
        msgs.add("Message 2".getBytes());

        Message packet1 = new Message(1, "Jakob", msgs);

        msgs.remove(1);
        msgs.add("Message 2.1".getBytes());

        Message packet2 = new Message(2, "Jakob", msgs);

        // Check whether it can handle reading fields we didn't specify
        packet1.getAckTable();
        packet2.getLCTable();

        //Check wheter the content is as expected
        assertEquals(
                "Message content is consistent (1)",
                "Message 1",
                new String(packet1.getMessageContents().get(0))
        );
        assertEquals(
                "Message content is consistent (2)",
                "Message 2",
                new String(packet1.getMessageContents().get(1))
        );
        assertEquals(
                "Message content is consistent (3)",
                "Message 1",
                new String(packet2.getMessageContents().get(0))
        );
        assertEquals(
                "Message content is consistent (4)",
                "Message 2.1",
                new String(packet2.getMessageContents().get(1))
        );
    }
}
