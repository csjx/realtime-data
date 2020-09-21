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
package edu.hawaii.soest.pacioos.text.configure;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Objects;

public class Configuration {

    private final Log log = LogFactory.getLog(Configuration.class);

    private final XMLConfiguration xmlConfig = new XMLConfiguration();

    private final String DATE_TIME_RANGE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public Configuration() {
    }

    public Configuration(String xmlConfiguration) throws ConfigurationException {

        // Load the given XML config file from the path
        xmlConfig.setListDelimiter("|".charAt(0));
        xmlConfig.load(xmlConfiguration);
    }

    /**
     * Get the server name
     * @return serverName the server name
     */
    public String getServerName() {
        return xmlConfig.getString("rbnbServer");
    }

    /**
     * Get the server port
     * @return serverPort the server port
     */
    public int getServerPort() {
        return xmlConfig.getInt("rbnbPort");
    }

    /**
     * Get the instrument identifier
     * @return identifier the instrument identifier
     */
    public String getIdentifier() {
        return xmlConfig.getString("identifier");
    }

    /**
     * Get the total channel count
     * @return totalChannels the total channel count
     */
    public int getTotalChannels() {
        int totalChannels = 0;
        Collection<?> channels = xmlConfig.getList("channels.channel.name");

        // Set the total channels
        if (channels != null) {
            totalChannels = channels.size();
        }
        return totalChannels;
    }

    /**
     * Get the total archivers count
     * @return totalArchivers the total archivers count
     */
    public int getTotalArchivers() {
        int totalArchivers = 0;
        Collection<?> archivers =
            xmlConfig.getList("channels.channel.archivers.archiver.archiveType");
        if (archivers != null) {
            totalArchivers = archivers.size();
        }
        return totalArchivers;
    }

    /**
     * Get the archive type for the given channel and archiver index
     * @param channelIndex the channel index in the XML configuration
     * @param archiverIndex the archiver index in the XML configuration
     * @return archiveType the archive type
     */
    public String getArchiveType(int channelIndex, int archiverIndex) {
        return xmlConfig.getString(
            "channels.channel(" + channelIndex + ").archivers.archiver" +
                "(" + archiverIndex + ")." + "archiveType");
    }

    public boolean hasArchiveInterval(int channelIndex, int archiverIndex) {
        String archiveIntervalPath =
            "channels.channel(" + channelIndex + ").archivers.archiver" +
                "(" + archiverIndex + ")." + "archiveInterval";
        return xmlConfig.configurationsAt(archiveIntervalPath).size() > 0;
    }

    public boolean hasArchiveDateRange(int channelIndex, int archiverIndex) {
        String archiveDateRangePath =
            "channels.channel(" + channelIndex + ").archivers.archiver" +
                "(" + archiverIndex + ")." + "archiveDateRange";
        return xmlConfig.configurationsAt(archiveDateRangePath).size() > 0;
    }

    public int getArchiveInterval(int channelIndex, int archiverIndex) {
        int archiveInterval = 0;

        if (hasArchiveInterval(channelIndex, archiverIndex)) {
            String archiveIntervalPath =
                "channels.channel(" + channelIndex + ").archivers.archiver" +
                    "(" + archiverIndex + ")." + "archiveInterval";
            String archiveIntervalStr = xmlConfig.getString(archiveIntervalPath);

            switch (Objects.requireNonNull(archiveIntervalStr)) {
                case "hourly":
                     archiveInterval = 3600;
                     break;
                case "daily":
                    archiveInterval = 86400;
                    break;
                case "weekly":
                    archiveInterval = 604800;
                    break;
                case "debug":
                    archiveInterval = 120;
                default:
                    log.error("Please use either hourly or daily " +
                        "for the archiving interval.");
                    System.exit(0);
            }
        }
        return archiveInterval;
    }

    public List<String> listDateFormats(int channelIndex) throws ConfigurationException {

        List<String> dateFormats =
            xmlConfig.getList("channels.channel(" +
                channelIndex +
                ").dateFormats.dateFormat");

        // Validate the formats
        if (dateFormats.size() != 0) {
            for (String dateFormat : dateFormats) {

                // validate the date format string
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);

                } catch (IllegalFormatException ife) {
                    String msg = "There was an error parsing the date format " +
                        dateFormat + ". The message was: " + ife.getMessage();
                    if (log.isDebugEnabled()) {
                        ife.printStackTrace();
                    }
                    throw new ConfigurationException(msg);
                }
            }
        } else {
            log.warn("No date format have been configured for this instrument.");
        }        return dateFormats;
    }

    public List<Integer> listDateFieldPositions(int channelIndex) throws ConfigurationException {

        // Get the date fields
        List<String> dateFieldList =
            xmlConfig.getList("channels.channel(" + channelIndex + ").dateFields.dateField");
        List<Integer> dateFields = new ArrayList<>();
        if (dateFieldList.size() != 0) {
            for (String dateField : dateFieldList) {
                try {
                    Integer newDateField = new Integer(dateField);
                    dateFields.add(newDateField);
                } catch (NumberFormatException e) {
                    String msg = "There was an error parsing the dateFields. The message was: " +
                        e.getMessage();
                    throw new ConfigurationException(msg);
                }
            }
        } else {
            log.warn("No date fields have been configured for this instrument.");
        }
        return dateFields;
    }

    public Instant getStartDateTime(int channelIndex, int archiverIndex)
        throws ConfigurationException {

        DateTimeFormatter formatter;
        String start;
        Instant startDateTime;

        if (hasArchiveDateRange(channelIndex, archiverIndex)) {
            String archiveDateRangePath =
                "channels.channel(" + channelIndex + ").archivers.archiver" +
                    "(" + archiverIndex + ")." + "archiveDateRange";
            start = xmlConfig.getString(archiveDateRangePath + ".startDateTime");
            formatter = DateTimeFormatter.ofPattern(DATE_TIME_RANGE_PATTERN);
            startDateTime = (Instant) formatter.parse(start);
        } else {
            String msg = "No archive date ranges were found at" +
                "channel " + channelIndex +
                " and archiver " + archiverIndex;
            throw new ConfigurationException(msg);
        }
        return startDateTime;
    }

    public Instant getEndDateTime(int channelIndex, int archiverIndex)
        throws ConfigurationException {

        DateTimeFormatter formatter;
        String end;
        Instant endDateTime;

        if (hasArchiveDateRange(channelIndex, archiverIndex)) {
            String archiveDateRangePath =
                "channels.channel(" + channelIndex + ").archivers.archiver" +
                    "(" + archiverIndex + ")." + "archiveDateRange";
            end = xmlConfig.getString(archiveDateRangePath + ".endDateTime");
            formatter = DateTimeFormatter.ofPattern(DATE_TIME_RANGE_PATTERN);
            endDateTime = (Instant) formatter.parse(end);
        } else {
            String msg = "No archive date ranges were found at " +
                "channel " + channelIndex +
                " and archiver " + archiverIndex;
            throw new ConfigurationException(msg);
        }
        return endDateTime;
    }

    /**
     *Get the channel name for the given channel index
     * @param channelIndex the channel index
     * @return channelName the channel name
     */
    public String getChannelName(int channelIndex) {
        return xmlConfig.getString("channels.channel(" + channelIndex + ").name");
    }

    public String getTimeZoneID(int channelIndex) {
        return xmlConfig.getString("channels.channel(" + channelIndex + ").timeZone");

    }

    public String getArchiveBaseDirectory(int channelIndex, int archiverIndex) {
        return xmlConfig.getString(
            "channels.channel(" + channelIndex + ").archivers.archiver" +
                "(" + archiverIndex + ")." + "archiveBaseDirectory");
    }

    public String getFieldDelimiter(int channelIndex) {
        return xmlConfig.getString("channels.channel(" + channelIndex + ").fieldDelimiter");
    }

    public String getMissingValueCode(int channelIndex) {
        return xmlConfig.getString("channels.channel(" + channelIndex + ").missingValueCode");
    }
}
