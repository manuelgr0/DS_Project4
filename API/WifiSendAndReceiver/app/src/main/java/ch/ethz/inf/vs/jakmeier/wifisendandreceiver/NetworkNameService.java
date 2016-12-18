package ch.ethz.inf.vs.jakmeier.wifisendandreceiver;

import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.os.Handler;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION;

/**
 * Created by Jakob on 17.12.2016.
 *
 * This class is responsible to constantly broadcast some information:
 *  - The fact that we use the WDMFAPI
 *  - Our MAC address
 *  - Our Network tag that we are currently using
 *
 */

public class NetworkNameService {

    private String mNetworkTag;
    private String mMacAddress;

    private WifiP2pManager mP2P;
    private WifiP2pManager.Channel mChannel;
    private boolean mStopped = true;


    private final static String TAG = "NetworkNameService";

    private MainActivity mainActivity;

    public NetworkNameService(String networkTag, String macAddress, MainActivity ma){
        mNetworkTag = networkTag;
        mMacAddress = macAddress;
        mainActivity = ma;
        mP2P = mainActivity.p2p;
        mChannel = mainActivity.channel;
        //mP2P = (WifiP2pManager) this.mainActivity.getSystemService(this.mainActivity.WIFI_P2P_SERVICE);
        if (mP2P == null) {
            Log.d(TAG, "This device does not support Wi-Fi Direct");
        }
    }

    // Starts providing our network information using Bonjour
    public void start(){
        if (mP2P != null) {
            // Here don't listen to anything, just speak with closed ears
            //mChannel = mP2P.initialize(this.mainActivity, this.mainActivity.getMainLooper(), null);

            Map<String, String> TXTrecord = new HashMap<>();
            TXTrecord.put("netID", mNetworkTag);

            WifiP2pDnsSdServiceInfo info = WifiP2pDnsSdServiceInfo.newInstance( mMacAddress, MainActivity.HELLO_SERVICE, TXTrecord);
            Handler h = new Handler();
            mStopped = false;
            repeatedNnsBroadcast(info, h);

        } else {
            Log.d(TAG, "Can't start Network name service without WifiP2PManager.");
        }
    }

    // Stop the ongoing information providing
    public void stop(){
        mStopped = true;
        mP2P.clearLocalServices(mChannel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                Log.d(TAG, "Cleared local services");
            }
            public void onFailure(int reason) {
                Log.d(TAG, "Clearing local services failed, error code " + reason);
            }
        });
    }

    // loop in 5 second steps
    private void repeatedNnsBroadcast(final WifiP2pDnsSdServiceInfo info, final Handler handler){
        // clear local services and wait for success
        mP2P.clearLocalServices(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                //Log.d(TAG, "Add local service :" + info);
                mP2P.addLocalService(mChannel, info, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        //Log.d(TAG, "Added local service, removing and re-adding after delay.");
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (!mStopped) {
                                    repeatedNnsBroadcast(info, handler);
                                }
                            }
                        }, 5000L);
                    }

                    @Override
                    public void onFailure(int reason) {
                        Log.d(TAG, "Adding local service failed, error code " + reason);
                        repeatedNnsBroadcast(info, handler);
                    }
                });
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "Clear local services failed, error code " + reason);
                repeatedNnsBroadcast(info, handler);
            }
        });

    }

}
