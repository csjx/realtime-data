%%%%%  tcdepths.m   
% JWells May 16, 2007
% a script to (a) adjust vertcal positions if tchain is
% tilted and (b) to interpolate data onto a finer vertical grid

%%% INPUT CONSTANTS: 

%%% (a) LENGTH OF TCHAIN -- "tclen" [m]
%%%     distance from sea floor to pressure sensor
%%%     tclen=8.165 m
%%% (b) position vector: "tcpos" [m]
%%%     Tnode spacing measured prior to deployment,
%%%     from top of thermistor probe to top of next probe.
%%%     Seafloor to tnode1 measured at deployment 5/10/07
%%%     tcpos=[0.85,1.85,2.85,3.84,4.84,5.84,6.84];
%%%     Distance from top tnode to P sensor = 1.325 m

%%% INPUT VARIABLES:
%%%     20 min (DT) averages of ADCP pressure "depADCP"
%%%     & tchain pressure "deptc"  (matched by times)
%%%     temperature vector

%%% CALCULATE
%%% (a) "tchgt"  (position of toop of tchain wrt to seafloor)
%%% (b) "tcMAB"  (corrected position of tnodes--in m above bottom)

tclen = 8.165;
tcpos = [0.85,1.85,2.85,3.84,4.84,5.84,6.84];
tcpos=tcpos';
% maxPdif =  7.2553;  % max difference betw ADCP and TC Pres
%                     % should indicate a ~straight up TChain
%                     % maxPdif 7.2553 from 68 hrs 5/11-14/07
%                     % (mean diff 7.2437 +- .0090 m)
maxPdif = 7.7923;   % ADCP moved 7/10/07.  New value based on 2+ days data                      
if ~isempty(adcpT) && ~isempty(adcpP)
    Ptim=find(adcpT>=tt-.1*DT/60/24 & adcpT<=tt+.1*DT/60/24);
    if ~isempty(Ptim)
        depADCP=adcpP(Ptim(1));
        if isfinite(depADCP) && isfinite(tcPlo)
            deptc=tcPlo;
            tctilt = deptc-(depADCP-maxPdif);
            tchgt = tclen-tctilt;
        else
            tchgt=tclen;
        end
    else
        tchgt=tclen;
    end
else
    tchgt=tclen;
end
tcMAB =(tchgt/tclen)*tcpos;
Tgrid = interp1(tcMAB,Temp,MABgrid);