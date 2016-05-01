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

import scc.exception.ClassificationTimeoutException;
import scc.exception.UnsatisfiableProblemException;
import scc.graphAlgo.Johnson;
import scc.graphDataStructure.Diagnosis;
import scc.graphDataStructure.LightAdjacencyList;
import scc.graphDataStructure.LightCycles;
import scc.graphDataStructure.LightEdge;
import scc.graphDataStructure.LightSCC;
import scc.graphDataStructure.LightSCCs;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import scc.algoSolver.ASPSolver;
import scc.algoSolver.SGASolver;
import scc.thread.TestDiagnosisThread;
import scc.util.LegacyFileUtil;
import util.OntoUtil;
import util.Params;
import util.Util;

public class Test2 {
	/*
	 * This test aims at comparing the time performance and 
	 * diagnosis quality of the SGA, ASP and greedy solver
	 */
	private static ExecutorService executor = 
			Executors.newFixedThreadPool(Params.NTHREADS);
	//public static List<ResultASPSGA> generalStats = Collections.synchronizedList(new LinkedList<ResultASPSGA>());

	private static String [] trackNames = {"anatomy","conference","largebio"};
	private static String trackName = trackNames[2];
	private static boolean whole = false;
	private static String ontoSize = whole ? "big" : "small";
	private static int count = 0, sccCount = 0, resumeCount, sccTotal, 
			minAlignSize = 6, maxAlignSize = 140, maxCycleSize = 100;
	
	public static final boolean 
		aspNonConsOn = true, 
		aspConsOn = true;
	public static boolean sgaOn = true;
	public static boolean greedyCardOn = true;
	public static boolean greedyWeightOn = true;
	public static final boolean filterDiagOn = true;

	/**
	 * @param args, 
	 * idx0 = parallel (true/false), 
	 * idx1 = repetition num (int), 
	 * idx2 = asp timeout (int, sec) 
	 * @throws IOException 
	 * @throws OWLOntologyCreationException 
	 */
	public static void main(String[] args) throws OWLOntologyCreationException, 
	IOException {

		//		int testRepetition = 1;
		Params.verbosity = 0;
		Params.alwaysTestDiagnosis = false;

		//		if(args.length == 0){
		//			Params.NTHREADS = 1;
		//			Params.ASPTimeout = 3*60; // 3 mins
		//			Params.SGATimeout = 3*60;
		//		}
		//		else if(args.length == 3){
		//			Params.test2Parallel = Boolean.parseBoolean(args[0]);
		//			testRepetition = Integer.parseInt(args[1]);
		//			Params.ASPTimeout = Integer.parseInt(args[2]);
		//			Params.SGATimeout = Params.ASPTimeout;
		//		}
		//		else {
		//			System.out.println("Usage: [multiThread (bool) repetitions ASPTimeout (sec)]");
		//		}

		if(args.length < 2)
			throw new Error("Repetition number and GB VM needed");

		Params.test2Repetitions = Integer.parseInt(args[0]);
		Params.test2VMGB = Double.parseDouble(args[1]);
		Params.test2Resume = Integer.parseInt(args[2]) == 1;

		System.out.println("Started: " + Util.getCurrTime());
		System.out.println("Repetitions: " + Params.test2Repetitions);
		System.out.println("ExtVM size: " + Params.test2VMGB);
		System.out.println("Resuming: " + Params.test2Resume);

		LegacyFileUtil.createDirPath(Params.test2OutDir);

		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		String startTime = sdf.format(Calendar.getInstance().getTime());
		FileWriter outFile = new FileWriter(Params.test2OutDir 
				+ "test2_" + Params.test2Repetitions + "rep.txt", 
				Params.test2Resume);
		if(Params.test2Resume){
			resumeCount = LegacyFileUtil.countLines(Params.test2OutDir 
					+ "test2_" + Params.test2Repetitions + "rep.txt");
			System.out.println("Resuming from line " + resumeCount);
		}
		PrintWriter out = new PrintWriter(outFile);

		System.out.println(Params.test2Repetitions + " REPETITION(S)");
		asmovTest(out);
		OntoUtil.unloadAllOntologies();
		lilyTest(out);
		OntoUtil.unloadAllOntologies();
		for (String name : trackNames) {
			trackName = name;
			if(trackName.equals("largebio")){
				whole = true;
				ontoSize = "big";
				trackTest(out);
				OntoUtil.unloadAllOntologies();
				whole = false;
				ontoSize = "small";
				trackTest(out);
				// do not unload (would be useless, it is the last test)
			}
			else {
				trackTest(out);
				OntoUtil.unloadAllOntologies();
			}
		}
		System.out.println("Mapping range = [" + minAlignSize + ", " 
				+ maxAlignSize + "]");
		System.out.println("Analyzed/Total SCCs : " + sccCount + "/" 
				+ sccTotal);
		System.out.println("Start: " + startTime);
		System.out.println("End: " 
				+ new SimpleDateFormat("HH:mm:ss").format(
						Calendar.getInstance().getTime()));
		out.close();
		outFile.close();
	}

	public static void testJar(String mapping, OWLOntology fstO, 
			OWLOntology sndO, PrintWriter out, boolean unloadOnto) 
					throws OWLOntologyCreationException, IOException, 
					ClassificationTimeoutException {
		Util.getUsedMemoryAndClean(1024,250);

		LightAdjacencyList adj = new LightAdjacencyList(fstO, sndO, null, 
				unloadOnto);

		System.out.println("#V = " + adj.getNodes().size());

		LightSCCs localSCCs = adj.getLocalSCCs();//, globSCCs = null; 
		Set<LightSCC> problematicSCCs = new HashSet<>();

		//Set<LightEdge> mappings = 
		adj.loadMappings(new File(mapping),null,Params.fullDetection);

		//Set<LightEdge> problemsFlattened = new HashSet<LightEdge>();
		//globSCCs = 
		adj.computeGlobalSCCsAndProblematicMappings(problematicSCCs, null);

		int numSCC = 0;
		
		for (LightSCC scc : problematicSCCs) {
			Set<LightEdge> align = scc.extractMappings(adj, false);
			int alignSize = align.size();
			if(alignSize < minAlignSize || alignSize > maxAlignSize) {
				// otherwise impossible to check if the global diagnosis is correct
				adj.removedMappings.addAll(align);
			}
		}

		// serialize adjacency list
		LegacyFileUtil.serializeObject(Params.test4SerDir + Params.test4AdjSer, adj);

		for (LightSCC scc : problematicSCCs) {

			LightCycles cycles = null;
			long cycleTime = -1;

			double aspCTime = 0, aspCWeight = 0, aspCSize = 0;
			double aspNCTime = 0, aspNCWeight = 0, aspNCSize = 0;
			double sgaTime = 0, sgaWeight = 0, sgaSize = 0;
			double greedyCTime = 0, greedyCWeight = 0, greedyCSize = 0;
			double greedyWTime = 0, greedyWWeight = 0, greedyWSize = 0;
			double filterTime = 0, filterWeight = 0, filterSize = 0;
			boolean oldSgaOn = Test2.sgaOn, 
					oldGreedyCardOn = Test2.greedyCardOn, 
					oldGreedyWeightOn = Test2.greedyWeightOn;
			
			int numMappings = scc.extractMappings(adj, true).size(); 

			++sccTotal;
			if(sccTotal % 1000 == 0)
				System.out.println(sccTotal + " SCCs");
			
			if(numMappings >= minAlignSize && numMappings <= maxAlignSize) {
				++sccCount;
				if(Params.test2Resume && sccCount <= resumeCount)
					continue;
				
				if(Params.test2Resume)
					System.out.println("SCC (resume counter): " + sccCount);

				System.out.println("SCC (align) " + (numSCC++) 
						+ ", M = " + numMappings);

				System.out.println(Util.getCurrTime());

				LegacyFileUtil.serializeObject(Params.test4SerDir 
						+ Params.test4SCCSer, scc);
				
				if(numMappings > maxCycleSize)
					Test2.sgaOn = Test2.greedyCardOn = Test2.greedyWeightOn 
							= false;

				cycleTime = Util.getMSec();
				if(Test2.sgaOn || Test2.greedyCardOn || Test2.greedyWeightOn){
					cycles = new Johnson().findElementaryCycles(adj,scc,
							localSCCs);
					cycleTime = Util.getDiffmsec(cycleTime);
					LegacyFileUtil.serializeObject(Params.test4SerDir 
							+ Params.test4CyclesSer, cycles);
					System.out.println(cycles.size() + " cycles in " 
							+ cycleTime + "ms");
				}
				else {
					cycles = null;
					cycleTime = -1;
				}

				System.out.println(Util.getCurrTime());

				double [] res;
				int timeout = 60;
				Params.ASPTimeout = timeout;

				if(Test2.aspNonConsOn){
					res = LegacyFileUtil.parseOutput(
							ExternalMain.asp(false, adj, scc, 
									Util.getMSec(),false,
									Params.test2Repetitions)); 
					aspNCTime = res[0];
					aspNCWeight = res[1];
					aspNCSize = res[2];
				}

				System.out.println(Util.getCurrTime());

				if(Test2.aspConsOn){
					res = LegacyFileUtil.parseOutput(
							ExternalMain.asp(true, adj, scc, 
									Util.getMSec(), false,
									Params.test2Repetitions));
					
					aspCTime = res[0];
					aspCWeight = res[1];
					aspCSize = res[2];
				}

				System.out.println(Util.getCurrTime());

				if(Test2.sgaOn){
					res = LegacyFileUtil.launchJar(LegacyFileUtil.getJarString(3,  
							Util.getMSec(),timeout,
							Math.max(aspNCWeight, 0), 
							Params.test2Repetitions));
					sgaTime = res[0];
					sgaWeight = res[1];
					sgaSize = res[2];
				}

				System.out.println(Util.getCurrTime());

				if(Test2.greedyCardOn){
					res = LegacyFileUtil.launchJar(LegacyFileUtil.getJarString(4, 
							Util.getMSec(), timeout, 
							Params.test2Repetitions));
					greedyCTime = res[0];
					greedyCWeight = res[1];
					greedyCSize = res[2];
				}

				System.out.println(Util.getCurrTime());

				if(Test2.greedyWeightOn){
					res = LegacyFileUtil.launchJar(LegacyFileUtil.getJarString(5, 
							Util.getMSec(),timeout,
							Params.test2Repetitions));
					greedyWTime = res[0];
					greedyWWeight = res[1];
					greedyWSize = res[2];
				}

				System.out.println(Util.getCurrTime());

				if(Test2.filterDiagOn){
					res = LegacyFileUtil.launchJar(LegacyFileUtil.getJarString(7, 
							Util.getMSec(),timeout,
							Params.test2Repetitions));
					filterTime = res[0];
					filterWeight = res[1];
					filterSize = res[2];
				}

				System.out.println(Util.getCurrTime());
				
				String result = ResultASPSGA.printResult(adj, scc, cycles, 
						new Double(cycleTime).longValue(),  
						new Double(aspCTime).longValue(), 
						new Double(aspNCTime).longValue(), 
						new Double(sgaTime).longValue(), 
						new Double(greedyCTime).longValue(),
						new Double(greedyWTime).longValue(),
						new Double(filterTime).longValue(),
						aspCWeight, aspNCWeight, sgaWeight, 
						greedyCWeight, greedyWWeight, filterWeight,
						aspCSize, aspNCSize, sgaSize, greedyCSize, 
						greedyWSize, filterSize);				
				out.append(result);
				out.flush();
				
				if(numMappings > maxCycleSize){
					Test2.sgaOn = oldSgaOn;
					Test2.greedyCardOn = oldGreedyCardOn; 
					Test2.greedyWeightOn = oldGreedyWeightOn;
				}
			}
		}
		LegacyFileUtil.deleteAllFiles(Params.test4SerDir);
	}

	public static void testOld(String[] args, OWLOntology fstO, OWLOntology sndO, 
			PrintWriter out, boolean unloadOnto) 
					throws OWLOntologyCreationException, IOException, ClassificationTimeoutException {

		String mapping = args[0];

		Util.getUsedMemoryAndClean(2048,250);

		LightAdjacencyList adj = new LightAdjacencyList(fstO, sndO, null, 
				unloadOnto);

		System.out.println("#V = " + adj.getNodes().size());

		LightSCCs localSCCs = adj.getLocalSCCs(), globSCCs = null; 
		Set<LightSCC> problematicSCCs = new HashSet<>();

		Set<LightEdge> mappings = adj.loadMappings(new File(mapping),null,
				Params.fullDetection);

		Set<LightEdge> problemsFlattened = new HashSet<LightEdge>();
		globSCCs = adj.computeGlobalSCCsAndProblematicMappings(problematicSCCs, 
				null);

		int numSCC = 0;

		List<Future<String>> handles = new ArrayList<Future<String>>();
		Future<String> future;
		//if(executor.isShutdown())
		executor = Executors.newFixedThreadPool(Params.NTHREADS);

		for (LightSCC scc : problematicSCCs) {
			if(scc.extractMappings(adj, true).size() >= 116)
			{
				// otherwise impossible to check if the global diagnosis is correct
				adj.removedMappings.addAll(scc.extractMappings(adj, false));
				continue;
			}

			if(Params.test2Parallel){
				future = executor.submit(
						new TestDiagnosisThread(adj,scc,localSCCs,numSCC++));
				handles.add(future);
			}
			else {
				System.out.println("SCC " + (numSCC++) + " (" 
						+ scc.printDimensions(adj) + ")");			

				LightCycles cycles = null;
				long cycleTime = Util.getMSec();
				if(Test2.sgaOn || Test2.greedyCardOn){
					cycles = new Johnson().findElementaryCycles(adj,scc,
							localSCCs);
					cycleTime = Util.getDiffmsec(cycleTime);
					System.out.println("Cycle in " 
							+ cycleTime + "ms");
				}
				else
					cycleTime = -1;

				long aspConsTime = Util.getMSec(), 
						aspNonConsTime = Util.getMSec(), 
						sgaTime = -1, simpleTime = -1;
				double aspNonConsDiagWeight = -1, aspConsDiagWeight = -1, 
						sgaDiagWeight = -1, simpleWeight = -1;

				boolean saveFile = false;
				if(Test2.sgaOn || Test2.greedyCardOn){
					saveFile = cycles.size() > Params.numCyclesSaveFile;
				}
				else {
					saveFile = scc.extractMappings(adj, true).size() 
							>= Params.numMappingsSaveFile;
				}
				Diagnosis d = null;
				double [] res;

				if(Test2.aspNonConsOn){
					res = launchASP(false, adj, scc, numSCC, saveFile, aspConsTime);
					aspNonConsTime = new Double(res[0] != -1 ? res[0] : res[1]).longValue();
					aspNonConsDiagWeight = res[2];
				}

				if(Test2.aspConsOn){
					res = launchASP(true, adj, scc, numSCC, saveFile, aspNonConsTime);
					aspConsTime = new Double(res[0] != -1 ? res[0] : res[1]).longValue();
					aspConsDiagWeight = res[2];
				}

				if(Test2.sgaOn){
					System.out.println(numSCC + " SGA Start");
					SGASolver solver = new SGASolver(adj, cycles, scc, 
							aspNonConsDiagWeight,Collections.<LightEdge> emptySet(),
							true);
					try {
						d = solver.computeDiagnosis();
						if(Params.testMode && !d.isDiagnosis(adj,scc))
							throw new Error(numSCC 
									+ " SGA model is not a diagnosis: " + d);
						if(Params.verbosity > 0)
							System.out.println(numSCC + " Solver diagnosis: " + d);
						sgaTime = d.getTime();
						sgaDiagWeight = d.getWeight();
					} catch (InterruptedException | TimeoutException e1) {
						e1.printStackTrace();
						System.exit(1);
					} catch (OutOfMemoryError e) {
						sgaTime = -1;
						sgaDiagWeight = -1;
						System.out.println(numSCC + " SGA out of memory!");
					}
					System.out.println("SGA done! " + numSCC);
				}

				if(Test2.greedyCardOn){
					System.out.println(numSCC + " Greedy Start");
					d = adj.computeDiagnosisOnCycles(cycles, scc, true);
					simpleTime = d.getTime();
					simpleWeight = d.getWeight();
					System.out.println("Greedy done! " + numSCC);
				}
				out.print(ResultASPSGA.printResult(adj, scc, cycles, cycleTime,  
						aspConsTime, aspNonConsTime, sgaTime, simpleTime,-1,-1, 
						aspConsDiagWeight, aspNonConsDiagWeight, 
						sgaDiagWeight, simpleWeight,-1,-1,-1,-1,-1,-1,-1,-1));
				out.flush();
			}
		}

		if(Params.test2Parallel){
			for (Future<String> h : handles) {
				try {
					out.print(h.get());
					//					String r = h.get();
					//					if(r != null)
					//						out.print(r);
				} catch (ExecutionException | InterruptedException e) {
					e.printStackTrace();
				}
			}
			out.flush();
		}
		executor.shutdownNow();

		LegacyFileUtil.deleteAllFiles(Params.tmpDir);

		//System.out.println("GlobalDiagnosis: " + adj.removedMappings.toString().replace(",", ",\n"));

		if(Params.testMode){
			adj.computeGlobalSCCsAndProblematicMappings(problematicSCCs, null);
			if(problematicSCCs.size() != 0){
				for (LightSCC lightSCC : problematicSCCs) {
					System.out.println(lightSCC.printDimensions(adj));
					lightSCC.printProblematicSCC(adj);
					for (LightEdge e : lightSCC.extractMappings(adj, true)) {
						if(adj.removedMappings.contains(e))
							System.out.println(e + " in scc even if in diagnosis");
					}

					for (LightEdge e : lightSCC.extractMappings(adj, false)) {
						if(adj.removedMappings.contains(e))
							System.out.println(e + " deleted from scc");
					}
				}
				throw new Error("NOT a diagnosis!");
			}
		}
		System.out.println("Diagnosis size: " + adj.removedMappings.size() 
				+ "/" + mappings.size());
	}

	public static double [] launchASP(boolean conservative, 
			LightAdjacencyList adj, LightSCC scc, int numSCC, boolean saveFile, 
			double startTime){

		double aspTime = -1, aspThreadTime = -1, diagSize = -1, aspDiagWeight = -1;

		ASPSolver asp = new ASPSolver(adj, scc, numSCC, 
				conservative, saveFile, Params.useDLV);
		Diagnosis d = null;

		System.out.println(numSCC + " ASP Start");
		try {
			d = asp.computeDiagnosis();
			if(Params.verbosity > 0)
				System.out.println(numSCC + " ASP diagnosis: " + d);
			if(Params.testMode && !d.isDiagnosis(adj,scc))
				throw new Error(numSCC + " ASP model is not a diagnosis!");
		} catch (InterruptedException e1) {
			System.out.println("InterruptedException in DiagnosisThread");
			e1.printStackTrace();
		} catch (TimeoutException e) {
			// timeout, no valid values for ASP
			if(Params.useDLV){
				aspTime = -1;
				aspDiagWeight = -1;
				if(Params.verbosity > 0)
					System.out.println(numSCC + " ASP timed out");
			}
		} catch (UnsatisfiableProblemException e) {
			System.err.println(e.getMessage());
		}
		System.out.println("ASP done! " + numSCC);
		if(d != null){
			aspTime = d.getTime();
			aspThreadTime = Util.getDiffmsec(startTime);
			aspDiagWeight = d.getWeight();
			diagSize = d.size();
			System.out.println("Clasp time = " + aspTime
					+ "\nThread time = " + aspThreadTime);
		}
		return new double[]{aspTime, aspThreadTime, aspDiagWeight, diagSize};
	}

	private static void lilyTest(PrintWriter out) 
			throws OWLOntologyCreationException, IOException{
		lilyTestAnatomy(out);
		lilyTestConference(out);
	}

	private static void lilyTestAnatomy(PrintWriter out) 
			throws OWLOntologyCreationException, IOException {

		String mappingDir = Params.dataFolder + "lily/oaei2007/anatomy";

		String fstOnto = mappingDir + "/mouse.owl";
		String sndOnto = mappingDir + "/human.owl";

		OWLOntology fstO = null;
		OWLOntology sndO = null;

		fstO = OntoUtil.load(fstOnto, true, OntoUtil.getManager(false));
		sndO = OntoUtil.load(sndOnto, true, OntoUtil.getManager(false));

		String [] myArgs = new String[4];

		for (int i = 1; i <= 3; i++) {
			myArgs[0] = mappingDir + "/lily" + i + ".rdf";

			if(new File(myArgs[0]).exists()){
				//testOld(myArgs,fstO,sndO,out,i==3);
				try {
					testJar(myArgs[0],fstO,sndO,out,i==3);
				} catch (ClassificationTimeoutException e) {
					System.err.println(e.getMessage());
					continue;
				}
			}
		}
	}

	private static void lilyTestConference(PrintWriter out) 
			throws OWLOntologyCreationException, IOException {

		String mappingDir = Params.dataFolder + "lily/oaei2007/conference";
		File directory = new File(mappingDir);
		String [] myArgs = new String[4];

		OWLOntology fstO = null;
		OWLOntology sndO = null;

		File[] files = directory.listFiles(
				new FilenameFilter() {
					public boolean accept(File dir, String name) {
						return name.toLowerCase().endsWith(".rdf") 
								&& !name.toLowerCase().startsWith(".");
					}
				}	
				);
		Arrays.sort(files);

		myArgs[0] = myArgs[1] = myArgs[2] = myArgs[3] = null;

		for (File elem : files){
			if(elem.isFile()){
				OntoUtil.getManager(true);

				String [] ontos = elem.getName().substring(0, 
						elem.getName().length()-4).split("-");
				fstO = OntoUtil.load(elem.getParentFile() + "/" + ontos[0] 
						+ ".owl", true, OntoUtil.getManager(false));
				if(!ontos[0].equals(ontos[1]))
					sndO = OntoUtil.load(elem.getParentFile() + "/" + ontos[1] 
							+ ".owl", true, OntoUtil.getManager(false));
				else
					sndO = fstO;
				myArgs[0] = elem.getAbsolutePath();
				//testOld(myArgs,fstO,sndO,out,false);
				try {
					testJar(myArgs[0],fstO,sndO,out,false);
				} catch (ClassificationTimeoutException e) {
					System.err.println(e.getMessage());
					continue;
				}
			}
		}
	}

	private static void asmovTest(PrintWriter out) 
			throws OWLOntologyCreationException, IOException{
		String mappingDir = Params.dataFolder + "asmov/";
		String [] years = {"2008","2009","2010"};

		for (String year : years) {
			File directory = new File(mappingDir + "ASMOV_" + year + "/");

			String fstOnto = mappingDir + "ASMOV_" + year + "/anatomy/mouse.owl";
			String sndOnto = mappingDir + "ASMOV_" + year + "/anatomy/human.owl";

			OWLOntology fstO = null;
			OWLOntology sndO = null;

			fstO = OntoUtil.load(fstOnto, true, OntoUtil.getManager(false));
			sndO = OntoUtil.load(sndOnto, true, OntoUtil.getManager(false));

			String [] myArgs = new String[4];

			for (int i = 1; i <= 4; i++) {
				myArgs[0] = directory.getAbsolutePath() + "/anatomy/ASMOV" 
						+ i + ".rdf";

				if(new File(myArgs[0]).exists()){
					//testOld(myArgs,fstO,sndO,out,i==4);
					try {
						testJar(myArgs[0],fstO,sndO,out,i==4);
					} catch (ClassificationTimeoutException e) {
						System.err.println(e.getMessage());
						continue;
					}
				}
			}

			directory = new File(mappingDir + "ASMOV_" + year + "/conference/");

			File[] files = directory.listFiles(
					new FilenameFilter() {
						public boolean accept(File dir, String name) {
							return name.toLowerCase().endsWith(".rdf") 
									&& !name.toLowerCase().startsWith(".");
						}
					}	
					);
			Arrays.sort(files);

			myArgs[0] = myArgs[1] = myArgs[2] = myArgs[3] = null;

			for (File elem : files){
				if(elem.isFile()){
					OntoUtil.getManager(true);
					String [] ontos = elem.getName().substring(0, 
							elem.getName().length()-4).split("-");
					fstO = OntoUtil.load(elem.getParentFile() + "/" + ontos[0] 
							+ ".owl", true, OntoUtil.getManager(false));
					sndO = OntoUtil.load(elem.getParentFile() + "/" + ontos[1] 
							+ ".owl", true, OntoUtil.getManager(false));
					myArgs[0] = elem.getAbsolutePath();

					//testOld(myArgs,fstO,sndO,out,false);
					try {
						testJar(myArgs[0],fstO,sndO,out,false);
					} catch (ClassificationTimeoutException e) {
						System.err.println(e.getMessage());
						continue;
					}
				}
			}
		}
	}

	private static void trackTest(PrintWriter out) 
			throws OWLOntologyCreationException, IOException {
		
		String mappingDir = Params.dataFolder + "oaei2012/" + trackName + "/";
		String [] myArgs = new String[4];

		File directory = new File(mappingDir + "alignments/");
		File[] files = directory.listFiles(
				new FilenameFilter() {
					public boolean accept(File dir, String name) {
						return !name.toLowerCase().startsWith(".");
					}
				}	
				);
		Arrays.sort(files);
		String fstOnto = null, sndOnto = null;

		if(trackName.equalsIgnoreCase("anatomy")){
			fstOnto = mappingDir + "onto/mouse.owl";
			sndOnto = mappingDir + "onto/human.owl";
		}

		OWLOntology fstO = null, sndO = null;

		if(fstOnto != null && sndOnto != null){
			OntoUtil.getManager(true);
			fstO = OntoUtil.load(fstOnto, true, OntoUtil.getManager(false));
			sndO = OntoUtil.load(sndOnto, true, OntoUtil.getManager(false));
		}

		for (File elem : files){
			if(elem.getName().endsWith("GRAPHDBG.rdf"))
				continue;

			if(elem.isDirectory()){

				if(trackName.equalsIgnoreCase("largebio")){

					File[] matchersDir = elem.listFiles(
							new FilenameFilter() {
								public boolean accept(File dir, String name) {
									return !name.toLowerCase().startsWith(".") 
											&& !name.toLowerCase().endsWith(
													"GRAPHDBG.rdf");
								}
							}	
							);

					for (File dir : matchersDir) {
						if(dir.isFile()){
							String [] tokens = dir.getName().split("_");
							if(!(tokens.length > 3) && tokens[1].equals(ontoSize)){
								String [] ontoNames = tokens[2].split("2");
								ontoNames[1] = ontoNames[1].replace(".rdf","");

								//								boolean skip = true;
								//								
								//								// NAME SELECTOR
								//								if((tokens[0].equals("hertuda") && tokens[1].equals("small"))
								//										&& tokens[2].equals("fma2snomed.rdf"))
								//									skip = false;
								//								
								////								if(skip && (tokens[0].equals("wmatch") && tokens[1].equals("small") 
								////										&& tokens[2].equals("fma2nci.rdf")))
								////									skip = false;
								//
								//								if(skip)
								//									continue;

								fstOnto = elem.getParentFile().getParent() 
										+ "/onto/" + Params.largebioOntologies.get(
											tokens[1] + ontoNames[0] + ontoNames[1]);
								sndOnto = elem.getParentFile().getParent() 
										+ "/onto/" + Params.largebioOntologies.get(
											tokens[1] + ontoNames[1] + ontoNames[0]);

								OntoUtil.getManager(true);
								fstO = OntoUtil.load(fstOnto, true, OntoUtil.getManager(false));
								sndO = OntoUtil.load(sndOnto, true, OntoUtil.getManager(false));

								myArgs[0] = dir.getAbsolutePath();
								myArgs[1] = dir.getAbsolutePath().replace(
										".rdf", "_GRAPHDBG.rdf");
								myArgs[2] = dir.getAbsolutePath().replace(
										".rdf", "_repaired_with_Alcomo_Hermit.rdf");
								myArgs[3] = dir.getAbsolutePath().replace(
										".rdf", "_repaired_with_LogMap.rdf"); 

								if(tokens[0].equals("logmap2noe"))
									myArgs[2] = myArgs[3] = null;

								if(new File(myArgs[0]).exists()){
									//testOld(myArgs,fstO,sndO,out,true);
									try {
										testJar(myArgs[0],fstO,sndO,out,true);
									} catch (ClassificationTimeoutException e) {
										System.err.println(e.getMessage());
										continue;
									}
								}
							}
						}
					}
				}
			}

			else if(elem.isFile()){
				if(trackName.equalsIgnoreCase("anatomy")){
					myArgs[0] = mappingDir + "alignments/" + elem.getName();
					myArgs[1] = mappingDir + "alignments/" 
							+ elem.getName().subSequence(
									0, elem.getName().lastIndexOf(".")) 
							+ "_GRAPHDBG.rdf";
				}

				else if(trackName.equalsIgnoreCase("conference")){
					myArgs = new String[4];
					OntoUtil.getManager(true);
					String [] ontos = elem.getName().substring(
							0, elem.getName().length()-4).split("-");
					fstO = OntoUtil.load(elem.getParentFile().getParentFile() 
							+ "/onto/" + ontos[1] + ".owl", true, OntoUtil.getManager(false));
					sndO = OntoUtil.load(elem.getParentFile().getParentFile() 
							+ "/onto/" + ontos[2] + ".owl", true, OntoUtil.getManager(false));
					myArgs[0] = elem.getAbsolutePath();
				}

				if(new File(myArgs[0]).exists()){
					//testOld(myArgs,fstO,sndO,out,false);
					try {
						testJar(myArgs[0],fstO,sndO,out,false);
					} catch (ClassificationTimeoutException e) {
						System.err.println(e.getMessage());
						continue;
					}
					System.out.println(++count);
				}
			}
		}
	}
}
