package com.example.manuel.wifimesh;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    public static final String SERVICE_TYPE = "_wdm_p2p._tcp";

    String SSID = "";
    MainActivity that = this;

    MainBCReceiver mBRReceiver;
    private IntentFilter filter;

    WifiServiceSearcher    mWifiServiceSearcher = null;
    WifiAccessPoint        mWifiAccessPoint = null;
    WifiConnection         mWifiConnection = null;
    Boolean serviceRunning = false;

    //change me  to be dynamic!!
    public String CLIENT_PORT_INSTANCE = "38000";
    public String SERVICE_PORT_INSTANCE = "38000";

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

                    ((TextView)findViewById(R.id.textView3)).append(readMessage);
                    break;

                case MY_HANDLE:
                    Object obj = msg.obj;
                    chat = (ChatManager) obj;

                    String helloBuffer = "Hello There from " +  chat.getSide() + " :" + Build.VERSION.SDK_INT + "Groupowner is " + SSID;

                    chat.write(helloBuffer.getBytes());
                    Log.d("","Wrote message: " + helloBuffer);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button1 = (Button) findViewById(R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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

                    // instantiate new AP and start it
                    mWifiAccessPoint = new WifiAccessPoint(that);
                    mWifiAccessPoint.Start();

                }
            }
        });

        Button button2 = (Button) findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(serviceRunning) { // stop all services to start anew, same as in button1
                    serviceRunning = false;
                    if(mWifiAccessPoint != null){
                        mWifiAccessPoint.Stop();
                        mWifiAccessPoint = null;
                    }

                    if(mWifiServiceSearcher != null){
                        mWifiServiceSearcher.Stop();
                        mWifiServiceSearcher = null;
                    }

                    if(mWifiConnection != null) {
                        mWifiConnection.Stop();
                        mWifiConnection = null;
                    }
                    Log.d("","Stopped");
                }else{
                    serviceRunning = true;
                    Log.d("","Started");

                    // start a new service searcher
                    mWifiServiceSearcher = new WifiServiceSearcher(that);
                    mWifiServiceSearcher.Start();
                }
            }
        });

        Button button3 = (Button) findViewById(R.id.button3);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mWifiConnection == null) {
                    if(mWifiAccessPoint != null){
                        mWifiAccessPoint.Stop();
                        mWifiAccessPoint = null;
                    }
                    if(mWifiServiceSearcher != null){
                        mWifiServiceSearcher.Stop();
                        mWifiServiceSearcher = null;
                    }

                    final String networkSSID = separated[1];
                    final String networkPass = separated[2];
                    final String ipAddress   = separated[3];

                    Log.d("Connection ", "Try to connect............." + ipAddress);
                    mWifiConnection = new WifiConnection(that,networkSSID,networkPass);
                    mWifiConnection.SetInetAddress(ipAddress);
                }
            }
        });

        Button button4 = (Button) findViewById(R.id.button4);
        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
                String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
                Log.d("Sender ip is:", ip);
                //if(mWifiConnection.GetInetAddress() == ip){
                if (mWifiConnection == null) {
                    Log.d("status", "................group owner");
                    chat = new ChatManager(groupSocket.getSocket(), myHandler, "Group!!!!!");
                    new Thread(chat).start();
                } else {
                    chat = new ChatManager(clientSocket.getSocket(), myHandler, "Client!!!!");
                    new Thread(chat).start();
                }
                /*if(clientSocket != null && clientSocket.getSocket() != null) {
                    chat = new ChatManager(clientSocket.getSocket(), myHandler, "Client!!!!");
                    new Thread(chat).start();
                }
                if(groupSocket != null && groupSocket.getSocket() != null) {
                    chat = new ChatManager(groupSocket.getSocket(), myHandler, "Group!!!!!");
                    new Thread(chat).start();
                }
                mWifiConnection.*/
            }
        });


        Button button5 = (Button) findViewById(R.id.button5);
        button5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mWifiConnection != null) {
                    mWifiConnection.Stop();
                }
                if(mWifiAccessPoint != null){
                    mWifiAccessPoint.Stop();
                }

                if(mWifiServiceSearcher != null){
                    mWifiServiceSearcher.Stop();
                }
                if(clientSocket != null) {
                    try {
                        clientSocket.close_socket();
                        Log.d("Closing", "ClientSocket closed");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if(groupSocket != null) {
                    try {
                        groupSocket.close_socket();
                        Log.d("Closing", "ServerSocket closed");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                Log.d("Closing", "  COMPLETE!!");
            }
        });

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


        LocalBroadcastManager.getInstance(this).registerReceiver((mBRReceiver), filter);

        try{
            groupSocket = new GroupOwnerSocketHandler(myHandler,Integer.parseInt(SERVICE_PORT_INSTANCE),that);
            groupSocket.start();
            Log.d("","Group socketserver started.");
        }catch (Exception e){
            Log.d("", "groupseocket error, :" + e.toString());
        }
    }

    @Override
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

    private class MainBCReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (WifiAccessPoint.DSS_WIFIAP_VALUES.equals(action)) {
                String s = intent.getStringExtra(WifiAccessPoint.DSS_WIFIAP_MESSAGE);
                Log.d("AP", s);

            }else if (WifiAccessPoint.DSS_WIFIAP_SERVERADDRESS.equals(action)) {
                InetAddress address = (InetAddress)intent.getSerializableExtra(WifiAccessPoint.DSS_WIFIAP_INETADDRESS);
                Log.d("AP", "inet address" + address.getHostAddress());

            }else if (WifiServiceSearcher.DSS_WIFISS_VALUES.equals(action)) {
                String s = intent.getStringExtra(WifiServiceSearcher.DSS_WIFISS_MESSAGE);
                Log.d("SS", s);

            }else if (WifiServiceSearcher.DSS_WIFISS_PEERCOUNT.equals(action)) {
                int s = intent.getIntExtra(WifiServiceSearcher.DSS_WIFISS_COUNT, -1);
                Log.d("SS", "found " + s + " peers");
                Log.d("", s+ " peers discovered.");

            }else if (WifiServiceSearcher.DSS_WIFISS_PEERAPINFO.equals(action)) {
                String s = intent.getStringExtra(WifiServiceSearcher.DSS_WIFISS_INFOTEXT);

                separated = s.split(":");
                Log.d("SS", "found SSID:" + separated[1] + ", pwd:"  + separated[2]+ "IP: " + separated[3]);
                ((TextView) findViewById(R.id.textView2)).setText("found SSID:" + separated[1] + ", pwd:"  + separated[2]);
                SSID = separated[1];

            }else if (WifiConnection.DSS_WIFICON_VALUES.equals(action)) {
                String s = intent.getStringExtra(WifiConnection.DSS_WIFICON_MESSAGE);
                Log.d("CON", s);

            }else if (WifiConnection.DSS_WIFICON_SERVERADDRESS.equals(action)) {
                int addr = intent.getIntExtra(WifiConnection.DSS_WIFICON_INETADDRESS, -1);
                Log.d("COM", "IP" + Formatter.formatIpAddress(addr));

                if(clientSocket == null &&  mWifiConnection != null) {
                    String IpToConnect = mWifiConnection.GetInetAddress();
                    Log.d("","Starting client socket conenction to : " + IpToConnect);
                    clientSocket = new ClientSocketHandler(myHandler,IpToConnect, Integer.parseInt(CLIENT_PORT_INSTANCE), that);
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
                    mWifiAccessPoint = new WifiAccessPoint(that);
                    mWifiAccessPoint.Start();

                    if(mWifiServiceSearcher != null){
                        mWifiServiceSearcher.Stop();
                        mWifiServiceSearcher = null;
                    }

                    mWifiServiceSearcher = new WifiServiceSearcher(that);
                    mWifiServiceSearcher.Start();
                }

                Log.d("COM", "Status " + conStatus);
            }else if (ClientSocketHandler.DSS_CLIENT_VALUES.equals(action)) {
                String s = intent.getStringExtra(ClientSocketHandler.DSS_CLIENT_MESSAGE);
                Log.d("Client", s);

            }else if (GroupOwnerSocketHandler.DSS_GROUP_VALUES.equals(action)) {
                String s = intent.getStringExtra(GroupOwnerSocketHandler.DSS_GROUP_MESSAGE);
                Log.d("Group", s);

            }
        }
    }
}