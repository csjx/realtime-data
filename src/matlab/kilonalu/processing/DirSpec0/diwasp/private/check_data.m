function DDS=check_data(DDS,type)
% internal DIWASP1.1 function
% checks data structures
%
%   DDS=check_data(DDS,type)
%       DDS is the data structure
%       type:1, Instrument data structure 2, Spectral matrix structure 3, Estimation parameters structure


%defaults:
SM.xaxisdir=90;
EP.dres=180;EP.nfft=[];EP.method='EMEP';EP.iter=100;



switch type
%Instrument data structure
case 1
    if ~isstruct(DDS)
        disp('DIWASP data_check: Instrument data type is not a structure');
    end
    error='';nc=1;
    if ( isfield(DDS,'layout') )
        [nr,nc]=size(getfield(DDS,'layout'));
        if nr<3
            if nr==2 DDS.layout(3,:)=0;
                ;else;error='layout';end
        ;end;else;error='layout';end
    if ( isfield(DDS,'datatypes') ) & all(size(getfield(DDS,'datatypes'))==[1 nc])==1 
        ;else;error='datatypes';end
    if ( isfield(DDS,'depth') ) & length(getfield(DDS,'depth'))==1 
        ;else;error='depth';end
    if ( isfield(DDS,'fs') ) & length(getfield(DDS,'fs'))==1 
        ;else;error='fs';end
    
    if ( isfield(DDS,'data') )
        if size(getfield(DDS,'data'),2)==nc; ;else;error='data';end
    else
        DDS.data=zeros(1,nc);
    end
    
    if length(error)>0
        disp(['Instrument data structure error: ' error ' not specified correctly']);
        DDS=[];return;
    end
%Spectral matrix    
case 2
    if ~isstruct(DDS)
        disp('DIWASP data_check: Spectral matrix data type is not a structure');
    end
    error='';
    if ( isfield(DDS,'freqs') )& min(size(getfield(DDS,'freqs')))==1
        nf=length(DDS.freqs);
        ;else;error='freqs';end
    if ( isfield(DDS,'dirs') )& min(size(getfield(DDS,'dirs')))==1
        nd=length(DDS.dirs);
        ;else;error='dirs';end
    if ( isfield(DDS,'S') )
        if ~((size(DDS.S,1)==nf & size(DDS.S,2)==nd) | isempty(DDS.S))
        error='S';end
        ;else;DDS.S=[];end
    if ( isfield(DDS,'xaxisdir') )
        if ~length(DDS.xaxisdir)==1 
        error='xaxisdir';end
        ;else;DDS.xaxisdir=SM.xaxisdir;end
        
    if length(error)>0
        disp(['Spectral matrix structure error: ' error ' not specified correctly']);
        DDS=[];return;
    end
    
%Estimation parameters    
case 3
    if ~isstruct(DDS)
        disp('DIWASP data_check: Estimation parameter data type is not a structure');
    end
    error='';
    if ( isfield(DDS,'dres') )
        if ~length(DDS.dres)==1 
            error='dres';
        elseif DDS.dres<10
            DDS.dres=10;
        end
        ;else;DDS.dres=EP.dres;end
    if ( isfield(DDS,'nfft') )
        if ~length(DDS.nfft)==1 
            error='nfft';
        elseif DDS.nfft<64
            DDS.nfft=64;
        end
        ;else;DDS.nfft=EP.nfft;end
    if ( isfield(DDS,'iter') )
        if ~length(DDS.iter)==1 
        error='iter';end
        ;else;DDS.iter=EP.iter;end
    if ( isfield(DDS,'smooth') )
        if ~strcmp(DDS.smooth,'OFF') 
        DDS.smooth='ON';end
        ;else;DDS.smooth='ON';end
    if ( isfield(DDS,'method') )
        if ~(any(strcmp(DDS.method,{'DFTM','EMLM','IMLM','EMEP','BDM'}))) 
        error='method';end
        ;else;DDS.method=EP.method;end
    
    if length(error)>0
        disp(['Estimation parameters structure error: ' error ' not specified correctly']);
        DDS=[];return;
    end
    
otherwise
    warning('DIWASP data_check: Data type unknown');
    DDS=[];
end
    
   