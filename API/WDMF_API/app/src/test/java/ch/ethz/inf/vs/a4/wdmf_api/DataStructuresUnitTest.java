package ch.ethz.inf.vs.a4.wdmf_api;

import org.junit.Test;

import ch.ethz.inf.vs.a4.wdmf_api.network_protocol_data.AckTable;
import ch.ethz.inf.vs.a4.wdmf_api.network_protocol_data.LCTable;

import static org.junit.Assert.*;

/**
 * Created by Jakob on 24.11.2016.
 */

public class DataStructuresUnitTest {
    private static String node1 = "Node 1";
    private static String node2 = "Node 2";
    private static String node3 = "Node 3";

    @Test
    public void LCTableBuildUp() throws Exception {
        LCTable lct1 = new LCTable(node1);

        // Not crucial
       // assertTrue(
       //         "LC-Tables should contain the owner",
        //        lct1.hasKey(node1)
        //);

        // check owner of tables
        assertEquals(
                "Node 1 is owner of its table",
                node1,
                lct1.getOwner()
        );
    }

    @Test
    public void MergeLCTables() throws Exception {
        LCTable lct1 = new LCTable(node1);
        LCTable lct2 = new LCTable(node2);
        LCTable lct3 = new LCTable(node3);

        lct3.merge(node1, lct1); // [1][2][1,3]
        lct1.merge(node2, lct2); // [1,2][2][1,3]
        lct1.merge(node3, lct3); // [1,2,3][2][1,3]
        lct3.merge(node2, lct2); // [1,2,3][2][1,3,2]

        System.out.println("LC-Table 1: " + lct1.toString());
        System.out.println("LC-Table 2: " + lct2.toString());
        System.out.println("LC-Table 3: " + lct3.toString());

        // check whether the tables contain the expected values
        assertTrue(
                "LC-Table 1 merges correctly",
                /*lct1.hasKey(node1) && */lct1.hasKey(node2) && lct1.hasKey(node3)
                        && lct1.getOwner().equals(node1)
        );
        assertTrue(
                "LC-Table 3 merges correctly",
                lct3.hasKey(node1) && lct3.hasKey(node2) /*&& lct3.hasKey(node3)*/
                        && lct3.getOwner().equals(node3)
        );
        assertTrue(
                "LC-Table 2 is not affected by other merges",
                !lct2.hasKey(node1) && /*lct2.hasKey(node2) && */!lct2.hasKey(node3)
                        && lct2.getOwner().equals(node2)
        );
    }

    @Test
    public void AckTableSingleBuildUpInsertAndDelete() throws Exception {
        AckTable at1 = new AckTable(node1);
        AckTable at2 = new AckTable(node2);
        AckTable at3 = new AckTable(node3);

        // check owner of table
        assertEquals(
                "Node 1 is owner of its ACK-Table",
                node1,
                at1.getOwner()
        );

        //merge before update
        at1.merge(at2);
        at1.merge(at3);

        // do some updates
        at1.update(node1, 2);
        at1.update(node2, 1);
        at1.update(node3, 5);
        at1.update(node2, 3);
        at1.update(node1, 1);

        System.out.println("ACK-Table 1: " + at1.toString());

        assertTrue(
                "ACK-Tables contain all encountered nodes",
                at1.hasKey(node1) && at1.hasKey(node2) && at1.hasKey(node3)
        );

        //delete a node
        at1.delete(node2);

        assertTrue(
                "ACK-Tables contain all encountered nodes minus deleted nodes",
                at1.hasKey(node1) && !at1.hasKey(node2) && at1.hasKey(node3)
        );
    }

    @Test
    public void SimpleMergeAckTables() throws Exception {
        AckTable at1 = new AckTable(node1);
        AckTable at2 = new AckTable(node2);

        at1.merge(at2);
        at2.merge(at1);

        // shouldn't change anything
        at1.merge(at1);

        // check table entries
        assertTrue(
                "Check wheter merge of ACK-Table 1 worked as expected",
                at1.hasKey(node1) && at1.hasKey(node2)
        );
        assertTrue(
                "Check wheter merge of ACK-Table 2 worked as expected",
                at2.hasKey(node1) && at2.hasKey(node2)
        );

        // check owner of tables
        assertEquals(
                "Node 1 is still owner of its ACK-Table",
                node1,
                at1.getOwner()
        );
        assertEquals(
                "Node 2 is still owner of its ACK-Table",
                node2,
                at2.getOwner()
        );

    }

    @Test
    public void MergeAndUseAckTables() throws Exception {
        AckTable at1 = new AckTable(node1);
        AckTable at2 = new AckTable(node2);
        AckTable at3 = new AckTable(node3);

        // send 10 messages from node 1 to nodes 2 and 3
        // they receive it + merge the tables
        for (int i = 0; i < 10; i ++){
            // merge before update
            at2.merge(at1);
            // node 2 writes in its table that it received packet with nr i
            at2.update(node1, i);
            // now node 2 sends ack back to node 1 which can merge tables
            at1.merge(at2);
            //then node 2 forwards the information to node 3 which merges and updates locally
            at3.merge(at2);
            at3.update(node1, i);
            // lastly node 3 also sends ack back to node 2 which can merge the tables then
            at2.merge(at3);
        }

        System.out.println("ACK-Table 1: " + at1.toString());
        System.out.println("ACK-Table 2: " + at2.toString());
        System.out.println("ACK-Table 3: " + at3.toString());

        // Check the table entries
        assertTrue(
                "Node 1 has correct ACK-Table",
                //it should know at least that node 3 got message 8
                at3.reachedAll(node1, 8)
        );
        assertTrue(
                "Node 2 has correct ACK-Table",
                //it should know everything at this point because it was merging with node 3
                at2.reachedAll(node1, 9)
        );
        assertTrue(
                "Node 1 has correct ACK-Table",
                //it should know everything because it0s at the end of the chain
                at3.reachedAll(node1, 9)
        );
    }
}
