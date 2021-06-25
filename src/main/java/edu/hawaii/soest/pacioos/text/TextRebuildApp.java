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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.hawaii.soest.helpers.ConsoleColors;
import edu.hawaii.soest.pacioos.text.concurrent.ReadResult;
import edu.hawaii.soest.pacioos.text.concurrent.WriteResult;
import edu.hawaii.soest.pacioos.text.concurrent.WriterTask;
import edu.hawaii.soest.pacioos.text.configure.ArchiverConfiguration;
import edu.hawaii.soest.pacioos.text.configure.ChannelConfiguration;
import edu.hawaii.soest.pacioos.text.configure.Configuration;
import edu.hawaii.soest.pacioos.text.concurrent.ReaderTask;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import tech.tablesaw.api.BooleanColumn;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.DateTimeColumn;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.FloatColumn;
import tech.tablesaw.api.InstantColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * An application that rebuilds raw and processed data files (hourly and daily)
 */
public class TextRebuildApp {

    /* The filesystem path to process */
    private static String path;

    /* The filesystem path where old files are moved if recovery is needed */
    private static String recoveryPath = "/backup/recovery";

    /* The instrument configuration */
    private static Configuration config;

    /* The initial table */
    private static Table table;

    /* The merged table */
    private static Table mergedTable;

    /* The sorted table */
    private static Table sortedTable;

    /* The de-duplicated table */
    private static Table dedupedTable;

    /* An executor service to process all tasks. Uses all processors except one */
    private static ExecutorService executor = new ThreadPoolExecutor(
        Runtime.getRuntime().availableProcessors() - 1,
        Runtime.getRuntime().availableProcessors() - 1,
        0L,
        TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>()
    );

    /* The list of file paths to process */
    private static List<Path> paths = new ArrayList<>();

    /* A map of read task results for reporting */
    private static Map<String, String> completedReadTasks = new HashMap<>();

    /* A map of write task results for reporting */
    private static Map<String, String> completedWriteTasks = new HashMap<>();

    /* Set up a log */
    private static final Log log = LogFactory.getLog(TextRebuildApp.class);

    /* The list of date time instants associated with the rebuild */
    private static List<Instant> rebuildDatesInUTC = new ArrayList<>();

    /* A file processing read queue */
    private static BlockingQueue<Future<ReadResult>> readQueue = new LinkedBlockingQueue<>();

    /* A file processing write queue */
    private static BlockingQueue<Future<WriteResult>> writeQueue = new LinkedBlockingQueue<>();

    /* The path to the rebuild success log file */
    private static String rebuildSuccessFile = "success.log";

    /* The path to the rebuild errors log file */
    private static String rebuildErrorFile = "errors.log";

    /**
     * The main entrypoint
     * @param args the arguments array
     */
    public static void main(String[] args) {
        // Pull in rebuilder properties
        try {
            PropertiesConfiguration propsConfig = new PropertiesConfiguration("archive_rebuild.properties");
            rebuildSuccessFile = propsConfig.getString("rebuild.success.file");
            rebuildErrorFile = propsConfig.getString("rebuild.error.file");

        } catch (ConfigurationException e) {
            e.printStackTrace();
        }


        String xmlConfiguration = null;
        path = null;


        if (args.length != 2) {
            log.error("Please provide the path to the instrument's XML configuration file " +
                "as the first parameter, and a path to a data file to rebuild as the second parameter.");
            System.exit(1);
        } else {
            xmlConfiguration = args[0];
            path = args[1];
        }

        // Parse the configuration file
        config = getConfiguration(xmlConfiguration);

        // Load the data file(s)
        paths = getDataFilePaths(path);

        mergedTable = getMergedTable();

        if ( mergedTable != null ) {

            // Deduplicate and sort rows of the merged table
            sortedTable = deduplicateAndSortTable(mergedTable);

            // Add a table Instant column for filtering by day and hour
            DateTimeColumn dateTimeColumn =
                (DateTimeColumn) sortedTable.column(sortedTable.columns().size() - 1);
            InstantColumn instantColumn =
                dateTimeColumn.asInstantColumn(ZoneId.of(config.getTimeZoneID(0)));
            instantColumn.setName("datetimesInUTC");
            sortedTable.addColumns(instantColumn);
            // Drop the original date time column since it's no longer needed
            sortedTable.removeColumns(sortedTable.columnCount() - 2);
            log.info(
                ConsoleColors.BLUE +
                "Merged table preview:\n" +
                sortedTable +
                ConsoleColors.RESET
            );
            Instant startDateInUTC = instantColumn.min().truncatedTo(ChronoUnit.DAYS);
            Instant endDateInUTC = instantColumn.max().truncatedTo(ChronoUnit.DAYS);


            // Build the list of dates for this rebuild
            while (!startDateInUTC.isAfter(endDateInUTC)) {
                rebuildDatesInUTC.add(startDateInUTC);
                startDateInUTC = startDateInUTC.plus(1, ChronoUnit.DAYS);
            }

            try {

                // Move the existing data directories aside for potential recovery
                moveDirectories();

                // Build the new directories
                buildDirectories();

                // Write the processed and raw data files
                writeFiles();


            } catch (IOException e) {
                log.error("Couldn't create the archive directories. " +
                    "Please adjust the permissions. Message: " + e.getMessage());
                System.exit(1);
            }
        }

        // Log the results of the processing
        reportResults();

        // Clean up
        shutdownExecutorService();

    }

    /**
     * Check that the given recovery path exists and is writable
     * @param path the recovery path to move existing data to
     * @return true if the path is valid
     */
    private static boolean recoveryPathIsValid(String path) {
        boolean valid = false;
        Path recoveryPath = Paths.get(path);
        valid = Files.exists(recoveryPath);
        valid = Files.isReadable(recoveryPath);
        valid = Files.isWritable(recoveryPath);

        return valid;
    }

    /*
     * Move the original files to a recovery directory in case of major errors
     */
    private static void moveDirectories() {
        // Get a relative path of the given data path
        Path relativePath = Paths.get("/").relativize(Paths.get(path));

        String recoveryTimestamp =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm")
                .format(Instant.now().atZone(ZoneOffset.ofHours(0)));
        // When the default recovery directory is unavailable, use the home directory
        if (!recoveryPathIsValid(recoveryPath)) {
            recoveryPath = String.join(
                FileSystems.getDefault().getSeparator(),
                System.getProperty("user.home"),
                "recovery",
                recoveryTimestamp,
                relativePath.toString()
            );
        } else {
            recoveryPath = Paths.get(recoveryPath, recoveryTimestamp, relativePath.toString()).toString();
        }

        // Move the original directory with files to the recovery directory
        Path recoveryDirectory = Paths.get(recoveryPath);
        try {
            // Make the recovery directories as needed
            if ( ! Files.exists(recoveryDirectory) ) {
                new File(recoveryDirectory.toString()).mkdirs();
            }
            Files.move(Paths.get(path),  recoveryDirectory, StandardCopyOption.REPLACE_EXISTING);
            log.info(
                ConsoleColors.BLUE +
                "\nMoved\n" + path +"\nto recovery directory:\n" + recoveryPath +
                ConsoleColors.RESET);

        } catch (IOException e) {
            log.error(
                ConsoleColors.RED +
                "Couldn't create or write to the recovery directories. " +
                "Please adjust the permissions. Message: " + e.getMessage() +
                ConsoleColors.RESET
            );
            System.exit(1);
        }
    }

    /*
     * Deduplicate and sort the merged table
     * @param mergedTable the table to process
     * @return sortedTable the sorted table
     */
    private static Table deduplicateAndSortTable(Table mergedTable) {
        Table lastRowOfNames = mergedTable.structure().last(1);
        String lastColumnName = lastRowOfNames.column(
            lastRowOfNames.columns().size() - 2).getString(0);
        log.info("Removing duplicate samples from the merged table.");
        dedupedTable = mergedTable.dropDuplicateRows();
        log.info("Sorting the merged table.");
        sortedTable = dedupedTable.sortAscendingOn(lastColumnName);

        return sortedTable;
    }

    /*
     * Read all of the given file paths and generate a merged table
     * @return mergedTable  the table of all data samples
     */
    private static Table getMergedTable() {
        // Submit all file paths to the executor to be turned into tables
        for (Path filePath : paths) {
            ReaderTask task = new ReaderTask(filePath, config);
            Future<ReadResult> tableResult = executor.submit(task);
            readQueue.add(tableResult);
        }
        // Create the final merged table
        table = createTable(config);

        Table mergedTable = null;
        ReadResult readResult = null;

        while ( readQueue.size() - 1 >= 0 ) {
            try {
                // Poll the readQueue as table results are generated
                Future<ReadResult> tableResultFuture = readQueue.poll(10, TimeUnit.MINUTES);
                // Get each TableResult. If this throws, report the exception
                if ( tableResultFuture != null ) {
                    readResult = tableResultFuture.get();
                    Table tableToMerge = readResult.getTable();
                    mergedTable = table.append(tableToMerge);
                    log.debug("Merged table: " + tableToMerge.name());
                    completedReadTasks.put(readResult.getPath().toString(), "COMPLETE");
                }

            } catch (Exception e) {
                // The table creation failed. Store the status.
                if (readResult != null) {
                    completedReadTasks.put(readResult.getPath().toString(), e.toString());
                }
            }
            if (log.isInfoEnabled()) {
                if (completedReadTasks.size() != 0 && completedReadTasks.size() % 1000 == 0) {
                    log.info("Read " + completedReadTasks.size() + " files.");
                }
            }
        }
        return mergedTable;
    }

    /*
     * Write the raw and processed data files based on the rebuildDatesInUTC list
     */
    private static void writeFiles() {
        int count = 0;
        Instant previousInstant = null;
        List<Instant> rebuildHoursInUTC = new ArrayList<>();
        ZonedDateTime currentZonedDateTime;

        // Handle the single day scenario
        if ( rebuildDatesInUTC.size() == 1 ) {
            previousInstant = rebuildDatesInUTC.get(0);
            count = 1;
        }

        // Iterate through all dates and create writer tasks
        for (Instant currentInstant : rebuildDatesInUTC) {
            if (count == 0) {
                previousInstant = currentInstant;
            } else {
                if ( previousInstant == currentInstant) {
                    // When working within a single day, add a day and roll back a millisecond
                    currentInstant = currentInstant
                        .plus(1L, ChronoUnit.DAYS)
                        .truncatedTo(ChronoUnit.DAYS);
                    currentInstant = currentInstant.minus(1L, ChronoUnit.MILLIS);

                } else {
                    // Just roll the current back into the day by a millisecond
                    currentInstant = currentInstant.minus(1L, ChronoUnit.MILLIS);
                }
                currentZonedDateTime = currentInstant.atZone(ZoneOffset.ofHours(0));

                // Write the PacIOOS 2020 format converted file
                Path basePath = Paths.get(
                    config.getArchiveBaseDirectory(0, 1), // TODO: Fix this
                    config.getIdentifier(),
                    String.format("%4d", currentZonedDateTime.getYear()),
                    String.format("%02d", currentZonedDateTime.getMonthValue()),
                    String.format("%02d", currentZonedDateTime.getDayOfMonth())
                );
                WriterTask dailyWriterTask =
                    new WriterTask(previousInstant, currentInstant, basePath,
                        sortedTable, config, "pacioos-2020-format");
                // Submit the writer task for execution
                Future<WriteResult> dailyWriteResult = executor.submit(dailyWriterTask);
                writeQueue.add(dailyWriteResult);

                // Build a list of hourly raw files to write

                while (!previousInstant.isAfter(currentInstant)) {
                    rebuildHoursInUTC.add(previousInstant);
                    previousInstant = previousInstant.plus(1, ChronoUnit.HOURS);
                }

                // For each hourly duration, submit a write task
                int hourlydateCount = 0;
                Instant previousHourlyInstant = null;
                ZonedDateTime currentHourlyZonedDateTime;
                for (Instant currentHourlyInstant : rebuildHoursInUTC) {
                    if (hourlydateCount == 0) {
                        previousHourlyInstant = currentHourlyInstant;
                    } else {
                        currentHourlyInstant =
                            currentHourlyInstant.minus(1L, ChronoUnit.MILLIS);
                        currentHourlyZonedDateTime =
                            currentHourlyInstant.atZone(ZoneOffset.ofHours(0));

                        // Write the raw format files
                        basePath = Paths.get(
                            config.getArchiveBaseDirectory(0, 0), // TODO Fix this
                            config.getIdentifier(),
                            config.getChannelName(0), // TODO: Fix this
                            String.format("%4d", currentHourlyZonedDateTime.getYear()),
                            String.format("%02d", currentHourlyZonedDateTime.getMonthValue()),
                            String.format("%02d", currentHourlyZonedDateTime.getDayOfMonth())
                        );
                        WriterTask hourlyWriterTask = new WriterTask(
                            previousHourlyInstant,
                            currentHourlyInstant,
                            basePath,
                            sortedTable,
                            config, "raw");
                        Future<WriteResult> hourlyWriteResult = executor.submit(hourlyWriterTask);
                        writeQueue.add(hourlyWriteResult);
                    }
                    currentHourlyInstant = currentHourlyInstant.plus(1L, ChronoUnit.MILLIS);
                    previousHourlyInstant = currentHourlyInstant;
                    hourlydateCount++;
                }

                currentInstant = currentInstant.plus(1L, ChronoUnit.MILLIS);
                previousInstant = currentInstant;
                rebuildHoursInUTC.clear();
            }
            count++;
        }

        WriteResult writeResult = null;
        while (writeQueue.size() - 1 > 0) {
            try {
                // Poll the writeQueue as write results are generated
                Future<WriteResult> writeResultFuture = writeQueue.poll(10, TimeUnit.MINUTES);
                // Get each WriteResult. If this throws, report the exception
                if (writeResultFuture != null) {
                    writeResult = writeResultFuture.get();
                    // Table tableToMerge = readResult.getTable();
                    // mergedTable = table.append(tableToMerge);
                    // log.debug("Merged table: " + tableToMerge.name());
                    completedWriteTasks.put(writeResult.getPath().toString(), writeResult.getMessage());
                }

            } catch (Exception e) {
                // The file write failed. Store the status.
                if (writeResult != null) {
                    completedWriteTasks.put(writeResult.getPath().toString(), e.getMessage());
                }
            }
            if (log.isInfoEnabled()) {
                if (completedWriteTasks.size() != 0 && completedWriteTasks.size() % 1000 == 0) {
                    log.info("Wrote " + completedWriteTasks.size() + " files.");
                }
            }
        }
    }


    /*
     * Build the directory structures for the new raw and processed data
     */
    private static void buildDirectories() throws IOException {

        String rawArchiveBaseDirectory = null;
        String processedArchiveBaseDirectory = null;
        // Get the base directories
        int channelCount = 0;
        for (ChannelConfiguration channelConfig : config.getChannelConfigurations()) {
            for (ArchiverConfiguration archiverConfig : channelConfig.getArchiverConfigurations()) {
                if (archiverConfig.getArchiveType().equals("raw")) {
                    rawArchiveBaseDirectory = archiverConfig.getArchiveBaseDirectory();
                } else if (archiverConfig.getArchiveType().equals("pacioos-2020-format")) {
                    processedArchiveBaseDirectory = archiverConfig.getArchiveBaseDirectory();
                } else {
                    log.debug("Didn't recognize the archive type. Skipping directory creation.");
                }
            }
            String instrumentName = config.getIdentifier();
            String channelName = config.getChannelName(channelCount);

            // Make the daily base directories if they don't exist
            if (rawArchiveBaseDirectory != null) {
                // Create raw directories, e.g. /data/raw/alawai/{instrument}/{channel}/{year}/{month}/{day}
                for (Instant instant : rebuildDatesInUTC) {
                    ZonedDateTime zonedDateTime = instant.atZone(ZoneOffset.ofHours(0));
                    String year = String.format("%4d", zonedDateTime.getYear());
                    String month = String.format("%02d", zonedDateTime.getMonthValue());
                    String day = String.format("%02d", zonedDateTime.getDayOfMonth());
                    Path rawPath = Paths.get(
                        rawArchiveBaseDirectory, instrumentName, channelName, year, month, day
                    );
                    if (!Files.exists(rawPath)) {
                        Files.createDirectories(rawPath);
                    }
                }
            }
            if (processedArchiveBaseDirectory != null) {
                // Create processed directories, e.g. /data/processed/pacioos/{instrument}/{year}/{month}/{day}
                for (Instant instant : rebuildDatesInUTC) {
                    ZonedDateTime zonedDateTime = instant.atZone(ZoneOffset.ofHours(0));
                    String year = String.format("%4d", zonedDateTime.getYear());
                    String month = String.format("%02d", zonedDateTime.getMonthValue());
                    String day = String.format("%02d", zonedDateTime.getDayOfMonth());
                    Path processedPath = Paths.get(
                        processedArchiveBaseDirectory, instrumentName, year, month, day
                    );
                    if (!Files.exists(processedPath)) {
                        Files.createDirectories(processedPath);
                    }
                }
            }
            channelCount++;
        }
    }

    /**
     * Log the results to file
     */
    private static void reportResults() {
        // Create a date string prefix for log success and error log files
        String timestamp = DateTimeFormatter
            .ofPattern("yyyy-MM-dd-HH-mm")
            .withZone(ZoneOffset.ofHours(0))
            .format(Instant.now());

        int readCount = 0;
        int writeCount = 0;
        int readErrorCount = 0;
        int writeErrorCount = 0;
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);

        // Add paths and messages to an error JSON object
        ArrayNode errorArrayNode = mapper.getNodeFactory().arrayNode();
        for (String key : completedReadTasks.keySet()) {
            String value = completedReadTasks.get(key);
            if ( value != null && ! value.matches("COMPLETE")) {
                log.debug(key + ": " + value);
                ObjectNode errorTasksNode = mapper.getNodeFactory().objectNode();
                errorTasksNode.put("path", key);
                errorTasksNode.put("message", value);
                errorArrayNode.add(errorTasksNode);
                readErrorCount++;
            } else {
                readCount++;
            }
        }

        // Add paths and messages to a success JSON object
        ArrayNode successArrayNode = mapper.getNodeFactory().arrayNode();
        for (String key : completedWriteTasks.keySet()) {
            String value = completedWriteTasks.get(key);
            if ( value != null && ! value.matches("COMPLETE")) {
                log.debug(key + ": " + value);
                ObjectNode errorTasksNode = mapper.getNodeFactory().objectNode();
                errorTasksNode.put("path", key);
                errorTasksNode.put("message", value);
                errorArrayNode.add(errorTasksNode);
                writeErrorCount++;
            } else {
                ObjectNode successTasksNode = mapper.getNodeFactory().objectNode();
                successTasksNode.put("path", key);
                successTasksNode.put("message", value);
                successArrayNode.add(successTasksNode);
                writeCount++;
            }
        }

        // Log any errors to the error log file
        FileLock errorFileLock = null;
        try {
            Path rebuildErrorFilePath =
                Paths.get(recoveryPath, timestamp + "_" + rebuildErrorFile);
            FileChannel errorChannel = FileChannel.open(
                rebuildErrorFilePath, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW
            );
            errorFileLock = errorChannel.lock();
            mapper.writeValue(rebuildErrorFilePath.toFile(), errorArrayNode);

        } catch (IOException e) {
            log.warn("Couldn't write to the error log file: " + e.toString());
        } finally {
            if (errorFileLock != null) {
                try {
                    errorFileLock.close();
                } catch (IOException e) {
                    log.warn("Couldn't close the error log file: " + e.toString());
                }
            }
        }

        // Log all successes to the success log file
        FileLock successFileLock = null;
        try {
            Path rebuildSuccessFilePath =
                Paths.get(recoveryPath, timestamp + "_" + rebuildSuccessFile);
            FileChannel successChannel = FileChannel.open(
                rebuildSuccessFilePath, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW
            );
            successFileLock = successChannel.lock();
            mapper.writeValue(rebuildSuccessFilePath.toFile(), successArrayNode);

        } catch (IOException e) {
            log.warn("Couldn't write to the success log file: " + e.toString());
        } finally {
            if (successFileLock != null) {
                try {
                    successFileLock.close();
                } catch (IOException e) {
                    log.warn("Couldn't close the success log file: " + e.toString());
                }
            }
        }

        // And log the final results
        log.info("--------------------- Results ---------------------------");
        log.info("Completed rebuilding          :\t" + path);
        log.info("Total original files          :\t" + paths.size());
        log.info("Total files read              :\t" + readCount);
        log.info("Total files written           :\t" + writeCount);
        log.info("Total files with read errors  :\t" + readErrorCount);
        log.info("Total files with write errors :\t" + writeErrorCount);
        log.info("Total samples processed       :\t" + (mergedTable != null ? mergedTable.rowCount() : 0));
        log.info("Total unique samples          :\t" + (dedupedTable != null ? dedupedTable.rowCount() : 0));
        log.info("---------------------------------------------------------");
        log.warn("See the error log for processing error details.");
        if ( readCount > 0 ) {
            log.info("\nThe data in\n" + path + "\nhave been moved to\n" + recoveryPath);
            log.info("If there were errors, don't delete the recovery directory so they can be dealt with.");
            log.info("Review the new files, and if you are satisfied with the rebuild, delete the recovery directory.");
        }

    }

    /**
     * Shuts down the executor service when all tasks are processed
     */
    private static void shutdownExecutorService() {
        // When all tasks are complete, shut down the executor
        if (log.isDebugEnabled()) {
            log.debug("Shutting down the executor service.");
        }

        // Shut down the executor
        executor.shutdown();
        try {
            // After 10 minutes, abort execution
            if (!executor.awaitTermination(10L, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    /*
     * Get a list of data files to process given a path
     * @param path  the path to the data file(s)
     * @return paths  the list of data file paths
     */
    private static List<Path> getDataFilePaths(String path) {

        Path givenPath = Paths.get(path);
        List<Path> paths = new ArrayList<>();
        boolean exists = Files.exists(FileSystems.getDefault().getPath(path));
        boolean isDirectory = Files.isDirectory(FileSystems.getDefault().getPath(path));

        // Return a single file if it is not a directory
        if ( exists && ! isDirectory ) {
            paths.add(givenPath);
            return paths;
        }

        // If the path is a directory, get all files within it
        if ( exists ) {
            try {
                List<Path> filePaths = Files.walk(givenPath)
                    .filter(Files::isRegularFile)
                    .filter(Files::isReadable)
                    .filter(currentPath -> ! currentPath.toFile().isHidden())
                    .collect(Collectors.toList());
                paths.addAll(filePaths);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return paths;
    }

    /*
     * Get a Configuration object from the XML configuration file path
     * @param xmlConfiguration the path to the XML configuration file
     * @return configuration  the configuration object
     */
    private static Configuration getConfiguration(String xmlConfiguration) {
        Configuration configuration = null;

        // Get a configuration based on the XML configuration
        try {
            configuration = new Configuration(xmlConfiguration);

        } catch (ConfigurationException e) {
            String msg = "The was a problem configuring the rebuilder. The message was: " +
                e.getMessage();
            log.error(msg);
            if (log.isDebugEnabled() ) {
                e.printStackTrace();
            }
            System.exit(1);
        }
        return configuration;
    }

    /*
     * Create the table of samples to rebuild in the archive directory
     * @param config the instrument configuration
     * @return table  a blank table
     */
    private static Table createTable(Configuration config) {
        Table table = Table.create("table");

        int count = 0;
        for (Iterator<ColumnType> it =
             Arrays.stream(config.getColumnTypes(0)).iterator(); it.hasNext(); ) {
            ColumnType columnType = it.next();
            String columnPrefix = "C";
            if (columnType == ColumnType.BOOLEAN ) {
                table.addColumns(BooleanColumn.create(columnPrefix + count));
            } else if (columnType == ColumnType.DOUBLE) {
                table.addColumns(DoubleColumn.create(columnPrefix + count));
            } else if (columnType == ColumnType.FLOAT) {
                table.addColumns(FloatColumn.create(columnPrefix + count));
            } else if (columnType == ColumnType.INSTANT) {
                table.addColumns(InstantColumn.create(columnPrefix + count));
            } else if (columnType == ColumnType.INTEGER) {
                table.addColumns(IntColumn.create(columnPrefix + count));
            // Force date and time columns to a single datetime column
            } else if (columnType == ColumnType.LOCAL_DATE) {
                table.addColumns(DateTimeColumn.create(columnPrefix + count));
            } else if (columnType == ColumnType.LOCAL_TIME) {
                continue;
            } else if (columnType == ColumnType.LOCAL_DATE_TIME) {
                table.addColumns(DateTimeColumn.create(columnPrefix + count));
            } else {
                table.addColumns(StringColumn.create(columnPrefix + count));
            }
            count++;
        }
        log.debug(table.structure());
        return table;
    }
}
