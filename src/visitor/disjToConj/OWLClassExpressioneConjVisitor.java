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

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx;
import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
import org.semanticweb.owlapi.model.OWLDataExactCardinality;
import org.semanticweb.owlapi.model.OWLDataFactory;
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

import util.FileUtil;
import util.OntoUtil;
import util.Params;


public class OWLClassExpressioneConjVisitor implements
		OWLClassExpressionVisitorEx<OWLClassExpression> {

	private boolean fullApprox = false;
	private OWLDataFactory dataFactory;
		
	public OWLClassExpressioneConjVisitor(OWLDataFactory dataFactory, 
			boolean fullApprox){
		this.dataFactory = dataFactory;
		this.fullApprox = fullApprox;
	} 
	
	@Override
	public OWLClassExpression visit(OWLClass ce) {
		return ce;
	}

	@Override
	public OWLClassExpression visit(OWLObjectIntersectionOf ce) {
		if(Params.verbosity > 1)
			FileUtil.writeLogAndConsole(ce.getClass() + " approximation");
		
		if(!ClassificationOverapproximator.map.containsKey(ce)){
			Set<OWLClassExpression> apxOp = new HashSet<OWLClassExpression>();
			
			for (OWLClassExpression op : ce.getOperandsAsList())
				apxOp.add(op.accept(this));
	
			ClassificationOverapproximator.map.put(ce, 
					dataFactory.getOWLObjectIntersectionOf(apxOp)
			);
		}
		return ClassificationOverapproximator.map.get(ce);
	}

	@Override
	public OWLClassExpression visit(OWLObjectUnionOf ce) {
		if(Params.verbosity > 1)
			FileUtil.writeLogAndConsole(ce.getClass() + " approximation");
		
		if(!ClassificationOverapproximator.map.containsKey(ce)){
			Set<OWLClassExpression> apxOp = new HashSet<OWLClassExpression>();
			
			Set<OWLClass> classes = new HashSet<>();
			for (OWLClassExpression op : ce.getOperandsAsList()){
				apxOp.add(op.accept(this));
				if(!op.isAnonymous() && op.isClassExpressionLiteral())
					classes.add(op.asOWLClass());
			}
			
			ClassificationOverapproximator.toRetract.addAll(
					OntoUtil.createDisjAxioms(classes,dataFactory));
	
			ClassificationOverapproximator.map.put(ce,dataFactory.
					getOWLObjectIntersectionOf(apxOp)
			);
		}
		return ClassificationOverapproximator.map.get(ce);
	}

	@Override
	public OWLClassExpression visit(OWLObjectComplementOf ce) {
		if(!fullApprox)
			return ce;
		
		if(Params.verbosity > 1)
			FileUtil.writeLogAndConsole(ce.getClass() + " approximation");
		
		if(!ClassificationOverapproximator.map.containsKey(ce)){
			OWLClassExpression op = ce.getOperand();
			ClassificationOverapproximator.map.put(ce,dataFactory.
					getOWLObjectComplementOf(op.accept(this)));
		}
		return ClassificationOverapproximator.map.get(ce);
	}

	@Override
	public OWLClassExpression visit(OWLObjectSomeValuesFrom ce) {
		if(!fullApprox)
			return ce;

		if(Params.verbosity > 1)
			FileUtil.writeLogAndConsole(ce.getClass() + " approximation");

		if(!ClassificationOverapproximator.map.containsKey(ce)){
			OWLClassExpression op = ce.getFiller();
			ClassificationOverapproximator.map.put(ce,dataFactory.
					getOWLObjectSomeValuesFrom(ce.getProperty(),op.accept(this)));
		}
		return ClassificationOverapproximator.map.get(ce);
	}

	@Override
	public OWLClassExpression visit(OWLObjectAllValuesFrom ce) {
		if(!fullApprox)
			return ce;

		if(Params.verbosity > 1)
			FileUtil.writeLogAndConsole(ce.getClass() + " approximation");

		if(!ClassificationOverapproximator.map.containsKey(ce)){
			OWLClassExpression op = ce.getFiller();
			ClassificationOverapproximator.map.put(ce,dataFactory.
					getOWLObjectAllValuesFrom(ce.getProperty(),op.accept(this)));
		}
		return ClassificationOverapproximator.map.get(ce);
	}

	@Override
	public OWLClassExpression visit(OWLObjectMinCardinality ce) {
		if(!fullApprox)
			return ce;

		if(Params.verbosity > 1)
			FileUtil.writeLogAndConsole(ce.getClass() + " approximation");

		if(!ClassificationOverapproximator.map.containsKey(ce)){
			OWLClassExpression op = ce.getFiller();
			ClassificationOverapproximator.map.put(ce,dataFactory.
					getOWLObjectMinCardinality(ce.getCardinality(),
							ce.getProperty(),op.accept(this)));
		}
		return ClassificationOverapproximator.map.get(ce);
	}

	@Override
	public OWLClassExpression visit(OWLObjectExactCardinality ce) {
		if(!fullApprox)
			return ce;

		if(Params.verbosity > 1)
			FileUtil.writeLogAndConsole(ce.getClass() + " approximation");

		if(!ClassificationOverapproximator.map.containsKey(ce)){
			OWLClassExpression op = ce.getFiller();
			ClassificationOverapproximator.map.put(ce,dataFactory.
					getOWLObjectExactCardinality(ce.getCardinality(),
							ce.getProperty(),op.accept(this)));
		}
		return ClassificationOverapproximator.map.get(ce);
	}

	@Override
	public OWLClassExpression visit(OWLObjectMaxCardinality ce) {
		if(!fullApprox)
			return ce;

		if(Params.verbosity > 1)
			FileUtil.writeLogAndConsole(ce.getClass() + " approximation");

		if(!ClassificationOverapproximator.map.containsKey(ce)){
			OWLClassExpression op = ce.getFiller();
			ClassificationOverapproximator.map.put(ce,dataFactory.
					getOWLObjectMaxCardinality(ce.getCardinality(),
							ce.getProperty(),op.accept(this)));
		}
		return ClassificationOverapproximator.map.get(ce);
	}

	@Override
	public OWLClassExpression visit(OWLObjectHasValue ce) {
		return ce;
	}

	@Override
	public OWLClassExpression visit(OWLObjectHasSelf ce) {
		return ce;
	}

	@Override
	public OWLClassExpression visit(OWLObjectOneOf ce) {
		return ce;
	}

	@Override
	public OWLClassExpression visit(OWLDataSomeValuesFrom ce) {
		return ce;
	}

	@Override
	public OWLClassExpression visit(OWLDataAllValuesFrom ce) {
		return ce;
	}

	@Override
	public OWLClassExpression visit(OWLDataHasValue ce) {
		return ce;
	}

	@Override
	public OWLClassExpression visit(OWLDataMinCardinality ce) {
		return ce;
	}

	@Override
	public OWLClassExpression visit(OWLDataExactCardinality ce) {
		return ce;
	}

	@Override
	public OWLClassExpression visit(OWLDataMaxCardinality ce) {
		return ce;
	}
}
