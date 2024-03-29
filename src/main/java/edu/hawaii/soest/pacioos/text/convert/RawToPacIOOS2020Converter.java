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
package edu.hawaii.soest.pacioos.text.convert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import tech.tablesaw.api.BooleanColumn;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.DateColumn;
import tech.tablesaw.api.DateTimeColumn;
import tech.tablesaw.api.InstantColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.api.TimeColumn;
import tech.tablesaw.columns.datetimes.DateTimeColumnType;
import tech.tablesaw.columns.instant.InstantColumnFormatter;
import tech.tablesaw.columns.strings.StringColumnType;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.io.csv.CsvWriteOptions;
import tech.tablesaw.io.csv.CsvWriter;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class RawToPacIOOS2020Converter implements Converter {

    /* Set up a class logger */
    private final Log log = LogFactory.getLog(RawToPacIOOS2020Converter.class);

    /* The data frame table of samples to operate on */
    private Table table;

    /* The converted data table */
    private Table convertedTable = Table.create("convertedTable");

    /* The data table name */
    private String tableName;

    /* The data table field delimiter */
    private String fieldDelimiter;

    /* The data table missing value code */
    private String missingValueCode;

    /* The data table column types */
    private ColumnType[] columnTypes;

    /* The data table number of header lines */
    private int numberHeaderLines = 0;

    /*The sample date format string */
    private String dateFormat;

    /*The sample time format string */
    private String timeFormat;

    /*The sample date time format string */
    private String dateTimeFormat;

    /* The sample time zone identifier */
    private ZoneId timeZoneId;

    /* The data prefix of each sample line (e.g. #) */
    private String dataPrefix;

    /* The record delimiter (line ending) of the samples */
    private String recordDelimiter;

    /* A list of the date field column number positions*/
    private List<String> dateFields;

    /* The list of date format strings from the instrument configuration */
    private List<String> dateFormats;

    /**
     * Construct a RawToPacIOOS2020Converter
     */
    public RawToPacIOOS2020Converter() {
    }

    @Override
    public void parse(InputStream samples) throws IOException {

        String dateFormat = getDateFormat() != null ? getDateFormat() : "";
        String timeFormat = getTimeFormat() != null ? getTimeFormat() : "";
        boolean includeHeaderLines = getNumberHeaderLines() > 0;
        String missingValueCode = getMissingValueCode() != null ? getMissingValueCode() : "";
        String fieldDelimiter = getFieldDelimiter() != null ? getFieldDelimiter() : ",";

        // Make all dates a single column for SBE-16 like instruments
        String datePattern =
            "([0-9][0-9]) " +                // day dd
                "([A-Za-z][A-Za-z][A-Za-z]) " +  // mon MMM
                "([0-9][0-9][0-9][0-9]), " +     // year yyyy, note comma
                "([0-9][0-9]):" +                // hour HH
                "([0-9][0-9]):" +                // min mm
                "([0-9][0-9])";                  // sec ss
        String dateReplacement = "$1 $2 $3 $4:$5:$6"; // note no comma

        String ysiDatePattern =
            "([0-9][0-9])/" +                // mon MMM
                "([0-9][0-9])/" +                // day dd
                "([0-9][0-9][0-9][0-9]) " +      // year yyyy, note space
                "([0-9][0-9]):" +                // hour HH
                "([0-9][0-9]):" +                // min mm
                "([0-9][0-9])";                  // sec ss
        String ysiDateReplacement = "$1/$2/$3$4:$5:$6"; // note no space

        // Coerce the samples to into the correct table format
        String samplesString = new String(samples.readAllBytes(), StandardCharsets.UTF_8);
        samplesString = samplesString
            .replaceAll("\r", "")
            .replaceAll(",+", ",")
            .replaceAll(" +", " ")
            .replaceAll(" +$", "")
            .replaceAll(datePattern, dateReplacement)
            .replaceAll(ysiDatePattern, ysiDateReplacement);

        if ( samplesString.contains("#")) {
            samplesString = samplesString
                .replaceAll(", *", ",")
                .replaceAll("\n", "")
                .replaceAll("# ", "\n")
                .replaceFirst("\n", "");
        }
        samples.close();

        // Create a new InputStream for to load the data into tablesaw
        samples = new BufferedInputStream(
            new ByteArrayInputStream(samplesString.getBytes(StandardCharsets.UTF_8)));

        // If the date/time fields are column 1, this is YSI, don't use a space separator
        int firstDateField = Integer.parseInt(getDateFields().get(0));
        String dateTimeSeparator = firstDateField > 1 ? " " : "";
        String datetimeFormat = getDateTimeFormat();

        // Get or build the single datetime format
        if (datetimeFormat == null) {
            List<String> dateFormats = listDateFormats();
            datetimeFormat = "";
            for (int i = 0; i < dateFormats.size(); i++) {
                if (i == dateFormats.size() - 1) {
                    datetimeFormat = datetimeFormat.concat(dateFormats.get(i));
                } else {
                    datetimeFormat = datetimeFormat.concat(dateFormats.get(i) + dateTimeSeparator);
                }
            }
        }

        // Get the STRING column types, remove any DATE and TIME column types,
        // and append a single DATETIME column type
        ColumnType[] colTypes = getColumnTypes();
        List<ColumnType> columnTypes =
            Arrays.stream(colTypes).filter(columnType ->
                columnType.toString().equalsIgnoreCase("string")
            ).collect(Collectors.toList());

        // For YSI instruments, prepend the datetime. Others, append it.
        if ( dateTimeSeparator.isEmpty() ) {
            columnTypes.add(0, DateTimeColumnType.instance());
        } else {
            columnTypes.add(DateTimeColumnType.instance());
        }

        colTypes = new ColumnType[columnTypes.size()];
        columnTypes.toArray(colTypes);
        setColumnTypes(colTypes);

        // handle hex-encoded field delimiters
        if ( fieldDelimiter.startsWith("0x") || fieldDelimiter.startsWith("\\x" )) {

            byte delimBytes = Byte.parseByte(fieldDelimiter.substring(2), 16);
            byte[] delimAsByteArray = new byte[]{delimBytes};
            fieldDelimiter = new String(
                delimAsByteArray, 0,
                delimAsByteArray.length,
                StandardCharsets.US_ASCII);
            setFieldDelimiter(fieldDelimiter);
        }

        // TODO: Ensure non-null values here.
        CsvReadOptions.Builder builder =
            CsvReadOptions.builder(samples)
                .commentPrefix("|".charAt(0))
                .dateFormat(DateTimeFormatter.ofPattern(dateFormat))
                .timeFormat(DateTimeFormatter.ofPattern(timeFormat))
                .dateTimeFormat(DateTimeFormatter.ofPattern(datetimeFormat))
                .header(includeHeaderLines)
                .missingValueIndicator(missingValueCode)
                .sample(false)
                .separator(getFieldDelimiter().charAt(0))
                .tableName("Samples")
                .columnTypes(getColumnTypes());

        CsvReadOptions options = builder.build();

        setTable(Table.read().usingOptions(options));
    }

    /**
     * Convert the parsed samples to the PacIOOS 2020 format:
     * <ul>
     *     <li>Convert date strings to UTC in ISO 8601 format</li>
     *     <li>Move date strings to the first column</li>
     *     <li>Normalize white space</li>
     *     <li>Convert line endings to Unix linefeed (0x0A)</li>
     *     <li>Change missing values to -999</li>
     * </ul>
     *
     */
    @Override
    public void convert() {

        // Find the date, time, or datetime columns, and create an instant column
        DateTimeColumn[] dateTimeColumns = table.dateTimeColumns();
        TimeColumn[] timeColumns = table.timeColumns();
        DateColumn[] dateColumns = table.dateColumns();
        InstantColumn instantColumn = InstantColumn.create("instantColumn", table.rowCount());

        BooleanColumn uniqueValues = BooleanColumn.create("isUnique", table.rowCount());

        // Create an instant column from the datetime
        if ( dateTimeColumns.length > 0 ) {
            instantColumn = dateTimeColumns[0].asInstantColumn(getTimeZoneId());
            instantColumn.setPrintFormatter(new InstantColumnFormatter(DateTimeFormatter.ISO_INSTANT));
            getConvertedTable().addColumns(instantColumn);
        // Or create it from the date and time columns
        } else if ( dateColumns.length > 0 && timeColumns.length > 0 ) {
            instantColumn = dateColumns[0].atTime(timeColumns[0]).asInstantColumn(getTimeZoneId());
            instantColumn.setPrintFormatter(new InstantColumnFormatter(DateTimeFormatter.ISO_INSTANT));
            getConvertedTable().addColumns(instantColumn);

        } else {
            log.error("Couldn't convert table: no date, time, or datetime columns.");
            log.error(table.print());
            setConvertedTable(table);
        }

        // Strip the data prefix from the first column if it exists
        if ( getColumnTypes()[0] instanceof StringColumnType ) {
            StringColumn firstColumn = (StringColumn) table.column(0);
            StringColumn firstColumnTrimmed;
            if (table.column(0).anyMatch(
                value -> value.toString().startsWith(getDataPrefix()))) {
                firstColumnTrimmed = firstColumn
                    .trim()
                    .replaceAll(getDataPrefix(), "")
                    .trim();
                getConvertedTable().addColumns(firstColumnTrimmed);
            } else {
                getConvertedTable().addColumns(firstColumn);
            }
        }

        // Add the rest of the string columns
        AtomicInteger count = new AtomicInteger();
        table.columns().forEach(column -> {
            count.getAndIncrement();
            if (column.type() == ColumnType.STRING) {
                if ( count.get() == 1 ) {
                    log.debug("Skipping the first column.");
                } else {
                    // Add string columns other than the first
                    getConvertedTable().addColumns(column);
                }
            }
        });

        // Sort and dedupe the table and reset the converted table
        setConvertedTable(getConvertedTable().sortOn(0));
        uniqueValues.set(0, true); // The first row is always unique

        // Flag duplicate rows with false
        for (int row = 0; row < table.rowCount(); row++) {
            int nextRow = row + 1;
            if (nextRow < table.rowCount()) {
                Instant nextDateTime = instantColumn.get(nextRow);
                if (nextDateTime.equals(instantColumn.get(row))) {
                    uniqueValues.set(nextRow, false);
                } else {
                    uniqueValues.set(nextRow, true);
                }
            }
        }
        // Filter duplicates out of the table
        setConvertedTable(getConvertedTable().addColumns(uniqueValues));
        Table deduped = getConvertedTable().where(uniqueValues.asSelection());
        deduped.removeColumns("isUnique");
        setConvertedTable(deduped);
        log.debug(getConvertedTable().print());
    }
    
    @Override
    public int write(OutputStream outputStream) throws IOException {

        int rowCount = 0;

        CsvWriteOptions options = CsvWriteOptions.builder(outputStream)
            .ignoreLeadingWhitespaces(true)
            .ignoreTrailingWhitespaces(true)
            .header(false)
            .lineEnd(getRecordDelimiter())
            .quoteAllFields(false)
            .separator(",".charAt(0))
            .usePrintFormatters(true)
            .build();

        try {
            CsvWriter csvWriter = new CsvWriter();
            csvWriter.write(getConvertedTable(), options);
            // Reset the converted table for the next converter task run
            rowCount = getConvertedTable().rowCount();
            setConvertedTable(Table.create("convertedTable"));
        } catch (Exception e) {
            log.error("Couldn't write the converted table: " + e.getMessage());
            log.error(convertedTable.print());
            throw(e);
        }
        return rowCount;
    }

    /**
     * Get the data table
     * @return table the data table to convert
     */
    public Table getTable() {
        return table;
    }

    /**
     * Set the data table
     * @param table the data table to convert
     */
    public void setTable(Table table) {
        this.table = table;
    }

    /**
     * Get the converted data table
     * @return convertedTable the converted table
     */
    public Table getConvertedTable() {
        return convertedTable;
    }

    /**
     * Set the converted data table
     * @param convertedTable the converted table
     */
    public void setConvertedTable(Table convertedTable) {
        this.convertedTable = convertedTable;
    }

    /**
     * Get the data table name
     * @return tableName the table name
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Set the data table name
     * @param tableName the table name
     */
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    /**
     * Get the data table field delimiter
     * @return fieldDelimiter the field delimiter
     */
    public String getFieldDelimiter() {
        return fieldDelimiter;
    }

    /**
     * Set the data table field delimiter
     * @param fieldDelimiter the field delimitier
     */
    public void setFieldDelimiter(String fieldDelimiter) {
        this.fieldDelimiter = fieldDelimiter;
    }

    /**
     * Get the data table missing value code
     * @return missingValueCode the missing value code
     */
    public String getMissingValueCode() {
        return missingValueCode;
    }

    /**
     * Set the data table missing value code
     * @param missingValueCode the missing value code
     */
    public void setMissingValueCode(String missingValueCode) {
        this.missingValueCode = missingValueCode;
    }

    /**
     * Get the data table column types
     * @return columnTypes the column types array
     */
    public ColumnType[] getColumnTypes() {
        return columnTypes;
    }

    /**
     * Set the data table column types
     * @param columnTypes the column types array
     */
    public void setColumnTypes(ColumnType[] columnTypes) {
        this.columnTypes = columnTypes;
    }

    /**
     * Get the data table number of header lines
     * @return numberHeaderLines the number of header lines
     */
    public int getNumberHeaderLines() {
        return numberHeaderLines;
    }

    /**
     * Set the data table number of header lines
     * @param numberHeaderLines the number of header lines
     */
    public void setNumberHeaderLines(int numberHeaderLines) {
        this.numberHeaderLines = numberHeaderLines;
    }

    /**
     * Get the sample date format
     * @return dateFormat the sample date format
     */
    public String getDateFormat() {
        return dateFormat;
    }

    /**
     * Set the sample date format
     * @param dateFormat the sample date format
     */
    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    /**
     * Get the sample time format
     * @return timeFormat the sample time format
     */
    public String getTimeFormat() {
        return timeFormat;
    }

    /**
     * Set the sample time format
     * @param timeFormat the sample time format
     */
    public void setTimeFormat(String timeFormat) {
        this.timeFormat = timeFormat;
    }

    /**
     * Get the sample date time format
     * @return dateTimeFormat the sample date time format
     */
    public String getDateTimeFormat() {
        return dateTimeFormat;
    }

    /**
     * Set the sample date time format
     * @param dateTimeFormat the sample date time format
     */
    public void setDateTimeFormat(String dateTimeFormat) {
        this.dateTimeFormat = dateTimeFormat;
    }

    /**
     * Get the sample time zone identifier
     * @return timeZoneId the time zone identifier
     */
    public ZoneId getTimeZoneId() {
        return timeZoneId;
    }

    /**
     * Set the sample time zone identifier
     * @param timeZoneId the time zone identifier
     */
    public void setTimeZoneId(ZoneId timeZoneId) {
        this.timeZoneId = timeZoneId;
    }

    public List<String> getDateFields() {
        return this.dateFields;
    }

    public void setDateFields(List<String> dateFields) {
        this.dateFields = dateFields;
    }

    public List<String> listDateFormats() {
        return this.dateFormats;
    }

    public void setDateFormats(List<String> dateFormats) {
        this.dateFormats = dateFormats;
    }
    public String getDataPrefix() {
        return dataPrefix;
    }

    public void setDataPrefix(String dataPrefix) {
        this.dataPrefix = dataPrefix;
    }

    public String getRecordDelimiter() {
        return recordDelimiter;
    }

    public void setRecordDelimiter(String recordDelimiter) {
        this.recordDelimiter = recordDelimiter;
    }
}
