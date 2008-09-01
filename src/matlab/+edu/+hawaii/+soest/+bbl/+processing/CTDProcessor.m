% The CTDProcessor class processes CTD time series data for a given interval
% and produces plots of the data.  This class relies on the 
% edu.hawaii.soest.bbl.configuration.Configure class to get runtime 
% configuration information.
  
%  Copyright: 2007 Regents of the University of Hawaii and the
%             School of Ocean and Earth Science and Technology
% 
%    Purpose: To process CTD data in seven day increments and produce plots
%             of observations.
%    Authors: Christopher Jones             
%    History: This code was adapted from code written by Eufemia Palomino at 
%             the University of California Santa Cruz for the Network for 
%             Environmental Observations of the Coastal Ocean (NEOCO) project
%             in 2003.
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
classdef CTDProcessor

  properties % of this class
  
    % The instance of the Configure class used to provide configuration
    % details for this CTDProcessor
    configuration;
    
    % The parsed ASCII string of CTD data as a cell array of observations
    ctdDataCellArray;
    
    % The raw ASCII string of CTD data from the RBNB Data Turbine to be processed
    ctdDataString;
    
    % The times associated with each frame of ctd data fetched from the RBNB
    ctdDataTimes;
    
    % The name returned by the RBNB Data Turbine
    ctdDataName;
    
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
  end % properties
  
  methods % functions available from this class
    
    % The Constructor: creates an instance of the CTDProcessor class
    % @returns ctdProcessor - an instance of the CTDProcessor
    function self = CTDProcessor(configuration)
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
    end % CTDProcessor
    
    % A method used to parse the raw data string. This method assumes that
    % the raw ctd channel data are returned in a specific ASCII format like:
    % # 26.8112,  5.42666, 0.2871, 0.5848,  34.4952, 15 Aug 2008 12:10:06\r
    % where the column order is:
    % 1) # (pound delimiter)
    % 2) temperature
    % 3) conductivity
    % 4) turbidity
    % 5) Chlorophyl-a
    % 6) salinity
    % 7) dd mmm yyyy HH:MM:SS (reported date-time stamp)
    % EOL character is a carraige return (\r\n)
    % @returns void
    function ctdDataCellArray = parse(self)
      
      % Parse the data string
      % Todo: fix this hardcoded Ala Wai CTD location configuration 
      ctdDataCellArray = textscan( ...
                           self.ctdDataString, ...
                           '# %f %f %f %f %f %s', ...
                           'BufSize', (100 * self.duration), ...
                           'Delimiter', ',', ...
                           'endOfLine', '\r\n', ...
                           'headerLines', '0');
      
    end %parse
    
    % A method used to process raw data string
    % @returns void
    function process(self)
      
      processTime = now();
      % get the most recent interval of data
      [self.ctdDataString, ...
       self.ctdDataTimes,  ...
       self.ctdDataName] = self.getRBNBData();
      
      % parse the data string into a cell array used to create graphics
      self.ctdDataCellArray = self.parse();
      
      % process the data and produce summary graphics    
      mtime   = datenum(self.ctdDataCellArray{6}, 'dd mmm yyyy HH:MM:SS');

      loc_id  = ones(length(self.ctdDataCellArray{1}), 1);
      project.data{1} = self.ctdDataCellArray{1};  % temperature
      project.data{2} = self.ctdDataCellArray{5};  % salinity
      project.data{3} = self.ctdDataCellArray{3};  % turbidity
      project.data{4} = self.ctdDataCellArray{4};  % chlorophyll

      i=[]; loc = [];
      loc.idn = [1 2 3 4 5 6 7];
      for i = loc.idn(1):loc.idn(end),
          loc.idx{i} = find(loc_id==i);
          if ~isempty(loc.idx{i}),
              nloc = length(loc.idn(i));
              if nloc > 1,
                  loc.ls{i} = num2str(loc.idn(i)); % can only take up to 99 stations
              elseif nloc == 1,
                  loc.ls{i} = ['0' num2str(loc.idn(i))];
              end
          end
      end

      % Site specific ranges
                                    % ALA WAI CANAL NS01 (AW01XX_002CTDXXXXR00)
      loc.yLimit{1} = { ...
        [20 45];...           % temperature
        [31.0 36.0]; ...      % salinity
        [0 2.0]; ...          % turbidity
        [0 10];...            % chlorophyll
      };

      %============
      % Plotting
      %============
      numberOfFigures = length(project.data);
      figureRectangle = [1950 800 800 800];
      paperPosition = [0 0 8.5 11.0];
      yLabelPosition = [-0.1 0.5042 0];
      project.graphicName = [
        {'Temperature (\circC)'},...
        {'Salinity (PSU)'},...
        {'Turbidity (NTU)'},...
        {'Chlorophyll Units (\mug/L)'},...
      ];
      % See http://geography.uoregon.edu/datagraphics/color_scales.htm,
      % Stepped Sequential scheme with numbers from
      % http://geography.uoregon.edu/datagraphics/color/StepSeq_25.txt
      % rows 1, 6, 11, 16
      project.graphicColor = [ ...
        % {[0.600 0.060 0.060]}, ...     % red   (muted)
        % {[0.060 0.420 0.600]}, ...     % blue  (muted)
        % {[0.600 0.330 0.060]}, ...     % brown (muted)
        % {[0.420 0.600 0.060]}, ...     % green (muted)
        {[255/255 0       0      ]}, ... % red   (bright)
        {[0       0       255/255]}, ... % blue  (bright)
        {[102/255 047/255 0      ]}, ... % brown (bright)
        {[0       255/255 0      ]}, ... % green (bright)
      ];
      project.graphicMarker = [{'.'},{'.'},{'.'},{'.'}];
      project.graphicMarkersize = [{3.0},{3.0},{3.0},{3.0}];
      project.graphicTickFormat = [{'%3.2f'},{'%3.3f'},{'%3.2f'},{'%3.2f'}];
      project.graphicYLimit = [ ...
        {[20 45]}, ...
        {[31.0 36.0]}, ...
        {[0 2.0]}, ...
        {[0 10]}, ...
      ];

      jbuf = [];
      for k = 1:length(loc.idn)
          if ~isempty(loc.idx{k})
              jbuf = [jbuf loc.idn(k)];
          end
      end;
      j=[];
      for j = jbuf,
          i=[];
          close all
          fh = figure(); clf;
          for i = 1:numberOfFigures,
              x=[]; y=[];
              
              % Position and Size of Figure
              set(gcf,'units','pixels','position',figureRectangle);
              set(gcf,'paperposition',paperPosition);
              
              % Plotting
              x=mtime(loc.idx{j}); y=project.data{i}(loc.idx{j}); 
              subplot(numberOfFigures, 1, i);
              plot(x,y,...
                  'color',project.graphicColor{i},...
                  'linestyle','none',...
                  'marker',project.graphicMarker{i},...
                  'markersize',project.graphicMarkersize{i});
              if i == 1
                titleText = ['Water Quality: latest observation period: ' ...
                            datestr(processTime - self.timerInterval/60/24, ...
                            'mm-dd-yyyy HH:MM') ' to ' ...
                            datestr(processTime,'HH:MM')];
                title(titleText);
              end
              grid on; axhan = gca; 
              
              % Limits
              xlim([(max(mtime) - self.duration/60/60/24) max(mtime)]);
              graphicYLimit = project.graphicYLimit{i};
              minYObservation=[]; maxYObservation=[];
              minYObservation=min(y); maxYObservation=max(y);
              
              if isempty(minYObservation)           | ...
                 isnan(minYObservation)             | ...
                 minYObservation == maxYObservation | ...
                 (maxYObservation - minYObservation) < 2 ,
                  myLimit = ylim;
                  minYObservation=myLimit(1); maxYObservation=myLimit(2);
              
              elseif ~isempty(graphicYLimit),
                  if minYObservation < graphicYLimit(1), 
                      y(find(y < graphicYLimit(1))) = nan;
                      minYObservation = min(y);
                  end
                  if maxYObservation > graphicYLimit(2) & maxYObservation > minYObservation,
                      y(find(y > graphicYLimit(2))) = nan;
                      maxYObservation = max(y);
                  end
                  if ~isempty(loc.yLimit{j})
                      if ~isempty(loc.yLimit{j}{i}),
                          if minYObservation<loc.yLimit{j}{i}(1),
                              minYObservation=loc.yLimit{j}{i}(1);
                          end
                          if maxYObservation>loc.yLimit{j}{i}(2),
                              maxYObservation=loc.yLimit{j}{i}(2);
                          end
                      end
                  end
              
              else,
                  minYObservation=[-.5]; maxYObservation=[1.5];
              
              end;
              ylim([minYObservation maxYObservation]);
              % Ticks and Ticklabels
              %xtick = get(axhan,'XTick');
              xtick = floor(now-6:now);
              xticklabel = datestr(xtick,'ddmmmyy');
              ytick = get(axhan,'YTick');
              yticklabel = num2str(ytick',project.graphicTickFormat{i});
              set(axhan,'XTick',xtick,'YTick',ytick);
              set(axhan,'XTickLabel',xticklabel,'YTickLabel',yticklabel);
              
                %Add Fahrenheit axis to temperature plot
                if i==1,
                    set(axhan,'Box','off');
                    yLimit2 = (ylim*9/5)+32;
                    ax2 = axes('Position',get(axhan,'Position'),'Color','none','YaxisLocation','right');
                    set(ax2,'YLim',yLimit2);
                    ytick2=get(ax2,'ytick');
                    yticklabel2 = num2str(ytick2');
                    set(ax2,'XTick',nan,'YTick',ytick2);
                    set(ax2,'XTickLabel',nan,'YTickLabel',yticklabel2);
                    set(ax2,'XAxisLocation','top');
                end;
              % Labeling, Positions and Sizes of Objects
              % setfontsize(09); setfontweight('bold');
              axes(axhan);
              hylab = ylabel(project.graphicName{i},'fontsize',10,'fontweight','normal'); 
              set(hylab,'units','normalized','position',yLabelPosition);
                 % Label Fahrenheit axis on temperature plot
                 if i==1,
                     axes(ax2); 
                     hylab2 = ylabel('Temperature (\circF)','fontsize',10,'fontweight','normal'); 
      %               set(hylab2,'units','pixels','position',[700 100 0]);
                 end;
              set(fh,'renderer','painters'); %,'visible','off');
          end
          
          % Export to Enhanced Postscript
          timestamp = datestr(now(), 'yyyymmddHHMMSS');
          print(fh,'-depsc2', ...
            [self.configuration.outputDirectory ...
             self.source  '_' timestamp '.10.1' '.eps']);
          
          % Convert to PNG
          eval(['!/opt/local/bin/convert -colors 65536 -density 800x800 -geometry 800x800 ' ...
            self.configuration.outputDirectory ...
            self.source '_' timestamp '.10.1' '.eps ' ...
            self.configuration.outputDirectory ...
            self.source  '_' datestr(now(), 'yyyymmddHHMMSS') '.10.1' '.png']);
          
          % Convert to PDF
          eval(['!/opt/local/bin/convert -colors 65536 -density 800x800 -geometry 800x800 ' ...
            self.configuration.outputDirectory ...
            self.source '_' timestamp '.10.1' '.eps ' ...
            self.configuration.outputDirectory ...
            self.source  '_' timestamp '.10.1' '.pdf']);
          
      end
      if ( self.configuration.debug )
        disp(['Processing complete.  Next process time: ' ...
              datestr(processTime + self.timerInterval/60/24, 'mm-dd-yyyy HH:MM')]);
      end
    end %process
    
    % A method used to fetch the ASCII data string for the given RBNB
    % Data Turbine source, channel, reference, and given time duration
    % @todo - support the RBNB 'absolute' reference
    % @param source - the name of the RBNB CTD source instrument
    % @param channel -  the name of the RBNB CTD channel
    % @param reference - the reference datum for the time series (newest, oldest)
    % @param duration - the duration of the time series to process in seconds
    function [ctdDataString, ctdDataTimes, ctdDataName] = getRBNBData(self)
      
      % set the pertinent properties of this CTDProcessor object
      
      % Create a new sink client to the DataTurbine
      matlabSink = rbnb_sink( ...
        [self.configuration.rbnbServer ':' ...
         self.configuration.rbnbPort], ...
         self.configuration.rbnbSinkName);
      
      % define the request details (get the latest 7 days of data)
      fullChannelName = [self.source '/' self.channel];
      
      % make the request to the DataTurbine and close the connection
      [ctdDataString, ...
       ctdDataTimes, ...
       ctdDataName] = ...
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
    
    % A getter method for the ctdDataCellArray property
    function value = get.ctdDataCellArray(self) 
      value = self.ctdDataCellArray; 
    end

    % A getter method for the ctdDataString property
    function value = get.ctdDataString(self) 
      value = self.ctdDataString; 
    end

    % A getter method for the ctdDataTimes property
    function value = get.ctdDataTimes(self) 
      value = self.ctdDataTimes; 
    end

    % A getter method for the ctdDataName property
    function value = get.ctdDataName(self) 
      value = self.ctdDataName; 
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
    
    % A setter method for the ctdDataCellArray property
    function self = set.ctdDataCellArray(self, value) 
      self.ctdDataCellArray = value;  
    end
    
    % A setter method for the ctdDataString property
    function self = set.ctdDataString(self, value) 
      self.ctdDataString = char(value)';  
    end
    
    % A setter method for the ctdDataTimes property
    function self = set.ctdDataTimes(self, value) 
      self.ctdDataTimes = value;  
    end
    
    % A setter method for the ctdDataName property
    function self = set.ctdDataName(self, value) 
      self.ctdDataName = value;  
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