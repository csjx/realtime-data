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

import edu.hawaii.soest.pacioos.text.configure.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.datetimes.DateTimeColumnType;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * A task that produces a table from a single data file,
 * producing a Table result with the table and the path to the file
 */
public class ReaderTask implements Callable<ReadResult> {

    /* Set up a log */
    private final Log log = LogFactory.getLog(ReaderTask.class);

    /* The path to the file that the table with be created from*/
    private final Path path;

    /* The instrument configuration used to build the table */
    private final Configuration config;

    /* The table created from the path */
    private ReadResult readResult;

    public ReaderTask(Path filePath, Configuration config) {
        this.path = filePath;
        this.config = config;
    }

    /**
     * Generates a table, or throws an exception if unable to do so.
     * @return table the generated table
     * @throws Exception if unable to generate a table
     */
    @Override
    public ReadResult call() throws Exception {


        ReadResult readResult = new ReadResult(getPath());

        try {
            if (log.isDebugEnabled()) {
                log.debug("Generating table for " + this.path.toString());
            }
            InputStream samples = new BufferedInputStream(
                new FileInputStream(path.toString()));

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
                .replaceAll(" +\r*\n+", "\n")
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
            int firstDateField = Integer.parseInt(config.getDateFields(0).get(0));
            String dateTimeSeparator = firstDateField > 1 ? " " : "";
            String datetimeFormat = config.getDateTimeFormat(0);

            // Get or build the single datetime format
            if (datetimeFormat == null) {
                List<String> dateFormats = config.listDateFormats(0);
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
            ColumnType[] colTypes = config.getColumnTypes(0);
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

            String fieldDelimiter = config.getFieldDelimiter(0);
            // handle hex-encoded field delimiters
            if ( fieldDelimiter.startsWith("0x") || fieldDelimiter.startsWith("\\x" )) {

                byte delimBytes = Byte.parseByte(fieldDelimiter.substring(2), 16);
                byte[] delimAsByteArray = new byte[]{delimBytes};
                fieldDelimiter = new String(
                    delimAsByteArray, 0,
                    delimAsByteArray.length,
                    StandardCharsets.US_ASCII);
            }

            CsvReadOptions.Builder builder =
                CsvReadOptions.builder(samples)
                    .commentPrefix("|".charAt(0))
                    .lineEnding("\n")
                    .header(false)
                    .missingValueIndicator(config.getMissingValueCode(0))
                    .sample(false)
                    .separator(fieldDelimiter.charAt(0))
                    .tableName(getPath().toString())
                    .columnTypes(colTypes);
            builder.dateTimeFormat(DateTimeFormatter.ofPattern(datetimeFormat));

            CsvReadOptions options = builder.build();
            Table table = Table.read().usingOptions(options);
            readResult.setTable(table);

            samples.close();

            if (log.isDebugEnabled()) {
                log.debug("Generated table for " + this.path);
            }

        } catch (Exception e) {

            String msg = "For " + config.getIdentifier() + ", path " + path.toString() +
                ". Couldn't generate the table correctly. The message was: " +
                e.getMessage();
            log.info(msg);
            throw e;
        }
        return readResult;
    }

    /**
     * Get the path to the data file being processed
      * @return path the data file path
     */
    public Path getPath() {
        return path;
    }

    /**
     * Get the instrument configuration for this task
     * @return the instrument configuration
     */
    public Configuration getConfig() {
        return config;
    }

    /**
     * Get the ReadResult associated with the task
     * @return readResult the table result
     */
    public ReadResult getReadResult() {
        return readResult;
    }
}
