function [xx,frac_nan] = nan_interp(x)
% nan_interp.m 11/10/99  Parker MacCready
% ***************************************
%      [xx,frac_nan] = nan_interp(x)
% ***************************************
% This interpolates over NaN's
% in a data vector "x" (column or row vectors OK)
% and returns the de-NaN'ed vector "xx" along with
% "frac_nan", the fraction of the original vector
% which was NaN's.
%
% This is designed to work fast compared with 
% reinterpolating the whole data vector.  It uses 
% linear interpolation but just in the places where
% it is needed.
%
% CAUTION: it pads with zeros, and later removes them.
% This will only affect the result if you have NaN's
% at either end of the vector.

[nr,nc] = size(x);
if (nc~=1 & nr~=1) | nr*nc==1
disp('Need to use a vector with nan_interp.');
return
end

% pad the start and end with zero
xx = x;
if nr==1
   xx = [0 xx];
   xx = [xx 0];
elseif nc==1
   xx = [0; xx];
   xx = [xx; 0];
end

% interpolate over NaN's
nanv = isnan(xx);         % a boolean vector of length N
frac_nan = sum(nanv) / length(xx);
%disp(['Number of NaNs = ',num2str(sum(nanv))]);
%disp(['NaN fraction = ',num2str(frac_nan)]);
dnanv = diff(nanv); % a vector of length N-1, all zeros except
                                                 % = 1 where we first encounter a NaN
                                                 % = -1 where we last encounter a NaN
                                                 % in any string of NaN's in the list
% now find the indices just before and after the NaN strings
prenan = find(dnanv == 1);
%disp(['Number of NaN strings = ',num2str(length(prenan))]);
postnan = find(dnanv == -1) + 1;
% interpolate over all the gaps
for gg = 1:length(prenan)
xx(prenan(gg):postnan(gg)) = linspace(xx(prenan(gg)),xx(postnan(gg)), ...
         (postnan(gg)-prenan(gg))+1);
end

% drop the first and the last points
xx(1) = [];
xx(end) = [];



