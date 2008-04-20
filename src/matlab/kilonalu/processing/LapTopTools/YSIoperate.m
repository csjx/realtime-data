clear
YSIdir='C:\Documents and Settings\User\Desktop\YSIdata';
cd(YSIdir);
YSIfile=fopen('KNApr12_07.txt');
Savedir='z:\YSI';
t0=datenum('04/12/07 12:30:00');
%%%%%%%%%%%%%%%%%%%%%%
YSImostrecent=[];
YSItime=[];
i=1;
m=1;
flg=0;
index=[0];
param=[];
time=t0;
t=t0;
while flg==0
    d=fgets(YSIfile);
    if d>1;
        fprintf([d '\n']);
        if size(str2num(d(19:end)),2)==11;
            param(i,:)=str2num(d(19:end));
            time(i)=datenum(d(1:18));
            index(m)=i;
            t(i)=time(end);
            i=i+1;
            m=m+1;
        end
        g=datestr(t(end),1);
        filnam=[g(1:6)];
        if (index(end)-index(1))>=100
            save([filnam '.mat'],'param','time');
            fprintf(['updating file ' filnam '\n']);
            index=[0];
            m=1;
        end
        % average data every 10 minutes and save to folder for online
        if t(end)>= t0+10/(24*60)
            avgstrt=min(find(time>t0));
            paramavg=mean(param(avgstrt:end,:),1);
            YSImostrecent=[YSImostrecent; paramavg];
            YSItime=[YSItime; t0];
            temp=YSImostrecent(:,1);
            sal=YSImostrecent(:,5);
            DOsat=YSImostrecent(:,9);
            pH=YSImostrecent(:,6);
            turbid=YSImostrecent(:,8);
            cd(Savedir);
            save YSImostrecent temp sal DOsat pH turbid YSItime;
            fprintf(['YSI most recent file generated' '\n']);
            cd(YSIdir);
            t0=t0+10/(24*60);
        end
        % after 1 day of sampling start new data file
        if floor(t(end))~=floor(t(1))
            i=1;
            param=[];
            clear t time
            fprintf(['generating new file']);
        end
    else
        pause(60);
    end
end