package ch.ethz.inf.vs.a4.wdmf_api;

import org.junit.Test;

import static org.junit.Assert.*;


public class ExampleUnitTest {
    @Test
    public void toStringWorks() throws Exception {
        LCTable ack = new LCTable();
        ack.insert ("eins", 1);
        ack.insert ("zwei", 2);

        LCTable bck = new LCTable();
        bck.insert ("drei", 3);
        bck.insert ("zwei", 4);

        ack.merge(bck);

       assertEquals("lol", ack.toString());
    }
}