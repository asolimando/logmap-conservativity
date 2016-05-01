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
import scc.graphDataStructure.Diagnosis;
import scc.graphDataStructure.LightAdjacencyList;
import scc.graphDataStructure.LightCycles;
import scc.graphDataStructure.LightEdge;
import scc.graphDataStructure.LightSCC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeoutException;

import scc.algoSolver.ASPSolver;
import scc.algoSolver.SGASolver;
import scc.util.LegacyFileUtil;
import util.Params;
import util.Util;

public class ExternalMain {

	public static int sleepGC = 50;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length < 1)
			throw new Error("Launching type needed");
		int k = Integer.parseInt(args[0]);

		LightSCC scc = null;
		LightAdjacencyList adj = null;
		LightCycles cycles = null;

		System.out.println("MEM: " + Util.getUsedMemory());
		long startTimeDeser = Util.getMSec();
		scc = (LightSCC) LegacyFileUtil.deserializeObject(Params.test4SerDir 
				+ Params.test4SCCSer);
		System.out.println("SCC deserialization: " + Util.getDiffmsec(startTimeDeser) + "ms");
		startTimeDeser = Util.getMSec();
		System.out.println("MEM: " + Util.getUsedMemory());

		adj = (LightAdjacencyList) LegacyFileUtil.deserializeObject(
				Params.test4SerDir + Params.test4AdjSer);
		System.out.println("ADJ deserialization: " + Util.getDiffmsec(startTimeDeser) + "ms");
		startTimeDeser = Util.getMSec();
		System.out.println("MEM: " + Util.getUsedMemory());

		if(k == 3 || k == 4 || k == 5){
			cycles = (LightCycles) LegacyFileUtil.deserializeObject(
					Params.test4SerDir + Params.test4CyclesSer);

			System.out.println("CYCLES deserialization: " 
					+ Util.getDiffmsec(startTimeDeser) + "ms");
			startTimeDeser = Util.getMSec();
			System.out.println("MEM: " + Util.getUsedMemory());
		}

		memoryCleanPostRepetition();
		
		long startTime = -1;

		if(k != 0) {
			if(args.length < 2)
				throw new Error("Invalid number of arguments for launching type " 
						+ k);
			else {	
				startTime = Long.parseLong(args[1]);
			}			
		}

		switch(k){
		// 1-1 filter (heuristic)
		case 0:
			if(args.length < 3)
				throw new Error("Invalid number of arguments for "
						+ "launching filter heur");
			filterHeur(adj,scc,Integer.parseInt(args[3]));
			break;
			// asp conservative
		case 1: 
			if(args.length < 4)
				throw new Error("Invalid number of arguments for launching ASP");
			Params.ASPTimeout = Integer.parseInt(args[2]);
			asp(true,adj,scc,startTime,false,Integer.parseInt(args[3]));
			break;
			// asp nonconservative 
		case 2: 
			if(args.length < 4)
				throw new Error("Invalid number of arguments for launching ASP");
			Params.ASPTimeout = Integer.parseInt(args[2]);
			asp(false,adj,scc,startTime,false,Integer.parseInt(args[3]));
			break;
			// sga
		case 3:
			if(args.length < 5)
				throw new Error("Invalid number of arguments for launching SGA");
			Params.SGATimeout = Integer.parseInt(args[2]);
			double target = Double.parseDouble(args[3]);
			sga(adj,scc,startTime,cycles,target,Integer.parseInt(args[4]));
			break;
			// greedy diagnosis (cardinality optimization)
		case 4:
			if(args.length < 3)
				throw new Error("Invalid number of arguments for "
						+ "launching greedy heur (card)");
			greedyHeur(adj,scc,startTime,cycles,true,Integer.parseInt(args[3]));
			break;
			// greedy diagnosis (weight optimization)
		case 5:
			if(args.length < 3)
				throw new Error("Invalid number of arguments for "
						+ "launching greedy heur (weight)");
			greedyHeur(adj,scc,startTime,cycles,false,Integer.parseInt(args[3]));
			break;
			// 1-1 filtering (asp)
		case 6:
			if(args.length < 4)
				throw new Error("Invalid number of arguments for launching ASP");
			Params.ASPTimeout = Integer.parseInt(args[2]);
			asp(true,adj,scc,startTime,true,Integer.parseInt(args[3]));
			break;
			// 1-1 filtering (heur) + diagnosis
		case 7:
			if(args.length < 4)
				throw new Error("Invalid number of arguments for launching ASP");
			Params.ASPTimeout = Integer.parseInt(args[2]);
			filterDiag(adj,scc,Integer.parseInt(args[3]));
			break;
		default:
			throw new Error("Invalid type: " + k);
		}
	}

	private static void greedyHeur(LightAdjacencyList adj, LightSCC scc, 
			long startTime, LightCycles cycles, boolean cardOpt, 
			int repetitions){
		double greedyCTimeTot = 0, greedyCWeightTot = 0, greedyCSizeTot = 0;
		for (int i = 0; i < repetitions; i++) {
			Diagnosis d = adj.computeDiagnosisOnCycles(cycles, scc, cardOpt);
			greedyCTimeTot += d.getTime();
			greedyCWeightTot += d.getWeight();
			greedyCSizeTot += d.size();
			memoryCleanPostRepetition();
			System.out.println("!GREEDY"+(cardOpt ? "C":"W")+";"
					+d.getTime()+";"+d.getWeight()+";"+d.size());
		}
		if(greedyCTimeTot > 0)
			greedyCTimeTot /= repetitions;
		if(greedyCWeightTot > 0)
			greedyCWeightTot /= repetitions;
		if(greedyCSizeTot > 0)
			greedyCSizeTot /= repetitions;
		
		System.out.println("EXT;"+greedyCTimeTot+";"+greedyCWeightTot
				+";"+greedyCSizeTot);//+";"+d);
	}

	private static void sga(LightAdjacencyList adj, LightSCC scc, 
			long startTime, LightCycles cycles, double target,int repetitions){
		long sgaTimeTot = 0, sgaTime = 0;
		double sgaWeightTot = 0, sgaWeight = 0, sgaSizeTot = 0, sgaSize = 0;
		int i = 0;
		for (; i < repetitions; i++) {
			SGASolver solver = new SGASolver(adj, cycles, scc, target,
					Collections.<LightEdge> emptySet(),true);
			Diagnosis d = null;
			try {
				d = solver.computeDiagnosis();
				sgaTimeTot += sgaTime = d.getTime();
				sgaWeightTot += sgaWeight = d.getWeight();
				sgaSizeTot += sgaSize = d.size();
			} catch (InterruptedException | TimeoutException e1) {
				e1.printStackTrace();
				System.exit(1);
			} catch (OutOfMemoryError e) {
				sgaTimeTot = -1;
				sgaWeightTot = -1;
				sgaSizeTot = -1;
				break;
			}
			memoryCleanPostRepetition();
			System.out.println("!SGA;"+sgaTime+";"+sgaWeight+";"+sgaSize);
			
			if(sgaTime >= Params.SGATimeout*1000){
				++i;
				break;
			}
		}

		if(sgaTimeTot > 0)
			sgaTimeTot /= i;
		if(sgaWeightTot > 0)
			sgaWeightTot /= i;
		if(sgaSizeTot > 0)
			sgaSizeTot /= i;

		System.out.println("EXT;"+sgaTimeTot + ";" + sgaWeightTot + ";" 
				+ sgaSizeTot);// + ";" + d);
	}

	public static String asp(boolean conservative, LightAdjacencyList adj, 
			LightSCC scc, long startTime, boolean filter, int repetitions){

		double aspTime = 0, diagSize = 0, aspDiagWeight = 0;

		ASPSolver asp = new ASPSolver(adj, scc, 0, conservative, false, 
				Params.useDLV);
		Diagnosis d = null;
		boolean optimal = false;
		int i = 0;
		for (; i < repetitions; i++) {
			optimal = false;
			try {
				d = filter ? asp.computeFiltered() : asp.computeDiagnosis();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			} catch (TimeoutException e) {
				// timeout, no valid values for ASP
				if(Params.useDLV){
					aspTime = -1;
					aspDiagWeight = -1;
				}
			} catch (UnsatisfiableProblemException e) {
				//System.err.println(e.getMessage());
				System.out.println("UNSAT");
				return "U;-1;-1;-1";
			}
			if(d != null){
				aspTime += d.getTime();
				aspDiagWeight += d.getWeight();
				diagSize += d.size();
				optimal = d.isOptimal();
				if(!optimal){
					++i;
					break;
				}
			}
			else {
				aspTime = -1;
				aspDiagWeight = -1;
				diagSize = -1;
				break;
			}
			System.out.println("!" + (conservative ? "C":"NC" ) + 
					"-ASP;" + d.getTime()+";"+d.getWeight()+";"+d.size());
		}
		
		if(aspTime > 0)
			aspTime /= i;
		if(aspDiagWeight > 0)
			aspDiagWeight /= i;
		if(diagSize > 0)
			diagSize /= i;
		
		String res = (optimal ? "EXT;" : "T;" ) +aspTime + ";" + aspDiagWeight 
				+ ";" + diagSize; // + ";" + d);
		System.out.println(res);
		return res;
	}

	private static void filterHeur(LightAdjacencyList adj, LightSCC scc, 
			int repetitions){
		ArrayList<Double> timeHeurs = new ArrayList<>(repetitions);
		Diagnosis dHeur = null; double actTime = 0;
		for (int c = 0; timeHeurs.size() < repetitions;c++) {
			long timeMulti = Util.getMSec();
			dHeur = new Diagnosis(adj.filterMultipleCorrespondences(
					scc, false));
			timeHeurs.add(actTime = new Double(Util.getDiffmsec(timeMulti)));
			if(c % 5 == 0)// || Util.getPercentageFreeMem() < 60)
				memoryCleanPostRepetition();
			System.out.println(actTime+";"+dHeur.getWeight()+";"+dHeur.size());
		}
		dHeur.setTime(new Double(Util.getAvg(timeHeurs)).longValue());
		scc.printDimensions(adj);
		System.out.println("EXT;" + dHeur.getTime() + ";" + dHeur.getWeight() 
				+ ";" + dHeur.size()); //+ ";" + dHeur);
	}

	private static void memoryCleanPostRepetition(){
		Util.getUsedMemoryAndCleanLight(0,sleepGC);
	}

	private static void filterDiag(LightAdjacencyList adj, LightSCC scc, 
			int repetitions){
		double filterTimeTot = 0, filterWeightTot = 0, filterTime = 0, 
				filterWeight = 0, filterSizeTot = 0, filterSize = 0;
		Diagnosis d = null;
		for (int i = 0; i < repetitions; i++) {
			long timeFilterDiag = Util.getMSec();
			d = new Diagnosis(adj.filterMultipleCorrespondences(scc, 
					i==repetitions-1));

			filterTimeTot += filterTime = Util.getDiffmsec(timeFilterDiag);
			filterWeightTot += filterWeight = d.getWeight();
			filterSizeTot += filterSize = d.size();
			
			if(i % 5 == 0) //|| Util.getPercentageFreeMem() < 60)
				memoryCleanPostRepetition();
			System.out.println("!FILTDIAG(FILT);"+filterTime+";"+filterWeight+";"+filterSize);
		}
		if(filterTimeTot > 0)
			filterTimeTot /= repetitions;
		if(filterWeightTot > 0)
			filterWeightTot /= repetitions;
		if(filterSizeTot > 0)
			filterSizeTot /= repetitions;
		
		double aspTime = 0, diagSize = 0, aspDiagWeight = 0;
		ASPSolver asp = new ASPSolver(adj, scc, 0, false, false, Params.useDLV);		
		Diagnosis aspD = null;
		boolean optimal = false;
		int i = 0;
		
		for (; i < repetitions; i++) {
			optimal = false;
			try {
				aspD = asp.computeDiagnosis();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			} catch (TimeoutException e) {
				// timeout, no valid values for ASP
				if(Params.useDLV){
					aspTime = -1;
					aspDiagWeight = -1;
				}
			} catch (UnsatisfiableProblemException e) {
				System.err.println(e.getMessage());
			}
			if(aspD != null){
				aspTime += aspD.getTime();
				aspDiagWeight += aspD.getWeight();
				diagSize += aspD.size();
				optimal = aspD.isOptimal();
				if(!optimal){
					++i;
					break;
				}
			}
			else {
				aspTime = -1;
				aspDiagWeight = -1;
				diagSize = -1;
				break;
			}
			System.out.println("!FILTDIAG(ASP);"+aspD.getTime()+";"
					+aspD.getWeight()+";"+aspD.size()); //+";"+aspD);
		}
		
		if(aspTime > 0)
			aspTime /= i;
		if(aspDiagWeight > 0)
			aspDiagWeight /= i;
		if(diagSize > 0)
			diagSize /= i;
		
		double totTime = filterTimeTot+aspTime, 
				totWeight = aspDiagWeight >= 0 
					? (filterWeightTot+aspDiagWeight) : -1,
				totSize = diagSize >= 0 ? (filterSizeTot+diagSize) : -1;
		
		System.out.println("EXT;" + totTime + ";" 
				+ totWeight + ";" + totSize);// + ";" + d); 
	}
}
