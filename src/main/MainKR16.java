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
package main;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import logmap.LogMapWrapper;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import auxStructures.Pair;
import repair.ConservativityRepairFacility;
import uk.ac.ox.krr.logmap2.Parameters;
import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;
import util.OntoUtil;
import util.Params;
import util.Util;

import enumerations.VIOL_KIND;

public class MainKR16 {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {

		List<VIOL_KIND> violKinds = new ArrayList<>();
		violKinds.add(VIOL_KIND.EQONLY);
		violKinds.add(VIOL_KIND.APPROX);
		violKinds.add(VIOL_KIND.FULL);
		
		// 1) some global parameters for the algorithm
		// this parameter controls if a full consistency repair (using justifications) 
		// is applied before using our method, if false it is using the 
		// Downling&Gallier method, but without completeness guarantees 
		Params.fullReasoningRepair = false; 
		// extra heuristic for library track, not needed here
		Parameters.repair_heuristic = false;
		Params.suppressOutputFully();
		Params.storeViolations = true; //so we can print them if needed
		
		// 2) loading ontologies and alignments
		OWLOntologyManager manager = OntoUtil.getManager(false);

		String [] ontoPaths = {"owled15/alice.owl", "owled15/bob.owl"};

		// load the two ontologies (the other only needed for the signature)
		OWLOntology ontoAlice = OntoUtil.load(ontoPaths[0], true, manager);
		OWLOntology ontoBob = OntoUtil.load(ontoPaths[1], true, manager);

		String alignPath = "owled15/align2.rdf";
		
		Set<MappingObjectStr> mappings = LogMapWrapper.getMappings(alignPath, 
				ontoAlice, ontoBob);
		
		// 3) create the conservativity repair facility 
		ConservativityRepairFacility repair = 
				new ConservativityRepairFacility(
						ontoAlice, ontoBob, manager, mappings);
		
		System.out.println("Original alignment:" + 
				Util.prettyPrint(mappings));
		
		System.out.println("Violations BEFORE repair");
		
		for (VIOL_KIND vk : violKinds) {
			List<Pair<Integer>> conservativityViol1 = 
					repair.getViolations(true, vk, repair.getRepairStep());
			List<Pair<Integer>> conservativityViol2 = 
					repair.getViolations(false, vk, repair.getRepairStep());

			System.out.println(vk + ", onto 1:\n" + 
					Util.prettyPrint(LogMapWrapper.getOWLClassFromIndexPair(
							conservativityViol1, repair.getAlignIndex())));
			System.out.println(vk + ", onto 2:\n" +
					Util.prettyPrint(LogMapWrapper.getOWLClassFromIndexPair(
							conservativityViol2, repair.getAlignIndex())));
		}
		
		// 4) the repair is computed
		repair.repair(true);
		Set<MappingObjectStr> repairedMappings = repair.getRepairedMappings();
		
		System.out.println("Repaired mappings:\n" + 
				Util.prettyPrint(repairedMappings));
		
		System.out.println("Violations AFTER repair");
		
		for (VIOL_KIND vk : violKinds) {
			List<Pair<Integer>> conservativityViol1 = 
					repair.getViolations(true, vk, repair.getRepairStep());
			List<Pair<Integer>> conservativityViol2 = 
					repair.getViolations(false, vk, repair.getRepairStep());

			System.out.println(vk + ", onto 1:\n" + 
					Util.prettyPrint(LogMapWrapper.getOWLClassFromIndexPair(
							conservativityViol1, repair.getAlignIndex())));
			System.out.println(vk + ", onto 2:\n" +
					Util.prettyPrint(LogMapWrapper.getOWLClassFromIndexPair(
							conservativityViol2, repair.getAlignIndex())));
		}
	}		
}
