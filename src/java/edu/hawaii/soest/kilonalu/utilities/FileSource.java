/**
 *  Copyright: 2010 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: To convert lines of an  ASCII data file into RBNB Data Turbine
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
package edu.hawaii.soest.kilonalu.utilities;

import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.Sink;
import com.rbnb.sapi.Source;
import com.rbnb.sapi.SAPIException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.lang.InterruptedException;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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

  /* The default date format for the timestamp in the data sample string */
  private static final SimpleDateFormat defaultDateFormat = 
		  new SimpleDateFormat("dd MMM yyyy HH:mm:ss");

  /* The default sample line delimiter separating columns of data */
  private static final String DEFAULT_DELIMITER = ",";
  
  /* The delimiter separating variables in the sample line */
  private static String delimiter = DEFAULT_DELIMITER;

  /* A list of date format patterns to be applied to designated date/time fields */
  private List<String> dateFormats = null;

  /* A one-based list of Integers corresponding to the observation date/time field indices */
  private List<Integer> dateFields = null;
  
  /* The instance of TimeZone to use when parsing dates */
  private TimeZone tz;
  
  /* The buffered reader data representing the data file stream  */
  private BufferedReader fileReader;
  
  /* The pattern used to identify data lines */
  private Pattern dataPattern;
  
  /* The timezone that the data samples are taken in as a string (UTC, HST, etc.) */
  private String timezone = "UTC";
  
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
  private final int POLL_INTERVAL = 10000;
  
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
      
      // on execute(), query the DT to find the timestamp of the last sample inserted
      // and be sure the following inserts are after that date
      ChannelMap requestMap = new ChannelMap();
      int entryIndex = requestMap.Add(getRBNBClientName() + "/" + getRBNBChannelName());
      logger.debug("Request Map: " + requestMap.toString());
      Sink sink = new Sink();
      sink.OpenRBNBConnection(getServer(), "lastEntrySink");
      
      sink.Request(requestMap, 0., 1., "newest");
      ChannelMap responseMap = sink.Fetch(5000); // get data within 5 seconds 
      // initialize the last sample date 
      Date initialDate = new Date();
      long lastSampleTimeAsSecondsSinceEpoch = initialDate.getTime()/1000L;
      logger.debug("Initialized the last sample date to: " + initialDate.toString());
      logger.debug("The last sample date as a long is: " + lastSampleTimeAsSecondsSinceEpoch);
      
      if ( responseMap.NumberOfChannels() == 0 )  {
        // set the last sample time to 0 since there are no channels yet
        lastSampleTimeAsSecondsSinceEpoch = 0L;
        logger.debug("Resetting the last sample date to the epoch: " +
                     (new Date(
                          lastSampleTimeAsSecondsSinceEpoch * 1000L
                          )).toString()
                     );
      
      } else if ( responseMap.NumberOfChannels() > 0 )  {
        lastSampleTimeAsSecondsSinceEpoch = 
          new Double(responseMap.GetTimeStart(entryIndex)).longValue();
        logger.debug("There are existing channels. Last sample time: " +
                     (new Date(
                         lastSampleTimeAsSecondsSinceEpoch * 1000L
                         )).toString()
                    );
                     
      }
      
      sink.CloseRBNBConnection();
      
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
            
            Date sampleDate = getSampleDate(line);
                        
            if ( this.dateFormats == null || this.dateFields == null ) {
            	logger.warn("Using the default datetime format and field for sample data. " +
                    "Use the -f and -d options to explicitly set date time fields.");
            }
            logger.debug("Sample date is: " + sampleDate.toString());
            
            // convert the sample date to seconds since the epoch
            long sampleTimeAsSecondsSinceEpoch = (sampleDate.getTime()/1000L);
            
            // only insert samples newer than the last sample seen at startup 
            // and that are not in the future  (> 1 hour since the CTD clock
            // may have drifted)
            Calendar currentCal = Calendar.getInstance();
            this.tz = TimeZone.getTimeZone(this.timezone);
            currentCal.setTimeZone(this.tz);
            currentCal.add(Calendar.HOUR, 1);
            Date currentDate = currentCal.getTime();
            
            if ( lastSampleTimeAsSecondsSinceEpoch < 
                 sampleTimeAsSecondsSinceEpoch     &&
                 sampleTimeAsSecondsSinceEpoch     < 
                 currentDate.getTime()/1000L ) {
              
              // add the sample timestamp to the rbnb channel map
              //registerChannelMap.PutTime(sampleTimeAsSecondsSinceEpoch, 0d);
              rbnbChannelMap.PutTime((double) sampleTimeAsSecondsSinceEpoch, 0d);
            
              // then add the data line to the channel map
              channelIndex = rbnbChannelMap.Add(getRBNBChannelName());
              rbnbChannelMap.PutMime(channelIndex, "text/plain");
              rbnbChannelMap.PutDataAsString(channelIndex, line + "\r\n");
              
              // be sure we are connected
              int numberOfChannelsFlushed = 0;
              
              try {
                
                //insert into the DataTurbine
            	numberOfChannelsFlushed = getSource().Flush(rbnbChannelMap, true);
            	
            	// in the event we just lost the network, sleep, try again
            	while (numberOfChannelsFlushed < 1 ) {
            		logger.debug("No channels flushed, trying again in 10 seconds.");
            		Thread.sleep(10000L);
                	numberOfChannelsFlushed = getSource().Flush(rbnbChannelMap, true);

            	}
                
              } catch ( SAPIException sapie ) {
                // reconnect if an exception is thrown on Flush()
            	logger.debug("Error while flushing the source: " + sapie.getMessage());
            	
            	logger.debug("Disconnecting from the DataTurbine.");
                disconnect();
            	logger.debug("Connecting to the DataTurbine.");
            	connect();
                getSource().Flush(rbnbChannelMap);
            	
                // in the event we just lost the network, sleep, try again
            	while (numberOfChannelsFlushed < 1 ) {
            		logger.debug("No channels flushed, trying again in 10 seconds.");
            		Thread.sleep(10000L);
                	numberOfChannelsFlushed = getSource().Flush(rbnbChannelMap, true);

            	}

              }
              
              // reset the last sample time to the sample just inserted
              lastSampleTimeAsSecondsSinceEpoch = sampleTimeAsSecondsSinceEpoch;
              logger.debug("Last sample time is now: " + 
                            (new Date(
                                        lastSampleTimeAsSecondsSinceEpoch * 1000L
                                     ).toString())
                          );
              logger.info(getRBNBClientName()                 +
                          " Sample sent to the DataTurbine: " +
                          line.trim());
              rbnbChannelMap.Clear();
              
            } else {
              logger.info("The current line is earlier than the last entry " +
                          "in the Data Turbine or is a date in the future. " +
                          "Skipping it.  The line was: " +
                          line);
              
            }
            
          } else {
            logger.info("The current line doesn't match an expected "     +
                        "data line pattern. Skipping it.  The line was: " +
                        line);
                        
          }
        }  // end if()
      } // end while
      
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

	/*
	args = new String[]{
			 "-F", "/Users/cjones/Documents/Development/bbl/trunk/test/resources/edu/hawaii/soest/kilonalu/utilities/NS01-example-data.txt",
			 "-e", "# *.*, *.*, *.*, *.*, *.*, *.*, *\\\\d{2} [A-Z][a-z][a-z] *\\\\d{4} *\\\\d{2}:\\\\d{2}:\\\\d{2}\\\\s*",
			 "-S", "TEST01_001CTDXXXXR00",
			 "-C", "DecimalASCIISampleData",
			 "-d", "yyyy-MM-dd HH:mm:ss",
			 "-f", "7",
			 "-s", "127.0.0.1",
			 "-p", "3333",
			 "-z", "126000",
			 "-Z", "31536000"
	};
	*/
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

    // handle the -t option
    if ( command.hasOption("t") ) {
      String timezone = command.getOptionValue("t");
      if ( timezone != null ) {
        setTimezone(timezone);
      }
    }

    // handle the -d option, requires the -f option
	if (command.hasOption("d")) {
		if (!command.hasOption("f")) {
			System.out.println("The -f option is required to identify which " + 
		    "fields in the sample correspond to observation dates/times.");
			return false;
		}

		List<String> formatList = new ArrayList<String>();
		String dateFormats = command.getOptionValue("d");
		if (dateFormats != null) {
			String[] formats = dateFormats.split(",");
			for (String format : formats) {
				try {
					SimpleDateFormat f = new SimpleDateFormat(format);
					formatList.add(format);

				} catch (Exception e) {
					System.out.println("The date format " + format + 
				        " is not a proper date or time format.  See " + 
					    "http://docs.oracle.com/javase/6/docs/api/java/text/SimpleDateFormat.html" + 
				        " for allowable character patterns.");
					e.printStackTrace();
					return false;
				}
			}
			setDateFormats(formatList);
		}
	}

    // handle the -d option, requires the -f option
    if ( command.hasOption("D") ) {
		String delim = command.getOptionValue("D");
		try {
			Pattern pattern = Pattern.compile(delim);

		} catch (PatternSyntaxException e) {
			System.out.println("The delimiter format " + delim + 
			    " is not a proper regular expression syntax.  See " + 
				"http://docs.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html" + 
			    " for allowable character patterns.");
				e.printStackTrace();
				return false;
		}
		setDelimiter(delim);
    }

    // handle the -f option
    if ( command.hasOption("f") ) {
    	if ( !command.hasOption("d") ) {
    		System.out.println("The -d' option is required to identify the " +
        	        "formats to be applied to date/time fields in the sample.");
        		return false;
    	}
    	List<Integer> dateFieldList = new ArrayList<Integer>();
    	String dateFields = command.getOptionValue("f");
    	String[] fields = dateFields.split(",");
    	for (String field : fields) {
    		try {
				Integer fieldIndex = new Integer(field);
				dateFieldList.add(fieldIndex);
				if ( field.equals("0") ) {
					System.out.println("The list of date fields must start " +
				        "with 1 (one), not 0 (zero).");
					return false;
				}
				
			} catch (NumberFormatException e) {
				System.out.println("The date field " + field + 
						" is not a proper field index number. Provide a " +
						"comma-separated list of numbers, not including zero, " +
						"that correspond to observation date, time, or datetime " +
						"fields in the sample data.  For instance, if the " +
						"observation timestamp was in column one of the sample, " +
						"just use '-f 1'. If column one holds the date, and column " +
						"two holds the time, use '-f 1,2'");

			}
    		setDateFields(dateFieldList);
    	}
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
   * A method that sets the delimiter between variables in a sample string
   * 
   * @param delim  the delimiter string as a Java regular expression
   */
  public void setDelimiter(String delim) {
	  this.delimiter = delim;
	  
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
    options.addOption("t", true,  "Timezone indicator (UTC, HST, EDT, etc.)");
    options.addOption("d", true,  "Date formats as a comma separated list(yyyy-MM-dd,HH:mm:ss)");
    options.addOption("D", true,  "Delimiter between variables as a regular expression (\",\", \"\\s+\")");
    options.addOption("f", true,  "Date fields as a one-based comma separated list(1,2)");
    options.addOption("p", true,  "RBNB Server Port Number");
                      
    return options;
  }

  /**
   * A method that starts the streaming of data lines from the source file to
   * the RBNB server.  
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
   * begins processing the data streaming from the source file.
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
   * A method that stops the streaming of data between the source file and
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
  
  /**
   * A method that gets the timezone string
   *
   * @return timezone  the timezone string (like "UTC", "HST", "PONT" etc.)
   */
  public String getTimezone() {
    return this.timezone;
    
  }
  
  /**
   * A method that sets the timezone string
   *
   * @param timezone  the timezone string (like "UTC", "HST", "PONT" etc.)
   */
  public void setTimezone(String timezone) {
    this.timezone = timezone;
    
  }

/**
 * Get the list of date formats that correspond to date and/or time fields in
 * the sample data.  For instance, column one of the sample may be a timestamp
 * formatted as "YYYY-MM-DD HH:MM:SS". Alternatively, columns one and two may
 * be a date and a time, such as "YYYY-MM-DD,HH:MM:SS". 
 * @return the dateFormats
 */
public List<String> getDateFormats() {
	return dateFormats;
	
}

/**
 * Set the list of date formats that correspond to date and/or time fields in
 * the sample data.  For instance, column one of the sample may be a timestamp
 * formatted as "YYYY-MM-DD HH:MM:SS". Alternatively, columns one and two may
 * be a date and a time, such as "YYYY-MM-DD,HH:MM:SS". 

 * @param dateFormats the dateFormats to set
 */
public void setDateFormats(List<String> dateFormats) {
	this.dateFormats = dateFormats;
	
}

/**
 * Get the list of Integers that correspond with field indices of the 
 * sample observation's date, time, or datetime columns.
 *
 * @return the dateFields
 */
public List<Integer> getDateFields() {
	return dateFields;
	
}

/**
 * Set the list of Integers that correspond with field indices of the 
 * sample observation's date, time, or datetime columns. The list must be
 * one-based, such as '1,2'.
 * @param dateFields the dateFields to set
 */
public void setDateFields(List<Integer> dateFields) {
	this.dateFields = dateFields;
	
}

/* return the sample observation date given minimal sample metadata */
public Date getSampleDate(String line) throws ParseException {
	
	/*
	 * Date time formats and field locations are highly dependent on instrument
	 * output settings.  The -d and -f options are used to set dateFormats and dateFields
	 * 
	 * this.dateFormats will look something like {"yyyy-MM-dd", "HH:mm:ss"} or 
	 * {"yyyy-MM-dd HH:mm:ss"} or {"yyyy", "MM", "dd", "HH", "mm", "ss"}
	 * 
	 * this.dateFields will also look something like {1,2} or {1} or {1, 2, 3, 4, 5, 6}
	 * 
	 * NS01 sample:
	 * # 26.1675,  4.93111,    0.695, 0.1918, 0.1163,  31.4138, 09 Dec 2012 15:46:55
	 * NS03 sample:
	 * #  25.4746,  5.39169,    0.401,  35.2570, 09 Dec 2012, 15:44:36
	 */
    // extract the date from the data line
	SimpleDateFormat dateFormat;
    String dateFormatStr = "";
    String dateString    = "";
    String[] columns     = line.trim().split(this.delimiter);
    logger.debug("Delimiter is: " + this.delimiter);
    logger.debug(Arrays.toString(columns));
    
    // build the total date format from the individual fields listed in dateFields
    int index = 0;
    for (Integer dateField : this.dateFields) {
    	dateFormatStr += this.dateFormats.get(index); //zero-based list
    	dateString    += columns[dateField.intValue() - 1].trim(); //zero-based list
    	index++;
    }
	logger.debug("Using date format string: " + dateFormatStr);
	logger.debug("Using date string       : " + dateString);

    this.tz = TimeZone.getTimeZone(this.timezone);
    if ( this.dateFormats == null || this.dateFields == null ) {
    	logger.warn("Using the defaault datetime field for sample data. " +
            "Use the -f and -d options to explicitly set date time fields.");
    	dateFormat = this.defaultDateFormat;
    }
    // init the date formatter
    dateFormat = new SimpleDateFormat(dateFormatStr);
	dateFormat.setTimeZone(this.tz);

	// parse the date string
    Date sampleDate = dateFormat.parse(dateString.trim());

    return sampleDate;
}

}
