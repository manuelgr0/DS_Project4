package ch.ethz.inf.vs.a4.wdmf_api;

import org.junit.Test;

import static org.junit.Assert.*;


public class ExampleUnitTest {
    @Test
    public void toStringWorks() throws Exception {

        AckTable ack1 = new AckTable("a");
        AckTable ack2 = new AckTable("b");
        AckTable ack3 = new AckTable("c");

        ack2.merge(ack3);
        ack2.update("c", 3);

        ack1.merge(ack2);


        ack1.update("b", 1);
        ack1.update("a", 2);






        System.out.println("ack1 = " + ack1.toString());



        assertEquals("", ack1.toString());
        //assertEquals("", ack2.toString());

    }
}