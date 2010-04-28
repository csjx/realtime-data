#!/bin/bash
BBL_HOME=/usr/local/bbl/trunk
export CLASSPATH=\
$BBL_HOME/build/classes/:\
$BBL_HOME/lib/rbnb.jar:\
$BBL_HOME/lib/commons-codec-1.3.jar:\
$BBL_HOME/lib/commons-cli-1.0.jar:\
$BBL_HOME/lib/commons-logging-1.0.4.jar:\
$BBL_HOME/lib/log4j-1.2.8.jar:\
$BBL_HOME/lib/log4j.properties

export IFACE=wlan0;
export IP=$(ip addr show $IFACE | grep "inet " | tr -s " " " " | cut -d" " -f3 | cut -d"/" -f1);
export SOURCE="PIMI01_001CTDXXXXR00";

# run the FileArchiverSink client, connecting to the RBNB server 
# @ 127.0.0.1:3333, reading from the source named 'PIMI01_001CTDXXXXR00' and the 
# data channel 'BinaryPD0EnsembleData'.  The client is also setting the sink 
# name to be 'PIMI01_001CTDXXXXR00_DecimalASCIIData_FileArchiver', and is
# setting the archive directory to be
# '/data/rbnb/PIMI01_001CTDXXXXR00/DecimalASCIISampleData'.  The sink client
# connects to the server once per hour, every hour, and archives the previous
# hour's worth of data collected.

java edu.hawaii.soest.kilonalu.utilities.FileArchiverSink\
 "-s "$IP\
 "-p 3333"\
 "-n "$SOURCE\
 "-c DecimalASCIISampleData"\
 "-I hourly"\
 "-k "$SOURCE"_DecimalASCIIData_FileArchiver"\
 "-d /data/raw/alawai/"$SOURCE"/DecimalASCIISampleData"
