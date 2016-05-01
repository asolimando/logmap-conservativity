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
import java.util.HashSet;
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

public class BiModuleViolationDetector extends AbstractModularDirectViolationDetector {

	private Pair<HashSet<OWLClass>> seeds = new Pair<HashSet<OWLClass>>(
					new HashSet<OWLClass>(), new HashSet<OWLClass>());
	
	public BiModuleViolationDetector(OWLOntology fstOnto,
			OWLOntology sndOnto, OWLOntology alignOnto,
			Pair<List<Pair<Integer>>> viols, JointIndexManager origIdx,
			JointIndexManager alignIdx, OntologyProcessing alignProc) {
		super(fstOnto, sndOnto, alignOnto, viols, origIdx, alignIdx, alignProc);

		Set<OWLClass> seed1 = seeds.getFirst(), 
				seed2 = seeds.getSecond();
		
		for (Pair<Integer> p : violId1)
			seed1.add(LogMapWrapper.getOWLClassFromIndex(p.getFirst(),alignIdx));
		
		for (Pair<Integer> p : violId2)
			seed2.add(LogMapWrapper.getOWLClassFromIndex(p.getFirst(),alignIdx));
	}

	@Override
	protected Pair<OWLClass> getOWLClass(Pair<Integer> p){
		return LogMapWrapper.getOWLClassFromIndexPair(p, alignIdx);
	}

	@Override
	protected boolean canReuseModule(int id,int moduleIdx) {		
		return true;
	}

	@Override
	protected int nextModuleIndex(int id) {
		return id;
	}

	@Override
	protected boolean sameModule(int id,int moduleIdx) {
		return true;
	}

	@Override
	protected Set<OWLClass> getSeedForModule(int moduleIdx, Pair<OWLClass> v) {
		return seeds.getFirst().contains(v.getFirst()) ? 
				seeds.getFirst() : seeds.getSecond();
	}

}
