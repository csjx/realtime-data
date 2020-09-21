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

import edu.hawaii.soest.kilonalu.utilities.FileArchiverSink;
import edu.hawaii.soest.pacioos.text.configure.Configuration;
import edu.hawaii.soest.pacioos.text.convert.RawToPacIOOS2020Converter;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.time.Instant;
import java.util.Objects;
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
        Configuration config = null;
        if (args.length != 1) {
            log.error("Please provide the path to the instrument's XML configuration file " +
                      "as a single parameter.");
            System.exit(1);
        } else {
            xmlConfiguration = args[0];
        }

        // Get a configuration based on the XML configuration
        try {
            config = new Configuration(xmlConfiguration);

        } catch (ConfigurationException e) {
            String msg = "The was a problem configuring the archiver. The message was: " +
                e.getMessage();
            log.error(msg);
            if (log.isDebugEnabled() ) {
                e.printStackTrace();
            }
        }

        int totalChannels = Objects.requireNonNull(config).getTotalChannels();
        int totalArchivers = Objects.requireNonNull(config).getTotalArchivers();

        if (totalArchivers == 0) {
            log.info("No archivers are configured for this instrument. Exiting.");
            System.exit(0);
        }

        // Use the configuration
        for (int channelIndex = 0; channelIndex < totalChannels; channelIndex++) {

            for (int archiverIndex = 0; archiverIndex < totalArchivers; archiverIndex++) {
                // Get an archiver for the channel if the interval or range is configured
                if (config.hasArchiveInterval(channelIndex, archiverIndex) ||
                    config.hasArchiveDateRange(channelIndex, archiverIndex)) {
                    FileArchiverSink archiver = getArchiver(config, channelIndex, archiverIndex);

                    // Archive the data on a schedule
                    if (archiver.getArchiveInterval() > 0) {
                        // override the command line start and end times
                        archiver.validateSetup();
                        archiver.setupArchiveTime();

                        TimerTask archiveDataTask = new TimerTask() {
                            public void run() {
                                log.debug("TimerTask.run() called.");

                                if (archiver.validateSetup()) {
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
                        if (archiver.validateSetup()) {
                            archiver.export();
                        }
                    }

                } else {
                    log.info("Archivers must have either an archiveInterval or " +
                        "archiveDateRange configured. Skipping this archiver.");
                }
            }

        }
    }

    private static FileArchiverSink getArchiver(Configuration config, int channelIndex, int archiverIndex) {

        // The archiver instance to configure and return
        FileArchiverSink archiver = new FileArchiverSink();
        // Get the channel name
        String channelName = config.getChannelName(channelIndex);
        // Get the channel time zone
        String timeZoneId = config.getTimeZoneID(channelIndex);
        // Get the archive type
        String archiveType = config.getArchiveType(channelIndex, archiverIndex);

        archiver.setServerName(config.getServerName());
        archiver.setServerPort(config.getServerPort());
        archiver.setRBNBClientName(config.getIdentifier() + "-" + archiveType + "-archiver");
        archiver.setSinkName(config.getIdentifier() + "-" + archiveType + "-archiver");
        archiver.setSourceName(config.getIdentifier());
        archiver.setFilePrefix(config.getIdentifier() + "_");

        // Get the archive interval
        int archiveInterval = config.getArchiveInterval(channelIndex, archiverIndex);
        if (archiveInterval > 0) {
            archiver.setArchiveInterval(archiveInterval);
        } else {
            // Otherwise archive by start and end dates
            try {
                Instant startDateTime = config.getStartDateTime(channelIndex, archiverIndex);
                Instant endDateTime = config.getEndDateTime(channelIndex, archiverIndex);
                if ( startDateTime != null && endDateTime !=null ) {
                    archiver.setStartTime(startDateTime.getEpochSecond());
                    archiver.setEndTime(endDateTime.getEpochSecond());
                } else {
                    String msg = "Couldn't configure an archiver date range, " +
                        "start or end date time is null.  Check the configuration.";
                    throw new ConfigurationException(msg);
                }
            } catch (ConfigurationException e) {
                log.error("Could not get an archiver: " + e.getMessage());
                if (log.isDebugEnabled()) {
                    e.printStackTrace();
                    System.exit(0);
                }
            }
        }

        // For raw archivers, include the channelName in the directory path
        if (archiveType != null && archiveType.equals("raw")) {

            archiver.setChannelPath(config.getIdentifier() + "/" + channelName);
            File file = new File(config.getArchiveBaseDirectory(channelIndex, archiverIndex) +
                "/" + config.getIdentifier() + "/" + channelName);
            archiver.setArchiveDirectory(file);

        // For pacioos archivers, exclude the channelName from the directory path
        } else if (archiveType != null && archiveType.equals("pacioos-2020-format")) {
            archiver.setChannelPath(config.getIdentifier());
            File file = new File(
                config.getArchiveBaseDirectory(channelIndex, archiverIndex) +
                "/" +
                config.getIdentifier());
            archiver.setArchiveDirectory(file);
            // Flag samples to be converted before export
            archiver.setConvertSamples(true);
            RawToPacIOOS2020Converter converter = new RawToPacIOOS2020Converter();
            converter.setFieldDelimiter(config.getFieldDelimiter(channelIndex));
            converter.setMissingValueCode(config.getMissingValueCode(channelIndex));
            converter.setNumberHeaderLines(0);
            converter.setTimeZoneId(timeZoneId);

            // TODO: set the dateFormats and dateFields in the converter, and use them
            //       to set the dateFormat, or timeFormat, or dateTimeFormat.

            // Set the converter for the archiver
            archiver.setConverter(converter);
        } else {
            log.error("Please use an archiveType of raw or pacioos-2020-format");
            System.exit(0);
        }

        return archiver;
    }
}
