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
package junit;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import logmap.LogMapWrapper;

import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasoner;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.elk.owlapi.proofs.Proofs;
import org.semanticweb.elk.reasoner.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapitools.proofs.ExplainingOWLReasoner;
import org.semanticweb.owlapitools.proofs.exception.ProofGenerationException;

import enumerations.REPAIR_METHOD;
import enumerations.REPAIR_STRATEGY;
import enumerations.VIOL_KIND;

import repair.ConservativityRepairFacility;

import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;
import util.OntoUtil;
import util.Params;
import util.Util;

public class RepairUnitTest {
	
	@Test
	public void repairDoesNotCreateNovelMappingsSUB() throws Exception {
		REPAIR_METHOD rm = REPAIR_METHOD.SUB;
		System.out.println("Testing for " + rm);
		repairTest(rm);
	}	
	
	@Test
	public void repairDoesNotCreateNovelMappingsEQ() throws Exception {
		REPAIR_METHOD rm = REPAIR_METHOD.EQ;
		System.out.println("Testing for " + rm);
		repairTest(rm);
	}	

	@Test
	public void repairDoesNotCreateNovelMappingsEQSUB() throws Exception {
		REPAIR_METHOD rm = REPAIR_METHOD.EQSUB;
		System.out.println("Testing for " + rm);
		repairTest(rm);
	}
	
	@Test
	public void repairDoesNotCreateNovelMappingsSUBEQ() throws Exception {
		REPAIR_METHOD rm = REPAIR_METHOD.SUBEQ;
		System.out.println("Testing for " + rm);
		repairTest(rm);
	}
	
	private void repairTest(REPAIR_METHOD rm) throws Exception{
		
		Params.defaultRepairMethod = rm;
		OWLOntologyManager manager = OntoUtil.getManager(false);
		
		OWLOntology cmt = OntoUtil.load("testdataset/onto/cmt.owl", true, manager), 
				ekaw = OntoUtil.load("testdataset/onto/ekaw.owl", true, manager);
		
		Set<MappingObjectStr> mappings = 
				LogMapWrapper.getMappings("testdataset/align/referenceSample-cmt-ekaw.rdf", cmt, ekaw);
		Set<MappingObjectStr> originalMappings = LogMapWrapper.cloneAlignment(mappings);
		
		ConservativityRepairFacility repairFac = 
				new ConservativityRepairFacility(cmt, ekaw, manager, mappings);
		
		repairFac.detectViolations(true, false);
		
		int subViolNum = repairFac.getTotalViolationNumber(VIOL_KIND.APPROX, 
				repairFac.getRepairStep(), false);
		int eqViolNum = repairFac.getTotalViolationNumber(VIOL_KIND.EQONLY, 
				repairFac.getRepairStep(), false);

		System.out.println("SUB VIOLS: " + subViolNum);
		System.out.println("EQ VIOLS: " + eqViolNum);

		repairFac.repair();
		
		Set<MappingObjectStr> repair = repairFac.getRepair();
		Set<MappingObjectStr> repairedAlign = repairFac.getRepairedMappings();
		
		System.out.println("Repair: " + Util.prettyPrint(repair));
		System.out.println("Repaired alignment: " + Util.prettyPrint(repairedAlign));
		
		for (MappingObjectStr m : repair)
			if(!LogMapWrapper.isContained(m, originalMappings, true))
				fail(m + " is not known");
		
		for (MappingObjectStr m : repairedAlign)
			if(!LogMapWrapper.isContained(m, originalMappings, true))
				fail(m + " is not known");

		originalMappings = LogMapWrapper.applyRepair(originalMappings, repair);
		if(!LogMapWrapper.areAlignmentsEquivalent(repairedAlign,originalMappings))
			fail("Repair applied to original set of mappings differ from repaired " +
					"alignment: " + Util.prettyPrint(originalMappings));
		
//		LogMapWrapper.writeAlignmentToFile(rm.name()+".rdf", 
//				OntoUtil.extractIRIString(cmt), 
//				OntoUtil.extractIRIString(ekaw), 
//				repairedAlign);
				
		OntoUtil.unloadAllOntologies();

	}
}
