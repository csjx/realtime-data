function [adcp,cfg,nens]=rdradcp_short_v1(name,varargin);
% RDRADCP  Read (raw binary) RDI ADCP files, 
%  ADCP=RDRADCP(NAME) reads the raw binary RDI BB/Workhorse ADCP file NAME and
%  puts all the relevant configuration and measured data into a data structure 
%  ADCP (which is self-explanatory). This program is designed for handling data
%  recorded by moored instruments (primarily Workhorse-type but can also read
%  Broadband) and then downloaded post-deployment.
%
%  [ADCP,CFG]=RDRADCP(...) returns configuration data in a
%  separate data structure.
%
%  Various options can be specified on input:
%  [..]=RDRADCP(NAME,NUMAV) averages NUMAV ensembles together in the result.
%  [..]=RDRADCP(NAME,NUMAV,NENS) reads only NENS ensembles (-1 for all).
%  [..]=RDRADCP(NAME,NUMAV,[NFIRST NEND]) reads only the specified range
%   of ensembles. This is useful if you want to get rid of bad data before/after
%   the deployment period.
%
%  Notes- sometimes the ends of files are filled with garbage. In this case you may
%         have to rerun things explicitly specifying how many records to read (or the
%         last record to read). I don't handle bad data very well.
%
%       - chaining of files does not occur (i.e. read .000, .001, etc.). Sometimes
%         a ping is split between the end of one file and the beginning of another.
%         The only way to get this data is to concatentate the files, using
%           cat file1.000 file1.001 > file1   (unix)
%           copy file1.000/B+file2.001/B file3.000/B     (DOS/Windows)
%
% R. Pawlowicz (rich@eos.ubc.ca) - 17/09/99

% JWells 9/25/06  See original rdradcp for complete Pawlowicz notes.  This
% version is only for a moored instrument with beam coordinate system and
% (I think) no bottom tracking.

num_av=5;   % Block filtering and decimation parameter (# ensembles to block together).
nens=-1;   % Read all ensembles.
century=2000;  % ADCP clock does not have century prior to firmware 16.05.
vels='no';   % Default to simple averaging

lv=length(varargin);
if lv>=1 & ~isstr(varargin{1}),
  num_av=varargin{1}; % Block filtering and decimation parameter (# ensembles to block together).
  varargin(1)=[];
  lv=lv-1;
  if lv>=1 & ~isstr(varargin{1}),
    nens=varargin{1};
    varargin(1)=[];
    lv=lv-1;
  end;
end;

% Check file information first

naminfo=dir(name);

if isempty(naminfo),
  fprintf('ERROR******* Can''t find file %s\n',name);
  return;
end;

fprintf('\nOpening file %s\n\n',name);
fd=fopen(name,'r','ieee-le');

% Read first ensemble to initialize parameters

[ens,hdr,cfg,pos]=rd_buffer(fd,-2); % Initialize and read first two records
fseek(fd,pos,'bof');              % Rewind

if (cfg.prog_ver<16.05 & cfg.prog_ver>5.999) | cfg.prog_ver<5.55,
  fprintf('***** Assuming that the century begins year %d (info not in this firmware version) \n\n',century);
else
  century=0;  % century included in clock.  
end;

dats=datenum(century+ens.rtc(1,:),ens.rtc(2,:),ens.rtc(3,:),ens.rtc(4,:),ens.rtc(5,:),ens.rtc(6,:)+ens.rtc(7,:)/100);
t_int=diff(dats);
fprintf('Record begins at %s\n',datestr(dats(1),0));
fprintf('Ping interval appears to be  %s\n',datestr(t_int,13));


% Estimate number of records (since I don't feel like handling EOFs correctly,
% we just don't read that far!)

extrabytes=0;
nensinfile=fix(naminfo.bytes/(hdr.nbyte+2+extrabytes));
fprintf('\nEstimating %d ensembles in this file\n',nensinfile);  

if length(nens)==1,
  if nens==-1,
    nens=nensinfile;
  end; 
  fprintf('   Reading %d ensembles, reducing by a factor of %d\n',nens,num_av); 
else
  fprintf('   Reading ensembles %d-%d, reducing by a factor of %d\n',nens,num_av); 
  fseek(fd,(hdr.nbyte+2+extrabytes)*(nens(1)-1),'cof');
  nens=diff(nens)+1;
end;

% Number of records after averaging.

n=fix(nens/num_av);
fprintf('Final result %d values\n',n); 

% Structure to hold all ADCP data 
% Not storing all the data contained in the raw binary file, merely

adcp=struct('name','adcp','config',cfg,'mtime',zeros(1,n),'number',zeros(1,n),'pitch',zeros(1,n),...
        'roll',zeros(1,n),'heading',zeros(1,n),'temperature',zeros(1,n),'pressure',zeros(1,n),...
        'beam1',zeros(cfg.n_cells,n),'beam2',zeros(cfg.n_cells,n),'beam3',zeros(cfg.n_cells,n),...
        'beam4',zeros(cfg.n_cells,n),'corr',zeros(cfg.n_cells,4,n),...
        'status',zeros(cfg.n_cells,4,n),'intens',zeros(cfg.n_cells,4,n));

% Calibration factors for backscatter data

clear global ens
% Loop for all records
for k=1:n,

  % Gives display so you know something is going on...  
  if rem(k,500)==0,  fprintf('\n%d',k*num_av);
  end;

  % Read an ensemble
  ens=rd_buffer(fd,num_av);

  if ~isstruct(ens), % If aborting...
    fprintf('Only %d records found..suggest re-running RDRADCP using this parameter\n',(k-1)*num_av);
    fprintf('(If this message preceded by a POSSIBLE PROGRAM PROBLEM message, re-run using %d)\n',(k-1)*num_av-1);
    break;
  end;
    
  dats=datenum(century+ens.rtc(1,:),ens.rtc(2,:),ens.rtc(3,:),ens.rtc(4,:),ens.rtc(5,:),ens.rtc(6,:)+ens.rtc(7,:)/100);
  adcp.mtime(k)=median(dats);  
  adcp.number(k)      =ens.number(1);
  adcp.heading(k)     =mean(ens.heading);
  adcp.pitch(k)       =mean(ens.pitch);
  adcp.roll(k)        =mean(ens.roll);
  adcp.temperature(k) =mean(ens.temperature);
  adcp.pressure(k)    =mean(ens.pressure);

  if isstr(vels),
    adcp.beam1(:,k)    =nmean(ens.beam1 ,2);
    adcp.beam2(:,k)   =nmean(ens.beam2,2);
    adcp.beam3(:,k)    =nmean(ens.beam3 ,2);
    adcp.beam4(:,k)   =nmean(ens.beam4,2);
  else
   adcp.beam1(:,k)    =nmedian(ens.beam1  ,vels(1),2);
   adcp.beam2(:,k)   =nmedian(ens.beam2,vels(1),2);
   adcp.beam3(:,k)    =nmedian(ens.beam3  ,vels(2),2);
   adcp.beam4(:,k)   =nmedian(ens.beam4,vels(3),2);
  end;
  
  adcp.corr(:,:,k)      =nmean(ens.corr,3);        % added correlation RKD 9/00
  adcp.status(:,:,k)	=nmean(ens.status,3);   
  adcp.intens(:,:,k)   =nmean(ens.intens,3);
   
end;  

fprintf('\n');
fclose(fd);

%-------------------------------------
function [hdr,pos]=rd_hdr(fd);
% Read config data
% Changed by Matt Brennan to skip weird stuff at BOF (can happen 
% when switching from one flash card to another in moored ADCPs).

cfgid=fread(fd,1,'uint16');
nread=0;
while cfgid~=hex2dec('7F7F'),
    cfgid=fread(fd,1,'uint16');
    nread=nread+2;
    if isempty(cfgid),  % End of file
        disp('EOF reached before finding valid cfgid')
        return;
    end
    pos=ftell(fd);
    if mod(pos,1000)==0
        disp(['Still looking for valid cfgid at file position ' num2str(pos) '...'])
    end
end; 

pos=ftell(fd)-2;
if nread>0,
  disp(['Junk found at BOF...skipping ' int2str(nread) ' bytes until ']);
  disp(['cfgid=' dec2hex(cfgid) ' at file pos ' num2str(pos)])
end;

hdr=rd_hdrseg(fd);

%-------------------------------------
function cfg=rd_fix(fd);
% Read config data

cfgid=fread(fd,1,'uint16');
if cfgid~=hex2dec('0000'),
 warning(['Fixed header ID ' cfgid 'incorrect - data corrupted or not a BB/WH raw file?']);
end; 

cfg=rd_fixseg(fd);

%--------------------------------------
function [hdr,nbyte]=rd_hdrseg(fd);
% Reads a Header

hdr.nbyte          =fread(fd,1,'int16'); 
fseek(fd,1,'cof');
ndat=fread(fd,1,'int8');
hdr.dat_offsets    =fread(fd,ndat,'int16');
nbyte=4+ndat*2;

%-------------------------------------
function opt=getopt(val,varargin);
% Returns one of a list (0=first in varargin, etc.)

if val+1>length(varargin),
	opt='unknown';
else
   opt=varargin{val+1};
end;
   			
%-------------------------------------
function [cfg,nbyte]=rd_fixseg(fd);
% Reads the configuration data from the fixed leader

%%disp(fread(fd,10,'uint8'))
%%fseek(fd,-10,'cof');

cfg.name='wh-adcp';
cfg.sourceprog='instrument';  % default - depending on what data blocks are
                              % around we can modify this later in rd_buffer.
cfg.prog_ver       =fread(fd,1,'uint8')+fread(fd,1,'uint8')/100;

if fix(cfg.prog_ver)==4 | fix(cfg.prog_ver)==5,
    cfg.name='bb-adcp';
elseif fix(cfg.prog_ver)==8 | fix(cfg.prog_ver)==16,
    cfg.name='wh-adcp';
else
    cfg.name='unrecognized firmware version'   ;    
end;    

config         =fread(fd,2,'uint8');  % Coded stuff
cfg.config         =[dec2base(config(2),2,8) '-' dec2base(config(1),2,8)];
cfg.beam_angle     =getopt(bitand(config(2),3),15,20,30);
cfg.beam_freq      =getopt(bitand(config(1),7),75,150,300,600,1200,2400);
cfg.beam_pattern   =getopt(bitand(config(1),8)==8,'concave','convex'); % 1=convex,0=concave
cfg.orientation    =getopt(bitand(config(1),128)==128,'down','up');    % 1=up,0=down
cfg.simflag        =getopt(fread(fd,1,'uint8'),'real','simulated'); % Flag for simulated data
fseek(fd,1,'cof'); 
cfg.n_beams        =fread(fd,1,'uint8');
cfg.n_cells        =fread(fd,1,'uint8');
cfg.pings_per_ensemble=fread(fd,1,'uint16');
cfg.cell_size      =fread(fd,1,'uint16')*.01;	 % meters
cfg.blank          =fread(fd,1,'uint16')*.01;	 % meters
cfg.prof_mode      =fread(fd,1,'uint8');         %
cfg.corr_threshold =fread(fd,1,'uint8');
cfg.n_codereps     =fread(fd,1,'uint8');
cfg.min_pgood      =fread(fd,1,'uint8');
cfg.evel_threshold =fread(fd,1,'uint16');
cfg.time_between_ping_groups=sum(fread(fd,3,'uint8').*[60 1 .01]'); % seconds
coord_sys     =fread(fd,1,'uint8');                                % Lots of bit-mapped info
cfg.coord=dec2base(coord_sys,2,8);
cfg.coord_sys      =getopt(bitand(bitshift(coord_sys,-3),3),'beam','instrument','ship','earth');
cfg.use_pitchroll  =getopt(bitand(coord_sys,4)==4,'no','yes');  
cfg.use_3beam      =getopt(bitand(coord_sys,2)==2,'no','yes');
cfg.bin_mapping    =getopt(bitand(coord_sys,1)==1,'no','yes');
cfg.xducer_misalign=fread(fd,1,'int16')*.01;    % degrees
cfg.magnetic_var   =fread(fd,1,'int16')*.01;	% degrees
cfg.sensors_src    =dec2base(fread(fd,1,'uint8'),2,8);
cfg.sensors_avail  =dec2base(fread(fd,1,'uint8'),2,8);
cfg.bin1_dist      =fread(fd,1,'uint16')*.01;	% meters
cfg.xmit_pulse     =fread(fd,1,'uint16')*.01;	% meters
cfg.water_ref_cells=fread(fd,2,'uint8');
cfg.fls_target_threshold =fread(fd,1,'uint8');
fseek(fd,1,'cof');
cfg.xmit_lag       =fread(fd,1,'uint16')*.01; % meters
nbyte=40;

if cfg.prog_ver>=8.14,  % Added CPU serial number with v8.14
  cfg.serialnum      =fread(fd,8,'uint8');
  nbyte=nbyte+8; 
end;

if cfg.prog_ver>=8.24,  % Added 2 more bytes with v8.24 firmware
  cfg.sysbandwidth  =fread(fd,2,'uint8');
  nbyte=nbyte+2;
end;

if cfg.prog_ver>=16.05,                      % Added 1 more bytes with v16.05 firmware
  cfg.syspower      =fread(fd,1,'uint8');
  nbyte=nbyte+1;
end;

% It is useful to have this precomputed.

cfg.ranges=cfg.bin1_dist+[0:cfg.n_cells-1]'*cfg.cell_size;
if cfg.orientation==1, cfg.ranges=-cfg.ranges; end
		
%-----------------------------
function [ens,hdr,cfg,pos]=rd_buffer(fd,num_av);

% To save it being re-initialized every time.
global ens hdr

% A fudge to try and read files not handled quite right.
global FIXOFFSET SOURCE

% If num_av<0 we are reading only 1 element and initializing
if num_av<0,
 SOURCE=0;
end; 
% This reinitializes to whatever length of ens we want to average.
if num_av<0 | isempty(ens),
 FIXOFFSET=0;   
 n=abs(num_av);
 [hdr,pos]=rd_hdr(fd);
 cfg=rd_fix(fd);
 fseek(fd,pos,'bof');
 clear global ens
 global ens
 
 ens=struct('number',zeros(1,n),'rtc',zeros(7,n),'BIT',zeros(1,n),'ssp',zeros(1,n),'depth',zeros(1,n),'pitch',zeros(1,n),...
            'roll',zeros(1,n),'heading',zeros(1,n),'temperature',zeros(1,n),'salinity',zeros(1,n),...
            'mpt',zeros(1,n),'heading_std',zeros(1,n),'pitch_std',zeros(1,n),...
            'roll_std',zeros(1,n),'adc',zeros(8,n),'error_status_wd',zeros(1,n),...
            'pressure',zeros(1,n),'pressure_std',zeros(1,n),...
            'beam1',zeros(cfg.n_cells,n),'beam2',zeros(cfg.n_cells,n),'beam3',zeros(cfg.n_cells,n),...
            'beam4',zeros(cfg.n_cells,n),'intens',zeros(cfg.n_cells,4,n),'percent',zeros(cfg.n_cells,4,n),...
            'corr',zeros(cfg.n_cells,4,n),'status',zeros(cfg.n_cells,4,n),...
            'smtime',zeros(1,n),'emtime',zeros(1,n),'slatitude',zeros(1,n),...
	        'slongitude',zeros(1,n),'elatitude',zeros(1,n),'elongitude',zeros(1,n),...
	        'flags',zeros(1,n));
  num_av=abs(num_av);
end;

k=0;
while k<num_av,
   
   
   id1=dec2hex(fread(fd,1,'uint16'));

   search_cnt=0;
   while ~strcmp(id1,'7F7F') & search_cnt<2000,
       if isempty(id1),  % End of file
           disp(['EOF reached after ' num2str(search_cnt) ' bytes searched for next valid ensemble start'])
           ens=-1;
           return;
       end;
       id1=dec2hex(fread(fd,1,'uint16'));
       search_cnt=search_cnt+2;
   end;
   if search_cnt==2000,
        error(['Searched 2000 entries...Not a workhorse/broadband file or bad data encountered: ->' id1]); 
   elseif search_cnt>0
       disp(['Searched ' int2str(search_cnt) ' bytes to find next valid ensemble start'])
   end

   startpos=ftell(fd)-2;  % Starting position.
   
   
   % Read the # data types.
   [hdr,nbyte]=rd_hdrseg(fd);      
   byte_offset=nbyte+2;
%% fprintf('# data types = %d\n  ',(length(hdr.dat_offsets)));
   % Read all the data types.
   for n=1:length(hdr.dat_offsets),

    id=fread(fd,1,'uint16');
%%  fprintf('ID=%s SOURCE=%d\n',dec2hex(id,4),SOURCE);
    
% handle all the various segments of data. Note that since I read the
% IDs as a two byte number in little-endian order the high and low bytes
% are exchanged compared to the values given in the manual.

    switch dec2hex(id,4),           
     case '0000',   % Fixed leader
      [cfg,nbyte]=rd_fixseg(fd);
      nbyte=nbyte+2;
      
    case '0080'   % Variable Leader
      k=k+1;
      ens.number(k)         =fread(fd,1,'uint16');
      ens.rtc(:,k)          =fread(fd,7,'uint8');
      ens.number(k)         =ens.number(k)+65536*fread(fd,1,'uint8');
      ens.BIT(k)            =fread(fd,1,'uint16');
      ens.ssp(k)            =fread(fd,1,'uint16');
      ens.depth(k)          =fread(fd,1,'uint16')*.1;   % meters
      ens.heading(k)        =fread(fd,1,'uint16')*.01;  % degrees
      ens.pitch(k)          =fread(fd,1,'int16')*.01;   % degrees
      ens.roll(k)           =fread(fd,1,'int16')*.01;   % degrees
      ens.salinity(k)       =fread(fd,1,'int16');       % PSU
      ens.temperature(k)    =fread(fd,1,'int16')*.01;   % Deg C
      ens.mpt(k)            =sum(fread(fd,3,'uint8').*[60 1 .01]'); % seconds
      ens.heading_std(k)    =fread(fd,1,'uint8');     % degrees
      ens.pitch_std(k)      =fread(fd,1,'int8')*.1;   % degrees
      ens.roll_std(k)       =fread(fd,1,'int8')*.1;   % degrees
      ens.adc(:,k)          =fread(fd,8,'uint8');
      nbyte=2+40;

      if strcmp(cfg.name,'bb-adcp'),
      
          if cfg.prog_ver>=5.55,
              fseek(fd,15,'cof'); % 14 zeros and one byte for number WM4 bytes
	          cent=fread(fd,1,'uint8');            % possibly also for 5.55-5.58 but
	          ens.rtc(:,k)=fread(fd,7,'uint8');    % I have no data to test.
	          ens.rtc(1,k)=ens.rtc(1,k)+cent*100;
	          nbyte=nbyte+15+8;
		  end;
          
      elseif strcmp(cfg.name,'wh-adcp'), % for WH versions.		

          ens.error_status_wd(k)=fread(fd,1,'uint32');
          nbyte=nbyte+4;;

	      if cfg.prog_ver>=8.13,  % Added pressure sensor stuff in 8.13
                  fseek(fd,2,'cof');   
                  ens.pressure(k)       =fread(fd,1,'uint32');  
                  ens.pressure_std(k)   =fread(fd,1,'uint32');
	          nbyte=nbyte+10;  
	      end;

	      if cfg.prog_ver>8.24,  % Spare byte added 8.24
	          fseek(fd,1,'cof');
	          nbyte=nbyte+1;
	      end;

	      if cfg.prog_ver>=16.05,   % Added more fields with century in clock 16.05
	          cent=fread(fd,1,'uint8');            
	          ens.rtc(:,k)=fread(fd,7,'uint8');   
	          ens.rtc(1,k)=ens.rtc(1,k)+cent*100;
	          nbyte=nbyte+8;
	      end;
      end;
  	      
    case '0100',  % Velocities
      vels=fread(fd,[4 cfg.n_cells],'int16')'*.001;     % m/s
      ens.beam1(:,k) =vels(:,1);
      ens.beam2(:,k)=vels(:,2);
      ens.beam3(:,k) =vels(:,3);
      ens.beam4(:,k)=vels(:,4);
      nbyte=2+4*cfg.n_cells*2;
      
    case '0200',  % Correlations
      ens.corr(:,:,k)   =fread(fd,[4 cfg.n_cells],'uint8')';
      nbyte=2+4*cfg.n_cells;
      
    case '0300',  % Echo Intensities  
      ens.intens(:,:,k)   =fread(fd,[4 cfg.n_cells],'uint8')';
      nbyte=2+4*cfg.n_cells;

    case '0400',  % Percent good
      ens.percent(:,:,k)   =fread(fd,[4 cfg.n_cells],'uint8')';
      nbyte=2+4*cfg.n_cells;
   
    case '0500',  % Status
         % Note in one case with a 4.25 firmware SC-BB, it seems like
         % this block was actually two bytes short!
      ens.status(:,:,k)   =fread(fd,[4 cfg.n_cells],'uint8')';
      nbyte=2+4*cfg.n_cells;
            
    case '0701', % Number of good pings
      fseek(fd,4*cfg.n_cells,'cof');
      nbyte=2+4*cfg.n_cells;
    
    case '0702', % Sum of squared velocities
      fseek(fd,4*cfg.n_cells,'cof');
      nbyte=2+4*cfg.n_cells;

    case '0703', % Sum of velocities      
      fseek(fd,4*cfg.n_cells,'cof');
      nbyte=2+4*cfg.n_cells;

             
    otherwise,
      
      fprintf('Unrecognized ID code: %s\n',dec2hex(id,4));
      nbyte=2;
     %% ens=-1;
     %% return;
        
    end;
   
    % here I adjust the number of bytes so I am sure to begin reading
    % at the next valid offset. If everything is working right I shouldn't have
    % to do this but every so often firmware changes result in some differences.

    %%fprintf('#bytes is %d, original offset is %d\n',nbyte,byte_offset);
    byte_offset=byte_offset+nbyte;   
      
    if n<length(hdr.dat_offsets),
      if hdr.dat_offsets(n+1)~=byte_offset,    
        %fprintf('%s: Adjust location by %d\n',dec2hex(id,4),hdr.dat_offsets(n+1)-byte_offset);
        fseek(fd,hdr.dat_offsets(n+1)-byte_offset,'cof');
      end;	
      byte_offset=hdr.dat_offsets(n+1); 
    end;
  end;

  % Now at the end of the record we have two reserved bytes, followed
  % by a two-byte checksum = 4 bytes to skip over.

  readbytes=ftell(fd)-startpos;
  offset=(hdr.nbyte+2)-byte_offset; % The 2 is for the checksum

  if offset ~=4 & FIXOFFSET==0, 
    fprintf('\n*****************************************************\n');
    if feof(fd),
      fprintf(' EOF reached unexpectedly - discarding this last ensemble\n');
      ens=-1;
    else
      fprintf('Adjust location by %d (readbytes=%d, hdr.nbyte=%d)\n',offset,readbytes,hdr.nbyte);
      fprintf(' NOTE - If this appears at the beginning of the read, it is\n');
      fprintf('        is a program problem, possibly fixed by a fudge\n');
      fprintf('        PLEASE REPORT TO rich@eos.ubc.ca WITH DETAILS!!\n\n');
      fprintf('      -If this appears at the end of the file it means\n');
      fprintf('       The file is corrupted and only a partial record has  \n');
      fprintf('       has been read\n');
    end;
    fprintf('******************************************************\n');
    FIXOFFSET=offset-4;
  end;  
  fseek(fd,4+FIXOFFSET,'cof'); 
   
  % An early version of WAVESMON and PARSE contained a bug which stuck an additional two
  % bytes in these files, but they really shouldn't be there 
  %if cfg.prog_ver>=16.05,    
  %	  fseek(fd,2,'cof');
  %end;
  	   
end;

% Blank out stuff bigger than error velocity
% big_err=abs(ens.beam4)>.2;
big_err=0;
	
% Blank out invalid data	
ens.beam1(ens.beam1==-32.768 | big_err)=NaN;
ens.beam2(ens.beam2==-32.768 | big_err)=NaN;
ens.beam3(ens.beam3==-32.768 | big_err)=NaN;
ens.beam4(ens.beam4==-32.768 | big_err)=NaN;




%--------------------------------------
function y=nmedian(x,window,dim);
% Copied from median but with handling of NaN different.

if nargin==2, 
  dim = min(find(size(x)~=1)); 
  if isempty(dim), dim = 1; end
end

siz = [size(x) ones(1,dim-ndims(x))];
n = size(x,dim);

% Permute and reshape so that DIM becomes the row dimension of a 2-D array
perm = [dim:max(length(size(x)),dim) 1:dim-1];
x = reshape(permute(x,perm),n,prod(siz)/n);

% Sort along first dimension
x = sort(x,1);
[n1,n2]=size(x);

if n1==1,
 y=x;
else
  if n2==1,
   kk=sum(finite(x),1);
   if kk>0,
     x1=x(max(fix(kk/2),1));
     x2=x(max(ceil(kk/2),1));
     x(abs(x-(x1+x2)/2)>window)=NaN;
   end;
   x = sort(x,1);
   kk=sum(finite(x),1);
   x(isnan(x))=0;
   y=NaN;
   if kk>0,
    y=sum(x)/kk;
   end;
  else
   kk=sum(finite(x),1);
   ll=kk<n1-2;
   kk(ll)=0;x(:,ll)=NaN;
   x1=x(max(fix(kk/2),1)+[0:n2-1]*n1);
   x2=x(max(ceil(kk/2),1)+[0:n2-1]*n1);

   x(abs(x-ones(n1,1)*(x1+x2)/2)>window)=NaN;
   x = sort(x,1);
   kk=sum(finite(x),1);
   x(isnan(x))=0;
   y=NaN+ones(1,n2);
   if any(kk),
    y(kk>0)=sum(x(:,kk>0))./kk(kk>0);
   end;
  end;
end; 

% Permute and reshape back
siz(dim) = 1;
y = ipermute(reshape(y,siz(perm)),perm);

%--------------------------------------
function y=nmean(x,dim);
% R_NMEAN Computes the mean of matrix ignoring NaN
%         values
%   R_NMEAN(X,DIM) takes the mean along the dimension DIM of X. 
%

kk=finite(x);
x(~kk)=0;

if nargin==1, 
  % Determine which dimension SUM will use
  dim = min(find(size(x)~=1));
  if isempty(dim), dim = 1; end
end;

if dim>length(size(x)),
 y=x;              % For matlab 5.0 only!!! Later versions have a fixed 'sum'
else
  ndat=sum(kk,dim);
  indat=ndat==0;
  ndat(indat)=1; % If there are no good data then it doesn't matter what
                 % we average by - and this avoid div-by-zero warnings.

  y = sum(x,dim)./ndat;
  y(indat)=NaN;
end;
