% KN_Archive

%
%zlm = zlim;

%
theta = atan(VVE./VVN)*180/pi;
ndum = find(theta<0);
theta(ndum) = 360+theta(ndum);
ndum = find(Z0>1);
nz = min(find(abs(Z0-5) == min(abs(Z0-5))));  % find Z = 5m (check if Z is negative or positive)
theta_av = atan(nanmean(VVE(ndum,end))/nanmean(VVN(ndum,end)))*180/pi;
if theta_av<0, theta_av = theta_av+360; end

V = real((VVE(:,end).^2+VVN(:,end).^2).^0.5);

cd(ftpdir);
fid = fopen('KNData.txt','wt');
ndum = find(Z0>1);
nav = find(Z0 > -zlm(2) & Z0 < -zlm(1));

fprintf(fid,['Latest observation period: ' datestr(Tt(end)-PROCINT/60/24/2) ' - ' datestr(Tt(end)+PROCINT/60/24/2,13) '\n \n']);
fprintf(fid,['Wave conditions: (' num2str(PROCINT) ' minute average):\n']);
fprintf(fid,['Hsig (zero crossing): \t \t ' num2str(H13(end),3) ' m \n']); 
fprintf(fid,['Hsig (spectral): \t \t ' num2str(Hss(end),3) ' m \n']); 
fprintf(fid,['Peak period: \t \t \t ' num2str(Tps(end),3) ' sec \n']); 
fprintf(fid,['Peak spectral direction: \t ' num2str(DTps(end),4) ' deg \n']); % Check
fprintf(fid,['Dominant spectral direction: \t ' num2str(Dps(end),4) ' deg\n \n']); % Check
fprintf(fid,['Water conditions \n']);
fprintf(fid,['Temperature: \t \t \t ' num2str(Ttmp(end),3) ' deg C \n']); % Check
fprintf(fid,['Acoustic backscatter (depth avg''d): ' num2str(nanmean(AIs(nav,end),1),4) ' counts \n\n']); % Check
fprintf(fid,['Currents (' num2str(PROCINT) ' minute average): \n']); % Check
fprintf(fid,['Depth-averaged: \t\t ' num2str(nanmean(V(nav),1),2) ' m/s\n']); % Check
fprintf(fid,['Direction (depth-averaged): \t ' num2str(theta_av,3) ' deg \n']);  % Check
fprintf(fid,['5 meter depth: \t \t \t' num2str(nanmean(V(nz),1),2) ' m/s \n']); % Check
fprintf(fid,['Direction (5 m depth): \t\t' num2str(theta(nz,end),3) ' deg \n']);  % Check
fprintf(fid,['Near-bed significant velocity: \t' num2str(VB(end),3) ' m/s \n']);  % Check
fclose(fid);

nyest = max(find(~isnan(H13(1:end-1)) == 1));
% newest record excluding last one  (this method avoids problems w/NaN at 
    % second to last spot ...
if length(Tt) == 1  % if first proc in deployment
    newday = 1;
elseif str2num(datestr(Tt(end),7)) ~= str2num(datestr(Tt(nyest),7)) % if the last two (valid) times don't have the same date
    % prevtim = 0 for new deployment, = 1 for continuing a deployment
    newday = 1;
else
    newday = 0;
end

if newday
    if prevtim ~= 0 % if this isn't a new deployment, then copy last file to a file named by date
        % first clear ftp/archive directory
        cd([ftpdir 'Archive']);
        delete('*');
        
        % copy to archive directory
        filnamarch=datestr(Tt(nyest),'mmddyy'); % name of the archive file after the date
        ext2='.txt';
        archfile=[filnamarch ext2];
        %[suc, mess, messid] = copyfile([ldir '\KNData.txt'],[archdir '\' eval('archfile')]);
        [suc2, mess, messid] = copyfile([ftpdir 'KNDataSumm.txt'],[ftpdir 'Archive/' archfile]);
        % also produce daily plot from last day
        trng = 72;  %plot time range in hours
        maxt = Tt(nyest);
        mint = max([min(Tt) maxt-trng/24]);
        suptext = ['Data for : ' datestr(Tt(nyest), 'mm/dd/yy') ];
        KN_Plot2007v3;
        
        % Write output to archive directory
        saveas(f1,['VP_' filnamarch],'jpeg');
        saveas(f3,['WQ_' filnamarch],'jpeg');
        saveas(f4,['WC_' filnamarch],'jpeg');
        
        % save copies to archdir for archival
        copyfile('*.*',archdir);
        
    end

    % if new day, start a new archive file and write the first line
    cd(ftpdir);

    fid = fopen('KNDataSumm.txt','wt');
    fprintf(fid,['Kilo Nalu Daily Observations: ' datestr(Tt(end),2) '\n \n']);
    fprintf(fid,['   Wave condtns (' num2str(PROCINT) '-minute avg):     ']);
    fprintf(fid,[' || Wtr condtns:|| Currents (' num2str(PROCINT) '-minute avg): \n']);
    fprintf(fid,'------------------------------------------------------------------------------------------\n');
    fprintf(fid,' Time  Hsig  Hsig  Peak  Peak   Dom  || Temp  Acstc || DpthAv Cur  5mCur  5m   NrBd\n');
    fprintf(fid,' (HST) (zc) (spec)  Prd   Dir   Dir  ||        Sctr ||  Cur   Dir         Dir   Vel\n');
    fprintf(fid,'        [m]   [m]  [sec] [deg] [deg] ||[degC] [cts] || [m/s] [deg] [m/s] [deg] [m/s]\n');
    fprintf(fid,'------------------------------------------------------------------------------------------\n');
    fclose(fid);
end

ndum = find(Z0>1);
% open the archive file and write the next line
fid=fopen('KNDataSumm.txt','a');
fprintf(fid,[' ' datestr(round(Tt(end)*24*60)/(24*60),15)]);     % time (every 20min)
fprintf(fid,' %5.2f %5.2f %4.1f  %4.0f  %4.0f  ||  %4.1f %4.0f  || %4.2f  %4.0f  %4.2f  %4.0f  %4.2f\n', ...
    H13(end),Hss(end),Tps(end),DTps(end),Dps(end),Ttmp(end),nanmean(AIs(ndum,end),1),nanmean(V(ndum),1),nanmean(theta(ndum,end),1),nanmean(V(nz),1),nanmean(theta(nz,end),1),VB(end));
%fprintf(fid,'\n');   
fclose(fid);

