#!/bin/bash
kill -15 `ps -ef | grep ADCPSource | grep KN02XX_020ADCP020R00 | cut -d" " -f2`;

echo "Stopped ADCPSource driver connected to the 20 meter node ADCP";
