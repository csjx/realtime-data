function [SM,ID]= makespec(freqlph,theta,spread,weights,Ho,ID,ndat,noise)

%DIWASP V1.1 function
%makespec: generates a directional wave spectra and pseudo data from the spectra
%
%[SM,IDout]=makespec(freqlph,theta,spread,weights,Ho,ID,ndat,noise)
%
%Outputs:
%SM		    A spectral matrix of the generated spectrum
%IDout   	Returns the input ID with data in field ID.data filled
%
%Inputs:
%freqlph 	3 component vector [l p h] containing:
%               the lowest frequency(l),peak, frequency(p) and highest frequency(h)
%theta      vector with the mean directions of a sea state component
%spread    	vector with the spreading parameters of a sea state component 
%weights  	vector with relative weights of sea state components
%Ho			RMS wave height for generated spectrum
%ID			Instrument data structure; field ID.data is ignored
%ndat       length of simulated data
%noise      level of simulated noise: 
%               Gaussian white noise added with variance of [noise*var(eta)]
%
%
%theta, spread and weights must all be the same length
%typical spreading parameter values are 25-100
%
%The generated spectrum is based on an TMA spectrum (Bouws et al. 1985 JGR 90 C1,975-986)
%with directional spreading calculated with a cosine power function (Mitsuyasu et al.1975 J.Phys Oceanogr.5,750-760)
%
%"help data_structures" for information on the DIWASP data structures

%Copyright (C) 2002 Coastal Oceanography Group, CWR, UWA, Perth

ID=check_data(ID,1);if isempty(ID) return;end;

%setup default values
dt=0.5;g=9.806;
siga=0.07;sigb=0.09;gamma=2;alpha=0.014;
nf=50;nd=60;

ncom=size(theta,2);
ns=size(ID.layout,2);

df=(freqlph(3)-freqlph(1))/nf;
ddir=2*pi/nd;

ffreqs=[freqlph(1):df:freqlph(3)]';
nf=size(ffreqs,1);
fpeak=freqlph(2)*ones(size(ffreqs));

flh=(ffreqs<=fpeak);
sigma=siga*flh-sigb*(flh-ones(size(flh)));

omgh=2*pi*ffreqs*(ID.depth/g)^0.5;
phi=zeros(size(omgh));
phi=phi+0.5*(omgh<=1).*omgh.^2;
phi=phi+(omgh>=2); 
phi=phi+(omgh>1 & omgh <2).*(ones(size(omgh))-0.5*(2*ones(size(omgh))-omgh).^2);

Ek=(alpha*g*g*(2*pi)^(-4.0))*phi.*(ffreqs.^(-5));
PhiPM=exp(-(5.0/4.0)*(ffreqs./fpeak).^(-4.0));
PhiJ=exp(log(gamma)*exp(-((ffreqs-fpeak).^2)./((2*sigma.^2).*fpeak.^2)));
ETMA=Ek.*PhiPM.*PhiJ;

dirs=[-pi:ddir:pi-ddir];
omg=2*pi*ffreqs;

for i=1:ncom
   GS(i,:)=(cos(0.5*(dirs-theta(i)*(pi/180)*ones(size(dirs))))).^(2*spread(i));
   sumGS(i)=sum(GS(i,:));
end

sumweights=sum(weights);
weights=(1/sumweights)*weights;
coeff=(1./sumGS).*weights;

Gg=zeros(size(dirs));
for i=1:ncom
   Gg=Gg+coeff(i)*GS(i,:);
end

spec=(ETMA*Gg);
fac=Ho/(sqrt(8*sum(sum(spec))*df*ddir*(180/pi)));
spec=fac*fac*spec;

SM.freqs=ffreqs;SM.dirs=(180/pi)*dirs;SM.S=spec;SM.xaxisdir=90;

disp('plotting spectrum...press any key to make data');
plotspec(SM,1);
drawnow;
pause

disp('writing spectrum matrix to file');
writespec(SM,'specmat.spec');

wns=wavenumber(omg,ID.depth*ones(size(omg)));
eamp=sqrt(2*df*ddir*(180/pi)*spec);

data=makewavedata(eamp,omg,wns,dirs,ID.layout,ID.datatypes,ID.depth,ID.fs,ndat);

for i=1:ns
   gsnoise=gsamp(0,noise*var(data(:,1)),ndat);
   data(:,i)=data(:,i)+gsnoise;
end

ID.data=data;

surfout = questdlg('Do you want to see a simulated sea surface?','DIWASP','Yes','No','No');	

if strcmp(surfout,'Yes')
xx=[1:2:50];
yy=[1:2:100];
surface=makerandomsea(eamp,wns,dirs,xx,yy);

[py,px]=meshgrid(yy,xx);

surf(px,py,surface);
axis equal;

end






