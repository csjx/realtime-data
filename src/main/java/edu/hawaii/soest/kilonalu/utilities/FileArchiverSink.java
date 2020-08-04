/*
 * RDV
 * Real-time Data Viewer
 * http://it.nees.org/software/rdv/
 *
 * Copyright (c) 2005-2007 University at Buffalo
 * Copyright (c) 2005-2007 NEES Cyberinfrastructure Center
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package edu.hawaii.soest.kilonalu.utilities;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;

import org.nees.rbnb.MarkerUtilities;
import org.nees.rbnb.RBNBBase;
import org.nees.rbnb.RBNBUtilities;
import org.nees.rbnb.TimeProgressListener;
import org.nees.rbnb.TimeRange;

import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.SAPIException;
import com.rbnb.sapi.Sink;
import com.rbnb.sapi.ChannelTree.Node;

/**
 * This class grabs data from an RBNB data source and saves it to a
 * directory structure where the data for the time stamp
 * yyyy-MM-dd:hh:mm:ss.nnn is saved to the file prefix_yyyyMMddhhmmssnnn.dat on the
 * directory path base-dir/yyyy/MM/dd/[hh/mm/]. The spliting of files to directory
 * structures is done to assure that no directory overflows its index table.
 *
 * @author Terry E. Weymouth
 * @author Moji Soltani
 * @author Jason P. Hanley
 * @author Christopher Jones
 */
public class FileArchiverSink extends RBNBBase {

    /** The Logger instance used to log system messages */
    private static Logger logger = Logger.getLogger(FileArchiverSink.class);

    /** the default RBNB sink name */
    private static final String DEFAULT_SINK_NAME = "FileArchiver";

    /** the default RBNB source name */
    private static final String DEFAULT_SOURCE_NAME = "KN0101_010ADCP010R00";

    /** the default RBNB channel name */
    private static final String DEFAULT_CHANNEL_NAME = "BinaryPD0EnsembleData";

    /** the default File prefix for archived filenames */
    private static final String DEFAULT_FILE_PREFIX = "KNXXXX_XXXADCPXXXRXX_";

    /** the File prefix for archived filenames */
    private String filePrefix = DEFAULT_FILE_PREFIX;

    /** the default File extension for archived filenames */
    private static final String DEFAULT_FILE_EXTENSION = ".10.1.dat";

    /** the File extension for archived filenames */
    private String fileExtension = DEFAULT_FILE_EXTENSION;

    /**
     * The archive interval used to periodically archive data (in seconds)
     */
    private int archiveInterval;

    /**
     * the default File path depth archived file directory paths.  The should
     * be one a SimpleDateFormat object of yyyy, MM, dd, HH, or mm.  It determines
     * if files are archived in directories down to Year, Month, Day, Hour,
     * or Minute.
     */
    private static final SimpleDateFormat DEFAULT_FILE_PATH_DEPTH =
        new SimpleDateFormat("dd");

    private SimpleDateFormat filePathDepth = DEFAULT_FILE_PATH_DEPTH;
    /** the RBNB sink */
    private final Sink sink;

    /** the RBNB sink name */
    private String sinkName = DEFAULT_SINK_NAME;

    /** the RBNB source name */
    private String sourceName = DEFAULT_SOURCE_NAME;

    /** the RBNB channel name */
    private String channelName = DEFAULT_CHANNEL_NAME;

    /** the full RBNB channel path */
    private String channelPath = sourceName + "/" + channelName;

    /** the start time for data export */
    private double startTime = 0.0;

    /** the end time for data export */
    private double endTime = Double.MAX_VALUE;

    /** the Calendar representation of the FileArchiver start time **/
    private static Calendar endArchiveCal;

    /** the Calendar representation of the FileArchiver start time **/
    private static Calendar beginArchiveCal;

    /** the event marker filter string */
    private String eventMarkerFilter;

    /** the list of time ranges to export */
    private List<TimeRange> timeRanges = new ArrayList<>();

    /** the default directory to archive to */
    public static final File DEFAULT_ARCHIVE_DIRECTORY = new File("/data/rbnb");

    /** the directory to archive to */
    private File archiveDirectory = DEFAULT_ARCHIVE_DIRECTORY;

    /** a flag to indicate if we are connected to the RBNB server or not */
    private boolean connected = false;

    /** a flag to control the export process */
    private boolean doExport = false;

    /** a list of registered progress listeners */
    private List<TimeProgressListener> listeners = new ArrayList<TimeProgressListener>();

    /** the duration of all the data to be exported */
    private double duration;

    /** number of seconds to go back from now to set a start time */
    private int secondsResetStart;

    /**
     * Constructor: creates FileArchiverSink.
     */
    public FileArchiverSink() {
        super();
        sink = new Sink();
    }

    /**
     * Runs FileArchiverSink.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        // Set up a simple logger that logs to the console
        BasicConfigurator.configure();

        final FileArchiverSink fileArchiverSink = new FileArchiverSink();

        if ( fileArchiverSink.parseArgs(args) ) {

            setupShutdownHook(fileArchiverSink);
            setupProgressListener(fileArchiverSink);

            // archive data on a schedule
            if ( fileArchiverSink.getArchiveInterval() > 0 ) {
                // override the command line start and end times
                fileArchiverSink.setupArchiveTime();

                TimerTask archiveData = new TimerTask() {
                    public void run() {
                        logger.debug("TimerTask.run() called.");

                        if ( fileArchiverSink.validateSetup() ) {
                            fileArchiverSink.export();
                            fileArchiverSink.setupArchiveTime();
                        }
                    }
                };

                Timer archiveTimer = new Timer();
                // run the archiveData timer task on the hour, every hour (or every day)
                archiveTimer.scheduleAtFixedRate(archiveData,
                    endArchiveCal.getTime(), fileArchiverSink.getArchiveInterval() * 1000);

            // archive data once based on the start and end times
            } else {
                fileArchiverSink.export();

            }
        }
    }

    /**
     * A method that initializes time variables for the File Archiver class.  For
     * now, it overrides the start and end times provided on the command line
     * and rolls the end time forward to be on the 2-minute (debug mode), hour, or day
     * and sets the start time to be one hour prior.  This results in hourly data files written
     * on the hour or daily files written on the day.
     */
    public void setupArchiveTime() {
        logger.debug("FileArchiverSink.setupArchiveTime() called.");

        // remove the time ranges assumed from the command line args
        getTimeRanges().clear();

        long eTime; // intermediate end time variable
        Date sDate; // intermediate start date variable
        long sTime; // intermediate start time variable

        if ( getArchiveInterval() == 120 ) {

            // set the execution time to be every two minutes (debug mode)
            endArchiveCal = Calendar.getInstance();
            endArchiveCal.clear(Calendar.MILLISECOND);
            endArchiveCal.clear(Calendar.SECOND);
            endArchiveCal.add(Calendar.MINUTE, 2);

            eTime = (endArchiveCal.getTime()).getTime();
            endTime = ((double) eTime) / 1000.0;

            /* the Calendar representation of the FileArchiver begin time **/
            beginArchiveCal = (Calendar) endArchiveCal.clone();

            // set the begin time of the duration 2 minutes prior to the execution time
            beginArchiveCal.add(Calendar.MINUTE, -2);
            endArchiveCal.add(Calendar.SECOND, -1);
            sDate = beginArchiveCal.getTime();
            sTime = sDate.getTime();
            startTime = ((double) sTime) / 1000.0;
            logger.debug("Next archive time will be " + endArchiveCal.getTime().toString());
            logger.debug("Archive begin time will be " + beginArchiveCal.getTime().toString());

        // schedule hourly on the hour
        } else if ( getArchiveInterval() == 3600 ) {
            // set the execution time to be on the upcoming hour.  Add a minute to
            // now() to be sure the next interval is in the next hour
            endArchiveCal = Calendar.getInstance();
            endArchiveCal.add(Calendar.MINUTE, 1);
            endArchiveCal.clear(Calendar.MILLISECOND);
            endArchiveCal.clear(Calendar.SECOND);
            endArchiveCal.clear(Calendar.MINUTE);
            endArchiveCal.add(Calendar.HOUR_OF_DAY, 1);

            eTime = (endArchiveCal.getTime()).getTime();
            endTime = ((double) eTime) / 1000.0;

            /* the Calendar representation of the FileArchiver begin time **/
            beginArchiveCal = (Calendar) endArchiveCal.clone();

            // set the begin time of the duration 1 hour prior to the execution time
            beginArchiveCal.add(Calendar.HOUR_OF_DAY, -1);
            endArchiveCal.add(Calendar.SECOND, -1);
            sDate = beginArchiveCal.getTime();
            sTime = sDate.getTime();
            startTime = ((double) sTime) / 1000.0;
            logger.debug("Next archive time will be " + endArchiveCal.getTime().toString());
            logger.debug("Archive begin time will be " + beginArchiveCal.getTime().toString());

        // else schedule daily on the day
        } else if ( getArchiveInterval() == 86400 ) {
            // set the execution time to be on the upcoming day
            endArchiveCal = Calendar.getInstance();
            endArchiveCal.add(Calendar.MINUTE, 1);
            endArchiveCal.clear(Calendar.MILLISECOND);
            endArchiveCal.clear(Calendar.SECOND);
            endArchiveCal.clear(Calendar.MINUTE);
            endArchiveCal.set(Calendar.HOUR_OF_DAY, 0);
            endArchiveCal.add(Calendar.DATE, 1);

            eTime = (endArchiveCal.getTime()).getTime();
            endTime = ((double) eTime) / 1000.0;

            /* the Calendar representation of the FileArchiver begin time **/
            beginArchiveCal = (Calendar) endArchiveCal.clone();

            // set the begin time of the duration 1 day prior to the execution time
            beginArchiveCal.add(Calendar.DATE, -1);
            endArchiveCal.add(Calendar.SECOND, -1);
            sDate = beginArchiveCal.getTime();
            sTime = sDate.getTime();
            startTime = ((double) sTime) / 1000.0;
            logger.debug("Next archive time will be " + endArchiveCal.getTime().toString());
            logger.debug("Archive begin time will be " + beginArchiveCal.getTime().toString());

            // else schedule weekly on the day
            } else if ( getArchiveInterval() == 604800 ) {
                // set the execution time to be on the upcoming hour
                endArchiveCal = Calendar.getInstance();
                endArchiveCal.add(Calendar.MINUTE, 1);
                endArchiveCal.clear(Calendar.MILLISECOND);
                endArchiveCal.clear(Calendar.SECOND);
                endArchiveCal.clear(Calendar.MINUTE);
                endArchiveCal.set(Calendar.HOUR_OF_DAY, 0);
                endArchiveCal.add(Calendar.DATE, 7);

                eTime = (endArchiveCal.getTime()).getTime();
                endTime = ((double) eTime) / 1000.0;

                /* the Calendar representation of the FileArchiver begin time **/
                beginArchiveCal = (Calendar) endArchiveCal.clone();

                // set the begin time of the duration 1 day prior to the execution time
                beginArchiveCal.add(Calendar.DATE, -7);
                endArchiveCal.add(Calendar.SECOND, -1);
                sDate = beginArchiveCal.getTime();
                sTime = sDate.getTime();
                startTime = ((double) sTime) / 1000.0;
                logger.debug("Next archive time will be " + endArchiveCal.getTime().toString());
                logger.debug("Archive begin time will be " + beginArchiveCal.getTime().toString());

            }
    }

    /**
     * Adds a shutdown hook to stop the export when called.
     *
     * @param fileArchiverSink the FileArchiverSink to stop
     */
    private static void setupShutdownHook(final FileArchiverSink fileArchiverSink) {
        logger.debug("FileArchiverSink.setupShutdownHook() called.");
        final Thread workerThread = Thread.currentThread();

        Runtime.getRuntime ().addShutdownHook (new Thread () {
            public void run () {
                fileArchiverSink.stopExport();
                try { workerThread.join(); } catch (InterruptedException e) {}
            }
        });
    }

    /**
     * Adds a progress listener to printout the status of the export
     *
     * @param fileArchiverSink the FileArchiverSink to monitor
     */
    private static void setupProgressListener(FileArchiverSink fileArchiverSink) {
        logger.debug("FileArchiverSink.setupProgressListener() called.");
        fileArchiverSink.addTimeProgressListener(new TimeProgressListener() {
            public void progressUpdate(double estimatedDuration, double consumedTime) {
                if (estimatedDuration == Double.MAX_VALUE) {
                    logger.info("Exported " + Math.round(consumedTime) + " seconds of data...");
                } else {
                    logger.info("Export of data " + Math.round(100*consumedTime/estimatedDuration) + "% complete...");
                }
            }
        });
    }

    /**
     * This method overrides the setOptions() method in RBNBBase and adds in
     * options for the various command line flags.
     */
    protected Options setOptions() {
        Options opt = setBaseOptions(new Options()); // uses h, s, p
        opt.addOption("k", true, "Sink Name (defaults to " + DEFAULT_SINK_NAME + ")");
        opt.addOption("n", true, "Source Name (defaults to " + DEFAULT_SOURCE_NAME + ")");
        opt.addOption("c", true, "Source Channel Name (defaults to " + DEFAULT_CHANNEL_NAME + ")");
        opt.addOption("d", true, "Base directory path (defaults to " + DEFAULT_ARCHIVE_DIRECTORY + ")");
        opt.addOption("S", true, "Start time (defauts to now)");
        opt.addOption("E", true, "End time (defaults to forever)");
        opt.addOption("I", true, "Interval (hourly, daily, or weekly) to periodically archive data\n Mututally exclusive with -E and -S");
        opt.addOption(OptionBuilder.withDescription("Event markers to filter start/stop times")
            .hasOptionalArg()
            .create("M"));
        opt.addOption("B", true, "Number of seconds to go back from now to set start time\n Mututally exclusive with -E and -S");

        setNotes("Writes data frames between start time and end time to the " +
                 "directory structure starting at the base directory. The time " +
                 "format is yyyy-MM-ddThh:mm:ss.nnn.");
        return opt;
    }

    /**
     * A method that sets the prefix string to be used in the archived file name
     *
     * @param filePrefix the prefix string to be used in the file name
     */
    public void setFilePrefix(String filePrefix) {
        this.filePrefix = filePrefix;
    }

    /**
     * A method that sets the extension string to be used in the archived file name
     *
     * @param fileExtension the extension string to be used in the file name
     */
    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    /**
     * This method overrides the setArgs() method in RBNBBase and sets the values
     * of the various command line arguments
     */
    protected boolean setArgs(CommandLine cmd) {
        logger.debug("FileArchiverSink.setArgs() called.");

        if (!setBaseArgs(cmd))
            return false;

        if (cmd.hasOption('n')) {
            String a = cmd.getOptionValue('n');
            if (a != null)
                sourceName = a;
                setFilePrefix(a + "_");
        }

        if (cmd.hasOption('c')) {
            String a = cmd.getOptionValue('c');
            if (a != null)
                channelName = a;
        }

        if (cmd.hasOption('k')) {
            String a = cmd.getOptionValue('k');
            if (a != null)
                sinkName = a;
        }

        if (cmd.hasOption('d')) {
            String a = cmd.getOptionValue('d');
            if (a != null)
                archiveDirectory = new File(a);
        }

        if (cmd.hasOption('S')) {
            String a = cmd.getOptionValue('S');
            if (a != null) {
                try {
                    Date d = FileArchiveUtility.getCommandFormat().parse(a);
                    long t = d.getTime();
                    startTime = ((double) t) / 1000.0;
                } catch (Exception e) {
                    logger.debug("Parse of start time failed " + a + e.getMessage());
                    printUsage();
                    return false;
                }
            }
        } else if (!cmd.hasOption('M')) {
            startTime = System.currentTimeMillis()/1000d;
        }

        if (cmd.hasOption('E')) {
            String a = cmd.getOptionValue('E');
            if (a != null) {
                try {
                    Date d = FileArchiveUtility.getCommandFormat().parse(a);
                    long t = d.getTime();
                    endTime = ((double) t) / 1000.0;
                } catch (Exception e) {
                    logger.debug("Parse of end time failed " + a);
                    printUsage();
                    return false;
                }
            }
        }

        if (cmd.hasOption('B')) {
            String a = cmd.getOptionValue('B');
            if (a != null) {
                try {
                    secondsResetStart = Integer.parseInt(a);
                    startTime = System.currentTimeMillis()/1000d - secondsResetStart;
                    endTime = System.currentTimeMillis()/1000d;
                    endArchiveCal = Calendar.getInstance();

                } catch (NumberFormatException nf) {
                    logger.debug("Please enter a number for seconds to reset the start to.");
                    return false;
                }
            }
        }
        if (startTime >= endTime) {
            logger.debug("The start time must come before the end time.");
            return false;
        }

        if (cmd.hasOption('M')) {
            String a = cmd.getOptionValue('M');
            if (a != null) {
                eventMarkerFilter = a;
            } else {
                eventMarkerFilter = "";
            }
        }

        // handle the -I option, test if it's an allowed value
        if ( cmd.hasOption("I") ) {
            String interval = cmd.getOptionValue("I");
            if ( interval != null ) {
                try {
                    if ( interval.equals("hourly") ) {
                        setArchiveInterval(3600);

                    } else if ( interval.equals("daily") ) {
                        setArchiveInterval(86400);

                    } else if ( interval.equals("weekly") ) {
                        setArchiveInterval(604800);

                    } else if ( interval.equals("debug") ) {
                        setArchiveInterval(120);

                    } else {
                        logger.debug("Please enter either hourly, daily, or weekly for the archiving interval.");

                    }
                } catch (NumberFormatException nf) {
                    return false;
                }
            }
        }

        channelPath = sourceName + "/" + channelName;

        return validateSetup();
    }

    public String getSinkName() {
        return sinkName;
    }

    public void setSinkName(String sinkName) {
        this.sinkName = sinkName;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getChannelPath() {
        return channelPath;
    }

    public void setChannelPath(String channelPath) {
        this.channelPath = channelPath;
    }

    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(double endTime) {
        this.endTime = endTime;
    }

    public File getArchiveDirectory() {
        return archiveDirectory;
    }

    public void setArchiveDirectory(File archiveDirectory) {
        this.archiveDirectory = archiveDirectory;
    }

    public Calendar getEndArchiveCal() {
        return endArchiveCal;
    }

    public void setEndArchiveCal(Calendar endArchiveCal) {
        FileArchiverSink.endArchiveCal = endArchiveCal;
    }

    public Calendar getBeginArchiveCal() {
        return beginArchiveCal;
    }

    public void setBeginArchiveCal(Calendar beginArchiveCal) {
        FileArchiverSink.beginArchiveCal = beginArchiveCal;
    }

    /**
     * Setup the paramters for data export.
     *
     * @param serverName the RBNB server name
     * @param serverPort the RBNB server port
     * @param sinkName the RBNB sink name
     * @param channelPath the full channel path
     * @param archiveDirectory the directory to archive to
     * @param startTime the start time
     * @param endTime the end time
     * @param eventMarkerFilter the event marker filter
     * @return true if the setup succeeded, false otherwise
     */
    public boolean setup(String serverName, int serverPort,
        String sinkName, String channelPath,
        File archiveDirectory, double startTime,
        double endTime, String eventMarkerFilter) {

        if (startTime >= endTime) {
            logger.debug("The start time must come before the end time.");
            return false;
        }

        setServerName(serverName);
        setServerPort(serverPort);

        this.sinkName = sinkName;
        this.channelPath = channelPath;
        this.archiveDirectory = archiveDirectory;
        this.startTime = startTime;
        this.endTime = endTime;
        this.eventMarkerFilter = eventMarkerFilter;

        return validateSetup();
    }

    /**
     * Validates the setup.  This method prepares the RBNB connection, sets up the
     * time ranges for data archival, and then chacks the time ranges for validity.
     *
     * @return true if the setup is valid, false otherwise
     */
    public boolean validateSetup() {
        logger.debug("FileArchiverSink.validateSetup() called.");
        printSetup();

        if (!connect()) {
            return false;
        }

        if (!setupTimeRanges()) {
            return false;
        }

        if (!checkTimeRanges()) {
            return false;
        }

        return true;
    }

    /**
     * Prints the setup parameters.
     */
    private void printSetup() {
        logger.debug("FileArchiverSink.printSetup() called.");
        logger.debug("Starting FileArchiverSink on " + getServer() + " as " + sinkName);
        logger.debug("    Archiving channel " + channelPath);
        logger.debug("    to directory " + archiveDirectory);

        if (endTime != Double.MAX_VALUE) {
            logger.debug("    from " + RBNBUtilities.secondsToISO8601(startTime) +
                    " to " + RBNBUtilities.secondsToISO8601(endTime));
        } else if (startTime != 0) {
            logger.debug("    from " + RBNBUtilities.secondsToISO8601(startTime));
        }

        if (eventMarkerFilter != null) {
            logger.debug("    using event marker filter " + eventMarkerFilter);
        }
    }

    /**
     * Sets up the time ranges based on the start and stop times and the event
     * marker filter.
     *
     * @return true if the time ranges are setup
     */
    private boolean setupTimeRanges() {
        logger.debug("FileArchiverSink.setupTimeRanges() called.");
        if (eventMarkerFilter == null) {
            timeRanges = new ArrayList<TimeRange>();
            timeRanges.add(new TimeRange(startTime, endTime));
        } else {
            try {
                timeRanges = MarkerUtilities.getTimeRanges(sink, eventMarkerFilter, startTime, endTime);
            } catch (SAPIException e) {
                logger.debug("Error retreiving event markers from server.");
                return false;
            } catch (IllegalArgumentException e) {
                logger.debug("Error: The event marker filter format is invalid.");
                return false;
            }
        }

        return true;
    }

    /**
     * Get the timeRanges
     * @return timeRanges  the time ranges that have been set
     */
    public List<TimeRange> getTimeRanges() {
        return timeRanges;
    }

    /**
     * Set the timeRanges
     * @param timeRanges the time ranges that have been set
     */
    public void setTimeRanges(List<TimeRange> timeRanges) {
        this.timeRanges = timeRanges;
    }

    /**
     * Checks the time ranges to see if they are valid and have data.
     *
     * @return true if the time ranges are valid
     */
    private boolean checkTimeRanges() {
        logger.debug("FileArchiverSink.checkTimeRanges() called.");
        Node channelMetadata;
        try {
            channelMetadata = RBNBUtilities.getMetadata(getServer(), channelPath);
        } catch (SAPIException e) {
            logger.debug("Error retrieving channel metadata from the server.");
            return false;
        }

        if (channelMetadata == null) {
            logger.debug("Error: Channel " + channelPath + " not found.");
            return false;
        }

        double currentTime = System.currentTimeMillis()/1000d;

        double channelStartTime = channelMetadata.getStart();
        double channelEndTime = channelStartTime + channelMetadata.getDuration();
        TimeRange channelTimeRange = new TimeRange(channelStartTime, channelEndTime);

        for (int i=0; i<timeRanges.size(); i++) {
            TimeRange timeRange = timeRanges.get(i);

            // allow end times in the future
            if (timeRange.getEndTime() > currentTime) {
                continue;
            }

            // skip time ranges in the past where there is no data
            if (!timeRange.intersects(channelTimeRange)) {
                logger.debug("Warning: Skipping the time range from " +
                    RBNBUtilities.secondsToISO8601(timeRange.getStartTime()) +
                    " to " +
                    RBNBUtilities.secondsToISO8601(timeRange.getEndTime()) +
                    " since there are no data for it.");
                timeRanges.remove(i--);
            }
        }

        if (timeRanges.size() == 0) {
            logger.debug("Error: There are no data for the specified time ranges.");
            logger.debug("There are data from " +
                RBNBUtilities.secondsToISO8601(channelStartTime) +
                " to " +
                RBNBUtilities.secondsToISO8601(channelEndTime) +
                ".");

            return false;
        }

        // move up start time to first data point
        TimeRange firstTimeRange = timeRanges.get(0);
        if (firstTimeRange.getStartTime() < channelTimeRange.getStartTime()) {
            logger.debug("Warning: Setting start time to " +
                RBNBUtilities.secondsToISO8601(channelTimeRange.getStartTime()) +
                " since there is no data before it.");
            firstTimeRange.setStartTime(channelTimeRange.getStartTime());
        }

        return true;
    }

    /**
     * Gets the start time.
     *
     * @return the start time
     */
    public double getStartTime() {
        return startTime;
    }

    /**
     * Gets the end time.
     *
     * @return the end time
     */
    public double getEndTime() {
        return endTime;
    }

    /**
     * Gets the event marker filter.
     *
     * @return the event marker filter
     */
    public String getEventMarkerFilter() {
        return eventMarkerFilter;
    }

    /**
     * A method that gets the archive interval
     *
     * @return the archive interval in seconds
     */
    public int getArchiveInterval() {
        return this.archiveInterval;
    }

    /**
     * A method that sets archive interval (in seconds)
     *
     * @param interval the archive interval (in seconds)
     */
    public void setArchiveInterval(int interval) {
        this.archiveInterval = interval;
    }

    /**
     * Export data to disk.
     */
    public boolean export() {
        logger.debug("FileArchiverSink.export() called.");
        doExport = true;

        if (!runWork()) {
            return false;
        }

        doExport = false;

        return true;
    }

    /**
     * Stop exporting data to disk. This will return immediately.
     */
    public void stopExport() {
        logger.debug("FileArchiverSink.stopExport() called.");
        doExport = false;
    }

    /**
     * Sees if data are being exported.
     *
     * @return true if exporting, false otherwise
     */
    public boolean isExporting() {
        return isConnected();
    }

    /**
     * Exports the data.
     */
    private boolean runWork() {
        logger.debug("FileArchiverSink.runWork() called.");
        int dataFramesExported = 0;

        try {
            FileArchiveUtility.confirmCreateDirPath(archiveDirectory);

            ChannelMap sMap = new ChannelMap();
            sMap.Add(channelPath);

            if (timeRanges.get(timeRanges.size()-1).getEndTime() == Double.MAX_VALUE) {
                duration = Double.MAX_VALUE;
            } else {
                duration = 0;
                for (TimeRange timeRange : timeRanges) {
                    duration += timeRange.getEndTime() - timeRange.getStartTime();
                }
            }

            double elapsedTime = 0;
            for (TimeRange timeRange : timeRanges) {
                if (timeRange.getEndTime() != Double.MAX_VALUE) {
                    logger.debug("Exporting data from " +
                        RBNBUtilities.secondsToISO8601(timeRange.getStartTime()) +
                         " to " +
                          RBNBUtilities.secondsToISO8601(timeRange.getEndTime()) +
                          ".");
                } else {
                    logger.debug("Exporting data from " +
                        RBNBUtilities.secondsToISO8601(timeRange.getStartTime()) +
                        ".");
                }


                if (!connect()) {
                    return false;
                }

                dataFramesExported += exportData(sMap,
                    timeRange.getStartTime(),
                    timeRange.getEndTime(),
                    duration,
                    elapsedTime);
                disconnect();

                elapsedTime += timeRange.getEndTime() - timeRange.getStartTime();
                fireProgressUpdate(elapsedTime);

                if (!doExport) {
                    break;
                }
            }
        } catch (SAPIException e) {
            logger.debug("Error getting data from server: " + e.getMessage() + ".");
            return false;
        } catch (IOException e) {
            logger.debug("Error writing data to file: " + e.getMessage() + ".");
            return false;
        } finally {
            disconnect();
        }

        if (doExport) {
            logger.debug("Export complete. Wrote " + dataFramesExported + " data frames.");
        } else {
            logger.debug("Export stopped. Wrote " + dataFramesExported + " data frames.");
        }

        return true;
    }

    /**
     * Exports data for a time range.
     *
     * @param map the channel map
     * @param startTime the start time for the data
     * @param endTime the end time for the data
     * @param baseTime the base elasped time for the export
     * @return the number of data frames written to disk
     * @throws SAPIException if there is an error getting the data from the server
     * @throws IOException if there is an error writing the file
     */
    private int exportData(ChannelMap map, double startTime, double endTime,
        double duration, double baseTime) throws SAPIException, IOException {
        logger.debug("FileArchiverSink.exportData() called.");

        //sink.Subscribe(map, startTime, 0.0, "absolute");
        sink.Subscribe(map, startTime, duration, "absolute");

        int frameCount = 0;
        int fetchRetryCount = 0;

        logger.debug("doExport is: " + doExport);

        while (doExport) {
            logger.debug("Get the channel map.");
            ChannelMap m = sink.Fetch(1800000); // fetch with 3 min sec timeout

            if (m.GetIfFetchTimedOut()) {
                if (++fetchRetryCount < 10) {
                    logger.debug("Warning: Request for data timed out, retrying.");
                    continue;
                } else {
                    logger.debug("Error: Unable to get data from server.");
                    break;
                }
            } else {
                logger.debug("Fetch has not timed out.");
                fetchRetryCount = 0;
            }

            logger.debug("Error: Get the channel index.");
            int index = m.GetIndex(channelPath);

            logger.debug("Channel index is: " + index);

            if (index < 0) {
                logger.debug("Error: The channel index was < 0.");
                break;
            }

            // convert sec to millisec
            double timestamp = m.GetTimes(index)[0];
            long unixTime = (long) (timestamp * 1000.0);
            File output =
            FileArchiveUtility.makePathFromTime(archiveDirectory, unixTime,
                filePrefix, filePathDepth, fileExtension);

            logger.debug("Writing data to: " + output.getPath());

            if (FileArchiveUtility.confirmCreateDirPath(output.getParentFile())) {

                byte[] data = m.GetData(index);
                int numberOfFrames = (m.GetTimes(index)).length;

                FileOutputStream out = new FileOutputStream(output);
                for ( int i = 0; i < data.length; i++ ) {

                    out.write(data[i]);
                    if ( i % data.length/numberOfFrames == 0 ) {
                        frameCount++;
                    }
                }

                out.close();
                doExport = false;

                // test the file write success
                String newFileName = output.getPath();
                File latestDataFile = new File(newFileName);

                if ( latestDataFile.length() > 0L ) {
                    logger.info("Successful export to " + latestDataFile.getPath());

                } else {
                    logger.info("Unsuccessful export. File " +
                        latestDataFile.getPath() +
                        " was " + latestDataFile.length() + " bytes.");

                }

            } else {
                logger.error("Couldn't confirm path was created to :" + output.getParentFile().getPath());

            }

        }
        logger.debug("Frame count is: " + frameCount);
        return frameCount;
    }

    /**
     * Connect to the RBNB server.
     *
     * @return true if connected, false otherwise
     */
    private boolean connect() {
        logger.debug("FileArchiverSink.connect() called.");
        if (isConnected()) {
            return true;
        }

        try {
            sink.OpenRBNBConnection(getServer(), sinkName);
        } catch (SAPIException e) {
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
        logger.debug("FileArchiverSink.disconnect() called.");
        if (!isConnected()) {
            return;
        }

        sink.CloseRBNBConnection();

        connected = false;
    }

    /**
     * Sees if we are connected to the RBNB server.
     *
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Adds a listener for the export progress.
     *
     * @param listener the listener to add
     */
    public void addTimeProgressListener(TimeProgressListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a listener for the export progress.
     *
     * @param listener the listener to remove
     */
    public void removeTimeProgressListener(TimeProgressListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notifies listener of progress.
     *
     * @param time the elapsed time to notify
     */
    private void fireProgressUpdate(double time) {
        for (TimeProgressListener listener : listeners) {
            listener.progressUpdate(duration, time);
        }
    }

    /** A method that returns the CVS version string */
    protected String getCVSVersionString() {
        return ("$LastChangedDate$\n"
                + "$LastChangedRevision$"
                + "$LastChangedBy$"
                + "$HeadURL$");
    }

}