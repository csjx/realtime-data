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
classdef Configure
  properties
    % A boolean property that enables or disables debugging 
    debug = true;
    
    % The platform specific path separator 
    pathSeparator = '/';
    
    % The source code base directory
    baseDirectory = '/Users/cjones/development/bbl/trunk/'; % note trailing slash
    
    % The output path directory
    outputDirectory = '/Users/cjones/development/bbl/trunk/test/'; % note trailing slash
    
    % The path of the RBNB software
    rbnbPath = '/Applications/RBNB-3.1a/'; % note trailing slash
    
    % The path of the RBNB java library
    rbnbLibraryPath = '/Applications/RBNB-3.1a/bin/rbnb.jar';
    
    % The path of the RBNB java library
    rbnbMatlabPath = '/Applications/RBNB-3.1a/Matlab/'; % note trailing slash
    
    % The name of the RBNB Data Turbine server
    rbnbServer = '127.0.0.1';
    
    % The port of the RBNB Data Turbine server
    rbnbPort = '3333';

    % The name of the sink client connecting to the RBNB Data Turbine server
    rbnbSinkName = 'MatlabCTDProcessingSink';
    
    % The name of the desired data source
    rbnbSource = 'AW01XX_002CTDXXXXR00';
    
    % The name of the desired data channel
    rbnbChannel = 'DecimalASCIISampleData';
    
    % The duration of the channel data query
    duration = 604800; % 60 sec x 60 min x 24 hr x 7 days
    
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
    end % Configure

  end % methods
  
end % classdef
