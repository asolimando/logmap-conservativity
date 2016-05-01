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
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import util.FileUtil;
import util.Params;

public abstract class AbstractMatcherTester extends AbstractCommonTester {
	/**
	 * @throws IOException 
	 * @throws OWLOntologyCreationException 
	 */
	public AbstractMatcherTester(String[] args, int testNumber) {

		this.testNumber = testNumber;
		
		if(args.length != 5)
			throw new IllegalArgumentException(
					"Five arguments are needed");
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
	
	protected void trackTest() throws OWLOntologyCreationException, IOException {

		Params.setParams(testKind);
		
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		String startTime = sdf.format(Calendar.getInstance().getTime());
		
		String prefixFile = "test" + testNumber + "_";

		prefixFile += testKind + "_" + repetitionsNum + "_matcher-" 
				+ (Params.fullDisj ? "full" : "light") + "-" + 
				(Params.preSCC ? "pre" : "post");

		FileUtil.createTestDataFile(testOutDir + prefixFile + ".txt");
		FileUtil.createLogFile(testOutDir + prefixFile + "_log.txt");

		String mappingDir = Params.dataFolder + "oaei2012/" + Params.trackName + "/";
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

		if(Params.trackName.equalsIgnoreCase("anatomy")){
			fstOnto = mappingDir + "onto/mouse.owl";
			sndOnto = mappingDir + "onto/human.owl";
		}
		else if(Params.trackName.equalsIgnoreCase("library")){
			fstOnto = mappingDir + "onto/stw.owl";
			sndOnto = mappingDir + "onto/thesoz.owl";
		}

		for (File elem : files){
			if(elem.isDirectory()){

				if(Params.trackName.equalsIgnoreCase("largebio")){

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
							if(!(tokens.length > 3) && tokens[1].equals(Params.ontoSize)){
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
				if(Params.trackName.equalsIgnoreCase("anatomy") || 
						Params.trackName.equalsIgnoreCase("library")){
					mappingFile = mappingDir + "alignments/" + elem.getName();
				}

				else if(Params.trackName.equalsIgnoreCase("conference")){
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
					FileUtil.writeLogAndConsole("" + count);
				}
				else
					FileUtil.writeErrorLogAndConsole("Mapping filename does not exists!");
			}
		}
		
		FileUtil.writeLogAndConsole("Start: " + startTime);
		FileUtil.writeLogAndConsole("End: " 
				+ new SimpleDateFormat("HH:mm:ss").format(
						Calendar.getInstance().getTime()));

		FileUtil.closeFiles();
	}
}
