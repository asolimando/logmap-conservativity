function exp5(infolder,pattern)

display(infolder)
display(pattern)

outFile='umls.tex';

dirListing = dir(fullfile(infolder, pattern));
if exist(outFile,'file')==2
    delete(outFile);
end

labels = {'wM', 'NLocalSCCs', 'EdgesLoadTime2', 'EdgesLoadTime1', 'NumD',...
    'NumPM', 'NumEdges1', 'DiagTime',...
    'NumNontrivGlobalSCCs', 'NumGlobalSCCs', 'NumProblSCCs', 'NumEdges2',...
    'TotTime', 'VtxLoadTime1', 'VtxLoadTime2', 'NumVtx2',...
    'NumVtx1', 'wPM', 'wD', 'ProblMapppingsDetTime', 'NumM','NumVSCCs',...
    'NumSubDiag','1M'};
labelsId = zeros(1,length(labels));

wMid = 1;
NLocalSCCsid = 2;
EdgesLoadTime2id = 3;
EdgesLoadTime1id = 4;
NumDid = 5;
NumPMid = 6;
NumEdges1id = 7;
DiagTimeid = 8;
NumNontrivGlobalSCCsid = 9;
NumGlobalSCCsid = 10;
NumProblSCCsid = 11;
NumEdges2id = 12;
TotTimeid = 13;
VtxLoadTime1id = 14;
VtxLoadTime2id = 15;
NumVtx2id = 16;
NumVtx1id = 17;
wPMid = 18;
wDid = 19;
ProblMapppingsDetTimeid = 20;
NumMid = 21;
NumVSCCsid = 22;
NumSubDiagid = 23;
oneMid = 24;

for d = 1:length(dirListing)
    if ~dirListing(d).isdir
        % use full path because the folder may not be the active path
        filename = fullfile(infolder,dirListing(d).name);
        display(filename);
        %filename = dirListing(d).name;
        
        fid = fopen(filename, 'rt');
        headers = fgets(fid);
        headers = regexp(headers, '\s', 'split');
        
        aggrMatr = textscan(fid,'%s %s %*[^\n]','Delimiter',' ');
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
        NumPMid = labelsId(1,NumPMid);
        NumEdges1id = labelsId(1,NumEdges1id);
        DiagTimeid = labelsId(1,DiagTimeid);
        NumNontrivGlobalSCCsid = labelsId(1,NumNontrivGlobalSCCsid);
        NumGlobalSCCsid = labelsId(1,NumGlobalSCCsid);
        NumProblSCCsid = labelsId(1,NumProblSCCsid);
        NumEdges2id = labelsId(1,NumEdges2id);
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
        oneMid = labelsId(1,oneMid);

        %last two params are rows and cols to ignore
        data = dlmread(filename, ' ', 1,1);
        
        % compute derived columns of interest
        rawstats = [...
            data(:,NumDid) ./ data(:,NumPMid),...
            data(:,NumPMid) ./ data(:,NumMid),...
            data(:,NumDid) ./ data(:,NumMid),...
            data(:,oneMid) ./ data(:,NumMid),...
            data(:,NumMid),...
            data(:,NumVSCCsid) ./ (data(:,NumVtx1id) + data(:,NumVtx2id)),...
            data(:,NumProblSCCsid),...
            data(:,NumNontrivGlobalSCCsid),...
			(data(:,DiagTimeid)/1000),...
	        (data(:,NumProblSCCsid) - data(:,NumSubDiagid)) ./ data(:,NumProblSCCsid),...
            %data(:,NumSubDiagid) ./ data(:,NumProblSCCsid),...
        ];

        rowLabels = [];
        for i = 1:size(aggrMatr{1,1},1)
            rowLabels{1,i} = strrep(aggrMatr{1,1}{i,:},'original',strcat('-',aggrMatr{1,2}{i,:}));
            % = cellstr(
        end        
        colLabels = {'\% D/PM','\% PM/M','\% D/M','\% 1-1M','$\vert$ M $\vert$','\% VtxSCC/Vtx',...
            'probSCC','NontrGlSCC','ASP (s)','\% OptDiag'};

        percCols = [(1 : 4), 6, 10];

        % convert into percentage
        rawstats(:,percCols) = rawstats(:,percCols) * 100;
	
    	matrix2latexSingle(rawstats, strcat('', outFile), ...
        'rowLabels', rowLabels, 'columnLabels', colLabels, 'alignment',...
        'c', 'format', '%-6.2f');
    end    
end
