/**
 *  Copyright: 2007 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: To convert a Seacat ASCII data source into RBNB Data Turbine
 *             frames for archival and realtime access.
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
package edu.hawaii.soest.kilonalu.ctd;

import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.Source;
import com.rbnb.sapi.SAPIException;

import edu.hawaii.soest.kilonalu.ctd.CTDParser;
import edu.hawaii.soest.kilonalu.utilities.SerialChannel;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;

import java.lang.InterruptedException;
import java.lang.StringBuffer;

import java.io.PrintWriter;
import java.io.InputStream;
import java.io.OutputStream;
//import java.io.DataInputStream;
import java.io.IOException;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;

import org.apache.commons.codec.binary.Hex;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

import org.nees.rbnb.RBNBBase;
import org.nees.rbnb.RBNBSource;

/**
 * A simple class used to harvest a decimal ASCII data stream from a Seacat 
 * 16plus CTD) over either 1) a TCP socket connection with a Raven XT cellular 
 * modem and serial2ip converter host or, 2) a local serial port. 
 * The data stream is then converted into RBNB frames 
 * and pushed into the RBNB DataTurbine real time server.  This class extends 
 * org.nees.rbnb.RBNBSource, which in turn extends org.nees.rbnb.RBNBBase, 
 * and therefore follows the API conventions found in the org.nees.rbnb code.  
 *
 * The parsing of the data stream relies on the premise that each sample of data
 * is a comma delimited string of values, and that each sample is terminated
 * by two newline characters (\n\n).  Each line is also prefixed by the pound (#)
 * character.
 *
 */
public class CTDSource extends RBNBSource {

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
   *
   * @see setArchiveMode()
   * @see getArchiveMode()
   */
  private String archiveMode = DEFAULT_ARCHIVE_MODE;

  /*
   * The default size of the ByteBuffer used to beffer the TCP stream from the
   * source instrument.
   */  
  private int DEFAULT_BUFFER_SIZE = 8192; // 8K

  /**
   * The size of the ByteBuffer used to beffer the TCP stream from the 
   * instrument.
   */
  private int bufferSize = DEFAULT_BUFFER_SIZE;
  
  /* A default RBNB channel name for the given source instrument */  
  private String DEFAULT_RBNB_CHANNEL = "DecimalASCIISampleData";

  /* The name of the RBNB channel for this data stream */
  private String rbnbChannelName = DEFAULT_RBNB_CHANNEL;
  
  /* A default source IP address for the given source instrument */
  private final String DEFAULT_SOURCE_HOST_NAME = "68.25.65.111";  

  /**
   * The domain name or IP address of the host machine that this Source 
   * represents and from which the data will stream. 
   */
  private String sourceHostName = DEFAULT_SOURCE_HOST_NAME;

  /* A default source TCP port for the given source instrument */  
  private final int DEFAULT_SOURCE_HOST_PORT  = 5111;

  /* The TCP port to connect to on the Source host machine */
  private int sourceHostPort = DEFAULT_SOURCE_HOST_PORT;

  /** The default serial port name used for serial communications */
  private static String DEFAULT_SERIAL_PORT = "/dev/ttyS0";
  
  /** The  serial port name used for serial communications */
  private String serialPortName = DEFAULT_SERIAL_PORT;
  
  /* The number of bytes in the ensemble as each byte is read from the stream */
  private int sampleByteCount = 0;
  
  /* The default log configuration file location */
  private final String DEFAULT_LOG_CONFIGURATION_FILE = "lib/log4j.properties";

  /* The log configuration file location */
  private String logConfigurationFile = DEFAULT_LOG_CONFIGURATION_FILE;
  
  /* The Logger instance used to log system messages */
  private static Logger logger = Logger.getLogger(CTDSource.class);

  protected int state = 0;
  
  private boolean readyToStream = false;
  
  private Thread streamingThread;
  
  /* A field indicating if the connection is serial or socket */
  private String connectionType;
  
  /* A field indicating if the CTD query output type is xml or text */
  private String outputType;
  
  /* A boolean field indicating if a command has been sent to the instrument */
  private boolean sentCommand = false;
  
  /* The command prefix used to send commands to the instrument */ 
  private String commandPrefix = "";
  
  /* The command suffix used to send commands to the instrument */ 
  private String commandSuffix = "\r\n";
  
  /* The default sampling interval in samples per hour (i.e every 4 minutes) */
  private final String DEFAULT_SAMPLE_INTERVAL = "15";
  
  /* The sampling interval in samples per hour */
  private final String sampleInterval = DEFAULT_SAMPLE_INTERVAL;
  
  /* The command used to set the sampling interval on the instrument */ 
  private String sampleIntervalCommand = "SampleInterval=";
  
  /* The command used to start the autonomous sampling on the instrument */ 
  private String startSamplingCommand = "StartNow";
  
  /* The command used to stop the autonomous sampling on the instrument */ 
  private String stopSamplingCommand = "Stop";
  
  /* The command used to set the observation datetime on the instrument */ 
  private String setDateTimeCommand = "DateTime="; // use mmddyyyyhhmmss
  
  /* The legacy command to display status information from the instrument */ 
  private String displayStatusCommand = "DS";
  
  /* The legacy command to display calibration information from the instrument */ 
  private String displayCalibrationCommand = "DCal";
  
  /* The command to get status information from the instrument */ 
  private String getStatusCommand = "GetSD";
  
  /* The command to get configuration information from the instrument */ 
  private String getConfigurationCommand = "GetCD";
  
  /* The command to get calibration information from the instrument */ 
  private String getCalibrationCommand = "GetCC";
  
  /* The command to get event information from the instrument */ 
  private String getEventsCommand = "GetED";
  
  /* The command to get hardware information from the instrument */ 
  private String getHardwareCommand = "GetHD";
  
  /* A boolean field that indicates if all CTDParser metadata fields are set */
  private boolean hasMetadata = false;
  
  /* A boolean field that indicates if sampling has been stopped on the instrument */
  private boolean samplingIsStopped = false;
  
  /* The command used to query or command the instrument */
  private String command;
  
  /* A boolean stating if the instrument clock is synced to NTP time */
  private boolean clockIsSynced = false;
  
  /* The datetime when the instrument clock was last synced to NTP time */
  private Date clockSyncDate;
  
  /* The date format for the timestamp applied to instrument clock */
  private static final SimpleDateFormat DATE_FORMAT = 
    new SimpleDateFormat("ddMMyyyyHHmmss");
  
  /* The timezone used for the sample date */
  private static final TimeZone TZ = TimeZone.getTimeZone("HST");
    
  /* The socket or file channel used for instrument communication */
  private ByteChannel channel;
  
  /* The instance of the CTD Parser class used to parse CTD output */
  private CTDParser ctdParser;
  
  /* The response string used as the output variable from the CTD */
  String responseString;
  
  /*
   * An internal Thread setting used to specify how long, in milliseconds, the
   * execution of the data streaming Thread should wait before re-executing
   * 
   * @see execute()
   */
  private final int RETRY_INTERVAL = 5000;
    
  /**
   * Constructor - create an empty instance of the CTDSource object, using
   * default values for the RBNB server name and port, source instrument name
   * and port, archive mode, archive frame size, and cache frame size. 
   */
  public CTDSource() {
  }

  /**
   * Constructor - create an instance of the CTDSource object, using the
   * argument values for the source instrument name and port, and the RBNB 
   * server name and port.  This constructor will use default values for the
   * archive mode, archive frame size, and cache frame size. 
   *
   * @param sourceHostName  the name or IP address of the source instrument
   * @param sourceHostPort  the TCP port of the source host instrument
   * @param serverName      the name or IP address of the RBNB server connection
   * @param serverPort      the TCP port of the RBNB server
   */
  public CTDSource(String sourceHostName, String sourceHostPort, 
                      String serverName, String serverPort) {
    
    setHostName(sourceHostName);
    setHostPort(Integer.parseInt(sourceHostPort));
    setServerName(serverName);
    setServerPort(Integer.parseInt(serverPort));
  }

  /**
   * Constructor - create an instance of the CTDSource object, using the
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
  public CTDSource(String sourceHostName, String sourceHostPort, 
                      String serverName, String serverPort, 
                      String archiveMode, int archiveFrameSize, 
                      int cacheFrameSize, String rbnbClientName) {
    
    setHostName(sourceHostName);
    setHostPort(Integer.parseInt(sourceHostPort));
    setServerName(serverName);
    setServerPort(Integer.parseInt(serverPort));
    setArchiveMode(archiveMode);
    setArchiveSize(archiveFrameSize);
    setCacheSize(cacheFrameSize);
    setRBNBClientName(rbnbClientName);
  }

  /**
   * A method that executes the streaming of data from the source to the RBNB
   * server after all configuration of settings, connections to hosts, and
   * thread initiatizing occurs.  This method contains the detailed code for 
   * streaming the data and interpreting the stream.
   */
  protected boolean execute() {
    logger.debug("CTDSource.execute() called.");
    
    // do not execute the stream if there is no connection
    if (  !isConnected() ) return false;
    
    boolean failed = false;

    this.hasMetadata = true;
    
    // test the connection type
    if ( this.connectionType.equals("serial") ) {
      
      // create a serial connection to the local serial port
      this.channel = getSerialConnection();
      
    } else if ( this.connectionType.equals("socket") ) {
      
      // otherwise create a TCP or UDP socket connection to the remote host
      this.channel = getSocketConnection();
      
    } else {
      logger.info("There was an error establishing either a serial or " +
                  "socket connection to the instrument.  Please be sure " +
                  "the connection type is set to either 'serial' or 'socket'.");
      return false;
      
    }
    
    // while data are being sent, read them into the buffer
    try {
      // create four byte placeholders used to evaluate up to a four-byte 
      // window.  The FIFO layout looks like:
      //           -------------------------
      //   in ---> | One | Two |Three|Four |  ---> out
      //           -------------------------
      byte byteOne   = 0x00,   // set initial placeholder values
           byteTwo   = 0x00,
           byteThree = 0x00,
           byteFour  = 0x00;
      
      // Create a buffer that will store the sample bytes as they are read
      ByteBuffer sampleBuffer = ByteBuffer.allocate(getBufferSize());
      
      // Declare sample variables to be used in the response parsing
      byte[] sampleArray;
      
      // create a byte buffer to store bytes from the TCP stream
      ByteBuffer buffer = ByteBuffer.allocateDirect(getBufferSize());
      
      // add a channel of data that will be pushed to the server.  
      // Each sample will be sent to the Data Turbine as an rbnb frame.
      ChannelMap rbnbChannelMap = new ChannelMap();
            
      // while there are bytes to read from the channel ...
      while ( this.channel.read(buffer) != -1 || buffer.position() > 0) {

        // prepare the buffer for reading
        buffer.flip();          
    
        // while there are unread bytes in the ByteBuffer
        while ( buffer.hasRemaining() ) {
          byteOne = buffer.get();
          logger.debug(
            "b1: " + new String(Hex.encodeHex((new byte[]{byteOne})))   + "\t" + 
            "b2: " + new String(Hex.encodeHex((new byte[]{byteTwo})))   + "\t" + 
            "b3: " + new String(Hex.encodeHex((new byte[]{byteThree}))) + "\t" + 
            "b4: " + new String(Hex.encodeHex((new byte[]{byteFour})))  + "\t" +
            "sample pos: "   + sampleBuffer.position()                  + "\t" +
            "sample rem: "   + sampleBuffer.remaining()                 + "\t" +
            "sample cnt: "   + sampleByteCount                          + "\t" +
            "buffer pos: "   + buffer.position()                        + "\t" +
            "buffer rem: "   + buffer.remaining()                       + "\t" +
            "state: "        + this.state
          );
          
          // Use a State Machine to process the byte stream.
          // Start building an rbnb frame for the entire sample, first by 
          // inserting a timestamp into the channelMap.  This time is merely
          // the time of insert into the data turbine, not the time of
          // observations of the measurements.  That time should be parsed out
          // of the sample in the Sink client code
    
          switch( this.state ) {
            
            case 0:  // wake up the instrument
              
              // check for instrument metadata fields
              if ( this.hasMetadata ) {
                this.state = 10;
                break;
                
              } else {
                
                // wake the instrument with two initial '\r\n' commands
                this.command = this.commandSuffix;
                this.sentCommand = queryInstrument(this.command);
                streamingThread.sleep(2000);
                this.command = this.commandSuffix;
                this.sentCommand = queryInstrument(this.command);
                
                this.state = 1;
                break;
                
              }
            
            case 1: // stop the sampling
              
              // allow time for the instrument response
              streamingThread.sleep(2000);
              this.command = this.commandPrefix       + 
                             this.stopSamplingCommand +
                             this.commandSuffix;
              this.sentCommand = queryInstrument(command);        
              
              if ( this.sentCommand ) {
                
                this.samplingIsStopped = true;
                
                // for newer firmware CTDs, use xml-based query commands
                if ( getOutputType().equals("xml") ) {
                  // create the CTD parser instance used to parse CTD output
                  this.ctdParser = new CTDParser();
                  this.state = 2;
                  break;
                
                // otherwise, use text-based query commands
                } else if ( getOutputType().equals("text") ) {
                  this.state = 12; // process DS and DCal commands
                  break;
                  
                } else {
                  logger.info("The CTD output type is not recognized. " +
                              "Please set the output type to either "   +
                              "'xml' or 'text'.");
                  failed = true;
                  return !failed;
                  
                }
                
              } else {
                break; // try stopping the instrument sampling again
                
              }
              
            case 2: // get the instrument status metadata
               
              if ( !this.ctdParser.getHasStatusMetadata() ) {
                
                this.command = this.commandPrefix    +
                               this.getStatusCommand +
                               this.commandSuffix;
                this.sentCommand = queryInstrument(command);        
                streamingThread.sleep(5000);
                this.state = 3;
                break;
                
              } else {
                
                // get the configuration metadata
                this.command = this.commandPrefix           + 
                               this.getConfigurationCommand +
                               this.commandSuffix;
                this.sentCommand = queryInstrument(command);        
                streamingThread.sleep(5000);
                this.state = 4;
                break;
                
              }
              
            case 3: // handle instrument status response
              
              // command response ends with <Executed/> (so find: ed/>)
              if ( byteOne == 0x3E   && byteTwo == 0x2F && 
                   byteThree == 0x64 && byteFour == 0x65 ) {
                
                // handle instrument status response
                sampleByteCount++; // add the last byte found to the count
                
                // add the last byte found to the sample buffer
                if ( sampleBuffer.remaining() > 0 ) {
                  sampleBuffer.put(byteOne);
                
                } else {
                  sampleBuffer.compact();
                  sampleBuffer.put(byteOne);
                  
                }
                
                // extract the sampleByteCount length from the sampleBuffer
                sampleArray = new byte[sampleByteCount];
                sampleBuffer.flip();
                logger.debug("sampleBuffer: " + sampleBuffer.toString());
                sampleBuffer.get(sampleArray);
                this.responseString = new String(sampleArray, "US-ASCII");
                
                // set the CTD metadata
                int executedIndex = this.responseString.indexOf("<Executed/>");
                this.responseString = this.responseString.substring(0, executedIndex - 1);
                
                this.ctdParser.setMetadata(this.responseString);
                
                // reset variables for the next sample
                sampleBuffer.clear();
                sampleByteCount = 0;
                
                // then get the instrument configuration metadata
                if ( !this.ctdParser.getHasConfigurationMetadata() ) {

                  this.command = this.commandPrefix           + 
                                 this.getConfigurationCommand +
                                 this.commandSuffix;
                  this.sentCommand = queryInstrument(command);        
                  streamingThread.sleep(5000);
                  this.state = 4;
                  break;

                } else {
                  
                  // get the calibration metadata
                  this.command = this.commandPrefix         + 
                                 this.getCalibrationCommand +
                                 this.commandSuffix;
                  this.sentCommand = queryInstrument(command);        
                  streamingThread.sleep(5000);
                  this.state = 5;
                  break;

                }
                
              } else {
                break; // continue reading bytes
                
              }
              
            case 4: // handle the instrument configuration metadata
              
              // command response ends with <Executed/> (so find: ed/>)
              if ( byteOne   == 0x3E && byteTwo  == 0x2F && 
                   byteThree == 0x64 && byteFour == 0x65 ) {
                
                // handle instrument configration response
                sampleByteCount++; // add the last byte found to the count
                
                // add the last byte found to the sample buffer
                if ( sampleBuffer.remaining() > 0 ) {
                  sampleBuffer.put(byteOne);
                
                } else {
                  sampleBuffer.compact();
                  sampleBuffer.put(byteOne);
                  
                }                
                
                // extract the sampleByteCount length from the sampleBuffer
                sampleArray = new byte[sampleByteCount];
                sampleBuffer.flip();
                logger.debug("sampleBuffer: " + sampleBuffer.toString());
                sampleBuffer.get(sampleArray);
                this.responseString = new String(sampleArray, "US-ASCII");
                
                // set the CTD metadata
                int executedIndex = this.responseString.indexOf("<Executed/>");
                this.responseString = this.responseString.substring(0, executedIndex - 1);
                
                this.ctdParser.setMetadata(this.responseString);
                
                // reset variables for the next sample
                sampleBuffer.clear();
                sampleByteCount = 0;
                
                // then get the instrument calibration metadata
                if ( !this.ctdParser.getHasCalibrationMetadata() ) {

                  this.command = this.commandPrefix         + 
                                 this.getCalibrationCommand +
                                 this.commandSuffix;
                  this.sentCommand = queryInstrument(command);        
                  streamingThread.sleep(5000);
                  this.state = 5;
                  break;

                } else {

                  this.command = this.commandPrefix    + 
                                 this.getEventsCommand +
                                 this.commandSuffix;
                  this.sentCommand = queryInstrument(command);        
                  streamingThread.sleep(5000);
                  this.state = 6;
                  break;

                }
                
              } else {
                break; // continue reading bytes
                
              }
              
            case 5: // handle the instrument calibration metadata
              
              // command response ends with <Executed/> (so find: ed/>)
              if ( byteOne   == 0x3E && byteTwo  == 0x2F && 
                   byteThree == 0x64 && byteFour == 0x65 ) {
                
                // handle instrument calibration response
                sampleByteCount++; // add the last byte found to the count
                
                // add the last byte found to the sample buffer
                if ( sampleBuffer.remaining() > 0 ) {
                  sampleBuffer.put(byteOne);
                
                } else {
                  sampleBuffer.compact();
                  sampleBuffer.put(byteOne);
                  
                }                
                
                // extract the sampleByteCount length from the sampleBuffer
                sampleArray = new byte[sampleByteCount];
                sampleBuffer.flip();
                logger.debug("sampleBuffer: " + sampleBuffer.toString());
                sampleBuffer.get(sampleArray);
                this.responseString = new String(sampleArray, "US-ASCII");
                
                // set the CTD metadata
                int executedIndex = this.responseString.indexOf("<Executed/>");
                this.responseString = this.responseString.substring(0, executedIndex - 1);
                
                this.ctdParser.setMetadata(this.responseString);
                
                // reset variables for the next sample
                sampleBuffer.clear();
                sampleByteCount = 0;
                
                // then get the instrument event metadata
                if ( !this.ctdParser.getHasEventMetadata() ) {

                  this.command = this.commandPrefix    + 
                                 this.getEventsCommand +
                                 this.commandSuffix;
                  this.sentCommand = queryInstrument(command);        
                  streamingThread.sleep(5000);
                  this.state = 6;
                  break;

                } else {

                  this.command = this.commandPrefix      +
                                 this.getHardwareCommand +
                                 this.commandSuffix;
                  this.sentCommand = queryInstrument(command);        
                  streamingThread.sleep(5000);
                  this.state = 7;
                  break;

                }
                
              } else {
                break; // continue reading bytes
                
              }
              
            case 6: // handle instrument event metadata
              
              // command response ends with <Executed/> (so find: ed/>)
              if ( byteOne   == 0x3E && byteTwo  == 0x2F && 
                   byteThree == 0x64 && byteFour == 0x65 ) {
                
                // handle instrument events response
                sampleByteCount++; // add the last byte found to the count
                
                // add the last byte found to the sample buffer
                if ( sampleBuffer.remaining() > 0 ) {
                  sampleBuffer.put(byteOne);
                
                } else {
                  sampleBuffer.compact();
                  sampleBuffer.put(byteOne);
                  
                }                
                
                // extract the sampleByteCount length from the sampleBuffer
                sampleArray = new byte[sampleByteCount];
                sampleBuffer.flip();
                logger.debug("sampleBuffer: " + sampleBuffer.toString());
                sampleBuffer.get(sampleArray);
                this.responseString = new String(sampleArray, "US-ASCII");
                
                // set the CTD metadata
                int executedIndex = this.responseString.indexOf("<Executed/>");
                this.responseString = this.responseString.substring(0, executedIndex - 1);
                
                this.ctdParser.setMetadata(this.responseString);
                
                // reset variables for the next sample
                sampleBuffer.clear();
                sampleByteCount = 0;
                
                // then get the instrument hardware metadata
                if ( !this.ctdParser.getHasHardwareMetadata() ) {

                  this.command = this.commandPrefix      +
                                 this.getHardwareCommand +
                                 this.commandSuffix;
                  this.sentCommand = queryInstrument(command);        
                  streamingThread.sleep(5000);
                  this.state = 7;
                  break;

                } else {

                  this.state = 8;
                  break;

                }
                
              } else {
                break; // continue reading bytes
                
              }
              
            case 7: // handle the instrument hardware response
            
              // command response ends with <Executed/> (so find: ed/>)
              if ( byteOne   == 0x3E && byteTwo  == 0x2F && 
                   byteThree == 0x64 && byteFour == 0x65 ) {
                
                // handle instrument hardware response
                sampleByteCount++; // add the last byte found to the count
                
                // add the last byte found to the sample buffer
                if ( sampleBuffer.remaining() > 0 ) {
                  sampleBuffer.put(byteOne);
                
                } else {
                  sampleBuffer.compact();
                  sampleBuffer.put(byteOne);
                  
                }                
                
                // extract the sampleByteCount length from the sampleBuffer
                sampleArray = new byte[sampleByteCount];
                sampleBuffer.flip();
                logger.debug("sampleBuffer: " + sampleBuffer.toString());
                sampleBuffer.get(sampleArray);
                this.responseString = new String(sampleArray, "US-ASCII");
                
                // set the CTD metadata
                int executedIndex = this.responseString.indexOf("<Executed/>");
                this.responseString = this.responseString.substring(0, executedIndex - 1);
                
                this.ctdParser.setMetadata(this.responseString);
                
                // reset variables for the next sample
                sampleBuffer.clear();
                sampleByteCount = 0;
                
                // sync the clock if it is not synced
                if ( !this.clockIsSynced ){
                  
                  this.state = 8;
                  break;
                  
                } else {
                  this.state = 9;
                  break;
                  
                }
                
              } else {
                break; // continue reading bytes
                
              }
              
            case 8: // set the instrument clock
              
              // is sampling stopped?
              if ( !this.samplingIsStopped ) {
                // wake the instrument with an initial '\r\n' command
                this.command = this.commandSuffix;
                this.sentCommand = queryInstrument(this.command);
                streamingThread.sleep(2000);
                
                // then stop the sampling
                this.command = this.commandPrefix     + 
                               this.stopSamplingCommand +
                               this.commandSuffix;
                this.sentCommand = queryInstrument(command);        
                this.samplingIsStopped = true;
                
              }
              
              // now set the clock
              if ( this.sentCommand ) {
                this.clockSyncDate = new Date();
                DATE_FORMAT.setTimeZone(TZ);
                String dateAsString = DATE_FORMAT.format(this.clockSyncDate);
                
                this.command = this.commandPrefix      +
                               this.setDateTimeCommand +
                               dateAsString            +
                               this.commandSuffix;
                this.sentCommand = queryInstrument(command);        
                streamingThread.sleep(5000);
                this.clockIsSynced = true;
                logger.info("The instrument clock has bee synced at " + 
                            this.clockSyncDate.toString());
                this.state = 9;
                break;
                
              } else {
                
                break; // try the clock sync again due to failure
                
              }
              
              
            case 9: // restart the instrument sampling
              
              if ( this.samplingIsStopped ) {
                
                this.hasMetadata = true;
                
                this.command = this.commandPrefix        +
                               this.startSamplingCommand +
                               this.commandSuffix;
                this.sentCommand = queryInstrument(command);        
                streamingThread.sleep(5000);
                
                if (this.sentCommand ) {
                  this.state = 10;
                  break;
                  
                } else {
                  break; // try starting the sampling again due to failure
                }

              } else {

                break;

              }
              
            case 10:
              
              // sample line is begun by \r\n
              // note bytes are in reverse order in the FIFO window
              if ( byteOne == 0x0A && byteTwo == 0x0D ) {
                // we've found the beginning of a sample, move on
                this.state = 11;
                break;
    
              } else {
                break;                
              }
            
            case 11: // read the rest of the bytes to the next EOL characters
              
              // sample line is terminated by \r\n
              // note bytes are in reverse order in the FIFO window
              if ( byteOne == 0x0A && byteTwo == 0x0D ) {
                
                sampleByteCount++; // add the last byte found to the count
                
                // add the last byte found to the sample buffer
                if ( sampleBuffer.remaining() > 0 ) {
                  sampleBuffer.put(byteOne);
                
                } else {
                  sampleBuffer.compact();
                  sampleBuffer.put(byteOne);
                  
                }                
                
                // extract just the length of the sample bytes out of the
                // sample buffer, and place it in the channel map as a 
                // byte array.  Then, send it to the data turbine.
                sampleArray = new byte[sampleByteCount];
                sampleBuffer.flip();
                logger.debug("sampleBuffer: " + sampleBuffer.toString());
                sampleBuffer.get(sampleArray);
                
                this.responseString = new String(sampleArray, "US-ASCII");
                
                // test if the sample is not just an instrument message
                if ( this.responseString.matches("^# [0-9].*\r\n" ) ||
                     this.responseString.matches("^#  [0-9].*\r\n") ||
                     this.responseString.matches("^ [0-9].*\r\n") ) {
                
                  // add the data observations string to the CTDParser object
                  // and populate the CTDParser data fields
                  //this.ctdParser.setData(this.responseString);
                  //this.ctdParser.parse();
                  
                  // build the channel map with all of the data and metadata channels:                  
                  int channelIndex = rbnbChannelMap.Add(getRBNBChannelName());
                  rbnbChannelMap.PutMime(channelIndex, "text/plain");
                  rbnbChannelMap.PutTimeAuto("server");
                  
                  // add the ASCII sample data field
                  rbnbChannelMap.PutDataAsString(channelIndex, this.responseString);
                  
                  //// add the samplingMode field data                                                                                 
                  //channelIndex = rbnbChannelMap.Add("samplingMode");                                                                 
                  //rbnbChannelMap.PutMime(channelIndex, "text/plain");                                                                
                  //rbnbChannelMap.PutDataAsString(channelIndex, this.ctdParser.getSamplingMode()); // String
                  //
                  //// add the temperatureSerialNumber field data                                                                      
                  //channelIndex = rbnbChannelMap.Add("temperatureSerialNumber");                                                      
                  //rbnbChannelMap.PutMime(channelIndex, "text/plain");                                                                
                  //rbnbChannelMap.PutDataAsString(channelIndex, this.ctdParser.getTemperatureSerialNumber()); // String   
                  //
                  //// add the conductivitySerialNumber field data                                                                     
                  //channelIndex = rbnbChannelMap.Add("conductivitySerialNumber");                                                     
                  //rbnbChannelMap.PutMime(channelIndex, "text/plain");                                                                
                  //rbnbChannelMap.PutDataAsString(channelIndex, this.ctdParser.getConductivitySerialNumber()); // String   
                  //
                  //// add the mainBatteryVoltage field data                                                                           
                  //channelIndex = rbnbChannelMap.Add("mainBatteryVoltage");                                                           
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{this.ctdParser.getMainBatteryVoltage()}); // double   
                  //
                  //// add the lithiumBatteryVoltage field data                                                                        
                  //channelIndex = rbnbChannelMap.Add("lithiumBatteryVoltage");                                                        
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{this.ctdParser.getLithiumBatteryVoltage()}); // double   
                  //
                  //// add the operatingCurrent field data                                                                             
                  //channelIndex = rbnbChannelMap.Add("operatingCurrent");                                                             
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{this.ctdParser.getOperatingCurrent()}); // double   
                  //
                  //// add the pumpCurrent field data                                                                                  
                  //channelIndex = rbnbChannelMap.Add("pumpCurrent");                                                                  
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{this.ctdParser.getPumpCurrent()}); // double   
                  //
                  //// add the channels01ExternalCurrent field data                                                                    
                  //channelIndex = rbnbChannelMap.Add("channels01ExternalCurrent");                                                    
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{this.ctdParser.getChannels01ExternalCurrent()}); // double   
                  //
                  //// add the channels23ExternalCurrent field data                                                                    
                  //channelIndex = rbnbChannelMap.Add("channels23ExternalCurrent");                                                    
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{this.ctdParser.getChannels23ExternalCurrent()}); // double   
                  //
                  //// add the loggingStatus field data                                                                                
                  //channelIndex = rbnbChannelMap.Add("loggingStatus");                                                                
                  //rbnbChannelMap.PutMime(channelIndex, "text/plain");                                                                
                  //rbnbChannelMap.PutDataAsString(channelIndex, this.ctdParser.getLoggingStatus()); // String   
                  //
                  //// add the numberOfScansToAverage field data                                                                       
                  //channelIndex = rbnbChannelMap.Add("numberOfScansToAverage");                                                       
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsInt32(channelIndex, new int[]{this.ctdParser.getNumberOfScansToAverage()}); // int      
                  //
                  //// add the numberOfSamples field data                                                                              
                  //channelIndex = rbnbChannelMap.Add("numberOfSamples");                                                              
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsInt32(channelIndex, new int[]{this.ctdParser.getNumberOfSamples()}); // int      
                  //
                  //// add the numberOfAvailableSamples field data                                                                     
                  //channelIndex = rbnbChannelMap.Add("numberOfAvailableSamples");                                                     
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsInt32(channelIndex, new int[]{this.ctdParser.getNumberOfAvailableSamples()}); // int      
                  //
                  //// add the sampleInterval field data                                                                               
                  //channelIndex = rbnbChannelMap.Add("sampleInterval");                                                               
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsInt32(channelIndex, new int[]{this.ctdParser.getSampleInterval()}); // int      
                  //
                  //// add the measurementsPerSample field data                                                                        
                  //channelIndex = rbnbChannelMap.Add("measurementsPerSample");                                                        
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsInt32(channelIndex, new int[]{this.ctdParser.getMeasurementsPerSample()}); // int      
                  //
                  //// add the transmitRealtime field data                                                                             
                  //channelIndex = rbnbChannelMap.Add("transmitRealtime");                                                             
                  //rbnbChannelMap.PutMime(channelIndex, "text/plain");                                                                
                  //rbnbChannelMap.PutDataAsString(channelIndex, this.ctdParser.getTransmitRealtime()); // String   
                  //
                  //// add the numberOfCasts field data                                                                                
                  //channelIndex = rbnbChannelMap.Add("numberOfCasts");                                                                
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsInt32(channelIndex, new int[]{this.ctdParser.getNumberOfCasts()}); // int      
                  //
                  //// add the minimumConductivityFrequency field data                                                                 
                  //channelIndex = rbnbChannelMap.Add("minimumConductivityFrequency");                                                 
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsInt32(channelIndex, new int[]{this.ctdParser.getMinimumConductivityFrequency()}); // int      
                  //
                  //// add the pumpDelay field data                                                                                    
                  //channelIndex = rbnbChannelMap.Add("pumpDelay");                                                                    
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsInt32(channelIndex, new int[]{this.ctdParser.getPumpDelay()}); // int      
                  //
                  //// add the automaticLogging field data                                                                             
                  //channelIndex = rbnbChannelMap.Add("automaticLogging");                                                             
                  //rbnbChannelMap.PutMime(channelIndex, "text/plain");                                                                
                  //rbnbChannelMap.PutDataAsString(channelIndex, this.ctdParser.getAutomaticLogging()); // String   
                  //
                  //// add the ignoreMagneticSwitch field data                                                                         
                  //channelIndex = rbnbChannelMap.Add("ignoreMagneticSwitch");                                                         
                  //rbnbChannelMap.PutMime(channelIndex, "text/plain");                                                                
                  //rbnbChannelMap.PutDataAsString(channelIndex, this.ctdParser.getIgnoreMagneticSwitch()); // String   
                  //
                  //// add the batteryType field data                                                                                  
                  //channelIndex = rbnbChannelMap.Add("batteryType");                                                                  
                  //rbnbChannelMap.PutMime(channelIndex, "text/plain");                                                                
                  //rbnbChannelMap.PutDataAsString(channelIndex, this.ctdParser.getBatteryType()); // String   
                  //
                  //// add the batteryCutoff field data                                                                                
                  //channelIndex = rbnbChannelMap.Add("batteryCutoff");                                                                
                  //rbnbChannelMap.PutMime(channelIndex, "text/plain");                                                                
                  //rbnbChannelMap.PutDataAsString(channelIndex, this.ctdParser.getBatteryCutoff()); // String   
                  //
                  //// add the pressureSensorType field data                                                                           
                  //channelIndex = rbnbChannelMap.Add("pressureSensorType");                                                           
                  //rbnbChannelMap.PutMime(channelIndex, "text/plain");                                                                
                  //rbnbChannelMap.PutDataAsString(channelIndex, this.ctdParser.getPressureSensorType()); // String   
                  //
                  //// add the pressureSensorRange field data                                                                          
                  //channelIndex = rbnbChannelMap.Add("pressureSensorRange");                                                          
                  //rbnbChannelMap.PutMime(channelIndex, "text/plain");                                                                
                  //rbnbChannelMap.PutDataAsString(channelIndex, this.ctdParser.getPressureSensorRange()); // String   
                  //
                  //// add the sbe38TemperatureSensor field data                                                                       
                  //channelIndex = rbnbChannelMap.Add("sbe38TemperatureSensor");                                                       
                  //rbnbChannelMap.PutMime(channelIndex, "text/plain");                                                                
                  //rbnbChannelMap.PutDataAsString(channelIndex, this.ctdParser.getSbe38TemperatureSensor()); // String   
                  //
                  //// add the gasTensionDevice field data                                                                             
                  //channelIndex = rbnbChannelMap.Add("gasTensionDevice");                                                             
                  //rbnbChannelMap.PutMime(channelIndex, "text/plain");                                                                
                  //rbnbChannelMap.PutDataAsString(channelIndex, this.ctdParser.getGasTensionDevice()); // String   
                  //
                  //// add the externalVoltageChannelZero field data                                                                   
                  //channelIndex = rbnbChannelMap.Add("externalVoltageChannelZero");                                                   
                  //rbnbChannelMap.PutMime(channelIndex, "text/plain");                                                                
                  //rbnbChannelMap.PutDataAsString(channelIndex, this.ctdParser.getExternalVoltageChannelZero()); // String   
                  //
                  //// add the externalVoltageChannelOne field data                                                                    
                  //channelIndex = rbnbChannelMap.Add("externalVoltageChannelOne");                                                    
                  //rbnbChannelMap.PutMime(channelIndex, "text/plain");                                                                
                  //rbnbChannelMap.PutDataAsString(channelIndex, this.ctdParser.getExternalVoltageChannelOne()); // String   
                  //
                  //// add the externalVoltageChannelTwo field data                                                                    
                  //channelIndex = rbnbChannelMap.Add("externalVoltageChannelTwo");                                                    
                  //rbnbChannelMap.PutMime(channelIndex, "text/plain");                                                                
                  //rbnbChannelMap.PutDataAsString(channelIndex, this.ctdParser.getExternalVoltageChannelTwo()); // String   
                  //
                  //// add the externalVoltageChannelThree field data                                                                  
                  //channelIndex = rbnbChannelMap.Add("externalVoltageChannelThree");                                                  
                  //rbnbChannelMap.PutMime(channelIndex, "text/plain");                                                                
                  //rbnbChannelMap.PutDataAsString(channelIndex, this.ctdParser.getExternalVoltageChannelThree()); // String   
                  //
                  //// add the echoCommands field data                                                                                 
                  //channelIndex = rbnbChannelMap.Add("echoCommands");                                                                 
                  //rbnbChannelMap.PutMime(channelIndex, "text/plain");                                                                
                  //rbnbChannelMap.PutDataAsString(channelIndex, this.ctdParser.getEchoCommands()); // String   
                  //
                  //// add the outputFormat field data                                                                                 
                  //channelIndex = rbnbChannelMap.Add("outputFormat");                                                                 
                  //rbnbChannelMap.PutMime(channelIndex, "text/plain");                                                                
                  //rbnbChannelMap.PutDataAsString(channelIndex, this.ctdParser.getOutputFormat()); // String   
                  //
                  //// add the temperatureCalibrationDate field data                                                                   
                  //channelIndex = rbnbChannelMap.Add("temperatureCalibrationDate");                                                   
                  //rbnbChannelMap.PutMime(channelIndex, "text/plain");                                                                
                  //rbnbChannelMap.PutDataAsString(channelIndex, this.ctdParser.getTemperatureCalibrationDate()); // String   
                  //
                  //// add the temperatureCoefficientTA0 field data                                                                    
                  //channelIndex = rbnbChannelMap.Add("temperatureCoefficientTA0");                                                    
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{this.ctdParser.getTemperatureCoefficientTA0()}); // double   
                  //
                  //// add the temperatureCoefficientTA1 field data                                                                    
                  //channelIndex = rbnbChannelMap.Add("temperatureCoefficientTA1");                                                    
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{this.ctdParser.getTemperatureCoefficientTA1()}); // double   
                  //
                  //// add the temperatureCoefficientTA2 field data                                                                    
                  //channelIndex = rbnbChannelMap.Add("temperatureCoefficientTA2");                                                    
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{this.ctdParser.getTemperatureCoefficientTA2()}); // double   
                  //
                  //// add the temperatureCoefficientTA3 field data                                                                    
                  //channelIndex = rbnbChannelMap.Add("temperatureCoefficientTA3");                                                    
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{this.ctdParser.getTemperatureCoefficientTA3()}); // double   
                  //
                  //// add the temperatureOffsetCoefficient field data                                                                 
                  //channelIndex = rbnbChannelMap.Add("temperatureOffsetCoefficient");                                                 
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{this.ctdParser.getTemperatureOffsetCoefficient()}); // double   
                  //
                  //// add the conductivityCalibrationDate field data                                                                  
                  //channelIndex = rbnbChannelMap.Add("conductivityCalibrationDate");                                                  
                  //rbnbChannelMap.PutMime(channelIndex, "text/plain");                                                                
                  //rbnbChannelMap.PutDataAsString(channelIndex, this.ctdParser.getConductivityCalibrationDate()); // String   
                  //
                  //// add the conductivityCoefficientG field data                                                                     
                  //channelIndex = rbnbChannelMap.Add("conductivityCoefficientG");                                                     
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{this.ctdParser.getConductivityCoefficientG()}); // double   
                  //
                  //// add the conductivityCoefficientH field data                                                                     
                  //channelIndex = rbnbChannelMap.Add("conductivityCoefficientH");                                                     
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{this.ctdParser.getConductivityCoefficientH()}); // double   
                  //
                  //// add the conductivityCoefficientI field data                                                                     
                  //channelIndex = rbnbChannelMap.Add("conductivityCoefficientI");                                                     
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{this.ctdParser.getConductivityCoefficientI()}); // double   
                  //
                  //// add the conductivityCoefficientJ field data                                                                     
                  //channelIndex = rbnbChannelMap.Add("conductivityCoefficientJ");                                                     
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{this.ctdParser.getConductivityCoefficientJ()}); // double   
                  //
                  //// add the conductivityCoefficientCF0 field data                                                                   
                  //channelIndex = rbnbChannelMap.Add("conductivityCoefficientCF0");                                                   
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{this.ctdParser.getConductivityCoefficientCF0()}); // double   
                  //
                  //// add the conductivityCoefficientCPCOR field data                                                                 
                  //channelIndex = rbnbChannelMap.Add("conductivityCoefficientCPCOR");                                                 
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{this.ctdParser.getConductivityCoefficientCPCOR()}); // double   
                  //
                  //// add the conductivityCoefficientCTCOR field data                                                                 
                  //channelIndex = rbnbChannelMap.Add("conductivityCoefficientCTCOR");                                                 
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{this.ctdParser.getConductivityCoefficientCTCOR()}); // double   
                  //
                  //// add the conductivityCoefficientCSLOPE field data                                                                
                  //channelIndex = rbnbChannelMap.Add("conductivityCoefficientCSLOPE");                                                
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{this.ctdParser.getConductivityCoefficientCSLOPE()}); // double   
                  //
                  //// add the pressureSerialNumber field data                                                                         
                  //channelIndex = rbnbChannelMap.Add("pressureSerialNumber");                                                         
                  //rbnbChannelMap.PutMime(channelIndex, "text/plain");                                                                
                  //rbnbChannelMap.PutDataAsString(channelIndex, this.ctdParser.getPressureSerialNumber()); // String   
                  //
                  //// add the pressureCoefficientPA0 field data                                                                       
                  //channelIndex = rbnbChannelMap.Add("pressureCoefficientPA0");                                                       
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{this.ctdParser.getPressureCoefficientPA0()}); // double   
                  //
                  //// add the pressureCoefficientPA1 field data                                                                       
                  //channelIndex = rbnbChannelMap.Add("pressureCoefficientPA1");                                                       
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{this.ctdParser.getPressureCoefficientPA1()}); // double   
                  //
                  //// add the pressureCoefficientPA2 field data                                                                       
                  //channelIndex = rbnbChannelMap.Add("pressureCoefficientPA2");                                                       
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{this.ctdParser.getPressureCoefficientPA2()}); // double   
                  //
                  //// add the pressureCoefficientPTCA0 field data                                                                     
                  //channelIndex = rbnbChannelMap.Add("pressureCoefficientPTCA0");                                                     
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{this.ctdParser.getPressureCoefficientPTCA0()}); // double   
                  //
                  //// add the pressureCoefficientPTCA1 field data                                                                     
                  //channelIndex = rbnbChannelMap.Add("pressureCoefficientPTCA1");                                                     
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{this.ctdParser.getPressureCoefficientPTCA1()}); // double   
                  //
                  //// add the pressureCoefficientPTCA2 field data                                                                     
                  //channelIndex = rbnbChannelMap.Add("pressureCoefficientPTCA2");                                                     
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{this.ctdParser.getPressureCoefficientPTCA2()}); // double   
                  //
                  //// add the pressureCoefficientPTCB0 field data                                                                     
                  //channelIndex = rbnbChannelMap.Add("pressureCoefficientPTCB0");                                                     
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{this.ctdParser.getPressureCoefficientPTCB0()}); // double   
                  //
                  //// add the pressureCoefficientPTCB1 field data                                                                     
                  //channelIndex = rbnbChannelMap.Add("pressureCoefficientPTCB1");                                                     
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{this.ctdParser.getPressureCoefficientPTCB1()}); // double   
                  //
                  //// add the pressureCoefficientPTCB2 field data                                                                     
                  //channelIndex = rbnbChannelMap.Add("pressureCoefficientPTCB2");                                                     
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{this.ctdParser.getPressureCoefficientPTCB2()}); // double   
                  //
                  //// add the pressureCoefficientPTEMPA0 field data                                                                   
                  //channelIndex = rbnbChannelMap.Add("pressureCoefficientPTEMPA0");                                                   
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{this.ctdParser.getPressureCoefficientPTEMPA0()}); // double   
                  //
                  //// add the pressureCoefficientPTEMPA1 field data                                                                   
                  //channelIndex = rbnbChannelMap.Add("pressureCoefficientPTEMPA1");                                                   
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{this.ctdParser.getPressureCoefficientPTEMPA1()}); // double   
                  //
                  //// add the pressureCoefficientPTEMPA2 field data                                                                   
                  //channelIndex = rbnbChannelMap.Add("pressureCoefficientPTEMPA2");                                                   
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{this.ctdParser.getPressureCoefficientPTEMPA2()}); // double   
                  //
                  //// add the pressureOffsetCoefficient field data                                                                    
                  //channelIndex = rbnbChannelMap.Add("pressureOffsetCoefficient");                                                    
                  //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");                                                  
                  //rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{this.ctdParser.getPressureOffsetCoefficient()}); // double   

                  // send the sample to the data turbine
                  getSource().Flush(rbnbChannelMap);
                  logger.info("Sent sample to the DataTurbine: " + this.responseString);
                  
                  // reset variables for the next sample
                  sampleBuffer.clear();
                  sampleByteCount = 0;
                  channelIndex = 0;
                  rbnbChannelMap.Clear();                      
                  logger.debug("Cleared b1,b2,b3,b4. Cleared sampleBuffer. Cleared rbnbChannelMap.");
                  
                  // check if the clock needs syncing (daily)
                  //Calendar currentCalendar = Calendar.getInstance();
                  //currentCalendar.setTime(new Date());
                  //Calendar lastSyncedCalendar = Calendar.getInstance();
                  //lastSyncedCalendar.setTime(this.clockSyncDate);
                  //
                  //currentCalendar.clear(Calendar.MILLISECOND);
                  //currentCalendar.clear(Calendar.SECOND);
                  //currentCalendar.clear(Calendar.MINUTE);
                  //currentCalendar.clear(Calendar.HOUR);
                  //
                  //lastSyncedCalendar.clear(Calendar.MILLISECOND);
                  //lastSyncedCalendar.clear(Calendar.SECOND);
                  //lastSyncedCalendar.clear(Calendar.MINUTE);
                  //lastSyncedCalendar.clear(Calendar.HOUR);
                  //
                  //// sync the clock daily
                  //if ( currentCalendar.before(lastSyncedCalendar) ) {
                  //  this.state = 8;
                  //  
                  //} else {
                  //  this.state = 10;
                  //  
                  //}
                  this.state = 10;
                  break;                  
                
                // the sample looks more like an instrument message, don't flush
                } else {
                  
                  logger.info("This string does not look like a sample, " +
                              "and was not sent to the DataTurbine.");
                  logger.info("Skipping sample: " + this.responseString);

                  // reset variables for the next sample
                  sampleBuffer.clear();
                  sampleByteCount = 0;
                  //rbnbChannelMap.Clear();                      
                  logger.debug("Cleared b1,b2,b3,b4. Cleared sampleBuffer. Cleared rbnbChannelMap.");
                  logger.debug("sampleBuffer: " + sampleBuffer.toString());
                  this.state = 10;
                  break;
                  
                }
                
              } else { // not 0x0A0D

                // still in the middle of the sample, keep adding bytes
                sampleByteCount++; // add each byte found

                if ( sampleBuffer.remaining() > 0 ) {
                  sampleBuffer.put(byteOne);
                } else {
                  sampleBuffer.compact();
                  logger.debug("Compacting sampleBuffer ...");
                  sampleBuffer.put(byteOne);
                  
                }
                
                break;
              } // end if for 0x0A0D EOL
          
            case 12: // alternatively use legacy DS and DCal commands
              
              // start by getting the DS status output
              this.command = this.commandPrefix        +
                             this.displayStatusCommand +
                             this.commandSuffix;
              this.sentCommand = queryInstrument(command);        
              streamingThread.sleep(5000);
              this.state = 13;
              break;
              
              
            case 13: // handle the DS command response
              
              // command should end with the S> prompt
              if ( byteOne == 0x7E   && byteTwo == 0x53 ) {
                
                // handle instrument status response
                sampleByteCount++; // add the last byte found to the count
                
                // add the last byte found to the sample buffer
                if ( sampleBuffer.remaining() > 0 ) {
                  sampleBuffer.put(byteOne);
                
                } else {
                  sampleBuffer.compact();
                  sampleBuffer.put(byteOne);
                  
                }
                
                // extract the sampleByteCount length from the sampleBuffer
                sampleArray = new byte[sampleByteCount - 2]; //subtract "S>"
                sampleBuffer.flip();
                sampleBuffer.get(sampleArray);
                this.responseString = new String(sampleArray, "US-ASCII");
                
                // reset variables for the next sample
                sampleBuffer.clear();
                sampleByteCount = 0;
                
                // then get the instrument calibration metadata
                  this.command = this.commandPrefix             +
                                 this.displayCalibrationCommand +
                                 this.commandSuffix;
                  this.sentCommand = queryInstrument(command);
                  streamingThread.sleep(5000);
                  this.state = 14;
                  break;
                
              } else {
                break; // continue reading bytes
                
              }
              
            case 14: // handle the DCal command response
              
              // command should end with the S> prompt
              if ( byteOne == 0x7E   && byteTwo == 0x53 ) {
                
                // handle instrument status response
                sampleByteCount++; // add the last byte found to the count
                
                // add the last byte found to the sample buffer
                if ( sampleBuffer.remaining() > 0 ) {
                  sampleBuffer.put(byteOne);
                
                } else {
                  sampleBuffer.compact();
                  sampleBuffer.put(byteOne);
                  
                }
                
                // extract the sampleByteCount length from the sampleBuffer
                sampleArray = new byte[sampleByteCount - 2]; // subtract "S>"
                sampleBuffer.flip();
                logger.debug("sampleBuffer: " + sampleBuffer.toString());
                sampleBuffer.get(sampleArray);
                
                // append the DCal output to the DS output
                this.responseString = 
                  this.responseString.concat(new String(sampleArray, "US-ASCII"));
                
                // and add the data delimiter expected in the CTDParser
                this.responseString = this.responseString.concat("*END*\r\n\r\n");
                
                // build the CTDParser object with legacy DS and DCal metadata
                this.ctdParser = new CTDParser(this.responseString);
                
                // reset variables for the next sample
                sampleBuffer.clear();
                sampleByteCount = 0;
                
                this.state = 8; // set the clock and start sampling
                break;
                
              } else {
                break; // continue reading bytes
                
              }
            
          } // end switch statement
          
          // shift the bytes in the FIFO window
          byteFour = byteThree;
          byteThree = byteTwo;
          byteTwo = byteOne;

        } //end while (more unread bytes)
    
        // prepare the buffer to read in more bytes from the stream
        buffer.compact();
    
    
      } // end while (more channel bytes to read)
      
      this.channel.close();
        
    } catch ( IOException e ) {
      // handle exceptions
      // In the event of an i/o exception, log the exception, and allow execute()
      // to return false, which will prompt a retry.
      //this.channel.close();
      failed = true;
      e.printStackTrace();
      return !failed;
    
    } catch ( InterruptedException intde ) {
      // in the event that the streamingThread is interrupted
      failed = true;
      intde.printStackTrace();
      return !failed;
    
    } catch ( SAPIException sapie ) {
      // In the event of an RBNB communication  exception, log the exception, 
      // and allow execute() to return false, which will prompt a retry.
      //this.channel.close();
      failed = true;
      sapie.printStackTrace();
      return !failed;
    
    } catch (ParseException pe ) {
      failed = true;
      logger.info("There was an error parsing the metadata response. " +
                  "The error message was: " + pe.getMessage());
      return !failed;
      
  //} finally {
  //  
  //  if (this.channel.isOpen() ) {
  //    try {
  //      this.channel.close();
  //      
  //    } catch ( IOException cioe ) {
  //      logger.debug("An error occurred trying to close the byte channel. " +
  //                   " The error message was: " + cioe.getMessage());
  //                   
  //    }
  //  }
    }
    
    return !failed;
  }
  
   /**
   * A method used to the TCP socket of the remote source host for communication
   * @param host       the name or IP address of the host to connect to for the
   *                   socket connection (reading)
   * @param portNumber the number of the TCP port to connect to (i.e. 2604)
   */
  protected SocketChannel getSocketConnection() {
    
    
    String host = getHostName();
    int portNumber = new Integer(getHostPort()).intValue();
    SocketChannel dataSocket = null;
    
    try {  
      
      // create the socket channel connection to the data source via the 
      // converter serial2IP converter      
      dataSocket = SocketChannel.open();
      dataSocket.connect( new InetSocketAddress(host, portNumber));
      
      // if the connection to the source fails, also disconnect from the RBNB
      // server and return null
      if ( !dataSocket.isConnected()) {
        dataSocket.close();
        disconnect();
        dataSocket = null;
      }      
    }  catch ( UnknownHostException ukhe ) {
      
      logger.info("Unable to look up host: " + host + "\n");
      disconnect();
      dataSocket = null;
    } catch (IOException nioe ) {
      logger.info("Couldn't get I/O connection to: " + host);
      disconnect();
      dataSocket = null;
    } catch (Exception e) {
      disconnect();
      dataSocket = null;            
    }
    return dataSocket;
    
  }

   /**
   * A method used to get a serial connection for communication
   */
  protected ByteChannel getSerialConnection() {
    logger.debug("CTDSource.getSerialConnection() called.");
    
    ByteChannel serialChannel = (ByteChannel) new SerialChannel(getSerialPort());  
    return serialChannel;
    
  }

  /**
   * A method that sets the size, in bytes, of the ByteBuffer used in streaming 
   * data from a source instrument via a TCP connection
   */
   public int getBufferSize() {
     return this.bufferSize;
   }
   
  /**
   * A method that returns the domain name or IP address of the source 
   * instrument (i.e. the serial-to-IP converter to which it is attached)
   */
  public String getHostName(){
    return this.sourceHostName;
  }

  /**
   * A method that returns the name of the RBNB channel that contains the 
   * streaming data from this instrument
   */
  public String getRBNBChannelName(){
    return this.rbnbChannelName;
  }

  /**
   * A method that returns the TCP port of the source 
   * instrument (i.e. the serial-to-IP converter to which it is attached)
   */
  public int getHostPort(){
    return this.sourceHostPort;
  }

  /**
   * A method that returns the versioning info for this file.  In this case, 
   * it returns a String that includes the Subversion LastChangedDate, 
   * LastChangedBy, LastChangedRevision, and HeadURL fields.
   */

  /**
   * A method that returns the name of the serial port used for serial
   * communication.  This defaults to "/dev/ttyS0".  On Windows, this
   * should be set to the appropriate comm port (e.g. COM1).  For use with 
   * FTDI serial-to-USB adapters on Linux, use "/dev/ttyUSB0".
   *
   * @return serialPort - the name of the serial port
   */
  public String getSerialPort() {
    return this.serialPortName;
    
  }
  
  /**
   * A method used to set the name of the serial port used for serial
   * communication.
   */
  private void setSerialPort(String serialPortName) {
    this.serialPortName = serialPortName;
      
  }
    
  /**
   * A method used to set the connection type, either serial or socket.
   */
  private void setConnectionType(String connectionType) {
    this.connectionType = connectionType;
      
  }
  
  /**
   * A method used to get the connection type, either serial or socket.
   *
   * @return connectionType - the connection type as a string.
   */
  public String getConnectionType() {
    return this.connectionType;
      
  }
  
  /**
   * A method used to set the output type, either xml or text.  By using
   * xml, xml-based CTD query commands will be used (GetSD, GetCD, GetCC, etc.).
   * When using text, legacy text-based commands will be used to query the
   * CTD (DS, DCal, etc.)
   */
  private void setOutputType(String outputType) {
    this.outputType = outputType;
      
  }
  
  /**
   * A method used to get the output type, either xml or text.
   *
   * @return outputType - the output type as a string.
   */
  public String getOutputType() {
    return this.outputType;
      
  }
  
  public String getCVSVersionString(){
    return (
    "$LastChangedDate$" +
    "$LastChangedBy$" +
    "$LastChangedRevision$" +
    "$HeadURL$"
    );
  }

  /**
   * A method that returns true if the RBNB connection is established
   * and if the data streaming Thread has been started
   */
  public boolean isRunning() {
    // return the connection status and the thread status
    return ( isConnected() && readyToStream );
  }
  
  /**
   * The main method for running the code
   * @ param args[] the command line list of string arguments, none are needed
   */

  public static void main (String args[]) {
    
    logger.info("CTDSource.main() called.");
    
    try {
      // create a new instance of the CTDSource object, and parse the command 
      // line arguments as settings for this instance
      final CTDSource ctdSource = new CTDSource();
      
      // Handle ctrl-c's and other abrupt death signals to the process
      Runtime.getRuntime().addShutdownHook(new Thread() {
        // stop the streaming process
        public void run() {
          ctdSource.stop();
        }
      }
      );
      
      // Set up a simple logger that logs to the console
      PropertyConfigurator.configure(ctdSource.getLogConfigurationFile());
      
      // parse the commandline arguments to configure the connection, then 
      // start the streaming connection between the source and the RBNB server.
      if ( ctdSource.parseArgs(args) ) {
        ctdSource.start();
      }
            
    } catch ( Exception e ) {
      logger.info("Error in main(): " + e.getMessage());
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
      
      // connect to the RBNB server
      if ( connect() ) {
        // run the data streaming code
        retry = !execute();
      }
      
      disconnect();
      
      if ( retry ) {
        try {
          Thread.sleep(RETRY_INTERVAL);
        } catch ( Exception e ){
          logger.info("There was an execution problem. Retrying. Message is: " +
          e.getMessage());
        }
      }
    }
    // stop the streaming when we are done
    stop();
  }

  /**
   * A method that queries the instrument to obtain its ID 
   *
   * @return result - a boolean result, true if the command succeeds
   */
  public boolean queryInstrument(String command) {
    
    // the result of the query
    boolean result = false;
    
    // only send the command if the socket is connected
    if ( this.channel.isOpen() ) {
      ByteBuffer commandBuffer = ByteBuffer.allocate( command.length() * 10);
      commandBuffer.put(command.getBytes());
      commandBuffer.flip();
      
      try {
        this.channel.write(commandBuffer);
        logger.debug("Wrote " + command + " to the instrument channel.");
        result = true;
        
      } catch (IOException ioe ) {
        logger.info("There was a problem sending the command to the " + 
                    "instrument. The error message was: " +
                    ioe.getMessage());
        result = false;
      }
    }
    return result;
  }
  
  /**
   * A method that sets the command line arguments for this class.  This method 
   * calls the <code>RBNBSource.setBaseArgs()</code> method.
   * 
   * @param command  The CommandLine object being passed in from the command
   */
  protected boolean setArgs(CommandLine command) {
    
    // first set the base arguments that are included on the command line
    if ( !setBaseArgs(command)) {
      return false;
    }
    
    // add command line arguments here
    
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
          logger.info("Error: Enter a numeric value for the host port. " +
                             hostPort + " is not a valid number.");
          return false;
        }
      }
    }

    // handle the -C option
    if ( command.hasOption("C") ) {
      String channelName = command.getOptionValue("C");
      if ( channelName != null ) {
        setChannelName(channelName);
      }
    }
    
    // handle the -c option
    if ( command.hasOption("c") ) {
      String communicationPort = command.getOptionValue("c");
      if ( communicationPort != null ) {
        setSerialPort(communicationPort);
      }
    }
    
    // handle the -t option
    if ( command.hasOption("t") ) {
      String connectionType = command.getOptionValue("t");
      if ( connectionType != null ) {
        if ( connectionType.equals("serial") || 
             connectionType.equals("socket") ) {
          setConnectionType(connectionType);
          
        } else {
          logger.info("The connection type was not recognized.  Please " +
                      "use either 'serial' or 'socket' for the '-t' option.");
          return false;
          
        }
      }
    }

    // handle the -o option
    if ( command.hasOption("o") ) {
      String outputType = command.getOptionValue("o");
      if ( outputType != null ) {
        
        if ( outputType.equals("xml") || outputType.equals("text") ) {
          setOutputType(outputType);          
          
        } else {
          logger.info("The output type was not recognized.  Please " +
                      "use either 'xml' or 'text' for the '-o' option.");
          return false;
          
        }
        
        setOutputType(outputType);
      }
    }
    
    
    if ( (command.hasOption("H") || command.hasOption("P")) &&
          command.hasOption("c") && (!command.hasOption("t")) ) {
      logger.info("There was a configuration error.  The '-H' and '-P' " +
                  "options are mutually exclusive of the '-c' option "   +
                  "(socket vs. serial connection type).  You must "      +
                  "include the '-t' option designating the connection "  +
                  "type, with either 'serial' or 'socket'.");
      return false;
    }

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
   * A method that sets the command line options for this class.  This method 
   * calls the <code>RBNBSource.setBaseOptions()</code> method in order to set
   * properties such as the sourceHostName, sourceHostPort, serverName, and
   * serverPort.
   */
  protected Options setOptions() {
    Options options = setBaseOptions(new Options());
    
    // Note: 
    // Command line options already provided by RBNBBase include:
    // -h "Print help"
    // -s "RBNB Server Hostname"
    // -p "RBNB Server Port Number"
    // -S "RBNB Source Name"
    // -v "Print Version information"
    
    // Command line options already provided by RBNBSource include:
    // -z "Cache size"
    // -Z "Archive size"
    
    // add command line options here
    options.addOption("H", true, "Source host name or IP e.g. " + getHostName());
    options.addOption("P", true, "Source host port number e.g. " + getHostPort());    
    options.addOption("C", true, "RBNB source channel name e.g. " + getRBNBChannelName());
    options.addOption("c", true, "communication serial port, e.g. /dev/ttyUSB0");
    options.addOption("o", true, "output type from the CTD, either 'xml' or 'text'");
    options.addOption("t", true, "connection type to the CTD, either 'serial' or 'socket'");
                      
    return options;
  }

  /**
   * A method that starts the streaming of data from the source instrument to
   * the RBNB server via an established TCP connection.  
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

  /**
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
   * A method that stops the streaming of data between the source instrument and
   * the RBNB server.  
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

  /**
   * A method that interrupts the thread created in startThread()
   */
  private void stopThread() {
    // set the streaming status to false and stop the Thread
    readyToStream = false;
    streamingThread.interrupt();
  }

  /**
   * A method that gets the log configuration file location
   *
   * @return logConfigurationFile  the log configuration file location
   */
  public String getLogConfigurationFile() {
    return this.logConfigurationFile;
  }
  
  /**
   * A method that sets the log configuration file name
   *
   * @param logConfigurationFile  the log configuration file name
   */
  public void setLogConfigurationFile(String logConfigurationFile) {
    this.logConfigurationFile = logConfigurationFile;
  }
  
}
