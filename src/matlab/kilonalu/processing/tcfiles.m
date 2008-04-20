function [tctime,tccal,tcpres]=tcfiles(tcfilnam);
% INPUT name of .log file recorded by Advanced Serial Data Logger software.
% BE SURE TO INCLUDE  ".log"   in file name
% Routine puts tchain .log (text) files into .mat files with 4 variables
% tctime -- vector of time stamps (matlab numbers)
% tcpres -- vector of pressure reading (taken x meters above thermistor 7)
% tctemp -- matrix of thermistor readings (7 x times). #1 is at bottom.
% tccal -- internode calibration  - see comments at end of page.
% Imagesc(tctemp) gives an upside down field. Use "axis xy" to flip.
% File name is the data starttime. (tc+ yr mon day hr min sec)
% OUTPUT .mat file with same name
% jwells  December 18 2006; February 9 2007 - revised to deal with headers 


% filnam=['tc07011220.log'];  % NOTE!!!  specify .log 
% path ='C:\KiloNalu\TChain\tcCalData\';  % change as needed
tctime=[];
tctemp=[];
tcpres=[];
tccal=[];

fid=fopen(tcfilnam);   
tcstr=[];
nextline=[];
while 1
    nextline=fgetl(fid);
    if ~ischar(nextline),   break,   end
    if length(nextline)==98 & str2num(nextline(1:3))==200
    tcstr=[tcstr;nextline];
    end
end
fclose(fid);

tcmatrix=[];
tck=1;
for tck=1:size(tcstr,1)
    tcmatrix(tck,:)=strread(tcstr(tck,:)); 
end

tcdate=[];
tci=1;
for tci=1:length(tcmatrix)
    d2=num2str(round(tcmatrix(tci,1)));
    yr=d2(1:4);
    mo=d2(5:6);
    dy=d2(7:8);
    hr=d2(9:10);
    mn=d2(11:12);
    sc=d2(13:14);
    d3=datenum(str2num(yr),str2num(mo),str2num(dy),str2num(hr),str2num(mn),str2num(sc));
    tcdate=[tcdate;d3];
end
clear nextline fid tck tci d2 yr mo dy hr mn sc d3
tcP=tcmatrix(:,9);
tcT=tcmatrix(:,2:8);
tctime=[tctime;tcdate];
tctemp=[tctemp;tcT];
tcpres=[tcpres;tcP];
clear tcmatrix tcstr tcP tcT tcdate
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% calibration factor calculated from measurements made
% over a 5 hour period on January 12, 2007.  The Tchain
% was in a tank with water being stirred by a pump to
% ensure an isothermal environment.
% Adjustments for the 7 tnodes in the order that the
% data is reported:
calfac=[-0.00652
         0.00337
        -0.00116
         0.00723
         0.00129
        -0.00269
        -0.00153];
% using same number of decimal places as T data
% see matlab figure: 'ThermistorCalibration'

tccal=[];
tcm=1;
for tcm=1:7
tccal(:,tcm)=tctemp(:,tcm)-calfac(tcm);
end
tccal=tccal';
% calculate position of pressure sensor (depth below surface)
% readings in m of fresh water at 4 deg C (i.e. assumes rho=1000 kg/m^3)
% HISSP=10.36032 kg/m^3 (corresponds to ~ 101.6kPa, from NWS and Flament's
% atlas); SWrho=1023.6 (from Flament's atlas)
tcpres=(tcpres-10.36735)*1000/1023.6;
% keyboard
% outfil=[strrep(tcfilnam,'.log','.mat')];  % saved as tcCal.mat
% save ([outfil], 'tctime', 'tcpres', 'tctemp', 'tccal');

clear calfac tcm tctemp ans
%%% all the 'clear' lines have been added so that this can be called as 
%%% a script without creating a ton of new variables
% keyboard