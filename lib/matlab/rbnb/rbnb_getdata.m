% ---------------------------------------------------------------------------
% V2.1
% rbnb_getdata  
%
% Description:	Get single channel resulting from previous Fetch
%
% Usage:    	omap = rbnb_get(cmap, index)
% 
% Where:
%   cmap:       ChannelMap object (from Fetch)
%   index:      Channel index (0 to NumberOfChannels)
%
% Output:
%   dat:        data cast to proper Matlab type and shape
%
% ---------------------------------------------------------------------------

function dat = rbnb_getdata(cmap, idx)

    typ = cmap.GetType(idx);
    
% cast to appropriate datatype
    if     (typ == cmap.TYPE_FLOAT64) 
      dat = cmap.GetDataAsFloat64(idx)';     % transpose col to row vector
    elseif (typ == cmap.TYPE_STRING) 
      dat = char(cmap.GetDataAsString(idx));  % Java string to Matlab char-array
    elseif     (typ == cmap.TYPE_FLOAT32) 
      dat = cmap.GetDataAsFloat32(idx)';
    elseif     (typ == cmap.TYPE_INT32)
      dat = cmap.GetDataAsInt32(idx)';
    elseif     (typ == cmap.TYPE_INT16)
      dat = cmap.GetDataAsInt16(idx)';
    else
      dat = cmap.GetData(idx);               % everything else byte-array
    end    
 
return
    
  