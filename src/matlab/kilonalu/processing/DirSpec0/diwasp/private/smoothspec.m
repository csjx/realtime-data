function S=smoothspec(Sin,kernel)
% smooths a directional spectrum using the 
% first dimension is frequency
% kernel is 2*3 matrix with smoothing parameters

f1=kernel(1,3);
f2=kernel(1,2);
f3=kernel(1,1);
d1=kernel(2,3);
d2=kernel(2,2);
d3=kernel(2,1);
tot=2*f1+2*f2+f3+2*d1+2*d2+d3;

nf=size(Sin,1);
nd=size(Sin,2);

S=Sin;

S(3:nf-2,3:nd-2)=(f1*Sin(1:nf-4,3:nd-2)+f2*Sin(2:nf-3,3:nd-2)+f3*Sin(3:nf-2,3:nd-2)+f2*Sin(4:nf-1,3:nd-2)+f1*Sin(5:nf,3:nd-2)+...
   d1*Sin(3:nf-2,1:nd-4)+d2*Sin(3:nf-2,2:nd-3)+d3*Sin(3:nf-2,3:nd-2)+d2*Sin(3:nf-2,4:nd-1)+d1*Sin(3:nf-2,5:nd))./tot;


