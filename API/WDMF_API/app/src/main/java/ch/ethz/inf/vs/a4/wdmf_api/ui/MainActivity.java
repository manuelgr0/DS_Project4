package ch.ethz.inf.vs.a4.wdmf_api.ui;

import android.content.ComponentName;
import android.content.Intent;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;


import ch.ethz.inf.vs.a4.wdmf_api.ipc_interface.WDMF_Connector;
import ch.ethz.inf.vs.a4.wdmf_api.R;

public class MainActivity extends AppCompatActivity  {


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

        // >> ONLY FOR TESTING <<
        Button testButton = (Button) findViewById(R.id.testButton);
        testButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent myIntent = new Intent(MainActivity.this, TestActivity.class);
                MainActivity.this.startActivity(myIntent);
            }
        });
        // END ONLY FOR TESTING //////

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
    }

    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        connector.disconnectFromWDMF();
        super.onStop();
    }

    @Override
    protected void onStart() {
        connector.connectToWDMF();
        super.onStart();
    }

    void startWDMFAPI(){
        Intent i = new Intent();
        i.setComponent(new ComponentName("ch.ethz.inf.vs.a4.wdmf_api", "ch.ethz.inf.vs.a4.wdmf_api.service.MainService"));

        startService(i);
    }

    void stopWDMFAPI(){
        Intent i = new Intent();
        i.setComponent(new ComponentName("ch.ethz.inf.vs.a4.wdmf_api", "ch.ethz.inf.vs.a4.wdmf_api.service.MainService"));
        stopService(i); //TODO: make sure it really stops!
    }

}
