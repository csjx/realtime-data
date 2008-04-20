function Vf = CurrFilt(V, nf, ny, ford)
% CurrFilt - filters current data using set parameters
% nf, filter cuttoff in minutes
% ny, Nyquist period in minutes 
% ford, filter order

Vf = V.*NaN;

% replace nans with zeros
ndum = find(isnan(V));
V(ndum) = zeros(size(ndum));

[b, a] = butter(ford,ny/nf);

for i = 1:size(V,1)
    Vf(i,:) = filtfilt(b,a,nan_interp(V(i,:)));
end

Vf(ndum) = NaN*zeros(size(ndum));
