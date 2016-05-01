/*******************************************************************************
 * Copyright 2012 by the Department of Computer Science (University of Oxford)
 * 
 *    This file is part of LogMap.
 * 
 *    LogMap is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 * 
 *    LogMap is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 * 
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with LogMap.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package uk.ac.ox.krr.logmap2.reasoning;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.BufferingMode;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.reasoner.impl.OWLClassNodeSet;

import eu.trowl.owlapi3.rel.reasoner.dl.RELReasonerFactory;
import eu.trowl.owlapi3.rel.reasoner.dl.RELReasoner;

public class TrOWL_adapted extends RELReasoner{
	
	
	public TrOWL_adapted(OWLOntologyManager manager, OWLOntology rootOntology) throws Exception {
		//super(manager, rootOntology);//, new SimpleConfiguration(), BufferingMode.BUFFERING);
		super(manager,rootOntology,true,true,true);		
	}
	
	/**
	 * Getting all disjoint classes is costly. We get only explicit disjointness.
	 * We will complete with questions (A intersection B) later  if necessary
	 */
	public NodeSet<OWLClass> getDisjointClasses(OWLClassExpression ce) {
        //super.ensurePrepared();
        OWLClassNodeSet nodeSet = new OWLClassNodeSet();
        if (!ce.isAnonymous()) {
            for (OWLOntology ontology : getRootOntology().getImportsClosure()) {
                for (OWLDisjointClassesAxiom ax : ontology.getDisjointClassesAxioms(ce.asOWLClass())) {
                    for (OWLClassExpression op : ax.getClassExpressions()) {
                        if (!op.isAnonymous() && !op.equals(ce)) { //Op must be differnt to ce
                            nodeSet.addNode(getEquivalentClasses(op));
                        }
                    }
                }
            }
        }        
        return nodeSet;
    }
	
}
