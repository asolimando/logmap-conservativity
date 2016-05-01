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
package visitor.disjToConj;

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLAsymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLAxiomVisitorEx;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDatatypeDefinitionAxiom;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointUnionAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalDataPropertyAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLHasKeyAxiom;
import org.semanticweb.owlapi.model.OWLInverseFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLIrreflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLNegativeDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNegativeObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLReflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;
import org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubDataPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;
import org.semanticweb.owlapi.model.OWLSymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLTransitiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.SWRLRule;

import util.FileUtil;
import util.OntoUtil;
import util.Params;


public class OWLAxiomOverapproximationVisitor implements OWLAxiomVisitorEx<OWLAxiom> {

	private OWLClassExpressioneConjVisitor ceVis;
	private boolean fullApprox = false;
	private OWLDataFactory dataFactory;
	
	public OWLAxiomOverapproximationVisitor(OWLDataFactory dataFactory, 
			boolean fullApprox){
		this.dataFactory = dataFactory;
		this.fullApprox = fullApprox;
		ceVis = new OWLClassExpressioneConjVisitor(dataFactory, fullApprox);
	}
	
	@Override
	public OWLAxiom visit(OWLSubClassOfAxiom axiom) {
		OWLAxiom newAxiom = axiom;
		
		if(Params.verbosity > 1)
			FileUtil.writeLogAndConsole(axiom.getClass() + " approximation");
		
		OWLClassExpression sub = axiom.getSubClass(), 
				sup = axiom.getSuperClass().accept(ceVis); 
		
		if(!axiom.getSuperClass().equals(sup)){
			newAxiom = 
					OntoUtil.getDataFactory().getOWLSubClassOfAxiom(
							sub, 
							sup, 
							axiom.getAnnotations());
			if(Params.verbosity > 1)
				FileUtil.writeLogAndConsole(newAxiom + "\n");
		}
		return newAxiom;
	}

	@Override
	public OWLAxiom visit(OWLDisjointClassesAxiom axiom) {
		OWLAxiom newAxiom = axiom;
		
		if(Params.verbosity > 1)
			FileUtil.writeLogAndConsole(axiom.getClass() + " approximation");
		
		Set<OWLClassExpression> opsApx = new HashSet<>();
		OWLClassExpression opApx = null;
		boolean approximated = false;
		
		for (OWLClassExpression ce : axiom.getClassExpressionsAsList()) {
			opApx = ce.accept(ceVis);
			if(!opApx.equals(ce))
				approximated = true;
			opsApx.add(opApx);
		}
		
		if(approximated){
			newAxiom = 
					OntoUtil.getDataFactory().
						getOWLDisjointClassesAxiom(
								opsApx,
								axiom.getAnnotations());
			if(Params.verbosity > 1)
				FileUtil.writeLogAndConsole(newAxiom + "\n");
		}
		
		return newAxiom;
	}

	@Override
	public OWLAxiom visit(OWLDisjointUnionAxiom axiom) {
		OWLAxiom newAxiom = axiom;
		
		if(Params.verbosity > 1)
			FileUtil.writeLogAndConsole(axiom.getClass() + " approximation");
		
		Set<OWLClassExpression> opsApx = new HashSet<>();
		OWLClassExpression opApx = null;
		boolean approximated = false;
		
		for (OWLClassExpression ce : axiom.getClassExpressions()) {
			opApx = ce.accept(ceVis);
			if(!opApx.equals(ce))
				approximated = true;
			opsApx.add(opApx);
		}
				
		if(approximated){
			newAxiom = dataFactory.getOWLDisjointUnionAxiom(
					axiom.getOWLClass(), opsApx, axiom.getAnnotations());
			if(Params.verbosity > 1)
				FileUtil.writeLogAndConsole(newAxiom + "\n");
		}
		
		return newAxiom;
	}
	
	@Override
	public OWLAxiom visit(OWLEquivalentClassesAxiom axiom) {
		OWLAxiom newAxiom = axiom;
		
		if(Params.verbosity > 1)
			FileUtil.writeLogAndConsole(axiom.getClass() + " approximation");
		
		Set<OWLClassExpression> opsApx = new HashSet<>();
		OWLClassExpression opApx = null;
		boolean approximated = false;
		
		for (OWLClassExpression ce : axiom.getClassExpressions()) {
			opApx = ce.accept(ceVis);
			if(!opApx.equals(ce))
				approximated = true;
			opsApx.add(opApx);
		}
				
		if(approximated){
			newAxiom = dataFactory.getOWLEquivalentClassesAxiom(opsApx,
								axiom.getAnnotations());
			if(Params.verbosity > 1)
				FileUtil.writeLogAndConsole(newAxiom + "\n");
		}
		
		return newAxiom;
	}

	@Override
	public OWLAxiom visit(OWLDataPropertyDomainAxiom axiom) {		
		if(!fullApprox)
			return axiom;

		OWLAxiom newAxiom = axiom;
		
		if(Params.verbosity > 1)
			FileUtil.writeLogAndConsole(axiom.getClass() + " approximation");
		OWLClassExpression domain = axiom.getDomain();
		domain = domain.accept(ceVis);
		
		if(!domain.equals(axiom.getDomain())){
			newAxiom = dataFactory.getOWLDataPropertyDomainAxiom(
					axiom.getProperty(), domain);
			if(Params.verbosity > 1)
				FileUtil.writeLogAndConsole(newAxiom + "\n");
		}
		return newAxiom;
	}

	@Override
	public OWLAxiom visit(OWLObjectPropertyDomainAxiom axiom) {
		if(!fullApprox)
			return axiom;

		OWLAxiom newAxiom = axiom;
		
		if(Params.verbosity > 1)
			FileUtil.writeLogAndConsole(axiom.getClass() + " approximation");
		OWLClassExpression domain = axiom.getDomain();
		domain = domain.accept(ceVis);
		
		if(!domain.equals(axiom.getDomain())){
			newAxiom = dataFactory.getOWLObjectPropertyDomainAxiom(
					axiom.getProperty(),domain);
			if(Params.verbosity > 1)
				FileUtil.writeLogAndConsole(newAxiom + "\n");
		}
		return newAxiom;
	}

	@Override
	public OWLAxiom visit(OWLObjectPropertyRangeAxiom axiom) {
		if(!fullApprox)
			return axiom;

		OWLAxiom newAxiom = axiom;
		
		if(Params.verbosity > 1)
			FileUtil.writeLogAndConsole(axiom.getClass() + " approximation");
		OWLClassExpression range = axiom.getRange();
		range = range.accept(ceVis);
		
		if(!range.equals(axiom.getRange())){
			newAxiom = dataFactory.getOWLObjectPropertyDomainAxiom(
					axiom.getProperty(),range);
			if(Params.verbosity > 1)
				FileUtil.writeLogAndConsole(newAxiom + "\n");
		}
		return newAxiom;
	}

	@Override
	public OWLAxiom visit(OWLClassAssertionAxiom axiom) {
		if(!fullApprox)
			return axiom;

		OWLAxiom newAxiom = axiom;
		OWLClassExpression ce = axiom.getClassExpression().accept(ceVis);
		if(!ce.equals(axiom.getClassExpression()))
			newAxiom = dataFactory.getOWLClassAssertionAxiom(
					ce,axiom.getIndividual(), axiom.getAnnotations());
		return newAxiom;
	}
	
	@Override
	public OWLAxiom visit(OWLHasKeyAxiom axiom) {
		if(!fullApprox)
			return axiom;
		
		OWLAxiom newAxiom = axiom;
		OWLClassExpression ce = axiom.getClassExpression().accept(ceVis);
		if(!ce.equals(axiom.getClassExpression()))
			newAxiom = dataFactory.getOWLHasKeyAxiom(
					ce,axiom.getDataPropertyExpressions(), 
					axiom.getAnnotations());
		return newAxiom;
	}
	
	@Override
	public OWLAxiom visit(OWLAnnotationAssertionAxiom axiom) {
		return axiom;
	}

	@Override
	public OWLAxiom visit(OWLSubAnnotationPropertyOfAxiom axiom) {
		return axiom;
	}

	@Override
	public OWLAxiom visit(OWLAnnotationPropertyDomainAxiom axiom) {
		return axiom;
	}

	@Override
	public OWLAxiom visit(OWLAnnotationPropertyRangeAxiom axiom) {
		return axiom;
	}
	
	@Override
	public OWLAxiom visit(OWLDifferentIndividualsAxiom axiom) {
		return axiom;
	}

	@Override
	public OWLAxiom visit(OWLObjectPropertyAssertionAxiom axiom) {
		return axiom;
	}

	@Override
	public OWLAxiom visit(SWRLRule rule) {
		return rule;
	}
	
	@Override
	public OWLAxiom visit(OWLSameIndividualAxiom axiom) {
		return axiom;
	}
	
	@Override
	public OWLAxiom visit(OWLSubPropertyChainOfAxiom axiom) {
		return axiom;
	}
	
	@Override
	public OWLAxiom visit(OWLDeclarationAxiom axiom) {
		return axiom;
	}

	@Override
	public OWLAxiom visit(OWLEquivalentObjectPropertiesAxiom axiom) {
		return axiom;
	}

	@Override
	public OWLAxiom visit(OWLNegativeDataPropertyAssertionAxiom axiom) {
		return axiom;
	}

	@Override
	public OWLAxiom visit(OWLDisjointDataPropertiesAxiom axiom) {
		return axiom;
	}

	@Override
	public OWLAxiom visit(OWLDisjointObjectPropertiesAxiom axiom) {
		return axiom;
	}

	@Override
	public OWLAxiom visit(OWLFunctionalObjectPropertyAxiom axiom) {
		return axiom;
	}

	@Override
	public OWLAxiom visit(OWLSubObjectPropertyOfAxiom axiom) {
		return axiom;
	}

	@Override
	public OWLAxiom visit(OWLSymmetricObjectPropertyAxiom axiom) {
		return axiom;
	}

	@Override
	public OWLAxiom visit(OWLDataPropertyRangeAxiom axiom) {
		return axiom;
	}

	@Override
	public OWLAxiom visit(OWLFunctionalDataPropertyAxiom axiom) {
		return axiom;
	}

	@Override
	public OWLAxiom visit(OWLEquivalentDataPropertiesAxiom axiom) {
		return axiom;
	}

	@Override
	public OWLAxiom visit(OWLDataPropertyAssertionAxiom axiom) {
		return axiom;
	}

	@Override
	public OWLAxiom visit(OWLTransitiveObjectPropertyAxiom axiom) {
		return axiom;
	}

	@Override
	public OWLAxiom visit(OWLIrreflexiveObjectPropertyAxiom axiom) {
		return axiom;
	}

	@Override
	public OWLAxiom visit(OWLSubDataPropertyOfAxiom axiom) {
		return axiom;
	}

	@Override
	public OWLAxiom visit(OWLInverseFunctionalObjectPropertyAxiom axiom) {
		return axiom;
	}

	@Override
	public OWLAxiom visit(OWLInverseObjectPropertiesAxiom axiom) {
		return axiom;
	}

	@Override
	public OWLAxiom visit(OWLDatatypeDefinitionAxiom axiom) {
		return axiom;
	}

	@Override
	public OWLAxiom visit(OWLNegativeObjectPropertyAssertionAxiom axiom) {
		return axiom;
	}

	@Override
	public OWLAxiom visit(OWLAsymmetricObjectPropertyAxiom axiom) {
		return axiom;
	}

	@Override
	public OWLAxiom visit(OWLReflexiveObjectPropertyAxiom axiom) {
		return axiom;
	}
}
