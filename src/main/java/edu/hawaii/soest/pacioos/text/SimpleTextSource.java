/*
 *  Copyright: 2020 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: A class that provides properties and methods common 
 *             to all simple text-based source drivers within this
 *             package.
 *
 *   Authors: Christopher Jones
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

import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.SAPIException;
import com.rbnb.sapi.Sink;
import edu.hawaii.soest.pacioos.text.configure.Configuration;
import org.apache.commons.cli.Options;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nees.rbnb.RBNBSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * a class that represents a simple text-based instrument as a Source driver
 * for an RBNB DataTurbine streaming server.
 * 
 * @author cjones
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
    protected String delimiter;

    /* The record delimiter between separate ASCII sample lines (like \r\n) */
    protected String recordDelimiter;
    
    /* The default date formatter for the timestamp in the data sample string */
    protected DateTimeFormatter defaultDateFormatter;
    
    /* A list of date format patterns to be applied to designated date/time fields */
    protected List<String> dateFormats = null;

    /* A one-based list of Integers corresponding to the observation date/time field indices */
    protected List<Integer> dateFields = null;
    
    /* The instance of TimeZone to use when parsing dates */
    protected TimeZone tz;

    /* The pattern used to identify data lines */
    protected Pattern dataPattern;
    
    /* The timezone that the data samples are taken in as a string (UTC, HST, etc.) */
    protected String timezone;
    
    /* The state used to track the data processing */
    private int state = 0;
      
    /* A boolean indicating if we are ready to stream (connected) */
    private boolean readyToStream = false;
    
    /* The thread used for streaming data */
    protected Thread streamingThread;
    
    /* The polling interval (millis) used to check for new lines in the data file */
    protected int pollInterval;
    
    /*
     * An internal Thread setting used to specify how long, in milliseconds, the
     * execution of the data streaming Thread should wait before re-executing
     * 
     * @see execute()
     */
    private int retryInterval = 5000;

    /* The identifier of the instrument (e.g. NS01) */
    private String identifier;

    /* The configuration instance used to configure the class */
    private Configuration config;

    /* The list of record delimiter characters provided by the config (usually 0x0D,0x0A) */
    protected String[] recordDelimiters;

    /* The first record delimiter byte */
    protected byte firstDelimiterByte;

    /* The second record delimiter byte */
    protected byte secondDelimiterByte;
      
    /**
     * Constructor: create an instance of the SimpleTextSource
     * @param config a configuration instance
     */
    public SimpleTextSource(Configuration config) throws ConfigurationException {
        
        // Pull the general configuration from the properties file
        /* The properties configuration used to provide general settings */
        PropertiesConfiguration propsConfig = new PropertiesConfiguration("textsource.properties");
        this.archiveMode       = propsConfig.getString("textsource.archive_mode");
        this.rbnbChannelName   = propsConfig.getString("textsource.rbnb_channel");
        this.serverName        = propsConfig.getString("textsource.server_name ");
        this.delimiter         = propsConfig.getString("textsource.delimiter");
        this.pollInterval      = propsConfig.getInt("textsource.poll_interval");
        this.retryInterval     = propsConfig.getInt("textsource.retry_interval");
        this.defaultDateFormatter =
            DateTimeFormatter.ofPattern(propsConfig.getString("textsource.default_date_format"));

        
        // parse the record delimiter from the config file
        // set the XML configuration in the simple text source for later use
        this.setConfiguration(config);

        // Get the number of channels for the instrument
        int totalChannels = config.getTotalChannels();

        // set the common configuration fields
        this.setConnectionType(config.getConnectionType());
        this.setIdentifier(config.getIdentifier());
        this.setRBNBClientName(config.getClientName());
        this.setServerName(config.getServerName());
        this.setServerPort(config.getServerPort());
        this.setCacheSize(config.getArchiveMemory());
        this.setArchiveSize(config.getArchiveSize());

        // find the default channel with the ASCII data string
        for (int channelIndex = 0; channelIndex < totalChannels; channelIndex++) {
            boolean isDefaultChannel = config.isDefaultChannel(channelIndex);
            if ( isDefaultChannel ) {
                this.setChannelName(config.getChannelName(channelIndex));
                this.setPattern(config.getChannelDataPattern(channelIndex));
                String fieldDelimiter = config.getFieldDelimiter(channelIndex);
                // handle hex-encoded field delimiters
                if ( fieldDelimiter.startsWith("0x") || fieldDelimiter.startsWith("\\x" )) {
                    
                    byte delimBytes = Byte.parseByte(fieldDelimiter.substring(2), 16);
                    byte[] delimAsByteArray = new byte[]{delimBytes};
                    String delim = null;
                    delim = new String(
                        delimAsByteArray, 0,
                        delimAsByteArray.length,
                        StandardCharsets.US_ASCII);
                    this.setDelimiter(delim);
                    
                } else {
                    this.setDelimiter(fieldDelimiter);

                }
                this.setRecordDelimiters(config.getRecordDelimiters(channelIndex));
                // set the date formats list
                setDateFormats(config.listDateFormats(channelIndex));

                // set the date fields list
                List<Integer> dateFields = config.listDateFieldPositions(channelIndex);
                this.setTimezone(config.getTimeZoneID(channelIndex));
                this.setTz(TimeZone.getTimeZone(getTimezone()));
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
            throw new ConfigurationException("[" + getIdentifier() + "/" + getChannelName() + " ] " +
                "The recordDelimiter must be one or two characters, " +
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
        this.rbnbChannelName = channelName;
    }
    
    /**
     * Return the RBNB DataTurbine channel name for this text source
     * @return rbnbChannelName the RBNB channel name
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
     * @return connectionType the connection type
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

        while (retry) {

            // connect to the RBNB server
            if (connect()) {
                // run the data streaming code
                retry = !execute();
            }

            disconnect();

            if (retry) {
                try {
                    log.debug("[" + getIdentifier() + "/" + getChannelName() + " ] " +
                        "Sleeping " + retryInterval + " while retrying the connection.");
                    Thread.sleep(retryInterval);

                } catch (Exception e) {
                    log.info("[" + getIdentifier() + "/" + getChannelName() + " ] " +
                        "There was an execution problem. Retrying. Message is: " + e.getMessage());
                    if (log.isDebugEnabled()) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * A method that starts the streaming of data lines from the source file to
     * the RBNB server.
     */
    public boolean start() {

        // return false if the streaming is running
        if (isRunning()) {
            return false;
        }

        // reset the connection to the RBNB server
        if (isConnected()) {
            disconnect();
        }
        connect();

        // return false if the connection fails
        if (!isConnected()) {
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
        if (!isRunning()) {
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
     * @return identifier  the identifier of the instrument
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
     * @param timezone the timezone string (like "UTC", "HST", "PONT" etc.)
     */
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    /**
     * Get the TZ time zone
     * @return the time zone
     */
    public TimeZone getTz() {
        return tz;
    }

    /**
     * Set the TZ time zone
     * @param tz the time zone
     */
    public void setTz(TimeZone tz) {
        this.tz = tz;
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
    public Instant getSampleInstant(String line) throws ParseException {

        /* The datetime to return */
        ZonedDateTime sampleDateTime;
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
        DateTimeFormatter dateTimeFormatter;
        StringBuilder dateFormatStr = new StringBuilder();
        StringBuilder dateString = new StringBuilder();
        String[] columns     = line.trim().split(this.delimiter);
        log.debug("[" + getIdentifier() + "/" + getChannelName() + " ] " +
            "Delimiter is: " + this.delimiter);
        log.debug(Arrays.toString(columns));
        // build the total date format from the individual fields listed in dateFields
        int index = 0;
        if ( this.dateFields != null && this.dateFormats != null ) {
            for (Integer dateField : this.dateFields) {
                try {
                    dateFormatStr.append(this.dateFormats.get(index)); //zero-based list
                    dateString.append(columns[dateField - 1].trim()); //zero-based list
                } catch (IndexOutOfBoundsException e) {
                    String msg = "[" + getIdentifier() + "/" + getChannelName() + " ] " +
                        "There was an error parsing the date from the sample using the date format '"
                            + dateFormatStr
                            + "' and the date field index of "
                            + dateField;
                    if (log.isDebugEnabled()) {
                        e.printStackTrace();
                    }
                    throw new ParseException(msg, 0);
                }
                index++;
            }
            log.debug("[" + getIdentifier() + "/" + getChannelName() + " ] " +
                "Using date format string: " + dateFormatStr);
            log.debug("[" + getIdentifier() + "/" + getChannelName() + " ] " +
                "Using date string       : " + dateString);
            log.debug("[" + getIdentifier() + "/" + getChannelName() + " ] " +
                "Using time zone         : " + this.timezone);
            
            this.tz = TimeZone.getTimeZone(this.timezone);
            if ( this.dateFormats == null || this.dateFields == null ) {
                log.warn("[" + getIdentifier() + "/" + getChannelName() + " ] " +
                    "Using the default datetime field for sample data.");
                dateTimeFormatter = this.defaultDateFormatter;
            }
            // init the date formatter
            dateTimeFormatter = DateTimeFormatter.ofPattern(dateFormatStr.toString());
            dateTimeFormatter = dateTimeFormatter.withZone(this.tz.toZoneId());

            // parse the date string
            sampleDateTime = ZonedDateTime.parse(
                dateString.toString().trim(), dateTimeFormatter);
            log.debug("[" + getIdentifier() + "/" + getChannelName() + " ] " +
                "Using sample instant    : " + sampleDateTime.toInstant().toString());
            return sampleDateTime.toInstant();
        } else {
            log.warn("[" + getIdentifier() + "/" + getChannelName() + " ] " +
                "No date formats or date fields were configured. Using the current local " +
                "date for this sample.");
        }

        // Fallback to the current local instant if the ZonedDateTime parsing fails
        return Instant.now();
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
     * Set the configuration object for this simple text source
     * 
     * @param config  the configuration instance
     */
    public void setConfiguration(Configuration config) {
        this.config = config;
        
    }

    /**
     * Return the configuration for this simple text source
     * 
     * @return config  the configuration instance
     */
    public Configuration getConfiguration(){
        return this.config;
        
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
     * Register the default channel name in the Data Turbine
     * @return true if the channel is registered
     * @throws SAPIException an SAPI exception
     */
    public boolean registerChannel() throws SAPIException {
        
            boolean registered = false;
            ChannelMap registerChannelMap = new ChannelMap(); // used only to register channels
        
        // add the DecimalASCIISampleData channel to the channelMap
        int channelIndex = registerChannelMap.Add(getChannelName());
        registerChannelMap.PutUserInfo(channelIndex, "units=none");                             
        // and register the RBNB channels
        getSource().Register(registerChannelMap);
        registerChannelMap.Clear();
        registered = true;
        
        return registered;

    }
    
    /**
     * For the given Data Turbine channel, request the last sample timestamp
     * for comparison against samples about to be flushed
     * @return lastSampleTimeAsSecondsSinceEpoch  the last sample time as seconds since the epoch
     * @throws SAPIException an SAPI exception
     */
    public long getLastSampleTime() throws SAPIException {
        
            // query the DT to find the timestamp of the last sample inserted
        Sink sink = new Sink();
        long lastSampleTimeAsSecondsSinceEpoch = Instant.now().getEpochSecond();
        
        try {
            ChannelMap requestMap = new ChannelMap();
            int entryIndex = requestMap.Add(getRBNBClientName() + "/" + getChannelName());
            log.debug("[" + getIdentifier() + "/" + getChannelName() + " ] " +
                "Request Map: " + requestMap.toString());
            sink.OpenRBNBConnection(getServer(), "lastEntrySink");
            
            sink.Request(requestMap, 0., 1., "newest");
            ChannelMap responseMap = sink.Fetch(5000); // get data within 5 seconds 
            // initialize the last sample date 
            log.debug("[" + getIdentifier() + "/" + getChannelName() + " ] " +
                "Initialized the last sample date to: " + Instant.ofEpochSecond(lastSampleTimeAsSecondsSinceEpoch));
            log.debug("[" + getIdentifier() + "/" + getChannelName() + " ] " +
                "The last sample date as a long is: " + lastSampleTimeAsSecondsSinceEpoch);
            
            if ( responseMap.NumberOfChannels() == 0 )    {
                // set the last sample time to 0 since there are no channels yet
                lastSampleTimeAsSecondsSinceEpoch = 0L;
                log.debug("[" + getIdentifier() + "/" + getChannelName() + " ] " +
                    "Resetting the last sample date to the epoch: " + Instant.ofEpochSecond(0));
            
            } else if ( responseMap.NumberOfChannels() > 0 )    {
                lastSampleTimeAsSecondsSinceEpoch = 
                    new Double(responseMap.GetTimeStart(entryIndex)).longValue();
                log.debug("[" + getIdentifier() + "/" + getChannelName() + " ] " +
                    "There are existing channels. Last sample time: " +
                        Instant.ofEpochSecond(lastSampleTimeAsSecondsSinceEpoch));
                                         
            }
        } finally {
            sink.CloseRBNBConnection();
        }
        return lastSampleTimeAsSecondsSinceEpoch;
    }

    /**
     * Send the sample to the DataTurbine
     *
     * @param sample the ASCII sample string to send
     * @throws IOException an IO exception
     * @throws SAPIException an SAPI exception
     */
    public int sendSample(String sample) throws IOException, SAPIException {
        int numberOfChannelsFlushed = 0;
        long sampleTimeAsSecondsSinceEpoch;

        // add a channel of data that will be pushed to the server.  
        // Each sample will be sent to the Data Turbine as an rbnb frame.
        ChannelMap rbnbChannelMap = new ChannelMap();
        try {
            Instant sampleInstant = getSampleInstant(sample);
            sampleTimeAsSecondsSinceEpoch = sampleInstant.getEpochSecond();

        } catch (ParseException e) {
            log.warn("[" + getIdentifier() + "/" + getChannelName() + " ] " +
                "A sample date couldn't be parsed from the sample.  Using the current local date." +
                " the error message was: " + e.getMessage());
            Instant sampleInstant = Instant.now();
            sampleTimeAsSecondsSinceEpoch = sampleInstant.getEpochSecond();
        }


        // send the sample to the data turbine
        rbnbChannelMap.PutTime((double) sampleTimeAsSecondsSinceEpoch, 0d);
        int channelIndex = rbnbChannelMap.Add(getChannelName());
        rbnbChannelMap.PutMime(channelIndex, "text/plain");

        // Add the delimiters back on to the sample after they were trimmed off
        StringBuilder sampleBuilder = new StringBuilder(sample);
        for (int i = 0; i < getRecordDelimiters().length; i++) {
            sampleBuilder.append(
                new String(new byte[]{Integer.decode(getRecordDelimiters()[i]).byteValue()})
            );
        }
        sample = sampleBuilder.toString();
        rbnbChannelMap.PutDataAsString(channelIndex, sample);

        try {
            numberOfChannelsFlushed = getSource().Flush(rbnbChannelMap);
            log.info("[" + getIdentifier() + "/" + getChannelName() + " ] " +
                " sample: " + sample.trim() + " sent data to the DataTurbine.");
            rbnbChannelMap.Clear();
            // in the event we just lost the network, sleep, try again
            while (numberOfChannelsFlushed < 1) {
                log.debug("[" + getIdentifier() + "/" + getChannelName() + " ] " +
                    "No channels flushed, trying again in 10 seconds.");
                Thread.sleep(10000L);
                numberOfChannelsFlushed = getSource().Flush(rbnbChannelMap, true);

            }

        } catch (InterruptedException e) {
            log.debug("[" + getIdentifier() + "/" + getChannelName() + " ] " +
                "The call to Source.Flush() was interrupted for " + getSource().GetClientName());
            e.printStackTrace();

        }


        return numberOfChannelsFlushed;
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
            
              log.warn("[" + getIdentifier() + "/" + getChannelName() + " ] " +
                  "The sample did not validate, and was not sent. The text was: " +
            sampleAsReadableText);
        }

        if ( log.isTraceEnabled() ) {
            log.trace("[" + getIdentifier() + "/" + getChannelName() + " ] " +
                "Sample bytes: " +
                new String(Hex.encodeHex(sample.getBytes(StandardCharsets.US_ASCII))));
            log.trace("[" + getIdentifier() + "/" + getChannelName() + " ] " +
                "Data pattern is: '" + this.dataPattern.toString() + "'");
            log.trace("[" + getIdentifier() + "/" + getChannelName() + " ] " +
                "Sample is      :  " + sample);
        }
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
