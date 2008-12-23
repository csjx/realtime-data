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

% The Configure class provides runtime configuration information for use by
% other classes that process near real-time data, such as the CTDProcessor class.
classdef ConfigureKN0101_010SBEX010R00
  properties
    % A boolean property that enables or disables debugging 
    debug = true;
    
    % The platform specific path separator 
    pathSeparator = '/';
    
    % The source code base directory
    baseDirectory = '/usr/local/bbl/trunk/'; % note trailing slash
    
    % The output path directory
    outputDirectory = '/var/www/html/OE/Data/CTD/KN0101_010SBEX010R00/'; % note trailing slash

    % The remote web server 
    remoteServer = 'bbl.ancl.hawaii.edu';
        
    % The path of the RBNB software
    rbnbPath = '/Applications/RBNB-3.1a/'; % note trailing slash
    
    % The path of the RBNB java library
    rbnbLibraryPath = '/usr/local/RBNB/V3.1B4a/bin/rbnb.jar';
    
    % The path of the RBNB java library
    rbnbMatlabPath = '/usr/local/RBNB/V3.1B4a/Matlab/'; % note trailing slash
    
    % The name of the RBNB Data Turbine server
    rbnbServer = 'bbl.ancl.hawaii.edu';
    
    % The port of the RBNB Data Turbine server
    rbnbPort = '3333';

    % The name of the sink client connecting to the RBNB Data Turbine server
    rbnbSinkName = 'KN0101_010SBEX010R00_MatlabProcessingSink';
    
    % The name of the desired data source
    rbnbSource = 'KN0101_010SBEX010R00';
    
    % The name of the desired data channel
    rbnbChannel = 'DecimalASCIISampleData';
    
    %The format string used to parse the columns of data
    dataFormatString = '%n %f %f %f %f %f %f %f %s %s';
    
    %The field delimiter for the ASCII text data string
    fieldDelimiter = ',';

    %The record delimiter for the ASCII text data string (end of line)
    recordDelimiter = '\n';
    
    %The number of header lines for the ASCII text data string (end of line)
    numberOfHeaderLines = '0';
    % The duration of the channel data query
    %duration = 604800; % 60 sec x 60 min x 24 hr x 7 days
    %duration = 2592000; % 60 sec x 60 min x 24 hr x 30 days
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
    function self = ConfigureKN0101_010SBEX010R00()
      % set the library paths based on the configuration properties 
    end % ConfigureKN0101_010SBEX010R00

  end % methods
  
end % classdef
