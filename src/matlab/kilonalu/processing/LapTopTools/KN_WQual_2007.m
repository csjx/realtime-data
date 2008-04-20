% KN_WQual_2007
clear
delete(timerfind);
% 
% %%% parameters for tchain processing
% 
% tcindir = 'C:\Documents and Settings\User\Desktop\TChain';  %TCdata\2007_05';  % data directory
% tcldir = 'C:\Documents and Settings\User\Desktop\TCtemp';  % temporary holding and processing
% tczdir = 'Z:\TChain';   % Z directory for desktop access
% adcpdir = 'Z:\ADCP\storage';
% 
% depfilnam = 'tc070510';
% 
% nsvdys = 21;   % days to save in summ file
% nodatagap = 1; % acceptable data gap (hours) before we need to add a filler
% nday=[];        % if empty labels the day
% 
% % prevtim: time for previous averaging interval  
% % use prevtim = 0 for new deployment (no old data to append)
% % use prevtim = 1 to continue existing deployment
% %%% still need to figure out tchain needs when ADCP depth changes
% % use prevtim = -1 to interpolate previous deployment data onto new depths
% prevtim = 1; 
% 
% % [MAB==meters above bottom] assigns depths to temperature data
% MABgrid=(0:.25:12)';  % depth vector used by tcdepths.m data interpolation
%                       % for the 10m tchain
% %%% end of tchain parameter section
% % Timer for T-chain processing
% strttim=now+1/60/24;
% DT=20; %processing interval in minutes for Tchain
% t1=timer('TimerFcn','KN_TC_RUN_2007','period',DT*60,'executionmode','fixeddelay');
% startat(t1,strttim);

%%% parameters for YSI processing
% YSIdir='C:\Documents and Settings\User\Desktop\YSIdata';
% YSIfid='KNjul05_07.txt';
% YSISavedir='Z:\YSI';
% t0=datenum('10 Jul 2007 14:00:00');
% ysip=0;
% note - start YSI sampling in Hyperterminal, then disconnect without stopping sampling
warning off all
YSIsavedir ='Z:\YSI';
YSIdir='C:\Documents and Settings\User\Desktop\YSIdata';
t0=datenum('17-Jul-2007 20:10:00'); % set this to 10 minutes BEFORE current time
DT2=10; % time between averaged samples (minutes)
YSIstrt=1; % If this is the first time starting up YSI, should be 1
% Note - if YSIstrt is 1, all previous YSI data will be overwritten - SAVEfirst!
strt=now+.5/60/24;
t2=timer('TimerFcn','KN_YSI_v2','period',DT2*60,'executionmode','fixeddelay');
startat(t2,strt);
%%% end of parameters for YSI processing

%%% parameters for LISST processing
% LISSTdir='C:\Documents and Settings\User\Desktop\LISST100\Data';
% LISSTSavedir='Z:\LISST';
% lisstT=[];
% VC=[];
% NUMP=[];
% %Timer for LISST processing
% DT3=10;
% t3=timer('TimerFcn','KN_LISST_RUN_2007v2','period',DT3*60,'executionmode','fixeddelay');
% startat(t3,strt3);


