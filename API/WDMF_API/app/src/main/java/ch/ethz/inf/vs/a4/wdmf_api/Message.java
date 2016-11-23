package ch.ethz.inf.vs.a4.wdmf_api;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Alessandro on 22.11.2016.
 */

//TODO: Add sender and sequence number to a message

public class Message {

    public static byte TYPE_TABLES_ONLY = 1;
    public static byte TYPE_MESSAGES_ONLY = 2;

    private byte type;
    private byte[] data;
    private LCTable lcT;
    private AckTable ackT;
    private List<byte[]> messages = new ArrayList<>();


    /** Several different constructors */

    public Message(byte[] data) {
        this.data = data;
        parse(this.data);
    }

    public Message(LCTable lcT, AckTable ackT) {
        type = TYPE_TABLES_ONLY;
        this.lcT = lcT;
        this.ackT = ackT;
        flatten(this.lcT, this.ackT);
    }

    public Message(List<byte[]> messages) {
        type = TYPE_MESSAGES_ONLY;
        this.messages = messages;
        flatten(this.messages);
    }

    /** Getters */

    public LCTable getLCTable() {
        return lcT;
    }

    public AckTable getAckTable() {
        return ackT;
    }

    public List<byte[]> getMessageContents() {
        return messages;
    }

    public byte[] getRawData() {
        return data;
    }

    /** Parsers */

    private void parse(byte[] data) {
        type = data[0];
        if (type == TYPE_MESSAGES_ONLY) {
            int dataSize = data.length;

            int offset = 4;
            while (offset < dataSize) {
                int msgSize = readNumber(data, offset);
                byte[] msg = new byte[msgSize];

                offset += 4;
                for (int i = 0; i < msgSize; i++)
                    msg[i] = data[offset + i];
                offset += msgSize;

                messages.add(msg);
            }

        } else if (type == TYPE_TABLES_ONLY) {
            int sizeLT = readNumber(data, 4);
            int sizeAT = data.length - sizeLT - 8;

            byte[] LT = new byte[sizeLT];
            byte[] AT = new byte[sizeAT];

            int offset = 8;
            for (int i = 0; i < sizeLT; i++)
                LT[i] = data[offset + i];

            offset += sizeLT;
            for (int i = 0; i < sizeAT; i++)
                AT[i] = data[offset + i];

            String lcTable = new String(LT);
            String ackTable = new String(AT);

            parseLCTable(lcTable);
            parseACKTable(ackTable);
        }
    }

    private void parseLCTable (String lct) {
        System.out.println("LCTable in raw format: " + lct);
        //Log.d("LCTable", lct); // Does not work with JUnit Tests

        String[] s = lct.substring(0, lct.length()-1).split("\\{");
        String owner = s[0];
        if (s.length == 1) { // nothing in LCTable yet
            lcT = new LCTable(owner);
        } else {
            String mappings = s[1];
            //System.out.println("owner: " + owner);
            //System.out.println("mappings: " + mappings);

            String[] mapping = mappings.split(", ");
            Hashtable<String, Long> hashtable = new Hashtable<>();
            for (int i = 0; i < mapping.length; i++) {
                String[] m = mapping[i].split("=");
                hashtable.put(m[0], new Long(m[1]));
            }

            lcT = new LCTable(owner, hashtable);
        }
        System.out.println("toString of LCTable after parsing: " + lct.toString());
    }

    private void parseACKTable(String ackt) {
        System.out.println("AckTable in raw format: " + ackt);
        //Log.d("AckTable", ackt); // Does not work with JUnit Tests

        int indexData = ackt.indexOf("{");
        String owner = ackt.substring(0, indexData);
        String s = ackt.substring(indexData+1, ackt.length()-1);
        //System.out.println("owner: " + owner);
        //System.out.println("mappings: " + s);

        List<String> rows = new ArrayList<>();
        Matcher mat = Pattern.compile("\\{(.*?)\\}").matcher(s);
        while (mat.find())
            rows.add(mat.group(1));

        Hashtable<String, Hashtable<String, Integer>> hashtable = new Hashtable<>();

        for (int i = 0; i < rows.size(); i++) {
            Hashtable<String, Integer> h = new Hashtable<>();
            indexData = s.indexOf("=");
            String o1 = s.substring(0, indexData);
            if ((indexData + 1) + (rows.get(i).length() + 2) < s.length()) { // not last mapping in string
                s = s.substring((indexData+1) + (2+rows.get(i).length()+2)); // truncate s
            }
            String[] row = rows.get(i).split(", ");
            for (int j = 0; j < row.length; j++) {
                String[] m = row[j].split("=");
                h.put(m[0], new Integer(m[1]));
            }

            hashtable.put(o1, h);
        }

        ackT = new AckTable(owner, hashtable);
        System.out.println("toString of AckTable after parsing: " + ackT.toString());
    }

    /** Flatteners */

    // creates a message that contains only tables;
    private void flatten(LCTable lcT, AckTable ackT) {
        type = TYPE_TABLES_ONLY;

        byte[] lT = lcT.toString().getBytes();
        byte[] aT = ackT.toString().getBytes();

        // find out how much raw data
        // first 4 bytes for type (I know, type has only one byte...)
        // second 4 bytes for length of lT
        int sizeOfData = 4 + 4 + lT.length + aT.length;

        // allocate space for data
        data = new byte[sizeOfData];
        data[0] = type;

        // write flattened data structures to data
        writeNumber(lT.length, data, 4);
        int offset = 8;
        for (int i = 0; i < lT.length; i++)
            data[offset + i] = lT[i];
        offset += lT.length;

        for (int i = 0; i < aT.length; i++)
            data[offset + i] = aT[i];
    }

    // creates a message that consists only of buffered messages
    private void flatten(List<byte[]> msgs) {
        type = TYPE_MESSAGES_ONLY;

        int n = msgs.size();

        // find out how much raw data
        int sizeOfData = 4;
        for (int i = 0; i < n; i++)
            sizeOfData += (msgs.get(i).length + 4);

        // allocate space for data
        data = new byte[sizeOfData];
        data[0] = type;

        int offset = 4;
        for (int i = 0; i < n; i++) {
            byte[] b = msgs.get(i);
            int size = b.length;
            writeNumber(size, data, offset);
            offset += 4;
            for (int j = 0; j < size; j++)
                data[offset + j] = b[j];
            offset += size;
        }
    }

    /** Helpers */

    // writes an int n to a byte array b starting at pos
    // Little Endian
    private void writeNumber(int n, byte[] b, int pos) {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(n);
        byte[] res = bb.array();

        for (int i = 0; i < 4; i++)
            b[pos + i] = res[i];
    }

    // reads an int from a byte array b starting at pos
    // assumes int is stored in Little Endian in b
    private int readNumber(byte[] b, int pos) {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < 4; i++)
            bb.put(b[pos + i]);

        return bb.getInt(0);
    }

}
