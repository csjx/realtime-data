# Reference

## File Naming Conventions

The original BBL Kilo Nalu Array real-time system adopted a file naming convention that identified instruments on the array based on the site id, water depth, instrument type, instrument depth, replicate number, and begin date timestamp.  The PacIOOS nearshore instruments also adopted this naming convention, but the fields in the file names have been modified slightly.  The original format was:

| Site ID | Instrument Depth |Instrument Type | Water Depth (m) | Replicate # | Begin Timestamp | Doc # | Rev | Ext |
|:-------:|:----------------:|:--------------:|:---------------:|:-----------:|:---------------:|:-----:|:---:|:---:|
| AW02XX  | 001              | CTDX           | XXX             | R00         | 20220101010100  |10     | 1 | dat|

### PacIOOS 2020 Format
In an effort to simplify the file names, we adopted a `pacioos-2020-format` convention that dropped the `Doc #` and `Rev` fields:

| Site ID | Instrument Depth |Instrument Type | Water Depth (m) | Replicate # | Begin Timestamp | Ext |
|:-------:|:----------------:|:--------------:|:---------------:|:-----------:|:---------------:|:---:|
| AW02XX  | 001              | CTDX           | XXX             | R00         | 20220101010100  | dat|

### Convention Variations

Some of the fields in the file formats are less pertinent for the Nearshore instruments than they were for the Kilo Nalu Array when it was deployed.  Likewise, while instrument `Site IDs` get changed when the instrument is physically relocated, there are times when an instrument is located at the same site, but the measurements being collected have changed and produce data files with differing numbers of data columns.  To accommodate this after 2020, we adopted a convention that reuses the `Water Depth` field to indicate an instrument `variation` exists at the site so that post-processing software can handle the differing column counts.

An example of this is at the NS02/AW02XX site, where the `Water Depth` field has two variations, `001` and `101`:

| Site ID | Instrument Depth |Instrument Type | Water Depth (m) | Replicate # | Begin Timestamp | Ext |
|:-------:|:----------------:|:--------------:|:---------------:|:-----------:|:---------------:|:---:|
| AW02XX  | 001              | CTDX           | XXX             | R00         | 20220101010100  | dat|
| AW02XX  | 101              | CTDX           | XXX             | R00         | 20220201010100  | dat|

Subsequent file names look like:

```
AW02XX_001CTDXXXXR00_20220221180246.dat
AW02XX_101CTDXXXXR00_20200626220000.dat
```

So to summarize, variations in the number of measurement columns in each text file will result in a few file identifier changes, with the original deployment using `_001` and subsequent variations using `_101`, `201`, etc.  The trailing `01` is still the water depth at the instrument site.

### File Naming Field Definitions

The file-based naming convention include the source convention, followed by an underscore, and the following fields:

- `Site ID` - a unique identifier that is comprised of an Array ID (KN), the array Node where the instrument is located (01, 02, 03, etc...), and the Subnode where the instrument is located (01, 02, 03, etc...). In the case where a Subnode is not appropriate, an XX will be used in the Subnode identifier. The Site ID portion of the identifier will be followed by an underscore to delimit the rest of the deployment identifier.

- `Instrument Depth` - The depth of the deployed instrument in the water column, expressed in meters of water below Mean Sea Level (MSL).

- `Instrument Type` - An identifier of up to 4 letters that acts as a code for the type of instrument deployed. In the cases where less than 4 letters are used, an X is used to fill the remaining letters.

- `Water Depth` - The depth of the water column where the Node and Subnode are located, expressed in meters of water below Mean Sea Level (MSL).

- `Replicate` - the replicate number of the instrument in cases where more than one instrument of the same type is deployed at the same Site ID, Water Depth, and Instrument Depth (R00, R01, R02, etc...).

- `Begin Timestamp` - The year, month, and day, hours, minute, and second of the first observation found in the data file. This will allow for easy time-based identification of the file at a glance. A period delimiter will follow the Begin Date field.

- `Document ID` - This is a two-digit numeric identifier that is unique to the deployment. No other file with the same combination of Site ID through Begin Date fields will have the same Document ID, which allows us to unambiguously identify a deployment file and its level of processing. The Document ID numbers will use the following convention to indicate levels of processing, and can continue at increments of 10 (10, 20, 30, 40, 50 ...):
    - 10 - indicates raw data are archived in the file with no processing from the data stream 
    - 20 - indicates the first level of processing, such as minimal QA/QC procedures
    - 30 - indicates the second level of processing, such as converting to ASCII encoding
A period delimiter will follow the Document ID field.

- `Revision` - A one digit number indicating the which revision the file has undergone. In some cases, a file with the same deployment fields and Document ID will be processed incorrectly, and will need updating. This field indicates how many times it has been revised so that files of previous revisions are not deleted or removed. A period delimiter will follow the Revision field.

- `Extension` - A three letter (or number) extension indicating the type of the file. Typical file-endings will be txt (ASCII encoded text files), dat (binary data files), csv (comma-separated values text files), etc.