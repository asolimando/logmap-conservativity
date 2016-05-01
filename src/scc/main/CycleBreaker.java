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
package scc.main;

import scc.exception.ClassificationTimeoutException;
import scc.graphDataStructure.LightAdjacencyList;
import scc.graphDataStructure.LightEdge;
import scc.graphDataStructure.LightSCC;
import scc.graphDataStructure.LightSCCs;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import scc.mapping.LightOAEIMappingHandler;

import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import util.Params;

public class CycleBreaker {

	private static final String usageMsg = "Usage: onto1 onto2 mapping mappingOut verbosity 1-1mapping\n" +
			"onto1-2 = existing and valid serializations of input ontologies\n" +
			"mapping = valid and existing mapping in AlignmentAPI format\n" +
			"mappingOut = pathname for the repaired mapping\n" +
			"verbosity = verbosity level of the application\n" +
			"1-1mapping = 1 -> removes n-m mappings from problematic SCCs, 0 -> removes them only if needed ";
	
	public static void main(String[] args) {
		
		if(args.length != 6){
			printUsage();
		}
		
		File onto1, onto2, mapping, mappingOut;
		int verbosity = 0;
		boolean filter1to1 = false, error = false;
		
		onto1 = new File(args[0]);
		onto2 = new File(args[1]);
		mapping = new File(args[2]);
		mappingOut = new File(args[3]);
		
		if(!onto1.exists() || !onto1.isFile()){
			System.err.println("The first ontology file must exist");			
			error = true;
		} 
		if(!onto2.exists() || !onto2.isFile()){
			System.err.println("The second ontology file must exist");
			error = true;
		}
		if(!mapping.exists() || !mapping.isFile()){
			System.err.println("The mapping file must exist");
			error = true;
		}
		try {
			verbosity = Integer.parseInt(args[4]);
		} catch(NumberFormatException e){
			System.err.println("Verbosity level must be an integer number");
			error = true;
		}
		
		try {
			int tmp = Integer.parseInt(args[5]);
			if(tmp == 0)
				filter1to1 = false;
			else if(tmp == 1)
				filter1to1 = true;
			else
				throw new NumberFormatException();
				
		} catch(NumberFormatException e){
			System.err.println("Multiple-occurrences support must 0 (false) or 1 (true)");
			error = true;
		} 
		
		if(error){
			printUsage();
		}
		Params.verbosity = verbosity;
		
		LightAdjacencyList adj = null;
		try {
			adj = new LightAdjacencyList(onto1, onto2, null, true, false, false);
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		} catch (ClassificationTimeoutException e) {
			System.err.println(e.getMessage());
			return;
		}
		
		LightOAEIMappingHandler parser = new LightOAEIMappingHandler();
		Set<LightEdge> mappings = null;
		try {
			mappings = parser.parse(mapping, adj, false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		LightSCCs localSCCs = null, globSCCs = null; 
		Set<LightSCC> problematicSCCs = new HashSet<>();
		Set<LightEdge> problemsFlattened = new HashSet<LightEdge>();
		
		globSCCs = adj.computeGlobalSCCsAndProblematicMappings(
				problematicSCCs, null);
		
		// we remove multiple-occurrences in problematic SCCs, if needed
		if(filter1to1)
			adj.filterMultipleCorrespondencesAndRecomputeGlobalSCCs(
					problematicSCCs, null);
		
		// TODO: compute diagnosis and saving repaired mapping
	}
	
	private static void printUsage(){
		System.out.println(usageMsg);
		System.exit(1);
	}

}
