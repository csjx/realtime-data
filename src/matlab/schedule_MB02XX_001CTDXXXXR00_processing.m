% schedule_MB02XX_001CTDXXXXR00_processing 
%   This is a short runtime script that kicks off processing and
%   display of CTD data.  It depends on configuration information
%   found in the Configure class, and creates an instance of the
%   CTDProcessor class, and then runs the process() method via a
%   timer object so that the processing regularly recurs.

%  Copyright: 2007 Regents of the University of Hawaii and the
%             School of Ocean and Earth Science and Technology 
%    Authors: Christopher Jones             
%  
% $HeadURL: https://bbl.ancl.hawaii.edu/projects/bbl/trunk/src/matlab/schedule_MB02XX_001CTDXXXXR00_processing.m $
% $LastChangedDate: 2013-11-27 16:48:35 -1000 (Wed, 27 Nov 2013) $
% $LastChangedBy: kilonalu $
% $LastChangedRevision: 1106 $
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
% baseDirectory     - is the location where the bbl software is installed    
% outputDirectory   - is the location where figure image files will be written
% libraryDirectory  - is where 3rd party matlab functions are located
% convertPath       - is the location of the command used to convert figures
% convertOptions    - are the options to be passed to the convert command
% copyPath          - is the location of the 'copy' command for the OS
% rbnbPath          - is the base directory for the DataTurbine installation
% rbnbLibraryPath   - is the library directory for the DataTurbine installation
% rbnbMatlabPath    - is the matlab library directory for the DataTurbine
set( configuration,                                                         ...
'baseDirectory'       , '/usr/local/bbl/trunk/'                           , ...
'outputDirectory'     , '/var/www/html/OE/KiloNalu/Data/CTD/MB02XX_001CTDXXXXR00/' , ...
'libraryDirectory'    , '/usr/local/bbl/trunk/lib/matlab/'                , ...
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
% rbnbServer        - is the IP address or name of the DataTurbine server
% rbnbSinkName      - is the name of the sink client to be used when connecting
% rbnbSource        - is the name of the DataTurbine source for the instrument
% rbnbChannel       - is the name of the DataTurbine channel for the instrument
% archiveDirectory  - is the name up the upper level archive directory for
%                     the instrument
% startDate         - is the start date in UTC(mm-dd-yyyy HH:MM:SS) that the 
%                     DataTurbine queries will use
% timeOffset        - is the difference in hours between the sensor time and UTC
%                     (ie. local timezone)
% duration          - is the duration (in seconds) that DataTurbine queries will use
% reference         - is the channel reference point (get oldest or newest data)
set( configuration,                                                         ...
'rbnbServer'          , 'bbl.ancl.hawaii.edu'                             , ...
'rbnbSinkName'        , 'MatlabMB02XX_001CTDXXXXR00ProcessingSink'        , ...
'rbnbSource'          , 'MB02XX_001CTDXXXXR00'                            , ...
'rbnbChannel'         , 'DecimalASCIISampleData'                          , ...
'archiveDirectory'    , 'alawai'                                          , ...
'dataStartDate'       , '10-10-2013 00:00:00'                             , ... % UTC
'sensorTimeOffset'    , -10                                               , ...
'duration'            , 2678400                                           , ...
'reference'           , 'newest'                                            ...
);                                                                        

% Set the boolean flags that enable or disable which parts of the code run where:
%
% createFigures     - is a flag to either create or not create figures
% exportFigures     - is a flag to export (to EPS) or not to export figures                                                                         
set( configuration,                                                         ...
'readArchive'         , false                                             , ...
'createFigures'       , true                                              , ...
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
                               0.070                                 , ...
                               10                                    , ...
                               0.040                                 , ...
                               5                                       ...
                             }                                         ...
);

% Set the depth (in meters) of the sensor below average Mean Low Low Water 
% level to correct output to MLLW  
set(configuration,                                                     ...
    'MLLWadjustment',         -1.08                                    ...
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
                       % Figure 1  (7 day plot)
                       {{'Nearshore Sensor, Wailupe (NS16), 7 day'}       , ... %Title prefix
                        {'7'}                                             , ... %Duration in days
                        {'.eps'}                                            ... %Output format
                        }                                                 ; ...
                       % Figure 2  (30 day plot)
                       {{'Nearshore Sensor, Wailupe (NS16), 30 day'}      , ... %Title prefix
                        {'30'}                                            , ... %Duration in days
                        {'.eps'}                                            ... %Output format
                        }                                                 ; ...
                       % Figure 3  (monthly plot)
                       {{'Nearshore Sensor, Wailupe (NS16)'}              , ... %Title prefix
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
                           {'fixed', [0 25 50 100 250 500 1000]} , ... %Turb
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

% Set the configuration parameters for the time series figures that should
% be generated.  The timeSeriesFigures property is a cell array that includes
% one or more cell arrays that contain figure properties:
% timeSeriesFigures = { {figure 1 properties}, {figure 2 properties}, ...}
% For each figure to be generated, the following properties must be included,
% in the order listed below:
%
% Figure Title Prefix       : The prefix string used at the top of the figure. This
%                             will be followed by 'latest observation period ...'
% Figure Start Date         : the start date (mm-dd-yyyy HH:MM:SS) in UTC, this
%                             is for TS plots only, set to empty string for timeSeries
% Figure Duration           : the number of seconds that the plots in the figure will
%                             represent (given as an integer)
% {YAxis Variables}         : A cell array of Y axis variable names as strings, 
%                             one for each subplot to be rendered in the figure. 
%                             The variable names must exactly match one of the 
%                             variable names in the dataVariableNames cell array, and
%                             can include derived variable names that are added.
%                             (typically 'temperature', 'chlorophyll', etc.)
% {XAxis Variable}          : A cell array with the X axis variable name as a string, 
%                             The variable name must exactly match one of the 
%                             variable names in the dataVariableNames cell array, and
%                             can include derived variable names that are added.
%                             (typically 'serialdate')
% XTick Step                : A number that is used as the step factor in determining
%                             how many tick labels will be placed on the X axis.  A
%                             smaller number will increase the number of tick labels
%                             (e.g. '.125' to produce hourly ticks), and larger values
%                             will decrease the number of tick labels (e.g. '3' on
%                             on a 21 day plot will reduce the labels to 7)
% Tick Format               : An array with number format strings, one for each
%                             subplot being produced.  Tick label numbers will be 
%                             formatted withthis string.
% Graphic Marker            : The graphic marker used to represent individual
%                             observations in the timeseries plot, one per subplot
% Graphic Markersize        : The size of each of the graphic markers, one per subplot
% Graphic Colors            : The graphic colors used for observations, one per subplot
% Moving Average Flag       : A '1' or '0' indicating whether or not a moving average
%                             should be calculated and rendered on the plots.
% Moving Average Duration   : A number (as a string) that indicates the duration
%                             of the moving average in seconds (e.g 1200 for a
%                             20 minute moving average)
% Moving Average Color      : The color to be used for the moving average line
% Moving Average Line Width : The width of the moving average line (as a string)
set( configuration,                                                    ...
'timeSeriesFigures'   , {                                              ...
                         % Figure 1                                    
                         {'Wailupe (NS16), 7 Day Water Quality'        , ... % titlePrefix
                          ''                                           , ... % figure start in UTC
                          '604800'                                     , ... % duration
                          {'temperature'                               , ... % xAxisVars
                           'salinity'                                  , ... 
                           'chlorophyll'                               , ... 
                           'turbidity'                                 , ...
                           'depth'}                                    , ... 
                          {'serialdate'}                               , ... % yAxisVar
                          '1'                                          , ... % xTickStep
                          [{'%3.2f'},{'%3.2f'},{'%3.2f'},{'%3.2f'},{'%3.2f'}]    , ... % tickFormat
                          [{'.'},{'.'},{'.'},{'.'},{'.'}]              , ... % marker
                          [{1.0},{1.0},{1.0},{1.0},{1.0}]              , ... % markerSize
                          [                                              ...
                            {[255/255 0       0      ]}                , ... % red
                            {[0        0       255/255]}               , ... % blue
                            {[0        255/255 0      ]}               , ... % green
                            {[102/255  047/255 0      ]}                 ... % brown
                            {[102/255  102/255 102/255]}                 ... % gray 
                          ]                                            , ...
                          '0'                                          , ... % include moving avg
                          '1200'                                       , ... % moving avg duration
                          [128/255 128/255 128/255]                    , ... % moving avg color: gray
                          '1'                                            ... % moving avg linewidth
                         }                                             , ...
                         % Figure 2                                    
                         {'Wailupe (NS16), 10 Day Water Quality'       , ... % titlePrefix
                          ''                                           , ... % figure start in UTC
                          '864000'                                     , ... % duration
                          {'temperature'                               , ... % xAxisVars
                           'salinity'                                  , ... 
                           'chlorophyll'                               , ... 
                           'turbidity'                                 , ... 
                           'depth'}                                    , ...                               
                          {'serialdate'}                               , ... % yAxisVar
                          '1'                                          , ... % xTickStep
                          [{'%3.2f'},{'%3.2f'},{'%3.2f'},{'%3.2f'},{'%3.2f'}]    , ... % tickFormat
                          [{'.'},{'.'},{'.'},{'.'},{'.'}]              , ... % marker
                          [{1.0},{1.0},{1.0},{1.0},{1.0}]              , ... % markerSize
                          [                                              ...
                            {[255/255 0       0      ]}                , ... % red
                            {[0        0       255/255]}               , ... % blue
                            {[0        255/255 0      ]}               , ... % green
                            {[102/255  047/255 0      ]}               , ... % brown
                            {[102/255  102/255 102/255]}                 ... % gray
                          ]                                            , ...
                          '0'                                          , ... % include moving avg
                          '1200'                                       , ... % moving avg duration
                          [128/255 128/255 128/255]                    , ... % moving avg color: gray
                          '1'                                            ... % moving avg linewidth
                         }                                             , ...
                         % Figure 3                                    
                         {'Wailupe (NS16), 30 Day Water Quality'       , ... % titlePrefix
                          ''                                           , ... % figure start in UTC
                          '2592000'                                    , ... % duration
                          {'temperature'                               , ... % xAxisVars
                           'salinity'                                  , ... 
                           'chlorophyll'                               , ... 
                           'turbidity'                                 , ... 
                           'depth'}                                    , ...                               
                          {'serialdate'}                               , ... % yAxisVar
                          '3'                                          , ... % xTickStep
                          [{'%3.2f'},{'%3.2f'},{'%3.2f'},{'%3.2f'},{'%3.2f'}]    , ... % tickFormat
                          [{'.'},{'.'},{'.'},{'.'},{'.'}]              , ... % marker
                          [{1.0},{1.0},{1.0},{1.0},{1.0}]              , ... % markerSize
                          [                                              ...
                            {[255/255 0       0      ]}                , ... % red
                            {[0        0       255/255]}               , ... % blue
                            {[0        255/255 0      ]}               , ... % green
                            {[102/255  047/255 0      ]}               , ... % brown
                            {[102/255  102/255 102/255]}                 ... % gray
                          ]                                            , ...
                          '0'                                          , ... % include moving avg
                          '1200'                                       , ... % moving avg duration
                          [128/255 128/255 128/255]                    , ... % moving avg color: gray
                          '1'                                            ... % moving avg linewidth
                         }                                             , ...
                       }                                                 ...
);

% Set the timer specific configuration details for periodically running the code
% where:
%
% startTime         - the time when the timer should start (in seconds, 0 meaning now)
% timerInterval     - the interval (in minutes) that the timer process should fire
set( configuration,                                                  ...
'startTime'           , 0                                          , ...
'timerInterval'       , 20                                           ...
);                                                                 

% Set up directory paths, and create a new DataProcessor instance
import edu.hawaii.soest.bbl.processing.DataProcessor;
javaaddpath(configuration.rbnbLibraryPath);
addpath(configuration.rbnbMatlabPath);
addpath(configuration.libraryDirectory);
NS16ctdProcessor = DataProcessor(configuration);

%keyboard
NS16ctdProcessor.process()

% schedule the processing
% set the timer start time based on the timer interval.
set(NS16ctdProcessor, 'timerStartTime', NS16ctdProcessor.configuration.timerInterval);

% set the timer object instance
set(NS16ctdProcessor, 'timerObject',                     ...
  timer('TimerFcn',                                    ...
        'NS16ctdProcessor.process',                      ...
        'period',                                      ...
        NS16ctdProcessor.configuration.timerInterval*60, ...
        'executionmode',                               ...
        'fixeddelay'));
startat(NS16ctdProcessor.timerObject, NS16ctdProcessor.timerStartTime);
