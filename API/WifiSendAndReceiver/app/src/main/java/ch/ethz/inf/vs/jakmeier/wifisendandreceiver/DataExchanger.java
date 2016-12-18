package ch.ethz.inf.vs.jakmeier.wifisendandreceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.format.Formatter;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Jakob on 17.12.2016.
 */

public class DataExchanger extends BroadcastReceiver {
    private enum Role {None, Server, Client};
    private Role role = Role.None;
    private ServerSocket serverSocket;

    // private WifiServiceSearcher    mWifiServiceSearcher = null;
    private WifiAccessPoint        mWifiAccessPoint = null;
    private WifiConnection         mWifiConnection = null;
    private Socket socket = null;
    private boolean connected = false;

    private MainActivity mainActivity;

    private final static String TAG = "Data Handler";

    public DataExchanger(MainActivity mainActivity){
        this.mainActivity = mainActivity;
    }

    public void connectToAP(final String ip, String SSID, String pw) {
        if(socket != null){
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        role = Role.Client;
        if (mWifiConnection == null) {
            if (mWifiAccessPoint != null) {
                mWifiAccessPoint.Stop();
                mWifiAccessPoint = null;
            }
            /* Moved that part to WifiServiceSearcher
            if (mWifiServiceSearcher != null) {
                mWifiServiceSearcher.Stop();
                mWifiServiceSearcher = null;
            }*/
            Log.d("Connection ", "Try to connect............." + ip);
            mWifiConnection = new WifiConnection(mainActivity, SSID, pw, ip);
//            mWifiConnection.SetInetAddress(ip);

            // connect to server socket now (in a separate thread for IO because we are bound to UI in the teset app)
            new Thread() {
                public void run() {
                    try {
                        socket = new Socket();
                        // 16s timeout
                        socket.connect(new InetSocketAddress(ip, 9100), 16000);
                        onSuccessfulConnect();
                    }
                    catch (IOException e) {
                        Log.d(TAG, "IO exception on client side");
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }

    // Opens a server socket
    public void openServer(){
        if (serverSocket == null){
            try {
                serverSocket = new ServerSocket(9100);

               // serverSocket.setSoTimeout(16000);
                serverSocket.setReuseAddress(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(socket != null){
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        role = Role.Server;

        Log.d(TAG, "Waiting for client...");
        mainActivity.setApDisplayText("Open server socket. Waiting for client...");

        new Thread() {
            public void run() {
                try {
                    Log.d(TAG, "Server socket IP:" + serverSocket.getInetAddress());
                    socket = serverSocket.accept();
                    //Log.d(TAG, "A new client Connected!");
                    onSuccessfulConnect();
                }
                catch (IOException e) {
                    Log.d(TAG, "IO exception on server side");
                    e.printStackTrace();
                }
            }
        }.start();
    }
    public void onSuccessfulConnect(){
        if(role == Role.Client) {
            Log.d(TAG, "Connected to server");
            //mainActivity.setApDisplayText("Connected to client");
        }
        else {
            Log.d(TAG,"Connected to client" );
            //mainActivity.setApDisplayText("Connected to server");
        }
        connected = true;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiAccessPoint.DSS_WIFIAP_SERVERADDRESS.equals(action)) {
            InetAddress address = (InetAddress)intent.getSerializableExtra(WifiAccessPoint.DSS_WIFIAP_INETADDRESS);
            Log.d("AP", "inet address" + address.getHostAddress());

        }else if (WifiServiceSearcher.DSS_WIFISS_PEERAPINFO.equals(action)) {
            /*String s = intent.getStringExtra(WifiServiceSearcher.DSS_WIFISS_INFOTEXT);

            separated = s.split(":");
            Log.d("SS", "found SSID:" + separated[1] + ", pwd:"  + separated[2]+ "IP: " + separated[3]);
            ((TextView) findViewById(R.id.textView2)).setText("found SSID:" + separated[1] + ", pwd:"  + separated[2]);
            SSID = separated[1];*/

        }else if (WifiConnection.DSS_WIFICON_VALUES.equals(action)) {
            String s = intent.getStringExtra(WifiConnection.DSS_WIFICON_MESSAGE);
            Log.d("CON", s);

        }else if (WifiConnection.DSS_WIFICON_SERVERADDRESS.equals(action)) {
            int addr = intent.getIntExtra(WifiConnection.DSS_WIFICON_INETADDRESS, -1);
            mainActivity.setApDisplayText("I should try to connect to the AP now. IP" + Formatter.formatIpAddress(addr));
            Log.d(TAG, "I should try to connect to the AP now. IP" + Formatter.formatIpAddress(addr));
            /*serverIp = Formatter.formatIpAddress(addr);
            if(clientSocket == null &&  mWifiConnection != null) {
                //String IpToConnect = mWifiConnection.GetInetAddress();
                Log.d("","Starting client socket conenction to : " + serverIp);
                clientSocket = new ClientSocketHandler(myHandler,serverIp, Integer.parseInt(CLIENT_PORT_INSTANCE), that);
                clientSocket.start();
            }*/
        }else if (WifiConnection.DSS_WIFICON_STATUSVAL.equals(action)) {
            int status = intent.getIntExtra(WifiConnection.DSS_WIFICON_CONSTATUS, -1);

            String conStatus = "";
            if(status == WifiConnection.ConectionStateNONE) {
                conStatus = "NONE";
            }else if(status == WifiConnection.ConectionStatePreConnecting) {
                conStatus = "PreConnecting";
            }else if(status == WifiConnection.ConectionStateConnecting) {
                conStatus = "Connecting";
                Log.d("", "Accesspoint connected");
            }else if(status == WifiConnection.ConectionStateConnected) {
                conStatus = "Connected";
            }else if(status == WifiConnection.ConectionStateDisconnected) {
                conStatus = "Disconnected";
                Log.d("", "Accesspoint Disconnected");
                if(mWifiConnection != null) {
                    mWifiConnection.Stop();
                    mWifiConnection = null;
                    // should stop etc.
//                    clientSocket = null;
                }
                // make sure services are re-started
/*                if(mWifiAccessPoint != null){
                    mWifiAccessPoint.Stop();
                    mWifiAccessPoint = null;
                }
                mWifiAccessPoint = new WifiAccessPoint(that);
                mWifiAccessPoint.Start();

                if(mWifiServiceSearcher != null){
                    mWifiServiceSearcher.Stop();
                    mWifiServiceSearcher = null;
                }

                mWifiServiceSearcher = new WifiServiceSearcher(that);
                mWifiServiceSearcher.Start();*/
            }

            Log.d("COM", "Status " + conStatus);
        }/*else if (ClientSocketHandler.DSS_CLIENT_VALUES.equals(action)) {
            String s = intent.getStringExtra(ClientSocketHandler.DSS_CLIENT_MESSAGE);
            Log.d("Client", s);

        }else if (GroupOwnerSocketHandler.DSS_GROUP_VALUES.equals(action)) {
            String s = intent.getStringExtra(GroupOwnerSocketHandler.DSS_GROUP_MESSAGE);
            Log.d("Group", s);

        }*/
        else{
            Log.d(TAG, "on Receive of BoradcastReceiver just got: " + intent.toString());
        }
    }
}
