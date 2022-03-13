# Configuring Instrument Drivers

Each instrument deployment gets documented in an XML configuration file in the ``${REALTIME_DATA}/conf`` directory with the same name as the instrument name.  For instance, the Waikiki Beach CTD is named ``WK01XX_001CTDXXXXR00``, and the corresponding XML file is ``WK01XX_001CTDXXXXR00.xml``.

To create a new instrument configuration, it's easiest to just copy an existing XML configuration document and add/replace values as need.  Here's an [example document](https://github.com/csjx/realtime-data/blob/main/conf/online/WK01XX_001CTDXXXXR00.xml).

## Adding an instrument by example

To add an instrument by copying and editing a previous XML configuration file, do the following:

1. Connect to the server:

    ```bash
    ssh -p 2222 kilonalu@realtime.pacioos.hawaii.edu
    ```

1. Locate a pre-existing instrument ID in DT.  The `online`, `offline` or `retired` directories can be searched to find an instrument ID that matches the configuration (columns, date/time stamp) of the new one you want to make. Use `cd /usr/local/realtime-data/conf/retired` to get into the `online`, `offline` or `retired` directories, then `ls -1` or `ls -l` to get a list of the files in the directory.

    ```bash
    $ ls -1
    AW01XX_002CTDXXXXR00.xml
    AW01XX_102CTDXXXXR00.xml
    AW02XX_101CTDXXXXR00.xml
    fg01_csp_fsm.xml
    MB02XX_001CTDXXXXR00.xml
    MU01XX_001YCTDXXXR00.xml
    MU01XX_101YCTDXXXR00.xml
    MU02XX_001YCTDXXXR00.xml
    PIFM01_101CTDXXXXR00.xml
    PIFM02_001CTDXXXXR00.xml
    PIGM01_001CTDXXXXR00.xml
    PIGM01_002CTDX002R00.xml
    PINM01_002CTDX008R00.xml
    pp2_pohnpei.xml
    pp3_palmyra.xml
    pp4_oahu_kewalo.xml
    pp5_oahu_waialae.xml
    pp6_palau.xml
```

1. Make a copy of the file you want to use with the `cp` command:

    ```bash
    cp instrumentname.xml newinstrumentname.xml
    ```

    For example:

    ```bash
    $ cp AW01XX_002CTDXXXXR00.xml CWB01XX_006CTDXXXXR00.xml
    ```

1. Confirm the file name with extension is in the directory.

    ```bash
    $ ls -1
    AW01XX_002CTDXXXXR00.xml
    AW01XX_102CTDXXXXR00.xml
    AW02XX_101CTDXXXXR00.xml
    CWB01XX_006CTDXXXXR00.xml
    fg01_csp_fsm.xml
    MB02XX_001CTDXXXXR00.xml
    MU01XX_001YCTDXXXR00.xml
    MU01XX_101YCTDXXXR00.xml
    MU02XX_001YCTDXXXR00.xml
    PIFM01_101CTDXXXXR00.xml
    PIFM02_001CTDXXXXR00.xml
    PIGM01_001CTDXXXXR00.xml
    PIGM01_002CTDX002R00.xml
    PINM01_002CTDX008R00.xml
    pp2_pohnpei.xml
    pp3_palmyra.xml
    pp4_oahu_kewalo.xml
    pp5_oahu_waialae.xml
    pp6_palau.xml
    ```

1. Before editing the file, check to see if the timezone in the file is correct:

    ```bash
    $ grep timeZone CWB01XX_006CTDXXXXR00.xml
    <timeZone>Pacific/Honolulu</timeZone>
    ```

    If the time zone is not correct for the given site, then find the correct Linux time zones. Use `cd /usr/share/zoneinfo/Pacific`, then `ls -1` to get the Pacific zones, and choose the correct one.

    ```bash
    $ cd /usr/share/zoneinfo/Pacific
    $ ls -l
    total 124
    -rw-r--r-- 1 root root  649 Oct 26 06:03 Apia
    lrwxrwxrwx 1 root root    5 Oct 26 06:03 Auckland -> ../NZ
    -rw-r--r-- 1 root root  296 Oct 26 06:03 Bougainville
    lrwxrwxrwx 1 root root   10 Oct 26 06:03 Chatham -> ../NZ-CHAT
    -rw-r--r-- 1 root root  296 Oct 26 06:03 Chuuk
    lrwxrwxrwx 1 root root   21 Oct 26 06:03 Easter -> ../Chile/EasterIsland
    -rw-r--r-- 1 root root  552 Oct 26 06:03 Efate
    -rw-r--r-- 1 root root  264 Oct 26 06:03 Enderbury
    -rw-r--r-- 1 root root  221 Oct 26 06:03 Fakaofo
    -rw-r--r-- 1 root root 1075 Oct 26 06:03 Fiji
    -rw-r--r-- 1 root root  183 Oct 26 06:03 Funafuti
    -rw-r--r-- 1 root root  268 Oct 26 06:03 Galapagos
    -rw-r--r-- 1 root root  186 Oct 26 06:03 Gambier
    -rw-r--r-- 1 root root  188 Oct 26 06:03 Guadalcanal
    -rw-r--r-- 1 root root  525 Oct 26 06:03 Guam
    -rw-r--r-- 1 root root  338 Oct 26 06:03 Honolulu
    lrwxrwxrwx 1 root root    8 Oct 26 06:03 Johnston -> Honolulu
    lrwxrwxrwx 1 root root    9 Oct 26 06:03 Kanton -> Enderbury
    -rw-r--r-- 1 root root  263 Oct 26 06:03 Kiritimati
    -rw-r--r-- 1 root root  386 Oct 26 06:03 Kosrae
    lrwxrwxrwx 1 root root   12 Oct 26 06:03 Kwajalein -> ../Kwajalein
    -rw-r--r-- 1 root root  339 Oct 26 06:03 Majuro
    -rw-r--r-- 1 root root  195 Oct 26 06:03 Marquesas
    -rw-r--r-- 1 root root  196 Oct 26 06:03 Midway
    -rw-r--r-- 1 root root  282 Oct 26 06:03 Nauru
    -rw-r--r-- 1 root root  229 Oct 26 06:03 Niue
    -rw-r--r-- 1 root root  933 Oct 26 06:03 Norfolk
    -rw-r--r-- 1 root root  328 Oct 26 06:03 Noumea
    lrwxrwxrwx 1 root root    6 Oct 26 06:03 Pago_Pago -> Midway
    -rw-r--r-- 1 root root  199 Oct 26 06:03 Palau
    -rw-r--r-- 1 root root  223 Oct 26 06:03 Pitcairn
    -rw-r--r-- 1 root root  334 Oct 26 06:03 Pohnpei
    lrwxrwxrwx 1 root root    7 Oct 26 06:03 Ponape -> Pohnpei
    lrwxrwxrwx 1 root root   28 Oct 26 06:03 Port_Moresby -> ../Antarctica/DumontDUrville
    -rw-r--r-- 1 root root  632 Oct 26 06:03 Rarotonga
    lrwxrwxrwx 1 root root    4 Oct 26 06:03 Saipan -> Guam
    lrwxrwxrwx 1 root root    6 Oct 26 06:03 Samoa -> Midway
    -rw-r--r-- 1 root root  187 Oct 26 06:03 Tahiti
    -rw-r--r-- 1 root root  183 Oct 26 06:03 Tarawa
    -rw-r--r-- 1 root root  398 Oct 26 06:03 Tongatapu
    lrwxrwxrwx 1 root root    5 Oct 26 06:03 Truk -> Chuuk
    -rw-r--r-- 1 root root  183 Oct 26 06:03 Wake
    -rw-r--r-- 1 root root  183 Oct 26 06:03 Wallis
    lrwxrwxrwx 1 root root    5 Oct 26 06:03 Yap -> Chuuk
    ```

    Note that some time zones are equivalent to others, for instance, `Pacific/Pago_Pago` points to `Pacific/Midway`, so they are the same zone.  


1. Use the `nano` editor for Linux to edit file, or `vim` if you are comfortable with vim commands. Replace the `<shortName>` with the new `NS[XX]` identifier, and find and replace instrument names.  Change the time zone and group if needed.

    ```bash
    $ cd /usr/local/realtime-data/conf/retired 
    $ nano CWB01XX_006CTDXXXXR00.xml 
    ```

    If the number of columns of data for the new instrument match the number of columns in the file, there is no need to change the <dataPattern> or <columnTypes> fields.  But if they differ, for instance if the turbidity column doesn’t exist for the the new sensor, change the following fields:

    - The `dataPattern` field:

    ```xml
    <dataPattern>#\s+\S+,\s+\S+,\s+\S+,\s+\S+,\s+\S+,\s+\S+,\s+\d{2}\s+\S{3}+d{4}\s+\d{2}:\d{2}:\d{2}\s*</dataPattern>
    ```

    Removing one `\s+\S+,` becomes:

    ```
    <dataPattern>#\s+\S+,\s+\S+,\s+\S+,\s+\S+,\s+\S+,\s+\d{2}\s+\S{3}+d{4}\s+\d{2}:\d{2}:\d{2}\s*</ dataPattern>
    ```

    - The `columnTypes` field:

    ```xml
    <columnTypes>
        <columnType>STRING</columnType>
        <columnType>STRING</columnType>
        <columnType>STRING</columnType>
        <columnType>STRING</columnType>
        <columnType>STRING</columnType>
        <columnType>STRING</columnType>
        <columnType>LOCAL_DATE_TIME</columnType>
    </columnTypes>
    ```

    Removing one `<columnType>STRING</columnType>` becomes:

    ```xml
    <columnTypes>
        <columnType>STRING</columnType>
        <columnType>STRING</columnType>
        <columnType>STRING</columnType>
        <columnType>STRING</columnType>
        <columnType>STRING</columnType>
        <columnType>LOCAL_DATE_TIME</columnType>
    </columnTypes>
    ```

    - The `dateFields` field:

    ```xml
    <dateFields>
        <dateField>7</dateField>
    </dateFields>
    ```

    Becomes the following, since the date field is now in the 6th column position: 

    ```xml
    <dateFields>
        <dateField>6</dateField>
    </dateFields>
    ```

    - The `archiveBaseDirectory` field:

    For instruments on Oahu, the base `raw` directory should be `/data/raw/alawai`. For instruments on Maui, the base `raw` directory should be `/data/raw/maui`.  Other Pacific insular island instruments should use base `raw` directory of `/data/raw/pacioos`.

1. Double check that the `<shortName>`, `<identifier>`, and `<rbnbName>` fields are correct and have the new instrument ids.  If these still have the old copied identifiers, a rebuild could cause an unintended overwrite of the wrong data directory.  Save the file, and proceed with building the new instrument's archive as described in the [Rebuilding Instrument Data](/manage-instruments/command.md#rebuilding-instrument-data) section.


## XML Configuration field definitions
This XML structure provides the necessary metadata for parsing, validating, archiving, and converting the data samples coming from the instrument.  The XML file has can have the following elements as children of the root ``instrument`` element:


| Element path             | Definition          | Repeatable |
|:-------------------------|:--------------------|:-----------|
| shortName                                                | The instrument short name (e.g. `NS03`)             | no         |
| **identifier**                                           | The instrument identifier (e.g. `WK01XX_001CTDXXXXR00`) | no         |
| connectionType                                           | The connection type: socket, file, or serial      | no         |
| connectionParams/hostName                                | For socket connections, the instrument host or IP | no         |
| connectionParams/hostPort                                | For socket connections, the instrument TCP port   | no         |
| filePath                                                 | For file connections, the data file full path     | no         |
| rbnbName                                                 | The name of the instrument source as it appears in the DataTurbine (e.g. `WK01XX_001CTDXXXXR00`)  | no         |
| rbnbServer                                               | The DataTurbine server hostname or IP             | no         |
| rbnbPort                                                 | The DataTurbine server host TCP port              | no         |
| archiveMemory                                            | The number of in-memory bytes to request for this instrument source.     | no         |
| archiveSize                                              | The number of on-disk bytes to request for this instrument source.       | no         |
| channels/channel                                         | A repeatable element for each channel's details.  | yes        |
| channels/channel/name                                    | The name of the channel for this instrument.      | no         |
| channels/channel/dataType                                | The type of the incoming data. For now, limited to a value of `String`.  | no         |
| channels/channel/dataPattern                             | The regular expression used to match a data sample. Assumes Java RegEx syntax. It is best to keep data pattern on one line.                 | no          |
| channels/channel/dataPrefix                              | A string of characters that prefix the data in a sample (e.g. `#`).   | no         |
| channels/channel/columnTypes/columnType                  | A physical storage type for a sample column. Allowed column types are currently: `LOCAL_DATE`, `LOCAL_DATE_TIME`, `LOCAL_TIME`, `STRING`.  | yes        |
| channels/channel/fieldDelimiter                          | The character that delimits variables within the sample. Use Hex notation for non-printing, whitespace characters (like space) (e.g `0x20`).    | no         |
| channels/channel/recordDelimiters                        | The character(s) that delimit records (samples) in a stream or file. Use Hex notation for non-printing characters, and separate characters with a pipe (e.g. 0x0d&vert;0x0A).                     | no         |
| channels/channel/missingValueCode                        | A code indicating that a value is missing in the sample (NaN, -999, etc.). | no         |
| channels/channel/dateFormats/dateFormat                  | The list of date formats for each sample date component that is in a separate variable. One or more date formats are required, reflecting the date/time variables in the data (e.g. `mm/dd/YYY`Y, e.g. `HH:MM:SS`) (Assumes Java date parsing syntax) Note: dateFormat and dateField are used together to locate, then parse the sample date.            | yes        |
| channels/channel/dateFields/dateField                    | The list of date fields for each sample date component in a separate variable. One or more date fields are required, corresponding to the date/time variable positions in the data (e.g 1 , e.g 2) (Correspond to the first and second variables in the data sample).             |  yes       |
| channels/channel/timeZone                                | The time zone identifier that the data were collected in (e.g. `Pacific/Honolulu`). The newest Java Time API parsers are strict about zone identifiers, and will fall back to UTC when the now-deprecated zone names (in the format of `HDT`, `HST`, `SST`, etc.) are used. They are considered ambiguous due to conflicting global use.  Only use identifiers that follow the `<region>/<locality>` pattern (like `Pacific/Samoa`). While an exact offset like `GMT+13:00` will also work, it may change over time due to daylight savings, so the long identifier is best.          | yes        |
| channels/channel/archivers/archiver                      | A repeatable element for each archiver's details. | yes        |
| channels/channel/archivers/<br>archiver/archiveType          | The type of the archiver. Must be "`raw`" or "`pacioos-2020-format`". | no         |
| channels/channel/archivers/<br>archiver/archiveInterval      | The interval used for archiving data files to disk from the DataTurbine channel. Must be either "`hourly`" or "`daily`". | no         |
| channels/channel/archivers/<br>archiver/archiveBaseDirectory | The base directory to archive channel data files created from the channel. Must be writable. The sourceName will be appended, and optionally the channelName (for raw data archiving).         | no         |
