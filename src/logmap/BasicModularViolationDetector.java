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

import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;

import com.google.common.collect.Sets;

import comparator.ViolationComparatorSubClass;

import uk.ac.ox.krr.logmap2.indexing.JointIndexManager;
import uk.ac.ox.krr.logmap2.indexing.OntologyProcessing;
import auxStructures.Pair;

public class BasicModularViolationDetector extends
		AbstractModularDirectViolationDetector {

	public BasicModularViolationDetector(OWLOntology fstOnto,
			OWLOntology sndOnto, OWLOntology alignOnto,
			Pair<List<Pair<Integer>>> viols, JointIndexManager origIdx,
			JointIndexManager alignIdx, OntologyProcessing alignProc) {
		super(fstOnto, sndOnto, alignOnto, viols, origIdx, alignIdx, alignProc);
		
		vc = new ViolationComparatorSubClass(origIdx);
	}

	@Override
	protected boolean canReuseModule(int id, int moduleIdx) {
		return origIdx.isSubClassOf(moduleIdx, id);
	}

	@Override
	protected int nextModuleIndex(int id) {
		return id;
	}

	@Override
	protected boolean sameModule(int id, int moduleIdx) {
		return moduleIdx == id;
	}

	@Override
	protected Set<OWLClass> getSeedForModule(int moduleIdx, Pair<OWLClass> v) {
		return Sets.newHashSet(v.getFirst());
	}

}
