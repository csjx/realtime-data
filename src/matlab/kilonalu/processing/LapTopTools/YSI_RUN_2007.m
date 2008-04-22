% YSI_RUN_2007
% carries out basic analysis of real-time data from KiloNalu YSI

fclose('all');
cd(indir)

% look for newest file
d=dir(['KNRT*r.*']);
clear dat
for i = 1:length(d)    
    dat(i) = datenum(d(i).date);
end
nn = find(dat == max(dat));  % newest file in directory ...

% % copy file to a local directory
% [suc, mess, messid] = copyfile(d(nn).name,[ldir '\temp_mostrecent.dat']);
% 
% Check if this is a new file name
ndum = find(d(nn).name == '.');
if str2num(d(nn).name(ndum+1:ndum+3)) ~= str2num(prevfilid) & prevtim > 0  % make sure there's more than 1 file in the directory
    newfile = 1;
else
    newfile = 0;
end

Tdum = [];
VEdum = [];
VNdum = [];
VUdum = [];
VErdum = [];
Tempdum = [];
Pdum = [];

% reprocess previous file (if newfile)
if length(dat)>1    
    if newfile
        [dum, nni] = sort(dat);
        nnp = nni(end-1);  % index for second most recent file
        % copy to local directory
        [suc, mess, messid] = copyfile(d(nnp).name,[ldir 'temp_2ndmostrecent.dat']);
        filnam = 'temp_2ndmostrecent.dat';
        cd(ldir);
        fprintf('converting raw data for completed data file ... \n');
        [adcp,cfg,nens]=rdradcp_short_v1(filnam,1,-1);  %read in all ensembles
    % %     adcp = rmfield(adcp,char('pressure_std','pitch_std', 'roll_std', 'heading_std', 'bt_range', 'bt_vel', 'salinity','depth'));  
    % %     rdr..short removes all these It also returns beam labels   
    % %     if beam 
    % %         adcp.beam1 = adcp.east_vel;
    % %         adcp.beam2 = adcp.north_vel;
    % %         adcp.beam3 = adcp.vert_vel;
    % %         adcp.beam4 = adcp.error_vel;
    % %         adcp = rmfield(adcp,char('east_vel','north_vel','vert_vel','error_vel'));       
    % %     end
    %%%%%%%%%%%%%% different naming system   %%%%%%%%%%%%%%
    % %     filnamout = [d(nnp).name(1:find(d(nnp).name == '.')-1) '_raw'];
        F=datestr(adcp.mtime(1),30);
        filnamout=['KN',F(3:8),F(10:15),'_raw'];
        save(filnamout, 'adcp','cfg');  % save data in mat format 
        fileload=[filnamout,'.mat'];
        adcp0 = adcp;
        clear adcp cfg
    else
        if prevtim >1
        cd (ldir)
        fprintf('loading previous .mat file ... \n');
        load (fileload)
        adcp0=adcp;
        clear adcp cfg
        end
    end
end


%cd(ldir);

if 1 
% process most recent file
% Process VMDas data from most recent file
filnam = 'temp_mostrecent.dat';
fprintf('converting raw data for most recent file ... \n');
%[adcp,cfg,nens]=rdradcp(filnam,1,-1);  %read in all ensembles

nens = -1;
cnt = 1;
while ~isstruct(nens) & cnt < 2
    cd(indir)
    % copy file to a local directory
    %[suc, mess, messid] = copyfile(d(nni(end-1)).name,[ldir '\temp_mostrecent.dat']);
    [suc, mess, messid] = copyfile(d(nn).name,[ldir 'temp_mostrecent.dat']);

    cd(ldir);
    [adcp,cfg,nens]=rdradcp_short_v1(filnam,1,-1);  %read in all ensembles
    cnt = cnt+1;
end


% adcp = rmfield(adcp,char('pressure_std','pitch_std', 'roll_std', 'heading_std', 'bt_range', 'bt_vel', 'salinity','depth'));
%%%%%%%%%%%%%%%%%%%%%%%%%
% if beam 
%     adcp.beam1 = adcp.east_vel;
%     adcp.beam2 = adcp.north_vel;
%     adcp.beam3 = adcp.vert_vel;
%     adcp.beam4 = adcp.error_vel;
%     adcp = rmfield(adcp,char('east_vel','north_vel','vert_vel','error_vel'));       
% end    
filnamout = 'tempmostrecent_raw';
save(filnamout, 'adcp','cfg');  % save data in mat format 

%if length(dat)>1
if length(dat)>1  & prevtim > 1 % concatenate data
    if beam
        adcp.beam1 = [adcp0.beam1 adcp.beam1];
        adcp.beam2 = [adcp0.beam2 adcp.beam2];
        adcp.beam3 = [adcp0.beam3 adcp.beam3];
        adcp.beam4 = [adcp0.beam4 adcp.beam4];
    else
        adcp.east_vel = [adcp0.east_vel adcp.east_vel];
        adcp.north_vel = [adcp0.north_vel adcp.north_vel];
        adcp.vert_vel = [adcp0.vert_vel adcp.vert_vel];
        adcp.error_vel = [adcp0.error_vel adcp.error_vel];
    end    
    adcp.mtime = [adcp0.mtime adcp.mtime];
    adcp.temperature = [adcp0.temperature adcp.temperature];
    adcp.pressure = [adcp0.pressure adcp.pressure];
    adcp.heading = [adcp0.heading adcp.heading];
    adcp.pitch = [adcp0.pitch adcp.pitch];
    adcp.roll = [adcp0.roll adcp.roll];
    adcp.intens = cat(3,adcp0.intens,adcp.intens);
end
end
datmess = ['data read: ' num2str(cnt)];
%adcp = adcp0;

%clear adcp0

% Process
% if prevtim == 0 | prevtim == 1  % use most recent DT minutes if analysis is starting
%     ts = floor((now-DT/60/24)*24*(60/DT))/(24*(60/DT)); % finds second most recent DT fraction of the hour
% else    % else use time from last analysis 
%     ts = prevtim;
% end

ts = floor((now-DT/60/24)*24*(60/DT))/(24*(60/DT)); % finds second most recent DT fraction of the hour
%ts = ts-1/24;



%%%%%%%%%%%%%%%%%%%%%%%%%
% Prepare for processing
% Convert to ENU if necessary
if beam 
    fprintf('Converting to earth coordinates ... \n');
%     ssnd = 1500;    %not being used
%     ECssnd = 1500;
    if headoffset~=0
        heading=adcp.heading;
    end
    [VelE, VelN, VelU, VelErr] = bm2earthmod_v2(adcp.beam1,adcp.beam2,adcp.beam3,adcp.beam4, ...
        heading, headoffset, adcp.pitch, adcp.roll, beams_up);
else
    VelE = adcp.east_vel;
    VelN = adcp.north_vel;
    VelU = adcp.vert_vel;
    VelErr = adcp.error_vel;
end

cellsiz = cfg.cell_size;
bin1dist = cfg.bin1_dist;
nbins = cfg.n_cells;

z0 = mean(adcp.pressure)/1000;  % pressure sensor depth (meters)
z = -z0+cfg.ranges;  % ADCP bin locations

Padcp = adcp.pressure/1000;  % note that division by 1000 takes place here
Tadcp = adcp.mtime;
Temp = adcp.temperature;
EI = squeeze(mean(adcp.intens,2));
AI=EIRangeCorrection_2007(EI,length(EI),nbins,cellsiz);
Padcp = nan_interp(Padcp);

clear adcp

% 
KN_ADCP_Proc_2007v2;
datmess = [ datmess procmess];
PROCINT = DT;

if ~isempty(nens)
    % 1 week plots
    trng = 7*24;  %plot time range in hours
    mint = max([min(Tt) max(Tt)-trng/24]);
    maxt = max(Tt)+1/24;
    suptext = ['Latest observation period: ' datestr(Tt(end)-PROCINT/60/24/2,'mm-dd-yyyy HH:MM') ' - ' datestr(Tt(end)+PROCINT/60/24/2,'HH:MM')];
    cd(ldir);
    KN_Plot2007v3;
    % Write output to ftp directory
    cd(ftpdir);
    saveas(f1,'VelProf_wk','jpeg');
    saveas(f3,'WtrQual_wk','jpeg');
    saveas(f4,'WaveChar_wk','jpeg');
    fprintf('Completed weekly plots \n');

    % 3 day plots
    trng = 72;  %plot time range in hours
    mint = max([min(Tt) max(Tt)-trng/24]);
    maxt = max(Tt)+1/24;
    suptext = ['Latest observation period: ' datestr(Tt(end)-PROCINT/60/24/2,'mm-dd-yyyy HH:MM') ' - ' datestr(Tt(end)+PROCINT/60/24/2,'HH:MM')];
    cd(ldir);
    KN_Plot2007v3;
    % Write output to ftp directory
    cd(ftpdir);
    saveas(f1,'VelProf','jpeg');
    saveas(f2,'VelProf2','jpeg');
    saveas(f3,'WtrQual','jpeg');
    saveas(f4,'WaveChar','jpeg');
    saveas(f5,'WaveSpec','jpeg');
    saveas(f6,'TS_Plot','jpeg');

    fprintf('Completed Daily plots \n');

    % Archive and text file output
    KNArchive_2007;
    fprintf('Completed archiving \n');
else
    datmess = [datmess '\n no data for ' datestr(Tt(end))];
end
kp = kp+1;     



% save figures to local directory
% cd(ldir)
% saveas(f1,['VelProf_' datestr(Tt(end),30)],'jpeg');
% saveas(f2,['VelProf2_' datestr(Tt(end),30)],'jpeg');
% saveas(f3,['WtrQual_' datestr(Tt(end),30)],'jpeg');
% saveas(f4,['WaveChar_' datestr(Tt(end),30)],'jpeg');
% saveas(f5,['WaveSpec_' datestr(Tt(end),30)],'jpeg');

camstat = 'Camera offline ...';
% save status file
fid = fopen('Notes.txt','wt');
fprintf(fid,[datmess '\n']);
fprintf(fid,[camstat '\n']);
fclose(fid);

% save ADAM status file
voltlog='';
[voltlog]=getADAM;
fid = fopen('AdamLog.txt','wt');
fprintf(fid,[voltlog '\n']);
fclose(fid);

% Update times
prevdat = dat(nn);
prevtim = ts;
ndum = find(d(nn).name == '.');
prevfilid = d(nn).name(ndum+1:ndum+3);
strttim = strttim + DT/60/24; % increment time for next analysis
fprintf(['Next data analysis at: ' datestr(strttim) '\n']);
%startat(t1,strttim);



