function exp3(infolder,outfolder,pattern)

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
        %          11:Mweight
        %	       12:aspCTime 13:aspNCTime 14:sgaTime 15:greedyCTime 
        %          16:simpleWTime 17:filterTime 
        %          18:aspCWeight 19:aspNCWeight 20:sgaDiagWeight 
        %          21:greedyCWeight 22:greedyWWeight 23:filterWeight
        %          24:aspCSize 25:aspNCSize 26:sgaSize 27:greedyCSize
        %          28:greedyWSize 29:filterSize
        
        % compute derived columns of interest
        rawstats = [...
            data(:,12) ./ data(:,13) * 100,...
            data(:,14) ./ data(:,13) * 100,...
            data(:,15) ./ data(:,13) * 100,...
            data(:,16) ./ data(:,13) * 100,...
            data(:,17) ./ data(:,13) * 100,...
            
            data(:,18) ./ data(:,19) * 100,...
            data(:,20) ./ data(:,19) * 100,...
            data(:,21) ./ data(:,19) * 100,...
            data(:,22) ./ data(:,19) * 100,...
            data(:,23) ./ data(:,19) * 100,...
            ];
        
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
        lab = {'\% ncASPT/ncASPT','\% sgaT/ncASPT','\% greedyCT/ncASPT',...
            '\% greedyWT/ncASPT','\% filtT/ncASPT','\% ncASPW/ncASPW',...
            '\% sgaW/ncASPW','\% greedyW/ncASPW','\% greedyWW/ncASPW',...
            '\% filtW/ncASPW'};
        
        % for avoiding to handle vectors
        if size(rawstats,1) == 1
            rawstats = [rawstats ; rawstats];
        end
        boxplot(rawstats,'labels',lab);
        
        ylabel('occurrence (%)');
        xlabel('value');
        
        set(gcf,'PaperUnits','centimeters')
        xSize = 12; ySize = 6;
        xLeft = (21-xSize)/2; yTop = (30-ySize)/2;
        set(gcf,'PaperPosition',[xLeft yTop xSize ySize])
        set(gcf,'Position',[0 0 xSize*50 ySize*50])
        %       legend(legend);
        
        print('-depsc', strcat(outfolder,'/exp3.eps'))
    end
end
