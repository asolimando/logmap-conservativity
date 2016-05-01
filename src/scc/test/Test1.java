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
import scc.graphDataStructure.Diagnosis;
import scc.graphDataStructure.LightAdjacencyList;
import scc.graphDataStructure.LightEdge;
import scc.graphDataStructure.LightSCC;
import scc.graphDataStructure.LightSCCs;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import scc.mapping.LightAlignment;
import scc.mapping.LightOAEIMappingHandler;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import scc.util.LegacyFileUtil;
import util.OntoUtil;
import util.Params;
import util.Util;

public class Test1 {

	// general statistics on the runtime (computation results and execution times)
	public static Map<String, Double> stats = new HashMap<>();
	// counter for the number of debugged alignment files
	private static int count = 0, sccCount = 0, resumeCount, sccTotal;
	private static LightOAEIMappingHandler oaeiParser = new LightOAEIMappingHandler();

	private static FileWriter outFile = null;
	private static PrintWriter out = null;

	private static int mbMinGC = 1024;
	private static int sleepGC = 250;

	private static int [] years = {2012,2013,2014};

	
	public static void main(String[] args) 
			throws OWLOntologyCreationException, IOException {

		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		String startTime = sdf.format(Calendar.getInstance().getTime());

		if(args.length == 0){
			Params.runAll = true;
			Params.test1Reference = false;
			Params.test4FilterAnalysis = false;
		}
		else if(args.length == 1){
			Params.runAll = true;
			Params.test1Reference = false;
			Params.test4FilterAnalysis = false;
			Params.verbosity = Integer.parseInt(args[0]);
		}
		else if(args.length == 2 || args.length == 3 || args.length == 4){
			Params.runAll = false;
			Params.verbosity = Integer.parseInt(args[1]);

			Params.test1Reference = (args.length >= 3 
					? (Integer.parseInt(args[2]) == 1) : false);
			Params.test4FilterAnalysis = (args.length >= 4 ? 
					(Integer.parseInt(args[3]) == 1) : false);

			switch(Integer.parseInt(args[0])){
			case 0: 
				Params.setParams(0);
				Params.trackNotAsmov = true;
				break;
			case 1:
				Params.setParams(1);
				Params.trackNotAsmov = true;
				break;
			case 2:
				Params.setParams(2);
				Params.trackNotAsmov = true;
				break;
			case 3:
				Params.setParams(3);
				Params.trackNotAsmov = true;
				break;
			case 4:
				Params.trackNotAsmov = false;
				break;
			case 5:
				Params.testLily = true;
				break;
			case 6:
				Params.runAll = true;
				break;
			case 7:
				Params.setParams(4);
				Params.trackNotAsmov = true;
				break;
			}
		}
		else {
			System.out.println("Usage: [[[[testParam] [verbosity]] [refAnalysis]] [filterAnalysis]]\n\n" +
					"no parameters at all for running all tests (no analyses) or\n" +
					"1) testParam:\n" +
					"0 = largebio BIG\n" +
					"1 = largebio SMALL\n" +
					"2 = anatomy\n" +
					"3 = conference\n" +
					"4 = ASMOV\n\n" +
					"5 = lily\n\n" +
					"6 = ALL\n\n" +
					"7 = library\n\n" +
					"2) verbosity level\n" +
					"3) reference analysis, 1 = on, 0 = off\n" + 
					"4) filter analysis, 1 = on, 0 = off"
					);
		}
		if(Params.test4FilterAnalysis) {
			LegacyFileUtil.createDirPath(Params.test4OutDir);
			LegacyFileUtil.createDirPath(Params.test4SerDir);
			if(Params.test4Resume){
				resumeCount = LegacyFileUtil.countLines(
						Params.test4OutDir + "test4.txt");
				System.out.println("Resuming from line " + resumeCount);
			}
		}
		else if(Params.test1Reference)
			LegacyFileUtil.deleteAnalysisTest1();
		else { 
			LegacyFileUtil.createDirPath(Params.test1OutDir);			
		}

		if(Params.runAll){
			Params.tryPellet = true;
			Params.inputOntoClassificationTimeout = 60;
			Params.inputOntoClassificationTimeout = 120;
			System.out.println("Testing Lily");
			lilyTest();
			OntoUtil.disposeAllReasoners();
			OntoUtil.unloadAllOntologies();
			System.out.println("Testing ASMOV");
			asmovTest();
			OntoUtil.disposeAllReasoners();
			OntoUtil.unloadAllOntologies();

			for (int year : years) {
				System.out.println("Testing year: " + year);
				for (String name : Params.trackNames) {
					System.out.println("Testing track: " + name);
					Params.trackName = name;
					if(Params.trackName.equals("largebio")){
						Params.setParams(0);
						trackTest(year);
						Params.setParams(1);
						trackTest(year);
						OntoUtil.disposeAllReasoners();
						OntoUtil.unloadAllOntologies();
					}
					else if(Params.trackName.equals("anatomy")){
						Params.setParams(2);
						trackTest(year);
						OntoUtil.disposeAllReasoners();
						OntoUtil.unloadAllOntologies();
					}
					else if(Params.trackName.equals("conference")){
						Params.setParams(3);
						trackTest(year);
						OntoUtil.disposeAllReasoners();
						OntoUtil.unloadAllOntologies();
					}
					else if(Params.trackName.equals("library")){
						Params.setParams(4);
						trackTest(year);
						OntoUtil.disposeAllReasoners();
						OntoUtil.unloadAllOntologies();
					}
					//					else
					//						trackTest(year);
					OntoUtil.disposeAllReasoners();
					OntoUtil.unloadAllOntologies();
				}
			}

		}
		else if(Params.testLily){
			lilyTest();
		}
		else {
			if(Params.trackNotAsmov){
				for (int i : years){
					System.out.println("Testing year: " + i);
					trackTest(i);
				}
			}
			else
				asmovTest();		
		}

		if(Params.test4FilterAnalysis){
			if(outFile != null)
				outFile.close();
			if(out != null)
				out.close();

			LegacyFileUtil.deleteAllFiles(Params.test4SerDir);
		}

		System.out.println("Start: " + startTime);
		System.out.println("End: " + new SimpleDateFormat("HH:mm:ss").format(
				Calendar.getInstance().getTime()));
	}

	private static void lilyTest() 
			throws OWLOntologyCreationException, IOException{
		Map<String, Map<String,Double>> generalStats = new HashMap<>();
		lilyTestAnatomy(generalStats);
		lilyTestConference(generalStats);

		if(!Params.test4FilterAnalysis)
			LegacyFileUtil.printStatsTest1ToFile(Params.test1OutDir + "lily.txt", 
					generalStats);
	}

	private static void lilyTestAnatomy(Map<String, Map<String,Double>> 
	generalStats) throws OWLOntologyCreationException, IOException {

		String mappingDir = Params.dataFolder + "lily/oaei2007/anatomy";

		String fstOnto = mappingDir + "/mouse.owl";
		String sndOnto = mappingDir + "/human.owl";

		OWLOntology fstO = null;
		OWLOntology sndO = null;

		double ontoLoadTime1 = Util.getMSec(), ontoLoadTime2;
		fstO = OntoUtil.load(fstOnto, true, OntoUtil.getManager(false));
		ontoLoadTime1 = Util.getDiffmsec(ontoLoadTime1);
		ontoLoadTime2 = Util.getMSec();
		sndO = OntoUtil.load(sndOnto, true, OntoUtil.getManager(false));
		ontoLoadTime2 = Util.getDiffmsec(ontoLoadTime2);

		String [] myArgs = new String[4];

		LightAlignment original = null, diagnosis = null, multiDiagnosis = null, 
				multiDiagnosisRefAnatomy = null, diagnosisRefAnatomy = null, 
				referenceAnatomy = null;

		for (int i = 1; i <= 3; i++) {
			myArgs[0] = mappingDir + "/lily" + i + ".rdf";

			if(Params.test4FilterAnalysis){
				testFilter(fstO,sndO,false,myArgs[0]);
			}
			else {

				if(new File(myArgs[0]).exists()){
					Util.getUsedMemoryAndClean(mbMinGC,sleepGC);
					diagnosis = test(myArgs,fstO,sndO,false,true,false).toAlignment();
					if(diagnosis == null)
						continue;

					Util.getUsedMemoryAndClean(mbMinGC,sleepGC);					
					multiDiagnosis = test(myArgs,fstO,sndO,false,
							true,true).toAlignment();
					if(multiDiagnosis == null)
						continue;

					stats.put("ontoLoadTime1", ontoLoadTime1);
					stats.put("ontoLoadTime2", ontoLoadTime2);
					generalStats.put("lily anatomy " 
							+ myArgs[0].substring(myArgs[0].lastIndexOf('/')+1)
							+ " 2007", stats);
					System.out.println(++count);
					stats = new HashMap<>();
					Util.getUsedMemoryAndClean(mbMinGC,sleepGC);
				}

				if(Params.test1Reference){				
					if(diagnosisRefAnatomy == null){
						diagnosisRefAnatomy = test(new String[] {Params.dataFolder 
								+ "asmov/" + 
						"ASMOV_2010/anatomy/mouse_anatomy_reference_2010.rdf"},
						fstO,sndO,false,false,false).toAlignment();
						if(diagnosisRefAnatomy == null)
							continue;
					}
					if(multiDiagnosisRefAnatomy == null){
						multiDiagnosisRefAnatomy = test(new String[] {Params.dataFolder + 
						"asmov/ASMOV_2010/anatomy/mouse_anatomy_reference_2010.rdf"},
						fstO,sndO,false,false,true).toAlignment();
						if(multiDiagnosisRefAnatomy == null)
							continue;
					}
					if(referenceAnatomy == null){
						referenceAnatomy = oaeiParser.parseAlignAPI(new File(
								Params.dataFolder 
								+ "asmov/ASMOV_2010/anatomy/mouse_anatomy_reference_2010.rdf"),false);
						if(referenceAnatomy == null)
							continue;
					}
					original = oaeiParser.parseAlignAPI(new File(myArgs[0]),false);
					LightOAEIMappingHandler.PRAnalysis(original, diagnosis, 
							referenceAnatomy, diagnosisRefAnatomy, 
							multiDiagnosis, multiDiagnosisRefAnatomy,
							"lily", "2007", "anatomy",myArgs[0]);
				}
			}
		}
	}

	private static void lilyTestConference(Map<String, Map<String,Double>> 
	generalStats) throws OWLOntologyCreationException, IOException {

		String mappingDir = Params.dataFolder + "lily/oaei2007/conference";
		File directory = new File(mappingDir);
		String [] myArgs = new String[4];

		OWLOntology fstO = null;
		OWLOntology sndO = null;

		LightAlignment reference = null, original = null, diagnosis = null, 
				multiDiagnosis = null, multiDiagnosisRef = null, 
				diagnosisRef = null;

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
				double ontoLoadTime1 = Util.getMSec(), ontoLoadTime2;
				fstO = OntoUtil.load(elem.getParentFile() + "/" + ontos[0] 
						+ ".owl", true, OntoUtil.getManager(false));
				ontoLoadTime1 = Util.getDiffmsec(ontoLoadTime1);
				ontoLoadTime2 = Util.getMSec();
				if(!ontos[0].equals(ontos[1])){
					sndO = OntoUtil.load(elem.getParentFile() + "/" + ontos[1] 
							+ ".owl", true, OntoUtil.getManager(false));
					ontoLoadTime2 = Util.getDiffmsec(ontoLoadTime2);
				}
				else{
					sndO = fstO;
					ontoLoadTime2 = ontoLoadTime1;
				}
				myArgs[0] = elem.getAbsolutePath();

				if(Params.test4FilterAnalysis){
					testFilter(fstO,sndO,false,myArgs[0]);
				}
				else {
					Util.getUsedMemoryAndClean(mbMinGC,sleepGC);
					diagnosis = test(myArgs,fstO,sndO,
							false,true,false).toAlignment();
					if(diagnosis == null)
						continue;

					Util.getUsedMemoryAndClean(mbMinGC,sleepGC);
					multiDiagnosis = test(myArgs,fstO,sndO,
							!Params.test1Reference, true,true).toAlignment();
					if(multiDiagnosis == null)
						continue;

					stats.put("ontoLoadTime1", ontoLoadTime1);
					stats.put("ontoLoadTime2", ontoLoadTime2);
					generalStats.put("lily conference " + elem.getName()
							+ " 2007", stats);
					System.out.println(++count);
					stats = new HashMap<>();
					Util.getUsedMemoryAndClean(mbMinGC,sleepGC);

					if(Params.test1Reference){
						String refStr = Params.dataFolder + "oaei2012/conference/reference/" 
								+ ontos[0] + "-" + ontos[1] + ".rdf" ;
						File referenceFile = new File(refStr.toLowerCase());
						if(referenceFile.exists()){
							reference = new LightAlignment(
									oaeiParser.parseAlignAPI(referenceFile,
											false)
									);

							diagnosisRef = test(new String[] {
									referenceFile.getAbsolutePath()},
									fstO,sndO,false,false,false).toAlignment();

							if(diagnosisRef == null)
								continue;

							multiDiagnosisRef = test(new String[] {
									referenceFile.getAbsolutePath()},
									fstO,sndO,true,false,true).toAlignment();

							if(multiDiagnosisRef == null)
								continue;

							original = oaeiParser.parseAlignAPI(
									new File(myArgs[0]),false);

							LightOAEIMappingHandler.PRAnalysis(original, 
									diagnosis, reference, diagnosisRef, 
									multiDiagnosis, multiDiagnosisRef, "lily", 
									"2007", "conference",
									referenceFile.getAbsolutePath());
						}
						else {
							System.out.println("Missing reference file: " 
									+ referenceFile.getPath() + "\n");
						}
					}
				}
			}
		}
	}

	private static void asmovTest() throws OWLOntologyCreationException, IOException{

		String mappingDir = Params.dataFolder + "asmov/";
		String [] years = {"2008","2009","2010"};
		Map<String, Map<String,Double>> generalStats = new HashMap<>();

		LightAlignment reference = null, original = null, diagnosis = null, 
				multiDiagnosis = null, multiDiagnosisRef = null, 
				multiDiagnosisRefAnatomy = null,
				diagnosisRef = null, diagnosisRefAnatomy = null, 
				referenceAnatomy = null;

		for (String year : years) {
			File directory = new File(mappingDir + "ASMOV_" + year + "/");

			String fstOnto = mappingDir + "ASMOV_" + year + "/anatomy/mouse.owl";
			String sndOnto = mappingDir + "ASMOV_" + year + "/anatomy/human.owl";

			OWLOntology fstO = null;
			OWLOntology sndO = null;
			double ontoLoadTime1 = Util.getMSec(), ontoLoadTime2;
			fstO = OntoUtil.load(fstOnto, true, OntoUtil.getManager(false));
			ontoLoadTime1 = Util.getDiffmsec(ontoLoadTime1);
			ontoLoadTime2 = Util.getMSec();
			sndO = OntoUtil.load(sndOnto, true, OntoUtil.getManager(false));
			ontoLoadTime2 = Util.getDiffmsec(ontoLoadTime2);

			String [] myArgs = new String[4];

			for (int i = 1; i <= 4; i++) {
				myArgs[0] = directory.getAbsolutePath() 
						+ "/anatomy/ASMOV" + i + ".rdf";

				if(Params.test4FilterAnalysis){
					testFilter(fstO,sndO,false,myArgs[0]);
				}
				else {
					if(new File(myArgs[0]).exists()){
						Util.getUsedMemoryAndClean(mbMinGC,sleepGC);
						diagnosis = test(myArgs,fstO,sndO,false,true,false).toAlignment();
						if(diagnosis == null)
							continue;
						Util.getUsedMemoryAndClean(mbMinGC,sleepGC);
						multiDiagnosis = test(myArgs,fstO,sndO,false,
								true,true).toAlignment();
						if(multiDiagnosis == null)
							continue;
						stats.put("ontoLoadTime1", ontoLoadTime1);
						stats.put("ontoLoadTime2", ontoLoadTime2);
						generalStats.put("asmov anatomy " 
								+ myArgs[0].substring(myArgs[0].lastIndexOf('/')+1)
								+ " " + year, stats);
						System.out.println(++count);
						stats = new HashMap<>();
						Util.getUsedMemoryAndClean(mbMinGC,sleepGC);
					}

					if(Params.test1Reference){				
						if(diagnosisRefAnatomy == null){
							diagnosisRefAnatomy = test(new String[] {mappingDir + 
							"ASMOV_2010/anatomy/mouse_anatomy_reference_2010.rdf"},
							fstO,sndO,false,false,false).toAlignment();
							if(diagnosisRefAnatomy == null)
								continue;
						}
						if(multiDiagnosisRefAnatomy == null){
							multiDiagnosisRefAnatomy = test(new String[] {mappingDir 
									+ "ASMOV_2010/anatomy/mouse_anatomy_reference_2010.rdf"},
									fstO,sndO,false,false,true).toAlignment();
							if(multiDiagnosisRefAnatomy == null)
								continue;
						}
						if(referenceAnatomy == null){
							referenceAnatomy = oaeiParser.parseAlignAPI(new File(
									mappingDir + "ASMOV_2010/anatomy/mouse_anatomy_reference_2010.rdf"),false);
							if(referenceAnatomy == null)
								continue;
						}

						original = oaeiParser.parseAlignAPI(new File(myArgs[0]),false);
						LightOAEIMappingHandler.PRAnalysis(original, diagnosis, 
								referenceAnatomy, diagnosisRefAnatomy, 
								multiDiagnosis, multiDiagnosisRefAnatomy,
								"ASMOV", year, "anatomy",myArgs[0]);
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

					ontoLoadTime1 = Util.getMSec();
					fstO = OntoUtil.load(elem.getParentFile() + "/" + ontos[0] 
							+ ".owl", true, OntoUtil.getManager(false));
					ontoLoadTime1 = Util.getDiffmsec(ontoLoadTime1);
					ontoLoadTime2 = Util.getMSec();
					sndO = OntoUtil.load(elem.getParentFile() + "/" + ontos[1] 
							+ ".owl", true, OntoUtil.getManager(false));
					ontoLoadTime2 = Util.getDiffmsec(ontoLoadTime2);
					myArgs[0] = elem.getAbsolutePath();

					if(Params.test4FilterAnalysis){
						testFilter(fstO,sndO,false,myArgs[0]);
					}
					else {
						Util.getUsedMemoryAndClean(mbMinGC,sleepGC);
						diagnosis = test(myArgs,fstO,sndO,
								false,true,false).toAlignment();
						if(diagnosis == null)
							continue;
						Util.getUsedMemoryAndClean(mbMinGC,sleepGC);
						multiDiagnosis = test(myArgs,fstO,sndO,
								!Params.test1Reference, true,true).toAlignment();
						if(multiDiagnosis == null)
							continue;

						stats.put("ontoLoadTime1", ontoLoadTime1);
						stats.put("ontoLoadTime2", ontoLoadTime2);
						generalStats.put("asmov conference " + elem.getName()
								+ " " + year, stats);
						System.out.println(++count);
						stats = new HashMap<>();
						Util.getUsedMemoryAndClean(mbMinGC,sleepGC);

						if(Params.test1Reference){
							File referenceFile = new File(
									Params.dataFolder + "oaei2012/conference/reference/" 
											+ ontos[0] + "-" + ontos[1] + ".rdf");
							if(referenceFile.exists()){
								reference = new LightAlignment(
										oaeiParser.parseAlignAPI(referenceFile,
												false)
										);

								diagnosisRef = test(new String[] {
										referenceFile.getAbsolutePath()},
										fstO,sndO,false,false,false).toAlignment();
								if(diagnosisRef == null)
									continue;

								multiDiagnosisRef = test(new String[] {
										referenceFile.getAbsolutePath()},
										fstO,sndO,true,false,true).toAlignment();
								if(multiDiagnosisRef == null)
									continue;

								original = oaeiParser.parseAlignAPI(
										new File(myArgs[0]),false);

								LightOAEIMappingHandler.PRAnalysis(original, 
										diagnosis, reference, diagnosisRef, 
										multiDiagnosis, multiDiagnosisRef, "ASMOV", 
										year, "conference",
										referenceFile.getAbsolutePath());
							}
							else {
								System.out.println("Missing reference file: " 
										+ referenceFile.getPath() + "\n");
							}
						}
					}
				}
			}
		}
		if(!Params.test4FilterAnalysis)
			LegacyFileUtil.printStatsTest1ToFile(Params.test1OutDir + "ASMOV.txt", 
					generalStats);
	}

	private static void trackTest(int year) 
			throws OWLOntologyCreationException, IOException{
		String mappingDir = Params.dataFolder + "oaei"+year+"/" + Params.trackName + "/";
		String [] myArgs = new String[4];

		Map<String,String> lbioref = Params.largebioRef;
		Map<String, String> lbioOnto = Params.largebioOntologies;
		if(year == 2013){
			lbioref = Params.largebioRef13;
			lbioOnto = Params.largebioOntologies13;
		}
		else if(year == 2014){
			lbioref = Params.largebioRef14;
			lbioOnto = Params.largebioOntologies14;
		}
		
//		String [] bioRefSuffixes = {"logmap","alcomo","voted","voted2",
//				"voted3","voted4","voted5","original"};

		LightAlignment referenceAnatomy = null, diagnosisRefAnatomy = null, 
				reference = null, original = null,  diagnosis = null, 
				diagnosisRef = null, multiDiagnosis = null, referenceLibrary = null,
				multiDiagnosisRef = null, multiDiagnosisRefAnatomy = null, 
				diagnosisRefLibrary = null, multiDiagnosisRefLibrary = null;

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

		if(Params.trackName.equalsIgnoreCase("anatomy")){
			fstOnto = mappingDir + "onto/mouse.owl";
			sndOnto = mappingDir + "onto/human.owl";
		}
		else if(Params.trackName.equalsIgnoreCase("library")){
			fstOnto = mappingDir + "onto/stw.owl";
			sndOnto = mappingDir + "onto/thesoz.owl";
		}

		OWLOntology fstO = null, sndO = null;
		double ontoLoadTimeAn1 = -1, ontoLoadTimeAn2 = -1, 
				ontoLoadTimeLib1 = -1, ontoLoadTimeLib2 = -1, 
				ontoLoadTimeConf1 = -1, ontoLoadTimeConf2 = -1;

		if(fstOnto != null && sndOnto != null){
			OntoUtil.getManager(true);

			if(Params.trackName.equalsIgnoreCase("anatomy")){
				ontoLoadTimeAn1 = Util.getMSec();
				fstO = OntoUtil.load(fstOnto, true, OntoUtil.getManager(false));
				ontoLoadTimeAn1 = Util.getDiffmsec(ontoLoadTimeAn1);
				ontoLoadTimeAn2 = Util.getMSec();
				sndO = OntoUtil.load(sndOnto, true, OntoUtil.getManager(false));
				ontoLoadTimeAn2 = Util.getDiffmsec(ontoLoadTimeAn2);

				if(!Params.test4FilterAnalysis && Params.test1Reference){
					diagnosisRefAnatomy = test(new String[] {mappingDir 
							+ "reference/mouse-human.rdf"}, fstO,sndO,false,false,
							false).toAlignment();
					if(diagnosisRefAnatomy != null)
						multiDiagnosisRefAnatomy = test(new String[] {mappingDir 
								+ "reference/mouse-human.rdf"}, fstO,sndO,false,false,
								true).toAlignment();

					if(multiDiagnosisRefAnatomy != null)
						referenceAnatomy = oaeiParser.parseAlignAPI(
								new File(mappingDir + "reference/mouse-human.rdf"),
								false);
				}
			}

			else if(Params.trackName.equalsIgnoreCase("library")){
				ontoLoadTimeLib1 = Util.getMSec();
				fstO = OntoUtil.load(fstOnto, true, OntoUtil.getManager(false));
				ontoLoadTimeLib1 = Util.getDiffmsec(ontoLoadTimeLib1);
				ontoLoadTimeLib2 = Util.getMSec();
				sndO = OntoUtil.load(sndOnto, true, OntoUtil.getManager(false));
				ontoLoadTimeLib2 = Util.getDiffmsec(ontoLoadTimeLib2);

				if(!Params.test4FilterAnalysis && Params.test1Reference){
					diagnosisRefLibrary = test(new String[] {mappingDir 
							+ "reference/stw-thesoz.rdf"}, fstO,sndO,false,false,
							false).toAlignment();
					if(diagnosisRefLibrary != null)
						multiDiagnosisRefLibrary = test(new String[] {mappingDir 
								+ "reference/stw-thesoz.rdf"}, fstO,sndO,false,false,
								true).toAlignment();

					if(multiDiagnosisRefLibrary != null)
						referenceLibrary = oaeiParser.parseAlignAPI(
								new File(mappingDir + "reference/stw-thesoz.rdf"),
								false);
				}
			}
		}

		Map<String, Map<String,Double>> generalStats = new HashMap<>();

		for (File elem : files){
			if(elem.getName().endsWith("GRAPHDBG.rdf"))
				continue;

			boolean lbio = Params.trackName.equalsIgnoreCase("largebio");

			if(lbio && (elem.isDirectory() || year > 2012)){
				File[] matchersDir = {elem};

				if(year == 2012){
					matchersDir = elem.listFiles(new FilenameFilter() {
						public boolean accept(File dir, String name) {
							return !name.toLowerCase().startsWith(".") 
									&& !name.toLowerCase().endsWith("GRAPHDBG.rdf");
						}
					}	
							);						
				}

				for (File dir : matchersDir) {
					if(dir.isFile()){
						String [] tokens = dir.getName().split("_");
						if(!(tokens.length > 3) 
								&& tokens[1].equals(Params.ontoSize)){
							String [] ontoNames = tokens[2].split("2");
							ontoNames[1] = ontoNames[1].replace(".rdf","");

							System.out.println(Arrays.toString(tokens));// + "\n" + Arrays.toString(ontoNames));

							//boolean skip = true;

							//								if(!tokens[0].equals("gomma") || !tokens[1].equals("big") || 
							//										!tokens[2].equals("Hertuda-anatomy-track1.rdf"))
							//									continue;

							// NAME FILTER: 
							//								if(!tokens[0].equals("gommaBK") || !tokens[1].equals("big") 
							//										|| !tokens[2].equals("snomed2nci.rdf"))
							//									continue;
							//								 
							//								if(!tokens[0].equals("servomap") || !tokens[1].equals("small") 
							//										|| !tokens[2].equals("snomed2nci.rdf"))
							//									continue;

							//								 NAME SELECTOR
							//								if(!(tokens[0].equals("hertuda") && tokens[1].equals("small"))) 
							//										//&& tokens[2].equals("fma2snomed.rdf"))
							//									continue;								
							//								if(!(tokens[0].equals("wmatch") && tokens[1].equals("small") 
							//										&& tokens[2].equals("fma2nci.rdf")))
							//									continue;

							//								 NAME SELECTOR
							//								if((tokens[0].equals("hertuda") && tokens[1].equals("small"))) 
							//									skip = false;
							//								
							//								if(skip && (tokens[0].equals("wmatch") && tokens[1].equals("small") 
							//										&& tokens[2].equals("fma2nci.rdf")))
							//									skip = false;
							//								
							//								if(skip)
							//									continue;

							fstOnto = elem.getParentFile().getParent() 
									+ "/onto/" 
									+ lbioOnto.get(
											tokens[1] + ontoNames[0] 
													+ ontoNames[1]);
							sndOnto = elem.getParentFile().getParent() 
									+ "/onto/" 
									+ lbioOnto.get(
											tokens[1] + ontoNames[1] 
													+ ontoNames[0]);

							double ontoLoadTime1, ontoLoadTime2;

							OntoUtil.getManager(true);
							ontoLoadTime1 = Util.getMSec();
							fstO = OntoUtil.load(fstOnto, true, 
									OntoUtil.getManager(false));
							ontoLoadTime1 = Util.getDiffmsec(ontoLoadTime1);
							ontoLoadTime2 = Util.getMSec();
							sndO = OntoUtil.load(sndOnto, true, 
									OntoUtil.getManager(false));
							ontoLoadTime2 = Util.getDiffmsec(ontoLoadTime2);

							myArgs[0] = dir.getAbsolutePath();
							myArgs[1] = dir.getAbsolutePath().replace(
									".rdf", "_GRAPHDBG.rdf");
							myArgs[2] = dir.getAbsolutePath().replace(".rdf", 
									"_repaired_with_Alcomo_Hermit.rdf");
							myArgs[3] = dir.getAbsolutePath().replace(
									".rdf", "_repaired_with_LogMap.rdf");

							if(Params.test4FilterAnalysis){
								testFilter(fstO,sndO,false,myArgs[0]);
							}
							else {
								if(tokens[0].equals("logmap2noe"))
									myArgs[2] = myArgs[3] = null;

								if(new File(myArgs[0]).exists()){
									Util.getUsedMemoryAndClean(mbMinGC,sleepGC);
									diagnosis = test(myArgs,fstO,sndO,
											false, 
											true,false).toAlignment();
									if(diagnosis == null)
										continue;
									Util.getUsedMemoryAndClean(mbMinGC,sleepGC);
									multiDiagnosis = test(myArgs,fstO,sndO,
											!Params.test1Reference, 
											true,true).toAlignment();
									if(multiDiagnosis == null)
										continue;
									stats.put("ontoLoadTime1", ontoLoadTime1);
									stats.put("ontoLoadTime2", ontoLoadTime2);
									generalStats.put(tokens[0].toLowerCase() 
											+ " largebio_" + tokens[1] + " " 
											+ myArgs[0].substring(
													myArgs[0].lastIndexOf('/')+1) 
													+ " " + year, stats);
									System.out.println(++count);
									stats = new HashMap<>();
									Util.getUsedMemoryAndClean(mbMinGC,sleepGC);
								}

								if(Params.test1Reference){
									original = oaeiParser.parseAlignAPI(
											new File(myArgs[0]),false);

									for (String s : lbioref.keySet()) {
										if(!s.startsWith(ontoNames[0].toLowerCase()
												+ontoNames[1].toLowerCase()))
											continue;
										String refFilename = lbioref.get(s);

										System.out.println(
												"Precision/Recall for " + s);
										reference = oaeiParser.parseAlignAPI(
												new File(mappingDir 
														+ "reference/" 
														+ refFilename),
																false);

										diagnosisRef = test(new String[] {
												mappingDir + "reference/" 
														+ refFilename},
														fstO,sndO,false,//"original".equals(s), 
														false,false).toAlignment();
										if(diagnosisRef == null)
											continue;

										multiDiagnosisRef = test(new String[] {
												mappingDir + "reference/" 
														+ refFilename},
															fstO,sndO,
															true,//"original".equals(s), 
															false,true).toAlignment();
										if(multiDiagnosisRef == null)
											continue;

										LightOAEIMappingHandler.PRAnalysis(
												original, diagnosis, 
												reference, diagnosisRef, 
												multiDiagnosis, 
												multiDiagnosisRef, 
												tokens[0], year + "", 
												"largebio_" + tokens[1],
												refFilename
												);
									}
								}
							}
						}
					}
				}
			}

			else if(elem.isFile()){
				// we do not reload the pair of onto each time, it is fixed
				boolean unloadOnto = false;
				File referenceFile = null;
				String matcherName = null;

				if(Params.trackName.equalsIgnoreCase("anatomy")){
					myArgs[0] = mappingDir + "alignments/" + elem.getName();
					myArgs[1] = mappingDir + "alignments/" 
							+ elem.getName().subSequence(0, 
									elem.getName().lastIndexOf(".")) 
									+ "_GRAPHDBG.rdf";

					matcherName = elem.getName().substring(0,
							year == 2012 ? elem.getName().indexOf('-') : 
								elem.getName().indexOf('.'));
				}

				else if(Params.trackName.equalsIgnoreCase("conference")){
					unloadOnto = true;
					myArgs = new String[4];
					OntoUtil.getManager(true);

					String [] ontos = elem.getName().substring(0, 
							elem.getName().length()-4).split("-");

					ontoLoadTimeConf1 = Util.getMSec();					
					fstO = OntoUtil.load(elem.getParentFile().getParentFile() 
							+ "/onto/" + ontos[1] + ".owl", true, 
							OntoUtil.getManager(false));
					ontoLoadTimeConf1 = Util.getDiffmsec(ontoLoadTimeConf1);
					ontoLoadTimeConf2 = Util.getMSec();
					sndO = OntoUtil.load(elem.getParentFile().getParentFile() 
							+ "/onto/" + ontos[2] + ".owl", true, 
							OntoUtil.getManager(false));
					ontoLoadTimeConf2 = Util.getDiffmsec(ontoLoadTimeConf2);

					myArgs[0] = elem.getAbsolutePath();

					matcherName = ontos[0];

					referenceFile = new File(mappingDir + "reference/" 
							+ ontos[1] + "-" + ontos[2] + ".rdf");
				}
				else if(Params.trackName.equalsIgnoreCase("library")){
					unloadOnto = false;
					myArgs = new String[4];
					OntoUtil.getManager(true);

					String [] ontos = {elem.getName().substring(0, 
							elem.getName().length()-4),"stw","thesoz"};

					ontoLoadTimeLib1 = Util.getMSec();					
					fstO = OntoUtil.load(elem.getParentFile().getParentFile() 
							+ "/onto/" + ontos[1] + ".owl", true, 
							OntoUtil.getManager(false));
					ontoLoadTimeLib1 = Util.getDiffmsec(ontoLoadTimeLib1);
					ontoLoadTimeLib2 = Util.getMSec();
					sndO = OntoUtil.load(elem.getParentFile().getParentFile() 
							+ "/onto/" + ontos[2] + ".owl", true, 
							OntoUtil.getManager(false));
					ontoLoadTimeLib2 = Util.getDiffmsec(ontoLoadTimeLib2);

					myArgs[0] = elem.getAbsolutePath();

					matcherName = ontos[0];

					referenceFile = new File(mappingDir + "reference/" 
							+ ontos[1] + "-" + ontos[2] + ".rdf");
				}

				if(Params.test4FilterAnalysis){
					testFilter(fstO,sndO,false,myArgs[0]);
				}
				else {
					if(new File(myArgs[0]).exists()){
						Util.getUsedMemoryAndClean(mbMinGC,sleepGC);
						diagnosis = test(myArgs,fstO,sndO,false,//Params.test1Reference ? false : unloadOnto, 
								true, false).toAlignment();
						if(diagnosis == null)
							continue;

						Util.getUsedMemoryAndClean(mbMinGC,sleepGC);
						multiDiagnosis = test(myArgs,fstO,sndO,Params.test1Reference 
								? false : unloadOnto, true, true).toAlignment();
						if(multiDiagnosis == null)
							continue;

						if(Params.trackName.equalsIgnoreCase("conference")){
							stats.put("ontoLoadTime1", ontoLoadTimeConf1);
							stats.put("ontoLoadTime2", ontoLoadTimeConf2);
						}
						else if(Params.trackName.equalsIgnoreCase("anatomy")){
							stats.put("ontoLoadTime1", ontoLoadTimeAn1);
							stats.put("ontoLoadTime2", ontoLoadTimeAn2);
						}
						else if(Params.trackName.equalsIgnoreCase("library")){
							stats.put("ontoLoadTime1", ontoLoadTimeLib1);
							stats.put("ontoLoadTime2", ontoLoadTimeLib2);
						}
						generalStats.put(matcherName.toLowerCase() 
								+ " " + Params.trackName + " " +
								myArgs[0].substring(
										myArgs[0].lastIndexOf('/')+1) + " " + year, stats);
						System.out.println(++count);
						stats = new HashMap<>();
						Util.getUsedMemoryAndClean(mbMinGC,sleepGC);
					}

					if(Params.test1Reference){
						original = oaeiParser.parseAlignAPI(new File(myArgs[0]),false);

						if(Params.trackName.equalsIgnoreCase("anatomy")){
							LightOAEIMappingHandler.PRAnalysis(original, 
									diagnosis, referenceAnatomy, diagnosisRefAnatomy, 
									multiDiagnosis, multiDiagnosisRefAnatomy, 
									matcherName, year+"", "anatomy", 
									"mouse-human.rdf");
						}
						else if(Params.trackName.equalsIgnoreCase("conference")){					
							reference = new LightAlignment(oaeiParser.parseAlignAPI(
									referenceFile,false));

							diagnosisRef = test(new String[] {
									referenceFile.getAbsolutePath()},
									fstO,sndO,false, false, false).toAlignment();
							if(diagnosisRef == null)
								continue;

							multiDiagnosisRef = test(new String[] {
									referenceFile.getAbsolutePath()},
									fstO,sndO,unloadOnto, false, true).toAlignment();
							if(multiDiagnosisRef == null)
								continue;

							LightOAEIMappingHandler.PRAnalysis(original, 
									diagnosis, reference, diagnosisRef, 
									multiDiagnosis, multiDiagnosisRef,  
									matcherName, year + "", "conference", 
									referenceFile.getAbsolutePath());
						}
						else if(Params.trackName.equalsIgnoreCase("library")){					
							LightOAEIMappingHandler.PRAnalysis(original, 
									diagnosis, referenceLibrary, diagnosisRefLibrary, 
									multiDiagnosis, multiDiagnosisRefLibrary, 
									matcherName, year+"", "library", 
									"stw-thesoz.rdf");
						}
					}
				}
			}
		}
		if(!Params.test4FilterAnalysis)
			LegacyFileUtil.printStatsTest1ToFile(Params.test1OutDir + Params.trackName 
					+ ( Params.trackName.equals("largebio") ? "_" 
							+ Params.ontoSize : "" ) + "_" + year + ".txt", generalStats);
	}

	public static void testFilter(OWLOntology fstO, OWLOntology sndO, 
			boolean unloadOnto, String alignPath) throws IOException {

		if(!new File(alignPath).exists())
			return;

		if(outFile == null && out == null){
			outFile = new FileWriter(Params.test4OutDir + "test4.txt", 
					Params.test4Resume);
			out = new PrintWriter(outFile);
		}

		LightAdjacencyList adj;
		try {
			adj = new LightAdjacencyList(fstO, sndO, null, unloadOnto);
		} catch (ClassificationTimeoutException e) {
			System.err.println(e.getMessage());
			return;
		}
		adj.loadMappings(new File(alignPath), null, Params.fullDetection);

		Set<LightSCC> problematicSCCs = new HashSet<>();
		adj.computeGlobalSCCsAndProblematicMappings(problematicSCCs, null);

		//		Diagnosis dASP = null, dHeur = null;
		double [] timeASPs = new double[Params.test4Repetitions];

		double timeASP, wASP, cASP, timeHeur, wHeur, cHeur;

		//		Diagnosis [] dASPs = new Diagnosis[Params.test2Repetitions]; 
		//		Diagnosis [] dHeurs = new Diagnosis[Params.test2Repetitions]; 
		double [] res;

		boolean adjSerialized = false;

		for (LightSCC scc : problematicSCCs) {
			timeASP = 0; 
			wASP = 0; 
			cASP = 0;
			timeHeur = 0; 
			wHeur = 0; 
			cHeur = 0;

			++sccTotal;
			if(sccTotal % 1000 == 0)
				System.out.println(sccTotal + " SCCs");

			LightAlignment a = new Diagnosis(
					scc.extractMappings(adj, false)).toAlignment();
			if(a.getOneOneMappingNumber() == a.nbCells()){
				System.out.println("No multiple, skip this SCC");
				continue;
			}

			if(a.nbCells() < 16 || a.nbCells() > 140)
				continue;

			++sccCount;
			if(Params.test4Resume && sccCount <= resumeCount)
				continue;

			if(Params.test4Resume)
				System.out.println("First resumed SCC: " + sccCount);

			if(!adjSerialized){
				LegacyFileUtil.serializeObject(Params.test4SerDir + "adj.ser", adj);
				adjSerialized = true;
			}
			LegacyFileUtil.serializeObject(Params.test4SerDir + "scc.ser", scc);

			Params.ASPTimeout = 60;
			res = LegacyFileUtil.parseOutput(
					ExternalMain.asp(true, adj, scc, 
							new Double(Util.getMSec()).longValue(),
							true,Params.test4Repetitions)); 

			timeASP = res[0];
			wASP = res[1];
			cASP = res[2];

			if(res[0] == -1 || res[1] == -1 || res[2] == -1)
				continue;

			double actualTimeHeur;

			// calling external jar
			actualTimeHeur = Util.getMSec();
			res = LegacyFileUtil.launchJar(LegacyFileUtil.getJarString(0, 
					new Double(actualTimeHeur).longValue(), 60,
					Params.test4Repetitions));

			timeHeur = res[0];
			wHeur = res[1];
			cHeur = res[2];

			int [] dim = scc.dimensions(adj);
			Set<LightEdge> m = scc.extractMappings(adj, false);
			double totW = new Diagnosis(m).getWeight();

			String line = dim[0] + " " + dim[1] + " " + dim[2] + " " + dim[3] 
					+ " " + dim[4] + " " + totW + " " + cASP + " " + cHeur + " " 
					+ wASP + " " + wHeur + " " + timeASP + " " + timeHeur + "\n";

			if(dim[2] < cASP || dim[2] < cHeur || totW < wASP || totW < wHeur)
				System.out.print("INVALID: " + line);

			out.print(LegacyFileUtil.stringInfoTest4Bis(adj,scc,cASP,wASP,timeASP,
					cHeur,wHeur,timeHeur));

			out.flush();
		}
		LegacyFileUtil.deleteAllFiles(Params.test4SerDir);
	}

	public static Diagnosis test(String[] args, OWLOntology fstO, 
			OWLOntology sndO, boolean unloadOnto, boolean computeStats, 
			boolean multipleFilter) 
					throws OWLOntologyCreationException, IOException {		

		System.out.println("START: " + Util.getCurrTime());
		System.out.println("File: " + args[0]);
		
		double totalStartTime = Util.getMSec();
		if(multipleFilter)
			computeStats = false;

		LightAdjacencyList adj;
		try {
			adj = new LightAdjacencyList(
					fstO, sndO, computeStats ? stats : null, unloadOnto
					);
		} catch (ClassificationTimeoutException e) {
			System.err.println(e.getMessage());
			return null;
		}
		Set<LightEdge> mappings = 
				adj.loadMappings(new File(args[0]), computeStats 
						? stats : null, Params.fullDetection);

		Set<LightSCC> problematicSCCs = new HashSet<>();

		Diagnosis d = null;
		boolean oldVal;
		if(multipleFilter){
			oldVal = Params.alwaysFilterMultiple; 
			Params.alwaysFilterMultiple = true;

			System.out.println(
					"Multiple-occurrences filtering in all problematic SCCs");
			double mDiagStartTime = Util.getMSec();

			d = adj.computeDiagnosis(new LightSCCs(), mappings, 
					problematicSCCs, null, totalStartTime);
			if(stats != null){
				stats.put("mDiagTime", Util.getDiffmsec(mDiagStartTime));
				stats.put("mTotTime", Util.getDiffmsec(totalStartTime));
				stats.put("mNumM",new Double(d.size()));
				stats.put("mwM",new Double(d.getWeight()));
			}
			Params.alwaysFilterMultiple = oldVal;
		}
		else {
			oldVal = Params.disableFilterMultiple; 
			Params.disableFilterMultiple = true;
			d = adj.computeDiagnosis(new LightSCCs(), mappings, 
					problematicSCCs, computeStats ? stats : null, 
							totalStartTime);
			Params.disableFilterMultiple = oldVal;
		}		

		System.out.println("END: " + Util.getCurrTime() + "\n");
		OntoUtil.disposeAllReasoners();
		
		return d;
	}
}

