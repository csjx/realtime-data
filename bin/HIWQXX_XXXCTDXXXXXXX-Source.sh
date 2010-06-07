#!/bin/bash
#BBL_HOME=/usr/local/bbl/trunk;
BBL_HOME=/Users/cjones/Documents/Development/bbl/trunk;
export CLASSPATH=\
$BBL_HOME/build/classes/:\
$BBL_HOME/lib/rbnb.jar:\
$BBL_HOME/lib/mail.jar:\
$BBL_HOME/lib/commons-codec-1.3.jar:\
$BBL_HOME/lib/commons-cli-1.0.jar:\
$BBL_HOME/lib/commons-logging-1.0.4.jar:\
$BBL_HOME/lib/commons-configuration-1.6.jar:\
$BBL_HOME/lib/commons-collections-3.2.1.jar:\
$BBL_HOME/lib/commons-lang-2.4.jar:\
$BBL_HOME/lib/log4j-1.2.8.jar:\
$BBL_HOME/lib/log4j.properties

java edu.hawaii.soest.kilonalu.ctd.StorXDispatcher\
 -s 192.168.103.50\
 -p 3333
