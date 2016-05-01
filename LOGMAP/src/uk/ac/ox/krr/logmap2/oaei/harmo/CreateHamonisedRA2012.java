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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import uk.ac.ox.krr.logmap2.io.OutPutFilesManager;
import uk.ac.ox.krr.logmap2.io.ReadFile;
import uk.ac.ox.krr.logmap2.utilities.Utilities;

public class CreateHamonisedRA2012 {
	
	enum Pair {FMA2NCI, FMA2SNOMED, SNOMED2NCI};
	
	String iri1;
	String iri2;
	String iri_fma = "http://bioontology.org/projects/ontologies/fma/fmaOwlDlComponent_2_0";
	String iri_nci = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";
	String iri_snomed = "http://www.ihtsdo.org/snomed";
	
	String voted_mappings_file;
	
	String mappings_path = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/Mappings_Tools_2012/";
	
	String small;
	String fma2nci_small="_small_fma2nci.txt";
	String fma2nci_big="_big_fma2nci.txt";
	String fma2nci_whole="_whole_fma2nci.txt";
	
	String big;
	String fma2snomed_small="_small_fma2snomed.txt";
	String fma2snomed_big="_big_fma2snomed.txt";
	String fma2snomed_whole="_whole2_fma2snomed.txt";
	
	String whole;
	String snomed2nci_small="_small_snomed2nci.txt";
	String snomed2nci_big="_big_snomed2nci.txt";
	String snomed2nci_whole="_whole2_snomed2nci.txt";
	
	
	String gs_standard_path;
	String gs_standard_fma2nci = "/usr/local/data/DataUMLS/UMLS_source_data/onto_mappings_FMA_NCI_dirty.txt";
	String gs_standard_fma2snomed = "/usr/local/data/DataUMLS/UMLS_source_data/onto_mappings_FMA_SNOMED_dirty.txt";
	String gs_standard_snomed2nci = "/usr/local/data/DataUMLS/UMLS_source_data/onto_mappings_SNOMED_NCI_dirty.txt";
	
	
	String weights_file;
	String file_weights_f2n_path = "/home/ernesto/Desktop/Voting_Harmo/Harmo_2012/precisions_fma2nci.txt";
	String file_weights_f2s_path = "/home/ernesto/Desktop/Voting_Harmo/Harmo_2012/precisions_fma2snomed.txt";
	String file_weights_s2n_path = "/home/ernesto/Desktop/Voting_Harmo/Harmo_2012/precisions_snomed2nci.txt";
	
	
	double min_votes = 100;
	double max_votes = 0;
	
	
	
	Map<String, Map<String, Double>> mappings2votes = new HashMap<String, Map<String, Double>>();
	
	Map<String, WightedSystemMappings> System2Mappings = new HashMap<String, WightedSystemMappings>();
	
	
	Map<String, Set<String>> gs_mappings = new HashMap<String, Set<String>>();
	int size_gs_clean = 0;
	
	
	Pair ontopair = Pair.FMA2NCI;
	//Pair ontopair = Pair.FMA2SNOMED;
	//Pair ontopair = Pair.SNOMED2NCI;
	
	public CreateHamonisedRA2012() throws Exception{
		
		selectOntologyPair();
		
		readPrecisionsAndMappings();
		
		createWeightedMappings();
		
		loadMappingsGS();
		
		extactMinMaxVotingValues();
		
		statisticsMappingsAndVotes();
		
		//Create outputs for GS
		//FMA-NCI
		if (ontopair == Pair.FMA2NCI){
			createOutputMappings(3.0);
			createOutputMappings(4.0);
			createOutputMappings(5.0);
		}
		else if (ontopair == Pair.FMA2SNOMED){
			//FMA-SNOMED
			createOutputMappings(2.0);
			createOutputMappings(3.0);
			//createOutputMappings(4.0);
		}
		else {
		//SNOMED-NCI
			createOutputMappings(2.0);
			createOutputMappings(3.0);
			//createOutputMappings(4.0);
		}
	}
	
	
	private void selectOntologyPair(){
		
		if (ontopair == Pair.FMA2NCI){
			
			small = fma2nci_small;
			big = fma2nci_big;
			whole = fma2nci_whole;
			
			weights_file = file_weights_f2n_path;
			
			iri1 = iri_fma;
			iri2 = iri_nci;
			
			gs_standard_path = gs_standard_fma2nci;
			
			voted_mappings_file = "oaei2012_FMA2NCI_voted_mappings";
			
			
		}
		
		else if (ontopair == Pair.FMA2SNOMED){
			
			small = fma2snomed_small;
			big = fma2snomed_big;
			whole = fma2snomed_whole;
			
			weights_file = file_weights_f2s_path;
			
			iri1 = iri_fma;
			iri2 = iri_snomed;
			
			gs_standard_path = gs_standard_fma2snomed;
			
			voted_mappings_file = "oaei2012_FMA2SNOMED_voted_mappings";
			
			
		}
		else {
			
			small = snomed2nci_small;
			big = snomed2nci_big;
			whole = snomed2nci_whole;
			
			weights_file = file_weights_s2n_path;
			
			iri1 = iri_snomed;
			iri2 = iri_nci;
			
			gs_standard_path = gs_standard_snomed2nci;
			
			voted_mappings_file = "oaei2012_SNOMED2NCI_voted_mappings";
			
		}
		
	}
	
	
	private void readPrecisionsAndMappings(){
		
		try {
			ReadFile reader = new ReadFile(weights_file);
			
			String line;
			
			String[] elements;
			
			while ((line = reader.readLine()) != null){
				
				if (line.startsWith("#"))
					continue;
				
				if (line.contains("\t")){
					elements=line.split("\t");
					
					System.out.println(elements[0] + " - " + elements[1]  + " - " + elements[2]  + " - " + elements[3]  + " - " + elements[4]);
					
					if (!System2Mappings.containsKey(elements[0])){
						System2Mappings.put(elements[0], new WightedSystemMappings());
					}
					
					if (Double.valueOf(elements[2])>0.0){
						
						System2Mappings.get(elements[0]).addMappings(mappings_path + elements[1] + small, Double.valueOf(elements[2]));
						
					}
					if (Double.valueOf(elements[3])>0.0){
						
						System2Mappings.get(elements[0]).addMappings(mappings_path + elements[1] + big, Double.valueOf(elements[3]));
					}
					
					if (Double.valueOf(elements[4])>0.0){
						
						System2Mappings.get(elements[0]).addMappings(mappings_path + elements[1] + whole, Double.valueOf(elements[4]));
					
					}
					
				}
					
			}
			
			reader.closeBuffer();
		}
		catch (Exception e){
			e.printStackTrace();
		}
		
		
	}
	
	
	private void createWeightedMappings(){
		
		for (String tool : System2Mappings.keySet()){
			
			createWeightedMappings(System2Mappings.get(tool));
		
		}
		
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
	
	
	
	
	
	
	
	
	private void extactMinMaxVotingValues(){
		
		double vote;
		
		
		
		for (String uri1: mappings2votes.keySet()){
			for (String uri2: mappings2votes.get(uri1).keySet()){
				
				vote = mappings2votes.get(uri1).get(uri2);
				
				if (vote > max_votes){
					max_votes=vote;
				}
				
				if (vote < min_votes){
					min_votes=vote;
				}
						
						
			}
		}
		
		System.out.println("Min votes: " + min_votes);
		System.out.println("Max votes: " + max_votes);
		
		
	}
	
	
	
	private void statisticsMappingsAndVotes(){
		
		double vote;
		
		int m;
		
		int m_good;
		
		double precision;
		double recall;
		double fvalue;
		
		
			
		for (double v=min_votes; v<=max_votes; v+=1.0){
		//for (double v=min_votes; v<=max_votes; v+=0.10){
		//for (double v=min_votes; v<=max_votes; v+=0.20){
		//Normal vote max 6 min 1
		//for (double v=1.0; v<=6.0; v+=1.0){
		
			m=0;
			m_good=0;
			
			for (String uri1: mappings2votes.keySet()){
				for (String uri2: mappings2votes.get(uri1).keySet()){
				
					vote = mappings2votes.get(uri1).get(uri2);
					
					if (vote >= v){
						
						m++;
						
						if (isInGoldStandard(uri1, uri2)){
							m_good++;
						}

					}
				}
			}
			
			precision = getRoundedDecimals((double)m_good/(double)m, 3);
			recall =    getRoundedDecimals((double)m_good/(double)size_gs_clean, 3);
			
						
			fvalue = getRoundedDecimals((2*recall*precision)/(precision+recall), 3);
			
			
			System.out.println(getRoundedDecimals(v, 1) + "\t" + m + "\t" + m_good + "\t" + precision + "\t" + recall + "\t" +fvalue);
			//System.out.println(getRoundedDecimals(v, 1) + "\t" + m + "\t" + m_good_gs2 + "\t" + precision_gs2 + "\t" + recall_gs2 + "\t" +fvalue_gs2);
			//System.out.println();
			
		}
	}

	
	
	protected double getRoundedDecimals(double conf, int decimals){
		//return (double)Math.round(conf*1000.0)/1000.0;  //not good!!
		
		return (double)Math.round(conf*Math.pow(10, decimals))/Math.pow(10, decimals); 
	}
	
	
	
	private boolean isInGoldStandard(String uri1, String uri2){
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
	private void loadMappingsGS() {
	
		try{
			ReadFile reader = new ReadFile(gs_standard_path);
			
			
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
				
				size_gs_clean++;
			}		
			
			reader.closeBuffer();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	
	
	
	
	private void createOutputMappings(double min_required_votes) throws Exception{
		
		double vote;
		double conf;
		
		OutPutFilesManager output_manager = new OutPutFilesManager();
				
		output_manager.createOutFiles(
				mappings_path + voted_mappings_file + String.valueOf(min_required_votes), 
				OutPutFilesManager.AllFormats,
				iri1, 
				iri2);
		
		//For the output confidence
		double y1_min_rated = 0.8;		
		double y2_max_rated = 1.0;
		
		
		double x1_min_votes = min_required_votes;//1.8; 
		
		double x2_max_votes = max_votes;

		
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
	
	

	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		try{
			new CreateHamonisedRA2012();
		}
		catch (Exception e){
			e.printStackTrace();
		}
		
		
	}

}
