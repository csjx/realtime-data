% LISST_run_2007
% reuns LISST through serial commands
try
    load('Z:\LISST\LISSTmostrecent');
catch
    pause(5);
    try
        load('Z:\LISST\LISSTmostrecent');
    catch
        disp('LISSTmostrecent file not loaded')
    end
end
s=serial('COM13','baudrate',9600,'Terminator','CR','inputbuffersize',1000);
fopen(s);
fprintf(s,'PSET 12');
fprintf(s,'RUN');
fprintf(s,'13');
pause(5);
sample=[];
j=1;
r=1;
for r=1:59
    dd=fgetl(s);
    if size(str2num(dd),1)>0
        sample(j)=str2num(dd);
        j=j+1;
    end
end
%plot(sample(1:32))
pause(5)
fprintf(s,hex2dec('03'));
fprintf(s,'PCLR 7,10');
fprintf(s,'PCLR 12');
fclose(s)
delete(s)
clear s dd j r
% process the data that has just been read in
d=[1.36 1.6 1.89 2.23 2.63 3.11 3.67 4.33 5.11 6.03 7.11 8.39 9.9 11.7 13.8 16.3 19.2 22.7 26.7 31.6 37.2 44.0 51.9 61.2 72.2 85.2 101 119 140 165 195 230];
r=d/2; clear d
month=str2num(datestr(now,5));
day=floor(sample(39)./200);
hour=sample(39)-200*floor(sample(39)./200);
min=floor(sample(40)./400);
sec=sample(40)-400*floor(sample(40)./400);
%lisstt=[datenum(2007,month,day,hour/2.,min,sec)].';
lisstt=now;
lisstT=[lisstT lisstt];
clear hour min sec day
vc=sample(1:32);
vpp = 1000*4/3*pi*(10^-6*r).^3; % volume per particle (L)
for j=1:length(vc)
    nump(j)=vc(j).*10^-6./vpp(j); %number of particles
end
VC=[VC; vc];
NUMP=[NUMP; nump];
cd(LISSTSavedir);
save LISSTmostrecent.m NUMP VC lisstT r
display(['LISST most recent file generated ' datestr(lisstt)])