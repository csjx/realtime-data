function EA=EIRangeCorrection_2007(EI,Npings,Nbins,Binsize);
%Range corrected echo amplitude for slant beams
% Input: EI (echo intensity from a slant beam)
%    Npings, Nbins: number of pings and bins in the EA matrix
%    Binsize: bin size (vertical distance in m)
% Output: EA (range corrected echo amplitude for the beam)	
 
alpha=0.41;     %attenuation factor (sound absorption coefficient)
% for 1200 hz Workhorse.  Typical value: K.L.Deines
% 'Backscatter Estimation Using Broadband Acoustic Doppler Current
% Profilers', IEEE 1999
% modified alpha was 0.48; now 0.41 for warmer water  (JW 3/07)
ASF=0.45;  %amplitude scale factor given by Workhorse Commands 
% and Output Data Format, RDI Manual P/N957-6156-00 (March 2005)
% a property of ADCP: change in estimated signal strength divided
% by change in input signal
 
% bin length along slant beam is vert_distance/cos(20)
delR=Binsize/0.94;  
J=(1:Nbins)';	% vector of bin numbers
% radial-spreading correction and absorption-loss correction
RC= 20*log10(J*delR) + 2*alpha*delR*(J-1); 
% create matrix replicating vector RC 
RC = repmat(RC,1,Npings);
% range-correct echo amplitude by adding all corrections
EA = ASF*EI + RC;
%--------------------------------------------------------

% routine is an adaptation of code from unknown source used at ODU
% JWells  January 2007