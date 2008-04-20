function [F,XP] = addllfft(x,dt,nF,color);
% [F,XP] = addllfft(x,dt,nF,color);
%
% x:      Single time series, fastest if its length is a power of 2
%         may be complex
% dt:     Sample interval
% nF:     Nominal number of frequency bands of resulting average
%         the actual number of bands will be less
% color:  Color used to plot spectrum (i.e. 'r' or 'g')
%
% F:      Center frequencies of resulting frequency bands
% XP:     Power spectrum of x (dimension: xunits^2/tunits)
%
% ADDLLFFT works just like LLFFT, but it only adds a new spectrm to 
% whatever has already been plotted. Use the color parameter to plot 
% a different color from LLFFT (which uses blue). You can run ADDLLFFT as 
% many times as you want on a given plot.
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

Dt=dt*np;                   % Dt = total duration of TS
f=[1:(np/2)]/Dt;            % frequency array up to Nyquist

xf=abs(fft(x));             % compute spectrum

xp=xf(2:(np/2+1)).^2;       % compute power spectrum & ignore zero freq
xp=xp*2/np^2/f(1);          % scale power spectrum

[F, XP] = logavg(f, xp, nF);

hold on
loglog(F,XP,color);
hold off

