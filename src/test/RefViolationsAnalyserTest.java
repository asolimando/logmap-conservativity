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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import logmap.LogMapWrapper;

import org.mindswap.pellet.exceptions.InconsistentOntologyException;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

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
import enumerations.VIOL_KIND;

public class RefViolationsAnalyserTest extends AbstractGoldTester {

	boolean useELK;

	int step, repairStep;

	JointIndexManager alignIdx, index, origIndex;
	OntologyProcessing alignProc, origProc, fstProc, sndProc, 
	fstProcOrig, sndProcOrig;
	OWLOntology fstO, sndO, alignOnto;
	OWLReasoner alignReasoner;

	List<Pair<List<Pair<Integer>>>> unsolvViol1 = new ArrayList<>();
	List<Pair<List<Pair<Integer>>>> unsolvViol2 = new ArrayList<>();
	List<Pair<List<Pair<Integer>>>> unsolvViolEQ = new ArrayList<>();

	Set<MappingObjectStr> originalMappings;
	Set<MappingObjectStr> consistentMappings;
	Set<MappingObjectStr> multistepRepair;

	Set<OWLAxiom> consistentAlignment;
	Set<OWLAxiom> originalAlignment;

	String mappingName;
	String mappingFile;

	boolean saveOnto = true;
	boolean suppressViolOutput = true;

	public RefViolationsAnalyserTest(String [] args){
		super(args, 0);
	}

	private void init(){
		step = 0;
		repairStep = 0;
		useELK = false;
		
		unsolvViol1.clear();
		unsolvViol2.clear();
		unsolvViolEQ.clear();
	}

	@Override
	public void realTest(String mappingFile, OWLOntology fstO,
			OWLOntology sndO, boolean unloadOnto, boolean rootViolations,
			boolean fullDisj, boolean useModules, String trackName) {

		if(!trackName.equals("conference"))
			return;
		
		init();

		mappingName = mappingFile.substring(mappingFile.lastIndexOf("/")+1, 
				mappingFile.length()-4);

//		if(mappingFile.contains("UMLS")){
//			mappingName = mappingFile.split("_")[1];
//			mappingName = mappingName.replace('2', '-');
//		}

		try {
			FileUtil.createTestDataFile(testOutDir + mappingName + ".txt");
		} catch (IOException e1) {
			e1.printStackTrace();
		}

//		FileUtil.enableDataOutFileBuffering();

		this.fstO = fstO;
		this.sndO = sndO;
		this.mappingFile = mappingFile;

		FileUtil.writeLogAndConsole("Ontology 0: " + fstO.toString());
		FileUtil.writeLogAndConsole("Ontology 1: " + sndO.toString());

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

		// STEP 2: load mappings and profile original consistency repair
		FileUtil.writeLogAndConsole("STEP " + (step) +": " +
				"load mappings and profile original consistency repair - " + 
				Util.getCurrTime());
		start = Util.getMSec();

//		consistentMappings = LogMapWrapper.repairInconsistentAlignments(
//				null, mappingFile, fstO, sndO, originalMappings);
		consistentMappings = originalMappings;

		consistentAlignment = OntoUtil.convertAlignmentToAxioms(
				fstO, sndO, consistentMappings);

		originalAlignment = Collections.unmodifiableSet(
				OntoUtil.convertAlignmentToAxioms(fstO, sndO, 
						originalMappings));

		FileUtil.writeLogAndConsole("STEP " + (step++) + ": " 
				+ Util.getDiffmsec(start) + " (ms) - " 
				+ Util.getCurrTime() + "\n");


		// STEP 3: create and classify the aligned ontology
		start = Util.getMSec();
		FileUtil.writeLogAndConsole("STEP " + (step) +": " +
				"create and classify the aligned ontology - " + 
				Util.getCurrTime());

		long alignClassifTime;
		try {
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

			if(!OntoUtil.checkClassification(reasoners) || 
					!alignReasoner.isConsistent()){
				FileUtil.writeErrorLogAndConsole("Timeout or inconsistent " +
						"aligned ontology, skipping the test");
				OntoUtil.disposeAllReasoners();
				return;
			}

			determineELKUsage(fstReasoner,sndReasoner,alignReasoner);

			OntoUtil.saveClassificationAxioms(alignOnto, alignReasoner, manager);
			alignClassifTime = Util.getDiffmsec(start);
		}
		catch(org.semanticweb.owlapi.reasoner.InconsistentOntologyException 
				| InconsistentOntologyException | IllegalArgumentException e){
			FileUtil.writeErrorLogAndConsole("Illegal aligned ontology: " 
					+ e.getMessage());
			OntoUtil.disposeAllReasoners();
			return;
		}

		FileUtil.writeLogAndConsole("STEP " + (step++) + ": " + alignClassifTime 
				+ " (ms) - " + Util.getCurrTime() + "\n");

		if(saveOnto){
			FileUtil.writeLogAndConsole("Saving aligned ontology");
			String ontoName = mappingFile.substring(
					mappingFile.lastIndexOf('/')+1, 
					mappingFile.lastIndexOf('.'));
			try {
				OntoUtil.save(alignOnto, testOntoDir + ontoName + ".owl", manager);
			} catch (OWLOntologyStorageException | OWLOntologyCreationException
					| IOException e) {
				e.printStackTrace();
			}
		}

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

		// CHECK INCOHERENT CLASSES BEFORE REPAIR
		FileUtil.writeDataOutFileNL("Incoherent classes:");
		for (OWLClass c : alignReasoner.getUnsatisfiableClasses().getEntitiesMinusBottom())
			FileUtil.writeDataOutFileNL(c.toString());

		// STEP 5: detect conservativity principle violations
		FileUtil.writeLogAndConsole("STEP " + (step) +": " +
				"detect conservativity principle violations - " + 
				Util.getCurrTime());
		start = Util.getMSec();
		long disjComputTime = detectViolations(true);

		FileUtil.writeLogAndConsole("STEP " + (step++) + ": " 
				+ Util.getDiffmsec(start) + " (ms) - " + Util.getCurrTime() + "\n");
		
		
		// STEP 6: printing violations to file
		FileUtil.writeLogAndConsole("STEP " + (step) +": " +
				"printing violations to file - " + 
				Util.getCurrTime());
		start = Util.getMSec();

		FileUtil.writeDataOutFileNL("\nConservativity principle violations:");

		for (Pair<Integer> p : getViolations(true, VIOL_KIND.APPROX, 1))
			FileUtil.writeDataOutFileNL(
					LogMapWrapper.getIRIStrFromIndexPair(p, index).toString());

		for (Pair<Integer> p : getViolations(false, VIOL_KIND.APPROX, 1))
			FileUtil.writeDataOutFileNL(
					LogMapWrapper.getIRIStrFromIndexPair(p, index).toString());

		FileUtil.writeLogAndConsole("STEP " + (step++) +": " + Util.getDiffmsec(start) 
				+ " (ms) - " + Util.getCurrTime() + "\n");

		FileUtil.flushFiles();
		OntoUtil.disposeAllReasoners();
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

	private long detectViolations(boolean pre){

		// STEP: detect violations using semantic indexes
		FileUtil.writeLogAndConsole("STEP " + (step) +": " +
				"detect violations using semantic indexes - " + 
				Util.getCurrTime());
		long disjComputTime = Util.getMSec();
		long start = disjComputTime;

		FileUtil.writeLogAndConsole("Detection of violations kind 1");
		unsolvViol1.add(LogMapWrapper.parallelConservativityViolationDetection(
				origIndex, fstProcOrig, sndProcOrig, alignIdx, 
				alignProc, Params.rootViolations, false, suppressViolOutput, 
				fstO,sndO,alignOnto));

		disjComputTime = Util.getDiffmsec(disjComputTime);

		FileUtil.writeLogAndConsole("Detection of violations kind 2");
		unsolvViol2.add(LogMapWrapper.parallelConservativityViolationDetection(
				origIndex, fstProcOrig, sndProcOrig, alignIdx, 
				alignProc, Params.rootViolations, true, suppressViolOutput, 
				fstO,sndO,alignOnto));

		FileUtil.writeLogAndConsole("Detection of violations kind 2");
		unsolvViolEQ.add(
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

			FileUtil.writeLogAndConsole("STEP " + (step++) +": " 
					+ Util.getDiffmsec(start) + " (ms) - " 
					+ Util.getCurrTime() + "\n");

			return Util.getDiffmsec(start);
		}

		return disjComputTime;
	}

	private List<Pair<Integer>> getViolations(boolean firstOnto, VIOL_KIND kind, 
			int repairStep){

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
		return getViolations(firstOnto, kind, repairStep).size();
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

	private boolean extractModules(){
		// STEP 0: modules extraction 
		FileUtil.writeLogAndConsole(
				"STEP " + (step) + ": modules extraction - " 
						+ Util.getCurrTime());
		long start = Util.getMSec();

		OverlappingExtractor4Mappings overlapping = 
				new OverlappingExtractor4Mappings();

		try {
			overlapping.createOverlapping(fstO, sndO, originalMappings);
		} catch (Exception e) {
			FileUtil.writeErrorLogAndConsole("Error while extracting " +
					"modules, skipping the test: " + e.getMessage());
			return false;
		}
		fstO = overlapping.getOverlappingOnto1();
		sndO = overlapping.getOverlappingOnto2();

		FileUtil.writeLogAndConsole("Module Ontology 0: " + fstO.toString());
		FileUtil.writeLogAndConsole("Module Ontology 1: " + sndO.toString());

		FileUtil.writeLogAndConsole("STEP " + (step++) + ": " 
				+ Util.getDiffmsec(start) + " (ms) - " 
				+ Util.getCurrTime() + "\n");

		//		OntoUtil.chooseReasoner(mappingFile, trackName);

		return true;
	}

	/**
	 * @param args
	 * @throws IOException 
	 * @throws OWLOntologyCreationException 
	 */
	public static void main(String[] args) 
			throws OWLOntologyCreationException, IOException {
		new RefViolationsAnalyserTest(args).trackTest();
	}
}