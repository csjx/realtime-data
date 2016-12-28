#!/bin/bash
export CLASSPATH=$REALTIME_DATA/realtime-data-1.1.0-jar-with-dependencies.jar;

export IFACE=wlan0;
export IP=$(ip addr show $IFACE | grep "inet " | tr -s " " " " | cut -d" " -f3 | cut -d"/" -f1);
export SOURCE="PINM01_002CTDX008R00";

# run the FileArchiverSink client, connecting to the RBNB server 
# @ 127.0.0.1:3333, reading from the source named 'PINM01_002CTDX008R00' and the 
# data channel 'BinaryPD0EnsembleData'.  The client is also setting the sink 
# name to be 'PINM01_002CTDX008R00_DecimalASCIIData_FileArchiver', and is
# setting the archive directory to be
# '/data/rbnb/PINM01_002CTDX008R00/DecimalASCIISampleData'.  The sink client
# connects to the server once per hour, every hour, and archives the previous
# hour's worth of data collected.

java edu.hawaii.soest.kilonalu.utilities.FileArchiverSink\
 "-s "$IP\
 "-p 3333"\
 "-n "$SOURCE\
 "-c DecimalASCIISampleData"\
 "-I hourly"\
 "-k "$SOURCE"_DecimalASCIIData_FileArchiver"\
 "-d /data/raw/pacioos/"$SOURCE"/DecimalASCIISampleData"
