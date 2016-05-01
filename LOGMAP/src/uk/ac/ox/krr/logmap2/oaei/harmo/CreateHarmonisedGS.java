/*******************************************************************************
 * Copyright 2012 by the Department of Computer Science (University of Oxford)
 * 
 *    This file is part of LogMap.
 * 
 *    LogMap is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 * 
 *    LogMap is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 * 
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with LogMap.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package uk.ac.ox.krr.logmap2.oaei.harmo;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import uk.ac.ox.krr.logmap2.io.OutPutFilesManager;
import uk.ac.ox.krr.logmap2.io.ReadFile;
import uk.ac.ox.krr.logmap2.utilities.Utilities;


public class CreateHarmonisedGS {
	
	
	double w_gomma_bk = 0.925;
	double w_logmap = 0.935;
	double w_gomma_nobk = 0.945;
	double w_logmaplt = 0.942;
	double w_aroma = 0.802;
	double w_maas = 0.580;
	double w_csa = 0.514;
	double w_mapsss = 0.840;
	

	double w_gomma_bk_ext = 0.817;
	double w_logmap_ext = 0.877;
	double w_gomma_nobk_ext = 0.856;
	double w_logmaplt_ext = 0.726;
	double w_aroma_ext = 0.471;
	double w_csa_ext = 0.514;
	double w_mapsss_ext = 0.459;
	
	double w_gomma_bk_all = 0.806;
	double w_logmap_all = 0.868;
	double w_gomma_nobk_all = 0.845;
	double w_logmaplt_all = 0.675;
	double w_aroma_all = 0.467;
	double w_csa_all = 0.514;
	double w_mapsss_all = 0.426;
	
	String aroma = "aroma";
	String gomma = "gomma";
	String gomma_back = "gomma_back";
	String csa = "csa";
	String logmap = "logmap2";
	String logmaplt = "logmap_lite";
	String maas = "maas";
	String mapsss = "mapsss";
	
	String small = "_small.txt";
	String big = "_big.txt";
	String whole = "_whole.txt";
	
	String mappings_path = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/Mappings_Tools/";
	
	String gs_mappings_dirty_path = "/usr/local/data/DataUMLS/UMLS_source_data/onto_mappings_FMA_NCI_dirty.txt";
	String gs_mappings_clean_path = "/usr/local/data/DataUMLS/UMLS_source_data/onto_mappings_FMA_NCI_cleantDG.txt";
	Map<String, Set<String>> gs_mappings_clean = new HashMap<String, Set<String>>();
	Map<String, Set<String>> gs_mappings_dirty = new HashMap<String, Set<String>>();

	Integer size_gs_clean = new Integer(0);
	Integer size_gs_dirty = new Integer(0);
	
	
	Map<String, Map<String, Double>> mappings2votes = new HashMap<String, Map<String, Double>>();
	
	
	WightedSystemMappings aromaMappings = new WightedSystemMappings();
	WightedSystemMappings gommaBackMappings = new WightedSystemMappings();
	WightedSystemMappings gommaMappings = new WightedSystemMappings();
	WightedSystemMappings logmapMappings = new WightedSystemMappings();
	WightedSystemMappings logmapLtMappings = new WightedSystemMappings();
	WightedSystemMappings csaMappings = new WightedSystemMappings();
	WightedSystemMappings maasMappings = new WightedSystemMappings();
	WightedSystemMappings mapsssMappings = new WightedSystemMappings();
	
	
	
	public CreateHarmonisedGS() throws Exception{
		
		//Aroma
		aromaMappings.addMappings(mappings_path+aroma+small, w_aroma);
		aromaMappings.addMappings(mappings_path+aroma+big, w_aroma_ext);
		aromaMappings.addMappings(mappings_path+aroma+whole, w_aroma_all);
		
		//Gomma back
		///gommaBackMappings.addMappings(mappings_path+gomma_back+small, w_gomma_bk);
		//gommaBackMappings.addMappings(mappings_path+gomma_back+big, w_gomma_bk_ext);
		//gommaBackMappings.addMappings(mappings_path+gomma_back+whole, w_gomma_bk_all);
		
		//Gomma (we add vote to GOMMA)
		gommaMappings.addMappings(mappings_path+gomma+small, w_gomma_nobk);
		gommaMappings.addMappings(mappings_path+gomma+big, w_gomma_nobk_ext);
		gommaMappings.addMappings(mappings_path+gomma+whole, w_gomma_nobk_all);
		gommaMappings.addMappings(mappings_path+gomma_back+small, w_gomma_bk);
		gommaMappings.addMappings(mappings_path+gomma_back+big, w_gomma_bk_ext);
		gommaMappings.addMappings(mappings_path+gomma_back+whole, w_gomma_bk_all);
		
		//Logmap
		logmapMappings.addMappings(mappings_path+logmap+small, w_logmap);
		logmapMappings.addMappings(mappings_path+logmap+big, w_logmap_ext);
		logmapMappings.addMappings(mappings_path+logmap+whole, w_logmap_all);
		
		//Logmap lt (we add vote to logmap)
		//logmapLtMappings.addMappings(mappings_path+logmaplt+small, w_logmaplt);
		//logmapLtMappings.addMappings(mappings_path+logmaplt+big, w_logmaplt_ext);
		//logmapLtMappings.addMappings(mappings_path+logmaplt+whole, w_logmaplt_all);
		logmapMappings.addMappings(mappings_path+logmaplt+small, w_logmaplt);
		logmapMappings.addMappings(mappings_path+logmaplt+big, w_logmaplt_ext);
		logmapMappings.addMappings(mappings_path+logmaplt+whole, w_logmaplt_all);
		
		//csa
		csaMappings.addMappings(mappings_path+csa+small, w_csa);
		csaMappings.addMappings(mappings_path+csa+big, w_csa_ext);
		csaMappings.addMappings(mappings_path+csa+whole, w_csa_all);
		
		//maas
		maasMappings.addMappings(mappings_path+maas+small, w_maas);
		
		//mapsss
		mapsssMappings.addMappings(mappings_path+mapsss+small, w_mapsss);
		mapsssMappings.addMappings(mappings_path+mapsss+big, w_mapsss_ext);
		mapsssMappings.addMappings(mappings_path+mapsss+whole, w_mapsss_all);
		
		
		//
		createWeightedMappings(aromaMappings);
		//createWeightedMappings(gommaBackMappings);
		createWeightedMappings(gommaMappings);
		createWeightedMappings(logmapMappings);
		createWeightedMappings(logmapLtMappings);
		createWeightedMappings(csaMappings);
		createWeightedMappings(maasMappings);
		createWeightedMappings(mapsssMappings);
		
		loadMappingsGS(gs_mappings_clean_path, gs_mappings_clean, true);
		loadMappingsGS(gs_mappings_dirty_path, gs_mappings_dirty, false);
		
		//countMappingsAndVotes();
		statisticsMappingsAndVotes();
	
		//Output
		createOutputMappings();
		
	}
	
	
	
	private void createWeightedMappings(WightedSystemMappings mappings4system){
		
		for (String uri1 : mappings4system.getMappings2Weight().keySet()){
			
			for (String uri2 : mappings4system.getMappings2Weight().get(uri1).keySet()){
			
				addWeightedVote(uri1, uri2, mappings4system.getMappings2Weight().get(uri1).get(uri2));
				//addVote(uri1, uri2);
			}
				
		}
		
	}
	
	

	private void addWeightedVote(String uri1, String uri2, double weight){
		
		if (!mappings2votes.containsKey(uri1)){
			mappings2votes.put(uri1, new HashMap<String, Double>());
		}
		
			
		if (mappings2votes.get(uri1).containsKey(uri2)){
			//We add value
			double new_weight = mappings2votes.get(uri1).get(uri2) + weight;		
			mappings2votes.get(uri1).put(uri2, new_weight); //And replace previous one
				
		}
		else{
			//No entry for uri2
			mappings2votes.get(uri1).put(uri2, weight);
		}
				
	}
	
	
	/**
	 * Not weighted vote
	 * @param uri1
	 * @param uri2
	 */
	private void addVote(String uri1, String uri2){
		
		if (!mappings2votes.containsKey(uri1)){
			mappings2votes.put(uri1, new HashMap<String, Double>());
		}
		
			
		if (mappings2votes.get(uri1).containsKey(uri2)){
			//We add value
			double new_weight = mappings2votes.get(uri1).get(uri2) + 1.0;		
			mappings2votes.get(uri1).put(uri2, new_weight); //And replace previous one
				
		}
		else{
			//No entry for uri2
			mappings2votes.get(uri1).put(uri2, 1.0);
		}
				
	}
	
	

	
	private void createOutputMappings() throws Exception{
		
		double vote;
		double conf;
		
		OutPutFilesManager output_manager = new OutPutFilesManager();
				
		output_manager.createOutFiles(
				mappings_path + "oaei2012_FMA2NCI_voted_mappings", 
				OutPutFilesManager.AllFormats,
				"http://bioontology.org/projects/ontologies/fma/fmaOwlDlComponent_2_0", 
				"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");
		
		double y1_min_rated = 0.8;
		
		double y2_max_rated = 1.0;
		
		double x1_min_votes = 1.8;
		
		double x2_max_votes = 4.7;
		
		double coef = (y2_max_rated-y1_min_rated)/(x2_max_votes-x1_min_votes);
		
		
		for (String uri1: mappings2votes.keySet()){
			for (String uri2: mappings2votes.get(uri1).keySet()){
				
				vote = mappings2votes.get(uri1).get(uri2);
				
				if (vote >= x1_min_votes){ //best f-value
					
					conf = coef*(vote-x1_min_votes) + y1_min_rated;  
							
					
					//Voting + confidence?? Give a ratio? 1.8 = 0.8
					output_manager.addClassMapping2Files(uri1, uri2, Utilities.EQ, conf);
					
					
				}
				
			}
		}
		
		output_manager.closeAndSaveFiles();
		
		
	}
	
	
	
	
	
	
	private void statisticsMappingsAndVotes(){
		
		double vote;
		
		int m;
		
		int m_good_gs1;
		int m_good_gs2;
		
		double precision_gs1;
		double recall_gs1;
		double fvalue_gs1;
		
		double precision_gs2;
		double recall_gs2;
		double fvalue_gs2;
		
		
		//Weigted vote
		double min = 0.4;
		double max = 4.7;//6.4
		for (double v=min; v<=max; v+=0.20){
		//Normal vote max 6 min 1
		//for (double v=1.0; v<=6.0; v+=1.0){
		
			m=0;
			m_good_gs1=0;
			m_good_gs2=0;
			
			for (String uri1: mappings2votes.keySet()){
				for (String uri2: mappings2votes.get(uri1).keySet()){
				
					vote = mappings2votes.get(uri1).get(uri2);
					
					if (vote >= v){
						
						m++;
						
						if (isInGoldStandard(uri1, uri2, gs_mappings_clean)){
							m_good_gs1++;
						}
						if (isInGoldStandard(uri1, uri2, gs_mappings_dirty)){
							m_good_gs2++;
						}
					}
				}
			}
			
			precision_gs1 = getRoundedDecimals((double)m_good_gs1/(double)m, 3);
			recall_gs1 =    getRoundedDecimals((double)m_good_gs1/(double)size_gs_clean, 3);
			
			precision_gs2 = getRoundedDecimals((double)m_good_gs2/(double)m, 3);
			recall_gs2 =    getRoundedDecimals((double)m_good_gs2/(double)size_gs_dirty, 3);
			
			fvalue_gs1 = getRoundedDecimals((2*recall_gs1*precision_gs1)/(precision_gs1+recall_gs1), 3);
			fvalue_gs2 = getRoundedDecimals((2*recall_gs2*precision_gs2)/(precision_gs2+recall_gs2), 3);
			
			
			System.out.println(getRoundedDecimals(v, 1) + "\t" + m + "\t" + m_good_gs1 + "\t" + precision_gs1 + "\t" + recall_gs1 + "\t" +fvalue_gs1);
			//System.out.println(getRoundedDecimals(v, 1) + "\t" + m + "\t" + m_good_gs2 + "\t" + precision_gs2 + "\t" + recall_gs2 + "\t" +fvalue_gs2);
			//System.out.println();
			
		}
	}

	
	
	protected double getRoundedDecimals(double conf, int decimals){
		//return (double)Math.round(conf*1000.0)/1000.0;
		
		return (double)Math.round(conf*Math.pow(10, decimals))/Math.pow(10, decimals); 
	}
	
	
	
	private boolean isInGoldStandard(String uri1, String uri2, Map<String, Set<String>> gs_mappings){
		if (!gs_mappings.containsKey(uri1)){
			return false;
		}
		
		if (!gs_mappings.get(uri1).contains(uri2)){
			return false;
		}
		
		return true;
		
	}
	
	
	/**
	 * Load Gold Standard Mappings
	 * @throws Exception
	 */
	private void loadMappingsGS(String path, Map<String, Set<String>> gs_mappings, boolean clean) {
	
		try{
			ReadFile reader = new ReadFile(path);
			
			
			String line;
			String[] elements;
			
			while ((line = reader.readLine()) != null){
				
				if (line.indexOf("|")<0){
					continue;
				}
				
				elements=line.split("\\|");
				
				if (!gs_mappings.containsKey(elements[0])){
					gs_mappings.put(elements[0], new HashSet<String>());
				}
				
				gs_mappings.get(elements[0]).add(elements[1]);
				
				if (clean)
					size_gs_clean++;
				else
					size_gs_dirty++;
			}		
			
			reader.closeBuffer();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * @deprecated
	 */
	private void countMappingsAndVotes(){
		
		
		int mappings=0;
		int mappings_good1 = 0;
		int mappings_good2 = 0;
		
		int votes1 = 0;
		int votes1_good1 = 0;
		int votes1_good2 = 0;
		
		int votes1_5 = 0;
		int votes1_5_good1 = 0;
		int votes1_5_good2 = 0;
		
		int votes2 = 0;
		int votes2_good1 = 0;
		int votes2_good2 = 0;
		
		int votes2_5 = 0;
		int votes2_5_good1 = 0;
		int votes2_5_good2 = 0;
		
		int votes3 = 0;
		int votes3_good1 = 0;
		int votes3_good2 = 0;
		
		
		int votes4 = 0;
		int votes4_good1 = 0;
		int votes4_good2 = 0;
		
		int votes5 = 0;
		int votes5_good1 = 0;
		int votes5_good2 = 0;
		
		int votes6 = 0;//max 6.4
		int votes6_good1 = 0;
		int votes6_good2 = 0;
		
		double vote;
		
		for (String uri1: mappings2votes.keySet()){
			for (String uri2: mappings2votes.get(uri1).keySet()){
				
				mappings++;//at least "one" vote
				if (isInGoldStandard(uri1, uri2, gs_mappings_clean)){
					mappings_good1++;
				}
				if (isInGoldStandard(uri1, uri2, gs_mappings_dirty)){
					mappings_good2++;
				}
				
				
				vote = mappings2votes.get(uri1).get(uri2);
				
				
				for (double v=0.0; v<=6.4; v+=0.1){
					
					
					
					
				}
				
				
				
				
				if (vote>=1){
					votes1++;
					if (isInGoldStandard(uri1, uri2, gs_mappings_clean)){
						votes1_good1++;
					}
					if (isInGoldStandard(uri1, uri2, gs_mappings_dirty)){
						votes1_good2++;
					}
				}
				
				if (vote>=1.5){
					votes1_5++;
					
					if (isInGoldStandard(uri1, uri2, gs_mappings_clean)){
						votes1_5_good1++;
					}
					if (isInGoldStandard(uri1, uri2, gs_mappings_dirty)){
						votes1_5_good2++;
					}
				
				}
				
				if (vote>=2.0){
					votes2++;
					
					if (isInGoldStandard(uri1, uri2, gs_mappings_clean)){
						votes2_good1++;
					}
					if (isInGoldStandard(uri1, uri2, gs_mappings_dirty)){
						votes2_good2++;
					}
					
				}
				
				if (vote>=2.5){
					votes2_5++;
					
					if (isInGoldStandard(uri1, uri2, gs_mappings_clean)){
						votes2_5_good1++;
					}
					if (isInGoldStandard(uri1, uri2, gs_mappings_dirty)){
						votes2_5_good2++;
					}
					
				}
				
				
				if (vote>=3.0){
					votes3++;
					
					if (isInGoldStandard(uri1, uri2, gs_mappings_clean)){
						votes3_good1++;
					}
					if (isInGoldStandard(uri1, uri2, gs_mappings_dirty)){
						votes3_good2++;
					}
				}
				
				if (vote>=4.0){
					votes4++;
					
					if (isInGoldStandard(uri1, uri2, gs_mappings_clean)){
						votes4_good1++;
					}
					if (isInGoldStandard(uri1, uri2, gs_mappings_dirty)){
						votes4_good2++;
					}
				}
				
				if (vote>=5.0){
					votes5++;
					
					if (isInGoldStandard(uri1, uri2, gs_mappings_clean)){
						votes5_good1++;
					}
					if (isInGoldStandard(uri1, uri2, gs_mappings_dirty)){
						votes5_good2++;
					}
				}
				
				if (vote>=6.0){
					votes6++;
					
					if (isInGoldStandard(uri1, uri2, gs_mappings_clean)){
						votes6_good1++;
					}
					if (isInGoldStandard(uri1, uri2, gs_mappings_dirty)){
						votes6_good2++;
					}
				}
				
				
			}
		}
		
		System.out.println(size_gs_clean);
		System.out.println(size_gs_dirty);
		
		System.out.println("All mappings: " + mappings);
		System.out.println("\tIn GS clean: " + mappings_good1 + ", P= " + (double)mappings_good1/(double)mappings + ", R= " + (double)mappings_good1/(double)size_gs_clean);
		System.out.println("\tIn GS drity: " + mappings_good2 + ", P= " + (double)mappings_good2/(double)mappings + ", R= " + (double)mappings_good2/(double)size_gs_dirty);
		System.out.println("");
		
		System.out.println("Mappings with votes >=1: " + votes1);
		System.out.println("\tIn GS clean: " + votes1_good1 + ", P= " + (double)votes1_good1/(double)votes1 + ", R= " + (double)votes1_good1/(double)size_gs_clean);
		System.out.println("\tIn GS drity: " + votes1_good2 + ", P= " + (double)votes1_good2/(double)votes1 + ", R= " + (double)votes1_good2/(double)size_gs_dirty);
		System.out.println("");
		
		System.out.println("Mappings with votes >=1.5: " + votes1_5);
		System.out.println("Mappings with votes >=1: " + votes1_5);
		System.out.println("\tIn GS clean: " + votes1_5_good1 + ", P= " + (double)votes1_5_good1/(double)votes1_5 + ", R= " + (double)votes1_5_good1/(double)size_gs_clean);
		System.out.println("\tIn GS drity: " + votes1_5_good2 + ", P= " + (double)votes1_5_good2/(double)votes1_5 + ", R= " + (double)votes1_5_good2/(double)size_gs_dirty);
		System.out.println("");
		
		System.out.println("Mappings with votes >=2: " + votes2);
		System.out.println("\tIn GS clean: " + votes2_good1 + ", P= " + (double)votes2_good1/(double)votes2 + ", R= " + (double)votes2_good1/(double)size_gs_clean);
		System.out.println("\tIn GS drity: " + votes2_good2 + ", P= " + (double)votes2_good2/(double)votes2 + ", R= " + (double)votes2_good2/(double)size_gs_dirty);
		System.out.println("");
		
		System.out.println("Mappings with votes >=2.5: " + votes2_5);
		System.out.println("\tIn GS clean: " + votes2_5_good1 + ", P= " + (double)votes2_5_good1/(double)votes2_5 + ", R= " + (double)votes2_5_good1/(double)size_gs_clean);
		System.out.println("\tIn GS drity: " + votes2_5_good2 + ", P= " + (double)votes2_5_good2/(double)votes2_5 + ", R= " + (double)votes2_5_good2/(double)size_gs_dirty);
		System.out.println("");
		
		System.out.println("Mappings with votes >=3: " + votes3);
		System.out.println("\tIn GS clean: " + votes3_good1 + ", P= " + (double)votes3_good1/(double)votes3 + ", R= " + (double)votes3_good1/(double)size_gs_clean);
		System.out.println("\tIn GS drity: " + votes3_good2 + ", P= " + (double)votes3_good2/(double)votes3 + ", R= " + (double)votes3_good2/(double)size_gs_dirty);
		System.out.println("");
		
		System.out.println("Mappings with votes >=4: " + votes4);
		System.out.println("\tIn GS clean: " + votes4_good1 + ", P= " + (double)votes4_good1/(double)votes4 + ", R= " + (double)votes4_good1/(double)size_gs_clean);
		System.out.println("\tIn GS drity: " + votes4_good2 + ", P= " + (double)votes4_good2/(double)votes4 + ", R= " + (double)votes4_good2/(double)size_gs_dirty);
		System.out.println("");
		
		System.out.println("Mappings with votes >=5: " + votes5);
		System.out.println("\tIn GS clean: " + votes5_good1 + ", P= " + (double)votes5_good1/(double)votes5 + ", R= " + (double)votes5_good1/(double)size_gs_clean);
		System.out.println("\tIn GS drity: " + votes5_good2 + ", P= " + (double)votes5_good2/(double)votes5 + ", R= " + (double)votes5_good2/(double)size_gs_dirty);
		
		System.out.println("Mappings with votes >=6: " + votes6);
		System.out.println("\tIn GS clean: " + votes6_good1 + ", P= " + (double)votes6_good1/(double)votes6 + ", R= " + (double)votes6_good1/(double)size_gs_clean);
		System.out.println("\tIn GS drity: " + votes6_good2 + ", P= " + (double)votes6_good2/(double)votes6 + ", R= " + (double)votes6_good2/(double)size_gs_dirty);
		System.out.println("");
		
	}
	
	
	
	
	
	public static void main(String[] args) {
	
		try{
			new CreateHarmonisedGS();
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}
	
	
	
	
	
	
	
	

}
