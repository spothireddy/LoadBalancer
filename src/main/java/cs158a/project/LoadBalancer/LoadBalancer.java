package cs158a.project.LoadBalancer;

import java.io.*;
import java.net.*;
import java.util.HashMap;

public class LoadBalancer {
	static boolean servingFrom1 = true;

	public static void main(String[] args) throws IOException {
		try {
			String host = "192.168.56.10";
			String host2 = "192.168.56.11";
			int remoteport = 80;
			int localport = 1124;

			System.out.println("Starting loadbalancer for " + host + ":"
					+ remoteport + " and " + host2 + ":" + remoteport
					+ " on port " + localport);

			runServer(host, host2, remoteport, localport); // never returns

		/*	boolean exit = false;
			while (!exit) {
				System.out.println("Enter command:");
				String input = System.console().readLine();
				switch (input) {
				case "protocol":
					System.out
							.println("Enter new protocol, options are rr h1 h2");
					input = System.console().readLine();
					if(input=="rr") 
						protocol = "rr";
					else if(input=="h1")
						protocol = "h1";
					else if(input=="h2")
						protocol = "h2";
					else System.out.println("Invalid input");
					break;
				case "exit": exit =true;
				break;
					}
					
				}*/

		} catch (Exception e) {
			System.err.println(e);
		}
	}

	public static void runServer(final String host, final String host2,
			int remoteport, int localport) throws IOException {
		String protocol = "rr";
		// Create a ServerSocket to listen for connections with
		System.out.println("Waiting for connection from a client...");
		ServerSocket ss = new ServerSocket(localport);
		final HashMap<String, String> clientMap = new HashMap<String, String>();

		final byte[] request = new byte[1024];

		byte[] reply = new byte[4096];

		while (true) {

			Socket client = null, server = null, server2 = null, tempServ = null, tempServ2 = null;
			try {

				client = ss.accept();

				final InetAddress localAddress = client.getLocalAddress();
				final int localPort = client.getLocalPort();
				final InetAddress remoteAddress = client.getInetAddress();
				final int remotePort = client.getPort();

				// System.out.println("LOCAL STUFF: " +
				// localAddress.getCanonicalHostName()+":"+localPort + " (" +
				// localAddress.getHostAddress() + ")");

				System.out.println("Client trying to connect from "
						+ remoteAddress.getCanonicalHostName() + ":"
						+ remotePort + " (" + remoteAddress.getHostAddress()
						+ ")");

				System.out.println("Get client streams");
				final InputStream streamFromClient = client.getInputStream();
				final OutputStream streamToClient = client.getOutputStream();

				try {
					System.out.println("Make connection to real server");
					server = new Socket(host, remoteport);

				} catch (IOException e) {
					server = new Socket(host2, remoteport);
					/*
					PrintWriter out = new PrintWriter(streamToClient);
					out.print("Proxy server cannot connect to " + host + ":"
							+ remoteport + ":\n" + e + "\n");
					out.flush();
					client.close();
					*/
					//continue;
				}
				
				try {
					System.out.println("Make connection to real server");
					server2 = new Socket(host2, remoteport);

				} catch (IOException e) {
					server2 = new Socket(host, remoteport);
					continue;
				}


				// Get server streams.
				System.out.println("Get server streams");
				final InputStream streamFromServer = server.getInputStream();
				final OutputStream streamToServer = server.getOutputStream();

				final InputStream streamFromServer2 = server2.getInputStream();
				final OutputStream streamToServer2 = server2.getOutputStream();

				
						
					
				
				// a thread to read the client's requests and pass them
				// to the server. A separate thread for asynchronous.
				Thread t = new Thread() {
					public void run() {
						int bytesRead;
						try {
							if (clientMap.containsKey(remoteAddress
									.getHostName())) {
								while ((bytesRead = streamFromClient
										.read(request)) != -1) {
									if (clientMap.get(remoteAddress
											.getHostName()) == "servingFrom1") {
										System.out
												.println("Client recognized as "
														+ remoteAddress
																.getHostName());
										System.out.println();
										System.out.println("Reconnecting "
												+ remoteAddress.getHostName()
												+ " to backend server " + host);
										streamToServer.write(request, 0,
												bytesRead);
										streamToServer.flush();

									} else {
										System.out
												.println("Client recognized as "
														+ remoteAddress
																.getHostAddress());
										System.out.println();
										System.out
												.println("Reconnecting "
														+ remoteAddress
																.getHostAddress()
														+ " to backend server "
														+ host2);
										streamToServer2.write(request, 0,
												bytesRead);
										streamToServer2.flush();

										clientMap.put(
												remoteAddress.getHostAddress(),
												"servingFrom2");
									}

								}
							} else {
								if (servingFrom1)
									clientMap.put(remoteAddress
											.getCanonicalHostName(),
											"servingFrom1");
								else
									clientMap.put(remoteAddress
											.getCanonicalHostName(),
											"servingFrom2");
								while ((bytesRead = streamFromClient
										.read(request)) != -1) {
									if (servingFrom1) {
										System.out
												.println("Load balancer connected to "
														+ host);
										streamToServer.write(request, 0,
												bytesRead);
										streamToServer.flush();
										servingFrom1 = false;

									} else {
										System.out
												.println("Load balancer connected to "
														+ host2);
										streamToServer2.write(request, 0,
												bytesRead);
										streamToServer2.flush();
										servingFrom1 = true;

									}

								}
							}
							while ((bytesRead = streamFromClient.read(request)) != -1) {
								if (servingFrom1) {
									System.out
											.println("Load balancer connected to "
													+ host);
									streamToServer.write(request, 0, bytesRead);
									streamToServer.flush();
									servingFrom1 = false;
								} else {
									System.out
											.println("Load balancer connected to "
													+ host2);
									streamToServer2
											.write(request, 0, bytesRead);
									streamToServer2.flush();
									servingFrom1 = true;
								}

							}
						} catch (IOException e) {
						}

						// the client closed the connection to us, so close our
						// connection to the server.
						try {
							streamToServer.close();
							streamToServer2.close(); // checkkkkk this
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
					if (clientMap.containsKey(remoteAddress.getHostName())) {
						if (clientMap.get(remoteAddress.getHostName()) == "servingFrom1") {
							while ((bytesRead = streamFromServer.read(reply)) != -1) {
								streamToClient.write(reply, 0, bytesRead);
								streamToClient.flush();
							}

						} else {
							while ((bytesRead = streamFromServer2.read(reply)) != -1) {
								streamToClient.write(reply, 0, bytesRead);
								streamToClient.flush();
							}

						}
					} else {
						if (servingFrom1) {
							while ((bytesRead = streamFromServer.read(reply)) != -1) {
								streamToClient.write(reply, 0, bytesRead);
								streamToClient.flush();
							}
							servingFrom1 = false;
						} else {
							while ((bytesRead = streamFromServer2.read(reply)) != -1) {
								streamToClient.write(reply, 0, bytesRead);
								streamToClient.flush();
							}
							servingFrom1 = true;
						}
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
					else if (server2 != null)
						server2.close();
					if (client != null)
						client.close();
				} catch (IOException e) {
				}
			}
		}
	}
}
