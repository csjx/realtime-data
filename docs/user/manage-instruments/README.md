# Managing Nearshore Realtime Instruments

The PacIOOS nearshore instruments fall into three categories:

* `online`
* `offline`
* `retired`

The `online` instruments are actively collecting and streaming data via a realtime cellular modem directly to `realtime.pacioos.hawaii.edu`.  The `offline` instruments are actively collecting data, but the data are manually collected roughly on a monthly basis and added to the archive.  Lastly, `retired` instruments are instruments that were once at a site but are no longer, or that have an older configuration no longer in use.  A good example is when the number of voltage channels changes for an instrument, like when a turbidity meter is added.  The old configuration will be retired.

The following pages walk through how to manage these instruments.  The instructions assume you have connected to `realtime.pacioos.hawaii.edu` via `SSH` on port `2222` as the `kilonalu` user:

```
$ ssh -p 2222 kilonalu@realtime.pacioos.hawaii.edu
```

## Data Storage and Formats
Raw and processed data are streamed into the Data Turbine in real time, and are written hourly to the `/data/raw` directory, and are converted to the `pacioos-2020-format` and written daily to the `/data/processed/pacioos` directory.  An example of raw Seabird instrument data for NS10 is:

```
# 24.0280, 5.26224, 1.369, 0.1694, 0.9492, 35.4265, 05 Apr 2013 18:03:43
```

whereas the converted equivalent data sample is:

```
2013-04-06T04:03:43Z,24.0280,5.26224,1.369,0.1694,0.9492,35.4265
```

The raw data timestamps are in local time, in this case, `Pacific/Honolulu`, whereas the processed data timestamps are in UTC, indicated by the ISO 8601 timestamp format with the `Z` suffix.  The processed data remove the `#` prefix character, remove all extra DOS line endings, convert the DOS line endings to Unix line endings (`CRLF` --> `LF`), and convert field delimiters with a comma (particularly in the case of space and tab-delimited data).






