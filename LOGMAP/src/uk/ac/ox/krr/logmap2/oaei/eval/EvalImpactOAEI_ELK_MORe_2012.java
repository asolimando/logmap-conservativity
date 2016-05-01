package uk.ac.ox.krr.logmap2.oaei.eval;

import java.io.File;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntology;

import uk.ac.ox.krr.logmap2.LogMap2_RepairFacility;
import uk.ac.ox.krr.logmap2.OntologyLoader;
import uk.ac.ox.krr.logmap2.io.LogOutput;
import uk.ac.ox.krr.logmap2.oaei.reader.MappingsReaderManager;
import uk.ac.ox.krr.logmap2.utilities.Utilities;
import uk.ac.ox.krr.logmap2.reasoning.ELKAccess;

public class EvalImpactOAEI_ELK_MORe_2012 {

	
	
	
	
	private static void StatisticsOAEI2012() throws Exception{
		
		int onto_pair;
		//onto_pair = Utilities.FMA2NCI;
		//onto_pair = Utilities.FMA2SNOMED;
		onto_pair = Utilities.SNOMED2NCI;
		String size;
		//size = "small";
		//size = "big";
		size = "whole";
	
		
		StatisticsOAEI2012("/usr/local/data/DataUMLS/UMLS_Onto_Versions/", onto_pair, size);
		
	}
		
	/**
	 * Used to extract statistics from OAEI 2012 tool outputs
	 */
	private static void StatisticsOAEI2012(String path_base, int ontoPair, String sizePair) throws Exception{

		OntologyLoader loader1;
		OntologyLoader loader2;
		OntologyLoader loader3;
		
		//String base_path = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/";
		String base_path = path_base;
		int onto_pair = ontoPair;
		String size = sizePair;
		

		String rootpath;
		String rootpath_fma2nci = base_path + "OAEI_datasets/oaei_2012/fma2nci/";
		String rootpath_fma2snomed = base_path + "OAEI_datasets/oaei_2012/fma2snmd/";
		String rootpath_snomed2nci = base_path + "OAEI_datasets/oaei_2012/snmd2nci/";
		
		String irirootpath;
		String irirootpath_fma2nci = "file:" + rootpath_fma2nci;
		String irirootpath_fma2snomed = "file:" + rootpath_fma2snomed;
		String irirootpath_snomed2nci = "file:" +  rootpath_snomed2nci;
		
		//String irirootpath = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2nci_dataset/";	
		//String mappings_path = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/OutputAlcomo/";
		
		String onto1;
		String onto2;
		String pattern;
		String extension;
		
		
				
		if (onto_pair==Utilities.FMA2NCI){
			
			irirootpath = irirootpath_fma2nci;
			if (size.equals("small")){
				onto1 = irirootpath + "oaei2012_FMA_small_overlapping_nci.owl";
				onto2 = irirootpath + "oaei2012_NCI_small_overlapping_fma.owl";
				pattern = "_small_fma2nci";
			}
			else if (size.equals("big")){
				onto1 = irirootpath + "oaei2012_FMA_extended_overlapping_nci.owl";
				onto2 = irirootpath + "oaei2012_NCI_extended_overlapping_fma.owl";
				pattern = "_big_fma2nci2";
			}
			else{
				onto1 = irirootpath + "oaei2012_FMA_whole_ontology.owl";
				onto2 = irirootpath + "oaei2012_NCI_whole_ontology.owl";
				pattern = "_whole_fma2nci";
			}
		}
		else if (onto_pair==Utilities.FMA2SNOMED){
			
			
			irirootpath = irirootpath_fma2snomed;
			
			if (size.equals("small")){
				onto1 = irirootpath + "oaei2012_FMA_small_overlapping_snomed.owl";
				onto2 = irirootpath + "oaei2012_SNOMED_small_overlapping_fma.owl";
				pattern = "_small_fma2snomed";
			}
			else if (size.equals("big")){
				onto1 = irirootpath + "oaei2012_FMA_extended_overlapping_snomed.owl";
				onto2 = irirootpath + "oaei2012_SNOMED_extended_overlapping_fma_nci.owl";
				pattern = "_big_fma2snomed";
			}
			else{
				onto1 = irirootpath_fma2nci + "oaei2012_FMA_whole_ontology.owl";
				onto2 = irirootpath + "oaei2012_SNOMED_whole_ontology.owl.zip";
				pattern = "_whole2_fma2snomed";
			}
		}
		else {
			
			irirootpath = irirootpath_snomed2nci;
			
			if (size.equals("small")){
				onto2 = irirootpath + "oaei2012_NCI_small_overlapping_snomed.owl";
				onto1 = irirootpath + "oaei2012_SNOMED_small_overlapping_nci.owl";
				pattern = "_small_snomed2nci";
			}
			else if (size.equals("big")){
				onto2 = irirootpath + "oaei2012_NCI_extended_overlapping_snomed.owl";
				onto1 = irirootpath_fma2snomed + "oaei2012_SNOMED_extended_overlapping_fma_nci.owl";
				pattern = "_big_snomed2nci";
			}
			else{
				onto2 = irirootpath_fma2nci + "oaei2012_NCI_whole_ontology.owl";
				onto1 = irirootpath_fma2snomed + "oaei2012_SNOMED_whole_ontology.owl.zip";
				//onto1 = irirootpath_fma2snomed + "oaei2012_SNOMED_extended_overlapping_fma_nci.owl"; //TODO
				pattern = "_whole2_snomed2nci";
			}
			
			
		}
		
		//String mappings_path = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/Mappings_Tools/";
		//String mappings_path = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/Mappings_Tools_2012/";			
		//String irirootpath_mappings = "file:" + mappings_path;
		//String mappings_path = base_path + "OAEI_datasets/Mappings_Tools_2012/Top7/";
		
		String mappings_path = base_path +  "OAEI_datasets/oaei_2013/reference_alignment/repaired_alignments/";
		
		String irpath_mappings = "file:" + mappings_path;
		
		
		
		//String irirootpath_mappings = "file:" + mappings_path; 
		
		File directory = new File(mappings_path);
		String filenames[] = directory.list();
		
				
		LogOutput.printAlways("Loading ontologies...");
		loader1 = new OntologyLoader(onto1);
		loader2 = new OntologyLoader(onto2);
		LogOutput.printAlways("...Done");
		
		Logger.getLogger("org.semanticweb.elk").setLevel(Level.OFF);//removed completely
				
		//Iterate over mappings
		for(int i=0; i<filenames.length; i++){
			
			//if (!filenames[i].contains(task) || filenames[i].contains("mapevo") || filenames[i].contains("mappso"))
			//if (!filenames[i].contains("mappso_small.txt"))
			//	continue;
			//if (!filenames[i].equals("oaei2012_FMA2NCI_voted_mappings.txt"))
			//if (!filenames[i].equals("hertuda_small_fma2nci_repaired_with_Alcomo_Hermit.rdf"))
			//if (!filenames[i].equals("gommaBK_small_fma2snomed.txt"))
			//if (!filenames[i].equals("servomap_big_fma2snomed.txt"))
			//if (!filenames[i].contains("wmatch_small_fma2nci.rdf"))
			//if (!filenames[i].contains("whole2_snomed2nci_repaired_with_Alcomo_Hermit.rdf"))
			//if (!filenames[i].equals("logmap2_small_fma2snomed.rdf"))				
			//if (!filenames[i].contains(pattern))
			//if (!filenames[i].contains("servomap_whole2_snomed2nci_repaired_with_Alcomo_Hermit.owl"))
			//if (!filenames[i].contains("oaei2013_SNOMED2NCI_repaired_UMLS_mappings.owl")) //>
			//if (!filenames[i].contains("onto_mappings_SNOMED_NCI_dirty_confidence_whole2_snomed2nci_repaired_with_LogMap.owl")) //2
			//if (!filenames[i].contains("onto_mappings_SNOMED_NCI_dirty_confidence_whole2_snomed2nci_repaired_with_LogMap_iterative.owl")) //0	
			if (!filenames[i].contains("oaei2013_SNOMED2NCI_repaired_UMLS_mappings.owl")) //15  (O with extended overlapping)	
				continue;
			
			
			System.out.println("Evaluation mappings: " + filenames[i]);
			
			
			
			loader3 = new OntologyLoader(irpath_mappings + filenames[i]);
			
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			
			OWLOntology onto_merged = manager.createOntology();
			manager.addAxioms(onto_merged, loader1.getOWLOntology().getAxioms());
			manager.addAxioms(onto_merged, loader2.getOWLOntology().getAxioms());
			manager.addAxioms(onto_merged, loader3.getOWLOntology().getAxioms());
			
			ELKAccess elk = new ELKAccess(manager, onto_merged, false);
			
			System.out.println("Unsatisfiable clases using ELK: " + elk.getUnsatisfiableClasses().size());
			System.out.println("\n\n");				
			
			
		}//iter files
			
		
			
			
			
		
	}
	
	
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		try{
			StatisticsOAEI2012();
		}
		catch (Exception e){
			e.printStackTrace();
		}
		
	}

}
