clear
LISSTdir='C:\Documents and Settings\User\Desktop\LISST100\Data';
cd(LISSTdir);
LISSTfile=fopen('KNApr12_07.asc');
lsavedir='z:\LISST';
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
YSIdir='C:\Documents and Settings\User\Desktop\YSIdata';
cd(YSIdir);
YSIfile=fopen('KNApr12_07.txt');
ysavedir='z:\YSI';
%t0=datenum('04/12/07 12:30:00');
t0=now;

%%%%%%%%%%%%%%%%%%%%%%
LISSTmostrecent=[];
LISSTtime=[];
i=1;
m=1;
flg=0;
index=[0];
param=[];
time=t0;
t=t0;
YSImostrecent=[];
YSItime=[];


while flg==0
    d=fgetl(LISSTfile);
    if d>1;
       if size(str2num(d),2)==42;
            data(i,:)=str2num(d);
            time(i)=now;
            index(m)=i;
            t(i)=time(end);
            i=i+1;
            m=m+1;
        end
        g=datestr(t(end),1);
        filnam=[g(1:6)];
        if (index(end)-index(1))>=1000
            save([filnam '.mat'],'data','time');
            fprintf(['updating file ' filnam '\n']);
            index=[0];
            m=1;
        end
        % average data every 10 minutes and save to folder for online
        if t(end)>= t0+10/(24*60)
            avgstrt=min(find(time>t0));
            dataavg=mean(data(avgstrt:end,1:32),1);
            LISSTmostrecent=[LISSTmostrecent; dataavg];
            LISSTtime=[LISSTtime; t0];
            cd(Savedir);
            save LISSTmostrecent LISSTtime dataavg;
            fprintf(['LISST most recent file generated' '\n']);
            cd(LISSTdir);
            t0=t0+10/(24*60);
        end
        % after 1 day of sampling start new data file
        if floor(t(end))~=floor(t(1))
            i=1;
            data=[];
            clear t time
            fprintf(['generating new file']);
        end
    else
        pause(10);
    end
end