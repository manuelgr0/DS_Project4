package ch.ethz.inf.vs.a4.wdmf_api;

import org.junit.Test;

import static org.junit.Assert.*;


public class ExampleUnitTest {
    @Test
    public void toStringWorks() throws Exception {

        LCTable a = new LCTable("a");
        LCTable b = new LCTable("b");
        LCTable c = new LCTable("c");

        c.merge("a", a);
        b.merge("c", c);


        a.merge("b", b);


        System.out.println("ack1 = " + b.toString());


        assertEquals("", a.toString());
        //assertEquals("", ack2.toString());

    }
}