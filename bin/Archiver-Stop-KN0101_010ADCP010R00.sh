#!/bin/bash

# set up the variables
sourceName="KN0101_010ADCP010R00";
sinkType="FileArchiverSink";
sourceString="10 meter 1200 kHz ADCP";

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
