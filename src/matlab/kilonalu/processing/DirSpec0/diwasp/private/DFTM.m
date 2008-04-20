function [S]=DFTM(xps,trm,kx,Ss,W,miter,displ)

szd=size(xps,1);
ffreqs=size(xps,3);
ddirs=size(trm,3);
ddir=8*atan(1.0)/ddirs;

if(displ<2)
   warning off;
end

for ff=1:ffreqs
   if(displ>=1)
      disp(['calculating for frequency' blanks(1) num2str(ff) ' of' blanks(1) num2str(ffreqs)]);
   end
      nxps=xps(:,:,ff);
   Sftmp=zeros(ddirs,1)+i*zeros(ddirs,1);
   for m=1:szd
      for n=1:szd
   
         H(1:ddirs)=trm(n,ff,1:ddirs);
   	  	Hs(1:ddirs)=conj(trm(m,ff,1:ddirs));
      	
         expx(1:ddirs)=exp(i*kx(m,n,ff,1:ddirs));
			xtemp=nxps(m,n).*H.*Hs.*expx;
			Sftmp(:)=Sftmp(:)+xtemp';

  
      end
   end
	E=Sftmp(:)';
   E=E./(ddir*sum(E));
   S(ff,:)=Ss(1,ff)*E;   
end

warning on;	