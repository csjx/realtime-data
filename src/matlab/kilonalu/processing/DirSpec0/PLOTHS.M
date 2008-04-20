function ploths(time,Hs,peakF,peakDir,peakSpread,FigNo);
%function ploths(time,Hs,peakF,peakDir,peakSpread,[FigNo]);
%
% time (Matlab datetime units)
% Hs, peakF, peakDir, peakSpread are taken directly from hs.m
% FigNo, if entered, is the number of the figure; otherwise, the next 
%   figure number is used.
%
% Copyright (C) 2001, Lee Gordon, NortekUSA LLC

if nargin<6;FigNo=0;end;

td=time-floor(time(1));

if(FigNo==0);figure;else figure(FigNo);end;
subplot(4,1,1);
plot(td,Hs,'k');
set(gca,'XTickLabel',[])
ylabel('Hs (m)');
subplot(4,1,2);
plot(td,peakF.^-1,'k');
set(gca,'XTickLabel',[])
ylabel('Peak period (s)');
subplot(4,1,3);
plot(td,peakDir,'k');
ylabel('Peak direction (deg)');
subplot(4,1,4);
plot(td,peakSpread,'k');
ylabel('Peak spreading (deg)');
xlabel(['Days starting ' datestr(time(1),2)]);

