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
import scc.graphDataStructure.LightEdge;
import scc.graphDataStructure.LightSCC;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import util.FileUtil;
import util.Params;
import util.Util;

import scc.algoSolver.ASPSolver;

public class DiagnosisThread implements Callable<Diagnosis> {

	private LightAdjacencyList adj;
	private LightSCC scc;
	private int numSCC;
	private boolean subOptimalDiag;
	private boolean useBridges;
	private boolean singleTry = false;
	
	private int extraMessagesVerbosity = 0;
	
	public DiagnosisThread(LightAdjacencyList adj, LightSCC scc, int numSCC, 
			boolean subOptimalDiag){
		this.adj = adj;
		this.scc = scc;
		this.numSCC = numSCC;
		this.subOptimalDiag = subOptimalDiag;
	}

	public DiagnosisThread(LightAdjacencyList adj, LightSCC scc, int numSCC, 
			boolean subOptimalDiag, boolean useBridges){
		this(adj,scc,numSCC,subOptimalDiag);
		this.useBridges = useBridges;
	}
	
	@Override
	public Diagnosis call() throws Exception {
		
		if(useBridges && Params.verbosity >= extraMessagesVerbosity)
			FileUtil.writeLogAndConsole(numSCC + " - useBridges = " + useBridges);
		int pre = scc.extractMappings(adj, false).size(),
				post = scc.extractMappings(adj, true).size();
		if(!Params.alwaysFilterMultiple && post != pre && Params.verbosity >= extraMessagesVerbosity)
			FileUtil.writeLogAndConsole(numSCC + " - " + post + " mappings out of " + pre );
		
		if(Params.verbosity > 0)
			FileUtil.writeLogAndConsole("SCC " + numSCC + " (" 
					+ scc.printDimensions(adj) + ")");

		if(useBridges)
			return computeDiagnosisUsingBridges();
		
		ASPSolver asp = new ASPSolver(adj, scc, numSCC, 
				Params.conservativeDiagnosis, false, Params.useDLV);
		Diagnosis d = null;
		try {
			d = asp.computeDiagnosis();
			if(d == null){
				if(!singleTry)
					return retryOnFailure();
				return null;
			}
			d.setOptimal(d.isOptimal() && !subOptimalDiag);
			
			if(Params.verbosity > 0)
				FileUtil.writeLogAndConsole(numSCC + " ASP diagnosis (size " + 
						d.size() + ") : " + d);
			if(Params.testMode && !d.isDiagnosis(adj,scc))
				throw new Error(numSCC + " ASP model is not a diagnosis!");
		} catch (InterruptedException e1) {
			FileUtil.writeLogAndConsole("InterruptedException in DiagnosisThread");
			e1.printStackTrace();
		} catch (TimeoutException e) {			
//			//if(Params.verbosity >= 0)
//				System.out.println(numSCC + " ASP timed out, relaunching " +
//						"after multiple occurrences filtering");
//			
//			if(!adj.filterMultipleCorrespondences(scc, true).isEmpty())
//				subOptimalDiag = false;
//			
//			return this.call();
			if(!singleTry)
				return retryOnFailure();
		}
		
		return d;
	}
	
	private Diagnosis computeDiagnosisUsingBridges() throws Exception {
		long startTime = Util.getMSec();
		Diagnosis d = new Diagnosis();
		d.setOptimal(false);

		if(Params.verbosity >= extraMessagesVerbosity)
			FileUtil.writeLogAndConsole(numSCC + " - Compute diagnosis using bridges");
		
		Set<LightEdge> bridges = new HashSet<>();
		
		try {
			scc.findBridges(adj, bridges);

			if(bridges == null || bridges.isEmpty()){
				if(Params.verbosity >= extraMessagesVerbosity)
					FileUtil.writeLogAndConsole(numSCC + 
						" - No bridges, removing all mappings");
				d.addAll(scc.extractMappings(adj, true));
				this.subOptimalDiag = true;
			}
			else {
//				d.addAll(bridges);	
				if(Params.verbosity >= extraMessagesVerbosity)
					FileUtil.writeLogAndConsole(numSCC + 
						" - Bridge diagnosis (" + bridges.size() 
						+ " mappings) in " 
						+ Util.getDiffmsec(startTime) + " (ms)");
				adj.removeMappings(bridges);
				this.subOptimalDiag = true;
			}
		} catch (InterruptedException e1) {
//			FileUtil.writeLogAndConsole(numSCC + 
//					" - InterruptedException in DiagnosisThread (using bridges)");
//			e1.printStackTrace();
			adj.removeMappings(bridges);
			if(scc.isProblematic(adj))
				return null;
			return d;
		}
		
//		FileUtil.writeLogAndConsole("Still problematic? " + scc.isProblematic(adj));				
		
		return d;
	}
	
	private Diagnosis retryOnFailure() throws Exception{
		
		if(Params.verbosity >= extraMessagesVerbosity)
			FileUtil.writeLogAndConsole("SCC " + numSCC + " - ASP timed out, " +
				"relaunching with filtering/bridges");
	
		Set<LightEdge> filtered = adj.filterMultipleCorrespondences(scc, true);
		
		if(Params.verbosity >= extraMessagesVerbosity)
			FileUtil.writeLogAndConsole(numSCC + " - FilteredNum: " + filtered.size());
		
		if(filtered.isEmpty()){
			this.useBridges = true;
			
			Future<Diagnosis> f = null;
			Diagnosis d = null;

			while(true){
				ExecutorService executor = Executors.newSingleThreadExecutor();
				f = executor.submit(this);
				executor.shutdown();
				
				try {
					d = f.get(Params.ASPTimeout,TimeUnit.SECONDS);
					if(this.useBridges && scc.isProblematic(adj)){
						if(Params.verbosity >= extraMessagesVerbosity)
							FileUtil.writeLogAndConsole(
								numSCC + " - Problematic after bridges, retry ASP");
						
						this.useBridges = false;
//						this.singleTry = true;
						continue;
					}
				} catch (ExecutionException e) {
					e.printStackTrace();
				} catch (InterruptedException e){
					if(Params.verbosity >= extraMessagesVerbosity)
						FileUtil.writeLogAndConsole(
							numSCC + " - ASP after bridges interrupted");
					break;
				} catch (TimeoutException e) {
					if(!f.isDone() == false && !f.isCancelled())
						f.cancel(true);
					
					if(this.useBridges){
						if(Params.verbosity >= extraMessagesVerbosity)
							FileUtil.writeLogAndConsole(
								numSCC + " - Timeout computing strong bridges, " +
								"removing all mappings");
						d = new Diagnosis();
						d.setOptimal(false);
						if(scc.isProblematic(adj))
							d.addAll(scc.extractMappings(adj, true));
						break;
					}
					else {
						if(Params.verbosity >= extraMessagesVerbosity)
							FileUtil.writeLogAndConsole(
								numSCC + " - Timeout computing ASP after bridges, " +
								"recompute bridges");
						this.useBridges = true;
//						this.singleTry = true;
					}
				}
				finally {
					if(!f.isCancelled() && !f.isDone())
						f.cancel(true);
					
					this.singleTry = true;

					executor.shutdownNow();
				}
				
				if(!scc.isProblematic(adj)){
					if(Params.verbosity >= extraMessagesVerbosity)
						FileUtil.writeLogAndConsole(numSCC + " - SCC not " +
								"problematic, exit");
					if(d == null){
						d = new Diagnosis();
						d.setOptimal(!this.subOptimalDiag);
					}
					break;
				}
			}
			
			if(d == null)
				throw new Error(numSCC + " - NULL diagnosis");
			return d;
		}
		else {
			subOptimalDiag = true;
		}
		
		return this.call();
	}
}
