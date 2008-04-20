function [S]=IMLM(xps,trm,kx,Ss,pidirs,miter,displ)

gamma=0.1;
beta=1.0;
alpha=0.1;

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
   
   for m=1:szd
      for n=1:szd
    	H(1:ddirs,m,n)=trm(n,ff,1:ddirs);
      Hs(1:ddirs,m,n)=conj(trm(m,ff,1:ddirs));
      expx(1:ddirs,m,n)=exp(i*kx(m,n,ff,1:ddirs));
      iexpx(1:ddirs,m,n)=exp(-i*kx(m,n,ff,1:ddirs));
      Htemp(:,m,n)=H(:,m,n).*Hs(:,m,n).*expx(:,m,n);
      iHtemp(:,m,n)=H(:,m,n).*Hs(:,m,n).*iexpx(:,m,n);
   end
   end
   
   
   invcps=inv(xps(:,:,ff));
   Sftmp=zeros(ddirs,1);
   for m=1:szd
      for n=1:szd
         	xtemp=invcps(m,n)*Htemp(:,m,n);
   			Sftmp(:)=Sftmp(:)+xtemp;
	  	end
   end
   Eo=(1./Sftmp(:));
   kappa=1./(ddir*sum(Eo));
   Eo=kappa*Eo;
   E=Eo;
   T=Eo;

   for it=1:miter   
      for m=1:szd
      	for n=1:szd
         	expG(m,n,:)=iHtemp(:,m,n).*E(:);   
   			ixps(m,n)=sum(expG(m,n,:))*ddir;
	   	end
      end
      invcps=inv(ixps);
      Sftmp=zeros(ddirs,1);
      for m=1:szd
      	for n=1:szd
         	xtemp=invcps(m,n)*Htemp(:,m,n);
   			Sftmp(:)=Sftmp(:)+xtemp;
	  		end
        end
        Told=T;
      T=(1./Sftmp(:));
      kappa=1./(ddir*sum(T));
      T=kappa*T;
      
      %lambda=ones(size(T))-(T./Eo)
      %ei=gamma*lambda.*E;
      ei=gamma*((Eo-T)+alpha*(T-Told));
      E=E+ei;
      kappa=1./(ddir*sum(E));
   	E=kappa*E;

            
   end
         
   S(ff,:)=Ss(1,ff)*E';
end

warning on;



