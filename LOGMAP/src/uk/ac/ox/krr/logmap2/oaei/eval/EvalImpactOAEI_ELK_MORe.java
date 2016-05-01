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
import uk.ac.ox.krr.logmap2.io.OWLAlignmentFormat;
import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;
import uk.ac.ox.krr.logmap2.oaei.reader.MappingsReaderManager;
import uk.ac.ox.krr.logmap2.utilities.Utilities;
import uk.ac.ox.krr.logmap2.reasoning.ELKAccess;
import uk.ac.ox.krr.logmap2.reasoning.MOReAccess;
import uk.ac.ox.krr.logmap2.reasoning.ReasonerAccess;
import uk.ac.ox.krr.logmap2.reasoning.ReasonerManager;

public class EvalImpactOAEI_ELK_MORe {

	
	
	
	
	private static void EvaluateCoherence2013() throws Exception{
		
		int onto_pair;
		onto_pair = Utilities.FMA2NCI; //0
		//onto_pair = Utilities.FMA2SNOMED; //1
		//onto_pair = Utilities.SNOMED2NCI; //2
		String size;
		
		size = "small";
		//size = "big";
		//size = "whole";
	
		int reasoner;
		reasoner = ReasonerManager.ELK; //2
		//reasoner = ReasonerManager.MORe; //5
		
		EvaluateCoherence2013(
				"/usr/local/data/DataUMLS/UMLS_Onto_Versions/", 
				"/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/oaei_2013/reference_alignment/",
				onto_pair, size, reasoner);
		///home/ernesto/OM_OAEI/REASONING2013 in elmo
	}
		
	/**
	 * Used to extract statistics from OAEI 2012 tool outputs
	 */
	private static void EvaluateCoherence2013(
			String path_base_onto, String path_base_mappings, int ontoPair, String sizePair, int reasoner) throws Exception{

		OntologyLoader loader1;
		OntologyLoader loader2;
		OntologyLoader loader3;
		
		//String base_path = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/";
		String base_path = path_base_onto;
		int onto_pair = ontoPair;
		String size = sizePair;
		

		String rootpath = base_path + "OAEI_datasets/oaei_2013/";
		

		String irirootpath = "file:" + rootpath;
		
		//String irirootpath = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2nci_dataset/";	
		//String mappings_path = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/OutputAlcomo/";
		
		String onto1;
		String onto2;
		String pattern;
		String extension;
		
		
				
		if (onto_pair==Utilities.FMA2NCI){
			
			if (size.equals("small")){
				onto1 = irirootpath + "oaei2013_FMA_small_overlapping_nci.owl";
				onto2 = irirootpath + "oaei2013_NCI_small_overlapping_fma.owl";
				pattern = "_fn1";
			}
			else{
				onto1 = irirootpath + "oaei2013_FMA_whole_ontology.owl";
				onto2 = irirootpath + "oaei2013_NCI_whole_ontology.owl";
				pattern = "_fn2";
			}
		}
		else if (onto_pair==Utilities.FMA2SNOMED){
			
			if (size.equals("small")){
				onto1 = irirootpath + "oaei2013_FMA_small_overlapping_snomed.owl";
				onto2 = irirootpath + "oaei2013_SNOMED_small_overlapping_fma.owl";
				pattern = "_fs1";
			}
			else{
				onto1 = irirootpath + "oaei2013_FMA_whole_ontology.owl";
				onto2 = irirootpath + "oaei2013_SNOMED_extended_overlapping_fma_nci.owl";
				pattern = "_fs2";
			}
		}
		else {
			
			if (size.equals("small")){
				onto2 = irirootpath + "oaei2013_NCI_small_overlapping_snomed.owl";
				onto1 = irirootpath + "oaei2013_SNOMED_small_overlapping_nci.owl";
				pattern = "_sn1";
			}
		
			else{
				onto2 = irirootpath + "oaei2013_NCI_whole_ontology.owl";
				onto1 = irirootpath + "oaei2013_SNOMED_extended_overlapping_fma_nci.owl";
				pattern = "_sn2";
			}
			
			
		}
		
		//String mappings_path = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/Mappings_Tools/";
		//String mappings_path = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/Mappings_Tools_2012/";			
		//String irirootpath_mappings = "file:" + mappings_path;
		//String mappings_path = base_path + "OAEI_datasets/Mappings_Tools_2012/Top7/";
		
		//String mappings_path = base_path +  "OAEI_datasets/oaei_2013/reference_alignment/repaired_alignments/";
		String mappings_path = path_base_mappings;
		
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
			//if (!filenames[i].contains("oaei2013_SNOMED2NCI_repaired_UMLS_mappings.owl")) //15  (O with extended overlapping)
			if (!filenames[i].contains("oaei2013_FMA2NCI_original_UMLS_mappings_with_confidence.rdf"))
				continue;
			
			
			System.out.println("Evaluation mappings: " + filenames[i]);
			
			
			//loader3 = new OntologyLoader(irpath_mappings + filenames[i]);
			//READ RDF mappings
			
			
			
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			
			OWLOntology onto_merged = manager.createOntology();
			manager.addAxioms(onto_merged, loader1.getOWLOntology().getAxioms());
			manager.addAxioms(onto_merged, loader2.getOWLOntology().getAxioms());
			manager.addAxioms(onto_merged, 
					getOWLOntologyFromRDFMappings(mappings_path + filenames[i]).getAxioms());
			
			
			ReasonerAccess reasonerAccess;
			
			if (reasoner == ReasonerManager.ELK){
				reasonerAccess = new ELKAccess(manager, onto_merged, false);
				System.out.println("Unsatisfiable clases using ELK: " + reasonerAccess.getUnsatisfiableClasses().size());
			}
			else{
				reasonerAccess = new MOReAccess(manager, onto_merged, false);
				System.out.println("Unsatisfiable clases using MOre: " + reasonerAccess.getUnsatisfiableClasses().size());
			}
			
			System.out.println("\n\n");				
			
			
		}//iter files
			
		
			
			
			
		
	}
	
	
	
	
	private static OWLOntology getOWLOntologyFromRDFMappings(String file) throws Exception{
		
		MappingsReaderManager managerReader = new MappingsReaderManager(file, MappingsReaderManager.OAEIFormat);
		
		OWLAlignmentFormat owlOutput =  new OWLAlignmentFormat(""); // we do not want to s ave the file
		
		for (MappingObjectStr mapping : managerReader.getMappingObjects()){
			
			owlOutput.addClassMapping2Output(
					mapping.getIRIStrEnt1(), 
					mapping.getIRIStrEnt1(), 
					mapping.getMappingDirection(), 
					mapping.getConfidence());
			
		}
		
		return owlOutput.getOWLOntology();
		
		
	}
	
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		try{
			EvaluateCoherence2013();
		}
		catch (Exception e){
			e.printStackTrace();
		}
		
	}

}
