% ---------------------------------------------------------------------------
% V2.1
% rbnb_get      
%
% Description:	Simple single-channel get/fetch/extract data chan
%
% Usage:  [dat, tim, nam] = rbnb_get(snk, name, start, duration [,tref [,byframe]])
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
%	dat:	    Data from fetched channel
%   tim:        Time vector corresponding to data
%   nam:        Name of fetched channel
%
% ---------------------------------------------------------------------------

function [dat, tim, nam] = rbnb_get(snk, cname, start, duration, tref)

  if(nargin < 5) tref = 'absolute';
  end
  
  cmap = rbnb_cmap;         % new channelMap with one channel
  cmap.Add(cname);

  snk.Request(cmap, start, duration, tref);

  cmap = snk.Fetch(6000000);                  % 60 sec timeout
%  cmap.GetMime(0);      % silent check

  if(cmap.NumberOfChannels < 1)
    error('FAIL: rbnb_get failed to get channel');
    return;
  end

  dat = rbnb_getdata(cmap, 0);
  tim = cmap.GetTimes(0)';
  nam = cmap.GetName(0);
 
return 
