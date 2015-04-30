package cs158a.project.LoadBalancer;

import java.io.*;
import java.net.*;

public class SimpleProxyServer {
	static boolean servingFrom1 = true;
  public static void main(String[] args) throws IOException {
    try {
      String host = "192.168.56.10";
      String host2 = "192.168.56.11";
      int remoteport = 80;
      int localport = 1119;
      // Print a start-up message
      System.out.println("Starting proxy for " + host + ":" + remoteport +
         " and " + host2 + ":" + remoteport + " on port " + localport);
      // And start running the server
      runServer(host, host2, remoteport, localport); // never returns
    } catch (Exception e) {
      System.err.println(e);
    }
  }

  /**
   * runs a single-threaded proxy server on
   * the specified local port. It never returns.
   */
  public static void runServer(final String host, final String host2, int remoteport, int localport)
      throws IOException {
    // Create a ServerSocket to listen for connections with
    ServerSocket ss = new ServerSocket(localport);

    final byte[] request = new byte[1024];
    byte[] reply = new byte[4096];

    while (true) {
      Socket client = null, server = null, server2=null;
      try {
        // Wait for a connection on the local port
        client = ss.accept();
        
        final InetAddress localAddress = client.getLocalAddress();
        final int localPort = client.getLocalPort();
        final InetAddress remoteAddress = client.getInetAddress();
        final int remotePort = client.getPort();
        
        //System.out.println("LOCAL STUFF: " + localAddress.getCanonicalHostName()+":"+localPort + " (" + localAddress.getHostAddress() + ")");
        
        System.out.println(
                "Accepted connection from "+
                remoteAddress.getCanonicalHostName() + ":" + 
                remotePort + " (" + remoteAddress.getHostAddress() + ")");
        
        
        System.out.println("CLIENT CONNECTED");
        final InputStream streamFromClient = client.getInputStream();
        final OutputStream streamToClient = client.getOutputStream();

        // Make a connection to the real server.
        // If we cannot connect to the server, send an error to the
        // client, disconnect, and continue waiting for connections.
        try {
          server = new Socket(host, remoteport);
          server2 = new Socket(host2, remoteport);
          
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
        
        final InputStream streamFromServer2 = server2.getInputStream();
        final OutputStream streamToServer2 = server2.getOutputStream();

        // a thread to read the client's requests and pass them
        // to the server. A separate thread for asynchronous.
        Thread t = new Thread() {
          public void run() {
            int bytesRead;
            try {
              while ((bytesRead = streamFromClient.read(request)) != -1) {
            	  if(servingFrom1){
            		  System.out.println("Load balancer connected to " + host);
            		  streamToServer.write(request, 0, bytesRead);
                      streamToServer.flush();
                      servingFrom1 = false;
            	  }
            	  else{
            		  System.out.println("Load balancer connected to " + host2);
            		  streamToServer2.write(request, 0, bytesRead);
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
              streamToServer2.close(); //checkkkkk this
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
        	
        	if(servingFrom1){
        		while ((bytesRead = streamFromServer.read(reply)) != -1) {
                    streamToClient.write(reply, 0, bytesRead);
                    streamToClient.flush();
                  }
        		servingFrom1 = false;
        	}
        	else{
        		while ((bytesRead = streamFromServer2.read(reply)) != -1) {
                    streamToClient.write(reply, 0, bytesRead);
                    streamToClient.flush();
                  }
        		servingFrom1 = true;
        	}
          
        } catch (IOException e) {
        	//e.printStackTrace();
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
          if (server2 != null)
              server2.close();
          if (client != null)
            client.close();
        } catch (IOException e) {
        }
      }
    }
  }
}
