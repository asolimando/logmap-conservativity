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
package visitor.structuralreduction;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitor;
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

import util.OntoUtil;
import util.Params;
import util.Util;

public class OWLClassExpressioneReducerVisitor implements
		OWLClassExpressionVisitor {

	@Override
	public void visit(OWLClass ce) {
		return;
	}

	@Override
	public void visit(OWLObjectIntersectionOf ce) {
		if(Params.verbosity > 1)
			System.out.println(ce.getClass() + " reduction");
		
		Set<OWLClassExpression> reducedOp = new HashSet<OWLClassExpression>();
		OWLClass newClass = null;
		
		for (OWLClassExpression op : ce.getOperandsAsList()) {
			if(op.isClassExpressionLiteral())
				reducedOp.add(ce);
			else {
				if(StructuralReducer.map.containsKey(op))
					reducedOp.add(StructuralReducer.map.get(op));						
				else {
					reducedOp.add(newClass = OntoUtil.createFreshClass(
							StructuralReducer.onto, 
							StructuralReducer.dataFactory, 
							StructuralReducer.manager));
					StructuralReducer.map.put(op, newClass);
					StructuralReducer.stack.push(op);
				}
			}
		}
		
		StructuralReducer.toAdd.add(
				StructuralReducer.dataFactory.getOWLEquivalentClassesAxiom(
						StructuralReducer.map.get(ce), 
						StructuralReducer.dataFactory.getOWLObjectIntersectionOf(
								reducedOp)
				)
		);
	}

	@Override
	public void visit(OWLObjectUnionOf ce) {
		if(Params.verbosity > 1)
			System.out.println(ce.getClass() + " reduction");

		Set<OWLClassExpression> reducedOp = new HashSet<OWLClassExpression>();
		OWLClass newClass = null;
		
		for (OWLClassExpression op : ce.getOperandsAsList()) {
			if(op.isClassExpressionLiteral())
				reducedOp.add(ce);
			else {
				if(StructuralReducer.map.containsKey(op))
					reducedOp.add(StructuralReducer.map.get(op));						
				else {
					reducedOp.add(newClass = OntoUtil.createFreshClass(
							StructuralReducer.onto, 
							StructuralReducer.dataFactory, 
							StructuralReducer.manager));
					StructuralReducer.map.put(op, newClass);
					StructuralReducer.stack.push(op);
				}
			}
		}
		
		StructuralReducer.toAdd.add(
				StructuralReducer.dataFactory.getOWLEquivalentClassesAxiom(
						StructuralReducer.map.get(ce), 
						StructuralReducer.dataFactory.getOWLObjectUnionOf(
								reducedOp)
				)
		);
	}

	@Override
	public void visit(OWLObjectComplementOf ce) {
		if(Params.verbosity > 1)
			System.out.println(ce.getClass() + " reduction");

		OWLClassExpression reducedOp = null,
				op = ce.getOperand();
		
		if(!op.isClassExpressionLiteral()){
			if(StructuralReducer.map.containsKey(op))
					reducedOp= StructuralReducer.map.get(op);
			else {
				reducedOp = OntoUtil.createFreshClass(
						StructuralReducer.onto, 
						StructuralReducer.dataFactory, 
						StructuralReducer.manager);
				
				StructuralReducer.map.put(op, reducedOp.asOWLClass());
				StructuralReducer.stack.push(op);
			}
		}
		else
			reducedOp = op;
		
		StructuralReducer.toAdd.add(
				StructuralReducer.dataFactory.getOWLEquivalentClassesAxiom(
						StructuralReducer.map.get(ce), 
						StructuralReducer.dataFactory.getOWLObjectComplementOf(
								reducedOp)
				)
		);
	}

	@Override
	public void visit(OWLObjectSomeValuesFrom ce) {
		if(Params.verbosity > 1)
			System.out.println(ce.getClass() + " reduction");

		OWLClassExpression reducedOp = null,
				op = ce.getFiller();
		
		if(!op.isClassExpressionLiteral()){
			if(StructuralReducer.map.containsKey(op))
					reducedOp= StructuralReducer.map.get(op);
			else {
				reducedOp = OntoUtil.createFreshClass(
						StructuralReducer.onto, 
						StructuralReducer.dataFactory, 
						StructuralReducer.manager);
				
				StructuralReducer.map.put(op, reducedOp.asOWLClass());
				StructuralReducer.stack.push(op);
			}
		}
		else
			reducedOp = op;
		
		StructuralReducer.toAdd.add(
				StructuralReducer.dataFactory.getOWLEquivalentClassesAxiom(
						StructuralReducer.map.get(ce), 
						StructuralReducer.dataFactory.getOWLObjectComplementOf(
								reducedOp)
				)
		);
	}

	@Override
	public void visit(OWLObjectAllValuesFrom ce) {
		if(Params.verbosity > 1)
			System.out.println(ce.getClass() + " reduction");

		OWLClassExpression reducedOp = null,
				op = ce.getFiller();
		
		if(!op.isClassExpressionLiteral()){
			if(StructuralReducer.map.containsKey(op))
					reducedOp= StructuralReducer.map.get(op);
			else {
				reducedOp = OntoUtil.createFreshClass(
						StructuralReducer.onto, 
						StructuralReducer.dataFactory, 
						StructuralReducer.manager);
				
				StructuralReducer.map.put(op, reducedOp.asOWLClass());
				StructuralReducer.stack.push(op);
			}
		}
		else
			reducedOp = op;
		
		StructuralReducer.toAdd.add(
				StructuralReducer.dataFactory.getOWLEquivalentClassesAxiom(
						StructuralReducer.map.get(ce), 
						StructuralReducer.dataFactory.getOWLObjectComplementOf(
								reducedOp)
				)
		);
	}

	@Override
	public void visit(OWLObjectMinCardinality ce) {
		if(Params.verbosity > 1)
			System.out.println(ce.getClass() + " reduction");

		OWLClassExpression reducedOp = null,
				op = ce.getFiller();
		
		if(!op.isClassExpressionLiteral()){
			if(StructuralReducer.map.containsKey(op))
					reducedOp= StructuralReducer.map.get(op);
			else {
				reducedOp = OntoUtil.createFreshClass(
						StructuralReducer.onto, 
						StructuralReducer.dataFactory, 
						StructuralReducer.manager);
				
				StructuralReducer.map.put(op, reducedOp.asOWLClass());
				StructuralReducer.stack.push(op);
			}
		}
		else
			reducedOp = op;
		
		StructuralReducer.toAdd.add(
				StructuralReducer.dataFactory.getOWLEquivalentClassesAxiom(
						StructuralReducer.map.get(ce), 
						StructuralReducer.dataFactory.getOWLObjectMinCardinality(
								ce.getCardinality(),ce.getProperty(),reducedOp)
				)
		);
	}

	@Override
	public void visit(OWLObjectExactCardinality ce) {
		if(Params.verbosity > 1)
			System.out.println(ce.getClass() + " reduction");

		OWLClassExpression reducedOp = null,
				op = ce.getFiller();
		
		if(!op.isClassExpressionLiteral()){
			if(StructuralReducer.map.containsKey(op))
					reducedOp= StructuralReducer.map.get(op);
			else {
				reducedOp = OntoUtil.createFreshClass(
						StructuralReducer.onto, 
						StructuralReducer.dataFactory, 
						StructuralReducer.manager);
				
				StructuralReducer.map.put(op, reducedOp.asOWLClass());
				StructuralReducer.stack.push(op);
			}
		}
		else
			reducedOp = op;
		
		StructuralReducer.toAdd.add(
				StructuralReducer.dataFactory.getOWLEquivalentClassesAxiom(
						StructuralReducer.map.get(ce), 
						StructuralReducer.dataFactory.getOWLObjectExactCardinality(
								ce.getCardinality(),ce.getProperty(),reducedOp)
				)
		);
	}

	@Override
	public void visit(OWLObjectMaxCardinality ce) {
		if(Params.verbosity > 1)
			System.out.println(ce.getClass() + " reduction");

		OWLClassExpression reducedOp = null,
				op = ce.getFiller();
		
		if(!op.isClassExpressionLiteral()){
			if(StructuralReducer.map.containsKey(op))
					reducedOp= StructuralReducer.map.get(op);
			else {
				reducedOp = OntoUtil.createFreshClass(
						StructuralReducer.onto, 
						StructuralReducer.dataFactory, 
						StructuralReducer.manager);
				
				StructuralReducer.map.put(op, reducedOp.asOWLClass());
				StructuralReducer.stack.push(op);
			}
		}
		else
			reducedOp = op;
		
		StructuralReducer.toAdd.add(
				StructuralReducer.dataFactory.getOWLEquivalentClassesAxiom(
						StructuralReducer.map.get(ce), 
						StructuralReducer.dataFactory.getOWLObjectMaxCardinality(ce.getCardinality(),ce.getProperty(),reducedOp)
				)
		);
	}

	@Override
	public void visit(OWLObjectHasValue ce) {
		return;
	}

	@Override
	public void visit(OWLObjectHasSelf ce) {
		return;
	}

	@Override
	public void visit(OWLObjectOneOf ce) {
		return;
	}

	@Override
	public void visit(OWLDataSomeValuesFrom ce) {
		return;
	}

	@Override
	public void visit(OWLDataAllValuesFrom ce) {
		return;
	}

	@Override
	public void visit(OWLDataHasValue ce) {
		return;
	}

	@Override
	public void visit(OWLDataMinCardinality ce) {
		return;
	}

	@Override
	public void visit(OWLDataExactCardinality ce) {
		return;
	}

	@Override
	public void visit(OWLDataMaxCardinality ce) {
		return;
	}
}
