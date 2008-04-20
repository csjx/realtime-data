function [S]=BDM(xps,trm,kx,Ss,pidirs,miter,displ)

nmod=6;

szd=size(xps,1);
freqs=size(xps,3);
ddirs=size(trm,3);

ddir=abs(pidirs(2)-pidirs(1));
pi=4.0*atan(1.0);

if(sum(kx)==0)
   warning('BDM method may not work with three quantity measurements');
   disp(' ');
end

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

  k=ddirs;
  
  dd=diag(ones(k,1))+diag(-2*ones(k-1,1),-1)+diag(ones(k-2,1),-2);
  dd(1,k-1:k)=[1 -2];
  dd(2,k)=1;
   
  
  for ff=1:freqs
     if(displ>0)
        disp(['calculating for frequency' blanks(1) num2str(ff) ' of' blanks(1) num2str(freqs)]);
     end
     a(1:k,1:M)=H(1:k,1:M,ff)*ddir;
     A=a';
     B=phi(:,ff);
     n=0;
     keepgoing=1;
     
     while(keepgoing)
        n=n+1;
        if(displ>0)
           disp(strcat('model :',num2str(n)));
        end
        if(n<=nmod)
        u=0.5^n;
        x(1:k,1)=log(1/(2*pi))*ones(k,1);
        stddiff=1;
        rlx=1.0;
        count=0;
        while(stddiff>0.001);
           count=count+1;
           F=exp(x);
           E=diag(F);
           A2=A*E;
           B2=B-A*F+A*E*x;
           Z(1:M,1:k)=A2;
           Z(M+1:M+k,1:k)=u*dd;
           Z(1:M,k+1)=B2;
           Z(M+1:M+k,k+1)=zeros(k,1);
           
           [Q,U]=qr(Z);
           
           UZ=U;
           TA=UZ(1:k,1:k);
           Tb=UZ(1:k,k+1);
           
           x1=TA\Tb;
           stddiff=std(x-x1);
           x=(1-rlx)*x+rlx*x1;
           if(count>miter|sum(isfinite(x))~=k)
              if(rlx>0.0625)
               rlx=rlx*0.5;
               if(displ==2)
                  disp(['relaxing computation...factor:' num2str(rlx,4)]);
               end
               if(sum(isfinite(x))~=k)
                  x(1:k,1)=log(1/(2*pi))*ones(k,1);
               end
               count=0;
            else
               if(displ==2)
                  warning('computation fully relaxed..bailing out');
               end
               if(n>1)
                  keepgoing=0;
               end
            	break;
            end
           end
        end
        sig2=((norm(A2*x-B2)).^2+(u*norm(dd*x)).^2)/M;
        ABIC(n)=M*(log(2*pi*sig2)+1)-k*log(u*u)+sum(log(diag(TA).^2));
        
        if(n>1)
        if(ABIC(n)>ABIC(n-1))
           keepgoing=0;
           n=n-1;
        end
     end
     
      if(keepgoing)
            xold=x;
      else
            x=xold;
      end

		else
   		keepgoing=0;
   	end
    
   end
   if(displ==2)
      disp(['best: ' num2str(n)]);
   end
   G=exp(x);        
   S(ff,1:k)=Ss(1,ff)*G';
end

warning on;

        
           
