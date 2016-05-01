/*******************************************************************************
 * Copyright 2012 by the Department of Computer Science (University of Oxford)
 * 
 *    This file is part of LogMap.
 * 
 *    LogMap is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 * 
 *    LogMap is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 * 
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with LogMap.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package uk.ac.ox.krr.logmap2.indexing;

import java.util.ArrayList;

import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Queue;
import java.util.Set;
import java.util.Map.Entry;

import org.semanticweb.owlapi.model.OWLClass;
import uk.ac.ox.krr.logmap2.indexing.entities.ClassIndex;
import uk.ac.ox.krr.logmap2.indexing.labelling_schema.Interval;
import uk.ac.ox.krr.logmap2.indexing.labelling_schema.IntervalLabelledHierarchy;
import uk.ac.ox.krr.logmap2.indexing.labelling_schema.PreIntervalLabelledHierarchy;
import uk.ac.ox.krr.logmap2.io.LogOutput;


/**
 * This class will gather the indexes of both ontologies
 * 
 *
 * @author Ernesto Jimenez-Ruiz
 * Created: Sep 12, 2011
 *
 */
public class JointIndexManager extends IndexManager {

	private int addedDisj;
	private boolean countDisj;
	private boolean dirty = false;
	Set<Integer> disjAdditionBuffer = new HashSet<>();
	Set<Integer> disjRemovalBuffer = new HashSet<>();

	boolean oldDisjComputation = false;

	public JointIndexManager(){
		super();
	}

	public void countingDisj(boolean countDisj){
		this.countDisj = countDisj;
	}

	public int getAddedDisj(){
		return addedDisj;
	}
	
	public boolean isLeaf(int i){
		if(!identifier2ClassIndex.containsKey(i))
			return false;
		
		ClassIndex ci = identifier2ClassIndex.get(i);
		
		return !ci.hasDirectSubClasses() 
					|| ci.getDirectSubclasses().isEmpty();
	}
	
	public Set<Integer> getLeaves(){
		Set<Integer> leaves = new HashSet<>();
		for (Integer i : getClassIdentifierSet())
			if(isLeaf(i))
				leaves.add(i);
		
		return leaves;
	}
	
	private boolean isInSameOntology(int i, OntologyProcessing origProc){
		return origProc == null || 
				origProc.getIdentifier4ConceptIRI(
						this.getIRIStr4ConceptIndex(i)) != -1;
	}

	public Set<Integer> getAllEquivalentClasses(int i, 
			OntologyProcessing origProc){
		
		Set<Integer> eq = new HashSet<>();
		Queue<Integer> q = new LinkedList<>();
		Set<Integer> visited = new HashSet<>();
		Integer actualId;

		if(getBottomClasses().contains(i))
			return eq;

		q.add(i);
		
		while(!q.isEmpty()){
			actualId = q.poll();
			if(visited.contains(actualId))
				continue;

			if(getBottomClasses().contains(actualId))
				continue;
			
			visited.add(actualId);

			ClassIndex classIndex = getClassIndex(actualId);
			
			if(isInSameOntology(actualId, origProc))
				eq.add(actualId);
			
			if(classIndex.hasEquivalentClasses()){
				Set<Integer> eqClasses = classIndex.getEquivalentClasses();
				eqClasses.remove(actualId);
				
				for (Integer eqId : eqClasses)
					q.add(eqId);					
			}
		}
		return eq;
	}
	
	public Set<Integer> getSubEquivalentClasses(int i, 
			OntologyProcessing origProc, boolean direct){
		Set<Integer> subEq = new HashSet<>();
		Queue<Integer> q = new LinkedList<>();
		Set<Integer> visited = new HashSet<>();
		Integer actualId;
		
		if(getBottomClasses().contains(i))
			return subEq;
		
		boolean print = false;
		
//		if(getIRIStr4ConceptIndex(i).equals("http://conference#Paper"))
//			print = true;
		
//		if(print && direct)
//			System.out.print("");
		
//		q.add(i);
				
		Set<Integer> equivTop = new HashSet<>();
		equivTop.add(i);	
		if(getClassIndex(i).hasEquivalentClasses())
			for (Integer eqIdTop : getClassIndex(i).getEquivalentClasses())
				if(eqIdTop != i)
					equivTop.add(eqIdTop);
		
		q.addAll(equivTop);

		while(!q.isEmpty()){
			actualId = q.poll();
			
			if(!direct){				
				if(visited.contains(actualId))
					continue;
				visited.add(actualId);
			}
			
			if(getBottomClasses().contains(actualId))
				continue;

			ClassIndex classIndex = getClassIndex(actualId);
						
//			if(print)
//				System.out.println("Actual: " + getIRIStr4ConceptIndex(actualId));
//			if(getIRIStr4ConceptIndex(actualId).equals(
//					"http://conference#Regular_contribution"))
//				System.out.println();
			
			boolean isEquivTop = equivTop.contains(actualId);
			boolean sameOnto = isInSameOntology(actualId, origProc); 
					
			if(sameOnto){
//				if(print)
//					System.out.println("ADDED: " + getIRIStr4ConceptIndex(actualId));
				subEq.add(actualId);
			}
			
			// if the class has an equivalent class in the ontology we are seeking violations, 
			// all its subclasses are not direct violation (they are subclasses of 
			// the equivalent one)
			boolean addAllSub = false;
			if(!isEquivTop && classIndex.hasEquivalentClasses()){
				Set<Integer> eqClasses = classIndex.getEquivalentClasses();
				eqClasses.remove(actualId);
				Set<Integer> tmpForSub = new HashSet<>();
				
				for (Integer eqSubId : eqClasses) {
//					if(print)
//						System.out.println("Eq-actual: " + getIRIStr4ConceptIndex(eqSubId));
					
					if(isInSameOntology(eqSubId, origProc)){						
						subEq.add(eqSubId);
//						if(print)
//							System.out.println("ADDED: " + getIRIStr4ConceptIndex(eqSubId));
						
						if(!direct)
							addSubClassesToQueue(q, actualId, eqSubId, print);
						else
							tmpForSub.add(eqSubId);
					}
					else {
						if(direct)
							addAllSub = true;
						addSubClassesToQueue(q, actualId, eqSubId, print);
					}
				}
				if(!sameOnto || addAllSub)
					for (Integer tmpId : tmpForSub)
						addSubClassesToQueue(q, actualId, tmpId, print);				
			}
			
			if(!(direct && sameOnto) || isEquivTop || addAllSub)
				addSubClassesToQueue(q,actualId,actualId,print);
		}

		subEq.remove(i);
		subEq.removeAll(getBottomClasses());
		
//		Set<Integer> filtered = new HashSet<>(subEq); 
//		
//		for (Integer j : subEq) {
//			ClassIndex ci = getClassIndex(j);
//			Set<Integer> supJ = new HashSet<>();
//			
//			if(ci.hasEquivalentClasses())
//				for (Integer eqJ : ci.getEquivalentClasses())
//					if(getClassIndex(eqJ).hasDirectSuperClasses())
//						supJ.addAll(getClassIndex(eqJ).getDirectSuperclasses());
//
//			if(ci.hasDirectSuperClasses())
//				supJ.addAll(ci.getDirectSuperclasses());
//			
//			filtered.removeAll(supJ);
//		}
//
//		return filtered;
		
		return subEq;
	}
	
	private void addSubClassesToQueue(Queue<Integer> q, int actualId, int i, boolean print){
		ClassIndex classIndex = getClassIndex(i);
		if(classIndex.hasDirectSubClasses()){
			Set<Integer> subIds = classIndex.getDirectSubclasses();
			subIds.remove(actualId);
			for (Integer subId : subIds) {
//				if(print)
//					System.out.println("Sub-actual: " + getIRIStr4ConceptIndex(subId));
				q.add(subId);
			}
		}
	}

	// copy constructor (not advisable to use clone())
	public JointIndexManager(JointIndexManager index){
		super((IndexManager) index);
		allSubClasses = new HashSet<>(index.allSubClasses);
		allSuperClasses = new HashSet<>(index.allSuperClasses);
		addedDisj = index.addedDisj;
		countDisj = index.countDisj;

		disjAdditionBuffer.addAll(index.disjAdditionBuffer);
		disjRemovalBuffer.addAll(index.disjRemovalBuffer);

		dirty = index.dirty;

		//TODO: to be removed
		//		if(index.sanityCheck() && !sanityCheck())
		//			throw new Error("UNSAFE COPY CONSTRUCTOR");
	}

	//	@Override
	//	public int hashCode() {
	//		final int prime = 31;
	//		int result = super.hashCode();
	//		result = prime * result
	//				+ ((allSubClasses == null) ? 0 : allSubClasses.hashCode());
	//		result = prime * result
	//				+ ((allSuperClasses == null) ? 0 : allSuperClasses.hashCode());
	//		return result;
	//	}
	//
	//	@Override
	//	public boolean equals(Object obj) {
	//		if (this == obj)
	//			return true;
	//		if (!super.equals(obj))
	//			return false;
	//		if (getClass() != obj.getClass())
	//			return false;
	//		JointIndexManager other = (JointIndexManager) obj;
	//		if (allSubClasses == null) {
	//			if (other.allSubClasses != null)
	//				return false;
	//		} else if (!allSubClasses.equals(other.allSubClasses))
	//			return false;
	//		if (allSuperClasses == null) {
	//			if (other.allSuperClasses != null)
	//				return false;
	//		} else if (!allSuperClasses.equals(other.allSuperClasses))
	//			return false;
	//		return true;
	//	}

	/**
	 * Scope to know if the entities in a mppings share scopes, or have somethin in common
	 * @param ide
	 * @return
	 */
	public Set<Integer> getScope4Identifier_Big(int ide){
		//We extract a big scope, still with a limit
		return getScope4Identifier(ide, 10, 10, 1000);
	}


	/**
	 * Scope to discover new mappings
	 * @param ide
	 * @return
	 */
	public Set<Integer> getScope4Identifier_Condifence(int ide){
		//TODO Store scope?? Better not...
		//3 levels of subclasses and 10 of superclasses
		return getScope4Identifier(ide, 3, 10, 50);
	}

	/**
	 * Scope to extract confidence in mappings
	 * @param ide
	 * @return
	 */
	public Set<Integer> getScope4Identifier_Expansion(int ide){
		return getScope4Identifier(ide, 2, 2, 50);
	}

	public Set<Integer> getSubsetOfSuperClasses4Identifier(int ide){
		return getScope4Identifier(ide, 0, 3, 10);
	}

	public Set<Integer> getSuperClasses4Identifier(int ide){
		return getScope4Identifier(ide, 0,  Integer.MAX_VALUE,  Integer.MAX_VALUE);
	}

	public Set<Integer> getSubsetOfSubClasses4Identifier(int ide){
		return getScope4Identifier(ide, 3, 0, 10);
	}

	public Set<Integer> getSubClasses4Identifier(int ide){
		return getScope4Identifier(ide, Integer.MAX_VALUE, 0, Integer.MAX_VALUE);
	}

	public Set<Integer> getScope4Identifier(int ide, int sub_levels, int super_levels, int max_size_subclasses){

		Set<Integer> scope = new HashSet<Integer>();

		allSuperClasses.clear();
		allSubClasses.clear();

		if (sub_levels>0){
			getSubclasses4Identifiers(getDirectSubClasses4Identifier(ide, false), sub_levels, max_size_subclasses);
			scope.addAll(allSubClasses);
		}

		if (super_levels>0){
			getSuperclasses4Identifiers(getDirectSuperClasses4Identifier(ide, false), super_levels);
			scope.addAll(allSuperClasses);
		}		
		//ide is not in scope

		return scope;

	}


	Set<Integer> allSuperClasses = new HashSet<Integer>();
	Set<Integer> allSubClasses = new HashSet<Integer>();

	private void getSubclasses4Identifiers(Set<Integer> classes, int level, int max_size_subclasses){

		allSubClasses.addAll(classes);

		if (level<1 || classes.size()<1 || allSubClasses.size() > max_size_subclasses) //exit condition
			return;

		Set<Integer> subClasses = new HashSet<Integer>();

		for (int ide : classes){
			subClasses.addAll(getDirectSubClasses4Identifier(ide, false));
		}


		getSubclasses4Identifiers(subClasses, level-1, max_size_subclasses);


	}


	private void getSuperclasses4Identifiers(Set<Integer> classes, int level){

		allSuperClasses.addAll(classes);

		if (level<1 || classes.size()<1) //exit condition
			return;

		Set<Integer> superClasses = new HashSet<Integer>();

		for (int ide : classes){
			superClasses.addAll(getDirectSuperClasses4Identifier(ide, false));
		}


		getSuperclasses4Identifiers(superClasses, level-1);


	}


	/**
	 * We set the small projection, that is we create a set of identifiers to consider (module)
	 * Note that currently all identifiers for all ontologies are treated from same structure, that is
	 * we have only one index
	 * @param mapped_entities_identifiers
	 */
	public void setSmallProjection4MappedEntities(Set<Integer> mapped_entities_identifiers){

		//Set<Integer> entities = new HashSet<Integer>(mapped_entities_identifiers);
		identifiersInModule.clear();
		identifiersInModule.addAll(mapped_entities_identifiers);

		/*Set<Integer> superClasses = new HashSet<Integer>();

		for (int ide : mapped_entities_identifiers){
			superClasses.addAll(getDirectSuperClasses4Identifier(ide, false));
		}*/


		//We get all superclasses
		getSuperclasses4Identifiers(mapped_entities_identifiers, 500);//We want to extract everything

		identifiersInModule.addAll(allSuperClasses);

		LogOutput.print("Size projection: " + identifiersInModule.size());

	}





	private void duplicateDirectSubClasses(){

		ident2DirectSubClasses_integration = new HashMap<Integer, Set<Integer>>();

		for (int parent:  getDirectSubClasses(false).keySet()){

			if (!ident2DirectSubClasses_integration.containsKey(parent)){
				ident2DirectSubClasses_integration.put(parent, new HashSet<Integer>());
			}

			for (int kid : getDirectSubClasses(false).get(parent)){
				ident2DirectSubClasses_integration.get(parent).add(kid);
			}

		}

	}



	/**
	 * 
	 * Returns the adapted map iden2direct subclasses considering the given mappings.
	 * We want to consider an integrated hierarchy
	 */
	private void setAdaptedMap4DirectSubclasses(Map<Integer, Set<Integer>> exact_mappings){//we have only exact mappings and equiv!

		//ident2DirectSubClasses_integration = new HashMap<Integer, Set<Integer>>(getDirectSubClasses(false));
		//TODO we need to create a completely new object. Sets in map are not duplicated
		duplicateDirectSubClasses();


		representativesFromMappings = new HashSet<Integer>();

		LogOutput.print("Original entries DirectSubclasses: " + ident2DirectSubClasses_integration.size());
		LogOutput.print("Original entries DirectSubclasses: " + getDirectSubClasses(false).size());


		for (int ide_rep : exact_mappings.keySet()){
			for (int ide_equiv : exact_mappings.get(ide_rep)){

				if (ide_rep > ide_equiv){ //we only consider one of the sides (Note that there are only exact mappings for indexing)
					break;
				}


				/*if (!ident2DirectSubClasses_integration.containsKey(ide_equiv) || 
						ident2DirectSubClasses_integration.get(ide_equiv).size()<1){
					continue;//In case there is nothing to add
				}*/


				//Keep representatives
				representativesFromMappings.add(ide_rep);

				//LogOutput.print("Size before: " + ident2DirectSubClasses_integration.get(ide_rep).size());
				//LogOutput.print("Size equiv: " + ident2DirectSubClasses_integration.get(ide_equiv).size());


				//Deal with parents
				for (int ide_parent : getDirectSuperClasses4Identifier(ide_equiv, false)){
					if (ident2DirectSubClasses_integration.containsKey(ide_parent)){//just in case
						ident2DirectSubClasses_integration.get(ide_parent).add(ide_rep);
						ident2DirectSubClasses_integration.get(ide_parent).remove(ide_equiv);
					}

				}


				//Deal with kids
				if (ident2DirectSubClasses_integration.containsKey(ide_equiv)){

					if (!ident2DirectSubClasses_integration.containsKey(ide_rep)){
						ident2DirectSubClasses_integration.put(ide_rep, new HashSet<Integer>());
					}

					/*if (ident2DirectSubClasses_integration.get(ide_equiv).size()>0){
						LogOutput.print("Original entries DirectSubclasses new: " + ident2DirectSubClasses_integration.get(ide_rep).size());
						LogOutput.print("Original entries DirectSubclasses base: " + getDirectSubClasses(false).get(ide_rep).size());
					}*/

					//We add all direc subclasses of equivalent class to representative
					ident2DirectSubClasses_integration.get(ide_rep).addAll(ident2DirectSubClasses_integration.get(ide_equiv));

					/*if (ident2DirectSubClasses_integration.get(ide_equiv).size()>0){
						LogOutput.print("Adapted entries DirectSubclasses new: " + ident2DirectSubClasses_integration.get(ide_rep).size());
						LogOutput.print("Adapted entries DirectSubclasses base: " + getDirectSubClasses(false).get(ide_rep).size());
					}*/
					//We remove occurrence of equivalente (we only want to consider representatives)
					ident2DirectSubClasses_integration.get(ide_equiv).clear();
					ident2DirectSubClasses_integration.remove(ide_equiv);
				}

				//LogOutput.print("Size after: " + ident2DirectSubClasses_integration.get(ide_rep).size());


			}
		}

		LogOutput.print("Adapted entries DirectSubclasses: " + ident2DirectSubClasses_integration.size());
		LogOutput.print("Adapted entries DirectSubclasses: " + getDirectSubClasses(false).size());

		LogOutput.print("Representatives from Mappings: " + representativesFromMappings.size());
		LogOutput.print("Mapping entries (sub mappings): " + exact_mappings.keySet().size());

	}







	/**
	 * This method will set up the interval labelling index. To the end it will adapt the 
	 * ident2directkids structure in order to take into account equivalence mappings
	 *  
	 */
	public void setIntervalLabellingIndex(Map<Integer, Set<Integer>> exact_mappings) {

		boolean fixCycles = true;

		//TODO we need to create new identifier2directkids with both ontologies + anchors + representatives
		setAdaptedMap4DirectSubclasses(exact_mappings);

		// Alessandro: 22 April 2014
		HashMap<Integer, Set<Integer>> ontoHierarchy = null;
		Set<Set<Integer>> nonTrivialSCCs = new HashSet<>();

		if(fixCycles){
			// they are not compatible
			IntervalLabelledHierarchy.fixCycles = false;

			ontoHierarchy = getIdent2DirectSubClasses_Integration();
			Map<Integer,Set<Integer>> sccs = 
					new LightTarjan().executeTarjan(ontoHierarchy);

			for (Set<Integer> scc : sccs.values()) {
				if(scc.size() > 1){
					nonTrivialSCCs.add(scc);
					
//					System.out.println("SCC: " + scc);
					// choose a representing node
					Integer represId = scc.iterator().next();
					Set<Integer> sccMinusRepr = new HashSet<>(scc);
					sccMinusRepr.remove(represId);
					
//					System.out.println("ReprNode: " + represId);
//					System.out.println("Repr: " + ontoHierarchy.get(represId));

					// OUT-ARCS: preserve nodes different from repr and reachable from scc
					for (Integer id : sccMinusRepr){
						ontoHierarchy.get(id).removeAll(scc);
						ontoHierarchy.get(represId).addAll(ontoHierarchy.get(id));
//						System.out.println(id + ": " + ontoHierarchy.get(id));
					}

					// the other nodes must disappear
					ontoHierarchy.get(represId).removeAll(sccMinusRepr);

//					System.out.println("Repr: " + ontoHierarchy.get(represId));
					
					// IN-ARCS: for each node not in the scc remove the other  
					// nodes from the children and add repr
					for (Entry<Integer, Set<Integer>> e : ontoHierarchy.entrySet()) {
						if(!scc.contains(e.getKey())){
							int pre = e.getValue().size();
							e.getValue().removeAll(sccMinusRepr);
							
							if(e.getValue().size() != pre){
								e.getValue().add(represId);
//								System.out.println(e.getKey() + " removed " + 
//										(pre - e.getValue().size()));
//								System.out.println(e.getKey() + " " + e.getValue());
							}
						}
					}					
				}
			}
			if(!nonTrivialSCCs.isEmpty()){
				System.out.println("Fixed " + nonTrivialSCCs.size() + " cycle(s)");
				// check if no more cycles exists
				sccs = new LightTarjan().executeTarjan(ontoHierarchy);
				int unsolved = 0;
				for (Set<Integer> scc : sccs.values())
					if(scc.size() > 1){
						++unsolved;
						for (Integer i : scc)
							System.out.println(i + ": " + ontoHierarchy.get(i));
					}
				if(unsolved > 0)
					throw new Error(unsolved + " unsolved SCCs");
			}
		}
		else
			IntervalLabelledHierarchy.fixCycles = true;

		//Create interval labelling
		IntervalLabelledHierarchy interval_schema = 
				new PreIntervalLabelledHierarchy(ontoHierarchy == null ? 
						getIdent2DirectSubClasses_Integration() : ontoHierarchy);

		//Structures
		for (int ident : interval_schema.getClassesToNodesMap().keySet()){ 

			identifier2ClassIndex.get(ident).setNode(interval_schema.getClassesToNodesMap().get(ident));

			//Uncomment for unsatisfiability tests (see disjoint intervals)
			//preOrderDesc2Identifier.put(interval_schema.getClassesToNodesMap().get(ident).getDescOrder(), ident);

			//Not used...
			//preOrderAnc2Identifier.put(interval_schema.getClassesToNodesMap().get(ident).getAscOrder(), ident);
		}

		// set as equivalent all the elements in the nontrivial SCCs
		for (Set<Integer> scc : nonTrivialSCCs) {
			for (Integer i : scc) {
				Set<Integer> sccMinusSelf = new HashSet<>(scc);
				sccMinusSelf.remove(i);
				if(identifier2ClassIndex.get(i).getEquivalentClasses() == null)
					identifier2ClassIndex.get(i).setEquivalentClasses(new HashSet<>(sccMinusSelf));
				else
					identifier2ClassIndex.get(i).getEquivalentClasses().addAll(sccMinusSelf);
			}
		}

		//Propagate preoreder (anc and desc) to equivalences equivalences
		//PROPAGATION of labels for EQUIVALENCES
		for (int iRep : getRepresentativeNodes()){
			if (identifier2ClassIndex.get(iRep).hasEquivalentClasses()){			
				for (int iEquiv : identifier2ClassIndex.get(iRep).getEquivalentClasses()){
					identifier2ClassIndex.get(iEquiv).setNode(identifier2ClassIndex.get(iRep).getNode());
				}
			}
		}

		//Propagation to equivalent entities from mappings
		for (int iRep : getRepresentativesFromMappings()){

			if (exact_mappings.containsKey(iRep)){//Just in case...
				for (int iEquiv : exact_mappings.get(iRep)){
					identifier2ClassIndex.get(iEquiv).setNode(identifier2ClassIndex.get(iRep).getNode());

					//LogOutput.print("REP: " + identifier2ClassIndex.get(iEquiv).getNode().getDescIntervals().toString());
					//LogOutput.print("\tEQUIV: " + identifier2ClassIndex.get(iEquiv).getNode().getDescIntervals().toString());

				}
			}
		}


		//Create disjoint intervals
		//setDisjointIntervals();
		createDisjointIntervalsStructure();

		//Create independent roots
		//createIdependentRootsStructure();

		//Fast check
		checkBasicSatisfiability();
	}


	public boolean isDirty(){
		//return true;
		return dirty;
		//return !disjAdditionBuffer.isEmpty() || !disjRemovalBuffer.isEmpty();
	}

	private void createDisjointIntervalsStructureDeletion(int i1, int i2){
		int wrong_preorder=0;
		int wrong_desc_intervals=0;

		List<Interval> list_intervals = new ArrayList<Interval>();

		//Check for classes with preorder -1
		if (identifier2ClassIndex.get(i1).getNode().getDescOrder()<0){
			wrong_preorder++;
		}

		//Check if a class with good preorder has a wrong descendant interval
		for (Interval cls_interval : identifier2ClassIndex.get(i1).getNode().getDescIntervals()){
			if (cls_interval.getLeftBound()<0 || cls_interval.getRightBound()<0){
				wrong_desc_intervals++;
			}
		}

		for (Interval disjcls_interval : identifier2ClassIndex.get(i2).getNode().getDescIntervals()){
			//Only correct intervals
			//Do not add negative intervals <-1,-1>  or <-id, -id>
			if (disjcls_interval.getLeftBound()>=0 && disjcls_interval.getRightBound()>=0){
				list_intervals.add(disjcls_interval);
			}
		}

		for (Interval cls_interval : identifier2ClassIndex.get(i1).getNode().getDescIntervals()){
			if(interval2disjointIntervals.containsKey(cls_interval)){
				Set<Interval> newSet = new HashSet<>();
				for (Interval interval : interval2disjointIntervals.get(cls_interval)) {
					boolean toRemove = false;
					for (Interval intDel : list_intervals) {
						if(intDel.hasNonEmptyIntersectionWith(interval)){
							toRemove = true;
							newSet.addAll(interval.removeInterval(intDel));
						}
					}
					if(!toRemove)
						newSet.add(interval);
				}

				interval2disjointIntervals.put(cls_interval, newSet);
			}
		}
	}

	/**
	 * Creates disjoint intervals structure.
	 * Note that adjacent intervals are merged
	 */
	private void createDisjointIntervalsStructure(Set<Integer> ids){

		//ORIGINAL no MERGED
		/*init = Calendar.getInstance().getTimeInMillis();
		for (int icls : Class2DisjointClasses.keySet()){
			for (Interval cls_interval : Identifier2DescIntervals.get(icls)){
				interval2disjointness.put(cls_interval, new ArrayList<Interval>());

				for (int disjcls : Class2DisjointClasses.get(icls)){
					for (Interval disjcls_interval : Identifier2DescIntervals.get(disjcls)){
						interval2disjointness.get(cls_interval).add(disjcls_interval);
					}
				}
			}
		}

		LogOutput.print(interval2disjointness);
		fin = Calendar.getInstance().getTimeInMillis();
		LogOutput.print("Time Getting Disjoint Intervals (s): " + (float)((double)fin-(double)init)/1000.0);
		 */

		//		if(ids == null || ids.isEmpty())
		//			ident2disjointclasses.clear();
		//		else
		//			for (Integer id : ids)
		//				ident2disjointclasses.remove(id);

		long init1, fin1;
		init1 = Calendar.getInstance().getTimeInMillis();
		List<Interval> list_intervals = new ArrayList<Interval>();
		Interval[] array_intervals;
		Interval current_interval; 

		int wrong_preorder=0;
		int wrong_desc_intervals=0;

		//		if(ids == null){
		//			interval2disjointIntervals.clear();
		//			ids = new HashSet<>(identifier2ClassIndex.keySet());
		//		}

		interval2disjointIntervals.clear();

		for (int icls : identifier2ClassIndex.keySet()){

			//Check for classes with preorder -1
			if (identifier2ClassIndex.get(icls).getNode().getDescOrder()<0){
				//LogOutput.print("Class with wrong preorder: " + icls + " - " + getName4ConceptIndex(icls) +  "  "  + identifier2ClassIndex.get(icls).getNode().getDescOrder());
				//LogOutput.print("\t Intervals: " + identifier2ClassIndex.get(icls).getNode().getDescIntervals());
				wrong_preorder++;
				continue; //Do not add to disjoint intervals
			}

			//Check if a class with good preorder has a wrong descendant interval
			for (Interval cls_interval : identifier2ClassIndex.get(icls).getNode().getDescIntervals()){
				if (cls_interval.getLeftBound()<0 || cls_interval.getRightBound()<0){
					//LogOutput.print("Class with wrong interval descendant: " + icls + " - " + getName4ConceptIndex(icls));
					//LogOutput.print("\t Intervals: " + identifier2ClassIndex.get(icls).getNode().getDescIntervals());
					wrong_desc_intervals++;
				}
			}

			if(!oldDisjComputation){

				if (identifier2ClassIndex.get(icls).hasDirectDisjointClasses()){

					//				if(icls == 28)
					//					System.out.println();

					//We create list of disjoint intervals
					for (int disjcls : identifier2ClassIndex.get(icls).getDisjointClasses()){
						for (Interval disjcls_interval : identifier2ClassIndex.get(disjcls).getNode().getDescIntervals()){

							//Only correct intervals
							//Do not add negative intervals <-1,-1>  or <-id, -id>
							if (disjcls_interval.getLeftBound()>=0 && disjcls_interval.getRightBound()>=0){
								list_intervals.add(disjcls_interval);
							}
							//							else
							//								System.err.println("Wrong interval " + disjcls_interval);
						}
					}

					if (list_intervals.size()>=3){

						//First Sort
						array_intervals = new Interval[list_intervals.size()];
						array_intervals = list_intervals.toArray(array_intervals);

						_Quicksort(array_intervals, 0, array_intervals.length-1);


						//Merge

						list_intervals.clear(); //we already have our sorted array

						current_interval=array_intervals[0];

						for (int i=1; i< array_intervals.length; i++){

							if (current_interval.isAdjacentTo(array_intervals[i])){
								current_interval = current_interval.getUnionWith(array_intervals[i]);
							}
							else {
								list_intervals.add(current_interval);
								current_interval=array_intervals[i];
							}														
						}
						list_intervals.add(current_interval);

						for (Interval cls_interval :  identifier2ClassIndex.get(icls).getNode().getDescIntervals()){
							List<Interval> tmpList = new LinkedList<>(list_intervals);

							if(interval2disjointIntervals.containsKey(cls_interval)){
								tmpList.addAll(interval2disjointIntervals.get(cls_interval));
								tmpList = mergeIntervals(tmpList);
								//continue;
							}							
							interval2disjointIntervals.put(cls_interval, 
									new HashSet<Interval>(tmpList));
						}
					}

					else if (list_intervals.size()==2){
						List<Interval> originalList = list_intervals.get(0).getUnionWithList(list_intervals.get(1));
						for (Interval cls_interval : identifier2ClassIndex.get(icls).getNode().getDescIntervals()){
							List<Interval> tmpList = new LinkedList<>(originalList);
							if(interval2disjointIntervals.containsKey(cls_interval)){
								tmpList.addAll(interval2disjointIntervals.get(cls_interval));
								tmpList = mergeIntervals(tmpList);
								//continue;
							}							
							interval2disjointIntervals.put(cls_interval, 
									new HashSet<Interval>(tmpList));
						}
					}

					else if (list_intervals.size()==1){ //Only one
						for (Interval cls_interval : identifier2ClassIndex.get(icls).getNode().getDescIntervals()){

							List<Interval> originalList = new LinkedList<>(list_intervals);

							if(interval2disjointIntervals.containsKey(cls_interval)){
								originalList.addAll(interval2disjointIntervals.get(cls_interval));
								originalList = mergeIntervals(originalList);
								//continue;
							}							
							interval2disjointIntervals.put(cls_interval, 
									new HashSet<Interval>(originalList));
						}
					}
					//else if list_intervals.size()==o do nothing


					//Empty structure
					list_intervals.clear();
				}
			}
			else {
				/* ORIGINAL */
				if (identifier2ClassIndex.get(icls).hasDirectDisjointClasses()){

					//We create list of disjoint intervals
					for (int disjcls : identifier2ClassIndex.get(icls).getDisjointClasses()){
						for (Interval disjcls_interval : identifier2ClassIndex.get(disjcls).getNode().getDescIntervals()){
							list_intervals.add(disjcls_interval);
						}
					}

					if (list_intervals.size()>=3){

						//First Sort
						array_intervals = new Interval[list_intervals.size()];
						array_intervals = list_intervals.toArray(array_intervals);

						_Quicksort(array_intervals, 0, array_intervals.length-1);


						//Merge

						list_intervals.clear(); //we already have our sorted array

						current_interval=array_intervals[0];

						for (int i=1; i< array_intervals.length; i++){

							if (current_interval.isAdjacentTo(array_intervals[i])){
								current_interval = current_interval.getUnionWith(array_intervals[i]);
							}
							else {
								list_intervals.add(current_interval);
								current_interval=array_intervals[i];
							}                                                                                                               
						}
						list_intervals.add(current_interval);


						for (Interval cls_interval :  identifier2ClassIndex.get(icls).getNode().getDescIntervals()){
							interval2disjointIntervals.put(cls_interval, new HashSet<Interval>(list_intervals));
						}
					}

					else if (list_intervals.size()==2){

						for (Interval cls_interval : identifier2ClassIndex.get(icls).getNode().getDescIntervals()){
							interval2disjointIntervals.put(
									cls_interval, 
									new HashSet<Interval>(list_intervals.get(0).getUnionWithList(list_intervals.get(1)))
									);
						}
					}

					else { //Only one
						for (Interval cls_interval : identifier2ClassIndex.get(icls).getNode().getDescIntervals()){
							interval2disjointIntervals.put(cls_interval, new HashSet<Interval>(list_intervals));
						}
					}

					//Empty structure
					list_intervals.clear();
				}
			}
		}

		LogOutput.print("Classes with wrong/negative preorder (-1 or -d): " + wrong_preorder);
		LogOutput.print("Classes with wrong/negative descendants intervals (<-1,-1> or <-id,-id>): " + wrong_desc_intervals);

		//LogOutput.print(interval2disjointIntervals.toString());

		//		if(!prev.equals(interval2disjointIntervals))
		//			System.out.println("AGGIORNATE DISJ");

		fin1 = Calendar.getInstance().getTimeInMillis();
		LogOutput.print("Time Getting Disjoint Intervals (merged) (s): " + (float)((double)fin1-(double)init1)/1000.0);
	}

	public boolean sanityCheck(){

		boolean safe = true;
		int countDisj = 0, countSub = 0, countSup = 0;

		checkPreOrders();

		for (int i : identifier2ClassIndex.keySet()) {
			ClassIndex ci = getClassIndex(i);
			OWLClass c = getOWLClass4ConceptIndex(i);

			if(ci.hasDirectDisjointClasses()){
				for (Integer d : ci.getDisjointClasses()) {
					if(!areDisjoint(i, d)){
						++countDisj;
						System.err.println(getOWLClass4ConceptIndex(i) + " has " 
								+ getOWLClass4ConceptIndex(d) 
								+ " as disjoint class but intervals don't match");
						safe = false;
					}
				}
			}
			if(ci.hasDirectSubClasses()){
				for (Integer d : ci.getDirectSubclasses()) {
					if(!isSubClassOf(d, i)){
						System.err.println(getOWLClass4ConceptIndex(d) 
								+ " is a " + getOWLClass4ConceptIndex(i) 
								+ " but subclass intervals check intervals fails");
						safe = false;
						countSub++;
					}

					if(!isSuperClassOf(i, d)){
						System.err.println(getOWLClass4ConceptIndex(d) 
								+ " is a " + getOWLClass4ConceptIndex(i) 
								+ " but superclass intervals check intervals fails");
						safe = false;
						countSub++;
					}
				}
			}

			if(ci.hasDirectSuperClasses()){
				for (Integer d : ci.getDirectSuperclasses()) {
					if(!isSuperClassOf(d, i)){
						System.err.println(getOWLClass4ConceptIndex(i) 
								+ " is a " + getOWLClass4ConceptIndex(d) 
								+ " but superclass intervals check intervals fails");
						safe = false;
						countSup++;
					}

					if(!isSubClassOf(i, d)){
						System.err.println(getOWLClass4ConceptIndex(i) 
								+ " is a " + getOWLClass4ConceptIndex(d) 
								+ " but subclass intervals check intervals fails");
						safe = false;
						countSup++;
					}
				}
			}
		}

		if(countDisj > 0)
			System.err.println(countDisj + " broken disj");
		if(countSub > 0)
			System.err.println(countSub + " broken subsumptions");
		if(countSup > 0)
			System.err.println(countSup + " broken (reverse) subsumptions");
		return safe;
	}

	/**
	 * Creates disjoint intervals structure.
	 * Note that adjacent intervals are merged
	 */
	private void createDisjointIntervalsStructure(){
		createDisjointIntervalsStructure(null);
	}

	public void mergeDisjointnessAxioms(JointIndexManager index, int ontoId){
		for (Integer i : index.getClassIdentifierSet())
			if(index.getClassIndex(i).getOntologyId() == ontoId)
				if(index.getClassIndex(i).hasDirectDisjointClasses())
					for (Integer d : index.getClassIndex(i).getDisjointClasses())
						addDisjointness(i, d, false);
		recreateDisjointIntervalsStructure();

		if(!index.isBasicSatisfiable())
			throw new Error("UNSAT index after merging");
	}

	/**
	 * Creates disjoint intervals structure.
	 * Note that adjacent intervals are merged
	 */
	public void recreateDisjointIntervalsStructure(){
		if(isDirty()){
			dirty = false;
			createDisjointIntervalsStructure();
			//			Set<Integer> ids = new HashSet<>();
			//
			//			ids.addAll(disjAdditionBuffer);
			//			//			ids.addAll(disjRemovalBuffer);
			//
			//			disjAdditionBuffer.clear();
			//			//			disjRemovalBuffer.clear();
			//
			//			//			if(ids.isEmpty())
			//			//				ids=null;
			//			createDisjointIntervalsStructure(ids);

			// TODO: to be set as testmode
			//sanityCheck();
			return;
		}
		else
			LogOutput.printAlways("VOID RECREATE DISJ SKIPPED");
	}


	/**
	 * Checks basic unsatisfiability after adding a new disjointness axiom
	 */
	public boolean isBasicSatisfiable(){

		boolean sat=true;
		unsatisfiableClassesILS.clear();

		if(isDirty()){
			System.err.println("SAT test called without an update to the " +
					"index after disjointness insertion!\nUpdating, then testing SAT");
			recreateDisjointIntervalsStructure();
		}

		for (Interval interv1 : interval2disjointIntervals.keySet()){

			for (Interval disj_interv : interval2disjointIntervals.get(interv1)){

				if (interv1.hasNonEmptyIntersectionWith(disj_interv)){

					LogOutput.print(
							"Classes in '" + 
									interv1.getIntersectionWith(disj_interv) + 
							"' are unsatisfiable");
					LogOutput.print("Involved intervals: " + interv1 + "  "  + disj_interv);// + "  " + interval2disjointIntervals.get(interv1));


					for (int pre = interv1.getIntersectionWith(disj_interv).getLeftBound(); 
							pre<=interv1.getIntersectionWith(disj_interv).getRightBound(); 
							pre++){

						//						Integer cid = null;
						// TODO Alessandro: to be improved
						//						for (Integer i : getClassIdentifierSet()) {
						//							if(getClassIndex(i).getNode().getDescOrder() == pre){
						//								cid = i;
						//								break;
						//							}
						//						}

						if (//cid != null || 
								getIdentifier4PreorderDesc(pre)>0){
							unsatisfiableClassesILS.add(getIdentifier4PreorderDesc(pre));
						}
						else{
							LogOutput.print("\tPreorder has not identifier");
						}

					}

					LogOutput.print("\t" + interv1 + "   "  + disj_interv);

					sat=false;
				}
			}	
		}
		//System.out.println(unsatisfiableClassesILS);
		if (sat){
			LogOutput.print("There are non unsatisfiable clases (non-empty intersection of disjoint intervals))");
		}

		return sat;
	}

	/**
	 * Checks basic unsatisfiability
	 */
	private void checkBasicSatisfiability(){

		boolean sat=true;
		unsatisfiableClassesILS.clear();

		for (Interval interv1 : interval2disjointIntervals.keySet()){

			for (Interval disj_interv : interval2disjointIntervals.get(interv1)){

				if (interv1.hasNonEmptyIntersectionWith(disj_interv)){

					LogOutput.print(
							"Classes in '" + 
									interv1.getIntersectionWith(disj_interv) + 
							"' are unsatisfiable");
					LogOutput.print("Involved intervals: " + interv1 + "  "  + disj_interv);// + "  " + interval2disjointIntervals.get(interv1));


					for (int pre = interv1.getIntersectionWith(disj_interv).getLeftBound(); 
							pre<=interv1.getIntersectionWith(disj_interv).getRightBound(); 
							pre++){

						if (getIdentifier4PreorderDesc(pre)>0){
							unsatisfiableClassesILS.add(getIdentifier4PreorderDesc(pre));
						}
						else{
							LogOutput.print("\tPreorder has not identifier");
						}

					}

					LogOutput.print("\t" + interv1 + "   "  + disj_interv);

					sat=false;


				}
			}	
		}

		if (sat){
			LogOutput.print("There are non unsatisfiable clases (non-empty intersection of disjoint intervals))");
		}
	}

	/**
	 * Given an identifier gets its disjoint intervals
	 * 
	 * @param cIdent
	 * @return
	 */
	public List<Interval> getDisjointIntervals4Identifier(int cIdent){

		List<Interval> disj_intervals=new ArrayList<Interval>();

		//it will only be necessary to compare one of the intervals for each entity
		int preorder = getPreOrderNumber(cIdent);


		//It may appear in several entries
		for (Interval disj_int1 : interval2disjointIntervals.keySet()){

			if (disj_int1.containsIndex(preorder)){

				for (Interval disj_int2 : interval2disjointIntervals.get(disj_int1)){

					disj_intervals.add(disj_int2);

				}	
			}
		}
		return disj_intervals; 
	}

	//private Interval sorted_intervals[];	
	private void _Quicksort(Interval matrix[], int a, int b)
	{
		//sorted_intervals = new Interval[matrix.length];
		Interval buf;
		int from = a;
		int to = b;
		Interval pivot = matrix[(from+to)/2];
		do {

			while(from <= b && matrix[from].hasLowerLeftBoundThan(pivot)){
				from++;
			}
			while(to >= a && matrix[to].hasGreaterLeftBoundThan(pivot)){
				to--;
			}
			if(from <= to){
				buf = matrix[from];
				matrix[from] = matrix[to];
				matrix[to] = buf;
				from++; to--;
			}
		}while(from <= to);

		if(a < to) {
			_Quicksort(matrix, a, to);
		}
		if(from < b){
			_Quicksort(matrix, from, b);
		}

		//sorted_intervals = matrix;

	}

	public boolean addDisjointness(int i1, int i2, boolean recompute){
		boolean effective = false;

		if(!identifier2ClassIndex.get(i1).hasDirectDisjointClasses() || 
				!identifier2ClassIndex.get(i1).getDisjointClasses().contains(i2)){
			identifier2ClassIndex.get(i1).addDisjointClass(i2);
			effective = true;
			//			if(ident2disjointclasses.containsKey(i1))
			//				ident2disjointclasses.get(i1).add(i2);
		}

		if(!identifier2ClassIndex.get(i2).hasDirectDisjointClasses() ||
				!identifier2ClassIndex.get(i2).getDisjointClasses().contains(i1)){
			identifier2ClassIndex.get(i2).addDisjointClass(i1);
			effective = true;
			//			if(ident2disjointclasses.containsKey(i2))
			//				ident2disjointclasses.get(i2).add(i1);
		}

		//		if(ident2disjointclasses.containsKey(i1))
		//			ident2disjointclasses.get(i1).add(i2);
		//		if(ident2disjointclasses.containsKey(i2))
		//			ident2disjointclasses.get(i2).add(i1);
		//
		//		identifier2ClassIndex.get(i1).addDisjointClass(i2);
		//		identifier2ClassIndex.get(i2).addDisjointClass(i1);
		//		effective = true;

		if(effective)
			dirty = true;

		if(countDisj && effective)
			addedDisj++;

		if(dirty && recompute){
			recreateDisjointIntervalsStructure();
			if(!areDisjoint(i1, i2))
				throw new Error("NOT UPDATED: " + i1 + " DISJ " + i2);
			if(!areDisjoint(i2, i1))
				throw new Error("NOT UPDATED: " + i2 + " DISJ " + i1);
		}

		return effective;
	}

	//	public boolean addDisjointness(int i1, int i2, boolean recompute){
	//		// we add the information for the new disjointness axiom before recomputing
	//		//		System.out.println(i1 + " -> " + identifier2ClassIndex.get(i1).getDisjointClasses());
	//		//		System.out.println(i2 + " -> " + identifier2ClassIndex.get(i2).getDisjointClasses());
	//
	//		boolean effective = false;
	//
	//		if(!identifier2ClassIndex.get(i1).hasDirectDisjointClasses() || 
	//				!identifier2ClassIndex.get(i1).getDisjointClasses().contains(i2)){
	//			identifier2ClassIndex.get(i1).addDisjointClass(i2);
	//			disjAdditionBuffer.add(i1);
	//			disjAdditionBuffer.add(i2);
	//			effective = true;
	//		}
	//
	//		if(!identifier2ClassIndex.get(i2).hasDirectDisjointClasses() ||
	//				!identifier2ClassIndex.get(i2).getDisjointClasses().contains(i1)){
	//			identifier2ClassIndex.get(i2).addDisjointClass(i1);
	//			disjAdditionBuffer.add(i1);
	//			disjAdditionBuffer.add(i2);
	//			effective = true;
	//		}		
	//		//		System.out.println(i1 + " -> " + identifier2ClassIndex.get(i1).getDisjointClasses());
	//		//		System.out.println(i2 + " -> " + identifier2ClassIndex.get(i2).getDisjointClasses());
	//
	//		if(countDisj && effective)
	//			addedDisj++;
	//
	//		if(effective)
	//			createDisjointIntervalsStructure(null);
	//
	//		return effective;
	//	}

	public boolean retractDisjointness(int i1, int i2, boolean recompute) {

		boolean effective = false;

		// we add the information for the new disjointness axiom before recomputing
		if(identifier2ClassIndex.get(i1).hasDirectDisjointClasses() &&
				identifier2ClassIndex.get(i1).getDisjointClasses().contains(i2)){
			identifier2ClassIndex.get(i1).removeDisjointClass(i2);
			effective = true;
			//			if(ident2disjointclasses.containsKey(i1))
			//				ident2disjointclasses.get(i1).remove(i2);
		}

		if(identifier2ClassIndex.get(i2).hasDirectDisjointClasses() &&
				identifier2ClassIndex.get(i2).getDisjointClasses().contains(i1)){
			identifier2ClassIndex.get(i2).removeDisjointClass(i1);
			effective = true;
			//			if(ident2disjointclasses.containsKey(i2))
			//				ident2disjointclasses.get(i2).remove(i1);
		}

		//		if(ident2disjointclasses.containsKey(i1))
		//			ident2disjointclasses.get(i1).remove(i2);
		//		if(ident2disjointclasses.containsKey(i2))
		//			ident2disjointclasses.get(i2).remove(i1);
		//
		//		identifier2ClassIndex.get(i1).removeDisjointClass(i2);
		//		identifier2ClassIndex.get(i2).removeDisjointClass(i1);
		//		effective = true;

		if(effective)
			dirty = true;

		if(countDisj && effective)
			addedDisj--;

		if(dirty && recompute){
			recreateDisjointIntervalsStructure();
			if(areDisjoint(i1, i2))
				throw new Error("NOT UPDATED: " + i1 + " NOT DISJ " + i2);
			if(areDisjoint(i2, i1))
				throw new Error("NOT UPDATED: " + i2 + " NOT DISJ " + i1);
		}

		return effective;
	}	

	//	public boolean retractDisjointness(int i1, int i2, boolean recompute) {
	//		//		System.out.println(identifier2ClassIndex.get(i1).getDisjointClasses());
	//		//		System.out.println(identifier2ClassIndex.get(i2).getDisjointClasses());
	//
	//		boolean effective = false;
	//
	//		// we add the information for the new disjointness axiom before recomputing
	//		if(identifier2ClassIndex.get(i1).hasDirectDisjointClasses() &&
	//				identifier2ClassIndex.get(i1).getDisjointClasses().contains(i2)){
	//			identifier2ClassIndex.get(i1).removeDisjointClass(i2);
	//
	////			disjRemovalBuffer.add(i1);
	////			disjRemovalBuffer.add(i2);
	//			effective = true;
	//		}
	//
	//		if(identifier2ClassIndex.get(i2).hasDirectDisjointClasses() &&
	//				identifier2ClassIndex.get(i2).getDisjointClasses().contains(i1)){
	//			identifier2ClassIndex.get(i2).removeDisjointClass(i1);
	//
	////			disjRemovalBuffer.add(i1);
	////			disjRemovalBuffer.add(i2);
	//			effective = true;
	//		}
	//
	//		//		System.out.println(identifier2ClassIndex.get(i1).getDisjointClasses());
	//		//		System.out.println(identifier2ClassIndex.get(i2).getDisjointClasses());
	//
	//		if(countDisj && effective)
	//			addedDisj--;
	//
	//		if(effective){
	//			createDisjointIntervalsStructureDeletion(i1,i2);
	//			createDisjointIntervalsStructureDeletion(i2,i1);
	//			//recreateDisjointIntervalsStructure();
	//		}
	//		
	//		return effective;
	//	}	

	public List<Interval> mergeIntervals(List<Interval> list_intervals){
		//		List<Interval> mergedList = new ArrayList<>();
		//		for (Interval i : list_intervals)
		//			mergedList.add(new Interval(i.getLeftBound(),i.getRightBound()));

		if (list_intervals.size()>=3){			
			//First Sort
			Interval [] array_intervals = new Interval[list_intervals.size()];
			array_intervals = list_intervals.toArray(array_intervals);

			_Quicksort(array_intervals, 0, array_intervals.length-1);

			//Merge			
			list_intervals.clear(); //we already have our sorted array

			Interval current_interval=array_intervals[0];

			for (int i=1; i< array_intervals.length; i++){

				if (current_interval.isAdjacentTo(array_intervals[i])){
					current_interval = current_interval.getUnionWith(array_intervals[i]);
				}
				else {
					list_intervals.add(current_interval);
					current_interval=array_intervals[i];
				}														
			}
			list_intervals.add(current_interval);
		}

		else if (list_intervals.size()==2){
			list_intervals = 
					list_intervals.get(0).getUnionWithList(
							list_intervals.get(1));
		}

		//else if list_intervals.size()<=1 do nothing
		return list_intervals;
	}

	public boolean checkPreOrders() {
		boolean ok = true;

		for (Integer i : getClassIdentifierSet())
			if(!isBottomClass(i) 
					&& getClassIndex(i).getNode().getDescOrder() < 0){
				ok = false;
				System.err.println("Class " + 
						getClassIndex(i).getNamespace() + "#" +
						getClassIndex(i).getEntityName() 
						+ " with id " + i + " of onto " 
						+ getClassIndex(i).getOntologyId() 
						+ " has a preorder -1!");
			}
		return ok;
	}
}
