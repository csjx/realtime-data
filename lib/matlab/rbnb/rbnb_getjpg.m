% ---------------------------------------------------------------------------
% V3.1B
% rbnb_getjpg
% Paul Hubbard hubbard@sdsc.edu
% Feb 20 2008
%
% Description:	Simple single-channel get/fetch/extract JPEG from video or image channel
%
% Uses rbnb_get to pull the raw data, and then converts the result into a Matlab image object.
%
% Usage:  [img, tim, nam] = rbnb_getjpg(snk, name, start, duration [,tref])
% 
% Where:
%   snk:        Sink connection object (from rbnb_sink)
%   name:       Name of object to get
%   start:      Start time
%   duration:   Time duration
%   tref:       Optional start time reference:
%                   'absolute'
%                   'oldest'
%                   'newest' (default)
% Output:
%	img:		Image, as returned from imread
%   tim:        Time vector corresponding to data
%   nam:        Name of fetched channel
%
% ---------------------------------------------------------------------------

function [img, tim, nam] = rbnb_getjpg(snk, cname, start, duration, tref)

    % If optional arg not used, set to default
	if(nargin < 5)
		tref = 'newest';
	end
	
	% Pull the image, comes back as an array of int8 bytes
	[myPic rtim rname] = rbnb_get(snk, cname, start, duration, tref);

	% Generate a temporary file with a hopefully-unique name. We save it for reload below.
	mytmp = tempname;
	
	% We have to use the filesystem to bypass Matlabs strong type system. Annoying, this.
	fh = fopen(mytmp, 'w');
	
	% Write data to file as untouched binary, no translation
	fwrite(fh, myPic, 'int8');
	fclose(fh);

	% Now load that file back in, *as an image*
	img = imread(mytmp);
    
    % A bit of perhaps-unnecessary housecleaning
	clear myPic;
    delete(mytmp);
		
    % Marshall return values
	tim = rtim;
	nam = rname;	
	
return