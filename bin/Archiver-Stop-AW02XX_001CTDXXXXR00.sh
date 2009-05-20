#!/bin/bash

# set up the variables
sourceName="AW02XX_001CTDXXXXR00";
sinkType="FileArchiverSink";
sourceString="1 meter NS02 CTD";

# find any pertinent processes
processDetails=$(ps -ef | grep $sinkType | grep $sourceName);
processCount=$(echo $processDetails | grep -ve "^$" | wc -l);

# kill the driver archiver if it exists
if [ $processCount == 1 ]
then
  kill -15 `ps -ef | grep $sinkType | grep $sourceName | tr -s ' ' ' ' | cut -d" " -f2`;
  sleep 1;
  echo "Stopped the $sinkType driver connected to the $sourceString.";
else
  echo "There are no running $sinkType archivers to stop.";
fi
