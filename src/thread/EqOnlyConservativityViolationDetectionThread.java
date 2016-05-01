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
package thread;

import java.util.List;
import java.util.concurrent.Callable;

import logmap.LogMapWrapper;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import auxStructures.Pair;

import uk.ac.ox.krr.logmap2.indexing.JointIndexManager;
import uk.ac.ox.krr.logmap2.indexing.OntologyProcessing;
import util.OntoUtil;

public class EqOnlyConservativityViolationDetectionThread 
	implements Callable<List<Pair<Integer>>> {

	JointIndexManager origIdx, alignIdx;
	OntologyProcessing origProc, alignProc;
	int ontoId;
	boolean suppressOutput;
	
	public EqOnlyConservativityViolationDetectionThread(
			JointIndexManager origIdx, OntologyProcessing origProc,
			JointIndexManager alignIdx, OntologyProcessing alignProc,
			int ontoId, boolean suppressOutput) {
		this.alignIdx = alignIdx;
		this.origIdx = origIdx;
		this.origProc= origProc;
		this.alignProc = alignProc;
		this.ontoId = ontoId;
		this.suppressOutput = suppressOutput;
	}

	@Override
	public List<Pair<Integer>> call() {
		return LogMapWrapper.detectEquivConservativityViolationWithSemanticIndex(
						origIdx, origProc, alignIdx, alignProc, ontoId, 
						suppressOutput);
	}
}
