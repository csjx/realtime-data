function [H,Tp,DTp,Dp]=infospec(SM)

%DIWASP V1.1 function
%infospec: calculates and displays information about a directional spectrum
%
%[Hsig,Tp,DTp,Dp]=infospec(SM)
%
%Outputs:
%Hsig		Signficant wave height (Hmo)
%Tp			Peak period
%DTp		Direction of spectral peak
%Dp			Dominant direction
%
%Inputs:
%SM   		A spectral matrix structure containing the file data
%
%Hsig is the significant wave height. Tp is the peak frequency, the highest point in the one dimensional spectrum. 
%DTp is the main direction of the peak period (i.e the highest point in the two-dimensional directional spectrum). 
%Dp is the dominant direction defined as the direction with the highest energy integrated over all frequencies.
%
%"help data_structures" for information on the DIWASP data structures

%Copyright (C) 2002 Coastal Oceanography Group, CWR, UWA, Perth

SM=check_data(SM,2);if isempty(SM) return;end;

H=HSig(SM);

S=sum(real(SM.S),2);

[P,I]=max(S);
Tp=1/(SM.freqs(I));
[P,I]=max(real(SM.S(I,:)));
DTp=SM.dirs(I);
[P,I]=max(real(sum(SM.S,1)));
Dp=SM.dirs(I);

disp(['Infospec::']);
disp(['Significant wave height (Hmo): ' num2str(H)]);
disp(['Peak period: ' num2str(Tp)]);
disp(['Direction of peak period: ' num2str(DTp) ' axis angle / ' num2str(compangle(DTp,SM.xaxisdir)) ' compass bearing']);
disp(['Dominant direction: ' num2str(Dp) ' axis angle / ' num2str(compangle(Dp,SM.xaxisdir)) ' compass bearing']);


function dirs=compangle(dirs,xaxisdir)
dirs=xaxisdir*ones(size(dirs))-dirs;
dirs=dirs+360*(dirs<0);
dirs=dirs-360*(dirs>360);