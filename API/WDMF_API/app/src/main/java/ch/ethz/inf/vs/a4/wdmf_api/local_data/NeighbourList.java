package ch.ethz.inf.vs.a4.wdmf_api.local_data;

import java.util.Date;
import java.util.HashMap;
import java.lang.Iterable;
import java.util.Iterator;

/**
 * Created by Jakob on 11.12.2016.
 *
 * A neighbour is a device visible through WIFI or whatever technology we use.
 * We are thinking about the neighbours in three different types:
 *  1. We have recently seen the neighbour and we know it belongs to our network
 *  2. We have seen the neighbour, but it has not been added to the network
 *  3. We cannot remember that we have seen the neighbour before (First touch or long break)
 * In this class, we store type 2 neighbours only. Each visible neighbour should be checked
 * against the LC-Table (type 1), if not contained look in the NeighbourList (type 2) and if it
 * not in there either, try to connect immediately. Depending on the result, it should be added
 * to one of the mentioned data structures.
 *
 */

public class NeighbourList {
    private HashMap<String, Date> neighbours;

    public NeighbourList(){
        neighbours = new HashMap<>();
    }

    // Add or update with date
    public void update_neighbour(String s, Date d) {
        neighbours.put(s,d);
    }
    // Add or update with current date
    public void update_neighbour(String s) {
        neighbours.put(s, new Date());
    }
    // Add or update many with current date
    public void update_neighbours(Iterable<String> visible){
        for(String n : visible){
            neighbours.put(n, new Date());
        }

    }
    // Note: Depending on the IO backend interface, we might want another add
    // function which takes an Iterable of something else

    // returns the date when a neighbour has been contacted last time,
    // or null if it has never been seem before
    public Date last_contact(String s){
        return neighbours.get(s);
    }


    // Remove really old entries to free memory
    public void remove_older_than(Date d){
        Iterator<HashMap.Entry<String, Date>> i = neighbours.entrySet().iterator();
        while(i.hasNext()){
            HashMap.Entry<String, Date> neighbour = i.next();
            if( neighbour.getValue().before(d)){
                i.remove();
            }
        }
    }
}
