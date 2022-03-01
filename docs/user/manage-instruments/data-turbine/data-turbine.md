# Managing the DataTurbine 

The DataTurbine software is installed in `/usr/local/RBNB/current`.  It's running on port `3333` on the UH campus server (DNS: `realtime.pacioos.hawaii.edu`, VPN IP: `192.168.103.50`), and is set up as a standard Linux service installed in the `/etc/init.d` directory, with a run level script called `rbnb`.   The DataTurbine is set to start whenever each of the systems is rebooted.  The server's event log is located in `/var/log/rbnb/rbnb.log`.  The DataTurbine's internal stream archive is located in `/var/lib/rbnb`.  In the event that instrument source drivers cannot connect to the DataTurbine, look at the event log to see if there are connection, memory, or file system errors.  If so, the DataTurbine service may need to be restarted, the stream archives reloaded (automatic), and the instrument drivers reconnected.

## Starting the DataTurbine

The DataTurbine service is started like any other Linux service by calling the run-level script.  To do so, as the `kilonalu` user, ssh to the campus lab Linux server, and execute the following command in a terminal

```
$ sudo service rbnb start
```

If prompted, enter the `kilonalu` user's password.  This will start the RBNB DataTurbine service and load any existing data stream archives found in the `/var/lib/rbnb` directory.

## Stopping the DataTurbine

The DataTurbine service is stopped like any other Linux service by calling the run-level script.  To do so, as the `kilonalu` user, ssh to the Linux server in question (shore lab or campus lab), and execute the following command in a terminal
 
```
$ sudo service rbnb stop
```

If prompted, enter the `kilonalu` user's password.  This will cleanly unload any existing data stream archives and stop the RBNB DataTurbine service.

## Troubleshooting the DataTurbine

There may be times when the DataTurbine isn't performing as expected.  For instance, client source drivers may not be able to connect, or stream replication from one DataTurbine to another may not continue.  There are a few common causes to these sorts of symptoms, including server memory problems, open file problems, or disk space problems.  As the `kilonalu` user, use the following command to inspect the DataTurbine's event log to see if any critical errors are being logged

```
$ tail -f /var/log/rbnb/rbnb.log
```

This will show the most recent log entries that pertain to the DataTurbine service, such as connections, disconnections, or errors.  Type `Control-c` to stop viewing the scrolling log file.  Errors such as 'too many open files', or java.lang.OutOfMemoryException indicate resource problems on the server.  The 'too many open files'  error indicates that the DataTurbine service has exceeded it's operating system-level limits for open files.  The best solution to this is to stop and start the DataTurbine, and reconnect the instrument streams.

At times the archivers fail to fetch data from the DataTurbine, and gaps in the archive record form. This usually shows up in the log files with a negative channel index error such as:

```
[DEBUG] 2020-12-24 09:59:59,544 (FileArchiverSink:exportData:1047) Channel index is: -1
```

You can use `grep` to search the log files for these errors with:

```
$ grep 'Channel index is: -1' /var/log/realtime-data.log
```

If you see these errors in recent files, stop and start the DataTurbine as described above, and then [rebuild the unarchived instrument data](../manage-instruments/command.html#rebuilding-unarchived-instrument-data).