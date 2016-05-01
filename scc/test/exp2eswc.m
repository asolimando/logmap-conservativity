function exp2eswc(infolder,outfolder,outFile,pattern,aggrCol,aggrColExt)

display(infolder)
display(outfolder)
display(outFile)
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

filterProbl = 1;

% 0 = skip it, 1 = matcher, 2 = track
%aggrCol = 1;
%aggrColExt = 0;

dirListing = dir(fullfile(infolder, pattern));
if ~exist(outfolder,'dir')
    mkdir(outfolder);
end

%labels = {'NumPM','NumD','NumM','NumProblSCCs','NumGlobalSCCs','NumNontrivGlobalSCCs'};

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
        
        % sort whole matrix on external aggregation column
        % non si puo fare, quella colonna manca in matrice "data"
        %data = sortrows(data,aggrColExt)
        
        % compute derived columns of interest
        rawstats = [...
            data(:,NumDid) ./ data(:,NumPMid),...
            data(:,NumPMid) ./ data(:,NumMid),...
            data(:,NumDid) ./ data(:,NumMid),...
            data(:,oneMid) ./ data(:,NumMid),...
            data(:,NumMid),...
            data(:,NumVSCCsid) ./ (data(:,NumVtx1id) + data(:,NumVtx2id)),...
            data(:,NumProblSCCsid),...
            (data(:,DiagTimeid)/1000),...
            (data(:,NumProblSCCsid) - data(:,NumSubDiagid)) ./ data(:,NumProblSCCsid),...
            ];
        
        % compute aggregated data for each unique aggregation value
        resultsMean = [];
        resultsStd = [];
        rowLabels = [];
        colLabels = {'\% D/PM','\% PM/M','\% D/M','\% 1-1M','M',...
            '\% VtxSCC/Vtx','probSCC','ASP (s)','\% OptDiag','\#M',...
            '\#TotM'};
        percCols = [(1 : 4), 6, 9];
        
        idxExt = ones(size(aggrMatr,1),1);
        for e = 1:max([1, size(uniqueValsExt,1)])
            if aggrColExt > 0
                uniqueValsExt(e,1)
                idxExt = strcmp(uniqueValsExt(e,1),aggrMatr{aggrColExt});
            end
            for u = 1:size(uniqueVals,1)
                %uniqueVals(u,1);
                % find index for unique element considered in this iteration
                idx = idxExt & strcmp(uniqueVals(u,1),aggrMatr{aggrCol});
                boxplotdata = rawstats(idx,:);
                
                if filterProbl==0
                    % convert NaN into 0
                    boxplotdata(isnan(boxplotdata)) = 0;
                else
                    % filter NaN
                    %boxplotdata = boxplotdata(~any(isnan(boxplotdata),2),:);
                    %boxplotdata=boxplotdata(~isnan(boxplotdata(:,2)),:);
                    boxplotdata = boxplotdata(find(sum(isnan(boxplotdata),2)==0),:);
                end
                
                % skip when there are no lines
                if size(boxplotdata,1) == 0
                    %display('skip line');
                    continue;
                end
                
                % only if the element has some rows it is added to the row
                % labels
                if aggrColExt > 0
                    rowLabels{length(rowLabels)+1} = ...
                        strcat(strrep(num2str(cell2mat(uniqueValsExt(e,1))),'_','\_'),'-',...
                        strrep(num2str(cell2mat(uniqueVals(u,1))),'_','\_'));
                else
                    rowLabels{length(rowLabels)+1} = ...
                        strrep(num2str(cell2mat(uniqueVals(u,1))),'_','\_');
                end
                
                % convert into percentage
                %boxplotdata(:,percCols) = boxplotdata(:,percCols) * 100;
                
                % number of mapping contributing to this row
                mappingsNum = size(boxplotdata,1);
                totMappingsNum = sum(idx == 1);
                
                if mappingsNum == 1
                    resultsMean = [resultsMean; boxplotdata(:,:),...
                        mappingsNum,totMappingsNum];
                    resultsStd = [resultsStd; zeros(size(boxplotdata))];
                else
                    resultsMean = [resultsMean; mean(boxplotdata(:,:)),...
                        mappingsNum,totMappingsNum];
                    resultsStd = [resultsStd; std(boxplotdata(:,:))];
                end
            end
        end
    end
    
    resultsMean(:,percCols) = resultsMean(:,percCols) * 100;
    resultsStd(:,percCols) = resultsStd(:,percCols) * 100;
    
    matrix2latex(resultsMean, resultsStd, strcat(outfolder, outFile), ...
        'rowLabels', rowLabels, 'columnLabels', colLabels, 'alignment',...
        'c', 'format', '%-6.2f');
    
    %display(stats);
    
    %sortCol = 4;
    %data = sortrows(rawstats(:,2:end),sortCol);
    %display(data);
    
    % set(gcf,'PaperUnits','centimeters')
    % xSize = 12; ySize = 9;
    % xLeft = (21-xSize)/2; yTop = (30-ySize)/2;
    % set(gcf,'PaperPosition',[xLeft yTop xSize ySize])
    % set(gcf,'Position',[0 0 xSize*50 ySize*50])
    %
    % plot(...
    %     data(:,4), data(:,5), '+-',...
    %     data(:,4), data(:,1), 'x-',... %    data(:,4), data(:,2), 'o-',...
    %     data(:,4), data(:,3), 's-',...
    %     'linewidth', 2);
    %
    % %mean((data(data(:,9)>0,12) ./ data(data(:,9)>0,9))) * 100
    %
    % legend('|Diag|', '|ProblSCCs|',...%'|GlobSCCs|',
    %     '|NontrivGlobSCCs|')
    % xlabel('Mappings Number')
    % ylabel('Cardinality')
    % print('-depsc', strcat(outfolder,'/', 'exp1.eps'))
    
    %min(stats(:,2))
    %mean(stats(:,2))
    %max(stats(:,2))
    %std(stats(:,2))
    
    %save('stats.txt', 'stats', '-ascii');
    
    %matrix2latex(resultsPQVLMean, resultsPQVLSTD, strcat(outfolder, '/PQVL.tex'), ...
    %    'rowLabels', rowLabelsPQVL, 'columnLabels', columnLabels, 'alignment', 'c', 'format', '%10.2e');
    
    %clear ; close all; clc
    
end
