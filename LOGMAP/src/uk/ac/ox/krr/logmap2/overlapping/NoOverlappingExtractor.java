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
package uk.ac.ox.krr.logmap2.overlapping;

import java.util.Calendar;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;


import uk.ac.ox.krr.logmap2.OntologyLoader;
import uk.ac.ox.krr.logmap2.io.LogOutput;

/**
 * This class will be used is no ovelapping is extracted. That is, the original ontologies are returned
 * 
 *
 * @author Ernesto Jimenez-Ruiz
 * Created: Sep 7, 2011
 *
 */
public class NoOverlappingExtractor extends OverlappingExtractor {

	long init, fin;
	

	
	public void createOverlapping(
			OWLOntology onto1, 
			OWLOntology onto2) {
		
		
		module1=onto1;
		module2=onto2;
		
	}
	

	public void createOverlapping(String iri_str1, String iri_str2) throws OWLOntologyCreationException{
		
		init = Calendar.getInstance().getTimeInMillis();
		OntologyLoader onto_loader1 = new OntologyLoader(iri_str1);		
		fin = Calendar.getInstance().getTimeInMillis();
		LogOutput.print("Time loading ontology 1 (s): " + (float)((double)fin-(double)init)/1000.0);
		
		init = Calendar.getInstance().getTimeInMillis();
		OntologyLoader onto_loader2 = new OntologyLoader(iri_str2);
		fin = Calendar.getInstance().getTimeInMillis();
		LogOutput.print("Time loading ontology 2 (s): " + (float)((double)fin-(double)init)/1000.0);

		
		module1=onto_loader1.getOWLOntology();
		module2=onto_loader2.getOWLOntology();
		
	}




	@Override
	public Set<OWLEntity> getBaseOverlappedEntities1() {
		return module1.getSignature();
	}




	@Override
	public Set<OWLEntity> getBaseOverlappedEntities2() {
		return module2.getSignature();
	}

}
