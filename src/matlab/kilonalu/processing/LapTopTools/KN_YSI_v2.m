%%%%%%%%%%%%%%%%%%%
% KN_YSI_v2
%%%%%%%%%%%%%%%%%%%%%
if YSIstrt % if the YSI is starting up for the first time
    % connect to YSI
    s=serial('COM5','InputBufferSize',500000);
    fopen(s);
    % reset all parameters
    i=1;  
    m=1;
    consts=[]; 
    param=[];
    time=[];
%    YSImostrecent=[];
%    YSItime=[];
    cd(YSIsavedir);    
    load YSImostrecent.mat
    fprintf(s,'0'); % break sampling
    pause(2);
    fprintf(s,'1'); % re-start sampling through Matlab instead of hyperterminal
    YSIstrt=0;
    pause(120);
else
    cd(YSIdir);
    load YSIall.mat;
    i=length(time)+1;
    m=length(time)+1;
    cd(YSIsavedir);
    load YSImostrecent.mat;
end
%%%%%%%%%%%%%%%%%%%%%%%
% get and save YSI data from past 10 minutes
display('acquiring YSI data');
d='d';
    while ischar(d)&size(d)>0
        d=fscanf(s);
        if size(str2num(d(19:end)),2)==11; % check this length, normally 11
            param(i,:)=str2num(d(19:end));
            time(i)=datenum(d(1:18));
            i=i+1;
        end
    end
fprintf(s,'0'); pause(2)% Stop sampling
fprintf(s,'0'); pause(2)% return to sampling menu
fprintf(s,'0'); pause(2)% return to main menu
fprintf(s,'8'); pause(2)% enter advanced menu
% clear input buffer before reading probe constants
    d='d';
    while ischar(d)&size(d)>0
        d=fscanf(s);
    end
% check probe constants
fprintf(s,'1'); pause(2)
    d='d';
    consts=[consts datestr(now)];
    while ischar(d)&size(d)>0
        d=fscanf(s);
        consts=[consts d];
    end
% save all data before processing
cd(YSIdir)
save YSIall.mat param time consts
display(['YSIall file generated ' datestr(now) ' with ' num2str(i-m) ' data points']);
m=i;
% start sampling again
fprintf(s,'0'); pause(2) % return to advanced menu
fprintf(s,'0'); pause(2) % return to main menu
fprintf(s,'1'); pause(2) % enter sampling menu
fprintf(s,'1'); pause(2) % choose discrete sampling
% clear input buffer before starting sampling
d='d';
    while ischar(d)&size(d)>0
        d=fscanf(s);
    end
fprintf(s,'1'); pause(2) % start sampling
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% process raw YSI data into 10 minute averages
while t0<now-10/24/60
    g=find(time>t0&time<t0+DT2/24/60);
    YSImostrecent=[YSImostrecent;nanmean(param(g,:))];
    YSItime=[YSItime; nanmean(time(g),2)];
    t0=t0+DT2/24/60;
end    
cd(YSIsavedir);
save YSImostrecent.mat YSImostrecent YSItime;
display(['YSImostrecent file generated ' datestr(YSItime(end))]);

