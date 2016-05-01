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
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import logmap.LogMapWrapper;

import org.semanticweb.owl.align.AlignmentException;

import eval.PrRecEvaluator;
import fr.inrialpes.exmo.align.impl.eval.PRecEvaluator;

import scc.graphDataStructure.Diagnosis;
import scc.mapping.LightAlignment;
import scc.mapping.LightOAEIMappingHandler;
import scc.mapping.PRecOneOneEvaluator;
import scc.util.LegacyFileUtil;
import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;
import util.FileUtil;
import util.Params;
import util.Util;

public class TestScoreReference {
	private static String folderPrefix = "test/test";
	private static String [] computedAlignFolders = {
		folderPrefix + "8",
		folderPrefix + "10",
		folderPrefix + "12",
	};
	private static String [] refAlignFolders = {
		"oaei2012",
		"oaei2013",
		"oaei2014",
	};
	private static Map<String,String> trackRefAlignMap = new HashMap<>();
	static {
		trackRefAlignMap.put("anatomy", "mouse-human.rdf");
		trackRefAlignMap.put("library", "stw-thesoz.rdf");
	}

	private static String [] computedAlignTrailers = 
		{"sccpre","sccpost","logmappre","logmappost"};

	private static String testOutDir;
	private static LightOAEIMappingHandler oaeiParser = new LightOAEIMappingHandler();

	private static void init(){
		Params.verbosity = 0;
		Params.alwaysTestDiagnosis = false;

		testOutDir = Params.testOutDir + "PRF" + "/";

		FileUtil.createDirPath(testOutDir);

		String prefixFile = "testPRF";

		try {
			FileUtil.createTestDataFile(testOutDir + prefixFile + ".txt");
			FileUtil.createLogFile(testOutDir + prefixFile + "_log.txt");
		} catch (IOException e) {
			System.err.println("Impossible to create test log/data files, aborting");
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void testRunner(){		

		init();

		FileUtil.writeLogAndConsole("GLOBAL TEST START: " + Util.getCurrTime());
		int c = 0;
//		String [] trackNames = {"anatomy","conference","largebio-big","largebio-small","library"};
		
		for (int i = 0; i < computedAlignFolders.length; i++) {
			for (String tn : Params.trackNames) {
				++c;
				test(c, i,tn);
			}
		}

		FileUtil.writeLogAndConsole("GLOBAL TEST END: " + Util.getCurrTime());
	}

	private static void test(int testCounter, int id, final String trackName){

		FileUtil.writeLogAndConsole(
				"TEST " + testCounter + " START: " + Util.getCurrTime());

		FileUtil.writeLogAndConsole(
				"Testing year " + (2012+id) + ", track " + trackName);

		String refMappingDir = Params.dataFolder + refAlignFolders[id] + "/" 
				+ trackName + "/reference/";
		String matcherName = null;

		File directory = new File(computedAlignFolders[id] + "/mappings");
		File[] files = directory.listFiles(
				new FilenameFilter() {
					public boolean accept(File dir, String name) {
						return !name.toLowerCase().startsWith(".") && 
								name.toLowerCase().startsWith(trackName) && 
								name.endsWith("unsatFree.rdf");
					}
				}
				);
		Arrays.sort(files);

		for (File file : files) {

			final String mappingFile = 
					FileUtil.getFilenameNoExtensionFromFullPath(
							file.getAbsolutePath());

			FileUtil.writeLogAndConsole("Testing computed file: " + 
					mappingFile + " (" + file.getAbsolutePath() + ")");

			List<File> siblingFiles = new LinkedList<>();
			File tmpFile; 

			for (String suffix : computedAlignTrailers)
				if((tmpFile = new File(file.getAbsolutePath().replace("unsatFree", suffix))).exists())
					siblingFiles.add(tmpFile);

			if(siblingFiles.size() != 4){
				FileUtil.writeLogAndConsole("Skipping file, found " + 
						siblingFiles.size() + " sibling files instead of 4");
				continue;
			}
					
			final Set<String> validRefNames = selectReference(mappingFile, id);
			matcherName = extractMatcherName(mappingFile).toLowerCase();

			String trackNameFull = trackName;
			if(trackNameFull.equals("largebio")){
				matcherName = matcherName.substring(0,matcherName.indexOf('_'));
				if(mappingFile.contains("small"))
					trackNameFull = trackNameFull + "-small";
				else if(mappingFile.contains("big"))
					trackNameFull = trackNameFull + "-big";
			}

			File directoryRef = new File(refMappingDir);
			File[] filesRef = directoryRef.listFiles(
					new FilenameFilter() {
						public boolean accept(File dir, String name) {
							return !name.toLowerCase().startsWith(".") && 
									validRefNames.contains(name);
						}
					}
					);
			Arrays.sort(filesRef);

			for (File fRef : filesRef) {
				if(!fRef.exists() || !fRef.isFile())
					continue;

				FileUtil.writeLogAndConsole("Against reference file: " + fRef);

				boolean alignAPI = false;

				if(alignAPI){
					try {
						PRAnalysis(
								oaeiParser.parseAlignAPI(file,false), 
								oaeiParser.parseAlignAPI(siblingFiles.get(0),false), 
								oaeiParser.parseAlignAPI(siblingFiles.get(1),false), 
								oaeiParser.parseAlignAPI(siblingFiles.get(2),false), 
								oaeiParser.parseAlignAPI(siblingFiles.get(3),false), 
								oaeiParser.parseAlignAPI(fRef,false), 
								matcherName, (2012+id) + "", trackNameFull, 
								FileUtil.getFilenameNoExtensionFromFullPath(fRef.getPath()));
					} catch (IOException e) {
						FileUtil.writeErrorLogAndConsole("I/O error while analyzing " +
								"loading alignments: " + e.getMessage());
						e.printStackTrace();
					}
				}
				else {
					PRAnalysis(
							LogMapWrapper.getMappings(file.getPath()),
							LogMapWrapper.getMappings(siblingFiles.get(0).getPath()),
							LogMapWrapper.getMappings(siblingFiles.get(1).getPath()),
							LogMapWrapper.getMappings(siblingFiles.get(2).getPath()),
							LogMapWrapper.getMappings(siblingFiles.get(3).getPath()),
							LogMapWrapper.getMappings(fRef.getPath()),
							matcherName, (2012+id) + "", trackNameFull, 
							FileUtil.getFilenameNoExtensionFromFullPath(fRef.getPath()));
				}
			}
		}

		FileUtil.writeLogAndConsole(
				"TEST " + testCounter + " END: " + Util.getCurrTime());
	}

	private static void printPREval(PRecEvaluator e, LightAlignment mapping, 
			String mappingLabel, LightAlignment ref, String refLabel){
		FileUtil.writeLogAndConsole(mappingLabel + " = " + mapping.nbCells() 
				+ " " + refLabel + " = " + ref.nbCells()
				+ " Precision: " + e.getPrecision() 
				+ " Recall: " + e.getRecall() 
				+ " F-Measure: " + e.getFmeasure());
	}

	private static void printPREval(PrRecEvaluator e, Set<MappingObjectStr> mapping, 
			String mappingLabel, Set<MappingObjectStr> ref, String refLabel){
		FileUtil.writeLogAndConsole(mappingLabel + " = " + LogMapWrapper.countMappings(mapping) 
				+ " " + refLabel + " = " + LogMapWrapper.countMappings(ref)
				+ " Precision: " + e.getPrecision() 
				+ " Recall: " + e.getRecall() 
				+ " F-Measure: " + e.getFMeasure());
	}

	private static void PRAnalysis(LightAlignment original, 
			LightAlignment repairedSCCPre, LightAlignment repairedSCCPost, 
			LightAlignment repairedLogMapPre, LightAlignment repairedLogMapPost, 
			LightAlignment reference, String matcher, String year, String track, 
			String mappingName){
		if(original.nbCells() == 0){
			FileUtil.writeLogAndConsole("Empty alignment, skipping analysis");
			return;
		}

		FileUtil.writeLogAndConsole("\n" + matcher + " (" + year + "): " + 
				track + "(" + mappingName + ")");

		try {
			PRecOneOneEvaluator 
			evalOrig = new PRecOneOneEvaluator(reference, original),
			evalSCCPre = new PRecOneOneEvaluator(reference, repairedSCCPre),
			evalSCCPost = new PRecOneOneEvaluator(reference, repairedSCCPost),
			evalLogMapPre = new PRecOneOneEvaluator(reference, repairedLogMapPre),
			evalLogMapPost = new PRecOneOneEvaluator(reference, repairedLogMapPost);

			// evaluates over all properties (relations)
			evalOrig.eval(new Properties());
			printPREval(evalOrig, original, "|M|", reference, "|R|");

			evalSCCPre.eval(new Properties());
			printPREval(evalSCCPre, repairedSCCPre, "|M|sccPre", reference, "|R|");

			evalSCCPost.eval(new Properties());
			printPREval(evalSCCPost, repairedSCCPost, "|M|sccPost", reference, "|R|");

			evalLogMapPre.eval(new Properties());
			printPREval(evalLogMapPre, repairedLogMapPre, "|M|lmPre", reference, "|R|");

			evalLogMapPost.eval(new Properties());
			printPREval(evalLogMapPost, repairedLogMapPost, "|M|lmPost", reference, "|R|");

			printAnalysisToFile(matcher, track, year, evalOrig, evalSCCPre, 
					evalSCCPost, evalLogMapPre, evalLogMapPost);

			LightAlignment [] aligns = new LightAlignment[]{repairedSCCPre,
					repairedSCCPost,repairedLogMapPre,repairedLogMapPost};
			int c = 0;
			for (PRecEvaluator pr : 
				new PRecEvaluator[]{evalSCCPre,evalSCCPost,evalLogMapPost,evalLogMapPost}) {
				if(evalOrig.getPrecision() > pr.getPrecision()){
					LightAlignment copyAlign = new LightAlignment(original);					
					System.out.println("Precision violation:" + 
							"\nOriginal\\Repaired\n" + copyAlign.diff(aligns[c]));
					LightAlignment diff;
					if((diff = aligns[c].diff(original)).nbCells() > 0)
						System.err.println(
								"Repair has elements not belonging to " +
								"original alignment: " + diff);
				}
				++c;
			}

		} catch (AlignmentException e) {
			e.printStackTrace();
		}
	}

	private static void PRAnalysis(Set<MappingObjectStr> original, 
			Set<MappingObjectStr> repairedSCCPre, Set<MappingObjectStr> repairedSCCPost, 
			Set<MappingObjectStr> repairedLogMapPre, Set<MappingObjectStr> repairedLogMapPost, 
			Set<MappingObjectStr> reference, String matcher, String year, String track, 
			String mappingName){

		if(LogMapWrapper.countMappings(original) == 0){
			FileUtil.writeLogAndConsole("Empty alignment, skipping analysis");
			return;
		}

		FileUtil.writeLogAndConsole("\n" + matcher + " (" + year + "): " + 
				track + "(" + mappingName + ")");

		PrRecEvaluator evalOrig = new PrRecEvaluator(original, reference),
				evalSCCPre = new PrRecEvaluator(repairedSCCPre, reference),
				evalSCCPost = new PrRecEvaluator(repairedSCCPost, reference),
				evalLogMapPre = new PrRecEvaluator(repairedLogMapPre, reference),
				evalLogMapPost = new PrRecEvaluator(repairedLogMapPost, reference);

		List<PrRecEvaluator> evals = new LinkedList<>();
		evals.add(evalOrig);
		evals.add(evalSCCPre);
		evals.add(evalSCCPost);
		evals.add(evalLogMapPost);
		evals.add(evalLogMapPost);

		List<Set<MappingObjectStr>> aligns = new LinkedList<>();
		aligns.add(original);
		aligns.add(repairedSCCPre);
		aligns.add(repairedSCCPost);
		aligns.add(repairedLogMapPre);
		aligns.add(repairedLogMapPost);

		evalOrig.eval();
		printPREval(evalOrig, original, "|M|", reference, "|R|");

		evalSCCPre.eval();
		printPREval(evalSCCPre, repairedSCCPre, "|M|sccPre", reference, "|R|");

		evalSCCPost.eval();
		printPREval(evalSCCPost, repairedSCCPost, "|M|sccPost", reference, "|R|");

		evalLogMapPre.eval();
		printPREval(evalLogMapPre, repairedLogMapPre, "|M|lmPre", reference, "|R|");

		evalLogMapPost.eval();
		printPREval(evalLogMapPost, repairedLogMapPost, "|M|lmPost", reference, "|R|");

		
		double [] totPrec = {0,0,0,0,0};
		double [] totRecall = {0,0,0,0,0};
		double [] totFM = {0,0,0,0,0};
				
		for (PrRecEvaluator pr : evals) {
			int c = 0;
//			if(evalOrig.getPrecision() > pr.getPrecision()){
//				
//			}
			totPrec[c] += pr.getPrecision();
			totRecall[c] += pr.getRecall();
			totFM[c] += pr.getFMeasure();
			++c;
		}
		printAnalysisToFile(matcher,track,year, evals.toArray(new PrRecEvaluator[0]));

		//		try {
		//			PRecOneOneEvaluator 
		//			evalOrig = new PRecOneOneEvaluator(reference, original),
		//			evalSCCPre = new PRecOneOneEvaluator(reference, repairedSCCPre),
		//			evalSCCPost = new PRecOneOneEvaluator(reference, repairedSCCPost),
		//			evalLogMapPre = new PRecOneOneEvaluator(reference, repairedLogMapPre),
		//			evalLogMapPost = new PRecOneOneEvaluator(reference, repairedLogMapPost);
		//			
		//			// evaluates over all properties (relations)
		//			evalOrig.eval(new Properties());
		//			printPREval(evalOrig, original, "|M|", reference, "|R|");
		//
		//			evalSCCPre.eval(new Properties());
		//			printPREval(evalSCCPre, repairedSCCPre, "|M|sccPre", reference, "|R|");
		//
		//			evalSCCPost.eval(new Properties());
		//			printPREval(evalSCCPost, repairedSCCPost, "|M|sccPost", reference, "|R|");
		//
		//			evalLogMapPre.eval(new Properties());
		//			printPREval(evalLogMapPre, repairedLogMapPre, "|M|lmPre", reference, "|R|");
		//
		//			evalLogMapPost.eval(new Properties());
		//			printPREval(evalLogMapPost, repairedLogMapPost, "|M|lmPost", reference, "|R|");
		//
		//			printAnalysisToFile(matcher.toLowerCase()+" "+track+" "+mappingName+" "+year, 
		//					evalOrig, evalSCCPre, evalSCCPost, evalLogMapPre, evalLogMapPost);
		//
		//			LightAlignment [] aligns = new LightAlignment[]{repairedSCCPre,
		//					repairedSCCPost,repairedLogMapPre,repairedLogMapPost};
		//			int c = 0;
		//			for (PRecEvaluator pr : 
		//				new PRecEvaluator[]{evalSCCPre,evalSCCPost,evalLogMapPost,evalLogMapPost}) {
		//				if(evalOrig.getPrecision() > pr.getPrecision()){
		//					LightAlignment copyAlign = new LightAlignment(original);					
		//					System.out.println("Precision violation:" + 
		//							"\nOriginal\\Repaired\n" + copyAlign.diff(aligns[c]));
		//					if(aligns[c].diff(original).nbCells() > 0)
		//						throw new Error("Repair has elements not belonging to original alignment");
		//				}
		//				++c;
		//			}
		//			
		//		} catch (AlignmentException e) {
		//			e.printStackTrace();
		//		}
	}

	private static void printAnalysisToFile(String matcher, String track, 
			String year, PRecOneOneEvaluator...evals){

		FileUtil.writeDataOutFile(matcher + " " + track + " " + year);

		for (PRecOneOneEvaluator eval : evals)
			FileUtil.writeDataOutFile(
					" " + eval.getFound() + 
					" " + eval.getExpected() + 
					" " + eval.getPrecision() + 
					" " + eval.getRecall() +
					" " + eval.getFmeasure() + 
					" " + eval.getOneOneAlign1() +
					" " + eval.getOneOneAlign2());

		FileUtil.writeDataOutFile("\n");
	}

	private static void printAnalysisToFile(String matcher, String track, 
			String year, PrRecEvaluator...evals){

		FileUtil.writeDataOutFile(matcher + " " + track + " " + year);

		for (PrRecEvaluator eval : evals)
			FileUtil.writeDataOutFile(
					" " + eval.getFound() + 
					" " + eval.getExpected() + 
					" " + eval.getPrecision() + 
					" " + eval.getRecall() +
					" " + eval.getFMeasure());

		FileUtil.writeDataOutFile("\n");
	}

	private static String extractMatcherName(String mappingFile) {
		return mappingFile.split("-")[1];
	}

	private static Set<String> selectReference(String filename, int id){
		String [] tokens = filename.split("-");

		switch(tokens[0]){
		case "anatomy":
		case "library":
			return Collections.singleton(trackRefAlignMap.get(tokens[0]));
		case "conference":
			//conference-AROMA-cmt-confof-unsatFree.rdf
			int len = tokens.length;
			return Collections.singleton(tokens[len-3]+"-"+tokens[len-2]+".rdf");
		case "largebio":
			Map<String,String> refMap = null;

			switch(id){
			case 0:
				refMap = Params.largebioRef;
				break;
			case 1:
				refMap = Params.largebioRef13;
				break;
			case 2:
				refMap = Params.largebioRef14;
				break;
			}
			String pref;
			Set<String> res = new HashSet<>();

			//largebio-aroma_small_fma2nci-unsatFree.rdf
			if(tokens.length == 3){
				String [] subTokens = tokens[1].split("_")[2].split("2");
				pref = subTokens[0]+subTokens[1];
			}
			//largebio-AML-BK-R_small_fma2nci-unsatFree.rdf
			else{
				String [] subTokens = tokens[tokens.length-2].split("_");
				subTokens = subTokens[subTokens.length-1].split("2");
				pref = subTokens[0]+subTokens[1];
			}
			for (String key : refMap.keySet())
				if(key.startsWith(pref) && key.toLowerCase().contains("original"))
					res.add(refMap.get(key));

			return res;

		default:
			return Collections.emptySet();
		}
	}

	public static void main(String [] args){
		testRunner();
	}
}
