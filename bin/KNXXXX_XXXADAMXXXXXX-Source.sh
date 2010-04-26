#!/bin/bash
BBL_HOME=/usr/local/bbl/trunk;
export CLASSPATH=\
$BBL_HOME/build/classes/:\
$BBL_HOME/lib/rbnb.jar:\
$BBL_HOME/lib/commons-codec-1.3.jar:\
$BBL_HOME/lib/commons-cli-1.0.jar:\
$BBL_HOME/lib/commons-logging-1.0.4.jar:\
$BBL_HOME/lib/commons-configuration-1.6.jar:\
$BBL_HOME/lib/commons-collections-3.2.1.jar:\
$BBL_HOME/lib/commons-lang-2.4.jar:\
$BBL_HOME/lib/log4j-1.2.8.jar:\
$BBL_HOME/lib/log4j.properties

# Start the ADAM Dispatcher class, connect to the
# localhost computer on port 5168 to receive UDP
# packets from multiple ADAM Sources.  The 
# Dispatcher will read configuration information from
# the lib/sensor.properties.xml file, and will create
# RBNB Sources for each of the ADAM sensors listed.
# These sources will then connect to the RBNB
# DataTurbine at 192.168.100.60:3333, and will begin
# streaming ADAM data.
java edu.hawaii.soest.kilonalu.adam.AdamDispatcher\
 -H localhost\
 -P 5168\
 -S KN00XX_010ADAM010R01\
 -s 192.168.100.60\
 -p 3333
