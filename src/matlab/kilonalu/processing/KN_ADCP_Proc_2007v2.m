% KN_ADCP_Proc_2007v2
% Processes real time data from moored ADCP    
% given adcp, cfg, ts, DT and adds results to a deployment file same as KN_ADCP_Proc_2007
% modified 4/6/07 to include filtered pressure signal (higher than tidal)

kk = kk+1;

% identify averaging start and end points 
% ts, DT are given
te = ts+DT/60/24;
tf = Tadcp(end);

% Process data between ts and te
fprintf([datestr(ts) ' to ' datestr(te) ' \n']);
if te>tf
    nens = [];
else
    nens = find(Tadcp>=ts & Tadcp<te);    % identify data section for velocity averaging
    %nspec = find(Tadcp>=ts_spec & Tadcp<te);    % identify data section for spectral averaging
end

if ~isempty(nens)
    tt0 = Tadcp(nens);     
    
    % Calculate mean velocities from each data burst (should confine mean
    % to in between zero upcrossings, but error is very small for long times ...)
    VE = nanmean(VelE(:,nens),2);
    VN = nanmean(VelN(:,nens),2);
    VU = nanmean(VelU(:,nens),2);
    VEr = nanmean(VelErr(:,nens),2);
    tt = mean(tt0);
    
    AcIn = nanmean(AI(:,nens),2);    % Backscatter data
    Tmp = nanmean(Temp(nens),2);
    Plo = nanmean(Padcp(nens),2);
    
    nnz = find(sum(isnan(VelU(:,nens))')/size(VelU(:,nens),2) < 0.25);  % only use depth bins where NaN's are less than 20% of data
    if isempty(nnz)
        nnz = 1;
    end
    for i = 1:length(nnz)
        VelE(nnz(i),nens) = nan_interp(VelE(nnz(i),nens));
        VelN(nnz(i),nens) = nan_interp(VelN(nnz(i),nens));
        VelU(nnz(i),nens) = nan_interp(VelU(nnz(i),nens));
        VelErr(nnz(i),nens) = nan_interp(VelErr(nnz(i),nens));
    end
    nnan = find(~isnan(VelE(:,nens)));
    
    % interpolate data to regular time vector 
    ti = [tt0(1):dti/(3600*24):tt0(end)];
    VEi = interp1(tt0',VelE(nnz,nens)',ti','spline');
    VNi = interp1(tt0',VelN(nnz,nens)',ti','spline');
    VUi = interp1(tt0',VelU(nnz,nens)',ti','spline');
    VEri = interp1(tt0',VelErr(nnz,nens)',ti','spline');
    Pi = interp1(tt0,Padcp(nens),ti,'spline');
% filter interpolated pressure data
    % filter parameters: set in calling function  KN_RT_2007.m
    % dti:  approximate time interval (in sec) for data interpolation
    % ford: filter order
    % fcut: filter cutoff (in minutes)
    nyq = (2*dti)/60;  % Nyquist period in minutes
    fPi = CurrFilt(Pi,fcut,nyq,ford);
% subsample filtered P data based on cutoff and interpolation periods
    subsam=floor(fcut*60/dti);  % fcut(min)*60(sec/min)/dti(sec)
    subsamstart=floor(subsam/2); % eliminates filter "tail"
    pf = fPi(subsamstart:subsam:end);
    tpf = ti(subsamstart:subsam:end);

    
    % find bins near the bed
    nbed = find(z(nnz)<(-z0+zbed));
    V = mean((VEi(:,nbed).^2 + VNi(:,nbed).^2).^0.5,2);
    VSort=sort(V);
    Vbed = mean(VSort((2*round(length(VSort)/3)):end));
    
    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%    
    
    % Filter pressure signal for tides and wave analysis 
    % first filter out high frequencies
    Phi = detrend(Padcp(nens)); % no filter 
    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    
    % Calculate significant wave height for each burst from zero crossing
    % analysis
    % Find zero crossings
    % Determine first zero upcrossing in burst
    nsign = Phi(2:end).*Phi(1:end-1); % product of successive measurements determines when the zero axis is crossed
    nz1 = find(nsign<0);  % negative values indicate crossing
    nz = nz1(find(Phi(nz1)<0));  % determine sign of pressure just prior to crossing (negative value indicates upcrossing)
    
    clear H tz
    for i = 1:length(nz)
        % Determine time of each crossing (interpolated to find crossing point
        % accurately)
        tz(i) = interp1(Phi(nz(i):nz(i)+1),tt0(nz(i):nz(i)+1),0); % replacing this line with an explicit interpolation statement will speed up the code
        
        % find max surface displacement between each crossing
        if i<length(nz)
            H(i) = (max(Phi(nz(i):nz(i+1)))-min(Phi(nz(i):nz(i+1))));
        end
    end
    
    HSort=sort(H);
    Hzc = mean(HSort((2*round(length(HSort)/3)):end));
    Tzc = mean(diff(tz))*24*3600;
    Hmx = max(H);
    %%%%%%%%%%%%%%%
    % calculate wave direction for each burst at each depth
    for j = 1:nbins
        nnan = (~isnan(VelE(j,nens)) & ~isnan(VelN(j,nens)));
        if length(nens(nnan))>0.1*length(nens)  % use data only if >10% are not NaNs
            pv(j,:) = polyfit(VelE(j,nens(nnan)),VelN(j,nens(nnan)),1);
        else
            pv(j,:) = NaN*[1 1];
        end
        a(j) = atan(pv(j,1));
    end
    alpha = (a')*180/pi + 180;    
    
    % Find 'mid-water' depths for rms V and for dir spectra calculations
    nb = find(z(nnz) < zds(1) & z(nnz) > zds(2));
    vrms = nanmean((mean((VEi(:,nb).^2 + VNi(:,nb).^2).^0.5,2)).^2).^0.5;
    
    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%    
    % Calculate directional spectra    
    if isempty(specfil) | specfil(1:4) == 'temp'
        specfil = 'temp.spec';
    else
        specfil = datestr(tt,30);
        cd(specdir);
    end

    ID.data = [nanmean(VNi(:,nb),2) nanmean(VEi(:,nb),2) Pi'];
    ID.layout = [zeros(1,2+1); zeros(1,2+1); mean(z(nnz(nb))')+z0+ztrans mean(z(nnz(nb))')+z0+ztrans ztrans];
    ID.datatypes = {'vely' 'velx' 'pres'};
    ID.fs = 1/dti;
    ID.depth = z0;
    [SMout,EPout]=dirspec(ID,SM,EP,{'PLOTTYPE',0,'MESSAGE',0,'FILEOUT',specfil}); 
    
   % Retrieve basic spectral quantities
    [Hsig,Tp,DTp,Dp]=infospec_mod(SMout,0);
    f = SMout.freqs;
    dirs = -(SMout.dirs-90)+180;
    
    df = f(2)-f(1);
    ddirs = abs(dirs(2)-dirs(1));
    SPf = sum(SMout.S,2)*ddirs; % Spectral density vs frequency
    SPd = sum(SMout.S,1)*df; % Spectral density vs direction

    procmess = ' \n Status: ok';
    if specsav  % saves daily spectral averages ...
        fprintf('calculating spectrum for day %d \n',nday); 
        if isempty(nday)
            nday = 1;
            dy = floor(tt);
            SDens = sum(SMout.S,2)';
            dyi = 1;
            Tday(nday) = floor(tt)
            Tspec = [];
            SD(nday,:) = SDens/dyi;
        elseif floor(tt) == dy
            SDens = SDens + sum(SMout.S,2)';
            dyi = dyi+1; 
            SD(nday,:) = SDens/dyi;
            fprintf('wave spec, day %d, file %d \n',nday,dyi); 
        else
            fprintf('New day \n'); 
            SD(nday,:) = SDens/dyi;
            dyi = 1;
            dy = floor(tt);
            SDens = sum(SMout.S,2)';
            nday = nday+1;
            Tday(nday) = floor(tt);
        end
        Dd(kk,:) = sum(SMout.S,1);
        if isempty(swelldir)
            Sd(kk,:) = sum(SMout.S,2)';
        else
            ndum = find(dirs>swelldir(1) & dirs<swelldir(2));
            Sd(kk,:) = sum(SMout.S(:,ndum),2)';
        end
        Tspec(kk) = tt;
    end
    

else
    tt = mean([ts te]);
    VE = ones(size(VelE,1),1)*NaN;
    VN = ones(size(VelE,1),1)*NaN;
    VU = ones(size(VelE,1),1)*NaN;
    VEr = ones(size(VelE,1),1)*NaN;
    alpha = ones(size(VelE,1),1)*NaN;    
    
    AcIn = ones(size(VelE,1),1)*NaN;    % Backscatter data
    Tmp = NaN;
    Plo = NaN;
    Hzc = NaN;
    Tzc = NaN;
    Hsig = NaN;
    Hmax = NaN;
    Vbed = NaN;
    vrms = NaN;
    DTp = NaN;
    Tp = NaN;
    Dp = NaN;
    tpf = NaN;
    pf = NaN;
    if specsav
        Sd(kk,:) = NaN*ones(1,length(SM.freqs));
        Dd(kk,:) = NaN*ones(1,length(SM.dirs));
        Tspec(kk) = tt;
    end
    procmess =  ' \n Status: No ADCP Data';
end

cd(ldir);
        
% change DirSpec angles to compass headings:
DTp = -(DTp-90)+180;
Dp = -(Dp-90)+180;
ndum = find(DTp<0);
DTp(ndum) = DTp(ndum)+360;
Dp(ndum) = Dp(ndum)+360;

% when the ADCP is relocated at a new depth the data in the _summ file has
% to be interpolated onto a new z axis & saved for continued plotting
if prevtim == -1
    try
        load([depfilnam '_summ']); % loads summary: Tt VVE VVN VVU VVEr Ttmp H13 HMX Hs ALPHA Tps Dps DTps Pp VB
    catch
        pause (5)
        try
            load([depfilnam '_summ']);
        catch
            pause (5)
            load([depfilnam '_summ']);
        end
    end
    save([depfilnam '_prevsumm'],'Tt','VVE','VVN','VVU','VVEr','AIs','Ttmp','H13','HMX','Hss','Tpzc','VB','VRMS','ALPHs','Tps','DTps','Dps','Pp','EP','dti','Z0','Tf','Pf','SPD','SPF');
    VVE=interp1(Z0,VVE,-z);
    VVN=interp1(Z0,VVN,-z);
    VVU=interp1(Z0,VVU,-z);
    VVEr=interp1(Z0,VVEr,-z);
    AIs=interp1(Z0,AIs,-z);
    ALPHs=interp1(Z0,ALPHs,-z);
    adjP=Pp-nanmean(Pp,2)+z0;
    Pf=Pf-nanmean(Pp,2)+z0;
    Pp = adjP;
    Z0 = -z;
    clear VEz VNz VUz VErz AIsz ALPHsz adjP adjPf
    try
        save([depfilnam '_summ'],'Tt','VVE','VVN','VVU','VVEr','AIs','Ttmp','H13','HMX','Hss','Tpzc','VB','VRMS','ALPHs','Tps','DTps','Dps','Pp','EP','dti','Z0','Tf','Pf','SPD','SPF');
    catch
        pause (5)
        try
            save([depfilnam '_summ'],'Tt','VVE','VVN','VVU','VVEr','AIs','Ttmp','H13','HMX','Hss','Tpzc','VB','VRMS','ALPHs','Tps','DTps','Dps','Pp','EP','dti','Z0','Tf','Pf','SPD','SPF');
        catch
            pause (5)
            save([depfilnam '_summ'],'Tt','VVE','VVN','VVU','VVEr','AIs','Ttmp','H13','HMX','Hss','Tpzc','VB','VRMS','ALPHs','Tps','DTps','Dps','Pp','EP','dti','Z0','Tf','Pf','SPD','SPF');
        end
    end    
end
    

if prevtim ~= 0
    % load in existing data
    try
        load([depfilnam '_summ']); % loads summary: Tt VVE VVN VVU VVEr Ttmp H13 HMX Hs ALPHA Tps Dps DTps Pp VB
    catch
        pause (5)
        try
            load([depfilnam '_summ']);
        catch
            pause (5)
            load([depfilnam '_summ']);
        end
    end
    % Adjust pressures from previous deployments
%     nprev = find(Tt<tstart);
%     if ~isempty(nprev)
%         Pp(nprev) = Pp(nprev)-nanmean(Pp(nprev),2)+nanmean(Pp(nprev+1:end),2);
%     end
%     
    nsvtim = find(Tt>Tt(end)-nsvdys);    % save only last nsvdys days of data
    nsvtf = find(Tf>Tt(end)-nsvdys);  % same re filtered P series
    % Check for gap in summary file:
    if Tt(end)<tt-nodatagap/24; % if gap is bigger than nodatagap then we need to add some fillers
        tfill = [Tt(end)+DT/(24*60):DT/(24*60):tt-5/(24*60)];    % fill in empty spaces of data ..
        ftfill= [Tt(end)+DT/(24*60):DT/(24*60):tt-11/(24*60)];
    else    
        tfill = [];
        ftfill = [];
    end
    nfill = length(tfill);
    fnfill= length(ftfill); % re filtered P time series 

    % Update data
    Tt = [Tt(nsvtim) tfill tt];
    VVE = [VVE(:,nsvtim) NaN*ones(size(VVE,1),nfill) VE];
    VVN = [VVN(:,nsvtim) NaN*ones(size(VVE,1),nfill) VN];
    VVU = [VVU(:,nsvtim) NaN*ones(size(VVE,1),nfill) VU];
    VVEr = [VVEr(:,nsvtim) NaN*ones(size(VVE,1),nfill) VEr];
    Ttmp = [Ttmp(nsvtim) NaN*ones(1,nfill) Tmp];
    H13 = [H13(nsvtim) NaN*ones(1,nfill) Hzc];
    HMX = [HMX(nsvtim) NaN*ones(1,nfill) Hmx];
    Hss = [Hss(nsvtim) NaN*ones(1,nfill) Hsig];
    Tpzc = [Tpzc(nsvtim) NaN*ones(1,nfill) Tzc];
    VB = [VB(nsvtim) NaN*ones(1,nfill) Vbed];
    VRMS = [VRMS(nsvtim) NaN*ones(1,nfill) vrms];
    ALPHs = [ALPHs(:,nsvtim) NaN*ones(size(alpha,1),nfill)  alpha];
    Tps = [Tps(nsvtim) NaN*ones(1,nfill) Tp];
    Dps = [Dps(nsvtim) NaN*ones(1,nfill) Dp];
    DTps = [DTps(nsvtim) NaN*ones(1,nfill) DTp];
    Pp = [Pp(nsvtim) NaN*ones(1,nfill) Plo];
    AIs = [AIs(:,nsvtim) NaN*ones(size(AcIn,1),nfill) AcIn];
    Pf = [Pf(nsvtf) NaN*ones(1,fnfill) pf];
    Tf = [Tf(nsvtf) NaN*ones(1,fnfill) tpf];
    SPF = [SPF(:,nsvtim) NaN*ones(size(SPf,1),nfill) SPf];
    SPD = [SPD(:,nsvtim) NaN*ones(size(SPd,2),nfill) SPd'];

else
    Tt = tt;
    VVE = VE;
    VVN = VN;
    VVU = VU;
    VVEr = VEr;
    Ttmp = Tmp;
    H13 = Hzc;
    HMX = Hmx;
    Tpzc = Tzc;
    Hss = Hsig;
    VB = Vbed;
    VRMS = vrms;
    ALPHs = alpha;
    Tps = Tp;
    Dps = Dp;
    DTps = DTp;
    Pp = Plo; % start new file
    AIs = AcIn;
    Pf = pf;
    Tf = tpf;    
    SPF = SPf;
    SPD = SPd;
end

% % These variables will be used in the plot routines
D = nanmean(Pp,2);
Z = D-cfg.ranges;
% D=z0;   
% Z=-z;
Z0 = Z;

clear VE VN VU VEr Tmp Hzc Hsig alpha Tp Dp DTp Plo AcIn pf tpf

% save cumulative data
% % % save([depfilnam '_summ' outext],'Tt','VVE','VVN','VVU','VVEr','AIs','Ttmp','H13','Hss','Tpzc','VB','VRMS','ALPHs','Tps','DTps','Dps','Pp','EP','dti','Z0');

% % % if specsav & ~isempty(nday)
% % %     save([depfilnam '_specsumm' outext] , 'f', 'dirs', 'Tday', 'SD', 'Sd', 'Dd', 'Tspec');
% % % end
try
    save([depfilnam '_summ'],'Tt','VVE','VVN','VVU','VVEr','AIs','Ttmp','H13','HMX','Hss','Tpzc','VB','VRMS','ALPHs','Tps','DTps','Dps','Pp','EP','dti','Z0','Tf','Pf','SPD','SPF','f');
catch
    pause (5)
    try
        save([depfilnam '_summ'],'Tt','VVE','VVN','VVU','VVEr','AIs','Ttmp','H13','HMX','Hss','Tpzc','VB','VRMS','ALPHs','Tps','DTps','Dps','Pp','EP','dti','Z0','Tf','Pf','SPD','SPF','f');
    catch
        pause (5)
        save([depfilnam '_summ'],'Tt','VVE','VVN','VVU','VVEr','AIs','Ttmp','H13','HMX','Hss','Tpzc','VB','VRMS','ALPHs','Tps','DTps','Dps','Pp','EP','dti','Z0','Tf','Pf','SPD','SPF','f');
    end
end  
if specsav & ~isempty(nday)
     save([depfilnam '_specsumm'] , 'f', 'dirs', 'Tday', 'SD', 'Sd', 'Dd', 'Tspec');
end
fprintf('Completed Processing\n');
