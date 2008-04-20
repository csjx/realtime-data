function [ F, S, dF, Ns, Ne ] = logavg( f, s, n )
%
%  [ F, S, dF, Ns, Ne ] = logavg( f, s, n )
%
%  Logarithmically averages the input spectrum, s, defined over 
%  frequencies f into n uniformly-spaced "log10" frequency bands.
%  The averaged spectrum and frequencies are returned in S and F,
%
%  dF holds the bandwidth of each band
%  Ns and Ne contain the start and end indices of each band
%
%  If s is a matrix, averaging is performed over each column.
%
%  Copyright (C) 2001, Lee Gordon, NortekUSA LLC

% data must be in columns
f=f(:);
[nc,nt]=size(s);
if(nc~=length(f))
  'column length must be same as length of f'
  return;
  end;

                                  % some preliminaries

lf = log( f ) ;
dlf = 1.000000001*(lf(length(f)) - lf(1)) / n ;  % log frequency increment
NDX = 1 + floor((lf-lf(1))/dlf);
AA=[find(diff(NDX)>0)' length(f)];    % array of transitions plus final f

Cs=cumsum(s);
Cf=cumsum(f);
F=[Cf(AA(1)) diff(Cf(AA)')]./[AA(1) diff(AA)];
S=[Cs(AA(1),:); diff(Cs(AA,:))]./([AA(1) diff(AA)]'*ones(1,nt));

F=F';
dF=[AA(1) diff(AA)]*(f(10)-f(9));
AA=AA';
dF=dF';

Ns=[1 AA(1:length(AA)-1)'+1]';    %starting positions of each band
Ne=AA;                          %ending positions of each band


