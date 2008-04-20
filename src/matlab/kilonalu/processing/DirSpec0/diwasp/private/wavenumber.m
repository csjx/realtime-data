function [k]= wavenumber(sigma,h)
%k = wavenumber(sigma,h)
%
%k is the matrix of same size as sigma and h containing the calculated wave numbers
%
%sigma is the wave frequencies in rad/s
%h is the water depth
%
%sigma and h must be scalars,vectors or matricies of the same dimensions
%

%modified from R.Dalrymple's java code
%

g=9.81;

 	a0=(sigma.*sigma.*h)./g;
   b1=1.0./(tanh(a0.^0.75));
   a1=a0.*(b1.^0.666);
   da1=1000.0;
   
d1=ones(size(h));
 while(max(d1)==1)  
 d1 = (abs(da1./a1) > .00000001);
	th=tanh(a1);
	ch=cosh(a1);
	f1=a0-(a1.*th);
	f2= - a1.*((1.0./ch).^2) -th;
	da1= -f1./f2;
	a1=a1+da1;
 end

k=a1./h;
   
  