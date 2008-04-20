function [Hs,peakF,peakDir,peakSpread] = hs(Su,Sp,Dir,Spread,F,dF);
% function [Hs,peakF,peakDir,peakSpread] = hs(Su,Sp,Dir,Spread,F,dF);
%
% Su, Sp, Dir, Spread, F and dF are taken drectly from wds.m
% 
% Output:
%     Hs           significant wave height
%     peakF        wave freauency (Hz) at the peak
%     peakDir      wave direction (deg) at the peak
%     peakSpread   wave spreading (deg) at the peak
% 
% see notes inside m-file for comments on computations
%
% Copyright (C) 2001, Lee Gordon, NortekUSA LLC

% Hs is 4(standard deviation inside wave band).
% The other parameters are computed at the peak of the spectrm 
% if the peak is at either end of the spectrum, the values are returned 
%   as nans.
% The peak frequency is interpolated using a parabolic fit to the shape of 
%   the peak.
% The peak direction and spreading are taken from the peak frequency band.
%
% There is nothing particularly intelligent about how these parameters 
% are computed. There are many ways to compute these or equivalent 
% integral parameters. Also, you may obtain much more stable estimates 
% of both direction and spreading by identifying wave "events" with 
% stationary wve parameters, and averaging over both frequency and time 
% within these events. 

[nf,nt]=size(Sp);

Hs=max(cumsum(Sp.*(dF*ones(1,nt)))).^.5*4;
Fp=nan*Hs;
Dp=nan*Hs;
Sprp=nan*Hs;

[B,nP]=max(Sp);
aa=find((nP~=1)&(nP~=nf));
A=nan*B;C=nan*B;
A(aa)=Sp(sub2ind(size(Sp),nP(aa)-1,aa));
C(aa)=Sp(sub2ind(size(Sp),nP(aa)+1,aa));

aa=find(not(isnan(sum([A; B; C]))));
if length(aa)>0;
  peakF(aa) = (log(F(nP(aa)+1))-log(F(nP(aa)-1)))'.*(-(C(aa)-A(aa))...
     ./(2.0*(A(aa)-2*B(aa)+C(aa))))/2;
  peakF(aa)=exp(log(F(nP(aa)))'+peakF(aa));
  peakDir(aa)=Dir(sub2ind(size(Dir),nP(aa),aa));
  peakSpread(aa)=Spread(sub2ind(size(Spread),nP(aa),aa));
  end;


