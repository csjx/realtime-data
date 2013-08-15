/**
 * 
 */
package edu.hawaii.soest.pacioos.text;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A mock data source that, upon a client connecting to 127.0.0.1:5111, streams all lines of the
 * mock data source file back to the client.
 * @author cjones
 *
 */
public class MockDataSource implements Runnable {

    private static final Log log = LogFactory.getLog(MockDataSource.class);

	ServerSocket serverSocket;

	Socket clientSocket;

	String mockCTDData;
	
	PrintWriter out;
	
	public MockDataSource(String mockCTDData) {
		this.mockCTDData = mockCTDData;
	}
	
	public static void main(String[] args) {
		String mockCTDData = 
			"/Users/cjones/Documents/Development/clean/bbl/trunk/src/test/resources/edu/hawaii/soest/pacioos/text/AW02XX001CTDXXXXR00-mock-data.txt";
		MockDataSource mds = new MockDataSource(mockCTDData);
		mds.run();
		mds.stop();
	
	}
	
	/**
	 * Implements the Runnable interface, and creates a local data source server by
	 * binding to the 127.0.0.1:5111 address.  Upon a client connection, sends all lines
	 * of the mock data file to the client.
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		String host = "127.0.0.1";
		int portNumber = 5111;
		clientSocket = null;
		InetAddress address = null;
		try {
		    address = InetAddress.getByName(host);
		    serverSocket = new ServerSocket(portNumber, 1, address);
		    log.info("Accepting connections on " + 
		      serverSocket.getInetAddress() + ":" + serverSocket.getLocalPort());
		    clientSocket = serverSocket.accept();
		  
		} catch (IOException ioe) {
			log.error("Couldn't open the server socket: " + ioe.getMessage());
		}
		
	    File ctdData = new File(mockCTDData);
	    List<String> lines = new ArrayList<String>();
		try {
			lines = FileUtils.readLines(ctdData);
			
		} catch (IOException e) {
			log.error("Couldn't read data file: " + e.getMessage());
			
		}
		
	    try {
			if ( this.clientSocket.isConnected()) {
			    out = new PrintWriter(clientSocket.getOutputStream(), true);
				// loop through the file and send each line over the wire
				for ( String line : lines ) {
						line = "\r\n" + line + "\r\n";
			    	//log.debug("Sending " + line);
			    	out.println(line);
			    	Thread.sleep(1000);
			    }
				out.close();
			}  else {
				log.debug("Client is not connected");
			}
		} catch (IOException e) {
			e.printStackTrace();
			
		} catch (InterruptedException e) {
			e.printStackTrace();
			
		}

	}
	

	/**
	 * Shuts down the client and server socket connects
	 */
	public void stop() {
		try {
			log.info("Closing the MockDataSource connections.");
			clientSocket.close();
			serverSocket.close();
			
		} catch (IOException e) {
			log.error("Couldn't close the server socket: " + e.getMessage());
		}
	}


}
