/*******************************************************************************
 * Copyright 2016 by the Department of Computer Science (University of Genova and University of Oxford)
 * 
 *    This file is part of LogMapC an extension of LogMap matcher for conservativity principle.
 * 
 *    LogMapC is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 * 
 *    LogMapC is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 * 
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with LogMapC.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import logmap.LogMapWrapper;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import uk.ac.ox.krr.logmap2.io.LogOutput;

import enumerations.DISJ_CHECK;
import enumerations.OS;
import enumerations.REASONER_KIND;
import enumerations.REPAIR_METHOD;
import enumerations.REPAIR_STRATEGY;
import enumerations.VIOL_KIND;

public class Params {
	
	/* START GENERAL PARAMS */
	public static boolean oaei = false;
	public static OS os = Util.getOS();
	
	// controls the verbosity of the program
	public static int verbosity = -1;
	public static boolean suppressConsole = false;
	public static boolean showFullExceptionTrace = true;
	public static boolean suppressFullReasoningOutput = false;
	public static boolean suppressDirectViolationsOutput = true;
	
	public static void suppressOutputFully(){
		verbosity = -1;
		suppressConsole = true;
		LogOutput.showOutpuLog(false);
		LogOutput.showOutpuLogAlways(false);
	}
	
	// enables some extra consistency checks on the data structures and values
	public static boolean testMode = false;	
	public static final boolean multiThreaded = true;
	public static int NTHREADS = Util.getNumberOfCores(multiThreaded);

	public static boolean removeNontrivialSafeCycles = true;
	public static boolean filterMultipleIfBigSCC = true;
	public static boolean alwaysTestDiagnosis = false;
	public static boolean alwaysFilterMultiple = false;
	public static boolean disableFilterMultiple = false;
	public static String dataFolder;
	
	static {
		dataFolder = FileUtil.readStringFromFile("dataFolder.txt");
		dataFolder = dataFolder.replace("\n", "");
		//"/home/ale/data/"
	}
	/* END GENERAL PARAMS */
	
	/* START LOGMAP PARAMS */
	public static final int violThreshold = (int) Math.pow(10, 4);
	static { 
		LogMapWrapper.setLogMapOutput(false);
	}
	/* END LOGMAP PARAMS */
	
	/* START DAGGER PARAMS */
	public static final int labelDims = 5;
	public static boolean useIndex = false;
	/* END DAGGER PARAMS */
	
	/* START GREEDY PARAMS */
	public static boolean greedyCardOpt = true;
	/* END GREEDY PARAMS */
	
//	/* START STRONG BRIDGES PARAMS */
//	public static final String outDirSB = "sb/";
//	public static final String outGraphSB = outDirSB + "graph";
//	public static final String outGraphSBWebG = outGraphSB + ".webg";
//	// values: DOMINATORS_LT, DOMINATORS_SNCA, HEADERS_FOREST, NAIVE
//	public static SF_Algorithm4SBs algoSB = SF_Algorithm4SBs.DOMINATORS_SNCA;	
//	/* END STRONG BRIDGES PARAMS */
	
	/* START OUT DIR PARAMS */
	public static final String 
			test1OutDir = "test/test1/", 
			test2OutDir = "test/test2/",
			test3OutDir = "test/test3/",
			test4OutDir = "test/test4/",
			test5OutDir = "test/test5/",
			test6OutDir = "test/test6/",
			test7OutDir = "test/test7/",
			testOutDir = "test/test";
	/* END OUT DIR PARAMS */
	
    /* START TEST1 PARAMS */
	public static boolean test1Reference = false;
	public static String test1Exp1OutPathname = test1OutDir + "exp1.text";
//	public static String test1Exp1Header = "NumM NumR MRPrec MRRec MRFme "
//			+ "NumD NumR DRPrec DRRec DRFme "
//			+ "NumDm NumR DmRPrec DmRRec DmRFme "
//			+ "NumD NumRD DRDPrec DRDRec DRDFme "
//			+ "NumD\n";
	/* END TEST1 PARAMS */
	
    /* START TEST2 PARAMS */
	public static boolean test2Resume = false;
	public static boolean test2Parallel = true;
	public static int test2Repetitions = 1;
	public static double test2VMGB = 10;
	/* END TEST2 PARAMS */
	
	/* START TEST4 PARAMS */
	public static double test4StdMax = 0.2;
	public static boolean test4Resume = false;
	public static boolean test4FilterAnalysis = false;
	public static int test4Repetitions = 2;
	public static String test4SerDir = "serial/";
	public static String test4SCCSer = "scc.ser";
	public static String test4AdjSer = "adj.ser";
	public static String test4CyclesSer = "cycles.ser";
	/* END TEST4 PARAMS */
	
	/* START TEST7 PARAMS */
	public static final String test7MappingDir = test7OutDir + "mappings/";
    /* END TEST7 PARAMS*/	

	/* START MULTIPLE MAPPINGS FILTERING PARAMS */
	// filtering thresholds on SCC
	public static final int arcsMaxThresholdFilter = 200;
	public static final int arcsNoMappingMaxThresholdFilter = 90;
	public static final int mappingsMaxThresholdFilter = 65;
	
	public static boolean useExactMapping = true;
	/* END MULTIPLE MAPPINGS FILTERING PARAMS */
	
	/* START ASP PARAMS */
	public static int maxAttemptOnError = 2;
	public static int ASPTimeout = 60; // in seconds
	public static boolean useDLV = false;
	public static boolean conservativeDiagnosis = false;
	public static int confidencePrecisionASP = (int) Math.pow(10, 7);
	// Vmtf, Vsids, Berkmin
	private static final String [] claspPossibleHeuristics 
		= {"Vmtf","Vsids","Berkmin"};
	public static String claspHeuristic = claspPossibleHeuristics[1];
	
	public static final int numCyclesSaveFile = 1000000;
	public static final int numMappingsSaveFile = 100;
	
	public static final String aspDirPath = "asp/"; 
	public static final String libDirPath = "lib/"; 	
	public static final String tmpDir = aspDirPath + "tmp/";
	public static final String savedDir = aspDirPath + "saved/";
	public static final String filterAspProgramPath = aspDirPath 
			+ "clasp_multifilter.dl";
	public static String claspNonConsSuffix = "_subwfes_tptnt.dl";
	public static String timeoutBinPath = libDirPath + "timeout-8.21-20";
	public static String aspBinPath = libDirPath + "clingo-3.0.5-amd64-linux";

	static {
		if(os.equals(OS.MACOS)){
			timeoutBinPath = libDirPath + "timeout_macos";
			aspBinPath = libDirPath + "clingo-3.0.5-macos-10.8.3";
		}
		else if(os.equals(OS.WIN)){
			aspBinPath = libDirPath + "clingo-3.0.5-win64.exe";
		}
	}
	
	public static String consAspProgramPath = aspDirPath 
			+ (Params.useDLV ? "dlv" : "clasp") + "_fes.dl";
	public static String nonconsAspProgramPath = aspDirPath 
			+ (Params.useDLV ? "dlv_subwfes.dl" 
					: ("clasp" + claspNonConsSuffix));

	public static void setASPProgramPaths(boolean useDLV){
		consAspProgramPath = aspDirPath + (useDLV ? "dlv" : "clasp") 
				+ "_fes.dl";
		nonconsAspProgramPath = aspDirPath + (Params.useDLV ? "dlv_subwfes.dl" 
						: ("clasp" + claspNonConsSuffix));
	}
	/* END ASP PARAMS */
	
	/* START SGA PARAMS */
	public static int SGATimeout = 60; // in seconds
	/* END SGA PARAMS */
	
	/* START VISUALIZATION PARAMS */
	public static final String edgeLabel = "isA";
	public static boolean visualizationGUI = false;
	/* END VISUALIZATION PARAMS */
	
	/* START CONSERVATIVITY GUI PARAMS */
	public static VIOL_KIND violKindToShow = VIOL_KIND.FULL;
	public static boolean preComputeDirectViols = false;
	/* END CONSERVATIVITY GUI PARAMS */

	/* START REPAIR PARAMS */
	public static boolean fullReasoningRepair = true;	
	public static int timeoutFullRepairExplanation = 10 * 1000; //ms
	public static boolean singleClassFullRepairStep = true;

	public static REPAIR_METHOD defaultRepairMethod = REPAIR_METHOD.SUBEQ;
	/* END REPAIR PARAMS */

	/* START MATERIALIZED FILES PARAMS */
	public static boolean saveOnto = false;
	public static boolean saveMappings = true;
	/* END MATERIALIZED FILES PARAMS */	
	
	/* START CONSERVATIVITY PARAMS */
	public static boolean rootViolations = true;
	public static boolean fullDisj = false;
	public static boolean preSCC = false;
	/* END CONSERVATIVITY PARAMS */
	
	/* START REASONING PARAMS */
	public static boolean useModules = false;

	// structural reasoning or classification with Hermit
	public static boolean fullDetection = false;
	public static int explanationsNumber = 1;
	public static boolean laconicJust = false;
	public static REASONER_KIND reasonerKind = REASONER_KIND.HERMIT;
	public static boolean tryPellet = false;
	public static REASONER_KIND reasonerAfterTimeout = REASONER_KIND.ELK;
	public static REASONER_KIND reasonerBasic = REASONER_KIND.STRUCTURAL;
	public static boolean bufferingReasoner = false;
	public static boolean incrementalReasoning = false;
	// asks optional reasoning on disjointness
	public static final boolean lightDisj = false;
	public static final boolean disj = false;
	public static int alignOntoClassificationTimeout = 350; // sec
	public static int inputOntoClassificationTimeout = 180; // sec
	// if more than X times do not save classification
	public static final int maxInferredAxiomsTimes = 140;
	public static final int minAxiomsInferenceBlock = 30000;
	
//	public static final boolean repairAlignedOnto = false;
	public static boolean reasonerViolationsCheck = false;
	public static boolean reasonerValidateIndex = false;
	public static boolean explanationBasedDirectViolationsCheck = false;
	public static int maxExplanationsForDirectViol = 30;
	
	public static Map<REASONER_KIND, Integer> reasonerServerPort = 
			new HashMap<>();
	
	static {
		reasonerServerPort.put(REASONER_KIND.KONCLUDE, 8082);
	}
	public static int getReasonerServerPort(REASONER_KIND rk){
		if(reasonerServerPort.containsKey(rk))
			return reasonerServerPort.get(rk);
		return -1;
	}
	/* END REASONING PARAMS */
	
	public static boolean checkDirectUnsolvedViolations = false;
	
	// suppress warning messages coming from ELK 
	static {
		Logger.getLogger("org.semanticweb.elk").setLevel(Level.ERROR);
	}
	
	public static void setParams(int testKind){
		switch(testKind){
		case 0: // largebio big
			trackName = trackNames[2];
			Params.alignOntoClassificationTimeout = 3 * 60;
			Params.inputOntoClassificationTimeout = 3 * 60;
			whole = true;
			useModules = true;
			ontoSize = "big";
			Params.tryPellet = false;
			break;
		case 1: // largebio small
			trackName = trackNames[2];
			Params.alignOntoClassificationTimeout = 3 * 60;
			Params.inputOntoClassificationTimeout = 3 * 60;
			whole = false;
			useModules = false;
			ontoSize = "small";
			Params.tryPellet = false;
			break;
		case 2: // anatomy
			trackName = trackNames[0];
			Params.tryPellet = false;
			useModules = false;
			alignOntoClassificationTimeout = 3 * 60;
			inputOntoClassificationTimeout = 3 * 60;
			break;
		case 3: // conference
			trackName = trackNames[1];
			Params.explanationsNumber = 1; 
			Params.tryPellet = true;
			useModules = false;
			Params.alignOntoClassificationTimeout = 1 * 60;
			Params.inputOntoClassificationTimeout = 1 * 60;
			singleClassFullRepairStep = true;
			break;
		case 4: // library
			trackName = trackNames[3];
			Params.tryPellet = false;
			useModules = false;
			alignOntoClassificationTimeout = 3 * 60;
			inputOntoClassificationTimeout = 3 * 60;
			break;
		case 5: // optique
			trackName = trackNames[4];			
			Params.tryPellet = false;
			useModules = false;
			alignOntoClassificationTimeout = 3 * 60;
			inputOntoClassificationTimeout = 3 * 60;
			timeoutFullRepairExplanation = 100 * 1000;
			singleClassFullRepairStep = true;
			break;
		}
	}
	
	/* START INDEXES PARAMS */
	public static boolean indexSanityCheck = false;
	/* END INDEXES PARAMS */
	
	/* START CONSERVATIVITY PARAMS */
	public static DISJ_CHECK disjCheckStrategy = DISJ_CHECK.SEMINDEX; 
	public static REPAIR_STRATEGY repairStrategy = 
			REPAIR_STRATEGY.CONSIST_THEN_CONSERV;
	public static boolean storeViolations = false;
	/* END CONSERVATIVITY PARAMS */
	
	/* START DATASET SELECTION PARAMS */
	// starts the whole testset (asmov, lily, full oaei)
	public static boolean runAll = true;
	
	// switch from full ontologies to fraction of them for the largebio oaei track, ignored if runAll is true
	public static boolean whole = false;
	public static String ontoSize = whole ? "big" : "small";
	
	// selects track (OAEI) or asmov for testing, ignored if runAll is true
	public static boolean trackNotAsmov = true;
	public static boolean testLily = false;
	public static final String [] trackNames = {"anatomy","conference","largebio","library","optique"};
	public static String trackName = trackNames[2];
	
	// maps name and filepath of the largebio ontologies
	public static Map<String,String> largebioOntologies = new HashMap<>();
	static {
		largebioOntologies.put("bigfmanci","oaei2012_FMA_extended_overlapping_nci.owl");
		largebioOntologies.put("bigfmasnomed","oaei2012_FMA_extended_overlapping_snomed.owl");
		largebioOntologies.put("smallfmanci","oaei2012_FMA_small_overlapping_nci.owl");
		largebioOntologies.put("smallfmasnomed","oaei2012_FMA_small_overlapping_snomed.owl");
		//largebioOntologies.put("bigfma","oaei2012_FMA_whole_ontology.owl");
		largebioOntologies.put("bigncifma","oaei2012_NCI_extended_overlapping_fma.owl");
		largebioOntologies.put("bigncisnomed","oaei2012_NCI_extended_overlapping_snomed.owl");
		largebioOntologies.put("smallncifma","oaei2012_NCI_small_overlapping_fma.owl");
		largebioOntologies.put("smallncisnomed","oaei2012_NCI_small_overlapping_snomed.owl");
		//largebioOntologies.put("bignci","oaei2012_NCI_whole_ontology.owl");
		largebioOntologies.put("bigsnomedfma","oaei2012_SNOMED_extended_overlapping_fma_nci.owl");
		largebioOntologies.put("bigsnomednci","oaei2012_SNOMED_extended_overlapping_fma_nci.owl");
		largebioOntologies.put("smallsnomedfma","oaei2012_SNOMED_small_overlapping_fma.owl");
		largebioOntologies.put("smallsnomednci","oaei2012_SNOMED_small_overlapping_nci.owl");
	}
	
	public static Map<String,String> largebioOntologies13 = new HashMap<>();
	static {
		largebioOntologies13.put("bigfmanci","oaei2013_FMA_whole_ontology.owl");
		largebioOntologies13.put("bigfmasnomed","oaei2013_FMA_whole_ontology.owl");
		largebioOntologies13.put("smallfmanci","oaei2013_FMA_small_overlapping_nci.owl");
		largebioOntologies13.put("smallfmasnomed","oaei2013_FMA_small_overlapping_snomed.owl");

		largebioOntologies13.put("bigncifma","oaei2013_NCI_whole_ontology.owl");
		largebioOntologies13.put("bigncisnomed","oaei2013_NCI_whole_ontology.owl");
		largebioOntologies13.put("smallncifma","oaei2013_NCI_small_overlapping_fma.owl");
		largebioOntologies13.put("smallncisnomed","oaei2013_NCI_small_overlapping_snomed.owl");

		largebioOntologies13.put("bigsnomedfma","oaei2013_SNOMED_extended_overlapping_fma_nci.owl");
		largebioOntologies13.put("bigsnomednci","oaei2013_SNOMED_extended_overlapping_fma_nci.owl");
		largebioOntologies13.put("smallsnomedfma","oaei2013_SNOMED_small_overlapping_fma.owl");
		largebioOntologies13.put("smallsnomednci","oaei2013_SNOMED_small_overlapping_nci.owl");
	}
	
	public static Map<String,String> largebioOntologies14 = new HashMap<>();
	static {
		largebioOntologies14.put("bigfmanci","oaei2014_FMA_whole_ontology.owl");
		largebioOntologies14.put("bigfmasnomed","oaei2014_FMA_whole_ontology.owl");
		largebioOntologies14.put("smallfmanci","oaei2014_FMA_small_overlapping_nci.owl");
		largebioOntologies14.put("smallfmasnomed","oaei2014_FMA_small_overlapping_snomed.owl");

		largebioOntologies14.put("bigncifma","oaei2014_NCI_whole_ontology.owl");
		largebioOntologies14.put("bigncisnomed","oaei2014_NCI_whole_ontology.owl");
		largebioOntologies14.put("smallncifma","oaei2014_NCI_small_overlapping_fma.owl");
		largebioOntologies14.put("smallncisnomed","oaei2014_NCI_small_overlapping_snomed.owl");

		largebioOntologies14.put("bigsnomedfma","oaei2014_SNOMED_extended_overlapping_fma_nci.owl");
		largebioOntologies14.put("bigsnomednci","oaei2014_SNOMED_extended_overlapping_fma_nci.owl");
		largebioOntologies14.put("smallsnomedfma","oaei2014_SNOMED_small_overlapping_fma.owl");
		largebioOntologies14.put("smallsnomednci","oaei2014_SNOMED_small_overlapping_nci.owl");
	}
	
	public static Map<String,String> largebioRef = new HashMap<>();
	static {
		largebioRef.put("fmancioriginal","oaei2012_FMA2NCI_original_UMLS_mappings.rdf");
		largebioRef.put("fmancilogmap","oaei2012_FMA2NCI_repaired_UMLS_mappings_logmap.rdf");
		largebioRef.put("fmancialcomo","oaei2012_FMA2NCI_repaired_UMLS_mappings_alcomo.rdf");
		largebioRef.put("fmancivoted","oaei2012_FMA2NCI_voted_mappings_oaei.rdf");
		largebioRef.put("fmancivoted3","oaei2012_FMA2NCI_voted_mappings3.0.rdf");
		largebioRef.put("fmancivoted4","oaei2012_FMA2NCI_voted_mappings4.0.rdf");
		largebioRef.put("fmancivoted5","oaei2012_FMA2NCI_voted_mappings5.0.rdf");
		
		largebioRef.put("fmasnomedoriginal","oaei2012_FMA2SNMD_original_UMLS_mappings.rdf");
		largebioRef.put("fmasnomedlogmap","oaei2012_FMA2SNMD_repaired_UMLS_mappings_logmap.rdf");
		largebioRef.put("fmasnomedalcomo","oaei2012_FMA2SNMD_repaired_UMLS_mappings_alcomo.rdf");
		largebioRef.put("fmasnomedvoted2","oaei2012_FMA2SNOMED_voted_mappings2.0.rdf");
		largebioRef.put("fmasnomedvoted3","oaei2012_FMA2SNOMED_voted_mappings3.0.rdf");
		
		largebioRef.put("snomedncioriginal","oaei2012_SNMD2NCI_original_UMLS_mappings.rdf");
		largebioRef.put("snomedncilogmap","oaei2012_SNMD2NCI_repaired_UMLS_mappings_logmap.rdf");
		largebioRef.put("snomedncivoted2","oaei2012_SNOMED2NCI_voted_mappings2.0.rdf");
		largebioRef.put("snomedncivoted3","oaei2012_SNOMED2NCI_voted_mappings3.0.rdf");
	}
	
	public static Map<String,String> largebioRef13 = new HashMap<>();

	static {
		largebioRef13.put("fmancioriginal","oaei2013_FMA2NCI_original_UMLS_mappings_with_confidence.rdf");
		largebioRef13.put("fmancirepaired","oaei2013_FMA2NCI_repaired_UMLS_mappings.rdf");
		
		largebioRef13.put("fmasnomedoriginal","oaei2013_FMA2SNOMED_original_UMLS_mappings_with_confidence.rdf");
		largebioRef13.put("fmasnomedrepaired","oaei2013_FMA2SNOMED_repaired_UMLS_mappings.rdf");
		
		largebioRef13.put("snomedncioriginal","oaei2013_SNOMED2NCI_original_UMLS_mappings_with_confidence.rdf");
		largebioRef13.put("snomedncirepaired","oaei2013_SNOMED2NCI_repaired_UMLS_mappings.rdf");
	}
	
	public static Map<String,String> largebioRef14 = new HashMap<>();

	static {
		largebioRef14.put("fmancirepaired","oaei2014_FMA2NCI_UMLS_mappings_with_flagged_repairs.rdf");
		
		largebioRef14.put("fmasnomedrepaired","oaei2014_FMA2SNOMED_UMLS_mappings_with_flagged_repairs.rdf");
		
		largebioRef14.put("snomedncirepaired","oaei2014_SNOMED2NCI_UMLS_mappings_with_flagged_repairs.rdf");
	}
	
	public static Map<String,List<String>> referenceAlign13 = new HashMap<>();
	static {
		List<String> refs = new LinkedList<>();
		refs.add("mouse-human.rdf");
		referenceAlign13.put("anatomy", refs);

		refs = new LinkedList<>();
		refs.add("oaei2013_SNOMED2NCI_repaired_UMLS_mappings.rdf");
		refs.add("oaei2013_FMA2SNOMED_repaired_UMLS_mappings.rdf");
		refs.add("oaei2013_FMA2NCI_repaired_UMLS_mappings.rdf");
		referenceAlign13.put("largebio", refs);

		refs = new LinkedList<>();
				refs.add("cmt-conference.rdf");
				refs.add("cmt-confof.rdf");
				refs.add("cmt-edas.rdf");
				refs.add("cmt-ekaw.rdf");
				refs.add("cmt-iasted.rdf");
				refs.add("cmt-sigkdd.rdf");
				refs.add("conference-confof.rdf");
				refs.add("conference-edas.rdf");
				refs.add("conference-ekaw.rdf");		
				refs.add("conference-iasted.rdf");
				refs.add("conference-sigkdd.rdf");
				refs.add("confof-edas.rdf");
				refs.add("confof-ekaw.rdf");
				refs.add("confof-iasted.rdf");
				refs.add("confof-sigkdd.rdf");
				refs.add("edas-ekaw.rdf");
				refs.add("edas-iasted.rdf");
				refs.add("edas-sigkdd.rdf");
				refs.add("ekaw-iasted.rdf");
				refs.add("ekaw-sigkdd.rdf");
				refs.add("iasted-sigkdd.rdf");
		referenceAlign13.put("conference", refs);

		refs = new LinkedList<>();
		refs.add("stw-thesoz.rdf");
		referenceAlign13.put("library", refs);

		refs = new LinkedList<>();
		refs.add("NPD-slegge.rdf");
		referenceAlign13.put("optique", refs);
	}
	
	public static Map<String,List<String>> referenceAlign14 = new HashMap<>();
	
	static {
		List<String> refs = new LinkedList<>();
		refs.add("mouse-human.rdf");
		referenceAlign14.put("anatomy", refs);

		refs = new LinkedList<>();
		refs.add(largebioRef14.get("fmancirepaired"));
		refs.add(largebioRef14.get("fmasnomedrepaired"));
		refs.add(largebioRef14.get("snomedncirepaired"));
		referenceAlign14.put("largebio", refs);

		refs = new LinkedList<>();
				refs.add("cmt-conference.rdf");
				refs.add("cmt-confof.rdf");
				refs.add("cmt-edas.rdf");
				refs.add("cmt-ekaw.rdf");
				refs.add("cmt-iasted.rdf");
				refs.add("cmt-sigkdd.rdf");
				refs.add("conference-confof.rdf");
				refs.add("conference-edas.rdf");
				refs.add("conference-ekaw.rdf");		
				refs.add("conference-iasted.rdf");
				refs.add("conference-sigkdd.rdf");
				refs.add("confof-edas.rdf");
				refs.add("confof-ekaw.rdf");
				refs.add("confof-iasted.rdf");
				refs.add("confof-sigkdd.rdf");
				refs.add("edas-ekaw.rdf");
				refs.add("edas-iasted.rdf");
				refs.add("edas-sigkdd.rdf");
				refs.add("ekaw-iasted.rdf");
				refs.add("ekaw-sigkdd.rdf");
				refs.add("iasted-sigkdd.rdf");
		referenceAlign14.put("conference", refs);

		refs = new LinkedList<>();
		refs.add("stw-thesoz.rdf");
		referenceAlign14.put("library", refs);
	}
	/* END DATASET SELECTION PARAMS */
}
