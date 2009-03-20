% schedule_AW02XX_001CTDXXXXR00_processing 
%   This is a short runtime script that kicks off processing and
%   display of CTD data.  It depends on configuration information
%   found in the Configure class, and creates an instance of the
%   CTDProcessor class, and then runs the process() method via a
%   timer object so that the processing regularly recurs.

%  Copyright: 2007 Regents of the University of Hawaii and the
%             School of Ocean and Earth Science and Technology 
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

  % Create a new Configure instance and set the configuration details
  import edu.hawaii.soest.bbl.configuration.Configure;
  configuration = Configure;
  
  % set the processing-specific details for the instrument
  set( configuration, ...
  'baseDirectory'       , '/home/cjones/development/bbl/trunk/'                      , ...
  'outputDirectory'     , '/var/www/html/OE/KiloNalu/Data/CTD/AW02XX_001CTDXXXXR00/' , ...
  'convertPath'         , '/usr/bin/convert'                                   , ...
  'copyPath'            , '/bin/cp'                                                  , ...
  'rbnbPath'            , '/usr/local/RBNB/V3.1B4a/'                                 , ...
  'rbnbLibraryPath'     , '/usr/local/RBNB/V3.1B4a/bin/rbnb.jar'                     , ...
  'rbnbMatlabPath'      , '/usr/local/RBNB/V3.1B4a/Matlab/'                          , ...
  'rbnbServer'          , '192.168.103.50'                                           , ...
  'rbnbSinkName'        , 'MatlabAW02XX_001CTDXXXXR00ProcessingSink'                 , ...
  'rbnbSource'          , 'AW02XX_001CTDXXXXR00'                                     , ...
  'rbnbChannel'         , 'DecimalASCIISampleData'                                   , ...
  'dataFormatString'    , '# %f %f %f %f %f %s'                                      , ...
  'dataVariableNames'   , {'temperature'                                             , ...
                           'conductivity'                                            , ...
                           'chlorophyllVolts'                                        , ...
                           'turbidityVolts'                                          , ...
                           'salinity'                                                , ...
                           'datetime'                                                  ...
                          }                                                          , ...
  'dataVariableUnits'   , {'\circC'                                                  , ...
                           'S/m'                                                     , ...
                           'V'                                                       , ...
                           'V'                                                       , ...
                           'PSU'                                                     , ...
                           'dd mmm yyyy HH:MM:SS'                                      ...
                          }                                                          , ...
  'derivedVariableNames', {'serialdate'                                              , ...
                           'chlorophyll'                                             , ...
                           'turbidity'                                                 ...
                          }                                                          , ...
  'timeSeriesFigures'   , {                                                            ...
%                           {'3 Day'                                                 , ...
%                            '259200'                                                , ...
%                            {'temperature'                                          , ...
%                             'salinity'                                             , ...
%                             'chlorophyll'                                          , ...
%                             'turbidity'}                                           , ...
%                            {'serialdate'}                                            ...
%                           }                                                        , ...
                            {'7 Day'                                                 , ...
                             '604800'                                                , ...
                             {'temperature'                                          , ...
                              'salinity'                                             , ...
                              'chlorophyll'                                          , ...
                              'turbidity'}                                           , ...
                             {'serialdate'}                                            ...
                            }                                                        , ...
%                           {'21 Day'                                                , ...
%                            '1814400'                                               , ...
%                            {'temperature'                                          , ...
%                             'salinity'                                             , ...
%                             'chlorophyll'                                          , ...
%                             'turbidity'}                                           , ...
%                            {'serialdate'}                                            ...
%                           }                                                        , ...
%                           {'1 Year'                                                , ...
%                            '31536000'                                              , ...
%                            {'temperature'                                          , ...
%                             'salinity'                                             , ...
%                             'chlorophyll'                                          , ...
%                             'turbidity'}                                           , ...
%                            {'serialdate'}                                            ...
%                           }                                                          ...
                          }                                                          , ...
  'fieldDelimiter'      , ','                                                        , ...
  'numberOfHeaderLines' , '0'                                                        , ...
  'duration'            , 604800                                                     , ...
  'instrumentSampleRate', 15                                                         , ...
  'reference'           , 'newest'                                                   , ...
  'startTime'           , 0                                                          , ...
  'createFigures'       , true                                                       , ...
  'exportFigures'       , true                                                       , ...
  'timerInterval'       , 20                                                           ...
  );
  
  % Set up directory paths, and create a new CTDProcessor instance
  import edu.hawaii.soest.bbl.processing.CTDProcessor;
  javaaddpath(configuration.rbnbLibraryPath);
  addpath(configuration.rbnbMatlabPath);
  ctdProcessor = CTDProcessor(configuration);
  
  % schedule the processing
  
  % set the timer start time based on the timer interval.
  set(ctdProcessor, 'timerStartTime', ctdProcessor.configuration.timerInterval);
  
  % set the timer object instance
  set(ctdProcessor, 'timerObject',                     ...
    timer('TimerFcn',                                  ...
          'ctdProcessor.process',                      ...
          'period',                                    ...
          ctdProcessor.configuration.timerInterval*60, ...
          'executionmode',                             ...
          'fixeddelay'));
  startat(ctdProcessor.timerObject, ctdProcessor.timerStartTime);
