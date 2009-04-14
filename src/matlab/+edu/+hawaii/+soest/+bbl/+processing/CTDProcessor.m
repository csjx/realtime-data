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
classdef CTDProcessor < hgsetget & dynamicprops

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
    
    % The start time of the timer object
    timerStartTime;
    
    % The timer interval for scheduled processing in minutes
    timerInterval = 20;
    
    % The timer object used to schedule processing
    timerObject;
    
    % The time of the current processing
    processTime;
  end % properties
  
  methods % functions available from this class
    
    % The Constructor: creates an instance of the CTDProcessor class
    % @returns ctdProcessor - an instance of the CTDProcessor
    function self = CTDProcessor(configuration)
      % set the configuration information for this processing instance
      self.configuration  = configuration;
    end % CTDProcessor
    
    % A method used to parse the raw data string. This method assumes that
    % the CTDProcessor.configuration.dataFormatString is set to provide data
    % typing information, CTDprocessor.configuration.duration is set to 
    % provide relative size information, CTDprocessor.configuration.fieldDelimiter
    % is set to provide the field delimiter character, and that 
    % CTDprocessor.configuration.numberOfHeaderLines is set to provide header 
    % information for the raw data string.
    % @returns void
    function ctdDataCellArray = parse(self)
      
      if ( self.configuration.debug )
        disp('CTDProcessor.parse() called.');
      end
      
      % Parse the data string
      % Todo: fix this hardcoded Ala Wai CTD location configuration
      if ( ~isempty(self.ctdDataString) ) 
      ctdDataCellArray = textscan( ...
                           self.ctdDataString, ...
                           self.configuration.dataFormatString, ...
                           'BufSize', (10 * self.configuration.duration), ...
                           'Delimiter', self.configuration.fieldDelimiter, ...
                           'headerLines', self.configuration.numberOfHeaderLines);
      end
      
    end %parse
    
    % A method used to process the raw data string, build derived variables,
    % create figures for the designated variables, and export the figures
    % to the designated formats.
    % @returns void
    function process(self)
      
      if ( self.configuration.debug )
        disp('CTDProcessor.process() called.');
      end
      
      set(self, 'processTime', now());
      % get the most recent interval of data
      [self.ctdDataString, ...
       self.ctdDataTimes,  ...
       self.ctdDataName] = self.getRBNBData();
      
      % parse the data string into a cell array used to create graphics
      self.ctdDataCellArray = self.parse();
      
      % Create derived variables.  For each variable name in the list of derived
      % variables, calculate the derived variable and append it to the
      % ctdDataCellArray for later use.  Also update the dataVariableNames and
      % dataVariableUnits properties.            
      try
        for derivedVariableNumber = 1:length(self.configuration.derivedVariableNames)
          self.ctdDataCellArray{length(self.ctdDataCellArray) + 1} = ...
            createDerivedVariable(self, ...
              self.configuration.derivedVariableNames{ ...
                derivedVariableNumber ...
              } ...
            );
        end
      catch derivedVariableException
        disp(derivedVariableException.message);
        return;
      end
      
      % create figures as outlined in the configuration properties
      if ( self.configuration.createFigures )
         for figureNumber = 1:length(self.configuration.timeSeriesFigures)
           try
             % Get the figure title prefix string
             figureTitlePrefix = ...
             char( ...
               self.configuration.timeSeriesFigures{figureNumber}(1) ...
             );
             
             % Get the figure duration in seconds
             figureDuration = ...
             str2num( ...
               char( ...
                 self.configuration.timeSeriesFigures{figureNumber}(2) ...
               ) ...
             );
             
             % Get the figure Y axis cell array
             figureYAxisVariables = ...
               self.configuration.timeSeriesFigures{figureNumber}(3);
             
               % Get the figure X axis cell array
               figureXAxisVariables = ...
                 self.configuration.timeSeriesFigures{figureNumber}(4);
             
             % now create the figure
             timesSeriesHandle = ...
               self.createTimesSeriesFigure( ...
                 figureTitlePrefix   , ...
                 figureDuration      , ...
                 figureYAxisVariables, ...
                 figureXAxisVariables  ...
               );
             % TSHandle = self.createTSFigure();    
             
             % export figures as outlined in the configuration properties
             if ( self.configuration.exportFigures )
               % call the export method
               outputFormat = '';
               outputParameters = '';
               self.export(timesSeriesHandle, outputFormat, outputParameters);
               % self.export(TSHandle);
             end
             
             % clean up variables
             clear figureTitlePrefix    ...
                   figureDuration       ...
                   figureYAxisVariables ...
                   figureXAxisVariables ...
                   timesSeriesHandle;
             
           catch figureException
             disp(figureException.message);
           end % end try statement
         end % end for loop
      end % end if statement
      
      if ( self.configuration.debug )
        disp(['Processing complete.  Next process time: ' ...
              datestr(self.processTime + self.configuration.timerInterval/60/24, ...
                      'mm-dd-yyyy HH:MM')]);
      end
      
    end %process
    
    % A method used to produce derived variables from the raw data.  This 
    % function can currently handle the following derivations:
    % serialdate: from date & time, or from datetime
    % depth: from pressure
    % turbidity: from turbidityVolts
    % chlorophyll: from chlorophyllVolts
    % The variable returned has the same length as the ctdDataCellArray so it
    % can be appended to that cell array for later use.
    % @returns value - the derived variable array
    function value = createDerivedVariable(self, derivedVariableName)
      
      if ( self.configuration.debug )
        disp('CTDProcessor.createDerivedVariable() called.');
      end
      
      % derive each of the named variables
      switch derivedVariableName
        
        % derive the serial date from either the date & time, or datetime
        case self.configuration.serialdateFieldName
          if ( ~isempty(self.ctdDataCellArray) ) 
            % Extract timestamp information out of the data cell array, and append a
            % serial date vector to the end of the data cell array for use in plotting.

            % Find which data columns represents the date & time columns 
            % or just the datetime column, and produce a vector of serial dates.  
            % Test that the variable units are also present in order to interpret 
            % the date, time, or datetimes.
            if ( ~isempty(find(strcmp(self.configuration.dateFieldName, ...
                                      self.configuration.dataVariableNames))) && ...
                 ~isempty(find(strcmp(self.configuration.timeFieldName, ...
                                      self.configuration.dataVariableNames))) && ...
                 length(self.configuration.dataVariableNames) == ...
                 length(self.configuration.dataVariableUnits) )    

              % use datenum(X, format), and paste the vectors together:
              % ([datevector spacevector timevector])
              % the find(strcmp()) is used to find the indices of the date and time
              % columns, using the configuration fields that designate which column
              % represents the date, time, or datetime columns
              value = datenum( ...
                        [char(self.ctdDataCellArray{ ...
                           find( ...
                             strcmp( ...
                               self.configuration.dateFieldName, ...
                               self.configuration.dataVariableNames ...
                             ) ...
                           ) ...
                         }) ...
                         repmat(' ', ...
                           length( ...
                             self.ctdDataCellArray{ ...
                               find( ...
                                 strcmp( ...
                                   self.configuration.dateFieldName, ...
                                   self.configuration.dataVariableNames ...
                                 ) ...
                               ) ...
                             } ...
                           ), 1 ...
                         ) ...
                         char(self.ctdDataCellArray{ ...
                           find( ...
                             strcmp( ...
                               self.configuration.timeFieldName, ...
                               self.configuration.dataVariableNames ...
                             ) ...
                           ) ...
                         }) ...
                        ], ...
                        [self.configuration.dataVariableUnits{ ...
                          find(strcmp( ...
                            self.configuration.dateFieldName, ...
                            self.configuration.dataVariableNames) ...
                          ) ...
                         } ...
                         ' ' ...
                         self.configuration.dataVariableUnits{ ...
                           find( ...
                             strcmp( ...
                               self.configuration.timeFieldName, ...
                               self.configuration.dataVariableNames) ...
                           ) ...
                         } ...
                        ] ...
                      );
                      
              % update the field names and units arrays
              updateDataVariableNames(self, self.configuration.serialdateFieldName);
              updateDataVariableUnits(self, 'days');
                      
            % the dates and times are in one column, datetime
            elseif ( ~isempty(find(strcmp(self.configuration.datetimeFieldName, ...
                                          self.configuration.dataVariableNames)))  && ...
                                  length(self.configuration.dataVariableNames) == ...
                                  length(self.configuration.dataVariableUnits) ) 

              % use datenum(X, format)
              value = datenum( ...
                        char(self.ctdDataCellArray{ ...
                           find( ...
                             strcmp( ...
                               self.configuration.datetimeFieldName, ...
                               self.configuration.dataVariableNames ...
                             ) ...
                           ) ...
                         }), ...
                        self.configuration.dataVariableUnits{ ...
                          find(strcmp( ...
                            self.configuration.datetimeFieldName, ...
                            self.configuration.dataVariableNames) ...
                          ) ...
                         } ...
                      );
              % update the field names and units arrays
              updateDataVariableNames(self, self.configuration.serialdateFieldName);
              updateDataVariableUnits(self, 'days');
            else
              value = [];
              error(['There are no date, time, or datetime fields designated ' ...
                     'in the dataVariableNames cell array.  Failed to build ' ...
                     'serial date vector. ' ...
                     'Set the dataVariableNames property and the dataVariableUnits ' ...
                     'property to include "date" and "time" names or a "datetime" name ' ...
                     'and their respective format strings.']);
            end % end if statement
          else
            % we have no data yet, return an empty array
            value = [];
          end % end if statement (~isempty(self.ctdDataCellArray))
        
        % derive the depth from pressure  
        case self.configuration.depthFieldName
          if ( ~isempty(self.ctdDataCellArray) ) 
            
            % 3. calculate the gravity variation with latitude and pressure:
            % 21.16 = latitude of Honolulu
            pressure = self.ctdDataCellArray{ ...
              find( ...
                strcmp( ...
                  self.configuration.pressureFieldName, ...
                  self.configuration.dataVariableNames ...
                ) ...
              ) ...
            };
            
            x  = [sin(21.16/57.29578)]^2;
            g1 = 9.780318 .* ...
                 [1 + (5.2788*10^-3 + 2.36*10^-5 .* x) .* x] ...
                 + 1.092*10^-6 .* pressure;

            % 4. calculate depth using g
            depth = [...
              ( ...
                ( ...
                  (-1.82*10^-15 .* pressure + 2.279*10^-10) .* ...
                  pressure - 2.2512*10^-5 ...
                ) .* pressure + 9.72659 ...
              ) .* pressure ...
            ] ./ g1;
            
            value = depth;
            
            clear x g1 pressure depth;            
            % update the field names and units arrays
            updateDataVariableNames(self, self.configuration.depthFieldName);
            updateDataVariableUnits(self, 'm');

            
          else
            value = [];
          end % end if statement (~isempty(self.ctdDataCellArray))
        
        % derive turbidity from turbidity volts
        case self.configuration.turbidityFieldName
          if ( ~isempty(self.ctdDataCellArray) ) 
            if ( ~isempty(find(strcmp(self.configuration.turbidityVoltageFieldName, ...
                                      self.configuration.dataVariableNames))) && ...
                 length(self.configuration.dataVariableNames) == ...
                 length(self.configuration.dataVariableUnits) )    
              
              % constants from the WetLabs FLNTU Characterization Sheet Nov 27, 2007
              darkCounts    = 0.065; % V (volts)
              scaleFactor   = 5;     % NTU/V
              maximumOutput = 4.97;  % V
              resolution    = 0.6;   % mV
              
              % get a reference to the turbidity volts array
              turbidityVoltsArray = ...
                self.ctdDataCellArray{ ...
                  find( ...
                    strcmp( ...
                      self.configuration.turbidityVoltageFieldName, ...
                      self.configuration.dataVariableNames ...
                    ) ...
                  ) ...
                };
              
              % build an array of ones for set-based multiplication
              onesArray     = ...
                ones(length(self.ctdDataCellArray{ ...
                              find( ...
                                strcmp( ...
                                  self.configuration.turbidityVoltageFieldName, ...
                                  self.configuration.dataVariableNames ...
                                ) ...
                              ) ...
                            } ...
                     ), 1 ...
                );
              
              darkCountsArray = onesArray .* darkCounts;              
              scaleFactorArray = onesArray .* scaleFactor;
              
              % NTU = Scale Factor x (Output - Dark Counts)
              value = scaleFactorArray .* (turbidityVoltsArray - darkCountsArray);
              
              clear darkCounts scaleFactor maximumOutput resolution ...
                    darkCountsArray scaleFactorArray onesArray;
              
              % update the field names and units arrays
              updateDataVariableNames(self, self.configuration.turbidityFieldName);
              updateDataVariableUnits(self, 'NTU');

            else
              error(['There is no turbidityVolts field designated ' ...
                     'in the dataVariableNames cell array.  Failed to build ' ...
                     'turbidity vector. ' ...
                     'Set the dataVariableNames property and the dataVariableUnits ' ...
                     'property to include "turbidityVolts" name ' ...
                     'and their respective format strings.']);              
            end
            
          else
            value = [];
          end % end if statement (~isempty(self.ctdDataCellArray))
        
        % derive chlorophyll from chlorophyll volts
        case self.configuration.chlorophyllFieldName
          if ( ~isempty(self.ctdDataCellArray) ) 
            if ( ~isempty(find(strcmp(self.configuration.chlorophyllVoltageFieldName, ...
                                      self.configuration.dataVariableNames))) && ...
                 length(self.configuration.dataVariableNames) == ...
                 length(self.configuration.dataVariableUnits) )    
              
              % constants from the WetLabs FLNTU Characterization Sheet Nov 27, 2007
              darkCounts    = 0.009; % V (volts)
              scaleFactor   = 10; % µg/l/V
              maximumOutput = 4.97; % V
              resolution    = 0.6;  % mV
              
              % get a reference to the chlorophyll volts array
              chlorophyllVoltsArray = ...
                self.ctdDataCellArray{ ...
                  find( ...
                    strcmp( ...
                      self.configuration.chlorophyllVoltageFieldName, ...
                      self.configuration.dataVariableNames ...
                    ) ...
                  ) ...
                };
              
              % build an array of ones for set-based multiplication
              onesArray     = ...
                ones(length(self.ctdDataCellArray{ ...
                              find( ...
                                strcmp( ...
                                  self.configuration.chlorophyllVoltageFieldName, ...
                                  self.configuration.dataVariableNames ...
                                ) ...
                              ) ...
                            } ...
                     ), 1 ...
                );
              
              darkCountsArray = onesArray .* darkCounts;              
              scaleFactorArray = onesArray .* scaleFactor;
              
              % CHL (µg/l) = Scale Factor x (Output - Dark Counts)
              value = scaleFactorArray .* (chlorophyllVoltsArray - darkCountsArray);
              
              clear darkCounts scaleFactor maximumOutput resolution ...
                    darkCountsArray scaleFactorArray onesArray;
             
              % update the field names and units arrays
              updateDataVariableNames(self, self.configuration.chlorophyllFieldName);
              updateDataVariableUnits(self, '\mug/l');
            
            else
              error(['There is no chlorophyllVolts field designated ' ...
                     'in the dataVariableNames cell array.  Failed to build ' ...
                     'chlorophyll vector. ' ...
                     'Set the dataVariableNames property and the dataVariableUnits ' ...
                     'property to include "chlorophyllVolts" name ' ...
                     'and their respective format strings.']);              
            end
          else
            value = [];
          end % end if statement (~isempty(self.ctdDataCellArray))          
      
      end % end switch
      
    end % end createDerivedVariable function
    
    % A method used to create a Temperature/Salinity figure.
    % @returns value - the handle to the figure object that is created  
    function value = createTSFigure(self                , ...
                                    figureTitlePrefix   , ...
                                    figureDuration      , ...
                                    figureYAxisVariables, ...
                                    figureXAxisVariables  ...
    )
      if ( self.configuration.debug )
        disp('CTDProcessor.createTSFigure() called.');
      end
      
    end
    
    % A method used to create a times series figure with subplots.
    % @returns value - the handle to the figure object that is created  
    function value = createTimesSeriesFigure(self, ...
                                             figureTitlePrefix   , ...
                                             figureDuration      , ...
                                             figureYAxisVariables, ...
                                             figureXAxisVariables  ...
    )
      if ( self.configuration.debug )
        disp('CTDProcessor.createTimesSeriesFigure() called.');
      end
      %%%% loc_id  = ones(length(self.ctdDataCellArray{1}), 1);
      %%%% project.data{1} = self.ctdDataCellArray{1};  % temperature
      %%%% project.data{2} = self.ctdDataCellArray{5};  % salinity
      %%%% project.data{3} = self.ctdDataCellArray{3};  % chlorophyll
      %%%% project.data{4} = self.ctdDataCellArray{4};  % turbidity

      %============
      % Plotting
      %============
      close(gcf);
      numberOfFigures = length(figureYAxisVariables{1});
      figureRectangle = [1950 800 800 800];
      paperPosition = [0 0 8.5 11.0];
      yLabelPosition = [-0.1 0.5042 0];
      
      % build a structure of the Y axis subplot labels
      for figureNumber = 1:numberOfFigures
        
        % get the variable label based on the Y axis variable list passed to 
        % this function
        variableLabel = char(figureYAxisVariables{1}(figureNumber));
        % lookup up the units based on the variable name
        unitLabel = ...
          self.configuration.dataVariableUnits{ ...
            find( ...
              strcmp( ...
                variableLabel, self.configuration.dataVariableNames ...
              ) ...
            ) ...
          };
        
        % build the subplot's Y label string
        figureYAxisLabel = [variableLabel ' ' '(' unitLabel ')'];
        
        % add the Y label to the structure
        project.figureYAxisVariables(figureNumber) = {figureYAxisLabel};
        
      end % end for loop
      
      
      % See http://geography.uoregon.edu/datagraphics/color_scales.htm,
      % Stepped Sequential scheme with numbers from
      % http://geography.uoregon.edu/datagraphics/color/StepSeq_25.txt
      % rows 1, 6, 11, 16
      project.graphicColor = [ ...
        {[255/255 0       0      ]}, ... % red   (bright)
        {[0       0       255/255]}, ... % blue  (bright)
        {[0       255/255 0      ]}, ... % green (bright)
        {[102/255 047/255 0      ]}, ... % brown (bright)
      ];
      project.graphicMarker = [{'.'},{'.'},{'.'},{'.'}];
      project.graphicMarkersize = [{3.0},{3.0},{3.0},{3.0}];
      project.graphicTickFormat = [{'%3.2f'},{'%3.3f'},{'%3.2f'},{'%3.2f'}];

     
      fh = figure(); clf;
      for i = 1:numberOfFigures,
        x=[]; y=[];
        
        % Position and Size of Figure
        set(gcf,'units','pixels','position',figureRectangle);
        set(gcf,'paperposition',paperPosition);
        
        % Plotting
        % build the X variable
        xAxisVariableName = char(figureXAxisVariables{1}(1));
        x = self.ctdDataCellArray{ ...
              find( ...
                strcmp( ...
                  xAxisVariableName, ...
                  self.configuration.dataVariableNames ...
                ) ...
              ) ...
            }; 
        
        % build the Y variable
        yAxisVariableName = char(figureYAxisVariables{1}(i));
        y = self.ctdDataCellArray{ ...
              find( ...
                strcmp( ...
                  yAxisVariableName, ...
                  self.configuration.dataVariableNames ...
                ) ...
              ) ...
            }; 
        
        subplot(numberOfFigures, 1, i);
        plot(x,y,...
            'color',project.graphicColor{i},...
            'linestyle','none',...
            'marker',project.graphicMarker{i},...
            'markersize',project.graphicMarkersize{i});
        if i == 1
          titleText = [figureTitlePrefix ' Water Quality: latest observation period: ' ...
                      datestr(self.processTime - self.configuration.timerInterval/60/24, ...
                      'mm-dd-yyyy HH:MM') ' to ' ...
                      datestr(self.processTime,'HH:MM')];
          title(titleText);
        end
        grid on; axhan = gca; 
        
        % Limits
        % set the X axis limits based on the figure duration passed into
        % this function
        xlim([(max(x) - (figureDuration/60/60/24 - 1)) max(x)]);
        
        % set the Y axis limits based on min and max observations
        minYObservation=[]; maxYObservation=[];
        minYObservation=min(y); maxYObservation=max(y);
        ylim([minYObservation maxYObservation]);
        
        % Ticks and Ticklabels
        %xtick = get(axhan,'XTick');
        xtick = floor(now - (figureDuration/60/60/24):now);
        xticklabel = datestr(xtick,'ddmmmyy');
        ytick = get(axhan,'YTick');
        yticklabel = num2str(ytick',project.graphicTickFormat{i});
        set(axhan,'XTick',xtick,'YTick',ytick);
        set(axhan,'XTickLabel',xticklabel,'YTickLabel',yticklabel);
        
        %Add Fahrenheit axis to temperature plot
        if ( strmatch(char(yAxisVariableName), ...
                      self.configuration.temperatureFieldName, 'exact') )
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
        axes(axhan);
        hylab = ylabel(project.figureYAxisVariables{i},'fontsize',10,'fontweight','normal'); 
        set(hylab,'units','normalized','position',yLabelPosition);
           % Label Fahrenheit axis on temperature plot
           if i==1,
               axes(ax2); 
               hylab2 = ylabel([yAxisVariableName ' (\circF)'],'fontsize',10,'fontweight','normal'); 
           end;
        set(fh,'renderer','painters'); %,'visible','off');
      end
      value = fh;
    end
    
    % A method used to export a figure to various vector and raster-based image
    % formats.  This method requires ImageMagick to be installed on the processing
    % machine.
    % @param inputFigure - the figure to be exported
    % @param outputFormat - the desired raster or vector format (EPS, PNG, JPG, PDF)
    % @param outputParameters
    % @returns void
    function export(self, inputFigure, outputFormat, outputParameters)
     
      if ( self.configuration.debug )
        disp('CTDProcessor.export() called.');
      end
          
          % Export to Enhanced Postscript
          timestamp = datestr(self.processTime, 'yyyymmddHHMMSS');
          print(inputFigure,'-depsc2', ...
            [self.configuration.outputDirectory ...
             self.configuration.rbnbSource  '_' timestamp '.10.1' '.eps']);
          
          % Copy to 'latest' EPS
          eval(['!' self.configuration.copyPath ' -f '  ...
            self.configuration.outputDirectory ...
            self.configuration.rbnbSource '_' timestamp '.10.1' '.eps ' ...
            self.configuration.outputDirectory ...
            'latest.eps']);
          
          % Convert to JPG
          eval(['!' self.configuration.convertPath ' -colorspace RGB -density 800x800 -geometry 800x800 ' ...
            self.configuration.outputDirectory ...
            self.configuration.rbnbSource '_' timestamp '.10.1' '.eps ' ...
            self.configuration.outputDirectory ...
            self.configuration.rbnbSource  '_' timestamp '.10.1' '.jpg']);

          % Copy to 'latest' JPG
          eval(['!' self.configuration.copyPath ' -f '  ...
            self.configuration.outputDirectory ...
            self.configuration.rbnbSource '_' timestamp '.10.1' '.jpg ' ...
            self.configuration.outputDirectory ...
            'latest.jpg']);


          % Convert to PDF
          eval(['!' self.configuration.convertPath ' -colorspace RGB ' ...
            self.configuration.outputDirectory ...
            self.configuration.rbnbSource '_' timestamp '.10.1' '.eps ' ...
            self.configuration.outputDirectory ...
            self.configuration.rbnbSource  '_' timestamp '.10.1' '.pdf']);
          
          % Copy to 'latest' PDF
          eval(['!' self.configuration.copyPath ' -f '  ...
            self.configuration.outputDirectory ...
            self.configuration.rbnbSource '_' timestamp '.10.1' '.pdf ' ...
            self.configuration.outputDirectory ...
            'latest.pdf']);        
    end
    
    % A method used to fetch the ASCII data string for the given RBNB
    % Data Turbine source, channel, reference, and given time duration
    % @todo - support the RBNB 'absolute' reference
    % @param source - the name of the RBNB CTD source instrument
    % @param channel -  the name of the RBNB CTD channel
    % @param reference - the reference datum for the time series (newest, oldest)
    % @param duration - the duration of the time series to process in seconds
    function [ctdDataString, ctdDataTimes, ctdDataName] = getRBNBData(self)
      
      if ( self.configuration.debug )
        disp('CTDProcessor.getRBNBData() called.');
      end
      
      % set the pertinent properties of this CTDProcessor object
      
      % Create a new sink client to the DataTurbine
      try
      matlabSink = rbnb_sink( ...
        [self.configuration.rbnbServer ':' ...
         self.configuration.rbnbPort], ...
         self.configuration.rbnbSinkName);
      catch rbnbSinkException
        disp('Could not create RBNB sink client.');
        disp(rbnbSinkException.message);
      end
      
      % define the request details (get the latest 7 days of data)
      fullChannelName = [self.configuration.rbnbSource '/' self.configuration.rbnbChannel];
      
      % make the request to the DataTurbine and close the connection
      try
      [ctdDataString, ...
       ctdDataTimes,  ...
       ctdDataName] = ...
        rbnb_get(matlabSink, fullChannelName, self.configuration.startTime, ...
        self.configuration.duration, self.configuration.reference);
      matlabSink.CloseRBNBConnection;
      clear matlabSink fullChannelName startTime;
      
      % write the data to disk  
      % fd = fopen([self.configuration.rbnbSource '.10.1.dat'], 'w', 'l');
      % fwrite(fd, mostRecentData, 'int8');
      % fclose(fd);
      
      catch rbnbChannelException
        disp('Could not get channel data.  Setting values to null.');
        disp(rbnbChannelException.message);
        ctdDataString = [];
        ctdDataTimes = [];
        ctdDataName = '';
      end
    end % getRBNBData
    
    % A method that appends a new variable field name to the dataVariableNames
    % property in order to keep track of variable names inside of the 
    % ctdDataCellArray
    function updateDataVariableNames(self, newFieldName)
      
      if ( self.configuration.debug )
        disp('CTDProcessor.updateDataVariableNames() called.');
      end
      
      % if the variable name does not already exist
      if ( isempty( ...
             find( ...
               strcmp( ...
                 newFieldName, ...
                 self.configuration.dataVariableNames ...
               ) ...
             ) ...
           ) ...
         )
         
        % add the new data column into the variable name and unit properties
        config = get(self, 'configuration');
        variableNameCellArray = get(config, 'dataVariableNames');      
        variableNameCellArray{length(variableNameCellArray) + 1} = newFieldName;      
        set(config, 'dataVariableNames', variableNameCellArray);     
        clear config variableNameCellArray;
      end
    end
    
    % A method that appends a new variable unit to the dataVariableUnits
    % property in order to keep track of variable units inside of the 
    % ctdDataCellArray
    function updateDataVariableUnits(self, newUnitName)
      
      if ( self.configuration.debug )
        disp('CTDProcessor.updateDataVariableUnits() called.');
      end
      
      % if the variable name does not already exist
      if ( isempty( ...
             find( ...
               strcmp( ...
                 newUnitName, ...
                 self.configuration.dataVariableUnits ...
               ) ...
             ) ...
           ) ...
         )
      
        % add the new data column into the variable name and unit properties
        config = get(self, 'configuration');
        variableUnitCellArray = get(config, 'dataVariableUnits');           
        variableUnitCellArray{length(variableUnitCellArray) + 1} = newUnitName;      
        set(config, 'dataVariableUnits', variableUnitCellArray);      
        clear config variableUnitCellArray;
      end
    end
    
    % --------------%
    % Setter methods 
    % --------------%

    % A setter method for the ctdDataString property
    function self = set.ctdDataString(self, value) 
      self.ctdDataString = char(value)';  
    end
    
    % A setter method for the timerStartTime property
    function self = set.timerStartTime(self, interval)
      % calculate the next start time based on the timer interval 
      value = ceil(now*24*60/interval)/(24*60/interval) ...
              + interval/60/24;
      self.timerStartTime = value;  
    end

    % --------------%
    % Getter methods 
    % --------------%
    
  end % methods
  
  events
  end % events
end % class definition
