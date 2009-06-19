/**
 *  Copyright: 2009 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: A class that fetches certain LOOP sample data fields produced by
 *            a Davis Scientific Vantage Pro 2 Weather station and converts them
 *            an ad hoc XML file syntax to update a weather web page.
 *   Authors: Christopher Jones
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
package edu.hawaii.soest.kilonalu.dvp2;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;

import java.text.DateFormat;

import java.util.Arrays;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;


import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.SAPIException;
import com.rbnb.sapi.Sink;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.nees.rbnb.RBNBBase;
import org.nees.rbnb.RBNBUtilities;

/**
 * This class grabs data from a Davis Instruments Vantage Pro 2 weather station
 * data source and converts certain channels to an ad hoc XML syntax used to 
 * update an HTML weather page.
 * 
 * @author Christopher Jones (modifications for BBL data)
 */
public class DavisWxXMLSink extends RBNBBase {
  /**
   * The export interval used to periodically export data (in seconds)
   */
  private int exportInterval = 5000;
  
  /** the export file */
  private File xmlFile;
  
  /** the export file output stream*/
  private FileOutputStream fileOutputStream;
  
  /** the RBNB sink name */
  private String sinkName = "DavisWxXMLSink";

  /** the RBNB sink */
  private Sink sink;
  
  /** the RBNB source name */
  private String sourceName = "KNWXXX_XXXDVP2XXXR00";

  /** the start time for data export */
  private double startTime = 0.0;

  /** the end time for data export */
  private double endTime = Double.MAX_VALUE;
  
  /** a flag to indicate if we are connected to the RBNB server or not */
  private boolean connected = false;

  /** The default log configuration file location */
  private final String DEFAULT_LOG_CONFIGURATION_FILE = "lib/log4j.properties";

  /** The log configuration file location */
  private String logConfigurationFile = DEFAULT_LOG_CONFIGURATION_FILE;
  
  /**
   * The Logger instance used to log system messages 
   */
  private static Logger logger = Logger.getLogger(DavisWxXMLSink.class);
  
  /**
   * Constructor: creates DavisWxXMLSink.
   */
  public DavisWxXMLSink() {
    super();    
    sink = new Sink();
  }
  
  /**
   * Runs DavisWxXMLSink.
   * 
   * @param args  the command line arguments
   */
  public static void main(String[] args) {
    
    final DavisWxXMLSink davisWxXMLSink = new DavisWxXMLSink();
    
    // Set up a simple logger that logs to the console
    PropertyConfigurator.configure(davisWxXMLSink.getLogConfigurationFile());
        
    if ( davisWxXMLSink.parseArgs(args) ) {
      
        // export data on a schedule

        TimerTask exportXML = new TimerTask() {
          public void run() {
            logger.debug("TimerTask.run() called.");
              davisWxXMLSink.export();      
          }
        };

        Timer exportTimer = new Timer();
        // run the exportXML timer task on the hour, every hour
        exportTimer.scheduleAtFixedRate(exportXML, 
          new Date(), davisWxXMLSink.exportInterval);      

    }
  }
  
  /**
   * This method overrides the setOptions() method in RBNBBase and adds in 
   * options for the various command line flags.
   */
  protected Options setOptions() {
    Options opt = setBaseOptions(new Options()); // uses h, s, p
    opt.addOption("f", true, "File output path of the XML file");
    
    return opt;
  }

  /**
   * This method overrides the setArgs() method in RBNBBase and sets the values
   * of the various command line arguments
   */
  protected boolean setArgs(CommandLine cmd) {
    logger.debug("DavisWxXMLSink.setArgs() called.");
    
    if (cmd.hasOption('f')) {
      String a = cmd.getOptionValue('f');
      if (a != null)
        this.xmlFile = new File(a);
    }
    
    return setBaseArgs(cmd);
    
  }

  /**
   * Setup the paramters for data export.
   * 
   * @param serverName         the RBNB server name
   * @param serverPort         the RBNB server port
   * @param sinkName           the RBNB sink name
   * @return                   true if the setup succeeded, false otherwise
   */
  public boolean setup(String serverName, int serverPort, String sinkName) {
    logger.debug("DavisWxXMLSink.setup() called.");
    
    setServerName(serverName);
    setServerPort(serverPort);
    
    this.sinkName = sinkName;
    
    return connect();
  }
  
  /**
   * Export data to disk.
   */
  public boolean export() {
    logger.debug("DavisWxXMLSink.export() called.");

    if ( setup(this.getServerName(), 
               this.getServerPort(), 
               this.getRBNBClientName()) ) {
    
      try {
        ChannelMap requestMap = new ChannelMap();
        
        String fullSourceName = "/KNHIGCampusDataTurbine" + 
                                "/" + this.getRBNBClientName() + "/";
        int channelIndex = 0;
        
        // add the barTrendAsString field data
        channelIndex = requestMap.Add(fullSourceName + "barTrendAsString");                // Falling Slowly
        
        // add the barometer field data
        channelIndex = requestMap.Add(fullSourceName + "barometer");                      // 29.9
        
        // add the insideTemperature field data
        channelIndex = requestMap.Add(fullSourceName + "insideTemperature");               // 83.9
        
        // add the insideHumidity field data
        channelIndex = requestMap.Add(fullSourceName + "insideHumidity");                  // 51
        
        // add the outsideTemperature field data
        channelIndex = requestMap.Add(fullSourceName + "outsideTemperature");              // 76.7
        
        // add the windSpeed field data
        channelIndex = requestMap.Add(fullSourceName + "windSpeed");                       // 5
        
        // add the tenMinuteAverageWindSpeed field data
        channelIndex = requestMap.Add(fullSourceName + "tenMinuteAverageWindSpeed");      // 4
        
        // add the windDirection field data
        channelIndex = requestMap.Add(fullSourceName + "windDirection");                   // 80
        
        // add the outsideHumidity field data
        channelIndex = requestMap.Add(fullSourceName + "outsideHumidity");                 // 73
        
        // add the rainRate field data
        channelIndex = requestMap.Add(fullSourceName + "rainRate");                        // 0.0
        
        // add the uvRadiation field data
        channelIndex = requestMap.Add(fullSourceName + "uvRadiation");                     // 0
        
        // add the solarRadiation field data
        channelIndex = requestMap.Add(fullSourceName + "solarRadiation");                  // 0.0
        
        // add the dailyRain field data
        channelIndex = requestMap.Add(fullSourceName + "dailyRain");                       // 0.0
        
        // add the monthlyRain field data
        channelIndex = requestMap.Add(fullSourceName + "monthlyRain");                     // 0.0
        
        // add the forecastAsString field data
        channelIndex = requestMap.Add(fullSourceName + "forecastAsString");                // Partially Cloudy
      
        // make the request to the DataTurbine for the above channels
        sink.Request(requestMap, 0.0, 0.0, "newest");
        ChannelMap responseMap = sink.Fetch(3000); // fetch with 5 sec timeout
        
        // then parse the results to and build the XML output
        int index = responseMap.GetIndex(fullSourceName + "barTrendAsString");
        
        if ( index >= 0 ) {
          // convert sec to millisec
          double timestamp = responseMap.GetTimes(index)[0];
          long unixTime = (long) (timestamp * 1000.0);        

          // get the updateDate
          Date updateDate = new Date(unixTime);
          String updateDateString = 
            DateFormat.getDateTimeInstance(DateFormat.LONG, 
                                           DateFormat.LONG).format(updateDate);
          
          // create the ad hoc XML file from the data from the DataTurbine
          this.fileOutputStream = new FileOutputStream(xmlFile);
          StringBuilder sb = new StringBuilder();

          sb.append("<?xml version=\"1.0\"?>\n");
          sb.append("<wx>\n");
          sb.append("  <updatetime>"  + updateDateString + "</updatetime>\n");
          sb.append("  <barotrend>"   + responseMap.GetDataAsString(responseMap.GetIndex(fullSourceName             + "barTrendAsString")         )[0]              + "</barotrend>\n" );
          sb.append("  <baropress>"   + (new Float(responseMap.GetDataAsFloat32(responseMap.GetIndex(fullSourceName + "barometer")                )[0])).toString() + "</baropress>\n" );
          sb.append("  <outtemp>"     + (new Float(responseMap.GetDataAsFloat32(responseMap.GetIndex(fullSourceName + "outsideTemperature")       )[0])).toString() + "</outtemp>\n"   );
          sb.append("  <intemp>"      + (new Float(responseMap.GetDataAsFloat32(responseMap.GetIndex(fullSourceName + "insideTemperature")        )[0])).toString() + "</intemp>\n"    );
          sb.append("  <inrelhum>"    + (new Integer(responseMap.GetDataAsInt32(responseMap.GetIndex(fullSourceName + "insideHumidity")           )[0])).toString() + "</inrelhum>\n"  );
          sb.append("  <outrelhum>"   + (new Integer(responseMap.GetDataAsInt32(responseMap.GetIndex(fullSourceName + "outsideHumidity")          )[0])).toString() + "</outrelhum>\n" );
          sb.append("  <windspd>"     + (new Integer(responseMap.GetDataAsInt32(responseMap.GetIndex(fullSourceName + "windSpeed")                )[0])).toString() + "</windspd>\n"   );
          sb.append("  <windavg>"     + (new Integer(responseMap.GetDataAsInt32(responseMap.GetIndex(fullSourceName + "tenMinuteAverageWindSpeed"))[0])).toString() + "</windavg>\n"   );
          sb.append("  <winddir>"     + (new Integer(responseMap.GetDataAsInt32(responseMap.GetIndex(fullSourceName + "windDirection")            )[0])).toString() + "</winddir>\n"   );
          sb.append("  <todayrain>"   + (new Float(responseMap.GetDataAsFloat32(responseMap.GetIndex(fullSourceName + "dailyRain")                )[0])).toString() + "</todayrain>\n" );
          sb.append("  <rainrate>"    + (new Float(responseMap.GetDataAsFloat32(responseMap.GetIndex(fullSourceName + "rainRate")                 )[0])).toString() + "</rainrate>\n"  );
          sb.append("  <uv>"          + (new Integer(responseMap.GetDataAsInt32(responseMap.GetIndex(fullSourceName + "uvRadiation")              )[0])).toString() + "</uv>\n"        );
          sb.append("  <solrad>"      + (new Float(responseMap.GetDataAsFloat32(responseMap.GetIndex(fullSourceName + "solarRadiation")           )[0])).toString() + "</solrad>\n"    );
          sb.append("  <monthrain>"   + (new Float(responseMap.GetDataAsFloat32(responseMap.GetIndex(fullSourceName + "monthlyRain")              )[0])).toString() + "</monthrain>\n" );
          sb.append("  <forecast>"    + responseMap.GetDataAsString(responseMap.GetIndex(fullSourceName             + "forecastAsString")         )[0]              + "</forecast>\n"  );
          sb.append("</wx>\n");
          
          logger.info("\n" + sb.toString());
          
          this.fileOutputStream.write(sb.toString().getBytes());
          this.fileOutputStream.close();
          
        } else {
          logger.debug("The index is out of bounds: " + index);
        }
                                                                                         
      } catch ( java.io.FileNotFoundException fnfe ) {
        logger.debug("Error: Unable to open XML output file for writing.");
        logger.debug("Error message: " + fnfe.getMessage());
        disconnect();
        return false;
      } catch ( java.io.IOException ioe ) {
        logger.debug("Error: There was a problem writing to the XML file.");
        logger.debug("Error message: " + ioe.getMessage());
        disconnect();
        return false;
      } catch ( com.rbnb.sapi.SAPIException sapie ){
        logger.debug("Error: There was a problem with the DataTurbine connection.");
        logger.debug("Error message: " + sapie.getMessage());
        disconnect();
        return false;
        
      }
      return true;
      
    } else {
      return false;
      
    }
  }
  

  /**
   * Connect to the RBNB server.
   * 
   * @return  true if connected, false otherwise
   */
  private boolean connect() {
    logger.debug("DavisWxXMLSink.connect() called.");
    if (isConnected()) {
      return true;
    }
    
    try {
      sink.OpenRBNBConnection(getServer(), sinkName);
      
    } catch ( SAPIException e ) {
      logger.debug("Error: Unable to connect to server.");
      disconnect();
      return false;
    }
    
    connected = true;
    
    return true;
  }
  
  /**
   * Disconnects from the RBNB server.
   */
  private void disconnect() {
    logger.debug("DavisWxXMLSink.disconnect() called.");
    if (!isConnected()) {
      return;
    }

    try {
      sink.CloseRBNBConnection();
      fileOutputStream.close();
      
    } catch (java.io.IOException ioe){
       ioe.printStackTrace();
       connected = false;
    }
    connected = false;
  }
  
  /**
   * Sees if we are connected to the RBNB server.
   * 
   * @return  true if connected, false otherwise
   */
  public boolean isConnected() {
    return connected;
  }

  /** A method that returns the CVS version string */
  protected String getCVSVersionString() {
    return ("$LastChangedDate: 2009-04-14 14:15:07 -0600 (Tue, 14 Apr 2009) $\n"
        + "$LastChangedRevision: 323 $"
        + "$LastChangedBy: cjones $"
        + "$HeadURL: https://bbl.ancl.hawaii.edu/projects/bbl/trunk/src/java/edu/hawaii/soest/kilonalu/utilities/DavisWxXMLSink.java $"); 
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