function eta=makerandomsea(eamp,wns,dirs,xx,yy)

%makerandomsea: make random sea surface elevation
%
%[eta] = makerandomsea(eamp,wns,dirs,xx,yy)
%
%outputs:
%eta     output surface elevation
%
%inputs:
%eamp    matrix of the component amplitudes [i,j] with i'th frequency component and j'th directional component
%wns     column vector of component wavenumbers in m-1. These must correspond to the frequency bins in input eamp
%dirs    row vector of component directions in rads. These must correspond to the directional bins in input eamp
%xx      row vector of x direction grid spacing
%yy      column vector of y direction grid spacing
%
%amplitude matrix must be [m by n] where m is number of wavenumber 
%components in wns and n is number of directional components in dirs
%


disp('calculating sea surface elevation');

nx=size(xx,2);
ny=size(yy,2);

randphase=8*atan(1.0)*rand(size(eamp));

for i=1:nx
   for j=1:ny
      eta(i,j)=sum(sum(eamp.*cos(xx(i)*wns*cos(dirs)+yy(j)*wns*sin(dirs)+randphase)));
   end
end


