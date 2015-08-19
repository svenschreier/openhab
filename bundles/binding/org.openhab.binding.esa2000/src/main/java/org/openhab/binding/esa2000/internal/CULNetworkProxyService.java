/**
 * 
 */
package org.openhab.binding.esa2000.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author svenschreier
 * 
 */
public class CULNetworkProxyService extends Thread {
	private static final Logger logger = LoggerFactory
			.getLogger(CULNetworkProxyService.class);

	private final int socketPort;
	private ServerSocket serverSocket;
	private List<Socket> socketList = new ArrayList<Socket>();

	public CULNetworkProxyService(int socketPort) {
		super();
		this.socketPort = socketPort;
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
						PrintWriter pw = new PrintWriter(
								socket.getOutputStream(), true);
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
}
