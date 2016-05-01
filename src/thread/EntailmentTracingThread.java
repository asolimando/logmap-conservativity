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

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import ontology.ExplanationProgMonitor;

import org.semanticweb.elk.owlapi.ElkReasoner;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owl.explanation.api.ExplanationGenerator;
import org.semanticweb.owl.explanation.api.ExplanationGeneratorFactory;
import org.semanticweb.owl.explanation.impl.blackbox.Configuration;
import org.semanticweb.owl.explanation.impl.blackbox.EntailmentCheckerFactory;
import org.semanticweb.owl.explanation.impl.blackbox.checker.BlackBoxExplanationGeneratorFactory;
import org.semanticweb.owl.explanation.impl.blackbox.checker.SatisfiabilityEntailmentCheckerFactory;
import org.semanticweb.owl.explanation.impl.laconic.LaconicExplanationGeneratorFactory;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapitools.proofs.exception.ProofGenerationException;

import util.FileUtil;
import util.OntoUtil;

public class EntailmentTracingThread extends AbstractExplanationThread {
	
	private int timeout, printEach;
	private OWLAxiom ax;
	private OWLReasoner r;
	private boolean suppressOutput;
	
	public EntailmentTracingThread(OWLAxiom ax, OWLReasoner r, int limit, 
			int timeout, boolean suppressOutput, int printEach){
		
		this.r = r;
		this.limit = limit;
		this.timeout = timeout;
		this.ax = ax;
		this.suppressOutput = suppressOutput;
		this.printEach = printEach;
	}
	
	@Override
	public Set<Explanation<OWLAxiom>> call() throws ProofGenerationException, 
	TimeoutException, ExecutionException, InterruptedException {
		return OntoUtil.getTracingForAxiom(r, ax, limit, timeout, 
				suppressOutput, printEach);
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		org.semanticweb.elk.reasoner.Reasoner internalReasoner = 
				((ElkReasoner) r).getInternalReasoner();
		if(!internalReasoner.isInterrupted()){
			FileUtil.writeLogAndConsole("Interrupting ELK");
			internalReasoner.interrupt();
			// otherwise next request will fail!
			internalReasoner.clearInterrupt();
		}
		return true;
	}
}