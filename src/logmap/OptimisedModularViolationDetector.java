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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;

import com.google.common.collect.Sets;

import comparator.ViolationComparatorLeaves;

import uk.ac.ox.krr.logmap2.indexing.JointIndexManager;
import uk.ac.ox.krr.logmap2.indexing.OntologyProcessing;
import uk.ac.ox.krr.logmap2.indexing.labelling_schema.Interval;
import uk.ac.ox.krr.logmap2.indexing.labelling_schema.IntervalForNode;
import uk.ac.ox.krr.logmap2.indexing.labelling_schema.IntervalTree;
import util.FileUtil;
import auxStructures.Pair;

public class OptimisedModularViolationDetector extends
		AbstractModularDirectViolationDetector {

	private List<Integer> leaves;
	private IntervalTree itree;
	private Map<Integer, Integer> nodeToLeaf;

	public OptimisedModularViolationDetector(OWLOntology fstOnto,
			OWLOntology sndOnto, OWLOntology alignOnto,
			Pair<List<Pair<Integer>>> viols, JointIndexManager origIdx,
			JointIndexManager alignIdx, OntologyProcessing alignProc) {
		super(fstOnto, sndOnto, alignOnto, viols, origIdx, alignIdx, alignProc);
		
		leaves = new ArrayList<>(origIdx.getLeaves());

		itree = new IntervalTree();

		nodeToLeaf = new HashMap<>();
				
		FileUtil.writeLogAndConsole(leaves.size() + " leave(s)");

		int uncovered = 0;
		
		for (Integer l : leaves)
			for (Interval interval : origIdx.getIntervalsAncestors(l))
				itree.addInterval(new IntervalForNode(interval.getLeftBound(), 
						interval.getRightBound(), l));

		for (Pair<Integer> p : violId1) {
			if(nodeToLeaf.containsKey(p.getFirst()))
				continue;
			
			List<Integer> leavesId = itree.get(p.getFirst());
			Collections.sort(leavesId);
			if(!leavesId.isEmpty()){
				for (Integer lid : leavesId)
					if(origIdx.isSubClassOf(lid, p.getFirst())){
						nodeToLeaf.put(p.getFirst(),lid);
						break;
					}
				nodeToLeaf.put(p.getFirst(),p.getFirst());
				uncovered++;
			}
			else {
				nodeToLeaf.put(p.getFirst(),p.getFirst());
				uncovered++;
			}
		}

		for (Pair<Integer> p : violId2) {
			if(nodeToLeaf.containsKey(p.getFirst()))
				continue;

			List<Integer> leavesId = itree.get(p.getFirst());
			Collections.sort(leavesId);
			if(!leavesId.isEmpty()){
				for (Integer lid : leavesId)
					if(origIdx.isSubClassOf(lid, p.getFirst())){
						nodeToLeaf.put(p.getFirst(),lid);
						break;
					}
				nodeToLeaf.put(p.getFirst(),p.getFirst());
				uncovered++;
			}
			else {
				nodeToLeaf.put(p.getFirst(),p.getFirst());
				uncovered++;
			}
		}
		
		FileUtil.writeLogAndConsole(uncovered + "/" + 
				(violId1.size() + violId2.size()) + " uncovered");
		
		vc = new ViolationComparatorLeaves(nodeToLeaf);
	}

				
	@Override
	protected boolean canReuseModule(int id, int moduleIdx) {
//		System.out.print("PRE " +
//				LogMapWrapper.getOWLClassFromIndex(moduleIdx, origIdx) + 
//				" isA " + LogMapWrapper.getOWLClassFromIndex(id, origIdx) 
//				+ "? ");
		boolean res = origIdx.isSubClassOf(moduleIdx, id);
//		System.out.println(res);
		return res;
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
		return Sets.newHashSet(LogMapWrapper.getOWLClassFromIndex(moduleIdx,origIdx));
	}
	
}
