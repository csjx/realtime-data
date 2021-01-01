/*
 *  Copyright: 2020 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
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

import com.rbnb.sapi.SAPIException;

import java.io.IOException;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.zip.Inflater;
import java.util.zip.DataFormatException;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math.linear.RealMatrix;

import org.apache.log4j.PropertyConfigurator;

import org.nees.rbnb.RBNBSource;

/**
 * A simple class used to harvest a compressed hexadecimal ASCII data file 
 * from a Brooke Ocean Seahorse profiler equipped with a Seacat SBE19plus CTD. 
 * Communication is over a TCP socket connection with an Iridium satellite modem
 * and a serial2ip converter host. The data samples are then converted into RBNB
 * frames and pushed into the RBNB DataTurbine real time server.  This class  
 * extends org.nees.rbnb.RBNBSource, which in turn extends org.nees.rbnb.RBNBBase, 
 * and therefore follows the API conventions found in the org.nees.rbnb code.  
 *
 * The parsing of the data stream relies on the premise that each sample (scan) 
 * of data is a Hex-encoded string of values, and that each sample is terminated
 * by a newline character (\n).  It is also assumed that the sample rate is 4Hz,
 * or a sample is taken every 15 seconds.
 *
 */
public class SeahorseSource extends RBNBSource {

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
  private int DEFAULT_BUFFER_SIZE = 8096; // 8K

  /**
   * The size of the ByteBuffer used to beffer the TCP stream from the 
   * instrument.
   */
  private int bufferSize = DEFAULT_BUFFER_SIZE;
  
  /*  A default RBNB channel name for the given source instrument */  
  private String DEFAULT_RBNB_CHANNEL = "HexadecimalASCIISampleData";

  /** The name of the RBNB channel for this data stream */
  private String rbnbChannelName = DEFAULT_RBNB_CHANNEL;
  
  /*  A default source IP address for the given source instrument */
  private final String DEFAULT_SOURCE_HOST_NAME = "127.0.0.1";  

  /**
   * The domain name or IP address of the host machine that this Source 
   * represents and from which the data will stream. 
   */
  private String sourceHostName = DEFAULT_SOURCE_HOST_NAME;

  /*  A default source TCP port for the given source instrument */  
  private final int DEFAULT_SOURCE_HOST_PORT  = 2101;

  /** The TCP port to connect to on the Source host machine  */
  private int sourceHostPort = DEFAULT_SOURCE_HOST_PORT;

  /* The socket channel used to establish TCP communication with the instrument */
  private SocketChannel socketChannel;
  
  /** The number of bytes in the ensemble as each byte is read from the stream */
  private int resultByteCount = 0;
  
  /** The command prefix used to send commands to the microcontroller */ 
  private String MODEM_COMMAND_PREFIX = "AT";

  /** The command suffix used to send commands to the microcontroller */ 
  private final String MODEM_COMMAND_SUFFIX = "\r";

  /** The command used to get the network registration status from the Iridium modem */ 
  private final String REGISTRATION_STATUS_COMMAND = "+CREG?";

  /** The command used to get the signal strength from the Iridium modem */ 
  private final String SIGNAL_STRENGTH_COMMAND = "+CSQ";

  /** The command used to answer a RING call from the Iridium modem */ 
  private final String ANSWER_COMMAND = "A";

  /**  The command used to acknowledge the connection from the instrument */ 
  private final String ACKNOWLEDGE_COMMAND = "ACK";

  /** The command used to get the ID from the instrument */ 
  private final String ID_COMMAND = "GID";

  /** The platform ID of the instrument (i.e. the SeahHorse identifier, not the CTD) */ 
  private  String platformID = "";
  
  /** The command used to get the battery voltage from the instrument */ 
  private final String BATTERY_VOLTAGE_COMMAND = "GBV";
  
  /** The command used to get the GPRMC data string from the instrument */ 
  private final String GPRMC_COMMAND = "GPS";

  /** The command used to get the name of the file to be downloaded from the instrument */ 
  private final String FILENAME_COMMAND = "GFN";

  /** The command used to get the remaining number of blocks (bytes) from the instrument */ 
  private final String NUMBER_OF_BLOCKS_COMMAND = "GNB";

  /** The remaining number of blocks (bytes) to download from the instrument */ 
  private int numberOfBlocks = 0;
  
  /** The command used to transfer blocks (bytes) from the instrument */ 
  private final String TRANSFER_BLOCKS_COMMAND = "TXB";

  /** The command used to disconnect (hang up) with the Iridium modem */ 
  private final String HANGUP_COMMAND = "H0";

  /**  The command used to close the transfer session with the instrument */ 
  private final String CLOSE_TRANSFER_SESSION_COMMAND = "REL";

  /** The command used to escape to command mode with the Iridium modem */ 
  private final String ESCAPE_SEQUENCE_COMMAND = "+++";

  /**  The okay status string expected from the instrument */ 
  private final String OKAY_STATUS = "OK";
  
  /**  The signal strength string expected from Iridium modem */ 
  private final String SIGNAL_STRENGTH = "+CSQ:";
  
  /**  The signal strength threshold string needed from Iridium modem (0 - 5) */ 
  private final int SIGNAL_THRESHOLD = 3;
  
  /**  The registration status string expected from the instrument */ 
  private final String REGISTRATION_STATUS = "+CREG:";
  
  /**  The call ring string expected from the instrument */ 
  private final String CALL_RING = "RING";
  
  /**  The connect rate string expected from the instrument */ 
  private final String CONNECT_RATE = "CONNECT 19200";
  
  /**  The ready status string expected from the instrument */ 
  private final String READY_STATUS = "READY";
  
  /**  The file name prefix string expected from the instrument */ 
  private final String FILENAME_PREFIX = "FILE=";
    
  /**  The file name to be downloaded from the instrument */ 
  private String fileNameToDownload = "";
  
  /**  The prefix string expected at the beginning of the data file name */ 
  private final String DATA_FILE_PREFIX = "SH__";
  
  /**  The prefix string expected at the beginning of the cast file name */ 
  private final String CAST_FILE_PREFIX = "CAST";
  
  /**  The blocksize prefix string expected from the instrument */ 
  private final String BLOCKSIZE_PREFIX = "BLOCKSIZE=";
  
  /**  The transfer complete string expected from the instrument */ 
  private final String TRANSFER_COMPLETE = "DONE";
  
  /**  The end of files string expected from the instrument */ 
  private final String END_OF_FILES = "NONE";
  
  /**  The session closed string expected from the instrument */ 
  private final String SESSION_CLOSED = "BYE";
  
  /**  The data file string downloaded from the instrument */ 
  private String dataFileString = "";
  
  /**  The cast file string downloaded from the instrument */ 
  private String castFileString = "";
  
  /** The command sent to the instrument */ 
  private String command;  
  
  /* A boolean field indicating if a command has been sent to the instrument */
  private boolean sentCommand = false;
  
  /*
   * The instance of the CTDParser object used to parse the CTD
   * data file and retrieve each of the data fields
   */
   private CTDParser ctdParser = null;
   
  /** The default log configuration file location */
  private final String DEFAULT_LOG_CONFIGURATION_FILE = "lib/log4j2.properties";

  /** The log configuration file location */
  private String logConfigurationFile = DEFAULT_LOG_CONFIGURATION_FILE;
  
  /** The Logger instance used to log system messages  */
  private static Log log = LogFactory.getLog(SeahorseSource.class);
  
  /* The channel map object used to transfer data to the DataTurbine*/
  private ChannelMap rbnbChannelMap;
  
  /* The channel index integer used to populate the channel map */
  int channelIndex;

  protected int state = 0;
  
  private boolean readyToStream = false;
  
  private Thread streamingThread;
  
  /*
   * An internal Thread setting used to specify how long, in milliseconds, the
   * execution of the data streaming Thread should wait before re-executing
   * 
   * @see execute()
   */
  private final int RETRY_INTERVAL = 5000;
    
  /*
   * An internal Thread setting used to specify how long, in milliseconds, the
   * execution of the data streaming Thread should sleep before continuing
   * 
   * @see execute()
   */
  private final int SLEEP_INTERVAL = 5000;
  
  /** 
   * The date format for the timestamp applied to the CTD sample e.g. "23 Sep 2009  11:29:15"
   */
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy  HH:mm:ss");
  
  /**  The timezone used for the sample date */
  private static final TimeZone TZ = TimeZone.getTimeZone("Pacific/Honolulu");
  
  /* The sample datetime reported by the parsed CTD data file as a Calendar */
  private Calendar sampleDateTime;
  
  /**
   * Constructor - create an empty instance of the SeahorseSource object, using
   * default values for the RBNB server name and port, source instrument name
   * and port, archive mode, archive frame size, and cache frame size. 
   */
  public SeahorseSource() {
  }

  /**
   * Constructor - create an instance of the SeahorseSource object, using the
   * argument values for the source instrument name and port, and the RBNB 
   * server name and port.  This constructor will use default values for the
   * archive mode, archive frame size, and cache frame size. 
   *
   * @param sourceHostName  the name or IP address of the source instrument
   * @param sourceHostPort  the TCP port of the source host instrument
   * @param serverName      the name or IP address of the RBNB server connection
   * @param serverPort      the TCP port of the RBNB server
   */
  public SeahorseSource(String sourceHostName, String sourceHostPort, 
                      String serverName, String serverPort) {
    
    setHostName(sourceHostName);
    setHostPort(Integer.parseInt(sourceHostPort));
    setServerName(serverName);
    setServerPort(Integer.parseInt(serverPort));
  }

  /**
   * Constructor - create an instance of the SeahorseSource object, using the
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
  public SeahorseSource(String sourceHostName, String sourceHostPort, 
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
    log.debug("SeahorseSource.execute() called.");
    // do not execute the stream if there is no connection
    if (  !isConnected() ) return false;
    
      boolean failed = false;
    
      this.socketChannel = getSocketConnection();
    
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
      
      // define a byte array that will be used to manipulate the incoming bytes
      byte[] resultArray;
      String resultString;
      
      // Create a buffer that will store the result bytes as they are read
      ByteBuffer resultBuffer = ByteBuffer.allocate(getBufferSize());
      
      // create a byte buffer to store bytes from the TCP stream
      ByteBuffer buffer = ByteBuffer.allocateDirect(getBufferSize());
      
      this.rbnbChannelMap = new ChannelMap();
      this.channelIndex = 0;
            
      // initiate the session with the modem, test if is network registered
      this.command = this.MODEM_COMMAND_PREFIX +
                     this.REGISTRATION_STATUS_COMMAND +
                     this.MODEM_COMMAND_SUFFIX;
      this.sentCommand = queryInstrument(this.command);
      
      // allow time for the modem to respond
      streamingThread.sleep(this.SLEEP_INTERVAL);
      
      // while there are bytes to read from the socketChannel ...
      while ( socketChannel.read(buffer) != -1 || buffer.position() > 0) {

        // prepare the buffer for reading
        buffer.flip();          
    
        // while there are unread bytes in the ByteBuffer
        while ( buffer.hasRemaining() ) {
          byteOne = buffer.get();

          //log.debug("b1: " + new String(Hex.encodeHex((new byte[]{byteOne})))   + "\t" + 
          //             "b2: " + new String(Hex.encodeHex((new byte[]{byteTwo})))   + "\t" + 
          //             "b3: " + new String(Hex.encodeHex((new byte[]{byteThree}))) + "\t" + 
          //             "b4: " + new String(Hex.encodeHex((new byte[]{byteFour})))  + "\t" +
          //             "result pos: "   + resultBuffer.position()                  + "\t" +
          //             "result rem: "   + resultBuffer.remaining()                 + "\t" +
          //             "result cnt: "   + resultByteCount                          + "\t" +
          //             "buffer pos: "   + buffer.position()                        + "\t" +
          //             "buffer rem: "   + buffer.remaining()                       + "\t" +
          //             "state: "        + state
          //);
          
          // Use a State Machine to process the byte stream.
          // Start building an rbnb frame for the entire sample, first by 
          // inserting a timestamp into the channelMap.  This time is merely
          // the time of insert into the data turbine, not the time of
          // observations of the measurements.  That time should be parsed out
          // of the sample in the Sink client code
    
          switch( state ) {
    
            case 0:
              
              // the network registration status should end in OK\r\n
              // note bytes are in reverse order in the FIFO window
              if ( byteOne   == 0x0A && byteTwo  == 0x0D && 
                   byteThree == 0x4B && byteFour == 0x4F ) {
                
                log.debug("Received the registration status result.");
                
                this.resultByteCount++; // add the last byte found to the count
                
                // add the last byte found to the result buffer
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                
                } else {
                  resultBuffer.compact();
                  resultBuffer.put(byteOne);
                
                }
                
                // report the network registration status string
                resultArray = new byte[this.resultByteCount];
                resultBuffer.flip();
                resultBuffer.get(resultArray);
                resultString = new String(resultArray, "US-ASCII");
                log.debug("Network Registration Result: " +
                             resultString.trim());
                
                resultBuffer.clear();
                this.resultByteCount = 0;
                resultArray = new byte[0];
                resultString = "";
                byteOne   = 0x00;
                byteTwo   = 0x00;
                byteThree = 0x00;
                byteFour  = 0x00;
                
                // send a request for the signal strength
                this.command = this.MODEM_COMMAND_PREFIX +
                               this.SIGNAL_STRENGTH_COMMAND +
                               this.MODEM_COMMAND_SUFFIX;
                this.sentCommand = queryInstrument(this.command);
                // allow time for the modem to respond
                streamingThread.sleep(this.SLEEP_INTERVAL);
                
                state = 1;
                break;
    
              } else {
                this.resultByteCount++; // add the last byte found to the count
                
                // add the last byte found to the result buffer
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                
                } else {
                  resultBuffer.compact();
                  resultBuffer.put(byteOne);
                
                }
                
                break;                
              }
            
            case 1: // report the signal strength of the Iridium modem
              
              // the signal strength status should end in OK\r\n
              // note bytes are in reverse order in the FIFO window
              if ( byteOne   == 0x0A && byteTwo  == 0x0D && 
                   byteThree == 0x4B && byteFour == 0x4F ) {
                
                log.debug("Received the signal strength result.");
                
                this.resultByteCount++; // add the last byte found to the count
                
                // add the last byte found to the result buffer
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                
                } else {
                  resultBuffer.compact();
                  resultBuffer.put(byteOne);
                  
                }                

                // report the signal strength status string
                resultArray = new byte[this.resultByteCount];
                resultBuffer.flip();
                resultBuffer.get(resultArray);
                resultString = new String(resultArray, "US-ASCII");
                log.debug("Signal Strength Result: " +
                             resultString.trim());
                
                int signalStrengthIndex = resultString.indexOf(
                                          this.SIGNAL_STRENGTH) + 5;
                                          
                int signalStrength = 
                  new Integer(resultString.substring(
                    signalStrengthIndex, signalStrengthIndex + 1)).intValue();
                    
                // test if the signal strength is above the threshold
                if ( signalStrength > SIGNAL_THRESHOLD ) {
                  
                  resultBuffer.clear();
                  this.resultByteCount = 0;
                  resultArray = new byte[0];
                  resultString = "";
                  byteOne   = 0x00;
                  byteTwo   = 0x00;
                  byteThree = 0x00;
                  byteFour  = 0x00;
                  
                  state = 2;
                  break;
                  
                // the signal strength is too low, check again
                } else {
                  
                  resultBuffer.clear();
                  this.resultByteCount = 0;
                  resultArray = new byte[0];
                  resultString = "";
                  byteOne   = 0x00;
                  byteTwo   = 0x00;
                  byteThree = 0x00;
                  byteFour  = 0x00;
                  
                  // resend a request for the signal strength
                  this.command = this.MODEM_COMMAND_PREFIX +
                                 this.SIGNAL_STRENGTH_COMMAND +
                                 this.MODEM_COMMAND_SUFFIX;
                  this.sentCommand = queryInstrument(this.command);
                  // allow time for the modem to respond
                  Thread.sleep(this.SLEEP_INTERVAL);
                  
                  state = 1;
                  break;
                  
                }
                
              } else {

                // still in the middle of the result, keep adding bytes
                this.resultByteCount++; // add each byte found

                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                } else {
                  resultBuffer.compact();
                  log.debug("Compacting resultBuffer ...");
                  resultBuffer.put(byteOne);
                  
                }
                
                break;
              }
          
            case 2: // handle the RING command from the instrument
              
              // listen for the RING command 
              // note bytes are in reverse order in the FIFO window
              if ( byteOne   == 0x47 && byteTwo  == 0x4E && 
                   byteThree == 0x49 && byteFour == 0x52 ) {
                
                log.debug("Received the RING command.");
                
                this.resultByteCount++; // add the last byte found to the count
                
                // add the last byte found to the result buffer
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                
                } else {
                  resultBuffer.compact();
                  resultBuffer.put(byteOne);
                
                }                
                
                resultBuffer.clear();
                this.resultByteCount = 0;
                resultArray = new byte[0];
                resultString = "";
                byteOne   = 0x00;
                byteTwo   = 0x00;
                byteThree = 0x00;
                byteFour  = 0x00;
                
                // answer the call
                this.command = this.MODEM_COMMAND_PREFIX +
                               this.ANSWER_COMMAND +
                               this.MODEM_COMMAND_SUFFIX;
                this.sentCommand = queryInstrument(this.command);
                // allow time for the modem to respond
                Thread.sleep(this.SLEEP_INTERVAL);
                
                state = 3;
                break;
                
              } else {
                
                // still in the middle of the result, keep adding bytes
                this.resultByteCount++; // add each byte found

                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                } else {
                  resultBuffer.compact();
                  log.debug("Compacting resultBuffer ...");
                  resultBuffer.put(byteOne);
                  
                }
                
                break;
                
              }
            
            case 3: // acknowledge the connection
            
              // the ready status string should end in READY\r
              // note bytes are in reverse order in the FIFO window
              if ( byteOne   == 0x0D && byteTwo  == 0x59 && 
                   byteThree == 0x44 && byteFour == 0x41) {
                
                log.debug("Received the ready status result.");
                
                this.resultByteCount++; // add the last byte found to the count
                
                // add the last byte found to the result buffer
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                
                } else {
                  resultBuffer.compact();
                  resultBuffer.put(byteOne);
                
                }                
                
                // report the connect rate and ready status string
                resultArray = new byte[this.resultByteCount];
                resultBuffer.flip();
                resultBuffer.get(resultArray);
                resultString = new String(resultArray, StandardCharsets.US_ASCII);
                
                // test the connect rate
                log.debug("Result from ATA: " + resultString);
                
                if ( resultString.indexOf(this.CONNECT_RATE) > 0 ) {
                  log.debug("Connect Rate Result: " +
                               this.CONNECT_RATE);
                  
                  // test the ready status
                  if ( resultString.indexOf(this.READY_STATUS) > 0 ) {
                    log.debug("Connect Rate Result: " +
                                 this.READY_STATUS);
                  
                    resultBuffer.clear();
                    this.resultByteCount = 0;
                    resultArray = new byte[0];
                    resultString = "";
                    byteOne   = 0x00;
                    byteTwo   = 0x00;
                    byteThree = 0x00;
                    byteFour  = 0x00;

                    // acknowledge the ready status
                    this.command = this.ACKNOWLEDGE_COMMAND +
                                   this.MODEM_COMMAND_SUFFIX;
                    this.sentCommand = queryInstrument(this.command);

                    // allow time for the modem to receive the ACK
                    Thread.sleep(this.SLEEP_INTERVAL);
                    
                    // query the instrument id
                    this.command = this.ID_COMMAND +
                                   this.MODEM_COMMAND_SUFFIX;
                    this.sentCommand = queryInstrument(this.command);
                    
                    // allow time for the modem to respond
                    Thread.sleep(this.SLEEP_INTERVAL);

                    state = 4;
                    break;
                    
                  } else {
                    log.debug("The ready status differs from: " +
                                 this.READY_STATUS);
                  
                    // throw an exception here?
                    break;
                  }
                  
                } else {
                  log.debug("The connect rate differs from: " +
                               this.CONNECT_RATE);
                  
                  // throw an exception here?
                  break;
                }
                
              } else {
              
                // still in the middle of the result, keep adding bytes
                this.resultByteCount++; // add each byte found
              
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                } else {
                  resultBuffer.compact();
                  log.debug("Compacting resultBuffer ...");
                  resultBuffer.put(byteOne);
              
                }
              
                break;
              
              }
              
            case 4: // get the instrument id
              
              // the instrument ID string should end in \r
              if ( byteOne == 0x0D ) {
                
                log.debug("Received the instrument ID result.");
                
                this.resultByteCount++; // add the last byte found to the count
                
                // add the last byte found to the result buffer
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                
                } else {
                  resultBuffer.compact();
                  resultBuffer.put(byteOne);
                
                }                
                
                // report the instrument ID string
                resultArray = new byte[this.resultByteCount];
                resultBuffer.flip();
                resultBuffer.get(resultArray);
                resultString = new String(resultArray, StandardCharsets.US_ASCII);
                log.debug("Seahorse Instrument ID: " + resultString.trim());
                
                // set the platformID variable
                this.platformID = resultString.substring(0, resultString.length() - 1);
                
                resultBuffer.clear();
                this.resultByteCount = 0;
                resultArray = new byte[0];
                resultString = "";
                byteOne   = 0x00;
                byteTwo   = 0x00;
                byteThree = 0x00;
                byteFour  = 0x00;
                
                // query the battery voltage
                this.command = this.BATTERY_VOLTAGE_COMMAND +
                               this.MODEM_COMMAND_SUFFIX;
                this.sentCommand = queryInstrument(this.command);
                
                // allow time for the modem to respond
                Thread.sleep(this.SLEEP_INTERVAL);
                
                state = 5;
                break;
                
              } else {
              
                // still in the middle of the result, keep adding bytes
                this.resultByteCount++; // add each byte found
              
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                } else {
                  resultBuffer.compact();
                  log.debug("Compacting resultBuffer ...");
                  resultBuffer.put(byteOne);
              
                }
              
                break;
              
              }
              
            case 5: // get the seahorse battery voltage
              
              // the battery voltage string should end in \r
              if ( byteOne == 0x0D ) {
                
                log.debug("Received the instrument battery voltage result.");
                
                this.resultByteCount++; // add the last byte found to the count
                
                // add the last byte found to the result buffer
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                
                } else {
                  resultBuffer.compact();
                  resultBuffer.put(byteOne);
                
                }                
                
                // report the battery voltage string
                resultArray = new byte[this.resultByteCount];
                resultBuffer.flip();
                resultBuffer.get(resultArray);
                resultString = new String(resultArray, StandardCharsets.US_ASCII);
                log.debug("Seahorse Battery Voltage: " + resultString.trim());
                
                resultBuffer.clear();
                this.resultByteCount = 0;
                resultArray = new byte[0];
                resultString = "";
                byteOne   = 0x00;
                byteTwo   = 0x00;
                byteThree = 0x00;
                byteFour  = 0x00;
                
                // query the GPS location
                this.command = this.GPRMC_COMMAND +
                               this.MODEM_COMMAND_SUFFIX;
                this.sentCommand = queryInstrument(this.command);
                
                // allow time for the modem to respond
                Thread.sleep(this.SLEEP_INTERVAL);
                
                state = 6;
                break;
                
              } else {
              
                // still in the middle of the result, keep adding bytes
                this.resultByteCount++; // add each byte found
              
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                } else {
                  resultBuffer.compact();
                  log.debug("Compacting resultBuffer ...");
                  resultBuffer.put(byteOne);
              
                }
              
                break;
              
              }
              
            case 6:
              
              // the GPRMC string should end in END\r
              // note bytes are in reverse order in the FIFO window
              if ( byteOne == 0x0D && byteTwo ==  0x44 && 
                   byteThree == 0x4E && byteFour == 0x45 ) {
                
                log.debug("Received the GPRMS result.");
                
                this.resultByteCount++; // add the last byte found to the count
                
                // add the last byte found to the result buffer
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                
                } else {
                  resultBuffer.compact();
                  resultBuffer.put(byteOne);
                
                }                
                
                // report the GPRMC string
                resultArray = new byte[this.resultByteCount];
                resultBuffer.flip();
                resultBuffer.get(resultArray);
                resultString = new String(resultArray, StandardCharsets.US_ASCII);
                log.debug("Seahorse GPRMC string: " + resultString.trim());
                
                resultBuffer.clear();
                this.resultByteCount = 0;
                resultArray = new byte[0];
                resultString = "";
                byteOne   = 0x00;
                byteTwo   = 0x00;
                byteThree = 0x00;
                byteFour  = 0x00;
                
                // query the file name for transfer
                this.command = this.FILENAME_COMMAND +
                               this.MODEM_COMMAND_SUFFIX;
                this.sentCommand = queryInstrument(this.command);
                
                // allow time for the modem to respond
                Thread.sleep(this.SLEEP_INTERVAL);
                
                state = 7;
                break;
                
              } else {
              
                // still in the middle of the result, keep adding bytes
                this.resultByteCount++; // add each byte found
              
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                } else {
                  resultBuffer.compact();
                  log.debug("Compacting resultBuffer ...");
                  resultBuffer.put(byteOne);
              
                }
              
                break;
              
              }
              
            case 7:
              
              // the file name string should end in .Z\r
              // note bytes are in reverse order in the FIFO window
              if ( byteOne == 0x0D && byteTwo == 0x5A && byteThree == 0x2E) {
                
                log.debug("Received the file name result.");
                
                this.resultByteCount++; // add the last byte found to the count
                
                // add the last byte found to the result buffer
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                
                } else {
                  resultBuffer.compact();
                  resultBuffer.put(byteOne);
                
                }                
                
                // report the file name string
                resultArray = new byte[this.resultByteCount];
                resultBuffer.flip();
                resultBuffer.get(resultArray);
                resultString = new String(resultArray, StandardCharsets.US_ASCII);
                log.debug("File name result: " + resultString.trim());
                
                resultString = resultString.trim();
                int fileNameIndex = resultString.indexOf(this.FILENAME_PREFIX);
                
                //extract just the filename from the result (excise the "FILE=")
                this.fileNameToDownload = 
                  resultString.substring(
                    (fileNameIndex + (this.FILENAME_PREFIX).length()), 
                    resultString.length());
                
                log.debug("File name to download: " + this.fileNameToDownload);
                
                // test to see if the GFN command returns FILES=NONE
                if ( !(resultString.indexOf(this.END_OF_FILES) > 0) ) {
                  
                  // there is a file to download. parse the file name,
                  // get the number of blocks to transfer
                  this.command = this.NUMBER_OF_BLOCKS_COMMAND +
                                 this.MODEM_COMMAND_SUFFIX;
                  this.sentCommand = queryInstrument(this.command);

                  // allow time for the modem to respond
                  Thread.sleep(this.SLEEP_INTERVAL);
                  
                  resultBuffer.clear();
                  this.resultByteCount = 0;
                  resultArray = new byte[0];
                  resultString = "";
                  byteOne   = 0x00;
                  byteTwo   = 0x00;
                  byteThree = 0x00;
                  byteFour  = 0x00;
                  
                  state = 8;
                  break;
                  
                } else {
                  
                  // We have downloaded all files. Parse the data string,
                  // build the channel map, and flush the data to the Dataturbine
                  // by iterating through the data matrix.  The metadata and
                  // ASCII data strings are flushed once with the first matrix
                  // row.
                  
                  // Parse the data file, not the cast file.
                  try {
                    
                    // parse the CTD data file
                    this.ctdParser = new CTDParser(this.dataFileString);
                    
                    // convert the raw frequencies and voltages to engineering
                    // units and return the data as a matrix
                    CTDConverter ctdConverter = new CTDConverter(this.ctdParser);
                    ctdConverter.convert();
                    RealMatrix convertedDataMatrix =
                      ctdConverter.getConvertedDataValuesMatrix();
                    
                    // Register the data and metadata channels;
                    failed = register();
                    
                    if ( ! failed ) {
                      // format the first sample date and use it as the first insert
                      // date.  Add the sampleInterval on each iteration to insert
                      // subsequent data rows.  Sample interval is by default 
                      // 4 scans/second for the CTD.
                      DATE_FORMAT.setTimeZone(TZ);
                      this.sampleDateTime = Calendar.getInstance();
                      this.sampleDateTime.setTime(
                        DATE_FORMAT.parse(ctdParser.getFirstSampleTime()));
                      
                      for (int row = 0; row < convertedDataMatrix.getRowDimension(); row++) {
                        
                        // Only insert the metadata fields and full ASCII text strings
                        // with the first row of data
                        if ( row == 0 ) {
                          // Add the samplingMode data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("samplingMode");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "text/plain");
                          this.rbnbChannelMap.PutDataAsString(this.channelIndex, this.ctdParser.getSamplingMode());
                          
                          // Add the firstSampleTime data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("firstSampleTime");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "text/plain");
                          this.rbnbChannelMap.PutDataAsString(this.channelIndex, this.ctdParser.getFirstSampleTime());
                          
                          // Add the fileName data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("fileName");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "text/plain");
                          this.rbnbChannelMap.PutDataAsString(this.channelIndex, this.ctdParser.getFileName());
                          
                          // Add the temperatureSerialNumber data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("temperatureSerialNumber");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "text/plain");
                          this.rbnbChannelMap.PutDataAsString(this.channelIndex, this.ctdParser.getTemperatureSerialNumber());
                          
                          // Add the conductivitySerialNumber data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("conductivitySerialNumber");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "text/plain");
                          this.rbnbChannelMap.PutDataAsString(this.channelIndex, this.ctdParser.getConductivitySerialNumber());
                          
                          // Add the systemUpLoadTime data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("systemUpLoadTime");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "text/plain");
                          this.rbnbChannelMap.PutDataAsString(this.channelIndex, this.ctdParser.getSystemUpLoadTime());
                          
                          // Add the cruiseInformation data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("cruiseInformation");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "text/plain");
                          this.rbnbChannelMap.PutDataAsString(this.channelIndex, this.ctdParser.getCruiseInformation());
                          
                          // Add the stationInformation data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("stationInformation");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "text/plain");
                          this.rbnbChannelMap.PutDataAsString(this.channelIndex, this.ctdParser.getStationInformation());
                          
                          // Add the shipInformation data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("shipInformation");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "text/plain");
                          this.rbnbChannelMap.PutDataAsString(this.channelIndex, this.ctdParser.getShipInformation());
                          
                          // Add the chiefScientist data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("chiefScientist");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "text/plain");
                          this.rbnbChannelMap.PutDataAsString(this.channelIndex, this.ctdParser.getChiefScientist());
                          
                          // Add the organization data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("organization");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "text/plain");
                          this.rbnbChannelMap.PutDataAsString(this.channelIndex, this.ctdParser.getOrganization());
                          
                          // Add the areaOfOperation data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("areaOfOperation");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "text/plain");
                          this.rbnbChannelMap.PutDataAsString(this.channelIndex, this.ctdParser.getAreaOfOperation());
                          
                          // Add the instrumentPackage data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("instrumentPackage");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "text/plain");
                          this.rbnbChannelMap.PutDataAsString(this.channelIndex, this.ctdParser.getInstrumentPackage());
                          
                          // Add the mooringNumber data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("mooringNumber");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "text/plain");
                          this.rbnbChannelMap.PutDataAsString(this.channelIndex, this.ctdParser.getMooringNumber());
                          
                          // Add the instrumentLatitude data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("instrumentLatitude");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsFloat64(this.channelIndex, new double []{this.ctdParser.getInstrumentLatitude()});
                          
                          // Add the instrumentLongitude data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("instrumentLongitude");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsFloat64(this.channelIndex, new double []{this.ctdParser.getInstrumentLongitude()});
                          
                          // Add the depthSounding data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("depthSounding");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsFloat64(this.channelIndex, new double []{this.ctdParser.getDepthSounding()});
                          
                          // Add the profileNumber data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("profileNumber");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "text/plain");
                          this.rbnbChannelMap.PutDataAsString(this.channelIndex, this.ctdParser.getProfileNumber());
                          
                          // Add the profileDirection data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("profileDirection");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "text/plain");
                          this.rbnbChannelMap.PutDataAsString(this.channelIndex, this.ctdParser.getProfileDirection());
                          
                          // Add the deploymentNotes data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("deploymentNotes");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "text/plain");
                          this.rbnbChannelMap.PutDataAsString(this.channelIndex, this.ctdParser.getDeploymentNotes());
                          
                          // Add the mainBatteryVoltage data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("mainBatteryVoltage");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsFloat64(this.channelIndex, new double []{this.ctdParser.getMainBatteryVoltage()});
                          
                          // Add the lithiumBatteryVoltage data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("lithiumBatteryVoltage");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsFloat64(this.channelIndex, new double []{this.ctdParser.getLithiumBatteryVoltage()});
                          
                          // Add the operatingCurrent data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("operatingCurrent");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsFloat64(this.channelIndex, new double []{this.ctdParser.getOperatingCurrent()});
                          
                          // Add the pumpCurrent data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("pumpCurrent");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsFloat64(this.channelIndex, new double []{this.ctdParser.getPumpCurrent()});
                          
                          // Add the channels01ExternalCurrent data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("channels01ExternalCurrent");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsFloat64(this.channelIndex, new double []{this.ctdParser.getChannels01ExternalCurrent()});
                          
                          // Add the channels23ExternalCurrent data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("channels23ExternalCurrent");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsFloat64(this.channelIndex, new double []{this.ctdParser.getChannels23ExternalCurrent()});
                          
                          // Add the loggingStatus data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("loggingStatus");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "text/plain");
                          this.rbnbChannelMap.PutDataAsString(this.channelIndex, this.ctdParser.getLoggingStatus());
                          
                          // Add the numberOfScansToAverage data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("numberOfScansToAverage");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsInt32(this.channelIndex, new int []{this.ctdParser.getNumberOfScansToAverage()});
                          
                          // Add the numberOfSamples data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("numberOfSamples");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsInt32(this.channelIndex, new int []{this.ctdParser.getNumberOfSamples()});
                          
                          // Add the numberOfAvailableSamples data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("numberOfAvailableSamples");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsInt32(this.channelIndex, new int []{this.ctdParser.getNumberOfAvailableSamples()});
                          
                          // Add the sampleInterval data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("sampleInterval");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsInt32(this.channelIndex, new int []{this.ctdParser.getSampleInterval()});
                          
                          // Add the measurementsPerSample data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("measurementsPerSample");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsInt32(this.channelIndex, new int []{this.ctdParser.getMeasurementsPerSample()});
                          
                          // Add the transmitRealtime data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("transmitRealtime");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "text/plain");
                          this.rbnbChannelMap.PutDataAsString(this.channelIndex, this.ctdParser.getTransmitRealtime());
                          
                          // Add the numberOfCasts data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("numberOfCasts");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsInt32(this.channelIndex, new int []{this.ctdParser.getNumberOfCasts()});
                          
                          // Add the minimumConductivityFrequency data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("minimumConductivityFrequency");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsInt32(this.channelIndex, new int []{this.ctdParser.getMinimumConductivityFrequency()});
                          
                          // Add the pumpDelay data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("pumpDelay");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsInt32(this.channelIndex, new int []{this.ctdParser.getPumpDelay()});
                          
                          // Add the automaticLogging data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("automaticLogging");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "text/plain");
                          this.rbnbChannelMap.PutDataAsString(this.channelIndex, this.ctdParser.getAutomaticLogging());
                          
                          // Add the ignoreMagneticSwitch data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("ignoreMagneticSwitch");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "text/plain");
                          this.rbnbChannelMap.PutDataAsString(this.channelIndex, this.ctdParser.getIgnoreMagneticSwitch());
                          
                          // Add the batteryType data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("batteryType");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "text/plain");
                          this.rbnbChannelMap.PutDataAsString(this.channelIndex, this.ctdParser.getBatteryType());
                          
                          // Add the batteryCutoff data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("batteryCutoff");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "text/plain");
                          this.rbnbChannelMap.PutDataAsString(this.channelIndex, this.ctdParser.getBatteryCutoff());
                          
                          // Add the pressureSensorType data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("pressureSensorType");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "text/plain");
                          this.rbnbChannelMap.PutDataAsString(this.channelIndex, this.ctdParser.getPressureSensorType());
                          
                          // Add the pressureSensorRange data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("pressureSensorRange");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "text/plain");
                          this.rbnbChannelMap.PutDataAsString(this.channelIndex, this.ctdParser.getPressureSensorRange());
                          
                          // Add the sbe38TemperatureSensor data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("sbe38TemperatureSensor");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "text/plain");
                          this.rbnbChannelMap.PutDataAsString(this.channelIndex, this.ctdParser.getSbe38TemperatureSensor());
                          
                          // Add the gasTensionDevice data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("gasTensionDevice");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "text/plain");
                          this.rbnbChannelMap.PutDataAsString(this.channelIndex, this.ctdParser.getGasTensionDevice());
                          
                          // Add the externalVoltageChannelZero data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("externalVoltageChannelZero");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "text/plain");
                          this.rbnbChannelMap.PutDataAsString(this.channelIndex, this.ctdParser.getExternalVoltageChannelZero());
                          
                          // Add the externalVoltageChannelOne data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("externalVoltageChannelOne");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "text/plain");
                          this.rbnbChannelMap.PutDataAsString(this.channelIndex, this.ctdParser.getExternalVoltageChannelOne());
                          
                          // Add the externalVoltageChannelTwo data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("externalVoltageChannelTwo");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "text/plain");
                          this.rbnbChannelMap.PutDataAsString(this.channelIndex, this.ctdParser.getExternalVoltageChannelTwo());
                          
                          // Add the externalVoltageChannelThree data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("externalVoltageChannelThree");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "text/plain");
                          this.rbnbChannelMap.PutDataAsString(this.channelIndex, this.ctdParser.getExternalVoltageChannelThree());
                          
                          // Add the echoCommands data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("echoCommands");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "text/plain");
                          this.rbnbChannelMap.PutDataAsString(this.channelIndex, this.ctdParser.getEchoCommands());
                          
                          // Add the outputFormat data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("outputFormat");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "text/plain");
                          this.rbnbChannelMap.PutDataAsString(this.channelIndex, this.ctdParser.getOutputFormat());
                          
                          // Add the temperatureCalibrationDate data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("temperatureCalibrationDate");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "text/plain");
                          this.rbnbChannelMap.PutDataAsString(this.channelIndex, this.ctdParser.getTemperatureCalibrationDate());
                          
                          // Add the temperatureCoefficientTA0 data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("temperatureCoefficientTA0");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsFloat64(this.channelIndex, new double []{this.ctdParser.getTemperatureCoefficientTA0()});
                          
                          // Add the temperatureCoefficientTA1 data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("temperatureCoefficientTA1");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsFloat64(this.channelIndex, new double []{this.ctdParser.getTemperatureCoefficientTA1()});
                          
                          // Add the temperatureCoefficientTA2 data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("temperatureCoefficientTA2");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsFloat64(this.channelIndex, new double []{this.ctdParser.getTemperatureCoefficientTA2()});
                          
                          // Add the temperatureCoefficientTA3 data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("temperatureCoefficientTA3");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsFloat64(this.channelIndex, new double []{this.ctdParser.getTemperatureCoefficientTA3()});
                          
                          // Add the temperatureOffsetCoefficient data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("temperatureOffsetCoefficient");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsFloat64(this.channelIndex, new double []{this.ctdParser.getTemperatureOffsetCoefficient()});
                          
                          // Add the conductivityCalibrationDate data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("conductivityCalibrationDate");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "text/plain");
                          this.rbnbChannelMap.PutDataAsString(this.channelIndex, this.ctdParser.getConductivityCalibrationDate());
                          
                          // Add the conductivityCoefficientG data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("conductivityCoefficientG");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsFloat64(this.channelIndex, new double []{this.ctdParser.getConductivityCoefficientG()});
                          
                          // Add the conductivityCoefficientH data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("conductivityCoefficientH");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsFloat64(this.channelIndex, new double []{this.ctdParser.getConductivityCoefficientH()});
                          
                          // Add the conductivityCoefficientI data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("conductivityCoefficientI");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsFloat64(this.channelIndex, new double []{this.ctdParser.getConductivityCoefficientI()});
                          
                          // Add the conductivityCoefficientJ data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("conductivityCoefficientJ");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsFloat64(this.channelIndex, new double []{this.ctdParser.getConductivityCoefficientJ()});
                          
                          // Add the conductivityCoefficientCF0 data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("conductivityCoefficientCF0");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsFloat64(this.channelIndex, new double []{this.ctdParser.getConductivityCoefficientCF0()});
                          
                          // Add the conductivityCoefficientCPCOR data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("conductivityCoefficientCPCOR");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsFloat64(this.channelIndex, new double []{this.ctdParser.getConductivityCoefficientCPCOR()});
                          
                          // Add the conductivityCoefficientCTCOR data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("conductivityCoefficientCTCOR");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsFloat64(this.channelIndex, new double []{this.ctdParser.getConductivityCoefficientCTCOR()});
                          
                          // Add the conductivityCoefficientCSLOPE data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("conductivityCoefficientCSLOPE");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsFloat64(this.channelIndex, new double []{this.ctdParser.getConductivityCoefficientCSLOPE()});
                          
                          // Add the pressureSerialNumber data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("pressureSerialNumber");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "text/plain");
                          this.rbnbChannelMap.PutDataAsString(this.channelIndex, this.ctdParser.getPressureSerialNumber());
                          
                          // Add the pressureCoefficientPA0 data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("pressureCoefficientPA0");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsFloat64(this.channelIndex, new double []{this.ctdParser.getPressureCoefficientPA0()});
                          
                          // Add the pressureCoefficientPA1 data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("pressureCoefficientPA1");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsFloat64(this.channelIndex, new double []{this.ctdParser.getPressureCoefficientPA1()});
                          
                          // Add the pressureCoefficientPA2 data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("pressureCoefficientPA2");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsFloat64(this.channelIndex, new double []{this.ctdParser.getPressureCoefficientPA2()});
                          
                          // Add the pressureCoefficientPTCA0 data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("pressureCoefficientPTCA0");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsFloat64(this.channelIndex, new double []{this.ctdParser.getPressureCoefficientPTCA0()});
                          
                          // Add the pressureCoefficientPTCA1 data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("pressureCoefficientPTCA1");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsFloat64(this.channelIndex, new double []{this.ctdParser.getPressureCoefficientPTCA1()});
                          
                          // Add the pressureCoefficientPTCA2 data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("pressureCoefficientPTCA2");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsFloat64(this.channelIndex, new double []{this.ctdParser.getPressureCoefficientPTCA2()});
                          
                          // Add the pressureCoefficientPTCB0 data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("pressureCoefficientPTCB0");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsFloat64(this.channelIndex, new double []{this.ctdParser.getPressureCoefficientPTCB0()});
                          
                          // Add the pressureCoefficientPTCB1 data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("pressureCoefficientPTCB1");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsFloat64(this.channelIndex, new double []{this.ctdParser.getPressureCoefficientPTCB1()});
                          
                          // Add the pressureCoefficientPTCB2 data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("pressureCoefficientPTCB2");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsFloat64(this.channelIndex, new double []{this.ctdParser.getPressureCoefficientPTCB2()});
                          
                          // Add the pressureCoefficientPTEMPA0 data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("pressureCoefficientPTEMPA0");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsFloat64(this.channelIndex, new double []{this.ctdParser.getPressureCoefficientPTEMPA0()});
                          
                          // Add the pressureCoefficientPTEMPA1 data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("pressureCoefficientPTEMPA1");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsFloat64(this.channelIndex, new double []{this.ctdParser.getPressureCoefficientPTEMPA1()});
                          
                          // Add the pressureCoefficientPTEMPA2 data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("pressureCoefficientPTEMPA2");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsFloat64(this.channelIndex, new double []{this.ctdParser.getPressureCoefficientPTEMPA2()});
                          
                          // Add the pressureOffsetCoefficient data to the channel map
                          this.channelIndex = this.rbnbChannelMap.Add("pressureOffsetCoefficient");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "application/octet-stream");
                          this.rbnbChannelMap.PutDataAsFloat64(this.channelIndex, new double []{this.ctdParser.getPressureOffsetCoefficient()});
                          
                          // Insert the file into the channel map. 
                          this.channelIndex = this.rbnbChannelMap.Add(this.rbnbChannelName);
                          this.rbnbChannelMap.PutMime(this.channelIndex, "text/plain");
                          this.rbnbChannelMap.PutDataAsString(this.channelIndex, this.dataFileString);
                          
                          this.channelIndex = this.rbnbChannelMap.Add("ASCIICastData");
                          this.rbnbChannelMap.PutMime(this.channelIndex, "text/plain");
                          this.rbnbChannelMap.PutDataAsString(this.channelIndex, this.castFileString);
                            
                        } 
                        
                        // Add in the matrix data row to the map here
                        List<String> variableNames = ctdParser.getDataVariableNames();
                        List<String> variableUnits = ctdParser.getDataVariableUnits();
                        
                        // iterate through the variable names and add them to
                        // the channel map.
                        for (int variableIndex = 0; 
                                 variableIndex < variableNames.size(); 
                                 variableIndex++ ) {
                          
                          //  Add the variable name to the channel map
                          this.channelIndex = 
                            this.rbnbChannelMap.Add(variableNames.get(variableIndex));
                          // The matrix is a double array, so set the data type below
                          this.rbnbChannelMap.PutMime(this.channelIndex, 
                                                 "application/octet-stream");
                          // add the data to the map from the [row,column] of the
                          // matrix (row is from the outer for loop)
                          this.rbnbChannelMap.PutDataAsFloat64(this.channelIndex, 
                            new double []{convertedDataMatrix.getEntry(row, variableIndex)});
                          
                        }
                        
                        
                        // Flush the channel map to the RBNB
                        double sampleTimeAsSecondsSinceEpoch = (double)
                          (this.sampleDateTime.getTimeInMillis()/1000);
                        this.rbnbChannelMap.PutTime(sampleTimeAsSecondsSinceEpoch, 0d);
                        getSource().Flush(this.rbnbChannelMap);
                      
                        log.info("Flushed data to the DataTurbine.");
                        this.rbnbChannelMap.Clear(); 
                        
                        // samples are taken 4x per second, so increment the
                        // sample time by 250 milliseconds for the next insert                     
                        this.sampleDateTime.add(Calendar.MILLISECOND, 250);
                        
                      } // end for loop 
                      
                    } //  end if !failed

                  } catch ( Exception e ) {
                    log.debug("Failed to parse the CTD data file: " + 
                                  e.getMessage());
                                  
                  }
                  
                  // there are no more files to read. close the Tx session.
                  this.command = this.CLOSE_TRANSFER_SESSION_COMMAND +
                                 this.MODEM_COMMAND_SUFFIX;
                  this.sentCommand = queryInstrument(this.command);

                  // allow time for the modem to respond
                  Thread.sleep(this.SLEEP_INTERVAL);

                  // clean up
                  resultBuffer.clear();
                  this.resultByteCount = 0;
                  resultArray = new byte[0];
                  resultString = "";
                  byteOne   = 0x00;
                  byteTwo   = 0x00;
                  byteThree = 0x00;
                  byteFour  = 0x00;
                  
                  state = 10;
                  break;
                  
                }
                
              } else {
              
                // still in the middle of the result, keep adding bytes
                this.resultByteCount++; // add each byte found
              
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                } else {
                  resultBuffer.compact();
                  log.debug("Compacting resultBuffer ...");
                  resultBuffer.put(byteOne);
              
                }
              
                break;
              
              }
              
            case 8:
              
              // the number of blocks string should end in \r
              if ( byteOne == 0x0D ) {
                
                log.debug("Received the number of blocks result.");
                
                this.resultByteCount++; // add the last byte found to the count
                
                // add the last byte found to the result buffer
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                
                } else {
                  resultBuffer.compact();
                  resultBuffer.put(byteOne);
                
                }                
                
                // report the number of blocks string
                resultArray = new byte[this.resultByteCount];
                resultBuffer.flip();
                resultBuffer.get(resultArray);
                resultString = new String(resultArray, StandardCharsets.US_ASCII);
                log.debug("Number of bytes reported: " + resultString.trim());
                
                int numberOfBlocksIndex = resultString.indexOf(this.BLOCKSIZE_PREFIX);
                
                // If 'BLOCKSIZE=' is not found, set the index to 0
                if ( numberOfBlocksIndex == -1 ) {
                  numberOfBlocksIndex = 0;
                  
                }
                
                resultString = 
                  resultString.substring(
                    (numberOfBlocksIndex + (this.BLOCKSIZE_PREFIX).length()), 
                    resultString.length());
                                
                // convert the string to an integer
                try {
                  this.numberOfBlocks = Integer.parseInt(resultString.trim());
                  log.debug("Number of bytes to download: " + this.numberOfBlocks);
                
                } catch ( java.lang.NumberFormatException nfe ) {
                  failed = true;
                  nfe.printStackTrace();
                  log.debug("Failed to convert returned string value " + 
                  "to an integer value.  The returned string is: " + this.numberOfBlocks);
                      
                }
                                
                // test to see if the GNB command returns DONE\r
                if ( !(resultString.indexOf(this.TRANSFER_COMPLETE) > 0) ) {
                  
                  // there are bytes to transfer. send the transfer command
                  
                  this.command = this.TRANSFER_BLOCKS_COMMAND +
                                 this.MODEM_COMMAND_SUFFIX;
                  this.sentCommand = queryInstrument(this.command);

                  // allow time for the modem to respond
                  Thread.sleep(this.SLEEP_INTERVAL);

                  //resultBuffer.clear(); dont clear the buffer
                  this.resultByteCount = 0;
                  resultArray = new byte[0];
                  resultString = "";
                  byteOne   = 0x00;
                  byteTwo   = 0x00;
                  byteThree = 0x00;
                  byteFour  = 0x00;
                  
                  state = 9;
                  break;
                  
                } else {
                  
                  // there are no more bytes to transfer.  
                  
                  // Decompress the file, which is under zlib compression.  
                  Inflater inflater = new Inflater();
                  inflater.setInput(resultBuffer.array());
                  byte[] output = new byte[resultBuffer.capacity()];
                  
                  int numDecompressed = inflater.inflate(output);
                  
                  // set the appropriate string variable
                  if ( this.fileNameToDownload.indexOf(DATA_FILE_PREFIX) > 0 ) {
                    this.dataFileString = new String(output);
                    
                    //report the file contents to the log
                    log.debug("File " + this.fileNameToDownload + ": ");                   
                    log.debug(this.dataFileString);                   
                    
                  } else {
                    this.castFileString = new String(output);
                    
                    //report the file contents to the log
                    log.debug("File " + this.fileNameToDownload + ": ");                   
                    log.debug(this.castFileString);                   
                    
                    
                  }
                                    
                  // Ask for the next file.
                  this.command = this.FILENAME_COMMAND +
                                 this.MODEM_COMMAND_SUFFIX;
                  this.sentCommand = queryInstrument(this.command);

                  // allow time for the modem to respond
                  Thread.sleep(this.SLEEP_INTERVAL);

                  //resultBuffer.clear(); dont clear the buffer
                  this.resultByteCount = 0;
                  resultArray = new byte[0];
                  resultString = "";
                  byteOne   = 0x00;
                  byteTwo   = 0x00;
                  byteThree = 0x00;
                  byteFour  = 0x00;
                  
                  state = 7; //back to the file name state
                  break;
                  
                }
                
              } else {
              
                // still in the middle of the result, keep adding bytes
                this.resultByteCount++; // add each byte found
              
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                } else {
                  resultBuffer.compact();
                  log.debug("Compacting resultBuffer ...");
                  resultBuffer.put(byteOne);
              
                }
              
                break;
              
              }
          
            case 9:
            
              // transfer up to the reported number of bytes
              if ( this.resultByteCount == this.numberOfBlocks ) {
              
                // we have downloaded the reported bytes. get the next section.
                // get the number of blocks to transfer
                this.command = this.NUMBER_OF_BLOCKS_COMMAND +
                               this.MODEM_COMMAND_SUFFIX;
                this.sentCommand = queryInstrument(this.command);

                // allow time for the modem to respond
                Thread.sleep(this.SLEEP_INTERVAL);
                
                //resultBuffer.clear();
                this.resultByteCount = 0;
                byteOne   = 0x00;
                byteTwo   = 0x00;
                byteThree = 0x00;
                byteFour  = 0x00;
                
                state = 8;
                break;
                 
              
              } else {
              
                // still in the middle of the result, keep adding bytes
                this.resultByteCount++; // add each byte found
              
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                } else {
                  resultBuffer.compact();
                  log.debug("Compacting resultBuffer ...");
                  resultBuffer.put(byteOne);
              
                }
              
                break;
              
              }
            
            case 10:
              
              // the response from the modem should end in BYE\r
              // note bytes are in reverse order in the FIFO window
              if ( byteOne == 0x0D && byteTwo == 0x45 && 
                   byteThree == 0x59 && byteFour == 0x42 ) {
                
                log.debug("Received the BYE command.");
                
                // continue to disconnect. send the escape sequence
                this.command = this.ESCAPE_SEQUENCE_COMMAND +
                               this.MODEM_COMMAND_SUFFIX;
                this.sentCommand = queryInstrument(this.command);

                // allow time for the modem to respond
                Thread.sleep(this.SLEEP_INTERVAL);

                resultBuffer.clear();
                this.resultByteCount = 0;
                byteOne   = 0x00;
                byteTwo   = 0x00;
                byteThree = 0x00;
                byteFour  = 0x00;
                
                state = 11;
                break; 
                
              } else {
              
                // still in the middle of the result, keep adding bytes
                this.resultByteCount++; // add each byte found
              
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                } else {
                  resultBuffer.compact();
                  log.debug("Compacting resultBuffer ...");
                  resultBuffer.put(byteOne);
              
                }
              
                break;
              
              }
            
            case 11:
            
              // the response from the modem should end in OK\r\n
              // note bytes are in reverse order in the FIFO window
              if ( byteOne == 0x0D && byteTwo == 0x0A && 
                   byteThree == 0x4B && byteFour == 0x4F ) {
                
                // now hang up.
                this.command = this.MODEM_COMMAND_PREFIX +
                               this.HANGUP_COMMAND +
                               this.MODEM_COMMAND_SUFFIX;
                this.sentCommand = queryInstrument(this.command);

                // allow time for the modem to respond
                Thread.sleep(this.SLEEP_INTERVAL);

                resultBuffer.clear();
                this.resultByteCount = 0;
                byteOne   = 0x00;
                byteTwo   = 0x00;
                byteThree = 0x00;
                byteFour  = 0x00;
                
                state = 12;
                break; 
                
              } else {
              
                // still in the middle of the result, keep adding bytes
                this.resultByteCount++; // add each byte found
              
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                } else {
                  resultBuffer.compact();
                  log.debug("Compacting resultBuffer ...");
                  resultBuffer.put(byteOne);
              
                }
              
                break;
              
              }
            
            case 12:
            
              // the response from the modem should end in OK\r\n
              // note bytes are in reverse order in the FIFO window
              if ( byteOne == 0x0D && byteTwo == 0x0A && 
                   byteThree == 0x4B && byteFour == 0x4F ) {
                
                // we are done. re-test if is network registered
                this.command = this.MODEM_COMMAND_PREFIX +
                               this.REGISTRATION_STATUS_COMMAND +
                               this.MODEM_COMMAND_SUFFIX;
                this.sentCommand = queryInstrument(this.command);

                // allow time for the modem to respond
                Thread.sleep(this.SLEEP_INTERVAL);

                resultBuffer.clear();
                this.resultByteCount = 0;
                byteOne   = 0x00;
                byteTwo   = 0x00;
                byteThree = 0x00;
                byteFour  = 0x00;
                
                state = 0;
                break; 
                
              } else {
              
                // still in the middle of the result, keep adding bytes
                this.resultByteCount++; // add each byte found
              
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                } else {
                  resultBuffer.compact();
                  log.debug("Compacting resultBuffer ...");
                  resultBuffer.put(byteOne);
              
                }
              
                break;
              
              }
                            
          } // end switch statement
          
          // shift the bytes in the FIFO window
          byteFour = byteThree;
          byteThree = byteTwo;
          byteTwo = byteOne;

        } //end while (more unread bytes)
    
        // prepare the buffer to read in more bytes from the stream
        buffer.compact();
    
    
      } // end while (more socketChannel bytes to read)
      socketChannel.close();
        
    } catch ( IOException | InterruptedException | DataFormatException e ) {
      // handle exceptions
      // In the event of an i/o exception, log the exception, and allow execute()
      // to return false, which will prompt a retry.
      failed = true;
      e.printStackTrace();
      return !failed;

    }

      return !failed;
  } // end if (  !isConnected() ) 
  
   /**
   * A method used to the TCP socket of the remote source host for communication
   */
  protected SocketChannel getSocketConnection() {

    String host = getHostName();
    int portNumber = getHostPort();
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
      System.err.println("Unable to look up host: " + host + "\n");
      disconnect();
      dataSocket = null;
    } catch (IOException nioe ) {
      System.err.println("Couldn't get I/O connection to: " + host);
      disconnect();
      dataSocket = null;
    } catch (Exception e) {
      disconnect();
      dataSocket = null;            
    }
    return dataSocket;
    
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

  /** A method that queries the instrument with a command */
  public boolean queryInstrument(String command) {
    
    // the result of the query
    boolean result = false;
    
    // only send the command if the socket is connected
    if ( this.socketChannel.isConnected() ) {
      ByteBuffer commandBuffer = ByteBuffer.allocate( command.length() * 10);
      commandBuffer.put(command.getBytes());
      commandBuffer.flip();
      
      try {
        this.socketChannel.write(commandBuffer);
        log.debug("Wrote " + command + " to the socket channel.");
        result = true;
        
      } catch (IOException ioe ) {
        ioe.printStackTrace();
        result = false;
      }
    }
    return result;
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

  public static void main (String[] args) {
    
    log.info("SeahorseSource.main() called.");
    
    try {
      // create a new instance of the SeahorseSource object, and parse the command 
      // line arguments as settings for this instance
      final SeahorseSource seahorseSource = new SeahorseSource();
      
      // Set up a simple log that logs to the console
      PropertyConfigurator.configure(seahorseSource.getLogConfigurationFile());
      
      // parse the commandline arguments to configure the connection, then 
      // start the streaming connection between the source and the RBNB server.
      if ( seahorseSource.parseArgs(args) ) {
        seahorseSource.start();
      }
      
      // Handle ctrl-c's and other abrupt death signals to the process
      Runtime.getRuntime().addShutdownHook(new Thread() {
        // stop the streaming process
        public void run() {
          seahorseSource.stop();
        }
      }
      );

    } catch ( Exception e ) {
      log.info("Error in main(): " + e.getMessage());
      e.printStackTrace();
    }
  }
  
  /*
   * A method that registers the CTD data and metadata channels with the 
   * DataTurbine.
   *
   * return boolean - returns true if the registration result succeeds
   */
   private boolean register() {
     
     // Register the CTD data and metadata channels with the DataTurbine
     try {
       
       // Add the sample data channel as ASCII Hex
       this.channelIndex = this.rbnbChannelMap.Add(this.rbnbChannelName);
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the cast data as ASCII
       this.channelIndex = this.rbnbChannelMap.Add("ASCIICastData");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       
       // Add the samplingMode data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("samplingMode");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the firstSampleTime data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("firstSampleTime");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the fileName data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("fileName");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the temperatureSerialNumber data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("temperatureSerialNumber");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the conductivitySerialNumber data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("conductivitySerialNumber");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the systemUpLoadTime data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("systemUpLoadTime");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the cruiseInformation data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("cruiseInformation");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the stationInformation data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("stationInformation");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the shipInformation data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("shipInformation");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the chiefScientist data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("chiefScientist");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the organization data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("organization");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the areaOfOperation data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("areaOfOperation");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the instrumentPackage data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("instrumentPackage");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the mooringNumber data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("mooringNumber");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the instrumentLatitude data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("instrumentLatitude");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=degrees");
       
       // Add the instrumentLongitude data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("instrumentLongitude");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=degrees");
       
       // Add the depthSounding data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("depthSounding");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=m");
       
       // Add the profileNumber data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("profileNumber");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the profileDirection data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("profileDirection");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the deploymentNotes data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("deploymentNotes");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the mainBatteryVoltage data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("mainBatteryVoltage");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=v");
       
       // Add the lithiumBatteryVoltage data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("lithiumBatteryVoltage");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=v");
       
       // Add the operatingCurrent data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("operatingCurrent");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=ma");
       
       // Add the pumpCurrent data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("pumpCurrent");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=ma");
       
       // Add the channels01ExternalCurrent data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("channels01ExternalCurrent");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=ma");
       
       // Add the channels23ExternalCurrent data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("channels23ExternalCurrent");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=ma");
       
       // Add the loggingStatus data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("loggingStatus");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the numberOfScansToAverage data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("numberOfScansToAverage");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the numberOfSamples data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("numberOfSamples");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the numberOfAvailableSamples data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("numberOfAvailableSamples");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the sampleInterval data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("sampleInterval");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the measurementsPerSample data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("measurementsPerSample");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the transmitRealtime data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("transmitRealtime");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the numberOfCasts data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("numberOfCasts");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the minimumConductivityFrequency data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("minimumConductivityFrequency");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=Hz");
       
       // Add the pumpDelay data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("pumpDelay");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=s");
       
       // Add the automaticLogging data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("automaticLogging");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the ignoreMagneticSwitch data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("ignoreMagneticSwitch");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the batteryType data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("batteryType");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the batteryCutoff data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("batteryCutoff");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the pressureSensorType data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("pressureSensorType");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the pressureSensorRange data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("pressureSensorRange");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the sbe38TemperatureSensor data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("sbe38TemperatureSensor");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the gasTensionDevice data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("gasTensionDevice");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the externalVoltageChannelZero data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("externalVoltageChannelZero");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the externalVoltageChannelOne data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("externalVoltageChannelOne");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the externalVoltageChannelTwo data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("externalVoltageChannelTwo");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the externalVoltageChannelThree data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("externalVoltageChannelThree");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the echoCommands data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("echoCommands");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the outputFormat data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("outputFormat");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the temperatureCalibrationDate data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("temperatureCalibrationDate");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the temperatureCoefficientTA0 data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("temperatureCoefficientTA0");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the temperatureCoefficientTA1 data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("temperatureCoefficientTA1");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the temperatureCoefficientTA2 data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("temperatureCoefficientTA2");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the temperatureCoefficientTA3 data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("temperatureCoefficientTA3");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the temperatureOffsetCoefficient data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("temperatureOffsetCoefficient");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the conductivityCalibrationDate data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("conductivityCalibrationDate");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the conductivityCoefficientG data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("conductivityCoefficientG");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the conductivityCoefficientH data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("conductivityCoefficientH");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the conductivityCoefficientI data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("conductivityCoefficientI");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the conductivityCoefficientJ data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("conductivityCoefficientJ");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the conductivityCoefficientCF0 data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("conductivityCoefficientCF0");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the conductivityCoefficientCPCOR data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("conductivityCoefficientCPCOR");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the conductivityCoefficientCTCOR data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("conductivityCoefficientCTCOR");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the conductivityCoefficientCSLOPE data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("conductivityCoefficientCSLOPE");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the pressureSerialNumber data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("pressureSerialNumber");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the pressureCoefficientPA0 data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("pressureCoefficientPA0");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the pressureCoefficientPA1 data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("pressureCoefficientPA1");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the pressureCoefficientPA2 data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("pressureCoefficientPA2");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the pressureCoefficientPTCA0 data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("pressureCoefficientPTCA0");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the pressureCoefficientPTCA1 data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("pressureCoefficientPTCA1");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the pressureCoefficientPTCA2 data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("pressureCoefficientPTCA2");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the pressureCoefficientPTCB0 data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("pressureCoefficientPTCB0");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the pressureCoefficientPTCB1 data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("pressureCoefficientPTCB1");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the pressureCoefficientPTCB2 data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("pressureCoefficientPTCB2");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the pressureCoefficientPTEMPA0 data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("pressureCoefficientPTEMPA0");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the pressureCoefficientPTEMPA1 data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("pressureCoefficientPTEMPA1");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the pressureCoefficientPTEMPA2 data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("pressureCoefficientPTEMPA2");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       // Add the pressureOffsetCoefficient data to the channel map
       this.channelIndex = this.rbnbChannelMap.Add("pressureOffsetCoefficient");
       this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=none");
       
       List<String> unitNames = this.ctdParser.getDataVariableUnits();
       
       // iterate through the variable units and add them to the channel map.
       for (int unitIndex = 0; unitIndex < unitNames.size(); unitIndex++ ) {
         
         //  Add the unit name to the channel map
         this.channelIndex = 
           this.rbnbChannelMap.Add(unitNames.get(unitIndex));
         // The matrix is a double array, so set the data type below
         this.rbnbChannelMap.PutUserInfo(this.channelIndex, "units=" + unitNames.get(unitIndex));
         
       }
     
       // register the channel map of variables and units with the DataTurbine
       getSource().Register(this.rbnbChannelMap);
       // reset variables for use with the incoming data
       this.rbnbChannelMap.Clear();
       this.channelIndex = 0;
       
       return true;
       
     } catch ( SAPIException sapie ) {
       // In the event of an RBNB communication  exception, log the exception, 
       // and allow execute() to return false, which will prompt a retry.
       log.debug("There was a problem registering the channels." +
                    " The error message was: " + sapie.getMessage());
                    
       sapie.printStackTrace();
       return false;
         
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
          log.info("There was an execution problem. Retrying. Message is: " +
          e.getMessage());
        }
      }
    }
    // stop the streaming when we are done
    stop();
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
          log.info("Error: Enter a numeric value for the host port. " +
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
    options.addOption("H", true, "Source host name or IP *" + getHostName());
    options.addOption("P", true, "Source host port number *" + getHostPort());    
    options.addOption("C", true, "RBNB source channel name *" + getRBNBChannelName());
    //options.addOption("M", true, "RBNB archive mode *" + getArchiveMode());    
                      
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

  /** A method that interrupts the thread created in startThread() */
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
