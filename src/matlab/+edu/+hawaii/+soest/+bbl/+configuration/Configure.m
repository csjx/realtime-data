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
% $HeadURL: $
% $LastChangedDate: $
% $LastChangedBy: $
% $LastChangedRevision: $
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
    
    % The path to the 'convert' program from the ImageMagick software.  This 
    % software is needed on the processing machine to produce PNG, JPG, and PDF
    % versions of the figures.
    convertPath = '/usr/local/bin/convert';
    
    % The path to the copy program from the ImageMagick software.  This 
    % software is needed on the processing machine to produce PNG, JPG, and PDF
    % versions of the figures.
    copyPath = '/bin/cp';

    % The path of the RBNB software
    rbnbPath = '/usr/local/RBNB/V3.1a/';
    
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
    
    % The string used to designate which variable name represents a turbidity
    % column in the data.  
    turbidityFieldName = 'turbidity';
    
    % The string used to designate which variable name represents a turbidity
    % voltage column in the data.  
    turbidityVoltageFieldName = 'turbidityVolts';
    
    % The string used to designate which variable name represents a dissolved
    % oxygen column in the data.  
    oxygenFieldName = 'oxygen';
    
    % The string used to designate which variable name represents a serial
    % date column in the data.  
    serialdateFieldName = 'serialdate';
    
    % The string used to designate which variable name represents an instrument
    % depth column in the data.  
    depthFieldName = 'depth';
    
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
    
    % A boolean property indicating whether or not figures should be exported
    exportFigures = true;
    
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
    
    
    tsFigures = { ...
      {'3 Day'  , '259200', {'temperature'}, {'salinity'}}, ...
      {'7 Day'  , '604800', {'temperature'}, {'salinity'}}, ...
      {'21 Day' , '604800', {'temperature'}, {'salinity'}}, ...
      {'365 Day', '604800', {'temperature'}, {'salinity'}}  ...
    };
    
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
    
    % The number of header lines for the ASCII text data string (end of line)
    numberOfHeaderLines = '0';
    
    % The duration of the channel data query
    %duration = 604800; % 60 sec x 60 min x 24 hr x 7 days
    %duration = 2592000; % 60 sec x 60 min x 24 hr x 7 days
    duration = 31536000; % 60 sec x 60 min x 24 hr x 365 days
    
    % The rate at which each sample is taken by the instrument (in seconds)
    instrumentSampleRate = 15;
    
    % The time reference of the channel data query
    reference = 'newest';
    
    % The start time channel data query
    startTime = 0;
    
    % The timer interval for scheduled processing in minutes
    timerInterval = 20;

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
