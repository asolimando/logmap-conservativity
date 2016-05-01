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
package uk.ac.ox.krr.logmap2.oaei.eval;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.Set;

import de.unima.alcomox.mapping.Characteristic;
import de.unima.alcomox.mapping.Correspondence;
import de.unima.alcomox.mapping.Mapping;
import de.unima.alcomox.ontology.Entity;

import uk.ac.ox.krr.logmap2.io.LogOutput;
import uk.ac.ox.krr.logmap2.io.ReadFile;
import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;
import uk.ac.ox.krr.logmap2.statistics.StatisticsManager;

public class GetPrecisionAndRecallMappings {

	//For precission and recall
	private Set<MappingObjectStr> mappings_gs_clean = new HashSet<MappingObjectStr>();
	private Set<MappingObjectStr> mappings_gs_dirty = new HashSet<MappingObjectStr>();
	private Set<MappingObjectStr> mappings = new HashSet<MappingObjectStr>();
	
	
	private String file_gs_clean = "/usr/local/data/DataUMLS/UMLS_source_data/onto_mappings_FMA_NCI_cleantDG.txt";
	private String file_gs_dirty = "/usr/local/data/DataUMLS/UMLS_source_data/onto_mappings_FMA_NCI_dirty.txt";
	
	private String file_mappings;
			
	
	public GetPrecisionAndRecallMappings() throws Exception{
		
		
		//Mapping ref = new Mapping("/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/oaei_2013/reference_alignment/repaired_alignments/oaei2013_FMA2NCI_repaired_UMLS_mappings.rdf");
		Mapping ref = new Mapping("/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/oaei_2013/reference_alignment/repaired_alignments/oaei2013_FMA2SNOMED_repaired_UMLS_mappings.rdf");
		//Mapping ref = new Mapping("/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/oaei_2013/reference_alignment/repaired_alignments/oaei2013_SNOMED2NCI_repaired_UMLS_mappings.rdf");
		
		Mapping mapping = new Mapping("/home/ernesto/OM_OAEI/OAEI_2013_new_stuff/SPHeRe/OAEI_2013_BioMed_Tasks/Task4.xml");
		
		Set<Correspondence> correspondencesAsSet = new HashSet<Correspondence>();
		
		for (Correspondence c : mapping.getCorrespondencesAsSet()){
			
			//new Correspondence()
			
			//Entity tmp = c.getSourceEntity();
			
			//c.setSourceEntity(c.getTargetEntity());
			
			//c.setTargetEntity(tmp);
			
			correspondencesAsSet.add(new Correspondence(c.getTargetEntityUri(), c.getSourceEntityUri(), c.getRelation(), c.getConfidence()));
			//correspondencesAsSet.add(new Correspondence(c.getSourceEntityUri(), c.getTargetEntityUri(), c.getRelation(), c.getConfidence()));
			
		}
		
		Mapping mapping2 = new Mapping(correspondencesAsSet);//
		
		mapping2.write("/home/ernesto/OM_OAEI/OAEI_2013_new_stuff/SPHeRe/OAEI_2013_BioMed_Tasks/sphere_fs2.rdf");
		
		
		System.out.println("Size ref: " + ref.size());
		System.out.println("Size mapping: " + mapping.size());
		System.out.println("Size mapping2: " + mapping2.size());
		
		Characteristic scores = new Characteristic(mapping2, ref);
		
		
		
		System.out.println("Scores: " + scores.toShortDesc());
		System.out.println("P: " + scores.getPrecision());
		System.out.println("R: " + scores.getRecall());
		System.out.println("F: " + scores.getFMeasure());
		
		if (true)
			return;
		
		
		
		
		
		loadMappings(mappings_gs_clean, file_gs_clean);
		loadMappings(mappings_gs_dirty, file_gs_dirty);
		
		//file_mappings = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/Mappings_Tools/mapsss_small.txt";
		//file_mappings = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/Mappings_Tools/mapsss_big.txt";
		//file_mappings = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/Mappings_Tools/mapsss_whole.txt";
		
		//file_mappings = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/Mappings_Tools/logmap2_whole.txt";
		file_mappings = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/Mappings_Tools/mappso_small.txt";
				
		loadMappings(mappings, file_mappings);
		
		
		System.out.println("WRT CLEAN GS mappings");
		getPrecisionAndRecallMappings(mappings_gs_clean);
		
		System.out.println("WRT DIRTY GS mappings");
		getPrecisionAndRecallMappings(mappings_gs_dirty);
		
		
	}
	
	
	
	/**
	 * Load Gold Standard Mappings
	 * @throws Exception
	 */
	private void loadMappings(Set<MappingObjectStr> mappings, String file) throws Exception{
	
		ReadFile reader = new ReadFile(file);
		
		
		String line;
		String[] elements;
		
		line=reader.readLine();
		
		int index1;
		int index2;
		double confidence;
		
		while (line!=null) {
			
			if (line.indexOf("|")<0){
				line=reader.readLine();
				continue;
			}
			
			elements=line.split("\\|");
			
			mappings.add(new MappingObjectStr(elements[0], elements[1]));
				
			line=reader.readLine();
		}		
		
		reader.closeBuffer();

	}
	

	
	private void getPrecisionAndRecallMappings(Set<MappingObjectStr> mappings_gs) throws Exception{

		
		Set <MappingObjectStr> intersection;
		
		
		double precision;
		double recall;
		
		
		
		System.out.println("MAPPINGS: " + mappings.size());
		
		
		//ALL UMLS MAPPINGS
		intersection=new HashSet<MappingObjectStr>(mappings);
		intersection.retainAll(mappings_gs);
		
		StatisticsManager.setGoodMFinal(intersection.size());
		
		
		precision=((double)intersection.size())/((double)mappings.size());
		recall=((double)intersection.size())/((double)mappings_gs.size());

		

		//System.out.println("WRT GS MAPPINGS");
		System.out.println("\tPrecision Mappings: " + precision);
		System.out.println("\tRecall Mapping: " + recall);
		System.out.println("\tF measure: " + (2*recall*precision)/(precision+recall));
		
		
		
		Set <MappingObjectStr> difference;
        difference=new HashSet<MappingObjectStr>(mappings_gs);
        difference.removeAll(mappings);
        //LogOutput.print("Difference in GS: " + difference.size());
        System.out.println("\tDifference in GS: " + difference.size());
        
        Set <MappingObjectStr> difference2;
        difference2=new HashSet<MappingObjectStr>(mappings);
        difference2.removeAll(mappings_gs);
        //LogOutput.print("Difference in Candidates: " + difference2.size());
        System.out.println("\tDifference in Candidates: " + difference2.size());
        
        //for (MappingObjectStr mapping : difference2){
        //	System.out.println(mapping.getIRIStrEnt1() + "  " + mapping.getIRIStrEnt2());
        //}
        
               
        
       
      
	}
	

	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		try{
			new GetPrecisionAndRecallMappings();
		}
		catch (Exception e){
			e.printStackTrace();
		}
		
	}

}
