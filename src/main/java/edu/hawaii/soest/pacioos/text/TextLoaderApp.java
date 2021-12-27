/*
 *  Copyright: 2021 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
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
import com.rbnb.sapi.Source;
import edu.hawaii.soest.helpers.ConsoleColors;
import edu.hawaii.soest.pacioos.text.configure.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.DateTimeColumn;
import tech.tablesaw.api.InstantColumn;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * An application to load text-based data into the DataTurbine
 *
 *
 */
public class TextLoaderApp {

    /* Set up a log */
    private static final Log log = LogFactory.getLog(TextLoaderApp.class);

    /**
     * The main entrypoint
     * @param args the arguments array providing the path to the instrument config file
     *             and the path to the raw data directory
     */
    public static void main(String[] args) {

        /* The path to the configuration file */
        String xmlConfiguration = null;
        /* The filesystem path to process */
        String dataDirectoryPath = null;

        if (args.length != 2) {
            log.error("Please provide the path to the instrument's XML configuration file.");
            System.exit(1);
        } else {
            xmlConfiguration = args[0];
            dataDirectoryPath = args[1];

        }

        // Create a TextRebuilder instance
        TextRebuilder.Builder builder =
            TextRebuilder.builder(xmlConfiguration).dataDirectory(dataDirectoryPath);
        TextRebuilder rebuilder = builder.build();

        // Parse the configuration file
        Configuration config = rebuilder.getConfiguration();

        Table mergedTable = rebuilder.getMergedTable();

        if ( mergedTable != null ) {

            // Is the datetime column first or last?
            int firstDateField = Integer.parseInt(config.getDateFields(0).get(0));
            // Sort the table using the datetime field's corresponding index
            int sortColumnIndex = firstDateField <= 1 ? 0 : mergedTable.columnCount() - 1;

            // Deduplicate and sort rows of the merged table
            Table sortedTable = rebuilder.deduplicateAndSortTable(mergedTable, sortColumnIndex);

            // Get the last year of data based on the latest data in the archive
            Table filteredTable = getFilteredTable(config, firstDateField, sortedTable);

            log.info(
                ConsoleColors.BLUE +
                    "Reloading the latest year of data into the Data Turbine:\n" +
                    filteredTable +
                    ConsoleColors.RESET
            );
            log.debug("Filtered table count: " + filteredTable.rowCount());

            try {
                // Remove existing instrument data from the Data Turbine
                clearInstrumentData(config);

                // Reload the instrument data into the Data Turbine
                sendSamples(config, filteredTable);

            } catch (SAPIException e) {
                e.printStackTrace();
            }
        }
        // Clean up
        rebuilder.shutdownExecutorService();
    }

    private static Table getFilteredTable(Configuration config, int firstDateField, Table sortedTable) {
        DateTimeColumn dateTimeColumn;
        InstantColumn instantColumn;
        // Add a table Instant column for filtering by day and hour
        if ( firstDateField > 1 ) {
            dateTimeColumn = (DateTimeColumn) sortedTable.column(sortedTable.columns().size() - 1);
            instantColumn = dateTimeColumn.asInstantColumn(ZoneId.of(config.getTimeZoneID(0)));
            instantColumn.setName("datetimesInUTC");
            sortedTable.addColumns(instantColumn);
            // Drop the original date time column since it's no longer needed
            sortedTable.removeColumns(sortedTable.columnCount() - 2);
        } else {
            dateTimeColumn = (DateTimeColumn) sortedTable.column(0);
            instantColumn = dateTimeColumn.asInstantColumn(ZoneId.of(config.getTimeZoneID(0)));
            instantColumn.setName("datetimesInUTC");
            sortedTable.addColumns(instantColumn);
            // Drop the original date time column since it's no longer needed
            sortedTable.removeColumns(0);
        }

        Instant oneYearPriorDateInUTC = instantColumn.max().minus(365, ChronoUnit.DAYS);

        // Filter the table for the last year's worth of data starting from the most recent sample
        Table filteredTable = sortedTable.where(instantColumn.isAfter(oneYearPriorDateInUTC));
        return filteredTable;
    }

    private static void sendSamples(Configuration config, Table filteredTable) throws SAPIException {
        Source rbnbSource = new Source(config.getArchiveMemory(), "append", config.getArchiveSize());
         rbnbSource.OpenRBNBConnection(
             config.getServerName() + ":" + config.getServerPort(),
             config.getIdentifier()
         );
        // Create a channel map with two channels, raw and processed

        // Construct raw and processed datetime formatters based on the configuration info
        double sampleTimeAsSecondsSinceEpoch;
        List<String> dateFields = config.getDateFields(0);
        List<String> dateFormats = config.getDateFormats(0);
        int firstDateFieldPosition = Integer.parseInt(dateFields.get(0));
        String dateTimeSeparator = firstDateFieldPosition > 1 && dateFields.size() > 1 ? "," : " ";
        String timeZoneID = config.getTimeZoneID(0);
        String rawFieldDelimiter = config.getFieldDelimiter(0);
        // handle hex-encoded field delimiters
        if ( rawFieldDelimiter.startsWith("0x") || rawFieldDelimiter.startsWith("\\x" ) ||
            rawFieldDelimiter.startsWith("0X") || rawFieldDelimiter.startsWith("\\X" )) {

            byte delimBytes = Byte.parseByte(rawFieldDelimiter.substring(2), 16);
            byte[] delimAsByteArray = new byte[]{delimBytes};
            rawFieldDelimiter = new String(
                delimAsByteArray, 0,
                delimAsByteArray.length,
                StandardCharsets.UTF_8);
        }
        String processedFieldDelimiter = ",";
        String dataPrefix = config.getDataPrefix(0);
        String recordDelimiter = config.getRecordDelimiter(0);

        DateTimeFormatter rawFormatter;
        DateTimeFormatter processedFormatter = DateTimeFormatter.ISO_INSTANT;
        StringBuilder dateFormatsStr;
        if ( dateFields.size() == 1 ) {
            rawFormatter =
                DateTimeFormatter.ofPattern(dateFormats.get(0)).withZone(ZoneId.of(timeZoneID));
        } else {
            dateFormatsStr = new StringBuilder();
            int index = 0;
            for (String dateFormat : dateFormats) {
                    dateFormatsStr.append(dateFormat);
                    if (index < dateFormats.size() - 1) {
                        dateFormatsStr.append(dateTimeSeparator);
                    }
                    index++;
            }
            rawFormatter = DateTimeFormatter.ofPattern(dateFormatsStr.toString());
        }

        ChannelMap rbnbChannelMap = new ChannelMap();
        int rawChannelIndex = rbnbChannelMap.Add(config.getChannelName(0));
        int convertedChannelIndex = rbnbChannelMap.Add("PacIOOS2020Format");
        rbnbChannelMap.PutMime(rawChannelIndex, "text/plain");
        rbnbChannelMap.PutMime(convertedChannelIndex, "text/plain");

        for (Row row : filteredTable) {
            StringBuilder rawSample = new StringBuilder(); // Most performant inside loop per SO article
            StringBuilder processedSample = new StringBuilder();
            List<Column<?>> columns = filteredTable.columns();
            if ( dataPrefix != null && ! dataPrefix.isEmpty() ) {
                rawSample.append(dataPrefix);
                rawSample.append(" ");
            }

            Instant sampleInstant = row.getInstant("datetimesInUTC");
            sampleTimeAsSecondsSinceEpoch = sampleInstant.getEpochSecond();
            rbnbChannelMap.PutTime(sampleTimeAsSecondsSinceEpoch, 0d);
            // Build the raw date/time columns or datetime column, determine column position
            if ( firstDateFieldPosition == 1 ) {
                rawSample.append(rawFormatter.format(sampleInstant.atZone(ZoneOffset.ofHours(0))));
                rawSample.append(rawFieldDelimiter);
                rawSample.append(" ");
                processedSample.append(processedFormatter.format(sampleInstant));
                processedSample.append(processedFieldDelimiter);

                // Append each raw string data column value after the datetime column
                int index = 0;
                for (Column<?> column : columns) {
                    if ( column.type() == ColumnType.STRING ) {
                        rawSample.append(row.getString(column.name()));
                        processedSample.append(row.getString(column.name()));

                        // Add the field delimiter to all but the last column, adjust for the datetime column
                        if ( index < columns.size() - 2 ) {
                            rawSample.append(rawFieldDelimiter);
                            rawSample.append(" ");
                            processedSample.append(processedFieldDelimiter);
                        }
                        index++;
                    }
                }
                rawSample.append(recordDelimiter);
                processedSample.append("\n");

                log.trace(rawSample.toString().trim());
                log.trace(processedSample.toString());
                // Add the raw and processed sample to the channel map
                rbnbChannelMap.PutDataAsString(rawChannelIndex, rawSample.toString());
                rbnbChannelMap.PutDataAsString(convertedChannelIndex, processedSample.toString());

            } else {
                // Append each raw data column before the date time for raw data only
                processedSample.append(processedFormatter.format(sampleInstant));
                processedSample.append(processedFieldDelimiter);

                int index = 0;
                for (Column<?> column : columns) {
                    if ( column.type() == ColumnType.STRING ) {
                        rawSample.append(row.getString(column.name()));
                        processedSample.append(row.getString(column.name()));

                        // Add the field delimiter to all but the last column, adjust for the datetime column
                        if ( index < columns.size() - 2 ) {
                            rawSample.append(rawFieldDelimiter);
                            rawSample.append(" ");
                            processedSample.append(processedFieldDelimiter);
                        } else if ( index < columns.size() - 1) {
                            rawSample.append(rawFieldDelimiter);
                            rawSample.append(" ");
                        }
                        index++;
                    }
                }
                rawSample.append(rawFormatter.format(sampleInstant));
                rawSample.append(recordDelimiter);
                processedSample.append("\n");

                log.trace(rawSample.toString().trim());
                log.trace(processedSample.toString());
                // Add the raw and processed sample to the channel map
                rbnbChannelMap.PutDataAsString(rawChannelIndex, rawSample.toString());
                rbnbChannelMap.PutDataAsString(convertedChannelIndex, processedSample.toString());
            }
        }

        // Flush channelmap
        log.info("Sending " + config.getIdentifier() + " data to " + config.getServerName());
        int sent = rbnbSource.Flush(rbnbChannelMap, true);
        // Disconnect
        rbnbSource.Detach();
        log.info("Reloaded the latest year of " + config.getIdentifier() +
            " data to the Data Turbine at " + config.getServerName());
    }

    /**
     * Remove Data Turbine channel data for the given instrument by resetting the ring buffer
     * @param config the instrument XML configuration
     * @throws SAPIException a Data Turbine exception
     */
    private static void clearInstrumentData(Configuration config) throws SAPIException {
        log.info("Clearing instrument data in the Data Turbine for " + config.getIdentifier());
        Source rbnbSource = new Source(config.getArchiveMemory(), "create", config.getArchiveSize());
        rbnbSource.OpenRBNBConnection(
            config.getServerName() + ":" + config.getServerPort(),
            config.getIdentifier()
        );
        rbnbSource.CloseRBNBConnection();
    }
}
