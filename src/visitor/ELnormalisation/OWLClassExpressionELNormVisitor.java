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

import java.util.HashSet;
import java.util.Set;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx;
import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
import org.semanticweb.owlapi.model.OWLDataExactCardinality;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataMaxCardinality;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLObjectHasSelf;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;

import util.Params;


public class OWLClassExpressionELNormVisitor implements 
	OWLClassExpressionVisitorEx<OWLClassExpression> {
	
	private ELNormaliser normaliser;
	
	public OWLClassExpressionELNormVisitor(ELNormaliser normaliser){
		this.normaliser = normaliser;
	}
	
	@Override
	public OWLClassExpression visit(OWLClass ce) {
		return ce;
	}

	@Override
	public OWLClassExpression visit(OWLObjectIntersectionOf ce) {
		if(Params.verbosity > 1)
			System.out.println(ce.getClass() + " reduction");
		
		Set<OWLClassExpression> reducedOp = new HashSet<OWLClassExpression>();

		// this flattens eventual nested conjunctions, as required
		Set<OWLClassExpression> operands = 
				((OWLObjectIntersectionOf) ce).asConjunctSet();

		if(operands.size() == 1)
			return operands.iterator().next();
		
		for (OWLClassExpression op : operands) {
			OWLClassExpression redOp = op.accept(this);
			if(op.isOWLNothing() || redOp == null)
				return null;
			reducedOp.add(redOp);
		}

		if(reducedOp.size() == 1)
			return reducedOp.iterator().next();
		
		return ELNormaliser.dataFactory.getOWLObjectIntersectionOf(reducedOp);
	}

	@Override
	public OWLClassExpression visit(OWLObjectUnionOf ce) {

		// this flattens eventual nested conjunctions, as required
		Set<OWLClassExpression> operands = 
				((OWLObjectUnionOf) ce).asDisjunctSet();
		
		operands.remove(ELNormaliser.dataFactory.getOWLNothing());
		
		if(operands.contains(ELNormaliser.dataFactory.getOWLThing()))
			return ELNormaliser.dataFactory.getOWLThing();
		
		Set<OWLClassExpression> reducedOp = new HashSet<OWLClassExpression>();
		
		OWLClassExpression tmpOp;
		
		for (OWLClassExpression op : operands){
			tmpOp = op.accept(this);
			
			if(tmpOp == null)
				return null;
			
			if(tmpOp.isOWLNothing())
				continue;
			
			reducedOp.add(tmpOp);
		}
		
		if(reducedOp.size() == 1)
			return reducedOp.iterator().next();

		if(reducedOp.contains(ELNormaliser.dataFactory.getOWLThing()))
			return ELNormaliser.dataFactory.getOWLThing();

		return null;
	}

	@Override
	public OWLClassExpression visit(OWLObjectComplementOf ce) {
		return null;
	}

	@Override
	public OWLClassExpression visit(OWLObjectSomeValuesFrom ce) {
		if(Params.verbosity > 1)
			System.out.println(ce.getClass() + " reduction");

		OWLClassExpression op = ce.getFiller(),
				reducedOp = op.accept(this);
		
		if(op.isOWLNothing() || ce.getProperty().isBottomEntity() || 
				ce.getProperty().isTopEntity() || reducedOp == null)
			return null;
		
		return normaliser.introduceFreshNamedConcept(
				ELNormaliser.dataFactory.getOWLObjectSomeValuesFrom(
						ce.getProperty(),reducedOp));
	}

	@Override
	public OWLClassExpression visit(OWLObjectAllValuesFrom ce) {
		if(ce.getFiller().isOWLNothing())
			return ELNormaliser.dataFactory.getOWLThing();
		
		return null;
	}

	@Override
	public OWLClassExpression visit(OWLObjectMinCardinality ce) {
		if(ce.getCardinality() == 1)
			return ELNormaliser.dataFactory.getOWLObjectSomeValuesFrom(
					ce.getProperty(), ce.getFiller());
		return null;
	}

	@Override
	public OWLClassExpression visit(OWLObjectExactCardinality ce) {
		return null;
	}

	@Override
	public OWLClassExpression visit(OWLObjectMaxCardinality ce) {
		return null;
	}

	@Override
	public OWLClassExpression visit(OWLObjectHasValue ce) {
		return null;
	}

	@Override
	public OWLClassExpression visit(OWLObjectHasSelf ce) {
		return null;
	}

	@Override
	public OWLClassExpression visit(OWLObjectOneOf ce) {
		return null;
	}

	@Override
	public OWLClassExpression visit(OWLDataSomeValuesFrom ce) {
		return null;
	}

	@Override
	public OWLClassExpression visit(OWLDataAllValuesFrom ce) {
		return null;
	}

	@Override
	public OWLClassExpression visit(OWLDataHasValue ce) {
		return null;
	}

	@Override
	public OWLClassExpression visit(OWLDataMinCardinality ce) {
		return null;
	}

	@Override
	public OWLClassExpression visit(OWLDataExactCardinality ce) {
		return null;
	}

	@Override
	public OWLClassExpression visit(OWLDataMaxCardinality ce) {
		return null;
	}
}
