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
package test;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import logmap.LogMapWrapper;

import org.mindswap.pellet.exceptions.InconsistentOntologyException;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import reasoning.ExtDisjReasoner;
import uk.ac.ox.krr.logmap2.LogMap2Core;
import uk.ac.ox.krr.logmap2.LogMap2_OAEI;
import uk.ac.ox.krr.logmap2.indexing.JointIndexManager;
import uk.ac.ox.krr.logmap2.indexing.OntologyProcessing;
import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;
import uk.ac.ox.krr.logmap2.overlapping.OverlappingExtractor4Mappings;
import util.FileUtil;
import util.OntoUtil;
import util.Params;
import util.Util;
import auxStructures.Pair;
import enumerations.REASONER_KIND;
import enumerations.REPAIR_STRATEGY;

public class Test7 {
	/*
	 * This test aims at testing conservativity principle using LogMap 
	 */
	private static String [] trackNames = {"anatomy","conference","largebio",
		"library","optique"};
	private static String trackName = trackNames[2];
	private static boolean whole = false;
	private static String ontoSize = whole ? "big" : "small";
	private static int count;
	private static OWLOntologyManager manager = OntoUtil.getManager(false);
	private static OWLOntology fstO, sndO;

	private static boolean checkPost = false;
	private static final boolean approxOnto = false;
	private static int repetitionsNum = 1;

	private static boolean rootViolations = true;
	private static boolean fullDisj = false;
	private static boolean useModules = false;
	//	private static boolean alsoEquiv = false;

	private static OWLOntology alignOnto;
	private static OWLReasoner alignReasoner;
	private static Set<MappingObjectStr> consistentMappings;
	private static boolean useELK = false;
	private static List<Integer> incoherences;
	private static JointIndexManager alignIdx;
	private static OntologyProcessing alignProc;
	
	/**
	 * @throws IOException 
	 * @throws OWLOntologyCreationException 
	 */
	public static void main(String[] args) throws OWLOntologyCreationException, 
	IOException {

		int testKind;

		if(args.length != 4)
			throw new IllegalArgumentException(
					"Three arguments are needed");
		testKind = Integer.parseInt(args[0]);
		repetitionsNum = Integer.parseInt(args[1]);
		if("full".equals(args[2]))
			fullDisj = true;
		else if("light".equals(args[2]))
			fullDisj = false;
		else
			throw new IllegalArgumentException("Invalid parameter for disjointness method: " 
					+ args[2] + "\nValid arguments are \"full\" and \"light\"");

		if("direct".equals(args[3]))
			rootViolations = true;
		else if("all".equals(args[3]))
			rootViolations = false;
		else 
			throw new IllegalArgumentException("Invalid parameter for violations method: " 
					+ args[3] + "\nValid arguments are \"direct\" and \"all\"");

		Params.verbosity = 0;
		Params.alwaysTestDiagnosis = false;

		FileUtil.createDirPath(Params.test7OutDir);
		FileUtil.createDirPath(Params.test7MappingDir);

		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		String startTime = sdf.format(Calendar.getInstance().getTime());

		String prefixFile = "test7_";

		prefixFile += testKind + "_" + repetitionsNum + "_gold-" 
				+ (fullDisj ? "full" : "light") ;

		FileUtil.createTestDataFile(Params.test7OutDir + prefixFile + ".txt");
		FileUtil.createLogFile(Params.test7OutDir + prefixFile + "_log.txt");

		if(testKind == 0) {
			trackName = trackNames[2];
			checkPost = false;
			//			approxOnto = true;
			Params.alignOntoClassificationTimeout = 3 * 60;
			Params.inputOntoClassificationTimeout = 3 * 60;
			whole = true;
			useModules = true;
			ontoSize = "big";
			trackTest();
		}
		else if(testKind == 1){
			trackName = trackNames[2];
			checkPost = false;
			//			approxOnto = true;
			Params.alignOntoClassificationTimeout = 3 * 60;
			Params.inputOntoClassificationTimeout = 3 * 60;
			whole = false;
			ontoSize = "small";
			trackTest();
		}
		else if(testKind == 2){
			trackName = trackNames[0];
			//			approxOnto = true;
			trackTest();
		}
		else if(testKind == 3) {
			trackName = trackNames[1];
			checkPost = false;
			//			approxOnto = true;
			Params.explanationsNumber = 1; 
			trackTest();
		}
		else if(testKind == 4){
			trackName = trackNames[3];
			checkPost = false;
			trackTest();
		}
		else if(testKind == 5){
			trackName = trackNames[4];
			checkPost = false;
			trackTest();
		}

		FileUtil.writeLogAndConsole("Start: " + startTime);
		FileUtil.writeLogAndConsole("End: " 
				+ new SimpleDateFormat("HH:mm:ss").format(
						Calendar.getInstance().getTime()));

		FileUtil.closeFiles();
	}

	private static void trackTest() 
			throws OWLOntologyCreationException, IOException {

		String mappingDir = Params.dataFolder + "oaei2013/" + trackName + "/";
		String mappingFile = null;
		String fstOnto = null, sndOnto = null;
		List<String> alignments = Params.referenceAlign13.get(trackName);

		if(trackName.equalsIgnoreCase("anatomy")){
			fstOnto = mappingDir + "onto/mouse.owl";
			sndOnto = mappingDir + "onto/human.owl";
			mappingFile = mappingDir + "reference/" + alignments.get(0);
			test(mappingFile, fstOnto, sndOnto, false);
		}
		else if(trackName.equalsIgnoreCase("library")){
			fstOnto = mappingDir + "onto/stw.owl";
			sndOnto = mappingDir + "onto/thesoz.owl";
			mappingFile = mappingDir + "reference/" + alignments.get(0);
			test(mappingFile, fstOnto, sndOnto, false);
		}
		else if(trackName.equalsIgnoreCase("conference")){
			for (String align : alignments) {
				String [] ontos = align.substring(
						0, align.length()-4).split("-");
				fstOnto = mappingDir + "onto/" + ontos[0] + ".owl";
				sndOnto = mappingDir + "onto/" + ontos[1] + ".owl";
				mappingFile = mappingDir + "reference/" + align;
				test(mappingFile, fstOnto, sndOnto, false);
			}
		}
		else if(trackName.equalsIgnoreCase("largebio")){
			for (String align : alignments) {

				// SNOMED2NCI is too long to be processed by the basic method 
				if(align.contains("SNOMED2NCI") && fullDisj){
					FileUtil.writeLogAndConsole("Skipping full test (too long) for " + align);
					continue;
				}

				String [] ontoNames = align.split("_")[1].split("2");
				mappingFile = mappingDir + "reference/" + align;

				ontoNames[0] = ontoNames[0].toLowerCase();
				ontoNames[1] = ontoNames[1].toLowerCase();

				fstOnto = mappingDir + "onto/" + Params.largebioOntologies13.get(
						ontoSize + ontoNames[0] + ontoNames[1]);
				sndOnto = mappingDir + "onto/" + Params.largebioOntologies13.get(
						ontoSize + ontoNames[1] + ontoNames[0]);

				test(mappingFile, fstOnto, sndOnto, true);
			}
		}
		else if(trackName.equalsIgnoreCase("optique")){
			fstOnto = Params.dataFolder + "Slegge_NPD_usecase/NPD_adaptedQFI.owl";
			sndOnto = Params.dataFolder 
					+ "Slegge_NPD_usecase/bootstrapped_onto_slegge_whole.owl";
			mappingFile = Params.dataFolder + "Slegge_NPD_usecase/" + alignments.get(0);
			test(mappingFile, fstOnto, sndOnto, false);
		}
	}

	private static void loadOntologies(String fstOnto, String sndOnto){
		OntoUtil.unloadAllOntologies(manager);
		manager = OntoUtil.getManager(true);
		try {
			fstO = OntoUtil.load(fstOnto, true, manager);
			sndO = OntoUtil.load(sndOnto, true, manager);
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
		if(approxOnto){
			// TODO: cache index?
			fstO = OntoUtil.overApproximateOntologyClassification(
					manager, fstO, null);//LogMapWrapper.buildOntologyIndex(fstO,null));
			sndO = OntoUtil.overApproximateOntologyClassification(
					manager, sndO, null);//LogMapWrapper.buildOntologyIndex(sndO,null));
		}
	}

	public static void test(String mappingFile, String fstOnto, String sndOnto, 
			boolean unloadOnto) throws OWLOntologyCreationException, IOException {		

		for (int i = 0; i < repetitionsNum; i++) {			
			FileUtil.writeLogAndConsole("\nTEST " + (++count) + " START: " 
					+ Util.getCurrTime());

			FileUtil.writeLogAndConsole(mappingFile);

			long totalStartTime = Util.getMSec();

			loadOntologies(fstOnto, sndOnto);
			testLogMap(mappingFile, fstO, sndO, unloadOnto, 
					rootViolations, fullDisj, useModules, trackName);

			FileUtil.flushDataFile();
			FileUtil.writeLogAndConsole("Total test time: " 
					+ Util.getDiffmsec(totalStartTime));
		}
	}

	public static void testLogMap(String mappingFile, 
			OWLOntology fstO, OWLOntology sndO, boolean unloadOnto, 
			boolean rootViolations, boolean fullDisj, boolean useModules, 
			String trackName) {

		// clean static variables
		alignOnto = null;
		alignReasoner = null;
		consistentMappings = null;
		useELK = false;
		incoherences = new ArrayList<>();
		alignIdx = null;
		alignProc = null;
		
		//TODO: to be removed, not working and not necessary
		boolean disjInAligned = false;
		// not really used anymore
		boolean avoidLeafSiblings = false;
		boolean checkEntailment = false;

		boolean suppressOutputViol = true;
		boolean saveOnto = false;

		FileUtil.enableDataOutFileBuffering();

		if(!new File(mappingFile).exists()){
			FileUtil.writeErrorLogAndConsole("Mapping file " + mappingFile 
					+ " does not exists, skipping this test");
			return;
		}

		FileUtil.writeLogAndConsole("Disjointness computation strategy: " 
				+ (fullDisj ? "FULL" : "LIGHT"));

		FileUtil.writeLogAndConsole("Ontology 0: " + fstO.toString());
		FileUtil.writeLogAndConsole("Ontology 1: " + sndO.toString());

		int signSizeFst = fstO.getClassesInSignature(true).size() + 
				fstO.getObjectPropertiesInSignature(true).size() + 
				fstO.getDataPropertiesInSignature(true).size();
		int signSizeSnd = sndO.getClassesInSignature(true).size() + 
				sndO.getObjectPropertiesInSignature(true).size() + 
				sndO.getDataPropertiesInSignature(true).size();

		/****** COLUMN 0 = alignment (matcher + ontologies) *********/
		String mappingName;

		mappingName = mappingFile.substring(mappingFile.lastIndexOf("/")+1, 
				mappingFile.length()-4);

		if(mappingFile.contains("UMLS")){
			mappingName = mappingFile.split("_")[1];
			mappingName = mappingName.replace('2', '-');
		}

		FileUtil.writeDataOutFile(mappingName + " ");

		Set<MappingObjectStr> originalMappings = 
				LogMapWrapper.getMappings(mappingFile, fstO, sndO);

		// if required we extract the modules
		if (useModules){
			// STEP 0: modules extraction 
			FileUtil.writeLogAndConsole(
					"\nSTEP 0: modules extraction - " + 
							Util.getCurrTime());
			long start = Util.getMSec();

			OverlappingExtractor4Mappings overlapping = 
					new OverlappingExtractor4Mappings();

			try {
				overlapping.createOverlapping(fstO, sndO, originalMappings);
			} catch (Exception e) {
				FileUtil.writeErrorLogAndConsole("Error while extracting " +
						"modules, skipping the test: " + e.getMessage());
				return;
			}
			fstO = overlapping.getOverlappingOnto1();
			sndO = overlapping.getOverlappingOnto2();

			FileUtil.writeLogAndConsole("Module Ontology 0: " + fstO.toString());
			FileUtil.writeLogAndConsole("Module Ontology 1: " + sndO.toString());

			FileUtil.writeLogAndConsole("STEP 0: " + Util.getDiffmsec(start) 
					+ " (ms) - " + Util.getCurrTime() + "\n");
		}

		OntoUtil.chooseReasoner(mappingFile, trackName);

		// STEP 1: load and classify input ontologies 
		FileUtil.writeLogAndConsole(
				"\nSTEP 1: input ontologies classification - " + 
						Util.getCurrTime());

		OWLReasoner fstReasoner = OntoUtil.getReasoner(fstO, 
				Params.reasonerKind, manager);
		OWLReasoner sndReasoner = OntoUtil.getReasoner(sndO, 
				Params.reasonerKind, manager);

		List<OWLReasoner> reasoners = new ArrayList<>(2);
		reasoners.add(fstReasoner);
		reasoners.add(sndReasoner);

		// required to build a complete index
		long classTime = OntoUtil.ontologyClassification(false, false, 
				reasoners, Params.tryPellet);

		fstReasoner = reasoners.get(0);
		sndReasoner = reasoners.get(1);
		reasoners.clear();

		determineELKUsage(fstReasoner, sndReasoner);
		
		if(!fstReasoner.getRootOntology().equals(fstO))
			throw new Error("First ontology for reasoner mismatch");
		if(!sndReasoner.getRootOntology().equals(sndO))
			throw new Error("Second ontology for reasoner mismatch");

		if(!OntoUtil.checkClassification(fstReasoner)){
			FileUtil.writeLogAndConsole("First input ontology not classified, " +
					"skipping the test");
			OntoUtil.disposeAllReasoners();
			return;
		}
		if(!OntoUtil.checkClassification(sndReasoner)){
			FileUtil.writeLogAndConsole("Second input ontology not classified, " +
					"skipping the test");
			OntoUtil.disposeAllReasoners();
			return;
		}

		//		OntoUtil.checkActiveReasoners(false);

		long start = Util.getMSec();

		// create the inferred ontology generator
		try {			
			OntoUtil.saveClassificationAxioms(fstO, fstReasoner, manager);
			OntoUtil.saveClassificationAxioms(sndO, sndReasoner, manager);

			FileUtil.writeLogAndConsole("Ontology 0 (classified): " 
					+ fstO.toString());
			FileUtil.writeLogAndConsole("Ontology 1 (classified): " 
					+ sndO.toString());			
		}
		catch(org.semanticweb.owlapi.reasoner.InconsistentOntologyException | 
				NullPointerException e){
			FileUtil.writeErrorLogAndConsole("Classified input ontologies reification failed: " 
					+ e.getMessage() + ", skipping this test");
			OntoUtil.disposeAllReasoners();
			return;
		}

		/****** COLUMN 1 = inputClassificationTime *********/
		FileUtil.writeDataOutFile((classTime + Util.getDiffmsec(start)) + " ");

		FileUtil.writeLogAndConsole("STEP 1: " + 
				(classTime + Util.getDiffmsec(start)) 
				+ " (ms) - " + Util.getCurrTime() + "\n");

		if(saveOnto){
			FileUtil.writeLogAndConsole("Saving classified input ontologies");

			String ontoName = mappingFile.substring(
					mappingFile.lastIndexOf('/')+1, 
					mappingFile.lastIndexOf('.'));
			String [] names = ontoName.split("-");
			try {
				OntoUtil.save(fstO, 
						"/home/ale/Downloads/ontologies/classified/" 
								+ names[0] + "_" + fstReasoner.getReasonerName() 
								+ ".owl", manager);
				OntoUtil.save(sndO, 
						"/home/ale/Downloads/ontologies/classified/" 
								+ names[1] + "_" + sndReasoner.getReasonerName() 
								+ ".owl", manager);
			} catch (OWLOntologyStorageException
					| OWLOntologyCreationException | IOException e) {
				e.printStackTrace();
			}
		}

		// STEP 2: load mappings and profile original consistency repair
		FileUtil.writeLogAndConsole(
				"STEP 2: load mappings and profile original consistency repair - " + 
						Util.getCurrTime());
		start = Util.getMSec();

		long repairTimeConservativity = Util.getMSec();
		consistentMappings = LogMapWrapper.repairInconsistentAlignments(
				null, mappingFile, fstO, sndO, originalMappings, true);
		repairTimeConservativity = Util.getDiffmsec(repairTimeConservativity);

		/****** COLUMN 2 = |M|, COLUMN 3 = repair_consistency time, COLUMN 4 = |R|_consistency *********/
		FileUtil.writeDataOutFile(LogMapWrapper.countMappings(originalMappings) 
				+ " " + repairTimeConservativity + " " + 
				(LogMapWrapper.countMappings(originalMappings) - 
						LogMapWrapper.countMappings(consistentMappings)) + " ");

		Set<OWLAxiom> consistentAlignment = OntoUtil.convertAlignmentToAxioms(
				fstO, sndO, consistentMappings),
				originalAlignment = Collections.unmodifiableSet(
						OntoUtil.convertAlignmentToAxioms(fstO, sndO, 
								originalMappings));

		FileUtil.writeLogAndConsole("STEP 2: " + Util.getDiffmsec(start) 
				+ " (ms) - " + Util.getCurrTime() + "\n");

		// STEP 3: create and classify the aligned ontology
		start = Util.getMSec();
		FileUtil.writeLogAndConsole(
				"STEP 3: create and classify the aligned ontology - " + 
						Util.getCurrTime());

//		OWLOntology alignOnto = null;
//		OWLOntology fstOntoMapping = null;
//		OWLOntology sndOntoMapping = null;

//		OWLReasoner alignReasoner = null;
//		OWLReasoner fstMappingReasoner = null;
//		OWLReasoner sndMappingReasoner = null;

		long alignClassifTime, incohCheckTime, totalAlignTime = start;
		try {
			FileUtil.writeLogAndConsole(consistentAlignment.size() + " mapping(s)");
			
			alignOnto = OntoUtil.getAlignedOntology(manager, 
					Params.repairStrategy.equals(
							REPAIR_STRATEGY.CONSIST_THEN_CONSERV) ? 
									consistentAlignment : originalAlignment, 
									fstO, sndO);
			alignReasoner = OntoUtil.getReasoner(alignOnto, 
					getAlignReasoner(), manager);

			reasoners.clear();

			reasoners.add(alignReasoner);

			alignClassifTime = OntoUtil.ontologyClassification(true, false, 
					reasoners, Params.tryPellet);

			alignReasoner = reasoners.get(0);

			if(!alignReasoner.getRootOntology().equals(alignOnto))
				throw new Error("Aligned ontology for reasoner mismatch");

			if(!OntoUtil.checkClassification(reasoners) || 
					!alignReasoner.isConsistent()){
				FileUtil.writeErrorLogAndConsole("Timeout or inconsistent " +
						"aligned ontology, skipping the test");
				OntoUtil.disposeAllReasoners();
				return;
			}

			if(!alignReasoner.isConsistent()){
				FileUtil.writeErrorLogAndConsole("Inconsistent " +
						"aligned ontology, skipping the test");
				OntoUtil.disposeAllReasoners();
				return;
			}

			incohCheckTime = Util.getMSec();
			checkIncoherentClasses();
			incohCheckTime = Util.getDiffmsec(incohCheckTime);
						
			determineELKUsage(fstReasoner,sndReasoner,alignReasoner);

			long startSave = Util.getMSec();
			OntoUtil.saveClassificationAxioms(alignOnto, alignReasoner, manager);
			alignClassifTime += Util.getDiffmsec(startSave);
			
			totalAlignTime = Util.getDiffmsec(totalAlignTime);
		}
		catch(org.semanticweb.owlapi.reasoner.InconsistentOntologyException 
				| InconsistentOntologyException | IllegalArgumentException e){
			FileUtil.writeErrorLogAndConsole("Illegal aligned ontology: " 
					+ e.getMessage());
			OntoUtil.disposeAllReasoners();
			return;
		}

		FileUtil.writeLogAndConsole("STEP 3: align " + alignClassifTime 
				+ " (ms), repairFull " + incohCheckTime + " (ms), total " + 
				totalAlignTime + " (ms) - " + Util.getCurrTime() + "\n");

//		// STEP 3 BIS: compute partially aligned ontologies (one input onto + mappings)
//		start = Util.getMSec();
//		FileUtil.writeLogAndConsole(
//				"STEP 3 BIS: compute partially aligned ontologies " +
//						"(one input onto + mappings) - " + 
//						Util.getCurrTime());
//
//		OntoUtil.chooseReasoner(mappingFile, trackName);
//
//		try {
//			fstOntoMapping = OntoUtil.getAlignedOntology(
//					Params.repairStrategy.equals(
//							REPAIR_STRATEGY.CONSIST_THEN_CONSERV) ? 
//									consistentAlignment : originalAlignment, 
//									fstO);
//
//			fstMappingReasoner = OntoUtil.getReasoner(fstOntoMapping, 
//					Params.reasonerKind, manager);
//
//			sndOntoMapping = OntoUtil.getAlignedOntology(
//					Params.repairStrategy.equals(
//							REPAIR_STRATEGY.CONSIST_THEN_CONSERV) ? 
//									consistentAlignment : originalAlignment, 
//									sndO);
//
//			sndMappingReasoner = OntoUtil.getReasoner(sndOntoMapping, 
//					Params.reasonerKind, manager);
//
//			reasoners.clear();
//			reasoners.add(fstMappingReasoner);
//			reasoners.add(sndMappingReasoner);
//
//			OntoUtil.ontologyClassification(false, false, reasoners, Params.tryPellet);
//			OntoUtil.checkClassification(reasoners);
//
//			fstMappingReasoner = reasoners.get(0);
//			sndMappingReasoner = reasoners.get(1);
//
//			if(!fstMappingReasoner.getRootOntology().equals(fstOntoMapping))
//				throw new Error("First ontology + mappings for reasoner mismatch");
//
//			if(!sndMappingReasoner.getRootOntology().equals(sndOntoMapping))
//				throw new Error("Second ontology + mappings for reasoner mismatch");
//
//			reasoners.clear();
//
//			OntoUtil.saveClassificationAxioms(fstOntoMapping, fstMappingReasoner);
//			OntoUtil.saveClassificationAxioms(sndOntoMapping, sndMappingReasoner);
//		}
//		catch(org.semanticweb.owlapi.reasoner.InconsistentOntologyException | 
//				InconsistentOntologyException | IllegalArgumentException e){
//			FileUtil.writeErrorLogAndConsole("Illegal aligned ontology: " + e.getMessage());
//			OntoUtil.disposeAllReasoners();
//			return;
//		}

		FileUtil.writeLogAndConsole("STEP 3 BIS: " + Util.getDiffmsec(start) 
				+ " (ms) - " + Util.getCurrTime() + "\n");

		// STEP 4: compute the semantic indexes for input/aligned ontologies
		start = Util.getMSec();
		FileUtil.writeLogAndConsole(
				"STEP 4: compute the semantic indexes for input/aligned ontologies - " + 
						Util.getCurrTime());

		alignIdx = new JointIndexManager();  
		JointIndexManager index = new JointIndexManager(),
				origIndex;

//		JointIndexManager fstMappingIndex = new JointIndexManager(),
//				sndMappingIndex = new JointIndexManager();

		OntologyProcessing [] oprocs = LogMapWrapper.indexSetup(
				fstO,sndO,index,fstReasoner,sndReasoner);
		OntologyProcessing fstProc = oprocs[0];
		OntologyProcessing sndProc = oprocs[1];

		origIndex = new JointIndexManager(index);

		OntologyProcessing fstProcOrig = new OntologyProcessing(fstProc);
		OntologyProcessing sndProcOrig = new OntologyProcessing(sndProc);

		alignProc = LogMapWrapper.indexSetup(alignOnto,alignIdx,alignReasoner);
//		OntologyProcessing fstMappingProc = LogMapWrapper.indexSetup(
//				fstOntoMapping,fstMappingIndex,fstMappingReasoner);
//		OntologyProcessing sndMappingProc = LogMapWrapper.indexSetup(
//				sndOntoMapping,sndMappingIndex,sndMappingReasoner);

		FileUtil.writeLogAndConsole("STEP 4: " + Util.getDiffmsec(start) 
				+ " (ms) - " + Util.getCurrTime() + "\n");


		// STEP 4 BIS: sanity check of indexes
		FileUtil.writeLogAndConsole(
				"STEP 4 BIS: indexes sanity check - " + Util.getCurrTime());
		if(Params.indexSanityCheck){
			start = Util.getMSec();

			FileUtil.writeLogAndConsole("Checking input ontologies preorders:");
			if(!index.checkPreOrders()){
				FileUtil.writeErrorLogAndConsole("Preorder errors, skipping the test");
				OntoUtil.disposeAllReasoners();
				return;
			}

			FileUtil.writeLogAndConsole("Checking aligned ontology preorders:");
			if(!alignIdx.checkPreOrders()){
				FileUtil.writeErrorLogAndConsole("Preorder errors, skipping the test");
				OntoUtil.disposeAllReasoners();
				return;
			}

//			FileUtil.writeLogAndConsole("Checking onto 0 + mappings preorders:");
//			if(!fstMappingIndex.checkPreOrders()){
//				FileUtil.writeErrorLogAndConsole("Preorder errors, skipping the test");
//				OntoUtil.disposeAllReasoners();
//				return;
//			}
//
//			FileUtil.writeLogAndConsole("Checking onto 1 + mappings preorders:");
//			if(!sndMappingIndex.checkPreOrders()){
//				FileUtil.writeErrorLogAndConsole("Preorder errors, skipping the test");
//				OntoUtil.disposeAllReasoners();
//				return;
//			}

			FileUtil.writeLogAndConsole("Testing joint input ontology index vs individual indexes");
			JointIndexManager testFstIdx = new JointIndexManager();
			OntologyProcessing testFstProc = LogMapWrapper.indexSetup(
					fstO,testFstIdx,fstReasoner);
			JointIndexManager testSndIdx = new JointIndexManager();
			OntologyProcessing testSndProc = LogMapWrapper.indexSetup(
					sndO,testSndIdx,sndReasoner);
			LogMapWrapper.compareTwoIndexes(testFstIdx, testFstProc, index, fstProc);
			LogMapWrapper.compareTwoIndexes(testSndIdx, testSndProc, index, sndProc);
		}

		if(saveOnto) {
			FileUtil.writeLogAndConsole("Saving aligned input ontology");
			try {
				String ontoName = mappingFile.substring(
						mappingFile.lastIndexOf('/')+1, 
						mappingFile.lastIndexOf('.'));
				OntoUtil.save(alignOnto, "/home/ale/Downloads/ontologies/aligned/" 
						+ ontoName + "-pre.owl", manager);
			} catch (OWLOntologyStorageException | OWLOntologyCreationException
					| IOException e) {
				e.printStackTrace();
			}
		}

		if(Params.reasonerValidateIndex){
			FileUtil.writeLogAndConsole("Validating indexes with reasoner:");
			FileUtil.writeLogAndConsole("Validating index input ontology 0");
			LogMapWrapper.validateIndexWithReasoner(index, fstProc, fstReasoner, 0);
			FileUtil.writeLogAndConsole("Validating index input ontology 1");
			LogMapWrapper.validateIndexWithReasoner(index, sndProc, sndReasoner, 1);
			if(alignReasoner != null){
				FileUtil.writeLogAndConsole("Validating index aligned ontology");
				LogMapWrapper.validateIndexWithReasoner(alignIdx, alignProc, 
						alignReasoner, 0);
			}
//			FileUtil.writeLogAndConsole("Validating index input ontology 0 + mappings");
//			LogMapWrapper.validateIndexWithReasoner(fstMappingIndex, fstMappingProc, 
//					fstMappingReasoner, 0);
//			FileUtil.writeLogAndConsole("Validating index input ontology 1 + mappings");
//			LogMapWrapper.validateIndexWithReasoner(sndMappingIndex, sndMappingProc, 
//					sndMappingReasoner, 0);
		}

		FileUtil.writeLogAndConsole("STEP 4 BIS: " + Util.getDiffmsec(start) 
				+ " (ms) - " + Util.getCurrTime() + "\n");

		// STEP 5: detect conservativity principle violations and disjointness axioms
		FileUtil.writeLogAndConsole(
				"STEP 5: detect conservativity principle violations and disjointness axioms - " + 
						Util.getCurrTime());
		start = Util.getMSec();
		long disjComputTime = Util.getMSec();

		FileUtil.writeLogAndConsole("Detection of violations kind 1");

		Pair<List<Pair<Integer>>> viols = 
				LogMapWrapper.parallelConservativityViolationDetection(index, 
						fstProc, sndProc, alignIdx, alignProc, rootViolations, 
						false, suppressOutputViol, fstO,sndO,alignOnto);
		disjComputTime = Util.getDiffmsec(disjComputTime);

		int initialViol1 = viols.getFirst().size();
		int initialViol2 = viols.getSecond().size();

		// new notion of conservativity
		if(Params.reasonerViolationsCheck && alignReasoner != null){
			List<Pair<OWLClass>> o1 = 
					LogMapWrapper.detectConservativityViolationUsingReasoner(
							fstO,alignOnto,fstReasoner,alignReasoner,manager,
							rootViolations,false);

			List<Pair<OWLClass>> o2 = 
					LogMapWrapper.detectConservativityViolationUsingReasoner(
							sndO,alignOnto,sndReasoner,alignReasoner,manager,
							rootViolations,false);

			LogMapWrapper.compareViolationsIndexReasoner(
					viols.getFirst(), o1, origIndex, fstProcOrig, 0, alignIdx, 
					alignProc, fstReasoner, alignReasoner, rootViolations);

			LogMapWrapper.compareViolationsIndexReasoner(
					viols.getSecond(), o2, origIndex, sndProcOrig, 1, alignIdx, 
					alignProc, sndReasoner, alignReasoner, rootViolations);
		}

		if(Params.explanationBasedDirectViolationsCheck){
			List<Pair<OWLClass>> dir1R = OntoUtil.explanationDetectionDirectViolations(fstO, 
					alignOnto, viols.getFirst(), index, 1, alignReasoner, false);
			List<Pair<OWLClass>> dir2R = OntoUtil.explanationDetectionDirectViolations(sndO, 
					alignOnto, viols.getSecond(), index, 2, alignReasoner, false);

			Pair<List<Pair<Integer>>> directViolIndex = 
					LogMapWrapper.parallelConservativityViolationDetection(
							origIndex, fstProcOrig, sndProcOrig, alignIdx, 
							alignProc, true, false, false, fstO,sndO,alignOnto);

			Pair<List<Pair<OWLClass>>> dirIndexClass = new Pair<>(
					LogMapWrapper.getOWLClassFromIndexPair(
							viols.getFirst(), index), 
							LogMapWrapper.getOWLClassFromIndexPair(
									viols.getSecond(), index));

			dirIndexClass = OntoUtil.graphDetectionDirectViolations(fstO, sndO, 
					alignOnto, dirIndexClass);

			FileUtil.writeLogAndConsole("1) Explanations = " + dir1R.size() 
					+ ", index = " + directViolIndex.getFirst().size() 
					+ ", graph = " + dirIndexClass.getFirst().size());

			OntoUtil.compareDirectViolations(dir1R, directViolIndex.getFirst(), 
					index);
			
			OntoUtil.compareDirectViolations(dir1R, 
					"explanations", dirIndexClass.getFirst(), "graph");

			FileUtil.writeLogAndConsole("2) Explanations = " + dir2R.size() 
					+ ", index = " + directViolIndex.getSecond().size()
					+ ", graph = " + dirIndexClass.getSecond().size());

			OntoUtil.compareDirectViolations(dir2R, directViolIndex.getSecond(), 
					index);
			
			OntoUtil.compareDirectViolations(dir2R, 
					"explanations", dirIndexClass.getSecond(), "graph");
		}

		if(!rootViolations && Params.checkDirectUnsolvedViolations){
			FileUtil.writeLogAndConsole("** START ** DIRECT violations kind 1");
			// using the index+graph-based method!
			LogMapWrapper.parallelConservativityViolationDetection(
							origIndex, fstProcOrig, sndProcOrig, alignIdx, 
							alignProc, true, false, suppressOutputViol,
							fstO,sndO,alignOnto);
			FileUtil.writeLogAndConsole("** END ** DIRECT violations kind 1");
		}
		
		/****** COLUMN 5 = viol pre 1, COLUMN 6 = viol pre 2 *********/
		FileUtil.writeDataOutFile(initialViol1 + " " + initialViol2 + " ");

		FileUtil.writeLogAndConsole("Detection of violations kind 2");
		// detect also full conservativity violations including equivalences
		Pair<List<Pair<Integer>>> violsEq = 
				LogMapWrapper.parallelConservativityViolationDetection(index, 
						fstProc, sndProc, alignIdx, alignProc, rootViolations, 
						true, suppressOutputViol, fstO,sndO,alignOnto);

		int initialViolEq1 = violsEq.getFirst().size();
		int initialViolEq2 = violsEq.getSecond().size();

		// full notion of conservativity
		if(Params.reasonerViolationsCheck && alignReasoner != null){
			List<Pair<OWLClass>> o1 = 
					LogMapWrapper.detectConservativityViolationUsingReasoner(
							fstO,alignOnto,fstReasoner,alignReasoner,manager,
							rootViolations,true);
			List<Pair<OWLClass>> o2 = 
					LogMapWrapper.detectConservativityViolationUsingReasoner(
							sndO,alignOnto,sndReasoner,alignReasoner,manager,
							rootViolations,true);

			LogMapWrapper.compareViolationsIndexReasoner(
					violsEq.getFirst(), o1, origIndex, fstProcOrig, 0, alignIdx,
					alignProc, fstReasoner, alignReasoner, rootViolations);

			LogMapWrapper.compareViolationsIndexReasoner(
					violsEq.getSecond(), o2, origIndex, sndProcOrig, 1, alignIdx, 
					alignProc, sndReasoner, alignReasoner, rootViolations);
		}
		violsEq = null;
		
		if(!rootViolations && Params.checkDirectUnsolvedViolations){
			FileUtil.writeLogAndConsole("** START ** DIRECT violations kind 2");
			// using the index+graph-based method!
			LogMapWrapper.parallelConservativityViolationDetection(
							origIndex, fstProcOrig, sndProcOrig, alignIdx, 
							alignProc, true, true, suppressOutputViol,
							fstO,sndO,alignOnto);
			FileUtil.writeLogAndConsole("** END ** DIRECT violations kind 2");
		}

		/****** COLUMN 7 = viol pre 1 equiv, COLUMN 8 = viol pre 2  equiv *********/
		FileUtil.writeDataOutFile(initialViolEq1 + " " + initialViolEq2 + " ");

//		FileUtil.writeLogAndConsole("Detection of violations kind 3");
//
//		List<Pair<Integer>> violsFst, violsSnd;
//
//		Pair<List<Pair<Integer>>> listPair = 
//				LogMapWrapper.parallelConservativityViolationDetection(index,
//						fstProc,sndProc,fstMappingIndex,fstMappingProc,
//						sndMappingIndex,sndMappingProc, rootViolations,true,
//						suppressOutputViol);		
//
//		violsFst = listPair.getFirst();
//		violsSnd = listPair.getSecond();
//
//
//		// old notion of conservativity (1 ontology + mappings)
//		if(Params.reasonerViolationsCheck && fstMappingReasoner != null && 
//				sndMappingReasoner != null){
//			List<Pair<OWLClass>> o1 = 
//					LogMapWrapper.detectConservativityViolationUsingReasoner(
//							fstO,fstOntoMapping,fstReasoner,
//							fstMappingReasoner,manager,
//							rootViolations,true);
//			List<Pair<OWLClass>> o2 = 
//					LogMapWrapper.detectConservativityViolationUsingReasoner(
//							sndO,sndOntoMapping,sndReasoner,
//							sndMappingReasoner,manager,
//							rootViolations,true);
//
//			LogMapWrapper.compareViolationsIndexReasoner(
//					violsFst, o1, origIndex, fstProcOrig, 0, alignIdx, alignProc, 
//					fstReasoner, fstMappingReasoner, rootViolations);
//
//			LogMapWrapper.compareViolationsIndexReasoner(
//					violsSnd, o2, origIndex, sndProcOrig, 1, alignIdx, alignProc, 
//					sndReasoner, sndMappingReasoner, rootViolations);
//		}
//
//		int initialViolFst = violsFst.size();		
//		int initialViolSnd = violsSnd.size();
//
//		violsFst = null;
//		violsSnd = null;
//
//		/****** COLUMN 9 = viol pre 1 only mapping, COLUMN 10 = viol pre 2 only mapping *********/
//		FileUtil.writeDataOutFile(initialViolFst + " " + initialViolSnd + " ");

		/****** COLUMN 9 = viol pre 1 only mapping, COLUMN 10 = viol pre 2 only mapping *********/
		FileUtil.writeDataOutFile(-1 + " " + -1 + " ");
		
		FileUtil.writeLogAndConsole("STEP 5 (new notion only): " 
				+ disjComputTime + " (ms)");

		FileUtil.writeLogAndConsole("STEP 5: " + Util.getDiffmsec(start) 
				+ " (ms) - " + Util.getCurrTime() + "\n");

		// STEP 6: adding disjointness axioms to input ontologies
		FileUtil.writeLogAndConsole(
				"STEP 6: adding disjointness axioms to input ontologies - " + 
						Util.getCurrTime());
		start = Util.getMSec();

		int disjNum1 = 0, disjNum2 = 0;
		if(!fullDisj){
			disjNum1 = viols.getFirst().size();
			disjNum2 = viols.getSecond().size();

			for (Pair<Integer> p : viols.getFirst()){
				if(disjInAligned){
					Pair<String> iris = LogMapWrapper.getIRIStrFromIndexPair(p,index); 
					//Pair<OWLClass> cls = LogMapWrapper.getOWLClassFromIndexPair(p,index);
					alignIdx.addDisjointness(
							alignProc.getIdentifier4ConceptIRI(iris.getFirst()),
							alignProc.getIdentifier4ConceptIRI(iris.getSecond()),
							//							alignProc.getClass2Identifier().get(cls.getFirst()), 
							//							alignProc.getClass2Identifier().get(cls.getSecond()), 
							false);
				}
				else 
					index.addDisjointness(p.getFirst(), p.getSecond(), false);
			}
			for (Pair<Integer> p : viols.getSecond()){

				if(disjInAligned){
					Pair<OWLClass> cls = LogMapWrapper.getOWLClassFromIndexPair(p,index);
					alignIdx.addDisjointness(
							alignProc.getClass2Identifier().get(cls.getFirst()), 
							alignProc.getClass2Identifier().get(cls.getSecond()), 
							false);
				}
				else
					index.addDisjointness(p.getFirst(), p.getSecond(), false);
			}
			if(disjInAligned)
				alignIdx.recreateDisjointIntervalsStructure();
			else
				index.recreateDisjointIntervalsStructure();

			disjComputTime += Util.getDiffmsec(start);

			FileUtil.writeLogAndConsole(viols.getFirst().size() 
					+ viols.getSecond().size() + " disjoint axioms added");
		}
		else {
			disjComputTime = Util.getMSec();
			//			if(fstOnto && sndOnto){
			Pair<Integer> disj = LogMapWrapper.parallelDisjointnessEnforcement(
					index, avoidLeafSiblings);
			disjNum1 = disj.getFirst();
			disjNum2 = disj.getSecond();
			//			}
			//			else if(fstOnto)
			//disjNum1 = LogMapWrapper.enforceDisjointnessIntoIndex(index,0,avoidLeafSiblings);
			//			else if(sndOnto)
			//disjNum2 = LogMapWrapper.enforceDisjointnessIntoIndex(index,1,avoidLeafSiblings);
			disjComputTime = Util.getDiffmsec(disjComputTime);

			FileUtil.writeLogAndConsole(disjNum1 + disjNum2 + " disjoint axioms added");
		}
		viols = null;
		/****** COLUMN 11 = disj. axioms onto 1, COLUMN 12 =  disj. axioms onto 2, COLUMN 13 = DISJTIME *********/
		FileUtil.writeDataOutFile(disjNum1 + " " + disjNum2 + " " + disjComputTime + " ");

		FileUtil.writeLogAndConsole("STEP 6: " + Util.getDiffmsec(start) 
				+ " (ms) - " + Util.getCurrTime() + "\n");

		// STEP 7: repair conservativity
		FileUtil.writeLogAndConsole("STEP 7: repair conservativity - " + Util.getCurrTime());
		start = Util.getMSec();

		Set<MappingObjectStr> singlestepRepair, multistepRepair;

		// repair consistency + conservativity at the same time (use original align)
		long repairTime = Util.getMSec();
		if(disjInAligned)
			singlestepRepair = LogMapWrapper.repairAlignmentAlignOntoIndex(
					fstO, sndO,mappingFile, alignProc, alignIdx, null);
		else
			singlestepRepair = LogMapWrapper.repairInconsistentAlignments(fstO, 
					sndO, mappingFile, fstProc, sndProc, index, null);
		repairTime = Util.getDiffmsec(repairTime);

		/****** COLUMN 12 = repair_both time, COLUMN 13 = |R|_both *********/
		FileUtil.writeDataOutFile(repairTime + " " + (
				LogMapWrapper.countMappings(originalMappings) - 
				LogMapWrapper.countMappings(singlestepRepair)) + " ");

		// if the original alignment was consistent the second repair is not needed
		if(originalMappings.size() == consistentMappings.size()){
			// ok to share, never modified
			multistepRepair = singlestepRepair;
		}
		else {
			// repair conservativity only (takes as input a coherent alignment)
			repairTime = Util.getMSec();
			if(disjInAligned)
				multistepRepair = LogMapWrapper.repairAlignmentAlignOntoIndex(fstO, sndO, 
						mappingFile, alignProc, alignIdx, consistentMappings);
			else
				multistepRepair = LogMapWrapper.repairInconsistentAlignments(fstO, 
						sndO, mappingFile, fstProc, sndProc, index, consistentMappings);
			repairTime = Util.getDiffmsec(repairTime);
		}
		/****** COLUMN 14 = repair_conservativity time, COLUMN 15 = |R|_conservativity *********/
		FileUtil.writeDataOutFile(repairTime + " " + (
				LogMapWrapper.countMappings(originalMappings) - 
				LogMapWrapper.countMappings(multistepRepair)) + " ");

		FileUtil.writeLogAndConsole("STEP 7: " + Util.getDiffmsec(start) 
				+ " (ms) - " + Util.getCurrTime() + "\n");

		OntoUtil.chooseReasoner(mappingFile, trackName);

		// choose the repair according to the desired repair strategy 
		consistentMappings = Params.repairStrategy.equals(
				REPAIR_STRATEGY.CONSIST_THEN_CONSERV) ? 
						multistepRepair : singlestepRepair;

		Set<OWLAxiom> diagnosis = new HashSet<>(originalAlignment);

		diagnosis.removeAll(OntoUtil.convertAlignmentToAxioms(fstO, sndO, 
				consistentMappings));

		if(Params.saveMappings){
			FileUtil.writeLogAndConsole("Serializing the repaired mappings");
			LogMapWrapper.saveMappings(Params.test7MappingDir + 
					mappingName.replace(".rdf", "") + (fullDisj ? "-full" : "-light"), 
					fstO, sndO, consistentMappings);
		}
		
		// STEP 8: update the aligned ontology for testing unsolved violations
		FileUtil.writeLogAndConsole(
				"STEP 8: update the aligned ontology for testing unsolved violations - " + 
						Util.getCurrTime());
		start = Util.getMSec();
		if(Params.bufferingReasoner){
			//TODO: it is not removing yet the result of the previous classification!

			// recompute aligned ontology with repaired mappings
			OntoUtil.removeAxiomsFromOntology(alignOnto, manager, diagnosis, false);
//			OntoUtil.removeAxiomsFromOntology(fstOntoMapping, manager, diagnosis, true);
//			OntoUtil.removeAxiomsFromOntology(sndOntoMapping, manager, diagnosis, true);
			FileUtil.writeLogAndConsole(diagnosis.size() + " axioms should have been removed");		
			FileUtil.writeLogAndConsole("Diagnosis: " + diagnosis);

//			try {
//				fstMappingReasoner.flush();				
//			}
//			catch (org.mindswap.pellet.exceptions.TimeoutException e){
//				OntoUtil.disposeReasoners(fstMappingReasoner);
//				fstMappingReasoner = OntoUtil.getReasoner(fstOntoMapping, 
//						Params.reasonerKind, manager);
//			}
//
//			try {
//				sndMappingReasoner.flush();				
//			}
//			catch (org.mindswap.pellet.exceptions.TimeoutException e){				
//				OntoUtil.disposeReasoners(sndMappingReasoner);
//				sndMappingReasoner = OntoUtil.getReasoner(sndOntoMapping, 
//						Params.reasonerKind, manager);
//			}

			try {
				if(alignReasoner != null)
					alignReasoner.flush();
			}
			catch(org.mindswap.pellet.exceptions.TimeoutException e){
				OntoUtil.disposeReasoners(alignReasoner);
				alignReasoner = OntoUtil.getReasoner(alignOnto, 
						Params.reasonerKind, manager);
			}

			reasoners.clear();
//			reasoners.add(fstMappingReasoner);
//			reasoners.add(sndMappingReasoner);
//
//			OntoUtil.ontologyClassification(false, false, reasoners, Params.tryPellet);
//			OntoUtil.checkClassification(reasoners);
//
//			fstMappingReasoner = reasoners.get(0);
//			sndMappingReasoner = reasoners.get(1);
//
//			if(!fstMappingReasoner.getRootOntology().equals(fstOntoMapping))
//				throw new Error("First ontology + mappings for reasoner mismatch");			
//
//			if(!sndMappingReasoner.getRootOntology().equals(sndOntoMapping))
//				throw new Error("Second ontology + mappings for reasoner mismatch");
//
//			reasoners.clear();

			if(alignReasoner != null){
				reasoners.add(alignReasoner);

				OntoUtil.ontologyClassification(true, false, reasoners, Params.tryPellet);
				OntoUtil.checkClassification(reasoners);

				alignReasoner = reasoners.get(0);
				reasoners.clear();

				if(!alignReasoner.getRootOntology().equals(alignOnto))
					throw new Error("Align ontology for reasoner mismatch");
			}
		}
		else {
			// otherwise they may complain for the ontology removal
//			OntoUtil.disposeReasoners(fstMappingReasoner, sndMappingReasoner, 
//					alignReasoner);

			manager.removeOntology(alignOnto);
//			manager.removeOntology(fstOntoMapping);
//			manager.removeOntology(sndOntoMapping);

			Set<OWLAxiom> cleanMappingAxioms = 
					OntoUtil.convertAlignmentToAxioms(fstO, sndO, 
							consistentMappings);

			alignOnto = OntoUtil.getAlignedOntology(manager, 
					cleanMappingAxioms, fstO, sndO);

//			fstOntoMapping = OntoUtil.getAlignedOntology(cleanMappingAxioms, 
//					fstO);
//
//			sndOntoMapping = OntoUtil.getAlignedOntology(cleanMappingAxioms, 
//					sndO);
//
//			fstMappingReasoner = OntoUtil.getReasoner(fstOntoMapping, 
//					Params.reasonerKind, manager);
//
//			sndMappingReasoner = OntoUtil.getReasoner(sndOntoMapping, 
//					Params.reasonerKind, manager);
//
//			reasoners.clear();
//			reasoners.add(fstMappingReasoner);
//			reasoners.add(sndMappingReasoner);
//
//			OntoUtil.ontologyClassification(false, false, reasoners, Params.tryPellet);
//			OntoUtil.checkClassification(reasoners);
//
//			fstMappingReasoner = reasoners.get(0);
//			sndMappingReasoner = reasoners.get(1);

			reasoners.clear();

//			if(!fstMappingReasoner.getRootOntology().equals(fstOntoMapping))
//				throw new Error("First ontology + mappings for reasoner mismatch");
//
//			if(!sndMappingReasoner.getRootOntology().equals(sndOntoMapping))
//				throw new Error("Second ontology + mappings for reasoner mismatch");			

			alignReasoner = OntoUtil.getReasoner(alignOnto, Params.reasonerKind, 
					manager);

			reasoners.add(alignReasoner);

			OntoUtil.ontologyClassification(true, false, reasoners, Params.tryPellet);
			OntoUtil.checkClassification(reasoners);

			alignReasoner = reasoners.get(0);

			if(!alignReasoner.getRootOntology().equals(alignOnto))
				throw new Error("Align ontology for reasoner mismatch");

			reasoners.clear();

			try {
				OntoUtil.saveClassificationAxioms(alignOnto, alignReasoner, manager);
//				OntoUtil.saveClassificationAxioms(fstOntoMapping, fstMappingReasoner);
//				OntoUtil.saveClassificationAxioms(sndOntoMapping, sndMappingReasoner);
			}
			catch(org.semanticweb.owlapi.reasoner.InconsistentOntologyException | 
					NullPointerException e){
				FileUtil.writeErrorLogAndConsole("Impossible to save the " +
						"inferred axioms into the ontologies:\n" + e.getMessage() 
						+ ",\n skipping this test");
				OntoUtil.disposeAllReasoners();
				return;
			}

			// we use an extended version of ELK also providing direct disjoint classes
			//if(OntoUtil.isELKReasoner(alignReasoner))
//			alignReasoner = new ExtDisjReasoner(alignReasoner);
		}

		FileUtil.writeLogAndConsole("STEP 8: " + Util.getDiffmsec(start) 
				+ " (ms) - " + Util.getCurrTime() + "\n");

		if(saveOnto){
			FileUtil.writeLogAndConsole("Saving classified repaired aligned ontology");
			String ontoName = mappingFile.substring(
					mappingFile.lastIndexOf('/')+1, 
					mappingFile.lastIndexOf('.'));
			try {
				OntoUtil.save(alignOnto, "/home/ale/Downloads/ontologies/aligned/" 
						+ ontoName + "-post.owl", manager);
			} catch (OWLOntologyStorageException | OWLOntologyCreationException
					| IOException e) {
				e.printStackTrace();
			}
		}

		// STEP 9: detect unsolved violations using semantic indexes
		FileUtil.writeLogAndConsole(
				"STEP 9: detect unsolved violations using semantic indexes - " + 
						Util.getCurrTime());
		start = Util.getMSec();

		if(checkEntailment && alignReasoner != null){
			FileUtil.writeLogAndConsole("TEST IF REMOVED MAPPINGS ARE STILL ENTAILED");

			if(!alignReasoner.isConsistent())
				FileUtil.writeLogAndConsole("INCONSISTENT ALIGNED ONTOLOGY");
			else {
				for (OWLAxiom owlAxiom : diagnosis) {
					if(alignReasoner.isEntailed(owlAxiom)){
						FileUtil.writeLogAndConsole(OntoUtil.computeExplanations(
								manager, owlAxiom, alignReasoner).toString());
					}
					else
						FileUtil.writeLogAndConsole(owlAxiom + " NOT entailed");
				}
			}
			FileUtil.writeLogAndConsole("Diagnosis entailment (ms): " 
					+ Util.getDiffmsec(start));

			start = Util.getMSec();
		}

		alignIdx = new JointIndexManager(); 
		alignProc = LogMapWrapper.indexSetup(alignOnto, alignIdx, alignReasoner);
//		fstMappingIndex = new JointIndexManager();
//		sndMappingIndex = new JointIndexManager();
//		fstMappingProc = LogMapWrapper.indexSetup(fstOntoMapping,
//				fstMappingIndex, fstMappingReasoner);
//		sndMappingProc = LogMapWrapper.indexSetup(sndOntoMapping,
//				sndMappingIndex, sndMappingReasoner);

		if(trackName.equals("conference") || trackName.equals("anatomy")){
			FileUtil.writeLogAndConsole("Pre-detection input ontology index sanity check");
			if(!origIndex.sanityCheck()){
				FileUtil.writeErrorLogAndConsole("Index sanity check failed, skipping the test");
				OntoUtil.disposeAllReasoners();
				return;
			}
			FileUtil.writeLogAndConsole("Pre-detection aligned ontology index sanity check");
			if(!alignIdx.sanityCheck()){
				FileUtil.writeErrorLogAndConsole("Index sanity check failed, skipping the test");
				OntoUtil.disposeAllReasoners();
				return;
			}
		}

		FileUtil.writeLogAndConsole("Detection of violations kind 1");
		Pair<List<Pair<Integer>>> unsolvViol = 
				LogMapWrapper.parallelConservativityViolationDetection(
						origIndex, fstProcOrig, sndProcOrig, alignIdx, 
						alignProc, rootViolations, false, suppressOutputViol, 
						fstO,sndO,alignOnto);

		/****** COLUMN 16 = viol post 1, COLUMN 17 = viol post 2 *********/
		FileUtil.writeDataOutFile(unsolvViol.getFirst().size() + " " 
				+ unsolvViol.getSecond().size() + " ");

		int unsolvedViolKind1 = unsolvViol.getFirst().size() 
				+ unsolvViol.getSecond().size(); 
		if(unsolvedViolKind1 > 0)
			FileUtil.writeErrorLogAndConsole(unsolvedViolKind1 + "/" 
					+ (initialViol1+initialViol2) 
					+ " unsolved violation(s)");

		if(Params.reasonerViolationsCheck && alignReasoner != null){
			List<Pair<OWLClass>> o1 = 
					LogMapWrapper.detectConservativityViolationUsingReasoner(
							fstO,alignOnto,fstReasoner,alignReasoner,manager,
							rootViolations,false);
			List<Pair<OWLClass>> o2 = 
					LogMapWrapper.detectConservativityViolationUsingReasoner(
							sndO,alignOnto,sndReasoner,alignReasoner,manager,
							rootViolations,false);

			LogMapWrapper.compareViolationsIndexReasoner(
					unsolvViol.getFirst(), o1, origIndex, fstProcOrig, 0, 
					alignIdx, alignProc, fstReasoner, alignReasoner, rootViolations);

			LogMapWrapper.compareViolationsIndexReasoner(
					unsolvViol.getSecond(), o2, origIndex, sndProcOrig, 1, 
					alignIdx, alignProc, sndReasoner, alignReasoner, rootViolations);
		}

		if(Params.explanationBasedDirectViolationsCheck){
			List<Pair<OWLClass>> dir1R = OntoUtil.explanationDetectionDirectViolations(fstO, 
					alignOnto, unsolvViol.getFirst(), index, 1, alignReasoner, 
					true);
			List<Pair<OWLClass>> dir2R = OntoUtil.explanationDetectionDirectViolations(sndO, 
					alignOnto, unsolvViol.getSecond(), index, 2, alignReasoner, 
					true);

			Pair<List<Pair<Integer>>> directViolIndex = 
					LogMapWrapper.parallelConservativityViolationDetection(
							origIndex, fstProcOrig, sndProcOrig, alignIdx, 
							alignProc, true, false, suppressOutputViol,
							fstO,sndO,alignOnto);

			Pair<List<Pair<OWLClass>>> dirIndexClass = new Pair<>(
					LogMapWrapper.getOWLClassFromIndexPair(
							unsolvViol.getFirst(), index), 
							LogMapWrapper.getOWLClassFromIndexPair(
									unsolvViol.getSecond(), index));

			dirIndexClass = OntoUtil.graphDetectionDirectViolations(fstO, sndO, 
					alignOnto, dirIndexClass);
			
			FileUtil.writeLogAndConsole("1) Explanations = " + dir1R.size() 
					+ ", index = " + directViolIndex.getFirst().size()
					+ ", graph = " + dirIndexClass.getFirst().size());

			OntoUtil.compareDirectViolations(dir1R, directViolIndex.getFirst(), 
					index);

			OntoUtil.compareDirectViolations(dir1R, 
					"explanations", dirIndexClass.getFirst(), "graph");
			
			FileUtil.writeLogAndConsole("2) Explanations = " + dir2R.size() 
					+ ", index = " + directViolIndex.getSecond().size()
					+ ", graph = " + dirIndexClass.getSecond().size());

			OntoUtil.compareDirectViolations(dir2R, directViolIndex.getSecond(), 
					index);
			
			OntoUtil.compareDirectViolations(dir2R, 
					"explanations", dirIndexClass.getSecond(), "graph");
		}

		if(!rootViolations && Params.checkDirectUnsolvedViolations && 
				unsolvedViolKind1 > 0){
			// using the index+graph-based method!
			Pair<List<Pair<Integer>>> unsolvDirViol = 
					LogMapWrapper.parallelConservativityViolationDetection(
							origIndex, fstProcOrig, sndProcOrig, alignIdx, 
							alignProc, true, false, suppressOutputViol,
							fstO,sndO,alignOnto);

			int unsolvedDirViolKind1 = unsolvDirViol.getFirst().size() 
					+ unsolvDirViol.getSecond().size();

			if(unsolvedDirViolKind1 > 0)
				FileUtil.writeErrorLogAndConsole(unsolvedDirViolKind1 + "/" 
						+ (initialViol1+initialViol2) 
						+ " unsolved violation(s) (DIRECT)");
		}

		FileUtil.writeLogAndConsole("Detection of violations kind 2");
		// the same considering also violations due to equivalences 
		unsolvViol = LogMapWrapper.parallelConservativityViolationDetection(
				origIndex, fstProcOrig, sndProcOrig, alignIdx, 
				alignProc, rootViolations, true, suppressOutputViol,
				fstO,sndO,alignOnto);

		if(Params.reasonerViolationsCheck && alignReasoner != null){
			List<Pair<OWLClass>> o1 = 
					LogMapWrapper.detectConservativityViolationUsingReasoner(
							fstO,alignOnto,fstReasoner,alignReasoner,manager,
							rootViolations,true);
			List<Pair<OWLClass>> o2 = 
					LogMapWrapper.detectConservativityViolationUsingReasoner(
							sndO,alignOnto,sndReasoner,alignReasoner,manager,
							rootViolations,true);

			LogMapWrapper.compareViolationsIndexReasoner(
					unsolvViol.getFirst(), o1, origIndex, fstProcOrig, 0, 
					alignIdx, alignProc, fstReasoner, alignReasoner, rootViolations);

			LogMapWrapper.compareViolationsIndexReasoner(
					unsolvViol.getSecond(), o2, origIndex, sndProcOrig, 1, 
					alignIdx, alignProc, sndReasoner, alignReasoner, rootViolations);
		}

		if(Params.explanationBasedDirectViolationsCheck){
			List<Pair<OWLClass>> dir1R = 
					OntoUtil.explanationDetectionDirectViolations(fstO, alignOnto, 
							unsolvViol.getFirst(), index, 1, alignReasoner, true);
			List<Pair<OWLClass>> dir2R = 
					OntoUtil.explanationDetectionDirectViolations(sndO, alignOnto, 
							unsolvViol.getSecond(), index, 2, alignReasoner, true);

			Pair<List<Pair<Integer>>> directViolIndex = 
					LogMapWrapper.parallelConservativityViolationDetection(
							origIndex, fstProcOrig, sndProcOrig, alignIdx, 
							alignProc, true, true, suppressOutputViol,
							fstO,sndO,alignOnto);
			
			Pair<List<Pair<OWLClass>>> dirIndexClass = new Pair<>(
					LogMapWrapper.getOWLClassFromIndexPair(
							unsolvViol.getFirst(), index), 
							LogMapWrapper.getOWLClassFromIndexPair(
									unsolvViol.getSecond(), index));

			dirIndexClass = OntoUtil.graphDetectionDirectViolations(fstO, sndO, 
					alignOnto, dirIndexClass);

			FileUtil.writeLogAndConsole("1) Explanations = " + dir1R.size() 
					+ ", index = " + directViolIndex.getFirst().size()
					+ ", graph = " + dirIndexClass.getFirst().size());

			OntoUtil.compareDirectViolations(dir1R, directViolIndex.getFirst(), 
					index);

			OntoUtil.compareDirectViolations(dir1R, 
					"explanations", dirIndexClass.getFirst(), "graph");
			
			FileUtil.writeLogAndConsole("2) Explanations = " + dir2R.size() 
					+ ", index = " + directViolIndex.getSecond().size()
					+ ", graph = " + dirIndexClass.getSecond().size());

			OntoUtil.compareDirectViolations(dir2R, directViolIndex.getSecond(), 
					index);
			
			OntoUtil.compareDirectViolations(dir2R, 
					"explanations", dirIndexClass.getSecond(), "graph");
		}

		/****** COLUMN 18 = viol post 1 equiv, COLUMN 19 = viol post 2 equiv *********/
		FileUtil.writeDataOutFile(unsolvViol.getFirst().size() + " " 
				+ unsolvViol.getSecond().size() + " ");

		int unsolvedViolKind2 = unsolvViol.getFirst().size() 
				+ unsolvViol.getSecond().size();

		if(unsolvedViolKind2 > 0)
			FileUtil.writeErrorLogAndConsole(unsolvedViolKind2 + "/" 
					+ (initialViolEq1+initialViolEq2) 
					+ " unsolved violation(s) considering equivalences");

		unsolvViol = null;

		if(!rootViolations && Params.checkDirectUnsolvedViolations && 
				unsolvedViolKind2 > 0){
			// using the index+graph-based method!
			Pair<List<Pair<Integer>>> unsolvDirViol = 
					LogMapWrapper.parallelConservativityViolationDetection(
							origIndex, fstProcOrig, sndProcOrig, alignIdx, 
							alignProc, true, true, suppressOutputViol,
							fstO,sndO,alignOnto);

			int unsolvedDirViolKind2 = unsolvDirViol.getFirst().size() 
					+ unsolvDirViol.getSecond().size();

			if(unsolvedDirViolKind2 > 0)
				FileUtil.writeErrorLogAndConsole(unsolvedDirViolKind2 + "/" 
						+ (initialViolEq1+initialViolEq2) 
						+ " unsolved violation(s) considering equivalences (DIRECT)");
		}

//		FileUtil.writeLogAndConsole("Detection of violations kind 3");
//
//		listPair = LogMapWrapper.parallelConservativityViolationDetection(index,
//				fstProc,sndProc,fstMappingIndex,fstMappingProc,sndMappingIndex, 
//				sndMappingProc, rootViolations,true,suppressOutputViol);	
//
//		violsFst = listPair.getFirst();
//		violsSnd = listPair.getSecond();
//		
//		int unsolvedViolKind3 = violsFst.size() + violsSnd.size(); 
//		if(unsolvedViolKind3 > 0)
//			FileUtil.writeErrorLogAndConsole(unsolvedViolKind3 + "/" 
//					+ (initialViolFst+initialViolSnd) 
//						+ " unsolved violation(s) original conservativity definition");
//
//		/****** COLUMN 20 = viol post 1 only mappings, COLUMN 21 = viol post 2 only mappings *********/
//		FileUtil.writeDataOutFile(violsFst.size() + " " +  violsSnd.size() + " ");

		/****** COLUMN 20 = viol post 1 only mappings, COLUMN 21 = viol post 2 only mappings *********/
		FileUtil.writeDataOutFile(-1 + " " +  -1 + " ");
		
//		// old notion of conservativity (1 ontology + mappings)
//		if(Params.reasonerViolationsCheck && alignReasoner != null){
//			List<Pair<OWLClass>> o1 = 
//					LogMapWrapper.detectConservativityViolationUsingReasoner(
//							fstO,fstOntoMapping,fstReasoner,
//							fstMappingReasoner,manager,
//							rootViolations,true);
//			List<Pair<OWLClass>> o2 = 
//					LogMapWrapper.detectConservativityViolationUsingReasoner(
//							sndO,sndOntoMapping,sndReasoner,
//							sndMappingReasoner,manager,
//							rootViolations,true);
//
//			LogMapWrapper.compareViolationsIndexReasoner(
//					violsFst, o1, origIndex, fstProcOrig, 0, alignIdx, alignProc, 
//					fstReasoner, fstMappingReasoner, rootViolations);
//
//			LogMapWrapper.compareViolationsIndexReasoner(
//					violsSnd, o2, origIndex, sndProcOrig, 1, alignIdx, alignProc, 
//					sndReasoner, sndMappingReasoner, rootViolations);
//		}

		FileUtil.writeLogAndConsole("STEP 9: " + Util.getDiffmsec(start) 
				+ " (ms) - " + Util.getCurrTime() + "\n");

		OntoUtil.disposeAllReasoners();

		if((unsolvedViolKind1 > (initialViol1+initialViol2)) ||
				(unsolvedViolKind2 > (initialViolEq1+initialViolEq2))) 
//				|| (unsolvedViolKind3 > (initialViolFst+initialViolSnd)))
			FileUtil.writeErrorLogAndConsole("Increased number of violations after repair!");

		/****** COLUMN 22 = aligned ontology classification time *********/
		FileUtil.writeDataOutFile(alignClassifTime + " ");

		/****** COLUMN 23-24 = signature size first-second ontology  ********/
		FileUtil.writeDataOutFileNL(signSizeFst + " " + signSizeSnd);

		FileUtil.flushWriteDataOutFile();
		OntoUtil.unloadAllOntologies(manager);
		OntoUtil.checkActiveReasoners(true);
	}
	
	private static boolean checkIncoherentClasses(){

		int unsats = alignReasoner.getUnsatisfiableClasses().getEntitiesMinusBottom().size();
		int steps = 0;
		while(Params.fullReasoningRepair && unsats > 0){
			steps++;
			FileUtil.writeLogAndConsole("Unsatisfiable classes (step " + 
					steps + "): " + unsats);

			Set<MappingObjectStr> repair = 
					OntoUtil.repairUnsatisfiabilitiesFullReasoning(
							alignReasoner.getUnsatisfiableClasses().getEntities(), 
							fstO, sndO, alignOnto, consistentMappings, useELK);
			
			FileUtil.writeLogAndConsole("Full reasoning repair: " + repair);
			FileUtil.writeLogAndConsole("Full reasoning repair size: " + 
					LogMapWrapper.countMappings(repair));
			
			LogMapWrapper.applyRepair(consistentMappings,repair);
			
			updateAlignedOntology(false);
			
			unsats = alignReasoner.getUnsatisfiableClasses().getEntitiesMinusBottom().size();
		}

		if(unsats == 0){			
			incoherences.add(
					alignReasoner.getUnsatisfiableClasses().getEntitiesMinusBottom().size());
			
			if(!alignReasoner.getUnsatisfiableClasses().isSingleton()){
				FileUtil.writeErrorLogAndConsole(
						alignReasoner.getUnsatisfiableClasses().getEntitiesMinusBottom().size() 
						+ " incoherent class(es): ");
				
				for (OWLClass c : 
					alignReasoner.getUnsatisfiableClasses().getEntitiesMinusBottom())
					FileUtil.writeErrorLogAndConsole(c.toString());
			}
			if(incoherences.size() > 1){
				int act = incoherences.get(incoherences.size()-1);
				int pre = incoherences.get(incoherences.size()-2);
				
				if(act > pre)
					FileUtil.writeErrorLogAndConsole("Incoherent classes increased " +
							"from " + pre + " to " + act);
			}
		}
		
		return steps == 0;
	}

	private static boolean updateAlignedOntology(boolean checkUnsat){
		// STEP 8: update the aligned ontology for testing unsolved violations
		if(checkUnsat)
			FileUtil.writeLogAndConsole("STEP: " +
				"update the aligned ontology for testing unsolved violations - " + 
				Util.getCurrTime());

		long start = Util.getMSec();

		// otherwise they may complain for the ontology removal
		OntoUtil.disposeReasoners(alignReasoner);

		manager.removeOntology(alignOnto);

		Set<OWLAxiom> cleanMappingAxioms = 
				OntoUtil.convertAlignmentToAxioms(fstO, sndO, consistentMappings);
		
		FileUtil.writeLogAndConsole(cleanMappingAxioms.size() + " mapping(s)");

		alignOnto = OntoUtil.getAlignedOntology(manager, 
				cleanMappingAxioms, fstO, sndO);

		List<OWLReasoner> reasoners = new ArrayList<>();

		alignReasoner = OntoUtil.getReasoner(alignOnto, getAlignReasoner(), 
				manager);

		reasoners.add(alignReasoner);

		OntoUtil.ontologyClassification(true, false, reasoners, Params.tryPellet);
		if(!OntoUtil.checkClassification(reasoners) 
				|| !alignReasoner.isConsistent()){
			FileUtil.writeErrorLogAndConsole("Timeout or inconsistent " +
					"aligned ontology, skipping the test");
			OntoUtil.disposeAllReasoners();
			return false;
		}

		alignReasoner = reasoners.get(0);

		if(!alignReasoner.getRootOntology().equals(alignOnto))
			throw new Error("Align ontology for reasoner mismatch");

		reasoners.clear();

		if(checkUnsat){
			checkIncoherentClasses();
		
			try {
				OntoUtil.saveClassificationAxioms(alignOnto, alignReasoner, manager);
			}
			catch(org.semanticweb.owlapi.reasoner.InconsistentOntologyException | 
					NullPointerException e){
				FileUtil.writeErrorLogAndConsole("Impossible to save the " +
							"inferred axioms into the ontologies:\n" + e.getMessage() 
							+ ",\n skipping this test");
				OntoUtil.disposeAllReasoners();
				return false;
			}
			// we use an extended version of ELK also providing direct disjoint classes
			//if(OntoUtil.isELKReasoner(alignReasoner))
	//		alignReasoner = new ExtDisjReasoner(alignReasoner);
	
			alignIdx = new JointIndexManager(); 
			alignProc = LogMapWrapper.indexSetup(alignOnto, alignIdx, alignReasoner);
		
			FileUtil.writeLogAndConsole("STEP : " + Util.getDiffmsec(start) 
					+ " (ms) - " + Util.getCurrTime() + "\n");
		}
		
		return true;
	}
	
	private static REASONER_KIND getAlignReasoner(){		
		return useELK ? REASONER_KIND.ELK : Params.reasonerKind;
	}
	
	private static void determineELKUsage(OWLReasoner ... reasoners){
		for (OWLReasoner r : reasoners) {
			if(OntoUtil.isELKReasoner(r)){
				useELK = true;
				return;
			}
		}
	}
}