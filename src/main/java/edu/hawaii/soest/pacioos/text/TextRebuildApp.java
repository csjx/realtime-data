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
package edu.hawaii.soest.pacioos.text;

import edu.hawaii.soest.helpers.ConsoleColors;
import edu.hawaii.soest.pacioos.text.configure.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import tech.tablesaw.api.DateTimeColumn;
import tech.tablesaw.api.InstantColumn;
import tech.tablesaw.api.Table;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * An application that rebuilds raw and processed data files (hourly and daily)
 */
public class TextRebuildApp {

    /* Set up a log */
    private static final Log log = LogFactory.getLog(TextRebuildApp.class);

    /**
     * The main entrypoint
     * @param args the arguments array
     */
    public static void main(String[] args) {

        log.debug("Called TextRebuildApp.main().");

        /* The path to the configuration file */
        String xmlConfiguration = null;
        /* The filesystem path to process */
        String dataDirectoryPath = null;
        /* The path to the rebuild success log file */
        String rebuildSuccessFile = "success.log";
        /* The path to the rebuild errors log file */
        String rebuildErrorFile = "errors.log";
        /* The filesystem path where old files are moved if recovery is needed */
        String recoveryBasePath = "/backup/recovery";

        if (args.length != 2) {
            log.error("Please provide the path to the instrument's XML configuration file " +
                "as the first parameter, and a path to a data file to rebuild as the second parameter.");
            System.exit(1);
        } else {
            xmlConfiguration = args[0];
            dataDirectoryPath = args[1];
        }

        // Pull in the archiver rebuilder properties
        try {
            PropertiesConfiguration propsConfig = new PropertiesConfiguration("archive_rebuild.properties");
            rebuildSuccessFile = propsConfig.getString("rebuild.success.file");
            rebuildErrorFile = propsConfig.getString("rebuild.error.file");
            recoveryBasePath = propsConfig.getString("rebuild.recovery.path", "/backup/recovery");

        } catch (ConfigurationException e) {
            log.info("Couldn't get the archive_rebuild.properties correctly. " +
                "The message was: " + e.getMessage());
            if (log.isDebugEnabled() ) {
                e.printStackTrace();
            }
        }

        // Create a TextRebuilder instance
        TextRebuilder.Builder builder =
            TextRebuilder.builder(xmlConfiguration)
                .dataDirectory(dataDirectoryPath)
                .rebuildErrorFile(rebuildErrorFile)
                .rebuildSuccessFile(rebuildSuccessFile)
                .recoveryBasePath(recoveryBasePath);

        TextRebuilder rebuilder = builder.build();

        // Parse the configuration file
        Configuration config = rebuilder.getConfiguration();

        // Load the data file(s)
        List<Path> paths = rebuilder.getDataFilePaths();

        Table mergedTable = rebuilder.getMergedTable();

        if ( mergedTable != null ) {

            // Is the datetime column first or last?
            int firstDateField = Integer.parseInt(config.getDateFields(0).get(0));
            // Sort the table using the datetime field's corresponding index
            int sortColumnIndex = firstDateField <= 1 ? 0 : mergedTable.columnCount() - 1;

            // Deduplicate and sort rows of the merged table
            Table sortedTable = rebuilder.deduplicateAndSortTable(mergedTable, sortColumnIndex);

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

            log.info(
                ConsoleColors.BLUE +
                "Merged table preview:\n" +
                sortedTable +
                ConsoleColors.RESET
            );
            Instant startDateInUTC = instantColumn.min().truncatedTo(ChronoUnit.DAYS);
            Instant endDateInUTC = instantColumn.max().truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS );


            // Build the list of dates for this rebuild
            List<Instant> rebuildDatesInUTC = new ArrayList<>();
            while (!startDateInUTC.isAfter(endDateInUTC)) {
                rebuildDatesInUTC.add(startDateInUTC);
                startDateInUTC = startDateInUTC.plus(1, ChronoUnit.DAYS);
            }

            log.debug("Rebuild start date in UTC: " + rebuildDatesInUTC.get(0).toString());
            log.debug("Rebuild end date in UTC  : " + rebuildDatesInUTC.get(rebuildDatesInUTC.size() - 1).toString());

            try {
                // Move the existing data directories aside for potential recovery
                rebuilder.moveDirectories();

                // Remove the processed directory for the given instrument
                rebuilder.removeProcessedDirectory();

                // Build the new directories
                rebuilder.buildDirectories(rebuildDatesInUTC);

                // Write the processed and raw data files
                rebuilder.writeFiles(rebuildDatesInUTC, sortedTable);

                // Log the results of the processing
                rebuilder.reportResults(dataDirectoryPath, paths.size(), mergedTable.rowCount(),
                    sortedTable.rowCount());

            } catch (IOException e) {
                log.error("Couldn't create the archive directories. " +
                    "Please adjust the permissions. Message: " + e.getMessage());
                System.exit(1);
            }
        }
        // Clean up
        rebuilder.shutdownExecutorService();
    }
}
