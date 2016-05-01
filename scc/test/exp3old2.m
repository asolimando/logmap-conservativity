function exp3old2(infolder,outfolder,pattern)

display(infolder)
display(outfolder)
display(pattern)

filterProbl = 1;

dirListing = dir(fullfile(infolder, pattern));
if ~exist(outfolder,'dir')
    mkdir(outfolder);
end

for d = 1:length(dirListing)
    if ~dirListing(d).isdir
        % use full path because the folder may not be the active path
        filename = fullfile(infolder,dirListing(d).name);
        display(filename);
        %filename = dirListing(d).name;
        
        %last two params are rows and cols to ignore
        data = dlmread(filename, ' ', 0,0);
        % header = 1:numCycles 2:avgCycleLen 3:cycleTime (ms) 4:vtx1 5:vtx2
        %          6:M 7:edge1 8:edge2 9:avgInDegree 10:avgOutDegree
        %	       11:aspConsTime 12:aspNonConsTime 13:sgaTime
        %          14:simpleTime 15:filterTime 16:aspConsDiagWeight
        %          17:aspNonConsDiagWeight 18:sgaDiagWeight 19:simpleWeight
        %          20:filterWeight
        
        % compute derived columns of interest
        rawstats = [...
            data(:,11) ./ data(:,12),...
            data(:,13) ./ data(:,12),...
            data(:,14) ./ data(:,12),...
            data(:,15) ./ data(:,12),...
            data(:,16) ./ data(:,17),...
            data(:,18) ./ data(:,17),...
            data(:,19) ./ data(:,17),...
            data(:,20) ./ data(:,17),...
            ];
        
        % rawstats = rawstats * 100;
        
        if filterProbl==0
            % convert NaN into 0
            rawstats(isnan(rawstats)) = 0;
        else
            % filter NaN
            %data = data(~any(isnan(data),2),:);
            %data = data(~isnan(data(:,2)),:);
            rawstats = rawstats(find(sum(isnan(rawstats),2)==0),:);
        end
        
        % compute aggregated data for each unique aggregation value
        globPref = {'\% '};
        prefLab = {'cASP','sga','greedy','filt'};
        suffLab = 'ncASP';
        
        % for avoiding to handle vectors
        if size(rawstats,1) == 1
            rawstats = [rawstats ; rawstats];
        end
        data1 = sortrows(data, 11);
        data1 = smooth(data1(:,11:15),0.25,'rloess');
        plot(data1);
        ylabel('%');
        xlabel('value');
        set(gcf,'PaperUnits','centimeters')
        xSize = 12; ySize = 6;
        xLeft = (21-xSize)/2; yTop = (30-ySize)/2;
        set(gcf,'PaperPosition',[xLeft yTop xSize ySize])
        set(gcf,'Position',[0 0 xSize*50 ySize*50])
        print('-depsc', strcat(outfolder,'/exp3time.eps'))
        
        data2 = sortrows(data, 16);
        data2 = smooth(data2(:,16:20),0.25,'rloess');
        plot(data2);
        ylabel('%');
        xlabel('value');
        set(gcf,'PaperUnits','centimeters')
        xSize = 12; ySize = 6;
        xLeft = (21-xSize)/2; yTop = (30-ySize)/2;
        set(gcf,'PaperPosition',[xLeft yTop xSize ySize])
        set(gcf,'Position',[0 0 xSize*50 ySize*50])
        print('-depsc', strcat(outfolder,'/exp3weight.eps'))
    end
end
