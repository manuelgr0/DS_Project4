package com.example.manuel.wifimesh;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;

import static android.content.Context.WIFI_SERVICE;

/**
 * Created by manue on 18.12.2016.
 */

public class Connection {




    public static final String SERVICE_TYPE = "_wdm_p2p._tcp";

    String SSID = "";
    Connection that = this;
    Context context;

    Connection.MainBCReceiver mBRReceiver;
    private IntentFilter filter;

    WifiServiceSearcher    mWifiServiceSearcher = null;
    WifiAccessPoint        mWifiAccessPoint = null;
    WifiConnection         mWifiConnection = null;
    Boolean serviceRunning = false;

    private String serverIp;
    private String macAddress;

    //change me  to be dynamic!!
    public String CLIENT_PORT_INSTANCE = "38080";
    public String SERVICE_PORT_INSTANCE = "38080";

    public static final int MESSAGE_READ = 0x400 + 1;
    public static final int MY_HANDLE = 0x400 + 2;

    String[] separated;

    GroupOwnerSocketHandler  groupSocket = null;
    ClientSocketHandler clientSocket = null;
    ChatManager chat = null;
    Handler myHandler  = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_READ:

                    byte[] readBuf = (byte[]) msg.obj;

                    String  readMessage = new String(readBuf, 0, msg.arg1);

                    Log.d("","Got message: " + readMessage);

                    break;

                case MY_HANDLE:
                    Object obj = msg.obj;
                    chat = (ChatManager) obj;
                    if (chat.type == 0) {
                        String helloBuffer = "Hello There from " +  chat.getSide() + " :" + Build.VERSION.SDK_INT + "Groupowner is " + SSID;

                        chat.write(helloBuffer.getBytes());
                    } else
                        chat.write(chat.getMsg());
            }
        }
    };

    public Connection(Context context) {

        this.context = context;
        WifiManager wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
        wifiManager.setWifiEnabled(false);

        wifiManager.setWifiEnabled(true);

        mBRReceiver = new MainBCReceiver();
        filter = new IntentFilter();
        filter.addAction(WifiAccessPoint.DSS_WIFIAP_VALUES);
        filter.addAction(WifiAccessPoint.DSS_WIFIAP_SERVERADDRESS);
        filter.addAction(WifiServiceSearcher.DSS_WIFISS_PEERAPINFO);
        filter.addAction(WifiServiceSearcher.DSS_WIFISS_PEERCOUNT);
        filter.addAction(WifiServiceSearcher.DSS_WIFISS_VALUES);
        filter.addAction(WifiConnection.DSS_WIFICON_VALUES);
        filter.addAction(WifiConnection.DSS_WIFICON_STATUSVAL);
        filter.addAction(WifiConnection.DSS_WIFICON_SERVERADDRESS);
        filter.addAction(ClientSocketHandler.DSS_CLIENT_VALUES);
        filter.addAction(GroupOwnerSocketHandler.DSS_GROUP_VALUES);


        LocalBroadcastManager.getInstance(context).registerReceiver((mBRReceiver), filter);
    }

    public void send(byte[] msg) {
        WifiManager wm = (WifiManager) context.getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        Log.d("Sender ip is:", ip);
        //if(mWifiConnection.GetInetAddress() == ip){
        if (mWifiConnection == null) {
            Log.d("status", "................group owner");
            //chat = new ChatManager(groupSocket.s, myHandler, "Group!!!!!");
            chat = new ChatManager(groupSocket.s, myHandler, msg);
            new Thread(chat).start();
        } else {
            //chat = new ChatManager(clientSocket.socket, myHandler, "Client!!!!");
            chat = new ChatManager(clientSocket.socket, myHandler, msg);
            new Thread(chat).start();
        }
    }

    public void connect(String macAddress) {
        this.macAddress = macAddress;
        try{
            groupSocket = new GroupOwnerSocketHandler(myHandler,Integer.parseInt(SERVICE_PORT_INSTANCE),context);
            groupSocket.start();
            Log.d("","Group socketserver started.");
        }catch (Exception e){
            Log.d("", "groupseocket error, :" + e.toString());
        }

        if(serviceRunning) { // stop all services to start anew
            serviceRunning = false;
            if(mWifiAccessPoint != null){ // AP already active
                mWifiAccessPoint.Stop();
                mWifiAccessPoint = null;
            }

            if(mWifiServiceSearcher != null){ // searcher active
                mWifiServiceSearcher.Stop(); // stop searcher (we're AP now)
                mWifiServiceSearcher = null;
            }

            if(mWifiConnection != null) { // already open connection
                mWifiConnection.Stop(); // close connection
                mWifiConnection = null;
            }
            Log.d("","Stopped");
        }else{
            serviceRunning = true;
            Log.d("","Started");

            WifiManager wifi = (WifiManager) context.getSystemService(WIFI_SERVICE);
            wifi.disconnect();

            WifiManager wm = (WifiManager) context.getSystemService(WIFI_SERVICE);
            List<WifiConfiguration> list = wm.getConfiguredNetworks();
            for( WifiConfiguration i : list ) {
                wm.removeNetwork(i.networkId);
                wm.saveConfiguration();
            }

            // instantiate new AP and start it
            mWifiAccessPoint = new WifiAccessPoint(context, this.macAddress);
            mWifiAccessPoint.Start();

        }
    }

    public void startServiceDiscovery() {
        Log.d("", "Started");

        // start a new service searcher
        mWifiServiceSearcher = new WifiServiceSearcher(context);
        mWifiServiceSearcher.Start();
    }



        /*Button button5 = (Button) findViewById(R.id.button5);
        button5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mWifiConnection != null) {
                    mWifiConnection.Stop();
                    mWifiConnection = null;
                }
                if(mWifiAccessPoint != null){
                    mWifiAccessPoint.Stop();
                    mWifiAccessPoint = null;
                }

                if(mWifiServiceSearcher != null){
                    mWifiServiceSearcher.Stop();
                    mWifiServiceSearcher = null;
                }
                if(clientSocket != null) {
                    try {
                        clientSocket.close_socket();
                        clientSocket = null;
                        Log.d("Closing", "ClientSocket closed");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if(groupSocket != null) {
                    try {
                        groupSocket.close_socket();
                        groupSocket = null;
                        Log.d("Closing", "ServerSocket closed");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                Log.d("Closing", "  COMPLETE!!");
            }
        });



    public void onDestroy() {
        super.onDestroy();
        if(mWifiConnection != null) {
            mWifiConnection.Stop();
            mWifiConnection = null;
        }
        if(mWifiAccessPoint != null){
            mWifiAccessPoint.Stop();
            mWifiAccessPoint = null;
        }

        if(mWifiServiceSearcher != null){
            mWifiServiceSearcher.Stop();
            mWifiServiceSearcher = null;
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBRReceiver);
    }

    */

    private class MainBCReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (WifiServiceSearcher.DSS_WIFISS_PEERAPINFO.equals(action)) {
                String s = intent.getStringExtra(WifiServiceSearcher.DSS_WIFISS_INFOTEXT);
                Log.d("aödlfjaölskd.......", s);
                separated = s.split(":");
                Log.d("SS", "found SSID:" + separated[6] + ", pwd:"  + separated[7]+ "IP: " + separated[8]);
                SSID = separated[6];

                if(mWifiConnection == null) {
                    if(mWifiAccessPoint != null){
                        mWifiAccessPoint.Stop();
                        mWifiAccessPoint = null;
                    }
                    if(mWifiServiceSearcher != null){
                        mWifiServiceSearcher.Stop();
                        mWifiServiceSearcher = null;
                    }

                    final String networkSSID = separated[6];
                    final String networkPass = separated[7];
                    final String ipAddress   = separated[8];
                    final String mMACAddress = separated[0] + ":" + separated[1] + ":" + separated[2] + ":" + separated[3] + ":" + separated[4] + ":" + separated[5];

                    WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                    wifi.disconnect();

                    WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                    List<WifiConfiguration> list = wm.getConfiguredNetworks();
                    for( WifiConfiguration i : list ) {
                        wm.removeNetwork(i.networkId);
                        wm.saveConfiguration();
                    }

                    WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wInfo = wifiManager.getConnectionInfo();
                    String macAddress = wInfo.getMacAddress();

                    Log.d("Connection ", "Try to connect............." + ipAddress);
                    Log.d("m1", mMACAddress);
                    Log.d("eingabe", macAddress);
                    if(mMACAddress.equals(macAddress)) {
                        Log.d("Right MAC    ", "YAAAAAAAAYYYYYY");
                        mWifiConnection = new WifiConnection(context, networkSSID, networkPass);
                        mWifiConnection.SetInetAddress(ipAddress);
                    } else {
                        Log.d("WRRRROOOOOONNNG   ", "MAAAAAC");
                    }
                }


            }else if (WifiConnection.DSS_WIFICON_SERVERADDRESS.equals(action)) {
                int addr = intent.getIntExtra(WifiConnection.DSS_WIFICON_INETADDRESS, -1);
                Log.d("COM", "IP" + Formatter.formatIpAddress(addr));
                serverIp = Formatter.formatIpAddress(addr);
                String IpToConnect = mWifiConnection.GetInetAddress();
                if(clientSocket == null &&  mWifiConnection != null) {
                    //String IpToConnect = mWifiConnection.GetInetAddress();
                    clientSocket = new ClientSocketHandler(myHandler, IpToConnect, Integer.parseInt(CLIENT_PORT_INSTANCE), context);
                    clientSocket.start();
                }
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
                        clientSocket = null;
                    }
                    // make sure services are re-started
                    if(mWifiAccessPoint != null){
                        mWifiAccessPoint.Stop();
                        mWifiAccessPoint = null;
                    }
                    mWifiAccessPoint = new WifiAccessPoint(context, macAddress);
                    mWifiAccessPoint.Start();

                    if(mWifiServiceSearcher != null){
                        mWifiServiceSearcher.Stop();
                        mWifiServiceSearcher = null;
                    }

                    //mWifiServiceSearcher = new WifiServiceSearcher(that);
                    //mWifiServiceSearcher.Start();
                }

                Log.d("COM", "Status " + conStatus);
            }
        }
    }
}
