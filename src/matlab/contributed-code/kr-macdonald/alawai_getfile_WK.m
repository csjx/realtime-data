%% Waikiki Instrument
mydir=('C:\Users\User\Desktop\alawai\WK01XX_001CTDXXXXR00\');
d=dir(mydir);
for j=3:length(d); %Year folder
    myfile=[mydir d(j).name];
    e=dir(myfile);
    for i=3:length(e);  % Month folder
        mynextfile=[myfile '\' e(i).name];
        f=dir(mynextfile);
        for h=3:length(f); % Day folder
            mythirdfile=[mynextfile '\' f(h).name];
            g=dir(mythirdfile);
            if(h==3 && i==3 && j==3)
                for k=3:length(g); % .dat files within the day folder
                    mylastfile=[mythirdfile '\' g(k).name];
                    [num, temp, cond, v1, sal, dum1, dum2] =...
                    textread(mylastfile,'%c%f%f%f%f%s%s','delimiter',','); % may need changes based on number of sensors
                    temporary=strcat(dum1, dum2);               
                    time = datenum(temporary, 'dd mmm yyyyHH:MM:SS');
                    if(k==3)
                        wktime = time;
                        wktemp = temp;
                        wksal = sal;
                        wkv1 = v1;
                     else
                        wktime = [wktime; time];
                        wktemp = [wktemp; temp];
                        wksal = [wksal; sal];
                        wkv1 = [wkv1; v1];
                    end
                end
            else
                for k=3:length(g); % .dat files within the day folder
                    mylastfile=[mythirdfile '\' g(k).name];
                    [num, temp, cond, v1, sal, dum1, dum2] =...
                        textread(mylastfile,'%c%f%f%f%f%s%s','delimiter',','); % may need changes based on number of sensors
                    temporary=strcat(dum1, dum2);               
                    time = datenum(temporary, 'dd mmm yyyyHH:MM:SS');
                    wktime = [wktime; time];
                    wktemp = [wktemp; temp];
                    wksal = [wksal; sal];
                    wkv1 = [wkv1; v1];
                end
            end
        end
    end
end

%% First Ala Wai instrument 

mydir=('C:\Users\User\Desktop\alawai\AW01XX_002CTDXXXXR00\');
d=dir(mydir);
for j=3:length(d); %Year folder
    myfile=[mydir d(j).name];
    e=dir(myfile);
    for i=3:length(e);  % Month folder
        mynextfile=[myfile '\' e(i).name];
        f=dir(mynextfile);
        for h=3:length(f); % Day folder
            mythirdfile=[mynextfile '\' f(h).name];
            g=dir(mythirdfile);
            if(h==3 && i==3 && j==3)
                for k=3:length(g); % .dat files within the day folder
                    mylastfile=[mythirdfile '\' g(k).name];
                    [num, temp, cond, v1, v2, sal, temporary] =...
                        textread(mylastfile,'%c%f%f%f%f%f%s','delimiter',','); % may need changes based on number of sensors               
                    time = datenum(temporary, 'dd mmm yyyyHH:MM:SS');
                    if(k==3)
                        aw1time = time;
                        aw1temp = temp;
                        aw1sal = sal;
                        aw1v1 = v1;
                        aw1v2 = v2;
                     else
                        aw1time = [aw1time; time];
                        aw1temp = [aw1temp; temp];
                        aw1sal = [aw1sal; sal];
                        aw1v1 = [aw1v1; v1];
                        aw1v2 = [aw1v2; v2];
                    end
                end
            else
                for k=3:length(g); % .dat files within the day folder
                    mylastfile=[mythirdfile '\' g(k).name];
                    [num, temp, cond, v1, v2, sal, temporary] =...
                        textread(mylastfile,'%c%f%f%f%f%f%s','delimiter',','); % may need changes based on number of sensors               
                    time = datenum(temporary, 'dd mmm yyyyHH:MM:SS');
                    aw1time = [aw1time; time];
                    aw1temp = [aw1temp; temp];
                    aw1sal = [aw1sal; sal];
                    aw1v1 = [aw1v1; v1];
                    aw1v2 = [aw1v2; v2];
                end
            end
        end
    end
end

%% Second Ala Wai instrument  

mydir=('C:\Users\User\Desktop\alawai\AW02XX_001CTDXXXXR00\');
d=dir(mydir);
for j=3:length(d); %Year folder
    myfile=[mydir d(j).name];
    e=dir(myfile);
    for i=3:length(e);  % Month folder
        mynextfile=[myfile '\' e(i).name];
        f=dir(mynextfile);
        for h=3:length(f); % Day folder
            mythirdfile=[mynextfile '\' f(h).name];
            g=dir(mythirdfile);
            if(h==3 && i==3 && j==3)
                for k=3:length(g); % .dat files within the day folder
                    mylastfile=[mythirdfile '\' g(k).name];
                    [num, temp, cond, v1, v2, sal, temporary] =...
                        textread(mylastfile,'%c%f%f%f%f%f%s','delimiter',','); % may need changes based on number of sensors               
                    time = datenum(temporary, 'dd mmm yyyyHH:MM:SS');
                    if(k==3)
                        aw2time = time;
                        aw2temp = temp;
                        aw2sal = sal;
                        aw2v1 = v1;
                        aw2v2 = v2;
                     else
                        aw2time = [aw2time; time];
                        aw2temp = [aw2temp; temp];
                        aw2sal = [aw2sal; sal];
                        aw2v1 = [aw2v1; v1];
                        aw2v2 = [aw2v2; v2];
                    end
                end
            else
                for k=3:length(g); % .dat files within the day folder
                    mylastfile=[mythirdfile '\' g(k).name];
                    [num, temp, cond, v1, v2, sal, temporary] =...
                        textread(mylastfile,'%c%f%f%f%f%f%s','delimiter',','); % may need changes based on number of sensors               
                    time = datenum(temporary, 'dd mmm yyyyHH:MM:SS');
                    aw2time = [aw2time; time];
                    aw2temp = [aw2temp; temp];
                    aw2sal = [aw2sal; sal];
                    aw2v1 = [aw2v1; v1];
                    aw2v2 = [aw2v2; v2];
                end
            end
        end
    end
end

%% Plots
%% Salinity
subplot(311)
plot(wktime,wksal)
datetick('x','mmm-dd HH:MM','keeplimits','keepticks')
title('WK01XX 001CTDXXXXR00')
hold on
subplot(312)
plot(aw1time,aw1sal)
datetick('x','mmm-dd HH:MM','keeplimits','keepticks')
title('AW01XX 002CTDXXXXR00')
subplot(313)
plot(aw2time,aw2sal)
datetick('x','mmm-dd HH:MM','keeplimits','keepticks')
title('AW02XX 001CTDXXXXR00')
suptitle('Salinity')
%% Temperature
Figure
subplot(311)
plot(wktime,wktemp)
datetick('x','mmm-dd HH:MM','keeplimits','keepticks')
title('WK01XX 001CTDXXXXR00')
hold on
subplot(312)
plot(aw1time,aw1temp)
datetick('x','mmm-dd HH:MM','keeplimits','keepticks')
title('AW01XX 002CTDXXXXR00')
subplot(313)
plot(aw2time,aw2temp)
datetick('x','mmm-dd HH:MM','keeplimits','keepticks')
title('AW02XX 001CTDXXXXR00')
suptitle('Temperature')
%% Fluorescence
Figure
subplot(311)
plot(wktime,wkv1)
datetick('x','mmm-dd HH:MM','keeplimits','keepticks')
title('WK01XX 001CTDXXXXR00')
hold on
subplot(312)
plot(aw1time,aw1v1)
datetick('x','mmm-dd HH:MM','keeplimits','keepticks')
title('AW01XX 002CTDXXXXR00')
subplot(313)
plot(aw2time,aw2v1)
datetick('x','mmm-dd HH:MM','keeplimits','keepticks')
title('AW02XX 001CTDXXXXR00')
suptitle('Fluorescence')
%% Transmittance
Figure
subplot(211)
plot(aw1time,aw1v2)
datetick('x','mmm-dd HH:MM','keeplimits','keepticks')
title('AW01XX 002CTDXXXXR00')
hold on
subplot(212)
plot(aw2time,aw2v2)
datetick('x','mmm-dd HH:MM','keeplimits','keepticks')
title('AW02XX 001CTDXXXXR00')
suptitle('Transmittance')