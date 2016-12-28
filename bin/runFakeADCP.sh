#!/bin/bash

# This is a quick shell script to run the fake ADCP stream generator, which basically
# just streams an existing ADCP PD0 binary data file over a serial port on the host machine.
# This is used for testing purposes only when a serial2IP converter like the 
# Digi Portserver TS MEI 4 port is attached to said serial port.
#
# This will only work if the RXTX libraries are installed correctly on the host machine,
# in particular, $JAVA_HOME/jre/lib/i386/librxtxSerial.so and 
# $JAVA_HOME/jre/lib/i386/librxtxParallel.so are installed.  See http://rxtx.org for
# installation of the libraries.
export REALTIME_DATA=/home/cjones/dev/realtime-data;
export CLASSPATH=${REALTIME_DATA}/realtime-data-1.0.0-jar-with-dependencies.jar

java edu.hawaii.soest.kilonalu.adcp.FakeSerialADCP 
