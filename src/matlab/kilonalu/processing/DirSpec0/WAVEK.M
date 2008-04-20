function K=wavek(F,H);
% function K=wavek(F,H);
% where K is wavenumber (rad/m)
%       F is frequency (Hz)
%       H is depth (m) 
%
% Copyright (C) 2001, Lee Gordon, NortekUSA LLC

g=9.80171;

% This routine use an approximate equation, then sharpens the result with
% one interpolation. The result is good to around 1 part in 10^-6.

% The equation came out of a textbook, but I have long since forgotton
% which one. If you know, please tell me! lgordon@nortekusa.com

e1=4*pi^2*F.^2.*H/g;        %f4 = omega^2 * h1/g
e2 = 1+0.6666666*e1 + 0.355555555*e1.^2 + 0.1608465608*e1.^3 + ...
     0.0632098765*e1.^4 + 0.0217540484*e1.^5 + 0.0065407983*e1.^6;
e3 = +e1.^2 + e1./e2;
K1=sqrt(e3)./H;

%compute error as basis for interpolation

o1=sqrt(g*K1.*tanh(K1.*H));
e1=o1.^2.*H/g;       
e2 = 1+0.6666666*e1 + 0.355555555*e1.^2 + 0.1608465608*e1.^3 + ...
     0.0632098765*e1.^4 + 0.0217540484*e1.^5 + 0.0065407983*e1.^6;
e3 = +e1.^2 + e1./e2;
K2=sqrt(e3)./H;

%interpolate
K=2*K1-K2;

