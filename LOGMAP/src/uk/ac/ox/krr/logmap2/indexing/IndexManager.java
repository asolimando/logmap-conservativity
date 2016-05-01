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

import java.io.*;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLDataFactory;
import uk.ac.ox.krr.logmap2.owlapi.SynchronizedOWLManager;
import uk.ac.ox.krr.logmap2.reasoning.ReasonerAccess;
import uk.ac.ox.krr.logmap2.indexing.entities.*;
import uk.ac.ox.krr.logmap2.indexing.labelling_schema.Interval;
import uk.ac.ox.krr.logmap2.io.LogOutput;
import uk.ac.ox.krr.logmap2.utilities.Lib;
import uk.ac.ox.krr.logmap2.utilities.Pair;
import uk.ac.ox.krr.logmap2.utilities.Utilities;

public abstract class IndexManager {

	/**To get the frequency and co-occurrence */
	//TODO Yujiao - something with stop word
	protected Map<String, Set<Integer>> singleWordInvertedIndex = 
			new HashMap<String, Set<Integer>>();
	//protected Map<String, Integer> frequency4words = new HashMap<String, Integer>();

	protected int calls_tax_question=0;
	protected int calls_disj_question=0;
	protected int unknown_disj_question=0;

	protected double time_tax_question=0.0;
	protected double time_disj_question=0.0;
	protected double time_disj_questionIT=0.0;
	
	protected boolean measureTime = false; 

	protected long init, fin;

	protected Set<Integer> bottomIdentifiers = new HashSet<>();			
	private Set<Integer> bottomIdentifiersReadOnly = 
			Collections.unmodifiableSet(bottomIdentifiers);
	
	public IndexManager() {
		super();
	}

	public IndexManager(IndexManager index) {
		// these fields need a deep copy for safely "cloning" dynamic objects

		for (Entry<String, Set<Integer>> e 
				: index.singleWordInvertedIndex.entrySet())
			singleWordInvertedIndex.put(e.getKey(), new HashSet<>(e.getValue()));
		
		// these structures should never be used for dynamic index, they are never updated
//		for (Entry<Integer, Set<Integer>> e 
//				: index.ident2disjointclasses.entrySet())
//			ident2disjointclasses.put(e.getKey(), new HashSet<>(e.getValue()));
//		
//		for (Entry<Integer, Set<Integer>> e 
//				: index.ident2equivalents.entrySet())
//			ident2equivalents.put(e.getKey(), new HashSet<>(e.getValue()));
//		
//		for (Entry<Integer, Set<Integer>> e 
//				: index.ident2subclasses.entrySet())
//			ident2subclasses.put(e.getKey(), new HashSet<>(e.getValue()));
//		
//		for (Entry<Integer, Set<Integer>> e 
//				: index.ident2subclasses_module.entrySet())
//			ident2subclasses_module.put(e.getKey(), 
//					new HashSet<>(e.getValue()));
//		
//		for (Entry<Integer, Set<Integer>> e 
//				: index.ident2superclasses.entrySet())
//			ident2superclasses.put(e.getKey(), new HashSet<>(e.getValue()));
//		
//		for (Entry<Integer, Set<Integer>> e 
//				: index.ident2superclasses_module.entrySet())
//			ident2superclasses_module.put(e.getKey(), new HashSet<>(e.getValue()));

		for (Entry<Integer, ClassIndex> e : 
			index.identifier2ClassIndex.entrySet())
			identifier2ClassIndex.put(e.getKey(), 
					new ClassIndex(e.getValue()));

		for (Entry<Integer, DataPropertyIndex> e : 
			index.identifier2DataPropIndex.entrySet()) {
			identifier2DataPropIndex.put(e.getKey(), 
					new DataPropertyIndex(e.getValue()));
		}

		for (Entry<Integer, ObjectPropertyIndex> e : 
			index.identifier2ObjPropIndex.entrySet()) {
			identifier2ObjPropIndex.put(e.getKey(), 
					new ObjectPropertyIndex(e.getValue()));
		}
		
		for (Entry<Interval, Set<Interval>> e : 
			index.interval2disjointIntervals.entrySet()) {
			
			Set<Interval> s = new HashSet<>();
			for (Interval i : e.getValue())
				s.add(new Interval(i.getLeftBound(), i.getRightBound()));
			
			interval2disjointIntervals.put(
					new Interval(e.getKey().getLeftBound(),
					e.getKey().getRightBound()), s);
		}

		for (Entry<Integer, IndividualIndex> e : 
			index.identifier2IndividualIndex.entrySet()) {
			identifier2IndividualIndex.put(e.getKey(), 
					new IndividualIndex(e.getValue()));
		}

		if(index.ident2DirectSubClasses_integration != null){
			ident2DirectSubClasses_integration = 
					new HashMap<Integer, Set<Integer>>();
			for (Entry<Integer, Set<Integer>> e : 
				index.ident2DirectSubClasses_integration.entrySet()) {

				ident2DirectSubClasses_integration.put(e.getKey(), 
						new HashSet<Integer>(e.getValue()));
			}
		}

		for (Entry<Set<Integer>, Integer> e : 
			index.generalHornAxioms.entrySet()) {			
			generalHornAxioms.put(new HashSet<Integer>(e.getKey()),e.getValue());
		}

		// ok with a shallow copy
		bottomIdentifiers = new HashSet<>(index.bottomIdentifiers);
		bottomIdentifiersReadOnly = Collections.unmodifiableSet(bottomIdentifiers);
				
		identifiersInModule.addAll(index.identifiersInModule);

		preOrderDesc2Identifier.putAll(index.preOrderDesc2Identifier);

		preOrderAnc2Identifier.putAll(index.preOrderAnc2Identifier);

		unsatisfiableClassesILS.addAll(index.unsatisfiableClassesILS);
		identifier2IRIOnto.putAll(index.identifier2IRIOnto);

		//factory = index.factory;		
		RootIdentifiers.addAll(index.RootIdentifiers);
		representativeNodes.addAll(index.representativeNodes);

		dangerousClasses.addAll(index.dangerousClasses);

		class_indiv_ident=index.class_indiv_ident; 		
		dprop_ident=index.dprop_ident;
		oprop_ident=index.oprop_ident;
		onto_ident=index.onto_ident;

		if(index.representativesFromMappings != null){
			representativesFromMappings = new HashSet<>();
			representativesFromMappings.addAll(index.representativesFromMappings);
		}
		else
			representativesFromMappings = null;
	}

	//	
	//	@Override
	//	public int hashCode() {
	//		final int prime = 31;
	//		int result = 1;
	//		result = prime * result
	//				+ ((RootIdentifiers == null) ? 0 : RootIdentifiers.hashCode());
	//		result = prime * result + class_indiv_ident;
	//		result = prime
	//				* result
	//				+ ((dangerousClasses == null) ? 0 : dangerousClasses.hashCode());
	//		result = prime * result + dprop_ident;
	//		result = prime
	//				* result
	//				+ ((generalHornAxioms == null) ? 0 : generalHornAxioms
	//						.hashCode());
	//		result = prime
	//				* result
	//				+ ((ident2DirectSubClasses_integration == null) ? 0
	//						: ident2DirectSubClasses_integration.hashCode());
	//		result = prime
	//				* result
	//				+ ((ident2disjointclasses == null) ? 0 : ident2disjointclasses
	//						.hashCode());
	//		result = prime
	//				* result
	//				+ ((ident2equivalents == null) ? 0 : ident2equivalents
	//						.hashCode());
	//		result = prime
	//				* result
	//				+ ((ident2subclasses == null) ? 0 : ident2subclasses.hashCode());
	//		result = prime
	//				* result
	//				+ ((ident2subclasses_module == null) ? 0
	//						: ident2subclasses_module.hashCode());
	//		result = prime
	//				* result
	//				+ ((ident2superclasses == null) ? 0 : ident2superclasses
	//						.hashCode());
	//		result = prime
	//				* result
	//				+ ((ident2superclasses_module == null) ? 0
	//						: ident2superclasses_module.hashCode());
	//		result = prime
	//				* result
	//				+ ((identifier2ClassIndex == null) ? 0 : identifier2ClassIndex
	//						.hashCode());
	//		result = prime
	//				* result
	//				+ ((identifier2DataPropIndex == null) ? 0
	//						: identifier2DataPropIndex.hashCode());
	//		result = prime
	//				* result
	//				+ ((identifier2IRIOnto == null) ? 0 : identifier2IRIOnto
	//						.hashCode());
	//		result = prime
	//				* result
	//				+ ((identifier2IndividualIndex == null) ? 0
	//						: identifier2IndividualIndex.hashCode());
	//		result = prime
	//				* result
	//				+ ((identifier2ObjPropIndex == null) ? 0
	//						: identifier2ObjPropIndex.hashCode());
	//		result = prime
	//				* result
	//				+ ((identifiersInModule == null) ? 0 : identifiersInModule
	//						.hashCode());
	//		result = prime
	//				* result
	//				+ ((individual2classTypes == null) ? 0 : individual2classTypes
	//						.hashCode());
	//		result = prime
	//				* result
	//				+ ((interval2disjointIntervals == null) ? 0
	//						: interval2disjointIntervals.hashCode());
	//		result = prime * result + onto_ident;
	//		result = prime * result + oprop_ident;
	//		result = prime
	//				* result
	//				+ ((preOrderAnc2Identifier == null) ? 0
	//						: preOrderAnc2Identifier.hashCode());
	//		result = prime
	//				* result
	//				+ ((preOrderDesc2Identifier == null) ? 0
	//						: preOrderDesc2Identifier.hashCode());
	//		result = prime
	//				* result
	//				+ ((representativeNodes == null) ? 0 : representativeNodes
	//						.hashCode());
	//		result = prime
	//				* result
	//				+ ((representativesFromMappings == null) ? 0
	//						: representativesFromMappings.hashCode());
	//		result = prime
	//				* result
	//				+ ((singleWordInvertedIndex == null) ? 0
	//						: singleWordInvertedIndex.hashCode());
	//		result = prime
	//				* result
	//				+ ((unsatisfiableClassesILS == null) ? 0
	//						: unsatisfiableClassesILS.hashCode());
	//		return result;
	//	}
	//
	//	@Override
	//	public boolean equals(Object obj) {
	//		if (this == obj)
	//			return true;
	//		if (obj == null)
	//			return false;
	//		if (getClass() != obj.getClass())
	//			return false;
	//		IndexManager other = (IndexManager) obj;
	//		if (RootIdentifiers == null) {
	//			if (other.RootIdentifiers != null)
	//				return false;
	//		} else if (!RootIdentifiers.equals(other.RootIdentifiers))
	//			return false;
	//		if (class_indiv_ident != other.class_indiv_ident)
	//			return false;
	//		if (dangerousClasses == null) {
	//			if (other.dangerousClasses != null)
	//				return false;
	//		} else if (!dangerousClasses.equals(other.dangerousClasses))
	//			return false;
	//		if (dprop_ident != other.dprop_ident)
	//			return false;
	//		if (generalHornAxioms == null) {
	//			if (other.generalHornAxioms != null)
	//				return false;
	//		} else if (!generalHornAxioms.equals(other.generalHornAxioms))
	//			return false;
	//		if (ident2DirectSubClasses_integration == null) {
	//			if (other.ident2DirectSubClasses_integration != null)
	//				return false;
	//		} else if (!ident2DirectSubClasses_integration
	//				.equals(other.ident2DirectSubClasses_integration))
	//			return false;
	//		if (ident2disjointclasses == null) {
	//			if (other.ident2disjointclasses != null)
	//				return false;
	//		} else if (!ident2disjointclasses.equals(other.ident2disjointclasses))
	//			return false;
	//		if (ident2equivalents == null) {
	//			if (other.ident2equivalents != null)
	//				return false;
	//		} else if (!ident2equivalents.equals(other.ident2equivalents))
	//			return false;
	//		if (ident2subclasses == null) {
	//			if (other.ident2subclasses != null)
	//				return false;
	//		} else if (!ident2subclasses.equals(other.ident2subclasses))
	//			return false;
	//		if (ident2subclasses_module == null) {
	//			if (other.ident2subclasses_module != null)
	//				return false;
	//		} else if (!ident2subclasses_module
	//				.equals(other.ident2subclasses_module))
	//			return false;
	//		if (ident2superclasses == null) {
	//			if (other.ident2superclasses != null)
	//				return false;
	//		} else if (!ident2superclasses.equals(other.ident2superclasses))
	//			return false;
	//		if (ident2superclasses_module == null) {
	//			if (other.ident2superclasses_module != null)
	//				return false;
	//		} else if (!ident2superclasses_module
	//				.equals(other.ident2superclasses_module))
	//			return false;
	//		if (identifier2ClassIndex == null) {
	//			if (other.identifier2ClassIndex != null)
	//				return false;
	//		} else if (!identifier2ClassIndex.equals(other.identifier2ClassIndex))
	//			return false;
	//		if (identifier2DataPropIndex == null) {
	//			if (other.identifier2DataPropIndex != null)
	//				return false;
	//		} else if (!identifier2DataPropIndex
	//				.equals(other.identifier2DataPropIndex))
	//			return false;
	//		if (identifier2IRIOnto == null) {
	//			if (other.identifier2IRIOnto != null)
	//				return false;
	//		} else if (!identifier2IRIOnto.equals(other.identifier2IRIOnto))
	//			return false;
	//		if (identifier2IndividualIndex == null) {
	//			if (other.identifier2IndividualIndex != null)
	//				return false;
	//		} else if (!identifier2IndividualIndex
	//				.equals(other.identifier2IndividualIndex))
	//			return false;
	//		if (identifier2ObjPropIndex == null) {
	//			if (other.identifier2ObjPropIndex != null)
	//				return false;
	//		} else if (!identifier2ObjPropIndex
	//				.equals(other.identifier2ObjPropIndex))
	//			return false;
	//		if (identifiersInModule == null) {
	//			if (other.identifiersInModule != null)
	//				return false;
	//		} else if (!identifiersInModule.equals(other.identifiersInModule))
	//			return false;
	//		if (individual2classTypes == null) {
	//			if (other.individual2classTypes != null)
	//				return false;
	//		} else if (!individual2classTypes.equals(other.individual2classTypes))
	//			return false;
	//		if (interval2disjointIntervals == null) {
	//			if (other.interval2disjointIntervals != null)
	//				return false;
	//		} else if (!interval2disjointIntervals
	//				.equals(other.interval2disjointIntervals))
	//			return false;
	//		if (onto_ident != other.onto_ident)
	//			return false;
	//		if (oprop_ident != other.oprop_ident)
	//			return false;
	//		if (preOrderAnc2Identifier == null) {
	//			if (other.preOrderAnc2Identifier != null)
	//				return false;
	//		} else if (!preOrderAnc2Identifier.equals(other.preOrderAnc2Identifier))
	//			return false;
	//		if (preOrderDesc2Identifier == null) {
	//			if (other.preOrderDesc2Identifier != null)
	//				return false;
	//		} else if (!preOrderDesc2Identifier
	//				.equals(other.preOrderDesc2Identifier))
	//			return false;
	//		if (representativeNodes == null) {
	//			if (other.representativeNodes != null)
	//				return false;
	//		} else if (!representativeNodes.equals(other.representativeNodes))
	//			return false;
	//		if (representativesFromMappings == null) {
	//			if (other.representativesFromMappings != null)
	//				return false;
	//		} else if (!representativesFromMappings
	//				.equals(other.representativesFromMappings))
	//			return false;
	//		if (singleWordInvertedIndex == null) {
	//			if (other.singleWordInvertedIndex != null)
	//				return false;
	//		} else if (!singleWordInvertedIndex
	//				.equals(other.singleWordInvertedIndex))
	//			return false;
	//		if (unsatisfiableClassesILS == null) {
	//			if (other.unsatisfiableClassesILS != null)
	//				return false;
	//		} else if (!unsatisfiableClassesILS
	//				.equals(other.unsatisfiableClassesILS))
	//			return false;
	//		return true;
	//	}
	
	public void addBottomIdentifier(int id) {
		bottomIdentifiers.add(id);
	}
	
	// it says whether this class is equivalent to bottom or not
	public boolean isBottomClass(int id){
		return bottomIdentifiers.contains(id);
	}
	
	public Set<Integer> getBottomClasses(){
		return bottomIdentifiersReadOnly;
	}
	
	public int getNumberOfTaxCalls(){
		return calls_tax_question;
	}

	public int getNumberOfDisjCalls(){
		return calls_disj_question;
	}

	public int getNumberOfUnknownDisjCalls(){
		return unknown_disj_question;
	}

	public double getTime4TaxCalls(){
		return time_tax_question;
	}

	public double getTime4DisjCalls(){
		return time_disj_question;
	}

	public double getAvgTime4TaxCalls(){
		return Utilities.getRoundValue(
				(time_tax_question/(double)calls_tax_question), 4);
	}

	public double getAvgTime4DisjCalls(){
		return Utilities.getRoundValue(
				(time_disj_question/(double)calls_tax_question), 4);
	}


	public void addWordOccurrence(String word, int ident) {
		Set<Integer> temp;

		if (singleWordInvertedIndex.containsKey(word))
			temp = singleWordInvertedIndex.get(word);
		else
			singleWordInvertedIndex.put(word, temp = new HashSet<Integer>());

		temp.add(ident);
	}

	public void printWordDistribution()
	{
		ArrayList<Pair<String, Integer>> word2frequency = new ArrayList<Pair<String, Integer>>();
		int frequency;

		for (String word : singleWordInvertedIndex.keySet())
		{
			frequency = singleWordInvertedIndex.get(word).size();
			word2frequency.add(new Pair<String, Integer>(word, frequency));
		}

		Collections.sort(word2frequency, new Comparator4String2Int());

		try
		{
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream("/auto/users/yzhou/word_distribution.txt")));

			writer.write(identifier2ClassIndex.size() + "\n");
			for (Pair<String, Integer> pair : word2frequency)
				writer.write(pair.getKey() + " " + pair.getValue() + "\n");

			writer.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		Lib.debuginfo("finished word distribution");
	}

	public Set<Integer> getCooccurrenceOfWords(Set<String> words)
	{
		Set<Integer> classList = null;
		Set<Integer> classList_temp = null;

		for (String word : words)
			if (singleWordInvertedIndex.containsKey(word))
				if (classList == null)
				{
					classList = new HashSet<Integer>();
					for (Integer ind : singleWordInvertedIndex.get(word))
						classList.add(ind);
				}
				else 
				{
					classList_temp = singleWordInvertedIndex.get(word);
					classList = intersectSet(classList, classList_temp);
				}

		return classList;
	}

	public Set<Integer> intersectSet(Set<Integer> list1, Set<Integer> list2)
	{
		if (list1 == null) return list2;
		if (list2 == null) return list1;

		Set<Integer> list = new HashSet<Integer>();

		for (Integer i : list1)
			if (list2.contains(i))
				list.add(i);

		return list;
	}



	public void clearSingleWordInvertedIndex(){
		singleWordInvertedIndex.clear();
	}


	protected Map<Integer, ClassIndex> identifier2ClassIndex = new HashMap<Integer, ClassIndex>();

	protected Map<Integer, DataPropertyIndex> identifier2DataPropIndex = new HashMap<Integer, DataPropertyIndex>();

	protected Map<Integer, ObjectPropertyIndex> identifier2ObjPropIndex = new HashMap<Integer, ObjectPropertyIndex>();

	protected Map<Integer, IndividualIndex> identifier2IndividualIndex = new HashMap<Integer,  IndividualIndex>();


	/**For projections*/
	protected Set<Integer> identifiersInModule = new HashSet<Integer>();


	//protected Map<Integer, Set<Integer>> Root2IndependentRoots= new HashMap<Integer, Set<Integer>>();

	/**Used to extract upper hierarchy 
	 * @deprecated ??**/
	protected Map<Integer, Integer> preOrderAnc2Identifier= new HashMap<Integer, Integer>();

	/**Used to get unsarisfiavle clases if unsat intervals has non empty intersection
	 * So far only used if not solved cases were found**/
	protected Map<Integer, Integer> preOrderDesc2Identifier= new HashMap<Integer, Integer>();



	/**We store disjointness as a interval of disjoint classes*/
	protected Map<Interval, Set<Interval>> interval2disjointIntervals = 
			new HashMap<Interval, Set<Interval>>();

	/**Unsat classes ILS*/
	protected Set<Integer> unsatisfiableClassesILS = new HashSet<Integer>();


	/** We need to know the iri of an entity */
	protected Map<Integer, String> identifier2IRIOnto = new HashMap<Integer, String>();

	//NOT NECESSARY
	/* Identifier of ontology toi number of classes. Important to know which ids belong to one onto or other */
	//protected Map<Integer, String> identifier2NumOfClasses = new HashMap<Integer, String>();


	//protected Set<Integer> RootAncPreorder = new HashSet<Integer>();


	//Logical iri (ending in #)
	//protected String iri_onto;

	//protected OWLOntologyManager managerOnto = OWLManager.createOWLOntologyManager();
	//protected OWLDataFactory factory = managerOnto.getOWLDataFactory();
	protected OWLDataFactory factory = SynchronizedOWLManager.createOWLDataFactory();
	//protected OWLOntology onto;


	/** Importnat for a ordered assessment */
	protected Set<Integer> RootIdentifiers = new HashSet<Integer>();

	/**This set will be used to propagate equivalences (entities store equivalents)*/
	//This set will need to be enriched with anchors... or at leats considered after indexing
	protected Set<Integer> representativeNodes = new HashSet<Integer>();


	/* *A^B->C axiom*/
	protected Map<Set<Integer>, Integer> generalHornAxioms= new HashMap<Set<Integer>, Integer>();

	public Set<Integer> dangerousClasses = new HashSet<Integer>();//equivalnet to TOP


	protected int class_indiv_ident=0; //shared by both 

	protected int dprop_ident=0;

	protected int oprop_ident=0;

	protected int onto_ident=0;

	//private int indiv_ident=0;


	public void clearAlternativeLabels4Classes(){

		for (int ident : identifier2ClassIndex.keySet()){

			if (identifier2ClassIndex.get(ident).hasAlternativeLabels()){
				identifier2ClassIndex.get(ident).deleteAltLabels();
			}
		}
	}


	public void clearStemmedAlternativeLabels4Classes(){

		for (int ident : identifier2ClassIndex.keySet()){

			if (identifier2ClassIndex.get(ident).hasStemmedAlternativeLabels()){
				identifier2ClassIndex.get(ident).deleteAltStemmedLabels();
			}

		}
	}


	public void clearTaxonomicalStructures(){

		identifier2ClassIndex.clear();
		identifier2DataPropIndex.clear();
		identifier2ObjPropIndex.clear();
		identifier2IndividualIndex.clear();

		identifiersInModule.clear();

		unsatisfiableClassesILS.clear();

		preOrderAnc2Identifier.clear();
		preOrderDesc2Identifier.clear();

		RootIdentifiers.clear();
		generalHornAxioms.clear();

		representativeNodes.clear();


		ident2disjointclasses.clear();
		ident2equivalents.clear();
		ident2subclasses.clear();
		ident2subclasses_module.clear();
		ident2superclasses.clear();
		ident2superclasses_module.clear();

		ident2DirectSubClasses_integration.clear();

		individual2classTypes.clear();


	}


	protected HashMap<Integer, Set<Integer>> ident2DirectSubClasses_integration;

	protected Set<Integer> representativesFromMappings;



	public HashMap<Integer, Set<Integer>> getIdent2DirectSubClasses_Integration(){
		return ident2DirectSubClasses_integration;
	}

	public Set<Integer> getRepresentativesFromMappings(){
		return representativesFromMappings;
	}



	public void clearAuxStructuresforLabellingSchema(){
		ident2DirectSubClasses_integration.clear();
		representativesFromMappings.clear();
	}


	public Set<Integer> getDangerousClasses(){
		return dangerousClasses;
	}

	public void addDangerousClasses(int ide){
		dangerousClasses.add(ide);
	}


	/**
	 * Debugging method
	 */
	public void printDirectDisjointness(){

		for (int icls : identifier2ClassIndex.keySet()){

			if (identifier2ClassIndex.get(icls).hasDirectDisjointClasses()){

				LogOutput.print(identifier2ClassIndex.get(icls).getEntityName() + ": " +identifier2ClassIndex.get(icls).getNode().getDescIntervals());

				//We create list of disjoint intervals
				for (int disjcls : identifier2ClassIndex.get(icls).getDisjointClasses()){

					LogOutput.print("\t" + identifier2ClassIndex.get(disjcls).getEntityName()+ ": " +identifier2ClassIndex.get(disjcls).getNode().getDescIntervals());

					//for (Interval disjcls_interval : identifier2ClassIndex.get(disjcls).getNode().getDescIntervals()){

					//	LogOutput.print("\t\t" + disjcls_interval);

					//}
				}

			}

		}

	}



	public abstract void setIntervalLabellingIndex(Map<Integer, Set<Integer>> exact_mappings);


	public void setJointReasoner(ReasonerAccess jointreasoner){

	}



	public String getIRIStrOnto4Id(int id){
		return identifier2IRIOnto.get(id);
	}


	public OWLDataFactory getFactory(){
		return factory;
	}


	public int addNewOntologyEntry(String iristr){

		identifier2IRIOnto.put(onto_ident, iristr);

		onto_ident++;

		return (onto_ident-1);

	}


	public int addNewClassEntry(){
		identifier2ClassIndex.put(class_indiv_ident, new ClassIndex(class_indiv_ident));
		class_indiv_ident++;
		return (class_indiv_ident-1);
	}




	public void setOntologyId4Class(int ident, int id){
		identifier2ClassIndex.get(ident).setOntologyId(id);
	}

	public void setClassName(int ident, String name){
		identifier2ClassIndex.get(ident).setEntityName(name);
	}

	public void setClassNamespace(int ident, String ns_ent){
		identifier2ClassIndex.get(ident).setNamespace(ns_ent);
	}

	public void setClassLabel(int ident, String label){
		identifier2ClassIndex.get(ident).setLabel(label);
	}

	public void addAlternativeClassLabel(int ident, String altlabel){
		identifier2ClassIndex.get(ident).addAlternativeLabel(altlabel);
	}

	public void addStemmedAltClassLabel(int ident, String label)
	{
		identifier2ClassIndex.get(ident).addStemmedAltLabel(label);
	}

	public void addRoot2Structure(int ide_root){
		RootIdentifiers.add(ide_root);
	}

	public Set<Integer> getRootIdentifiers(){
		return RootIdentifiers;
	}

	public void addRepresentativeNode(int ide_rep){
		representativeNodes.add(ide_rep);
	}

	public Set<Integer> getRepresentativeNodes(){
		return representativeNodes;
	}


	//TODO same set of class could intersect to different heads...
	public void addGeneralHornAxiom2Structure(Set<Integer> body, int head){
		if (!generalHornAxioms.containsKey(body)){
			generalHornAxioms.put(new HashSet<Integer>(body), head);
		}	
	}

	public Map<Set<Integer>, Integer> getGeneralHornAxiom(){
		return generalHornAxioms;
	}






	public int addNewIndividualEntry(){
		identifier2IndividualIndex.put(class_indiv_ident, new IndividualIndex(class_indiv_ident));
		class_indiv_ident++;
		return (class_indiv_ident-1);
	}




	public void setOntologyId4Individual(int ident, int id){
		identifier2IndividualIndex.get(ident).setOntologyId(id);
	}

	public void setIndividualName(int ident, String name){
		identifier2IndividualIndex.get(ident).setEntityName(name);
	}

	public void setIndividualNamespace(int ident, String ns_ent){
		identifier2IndividualIndex.get(ident).setNamespace(ns_ent);
	}

	//will be equal to name
	public void setIndividualLabel(int ident, String label){
		identifier2IndividualIndex.get(ident).setLabel(label);
	}

	public void addAlternativeIndividualLabel(int ident, String altlabel){
		identifier2IndividualIndex.get(ident).addAlternativeLabel(altlabel);
	}







	public int addNewDataPropertyEntry(){
		identifier2DataPropIndex.put(dprop_ident, new DataPropertyIndex(dprop_ident));
		dprop_ident++;
		return (dprop_ident-1);
	}


	public void setOntologyId4DataProp(int ident, int id){
		identifier2DataPropIndex.get(ident).setOntologyId(id);
	}

	public void setDataPropName(int ident, String name){
		identifier2DataPropIndex.get(ident).setEntityName(name);
	}

	public void setDataPropNamespace(int ident, String ns_ent){
		identifier2DataPropIndex.get(ident).setNamespace(ns_ent);
	}


	public void setDataPropLabel(int ident, String label){
		identifier2DataPropIndex.get(ident).setLabel(label);
	}


	public void addDomainClass4DataProperty(int ident, int domain_ident){
		identifier2DataPropIndex.get(ident).addDomainClassIndex(domain_ident);
	}

	public void addRangeType4DataProperty(int ident, String range){
		identifier2DataPropIndex.get(ident).addRangeType(range);
	}


	public void addAlternativeDataPropertyLabel(int ident, String altlabel){
		identifier2DataPropIndex.get(ident).addAlternativeLabel(altlabel);
	}



	public void addType4Individual(int ident, int type_class){
		identifier2IndividualIndex.get(ident).addClassTypeIndex(type_class);
	}







	public int addNewObjectPropertyEntry(){
		identifier2ObjPropIndex.put(oprop_ident, new ObjectPropertyIndex(oprop_ident));
		oprop_ident++;
		return (oprop_ident-1);
	}


	public void setOntologyId4ObjectProp(int ident, int id){
		identifier2ObjPropIndex.get(ident).setOntologyId(id);
	}

	public void setObjectPropName(int ident, String name){
		identifier2ObjPropIndex.get(ident).setEntityName(name);
	}

	public void setObjectPropNamespace(int ident, String ns_ent){
		identifier2ObjPropIndex.get(ident).setNamespace(ns_ent);
	}

	public void setObjectPropLabel(int ident, String label){
		identifier2ObjPropIndex.get(ident).setLabel(label);
	}

	public void addDomainClass4ObjectProperty(int ident, int domain_ident){
		identifier2ObjPropIndex.get(ident).addDomainClassIndex(domain_ident);
	}

	public void addRangeClass4ObjectProperty(int ident, int range_ident){
		identifier2ObjPropIndex.get(ident).addRangeClassIndex(range_ident);
	}

	public void addAlternativeObjectPropertyLabel(int ident, String altlabel){
		identifier2ObjPropIndex.get(ident).addAlternativeLabel(altlabel);
	}



	public Map<Integer, ClassIndex> getIdentifier2ClassIndexMap(){
		return identifier2ClassIndex;
	}


	public Map<Integer, DataPropertyIndex> getIdentifier2DataPropIndexMap(){
		return identifier2DataPropIndex;
	}


	public Map<Integer, ObjectPropertyIndex> getIdentifier2ObjectPropIndexMap(){
		return identifier2ObjPropIndex;
	}

	public Map<Integer, IndividualIndex> getIdentifier2IndividualIndexMap(){
		return identifier2IndividualIndex;
	}


	public ClassIndex getClassIndex(int ident){
		return identifier2ClassIndex.get(ident);
	}


	public DataPropertyIndex getDataPropertyIndex(int ident){
		return identifier2DataPropIndex.get(ident);
	}


	public ObjectPropertyIndex getObjectPropertyIndex(int ident){
		return identifier2ObjPropIndex.get(ident);
	}

	public IndividualIndex getIndividualIndex(int ident){
		return identifier2IndividualIndex.get(ident);
	}

	public int getSizeIndexClasses(){
		return identifier2ClassIndex.size();
	}




	public int getSizeDataProperties(){
		return identifier2DataPropIndex.size();
	}


	public int getSizeObjectProperties(){
		return identifier2ObjPropIndex.size();
	}


	public int getSizeIndexIndividuals(){
		return identifier2IndividualIndex.size();
	}


	public Set<Integer> getDomainDataProp4Identifier(int index){
		return identifier2DataPropIndex.get(index).getDomainClassIndexes();
	}

	public Set<Integer> getDomainObjProp4Identifier(int index){
		return identifier2ObjPropIndex.get(index).getDomainClassIndexes();
	}

	public Set<String> getRangeDataProp4Identifier(int index){
		return identifier2DataPropIndex.get(index).getRangeTypes();
	}

	public Set<Integer> getRangeObjProp4Identifier(int index){
		return identifier2ObjPropIndex.get(index).getRangeClassIndexes();			
	}


	public Set<Integer> getIndividualClassTypes4Identifier(int ident){
		return identifier2IndividualIndex.get(ident).getClassTypes();
	}




	public Set<Integer> getClassIdentifierSet(){
		return identifier2ClassIndex.keySet();
	}


	public Set<Integer> getDataPropIdentifierSet(){
		return identifier2DataPropIndex.keySet();
	}

	public Set<Integer> getObjectPropIdentifierSet(){
		return identifier2ObjPropIndex.keySet();
	}


	public Set<Integer> getIndividuaIdentifierSet(){
		return identifier2IndividualIndex.keySet();
	}

	/*public String getLabel4ConceptName(String name){ //useful if onto contains codes as concept names

		return getLabel4ConceptIndex(
				getIdentifier4ConceptName(name));



	}*/




	/**
	 * Returns -1 if preorder does not contain ident
	 * @param preDesc
	 * @return
	 */
	public int getIdentifier4PreorderDesc(int preDesc){ 
		if (preOrderDesc2Identifier.containsKey(preDesc))
			return preOrderDesc2Identifier.get(preDesc);
		else
			return -1;


	}


	public int getIdentifier4PreorderAnc(int preAnc){ 

		if (preOrderAnc2Identifier.containsKey(preAnc))
			return preOrderAnc2Identifier.get(preAnc);
		else
			return -1;

	}


	public String getProcessedName4ConceptIndex(int index){
		String name = getName4ConceptIndex(index).toLowerCase();

		if (name.indexOf("_")>0){ 
			return name.replaceAll("_", "");
		}
		else if (name.indexOf(" ")>0){ 
			return name.replaceAll(" ", "");
		}

		return name;

	}


	public String getName4ConceptIndex(int index){
		return identifier2ClassIndex.get(index).getEntityName();		
	}

	public String getName4DataPropIndex(int index){
		return identifier2DataPropIndex.get(index).getEntityName();		
	}

	public String getName4ObjPropIndex(int index){
		return identifier2ObjPropIndex.get(index).getEntityName();		
	}

	public String getName4IndividualIndex(int index){
		return identifier2IndividualIndex.get(index).getEntityName();		
	}


	public String getIRIStr4ConceptIndex(int index){		

		int onto_id = identifier2ClassIndex.get(index).getOntologyId();

		return identifier2ClassIndex.get(index).getIRI(getIRIStrOnto4Id(onto_id));		
	}

	public String getNameSpace4ConceptIndex(int index){		

		int onto_id = identifier2ClassIndex.get(index).getOntologyId();

		//if different from IRI onto
		if (identifier2ClassIndex.get(index).hasDifferentNamespace()){
			return identifier2ClassIndex.get(index).getNamespace();
		}
		else{
			//return getIRIStrOnto4Id(onto_id);
			return "";
		} 
	}


	public String getIRIStr4DataPropIndex(int index){		

		int onto_id = identifier2DataPropIndex.get(index).getOntologyId();

		return identifier2DataPropIndex.get(index).getIRI(getIRIStrOnto4Id(onto_id));		
	}

	public String getIRIStr4ObjPropIndex(int index){		

		int onto_id = identifier2ObjPropIndex.get(index).getOntologyId();

		return identifier2ObjPropIndex.get(index).getIRI(getIRIStrOnto4Id(onto_id));		
	}


	public String getIRIStr4IndividualIndex(int index){		

		int onto_id = identifier2IndividualIndex.get(index).getOntologyId();

		return identifier2IndividualIndex.get(index).getIRI(getIRIStrOnto4Id(onto_id));		
	}




	public IRI getIRI4ConceptIndex(int index){
		return IRI.create(getIRIStr4ConceptIndex(index));
	}

	public IRI getIRI4DataProptIndex(int index){
		return IRI.create(getIRIStr4DataPropIndex(index));
	}

	public IRI getIRI4ObjPropIndex(int index){
		return IRI.create(getIRIStr4ObjPropIndex(index));
	}

	public IRI getIRI4IndividualIndex(int index){
		return IRI.create(getIRIStr4IndividualIndex(index));
	}



	public String getLabel4ConceptIndex(int index){
		return identifier2ClassIndex.get(index).getLabel();
	}

	public String getLabel4DataPropIndex(int index){
		return identifier2DataPropIndex.get(index).getLabel();
	}

	public String getLabel4ObjPropIndex(int index){
		return identifier2ObjPropIndex.get(index).getLabel();
	}


	public String getLabel4IndividualIndex(int index){
		return identifier2IndividualIndex.get(index).getLabel();
	}



	public Set<String> getAlternativeLabels4ConceptIndex(int index){

		if (identifier2ClassIndex.get(index).hasAlternativeLabels())
			return identifier2ClassIndex.get(index).getAlternativeLabels();

		HashSet<String> set = new HashSet<String>();
		set.add(getLabel4ConceptIndex(index));
		return set;

	}

	public Set<String> getAlternativeLabels4IndividualIndex(int index){

		if (identifier2IndividualIndex.get(index).hasAlternativeLabels())
			return identifier2IndividualIndex.get(index).getAlternativeLabels();

		HashSet<String> set = new HashSet<String>();
		set.add(getLabel4IndividualIndex(index));
		return set;

	}

	public Set<String> getAlternativeLabels4ObjectPropertyIndex(int index){

		if (identifier2ObjPropIndex.get(index).hasAlternativeLabels())
			return identifier2ObjPropIndex.get(index).getAlternativeLabels();

		HashSet<String> set = new HashSet<String>();
		set.add(getLabel4ObjPropIndex(index));
		return set;

	}



	public Set<String> getAlternativeLabels4DataPropertyIndex(int index){

		if (identifier2DataPropIndex.get(index).hasAlternativeLabels())
			return identifier2DataPropIndex.get(index).getAlternativeLabels();

		HashSet<String> set = new HashSet<String>();
		set.add(getLabel4DataPropIndex(index));
		return set;

	}


	public boolean hasObjectPropertyAlternativeLabels(int index){

		return identifier2ObjPropIndex.get(index).hasAlternativeLabels();

	}


	public boolean hasDataPropertyAlternativeLabels(int index){

		return identifier2DataPropIndex.get(index).hasAlternativeLabels();

	}



	public boolean hasIndividualAlternativeLabels(int index){

		return identifier2IndividualIndex.get(index).hasAlternativeLabels();

	}


	public OWLClass getOWLClass4ConceptIndex(int index){
		return factory.getOWLClass(getIRI4ConceptIndex(index));
	}



	public OWLDataProperty getOWLDataProperty4PropertyIndex(int index){
		return factory.getOWLDataProperty(getIRI4DataProptIndex(index));
	}

	public OWLObjectProperty getOWLObjectProperty4PropertyIndex(int index){
		return factory.getOWLObjectProperty(getIRI4ObjPropIndex(index));
	}

	public OWLNamedIndividual getOWLNamedIndividual4IndividualIndex(int index){
		return factory.getOWLNamedIndividual(getIRI4IndividualIndex(index));
	}










	/*public Map<String, Set<Integer>> getInvertedFileSingle(){
		return invertedFileSingle;
	}*/

	public int getPreOrderNumber(int conceptIdentifier){
		return identifier2ClassIndex.get(conceptIdentifier).getNode().getDescOrder();
	}


	public int getPreOrderNumberReversed(int conceptIdentifier){
		return identifier2ClassIndex.get(conceptIdentifier).getNode().getAscOrder();
	}


	public int getTopologicalOrder(int conceptIdentifier){
		//return identifier2ClassIndex.get(conceptIdentifier).getNode().getTopolicalOrder();
		return identifier2ClassIndex.get(conceptIdentifier).getHierarchyLevel();
	}



	public Set<Interval> getIntervalsDescendants(int conceptIdentifier){
		return identifier2ClassIndex.get(conceptIdentifier).getNode().getDescIntervals();
	}

	public Set<Interval> getIntervalsAncestors(int conceptIdentifier){		
		return identifier2ClassIndex.get(conceptIdentifier).getNode().getAscIntervals();
	}



	public Set<Integer> getDirectDisjointClasses4Identifier(int conceptIdentifier){

		if (identifier2ClassIndex.get(conceptIdentifier).hasDirectDisjointClasses())
			return identifier2ClassIndex.get(conceptIdentifier).getDisjointClasses();		
		else
			return Collections.emptySet();
	}




	public Set<Integer> getDirectSuperClasses4Identifier(int conceptIdentifier, boolean module){

		if (module)
			return getDirectSuperClassesModule4Identifier(conceptIdentifier);
		else
			return getDirectSuperClassesOnto4Identifier(conceptIdentifier);
	}


	public Set<Integer> getDirectSubClasses4Identifier(int conceptIdentifier, boolean module){
		if (module)
			return getDirectSubClassesModule4Identifier(conceptIdentifier);
		else
			return getDirectSubClassesOnto4Identifier(conceptIdentifier);
	}


	private Set<Integer> getDirectSubClassesOnto4Identifier(int conceptIdentifier){


		if (identifier2ClassIndex.get(conceptIdentifier).hasDirectSubClasses()){
			return identifier2ClassIndex.get(conceptIdentifier).getDirectSubclasses();
		}
		else			
			return Collections.emptySet();
	}


	private Set<Integer> getDirectSuperClassesOnto4Identifier(int conceptIdentifier){

		if (identifier2ClassIndex.get(conceptIdentifier).hasDirectSuperClasses()){
			return identifier2ClassIndex.get(conceptIdentifier).getDirectSuperclasses();
		}
		else
			return Collections.emptySet();
	}


	private Set<Integer> getDirectSubClassesModule4Identifier(int conceptIdentifier){

		if (identifiersInModule.contains(conceptIdentifier)){
			return getDirectSubClassesOnto4Identifier(conceptIdentifier);
		}		
		else
			return Collections.emptySet();
	}


	private Set<Integer> getDirectSuperClassesModule4Identifier(int conceptIdentifier){

		if (identifiersInModule.contains(conceptIdentifier)){
			return getDirectSuperClassesOnto4Identifier(conceptIdentifier);
		}		
		else
			return Collections.emptySet();

	}


	public Set<Integer> getRoots4Identifier(int conceptIdentifier){
		if (identifier2ClassIndex.get(conceptIdentifier).hasRoots()){
			return identifier2ClassIndex.get(conceptIdentifier).getRoots();
		}
		else{
			return Collections.emptySet();
		}

	}


	private Map<Integer,Set<Integer>> ident2equivalents = new HashMap<Integer,Set<Integer>>();

	public Map<Integer,Set<Integer>> getEquivalentClasses(){

		if (ident2equivalents.size()>0)
			return ident2equivalents;

		for (int ident : identifier2ClassIndex.keySet()){
			if (identifier2ClassIndex.get(ident).hasEquivalentClasses()){
				ident2equivalents.put(ident, identifier2ClassIndex.get(ident).getEquivalentClasses());
			}
		}

		return ident2equivalents;
	}



	private Map<Integer,Set<Integer>> ident2subclasses_module = new HashMap<Integer,Set<Integer>>();

	private Map<Integer,Set<Integer>> getDirectSubClassesModule(){

		if (ident2subclasses_module.size()>0)
			return ident2subclasses_module;

		for (int ident : identifiersInModule){
			if (identifier2ClassIndex.get(ident).hasDirectSubClasses()){
				ident2subclasses_module.put(ident, identifier2ClassIndex.get(ident).getDirectSubclasses());
			}
		}

		return ident2subclasses_module;
	}


	private Map<Integer,Set<Integer>> ident2superclasses_module = new HashMap<Integer,Set<Integer>>();

	private Map<Integer,Set<Integer>> getDirectSuperClassesModule(){
		if (ident2superclasses_module.size()>0)
			return ident2superclasses_module;

		for (int ident : identifiersInModule){
			if (identifier2ClassIndex.get(ident).hasDirectSuperClasses()){
				ident2superclasses_module.put(ident, identifier2ClassIndex.get(ident).getDirectSuperclasses());
			}
		}

		return ident2superclasses_module;
	}




	private Map<Integer,Set<Integer>> ident2subclasses = new HashMap<Integer,Set<Integer>>();

	private Map<Integer,Set<Integer>> getDirectSubClassesOnto(){

		if (ident2subclasses.size()>0)
			return ident2subclasses;

		for (int ident : identifier2ClassIndex.keySet()){
			if (identifier2ClassIndex.get(ident).hasDirectSubClasses()){
				ident2subclasses.put(ident, identifier2ClassIndex.get(ident).getDirectSubclasses());
			}
		}


		return ident2subclasses;
	}


	private Map<Integer,Set<Integer>> ident2superclasses = new HashMap<Integer,Set<Integer>>();

	private Map<Integer,Set<Integer>> getDirectSuperClassesOnto(){
		if (ident2superclasses.size()>0)
			return ident2superclasses;

		for (int ident : identifier2ClassIndex.keySet()){
			if (identifier2ClassIndex.get(ident).hasDirectSuperClasses()){
				ident2superclasses.put(ident, identifier2ClassIndex.get(ident).getDirectSuperclasses());
			}
		}


		return ident2superclasses;
	}




	private Map<Integer,Set<Integer>> individual2classTypes = new HashMap<Integer,Set<Integer>>();

	public Map<Integer,Set<Integer>> getDirectIndividualClassTypes(){

		if (individual2classTypes.size()>0)
			return individual2classTypes;

		for (int ident : identifier2IndividualIndex.keySet()){
			if (identifier2IndividualIndex.get(ident).hasDirectClassTypes()){
				individual2classTypes.put(ident, identifier2IndividualIndex.get(ident).getClassTypes());
			}
		}


		return individual2classTypes;
	}





	public Map<Integer,Set<Integer>> getDirectSubClasses(boolean module){
		if (module)
			return getDirectSubClassesModule();
		else
			return getDirectSubClassesOnto();
	}


	public Map<Integer,Set<Integer>> getDirectSuperClasses(boolean module){
		if (module)
			return getDirectSuperClassesModule();
		else
			return getDirectSuperClassesOnto();
	}


	protected Map<Integer,Set<Integer>> ident2disjointclasses = new HashMap<Integer,Set<Integer>>();

	public Map<Integer,Set<Integer>> getDirectDisjointClasses(){
		if (ident2disjointclasses.size()>0)
			return ident2disjointclasses;

		for (int ident : identifier2ClassIndex.keySet()){
			if (identifier2ClassIndex.get(ident).hasDirectDisjointClasses()){
				ident2disjointclasses.put(ident, identifier2ClassIndex.get(ident).getDisjointClasses());
			}
		}

		return ident2disjointclasses;
	}





	//public int getIdentifier4ClassName(String name){
	//	return className2Identifier.get(name);
	//}


	/*public int getIdentifier4PreOrderDesc(int predesc){
		return preOrderDesc2Identifier.get(predesc);
	}


	public int getIdentifier4PreOrderAnc(int preanc){
		return preOrderAnc2Identifier.get(preanc);
	}*/


	//FOR the MODULE of mapped entities
	/**
	 * Sets projection (i.e. modules) for mapped entities	
	 * @param mapped_entities
	 * @deprecated Now we use the whole onto since we have already extracted overlapping
	 *
	 */
	public void setBigProjection4entities(Set<Integer> mapped_entities_identifiers){


		//This is not a module..

		identifiersInModule.clear();
		identifiersInModule.addAll(mapped_entities_identifiers);
		//Set<Integer> entities = new HashSet<Integer>(mapped_entities_identifiers);



		//Either we extract a limited level of subclasses or only from level topo_max/3
		int topo_max=0;
		float topo2extract;
		int maxsubclasses2extract;


		for (int ent_ident : mapped_entities_identifiers){
			if (getTopologicalOrder(ent_ident) > topo_max){
				topo_max=getTopologicalOrder(ent_ident);
			}
		}

		//topo2extract=(topo_max/3)+1;
		//maxsubclasses2extract=50;
		topo2extract=(topo_max/3);
		maxsubclasses2extract=75;



		for (int ent_ident : mapped_entities_identifiers){

			for (Interval interv : getIntervalsAncestors(ent_ident)){
				for (int pre=interv.getLeftBound(); pre<=interv.getRightBound(); pre++){

					//Get ide from preorder
					identifiersInModule.add(getIdentifier4PreorderAnc(pre)); //we add all parents!

				}					
			}


			//All children for an specific level onwards or if the number of subclasses is small
			if (getTopologicalOrder(ent_ident)>=topo2extract || getNumOfSubClasses4identifier(ent_ident)<maxsubclasses2extract){
				for (Interval interv : getIntervalsDescendants(ent_ident)){

					for (int pre=interv.getLeftBound(); pre<=interv.getRightBound(); pre++){

						//Get ide from preorder
						identifiersInModule.add(getIdentifier4PreorderDesc(pre)); //we add all children!

					}					
				}

			}
			else{				
				//Add 1 level of Subclasses	for top-like classes	
				for (int ide : getDirectSubClasses4Identifier(ent_ident, false)){
					identifiersInModule.add(ide); //we add 1 level of children
				}
			}

		}

		LogOutput.print("\t" + topo_max + "  " + topo2extract);		
		LogOutput.print("\tProjection size: " + identifiersInModule.size());

	}


	//FOR the MODULE of mapped entities
	/**
	 * Sets small projection for mapped entities	
	 * @param mapped_entities
	 * @deprecated Old method (see new in JointIndexManager)
	 */
	public void setSmallProjection4entities(Set<Integer> mapped_entities_identifiers){


		//This is not a module..


		//Set<Integer> entities = new HashSet<Integer>(mapped_entities_identifiers);
		identifiersInModule.clear();
		identifiersInModule.addAll(mapped_entities_identifiers);

		int ident_parent;

		for (int ent_ident : mapped_entities_identifiers){

			for (Interval interv : getIntervalsAncestors(ent_ident)){
				for (int pre=interv.getLeftBound(); pre<=interv.getRightBound(); pre++){

					ident_parent = getIdentifier4PreorderAnc(pre);

					//Get ide from preorder
					if (ident_parent>-1){ //Root node. It has a preorder but not identifier

						identifiersInModule.add(ident_parent); //we add all parents except the root						
					}


				}					
			}

			//Add 1 level of Subclasses			
			for (int ide : getDirectSubClasses4Identifier(ent_ident, false)){
				identifiersInModule.add(ide); //we add 1 level of children
			}

		}

		LogOutput.print("\tProjection size: " + identifiersInModule.size());

	}





	public int getNumOfSubClasses4identifier(int cIdent){

		int numsubclasses=0;

		for (Interval i : getIntervalsDescendants(cIdent)){
			numsubclasses+=i.getRightBound()-i.getLeftBound();
		}
		return numsubclasses;
	}




	/**
	 * Preorder of class 1 must be included in one of the class 2 intervals
	 * @param cIdent1
	 * @param cIdent2
	 * @return
	 */
	public boolean isSubClassOf(int cIdent1, int cIdent2){

		calls_tax_question++;
		if(measureTime)
			init = Calendar.getInstance().getTimeInMillis();

		int preorder1 = getPreOrderNumber(cIdent1);

		for (Interval i2 : getIntervalsDescendants(cIdent2)){		
			if (i2.containsIndex(preorder1)){

				fin = Calendar.getInstance().getTimeInMillis();
				time_tax_question+=(float)((double)fin-(double)init)/1000.0;

				return true;
			}
		}

		if(measureTime){
			fin = Calendar.getInstance().getTimeInMillis();
			time_tax_question+=(float)((double)fin-(double)init)/1000.0;
		}
		
		return false;

	}


	/**
	 * Preorder of class 1 must be included in one of the class 2 intervals
	 * @param cIdent1
	 * @param cIdent2
	 * @return
	 */
	private boolean isSubClassOfInverseTax(int cIdent1, int cIdent2){

		calls_tax_question++;
		if(measureTime)
			init = Calendar.getInstance().getTimeInMillis();

		int preorder1 = getPreOrderNumberReversed(cIdent1);

		for (Interval i2 : getIntervalsAncestors(cIdent2)){		
			if (i2.containsIndex(preorder1)){

				fin = Calendar.getInstance().getTimeInMillis();
				time_tax_question+=(float)((double)fin-(double)init)/1000.0;

				return true;
			}
		}
		if(measureTime){
			fin = Calendar.getInstance().getTimeInMillis();
			time_tax_question+=(float)((double)fin-(double)init)/1000.0;
		}
		return false;

	}


	public boolean isSuperClassOf(int cIdent1, int cIdent2){
		return isSubClassOfInverseTax(cIdent1, cIdent2);
	}





	/**
	 * Given two class identifiers checks if they have same pr-eorder.
	 * (See indexes propagation for equivalences)
	 * @param cIdent1
	 * @param cIdent2
	 * @return
	 */
	public boolean areEquivalentClasses(int cIdent1, int cIdent2){

		if (getPreOrderNumber(cIdent1)==getPreOrderNumber(cIdent2)){
			return true;
		}

		return false;
	}


	/**
	 * 
	 * Given two class identifiers checks if they same direct parents
	 * 
	 * @param cIdent1
	 * @param cIdent2
	 * @return
	 */
	public boolean areSiblings(int cIdent1, int cIdent2){

		//Do not performs intersections since we need to delete

		for (int parent1 : getDirectSuperClassesOnto4Identifier(cIdent1)){
			if (getDirectSuperClassesOnto4Identifier(cIdent2).contains(parent1)){
				return true;
			}
		}

		return false;

	}


	/**
	 * Given two identifiers checks if they are disjoint.
	 * That is, their pre-order index are included in disjoint intervals, respectively.
	 * @param cIdent1
	 * @param cIdent2
	 * @return
	 */
	public boolean areDisjoint(int cIdent1, int cIdent2){

		calls_disj_question++;
		if(measureTime)
			init = Calendar.getInstance().getTimeInMillis();

		//it will only be necessary to compare one of the intervals for each entity
		int preorder1 = getPreOrderNumber(cIdent1);
		int preorder2 = getPreOrderNumber(cIdent2);


		for (Interval disj_int1 : interval2disjointIntervals.keySet()){

			if (disj_int1.containsIndex(preorder1)){

				for (Interval disj_int2 : interval2disjointIntervals.get(disj_int1)){

					if (disj_int2.containsIndex(preorder2)){
						if(measureTime){
							fin = Calendar.getInstance().getTimeInMillis();
							time_disj_question+=(float)((double)fin-(double)init)/1000.0;
						}
						return true;
					}	
				}	
			}
		}
		
//		if(identifier2ClassIndex.get(cIdent1).hasDirectDisjointClasses() &&
//			identifier2ClassIndex.get(cIdent1).getDisjointClasses().contains(cIdent2))
//			System.err.println(
//					getOWLClass4ConceptIndex(cIdent1) + " has " + 
//							getOWLClass4ConceptIndex(cIdent2) 
//							+ " as disjont but intervals don't match");
//		if(identifier2ClassIndex.get(cIdent2).hasDirectDisjointClasses() &&
//				identifier2ClassIndex.get(cIdent2).getDisjointClasses().contains(cIdent1))
//			System.err.println(
//					getOWLClass4ConceptIndex(cIdent2) + " has " + 
//							getOWLClass4ConceptIndex(cIdent1) 
//							+ " as disjont but intervals don't match");

		if(measureTime){
			fin = Calendar.getInstance().getTimeInMillis();
			time_disj_question+=(float)((double)fin-(double)init)/1000.0;
		}
		return false; 
	}


	/**
	 * Checks if concept 1 (cIdent1) is disjoint with any of the descendants of concept 2 (cIdent2) 
	 * 
	 * @param cIdent1
	 * @param cIdent2
	 * @return
	 */
	public boolean isDisjointWithDescendants(int cIdent1, int cIdent2){

		calls_disj_question++;
		if(measureTime)
			init = Calendar.getInstance().getTimeInMillis();


		int preorder1 = getPreOrderNumber(cIdent1);
		Set<Interval> descendants = getIntervalsDescendants(cIdent2);



		for (Interval disj_int1 : interval2disjointIntervals.keySet()){

			for (Interval desc : descendants){

				//SOme descendants involve in disjointness
				if (disj_int1.hasNonEmptyIntersectionWith(desc)){

					for (Interval disj_int2 : interval2disjointIntervals.get(disj_int1)){

						//Our concepts is inconflict with some of the descendants
						//Thus cIdent1 and cIdent2 cannot be matched!! 
						if (disj_int2.containsIndex(preorder1)){
							if(measureTime){
								fin = Calendar.getInstance().getTimeInMillis();
								time_disj_question+=(float)((double)fin-(double)init)/1000.0;
							}
							return true;

						}

					}

					//break; //we do not need to check next desc1 since no desc2 intersects with disj_int2			

				}

			}
		}	


		//Necessary to check both sides!!
		for (Interval disj_int1 : interval2disjointIntervals.keySet()){

			if (disj_int1.containsIndex(preorder1)){

				for (Interval disj_int2 : interval2disjointIntervals.get(disj_int1)){

					//Our concepts is inconflict with some of the descendants
					//Thus cIdent1 and cIdent2 cannot be matched!! 
					for (Interval desc : descendants){

						//SOme descendants involve in disjointness
						if (disj_int2.hasNonEmptyIntersectionWith(desc)){

							if(measureTime){
								fin = Calendar.getInstance().getTimeInMillis();
								time_disj_question+=(float)((double)fin-(double)init)/1000.0;
							}
							return true;

						}

					}

				}

			}
		}	

		if(measureTime){
			fin = Calendar.getInstance().getTimeInMillis();
			time_disj_question+=(float)((double)fin-(double)init)/1000.0;
		}
		return false; 
	}

	/**
	 * Given two identifiers checks if they are partially disjoint.
	 * 
	 * Very aggressive. Since the concepts may still be matcheable.
	 * 
	 * Cases that are not directly disjoint but their intervals intersects with disjoint intervals
	 * 
	 * 
	 * @param cIdent1
	 * @param cIdent2
	 * @return
	 */
	public boolean arePartiallyDisjoint(int cIdent1, int cIdent2){
		
		calls_disj_question++;
		if(measureTime)
			init = Calendar.getInstance().getTimeInMillis();

		Set<Interval> descendants1 = getIntervalsDescendants(cIdent1);
		Set<Interval> descendants2 = getIntervalsDescendants(cIdent2);


		for (Interval disj_int1 : interval2disjointIntervals.keySet()){

			for (Interval desc1 : descendants1){

				if (disj_int1.hasNonEmptyIntersectionWith(desc1)){

					for (Interval disj_int2 : interval2disjointIntervals.get(disj_int1)){

						for (Interval desc2 : descendants2){

							if (disj_int2.hasNonEmptyIntersectionWith(desc2)){
								return true;
							}
						}					

					}
					break; //we do not need to check next desc1 since no desc2 intersects with disj_int2			
				}
			}
		}			
		if(measureTime){
			fin = Calendar.getInstance().getTimeInMillis();
			time_disj_question+=(float)((double)fin-(double)init)/1000.0;
		}
		return false; 
	}

	public Set<Integer> getUnsatisfiableClassesILS(){
		return unsatisfiableClassesILS;
	}


	/**
	 * True if their desc intervals intersects
	 * @param cIdent1
	 * @param cIdent2
	 * @return
	 */
	public boolean areConceptsSharingDescendants(int cIdent1, int cIdent2){

		for (Interval i1 : getIntervalsDescendants(cIdent1)){
			for (Interval i2 : getIntervalsDescendants(cIdent2)){

				if (i1.hasNonEmptyIntersectionWith(i2)){
					return true;
				}
			}
		}
		return false;		
	}

	/**
	 * True if their anc intervals intersects
	 * @param cIdent1
	 * @param cIdent2
	 * @return
	 */
	public boolean areConceptsSharingAncestors(int cIdent1, int cIdent2){

		for (Interval i1 : getIntervalsAncestors(cIdent1)){
			for (Interval i2 : getIntervalsAncestors(cIdent2)){

				if (i1.hasNonEmptyIntersectionWith(i2)){
					return true;
				}
			}
		}
		return false;		
	}


	public abstract Set<Integer> getScope4Identifier_Big(int ide);

	public abstract Set<Integer> getScope4Identifier_Condifence(int ide);

	public abstract Set<Integer> getScope4Identifier_Expansion(int ide);

	public abstract void setSmallProjection4MappedEntities(Set<Integer> mapped_entities_identifiers);

	public abstract Set<Integer> getSubsetOfSuperClasses4Identifier(int ide);

	public abstract Set<Integer> getSubsetOfSubClasses4Identifier(int ide);


	//Not used anymore
	//public abstract boolean belong2IndependentBranches(int cIdent1, int cIdent2);
	//public abstract boolean areConceptsSharingRoots(int cIdent1, int cIdent2);



}


class Comparator4String2Int implements Comparator<Pair<String, Integer>>
{
	@Override
	public int compare(Pair<String, Integer> o1, Pair<String, Integer> o2) {
		return o2.getValue() - o1.getValue();
	}

}

