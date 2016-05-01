function exp4(infolder,outfolder,pattern,aggrCol,aggrColExt)

display(infolder)
display(outfolder)
display(pattern)
display(aggrCol)
display(aggrColExt)

%if strcmp(pattern,'')
%    pattern = 'merged.txt';
%end
%pattern = 'largebio_big.txt';
%pattern = 'largebio_small.txt';
%pattern = 'conference.txt';
%pattern = 'anatomy.txt';
%pattern = 'ASMOV.txt';

% 0 = skip it, 1 = matcher, 2 = track
%aggrCol = 1;
%aggrColExt = 0;

dirListing = dir(fullfile(infolder, pattern));
if ~exist(outfolder,'dir')
    mkdir(outfolder);
end

labels = {'wM', 'NLocalSCCs', 'EdgesLoadTime2', 'EdgesLoadTime1', 'NumD',...
    'mDiagTime', 'NumPM', 'NumEdges1', 'mwM', 'DiagTime',...
    'NumNontrivGlobalSCCs', 'NumGlobalSCCs', 'NumProblSCCs', 'NumEdges2',...
    'mNumM', 'TotTime', 'VtxLoadTime1', 'VtxLoadTime2', 'NumVtx2',...
    'NumVtx1', 'wPM', 'wD', 'ProblMapppingsDetTime', 'NumM','NumVSCCs',...
    'NumSubDiag','mTotTime','1M','ontoLoadTime1','ontoLoadTime2'};
labelsId = zeros(1,length(labels));

wMid = 1;
NLocalSCCsid = 2;
EdgesLoadTime2id = 3;
EdgesLoadTime1id = 4;
NumDid = 5;
mDiagTimeid = 6;
NumPMid = 7;
NumEdges1id = 8;
mwMid = 9;
DiagTimeid = 10;
NumNontrivGlobalSCCsid = 11;
NumGlobalSCCsid = 12;
NumProblSCCsid = 13;
NumEdges2id = 14;
mNumMid = 15;
TotTimeid = 16;
VtxLoadTime1id = 17;
VtxLoadTime2id = 18;
NumVtx2id = 19;
NumVtx1id = 20;
wPMid = 21;
wDid = 22;
ProblMapppingsDetTimeid = 23;
NumMid = 24;
NumVSCCsid = 25;
NumSubDiagid = 26;
mTotTimeid = 27;
oneMid = 28;
ontoLoadTime1id = 29;
ontoLoadTime2id = 30;

for d = 1:length(dirListing)
    if ~dirListing(d).isdir
        % use full path because the folder may not be the active path
        filename = fullfile(infolder,dirListing(d).name);
        display(filename);
        %filename = dirListing(d).name;
        
        fid = fopen(filename, 'rt');
        headers = fgets(fid);
        headers = regexp(headers, '\s', 'split');
        
        % go to the beginning
        %        fseek(fid, 0, 'bof');
        aggrMatr = textscan(fid,'%s %s %*[^\n]','Delimiter',' ');
        %        aggrMatr{:,aggrCol}
        fclose(fid);
        
        % find the index for the different columns of interest
        for e = 1:length(labels)
            for f = 1:length(headers)
                %labels{e}
                if strcmp(labels{e}, headers{f})
                    labelsId(1,e) = f+1;
                    break
                end
            end
            if labelsId(1,e) == 0
                error(strcat('Unmatched element: ', labels{e}));
            end
        end
        
        % write the column index in the index variable
        wMid = labelsId(1,wMid);
        NLocalSCCsid = labelsId(1,NLocalSCCsid);
        EdgesLoadTime2id = labelsId(1,EdgesLoadTime2id);
        EdgesLoadTime1id = labelsId(1,EdgesLoadTime1id);
        NumDid = labelsId(1,NumDid);
        mDiagTimeid = labelsId(1,mDiagTimeid);
        NumPMid = labelsId(1,NumPMid);
        NumEdges1id = labelsId(1,NumEdges1id);
        mwMid = labelsId(1,mwMid);
        DiagTimeid = labelsId(1,DiagTimeid);
        NumNontrivGlobalSCCsid = labelsId(1,NumNontrivGlobalSCCsid);
        NumGlobalSCCsid = labelsId(1,NumGlobalSCCsid);
        NumProblSCCsid = labelsId(1,NumProblSCCsid);
        NumEdges2id = labelsId(1,NumEdges2id);
        mNumMid = labelsId(1,mNumMid);
        TotTimeid = labelsId(1,TotTimeid);
        VtxLoadTime1id = labelsId(1,VtxLoadTime1id);
        VtxLoadTime2id = labelsId(1,VtxLoadTime2id);
        NumVtx2id = labelsId(1,NumVtx2id);
        NumVtx1id = labelsId(1,NumVtx1id);
        wPMid = labelsId(1,wPMid);
        wDid = labelsId(1,wDid);
        ProblMapppingsDetTimeid = labelsId(1,ProblMapppingsDetTimeid);
        NumMid = labelsId(1,NumMid);
        NumVSCCsid = labelsId(1,NumVSCCsid);
        NumSubDiagid = labelsId(1,NumSubDiagid);
        mTotTimeid = labelsId(1,mTotTimeid);
        oneMid = labelsId(1,oneMid);
        ontoLoadTime1id = labelsId(1,ontoLoadTime1id);
        ontoLoadTime2id = labelsId(1,ontoLoadTime2id);
        
        %last two params are rows and cols to ignore
        data = dlmread(filename, ' ', 1,3);
        
        % compute unique values from aggregation columns
        uniqueVals = uniqueRowsCA(aggrMatr{:,aggrCol});
        uniqueValsExt = [];
        if aggrColExt > 0
            uniqueValsExt = uniqueRowsCA(aggrMatr{:,aggrColExt});
        end
        
        % compute aggregated data for each unique aggregation value
        labels = {'VtxLoad','EdgesLoad','Detection','Diagnosis','OntoLoad'};
        
        idxExt = ones(size(aggrMatr,1),1);
        for e = 1:max([1, size(uniqueValsExt,1)])
            if aggrColExt > 0
                actualExtVal = uniqueValsExt(e,1)
                idxExt = strcmp(uniqueValsExt(e,1),aggrMatr{aggrColExt});
            end
            for u = 1:size(uniqueVals,1)
                actualIntVal = uniqueVals(u,1);
                % find index for unique element considered in this iteration
                idx = idxExt & strcmp(uniqueVals(u,1),aggrMatr{aggrCol});
                idx = idx & (data(:,NumProblSCCsid)>0);
                
                % number of mapping contributing to this statistics
                mappingsNum = sum(idx == 1);
                % number of mapping contributing to this row
                totMappingsNum = size(data(idx,:),1);
                
                % skip when there are no lines
                if mappingsNum == 0
                    %display('skip line');
                    continue;
                end
                
                if mappingsNum == 1
                    meandata = data(idx,:);
                else
                    meandata = mean(data(idx,:));
                end
                
                % compute derived columns of interest
                partTotTime = meandata(:,VtxLoadTime1id) + meandata(:,VtxLoadTime2id) +...
                    meandata(:,EdgesLoadTime1id) + meandata(:,EdgesLoadTime2id)+ ...
                    meandata(:,ProblMapppingsDetTimeid) +...
                    meandata(:,ontoLoadTime1id) + meandata(:,ontoLoadTime2id);
                TotTime = partTotTime + meandata(:,DiagTimeid);
                mTotTime = partTotTime + meandata(:,mDiagTimeid);
                
                rawstats = [...
                    (meandata(:,VtxLoadTime1id) + meandata(:,VtxLoadTime2id)) ./ TotTime,...
                    (meandata(:,EdgesLoadTime1id) + meandata(:,EdgesLoadTime2id)) ./ TotTime,...
                    meandata(:,ProblMapppingsDetTimeid) ./ TotTime,...
                    meandata(:,DiagTimeid) ./ TotTime,...
                    (meandata(:,ontoLoadTime1id) + meandata(:,ontoLoadTime2id)) ./ TotTime...
                    ];
                
                mrawstats = [...
                    (meandata(:,VtxLoadTime1id) + meandata(:,VtxLoadTime2id)) ./ mTotTime,...
                    (meandata(:,EdgesLoadTime1id) + meandata(:,EdgesLoadTime2id)) ./ mTotTime,...
                    meandata(:,ProblMapppingsDetTimeid) ./ mTotTime,...
                    meandata(:,mDiagTimeid) ./ mTotTime,...
                    (meandata(:,ontoLoadTime1id) + meandata(:,ontoLoadTime2id)) ./ mTotTime...
                    ];
                
                percCols = (1 : size(rawstats,2));
                
                % convert into percentage
                rawstats(:,percCols) = rawstats(:,percCols) * 100;
                mrawstats(:,percCols) = mrawstats(:,percCols) * 100;
                
                titleLabel = actualIntVal;
                if aggrColExt > 0
                    titleLabel = strcat(titleLabel,'-',actualExtVal);
                end
                
                titleLabel = strrep(strrep(titleLabel,'\',''),'_','')
                
                figure;
                %h=subplot(1,2,1);
                bar([rawstats;mrawstats],'stacked');%;zeros(1,size(rawstats,2))
                %                ax = axis;ax(2) = 1.6; %Change axis limit
                %                axis(ax)
                
%                 hAxes = gca;
%                 hAxes_pos = get(hAxes,'Position');
%                 
%                 hAxes2 = axes('Position',hAxes_pos);

%                                  x = get(gca,'xlim');
%                                  x = 0:x(2);
%                 for k = 1:10
%                     hold on;
%                     plot(x, 10*k*ones(size(x)), 'LineWidth', 0.05, ...
%                         'Color', [0,0,0]+0.02, 'LineStyle', ':'); %linestyle --
%                 end
%                 set(hAxes2,'YAxisLocation','right',...
%                     'Color','none',...
%                     'XTickLabel',[])
%                 
%                 h1_xlim = get(hAxes,'XLim'); % store x-axis limits of first axes
%                 set(hAxes2,'XLim',h1_xlim) % specify x-axis limits of second axes
                
                ylim([0 100]);
                 set(gca,'XTickLabel',{'Diagnosis','MultipleFilter+Diagnosis'},...
                 'YGrid','on','YMinorTick'  , 'on',...
                 'TickDir'     , 'out','YTick', 0:10:100)

                %firstax = gca;
                ylabel('tot. time %');
                xlabel(strcat(num2str(mappingsNum),' mapping(s) used'));
                %p = get(h, 'pos');
                %x = get(gca,'xlim');
                %x = x(1):x(2);
                %p(3) = p(3) + 0.05;
                %set(h, 'pos', p);
                
                set(gcf,'PaperUnits','centimeters')
                xSize = 12; ySize = 6;
                xLeft = (21-xSize)/2; yTop = (30-ySize)/2;
                set(gcf,'PaperPosition',[xLeft yTop xSize ySize])
                set(gcf,'Position',[0 0 xSize*50 ySize*50])
                legend(labels, 'Location', 'NorthEast');

                if iscell(titleLabel)
                    titleLabel=titleLabel{1};
                end
                
                print('-depsc', strcat(outfolder,'/', 'exp4-',...
                    titleLabel, '.eps'))
            end
        end
    end
    
    clear ; close all; clc
end
