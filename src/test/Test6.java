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
package test;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import logmap.LogMapWrapper;

import org.mindswap.pellet.utils.intset.IntIterator;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.InferredAxiomGenerator;
import org.semanticweb.owlapi.util.InferredEquivalentClassAxiomGenerator;
import org.semanticweb.owlapi.util.InferredOntologyGenerator;
import org.semanticweb.owlapi.util.InferredSubClassAxiomGenerator;

import uk.ac.ox.krr.logmap2.LogMap2_RepairFacility;
import uk.ac.ox.krr.logmap2.indexing.JointIndexManager;
import uk.ac.ox.krr.logmap2.indexing.OntologyProcessing;
import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;
import util.FileUtil;
import util.OntoUtil;
import util.Params;
import util.Util;
import auxStructures.Pair;
import enumerations.REASONER_KIND;
import enumerations.REPAIR_STRATEGY;

public class Test6 {
	/*
	 * This test aims at testing conservativity principle using LogMap 
	 */
	private static String [] trackNames = {"anatomy","conference","largebio"};
	private static String trackName = trackNames[2];
	private static boolean whole = false;
	private static String ontoSize = whole ? "big" : "small";
	private static int count;
	private static OWLOntologyManager manager = OntoUtil.getManager(false);
	private static OWLOntology fstO, sndO;

	private static boolean checkPost = false;
	private static boolean approxOnto = false;
	private static int repetitionsNum = 1;

	private static boolean rootViolations = false;
	private static boolean fullDisj = false;
	private static boolean useModules = false;
//	private static boolean alsoEquiv = false;


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

		int testKind;

		if(args.length != 4)
			throw new IllegalArgumentException(
					"Three arguments are needed");
		testKind = Integer.parseInt(args[0]);
		repetitionsNum = Integer.parseInt(args[1]);
		if("full".equals(args[2]))
			fullDisj = true;
		else if("light".equals(args[2]))
			fullDisj = false;
		else
			throw new IllegalArgumentException("Invalid parameter for disjointness method: " 
					+ args[2] + "\nValid arguments are \"full\" and \"light\"");

		if("direct".equals(args[3]))
			rootViolations = true;
		else if("all".equals(args[3]))
			rootViolations = false;
		else 
			throw new IllegalArgumentException("Invalid parameter for violations method: " 
					+ args[3] + "\nValid arguments are \"direct\" and \"all\"");
		
		Params.verbosity = 0;
		Params.alwaysTestDiagnosis = false;

		FileUtil.createDirPath(Params.test6OutDir);

		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		String startTime = sdf.format(Calendar.getInstance().getTime());

		String prefixFile = "test6_";
		//		if(testKind == 2){
		//			Params.reasonerKind = ENUM_REASONER.HERMIT;
		//			prefixFile = prefixFile + "hermit";
		//		}
		//		else if(testKind == 3){
		//			Params.reasonerKind = ENUM_REASONER.PELLET;
		//			prefixFile = prefixFile + "pellet";
		//		}
		//		else if(testKind == 0 || testKind == 1){
		//			Params.reasonerKind = ENUM_REASONER.ELK;
		//			prefixFile = prefixFile + "elk";
		//		}
		//		else {
		//			Params.reasonerKind = ENUM_REASONER.STRUCTURAL;
		//			prefixFile = prefixFile + "struct";			
		//		}
		prefixFile += testKind + "_" + repetitionsNum + "_matcher-" 
				+ (fullDisj ? "full" : "light");

		FileUtil.createTestDataFile(Params.test6OutDir + prefixFile + ".txt");
		FileUtil.createLogFile(Params.test6OutDir + prefixFile + "_log.txt");

		//		asmovTest();
		//		OntoUtil.unloadAllOntologies();
		//		lilyTest();
		//		OntoUtil.unloadAllOntologies();
		//		for (String name : trackNames) {
		//			trackName = name;
		if(testKind == 0) {
			trackName = trackNames[2];
			checkPost = false;
			Params.alignOntoClassificationTimeout = 20;
			Params.inputOntoClassificationTimeout = 20;
			whole = true;
			ontoSize = "big";
			useModules = true;
			trackTest();
			//OntoUtil.unloadAllOntologies();
		}
		else if(testKind == 1){
			trackName = trackNames[2];
			checkPost = false;
			whole = false;
			ontoSize = "small";
			trackTest();
		}
		else if(testKind == 2){
			trackName = trackNames[0];
			trackTest();
		}
		else if(testKind == 3) {
			trackName = trackNames[1];
			checkPost = false;
			Params.explanationsNumber = 1; 
			trackTest();
		}
		//		}

		FileUtil.writeLogAndConsole("Start: " + startTime);
		FileUtil.writeLogAndConsole("End: " 
				+ new SimpleDateFormat("HH:mm:ss").format(
						Calendar.getInstance().getTime()));

		FileUtil.closeFiles();
	}

	private static void lilyTest() throws OWLOntologyCreationException, 
	IOException {
		lilyTestAnatomy();
		lilyTestConference();
	}

	private static void lilyTestAnatomy() throws OWLOntologyCreationException, 
	IOException {

		String mappingDir = Params.dataFolder + "lily/oaei2007/anatomy";

		String fstOnto = mappingDir + "/mouse.owl";
		String sndOnto = mappingDir + "/human.owl";

		String mappingFile;

		for (int i = 1; i <= 3; i++) {
			mappingFile = mappingDir + "/lily" + i + ".rdf";

			if(new File(mappingFile).exists())
				test(mappingFile,fstOnto,sndOnto,i==3);
		}
	}

	private static void lilyTestConference() 
			throws OWLOntologyCreationException, IOException {

		String mappingDir = Params.dataFolder + "lily/oaei2007/conference";
		File directory = new File(mappingDir);
		String mappingFile;

		File[] files = directory.listFiles(
				new FilenameFilter() {
					public boolean accept(File dir, String name) {
						return name.toLowerCase().endsWith(".rdf") 
								&& !name.toLowerCase().startsWith(".");
					}
				}	
				);
		Arrays.sort(files);

		String fstOnto,sndOnto;

		for (File elem : files){
			if(elem.isFile()){
				String [] ontos = elem.getName().substring(0, 
						elem.getName().length()-4).split("-");
				fstOnto = elem.getParentFile() + "/" + ontos[0] + ".owl";
				sndOnto = elem.getParentFile() + "/" + ontos[1] + ".owl";
				mappingFile = elem.getAbsolutePath();
				test(mappingFile,fstOnto,sndOnto,false);
			}
		}
	}

	private static void asmovTest() 
			throws OWLOntologyCreationException, IOException{
		String mappingDir = Params.dataFolder + "asmov/";
		String [] years = {"2008","2009","2010"};

		for (String year : years) {
			File directory = new File(mappingDir + "ASMOV_" + year + "/");

			String fstOnto = mappingDir + "ASMOV_" + year + "/anatomy/mouse.owl";
			String sndOnto = mappingDir + "ASMOV_" + year + "/anatomy/human.owl";

			String mappingFile;

			for (int i = 1; i <= 4; i++) {
				mappingFile = directory.getAbsolutePath() + "/anatomy/ASMOV" 
						+ i + ".rdf";

				if(new File(mappingFile).exists())
					test(mappingFile,fstOnto,sndOnto,i==4);
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

			for (File elem : files){
				if(elem.isFile()){
					String [] ontos = elem.getName().substring(0, 
							elem.getName().length()-4).split("-");
					fstOnto = elem.getParentFile() + "/" + ontos[0] + ".owl";
					sndOnto = elem.getParentFile() + "/" + ontos[1] + ".owl";
					mappingFile = elem.getAbsolutePath();

					test(mappingFile,fstOnto,sndOnto,false);
				}
			}
		}
	}

	private static void trackTest() 
			throws OWLOntologyCreationException, IOException {

		String mappingDir = Params.dataFolder + "oaei2012/" + trackName + "/";
		String mappingFile = null;

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

		for (File elem : files){
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

								mappingFile = dir.getAbsolutePath();

								if(new File(mappingFile).exists()){
									test(mappingFile,fstOnto,sndOnto,true);
								}
							}
						}
					}
				}
			}

			else if(elem.isFile()){
				if(trackName.equalsIgnoreCase("anatomy")){
					mappingFile = mappingDir + "alignments/" + elem.getName();
				}

				else if(trackName.equalsIgnoreCase("conference")){
					String [] ontos = elem.getName().substring(
							0, elem.getName().length()-4).split("-");
					fstOnto = elem.getParentFile().getParentFile() 
							+ "/onto/" + ontos[1] + ".owl";
					sndOnto = elem.getParentFile().getParentFile() 
							+ "/onto/" + ontos[2] + ".owl";

					mappingFile = elem.getAbsolutePath();
				}

				if(mappingFile == null)
					FileUtil.writeErrorLogAndConsole("Mapping pathname is null!");

				if(new File(mappingFile).exists()){
					test(mappingFile,fstOnto,sndOnto,false);
					FileUtil.writeLogAndConsole("" + (++count));
				}
			}
		}
	}

	private static void loadOntologies(String fstOnto, String sndOnto){
		OntoUtil.unloadAllOntologies(manager);
		manager = OntoUtil.getManager(true);
		try {
			fstO = OntoUtil.load(fstOnto, true, manager);
			sndO = OntoUtil.load(sndOnto, true, manager);
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
		if(approxOnto){
			// TODO: cache index?
			fstO = OntoUtil.overApproximateOntologyClassification(
					manager, fstO, null);//LogMapWrapper.buildOntologyIndex(fstO,null));
			sndO = OntoUtil.overApproximateOntologyClassification(
					manager, sndO, null);//LogMapWrapper.buildOntologyIndex(sndO,null));
		}
	}

	public static void test(String mappingFile, String fstOnto, String sndOnto, 
			boolean unloadOnto) throws OWLOntologyCreationException, IOException {		

//		if(count == 198)
//			return;
		//		if(!"/home/ale/data/oaei2012/conference/alignments/MEDLEY-confof-iasted.rdf".equals(mappingFile))
		//			return;

		//		if(!"/home/ale/data/oaei2012/conference/alignments/MaasMatch-edas-ekaw.rdf".equals(mappingFile))
		//			return;

		for (int i = 0; i < repetitionsNum; i++) {			
			FileUtil.writeLogAndConsole("\nTEST " + (++count) + " START: " 
					+ Util.getCurrTime());

			FileUtil.writeLogAndConsole(mappingFile);

			long totalStartTime = Util.getMSec();

			loadOntologies(fstOnto, sndOnto);
			
			try {
				Test7.testLogMap(mappingFile, fstO, sndO, unloadOnto, 
					rootViolations, fullDisj, useModules, trackName);
			}
			catch(Exception | Error e){
				FileUtil.writeErrorLogAndConsole(
					"Skipped test " + count + ": \nError/Exception: " + e.getClass() 
					+ "\nMessage: " + e.getMessage() 
					+ "\nThrowing Line: " + e.getStackTrace()[0]);
			}
			finally {
				FileUtil.flushDataFile();
				FileUtil.writeLogAndConsole("Total test time: " 
						+ Util.getDiffmsec(totalStartTime));
			}
		}
	}
}