function trm=velx(ffreqs,dirs,wns,z,depth)

Kz=cosh(z*wns)./sinh(depth*wns);
%include a maximum cuttoff for the velocity response function
Kz(find(Kz<0.1))=0.1;
Kz(find(isnan(Kz)))=1;
trm=(ffreqs.*Kz)*cos(dirs);