#!/bin/bash
cd /usr/local/bbl/trunk;./bin/KN02XX_020ADCP020R00-Source.sh > /var/log/rbnb/KN02XX_020ADCP020R00-Source.log 2>&1 &
echo "Started ADCP Source driver to 20 meter node ADCP";
echo "Run 'tail -f /var/log/rbnb/KN02XX_020ADCP020R00-Source.log' to view the streaming log."; 
