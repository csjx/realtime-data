% DirSpecMov
% Creates a movie of directional spectra vs. time from ADCP data set
% G. Pawlak 8/19/2004

clear
gdir = 'C:\Documents and Settings\Geno\My Documents\';
direc = [gdir 'WaveBLField\ProfilerData\ADCP'];
movfilnam = 'specmov_aug';
filnam = ['0804RT002_bm'; '0804RT003_bm'];

clims = [0 10e-3];
tinter = 1;  % time interval (in hours) between successive frames
cd(direc);

figure(1)
clf
set(gcf,'position',[360    70   680   860]);

movieon = 0;
if movieon
% 	disp('Resize figure and touch any key to continue')
% 	pause;	% pause to resize screen
	MakeQTMovie('start',[movfilnam,'.mov']);
end

hs1 = subplot(4,1,1);
hs2 = subplot(4,1,2);
hs3 = subplot(4,1,3);
hs4 = subplot(4,1,4);

set(hs1,'position',[0.1 0.5 0.8 0.45]);
set(hs2,'position',[0.1 0.35 0.8 0.125]);
set(hs3,'position',[0.1 0.2 0.8 0.125]);
set(hs4,'position',[0.1 0.05 0.8 0.125]);
flg = 0;

% Load in and plot summary wave data 
tt_all = [];
Hs_all = [];
Hs2_all = [];
Tp_all = [];
Dp_all = [];
DTp_all = [];

for i = 1:size(filnam,1)
    load(filnam(i,:),'tt','Hsig','H13','Tp','DTp','Dp');
    tt_all = [tt_all tt];
    Hs_all = [Hs_all Hsig];
    Hs2_all = [Hs2_all H13];
    Tp_all = [Tp_all Tp];
    Dp_all = [Dp_all Dp];
    DTp_all = [DTp_all DTp];
    
    axes(hs2)
    plot(tt,Hsig,tt,H13);
    hold on
    
    axes(hs3)
    plot(tt,Tp);
    hold on
    
    axes(hs4)
    plot(tt,DTp,tt,Dp);
    hold on
end

axes(hs2)
axis([min(tt_all) max(tt_all) 0 1]);
set(gca,'xticklabel','');

axes(hs3)
axis([min(tt_all) max(tt_all) 5 20]);
set(gca,'xticklabel','');

axes(hs4)
axis([min(tt_all) max(tt_all) 140 220]);
datetick('x',6,'keeplimits');

% Plot profiler movement 
cd([gdir 'WaveBLField\ProfilerData\Profiler']);

axes(hs2)
load('Proftimes_0804T165214');
line([tpos_s; tpos_s],[0*ones(length(tpos_s),1) 10*ones(length(tpos_s),1)]','color','g')
load('Proftimes_0803T111919');
line([tpos_s; tpos_s],[0*ones(length(tpos_s),1) 10*ones(length(tpos_s),1)]','color','r')
cd(direc);

% retrieve spectrum files
direc = dir('Proc*.spec');
for i = 1:length(direc)
    mon(i) = str2num(direc(i).name(min(find(direc(i).name == 'M'))+1:find(direc(i).name == 'D')-1));
    day(i) = str2num(direc(i).name(find(direc(i).name == 'D')+1:find(direc(i).name == 'H')-1));
    hr(i) = str2num(direc(i).name(find(direc(i).name == 'H')+1:max(find(direc(i).name == 'M'))-1));
    mn(i) = str2num(direc(i).name(max(find(direc(i).name == 'M'))+1:find(direc(i).name == '.')-1));
    tdir(i) = datenum(2004,mon(i),day(i),hr(i),mn(i),0);
end
[dum,nsort] = sort(tdir);

spectim = tdir(nsort(1));
k = 1;
spi = 1;
while flg ~= 1
    fprintf('adding frame # %g \n',k);
    figure(1)
    axes(hs1);
    cla
    nn = find(abs(tdir-spectim)<(tinter/2)/24);
    if ~isempty(nn)
        SM = readspec(direc(nn).name);
        hl = polar(-Dp_all(spi)*pi/180*ones(1,2)-pi/2,0.4*[-1 1]);
        hold on
        set(hl,'linewidth',2,'color','r');
        h2 = polar(-DTp_all(spi)*pi/180*ones(1,2)-pi/2,0.4*[-1 1]);
        set(h2,'linewidth',2,'color','g');
        h3 = polar([0:1:360]*pi/180,ones(size([0:1:360]))/Tp_all(spi),'r');
        
        plotspecmod(SM,4,clims);
        title(['Directional Spectra for ' datestr(tdir(nn))]);
        
        axes(hs2);
        hp1 = plot(tt_all(spi),Hs_all(spi),'ro',tt_all(spi),Hs2_all(spi),'ro');
        axes(hs3);
        hp2 = plot(tt_all(spi),Tp_all(spi),'ro');
        axes(hs4);
        hp3 = plot(tt_all(spi),DTp_all(spi),'ro',tt_all(spi),Dp_all(spi),'ro');
        drawnow
        spi = spi+1;
    else
        title(['No Spectra for ' datestr(spectim)]);
        hp2 = plot(tt_all(spi),0,'ro');

    end
    
    if movieon
        MakeQTMovie addfigure
    end	
    k = k+1;
    if ~isempty(nn)
        axes(hs2);
        delete(hp1);
        axes(hs3);
        delete(hp2);
        axes(hs4);
        delete(hp3);
    end
    
    
    spectim = spectim+tinter/24;
    if spectim> max(tdir)
        flg = 1;
    end
    
end
if movieon
	MakeQTMovie finish
end
