% KN_TC_RUN_DK

% modified 8/24/07  to run on shorelab desk top (most try/catch 
% modules can probably be eliminated) 
% Initializations from KN_TC_2007 added to KN_RT_2007
% modified 8/15/07 to calc max & mean dist Psensor-to-surface 
% modified 8/13/07 to calc min distance Psensor-to-surface (m2s)
% modified 8/13/07 to move depfilnam_summ to tcldir (not tczdir)
% modified JW 8/9/07 to add try/catch to cd's to Z directory
% modified JW 7/13/07 to add try/catch around calls to shared
%   Z directory: e.g. load adcp_mostrecent line 99 and ff
% modified JW 5/30/07 to create txt file w T profile (forBEM)
% original JWells May 16, 2007
%
% Called KN_TC_RUN_2007 on the laptop in TCtools

try
    cd(tcindir)

    % look for newest file
    tcd=dir(['tc*.log']);
    clear dat
    tci=1;
    for tci = 1:length(tcd)    
        dat(tci) = datenum(tcd(tci).date);
    end
    nn = find(dat == max(dat));  % newest file in directory ...

    % copy most recent file to a local directory
    filnam = 'tctemp_mostrecent.log';
    [suc, mess, messid] = copyfile(tcd(nn).name,[tcldir,'/',filnam]);
    % process most recent file
    cd (tcldir)
    fprintf('converting raw tc data for most recent file ... \n');
    [tctime,tccal,tcpres]=tcfiles(filnam);

    if prevtim > 0 && length(dat)>1  % continuing deployment & more than 1 file
        [dum, nni] = sort(dat);
        nnp = nni(end-1);  % index for second most recent file
        % copy to local directory
        cd (tcindir)
        filnam = 'tctemp_2ndmostrecent.log';
        [suc, mess, messid] = copyfile(tcd(nnp).name,[tcldir,'/',filnam]); 
        cd (tcldir)
        fprintf('converting raw tc data for completed data file ... \n');
        [tctime0,tccal0,tcpres0]=tcfiles(filnam);
        tctime = [tctime0; tctime];
        tccal = [tccal0, tccal];
        tcpres = [tcpres0; tcpres];
        clear tctime0 tccal0 tcpres0
        nonewdata=0;
    end
catch
    disp('Z: tchain files not available')
    nonewdata=1;
end
% finds 3rd most recent DT  (staying 20 minutes behind ADCP processing)
tcts = floor((now-2*DT/60/24)*24*(60/DT))/(24*(60/DT)); % finds 2nd most recent DT 

%%%%%%%%% creating 20 minute averages %%%%%%%%%%%%%%
tcte = tcts+DT/60/24;
if exist('tctime')
    tctf = tctime(end);
else
    tctf=tcte-1;
end
% Process data between tcts and tcte
fprintf([datestr(tcts) ' to ' datestr(tcte) ' \n']);
if (tcte-(DT/4)/60/24)>tctf
    tcnens = [];
else
    tcnens = find(tctime>=tcts & tctime<tcte);    % identify data averaging section   
end

if nonewdata
    tcnens=[];
end

if ~isempty(tcnens)
    tt0 = tctime(tcnens);   
    Temp = nanmean(tccal(:,tcnens),2);
    ttmn = nanmean(tt0);
    tctt=round(ttmn*24*60)/(60*24);
    tcPlo = nanmean(tcpres(tcnens),1);
    m2s=min(tcpres(tcnens));  % min distance bewteen P sensor and surface
    tcbot = Temp(min(find(isfinite(Temp)))); 
    tctop = Temp(max(find(isfinite(Temp)))); 
    tcmean = nanmean(Temp);
    clear tt0 ttmn
else
    ttmn = mean([tcts tcte]);
    tctt=round(ttmn*24*60)/(60*24);
    clear ttmn
    Temp = ones(size(MABgrid,1),1)*NaN;
    tcPlo = NaN;
    m2s = NaN;
    tcbot = NaN;
    tctop = NaN;
    tcmean = NaN;
end
if isfinite(Temp)
    %%%%%  load adcp time and pressure data  %%%%%
    adcpP=Pp;
    adcpT=Tt;
    cd (tcldir)
    tcdepths;   % assigns tnode data to depth bins
else
    Tgrid = ones(size(MABgrid,1),1)*NaN;
end

if prevtim ~= 0
    % load in existing data
%     load([tcldir,'/',tcdepfilnam '_summ']); % loads in summary data file 
    load([tczdir,'/','TCmostrecent']);  % loads in summary data file 
    nsvtim = find(TCt>TCt(end)-nsvdys);    % save only last nsvdys days
    % Check for gap in summary file:
    % if gap is bigger than nodatagap then we need to add some fillers
    if TCt(end)<tctt-nodatagap/24;
        tfill = [TCt(end)+DT/(24*60):DT/(24*60):tctt-5/(24*60)]';  % fill in
    else    
        tfill = [];
    end
        nfill = length(tfill);
        
    % Update data
    TCt = [TCt(nsvtim); tfill; tctt];
    TCgrid = [TCgrid(:,nsvtim) NaN*ones(size(TCgrid,1),nfill) Tgrid];
    TCP = [TCP(nsvtim); NaN*ones(nfill,1); tcPlo];
    TCm2s = [TCm2s(nsvtim); NaN*ones(nfill,1); m2s];
    TCbot = [TCbot(nsvtim); NaN*ones(nfill,1); tcbot];
    TCtop = [TCtop(nsvtim); NaN*ones(nfill,1); tctop];
    TCmean = [TCmean(nsvtim); NaN*ones(nfill,1); tcmean];
else
    TCt = tctt;
    TCgrid = Tgrid;
    TCP = tcPlo;
    TCm2s = m2s;
    TCbot = tcbot;
    TCtop = tctop;
    TCmean = tcmean;
end

%%%%%%  text file with current T profile (for BEM) %%%%%
Tprof=Tgrid';
MAB=MABgrid';
TAB=Tprof(isfinite(Tprof));
Thgt=MAB(isfinite(Tprof));
Tdate=datestr(tctt,30);
Tdate=[Tdate(1:8),Tdate(10:13)];
Tdate=str2num(Tdate);
Tdates=repmat(Tdate,size(Thgt));
Ttable=[Tdates; Thgt; TAB];
clear Tdate Tdates Thgt TAB MAB Tprof
try
    cd (tczdir)
    fid = fopen('LatestTProfile.txt','wt');
    fprintf(fid,'Date_Time     MAB   Temp(C)\n\n');
    fprintf(fid,'%12.0f  %3.2f  %3.4f\n',Ttable);
    fclose(fid);
catch
    pause (5)
    try
        cd (tczdir)
        fid = fopen('LatestTProfile.txt','wt');
        fprintf(fid,'Date_Time     MAB   Temp(C)\n\n');
        fprintf(fid,'%12.0f  %3.2f  %3.4f\n',Ttable);
        fclose(fid);
    catch
        disp('Failed to write latest T profile')
    end
end
cd (tcldir)
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

clear tctt Tgrid tcPlo m2s tcbot tctop tcmean

% save cumulative data
save([tcldir,'/',tcdepfilnam '_summ'],'TCt','TCgrid','TCP', 'TCm2s', 'MABgrid','TCbot','TCtop','TCmean')
try
    save([tczdir,'/','TCmostrecent'],'TCt','TCgrid','TCP', 'TCm2s', 'MABgrid','TCbot','TCtop','TCmean')
catch
    pause (5)
    try
      save([tczdir,'/','TCmostrecent'],'TCt','TCgrid','TCP', 'TCm2s', 'MABgrid','TCbot','TCtop','TCmean')
    catch
      disp('Failed to save updated TCmostrecent')
    end
end
fprintf('Completed TChain Subroutine\n');
%%%%%%%%%%%%%%%
% 
% % Update file information
% 
% prevtim = tcts;
% 
% strttim = strttim + DT/60/24; % increment time for next analysis
% fprintf(['Next data analysis at: ' datestr(strttim) '\n']);