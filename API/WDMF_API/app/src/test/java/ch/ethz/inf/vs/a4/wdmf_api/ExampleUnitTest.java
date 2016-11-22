package ch.ethz.inf.vs.a4.wdmf_api;

import android.util.Log;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;


public class ExampleUnitTest {
    @Test
    public void toStringWorks() throws Exception {

        /*AckTable ack1 = new AckTable("a");
        AckTable ack2 = new AckTable("b");afds*/
        //AckTable ack3 = ack2.clone();

        /*  ack1.insert("a", "b", 1);
        ack1.insert("b", "a", 2);
        ack1.insert("b", "b", -1);

        ack2.insert("b", "c", 3);
        ack2.insert("c", "b", 4);
        ack2.insert("c", "c", -1);
         */

        /*ack1.merge(ack2);
        System.out.println("ack1 = " + ack1.toString());
        System.out.println("ack2 = " + ack2.toString());asdf*/

        //assertEquals("", ack1.toString());
        //assertEquals("", ack2.toString());

        List<byte[]> msgs = new ArrayList<>();

        msgs.add("msg1".getBytes());
        msgs.add("msg22ajkdlsöfljasdölfadsjfasdjfsdölkfjasdölkjfjkadslfjlk".getBytes());
        msgs.add("msg3".getBytes());

        Message msg = new Message(msgs);
        System.out.println("Message after flattening: " + new String(msg.getRawData()));
        Message m = new Message(msg.getRawData());
        System.out.println  ("Message after parsing: " +
                                new String(m.getMessageContents().get(0)) + ", " +
                                new String(m.getMessageContents().get(1)) + ", " +
                                new String(m.getMessageContents().get(2))
                            );

        LCTable lc = new LCTable("A");
        lc.merge("B", new LCTable("B"));
        lc.merge("C", new LCTable("C"));

        AckTable ack = new AckTable("A");

        msg = new Message(lc, ack);
        m = new Message(msg.getRawData());
    }
}