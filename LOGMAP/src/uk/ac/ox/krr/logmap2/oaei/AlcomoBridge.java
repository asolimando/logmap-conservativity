package uk.ac.ox.krr.logmap2.oaei;

import java.io.File;
import java.util.Calendar;
import java.util.Set;

import de.unima.alcomox.ExtractionProblem;
import de.unima.alcomox.Settings;
import de.unima.alcomox.exceptions.AlcomoException;
import de.unima.alcomox.mapping.Characteristic;
import de.unima.alcomox.mapping.Correspondence;
import de.unima.alcomox.mapping.Mapping;
import de.unima.alcomox.mapping.SemanticRelation;
import de.unima.alcomox.ontology.IOntology;

import uk.ac.ox.krr.logmap2.io.OWLAlignmentFormat;
import uk.ac.ox.krr.logmap2.reasoning.ReasonerManager;
import uk.ac.ox.krr.logmap2.reasoning.SatisfiabilityIntegration;
import uk.ac.ox.krr.logmap2.utilities.Utilities;


import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;


/**
 * Only used for repair test. Alcomo is NOT currently integrated within LogMap
 * @author Ernesto
 *
 */
public class AlcomoBridge {
	
	
	public static void main(String[] args) {
		try{			
			//Only for statistics
			if (args.length==5)
				StatisticsOAEI2012(
						args[0], 
						Integer.valueOf(args[1]), 
						args[2],
						Integer.valueOf(args[3]),
						Boolean.valueOf(args[4]));
			else
				StatisticsOAEI2012();
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}
	
	
	
	private static void checkSatisfiabilityMappings(Set<OWLAxiom> onto1, Set<OWLAxiom> onto2, Mapping mappings) throws Exception {
		
		OWLAlignmentFormat owlformat = new OWLAlignmentFormat("");
		
		
		int rel = Utilities.EQ;
		
		int mixed_entities=0;
		
		for (Correspondence map : mappings.getCorrespondencesAsSet()){
			
			//default
			rel = Utilities.EQ;
			
			if (map.getRelation().getType()==SemanticRelation.EQUIV){
				rel = Utilities.EQ;
			}
			else if (map.getRelation().getType()==SemanticRelation.SUB){
				rel = Utilities.L2R;
			}
			else if (map.getRelation().getType()==SemanticRelation.SUPER){
				rel = Utilities.R2L;
				//System.out.println("Semantic relationship: " +  map.getRelation().getType() + "   " + map.getRelation());
			}
			else{
				System.out.println("Unknown semantic relationship: " +  map.getRelation().getType() + "   " + map.getRelation()); 
			}
			
			
			
			/*System.out.println(map);
			System.out.println(map.getSourceEntityUri());
			System.out.println(map.getTargetEntityUri());
			
			System.out.println(map.getTargetEntity());
			System.out.println(map.getSourceEntity());
			
			System.out.println(map.getTargetEntity().toString());
			System.out.println(map.getSourceEntity().toString());
			*/
			
			if (map.getSourceEntity()==null || map.getTargetEntity()==null){
				owlformat.addClassMapping2Output(
						map.getSourceEntityUri(),
						map.getTargetEntityUri(),
						rel,
						map.getConfidence()
						);
			}
			
			else if (map.getSourceEntity().isConcept() && map.getTargetEntity().isConcept()){
											
				owlformat.addClassMapping2Output(
						map.getSourceEntityUri(),
						map.getTargetEntityUri(),
						rel,
						map.getConfidence()
						);				
			}
			else if (map.getSourceEntity().isDataProperty() && map.getTargetEntity().isDataProperty()){
				
				owlformat.addDataPropMapping2Output(
						map.getSourceEntityUri(),
						map.getTargetEntityUri(),
						rel,
						map.getConfidence()
						);				
			}
			else if (map.getSourceEntity().isObjectProperty() && map.getTargetEntity().isObjectProperty()){
				
				owlformat.addDataPropMapping2Output(
						map.getSourceEntityUri(),
						map.getTargetEntityUri(),
						rel,
						map.getConfidence()
						);				
			}
			else{
				System.out.println("Mixing entities: \n\t" + map.getSourceEntityUri() + "\n\t"	+ map.getTargetEntityUri());
				mixed_entities++;
				/*
				 owlformat.addClassMapping2Output(
						map.getSourceEntityUri(),
						map.getTargetEntityUri(),
						rel,
						map.getConfidence()
						);
				*/
			}
			
			
		}
		
		System.out.println("\tNum mixed mappings: " + mixed_entities);
		
		SatisfiabilityIntegration.setTimeoutClassSatisfiabilityCheck(60);
		SatisfiabilityIntegration sat_checker = new SatisfiabilityIntegration(
				onto1, 
				onto2,
				owlformat.getOWLOntology().getAxioms(),
				true,//class sat
				true,//Time_Out_Class
				false); //use factory
		
		
	}
	
	
	
	private static void StatisticsOAEI2012() throws Exception{
		
		int onto_pair;
		onto_pair = Utilities.FMA2NCI;
		//onto_pair = Utilities.FMA2SNOMED;
		//onto_pair = Utilities.SNOMED2NCI;
		String size;
		//size = "small";
		//size = "big";
		size = "whole";
		int reasoner;
		reasoner = ReasonerManager.HERMIT;
		boolean split=true;
		
		
		
		StatisticsOAEI2012(
				"/usr/local/data/DataUMLS/UMLS_Onto_Versions/", 
				onto_pair, size, reasoner, split);
		
	}
	
	/**
	 * Used to extract statistics from OAEI 2012 tool outputs
	 */
	private static void StatisticsOAEI2012(
			String path_base, 
			int ontoPair,
			String sizePair,
			int reasoner,
			boolean split) throws Exception{

		long init, fin;
		

		
		//String base_path = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/";
		String base_path = path_base;
		int onto_pair = ontoPair;
		String size = sizePair;
		
		
		
		//FMA2NCI
		String rootpath;
		String rootpath_fma2nci = base_path + "OAEI_datasets/oaei_2012/fma2nci/";
		String rootpath_fma2snomed = base_path + "OAEI_datasets/oaei_2012/fma2snmd/";
		String rootpath_snomed2nci = base_path + "OAEI_datasets/oaei_2012/snmd2nci/";

		
		String irirootpath;
		String irirootpath_fma2nci = "file:" + rootpath_fma2nci;
		String irirootpath_fma2snomed = "file:" + rootpath_fma2snomed;
		String irirootpath_snomed2nci = "file:" +  rootpath_snomed2nci;
		
		
		
		//String path_gs;
		
		//String irirootpath = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2nci_dataset/";	
		//String mappings_path = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/OutputAlcomo/";
		
		String onto1;
		String onto2;
		String pattern;
		String extension;
		
		String refAlignment;
		
		
				
		extension = "rdf";		
		
		
		if (onto_pair==Utilities.FMA2NCI){
			
			irirootpath = irirootpath_fma2nci;
			rootpath = rootpath_fma2nci;
			
			refAlignment = rootpath + "oaei2012_FMA2NCI_original_UMLS_mappings.rdf";
			
			if (size.equals("small")){
				onto1 = irirootpath + "oaei2012_FMA_small_overlapping_nci.owl";
				onto2 = irirootpath + "oaei2012_NCI_small_overlapping_fma.owl";
				pattern = "_small_fma2nci." + extension;
			}
			else if (size.equals("big")){
				onto1 = irirootpath + "oaei2012_FMA_extended_overlapping_nci.owl";
				onto2 = irirootpath + "oaei2012_NCI_extended_overlapping_fma.owl";
				pattern = "_big_fma2nci." + extension;
			}
			else{
				onto1 = irirootpath + "oaei2012_FMA_whole_ontology.owl";
				onto2 = irirootpath + "oaei2012_NCI_whole_ontology.owl";
				pattern = "_whole_fma2nci." + extension;
			}
		}
		else if (onto_pair==Utilities.FMA2SNOMED){
			
			irirootpath = irirootpath_fma2snomed;
			rootpath = rootpath_fma2snomed;
			
			refAlignment = rootpath + "oaei2012_FMA2SNMD_original_UMLS_mappings.rdf";
			
			if (size.equals("small")){
				onto1 = irirootpath + "oaei2012_FMA_small_overlapping_snomed.owl";
				onto2 = irirootpath + "oaei2012_SNOMED_small_overlapping_fma.owl";
				pattern = "_small_fma2snomed." + extension;
			}
			else if (size.equals("big")){
				onto1 = irirootpath + "oaei2012_FMA_extended_overlapping_snomed.owl";
				onto2 = irirootpath + "oaei2012_SNOMED_extended_overlapping_fma_nci.owl";
				pattern = "_big_fma2snomed." + extension;
			}
			else{
				onto1 = irirootpath_fma2nci + "oaei2012_FMA_whole_ontology.owl";
				onto2 = irirootpath + "oaei2012_SNOMED_whole_ontology.owl.zip";
				pattern = "_whole2_fma2snomed." + extension;
			}
		}
		else {
			
					
			irirootpath = irirootpath_snomed2nci;
			rootpath = rootpath_snomed2nci;
			
			refAlignment = rootpath + "oaei2012_SNMD2NCI_original_UMLS_mappings.rdf";
			
			if (size.equals("small")){
				onto2 = irirootpath + "oaei2012_NCI_small_overlapping_snomed.owl";
				onto1 = irirootpath + "oaei2012_SNOMED_small_overlapping_nci.owl";
				pattern = "_small_snomed2nci." + extension;
			}
			else if (size.equals("big")){
				onto2 = irirootpath + "oaei2012_NCI_extended_overlapping_snomed.owl";
				onto1 = irirootpath_fma2snomed + "oaei2012_SNOMED_extended_overlapping_fma_nci.owl";
				pattern = "_big_snomed2nci." + extension;
			}
			else{
				onto2 = irirootpath_fma2nci + "oaei2012_NCI_whole_ontology.owl";
				onto1 = irirootpath_fma2snomed + "oaei2012_SNOMED_whole_ontology.owl.zip";
				pattern = "_whole2_snomed2nci." + extension;
			}
			
			
		}
		
		//String mappings_path = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/Mappings_Tools_2012/";			
		//String mappings_path = base_path + "OAEI_datasets/Mappings_Tools_2012/";
		String mappings_path = base_path + "OAEI_datasets/oaei_2013/";
		//TODO Remove
		//pattern = "FMA2NCI";
		
		
		
				
		File directory = new File(mappings_path);
		String filenames[] = directory.list();
		
		
		
		//ALCOMO SETTING
		if (reasoner == ReasonerManager.HERMIT) //hermit 0, Pellet 1
			Settings.BLACKBOX_REASONER = Settings.BlackBoxReasoner.HERMIT;
		else
			Settings.BLACKBOX_REASONER = Settings.BlackBoxReasoner.PELLET;

		
		// if you want to force to generate a one-to-one alignment add this line
		// by default its set to false
		Settings.ONE_TO_ONE = false;
		
		// load ontologies as IOntology (uses fast indexing for efficient reasoning)
		// formerly LocalOntology now IOntology is recommended
		
		init = Calendar.getInstance().getTimeInMillis();
		IOntology sourceOnt = new IOntology(onto1);
		IOntology targetOnt = new IOntology(onto2);
		fin = Calendar.getInstance().getTimeInMillis();
		System.out.println("Time Loading ontologies with ALCOMO (s): " + (float)((double)fin-(double)init)/1000.0);
		
		
		// compare against reference alignment
		Mapping ref = new Mapping(refAlignment);
			
		
		
		
		
		
		String fileNameNoExtension;
		
				
		//Iterate over mappings
		for(int i=0; i<filenames.length; i++){
			
			//try {
			
			init = Calendar.getInstance().getTimeInMillis();
			
			//if (!filenames[i].contains(task) || filenames[i].contains("mapevo") || filenames[i].contains("mappso"))
			//if (!filenames[i].contains("mappso_small.txt"))
			//	continue;
			//if (!filenames[i].equals("oaei2012_FMA2NCI_voted_mappings.txt"))
			//if (!filenames[i].equals("oaei2012_SNMD2NCI_original_UMLS_mappings.rdf"))
			
			if (!filenames[i].contains(pattern))
				continue;
			
			fileNameNoExtension = filenames[i].split("\\.")[0];
			
			System.out.println("Evaluation mappings with ALCOMO: " + filenames[i]);
			
			// load the mapping
			Mapping mapping = new Mapping(mappings_path + filenames[i]);
			//mapping.applyThreshhold(0.3);
			//System.out.println("thresholded input mapping has " + mapping.size() + " correspondences");
			
			//Should be here and no after the split
			Characteristic cBefore = new Characteristic(mapping, ref);
			System.out.println("\tOriginal mappings: " + mapping.size() + " correspondences");
			
			
			if (split)
				mapping.splitToSubsumptionCorrespondences();

			
			// define diagnostic problem
			ExtractionProblem ep = new ExtractionProblem(
					ExtractionProblem.ENTITIES_CONCEPTSPROPERTIES,
					//ExtractionProblem.METHOD_OPTIMAL,
					//ExtractionProblem.METHOD_GREEDY, //METHOD_OPTIMAL
					ExtractionProblem.METHOD_GREEDY_MINIMIZE, //new tests 2013
					ExtractionProblem.REASONING_EFFICIENT
			);
			
			// attach ontologies and mapping to the problem
			ep.bindSourceOntology(sourceOnt);
			ep.bindTargetOntology(targetOnt);
			ep.bindMapping(mapping);
			
			// solve the problem
			
			ep.solve();
		
			//System.out.println("UNSATISFIABILITIES found by Alcomo: " + ep.getMergedUnsatisfiableEntities().size());
			
			Mapping extracted = ep.getExtractedMapping();
			
			
			fin = Calendar.getInstance().getTimeInMillis();
			System.out.println("Time Repairing mappings with ALCOMO (s): " + (float)((double)fin-(double)init)/1000.0);

			
			
			
			
			
			System.out.println("\nPrecision and Recall clean mappings:");			
			
			if (split){
				System.out.println("\n\tsub mappings reduced from " + mapping.size() + " to " + extracted.size() + " correspondences");
				System.out.println("\tremoved the following sub correspondences:" + ep.getDiscardedMapping().size());
				
				extracted.joinToEquivalence();
				
				//TODO Do not do it. there are some dependencies with extracted mappings
				//mapping.joinToEquivalence();
				//ep.getDiscardedMapping().joinToEquivalence();
				System.out.println("\tFinal joined equivalence mappings: " + extracted.size() + " correspondences");
			}
			else{
				System.out.println("\n\tmapping reduced from " + mapping.size() + " to " + extracted.size() + " correspondences");
				System.out.println("\tremoved the following correspondences:" + ep.getDiscardedMapping().size());
			}
			
			
			
			
			
			Characteristic cAfter = new Characteristic(extracted, ref);
			
			System.out.println("");
			System.out.println("\tbefore debugging (pre, rec, f): " + cBefore.toShortDesc());
			System.out.println("\tafter debugging (pre, rec, f):  " + cAfter.toShortDesc());
			
			System.out.println("\tIncrease/decrease:  " + 
					//Utilities.getRoundValue((cAfter.getPrecision()-cBefore.getPrecision()),3) + "  " +
					//Utilities.getRoundValue((cAfter.getRecall()-cBefore.getRecall()),3) + "  " +
					//Utilities.getRoundValue((cAfter.getFMeasure()-cBefore.getFMeasure()),3));
					(cAfter.getPrecision()-cBefore.getPrecision()) + "  " +
					(cAfter.getRecall()-cBefore.getRecall()) + "  " +
					(cAfter.getFMeasure()-cBefore.getFMeasure()));
			
			
			System.out.println("\nUNSAT clean mappings with Alcomo: ");
			if (onto_pair!=Utilities.SNOMED2NCI){ //do not reason with SNOMED and NCI
				//checkSatisfiabilityMappings(sourceOnt.getAxioms(), targetOnt.getAxioms(), extracted);
			}
			else {
				//Check with LogMap
				//TODO create basic method in Repair facility to check unsat!
			}
			
			
			if (reasoner == ReasonerManager.HERMIT) 
				extracted.write(mappings_path + fileNameNoExtension + "_repaired_with_Alcomo_Hermit.rdf", Mapping.FORMAT_RDF);
			else
				extracted.write(mappings_path + fileNameNoExtension + "_repaired_with_Alcomo_Pellet.rdf", Mapping.FORMAT_RDF);
			
			

			
			
			//System.out.println(sourceOnt.getAxioms().size());
			//System.out.println(targetOnt.getAxioms().size());
			
			//Satisfiability check!
			//System.out.println("\nUNSAT original mappings: ");
			//checkSatisfiabilityMappings(sourceOnt.getAxioms(), targetOnt.getAxioms(), mapping);
						
			
			
			
			System.out.println("\n\n");				
			
			
			
			
		}//iter files
		
		
		
	}
	

}
