% note - start YSI sampling in Hyperterminal, then disconnect without stopping sampling
fclose('all');
clear
warning off all
% timer for YSI processing
YSIsavedir ='C:\Documents and Settings\HP USER\Desktop\AllKNdata\YSIdata';
YSIdir='C:\Documents and Settings\HP USER\Desktop\AllKNdata\YSIdata';
t0=datenum('13-Jul-2007 15:20:00'); % set this to 10 minutes BEFORE current time
DT2=10; % time between averaged samples (minutes)
YSIstrt=1; % If this is the first time starting up YSI, should be 1
% Note - if YSIstrt is 1, all previous YSI data will be overwritten - SAVEfirst!
strt=now+.5/60/24;
t2=timer('TimerFcn','KN_YSI_v2','period',DT2*60,'executionmode','fixeddelay');
startat(t2,strt);
