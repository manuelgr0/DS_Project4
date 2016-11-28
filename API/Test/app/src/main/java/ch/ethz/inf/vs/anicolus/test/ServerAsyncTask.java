package ch.ethz.inf.vs.anicolus.test;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by manue on 28.11.2016.
 */

public class ServerAsyncTask extends AsyncTask {
    private Context context;

    public ServerAsyncTask(Context context) {
        this.context = context;
    }

    @Override
    protected Object doInBackground(Object[] params) {
        try {
            /**
             * Create a server socket and wait for client connections. This
             * call blocks until a connection is accepted from a client
             */
            ServerSocket serverSocket = new ServerSocket(8888);
            Socket client = serverSocket.accept();

            /**
              * If this code is reached, a client has connected and transferred data
              * Save the input stream from the client as a JPEG file
              */

            InputStream inputstream = client.getInputStream();
            String input = inputstream.toString();
            Log.d("GOT INPUT:   ", input);
            serverSocket.close();
            return input;
        } catch (IOException e) {
            Log.e("Exception : ", e.getMessage());
            return "hallo";
        }

    }

    /**
     * Start activity that can handle the JPEG image
     *      
     */

    @Override
    protected void onPostExecute(Object o) {}
}
