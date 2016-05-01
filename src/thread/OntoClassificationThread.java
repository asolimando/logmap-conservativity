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

import java.util.concurrent.Callable;

import org.semanticweb.owlapi.reasoner.InconsistentOntologyException;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import util.FileUtil;

public class OntoClassificationThread implements Callable<Boolean> {
	
	OWLReasoner reasoner;
	boolean disj = false;
	
	public OntoClassificationThread(OWLReasoner reasoner){
		this.reasoner = reasoner;
		//this.disj = disj;
	}
	
	public OWLReasoner getReasoner(){
		return reasoner;
	}
	
	@Override
	public Boolean call() {
		try {
			if(disj)
				reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY,
						InferenceType.DISJOINT_CLASSES);
			else
				reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
			
		}
		catch(org.mindswap.pellet.exceptions.InconsistentOntologyException | 
				InconsistentOntologyException | 
				java.lang.OutOfMemoryError | 
				StackOverflowError e){
			FileUtil.writeErrorLogAndConsole(e.getMessage());
			return false;
		}
		return true;
	}
}