package ch.ethz.inf.vs.a4.wdmf_api;


import java.util.Hashtable;

//TODO: Add function to ask if the owner of an ACK-Table already got a message with given sender and sequence number

public class AckTable {

    private Hashtable<String, Hashtable<String, Integer>> hash = new Hashtable<>();
    private String owner;

    public AckTable(String node) {
        insert(node, node, -1);
        this.owner = node;
    }

    public AckTable(String node, Hashtable<String, Hashtable<String, Integer>> h) {
        if (h.size() == 0)
            throw new IllegalArgumentException("Hashtable must be non-empty.");
        else {
            this.owner = node;
            this.hash = h;
        }
    }

    private void insert(String sender, String receiver, Integer value) {

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

    public void update(String receiver, Integer seqNo) {
        if (receiver != owner && hash.containsKey(owner) && hash.get(owner).containsKey(receiver))
            insert(owner, receiver, seqNo);
    }

    public boolean hasKey(String node) {
        return hash.containsKey(node);
    }

    public void delete(String node) {
        this.hash.remove(node);
        for(String key : this.hash.keySet()){
            this.hash.get(key).remove(node);
        }
    }

    public void merge(AckTable other) {

        for (String keyS : other.hash.keySet()) {
            //This doesn't have keyS
            if (!this.hash.containsKey(keyS)) {
                this.hash.put(keyS, (Hashtable<String, Integer>) other.hash.get(keyS).clone());

                for (String n : this.hash.keySet()) {

                    if (!other.hash.containsKey(n)) {
                        this.insert(keyS, n, -1);
                        this.insert(n, keyS, -1);
                    }
                }


            }

            //This does have keyS
            if (this.hash.containsKey(keyS)) {

                for (String keyR : other.hash.get(keyS).keySet()) {
                    //This doesn't have keyR or this copy is smaller
                    if (!this.hash.get(keyS).containsKey(keyR) || this.hash.get(keyS).get(keyR) < other.hash.get(keyS).get(keyR)) {
                        this.insert(keyS, keyR, other.hash.get(keyS).get(keyR));
                    }


                }

            }

        }

    }

    public boolean reachedAll(String sender, Integer seqNo) {
        for (String rec : this.hash.get(sender).keySet()) {
            if (sender != rec && this.hash.get(sender).get(rec) < seqNo) {
                return false;
            }
        }

        return true;

    }

    public String toString() {

        return this.owner + this.hash.toString();

    }

}


