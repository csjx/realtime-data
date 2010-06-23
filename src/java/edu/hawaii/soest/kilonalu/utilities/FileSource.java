/**
 *  Copyright: 2010 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: To convertlines of an  ASCII data file into RBNB Data Turbine
 *             frames for archival and realtime access.
 *    Authors: Christopher Jones
 *
 * $HeadURL: $
 * $LastChangedDate: $
 * $LastChangedBy:  $
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
package edu.hawaii.soest.kilonalu.utilities;

import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.Source;
import com.rbnb.sapi.SAPIException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.lang.InterruptedException;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

import org.nees.rbnb.RBNBBase;
import org.nees.rbnb.RBNBSource;

/**
 * A class used to harvest ASCII data lines from a file.
 * The data are converted into RBNB frames and pushed into the RBNB DataTurbine 
 * real time server.  This class extends org.nees.rbnb.RBNBSource, which in 
 * turn extends org.nees.rbnb.RBNBBase, and therefore follows the API 
 * conventions found in the org.nees.rbnb code.  
 *
 */
public class FileSource extends RBNBSource {

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

  /* A default RBNB channel name for the given source instrument */  
  private String DEFAULT_RBNB_CHANNEL = "DecimalASCIISampleData";

  /* The name of the RBNB channel for this data stream */
  private String rbnbChannelName = DEFAULT_RBNB_CHANNEL;
  
  /* A default source address for the given source email server */
  private final String DEFAULT_FILE_NAME = "/tmp/data.txt";  

  /* A file name for the given data source file */
  private String fileName = DEFAULT_FILE_NAME;

  /* The default IP address or DNS name of the RBNB server */
  private static final String DEFAULT_SERVER_NAME = "localhost";
  
  /* The default TCP port of the RBNB server */
  private static final int DEFAULT_SERVER_PORT = 3333;
  
  /* The IP address or DNS name of the RBNB server */
  private String serverName = DEFAULT_SERVER_NAME;
  
  /* The default TCP port of the RBNB server */
  private int serverPort = DEFAULT_SERVER_PORT;
  
  /* The address and port string for the RBNB server */
  private String server = serverName + ":" + serverPort;
  
  /* The date format for the timestamp in the data sample string */
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
  
  /* The timezone used for the sample date */
  private static final TimeZone TZ = TimeZone.getTimeZone("SST");
  
  /* The buffered reader data representing the data file stream  */
  BufferedReader fileReader;
  
  /* The pattern used to identify data lines */
  Pattern dataPattern;
  
  /* The default log configuration file location */
  private final String DEFAULT_LOG_CONFIGURATION_FILE = "lib/log4j.properties";

  /* The log configuration file location */
  private String logConfigurationFile = DEFAULT_LOG_CONFIGURATION_FILE;
  
  /* The Logger instance used to log system messages */
  private static Logger logger = Logger.getLogger(FileSource.class);

  /* The state used to track the data processing */
  protected int state = 0;
    
  /* A boolean indicating if we are ready to stream (connected) */
  private boolean readyToStream = false;
  
  /* The thread used for streaming data */
  private Thread streamingThread;
  
  /* The polling interval (millis) used to check for new lines in the data file */
  private final int POLL_INTERVAL = 1000;
  
  /*
   * An internal Thread setting used to specify how long, in milliseconds, the
   * execution of the data streaming Thread should wait before re-executing
   * 
   * @see execute()
   */
  private final int RETRY_INTERVAL = 5000;
    
  /**
   * Constructor - create an empty instance of the FileSource object, using
   * default values for the RBNB server name and port, source instrument name,
   * archive mode, archive frame size, and cache frame size. 
   */
  public FileSource() {
  }

  /**
   * A method that executes the reading of data from the data file to the RBNB
   * server after all configuration of settings, connections to hosts, and
   * thread initiatizing occurs.  This method contains the detailed code for 
   * reading the data and interpreting the data files.
   */
  protected boolean execute() {
    logger.debug("FileSource.execute() called.");
    boolean failed = true; // indicates overall success of execute()
    
    // do not execute the stream if there is no connection
    if (  !isConnected() ) return false;
    
    try {
      
      // open the data file for monitoring
      FileReader reader = new FileReader(new File(getFileName()));
      this.fileReader = new BufferedReader(reader);
      
      // add channels of data that will be pushed to the server.  
      // Each sample will be sent to the Data Turbine as an rbnb frame.  
      int channelIndex = 0;
      
      ChannelMap rbnbChannelMap     = new ChannelMap(); // used to insert channel data
      ChannelMap registerChannelMap = new ChannelMap(); // used to register channels
      
      // add the DecimalASCIISampleData channel to the channelMap
      channelIndex = registerChannelMap.Add(getRBNBChannelName());
      registerChannelMap.PutUserInfo(channelIndex, "units=none");               
      // and register the RBNB channels
      getSource().Register(registerChannelMap);
      registerChannelMap.Clear();
      
      // poll the data file for new lines of data and insert them into the RBNB
      while (true) {
        String line = fileReader.readLine();
        
        if (line == null ) {
          this.streamingThread.sleep(POLL_INTERVAL);
          
        } else {
          
          // test the line for the expected data pattern
          Matcher matcher = this.dataPattern.matcher(line);
          
          // if the line matches the data pattern, insert it into the DataTurbine
          if ( matcher.matches() ) {
            logger.debug("This line matches the data line pattern: " + line);
            
            // extract the date from the data line
            String[] columns      = line.trim().split(",");
            String   dateString   = columns[columns.length - 1]; // last field

            DATE_FORMAT.setTimeZone(TZ);
            Date sampleDate = DATE_FORMAT.parse(dateString);
            
            // convert the sample date to seconds since the epoch
            Calendar sampleDateTime = Calendar.getInstance();
            sampleDateTime.setTime(sampleDate);
            double sampleTimeAsSecondsSinceEpoch = (double)
              (sampleDateTime.getTimeInMillis()/1000);

            // add the sample timestamp to the rbnb channel map
            //registerChannelMap.PutTime(sampleTimeAsSecondsSinceEpoch, 0d);
            rbnbChannelMap.PutTime(sampleTimeAsSecondsSinceEpoch, 0d);
            
            // then add the data line to the channel map and insert it
            // into the DataTurbine
            channelIndex = rbnbChannelMap.Add(getRBNBChannelName());
            rbnbChannelMap.PutMime(channelIndex, "text/plain");
            rbnbChannelMap.PutDataAsString(channelIndex, line);
            getSource().Flush(rbnbChannelMap);
            
            logger.info(getRBNBClientName() + " Sample sent to the DataTurbine: " + line.trim());
            rbnbChannelMap.Clear();
            
          } else {
            logger.info("The current line doesn't match an expected "     +
                        "data line pattern. Skipping it.  The line was: " +
                        line);
                        
          }
        }
      }
      
    } catch ( ParseException pe ) {
      logger.info("There was a problem parsing the sample date string. " +
                  "The message was: " + pe.getMessage());
      try {
        this.fileReader.close();        
      } catch (IOException ioe2) {
        logger.info("There was a problem closing the data file.  The " +
                    "message was: " + ioe2.getMessage());
      }
      failed = true;
      return !failed;
    } catch ( SAPIException sapie ) {
      logger.info("There was a problem communicating with the DataTurbine. " +
                  "The message was: " + sapie.getMessage());
      try {
        this.fileReader.close();        
      } catch (IOException ioe2) {
        logger.info("There was a problem closing the data file.  The " +
                    "message was: " + ioe2.getMessage());
      }
      failed = true;
      return !failed;
      
    } catch ( InterruptedException ie ) {
      logger.info("There was a problem while polling the data file. The " +
                   "message was: " + ie.getMessage());
      try {
        this.fileReader.close();        
      } catch (IOException ioe2) {
        logger.info("There was a problem closing the data file.  The " +
                    "message was: " + ioe2.getMessage());
      }
      failed = true;
      return !failed;
      
    } catch ( IOException ioe ) {
      logger.info("There was a problem opening the data file. " + 
                   "The message was: " + ioe.getMessage());
      failed = true;
      return !failed;
      
    }
    
  }
  
  /**
   * A method that returns the name of the RBNB channel that contains the 
   * streaming data from this instrument
   */
  public String getRBNBChannelName(){
    return this.rbnbChannelName;
  }

  /**
   * A method that gets the file name of the source data file
   *
   * @return fileName - the name of the source data file
   */
  public String getFileName() {
    return this.fileName;
  }

  /**
   * A method that returns the versioning info for this file.  In this case, 
   * it returns a String that includes the Subversion LastChangedDate, 
   * LastChangedBy, LastChangedRevision, and HeadURL fields.
   */
  public String getCVSVersionString(){
    return (
    "$LastChangedDate: 2010-04-13 17:57:16 -0600 (Tue, 13 Apr 2010) $" +
    "$LastChangedBy: cjones $" +
    "$LastChangedRevision: 609 $" +
    "$HeadURL: https://bbl.ancl.hawaii.edu/projects/bbl/trunk/src/java/edu/hawaii/soest/kilonalu/ctd/FileSource.java $"
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
   * The main method for running the code
   * @ param args[] the command line list of string arguments, none are needed
   */
  public static void main (String args[]) {
        
    try {
      // create a new instance of the FileSource object, and parse the command 
      // line arguments as settings for this instance
      final FileSource fileSource = new FileSource();
            
      // Handle ctrl-c's and other abrupt death signals to the process
      Runtime.getRuntime().addShutdownHook(new Thread() {
        // stop the streaming process
        public void run() {
             
          fileSource.disconnect();
             
        }
      }
      );
      
      // Set up a simple logger that logs to the console
      PropertyConfigurator.configure(fileSource.getLogConfigurationFile());
      
      // parse the commandline arguments to configure the connection, then 
      // start the streaming connection between the source and the RBNB server.
      if ( fileSource.parseArgs(args) ) {
        fileSource.start();
        
      }
        
    } catch ( Exception e ) {
      logger.info("Error in main(): " + e.getMessage());
      e.printStackTrace();
      
    }
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
    
    // handle the -F option
    if ( command.hasOption("F") ) {
      String fileName = command.getOptionValue("F");
      if ( fileName != null ) {
        setFileName(fileName);
      }
    }

    // handle the -e option
    if ( command.hasOption("e") ) {
      String expression = command.getOptionValue("e");
      if ( expression != null ) {
        setPattern(expression);
      }
    }

    // handle the -C option
    if ( command.hasOption("C") ) {
      String channelName = command.getOptionValue("C");
      if ( channelName != null ) {
        setChannelName(channelName);
      }
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
    
    return true;
  }

  /**
   * A method that sets the file name of the source data file
   *
   * @param fileName - the name of the source data file
   */
  public void setFileName(String fileName) {
    this.fileName = fileName;
    
  }

  /**
   * A method that sets the regular expression pattern for matching data lines
   *
   * @param pattern - the pattern string used to match data lines
   */
  public void setPattern(String pattern) {
    this.dataPattern = Pattern.compile(pattern);
    
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
    options.addOption("F", true, "Data source file name e.g. " + getFileName());
    options.addOption("e", true, "regular expression for data line matching, e.g \"*[0-9][0-9]\" ");
    options.addOption("C", true, "RBNB source channel name e.g. " + getRBNBChannelName());
    options.addOption("s", true,  "RBNB Server Hostname");
    options.addOption("p", true,  "RBNB Server Port Number");
                      
    return options;
  }

}
