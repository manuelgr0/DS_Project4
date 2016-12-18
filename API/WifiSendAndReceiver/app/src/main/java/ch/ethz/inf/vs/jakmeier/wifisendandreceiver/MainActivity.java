package ch.ethz.inf.vs.jakmeier.wifisendandreceiver;

import android.net.wifi.p2p.WifiP2pManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    // TODO: maybe change that to something from us?
   // public static final String SERVICE_TYPE = "_wdm_p2p._tcp";
    public static final String HELLO_SERVICE = "hi.wdmfapi._tcp";
    public static final String PLEASE_CONNECT = "connect.wdmfapi._tcp";

    private WifiAccessPoint mWifiAccessPoint;
    private WifiServiceSearcher mWifiServiceSearcher;

//    public NetworkNameService mNNS;

    public WifiP2pManager p2p;
    public  WifiP2pManager.Channel channel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        updateAp(false);

        // Init P2P manager
        p2p = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
        mWifiServiceSearcher = new WifiServiceSearcher(this);
        if (p2p == null) {
            Log.d("Main", "This device does not support Wi-Fi Direct");
        }else {
            channel = p2p.initialize(this, getMainLooper(), mWifiServiceSearcher);
            mWifiServiceSearcher.setChannel(channel);
        }

            // LISTENING NETWORK STUFF
        Button serviceBtn = (Button) findViewById(R.id.discover_services);
        serviceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWifiServiceSearcher.Start();
            }
        });

        // JOINING NETWORK STUFF
 /*       mNNS = new NetworkNameService("Test network tag", "11:ff:dd:22:22:ee", this);
        final Button joinBtn = (Button) findViewById(R.id.join_button);
        joinBtn.setOnClickListener(new View.OnClickListener() {
            boolean on = false;
            @Override
            public void onClick(View v) {
                if(on) {
                    mNNS.stop();
                    joinBtn.setText("Join Network");
                }
                else{
                    mNNS.start();
                    joinBtn.setText("Leave network");
                    clearNetworkDisplay();
                }
                on = !on;
            }
        });*/

        // INITIATE CONNECTION STUFF
        mWifiAccessPoint = new WifiAccessPoint(this);
        final Button connectBtn = (Button) findViewById(R.id.connect);
        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mWifiServiceSearcher != null) {
                    synchronized (mWifiServiceSearcher) {
                        String neighbour = mWifiServiceSearcher.availableNodes.get(0);
                        mWifiAccessPoint.Start(neighbour);
                    }
                    // open server socket
                    mWifiServiceSearcher.Stop();
//                    mNNS.stop();
                    new DataExchanger(MainActivity.this).openServer();
                }
            }
        });

    }

    public void updateApDisplay(boolean on, String name, String pw, String owner,  int nodes){
        TextView apDisplay = (TextView)findViewById(R.id.ap_display);
        if(on) {
            apDisplay.setText(
                    "Wifi Group is online and connected to " + nodes + " devices.\n" +
                            "Name: " + name +
                            "\nPassword: " + pw +
                            "\nOwner: " + owner
            );
        }else{
            apDisplay.setText("Wifi group is offline.");
        }
    }

    public void setApDisplayText(String text){
        ((TextView)findViewById(R.id.ap_display)).setText(text);
    }

    public void updateAp(boolean b) {
        /*Button apBtn = (Button) findViewById(R.id.access_point);
        if(b){
            apBtn.setText("Stop AP");
            apBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mWifiAccessPoint.Stop();
                    updateAp(false);
                }
            });
        }
        else {
            apBtn.setText("Start AP");
            apBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // instantiate new AP and start it

                    //mWifiAccessPoint.Start();
                }
            });
        }*/
    }

    public void updateServiceDisplay(WifiServiceSearcher.ServiceState state){
        TextView display = (TextView)findViewById(R.id.service_display);
        display.setText("Service discovery state: " + state);
    }

    public void addNodeToNetworkDisplay(String node){
        TextView display = (TextView)findViewById(R.id.network_display);
        display.append("\n: " + node);
    }

    public void clearNetworkDisplay(){
        TextView display = (TextView)findViewById(R.id.network_display);
        display.setText("Nodes in the network:");
        display.setMovementMethod(new ScrollingMovementMethod());
    }
}
