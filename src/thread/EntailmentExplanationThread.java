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

import ontology.ExplanationProgMonitor;

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

public class EntailmentExplanationThread extends AbstractExplanationThread {
	
	private ExplanationGenerator<OWLAxiom> exManager;
	private ExplanationProgMonitor explProgMon;
	private OWLAxiom ax;
	
	public EntailmentExplanationThread(OWLOntology alignOnto, OWLAxiom ax,
			OWLReasonerFactory reasonerFactory, int limit, 
			boolean suppressOutput, int timeout, boolean laconic, int printEach){
		this(alignOnto.getAxioms(), ax, reasonerFactory, limit, 
				suppressOutput, timeout, laconic, printEach);
	}
	
	public EntailmentExplanationThread(Set<OWLAxiom> axioms, OWLAxiom ax,
			OWLReasonerFactory reasonerFactory, int limit, 
			boolean suppressOutput, int timeout, boolean laconic, int printEach){

		EntailmentCheckerFactory<OWLAxiom> ecf = 
				new SatisfiabilityEntailmentCheckerFactory(
						reasonerFactory,true,timeout);
		
		this.explProgMon = new ExplanationProgMonitor(suppressOutput, printEach);
		
		ExplanationGeneratorFactory<OWLAxiom> explGenFactory = 
				laconic ? 
				new LaconicExplanationGeneratorFactory<OWLAxiom>(
						new BlackBoxExplanationGeneratorFactory<OWLAxiom>(
								new Configuration<OWLAxiom>(ecf))) : 
									new BlackBoxExplanationGeneratorFactory<OWLAxiom>(
											new Configuration<OWLAxiom>(ecf));
		
		this.exManager = explGenFactory.createExplanationGenerator(axioms, 
				explProgMon);
		
		this.limit = limit;
		this.ax = ax;
	}
	
	@Override
	public Set<Explanation<OWLAxiom>> call() {
		if(limit == 0)
			return exManager.getExplanations(ax);
		
		return exManager.getExplanations(ax, limit);
	}
	
	@Override
	public boolean cancel(boolean mayInterruptIfRunning){
		explProgMon.cancel();
		return true;
	}
}