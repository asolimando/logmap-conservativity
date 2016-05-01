function exp7(infolder,outfolder,pattern,kind)

display(infolder)
display(outfolder)
display(pattern)
display(kind)

%pattern = 'test4.text';

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
        
        % headerOld: 1:vtx1, 2:vtx2, 3:M, 4:edges1, 5:edges2, 6:aspWeight, 7:heurWeight, 8:aspTime, 9:heurTime
        % header: 1:vtx1, 2:vtx2, 3:M, 4:edges1, 5:edges2, 6:totWM, 7:cardASP, 8:cardHeur, 9:aspWeight, 10:heurWeight, 11:aspTime, 12:heurTime
        
        %last two params are rows and cols to ignore
        data = dlmread(filename,' ',0,0);
        maxX = 140;
        data = data(data(:,9) > 0 | data(:,10) > 0,:);
        %data = data(data(:,3) < maxX & data(:,7) > 0,:);
        %data = data(data(:,3) < 60 & data(:,11) < 60,:);
        
        data(:,11:12) = data(:,11:12)/1000;
        
        % compute aggregated data for each unique aggregation value
        labelLegend = {'wASP','wHeur','tASP','tHeur'};
        %labelLegend = {'%subopt','tASP','tHeur'};
        
        if kind == 1
            xIdx = 3;
            data = [data, round((data(:,6)-data(:,9)) ./ data(:,6))*100];
            labX = '|M|';
        elseif kind == 2
            xIdx = 13;
            data = [data, round((data(:,3)-data(:,7)) ./ data(:,3))*100];
            labX = '%1-1';
        else
            xIdx = 13;
            data = [data, round((data(:,6)-data(:,9)) ./ data(:,6))*100];
            labX = '%1-1w';
        end
        
        data = sortrows(data,xIdx);
        uniqueMapNums = sort(unique(data(:,xIdx)));
        cols = 9:13; %6:9;
        meandata = zeros(length(uniqueMapNums),length(cols)+1);
        
        for i = 1:length(uniqueMapNums)
            idx = find(data(:,xIdx) == uniqueMapNums(i));
            if sum(idx > 0) == 1
                meandata(i,:) = data(idx,[xIdx,cols]);
                %meandata(i,:) = (meandata(i-1,:));
            else
                meandata(i,:) = mean(data(idx,[xIdx,cols]));
            end
        end
        
        colors = {'k','r','g','b'};
        figure;
        [ax,H1,H2] = plotyy(meandata(:,1),meandata(:,2:3),...%[
            ...%((meandata(:,3)-meandata(:,2))./meandata(:,2)*100),...%meandata(:,6)],...
            meandata(:,1),meandata(:,4:5),'plot');
        set(H1,'color',colors{1})
        set(H2,'color',colors{2})
        set(H1(1),'Marker','*')
        set(H1(2),'Marker','o')
        set(H2(1),'Marker','+')
        set(H2(2),'Marker','s')
        
        set(get(ax(1),'Ylabel'),'String','Diagnosis Weight')
        %set(get(ax(1),'Ylabel'),'String','%')
        set(get(ax(1),'Ylabel'),'color',colors{1})
        set(get(ax(2),'Ylabel'),'String','Time (s)')
        set(get(ax(2),'Ylabel'),'color',colors{2})
        set(ax(1),'ycolor',colors{1});
        set(ax(2),'ycolor',colors{2});
        %set(ax,{'ycolor'},{colors{1};colors{2}})
        set(ax(1),'YTick',0:10:100)
        xlim(ax(1),[min(uniqueMapNums)-1 max(uniqueMapNums)+5])
        ylim(ax(1),[0 100])
        set(ax(2),'YTick',0:10:65)
        xlim(ax(2),[min(uniqueMapNums)-1 max(uniqueMapNums)+5])
        ylim(ax(2),[0 65])
        %set(ax(2),'YTick',0:0.01:0.1)
        %set(gca,'YMinorTick','on')
        
        %        set(gca,...%'XMinorTick'  , 'on',...
        %            'TickDir','out','XTick', min(uniqueMapNums):1:max(uniqueMapNums));
        
        xlabel(labX);
        legend(labelLegend,'Location','NorthWest'); %'BestOutside','Best',
        
        set(gcf,'PaperUnits','centimeters')
        xSize = 12; ySize = 8;
        xLeft = (21-xSize)/2; yTop = (30-ySize)/2;
        set(gcf,'PaperPosition',[xLeft yTop xSize ySize])
        set(gcf,'Position',[0 0 xSize*50 ySize*50])
        
        print('-depsc', strcat(outfolder,'/exp7-',num2str(kind),'.eps'))
    end
    %clear ; close all; clc
end
