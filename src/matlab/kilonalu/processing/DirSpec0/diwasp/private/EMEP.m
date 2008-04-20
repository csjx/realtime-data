function [S]=EMEP(xps,trm,kx,Ss,pidirs,miter,displ)

szd=size(xps,1);
freqs=size(xps,3);
ddirs=size(trm,3);

ddir=abs(pidirs(2)-pidirs(1));
pi=4.0*atan(1.0);

if(displ<2)
   warning off;
end

Co=real(xps);
Quad=-imag(xps);

for ff=1:freqs
   
   xpsx(:,:,ff)=diag(xps(:,:,ff))*(diag(xps(:,:,ff))');
   sigCo(:,:,ff)=sqrt(0.5*(xpsx(:,:,ff)+Co(:,:,ff).^2-Quad(:,:,ff).^2));
   sigQuad(:,:,ff)=sqrt(0.5*(xpsx(:,:,ff)-Co(:,:,ff).^2+Quad(:,:,ff).^2));
end


for ff=1:freqs
   
index=0;
for m=1:szd
   for n=m:szd
      expx(1:ddirs)=exp(-i*kx(m,n,ff,1:ddirs));
      Hh(1:ddirs)=trm(m,ff,1:ddirs);
      Hhs(1:ddirs)=conj(trm(n,ff,1:ddirs));
      Htemp=(Hh.*Hhs.*expx);
      	
         
      if(Htemp(1)~=Htemp(2))
      index=index+1;
      phi(index,ff)=real(xps(m,n,ff))./(sigCo(m,n,ff)*Ss(1,ff));
      H(1:ddirs,index,ff)=real(Htemp)./sigCo(m,n,ff);

         if(kx(m,n,1,1)+kx(m,n,1,2)~=0)
            index=index+1;
         	phi(index,ff)=imag(xps(m,n,ff))./(sigQuad(m,n,ff)*Ss(1,ff));
         	H(1:ddirs,index,ff)=imag(Htemp)./sigQuad(m,n,ff);
      	end
      end
     end
  end
  end
  M=index;
   
      
  for eni=1:M/2+1
    cosnt(1:ddirs,1:M,eni)=cos(eni*pidirs')*ones(1,M);
    sinnt(1:ddirs,1:M,eni)=sin(eni*pidirs')*ones(1,M);
  end
 
  cosn=cos([1:M/2+1]'*pidirs);
  sinn=sin([1:M/2+1]'*pidirs);
  
   
  for ff=1:freqs
     if (displ>=1) 
        disp(['calculating for frequency' blanks(1) num2str(ff) ' of' blanks(1) num2str(freqs)]);
     end
   Hi(1:ddirs,1:M)=H(1:ddirs,1:M,ff);   
   Phione=(ones(size(pidirs'))*phi(1:M,ff)');  
     
   keepgoing=1;
   n=0;
   AIC=[];
   
   
   while(keepgoing==1)
      n=n+1;
      
      if(n<=M/2+1)
      if(displ>0)
           disp(strcat('model :',num2str(n)));
      end
      a1(1:n)=0.0;
      b1(1:n)=0.0;
      
      a2(1:n)=100.0;
      b2(1:n)=100.0;
      
      count=0;
      rlx=1.0;	
      while(max(abs(a2(1:n)))>0.01 | max(abs(b2(1:n)))>0.01)
         count=count+1;
         Fn=(a1(1:n)*cosn(1:n,:)+b1(1:n)*sinn(1:n,:))';
               
         Fnexp=exp(Fn)*ones(1,M);
         PhiHF=(Phione-Hi).*Fnexp;
         Z(1:M)=sum(PhiHF)./sum(Fnexp);
         for eni=1:n
            X(eni,1:M)=Z.*(...
               ( sum(Fnexp.*cosnt(:,:,eni))./sum(Fnexp) ) -...
               ( sum(PhiHF.*cosnt(:,:,eni))./sum(PhiHF) )...
               );
            Y(eni,1:M)=Z.*(...
               ( sum(Fnexp.*sinnt(:,:,eni))./sum(Fnexp) )-...
               ( sum(PhiHF.*sinnt(:,:,eni))./sum(PhiHF) )...
               );
         end
   		C(:,1:n)=(X(1:n,1:M))';
         C(:,n+1:2*n)=(Y(1:n,1:M))';
         
         out=C(:,1:n*2)\Z';
         out=out';
                  
         a2old=a2(1:n);
         b2old=b2(1:n);
         a2=out(1:n);
         b2=out(n+1:2*n);
         if sum((abs(a2)-abs(a2old))>100) | sum((abs(b2)-abs(b2old))>100 |count>miter)
            if(rlx>0.0625)
               rlx=rlx*0.5;
               if(displ==2)
                  disp(['relaxing computation...factor:' num2str(rlx,4)]);
               end

               count=0;
               a1(1:n)=0.0;
               b1(1:n)=0.0;
            else
               if(displ==2)
                  warning('computation fully relaxed..bailing out');
               end
            	keepgoing=0;
            	break;
            end
         else  
	         a1=a1(1:n)+rlx*a2;
   	      b1=b1(1:n)+rlx*b2;
         end
      end
      
      error=Z-a2(1:n)*X(1:n,:)-b2(1:n)*Y(1:n,:);
            
      AIC(n)=M*(log(2*pi*var(error))+1)+4*n+2;
      
      if(n>1)
           if((AIC(n)>AIC(n-1))| isnan(AIC(n)))
              keepgoing=0;
           end
      end
      
      
        	a1held(n,1:n)=a1(1:n);
         b1held(n,1:n)=b1(1:n);
         best=n;
         
         if~(keepgoing)
            if(n>1)
      		a1=a1held(n-1,1:n-1);
         	b1=b1held(n-1,1:n-1);
            best=n-1;
            else
            a1=0.0;
            b1=0.0;
            end
			end
      
   else
      keepgoing=0;
   end
     
end
   if(displ==2)
        disp(['best: ' num2str(best)]);
   end

   G=exp(a1*cosn(1:best,:)+b1*sinn(1:best,:))';
     
   SG=G/(sum(G)*ddir);        
           
	S(ff,1:ddirs)=Ss(1,ff)*SG';
   
  	end
   
warning on;