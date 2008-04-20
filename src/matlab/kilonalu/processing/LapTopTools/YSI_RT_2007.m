% YSI_RT_2007
% initializes real-time analysis of data from KiloNalu observatory
%
% "ORIGINAL" version from GP 12/2006

clear
delete(timerfind)
YSIdir='C:\Documents and Settings\User\Desktop\YSIdata';
LISSTdir='C:\Program Files\Sequoia\LISST100';
Savedir1='z:\YSI';
Savedir2='z:\LISST';

% Timing parameters: 
DT = 10; % processing interval in minutes
nsvdys = 21;   % days to save in summ file
strttim = ceil(now*24*60/DT)/(24*60/DT) + 5/60/24; % time for first analysis
tstart = now;

% Processing Parameters:


t1 = timer('TimerFcn','YSI_RUN_2007','period',DT*60,'executionmode','fixeddelay');
startat(t1,strttim);

