/**
 *  Copyright: 2007 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: To convert Advantech ADAM 60XX binary engineering data into 
 *             RBNB Data Turbine frames for archival and realtime access.
 *    Authors: Christopher Jones
 *
 * $HeadURL: https://bbl.ancl.hawaii.edu/projects/bbl/trunk/src/java/edu/hawaii/soest/kilonalu/adam/AdamSource.java $
 * $LastChangedDate: 2009-06-19 16:37:18 -0600 (Fri, 19 Jun 2009) $
 * $LastChangedBy: cjones $
 * $LastChangedRevision: 398 $
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

import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.Source;
import com.rbnb.sapi.SAPIException;

import java.lang.StringBuffer;
import java.lang.StringBuilder;
import java.lang.InterruptedException;

import java.io.File;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.DataInputStream;
import java.io.IOException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import java.nio.ByteBuffer;

import java.text.SimpleDateFormat;

import java.util.Arrays;
import java.util.Date;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;

import org.apache.commons.codec.binary.Hex;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

import org.apache.commons.lang.exception.NestableException;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

import org.nees.rbnb.RBNBBase;
import org.nees.rbnb.RBNBSource;

/**
 * A simple class used to process a binary data stream from an Advantech
 * ADAM 6XXX module.  The data stream is converted into RBNB frames 
 * and pushed into the RBNB DataTurbine real time server.  This class extends 
 * org.nees.rbnb.RBNBSource, which in turn extends org.nees.rbnb.RBNBBase, 
 * and therefore follows the API conventions found in the org.nees.rbnb code.  
 *
 * The parsing of the data stream is performed by the <code>AdamParser</code>
 * class.
 *
 */
public class AdamSource extends RBNBSource {

  /**
   * The default log configuration file location
   */
  private final String DEFAULT_LOG_CONFIGURATION_FILE = "lib/log4j.properties";

  /**
   * The log configuration file location
   */
  private String logConfigurationFile = DEFAULT_LOG_CONFIGURATION_FILE;
  
  /**
   * The Logger instance used to log system messages 
   */
  private static Logger logger = Logger.getLogger(AdamSource.class);
  
  
///**
// * The XML configuration file location for the list of sensor properties
// */
//private String xmlConfigurationFile = "lib/sensor.properties.xml";
  
  /*
   *  A default archive mode for the given source connection to the RBNB server.
   * Valid modes include 'append', 'create', 'load' and 'none'.
   */
  private final String DEFAULT_ARCHIVE_MODE = "append";
  
  /*
   * The mode in which the source interacts with the RBNB archive. Valid modes 
   * include 'append', 'create', 'load' and 'none', however, Kilo Nalu 
   * instruments should append to an archive, which will create one if none 
   * exist.
   */
  private String archiveMode = DEFAULT_ARCHIVE_MODE;

  /*
   * The default size of the ByteBuffer used to beffer the TCP stream from the
   * source instrument.
   */  
  private int DEFAULT_BUFFER_SIZE = 256; // bytes

  /**
   * The size of the ByteBuffer used to beffer the TCP stream from the 
   * instrument.
   */
  private int bufferSize = DEFAULT_BUFFER_SIZE;
  
  /*
   *  A default source IP address for the source sensor data
   */
  private final String DEFAULT_SOURCE_HOST_NAME = "localhost";  

///**
// * The domain name or IP address of the host machine that this Source 
// * represents and from which the data will stream. 
// */
//private String sourceHostName = DEFAULT_SOURCE_HOST_NAME;
//
///*
// *  A default source TCP port for the source sensor data
// */  
//private final int DEFAULT_SOURCE_HOST_PORT  = 5168;
//
///**
// * The TCP port to connect to on the Source host machine 
// */
//private int sourceHostPort = DEFAULT_SOURCE_HOST_PORT;
//
  /*
   *  A default channel name for the source sensor ASCII data
   */  
  private final String DEFAULT_CHANNEL_NAME  = "DecimalASCIISampleData";
  
  /**
   * The RBNB channel name for the ASCII data
   */
  private String rbnbChannelName = DEFAULT_CHANNEL_NAME;
//
///**
// * The number of bytes in the ensemble as each byte is read from the stream
// */
//private int sampleByteCount = 0;
//
///*
// * An integer value indicating the execution state.  This is used by the 
// * execute() method while parsing the stream of bytes from the instrument
// */
//protected int state = 0;
//
///*
// * A boolean field indicating if the instrument connection is ready to stream
// */
//private boolean readyToStream = false;
//
//
///*
// * The thread that is run for streaming data from the instrument
// */
//private Thread streamingThread;
//
///*
// * The socket used to establish UDP communication with the instrument
// */
//private DatagramSocket datagramSocket;
//
///*
// * The datagram object used to represent an individual incoming UDP packet
// */
//private DatagramPacket datagramPacket;
//
///*
// * An internal Thread setting used to specify how long, in milliseconds, the
// * execution of the data streaming Thread should wait before re-executing
// * 
// * @see execute()
// */
//private final int RETRY_INTERVAL = 5000;
  
  /** 
   * The date format for the timestamp applied to the LOOP sample 04 Aug 2008 09:15:01
   */
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
  
  /**
   * The timezone used for the sample date
   */
  private static final TimeZone TZ = TimeZone.getTimeZone("HST");
  
  /*
   * The instance of the AdamParser object used to parse the binary LOOP
   * data packet and retrieve each of the data fields
   */
   private AdamParser adamParser = null;
   
   /**
    * Constructor - create an empty instance of the AdamSource object, using
    * default values for the RBNB server name and port, source instrument name
    * and port, archive mode, archive frame size, and cache frame size. 
    */
   public AdamSource() {
   }
   
  /**
   * Constructor - create an instance of the AdamSource object, using the
   * argument values for the source instrument name and port, and the RBNB 
   * server name and port, the archive mode, archive frame size, and cache 
   * frame size.  A frame is created at each call to flush() to an RBNB server,
   * and so the frame sizes below are relative to the number of bytes of data
   * loaded in the ChannelMap that is flushed to the RBNB server.
   *
   * @param sourceHostName   the name or IP address of the source instrument
   * @param sourceHostPort   the TCP port of the source host instrument
   * @param serverName       the name or IP address of the RBNB server 
   * @param serverPort       the TCP port of the RBNB server
   * @param archiveMode      the RBNB archive mode: append, load, create, none
   * @param archiveFrameSize the size, in frames, for the RBNB server to archive
   * @param cacheFrameSize   the size, in frames, for the RBNB server to cache
   * @param rbnbClientName   the unique name of the source RBNB client
   */
  public AdamSource(String serverName, String serverPort, 
                    String archiveMode, int archiveFrameSize, 
                    int cacheFrameSize, String rbnbClientName) {
    
    setServerName(serverName);
    setServerPort(Integer.parseInt(serverPort));
    setArchiveMode(archiveMode);
    setArchiveSize(archiveFrameSize);
    setCacheSize(cacheFrameSize);
    setRBNBClientName(rbnbClientName);
  }

  /**
   * A method that processes the data ByteBuffer passed in for the given IP
   * address of the ADAM sensor, parses the binary ADAM data, and flushes the
   * data to the DataTurbine given the sensor properties in the XMLConfiguration
   * passed in.
   *
   * @param datagramAddress - the IP address of the datagram of this packet of data
   * @param xmlConfig       - the XMLConfiguration object containing the list of
   *                          sensor properties
   * @param sampleBuffer    - the binary data sample as a ByteBuffer
   */
  protected boolean process(String datagramAddress, XMLConfiguration xmlConfig,
                            ByteBuffer sampleBuffer) {
    
    logger.debug("AdamSource.process() called.");
    // do not execute the stream if there is no connection
    if (  !isConnected() ) return false;
    
    boolean failed = false;
    
    try {
      
      // add channels of data that will be pushed to the server.  
      // Each sample will be sent to the Data Turbine as an rbnb frame.  Information
      // on each channel is found in the XMLConfiguration file (sensors.properties.xml)
      // and the AdamParser object (to get the actual voltages for each ADAM channel)
      ChannelMap rbnbChannelMap     = new ChannelMap(); // used to flush channels
      ChannelMap registerChannelMap = new ChannelMap(); // used to register channels
      int channelIndex = 0;
      
      // Create a buffer that will store the sample bytes as they are read
//      byte[] bufferArray = new byte[getBufferSize()];
      
//    // and a ByteBuffer used to transfer the bytes to the parser
//    ByteBuffer sampleBuffer = ByteBuffer.allocate(getBufferSize());
      
      // bind to the socket, and create a datagram packet to store incoming packets
//    this.datagramSocket = new DatagramSocket(getHostPort());
//    this.datagramPacket = new DatagramPacket(bufferArray, bufferArray.length);
      
//    // Get the sensor channel configuration properties file
//    File xmlConfigFile = new File(this.xmlConfigurationFile);
//    XMLConfiguration xmlConfig = new XMLConfiguration(xmlConfigFile);
      // while there are bytes to read from the socket ...
//    while ( !failed ) {
        
        // receive any incoming UDP packets and parse the data payload
//      datagramSocket.receive(this.datagramPacket);
        
//      logger.debug("Host: " + datagramPacket.getAddress() + 
//                    " data: " + new String(Hex.encodeHex(datagramPacket.getData())));
//      
//      // the address seems to be returned with a leading slash (/). Trim it.
//      String datagramAddress = datagramPacket.getAddress().toString().replaceAll("/", "");
//      
//      sampleBuffer.put(datagramPacket.getData());
        
        this.adamParser = new AdamParser(sampleBuffer);
        
        logger.debug(                                                        "\n" +
          "channelZero       : "  + this.adamParser.getChannelZero()       + "\n" +
          "channelOne        : "  + this.adamParser.getChannelOne()        + "\n" +
          "channelTwo        : "  + this.adamParser.getChannelTwo()        + "\n" +
          "channelThree      : "  + this.adamParser.getChannelThree()      + "\n" +
          "channelFour       : "  + this.adamParser.getChannelFour()       + "\n" +
          "channelFive       : "  + this.adamParser.getChannelFive()       + "\n" +
          "channelSix        : "  + this.adamParser.getChannelSix()        + "\n" +
          "channelSeven      : "  + this.adamParser.getChannelSeven()      + "\n" +
          "channelAverage    : "  + this.adamParser.getChannelAverage()    + "\n" +
          "channelZeroMax    : "  + this.adamParser.getChannelZeroMax()    + "\n" +
          "channelOneMax     : "  + this.adamParser.getChannelOneMax()     + "\n" +
          "channelTwoMax     : "  + this.adamParser.getChannelTwoMax()     + "\n" +
          "channelThreeMax   : "  + this.adamParser.getChannelThreeMax()   + "\n" +
          "channelFourMax    : "  + this.adamParser.getChannelFourMax()    + "\n" +
          "channelFiveMax    : "  + this.adamParser.getChannelFiveMax()    + "\n" +
          "channelSixMax     : "  + this.adamParser.getChannelSixMax()     + "\n" +
          "channelSevenMax   : "  + this.adamParser.getChannelSevenMax()   + "\n" +
          "channelAverageMax : "  + this.adamParser.getChannelAverageMax() + "\n" +
          "channelZeroMin    : "  + this.adamParser.getChannelZeroMin()    + "\n" +
          "channelOneMin     : "  + this.adamParser.getChannelOneMin()     + "\n" +
          "channelTwoMin     : "  + this.adamParser.getChannelTwoMin()     + "\n" +
          "channelThreeMin   : "  + this.adamParser.getChannelThreeMin()   + "\n" +
          "channelFourMin    : "  + this.adamParser.getChannelFourMin()    + "\n" +
          "channelFiveMin    : "  + this.adamParser.getChannelFiveMin()    + "\n" +
          "channelSixMin     : "  + this.adamParser.getChannelSixMin()     + "\n" +
          "channelSevenMin   : "  + this.adamParser.getChannelSevenMin()   + "\n" +
          "channelAverageMin : "  + this.adamParser.getChannelAverageMin() + "\n"
        
        );
        
        // create a TreeMap to hold the voltageChannel and its associated
        // RBNB ChannelMap channel string.  When the RBNB ChannelMap is
        // populated, this TreeMap will be consulted
        TreeMap<Integer, String> voltageChannelTreeMap = new TreeMap();
        
        // create a character string to store characters from the voltage values
        StringBuilder decimalASCIISampleData = new StringBuilder();
        
        // Create a list of sensors from the properties file, and iterate through
        // the list, matching the datagram IP address to the address in the 
        // xml configuration file.  If there is a match, find the correct voltage
        // channel to measurement mappings, create a corresponding RBNB channel
        // map, and flush the data to the DataTurbine.        
        
        List sensorList  = xmlConfig.getList("sensor.address");
        
        // declare the properties that will be pulled from the 
        // sensor.properties.xml file
        String address        = "";
        String sourceName     = "";
        String description    = "";
        String type           = "";
        String cacheSize      = "";
        String archiveSize    = "";
        String archiveChannel = "";
        String portNumber     = "";
        String voltageChannel = "";
        String measurement    = "";
        
        // evaluate each sensor listed in the sensor.properties.xml file
        for ( Iterator sIterator = sensorList.iterator(); sIterator.hasNext(); ) {
          
          // get each property value of the sensor
          int index = sensorList.indexOf(sIterator.next());
          address     = (String) xmlConfig.getProperty("sensor(" + index + ").address" );
          sourceName  = (String) xmlConfig.getProperty("sensor(" + index + ").name" );
          description = (String) xmlConfig.getProperty("sensor(" + index + ").description" );
          type        = (String) xmlConfig.getProperty("sensor(" + index + ").type" );
          
          
          logger.debug("Sensor details:"                      + 
                       "\n\t\t\t\t\t\t\t\t\t\taddress     : " + address +
                       "\n\t\t\t\t\t\t\t\t\t\tname        : " + sourceName +
                       "\n\t\t\t\t\t\t\t\t\t\tdescription : " + description +
                       "\n\t\t\t\t\t\t\t\t\t\ttype        : " + type
                       );
          
          // move to the next sensor if this doesn't match the RBNB source name
          if ( !sourceName.equals(getRBNBClientName()) ) {
            continue;
          }
          
          List   portList  = xmlConfig.getList("sensor(" + index + ").ports.port[@number]");
          // get each port of the sensor, along with the port properties
          for ( Iterator pIterator = portList.iterator(); pIterator.hasNext(); ) {
            int pindex = portList.indexOf(pIterator.next());
            
            // get the port number value
            portNumber = (String) xmlConfig.getProperty("sensor("    +
                                  index                              +
                                  ").ports.port("                    +
                                  pindex                             +
                                  ")[@number]");
                                  
            logger.debug("\tport " + portNumber + " details:");
            
            List measurementList = xmlConfig.getList("sensor("       +
                                                     index           +
                                                     ").ports.port(" +
                                                     pindex          +
                                                     ").measurement[@label]");
            
            // get each measurement and voltageChannel for the given port
            for ( Iterator mIterator = measurementList.iterator(); mIterator.hasNext(); ) {
              int mindex = measurementList.indexOf(mIterator.next());
              
              // build the property paths into the config file
              String voltagePath      = "sensor("        +
                                        index            +
                                        ").ports.port("  +
                                        pindex           +
                                        ").measurement(" +
                                        mindex           +
                                        ").voltageChannel";
                                   
              String measurementPath = "sensor("         +
                                       index             +
                                       ").ports.port("   +
                                       pindex            +
                                       ").measurement("  +
                                       mindex            +
                                       ")[@label]";
              
              // get the voltageChannel and measurement label values
              voltageChannel = (String) xmlConfig.getProperty(voltagePath);
              measurement    = (String) xmlConfig.getProperty(measurementPath);
              logger.debug("\t\t"                     + 
                           "voltageChannel: "         + 
                           voltageChannel             + 
                           "\n\t\t\t\t\t\t\t\t\t\t\t" + 
                           "measurement label: "      + 
                           measurement
                           );
              
              // Match the datagram address with the address in the xmlConfig file
              if ( datagramAddress.equals(address) ) {
                
                // and only add channel data for this class instance RBNB Source name
                if ( sourceName.equals(getRBNBClientName()) ) {
                  
                  // create an Integer out of the voltageChannel
                  Integer voltageChannelInt = new Integer(voltageChannel);
                  // build the RBNB channel path string
                  String channelPath = "port" + "/" + portNumber + "/" + measurement;
                  voltageChannelTreeMap.put(voltageChannelInt, channelPath);
                  
                } else {
                logger.debug("\t\tSource names don't match: " + sourceName + " != " + getRBNBClientName());
                
                } // end sourceName if() statement
              
              } else {
                logger.debug("\t\tNo IP address match. " + datagramAddress + " != " + address);
              
              } //end IP address if() statement
            } // end for each channel
          } // end for each port
          
          // now that we've found the correct sensor, exit the sensor loop
          break;
          
        } // end for each sensor
        
        // Build the RBNB channel map from the entries in the tree map
        // by doing a lookup of the ADAM voltage channel values based
        // on the voltage channel number in the treemap.  Also add the voltages
        // to the DecimalASCIISampleData string (and then channel)
        for ( Iterator vcIterator = voltageChannelTreeMap.keySet().iterator(); vcIterator.hasNext(); ) {
        
          int voltageChannelFromMap = ((Integer) vcIterator.next()).intValue();
          String channelPathFromMap = voltageChannelTreeMap.get(voltageChannelFromMap);
          float voltageValue = -9999.0f;
        
          // look up the voltage value from the AdamParser object based
          // on the voltage channel set in the xmlConfig file (via the treemap)
          switch ( voltageChannelFromMap ) {
            case 0:
              voltageValue = this.adamParser.getChannelZero();
              break;
            case 1:
              voltageValue = this.adamParser.getChannelOne();
              break;
            case 2:
              voltageValue = this.adamParser.getChannelTwo();
              break;
            case 3:
              voltageValue = this.adamParser.getChannelThree();
              break;
            case 4:
              voltageValue = this.adamParser.getChannelFour();
              break;
            case 5:
              voltageValue = this.adamParser.getChannelFive();
              break;
            case 6:
              voltageValue = this.adamParser.getChannelSix();
              break;
            case 7:
              voltageValue = this.adamParser.getChannelSeven();
              break;
          }
        
          // now add the channel and the voltage value to the RBNB channel maps
        
          channelIndex = registerChannelMap.Add(channelPathFromMap);
          registerChannelMap.PutUserInfo(channelIndex, "units=volts");
          
          logger.debug("Voltage Channel Tree Map: " + voltageChannelTreeMap.toString());
          
          // then the channel and voltage
          channelIndex = rbnbChannelMap.Add(channelPathFromMap);
          rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");
          rbnbChannelMap.PutDataAsFloat32(channelIndex, new float[]{voltageValue});
          decimalASCIISampleData.append(String.format("%05.3f", (Object) voltageValue) + ", ");
          
        }
        
        // and only flush data for this class instance RBNB Source name
        if ( sourceName.equals(getRBNBClientName()) && datagramAddress.equals(address) ) {
        
          // add the timestamp to the rbnb channel map
          registerChannelMap.PutTimeAuto("server");
          rbnbChannelMap.PutTimeAuto("server");
          
          // then add a timestamp to the end of the ASCII version of the sample
          DATE_FORMAT.setTimeZone(TZ);
          String sampleDateAsString = DATE_FORMAT.format(new Date()).toString();
          decimalASCIISampleData.append(sampleDateAsString);
          decimalASCIISampleData.append("\n");
          
          // add the DecimalASCIISampleData channel to the channelMap
          channelIndex = rbnbChannelMap.Add(getRBNBChannelName());
          rbnbChannelMap.PutMime(channelIndex, "text/plain");
          rbnbChannelMap.PutDataAsString(channelIndex, decimalASCIISampleData.toString());
          
          // Now register the RBNB channels, and flush the rbnbChannelMap to the
          // DataTurbine
          getSource().Register(registerChannelMap);
          getSource().Flush(rbnbChannelMap);
          logger.info("Sample sent to the DataTurbine: " + decimalASCIISampleData.toString());
          registerChannelMap.Clear();
          rbnbChannelMap.Clear();
          
          sampleBuffer.clear();
        } else {
          logger.debug("\t\tSource names don't match: " + sourceName + " != " + getRBNBClientName());
          registerChannelMap.Clear();
          rbnbChannelMap.Clear();
          
          sampleBuffer.clear();
        }
        
//      
//    } // end while (more socket bytes to read)
              
//  } catch ( IOException e ) {
//    // handle exceptions
//    // In the event of an i/o exception, log the exception, and allow execute()
//    // to return false, which will prompt a retry.
//    failed = true;
//    e.printStackTrace();
//    return !failed;
//  } catch ( ConfigurationException ce ) {
//    // handle exceptions
//    // In the event of an configuration exception, log the exception, and allow execute()
//    // to return false, which will prompt a retry.
//    failed = true;
//    ce.printStackTrace();
//    return !failed;
//  
    } catch ( SAPIException sapie ) {
      // In the event of an RBNB communication  exception, log the exception, 
      // and allow execute() to return false, which will prompt a retry.
      failed = true;
      sapie.printStackTrace();
      return !failed;
      
    }
    
    return !failed;
  } // end if (  !isConnected() ) 
  
  /**
   * A method that sets the size, in bytes, of the ByteBuffer used in streaming 
   * data from a source instrument via a TCP connection
   */
   public int getBufferSize() {
     return this.bufferSize;
   }
   
///**
// * A method that returns the domain name or IP address of the source 
// * instrument (i.e. the serial-to-IP converter to which it is attached)
// */
//public String getHostName(){
//  return this.sourceHostName;
//}
//
///**
// * A method that returns the TCP port of the source 
// * instrument (i.e. the serial-to-IP converter to which it is attached)
// */
//public int getHostPort(){
//  return this.sourceHostPort;
//}
//
  /**
   * A method that returns the name of the RBNB channel that contains the 
   * streaming data from this instrument
   */
  public String getRBNBChannelName(){
    return this.rbnbChannelName;
  }

  /**
   * A method that returns the versioning info for this file.  In this case, 
   * it returns a String that includes the Subversion LastChangedDate, 
   * LastChangedBy, LastChangedRevision, and HeadURL fields.
   */

  public String getCVSVersionString(){
    return (
    "$LastChangedDate: 2009-06-19 16:37:18 -0600 (Fri, 19 Jun 2009) $" +
    "$LastChangedBy: cjones $" +
    "$LastChangedRevision: 398 $" +
    "$HeadURL: https://bbl.ancl.hawaii.edu/projects/bbl/trunk/src/java/edu/hawaii/soest/kilonalu/adam/AdamSource.java $"
    );
  }

///**
// * A method that returns true if the RBNB connection is established
// * and if the data streaming Thread has been started
// */
//public boolean isRunning() {
//  // return the connection status and the thread status
//  return ( isConnected() && readyToStream );
//}
//
///**
// * The main method for running the code
// * @ param args[] the command line list of string arguments, none are needed
// */
//
//public static void main (String args[]) {
//  
//  logger.info("AdamSource.main() called.");
//  
//  try {
//    // create a new instance of the AdamSource object, and parse the command 
//    // line arguments as settings for this instance
//    final AdamSource adamSource = new AdamSource();
//    
//    // Set up a simple logger that logs to the console
//    PropertyConfigurator.configure(adamSource.getLogConfigurationFile());
//    
//    // parse the commandline arguments to configure the connection, then 
//    // start the streaming connection between the source and the RBNB server.
//    if ( adamSource.parseArgs(args) ) {
//      adamSource.start();
//    }
//    
//    // Handle ctrl-c's and other abrupt death signals to the process
//    Runtime.getRuntime().addShutdownHook(new Thread() {
//      // stop the streaming process
//      public void run() {
//        adamSource.stop();
//      }
//    }
//    );
//    
//  } catch ( Exception e ) {
//    logger.info("Error in main(): " + e.getMessage());
//    e.printStackTrace();
//  }
//}
//
///*
// * A method that runs the data streaming work performed by the execute()
// * by handling execution problems and continuously trying to re-execute after 
// * a specified retry interval for the thread.
// */
//private void runWork() {
//  
//  // handle execution problems by retrying if execute() fails
//  boolean retry = true;
//  
//  while ( retry ) {
//    
//    // connect to the RBNB server
//    if ( connect() ) {
//      // run the data streaming code
//      retry = !execute();
//    }
//    
//    disconnect();
//    
//    if ( retry ) {
//      try {
//        Thread.sleep(RETRY_INTERVAL);
//      } catch ( Exception e ){
//        logger.info("There was an execution problem. Retrying. Message is: " +
//        e.getMessage());
//      }
//    }
//  }
//  // stop the streaming when we are done
//  stop();
//}
//

  /**
   * A method that starts the connection with the RBNB DataTurbine
   */
  public boolean startConnection() {
    return connect();
  }
   
  /**
   * A method that starts the connection with the RBNB DataTurbine
   */
  public void stopConnection() {
    disconnect();
  }
   
  /**
   * A method that sets the command line arguments for this class.  This method 
   * calls the <code>RBNBSource.setBaseArgs()</code> method.
   * 
   * @param command  The CommandLine object being passed in from the command
   */
  protected boolean setArgs(CommandLine command) {
    
//  // first set the base arguments that are included on the command line
//  if ( !setBaseArgs(command)) {
//    return false;
//  }
//  
//  // add command line arguments here
//  
//  // handle the -H option
//  if ( command.hasOption("H") ) {
//    String hostName = command.getOptionValue("H");
//    if ( hostName != null ) {
//      setHostName(hostName);
//    }
//  }
//
//  // handle the -P option, test if it's an integer
//  if ( command.hasOption("P") ) {
//    String hostPort = command.getOptionValue("P");
//    if ( hostPort != null ) {
//      try {
//        setHostPort(Integer.parseInt(hostPort));
//        
//      } catch ( NumberFormatException nfe ){
//        logger.info("Error: Enter a numeric value for the host port. " +
//                           hostPort + " is not a valid number.");
//        return false;
//      }
//    }
//  }
//
//  // handle the -C option
//  if ( command.hasOption("C") ) {
//    String channelName = command.getOptionValue("C");
//    if ( channelName != null ) {
//      setChannelName(channelName);
//    }
//  }
//
    return true;
  }

  /**
   * A method that sets the size, in bytes, of the ByteBuffer used in streaming 
   * data from a source instrument via a TCP connection
   *
   * @param bufferSize  the size, in bytes, of the ByteBuffer
   */
  public void setBuffersize(int bufferSize) {
    this.bufferSize = bufferSize;
  }
  
///**
// * A method that sets the domain name or IP address of the source 
// * instrument (i.e. the serial-to-IP converter to which it is attached)
// *
// * @param hostName  the domain name or IP address of the source instrument
// */
//public void setHostName(String hostName) {
//  this.sourceHostName = hostName;
//}
//
///**
// * A method that sets the TCP port of the source 
// * instrument (i.e. the serial-to-IP converter to which it is attached)
// *
// * @param hostPort  the TCP port of the source instrument
// */
//public void setHostPort(int hostPort) {
//  this.sourceHostPort = hostPort;
//}

  /**
   * A method that sets the RBNB channel name of the source instrument's data
   * stream
   *
   * @param channelName  the name of the RBNB channel being streamed
   */
  public void setChannelName(String channelName) {
    this.rbnbChannelName = channelName;
  }

  /**
   * A method that sets the command line options for this class.  This method 
   * calls the <code>RBNBSource.setBaseOptions()</code> method in order to set
   * properties such as the sourceHostName, sourceHostPort, serverName, and
   * serverPort.
   */
  protected Options setOptions() {
    Options options = setBaseOptions(new Options());
//  
//  // Note: 
//  // Command line options already provided by RBNBBase include:
//  // -h "Print help"
//  // -s "RBNB Server Hostname"
//  // -p "RBNB Server Port Number"
//  // -S "RBNB Source Name"
//  // -v "Print Version information"
//  
//  // Command line options already provided by RBNBSource include:
//  // -z "Cache size"
//  // -Z "Archive size"
//  
//  // add command line options here
//  options.addOption("H", true, "Source host name or IP *" + getHostName());
//  options.addOption("P", true, "Source host port number *" + getHostPort());    
//  options.addOption("C", true, "RBNB source channel name *" + getRBNBChannelName());
//  //options.addOption("M", true, "RBNB archive mode *" + getArchiveMode());    
                      
    return options;
  }

///**
// * A method that starts the streaming of data from the source instrument to
// * the RBNB server via an established TCP connection.  
// */
//public boolean start() {
//  
//  // return false if the streaming is running
//  if ( isRunning() ) {
//    return false;
//  }
//  
//  // reset the connection to the RBNB server
//  if ( isConnected() ) {
//    disconnect();
//  }
//  connect();
//  
//  // return false if the connection fails
//  if ( !isConnected() ) {
//    return false;
//  }
//  
//  // begin the streaming thread to the source
//  startThread();
//  
//  return true;  
//}
//
///**
// * A method that creates and starts a new Thread with a run() method that 
// * begins processing the data streaming from the source instrument.
// */
//private void startThread() {
//  
//  // build the runnable class and implement the run() method
//  Runnable runner = new Runnable() {
//    public void run() {
//      runWork();
//    }
//  };
//  
//  // build the Thread and start it, indicating that it has been started
//  readyToStream = true;
//  streamingThread = new Thread(runner, "StreamingThread");
//  streamingThread.start();     
//}
//
///**
// * A method that stops the streaming of data between the source instrument and
// * the RBNB server.  
// */ 
//public boolean stop() {
//  
//  // return false if the thread is not running
//  if ( !isRunning() ) {
//    return false;
//  }
//  
//  // stop the thread and disconnect from the RBNB server
//  stopThread();
//  disconnect();
//  return true;
//}
//
///**
// * A method that interrupts the thread created in startThread()
// */
//private void stopThread() {
//  // set the streaming status to false and stop the Thread
//  readyToStream = false;
//  streamingThread.interrupt();
//}
//
///**
// * A method that gets the log configuration file location
// *
// * @return logConfigurationFile  the log configuration file location
// */
//public String getLogConfigurationFile() {
//  return this.logConfigurationFile;
//}
//
///**
// * A method that sets the log configuration file name
// *
// * @param logConfigurationFile  the log configuration file name
// */
//public void setLogConfigurationFile(String logConfigurationFile) {
//  this.logConfigurationFile = logConfigurationFile;
//}
//
}
