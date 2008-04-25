function [voltlog]=getADAM;
% goes to ADAM status text files and copies last line
retdir=pwd;
% CSJ changed for testing
%Adamdir='C:\Documents and Settings\Geno\Desktop\ADAM\log_filies_2006_11_11';
base_dir = '/home/kilonalu/projects/bbl/trunk/src/matlab/kilonalu/processing/';
Adamdir=[base_dir 'log_files'];
cd (Adamdir);
d=dir(['*.log']);
clear dat
i=1;
for i = 1:length(d)    
   dat(i) = datenum(d(i).date);
end
nn = find(dat == max(dat));  % newest file in directory ...
    [dum, nni] = sort(dat);
    nnp = nni(end-1);  % index for second most recent file
adamdate=datestr(dat(nnp),1);
afil=d(nnp).name;
%%%%% lines 6:16 + line 58 added May 3, 2007
% f=dir('*.log');
% fff=[];
% i=1;
% for i=1:length(f)
%     ff=f(i,:).name;
%     fff=[fff;ff];
% end
% [forder,nnf]=sortrows(fff);   
% afil=f(nnf(end-1),:).name;
fid=fopen(afil);
adammess=[];
nextline=[];
while 1
    nextline=fgetl(fid);
    if nextline==-1,   break,   end
    adammess=nextline;
end
fclose ('all');
cd (retdir)

%%% convert numbers to voltages
comma=findstr(adammess,',');
comma=[0,comma,length(adammess)+1];
Vt1=[];
i=1;
for i=1:length(comma)-1
    Vt=str2double(adammess(comma(i)+1:comma(i+1)-1));
    Vt1=[Vt1;Vt];
end
Vt2=Vt1(~isnan(Vt1));
Vt3=10*((Vt2-32768)/32768);
%%% restore first 2 entries (non Volt) and convert voltages back to strings
Vt4=round(Vt3*1000)/1000;
voltstr=num2str(Vt4);
voltages=[];
j=1;
for j=1:length(Vt4)
    voltages=[voltages,',',voltstr(j,:)];
end
% voltlog=[adammess(1:comma(2)),adammess(comma(2)+1:comma(3)-1),voltages];
voltlog=[adamdate,adammess(1:comma(2)),adammess(comma(2)+1:comma(3)-1),voltages];
% keyboard

