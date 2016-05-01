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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import ontology.UnsatExplanationProgressMonitor;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owl.explanation.api.ExplanationGenerator;
import org.semanticweb.owl.explanation.api.ExplanationGeneratorFactory;
import org.semanticweb.owl.explanation.api.ExplanationGeneratorInterruptedException;
import org.semanticweb.owl.explanation.impl.blackbox.Configuration;
import org.semanticweb.owl.explanation.impl.blackbox.EntailmentCheckerFactory;
import org.semanticweb.owl.explanation.impl.blackbox.checker.BlackBoxExplanationGeneratorFactory;
import org.semanticweb.owl.explanation.impl.blackbox.checker.SatisfiabilityEntailmentCheckerFactory;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import com.google.common.collect.Sets;

import util.FileUtil;
import util.OntoUtil;

public class SatExplanationThread implements Callable<Set<OWLAxiom>> {
	
	private int limit;
	private OWLClass c;
	private ExplanationGenerator<OWLAxiom> exManager;
	private UnsatExplanationProgressMonitor explProgMon;
	private OWLReasonerFactory reasonerFactory;
	private Map<OWLAxiom, Double> mappingsMap;
	private OWLOntology alignOnto;
	private boolean suppressOutput;
	private long timeout = 0;
	
	public SatExplanationThread(OWLOntology alignOnto, 
			Map<OWLAxiom, Double> mappingsMap, OWLClass c, 
			OWLReasonerFactory reasonerFactory, int limit, 
			boolean useELK, boolean suppressOutput, long timeout){
		
		this.reasonerFactory = useELK ? 
				new ElkReasonerFactory() : reasonerFactory;
		
		this.alignOnto = alignOnto; 
		this.mappingsMap= mappingsMap; 
		this.suppressOutput = suppressOutput;

		this.limit = limit;
		this.c = c;
		this.timeout = timeout;

		setupExplanationGenerator();		
	}
	
	public void changeReasonerFactory(OWLReasonerFactory reasonerFactory){
		this.reasonerFactory = reasonerFactory;
		setupExplanationGenerator();
	}
	
	public void changeReasonerFactoryELK(){
		changeReasonerFactory(new ElkReasonerFactory());
	}

	public void changeReasonerFactoryPellet(){
		changeReasonerFactory(new PelletReasonerFactory());
	}
	
	private void setupExplanationGenerator(){
		EntailmentCheckerFactory<OWLAxiom> ecf = 
				new SatisfiabilityEntailmentCheckerFactory(
						this.reasonerFactory, true, this.timeout);
				
		this.explProgMon = new UnsatExplanationProgressMonitor(
				mappingsMap,suppressOutput);
		
		ExplanationGeneratorFactory<OWLAxiom> explGenFactory = 
//				new InconsistentOntologyExplanationGeneratorFactory(
//						this.reasonerFactory, Params.timeoutFullRepairExplanation);
//		//		new InconsistentOntologyExplanationGeneratorFactory(
//		//			this.reasonerFactory, Params.timeoutFullRepairExplanation);
						new BlackBoxExplanationGeneratorFactory<OWLAxiom>(
								new Configuration<OWLAxiom>(ecf));
				
		this.exManager = explGenFactory.createExplanationGenerator(
				alignOnto, explProgMon);
	}
	
	@Override
	public Set<OWLAxiom> call() {
		OWLSubClassOfAxiom ax = OntoUtil.getSubClassOfAxiom(c, 
				OntoUtil.getDataFactory().getOWLNothing());

		Set<Explanation<OWLAxiom>> expls = null;
		try {
//		if(limit == 0)
			expls = exManager.getExplanations(ax);
//		else 
//			expls = exManager.getExplanations(ax, limit);
		}
		catch(ExplanationGeneratorInterruptedException e){
			if(explProgMon.getRepair() == null)
				throw e;
		}
		
		if(expls != null && expls.isEmpty()) {
			FileUtil.writeErrorLogAndConsole(
					(reasonerFactory instanceof ElkReasonerFactory ? 
					"ELK" : reasonerFactory.getReasonerName()) + 
					" cannot detect the unsatisfiability for class " + c);
			return null;
		}
		
		if(explProgMon.getRepair() == null){
			FileUtil.writeLogAndConsole(expls.size() + 
					" explanation(s) found, none selected as a repair");
		}
		
		return Sets.newHashSet(explProgMon.getRepair());
	}
}