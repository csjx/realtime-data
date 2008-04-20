function [F,XP] = llfft(x,dt,xname,xunits,tunits,nF);
% [F,XP] = llfft(x,dt,xname,xunits,tunits,nF);
%
% x:      single time series, fastest if its length is a power of 2
%         may be complex
% dt:     sample interval
% xname:  name of x variable (i.e. 'Pressure' or 'Velocity')
% xunits: units for x (i.e. '(m/s)' or 'm')
% tunits: units for frequency (i.e. if dt is in 's', tunits is 'Hz');
% nF:     nominal number of frequency bands of resulting average
%         the actual number of bands will be less 
%
% F:      center frequencies of resulting frequency bands
% XP:     power spectrum of x (dimension: xunits^2/tunits)
% 
% 
% LLFFT transforms the time series x, computes the scaled power 
% spectrum, averages it into logarithmically-increasing bands and plots 
% the spectrum with a log-log plot, including titles and units. 
% 
% To add more spectra to the plot, use addllfft.m 
%
% Copyright (C) 2001, Lee Gordon, NortekUSA LLC

[r,c]=size(x);
  if min(r,c)>1,
  fprintf('llfft does not work for multiple time series\r\n');
  return;
  end;
if c>r, x=x'; end;

np=length(x);               % number of points
  if mod(np,2)==1,          % make even number of points
  np=np-1;                  % (it's just easier)
  x=x(1:np);
  end;

Dt=np*dt;                   % Dt = total duration of TS
f=[1:(np/2)]/Dt;            % frequency array up to Nyquist

xf=abs(fft(x));             % compute spectrum

xp=xf(2:(np/2+1)).^2;       % compute power spectrum & ignore zero freq
xp=xp*2/np^2/f(1);          % scale power spectrum

[F, XP] = logavg(f, xp, nF);   % logarithmically average the spectrum

loglog(F,XP,'b');
xlabel(['Frequency (',tunits,')']);
ylabel([xname,' (',xunits,'^2/',tunits,')']);
zoom on;


