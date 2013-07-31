#!/bin/bash

# this script reads 31 days of data from the archive in DT

cd /data/processed/read_archive 

NAME='REPLACENAME'    

rm data_$NAME.dat

# Folders and names   
#WQBAW alawai/WQAW01_XXXCTDX001R00
#WQBKN kilonalu/WQKN01_XXXCTDX001R00
#WQB04 bigisland/WQB_04
#NS02  alawai/AW02XX_001CTDXXXXR00/
#NS03  alawai/WK01XX_001CTDXXXXR00
#NS04  alawai/WK02XX_001CTDXXXXR00
#NS10  alawai/MB01XX_001CTDXXXXR00/
#NS11  pacioos/PINM01_002CTDX008R00

if [ "$NAME" == "WQAW01_XXXCTDX001R00" ] || [ "$NAME" == "AW02XX_001CTDXXXXR00" ] || [ "$NAME" == "WK01XX_001CTDXXXXR00" ] || [  "$NAME" == "WK02XX_001CTDXXXXR00" ] || [ "$NAME" == "MB01XX_001CTDXXXXR00" ]
then
   FLD='alawai'
elif [ "$NAME" == "WQKN01_XXXCTDX001R00" ]
then
   FLD='kilonalu'
elif [ "$NAME" == "WQB_04" ]
then
   FLD='bigisland'
else        #[[ "$NAME" == "PINM01_002CTDX008R00" ]]
   FLD='pacioos'
fi

#echo $FLD
ENDDATE=$(date -d '+2 days' +%Y%m%d)    
CURRENT=$(date -d '32 days ago' +%Y%m%d)  
#echo $CURRENT

#create file to write to
touch data_$NAME.dat


while [ "$ENDDATE" != "$CURRENT" ]
do

 TODAY=${CURRENT}
 YEARNOW=$(date -d "$TODAY" +%Y)
 MONTHNOW=$(date -d "$TODAY" +%m)
 DAYNOW=$(date -d "$TODAY" +%d)

 if [ -d "/data/raw/$FLD/$NAME/DecimalASCIISampleData/$YEARNOW/$MONTHNOW/$DAYNOW/" ]
 then
    filepath="/data/raw/$FLD/$NAME/DecimalASCIISampleData/$YEARNOW/$MONTHNOW/$DAYNOW/"
    for files in "$filepath"/*
    do
	cat $files >> data_$NAME.dat
    done
 fi

 CURRENT=$(date -d "$CURRENT +1 days" +%Y%m%d)

done
