clear
flg = 0;
filnam = 'KILONALU.cdf';
 fid = fopen(filnam,'rt');
% fread(fid, 600, 'uint8=>char', 'ieee-le');
 dum = fgets(fid);
 dum = fgets(fid);
% d = fscanf(fid,'"%s","%s",%g,%g,%g,%g,%g,%g,%g,%g,%g',inf);
a = 1;
while flg == 0
    d = fgets(fid);
    if d == -1
        flg=1;
        break
    end
    t(a) = datenum([d(2:9),' ',d(13:20)]);
    temp(a) = str2num(d(23:27));
    spcond(a) = str2num(d(29:34));
    cond(a) = str2num(d(36:41));
    slinty(a) = str2num(d(43:47));
    docent(a) = str2num(d(49:52));
    doconc(a) = str2num(d(54:57));
    pH(a) = str2num(d(59:62));
    turb(a) = str2num(d(64:end));
%     elseif 0<d(67:end)<10
%     turb(a,:) = ['00' d(67)];
%     elseif d(67:end)>10
%     turb(a,:) = d(67:end);
%     end
    a = a+1;    
end
i=1;
j=1;
while j<length(t)-90
    time(i) = mean(t(j:j+90));
    turbidity(i) = mean(turb(j:j+90));
    Temp(i) = mean(temp(j:j+90));
    SPcond(i) = mean(spcond(j:j+90));
    salinity(i) = mean(slinty(j:j+90));
    DOcent(i) = mean(docent(j:j+90));
    pHavg(i) = mean(pH(j:j+90));
    i = i+1;
    j = j+90;
end
clear i j t temp spcond slnty docent doconc pH turb flg a d dum fid file filnam
