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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import util.FileUtil;
import util.Params;

public abstract class AbstractGoldTester extends AbstractCommonTester {
	
	private static String oaeiFolder = "oaei2013/";
	private static Map<String, List<String>> refAlignMap = Params.referenceAlign13;
	private static Map<String, String> ontoMap = Params.largebioOntologies13;
	
	/**
	 * @throws IOException 
	 * @throws OWLOntologyCreationException 
	 */
	public AbstractGoldTester(String[] args, int testNumber) {

		this.testNumber = testNumber;
		
		if(testNumber == 11){
			oaeiFolder = "oaei2014/";
			refAlignMap = Params.referenceAlign14;
			ontoMap = Params.largebioOntologies14;
		}
		
		if(args.length != 5)
			throw new IllegalArgumentException(
					"Three arguments are needed");
		testKind = Integer.parseInt(args[0]);
		repetitionsNum = Integer.parseInt(args[1]);
		if("full".equals(args[2]))
			Params.fullDisj = true;
		else if("light".equals(args[2]))
			Params.fullDisj = false;
		else
			throw new IllegalArgumentException(
					"Invalid parameter for disjointness method: " + args[2] + 
						"\nValid arguments are \"full\" and \"light\"");

		if("direct".equals(args[3]))
			Params.rootViolations = true;
		else if("all".equals(args[3]))
			Params.rootViolations = false;
		else 
			throw new IllegalArgumentException(
					"Invalid parameter for violations method: " + args[3] + 
						"\nValid arguments are \"direct\" and \"all\"");

		if("sccPre".equals(args[4]))
			Params.preSCC = true;
		else if("sccPost".equals(args[4]))
			Params.preSCC = false;
		else 
			throw new IllegalArgumentException(
					"Invalid parameter for SCC to be used as pre-processing " +
					"	or post-processing: " + args[4] + 
						"\nValid arguments are \"sccPre\" and \"sccPost\"");
		
		Params.verbosity = 0;
		Params.alwaysTestDiagnosis = false;

		testOutDir = Params.testOutDir + testNumber + "/";
		testMappingDir = testOutDir + "mappings/";
		testOntoDir = testOutDir + "ontologies/";
		
		FileUtil.createDirPath(testOutDir);
		FileUtil.createDirPath(testMappingDir);
		FileUtil.createDirPath(testOntoDir);
		
		Params.setParams(testKind);
	}
	
	public void trackTest() throws OWLOntologyCreationException, IOException {

		Params.setParams(testKind);

		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		String startTime = sdf.format(Calendar.getInstance().getTime());
		
		String prefixFile = "test" + testNumber + "_";

		prefixFile += testKind + "_" + repetitionsNum + "_gold-" 
				+ (Params.fullDisj ? "full" : "light") + "-" 
				+ (Params.preSCC ? "pre" : "post");

		FileUtil.createTestDataFile(testOutDir + prefixFile + ".txt");
		FileUtil.createLogFile(testOutDir + prefixFile + "_log.txt");

		String mappingDir = Params.dataFolder + oaeiFolder + Params.trackName + "/";
		String mappingFile = null;
		String fstOnto = null, sndOnto = null;
		List<String> alignments = refAlignMap.get(Params.trackName);

		if(Params.trackName.equalsIgnoreCase("anatomy")){
			fstOnto = mappingDir + "onto/mouse.owl";
			sndOnto = mappingDir + "onto/human.owl";
			mappingFile = mappingDir + "reference/" + alignments.get(0);
			test(mappingFile, fstOnto, sndOnto, false);
		}
		else if(Params.trackName.equalsIgnoreCase("library")){
			fstOnto = mappingDir + "onto/stw.owl";
			sndOnto = mappingDir + "onto/thesoz.owl";
			mappingFile = mappingDir + "reference/" + alignments.get(0);
			test(mappingFile, fstOnto, sndOnto, false);
		}
		else if(Params.trackName.equalsIgnoreCase("conference")){
			for (String align : alignments) {
				String [] ontos = align.substring(
						0, align.length()-4).split("-");
				fstOnto = mappingDir + "onto/" + ontos[0] + ".owl";
				sndOnto = mappingDir + "onto/" + ontos[1] + ".owl";
				mappingFile = mappingDir + "reference/" + align;
				test(mappingFile, fstOnto, sndOnto, false);
			}
		}
		else if(Params.trackName.equalsIgnoreCase("largebio")){
			for (String align : alignments) {

				// SNOMED2NCI is too long to be processed by the basic method 
				if(align.contains("SNOMED2NCI") && Params.fullDisj){
					FileUtil.writeLogAndConsole("Skipping full test (too long) for " + align);
					continue;
				}

				String [] ontoNames = align.split("_")[1].split("2");
				mappingFile = mappingDir + "reference/" + align;

				ontoNames[0] = ontoNames[0].toLowerCase();
				ontoNames[1] = ontoNames[1].toLowerCase();

				fstOnto = mappingDir + "onto/" + ontoMap.get(
						Params.ontoSize + ontoNames[0] + ontoNames[1]);
				sndOnto = mappingDir + "onto/" + ontoMap.get(
						Params.ontoSize + ontoNames[1] + ontoNames[0]);

				test(mappingFile, fstOnto, sndOnto, true);
			}
		}
		else if(Params.trackName.equalsIgnoreCase("optique")){
			fstOnto = Params.dataFolder + "Slegge_NPD_usecase/NPD_adaptedQFI.owl";
			sndOnto = Params.dataFolder 
					+ "Slegge_NPD_usecase/bootstrapped_onto_slegge_whole.owl";
			mappingFile = Params.dataFolder + "Slegge_NPD_usecase/" + alignments.get(0);
			test(mappingFile, fstOnto, sndOnto, false);
		}
		
		FileUtil.writeLogAndConsole("Start: " + startTime);
		FileUtil.writeLogAndConsole("End: " 
				+ new SimpleDateFormat("HH:mm:ss").format(
						Calendar.getInstance().getTime()));

		FileUtil.closeFiles();
	}
}
