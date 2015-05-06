package test;


import java.io.*;
import java.net.*;

public class LoadBalancer {
	static boolean servingFrom1 = true;
	static boolean allServersDown = false;
	static boolean server1Down = false;
	static boolean server2Down = false;
	static InputStream streamFromServer = null;
	static OutputStream streamToServer = null;
	static Socket client = null;
	static Socket server = null;
	static Socket server2 = null;

	public static void main(String[] args) throws IOException {
		try {
			String host = "192.168.56.10";
			String host2 = "192.168.56.11";
			String loadBalancer = "192.168.56.1";
			int remoteport = 80;
			int localport = 1127;
			// Print a start-up message
			System.out.println("Starting proxy for " + host + ":" + remoteport
					+ " and " + host2 + ":" + remoteport + " on port "
					+ localport);
			// And start running the server
			runServer(host, host2, remoteport, localport); // never returns
		} catch (Exception e) {
			System.err.println("1. ERROR OCCURED " );
			e.printStackTrace();
		}
	}
	
	/**
	 * Make connection to server 1
	 */
	public static void connectToServer1(String host, int remoteport, OutputStream streamToClient){
		// Make a connection to the real server.
		// If we cannot connect to the server, send an error to the
		// client, disconnect, and continue waiting for connections.
		try {
			System.out.println("Make connection to real server for 1");
			
			server = new Socket(host, remoteport);
			
			streamFromServer = server.getInputStream();
			streamToServer = server.getOutputStream();
			

		} catch (IOException e) {
			System.out.println("Server 1 is down.");
			server = null;
			//servingFrom1 = false;
			server1Down = true;
			/**
			PrintWriter out = new PrintWriter(streamToClient);
			System.out.print("Proxy server cannot connect to " + host + ":"
					+ remoteport + ":\n" + e + "\n");
			e.printStackTrace();
			out.print("HELLOOOOOOO ");
			out.flush();
			//client.close();
			//continue;
			 * 
			*/
		}finally{
			
		}
	}
	
	
	/**
	 * Connect to server 2
	 */
	public static void connectToServer2(String host2, int remoteport, OutputStream streamToClient){
		try {
			System.out.println("Make connection to real server for 2");
			
			server2 = new Socket(host2, remoteport);
			System.out.println("Established connection to server 2");
			//lll
			streamFromServer = server2.getInputStream();
			streamToServer = server2.getOutputStream();
		} catch (Exception e) {
			server2 = null;
			server2Down = true;
			
			/**
			if(server1Down){
				allServersDown = true;
			}
			else{
				servingFrom1 = true;
			}
			**/
			
			
			/**
			PrintWriter out = new PrintWriter(streamToClient);
			System.out.print("Proxy server cannot connect to " + host2 + ":"
					+ remoteport + ":\n" + e + "\n");
			System.out.println("Server 2 is down.");
			out.print("HELLOOOOO 2");
			e.printStackTrace();
			//out.flush();
			//client.close();
			//continue;
			 * 
			*/
		} finally{
			System.out.println("Server 2: End of try catch");
		}
		
		//System.out.println("Server 2: End of try catch");
	}
	
	
	/**
	 * runs a single-threaded proxy server on the specified local port. It never
	 * returns.
	 */
	public static void runServer(final String host, final String host2,
			int remoteport, int localport) throws IOException {
		// Create a ServerSocket to listen for connections with
		System.out.println("Waiting for connection from a client...");
		ServerSocket ss = new ServerSocket(localport);

		final byte[] request = new byte[1024];
		byte[] reply = new byte[4096];

		while (true) {
			
			try {
				// Wait for a connection on the local port
				client = ss.accept();

				final SocketAddress localAddress = client
						.getLocalSocketAddress(); // client.getLocalAddress();
				final int localPort = client.getPort();
				final InetAddress remoteAddress = client.getInetAddress();
				final int remotePort = client.getPort();

				System.out.println("LOCAL STUFF: " + localAddress.toString()
						+ ":" + localPort + " (" + localAddress.toString()
						+ ")");

				System.out.println("Client trying to connect from "
						+ remoteAddress.getCanonicalHostName() + ":"
						+ remotePort + " (" + remoteAddress.getHostAddress()
						+ ")");

				System.out.println("Get client streams");
				final InputStream streamFromClient = client.getInputStream();
				final OutputStream streamToClient = client.getOutputStream();

				
				

				
				// Get server streams.
				
				System.out.println("Get server streams");
				
				if(servingFrom1){				
					connectToServer1(host, remoteport, streamToClient);
					
					if(server2Down){
						allServersDown = true;
					}
					else if(server1Down){
						servingFrom1 = false;
					}
				}
				
				if(!servingFrom1){
					
					connectToServer2(host2,remoteport,streamToClient);
					
					
					if (server2Down && !server1Down){
						servingFrom1 = true;
						connectToServer1(host, remoteport, streamToClient);
					}
					
					if(server1Down && server2Down){
						allServersDown = true;
					}
					
					
				}
				
			

				
				
				

				

				// a thread to read the client's requests and pass them
				// to the server. A separate thread for asynchronous.
				Thread t = new Thread() {
					public void run() {
						int bytesRead;
						try {
							//boolean x = server.isConnected();
							System.out.println("Read Client's request");

								
								while ((bytesRead = streamFromClient.read(request)) != -1) {
									if (servingFrom1 ) {
										System.out
												.println("Forwarding client request to server at "
														+ host);
										streamToServer.write(request, 0, bytesRead);
										streamToServer.flush();
										servingFrom1 = false;
									} else if(!servingFrom1 ) {
										System.out
												.println("Forwarding client request to server at "
														+ host2);
										streamToServer
												.write(request, 0, bytesRead);
										streamToServer.flush();
										servingFrom1 = true;
									}
									
									// the client closed the connection to us, so close our
									// connection to the server.
									System.out.println("Closed connection to server");
									streamToServer.close();
									//streamToServer2.close(); // checkkkkk this
									System.out.println("end connection");

								}
							
							
						} catch (IOException e) {
							System.out.println("THREAD EXCEPTION!!!!!!");
							e.printStackTrace();
						}
						finally{
							
						}

						

					}
				};

				// Start the client-to-server request thread running
				t.start();

				// Read the server's responses
				// and pass them back to the client.
				int bytesRead;
				try {
					//System.out.println("Reading server's response");
				/**
				 * 	if(allServersDown){
						reply = "ALL SERVERS ARE DOWN. TRY AGAIN LATER.".getBytes();
						System.out.println("9999: ALL SERVERS DOWN");
					}
				else 
				 */
					
					if (servingFrom1) {
						
						while ((bytesRead = streamFromServer.read(reply)) != -1) {
							streamToClient.write(reply, 0, bytesRead);
							streamToClient.flush();
						}
						servingFrom1 = false;
					} else if(!servingFrom1){
						while ((bytesRead = streamFromServer.read(reply)) != -1) {
							streamToClient.write(reply, 0, bytesRead);
							streamToClient.flush();
						}
						servingFrom1 = true;
					}

				} catch (IOException e) {
					// e.printStackTrace();
				}

				// The server closed its connection to us, so we close our
				// connection to our client.
				streamToClient.close();
				System.out.println("Closed connection to client.");
			} catch (IOException e) {
				System.err.println(e);
			} finally {
				try {
					if (server != null){
						server.close();
						System.out.println("close connection to server~~");
					}
					else if (server2 != null){
						server2.close();
						System.out.println("close connection to server~2~");
					}
						
					if (client != null)
						client.close();
					
					
					
				} catch (IOException e) {
					
					System.out.println(" CLOSE 1" + e);
				}
				finally{
					allServersDown = false;
					server1Down = false;
					server2Down = false;
				}
			}
		}

	}

}

/*
class ThreadProxy extends Thread {
	private Socket sClient;
	private final String SERVER_URL;
	private final int SERVER_PORT;

	ThreadProxy(Socket sClient, String ServerUrl, int ServerPort) {
		this.SERVER_URL = ServerUrl;
		this.SERVER_PORT = ServerPort;
		this.sClient = sClient;
		this.start();
	}

	@Override
	public void run() {

	}
}
*/
