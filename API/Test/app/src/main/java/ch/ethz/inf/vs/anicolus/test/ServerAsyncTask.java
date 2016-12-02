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
    private static boolean serverexist;
    public static ServerSocket serverSocket;

    public ServerAsyncTask(Context context) throws IOException {
        this.context = context;
    }

    @Override
    protected Object doInBackground(Object[] params) {
        try {
            /**
             * Create a server socket and wait for client connections. This
             * call blocks until a connection is accepted from a client
             */
            if(!serverexist) {
                serverSocket = new ServerSocket(8088);
                serverexist = true;
            }
            Socket client = serverSocket.accept();

            /**
              * If this code is reached, a client has connected and transferred data
              * Save the input stream from the client as a JPEG file
              */

            InputStream inputstream = client.getInputStream();
            while (inputstream.available() < 10);
            byte[] b = new byte[inputstream.available()];
            inputstream.read(b);
            String input = new String(b, "UTF-8");
            Log.d("GOT INPUT:   ", input);
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
