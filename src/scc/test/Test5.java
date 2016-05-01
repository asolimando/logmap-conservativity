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

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import scc.util.LegacyFileUtil;
import util.OntoUtil;
import util.Params;
import util.Util;
import enumerations.REASONER_KIND;
import scc.exception.ClassificationTimeoutException;
import scc.graphDataStructure.Diagnosis;
import scc.graphDataStructure.LightAdjacencyList;
import scc.graphDataStructure.LightEdge;
import scc.graphDataStructure.LightSCC;
import scc.graphDataStructure.LightSCCs;

public class Test5 {
	/*
	 * This test aims at testing the heuristic completeness 
	 */
	private static String [] trackNames = {"anatomy","conference","largebio"};
	private static String trackName = trackNames[2];
	private static boolean whole = false;
	private static String ontoSize = whole ? "big" : "small";
	private static int count;
//	private static String [] filter = {
//		"ASMOV_2008/conference/Cocus-confious.rdf",
//		"ASMOV_2008/conference/confious-iasted.rdf",
//		"MEDLEY-iasted-sigkdd.rdf",
//		"ASMOV_2009/conference/confious-OpenConf.rdf",
//		"ASMOV_2010/conference/cmt-linklings.rdf",		
//		"ASMOV_2010/conference/cocus-confious.rdf",
//		"ASMOV_2010/conference/cocus-iasted.rdf",
//		"ASMOV_2010/conference/cocus-linklings.rdf",
//		"ASMOV_2010/conference/cocus-openconf.rdf",
//		"ASMOV_2010/conference/conference-confious.rdf",
//	};

	private static String [] pick = {
		"MaasMatch-anatomy-track1.rdf",
		"MEDLEY-ekaw-sigkdd.rdf",
		"ASE-cmt-iasted.rdf",
		"ASE-edas-ekaw.rdf",
		"ASE-edas-sigkdd.rdf",
		"MEDLEY-cmt-ekaw.rdf",
		"AROMA-ekaw-iasted.rdf",
		"ASE-cmt-ekaw.rdf",
		"MaasMatch-cmt-ekaw.rdf",
		"AROMA-edas-ekaw.rdf",
	};
	
//	private static ExecutorService executor = 
//			Executors.newFixedThreadPool(Params.NTHREADS);
	
	/**
	 * @throws IOException 
	 * @throws OWLOntologyCreationException 
	 */
	public static void main(String[] args) throws OWLOntologyCreationException, 
	IOException {

		Params.verbosity = 0;
		Params.alwaysTestDiagnosis = false;

		LegacyFileUtil.createDirPath(Params.test5OutDir);

		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		String startTime = sdf.format(Calendar.getInstance().getTime());
		
		String prefixFile = "test5_";
		if(Params.reasonerKind == REASONER_KIND.HERMIT)
			prefixFile = prefixFile + "hermit";
		else if(Params.reasonerKind == REASONER_KIND.PELLET)
			prefixFile = prefixFile + "pellet";
		else
			prefixFile = prefixFile + "struct";
		
		FileWriter outFile = new FileWriter(Params.test5OutDir 
				+ prefixFile + ".txt"), 
				outLogFile = new FileWriter(Params.test5OutDir 
						+ prefixFile + "_log.txt");
		PrintWriter out = new PrintWriter(outFile), 
				outLog = new PrintWriter(outLogFile);

		asmovTest(out, outLog);
		OntoUtil.unloadAllOntologies();
		lilyTest(out, outLog);
		OntoUtil.unloadAllOntologies();
		for (String name : trackNames) {
			trackName = name;
			if(trackName.equals("largebio")){
				Params.alignOntoClassificationTimeout = 20 * 60;
				Params.inputOntoClassificationTimeout = 20 * 60;
//				whole = true;
//				ontoSize = "big";
//				trackTest(out, outLog);
//				OntoUtil.unloadAllOntologies();
				whole = false;
				ontoSize = "small";
				trackTest(out, outLog);
				// do not unload (would be useless, it is the last test)
				OntoUtil.unloadAllOntologies();
			}
			else if(trackName.equals("anatomy")){
				trackTest(out,outLog);
				OntoUtil.unloadAllOntologies();
			}
			else {
				trackTest(out,outLog);
				OntoUtil.unloadAllOntologies();
			}
		}

		LegacyFileUtil.writeFileAndConsole(outLog, "Start: " + startTime);
		LegacyFileUtil.writeFileAndConsole(outLog, "End: " 
				+ new SimpleDateFormat("HH:mm:ss").format(
						Calendar.getInstance().getTime()));

		out.close();
		outFile.close();
		outLog.close();
		outLogFile.close();
	}

	private static void lilyTest(PrintWriter out, PrintWriter outLog) 
			throws OWLOntologyCreationException, IOException{
		lilyTestAnatomy(out, outLog);
		lilyTestConference(out, outLog);
	}

	private static void lilyTestAnatomy(PrintWriter out, PrintWriter outLog) 
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
				test(myArgs[0],fstO,sndO,out,outLog,i==3);
			}
		}
	}

	private static void lilyTestConference(PrintWriter out, PrintWriter outLog) 
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

				String [] ontos = elem.getName().substring(0, 
						elem.getName().length()-4).split("-");
				fstO = OntoUtil.load(elem.getParentFile() + "/" + ontos[0] 
						+ ".owl", true, OntoUtil.getManager(true));
				if(!ontos[0].equals(ontos[1]))
					sndO = OntoUtil.load(elem.getParentFile() + "/" + ontos[1] 
							+ ".owl", true, OntoUtil.getManager(false));
				else
					sndO = fstO;
				myArgs[0] = elem.getAbsolutePath();
				test(myArgs[0],fstO,sndO,out,outLog,false);
			}
		}
	}

	private static void asmovTest(PrintWriter out, PrintWriter outLog) 
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
					test(myArgs[0],fstO,sndO,out,outLog,i==4);
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

					test(myArgs[0],fstO,sndO,out,outLog,false);
				}
			}
		}
	}

	private static void trackTest(PrintWriter out, PrintWriter outLog) 
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
									test(myArgs[0],fstO,sndO,out,outLog,true);
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
							+ "/onto/" + ontos[1] + ".owl", true, 
							OntoUtil.getManager(false));
					sndO = OntoUtil.load(elem.getParentFile().getParentFile() 
							+ "/onto/" + ontos[2] + ".owl", true, 
							OntoUtil.getManager(false));
					myArgs[0] = elem.getAbsolutePath();
				}

				if(myArgs[0] == null)
					System.err.println("Mapping pathname is null!");

				if(new File(myArgs[0]).exists()){
					test(myArgs[0],fstO,sndO,out,outLog,false);
					System.out.println(++count);
				}
			}
		}
	}

	public static void test(String mappingFile, OWLOntology fstO, 
			OWLOntology sndO, PrintWriter out, PrintWriter outLog, 
			boolean unloadOnto) 
					throws OWLOntologyCreationException, IOException {		

		LegacyFileUtil.writeFileAndConsole(outLog, "\nTEST START: " 
				+ Util.getCurrTime());

//		boolean ok = false;
//		for (String s : pick) {
//			if(mappingFile.contains(s)){
//				ok = true;
//				break;
//			}
//		}
//		if(!ok)
//			return;
		
		// hermit experiences a stackoverflow...
//		if(mappingFile.contains("AROMA-anatomy-track1.rdf") || 
//				mappingFile.contains("MaasMatch-anatomy-track1.rdf"))
//			return;
//		for (String s : filter) {
//			if(mappingFile.contains(s)){
//				System.out.println("Filtered " + mappingFile);
//				return;
//			}
//		}

		double totalStartTime = Util.getMSec();

		LightAdjacencyList adj;
		try {
			adj = new LightAdjacencyList(fstO, sndO, null, false);
		} catch (ClassificationTimeoutException e) {
			printDummyStats(out,mappingFile);
			return;
		}
		outLog.println(OntoUtil.getDLName(fstO) + " " + fstO.toString()); 
		outLog.println(OntoUtil.getDLName(sndO) + " " + sndO.toString());

		adj.loadMappings(new File(mappingFile), null, true);
		outLog.println(mappingFile);

		OWLOntology alignedOnto = 
				OntoUtil.getManager(false).getOntology(
						LightAdjacencyList.alignIRI);
		OWLReasoner alignReasoner = 
				OntoUtil.getReasoner(alignedOnto, Params.reasonerKind, 
						OntoUtil.getManager(false));
		outLog.println("ALIGNED_ONTO: " + OntoUtil.getDLName(alignedOnto));
		
		List<OWLReasoner> reasoners = Collections.singletonList(alignReasoner);
		LightSCCs problematicSCCs = adj.detectProblematicSCCs(reasoners, 
				alignedOnto);
		alignReasoner = reasoners.get(0);
		
		if(problematicSCCs == null){
			OntoUtil.unloadOntologies(alignedOnto);
			printDummyStats(out,mappingFile);
			return;
		}
		
		LightSCCs unsolvProblSCCs = 
				adj.detectUnsolvableProblematicSCCs(alignReasoner, 
						problematicSCCs);
		int unsolv = unsolvProblSCCs.size();

		OntoUtil.disposeReasoners(alignReasoner);
		
//		if(!problematicSCCs.isEmpty()){
//			outLog.println("\n" + problematicSCCs.size() 
//					+ " REASONER SCCs:");
//			for (LightSCC scc : problematicSCCs) {
//				outLog.println(scc.problematicSCCAsString(adj));
//				if(unsolvProblSCCs.contains(scc)){
//					outLog.println("Printing explanation(s)");
//					Set<AxiomExplanation> axExplanations = 
//							OntoUtil.computeExplanations(adj, scc, 
//									alignReasoner);
//					FileUtil.printExplanations(axExplanations, outLog);
//				}
//			}
//			outLog.print("\n");
//		}			

		Set<LightSCC> heurProblematicSCCs = new HashSet<LightSCC>();
		LightSCCs globalSCCs = 
				adj.computeGlobalSCCsAndProblematicMappings(heurProblematicSCCs, 
						null);

		int common = Util.computeIntersection(heurProblematicSCCs, 
				problematicSCCs).size();
				
		Test5.printStats(out, mappingFile, unsolv, problematicSCCs.size(), 
				heurProblematicSCCs.size(), common);

//		if(!heurProblematicSCCs.isEmpty()){
//			outLog.println("\n" + heurProblematicSCCs.size() 
//					+ " HEURISTIC SCCs:");
//			for (LightSCC lightSCC : heurProblematicSCCs) {
//				outLog.println(lightSCC.problematicSCCAsString(adj));
//			}
//			outLog.print("\n");
//		}
		
		if(common < heurProblematicSCCs.size())
		{
			LightSCCs pSCCs = new LightSCCs();
			pSCCs.addAll(problematicSCCs);
			pSCCs.removeAll(heurProblematicSCCs);
			System.out.println("SCC Heuristic not detected by Reasoner:");
			for (LightSCC heurSCC : pSCCs) {
				boolean skip = false;
				for (LightSCC reasSCC : problematicSCCs) {
					if(reasSCC.containsAll(heurSCC)){
						skip = true;
						break;
					}
				}
				if(!skip)
					System.out.println(heurSCC);
			}
		}

		LegacyFileUtil.writeFileAndConsole(outLog, common + " SCCs in common");
		
		int preProblems = problematicSCCs.size();
		// test if the diagnosis is logically effective
		Diagnosis hDiag = adj.computeDiagnosis(globalSCCs, 
				adj.getOriginalMappings(), heurProblematicSCCs, null, 
				Util.getMSec());
		OntoUtil.removeAxiomsFromOntology(alignedOnto, 
				OntoUtil.getManager(false), hDiag.toOWLAxioms(), true);
		problematicSCCs.clear();

		reasoners = Collections.singletonList(alignReasoner);
		alignReasoner = OntoUtil.getReasoner(alignedOnto, Params.reasonerKind, 
				OntoUtil.getManager(false));		
		problematicSCCs = adj.detectProblematicSCCs(reasoners, 
				alignedOnto);
		alignReasoner = reasoners.get(0);
		
		for (LightEdge e : hDiag) {
			OWLAxiom eAx = e.toOWLAxiom();
			if(alignedOnto.containsAxiom(eAx))
				System.out.println(e + " not removed!");
			if(alignReasoner.isEntailed(eAx)){
				if(!(
						alignReasoner.getUnsatisfiableClasses().contains(
								e.from.getOWLClass()) || 
						alignReasoner.getUnsatisfiableClasses().contains(
								e.to.getOWLClass()) ||
						alignReasoner.getTopClassNode().contains(
								e.from.getOWLClass()) ||
						alignReasoner.getTopClassNode().contains(
								e.to.getOWLClass())
				)){
//						!(alignReasoner.getUnsatisfiableClasses().contains(
//						e.from.getOWLClasss()) || 
//						alignReasoner.getTopClassNode().contains(
//								e.to.getOWLClasss()))
					//System.out.println("But " + e.from + " is empty!");
					System.out.println(e + " still entailed:");
					System.out.println(OntoUtil.computeExplanations(
							OntoUtil.getManager(false), eAx, alignReasoner));
					System.out.println(alignReasoner.getBottomClassNode());
					System.out.println(alignReasoner.getTopClassNode());
				}
			}
		}
		
		OntoUtil.disposeReasoners(alignReasoner);
		
		if(!problematicSCCs.isEmpty())
			LegacyFileUtil.writeFileAndConsole(outLog, 
					"Unsolved problems: " + problematicSCCs.size() + "/" + preProblems);

		//adj.unloadOntologies(fstO,sndO);
		OntoUtil.unloadOntologies(alignedOnto);

		out.flush();
		LegacyFileUtil.writeFileAndConsole(outLog, 
				"Full detection time: " + Util.getDiffmsec(totalStartTime));
	}
	
	private static void printDummyStats(PrintWriter out, String mappingFile){
		printStats(out, mappingFile, -1, -1, -1, -1);
		out.flush();
	}
	
	private static void printStats(PrintWriter out, String mappingFile, 
			int unsolv, int problSCCsSize, int heurProblSCCsSize, int common){
		out.println(mappingFile
				//mappingFile.subSequence(mappingFile.lastIndexOf('/'), mappingFile.length()) 
				+ " " + unsolv + " " + problSCCsSize + " " + heurProblSCCsSize 
				+ " " + common);
	}
}
