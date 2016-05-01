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

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;



public class PelletAccess extends ReasonerAccessImpl{
			
	public PelletAccess(OWLOntologyManager ontoManager, OWLOntology onto, boolean useFactory) throws Exception{		
		super(ontoManager,onto, useFactory);
		
	}
	
	
	protected void setUpReasoner(boolean useFactory) throws Exception{
			
		reasonerFactory = new PelletReasonerFactory();
		
		if (useFactory){			 
			reasoner = reasonerFactory.createReasoner(ontoBase);
		}
		else{
			reasoner=new Pellet_adapted(ontoBase);			
		}
		
		reasonerName = "Pellet";
		
	}
	
		
}


