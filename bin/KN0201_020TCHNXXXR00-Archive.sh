#!/bin/bash
export CLASSPATH=$REALTIME_DATA/realtime-data-1.1.0-jar-with-dependencies.jar;

# run the FileArchiverSink client, connecting to the RBNB server 
# @ 192.168.100.60:3333, reading from the source named 'KN0201_020TCHNXXXR00' and the 
# data channel 'DecimalASCIISampleData'.  The client is also setting the sink 
# name to be 'KN0201_020TCHNXXXR00_DecimalASCIISampleData_FileArchiver', and is
# setting the archive directory to be '
# '/data/rbnb/KN0201_020TCHNXXXR00/DecimalASCIISampleData'.  The sink client
# connects to the server once per hour, every hour, and archives the previous
# hour's worth of data collected.

java edu.hawaii.soest.kilonalu.utilities.FileArchiverSink\
 -s 192.168.100.60\
 -p 3333\
 -n KN0201_020TCHNXXXR00\
 -c DecimalASCIISampleData\
 -I hourly\
 -k KN0201_020TCHNXXXR00_DecimalASCIISampleData_FileArchiver\
 -d /data/kilonalu/KN0201_020TCHNXXXR00/DecimalASCIISampleData
