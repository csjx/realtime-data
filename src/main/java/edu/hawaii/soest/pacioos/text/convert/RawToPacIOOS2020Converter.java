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
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;

public class RawToPacIOOS2020Converter implements Converter {

    /* Set up a class logger */
    private final Log log = LogFactory.getLog(RawToPacIOOS2020Converter.class);

    /* The data frame table of samples to operate on */
    private Table table;

    /* The converted data table */
    private Table convertedTable;

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
    private String timeZoneId;

    /**
     * Construct a RawToPacIOOS2020Converter
     */
    public RawToPacIOOS2020Converter() {
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

        // TODO: Convert the samples
        // convertedTable = ...

    }

    @Override
    public void parse(InputStream samples) throws IOException {

        CsvReadOptions.Builder builder =
            CsvReadOptions.builder(samples)
                .dateFormat(DateTimeFormatter.ofPattern(getDateFormat()))
                .timeFormat(DateTimeFormatter.ofPattern(getTimeFormat()))
                .dateTimeFormat(DateTimeFormatter.ofPattern(getDateTimeFormat()))
                .header(getNumberHeaderLines() > 0)
                .missingValueIndicator(getMissingValueCode())
                .sample(false)
                .separator(getFieldDelimiter().charAt(0))
                .tableName("Samples")
                .columnTypes(getColumnTypes());

        CsvReadOptions options = builder.build();

        Table samplesTable = Table.read().usingOptions(options);


    }

    @Override
    public int write(OutputStream outputStream) {

        // TODO: Write the table to the output stream
        return getConvertedTable().rowCount();
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
    public String getTimeZoneId() {
        return timeZoneId;
    }

    /**
     * Set the sample time zone identifier
     * @param timeZoneId the time zone identifier
     */
    public void setTimeZoneId(String timeZoneId) {
        this.timeZoneId = timeZoneId;
    }
}
