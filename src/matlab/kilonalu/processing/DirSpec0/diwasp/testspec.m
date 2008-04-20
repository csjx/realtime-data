function EP=testspec(ID,theta,spread,weights,EP);

%DIWASP V1.1 function
%testspec: Testing routine for directional wave spectrum estimation methods
%
%[EPout] = testspec(ID,theta,spread,weights,EP)
%
%Outputs:
%EPout   		The estimation parameters structure used in the test.
%
%Inputs:
%ID				An instrument data structure containing the measured data. The ID.data field is ignored.
%theta       	vector with the mean directions of a sea state component
%spread    		vector with the spreading parameters of a sea state component 
%weights  		vector with relative weights of sea state components
%EP   			The estimation parameters structure with the values under test used. Default settings are used where not specified. 
%
%All inputs are required
%
%Testspec details:
%
%The fields ID.layout and ID.datatypes and ID.depth are used to specify the arrangement of the imaginary sensors.
%
%The function outputs a plot of the specified spreading function (solid line) and the estimated spreading shape (dotted line). 
%
%The calculation is carried out for a frequency of 0.2 Hz. 
%The inputs theta, spread and weights determine the shape of the directional spreading function. 
%Each of these inputs is a vector of length n where n is the number of sea state components. 
%Each sea state component has a mean direction and a spreading parameter.
%The directional spreading is calculated with a cosine power function (Mitsuyasu et al.1975 J.Phys Oceanogr.5,750-760)
%
%"help data_structures" for information on the DIWASP data structures
%
%All of the implemented calculation algorithms are as described by:
%Hashimoto,N. 1997 "Analysis of the directional wave spectrum from field data" 
%In: Advances in Coastal Engineering Vol.3. Ed:Liu,P.L-F. Pub:World Scientific,Singapore
%
%Copyright (C) 2002 Coastal Oceanography Group, CWR, UWA, Perth

S=1;F=0.2;ddir=2*pi/EP.dres;ncom=size(theta,2);

ID=check_data(ID,1);if isempty(ID) return;end;
EP=check_data(EP,3);if isempty(EP) return;end;

szd=size(ID.layout,2);
szdt=size(ID.datatypes,2);

dirs=[-180:1:179];

disp(' ');disp('calculating.....');disp(' ');

disp('wavenumbers')
wns=wavenumber(2*pi*F,ID.depth);

dres=EP.dres;
pidirs=[-pi:(2*pi/dres):pi-(2*pi/dres)];

%generate spreading function
for ni=1:ncom
   GS(ni,:)=(cos(0.5*(pidirs-theta(ni)*(pi/180)*ones(size(pidirs))))).^(2*spread(ni));
   sumGS(ni)=sum(GS(ni,:));
end

sumweights=sum(weights);
weights=(1/sumweights)*weights;
coeff=(1./(sumGS*ddir)).*weights;

Gg=zeros(size(pidirs));
for ni=1:ncom
   Gg=Gg+coeff(ni)*GS(ni,:);
end

disp('transfer parameters');
for m=1:szd
	trm(m,:,:)=feval(ID.datatypes{m},2*pi*F,pidirs,wns,ID.layout(3,m),ID.depth);
 for n=1:szd
    kx(m,n,:,:)=wns*((ID.layout(1,n)-ID.layout(1,m))*cos(pidirs)+(ID.layout(2,n)-ID.layout(2,m))*sin(pidirs));
end
end

disp('cross power spectra');disp(' ');
for m=1:szd
   Ss(m,1)=S;
for n=1:szd
   expx(1:dres)=exp(-i*kx(m,n,1,1:dres));
   Hh(1:dres)=trm(m,1,1:dres);
   Hhs(1:dres)=conj(trm(n,1,1:dres));
   Htemp=(Hh.*Hhs.*expx);
   expG=Htemp.*Gg;   
   xps(m,n,1)=sum(expG)*ddir;
 end
end

   
% call appropriate estimation function
disp(['directional spectra using' blanks(1) EP.method ' method']);
disp(' ')
 Specout=feval(EP.method,xps,trm,kx,Ss,pidirs,200,2);
 
% map spectrum onto user defined direction vector
Dd=pidirs*(180/pi);

disp('finished...plotting spectrum');
fig=figure;
plot(Dd,real(Specout),'b.',Dd,Gg,'k');

nse=sum((real(Specout)-Gg).^2)./sum(Gg.^2);

T=['Directional spreading estimated using ' blanks(1) EP.method ' method' blanks(2) 'NSE:' num2str(nse,3)];
title(T);
ylabel('');
xlabel('direction [degrees]');
zlabel('m^2');


