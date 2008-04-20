% KN_YSI_RUN_2007
while t0<now-DT2/24/60;
fiload=0;
try
    load('Z:\YSI\YSImostrecent');
catch
    pause(5);
    try
        load('Z:\YSI\YSImostrecent');
    catch
        disp('YSImostrecent file not loaded');
        fiload=1;
    end
end
i=1;
param=[];
tysi=0;
cd(YSIdir);
YSIfile=fopen(YSIfid);
fseek(YSIfile,ysip,'bof');
d='1';
if fiload==1
    YSImostrecent=[YSImostrecent; NaN*ones(1,11)];
    YSItime=[YSItime t0+10/24/60];
else
    while ischar(d)&tysi(end)<t0+DT2/24/60
        ysip=ftell(YSIfile);
        if size(str2num(d(19:end)),2)==11
            param(i,:)=str2num(d(19:end));
            try
                tysi(i,:)=datenum(d(1:18));
            catch
                tysi(i)=YSItime(end)+1/(24*60);
                fprintf(['bad date vector ' datestr(tysi(i)) '\n']);
            end
            i=i+1;
        end
        d=fgetl(YSIfile);
    end
    if size(param,1)>=2
        avg=find(tysi>t0&tysi<t0+DT2/24/60);
        paramavg=nanmean(param(avg,:),1);
        YSIt=nanmean(tysi);
        if YSIt<YSItime(end)
            YSIt=YSItime(end)+DT2/24/60;
        end
%         YSIall=[YSIall; paramavg];
%         YSItimeall=[YSItimeall; t0+(5/(24*60))];
        YSImostrecent=[YSImostrecent; paramavg];
        YSItime=[YSItime; YSIt];
%         YSIsave=find(YSItime>t0-21);
%         YSImostrecent=YSIall(YSIsave,:);
%         YSItime=YSItimeall(YSIsave);
        cd(YSISavedir)
        save YSImostrecent.mat YSImostrecent YSItime
%         cd(YSIdir)
%         save YSIall.mat YSIall YSItimeall
        fprintf(['YSI most recent file generated at ' datestr(YSItime(end)) ' YSIp = ' num2str(ysip) '\n']);
    end
end
fclose('all');
t0=t0+DT2/24/60;
end