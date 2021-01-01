/**
 *  Copyright: 2007 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: To read Advantech ADAM 60XX binary engineering data over UDP
 *             communication and forward packet data to AdamSource drivers for
 *             conversion and upload to the RBNB DataTurbine.
 *    Authors: Christopher Jones
 *
 * $HeadURL$
 * $LastChangedDate$
 * $LastChangedBy$
 * $LastChangedRevision$
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */ 
package edu.hawaii.soest.kilonalu.adam;


import java.io.File;
import java.io.IOException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import java.nio.ByteBuffer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import org.apache.commons.codec.binary.Hex;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A simple class used to harvest a decimal ASCII data stream from an Advantech
 * ADAM 6XXX module over a UDP socket connection  from a 
 * serial2ip converter host. The data stream is then converted into RBNB frames 
 * and pushed into the RBNB DataTurbine real time server using multiple AdamSource
 * objects, depending on which address the datagram originates from. 
 */
public class AdamDispatcher {

  /**
   * The Logger instance used to log system messages 
   */
  private static Log log = LogFactory.getLog(AdamDispatcher.class);
  
  
  /**
   * The XML configuration file location for the list of sensor properties
   */
  private String xmlConfigurationFile = "lib/sensor.properties.xml";
  
  /**
   * The XML configuration object with the list of sensor properties
   */
  private XMLConfiguration xmlConfiguration;
  
  /*
   *  A default source IP address for the source sensor data
   */
  private final String DEFAULT_SOURCE_HOST_NAME = "localhost";  

  /**
   * The domain name or IP address of the host machine that this Source 
   * represents and from which the data will stream. 
   */
  private String sourceHostName = DEFAULT_SOURCE_HOST_NAME;

  /*
   *  A default source UDP port for the source sensor data
   */  
  private final int DEFAULT_SOURCE_HOST_PORT  = 5168;

  /**
   * The UDP port to connect to on the Source host machine 
   */
  private int sourceHostPort = DEFAULT_SOURCE_HOST_PORT;
  
  /*
   * The default IP address or DNS name of the RBNB server
   */
  private static final String DEFAULT_SERVER_NAME = "localhost";
  
  /*
   * The default TCP port of the RBNB server
   */
  private static final int DEFAULT_SERVER_PORT = 3333;
  
  /*
   * The IP address or DNS name of the RBNB server
   */
  private String serverName = DEFAULT_SERVER_NAME;
  /*
   * The default TCP port of the RBNB server
   */
  private int serverPort = DEFAULT_SERVER_PORT;
  
  /*
   * The address and port string for the RBNB server
   */
  private String server = serverName + ":" + serverPort;
  
  /*
   * The default archive mode for RBNB Source clients
   */
  private static final String DEFAULT_ARCHIVE_MODE = "append";
  
  /*
   * The archive mode for RBNB Source clients
   */
  private String archiveMode = DEFAULT_ARCHIVE_MODE;
  
  /*
   * The default size of the ByteBuffer used to buffer the UDP stream from the
   * source instrument.
   */  
  private int DEFAULT_BUFFER_SIZE = 256; // bytes

  /**
   * The size of the ByteBuffer used to buffer the UDP stream from the 
   * source instrument.
   */
  private int bufferSize = DEFAULT_BUFFER_SIZE;
  
  /*
   * The thread that is run for streaming data from the instrument
   */
  private Thread streamingThread;
  
  /*
   * The the connection status for the UDP port for the data streams
   */
  private boolean connected = false;
  
  /*
   * A boolean field indicating if the instrument connection is ready to stream
   */
  private boolean readyToStream = false;

  /*
   * The socket used to establish UDP communication with the instrument
   */
  private DatagramSocket datagramSocket;
  
  /*
   * The datagram object used to represent an individual incoming UDP packet
   */
  private DatagramPacket datagramPacket;
  
  /*
   * A hash map that contains IP address to RBNB Source mappings
   */
  private HashMap<String, AdamSource> sourceMap;
  
  /*
   * An internal Thread setting used to specify how long, in milliseconds, the
   * execution of the data streaming Thread should wait before re-executing
   * 
   * @see execute()
   */
  private final int RETRY_INTERVAL = 5000;
   
  /**
   * Constructor - create an empty instance of the AdamDispatcher object, using
   * default values for the RBNB server name and port, source instrument name
   * and port, archive mode, archive frame size, and cache frame size. 
   */
  public AdamDispatcher() {
  }

  /**
   * A method that executes the streaming of data from the source to the RBNB
   * server after all configuration of settings, connections to hosts, and
   * thread initiatizing occurs.  This method contains the detailed code for 
   * streaming the data and interpreting the stream.
   * 
   * @return failed True if the execution fails
   */
  protected boolean execute() {
    log.info("AdamDispatcher.execute() called.");
    // do not execute the stream if there is no connection
    if (  !isConnected() ) return false;
    
    boolean failed = false;
    
    // while data are being sent, read them into the buffer
    try {

      // Create a buffer that will store the sample bytes as they are read
      byte[] bufferArray = new byte[getBufferSize()];

      // and a ByteBuffer used to transfer the bytes to the parser
      ByteBuffer sampleBuffer = ByteBuffer.allocate(getBufferSize());
      
      this.datagramPacket = new DatagramPacket(bufferArray, bufferArray.length);
      
      // while there are bytes to read from the socket ...
      while ( !failed ) {
        
        // receive any incoming UDP packets and parse the data payload
        datagramSocket.receive(this.datagramPacket);
        
        log.debug("Host: " + datagramPacket.getAddress() + 
                      " data: " + new String(Hex.encodeHex(datagramPacket.getData())));
        
        // the address seems to be returned with a leading slash (/). Trim it.
        String datagramAddress = datagramPacket.getAddress().toString().replaceAll("/", "");
        
        sampleBuffer.put(datagramPacket.getData());
        
        // Given the IP address of the source UDP packet and the data ByteBuffer,
        // find the correct source in the sourceMap hash and process the data
        if (sourceMap.get(datagramAddress) != null ) {
          
          AdamSource source = sourceMap.get(datagramAddress);

          // process the data using the AdamSource driver
          source.process(datagramAddress, this.xmlConfiguration, sampleBuffer);
            
        } else {
          log.debug("There is no configuration information for " +
                      "the ADAM module at " + datagramAddress      +
                      ". Please add the configuration to the "     +
                      "sensor.properties.xml configuration file.");
        }
        
        sampleBuffer.clear();
        
      } // end while (more socket bytes to read)
      
      disconnect();
//      
    } catch ( IOException e ) {
      // handle exceptions
      // In the event of an i/o exception, log the exception, and allow execute()
      // to return false, which will prompt a retry.
      failed = true;
      e.printStackTrace();
      return !failed;
      
    }
    
    return !failed;
  }

  /**
   * A method used to connect to the UDP port of the host that will have the 
   * UDP stream of data packets, and that will also connect each of the AdamSource
   * drivers to the DataTurbine.
   * 
   * @return isConnected True if the source is connected to the Data Turbine
   */
  protected boolean connect() {
    if ( isConnected() ) {
      return true;
    }
    
    try {
      // bind to the UDP socket
      this.datagramSocket = new DatagramSocket(getHostPort());
      
      // Create a list of sensors from the properties file, and iterate through
      // the list, creating an RBNB Source object for each sensor listed. Store
      // these objects in a HashMap for later referral.
      
      this.sourceMap = new HashMap<String, AdamSource>();
      
      List sensorList  = this.xmlConfiguration.getList("sensor.address");
      
      // declare the properties that will be pulled from the 
      // sensor.properties.xml file
      String address        = "";
      String sourceName     = "";
      String cacheSize      = "";
      String archiveSize    = "";
      String archiveChannel = "";
      
      // evaluate each sensor listed in the sensor.properties.xml file
      for ( Iterator sIterator = sensorList.iterator(); sIterator.hasNext(); ) {
        
        // get each property value of the sensor
        int index = sensorList.indexOf(sIterator.next());
        address     = (String) this.xmlConfiguration.getProperty("sensor(" + index + ").address" );
        sourceName  = (String) this.xmlConfiguration.getProperty("sensor(" + index + ").name" );
        cacheSize   = (String) this.xmlConfiguration.getProperty("sensor(" + index + ").cacheSize" );
        archiveSize = (String) this.xmlConfiguration.getProperty("sensor(" + index + ").archiveSize" );
      
        // given the properties, create an RBNB Source object
        AdamSource adamSource = 
          new AdamSource(this.serverName, 
                         (new Integer(this.serverPort)).toString(),
                         this.archiveMode, 
                         (new Integer(archiveSize)),
                         (new Integer(cacheSize)),
                         sourceName);
        adamSource.startConnection();
        sourceMap.put(address, adamSource);
        
      }
      connected = true;
      
    } catch (SocketException se) {
      System.err.println("Failed to connect to the UDP data stream. " +
        "The error message was: " + se.getMessage());
     
    }
  
    return connected;
  }

  /*
   * A method that returns the connection status
   * 
   * @return connected True if the source is connected to the Data Turbine
   */
  public boolean isConnected() {
    return connected;
    
  } 
  
  /**
   * A method used to disconnect from the UDP port of the host that has the 
   * UDP stream of data packets, and that will also disconnect each of the 
   * AdamSource drivers from the DataTurbine.
   */
  protected void disconnect() { 
    
    // disconnect from the UDP socket
    if ( datagramSocket != null ) {
      this.datagramSocket.close();
      
    }
    
    // get each source and disconnect from the DataTurbine
    for ( Iterator sIterator = (sourceMap.keySet()).iterator(); sIterator.hasNext(); ) {
      
      AdamSource adamSource = sourceMap.get(sIterator.next());
      adamSource.stopConnection();
      log.info("Disconnected from source: " + adamSource.getRBNBClientName());
    }
    connected = false;
    
  }
 
  /**
   * A method that sets the size, in bytes, of the ByteBuffer used in streaming 
   * data from a source instrument via a TCP connection
   * 
   * @return bufferSize The size of the buffer
   */
   public int getBufferSize() {
     return this.bufferSize;
   }
   
  /**
   * A method that returns the domain name or IP address of the source 
   * instrument (i.e. the serial-to-IP converter to which it is attached)
   * 
   * @return sourceHostName  The name of the source host
   */
  public String getHostName(){
    return this.sourceHostName;
  }

  /**
   * A method that returns the TCP port of the source 
   * instrument (i.e. the serial-to-IP converter to which it is attached)
   * 
   * @return sourceHost Port the port of the source host
   */
  public int getHostPort(){
    return this.sourceHostPort;
  }

  /**
   * A method that returns true if the UDP DatagramSocket connection is 
   * established and if the data streaming Thread has been started
   * 
   * @return isRunning True if the source is running
   */
  public boolean isRunning() {
    // return the connection status and the thread status
    return ( isConnected() && readyToStream );
  }
  
  /**
   * The main method for running the code
   * @param args the command line list of string arguments
   */
  public static void main (String[] args) {
   
    try {
      // create a new instance of the AdamDispatcher object, and parse the command 
      // line arguments and xml configuration as settings for this instance
      final AdamDispatcher adamDispatcher = new AdamDispatcher();
      
      log.info("AdamDispatcher.main() called.");
      
      // parse the commandline arguments and the sensor configuration file
      // to configure the UDP source and RBNB serverconnections, then 
      // start the streaming connection between the source and the RBNB server.
      if ( adamDispatcher.parseArgs(args) && adamDispatcher.parseConfiguration()) {
        adamDispatcher.start();
      }
      
      // Handle ctrl-c's and other abrupt death signals to the process
      Runtime.getRuntime().addShutdownHook(new Thread() {
        // stop the streaming process
        public void run() {
          adamDispatcher.stop();
        }
      }
      );
      
    } catch ( Exception e ) {
      log.info("Error in main(): " + e.getMessage());
      e.printStackTrace();
    }
  }
  
  /*
   * A method that runs the data streaming work performed by the execute()
   * by handling execution problems and continuously trying to re-execute after 
   * a specified retry interval for the thread.
   */
  private void runWork() {
    
    // handle execution problems by retrying if execute() fails
    boolean retry = true;
    
    while ( retry ) {
      
      // connect to the UDP socket, and establish RBNB source connections for
      // each of the sensors
      if ( connect() ) {
        
        // run the data streaming code
        retry = !execute();
      }
      
      disconnect();
      
      if ( retry ) {
        try {
          Thread.sleep(RETRY_INTERVAL);
        } catch ( Exception e ){
          log.info("There was an execution problem. Retrying. Message is: " +
          e.getMessage());
        }
      }
    }
    // stop the streaming when we are done
    stop();
  }

  /*
   * A method used to parse the command line arguments and configure the UDP
   * and RBNB connections
   * 
   * @return argumentsParsed True if the arguments were parsed
   */
  private boolean parseArgs( String[] args) {
    
    CommandLine command;
    try {
      command = ( new PosixParser() ).parse(setOptions(), args);
      
    } catch ( ParseException pe ) {
      log.info("There was an error parsing the command line options. "  + 
      "Please be sure to use the correct options. The error message was: " +
      pe.getMessage());
      return false;
    }
    return setArgs(command);
    
  }
  
  /*
   * A method used to get the sensor configuration properties for each of
   * the listed ADAM sensors
   *
   * @param configurationFile - the name of the XML configuration file
   * @return failed True if the configuration parsing fails
   */
   private boolean parseConfiguration() {
     
     boolean failed = true;
     
     try {
       // create an XML Configuration object from the sensor XML file
       File xmlConfigFile = new File(this.xmlConfigurationFile);
       this.xmlConfiguration = new XMLConfiguration(xmlConfigFile);
       failed = false;
       
     } catch ( NullPointerException npe ) {
       log.info("There was an error reading the XML configuration file. " +
         "The error message was: " + npe.getMessage());

     } catch ( ConfigurationException ce ) {
       log.info("There was an error creating the XML configuration. " +
         "The error message was: " + ce.getMessage());
       
     }
     return !failed;
     
   }

  /**
   * A method that sets the command line arguments for this class.
   * 
   * @param command  The CommandLine object being passed in from the command
   * @return argumentsSet True if the arguments are set
   */
  protected boolean setArgs(CommandLine command) {
    
    // handle the -h option
    if ( command.hasOption('h') ) {
      printUsage();
      return false;
    }
    
    // handle the -s option
    if ( command.hasOption('s') ) {
     String serverName = command.getOptionValue('s');
     if ( serverName != null ) setServerName(serverName);
    }

    // handle the -p option
    if ( command.hasOption('p') ) {
      String serverPort = command.getOptionValue('p');
      if ( serverPort != null ) {
        try {
          setServerPort(Integer.parseInt(serverPort));
          
        } catch (NumberFormatException nf) {
          System.out.println(
            "Please enter a numeric value for -p (server port). " + 
            serverPort + " is not valid.");
          return false;
        }
      }
    }
    
    // handle the -H option
    if ( command.hasOption("H") ) {
      String hostName = command.getOptionValue("H");
      if ( hostName != null ) {
        setHostName(hostName);
      }
    }

    // handle the -P option, test if it's an integer
    if ( command.hasOption("P") ) {
      String hostPort = command.getOptionValue("P");
      if ( hostPort != null ) {
        try {
          setHostPort(Integer.parseInt(hostPort));
          
        } catch ( NumberFormatException nfe ){
          log.info("Please enter a numeric value for the host port. " +
                      hostPort + " is not a valid number.");
          return false;
        }
      }
    }

    return true;
  }

  /**
   * A method that sets the domain name or IP address of the source 
   * instrument (i.e. the serial-to-IP converter to which it is attached)
   *
   * @param hostName  the domain name or IP address of the source instrument
   */
  public void setHostName(String hostName) {
    this.sourceHostName = hostName;
  }

  /**
   * A method that sets the TCP port of the source 
   * instrument (i.e. the serial-to-IP converter to which it is attached)
   *
   * @param hostPort  the TCP port of the source instrument
   */
  public void setHostPort(int hostPort) {
    this.sourceHostPort = hostPort;
  }

  /**
   * A method that sets the domain name or IP address of the RBNB server
   *
   * @param serverName  the domain name or IP address of the RBNB server
   */
  public void setServerName(String serverName) {
    this.serverName = serverName;
  }

  /**
   * A method that sets the TCP port of the RBNB server 
   *
   * @param serverPort  the TCP port of the RBNB server
   */
  public void setServerPort(int serverPort) {
    this.serverPort = serverPort;
  }

  /**
   * A method that sets the command line options for this class.
   * 
   * @return options The command line options being set
   */
  protected Options setOptions() {
    Options options = new Options();
    
    options.addOption("h", false, "Print help");
    options.addOption("s", true,  "RBNB Server Hostname");
    options.addOption("p", true,  "RBNB Server Port Number");
    options.addOption("H", true,  "Source host name or IP");
    options.addOption("P", true,  "Source host port number");    
                      
    return options;
  }

  /**
   * A method that starts the streaming of data from the source instruments to
   * the RBNB server via an established TCP connection.  
   * 
   * @return started True if the source is started
   */
  public boolean start() {
    
    // return false if the streaming is running
    if ( isRunning() ) {
      return false;
    }
    
    // reset the connection to the RBNB server
    if ( isConnected() ) {
      disconnect();
    }
    connect();
    
    // return false if the connection fails
    if ( !isConnected() ) {
      return false;
    }
    
    // begin the streaming thread to the source
    startThread();
    
    return true;  
  }

  /*
   * A method that creates and starts a new Thread with a run() method that 
   * begins processing the data streaming from the source instrument.
   */
  private void startThread() {
    
    // build the runnable class and implement the run() method
    Runnable runner = new Runnable() {
      public void run() {
        runWork();
      }
    };
    
    // build the Thread and start it, indicating that it has been started
    readyToStream = true;
    streamingThread = new Thread(runner, "StreamingThread");
    streamingThread.start();     
  }

  /**
   * A method that stops the streaming of data between the source instruments and
   * the RBNB server.  
   * 
   * @return stopped True if the source is stopped
   */ 
  public boolean stop() {
    
    // return false if the thread is not running
    if ( !isRunning() ) {
      return false;
    }
    
    // stop the thread and disconnect from the RBNB server
    stopThread();
    disconnect();
    return true;
  }

  /*
   * A method that interrupts the thread created in startThread()
   */
  private void stopThread() {
    // set the streaming status to false and stop the Thread
    readyToStream = false;
    streamingThread.interrupt();
  }

  /**
   * Print out the usage of this application to standard output.
   */
  protected void printUsage() {
    HelpFormatter helpFormatter = new HelpFormatter();
    helpFormatter.printHelp(this.getClass().getName(),setOptions());
    
  }
    
}
