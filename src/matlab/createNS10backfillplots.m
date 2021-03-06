% schedule_MB01XX_001CTDXXXXR00_processing 
%   This is a short runtime script that kicks off processing and
%   display of CTD data.  It depends on configuration information
%   found in the Configure class, and creates an instance of the
%   CTDProcessor class, and then runs the process() method via a
%   timer object so that the processing regularly recurs.

%  Copyright: 2007 Regents of the University of Hawaii and the
%             School of Ocean and Earth Science and Technology 
%    Authors: Christopher Jones             
%  
% $HeadURL: https://bbl.ancl.hawaii.edu/projects/bbl/trunk/src/matlab/schedule_MB01XX_001CTDXXXXR00_processing.m $
% $LastChangedDate: 2016-03-08 17:09:19 -1000 (Tue, 08 Mar 2016) $
% $LastChangedBy: kilonalu $
% $LastChangedRevision: 1160 $
% 
% This program is free software; you can redistribute it and/or modify
% it under the terms of the GNU General Public License as published by
% the Free Software Foundation; either version 2 of the License, or
% (at your option) any later version.
% 
% This program is distributed in the hope that it will be useful,
% but WITHOUT ANY WARRANTY; without even the implied warranty of
% MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
% GNU General Public License for more details.
% 
% You should have received a copy of the GNU General Public License
% along with this program; if not, write to the Free Software
% Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
% 

% Create a new Configure instance and set the configuration details
import edu.hawaii.soest.bbl.configuration.Configure;
configuration = Configure;

% Set the processing-specific configuration details for the instrument

% Set the pertinent directory and file locations (so they're not hard-coded),
% where:
%
% baseDirectory     - is the location where the realtime-data software is installed    
% outputDirectory   - is the location where figure image files will be written
% libraryDirectory  - is where 3rd party matlab functions are located
% convertPath       - is the location of the command used to convert figures
% convertOptions    - are the options to be passed to the convert command
% copyPath          - is the location of the 'copy' command for the OS
% rbnbPath          - is the base directory for the DataTurbine installation
% rbnbLibraryPath   - is the library directory for the DataTurbine installation
% rbnbMatlabPath    - is the matlab library directory for the DataTurbine
set( configuration,                                                         ...
'baseDirectory'       , '/usr/local/realtime-data/'                           , ...
'outputDirectory'     , '/var/www/html/OE/KiloNalu/Data/CTD/MB01XX_001CTDXXXXR00/' , ...
'libraryDirectory'    , '/usr/local/realtime-data/scripts/matlab/'                , ...
'convertPath'         , '/usr/bin/gs'                                     , ...
'convertOptions'      , [' -q'                                              ...
                         ' -dNOPAUSE'                                       ...
                         ' -dBATCH'                                         ...
                         ' -dSAFER'                                         ... 
                         ' -dTextAlphaBits=4'                               ...
                         ' -dGraphicsAlphaBits=4'                           ...
                         ' -dJPEGQ=100'                                     ...
                         ' -dDEVICEWIDTH=1000'                              ...
                         ' -dDEVICEHEIGHT=800'                              ...
                         ' -sDEVICE=jpeg'                                   ...
                         ' -r90x90'                                         ...
                         ' -sOutputFile='                                   ...
                         ]                                                , ...
'copyPath'            , '/bin/cp'                                         , ...
'mkdirPath'           , '/bin/mkdir'                                      , ...
'rbnbPath'            , '/usr/local/RBNB/current/'                        , ...
'rbnbLibraryPath'     , '/usr/local/RBNB/current/bin/rbnb.jar'            , ...
'rbnbMatlabPath'      , '/usr/local/RBNB/current/Matlab/'                   ...
);                                                                        

% Set the pertinent DataTurbine configuration details (so they're not hard-coded)
% where:
%
% rbnbServer        - is the IP address or name of the4 DataTurbine server
% rbnbSinkName      - is the name of the sink client to be used when connecting
% rbnbSource        - is the name of the DataTurbine source for the instrument
% rbnbChannel       - is the name of the DataTurbine channel for the instrument
% archiveDirectory  - is the name up the upper level archive directory for
%                     the instrument
% startDate         - is the start date (mm-dd-yyyy HH:MM:SS) that the 
%                     DataTurbine queries will use
% timeOffset        - is the difference in hours between the sensor time and UTC
%                     (ie. local timezone)
% duration          - is the duration (in seconds) that DataTurbine queries will use
% reference         - is the channel reference point (get oldest or newest data)
set( configuration,                                                         ...
'rbnbServer'          , 'realtime.pacioos.hawaii.edu'                             , ...
'rbnbSinkName'        , 'MatlabMB01XX_001CTDXXXXR00ProcessingSink'        , ...
'rbnbSource'          , 'MB01XX_001CTDXXXXR00'                            , ...
'rbnbChannel'         , 'DecimalASCIISampleData'                          , ...
'archiveDirectory'    , 'alawai'                                          , ...
'dataStartDate'       , '07-01-2015 10:00:00'                             , ...
'dataEndDate'         , '01-01-2016 00:00:00'                             , ...
'sensorTimeOffset'    , -10                                               , ...
'duration'            , 'full'                                            , ...
'reference'           , 'newest'                                            ...
);                                                                        

% Set the boolean flags that enable or disable which parts of the code run where:
%
% createFigures     - is a flag to either create or not create figures
% exportFigures     - is a flag to export (to EPS) or not to export figures                                                                         
set( configuration,                                                         ...
'readArchive'         , true                                              , ...
'createFigures'       , false                                             , ...
'createPacIOOSFigures', true                                              , ...
'exportFigures'       , true                                                ...
);                                                                        

% Set the parsing specific configuration details for the ASCII data being
% returned from the DataTurbine where:
%
% dataFormatString     - is the fprintf format string used by textscan() to parse
%                        the ASCII data string
% fieldDelimiter       - is the character used to delimit variables in the data
% numberOfHeaderLines  - is the number of header lines found in the data string
% instrumentSampleRate - is an integer of the approximate per minute sampling rate of the 
%                        instrument, used by textscan() to allocate buffer memory
% dataVariableNames    - is a cell array of the data variables found in the ASCII data,
%                        in the order that each column parsed.  These must correspond
%                        with the dataFormatString (one variable per format)
% dataVariableUnits    - is a cell array of the data variable units found in the ASCII
%                        data, in the order that each column parsed.  These must 
%                        correspond with each of the dataVariableNames.  For
%                        dates, use the datestr() formats (i.e yyyy mm dd, etc.)
%                        These are used for axes labels
set( configuration,                                                         ...
'dataFormatString'    , '# %f %f %f %f %f %f %s'                          , ...
'fieldDelimiter'      , ','                                               , ...
'numberOfHeaderLines' , '0'                                               , ...
'instrumentSampleRate', 15                                                , ...
'dataVariableNames'   , {'temperature'                                    , ...
                         'conductivity'                                   , ...
                         'pressure'                                       , ...
                         'chlorophyllVolts'                               , ...
                         'turbidityVolts'                                 , ...
                         'salinity'                                       , ...
                         'datetime'                                         ...
                        }                                                 , ...
'dataVariableUnits'   , {'\circC'                                         , ...
                         'S/m'                                            , ...
                         'decibars'                                       , ...
                         'V'                                              , ...
                         'V'                                              , ...
                         'PSU'                                            , ...
                         'dd mmm yyyy HH:MM:SS'                             ...
                        }                                                   ...
);                                                                        
                                                                          
set( configuration,                                                         ...
'derivedVariableNames', {'serialdate'                                     , ...
                         'depth'                                          , ...
                         'chlorophyll'                                    , ...
                         'turbidity'                                        ...
                        }                                                   ...
);

% Set the latitude and longitude of the sensor
set(configuration,                                                          ...
'sensorLatitude', 21.28009444                                             , ...
'sensorLongitude', -157.71083                                               ...
);

% set the calibration coefficient name/value pairs.  These will be used
% in the createDerivedVariables() method for specific derivations where:
%
% calibrationCoefficientNames  - is a cell array of the names of the callibration
%                                coefficients that will be used in calculating 
%                                certain derived variables.  For instance,
%                                chlorophyllDarkCounts and chlorophyllScaleFactor
%                                (and their corresponding values) will be used
%                                to derive chlorophyll from the chlorophyllVolts
%                                column of the ASCII data.
% calibrationCoefficientValues - is a cell array of the values of the callibration
%                                coefficients that will be used in calculating 
%                                certain derived variables. these must correspond
%                                with the calibrationCoefficientNames array.
set(configuration,                                                     ...
  'calibrationCoefficientNames'                                      , ...
                             {                                         ...
                               'chlorophyllDarkCounts'               , ...
                               'chlorophyllScaleFactor'              , ...
                               'turbidityDarkCounts'                 , ...
                               'turbidityScaleFactor'                  ...
                             }                                       , ...
  'calibrationCoefficientValues'                                     , ...
                             {                                         ...
                               0.064                                 , ...
                               10                                    , ...
                               0.075                                 , ...
                               5                                       ...
                             }                                         ...
);

% Set the depth (in meters) of the sensor below average Mean Low Low Water 
% level to correct output to MLLW  
set(configuration,                                                     ...
    'MLLWadjustment',              [-1.9 -1.4]                       , ...
    'MLLWadjustmentDate',          {'10 Sep 2013 12:00:00'}            ...
    );

% Set title and duration for PaciOOS time series plots
% PacIOOSFigures = { {figure 1 properties}; {figure 2 properties}; ...}
% Set properties in order listed below for each figure to be created:
%
% Figure Title Prefix       : Sensor location and name   (string)
%
% Figure Duration           : Duration of figures in days (string)
%
% Output format             : Output as .eps and/or .jpg  
%                             (set as {.eps,.jpg} for both)

set( configuration,                                                         ...
    'PacIOOSFigures', {                                                     ...
                       % Figure 3  (monthly plot)
                       {{'Nearshore Sensor, Maunalua Bay (NS10)'}         , ... %Title prefix
                        {'monthly'}                                       , ...
                        {'.eps'}                                            ... %Output format
                        }                                                   ...
                       }                                                    ...
                    )                                                     ;
                
% Set the configuration parameters for the PacIOOS figures that should
% be generated.  PacIOOSFigureProperties is a cell array that includes
% five sub cell arrays that contain figure properties.
% For the figures to be generated, the following properties must be included,
% in the order listed below:
%
% 1) cell array of variables to plot (as strings)
% 2) cell array of axis labels (as strings)
% 3) cell array of axis locations (positions are 1-8, 1 being topmost-left
%    8 being bottommost-right
% 4) cell array of plot colors, each color represented as a 3x1 array
% 5) cell array containing info for y-axis range and scaling.
%    set 1st parameter to 'dynamic' or 'fixed'
%    if 'dynamic' then 2nd parameter is the multiple used in dynamic scaling
%    if 'fixed' then 2nd parameter is the min/max values
%     if more than 2 values input, will result in step-scaling between the
%     values (upwards if values in numerical order, downwards if in reverse
%     numerical order)
%    3rd parameter is the number of ticks for the axis (will default to 5
%     if no value set)

set(configuration,                                                 ...
    'PacIOOSFigureProperties',{                                    ...
                         {                                ... %Figure Variables
                         'adjustedDepth'                         , ...
                         'temperature'                           , ....
                         'salinity'                              , ...
                         'turbidity'                             , ...
                         'chlorophyll'                             ...
                         }                                       , ...
                        {                                ... %Axis Labels
                         'Actual WL (m)'                         , ...
                         'Temperature (\circC)'                  , ...
                         'Salinity (PSU)'                        , ...
                         'Turbidity (NTU)'                       , ...
                         'Chlorophyll (\mug/L)'                    ...
                         }                                       , ...
                         {1 2 3 4 5}                   , ... %Plot Locations
                         {                               ... %Plot Colors
                          [0    0    0   ]                       , ... %WL
                          [1    0    0   ]                       , ... %Temp
                          [0    0    0   ]                       , ... %Sal
                          [0.63 0.4  0.31]                       , ... %Turb
                          [0.1  0.55 0.35]                         ... %Chlo
                          }                                      , ...
                          {                              ... %y-axis ranges
                           {'fixed', [-0.4 1.2], 4}              , ... %WL
                           {'dynamic',  2, 4}                    , ... %Temp
                           {'fixed', [36 31 21 11 1 -4], 5}      , ... %Sal
                           {'fixed', [0 25]}                     , ... %Turb
                           {'fixed', [0 10 20 30 40 50]}           ... %Chlo
                           }                                       ...
                          }                                        ...
                        )                                        ;

% Tell the DataProcessor which type of figure to create
% Either: temperatureSalinity or timeSeries
set( configuration, 'currentFigureType', 'timeSeries');

% Tell the DataProcessor what format to use when exporting TS or timeseries
% figures
set( configuration, 'outputFormat', {'.eps' '.jpg'});


% Set the timer specific configuration details for periodically running the code
% where:
%
% startTime         - the time when the timer should start (in seconds, 0 meaning now)
% timerInterval     - the interval (in minutes) that the timer process should fire
%set( configuration,                                                  ...
%'startTime'           , 0                                          , ...
%'timerInterval'       , 20                                           ...
%);                                                                 

% Set up directory paths, and create a new DataProcessor instance
import edu.hawaii.soest.bbl.processing.DataProcessor;
javaaddpath(configuration.rbnbLibraryPath);
addpath(configuration.rbnbMatlabPath);
addpath(configuration.libraryDirectory);
NS10ctdProcessor = DataProcessor(configuration);

NS10ctdProcessor.process()
