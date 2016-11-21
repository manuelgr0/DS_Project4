package ch.ethz.inf.vs.a4.wdmf_api;

import org.junit.Test;

import static org.junit.Assert.*;


public class ExampleUnitTest {
    @Test
    public void toStringWorks() throws Exception {
        AckTable ack = new AckTable();
        ack.insert ("eins", 1);
        ack.insert ("zwei", 2);

        AckTable bck = new AckTable();
        bck.insert ("drei", 3);
        bck.insert ("zwei", 4);

        ack.merge(bck);

       assertEquals("lol", ack.toString());
    }
}