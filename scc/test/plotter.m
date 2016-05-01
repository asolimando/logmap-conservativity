% 
function plotter(infolder, outfolder)

if ~exist(outfolder,'dir')
    mkdir(outfolder);
end
    
dirListing = dir(fullfile(infolder, 'out_*.txt'));

%data = zeros(0,19);

for d = 1:length(dirListing)
    if ~dirListing(d).isdir
        % use full path because the folder may not be the active path
        %filename = fullfile(infolder,dirListing(d).name);
        filename = dirListing(d).name;        
        data = dlmread(filename, ' ', 1,1); %importdata(filename);
        filename = strrep(filename,'.txt','');        
        
        data = data(data(:,9)>0,:);
        
        data(:,1) = data(:,1) / 1000; % seconds
        data(:,2) = data(:,2) / 1000; % seconds
        data(:,3) = data(:,3) / 1000; % seconds
        data(:,7) = data(:,7) / 1000; % seconds
        data(:,11) = data(:,11) / 1000; % seconds
        data(:,13) = data(:,13) / 1000; % seconds
        data(:,14) = data(:,14) / 1000; % seconds
        data(:,15) = data(:,15) / 1000; % seconds
        data(:,16) = data(:,16) / 1000; % seconds

        sortCol = 4;
        data = sortrows(data,sortCol);

        set(gcf,'PaperUnits','centimeters')
        xSize = 12; ySize = 9;
        xLeft = (21-xSize)/2; yTop = (30-ySize)/2;
        set(gcf,'PaperPosition',[xLeft yTop xSize ySize])
        set(gcf,'Position',[0 0 xSize*50 ySize*50])

        plot(...
            data(:,4), data(:,7), '+-',...
            data(:,4), data(:,16), 'x-',...
            data(:,4), data(:,11), 'o-',...
            'linewidth', 2);

        mean((data(data(:,9)>0,12) ./ data(data(:,9)>0,9))) * 100

        legend('ComputeBridges', 'ProblemDetTime','TotalTime', 'Location', 'Best')
    	xlabel('Mappings Number')
    	ylabel('Time (s)')
    	print('-depsc', strcat(outfolder,'/', filename, '_1.eps'))
        
        %%%%%%%%%%% second plot
        figure;
        data(:,20) = ((data(:,5)+data(:,10))./data(:,6));
        data = sortrows(data,20);
        
        plot(...
            data(:,20), data(:,7),'+-',...
            data(:,20), data(:,16),'x-',...
            data(:,20), data(:,11),'o-',...
            'linewidth', 2);

        legend('ComputeBridges', 'ProblemDetTime','TotalTime', 'Location', 'Best')
    	xlabel('AVG-V-SCC')
    	ylabel('Time (s)')
    	print('-depsc', strcat(outfolder,'/', filename, '_2.eps'))
    end
end
%clear ; close all; clc
close all;
end
