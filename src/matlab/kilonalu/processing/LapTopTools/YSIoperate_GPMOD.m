clear
YSIdir='C:\Documents and Settings\User\Desktop\YSIdata';
LISSTdir='C:\Documents and Settings\User\Desktop\LISST100\Data';
Savedir='z:\YSI';
t0=datenum('04/12/07 12:30:00');
%%%%%%%%%%%%%%%%%%%%%%
YSImostrecent=[];
LISSTmostrecent=[];
tLISST=[];
tysi=[];
i=1;
m=1;
flg=0;
index=[0];
param=[];
time=t0;
t=t0;
while flg==0
    if now >= t0+10/(24*60)
        cd(YSIdir);
        YSIfile=fopen('KNApr12_07.txt');
        d=fgets(YSIfile);
        while d>1;
            d=fgets(YSIfile);
            if size(str2num(d(19:end)),2)==11;
                param(i,:)=str2num(d(19:end));
                tysi(i)=datenum(d(1:18));
                index(m)=i;
                t(i)=time(end);
                i=i+1;
                m=m+1;
            end
        end
            % average data every 10 minutes and save to folder for online
            avg=find(tysi>t0&tysi<(t0+10/(60*24)));
            paramavg=mean(param(avg,:),1);
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
end