package ch.ethz.inf.vs.jakmeier.wifisendandreceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION;


public class WifiServiceSearcher implements WifiP2pManager.ChannelListener{

    public ArrayList<String> availableNodes;

    String TAG = "WifiServiceSearcher";

    static final public String DSS_WIFISS_VALUES = "ch.ethz.inf.vs.jakmeier.wifisendandreceiver.DSS_WIFISS_VALUES";
    static final public String DSS_WIFISS_MESSAGE = "ch.ethz.inf.vs.jakmeier.wifisendandreceiver.DSS_WIFISS_MESSAGE";

    static final public String DSS_WIFISS_PEERCOUNT = "ch.ethz.inf.vs.jakmeier.wifisendandreceiver.DSS_WIFISS_PEERCOUNT";
    static final public String DSS_WIFISS_COUNT = "ch.ethz.inf.vs.jakmeier.wifisendandreceiver.DSS_WIFISS_COUNT";

    static final public String DSS_WIFISS_PEERAPINFO = "ch.ethz.inf.vs.jakmeier.wifisendandreceiver.DSS_WIFISS_PEERAPINFO";
    static final public String DSS_WIFISS_INFOTEXT = "ch.ethz.inf.vs.jakmeier.wifisendandreceiver.DSS_WIFISS_INFOTEXT";

    LocalBroadcastManager broadcaster;
    MainActivity mainActivity;
    String mMac = "MAC Address not initialized";

    private BroadcastReceiver receiver;
    private IntentFilter filter;

    private WifiP2pManager p2p;
    private  WifiP2pManager.Channel channel;
   // private WifiP2pManager.DnsSdServiceResponseListener serviceListener;
    private WifiP2pManager.PeerListListener peerListListener;

    private WifiP2pDnsSdServiceRequest mDnsSdServiceRequest;
    private Handler mServiceDiscoveryHandler;
    private boolean mServiceDiscoveryKeepGoing;
    private boolean mStarted = false;

    enum ServiceState{
        NONE,
        DiscoverPeer,
        DiscoverService
    }
    ServiceState myServiceState = ServiceState.NONE;


    public WifiServiceSearcher(MainActivity ma) {
        this.mainActivity = ma;
        this.broadcaster = LocalBroadcastManager.getInstance(this.mainActivity);
        p2p = mainActivity.p2p;
        //channel = mainActivity.channel;
    }
    public void setChannel(WifiP2pManager.Channel channel){
        this.channel = channel;
    }


    public void Start() {

        //p2p = (WifiP2pManager) this.mainActivity.getSystemService(this.mainActivity.WIFI_P2P_SERVICE);
        if (p2p == null) {
            Log.d(TAG, "This device does not support Wi-Fi Direct");
        }else {

            //channel = p2p.initialize(this.mainActivity, this.mainActivity.getMainLooper(), this);

            mStarted = true;
            receiver = new ServiceSearcherReceiver();
            filter = new IntentFilter();
            filter.addAction(WIFI_P2P_STATE_CHANGED_ACTION);
            filter.addAction(WIFI_P2P_CONNECTION_CHANGED_ACTION);
            filter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
            filter.addAction(WIFI_P2P_PEERS_CHANGED_ACTION);

            mainActivity.registerReceiver(receiver, filter);

            peerListListener = new WifiP2pManager.PeerListListener() {

                public void onPeersAvailable(WifiP2pDeviceList peers) {

                    final WifiP2pDeviceList pers = peers;
                    int numm = 0;
                    mainActivity.clearNetworkDisplay();
                    availableNodes = new ArrayList<>();
                    for (WifiP2pDevice peer : pers.getDeviceList()) {
                        numm++;
                        Log.d(TAG, "\t" + numm + ": "  + peer.deviceName + " " + peer.deviceAddress);
                        mainActivity.addNodeToNetworkDisplay(peer.deviceName + ": " + peer.deviceAddress);
                        availableNodes.add(peer.deviceAddress);
                    }

                    if(broadcaster != null) {
                        Intent intent = new Intent(DSS_WIFISS_PEERCOUNT);
                        intent.putExtra(DSS_WIFISS_COUNT, numm);
                        broadcaster.sendBroadcast(intent);
                    }

                    if(numm > 0){
                        Log.d(TAG, "start service Discovery");
                        startServiceDiscovery();
                    }else{
                        startPeerDiscovery();
                    }
                }
            };

            WifiP2pManager.DnsSdServiceResponseListener serviceListener;
            serviceListener = new WifiP2pManager.DnsSdServiceResponseListener() {

                public void onDnsSdServiceAvailable(String instanceName, String serviceType, WifiP2pDevice device) {

                    /*if (serviceType.startsWith(MainActivity.HELLO_SERVICE)) {

                        //instance name has AP information for Client connection
                        /*if(broadcaster != null) {
                            Intent intent = new Intent(DSS_WIFISS_PEERAPINFO);
                            intent.putExtra(DSS_WIFISS_INFOTEXT, instanceName);
                            broadcaster.sendBroadcast(intent);
                        }*//*
                        Log.d(TAG, "Hello from: " + instanceName + " -  Device: " + device.deviceName);

                    } else if (serviceType.startsWith(MainActivity.PLEASE_CONNECT)){
                        mainActivity.setApDisplayText("Connection request from :"  + instanceName + " -  Device: " + device.deviceName);
                        Log.d(TAG, "Connection request from :"  + instanceName + " -  Device: " + device.deviceName);
                        new DataExchanger(mainActivity).connectToAP();
                    } else {
                        Log.d(TAG, "Not our Service or Network, :" + serviceType + ":");
                    }*/

                    startPeerDiscovery();
                }
            };

            WifiP2pManager.DnsSdTxtRecordListener txtReportListener;
            txtReportListener = new WifiP2pManager.DnsSdTxtRecordListener() {
                @Override
                public void onDnsSdTxtRecordAvailable(String s, Map<String, String> map, WifiP2pDevice wifiP2pDevice) {
                    Log.d(TAG, "Received TXT record from " + wifiP2pDevice.deviceName +" --- " + s + "\n" + map.toString());
                    if(map.containsKey("for") && mStarted){
                        //if(map.get("for").equals(mMac)){
                            // We are asked to connect to an open AP of another node
                            String[] apInfo = s.split(":");
                            String ssid = apInfo[1];
                            String pw = apInfo[2];
                            String[] ipParts = apInfo[3].split("\\.");
                            if(ipParts.length >= 4) {
                                String ip = ipParts[0] + "." + ipParts[1] + "." + ipParts[2] + "." + ipParts[3];
                                Stop();
//                                mainActivity.mNNS.stop();
                                new DataExchanger(mainActivity).connectToAP(ip, ssid, pw);
                            } else{
                                Log.d(TAG, "ip parts wrong: " + apInfo[3]);
                            }

                        //}
                    }
                }
            };

            p2p.setDnsSdResponseListeners(channel, serviceListener, txtReportListener);
            startPeerDiscovery();
        }
    }


    public void Stop() {
        if(mStarted) {
            this.mainActivity.unregisterReceiver(receiver);
            stopDiscovery();
            stopPeerDiscovery();
            mStarted = false;
        }
    }

    @Override
    public void onChannelDisconnected() {
        //Stop();
        //
    }
    private void startPeerDiscovery() {
        p2p.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                myServiceState = ServiceState.DiscoverPeer;
                mainActivity.updateServiceDisplay(myServiceState);
                Log.d(TAG, "Started peer discovery");
            }
            public void onFailure(int reason) {
                Log.d(TAG, "Starting peer discovery failed, error code " + reason);}
        });
    }

    private void stopPeerDiscovery() {
        p2p.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                Log.d(TAG, "Stopped peer discovery");}
            public void onFailure(int reason) {
                Log.d(TAG, "Stopping peer discovery failed, error code " + reason);}
        });
    }

    private void startServiceDiscovery() {

        mDnsSdServiceRequest = WifiP2pDnsSdServiceRequest.newInstance();//MainActivity.PLEASE_CONNECT);
        mServiceDiscoveryHandler = new Handler();
        mServiceDiscoveryKeepGoing = true;
        repeatingServiceDiscoveryPart();

    }

    private void repeatingServiceDiscoveryPart() {
        p2p.clearServiceRequests(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                //Log.d(TAG, "Cleared service requests");
                p2p.addServiceRequest(channel, mDnsSdServiceRequest, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        //Log.d(TAG, "Added service request");

                        p2p.discoverServices(channel, new WifiP2pManager.ActionListener() {
                            public void onSuccess() {
                                //Log.d(TAG, "Started service discovery, will restart after a delay");
                                myServiceState = ServiceState.DiscoverService;
                                mainActivity.updateServiceDisplay(myServiceState);

                                // Call this function again after a delay
                                mServiceDiscoveryHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {

                                        // Check if stopDiscovery has been called in the meantime
                                        if (mServiceDiscoveryKeepGoing) {
                                            repeatingServiceDiscoveryPart();
                                        }
                                    }
                                }, 5000L);
                            }

                            public void onFailure(int reason) {
                                Log.d(TAG, "Starting service discovery failed, error code " + reason);
                                mServiceDiscoveryHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {

                                        // Check if stopDiscovery has been called in the meantime
                                        if (mServiceDiscoveryKeepGoing) {
                                            repeatingServiceDiscoveryPart();
                                        }
                                    }
                                }, 5000L);
                            }
                        });
                    }

                    public void onFailure(int reason) {
                        Log.d(TAG, "Adding service request failed, error code " + reason);
                        // No point starting service discovery
                        mServiceDiscoveryHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {

                                // Check if stopDiscovery has been called in the meantime
                                if (mServiceDiscoveryKeepGoing) {
                                    repeatingServiceDiscoveryPart();
                                }
                            }
                        }, 5000L);
                    }
                });
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "Adding service request failed, error code " + reason);
                mServiceDiscoveryHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        // Check if stopDiscovery has been called in the meantime
                        if (mServiceDiscoveryKeepGoing) {
                            repeatingServiceDiscoveryPart();
                        }
                    }
                }, 5000L);
            }
        });
    }

    private void stopDiscovery() {
        mServiceDiscoveryKeepGoing = false;
        p2p.clearServiceRequests(channel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                Log.d(TAG, "Cleared service requests");}
            public void onFailure(int reason) {
                Log.d(TAG, "Clearing service requests failed, error code " + reason);}
        });
    }

    private class ServiceSearcherReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    Log.d(TAG, "wifi p2p state enabled");
                } else {
                    Log.d(TAG, "wifi p2p state has changed to " + state);
                }
            }else if(WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                if(myServiceState != ServiceState.DiscoverService) {
                    p2p.requestPeers(channel, peerListListener);
                }
            } else if(WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                //WifiP2pDevice device = intent.getParcelableExtra(EXTRA_WIFI_P2P_DEVICE);
                //addText("Local device: " + MyP2PHelper.deviceToString(device));
                Log.d(TAG,"This device changed");
            } else if(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action)) {

                int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED);
                String persTatu = "Discovery state changed to ";

                if(state == WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED){
                    persTatu = persTatu + "Stopped.";
//                    startPeerDiscovery();
                }else if(state == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED){
                    persTatu = persTatu + "Started.";
                }else{
                    persTatu = persTatu + "unknown  " + state;
                }
                Log.d(TAG, persTatu);

            } else if (WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                if (networkInfo.isConnected()) {
                    Log.d(TAG, "Connected");
                    // Why did we do this?
                    //startPeerDiscovery();
                } else{
                    Log.d(TAG, "Disconnected");
                    // Why did we do this?
                    startPeerDiscovery();
                }
            }
            else {
                Log.d(TAG, "Unknown WIFI_P2P action: " + action);
            }
        }
    }
}
