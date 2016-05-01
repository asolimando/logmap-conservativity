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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

import comparator.ViolationComparatorLeaves;
import comparator.ViolationComparatorSiblingLeaves;

import uk.ac.ox.krr.logmap2.indexing.JointIndexManager;
import uk.ac.ox.krr.logmap2.indexing.OntologyProcessing;
import uk.ac.ox.krr.logmap2.indexing.labelling_schema.Interval;
import uk.ac.ox.krr.logmap2.indexing.labelling_schema.IntervalForNode;
import uk.ac.ox.krr.logmap2.indexing.labelling_schema.IntervalTree;
import util.FileUtil;
import auxStructures.Pair;

public class OptimisedAlignModularViolationDetector extends
AbstractModularDirectViolationDetector {

	private List<Integer> leaves;
	private IntervalTree itree;
	private Map<Integer, Integer> nodeToLeaf;

	private Map<Integer,Integer> seedPosition = Maps.newHashMap();
	private Multimap<Integer,Integer> positionSeed;
	
	public OptimisedAlignModularViolationDetector(OWLOntology fstOnto,
			OWLOntology sndOnto, OWLOntology alignOnto,
			Pair<List<Pair<Integer>>> viols, JointIndexManager origIdx,
			JointIndexManager alignIdx, OntologyProcessing alignProc) {
		super(fstOnto, sndOnto, alignOnto, viols, origIdx, alignIdx, alignProc);

		leaves = new ArrayList<>(alignIdx.getLeaves());

		itree = new IntervalTree();

		nodeToLeaf = new HashMap<>();

		FileUtil.writeLogAndConsole(leaves.size() + " leave(s)");

		int uncovered = 0;
		int leavesViol = 0;

		for (Integer l : leaves)
			for (Interval interval : alignIdx.getIntervalsAncestors(l))
				itree.addInterval(new IntervalForNode(interval.getLeftBound(), 
						interval.getRightBound(), l));

		for (Pair<Integer> p : violId1) {
			LogMapWrapper.convertIndex(p, alignProc, origIdx);

			if(nodeToLeaf.containsKey(p.getFirst()))
				continue;

			List<Integer> leavesId = itree.get(p.getFirst());
			Collections.sort(leavesId);

			if(!leavesId.isEmpty()){
				for (Integer lid : leavesId)
					if(alignIdx.isSubClassOf(lid, p.getFirst())){
						nodeToLeaf.put(p.getFirst(),lid);
						break;
					}
				nodeToLeaf.put(p.getFirst(),p.getFirst());
				uncovered++;
				if(leaves.contains(p.getFirst()))
					++leavesViol;
			}
			else {
				nodeToLeaf.put(p.getFirst(),p.getFirst());
				uncovered++;
				if(leaves.contains(p.getFirst()))
					++leavesViol;
			}
		}

		for (Pair<Integer> p : violId2) {
			LogMapWrapper.convertIndex(p, alignProc, origIdx);

			if(nodeToLeaf.containsKey(p.getFirst()))
				continue;

			List<Integer> leavesId = itree.get(p.getFirst());
			Collections.sort(leavesId);

			if(!leavesId.isEmpty()){
				for (Integer lid : leavesId)
					if(alignIdx.isSubClassOf(lid, p.getFirst())){
						nodeToLeaf.put(p.getFirst(),lid);
						break;
					}
				nodeToLeaf.put(p.getFirst(),p.getFirst());
				uncovered++;
				if(leaves.contains(p.getFirst()))
					++leavesViol;
			}
			else {
				nodeToLeaf.put(p.getFirst(),p.getFirst());
				uncovered++;

				if(leaves.contains(p.getFirst()))
					++leavesViol;
			}
		}

		FileUtil.writeLogAndConsole(uncovered + "/" + 
				(violId1.size() + violId2.size()) + " uncovered");

		FileUtil.writeLogAndConsole(leavesViol  + " violation for leaves");

		int posCount = 0;

		ext: 
			for (Integer seed : nodeToLeaf.values()) {
				for (Integer procSeed : seedPosition.keySet()) {
					if(alignIdx.areConceptsSharingAncestors(seed, procSeed)){
					//if(alignIdx.areSiblings(seed, procSeed)){
						seedPosition.put(seed, seedPosition.get(procSeed));
						continue ext;
					}
				}
				seedPosition.put(seed, posCount++);
			}
		
		positionSeed = Multimaps.invertFrom(Multimaps.forMap(seedPosition), 
						ArrayListMultimap.<Integer,Integer>create());
						
		vc = new ViolationComparatorSiblingLeaves(nodeToLeaf, seedPosition);				

		FileUtil.writeLogAndConsole(posCount + " module(s) will be used");
		
		//		vc = new ViolationComparatorLeaves(nodeToLeaf);
	}

	@Override
	protected Pair<OWLClass> getOWLClass(Pair<Integer> p){
		return LogMapWrapper.getOWLClassFromIndexPair(p, alignIdx);
	}

	@Override
	protected boolean canReuseModule(int id,int moduleIdx) {
		//		System.out.print("PRE" + 
		//				LogMapWrapper.getOWLClassFromIndex(moduleIdx, alignIdx) + 
		//				" isA " + LogMapWrapper.getOWLClassFromIndex(id, alignIdx) 
		//				+ "? ");
//		boolean res = alignIdx.isSubClassOf(moduleIdx, id);
//		//		System.out.println(res);
//		return res;
		
		return seedPosition.get(moduleIdx) == seedPosition.get(id);
	}

	@Override
	protected int nextModuleIndex(int id) {
		int moduleIdx = nodeToLeaf.get(id);
		//		System.out.println("NEXT " + 
		//				LogMapWrapper.getOWLClassFromIndex(moduleIdx, origIdx) + 
		//				" isA " + LogMapWrapper.getOWLClassFromIndex(id, origIdx) 
		//				+ "? " + origIdx.isSubClassOf(moduleIdx, id));
		return moduleIdx;
	}

	@Override
	protected boolean sameModule(int id,int moduleIdx) {
		return moduleIdx == nodeToLeaf.get(id);
	}

	@Override
	protected Set<OWLClass> getSeedForModule(int moduleIdx, Pair<OWLClass> v) {
		Set<OWLClass> res = Sets.newHashSet();
		
		for (Integer idx : positionSeed.get(seedPosition.get(moduleIdx)))
			res.add(LogMapWrapper.getOWLClassFromIndex(idx,alignIdx));
		
		return res;
//		return Sets.newHashSet(LogMapWrapper.getOWLClassFromIndex(moduleIdx,alignIdx));
	}

}
