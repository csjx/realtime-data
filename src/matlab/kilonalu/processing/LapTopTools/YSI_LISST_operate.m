clear
%%%%%%%%%%%%%%%%%%%%%%%%%%%
KN_TC_2007   % loads tchain parameters;  timer functions are commented out
%%%%%%%%%%%%%%%%%%%%%%%%%%%%
YSIdir='C:\Documents and Settings\User\Desktop\YSIdata';
YSIfid='KNMay24_07.TXT';
LISSTdir='C:\Program Files\Sequoia\LISST100';
LISSTfid='KNMay10_2007.asc';
Savedir1='z:\YSI';
Savedir2='z:\LISST';
t0=datenum('24-May-2007 18:20:00');
%%%%%%%%%%%%%%%%%%%%%%
load('Z:\YSI\YSImostrecent');
load('Z:\LISST\LISSTmostrecent');  % start processing at the end of the last averaging interval
% LISSTmostrecent=[];
% LISSTtime=[];
%t0 = YSItime(end);
tlisst=[];
tysi=0;
i=1;
flg=0;
param=[];
psd=[];
ysip=0;
lisstp=0;
while flg==0
     if now>strttim
         KN_TC_RUN_2007
     end
    if now >= t0+10.5/(24*60)        
        cd(YSIdir);
        YSIfile=fopen(YSIfid);
        if YSIfile<0
            error;
        end
        fseek(YSIfile,ysip,'bof');
        i=1;
        param=[];
        tysi = 0;
        d ='1';
        while ischar(d) & tysi(end)<t0+10/(24*60)
            ysip=ftell(YSIfile);
            if size(str2num(d(19:end)),2)==11
                param(i,:)=str2num(d(19:end));
                if param(i,1)<24.2
                    param(i,1)=NaN;
                elseif param(i,1)>25.5
                    param(i,1)=NaN;
                end
                try
                tysi(i)=datenum(d(1:18));
                catch
                    tysi(i)  = t0+1/(24*3600); % use same time as previous sample plus 1 sec
                    fprintf(['Bad Date vector: ' d(1:18) ' \n']);
                end
                i=i+1;
                %fprintf('data point read %d \n',i);
            else
                %fprintf('no data read \n');
            end
            d=fgetl(YSIfile);

        end
        % average data every 10 minutes and save to folder for online
        if size(param,2)>2;
            avg=find(tysi>t0&tysi<(t0+10/(60*24)));
            paramavg=nanmean(param(avg,:),1);
            YSImostrecent=[YSImostrecent; paramavg];
            YSItime=[YSItime; t0+5/(24*60)];
            cd(Savedir1);
            save YSImostrecent.mat YSImostrecent YSItime;
            fprintf(['YSI most recent file generated, t = ' datestr(t0) ' YSIp = ' num2str(ysip) '\n' ]);
        end
        % repeat process for LISST data
        cd(LISSTdir);
        LISSTfile=fopen(LISSTfid);
        if LISSTfile<0
            error;
        end
        fseek(LISSTfile,lisstp,'bof');
        i=1;
        psd=[];
        tlisst=0;
        d ='1';
        d2=0;
        while ischar(d) & tlisst(end)<t0+10/(24*60)
            d2=str2num(d);
            lisstp=ftell(LISSTfile);
            if size(d2,2)==42
                psd(i,:)=d2(1:32);
                year=str2num(datestr(now,10));
                month=str2num(datestr(now,5));
                day=floor(d2(39)/200);
                hour=(d2(39)-day*200)/2;
                minute=floor(d2(40)/400);
                sec=(d2(40)-minute*400)/4;
                tlisst(i)=datenum(year,month,day,hour,minute,sec);
                %if rem(i,20) == 0
                   % fprintf(['data point read %d, time: ' datestr(tlisst(i)) '; pos: %d \n'],i, lisstp);
               % end
                i=i+1;
            else
                %fprintf('no data read \n');
            end
            d=fgetl(LISSTfile);

        end
        % average data every 10 minutes and save to folder for online
        if size(psd,1)>2;
            avg=find(tlisst>t0&tlisst<(t0+10/(60*24)));
            psdavg=mean(psd(avg,:),1);
            LISSTmostrecent=[LISSTmostrecent; psdavg];
            LISSTtime=[LISSTtime; t0+5/(24*60)];
            cd(Savedir2);
            save LISSTmostrecent.mat LISSTmostrecent LISSTtime;
            fprintf(['LISST most recent file generated, t = ' datestr(t0) ' LISSTp = ' num2str(lisstp) ' \n']);
        end
        fclose('all');
        t0=t0+10/(24*60);
    else  % give the machine a break if its waiting ...
            pause(60);
    end;
end