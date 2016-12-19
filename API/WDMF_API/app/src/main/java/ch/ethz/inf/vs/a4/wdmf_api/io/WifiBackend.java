package ch.ethz.inf.vs.a4.wdmf_api.io;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import ch.ethz.inf.vs.a4.wdmf_api.service.MainService;

/**
 * Created by Jakob on 18.12.2016.
 */

public class WifiBackend {

    private static Connection con;
    private static HashMap<String, byte[]> wifiBuffer = new HashMap<>();
    private static boolean outGoingConnectionReady = false;
    private static volatile boolean incommingConnectionWaiting = false;
    private static String mMacAddr = null;

    public static void init(Context ctx){
        con = new Connection(ctx);
        con.discoverp();
    }

    public static void connectTo(String macAddress) throws Exception {
        con.stopServiceDiscovery();
        con.connect(macAddress);
    }

    public static boolean incommingConnectionRequestWaiting(){
        return incommingConnectionWaiting;
    }

    public static byte[] waitForReceive(String node){
        final long startTime = new Date().getTime();
        synchronized (wifiBuffer){
            while(new Date().getTime() < startTime + MainService.WIFI_RECEIVE_TIMEOUT) {
                if (wifiBuffer.containsKey(node)) {
                    return wifiBuffer.remove(node);
                }
                try {
                    wifiBuffer.wait(200);
                } catch(InterruptedException e){
                    e.printStackTrace();
                    return null;
                }
            }
            return null;
        }
    }

    public static void  receiveFrameFrom(byte[] frame, String macAddr) {
        mMacAddr = macAddr;
        synchronized(wifiBuffer) {
            if(wifiBuffer.containsKey(macAddr)){
                Log.d("WifiBackend", "frame from " + macAddr + " overwritten in Wifi Buffer");
            }
            wifiBuffer.put(macAddr, frame);
            wifiBuffer.notify();
        }
    }

    public static void send(byte[] frame) throws Exception {
        con.send(frame);
    }

    public static String getOtherMacAddress() {
        return mMacAddr;
    }

    public static void close() {
        con.closeConnection();
        con.startServiceDiscovery();
        mMacAddr = null;
        outGoingConnectionReady = false;
        incommingConnectionWaiting = false;
    }

    public static ArrayList<String> getNeighbourhood() {
        con.discoverp(); // QUESTIONABLE
        return con.updatepeer();
    }

    public static void incomingConnectionReceived(){
        if(incommingConnectionWaiting){
            Log.d("WifiBackend", "two incoming connections waiting at the same time, one is ignored");
        }
        incommingConnectionWaiting = true;
    }

}
