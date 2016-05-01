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

import scc.exception.UnsatisfiableProblemException;
import scc.graphAlgo.Johnson;
import scc.graphDataStructure.Diagnosis;
import scc.graphDataStructure.LightAdjacencyList;
import scc.graphDataStructure.LightCycles;
import scc.graphDataStructure.LightEdge;
import scc.graphDataStructure.LightSCC;
import scc.graphDataStructure.LightSCCs;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import util.OntoUtil;
import util.Params;
import util.Util;
import scc.algoSolver.ASPSolver;
import scc.algoSolver.SGASolver;

public class SmallTester {
	public static void main(String[] args) throws OWLOntologyCreationException, 
		IOException, InterruptedException, TimeoutException {
		
		String mappingPath = args.length == 0 ? "test_data/input9.txt" : args[0];
		
		OntoUtil.getManager(true);
		Set<LightEdge> mappings = new HashSet<LightEdge>();
		
		LightAdjacencyList adj = new LightAdjacencyList(mappingPath, mappings);
		
		System.out.println(adj);

		LightSCCs localSCCs = adj.getLocalSCCs();
		System.out.println("LocalSCCs:" + localSCCs);
		
		LightSCCs globalSCCs = null;
		System.out.println(globalSCCs = adj.loopDetection(true, null,false));
		
		Params.verbosity = 10;
		
		Set<LightEdge> problematicMappings = new HashSet<>();
		Set<LightSCC> problematicSCCs = new HashSet<>();
		
		adj.computeGlobalSCCsAndProblematicMappings(problematicSCCs, null);
		int originalProblemsNum = problematicSCCs.size();
		
		int numSCC = 0;
		Diagnosis [] globalDiagnoses = {new Diagnosis(), new Diagnosis(), 
				new Diagnosis()};
		
		for (LightSCC scc : problematicSCCs) {
			long cycleTime = Util.getMSec();
			LightCycles cycles = new Johnson().findElementaryCycles(adj,scc,localSCCs);
			System.out.println("Cycle in " + (Util.getDiffmsec(cycleTime)) + "ms");
			System.out.println(cycles.toString().replace("], ", "],\n"));

			//int numCycles = cycles.size();
			long aspConsTime = Util.getMSec(), 
					aspNonConsTime = Util.getMSec(), sgaTime = 0, 
					simpleTime = 0;
			double aspConsDiagWeight = -1, aspNonConsDiagWeight = -1, 
					sgaDiagWeight = -1, simpleWeight = -1;
			
			Diagnosis dConsASP = null, dNonConsASP = null, dSGA = null, 
					dGreedy = null;
			System.out.print("Conservative ASP...");
			try {
				dConsASP = new ASPSolver(adj, scc, numSCC, 
						true, false, Params.useDLV).computeDiagnosis();
				if(Params.verbosity > 0)
					System.out.println(numSCC + " ASP diagnosis: " + dConsASP);
				if(Params.testMode && !dConsASP.isDiagnosis(adj,scc))
					throw new Error(numSCC + " ASP model is not a diagnosis!");
			} catch (InterruptedException e1) {
				System.out.println("InterruptedException in DiagnosisThread");
				e1.printStackTrace();
			} catch (TimeoutException e) {
				// timeout, no valid values for ASP
				aspConsTime = -1;
				aspConsDiagWeight = -1;
				
				if(Params.verbosity > 0)
					System.out.println(numSCC + " ASP timed out");
			} catch (UnsatisfiableProblemException e) {
				System.err.println(e.getMessage());
			}
			System.out.println("done!");
			if(dConsASP != null){
				aspConsTime = Util.getDiffmsec(aspConsTime);
				aspConsDiagWeight = dConsASP.getWeight();
				globalDiagnoses[0].addAll(dConsASP);
			}
			
			System.out.print("NonConservative ASP...");
			try {
				dConsASP = new ASPSolver(adj, scc, numSCC, 
						false, false, Params.useDLV).computeDiagnosis();
				if(Params.verbosity > 0)
					System.out.println(numSCC + " ASP diagnosis: " + dConsASP);
				if(Params.testMode && !dConsASP.isDiagnosis(adj,scc))
					throw new Error(numSCC + " ASP model is not a diagnosis!");
			} catch (InterruptedException e1) {
				System.out.println("InterruptedException in DiagnosisThread");
				e1.printStackTrace();
			} catch (TimeoutException e) {
				// timeout, no valid values for ASP
				aspNonConsTime = -1;
				aspNonConsDiagWeight = -1;
				
				if(Params.verbosity > 0)
					System.out.println(numSCC + " ASP timed out");
			} catch (UnsatisfiableProblemException e) {
				System.err.println(e.getMessage());
			}
			System.out.println("done!");
			if(dConsASP != null){
				aspConsTime = Util.getDiffmsec(aspConsTime);
				aspConsDiagWeight = dConsASP.getWeight();
				globalDiagnoses[0].addAll(dConsASP);
			}
			
			System.out.print("SGA...");
			SGASolver solver = new SGASolver(adj, cycles, scc, aspConsDiagWeight, 
					Collections.<LightEdge> emptySet(),false);
			sgaTime = Util.getMSec();
			try {
				dSGA = solver.computeDiagnosis();
				if(Params.testMode && !dSGA.isDiagnosis(adj,scc))
					throw new Error(numSCC + " SGA model is not a diagnosis: " + dSGA);
				if(Params.verbosity > 0)
					System.out.println(numSCC + " SGA Solver diagnosis: " + dSGA);
			} catch (InterruptedException | TimeoutException e1) {
				e1.printStackTrace();
				System.exit(1);
			}
			sgaTime = Util.getDiffmsec(sgaTime);
			sgaDiagWeight = dSGA.getWeight();
			System.out.println("done!");
			
			globalDiagnoses[1].addAll(dSGA);
			
			if(dConsASP == null){
				globalDiagnoses[0].addAll(dSGA);
			}

			System.out.print("Greedy...");
			simpleTime = Util.getMSec();
			dGreedy = adj.computeDiagnosisOnCycles(cycles, scc, 
					Params.greedyCardOpt);
			simpleTime = Util.getDiffmsec(simpleTime);
			simpleWeight = dGreedy.getWeight();
			System.out.println("done!");
			
			globalDiagnoses[2].addAll(dGreedy);
			
			if(Params.verbosity > 0)
				System.out.println(numSCC + " Greedy diagnosis: " + dGreedy);
			
			numSCC++;
			
			new ResultASPSGA(adj, scc, cycles, cycleTime, aspConsTime, 
					aspNonConsTime, sgaTime, simpleTime,-1,-1,aspConsDiagWeight, 
					aspNonConsDiagWeight, sgaDiagWeight, simpleWeight,
					-1,-1,-1, -1,-1, -1,-1, -1);
		}
		
		String [] diagLabel = {"ASP","SGA","Greedy"};
		int a = 0;
		
		for (Diagnosis diagnosis : globalDiagnoses) {
			adj.removedMappings.clear();
			adj.removedMappings.addAll(diagnosis);
			
			System.out.println("\n\nChecking " + diagLabel[a++] + " diagnosis");
			
			adj.computeGlobalSCCsAndProblematicMappings(problematicSCCs, null);
			
			if(problematicSCCs.size() > 0){
				System.out.println(problematicSCCs);
				System.out.println(problematicSCCs.size() + " problematic SCCs (and their " 
						+ problematicSCCs.size() + " mappings):");

				if(Params.verbosity > 0)
					for (LightSCC scc : problematicSCCs)
						scc.printProblematicSCC(adj);

				System.err.println("FALLITO " + problematicSCCs.size() + " SU " + originalProblemsNum);
			}
		}
	}
}
