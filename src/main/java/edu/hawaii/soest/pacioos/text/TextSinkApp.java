/*
 *  Copyright: 2020 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: A class that is the main entry point for communicating
 *             with a DataTurbine and archiving channel data to disk.
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

import edu.hawaii.soest.kilonalu.utilities.FileArchiveUtility;
import edu.hawaii.soest.kilonalu.utilities.FileArchiverSink;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A simple class used to start a FileArchiverSink archiver.  Configure the archiver by changing
 * the XML-based configuration file with the proper settings.
 * 
 * @author cjones
 */
public class TextSinkApp {

    private static final Log log = LogFactory.getLog(TextSinkApp.class);

    /**
     * @param args the main arguments
     */
    public static void main(String[] args) {
        
        String xmlConfiguration = null;
        if (args.length != 1) {
            log.error("Please provide the path to the instrument's XML configuration file " +
                      "as a single parameter.");
            System.exit(1);
        } else {
            xmlConfiguration = args[0];
        }

        try {

            // Figure out how many archiver sinks we need
            XMLConfiguration xmlConfig = new XMLConfiguration();
            xmlConfig.setListDelimiter(new String("|").charAt(0));
            xmlConfig.load(xmlConfiguration);

            // Get the channels to archive
            Collection<?> channels = xmlConfig.getList("channels.channel.name");
            int totalChannels = 0;
            if ( channels != null ) {
                totalChannels = channels.size();
            }

            // Loop through each channel and find the archiving properties
            for (int channelIndex = 0; channelIndex < totalChannels; channelIndex++) {
                String channelName =
                    xmlConfig.getString("channels.channel(" + channelIndex + ").name");

                // Get the list of archiver names
                Collection<?> archivers =
                    xmlConfig.getList("channels.channel.archivers.archiver.archiveType");
                int totalArchivers = 0;
                if (archivers != null) {
                    totalArchivers = archivers.size();
                }

                if ( totalArchivers == 0 ) {
                    log.info("No archivers are configured for this instrument. Exiting.");
                    System.exit(0);
                }

                // Start an archiver sink for each listed in the instrument's XML configuration
                for (int archiverIndex = 0; archiverIndex < totalArchivers; archiverIndex++) {

                    FileArchiverSink archiver = new FileArchiverSink();
                    String serverName = xmlConfig.getString("rbnbServer");
                    int serverPort = xmlConfig.getInt("rbnbPort");
                    String identifier = xmlConfig.getString("identifier");
                    String archiveType = xmlConfig.getString(
                        "channels.channel(" + channelIndex + ").archivers.archiver" +
                            "(" + archiverIndex + ")." + "archiveType");
                    // Is this an interval-based or range-based archiver?
                    String archiveInterval = null;
                    String startDateTime = null;
                    String endDateTime = null;
                    String archiveIntervalPath =
                        "channels.channel(" + channelIndex + ").archivers.archiver" +
                        "(" + archiverIndex + ")." + "archiveInterval";
                    boolean hasArchiveInterval = xmlConfig.configurationsAt(archiveIntervalPath).size() > 0;
                    String archiveDateRangePath =
                            "channels.channel(" + channelIndex + ").archivers.archiver" +
                                    "(" + archiverIndex + ")." + "archiveDateRange";
                    boolean hasArchiveDateRange = xmlConfig.configurationsAt(archiveDateRangePath).size() > 0;
                    if ( hasArchiveInterval ) {
                        // An interval-based archiver
                        archiveInterval = xmlConfig.getString(archiveIntervalPath);
                    } else if ( hasArchiveDateRange ) {
                        // A range-based archiver
                        startDateTime = xmlConfig.getString(archiveDateRangePath + ".startDateTime");
                        endDateTime = xmlConfig.getString(archiveDateRangePath + ".endDateTime");
                    } else {
                        log.info("Archivers must have either an archiveInterval or " +
                            "archiveDateRange configured. Skipping this archiver.");
                        continue;
                    }

                    String archiveBaseDirectory = xmlConfig.getString(
                        "channels.channel(" + channelIndex + ").archivers.archiver" +
                            "(" + archiverIndex + ")." + "archiveBaseDirectory");

                    /*
                     -I    Interval (hourly, daily, or weekly) to periodically archive data
                           Mutually exclusive with -E and -S
                     -c    Source Channel Name (defaults to BinaryPD0EnsembleData)
                     -d    Base directory path (defaults to /data/rbnb)
                     -k    Sink Name (defaults to FileArchiver)
                     -n    Source Name (defaults to KN0101_010ADCP010R00)
                     -p    RBNB Server Port Number *3333
                     -s    RBNB Server Hostname *localhost
                    */

                    archiver.setServerName(serverName);
                    archiver.setServerPort(serverPort);
                    archiver.setRBNBClientName(identifier + "-" + archiveType + "-archiver");
                    archiver.setSinkName(identifier + "-" + archiveType + "-archiver");
                    archiver.setSourceName(identifier);
                    archiver.setFilePrefix(identifier + "_");

                    // For raw archivers, include the channelName in the directory path
                    if (archiveType != null && archiveType.equals("raw")) {
                        archiver.setChannelPath(identifier + "/" + channelName);
                        File file = new File(archiveBaseDirectory + "/" + identifier + "/" + channelName);
                        archiver.setArchiveDirectory(file);
                    // For pacioos archivers, exclude the channelName from the directory path
                    } else if (archiveType != null  && archiveType.equals("pacioos-2020-format")) {
                        archiver.setChannelPath(identifier);
                        File file = new File(archiveBaseDirectory + "/" + identifier);
                        archiver.setArchiveDirectory(file);
                    } else {
                        log.error("Please use an archiveType of raw or pacioos-2020-format");
                        System.exit(0);
                    }

                    // Set the archiveInterval in seconds based on the configuration
                    if ( hasArchiveInterval && archiveInterval != null ) {
                        switch (archiveInterval) {
                            case "hourly":
                                archiver.setArchiveInterval(3600);

                                break;
                            case "daily":
                                archiver.setArchiveInterval(86400);

                                break;
                            case "weekly":
                                archiver.setArchiveInterval(604800);

                                break;
                            case "debug":
                                archiver.setArchiveInterval(120);

                                break;
                            default:
                                log.error("Please use either hourly, daily, or weekly " +
                                        "for the archiving interval.");
                                System.exit(0);
                        }
                    } else if ( hasArchiveDateRange && startDateTime != null && endDateTime != null ) {
                        // Set the archiver start time
                        Date startDate = FileArchiveUtility.getCommandFormat().parse(startDateTime);
                        long stime = startDate.getTime();
                        double startTime = ((double) stime) / 1000.0;
                        archiver.setStartTime(startTime);

                        // Set the archiver end time
                        Date endDate = FileArchiveUtility.getCommandFormat().parse(endDateTime);
                        long etime = endDate.getTime();
                        double endTime = ((double) etime) / 1000.0;
                        archiver.setEndTime(endTime);
                    } else {
                        log.error("Please use either an hourly or daily archiving interval or " +
                            "set a start and end archive date range.");
                        System.exit(0);
                    }

                    // Archive the data on a schedule
                    if ( archiver.getArchiveInterval() > 0 ) {
                        // override the command line start and end times
                        archiver.validateSetup();
                        archiver.setupArchiveTime();

                        TimerTask archiveDataTask = new TimerTask() {
                            public void run() {
                                log.debug("TimerTask.run() called.");

                                if ( archiver.validateSetup() ) {
                                    archiver.export();
                                    archiver.setupArchiveTime();
                                }
                            }
                        };

                        Timer archiveTimer = new Timer();
                        // run the archiveData timer task on the hour, every hour (or every day)
                        archiveTimer.scheduleAtFixedRate(archiveDataTask,
                            archiver.getEndArchiveCal().getTime(),
                            archiver.getArchiveInterval() * 1000);

                    } else {
                        // archive data once based on the start and end times
                        if ( archiver.validateSetup() ) {
                            archiver.export();
                        }
                    }
                } // end for(archivers)
            } // end for(channels)

        } catch (ConfigurationException | ParseException e) {
            if (log.isDebugEnabled() ) {
                e.printStackTrace();                
            }
            
            log.error("There was a problem configuring the archiver.  The error message was: " +
                      e.getMessage());
            System.exit(1);
        }
    }
}
