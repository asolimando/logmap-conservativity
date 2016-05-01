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
package scc.thread;

import scc.graphDataStructure.Diagnosis;
import scc.graphDataStructure.LightAdjacencyList;
import scc.graphDataStructure.LightCycles;
import scc.graphDataStructure.LightSCC;

import java.util.concurrent.Callable;

import util.Params;

public class SimpleDiagnosisThread implements Callable<Diagnosis> {

	private LightCycles cycles;
	private LightSCC scc;
	private LightAdjacencyList adj;
	
	public SimpleDiagnosisThread(LightAdjacencyList adj, LightCycles cycles, 
			LightSCC scc){
		this.adj = adj;
		this.scc = scc;
		this.cycles = cycles;
	}

	@Override
	public Diagnosis call() throws Exception {
		return adj.computeDiagnosisOnCycles(cycles, scc, Params.greedyCardOpt);
	}
}
