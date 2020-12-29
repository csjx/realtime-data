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
import tech.tablesaw.api.ColumnType;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Objects;

/**
 * A Configuration represents a single instrument deployment's XML-based configuration
 */
public class Configuration {

    private final Log log = LogFactory.getLog(Configuration.class);

    private final XMLConfiguration xmlConfig = new XMLConfiguration();

    private final String DATE_TIME_RANGE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    /* The list of channel  configurations */
    private List<ChannelConfiguration> channelConfigurations = new ArrayList<>();

    /**
     * Construct an empty configuration
     */
    public Configuration() {
    }

    /**
     * Construct a configuration from the given file path
     * @param xmlConfiguration the path to the XML configuration file
     * @throws ConfigurationException a configuration exception
     */
    public Configuration(String xmlConfiguration) throws ConfigurationException {

        // Load the given XML config file from the path
        xmlConfig.setListDelimiter("|".charAt(0));
        xmlConfig.load(xmlConfiguration);


        // Set the channel configurations
        Collection<?> channels = xmlConfig.getList("channels.channel.name");
        if ( channels != null ) {
            for (int channelIndex = 0; channelIndex < channels.size(); channelIndex++) {
                ChannelConfiguration channelConfiguration = new ChannelConfiguration(
                    isDefaultChannel(channelIndex),
                    getChannelName(channelIndex),
                    getChannelDataType(channelIndex),
                    getChannelDataPattern(channelIndex),
                    getDataPrefix(channelIndex),
                    getColumnTypes(channelIndex),
                    getFieldDelimiter(channelIndex),
                    getRecordDelimiter(channelIndex),
                    getDateFormats(channelIndex),
                    getDateFields(channelIndex),
                    getTimeZoneID(channelIndex),
                    getArchiverConfigurations(channelIndex));
                channelConfigurations.add(channelConfiguration);
            }
        }
    }

    /**
     * List the channel archiver configurations
     * @return archiverConfigurations the list of archiver configurations
     */
    public List<ArchiverConfiguration> getArchiverConfigurations(int channelIndex) {
        List<ArchiverConfiguration> archiverConfigurations = new ArrayList<>();
        Collection<?> archivers =
            xmlConfig.getList(
                "channels.channel(" +
                channelIndex +
                ").archivers.archiver.archiveType");

        for (int archiverIndex = 0; archiverIndex < archivers.size(); archiverIndex++) {
            ArchiverConfiguration archiverConfiguration =
                new ArchiverConfiguration(
                    getArchiveType(channelIndex, archiverIndex),
                    getArchiveInterval(channelIndex, archiverIndex),
                    getArchiveBaseDirectory(channelIndex, archiverIndex)
                );
            archiverConfigurations.add(archiverConfiguration);
        }
        return archiverConfigurations;
    }


    /**
     * Get the channel data pattern
     * @param channelIndex the channel index
     * @return dataPattern the channel data pattern
     */
    public String getChannelDataPattern(int channelIndex) {
        return xmlConfig.getString(
            "channels.channel(" + channelIndex + ").dataPattern"
        );
    }


    /**
     * Get the channel data type
     * @param channelIndex the channel index
     * @return dataType the channel data type
     */
    public String getChannelDataType(int channelIndex) {
        return xmlConfig.getString(
            "channels.channel(" + channelIndex + ").dataType"
        );
    }

    /**
     * Check if the channel is the default
     * @param channelIndex the channel index
     * @return true if the channel is the default
     */
    public boolean isDefaultChannel(int channelIndex) {
        return xmlConfig.getBoolean(
            "channels.channel(" + channelIndex + ")[@default]"
        );
    }

    /**
     * Get the RBNB client name
     * @return rbnbName the RBNB client name
     */
    public String getClientName() {
        return xmlConfig.getString("rbnbName");
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

        if ( getChannelConfigurations() != null ) {
            totalChannels = getChannelConfigurations().size();
        }
        return totalChannels;
    }

    /**
     * Get the total archivers count
     * @return totalArchivers the total archivers count
     */
    public int getTotalArchivers() {
        int totalArchivers = 0;

        if ( getChannelConfigurations() != null ) {
            for (ChannelConfiguration channelConfiguration : getChannelConfigurations() ) {
                totalArchivers += channelConfiguration.getTotalArchivers();
            }
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

    /**
     * Return true if the archive interval is set
     * @param channelIndex the desired channel index
     * @param archiverIndex the desired archiver index
     * @return true if an archive interval has been set
     */
    public boolean hasArchiveInterval(int channelIndex, int archiverIndex) {
        String archiveIntervalPath =
            "channels.channel(" + channelIndex + ").archivers.archiver" +
                "(" + archiverIndex + ")." + "archiveInterval";
        return xmlConfig.configurationsAt(archiveIntervalPath).size() > 0;
    }

    /**
     * Return true if the archive date range is set
     * @param channelIndex the desired channel index
     * @param archiverIndex the desired archiver index
     * @return true if an archive date range has been configured
     */
    public boolean hasArchiveDateRange(int channelIndex, int archiverIndex) {
        String archiveDateRangePath =
            "channels.channel(" + channelIndex + ").archivers.archiver" +
                "(" + archiverIndex + ")." + "archiveDateRange";
        return xmlConfig.configurationsAt(archiveDateRangePath).size() > 0;
    }

    /**
     * Get the archive interval
     * @param channelIndex the desired channel index
     * @param archiverIndex the desired archiver index
     * @return archiveInterval the archive interval
     */
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
                    break;
                default:
                    log.error("Please use either hourly or daily " +
                        "for the archiving interval.");
                    System.exit(0);
            }
        }
        return archiveInterval;
    }

    /**
     * List the date formats
     * @param channelIndex the desired channel index
     * @return dateFormats the date formats in the configuration
     * @throws ConfigurationException a configuration exception
     */
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

    /**
     * List the date field positions
     * @param channelIndex the desired channel index
     * @return dateFields the field positions in the configuration
     * @throws ConfigurationException a configuration exception
     */
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

    /**
     * Get the start datetime
     * @param channelIndex the desired channel index
     * @param archiverIndex the desired archiver index
     * @return startDateTime the start datetime of the archive date range
     * @throws ConfigurationException a configuration exception
     */
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

    /**
     * Get the end datetime
     * @param channelIndex the desired channel index
     * @param archiverIndex the desired archiver index
     * @return the end date of an archive date range
     * @throws ConfigurationException a configuration exception
     */
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

    /**
     * Get the time zone id
     * @param channelIndex the desired channel index
     * @return the sample time zone id
     */
    public String getTimeZoneID(int channelIndex) {
        return xmlConfig.getString("channels.channel(" + channelIndex + ").timeZone");

    }

    /**
     * Get the archive base directory
     * @param channelIndex the desired channel index
     * @param archiverIndex the desired archiver index
     * @return archiveBaseDirectory the archive base directory
     */
    public String getArchiveBaseDirectory(int channelIndex, int archiverIndex) {
        return xmlConfig.getString(
            "channels.channel(" + channelIndex + ").archivers.archiver" +
                "(" + archiverIndex + ")." + "archiveBaseDirectory");
    }

    /**
     * Get the field delimiter
     * @param channelIndex the desired channel index
     * @return fieldDelimiter the field (column) delimiter of the sample
     */
    public String getFieldDelimiter(int channelIndex) {
        return xmlConfig.getString("channels.channel(" + channelIndex + ").fieldDelimiter");
    }

    /**
     * Get the RBNB archive memory amount (in bytes)
     * @return archiveMemory the RBNB archive memory amount (in bytes)
     */
    public int getArchiveMemory() {
        return xmlConfig.getInt("archiveMemory");
    }

    /**
     * Get the RBNB archive size (in bytes)
     * @return archiveSize the RBNB archive size (in bytes)
     */
    public int getArchiveSize() {
        return xmlConfig.getInt("archiveSize");
    }

    /**
     * Get the missing value code
     * @param channelIndex the desired channel index
     * @return missingValueCode the missingValueCode present in the sample data
     */
    public String getMissingValueCode(int channelIndex) {
        return xmlConfig.getString("channels.channel(" + channelIndex + ").missingValueCode");
    }

    /**
     * Get the data prefix (e.g. #)
     * @param channelIndex the desired channel index
     * @return dataPrefix the data prefix character
     */
    public String getDataPrefix(Integer channelIndex) {
        String prefix;

        prefix = xmlConfig.getString("channels.channel(" +
            channelIndex +
            ").dataPrefix");
        return prefix;
    }

    /**
     * Return true if the data prefix is set
     * @param channelIndex the desired channel index
     * @return true if the dataPrefix configuration is set
     */
    public boolean getHasDataPrefix(int channelIndex) {
        return getDataPrefix(channelIndex).length() > 0;
    }

    /**
     * Get the column types
     * @param channelIndex the desired channel index
     * @return columnTypes the column types of the sample table
     */
    public ColumnType[] getColumnTypes(int channelIndex) {
        List<ColumnType> columnTypes = new ArrayList<>();
        List<String> desiredTypes;
        String type;

        // Create a list of allowed types
        String[] allowedTypes = new String[]{
            "BOOLEAN", "DOUBLE", "FLOAT", "INSTANT", "INTEGER", "LOCAL_DATE", "LOCAL_DATE_TIME",
            "LOCAL_TIME", "LONG", "SHORT", "SKIP", "STRING", "TEXT"};
        List<String> allowed = Arrays.asList(allowedTypes);

        // Get a list of desired types
        desiredTypes =
            xmlConfig.getList("channels.channel(" + channelIndex + ").columnTypes.columnType");

        // Validate the desired types so we don't run into errors downstream
        for ( String desiredType : desiredTypes ) {
            type = desiredType.trim();
            if ( ! allowed.contains(type) ) {
                log.warn("The column type: " +
                    desiredType +
                    " is not allowed in the configuration. Allowed types are: " +
                    Arrays.toString(allowedTypes));
            } else {
                switch (type) {
                    case "BOOLEAN":
                        columnTypes.add(ColumnType.BOOLEAN);
                        break;
                    case "DOUBLE":
                        columnTypes.add(ColumnType.DOUBLE);
                        break;
                    case "FLOAT":
                        columnTypes.add(ColumnType.FLOAT);
                        break;
                    case "INSTANT":
                        columnTypes.add(ColumnType.INSTANT);
                        break;
                    case "INTEGER":
                        columnTypes.add(ColumnType.INTEGER);
                        break;
                    case "LOCAL_DATE":
                        columnTypes.add(ColumnType.LOCAL_DATE);
                        break;
                    case "LOCAL_DATE_TIME":
                        columnTypes.add(ColumnType.LOCAL_DATE_TIME);
                        break;
                    case "LOCAL_TIME":
                        columnTypes.add(ColumnType.LOCAL_TIME);
                        break;
                    case "LONG":
                        columnTypes.add(ColumnType.LONG);
                        break;
                    case "SHORT":
                        columnTypes.add(ColumnType.SHORT);
                        break;
                    case "STRING":
                        columnTypes.add(ColumnType.STRING);
                        break;
                    case "TEXT":
                        columnTypes.add(ColumnType.TEXT);
                        break;
                    default:
                        columnTypes.add(ColumnType.SKIP);
                }
            }
        }
        return columnTypes.toArray(new ColumnType[0]);
    }

    /**
     * Get the DateFormat from the configuration
     * @param channelIndex the index of the channel in the configuration
     * @return format the date format string
     * TODO: Note that we have hardcoded the date as the first format in the list. Change this.
     */
    public String getDateFormat(int channelIndex) {
        String format = null;

        // return
        if (getDateFormats(channelIndex).size() > 1) {
            format = getDateFormats(channelIndex).get(0);
        }
        return format;
    }

    /**
     * Get the TimeFormat from the configuration
     * @param channelIndex the index of the channel in the configuration
     * @return format the time format string
     * TODO: Note that we have hardcoded the time as the second format in the list. Change this.
     */
    public String getTimeFormat(int channelIndex) {
        String format = null;
        if (getDateFormats(channelIndex).size() > 1) {
            format = getDateFormats(channelIndex).get(1);
        }
        return format;
    }

    /**
     * Get the DateTimeFormat from the configuration
     * @param channelIndex the index of the channel in the configuration
     * @return format the date and time format string
     */
    public String getDateTimeFormat(int channelIndex) {
        String format = null;
        if (getDateFormats(channelIndex).size() == 1) {
            format = getDateFormats(channelIndex).get(0);
        }
        return format;
    }

    /**
     * Get the date fields
     * @param channelIndex the desired channel index
     * @return dateFields the date fields in the configuration
     */
    public List<String> getDateFields(int channelIndex) {
        List<String> dateFields =
            xmlConfig.getList("channels.channel(" + channelIndex + ").dateFields.dateField");
        return dateFields;
    }

    /**
     * Get the total date field count
     * @param channelIndex the desired channel index
     * @return totalDateFields the count of the total date fields
     */
    public int getTotalDateFields(int channelIndex) {
        List<String> dateFields = getDateFields(channelIndex);
        return dateFields.size();
    }

    /**
     * Get the date formats
     * @param channelIndex the desired channel index
     * @return dateFormats
     */
    public List<String> getDateFormats(int channelIndex) {
        List<String> dateFormats =
            xmlConfig.getList("channels.channel(" + channelIndex + ").dateFormats.dateFormat");
        return dateFormats;
    }

    /**
     * Get the total date formats count
     * @param channelIndex the desired channel index
     * @return totalDateFormats the total count of date formats
     */
    public int getTotalDateFormats(int channelIndex) {
        List<String> dateFormats = getDateFormats(channelIndex);
        return dateFormats.size();
    }

    /**
     * Get the record delimiter (line ending)
     * @param channelIndex the desired channel index
     * @return recordDelimiter the line ending string of characters
     */
    public String getRecordDelimiter(int channelIndex) {
        List<String> recordDelims = xmlConfig.getList(
            "channels.channel(" + channelIndex + ").recordDelimiters");

        StringBuilder recordDelimiters = new StringBuilder();
        for (String delim : recordDelims) {
            recordDelimiters.append(
                new String(new byte[] {Integer.decode(delim).byteValue()})
            );
        }
        return recordDelimiters.toString();
    }

    public String[] getRecordDelimiters(int channelIndex) {
        return xmlConfig.getStringArray(
            "channels.channel(" + channelIndex + ").recordDelimiters");
    }

    /**
     * Get the instrument connection type
     * @return connectionType the instrument connection type
     */
    public String getConnectionType() {
        return xmlConfig.getString("connectionType");
    }

    /**
     * Get the data file path for file text sources
     * @return filePath the path to the data file
     */
    public String getDataFilePath() {
        return xmlConfig.getString("connectionParams.filePath");
    }

    /**
     * Get the socket connection type parameter host name
     * @return hostName the host name
     */
    public String getHostNameConnectionParam() {
        return xmlConfig.getString("connectionParams.hostName");
    }

    /**
     * Get the socket connection type host port
     * @return hostPort the host port
     */
    public int getHostPortConnectionParam() {
        return xmlConfig.getInt("connectionParams.hostPort");
    }

    /**
     * Get the serial port connection parameter
     * @return serialPort the serial port setting
     */
    public String getSerialPortConnectionParam() {
        return xmlConfig.getString("connectionParams.serialPort");
    }

    /**
     * Get the serial port connection parameter
     * @return serialBaudRate the serial baud rate setting
     */
    public int getSerialBaudRateConnectionParam() {
        return xmlConfig.getInt("connectionParams.serialPortParams.baudRate");
    }

    /**
     * Get the serial data bits connection parameter
     * @return serialDataBits the serial data bits setting
     */
    public int getSerialDataBitsConnectionParam() {
        return xmlConfig.getInt("connectionParams.serialPortParams.dataBits");
    }

    /**
     * Get the serial stop bits connection parameter
     * @return serialStopBits the serial stop bits setting
     */
    public int getSerialStopBitsConnectionParam() {
        return xmlConfig.getInt("connectionParams.serialPortParams.stopBits");
    }

    /**
     * Get the serial parity connection parameter
     * @return serialParity the serial parity setting
     */
    public String getSerialParityConnectionParam() {
        return xmlConfig.getString("connectionParams.serialPortParams.parity");
    }

    /**
     * Get the list of channel configurations
     * @return channelConfigurations the list of channel configurations
     */
    public List<ChannelConfiguration> getChannelConfigurations() {
        return channelConfigurations;
    }

    /**
     * Set the list of channel configurations
     * @param channelConfigurations  the list of channel configurations
     */
    public void setChannelConfigurations(List<ChannelConfiguration> channelConfigurations) {
        this.channelConfigurations = channelConfigurations;
    }
}
