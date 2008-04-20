% This script loads a wave dataset and uses four functions to process 
% and display the data. 

load vdat;
%load vdatl;             % you can use this demo on either dataset

disp(readme);
disp(' ');
disp('The next plot shows spectra of the raw velocity and pressure data.');

input('Press any key...');

figure(1);
llfft(vu(:,1)+i*vv(:,1),0.5,'Velocity','(m/s)','Hz',70);
addllfft(vp(:,1),0.5,70,'r');
legend('Velocity spectrum','Pressure spectrum (m^2/Hz)',3);

disp('Next we will compute the wave directional spectra.');
input('Press any key...');

lf=.035;      %Hz - low frequency cutoff
maxfac=200;   %   - maximum value of factor scaling pressure to waves
minspec=0.1;  %m^2/Hz - minimum spectral level for computing
              %         direction and spreading
Ndir=2.3;     %deg - direction offset (includes compass error and 
              %      misalignment of cable probe relative to case
              % the offset for the Aquadopp Profiler is 0

parms=[lf maxfac minspec Ndir];

[Su,Sp,Dir,Spread,F,dF] = wds(vu,vv,vp,dt,100,hp,hv,parms);

disp(' ');
disp('We computed wave directional spectra using wds.m. The ');
disp('results are in the following arrays: ');
disp('  Sp, Su: surface wave spectra (m^2/Hz) based on pressure (Sp) ');
disp('          and velocity (Su) ');
disp('  Dir, Spread: direction (deg) and spreading (deg)');
disp('  F, dF: frequency and bandwidth at each frequency');
disp('   ');
disp('Look inside demo.m to see the parameters used to compute');
disp('these results.');
disp('   ');
disp('There is a spectrum for each of the 24 original time series.');
disp('The spectra have been cut off below 0.035 Hz and log-averaged, ');
disp('which means that the bandwidth at each frequency is ');
disp('proportional to the frequency (try the command: plot(F,dF)). ');
disp('The result is 38 frequency bands. ');
disp(' ');
disp('Next we will display the spectra; the match between the wave');
disp('spectra based on pressure and velocity is a sanity check on the ');
disp('depth parameters we used in the computation.');
disp(' ');

input('Press any key...');
plotwds(vt,Su,Sp,Dir,Spread,F,2);

disp('Now we will compute some integral parameters (Hs and ');
disp('frequency, direction and spreading at the peaks), then ');
disp('display them. ');
disp('     ');
input('Press any key...');

[Hs,peakF,peakDir,peakSpread] = hs(Su,Sp,Dir,Spread,F,dF);
ploths(vt,Hs,peakF,peakDir,peakSpread,4);



