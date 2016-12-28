#!/bin/bash
# set up the variables
sourceName="WK02XX_001CTDXXXXR00";
sourceType="CTDSource";
sourceString="1 meter NS04 CTD";

# start the instrument driver
cd ${REALTIME_DATA};
./bin/$sourceName-Source.sh >> /var/log/rbnb/$sourceName-Source.log 2>&1 &

# tail the log file to confirm the driver is running
echo -e "\nStarted $sourceString driver\n";
echo -e "Starting to view the $sourceString streaming log.\n"; 

for i in $(seq 1 1 3)
do # flash instructions on stopping the tail process
 echo -en "\r** Type Control-C to stop viewing the log. **"; 
 sleep 1;
 echo -en "\r   Type Control-C to stop viewing the log.   "; 
 sleep 1;
done
echo -e "\n";

# start tailing the log file
tail -f /var/log/rbnb/$sourceName-Source.log; 
