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
export BBL_HOME=/home/cjones/development/bbl/trunk;
export CLASSPATH=\
/home/cjones/development/bbl/build/classes/:\
/home/cjones/development/bbl/lib/rbnb.jar:\
/home/cjones/development/bbl/lib/commons-codec-1.3.jar:\
/home/cjones/development/bbl/lib/commons-cli-1.0.jar:\
/home/cjones/development/bbl/lib/commons-logging-1.0.4.jar:\
/home/cjones/development/bbl/lib/log4j-1.2.8.jar:\
/home/cjones/development/bbl/lib/log4j.properties

java edu.hawaii.soest.kilonalu.adcp.FakeSerialADCP 
