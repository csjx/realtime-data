function [SM]=readspec(filename)

%DIWASP V1.1 function
%readspec: reads in spectrum file in DIWASP format and displays surface plot
%
%[SM]=readspec(filename)
%
%Outputs:
%SM   		A spectral matrix structure containing the file data
%
%Inputs:
%filename	filename for the file in DIWASP format including file extension
%
%"help data_structures" for information on the DIWASP data structures

%Copyright (C) 2002 Coastal Oceanography Group, CWR, UWA, Perth
% Mod by G. Pawlak 8/20/04 to suppress plotting

datain=load(filename); 

SM.xaxisdir=datain(1);
nfreq=datain(2);
ndirs=datain(3);
SM.freqs=datain(4:nfreq+3);
SM.dirs=datain(nfreq+4:nfreq+3+ndirs);
headercheck=datain(nfreq+ndirs+4);
if headercheck~=999
   error('corrupt file header');
end
mat=datain(nfreq+ndirs+5:nfreq+ndirs+4+(nfreq*ndirs));

S=reshape(mat,ndirs,nfreq);
SM.S=S';

%plotspec(SM,1);
