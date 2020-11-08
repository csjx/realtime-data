/*
 *  Copyright: 2020 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
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

import tech.tablesaw.api.ColumnType;

import java.util.List;

/**
 * Represents the channel configuration details as defined in
 * the instrument XML configuration file.
 */
public class ChannelConfiguration {


    /* If the channel is the default */
    private boolean defaultChannel;

    /* The channel name */
    private String name;

    /* The channel data type */
    private String dataType;

    /* The channel data pattern */
    private String dataPattern;

    /* The channel data prefix */
    private String dataPrefix;

    /* The channel column types */
    private ColumnType[] columnTypes;

    /* The channel field delimiter */
    private String fieldDelimiter;

    /* The channel record delimiter */
    private String recordDelimiter;

    /* The channel date formats */
    private List<String> dateFormats;

    /* The channel date fields */
    private List<String> dateFields;

    /* The channel timezone */
    private String timeZone;

    /* The list of channel archiver configurations*/
    private List<ArchiverConfiguration> archiverConfigurations;

    /**
     * Construct an empty channel configuration
     */
    public ChannelConfiguration() {

    }

    /**
     * Construct a channel configuration
     * @param defaultChannel if the channel is the default
     * @param name the channel name
     * @param dataType the channel data type
     * @param dataPattern the channel data pattern
     * @param dataPrefix the channel data prefix
     * @param columnTypes the channel column types
     * @param fieldDelimiter the channel field delimiter
     * @param recordDelimiter the channel record delimiters
     * @param dateFormats the channel date formats
     * @param dateFields the channel date fields
     * @param timeZone the channel timezone
     * @param archiverConfigurations the list of archiver configurations
     */
    public ChannelConfiguration(boolean defaultChannel, String name, String dataType,
        String dataPattern, String dataPrefix, ColumnType[] columnTypes,
        String fieldDelimiter, String recordDelimiter, List<String> dateFormats,
        List<String> dateFields, String timeZone, List<ArchiverConfiguration> archiverConfigurations) {
        this.defaultChannel = defaultChannel;
        this.name = name;
        this.dataType = dataType;
        this.dataPattern = dataPattern;
        this.dataPrefix = dataPrefix;
        this.columnTypes = columnTypes;
        this.fieldDelimiter = fieldDelimiter;
        this.recordDelimiter = recordDelimiter;
        this.dateFormats = dateFormats;
        this.dateFields = dateFields;
        this.timeZone = timeZone;
        this.archiverConfigurations = archiverConfigurations;
    }

    /**
     * Get the default channel status
     * @return defaultChannel true if this is the default channel
     */
    public boolean isDefaultChannel() {
        return defaultChannel;
    }

    /**
     * Set if this is the default channel
     * @param defaultChannel the default channel status
     */
    public void setDefaultChannel(boolean defaultChannel) {
        this.defaultChannel = defaultChannel;
    }

    /**
     * Get the channel name
     * @return name the channel name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the channel name
     * @param name  the channel name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the channel data type
     * @return dataType the channel data type
     */
    public String getDataType() {
        return dataType;
    }

    /**
     * Set the channel data type
     * @param dataType the channel data type
     */
    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    /**
     * Get the channel data pattern
     * @return dataPattern the channel data pattern
     */
    public String getDataPattern() {
        return dataPattern;
    }

    /**
     * Set the channel data pattern
     * @param dataPattern  the channel data pattern
     */
    public void setDataPattern(String dataPattern) {
        this.dataPattern = dataPattern;
    }

    /**
     * Get the channel dataPrefix
     * @return dataPrefix the channel data prefix
     */
    public String getDataPrefix() {
        return dataPrefix;
    }

    /**
     * Set the channel data prefix
     * @param dataPrefix the channel data prefix
     */
    public void setDataPrefix(String dataPrefix) {
        this.dataPrefix = dataPrefix;
    }

    /**
     * Get the channel columnTypes
     * @return columnTypes the channel column types
     */
    public ColumnType[] getColumnTypes() {
        return columnTypes;
    }

    /**
     * Set the channel column types
     * @param columnTypes the channel column types
     */
    public void setColumnTypes(ColumnType[] columnTypes) {
        this.columnTypes = columnTypes;
    }

    /**
     * Get the channel field delimiter
     * @return fieldDelimiter the channel field delimiter
     */
    public String getFieldDelimiter() {
        return fieldDelimiter;
    }

    /**
     * Set the channel field delimiter
     * @param fieldDelimiter  the channel field delimiter
     */
    public void setFieldDelimiter(String fieldDelimiter) {
        this.fieldDelimiter = fieldDelimiter;
    }

    /**
     * Get the channel record delimiters
     * @return recordDelimiter the channel record delimiter
     */
    public String getRecordDelimiters() {
        return recordDelimiter;
    }

    /**
     * Set the channel record delimiter
     * @param recordDelimiter  the channel record delimiter
     */
    public void setRecordDelimiters(String recordDelimiter) {
        this.recordDelimiter = recordDelimiter;
    }

    /**
     * Get the channel date formats
     * @return dateFormats the channel date formats
     */
    public List<String> getDateFormats() {
        return dateFormats;
    }

    /**
     * Set the channel date formats
     * @param dateFormats  the channel date formats
     */
    public void setDateFormats(List<String> dateFormats) {
        this.dateFormats = dateFormats;
    }

    /**
     * Get the channel date fields
     * @return dateFields the channel date fields
     */
    public List<String> getDateFields() {
        return dateFields;
    }

    /**
     * Set the channel date fields
     * @param dateFields  the channel date fields
     */
    public void setDateFields(List<String> dateFields) {
        this.dateFields = dateFields;
    }

    /**
     * Get the channel time zone
     * @return timeZone the channel time zone
     */
    public String getTimeZone() {
        return timeZone;
    }

    /**
     * Set the channel time zone
     * @param timeZone  the channel time zone
     */
    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    /**
     * Get the channel archiver configurations
     * @return archiverConfigurations the channel archiver configurations
     */
    public List<ArchiverConfiguration> getArchiverConfigurations() {
        return archiverConfigurations;
    }

    /**
     * Set the channel archiver configurations
     * @param archiverConfigurations the channel archiver configurations
     */
    public void setArchiverConfigurations(List<ArchiverConfiguration> archiverConfigurations) {
        this.archiverConfigurations = archiverConfigurations;
    }

    public int getTotalArchivers() {
        int totalArchivers = 0;
        if ( getArchiverConfigurations() != null ) {
            totalArchivers = getArchiverConfigurations().size();
        }
        return totalArchivers;
    }
}
