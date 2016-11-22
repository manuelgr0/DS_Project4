package ch.ethz.inf.vs.a4.wdmf_api;

import org.junit.Test;

import static org.junit.Assert.*;


public class ExampleUnitTest {
    @Test
    public void toStringWorks() throws Exception {

        AckTable ack1 = new AckTable("a");
        AckTable ack2 = new AckTable("b");
        //AckTable ack3 = ack2.clone();

      /*  ack1.insert("a", "b", 1);
        ack1.insert("b", "a", 2);
        ack1.insert("b", "b", -1);


        ack2.insert("b", "c", 3);
        ack2.insert("c", "b", 4);
        ack2.insert("c", "c", -1); */

        /*ack1.merge(ack2);

        System.out.println("ack1 = " + ack1.toString());
        System.out.println("ack2 = " + ack2.toString());


        assertEquals("", ack1.toString());*/
        //assertEquals("", ack2.toString());

        LCTable lcTable = new LCTable();
        lcTable.insert("a", 1);

        Message msg = new Message(lcTable, ack1);
        Message msg1 = new Message(msg.getRawData());
    }
}