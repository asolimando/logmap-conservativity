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
package reasoning;

import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.IllegalConfigurationException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;

public class UnsupportedDTHermitReasonerFactory implements 
	org.semanticweb.owlapi.reasoner.OWLReasonerFactory {
	
	private org.semanticweb.HermiT.Reasoner.ReasonerFactory fact = 
			new org.semanticweb.HermiT.Reasoner.ReasonerFactory();
	
	@Override
	public String getReasonerName() {
		return fact.getReasonerName();
	}

	@Override
	public OWLReasoner createNonBufferingReasoner(OWLOntology ontology) {
		org.semanticweb.HermiT.Configuration c = 
				new org.semanticweb.HermiT.Configuration();
		c.ignoreUnsupportedDatatypes=true;
		c.bufferChanges = false;
		return new Reasoner(c, ontology);
	}

	@Override
	public OWLReasoner createReasoner(OWLOntology ontology) {
		org.semanticweb.HermiT.Configuration c = 
				new org.semanticweb.HermiT.Configuration();
		c.ignoreUnsupportedDatatypes=true;
		return new Reasoner(c, ontology);
	}

	@Override
	public OWLReasoner createNonBufferingReasoner(OWLOntology ontology,
			OWLReasonerConfiguration config)
			throws IllegalConfigurationException {
		return fact.createNonBufferingReasoner(ontology,config);
	}

	@Override
	public OWLReasoner createReasoner(OWLOntology ontology,
			OWLReasonerConfiguration config)
			throws IllegalConfigurationException {
		return fact.createReasoner(ontology,config);
	}
}
