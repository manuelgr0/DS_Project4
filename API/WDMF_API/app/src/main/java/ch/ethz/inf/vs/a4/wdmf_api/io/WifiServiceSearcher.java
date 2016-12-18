package ch.ethz.inf.vs.a4.wdmf_api.io;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION;


public class WifiServiceSearcher  implements WifiP2pManager.ChannelListener{

    String TAG = "WifiServiceSearcher";

    static final public String DSS_WIFISS_VALUES = "test.microsoft.com.mywifimesh.DSS_WIFISS_VALUES";
    static final public String DSS_WIFISS_MESSAGE = "test.microsoft.com.mywifimesh.DSS_WIFISS_MESSAGE";

    static final public String DSS_WIFISS_PEERCOUNT = "test.microsoft.com.mywifimesh.DSS_WIFISS_PEERCOUNT";
    static final public String DSS_WIFISS_COUNT = "test.microsoft.com.mywifimesh.DSS_WIFISS_COUNT";

    static final public String DSS_WIFISS_PEERAPINFO = "test.microsoft.com.mywifimesh.DSS_WIFISS_PEERAPINFO";
    static final public String DSS_WIFISS_INFOTEXT = "test.microsoft.com.mywifimesh.DSS_WIFISS_INFOTEXT";

    WifiServiceSearcher that = this;
    LocalBroadcastManager broadcaster = null;
    Context context = null;

    private BroadcastReceiver receiver = null;
    private IntentFilter filter = null;

    private WifiP2pManager p2p = null;
    private WifiP2pManager.Channel channel = null;
    private WifiP2pManager.DnsSdServiceResponseListener serviceListener = null;
    private WifiP2pManager.PeerListListener peerListListener = null;

    enum ServiceState{
        NONE,
        DiscoverPeer,
        DiscoverService
    }
    ServiceState myServiceState = ServiceState.NONE;


    public WifiServiceSearcher(Context Context) {
        this.context = Context;
        this.broadcaster = LocalBroadcastManager.getInstance(this.context);
    }


    public void Start() {

        p2p = (WifiP2pManager) this.context.getSystemService(this.context.WIFI_P2P_SERVICE);
        if (p2p == null) {
            Log.d(TAG, "This device does not support Wi-Fi Direct");
        }else {

            channel = p2p.initialize(this.context, this.context.getMainLooper(), this);

            receiver = new ServiceSearcherReceiver();
            filter = new IntentFilter();
            filter.addAction(WIFI_P2P_STATE_CHANGED_ACTION);
            filter.addAction(WIFI_P2P_CONNECTION_CHANGED_ACTION);
            filter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
            filter.addAction(WIFI_P2P_PEERS_CHANGED_ACTION);

            this.context.registerReceiver(receiver, filter);

            peerListListener = new WifiP2pManager.PeerListListener() {

                public void onPeersAvailable(WifiP2pDeviceList peers) {

                    WifiP2pDeviceList pers = peers;
                    int numm = 0;
                    for (WifiP2pDevice peer : pers.getDeviceList()) {
                        numm++;
                        Log.d(TAG, "\t" + numm + ": "  + peer.deviceName + " " + peer.deviceAddress);
                    }

                    if(broadcaster != null) {
                        Intent intent = new Intent(DSS_WIFISS_PEERCOUNT);
                        intent.putExtra(DSS_WIFISS_COUNT, numm);
                        broadcaster.sendBroadcast(intent);
                    }

                    if(numm > 0){
                        startServiceDiscovery();
                    }else{
                        startPeerDiscovery();
                    }
                }
            };

            serviceListener = new WifiP2pManager.DnsSdServiceResponseListener() {

                public void onDnsSdServiceAvailable(String instanceName, String serviceType, WifiP2pDevice device) {

                    if (serviceType.startsWith(Connection.SERVICE_TYPE)) {

                        //instance name has AP information for Client connection
                        if(broadcaster != null) {
                            Intent intent = new Intent(DSS_WIFISS_PEERAPINFO);
                            intent.putExtra(DSS_WIFISS_INFOTEXT, instanceName);
                            broadcaster.sendBroadcast(intent);
                        }


                    } else {
                        Log.d(TAG, "Not our Service, :" + Connection.SERVICE_TYPE + "!=" + serviceType + ":");
                    }

                    startPeerDiscovery();
                }
            };

            p2p.setDnsSdResponseListeners(channel, serviceListener, null);
            //startPeerDiscovery();
        }
    }


    public void Stop() {
        this.context.unregisterReceiver(receiver);
        stopDiscovery();
        stopPeerDiscovery();
    }

    @Override
    public void onChannelDisconnected() {
        //
    }
    private void startPeerDiscovery() {
        myServiceState = ServiceState.NONE;
        p2p.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                myServiceState = ServiceState.DiscoverPeer;
                Log.d(TAG, "Started peer discovery");
            }
            public void onFailure(int reason) {Log.d(TAG, "Starting peer discovery failed, error code " + reason);}
        });
    }

    private void stopPeerDiscovery() {
        myServiceState = ServiceState.NONE;
        p2p.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {Log.d(TAG, "Stopped peer discovery");}
            public void onFailure(int reason) {Log.d(TAG, "Stopping peer discovery failed, error code " + reason);}
        });
    }

    private void startServiceDiscovery() {
        WifiP2pDnsSdServiceRequest request = WifiP2pDnsSdServiceRequest.newInstance(Connection.SERVICE_TYPE);
        final Handler handler = new Handler();
        p2p.addServiceRequest(channel, request, new WifiP2pManager.ActionListener() {

            public void onSuccess() {
                Log.d("Added service request", ".");
                handler.postDelayed(new Runnable() {
                    //There are supposedly a possible race-condition bug with the service discovery
                    // thus to avoid it, we are delaying the service discovery start here
                    public void run() {
                        p2p.discoverServices(channel, new WifiP2pManager.ActionListener() {
                            public void onSuccess() {
                                Log.d("Startedservicediscovery", ",");
                                myServiceState = ServiceState.DiscoverService;
                            }
                            public void onFailure(int reason) {Log.d("Starting service", " discovery failed, error code " + reason);}
                        });
                    }
                }, 1000);
            }

            public void onFailure(int reason) {
                Log.d("Adding service", " request failed, error code " + reason);
                // No point starting service discovery
            }
        });
    }

    private void stopDiscovery() {
        p2p.clearServiceRequests(channel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {Log.d(TAG, "Cleared service requests");}
            public void onFailure(int reason) {Log.d(TAG, "Clearing service requests failed, error code " + reason);}
        });
    }

    private class ServiceSearcherReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {

                } else {

                }
            }else if(WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                if(myServiceState == ServiceState.DiscoverPeer) {
                    Log.d("status", "ServiceState.DiscoverPeer");
                    p2p.requestPeers(channel, peerListListener);
                }
            } else if(WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                //WifiP2pDevice device = intent.getParcelableExtra(EXTRA_WIFI_P2P_DEVICE);
                //addText("Local device: " + MyP2PHelper.deviceToString(device));
            } else if(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action)) {

                int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED);
                String persTatu = "Discovery state changed to ";

                if(state == WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED){
                    persTatu = persTatu + "Stopped.";
                    Log.d("status", "WIFI_P2P_DISCOVERY_STOPPED");
                    startPeerDiscovery();
                }else if(state == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED){
                    persTatu = persTatu + "Started.";
                }else{
                    persTatu = persTatu + "unknown  " + state;
                }
                Log.d(TAG, persTatu);

            } else if (WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                if (networkInfo.isConnected()) {
                    Log.d(TAG, "Connected changed action");
                    //startPeerDiscovery();
                } else{
                    Log.d(TAG, "DIS-Connected not changed action");
                    startPeerDiscovery();
                }
            }
        }
    }
}
