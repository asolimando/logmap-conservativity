function exp3_final(infolder,outfolder,pattern)

kind = 1;

display(infolder)
display(outfolder)
display(pattern)
display(kind)

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
        
        % header:
        % 1:numCycles 2:avgCycleLen 3:cycleTime (ms) 4:vtx1 5:vtx2
        % 6:M 7:edge1 8:edge2 9:avgInDegree 10:avgOutDegree 11:Mweight
        % 12:aspCTime 13:aspNCTime 14:sgaTime 15:greedyCTime 16:greedyWTime 17:filterTime
        % 18:aspCWeight 19:aspNCWeight 20:sgaDiagWeight 21:greedyCWeight 22:greedyWWeight 23:filterWeight
        % 24:aspCSize 25:aspNCSize 26:sgaSize 27:greedyCSize 28:greedyWSize 29:filterSize
        
        %last two params are rows and cols to ignore
        data = dlmread(filename,' ',0,0);
        maxX = 145;
        %data = data(data(:,19) > 0 | data(:,10) > 0,:);
        %data = data(data(:,3) < maxX & data(:,7) > 0,:);
        %data = data(data(:,3) < 60 & data(:,11) < 60,:);
        
        data(:,12:17) = data(:,12:17)/1000;
        
        % compute aggregated data for each unique aggregation value
        labelLegend = {'wASP','wSGA','wHeurC','wHeurW','wFilt','tASP','tSGA','tHeurC','tHeurW','tFilt'};
        
        if kind == 1
            xIdx = 6;
            %            data = [data, round((data(:,6)-data(:,9)) ./ data(:,6))*100];
            labX = '|M|';
        elseif kind == 2
            xIdx = 13;
            %            data = [data, round((data(:,3)-data(:,7)) ./ data(:,3))*100];
            labX = '%1-1';
        else
            xIdx = 13;
            %            data = [data, round((data(:,6)-data(:,9)) ./ data(:,6))*100];
            labX = '%1-1w';
        end
        
        data = sortrows(data,xIdx);
        uniqueMapNums = sort(unique(data(:,xIdx)));
        cols = [6,12:23];
        meandata = zeros(length(uniqueMapNums),length(cols)+1);
        
        for i = 1:length(uniqueMapNums)
            idx = find(data(:,xIdx) == uniqueMapNums(i));
            if sum(idx > 0) == 1
                meandata(i,:) = data(idx,[xIdx,cols]);
            else
                meandata(i,:) = mean(data(idx,[xIdx,cols]));
            end
        end
        
        colors = {'k','r','g','b'};
        figure;
        [ax,H1,H2] = plotyy(meandata(:,1),meandata(:,9:13),...
        meandata(:,1),meandata(:,3:7),'plot');
        set(H1,'color',colors{1})
        set(H2,'color',colors{2})
        
        set(H1(1),'Marker','*')
        set(H1(2),'Marker','o')
        set(H1(3),'Marker','+')
        set(H1(4),'Marker','s')
        set(H1(5),'Marker','d')
        
        set(H2(1),'Marker','*')
        set(H2(2),'Marker','o')
        set(H2(3),'Marker','+')
        set(H2(4),'Marker','s')
        set(H2(5),'Marker','d')
        
        set(get(ax(1),'Ylabel'),'String','Diagnosis Weight')
        set(get(ax(1),'Ylabel'),'color',colors{1})
        set(get(ax(2),'Ylabel'),'String','Time (s)')
        set(get(ax(2),'Ylabel'),'color',colors{2})
        set(ax(1),'ycolor',colors{1});
        set(ax(2),'ycolor',colors{2});
        %set(ax(1),'YTick',0:10:100)
        xlim(ax(1),[min(uniqueMapNums)-1 max(uniqueMapNums)+5])
        %ylim(ax(1),[0 100])
        %set(ax(2),'YTick',0:10:65)
        xlim(ax(2),[min(uniqueMapNums)-1 max(uniqueMapNums)+5])
        
        xlabel(labX);
        legend(labelLegend,'Location','NorthWest'); %'BestOutside','Best',
        
        set(gcf,'PaperUnits','centimeters')
        xSize = 12; ySize = 8;
        xLeft = (21-xSize)/2; yTop = (30-ySize)/2;
        set(gcf,'PaperPosition',[xLeft yTop xSize ySize])
        set(gcf,'Position',[0 0 xSize*50 ySize*50])
        
        print('-depsc', strcat(outfolder,'/exp3.eps'))
    end
    %clear ; close all; clc
end
