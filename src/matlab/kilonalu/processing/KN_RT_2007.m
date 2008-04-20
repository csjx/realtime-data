% KN_RT_2007
% initializes real-time analysis of data from KiloNalu observatory
%
% "ORIGINAL" version from GP 12/2006

clear
delete(timerfind)
base_dir = '/home/kilonalu/projects/bbl/trunk/kilonalu/processing/';
indir=[base_dir 'temp_data/latest_data/'];  % data directory
ldir=[base_dir 'temp_data/hold_and_process/'];  % temporary holding and processing
ftpdir = '/var/www/html/OE/KiloNalu/Data/';  % ftp directory
archdir = [base_dir 'KiloNalu/ADCP/archive/'];   %archive directory
specdir=[base_dir 'KiloNalu/ADCP/spectra/'];  % wave spectra storage (& archive?)

javaaddpath('/usr/local/RBNB/V3.0/bin/rbnb.jar');
addpath('/usr/local/RBNB/V3.0/Matlab/');
addpath(base_dir);
addpath([base_dir 'DirSpec0']);
addpath([base_dir 'DirSpec0/diwasp']);
%indir=[base_dir 'KiloNalu/ADCP/data/033007/'];  % data directory
%ldir=[base_dir 'KiloNalu/ADCP/storage/'];  % temporary holding and processing
%ftpdir = [base_dir 'KiloNalu/ADCP/ftp/'];  % ftp directory
%archdir = [base_dir 'KiloNalu/ADCP/archive/'];   %archive directory
%specdir=[base_dir 'KiloNalu/ADCP/spectra/'];  % wave spectra storage (& archive?)
% Write log files, initialize variables
depfilnam = '033007';

instrtype = 'adc'; % adc for ADCP, aqd for Aquadopp, vec for Vector
reproc = 1; % 0 = new deployment
            % 1 = continue deployment (do not reprocess existing data)
            % 2 = Reprocess existing data (all data in folder)

%%%%%% parameters for TChain processing - used by KN_TC_RUN_DK
TChain=0;  % 1: process tchain data; 0: skip tchain processing            
tcdepfilnam = 'tc070510';
tcindir = [base_dir 'TCindir']; % data directory on laptop
tcldir = [base_dir 'KiloNalu/TChain/TCtemp/'];  % temporary holding and processing
tczdir = [base_dir 'KiloNalu/TChain/'];   % summ files for plots
adcpdir = ldir;
% [MAB==meters above bottom]
MABgrid=(0:.25:12)';  % depth vector used by tcdepths.m data interpolation
                      % for the 10m tchain

% Timing parameters: 
DT = 20; % processing interval in minutes
nsvdys = 21;   % days to save in summ file

nodatagap = 1; % acceptable data gap (hours) before we need to add a filler

strttim = now+1/60/24;  % time for first analysis 
%strttim = ceil(now*24*60/DT)/(24*60/DT) + 5/60/24;
tstart = now;

% Processing Parameters:
dti = 1;  % approximate time interval (in seconds) for data interpolation for regular time series
ford=5; % filter order for filtering pressure data in KN_ADCP_Proc_2007
fcut=.5;  % pressure filter cutoff in minutes
ztrans = 0.3;
%%% if using instrument heading ....
% headoffset=10;
% %%%% if using observed heading ....
headoffset=0;
%heading=293;% FILL IN;
%heading = 127; % 7/10/07
heading = 200; %CSJ per JRW 4/17/2008
beams_up = 1;
zds = [-4 -8];  % Use data at this depth range for dir spectra analysis (z is positive UP)
beam = 1;  % data in beam coordinates? 1=y, 0=n
% Note :  the current version of RT_RUN is ONLY for beam coordinates
zlm = [-12 -1];
zbed=3;  % specify bottom velocities at up to # m above bed

YSI=0; %If YSI is connected, change this to 1
LISST=0; % if LISST is connected, change this to 1
ADCP = 1;
ADAM = 0;
% Spectral parameters
SM.freqs = [0.0025:0.005:0.35];
SM.dirs = [0:1:180];
SM.xaxisdir = 90;
EP.method = 'EMEP';

kp = 0;    % what is this for???????
% more spectral parameters
% next 5 lines added 3/1/07 for wave directional spectra routines
kk = 0;    % to count wave routine iterations
specsav=1;      % to save spec-files (0 is don't save)
swelldir=[];    % to limit spectra to incident waves
nday=[];        % if empty labels the day
specfil=[];     % if empty names a new file
prevfilid = '999'; % file identifier for previous data file
prevdat = 0;  % time for previously analyzed file 
% prevtim: time for previous averaging interval  
% use prevtim = 0 for new deployment (no old data to append)
% use prevtim = 1 to continue existing deployment
% use prevtim = -1 to interpolate previous deployment data onto new depths
prevtim = 0;  

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%%%%%%%  NOT IN USE   %%%%%%%%%%%%%%%%%
% first process all existing data ??
if reproc == 2
      prevtim = 1;
      KN_reproc;
end

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% % if continued deployment ...
% 	Read in processed data file and determine latest process time
% 
% Find all .enr files (or log files) since the last process time.
% 	Sort and convert each to .mat files
% 	Process and add to process data file
% 	
% Plot processed figures
% Wait for next process time
% Read in most recent file
% How to set ts?!
% 1  use last 20 minutes of data (te - DT), but process data again, based on the hour 

% Set timers
t1 = timer('TimerFcn','KN_RT_RUN_2007v3','period',DT*60,'executionmode','fixeddelay');
startat(t1,strttim);


