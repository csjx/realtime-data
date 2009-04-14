% The SBE37Processor class processes SBE37 time series data for a given interval
% and produces plots of the data.  This class relies on the 
% edu.hawaii.soest.bbl.configuration.Configure class to get runtime 
% configuration information.
  
%  Copyright: 2007 Regents of the University of Hawaii and the
%             School of Ocean and Earth Science and Technology
% 
%    Purpose: To process SBE37 data in yearly increments and produce plots
%             of observations.
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
classdef SBE37Processor

  properties % of this class
  
    % The instance of the Configure class used to provide configuration
    % details for this SBE37Processor
    configuration;
    
    % The parsed ASCII string of SBE37 data as a cell array of observations
    sbe37DataCellArray;
    
    % The raw ASCII string of SBE37 data from the RBNB Data Turbine to be processed
    sbe37DataString;
    
    % The times associated with each frame of sbe37 data fetched from the RBNB
    sbe37DataTimes;
    
    % The name returned by the RBNB Data Turbine
    sbe37DataName;
    
    % The name of the RBNB Data Turbine server
    rbnbServer;
    
    % The port of the RBNB Data Turbine server
    rbnbPort;
    
    % The RBNB source of the time series to process
    source;
    
    % The RBNB channel of the time series to process
    channel;
    
    % The duration of the time series to process in seconds
    duration;
    
    % The RBNB reference datum of the time series to process (oldest, newest)
    reference;
    
    % The start time of the time series to process in seconds
    startTime;
    
    % The timer interval for scheduled processing in minutes
    timerInterval = 20;
    
    % The timer start time for scheduled processing
    timerStartTime;    
    
    % The timer object used to schedule processing
    timerObject;
    
    % The multiplication factor (in bytes) used in parse() to allow enough
    % memory for textscan() to buffer the entire data string from the call to
    % getRBNBData()
    bufferMemoryMultiplier = 500;
  end % properties
  
  methods % functions available from this class
    
    % The Constructor: creates an instance of the SBE37Processor class
    % @returns sbe37Processor - an instance of the SBE37Processor
    function self = SBE37Processor(configuration)
      % set the configuration information for this processing instance
      self.configuration  = configuration;
      self.rbnbServer     = self.configuration.rbnbServer;
      self.rbnbPort       = self.configuration.rbnbPort;
      self.source         = self.configuration.rbnbSource;
      self.channel        = self.configuration.rbnbChannel;
      self.reference      = self.configuration.reference;
      self.duration       = self.configuration.duration;
      self.startTime      = self.configuration.startTime;
      self.timerInterval  = self.configuration.timerInterval;
    end % SBE37Processor
    
    % A method used to parse the raw data string. This method assumes that
    % the raw sbe37 channel data are returned in a specific ASCII format like:
    % # 26.8112,  5.42666, 0.2871, 0.5848,  34.4952, 15 Aug 2008 12:10:06\r
    % where the column order is:
    % mmmmm,ttt.tttt,cc.ccccc, pppp.ppp, dddd.ddd, sss.ssss, vvvv.vvv, rrr.rrrr, mm-dd-yyyy, hh:mm:ss
    % m = MicroCAT serial Number
    % t = temperature in Celsius
    % c = conductivity S/m
    % p = pressure in decibars
    % d = depth in meters
    % s = salinity in PSU
    % v = sound velocity in meters/second
    % r = density sigma kg/m^3
    % EOL character is a carraige return (\n)
    % @returns void
    function sbe37DataCellArray = parse(self)
      
      % Parse the data string
      % Todo: fix this hardcoded Kilo Nalu SBE37 location configuration 
      % Example sample:
      % 05587, 25.3672, 5.35422,   11.154,   11.085,  35.0608, 1535.540,  23.3230, 10 Dec 2008, 09:57:28
      % Format:
      sbe37DataCellArray =                                        ...
        textscan(                                                 ...
          self.sbe37DataString,                                   ...
          self.configuration.dataFormatString,                    ...
          'BufSize', (ceil(self.bufferMemoryMultiplier *          ...
                      ceil(self.duration/                         ...
                      self.configuration.instrumentSampleRate))), ...
          'Delimiter',   self.configuration.fieldDelimiter,       ...
          'endOfLine',   self.configuration.recordDelimiter,      ...
          'headerLines', self.configuration.numberOfHeaderLines   ...
        );
      
    end %parse
    
    % A method used to process raw data string
    % @returns void
    function process(self)
      
      processTime = now();
      % get the most recent interval of data
      [self.sbe37DataString, ...
       self.sbe37DataTimes,  ...
       self.sbe37DataName] = self.getRBNBData();
      
      % parse the data string into a cell array used to create graphics
      self.sbe37DataCellArray = self.parse();
      
      % process the data and produce summary graphics    
      
      
      % produce the vector of timestamps based on the data observations
      timeStringArray = [char(self.sbe37DataCellArray{9}) ...
                         repmat(' ', length(self.sbe37DataCellArray{9}), 1) ...
                         char(self.sbe37DataCellArray{10})];

      temperatureArray = self.sbe37DataCellArray{2};  % temperature
      salinityArray = self.sbe37DataCellArray{6};     % salinity
      
      for i = 1:length(timeStringArray)
        try
          timeVector(i) = datenum(timeStringArray(i,1:20), 'dd mmm yyyy HH:MM:SS');
        catch
          timeVector(i) = NaN;
        end
      end

      currentYearIndices = find( timeVector < max(timeVector) - 7 );
      currentWeekIndices = intersect(find( timeVector > max(timeVector) - 7), ...
                                 find(timeVector < max(timeVector) - 1));
      currentDayIndices = find( timeVector > max(timeVector) - 1 );
      currentObservationIndex = find(timeVector == max(timeVector));

      currentYearPlot = ...
      plot(self.sbe37DataCellArray{6}(currentYearIndices), ...
           self.sbe37DataCellArray{2}(currentYearIndices), ...
           'LineStyle', 'none',                          ...
           'Marker', '.',                                ...
           'MarkerSize', 1,                              ...
           'MarkerFaceColor', [200/255 200/255 200/255], ...
           'MarkerEdgeColor', [200/255 200/255 200/255]  ...
          );    
      set(gca, 'NextPlot', 'add');
      currentWeekPlot = ...
      plot(self.sbe37DataCellArray{6}(currentWeekIndices), ...
           self.sbe37DataCellArray{2}(currentWeekIndices), ...
           'LineStyle', 'none',                          ...
           'Marker', '.',                                ...
           'MarkerSize', 1,                              ...
           'MarkerFaceColor', [000/255 000/255 255/255], ...
           'MarkerEdgeColor', [000/255 000/255 255/255]  ...
          );    
      set(gca, 'NextPlot', 'add');
      currentDayPlot = ...
      plot(self.sbe37DataCellArray{6}(currentDayIndices), ...
           self.sbe37DataCellArray{2}(currentDayIndices), ...
           'LineStyle', 'none',                        ...
           'Marker', '.',                              ...
           'MarkerSize', 1,                            ...
           'MarkerFaceColor', [000/255 255/255 000/255], ...
           'MarkerEdgeColor', [000/255 255/255 000/255]  ...
          );    
      set(gca, 'NextPlot', 'add');
      currentObservationPlot = ...
      plot(self.sbe37DataCellArray{6}(currentObservationIndex), ...
           self.sbe37DataCellArray{2}(currentObservationIndex), ...
           'LineStyle', 'none',             ...
           'LineWidth', 1.0,                ...
           'Marker', 'o',                   ...
           'MarkerSize', 12,                ...
           'MarkerFaceColor', 'none',       ...
           'MarkerEdgeColor', [255/255 0 0] ...
          );

      hTitle  = title (['Temperature/Salinity Plot: '...
                         'current observation period: '...
                         datestr(min(timeVector), 'ddmmmyyyy HH:MM:SS') ...
                         ' - ' ...
                         datestr(max(timeVector), 'ddmmmyyyy HH:MM:SS') ...
                         ' (HST)']);
      hXLabel = xlabel('Salinity (PSU)');
      hYLabel = ylabel('Temperature (\circC)');

      % Add axes styling
      set(gca, ...
        'Box'         , 'off'                                 , ...
        'TickDir'     , 'out'                                , ...
        'TickLength'  , [.01 .01]                            , ...
        'XMinorTick'  , 'on'                                 , ...
        'YMinorTick'  , 'on'                                 , ...
        'YTickLabel'  , num2str(get(gca, 'Ytick')', '%3.2f') , ...
        'XTickLabel'  , num2str(get(gca, 'Xtick')', '%3.2f') , ...
        'XGrid'       , 'on'                                 , ...
        'YGrid'       , 'on'                                 , ...
        'XColor'      , [.3 .3 .3]                           , ...
        'YColor'      , [.3 .3 .3]                           , ...
        'LineWidth'   , 1 );

      % Add the legend and increase legend point marker size
      legendHandle = ...
      legend('prior year observations',        ...
             'current week observations',        ...
             'current day observations',         ...
             'current observation', ...
             'Location', 'NorthWest');

      % Increase point marker size in the legend
      legendChildren = get(legendHandle, 'Children');
      for child = 1:length(legendChildren)
        try
          if ( ~isempty(strmatch(get(legendChildren(child), 'Marker'), '.')) )
            set(legendChildren(child), 'MarkerSize', 20);
          end
        catch
          continue;
        end
      end

      set(gcf,'renderer','painters'); %,'visible','off');

      % Export to Enhanced Postscript
      timestamp = datestr(processTime, 'yyyymmddHHMMSS');
      print(gcf,'-depsc2', ...
        [self.configuration.outputDirectory ...
         self.source  '_' timestamp '.10.1' '.eps']);

      % Convert to JPG
      eval(['!/usr/local/bin/convert -density 800x800 -geometry 800x800 ' ...
        self.configuration.outputDirectory ...
        self.source '_' timestamp '.10.1' '.eps ' ...
        self.configuration.outputDirectory ...
        self.source  '_' timestamp '.10.1' '.jpg']);

      % Copy to 'latest' JPG
      eval(['!/bin/cp -f '  ...
        self.configuration.outputDirectory ...
        self.source '_' timestamp '.10.1' '.jpg ' ...
        self.configuration.outputDirectory ...
        'latest.jpg']);

      % Convert to PDF
      eval(['!/usr/local/bin/convert ' ...
        self.configuration.outputDirectory ...
        self.source '_' timestamp '.10.1' '.eps ' ...
        self.configuration.outputDirectory ...
        self.source  '_' timestamp '.10.1' '.pdf']);

      % Copy to 'latest' PDF
      eval(['!/bin/cp -f '  ...
        self.configuration.outputDirectory ...
        self.source '_' timestamp '.10.1' '.pdf ' ...
        self.configuration.outputDirectory ...
        'latest.pdf']);

      

      if ( self.configuration.debug )
        disp(['Processing complete.  Next process time: ' ...
              datestr(processTime + self.timerInterval/60/24, 'mm-dd-yyyy HH:MM')]);
      end
    end %process
    
    % A method used to fetch the ASCII data string for the given RBNB
    % Data Turbine source, channel, reference, and given time duration
    % @todo - support the RBNB 'absolute' reference
    % @param source - the name of the RBNB SBE37 source instrument
    % @param channel -  the name of the RBNB SBE37 channel
    % @param reference - the reference datum for the time series (newest, oldest)
    % @param duration - the duration of the time series to process in seconds
    function [sbe37DataString, sbe37DataTimes, sbe37DataName] = getRBNBData(self)
      
      % set the pertinent properties of this SBE37Processor object
      
      % Create a new sink client to the DataTurbine
      matlabSink = rbnb_sink( ...
        [self.configuration.rbnbServer ':' ...
         self.configuration.rbnbPort], ...
         self.configuration.rbnbSinkName);
      
      % define the request details (get the latest 7 days of data)
      fullChannelName = [self.source '/' self.channel];
      
      % make the request to the DataTurbine and close the connection
      [sbe37DataString, ...
       sbe37DataTimes, ...
       sbe37DataName] = ...
        rbnb_get(matlabSink, fullChannelName, self.startTime, ...
        self.duration, self.reference);
      matlabSink.CloseRBNBConnection;
      clear matlabSink fullChannelName startTime;
      
      % write the data to disk  
      % fd = fopen([self.source '.10.1.dat'], 'w', 'l');
      % fwrite(fd, mostRecentData, 'int8');
      % fclose(fd);
      
    end % getRBNBData
    
    % --------------%
    % Getter methods 
    % --------------%
    
    % A getter method for the configuration property
    function value = get.configuration(self)
      value = self.configuration; 
    end
    
    % A getter method for the sbe37DataCellArray property
    function value = get.sbe37DataCellArray(self) 
      value = self.sbe37DataCellArray; 
    end

    % A getter method for the sbe37DataString property
    function value = get.sbe37DataString(self) 
      value = self.sbe37DataString; 
    end

    % A getter method for the sbe37DataTimes property
    function value = get.sbe37DataTimes(self) 
      value = self.sbe37DataTimes; 
    end

    % A getter method for the sbe37DataName property
    function value = get.sbe37DataName(self) 
      value = self.sbe37DataName; 
    end

    % A getter method for the source property
    function value = get.source(self) 
      value = self.source; 
    end

    % A getter method for the channel property
    function value = get.channel(self) 
      value = self.channel; 
    end

    % A getter method for the reference property
    function value = get.reference(self) 
      value = self.reference; 
    end

    % A getter method for the duration property
    function value = get.duration(self) 
      value = self.duration; 
    end

    % A getter method for the startTime property
    function value = get.startTime(self) 
      value = self.startTime; 
    end
    
    % A getter method for the timerInterval property
    function value = get.timerInterval(self) 
      value = self.timerInterval; 
    end

    % A getter method for the timerStartTime property
    function value = get.timerStartTime(self) 
      value = self.timerStartTime; 
    end
    
    % A getter method for the timerObject property
    function value = get.timerObject(self) 
      value = self.timerObject; 
    end
    % --------------%
    % Setter methods 
    % --------------%

    % A setter method for the configuration property
    function self = set.configuration(self, value)
      self.configuration = value;  
    end
    
    % A setter method for the sbe37DataCellArray property
    function self = set.sbe37DataCellArray(self, value) 
      self.sbe37DataCellArray = value;  
    end
    
    % A setter method for the sbe37DataString property
    function self = set.sbe37DataString(self, value) 
      self.sbe37DataString = char(value)';  
    end
    
    % A setter method for the sbe37DataTimes property
    function self = set.sbe37DataTimes(self, value) 
      self.sbe37DataTimes = value;  
    end
    
    % A setter method for the sbe37DataName property
    function self = set.sbe37DataName(self, value) 
      self.sbe37DataName = value;  
    end
    
    % A setter method for the source property
    function self = set.source(self, value) 
      self.source = value;  
    end
    
    % A setter method for the channel property
    function self = set.channel(self, value) 
      self.channel = value;  
    end
    
    % A setter method for the reference property
    function self = set.reference(self, value) 
      self.reference = value;  
    end
    
    % A setter method for the duration property
    function self = set.duration(self, value) 
      self.duration = value;  
    end
    
    % A setter method for the startTime property
    function self = set.startTime(self, value) 
      self.startTime = value;  
    end
    
    % A setter method for the timerInterval property
    function self = set.timerInterval(self, value) 
      self.timerInterval = value;  
    end
   
    % A setter method for the timerStartTime property
    function self = set.timerStartTime(self, interval)
      % calculate the next start time based on the timer interval 
      value = ceil(now*24*60/interval)/(24*60/interval) ...
              + interval/60/24;
      self.timerStartTime = value;  
    end
   
    % A setter method for the timerObject property
    function self = set.timerObject(self, value) 
      self.timerObject = value;  
    end
   
  end % methods
  
  events
  end % events
end % class definition
