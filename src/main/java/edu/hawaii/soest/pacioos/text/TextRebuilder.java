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
import edu.hawaii.soest.pacioos.text.concurrent.ReaderTask;
import edu.hawaii.soest.pacioos.text.concurrent.WriteResult;
import edu.hawaii.soest.pacioos.text.concurrent.WriterTask;
import edu.hawaii.soest.pacioos.text.configure.ArchiverConfiguration;
import edu.hawaii.soest.pacioos.text.configure.ChannelConfiguration;
import edu.hawaii.soest.pacioos.text.configure.Configuration;
import org.apache.commons.configuration.ConfigurationException;
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
 * A class that rebuilds raw and processed text data files (hourly and daily)
 */
public class TextRebuilder {

    /* The Configuration instance created from the instrument XMl config file */
    private final Configuration configuration;

    /* The list of data file paths to process */
    private List<Path> dataFilePaths;

    /* The filesystem path where old files are moved if recovery is needed */
    private String recoveryBasePath;

    /* An executor service to process all tasks. Uses all processors except one */
    private final ExecutorService executor = new ThreadPoolExecutor(
        Runtime.getRuntime().availableProcessors() - 1,
        Runtime.getRuntime().availableProcessors() - 1,
        0L,
        TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>()
    );

    /* Set up a log */
    private static final Log log = LogFactory.getLog(TextRebuilder.class);

    /* The path to the rebuild success log file */
    private String rebuildSuccessFile = "success.log";

    /* The path to the rebuild errors log file */
    private String rebuildErrorFile = "errors.log";
    
    /* A map of read task results for reporting */
    private final Map<String, String> completedReadTasks = new HashMap<>();

    /* A map of write task results for reporting */
    private final Map<String, String> completedWriteTasks = new HashMap<>();

    /**
     * Construct a new text rebuild application using a Builder and its options
     */
    private TextRebuilder(TextRebuilder.Builder builder) {
        this.configuration = builder.configuration;
        this.rebuildErrorFile = builder.rebuildErrorFile;
        this.rebuildSuccessFile = builder.rebuildSuccessFile;
        this.recoveryBasePath = builder.recoveryBasePath;
        this.dataFilePaths = builder.dataFilePaths;
    }

    /**
     * Create a TextRebuilder.Builder instance given an instrument XML configuration file
     * @param xmlConfiguration the instrument XML configuration file
     * @return the Builder instance
     */
    public static Builder builder(String xmlConfiguration) {
        return new Builder(xmlConfiguration);
    }

    /**
     * Check that the given recovery path exists and is writable
     * @param path the recovery path to move existing data to
     * @return true if the path is valid
     */
    protected boolean recoveryPathIsValid(String path) {
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
    protected void moveDirectories() {

        String recoveryTimestamp =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm")
                .format(Instant.now().atZone(ZoneOffset.ofHours(0)));

        // Make sure the recovery path is valid
        if (!recoveryPathIsValid(recoveryBasePath)) {
            // When the default recovery directory is unavailable, use the home directory
            recoveryBasePath = String.join(
                FileSystems.getDefault().getSeparator(),
                System.getProperty("user.home"),
                "recovery",
                recoveryTimestamp
            );
        } else {
            recoveryBasePath = Paths.get(recoveryBasePath, recoveryTimestamp).toString();
        }

        // Move the list of original paths
        Path recoveryItem;
        Path relativePath;
        for (Path path : dataFilePaths) {
            // Get a relative path of the given data path
            relativePath = Paths.get("/").relativize(path);
            // Set the recovery directory path (or fall back to one in the user's home)
            recoveryItem = Paths.get(recoveryBasePath.toString(), relativePath.toString());

            // Move the original directory with files to the recovery directory
            try {
                // Make the recovery directories as needed
                if ( ! Files.exists(recoveryItem) ) {
                    if ( Files.isDirectory(recoveryItem) ) {
                        new File(recoveryItem.toString()).mkdirs();
                    } else {
                        new File(recoveryItem.getParent().toString()).mkdirs();
                    }
                }
                Files.move(path, recoveryItem, StandardCopyOption.REPLACE_EXISTING);
                log.debug(
                    ConsoleColors.BLUE +
                        "\nMoved\n" + path +"\nto recovery directory:\n" + recoveryBasePath +
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
    }

    /*
     * Deduplicate and sort the merged table
     * @param mergedTable the table to process
     * @return sortedTable the sorted table
     */
    protected Table deduplicateAndSortTable(Table mergedTable, int sortColumnIndex) {
        log.info("Removing duplicate samples from the merged table.");
        Table dedupedTable = mergedTable.dropDuplicateRows();
        log.info("Sorting the merged table.");

        return dedupedTable.sortOn(sortColumnIndex);
    }

    /*
     * Read all of the given file paths and generate a merged table
     * @return mergedTable  the table of all data samples
     */
    protected Table getMergedTable() {
        /* A file processing read queue */
        BlockingQueue<Future<ReadResult>> readQueue = new LinkedBlockingQueue<>();
        /* The initial table */
        Table table;

        // Submit all file paths to the executor to be turned into tables
        for (Path filePath : this.dataFilePaths) {
            ReaderTask task = new ReaderTask(filePath, this.configuration);
            Future<ReadResult> tableResult = executor.submit(task);
            readQueue.add(tableResult);
        }
        // Create the final merged table
        table = createTable(this.configuration);

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
    protected void writeFiles(List<Instant> rebuildDatesInUTC, Table sortedTable) {
        int count = 0;
        Instant previousInstant = null;
        List<Instant> rebuildHoursInUTC = new ArrayList<>();
        ZonedDateTime currentZonedDateTime;
        /* A file processing write queue */
        BlockingQueue<Future<WriteResult>> writeQueue = new LinkedBlockingQueue<>();

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
                    // When working within a single day
                    currentInstant = currentInstant
                        .plus(1L, ChronoUnit.DAYS)
                        .truncatedTo(ChronoUnit.DAYS);
                }
                // Now roll the current instant back into the day by a millisecond
                currentInstant = currentInstant.minus(1L, ChronoUnit.MILLIS);
                currentZonedDateTime = currentInstant.atZone(ZoneOffset.ofHours(0));

                // Write the PacIOOS 2020 format converted file
                Path basePath = Paths.get(
                    configuration.getArchiveBaseDirectory(0, 1), // TODO: Fix this
                    configuration.getIdentifier(),
                    String.format("%4d", currentZonedDateTime.getYear()),
                    String.format("%02d", currentZonedDateTime.getMonthValue()),
                    String.format("%02d", currentZonedDateTime.getDayOfMonth())
                );
                WriterTask dailyWriterTask =
                    new WriterTask(previousInstant, currentInstant, basePath,
                        sortedTable, configuration, "pacioos-2020-format");
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
                            configuration.getArchiveBaseDirectory(0, 0), // TODO Fix this
                            configuration.getIdentifier(),
                            configuration.getChannelName(0), // TODO: Fix this
                            String.format("%4d", currentHourlyZonedDateTime.getYear()),
                            String.format("%02d", currentHourlyZonedDateTime.getMonthValue()),
                            String.format("%02d", currentHourlyZonedDateTime.getDayOfMonth())
                        );
                        WriterTask hourlyWriterTask = new WriterTask(
                            previousHourlyInstant,
                            currentHourlyInstant,
                            basePath,
                            sortedTable,
                            configuration, "raw");
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
    protected void buildDirectories(List<Instant> rebuildDatesInUTC) throws IOException {

        String rawArchiveBaseDirectory = null;
        String processedArchiveBaseDirectory = null;
        // Get the base directories
        int channelCount = 0;
        for (ChannelConfiguration channelConfig : configuration.getChannelConfigurations()) {
            for (ArchiverConfiguration archiverConfig : channelConfig.getArchiverConfigurations()) {
                if (archiverConfig.getArchiveType().equals("raw")) {
                    rawArchiveBaseDirectory = archiverConfig.getArchiveBaseDirectory();
                } else if (archiverConfig.getArchiveType().equals("pacioos-2020-format")) {
                    processedArchiveBaseDirectory = archiverConfig.getArchiveBaseDirectory();
                } else {
                    log.debug("Didn't recognize the archive type. Skipping directory creation.");
                }
            }
            String instrumentName = configuration.getIdentifier();
            String channelName = configuration.getChannelName(channelCount);

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
    protected void reportResults(String path, int pathsCount, int mergedTableRows, int dedupedTableRows) {
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
                Paths.get(recoveryBasePath, timestamp + "_" + rebuildErrorFile);
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
                Paths.get(recoveryBasePath, timestamp + "_" + rebuildSuccessFile);
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
        log.info("Total original files          :\t" + pathsCount);
        log.info("Total files read              :\t" + readCount);
        log.info("Total files written           :\t" + writeCount);
        log.info("Total files with read errors  :\t" + readErrorCount);
        log.info("Total files with write errors :\t" + writeErrorCount);
        log.info("Total samples processed       :\t" + mergedTableRows);
        log.info("Total unique samples          :\t" + dedupedTableRows);
        log.info("---------------------------------------------------------");
        log.warn("See the error log for processing error details.");
        if ( readCount > 0 ) {
            log.info(ConsoleColors.BLUE + "\nThe data in\n" + path + "\nhave been moved to\n" + recoveryBasePath + ConsoleColors.RESET);
            log.info(ConsoleColors.BLUE + "If there were errors, don't delete the recovery directory so they can be dealt with." + ConsoleColors.RESET);
            log.info(ConsoleColors.BLUE + "Review the new files, and if you are satisfied with the rebuild, delete the recovery directory." + ConsoleColors.RESET);
        }

    }

    /**
     * Shuts down the executor service when all tasks are processed
     */
    protected void shutdownExecutorService() {
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
     * Get a Configuration object from the XML configuration file path
     * @param xmlConfiguration the path to the XML configuration file
     * @return configuration  the configuration object
     */
    protected Configuration getConfiguration(String xmlConfiguration) {
        return this.configuration;
    }

    /*
     * Create the table of samples to rebuild in the archive directory
     * @param config the instrument configuration
     * @return table  a blank table
     */
    protected Table createTable(Configuration config) {
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

    /**
     * A builder to build a TextRebuilder class
     */
    protected static class Builder {

        /* the path to the configuration file */
        private final String xmlConfiguration;

        /* The Configuration instance created from the instrument XMl config file */
        private final Configuration configuration;

        /* The rebuildSuccessFile used during the rebuild process */
        private String rebuildSuccessFile;

        /* The rebuildErrorFile used during the rebuild process */
        private String rebuildErrorFile;

        /* The recoveryBasePath used during the rebuild process */
        private String recoveryBasePath;

        /* The list of data file paths to process */
        private List<Path> dataFilePaths;

        /**
         * Construct a TextRebuilder.Builder based on the XML config file path
         * @param xmlConfiguration the path to the configuration file
         */
        public Builder(String xmlConfiguration) {
            this.xmlConfiguration = xmlConfiguration;
            this.configuration = this.configuration(xmlConfiguration);
        }

        /**
         * Return a TextRebuilder instance using the Builder options
         * @return
         */
        public TextRebuilder build() {
            return new TextRebuilder(this);
        }

        /**
         * Set the rebuildSuccessFile in the TextRebuilder instance
         * @param rebuildSuccessFile  the rebuild success file used for logging
         * @return  the TextRebuilder.Builder instance
         */
        public Builder rebuildSuccessFile(String rebuildSuccessFile) {
            this.rebuildSuccessFile = rebuildSuccessFile;
            return this;
        }

        /**
         * Set the rebuildErrorFile in the TextRebuilder instance
         * @param rebuildErrorFile  the rebuild error file used for logging
         * @return  the TextRebuilder.Builder instance
         */
        public Builder rebuildErrorFile(String rebuildErrorFile) {
            this.rebuildErrorFile = rebuildErrorFile;
            return this;
        }

        /**
         * Set the recoveryBasePath in the TextRebuilder instance
         * @param recoveryBasePath  the recovery base path used to back up data files
         * @return  the TextRebuilder.Builder instance
         */
        public Builder recoveryBasePath(String recoveryBasePath) {
            this.recoveryBasePath = recoveryBasePath;
            return this;
        }

        /*
         * Get a Configuration object from the XML configuration file path
         * @param xmlConfiguration the path to the XML configuration file
         * @return configuration  the configuration object
         */
        private Configuration configuration(String xmlConfiguration) {
            Configuration configuration = null;

            // Get a configuration based on the XML configuration
            try {
                configuration = new Configuration(xmlConfiguration);

            } catch (ConfigurationException e) {
                String msg = "There was a problem configuring the rebuilder. " +
                    "The message was: " + e.getMessage();
                log.error(msg);
                if (log.isDebugEnabled() ) {
                    e.printStackTrace();
                }
                System.exit(1);
            }
            return configuration;
        }

        /**
         * Set a list of data files to process given a path
         * @param dataDirectoryPath  the path to the data directory
         * @return builder  the list of data file paths
         */
        public Builder dataDirectory(String dataDirectoryPath) {

            Path givenPath = Paths.get(dataDirectoryPath);
            dataFilePaths = new ArrayList<>();
            boolean exists = Files.exists(FileSystems.getDefault().getPath(dataDirectoryPath));
            boolean isDirectory = Files.isDirectory(FileSystems.getDefault().getPath(dataDirectoryPath));

            // Return a single file if it is not a directory
            if ( exists && ! isDirectory ) {
                dataFilePaths.add(givenPath);
                return this;
            }

            // If the path is a directory, get all files within it
            if ( exists ) {
                try {
                    List<Path> filePaths = Files.walk(givenPath)
                        .filter(Files::isRegularFile)
                        .filter(Files::isReadable)
                        .filter(currentPath -> ! currentPath.toFile().isHidden())
                        .collect(Collectors.toList());
                    this.dataFilePaths.addAll(filePaths);

                } catch (IOException e) {
                    log.info("There was a problem listing the data file paths. " +
                        "The message was: " + e.getMessage());
                    if ( log.isDebugEnabled() ) {
                        e.printStackTrace();
                    }
                }
            }
            return this;
        }

    }

    /**
     * Get the Rebuilder configuration 
     * @return configuration  the rebuilder configuration instance
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Get the Rebuilder data file paths 
     * @return dataFilePaths  the rebuilder data file paths
     */
    public List<Path> getDataFilePaths() {
        return dataFilePaths;
    }

    /**
     * Get the Rebuilder recovery base path 
     * @return recoveryBasePath  the recovery base path
     */
    public String getRecoveryBasePath() {
        return recoveryBasePath;
    }

    /**
     * Get the Rebuilder success file
     * @return rebuildSuccessFile  the rebuilder success file
     */
    public String getRebuildSuccessFile() {
        return rebuildSuccessFile;
    }

    /**
     * Get the Rebuilder error file
     * @return rebuildErrorFile the rebuilder error file
     */
    public String getRebuildErrorFile() {
        return rebuildErrorFile;
    }
}
