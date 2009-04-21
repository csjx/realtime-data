% ---------------------------------------------------------------------------
% rbnb_cmap
%
% Description:	Create new RBNB ChannelMap object
%
% Usage:    	cmap = rbnb_cmap
% 
% Output:    
%   src:        ChannelMap object
%
% ---------------------------------------------------------------------------

function cmap = rbnb_cmap(varargin)

    cmap = javaObject('com.rbnb.sapi.ChannelMap', varargin{:});

return
    