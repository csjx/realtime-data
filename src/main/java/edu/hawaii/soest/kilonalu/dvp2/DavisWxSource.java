/*
 *  Copyright: 2020 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: To convert a Davis Scientific Vantage Pro 2 ASCII data source into 
 *             RBNB Data Turbine frames for archival and realtime access.

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
package edu.hawaii.soest.kilonalu.dvp2;

import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.SAPIException;

import java.lang.StringBuilder;
import java.lang.InterruptedException;
import java.io.IOException;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import java.text.SimpleDateFormat;

import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;

import org.apache.commons.codec.binary.Hex;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.log4j.PropertyConfigurator;

import org.nees.rbnb.RBNBSource;

/**
 * A simple class used to harvest a decimal ASCII data stream from a Davis
 * Scientif Vantage Pro 2 weather station over a TCP socket connection  to a 
 * serial2ip converter host. The data stream is then converted into RBNB frames 
 * and pushed into the RBNB DataTurbine real time server.  This class extends 
 * org.nees.rbnb.RBNBSource, which in turn extends org.nees.rbnb.RBNBBase, 
 * and therefore follows the API conventions found in the org.nees.rbnb code.  
 *
 * The parsing of the data stream relies on the premise that each sample of data
 * is a comma delimited string of values, and that each sample is terminated
 * by a newline character (\n) followed by a two character prompt (S>).  
 *
 */
public class DavisWxSource extends RBNBSource {

  /**
   * The default log configuration file location
   */
  private final String DEFAULT_LOG_CONFIGURATION_FILE = "lib/log4j2.properties";

  /**
   * The log configuration file location
   */
  private String logConfigurationFile = DEFAULT_LOG_CONFIGURATION_FILE;
  
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
  private String DEFAULT_RBNB_CHANNEL = "BinarySampleData";

  /**
   * The name of the RBNB channel for this data stream
   */
  private String rbnbChannelName = DEFAULT_RBNB_CHANNEL;
  
  /*
   *  A default source IP address for the given source instrument
   */
  private final String DEFAULT_SOURCE_HOST_NAME = "localhost";  

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
  
  /**
   *The command prefix used to send commands to the microcontroller
   */ 
  private String commandPrefix = "";

  /**
   *The command suffix used to send commands to the microcontroller
   */ 
  private String commandSuffix = "\n";

  /**
   *The command used to get the ID from the instrument
   */ 
  private String idCommand = "";

  /**
   *The command used to have the instrument take a sample
   */ 
  private String takeSampleCommand = "LOOP 1";
  
  /**
   *The command sent to the instrument
   */ 
  private String command;
  
  /**
   * The number of bytes in the ensemble as each byte is read from the stream
   */
  private int sampleByteCount = 0;
  
  /**
   * The Logger instance used to log system messages 
   */
  private static Log log = LogFactory.getLog(DavisWxSource.class);

  /*
   * An integer value indicating the execution state.  This is used by the 
   * execute() method while parsing the stream of bytes from the instrument
   */
  protected int state = 0;
  
  /*
   * A boolean field indicating if the instrument connection is ready to stream
   */
  private boolean readyToStream = false;

  /*
   * A boolean field indicating if a command has been sent to the instrument
   */
  private boolean sentCommand = false;
  
  /*
   * The thread that is run for streaming data from the instrument
   */
  private Thread streamingThread;
  
  /*
   * The socket channel used to establish TCP communication with the instrument
   */
  private SocketChannel socketChannel;
  /*
   * An internal Thread setting used to specify how long, in milliseconds, the
   * execution of the data streaming Thread should wait before re-executing
   * 
   * @see execute()
   */
  private final int RETRY_INTERVAL = 5000;
  
  /** 
   * The date format for the timestamp applied to the LOOP sample 04 Aug 2008 09:15:01
   */
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
  
  /**
   * The timezone used for the sample date
   */
  private static final TimeZone TZ = TimeZone.getTimeZone("Pacific/Honolulu");
  
  /*
   * The instance of the DavisWxParser object used to parse the binary LOOP
   * data packet and retrieve each of the data fields
   */
   private DavisWxParser davisWxParser = null;
   
  /**
   * Constructor - create an empty instance of the DavisWxSource object, using
   * default values for the RBNB server name and port, source instrument name
   * and port, archive mode, archive frame size, and cache frame size. 
   */
  public DavisWxSource() {
  }

  /**
   * Constructor - create an instance of the DavisWxSource object, using the
   * argument values for the source instrument name and port, and the RBNB 
   * server name and port.  This constructor will use default values for the
   * archive mode, archive frame size, and cache frame size. 
   *
   * @param sourceHostName  the name or IP address of the source instrument
   * @param sourceHostPort  the TCP port of the source host instrument
   * @param serverName      the name or IP address of the RBNB server connection
   * @param serverPort      the TCP port of the RBNB server
   */
  public DavisWxSource(String sourceHostName, String sourceHostPort, 
                       String serverName, String serverPort) {
    
    setHostName(sourceHostName);
    setHostPort(Integer.parseInt(sourceHostPort));
    setServerName(serverName);
    setServerPort(Integer.parseInt(serverPort));
  }

  /**
   * Constructor - create an instance of the DavisWxSource object, using the
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
  public DavisWxSource(String sourceHostName, String sourceHostPort, 
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
    log.debug("DavisWxSource.execute() called.");
    // do not execute the stream if there is no connection
    if (  !isConnected() ) return false;
    
      boolean failed = false;
    
    // while data are being sent, read them into the buffer
    try {

      this.socketChannel = getSocketConnection();
      
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
      
      // create a byte buffer to store bytes from the TCP stream
      ByteBuffer buffer = ByteBuffer.allocateDirect(getBufferSize());
      
      // add a channel of data that will be pushed to the server.  
      // Each sample will be sent to the Data Turbine as an rbnb frame.
      ChannelMap rbnbChannelMap = new ChannelMap();
      int channelIndex = 0;
      
      // add the raw binary LOOP packet data
      //channelIndex = rbnbChannelMap.Add(getRBNBChannelName());
      //rbnbChannelMap.PutUserInfo(channelIndex, "units=none");
      
      // add the barTrendAsString field data
      channelIndex = rbnbChannelMap.Add("barTrendAsString");                // Falling Slowly
      rbnbChannelMap.PutUserInfo(channelIndex, "units=none");
      
      // add the barometer field data
      channelIndex = rbnbChannelMap.Add("barometer");                      // 29.9
      rbnbChannelMap.PutUserInfo(channelIndex, "units=inch Hg");
      
      // add the insideTemperature field data
      channelIndex = rbnbChannelMap.Add("insideTemperature");               // 83.9
      rbnbChannelMap.PutUserInfo(channelIndex, "units=degrees F");
      
      // add the insideHumidity field data
      channelIndex = rbnbChannelMap.Add("insideHumidity");                  // 51
      rbnbChannelMap.PutUserInfo(channelIndex, "units=percent");
      
      // add the outsideTemperature field data
      channelIndex = rbnbChannelMap.Add("outsideTemperature");              // 76.7
      rbnbChannelMap.PutUserInfo(channelIndex, "units=degrees F");
      
      // add the windSpeed field data
      channelIndex = rbnbChannelMap.Add("windSpeed");                       // 5
      rbnbChannelMap.PutUserInfo(channelIndex, "units=mph");
      
      // add the tenMinuteAverageWindSpeed field data
      channelIndex = rbnbChannelMap.Add("tenMinuteAverageWindSpeed");      // 4
      rbnbChannelMap.PutUserInfo(channelIndex, "units=mph");
      
      // add the windDirection field data
      channelIndex = rbnbChannelMap.Add("windDirection");                   // 80
      rbnbChannelMap.PutUserInfo(channelIndex, "units=degrees");
      
      // add the outsideHumidity field data
      channelIndex = rbnbChannelMap.Add("outsideHumidity");                 // 73
      rbnbChannelMap.PutUserInfo(channelIndex, "units=percent");
      
      // add the rainRate field data
      channelIndex = rbnbChannelMap.Add("rainRate");                        // 0.0
      rbnbChannelMap.PutUserInfo(channelIndex, "units=inch/hour");
      
      // add the uvRadiation field data
      channelIndex = rbnbChannelMap.Add("uvRadiation");                     // 0
     rbnbChannelMap.PutUserInfo(channelIndex, "UV index");
      
      // add the solarRadiation field data
      channelIndex = rbnbChannelMap.Add("solarRadiation");                  // 0.0
      rbnbChannelMap.PutUserInfo(channelIndex, "watt/m^2");
      
      // add the stormRain field data
      channelIndex = rbnbChannelMap.Add("stormRain");                       // 0.0
      rbnbChannelMap.PutUserInfo(channelIndex, "inch");
      
      // add the currentStormStartDate field data
      channelIndex = rbnbChannelMap.Add("currentStormStartDate");           // -1--1-1999
      rbnbChannelMap.PutUserInfo(channelIndex, "units=none");
      
      // add the dailyRain field data
      channelIndex = rbnbChannelMap.Add("dailyRain");                       // 0.0
      rbnbChannelMap.PutUserInfo(channelIndex, "units=inch");
      
      // add the monthlyRain field data
      channelIndex = rbnbChannelMap.Add("monthlyRain");                     // 0.0
      rbnbChannelMap.PutUserInfo(channelIndex, "units=inch");
      
      // add the yearlyRain field data
      channelIndex = rbnbChannelMap.Add("yearlyRain");                      // 15.0
      rbnbChannelMap.PutUserInfo(channelIndex, "units=inch");
      
      // add the dailyEvapoTranspiration field data
      channelIndex = rbnbChannelMap.Add("dailyEvapoTranspiration");         // 0.0
      rbnbChannelMap.PutUserInfo(channelIndex, "units=inch");
      
      // add the monthlyEvapoTranspiration field data
      channelIndex = rbnbChannelMap.Add("monthlyEvapoTranspiration");       // 0.0
      rbnbChannelMap.PutUserInfo(channelIndex, "units=inch");
      
      // add the yearlyEvapoTranspiration field data
      channelIndex = rbnbChannelMap.Add("yearlyEvapoTranspiration");        // 93.0
      rbnbChannelMap.PutUserInfo(channelIndex, "units=inch");
      
      // add the transmitterBatteryStatus field data
      channelIndex = rbnbChannelMap.Add("transmitterBatteryStatus");        // 0
      rbnbChannelMap.PutUserInfo(channelIndex, "units=none");
      
      // add the consoleBatteryVoltage field data
      channelIndex = rbnbChannelMap.Add("consoleBatteryVoltage");           // 4.681640625
      rbnbChannelMap.PutUserInfo(channelIndex, "units=volts");
      
      // add the forecastAsString field data
      channelIndex = rbnbChannelMap.Add("forecastAsString");                // Partially Cloudy
      rbnbChannelMap.PutUserInfo(channelIndex, "units=none");
      
      // add the forecastRuleNumberAsString field data
      //channelIndex = rbnbChannelMap.Add("forecastRuleNumberAsString");      // Increasing clouds with little temperature change.
      //rbnbChannelMap.PutUserInfo(channelIndex, "units=none");
      
      // add the timeOfSunrise field data
      channelIndex = rbnbChannelMap.Add("timeOfSunrise");                   // 05:49
      rbnbChannelMap.PutUserInfo(channelIndex, "units=none");
      
      // add the timeOfSunset field data
      channelIndex = rbnbChannelMap.Add("timeOfSunset");                    // 19:11
      rbnbChannelMap.PutUserInfo(channelIndex, "units=none");               
      
      channelIndex = rbnbChannelMap.Add("DecimalASCIISampleData");          // sample data as ASCII
      rbnbChannelMap.PutUserInfo(channelIndex, "units=none");               
      
      // register the channel map of variables and units with the DataTurbine
      getSource().Register(rbnbChannelMap);
      // reset variables for use with the incoming data
      rbnbChannelMap.Clear();
      channelIndex = 0;

    // wake the instrument with an initial '\n' command
    this.command = this.commandSuffix;
    this.sentCommand = queryInstrument(this.command);

    // allow time for the instrument response
    streamingThread.sleep(2000);
    this.command = this.commandPrefix + 
                   this.takeSampleCommand +
                   this.commandSuffix;
    this.sentCommand = queryInstrument(command);        
            
      // while there are bytes to read from the socket ...
      while ( this.socketChannel.read(buffer) != -1 || buffer.position() > 0) {
        // prepare the buffer for reading
        buffer.flip();          
    
        // while there are unread bytes in the ByteBuffer
        while ( buffer.hasRemaining() ) {
          byteOne = buffer.get();
          //log.debug("b1: " + new String(Hex.encodeHex((new byte[]{byteOne})))   + "\t" + 
          //             "b2: " + new String(Hex.encodeHex((new byte[]{byteTwo})))   + "\t" + 
          //             "b3: " + new String(Hex.encodeHex((new byte[]{byteThree}))) + "\t" + 
          //             "b4: " + new String(Hex.encodeHex((new byte[]{byteFour})))  + "\t" +
          //             "sample pos: "   + sampleBuffer.position()                  + "\t" +
          //             "sample rem: "   + sampleBuffer.remaining()                 + "\t" +
          //             "sample cnt: "   + sampleByteCount                          + "\t" +
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
              
              // sample line is begun by "ACK L" (the first part of ACK + "LOOP")
              // note bytes are in reverse order in the FIFO window
              if ( byteOne == 0x4C && byteTwo == 0x06 ) {
                
                sampleByteCount++; // add the last byte found to the count
                
                // add the last byte found to the sample buffer
                if ( sampleBuffer.remaining() > 0 ) {
                  sampleBuffer.put(byteOne);
                
                } else {
                  sampleBuffer.compact();
                  sampleBuffer.put(byteOne);
                  
                }
                
                // we've found the beginning of a sample, move on
                state = 1;
                break;
                
              } else {
                break;                
              }
            
            case 1: // read the rest of the bytes to the next EOL characters
              
              // sample line is terminated by "\n\r"
              // note bytes are in reverse order in the FIFO window
              if ( byteOne == 0x0D && byteTwo == 0x0A ) {
                
                sampleByteCount++; // add the last byte found to the count
                
                // add the last byte found to the sample buffer
                if ( sampleBuffer.remaining() > 0 ) {
                  sampleBuffer.put(byteOne);
                
                } else {
                  sampleBuffer.compact();
                  sampleBuffer.put(byteOne);
                  
                }
                state = 3;
                break;
              } else { // not 0x0A0D

                // still in the middle of the sample, keep adding bytes
                sampleByteCount++; // add each byte found

                if ( sampleBuffer.remaining() > 0 ) {
                  sampleBuffer.put(byteOne);
                } else {
                  sampleBuffer.compact();
                  log.debug("Compacting sampleBuffer ...");
                  sampleBuffer.put(byteOne);
                  
                }
                
                break;
              } // end if for 0x0A0D EOL
          
                
            case 3:
               
               // At this point, we've found the \n\r delimiter, read the first
               // of 2 CRC bytes
               sampleByteCount++; // add the last byte found to the count
               
               // add the last byte found to the sample buffer
               if ( sampleBuffer.remaining() > 0 ) {
                 sampleBuffer.put(byteOne);
               
               } else {
                 sampleBuffer.compact();
                 sampleBuffer.put(byteOne);
                 
               }
               state = 4;
               break;
                
            case 4:
               
               // At this point, we've found the \n\r delimiter, read the second
               // of 2 CRC bytes
               sampleByteCount++; // add the last byte found to the count
               
               // add the last byte found to the sample buffer
               if ( sampleBuffer.remaining() > 0 ) {
                 sampleBuffer.put(byteOne);
               
               } else {
                 sampleBuffer.compact();
                 sampleBuffer.put(byteOne);
               
               }
               state = 0;
               
               // extract just the length of the sample bytes out of the
               // sample buffer, and place it in the channel map as a 
               // byte array.  Then, send it to the data turbine.
               byte[] sampleArray = new byte[sampleByteCount];
               
               try {
                 sampleBuffer.flip();
                 sampleBuffer.get(sampleArray);
                 
                 // parse and send the sample to the data turbine
                 this.davisWxParser = new DavisWxParser(sampleBuffer);
                 
               } catch (java.lang.Exception e) {
                 log.info("There was a problem parsing the binary weather LOOP packet. Skipping this sample.");
                 byteOne   = 0x00;
                 byteTwo   = 0x00;
                 byteThree = 0x00;
                 byteFour  = 0x00;
                 sampleBuffer.clear();
                 sampleByteCount = 0;
                 rbnbChannelMap.Clear();                      
                 break;
               }
               
               // create a character string to store characters from the TCP stream
               StringBuilder decimalASCIISampleData = new StringBuilder();
               
               rbnbChannelMap.PutTimeAuto("server");
               
               // add the raw binary LOOP packet data
               //channelIndex = rbnbChannelMap.Add(getRBNBChannelName());
               //rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");
               //rbnbChannelMap.PutDataAsByteArray(channelIndex, sampleArray);         // raw binary LOOP packet
               
               // add the barTrendAsString field data
               channelIndex = rbnbChannelMap.Add("barTrendAsString");                // Falling Slowly
               rbnbChannelMap.PutMime(channelIndex, "text/plain");
               rbnbChannelMap.PutDataAsString(channelIndex, davisWxParser.getBarTrendAsString());
               decimalASCIISampleData.append(String.format("\"%16s\"", davisWxParser.getBarTrendAsString())).append(", ");
               
               // add the packetType field to the ASCII string only
               decimalASCIISampleData.append(String.format("%1d", davisWxParser.getPacketType())).append(", ");
               
               // add the nextRecord field to the ASCII string only
               decimalASCIISampleData.append(String.format("%04d", davisWxParser.getNextRecord())).append(", ");

               // add the barometer field data
               channelIndex = rbnbChannelMap.Add("barometer");                      // 29.9
               rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");
               rbnbChannelMap.PutDataAsFloat32(channelIndex, new float[]{davisWxParser.getBarometer()});
               decimalASCIISampleData.append(String.format("%06.4f", davisWxParser.getBarometer())).append(", ");
               
               // add the insideTemperature field data
               channelIndex = rbnbChannelMap.Add("insideTemperature");               // 83.9
               rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");
               rbnbChannelMap.PutDataAsFloat32(channelIndex, new float[]{davisWxParser.getInsideTemperature()});
               decimalASCIISampleData.append(String.format("%05.2f", davisWxParser.getInsideTemperature())).append(", ");
               
               // add the insideHumidity field data
               channelIndex = rbnbChannelMap.Add("insideHumidity");                  // 51
               rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");
               rbnbChannelMap.PutDataAsInt32(channelIndex, new int[]{davisWxParser.getInsideHumidity()});
               decimalASCIISampleData.append(String.format("%03d", davisWxParser.getInsideHumidity())).append(", ");
               
               // add the outsideTemperature field data
               channelIndex = rbnbChannelMap.Add("outsideTemperature");              // 76.7
               rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");
               rbnbChannelMap.PutDataAsFloat32(channelIndex, new float[]{davisWxParser.getOutsideTemperature()});
               decimalASCIISampleData.append(String.format("%05.2f", davisWxParser.getOutsideTemperature())).append(", ");
               
               // add the windSpeed field data
               channelIndex = rbnbChannelMap.Add("windSpeed");                       // 5
               rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");
               rbnbChannelMap.PutDataAsInt32(channelIndex, new int[]{davisWxParser.getWindSpeed()});
               decimalASCIISampleData.append(String.format("%03d", davisWxParser.getWindSpeed())).append(", ");
               
               // add the tenMinuteAverageWindSpeed field data
               channelIndex = rbnbChannelMap.Add("tenMinuteAverageWindSpeed");      // 4
               rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");
               rbnbChannelMap.PutDataAsInt32(channelIndex, new int[]{davisWxParser.getTenMinuteAverageWindSpeed()});
               decimalASCIISampleData.append(String.format("%03d", davisWxParser.getTenMinuteAverageWindSpeed())).append(", ");
               
               // add the windDirection field data
               channelIndex = rbnbChannelMap.Add("windDirection");                   // 80
               rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");
               rbnbChannelMap.PutDataAsInt32(channelIndex, new int[]{davisWxParser.getWindDirection()});
               decimalASCIISampleData.append(String.format("%03d", davisWxParser.getWindDirection())).append(", ");
               
               
               // add the extraTemperature fields as ASCII only
               float[] extraTemperatures = davisWxParser.getExtraTemperatures();
               for (float temperature : extraTemperatures) {
                 decimalASCIISampleData.append(String.format("%05.2f", temperature)).append(", ");
                 
               }

               // add the soilTemperature fields as ASCII only
               float[] soilTemperatures = davisWxParser.getSoilTemperatures();
               for (float soil : soilTemperatures) {
                 decimalASCIISampleData.append(String.format("%05.2f", soil)).append(", ");
                 
               }
               
               // add the leafTemperature fields as ASCII only
               float[] leafTemperatures = davisWxParser.getLeafTemperatures();
               for (float leaf : leafTemperatures) {
                 decimalASCIISampleData.append(String.format("%05.2f", leaf)).append(", ");
                 
               }
               
               // add the outsideHumidity field data
               channelIndex = rbnbChannelMap.Add("outsideHumidity");                 // 73
               rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");
               rbnbChannelMap.PutDataAsInt32(channelIndex, new int[]{davisWxParser.getOutsideHumidity()});
               decimalASCIISampleData.append(String.format("%03d", davisWxParser.getOutsideHumidity())).append(", ");
               
               // add the rainRate field data
               channelIndex = rbnbChannelMap.Add("rainRate");                        // 0.0
               rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");
               rbnbChannelMap.PutDataAsFloat32(channelIndex, new float[]{davisWxParser.getRainRate()});
               decimalASCIISampleData.append(String.format("%04.2f", davisWxParser.getRainRate())).append(", ");
               
               // add the uvRadiation field data
               channelIndex = rbnbChannelMap.Add("uvRadiation");                     // 0
               rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");
               rbnbChannelMap.PutDataAsInt32(channelIndex, new int[]{davisWxParser.getUvRadiation()});
               decimalASCIISampleData.append(String.format("%03d", davisWxParser.getUvRadiation())).append(", ");
               
               // add the solarRadiation field data
               channelIndex = rbnbChannelMap.Add("solarRadiation");                  // 0.0
               rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");
               rbnbChannelMap.PutDataAsFloat32(channelIndex, new float[]{davisWxParser.getSolarRadiation()});
               decimalASCIISampleData.append(String.format("%04.1f", davisWxParser.getSolarRadiation())).append(", ");
               
               // add the stormRain field data
               channelIndex = rbnbChannelMap.Add("stormRain");                       // 0.0
               rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");
               rbnbChannelMap.PutDataAsFloat32(channelIndex, new float[]{davisWxParser.getStormRain()});
               decimalASCIISampleData.append(String.format("%04.2f", davisWxParser.getStormRain())).append(", ");
               
               // add the currentStormStartDate field data
               channelIndex = rbnbChannelMap.Add("currentStormStartDate");           // -1--1-1999
               rbnbChannelMap.PutMime(channelIndex, "text/plain");
               rbnbChannelMap.PutDataAsString(channelIndex, davisWxParser.getCurrentStormStartDate());
               decimalASCIISampleData.append(String.format("%10s", davisWxParser.getCurrentStormStartDate())).append(", ");
               
               // add the dailyRain field data
               channelIndex = rbnbChannelMap.Add("dailyRain");                       // 0.0
               rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");
               rbnbChannelMap.PutDataAsFloat32(channelIndex, new float[]{davisWxParser.getDailyRain()});
               decimalASCIISampleData.append(String.format("%04.2f", davisWxParser.getDailyRain())).append(", ");
               
               // add the monthlyRain field data
               channelIndex = rbnbChannelMap.Add("monthlyRain");                     // 0.0
               rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");
               rbnbChannelMap.PutDataAsFloat32(channelIndex, new float[]{davisWxParser.getMonthlyRain()});
               decimalASCIISampleData.append(String.format("%04.2f", davisWxParser.getMonthlyRain())).append(", ");
                              
               // add the yearlyRain field data
               channelIndex = rbnbChannelMap.Add("yearlyRain");                      // 15.0
               rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");
               rbnbChannelMap.PutDataAsFloat32(channelIndex, new float[]{davisWxParser.getYearlyRain()});
               decimalASCIISampleData.append(String.format("%04.2f", davisWxParser.getYearlyRain())).append(", ");
                              
               // add the dailyEvapoTranspiration field data
               channelIndex = rbnbChannelMap.Add("dailyEvapoTranspiration");         // 0.0
               rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");
               rbnbChannelMap.PutDataAsFloat32(channelIndex, new float[]{davisWxParser.getDailyEvapoTranspiration()});
               decimalASCIISampleData.append(String.format("%04.2f", davisWxParser.getDailyEvapoTranspiration())).append(", ");
                              
               // add the monthlyEvapoTranspiration field data
               channelIndex = rbnbChannelMap.Add("monthlyEvapoTranspiration");       // 0.0
               rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");
               rbnbChannelMap.PutDataAsFloat32(channelIndex, new float[]{davisWxParser.getMonthlyEvapoTranspiration()});
               decimalASCIISampleData.append(String.format("%04.2f", davisWxParser.getMonthlyEvapoTranspiration())).append(", ");
                              
               // add the yearlyEvapoTranspiration field data
               channelIndex = rbnbChannelMap.Add("yearlyEvapoTranspiration");        // 93.0
               rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");
               rbnbChannelMap.PutDataAsFloat32(channelIndex, new float[]{davisWxParser.getYearlyEvapoTranspiration()});
               decimalASCIISampleData.append(String.format("%04.2f", davisWxParser.getYearlyEvapoTranspiration())).append(", ");
               
               // add the consoleBatteryVoltage field data
               channelIndex = rbnbChannelMap.Add("consoleBatteryVoltage");           // 4.681640625
               rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");
               rbnbChannelMap.PutDataAsFloat32(channelIndex, new float[]{davisWxParser.getConsoleBatteryVoltage()});
               decimalASCIISampleData.append(String.format("%04.2f", davisWxParser.getConsoleBatteryVoltage())).append(", ");
               
               // add the forecastAsString field data
               channelIndex = rbnbChannelMap.Add("forecastAsString");                // Partially Cloudy
               rbnbChannelMap.PutMime(channelIndex, "text/plain");
               rbnbChannelMap.PutDataAsString(channelIndex, davisWxParser.getForecastAsString());
               decimalASCIISampleData.append(String.format("\"%47s\"", davisWxParser.getForecastAsString())).append(", ");
                              
               // add the forecastRuleNumberAsString field data as ASCII only
               decimalASCIISampleData.append(String.format("\"%167s\"", davisWxParser.getForecastRuleNumberAsString())).append(", ");
               
               // add the timeOfSunrise field data
               channelIndex = rbnbChannelMap.Add("timeOfSunrise");                   // 05:49
               rbnbChannelMap.PutMime(channelIndex, "text/plain");
               rbnbChannelMap.PutDataAsString(channelIndex, davisWxParser.getTimeOfSunrise());
               decimalASCIISampleData.append(String.format("%5s", davisWxParser.getTimeOfSunrise())).append(", ");
                              
               // add the timeOfSunset field data
               channelIndex = rbnbChannelMap.Add("timeOfSunset");                    // 19:11
               rbnbChannelMap.PutMime(channelIndex, "text/plain");
               rbnbChannelMap.PutDataAsString(channelIndex, davisWxParser.getTimeOfSunset());
               decimalASCIISampleData.append(String.format("%5s", davisWxParser.getTimeOfSunset())).append(", ");
               
               // then add a timestamp to the end of the sample
               DATE_FORMAT.setTimeZone(TZ);
               String sampleDateAsString = DATE_FORMAT.format(new Date()).toString();
               decimalASCIISampleData.append(sampleDateAsString);
               decimalASCIISampleData.append("\n");
               
               // add the ASCII CSV string of selected fields as a channel
               channelIndex = rbnbChannelMap.Add(getRBNBChannelName());                    // 19:11
               rbnbChannelMap.PutMime(channelIndex, "text/plain");
               rbnbChannelMap.PutDataAsString(channelIndex, decimalASCIISampleData.toString());
               
               // finally, send the channel map of data to the DataTurbine
               getSource().Flush(rbnbChannelMap);
               String sampleString = new String(Hex.encodeHex(sampleArray));
               log.info("Sample: " + sampleString);
               log.debug("barTrendAsString:               " + davisWxParser.getBarTrendAsString());
               log.debug("barometer:                      " + davisWxParser.getBarometer());
               log.debug("insideTemperature:              " + davisWxParser.getInsideTemperature());
               log.debug("insideHumidity:                 " + davisWxParser.getInsideHumidity());
               log.debug("outsideTemperature:             " + davisWxParser.getOutsideTemperature());
               log.debug("windSpeed:                      " + davisWxParser.getWindSpeed());
               log.debug("tenMinuteAverageWindSpeed:      " + davisWxParser.getTenMinuteAverageWindSpeed());
               log.debug("windDirection:                  " + davisWxParser.getWindDirection());
               log.debug("outsideHumidity:                " + davisWxParser.getOutsideHumidity());
               log.debug("rainRate:                       " + davisWxParser.getRainRate());
               log.debug("uvRadiation:                    " + davisWxParser.getUvRadiation());
               log.debug("solarRadiation:                 " + davisWxParser.getSolarRadiation());
               log.debug("stormRain:                      " + davisWxParser.getStormRain());
               log.debug("currentStormStartDate:          " + davisWxParser.getCurrentStormStartDate());
               log.debug("dailyRain:                      " + davisWxParser.getDailyRain());
               log.debug("monthlyRain:                    " + davisWxParser.getMonthlyRain());
               log.debug("yearlyRain:                     " + davisWxParser.getYearlyRain());
               log.debug("dailyEvapoTranspiration:        " + davisWxParser.getDailyEvapoTranspiration());
               log.debug("monthlyEvapoTranspiration:      " + davisWxParser.getMonthlyEvapoTranspiration());
               log.debug("yearlyEvapoTranspiration:       " + davisWxParser.getYearlyEvapoTranspiration());
               log.debug("transmitterBatteryStatus:       " + Arrays.toString(davisWxParser.getTransmitterBatteryStatus()));
               log.debug("consoleBatteryVoltage:          " + davisWxParser.getConsoleBatteryVoltage());
               log.debug("forecastAsString:               " + davisWxParser.getForecastAsString());
               //log.debug("forecastRuleNumberAsString:     " + davisWxParser.getForecastRuleNumberAsString());
               log.debug("timeOfSunrise:                  " + davisWxParser.getTimeOfSunrise());
               log.debug("timeOfSunset:                   " + davisWxParser.getTimeOfSunset());
               log.info(" flushed data to the DataTurbine. ");
               
                 byteOne   = 0x00;
                 byteTwo   = 0x00;
                 byteThree = 0x00;
                 byteFour  = 0x00;
                 sampleBuffer.clear();
                 sampleByteCount = 0;
                 rbnbChannelMap.Clear();                      
                 //log.debug("Cleared b1,b2,b3,b4. Cleared sampleBuffer. Cleared rbnbChannelMap.");
                 //state = 0;

                // Once the sample is flushed, take a new sample
                  // allow time for the instrument response
                  streamingThread.sleep(2000);
                  this.command = this.commandPrefix + 
                                 this.takeSampleCommand +
                                 this.commandSuffix;
                  this.sentCommand = queryInstrument(command);
                  
          } // end switch statement
          
          // shift the bytes in the FIFO window
          byteFour = byteThree;
          byteThree = byteTwo;
          byteTwo = byteOne;

        } //end while (more unread bytes)
        
        // prepare the buffer to read in more bytes from the stream
        buffer.compact();
    
    
      } // end while (more socket bytes to read)
      this.socketChannel.close();
        
    } catch ( IOException | SAPIException | InterruptedException e ) {
      // handle exceptions
      // In the event of an i/o exception, log the exception, and allow execute()
      // to return false, which will prompt a retry.
      failed = true;
      e.printStackTrace();
      return false;
    } // In the event of an RBNB communication  exception, log the exception,
    // and allow execute() to return false, which will prompt a retry.


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
      //dataSocket.configureBlocking(false);
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
   * A method that returns the TCP port of the source 
   * instrument (i.e. the serial-to-IP converter to which it is attached)
   */
  public int getHostPort(){
    return this.sourceHostPort;
  }

  /**
   * A method that queries the instrument to obtain its ID
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
    
    log.info("DavisWxSource.main() called.");
    
    try {
      // create a new instance of the DavisWxSource object, and parse the command 
      // line arguments as settings for this instance
      final DavisWxSource davisWxSource = new DavisWxSource();
      
      // Set up a simple log that logs to the console
      PropertyConfigurator.configure(davisWxSource.getLogConfigurationFile());

      // parse the commandline arguments to configure the connection, then 
      // start the streaming connection between the source and the RBNB server.
      if ( davisWxSource.parseArgs(args) ) {
        davisWxSource.start();
      }
      
      // Handle ctrl-c's and other abrupt death signals to the process
        // stop the streaming process
        Runtime.getRuntime().addShutdownHook(new Thread(davisWxSource::stop)
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
    Runnable runner = this::runWork;
    
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
