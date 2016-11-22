package ch.ethz.inf.vs.a4.wdmf_api;


import java.util.Hashtable;

public class AckTable {

    private Hashtable<String, Hashtable<String, Integer>> hash = new Hashtable<>();

    public AckTable(String node) {
        insert(node, node, -1);
    }


    private AckTable() {
        //private, empty constructor for cloning
    }

    public void insert(String sender, String receiver, Integer value) {

        System.out.println();
        Hashtable<String, Integer> h;
        if (this.hash.containsKey(sender)) {
            h = this.hash.get(sender);
        } else {
            h = new Hashtable<String, Integer>();
            this.hash.put(sender, h);
        }

        h.put(receiver, value);
    }

    public boolean hasKey(String node) {
        return hash.containsKey(node);
    }

    public void delete(String node) {
        this.hash.remove(node);
    }


    public void merge(AckTable other) {


        for (String keyS : other.hash.keySet()) {
            //This doesn't have keyS
            if (!this.hash.containsKey(keyS)) {
                this.hash.put(keyS, other.hash.get(keyS));

                for (String n : this.hash.keySet()) {

                    if (!other.hash.containsKey(n)) {
                        System.out.println(keyS + "  " + n);
                        this.hash.get("b").put("a", -1);
                        this.hash.get("a").put("b", -1);
                        //this.insert(keyS, n, -1); // <-- fucked up line
                        //this.insert(n, keyS, -1);
                    }
                }


            } /*

            //This does have keyS
            if (this.hash.containsKey(keyS)) {

                for (String keyR : other.hash.get(keyS).keySet()) {
                    //This doesn't have keyR
                    if (!this.hash.get(keyS).containsKey(keyR)) {
                        this.insert(keyS, keyR, other.hash.get(keyS).get(keyR));
                    }

                    //This does have keyR
                    if (this.hash.get(keyS).containsKey(keyR)) {

                        if (this.hash.get(keyS).get(keyR) < other.hash.get(keyS).get(keyR))
                            this.insert(keyS, keyR, other.hash.get(keyS).get(keyR));
                    }

                }

            } */

        }

    }

    public boolean reachedAll(String sender, Integer seqNo) {
        for (String rec : this.hash.get(sender).keySet()) {
            if (this.hash.get(sender).get(rec) < seqNo) {
                return false;
            }
        }

        return true;

    }

    public AckTable clone() {
        AckTable ret = new AckTable();
        ret.hash = (Hashtable<String, Hashtable<String, Integer>>) this.hash.clone();
        return ret;
    }

    public String toString() {

        return this.hash.toString();

    }

}


