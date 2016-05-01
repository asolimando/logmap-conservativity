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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import logmap.LogMapWrapper;

import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import uk.ac.ox.krr.logmap2.LogMap2_RepairFacility;
import uk.ac.ox.krr.logmap2.io.LogOutput;
import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;
import uk.ac.ox.krr.logmap2.repair.AnchorAssessment;
import util.OntoUtil;
import util.Params;

import enumerations.REPAIR_METHOD;
import enumerations.VIOL_KIND;
import auxStructures.Pair;
import auxStructures.RepairStatus;

public class MASRepair {

	private boolean repairAlso = false, fstOnto;
	private OWLOntology knownOnto, hiddenOnto;
	private REPAIR_METHOD repairMethod = Params.defaultRepairMethod;

	private boolean keepLastMapping = false;
	private boolean consistencyRepair = true;
	private boolean verbose = false;
	
	/**
	 * MASRepair constructor
	 * @param knownOnto input ontology "known" to the agent
	 * @param hiddenOnto input ontology of the other agent (only needed for the signature)
	 * @param repairAlso true if we want to detect and repair, false if only detection is needed
	 * @param fstOnto true if the private ontology is input ontology 1, false otherwise
	 * @throws OWLOntologyCreationException thrown when it is not possible to create the fresh ontology
	 */
	public MASRepair(OWLOntology knownOnto, OWLOntology hiddenOnto, 
			boolean repairAlso, boolean fstOnto) throws OWLOntologyCreationException{
		this.repairAlso = repairAlso;
		this.fstOnto = fstOnto;
		Set<OWLEntity> signature = hiddenOnto.getSignature(false);
		OntoUtil.unloadOntologies(hiddenOnto);
		init(knownOnto, hiddenOnto.getOntologyID(), signature);
	}

	/**
	 * MASRepair constructor
	 * @param knownOnto input ontology "known" to the agent
	 * @param hiddenOntoID identifier of the input ontology of the other agent
	 * @param signatureHiddenOnto signature of the input ontology of the other agent
	 * @param repairAlso true if we want to detect and repair, false if only detection is needed
	 * @param fstOnto true if the private ontology is input ontology 1, false otherwise
	 * @throws OWLOntologyCreationException thrown when it is not possible to create the fresh ontology
	 */
	public MASRepair(OWLOntology knownOnto, OWLOntologyID hiddenOntoID, 
			Set<OWLEntity> signatureHiddenOnto, boolean repairAlso, boolean fstOnto) 
					throws OWLOntologyCreationException{
		this.repairAlso = repairAlso;
		this.fstOnto = fstOnto;
		init(knownOnto, hiddenOntoID, signatureHiddenOnto);
	}

	/**
	 * Controls the verbosity of the present class
	 * @param verbose true if messages are printed, false otherwise
	 */
	public void setVerbosity(boolean verbose){
		this.verbose = verbose;
	} 

	/**
	 * Returns the verbosity of the class
	 * @return true if messages are written as output, false otherwise
	 */	
	public boolean getVerbosity(){
		return verbose;
	} 

	/**
	 * Getter for input ontologies
	 * @param first true if the input ontology 1 is needed, false otherwise
	 * @return the corresponding input ontology
	 */
	private OWLOntology getOntology(boolean first){
		if(fstOnto)
			return first ? knownOnto : hiddenOnto;
		else			
			return first ? hiddenOnto : knownOnto;
	}

	/**
	 * Alignment loading method, must be called after loading the input ontologies
	 * @param alignPath path to the rdf file storing the alignment in the AlignmentAPI format
	 * @return the alignment represented with the internal format used by LogMap
	 */
	public List<MappingObjectStr> loadMappings(String alignPath){		
		return loadMappings(getOntology(true), getOntology(false), alignPath);
	}

	/**
	 * Alignment loading method, requires as input the input ontologies
	 * @param o1 input ontology 1
	 * @param o2 input ontology 2
	 * @param alignPath alignPath path to the rdf file storing the alignment in the AlignmentAPI format
	 * @return
	 */
	private List<MappingObjectStr> loadMappings(
			OWLOntology o1, OWLOntology o2, String alignPath){		
		List<MappingObjectStr> mappings = 
				new ArrayList<>(LogMapWrapper.getMappings(alignPath, o1 , o2));

				//		System.out.println(o1);
				//		System.out.println(o2);

				Collections.sort(mappings, new Comparator<MappingObjectStr>() {
					public int compare(MappingObjectStr m1, MappingObjectStr m2) {
						if(m1.getConfidence() > m2.getConfidence())
							return -1; 
						if(m1.getConfidence() < m2.getConfidence())
							return 1; 		
						int res = m1.getIRIStrEnt1().compareToIgnoreCase(m2.getIRIStrEnt1());

						if(res == 0)
							res = m1.getIRIStrEnt2().compareToIgnoreCase(m2.getIRIStrEnt2());

						return res;
					}
				});

				return mappings;
	}

	@Deprecated
	/**
	 * Assess violations for a set of pre-shared mappings after the addition of a novel one
	 * @param repairs the actual consistency repair facility that needs to be updated to reflect the mapping addition
	 * @param actualMappings actual set of agreed/shared mappings
	 * @param m novel mapping to be exchanged by the agents
	 * @param preRepair true if repair is applied before detecting novel violations due to the mapping addition
	 * @param repairMethod selects the desiderd repair method
	 * @param violKinds selects the set violation notions to be used in the detection phase
	 * @return information about the status of the shared mappings considering the novel addition
	 * @throws Exception 
	 */
	public RepairStatus assessMapping(List<ConservativityRepairFacility> repairs, 
			Set<MappingObjectStr> actualMappings, MappingObjectStr m, 
			boolean preRepair, REPAIR_METHOD repairMethod, 
			List<VIOL_KIND> violKinds) throws Exception{

		if(repairs.size() != 1 || repairs.get(0) == null)
			throw new IllegalArgumentException("Expected a list of " +
					"ConservativityRepairFacility objects with exactly " +
					"one non null element");

		this.repairMethod = repairMethod; 

		ConservativityRepairFacility repair = repairs.get(0);

		ConservativityRepairFacility newRepair = 
				new ConservativityRepairFacility(repairs.get(0), 
						actualMappings, preRepair);
		repair.dispose();

		repairs.remove(0);
		repairs.add(0, newRepair);

		LogMap2_RepairFacility repairFac = 
				getConsistencyRepairFacility(actualMappings,
						Collections.singleton(m));

		return assessMapping(repairFac, newRepair, actualMappings, 
				m, preRepair, violKinds);
	}

	/**
	 * Assess violations for a set of pre-shared mappings after the addition of a novel one
	 * @param manager the ontology manager storing the input ontologies
	 * @param actualMappings actual set of agreed/shared mappings
	 * @param m novel mapping to be exchanged by the agents
	 * @param preRepair true if repair is applied before detecting novel violations due to the mapping addition
	 * @param repairMethod selects the desiderd repair method
	 * @param violKinds selects the set violation notions to be used in the detection phase
	 * @return information about the status of the shared mappings considering the novel addition
	 * @throws Exception 
	 */
	public RepairStatus assessMapping(OWLOntologyManager manager, 
			Set<MappingObjectStr> actualMappings, MappingObjectStr m, 
			boolean preRepair, REPAIR_METHOD repairMethod, 
			List<VIOL_KIND> violKinds) throws Exception{

		this.repairMethod = repairMethod; 

		Set<MappingObjectStr> localMappings = LogMapWrapper.cloneAlignment(actualMappings);
		if(m!=null)
			localMappings.add(m);

		ConservativityRepairFacility repair = 
				new ConservativityRepairFacility(getOntology(true), 
						getOntology(false), manager, localMappings, preRepair, 
						false, repairMethod);

		consistencyRepair = violKinds.contains(VIOL_KIND.CONSISTENCY);

		LogMap2_RepairFacility repFac = getConsistencyRepairFacility(localMappings);

		return assessMapping(repFac, repair, localMappings, m, preRepair, violKinds);
	}

	/**
	 * Assess violations for a set of pre-shared mappings after the addition of a novel one
	 * @param repFac the conservativity repair facility for the actual set of mappings
	 * @param repair consistency repair facility for the actual set of mappings
	 * @param actualMappings actual set of agreed/shared mappings
	 * @param m novel mapping to be exchanged by the agents
	 * @param preRepair true if repair is applied before detecting novel violations due to the mapping addition
	 * @param violKinds selects the set violation notions to be used in the detection phase
	 * @return information about the status of the shared mappings considering the novel addition
	 * @throws Exception 
	 */
	public RepairStatus assessMapping(LogMap2_RepairFacility repFac, 
			ConservativityRepairFacility repair, 
			Set<MappingObjectStr> actualMappings, MappingObjectStr m, 
			boolean preRepair, List<VIOL_KIND> violKinds) throws Exception{

		int viols = 0, consistencyViol = 0, conservativityViol = 0;
		RepairStatus rs = new RepairStatus();

		if(violKinds.contains(VIOL_KIND.CONSISTENCY)){

			if(repFac == null)
				throw new IllegalArgumentException("LogMap2_RepairFacility " +
						"cannot be null when checking conservativity violations");

			int tmp = repFac.getUnsatsLowerBound();
			if(tmp > 0 && verbose){
				LogOutput.printAlways("Unsat classes detected: " + tmp);
				System.out.println("Unsat classes detected: " + tmp);
			}
			rs.setViolations(VIOL_KIND.CONSISTENCY, tmp);
			consistencyViol += tmp;
		}

		if(repairAlso && consistencyViol > 0){
			if(keepLastMapping)
				repair.consistencyRepair(true,Collections.singleton(m));					
			else
				repair.consistencyRepair(true);
		}

		for (VIOL_KIND vk : violKinds){

			if(!vk.equals(VIOL_KIND.CONSISTENCY)){
				if(repair == null)
					throw new IllegalArgumentException("ConservativityRepair " +
							"cannot be null when checking conservativity violations");

				int tmp = repair.getViolationNumber(fstOnto, vk, 
						repair.getRepairStep(), false);
				rs.setViolations(vk, tmp);
				conservativityViol += tmp;

				if(tmp > 0 && Params.storeViolations){
					List<Pair<Integer>> viol = 
							repair.getViolations(fstOnto, vk, repair.getRepairStep());
					if(!viol.isEmpty() && verbose)
						System.out.println(vk.name() + " violation(s): " + 
								LogMapWrapper.getOWLClassFromIndexPair(viol, 
							repair.getAlignIndex()).toString().replace(">>, <<", 
									">>,\n<<"));
				}
			}
			// already handled
		}

		viols = consistencyViol + conservativityViol;


		if(repairAlso && conservativityViol > 0){

			// repairing with SUB will always repair also inconsistencies
			if(repairMethod.equals(REPAIR_METHOD.EQ) && consistencyViol > 0 && 
					!violKinds.contains(VIOL_KIND.CONSISTENCY))
				if(keepLastMapping)
					repair.consistencyRepair(true,Collections.singleton(m));					
				else
					repair.consistencyRepair(true);

			if(keepLastMapping)
				repair.repair(repair.getViolations(fstOnto, VIOL_KIND.APPROX, 
						repair.getRepairStep()),false,false,Collections.singleton(m));
			else
				repair.repair(repair.getViolations(fstOnto, VIOL_KIND.APPROX, 
						repair.getRepairStep()),false,false);

			rs.setRepair(repair.getRepair());
			rs.setRepairedMappings(repair.getRepairedMappings());
		}

		if(viols > 0 && verbose){
			System.out.println("Mapping " + m + " would cause " + 
					viols + " violation(s)");
			
		}
		//		else if(viols > 0 && repairAlso && !repair.getRepairedMappings().contains(m))
		//			System.out.println("Mapping " + m + 
		//					" presents incompatibilities and is discarded");

		else
			if(verbose)
				System.out.println("Mapping " + m + 
					" could be added to the set of safe mappings");

		return rs;
	}


	/**
	 * Initialization method for input ontologies information
	 * @param knownOnto input ontology known by the agent
	 * @param hiddenOntoID ID of input ontology private to the other agent
	 * @param signatureHiddenOnto signature of input ontology private to the other agent
	 * @throws OWLOntologyCreationException thrown when it is not possible to create the fresh ontology
	 */
	private void init(OWLOntology knownOnto, OWLOntologyID hiddenOntoID, 
			Set<OWLEntity> signatureHiddenOnto) throws OWLOntologyCreationException{

		OWLOntologyManager manager = OntoUtil.getManager(false);

		this.knownOnto = knownOnto;
		hiddenOnto = manager.createOntology(hiddenOntoID);

		OntoUtil.addEntitiesDeclarationToOntology(
				hiddenOnto, manager, signatureHiddenOnto);

		if(verbose)
			System.out.println("Known ontology IRI: " + 
				OntoUtil.getIRIShortFragment(
						knownOnto.getOntologyID().getOntologyIRI()));
	}

	/**
	 * Method for checking the violations (and optionally repair them) of a set of mappings
	 * @param alignPath path to the rdf file storing the alignment in the AlignmentAPI format
	 * @param violKinds the notions of violations to be detected
	 * @param repairMethod the repair method to apply
	 * @return information about the status of the shared mappings considering the novel addition
	 * @throws Exception 
	 */
	public RepairStatus check(String alignPath, List<VIOL_KIND> violKinds, 
			REPAIR_METHOD repairMethod) throws Exception {

		this.repairMethod = repairMethod; 

		OWLOntology o1 = getOntology(true), o2 = getOntology(false);

		List<MappingObjectStr> mappings = loadMappings(o1, o2, alignPath);

		return check(mappings, violKinds, repairMethod);
	}

	private LogMap2_RepairFacility getConsistencyRepairFacility(
			Set<MappingObjectStr> actualMappings){
		return getConsistencyRepairFacility(
				actualMappings,Collections.<MappingObjectStr> emptySet());
	}

	private LogMap2_RepairFacility getConsistencyRepairFacility(
			Set<MappingObjectStr> actualMappings, 
			Set<MappingObjectStr> mappingsToKeep){
		return new LogMap2_RepairFacility(getOntology(true), getOntology(false), 
				actualMappings, true, false, mappingsToKeep, false);
	}

	/**
	 * Method for checking the violations (and optionally repair them) of a set of mappings
	 * @param mappings the alignment represented with the internal format used by LogMap
	 * @param violKinds the notions of violations to be detected
	 * @param repairMethod the repair method to apply 
	 * @return information about the status of the shared mappings considering the novel addition
	 * @throws Exception 
	 */
	public RepairStatus check(List<MappingObjectStr> mappings, 
			List<VIOL_KIND> violKinds, REPAIR_METHOD repairMethod) throws Exception {

		boolean incremental = true;

		this.repairMethod = repairMethod; 

		RepairStatus rs = null;
		OWLOntologyManager manager = OntoUtil.getManager(false);

		Set<MappingObjectStr> actualMappings = new HashSet<>();

		Iterator<MappingObjectStr> itr = mappings.iterator();
		MappingObjectStr m = null;

		boolean preRepair = false;

		int c = 0;

		while(itr.hasNext()){
			m = itr.next();
			c++;

			if(verbose){
				System.out.println("\nActual size of the set of safe mappings: " + 
					LogMapWrapper.countMappings(actualMappings));

				System.out.println("Testing mapping " + c + " out of " + 
					mappings.size() + ": " + m);
			}
			
			actualMappings.add(m);

			if(incremental){
				ConservativityRepairFacility repair = null;

				boolean checkingConsistency = violKinds.contains(VIOL_KIND.CONSISTENCY);

				if((checkingConsistency && violKinds.size() > 1) 
						|| (!checkingConsistency && !violKinds.isEmpty())){
					repair = new ConservativityRepairFacility(getOntology(true), 
							getOntology(false), manager, actualMappings, 
							preRepair, false, repairMethod);
				}
				
				LogMap2_RepairFacility repFac = null;

				if(checkingConsistency)
					repFac = getConsistencyRepairFacility(actualMappings);

				assessMapping(repFac, repair, actualMappings, m, preRepair, violKinds);
			}
			else
				assessMapping(manager, actualMappings, m, preRepair, 
						repairMethod, violKinds);
		}

		return rs;
	}

	public static void main(String[] args) throws Exception {

		Params.storeViolations = true;

		int testNum = 2;

		List<VIOL_KIND> violKinds = new ArrayList<>();
		violKinds.add(VIOL_KIND.EQONLY);
		//		violKinds.add(VIOL_KIND.APPROX);
		//		violKinds.add(VIOL_KIND.FULL);

		OWLOntologyManager manager = OntoUtil.getManager(false);

		String [] ontoPaths = new String[2];
		String alignPath = null;

		switch (testNum) {
		case 1:
			ontoPaths[0] = "/home/ale/data/oaei2013/conference/onto/cmt.owl"; 
			ontoPaths[1] = "/home/ale/data/oaei2013/conference/onto/confof.owl";
			alignPath = "/home/ale/data/oaei2013/conference/reference/cmt-confof.rdf";
			break;
		case 2:
			ontoPaths[0] = "/home/ale/data/oaei2013/anatomy/onto/mouse.owl"; 
			ontoPaths[1] = "/home/ale/data/oaei2013/anatomy/onto/human.owl";
			alignPath = "/home/ale/data/oaei2013/anatomy/reference/mouse-human.rdf";
			break;
		case 3:
			ontoPaths[0] = 
			"/home/ale/data/oaei2013/largebio/onto/oaei2013_FMA_small_overlapping_nci.owl"; 
			ontoPaths[1] = 
					"/home/ale/data/oaei2013/largebio/onto/oaei2013_NCI_small_overlapping_fma.owl";
			alignPath = 
					"/home/ale/data/oaei2013/largebio/reference/oaei2013_FMA2NCI_repaired_UMLS_mappings.rdf";
			break;
		case 4:
			ontoPaths[0] = "/home/ale/data/oaei2013/library/onto/stw.owl"; 
			ontoPaths[1] = "/home/ale/data/oaei2013/library/onto/thesoz.owl";
			alignPath = "/home/ale/data/oaei2013/library/reference/stw-thesoz.rdf";
			break;
		case 5:
			ontoPaths[0] = 
			"/home/ale/data/oaei2013/largebio/onto/oaei2013_SNOMED_extended_overlapping_fma_nci.owl";
			ontoPaths[1] = 
					"/home/ale/data/oaei2013/largebio/onto/oaei2013_NCI_whole_ontology.owl"; 
			alignPath = 
					"/home/ale/data/oaei2013/largebio/reference/oaei2013_SNOMED2NCI_repaired_UMLS_mappings.rdf";
			break;
		case 6:
			ontoPaths[0] = "/home/ale/data/oaei2013/conference/onto/confof.owl"; 
			ontoPaths[1] = "/home/ale/data/oaei2013/conference/onto/ekaw.owl";
			alignPath = "/home/ale/data/oaei2013/conference/reference/confof-ekaw.rdf";
			break;
		default:
			ontoPaths[0] = "/home/ale/data/oaei2013/conference/onto/conference.owl"; 
			ontoPaths[1] = "/home/ale/data/oaei2013/conference/onto/sigkdd.owl";
			alignPath = "/home/ale/data/oaei2013/conference/reference/conference-sigkdd.rdf";
			break;
		}

		OWLOntology o1 = OntoUtil.load(ontoPaths[0], true, manager);
		OWLOntology o2 = OntoUtil.load(ontoPaths[1], true, manager);

		boolean fstOnto = true;
		boolean repairAlso = true;

		MASRepair masRepair = new MASRepair(fstOnto ? o1 : o2, 
				fstOnto ? o2 : o1, repairAlso, fstOnto);

		List<MappingObjectStr> mappings = masRepair.loadMappings(alignPath);

		//		masRepair.check(alignPath, violKinds, REPAIR_METHOD.SUB);
		masRepair.check(mappings, violKinds, REPAIR_METHOD.SUB);

		OntoUtil.unloadAllOntologies();
	}

	public OWLOntology getKnownOntology() {
		return knownOnto;
	}

	public OWLOntology getHiddenOntology() {
		return hiddenOnto;
	}
}
