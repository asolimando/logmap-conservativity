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
package logmap;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owl.align.Relation;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.EntityType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import com.google.common.collect.Sets;

import auxStructures.Pair;

import enumerations.DISJ_CHECK;
import enumerations.REASONER_KIND;
import enumerations.REPAIR_STRATEGY;
import fr.inrialpes.exmo.align.impl.URICell;
import fr.inrialpes.exmo.align.impl.rel.EquivRelation;
import fr.inrialpes.exmo.align.impl.rel.SubsumeRelation;
import fr.inrialpes.exmo.align.impl.rel.SubsumedRelation;
import thread.ConservativityViolationDetectionThread;
import thread.DisjointnessEnforcementThread;
import thread.EqOnlyConservativityViolationDetectionThread;
import uk.ac.ox.krr.logmap2.LogMap2_RepairFacility;
import uk.ac.ox.krr.logmap2.Parameters;
import uk.ac.ox.krr.logmap2.indexing.IndexManager;
import uk.ac.ox.krr.logmap2.indexing.JointIndexManager;
import uk.ac.ox.krr.logmap2.indexing.OntologyProcessing;
import uk.ac.ox.krr.logmap2.indexing.ReasonerBasedIndexManager;
import uk.ac.ox.krr.logmap2.indexing.entities.ClassIndex;
import uk.ac.ox.krr.logmap2.io.LogOutput;
import uk.ac.ox.krr.logmap2.io.OAEIAlignmentOutput;
import uk.ac.ox.krr.logmap2.io.OutPutFilesManager;
import uk.ac.ox.krr.logmap2.lexicon.LexicalUtilities;
import uk.ac.ox.krr.logmap2.mappings.CandidateMappingManager;
import uk.ac.ox.krr.logmap2.mappings.MappingManager;
import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;
import uk.ac.ox.krr.logmap2.oaei.reader.MappingsReaderManager;
import uk.ac.ox.krr.logmap2.overlapping.LexicalOverlappingExtractor;
import uk.ac.ox.krr.logmap2.overlapping.NoOverlappingExtractor;
import uk.ac.ox.krr.logmap2.overlapping.OverlappingExtractor;
import uk.ac.ox.krr.logmap2.reasoning.ReasonerManager;
import uk.ac.ox.krr.logmap2.repair.AnchorsAssessmentFullReasoning;
import uk.ac.ox.krr.logmap2.repair.hornSAT.DowlingGallierHornSAT;
import uk.ac.ox.krr.logmap2.repair.hornSAT.HornClause;
import uk.ac.ox.krr.logmap2.utilities.Utilities;
import util.FileUtil;
import util.OntoUtil;
import util.Params;
import util.Util;

public class LogMapWrapper {

	private static int cellID = 0;

	public static void setLogMapOutput(boolean enabled){
		LogOutput.showOutpuLog(enabled); 
//		LogOutput.showOutpuLogAlways(enabled); 
	}

	public static Cell convertToCell(MappingObjectStr m){

		Relation r = null;
		if(m.getTypeOfMapping() == MappingObjectStr.EQ)
			r = EquivRelation.createRelation("=");
		else if(m.getTypeOfMapping() == MappingObjectStr.SUB)
			r = SubsumedRelation.createRelation("<");
		else if(m.getTypeOfMapping() == MappingObjectStr.SUP)
			r = SubsumeRelation.createRelation(">");

		Cell c = null;
		try {
			c = new URICell(cellID++ + "", 
					IRI.create(m.getIRIStrEnt1()).toURI(), 
					IRI.create(m.getIRIStrEnt2()).toURI(), 
					r, 
					m.getConfidence());
		} catch (AlignmentException e) {
			FileUtil.printStackTrace(e, true);
		}
		
		return c;
	}
	
	public static Set<MappingObjectStr> convertToLogMapAlignment(OWLOntology o1, 
			OWLOntology o2, Alignment align) throws AlignmentException{
		Set<MappingObjectStr> res = new HashSet<>();
		for (Cell c : align) {
			MappingObjectStr m = convertToLogMapMapping(o1, o2, c);
			if(m != null)
				res.add(m);
		}
		return res;
	}
	
	public static MappingObjectStr convertToLogMapMapping(OWLOntology o1, OWLOntology o2, Cell c) throws AlignmentException{
		int dir = -1;
		
		int typeMapping = -1;
		
		OWLEntity e1 = null, e2 = null; 
		try {
			e1 = OntoUtil.getEntityFromName(o1, 
					OntoUtil.getIRIShortFragment(IRI.create(c.getObject1AsURI())));
			e2 = OntoUtil.getEntityFromName(o2, 
					OntoUtil.getIRIShortFragment(IRI.create(c.getObject2AsURI())));
			
			if(e1 == null || e2 == null){
				e1 = OntoUtil.getEntityFromName(o2, 
						OntoUtil.getIRIShortFragment(IRI.create(c.getObject1AsURI())));
				e2 = OntoUtil.getEntityFromName(o1, 
						OntoUtil.getIRIShortFragment(IRI.create(c.getObject2AsURI())));
			}
		} catch (AlignmentException e) {
			FileUtil.printStackTrace(e, true);
			return null;
		}

		if(e1 == null || e2 == null){
			FileUtil.writeErrorLogAndConsole("Unknown entities, cannot convert");
			return null;
		}
		
		if(e1.isOWLClass() && e2.isOWLClass())
			typeMapping = MappingObjectStr.CLASSES;
		else if(e1.isOWLDataProperty() && e2.isOWLDataProperty())
			typeMapping = MappingObjectStr.DATAPROPERTIES;
		else if(e1.isOWLObjectProperty() && e2.isOWLObjectProperty())
			typeMapping = MappingObjectStr.OBJECTPROPERTIES;
		else if(e1.isOWLNamedIndividual() && e2.isOWLNamedIndividual())
			typeMapping = MappingObjectStr.INSTANCES;
		else {
			FileUtil.writeErrorLogAndConsole("Unknown entities type, cannot convert");
			return null;
		}
		
		if(c.getRelation() instanceof EquivRelation)
			dir = MappingObjectStr.EQ;
		else if(c.getRelation() instanceof SubsumedRelation)
			dir = MappingObjectStr.SUB;
		else if(c.getRelation() instanceof SubsumeRelation)
			dir = MappingObjectStr.SUP;
		else {
			throw new RuntimeException("Unknown relation for cell: " + c);
		}
		return new MappingObjectStr(c, dir, typeMapping);
	}
	
	public static void sanitizeMappingType(OWLOntology onto1, OWLOntology onto2, 
			Set<MappingObjectStr> mappings){
		int discarded = 0, origNum = LogMapWrapper.countMappings(mappings);

		if(onto1 != null && onto2 != null){
			Iterator<MappingObjectStr> itr = mappings.iterator();
			MappingObjectStr map;
			while(itr.hasNext()){
				map = itr.next();

				if (onto1.containsClassInSignature(
						IRI.create(map.getIRIStrEnt1()), true)
						&& onto2.containsClassInSignature(
								IRI.create(map.getIRIStrEnt2()), true)) {
					map.setTypeOfMapping(Utilities.CLASSES);
				}
				else if (onto1.containsObjectPropertyInSignature(
						IRI.create(map.getIRIStrEnt1()), true)
						&& onto2.containsObjectPropertyInSignature(
								IRI.create(map.getIRIStrEnt2()), true)) {
					map.setTypeOfMapping(Utilities.OBJECTPROPERTIES);
				}
				else if (onto1.containsDataPropertyInSignature(
						IRI.create(map.getIRIStrEnt1()), true)
						&& onto2.containsDataPropertyInSignature(
								IRI.create(map.getIRIStrEnt2()), true)) {
					map.setTypeOfMapping(Utilities.DATAPROPERTIES);
				}
				else if (onto1.containsIndividualInSignature(
						IRI.create(map.getIRIStrEnt1()), true)
						&& onto2.containsIndividualInSignature(
								IRI.create(map.getIRIStrEnt2()), true)) {
					map.setTypeOfMapping(Utilities.INSTANCES);
				}
				else {
					//map.setTypeOfMapping(Utilities.UNKNOWN);
					itr.remove();
					discarded+=LogMapWrapper.countMappings(map);
				}
			}
		}

		if(discarded > 0)
			FileUtil.writeLogAndConsole(discarded + "/" + origNum + 
					" ill-typed mapping(s), discarded");
	}

	public static Set<MappingObjectStr> getMappings(String mappingPathname, 
			OWLOntology onto1, OWLOntology onto2){
		MappingsReaderManager readermanager = 
				new MappingsReaderManager(mappingPathname, "RDF");
		Set<MappingObjectStr> mappings = readermanager.getMappingObjects();

		sanitizeMappingType(onto1, onto2, mappings);

		return mappings;
	}

	public static Set<MappingObjectStr> getMappings(String mappingPathname){
		MappingsReaderManager readermanager = 
				new MappingsReaderManager(mappingPathname, "RDF");
		Set<MappingObjectStr> mappings = readermanager.getMappingObjects();

		return mappings;
	}

	public static Set<MappingObjectStr> repairAlignmentAlignOntoIndex(
			OWLOntology onto1, OWLOntology onto2,
			String mappingPathname, OntologyProcessing op, 
			JointIndexManager index, Set<MappingObjectStr> cleanAlign){
		return repairAlignmentAlignOntoIndex(onto1,onto2,mappingPathname,op,
				index,cleanAlign);
	}

	public static Set<MappingObjectStr> repairAlignmentAlignOntoIndex(
			OWLOntology onto1, OWLOntology onto2,
			String mappingPathname, OntologyProcessing op, 
			JointIndexManager index, Set<MappingObjectStr> cleanAlign, 
			Set<MappingObjectStr> mappingToKeep){

		FileUtil.writeLogAndConsole("LogMap repairing (using aligned index)");

		Set<MappingObjectStr> input_mappings = cleanAlign != null 
				? cleanAlign : getMappings(mappingPathname, null, null);

		LogMap2_RepairFacility logmap2_repair = 
				new LogMap2_RepairFacility(onto1, onto2, 
						new OntologyProcessing(op), 
						new JointIndexManager(index), 
						input_mappings, false, false, mappingToKeep);

		Set<MappingObjectStr> repaired_mappings = logmap2_repair.getCleanMappings();
		fixRepairedMappings(input_mappings, repaired_mappings);

		FileUtil.writeLogAndConsole("Repaired mappings using LogMap: " 
				+ LogMapWrapper.countMappings(repaired_mappings));
		FileUtil.writeLogAndConsole("Mappings removed by LogMap: " 
				+ (LogMapWrapper.countMappings(input_mappings) 
						- LogMapWrapper.countMappings(repaired_mappings)));

		return repaired_mappings;
	}

	public static int findCorrespondingIdentifier(OWLClass c, 
			JointIndexManager index, OntologyProcessing op){

		Integer id = op.getClass2Identifier().get(c);

		if(id != null)
			return id;
		throw new Error("Identifier for class " + c + " not found!!");
	}

	public static boolean compareTwoIndexes(JointIndexManager mainIdx, 
			OntologyProcessing mainOP, JointIndexManager otherIdx,
			OntologyProcessing otherOP){

		boolean ok = true;

		for (Integer i : mainIdx.getClassIdentifierSet()) {
			OWLClass ci = mainIdx.getOWLClass4ConceptIndex(i); 
			int oi = findCorrespondingIdentifier(ci, otherIdx, otherOP);
			for (Integer j : mainIdx.getClassIdentifierSet()) {
				if(i==j || 
						mainIdx.getClassIndex(i).getOntologyId() != 
						mainIdx.getClassIndex(j).getOntologyId())
					continue;
				OWLClass cj = mainIdx.getOWLClass4ConceptIndex(j); 
				int oj = findCorrespondingIdentifier(cj, otherIdx, otherOP);

				if(mainIdx.areDisjoint(i, j) != otherIdx.areDisjoint(oi,oj)){
					FileUtil.writeErrorLogAndConsole(ci + " DISJ " + cj + ":");
					FileUtil.writeErrorLogAndConsole("Main = " + 
							mainIdx.areDisjoint(i, j));
					FileUtil.writeErrorLogAndConsole("Other = " + 
							otherIdx.areDisjoint(oi,oj) + "\n");
					ok = false;
				}

				if(mainIdx.areDisjoint(j, i) != otherIdx.areDisjoint(oj,oi)){
					FileUtil.writeErrorLogAndConsole(cj + " DISJ " + ci + ":\n");
					FileUtil.writeErrorLogAndConsole("Main = " + mainIdx.areDisjoint(j, i));
					FileUtil.writeErrorLogAndConsole("Other = " + 
							otherIdx.areDisjoint(oj,oi) + "\n");
					ok = false;
				}

				if(mainIdx.isSubClassOf(i,j) != otherIdx.isSubClassOf(oi,oj)){
					FileUtil.writeErrorLogAndConsole(ci + " SUB " + cj + ":\n");
					FileUtil.writeErrorLogAndConsole("Main = " + mainIdx.isSubClassOf(i,j));
					FileUtil.writeErrorLogAndConsole("Other = " + otherIdx.isSubClassOf(oi,oj) + "\n");
					ok = false;
				}

				if(mainIdx.isSuperClassOf(i,j) != otherIdx.isSuperClassOf(oi,oj)){
					FileUtil.writeErrorLogAndConsole(ci + " SUP " + cj + ":\n");
					FileUtil.writeErrorLogAndConsole("Main = " + mainIdx.isSuperClassOf(i,j));
					FileUtil.writeErrorLogAndConsole("Other = " + otherIdx.isSuperClassOf(oi,oj) + "\n");
					ok = false;
				}
			}
		}

		if(!ok)
			throw new Error("Mismatch between the indices");

		return ok;
	}

	// kept as a sanity check but the bug has been fixed in LogMap 
	// (AnchorAssessment class, method "remove_weaken_ConflictiveMappings2")
	private static void fixRepairedMappings(Set<MappingObjectStr> input, 
			Set<MappingObjectStr> repaired){
		Iterator<MappingObjectStr> itr = repaired.iterator();
		int fixNum = 0;

		ext:
			while(itr.hasNext()){
				MappingObjectStr rm = itr.next();
				if(rm.getMappingDirection() == Utilities.EQ)
					continue;
				for (MappingObjectStr im : input){
					if(MappingObjectStr.doCoincide(rm, im))
						continue ext;
					if(MappingObjectStr.areCompatibleMappings(rm, im)){
						// it is a weakening
						if(im.getMappingDirection() == Utilities.EQ)
							continue ext;
						// the opposite mappings exists, should be deleted...
						if(MappingObjectStr.haveOppositeDirection(rm, im)){
							itr.remove();
							fixNum++;
							continue ext;
						}
					}
				}
				throw new RuntimeException("Reaired mapping " + rm 
						+ " did not exist as input mapping");
			}

		if(fixNum > 0){
			FileUtil.writeLogAndConsole(fixNum + 
					" wrong weakened mapping(s) deleted");
			for (MappingObjectStr m : repaired)
				FileUtil.writeLogAndConsole(m.toString());
		}
	}

	public static boolean isContained(MappingObjectStr m,
			Set<MappingObjectStr> alignment){
		return isContained(m, alignment,false);
	}
	
	public static boolean isContained(MappingObjectStr m,
			Set<MappingObjectStr> alignment, boolean allowSubsumed){
		
		for (MappingObjectStr rem : alignment){
			if(MappingObjectStr.doCoincide(m, rem))
				return true;
			if(allowSubsumed && 
					MappingObjectStr.isWeakeningOf(m, rem))
				return true;
		}
		
		return false;
	}

	public static Set<MappingObjectStr> mappingDifference(
			Set<MappingObjectStr> a,
			Set<MappingObjectStr> b) {
		
//		System.out.println(a.toString().replace(", ", ",\n"));
//		System.out.println(b.toString().replace(", ", ",\n"));
		
		Set<MappingObjectStr> res = new HashSet<>(a.size());
		
		for (MappingObjectStr m : a)
			res.add(new MappingObjectStr(m));
		
		Iterator<MappingObjectStr> itr;
		MappingObjectStr m;

		for (MappingObjectStr rem : b){
			itr = res.iterator();
			while(itr.hasNext()){
				m = itr.next();
				if(MappingObjectStr.doCoincide(m, rem)){
					itr.remove();
					break;
				}
				if(MappingObjectStr.areCompatibleMappings(m,rem))
					m.weakenMapping(rem);
			}
		}
		
		return res;
	}

	public static Set<MappingObjectStr> applyRepair(
			Set<MappingObjectStr> consistentMappings, 
			Set<MappingObjectStr> repair){

		int pre = LogMapWrapper.countMappings(consistentMappings);

		Iterator<MappingObjectStr> itr;
		MappingObjectStr m;

		for (MappingObjectStr rem : repair){
			itr = consistentMappings.iterator();
			while(itr.hasNext()){
				m = itr.next();
				if(MappingObjectStr.doCoincide(m, rem)){
					itr.remove();
					continue; // should be a break but we lack reliable regression tests...
				}
				if(MappingObjectStr.areCompatibleMappings(m,rem))
					m.weakenMapping(rem);
			}
		}

		int post = LogMapWrapper.countMappings(consistentMappings);

		if((pre-post) != LogMapWrapper.countMappings(repair)){
			throw new RuntimeException("Removed " + (pre-post) 
					+ " mappings instead of " 
					+ LogMapWrapper.countMappings(repair));
		}

		return consistentMappings;
	}

	public static Set<MappingObjectStr> repairInconsistentAlignments(
			OWLOntology onto1, OWLOntology onto2,
			String mappingPathname, OntologyProcessing op1, 
			OntologyProcessing op2, JointIndexManager index, 
			Set<MappingObjectStr> cleanAlign){
		return repairInconsistentAlignments(onto1, onto2, mappingPathname, 
				op1, op2, index, cleanAlign, 
				Collections.<MappingObjectStr> emptySet());
	}

	public static Set<MappingObjectStr> repairInconsistentAlignments(
			OWLOntology onto1, OWLOntology onto2,
			String mappingPathname, OntologyProcessing op1, 
			OntologyProcessing op2, JointIndexManager index, 
			Set<MappingObjectStr> cleanAlign, 
			Set<MappingObjectStr> mappingsToKeep){

		FileUtil.writeLogAndConsole("LogMap repairing (reusing indexes)");

		Set<MappingObjectStr> repaired_mappings = null;
		Set<MappingObjectStr> input_mappings = new HashSet<>(cleanAlign != null 
				? cleanAlign : getMappings(mappingPathname, null, null));

		FileUtil.writeLogAndConsole("Number of original mappings: " 
				+ LogMapWrapper.countMappings(input_mappings));

		LogMap2_RepairFacility logmap2_repair = 
				new LogMap2_RepairFacility(onto1, onto2, 
						new OntologyProcessing(op1), 
						new OntologyProcessing(op2), 
						new JointIndexManager(index), 
						input_mappings, false, false);

		//Set of mappings repaired by LogMap
		repaired_mappings = logmap2_repair.getCleanMappings();
		fixRepairedMappings(input_mappings,repaired_mappings);

		FileUtil.writeLogAndConsole("Repaired mappings using LogMap: " 
				+ LogMapWrapper.countMappings(repaired_mappings));
		FileUtil.writeLogAndConsole("Mappings removed by LogMap: " 
				+ (LogMapWrapper.countMappings(input_mappings) 
						- LogMapWrapper.countMappings(repaired_mappings)));

		if(Params.testMode){
			//check if input mappings lead to unsatisfiabilities
			FileUtil.writeLogAndConsole("Satisfiability with input mappings");
			try {
				logmap2_repair.checkSatisfiabilityInputMappings();

				//check if repaired mappings by LogMap still lead to 
				// unsatisfiabilities 
				FileUtil.writeLogAndConsole("Satisfiability with repaired " +
						"mappings using LogMap");

				logmap2_repair.checkSatisfiabilityCleanMappings();
			} catch (Exception e) {
				FileUtil.writeErrorLogAndConsole(e.getMessage());
			}
		} 

		return repaired_mappings;
	}

	public static Set<MappingObjectStr> cloneAlignment(Set<MappingObjectStr> align){
		Set<MappingObjectStr> cloned = new HashSet<>(align.size());
		
		for (MappingObjectStr m : align)
			cloned.add(new MappingObjectStr(m));			
		
		return cloned;
	}

	public static Set<MappingObjectStr> repairInconsistentAlignments(
			OWLReasoner reasoner, String mappingPathname, OWLOntology onto1, 
			OWLOntology onto2, Set<MappingObjectStr> cleanAlign, 
			boolean fullReasoningRepair){
		return repairInconsistentAlignments(reasoner, mappingPathname, onto1, 
				onto2, cleanAlign, fullReasoningRepair, 
				Collections.<MappingObjectStr> emptySet());
	}

	public static Set<MappingObjectStr> repairInconsistentAlignments(
			OWLReasoner reasoner, String mappingPathname, OWLOntology onto1, 
			OWLOntology onto2, Set<MappingObjectStr> cleanAlign, 
			boolean fullReasoningRepair, Set<MappingObjectStr> mappingsToKeep){
		Set<MappingObjectStr> repaired_mappings = null;
		try {
			if(reasoner == null || (!Params.testMode 
					|| !reasoner.isConsistent())){
				FileUtil.writeLogAndConsole("LogMap repairing");

				Set<MappingObjectStr> input_mappings = new HashSet<>(
						cleanAlign != null ? cloneAlignment(cleanAlign) : 
							getMappings(mappingPathname, onto1, onto2));

				FileUtil.writeLogAndConsole("Number of original mappings: " 
						+ LogMapWrapper.countMappings(input_mappings));

				// Param 3: If the intersection or overlapping of the ontologies 
				// are extracted before the repair
				// Param 4: If the repair is performed in a two steps process 
				// (optimal) or in one cleaning step (more aggressive)
				LogMap2_RepairFacility logmap2_repair = 
						new LogMap2_RepairFacility(onto1, onto2, 
								input_mappings, false, false);

				//Set of mappings repaired by LogMap
				repaired_mappings = logmap2_repair.getCleanMappings();
				fixRepairedMappings(input_mappings, repaired_mappings);

				FileUtil.writeLogAndConsole("Repaired mappings using LogMap: " 
						+ LogMapWrapper.countMappings(repaired_mappings));
				FileUtil.writeLogAndConsole("Mappings removed by LogMap: " 
						+ (LogMapWrapper.countMappings(input_mappings) 
								- LogMapWrapper.countMappings(repaired_mappings)));

				if(Params.testMode){
					//check if input mappings lead to unsatisfiabilities
					FileUtil.writeLogAndConsole("Satisfiability with input mappings");
					logmap2_repair.checkSatisfiabilityInputMappings();

					//check if repaired mappings by LogMap still lead to 
					// unsatisfiabilities 
					FileUtil.writeLogAndConsole("Satisfiability with repaired " +
							"mappings using LogMap");
					logmap2_repair.checkSatisfiabilityCleanMappings();
				}

				if(false && Params.fullReasoningRepair){

					LexicalUtilities lexicalUtilities = new LexicalUtilities();

					OverlappingExtractor overlappingExtractor;

					if (!Parameters.use_overlapping)
						overlappingExtractor = new NoOverlappingExtractor();
					else
						overlappingExtractor = 
						new LexicalOverlappingExtractor(lexicalUtilities);

					overlappingExtractor.createOverlapping(onto1, onto2);

					IndexManager index = new ReasonerBasedIndexManager();

					//					overlappingExtractor.keepOnlyTBOXOverlapping();

					OntologyProcessing onto_process1 = 
							new OntologyProcessing(
									overlappingExtractor.getOverlappingOnto1(), 
									index, lexicalUtilities);
					OntologyProcessing onto_process2 = 
							new OntologyProcessing(
									overlappingExtractor.getOverlappingOnto2(), 
									index, lexicalUtilities);

					onto_process1.precessLexicon();
					onto_process2.precessLexicon();

					lexicalUtilities.clearStructures();

					onto_process1.clearStemmedLabels();
					onto_process2.clearStemmedLabels();

					onto_process1.setTaxonomicData();
					onto_process2.setTaxonomicData();

					overlappingExtractor.keepOnlyTBOXOverlapping();

					MappingManager mapping_extractor = 
							new CandidateMappingManager(index, 
									onto_process1, onto_process2);

					LogMapWrapper.addMappingsToMappingManager(repaired_mappings, 
							mapping_extractor, onto_process1, onto_process2);

					AnchorsAssessmentFullReasoning anchAssessFull = 
							new AnchorsAssessmentFullReasoning(
									ReasonerManager.HERMIT, 
									index, 
									mapping_extractor, 
									overlappingExtractor, 
									true);

					Set<OWLAxiom> repair = 
							anchAssessFull.classifyAndRepairUnsatisfiability();

					if(!repair.isEmpty()){
						Set<MappingObjectStr> repairFull = new HashSet<>();

						for (OWLAxiom ax : repair) {
							boolean equiv = false;
							OWLSubClassOfAxiom subAx = null;

							if(ax.getAxiomType().equals(AxiomType.SUBCLASS_OF))
								subAx = (OWLSubClassOfAxiom) ax;
							else if(ax.getAxiomType().equals(AxiomType.EQUIVALENT_CLASSES)){
								subAx = ((OWLEquivalentClassesAxiom)ax).asOWLSubClassOfAxioms().iterator().next();
								equiv = true;
							}
							else
								throw new Error("Invalid axiom type for full logmap repair: " + 
										ax.getAxiomType() + ", axiom: " + ax);

							Pair<OWLClass> cls = 
									OntoUtil.getNamedClassesFromSubClassAxiom(
											subAx, false);

							repairFull.add(new MappingObjectStr(
									cls.getFirst().getIRI().toString(), 
									cls.getSecond().getIRI().toString(), 1, 
									equiv ? Utilities.EQ : Utilities.L2R));
						}

						FileUtil.writeLogAndConsole("Full repair size: " + 
								countMappings(repairFull));

						if(countMappings(repairFull) == 6)
							System.out.println();

						//						Set<MappingObjectStr> addRep = new HashSet<>();
						//						
						//						for (MappingObjectStr m : repairFull) {
						//							if(m.getMappingDirection() == Utilities.EQ){
						//								addRep.add(new MappingObjectStr(m.getIRIStrEnt1(), 
						//										m.getIRIStrEnt2(), m.getConfidence(), 
						//										Utilities.L2R, m.getTypeOfMapping()));								
						//								
						//								addRep.add(new MappingObjectStr(m.getIRIStrEnt1(), 
						//										m.getIRIStrEnt2(), m.getConfidence(), 
						//										Utilities.R2L, m.getTypeOfMapping()));
						//							}
						//						}

						repaired_mappings.removeAll(repairFull);
						//						repaired_mappings.removeAll(addRep);
					}
				}
			}
		}
		catch(Exception e){
			FileUtil.writeErrorLogAndConsole(e.getMessage());
		}
		return repaired_mappings;
	}

	public static void addMappingsToMappingManager(
			Set<MappingObjectStr> input_mappings, 
			MappingManager mapping_extractor, 
			OntologyProcessing ontoProc1, 
			OntologyProcessing ontoProc2){

		for (MappingObjectStr mapping : input_mappings){

			int id1 = -1, id2 = -1;

			String iri1 = mapping.getIRIStrEnt1(), iri2 = mapping.getIRIStrEnt2();
			String iriFrag1 = IRI.create(iri1).getFragment(),
					iriFrag2 = IRI.create(iri2).getFragment();

			switch(mapping.getMappingDirection()){
			case Utilities.EQ:
				switch(mapping.getTypeOfMapping()){
				case Utilities.CLASSES:
					id1 = ontoProc1.getIdentifier4ConceptIRI(iri1);
					id2 = ontoProc2.getIdentifier4ConceptIRI(iri2);

					mapping_extractor.addSubMapping2ListOfAnchors(id1, id2);
					mapping_extractor.addSubMapping2ListOfAnchors(id2, id1);
					break;
				case Utilities.DATAPROPERTIES:
					id1 = ontoProc1.getIdentifier4DataPropName(iriFrag1);
					id2 = ontoProc2.getIdentifier4DataPropName(iriFrag2);

					mapping_extractor.addDataPropertyAnchor(id1, id2);
					mapping_extractor.addDataPropertyAnchor(id2, id1);
					break;
				case Utilities.OBJECTPROPERTIES:
					id1 = ontoProc1.getIdentifier4ObjectPropName(iriFrag1);
					id2 = ontoProc2.getIdentifier4ObjectPropName(iriFrag2);

					mapping_extractor.addObjectPropertyAnchor(id1, id2);
					mapping_extractor.addObjectPropertyAnchor(id2, id1);
					break;
				default:
					throw new Error("Impossible to add to mapping manager " +
							"a mapping of unknown type: " + 
							mapping.getTypeOfMapping());
				}
				break;
			case Utilities.L2R:
				switch(mapping.getTypeOfMapping()){
				case Utilities.CLASSES:
					id1 = ontoProc1.getIdentifier4ConceptIRI(iri1);
					id2 = ontoProc2.getIdentifier4ConceptIRI(iri2);

					mapping_extractor.addSubMapping2ListOfAnchors(id1, id2);
					break;
				case Utilities.DATAPROPERTIES:
					id1 = ontoProc1.getIdentifier4DataPropName(iriFrag1);
					id2 = ontoProc2.getIdentifier4DataPropName(iriFrag2);

					mapping_extractor.addDataPropertyAnchor(id1, id2);
					break;
				case Utilities.OBJECTPROPERTIES:
					id1 = ontoProc1.getIdentifier4ObjectPropName(iriFrag1);
					id2 = ontoProc2.getIdentifier4ObjectPropName(iriFrag2);

					mapping_extractor.addObjectPropertyAnchor(id1, id2);
					break;
				default:
					throw new Error("Impossible to add to mapping manager " +
							"a mapping of unknown type: " + 
							mapping.getTypeOfMapping());
				}
				break;
			case Utilities.R2L:
				switch(mapping.getTypeOfMapping()){
				case Utilities.CLASSES:
					id1 = ontoProc1.getIdentifier4ConceptIRI(iri1);
					id2 = ontoProc2.getIdentifier4ConceptIRI(iri2);

					mapping_extractor.addSubMapping2ListOfAnchors(id2, id1);
					break;
				case Utilities.DATAPROPERTIES:
					id1 = ontoProc1.getIdentifier4DataPropName(iriFrag1);
					id2 = ontoProc2.getIdentifier4DataPropName(iriFrag2);

					mapping_extractor.addDataPropertyAnchor(id2, id1);
					break;
				case Utilities.OBJECTPROPERTIES:
					id1 = ontoProc1.getIdentifier4ObjectPropName(iriFrag1);
					id2 = ontoProc2.getIdentifier4ObjectPropName(iriFrag2);

					mapping_extractor.addObjectPropertyAnchor(id2, id1);
					break;
				default:
					throw new Error("Impossible to add to mapping manager " +
							"a mapping of unknown type: " + 
							mapping.getTypeOfMapping());
				}
				break;
			default:
				throw new Error("Impossible to add to mapping manager " +
						"a mapping of unknown direction: " + 
						mapping.getMappingDirection());
			}
		}
	}

	public static int countMappings(Set<MappingObjectStr> mappings){
		int tot = 0;

		for (MappingObjectStr m : mappings)
			tot += countMappings(m);

		return tot;
	}

	public static int countMappings(MappingObjectStr m){
		switch(m.getMappingDirection()){
		case Utilities.EQ:
			if(m.getIRIStrEnt1().equals(m.getIRIStrEnt2()))
				return 1;
			else
				return 2;
		case Utilities.L2R:
		case Utilities.R2L:
		default:
			return 1;
		}
	}

	public static boolean compareViolationsIndexReasoner(
			List<Pair<Integer>> indexViol,
			List<Pair<OWLClass>> reasonerViol, 
			JointIndexManager index, OntologyProcessing ontoProc,
			int ontoId, JointIndexManager alignIndex, 
			OntologyProcessing alignProc, OWLReasoner reasoner, 
			OWLReasoner alignR, boolean directViol){

		//		if(reasonerViol == null){
		//			FileUtil.writeErrorLogAndConsole("List of violations computed by the " +
		//					"reasoner is empty, cannot compare");
		//			return true;
		//		}
		String out = "(Onto " + ontoId + "): index = " 
				+ indexViol.size() 
				+ ", reasoner = " + reasonerViol.size() + "\n" + 
				Util.getCurrTime();

		if(indexViol.size() != reasonerViol.size()){				
			FileUtil.writeErrorLogAndConsole(out);
			List<Pair<OWLClass>> indexViolClass = new ArrayList<>(indexViol.size());
			for (Pair<Integer> p : indexViol) {
				indexViolClass.add(LogMapWrapper.getOWLClassFromIndexPair(p,index));
				//				System.out.println(
				//						LogMapWrapper.getOWLClassFromIndexPair(p,index));
			}
			//			System.out.println("\n" + reasonerViol.toString().replace(">>, ", ">>,\n"));
			Set<Pair<OWLClass>> intersection = 
					Util.computeIntersection(indexViolClass, reasonerViol);

			reasonerViol.removeAll(intersection);
			indexViolClass.removeAll(intersection);

			FileUtil.writeLogAndConsole("Detected by index but not by reasoner: " 
					+ indexViolClass.size()); 
			//\n + indexViolClass.toString().replace(">>, ", ">>,\n"));

			for (Pair<OWLClass> pair : indexViolClass) {
				checkViolationAgainstIndexAndReasoner(pair,index, ontoProc, 
						alignIndex, alignProc, reasoner, alignR, directViol);
			}

			FileUtil.writeLogAndConsole("Detected by reasoner but not by index: " 
					+ reasonerViol.size()); 
			//\n + reasonerViol.toString().replace(">>, ", ">>,\n"));

			for (Pair<OWLClass> pair : reasonerViol) {
				checkViolationAgainstIndexAndReasoner(pair,index, ontoProc, 
						alignIndex, alignProc, reasoner, alignR, directViol);
			}
			return true;
		}
		else 
			FileUtil.writeLogAndConsole(out);
		return false;
	}

	private static void checkViolationAgainstIndexAndReasoner(
			Pair<OWLClass> pair, 
			JointIndexManager index, OntologyProcessing ontoProc,
			JointIndexManager alignIndex, OntologyProcessing alignProc, 
			OWLReasoner origR, OWLReasoner alignR, boolean directViol){

		boolean preIdx = index.isSubClassOf(
				ontoProc.getIdentifier4ConceptIRI(
						pair.getFirst().getIRI().toString()), 
						ontoProc.getIdentifier4ConceptIRI(
								pair.getSecond().getIRI().toString())); 
		boolean postIdx = alignIndex.isSubClassOf(
				alignProc.getIdentifier4ConceptIRI(
						pair.getFirst().getIRI().toString()), 
						alignProc.getIdentifier4ConceptIRI(
								pair.getSecond().getIRI().toString()));
		boolean preR = origR.getSubClasses(pair.getSecond(), 
				directViol).containsEntity(pair.getFirst()) || 
				origR.getEquivalentClasses(pair.getFirst()).contains(
						pair.getSecond());
		boolean postR = alignR.getSubClasses(pair.getSecond(), 
				directViol).containsEntity(pair.getFirst()) || 
				alignR.getEquivalentClasses(pair.getFirst()).contains(
						pair.getSecond());

		FileUtil.writeLogAndConsole(pair + ":");
		if(!preIdx && postIdx){
			FileUtil.writeLogAndConsole("violation for index");
		}
		else {//if(preIdx && postIdx) {
			FileUtil.writeLogAndConsole("NOT a violation for index");
			FileUtil.writeLogAndConsole("holds in original index? " + preIdx);
			FileUtil.writeLogAndConsole("holds in aligned index? " + postIdx);
		}

		if(!preR && postR){
			FileUtil.writeLogAndConsole("violation for reasoner");
		}
		else { //if(preR && postR) {
			FileUtil.writeLogAndConsole("NOT a violation for reasoner");
			FileUtil.writeLogAndConsole("holds in original onto? " + preR);
			FileUtil.writeLogAndConsole("holds in aligned onto? " + postR);
		}
	}

	public static Set<OWLClass> getInputSubClasses(OWLOntology orig, 
			OWLReasoner alignR, OWLClass c, boolean direct){
		Set<OWLClass> subC = new HashSet<>();
		Queue<OWLClass> q = new LinkedList<>();
		OWLClass t;
		Set<OWLClass> classSig = orig.getClassesInSignature();
		classSig.removeAll(alignR.getUnsatisfiableClasses().getEntities());

		boolean print = false;
		//		if(c.getIRI().toString().equals(
		//				"http://eu.optique.bootstrapping.ontology/postgres_slegge_statoil/composite_obj_x"))
		//			print = true;

		Set<OWLClass> visited = new HashSet<>();
		Set<OWLClass> equivTop = new HashSet<>(alignR.getEquivalentClasses(c).getEntities());

		Set<OWLClass> dangClasses = alignR.getUnsatisfiableClasses().getEntities();
		dangClasses.addAll(alignR.getTopClassNode().getEntities());

		q.addAll(equivTop);

		while(!q.isEmpty()){
			t = q.poll();

			if(dangClasses.contains(t) || t.isAnonymous() || t.isBottomEntity() || t.isTopEntity())
				continue;

			if(//!direct && 
					visited.contains(t)){
				//				System.out.println("skip: " + t);
				continue;
			}

			visited.add(t);

			if(print)
				FileUtil.writeLogAndConsole("Actual: " + t);

			// if the class has an equivalent class in the ontology we are seeking violations, 
			// all its subclasses are not direct violation (they are subclasses of 
			// the equivalent one)
			boolean blockSub = false;
			boolean isEquivTop = equivTop.contains(t);
			boolean sameOnto = classSig.contains(t);

			if(sameOnto)
				subC.add(t);				

			if(!isEquivTop){
				Set<OWLClass> tmpQueue = new HashSet<>();
				Set<OWLClass> eqClasses = alignR.getEquivalentClasses(t).getEntitiesMinus(t);
				for (OWLClass eqSubId : eqClasses) {
					if(print)
						FileUtil.writeLogAndConsole("Eq-actual: " + eqSubId.getIRI().toString());
					if(classSig.contains(eqSubId)){
						subC.add(eqSubId);
						//						blockSub = true;
					}
					else
						tmpQueue.add(eqSubId);
				}
				//				if(!blockSub)
				q.addAll(tmpQueue);
			}

			//			if(!direct)
			//				blockSub = false;

			if(!(direct && sameOnto)){
				Set<OWLClass> subClasses = alignR.getSubClasses(t, true).getFlattened();
				subClasses.remove(t);
				for (OWLClass subClass : subClasses) {
					if(print)
						FileUtil.writeLogAndConsole("Sub-actual: " + subClass.getIRI().toString());
					q.add(subClass);
				}
			}
		}
		subC.remove(c);
		subC.removeAll(alignR.getUnsatisfiableClasses().getEntities());
		return subC;
	}

	private static void addSubClassesToQueue(Queue<OWLClass> q, 
			OWLClass t, OWLClass d, OWLReasoner alignR, boolean print){

		Set<OWLClass> subClasses = alignR.getSubClasses(t, true).getFlattened();
		subClasses.remove(t);
		for (OWLClass subClass : subClasses) {
			if(print)
				FileUtil.writeLogAndConsole("Sub-actual: " + subClass.getIRI().toString());
			q.add(subClass);
		}
	}

	public static Set<OWLClass> getDirectInputSubClasses(OWLOntology orig, 
			OWLReasoner alignR, OWLClass c, boolean direct){

		if(!direct)
			getInputSubClasses(orig, alignR, c, direct);

		Set<OWLClass> subC = new HashSet<>();
		Queue<OWLClass> q = new LinkedList<>();
		OWLClass t;
		Set<OWLClass> classSig = orig.getClassesInSignature();
		classSig.removeAll(alignR.getUnsatisfiableClasses().getEntities());

		boolean print = false;
		//		if(c.getIRI().toString().equals(
		//				"http://eu.optique.bootstrapping.ontology/postgres_slegge_statoil/composite_obj_x"))
		//			print = true;

		Set<OWLClass> visited = new HashSet<>();
		Set<OWLClass> equivTop = new HashSet<>(alignR.getEquivalentClasses(c).getEntities());

		Set<OWLClass> dangClasses = alignR.getUnsatisfiableClasses().getEntities();
		dangClasses.addAll(alignR.getTopClassNode().getEntities());

		q.addAll(equivTop);

		while(!q.isEmpty()){
			t = q.poll();

			if(dangClasses.contains(t) || t.isAnonymous() || t.isBottomEntity() || t.isTopEntity())
				continue;

			//			if(//!direct && 
			//					visited.contains(t)){
			////				System.out.println("skip: " + t);
			//				continue;
			//			}

			visited.add(t);

			if(print)
				FileUtil.writeLogAndConsole("Actual: " + t);

			// if the class has an equivalent class in the ontology we are seeking violations, 
			// all its subclasses are not direct violation (they are subclasses of 
			// the equivalent one)
			boolean isEquivTop = equivTop.contains(t);
			boolean sameOnto = classSig.contains(t);

			if(sameOnto)
				subC.add(t);				

			if(!isEquivTop){
				Set<OWLClass> eqClasses = alignR.getEquivalentClasses(t).getEntitiesMinus(t);
				for (OWLClass eqSubId : eqClasses) {
					if(print)
						FileUtil.writeLogAndConsole("Eq-actual: " + eqSubId.getIRI().toString());

					if(classSig.contains(eqSubId))
						subC.add(eqSubId);
					else
						addSubClassesToQueue(q, t, eqSubId, alignR, print);
				}
			}

			if(!(direct && sameOnto) || isEquivTop)
				addSubClassesToQueue(q, t, t, alignR, print);
		}

		subC.remove(c);
		subC.removeAll(alignR.getUnsatisfiableClasses().getEntities());

		return subC;
	}

	public static List<Pair<OWLClass>> detectConservativityViolationUsingReasoner(
			OWLOntology orig, OWLOntology aligned, OWLReasoner origR, 
			OWLReasoner alignR, OWLOntologyManager manager, 
			boolean directViol, boolean equivAlso){

		//		if(!origR.isConsistent() || !alignR.isConsistent())
		//			return null;

		// the method relies on the assumption that the classification inference
		// has been materialized in the ontologies

		List<Pair<OWLClass>> viols = new ArrayList<>();

		if(!equivAlso){

			List<OWLClass> classSig = new ArrayList<>(orig.getClassesInSignature());
			classSig.remove(alignR.getUnsatisfiableClasses().getEntities());
			classSig.removeAll(alignR.getTopClassNode().getEntities());

			for (OWLClass c : classSig) {
				//			if(c.getIRI().toString().equals("http://human.owl#NCI_C33737"))
				//				System.out.print("");

				Set<OWLClass> origSubC = origR.getSubClasses(c, true).getFlattened();
				origSubC.addAll(origR.getEquivalentClasses(c).getEntities());
				origSubC.remove(c);

				origSubC.removeAll(origR.getUnsatisfiableClasses().getEntities());
				origSubC.remove(OntoUtil.getDataFactory().getOWLThing());						

				Set<OWLClass> origSubCAll = origR.getSubClasses(c, false).getFlattened();
				origSubCAll.remove(c);
				origSubCAll.removeAll(origR.getUnsatisfiableClasses().getEntities());

				Set<OWLClass> alignSubC = getDirectInputSubClasses(orig,alignR,c,directViol);
				alignSubC.remove(OntoUtil.getDataFactory().getOWLThing());

				alignSubC.removeAll(origSubCAll);
				alignSubC.remove(c);

				for (OWLClass b : alignSubC){
					Set<OWLClass> origSubBAll = origR.getSubClasses(b, false).getFlattened();
					origSubBAll.remove(b);
					origSubBAll.removeAll(origR.getUnsatisfiableClasses().getEntities());

					if(origSubBAll.contains(c) || 
							origSubCAll.contains(b) || 
							origR.getEquivalentClasses(c).contains(b) ||
							!Util.computeIntersection(origSubCAll, origSubBAll).isEmpty())
						continue;

					if(classSig.contains(b))
						viols.add(new Pair<OWLClass>(b,c));
				}
			}
		}
		else {

			List<OWLClass> classSig = new ArrayList<>(orig.getClassesInSignature());
			classSig.remove(alignR.getUnsatisfiableClasses().getEntities());
			classSig.removeAll(alignR.getTopClassNode().getEntities());

			for (OWLClass c : classSig) {

				//				if(c.getIRI().toString().equals("http://human.owl#NCI_C33737"))
				//					System.out.print("");

				Set<OWLClass> origSubC = origR.getSubClasses(c, 
						false).getFlattened();
				origSubC.addAll(origR.getEquivalentClasses(c).getEntities());

				origSubC.removeAll(origR.getUnsatisfiableClasses().getEntities());
				origSubC.remove(OntoUtil.getDataFactory().getOWLThing());

				Set<OWLClass> alignSubC = getDirectInputSubClasses(orig,alignR,c,directViol);
				//						rootViolations ? 
				//						getDirectInputSubClasses(orig,alignR,c) : 
				//							alignR.getSubClasses(c, rootViolations).getFlattened();
				alignSubC.addAll(alignR.getEquivalentClasses(c).getEntities());

				alignSubC.removeAll(alignR.getUnsatisfiableClasses().getEntities());
				alignSubC.remove(OntoUtil.getDataFactory().getOWLThing());

				alignSubC.removeAll(origSubC);

				for (OWLClass b : alignSubC)
					if(classSig.contains(b))
						viols.add(new Pair<OWLClass>(b,c));
			}
		}
		return viols;
	}

	public static List<Pair<Integer>> detectEquivConservativityViolationWithSemanticIndex(
			JointIndexManager origIdx, OntologyProcessing origProc, 
			JointIndexManager alignIdx, OntologyProcessing alignProc, 
			int ontoId, boolean suppressOutput){

		long start = Util.getMSec();

		Integer iO, jO;
		String iIRI, jIRI;
		ClassIndex clIdxIO, clIdxJO;
		Set<Pair<Integer>> allViols = new HashSet<>();

		for (Integer i : alignIdx.getClassIdentifierSet()) {

			if(alignIdx.isBottomClass(i))
				continue;

			iIRI = alignIdx.getIRIStr4ConceptIndex(i);

			iO = origProc.getIdentifier4ConceptIRI(iIRI);

			// wrong ontology
			if(iO == -1)
				continue;

			clIdxIO = origIdx.getClassIndex(iO);

			if(clIdxIO.getOntologyId() != ontoId)
				continue;

			//			if(iIRI.equals("http://human.owl#NCI_C33737"))
			//				System.out.println();

			Set<Integer> subEqAlign = alignIdx.getAllEquivalentClasses(
					i,origProc);

			for (Integer j : subEqAlign){

				jIRI = alignIdx.getIRIStr4ConceptIndex(j);

				if(jIRI == null)
					FileUtil.writeErrorLogAndConsole("IRI is null for index " + j);

				jO = origProc.getIdentifier4ConceptIRI(jIRI);

				if(jO == -1)
					continue;

				clIdxJO = origIdx.getClassIndex(jO);

				if(clIdxJO.getOntologyId() != ontoId)
					continue;

				if(!origIdx.areEquivalentClasses(jO, iO) && 
						!allViols.contains(new Pair<>(iO,jO)))
					allViols.add(new Pair<Integer>(jO,iO));
			}
		}

		//		if(allViols.size() > 0)
		FileUtil.writeLogAndConsole("ONTO " + ontoId + ": " 
				+ allViols.size() + " EQ-VIOLATION(S)");

		FileUtil.writeLogAndConsole("Index violation (EQ) detection (ms): " 
				+ Util.getDiffmsec(start));

		return new ArrayList<>(allViols);
	}

	public static int getCorrespondingIdentifier(OntologyProcessing trgProc, 
			JointIndexManager srcIdx,  int i){
		String iIRI = srcIdx.getIRIStr4ConceptIndex(i);

		return trgProc.getIdentifier4ConceptIRI(iIRI);
	}

	public static List<Pair<Integer>> detectConservativityViolationWithSemanticIndex(
			JointIndexManager origIdx, OntologyProcessing origProc, 
			JointIndexManager alignIdx, OntologyProcessing alignProc, 
			boolean directViol, boolean alsoEquiv, int ontoId, 
			boolean suppressOutput){

		long start = Util.getMSec();

		Integer iO, jO;
		String iIRI, jIRI;
		ClassIndex clIdxIO, clIdxJO;
		Set<Pair<Integer>> allViols = new HashSet<>();

		for (Integer i : alignIdx.getClassIdentifierSet()) {

			if(alignIdx.isBottomClass(i))
				continue;

			iIRI = alignIdx.getIRIStr4ConceptIndex(i);

			iO = origProc.getIdentifier4ConceptIRI(iIRI);

			// wrong ontology
			if(iO == -1)
				continue;

			clIdxIO = origIdx.getClassIndex(iO);

			if(clIdxIO.getOntologyId() != ontoId)
				continue;

			//			if(iIRI.equals("http://human.owl#NCI_C33737"))
			//				System.out.println();

			Set<Integer> subEqAlign = alignIdx.getSubEquivalentClasses(
					i,origProc,directViol);

			//			Set<Integer> subEqOrig = origIdx.getDirectSubEquivalentClasses(iO,null);

			for (Integer j : subEqAlign){

				jIRI = alignIdx.getIRIStr4ConceptIndex(j);

				if(jIRI == null)
					FileUtil.writeErrorLogAndConsole("IRI is null for index " + j);

				jO = origProc.getIdentifier4ConceptIRI(jIRI);

				if(jO == -1)
					continue;

				clIdxJO = origIdx.getClassIndex(jO);

				if(clIdxJO.getOntologyId() != ontoId 
						|| (!alsoEquiv && (
								origIdx.isSubClassOf(iO, jO) || 
								origIdx.isSubClassOf(jO, iO) || 
								origIdx.areConceptsSharingDescendants(iO, jO))))
					continue;

				if(!origIdx.isSubClassOf(jO, iO)){//!subEqOrig.contains(jO)){
					allViols.add(new Pair<Integer>(jO,iO));
					//					if(!suppressOutput)
					//						FileUtil.writeLogAndConsole(jIRI + " --isA-> " + iIRI);
				}
			}
		}

		//		if(!suppressOutput)
		//			for (Pair<Integer> pair : allViols)
		//				FileUtil.writeLogAndConsole(
		//						LogMapWrapper.getOWLClassFromIndexPair(pair, 
		//								origIdx).toString());
		//		if(allViols.size() > 0)
		FileUtil.writeLogAndConsole("ONTO " + ontoId + ": " 
				+ allViols.size() + " VIOLATION(S)");

		FileUtil.writeLogAndConsole("Index violation detection (ms): " 
				+ Util.getDiffmsec(start));
		return new ArrayList<>(allViols);
	}

	public static Pair<String> getIRIStrFromIndexPair(Pair<Integer> p, 
			JointIndexManager idx){
		return new Pair<>(
				idx.getIRIStr4ConceptIndex(p.getFirst()),
				idx.getIRIStr4ConceptIndex(p.getSecond())
				);
	}

	public static List<Pair<OWLClass>> getOWLClassFromIndexPair(
			List<Pair<Integer>> l, JointIndexManager idx){
		List<Pair<OWLClass>> lc = new ArrayList<>(l.size());
		for (Pair<Integer> p : l)
			lc.add(getOWLClassFromIndexPair(p, idx));
		return lc;
	}

	public static Pair<OWLClass> getOWLClassFromIndexPair(Pair<Integer> p, 
			JointIndexManager idx){
		return new Pair<>(
				getOWLClassFromIndex(p.getFirst(), idx),
				getOWLClassFromIndex(p.getSecond(), idx)
				);
	}

	public static OWLClass getOWLClassFromIndex(Integer i, JointIndexManager idx){
		return idx.getOWLClass4ConceptIndex(i);
	}

	public static Pair<List<Pair<Integer>>> parallelConservativityViolationDetection(
			JointIndexManager origIdx, OntologyProcessing origProc1, 
			OntologyProcessing origProc2,
			JointIndexManager alignIdx1, OntologyProcessing alignProc1,
			JointIndexManager alignIdx2, OntologyProcessing alignProc2,
			boolean rootViolations, boolean alsoEquiv, boolean suppressOutput){

		ExecutorService	executor = Executors.newFixedThreadPool(2);

		List<Callable<List<Pair<Integer>>>> tasks = new ArrayList<>(2);
		List<Future<List<Pair<Integer>>>> futures;
		List<List<Pair<Integer>>> results = new ArrayList<>(2);

		tasks.add(new ConservativityViolationDetectionThread(
				new JointIndexManager(origIdx),
				origProc1,
				alignIdx1,
				alignProc1,
				0,rootViolations,alsoEquiv,suppressOutput,
				null,null,null));

		tasks.add(new ConservativityViolationDetectionThread(
				new JointIndexManager(origIdx),
				origProc2,
				alignIdx2,
				alignProc2,
				1,rootViolations,alsoEquiv,suppressOutput,
				null,null,null));

		try {
			futures = executor.invokeAll(tasks);
			executor.shutdown();

			for (Future<List<Pair<Integer>>> f : futures)
				results.add(f.get());
		}
		catch (Throwable e) {
			FileUtil.writeErrorLogAndConsole(e.getMessage());
		}

		int unsolvViol = 0;

		if(suppressOutput){
			//			FileUtil.writeLogAndConsole("ONTO 0 + mappings: ");
			//			unsolvViol = results.get(0).size();
			//			FileUtil.writeLogAndConsole(unsolvViol + " violations");
		}
		else {
			FileUtil.writeLogAndConsole("ONTO 0 + mappings: ");
			for (Pair<Integer> p : results.get(0)) {
				unsolvViol++;

				Pair<OWLClass> pCls = 
						LogMapWrapper.getOWLClassFromIndexPair(p, origIdx);
				OWLAxiom subAx = OntoUtil.getDataFactory().getOWLSubClassOfAxiom(
						pCls.getFirst(), pCls.getSecond());
				FileUtil.writeLogAndConsole(subAx.toString());
			}
			FileUtil.writeLogAndConsole(unsolvViol + " violations");
		}

		unsolvViol = 0;

		if(suppressOutput){
			//			FileUtil.writeLogAndConsoleNONL("ONTO 1 + mappings: ");
			//			unsolvViol = results.get(1).size();
			//			FileUtil.writeLogAndConsole(unsolvViol + " violations");
		}
		else {
			FileUtil.writeLogAndConsole("ONTO 1 + mappings: ");
			for (Pair<Integer> p : results.get(1)){
				unsolvViol++;
				Pair<OWLClass> pCls = 
						LogMapWrapper.getOWLClassFromIndexPair(p, origIdx);
				OWLAxiom subAx = 
						OntoUtil.getDataFactory().getOWLSubClassOfAxiom(
								pCls.getFirst(), pCls.getSecond());
				FileUtil.writeLogAndConsole(subAx.toString());
			}
			FileUtil.writeLogAndConsole(unsolvViol + " violations");
		}

		return new Pair<List<Pair<Integer>>>(results.get(0),results.get(1));
	}

	public static Pair<List<Pair<Integer>>> parallelEqConservativityViolationDetection(
			JointIndexManager origIdx, OntologyProcessing origProc1, 
			OntologyProcessing origProc2, JointIndexManager alignIdx, 
			OntologyProcessing alignProc, boolean suppressOutput){

		ExecutorService	executor = Executors.newFixedThreadPool(2);

		List<Callable<List<Pair<Integer>>>> tasks = new ArrayList<>(2);
		List<Future<List<Pair<Integer>>>> futures;
		List<List<Pair<Integer>>> results = new ArrayList<>(2);

		tasks.add(new EqOnlyConservativityViolationDetectionThread(
				new JointIndexManager(origIdx),
				//origIdx,
				origProc1,
				new JointIndexManager(alignIdx),
				//alignIdx,
				new OntologyProcessing(alignProc),
				//alignProc,
				0,suppressOutput
				));

		tasks.add(new EqOnlyConservativityViolationDetectionThread(
				new JointIndexManager(origIdx),
				//origIdx,
				origProc2,
				new JointIndexManager(alignIdx),
				//alignIdx,
				new OntologyProcessing(alignProc),
				//alignProc,
				1,suppressOutput
				));

		try {
			futures = executor.invokeAll(tasks);
			executor.shutdown();

			for (Future<List<Pair<Integer>>> f : futures)
				results.add(f.get());
		}
		catch (Throwable e) {
			FileUtil.writeErrorLogAndConsole(e.getMessage());
		}

		int unsolvViol = 0;

		if(!suppressOutput){
			FileUtil.writeLogAndConsole("ONTO 1: ");
			for (Pair<Integer> p : results.get(0)) {
				unsolvViol++;

				Pair<OWLClass> pCls = 
						LogMapWrapper.getOWLClassFromIndexPair(p, origIdx);
				OWLAxiom subAx = OntoUtil.getDataFactory().getOWLSubClassOfAxiom(
						pCls.getFirst(), pCls.getSecond());
				FileUtil.writeLogAndConsole(subAx.toString());
			}
			FileUtil.writeLogAndConsole(unsolvViol + " violations");
		}

		unsolvViol = 0;

		if(!suppressOutput){
			FileUtil.writeLogAndConsole("ONTO 2: ");
			for (Pair<Integer> p : results.get(1)){
				unsolvViol++;
				Pair<OWLClass> pCls = 
						LogMapWrapper.getOWLClassFromIndexPair(p, origIdx);
				OWLAxiom subAx = 
						OntoUtil.getDataFactory().getOWLSubClassOfAxiom(
								pCls.getFirst(), pCls.getSecond());
				FileUtil.writeLogAndConsole(subAx.toString());
			}
			FileUtil.writeLogAndConsole(unsolvViol + " violations");
		}

		return new Pair<List<Pair<Integer>>>(results.get(0),results.get(1));
	}

	public static Pair<Integer> getPairOfIdentifiersFromPairOfClasses(
			Pair<OWLClass> pair, OntologyProcessing alignProc){
		return new Pair<Integer>(
				alignProc.getClass2Identifier().get(pair.getFirst()),
				alignProc.getClass2Identifier().get(pair.getSecond()));
	}

	public static List<Pair<Integer>> getPairsOfIdentifiersFromPairsOfClasses(
			List<Pair<OWLClass>> classes, OntologyProcessing alignProc){

		List<Pair<Integer>> res = new ArrayList<>(classes.size());
		for (Pair<OWLClass> pair : classes)
			res.add(getPairOfIdentifiersFromPairOfClasses(pair, alignProc));

		return res;
	}

	public static Pair<List<Pair<Integer>>> modularGraphDetectionDirectViolations(
			OWLOntology fstOnto, OWLOntology sndOnto, OWLOntology alignOnto, 
			Pair<List<Pair<Integer>>> viols, JointIndexManager origIdx, 
			JointIndexManager alignIdx, OntologyProcessing alignProc){

		/**
		 * Proposition 1: O_alpha = O_A (its bottom-locality based module) if alpha = A \subseteq B
		 * Corolllary 1: A \subseteq B implies B \in sig(O_A)
		 * Proposition 2: if B \in sig(O_A) then O_B \subseteq O_A 
		 */

		boolean old = false;

		AbstractModularDirectViolationDetector detector;

		long start = Util.getMSec();

		if(old)
			detector = new BasicModularViolationDetector(
					fstOnto, sndOnto, alignOnto, viols, origIdx, alignIdx, alignProc);
		else 
			//			detector = new OptimisedModularViolationDetector(
			//					fstOnto, sndOnto, alignOnto, viols, origIdx, alignIdx, alignProc);
			//			detector = new OptimisedAlignModularViolationDetector(
			//				fstOnto, sndOnto, alignOnto, viols, origIdx, alignIdx, alignProc);

			detector = new BiModuleViolationDetector(fstOnto, sndOnto, 
					alignOnto, viols, origIdx, alignIdx, alignProc);

		Pair<List<Pair<Integer>>> res = detector.detectDirectViolations();

		FileUtil.writeLogAndConsole(
				"Modular graph direct violation detection: " + 
						Util.getDiffmsec(start) + " (ms)");

		return res;
	}

	public static void convertIndex(List<Pair<Integer>> pairs, 
			OntologyProcessing trgProc, JointIndexManager srcIdx){
		for (Pair<Integer> pair : pairs)
			convertIndex(pair, trgProc, srcIdx);
	}

	public static void convertIndex(Pair<Integer> p, OntologyProcessing trgProc, 
			JointIndexManager srcIdx){
		p.setFirst(LogMapWrapper.getCorrespondingIdentifier(
				trgProc, srcIdx, p.getFirst()));
		p.setSecond(LogMapWrapper.getCorrespondingIdentifier(
				trgProc, srcIdx, p.getFirst()));
	}

	public static Pair<List<Pair<Integer>>> parallelDirectConservativityViolationDetection(
			JointIndexManager origIdx, OntologyProcessing origProc1, 
			OntologyProcessing origProc2, JointIndexManager alignIdx, 
			OntologyProcessing alignProc, boolean alsoEquiv, boolean suppressOutput,
			OWLOntology inputOnto1, OWLOntology inputOnto2, 
			OWLOntology alignOnto, Pair<List<Pair<Integer>>> pair){

		boolean modular = false;

		FileUtil.writeLogAndConsole("b) Detecting direct violations using index");			
		Pair<List<Pair<Integer>>> dirViolations = 
				parallelConservativityViolationDetection(origIdx,origProc1,
						origProc2,alignIdx,alignProc,true,alsoEquiv,
						suppressOutput);

		FileUtil.writeLogAndConsole("c) Detecting direct violations using graph");
		long start = Util.getMSec();

		Set<Pair<Integer>> unkViolSet1 = new HashSet<>(pair.getFirst());
		pair.getFirst().clear();
		unkViolSet1.removeAll(dirViolations.getFirst());

		List<Pair<Integer>> unkViol1 = new ArrayList<>(unkViolSet1);
		unkViolSet1.clear();

		Set<Pair<Integer>> unkViolSet2 = new HashSet<>(pair.getSecond());
		pair.getSecond().clear();
		unkViolSet2.removeAll(dirViolations.getSecond());

		List<Pair<Integer>> unkViol2 = new ArrayList<>(unkViolSet2);
		unkViolSet2.clear();			

		// MODULAR (START)
		if(modular){
			Pair<List<Pair<Integer>>> otherDirViolPairID =
					modularGraphDetectionDirectViolations(
							inputOnto1, inputOnto2, alignOnto, 
							new Pair<>(unkViol1,unkViol2), 
							origIdx, alignIdx, alignProc);
			dirViolations.getFirst().addAll(otherDirViolPairID.getFirst());
			dirViolations.getSecond().addAll(otherDirViolPairID.getSecond());
			// MODULAR (END)
		}
		// WHOLE GRAPH (START)
		else {
			Pair<List<Pair<OWLClass>>> unkViolPair = new Pair<>(
					LogMapWrapper.getOWLClassFromIndexPair(unkViol1, origIdx),
					LogMapWrapper.getOWLClassFromIndexPair(unkViol2, origIdx)
					);

			Pair<List<Pair<OWLClass>>> otherDirViolPair = 
					OntoUtil.graphDetectionDirectViolations(inputOnto1, 
							inputOnto2, alignOnto, unkViolPair);
			dirViolations.getFirst().addAll(
					LogMapWrapper.getPairsOfIdentifiersFromPairsOfClasses(
							otherDirViolPair.getFirst(), origProc1));

			dirViolations.getSecond().addAll(
					LogMapWrapper.getPairsOfIdentifiersFromPairsOfClasses(
							otherDirViolPair.getSecond(), origProc2));
		}
		// WHOLE GRAPH (END)

		FileUtil.writeLogAndConsole("Total time (c): " + 
				Util.getDiffmsec(start) + " (ms)");

		FileUtil.writeLogAndConsole("d) Combining both results");
		pair = dirViolations;
		FileUtil.writeLogAndConsole("ONTO 0: " 
				+ pair.getFirst().size() + " violations (DIRECT)");
		FileUtil.writeLogAndConsole("ONTO 1: " 
				+ pair.getSecond().size() + " violations (DIRECT)");

		return pair;
	}

	public static Pair<List<Pair<Integer>>> parallelConservativityViolationDetection(
			JointIndexManager origIdx, OntologyProcessing origProc1, 
			OntologyProcessing origProc2, JointIndexManager alignIdx, 
			OntologyProcessing alignProc, boolean rootViolations, 
			boolean alsoEquiv, boolean suppressOutput,
			OWLOntology inputOnto1, OWLOntology inputOnto2, 
			OWLOntology alignOnto){

		long startTotal = Util.getMSec();

		if(!rootViolations)
			FileUtil.writeLogAndConsole("a) Detecting all violations using index");

		Pair<List<Pair<Integer>>> violations = 
				parallelConservativityViolationDetection(origIdx,origProc1,
						origProc2,alignIdx,alignProc,false,alsoEquiv,
						suppressOutput);

		if(rootViolations)
			parallelDirectConservativityViolationDetection(
					origIdx, origProc1, origProc2, alignIdx, alignProc, 
					alsoEquiv, suppressOutput, inputOnto1, inputOnto2, 
					alignOnto, new Pair<List<Pair<Integer>>>(
							new ArrayList<>(violations.getFirst()), 
							new ArrayList<>(violations.getSecond())));

		FileUtil.writeLogAndConsole("Total detection time: " + 
				Util.getDiffmsec(startTotal) + " (ms)");

		return violations;
	}

	private static Pair<List<Pair<Integer>>> parallelConservativityViolationDetection(
			JointIndexManager origIdx, OntologyProcessing origProc1, 
			OntologyProcessing origProc2, JointIndexManager alignIdx, 
			OntologyProcessing alignProc, boolean rootViolations, 
			boolean alsoEquiv, boolean suppressOutput
			){
		return parallelConservativityViolationDetection(origIdx, origProc1, 
				origProc2, alignIdx, alignProc, rootViolations, 
				alsoEquiv, suppressOutput, null, null, null, null);
	}

	private static Pair<List<Pair<Integer>>> parallelConservativityViolationDetection(
			JointIndexManager origIdx, OntologyProcessing origProc1, 
			OntologyProcessing origProc2, JointIndexManager alignIdx, 
			OntologyProcessing alignProc, boolean rootViolations, 
			boolean alsoEquiv, boolean suppressOutput,
			OWLOntology inputOnto1, OWLOntology inputOnto2, 
			OWLOntology alignOnto, OWLReasoner alignR 
			){

		ExecutorService	executor = Executors.newFixedThreadPool(2);

		List<Callable<List<Pair<Integer>>>> tasks = new ArrayList<>(2);
		List<Future<List<Pair<Integer>>>> futures;
		List<List<Pair<Integer>>> results = new ArrayList<>(2);

		tasks.add(new ConservativityViolationDetectionThread(
				new JointIndexManager(origIdx),
				//origIdx,
				origProc1,
				new JointIndexManager(alignIdx),
				//alignIdx,
				new OntologyProcessing(alignProc),
				//alignProc,
				0,rootViolations,alsoEquiv,suppressOutput,
				inputOnto1,alignOnto,alignR));

		tasks.add(new ConservativityViolationDetectionThread(
				new JointIndexManager(origIdx),
				//origIdx,
				origProc2,
				new JointIndexManager(alignIdx),
				//alignIdx,
				new OntologyProcessing(alignProc),
				//alignProc,
				1,rootViolations,alsoEquiv,suppressOutput,
				inputOnto2,alignOnto,alignR));

		try {
			futures = executor.invokeAll(tasks);
			executor.shutdown();

			for (Future<List<Pair<Integer>>> f : futures)
				results.add(f.get());
		}
		catch (Throwable e) {
			//(InterruptedException | ExecutionException e) {
			FileUtil.writeErrorLogAndConsole(e.getMessage());
		}

		int unsolvViol = 0;


		if(suppressOutput){
			//			FileUtil.writeLogAndConsoleNONL("ONTO 0: ");
			//			unsolvViol = results.get(0).size();
			//			FileUtil.writeLogAndConsole(unsolvViol + " violations");
		}
		else {
			FileUtil.writeLogAndConsole("ONTO 0: ");
			for (Pair<Integer> p : results.get(0)) {
				unsolvViol++;

				Pair<OWLClass> pCls = 
						LogMapWrapper.getOWLClassFromIndexPair(p, origIdx);
				OWLAxiom subAx = OntoUtil.getDataFactory().getOWLSubClassOfAxiom(
						pCls.getFirst(), pCls.getSecond());
				FileUtil.writeLogAndConsole(subAx.toString());
			}
			FileUtil.writeLogAndConsole(unsolvViol + " violations");
		}
		unsolvViol = 0;

		if(suppressOutput){
			//			FileUtil.writeLogAndConsoleNONL("ONTO 1: ");
			//			unsolvViol = results.get(1).size();
			//			FileUtil.writeLogAndConsole(unsolvViol + " violations");
		}
		else {
			FileUtil.writeLogAndConsole("ONTO 1: ");
			for (Pair<Integer> p : results.get(1)){
				unsolvViol++;
				Pair<OWLClass> pCls = 
						LogMapWrapper.getOWLClassFromIndexPair(p, origIdx);
				OWLAxiom subAx = 
						OntoUtil.getDataFactory().getOWLSubClassOfAxiom(
								pCls.getFirst(), pCls.getSecond());
				FileUtil.writeLogAndConsole(subAx.toString());
			}
			FileUtil.writeLogAndConsole(unsolvViol + " violations");
		}

		return new Pair<List<Pair<Integer>>>(results.get(0),results.get(1));
	}

	public static Pair<Integer> parallelDisjointnessEnforcement(
			JointIndexManager index, boolean avoidLeafSiblings){

		JointIndexManager copyIdx = new JointIndexManager(index);

		ExecutorService	executor = Executors.newFixedThreadPool(2);

		List<Callable<String>> tasks = new ArrayList<>(2);
		//List<String> results = new ArrayList<>(2);		
		List<Future<String>> futures; 

		DisjointnessEnforcementThread 
		r1 = new DisjointnessEnforcementThread(index, 0, avoidLeafSiblings),
		r2 = new DisjointnessEnforcementThread(copyIdx, 1, avoidLeafSiblings);

		tasks.add(r1);
		tasks.add(r2);

		try {
			futures = executor.invokeAll(tasks);
			executor.shutdown();

			for (Future<String> f : futures)
				FileUtil.writeLogAndConsole(f.get());
		} catch(Throwable e){
			//catch (InterruptedException | ExecutionException e) {
			FileUtil.writeErrorLogAndConsole(e.getMessage());
		}

		index.mergeDisjointnessAxioms(copyIdx, 1);

		return new Pair<Integer>(r1.getDisjNum(),r2.getDisjNum());
	}

	public static int enforceDisjointnessIntoIndex(JointIndexManager index,
			int ontoId, boolean avoidLeafSiblings) {

		//long startEnforce = Util.getMSec(), enforceTime;

		String ontoPref = "Onto " + ontoId + ": ";

		Set<Integer> ids = index.getClassIdentifierSet();
		ArrayList<Integer> idList = new ArrayList<>(ids.size());
		Set<Integer> visited = new HashSet<>(index.getRootIdentifiers());
		Queue<Integer> queue = new LinkedList<>();

		for (Integer i : visited)
			if(index.getClassIndex(i).getOntologyId() == ontoId)
				queue.add(i);

		Integer idAux;
		while(!queue.isEmpty()){
			idAux = queue.poll();
			idList.add(idAux);
			for (Integer ch : index.getDirectSubClasses4Identifier(idAux, false)) {
				if(!visited.contains(ch) && 
						index.getClassIndex(ch).getOntologyId() == ontoId){
					queue.add(ch);
					visited.add(ch);
				}
			}
		}

		int tot, addedAxs = 0;
		FileUtil.writeLogAndConsoleNONL(ontoPref + "(" + idList.size() + "^2 - " + idList.size() + ") / 2 = ");
		FileUtil.writeLogAndConsole((tot = ( ((int)(((int) Math.pow(idList.size(), 
				2))-idList.size())/2))) + "" );
		ids = null;

		//FileUtil.writeDataOutFile(tot + " ");

		Map<Integer,Set<Integer>> bannedMap = new HashMap<>();
		Set<Integer> banned;

		FileUtil.writeLogAndConsole(Params.disjCheckStrategy + "");
		if(!index.isBasicSatisfiable())
			throw new Error(ontoPref + "Impossible to fix an already unsat index");
		else
			FileUtil.writeLogAndConsole(ontoPref + "Initially sat index");

		int disjBufferSize = idList.size()*2;
		int [] disjBuffer1 = new int[disjBufferSize];
		int [] disjBuffer2 = new int[disjBufferSize];

		int c, d = 0, bufferId = 0;
		OWLClass c1,c2;

		//		index.countingDisj(true);
		//		Params.verbosity = 1;

		int perc, prePerc = -1, outerLoops = 0, step = 0;
		for (Integer i1 : idList) {
			++outerLoops;
			c1 = index.getOWLClass4ConceptIndex(i1);
			if(c1.isTopEntity() || c1.isBottomEntity()){
				++d;
				continue;
			}
			banned = bannedMap.get(i1);
			for (c = outerLoops; c < idList.size(); step++,c++) {
				int i2 = idList.get(c);

				if(((perc = (int) Util.getPercentage(step, tot)) % 5 == 0) 
						&& prePerc != perc){
					prePerc = perc;
					FileUtil.writeLogAndConsole(ontoPref + perc + "% " + Util.getCurrTime());
				}

				if(i1==i2){
					++d;
					continue;
				}

				if(banned != null && banned.contains(i2)){
					++d;
					continue;
				}

				c2 = index.getOWLClass4ConceptIndex(i2);
				if(c2.isTopEntity() || c2.isBottomEntity()){
					++d;
					continue;
				}

				//System.out.println(c1 + " " + c2);

				if(cannotBeNewDisjointness(index,i1,i2,avoidLeafSiblings)){
					++d;
					continue;
				}

				++bufferId;
				//System.out.println(c1 + " " + c2);
				//System.out.println(bufferId);

				if(Params.disjCheckStrategy.equals(DISJ_CHECK.SEMINDEX)){						

					// update the index with the new disjointness
					index.addDisjointness(i1, i2, false);						

					disjBuffer1[bufferId-1] = i1;
					disjBuffer2[bufferId-1] = i2;

					//					int numDisj = index.getAddedDisj();
					//					System.out.println("#Disj = " + numDisj 
					//							+ ", BUFFER = " + (bufferId-1) + "/" 
					//							+ disjBuffer1.length);

					//					System.out.println("SAT ? " + index.isBasicSatisfiable());

					if(//index.isBasicSatisfiable() && 
							bufferId < disjBufferSize && 
							c < (idList.size()-1) && 
							outerLoops < (idList.size()-1))
						continue;

					//System.out.println(bufferId + " over " + disjBufferSize);

					//System.out.println("CHECK (pre unsat = " + index.isBasicSatisfiable() + ")");
					index.recreateDisjointIntervalsStructure();
					//					System.out.println("(" + c1 + ", " + c2 + ")");
					//					System.out.println("DISJ? " + index.areDisjoint(i1, i2));
					//					System.out.println("SIB? " + index.areSiblings(i1, i2));

					if(Params.testMode){
						for (int i = 0; i < bufferId; i++) {
							int id1 = disjBuffer1[i], id2 = disjBuffer2[i];
							if(!index.areDisjoint(id1, id2)) {
								FileUtil.writeErrorLogAndConsole(ontoPref + "1 NOT UPDATED " + id1 + " and " + id2);
								//						index.addDisjointness(id1, id2, true);
								//						index.addDisjointness(id2, id1, true);						
							}
							if(!index.areDisjoint(id2, id1)){
								FileUtil.writeErrorLogAndConsole(ontoPref + "2 NOT UPDATED " + id2 + " and " + id1);
								//						index.addDisjointness(id2, id1, true);
								//						index.addDisjointness(id1, id2, true);
							}
						}
					}

					//					numDisj = index.getAddedDisj();
					//					System.out.println("#Disj = " + numDisj 
					//							+ ", BUFFER = " + (bufferId-1) + "/" 
					//							+ disjBuffer1.length);

					if(!index.isBasicSatisfiable()){
						int retracted = 0;
						int realBufferLen = Math.min(bufferId,
								disjBuffer1.length) - 1;
						long startFix = Util.getMSec();

						if(!Params.repairStrategy.equals(
								REPAIR_STRATEGY.CONSIST_THEN_CONSERV)){

							retracted = fixUnsafeDisjDowlingGallier(index,
									realBufferLen,disjBuffer1,disjBuffer2,
									bannedMap,idList);
							addedAxs += bufferId - retracted;
							index.recreateDisjointIntervalsStructure();
							if(!index.isBasicSatisfiable())
								throw new Error(ontoPref + "UNSAT AFTER FIX!");
						}
						else {
							FileUtil.writeLogAndConsole(ontoPref + "new class unsatisfiabilities " +
									"detected, removing responsible disj axioms");
							if(Params.verbosity > 0)
								FileUtil.writeLogAndConsole(ontoPref + "Buffer size = " + realBufferLen);

							Set<Integer> retrIds = new HashSet<Integer>();

							retracted = fixUnsafeDisjIndex(index,bufferId,
									disjBuffer1,disjBuffer2,bannedMap, 0, 
									realBufferLen, retrIds);

							//							System.out.println("#Disj (pre) = " + numDisj);
							//							System.out.println("#Disj (post) = " + index.getAddedDisj());
							if(Params.verbosity > 0)
								FileUtil.writeLogAndConsole(ontoPref + retrIds.size() 
										+ " vs " + retracted);

							index.recreateDisjointIntervalsStructure();
							//							for (Integer i : retrIds) {
							//								if(index.areDisjoint(disjBuffer1[i], 
							//										disjBuffer2[i]))
							//									System.err.println("(" + disjBuffer1[i] 
							//											+ "," + disjBuffer2[i] 
							//													+ ") still in the index!!");
							//							}
							if(!index.isBasicSatisfiable())
								throw new Error(ontoPref + "UNSAT AFTER FIX!");
						}
						FileUtil.writeLogAndConsole(ontoPref + "retracted disjointness " +
								"after check = " + retracted + "/" + bufferId);
						FileUtil.writeLogAndConsole(ontoPref + "fix time = " 
								+ Util.getDiffmsec(startFix));
						addedAxs += bufferId - retracted;
					}
					else {
						addedAxs += bufferId;
						//System.out.println("ADDED AXIOMS: " + addedAxs);

						// avoid redundant disjointness axioms
						for (int i = 0; i < bufferId; i++) {
							banDescendants(index, bannedMap, disjBuffer1[i], 
									disjBuffer2[i]);
							banDescendants(index, bannedMap, disjBuffer2[i], 
									disjBuffer1[i]);
						}

						if(addedAxs % (5 * disjBufferSize) == 0)
							FileUtil.writeLogAndConsole(ontoPref + "AX = " + addedAxs 
									+ ", TESTS = " + step + "/" + tot 
									+ ", OUTERLOOP = " + outerLoops);
					}
					bufferId = 0;
				}
				else {
					// avoid redundant disjointness axioms
					banDescendants(index, bannedMap, i1, i2);
					banDescendants(index, bannedMap, i2, i1);
				}
			}
			//bannedMap.remove(i1);
		}

		FileUtil.writeLogAndConsole("BUFFER LAST: " + bufferId);
		if(bufferId > 0){
			index.recreateDisjointIntervalsStructure();
			if(!index.isBasicSatisfiable()){
				Set<Integer> retrIds = new HashSet<Integer>();
				int retracted = fixUnsafeDisjIndex(index,bufferId,
						disjBuffer1,disjBuffer2,bannedMap, 0, 
						bufferId-1, retrIds);
				if(Params.verbosity > 0)
					FileUtil.writeLogAndConsole(ontoPref + retrIds.size() + " vs " + retracted);
			}
		}
		else 
			index.recreateDisjointIntervalsStructure();

		bannedMap.clear();
		FileUtil.writeLogAndConsole(ontoPref + addedAxs + " disjointness axioms added");
		FileUtil.writeLogAndConsole(ontoPref + "saved " + d + " (" + (((float)d)/tot*100) 
				+ "%) disjointness tests ");

		if(!index.isBasicSatisfiable())
			throw new Error(ontoPref + "UNSAT AFTER FIX!");

		//FileUtil.writeDataOutFile(d + " " + addedAxs + " ");

		//enforceTime = Util.getDiffmsec(startEnforce);
		//FileUtil.writeDataOutFile(1 + " " + enforceTime + " ");

		return addedAxs;
	}

	public static boolean cannotBeNewDisjointness(JointIndexManager index, 
			int i1, int i2, boolean avoidLeafSiblings){
		return index.isSubClassOf(i1, i2) 
				|| index.isSubClassOf(i2, i1)
				//				|| index.isSuperClassOf(i1, i2) 
				//				|| index.isSuperClassOf(i2, i1)
				|| index.areConceptsSharingDescendants(i1, i2) 
				|| (avoidLeafSiblings && 
						index.areSiblings(i1, i2) && 
						index.getNumOfSubClasses4identifier(i1) == 0 && 
						index.getNumOfSubClasses4identifier(i2) == 0)
						|| index.areDisjoint(i1, i2);
	}

	public static int fixUnsafeDisjIndex(JointIndexManager index, 
			int bufferSize, int [] disjBuffer1, int [] disjBuffer2, 
			Map<Integer, Set<Integer>> bannedMap, int start, int end, 
			Set<Integer> retracted){

		//		if(!index.isBasicSatisfiable())
		//			Params.verbosity = 1;

		if(Params.verbosity > 0)
			FileUtil.writeLogAndConsoleNONL("Testing [" + start + ", " + end + "]... ");

		// safe interval in [0,start), 
		// unknown in [start, end], 
		// problematic in (end, bufferId]

		// flaws that should never happen...
		if(start < 0){
			FileUtil.writeErrorLogAndConsole("Received 'start' = " + start);
			start = 0;
		}
		if(end > bufferSize){
			FileUtil.writeErrorLogAndConsole("Received 'end' = " + end);
			end = bufferSize;
		}
		if(start > end)
			return 0;

		//		index.recreateDisjointIntervalsStructure();
		//		for (int i = 0; i <= end; i++)
		//			if(!index.areDisjoint(disjBuffer1[i], disjBuffer2[i])){
		//				if(i >= start)
		//					System.err.println(i + " not disjoint");
		//				else
		//					System.out.println(i + " not disjoint");				
		//			}

		// in order to be able to analyze interval [start,end], we clean (end,bufferSize]		
		for (int i = end+1; i < bufferSize; i++)
			index.retractDisjointness(disjBuffer1[i], disjBuffer2[i], false);

		// only one element, we retract it
		if(start == end){
			int i1 = disjBuffer1[start], 
					i2 = disjBuffer2[start];
			if((bannedMap.containsKey(i1) 
					&& bannedMap.get(i1).contains(i2))
					||
					(bannedMap.containsKey(i2) 
							&& bannedMap.get(i2).contains(i1))
					){
				//retracted.add(start);
				index.retractDisjointness(i1, i2, false);
				banAncestors(index,bannedMap,i1,i2);
				banAncestors(index,bannedMap,i2,i1);
				if(Params.verbosity > 0)
					FileUtil.writeLogAndConsole("banned, we retract (" 
							+ i1 + ", " + i2 + ")");
				return 1;
			}
			else {
				// avoided if we can use the bannedMap to answer 
				index.recreateDisjointIntervalsStructure();

				// this may happen if an interval of length two is unsat
				// but only due to one of the two disj axioms  
				if(index.areDisjoint(i1, i2) 
						&& index.isBasicSatisfiable()){
					if(Params.verbosity > 0)
						FileUtil.writeLogAndConsole("safe disj");
					banDescendants(index, bannedMap, i1, i2);
					banDescendants(index, bannedMap, i2, i1);
					return 0;
				}
				else if(index.areDisjoint(i1, i2)){
					// if needed use this, the remainder of the body is for early problem detection
					//					index.retractDisjointness(i1, i2, false);
					//					retracted.add(start);
					//					banAncestors(index,bannedMap,i1,i2);
					//					banAncestors(index,bannedMap,i2,i1);
					//					System.out.println("unsafe, retracted (" 
					//							+ i1 + ", " + i2 + ")");
					//					return 1;

					index.retractDisjointness(i1, i2, true);
					// if it is not satisfiable there is an unsolved error in 
					// the preceding part that won't be analyzed further...					
					if(index.isBasicSatisfiable()){
						retracted.add(start);
						banAncestors(index,bannedMap,i1,i2);
						banAncestors(index,bannedMap,i2,i1);
						if(Params.verbosity > 0)
							FileUtil.writeLogAndConsole("unsafe, retracted (" 
									+ i1 + ", " + i2 + ")");
						return 1;
					}
					// this case should not happen... unless the fix will fail
					else {
						throw new Error("Impossible to fix, a previous error was skipped");
						//						if(!retracted.contains(start))
						//							index.addDisjointness(i1, i2, false);
						//						System.out.println("safe disj because we cannot prove the opposite");
						//						banDescendants(index, bannedMap, i1, i2);
						//						banDescendants(index, bannedMap, i2, i1);
						//						return 0;
					}
				}
			}
			throw new Error("Inconsistent state while solving a disjointness");
		}

		index.recreateDisjointIntervalsStructure();

		// if [start,end] is ok the problem is in (end, bufferSize]
		if(index.isBasicSatisfiable()){
			if(Params.verbosity > 0)
				FileUtil.writeLogAndConsole("safe");

			for (int i = start; i <= end; i++) {
				banDescendants(index, bannedMap, disjBuffer1[i], 
						disjBuffer2[i]);
				banDescendants(index, bannedMap, disjBuffer2[i], 
						disjBuffer1[i]);
			}			
			return 0;
		}
		// [start, end] is already problematic, fix it
		else {
			if(Params.verbosity > 0)
				FileUtil.writeLogAndConsole("problematic");
			int newEnd = start + (int) Math.floor(((float)(end-start))/2);

			int retractedNum = fixUnsafeDisjIndex(index, end+1, disjBuffer1, 
					disjBuffer2, bannedMap, start, newEnd, retracted); 

			// reinsert disj axioms of interval [newEnd+1,end] (removed by preceding call)
			for (int i = newEnd+1; i <= end; i++){
				if(!retracted.contains(i))
					index.addDisjointness(disjBuffer1[i], disjBuffer2[i], false);
			}

			retractedNum += fixUnsafeDisjIndex(index, bufferSize, 
					disjBuffer1, disjBuffer2, bannedMap, newEnd+1, end, 
					retracted);

			if(retractedNum <= 0){
				FileUtil.writeErrorLogAndConsole("[" + start + ", " + end 
						+ "] was problematic but no disj were removed!?");
				index.recreateDisjointIntervalsStructure();
				FileUtil.writeLogAndConsole("Really solved? " 
						+ index.isBasicSatisfiable());
			}

			return retractedNum;
		}
	}

	public static int fixUnsafeDisjDowlingGallier(JointIndexManager index, 
			int bufferId, int [] disjBuffer1, int [] disjBuffer2, 
			Map<Integer, Set<Integer>> bannedMap, List<Integer> idList){
		Map<Integer, Set<Integer>> tax = index.getDirectSubClasses(false);
		Map<Integer, Set<Integer>> equiv = index.getEquivalentClasses();
		Map<Integer, Set<Integer>> disjointness = index.getDirectDisjointClasses();
		Map<Set<Integer>, Integer> generalHornAxioms = index.getGeneralHornAxiom();
		Map<Integer, Set<Integer>> types = index.getDirectIndividualClassTypes();
		Map<Integer, Set<Integer>> fixedmappings = new HashMap<Integer, Set<Integer>>();
		Map<Integer, Set<Integer>> mappings= new HashMap<Integer, Set<Integer>>();
		Set<HornClause> mappings2ignore = new HashSet<HornClause>();

		int retracted = 0;

		DowlingGallierHornSAT sat = 
				new DowlingGallierHornSAT(tax, equiv, disjointness, 
						generalHornAxioms, false, types, fixedmappings, 
						mappings, mappings2ignore);

		// these will be the safe axioms (unretracted)
		Set<Pair<Integer>> safeDisj = new HashSet<>();
		for (int i = 0; i < bufferId; i++)
			safeDisj.add(new Pair<Integer>(
					disjBuffer1[i],
					disjBuffer2[i]));

		if(!sat.isSatisfiable()){
			FileUtil.writeErrorLogAndConsole("Ontology unsatisfiability detected");

			for (HornClause unsafeDisj 
					: sat.getConflictiveDisjointness()) {
				FileUtil.writeLogAndConsole("Retracting disjointness " 
						+ unsafeDisj.getLeftHS1() + ", " 
						+ unsafeDisj.getLeftHS2());
				// ancestors will cause problems too
				banAncestors(index,bannedMap,
						unsafeDisj.getLeftHS1(),
						unsafeDisj.getLeftHS2());
				banAncestors(index,bannedMap,
						unsafeDisj.getLeftHS2(),
						unsafeDisj.getLeftHS1());
				safeDisj.remove(new Pair<Integer>(
						unsafeDisj.getLeftHS1(),
						unsafeDisj.getLeftHS2()));
				safeDisj.remove(new Pair<Integer>(
						unsafeDisj.getLeftHS2(),
						unsafeDisj.getLeftHS1()));
				++retracted;
			}
		}
		else {
			for (Integer id : idList){
				if(!sat.isSatisfiable(id)){
					for (HornClause unsafeDisj 
							: sat.getConflictiveDisjointness()) {
						FileUtil.writeLogAndConsole("Retracting disjointness " 
								+ unsafeDisj.getLeftHS1() + ", " 
								+ unsafeDisj.getLeftHS2());
						safeDisj.remove(new Pair<Integer>(
								unsafeDisj.getLeftHS1(),
								unsafeDisj.getLeftHS2()));
						safeDisj.remove(new Pair<Integer>(
								unsafeDisj.getLeftHS2(),
								unsafeDisj.getLeftHS1()));
						// ancestors will cause problems too
						banAncestors(index,bannedMap,
								unsafeDisj.getLeftHS1(),
								unsafeDisj.getLeftHS2());
						banAncestors(index,bannedMap,
								unsafeDisj.getLeftHS2(),
								unsafeDisj.getLeftHS1());
						++retracted;
					}
				}
			}
		}

		// avoid already entailed disjointness axioms
		for (Pair<Integer> pair : safeDisj) {
			banDescendants(index, bannedMap, 
					pair.getFirst(), pair.getSecond());
			banDescendants(index, bannedMap, 
					pair.getSecond(), pair.getFirst());
		}

		return retracted;
	}

	public static void banAncestors(JointIndexManager index, 
			Map<Integer, Set<Integer>> bannedMap, int id1, int id2){
		Set<Integer> banned = null;
		Set<Integer> sups = index.getSubsetOfSuperClasses4Identifier(id1);
		Set<Integer> sups2 = index.getSubsetOfSuperClasses4Identifier(id2);
		for (Integer sup : sups) {
			if(sups2.isEmpty())
				break;
			if(!bannedMap.containsKey(sup))
				bannedMap.put(sup, new HashSet<Integer>());
			banned = bannedMap.get(sup);
			banned.addAll(index.getSubsetOfSuperClasses4Identifier(id2));
		}
		if(!sups.isEmpty()){
			if(!bannedMap.containsKey(id2))
				bannedMap.put(id2, new HashSet<Integer>());
			bannedMap.get(id2).addAll(sups);
		}
	}

	public static void banDescendants(JointIndexManager index, 
			Map<Integer, Set<Integer>> bannedMap, int id1, int id2){
		Set<Integer> banned = null;
		Set<Integer> subs = index.getSubsetOfSubClasses4Identifier(id1);
		Set<Integer> subs2 = index.getSubsetOfSubClasses4Identifier(id2);
		for (Integer sub : subs) {
			if(subs2.isEmpty())
				break;
			if(!bannedMap.containsKey(sub))
				bannedMap.put(sub, new HashSet<Integer>());
			banned = bannedMap.get(sub);
			banned.addAll(index.getSubsetOfSubClasses4Identifier(id2));
		}
		if(!subs.isEmpty()){
			if(!bannedMap.containsKey(id2))
				bannedMap.put(id2, new HashSet<Integer>());
			bannedMap.get(id2).addAll(subs);
		}
	}

	public static void incrementalDisjSafetyCheck(OWLReasoner reasoner){
		if(Params.incrementalReasoning 
				&& !Params.reasonerKind.equals(REASONER_KIND.PELLET))
			throw new UnsupportedOperationException(
					"This feature is only supported by Pellet reasoner");
		//		if(!Params.incrementalReasoning)
		//			throw new UnsupportedOperationException(
		//					"Incremental reasoning is not enabled");
	}

	//	public static OntologyProcessing indexSetup(OWLOntology onto, 
	//			JointIndexManager index){
	//		return indexSetup(onto,index,null);
	//	}

	public static OntologyProcessing indexSetup(OWLOntology onto, 
			JointIndexManager index, OWLReasoner r){

		// we reuse extended reasoners only because otherwise they perform poorly on disjointness axioms 
		OntologyProcessing ontoProc = getOntoProcessing(onto,index,
				((r != null && r.isPrecomputed(InferenceType.CLASS_HIERARCHY) 
				//&& OntoUtil.isELKReasoner(r)
				&& OntoUtil.isExtendedReasoner(r)
						) ? r : null));

		index.setIntervalLabellingIndex(new HashMap<Integer,Set<Integer>>());
		index.clearAuxStructuresforLabellingSchema();
		return ontoProc;
	}

	public static OntologyProcessing [] indexSetup(OWLOntology onto1, 
			OWLOntology onto2, JointIndexManager index, OWLReasoner r1, 
			OWLReasoner r2){

		boolean useR1 = r1 != null && r1.isPrecomputed(InferenceType.CLASS_HIERARCHY) 
				&& OntoUtil.isExtendedReasoner(r1), 
				useR2 = r2 != null && r2.isPrecomputed(InferenceType.CLASS_HIERARCHY) 
				&& OntoUtil.isExtendedReasoner(r2);

		OntologyProcessing onto_process1 = new OntologyProcessing(
				onto1, index, new LexicalUtilities(), 
				(useR1 ? r1 : null));

		OntologyProcessing onto_process2 = new OntologyProcessing(
				onto2, index, new LexicalUtilities(), 
				(useR2 ? r2 : null));

		long init, fin;

		//Extracts lexicon
		init = Util.getMSec();
		onto_process1.precessLexicon(false);
		onto_process2.precessLexicon(false);
		fin = Util.getMSec();
		FileUtil.writeLogAndConsole("Time indexing entities (s): " 
				+ (float)((double)fin-(double)init)/1000.0);

		//Extracts Taxonomy
		//Also extracts A^B->C
		init = Util.getMSec();
		onto_process1.setTaxonomicData(useR1 ? r1 : null);
		onto_process2.setTaxonomicData(useR2 ? r2 : null);

		index.setIntervalLabellingIndex(new HashMap<Integer, Set<Integer>>());
		index.clearAuxStructuresforLabellingSchema();

		if(!useR1)
			onto_process1.clearReasoner();
		if(!useR2)
			onto_process2.clearReasoner();

		fin = Util.getMSec();

		FileUtil.writeLogAndConsole("Time extracting structural information (s): " 
				+ (float)((double)fin-(double)init)/1000.0);

		return new OntologyProcessing[]{onto_process1,onto_process2};
	}

	//	public static OntologyProcessing getOntoProcessing(OWLOntology onto, 
	//			JointIndexManager index){
	//		OntologyProcessing onto_process = new OntologyProcessing(onto, index, 
	//				new LexicalUtilities());
	//		onto_process.precessLexicon(false);
	//		onto_process.setTaxonomicData();
	//		onto_process.clearReasoner();
	//		//		onto_process.clearFrequencyRelatedStructures();		
	//		//		onto_process.clearFilteredInvertedIndex();				
	//		return onto_process;
	//	}

	public static OntologyProcessing getOntoProcessing(OWLOntology onto, 
			JointIndexManager index, OWLReasoner r){
		OntologyProcessing onto_process = new OntologyProcessing(onto, index, 
				new LexicalUtilities(), OntoUtil.isExtendedReasoner(r) ? r : null);
		long init, fin;

		//Extracts lexicon
		init = Util.getMSec();
		onto_process.precessLexicon(false);
		fin = Util.getMSec();
		FileUtil.writeLogAndConsole("Time indexing entities (s): " 
				+ (float)((double)fin-(double)init)/1000.0);

		init = Util.getMSec();
		onto_process.setTaxonomicData(r);

		if(r==null)
			onto_process.clearReasoner();
		fin = Util.getMSec();

		FileUtil.writeLogAndConsole("Time extracting structural information (s): " 
				+ (float)((double)fin-(double)init)/1000.0);

		//		onto_process.clearFrequencyRelatedStructures();		
		//		onto_process.clearFilteredInvertedIndex();				
		return onto_process;
	}

	@Deprecated
	public static JointIndexManager buildOntologiesIndex(OWLOntology onto1, 
			OWLOntology onto2){
		JointIndexManager index = new JointIndexManager();

		OntologyProcessing onto_process1 = new OntologyProcessing(
				onto1, index, new LexicalUtilities());
		OntologyProcessing onto_process2 = new OntologyProcessing(
				onto2, index, new LexicalUtilities());

		long init, fin;

		//Extracts lexicon
		init = Util.getMSec();
		onto_process1.precessLexicon(false);
		onto_process2.precessLexicon(false);
		fin = Util.getMSec();
		FileUtil.writeLogAndConsole("Time indexing entities (s): " 
				+ (float)((double)fin-(double)init)/1000.0);

		//Extracts Taxonomy
		//Also extracts A^B->C
		init = Util.getMSec();
		onto_process1.setTaxonomicData();
		onto_process2.setTaxonomicData();

		index.setIntervalLabellingIndex(new HashMap<Integer, Set<Integer>>());
		index.clearAuxStructuresforLabellingSchema();

		onto_process1.clearReasoner();
		onto_process2.clearReasoner();

		fin = Util.getMSec();

		FileUtil.writeLogAndConsole("Time extracting structural information (s): " 
				+ (float)((double)fin-(double)init)/1000.0);

		return index;
	}

	@Deprecated
	public static JointIndexManager buildOntologiesIndex(OWLOntology onto1, 
			OWLOntology onto2, String mapping){

		Set<MappingObjectStr> input_mappings = new HashSet<>();

		//Create Index and new Ontology Index..
		if(mapping != null){
			MappingsReaderManager readermanager = 
					new MappingsReaderManager(mapping, "RDF");

			input_mappings = readermanager.getMappingObjects();
		}

		// Param 3: If the intersection or overlapping of the ontologies 
		// are extracted before the repair
		// Param 4: If the repair is performed in a two steps process 
		// (optimal) or in one cleaning step (more aggressive)
		LogMap2_RepairFacility logmap2_repair = 
				new LogMap2_RepairFacility(onto1, onto2, 
						input_mappings, false, false);

		Field operands;
		JointIndexManager index = null;

		index = new JointIndexManager();

		try {
			Class rightJavaClass = logmap2_repair.getClass();//.getSuperclass();
			Field[] fs = rightJavaClass.getDeclaredFields();

			operands = Class.forName(rightJavaClass.getName()).getDeclaredField(
					fs[5].getName());

			// BEGIN reflection code
			operands.setAccessible(true);
			index = (JointIndexManager) operands.get(logmap2_repair);
			operands.setAccessible(false);
			// END reflection code
		}
		catch(Exception e){
			e.printStackTrace();
		}

		return index;
	}

	static public void validateIndexWithReasoner(JointIndexManager index, 
			OntologyProcessing op, OWLReasoner reasoner, int ontoId){
		OWLClass ci, cj;
		for (Integer i : index.getClassIdentifierSet()) {
			if(index.getClassIndex(i).getOntologyId() != ontoId || index.isBottomClass(i))
				continue;
			ci = index.getOWLClass4ConceptIndex(i);

			if(reasoner.getUnsatisfiableClasses().contains(ci))
				continue;

			for (Integer j : index.getClassIdentifierSet()) {
				if(i == j || index.getClassIndex(j).getOntologyId() != ontoId || 
						index.isBottomClass(j))
					continue;
				cj = index.getOWLClass4ConceptIndex(j);

				if(reasoner.getUnsatisfiableClasses().contains(cj))
					continue;

				// we skip this test because disjoint axioms inference is very 
				// costly for the reasoner

				//				if(index.areDisjoint(i, j) != index.areDisjoint(j, i) 
				//				|| 
				//				index.areDisjoint(i, j) != 
				//					reasoner.getDisjointClasses(ci).containsEntity(cj)
				//				||
				//				index.areDisjoint(j, i) != 
				//					reasoner.getDisjointClasses(ci).containsEntity(cj)){
				//			FileUtil.writeErrorLogAndConsole(ci + " DISJ " + cj + " = " + index.areDisjoint(i, j));
				//			FileUtil.writeErrorLogAndConsole(cj + " DISJ " + ci + " = " + index.areDisjoint(j, i));
				//			FileUtil.writeErrorLogAndConsole(ci + " DISJ " + cj + " reasoner = " 
				//					+ reasoner.getDisjointClasses(ci).containsEntity(cj));
				//			
				//			FileUtil.writeErrorLogAndConsoleNONL("Index (first class): ");
				//			for (int id : index.getDirectDisjointClasses4Identifier(i)) {
				//				FileUtil.writeErrorLogAndConsole(index.getOWLClass4ConceptIndex(id).toString());
				//			}
				//			FileUtil.writeErrorLogAndConsole("\n");
				//			FileUtil.writeErrorLogAndConsoleNONL("Index (second class): ");
				//			for (int id : index.getDirectDisjointClasses4Identifier(j)) {
				//				FileUtil.writeErrorLogAndConsole(index.getOWLClass4ConceptIndex(id).toString());
				//			}
				//			FileUtil.writeErrorLogAndConsole("\n");
				//			FileUtil.writeErrorLogAndConsole("Reasoner: " + 
				//			reasoner.getDisjointClasses(ci).getFlattened().toString().replace(", ", ",\n") + "\n");
				//		}

				//				if(index.areDisjoint(i, j) != index.areDisjoint(j, i) 
				//						|| 
				//						index.areDisjoint(i, j) != 
				//							reasoner.getDisjointClasses(ci).containsEntity(cj)
				//						||
				//						index.areDisjoint(j, i) != 
				//							reasoner.getDisjointClasses(ci).containsEntity(cj)){
				//					FileUtil.writeErrorLogAndConsole(ci + " DISJ " + cj + " = " + index.areDisjoint(i, j));
				//					FileUtil.writeErrorLogAndConsole(cj + " DISJ " + ci + " = " + index.areDisjoint(j, i));
				//					FileUtil.writeErrorLogAndConsole(ci + " DISJ " + cj + " reasoner = " 
				//							+ reasoner.getDisjointClasses(ci).containsEntity(cj));
				//					
				//					FileUtil.writeErrorLogAndConsoleNONL("Index (first class): ");
				//					for (int id : index.getDirectDisjointClasses4Identifier(i)) {
				//						FileUtil.writeErrorLogAndConsole(index.getOWLClass4ConceptIndex(id).toString());
				//					}
				//					FileUtil.writeErrorLogAndConsole("\n");
				//					FileUtil.writeErrorLogAndConsoleNONL("Index (second class): ");
				//					for (int id : index.getDirectDisjointClasses4Identifier(j)) {
				//						FileUtil.writeErrorLogAndConsole(index.getOWLClass4ConceptIndex(id).toString());
				//					}
				//					FileUtil.writeErrorLogAndConsole("\n");
				//					FileUtil.writeErrorLogAndConsole("Reasoner: " + 
				//					reasoner.getDisjointClasses(ci).getFlattened().toString().replace(", ", ",\n") + "\n");
				//				}

				if(index.isSubClassOf(i, j) != index.isSuperClassOf(j, i) 
						|| 
						(index.isSubClassOf(i, j) &&  
								!reasoner.getSubClasses(cj, false).containsEntity(ci) &&
								!reasoner.getEquivalentClasses(cj).contains(ci))){
					FileUtil.writeErrorLogAndConsole(ci + " ISA " + cj + " = " + index.isSubClassOf(i, j));
					FileUtil.writeErrorLogAndConsole(cj + " SUPCLASSOF " + ci + " = " + index.isSuperClassOf(j, i));
					FileUtil.writeErrorLogAndConsole(ci + " ISA " + cj + " reasoner = " 
							+ reasoner.getSubClasses(cj, false).containsEntity(ci));

					FileUtil.writeErrorLogAndConsoleNONL("Index (first class): ");
					for (Integer id : index.getSubClasses4Identifier(i)) {
						FileUtil.writeErrorLogAndConsole(""+index.getOWLClass4ConceptIndex(id));
					}
					FileUtil.writeErrorLogAndConsole("\n");
					FileUtil.writeErrorLogAndConsoleNONL("Index (second class): ");

					for (Integer id : index.getSuperClasses4Identifier(j)) {
						FileUtil.writeErrorLogAndConsole(""+index.getOWLClass4ConceptIndex(id));
					}
					FileUtil.writeErrorLogAndConsole("\n");
					FileUtil.writeErrorLogAndConsole("Reasoner: " + 
							reasoner.getSubClasses(ci, false).getFlattened().toString().replace(", ", ",\n") + "\n" + 
							reasoner.getEquivalentClasses(ci).toString().replace(", ", ",\n") + "\n");
				}
				if(index.areEquivalentClasses(i, j) != index.areEquivalentClasses(j, i)){
					FileUtil.writeErrorLogAndConsole(ci + " EQUIV " + cj + " = " + index.areEquivalentClasses(i,j));
					FileUtil.writeErrorLogAndConsole(cj + " EQUIV " + ci + " = " + index.areEquivalentClasses(j,i));
				}
				if(index.areEquivalentClasses(i, j)
						&& (index.areEquivalentClasses(i, j) != index.isSubClassOf(j, i))){
					FileUtil.writeErrorLogAndConsole(ci + " EQUIV " + cj + " but not " + cj + " ISA " + ci+"\n");
				}

				if(index.areEquivalentClasses(i, j) && 
						index.areEquivalentClasses(i, j) != index.isSuperClassOf(j, i))
					FileUtil.writeErrorLogAndConsole(ci + " EQUIV " + cj + " but not " + ci + " ISA " + cj+"\n");

				if(index.areEquivalentClasses(i, j) != reasoner.getEquivalentClasses(ci).contains(cj)){
					FileUtil.writeErrorLogAndConsole(ci + " EQUIV " + cj + " = " 
							+ index.areEquivalentClasses(i, j));
					FileUtil.writeErrorLogAndConsole(ci + " EQUIV " + cj + " reasoner? " 
							+ reasoner.getEquivalentClasses(ci).contains(cj)+"\n");
				}
			}
		}
	}

	public static void saveMappings(String output_file, OWLOntology onto1, 
			OWLOntology onto2, Set<MappingObjectStr> input_mappings){
		saveMappings(output_file, 
				onto1.getOntologyID().getOntologyIRI().toString(), 
				onto2.getOntologyID().getOntologyIRI().toString(),
				input_mappings);
	}

	public static void saveMappings(String output_file, String ontoIRI1, 
			String ontoIRI2, Set<MappingObjectStr> input_mappings){

		OutPutFilesManager outPutFilesManager = new OutPutFilesManager();

		try {
			outPutFilesManager.createOutFiles(
					output_file,
					//OutPutFilesManager.AllFormats,
					OutPutFilesManager.OAEIFormat,
					ontoIRI1,
					ontoIRI2);

			for (MappingObjectStr map : input_mappings){

				if (map.getTypeOfMapping()==Utilities.CLASSES){

					outPutFilesManager.addClassMapping2Files(
							map.getIRIStrEnt1(),
							map.getIRIStrEnt2(),
							map.getMappingDirection(), 
							map.getConfidence()
							);
				}

				else if (map.getTypeOfMapping()==Utilities.OBJECTPROPERTIES){ 

					outPutFilesManager.addObjPropMapping2Files(
							map.getIRIStrEnt1(),
							map.getIRIStrEnt2(),
							map.getMappingDirection(), 
							map.getConfidence()
							);

				}

				else if (map.getTypeOfMapping()==Utilities.DATAPROPERTIES){ 

					outPutFilesManager.addDataPropMapping2Files(
							map.getIRIStrEnt1(),
							map.getIRIStrEnt2(),
							map.getMappingDirection(), 
							map.getConfidence()
							);
				}

				else if (map.getTypeOfMapping()==Utilities.INSTANCES){ 
					outPutFilesManager.addInstanceMapping2Files(
							map.getIRIStrEnt1(),
							map.getIRIStrEnt2(),
							//map.getMappingDirection(), 
							map.getConfidence()
							);

				}
			}
			outPutFilesManager.closeAndSaveFiles();
		}
		catch (Exception e){
			FileUtil.writeErrorLogAndConsole("Error saving mappings...");
			e.printStackTrace();
		}
	}

	public static Collection<Pair<OWLClass>> getOWLClassesFromMappings(
			MappingObjectStr m) {
		Set<Pair<OWLClass>> res = Sets.newHashSet();

		if(m.getMappingDirection() == Utilities.EQ){
			res.add(OntoUtil.getOWLClassesFromIRIs(
					m.getIRIStrEnt1(),m.getIRIStrEnt2()));
			res.add(OntoUtil.getOWLClassesFromIRIs(
					m.getIRIStrEnt2(),m.getIRIStrEnt1()));
		}
		else if(m.getMappingDirection() == Utilities.L2R)
			res.add(OntoUtil.getOWLClassesFromIRIs(
					m.getIRIStrEnt1(),m.getIRIStrEnt2()));

		else if(m.getMappingDirection() == Utilities.R2L)
			res.add(OntoUtil.getOWLClassesFromIRIs(
					m.getIRIStrEnt2(),m.getIRIStrEnt1()));

		return res;
	}

	public static Collection<? extends Pair<OWLClass>> getOWLClassesFromMappings(
			Collection<? extends MappingObjectStr> pairs) {
		Set<Pair<OWLClass>> res = Sets.newHashSet();

		for (MappingObjectStr m : pairs)
			res.addAll(getOWLClassesFromMappings(m));

		return res;
	}

	public static URL saveTemporaryMappings(String string,
			String iriOntology1, String iriOntology2,
			Set<MappingObjectStr> repairedMappings) {

		File f = null;

		try {
			f = File.createTempFile(string, ".rdf");
			saveMappings(FileUtil.removeExtension(f.getAbsolutePath()), 
					iriOntology1, iriOntology1, repairedMappings);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			return f.toURI().toURL();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		return null;
	}

	public static OWLEntity getEntity(boolean fst, MappingObjectStr m) {
		String iriStr = fst ? m.getIRIStrEnt1() : m.getIRIStrEnt2();
		IRI iri = IRI.create(iriStr);
		OWLEntity ent = null;
		
		switch(m.getTypeOfMapping()){
		case Utilities.CLASSES:
			ent = OntoUtil.getDataFactory().getOWLEntity(EntityType.CLASS, iri);
			break;
		case Utilities.DATAPROPERTIES:
			ent = OntoUtil.getDataFactory().getOWLEntity(EntityType.DATA_PROPERTY, iri);
			break;
		case Utilities.OBJECTPROPERTIES:
			ent = OntoUtil.getDataFactory().getOWLEntity(EntityType.OBJECT_PROPERTY, iri);
			break;
		default:
			throw new Error("Unknown mapping type: " + m.getTypeOfMapping());
		}
		
		return ent;
	}

	public static Set<OWLEntity> getSignature(boolean firstOnto, 
			Set<MappingObjectStr> mappings) {

		Set<OWLEntity> sign = new HashSet<>();
		
		for (MappingObjectStr m : mappings)
			sign.add(getEntity(firstOnto, m));	
		
		return sign;
	}

	public static Set<MappingObjectStr> addCloningAlignment(MappingObjectStr m,
			Set<MappingObjectStr> align) {
		Set<MappingObjectStr> newAlign = cloneAlignment(align);
		newAlign.add(new MappingObjectStr(m));			
			
		return newAlign;
	}

	public static boolean isAlignmentContained(
			Set<MappingObjectStr> align1,
			Set<MappingObjectStr> align2) {
		
		for (MappingObjectStr m : align1)
			if(!LogMapWrapper.isContained(m, align2, true))
				return false;
				
		return true;
	}

	public static boolean areAlignmentsEquivalent(
			Set<MappingObjectStr> align1,
			Set<MappingObjectStr> align2) {
		return isAlignmentContained(align1, align2) && 
				isAlignmentContained(align2, align1);
	}
	
	public static void writeAlignmentToFile(String name, String oiri1, 
			String oiri2, Set<MappingObjectStr> align) throws Exception{
		OAEIAlignmentOutput oaeiAlignOutput = new OAEIAlignmentOutput(name,oiri1,oiri2,true); 
		for (MappingObjectStr m : align) {
			switch(m.getTypeOfMapping()){
				case Utilities.CLASSES:
					oaeiAlignOutput.addClassMapping2Output(m.getIRIStrEnt1(), 
							m.getIRIStrEnt2(), m.getMappingDirection(), 
							m.getConfidence());
				break;
				case Utilities.DATAPROPERTIES:
					oaeiAlignOutput.addClassMapping2Output(m.getIRIStrEnt1(), 
							m.getIRIStrEnt2(), m.getMappingDirection(), 
							m.getConfidence());
				break;
				case Utilities.OBJECTPROPERTIES:
					oaeiAlignOutput.addClassMapping2Output(m.getIRIStrEnt1(), 
							m.getIRIStrEnt2(), m.getMappingDirection(), 
							m.getConfidence());
				break;
				case Utilities.INSTANCES:
					oaeiAlignOutput.addClassMapping2Output(m.getIRIStrEnt1(), 
							m.getIRIStrEnt2(), m.getMappingDirection(), 
							m.getConfidence());
				break;
				default:
					throw new Error("Unknown mapping type: " + 
							m.getTypeOfMapping());
			}
		}
		oaeiAlignOutput.saveOutputFile();
	}
}
