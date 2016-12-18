package ch.ethz.inf.vs.jakmeier.wifisendandreceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION;


public class WifiAccessPoint implements WifiP2pManager.ConnectionInfoListener,WifiP2pManager.ChannelListener,WifiP2pManager.GroupInfoListener{

    static final public String DSS_WIFIAP_VALUES = "ch.ethz.inf.vs.jakmeier.wifisendandreceiver.DSS_WIFIAP_VALUES";
    static final public String DSS_WIFIAP_MESSAGE = "ch.ethz.inf.vs.jakmeier.wifisendandreceiver.DSS_WIFIAP_MESSAGE";

    static final public String DSS_WIFIAP_SERVERADDRESS = "ch.ethz.inf.vs.jakmeier.wifisendandreceiver.DSS_WIFIAP_SERVERADDRESS";
    static final public String DSS_WIFIAP_INETADDRESS = "ch.ethz.inf.vs.jakmeier.wifisendandreceiver.DSS_WIFIAP_INETADDRESS";

    LocalBroadcastManager broadcaster;
    MainActivity mainActivity;

    private WifiP2pManager p2p;
    private WifiP2pManager.Channel channel;

    String mNetworkName = "";
    String mPassphrase = "";
    String mInetAddress = "";
    String mConnectoTo = "uninitialized";

    String TAG = "WifiAccessPoint";

    private WifiP2pDnsSdServiceInfo mDnsSdServiceInfo;
    private String mInstanceName;
    private Handler mDnsSdServiceHandler;
    private boolean mKeepBroadcastingLocalSercive;

    private BroadcastReceiver receiver;
    private IntentFilter filter;

    public WifiAccessPoint(MainActivity ma) {
        this.mainActivity = ma;
        p2p = mainActivity.p2p;
        channel = mainActivity.channel;
        this.broadcaster = LocalBroadcastManager.getInstance(this.mainActivity);
    }

    // Build up a connection to a node by starting a AP and advertising it as for that node only
    public void Start(String node) {

        mConnectoTo = node;
        mainActivity.setApDisplayText("Starting to connect to: " + node + " ..." );
        //p2p = (WifiP2pManager) this.mainActivity.getSystemService(this.mainActivity.WIFI_P2P_SERVICE);

        if (p2p == null) {
            Log.d(TAG, "This device does not support Wi-Fi Direct");
        } else {

            //channel = p2p.initialize(this.mainActivity, this.mainActivity.getMainLooper(), this);

            receiver = new AccessPointReceiver();
            filter = new IntentFilter();
            filter.addAction(WIFI_P2P_STATE_CHANGED_ACTION);
            filter.addAction(WIFI_P2P_CONNECTION_CHANGED_ACTION);
            mainActivity.registerReceiver(receiver, filter);

            //WifiP2pConfig config = new WifiP2pConfig();
            //config.deviceAddress = mConnectoTo;
            // Connect causes a popup, therfore we need to go the local group route...
            //p2p.connect(channel, config, new WifiP2pManager.ActionListener() {

            removeGroup();
            p2p.createGroup(channel,new WifiP2pManager.ActionListener() {
                public void onSuccess() {
                    mainActivity.updateAp(true);
                    //Log.d(TAG, "p2p connection to " + mConnectoTo  + " established");
                    Log.d(TAG, "Creating Local Group ");
                    mainActivity.setApDisplayText("Created local group for node " + mConnectoTo+ " ...");
                    //mainActivity.setApDisplayText("p2p connection to " + mConnectoTo  + " established");
                }

                public void onFailure(int reason) {
                    Log.d(TAG, "Local Group failed, error code " + reason);
                }
            });
        }
    }

    public void Stop() {
        this.mainActivity.unregisterReceiver(receiver);
        stopLocalServices();
        removeGroup();
    }

    public void removeGroup() {
        p2p.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {
                p2p.removeGroup(channel, new WifiP2pManager.ActionListener() {
                    public void onSuccess() {
                        Log.d(TAG, "Cleared Local Group ");
                    }

                    public void onFailure(int reason) {
                        Log.d(TAG, "Clearing Local Group failed, error code " + reason);
                    }
                });
            }
        });
    }

    @Override
    public void onChannelDisconnected() {
        Log.d(TAG, "onChannelDisconnected was called");
        Stop();
        // see how we could avoid looping
        //     p2p = (WifiP2pManager) this.context.getSystemService(this.context.WIFI_P2P_SERVICE);
        //     channel = p2p.initialize(this.context, this.context.getMainLooper(), this);
    }

    @Override
    public void onGroupInfoAvailable(WifiP2pGroup group) {
        try {
            int numm = 0;
            for (WifiP2pDevice peer : group.getClientList()) {
                numm++;
                Log.d(TAG, "Client " + numm + " : "  + peer.deviceName + " " + peer.deviceAddress);
            }

            // DEBUG ONLY: Update UI
            mainActivity.updateApDisplay(true, group.getNetworkName(), group.getPassphrase(), group.getOwner().deviceName ,numm);

            if(mNetworkName.equals(group.getNetworkName()) && mPassphrase.equals(group.getPassphrase())){
                Log.d(TAG, "Already have local service for " + mNetworkName + " ," + mPassphrase);
            }else {

                mNetworkName = group.getNetworkName();
                mPassphrase = group.getPassphrase();
                startLocalService("NI:" + group.getNetworkName() + ":" + group.getPassphrase() + ":" + mInetAddress);
            }
        } catch(Exception e) {
            Log.d(TAG, "onGroupInfoAvailable, error: " + e.toString());
        }
    }

    private void startLocalService(String instance) {

        // password and network name are also passed in the instance name so the map is redundant
        Map<String, String> record = new HashMap<String, String>();
        record.put("for", mConnectoTo);
        //record.put("password", mPassphrase);
        //record.put("networkName", mNetworkName);

        mDnsSdServiceInfo = WifiP2pDnsSdServiceInfo.newInstance( instance, MainActivity.PLEASE_CONNECT, record);
        mInstanceName = instance;
        mDnsSdServiceHandler = new Handler();

        mKeepBroadcastingLocalSercive = true;
        Log.d(TAG, "Local AP service for " + mConnectoTo + "started");
        mainActivity.setApDisplayText("Local AP service for " + mConnectoTo + "started");
        repeatingServiceBroadcastPart();

    }

    private void repeatingServiceBroadcastPart() {
        //Log.d(TAG, "Clearing local services");
        p2p.clearLocalServices(channel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                //Log.d(TAG, "Add local service :" + mInstanceName);
                p2p.addLocalService(channel, mDnsSdServiceInfo, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        //Log.d(TAG, "Added local service, removing and re-adding after delay.");
                        mDnsSdServiceHandler.postDelayed(new Runnable() {

                            @Override
                            public void run() {
                                if (mKeepBroadcastingLocalSercive) {
                                    repeatingServiceBroadcastPart();
                                }
                            }
                        }, 5000L);
                    }

                    @Override
                    public void onFailure(int reason) {
                        Log.d(TAG, "Adding local service failed, error code " + reason);
                        repeatingServiceBroadcastPart();
                    }
                });
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "Clearing local service failed, error code " + reason);
                repeatingServiceBroadcastPart();
            }
        });
    }

    private void stopLocalServices() {
        mNetworkName = "";
        mPassphrase = "";

        mKeepBroadcastingLocalSercive = false;

        p2p.clearLocalServices(channel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                Log.d(TAG, "Cleared local services");
            }

            public void onFailure(int reason) {
                Log.d(TAG, "Clearing local services failed, error code " + reason);
            }
        });
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        try {
            if (info.isGroupOwner) {
                if(broadcaster != null) {
                    mInetAddress = info.groupOwnerAddress.getHostAddress();
                    Intent intent = new Intent(DSS_WIFIAP_SERVERADDRESS);
                    intent.putExtra(DSS_WIFIAP_INETADDRESS, (Serializable)info.groupOwnerAddress);
                    broadcaster.sendBroadcast(intent);
                }
                p2p.requestGroupInfo(channel,this);
            } else if(info.groupOwnerAddress != null){
                Log.d(TAG, "we are client !! group owner address is: " + info.groupOwnerAddress.getHostAddress());
            }
            else{
                Log.d(TAG, "unexpected info: " + info);
            }
        } catch(Exception e) {
            Log.d(TAG, "onConnectionInfoAvailable, error: " + e.toString());
        }
    }


    private class AccessPointReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    Log.d("APR received", intent.getExtras().toString());
                    // startLocalService();
                } else {
                    //stopLocalService();
                    //Todo: Add the state monitoring in higher level, stop & re-start all when happening
                }
            }  else if (WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                if (networkInfo.isConnected()) {
                    Log.d(TAG, "We are connected, will check info now");
                    p2p.requestConnectionInfo(channel, WifiAccessPoint.this);
                } else{
                    Log.d(TAG, "We are disconnected");
                }
            }
        }
    }
}