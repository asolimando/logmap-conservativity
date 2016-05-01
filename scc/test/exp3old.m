function exp3old(infolder,outfolder,pattern)

display(infolder)
display(outfolder)
display(pattern)

filterProbl = 0;

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
            data(:,11) - data(:,12),...
            data(:,13) - data(:,12),...
            data(:,14) - data(:,12),...
            data(:,15) - data(:,12),...
            (data(:,16) - data(:,17)) ./ data(:,17),...
            (data(:,18) - data(:,17)) ./ data(:,17),...
            (data(:,19) - data(:,17)) ./ data(:,17),...
            (data(:,20) - data(:,17)) ./ data(:,17),...
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
        globPref = {'% '};
        prefLab = {'cASP','sga','greedy','filt'};
        suffLab = 'ncASP';
        
        % for avoiding to handle vectors
        if size(rawstats,1) == 1
            rawstats = [rawstats ; rawstats];
        end
        
        for k=1:length(prefLab)
            prefLab(k)        
            boxplot(rawstats(abs(rawstats(:,[k])) > power(10,-16),[k]),'labels',[...
                strcat(globPref,prefLab(k),'/',suffLab,' (T)'),...
            ]);

            ylabel('%');
%            xlabel('value');

            set(gcf,'PaperUnits','centimeters')
            xSize = 12; ySize = 6;
            xLeft = (21-xSize)/2; yTop = (30-ySize)/2;
            set(gcf,'PaperPosition',[xLeft yTop xSize ySize])
            set(gcf,'Position',[0 0 xSize*50 ySize*50])
            %       legend(legend);

            print('-depsc', strcat(outfolder,'/exp3',num2str(cell2mat(prefLab(k))),'t.eps'))

	    boxplot(rawstats(abs(rawstats(:,[k+4])) > power(10,-16),[k+4]),'labels',[...
                strcat(globPref,prefLab(k),'/',suffLab,' (W)')...
            ]);                

            ylabel('%');
%            xlabel('value');

            set(gcf,'PaperUnits','centimeters')
            xSize = 12; ySize = 6;
            xLeft = (21-xSize)/2; yTop = (30-ySize)/2;
            set(gcf,'PaperPosition',[xLeft yTop xSize ySize])
            set(gcf,'Position',[0 0 xSize*50 ySize*50])
            %       legend(legend);

            print('-depsc', strcat(outfolder,'/exp3',num2str(cell2mat(prefLab(k))),'w.eps'))
        end 
    end
end
