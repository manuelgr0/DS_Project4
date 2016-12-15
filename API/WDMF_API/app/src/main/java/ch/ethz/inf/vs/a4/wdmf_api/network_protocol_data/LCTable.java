package ch.ethz.inf.vs.a4.wdmf_api.network_protocol_data;


import java.util.Date;
import java.util.Hashtable;
import java.util.Set;

public class LCTable {

    private Hashtable<String, Long> hash = new Hashtable<>();
    private String owner;

    public LCTable(String node) {
        this.owner = node;
    }

    public LCTable(String node, Hashtable<String, Long> h) {
        this.owner = node;
        this.hash = h;
    }

    public long getLastContactTimestamp(String node){
        return hash.get(node);
    }

    public Set<String> getNodeSet(){
        return hash.keySet();
    }

    public void removeNode(String node){
        hash.remove(node);
    }

    public String getOwner() {
        return owner;
    }

    public boolean hasKey(String node){
       return hash.containsKey(node);
    }

    public boolean entryIsOlderThan(String node, long timestamp){
        return hash.containsKey(node) && hash.get(node) < timestamp;
    }

    public void delete (String node){
        hash.remove(node);

    }


    public void merge (String sender, LCTable other){
       for (String key : other.hash.keySet()){
           if (key != owner && (!hasKey(key) || hash.get(key) < other.hash.get(key))){
                hash.put(key, other.hash.get(key));
           }
       }
        Date date = new Date();
        hash.put(sender, date.getTime());
    }


    public String toString (){
        return owner + hash.toString();
    }

}
