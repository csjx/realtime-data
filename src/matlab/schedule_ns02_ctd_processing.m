%  Copyright: 2007 Regents of the University of Hawaii and the
%             School of Ocean and Earth Science and Technology
% 
%    Purpose: This is a short runtime script that kicks off processing and
%             display of CTD data.  It depends on configuration information
%             found in the Configure class, and creates an instance of the
%             CTDProcessor class, and then runs the process() method via a
%             timer object so that the processing regularly recurs.
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

  % Get the configuration details
  import edu.hawaii.soest.bbl.configuration.ConfigureNS02;
  configuration = ConfigureNS02;
  
  % Set up directory paths, and create a new CTDProcessor instance
  import edu.hawaii.soest.bbl.processing.CTDProcessor;
  javaaddpath(configuration.rbnbLibraryPath);
  addpath(configuration.rbnbMatlabPath);
  ctdProcessor = CTDProcessor(configuration);
  
  % schedule the processing
  % set the timer start time based on the timer interval.  This syntax is odd,
  % but can be seen as ctdProcessor.setTimerStartTime(ctdProcessor.timerInterval)
  ctdProcessor.timerStartTime = ctdProcessor.timerInterval;
  % set the timer object instance
  ctdProcessor.timerObject = timer('TimerFcn', 'ctdProcessor.process', ...
                'period',ctdProcessor.timerInterval*60,'executionmode','fixeddelay');
  startat(ctdProcessor.timerObject, ctdProcessor.timerStartTime);
