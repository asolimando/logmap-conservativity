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

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import logmap.LogMapWrapper;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import auxStructures.Pair;
import auxStructures.RepairStatus;

import repair.ConservativityRepairFacility;
import repair.MASRepair;
import uk.ac.ox.krr.logmap2.Parameters;
import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;
import util.OntoUtil;
import util.Params;
import util.Util;

import enumerations.REPAIR_METHOD;
import enumerations.VIOL_KIND;

public class MainOWLED15 {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {

		boolean KR = true, verbose = true;
		
		List<VIOL_KIND> violKinds = new ArrayList<>();
		violKinds.add(VIOL_KIND.EQONLY);
		violKinds.add(VIOL_KIND.APPROX);
		violKinds.add(VIOL_KIND.CONSISTENCY);

		REPAIR_METHOD rm = REPAIR_METHOD.SUBEQ;
		
		Params.fullReasoningRepair = false;
		Parameters.repair_heuristic = false;
		Params.suppressOutputFully();
		Params.storeViolations = verbose;
		
		OWLOntologyManager managerTmp = OntoUtil.getManager(false), 
				manager = OntoUtil.getManager(true);

		String [] ontoPaths = {"owled15/aliceNODISJ.owl", "owled15/bob.owl"};
		if(KR)
			ontoPaths[0] = "owled15/alice.owl";
		
		// load the two ontologies (the other only needed for the signature)
		OWLOntology ontoAlice = OntoUtil.load(ontoPaths[0], true, managerTmp);
		OWLOntology ontoBob = OntoUtil.load(ontoPaths[1], true, managerTmp);

		String alignPath = "owled15/align" + (KR ? "" : "2") + ".rdf";
		
		List<OWLOntology> ontos = new ArrayList<>(2);
		ontos.add(ontoAlice);
		ontos.add(ontoBob);

		MASRepair masRepairAlice = null, masRepairBob = null, 
				repairFacActive = null, repairFacPassive = null;
		try {
			masRepairAlice = new MASRepair(ontoAlice, ontoBob, true, true);
			masRepairBob = new MASRepair(ontoBob, ontoAlice, true, false);
			
			if(verbose){
				masRepairAlice.setVerbosity(true);
				masRepairBob.setVerbosity(true);
			}
		} catch (OWLOntologyCreationException e) {
			fail("Exception while creating the \"hidden\" ontology: " + e.getMessage());
		}

		List<MappingObjectStr> mappings = masRepairAlice.loadMappings(alignPath);
		if(KR){
			Collections.swap(mappings, 1, 3);
			Collections.swap(mappings, 2, 3);
			Collections.swap(mappings, 3, 4);
		}
		else {
			Collections.swap(mappings, 1, 3);
		}
		System.out.println("Loaded set of mappings:\n"+Util.prettyPrint(mappings));

		Set<MappingObjectStr> commitmentStore = new HashSet<>();

		Iterator<MappingObjectStr> itr = mappings.iterator();
		int c = 0;
		RepairStatus rsActive = null, rsPassive = null;
		int activeAgentID = 0, passiveAgentID = 1;
		String [] agentLabels = {"Alice","Bob"};

		System.out.println("\n"+MOVE.JOIN.toString(0));
		System.out.println("\n"+MOVE.JOIN.toString(1));
				
		while (itr.hasNext()) {
			MappingObjectStr m = itr.next();
					
//			if(c == 2){
//				c++;
//				System.out.println("Skipping " + m);
//				continue;
//			}
			
			if(c % 2 == 0){// || c == 5){
				repairFacActive = masRepairAlice;
				repairFacPassive = masRepairBob;
				activeAgentID = 0;
				passiveAgentID = 1;
			}
			else {
				repairFacActive = masRepairBob;
				repairFacPassive = masRepairAlice;
				activeAgentID = 1;
				passiveAgentID = 0;
			}

			c++;

			System.out.println("Actual commitment store: " + 
			commitmentStore.toString().replace(", ", ",\n"));
			
			if(m.getConfidence() < 0.45){
				System.out.println("\n"+MOVE.ASSERT.toString(passiveAgentID) + m);
				System.out.println("\n"+MOVE.REJECTC.toString(passiveAgentID) + m);
				continue;
			}
			
			System.out.println(agentLabels[activeAgentID] + " checks before asserting");
			rsActive = repairFacActive.assessMapping(manager, commitmentStore, 
					m, false, rm, violKinds);
			
			Set<MappingObjectStr> nextCS = LogMapWrapper.cloneAlignment(commitmentStore);
			nextCS.add(m);
			
			if(rsActive.hasViolations()){

				System.out.println("\n"+MOVE.ASSERT.toString(activeAgentID) + m + 
						" with REPAIR, " + rsActive.getViolationsNumber() + 
						" violation(s)");
				
				if(rsActive.hasValidRepair()){
					Set<MappingObjectStr> repairActive = rsActive.getRepair();

					// m could be modified!
					nextCS = LogMapWrapper.applyRepair(nextCS, repairActive);
										
					System.out.println(agentLabels[activeAgentID] + 
							" checks his/her own repair before proposing it");
					System.out.println(agentLabels[activeAgentID] + 
							"'s repair: " + repairActive.toString().replace(", ", ",\n"));

					rsActive = repairFacActive.assessMapping(manager, 
							nextCS, m, false, rm, violKinds);
					
					if(rsActive.hasViolations())
						System.out.println(agentLabels[activeAgentID] + 
								" cannot solve his/her " + 
								rsActive.getViolationsNumber() + 
								" violation(s) for " + m + ", not proposing it");
					else 
						if(!LogMapWrapper.isContained(m, nextCS)){
							System.out.println("The repair would remove the mapping itself, not proposing it");
							continue;
						}					
				}

				else {
					System.out.println(agentLabels[activeAgentID] + 
							" cannot solve his/her " + 
							rsActive.getViolationsNumber() + 
							" violation(s) for " + m + ", not proposing it");
					continue;
				}
			}
			else 
				System.out.println("\n"+MOVE.ASSERT.toString(activeAgentID) + m);
			

			// notice: m could have been weakened!
			
			System.out.println(agentLabels[passiveAgentID] + " checks before accepting");

			// we indirectly "apply" the suggested repair by using the repaired CS
			rsPassive = repairFacPassive.assessMapping(manager, nextCS, 
					m, false, rm, violKinds);

			if(rsPassive.hasViolations()){

				Set<MappingObjectStr> repairAsReply = rsPassive.getRepair();

				if(rsPassive.hasValidRepair()){
										
					System.out.println(agentLabels[passiveAgentID] + 
							" checks his/her own repair before proposing it:\n" + 
							repairAsReply.toString().replace(", ", ",\n"));
					
					nextCS = LogMapWrapper.applyRepair(nextCS, repairAsReply);
					
					rsPassive = repairFacPassive.assessMapping(manager, 
							nextCS, null, false, rm, violKinds);

					if(rsPassive.hasViolations()){
						System.out.println(m + ": " + agentLabels[passiveAgentID] + 
								" has " + rsPassive.getViolationsNumber() + 
								" violation(s) but cannot compute a repair, rejecting");
						System.out.println("\n"+MOVE.REJECTC.toString(passiveAgentID) + m);
						continue;
					}
					else {
						System.out.println("\n"+MOVE.REPAIR.toString(passiveAgentID) 
								+ "\n" + repairAsReply);

						// let's see if the repair is fine also for the other agent
						System.out.println(agentLabels[activeAgentID] + 
								" checks repair before accepting it");
						
						rsActive = repairFacActive.assessMapping(manager, 
								nextCS, m, false, rm, violKinds);

						if(rsActive.hasViolations()){
							System.out.println("\n"+MOVE.REJECTR.toString(activeAgentID) + m);
							continue;
						}
						else {
							System.out.println("\n"+MOVE.ACCEPTR.toString(activeAgentID));
							commitmentStore = nextCS;
							continue;
						}
					}										
				}
				else {
					System.out.println("\n"+MOVE.REJECTC.toString(passiveAgentID) + m);
					continue;
				}
			}
			else {
				System.out.println("\n"+MOVE.ACCEPTC.toString(passiveAgentID) + m);
				commitmentStore = nextCS;
				continue;
			}
		}

		System.out.println("\n"+MOVE.CLOSE.toString(1));
		System.out.println("\n"+MOVE.CLOSE.toString(0));

		System.out.println("Final commitment store: " + 
				commitmentStore.toString().replace(", ", ",\n"));
		
		System.out.println("END");
	}

	private enum MOVE {

		JOIN("1A","2B"),
		ASSERT("3A","6B"),
		ACCEPTC("7A","4B"),
		REJECTC("7A","4B"),
		REPAIR("7A","4B"),
		ACCEPTR("5A","8B"),
		REJECTR("5A","8B"),
		CLOSE("3A","6B");

		private String lblAgent1, lblAgent2;
		private String [] agentLabels = {"Alice","Bob"};

		private MOVE(String lblAgent1, String lblAgent2){
			this.lblAgent1 = lblAgent1;
			this.lblAgent2 = lblAgent2;
		}

		public String toString(int i){
			return "MOVE " + (i == 0 ? lblAgent1 : lblAgent2) + " " + 
					agentLabels[i] + " - " + name() + ": ";
		}
	}
}
