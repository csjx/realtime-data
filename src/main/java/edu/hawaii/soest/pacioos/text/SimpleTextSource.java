/**
 *  Copyright: 2013 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: A class that provides properties and methods common 
 *             to all simple text-based source drivers within this
 *             package.
 *
 *   Authors: Christopher Jones
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
package edu.hawaii.soest.pacioos.text;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.Options;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nees.rbnb.RBNBSource;

import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.SAPIException;

/**
 * a class that represents a simple text-based instrument as a Source driver
 * for an RBNB DataTurbine streaming server.
 * 
 * @author cjones
 *
 */
public abstract class SimpleTextSource extends RBNBSource {

    private final Log log = LogFactory.getLog(SimpleTextSource.class);
    
    /*
     * The mode in which the source interacts with the RBNB archive. Valid modes 
     * include 'append', 'create', 'load' and 'none'.
     *
     * @see setArchiveMode()
     * @see getArchiveMode()
     */
    private String archiveMode;

    /* The name of the RBNB channel for this data stream */
    private String rbnbChannelName;
    
    /* The IP address or DNS name of the RBNB server */
    private String serverName;
    
    /* The default TCP port of the RBNB server */
    private int serverPort;
    
    /* The address and port string for the RBNB server */
    private String server = serverName + ":" + serverPort;

    /* The driver connection type (file, socket, or serial) */
    private String connectionType;

    /* The delimiter separating variables in the sample line */
    private String delimiter;

    /* The record delimiter between separate ASCII sample lines (like \r\n) */
    private String recordDelimiter;
    
    /* The default date format for the timestamp in the data sample string */
    private SimpleDateFormat defaultDateFormat;
    
    /* A list of date format patterns to be applied to designated date/time fields */
    private List<String> dateFormats = null;

    /* A one-based list of Integers corresponding to the observation date/time field indices */
    private List<Integer> dateFields = null;
    
    /* The instance of TimeZone to use when parsing dates */
    private TimeZone tz;

    /* The pattern used to identify data lines */
    private Pattern dataPattern;
    
    /* The timezone that the data samples are taken in as a string (UTC, HST, etc.) */
    private String timezone;
    
    /* The state used to track the data processing */
    private int state = 0;
      
    /* A boolean indicating if we are ready to stream (connected) */
    private boolean readyToStream = false;
    
    /* The thread used for streaming data */
    private Thread streamingThread;
    
    /* The polling interval (millis) used to check for new lines in the data file */
    private int pollInterval;
    
    /*
     * An internal Thread setting used to specify how long, in milliseconds, the
     * execution of the data streaming Thread should wait before re-executing
     * 
     * @see execute()
     */
    private int retryInterval = 5000;

    /* The identifier of the instrument (e.g. NS01) */
    private String identifier;

    /* The XML-based configuration instance used to configure the class */
    private XMLConfiguration xmlConfig;

    /* The list of record delimiter characters provided by the config (usually 0x0D,0x0A) */
    private String[] recordDelimiters;

    /* The first record delimiter byte */
    private byte firstDelimiterByte;

    /* The second record delimiter byte */
    private byte secondDelimiterByte;
      
    /**
     * Constructor: create an instance of the simple SimpleTextSource
     * @param xmlConfig 
     */
    public SimpleTextSource(XMLConfiguration xmlConfig) throws ConfigurationException {
        
        this.xmlConfig = xmlConfig;
        // Pull the general configuration from the properties file
        Configuration config   = new PropertiesConfiguration("pacioos.properties");
        this.archiveMode       = config.getString("pacioos.textsource.archive_mode");
        this.rbnbChannelName   = config.getString("pacioos.textsource.rbnb_channel");
        this.serverName        = config.getString("pacioos.textsource.server_name ");
        this.delimiter         = config.getString("pacioos.textsource.delimiter");
        this.pollInterval      = config.getInt("pacioos.textsource.poll_interval");
        this.retryInterval     = config.getInt("pacioos.textsource.retry_interval");
        this.defaultDateFormat = new SimpleDateFormat(
            config.getString("pacioos.textsource.default_date_format"));

        
        // parse the record delimiter from the config file
        // set the XML configuration in the simple text source for later use
        this.setConfiguration(xmlConfig);
        
        // set the common configuration fields
        String connectionType = this.xmlConfig.getString("connectionType");
        this.setConnectionType(connectionType);
        String channelName = xmlConfig.getString("channelName");
        this.setChannelName(channelName);
        String identifier = xmlConfig.getString("identifier");
        this.setIdentifier(identifier);
        String rbnbName = xmlConfig.getString("rbnbName");
        this.setRBNBClientName(rbnbName);
        String rbnbServer = xmlConfig.getString("rbnbServer");
        this.setServerName(rbnbServer);
        int rbnbPort = xmlConfig.getInt("rbnbPort");
        this.setServerPort(rbnbPort);
        int archiveMemory = xmlConfig.getInt("archiveMemory");
        this.setCacheSize(archiveMemory);
        int archiveSize = xmlConfig.getInt("archiveSize");
        this.setArchiveSize(archiveSize);
        
        // set the default channel information 
        Object channels = xmlConfig.getList("channels.channel.name");
        int totalChannels = 1;
        if ( channels instanceof Collection) {
            totalChannels = ((Collection<?>) channels).size();
            
        }        
        // find the default channel with the ASCII data string
        for (int i = 0; i < totalChannels; i++) {
            boolean isDefaultChannel = xmlConfig.getBoolean("channels.channel(" + i + ")[@default]");
            if ( isDefaultChannel ) {
                String name = xmlConfig.getString("channels.channel(" + i + ").name");
                this.setChannelName(name);
                String dataPattern = xmlConfig.getString("channels.channel(" + i + ").dataPattern");
                this.setPattern(dataPattern);
                String fieldDelimiter = xmlConfig.getString("channels.channel(" + i + ").fieldDelimiter");
                // handle hex-encoded field delimiters
                if ( fieldDelimiter.startsWith("0x") || fieldDelimiter.startsWith("\\x" )) {
                    
                    Byte delimBytes = Byte.parseByte(fieldDelimiter.substring(2), 16);
                    byte[] delimAsByteArray = new byte[]{delimBytes.byteValue()};
                    String delim = null;
                    try {
                        delim = new String(delimAsByteArray, 0, delimAsByteArray.length, "ASCII");
                        
                    } catch (UnsupportedEncodingException e) {
                        throw new ConfigurationException("There was an error parsing the field delimiter." +
                            " The message was: " + e.getMessage());
                    }
                    this.setDelimiter(delim);
                    
                } else {
                    this.setDelimiter(fieldDelimiter);

                }
                String[] recordDelimiters = xmlConfig.getStringArray("channels.channel(" + i + ").recordDelimiters");
                this.setRecordDelimiters(recordDelimiters);
                // set the date formats list
                List<String> dateFormats = (List<String>) xmlConfig.getList("channels.channel(" + i + ").dateFormats.dateFormat");
                if ( dateFormats.size() != 0 ) {
                    for (String dateFormat : dateFormats) {
                        
                        // validate the date format string
                        try {
                            SimpleDateFormat format = new SimpleDateFormat(dateFormat);
                            
                        } catch (IllegalFormatException ife) {
                            String msg = "There was an error parsing the date format " +
                                dateFormat + ". The message was: " + ife.getMessage();
                            if ( log.isDebugEnabled() ) {
                                ife.printStackTrace();
                            }
                            throw new ConfigurationException(msg);
                        }
                    }
                    setDateFormats(dateFormats);
                } else {
                    log.warn("No date formats have been configured for this instrument.");
                }
                
                // set the date fields list
                List<String> dateFieldList = xmlConfig.getList("channels.channel(" + i + ").dateFields.dateField");
                List<Integer> dateFields = new ArrayList<Integer>();
                if ( dateFieldList.size() != 0 ) {
                    for ( String dateField : dateFieldList ) {
                        try {
                            Integer newDateField = new Integer(dateField);
                            dateFields.add(newDateField);
                        } catch (NumberFormatException e) {
                            String msg = "There was an error parsing the dateFields. The message was: " +
                                    e.getMessage();
                            throw new ConfigurationException(msg);
                        }
                    }
                    setDateFields(dateFields);
                    
                } else {
                    log.warn("No date fields have been configured for this instrument.");
                }
                String timeZone = xmlConfig.getString("channels.channel(" + i + ").timeZone");
                this.setTimezone(timeZone);
                break;
            }
            
        }

        // Check the record delimiters length and set the first and optionally second delim characters
        if ( this.recordDelimiters.length == 1 ) {
            this.firstDelimiterByte = (byte) Integer.decode(this.recordDelimiters[0]).byteValue();
        } else if ( this.recordDelimiters.length == 2 ) {
            this.firstDelimiterByte  = (byte) Integer.decode(this.recordDelimiters[0]).byteValue();
            this.secondDelimiterByte = (byte) Integer.decode(this.recordDelimiters[1]).byteValue();

        } else {
            throw new ConfigurationException("The recordDelimiter must be one or two characters, " +
                "separated by a pipe symbol (|) if there is more than one delimiter character.");
        }
        byte[] delimiters = new byte[]{};

    }
    
    /**
     * Set the name of the RBNB DataTurbine channel for this text source
     * 
     * @param channelName  the name of the channel
     */
    public void setChannelName(String channelName) {
        
    }
    
    /**
     * Return the RBNB DataTurbine channel name for this text source
     * @return
     */
    public String getChannelName() {
        return this.rbnbChannelName;
    
    }
    
    /**
     * Set the connection for this text source
     * 
     * @param connectionType  the connection type ('file', 'socket', or 'serial')
     */
    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }
    
    /**
     * Return the connection type for this text source
     * @return
     */
    public String getConnectionType() {
        return this.connectionType;
    
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
            Thread.sleep(retryInterval);
            
          } catch ( Exception e ){
            log.info("There was an execution problem. Retrying. Message is: " + e.getMessage());
            if ( log.isDebugEnabled() ) {
                e.printStackTrace();
            }
            
          }
        }
      }
      // stop the streaming when we are done
      stop();
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
     * A method that executes the reading of data from the instrument to the RBNB
     * server after all configuration of settings, connections to hosts, and
     * thread initializing occurs.  This method contains the detailed code for 
     * reading and interpreting the streaming data. this method must be implemented by
     * a connection-specific source class.
     */
    protected abstract boolean execute();

    /**
     * A method that starts the connection with the RBNB DataTurbine. Used as a public wrapper
     * method to RBNBSource.connect(), which has protected scope.
     */
    public boolean startConnection() {
      return connect();
    }
     
    /**
     * A method that stops the connection with the RBNB DataTurbine. Used as a public wrapper
     * method to RBNBSource.disconnect(), which has protected scope.
     */
    public void stopConnection() {
      disconnect();
    }

    /**
     * Set the identifier of the instrument, usually a string like "NS01".
     * 
     * @param identifier  the identifier of the instrument
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
     
    /**
     * Set the identifier of the instrument, usually a string like "NS01".
     * 
     * @param identifier  the identifier of the instrument
     */
    public String getIdentifier() {
        return this.identifier;
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
     * a method that gets the field delimiter set for the data sample
     * @return the delimiter
     */
    public String getDelimiter() {
        return delimiter;
    }

    /**
     * Sets the delimiter used in the data sample (e.g. ',')
     * 
     * @param delimiter the delimiter to set
     */
    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    /**
     * a method that gets data pattern for the data sample
     * 
     * @return dataPattern  the pattern as a string
     */
    public String getPattern() {
        return this.dataPattern.toString();
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

    /**
     *  return the sample observation date given minimal sample metadata
     */
    public Date getSampleDate(String line) throws ParseException {
        
        /*
         * Date time formats and field locations are highly dependent on instrument
         * output settings.  The -d and -f options are used to set dateFormats and dateFields,
         * or in the XML-based configuration file
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
        log.debug("Delimiter is: " + this.delimiter);
        log.debug(Arrays.toString(columns));
        Date sampleDate = new Date();
        // build the total date format from the individual fields listed in dateFields
        int index = 0;
        if ( this.dateFields != null && this.dateFormats != null ) {
            for (Integer dateField : this.dateFields) {
                try {
                    dateFormatStr += this.dateFormats.get(index); //zero-based list
                    dateString += columns[dateField.intValue() - 1].trim(); //zero-based list
                } catch (IndexOutOfBoundsException e) {
                    String msg = "There was an error parsing the date from the sample using the date format '"
                            + dateFormatStr
                            + "' and the date field index of "
                            + dateField.intValue();
                    if (log.isDebugEnabled()) {
                        e.printStackTrace();
                    }
                    throw new ParseException(msg, 0);
                }
                index++;
            }
            log.debug("Using date format string: " + dateFormatStr);
            log.debug("Using date string       : " + dateString);
            log.debug("Using time zone         : " + this.timezone);
            
            this.tz = TimeZone.getTimeZone(this.timezone);
            if ( this.dateFormats == null || this.dateFields == null ) {
                log.warn("Using the default datetime field for sample data.");
                dateFormat = this.defaultDateFormat;
            }
            // init the date formatter
            dateFormat = new SimpleDateFormat(dateFormatStr);
            dateFormat.setTimeZone(this.tz);
            
            // parse the date string
            sampleDate = dateFormat.parse(dateString.trim());
        } else {
            log.info("No date formats or date fields were configured. Using the current date for this sample.");
        }

        return sampleDate;
    }

    /**
     * Return the record delimiters string array that separates sample lines of data
     * 
     * @return recordDelimiters - the record delimiters array between samples
     */
    public String[] getRecordDelimiters() {
        return this.recordDelimiters;
        
    }

    /**
     * Set the record delimiters string array that separates sample lines of data
     * 
     * @param recordDelimiters  the record delimiters array between samples
     */
    public void setRecordDelimiters(String[] recordDelimiters) {
        this.recordDelimiters = recordDelimiters;
        
    }

    /**
     * Set the XML configuration object for this simple text source
     * 
     * @param xmlConfig  the XML configuration instance
     */
    public void setConfiguration(XMLConfiguration xmlConfig) {
        this.xmlConfig = xmlConfig;
        
    }

    /**
     * Return the XML configuration for this simple text source
     * 
     * @return xmlConfig  the XML configuration instance
     */
    public XMLConfiguration getConfiguration(){
        return this.xmlConfig;
        
    }
    
    /**
     * A method that sets the command line options for this class.  This method 
     * calls the <code>RBNBSource.setBaseOptions()</code> method in order to set
     * properties such as the sourceHostName, sourceHostPort, serverName, and
     * serverPort.
     * 
     * Note: We no longer use command-line options for the drivers.  Instead, configuration
     * is set using an XML-based configuration file.
     */
    @Override
    protected Options setOptions() {
        
        //Options options = setBaseOptions(new Options());
        Options options = new Options();
      
        options.addOption("h", true,  
            "The SimpleTextSource driver is used to connect to file, serial, or socket" +
            "-based instruments.  To configure an instrument, provide the path to the XML" +
            "-based configuration file.  See the driver documentation for creating the " +
            "configuration file");
                        
        return options;
    }

    /**
     * Send the sample to the DataTurbine
     * 
     * @param sample the ASCII sample string to send
     * @throws IOException
     * @throws SAPIException
     */
    public boolean sendSample(String sample) throws IOException, SAPIException {
        boolean sent = false;
        
        // add a channel of data that will be pushed to the server.  
        // Each sample will be sent to the Data Turbine as an rbnb frame.
        ChannelMap rbnbChannelMap = new ChannelMap();
        Date sampleDate = new Date();
        try {
            sampleDate = getSampleDate(sample);
            
        } catch (ParseException e) {
            log.warn("A sample date couldn't be parsed from the sample.  Using the current date." +
                " the error message was: " + e.getMessage());
        }
        long sampleTimeAsSecondsSinceEpoch = (sampleDate.getTime()/1000L);
        
        // send the sample to the data turbine
        rbnbChannelMap.PutTime((double) sampleTimeAsSecondsSinceEpoch, 0d);
        int channelIndex = rbnbChannelMap.Add(getChannelName());
        rbnbChannelMap.PutMime(channelIndex, "text/plain");
        rbnbChannelMap.PutDataAsString(channelIndex, sample);
        getSource().Flush(rbnbChannelMap);
        log.info("Sample: " + sample.substring(0, sample.length() - this.recordDelimiters.length) + 
                 " sent data to the DataTurbine.");
        
        sent = true;
        rbnbChannelMap.Clear();                      

        return sent;
    }

    /**
     * Validate the sample string against the sample pattern provided in the configuration
     * 
     * @param sample  the sample string to validate
     * @return isValid  true if the sample string match the sample pattern
     */
    public boolean validateSample(String sample) {
        boolean isValid = false;

        // test the line for the expected data pattern
        Matcher matcher = this.dataPattern.matcher(sample);

        if ( matcher.matches() ) {
            isValid = true;
        } else {
            String sampleAsReadableText = sample.replaceAll("\\x0D", "0D");
            sampleAsReadableText = sampleAsReadableText.replaceAll("\\x0A", "0A");
            
              log.warn("The sample did not validate, and was not sent. The text was: " +
            sampleAsReadableText);
        }
        
        try {
            log.debug("Sample bytes: " + new String(Hex.encodeHex(sample.getBytes("US-ASCII"))));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        log.debug("Data pattern is: '" + this.dataPattern.toString() + "'");
        log.debug("Sample is      :  " + sample);
        
        return isValid;
        
    }
    
    /**
     * Return the first delimiter character as a byte
     */
    public byte getFirstDelimiterByte() {
        return this.firstDelimiterByte;
    }

    /**
     * Return the second delimiter character as a byte
     */
    public byte getSecondDelimiterByte() {
        return this.secondDelimiterByte;
    }

}
