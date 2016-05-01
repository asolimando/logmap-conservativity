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
package visitor.ELnormalisation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLAsymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLAxiomVisitor;
import org.semanticweb.owlapi.model.OWLAxiomVisitorEx;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
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
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
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

import util.OntoUtil;
import util.Params;

public class OWLAxiomELNormVisitor implements OWLAxiomVisitorEx<OWLAxiom> {

	public OWLClassExpressionELNormVisitor ceVis;
	private ELNormaliser normaliser;
	
	public OWLAxiomELNormVisitor(ELNormaliser normaliser){
		this.normaliser = normaliser;
		 ceVis = new OWLClassExpressionELNormVisitor(normaliser);
	}
	
	@Override
	public OWLAxiom visit(OWLAnnotationAssertionAxiom axiom) {
		return null;
	}

	@Override
	public OWLAxiom visit(OWLSubAnnotationPropertyOfAxiom axiom) {
		return null;
	}

	@Override
	public OWLAxiom visit(OWLAnnotationPropertyDomainAxiom axiom) {
		return null;
	}

	@Override
	public OWLAxiom visit(OWLAnnotationPropertyRangeAxiom axiom) {
		return null;
	}
	
	@Override
	public OWLAxiom visit(OWLDifferentIndividualsAxiom axiom) {
		return null;
	}

	@Override
	public OWLAxiom visit(OWLObjectPropertyAssertionAxiom axiom) {
		return null;
	}

	@Override
	public OWLAxiom visit(SWRLRule rule) {
		return null;
	}
	
	@Override
	public OWLAxiom visit(OWLSameIndividualAxiom axiom) {
		return null;
	}
	
	@Override
	public OWLAxiom visit(OWLSubPropertyChainOfAxiom axiom) {
		return null;
	}
	
	@Override
	public OWLAxiom visit(OWLDeclarationAxiom axiom) {
		return null;
	}

	@Override
	public OWLAxiom visit(OWLSubClassOfAxiom axiom) {
		OWLAxiom newAxiom = axiom;
		
		if(Params.verbosity > 1)
			System.out.println(axiom.getClass() + " reduction");
		
		OWLClassExpression sub = null, sup = null;
		boolean modified = true;
		
		if(!axiom.isGCI()){
			sub = axiom.getSubClass();
			
			if(sub.isOWLNothing())
				return null;
		}
		else {
			// EL terminologies do not allow complex class expressions as LHS
			
			// if union, try to split in multiple subclassof axioms
			if(axiom.getSubClass().getClassExpressionType().equals(
					ClassExpressionType.OBJECT_UNION_OF)){
				OWLAxiom tmpAx;
				for (OWLClassExpression ce : 
					((OWLObjectUnionOf) axiom.getSubClass()).asDisjunctSet()) {
					tmpAx = normaliser.normaliseAxiom(
							ELNormaliser.dataFactory.getOWLSubClassOfAxiom(
									ce, axiom.getSuperClass()));
					if(tmpAx != null)
						OntoUtil.addAxiomsToOntology(normaliser.normalisedOnto, 
								ELNormaliser.manager, Collections.singleton(tmpAx), 
								false);
				}
			}
			
			return null;
		}
		
		sup = axiom.getSuperClass().accept(ceVis);
		
		if(sup == null)
			return null;
		
		if(sup.equals(axiom.getSuperClass()))
			modified = false;
		
		if(modified)
			newAxiom = ELNormaliser.dataFactory.getOWLSubClassOfAxiom(sub, 
							sup, axiom.getAnnotations());
		
		return newAxiom;
	}

	@Override
	public OWLAxiom visit(OWLNegativeObjectPropertyAssertionAxiom axiom) {
		return null;
	}

	@Override
	public OWLAxiom visit(OWLAsymmetricObjectPropertyAxiom axiom) {
		return null;
	}

	@Override
	public OWLAxiom visit(OWLReflexiveObjectPropertyAxiom axiom) {
		return null;
	}

	@Override
	public OWLAxiom visit(OWLDisjointClassesAxiom axiom) {
		return null;
	}
	
	@Override
	public OWLAxiom visit(OWLDisjointUnionAxiom axiom) {
		return null;
	}
	
	@Override
	public OWLAxiom visit(OWLEquivalentClassesAxiom axiom) {
		OWLAxiom newAxiom = null;
		
		if(Params.verbosity > 1)
			System.out.println(axiom.getClass() + " reduction");
		
		boolean modified = true;
		
		List<OWLClassExpression> operands = axiom.getClassExpressionsAsList();
		
		if(operands.size() <= 1)
			return null;
		
		if(operands.size() > 2)
			throw new IllegalArgumentException("EL normaliser expects pairwise " +
					"equivalent class axioms");
		
		if(!operands.get(0).isClassExpressionLiteral() && 
				!operands.get(1).isClassExpressionLiteral())
			throw new IllegalArgumentException("EL normaliser expects at " +
					"least an element to be atomic for equivalent class axioms");
		
		List<OWLClassExpression> reducedOp = new ArrayList<OWLClassExpression>(2);
		
		reducedOp.add(operands.get(0).accept(ceVis));
		reducedOp.add(operands.get(1).accept(ceVis));

		if(reducedOp.get(0) == null || reducedOp.get(1) == null)
			return null;

		if(reducedOp.get(0).equals(operands.get(0)) || 
				reducedOp.get(1).equals(operands.get(1)))
			modified = false;
		
		if(modified)
			return ELNormaliser.dataFactory.getOWLEquivalentClassesAxiom(
							new HashSet<>(reducedOp),
							axiom.getAnnotations());
		
		return axiom;
	}

	@Override
	public OWLAxiom visit(OWLDataPropertyDomainAxiom axiom) {
		return null;
	}

	@Override
	public OWLAxiom visit(OWLObjectPropertyDomainAxiom axiom) {
		return null;
	}

	@Override
	public OWLAxiom visit(OWLEquivalentObjectPropertiesAxiom axiom) {
		return null;
	}

	@Override
	public OWLAxiom visit(OWLNegativeDataPropertyAssertionAxiom axiom) {
		return null;
	}

	@Override
	public OWLAxiom visit(OWLDisjointDataPropertiesAxiom axiom) {
		return null;
	}

	@Override
	public OWLAxiom visit(OWLDisjointObjectPropertiesAxiom axiom) {
		return null;
	}

	@Override
	public OWLAxiom visit(OWLObjectPropertyRangeAxiom axiom) {
		return null;
	}

	@Override
	public OWLAxiom visit(OWLFunctionalObjectPropertyAxiom axiom) {
		return null;
	}

	@Override
	public OWLAxiom visit(OWLSubObjectPropertyOfAxiom axiom) {
		return null;
	}

	@Override
	public OWLAxiom visit(OWLSymmetricObjectPropertyAxiom axiom) {
		return null;
	}

	@Override
	public OWLAxiom visit(OWLDataPropertyRangeAxiom axiom) {
		return null;
	}

	@Override
	public OWLAxiom visit(OWLFunctionalDataPropertyAxiom axiom) {
		return null;
	}

	@Override
	public OWLAxiom visit(OWLEquivalentDataPropertiesAxiom axiom) {
		return null;
	}

	@Override
	public OWLAxiom visit(OWLClassAssertionAxiom axiom) {
		return null;
	}

	@Override
	public OWLAxiom visit(OWLDataPropertyAssertionAxiom axiom) {
		return null;	
	}

	@Override
	public OWLAxiom visit(OWLTransitiveObjectPropertyAxiom axiom) {
		return null;
	}

	@Override
	public OWLAxiom visit(OWLIrreflexiveObjectPropertyAxiom axiom) {
		return null;
	}

	@Override
	public OWLAxiom visit(OWLSubDataPropertyOfAxiom axiom) {
		return null;
	}

	@Override
	public OWLAxiom visit(OWLInverseFunctionalObjectPropertyAxiom axiom) {
		return null;
	}

	@Override
	public OWLAxiom visit(OWLInverseObjectPropertiesAxiom axiom) {
		return null;
	}

	@Override
	public OWLAxiom visit(OWLHasKeyAxiom axiom) {
		return null;
	}

	@Override
	public OWLAxiom visit(OWLDatatypeDefinitionAxiom axiom) {
		return null;
	}

}
