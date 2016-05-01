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

import scc.graphAlgo.Johnson;
import scc.graphDataStructure.Diagnosis;
import scc.graphDataStructure.LightAdjacencyList;
import scc.graphDataStructure.LightCycles;
import scc.graphDataStructure.LightEdge;
import scc.graphDataStructure.LightSCC;
import scc.graphDataStructure.LightSCCs;

import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

import scc.test.ResultASPSGA;
import scc.test.Test2;
import util.FileUtil;
import util.Params;
import util.Util;

import scc.algoSolver.SGASolver;

public class TestDiagnosisThread implements Callable<String> {

	private LightAdjacencyList adj;
	private LightSCC scc;
	private LightSCCs localSCCs;
	private int numSCC;
	
	public TestDiagnosisThread(LightAdjacencyList adj, LightSCC scc, 
			LightSCCs localSCCs, int numSCC){
		this.adj = adj;
		this.scc = scc;
		this.localSCCs = localSCCs;
		this.numSCC = numSCC;
	}

	@Override
	public String call() throws Exception {
		
		FileUtil.writeLogAndConsole("SCC " + numSCC + " (" + scc.printDimensions(adj) + ")");			
		
		LightCycles cycles = null;
		long cycleTime = Util.getMSec();
		if(Test2.sgaOn || Test2.greedyCardOn){
			cycles = new Johnson().findElementaryCycles(adj,scc,localSCCs);
			cycleTime = Util.getDiffmsec(cycleTime);
			FileUtil.writeLogAndConsole("Cycle in " 
					+ cycleTime + "ms");
		}
		else
			cycleTime = -1;
	
		long aspConsTime = Util.getMSec(), 
				aspNonConsTime = Util.getMSec(), 
				sgaTime = -1, simpleTime = -1;
		double aspNonConsDiagWeight = -1, aspConsDiagWeight = -1, 
				sgaDiagWeight = -1, simpleWeight = -1;
		
		boolean saveFile = true;
		if(Test2.sgaOn || Test2.greedyCardOn)
			saveFile = cycles.size() > Params.numCyclesSaveFile;
		else
			saveFile = scc.extractMappings(adj, true).size() 
				>= Params.numMappingsSaveFile;

		Diagnosis d = null;
		double [] res;
		
		if(Test2.aspNonConsOn){
			res = Test2.launchASP(false, adj, scc, numSCC, saveFile, aspConsTime);
			aspNonConsTime = new Double(res[0] != -1 ? res[0] : res[1]).longValue();
			aspNonConsDiagWeight = res[2];
		}
		
		if(Test2.aspConsOn){
			res = Test2.launchASP(true, adj, scc, numSCC, saveFile, aspNonConsTime);
			aspConsTime = new Double(res[0] != -1 ? res[0] : res[1]).longValue();
			aspConsDiagWeight = res[2];
		}
		
		if(Test2.sgaOn){
			FileUtil.writeLogAndConsole(numSCC + " SGA Start");
			SGASolver solver = new SGASolver(adj, cycles, scc, 
					aspNonConsDiagWeight, Collections.<LightEdge> emptySet(),true);
			try {
				d = solver.computeDiagnosis();
				if(Params.testMode && !d.isDiagnosis(adj,scc))
					throw new Error(numSCC + " SGA model is not a diagnosis: " + d);
				if(Params.verbosity > 0)
					FileUtil.writeLogAndConsole(numSCC + " Solver diagnosis: " + d);
				sgaTime = d.getTime();
				sgaDiagWeight = d.getWeight();
			} catch (InterruptedException | TimeoutException e1) {
				e1.printStackTrace();
				System.exit(1);
			} catch (OutOfMemoryError e) {
				sgaTime = -1;
				sgaDiagWeight = -1;
				FileUtil.writeLogAndConsole(numSCC + " SGA out of memory!");
			}
			FileUtil.writeLogAndConsole("SGA done! " + numSCC);
		}

		if(Test2.greedyCardOn){
			FileUtil.writeLogAndConsole(numSCC + " Greedy Start");
			d = adj.computeDiagnosisOnCycles(cycles, scc, Params.greedyCardOpt);
			simpleTime = d.getTime();
			simpleWeight = d.getWeight();
			FileUtil.writeLogAndConsole("Greedy done! " + numSCC);
		}
		
		adj.removedMappings.addAll(d);
		
		return ResultASPSGA.printResult(adj, scc, cycles, cycleTime, aspConsTime, 
				aspNonConsTime, sgaTime, simpleTime, -1, -1, aspConsDiagWeight, 
				aspNonConsDiagWeight, sgaDiagWeight, simpleWeight, 
				-1,-1,-1,-1,-1,-1,-1,-1);
	}
}
