% ---------------------------------------------------------------------------
% rbnb_source
%
% Description:	Create Source object, connect to RBNB server
%
% Usage:    	src = rbnb_source(server, srcname [, cacheSize, arcMode, arcSize])
% 
% Where:
%   server:     Name of server to connect (e.g. 'localhost')
%   srcname:    Name to identify this source (e.g. 'mySource')
%	cacheSize:  Number of frames in cache (default 100)
%   arcMode:    Archive mode, choices:  
%               'none'   - no archive
%               'create' - create new archive (delete existing)
%               'append' - create of add to existing archive
%               'load'   - load existing archive
%
%	arcSize:    Number of frames in archive (default 0) 
%
% Output:    
%   src:        Source object
%
% ---------------------------------------------------------------------------

function src = rbnb_source(server, srcname, varargin)

    src = javaObject('com.rbnb.sapi.Source', varargin{:});
    src.OpenRBNBConnection(server, srcname);
return


    