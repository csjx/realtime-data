% CONFIGURE Create an instance of the processor configuration class
%   The Configure class provides runtime configuration information for use by
%   other classes that process near real-time data, such as the CTDProcessor class.

%  Copyright: 2007 Regents of the University of Hawaii and the
%             School of Ocean and Earth Science and Technology
% 
%    Purpose: To provide configuration information for near real-time
%             data processors, such as the CTDProcessor class.
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

classdef Configure < hgsetget & dynamicprops
  properties
    % A boolean property that enables or disables debugging 
    debug = true;
    
    % The platform specific path separator 
    pathSeparator = '/';
    
    % The source code base directory
    baseDirectory = '/usr/local/bbl/trunk/';
    
    % The output path directory
    outputDirectory = '/var/www/html/OE/KiloNalu/Data/CTD/';
    
    % The library directory for 3rd party Matlab functions or classes
    libraryDirectory = '/usr/local/bbl/trunk/lib/matlab';

    % The path to the convert program.  This may either be the 'convert' command
    % from the ImageMagick software, or the 'gs' command from Ghostscript.  This 
    % software is needed on the processing machine to produce PNG, JPG, and PDF
    % versions of the figures.
    convertPath = '/usr/bin/gs';
    
    % The list of command line options passed to the convert command.  This 
    % software is needed on the processing machine to produce PNG, JPG, or PDF
    % versions of the figures.  Using external software such as ImageMagick or 
    % Ghostscript produces better graphics than the internal Matlab versions, 
    % since they start from a higher resolution vector graphics file (EPS). 
    convertOptions = [' -q'                    ...
                      ' -dNOPAUSE'             ...
                      ' -dBATCH'               ...
                      ' -dSAFER'               ... 
                      ' -dTextAlphaBits=4'     ...
                      ' -dGraphicsAlphaBits=4' ...
                      ' -dJPEGQ=100'           ...
                      ' -sDEVICE=jpeg'         ...
                      ' -r90x90'               ...
                      ' -sOutputFile='];

    % The path to the copy program from the ImageMagick software.  This 
    % software is needed on the processing machine to produce PNG, JPG, and PDF
    % versions of the figures.
    copyPath = '/bin/cp';
    
    % The path to the mkdir program. Used to create additional directories
    % to organize output plots by date.
    mkdirPath = '/bin/mkdir';
    
    % The path to the wget program.  Used for data retrieval from remote
    % sources.
    wgetPath = '/usr/bin/wget';

    % The path of the RBNB software
    rbnbPath = '/usr/local/RBNB/current/';
    
    % The path of the RBNB java library
    rbnbLibraryPath;
    
    % The path of the RBNB java library
    rbnbMatlabPath;
    
    % The name of the RBNB Data Turbine server
    rbnbServer = 'bbl.ancl.hawaii.edu';
    
    % The port of the RBNB Data Turbine server
    rbnbPort = '3333';

    % The name of the sink client connecting to the RBNB Data Turbine server
    rbnbSinkName = 'MatlabCTDProcessingSink';
    
    % The name of the desired data source
    rbnbSource = 'KN0101_010SBEX010R00';
    
    % The name of the desired data channel
    rbnbChannel = 'DecimalASCIISampleData';
    
    %The format string used to parse the columns of data
    dataFormatString = '%n %f %f %f %f %f %f %f %s %s';
    
    % The latitude and longitude of the sensor
    sensorLatitude =  [];
    sensorLongitude = [];
    
    % The string used to designate which variable name represents a date column
    % in the data.  
    dateFieldName = 'date';
    
    % The string used to designate which variable name represents a time column
    % in the data.
    timeFieldName = 'time';
    
    % The string used to designate which variable name represents a datetime 
    % column in the data.  
    datetimeFieldName = 'datetime';
    
    % The string used to designate which variable name represents a temperature 
    % column in the data.  
    temperatureFieldName = 'temperature';
    
    % The string used to designate which variable name represents a conductivity
    % column in the data.  
    conductivityFieldName = 'conductivity';
    
    % The string used to designate which variable name represents a conductivity
    % column in the data.  
    pressureFieldName = 'pressure';
    
    % The string used to designate which variable name represents a salinity
    % column in the data.  
    salinityFieldName = 'salinity';
    
    % The string used to designate which variable name represents a chlorophyll
    % column in the data.  
    chlorophyllFieldName = 'chlorophyll';
    
    % The string used to designate which variable name represents a chlorophyll
    % voltage column in the data.  
    chlorophyllVoltageFieldName = 'chlorophyllVolts';
    
    % The string used to designate which variable name represents a chlorophyll
    % column in the data that has been converted into RFU units.  
    chlorophyllRFUFieldName = 'chlorophyllRFU';
    
    % The string used to designate which variable name represents a chlorophyll
    % dark count calibration value to interpret the chlorophylVoltages.  
    chlorophyllDarkCountsFieldName = 'chlorophyllDarkCounts';
    
    % The string used to designate which variable name represents a chlorophyll
    % scale factor calibration value to interpret the chlorophylVoltages.  
    chlorophyllScaleFactorFieldName = 'chlorophyllScaleFactor';
    
    % The string used to designate which variable name represents a turbidity
    % column in the data.  
    turbidityFieldName = 'turbidity';
    
    % The string used to designate which variable name represents a turbidity
    % voltage column in the data.  
    turbidityVoltageFieldName = 'turbidityVolts';
    
    % The string used to designate which variable name represents a turbidity
    % dark count calibration value to interpret the turbidityVoltages.  
    turbidityDarkCountsFieldName = 'turbidityDarkCounts';
    
    % The string used to designate which variable name represents a turbidity
    % scale factor calibration value to interpret the turbidityVoltages.  
    turbidityScaleFactorFieldName = 'turbidityScaleFactor';
    
    % The string used to designate which variable name represents a dissolved
    % oxygen column in the data.  
    dissolvedOxygenFieldName = 'dissolvedOxygen';
    
    % The string used to designate which variable name represents a dissolved
    % oxygen column in the data.  
    dissolvedOxygenPhaseFieldName = 'dissolvedOxygenPhase';
    
    % The string used to designate which variable name represents a dissolved
    % oxygen column in the data.  
    dissolvedOxygenVoltsFieldName = 'dissolvedOxygenVolts';
    
    % The string used to designate which variable name represents a dissolved
    % oxygen column in the data that has already been converted to mL/L.  
    dissolvedOxygenMetricFieldName = 'dissolvedOxygenMetric';
    
    % The string used to designate which variable name represents a
    % thermistor voltage column in the data.  
    thermistorVoltageFieldName = 'thermistorVolts';
    
    % The string used to designate which variable name represents a oxygen
    % saturation column in the data.  
    oxygenSaturationFieldName = 'oxygenSaturation';
    
    % The string used to designate which variable name represents a serial
    % date column in the data.  
    serialdateFieldName = 'serialdate';
    
    % The string used to designate which variable name represents an instrument
    % depth column in the data.  
    depthFieldName = 'depth';
    
    % A property used to store calibration coefficient names for use in interpreting
    % raw data voltages or signals (e.g. chlorophyll and turbidity coefficients)
    calibrationCoefficientNames = {};

    % A property used to store calibration coefficient values for use in interpreting
    % raw data voltages or signals (e.g. chlorophyll and turbidity coefficients)
    % The list must be the same length as the above calibrationCoefficientNames
    calibrationCoefficientValues = {};

    % A list (cell array) of the names of derived variables that need to be
    % calculated from the raw data variables.  The order of the derived fields
    % is important, since later variables may rely on earlier ones.  
    % Common derivations are:
    % serialdate: from date & time, or from datetime
    % depth: from pressure
    % turbidity: from turbidityVolts
    % chlorophyll: from chlorophyllVolts
    derivedVariableNames = {'serialdate', 'depth'};
    
    % A boolean property indicating whether or not figures should be created
    createFigures = true;
    
    % A boolean property indicating whether or not PacIOOS figures should
    % be created
    createPacIOOSFigures = true;
    
    % A boolean property indicating whether or not figures should be exported
    exportFigures = true;
    
    % A boolean property indicating whether or not to close the plot window
    % after export
    closePlotWindow = true;
    
    % A boolean property indicating whether or not data should be read in
    % from the archives (rather than the ring buffer)
    readArchive = false;
    
    % A string containing the path to the read_archive scripts
    read_archivePath = '/data/processed/read_archive/'
    
    % A string containing the name of the base archive directory for the sensor
    archiveDirectory = 'alawai'
    
    % A string containing URL info for LOBOViz data
    LOBOVizURL=['http://hawaii.loboviz.com/cgi-data/nph-data.cgi?' ...
                'node=29&data_format=text&x=date&y=cdom,fluorescence,' ...
                'nitrate,oxygen,oxygen_percent,salinity,temperature,turbidity'];
    
    % A string containing the path to the LOBOViz data directory on BBL       
    LOBOVizPath='/data/processed/LOBOViz/';
    
    % Astring indicating the type of figure to produce.  This is currently
    % limited to 'timeSeries' and 'temperatureSalinity'
    currentFigureType = '';
    
    % A cell array property that gives the details needed to produce multiple
    % time series figures, each potentially with subplots.  The cell fields are:
    % 1) figure title prefix (string)
    % 2) figure time duration (string in seconds)
    % 3) cell array of y axis subplot variables (as strings)
    % 4) cell array of x axis variables (as strings)
    % Since these settings are for time series plots, the x axis should only have
    % one variable (serialdate), which will be used for each subplot    
    %timeSeriesFigures = { ...
    %  {'3 Day' , '259200', {'temperature', 'salinity', 'depth'}, {'serialdate'}}, ...
    %  {'7 Day' , '604800', {'temperature', 'salinity', 'depth'}, {'serialdate'}}, ...
    %  {'21 Day', '604800', {'temperature', 'salinity', 'depth'}, {'serialdate'}}  ...
    %};
    timeSeriesFigures = { ...
      {'7 Day' , '604800', {'temperature', 'salinity', 'depth'}, {'serialdate'}} ...
    };
    

    % A cell array property containing information for PacIOOS plots
    % 1) figure title prefix (string)
    % 2) figure duration in days (string)
    % 3) output format for the figure (string)
    PacIOOSFigures = {{'7 day','7','.eps'}};
    
    % A cell array property containing configuration info for PacIOOS plots
    % 1) cell array of variables to plot (as strings)
    % 2) cell array of axis labels (as strings)
    % 3) cell array of axis locations (positions are 1-8, 1 being topmost-left
    %    8 being bottommost-right
    % 4) cell array of plot colors, each color represented as a 3x1 array
    % 5) cell array containing info for y-axis range and scaling.
    %    1st parameter determines dynamic vs fixed/semi dynamic axis.
    %    2nd parameter is the multiple used in dynamic scaling or the
    %    min/max values used for fixed or semi-dynamic axis
    %    3rd parameter is the number of ticks for the axis
    PacIOOSFigureProperties = {                                     ...
        {'adjustedDepth','temperature','salinity'}                , ...
        {'Actual WL (m)','Temperature (\circC)','Salinity (PSU)'} , ...
        {1 2 3}                                                   , ...
        {[0 0 0],[1 0 0],[0 0 0]}                                 , ...
        {{'fixed', [-0.4 1.2], 4},{'dynamic',  2, 4},{'fixed', [36 31 21 11 1 -4], 5}} ...
        };
    
    % A cell array property that gives the details needed to produce multiple
    % temperature-salinity plots
    tsFigures = {};
    
    % The format string used to parse the columns of data.  The order is critical
    % because it must match the order of the data in the ASCII data string
    dataVariableNames = {   ...
       'temperature'      , ...
       'conductivity'     , ...
       'pressure'         , ...
       'salinity'         , ...
       'date'             , ...
       'time'               ...
      };
                        
    % The format string used to parse the columns of data.  Each entry must
    % correspond with entries in the dataVariableNames property
    dataVariableUnits = {   ...
       '\circC'           , ...
       'S/m'              , ...
       'decibars'         , ...
       'PSU'              , ...
       'dd mmm yyyy'      , ...
       'HH:MM:SS'           ...
      };

    %The field delimiter for the ASCII text data string
    fieldDelimiter = ',';

    %The record delimiter for the ASCII text data string (end of line)
    recordDelimiter = '\n';
    
    %A boolean property that tells the parsing function whether to treat
    %multiple delimiters as one (may be necessary if whitespace delimiters
    %are used)
    ignoreMultipleDelims = false;
    
    % The number of header lines for the ASCII text data string (end of line)
    numberOfHeaderLines = '0';
    
    % The duration of the channel data query
    %duration = 604800; % 60 sec x 60 min x 24 hr x 7 days
    %duration = 2592000; % 60 sec x 60 min x 24 hr x 7 days
    duration = 31536000; % 60 sec x 60 min x 24 hr x 365 days
    
    % The duration in days of the channel data query
    duration_days = 0;
    
    % A boolean property that tells the processor whether to check for the
    % last month of plotted data to determine the data start date
    check_last = false;
    
    % The rate at which each sample is taken by the instrument (in seconds)
    instrumentSampleRate = 15;
    
    % The time reference of the channel data query
    reference = 'newest';
    
    % The difference in hours between sensor time and UTC
    sensorTimeOffset = 0;
    
    % The start time of the channel data query
    startTime = 0;
    
    % The start date of the channel data query
    dataStartDate = '05-01-2008 00:00:00';
    
    % The end date of the desired data set
    dataEndDate = ' ';
    
    % The timer interval for scheduled processing in minutes
    timerInterval = 20;
    
    % The depth of the instrument below average Mean Low Low Water level
    % (in meters)
    MLLWadjustment = 0;
   
    % The date and time a change was made in sensor depth relative to MLLW
    MLLWadjustmentDate={};
 
    % Cell array containing the output format used to export plots. 
    % Can export as .eps, .jpg, or in both formats.
    outputFormat={'.eps' '.jpg'};

  end % properties
  
  methods

    % The Constructor: creates an instance of the Configure object, and sets
    % the pertinent software library paths
    function self = Configure()
      % set the library paths based on the configuration properties
      set(self, 'rbnbLibraryPath', ...
          [self.rbnbPath ...
          'bin' ...
          self.pathSeparator ...
          'rbnb.jar']);
      set(self, 'rbnbMatlabPath', ...
          [self.rbnbPath ...
          'Matlab' ...
          self.pathSeparator]);
          
    end % Configure
  end % methods
  
end % classdef
