package ch.ethz.inf.vs.a4.wdmf_api.ui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import ch.ethz.inf.vs.a4.wdmf_api.ipc_interface.WDMF_Connector;
import ch.ethz.inf.vs.a4.wdmf_api.R;
import ch.ethz.inf.vs.a4.wdmf_api.io.WiFiDirectBroadcastReceiver;

public class MainActivity extends AppCompatActivity implements WifiP2pManager.ConnectionInfoListener/*, MessageTarget*/ {

    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;
        private ArrayList<WifiP2pDevice> peers_list;

    public static final int SERVER_PORT = 4545;
    public static final int MESSAGE_READ = 0x400 + 1;
    public static final int MY_HANDLE = 0x400 + 2;
    private final WDMF_Connector connector = new WDMF_Connector(this, "Test App from Manu and Köbi") {
        @Override
        public void onReceiveMessage(byte[] msg) {
            // Do nothing for now, calling it is not implemented yet anyway
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);
        peers_list = new ArrayList<>();

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        Button btn_search = (Button) findViewById(R.id.search);
        btn_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                discoverPeers();
            }
        });
        Button btn_connect = (Button) findViewById(R.id.connect);
        btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectPeers();
            }
        });

        // SEND MSG TO SERVICE TESTER //

        final Button btn = (Button) findViewById(R.id.startService);
        btn.setOnClickListener(new View.OnClickListener() {
            boolean on = false;
            @Override
            public void onClick(View v) {
                if (on) {
                    stopWDMFAPI();
                    btn.setText("Start Service");
                } else {
                    startWDMFAPI();
                    btn.setText("Stop Service");
                }
                on = !on;
            }
        });

        Button btn_send1 = (Button) findViewById(R.id.send1);
        btn_send1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connector.broadcastMessage(new byte[]{1,2,3});
            }
        });
        Button btn_send2 = (Button) findViewById(R.id.send2);
        btn_send2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connector.broadcastMessage(new byte[]{-1,-2,-3});
                connector.setNetworkTag("New Name");
                Log.d("XXXX", "Buffer Size:"  + connector.get_buffer_size() + "KB");
            }
        });
        Button btn_settings = (Button) findViewById(R.id.settingsButton);
        btn_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(MainActivity.this, SettingsActivity.class);
                MainActivity.this.startActivity(myIntent);
            }
        });

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

    @Override
    protected void onStop() {
        if (mManager != null && mChannel != null) {
            mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onFailure(int reasonCode) {
                    Log.d("wifidirect:  ", "Disconnect failed. Reason :" + reasonCode);
                }
                @Override
                public void onSuccess() {
                }
            });
        }
        connector.disconnectFromWDMF();
        super.onStop();
    }

    @Override
    protected void onStart() {
        connector.connectToWDMF();
        super.onStart();
    }

    void discoverPeers() {
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                //when successful
                //TODO
            }

            @Override
            public void onFailure(int reasonCode) {
                //when there was a problem
                //TODO
            }
        });
    }

    void connectPeers() {
        //connects to every peers around
        if (this.peers_list.size() == 0) {
            TextView research = (TextView) findViewById(R.id.research);
            research.setText("Found 0 device :'(");
            return;
        }
        for (WifiP2pDevice device : this.peers_list) {
            connect_device(device);
        }
    }

    void connect_device(WifiP2pDevice device) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        final String s = config.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                //if it connected, we display the info of the device on the screen
                TextView text = (TextView) findViewById(R.id.device);
                String devices_connected = text.getText().toString() + "\n" + s;
                text.setText(devices_connected);
            }
            @Override
            public void onFailure(int reason) {
                //TODO: Retry after timeout?
                Toast.makeText(MainActivity.this, "Connection failed!", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {

        /*
         * The group owner accepts connections using a server socket and then spawns a
         * client socket for every client. This is handled by {@code
         * GroupOwnerSocketHandler}
         */

        Thread handler = null;

        // InetAddress from WifiP2pInfo struct.
        try {
            InetAddress groupOwnerAddress = InetAddress.getByName(info.groupOwnerAddress.getHostAddress());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        // After the group negotiation, we can determine the group owner.
       /* if (info.groupFormed && info.isGroupOwner) {
            // Do whatever tasks are specific to the group owner.
            // One common case is creating a server thread and accepting
            // incoming connections.
            Log.d("wifidirect:  ", "Connected as group owner");
            try {
                handler = new GroupOwnerSocketHandler(((MessageTarget) this).getHandler());
                handler.start();
            } catch (IOException e) {
                Log.d("wifidirect:  ", "Failed to create a server thread - " + e.getMessage());
                return;
            }
        } else if (info.groupFormed) {
            // The other device acts as the client. In this case,
            // you'll want to create a client thread that connects to the group
            // owner.
            Log.d("wifidirect:  ", "Connected as peer");
            handler = new ClientSocketHandler(((MessageTarget) this).getHandler(), p2pInfo.groupOwnerAddress);
            handler.start();
        }
        chatFragment = new WiFiChatFragment();
        getFragmentManager().beginTransaction().replace(R.id.container_root, chatFragment).commit();*/
    }

    ArrayList<WifiP2pDevice> getPeersList() {
        return this.peers_list;
    }

    public void setPeersList(ArrayList<WifiP2pDevice> a) {
        this.peers_list = a;
    }

    void startWDMFAPI(){
        Intent i = new Intent();
        i.setComponent(new ComponentName("ch.ethz.inf.vs.a4.wdmf_api", "ch.ethz.inf.vs.a4.wdmf_api.service.MainService"));

        //read preferences before starting the service /*this should happen in the service*/
       /* SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        String bufferSize = pref.getString(getResources().getString(R.string.pref_key_buffer_size),
                            (getResources().getString(R.string.pref_default_buffer_size)));
        String networkName = pref.getString(getResources().getString(R.string.pref_key_network_name),
                (getResources().getString(R.string.pref_default_network_name)));
        String timeout = pref.getString(getResources().getString(R.string.pref_key_timeout),
                (getResources().getString(R.string.pref_default_timeout)));*/

        // Here we trust that the preference stores a proper int string, which we ensure with the listener
        //i.putExtra("bufferSize", Long.valueOf(bufferSize));
        //i.putExtra("timeout", Integer.valueOf(timeout));
        //i.putExtra("networkName", networkName);

        startService(i);
    }

    void stopWDMFAPI(){
        Intent i = new Intent();
        i.setComponent(new ComponentName("ch.ethz.inf.vs.a4.wdmf_api", "ch.ethz.inf.vs.a4.wdmf_api.service.MainService"));
        stopService(i); //TODO: make sure it really stops!
    }

}
