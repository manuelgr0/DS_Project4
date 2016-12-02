package ch.ethz.inf.vs.anicolus.test;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.View;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by manue on 28.11.2016.
 */

public class ClientTask extends Thread {
    private Context context;
    private String host;
    private int port;

    public ClientTask(Context context, String host, int port) {
        this.context = context;
        this.host = host;
        this.port = port;
    }

    @Override
    public void run() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int len;
                Socket socket = new Socket();
                byte buf[] = new byte[1024];
                try {
                    /**
                      * Create a client socket with the host,
                      * port, and timeout information.
                      */
                    socket.bind(null);
                    socket.connect(new InetSocketAddress(host, port));

                    if(socket.isConnected())
                        Log.d("+++++CONNECTED+++++++", "   ");

                    /**
                          * Create a byte stream from a String and pipe it to the output stream
                          * of the socket. This data will be retrieved by the server device.
                          */
                    OutputStream outputStream = socket.getOutputStream();
                    String input = "Hallo ...?";
                    outputStream.write(input.getBytes("UTF-8"));
                    outputStream.close();
                } catch (IOException e) {
                    //catch logic
                    e.printStackTrace();
                    Log.d("blabla", "bliblubla");
                }

/**
  * Clean up any open sockets when done
  * transferring or if an exception occurred.
  */ finally {
                    if (socket != null) {
                        if (socket.isConnected()) {
                            try {
                                socket.close();
                            } catch (IOException e) {
                                //catch logic
                            }
                        }
                    }
                }
            }
        }).start();

    }
}
