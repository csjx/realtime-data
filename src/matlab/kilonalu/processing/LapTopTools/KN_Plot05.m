% KN_plot05 - needs trng input
% calculate current direction versus depth
theta = atan(VVE./VVN)*180/pi;
ndum = find(theta<0);
theta(ndum) = 360+theta(ndum);
YSI=1; %If YSI is connected, change this to 1
% mask out velocities near and above the surface
ndum = find(Z<1);

VVE(ndum, :) = VVE(ndum,:)*NaN;
VVN(ndum, :) = VVN(ndum,:)*NaN;
VVU(ndum, :) = VVU(ndum,:)*NaN;
AIs(ndum, :) = AIs(ndum,:)*NaN;

% plot all velocities
V = real((VVE(:,end).^2+VVN(:,end).^2).^0.5);

color1 = 'k';
color2 = 'g';
color3 = 'r';
fs1 = 8;
pagscl = 0.65;
tickspc = 1;
airng = 40;
turbidrng = 10;
mintur=nanmean(turbid)-turbidrng;
maxtur=nanmean(turbid)+turbidrng;



ntdum = find(Tt>mint);
% determine temperature scale
tmprng = max([round(((max(Ttmp(ntdum))-min(Ttmp(ntdum)))/2)*10)/10 1]);
% determine wave height scale
Hscl = min([max([round(max([H13(ntdum) Hss(ntdum)])*1.2*100)/100 1]) 3]);

% Bring in YSI parameters
if YSI==1
    load YSImostrecent
    mintur=nanmean(turbid)-turbidrng;
    maxtur=nanmean(turbid)+turbidrng;
end
%mint = max([min(Tt) max(Tt)-trng/24]);
%maxt = max(Tt)+1/24;
% Low frequency data
f1 = figure(1);
velscl = 0.3; %round(max(max([abs(VVE) abs(VVN)]))*1.1*100)/100;
set(gcf,'position',  [15   300  pagscl*[700   750]]);
clf
%note that suptitle causes problems with axes positions if its at the end
%of the figure commands
hst = suptitle(suptext);
set(hst, 'fontsize',8,'position',[0.5 -0.025 0 ]);
subplot(4,1,1)
imagesc(Tt,-Z,VVE,velscl*[-1 1])
axis([mint maxt zlim])
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
grid
colorbar;

set(gca,'ydir','normal');
subplot(4,1,2)
imagesc(Tt,-Z,VVN,velscl*[-1 1])
axis([mint maxt zlim])
set(gca,'xtick',xtick);
set(gca,'xticklabel','');
title('North Velocity (m/s)','fontsize',fs1)
ylabel('depth (m)','fontsize',fs1);
set(gca,'ydir','normal');
grid
colorbar;

subplot(4,1,3)
imagesc(Tt,-Z,VVU,0.05*[-1 1])
axis([mint maxt zlim])
set(gca,'xtick',xtick);
set(gca,'xticklabel','');
title('Vertical Velocity (m/s)','fontsize',fs1)
ylabel('depth (m)','fontsize',fs1);
set(gca,'ydir','normal');
grid
colorbar;
dum = get(gca,'position');

subplot(4,1,4)
plot(Tt,Pp/1000-D,color1)
hold on
plot(Tt(end),Pp(end)/1000-D,[color1 'o'])
axis([mint maxt 0.8*[-1 1] ])
set(gca,'xtick',xtick);
datetick('x',datstyle,'keeplimits','keepticks');
title('Tidal Amplitude (m)','fontsize',fs1)
ylabel('m','fontsize',fs1);
xlabel('time','fontsize',fs1);
dum2 = get(gca,'position');
set(gca,'position',[dum2(1:2) dum(3:4)])
grid
ch = get(gcf,'children');
set(ch,'fontsize',fs1)

pos = get(gcf,'position');
set(gcf,'paperunits','points','paperposition',[150 150 pagscl*pos(3:4)]);


% Velocity Profile
%velscl = round(max(max(abs(V)))*1.25*100)/100;

f2 = figure(2);
set(gcf,'position',[625   385   [325   460]])
clf
subplot(2,1,1)
plot(V.*sign(VVE(:,end)),z,color1)
hold on
plot(V.*sign(VVE(:,end)),z,[color1 '.']);
plot(V(end).*sign(VVE(end,end)),z(end),[color3 'o']);
plot(V(1).*sign(VVE(1,end)),z(1),[color1 'o']);
set(gca,'ydir','reverse');
axis([velscl*[-1 1] zlim])
title('Velocity profile (20 minute average)','fontsize',fs1)
xlabel('Velocity (m/s)','fontsize',fs1);
ylabel('depth (m)','fontsize',fs1);
set(gca,'ydir','normal');
grid

subplot(2,1,2)
hold on
line(velscl*[-1 1],[0 0],'color',0.5*[1 1 1])
line([0 0],velscl*[-1 1],'color',0.5*[1 1 1])
plot(VVE(:,end),VVN(:,end),'w.',VVE(:,end),VVN(:,end),color1)
plot(VVE(1,end),VVN(1,end),[color1 'o'],VVE(end,end),VVN(end,end),[color3 'o'])
xlabel('east velocity (m/s)','fontsize',fs1);
ylabel('north velocity (m/s)','fontsize',fs1);
set(gca,'dataaspectratio',[1 1 1]);
axis(velscl*[-1 1 -1 1]);
ch = get(gcf,'children');
set(ch,'fontsize',fs1)

hst = suptitle(suptext);
set(hst, 'fontsize',8)
pos = get(gcf,'position');
set(gcf,'paperunits','points','paperposition',[150 150 pagscl*pos(3:4)]);


% Water quality
if YSI
    f3 = figure(3);
    clf
    set(gcf,'position',  [15   50  pagscl*[700   750]]);
    ai = nanmean(AIs,1);
    aimin=round(nanmean(ai,2)-airng);
    aimax=round(nanmean(ai,2)+airng);
    set(gca,'ydir','normal');
    hst = suptitle(suptext);
    set(hst, 'fontsize',8,'position',[0.5 -0.025 0 ]);

    subplot(5,1,1)
    imagesc(Tt,-Z,AIs,[nanmean(ai,2)-airng nanmean(ai,2)+airng])
    axis([mint maxt zlim])
    set(gca,'xtick',xtick);
    set(gca,'xticklabel','');
    title('acoustic intensity (counts)','fontsize',fs1)
    ylabel('depth (m)','fontsize',fs1);
    set(gca,'ydir','normal');
    grid
    colorbar;
    dum = get(gca,'position');
    
    subplot(5,1,2);
    [AX,H1,H2]=plotyy(YSItime,turbid,Tt,ai);
    axes(AX(1));axis([mint maxt mintur maxtur]);
    axes(AX(2));axis([mint maxt [nanmean(ai,2)-airng nanmean(ai,2)+airng] ]);
    set(AX(1),'xtick',xtick);set(AX(2),'xtick',xtick);
    set(AX(1),'xticklabel','');set(AX(2),'xticklabel','');
    title('acoustic intensity (counts) and turbidity (NTU)','fontsize',fs1);
    axes(AX(1));dum2 = get(gca,'position');set(gca,'position',[dum2(1:2) dum(3:4)*1.03])
    axes(AX(2));set(gca,'position',[dum2(1:2) dum(3:4)*1.03]);
    axes(AX(1));ylabel('NTU','fontsize',fs1);set(gca,'ytick',[-5 0 5]);
    axes(AX(2));ylabel('counts','fontsize',fs1);set(gca,'ytick',[aimin round(aimin+((aimax-abs(aimin))/2)) aimax-1]);
    
    grid on
    
    subplot(5,1,3)
    [AX,H1,H2]=plotyy(YSItime,DOsat,YSItime,pH);
    axes(AX(1));axis([mint maxt 10 50]);
    axes(AX(2));axis([mint maxt 7 9]);
    set(AX(1),'xtick',xtick);set(AX(2),'xtick',xtick);
    set(AX(1),'xticklabel','');set(AX(2),'xticklabel','');
    title('Dissolved Oxygen and pH','fontsize',fs1)
    axes(AX(1));ylabel('DO (%)','fontsize',fs1);set(gca,'ytick',[10 30 50]);
    axes(AX(2));ylabel('pH','fontsize',fs1);set(gca,'ytick',[7 8 9]);
    axes(AX(1));dum2 = get(gca,'position');set(gca,'position',[dum2(1:2) dum(3:4)*1.03])
    axes(AX(2));set(gca,'position',[dum2(1:2) dum(3:4)*1.03]);
    grid on
    
    subplot(5,1,4)
    [AX,H1,H2] = plotyy(YSItime,sal,YSItime,temp);
    axes(AX(1));axis([mint maxt 34 36]);
    axes(AX(2));axis([mint maxt 24 26 ]);
    set(AX(1),'xtick',xtick);set(AX(2),'xtick',xtick);
    set(AX(1),'xticklabel','');set(AX(2),'xticklabel','');
    title('Temperature (ADCP in black, YSI in green) and Salinity (ppt)','fontsize',fs1)
    axes(AX(1));ylabel('ppt','fontsize',fs1);set(gca,'ytick',[34 35 36]);
    axes(AX(2));ylabel('deg C','fontsize',fs1);set(gca,'ytick',[24 25 26]);
    hold on;
    plot(Tt,Ttmp,color1);
    axes(AX(1));dum2 = get(gca,'position');set(gca,'position',[dum2(1:2) dum(3:4)*1.03]);
    axes(AX(2));set(gca,'position',[dum2(1:2) dum(3:4)*1.03]);
    grid on
    
    subplot(5,1,5)
    plot(Tt,Pp/1000-D,color1)
    hold on
    plot(Tt(end),Pp(end)/1000-D,[color1 'o'])
    axis([mint maxt 0.8*[-1 1] ])
    set(gca,'xtick',xtick);
    datetick('x',datstyle,'keeplimits','keepticks');
    title('Tidal Amplitude (m)','fontsize',fs1)
    ylabel('m','fontsize',fs1);
    xlabel('time','fontsize',fs1);
    dum2 = get(gca,'position');
    set(gca,'position',[dum2(1:2) dum(3:4)*1.03])
    grid
    ch = get(gcf,'children');
    set(ch,'fontsize',fs1)
    set(gca,'position',[dum2(1:2) dum(3:4)*1.03])
   
    pos = get(gcf,'position');
    set(gcf,'paperunits','points','paperposition',[150 150 pagscl*pos(3:4)]);

else
    f3 = figure(3);
    clf
    set(gcf,'position',  [15   50  pagscl*[700   750]]);
    ai = nanmean(AIs,1);
    set(gca,'ydir','normal');
    hst = suptitle(suptext);
    set(hst, 'fontsize',8,'position',[0.5 -0.025 0 ]);

    subplot(4,1,1)
    imagesc(Tt,-Z,AIs,[nanmean(ai,2)-airng nanmean(ai,2)+airng])
    axis([mint maxt zlim])
    set(gca,'xtick',xtick);
    set(gca,'xticklabel','');
    title('acoustic intensity (counts)','fontsize',fs1)
    ylabel('depth (m)','fontsize',fs1);
    set(gca,'ydir','normal');
    grid
    colorbar;
    dum = get(gca,'position');

    subplot(4,1,2)
    plot(Tt,ai,color1)
    hold on
    plot(Tt(end),ai(end),[color1 'o'])
    axis([mint maxt [nanmean(ai,2)-airng nanmean(ai,2)+airng] ])
    set(gca,'xtick',xtick);
    set(gca,'xticklabel','');
    title('depth averaged acoustic intensity','fontsize',fs1);
    ylabel('counts','fontsize',fs1)
    grid
    dum2 = get(gca,'position');
    set(gca,'position',[dum2(1:2) dum(3:4)])

    subplot(4,1,3)
    plot(Tt,Ttmp,color1)
    hold on
    plot(Tt(end),Ttmp(end),[color1 'o'])
    axis([mint maxt [nanmean(Ttmp,2)-tmprng nanmean(Ttmp,2)+tmprng] ])
    % if mint == max(Tt)-trng/24
    %     xtick = get(gca,'xtick');
    %     set(gca,'xtick',[xtick(1):0.5:xtick(end)]);
    % end
    set(gca,'xtick',xtick);
    set(gca,'xticklabel','');
    title('water temperature','fontsize',fs1);
    ylabel('deg C','fontsize',fs1);
    xlabel('time','fontsize',fs1);
    dum2 = get(gca,'position');
    set(gca,'position',[dum2(1:2) dum(3:4)])  % correct position for colorbar
    grid
    ch = get(gcf,'children');
    set(ch,'fontsize',fs1)

    subplot(4,1,4)
    plot(Tt,Pp/1000-D,color1)
    hold on
    plot(Tt(end),Pp(end)/1000-D,[color1 'o'])
    axis([mint maxt 0.8*[-1 1] ])
    set(gca,'xtick',xtick);
    datetick('x',datstyle,'keeplimits','keepticks');
    title('Tidal Amplitude (m)','fontsize',fs1)
    ylabel('m','fontsize',fs1);
    xlabel('time','fontsize',fs1);
    dum2 = get(gca,'position');
    set(gca,'position',[dum2(1:2) dum(3:4)])
    grid
    ch = get(gcf,'children');
    set(ch,'fontsize',fs1)

    pos = get(gcf,'position');
    set(gcf,'paperunits','points','paperposition',[150 150 pagscl*pos(3:4)]);
end


% Waves
f4 = figure(4);
clf
clims = [0 10e-3];

set(gcf,'position',[   550    50   pagscl*[600   650]]);
hst = suptitle(suptext);
set(hst, 'fontsize',8,'position',[0.5 -0.025 0 ]);

subplot(3,1,1);
hp = plot(Tt,Hss,color1,Tt,H13,color2);
hold on
plot(Tt(end),Hss(end),[color1 'o'],Tt(end),H13(end),[color2 'o']);
axis([mint maxt 0 Hscl]);
set(gca,'xtick',xtick);
set(gca,'xticklabel','');
title('significant wave height','fontsize',fs1)
ylabel('meters','fontsize',fs1)
grid
legend(hp,'Spect','Zero-xing','Location','best')

subplot(3,1,2);
hp = plot(Tt,Tps,color1,Tt,TZC,color2);
hold on
plot(Tt(end),Tps(end),[color1 'o'],Tt(end),TZC(end),[color2 'o']);
axis([mint maxt 4 25]);
set(gca,'xtick',xtick);
set(gca,'xticklabel','');
title('dominant wave period','fontsize',fs1);
ylabel('seconds','fontsize',fs1);
grid
legend(hp,'Spect','Zero-xing','Location','best')

subplot(3,1,3);
hp = plot(Tt,DTps,color1,Tt,nanmean(ALPHs),color2,Tt,Dps,color3);
hold on
plot(Tt(end),DTps(end),[color1 'o'],Tt(end),Dps(end),[color2 'o']);
axis([mint maxt 140 220]);
set(gca,'xtick',xtick);
datetick('x',datstyle,'keeplimits','keepticks');
legend(hp,'Spect peak','Zero-xing','Dmnt (spect)','Location','Southwest');
title('peak and dominant wave direction','fontsize',fs1);
ylabel('degrees','fontsize',fs1);
xlabel('time','fontsize',fs1)
grid

ch = get(gcf,'children');
set(ch,'fontsize',6)

pos = get(gcf,'position');
set(gcf,'paperunits','points','paperposition',[150 150 pagscl*pos(3:4)]);


f5 = figure(5);
set(gcf,'position',[800    50   0.75*[480   400]])
hl = polar(-Dps(end)*pi/180*ones(1,2)-pi/2,0.4*[-1 1]);
hold on
set(hl,'linewidth',2,'color','r');
h2 = polar(-DTps(end)*pi/180*ones(1,2)-pi/2,0.4*[-1 1]);
set(h2,'linewidth',2,'color',color2);
h3 = polar([0:1:360]*pi/180,ones(size([0:1:360]))/Tps(end),'r');
%plotspecmod(SMout,4,clims);
title(['Directional Spectra for '  datestr(Tt(end)-PROCINT/60/24/2) ' - ' datestr(Tt(end)+PROCINT/60/24/2,13)],'fontsize',fs1);
ch = get(gcf,'children');
set(ch,'fontsize',fs1)

pos = get(gcf,'position');
set(gcf,'paperunits','points','paperposition',[150 250 pos(3:4)]);
