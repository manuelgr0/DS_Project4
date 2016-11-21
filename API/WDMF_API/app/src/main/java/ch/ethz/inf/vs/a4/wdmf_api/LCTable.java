package ch.ethz.inf.vs.a4.wdmf_api;


import java.util.Hashtable;

public class LCTable {

    private Hashtable<String, Integer> hash = new Hashtable<String,Integer>();

    public LCTable() {
        //empty constructor
    }

    public void insert(String node, Integer value){
        hash.put(node, value);

    }

    public boolean hasKey(String node){
       return hash.containsKey(node);
    }

    public void delete (String node){
        hash.remove(node);

    }

   
    public void merge (LCTable other){
       for (String key : other.hash.keySet()){
           if (!hasKey(key) || hash.get(key) < other.hash.get(key)){
                hash.put(key, other.hash.get(key));
           }
       }
    }

    public void update (String node, Integer value){
        hash.put(node, value);
    }

    public String toString (){
        return hash.toString();
    }

}
