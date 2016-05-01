package uk.ac.ox.krr.logmap2.oaei.harmo;

import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.List;

import uk.ac.ox.krr.logmap2.io.ReadFile;
import uk.ac.ox.krr.logmap2.io.WriteFile;
import uk.ac.ox.krr.logmap2.oaei.harmo.CreateHamonisedRA2012.Pair;

import de.unima.alcomox.mapping.Characteristic;
import de.unima.alcomox.mapping.Mapping;

/**
 * 
 * @author Ernesto
 *
 */
public class DistanceMatrix {
 
	String mappings_path = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/Mappings_Tools_2012/";
	
	String ext = ".rdf";
	
	String small;
	String fma2nci_small="_small_fma2nci";
	String fma2nci_big="_big_fma2nci";
	String fma2nci_whole="_whole_fma2nci";
	
	String big;
	String fma2snomed_small="_small_fma2snomed";
	String fma2snomed_big="_big_fma2snomed";
	String fma2snomed_whole="_whole2_fma2snomed";
	
	String whole;
	String snomed2nci_small="_small_snomed2nci";
	String snomed2nci_big="_big_snomed2nci";
	String snomed2nci_whole="_whole2_snomed2nci";
	
	
	String gs_standard;
	String gs_standard_fma2nci = "oaei2012_FMA2NCI_original_UMLS_mappings.rdf";
	String gs_standard_fma2snomed = "oaei2012_FMA2SNMD_original_UMLS_mappings.rdf";
	String gs_standard_snomed2nci = "oaei2012_SNMD2NCI_original_UMLS_mappings.rdf";
	
	
	String gs_standard_logmap;
	String gs_standard_fma2nci_logmap = "oaei2012_FMA2NCI_repaired_UMLS_mappings_logmap.rdf";
	String gs_standard_fma2snomed_logmap = "oaei2012_FMA2SNMD_repaired_UMLS_mappings_logmap.rdf";
	String gs_standard_snomed2nci_logmap = "oaei2012_SNMD2NCI_repaired_UMLS_mappings_logmap.rdf";
	
	String gs_standard_alcomo;
	String gs_standard_fma2nci_alcomo = "oaei2012_FMA2NCI_repaired_UMLS_mappings_alcomo.rdf";
	String gs_standard_fma2snomed_alcomo = "oaei2012_FMA2SNMD_repaired_UMLS_mappings_alcomo.rdf";
	String gs_standard_snomed2nci_alcomo = "oaei2012_SNMD2NCI_repaired_UMLS_mappings_alcomo.rdf";
	
	
	
	//In that case will give us the names of the tools in ecah track/task
	String weights_file;
	String file_weights_f2n_path = "/home/ernesto/Desktop/Voting_Harmo/Harmo_2012/precisions_fma2nci.txt";
	String file_weights_f2s_path = "/home/ernesto/Desktop/Voting_Harmo/Harmo_2012/precisions_fma2snomed.txt";
	String file_weights_s2n_path = "/home/ernesto/Desktop/Voting_Harmo/Harmo_2012/precisions_snomed2nci.txt";
	
	String voted_mappings_file;
	List<String> votes;
	
	LinkedHashMap<String, Mapping> System2Mappings_small = new LinkedHashMap<String, Mapping>();
	LinkedHashMap<String, Mapping> System2Mappings_big = new LinkedHashMap<String, Mapping>();
	LinkedHashMap<String, Mapping> System2Mappings_whole = new LinkedHashMap<String, Mapping>();
	
	
	//Pair ontopair = Pair.FMA2NCI;
	//Pair ontopair = Pair.FMA2SNOMED;
	Pair ontopair = Pair.SNOMED2NCI;
	
	WriteFile writer;
	
	
	
	public DistanceMatrix() throws Exception{
		
		//Read mappings and compare them to each other...
		selectOntologyPair();
		
		readToolMappings();
		
		readGSMappings("UMLS", gs_standard);
		readGSMappings("UMLS_L", gs_standard_logmap);
		readGSMappings("UMLS_A", gs_standard_alcomo);
					
		for (String vote : votes){
			readGSMappings("Vote_"+Double.valueOf(vote).intValue(), voted_mappings_file + vote + ".rdf");
		}
		
		
				
		createDistanceMatrix(System2Mappings_small, "distance_M_" + small);
		createDistanceMatrix(System2Mappings_big, "distance_M_" + big);
		createDistanceMatrix(System2Mappings_whole, "distance_M_" + whole);
		
		
	}
	
	
	
	private void selectOntologyPair(){
		
		if (ontopair == Pair.FMA2NCI){
			
			small = fma2nci_small;
			big = fma2nci_big;
			whole = fma2nci_whole;
			
			weights_file = file_weights_f2n_path;
			
					
			gs_standard = gs_standard_fma2nci;
			gs_standard_alcomo = gs_standard_fma2nci_alcomo;
			gs_standard_logmap = gs_standard_fma2nci_logmap;
			
			voted_mappings_file = "oaei2012_FMA2NCI_voted_mappings";
			
			votes = Arrays.asList("3.0", "4.0", "5.0");
			
			
		}
		
		else if (ontopair == Pair.FMA2SNOMED){
			
			small = fma2snomed_small;
			big = fma2snomed_big;
			whole = fma2snomed_whole;
			
			weights_file = file_weights_f2s_path;
			
			
			gs_standard = gs_standard_fma2snomed;
			gs_standard_logmap = gs_standard_fma2snomed_logmap;
			gs_standard_alcomo = gs_standard_fma2snomed_alcomo;
			
			voted_mappings_file = "oaei2012_FMA2SNOMED_voted_mappings";
			
			votes = Arrays.asList("2.0", "3.0");
			
			
		}
		else {
			
			small = snomed2nci_small;
			big = snomed2nci_big;
			whole = snomed2nci_whole;
			
			weights_file = file_weights_s2n_path;
			
			gs_standard = gs_standard_snomed2nci;
			gs_standard_alcomo = gs_standard_snomed2nci_alcomo;
			gs_standard_logmap = gs_standard_snomed2nci_logmap;
			
			
			voted_mappings_file = "oaei2012_SNOMED2NCI_voted_mappings";
			
			votes = Arrays.asList("2.0", "3.0");
			
		}
		
	}
	
	
	private void readGSMappings(String name, String file_name) throws Exception{
		
		System2Mappings_small.put(name, new Mapping(mappings_path + file_name));
		System2Mappings_big.put(name, new Mapping(mappings_path + file_name));
		System2Mappings_whole.put(name, new Mapping(mappings_path + file_name));
		
	}
	
	
	private void readToolMappings(){
		
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
					
					
					
					if (Double.valueOf(elements[2])>0.0){
						System2Mappings_small.put(elements[5], new Mapping(mappings_path + elements[1] + small + ext));
					}
					
					
					if (Double.valueOf(elements[3])>0.0){
						
						System2Mappings_big.put(elements[5], new Mapping(mappings_path + elements[1] + big + ext));
					}
					
					if (Double.valueOf(elements[4])>0.0){
						
						System2Mappings_whole.put(elements[5], new Mapping(mappings_path + elements[1] + whole + ext));
					
					}
					
				}
					
			}
			
			reader.closeBuffer();
		}
		catch (Exception e){
			e.printStackTrace();
		}
		
		
	}
	
	
	private void createDistanceMatrix(LinkedHashMap<String, Mapping> System2Mappings, String nameOutputFile){
	
		
		writer =  new WriteFile(mappings_path + nameOutputFile);
	
		String line="";
	
		
		for (String set1: System2Mappings.keySet()){
			
			line += "\t" + set1;
			
		}
		
		writer.writeLine(line);
		System.out.print(line);
		
		
		System.out.println("");
		for (String name: System2Mappings.keySet()){
			System.out.println(name + ": " + System2Mappings.get(name).size());
		}
		
		Double[][] distance_matrix = new Double[System2Mappings.size()][System2Mappings.size()];
		
		Characteristic differences;
		
		int intersection;
		int union;
		double distance;
		
		int i;
		int j;
		
		i=0;		
		for (String set1: System2Mappings.keySet()){
			
			j=0;
			
			System.out.print("\n" + set1);
			
			line=set1;
			
			for (String set2: System2Mappings.keySet()){
				
				//get jaccard distances...
				differences = new Characteristic(
						System2Mappings.get(set1), 
						System2Mappings.get(set2));
				
				
				//System.out.println(set1 + " VS " + set2);
				
				//System.out.println(System2Mappings.get(set1).size());
				//System.out.println(System2Mappings.get(set2).size());
				
				intersection = differences.getNumOfRulesCorrect();//Mappings of set1 in set2 (intersection)
				//union = differences.getNumOfRulesGold() + differences.getNumOfRulesMatcher();
				union = System2Mappings.get(set1).getUnion(System2Mappings.get(set2)).size();
				
				//System.out.println("\n" + System2Mappings.get(set1).size());
				//System.out.println(System2Mappings.get(set2).size());
				//System.out.println(union);
				
				distance = 1 - ((double)intersection)/((double)union);

				distance_matrix[i][j] = distance; 
				
				//if (line.length()>0)
					line += "\t" + distance;
				//else {
				//	line = String.valueOf(distance);					
				//}
				
				System.out.print("\t" + distance);
												
				j++;
				
			}
			
			i++;
			
			writer.writeLine(line);
			
		}
		
		writer.closeBuffer();
		
	}
	
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		try{
			new DistanceMatrix();
		}
		catch (Exception e){
			e.printStackTrace();
		}
		
		

	}

}
