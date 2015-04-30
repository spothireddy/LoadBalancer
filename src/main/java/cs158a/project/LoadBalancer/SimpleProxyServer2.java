package cs158a.project.LoadBalancer;

import java.io.*;
import java.net.*;

public class SimpleProxyServer2 {
	public static void main(String[] args) throws IOException {
		try {
			String host = "192.168.56.10";
			String host2 = "192.168.56.11";
			String host3 = "192.168.56.20";
			int remoteport = 80;
			int localport = 1122;
			// Print a start-up message
			System.out.println("Starting proxy for " + host + ":" + remoteport
					+ " and " + host2 + ":" + remoteport + " on port "
					+ localport);
			// And start running the server
			runServer(host, remoteport, localport); // never returns
			System.out.println("NOW RUNNING TWO");
			runServer(host2, remoteport, localport);
			//runServer(host3, remoteport, localport);
		} catch (Exception e) {
			System.err.println(e);
		}
	}

	/**
	 * runs a single-threaded proxy server on the specified local port. It never
	 * returns.
	 */
	public static void runServer(final String host, int remoteport,
			int localport) throws IOException {
		// Create a ServerSocket to listen for connections with
		ServerSocket ss = new ServerSocket(localport);

		final byte[] request = new byte[1024];
		byte[] reply = new byte[4096];

		while (true) {
			Socket client = null, server = null;
			try {
				// Wait for a connection on the local port
				System.out.println("Waiting for connection from a client...");
				client = ss.accept();

				final SocketAddress localAddress = client
						.getLocalSocketAddress(); // client.getLocalAddress();
				final int localPort = client.getPort();
				final InetAddress remoteAddress = client.getInetAddress();
				final int remotePort = client.getPort();

				/*
				System.out.println("LOCAL STUFF: " + localAddress.toString()
						+ ":" + localPort + " (" + localAddress.toString()
						+ ")");
				*/
				System.out.println("Client trying to connect from "
						+ remoteAddress.getCanonicalHostName() + ":"
						+ remotePort + " (" + remoteAddress.getHostAddress()
						+ ")");

				System.out.println("CLIENT CONNECTED");
				final InputStream streamFromClient = client.getInputStream();
				final OutputStream streamToClient = client.getOutputStream();

				// Make a connection to the real server.
				// If we cannot connect to the server, send an error to the
				// client, disconnect, and continue waiting for connections.
				try {
					server = new Socket(host, remoteport);

				} catch (IOException e) {
					PrintWriter out = new PrintWriter(streamToClient);
					out.print("Proxy server cannot connect to " + host + ":"
							+ remoteport + ":\n" + e + "\n");
					out.flush();
					client.close();
					continue;
				}

				// Get server streams.
				final InputStream streamFromServer = server.getInputStream();
				final OutputStream streamToServer = server.getOutputStream();

				// a thread to read the client's requests and pass them
				// to the server. A separate thread for asynchronous.
				Thread t = new Thread() {
					public void run() {
						int bytesRead;
						try {
							while ((bytesRead = streamFromClient.read(request)) != -1) {

								System.out
										.println("Load balancer connected to "
												+ host);
								streamToServer.write(request, 0, bytesRead);
								streamToServer.flush();

							}
						} catch (IOException e) {
						}

						// the client closed the connection to us, so close our
						// connection to the server.
						try {
							streamToServer.close();
							System.out.println("end connection");
						} catch (IOException e) {
						}
					}
				};

				// Start the client-to-server request thread running
				t.start();

				// Read the server's responses
				// and pass them back to the client.
				int bytesRead;
				try {

					while ((bytesRead = streamFromServer.read(reply)) != -1) {
						streamToClient.write(reply, 0, bytesRead);
						streamToClient.flush();

					}

				} catch (IOException e) {
					// e.printStackTrace();
				}

				// The server closed its connection to us, so we close our
				// connection to our client.
				streamToClient.close();
			} catch (IOException e) {
				System.err.println(e);
			} finally {
				try {
					if (server != null)
						server.close();

					if (client != null)
						client.close();
				} catch (IOException e) {
				}
			}
		}

	}

}

class ThreadProxy extends Thread {
	private Socket client;
	private Socket server;
	private final String SERVER_URL;
	private final int SERVER_PORT;

	ThreadProxy(Socket client, Socket server, String ServerUrl, int ServerPort) {
		this.SERVER_URL = ServerUrl;
		this.SERVER_PORT = ServerPort;
		this.client = client;
		this.server = server;
		this.start();
	}

	@Override
	public void run() {

	}
}
