function trm=pres(ffreqs,dirs,wns,z,depth)

Kz=cosh(z*wns)./cosh(depth*wns);
%include a maximum cuttoff for the pressure response function
Kz(find(Kz<0.1))=0.1;
Kz(find(isnan(Kz)))=1;
trm=Kz*ones(size(dirs));

