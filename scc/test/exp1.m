function exp1(infolder,outfolder,pattern,extCol,intCol,multi)

display(infolder)
display(pattern)
display(extCol)
display(intCol)
display(multi)

dirListing = dir(fullfile(infolder, pattern));
if ~exist(outfolder,'dir')
    mkdir(outfolder);
end

yearCol = 4;
trackCol = 2;
matcherCol = 1;

%extCol = matcherCol;
%intCol = 0;

aggrByInt = min(1,intCol);

for d = 1:length(dirListing)
    if ~dirListing(d).isdir
        % use full path because the folder may not be the active path
        filename = fullfile(infolder,dirListing(d).name);
        display(filename);
        %filename = dirListing(d).name;
        
        fid = fopen(filename, 'rt');
        
        % go to the beginning
        %        fseek(fid, 0, 'bof');
        aggrMatr = textscan(fid,'%s %s %s %s %*[^\n]','Delimiter',' ');
        %        aggrMatr{:,aggrCol}
        fclose(fid);
        
        %last two params are rows and cols to ignore
        data = dlmread(filename, ' ', 0,4);
        
        % compute unique values from aggregation columns
        uniqueValsInt = [];
        if aggrByInt
            uniqueValsInt = uniqueRowsCA(aggrMatr{:,intCol});
        end
        uniqueValsExt = uniqueRowsCA(aggrMatr{:,extCol});
        
        idxInt = ones(size(aggrMatr,1),1);
        actualIntValue = ' ';

	numElemPerRow = 7;

        colsBar = sort([...
 	    3 + (0:numElemPerRow:size(data,2)-1),...
	    4 + (0:numElemPerRow:size(data,2)-1),...
            5 + (0:numElemPerRow:size(data,2)-1),...
	    6 + (0:numElemPerRow:size(data,2)-1),...
	    7 + (0:numElemPerRow:size(data,2)-1)...
	]);
        
        for u = 1:max([1, (size(uniqueValsInt,1) * aggrByInt)])
            if ~intCol == 0
                actualIntValue = strrep(num2str(cell2mat(uniqueValsInt(u,1))),'_','\_');
                idxInt = strcmp(uniqueValsInt(u,1),aggrMatr{intCol});
            end
            for e = 1:size(uniqueValsExt,1)
                actualExtValue = strrep(num2str(cell2mat(uniqueValsExt(e,1))),'_','\_');
                idxExt = strcmp(uniqueValsExt(e,1),aggrMatr{extCol});
                % find index for unique element considered in this iteration
                idx = idxExt & idxInt;
                
                if ~sum(idx == 1)
                    display(strcat('skipping ',actualExtValue));
                    continue
                end
                
                % in data, each element of the following elements have 5
                % info (|M|, |R|, Precision, Recall, F-Measure, 1-1R, 1-1M):
                % 1 = |M| vs |R|
                % 2 = |M\\Diag| vs |R|
                % 3 = |M\\mDiag| vs |R|
                % 4 = |M\\Diag| vs |R\\RDiag|
                % 5 = |M\\Diag| vs |R\\mRDiag|
                % 6 = |M\\mDiag| vs |R\\RDiag|
                % 7 = |M\\mDiag| vs |R\\mRDiag|
                % 8 = |M| vs |R\\RDiag|
                % 9 = |M| vs |R\\mRDiag|
                
                % 1,2,3
                refIdx = 1; 
                debIdx = 2;
                mdebIdx = 3;
                
                colRef = (numElemPerRow*refIdx)-4:numElemPerRow*refIdx-2;
                colDeb = (numElemPerRow*debIdx)-4:numElemPerRow*debIdx-2;
                colMDeb = (numElemPerRow*mdebIdx)-4:numElemPerRow*mdebIdx-2;
                
                if ~multi
                    dataDiag = [(data(idx,colDeb) - data(idx,colRef))*100,... 
                        data(idx,numElemPerRow*debIdx-1) ./ data(idx,numElemPerRow*debIdx-5) * 100,...
                        data(idx,numElemPerRow*debIdx) ./ data(idx,numElemPerRow*debIdx-6) * 100];
                else
                    dataDiag = [(data(idx,colMDeb) - data(idx,colRef))*100,...
                        data(idx,numElemPerRow*mdebIdx-1) ./ data(idx,numElemPerRow*mdebIdx-5) * 100,...
                        data(idx,numElemPerRow*mdebIdx) ./ data(idx,numElemPerRow*mdebIdx-6) * 100];
                end
                
                % delete rows with all zeros (empty diagnosis, not usefull)
                dataDiag( ~any(dataDiag(:,1:end-2),2), : ) = [];  %rows
                
                % if now it is empty, skip it
                if ~size(dataDiag,1)
                    continue;
                end
                
                %B = reshape(dataNew(idx,6),3*size(dataNew,1),size(dataNew,2)/3)';
                
                titleLabel = actualExtValue;
                if aggrByInt
                    titleLabel = strcat(titleLabel,'-',actualIntValue);
                end
                if multi
                    titleLabel = strcat(titleLabel,'-multi');
                end
                
                titleLabel = strrep(strrep(titleLabel,'\',''),'_','')
                
                if size(dataDiag,1) == 1
                    dataDiag = [dataDiag; dataDiag];
                end
                
                figure;
                h=subplot(1,2,1);
                boxplot(dataDiag,'labels', {'pr','rc', 'fm.','1-1 R','1-1 M'});%,...
%                    'labelorientation','inline');
                %firstax = gca;
                ylabel('gain (%)');
		set(gca,'YMinorTick'  , 'on',...
                 'TickDir'     , 'out','YTick', -20:20:100);
                p = get(h, 'pos');
                x = get(gca,'xlim');
                x = x(1):x(2);
                p(3) = p(3) + 0.05;
                set(h, 'pos', p);
                
                ylim([-20 105]);
                
%                 if extCol == trackCol
%                     ylim([-8 15]);
%                 elseif extCol == matcherCol
%                     ylim([-35 40]);
%                 end
                
                for k = -2:10
                    hold on;
                    plot(x, 10*k*ones(size(x)), 'LineWidth', 0.05, 'Color', [0,0,0]+0.02, 'LineStyle', ':'); %linestyle --
                end
                
                %                 h=subplot(1,2,2);
                %                 boxplot(dataMDiag,'labels', {'Precision','Recall', 'F-Measure'});
                %                 sndax = gca;
                %                 set(gca,'YTickLabel',[]);
                %                 xlabel('Multiple-Occ. Filter Diagnosis');
                %                 ylabel('');
                %                 p = get(h, 'pos');
                %                 p(1) = p(1) - 0.05;
                %                 p(3) = p(3) + 0.05;
                %                 set(h, 'pos', p);
                %
                %                 if (seq(i)<12)
                %                     ylim(firstax,[-1,25]);
                %                     ylim(sndax,[-1,25]);
                %                 else
                %                     if (seq(i)<52)
                %                         ylim(firstax,[-0.36,9]);
                %                         ylim(sndax,[-0.36,9]);
                %                     else
                %                         if (seq(i)<=100)
                %                             ylim(firstax,[-0.3,14]);
                %                             ylim(sndax,[-0.3,14]);
                %                         else
                %                             ylim(firstax,[-0.14,3.5]);
                %                             ylim(sndax,[-0.14,3.5]);
                %                         end
                %                     end
                %                 end
                
                set(gcf,'PaperUnits','centimeters')
                xSize = 12; ySize = 6;
                xLeft = (21-xSize)/2; yTop = (30-ySize)/2;
                set(gcf,'PaperPosition',[xLeft yTop xSize ySize])
                set(gcf,'Position',[0 0 xSize*50 ySize*50])
                
                print('-depsc', strcat(outfolder,'/', 'exp1-',...
                    titleLabel, '.eps'))
                %                 %                 group = ones(1,length(colsBar))
                %                 %                 counter = 1;
                %                 %                 for a = 3:3:size(group,2)
                %                 %                     group(1,a-2:a) = group(1,a-2:a) * counter;
                %                 %                     counter = counter + 1;
                %                 %                 end
                %                 %
                %                 %                 size(group)
                %
                %                 %data(idx,colsBar)
                %                 elems = find(idx == 1);
                %                 for c = 1:length(elems)
                %                     titleLabel = strcat(actualExtValue,'-',actualIntValue);%,'-',num2str(c))
                %
                %                     % print a bargraph for each row
                %                     set(gcf,'PaperUnits','centimeters')
                %                     xSize = 12;
                %                     ySize = 9;
                %                     xLeft = (21-xSize)/2;
                %                     yTop = (30-ySize)/2;
                %                     set(gcf,'PaperPosition',[xLeft yTop xSize ySize])
                %                     set(gcf,'Position',[0 0 xSize*50 ySize*50])
                %
                %                     B = reshape(data(elems(c),colsBar),3,length(colsBar)/3)';
                %
                %                     gca = bar(B)
                %                     set(gca,'XLim',[0.0 1.0]);
                %                     %bar(data(idx(c),colsBar))
                %                     title(titleLabel)
                %
                %                     legend('Precision', 'Recall','F-Measure')
                %                     %xlabel('Matching task')
                %                     %ylabel('Cardinality')
                %                     print('-depsc', strcat(outfolder,'/', 'exp1-',...
                %                         titleLabel, '.eps'))
                %
                %                 end
            end
        end
    end
    close all;
    %    clear ; close all; clc
    
end
