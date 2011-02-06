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
package edu.hawaii.soest.hioos.storx;

import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.Source;
import com.rbnb.sapi.SAPIException;

import edu.hawaii.soest.hioos.satlantic.Calibration;

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

import org.dhmp.util.HierarchicalMap;
import org.dhmp.util.BasicHierarchicalMap;

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
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy HH:mm:ss Z");
  
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
   * A method that processes the data object passed and flushes the
   * data to the DataTurbine given the sensor properties in the XMLConfiguration
   * passed in.
   *
   * @param xmlConfig - the XMLConfiguration object containing the list of
   *                    sensor properties
   * @param frameMap  - the parsed data as a HierarchicalMap object
   */
  public boolean process(XMLConfiguration xmlConfig, HierarchicalMap frameMap) {
    
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
        
      // this.storXParser = new StorXParser(storXFrame);
      
      String sensorName         = null;
      String sensorSerialNumber = null;
      String sensorDescription  = null;
      boolean isImmersed        = false;
      String calibrationURL     = null;
      
      List sensorList = xmlConfig.configurationsAt("account.logger.sensor");
      
      for (Iterator sIterator = sensorList.iterator(); sIterator.hasNext(); ) {
      //  
        HierarchicalConfiguration sensorConfig = 
          (HierarchicalConfiguration) sIterator.next();
        sensorSerialNumber = sensorConfig.getString("serialNumber");
        
        // find the correct sensor configuration properties
        if ( sensorSerialNumber.equals(frameMap.get("serialNumber")) ) {
        
          sensorName = sensorConfig.getString("name");
          sensorDescription = sensorConfig.getString("description");
          isImmersed = new Boolean(sensorConfig.getString("isImmersed")).booleanValue();
          calibrationURL = sensorConfig.getString("calibrationURL");
          
          // get a Calibration instance to interpret raw sensor values
          Calibration calibration = new Calibration();
          
          if ( calibration.parse(calibrationURL) ) {
            
            // Build the RBNB channel map 

            // get the sample date and convert it to seconds since the epoch
            Date sampleDate = (Date) frameMap.get("date");
            Calendar sampleDateTime = Calendar.getInstance();
            sampleDateTime.setTime(sampleDate);
            double sampleTimeAsSecondsSinceEpoch = (double)
              (sampleDateTime.getTimeInMillis()/1000);
            // and create a string formatted date
            DATE_FORMAT.setTimeZone(TZ);
            String sampleDateAsString = DATE_FORMAT.format(sampleDate).toString();
            
            // get the sample data from the frame map
            ByteBuffer rawFrame          = (ByteBuffer) frameMap.get("rawFrame");
            StorXFrame storXFrame        = (StorXFrame) frameMap.get("parsedFrameObject");
            String serialNumber          = storXFrame.getSerialNumber();
            int rawAnalogChannelOne   = storXFrame.getAnalogChannelOne();
            int rawAnalogChannelTwo   = storXFrame.getAnalogChannelTwo();
            int rawAnalogChannelThree = storXFrame.getAnalogChannelThree();
            int rawAnalogChannelFour  = storXFrame.getAnalogChannelFour();
            int rawAnalogChannelFive  = storXFrame.getAnalogChannelFive();
            int rawAnalogChannelSix   = storXFrame.getAnalogChannelSix();
            int rawAnalogChannelSeven = storXFrame.getAnalogChannelSeven();
            double rawInternalVoltage    = new Float(storXFrame.getInternalVoltage()).doubleValue();
            
            // apply calibrations to the observed data
            double analogChannelOne   = calibration.apply((double) rawAnalogChannelOne  , isImmersed, "AUX_1");
            double analogChannelTwo   = calibration.apply((double) rawAnalogChannelTwo  , isImmersed, "AUX_2");
            double analogChannelThree = calibration.apply((double) rawAnalogChannelThree, isImmersed, "AUX_3");
            double analogChannelFour  = calibration.apply((double) rawAnalogChannelFour , isImmersed, "AUX_4");
            double analogChannelFive  = calibration.apply((double) rawAnalogChannelFive , isImmersed, "AUX_5");
            double analogChannelSix   = calibration.apply((double) rawAnalogChannelSix  , isImmersed, "AUX_6");
            double analogChannelSeven = calibration.apply((double) rawAnalogChannelSeven, isImmersed, "AUX_7");
            double internalVoltage    = calibration.apply(rawInternalVoltage   , isImmersed, "SV");         
            
            String sampleString = "";
            sampleString += String.format("%s",     serialNumber         ) + ", ";
            sampleString += String.format("%5d",    rawAnalogChannelOne  ) + ", ";
            sampleString += String.format("%5d",    rawAnalogChannelTwo  ) + ", ";
            sampleString += String.format("%5d",    rawAnalogChannelThree) + ", ";
            sampleString += String.format("%5d",    rawAnalogChannelFour ) + ", ";
            sampleString += String.format("%5d",    rawAnalogChannelFive ) + ", ";
            sampleString += String.format("%5d",    rawAnalogChannelSix  ) + ", ";
            sampleString += String.format("%5d",    rawAnalogChannelSeven) + ", ";
            sampleString += String.format("%05.2f", internalVoltage      ) + ", ";
            sampleString += sampleDateAsString;
            sampleString += storXFrame.getTerminator();
            
            // add the sample timestamp to the rbnb channel map
            //registerChannelMap.PutTime(sampleTimeAsSecondsSinceEpoch, 0d);
            rbnbChannelMap.PutTime(sampleTimeAsSecondsSinceEpoch, 0d);

            // add the BinaryRawSatlanticFrameData channel to the channelMap
            channelIndex = registerChannelMap.Add("BinaryRawSatlanticFrameData");
            registerChannelMap.PutUserInfo(channelIndex, "units=none");               
            channelIndex = rbnbChannelMap.Add("BinaryRawSatlanticFrameData");
            rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");
            rbnbChannelMap.PutDataAsByteArray(channelIndex, rawFrame.array());
            
            // add the DecimalASCIISampleData channel to the channelMap
            channelIndex = registerChannelMap.Add(getRBNBChannelName());
            registerChannelMap.PutUserInfo(channelIndex, "units=none");               
            channelIndex = rbnbChannelMap.Add(getRBNBChannelName());
            rbnbChannelMap.PutMime(channelIndex, "text/plain");
            rbnbChannelMap.PutDataAsString(channelIndex, sampleString);
            
            // add the serialNumber channel to the channelMap
            channelIndex = registerChannelMap.Add("serialNumber");
            registerChannelMap.PutUserInfo(channelIndex, "units=none");               
            channelIndex = rbnbChannelMap.Add("serialNumber");
            rbnbChannelMap.PutMime(channelIndex, "text/plain");
            rbnbChannelMap.PutDataAsString(channelIndex, serialNumber);
            
            // add the analogChannelOne channel to the channelMap
            channelIndex = registerChannelMap.Add("analogChannelOne");
            registerChannelMap.PutUserInfo(channelIndex, "units=none");               
            channelIndex = rbnbChannelMap.Add("analogChannelOne");
            rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");
            rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{analogChannelOne});
            
            // add the analogChannelTwo channel to the channelMap
            channelIndex = registerChannelMap.Add("analogChannelTwo");
            registerChannelMap.PutUserInfo(channelIndex, "units=none");               
            channelIndex = rbnbChannelMap.Add("analogChannelTwo");
            rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");
            rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{analogChannelTwo});
            
            // add the analogChannelThree channel to the channelMap
            channelIndex = registerChannelMap.Add("analogChannelThree");
            registerChannelMap.PutUserInfo(channelIndex, "units=none");               
            channelIndex = rbnbChannelMap.Add("analogChannelThree");
            rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");
            rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{analogChannelThree});
            
            // add the analogChannelFour channel to the channelMap
            channelIndex = registerChannelMap.Add("analogChannelFour");
            registerChannelMap.PutUserInfo(channelIndex, "units=none");               
            channelIndex = rbnbChannelMap.Add("analogChannelFour");
            rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");
            rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{analogChannelFour});
            
            // add the analogChannelFive channel to the channelMap
            channelIndex = registerChannelMap.Add("analogChannelFive");
            registerChannelMap.PutUserInfo(channelIndex, "units=none");               
            channelIndex = rbnbChannelMap.Add("analogChannelFive");
            rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");
            rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{analogChannelFive});
            
            // add the analogChannelSix channel to the channelMap
            channelIndex = registerChannelMap.Add("analogChannelSix");
            registerChannelMap.PutUserInfo(channelIndex, "units=none");               
            channelIndex = rbnbChannelMap.Add("analogChannelSix");
            rbnbChannelMap.PutMime(channelIndex, "text/plain");
            rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{analogChannelSix});
            
            // add the analogChannelSeven channel to the channelMap
            channelIndex = registerChannelMap.Add("analogChannelSeven");
            registerChannelMap.PutUserInfo(channelIndex, "units=none");               
            channelIndex = rbnbChannelMap.Add("analogChannelSeven");
            rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");
            rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{analogChannelSeven});
            
            // add the internalVoltage channel to the channelMap
            channelIndex = registerChannelMap.Add("internalVoltage");
            registerChannelMap.PutUserInfo(channelIndex, "units=V");               
            channelIndex = rbnbChannelMap.Add("internalVoltage");
            rbnbChannelMap.PutMime(channelIndex, "application/octet-stream");
            rbnbChannelMap.PutDataAsFloat64(channelIndex, new double[]{internalVoltage});

            // Now register the RBNB channels, and flush the rbnbChannelMap to the
            // DataTurbine
            getSource().Register(registerChannelMap);
            getSource().Flush(rbnbChannelMap);
            logger.info("Sample sent to the DataTurbine:" + sampleString);
            
            registerChannelMap.Clear();
            rbnbChannelMap.Clear();
            
          } else {
            
            logger.info("Couldn't apply the calibration coefficients. " +
                        "Skipping this sample.");
                        
          } // end if()
          
        } // end if()
        
      } // end for()                                             
      
      //getSource.Detach();
      success = true;
      
    } catch ( Exception sapie ) {
    //} catch ( SAPIException sapie ) {
      // In the event of an RBNB communication  exception, log the exception, 
      // and allow execute() to return false, which will prompt a retry.
      success = false;
      sapie.printStackTrace();
      return success;
      
    }
    
    return success;
  } // end if (  !isConnected() ) 
  
  /**
   * A method that gets the size, in bytes, of the ByteBuffer used in streaming 
   * data from a source instrument via a TCP connection
   */
   public int getBufferSize() {
     return this.bufferSize;
   }
   
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
    "$LastChangedDate: 2010-04-10 16:41:48 -0600 (Sat, 10 Apr 2010) $" +
    "$LastChangedBy: cjones $" +
    "$LastChangedRevision: 604 $" +
    "$HeadURL: https://bbl.ancl.hawaii.edu/projects/bbl/trunk/src/java/edu/hawaii/soest/kilonalu/storX/StorXSource.java $"
    );
  }


  /**
   * A method that starts the connection with the RBNB DataTurbine
   */
  public boolean startConnection() {
    return connect();
  }
   
  /**
   * A method that stops the connection with the RBNB DataTurbine
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
    
    return true;
  }

  /**
   * A method that sets the command line options for this class.  This method 
   * calls the <code>RBNBSource.setBaseOptions()</code> method in order to set
   * properties such as the sourceHostName, sourceHostPort, serverName, and
   * serverPort.
   */
  protected Options setOptions() {
    Options options = setBaseOptions(new Options());
                      
    return options;
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
