% ---------------------------------------------------------------------------
% V2.1
% rbnb_put
%
% Description:	Put data into an RBNB for specified channel(s) and time.
%
% Usage:    	rbnb_put(src, name, data [, start, duration])
% 
% Where:
%   src:        Source connection object (from rbnb_open)
%   name:       Name of channel to put
%	data:  	    data vector
%	start:      start timestamp (default = 0)
%   duration:   duration of data (default = 0)
%
% Output:
%	nput:       1 if success, 0 if failure
%
% ---------------------------------------------------------------------------

function nput = rbnb_put(src, cnam, cdat, start, duration)

    if (nargin < 4) start = 0;
    end
    if (nargin < 5) duration = 0;
    end
    
    cmap = rbnb_cmap;
    ix = cmap.Add(cnam);
    cmap.PutTime(start, duration);
    
    if     (isa(cdat,'double')) cmap.PutDataAsFloat64(ix, cdat);
    elseif (isa(cdat,'char'))   cmap.PutDataAsString (ix, cdat);      
    else                        cmap.PutDataAsInt8   (ix, cdat);    
    end
    
    nput = src.Flush(cmap, 1);
  
  return
   

    