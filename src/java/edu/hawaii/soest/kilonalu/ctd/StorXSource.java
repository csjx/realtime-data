/**
 *  Copyright: 2010 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: To convert a Satlantic STOR-X binary data file into 
 *             RBNB Data Turbine channels for archival and realtime access.
 *    Authors: Christopher Jones
 *
 * $HeadURL: $
 * $LastChangedDate: $
 * $LastChangedBy: $
 * $LastChangedRevision: $
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
import java.lang.StringBuilder;
import java.lang.InterruptedException;

import java.io.File;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.DataInputStream;
import java.io.IOException;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import java.nio.ByteBuffer;

import java.text.SimpleDateFormat;

import java.util.Arrays;
import java.util.Calendar;
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
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

import org.apache.commons.lang.exception.NestableException;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

import org.nees.rbnb.RBNBBase;
import org.nees.rbnb.RBNBSource;

/**
 * A simple class used to process binary data from a Satlantic
 * STOR-X data logger.  The data file is converted into RBNB frames 
 * and pushed into the RBNB DataTurbine real time server.  This class extends 
 * org.nees.rbnb.RBNBSource, which in turn extends org.nees.rbnb.RBNBBase, 
 * and therefore follows the API conventions found in the org.nees.rbnb code.  
 *
 * The parsing of the data stream is performed by the <code>StorXParser</code>
 * class.
 *
 */
public class StorXSource extends RBNBSource {

  /* The default log configuration file location */
  private final String DEFAULT_LOG_CONFIGURATION_FILE = "lib/log4j.properties";

  /* The log configuration file location */
  private String logConfigurationFile = DEFAULT_LOG_CONFIGURATION_FILE;
  
  /* The Logger instance used to log system messages */
  private static Logger logger = Logger.getLogger(StorXSource.class);
  
  /* The XML configuration file location for the list of sensor properties */
  private String xmlConfigurationFile = "lib/sensor.properties.xml";
  
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

  /*
   * The size of the ByteBuffer used to beffer the TCP stream from the 
   * instrument.
   */
  private int bufferSize = DEFAULT_BUFFER_SIZE;
  
  /* A default source IP address for the source sensor data */
  private final String DEFAULT_SOURCE_HOST_NAME = "localhost";  

  /* A default channel name for the source sensor ASCII data */  
  private final String DEFAULT_CHANNEL_NAME  = "DecimalASCIISampleData";
  
  /* The RBNB channel name for the ASCII data */
  private String rbnbChannelName = DEFAULT_CHANNEL_NAME;
  
  /* The date format for the timestamp in the data sample string */
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
  
  /* The timezone used for the sample date */
  private static final TimeZone TZ = TimeZone.getTimeZone("HST");
  
  /*
   * The instance of the StorXParser object used to parse the STOR-X
   * data file and retrieve each of the data fields
   */
   private StorXParser storXParser = null;
   
   /**
    * Constructor - create an empty instance of the StorXSource object, using
    * default values for the RBNB server name and port, source instrument name
    * and port, archive mode, archive frame size, and cache frame size. 
    */
   public StorXSource() {
   }
   
  /**
   * Constructor - create an instance of the StorXSource object, using the
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
  public StorXSource(String serverName, String serverPort, 
                    String archiveMode, int archiveFrameSize, 
                    int cacheFrameSize, String rbnbClientName) {
    
    // Set up a simple logger that logs to the console                   
    PropertyConfigurator.configure(getLogConfigurationFile());
    
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
   * @param serialNumber      - the sensor serial number 
   * @param xmlConfig         - the XMLConfiguration object containing the list of
   *                            sensor properties
   * @param messageAttachment - the binary data file as a ByteBuffer
   */
  protected boolean process(String serialNumber, XMLConfiguration xmlConfig,
                            ByteBuffer messageAttachment) {
    
    logger.debug("StorXSource.process() called.");
    // do not execute the stream if there is no connection
    if (  !isConnected() ) return false;
    
    boolean success = false;
    
    try {
      
      // add channels of data that will be pushed to the server.  
      // Each sample will be sent to the Data Turbine as an rbnb frame.  Information
      // on each channel is found in the XMLConfiguration file (email.account.properties.xml)
      // and the StorXParser object (to get the data string)
      ChannelMap rbnbChannelMap     = new ChannelMap(); // used to flush channels
      ChannelMap registerChannelMap = new ChannelMap(); // used to register channels
      int channelIndex = 0;
        
      this.storXParser = new StorXParser(messageAttachment);
      
      String sensorName           = null;
      String sensorSerialNumber   = null;
      String sensorDescription    = null;
      
      List sensorList = xmlConfig.configurationsAt("account.sensor");
      
      for (Iterator sIterator = sensorList.iterator(); sIterator.hasNext(); ) {
        
        HierarchicalConfiguration sensorConfig = 
          (HierarchicalConfiguration) sIterator.next();
        sensorSerialNumber = sensorConfig.getString("serialNumber");
        
        // find the correct sensor configuration properties
        if ( sensorSerialNumber.equals(serialNumber) ) {
        
          sensorName = sensorConfig.getString("name");
          sensorDescription = sensorConfig.getString("description");
        
          // the message attachment may contain one or more data sample strings,
          // so iterate through them and add them to the RBNB
          String[] sampleStrings = this.storXParser.getSampleStrings();
          Date[]   sampleDates   = this.storXParser.getSampleDates();
          
          if ( sampleStrings.length > 0 && sampleStrings != null ) {
            for ( int index = 0; index < sampleStrings.length; index++ ) {

              // Build the RBNB channel map 

              // get the sample string
              String sampleString = sampleStrings[index];

              // get the sample date and convert it to seconds since the epoch
              Calendar sampleDateTime = Calendar.getInstance();
              sampleDateTime.setTime(sampleDates[index]);
              double sampleTimeAsSecondsSinceEpoch = (double)
                (sampleDateTime.getTimeInMillis()/1000);

              // add the sample timestamp to the rbnb channel map
              //registerChannelMap.PutTime(sampleTimeAsSecondsSinceEpoch, 0d);
              rbnbChannelMap.PutTime(sampleTimeAsSecondsSinceEpoch, 0d);

              // add the DecimalASCIISampleData channel to the channelMap
              channelIndex = registerChannelMap.Add(getRBNBChannelName());
              registerChannelMap.PutUserInfo(channelIndex, "units=none");               
              channelIndex = rbnbChannelMap.Add(getRBNBChannelName());
              rbnbChannelMap.PutMime(channelIndex, "text/plain");
              rbnbChannelMap.PutDataAsString(channelIndex, sampleString);

              // Now register the RBNB channels, and flush the rbnbChannelMap to the
              // DataTurbine
              getSource().Register(registerChannelMap);
              getSource().Flush(rbnbChannelMap);
              logger.info(getRBNBClientName() + " Sample sent to the DataTurbine: " + sampleString);
              registerChannelMap.Clear();
              rbnbChannelMap.Clear();

            } // end for() 

          } else {
            logger.debug("The are no sample strings to to insert.");
            
          }//end if()
          // leave the for loop since we've found the correct config
          break;
          
        } // end if()
        
      } // end for()                                             
      
      //getSource.Detach();
      success = true;
      
    } catch ( SAPIException sapie ) {
      // In the event of an RBNB communication  exception, log the exception, 
      // and allow execute() to return false, which will prompt a retry.
      success = false;
      sapie.printStackTrace();
      return success;
      
    }
    
    return success;
  } // end if (  !isConnected() ) 
  
}
