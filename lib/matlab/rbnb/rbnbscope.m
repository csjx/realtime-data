% RBNB Scope Demo

server = 'phm.creare.com';      % name or IP address of PHM computer
cname = 'QuickLook/chan01';     % PHM chans are 'QuickLook/chan##', ## = 01-48
duration = 0.01;                % time-interval to fetch

sink = rbnb_sink(server,'mySink');

for(i=1:1000)
  dat = rbnb_get(sink, cname, 0, duration, 'newest');
  plot(dat);
  drawnow;
end


