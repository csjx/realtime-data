function [Su,Sp,Dir,Spread,F,dF,DOF] = wds(u,v,p,dt,nF,hp,hv,parms);
% [Su,Sp,Dir,Spread,F,dF,DOF] = wds(u,v,p,dt,nF,hp,hv,[parms]);
%
% Su is the surface elevation spectra (m^2/Hz), based on the velocity data
% Sp is the surface elevation spectra (m^2/Hz), based on the pressure data
% Dir is the wave direction (deg) 
% Spread is the wave spreading (deg)
%     all are in matrices (size = [nf nt], where
%     nf is the number of output frequency bands
%     nt is the number of input time series
% F is the center frequency of each band
% dF is the bandwidth of each band
% DOF is th number of degrees of freedom in each band
%
% u,v are east and north components of velocity (m/s), 
% p is pressure (m)
%     all are in matrices (size = [np nt]), where
%     np = time series length (must be even; power of 2 is fastest),
%     nt = number of time series
% dt is the sample interval in s (typically 0.5 or 1 s)
% nF is the nominal number of output frequencies; the result is nf <= nF
% hp is the height of the pressure sensor above the bottom (m)
%     (this means the water depth is the mean pressure plus hp)
% hv is the height of the velocity cell above the pressure sensor (m)
%
% parms = [lf, maxfac, minspec, Ndir] 
%   lf is the low-frequency cutoff. F<lf are not output
%   maxfac is the largest factor scaling pressure to surface elevation
%     spectra and directions at F above this cutoff are returned as NaNs
%   minspec is the minimum spectral level for which direction is computed. 
%     directions for spectra below this level are returned as NaNs
%   Ndir is the direction of the "north" component (degrees)
%   default: parms = [0.03 200 0.03 0];
% 
% Note: The routine is faster if the length of the time series
%   factors into small integers, and fastest if it is a power of 2
% 
% Copyright (C) 2001, Lee Gordon, NortekUSA LLC

if nargin<8,
  parms=[0.03 200 0.03 0];
  end;
lf=parms(1);
maxfac=parms(2);
minspec=parms(3);
Ndir=parms(4);

[np,nt]=size(p);            % no. of points & no. of time series
  if mod(np,2)==1,          % make even number of points
  'Time series lengths must be even'    % (it's just easier)
  return;
  end;

Dt=np*dt;
f=[1:(np/2)]/Dt;            % frequency array up to Nyquist

uf=(fft(u));             % compute spectrum
vf=(fft(v));             % 
pf=(fft(p));

up=abs(uf(2:(np/2+1),:).^2);       % compute power spectrum & ignore zero freq
vp=abs(vf(2:(np/2+1),:).^2);       % (this uses first half of spectrum)
pp=abs(pf(2:(np/2+1),:).^2);
up=up*2/np^2/f(1);          % scale power spectrum
vp=vp*2/np^2/f(1);
pp=pp*2/np^2/f(1);

pup=real((pf.*conj(uf))*2/np^2/f(1)); % scaled cross-spectra
pup=pup(2:(np/2+1),:);    %limit to the same frequencies as power spectra
pvp=real((pf.*conj(vf))*2/np^2/f(1));
pvp=pvp(2:(np/2+1),:);
puv=real((uf.*conj(vf))*2/np^2/f(1));
puv=puv(2:(np/2+1),:);

[F, Cuu] = logavg(f, up, nF);  % average into log bands
[F, Cvv] = logavg(f, vp, nF);
[F, Cpp] = logavg(f, pp, nF);
[F, Cpu] = logavg(f, pup, nF);
[F, Cpv, dF, Ns, Ne] = logavg(f, pvp, nF);
[F, Cuv] = logavg(f, puv, nF);
DOF=2*(Ne-Ns+1);

aa=find(F>lf);            % low frequency cutoff
lF=length(aa);            % number of frequencies we keep

F=F(aa);
dF=dF(aa);
DOF=DOF(aa);

Cuu=Cuu(aa,:);
Cvv=Cvv(aa,:);
Cpp=Cpp(aa,:);
Cpu=Cpu(aa,:);
Cpv=Cpv(aa,:);
Cuv=Cuv(aa,:);

mp=ones(lF,1)*mean(p);        % vertical scaling

F2=F*ones(1,nt);
k=wavek(F2,mp+hp);

sinhkh=sinh(k.*(hp+mp));        %sinh scaling for water depth
coshkh=cosh(k.*(hp+mp));        %cosh scaling for water depth
coshkhz=cosh(k.*hp);            %scaling for pressure sensor elevation
coshkhZ=cosh(k.*(hp+hv));       %scaling for velocity elevation

ac=find((coshkh.^2)>maxfac);
Su=(Cuu+Cvv).*(sinhkh./(2*pi*F2)./coshkhZ).^2;
Sp=Cpp.*(coshkh./coshkhz).^2;
ad=union(find(Sp<minspec),ac);

Dir=57.296*atan2(Cpu,Cpv)+Ndir;
Dir=mod(Dir+180,360)-180;

R2=((Cuu - Cvv).^2 + 4*Cuv.^2).^.5./(Cuu+Cvv);
Spread = 57.296*((1-R2)/2).^.5;

Su(ac)=nan;
Sp(ac)=nan;
Dir(ad)=nan;
Spread(ad)=nan;


