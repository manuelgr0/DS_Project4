package ch.ethz.inf.vs.anicolus.test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;
    private ArrayList<WifiP2pDevice> peers_list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("öalskdjföalsdjföl", "laödkjfölasjd");

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);
        peers_list = new ArrayList<WifiP2pDevice>();

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        Button btn_search = (Button) findViewById(R.id.button1);
        btn_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("hallo     :    " , "button clicked");
                discoverPeers();
            }
        });

        Button btn_connect = (Button) findViewById(R.id.button2);
        btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("connecting", "connecting");
                connect_device();
            }
        });

        Button btn_send = (Button) findViewById(R.id.button3);
        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("------------", "------------------");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {
                            @Override
                            public void onConnectionInfoAvailable(WifiP2pInfo info) {
                                // InetAddress from WifiP2pInfo struct.
                                try {
                                    InetAddress groupOwnerAddress = InetAddress.getByName(info.groupOwnerAddress.getHostAddress());
                                } catch (UnknownHostException e) {
                                    e.printStackTrace();
                                }

                                Log.d("groupowner is     ", info.groupOwnerAddress.getHostAddress());

                                // After the group negotiation, we can determine the group owner.
                                if (info.groupFormed && info.isGroupOwner) {
                                    // Do whatever tasks are specific to the group owner.
                                    // One common case is creating a server thread and accepting
                                    // incoming connections.
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ServerAsyncTask server = new ServerAsyncTask(getBaseContext());
                                            Object res = server.doInBackground(new Object[1]);
                                            final TextView text = (TextView) findViewById(R.id.textView3);
                                            final String devices_result = text.getText().toString() + "\n" + res.toString();
                                            Handler h = text.getHandler();
                                            h.post(new Runnable() {
                                                @Override
                                                public void run() {

                                                    text.setText(devices_result);
                                                }
                                            });
                                        }
                                    }).start();

                                } else if (info.groupFormed) {
                                    // The other device acts as the client. In this case,
                                    // you'll want to create a client thread that connects to the group
                                    // owner.

                                    ClientTask client = new ClientTask(getBaseContext(), info.groupOwnerAddress.getHostAddress(), 8888);
                                    client.run();
                                }
                            }
                        });
                    }
                }).start();

            }
        });
    }

    void discoverPeers() {
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                //when successful
                //TODO
                TextView text = (TextView) findViewById(R.id.textView1);
                String devices_found = text.getText().toString() + "\n" + peers_list.toString();
                text.setText(devices_found);

            }

            @Override
            public void onFailure(int reasonCode) {
                //when there was a problem
                //TODO
            }
        });
    }

    void connect_device() {
        //obtain a peer from the WifiP2pDeviceList
        if (this.peers_list.size() == 0) {
            TextView research = (TextView) findViewById(R.id.textView2);
            research.setText("Found 0 device :'(");
            return;
        }
        WifiP2pDevice device = peers_list.get(0);
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        final String s = config.deviceAddress;
        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                //success logic
                TextView text = (TextView) findViewById(R.id.textView2);
                String devices_connected = text.getText().toString() + "\n" + s;
                text.setText(devices_connected);
            }

            @Override
            public void onFailure(int reason) {
                //failure logic
            }
        });
    }

    ArrayList<WifiP2pDevice> getPeersList() {
        return this.peers_list;
    }

    void setPeersList(ArrayList<WifiP2pDevice> a) {
        this.peers_list = a;
    }

    /* register the broadcast receiver with the intent values to be matched */
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
    }

    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }
}
