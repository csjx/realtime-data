# The `manage-instruments` command

The `manage-instruments` command allows you to list all of the instruments, start and stop communication drivers for the realtime instruments, and start and stop the raw and processed data archiver processes for each realtime instrument.  It also allows you to rebuild an entire instrument's archive and add or backfill data to the raw archive.  Lastly, you can reload the latest year worth of data into the Data Turbine after an instrument archive has been rebuilt.  See the command usage below.

## Command Usage

In a terminal, run the `manage-instruments` command with the `-h` option to get the help text:

```
$ manage-instruments -h

Usage:
manage-instruments -l
manage-instruments -c driver|archiver -o start|stop [-a] [-h] [-i instr1] [-i instr2] [-i instrN]
manage-instruments -c rebuilder -i instr -d directory
manage-instruments -c reloader -i instr -d directory

List the status of all instrument drivers and archivers, or
start or stop one or more instrument source drivers or archivers by optionally providing the instrument id.

Alternatively, rebuild an instrument archive by providing the instrument id, and an existing raw archive directory path.

Lastly, reload an instrument's data into the Data Turbine by providing an instrument id and raw data directory.

OPTIONS:

    -a  Start, stop or show status for all configured instruments
    -c  The command to run, 'driver', 'archiver', 'rebuilder', 'reloader', to stream, archive, rebuild or reload data.
    -h  Show this message.
    -i  The instrument id. Use -i multiple times for multiple instruments. Use once for a rebuild or reload.
    -l  List the status of all drivers and archivers.
    -o  Indicate which operation to perform, start, stop, or status (requires 'driver' or 'archiver' commands).
    -d  The existing raw instrument archive to rebuild (requires 'rebuilder' or 'reloader' command).
    -V  Show the version (1.4.4)

```

## Listing Instruments

To list instruments, run the `manage-instruments` command with the `-l` option.  This provides a listing sorted by the instrument `shortName` (NS01, NS02, ...), and shows the status of the instrument driver, archiver, and its deployment type.
The status for the `online` instruments should show in <span style="color:green">green</span> text if both the driver and archiver are running, otherwise the line will be shown in <span style="color:red">red</span>.  The `offline` and `retired` instruments will always show as <span style="color:blue">blue</span> text.

```
$ manage-instruments -l
       Name           Instrument   Driver Status Archiver Status Deployment Type
       NS01 AW01XX_001CTDXXXXR00     Not Running     Not Running         retired
       NS02 AW02XX_001CTDXXXXR00         Running         Running          online
       NS02 AW02XX_101CTDXXXXR00     Not Running     Not Running         retired
       NS02 AW02XX_201CTDXXXXR00     Not Running     Not Running         retired
       NS03 WK01XX_001CTDXXXXR00         Running         Running          online
       NS04 WK02XX_001CTDXXXXR00         Running         Running          online
       NS05 PIAS01_001CTDXXXXR00     Not Running     Not Running         offline
       NS06 PIFM01_001CTDXXXXR00     Not Running     Not Running         retired
       NS06 PIFM01_101CTDXXXXR00     Not Running     Not Running         retired
       NS06 PIFM02_001CTDXXXXR00     Not Running     Not Running         retired
       NS06 PIFM02_002CTDXXXXR00     Not Running     Not Running         offline
       NS06 PIFM02_101CTDXXXXR00     Not Running     Not Running         retired
       NS07 PIMI01_001CTDXXXXR00     Not Running     Not Running         offline
       NS08 PIPL01_001CTDXXXXR00     Not Running     Not Running         offline
       NS09 PIGM01_001CTDXXXXR00     Not Running     Not Running         retired
       NS10 MB01XX_001CTDXXXXR00         Running         Running          online
       NS10 MB01XX_101CTDXXXXR00     Not Running     Not Running         retired
       NS11 PINM01_002CTDX008R00     Not Running     Not Running         offline
       NS12 MU02XX_001YCTDXXXR00     Not Running     Not Running         retired
       NS12 MU02XX_002CTDXXXXR00     Not Running     Not Running         offline
       NS13 MU01XX_001YCTDXXXR00     Not Running     Not Running         retired
       NS13 MU01XX_101YCTDXXXR00     Not Running     Not Running         retired
       NS15 PIGM01_002CTDX002R00     Not Running     Not Running         retired
       NS16 MB02XX_001CTDXXXXR00     Not Running     Not Running         retired
       NS17 PIGM01_003CTDX002R00     Not Running     Not Running         offline
       PP02          pp2_pohnpei     Not Running     Not Running         retired
```
## Starting Drivers

The realtime instrument drivers can be started all at once with the `-a` option, or individually with one or more `-i` script options.  The command looks like the following, where the `-c` command option is set to `driver`, the `-o` operation option is set to `start`.

To start all drivers, use:

```
manage-instruments -c driver -o start -a
```

To start a single driver, such as the NS02 (AW02XX_001CTDXXXXR00) instrument, use:

```
manage-instruments -c driver -o start -i AW02XX_001CTDXXXXR00
```

To start more than one driver, such as the NS02 (AW02XX_001CTDXXXXR00) and the NS03 (WK01XX_001CTDXXXXR00) instrument, use:

```
manage-instruments -c driver -o start -i AW02XX_001CTDXXXXR00 -i WK01XX_001CTDXXXXR00
```

## Stopping Drivers

The realtime instrument drivers can be stopped all at once with the `-a` option, or individually with one or more `-i` script options.  The command looks like the following, where the `-c` command option is set to `driver`, the `-o` operation option is set to `stop`.

To stop all drivers, use:

```
manage-instruments -c driver -o stop -a
```

To stop a single driver, such as the NS02 (AW02XX_001CTDXXXXR00) instrument, use:

```
manage-instruments -c driver -o stop -i AW02XX_001CTDXXXXR00
```

To stop more than one driver, such as the NS02 (AW02XX_001CTDXXXXR00) and the NS03 (WK01XX_001CTDXXXXR00) instrument, use:

```
manage-instruments -c driver -o stop -i AW02XX_001CTDXXXXR00 -i WK01XX_001CTDXXXXR00
```

## Starting Archivers

The realtime instrument archivers can be started all at once with the `-a` option, or individually with one or more `-i` script options.  The command looks like the following, where the `-c` command option is set to `archiver`, the `-o` operation option is set to `start`.

To start all archivers, use:

```
manage-instruments -c archiver -o start -a
```

To start a single archiver, such as the NS02 (AW02XX_001CTDXXXXR00) instrument, use:

```
manage-instruments -c archiver -o start -i AW02XX_001CTDXXXXR00
```

To start more than one archiver, such as the NS02 (AW02XX_001CTDXXXXR00) and the NS03 (WK01XX_001CTDXXXXR00) instrument, use:

```
manage-instruments -c archiver -o start -i AW02XX_001CTDXXXXR00 -i WK01XX_001CTDXXXXR00
```


## Stopping Archivers


The realtime instrument archivers can be stopped all at once with the `-a` option, or individually with one or more `-i` script options.  The command looks like the following, where the `-c` command option is set to `archiver`, the `-o` operation option is set to `stop`.

To stop all archivers, use:

```
manage-instruments -c archiver -o stop -a
```

To stop a single archiver, such as the NS02 (AW02XX_001CTDXXXXR00) instrument, use:

```
manage-instruments -c archiver -o stop -i AW02XX_001CTDXXXXR00
```

To stop more than one archiver, such as the NS02 (AW02XX_001CTDXXXXR00) and the NS03 (WK01XX_001CTDXXXXR00) instrument, use:

```
manage-instruments -c archiver -o stop -i AW02XX_001CTDXXXXR00 -i WK01XX_001CTDXXXXR00
```

## Rebuilding Instrument Data

Needing to rebuild an instrument's data archive is usually due to one of two issues:

1) There are problems with the previous years' files (corrupted data, incorrectly columns, incorrect dates, etc.)
2) New data manually collected from the instrument need to be added to fill gaps or extend the time series

Instrument data are rebuilt one directory at a time using the `manage-instruments` command with the `-c rebuilder` command option.  This option is accompanied by two other options: `-i` to indicate the instrument identifier, and `-d` to indicate the pathe to the instrument's **`raw`** data directory.  For instance:

```
$ manage-instruments -c rebuilder -i AW02XX_001CTDXXXXR00 -d /data/raw/alawai/AW02XX_001CTDXXXXR00
```

To rebuild the archive without adding any data, just run the above command.  To backfill data gaps or to add new data to the time series, first place the QA/QC'd data file (or files) into the raw data directory.  For example:

```
$ cp /home/kilonalu/AW02XX_001CTDXXXXR00_backfill.txt /data/raw/alawai/AW02XX_001CTDXXXXR00
```

It's important that the new file is cleaned, has no empty lines, no non-ASCII characters, and adheres to the `dataPattern` found in the instrument's configuration file.  The order of the samples in the file does not matter because they get sorted and deduplicated during the rebuild process.

## Interpreting the output

When the rebuilder successfully runs, you will see it read in thousands of data files found in the raw directory.  If there are errors reading files, you will also see those `[ERROR]` lines, which means you will need to look at the errors more closely.

Once all of the data are read, they are sorted and de-duplicated, and a preview of the merged table is shown.  You can see the beginning and end of the time series and the data columns that were parsed:

```
20220112-20:31:23: [INFO]: Read 1000 files.
20220112-20:31:24: [INFO]: Read 2000 files.
...
20220112-20:31:53: [INFO]: Read 96000 files.
20220112-20:31:53: [INFO]: Removing duplicate samples from the merged table.
20220112-20:32:03: [INFO]: Sorting the merged table.
20220112-20:32:07: [INFO]: Merged table preview:
                                         table                                         
   C0     |     C1     |    C2    |    C3    |    C4     |       datetimesInUTC       |
---------------------------------------------------------------------------------------
 27.8346  |  5.372608  |  0.1984  |  0.2196  |  33.3693  |  2008-07-29T00:19:15.000Z  |
 27.8713  |  5.372435  |  0.2119  |  0.2444  |  33.3421  |  2008-07-29T00:23:15.000Z  |
 27.9193  |  5.354701  |  0.2046  |  0.2336  |  33.1846  |  2008-07-29T00:27:15.000Z  |
 28.0084  |  5.323989  |  0.2070  |  0.2457  |  32.9086  |  2008-07-29T00:35:15.000Z  |
 27.9935  |  5.336224  |  0.2045  |  0.2554  |  33.0040  |  2008-07-29T00:39:15.000Z  |
 28.0111  |  5.330332  |  0.2033  |  0.2469  |  32.9507  |  2008-07-29T00:43:15.000Z  |
 27.8922  |  5.359811  |  0.2190  |  0.2469  |  33.2393  |  2008-07-29T00:47:15.000Z  |
 27.9748  |  5.347654  |  0.2166  |  0.2614  |  33.0965  |  2008-07-29T00:51:15.000Z  |
 28.0145  |  5.347622  |  0.2033  |  0.2529  |  33.0685  |  2008-07-29T00:55:15.000Z  |
 28.0989  |  5.318298  |  0.2154  |  0.2663  |  32.8061  |  2008-07-29T00:59:15.000Z  |
     ...  |       ...  |     ...  |     ...  |      ...  |                       ...  |
 24.6137  |   4.92874  |  0.4512  |  0.5778  |  32.4875  |  2021-12-26T19:23:05.000Z  |
 24.6154  |   4.93673  |  0.4478  |  0.5765  |  32.5453  |  2021-12-26T19:27:05.000Z  |
 24.6464  |   4.94578  |  0.4196  |  0.5634  |  32.5897  |  2021-12-26T19:31:05.000Z  |
 24.6259  |   4.94125  |  0.4479  |  0.5774  |  32.5711  |  2021-12-26T19:35:05.000Z  |
 24.6453  |   4.91687  |  0.4539  |  0.5792  |  32.3771  |  2021-12-26T19:39:05.000Z  |
 24.7324  |   4.91905  |  0.4544  |  0.5801  |  32.3307  |  2021-12-26T19:43:05.000Z  |
 24.7390  |   4.92986  |  0.4337  |  0.5704  |  32.4055  |  2021-12-26T19:47:05.000Z  |
 24.6908  |   4.93056  |  0.4225  |  0.5651  |  32.4454  |  2021-12-26T19:51:05.000Z  |
 24.6627  |   4.92805  |  0.4094  |  0.5595  |  32.4471  |  2021-12-26T19:55:05.000Z  |
 24.6594  |   4.92765  |  0.3935  |  0.5509  |  32.4465  |  2021-12-26T19:59:05.000Z  |
20220112-20:32:24: [INFO]: Deleted /data/processed/pacioos/AW02XX_001CTDXXXXR00
20220112-20:32:34: [INFO]: Wrote 1000 files.
20220112-20:32:41: [INFO]: Wrote 2000 files.
...
20220112-20:55:23: [INFO]: Wrote 116000 files.
20220112-20:55:38: [INFO]: Wrote 117000 files.
20220112-20:55:48: [INFO]: --------------------- Results ---------------------------
20220112-20:55:48: [INFO]: Completed rebuilding          :	/data/raw/alawai/AW02XX_001CTDXXXXR00/
20220112-20:55:48: [INFO]: Total original files          :	96140
20220112-20:55:48: [INFO]: Total files read              :	96140
20220112-20:55:48: [INFO]: Total files written           :	100337
20220112-20:55:48: [INFO]: Total files with read errors  :	0
20220112-20:55:48: [INFO]: Total files with write errors :	17214
20220112-20:55:48: [INFO]: Total samples processed       :	2020517
20220112-20:55:48: [INFO]: Total unique samples          :	2020517
20220112-20:55:48: [INFO]: ---------------------------------------------------------
20220112-20:55:48: [WARN]: See the error log for processing error details.
20220112-20:55:48: [INFO]: 
The data in
/data/raw/alawai/AW02XX_001CTDXXXXR00/
have been moved to
/backup/recovery/2022-01-13-03-32
20220112-20:55:48: [INFO]: If there were errors, don't delete the recovery directory so they can be dealt with.
20220112-20:55:48: [INFO]: Review the new files, and if you are satisfied with the rebuild, delete the recovery directory.
```

After the data have been read, sorted, and de-duplicated, they are written back to the raw and processed data directories.  The raw data are first moved into the `/backup/recovery/{current-timestamp}` directory, and the processed data are just deleted entirely.  After all of the raw and processed data are written, a short summary of what happened is shown.

## Looking further into errors

If there are lines with `[ERROR]` in the output, or if there are read or write errors in the summary, you'll need to investigate further.  This is usually caused by data that have the wrong number of columns, or that have corrupt data lines that don't match the `dataPattern` in the configuration file.

To look further, change directories into the `/backup/recovery/{timestamp}` directory and run the following command, for example:

```
$ cd /backup/recovery/2022-01-12-20-55
$ jq '.[] | select(.message | contains("No data") | not)' ./*rebuild_errors.json
```

This parses the JSON-based error file and searches for errors that don't contain the words "No data".  This filters out all of the errors where there were just no data for that time period to write to disk.

Look at the reasons for each error.  A column width error would look similar to this:

```
{
  "path": "/data/raw/alawai/AW02XX_001CTDXXXXR00/DecimalASCIISampleData/2019/05/10/AW02XX_001CTDXXXXR00_20190510030000.10.1.dat",
  "message": "java.util.concurrent.ExecutionException: tech.tablesaw.io.AddCellToColumnException: Error while adding cell from row 0 and column C0(position:0): Row number 0 contains 6 columns. 7 expected."
}
```
An example of a data sample that could not be parsed because of corrupted text is:

```
{
  "path": "/data/raw/alawai/AW02XX_001CTDXXXXR00/DecimalASCIISampleData/2014/06/12/AW02XX_001CTDXXXXR00_20140612220000.10.1.dat",
  "message": "java.util.concurrent.ExecutionException: tech.tablesaw.io.AddCellToColumnException: Error while adding cell from row 3 and column C4(position:4): Text '�122 Jun 2014 12:13:21' could not be parsed at index 0"
}
```
Each of these files need to be edited manually to remove or correct the corrupt samples, or to fix the column widths so they adhere to the `dataPattern` in the configuration file.

## Rerunning the rebuilder after restoring data

Once you've fixed all errors with the data, it's usually best to delete the current data that were written to `/data/raw/{area}/{instrument}`, and copy all of the recovery data back into place before you re-run the rebuilder command. For instance:

> Note: Before doing so, confirm that all of the raw data were written into the recovery directory to make sure you don't lose any data

```
$ rm -rf /data/raw/alawai/AW02XX_001CTDXXXXR00/*
$ cp -r /backup/recovery/2022-01-12-20-55/data/raw/alawai/AW02XX_001CTDXXXXR00/* /data/raw/alawai/AW02XX_001CTDXXXXR00/
```

Then, re-run the rebuilder command.

## Rebuilding Unarchived Instrument Data

There may be cases where an instruments archiver fails to write data from the Data Turbine to disk.  This could be due to a Data Turbine communication error, or because the archiver process has stopped or failed for one reason or another.  This is often fixed by restarting the given archiver.

You can rebuild the archive and gap fill the missing data by fetching the data from the Data Turbine, putting it into the instrument's raw data directory, and running the rebuilder command as usual.  For instance, to grab the last 7 days of data for the `WK02XX_001CTDXXXXR00` instrument, run the following in a terminal:

```
$ identifier="WK02XX_001CTDXXXXR00"
$ baseDirectory="/data/raw/alawai/"
$ durationInSeconds=$(bc <<< "60 * 60 * 24 * 7") # Calculate the number of seconds in 7 days
$curl -s -o "${baseDirectory}${identifier}/${identifier}-gapfill.txt" \
    "https://realtime.pacioos.hawaii.edu/RBNB/${identifier}/DecimalASCIISampleData?d=${durationInSeconds}&r=newest"
```

This will fetch 7 days worth of data from the Data Turbine and put it in `/data/raw/alawai/WK02XX_001CTDXXXXR00/WK02XX_001CTDXXXXR00-gapfill.txt`.  Then, run the `manage-instruments` with the `-c rebuilder` command as described above to integrate these data into the archive.

## Reloading Instrument Data

Once you have successfully rebuilt the data archive for an instrument, the most recent year's worth of data needs to be reloaded into the Data Turbine.  If you are rebuilding an `online` instrument that is actively streaming, stop the instrument driver first before reloading data.  To reload the data, use the reloader command. For example:

```
$ manage-instruments -c reloader -i AW02XX_001CTDXXXXR00 -d /data/raw/alawai/AW02XX_001CTDXXXXR00/
```

This process will read in all of the archive data files (just like the rebuilder).  It will filter out all but the most recent year's worth of data.  It will connect to the Data Turbine and clear out the data for the given instrument, and then will upload the latest year worth of data.

Once this process is complete, if the instrument is `online`, start the driver back up to continue streaming data.