/**
 *
 */
package org.openhab.binding.esa2000.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.openhab.io.transport.cul.CULListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author svenschreier
 *
 */
public class CULNetworkProxyService extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(CULNetworkProxyService.class);

    private final int socketPort;
    private final CULListener culListener;
    private ServerSocket serverSocket;
    private List<Socket> socketList = new ArrayList<Socket>();

    public CULNetworkProxyService(int socketPort, CULListener culListener) {
        super();
        this.socketPort = socketPort;
        this.culListener = culListener;
        start();
    }

    /**
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
        try {
            // create a new socket to given port
            this.serverSocket = new ServerSocket(this.socketPort);
            logger.info("Waiting for incoming connections to port " + this.socketPort);

            while (true) {
                // wait for incoming connection
                final Socket socket = this.serverSocket.accept();
                // add socket to list
                this.socketList.add(socket);

                logger.info("A new connection to socket established ...");

                SocketHandler handler = new SocketHandler(socket, this.culListener);
                handler.start();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void send(String data) {
        if (!this.socketList.isEmpty()) {
            for (final Socket socket : this.socketList) {
                if (socket.isConnected()) {
                    try {
                        PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
                        pw.write(data);
                        pw.flush();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private class SocketHandler extends Thread {
        private Socket socket = null;
        private CULListener culListener;

        public SocketHandler(Socket socket, CULListener culListener) {
            this.socket = socket;
            this.culListener = culListener;
        }

        @Override
        public void run() {
            try {
                BufferedReader in;

                in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));

                while (true) {
                    final String input = in.readLine();

                    // send data to listener
                    this.culListener.dataReceived(input);
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

    }
}
