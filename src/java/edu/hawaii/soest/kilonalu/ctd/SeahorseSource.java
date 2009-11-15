/**
 *  Copyright: 2007 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: To convert a Seacat ASCII data source into RBNB Data Turbine
 *             frames for archival and realtime access.
 *    Authors: Christopher Jones
 *
 * $HeadURL: $
 * $LastChangedDate: $
 * $LastChangedBy: cjones $
 * $LastChangedRevision:  $
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

import java.lang.StringBuffer;

import java.io.PrintWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.DataInputStream;
import java.io.IOException;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import java.util.zip.Inflater;
import java.util.zip.DataFormatException;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;

import org.apache.commons.codec.binary.Hex;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

import org.nees.rbnb.RBNBBase;
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
  
  /*
   *  A default RBNB channel name for the given source instrument
   */  
  private String DEFAULT_RBNB_CHANNEL = "HexadecimalASCIISampleData";

  /**
   * The name of the RBNB channel for this data stream
   */
  private String rbnbChannelName = DEFAULT_RBNB_CHANNEL;
  
  /*
   *  A default source IP address for the given source instrument
   */
  private final String DEFAULT_SOURCE_HOST_NAME = "127.0.0.1";  

  /**
   * The domain name or IP address of the host machine that this Source 
   * represents and from which the data will stream. 
   */
  private String sourceHostName = DEFAULT_SOURCE_HOST_NAME;

  /*
   *  A default source TCP port for the given source instrument
   */  
  private final int DEFAULT_SOURCE_HOST_PORT  = 2101;

  /**
   * The TCP port to connect to on the Source host machine 
   */
  private int sourceHostPort = DEFAULT_SOURCE_HOST_PORT;

  /*
   * The socket channel used to establish TCP communication with the instrument
   */
  private SocketChannel socketChannel;
  
  /**
   * The number of bytes in the ensemble as each byte is read from the stream
   */
  private int resultByteCount = 0;
  
  /**
   * The command prefix used to send commands to the microcontroller
   */ 
  private String MODEM_COMMAND_PREFIX = "AT";

  /**
   * The command suffix used to send commands to the microcontroller
   */ 
  private final String MODEM_COMMAND_SUFFIX = "\r";

  /**
   * The command used to get the network registration status from the Iridium modem
   */ 
  private final String REGISTRATION_STATUS_COMMAND = "+CREG?";

  /**
   * The command used to get the signal strength from the Iridium modem
   */ 
  private final String SIGNAL_STRENGTH_COMMAND = "+CSQ";

  /**
   * The command used to answer a RING call from the Iridium modem
   */ 
  private final String ANSWER_COMMAND = "A";

  /**
   * The command used to acknowledge the connection from the instrument
   */ 
  private final String ACKNOWLEDGE_COMMAND = "ACK";

  /**
   *The command used to get the ID from the instrument
   */ 
  private final String ID_COMMAND = "GID";

  /**
   *The platform ID of the instrument (i.e. the SeahHorse identifier, not the CTD)
   */ 
  private  String platformID = "";
  
  /**
   *The command used to get the battery voltage from the instrument
   */ 
  private final String BATTERY_VOLTAGE_COMMAND = "GBV";
  
  /**
   *The command used to get the GPRMC data string from the instrument
   */ 
  private final String GPRMC_COMMAND = "GPS";

  /**
   *The command used to get the name of the file to be downloaded from the instrument
   */ 
  private final String FILENAME_COMMAND = "GFN";

  /**
   *The command used to get the remaining number of blocks (bytes) from the instrument
   */ 
  private final String NUMBER_OF_BLOCKS_COMMAND = "GNB";

  /**
   *The remaining number of blocks (bytes) to download from the instrument
   */ 
  private int numberOfBlocks = 0;
  
  /**
   *The command used to transfer blocks (bytes) from the instrument
   */ 
  private final String TRANSFER_BLOCKS_COMMAND = "TXB";

  /**
   *The command used to disconnect (hang up) with the Iridium modem
   */ 
  private final String HANGUP_COMMAND = "H0";

  /**
   * The command used to close the transfer session with the instrument
   */ 
  private final String CLOSE_TRANSFER_SESSION_COMMAND = "REL";

  /**
   *The command used to escape to command mode with the Iridium modem
   */ 
  private final String ESCAPE_SEQUENCE_COMMAND = "+++";

  /**
   * The okay status string expected from the instrument
   */ 
  private final String OKAY_STATUS = "OK";
  
  /**
   * The signal strength string expected from Iridium modem
   */ 
  private final String SIGNAL_STRENGTH = "+CSQ:";
  
  /**
   * The signal strength threshold string needed from Iridium modem (0 - 5)
   */ 
  private final int SIGNAL_THRESHOLD = 3;
  
  /**
   * The registration status string expected from the instrument
   */ 
  private final String REGISTRATION_STATUS = "+CREG:";
  
  /**
   * The call ring string expected from the instrument
   */ 
  private final String CALL_RING = "RING";
  
  /**
   * The connect rate string expected from the instrument
   */ 
  private final String CONNECT_RATE = "CONNECT 19200";
  
  /**
   * The ready status string expected from the instrument
   */ 
  private final String READY_STATUS = "READY";
  
  /**
   * The file name prefix string expected from the instrument
   */ 
  private final String FILENAME_PREFIX = "FILE=";
    
  /**
   * The file name to be downloaded from the instrument
   */ 
  private String fileNameToDownload = "";
  
  /**
   * The prefix string expected at the beginning of the data file name
   */ 
  private final String DATA_FILE_PREFIX = "SH__";
  
  /**
   * The prefix string expected at the beginning of the cast file name
   */ 
  private final String CAST_FILE_PREFIX = "CAST";
  
  /**
   * The blocksize prefix string expected from the instrument
   */ 
  private final String BLOCKSIZE_PREFIX = "BLOCKSIZE=";
  
  /**
   * The transfer complete string expected from the instrument
   */ 
  private final String TRANSFER_COMPLETE = "DONE";
  
  /**
   * The end of files string expected from the instrument
   */ 
  private final String END_OF_FILES = "NONE";
  
  /**
   * The session closed string expected from the instrument
   */ 
  private final String SESSION_CLOSED = "BYE";
  
  /**
   *The command sent to the instrument
   */ 
  private String command;  
  
  /*
   * A boolean field indicating if a command has been sent to the instrument
   */
  private boolean sentCommand = false;
  
  /*
   * The instance of the CTDParser object used to parse the CTD
   * data file and retrieve each of the data fields
   */
   private CTDParser ctdParser = null;
   
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
  private static Logger logger = Logger.getLogger(SeahorseSource.class);

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
  private final int SLEEP_INTERVAL = 2000;
  
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
    logger.debug("SeahorseSource.execute() called.");
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
      
      // add a channel of data that will be pushed to the server.  
      // Each sample will be sent to the Data Turbine as an rbnb frame.
      ChannelMap rbnbChannelMap = new ChannelMap();
      int channelIndex = 0;
      
      // Add the sample data channel as ASCII Hex
      channelIndex = rbnbChannelMap.Add(this.rbnbChannelName);
      rbnbChannelMap.PutUserInfo(channelIndex, "units=none");
      
      // Add the cast data as ASCII
      channelIndex = rbnbChannelMap.Add("ASCIICastData");
      rbnbChannelMap.PutUserInfo(channelIndex, "units=none");
      
      // register the channel map of variables and units with the DataTurbine
      getSource().Register(rbnbChannelMap);
      // reset variables for use with the incoming data
      rbnbChannelMap.Clear();
      channelIndex = 0;
      
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

          //logger.debug("b1: " + new String(Hex.encodeHex((new byte[]{byteOne})))   + "\t" + 
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
                
                resultByteCount++; // add the last byte found to the count
                
                // add the last byte found to the result buffer
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                
                } else {
                  resultBuffer.compact();
                  resultBuffer.put(byteOne);
                
                }
                
                // report the network registration status string
                resultArray = new byte[resultByteCount];
                resultBuffer.flip();
                resultBuffer.get(resultArray);
                resultString = new String(resultArray, "US-ASCII");
                logger.debug("Network Registration Result: " +
                             resultString.trim());
                
                resultBuffer.clear();
                resultByteCount = 0;
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
                resultByteCount++; // add the last byte found to the count
                
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
                
                resultByteCount++; // add the last byte found to the count
                
                // add the last byte found to the result buffer
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                
                } else {
                  resultBuffer.compact();
                  resultBuffer.put(byteOne);
                  
                }                

                // report the signal strength status string
                resultArray = new byte[resultByteCount];
                resultBuffer.flip();
                resultBuffer.get(resultArray);
                resultString = new String(resultArray, "US-ASCII");
                logger.debug("Signal Strength Result: " +
                             resultString.trim());
                
                int signalStrengthIndex = resultString.indexOf(
                                          this.SIGNAL_STRENGTH) + 5;
                                          
                int signalStrength = 
                  new Integer(resultString.substring(
                    signalStrengthIndex, signalStrengthIndex + 1)).intValue();
                    
                // test if the signal strength is above the threshold
                if ( signalStrength > SIGNAL_THRESHOLD ) {
                  
                  resultBuffer.clear();
                  resultByteCount = 0;
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
                  resultByteCount = 0;
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
                  streamingThread.sleep(this.SLEEP_INTERVAL);
                  
                  state = 1;
                  break;
                  
                }
                
              } else {

                // still in the middle of the result, keep adding bytes
                resultByteCount++; // add each byte found

                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                } else {
                  resultBuffer.compact();
                  logger.debug("Compacting resultBuffer ...");
                  resultBuffer.put(byteOne);
                  
                }
                
                break;
              }
          
            case 2: // handle the RING command from the instrument
              
              // the signal strength status should end in OK\r\n
              // note bytes are in reverse order in the FIFO window
              if ( byteOne   == 0x47 && byteTwo  == 0x4E && 
                   byteThree == 0x49 && byteFour == 0x52 ) {
                
                resultByteCount++; // add the last byte found to the count
                
                // add the last byte found to the result buffer
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                
                } else {
                  resultBuffer.compact();
                  resultBuffer.put(byteOne);
                
                }                
                
                resultBuffer.clear();
                resultByteCount = 0;
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
                streamingThread.sleep(this.SLEEP_INTERVAL);
                
                state = 3;
                break;
                
              } else {
                
                // still in the middle of the result, keep adding bytes
                resultByteCount++; // add each byte found

                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                } else {
                  resultBuffer.compact();
                  logger.debug("Compacting resultBuffer ...");
                  resultBuffer.put(byteOne);
                  
                }
                
                break;
                
              }
            
            case 3: // acknowledge the connection
            
              // the ready status string should end in READY\r
              // note bytes are in reverse order in the FIFO window
              if ( byteOne   == 0x0D && byteTwo  == 0x59 && 
                   byteThree == 0x44 && byteFour == 0x41) {
                
                resultByteCount++; // add the last byte found to the count
                
                // add the last byte found to the result buffer
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                
                } else {
                  resultBuffer.compact();
                  resultBuffer.put(byteOne);
                
                }                
                
                // report the connect rate and ready status string
                resultArray = new byte[resultByteCount];
                resultBuffer.flip();
                resultBuffer.get(resultArray);
                resultString = new String(resultArray, "US-ASCII");
                
                // test the connect rate
                if ( resultString.indexOf(this.CONNECT_RATE) > 0 ) {
                  logger.debug("Connect Rate Result: " +
                               this.CONNECT_RATE);
                  
                  // test the ready status
                  if ( resultString.indexOf(this.READY_STATUS) > 0 ) {
                    logger.debug("Connect Rate Result: " +
                                 this.READY_STATUS);
                  
                    resultBuffer.clear();
                    resultByteCount = 0;
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
                    streamingThread.sleep(this.SLEEP_INTERVAL);
                    
                    // query the instrument id
                    this.command = this.ID_COMMAND +
                                   this.MODEM_COMMAND_SUFFIX;
                    this.sentCommand = queryInstrument(this.command);
                    
                    // allow time for the modem to respond
                    streamingThread.sleep(this.SLEEP_INTERVAL);

                    state = 4;
                    break;
                    
                  } else {
                    logger.debug("The ready status differs from: " +
                                 this.READY_STATUS);
                  
                    // throw an exception here?
                    break;
                  }
                  
                } else {
                  logger.debug("The connect rate differs from: " +
                               this.CONNECT_RATE);
                  
                  // throw an exception here?
                  break;
                }
                
              } else {
              
                // still in the middle of the result, keep adding bytes
                resultByteCount++; // add each byte found
              
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                } else {
                  resultBuffer.compact();
                  logger.debug("Compacting resultBuffer ...");
                  resultBuffer.put(byteOne);
              
                }
              
                break;
              
              }
              
            case 4: // get the instrument id
              
              // the instrument ID string should end in \r
              if ( byteOne == 0x0D ) {
                
                resultByteCount++; // add the last byte found to the count
                
                // add the last byte found to the result buffer
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                
                } else {
                  resultBuffer.compact();
                  resultBuffer.put(byteOne);
                
                }                
                
                // report the instrument ID string
                resultArray = new byte[resultByteCount];
                resultBuffer.flip();
                resultBuffer.get(resultArray);
                resultString = new String(resultArray, "US-ASCII");
                logger.debug("Seahorse Instrument ID: " + resultString.trim());
                
                // set the platformID variable
                this.platformID = resultString.substring(0, resultString.length() - 1);
                
                resultBuffer.clear();
                resultByteCount = 0;
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
                streamingThread.sleep(this.SLEEP_INTERVAL);
                
                state = 5;
                break;
                
              } else {
              
                // still in the middle of the result, keep adding bytes
                resultByteCount++; // add each byte found
              
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                } else {
                  resultBuffer.compact();
                  logger.debug("Compacting resultBuffer ...");
                  resultBuffer.put(byteOne);
              
                }
              
                break;
              
              }
              
            case 5: // get the seahorse battery voltage
              
              // the battery voltage string should end in \r
              if ( byteOne == 0x0D ) {
                
                resultByteCount++; // add the last byte found to the count
                
                // add the last byte found to the result buffer
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                
                } else {
                  resultBuffer.compact();
                  resultBuffer.put(byteOne);
                
                }                
                
                // report the battery voltage string
                resultArray = new byte[resultByteCount];
                resultBuffer.flip();
                resultBuffer.get(resultArray);
                resultString = new String(resultArray, "US-ASCII");
                logger.debug("Seahorse Battery Voltage: " + resultString.trim());
                
                resultBuffer.clear();
                resultByteCount = 0;
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
                streamingThread.sleep(this.SLEEP_INTERVAL);
                
                state = 6;
                break;
                
              } else {
              
                // still in the middle of the result, keep adding bytes
                resultByteCount++; // add each byte found
              
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                } else {
                  resultBuffer.compact();
                  logger.debug("Compacting resultBuffer ...");
                  resultBuffer.put(byteOne);
              
                }
              
                break;
              
              }
              
            case 6:
              
              // the GPRMC string should end in END\r
              // note bytes are in reverse order in the FIFO window
              if ( byteOne == 0x0D && byteTwo ==  0x44 && 
                   byteThree == 0x4E && byteFour == 0x45 ) {
                
                resultByteCount++; // add the last byte found to the count
                
                // add the last byte found to the result buffer
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                
                } else {
                  resultBuffer.compact();
                  resultBuffer.put(byteOne);
                
                }                
                
                // report the GPRMC string
                resultArray = new byte[resultByteCount];
                resultBuffer.flip();
                resultBuffer.get(resultArray);
                resultString = new String(resultArray, "US-ASCII");
                logger.debug("Seahorse GPRMC string: " + resultString.trim());
                
                resultBuffer.clear();
                resultByteCount = 0;
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
                streamingThread.sleep(this.SLEEP_INTERVAL);
                
                state = 7;
                break;
                
              } else {
              
                // still in the middle of the result, keep adding bytes
                resultByteCount++; // add each byte found
              
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                } else {
                  resultBuffer.compact();
                  logger.debug("Compacting resultBuffer ...");
                  resultBuffer.put(byteOne);
              
                }
              
                break;
              
              }
              
            case 7:
              
              // the file name string should end in .Z\r
              // note bytes are in reverse order in the FIFO window
              if ( byteOne == 0x0D && byteTwo == 0x5A && byteThree == 0x2E) {
                
                resultByteCount++; // add the last byte found to the count
                
                // add the last byte found to the result buffer
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                
                } else {
                  resultBuffer.compact();
                  resultBuffer.put(byteOne);
                
                }                
                
                // report the file name string
                resultArray = new byte[resultByteCount];
                resultBuffer.flip();
                resultBuffer.get(resultArray);
                resultString = new String(resultArray, "US-ASCII");
                logger.debug("File name result: " + resultString.trim());
                
                resultString = resultString.trim();
                int fileNameIndex = resultString.indexOf(this.FILENAME_PREFIX);
                
                //extract just the filename from the result (excise the "FILE=")
                this.fileNameToDownload = 
                  resultString.substring(
                    (fileNameIndex + (this.FILENAME_PREFIX).length() + 1), 
                    resultString.length());
                
                logger.debug("File name to download: " + this.fileNameToDownload);
                
                // test to see if the GFN command returns FILES=NONE
                if ( !(resultString.indexOf(this.END_OF_FILES) > 0) ) {
                  
                  // there is a file to download. parse the file name,
                  // get the number of blocks to transfer
                  this.command = this.NUMBER_OF_BLOCKS_COMMAND +
                                 this.MODEM_COMMAND_SUFFIX;
                  this.sentCommand = queryInstrument(this.command);

                  // allow time for the modem to respond
                  streamingThread.sleep(this.SLEEP_INTERVAL);
                  
                  resultBuffer.clear();
                  resultByteCount = 0;
                  resultArray = new byte[0];
                  resultString = "";
                  byteOne   = 0x00;
                  byteTwo   = 0x00;
                  byteThree = 0x00;
                  byteFour  = 0x00;
                  
                  state = 8;
                  break;
                  
                } else {
                  
                  // We have downloaded all files. Flush the channel map to the RBNB
                  rbnbChannelMap.PutTimeAuto("server");
                  getSource().Flush(rbnbChannelMap);

                  logger.info("Flushed data to the DataTurbine.");

                  // there are no more files to read. close the Tx session.
                  this.command = this.CLOSE_TRANSFER_SESSION_COMMAND +
                                 this.MODEM_COMMAND_SUFFIX;
                  this.sentCommand = queryInstrument(this.command);

                  // allow time for the modem to respond
                  streamingThread.sleep(this.SLEEP_INTERVAL);

                  // clean up
                  resultBuffer.clear();
                  resultByteCount = 0;
                  resultArray = new byte[0];
                  resultString = "";
                  byteOne   = 0x00;
                  byteTwo   = 0x00;
                  byteThree = 0x00;
                  byteFour  = 0x00;
                  rbnbChannelMap.Clear();                      
                  
                  state = 10;
                  break;
                  
                }
                
              } else {
              
                // still in the middle of the result, keep adding bytes
                resultByteCount++; // add each byte found
              
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                } else {
                  resultBuffer.compact();
                  logger.debug("Compacting resultBuffer ...");
                  resultBuffer.put(byteOne);
              
                }
              
                break;
              
              }
              
            case 8:
              
              // the number of blocks string should end in \r
              if ( byteOne == 0x0D ) {
                
                resultByteCount++; // add the last byte found to the count
                
                // add the last byte found to the result buffer
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                
                } else {
                  resultBuffer.compact();
                  resultBuffer.put(byteOne);
                
                }                
                
                // report the number of blocks string
                resultArray = new byte[resultByteCount];
                resultBuffer.flip();
                resultBuffer.get(resultArray);
                resultString = new String(resultArray, "US-ASCII");
                logger.debug("Number of bytes reported: " + resultString.trim());
                
                int lineEndIndex = resultString.indexOf(this.MODEM_COMMAND_SUFFIX);
                int numberOfBlocksIndex = resultString.indexOf(this.FILENAME_PREFIX);
                
                logger.debug("Number of bytes to download: " + this.numberOfBlocks);
                
                // convert the string to an integer
                try {
                  this.numberOfBlocks = new Integer(resultString.trim()).intValue();
                
                } catch ( java.lang.NumberFormatException nfe ) {
                  failed = true;
                  nfe.printStackTrace();
                  logger.debug("Failed to convert returned string value " + 
                  "to an integer value.  The returned string is: " + this.numberOfBlocks);
                      
                }
                                
                // test to see if the GNB command returns DONE\r
                if ( !(resultString.indexOf(this.TRANSFER_COMPLETE) > 0) ) {
                  
                  // there are bytes to transfer. send the transfer command
                  
                  this.command = this.TRANSFER_BLOCKS_COMMAND +
                                 this.MODEM_COMMAND_SUFFIX;
                  this.sentCommand = queryInstrument(this.command);

                  // allow time for the modem to respond
                  streamingThread.sleep(this.SLEEP_INTERVAL);

                  //resultBuffer.clear(); dont clear the buffer
                  resultByteCount = 0;
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
                  String fileString = new String(output);
                  
                  //report the file contents to the log
                  logger.debug("File " + this.fileNameToDownload + ": ");                   
                  logger.debug(fileString);                   
                  
                  // Parse the data file, not the cast file.
                  try {
                    if ( this.fileNameToDownload.indexOf(DATA_FILE_PREFIX) > 0 ) {
                      this.ctdParser = new CTDParser(fileString);
                      
                      // TODO: insert the individual data channels into the map.
                      
                    } 

                  } catch ( Exception e ) {
                    logger.debug("Failed to parse the CTD data file: " + 
                                  e.getMessage());
                                  
                  }
                  
                  // Insert the file into the channel map. 
                  if ( fileNameToDownload.indexOf(DATA_FILE_PREFIX) > 0) {
                    channelIndex = rbnbChannelMap.Add(this.rbnbChannelName);
                    rbnbChannelMap.PutMime(channelIndex, "text/plain");
                    rbnbChannelMap.PutDataAsString(channelIndex, fileString);
                      
                  } else if ( fileNameToDownload.indexOf(CAST_FILE_PREFIX) > 0 ) {
                    channelIndex = rbnbChannelMap.Add("ASCIICastData");
                    rbnbChannelMap.PutMime(channelIndex, "text/plain");
                    rbnbChannelMap.PutDataAsString(channelIndex, fileString);
                      
                  }
                  
                  // Ask for the next file.
                  this.command = this.FILENAME_COMMAND +
                                 this.MODEM_COMMAND_SUFFIX;
                  this.sentCommand = queryInstrument(this.command);

                  // allow time for the modem to respond
                  streamingThread.sleep(this.SLEEP_INTERVAL);

                  //resultBuffer.clear(); dont clear the buffer
                  resultByteCount = 0;
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
                resultByteCount++; // add each byte found
              
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                } else {
                  resultBuffer.compact();
                  logger.debug("Compacting resultBuffer ...");
                  resultBuffer.put(byteOne);
              
                }
              
                break;
              
              }
          
            case 9:
            
              // transfer up to the reported number of bytes
              if ( resultByteCount == this.numberOfBlocks ) {
              
                // we have downloaded the reported bytes. get the next section.
                // get the number of blocks to transfer
                this.command = this.NUMBER_OF_BLOCKS_COMMAND +
                               this.MODEM_COMMAND_SUFFIX;
                this.sentCommand = queryInstrument(this.command);

                // allow time for the modem to respond
                streamingThread.sleep(this.SLEEP_INTERVAL);
                
                //resultBuffer.clear();
                resultByteCount = 0;
                byteOne   = 0x00;
                byteTwo   = 0x00;
                byteThree = 0x00;
                byteFour  = 0x00;
                
                state = 8;
                break;
                 
              
              } else {
              
                // still in the middle of the result, keep adding bytes
                resultByteCount++; // add each byte found
              
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                } else {
                  resultBuffer.compact();
                  logger.debug("Compacting resultBuffer ...");
                  resultBuffer.put(byteOne);
              
                }
              
                break;
              
              }
            
            case 10:
              
              // the response from the modem should end in BYE\r
              // note bytes are in reverse order in the FIFO window
              if ( byteOne == 0x0D && byteTwo == 0x45 && 
                   byteThree == 0x59 && byteFour == 0x42 ) {
                
                // continue to disconnect. send the escape sequence
                this.command = this.ESCAPE_SEQUENCE_COMMAND +
                               this.MODEM_COMMAND_SUFFIX;
                this.sentCommand = queryInstrument(this.command);

                // allow time for the modem to respond
                streamingThread.sleep(this.SLEEP_INTERVAL);

                resultBuffer.clear();
                resultByteCount = 0;
                byteOne   = 0x00;
                byteTwo   = 0x00;
                byteThree = 0x00;
                byteFour  = 0x00;
                
                state = 11;
                break; 
                
              } else {
              
                // still in the middle of the result, keep adding bytes
                resultByteCount++; // add each byte found
              
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                } else {
                  resultBuffer.compact();
                  logger.debug("Compacting resultBuffer ...");
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
                streamingThread.sleep(this.SLEEP_INTERVAL);

                resultBuffer.clear();
                resultByteCount = 0;
                byteOne   = 0x00;
                byteTwo   = 0x00;
                byteThree = 0x00;
                byteFour  = 0x00;
                
                state = 12;
                break; 
                
              } else {
              
                // still in the middle of the result, keep adding bytes
                resultByteCount++; // add each byte found
              
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                } else {
                  resultBuffer.compact();
                  logger.debug("Compacting resultBuffer ...");
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
                streamingThread.sleep(this.SLEEP_INTERVAL);

                resultBuffer.clear();
                resultByteCount = 0;
                byteOne   = 0x00;
                byteTwo   = 0x00;
                byteThree = 0x00;
                byteFour  = 0x00;
                
                state = 0;
                break; 
                
              } else {
              
                // still in the middle of the result, keep adding bytes
                resultByteCount++; // add each byte found
              
                if ( resultBuffer.remaining() > 0 ) {
                  resultBuffer.put(byteOne);
                } else {
                  resultBuffer.compact();
                  logger.debug("Compacting resultBuffer ...");
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
        
    } catch ( IOException e ) {
      // handle exceptions
      // In the event of an i/o exception, log the exception, and allow execute()
      // to return false, which will prompt a retry.
      failed = true;
      e.printStackTrace();
      return !failed;
    } catch ( SAPIException sapie ) {
      // In the event of an RBNB communication  exception, log the exception, 
      // and allow execute() to return false, which will prompt a retry.
      failed = true;
      sapie.printStackTrace();
      return !failed;
    } catch ( java.lang.InterruptedException ine) {
      failed = true;
      ine.printStackTrace();
      return !failed;
    } catch ( java.util.zip.DataFormatException dfe ) {
      failed = true;
      dfe.printStackTrace();
      return !failed;
    }
    
    return !failed;
  } // end if (  !isConnected() ) 
  
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

  /**
   * A method that queries the instrument with a command
   */
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
        logger.debug("Wrote " + command + " to the socket channel.");
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
    "$LastChangedDate: 2009-07-14 06:25:25 -0600 (Tue, 14 Jul 2009) $" +
    "$LastChangedBy: cjones $" +
    "$LastChangedRevision: 454 $" +
    "$HeadURL: https://bbl.ancl.hawaii.edu/projects/bbl/trunk/src/java/edu/hawaii/soest/kilonalu/ctd/SeahorseSource.java $"
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
    
    logger.info("SeahorseSource.main() called.");
    
    try {
      // create a new instance of the SeahorseSource object, and parse the command 
      // line arguments as settings for this instance
      final SeahorseSource seahorseSource = new SeahorseSource();
      
      // Set up a simple logger that logs to the console
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

}
