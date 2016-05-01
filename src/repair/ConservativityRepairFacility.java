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
package repair;

import enumerations.REASONER_KIND;
import enumerations.REPAIR_METHOD;
import enumerations.VIOL_KIND;
import scc.exception.ClassificationTimeoutException;
import scc.graphDataStructure.Diagnosis;
import scc.graphDataStructure.LightAdjacencyList;
import scc.graphDataStructure.LightSCC;
import scc.graphDataStructure.LightSCCs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import logmap.LogMapWrapper;

import org.mindswap.pellet.exceptions.InconsistentOntologyException;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.UnloadableImportException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import com.google.common.collect.Sets;

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

public class ConservativityRepairFacility {

	private int repairStep;

	private REPAIR_METHOD repairMethod;
	
	private JointIndexManager alignIdx, origIndex;
	private OntologyProcessing alignProc, fstProcOrig, sndProcOrig;
	private OWLOntology fstO, sndO, alignOnto;
	private OWLReasoner fstReasoner, sndReasoner, alignReasoner;

	private List<Pair<Integer>> numUnsolvViol1 = new ArrayList<>();
	private List<Pair<Integer>> numUnsolvViol2 = new ArrayList<>();
	private List<Pair<Integer>> numUnsolvViolEQ = new ArrayList<>();
	
	private List<Pair<List<Pair<Integer>>>> unsolvViol1 = new ArrayList<>();
	private List<Pair<List<Pair<Integer>>>> unsolvViol2 = new ArrayList<>();
	private List<Pair<List<Pair<Integer>>>> unsolvViolEQ = new ArrayList<>();

	private List<Pair<Integer>> numDirUnsolvViol1 = new ArrayList<>();
	private List<Pair<Integer>> numDirUnsolvViol2 = new ArrayList<>();
	private List<Pair<Integer>> numDirUnsolvViolEQ = new ArrayList<>();
	
	private List<Pair<List<Pair<Integer>>>> dirUnsolvViol1 = new ArrayList<>();
	private List<Pair<List<Pair<Integer>>>> dirUnsolvViol2 = new ArrayList<>();
	private List<Pair<List<Pair<Integer>>>> dirUnsolvViolEQ = new ArrayList<>();
	
	private List<Integer> incoherences = new ArrayList<>();

	private Set<MappingObjectStr> originalMappings;
	private Set<MappingObjectStr> consistentMappings;

	private Set<OWLAxiom> originalAlignment;
	private Set<OWLAxiom> consistentAlignment;

	private String mappingName, mappingFile;
	
	private OWLOntologyManager manager;
	private boolean preSCC, useSCC, useELK, directViolations, useModules, useSUB;

	private boolean saveOnto = false, saveMappings = false;
	private boolean failed = false;
	
	private LightAdjacencyList adj;
	
	private String testOntoDir = "test/testEvo/onto/", testMappingDir = "test/testEvo/mappings/";
	
	public ConservativityRepairFacility(OWLOntology fstO, OWLOntology sndO, 
			OWLOntologyManager manager, String mappingFile){
		this(fstO, sndO, manager, mappingFile, Params.defaultRepairMethod);
	}
	
	public ConservativityRepairFacility(ConservativityRepairFacility r, 
			Set<MappingObjectStr> mappings, boolean repairPre){
		
		this.fstO = r.fstO;
		this.sndO = r.sndO;
		this.repairMethod = r.repairMethod;
		
//		this.fstProcOrig = r.fstProcOrig;
//		this.sndProcOrig = r.sndProcOrig;
		
		this.fstReasoner = r.fstReasoner;
		this.sndReasoner = r.sndReasoner;
		
		this.manager = r.manager;
		
		initRepairStrategy(repairMethod);
		
		initializeOriginalAlignment(mappings);

		if(repairPre){
			consistentMappings = initialConsistencyRepair();
		}
		else {
			consistentMappings = LogMapWrapper.cloneAlignment(originalMappings);
			
			originalAlignment = Collections.unmodifiableSet(
					OntoUtil.convertAlignmentToAxioms(fstO, sndO, 
							originalMappings));

			consistentAlignment = OntoUtil.convertAlignmentToAxioms(
					fstO, sndO, consistentMappings);
		}
		
		failed = classifyAlignedAndMaterialiseClosure();
		
		if(!failed){
			computeSemanticIndexes();
			detectViolations(true, directViolations);
		}
	}

	
	public ConservativityRepairFacility(OWLOntology fstO, OWLOntology sndO, 
			OWLOntologyManager manager, String mappingFile, 
			REPAIR_METHOD repairMethod){
		this.manager = manager;
		this.fstO = fstO;
		this.sndO = sndO;
		this.mappingFile = mappingFile;
		
		this.repairMethod = repairMethod;
		initRepairStrategy(repairMethod);
				
		init();
	}

	public ConservativityRepairFacility(OWLOntology fstO, OWLOntology sndO, 
			OWLOntologyManager manager, String mappingFile, 
			boolean directViolations){
		this(fstO, sndO, manager, mappingFile, directViolations, Params.defaultRepairMethod);
	}

	public ConservativityRepairFacility(OWLOntology fstO, OWLOntology sndO, 
			OWLOntologyManager manager, String mappingFile, 
			boolean directViolations, REPAIR_METHOD repairMethod){
		this.manager = manager;
		this.fstO = fstO;
		this.sndO = sndO;
		this.mappingFile = mappingFile;
		this.directViolations = directViolations;
		
		this.repairMethod = repairMethod;
		initRepairStrategy(repairMethod);
		
		init();
	}
	
	public ConservativityRepairFacility(String fstOPath, String sndOPath, 
			boolean local, String mappingFile){
		this(fstOPath, sndOPath, local, mappingFile, Params.defaultRepairMethod);
	}
	
	public ConservativityRepairFacility(String fstOPath, String sndOPath, 
			boolean local, String mappingFile, REPAIR_METHOD repairMethod){
		this.manager = OntoUtil.getManager(true);
		loadOntologies(fstOPath, sndOPath, local);
		this.mappingFile = mappingFile;
		
		this.repairMethod = repairMethod;
		initRepairStrategy(repairMethod);
		
		init();
	}

	public ConservativityRepairFacility(OWLOntology fstO, OWLOntology sndO, 
			OWLOntologyManager manager, Set<MappingObjectStr> mappings, 
			boolean preRepair, boolean useModules){
		this(fstO, sndO, manager, mappings, preRepair, useModules, Params.defaultRepairMethod, false);
	}

	public ConservativityRepairFacility(OWLOntology fstO, OWLOntology sndO, 
			OWLOntologyManager manager, Set<MappingObjectStr> mappings, 
			boolean preRepair){
		this(fstO, sndO, manager, mappings, preRepair, true, Params.defaultRepairMethod, false);
	}

	public ConservativityRepairFacility(OWLOntology fstO, OWLOntology sndO, 
			OWLOntologyManager manager, Set<MappingObjectStr> mappings){
		this(fstO, sndO, manager, mappings, true, true, Params.defaultRepairMethod, false);
	}
	
	public ConservativityRepairFacility(boolean preserveOutFile, OWLOntology fstO, OWLOntology sndO, 
			OWLOntologyManager manager, Set<MappingObjectStr> mappings){
		this(fstO, sndO, manager, mappings, true, true, Params.defaultRepairMethod, preserveOutFile);
	}

	public ConservativityRepairFacility(OWLOntology fstO, OWLOntology sndO, 
			OWLOntologyManager manager, Set<MappingObjectStr> mappings, 
			boolean preRepair, boolean useModules, REPAIR_METHOD repairMethod){
		this(fstO, sndO, manager, mappings, preRepair, true, repairMethod, false);
	}
	
	public ConservativityRepairFacility(OWLOntology fstO, OWLOntology sndO, 
			OWLOntologyManager manager, Set<MappingObjectStr> mappings, 
			boolean preRepair, boolean useModules, REPAIR_METHOD repairMethod, 
			boolean preserveOutFile){
		
		this.repairMethod = repairMethod;
		initRepairStrategy(repairMethod);
		
		this.manager = manager;
		this.fstO = fstO;
		this.sndO = sndO;

		this.useModules = useModules;
		
		initializeOriginalAlignment(mappings);
		if(!preserveOutFile)
			FileUtil.disableDataOutput();
		
		if(useModules)
			if(!extractModules())
				return;

		classifyInputAndMaterialiseClosure();
		
		if(preRepair)
			consistentMappings = initialConsistencyRepair();
		else
			consistentMappings = LogMapWrapper.cloneAlignment(originalMappings);
		
		originalAlignment = Collections.unmodifiableSet(
				OntoUtil.convertAlignmentToAxioms(fstO, sndO, 
						originalMappings));

		consistentAlignment = OntoUtil.convertAlignmentToAxioms(
				fstO, sndO, consistentMappings);
		
		failed = classifyAlignedAndMaterialiseClosure();
		
		if(!failed){
			computeSemanticIndexes();
			
			detectViolations(true, directViolations);		
		}
	}

	public ConservativityRepairFacility(String fstOPath, String sndOPath, 
			boolean local, Set<MappingObjectStr> mappings){
		this(fstOPath, sndOPath, local, mappings, Params.defaultRepairMethod);
	}

	public ConservativityRepairFacility(String fstOPath, String sndOPath, 
			boolean local, Set<MappingObjectStr> mappings, 
			REPAIR_METHOD repairMethod){
		
		Params.oaei = true;
		useModules = true;
		
		saveOnto = false;
		saveMappings = false;
		
		this.repairMethod = repairMethod;
		initRepairStrategy(repairMethod);
		
		this.manager = OntoUtil.getManager(true);
		loadOntologies(fstOPath, sndOPath, local);
		initializeOriginalAlignment(mappings);
		FileUtil.disableDataOutput();
		
		if(useModules)
			if(!extractModules())
				return;

		classifyInputAndMaterialiseClosure();
		
		//consistentMappings = new HashSet<>(originalMappings);

		consistentMappings = initialConsistencyRepair();
		
		originalAlignment = Collections.unmodifiableSet(
				OntoUtil.convertAlignmentToAxioms(fstO, sndO, 
						originalMappings));

		consistentAlignment = OntoUtil.convertAlignmentToAxioms(
				fstO, sndO, consistentMappings);
		
		failed = classifyAlignedAndMaterialiseClosure();
		
		if(!failed){
			computeSemanticIndexes();
			
			detectViolations(true, directViolations);
		}
	}
	
	private void clearRepairHistory(){
		repairStep = 0;
		
		numUnsolvViol1.clear();
		numUnsolvViol2.clear();
		numUnsolvViolEQ.clear();
		
		for (Pair<List<Pair<Integer>>> p : unsolvViol1) {
			p.getFirst().clear();
			p.getSecond().clear();
		}
		unsolvViol1.clear();

		for (Pair<List<Pair<Integer>>> p : unsolvViol2) {
			p.getFirst().clear();
			p.getSecond().clear();
		}
		unsolvViol2.clear();
		
		for (Pair<List<Pair<Integer>>> p : unsolvViolEQ) {
			p.getFirst().clear();
			p.getSecond().clear();
		}
		unsolvViolEQ.clear();

		numDirUnsolvViol1.clear();
		numDirUnsolvViol2.clear();
		numDirUnsolvViolEQ.clear();
		
		for (Pair<List<Pair<Integer>>> p : dirUnsolvViol1) {
			p.getFirst().clear();
			p.getSecond().clear();
		}
		dirUnsolvViol1.clear();

		for (Pair<List<Pair<Integer>>> p : dirUnsolvViol2) {
			p.getFirst().clear();
			p.getSecond().clear();
		}
		dirUnsolvViol2.clear();
		
		for (Pair<List<Pair<Integer>>> p : dirUnsolvViolEQ) {
			p.getFirst().clear();
			p.getSecond().clear();
		}
		dirUnsolvViolEQ.clear();
	
		incoherences.clear();
	}
	
	public void updateInitialAlignment(Set<MappingObjectStr> mappings, 
			boolean preRepair, boolean detect){		
		
//		Set<OWLEntity> sign1 = LogMapWrapper.getSignature(true, mappings);
//		Set<OWLEntity> sign2 = LogMapWrapper.getSignature(false, mappings);

		clearRepairHistory();
		
//		if(useModules && !(
//				fstO.getSignature().containsAll(sign1) && 
//				sndO.getSignature().containsAll(sign2))){
//			OntoUtil.disposeAllReasoners();
//			OntoUtil.unloadAllOntologies();
//			
//			if(!extractModules())
//				return;
//			
//			classifyInputAndMaterialiseClosure();
//			
//			LogMapWrapper.sanitizeMappingType(fstO, sndO, mappings);
//			originalMappings = mappings;
//			FileUtil.disableDataOutput();
//			
//			if(useModules)
//				if(!extractModules())
//					return;
//
//			classifyInputAndMaterialiseClosure();
//			
//			if(preRepair)
//				consistentMappings = initialConsistencyRepair();
//			else
//				consistentMappings = new HashSet<>(originalMappings);
//
//			originalAlignment = Collections.unmodifiableSet(
//					OntoUtil.convertAlignmentToAxioms(fstO, sndO, 
//							originalMappings));
//
//			consistentAlignment = OntoUtil.convertAlignmentToAxioms(
//					fstO, sndO, consistentMappings);
//			
//			classifyAlignedAndMaterialiseClosure();
//			
//			computeSemanticIndexes();
//			
//			if(detect)
//				detectViolations(true, directViolations);			
//		}

		initializeOriginalAlignment(mappings);

//		if(preRepair)
//			consistentMappings = initialConsistencyRepair();
//		else
		consistentMappings = LogMapWrapper.cloneAlignment(originalMappings);
		
		originalAlignment = Collections.unmodifiableSet(
				OntoUtil.convertAlignmentToAxioms(fstO, sndO, 
						originalMappings));

		consistentAlignment = OntoUtil.convertAlignmentToAxioms(
				fstO, sndO, consistentMappings);

//		OntoUtil.checkActiveReasoners(false);

		updateAlignedOntology(false);

		detectViolations(true, directViolations);
	}
	
	private void initRepairStrategy(REPAIR_METHOD repairMethod){
		switch (repairMethod) {
		case EQ:
			useSUB = false;
			useSCC = true;
			preSCC = true;
			break;
		case SUB:
			useSUB = true;
			useSCC = false;
			break;
		case SUBEQ:
			useSUB = true;
			useSCC = true;
			preSCC = false;			
			break;
		case EQSUB:
			useSUB = true;
			useSCC = true;
			preSCC = true;
			break;
		}
	}
	
	private void initializeOriginalAlignment(Set<MappingObjectStr> mappings){		
		originalMappings = LogMapWrapper.cloneAlignment(mappings);
		LogMapWrapper.sanitizeMappingType(fstO, sndO, originalMappings);
	}
	
	private REPAIR_METHOD getRepairMethod(){
		return repairMethod;
	}
	
	public void enableSaveMappings(){
		saveMappings = true;		
		FileUtil.createDirPath(testMappingDir);
	}

	public void enableSaveOntologies(){
		saveOnto = true;		
		FileUtil.createDirPath(testOntoDir);
	}
	
	public void enableSaveOntoMappings(){
		saveOnto = true;
		saveMappings = true;
		
		FileUtil.createDirPath(testOntoDir);
		FileUtil.createDirPath(testMappingDir);
	}
	
	public void setMappingFile(String mappingFile){
		int idxSlash = mappingFile.lastIndexOf('/');
		
		this.mappingFile = mappingFile;
		this.mappingName = idxSlash > 0 ? 
				mappingFile.substring(idxSlash+1) : mappingFile;
		
		int idxDot = mappingName.lastIndexOf('.');
		
		this.mappingName = idxDot > 0 ? 
				mappingName.substring(0, idxDot) : mappingFile;
	}
	
	private void init(){
		
		Parameters.print_output = false;
		Parameters.print_output_always = false;

		if(saveOnto)
			FileUtil.createDirPath(testOntoDir);
		if(saveMappings)
			FileUtil.createDirPath(testMappingDir);
		
		mappingName = mappingFile.substring(mappingFile.lastIndexOf("/")+1, 
				mappingFile.length()-4);
	
		if(mappingFile.contains("UMLS")){
			mappingName = mappingFile.split("_")[1];
			mappingName = mappingName.replace('2', '-');
		}
			
		FileUtil.writeDataOutFile(mappingName + " ");
	
		initializeOriginalAlignment(LogMapWrapper.getMappings(mappingFile, fstO, sndO));
		
		if(useModules)
			if(!extractModules())
				return;

		classifyInputAndMaterialiseClosure();
		
		initialConsistencyRepair();
		
		failed = classifyAlignedAndMaterialiseClosure();
		
		if(!failed){
			computeSemanticIndexes();
			detectViolations(true, directViolations);
		}
	}
	
	public void repair(){
		repair(true);
	}
	
	public void repair(List<Pair<Integer>> violations,boolean detectAfter,
			boolean updateAlignOnto){
		repair(violations,detectAfter,updateAlignOnto,
				Collections.<MappingObjectStr> emptySet());
	}
	
	public void repair(List<Pair<Integer>> violations, 
			boolean updateAlignOnto){
		repair(violations,true,updateAlignOnto,Collections.<MappingObjectStr> emptySet());
	}
	
	public void repair(List<Pair<Integer>> violations){
		repair(violations,true,true,Collections.<MappingObjectStr> emptySet());
	}
	
	public void repair(List<Pair<Integer>> violations, boolean detectAfter, 
			boolean updateAlignOnto, Set<MappingObjectStr> mappingsToKeep){
		if(useSCC && preSCC)
			SCCRepair(directViolations,detectAfter);
		
		if(useSUB){
			addDisjointness(violations);
			conservativityRepair(updateAlignOnto,mappingsToKeep);
		}
		
		if(useSCC && !preSCC)
			SCCRepair(directViolations,detectAfter);
		
		if(detectAfter)
			detectViolations(false, directViolations);
	}

	public void repair(boolean detectAfter, boolean updateAlignOnto){
		repair(detectAfter,updateAlignOnto,Collections.<MappingObjectStr> emptySet());
	}

	public void repair(boolean detectAfter, boolean updateAlignOnto, 
			Set<MappingObjectStr> mappingsToKeep){
		if(useSCC && preSCC)
			SCCRepair(directViolations,detectAfter);

		if(useSUB){
			addDisjointness();
			conservativityRepair(updateAlignOnto,mappingsToKeep);
		}
		
		if(useSCC && !preSCC)
			SCCRepair(directViolations,detectAfter);
		
		if(detectAfter)
			detectViolations(false, directViolations);
	}
	
	public void repair(boolean detectAfter){
		repair(detectAfter,true);
	}
	
	public double[] getSelectiveRepairStats(List<Pair<Integer>> violations){
		List<Pair<Integer>> unsolvedViols = new ArrayList<>();
		
		unsolvedViols.addAll(getViolations(true, VIOL_KIND.APPROX, getRepairStep()-1));
		unsolvedViols.addAll(getViolations(false, VIOL_KIND.APPROX, getRepairStep()-1));
		
		unsolvedViols.removeAll(getViolations(true, VIOL_KIND.APPROX, getRepairStep()));
		unsolvedViols.removeAll(getViolations(false, VIOL_KIND.APPROX, getRepairStep()));
			
		return new double[]{
			Util.getPrecision(unsolvedViols, violations),
			Util.getRecall(unsolvedViols, violations),
			Util.getFMeasure(unsolvedViols, violations)
		};
	} 
	
	public Set<MappingObjectStr> getOriginalMappings(){
		return LogMapWrapper.cloneAlignment(originalMappings);
	}
	
	public Set<MappingObjectStr> getRepairedMappings(){
		return consistentMappings;
	}
		
	public LightAdjacencyList getAdjacencyList(){
		return adj;
	}
	
	public void setAdjacencyList(LightAdjacencyList adj){
		this.adj = adj;
	}
	
	public JointIndexManager getAlignIndex(){
		return alignIdx;
	}

	public JointIndexManager getOrigIndex(){
		return origIndex;
	}

	public OntologyProcessing getFstOntoProc(){
		return new OntologyProcessing(fstProcOrig);
	}

	public OntologyProcessing getSndOntoProc(){
		return new OntologyProcessing(sndProcOrig);
	}
	
	public OntologyProcessing getAlignOntoProc(){
		return new OntologyProcessing(alignProc);
	}	
	
	public OWLReasoner getFirstReasoner(){
		return fstReasoner;
	}

	public OWLReasoner getSecondReasoner(){
		return sndReasoner;
	}
	
	public int getRepairStep(){
		return repairStep;
	}
	
	public Set<MappingObjectStr> getRepair(){
		return LogMapWrapper.mappingDifference(originalMappings,consistentMappings);
	}

	private void loadOntologies(String fstOnto, String sndOnto, boolean local){
		OntoUtil.unloadAllOntologies(manager);
		manager = OntoUtil.getManager(true);
		try {
			fstO = OntoUtil.load(fstOnto, local, manager);
			// this should be fine because that exception is only thrown for identical ontologies
			try {
				sndO = OntoUtil.load(sndOnto, local, manager);
			}
			catch(OWLOntologyAlreadyExistsException e){
				sndO = fstO;
			}
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
	}

	private void conservativityRepair(boolean detectAfter){
		conservativityRepair(detectAfter, Collections.<MappingObjectStr> emptySet());
	}
	
	private void conservativityRepair(boolean detectAfter, 
			Set<MappingObjectStr> mappingsToKeep){
		// STEP 7: repair conservativity
		FileUtil.writeLogAndConsole(
				"Repair conservativity - " + Util.getCurrTime());
		long start = Util.getMSec();

		int pre = LogMapWrapper.countMappings(consistentMappings);
		
		consistentMappings = LogMapWrapper.repairInconsistentAlignments(
				fstO, sndO, mappingFile, getFstOntoProc(), getSndOntoProc(), 
				alignIdx, consistentMappings, mappingsToKeep);
		int post = LogMapWrapper.countMappings(consistentMappings); 
		
		FileUtil.writeLogAndConsole(Util.getDiffmsec(start) 
				+ " (ms) - " + Util.getCurrTime() + "\n");

		Set<OWLAxiom> diagnosis = new HashSet<>(originalAlignment);
		diagnosis.removeAll(OntoUtil.convertAlignmentToAxioms(fstO, sndO, 
				consistentMappings));

		if(Params.oaei)
			return;
		
		if(!detectAfter || ((pre-post > 0) && !updateAlignedOntology(true)))
			return;
		
		if(saveMappings){
			FileUtil.writeLogAndConsole("Serializing the repaired mappings");
			LogMapWrapper.saveMappings(testMappingDir + 
					mappingName + "-logmap", 
					fstO, sndO, consistentMappings);
		}
		
		if(saveOnto){
			FileUtil.writeLogAndConsole("Saving aligned ontology repaired by LogMap");
			String ontoName = mappingName;
			try {
				OntoUtil.save(alignOnto, testOntoDir + ontoName + "-logmap.owl", manager);
			} catch (OWLOntologyStorageException | OWLOntologyCreationException
					| IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void addDisjointness(List<Pair<Integer>> violations){
		// STEP 6: adding disjointness axioms to input ontologies
		FileUtil.writeLogAndConsole(
				"Adding disjointness axioms to input ontologies - " + 
						Util.getCurrTime());
		long start = Util.getMSec();

		for (Pair<Integer> p : violations)
			alignIdx.addDisjointness(p.getFirst(), p.getSecond(), false);

		int disjNum = violations.size();

		if(Params.oaei){
			unsolvViol1.clear();
			dirUnsolvViol1.clear();
		}
		
		FileUtil.writeLogAndConsole(disjNum + " disjoint axioms added");

		alignIdx.recreateDisjointIntervalsStructure();

		FileUtil.writeLogAndConsole(Util.getDiffmsec(start) + " (ms) -" + Util.getCurrTime());
	}
	
	private int addDisjointness(boolean fstOnto){
		int repairStepForDisj = (preSCC ? repairStep : 1);

		for (Pair<Integer> p : getViolations(fstOnto, VIOL_KIND.APPROX, repairStepForDisj))
			alignIdx.addDisjointness(p.getFirst(), p.getSecond(), false);

		return getViolations(fstOnto, VIOL_KIND.APPROX, repairStepForDisj).size();
	}
	
	private void addDisjointness(){
		// STEP 6: adding disjointness axioms to input ontologies
		FileUtil.writeLogAndConsole(
				"Adding disjointness axioms to input ontologies - " + 
						Util.getCurrTime());
		long start = Util.getMSec();

		int disjNum1 = 0, disjNum2 = 0;
//		int repairStepForDisj = (preSCC ? repairStep : 1);
//
//		for (Pair<Integer> p : getViolations(true, VIOL_KIND.APPROX, repairStepForDisj))
//			alignIdx.addDisjointness(p.getFirst(), p.getSecond(), false);
//
//		disjNum1 = getViolations(true, VIOL_KIND.APPROX, repairStepForDisj).size();
//
//		for (Pair<Integer> p : getViolations(false, VIOL_KIND.APPROX, repairStepForDisj))
//			alignIdx.addDisjointness(p.getFirst(), p.getSecond(), false);
//		
//		disjNum2 = getViolations(false, VIOL_KIND.APPROX, repairStepForDisj).size();
		
		disjNum1 = addDisjointness(true);
		disjNum2 = addDisjointness(false);

		if(Params.oaei){
			unsolvViol1.clear();
			dirUnsolvViol1.clear();			
		}
		
		FileUtil.writeLogAndConsole(disjNum1 + disjNum2 + " disjoint axioms added");

		alignIdx.recreateDisjointIntervalsStructure();

		FileUtil.writeLogAndConsole(Util.getDiffmsec(start) + " (ms) -" + Util.getCurrTime());
	}
	
	private boolean updateAlignedOntology(boolean checkUnsat){
		// STEP 8: update the aligned ontology for testing unsolved violations
		FileUtil.writeLogAndConsole(
				"Update the aligned ontology for testing unsolved violations - " + 
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

		alignReasoner = reasoners.get(0);

		if(!alignReasoner.getRootOntology().equals(alignOnto))
			throw new Error("Align ontology for reasoner mismatch");

		if(checkUnsat){
			checkIncoherentClasses();

			if(directViolations){
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
			}
			
			alignIdx = new JointIndexManager(); 
			alignProc = LogMapWrapper.indexSetup(alignOnto, alignIdx, alignReasoner);
	
			FileUtil.writeLogAndConsole(Util.getDiffmsec(start) 
					+ " (ms) - " + Util.getCurrTime() + "\n");
		}

		if(!OntoUtil.checkClassification(reasoners) 
				|| !alignReasoner.isConsistent()){
			FileUtil.writeErrorLogAndConsole("Timeout or inconsistent " +
					"aligned ontology, skipping the test");
			OntoUtil.disposeAllReasoners();
			return false;
		}

		reasoners.clear();
		
		return true;
	}
	
	private void SCCRepair(boolean directViolations, boolean detectAfter){
		
		long sccTime = Util.getMSec();
		Diagnosis d = repairSCC(fstReasoner,sndReasoner);
		sccTime = Util.getDiffmsec(sccTime);
		updateMappings(d);
		if(!d.isEmpty() && !updateAlignedOntology(true))
			return;
		if(detectAfter)
			detectViolations(false, directViolations);

//		int sccDiagSize = LogMapWrapper.countMappings(originalMappings) - d.size();

//		if(saveOnto){
//			FileUtil.writeLogAndConsole("Saving aligned ontology repaired by SCC");
//			String ontoName = mappingFile.substring(
//					mappingFile.lastIndexOf('/')+1, 
//					mappingFile.lastIndexOf('.'));
//			try {
//				OntoUtil.save(alignOnto, testOntoDir + ontoName + "-scc.owl", manager);
//			} catch (OWLOntologyStorageException | OWLOntologyCreationException
//					| IOException e) {
//				e.printStackTrace();
//			}
//		}
		
		if(saveMappings){
			FileUtil.writeLogAndConsole("Serializing the repaired mappings");
			LogMapWrapper.saveMappings(testMappingDir + 
					mappingName + "-scc", fstO, sndO, consistentMappings);
		}
		
		if(saveOnto){
			FileUtil.writeLogAndConsole("Saving aligned ontology repaired by SCC");
			String ontoName = mappingName;
			try {
				OntoUtil.save(alignOnto, testOntoDir + ontoName + "-scc.owl", manager);
			} catch (OWLOntologyStorageException | OWLOntologyCreationException
					| IOException e) {
				e.printStackTrace();
			}
		}
	}

	private Set<MappingObjectStr> initialConsistencyRepair(){
		boolean oldVal = Parameters.repair_heuristic;
		
		Parameters.repair_heuristic = false;
		consistentMappings = LogMapWrapper.repairInconsistentAlignments(
				null, mappingFile, fstO, sndO, 
				originalMappings, 
				Params.fullReasoningRepair);
		Parameters.repair_heuristic = oldVal;

		originalAlignment = Collections.unmodifiableSet(
				OntoUtil.convertAlignmentToAxioms(fstO, sndO, 
						originalMappings));

		consistentAlignment = OntoUtil.convertAlignmentToAxioms(
				fstO, sndO, consistentMappings);

		return consistentMappings;
	}
	
	public Set<MappingObjectStr> consistencyRepair(boolean useHeur){
		return consistencyRepair(useHeur,Collections.<MappingObjectStr> emptySet());
	}
	
	public Set<MappingObjectStr> consistencyRepair(boolean useHeur, 
			Set<MappingObjectStr> mappingsToKeep){
		boolean oldVal = Parameters.repair_heuristic;
		
		Parameters.repair_heuristic = useHeur;
		consistentMappings = LogMapWrapper.repairInconsistentAlignments(
				null, mappingFile, fstO, sndO, 
				originalMappings, 
				Params.fullReasoningRepair,mappingsToKeep);
		Parameters.repair_heuristic = oldVal;

		originalAlignment = Collections.unmodifiableSet(
				OntoUtil.convertAlignmentToAxioms(fstO, sndO, 
						originalMappings));

		consistentAlignment = OntoUtil.convertAlignmentToAxioms(
				fstO, sndO, consistentMappings);
		
		return consistentMappings;
	}

	private boolean classifyAlignedAndMaterialiseClosure(){
		
		List<OWLReasoner> reasoners = new ArrayList<>(2);
		long start = Util.getMSec();
		long alignClassifTime;

		try {
			FileUtil.writeLogAndConsole(consistentAlignment.size() + " mapping(s)");

			alignOnto = OntoUtil.getAlignedOntology(manager, 
					consistentAlignment, fstO, sndO);
			alignReasoner = OntoUtil.getReasoner(alignOnto, 
					getAlignReasoner(), manager);

			reasoners.clear();
			reasoners.add(alignReasoner);

			try {
				alignClassifTime = OntoUtil.ontologyClassification(true, false, 
					reasoners, Params.tryPellet);
			//			OntoUtil.checkClassification(reasoners);
			}
			catch(NullPointerException e){
				FileUtil.writeErrorLogAndConsole("NullPointer when classifying " +
						"aligned onto, skipping the test");
				OntoUtil.disposeAllReasoners();
				return true;
			}
			
			alignReasoner = reasoners.get(0);

			if(!alignReasoner.getRootOntology().equals(alignOnto))
				throw new Error("Aligned ontology for reasoner mismatch");

			if(!OntoUtil.checkClassification(reasoners) || 
					!alignReasoner.isConsistent()){
				FileUtil.writeErrorLogAndConsole("Timeout or inconsistent " +
						"aligned ontology, skipping the test");
				OntoUtil.disposeAllReasoners();
				return true;
			}
			
			checkIncoherentClasses();

			determineELKUsage(fstReasoner,sndReasoner,alignReasoner);

			if(directViolations)
				OntoUtil.saveClassificationAxioms(alignOnto, alignReasoner, manager);
			
			alignClassifTime = Util.getDiffmsec(start);
			
			if(saveOnto){
				FileUtil.writeLogAndConsole("Saving starting aligned ontology");
				String ontoName = mappingName;
				try {
					OntoUtil.save(alignOnto, 
							testOntoDir + ontoName + "-original.owl", manager);
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
			return true;
		}
		
		return false;
	}
	
	public boolean hasFailed(){
		return failed;
	}

	private void computeSemanticIndexes(){

		long start = Util.getMSec();
		FileUtil.writeLogAndConsole(
				"Compute the semantic indexes for input/aligned ontologies - " 
						+ Util.getCurrTime());

		alignIdx = new JointIndexManager();

		OntologyProcessing [] oprocs = LogMapWrapper.indexSetup(
				fstO,sndO,alignIdx,fstReasoner,sndReasoner);
		fstProcOrig = oprocs[0];
		sndProcOrig = oprocs[1];

		origIndex = new JointIndexManager(alignIdx);

		alignProc = LogMapWrapper.indexSetup(alignOnto,alignIdx,alignReasoner);

		FileUtil.writeLogAndConsole(Util.getDiffmsec(start) + " (ms) - " + 
				Util.getCurrTime() + "\n");
	}

	private void classifyInputAndMaterialiseClosure(){
		fstReasoner = OntoUtil.getReasoner(fstO, Params.reasonerKind, manager);
		sndReasoner = OntoUtil.getReasoner(sndO, Params.reasonerKind, manager);

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
			FileUtil.writeLogAndConsole("First input ontology not classified");
			OntoUtil.disposeAllReasoners();
			return;
		}
		if(!OntoUtil.checkClassification(sndReasoner)){
			FileUtil.writeLogAndConsole("Second input ontology not classified");
			OntoUtil.disposeAllReasoners();
			return;
		}

		FileUtil.writeLogAndConsole(classTime + " (ms) - " + Util.getCurrTime());

		long start = Util.getMSec();

		// create the inferred ontology generator
		try {
			OntoUtil.saveClassificationAxioms(fstO, fstReasoner, manager);
			OntoUtil.saveClassificationAxioms(sndO, sndReasoner, manager);
		}
		catch(org.semanticweb.owlapi.reasoner.InconsistentOntologyException | 
				NullPointerException e){
			FileUtil.writeErrorLogAndConsole(
					"Classified input ontologies reification failed: " 
							+ e.getMessage() + ", skipping this test");
			OntoUtil.disposeAllReasoners();
			return;
		}

		FileUtil.writeLogAndConsole(Util.getDiffmsec(start) + 
				" (ms) - " + Util.getCurrTime());
	}

	private void checkIncoherentClasses(){
//		incoherences.add(
//				alignReasoner.getUnsatisfiableClasses().getEntitiesMinusBottom().size());
//
//		if(!alignReasoner.getUnsatisfiableClasses().isSingleton()){
//			FileUtil.writeErrorLogAndConsole(
//					alignReasoner.getUnsatisfiableClasses().getEntitiesMinusBottom().size() 
//					+ " incoherent class(es): ");
//
//			for (OWLClass c : 
//				alignReasoner.getUnsatisfiableClasses().getEntitiesMinusBottom())
//				FileUtil.writeErrorLogAndConsole(c.toString());
//		}
//		if(incoherences.size() > 1){
//			int act = incoherences.get(incoherences.size()-1);
//			int pre = incoherences.get(incoherences.size()-2);
//
//			if(act > pre)
//				FileUtil.writeErrorLogAndConsole("Incoherent classes increased " +
//						"from " + pre + " to " + act);
//		}

		int unsats = alignReasoner.getUnsatisfiableClasses().getSize()-1;
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
				
			unsats = alignReasoner.getUnsatisfiableClasses().getSize()-1;
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
	}

	private boolean extractModules(){
		long start = Util.getMSec();

		OverlappingExtractor4Mappings overlapping = 
				new OverlappingExtractor4Mappings();

		try {
			overlapping.createOverlapping(fstO, sndO, originalMappings);
		} catch (Exception e) {
			FileUtil.writeErrorLogAndConsole("Error while extracting " +
					"modules, skipping the test: " + e.getMessage() + "\n" + 
					Arrays.toString(e.getStackTrace()));
			
			if(Params.oaei){
				FileUtil.writeLogAndConsole("Using the original ontologies");
				return true;
			}
			
			return false;
		}
		fstO = overlapping.getOverlappingOnto1();
		sndO = overlapping.getOverlappingOnto2();

		FileUtil.writeLogAndConsole("Module Ontology 0: " + fstO.toString());
		FileUtil.writeLogAndConsole("Module Ontology 1: " + sndO.toString());

		FileUtil.writeLogAndConsole(
				Util.getDiffmsec(start) + " (ms) - " 
						+ Util.getCurrTime() + "\n");

		return true;
	}
	
	public Set<MappingObjectStr> updateMappings(Diagnosis d){

		int pre = LogMapWrapper.countMappings(consistentMappings);

		Set<MappingObjectStr> diag = d.toMappingObjectStr();		

		LogMapWrapper.applyRepair(consistentMappings,diag);
		
		int post = LogMapWrapper.countMappings(consistentMappings);

		if((pre-post) != d.size()){
			throw new RuntimeException("SCC removed " + d.size() 
					+ " mapping(s) but only " + (pre-post) 
					+ " were actually removed");
		}
		else
			FileUtil.writeLogAndConsole("SCC removed " + (pre-post) + " mapping(s)");

		return consistentMappings;
	}
	
	public Diagnosis repairSCC(OWLReasoner fstReasoner, OWLReasoner sndReasoner){

		Diagnosis hDiag = null;

		// STEP 10: SCC-based diagnosis computation
		FileUtil.writeLogAndConsole("SCC-based diagnosis computation - " 
				+ Util.getCurrTime());

		long start = Util.getMSec();

		LightAdjacencyList adj;
		try {
			adj = new LightAdjacencyList(fstO, sndO, null, false, fstReasoner, 
					sndReasoner);
		} catch (ClassificationTimeoutException e) {
			return hDiag;
		}

		adj.loadMappings(mappingFile == null ? null : new File(mappingFile), consistentMappings);

		Set<LightSCC> problematicSCCs = new HashSet<>();
		LightSCCs globalSCCs = new LightSCCs();
//				adj.computeGlobalSCCsAndProblematicMappings(problematicSCCs, null);
		hDiag = adj.computeDiagnosis(globalSCCs, adj.getOriginalMappings(), 
				problematicSCCs, null, Util.getMSec());

		FileUtil.writeLogAndConsole(Util.getDiffmsec(start) + " (ms) - " 
				+ Util.getCurrTime() + "\n");

		return hDiag;
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
			Pair<List<Pair<Integer>>> viols, boolean directViols){
		
		int num1 = viols.getFirst().size();
		int num2 = viols.getSecond().size();
		
		switch(kind){
		case APPROX:
			
			// store them anyway for repair, cleared later
			if(directViols){
				dirUnsolvViol1.add(viols);
				numDirUnsolvViol1.add(new Pair<>(num1,num2));				
			}
			else {
				unsolvViol1.add(viols);
				numUnsolvViol1.add(new Pair<>(num1,num2));
			}
			
			break;
			
		case FULL:		
			
			if(directViols){
				if(Params.storeViolations)
					dirUnsolvViol2.add(viols);
				numDirUnsolvViol2.add(new Pair<>(num1,num2));				
			}
			else {
				if(Params.storeViolations)
					unsolvViol2.add(viols);
				numUnsolvViol2.add(new Pair<>(num1,num2));				
			}
			
			break;
			
		case EQONLY:
			
			if(directViols){
				if(Params.storeViolations)
					dirUnsolvViolEQ.add(viols);
				numDirUnsolvViolEQ.add(new Pair<>(num1,num2));				
			}
			else {
				if(Params.storeViolations)
					unsolvViolEQ.add(viols);
				numUnsolvViolEQ.add(new Pair<>(num1,num2));				
			}

			break;
		}
	}

	public long detectViolations(boolean pre, boolean rootViolations){

		// STEP: detect violations using semantic indexes
		FileUtil.writeLogAndConsole("Detect violations using semantic indexes - " + 
				Util.getCurrTime());
		long disjComputTime = Util.getMSec();
		long start = disjComputTime;

		FileUtil.writeLogAndConsole("Detection of violations kind 1");
		storeUnsolvedViolations(VIOL_KIND.APPROX, 
				LogMapWrapper.parallelConservativityViolationDetection(
				origIndex, fstProcOrig, sndProcOrig, alignIdx, 
				alignProc, rootViolations, false, Params.suppressDirectViolationsOutput, 
				fstO,sndO,alignOnto), false);

		if(rootViolations){
			FileUtil.writeLogAndConsole("Detection of direct violations kind 1");
			storeUnsolvedViolations(VIOL_KIND.APPROX, 
					LogMapWrapper.parallelDirectConservativityViolationDetection(
				origIndex, fstProcOrig, sndProcOrig, alignIdx, 
				alignProc, false, Params.suppressDirectViolationsOutput, 
				fstO,sndO,alignOnto,new Pair<List<Pair<Integer>>>(
						new ArrayList<>(unsolvViol1.get(repairStep).getFirst()),
						new ArrayList<>(unsolvViol1.get(repairStep).getSecond())
						)),true);
		}
		
		disjComputTime = Util.getDiffmsec(disjComputTime);

		if(!Params.oaei){
			FileUtil.writeLogAndConsole("Detection of violations kind 2");
			storeUnsolvedViolations(VIOL_KIND.FULL,
					LogMapWrapper.parallelConservativityViolationDetection(
					origIndex, fstProcOrig, sndProcOrig, alignIdx, 
					alignProc, rootViolations, true, Params.suppressDirectViolationsOutput, 
					fstO,sndO,alignOnto),false);
			
			if(Params.visualizationGUI || rootViolations){
				FileUtil.writeLogAndConsole("Detection of direct violations kind 2");
				storeUnsolvedViolations(VIOL_KIND.FULL,
						LogMapWrapper.parallelDirectConservativityViolationDetection(
					origIndex, fstProcOrig, sndProcOrig, alignIdx, 
					alignProc, true, Params.suppressDirectViolationsOutput, 
					fstO,sndO,alignOnto,new Pair<List<Pair<Integer>>>(
							new ArrayList<>(unsolvViol2.get(repairStep).getFirst()),
							new ArrayList<>(unsolvViol2.get(repairStep).getSecond())
							)), true);
			}
	
			FileUtil.writeLogAndConsole("Detection of violations kind 3");
			storeUnsolvedViolations(VIOL_KIND.EQONLY,
					LogMapWrapper.parallelEqConservativityViolationDetection(
							origIndex, fstProcOrig, sndProcOrig, alignIdx, 
							alignProc, Params.suppressDirectViolationsOutput),false);
	
			if(Params.visualizationGUI || rootViolations){
				FileUtil.writeLogAndConsole("Detection of direct violations kind 3");
				List<Pair<Integer>> f,s;
				Pair<List<Pair<Integer>>> dirUnsolvViolEQList = 
						new Pair<List<Pair<Integer>>>(
								f = new ArrayList<Pair<Integer>>(), 
								s = new ArrayList<Pair<Integer>>());
	
				for (Pair<Integer> v : unsolvViolEQ.get(repairStep).getFirst())
					if(dirUnsolvViol2.get(repairStep).getFirst().contains(v))
						f.add(v);
				
				for (Pair<Integer> v : unsolvViolEQ.get(repairStep).getSecond())
					if(dirUnsolvViol2.get(repairStep).getSecond().contains(v))
						s.add(v);
				
				storeUnsolvedViolations(VIOL_KIND.EQONLY,dirUnsolvViolEQList,true);
			}
		}
		
		repairStep++;

		if(!pre){
			if(getTotalActualStepViolationNumber(VIOL_KIND.APPROX, directViolations) > 0)
				FileUtil.writeErrorLogAndConsole(
						getTotalActualStepViolationNumber(VIOL_KIND.APPROX, directViolations) + "/" 
								+ getTotalInitialViolationNumber(VIOL_KIND.APPROX, directViolations) 
								+ " unsolved violation(s) approximated notion");

			if(getTotalActualStepViolationNumber(VIOL_KIND.FULL, directViolations) > 0)
				FileUtil.writeErrorLogAndConsole(
						getTotalActualStepViolationNumber(VIOL_KIND.FULL, directViolations) + "/" 
								+ getTotalInitialViolationNumber(VIOL_KIND.FULL, directViolations) 
								+ " unsolved violation(s) full notion");

			if(getTotalActualStepViolationNumber(VIOL_KIND.EQONLY, directViolations) > 0)
				FileUtil.writeErrorLogAndConsole(
						getTotalActualStepViolationNumber(VIOL_KIND.EQONLY, directViolations) + "/" 
								+ getTotalInitialViolationNumber(VIOL_KIND.EQONLY, directViolations)
								+ " unsolved violation(s) equivalences ONLY");

			if(getTotalViolationNumber(VIOL_KIND.APPROX, repairStep, directViolations) > 
			getTotalViolationNumber(VIOL_KIND.APPROX, repairStep-1, directViolations) || 
			getTotalViolationNumber(VIOL_KIND.FULL, repairStep, directViolations) > 
			getTotalViolationNumber(VIOL_KIND.FULL, repairStep-1, directViolations) ||
			getTotalViolationNumber(VIOL_KIND.EQONLY, repairStep, directViolations) > 
			getTotalViolationNumber(VIOL_KIND.EQONLY, repairStep-1, directViolations))
				FileUtil.writeErrorLogAndConsole(
						"Increased number of violations!");

			FileUtil.writeLogAndConsole(Util.getDiffmsec(start) + " (ms) - " 
					+ Util.getCurrTime() + "\n");

			return Util.getDiffmsec(start);
		}

		return disjComputTime;
	}

	public List<Pair<Integer>> getViolations(
			boolean firstOnto, VIOL_KIND kind, int repairStep){
		return getViolations(firstOnto, kind, repairStep, false);
	}
	
	public List<Pair<Integer>> getDirectViolations(
			boolean firstOnto, VIOL_KIND kind, int repairStep){
		return getViolations(firstOnto, kind, repairStep, true);
	}
	
	public List<Pair<Integer>> getViolations(boolean firstOnto, VIOL_KIND kind, 
			int repairStep, boolean direct){

		switch (kind) {
		case APPROX:
			if(firstOnto)
				return Collections.unmodifiableList(
						direct ? dirUnsolvViol1.get(repairStep-1).getFirst() : 
						unsolvViol1.get(repairStep-1).getFirst());

			return Collections.unmodifiableList(
					direct ? dirUnsolvViol1.get(repairStep-1).getSecond() : 
				unsolvViol1.get(repairStep-1).getSecond());

		case FULL:
			if(firstOnto)
				return Collections.unmodifiableList(
						direct ? dirUnsolvViol2.get(repairStep-1).getFirst() : 
							unsolvViol2.get(repairStep-1).getFirst());

			return Collections.unmodifiableList(
					direct ? dirUnsolvViol2.get(repairStep-1).getSecond() : 
						unsolvViol2.get(repairStep-1).getSecond());

		case EQONLY:
			if(firstOnto)
				return Collections.unmodifiableList(
						direct ? dirUnsolvViolEQ.get(repairStep-1).getFirst() : 
							unsolvViolEQ.get(repairStep-1).getFirst());

			return Collections.unmodifiableList(
					direct ? dirUnsolvViolEQ.get(repairStep-1).getSecond() : 
				unsolvViolEQ.get(repairStep-1).getSecond());

		default:
			throw new Error("Unknown/unsupported violation kind " + kind);
		}
	}

	public int getViolationNumber(boolean firstOnto, VIOL_KIND kind, int repairStep, boolean direct){
//		return getViolations(firstOnto, kind, repairStep, false).size();
		switch (kind) {
		case APPROX:
			if(firstOnto)
				return direct ? numDirUnsolvViol1.get(repairStep-1).getFirst() : 
						numUnsolvViol1.get(repairStep-1).getFirst();

			return direct ? numDirUnsolvViol1.get(repairStep-1).getSecond() : 
				numUnsolvViol1.get(repairStep-1).getSecond();

		case FULL:
			if(firstOnto)
				return direct ? numDirUnsolvViol2.get(repairStep-1).getFirst() : 
					numUnsolvViol2.get(repairStep-1).getFirst();

			return direct ? numDirUnsolvViol2.get(repairStep-1).getSecond() : 
				numUnsolvViol2.get(repairStep-1).getSecond();

		case EQONLY:
			if(firstOnto)
				return direct ? numDirUnsolvViolEQ.get(repairStep-1).getFirst() : 
					numUnsolvViolEQ.get(repairStep-1).getFirst();

			return direct ? numDirUnsolvViolEQ.get(repairStep-1).getSecond() : 
				numUnsolvViolEQ.get(repairStep-1).getSecond();

		default:
			throw new Error("Unknown/unsupported violation kind " + kind);
		}
	}

	public int getTotalViolationNumber(VIOL_KIND kind, int repairStep, boolean direct){
		return getViolationNumber(true,kind,repairStep,direct) + 
				getViolationNumber(false,kind,repairStep,direct);
	}

	public int getTotalActualStepViolationNumber(VIOL_KIND kind, boolean direct){
		return getViolationNumber(true,kind,repairStep,direct) + 
				getViolationNumber(false,kind,repairStep,direct);
	}

	public int getTotalInitialViolationNumber(VIOL_KIND kind, boolean direct){
		return getViolationNumber(true,kind,1,direct) + getViolationNumber(false,kind,1,direct);
	}

	public boolean isDirectViolation(Pair<OWLClass> v) {
//		VIOL_KIND [] arr = {VIOL_KIND.APPROX, VIOL_KIND.EQONLY, VIOL_KIND.FULL};
//		
//		for (VIOL_KIND kind : arr)
//			if(isDirectViolation(v, kind))
//				return true;
//		
//		return false;
		
		return isDirectViolation(v, Params.violKindToShow);
	}

	public boolean isDirectViolation(Pair<OWLClass> v, VIOL_KIND kind) {
		
		return getViolations(true, kind, repairStep, true).contains(
				LogMapWrapper.getPairOfIdentifiersFromPairOfClasses(v, fstProcOrig)) 
					|| getViolations(false, kind, repairStep, true).contains(
							LogMapWrapper.getPairOfIdentifiersFromPairOfClasses(v, sndProcOrig));
	}

	public void dispose() {
		OntoUtil.disposeAllReasoners();
		OntoUtil.unloadOntologies(alignOnto);
	}

	public String getFirstOntologyIRIStr() {
		return OntoUtil.extractIRIString(fstO);
	}
	
	public String getSecondOntologyIRIStr() {
		return 	OntoUtil.extractIRIString(sndO);
	}
}
