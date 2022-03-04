# Configuring Instrument Drivers

Each instrument deployment gets documented in an XML configuration file in the ``${REALTIME_DATA}/conf`` directory with the same name as the instrument name.  For instance, the Waikiki Beach CTD is named ``WK01XX_001CTDXXXXR00``, and the corresponding XML file is ``WK01XX_001CTDXXXXR00.xml``.

To create a new instrument configuration, it's easiest to just copy an existing XML configuration document and add/replace values as need.  Here's an [example document](https://github.com/csjx/realtime-data/blob/master/conf/online/WK01XX_001CTDXXXXR00.xml).

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
