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

import enumerations.REASONER_KIND;
import enumerations.VIOL_KIND;
import scc.exception.ClassificationTimeoutException;
import scc.graphDataStructure.Diagnosis;
import scc.graphDataStructure.LightAdjacencyList;
import scc.graphDataStructure.LightEdge;
import scc.graphDataStructure.LightSCC;
import scc.graphDataStructure.LightSCCs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import logmap.LogMapWrapper;

import org.mindswap.pellet.exceptions.InconsistentOntologyException;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import reasoning.ExtDisjReasoner;

import uk.ac.ox.krr.logmap2.Parameters;
import uk.ac.ox.krr.logmap2.indexing.JointIndexManager;
import uk.ac.ox.krr.logmap2.indexing.OntologyProcessing;
import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;
import uk.ac.ox.krr.logmap2.overlapping.OverlappingExtractor4Mappings;
import util.FileUtil;
import util.OntoUtil;
import util.Params;
import util.Util;
import auxStructures.Pair;

public class CombinedRepair {
	boolean useELK;

	int step, repairStep;

	JointIndexManager alignIdx, index, origIndex;
	OntologyProcessing alignProc, origProc, fstProc, sndProc, 
	fstProcOrig, sndProcOrig;
	OWLOntology fstO, sndO, alignOnto;
	OWLReasoner alignReasoner;

	List<Pair<Integer>> numUnsolvViol1 = new ArrayList<>();
	List<Pair<Integer>> numUnsolvViol2 = new ArrayList<>();
	List<Pair<Integer>> numUnsolvViolEQ = new ArrayList<>();
	
	List<Pair<List<Pair<Integer>>>> unsolvViol1 = new ArrayList<>();
	List<Pair<List<Pair<Integer>>>> unsolvViol2 = new ArrayList<>();
	List<Pair<List<Pair<Integer>>>> unsolvViolEQ = new ArrayList<>();
	List<Integer> incoherences = new ArrayList<>();
	
	Set<MappingObjectStr> originalMappings;
	Set<MappingObjectStr> consistentMappings;
	Set<MappingObjectStr> multistepRepair;

	Set<OWLAxiom> consistentAlignment;
	Set<OWLAxiom> originalAlignment;

	String mappingName;
	String mappingFile;

	private boolean suppressViolOutput = true;

	OWLOntologyManager manager;
	String testMappingDir;
	String testOntoDir;
	boolean preSCC;
	
	long moduleTime = 0;
	int numSCC = 0, diagApxNum = 0;
	
	public CombinedRepair(OWLOntologyManager manager, String testMappingDir, 
			String testOntoDir, boolean preSCC){
		this.manager = manager;
		this.testMappingDir = testMappingDir;
		this.testOntoDir = testOntoDir;
		this.preSCC = preSCC;
	}

	private void init(){
		step = 0;
		repairStep = 0;
		useELK = false;
		
		unsolvViol1.clear();
		unsolvViol2.clear();
		unsolvViolEQ.clear();
		incoherences .clear();
	}

	public void repair(String mappingFile, OWLOntology fstO,
			OWLOntology sndO, boolean unloadOnto, boolean rootViolations,
			boolean fullDisj, boolean useModules, String trackName){

//		double totalStartTime = System.currentTimeMillis();
		
		long sccTime = 0;
		int sccUnsolved1Fst = 0, sccUnsolved1Snd = 0, 
				sccUnsolved2Fst = 0, sccUnsolved2Snd = 0, 
				sccUnsolved3Fst = 0, sccUnsolved3Snd = 0, 
				sccDiagSize = 0;
		init();

		FileUtil.enableDataOutFileBuffering();

		this.fstO = fstO;
		this.sndO = sndO;
		this.mappingFile = mappingFile;

		FileUtil.writeLogAndConsole("Ontology 0: " + fstO.toString());
		FileUtil.writeLogAndConsole("Ontology 1: " + sndO.toString());

		int signSizeFst = fstO.getClassesInSignature(true).size() + 
				fstO.getObjectPropertiesInSignature(true).size() + 
				fstO.getDataPropertiesInSignature(true).size();
		int signSizeSnd = sndO.getClassesInSignature(true).size() + 
				sndO.getObjectPropertiesInSignature(true).size() + 
				sndO.getDataPropertiesInSignature(true).size();

		/****** COLUMN 0 = alignment (matcher + ontologies) *********/
		mappingName = mappingFile.substring(mappingFile.lastIndexOf("/")+1, 
				mappingFile.length()-4);

		if(mappingFile.contains("UMLS")){
			mappingName = mappingFile.split("_")[1];
			mappingName = mappingName.replace('2', '-');
		}

		if(mappingFile.contains("/home/ale/data/oaei2012/library/alignments/AROMA.rdf")){
			FileUtil.writeDataOutFile("Skipping AROMA 2012 (requires > 26GB)");
			return;
		}
		if(mappingFile.contains("/home/ale/data/oaei2013/library/alignments/xmapGen.rdf")){
			FileUtil.writeDataOutFile("Skipping XMAPGEN 2013 (requires > 26GB)");
			return;
		}
		
		FileUtil.writeDataOutFile(mappingName + " ");

		originalMappings = LogMapWrapper.getMappings(mappingFile, fstO, sndO);

		// if required we extract the modules (the ontologies are updated)
		if (useModules)
			if(!extractModules())
				return;

		/* START - LOGMAP VIOLATIONS  */
		// STEP 1: load and classify input ontologies 
		FileUtil.writeLogAndConsole(
				"\nSTEP " + (step) +": " + "input ontologies classification - " + 
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

		long saveClassTime = Util.getDiffmsec(start);
		
		/****** COLUMN 1 = inputClassificationTime *********/
		FileUtil.writeDataOutFile((classTime + saveClassTime) + " ");

		FileUtil.writeLogAndConsole("STEP " + (step++) +": " + 
				(classTime + saveClassTime) 
				+ " (ms) - " + Util.getCurrTime() + "\n");		


		// STEP 2: load mappings and profile original consistency repair
		FileUtil.writeLogAndConsole("STEP " + (step) +": " +
				"load mappings and profile original consistency repair - " + 
				Util.getCurrTime());
		start = Util.getMSec();


		long repairTimeConservativity = Util.getMSec();

		boolean oldVal = Parameters.repair_heuristic;
		Parameters.repair_heuristic = false;
		consistentMappings = LogMapWrapper.repairInconsistentAlignments(
				null, mappingFile, fstO, sndO, originalMappings, 
				Params.fullReasoningRepair);
		Parameters.repair_heuristic = oldVal;
		
		repairTimeConservativity = Util.getDiffmsec(repairTimeConservativity);

		originalAlignment = Collections.unmodifiableSet(
				OntoUtil.convertAlignmentToAxioms(fstO, sndO, 
						originalMappings));

		consistentAlignment = OntoUtil.convertAlignmentToAxioms(
				fstO, sndO, consistentMappings);

		FileUtil.writeLogAndConsole("STEP " + (step++) + ": " 
				+ Util.getDiffmsec(start) + " (ms) - " 
				+ Util.getCurrTime() + "\n");


		// STEP 3: create and classify the aligned ontology
		start = Util.getMSec();
		FileUtil.writeLogAndConsole("STEP " + (step) +": " +
				"create and classify the aligned ontology - " + 
				Util.getCurrTime());

		long alignClassifTime, incohCheckTime, totalAlignTime = start;
		try {
			FileUtil.writeLogAndConsole(consistentAlignment.size() + " mapping(s)");
			
			alignOnto = OntoUtil.getAlignedOntology(manager, 
					consistentAlignment, fstO, sndO);
			alignReasoner = OntoUtil.getReasoner(alignOnto, 
					getAlignReasoner(), manager);

			reasoners.clear();
			reasoners.add(alignReasoner);

			alignClassifTime = OntoUtil.ontologyClassification(true, false, 
					reasoners, Params.tryPellet);
			
			alignReasoner = reasoners.get(0);

			if(!alignReasoner.getRootOntology().equals(alignOnto))
				throw new Error("Aligned ontology for reasoner mismatch");

			if(!OntoUtil.checkClassification(reasoners)){
				FileUtil.writeErrorLogAndConsole("Classification failed, " +
						"only lower bound results");
//				OntoUtil.disposeAllReasoners();
//				return;
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

			if(rootViolations){
				start = Util.getMSec();
				OntoUtil.saveClassificationAxioms(alignOnto, alignReasoner, manager);
				alignClassifTime += Util.getDiffmsec(start);
			}
			
			totalAlignTime = Util.getDiffmsec(totalAlignTime);
			
			if(Params.saveMappings){
				FileUtil.writeLogAndConsole("Serializing the repaired (unsat) mappings");
				LogMapWrapper.saveMappings(testMappingDir + Params.trackName + 
						"-" + mappingName + "-unsatFree", fstO, sndO, 
						consistentMappings);
			}
			
			if(Params.saveOnto){
				FileUtil.writeLogAndConsole("Saving aligned ontology repaired (unsat)");
				String ontoName = mappingFile.substring(
						mappingFile.lastIndexOf('/')+1, 
						mappingFile.lastIndexOf('.'));
				try {
					OntoUtil.save(alignOnto, testOntoDir + ontoName + "-nounsat.owl", manager);
				} catch (OWLOntologyStorageException | OWLOntologyCreationException
						| IOException e) {
					e.printStackTrace();
				}
			}
		}
		catch(org.semanticweb.owlapi.reasoner.InconsistentOntologyException 
				| InconsistentOntologyException | IllegalArgumentException e){
			FileUtil.writeErrorLogAndConsole("Illegal aligned ontology: " 
					+ e.getMessage());
			OntoUtil.disposeAllReasoners();
			return;
		}
		
		if(Params.saveOnto){
			FileUtil.writeLogAndConsole("Saving starting aligned ontology");
			String ontoName = mappingFile.substring(
					mappingFile.lastIndexOf('/')+1, 
					mappingFile.lastIndexOf('.'));
			try {
				OntoUtil.save(alignOnto, 
						testOntoDir + ontoName + "-original.owl", manager);
			} catch (OWLOntologyStorageException | OWLOntologyCreationException
					| IOException e) {
				e.printStackTrace();
			}
		}

		FileUtil.writeLogAndConsole("STEP " + (step++) + ": align " + alignClassifTime 
				+ " (ms), repairFull " + incohCheckTime + " (ms), total " + 
				totalAlignTime + " (ms) - " + Util.getCurrTime() + "\n");

		
		/****** COLUMN 2 = |M|, COLUMN 3 = repair_consistency time, COLUMN 4 = |R|_consistency *********/
		FileUtil.writeDataOutFile(LogMapWrapper.countMappings(originalMappings) 
				+ " " + (repairTimeConservativity+incohCheckTime) + " " + 
				(LogMapWrapper.countMappings(originalMappings) - 
						LogMapWrapper.countMappings(consistentMappings)) + " ");


		// STEP 4: compute the semantic indexes for input/aligned ontologies
		start = Util.getMSec();
		FileUtil.writeLogAndConsole("STEP " + (step) + 
				": compute the semantic indexes for input/aligned ontologies - " 
				+ Util.getCurrTime());

		alignIdx = new JointIndexManager();
		index = new JointIndexManager();

		OntologyProcessing [] oprocs = LogMapWrapper.indexSetup(
				fstO,sndO,index,fstReasoner,sndReasoner);
		fstProc = oprocs[0];
		sndProc = oprocs[1];

		origIndex = new JointIndexManager(index);

		fstProcOrig = new OntologyProcessing(fstProc);
		sndProcOrig = new OntologyProcessing(sndProc);

		alignProc = LogMapWrapper.indexSetup(alignOnto,alignIdx,alignReasoner);

		FileUtil.writeLogAndConsole("STEP " + (step++) + ": " + Util.getDiffmsec(start) 
				+ " (ms) - " + Util.getCurrTime() + "\n");


		// STEP 5: detect conservativity principle violations and disjointness axioms
		FileUtil.writeLogAndConsole("STEP " + (step) +": " +
				"detect conservativity principle violations and disjointness axioms - " + 
				Util.getCurrTime());
		start = Util.getMSec();
		long disjComputTime = detectViolations(true, rootViolations);

		/****** COLUMN 5 = viol pre 1, COLUMN 6 = viol pre 2 *********/
		FileUtil.writeDataOutFile(getViolationNumber(true, VIOL_KIND.APPROX, 1) 
				+ " " + getViolationNumber(false, VIOL_KIND.APPROX, 1) + " ");

		/****** COLUMN 7 = viol pre 1 equiv, COLUMN 8 = viol pre 2  equiv *********/
		FileUtil.writeDataOutFile(getViolationNumber(true, VIOL_KIND.FULL, 1) 
				+ " " + getViolationNumber(false, VIOL_KIND.FULL, 1) + " ");

		/****** COLUMN 9 = viol pre 1 EQ only, COLUMN 10 = viol pre 2 EQ only *********/
		FileUtil.writeDataOutFile(getViolationNumber(true, VIOL_KIND.EQONLY, 1) 
				+ " " + getViolationNumber(false, VIOL_KIND.EQONLY, 1) + " ");

		FileUtil.writeLogAndConsole("STEP " + (step++) + ": " 
				+ Util.getDiffmsec(start) + " (ms) - " + Util.getCurrTime() + "\n");

		if(preSCC){
			sccTime = Util.getMSec();
			Diagnosis d = repairSCC(fstReasoner,sndReasoner);
			sccTime = Util.getDiffmsec(sccTime);

			FileUtil.writeLogAndConsole("SCC repair total time: " + sccTime + " (ms)");
					
			updateMappings(d);
			if(!d.isEmpty() && !updateAlignedOntology(true))
				return;
			detectViolations(false, rootViolations);

			sccUnsolved1Fst = getViolationNumber(true, VIOL_KIND.APPROX, repairStep); 
			sccUnsolved1Snd = getViolationNumber(false, VIOL_KIND.APPROX, repairStep); 
			sccUnsolved2Fst = getViolationNumber(true, VIOL_KIND.FULL, repairStep); 
			sccUnsolved2Snd = getViolationNumber(false, VIOL_KIND.FULL, repairStep);
			sccUnsolved3Fst = getViolationNumber(true, VIOL_KIND.EQONLY, repairStep);
			sccUnsolved3Snd = getViolationNumber(false, VIOL_KIND.EQONLY, repairStep);

			sccDiagSize = d.size();
			//sccDiagSize = LogMapWrapper.countMappings(originalMappings) - d.size();
			
			if(Params.saveMappings){
				FileUtil.writeLogAndConsole("Serializing the repaired mappings");
				LogMapWrapper.saveMappings(testMappingDir + Params.trackName + "-" +
						mappingName + "-sccpre", fstO, sndO, consistentMappings);
			}
			
			if(Params.saveOnto){
				FileUtil.writeLogAndConsole("Saving aligned ontology repaired by SCC");
				String ontoName = mappingFile.substring(
						mappingFile.lastIndexOf('/')+1, 
						mappingFile.lastIndexOf('.'));
				try {
					OntoUtil.save(alignOnto, testOntoDir + ontoName + "-sccpre.owl", manager);
				} catch (OWLOntologyStorageException | OWLOntologyCreationException
						| IOException e) {
					e.printStackTrace();
				}
			}
		}

		// STEP 6: adding disjointness axioms to input ontologies
		FileUtil.writeLogAndConsole("STEP " + (step) +": " +
				"adding disjointness axioms to input ontologies - " + 
				Util.getCurrTime());
		start = Util.getMSec();

		int disjNum1 = 0, disjNum2 = 0;
		int repairStepForDisj = (preSCC ? repairStep : 1);

//		int disjCounter = 0, intRepairs = 0;
		long repairTimePre = 0;
		
		for (Pair<Integer> p : getViolations(true, VIOL_KIND.APPROX, repairStepForDisj)){
			index.addDisjointness(p.getFirst(), p.getSecond(), false);
			
//			++disjCounter;
//			
//			if(disjCounter > Params.violThreshold){
//				long init = Util.getMSec();
//				
//				index.recreateDisjointIntervalsStructure();
//
//				multistepRepair = LogMapWrapper.repairInconsistentAlignments(
//						fstO, sndO, mappingFile, fstProc, sndProc, index, 
//						multistepRepair == null ? consistentMappings : multistepRepair);
//				
//				repairTimePre += Util.getDiffmsec(init);
//				++intRepairs;
//			}
		}
		
		for (Pair<Integer> p : getViolations(false, VIOL_KIND.APPROX, repairStepForDisj)){
			index.addDisjointness(p.getFirst(), p.getSecond(), false);

//			++disjCounter;
//			
//			if(disjCounter > Params.violThreshold){
//				long init = Util.getMSec();
//				
//				index.recreateDisjointIntervalsStructure();
//				
//				multistepRepair = LogMapWrapper.repairInconsistentAlignments(
//						fstO, sndO, mappingFile, fstProc, sndProc, index, 
//						multistepRepair == null ? consistentMappings : multistepRepair);
//
//				repairTimePre += Util.getDiffmsec(init);			
//				++intRepairs;
//			}
		}

		disjNum1 = getViolations(true, VIOL_KIND.APPROX, repairStepForDisj).size();
		disjNum2 = getViolations(false, VIOL_KIND.APPROX, repairStepForDisj).size();

		if(!Params.storeViolations){
			unsolvViol1.get(repairStepForDisj-1).getFirst().clear();
			unsolvViol1.get(repairStepForDisj-1).getSecond().clear();
		}
		
		index.recreateDisjointIntervalsStructure();

		disjComputTime += Util.getDiffmsec(start);

		FileUtil.writeLogAndConsole(disjNum1 + disjNum2 
				+ " disjoint axioms added (" + disjComputTime + " ms)");

//		if(intRepairs > 0){
//			disjComputTime -= repairTimePre;
//
//			FileUtil.writeLogAndConsole(intRepairs + " intermediate repairs " +
//				"were needed: " + repairTimePre + "ms");
//		}
		
		/****** COLUMN 11 = disj. axioms onto 1, COLUMN 12 =  disj. axioms onto 2, COLUMN 13 = DISJTIME *********/
		FileUtil.writeDataOutFile(disjNum1 + " " + disjNum2 + " " + disjComputTime + " ");

		FileUtil.writeLogAndConsole("STEP " + (step++) +": " + Util.getDiffmsec(start) 
				+ " (ms) - " + Util.getCurrTime() + "\n");


		// STEP 7: repair conservativity
		FileUtil.writeLogAndConsole("STEP " + (step) + ": " + 
				"repair conservativity - " + Util.getCurrTime());
		start = Util.getMSec();

		int mappingsNumPre = LogMapWrapper.countMappings(consistentMappings);
		
		// repair conservativity only (takes as input a coherent alignment)
		long repairTime = Util.getMSec();
		multistepRepair = LogMapWrapper.repairInconsistentAlignments(fstO, sndO, 
				mappingFile, fstProc, sndProc, index, consistentMappings);

		int mappingsNumPost = LogMapWrapper.countMappings(multistepRepair); 

		repairTime = Util.getDiffmsec(repairTime) + repairTimePre;

		/****** COLUMN 14 = repair_both time, COLUMN 15 = |R|_both *********/
		FileUtil.writeDataOutFile(-1 + " " + -1 + " ");

		/****** COLUMN 16 = repair_conservativity time, COLUMN 17 = |R|_conservativity *********/
		FileUtil.writeDataOutFile(repairTime + " " + (mappingsNumPre - mappingsNumPost) + " ");

		FileUtil.writeLogAndConsole("STEP " + (step++) +": " + Util.getDiffmsec(start) 
				+ " (ms) - " + Util.getCurrTime() + "\n");

		OntoUtil.chooseReasoner(mappingFile, trackName);

		consistentMappings = multistepRepair;
		Set<OWLAxiom> diagnosis = new HashSet<>(originalAlignment);
		diagnosis.removeAll(OntoUtil.convertAlignmentToAxioms(fstO, sndO, 
				consistentMappings));

		if(((mappingsNumPre - mappingsNumPost) > 0 ) 
				&& !updateAlignedOntology(true))
			return;

		if(Params.saveMappings){
			FileUtil.writeLogAndConsole("Serializing the repaired mappings");
			LogMapWrapper.saveMappings(testMappingDir + Params.trackName + "-" +
					mappingName + "-logmap" + (preSCC ? "post" : "pre"), 
					fstO, sndO, consistentMappings);
		}
		
		// STEP 9: detect unsolved violations using semantic indexes
		detectViolations(false, rootViolations);

		/****** COLUMN 18 = viol post 1, COLUMN 19 = viol post 2 *********/
		FileUtil.writeDataOutFile(
				getViolationNumber(true, VIOL_KIND.APPROX, repairStep) + " " + 
						getViolationNumber(false, VIOL_KIND.APPROX, repairStep) + " ");

		/****** COLUMN 20 = viol post 1 full, COLUMN 21 = viol post 2 full *********/
		FileUtil.writeDataOutFile(
				getViolationNumber(true, VIOL_KIND.FULL, repairStep) + " " + 
						getViolationNumber(false, VIOL_KIND.FULL, repairStep) + " ");

		/****** COLUMN 22 = viol post 1 eq only, COLUMN 23 = viol post 2 eq only *********/
		FileUtil.writeDataOutFile(
				getViolationNumber(true, VIOL_KIND.EQONLY, repairStep) + " " + 
						getViolationNumber(false, VIOL_KIND.EQONLY, repairStep) + " ");

		/* END - LOGMAP VIOLATIONS  */

		if(Params.saveOnto){
			FileUtil.writeLogAndConsole("Saving aligned ontology repaired by LogMap");
			String ontoName = mappingFile.substring(
					mappingFile.lastIndexOf('/')+1, 
					mappingFile.lastIndexOf('.'));
			try {
				OntoUtil.save(alignOnto, testOntoDir + ontoName + "-logmap" 
						 + (preSCC ? "post" : "pre") + ".owl", manager);
			} catch (OWLOntologyStorageException | OWLOntologyCreationException
					| IOException e) {
				e.printStackTrace();
			}
		}

		if(!preSCC){
			sccTime = Util.getMSec();
			Diagnosis d = repairSCC(fstReasoner,sndReasoner);
			sccTime = Util.getDiffmsec(sccTime);

			FileUtil.writeLogAndConsole("SCC repair total time: " + sccTime + " (ms)");
			
			updateMappings(d);

			if(!d.isEmpty() && !updateAlignedOntology(true))
				return;

			OntoUtil.disposeAllReasoners();

			detectViolations(false, rootViolations);

			sccUnsolved1Fst = getViolationNumber(true, VIOL_KIND.APPROX, repairStep); 
			sccUnsolved1Snd = getViolationNumber(false, VIOL_KIND.APPROX, repairStep); 
			sccUnsolved2Fst = getViolationNumber(true, VIOL_KIND.FULL, repairStep); 
			sccUnsolved2Snd = getViolationNumber(false, VIOL_KIND.FULL, repairStep);
			sccUnsolved3Fst = getViolationNumber(true, VIOL_KIND.EQONLY, repairStep);
			sccUnsolved3Snd = getViolationNumber(false, VIOL_KIND.EQONLY, repairStep);

			sccDiagSize = d.size();
					//LogMapWrapper.countMappings(multistepRepair) - d.size();

			if(Params.saveMappings){
				FileUtil.writeLogAndConsole("Serializing the repaired mappings");
				LogMapWrapper.saveMappings(testMappingDir + Params.trackName + 
						"-" + mappingName + "-sccpost", 
						fstO, sndO, consistentMappings);
			}
			
			if(Params.saveOnto){
				FileUtil.writeLogAndConsole("Saving aligned ontology repaired by SCC");
				String ontoName = mappingFile.substring(
						mappingFile.lastIndexOf('/')+1, 
						mappingFile.lastIndexOf('.'));
				try {
					OntoUtil.save(alignOnto, testOntoDir + ontoName + "-sccpost.owl", manager);
				} catch (OWLOntologyStorageException | OWLOntologyCreationException
						| IOException e) {
					e.printStackTrace();
				}
			}
		}

		OntoUtil.disposeAllReasoners();

		/****** COLUMN 24 = aligned ontology classification time *********/
		FileUtil.writeDataOutFile(alignClassifTime + " ");

		/****** COLUMN 25-26 = signature size first-second ontology  ********/
		FileUtil.writeDataOutFile(signSizeFst + " " + signSizeSnd + " ");

		/****** COLUMN 27-37 = SCC repair time, |Diagnosis|, unsolvedkind1Fst, 
			 		unsolvedkind1Snd, unsolvedkind2Fst, unsolvedkind2Snd, 
			 		unsolvedEQFst, unsolvedEQSnd, SCC first, usedELK, module1Time 
		 ********/
		FileUtil.writeDataOutFileNL(sccTime + " " + sccDiagSize + " " + 
				sccUnsolved1Fst + " " + sccUnsolved1Snd + " " + 
				sccUnsolved2Fst + " " + sccUnsolved2Snd + " " +
				sccUnsolved3Fst + " " + sccUnsolved3Snd + " " + 
				(preSCC ? "1":"0") + " " + (useELK ? "1":"0") + " " + moduleTime);
		
		FileUtil.flushWriteDataOutFile();
		OntoUtil.unloadAllOntologies(manager);
		OntoUtil.checkActiveReasoners(true);
	}

	private REASONER_KIND getAlignReasoner(){		
		return useELK ? REASONER_KIND.ELK : Params.reasonerKind;
	}

	private void determineELKUsage(OWLReasoner ... reasoners){
		for (OWLReasoner r : reasoners) {
			if(OntoUtil.isELKReasoner(r)){
				useELK = true;
				return;
			}
		}
	}
	
	private void storeUnsolvedViolations(VIOL_KIND kind, 
			Pair<List<Pair<Integer>>> viols){
		
		int num1 = viols.getFirst().size();
		int num2 = viols.getSecond().size();
		
		switch(kind){
		case APPROX:
			// store them anyway for repair, cleared later
			unsolvViol1.add(viols);
			numUnsolvViol1.add(new Pair<>(num1,num2));
			break;
		case FULL:
			if(Params.storeViolations)
				unsolvViol2.add(viols);
			numUnsolvViol2.add(new Pair<>(num1,num2));
			break;
		case EQONLY:
			if(Params.storeViolations)
				unsolvViolEQ.add(viols);
			numUnsolvViolEQ.add(new Pair<>(num1,num2));
			break;
		}
	}

	private long detectViolations(boolean pre, boolean rootViolations){

		// STEP: detect violations using semantic indexes
		FileUtil.writeLogAndConsole("STEP " + (step) +": " +
				"detect violations using semantic indexes - " + 
				Util.getCurrTime());
		long disjComputTime = Util.getMSec();
		long start = disjComputTime;

		FileUtil.writeLogAndConsole("Detection of violations kind 1");
//		unsolvViol1.add(LogMapWrapper.parallelConservativityViolationDetection(
//				origIndex, fstProcOrig, sndProcOrig, alignIdx, 
//				alignProc, rootViolations, false, suppressViolOutput, 
//				fstO,sndO,alignOnto));

		storeUnsolvedViolations(VIOL_KIND.APPROX, 
				LogMapWrapper.parallelConservativityViolationDetection(
				origIndex, fstProcOrig, sndProcOrig, alignIdx, 
				alignProc, rootViolations, false, suppressViolOutput, 
				fstO,sndO,alignOnto));

		
		disjComputTime = Util.getDiffmsec(disjComputTime);

		FileUtil.writeLogAndConsole("Detection of violations kind 2");
//		unsolvViol2.add(LogMapWrapper.parallelConservativityViolationDetection(
//				origIndex, fstProcOrig, sndProcOrig, alignIdx, 
//				alignProc, rootViolations, true, suppressViolOutput, 
//				fstO,sndO,alignOnto));
		
		storeUnsolvedViolations(VIOL_KIND.FULL, 
				LogMapWrapper.parallelConservativityViolationDetection(
				origIndex, fstProcOrig, sndProcOrig, alignIdx, 
				alignProc, rootViolations, true, suppressViolOutput, 
				fstO,sndO,alignOnto));

		FileUtil.writeLogAndConsole("Detection of violations kind 3");
//		unsolvViolEQ.add(
//				LogMapWrapper.parallelEqConservativityViolationDetection(
//						origIndex, fstProcOrig, sndProcOrig, alignIdx, 
//						alignProc, suppressViolOutput));
		
		storeUnsolvedViolations(VIOL_KIND.EQONLY, 
				LogMapWrapper.parallelEqConservativityViolationDetection(
						origIndex, fstProcOrig, sndProcOrig, alignIdx, 
						alignProc, suppressViolOutput));

		repairStep++;

		if(!pre){
			if(getTotalActualStepViolationNumber(VIOL_KIND.APPROX) > 0)
				FileUtil.writeErrorLogAndConsole(
						getTotalActualStepViolationNumber(VIOL_KIND.APPROX) + "/" 
								+ getTotalInitialViolationNumber(VIOL_KIND.APPROX) 
								+ " unsolved violation(s) approximated notion");

			if(getTotalActualStepViolationNumber(VIOL_KIND.FULL) > 0)
				FileUtil.writeErrorLogAndConsole(
						getTotalActualStepViolationNumber(VIOL_KIND.FULL) + "/" 
								+ getTotalInitialViolationNumber(VIOL_KIND.FULL) 
								+ " unsolved violation(s) full notion");

			if(getTotalActualStepViolationNumber(VIOL_KIND.EQONLY) > 0)
				FileUtil.writeErrorLogAndConsole(
						getTotalActualStepViolationNumber(VIOL_KIND.EQONLY) + "/" 
								+ getTotalInitialViolationNumber(VIOL_KIND.EQONLY)
								+ " unsolved violation(s) equivalences ONLY");

			if(getTotalViolationNumber(VIOL_KIND.APPROX, repairStep) > 
				getTotalViolationNumber(VIOL_KIND.APPROX, repairStep-1) || 
				getTotalViolationNumber(VIOL_KIND.FULL, repairStep) > 
				getTotalViolationNumber(VIOL_KIND.FULL, repairStep-1) ||
				getTotalViolationNumber(VIOL_KIND.EQONLY, repairStep) > 
				getTotalViolationNumber(VIOL_KIND.EQONLY, repairStep-1))
				FileUtil.writeErrorLogAndConsole(
						"Increased number of violations!");
			
			FileUtil.writeLogAndConsole("STEP " + (step++) +": " 
					+ Util.getDiffmsec(start) + " (ms) - " 
					+ Util.getCurrTime() + "\n");

			return Util.getDiffmsec(start);
		}

		return disjComputTime;
	}

	private List<Pair<Integer>> getViolations(boolean firstOnto, VIOL_KIND kind, 
			int repairStep){

		if(!kind.equals(VIOL_KIND.APPROX) && !Params.storeViolations)
			return null;
		
		switch (kind) {
		case APPROX:
			if(firstOnto)
				return unsolvViol1.get(repairStep-1).getFirst();

			return unsolvViol1.get(repairStep-1).getSecond();

		case FULL:
			if(firstOnto)
				return unsolvViol2.get(repairStep-1).getFirst();

			return unsolvViol2.get(repairStep-1).getSecond();

		case EQONLY:
			if(firstOnto)
				return unsolvViolEQ.get(repairStep-1).getFirst();

			return unsolvViolEQ.get(repairStep-1).getSecond();

		default:
			throw new Error("Unknown/unsupported violation kind " + kind);
		}
	}

	private int getViolationNumber(boolean firstOnto, VIOL_KIND kind, int repairStep){
//		return getViolations(firstOnto, kind, repairStep).size();
		
		switch (kind) {
		case APPROX:
			if(firstOnto)
				return numUnsolvViol1.get(repairStep-1).getFirst();

			return numUnsolvViol1.get(repairStep-1).getSecond();

		case FULL:
			if(firstOnto)
				return numUnsolvViol2.get(repairStep-1).getFirst();

			return numUnsolvViol2.get(repairStep-1).getSecond();

		case EQONLY:
			if(firstOnto)
				return numUnsolvViolEQ.get(repairStep-1).getFirst();

			return numUnsolvViolEQ.get(repairStep-1).getSecond();

		default:
			throw new Error("Unknown/unsupported violation kind " + kind);
		}
	}

	private int getTotalViolationNumber(VIOL_KIND kind, int repairStep){
		return getViolationNumber(true,kind,repairStep) + 
				getViolationNumber(false,kind,repairStep);
	}

	private int getTotalActualStepViolationNumber(VIOL_KIND kind){
		return getViolationNumber(true,kind,repairStep) + 
				getViolationNumber(false,kind,repairStep);		
	}

	private int getTotalInitialViolationNumber(VIOL_KIND kind){
		return getViolationNumber(true,kind,1) + getViolationNumber(false,kind,1);
	}

	private boolean updateAlignedOntology(boolean checkUnsat){
		// STEP 8: update the aligned ontology for testing unsolved violations
		if(checkUnsat)
			FileUtil.writeLogAndConsole("STEP " + (step) +": " +
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
		
			FileUtil.writeLogAndConsole("STEP " + (step++) +": " + Util.getDiffmsec(start) 
					+ " (ms) - " + Util.getCurrTime() + "\n");
		}
		
		return true;
	}

	private boolean checkIncoherentClasses(){

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
		
		return steps == 0;// || (steps > 0 && unsats == 0);
	}

	public Set<MappingObjectStr> updateMappings(Diagnosis d){

		int pre = LogMapWrapper.countMappings(consistentMappings);

		Set<MappingObjectStr> diag = d.toMappingObjectStr();		

		LogMapWrapper.applyRepair(consistentMappings,diag);
		
		int post = LogMapWrapper.countMappings(consistentMappings);

		if((pre-post) != d.size()){
			throw new RuntimeException("SCC removed " + d.size() 
					+ " mappings but only " + (pre-post) 
					+ " were actually removed");
		}
		else
			FileUtil.writeLogAndConsole("SCC removed " + (pre-post) + " mappings");

		return consistentMappings;
	}

	private Diagnosis repairSCC(OWLReasoner fstReasoner, OWLReasoner sndReasoner){

		Diagnosis hDiag = null;

		// STEP 10: SCC-based diagnosis computation
		FileUtil.writeLogAndConsole(
				"STEP " + (step) + ": SCC-based diagnosis computation - " 
						+ Util.getCurrTime());

		long start = Util.getMSec();

		LightAdjacencyList adj;
		try {
			adj = new LightAdjacencyList(fstO, sndO, null, false, fstReasoner, 
					sndReasoner);
		} catch (ClassificationTimeoutException e) {
			return hDiag;
		}

		adj.loadMappings(new File(mappingFile), consistentMappings);
		//		adj.loadMappings(
		//				new File(testMappingDir + mappingName + ".rdf"),null,false);

		Set<LightSCC> problematicSCCs = new HashSet<>();
		LightSCCs globalSCCs = new LightSCCs();
//		adj.computeGlobalSCCsAndProblematicMappings(problematicSCCs, null);
		hDiag = adj.computeDiagnosis(globalSCCs, adj.getOriginalMappings(), 
				problematicSCCs, null, Util.getMSec());

//		for (LightSCC scc : problematicSCCs) {
//			FileUtil.writeLogAndConsole(scc.problematicSCCAsString(adj));
//		}
		
		FileUtil.writeLogAndConsole("STEP " + (step++) + ": " 
				+ Util.getDiffmsec(start) + " (ms) - " 
				+ Util.getCurrTime() + "\n");

		return hDiag;
	}

	private boolean extractModules(){
		// STEP 0: modules extraction 
		FileUtil.writeLogAndConsole(
				"STEP " + (step) + ": modules extraction - " 
						+ Util.getCurrTime());
		long start = Util.getMSec();

		OverlappingExtractor4Mappings overlapping = 
				new OverlappingExtractor4Mappings();

		try {
			long startM = Util.getMSec();
			overlapping.createOverlapping(fstO, sndO, originalMappings);
			moduleTime = Util.getDiffmsec(startM);			
		} catch (Exception e) {
			FileUtil.writeErrorLogAndConsole("Error while extracting " +
					"modules, skipping the test: " + e.getMessage());
			return false;
		}
		

		fstO = overlapping.getOverlappingOnto1();
		sndO = overlapping.getOverlappingOnto2();
		
		FileUtil.writeLogAndConsole("Module Ontology 0: " + fstO.toString());
		FileUtil.writeLogAndConsole("Module Ontology 1: " + sndO.toString());
		FileUtil.writeLogAndConsole("Modules extracted in " + moduleTime + " ms");
		
		FileUtil.writeLogAndConsole("STEP " + (step++) + ": " 
				+ Util.getDiffmsec(start) + " (ms) - " 
				+ Util.getCurrTime() + "\n");

		//		OntoUtil.chooseReasoner(mappingFile, trackName);

		return true;
	}

}
