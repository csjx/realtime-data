% KN_plot05a - needs trng input
% calculate current direction versus depth
% version "a" plots YSI data
%

% Z = Z0;
% D = nanmean(Pp,2);
% z = -Z;
% HMX ;
% TZC = Tps;
% SMout;
%

theta = atan(VVE./VVN)*180/pi;
ndum = find(theta<0);
theta(ndum) = 360+theta(ndum);
% YSI=1; %If YSI is connected, change this to 1
% LISST=1; % if LISST is connected, change this to 1
% mask out velocities near and above the surface
ndum = find(Z<1);

VVE(ndum,  :)  = VVE(ndum,:)*NaN;
VVN(ndum,  :)  = VVN(ndum,:)*NaN;
VVU(ndum,  :)  = VVU(ndum,:)*NaN;
AIs(ndum,  :)  = AIs(ndum,:)*NaN;

% plot all velocities
V = real((VVE(:,end).^2+VVN(:,end).^2).^0.5);
VBC = nanmean(real((VVE(nbed,:).^2+VVN(nbed,:).^2).^0.5));
AIb = nanmean(AIs(nbed,:));

color1 = 'k';
color2 = 'g';
color3 = 'r';
fs1 = 7;
pagscl = 0.65;
tickspc = 1;
airng = 20;
turbidrng = 4;
prng = 0.7; 
lnw = 1.5;

% mintur=nanmean(turbid)-turbidrng;
% maxtur=nanmean(turbid)+turbidrng;


ntdum = find(Tt>mint);
% determine temperature scale
tmprng = max([round(((max(Ttmp(ntdum))-min(Ttmp(ntdum)))/2)*10)/10 1]);
% determine wave height scale
%Hscl = min([max([round(max([H13(ntdum) Hss(ntdum) HMX(ntdum)])*1.2*100)/100 1]) 3]);
% modified next line to include spectral wave height in plot limit (Again)
% - GP 8/8/08
Hscl = min([max([round(max([H13(ntdum) Hss(ntdum) HMX(ntdum)])*1.25*100)/100 1]) 3]);  

% Bring in YSI parameters  %% try/catch added 5/31/07
if YSI==1
    try
        load('C:\KiloNalu\YSI\YSImostrecent');
    catch
        pause (5)
        try
            load('C:\KiloNalu\YSI\YSImostrecent');
        catch
            disp('Failed to load YSImostrecent')
            YSI=0;
        end
    end     
end
if LISST==1
    try
        load('C:\KiloNalu\LISST\LISSTmostrecent');
    catch
        pause (5)
        try
             load('C:\KiloNalu\LISST\LISSTmostrecent');
        catch
            disp('Failed to load LISSTmostrecent')
            LISST=0;
        end
    end    
end
if TChain==1
    try
        load('C:\KiloNalu\TChain\TCmostrecent');
    catch
        pause (5)
        try
            load('C:\KiloNalu\TChain\TCmostrecent');
        catch
            disp('Failed to load TCmostrecent')
            TChain=0;
        end
    end    
end
% if YSI==1
%     load('C:\KiloNalu\YSI\YSImostrecent');
% end
% if LISST==1
%     load('C:\KiloNalu\LISST\LISSTmostrecent');
% end
% if TChain==1   % Bring in TC parameters
%   load('C:\KiloNalu\TChain\TCmostrecent');
% end
%mint = max([min(Tt) max(Tt)-trng/24]);
%maxt = max(Tt)+1/24;
% Low frequency data
vsiz = 1;
f1 = figure(1);
velscl = 0.3; %round(max(max([abs(VVE) abs(VVN)]))*1.1*100)/100;
set(gcf,'position',  [15   250  pagscl*[700   850]]);
clf
%note that suptitle causes problems with axes positions if its at the end
%of the figure commands
hst = suptitle(suptext);
set(hst, 'fontsize',8,'position',[0.5 -0.025 0 ]);
subplot(5,1,1)
imagesc(Tt,-Z,VVE,velscl*[-1 1])
axis([mint maxt zlm])
xtick = get(gca,'Xtick');
datstyle = 15;
if mint == max(Tt)-trng/24
    if trng > 72
        xtick = [xtick(1):tickspc:xtick(end)];
        datstyle = 6;
    else
        xtick = [xtick(1):tickspc/2:xtick(end)];
    end
end
set(gca,'xtick',xtick);
set(gca,'xticklabel','');
title('East Velocity (m/s)','fontsize',fs1)
ylabel('depth (m)','fontsize',fs1);
set(gca,'ydir','normal');
dum = get(gca,'position');
%set(gca,'position',[dum(1:3) vsiz*dum(4)])
grid
colorbar;
set(gca,'fontsize',fs1)

set(gca,'ydir','normal');
subplot(5,1,2)
imagesc(Tt,-Z,VVN,velscl*[-1 1])
axis([mint maxt zlm])
set(gca,'xtick',xtick);
set(gca,'xticklabel','');
title('North Velocity (m/s)','fontsize',fs1)
ylabel('depth (m)','fontsize',fs1);
set(gca,'ydir','normal');
grid
dum = get(gca,'position');
%set(gca,'position',[dum(1:3) vsiz*dum(4)])
colorbar;
set(gca,'fontsize',fs1)

subplot(5,1,3)
imagesc(Tt,-Z,VVU,0.025*[-1 1])
axis([mint maxt zlm])
set(gca,'xtick',xtick);
set(gca,'xticklabel','');
title('Vertical Velocity (m/s)','fontsize',fs1)
ylabel('depth (m)','fontsize',fs1);
set(gca,'ydir','normal');
grid
colorbar;
dum = get(gca,'position');
%set(gca,'position',[dum(1:3) vsiz*dum(4)])
set(gca,'fontsize',fs1)

subplot(5,1,4)

hp = plot(Tt,nanmean(VVE,1),color1,Tt,nanmean(VVN,1),color2);
set(hp,'linewidth',lnw)
hold on
hp = plot(Tt(end),nanmean(VVE(:,end),1),[color1 'o'],Tt(end),nanmean(VVN(:,end),1),[color2 'o']);
set(hp,'linewidth',lnw)
axis([mint maxt velscl*[-1 1] ])
set(gca,'xtick',xtick);
set(gca,'xticklabel','');
title('Depth Averaged Current (m/s): East-black, North-green','fontsize',fs1)
ylabel('m/s','fontsize',fs1);
dum2 = get(gca,'position');
set(gca,'position',[dum2(1:2) dum(3) vsiz*dum(4)])
grid
set(gca,'fontsize',fs1)

subplot(5,1,5)
plot(Tf,Pf-D,'color',.5*[1 1 1],'linewidth',2)
hold on
plot(Tt,Pp-D,color1,'linewidth',lnw)
plot(Tt(end),Pp(end)-D,[color1 'o'])
% hp = plot(Tt,Pp-D,color1);
% set(hp,'linewidth',1.5)
% hold on
% hp = plot(Tt(end),Pp(end)-D,[color1 'o']);
% set(hp,'linewidth',1.5)
%
axis([mint maxt prng*[-1 1] ])
set(gca,'xtick',xtick);
datetick('x',datstyle,'keeplimits','keepticks');
title('Tidal Amplitude (m)','fontsize',fs1)
ylabel('m','fontsize',fs1);
xlabel('time (HST)','fontsize',fs1);
dum2 = get(gca,'position');
set(gca,'position',[dum2(1:2) dum(3) vsiz*dum(4)])
grid
set(gca,'fontsize',fs1)

pos = get(gcf,'position');
set(gcf,'paperunits','points','paperposition',[150 150 pagscl*pos(3:4)]);


% Velocity Profile
%velscl = round(max(max(abs(V)))*1.25*100)/100;

ndum = find(Tt>Tt(end)-4/24);
ndum1 = find(Tt>Tt(end)-1);
f2 = figure(2);
set(gcf,'position',[625   285   [325   460]])
clf
subplot(2,1,1)
for jj = 1:length(ndum)
    Vdum = real((VVE(:,ndum(jj)).^2+VVN(:,ndum(jj)).^2).^0.5);
    plot(Vdum.*sign(VVE(:,ndum(jj))),-Z0,'color',0.8*[1 1 1])
    hold on
end
plot(V.*sign(VVE(:,end)),-Z0,color1)
plot(V.*sign(VVE(:,end)),-Z0,[color1 '.']);
plot(V(end).*sign(VVE(end,end)),-Z0(end),[color3 'o']);
plot(V(1).*sign(VVE(1,end)),-Z0(1),[color1 'o']);
set(gca,'ydir','reverse');
axis([velscl*[-1 1] zlm])
title('Velocity profile: 20 minute avg (Prev. 4 hrs: gray)','fontsize',fs1)
xlabel('Velocity (m/s)','fontsize',fs1);
ylabel('depth (m)','fontsize',fs1);
set(gca,'ydir','normal');
grid
set(gca,'fontsize',fs1)

subplot(2,1,2)
plot(VVE(end,ndum1),VVN(end,ndum1),'o','color',0.8*[1 1 1])
hold on
plot(VVE(:,end),VVN(:,end),'w.')
line(velscl*[-1 1],[0 0],'color',0.5*[1 1 1])
line([0 0],velscl*[-1 1],'color',0.5*[1 1 1])
plot(VVE(:,end),VVN(:,end),'w.',VVE(:,end),VVN(:,end),color1)
plot(VVE(1,end),VVN(1,end),[color1 'o'],VVE(end,end),VVN(end,end),[color3 'o'])
xlabel('east velocity (m/s)','fontsize',fs1);
ylabel('north velocity (m/s)','fontsize',fs1);
set(gca,'dataaspectratio',[1 1 1]);
axis(velscl*[-1 1 -1 1]);
set(gca,'fontsize',fs1)
title('Velocity profile (Prev. 1 day: gray)','fontsize',fs1)

hst = suptitle(suptext);
set(hst, 'fontsize',8)
pos = get(gcf,'position');
set(gcf,'paperunits','points','paperposition',[150 150 pagscl*pos(3:4)]);


% Water quality
    f3 = figure(3);
    clf
    set(gcf,'position',  [15   50  pagscl*[700   750]]);
    ai = nanmean(AIs,1);
    set(gca,'ydir','normal');
    hst = suptitle(suptext);
    set(hst, 'fontsize',8,'position',[0.5 -0.025 0 ]);


if YSI
    DOoffset = 0;
    set(gcf,'position',  [15   50  pagscl*[700   850]]);
    aimin=round(nanmean(ai,2)-airng);
    aimax=round(nanmean(ai,2)+airng);

    subplot(5,1,1) %color plot of AI vs. depth
    imagesc(Tt,-Z,AIs,[nanmean(ai,2)-airng nanmean(ai,2)+airng])
    axis([mint maxt zlm])
    set(gca,'xtick',xtick);
    set(gca,'xticklabel','');
    title('acoustic intensity (counts)','fontsize',fs1)
    ylabel('depth (m)','fontsize',fs1);
    set(gca,'ydir','normal');
    grid
    colorbar;
    dum = get(gca,'position');
    set(gca,'fontsize',fs1)
    
    subplot(5,1,2);  % YSI turbidity and depth averaged AI
    [AX,H1,H2]=plotyy(YSItime,YSImostrecent(:,8),Tt,ai);
    hold on
    plot(Tt,AIb,color1);
    axes(AX(1));axis([mint maxt -1 3]);
    axes(AX(2));axis([mint maxt [nanmean(ai,2)-airng nanmean(ai,2)+airng] ]);
    set(AX(1),'xtick',xtick);set(AX(2),'xtick',xtick);
    set(AX(1),'xticklabel','');set(AX(2),'xticklabel','');
    title('acoustic intensity (counts) and turbidity (NTU)','fontsize',fs1);
    axes(AX(1));dum2 = get(gca,'position');set(gca,'position',[dum2(1:2) dum(3:4)*1.03])
    axes(AX(2));set(gca,'position',[dum2(1:2) dum(3:4)*1.03]);
    axes(AX(1));ylabel('NTU','fontsize',fs1);
    set(gca,'ytick',[0 1 2 3 ],'fontsize',fs1);
    %set(AX(1),'yticklabel','')
    axes(AX(2));ylabel('counts','fontsize',fs1);set(gca,'ytick',[aimin round(aimin+((aimax-abs(aimin))/2)) aimax-1]);
    set(gca,'fontsize',fs1)
    grid on
    
    subplot(5,1,3) % dissolved oxygen and pH
    [AX,H1,H2]=plotyy(YSItime,YSImostrecent(:,9)+DOoffset,YSItime,YSImostrecent(:,6));
    axes(AX(1));axis([mint maxt 85 110]);
    axes(AX(2));axis([mint maxt 8.0 8.4]);
    set(AX(1),'xtick',xtick);set(AX(2),'xtick',xtick);
    set(AX(1),'xticklabel','');set(AX(2),'xticklabel','');
    title('Dissolved Oxygen and pH','fontsize',fs1)
    axes(AX(1));ylabel('DO (%)','fontsize',fs1);
    set(gca,'ytick',[90 100 110]);
    %set(AX(1),'yticklabel','')
    axes(AX(2));ylabel('pH','fontsize',fs1);
    set(gca,'ytick',[8.0 8.1 8.2 8.3 8.4]);
    %set(AX(2),'yticklabel','')
    axes(AX(1));dum2 = get(gca,'position');set(gca,'position',[dum2(1:2) dum(3:4)*1.03])
    set(gca,'fontsize',fs1)    
    axes(AX(2));set(gca,'position',[dum2(1:2) dum(3:4)*1.03]);
    grid on
    set(gca,'fontsize',fs1)
    
    subplot(5,1,4)
    [AX,H1,H2] = plotyy(YSItime,YSImostrecent(:,5),YSItime,YSImostrecent(:,1));
    axes(AX(1));    
    axis([mint maxt 35 36]);
    axes(AX(2));
    axis([mint maxt [nanmean(Ttmp,2)-tmprng nanmean(Ttmp,2)+tmprng] ])
    %axis([mint maxt 24 26 ]);
    set(AX(1),'xtick',xtick);set(AX(1),'ytick',[35 35.5 36]);set(AX(2),'xtick',xtick);
    set(AX(1),'xticklabel','');set(AX(2),'xticklabel','');set(gca,'ytick',[24 25 26]);
    title('Temperature (ADCP: red, YSI: green) and Salinity (ppt)','fontsize',fs1)
    axes(AX(1));ylabel('ppt','fontsize',fs1);%set(gca,'ytick',[34.5 35 35.5]);
    axes(AX(2));ylabel('deg C','fontsize',fs1);%
    hold on;
    plot(Tt,Ttmp,color3);
    axes(AX(1));dum2 = get(gca,'position');set(gca,'position',[dum2(1:2) dum(3:4)*1.03]);
    set(gca,'fontsize',fs1)
    axes(AX(2));set(gca,'position',[dum2(1:2) dum(3:4)*1.03]);
    grid on
    set(gca,'fontsize',fs1)
    
    subplot(5,1,5)
    plot(Tf,Pf-D,'color',.5*[1 1 1],'linewidth',2)
    hold on
    plot(Tt,Pp-D,color1,'linewidth',1)
    plot(Tt(end),Pp(end)-D,[color1 'o'])
%     plot(Tt,Pp-D,color1)
%     hold on
%     plot(Tt(end),Pp(end)-D,[color1 'o'])
%     %
    axis([mint maxt prng*[-1 1] ])
    set(gca,'xtick',xtick);
    datetick('x',datstyle,'keeplimits','keepticks');
    title('Tidal Amplitude (m)','fontsize',fs1)
    ylabel('m','fontsize',fs1);
    xlabel('time (HST)','fontsize',fs1);
    dum2 = get(gca,'position');
    set(gca,'position',[dum2(1:2) dum(3:4)*1.03])
    grid
    set(gca,'fontsize',fs1)
    set(gca,'position',[dum2(1:2) dum(3:4)*1.03])
   
    pos = get(gcf,'position');
    set(gcf,'paperunits','points','paperposition',[150 150 pagscl*pos(3:4)]);

f6 = figure(6);
clf;
clims = [0 0.01];
set(gcf,'position',  [700   250  pagscl*[700   600]]);
%hs1 = subplot(3,1,1);
hp =  plot(YSImostrecent(:,5),YSImostrecent(:,1),'k.');
hold on
ndum = find(YSItime>Tt(end)-0.25); % last 6 hours of data
plot(YSImostrecent(ndum,5),YSImostrecent(ndum,1),'r.');
grid
axis([ 35 36 [nanmean(Ttmp,2)-tmprng nanmean(Ttmp,2)+tmprng]]);
title(['TS diagram for '  datestr(YSItime(1),'mm-dd-yyyy HH:MM') ' - ' datestr(YSItime(end),'mm-dd-yyyy HH:MM')],'fontsize',fs1);
xlabel('Salinity (PSU)','fontsize',fs1);
ylabel('Temperature (deg C)','fontsize',fs1);
set(gca,'fontsize',fs1)
    pos = get(gcf,'position');
    set(gcf,'paperunits','points','paperposition',[150 150 pagscl*pos(3:4)]);


else
    subplot(4,1,1)
    imagesc(Tt,-Z,AIs,[nanmean(ai,2)-airng nanmean(ai,2)+airng])
    axis([mint maxt zlm])
    set(gca,'xtick',xtick);
    set(gca,'xticklabel','');
    title('acoustic intensity (counts)','fontsize',fs1)
    ylabel('depth (m)','fontsize',fs1);
    set(gca,'ydir','normal');
    grid
    colorbar;
    dum = get(gca,'position');
    set(gca,'fontsize',fs1)

    subplot(4,1,2)
    hp = plot(Tt,ai,color1);
    set(hp,'linewidth',lnw)
    hold on
    hp = plot(Tt(end),ai(end),[color1 'o']);
    set(hp,'linewidth',lnw)
    axis([mint maxt [nanmean(ai,2)-airng nanmean(ai,2)+airng] ])
    set(gca,'xtick',xtick);
    set(gca,'xticklabel','');
    title('depth averaged acoustic intensity','fontsize',fs1);
    ylabel('counts','fontsize',fs1)
    grid
    dum2 = get(gca,'position');
    set(gca,'position',[dum2(1:2) dum(3:4)])
    set(gca,'fontsize',fs1)
    
    subplot(4,1,3)
    hp = plot(Tt,Ttmp,color1);
    set(hp,'linewidth',lnw)
    hold on
    hp = plot(Tt(end),Ttmp(end),[color1 'o']);
    set(hp,'linewidth',lnw)
    axis([mint maxt [nanmean(Ttmp,2)-tmprng nanmean(Ttmp,2)+tmprng] ])
    % if mint == max(Tt)-trng/24
    %     xtick = get(gca,'xtick');
    %     set(gca,'xtick',[xtick(1):0.5:xtick(end)]);
    % end
    set(gca,'xtick',xtick);
    set(gca,'xticklabel','');
    title('water temperature (bottom)','fontsize',fs1);
    ylabel('deg C','fontsize',fs1);
    %xlabel('time','fontsize',fs1);
    dum2 = get(gca,'position');
    set(gca,'position',[dum2(1:2) dum(3:4)])  % correct position for colorbar
    grid
    set(gca,'fontsize',fs1)
    
    subplot(4,1,4)
    plot(Tf,Pf-D,'color',.5*[1 1 1],'linewidth',2)
    hold on
    plot(Tt,Pp-D,color1,'linewidth',lnw)
    plot(Tt(end),Pp(end)-D,[color1 'o'])
%     hp = plot(Tt,Pp-D,color1);
%     set(hp,'linewidth',lnw)
%     hold on
%     hp = plot(Tt(end),Pp(end)-D,[color1 'o']);
%     set(hp,'linewidth',lnw)
    %
    axis([mint maxt prng*[-1 1] ])
    set(gca,'xtick',xtick);
    datetick('x',datstyle,'keeplimits','keepticks');
    title('Tidal Amplitude (m)','fontsize',fs1)
    ylabel('m','fontsize',fs1);
    xlabel('time (HST)','fontsize',fs1);
    dum2 = get(gca,'position');
    set(gca,'position',[dum2(1:2) dum(3:4)])
    grid
    set(gca,'fontsize',fs1)
    
    pos = get(gcf,'position');
    set(gcf,'paperunits','points','paperposition',[150 150 pagscl*pos(3:4)]);
end


% Waves
f4 = figure(4);
clf
clims = [0 10e-3];

set(gcf,'position',[   550    50   pagscl*[700   750]]);
hst = suptitle(suptext);
set(hst, 'fontsize',8,'position',[0.5 -0.025 0 ]);

subplot(4,1,1);
hp = plot(Tt,HMX,'r',Tt(end),HMX(end),'ro');
set(hp,'linewidth',lnw)
hold on
hp = plot(Tt,Hss,color1,Tt,H13,color2);
set(hp,'linewidth',lnw)
hp = plot(Tt(end),Hss(end),[color1 'o'],Tt(end),H13(end),[color2 'o']);
set(hp,'linewidth',lnw)
axis([mint maxt 0 Hscl]);
set(gca,'xtick',xtick);
set(gca,'xticklabel','');
title('Sig Wv Hght: spectral (black), zero-xing (grn); Max Wv Hght (red)','fontsize',fs1)
ylabel('meters','fontsize',fs1)
grid
set(gca,'fontsize',fs1)
%legend(hp,'Spect','Zero-xing','Location','best')

subplot(4,1,2);
hp = plot(Tt,Tps,color1);%,Tt,TZC,color2);
set(hp,'linewidth',lnw)
hold on
hp = plot(Tt(end),Tps(end),[color1 'o']);%Tt(end),TZC(end),[color2 'o']);
set(hp,'linewidth',lnw)
axis([mint maxt 4 25]);
set(gca,'xtick',xtick);
set(gca,'xticklabel','');
title('Dom Wave Period','fontsize',fs1);
ylabel('seconds','fontsize',fs1);
grid
set(gca,'fontsize',fs1)
%legend(hp,'Spect','Zero-xing','Location','best')

subplot(4,1,3);
hp = plot(Tt,DTps,color1,Tt,Dps,color3); %Tt,nanmean(ALPHs),color2,
set(hp,'linewidth',lnw)
hold on
hp = plot(Tt(end),DTps(end),[color1 'o'],Tt(end),Dps(end),[color3 'o']);
set(hp,'linewidth',lnw)
axis([mint maxt 140 220]);
set(gca,'xtick',xtick);
set(gca,'xticklabel','');
%datetick('x',datstyle,'keeplimits','keepticks');
%legend(hp,'Spect peak','Zero-xing','Dmnt (spect)','Location','Southwest');
title('Wave Direction: Spectral peak (black), dominant (red)','fontsize',fs1);
ylabel('degrees','fontsize',fs1);
%xlabel('time','fontsize',fs1)
grid
set(gca,'fontsize',fs1)

subplot(4,1,4);
maxvb = min([0.4 nanmax(VB)+0.1]);
hp = plot(Tt,VB,color1);
set(hp,'linewidth',lnw)
hold on
plot(Tt(end),VB(end),[color1 'o']);

hp = plot(Tt,VBC,color2);
set(hp,'linewidth',lnw)
hold on
plot(Tt(end),VBC(end),[color2 'o']);

axis([mint maxt 0 maxvb]);
% if mint == max(Tt)-trng/24  % if there are 3 days of data
%     xtick = get(gca,'Xtick');
%     set(gca,'Xtick',[xtick(1):1/2:xtick(end)]);
% else
%     set(gca,'Xtick',[xtick])
% end
set(gca,'Xtick',[xtick])
datetick('x',datstyle,'keeplimits','keepticks');
title('Near Bed Significant Velocity: Total-blk, Currents-grn','fontsize',fs1);
ylabel('m/s','fontsize',fs1);
xlabel('time (HST)','fontsize',fs1)
grid
set(gca,'fontsize',fs1)
%set(gca,'position',[0.1300 0.1037 0.7750 0.14])

pos = get(gcf,'position');
set(gcf,'paperunits','points','paperposition',[150 150 pagscl*pos(3:4)]);


% f5 = figure(5);
% set(gcf,'position',[800    50   0.75*[480   400]])
% hl = polar(-Dps(end)*pi/180*ones(1,2)-pi/2,0.4*[-1 1]);
% hold on
% set(hl,'linewidth',2,'color','r');
% h2 = polar(-DTps(end)*pi/180*ones(1,2)-pi/2,0.4*[-1 1]);
% set(h2,'linewidth',2,'color',color2);
% h3 = polar([0:1:360]*pi/180,ones(size([0:1:360]))/Tps(end),'r');
% plotspecmod(SMout,4,clims);
% title(['Directional Spectra for '  datestr(Tt(end)-PROCINT/60/24/2) ' - ' datestr(Tt(end)+PROCINT/60/24/2,13)],'fontsize',fs1);
% set(gca,'fontsize',fs1)
% 
% pos = get(gcf,'position');
% set(gcf,'paperunits','points','paperposition',[150 250 pos(3:4)]);


f5 = figure(5);
clf;
clims = [0 0.01];
set(gcf,'position',  [700   50  pagscl*[700   850]]);
hs1 = subplot(3,1,1);
hl = polar(-Dps(end)*pi/180*ones(1,2)-pi/2,0.4*[-1 1]);
hold on
set(hl,'linewidth',2,'color','r');
h2 = polar(-DTps(end)*pi/180*ones(1,2)-pi/2,0.4*[-1 1]);
set(h2,'linewidth',2,'color',color2);
h3 = polar([0:1:360]*pi/180,ones(size([0:1:360]))/Tps(end),'r');
plotspecmod(SMout,4,clims);
title(['Directional Spectra for '  datestr(Tt(end)-PROCINT/60/24/2,'mm-dd-yyyy HH:MM') ' - ' datestr(Tt(end)+PROCINT/60/24/2,'HH:MM')],'fontsize',fs1);
set(gca,'fontsize',fs1)
%set(get(gca,'children'),'fontsize',fs1)
dum1 = get(hs1,'position');

hs2 = subplot(3,1,2);
sflim = 0.4*180*clims;
imagesc(Tt,f,SPF,sflim);
    title('Spectral intensity vs frequency','fontsize',fs1)
    ylabel('frequency','fontsize',fs1);
    set(gca,'ydir','normal');
    set(gca,'xtick',xtick);
    set(gca,'xticklabel','');
    grid
xlim([mint maxt]);
ylim([0 0.3]);

% hold on
% plot(Tt,1./Tps,'r')

    colorbar;
set(gca,'fontsize',fs1)
    
hs3 = subplot(3,1,3);
sdlim = 0.5*clims*(f(end)-f(1));
imagesc(Tt,dirs,SPD,sdlim);
    set(gca,'ydir','normal');
    grid
set(gca,'xtick',xtick);
datetick('x',datstyle,'keeplimits','keepticks');
title('Spectral intensity vs source direction','fontsize',fs1)
ylabel('direction (deg)','fontsize',fs1);
xlabel('time (HST)','fontsize',fs1);
xlim([mint maxt]);
ylim([140 230]);
colorbar;
set(gca,'fontsize',fs1)


set(hs1,'position',[dum2(1) 0.55 0.7 0.41])
set(hs2,'position',[dum2(1) 0.28 0.7 0.16])
set(hs3,'position',[dum2(1) 0.08 0.7 0.16])

pos = get(gcf,'position');
set(gcf,'paperunits','points','paperposition',[150 250 pagscl*pos(3:4)]);


if LISST==1
    f7 = figure(7);
    clf;
    set(gcf,'position',[300   360   640   400])
    subplot(2,1,1)
    imagesc(LISSTtime,[250:-251/32:1],LISSTmostrecent.');
    axis([mint maxt 1 250])
    set(gca,'xtick',xtick);
    set(gca,'xticklabel','');
    title(['Particle size distribution for '  datestr(LISSTtime(1),'mm-dd-yyyy HH:MM') ' - ' datestr(LISSTtime(end),'mm-dd-yyyy HH:MM')],'fontsize',fs1);
    ylabel('Particle Radius','fontsize',fs1);
    set(gca,'ydir','normal');
    grid
    colorbar;
    dum = get(gca,'position');
    set(gca,'fontsize',fs1)

    subplot(2,1,2)
    plot(LISSTtime,sum(LISSTmostrecent'));
    mxL = max(sum(LISSTmostrecent'))*1.2;
    axis([mint maxt 1 mxL])
    set(gca,'xtick',xtick);
    datetick('x',datstyle,'keeplimits','keepticks');
    title(['Total particle load?'],'fontsize',fs1);
    %ylabel('Particle Radius','fontsize',fs1);
    set(gca,'ydir','normal');
    grid
    dum2 = get(gca,'position');
    set(gca,'position',[dum2(1:2) dum(3:4)])
    set(gca,'fontsize',fs1)
    
    pos = get(gcf,'position');
    set(gcf,'paperunits','points','paperposition',[150 250 pagscl*pos(3:4)]);

end


if TChain==1  % Create figure 8
    tczlm=[zlm(1) zlm(2)-3];

f8 = figure(8);
    clf
    set(gcf,'position',  [150   100  pagscl*[700   750]]);
    set(gca,'ydir','normal');
%note that suptitle causes problems with axes positions if its at the end
%of the figure commands
    hst = suptitle(suptext);
    set(hst, 'fontsize',8,'position',[0.5 -0.025 0 ]);
    TCrng = [nanmean(TCbot,1)-0.7*tmprng nanmean(TCtop,1)+tmprng];

    if YSI
%%%
    subplot(4,1,1)  % color plot of TC temperatures vs depth
    imagesc(TCt,MABgrid-D,TCgrid),axis xy
    axis([mint maxt tczlm])
    caxis(TCrng);
    xtick = get(gca,'Xtick');
    datstyle = 15;
    if mint == max(Tt)-trng/24
        if trng > 72
            xtick = [xtick(1):tickspc:xtick(end)];
            datstyle = 6;
        else
            xtick = [xtick(1):tickspc/2:xtick(end)];
        end
    end
    set(gca,'xtick',xtick);
    set(gca,'xticklabel','');
    title('Temperature (^oC)','fontsize',fs1)
    ylabel('depth (m)','fontsize',fs1);
    set(gca,'ydir','normal');
    dum = get(gca,'position');
    %set(gca,'position',[dum(1:3) vsiz*dum(4)])
    grid
    colorbar;
    dum = get(gca,'position');
    set(gca,'fontsize',fs1)
    set(gca,'ydir','normal');

%%%
    subplot(4,1,2);  % top bottom and mean TC temperatures
    plot(TCt,TCtop,color3,'linewidth',lnw),hold on
    plot(TCt,TCbot,color2,'linewidth',lnw)
    plot(TCt,TCmean,color1,'linewidth',lnw)
    axis([mint maxt TCrng])
%    axis([mint maxt .99*min(TCbot) 1.01*max(TCtop)])
    set(gca,'xtick',xtick);
    set(gca,'xticklabel','');
    title('Temperature (Topmost:red, Bottom:green, Mean:black)','fontsize',fs1)
    ylabel('^oC','fontsize',fs1);
    set(gca,'ydir','normal');
    grid on
    dum2 = get(gca,'position');
    set(gca,'position',[dum2(1:2) dum(3:4)])

%%%
    subplot(4,1,3)
    [AX,H1,H2] = plotyy(YSItime,YSImostrecent(:,5),YSItime,YSImostrecent(:,1));
    axes(AX(1));    
    axis([mint maxt 35 36]);
    axes(AX(2));
    axis([mint maxt [nanmean(Ttmp,2)-tmprng nanmean(Ttmp,2)+tmprng] ])
    %axis([mint maxt 24 26 ]);
    set(AX(1),'xtick',xtick);set(AX(1),'ytick',[35 35.5 36]);set(AX(2),'xtick',xtick);
    set(AX(1),'xticklabel','');set(AX(2),'xticklabel','');set(gca,'ytick',[24 25 26]);
    title('Temperature (ADCP: red, YSI: green) and Salinity (ppt)','fontsize',fs1)
    axes(AX(1));ylabel('ppt','fontsize',fs1);%set(gca,'ytick',[34.5 35 35.5]);
    axes(AX(2));ylabel('deg C','fontsize',fs1);%
    hold on;
    plot(Tt,Ttmp,color3);
    axes(AX(1));dum2 = get(gca,'position');
    set(gca,'position',[dum2(1:2) dum(3:4)]);
    set(gca,'fontsize',fs1)
    axes(AX(2));
    set(gca,'position',[dum2(1:2) dum(3:4)]);
    grid on
    set(gca,'fontsize',fs1)

%%%
    subplot(4,1,4)
    plot(Tf,Pf-D,'color',.5*[1 1 1],'linewidth',2)
    hold on
    plot(Tt,Pp-D,color1,'linewidth',1)
    plot(Tt(end),Pp(end)-D,[color1 'o'])
    axis([mint maxt prng*[-1 1] ])
    set(gca,'xtick',xtick);
    datetick('x',datstyle,'keeplimits','keepticks');
    title('Tidal Amplitude (m)','fontsize',fs1)
    ylabel('m','fontsize',fs1);
    xlabel('time (HST)','fontsize',fs1);
    dum2 = get(gca,'position');
    set(gca,'position',[dum2(1:2) dum(3:4)])
    grid
    set(gca,'fontsize',fs1)
    set(gca,'position',[dum2(1:2) dum(3:4)])

    pos = get(gcf,'position');
    set(gcf,'paperunits','points','paperposition',[150 150 pagscl*pos(3:4)]);

    else
    subplot(3,1,1)  % color plot of TC temperatures vs depth
    imagesc(TCt,MABgrid-D,TCgrid),axis xy
    caxis(TCrng);
    axis([mint maxt tczlm])
    xtick = get(gca,'Xtick');
    datstyle = 15;
    if mint == max(Tt)-trng/24
        if trng > 72
            xtick = [xtick(1):tickspc:xtick(end)];
            datstyle = 6;
        else
            xtick = [xtick(1):tickspc/2:xtick(end)];
        end
    end
    set(gca,'xtick',xtick);
    set(gca,'xticklabel','');
    title('Temperature (^oC)','fontsize',fs1)
    ylabel('depth (m)','fontsize',fs1);
    set(gca,'ydir','normal');
    dum = get(gca,'position');
    %set(gca,'position',[dum(1:3) vsiz*dum(4)])
    grid
    colorbar;
    dum = get(gca,'position');
    set(gca,'fontsize',fs1)
    set(gca,'ydir','normal');

%%%
    subplot(3,1,2);  % top bottom and mean TC temperatures
    plot(TCt,TCtop,color3,'linewidth',lnw),hold on
    plot(TCt,TCbot,color2,'linewidth',lnw)
    plot(TCt,TCmean,color1,'linewidth',lnw)
    %axis([mint maxt .99*min(TCbot) 1.01*max(TCtop)])
    axis([mint maxt TCrng])
    set(gca,'xtick',xtick);
    set(gca,'xticklabel','');
    title('Temperature (Topmost:red, Bottom:green, Mean:black)','fontsize',fs1)
    ylabel('^oC','fontsize',fs1);
    set(gca,'ydir','normal');
    grid on
    dum2 = get(gca,'position');
    set(gca,'position',[dum2(1:2) dum(3:4)])

%%%
    subplot(3,1,3)
    plot(Tf,Pf-D,'color',.5*[1 1 1],'linewidth',2)
    hold on
    plot(Tt,Pp-D,color1,'linewidth',1)
    plot(Tt(end),Pp(end)-D,[color1 'o'])
    axis([mint maxt prng*[-1 1] ])
    set(gca,'xtick',xtick);
    datetick('x',datstyle,'keeplimits','keepticks');
    title('Tidal Amplitude (m)','fontsize',fs1)
    ylabel('m','fontsize',fs1);
    xlabel('time (HST)','fontsize',fs1);
    dum2 = get(gca,'position');
    set(gca,'position',[dum2(1:2) dum(3:4)])
    grid
    set(gca,'fontsize',fs1)
    set(gca,'position',[dum2(1:2) dum(3:4)])

    pos = get(gcf,'position');
    set(gcf,'paperunits','points','paperposition',[150 150 pagscl*pos(3:4)]);
    end
end