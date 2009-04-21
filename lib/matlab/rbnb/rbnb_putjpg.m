% ---------------------------------------------------------------------------
% V3.1B
% rbnb_putjpg
% Paul Hubbard hubbard@sdsc.edu
% Feb 20 2008
%
% Description:	Put a JPEG image into the DataTurbine
%
% Usage:  rbnb_putjpg(src, name, timestamp, img, [userinfo [,mime [,duration [,tref]]]])
% 
% Where:
%   src:        Source connection object (from rbnb_source)
%   name:       Name of object to put
%   start:      Start time
%   img:		MATLAB image to write
%   userinfo:   Static metadata for PutUserInfo
%   mime:		Static MIME type (default is 'image/jpeg')
%   duration:   Time duration
%   tref:       Optional start time reference:
%                   'absolute'
%                   'oldest'
%                   'newest' (default)
%   
% Output:
%	nput:       1 if success, 0 if failure
%
% ---------------------------------------------------------------------------

function nput = rbnb_putjpg(src, cname, timestamp, img, userinfo, mime, duration, tref)

	% Assign defaults if necessary
	if (nargin < 5) 
        userinfo = '';
	end
	
	if(nargin < 6) 
        mime = 'image/jpeg';
	end
	
	if(nargin < 7) 
        duration = 0.0;
	end;
	
	if(nargin < 8) 
        tref = 'newest';
	end
	
	% Convert image into array of bytes by funneling into a temporary file and
	% reading it back as a bytestream of int8s.
	% Required due to inflexible typing system in Matlab.
	mytmp = tempname;
	imwrite(img, mytmp, 'jpeg');
	fh = fopen(mytmp, 'r');
	bytestream = fread(fh, 'int8');
	fclose(fh);
    delete(mytmp);
	
	bytestream = int8(bytestream);	
	
	% Create and populate a channel map
	cmap = rbnb_cmap;
	ix = cmap.Add(cname);
	
	cmap.PutTime(timestamp, duration);  
    % TODO: Let user specify TimeAuto - how?
    %cmap.PutTimeAuto('timeofday');
	
	% Add user metadata if present
	if(userinfo ~= '')
		cmap.PutUserInfo(ix, userinfo);
	end
	
	% Always want MIME type, used by RDV and other smart clients
	cmap.PutMime(ix, mime);
	
    cmap.PutDataAsByteArray(ix, bytestream);
    
	nput = src.Flush(cmap, 1);
			
return