package ch.ethz.inf.vs.a4.wdmf_api;


import java.util.Hashtable;

public class AckTable {

    private Hashtable<String, Hashtable<String, Integer>> hash = new Hashtable<>();

    public AckTable() {
        //empty constructor
    }

    public void insert(String sender, String receiver, Integer value) {

        Hashtable<String, Integer> h;
        if (hash.containsKey(sender)) {
            h = hash.get(sender);
        } else {
            h = new Hashtable<String, Integer>();
            hash.put(sender, h);
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

        for (String t : this.hash.keySet()) {
            for (String o : other.hash.keySet()) {
                if (!this.hash.containsKey(o)) {
                    System.out.println("t is " + t + " o is " +o);
                    this.insert(t, o, -1);
                }
            }
        }

        for (String keyS : other.hash.keySet()) {
            //This doesn't have keyS
            if (!this.hash.containsKey(keyS)) {
                this.hash.put(keyS, other.hash.get(keyS));

                for (String n : this.hash.keySet()) {

                    if (!other.hash.containsKey(n)) {
                        this.insert(keyS, n, -1);
                    }
                }


            }

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

            }

        }

    }

    public boolean reachedAll(Integer seqNo) {
        return true;

    }

    public String toString() {

        return this.hash.toString();

    }

}


