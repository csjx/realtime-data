/*
 *  Copyright: 2020 Regents of the University of Hawaii and the
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
package edu.hawaii.soest.pacioos.text.concurrent;

import edu.hawaii.soest.helpers.ConsoleColors;
import edu.hawaii.soest.pacioos.text.configure.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import tech.tablesaw.api.InstantColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.instant.PackedInstant;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Represents a task for writing a table of samples to disk
 */
public class WriterTask implements Callable<WriteResult> {

    Log log = LogFactory.getLog(WriterTask.class);

    /* The begin date of the samples to write */
    private final Instant beginDate;
    /* The end date of the samples to write */
    private final Instant endDate;
    /* The base path to the base instrument folder for writing */
    private final Path basePath;
    /* The full sorted table of all samples to query samples from */
    private final Table sortedTable;
    /* The instrument identifier */
    private final Configuration config;
    /* The filtered result table */
    private Table table;
    /* The archive format to write, raw or pacioos-2020-format */
    private final String archiveFormat;

    /**
     * Construct a writer task
     * @param beginDate the begin date of the samples to write
     * @param endDate the end date of the samples to write
     * @param basePath the base path to the base instrument folder for writing
     * @param config the instrument configuration
     * @param archiveFormat the archive format - raw or pacioos-2020-format
     */
    public WriterTask(Instant beginDate, Instant endDate,
                      Path basePath, Table sortedTable, Configuration config, String archiveFormat) {
        this.beginDate = beginDate;
        this.endDate = endDate;
        this.basePath = basePath;
        this.sortedTable = sortedTable;
        this.config = config;
        this.archiveFormat = archiveFormat;
    }
    /**
     * Generates a write result, or throws an exception if unable to do so.
     *
     * @return the write result
     * @throws Exception if unable to generate a write result
     */
    @Override
    public WriteResult call() throws Exception {

        ZonedDateTime zonedDateTime = beginDate.atZone(ZoneOffset.ofHours(0));
        // Build a file suffix
        String dateStr = zonedDateTime.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String suffix = ".dat";

        String year = String.format("%4d", zonedDateTime.getYear());
        String month = String.format("%02d", zonedDateTime.getMonthValue());
        String day = String.format("%02d", zonedDateTime.getDayOfMonth());
        Path filePath = Paths.get(
            basePath.toString(), config.getIdentifier() + "_" + dateStr + suffix);

        // Find table rows with dates between the begin and end dates
        InstantColumn datesInUTC =
            sortedTable.instantColumn(sortedTable.columns().size() - 1);
        Table currentDayTable = sortedTable.where(
            datesInUTC.isBetweenIncluding(
                PackedInstant.pack(beginDate),
                PackedInstant.pack(endDate)
            ));
        currentDayTable.setName(beginDate.toString() + " to " + endDate.toString());

        WriteResult writeResult = new WriteResult(filePath);

        // If there are no data, return with a message
        if ( currentDayTable.rowCount() < 1 ) {
            writeResult.setTable(currentDayTable);
            writeResult.setMessage("No data samples for this time period.");
            return writeResult;
        }

        // Write the data to the file path after locking it
        FileLock fileLock = null;
        Table rawTable = Table.create(currentDayTable.name());
        Table processedTable = Table.create(currentDayTable.name());
        StringBuilder sample = new StringBuilder();
        DateTimeFormatter processedFormatter = DateTimeFormatter.ISO_INSTANT;

        // Get the raw date format from the config
        List<String> dateFormats = config.listDateFormats(0);
        StringBuilder pattern = new StringBuilder();
        if (dateFormats.size() > 1) {
            // Handle multi-column date/times
            pattern.append(config.getDateFormat(0));
            pattern.append("'");
            pattern.append(config.getFieldDelimiter(0));
            pattern.append(" '");
            pattern.append(config.getTimeFormat(0));
        } else {
            // Handle single column date times
            pattern.append(config.getDateTimeFormat(0));
        }

        try {
            log.trace("For path " + filePath + " date time format is: " + pattern.toString());

            DateTimeFormatter rawFormatter = DateTimeFormatter.ofPattern(pattern.toString());

            // Handle raw and processed files separately
            if ( archiveFormat.equals("raw") ) {
                FileChannel rawFileChannel = FileChannel.open(filePath,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE
                    );
                fileLock = rawFileChannel.lock();
                currentDayTable.stream().iterator().forEachRemaining(row -> {
                    // Append the hashtag
                    sample.append("#  ");
                    // Append each measurement column
                    for (int index = 0; index <= row.columnCount() - 2; index++ ) {
                        sample.append(row.getString(index));
                        sample.append(config.getFieldDelimiter(0));
                        sample.append(" ");
                    }
                    // Append the ISO timestamp
                    String timestamp = rawFormatter.format(
                        row.getInstant(row.columnCount() - 1)
                            .atZone(ZoneId.of(config.getTimeZoneID(0)))
                    );
                    sample.append(timestamp);
                    sample.append("\r\n");

                    // Write the sample file
                    try {
                        rawFileChannel.write(
                            ByteBuffer.wrap(sample.toString().getBytes(StandardCharsets.UTF_8))
                        );
                    } catch (IOException e) {
                        log.error(
                            ConsoleColors.RED +
                            e.toString() +
                            ConsoleColors.RESET
                        );
                        writeResult.setTable(currentDayTable);
                        writeResult.setMessage(e.toString());
                        sample.delete(0, sample.length()); // Clear the sample on error
                    }
                    // Clear the sample for the next row
                    sample.delete(0, sample.length());
                });
            } else if ( archiveFormat.equals("pacioos-2020-format") ) {
                FileChannel processedFileChannel = FileChannel.open(filePath,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE
                );
                fileLock = processedFileChannel.lock();
                currentDayTable.stream().iterator().forEachRemaining(row -> {
                    // Append the ISO timestamp
                    String timestamp = processedFormatter.format(
                        row.getInstant(row.columnCount() - 1)
                    );
                    sample.append(timestamp);
                    sample.append(config.getFieldDelimiter(0));
                    // Append each measurement column
                    for (int index = 0; index <= row.columnCount() - 2; index++ ) {
                        sample.append(row.getString(index));
                        if ( index == row.columnCount() - 2 ) {
                            sample.append("\n");
                        } else {
                            sample.append(config.getFieldDelimiter(0));
                        }
                    }
                    // Write the sample file
                    try {
                        processedFileChannel.write(
                            ByteBuffer.wrap(sample.toString().getBytes(StandardCharsets.UTF_8))
                        );
                    } catch (IOException e) {
                        log.error(
                            ConsoleColors.RED +
                            e.toString() +
                            ConsoleColors.RESET
                        );
                        writeResult.setTable(currentDayTable);
                        writeResult.setMessage(e.toString());
                        sample.delete(0, sample.length()); // Clear the sample on error
                    }
                    // Clear the sample for the next row
                    sample.delete(0, sample.length());
                });
            } else {
                log.warn(
                    ConsoleColors.YELLOW +
                    "Didn't recognize archive format: " + archiveFormat + ". Skipping." +
                    ConsoleColors.RESET);
            }
            writeResult.setMessage("COMPLETE");

        } catch (IOException e) {
            log.error(
                ConsoleColors.RED +
                    e.toString() +
                    ConsoleColors.RESET
            );
            writeResult.setTable(currentDayTable);
            writeResult.setMessage(e.toString());
            return writeResult;
        } catch (IllegalArgumentException iae) {
            log.error(
                ConsoleColors.RED +
                    iae.toString() +
                    ConsoleColors.RESET
            );
            writeResult.setMessage(iae.toString());
            return writeResult;

        } finally {
            if ( fileLock != null ) {
                fileLock.close();
            }
        }

        log.debug("Wrote " + currentDayTable.name() + ", " + currentDayTable.rowCount() + " samples.");

        writeResult.setTable(currentDayTable);
        return writeResult;
    }

    /**
     * Get the begin date
     * @return the begin date
     */
    public Instant getBeginDate() {
        return beginDate;
    }

    /**
     * Get the end date
     * @return the end date
     */
    public Instant getEndDate() {
        return endDate;
    }

    /**
     * Get the base path
     * @return the base path
     */
    public Path getBasePath() {
        return basePath;
    }

    /**
     * Get the full sorted table
     * @return the full sorted table
     */
    public Table getSortedTable() {
        return sortedTable;
    }

    /**
     * Get the filtered table
     * @return the filtered table
     */
    public Table getTable() {
        return table;
    }
}
