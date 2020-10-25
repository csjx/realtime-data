Realtime Data Project
=====================

The Realtime Data Project provides streaming software for common scientific instruments used in oceanography, such as:

* RDI Workhorse ADCPs
* Seabird CTDs and SBEs
* PME T-Chains
* ECO FLNTU fluorometry/turbidity
* Brooke Ocean Seahorse profiler
* Satlantic ISUS V3 nitrate
* Satlantic StorX loggers
* Davis Scientific Vantage Pro 2 weather station
* Advantech ADAM 6XXX modules

Data that are transmitted from instruments over serial, wifi, ethernet, cellular, or satellite connections are streamed into a Data Turbine server in close to real time (depending on network latency).  The software used to stream the data (referred to as drivers) may run on a remotely deployed computer, or on the server hosting the Data Turbine server, or any other network-connected device.  The drivers are written in Java, and there is some support for streaming data using Python.  This project also provides Matlab code for retrieving data from the Data Turbine server and plotting common variables.

This guide provides documentation for the driver software, including installation, configuration, and overall use.  It also describes the operational deployment details at the University of Hawaii at Manoa.

.. toctree::
    :numbered:

    user/user-guide
    dev/rebuilder-plan

..  dev/developer-guide

Indices and tables
==================

* :ref:`genindex`
* :ref:`modindex`
* :ref:`search`

