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

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import auxStructures.Pair;

import logmap.LogMapWrapper;
import uk.ac.ox.krr.logmap2.indexing.JointIndexManager;
import uk.ac.ox.krr.logmap2.indexing.OntologyProcessing;
import util.OntoUtil;

public class ConservativityViolationDetectionThread 
						implements Callable<List<Pair<Integer>>> {
	
	JointIndexManager origIdx, alignIdx;
	OntologyProcessing origProc, alignProc;
	int ontoId;
	boolean rootViolations,alsoEquiv,suppressOutput;
	
	OWLOntology inputOnto, alignOnto;
	OWLReasoner alignR;
	
	public ConservativityViolationDetectionThread(JointIndexManager origIdx, 
			OntologyProcessing origProc, JointIndexManager alignIdx, 
			OntologyProcessing alignProc, int ontoId, boolean rootViolations, 
			boolean alsoEquiv, boolean suppressOutput, 
			OWLOntology inputOnto, OWLOntology alignOnto, OWLReasoner alignR 
			){
		this.alignIdx = alignIdx;
		this.origIdx = origIdx;
		this.origProc= origProc;
		this.alignProc = alignProc;
		this.ontoId = ontoId;
		this.rootViolations = rootViolations;
		this.alsoEquiv = alsoEquiv;
		this.suppressOutput = suppressOutput;
		
		this.inputOnto = inputOnto;
		this.alignOnto = alignOnto;
		this.alignR = alignR;
	}
	
	@Override
	public List<Pair<Integer>> call() {

		List<Pair<Integer>> result = null;				

		if(rootViolations && inputOnto != null && alignOnto != null && 
				alignR != null){
			
			result = LogMapWrapper.detectConservativityViolationWithSemanticIndex(
					origIdx, origProc, alignIdx, alignProc, false, 
					alsoEquiv, ontoId, suppressOutput);
			
			List<Pair<Integer>> knownDirect = 
					LogMapWrapper.detectConservativityViolationWithSemanticIndex(
					origIdx, origProc, alignIdx, alignProc, rootViolations, 
					alsoEquiv, ontoId, suppressOutput);
			result.removeAll(knownDirect);
			
			List<Pair<OWLClass>> dirR = 
					OntoUtil.explanationDetectionDirectViolations(inputOnto, alignOnto, 
							result, origIdx, ontoId, alignR, suppressOutput);
			
			List<Pair<Integer>> dirI = 
					LogMapWrapper.getPairsOfIdentifiersFromPairsOfClasses(dirR,
							alignProc);
			
			result.clear();
			result.addAll(knownDirect);
			result.addAll(dirI);
		}
		else {
			result = LogMapWrapper.detectConservativityViolationWithSemanticIndex(
					origIdx, origProc, alignIdx, alignProc, rootViolations, 
					alsoEquiv, ontoId, suppressOutput);
		}
		
		return result;
	}
}