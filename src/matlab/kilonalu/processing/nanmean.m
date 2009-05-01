
function y = nanmean(x,dim);
% Calculate mean of matrix x, ignoring NaN's
% dim = 1 - calculate vertical mean
% dim = 2 - calculate horizontal mean
% GP - Nov. 2000 from R. Pawlowicz code

if nargin == 1
    dim = 1;
end

if 0 
switch dim
case 1
   for i = 1:size(x,2)
      xx = x(find(isnan(x(:,i))==0),i);
      if isempty(xx)
         y(i) = NaN;
      else
         y(i) = mean(xx);
      end
   end
case 2
   for i = 1:size(x,1)
      xx = x(i,find(isnan(x(i,:))==0));
      if isempty(xx)
         y(i) = NaN;
      else
         y(i) = mean(xx);
      end
   end
   y = y';
end
end


kk=isfinite(x);
x(~kk)=0;

ndat=sum(kk,dim);
indat=ndat==0;
ndat(indat)=1; % If there are no good data then it doesn't matter what
                 % we average by - and this avoid div-by-zero warnings.

y = sum(x,dim)./ndat;
y(indat)=NaN;