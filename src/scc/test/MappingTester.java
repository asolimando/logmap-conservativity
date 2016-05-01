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
package scc.test;

import java.io.IOException;
import java.util.Collections;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import scc.exception.ClassificationTimeoutException;

import util.OntoUtil;

import scc.graphDataStructure.LightAdjacencyList;

public class MappingTester {
	public static void main(String[] args) throws OWLOntologyCreationException, 
		IOException, ClassificationTimeoutException{
		String mappingPath = args.length == 0 ? "" : args[0],
		ontoPath1 = args.length == 0 ? "" : args[1],
		ontoPath2 = args.length == 0 ? "" : args[2];
		
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	
		OWLOntology onto1,onto2;
		
		onto1 = OntoUtil.load(ontoPath1, true, manager);
		onto2 = OntoUtil.load(ontoPath2, true, manager);
		
		LightAdjacencyList adj = new LightAdjacencyList(onto1, onto2, null, true);
		
		adj.getOAEIHandler().checkMappings(adj,
				Collections.singleton(mappingPath).toArray(new String[0]));
	}
}
