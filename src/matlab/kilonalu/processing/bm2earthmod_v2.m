function [velE, velN, velU, velErr] = bm2earthmod_v2(beam1, beam2, beam3, beam4, head, HeadingOffset, pitch, roll, beams_up);
% bm2earth.m converts RDI data recorded in BEAM to earth coordinates
% bm2earthmod - modified by G. Pawlak 3/2004 to improve run time
% bm2earthmod_v2 - modified by J. Wells 1/2007 to divide the vertical
% velocity by 2.  See code line 71
% function earth=bm2earth(beam, head, HeadingOffset, roll, ssnd, ECssnd, beams_up, xfreq, convex, sensor_config, BeamAngle);
% where 
% beam = bins by beam matrix of data, a single ensemble, in mm/s
% head = value for the ensemble, in degrees
% HeadingOffset = any additional bias to apply to the heading.
%        Values entered here are added to the heading value
%        If EB command has been set in the ADCP, heading has already
%        been corrected by the value set by the EB command.
% pitch = value for the ensemble, in degrees
% roll = value for the ensemble, in degrees
% earth = resultant matrix
% ssnd = calculated speed of sound in m/s at transducer head
% ECssnd = speed of sound assumption from the EC command
% beams_up = 1 for upward looking (default), 0 for downward 
% xfreq = transmit frequency, default = 300 Khz 
% convex = 1 for convex (default) or 0 for concave xducers
% sensor_config = sensor configuration, default = 1, fixed
% BeamAngle = default = 20 degrees

% Written by Marinna Martini for the 
% U.S. Geological Survey
% Branch of Atlantic Marine Geology
% Thanks to Al Pluddeman at WHOI for helping to identify the 
% tougher bugs in developing this algorithm

% check inputs
if exist('beams_up') ~= 1, beams_up = 1; end
if exist('BeamAngle')~=1, BeamAngle = 20.0; end

% generic instrument transformation matrix

% Step 1 - determine rotation angles from sensor readings

% make sure everything is expressed in radians for MATLAB
d2r=pi/180; % conversion from degrees to radians
RR=roll*d2r;
KA=sqrt(1.0 - (sin(pitch*d2r).*sin(roll*d2r)).^2);
PP=asin(sin(pitch*d2r).*cos(roll*d2r)./KA);
% fix heading bias
% add heading bias to conform with RDI conventions
HH=(head+HeadingOffset)*d2r;

% Step 2 - calculate trig functions and scaling factors
CP=cos(PP); CR=cos(RR); CH=cos(HH); 
SP=sin(PP); SR=sin(RR); SH=sin(HH);
C30=cos(BeamAngle*d2r);
S30=sin(BeamAngle*d2r);

% form the transducer to instrument coordinate system
% scaling constants
% original Al Plueddeman version where theta is the 
% beam angle from the horizontal
% sthet0=sin(d2r*(90-BeamAngle));
% cthet0=cos(d2r*(90-BeamAngle));
% VXS = VYS = SSCOR / (2.0*cthet0);
% VZS = VES = SSCOR / (2.0*sthet0);
% correct for speed of sound using ADCP sound speed
% based on thermistor measurements, where 1500 was the
% assumed sound speed.
%SSCOR = ssnd/1500;
%SSCOR = ssnd/ECssnd;
SSCOR = 1;

VXS = SSCOR/(2.0*S30);  % Single value Constant
VYS = VXS;
VZS = SSCOR/(4.0*C30);  % divisor of 2 changed to divisor of 4  
                        % because 4 beam vel are being summed. (JW 1/07)
VES = SSCOR/(2.0*C30);  % leave divisor as 2 for error velocity 
                        % because it's based on differences between sets
                        % of 2 velocities

VY = VYS.*(-beam3+beam4);
velErr = VES.*(+beam1+beam2-beam3-beam4);
if beams_up == 1,
    % for upward looking convex
    VX = VXS.*(-beam1+beam2);
    VZ = VZS.*(-beam1-beam2-beam3-beam4);
else
    % for downward looking convex
    VX = VXS.*(+beam1-beam2);
    VZ = VZS.*(+beam1+beam2+beam3+beam4);
end

% Step 5: convert to earth coodinates
for i = 1:size(beam1,1)
    velE(i,:) =  VX(i,:).*(CH.*CR + SH.*SR.*SP) + VY(i,:).*SH.*CP + VZ(i,:).*(CH.*SR - SH.*CR.*SP);
    velN(i,:) = -VX(i,:).*(SH.*CR - CH.*SR.*SP) + VY(i,:).*CH.*CP - VZ(i,:).*(SH.*SR + CH.*SP.*CR);
    velU(i,:) = -VX(i,:).*(SR.*CP)            + VY(i,:).*SP     + VZ(i,:).*(CP.*CR);
end


