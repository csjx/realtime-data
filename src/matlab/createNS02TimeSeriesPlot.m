% createTimeSeriesPlot 
%   This is a short script that creates time series plots of data
%   from DataTurbine sources.  It depends on configuration information
%   found in the Configure class, and creates an instance of the
%   DataProcessor class, and then runs the process() method to create
%   the desired figures.

%  Copyright: 2010 Regents of the University of Hawaii and the
%             School of Ocean and Earth Science and Technology 
%    Authors: Christopher Jones             
%  
% $HeadURL$
% $LastChangedDate$
% $LastChangedBy$
% $LastChangedRevision$
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
'baseDirectory'       , '/Users/cjones/Documents/Development/bbl/trunk/'  , ...
'outputDirectory'     , '/Users/cjones/Documents/Development/bbl/trunk/test/'       , ...
'libraryDirectory'    , '/Users/cjones/Documents/Development/bbl/trunk/lib/matlab/' , ...
'convertPath'         , '/opt/local/bin/gs'                               , ...
'convertOptions'      , [' -q'                                              ...
                         ' -dNOPAUSE'                                       ...
                         ' -dBATCH'                                         ...
                         ' -dSAFER'                                         ... 
                         ' -dTextAlphaBits=4'                               ...
                         ' -dGraphicsAlphaBits=4'                           ...
                         ' -dJPEGQ=100'                                     ...
                         ' -sDEVICE=jpeg'                                   ...
                         ' -r90x90'                                         ...
                         ' -sOutputFile='                                   ...
                         ]                                                , ...
'copyPath'            , '/bin/cp'                                         , ...
'rbnbPath'            , '/Applications/RBNB/current/'                     , ...
'rbnbLibraryPath'     , '/Applications/RBNB/current/bin/rbnb.jar'         , ...
'rbnbMatlabPath'      , '/Applications/RBNB/current/Matlab/'                ...
);                                                                        

% Set the pertinent DataTurbine configuration details (so they're not hard-coded)
% where:
%
% rbnbServer        - is the IP address or name of the DataTurbine server
% rbnbSinkName      - is the name of the sink client to be used when connecting
% rbnbSource        - is the name of the DataTurbine source for the instrument
% rbnbChannel       - is the name of the DataTurbine channel for the instrument
% startDate         - is the start date in UTC(mm-dd-yyyy HH:MM:SS) that the 
%                     DataTurbine queries will use
% duration          - is the duration (in seconds) that DataTurbine queries will use
% reference         - is the channel reference point (oldest, newest, absolute)
%                     If reference is newest, the query will fetch duration in
%                     seconds prior to the current time.  If reference is oldest,
%                     the query will fetch duration in seconds after the oldest
%                     observation in the DataTurbine.  If reference is absolute,
%                     the query will fetch duration in seconds after the startDate.
set( configuration,                                                         ...
'rbnbServer'          , 'bbl.ancl.hawaii.edu'                             , ...
'rbnbSinkName'        , 'MatlabAW02XX_001CTDXXXXR00PlottingSink'          , ...
'rbnbSource'          , 'AW02XX_001CTDXXXXR00'                            , ...
'rbnbChannel'         , 'DecimalASCIISampleData'                          , ...
'dataStartDate'       , '02-24-2010 10:00:00'                             , ... % UTC
'duration'            , 1209600                                           , ...
'reference'           , 'absolute'                                          ...
);                                                                        

% Set the boolean flags that enable or disable which parts of the code run where:
%
% createFigures     - is a flag to either create or not create figures
% exportFigures     - is a flag to export (to EPS) or not to export figures                                                                         
set( configuration,                                                         ...
'createFigures'       , true                                              , ...
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
'dataFormatString'    , '# %f %f %f %f %f %s'                             , ...
'fieldDelimiter'      , ','                                               , ...
'numberOfHeaderLines' , '0'                                               , ...
'instrumentSampleRate', 15                                                , ...
'dataVariableNames'   , {'temperature'                                    , ...
                         'conductivity'                                   , ...
                         'chlorophyllVolts'                               , ...
                         'turbidityVolts'                                 , ...
                         'salinity'                                       , ...
                         'datetime'                                         ...
                        }                                                 , ...
'dataVariableUnits'   , {'\circC'                                         , ...
                         'S/m'                                            , ...
                         'V'                                              , ...
                         'V'                                              , ...
                         'PSU'                                            , ...
                         'dd mmm yyyy HH:MM:SS'                             ...
                        }                                                   ...
);                                                                        
                                                                          
set( configuration,                                                         ...
'derivedVariableNames', {'serialdate'                                     , ...
                         'chlorophyll'                                    , ...
                         'turbidity'                                        ...
                        }                                                   ...
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
                               0.009                                 , ...
                               10                                    , ...
                               0.065                                 , ...
                               5                                       ...
                             }                                         ...
);

% Set the configuration parameters for the time series figures that should
% be generated.  The timeSeriesFigures property is a cell array that includes
% one or more cell arrays that contain figure properties:
% timeSeriesFigures = { {figure 1 properties}, {figure 2 properties}, ...}
% For each figure to be generated, the following properties must be included,
% in the order listed below:
%
% Figure Title Prefix       : The prefix string used at the top of the figure. This
%                             will be followed by 'latest observation period ...'
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
                         % 3-day plot
                         {'Ala Wai Canal (NS02), 3 Day Water Quality' , ... % titlePrefix
                          '02-26-2010 10:00:00'                        , ... % figure start in UTC
                          '259200'                                     , ... % duration
                          {'temperature'                               , ... % xAxisVars
                           'salinity'                                  , ... 
                           'chlorophyll'                               , ... 
                           'turbidity'}                                , ... 
                          {'serialdate'}                               , ... % yAxisVar
                          '.5'                                         , ... % xTickStep
                          [{'%3.2f'},{'%3.2f'},{'%3.2f'},{'%3.2f'}]    , ... % tickFormat
                          [{'.'},{'.'},{'.'},{'.'}]                    , ... % marker
                          [{3.0},{3.0},{3.0},{3.0}]                    , ... % markerSize
                          [                                              ...
                            {[255/255 0       0      ]}                , ... % red
                            {[0        0       255/255]}               , ... % blue
                            {[0        255/255 0      ]}               , ... % green
                            {[102/255  047/255 0      ]}                 ... % brown
                          ]                                            , ...
                          '0'                                          , ... % include moving avg
                          '1200'                                       , ... % moving avg duration
                          [128/255 128/255 128/255]                    , ... % moving avg color: gray
                          '1'                                            ... % moving avg linewidth
                         }                                             , ...
                         % 10-day plot
                         {'Ala Wai Canal (NS02), 10 Day Water Quality' , ... % titlePrefix
                          '02-24-2010 10:00:00'                        , ... % figure start in UTC
                          '864000'                                     , ... % duration
                          {'temperature'                               , ... % xAxisVars
                           'salinity'                                  , ... 
                           'chlorophyll'                               , ... 
                           'turbidity'}                                , ... 
                          {'serialdate'}                               , ... % yAxisVar
                          '1'                                          , ... % xTickStep
                          [{'%3.2f'},{'%3.2f'},{'%3.2f'},{'%3.2f'}]    , ... % tickFormat
                          [{'.'},{'.'},{'.'},{'.'}]                    , ... % marker
                          [{3.0},{3.0},{3.0},{3.0}]                    , ... % markerSize
                          [                                              ...
                            {[255/255 0       0      ]}                , ... % red
                            {[0        0       255/255]}               , ... % blue
                            {[0        255/255 0      ]}               , ... % green
                            {[102/255  047/255 0      ]}                 ... % brown
                          ]                                            , ...
                          '0'                                          , ... % include moving avg
                          '1200'                                       , ... % moving avg duration
                          [128/255 128/255 128/255]                    , ... % moving avg color: gray
                          '1'                                            ... % moving avg linewidth
                         }                                             , ...
                       }                                                 ...
);


% Set up directory paths, and create a new DataProcessor instance
import edu.hawaii.soest.bbl.processing.DataProcessor;
javaaddpath(configuration.rbnbLibraryPath);
addpath(configuration.rbnbMatlabPath);
addpath(configuration.libraryDirectory);
NS02ctdProcessor = DataProcessor(configuration);
NS02ctdProcessor.process();