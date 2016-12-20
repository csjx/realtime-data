Realtime Data
=============

Realtime data provides software for streaming data from oceanographic instruments, and uses the [DataTurbine](http://dataturbine.org) as the realtime streaming middleware server. The software originated with the Benthic Boundary Layer (BBL) Project at the University of Hawaii, Manoa. The BBL Project was associated with the [Kilo Nalu Nearshore Observatory](http://www.soest.hawaii.edu/OE/KiloNalu/), the [Hawaii Ocean Observing System](http://soest.hawaii.edu/hioos), and the [Pacific Islands Ocean Observing System](http://pacioos.org). The project was supported by a National Science Foundation grant (NSF Award #OCE-0536607-000) to the University of Hawaii.

* Contributors: Christopher Jones, Margaret McManus, Geno Pawlak, Judith Wells, KR MacDonald, Ross Timmerman, Conor Jerolmon, Joseph Gilmore
* Developed at: [School of Ocean and Earth Science and Technology, University of Hawaii at Manoa](http://soest.hawaii.edu)

The BBL Project is an open source, community project.  We welcome contributions in many forms, including code, graphics, documentation, bug reports, testing, etc.

License
-------
```
Copyright [2013] [Regents of the University of Hawaii and the School of Ocean and Earth Science and Technology]

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

Set up the software by unpacking the zip file and creating the log directory:
    
    $ cd /usr/local
    $ sudo unzip bbl-1.0.0-SNAPSHOT-pacioos.zip
    $ sudo chown -R ${USER} /usr/local/pacioos
    $ export BBL_HOME=/usr/local/pacioos
    $ sudo mkdir -p /var/log/bbl
    $ sudo chown -R ${USER} /var/log/bbl
    
Edit or add configuration files for each instrument in the `conf/` directory, setting the correct connection type, DataTurbine address, port, instrument host, port, etc.  Ensure the dataPattern matches a line of data from the instrument, and ensure the dateFormats and dateFields correctly describe the formats and locations of the date/time variables in the data sample text.

Start one or more instrument drivers with the management script. Get the usage with:

    $ $BBL_HOME/scripts/shell/manage-instruments.sh -h






