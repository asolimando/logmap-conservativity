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

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.uncommons.maths.random.MersenneTwisterRNG;

import util.Params;
import util.Util;

import scc.graphDataStructure.Diagnosis;
import scc.graphDataStructure.LightAdjacencyList;
import scc.graphDataStructure.LightEdge;
import scc.graphDataStructure.LightSCC;
import scc.graphDataStructure.LightSCCs;

public class RandomGraphTester {

	private static final int maxVertices = 1000;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int maxIterations = 100;
		
		Random rnd = new MersenneTwisterRNG();
		//PoissonGenerator poisson = new PoissonGenerator(rnd.nextDouble(), rnd);
		
		for (int i = 0; i < maxIterations; i++) {
			Set<LightEdge> mappings = new HashSet<>();
			
			int numVertices1 = rnd.nextInt(maxVertices), numVertices2 = rnd.nextInt(maxVertices);
			double connPerc1 = Math.min(rnd.nextDouble(), 0.1), 
					connPerc2 = Math.min(rnd.nextDouble(), 0.1), 
					connPercMappings = Math.min(rnd.nextDouble(), 0.005);
			
			int fullConnectivity1 = new Integer((int) Math.pow(numVertices1, 2) - numVertices1).intValue();
			int fullConnectivity2 = new Integer((int) (Math.pow(numVertices2, 2) - numVertices2)).intValue();
			int alignedFullConnectivity = new Integer((int) (Math.pow(numVertices1+numVertices2, 2) 
					- (numVertices1+numVertices2))).intValue();
			
			System.out.println("|V1| = " + numVertices1 + ", |V2| = " + numVertices2 
					+ ", |E1| = " + ((int) fullConnectivity1 * connPerc1) 
					+ ", |E2| = " + ((int) fullConnectivity2 * connPerc2) 
					+ ", |M| = " +  ((int) alignedFullConnectivity * connPercMappings));

			Params.verbosity = 10;
			
			LightAdjacencyList adj = new LightAdjacencyList(mappings, numVertices1, numVertices2, 
					connPerc1, connPerc2, connPercMappings);

			LightSCCs globalSCCs = new LightSCCs();
			
			Set<LightEdge> problematicMappings = new HashSet<>();
			Set<LightSCC> problematicSCCs = new HashSet<>();
			
			long time = Util.getMSec();
			Diagnosis d = adj.computeDiagnosis(globalSCCs,problematicMappings,
					problematicSCCs, null,time);			
			System.out.println("#Diagnosis/#Mappings = " + d.size() + " / " + mappings.size() 
					+ " in " + (Util.getDiffmsec(time)) + "ms");
		}
	}

}
