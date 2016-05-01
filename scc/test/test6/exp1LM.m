function exp1LM(infolder,outfolder,pattern)

display(infolder)
display(outfolder)
display(pattern)

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
        
        % header: 1:DL1, 2:#logAxioms1,  3:#preUnsatClasses1   4:#disjTest1,  5:#saved tests1,  6:#disjAdded1,  7:postUnsat1,  8:disjAddTime1,
        %         9:DL2, 10:#logAxioms2, 11:#preUnsatClasses2, 12:#disjTest2, 13:#saved tests2, 14:#disjAdded2, 15:postUnsat2, 16:disjAddTime2
        %         17:|M|, 18:|M_clean_consistency|, 19:|M_clean_conservativity|, 20:LM_time_consistency, 21:LM_time_conservativity
        
        %last two params are rows and cols to ignore
        %data = dlmread(filename,' ',0,0);
        dataStr = textread(filename, '%s', 'whitespace',' ');
        data = zeros(size(dataStr,1)/21,19);
        for i=1:size(dataStr,1)/21
            idxs = [2:8 10:21]+(21*(i-1));
            data(i,:) = cellfun(@str2num, dataStr(idxs,:))
            %cell2mat(dataStr([2:8 10:21]*(i*21)))';
        end

        % header: 1:#logAxioms1,  2:#preUnsatClasses1   3:#disjTest1,  4:#saved tests1,  5:#disjAdded1,  6:postUnsat1,  7:disjAddTime1,
        %         8:#logAxioms2, 9:#preUnsatClasses2, 10:#disjTest2, 11:#saved tests2, 12:#disjAdded2, 13:postUnsat2, 14:disjAddTime2
        %         15:|M|, 16:|M_clean_consistency|, 17:|M_clean_conservativity|, 18:LM_time_consistency, 19:LM_time_conservativity
                
        % ms -> s
        data(:,7) = data(:,7)/1000;
        data(:,14) = data(:,14)/1000;
        data(:,18) = data(:,18)/1000;
        data(:,19) = data(:,19)/1000;
        
        [~,sorted_inds3] = sort( data(:,3) );
        [~,sorted_inds5] = sort( data(:,5) );
        [~,sorted_inds15] = sort( data(:,15) );
        
        %   plot(data(sorted_inds3,3),data(sorted_inds3,4:5))
        %	plot(data(sorted_inds5,5),data(sorted_inds5,10)-data(sorted_inds5,9))
        %	plot(data(sorted_inds5,5),data(sorted_inds5,9)-data(sorted_inds5,10))
        %	plot(data(sorted_inds3,3),data(sorted_inds3,10)-data(sorted_inds3,9))
        %	plot(data(sorted_inds5,5),data(sorted_inds5,11:12))
        %	plot(data(sorted_inds3,3),data(sorted_inds3,7))
        %	plot(data(sorted_inds3,3),data(sorted_inds3,[4:5 7]))
        %	plot(data(sorted_inds3,3),[(data(sorted_inds3,5) ./ data(sorted_inds3,3)) data(sorted_inds3,[4:5 7])])
        %	plot(data(sorted_inds3,3),[(data(sorted_inds3,5) ./ data(sorted_inds3,3)) data(sorted_inds3,7)])
        %	plot(data(sorted_inds3,3),[(data(sorted_inds3,5) ./ data(sorted_inds3,3)) *100 data(sorted_inds3,7)])
        
        colors = {'k','r','g','b'};
        figure;
        idx = sorted_inds15;
        xIdx = 15;
        [ax,H1,H2] = plotyy(data(idx,xIdx),data(idx,16:17),...
            data(idx,xIdx),data(idx,18:19),'plot');
        set(H1,'color',colors{1})
        set(H2,'color',colors{2})
        set(H1(1),'Marker','*')
        set(H1(2),'Marker','o')
        set(H2(1),'Marker','+')
        set(H2(2),'Marker','s')
        
        set(get(ax(1),'Ylabel'),'String','Clean Alignment (#Elems)')
        set(get(ax(1),'Ylabel'),'color',colors{1})
        set(get(ax(2),'Ylabel'),'String','Time (s)')
        set(get(ax(2),'Ylabel'),'color',colors{2})
        set(ax(1),'ycolor',colors{1});
        set(ax(2),'ycolor',colors{2});
        %set(ax(1),'YTick',0:10:100)
        %xlim(ax(1),[min(uniqueMapNums)-1 max(uniqueMapNums)+5])
        %ylim(ax(1),[0 100])
        %set(ax(2),'YTick',0:10:65)
        %xlim(ax(2),[min(uniqueMapNums)-1 max(uniqueMapNums)+5])
        %ylim(ax(2),[0 65])
        
        %xlabel(labX);
        labelLegend = {'Repaired Original','Repaired Conserv.',...
            'Repair time original','Repair time Conserv.'};
        legend(labelLegend,'Location','NorthEast'); %'BestOutside','Best',
        
        set(gcf,'PaperUnits','centimeters')
        xSize = 12; ySize = 8;
        xLeft = (21-xSize)/2; yTop = (30-ySize)/2;
        set(gcf,'PaperPosition',[xLeft yTop xSize ySize])
        set(gcf,'Position',[0 0 xSize*50 ySize*50])
        
        print('-depsc', strcat(outfolder,'/exp1LM.eps'))
    end
    %clear ; close all; clc
end
