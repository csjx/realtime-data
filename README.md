Realtime Data
=============

The Realtime Data Project provides streaming software for common scientific instruments used in oceanography and other environmental sciences, such as:

* RDI Workhorse ADCPs
* Seabird CTDs and SBEs
* PME T-Chains
* ECO FLNTU fluorometry/turbidity
* Brooke Ocean Seahorse profiler
* Satlantic ISUS V3 nitrate sensor
* Satlantic StorX loggers
* Davis Scientific Vantage Pro 2 weather station
* Advantech ADAM 6XXX modules

It uses the [DataTurbine](http://dataturbine.org) as the realtime streaming middleware server. 

The software originated with the Benthic Boundary Layer (BBL) Project at the University of Hawaii, Manoa. The BBL Project was associated with the [Kilo Nalu Nearshore Observatory](http://www.soest.hawaii.edu/OE/KiloNalu/), the [Hawaii Ocean Observing System](http://soest.hawaii.edu/hioos), and the [Pacific Islands Ocean Observing System](http://pacioos.org). The project was supported by a National Science Foundation grant (NSF Award #OCE-0536607-000) to the University of Hawaii.

* Documentation: https://csjx.github.io/realtime-data
* Contributors: Christopher Jones, Margaret McManus, Geno Pawlak, Judith Wells, KR MacDonald, Ross Timmerman, Conor Jerolmon, Joseph Gilmore, Gordon Walker
* Developed at: [School of Ocean and Earth Science and Technology, University of Hawaii at Manoa](http://soest.hawaii.edu)

The Realtime Data project is an open source, community project.  We welcome contributions in many forms, including code, graphics, documentation, bug reports, testing, etc.

License
-------
```
Copyright [2016] [Regents of the University of Hawaii and the School of Ocean and Earth Science and Technology]

This program is free software; you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free Software
Foundation; either version 2 of the License, or (at your option) any later
version.

This program is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program; if not, write to the Free Software Foundation, Inc., 59 Temple
Place, Suite 330, Boston, MA 02111-1307 USA
```

The 'DirSpec0' directory contains code written by Geno Pawlak for processing directional wave spectra, and includes the 'Directional Wave Spectra Toolbox, Version 1.1', written by David Johnson, Coastal Oceanography Group, Center for Water Research, University of Western Australia, Perth. This toolbox is released under the GNU General Public License; see the license.txt file in that directory for details.

Dependencies
------------

This project is built using [Apache Maven](http://maven.apache.org). It also includes code written in the [Matlab](http://mathworks.com) language for plotting data, and software written in the [Python](http://python.org) language. It has largely been developed on the MacOS X and Linux platforms, although should work under Windows using [Cygwin](http://cygwin.com).

Quick Start
-----------

Set up the software by unpacking the zip file, setting environment variables, and creating the log directory:
    
    $ cd /usr/local
    $ sudo curl -L -O https://github.com/csjx/realtime-data/raw/1.4.4/realtime-data-1.4.4-bin.zip
    $ sudo unzip realtime-data-1.4.4-bin.zip
    $ sudo chown -R ${USER} /usr/local/realtime-data
    $ export REALTIME_DATA=/usr/local/realtime-data
    $ export PATH=${PATH}:${REALTIME_DATA}/scripts/shell
    $ sudo mkdir -p /var/log/realtime-data
    $ sudo chown -R ${USER} /var/log/realtime-data
    
Edit or add configuration files for each instrument in the `conf/` directory, setting the correct connection type, DataTurbine address, port, instrument host, port, etc.  Ensure the dataPattern matches a line of data from the instrument, and ensure the dateFormats and dateFields correctly describe the formats and locations of the date/time variables in the data sample text.

Start one or more instrument drivers with the management script. Get the usage with:

    $ manage-instruments.sh -h
    
For older drivers, use the `Start-<SOURCE>.sh`, `Stop-<SOURCE>.sh`,  `Archiver-Start-<SOURCE>.sh`, and  `Archiver-Stop-<SOURCE>.sh` scripts found in `${REALTIME_DATA}/scripts/shell`.  See the documentation at https://csjx.github.io/realtime-data for details.

Development
-----------
For Java development, a few libraries need to be loaded that are not found via Maven Central.  Add the `utilities.jar`, `dhmp.jar`, and `rbnb.jar` files to your Maven repository using:

    mvn install:install-file -DgroupId=org.dataturbine -DartifactId=rbnb -Dversion=3.4b -Dpackaging=jar -Dfile=${REALTIME_DATA}/lib/rbnb.jar
    mvn install:install-file -DgroupId=edu.ucsb.nceas -DartifactId=utilities -Dversion=1.1 -Dpackaging=jar -Dfile=${REALTIME_DATA}/lib/utilities.jar
    mvn install:install-file -DgroupId=org.dhmp -DartifactId=dhmp -Dversion=1.0 -Dpackaging=jar -Dfile=${REALTIME_DATA}/lib/dhmp.jar




