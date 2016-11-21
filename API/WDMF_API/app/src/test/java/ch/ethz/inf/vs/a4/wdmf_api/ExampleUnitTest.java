package ch.ethz.inf.vs.a4.wdmf_api;

import org.junit.Test;

import static org.junit.Assert.*;


public class ExampleUnitTest {
    @Test
    public void toStringWorks() throws Exception {

        AckTable ack1 = new AckTable();
        AckTable ack2 = new AckTable();

        ack1.insert("a", "a", -1);
        ack1.insert("a", "b", 1);
        ack1.insert("b", "a", 2);
        ack1.insert("b", "b", -1);

        ack2.insert("b", "b", -1);
        ack2.insert("b", "c", 3);
        ack2.insert("c", "b", 4);
        ack2.insert("c", "c", -1);

        ack1.merge(ack2);


        assertEquals("lol", ack1.toString());
    }
}