%DIWASP Version 1.1 help file
%This describes the three structures used in DIWASP functions
%
%The input data structure(ID) contains fields:
% ID.data          measured wave data matrix - data in columns, one column per sensor
% ID.layout        layout of the sensors - x,y,z in each column. 
%                    x and y from arbitrary origin and z measured upwards from seabed (m)
% ID.datatypes     sensor type. Enter as cell list: e.g. {'elev' 'pres'}. Currently supported:
%                   'elev' 	surface elevation
%                   'pres'	pressure
%                   'velx'	x component velocity
%                   'vely'	y component velocity
%                   'velz'	z component velocity
%                   'vels'	vertical velocity of surface
%				    'accs'	vertical acceleration of surface
%				    'slpx'	x component surface slope
%				    'slpy'	y component surface slope
% ID.depth         mean overall depth of measurement area (m)
% ID.fs            sampling frequency of instruments - must be single figure for all (Hz)
%
%The spectral matrix structure(SM) contains fields:
% SM.freqs		 vector of length nf defining the bin centres of the spectral matrix frequency axis
% SM.dirs	     vector of length nd defining the bin centres of the spectral matrix direction axis
% SM.S			 matrix of size [nf,nd] containing the actual spectral density
% SM.xaxisdir	 compass direction of the x axis.
%
%The estimation parameter(EP) structure contains the fields:
% EP.method	   estimation method used. Currently supported:                  
%                	'DFTM'	Direct Fourier transform method
%                   'EMLM'	Extended maximum likelihood method
%                   'IMLM'   Iterated maximum likelihood method
%                   'EMEP'  Extended maximum entropy principle
%                   'BDM'    Bayesian direct method
% EP.nfft		number of DFTs used to calculate the frequency spectra: frequency resolution is (EP.nfft)/2
% EP.dres		directional resolution of calculation itself specified as the number of directional bins 
%                  which cover the whole circle. Note that the actual output resolution is determined by SM.dirs
% EP.iter		number of iterations: this has various effects for different methods
% EP.smooth	    smoothing applied: 'ON' or 'OFF'
%



%%Note that the name of the structures are arbitrary but the field names must not be changed.

%This file is only a help file