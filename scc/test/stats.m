function stats(infolder)
    
dirListing = dir(fullfile(infolder, 'out_*.txt'));

stats = zeros(0,2);

for d = 1:length(dirListing)
    if ~dirListing(d).isdir
        % use full path because the folder may not be the active path
        %filename = fullfile(infolder,dirListing(d).name);
        filename = dirListing(d).name;
        data = dlmread(filename, ' ', 1,1); %importdata(filename);
        %filename = strrep(filename,'.txt','');

        actualRows = [data(data(:,9)>0,9) ./ data(data(:,9)>0,6) , data(data(:,9)>0,12) ./ data(data(:,9)>0,9)];

        stats = [stats ; actualRows];
    end
end

stats = stats * 100;

min(stats(:,2))
mean(stats(:,2))
max(stats(:,2))
std(stats(:,2))

%save('stats.txt', 'stats', '-ascii');

    figure;
    boxplot( stats, 'labels', {'Problem/GlobSCC','%UnsolvedProblems'} );	
    xlabel('Kind');
    ylabel('%');

%matrix2latex(resultsPQVLMean, resultsPQVLSTD, strcat(outfolder, '/PQVL.tex'), ...
%    'rowLabels', rowLabelsPQVL, 'columnLabels', columnLabels, 'alignment', 'c', 'format', '%10.2e');

%clear ; close all; clc

end
