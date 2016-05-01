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
package uk.ac.ox.krr.logmap2;


import java.io.*;

import java.util.Calendar;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.Map;
import java.util.TreeSet;

import java.util.HashSet;

import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLDataFactory;


import uk.ac.manchester.syntactic_locality.ModuleExtractor;
import uk.ac.ox.krr.logmap2.io.*;
//import uk.ac.ox.krr.logmap2.reasoning.deprecated.SatisfiabilityIntegration;
import uk.ac.ox.krr.logmap2.reasoning.SatisfiabilityIntegration;
import uk.ac.ox.krr.logmap2.repair.*;
import uk.ac.ox.krr.logmap2.mappings.objects.*;
import uk.ac.ox.krr.logmap2.oaei.Oraculo;
import uk.ac.ox.krr.logmap2.overlapping.*;
import uk.ac.ox.krr.logmap2.owlapi.SynchronizedOWLManager;
import uk.ac.ox.krr.logmap2.utilities.Lib;
import uk.ac.ox.krr.logmap2.utilities.PrecomputeIndexCombination;

import uk.ac.ox.krr.logmap2.lexicon.LexicalUtilities;
import uk.ac.ox.krr.logmap2.utilities.Utilities;

import uk.ac.ox.krr.logmap2.indexing.*;
import uk.ac.ox.krr.logmap2.indexing.entities.ClassIndex;
import uk.ac.ox.krr.logmap2.interactive.*;

import uk.ac.ox.krr.logmap2.interactive.objects.MappingObjectInteractivity;
import uk.ac.ox.krr.logmap2.mappings.MappingManager;
import uk.ac.ox.krr.logmap2.mappings.CandidateMappingManager;

import uk.ac.ox.krr.logmap2.io.LogOutput;

import uk.ac.ox.krr.logmap2.statistics.*;




/**
 * This class implements LogMap 2 algorithm calling it main functionalities
 * 
 * @author ernesto
 *
 */
public class LogMap2Core {
	
	private OverlappingExtractor overlappingExtractor;
	
	private IndexManager index;
	
	private OntologyProcessing onto_process1;
	private OntologyProcessing onto_process2;
	
	//private AnchorExtraction mapping_extractor;
	private MappingManager mapping_extractor;
	
	private AnchorAssessment mappings_assessment;
	
	private InteractiveProcess interactiveProcessManager;
	
	//For precission and recall
	private Set<MappingObjectStr> mappings_gs = new HashSet<MappingObjectStr>();
	
	private LexicalUtilities lexicalUtilities = new LexicalUtilities();
	
	private OWLDataFactory dataFactory;

	private String prefix4IRIs;
	private String logmap_mappings_path="";
	private String gs_mappings="";
	
	long init_global, init, fin;
	
	

	boolean cleanD_G=true;

	
	boolean useInteractivity=true;
	boolean useHeuristics=true;
	boolean orderQuestions=true;
	int error_user = 0; //%error for user
	boolean record_interactivity=false;
	
	boolean ask_everything=false;
	
	boolean evaluate_impact=false;
	
	//boolean overlapping=true;
	//String interactivityFile;
	
	

	
	/**
	 * OAEI constructor
	 * @param iri1_str
	 * @param iri2_str
	 * @throws Exception
	 */
	public LogMap2Core(
			String iri1_str, 
			String iri2_str) throws Exception{
		
		this(iri1_str, iri2_str, "", "", "", "", false, false, false, 0, false, false, false);
		
	}
	
	
	/**
	 * Constructor for tests: conference task with gold standard
	 * @param iri1_str
	 * @param iri2_str
	 * @param evaluate_impact
	 * @param file_gs Gold standard file
	 * @throws Exception
	 */
	public LogMap2Core(
			String iri1_str, 
			String iri2_str,
			boolean evaluate_impact,
			String file_gs) throws Exception{
		
		this(iri1_str, iri2_str, "", "", file_gs, "", false, false, false, 0, false, false, evaluate_impact);
		
	}
	
	
	/**
	 * Constructor for tests: conference/multifarm task
	 * @param iri1_str
	 * @param iri2_str
	 * @param evaluate_impact
	 */
	public LogMap2Core(
			String iri1_str, 
			String iri2_str,
			boolean evaluate_impact) throws Exception {
		
		this(iri1_str, iri2_str, "", "", "", "", false, false, false, 0, false, false, evaluate_impact); //eval impact
		
	}
	
	
	/**
	 * Constructor for tests: instance matching task
	 * @param iri1_str
	 * @param iri2_str
	 * @param output_path
	 * @param gs_file
	 * @param evaluate_impact
	 * @throws Exception
	 */
	public LogMap2Core(
			String iri1_str, 
			String iri2_str, 
			String output_path,
			String gs_file,
			boolean evaluate_impact) throws Exception {
		
		this(iri1_str, iri2_str, "", "", gs_file, output_path, false, false, false, 0, false, false, evaluate_impact); //eval impact
		
	}
	

	
	
	/**
	 * basic constructor: currently only from LogMap2_launch
	 * @param iri1_str
	 * @param iri2_str
	 * @param output_path
	 * @param eval_impact
	 * @throws Exception
	 */
	public LogMap2Core(
			String iri1_str, 
			String iri2_str, 
			String output_path,
			boolean eval_impact) throws Exception{
		
		this(iri1_str, iri2_str, "", "", "", output_path, false, false, false, 0, false, false, eval_impact);
		
	}
	
	
	
	
	/**
	 * Constructor from a java application
	 */
	public LogMap2Core(
			OWLOntology onto1,
			OWLOntology onto2) throws Exception{
		
		
		//we keep true in order to not to delete modules overlappings
		//See IndexLexiconAndStructure
		//Furthremore we may want to evaluate impact at some point
		//Although in this constructor it does NIT evaluate impact
		evaluate_impact=true;
		
		//dataFactory = OWLManager.getOWLDataFactory();
		dataFactory = SynchronizedOWLManager.createOWLDataFactory();
		
		init_global = init = Calendar.getInstance().getTimeInMillis();
		
		//INIT LOGMAP: lex and precomp integer combinations
		InitLogMap();
		
		
		//Overlapping estimation
		OverlappingEstimation(onto1, onto2);
		

		//Indexes lexicon (IF creation) and structure
		IndexLexiconAndStructure();
				
		fin = Calendar.getInstance().getTimeInMillis();
		LogOutput.printAlways("Time Parsing and Index Lexicon (s): " + (float)((double)fin-(double)init)/1000.0);
							
		
		//EXTRACT, CLEAN ANCHORS and INDEX INTLABELLING
		createAndCleanAnchors();
		
		
		//Extract new candidates (Level 1), clean them and index labelling
		createCandidateMappings();
		
		
		//Extract more candidates Level 2 (If interactivity is active then those are the mappings to ask)
		createCandidateMappingsLevel2();
		
		//Last Cleaning
		//lastLogicalCleaning();
		
		
		createAndAssessPropertyMappings();
		
		
		//Optional (see parameters file)
		if (Parameters.perform_instance_matching){
			createAndAssessInstanceMappings();
		}
		
		
		fin = Calendar.getInstance().getTimeInMillis();
		System.out.println("TOTAL MATCHING TIME (s): " + (float)((double)fin-(double)init_global)/1000.0);
		
		
		//Clean index structures and others...
		//Not here because OAEI and call fro other application
		//index.clearTaxonomicalStructures();		
		
		
	}
	
	
	/**
	 * Constructor for test (implementation process)
	 * @param iri1_str
	 * @param iri2_str
	 * @param iri1_str_out
	 * @param iri2_str_out
	 * @param gs_mappings
	 * @param logmap_mappings_path
	 * @param useInteractivity
	 * @param useHeuristics
	 * @param orderQuestions
	 * @throws Exception
	 */
	public LogMap2Core(
			String iri1_str, 
			String iri2_str, 
			String iri1_str_out, 
			String iri2_str_out, 
			String gs_mappings, 
			String logmap_mappings_path,
			boolean useInteractivity,
			boolean useHeuristics, //should be an input parameter
			boolean orderQuestions,
			int error_user,
			boolean ask_everything,
			boolean record_interactivity,
			boolean evaluate_impact) throws Exception{
		
		
		this.logmap_mappings_path = logmap_mappings_path;
		this.gs_mappings = gs_mappings;
		//this.interactivityFile=interactivityFile;
		
		this.useInteractivity=useInteractivity;
		this.useHeuristics=useHeuristics;
		this.orderQuestions=orderQuestions;
		
		this.error_user = error_user;
		
		this.record_interactivity=record_interactivity;
		
		this.ask_everything=ask_everything;
		
		
		this.evaluate_impact=evaluate_impact;
		
		
		//dataFactory = OWLManager.getOWLDataFactory();
		dataFactory = SynchronizedOWLManager.createOWLDataFactory();
		
		if (logmap_mappings_path.startsWith("/"))
			prefix4IRIs = "file:";
		else
			prefix4IRIs = "file:/";
		
		
		
		init_global = init = Calendar.getInstance().getTimeInMillis();
		
		//INIT LOGMAP: lex and precomp integer combinations
		InitLogMap();
		
		
		//Overlapping estimation
		OverlappingEstimation(iri1_str, iri2_str);
		//OverlappingEstimation(iri1_str, iri2_str, iri1_str_out, iri2_str_out);//test
		//if (true)
		//	return;
		
		
		
		//Indexes lexicon (IF creation) and structure
		IndexLexiconAndStructure();
		
		
		fin = Calendar.getInstance().getTimeInMillis();
		LogOutput.printAlways("Time Parsing and Index Lexicon (s): " + (float)((double)fin-(double)init)/1000.0);
		
		
		init = Calendar.getInstance().getTimeInMillis();
		
		
		
		//Only for statistical purposes (and interactivity)
		if (!gs_mappings.equals("")){
			loadMappingsGS();
		}
		//printStatisticsGoldStandard();
		
		
		//EXTRACT, CLEAN ANCHORS and INDEX INTLABELLING
		createAndCleanAnchors();
		
		
		//Extract new candidates (Level 1), clean them and index labelling
		createCandidateMappings();
		//mapping_extractor.printStatisticsMappingEvaluation();
		
		
		//Anchors should be non ambiguous for interval labelling index 
		//LogOutput.print("AMBIGUOUS ANCHORS: " + areAnchorsdAmbiguous());
		
		
		
		StatisticsManager.setPrecisionAndRecallAnchors(mapping_extractor.getStringGoldStandardAnchors().size());				
		//Statistics before interactivity
		StatisticsManager.printStatisticsLogMap_mappings();
		
		
		
		//Extract more candidates Level 2 (If interactivity is active then those are the mappings to ask)
		//Also weakened in previous iterations
		createCandidateMappingsLevel2();
		
		
		//mapping_extractor.printAllDicoveredMappingsStatistics();				
		//mapping_extractor.printHarDiscardedStatistics();	
		//mapping_extractor.printStatisticsMappingEvaluation();//|Everything
		
		
		StatisticsManager.printMappingsAskedHeur();
		
		
		//mapping_extractor.printStatisticsLogMap_mappings();
		
		//Tests
		//outputMappings(mapping_extractor.getAnchors(), logmap_mappings_path + "all_extracted.txt");
		//outputMappings(mapping_extractor.getDircardedAnchors(), logmap_mappings_path + "discarded.txt");
		//orderedOutputMappings(true);   //For LUCADA
		
		
		
		//Last cleaning just in case
		//lastLogicalCleaning();
		
		
		
		createAndAssessPropertyMappings();
		
		
		//Optional (see parameters file)
		if (Parameters.perform_instance_matching){
			createAndAssessInstanceMappings();
			
			if (Parameters.output_instance_mapping_files){
				
				outputInstaceMappings4Evaluation();
				
			}
			
		}
		
		
				
		//TODO Delete OntologyProcessing if possible
			
	
		//OUTPUT
		//P&R wrt GS
		if (!gs_mappings.equals("")){
			getPrecisionAndRecallMappings();
		}
		
		
		File file_folder = new File(logmap_mappings_path);
		
		
		if (!logmap_mappings_path.equals("") && file_folder.exists() && file_folder.isAbsolute()){ //Some experiments do not provide path...
			
			//if (!file_folder.isAbsolute()){
			//	logmap_mappings_path = file_folder.getAbsolutePath();
			//}
			
			//FINAL OVERLAPPING
			//Extracts new overlapping for anchors and saves overlapping as OWL files
			LogOutput.print("Creating overlapping output");
			createOutput4Overlapping(true); //TODO UNCOMMENT (just removed for tests)
			
			//Mapping OUTPUTFILES
			LogOutput.print("Saving output mapping files");
			saveExtractedMappings("logmap2_mappings");
		}
		else{
			//System.err.println("The given output path is not absolute or it does not exist. The output mappings cannot be stored.");
			LogOutput.print("The given output path is not absolute or it does not exist. The output mappings cannot be stored.");
		}
		
		
		
		LogOutput.print("Average time taxonomic queries: " + 
				index.getAvgTime4TaxCalls() + ". Total: " + index.getTime4TaxCalls()  + ". Num calls: " + index.getNumberOfTaxCalls());
		
		LogOutput.print("Average time disjointness queries: " + 
				index.getAvgTime4DisjCalls() + ". Total: " + index.getTime4DisjCalls()  + ". Num calls: " + index.getNumberOfDisjCalls());
		
		
		
		fin = Calendar.getInstance().getTimeInMillis();
		LogOutput.printAlways("TOTAL MATCHING TIME (s): " + (float)((double)fin-(double)init_global)/1000.0);
		
		
		if (evaluate_impact){
			init = Calendar.getInstance().getTimeInMillis();
			//impactIntegration();
			impactIntegration(iri1_str, iri2_str);
			fin = Calendar.getInstance().getTimeInMillis(); //load input ontologies
			LogOutput.print("Time checking impact (s): " + (float)((double)fin-(double)init)/1000.0);
		}
		
		
		
		
		
		//Clean index structures and others...
		//Not here because OAEI
		//index.clearTaxonomicalStructures();
		//Since we need to return the mappings! We cannot remove the index!
		
		
	}
	
	SatisfiabilityIntegration sat_checker;
	boolean hasUnSat = false;
	int numUnsat = 0;
	
	public boolean hasUnsatClasses(){
		
		return hasUnSat;
	}
	
	public int getNumUnsatClasses(){
		
		return numUnsat;
	}
	
	
	private void impactIntegration() throws Exception {
		
		OWLOntology mappins_owl_onto = getOWLOntology4Mappings();
		
		LogOutput.print(overlappingExtractor.getOverlappingOnto1().getAxiomCount());
		LogOutput.print(overlappingExtractor.getOverlappingOnto2().getAxiomCount());
		LogOutput.print(mappins_owl_onto.getAxiomCount());
		
		
		
		sat_checker = new SatisfiabilityIntegration(
				overlappingExtractor.getOverlappingOnto1(), 
				overlappingExtractor.getOverlappingOnto2(),
				mappins_owl_onto,
				true,//Time_Out_Class
				false); //use factory
		
		hasUnSat=sat_checker.hasUnsatClasses();
		numUnsat=sat_checker.getNumUnsatClasses();
		
		LogOutput.print("Num unsat classes: " + sat_checker.getNumUnsatClasses());
		
		
		/*ImpactAuditedIntegration impact = new ImpactAuditedIntegration();
		
		try{
			impact.reasonWithGivenOntologiesHermit(
				overlappingExtractor.getOverlappingOnto1(), 
				overlappingExtractor.getOverlappingOnto2(),
				getOWLOntology4Mappings());
		}
		catch (Exception e){
			System.err.println("HermiT could not deal with these ontologies: " + e.getMessage());
		}
		
		try{
			impact.reasonWithGivenOntologiesPellet(
				overlappingExtractor.getOverlappingOnto1(), 
				overlappingExtractor.getOverlappingOnto2(),
				getOWLOntology4Mappings());
		}
		catch (Exception e){
			System.err.println("Pellet could not deal with these ontologies: " + e.getMessage());
		}
		*/
		
		
	}
	
	private void impactIntegration(String iri1, String iri2) throws Exception {
		
		OWLOntology mappings_owl_onto = getOWLOntology4Mappings();
		
		OntologyLoader loader1 = new OntologyLoader(iri1);
		OntologyLoader loader2 = new OntologyLoader(iri2);
		
		LogOutput.printAlways("Evaluating impact...");
				
		LogOutput.print("Axioms onto1: " + loader1.getOWLOntology().getAxiomCount());
		LogOutput.print("Axioms onto2: " + loader2.getOWLOntology().getAxiomCount());
		LogOutput.print("Mapping Axioms: " + mappings_owl_onto.getAxiomCount());
		
		
		
		sat_checker = new SatisfiabilityIntegration(
				loader1.getOWLOntology(), 
				loader2.getOWLOntology(),
				mappings_owl_onto,
				true,//Time_Out_Class
				false); //use factory
		
		
		hasUnSat=sat_checker.hasUnsatClasses();
		numUnsat=sat_checker.getNumUnsatClasses();
		
		//OWLManager.createOWLOntologyManager().saveOntology(mappings_owl_onto, new RDFXMLOntologyFormat(),
				//IRI.create("file:/usr/local/data/MappingsConferenceBenchmark/ontologies/mappings-cmt-confof.owl"));
		//		IRI.create("file:/usr/local/data/MappingsConferenceBenchmark/ontologies/mappings-edas-ekaw.owl"));
		LogOutput.print("Num unsat classes: " + sat_checker.getNumUnsatClasses());
	}
	
	
	
	/**
	 * This method will be used to build an OWLOntology object to evaluate the logical impact
	 * The ontology will not be stored (see @saveExtractedMappings())
	 * 
	 * @return
	 * @throws Exception
	 */
	public OWLOntology getOWLOntology4Mappings() throws Exception{
	
		int ident2;
		
		OWLAlignmentFormat owlformat = new OWLAlignmentFormat("");
		
		int dir_mapping;
		
		for (int ide1 : getClassMappings().keySet()){
			for (int ide2 : getClassMappings().get(ide1)){
				
				//LogOutput.print(getIRI4ConceptIdentifier(ide1));
				//LogOutput.print(getIRI4ConceptIdentifier(ide2));
				//LogOutput.print(getConfidence4ConceptMapping(ide1, ide2));
				//LogOutput.print("");
				
				dir_mapping = getDirClassMapping(ide1, ide2);
				
				if (dir_mapping!=Utilities.NoMap){
						
					//System.out.println(getIRI4ConceptIdentifier(ide1));
					//System.out.println(getIRI4ConceptIdentifier(ide2));
					//System.out.println(dir_mapping);
								
					if (dir_mapping!=Utilities.R2L){
						owlformat.addClassMapping2Output(
								getIRI4ConceptIdentifier(ide1),
								getIRI4ConceptIdentifier(ide2),
								dir_mapping,
								getConfidence4ConceptMapping(ide1, ide2)
								);
					}
					else{
						owlformat.addClassMapping2Output(
								getIRI4ConceptIdentifier(ide2),
								getIRI4ConceptIdentifier(ide1),								
								dir_mapping,
								getConfidence4ConceptMapping(ide1, ide2)
							);
					}
				}
			}
		}
		
		for (int ide1 : getDataPropMappings().keySet()){
			
			
			
			//System.out.println(getIRI4DataPropIdentifier(ide1));
			//System.out.println(getIRI4DataPropIdentifier(getDataPropMappings().get(ide1)));
			//System.out.println(getConfidence4DataPropConceptMapping(ide1, getDataPropMappings().get(ide1)));
			
			
			owlformat.addDataPropMapping2Output(
					getIRI4DataPropIdentifier(ide1),
					getIRI4DataPropIdentifier(getDataPropMappings().get(ide1)),
					Utilities.EQ,  
					getConfidence4DataPropConceptMapping(ide1, getDataPropMappings().get(ide1))//1.0
				);
		}
		
		for (int ide1 : getObjectPropMappings().keySet()){
			
			//System.out.println(getIRI4ObjectPropIdentifier(ide1));
			//System.out.println(getIRI4ObjectPropIdentifier(getObjectPropMappings().get(ide1)));
			//System.out.println(getConfidence4ObjectPropConceptMapping(ide1, getObjectPropMappings().get(ide1)));
			//LogOutput.print("");
				
			owlformat.addObjPropMapping2Output(
					getIRI4ObjectPropIdentifier(ide1),
					getIRI4ObjectPropIdentifier(getObjectPropMappings().get(ide1)),
					Utilities.EQ, 
					getConfidence4ObjectPropConceptMapping(ide1, getObjectPropMappings().get(ide1))//1.0
				);
		}
		

		if (Parameters.perform_instance_matching){
			
			for (int ide1 : getInstanceMappings().keySet()){
				for (int ide2 : getInstanceMappings().get(ide1)){
				
					owlformat.addInstanceMapping2Output(
							getIRI4InstanceIdentifier(ide1), 
							getIRI4InstanceIdentifier(ide2), 
							getConfidence4InstanceMapping(ide1, ide2)
						);
					
				}
			}
		}
		
		return owlformat.getOWLOntology();
		
	}
	
	
	
	private boolean areAnchorsdAmbiguous(){
		
		for (int ide : mapping_extractor.getAnchors().keySet()){
			
			if (mapping_extractor.getAnchors().get(ide).size()>1){
				LogOutput.print(ide + "  " + mapping_extractor.getAnchors().get(ide));
				return true;
			}
			
		}
		
		return false;
		
		
	}
	
	
	
	
	private void InitLogMap() throws Exception{
		
		//Show print outs
		LogOutput.showOutpuLog(Parameters.print_output); //False for OAEI?
		LogOutput.showOutpuLogAlways(Parameters.print_output || Parameters.print_output_always);
		
		
		
		//Only from OAEI-LogMap
		//Parameters.readParameters();
		
		
		lexicalUtilities.loadStopWords();
		//LexicalUtilities.loadStopWordsExtended();
		
		if (Parameters.use_umls_lexicon)
			lexicalUtilities.loadUMLSLexiconResources();
		
		lexicalUtilities.setStemmer(); //creates stemmer object (Paice by default)
		
		//Lib.debuginfo(LexicalUtilities.getStemming4Word("Prolactin") + " " + LexicalUtilities.getStemming4Word("brachii") + "\n");
		
		fin = Calendar.getInstance().getTimeInMillis();
		LogOutput.print("Time initializing lexical utilities (s): " + (float)((double)fin-(double)init)/1000.0);
		
		
		//init = Calendar.getInstance().getTimeInMillis();
		//PrecomputeIndexCombination.preComputeIdentifierCombination();
		//fin = Calendar.getInstance().getTimeInMillis();
		//LogOutput.print("Time precomputing index combinations (s): " + (float)((double)fin-(double)init)/1000.0);
		
	}
	
	
	
	private void OverlappingEstimation(String iri1_str, String iri2_str) throws Exception{
		
		LogOutput.print("OVERLAPPING");
		init = Calendar.getInstance().getTimeInMillis();
		
		//Overlapping to store and compare 
		//overlappingExtractor = new LexicalOverlappingExtractor(gs_mappings, logmap_mappings, iri1_str_out, iri2_str_out);
		
		//Overlapping to be used in the normal behaviour
		//No overlapping if size onto <10,000 entities (see parameters file)
		if (!Parameters.use_overlapping){
			overlappingExtractor = new NoOverlappingExtractor();
		}
		else{
			overlappingExtractor = new LexicalOverlappingExtractor(lexicalUtilities);
		}

		
		overlappingExtractor.createOverlapping(iri1_str, iri2_str);
		fin = Calendar.getInstance().getTimeInMillis();
		LogOutput.print("Time extracting overlapping (s): " + (float)((double)fin-(double)init)/1000.0);
	}
	
	private void OverlappingEstimation(OWLOntology onto1, OWLOntology onto2) throws Exception{
		
		LogOutput.print("OVERLAPPING");
		init = Calendar.getInstance().getTimeInMillis();
		
		//Overlapping to store and compare 
		//overlappingExtractor = new LexicalOverlappingExtractor(gs_mappings, logmap_mappings, iri1_str_out, iri2_str_out);
		
		//Overlapping to be used in the normal behaviour
		//No overlapping if size onto <10,000 entities (see parameters file)
		if (!Parameters.use_overlapping){
			overlappingExtractor = new NoOverlappingExtractor();
		}
		else{
			overlappingExtractor = new LexicalOverlappingExtractor(lexicalUtilities);
		}

		
		overlappingExtractor.createOverlapping(onto1, onto2);
		fin = Calendar.getInstance().getTimeInMillis();
		LogOutput.print("Time extracting overlapping (s): " + (float)((double)fin-(double)init)/1000.0);
	}
	
	
	
	private void IndexLexiconAndStructure() throws Exception{
		
		//Create Index and new Ontology Index...
		index = new JointIndexManager();
		
		
		//Process ontologies: lexicon and taxonomy (class) and IFs
		onto_process1 = new OntologyProcessing(overlappingExtractor.getOverlappingOnto1(), index, lexicalUtilities);
		onto_process2 = new OntologyProcessing(overlappingExtractor.getOverlappingOnto2(), index, lexicalUtilities);
		
		
		//Extracts lexicon
		init = Calendar.getInstance().getTimeInMillis();
		onto_process1.precessLexicon();
		onto_process2.precessLexicon();
		fin = Calendar.getInstance().getTimeInMillis();
		LogOutput.print("Time extracting lexicon and IF (s): " + (float)((double)fin-(double)init)/1000.0);
		
		//I guess can be deleted here
		lexicalUtilities.clearStructures();
		
		
		init = Calendar.getInstance().getTimeInMillis();
		//Init Mapping extractor: intersects IF and extract IF weak
		//mapping_extractor = new LexicalMappingExtractor(index, onto_process1, onto_process2);
		mapping_extractor = new CandidateMappingManager(index, onto_process1, onto_process2);
		
		//Statistics
		StatisticsManager.reInitValues();
		StatisticsManager.setMappingManager(mapping_extractor);
		
		
		mapping_extractor.intersectInvertedFiles();
		fin = Calendar.getInstance().getTimeInMillis();
		LogOutput.print("Time intersecting IF and extracting IF weak (s): " + (float)((double)fin-(double)init)/1000.0);
		
		//Clear ontology stemmed labels 
		onto_process1.clearStemmedLabels();
		onto_process2.clearStemmedLabels();
		
		//Extracts Taxonomy
		//Also extracts A^B->C
		init = Calendar.getInstance().getTimeInMillis();
		onto_process1.setTaxonomicData();
		onto_process2.setTaxonomicData();
		fin = Calendar.getInstance().getTimeInMillis();
		LogOutput.print("Time extracting structural information (s): " + (float)((double)fin-(double)init)/1000.0);
		
		
		//TODO Remove!!
		//if (!gs_mappings.equals("")){
		//	loadMappingsGS();
		//}
		
		
		
		//Keep only TBOX axioms to extract final module
		if (logmap_mappings_path.equals("")){ // no output files for overlappings
			if (!evaluate_impact){
				overlappingExtractor.clearModulesOverlapping();//Not necessary if we do no provide output
			}
		}
		else {
			overlappingExtractor.keepOnlyTBOXOverlapping(!evaluate_impact);
		}
		
		
		
		
		//We do not need the references to OWLEntities anymore
		onto_process1.clearOntologyRelatedInfo();
		onto_process2.clearOntologyRelatedInfo();

		/*
		loadMappingsGS(gs_mappings);
		printStatisticsGoldStandard();
		if (true)
		{
			LogOutput.print("finished!");
			return ;
		}
		*/
		
		
		//We first create weak anchors to be used for scopes
		init = Calendar.getInstance().getTimeInMillis();
		mapping_extractor.extractAllWeakMappings();
		fin = Calendar.getInstance().getTimeInMillis();
		LogOutput.print("Time creating all weak anchors (s): " + (float)((double)fin-(double)init)/1000.0);
		
		
		//Extract subsets from all weak mappings to evaluate later
		//----------------------------------------------------------
		init = Calendar.getInstance().getTimeInMillis();
		mapping_extractor.extractCandidatesSubsetFromWeakMappings();
		fin = Calendar.getInstance().getTimeInMillis();
		LogOutput.print("Time creating candidate subset of weak anchors (s): " + (float)((double)fin-(double)init)/1000.0);
		
		
		//Remove structures used for frequency extractor
		
		//Frequency structures
		onto_process1.clearFrequencyRelatedStructures();
		onto_process2.clearFrequencyRelatedStructures();
		index.clearSingleWordInvertedIndex();
		
		//Only used by frequency-like weak mappings
		index.clearStemmedAlternativeLabels4Classes();
		
		
	}
	
	
	
	private void createAndCleanAnchors() throws Exception{
		
		LogOutput.printAlways("\nANCHOR DIAGNOSIS ");
		
		
		//TODO test for mouse 2 anatomy
		//Background knowledge: mappings composition
		//((CandidateMappingManager)mapping_extractor).createCandidatesFromBackgroundKnowledge();
		//getPrecisionAndRecallMappings();
		
		
		init = Calendar.getInstance().getTimeInMillis();
		mapping_extractor.createAnchors();
		
		//Create different groups: "exact", ambiguity and no_scope (different sets...). We will add them later (almost done)
		
		fin = Calendar.getInstance().getTimeInMillis();
		LogOutput.printAlways("Time creating anchors (s): " + (float)((double)fin-(double)init)/1000.0);
		
		
		
		
		//if (true){
		//	return ;
		//}
		
		//Tests
		//outputMappings(mapping_extractor.getAnchors(), logmap_mappings_path + "good.txt");
		
		
		
		
		countAnchors();
		mappings_assessment = new AnchorAssessment(index, mapping_extractor);
		
				
		if (cleanD_G){
		
			//Only for statistical purposes
			/*
			init = Calendar.getInstance().getTimeInMillis();
			mappings_assessment.CountSatisfiabilityOfIntegration_DandG(mapping_extractor.getAnchors());
			fin = Calendar.getInstance().getTimeInMillis();
			LogOutput.printAlways("Time checking satisfiability D&G (s): " + (float)((double)fin-(double)init)/1000.0);
			*/
			
			init = Calendar.getInstance().getTimeInMillis();
			mappings_assessment.CheckSatisfiabilityOfIntegration_DandG(mapping_extractor.getAnchors());
			fin = Calendar.getInstance().getTimeInMillis();
			LogOutput.printAlways("Time cleaning anchors D&G (s): " + (float)((double)fin-(double)init)/1000.0);
		}
		
		//After repairing exact
		mapping_extractor.setExactAsFixed(true);
		countAnchors();
		
		
		//TODO extract further disjointness based on weak anchors and mappings
		
		
		
		//INTERVAL LABELLING SCHEMA
		//--------------------------
		init = Calendar.getInstance().getTimeInMillis();
		
		//Index already have the necessary taxonomical information apart from the equiv mappings

		index.setIntervalLabellingIndex(mapping_extractor.getFixedAnchors());
		
		index.clearAuxStructuresforLabellingSchema();
		
		fin = Calendar.getInstance().getTimeInMillis();
		LogOutput.printAlways("Time indexing hierarchy + anchors (ILS) (s): " + (float)((double)fin-(double)init)/1000.0);
		
	}
	
	
	
	
	private void countAnchors(){
		
		int numMappings = 0;
		
		for (int ide1: mapping_extractor.getAnchors().keySet()){
			for (int ide2: mapping_extractor.getAnchors().get(ide1)){
			
				if (ide1<ide2)
					numMappings++;
				
			}
			
			
			
		}
		
		LogOutput.print("\nNum Anchors: " + numMappings + "\n");
		
		
	}
	
	
	
	private void createCandidateMappings() throws Exception{
		
		LogOutput.printAlways("\nCANDIDATE DIAGNOSIS 1");
		
		//After this method we will have 3 sets: Mappings2Review with DandG, Mappings to ask user, and discarded mappoings
		
		init = Calendar.getInstance().getTimeInMillis();
		mapping_extractor.createCandidates();
		
		
		//Delete Alt labels in class index
		//We won't extract more mappings
		index.clearAlternativeLabels4Classes();
		
		fin = Calendar.getInstance().getTimeInMillis();
		LogOutput.printAlways("Time creating candidates (s): " + (float)((double)fin-(double)init)/1000.0);
		
		
		//Uncomment if no fixed anchors
		//We add candidates to anchors, nothing noew is fixed
		//mapping_extractor.setExactAsFixed(false);
		//mapping_extractor.moveMappingsToReview2AnchorList();
		
		
		//Clean with Dawling and Gallier mappings 2 review
		//-- D&G
		//countAnchors();
		//mappings_assessment = new AnchorAssessment(index, mapping_extractor);
		init = Calendar.getInstance().getTimeInMillis();
		if (cleanD_G){
			//Adds clean mappings to anchors. Conflictive and split mappings to respective sets
			init = Calendar.getInstance().getTimeInMillis();
			mappings_assessment.CheckSatisfiabilityOfIntegration_DandG(mapping_extractor.getMappings2Review());  //With Fixed mappings!
			//mappings_assessment.CheckSatisfiabilityOfIntegration_DandG(mapping_extractor.getAnchors());  //No fixed mappings
			fin = Calendar.getInstance().getTimeInMillis();
			LogOutput.printAlways("Time cleaning new candidates D&G (s): " + (float)((double)fin-(double)init)/1000.0);
		}
		
		//Merge mappings 2 review and anchors
		//Comment if no fixed anchors
		mapping_extractor.moveMappingsToReview2AnchorList();
		
		
		
	
		
		
		countAnchors();
		
		
		//Remove mappings to review
		mapping_extractor.getMappings2Review().clear();
		
		
		//INTERVAL LABELLING SCHEMA
		//--------------------------
		init = Calendar.getInstance().getTimeInMillis();
		index.setIntervalLabellingIndex(mapping_extractor.getAnchors());//It also contains mappings 2 review
		index.clearAuxStructuresforLabellingSchema();
		fin = Calendar.getInstance().getTimeInMillis();
		LogOutput.printAlways("Time indexing hierarchy + anchors and candidates I (ILS) (s): " + (float)((double)fin-(double)init)/1000.0);
		
		//LogOutput.print("\n\nNEW UNSAT:");
		//To check not solved cases of unsatisfiability
		//mappings_assessment.CheckSatisfiabilityOfConcreteClasses_DandG(mapping_extractor.getAnchors(), index.getUnsatisfiableClassesILS());
		
		//Assess mappings 2 ask user
		mapping_extractor.assessMappings2AskUser();
		
				
		//Get Anchor statistics (after 2 iterations and cleaning them)
		StatisticsManager.extractStatisticsAnchors();
		
		
		
		
		
		
		
	}
		
	
	/**
	 * Only for interactivity testing
	 */
	private void createCandidateMappingsInteractiveProcess2(){
		
		
		boolean useThreshold=false;
		
		//At this point we only have mappings to ask user
		init = Calendar.getInstance().getTimeInMillis();
		
		
		if (!useThreshold)
			interactiveProcessManager = 
				new InteractiveProcessAmbiguity(
						index, mapping_extractor, useHeuristics, orderQuestions, error_user, ask_everything,
						record_interactivity,
						logmap_mappings_path + "SimulationInteractivity_" + useInteractivity + "_" + useHeuristics + "_" + orderQuestions + ".txt");
		else 
			interactiveProcessManager = new InteractiveProcessThreshold(index, mapping_extractor);
		
		if (useInteractivity){
			interactiveProcessManager.startInteractiveProcess(); //Starts "automatic" user interaction			
		}
		//TODO!!!!
		interactiveProcessManager.endInteractiveProcess(mapping_extractor.isFilterWithHeuristicsSecondLevelMappings()); //adds mappings selected by user and logmap heuristics
		/*else {
			for (MappingObjectInteractivity mapping : mapping_extractor.getListOfMappingsToAskUser()){
				
				mapping_extractor.addSubMapping2Mappings2Review(mapping.getIdentifierOnto1(), mapping.getIdentifierOnto2());
				mapping_extractor.addSubMapping2Mappings2Review(mapping.getIdentifierOnto2(), mapping.getIdentifierOnto1());
				
			}
		}*/
		
		
		
		fin = Calendar.getInstance().getTimeInMillis();
		LogOutput.print("Time interactive process (s): " + (float)((double)fin-(double)init)/1000.0);

		
		
	}
	
	
	/**
	 * Uses an oracle for the interactivity (i.e GS)
	 */
	private void createCandidateMappingsInteractiveProcess(){
				
		InteractiveProcessOAEI interactivityOAEI = 
				new InteractiveProcessOAEI(index, mapping_extractor, false, false);
		
		//We strat asking to oracle and we also try to apply automatic decisions 
		interactivityOAEI.startInteractiveProcess();
		
		//We add to list of accepted mappings the validated mappings
		interactivityOAEI.endInteractiveProcess();
		
		
		//Adhoc method
		//askAll2AOracle()
		
		
	}
	
	
	/**
	 * @deprecated
	 */
	private void askAll2AOracle(){
		//Adhoc method ask everything
		//------------------------------------
		for (MappingObjectInteractivity mapping : mapping_extractor.getListOfMappingsToAskUser()){
					
			if (Oraculo.isMappingValid(
					index.getIRIStr4ConceptIndex(mapping.getIdentifierOnto1()),
					index.getIRIStr4ConceptIndex(mapping.getIdentifierOnto2()))){
						
				//SOME MAPPINGS MAY REPRESENT ONLY ONE SIDE
				//EITHER THE USER DECIDED TO SPLIT IT or IT WAS SPLIT BY D&G
				if (mapping.getDirMapping()==Utilities.EQ || mapping.getDirMapping()==Utilities.L2R)
					mapping_extractor.addSubMapping2Mappings2Review(mapping.getIdentifierOnto1(), mapping.getIdentifierOnto2());
						
				if (mapping.getDirMapping()==Utilities.EQ || mapping.getDirMapping()==Utilities.R2L)
					mapping_extractor.addSubMapping2Mappings2Review(mapping.getIdentifierOnto2(), mapping.getIdentifierOnto1());
					
			}
		}
	}
	
	
	
	
	private boolean hasScopeAll(MappingObjectInteractivity m){
		
		return (mapping_extractor.extractScopeAll4Mapping(m.getIdentifierOnto1(), m.getIdentifierOnto2())>Parameters.bad_score_scope);
		
	}
	
	private boolean hasGoodConfidence(MappingObjectInteractivity m){
		
		return (mapping_extractor.getConfidence4Mapping(m.getIdentifierOnto1(), m.getIdentifierOnto2())>Parameters.good_confidence);
		
	}
	
	/**
	 * Automatic decisions to mappings to ask if interactivity is not active
	 */
	private void performAutomaticDecisions(){

		for (MappingObjectInteractivity mapping : mapping_extractor.getListOfMappingsToAskUser()){
			
			
			//See createMappings2AskUser in mapping_extractor for more information about the use of this filter
			if (!mapping_extractor.isFilterWithHeuristicsSecondLevelMappings() 
				|| hasScopeAll(mapping) && hasGoodConfidence(mapping)){
				
				//SOME MAPPINGS MAY REPRESENT ONLY ONE SIDE
				//EITHER THE USER DECIDED TO SPLIT IT or IT WAS SPLIT BY D&G
				if (mapping.getDirMapping()==Utilities.EQ || mapping.getDirMapping()==Utilities.L2R)
					mapping_extractor.addSubMapping2Mappings2Review(mapping.getIdentifierOnto1(), mapping.getIdentifierOnto2());
				
				if (mapping.getDirMapping()==Utilities.EQ || mapping.getDirMapping()==Utilities.R2L)
					mapping_extractor.addSubMapping2Mappings2Review(mapping.getIdentifierOnto2(), mapping.getIdentifierOnto1());
			
			}
		}
				
		//mapping_extractor.setStringAnchors(); ???
			
	}
	
	
	
	private void createCandidateMappingsLevel2() throws Exception{
		
		
		LogOutput.printAlways("\nCANDIDATE DIAGNOSIS 2");
					
		if (Oraculo.isActive()){
			LogOutput.printAlways("Oracle is active for interactivity.");
			createCandidateMappingsInteractiveProcess();
		}
		else{
			LogOutput.printAlways("Oracle is not active. Performing automatic decisions.");
			performAutomaticDecisions();
		}
		
		
		//Also index interval labelling?? Problem, no mappings 1-1 for index....
		//We keep previous index
		
		
		
		
		//Add weakened mappings by D and G iff no conflictive or already inferred
		//Add no conflictive to anchors and the clean together with interactivity
		//We should add a low confidence so that if they are involved in an error then remove
		//isMappingWeakenedDandG
		//!!!Already Included in assesMappings2AskUser!!!
		//mapping_extractor.assesWeakenedMappingsDandG(true, false); //we add them to mappings to review
		
		
		//Retrieve mappings from conflictive mappings if not in conflict
		//Then add to mapping to review
		if (Parameters.second_chance_conflicts)
			secondChanceConflictiveMappingsD_G();
		
		
		
		
		
		
		//Clean interactive-like mappings + weakened with DandG		
		
		init = Calendar.getInstance().getTimeInMillis();
		if (cleanD_G){
			
			
			//For statistics
			/*
			init = Calendar.getInstance().getTimeInMillis();
			mappings_assessment.CountSatisfiabilityOfIntegration_DandG(mapping_extractor.getMappings2Review());
			fin = Calendar.getInstance().getTimeInMillis();
			LogOutput.printAlways("Time checking satisfiability D&G (s): " + (float)((double)fin-(double)init)/1000.0);
			*/
			
			//Adds clean mappings to anchors. Conflictive and split mappings to respective sets
			init = Calendar.getInstance().getTimeInMillis();
			mappings_assessment.CheckSatisfiabilityOfIntegration_DandG(mapping_extractor.getMappings2Review());  //With Fixed mappings!
			//mappings_assessment.CheckSatisfiabilityOfIntegration_DandG(mapping_extractor.getAnchors());  //No fixed mappings
			fin = Calendar.getInstance().getTimeInMillis();
			LogOutput.printAlways("Time cleaning interactive mappings D&G (s): " + (float)((double)fin-(double)init)/1000.0);
		}
		
		//Merge mappings 2 review and anchors
		//Comment if no fixed anchors
		mapping_extractor.moveMappingsToReview2AnchorList();
		
		
		//Add new weakened mappings
		//Do not add them? Since they are suspicious>?
		//mapping_extractor.assesWeakenedMappingsDandG(false, true); //we add them to anchors
		
		
		//Remove mappings to review
		mapping_extractor.getMappings2Review().clear();
		
		
		
	}
	
	/**
	 * This method aims at giving a second chance to those mappings involved in a conflict
	 * and removed using the D&G method. Some of them may be good and not cause an error
	 */
	private void secondChanceConflictiveMappingsD_G(){
		
		LogOutput.print("Second chance to mappings discarded by Dowling and Gallier.");
		
		int second_chance = 0;
		int good_second_chance = 0;
		
		Map<Integer, Set<Integer>> toDelete = new HashMap<Integer, Set<Integer>>();
		
		
		//Check conflicts with fixed mappings
		//--------------------------------------
		for (int ide1 : mapping_extractor.getConflictiveMappings_D_G().keySet()){
		
			for (int ide2 : mapping_extractor.getConflictiveMappings_D_G().get(ide1)){
				
				if (mapping_extractor.isMappingInConflictWithFixedMappings(ide1, ide2)){
					
					if (!toDelete.containsKey(ide1)){
						toDelete.put(ide1, new HashSet<Integer>());
					}
					toDelete.get(ide1).add(ide2);
					

				}
				
			}
		}
		
		
		for (int ide1 : toDelete.keySet()){
			for (int ide2 : toDelete.get(ide1)){
				
				mapping_extractor.addSubMapping2ConflictiveAnchors(ide1, ide2);
				
				mapping_extractor.removeSubMappingFromConflictive_D_G(ide1, ide2);
			}
		}
		
		toDelete.clear();
				
				
		
		//Check if they are in conflict between them
		//If in conflict then check confidences and remove the one with less value
		//-----------------------------------------------------------------------------
		for (int ide1 : mapping_extractor.getConflictiveMappings_D_G().keySet()){
			
			for (int ide2 : mapping_extractor.getConflictiveMappings_D_G().get(ide1)){
				
				for (int ideA : mapping_extractor.getConflictiveMappings_D_G().keySet()){
					
					for (int ideB : mapping_extractor.getConflictiveMappings_D_G().get(ideA)){
						
						if (ide1==ideA && ide2==ideB)
							continue;
						
						if (mapping_extractor.areMappingsInConflict(ide1, ide2, ideA, ideB)){
						
							//Check confidences		
							if (mapping_extractor.getConfidence4Mapping(ide1, ide2) >=
									mapping_extractor.getConfidence4Mapping(ideA, ideB)){
							
								if (!toDelete.containsKey(ideA)){
									toDelete.put(ideA, new HashSet<Integer>());
								}
								toDelete.get(ideA).add(ideB);
							}
							else{
								
								if (!toDelete.containsKey(ide1)){
									toDelete.put(ide1, new HashSet<Integer>());
								}
								toDelete.get(ide1).add(ide2);
							}
						}
						
						
					}
				}
			}	
		}
		
		
		for (int ide1 : toDelete.keySet()){
			for (int ide2 : toDelete.get(ide1)){
				
				mapping_extractor.addSubMapping2ConflictiveAnchors(ide1, ide2);
				
				mapping_extractor.removeSubMappingFromConflictive_D_G(ide1, ide2);
			}
		}
		toDelete.clear();
		
		
		
		//Check conflicts with mappings 2 review
		//--------------------------------------
		for (int ide1 : mapping_extractor.getConflictiveMappings_D_G().keySet()){
					
			for (int ide2 : mapping_extractor.getConflictiveMappings_D_G().get(ide1)){
				
				//Already filtered
				//if (mapping_extractor.isMappingInConflictWithFixedMappings(ide1, ide2)){
				//	mapping_extractor.addSubMapping2ConflictiveAnchors(ide1, ide2);
				//}
				//else { //If no conflict add to mappings to review
					
					//Check if there are mappings in conflict in mappings2review
					for (int ideA : mapping_extractor.getMappings2Review().keySet()){
						
						for (int ideB : mapping_extractor.getMappings2Review().get(ideA)){
							
							if (mapping_extractor.areMappingsInConflict(ide1, ide2, ideA, ideB)){
								
								//Remove mapping 2 review. We give priority to the anchor deleted by D&G
								if (!toDelete.containsKey(ideA)){
									toDelete.put(ideA, new HashSet<Integer>());
								}
								toDelete.get(ideA).add(ideB);		
										
							}
							
						}
					}
					//We remove them to facilitate D&G processs
					for (int ideA : toDelete.keySet()){
						for (int ideB : toDelete.get(ideA)){
							mapping_extractor.removeSubMappingFromMappings2Review(ideA, ideB);
						}
					}
					
					
					mapping_extractor.addSubMapping2Mappings2Review(ide1, ide2); //check with new ones
					
					if (mapping_extractor.isMappingInGoldStandard(ide1, ide2))
						good_second_chance++;
					
					second_chance++;
					
					 
					
					
				}
			//}
		}
		LogOutput.print("Mappings with second chance: " + second_chance + " in GS: " + good_second_chance);
		
		
	}
	
	
	/**
	 * We perform a last check using D&G
	 */
	private void lastLogicalCleaning(){
		
		if (cleanD_G){
		
			//Adds clean mappings to anchors. Conflictive and split mappings to respective sets
			init = Calendar.getInstance().getTimeInMillis();
		
			mapping_extractor.setExactAsFixed(false);
			mappings_assessment.CheckSatisfiabilityOfIntegration_DandG(mapping_extractor.getAnchors());  ///No fixed mappings: we clean everything just in case
			mapping_extractor.setExactAsFixed(true);
			
			fin = Calendar.getInstance().getTimeInMillis();
			LogOutput.printAlways("LAST CLEANING MAPPINGS D&G (s): " + (float)((double)fin-(double)init)/1000.0);
			
		}
		
		
	}
	
	
	
	
	/**
	 * Discovery and assessment of DATA and OBJECT property mappings
	 */
	private void createAndAssessPropertyMappings(){
		mapping_extractor.createObjectPropertyAnchors();
		mapping_extractor.createDataPropertyAnchors();
		
		
		//Delete inverted files for properties
		onto_process1.clearInvertedFiles4properties();
		onto_process2.clearInvertedFiles4properties();
	}
	
	
	/**
	 * Discovery and assessment of Instance mappings
	 */
	private void createAndAssessInstanceMappings(){
		
		mapping_extractor.createInstanceAnchors();
		
		//Clean D&G
		if (mapping_extractor.getInstanceMappings().size()>0 && cleanD_G){
			
			init = Calendar.getInstance().getTimeInMillis();
			
			//We have an specific method since there is not a top-down search. And we first repair classes
			mappings_assessment.CheckSatisfiabilityOfIntegration_DandG_Individuals(
					mapping_extractor.getInstanceMappings());
			
			fin = Calendar.getInstance().getTimeInMillis();
			LogOutput.printAlways("Time cleaning instance mappings D&G (s): " + (float)((double)fin-(double)init)/1000.0);
		}
		
		
		onto_process1.clearInvertedFiles4Individuals();
		onto_process2.clearInvertedFiles4Individuals();
	
	
	}
	
	
	
	Set<OWLEntity> signature_onto1 = new HashSet<OWLEntity>();
	Set<OWLEntity> signature_onto2 = new HashSet<OWLEntity>();
	
	private void createSignatureFromMappings(Map<Integer, Set<Integer>> mappings){
		
		for (int idea : mappings.keySet()){
			for (int ideb : mappings.get(idea)){
				
				if (mapping_extractor.isId1SmallerThanId2(idea, ideb)){
					
					signature_onto1.add(
							dataFactory.getOWLClass(index.getIRI4ConceptIndex(idea)));
					
					//System.out.println(index.getIRI4ConceptIndex(idea));
					
					
					signature_onto2.add(
							dataFactory.getOWLClass(index.getIRI4ConceptIndex(ideb)));
					
				}
				else{ 
					
					//Not alresdy included
					if (!mapping_extractor.isMappingAlreadyInList(ideb, idea)){
					
						signature_onto1.add(
								dataFactory.getOWLClass(index.getIRI4ConceptIndex(ideb)));

						//System.out.println(index.getIRI4ConceptIndex(ideb));
						
						signature_onto2.add(
								dataFactory.getOWLClass(index.getIRI4ConceptIndex(idea)));
						
					}
					
					
				}
			}
		}
		
	}
	
	/**
	 * This method creates the OWL files correspondent to the overlapping/module files.
	 * Uses Old extractor
	 */
	private void createOutput4Overlapping(boolean use_discarded){
		
		
		createSignatureFromMappings(mapping_extractor.getAnchors());
		if (use_discarded)
			createSignatureFromMappings(mapping_extractor.getDircardedAnchors());
		
		
		
		//EXTRACT MODULE 1
		ModuleExtractor module_extractor1 = new ModuleExtractor(
				overlappingExtractor.getTBOXOverlappingOnto1(), false, false, true, true, false);
		
		module_extractor1.getLocalityModuleForSignatureGroup(signature_onto1, onto_process1.getOntoIRI());		
		
		module_extractor1.saveExtractedModule(prefix4IRIs + logmap_mappings_path + "/module1_overlapping_logmap2.owl"); //"Output/module1.owl"
		
		module_extractor1.clearStrutures();
		overlappingExtractor.getTBOXOverlappingOnto1().clear();
		signature_onto1.clear();
		
		
		//EXTRACT MODULE 2
		ModuleExtractor module_extractor2 = new ModuleExtractor(
				overlappingExtractor.getTBOXOverlappingOnto2(), false, false, true, true, false);
		
		module_extractor2.getLocalityModuleForSignatureGroup(signature_onto2, onto_process2.getOntoIRI());		
		
		module_extractor2.saveExtractedModule(prefix4IRIs + logmap_mappings_path + "/module2_overlapping_logmap2.owl"); //Output/module2.owl
		
		module_extractor2.clearStrutures();
		overlappingExtractor.getTBOXOverlappingOnto2().clear();	
		signature_onto2.clear();
		
		
		
		
	}
	
	
	
	private void saveExtractedMappings(String file_name){
		
		int dirMapping;
		
		OutPutFilesManager outPutFilesManager = new OutPutFilesManager();
		
		try {
			outPutFilesManager.createOutFiles(
					//logmap_mappings_path + "Output/mappings",
					logmap_mappings_path + "/" + file_name,
					OutPutFilesManager.AllFormats,
					onto_process1.getOntoIRI(),
					onto_process1.getOntoIRI());
			
			if (Parameters.output_class_mappings){
			
				for (int idea : mapping_extractor.getAnchors().keySet()){
					for (int ideb : mapping_extractor.getAnchors().get(idea)){
						
						//This is important to keep compatibility with OAEI and Flat alignment formats
						//The order of mappings is important
						//For OWL output would be the same since mappings are axioms
						if (mapping_extractor.isId1SmallerThanId2(idea, ideb)){
							
							if (mapping_extractor.isMappingAlreadyInList(ideb, idea)){
								dirMapping=Utilities.EQ;
							}
							else {
								dirMapping=Utilities.L2R;
							}
							
							outPutFilesManager.addClassMapping2Files(
									index.getIRIStr4ConceptIndex(idea),
									index.getIRIStr4ConceptIndex(ideb),
									dirMapping, 
									mapping_extractor.getConfidence4Mapping(idea, ideb));
						}
						else {
							if (mapping_extractor.isMappingAlreadyInList(ideb, idea)){
								//Do nothing
							}
							else {
								outPutFilesManager.addClassMapping2Files(
										index.getIRIStr4ConceptIndex(ideb),
										index.getIRIStr4ConceptIndex(idea),
										Utilities.R2L, 
										mapping_extractor.getConfidence4Mapping(idea, ideb));
							}
						}
					
						
					}
				}
			}
			
			
			if (Parameters.output_prop_mappings){
			
				for (int ide1 : getDataPropMappings().keySet()){							
					outPutFilesManager.addDataPropMapping2Files(
							getIRI4DataPropIdentifier(ide1),
							getIRI4DataPropIdentifier(getDataPropMappings().get(ide1)),
							Utilities.EQ,  
							getConfidence4DataPropConceptMapping(ide1, getDataPropMappings().get(ide1))//1.0
						);
				}
				
				for (int ide1 : getObjectPropMappings().keySet()){
						
					outPutFilesManager.addObjPropMapping2Files(
							getIRI4ObjectPropIdentifier(ide1),
							getIRI4ObjectPropIdentifier(getObjectPropMappings().get(ide1)),
							Utilities.EQ, 
							getConfidence4ObjectPropConceptMapping(ide1, getObjectPropMappings().get(ide1))//1.0
						);
				}
			}
			
			

			if (Parameters.perform_instance_matching && Parameters.output_instance_mappings){
				
				for (int ide1 : getInstanceMappings().keySet()){
					for (int ide2 : getInstanceMappings().get(ide1)){
					
						outPutFilesManager.addInstanceMapping2Files(
								getIRI4InstanceIdentifier(ide1), 
								getIRI4InstanceIdentifier(ide2), 
								getConfidence4InstanceMapping(ide1, ide2)
							);
						
					}
				
				}
			}
			
			
			
			outPutFilesManager.closeAndSaveFiles();
			
		}
		catch (Exception e){
			System.err.println("Error saving mappings...");
			e.printStackTrace();
		}
		
		
	}

	
	
	
	/**
	 * Load Gold Standard Mappings
	 * @throws Exception
	 */
	private void loadMappingsGS() throws Exception{
	
		ReadFile reader = new ReadFile(gs_mappings);
		
		
		String line;
		String[] elements;
		
		line=reader.readLine();
		
		int index1;
		int index2;
		double confidence;
		
		int wrong=0;
		
		while (line!=null) {
			
			if (line.indexOf("|")<0 && line.indexOf("\t")<0){
				line=reader.readLine();
				continue;
			}
			
			if (line.indexOf("|")>=0)
				elements=line.split("\\|");
			else { // if (line.indexOf("\t")>=0){
				elements=line.split("\\t");
			}
			
			//TODO temporal, only for im assessment
			/*if (!overlappingExtractor.getOverlappingOnto1().containsEntityInSignature(IRI.create(elements[0]), true) ||
					!overlappingExtractor.getOverlappingOnto2().containsEntityInSignature(IRI.create(elements[1]), true)){
				//LogOutput.printAlways("Wrong mapping: " + elements[0] + "  " + elements[1]);			
				wrong++;
				line=reader.readLine();
				continue;
			}*/
			
			
			
			//Necessary for preccsion and recall or only for GS cleaning
			index1=onto_process1.getIdentifier4ConceptName(Utilities.getEntityLabelFromURI(elements[0]));
			index2=onto_process2.getIdentifier4ConceptName(Utilities.getEntityLabelFromURI(elements[1]));
			
			if (index1>0 && index2>0){	//IN CASE IT DOES NOT EXISTS
				
				mapping_extractor.addMapping2GoldStandardAnchors(index1, index2);
				
				//if (umls_assessment)
				//	identifier2exactMapping.add(new MappingObjectIdentifiers(index1, index2));
				
			}
			
			//System.out.println(elements[0] + "  " + elements[1]);
			
			mappings_gs.add(new MappingObjectStr(elements[0], elements[1]));
			mapping_extractor.getStringGoldStandardAnchors().add(new MappingObjectStr(elements[0], elements[1]));
			
			
				
			line=reader.readLine();
		}		
		
		reader.closeBuffer();
		
		LogOutput.printAlways("Wrong mappings: " + wrong);

	}
	
	private void printStatisticsGoldStandard() throws IOException
	{
		Map<Integer, Set<Integer>> GS = mapping_extractor.getGoldStandardMappings();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("/auto/users/yzhou/gsmappings.txt")));
		
		ClassIndex class1, class2;
		Set<Integer> super1, super2, sub1, sub2, scope1, scope2;
		int count;
		
		for (Entry<Integer, Set<Integer>> entry : GS.entrySet())
		{
			class1 = index.getClassIndex(entry.getKey());
			super1 = class1.getDirectSuperclasses();
			sub1 = class1.getDirectSubclasses();
			
			scope1 = index.getScope4Identifier_Condifence(entry.getKey());
			
			for (int id : entry.getValue())
			{
				class2 = index.getClassIndex(id);
				super2 = class2.getDirectSuperclasses();
				sub2 = class2.getDirectSubclasses();
				scope2  = index.getScope4Identifier_Condifence(id);
				
				writer.write(class1.getEntityName() + " | " + class2.getEntityName() + " ");
				
				count = 0;
				for (int p : super1)
					for (int q : super2)
						if (mapping_extractor.isMappingInGoldStandard(p, q))
							++count;
				writer.write(count + " ");
				
				count = 0;
				
				for (int p : sub1)
					for (int q : sub2)
						if (mapping_extractor.isMappingInGoldStandard(p, q))
							++count;
				writer.write(count + " ");
				
				count = 0;
				for (int p : scope1)
					for (int q : scope2)
						if (mapping_extractor.isMappingInGoldStandard(p, q))
							++count;
				writer.write(count + "\n");
			}
		}
		
		writer.close();
	}
	
	
	
	
	
	
	
	
	
	double precision=0.0;
	double recall=0.0;
	double fmeasure=0.0;
	
	public double getPrecision(){
		return precision;
	}
	public double getRecall(){
		return recall;
	}
	public double getFmeasure(){
		return fmeasure;
	}
	
	
	
	public Set<MappingObjectStr> getLogMapMappings(){
		
		mapping_extractor.setStringAnchors(
				Parameters.output_class_mappings,
				Parameters.output_prop_mappings,
				Parameters.output_instance_mappings);
		
		return mapping_extractor.getStringAnchors();
	}
	
	
	
	private void getPrecisionAndRecallMappings() throws Exception{

		
		Set <MappingObjectStr> intersection;
		
		
		//double precision;
		//double recall;
		
		mapping_extractor.setStringAnchors(
				Parameters.output_class_mappings,
				Parameters.output_prop_mappings,
				Parameters.output_instance_mappings);
		
		
		StatisticsManager.setMFinal(mapping_extractor.getStringAnchors().size());
		LogOutput.print("MAPPINGS: " + mapping_extractor.getStringAnchors().size());
		
		
		//ALL UMLS MAPPINGS
		intersection=new HashSet<MappingObjectStr>(mapping_extractor.getStringAnchors());
		intersection.retainAll(mappings_gs);
		
		StatisticsManager.setGoodMFinal(intersection.size());
		
		
		precision=((double)intersection.size())/((double)mapping_extractor.getStringAnchors().size());
		recall=((double)intersection.size())/((double)mappings_gs.size());

		//String dir = "/auto/users/yzhou/LogMapStuff/Test/FMA2NCI/";
		String dir = logmap_mappings_path;
		BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(dir + "_itersection.txt")));
		for (MappingObjectStr obj : intersection)
			writer.write(obj.getIRIStrEnt1() + "|" + obj.getIRIStrEnt2() + " " + obj.getConfidence() + "\n");
		writer.close();

		writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(dir + "_add_bad.txt")));
		for (MappingObjectStr obj : mapping_extractor.getStringAnchors())
			if (!intersection.contains(obj))
				writer.write(obj.getIRIStrEnt1() + "|" + obj.getIRIStrEnt2() + " " + obj.getConfidence() + "\n");
		writer.close();

		writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(dir + "_lose_good.txt")));
		for (MappingObjectStr obj : mappings_gs)
			if (!intersection.contains(obj))
				writer.write(obj.getIRIStrEnt1() + "|" + obj.getIRIStrEnt2() + "\n");
		writer.close();

		
		fmeasure=(2*recall*precision)/(precision+recall);
		
		LogOutput.printAlways("WRT GS MAPPINGS");
		LogOutput.printAlways("\tPrecision Mappings: " + precision);
		LogOutput.printAlways("\tRecall Mapping: " + recall);
		LogOutput.printAlways("\tF measure: " + (2*recall*precision)/(precision+recall));
		
		LogOutput.print("WRT GS MAPPINGS");
		LogOutput.print("\tPrecision Mappings: " + precision);
		LogOutput.print("\tRecall Mapping: " + recall);
		LogOutput.print("\tF measure: " + (2*recall*precision)/(precision+recall));
		
		
		
		//
		/*getPrecisionandRecallMappings4Subset(0.95);
		getPrecisionandRecallMappings4Subset(0.80);
		getPrecisionandRecallMappings4Subset(0.50);
		getPrecisionandRecallMappings4Subset(0.20);
		*/
		
		Set <MappingObjectStr> difference;
        difference=new HashSet<MappingObjectStr>(mappings_gs);
        difference.removeAll(mapping_extractor.getStringAnchors());
        //LogOutput.print("Difference in GS: " + difference.size());
        LogOutput.printAlways("Difference in GS: " + difference.size());
        if (difference.size()<250){
	        for (MappingObjectStr mapping : difference){
	        	LogOutput.print("\t" + mapping.getIRIStrEnt1() + "--" + mapping.getIRIStrEnt2());
	        }
		}
        Set <MappingObjectStr> difference2;
        difference2=new HashSet<MappingObjectStr>(mapping_extractor.getStringAnchors());
        difference2.removeAll(mappings_gs);
        //LogOutput.print("Difference in Candidates: " + difference2.size());
        LogOutput.printAlways("Difference in Candidates: " + difference2.size());
        if (difference2.size()<100){
	        for (MappingObjectStr mapping : difference2){
	        	LogOutput.print("\t" + mapping.getIRIStrEnt1() + "--" + mapping.getIRIStrEnt2());
	        }
        }
        
        
        StatisticsManager.setMMissing(difference.size());
               
        
       
      
	}
	
	
	
	private void clearStructures(){
		
		//TODO
		
		
		
	}
	
	
	
	
	
	
	private void outputInstaceMappings4Evaluation() throws Exception{
		
		//Only exact mappings (this method is only for statistics purposes)
		String pref = "";
		
		FlatAlignmentFormat good_file = new FlatAlignmentFormat(logmap_mappings_path + pref + "logmap_instance_mappings.txt");
		FlatAlignmentFormat disc1_file = new FlatAlignmentFormat(logmap_mappings_path + pref + "discarded1_instance_mappings.txt");
		FlatAlignmentFormat disc2_file = new FlatAlignmentFormat(logmap_mappings_path + pref + "discarded2_instance_mappings.txt");
		FlatAlignmentFormat incomp_file = new FlatAlignmentFormat(logmap_mappings_path + pref + "incompatible_instance_mappings.txt");
		
		int type;
		
		for (int ide1 : mapping_extractor.getInstanceMappings4OutputType().keySet()) {
		
			for (int ide2 : mapping_extractor.getInstanceMappings4OutputType().get(ide1).keySet()){
			
				
				type = mapping_extractor.getInstanceMappings4OutputType().get(ide1).get(ide2);
				
				
				if (type==0){
				
					good_file.addInstanceMapping2Output(
							index.getIRIStr4IndividualIndex(ide1), 
							index.getIRIStr4IndividualIndex(ide2),
							"=", 
							mapping_extractor.getISUB4InstanceMapping(ide1, ide2),
							mapping_extractor.getCompFactor4InstanceMapping(ide1, ide2),
							mapping_extractor.getScope4InstanceMapping(ide1, ide2));
					
				}
				else if (type==1){
				
					disc1_file.addInstanceMapping2Output(
							index.getIRIStr4IndividualIndex(ide1), 
							index.getIRIStr4IndividualIndex(ide2),
							"=", 
							mapping_extractor.getISUB4InstanceMapping(ide1, ide2),
							mapping_extractor.getCompFactor4InstanceMapping(ide1, ide2),
							mapping_extractor.getScope4InstanceMapping(ide1, ide2));
					
				}
				else if (type==2){
				
					disc2_file.addInstanceMapping2Output(
							index.getIRIStr4IndividualIndex(ide1), 
							index.getIRIStr4IndividualIndex(ide2),
							"=", 
							mapping_extractor.getISUB4InstanceMapping(ide1, ide2),
							mapping_extractor.getCompFactor4InstanceMapping(ide1, ide2),
							mapping_extractor.getScope4InstanceMapping(ide1, ide2));
					
				}
				else if (type==3){
				
					incomp_file.addInstanceMapping2Output(
							index.getIRIStr4IndividualIndex(ide1), 
							index.getIRIStr4IndividualIndex(ide2),
							"=", 
							mapping_extractor.getISUB4InstanceMapping(ide1, ide2),
							mapping_extractor.getCompFactor4InstanceMapping(ide1, ide2),
							mapping_extractor.getScope4InstanceMapping(ide1, ide2));
					
				}
				
				
				
				
			}
			
		}
		
		good_file.saveOutputFile();
		disc1_file.saveOutputFile();
		disc2_file.saveOutputFile();
		incomp_file.saveOutputFile();
		
		
	}
		
		
	
	
	
	
	
	private void outputMappings(Map<Integer, Set<Integer>> mappings, String fileName) throws Exception{
		
		//Only exact mappings (this method is only for statistics purposes)
		
		FlatAlignmentFormat outputFile = new FlatAlignmentFormat(fileName);
		
		LogOutput.print(fileName);
		
		String inGS;
		
		for (int idea : mappings.keySet()){
			
			for (int ideb : mappings.get(idea)){
				
				if (idea<ideb){
					
					
					if (mapping_extractor.isMappingInGoldStandard(idea, ideb)){
						inGS="YES";
					}
					else{
						inGS="NO";
					}
					
					//Onto 1 to onto2
					//if (mapping_extractor.hasWeakMappingSim(idea, ideb)){//Only weak mappings
						outputFile.addClassMapping2Output(
								index.getName4ConceptIndex(idea), 
								index.getName4ConceptIndex(ideb),
								Utilities.EQ, 
								mapping_extractor.getSimWeak4Mapping2(idea, ideb),
								mapping_extractor.extractScopeAll4Mapping(idea, ideb) +"|"+ 
								mapping_extractor.extractISUB4Mapping(idea, ideb) +"|"+ inGS);
					//}					
				}
				
			}
			
		}
		

		
		outputFile.saveOutputFile();
		
		
		
	}
	
	
	
	
	private void orderedOutputMappings(boolean include_discarded) throws Exception{
		
		TreeSet<MappingObjectInteractivity> ordered_output_mappings = 
				new TreeSet<MappingObjectInteractivity>(new ComparatorConfidence());
		
		
		MappingObjectInteractivity mapping;
		
		Iterator<MappingObjectInteractivity> it;
		
		WriteFile writer;
		
		String inGS;
		
		
		//ORDER ANCHORS
		for (int idea : mapping_extractor.getAnchors().keySet()){
			
			for (int ideb : mapping_extractor.getAnchors().get(idea)){
				
				if (idea<ideb){
					
					ordered_output_mappings.add(new MappingObjectInteractivity(idea, ideb));
					
				}
			}
		}
		
		//SAVE ANCHORS
		writer = new WriteFile("/usr/local/data/DataUMLS/UMLS_Onto_Versions/LUCADA/ordered_output_mappings_NCI2LUCADA.txt");
		
		
		writer.writeLine(
				"Label 1" + "|" +
				"Label 2" + "|" +
				"IRI 1" + "|" +
				"IRI 2" + "|" +
				"Confidence" + "|" +
				"ISUB (Lex. Sim.)"  + "|" +
				"Scope" + "|" +
				"IN GS"
				);
		
		it = ordered_output_mappings.descendingIterator();
						
		while (it.hasNext()){
			
			mapping = it.next();
			
			if (mapping_extractor.isMappingInGoldStandard(mapping.getIdentifierOnto1(), mapping.getIdentifierOnto2())){
				inGS="YES";
			}
			else{
				inGS="NO";
			}
			
			writer.writeLine(
					index.getLabel4ConceptIndex(mapping.getIdentifierOnto1()) + "|" +
					index.getLabel4ConceptIndex(mapping.getIdentifierOnto2()) + "|" +
					index.getIRIStr4ConceptIndex(mapping.getIdentifierOnto1()) + "|" +
					index.getIRIStr4ConceptIndex(mapping.getIdentifierOnto2()) + "|" +
					getConfidence(mapping) + "|" +
					mapping_extractor.extractISUB4Mapping(mapping.getIdentifierOnto1(), mapping.getIdentifierOnto2())  + "|" +
					mapping_extractor.extractScopeAnchors4Mapping(mapping.getIdentifierOnto1(), mapping.getIdentifierOnto2()) + "|" +
					inGS
					);
		
			
			
		}
		
		writer.closeBuffer();
		
		
		ordered_output_mappings.clear();
		
		if (include_discarded){
		
			//ORDER DISCARDED
			/*for (int idea : mapping_extractor.getDircardedAnchors().keySet()){
				
				for (int ideb : mapping_extractor.getDircardedAnchors().get(idea)){
					
					if (idea<ideb){
						
						ordered_output_mappings.add(new MappingObjectInteractivity(idea, ideb));
						
					}
				}
			}*/
			
			for (int idea : mapping_extractor.getHardDircardedAnchors().keySet()){
				
				for (int ideb : mapping_extractor.getHardDircardedAnchors().get(idea)){
					
					if (idea<ideb){
						
						ordered_output_mappings.add(new MappingObjectInteractivity(idea, ideb));
						
					}
				}
			}
			
			
			//SAVE DISCARDED
			writer = new WriteFile("/usr/local/data/DataUMLS/UMLS_Onto_Versions/LUCADA/ordered_harddiscarded_mappings_NCI2LUCADA.txt");
			
			it = ordered_output_mappings.descendingIterator();
			
			writer.writeLine(
					"Label 1" + "|" +
					"Label 2" + "|" +
					"IRI 1" + "|" +
					"IRI 2" + "|" +
					"Confidence" + "|" +
					"ISUB (Lex. Sim.)"  + "|" +
					"Scope"  + "|" +
					"IN GS"
					);
			
			while (it.hasNext()){
				
				mapping = it.next();
				
				if (mapping_extractor.isMappingInGoldStandard(mapping.getIdentifierOnto1(), mapping.getIdentifierOnto2())){
					inGS="YES";
				}
				else{
					inGS="NO";
				}
				
				writer.writeLine(
						index.getLabel4ConceptIndex(mapping.getIdentifierOnto1()) + "|" +
						index.getLabel4ConceptIndex(mapping.getIdentifierOnto2()) + "|" +
						index.getIRIStr4ConceptIndex(mapping.getIdentifierOnto1()) + "|" +
						index.getIRIStr4ConceptIndex(mapping.getIdentifierOnto2()) + "|" +
						getConfidence(mapping) + "|" +
						mapping_extractor.extractISUB4Mapping(mapping.getIdentifierOnto1(), mapping.getIdentifierOnto2())  + "|" +
						mapping_extractor.extractScopeAnchors4Mapping(mapping.getIdentifierOnto1(), mapping.getIdentifierOnto2())+ "|" +
						inGS
						);
			
				
			}
			
			writer.closeBuffer();
			
			
			ordered_output_mappings.clear();
			
		}		
		
		
		
		
	}


	
	
	
	public Map<Integer, Set<Integer>> getClassMappings(){
		return mapping_extractor.getAnchors();
		
	}
	
	public int getDirClassMapping(int ide1, int ide2){
		return mapping_extractor.getDirMapping(ide1, ide2);
	}
	
	
	public String getIRIOntology1(){
		return onto_process1.getOntoIRI();
	}
	
	public String getIRIOntology2(){
		return onto_process2.getOntoIRI();
	}

	public Map<Integer, Integer> getDataPropMappings(){
		return mapping_extractor.getDataPropertyAnchors();	
	}
	
	
	public Map<Integer, Integer> getObjectPropMappings(){
		return mapping_extractor.getObjectPropertyAnchors();	
	}
	
	public Map<Integer, Set<Integer>> getInstanceMappings(){
		return mapping_extractor.getInstanceMappings();	
	}
	
	
	
	public double getConfidence4ConceptMapping(int ide1, int ide2){
		return mapping_extractor.getConfidence4Mapping(ide1, ide2);
	}
	
	public double getConfidence4DataPropConceptMapping(int ide1, int ide2){
		return mapping_extractor.getConfidence4DataPropertyAnchor(ide1, ide2);
	}
	
	public double getConfidence4ObjectPropConceptMapping(int ide1, int ide2){
		return mapping_extractor.getConfidence4ObjectPropertyAnchor(ide1, ide2);
	}
	
	public double getConfidence4InstanceMapping(int ide1, int ide2){
		return mapping_extractor.getConfidence4InstanceMapping(ide1, ide2);
	}
	
	
	public String getIRI4ConceptIdentifier(int ide){
		return index.getIRIStr4ConceptIndex(ide);
	}
	
	public String getIRI4DataPropIdentifier(int ide){
		return index.getIRIStr4DataPropIndex(ide);
	}
	
	public String getIRI4ObjectPropIdentifier(int ide){
		return index.getIRIStr4ObjPropIndex(ide);
	}
	
	public String getIRI4InstanceIdentifier(int ide){
		return index.getIRIStr4IndividualIndex(ide);
	}
	
	
	public void clearIndexStructures(){
		index.clearTaxonomicalStructures();
	}
	
	
	
	
	
	
	
	static int ontopair = 0;
	static boolean yujiao = false;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		//long init, fin;
		
		String uri1;
		String uri2;
		String uri1_out;
		String uri2_out;
		String gs_mappings; 
		String logmap_mappings;
		String module_gs1;
		String module_gs2;
		
		String dir;
		//String interactivityFile;
		
		
		boolean useInteractivity;
		boolean useHeuristics;
		boolean orderQuestions;
		
		
		boolean record_interactivity;
		
		int user_error;
		
		boolean ask_everything;
		
		boolean evaluate_impact=false;
		
		Parameters.print_output = false;
		Parameters.print_output_always = true;
		
		boolean small = true;
		
		
		/*public static int FMA2NCI=0;
		public static int FMA2SNOMED=1;
		public static int SNOMED2NCI=2;
		public static int MOUSE2HUMAN=5;*/
		
		if (args.length!=7){
			ontopair=Utilities.MOUSE2HUMAN;
			
			ontopair=Utilities.FMA2NCI;
			//ontopair=Utilities.NCIpeque2FMA;
			//ontopair=Utilities.NCI2FMApeque;
			//ontopair=Utilities.SNOMED2LUCADA;
			//ontopair=Utilities.NCI2LUCADA;
			//ontopair=Utilities.FMA2LUCADA;

			//ontopair=Utilities.FMA2SNOMED;
			//ontopair=Utilities.SNOMED2NCI;
			
			//ontopair=Utilities.LIBRARY;
			//ontopair=Utilities.CONFERENCE;
			ontopair=Utilities.INSTANCE;
	
			//ontopair=20; //other
	
			
			
			useInteractivity=false;
			useHeuristics=false;
			orderQuestions=false;
			
			//Anchors plus toask
			ask_everything = false;
			
			record_interactivity=false;
			user_error=0;
			
			
			//interactivityFile = "simulationInteractivityFMA2NCI_noheuristics.txt";
			//interactivityFile = "simulationInteractivityFMA2NCI_heuristics_no_order.txt";
			
			
		}
		else{
			ontopair=Integer.valueOf(args[0]);
	
			useInteractivity=Boolean.valueOf(args[1]);
			useHeuristics=Boolean.valueOf(args[2]);
			orderQuestions=Boolean.valueOf(args[3]);
			user_error = Integer.valueOf(args[4]);
			ask_everything =Boolean.valueOf(args[5]);
			record_interactivity=Boolean.valueOf(args[6]);
			
			
			LogOutput.print(args[0] + " " + args[1] + " " + args[2] + " " + args[3]);
			
		}
		
		String path = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/oaei_2013/";
		String irirootpath = "file:" + path;
		
		
		if (ontopair==Utilities.FMA2NCI){
			
			//interactivityFile = "simulationInteractivityFMA2NCI.txt";
			
			
			if (yujiao){
				dir = "/auto/users/yzhou/LogMapStuff/Test/FMA2NCI/";

				uri1= "file:" + dir + "FMADL_2_0_with_synonyms.owl";		
				uri2= "file:" + dir + "NCI_Thesaurus_08.05d_with_synonyms.owl";
				
				uri1_out= "file:" + dir + "FMA_overlapping_nci.owl";
				uri2_out= "file:" + dir + "NCI_overlapping_fma.owl";
				
				
				gs_mappings = dir + "onto_mappings_FMA_NCI_dirty.txt";
				//logmap_mappings = "file:" + dir + "FMA2NCI_logmap_mappings.owl";
				logmap_mappings = dir + "FMA2NCI_logmap2_";
			}
			else {
				
				
				//uri1= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/FMADL_2_0_with_synonyms.owl";		
				//uri2= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/NCI_Thesaurus_08.05d_with_synonyms.owl";
				
				//String irirootpath = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/oaei_2012/fma2nci/";
				
				if (small){
					uri1 = irirootpath + "oaei2013_FMA_small_overlapping_nci.owl";
					uri2 = irirootpath + "oaei2013_NCI_small_overlapping_fma.owl";
				}				
				else{
					uri1 = irirootpath + "oaei2013_FMA_whole_ontology.owl";
					uri2 = irirootpath + "oaei2013_NCI_whole_ontology.owl";
				}
				//uri1= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2nci_dataset/oaei2012_FMA_whole_ontology.owl";		
				//uri2= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2nci_dataset/oaei2012_NCI_whole_ontology.owl";
				
				//uri1= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2nci_dataset/oaei2012_FMA_big_overlapping_nci.owl";		
				//uri2= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2nci_dataset/oaei2012_NCI_big_overlapping_fma.owl";
				
				//uri1= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2nci_dataset/FMA_bigoverlapping_nci.owl";		
				//uri2= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2nci_dataset/NCI_bigoverlapping_fma.owl";
				
				
				//uri1= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/FMA_overlapping_nci.owl";
				//uri2= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/NCI_overlapping_fma.owl";
				
				
				//old
				//uri1=  "http://seals-test.sti2.at/tdrs-web/testdata/persistent/cf0378d9-da30-4b58-b937-192028ed4961/1dc20ac1-400c-4c01-afc1-1e0e80f16ace/suite/original-fma-c-nci-c/component/source";
				//uri2=  "http://seals-test.sti2.at/tdrs-web/testdata/persistent/cf0378d9-da30-4b58-b937-192028ed4961/1dc20ac1-400c-4c01-afc1-1e0e80f16ace/suite/original-fma-c-nci-c/component/target";
				
				//uri1=  "http://seals-test.sti2.at/tdrs-web/testdata/persistent/cf0378d9-da30-4b58-b937-192028ed4961/a9fe4a9b-54c0-4ecf-bdfe-f405cf72193d/suite/original-fma-c-nci-c/component/source";
				//uri2=  "http://seals-test.sti2.at/tdrs-web/testdata/persistent/cf0378d9-da30-4b58-b937-192028ed4961/a9fe4a9b-54c0-4ecf-bdfe-f405cf72193d/suite/original-fma-c-nci-c/component/target";
				
				//uri1 = "http://seals-test.sti2.at/tdrs-web/testdata/persistent/cf0378d9-da30-4b58-b937-192028ed4961/a9fe4a9b-54c0-4ecf-bdfe-f405cf72193d/suite/original-fma-a-nci-a/component/source";
				//uri2 = "http://seals-test.sti2.at/tdrs-web/testdata/persistent/cf0378d9-da30-4b58-b937-192028ed4961/a9fe4a9b-54c0-4ecf-bdfe-f405cf72193d/suite/original-fma-a-nci-a/component/target";
				
				
				uri1_out= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/FMA_overlapping_nci.owl";
				uri2_out= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/NCI_overlapping_fma.owl";
				
				//uri1_out= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/FMA_bigoverlapping_nci.owl";
				//uri2_out= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/NCI_bigoverlapping_fma.owl";
				
				//gs_mappings = "/usr/local/data/DataUMLS/UMLS_source_data/onto_mappings_FMA_NCI_cleantDG.txt";
				
				gs_mappings = path + "oaei2013_FMA2NCI_repaired_UMLS_mappings.txt";
				
				//gs_mappings = "/usr/local/data/DataUMLS/UMLS_source_data/onto_mappings_FMA_NCI_dirty.txt";
				logmap_mappings = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/ISWC_LogMap0.9_Mappings/FMA2NCI_logmap_mappings.owl";
				//module_gs1 = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/FMA2NCI/FMA_overlappingUMLScleant_nci.owl";
				//module_gs2 = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/FMA2NCI/NCI_overlappingUMLScleant_fma.owl";
				
				logmap_mappings = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/LogMap2_Mappings/FMA2NCI_logmap2_Output";
			}
																				
		}
		else if (ontopair==Utilities.FMA2SNOMED){
			//interactivityFile = "simulationInteractivityFMA2SNMD.txt";
			
			if (yujiao){
				
				dir = "/auto/users/yzhou/LogMapStuff/Test/FMA2SNOMED/";
				
				uri1= "file:" + dir + "FMA_bigoverlapping_snmd.owl";		
				uri2= "file:" + dir + "SNMD_bigoverlapping_fma.owl";
				
				uri1_out= "file:" + dir + "FMA_overlapping_snmd.owl";
				uri2_out= "file:" + dir + "SNMD_overlapping_fma.owl";
				
				gs_mappings = dir + "onto_mappings_FMA_SNOMED_dirty.txt";
				
				logmap_mappings = dir + "FMA2SNOMED_logmap2_";
				
			}
			else {
				//uri1= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/FMADL_2_0_with_synonyms.owl";
				//uri1= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2nci_dataset/oaei2012_FMA_whole_ontology.owl";
				//uri2= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/snomed20090131_replab.owl";
								
				//uri1= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/FMA_bigoverlapping_snmd.owl";
				//uri2= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/SNMD_bigoverlapping_fma.owl";
				
				//uri1= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2snmd_dataset/oaei2012_FMA_big_overlapping_snomed.owl";		
				//uri2= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2snmd_dataset/oaei2012_SNOMED_big_overlapping_fma.owl";
				
				//String rootpath_fma2snomed = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/oaei_2012/fma2snmd/";
				
				//uri1 = rootpath_fma2snomed + "oaei2012_FMA_small_overlapping_snomed.owl";
				//uri2 = rootpath_fma2snomed + "oaei2012_SNOMED_small_overlapping_fma.owl";
				
				if (small){
					uri1 = irirootpath + "oaei2013_FMA_small_overlapping_snomed.owl";
					uri2 = irirootpath + "oaei2013_SNOMED_small_overlapping_fma.owl";
				}
				else{
					uri1 = irirootpath + "oaei2013_FMA_whole_ontology.owl";
					uri2 = irirootpath + "oaei2013_SNOMED_extended_overlapping_fma_nci.owl";
				}
				
				//uri1_out= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/FMA_bigoverlapping_snmd.owl";
				//uri2_out= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/SNMD_bigoverlapping_fma.owl";
				
				uri1_out= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/FMA_overlapping_snmd.owl";
				uri2_out= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/SNMD_overlapping_fma.owl";
				
				//gs_mappings = "/usr/local/data/DataUMLS/UMLS_source_data/onto_mappings_FMA_SNOMED_cleantDG.txt";
				//gs_mappings = "/usr/local/data/DataUMLS/UMLS_source_data/onto_mappings_FMA_SNOMED_dirty.txt";
				gs_mappings = path + "oaei2013_FMA2SNOMED_repaired_UMLS_mappings.txt";
				
				logmap_mappings = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/ISWC_LogMap0.9_Mappings/FMA2SNMD_logmap_mappings.owl";
				//module_gs1 = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/FMA2SNOMED/FMA_overlappingUMLScleant_snmd.owl";
				//module_gs2 = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/FMA2SNOMED/SNMD_overlappingUMLScleant_fma.owl";
				
				logmap_mappings = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/LogMap2_Mappings/FMA2SNOMED_logmap2_Output";
			}
				
					
		}
		else if (ontopair==Utilities.SNOMED2NCI){
			
			//interactivityFile = "simulationInteractivitySNMD2NCI.txt";
			
			if (yujiao){
			
				dir = "/auto/users/yzhou/LogMapStuff/Test/SNOMED2NCI/";
				uri1= "file:" + dir + "SNMD_bigoverlapping_nci.owl";		
				uri2= "file:" + dir + "NCI_bigoverlapping_snmd.owl";
				
				uri1_out= "file:" + dir + "SNMD_bigoverlapping_nci.owl";
				uri2_out= "file:" + dir + "NCI_bigoverlapping_snmd.owl";
				
				
				
				gs_mappings = dir + "onto_mappings_SNOMED_NCI_dirty.txt";
				
				logmap_mappings = "file:" + dir + "SNMD2NCI_logmap_mappings.owl";
				
				logmap_mappings = dir + "SNOMED2NCI_logmap2_";
				
				
				
			}
			else {
				
				//uri1= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/snomed20090131_replab.owl";		
				//uri2= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/NCI_Thesaurus_08.05d_with_synonyms.owl";//
				//uri2= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2nci_dataset/oaei2012_NCI_whole_ontology.owl";
				
				if (small){
					uri1 = irirootpath + "oaei2013_SNOMED_small_overlapping_nci.owl";
					uri2 = irirootpath + "oaei2013_NCI_small_overlapping_snomed.owl";
				}
				else{
					uri1 = irirootpath + "oaei2013_SNOMED_extended_overlapping_fma_nci.owl";
					uri2 = irirootpath + "oaei2013_NCI_whole_ontology.owl";
				}
				
				//uri1_out= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/SNMD_bigoverlapping_nci.owl";
				//uri2_out= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/NCI_bigoverlapping_snmd.owl";
				uri1_out= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/SNMD_overlapping_nci.owl";
				uri2_out= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/NCI_overlapping_snmd.owl";
				
				
				//gs_mappings = "/usr/local/data/DataUMLS/UMLS_source_data/onto_mappings_SNOMED_NCI_cleantDG.txt";
				//gs_mappings = "/usr/local/data/DataUMLS/UMLS_source_data/onto_mappings_SNOMED_NCI_dirty.txt";
				gs_mappings = path + "oaei2013_SNOMED2NCI_repaired_UMLS_mappings.txt";
				
				
				
				logmap_mappings = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/ISWC_LogMap0.9_Mappings/SNMD2NCI_logmap_mappings.owl";
				//module_gs1 = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/SNOMED2NCI/SNMD_overlappingUMLScleant_nci.owl";
				//module_gs2 = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/SNOMED2NCI/NCI_overlappingUMLScleant_snmd.owl";
				
				logmap_mappings = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/LogMap2_Mappings/SNOMED2NCI_logmap2_Output";
			}
			
			
		}
		else if (ontopair==Utilities.MOUSE2HUMAN){
			
			//interactivityFile = "simulationInteractivityMouse.txt";
			
			//uri1= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/Anatomy/mouse_anatomy_2010.owl";
			//uri2= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/Anatomy/nci_anatomy_2010.owl";
			
			uri1= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/Anatomy/2012/mouse2012.owl";
			uri2= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/Anatomy/2012/human2012.owl";
			
			uri1_out= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/Anatomy/mouse_bigoverlapping_nci.owl";
			uri2_out= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/Anatomy/NCI_bigoverlapping_mouse.owl";
			
			logmap_mappings = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/ISWC_LogMap0.9_Mappings/MOUSE2NCIAn_logmap_mappings.owl";
			//gs_mappings = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/Anatomy/GSAll_Anatomy_2010.txt";
			
			gs_mappings = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/Anatomy/2012/reference2012.txt";
			
			
			logmap_mappings = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/LogMap2_Mappings/Mouse_logmap2_Output";
			
		}
		else  if (ontopair==Utilities.NCIpeque2FMA){
			
			//interactivityFile = "simulationInteractivityNCIPequeFMA.txt";
					
			uri1= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2nci_dataset/oaei2012_FMA_whole_ontology.owl";
			uri2= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2nci_dataset/oaei2012_NCI_small_overlapping_fma.owl";
					
					//Not used
					uri1_out= "";
					uri2_out= "";
					
										
					gs_mappings = "/usr/local/data/DataUMLS/UMLS_source_data/onto_mappings_FMA_NCI_cleantDG.txt";
					//gs_mappings = "/usr/local/data/DataUMLS/UMLS_source_data/onto_mappings_FMA_NCI_dirty.txt";
					logmap_mappings = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/ISWC_LogMap0.9_Mappings/FMA2NCI_logmap_mappings.owl";
					//module_gs1 = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/FMA2NCI/FMA_overlappingUMLScleant_nci.owl";
					//module_gs2 = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/FMA2NCI/NCI_overlappingUMLScleant_fma.owl";
					
					logmap_mappings = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/LogMap2_Mappings/FMA2NCI_logmap2_Output";
				
		}
		else if (ontopair==Utilities.NCI2FMApeque){
			
			//interactivityFile = "simulationInteractivityNCIFMAPeque.txt";
					
			uri1= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2nci_dataset/oaei2012_FMA_small_overlapping_nci.owl";
			uri2= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2nci_dataset/oaei2012_NCI_whole_ontology.owl";
					
					//Not used
					uri1_out= "";
					uri2_out= "";
					
										
					gs_mappings = "/usr/local/data/DataUMLS/UMLS_source_data/onto_mappings_FMA_NCI_cleantDG.txt";
					//gs_mappings = "/usr/local/data/DataUMLS/UMLS_source_data/onto_mappings_FMA_NCI_dirty.txt";
					logmap_mappings = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/ISWC_LogMap0.9_Mappings/FMA2NCI_logmap_mappings.owl";
					//module_gs1 = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/FMA2NCI/FMA_overlappingUMLScleant_nci.owl";
					//module_gs2 = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/FMA2NCI/NCI_overlappingUMLScleant_fma.owl";
					
					logmap_mappings = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/LogMap2_Mappings/FMA2NCI_logmap2_Output";
				
		}
		
		else if (ontopair==Utilities.SNOMED2LUCADA){
			
			//interactivityFile = "simulationInteractivitySNMD2LUCADA.txt";
			
			//uri1= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/snomed20090131_replab.owl";
			//uri1= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/LUCADA/snomed20110131_replab.owl";
			uri1= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/LUCADA/snomed20110131_replab_with_ids.owl";
			uri2= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/LUCADA/LUCADAOntology15September2011.owl";
					
			//Not used
			uri1_out= "";
			uri2_out= "";
					
										
			gs_mappings = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/LUCADA/references_to_snomed.txt";
			//gs_mappings = "/usr/local/data/DataUMLS/UMLS_source_data/onto_mappings_FMA_NCI_dirty.txt";
			logmap_mappings = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/ISWC_LogMap0.9_Mappings/FMA2NCI_logmap_mappings.owl";
			//module_gs1 = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/FMA2NCI/FMA_overlappingUMLScleant_nci.owl";
			//module_gs2 = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/FMA2NCI/NCI_overlappingUMLScleant_fma.owl";
					
			logmap_mappings = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/LogMap2_Mappings/SNOMED2LUCADA_logmap2_Output";
			
			
		}
		
		else if (ontopair==Utilities.FMA2LUCADA){
			
			//interactivityFile = "simulationInteractivityNCI2LUCADA.txt";
			
			uri1= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2nci_dataset/oaei2012_FMA_whole_ontology.owl";
			uri2= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/LUCADA/LUCADAOntology15September2011.owl";
					
			//Not used
			uri1_out= "";
			uri2_out= "";
					
										
			gs_mappings = "/usr/local/data/DataUMLS/UMLS_source_data/onto_mappings_FMA_NCI_cleantDG.txt"; //??
			//gs_mappings = "/usr/local/data/DataUMLS/UMLS_source_data/onto_mappings_FMA_NCI_dirty.txt";
			logmap_mappings = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/ISWC_LogMap0.9_Mappings/FMA2NCI_logmap_mappings.owl";
			//module_gs1 = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/FMA2NCI/FMA_overlappingUMLScleant_nci.owl";
			//module_gs2 = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/FMA2NCI/NCI_overlappingUMLScleant_fma.owl";
					
			logmap_mappings = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/LogMap2_Mappings/FMA2LUCADA_logmap2_Output";
			
			//overlapping=false;
			
			
		}
		
		else if (ontopair==Utilities.NCI2LUCADA){
			
			//interactivityFile = "simulationInteractivityNCI2LUCADA.txt";
			
			uri1= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2nci_dataset/oaei2012_NCI_whole_ontology.owl";
			uri2= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/LUCADA/LUCADAOntology15September2011.owl";
					
			//Not used
			uri1_out= "";
			uri2_out= "";
					
										
			gs_mappings = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/LUCADA/references_to_nci.txt";
			//gs_mappings = "/usr/local/data/DataUMLS/UMLS_source_data/onto_mappings_FMA_NCI_dirty.txt";
			logmap_mappings = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/ISWC_LogMap0.9_Mappings/FMA2NCI_logmap_mappings.owl";
			//module_gs1 = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/FMA2NCI/FMA_overlappingUMLScleant_nci.owl";
			//module_gs2 = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/FMA2NCI/NCI_overlappingUMLScleant_fma.owl";
					
			logmap_mappings = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/LogMap2_Mappings/NCI2LUCADA_logmap2_Output";
			
			//overlapping=false;
			
			
		}
		
		else if (ontopair==Utilities.LIBRARY){
		
			
			uri1= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/Library/stw.owl";
			uri2= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/Library/thesoz.owl";
					
			//Not used
			uri1_out= "";
			uri2_out= "";
					
										
			//gs_mappings = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/Library/empty_gs.txt";
			gs_mappings = "/home/ernesto/OM_OAEI/OAEI_2013_new_stuff/library_referenceAll.txt";
			
			logmap_mappings = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/LogMap2_Mappings/Library_logmap2_Output";
			
			//overlapping=false;
			
			
		}
		
		
		else if (ontopair==Utilities.CONFERENCE){
		
			
			//uri1= "file:/usr/local/data/ConfOntosOAEI/cmt.owl";
			//uri2= "file:/usr/local/data/ConfOntosOAEI/cocus.owl";
			
			uri1= "file:/usr/local/data/ConfOntosOAEI/paperdyne.owl";
			uri2= "file:/usr/local/data/ConfOntosOAEI/OpenConf.owl";
			
			
			//uri1= "file:/usr/local/data/ConfOntosOAEI2/onto.rdf";
			//uri2= "file:/usr/local/data/ConfOntosOAEI2/onto.rdf";
			
			//Not used
			uri1_out= "";
			uri2_out= "";
					
										
			gs_mappings = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/Library/empty_gs.txt";

			logmap_mappings = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/LogMap2_Mappings/Conference_logmap2_Output";
			
			//overlapping=false;
			
			
		}
		
		
		
		else if (ontopair==Utilities.INSTANCE){
		
			
			uri1= "file:/usr/local/data/Instance/sandbox/sandbox.owl";
			//uri2= "file:/usr/local/data/Instance/sandbox/sandbox.owl";
			uri2= "file:/usr/local/data/Instance/sandbox/002/sandbox.owl";
			
			uri1= "file:/usr/local/data/Instance/iimb/onto.owl";
			uri2= "file:/usr/local/data/Instance/iimb/070/onto.owl";
			
			uri1 = "file:/usr/local/data/Instance/cristinaStuff/locations/dbpedia6.rdf";
			uri2 = "file:/usr/local/data/Instance/cristinaStuff/locations/nyt3.rdf";
			
			//uri1= "file:/usr/local/data/Instance/sandbox/sandbox.owl";
			//uri2= "file:/usr/local/data/Instance/sandbox/sandbox.owl";
			
			//uri1= "file:/usr/local/data/Instance/iimb/onto.owl";
			//uri2= "file:/usr/local/data/Instance/iimb/onto.owl";
			
			
			
			//Not used
			uri1_out= "";
			uri2_out= "";
					
										
			//gs_mappings = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/Instance/sandbox/empty_gs.txt";

			gs_mappings = "";
			
			logmap_mappings = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/LogMap2_Mappings/Instance_logmap2_Output";
			
			//overlapping=false;
			
			
		}
		else{//OTHER
			

			//uri1= "http://csu6325.cs.ox.ac.uk/ontologies/matching_03_09_2012/xebr_2011_09_12.zip";
			//uri2= "http://csu6325.cs.ox.ac.uk/ontologies/matching_03_09_2012/it_gaap_ci_ese_2011.zip"; //fails

			
			uri1= "http://csu6325.cs.ox.ac.uk/ontologies/matching_03_09_2012/it_gaap.zip";
			uri2= "http://csu6325.cs.ox.ac.uk/ontologies/matching_03_09_2012/xebr_2011_09_12.zip";
			
			
			uri1= "file:/home/ernesto/Desktop/EvalOAEITrack/bencherror/bch4-101.rdf";
			uri2= "file:/home/ernesto/Desktop/EvalOAEITrack/bencherror/bch4-250.rdf";
			
			uri1="file:/usr/local/data/ConfOntosOAEI2/cocus.owl";
			uri2="file:/usr/local/data/ConfOntosOAEI2/cmt.owl";
			
			
			uri1="http://cui.unige.ch/isi/onto/citygml2.0.owl";
			uri2="http://cui.unige.ch/isi/onto/Inspire-TN.owl";
			
			
			uri1 = "http://csu6325.cs.ox.ac.uk/ontologies/matching_29_11_2012/dbpedia_3.8.owl";
			uri2 = "http://csu6325.cs.ox.ac.uk/ontologies/matching_29_11_2012/Wikipedia.zip";
			
			
			
			uri1 = "http://seals-test.sti2.at/tdrs-web/testdata/persistent/biblio-dataset/biblio-dataset-r1/suite/101/component/source";
			uri2 = "http://seals-test.sti2.at/tdrs-web/testdata/persistent/biblio-dataset/biblio-dataset-r1/suite/101/component/target";
		   

			//uri1= "http://csu6325.cs.ox.ac.uk/ontologies/matching_03_09_2012/xebr_2011_09_12.zip";
			//uri2= "http://csu6325.cs.ox.ac.uk/ontologies/matching_03_09_2012/it_gaap_label_en.zip"; //fails

			//uri1= "http://csu6325.cs.ox.ac.uk/ontologies/matching_03_09_2012/it_gaap_verboseLabel_en.zip";
			//uri2= "http://csu6325.cs.ox.ac.uk/ontologies/matching_03_09_2012/xebr_2011_09_12.zip";
			
			
			//Not used
			uri1_out= "";
			uri2_out= "";
					
			gs_mappings = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/Library/empty_gs.txt";
			
			logmap_mappings = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/LogMap2_Mappings/Other_logmap2_Output";
			
			
		}
		
		
		
		
		try{
			//init = Calendar.getInstance().getTimeInMillis();
							
			
			new LogMap2Core(
					uri1, 
					uri2, 
					uri1_out, 
					uri2_out, 
					gs_mappings, 
					logmap_mappings,
					useInteractivity,
					useHeuristics, //should be an input parameter
					orderQuestions,
					user_error,
					ask_everything,
					record_interactivity,
					evaluate_impact);
		
			//fin = Calendar.getInstance().getTimeInMillis();
			//LogOutput.print("TOTAL TIME (s): " + (float)((double)fin-(double)init)/1000.0);
		}
		catch (Exception e){
			e.printStackTrace();
		}
		
	}
	
	
	
	
	private double getConfidence(MappingObjectInteractivity m){
		
		return (mapping_extractor.getConfidence4Mapping(m.getIdentifierOnto1(), m.getIdentifierOnto2()));
		
	}
	
	
	
	/**
	 * @author Ernesto
	 *
	 */
	private class ComparatorConfidence implements Comparator<MappingObjectInteractivity> {
		
		public int compare(MappingObjectInteractivity m1, MappingObjectInteractivity m2) {
			
			if (getConfidence(m1) < getConfidence(m2))
				return -1;
			else
				return 1;
		}
		
	}
	
	

}
