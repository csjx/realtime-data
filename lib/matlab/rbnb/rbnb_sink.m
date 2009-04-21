% ---------------------------------------------------------------------------
% V2.1
% rbnb_sink
%
% Description:	Create Sink object, connect to RBNB server
%
% Usage:    	sink = rbnb_sink(server, snkname)
% 
% Where:
%   server:     Name of server to connect (e.g. 'localhost')
%   snkname:    Name to identify this sink (e.g. 'mySink')
%
% Output:
%   sink:       RBNB Sink object
%
% ---------------------------------------------------------------------------

function sink = rbnb_sink(server, snkname, varargin)

    sink = javaObject('com.rbnb.sapi.Sink', varargin{:});
    sink.OpenRBNBConnection(server, snkname);
return
    
    
