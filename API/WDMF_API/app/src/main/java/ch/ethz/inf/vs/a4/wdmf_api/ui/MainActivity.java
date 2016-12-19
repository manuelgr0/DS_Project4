package ch.ethz.inf.vs.a4.wdmf_api.ui;

import android.content.ComponentName;
import android.content.Intent;

import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;


import org.w3c.dom.Text;

import ch.ethz.inf.vs.a4.wdmf_api.ipc_interface.WDMF_Connector;
import ch.ethz.inf.vs.a4.wdmf_api.R;
import ch.ethz.inf.vs.a4.wdmf_api.service.IncomingHandler;
import ch.ethz.inf.vs.a4.wdmf_api.service.MainService;

public class MainActivity extends AppCompatActivity  {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // SEND MSG TO SERVICE TESTER //

        final Button btn = (Button) findViewById(R.id.startButton);
        final TextView txt = (TextView) findViewById(R.id.textView);
        btn.setOnClickListener(new View.OnClickListener() {
            boolean on = false;
            @Override
            public void onClick(View v) {
                if (on) {
                    stopWDMFAPI();
                    txt.setText("Off");
                } else {
                    startWDMFAPI();
                    txt.setText("On");
                }
                on = !on;
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
        //connector.disconnectFromWDMF();
        super.onStop();
    }

    @Override
    protected void onStart() {
        //connector.connectToWDMF();
        super.onStart();
    }

    void startWDMFAPI(){
        Intent i = new Intent();
        i.setComponent(new ComponentName("ch.ethz.inf.vs.a4.wdmf_api", "ch.ethz.inf.vs.a4.wdmf_api.service.MainService"));

        startService(i);
        //connector.connectToWDMF();
    }

    void stopWDMFAPI(){
        Intent i = new Intent();
        i.setComponent(new ComponentName("ch.ethz.inf.vs.a4.wdmf_api", "ch.ethz.inf.vs.a4.wdmf_api.service.MainService"));

        // make sure it really stops:
        // send a start command telling the service to stop it ongoing work
        // this is the very simplest way to communicate to the Service, binding to it is not possible
        // anymore as soon as other clients have connected to the Messenger.
        Intent stop = (Intent) i.clone();
        stop.putExtra("stop", true);
        startService(stop);

        // stop started service,
        // note: if other clients are still bound to it, it will not be destroyed, which also the
        //       the reason why above command is necessary
        stopService(i);
    }
}
