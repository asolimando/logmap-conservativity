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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import ontology.ViolationExplanationProgressMonitor;

import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owl.explanation.api.ExplanationGenerator;
import org.semanticweb.owl.explanation.api.ExplanationGeneratorFactory;
import org.semanticweb.owl.explanation.impl.blackbox.Configuration;
import org.semanticweb.owl.explanation.impl.blackbox.EntailmentCheckerFactory;
import org.semanticweb.owl.explanation.impl.blackbox.checker.BlackBoxExplanationGeneratorFactory;
import org.semanticweb.owl.explanation.impl.blackbox.checker.SatisfiabilityEntailmentCheckerFactory;
import org.semanticweb.owl.explanation.impl.laconic.LaconicExplanationGeneratorFactory;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import util.OntoUtil;
import auxStructures.Pair;

public class ConservativityExplanationThread implements Callable<Boolean> {
	
	private int limit;
	private Pair<OWLClass> v;
	private ExplanationGenerator<OWLAxiom> exManager;
	private ViolationExplanationProgressMonitor explProgMon;
	
	public ConservativityExplanationThread(OWLOntology inputOnto, OWLOntology alignOnto, 
			Pair<OWLClass> v, OWLReasonerFactory reasonerFactory, int limit, 
			boolean useELK, OWLReasoner alignR, boolean suppressOutput){
		
		EntailmentCheckerFactory<OWLAxiom> ecf = 
				new SatisfiabilityEntailmentCheckerFactory(!useELK ? 
						reasonerFactory : new ElkReasonerFactory(),true,15000);
		
		Set<OWLClass> classSig = inputOnto.getClassesInSignature();
		classSig.remove(OntoUtil.getDataFactory().getOWLThing());
		classSig.remove(OntoUtil.getDataFactory().getOWLNothing());
		
		this.explProgMon = new ViolationExplanationProgressMonitor(
				inputOnto,v,classSig,suppressOutput);
		
		ExplanationGeneratorFactory<OWLAxiom> explGenFactory = 
				new LaconicExplanationGeneratorFactory<OWLAxiom>(
						new BlackBoxExplanationGeneratorFactory<OWLAxiom>(
								new Configuration<OWLAxiom>(ecf)));
		
//		this.exManager = 
//				explGenFactory.createExplanationGenerator(alignOnto, explProgMon);
		
//		OWLOntology module = null;
//		try {
//			System.out.println("Module for: " + v);
//			
//			IRI iri = IRI.create(
//					OntoUtil.getManager(false).getOntologyDocumentIRI(
//							alignOnto).toString() 
//								+ v.getFirst().getIRI().getFragment());
//					
//			if(OntoUtil.getManager(false).contains(iri))
//				module = OntoUtil.getManager(false).getOntology(iri); 
//			else
//				module = OntoUtil.extractModule(OntoUtil.getManager(false),
//					alignR, Collections.singleton((OWLEntity) v.getFirst()), 
//					v.getFirst().getIRI().getFragment());
//		} catch (OWLOntologyCreationException | OWLOntologyStorageException e) {
//			e.printStackTrace();
//		}
		
		// the ontology is already closed by inference, we only need subsumptions
		Set<OWLAxiom> subAxioms = new HashSet<>(); 
		subAxioms.addAll(alignOnto.getAxioms(AxiomType.EQUIVALENT_CLASSES));
		subAxioms.addAll(alignOnto.getAxioms(AxiomType.SUBCLASS_OF));
		
		this.exManager = explGenFactory.createExplanationGenerator(subAxioms, 
						explProgMon);
		
		this.limit = limit;
		this.v = v;
	}
	
	@Override
	public Boolean call() {
		OWLSubClassOfAxiom ax = OntoUtil.getSubClassOfAxiom(v);
		if(limit == 0)
			exManager.getExplanations(ax);
		else 
			exManager.getExplanations(ax, limit);
		
		return explProgMon.isDirect();
	}
}