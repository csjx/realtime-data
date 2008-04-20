function plotwds(time,Su,Sp,Dir,Spread,F,FigNo);
%function plotwds(time,Su,Sp,Dir,Spread,F,FigNo);
%
% time (Matlab datetime units)
% Su, Sp, Dir, Spread, and F are taken directly from wds.m
%
% See comments on computations inside m-file

% With perfect sensors and precise knowledge of the various depths, the 
% two spectra based on pressure and velocity should match each other.
% Here are several errors that can lead to differences:
%  a) pressure offset
%  b) errors in the elevation of the pressure and/or velocity sensors 
%     above the bottom 
% Differences in the spectra at the high end may come from noises being 
% blown up by rapidly increasing scale factors, or (with an Aquadopp
% Profiler) from a small bias caused by the way the velocity cell
% averages the exponentially-decaying velocity. 
%
% Copyright (C) 2001, Lee Gordon, NortekUSA LLC

if nargin<7;FigNo=0;end;

td=time-floor(time(1));

clim2=log10(max([Sp(:); Su(:)]));
clim1=clim2-3;

if(FigNo==0);figure;else figure(FigNo);end;
subplot(4,1,1);
imagesc(td,log10(F),log10(Sp),[clim1 clim2]);colorbar;
set(gca,'XTickLabel',[]);
title('log(wave spectrum) based on pressure')
ylabel('log(F) (Hz)');
subplot(4,1,2);
imagesc(td,log10(F),log10(Su),[clim1 clim2]);colorbar;
set(gca,'XTickLabel',[]);
title('log(wave spectrum) based on velocity')
ylabel('log(F) (Hz)');
subplot(4,1,3);
imagesc(td,log10(F),Dir);colorbar;
set(gca,'XTickLabel',[]);
title('Direction (deg N)');
ylabel('log(F) (Hz)');
subplot(4,1,4);
imagesc(td,log10(F),Spread);colorbar;
ylabel('log(F) (Hz)');
title('Spread (deg)');
xlabel(['Time (days) starting ' datestr(time(1),2)]);

if(FigNo==0);figure;else figure(FigNo+1);end;
loglog(F,mean(Sp'),'r',F,mean(Su'),'b');
xlabel('Frequency (Hz)');
ylabel('Wave Spectrum (m^2/Hz)');
%axis([.01 .4 0.004 .5]);
legend('P','V');


