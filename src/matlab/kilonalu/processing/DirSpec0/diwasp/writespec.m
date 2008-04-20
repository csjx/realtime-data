function writespec(SM,filename)

%DIWASP V1.1 function
%writespec: writes spectrum matrix to file using DIWASP format
%
%writespec(SM,filename)
%
%Inputs:
%SM   		A spectral matrix structure
%filename	String containing the filename including file extension if required
%
%All inputs required
%
%"help data_structures" for information on the DIWASP data structures

%Copyright (C) 2002 Coastal Oceanography Group, CWR, UWA, Perth


SM=check_data(SM,2);if isempty(SM) return;end;

nf=max(size(SM.freqs));nd=max(size(SM.dirs));

streamout(1)=SM.xaxisdir;   
streamout(2)=nf;
streamout(3)=nd;
streamout(4:nf+3)=SM.freqs;
streamout(nf+4:nf+nd+3)=SM.dirs;
streamout(nf+nd+4)=999;
streamout(nf+nd+5:nf+nd+4+(nf*nd))=reshape(real(SM.S'),nf*nd,1);

streamout=streamout';

eval(['save ',filename,' streamout -ASCII']);


