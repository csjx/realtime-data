Overview
========

The Kilo Nalu streaming system is a centralized means of collecting data in near real-time that uses the Open Source DataTurbine software (see http://www.dataturbine.org).  The system consists of instruments deployed on the Kilo Nalu cable array (and others via wireless connections) that communicate with the DataTurbine streaming service via customized drivers (written in Java) that understand the stream formats of each instrument.  These drivers are known as ‘Sources’, and the names of the drivers reflect this (e.g. ADCPSource).  Software client programs that connect to the DataTurbine to fetch data are known as ‘Sinks’, and their names reflect this as well (e.g. FileArchiverSink).  The primary DataTurbine installation is on the shore station linux server, and mirrors the data streams to the secondary DataTurbine installation at the UH Manoa campus via the wireless VPN connection.  At the shore station, data streams are fetched from the DataTurbine on an hourly or daily basis (depending on the specific instrument), and are archived to disk by the FileArchiverSink client for each instrument.   These files are also mirrored to the campus Linux server via an hourly process (using the `rsync` command).   Web-based graphics are produced using Matlab code that queries the campus DataTurbine on a scheduled basis.
Each of these system components are described in more detail below, along with instructions on how to manage each of them.  **Note: This guide assumes familiarity with the Linux operating system and commands.**

.. figure:: images/kilonalu-overview-diagram-simple.png
   
*Figure 1. Overview of the Kilo Nalu real-time data software and backup architecture.  Data are mirrored to the campus system using the DataTurbine software and via rsync, and are accessed by desktop and web-based applications. Note: The KiloNalu cable array is currently shut down and offline.*
  
Managing the DataTurbine Server Software
========================================

The DataTurbine software is installed in `/usr/local/RBNB/current`.  It’s running on port `3333` both on the shore station Linux server (Internet IP: `168.105.160.139`, VPN IP `192.168.100.60`) and the UH campus server (Internet IP: `bbl.ancl.hawaii.edu`, VPN IP: `192.168.103.50`), and is set up as a standard Linux service installed in the `/etc/init.d` directory, with a run level script called `rbnb`.   The DataTurbine is set to start whenever each of the systems is rebooted.  The server’s event log is located in `/var/log/rbnb/rbnb.log`.  The DataTurbine’s internal stream archive is located in `/var/lib/rbnb`.  In the event that instrument source drivers cannot connect to the DataTurbine, look at the event log to see if there are connection, memory, or file system errors.  If so, the DataTurbine service may need to be restarted, the stream archives reloaded (automatic), and the instrument drivers reconnected.

Starting the DataTurbine
------------------------

The DataTurbine service is started like any other Linux service by calling the run-level script.  To do so, as the kilonalu user, ssh to the Linux server in question (shore lab or campus lab), and execute the following command in a terminal
::

$ sudo service rbnb start

If prompted, enter the `kilonalu` user’s password.  This will start the RBNB DataTurbine service and load any existing data stream archives found in the `/var/lib/rbnb` directory.

Stopping the DataTurbine
------------------------

The DataTurbine service is stopped like any other Linux service by calling the run-level script.  To do so, as the `kilonalu` user, ssh to the Linux server in question (shore lab or campus lab), and execute the following command in a terminal
::
    
$ sudo service rbnb stop

If prompted, enter the `kilonalu` user’s password.  This will cleanly unload any existing data stream archives and stop the RBNB DataTurbine service.

Troubleshooting the DataTurbine
-------------------------------

There may be times when the DataTurbine isn’t performing as expected.  For instance, client source drivers may not be able to connect, or stream replication from one DataTurbine to another may not continue.  There are a few common causes to these sorts of symptoms, including server memory problems, open file problems, or disk space problems.  As the `kilonalu` user, use the following command to inspect the DataTurbine’s event log to see if any critical errors are being logged
::

$ tail -f /var/log/rbnb/rbnb.log

This will show the most recent log entries that pertain to the DataTurbine service, such as connections, disconnections, or errors.  Type `Control-c` to stop viewing the scrolling log file.  Errors such as ‘too many open files’, or java.lang.OutOfMemoryException indicate resource problems on the server.  The ‘too many open files’  error indicates that the DataTurbine service has exceeded it’s operating system-level limits for open files.  The best solution to this is to stop and start the DataTurbine, and reconnect the instrument streams.  Also, add an issue at https://github.com/csjx/realtime-data/issues.  Out of memory errors may be caused by the aggregate memory requests by instrument drivers exceeding the available memory on the server.  To mitigate this, the drivers can be tuned to request less memory.   As an example, the 20m 1200 kHz ADCP is started by calling the startup script found in /usr/local/bbl/trunk/bin called KN02XX_020ADCP020R00-Source.sh.  The pertinent line of this script calls the Java source driver with a number of command line parameters.
::

    java edu.hawaii.soest.kilonalu.adcp.ADCPSource\
      -H 192.168.100.139\
      -P 2102\
      -S KN02XX_020ADCP020R00\
      -C BinaryPD0EnsembleData\
      -s 192.168.100.60\
      -p 3333\
      -z 50000\
      -Z 31536000

The `-z` option requests that `50000` RBNB data frames (in this case ADCP ensembles) be stored in physical memory, whereas the `-Z` option requests that `31536000` RBNB data frames be stored on disk before they are overwritten.  For the 20m 1200 kHz ADCP, this equates to one `955` byte ensemble per frame, resulting in a memory request of `(955b x 50000) =  47.75MB`.  The on-disk storage request equates to around six months of ensembles, or `(955b x 31536000) =  30.1GB`.  These resource request values can be adjusted if the aggregate requests of all of the instrument drivers exceed the limits of the server in terms of memory and disk space.  The shore station server currently has `8GB` of physical memory, and `385GB` of disk space available to the DataTurbine.  The campus BBL server currently has `12GB` of physical memory, and `50GB` of disk space available to the DataTurbine.

Managing the DataTurbine Instrument Drivers
===========================================

Each instrument type in the water has a corresponding instrument driver used to connect it to the DataTurbine.  For instance, for ADCPs, there’s a Java-based driver called ADCPSource, and for the CTDs, there’s a driver called CTDSource.  The following table lists the instruments and their associated drivers, along with which DataTurbine they connect to by default.  Driver Source naming conventions can be found in the `data management plan`_.

.. _`data management plan`: https://github.com/csjx/realtime-data/raw/master/docs/dev/BBL-requirements-document-and-management-plan.pdf

+------------------------------+-------------------------+--------------------+-----------------+---------------------+
| Instrument Description       |  DataTurbine Name       |  Instrument IP     |  Driver Name    |   Host DataTurbine  |
+==============================+=========================+====================+=================+=====================+
| 10m CN ADAM monitor 1        |  KN00XX_010ADAM010R01   |  192.168.100.201   |  ADAMSource     |   Shore Station Lab |
+------------------------------+-------------------------+--------------------+-----------------+---------------------+
| 10m CN ADAM monitor 2        |  KN00XX_010ADAM010R02   |  192.168.100.202   |  ADAMSource     |   Shore Station Lab |
+------------------------------+-------------------------+--------------------+-----------------+---------------------+
| 10m SN ADAM monitor 1        |  KN01XX_010ADAM010R01   |  192.168.100.205   |  ADAMSource     |   Shore Station Lab |
+------------------------------+-------------------------+--------------------+-----------------+---------------------+
| 10m SN ADAM monitor 2        |  KN01XX_010ADAM010R02   |  192.168.100.206   |  ADAMSource     |   Shore Station Lab |
+------------------------------+-------------------------+--------------------+-----------------+---------------------+
| 10m 1200kHz ADCP             |  KN0101_010ADCP010R00   |  192.168.100.136   |  ADCPSource     |   Shore Station Lab |
+------------------------------+-------------------------+--------------------+-----------------+---------------------+
| 10m WetLabs FLNTU            |  KN0101_010FLNT010R00   |  192.168.100.136   |  FLNTUSource    |   Shore Station Lab |
+------------------------------+-------------------------+--------------------+-----------------+---------------------+
| 10m TChain                   |  KN0101_010TCHN010R00   |  192.168.100.136   |  TChainSource   |   Shore Station Lab |
+------------------------------+-------------------------+--------------------+-----------------+---------------------+
| 10m Seabird SBE37            |  KN0101_010SBEX010R00   |  192.168.100.136   |  SBE37Source    |   Shore Station Lab |
+------------------------------+-------------------------+--------------------+-----------------+---------------------+
| 20m Sub ADAM monitor 1       |  KN0201_010ADAM010R01   |  192.168.100.221   |  ADAMSource     |   Shore Station Lab |
+------------------------------+-------------------------+--------------------+-----------------+---------------------+
| 20m Sub ADAM monitor 2       |  KN0201_010ADAM010R02   |  192.168.100.222   |  ADAMSource     |   Shore Station Lab |
+------------------------------+-------------------------+--------------------+-----------------+---------------------+
| 20m 1200kHz ADCP             |  KN02XX_020ADCP020R00   |  192.168.100.139   |  ADCPSource     |   Shore Station Lab |
+------------------------------+-------------------------+--------------------+-----------------+---------------------+
| 20m TChain                   |  KN0201_020TCHN020R00   |  192.168.100.139   |  TChainSource   |   Shore Station Lab |
+------------------------------+-------------------------+--------------------+-----------------+---------------------+
| 20m Seahorse CTD             |  KN0201_020CTDX020R00   |  192.168.100.139   |  CTDSource      |   Campus HIG Lab    |
+------------------------------+-------------------------+--------------------+-----------------+---------------------+
| JABSOM Wx Station            |  KNWXXX_XXXDVP2XXXR00   |  168.105.160.135   |  DavisWxSource  |   Campus HIG Lab    |
+------------------------------+-------------------------+--------------------+-----------------+---------------------+
| 01m Alawai NS01 CTD          |  AW01XX_002CTDXXXXR00   |  68.25.35.242      |  CTDSource      |   Campus HIG Lab    |
+------------------------------+-------------------------+--------------------+-----------------+---------------------+
| 01m Alawai NS02 CTD          |  AW02XX_001CTDXXXXR00   |  68.25.32.149      |  CTDSource      |   Campus HIG Lab    |
+------------------------------+-------------------------+--------------------+-----------------+---------------------+
| 01m Atlantis NS03 CTD        |  WK01XX_001CTDXXXXR00   |  68.25.168.134     |  CTDSource      |   Campus HIG Lab    |
+------------------------------+-------------------------+--------------------+-----------------+---------------------+
| 01m Aquarium NS04 CTD        |  WK02XX_001CTDXXXXR00   |  68.25.74.204      |  CTDSource      |   Campus HIG Lab    |
+------------------------------+-------------------------+--------------------+-----------------+---------------------+
| 01m American Samoa NS05 CTD  |  PIAS01_001CTDXXXXR00   |  10.8.0.3          |  FileSource     |   Campus HIG Lab    |
+------------------------------+-------------------------+--------------------+-----------------+---------------------+
| 01m Micronesia NS06 CTD      |  PIFM01_001CTDXXXXR00   |  10.8.0.4          |  FileSource     |   Campus HIG Lab    |
+------------------------------+-------------------------+--------------------+-----------------+---------------------+
| 01m Marshall Islands NS07 CTD|  PIMI01_001CTDXXXXR00   |  10.8.0.5          |  FileSource     |   Campus HIG Lab    |
+------------------------------+-------------------------+--------------------+-----------------+---------------------+
| 01m Palau NS08 CTD           |  PIPL01_001CTDXXXXR00   |  10.8.0.2          |  FileSource     |   Campus HIG Lab    |
+------------------------------+-------------------------+--------------------+-----------------+---------------------+
| 01m Guam NS09 CTD            |  PIGM01_001CTDXXXXR00   |  TBD               |  TBD            |   Campus HIG Lab    |
+------------------------------+-------------------------+--------------------+-----------------+---------------------+
| 01m Maunalua Bay NS10 CTD    |  MB01XX_001CTDXXXXR00   |  24.221.193.197    |  CTDSource      |   Campus HIG Lab    |
+------------------------------+-------------------------+--------------------+-----------------+---------------------+
| 0m WQB-AW CTD, ISUS, STORX   |  HIWQXX_XXXCTDXXXXXXX   |  N/A               |  StorXSource    |   Campus HIG Lab    |
+------------------------------+-------------------------+--------------------+-----------------+---------------------+
| 0m WQB-KN CTD, ISUS, STORX   |  HIWQXX_XXXCTDXXXXXXX   |  N/A               |  StorXSource    |   Campus HIG Lab    |
+------------------------------+-------------------------+--------------------+-----------------+---------------------+
| 01m Mariana Islands CTD      |  PINM01_001CTDXXXXR00   |  N/A               |  FileSource     |   Campus HIG Lab    |
+------------------------------+-------------------------+--------------------+-----------------+---------------------+

Starting Instrument Drivers
---------------------------

Starting Drivers through Dispatchers
------------------------------------

ADAM Module engineering data
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

STOR-X Data Logger data
~~~~~~~~~~~~~~~~~~~~~~~

Stopping Instrument Drivers
---------------------------


Troubleshooting Instrument Drivers
----------------------------------

Rebuilding Channel Data
~~~~~~~~~~~~~~~~~~~~~~~

Replicating Instrument Data Streams
===================================

Managing the DataTurbine File Archivers
=======================================

Starting the Instrument File Archivers
--------------------------------------

Stopping the Instrument File Archivers
--------------------------------------

Understanding File-based replication
====================================

Managing the Matlab Instrument Plotting Code
============================================

Connecting to the Server via VNC
--------------------------------

Starting the Instrument Plotting Code
-------------------------------------

Stopping the Instrument Plotting Code
-------------------------------------

Viewing Instrument Plots
------------------------



