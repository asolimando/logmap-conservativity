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

import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import util.FileUtil;
import util.Params;

public abstract class AbstractMatcherTest2015 extends CombinedMatcher12Test {

	public AbstractMatcherTest2015(String [] args, int testNumber) {		
		super(args, testNumber);
	}

	@Override
	protected void trackTest() throws OWLOntologyCreationException, IOException {

		if(testKind != 3)
			throw new IllegalArgumentException("Only conference track supported " +
					"at the moment for OAEI 2015");
		
		Params.setParams(testKind);
		
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		String startTime = sdf.format(Calendar.getInstance().getTime());
		
		String prefixFile = "test" + testNumber + "_";

		prefixFile += testKind + "_" + repetitionsNum + "_matcher-" 
				+ (Params.fullDisj ? "full" : "light") + "-" + 
				(Params.preSCC ? "pre" : "post");

		FileUtil.createTestDataFile(testOutDir + prefixFile + ".txt");
		FileUtil.createLogFile(testOutDir + prefixFile + "_log.txt");

		String mappingDir = Params.dataFolder + "oaei2015/" + Params.trackName + "/";
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

		boolean unloadOnto = false;
				
		for (File elem : files){
			if(elem.isFile()){
				unloadOnto = false;
				if(Params.trackName.equalsIgnoreCase("conference")){
					String [] ontos = elem.getName().substring(
							0, elem.getName().length()-4).split("-");
					fstOnto = elem.getParentFile().getParentFile() 
							+ "/onto/" + ontos[1] + ".owl";
					sndOnto = elem.getParentFile().getParentFile() 
							+ "/onto/" + ontos[2] + ".owl";
				}
				else if(Params.trackName.equalsIgnoreCase("largebio")){
					unloadOnto = true;

					String [] chunks = elem.getName().substring(
							0, elem.getName().length()-4).split("_");
					String [] ontos = chunks[2].split("2");
					
					if(chunks[1].equals("big")){
						if(testKind == 1)
							continue;
						Params.whole = true;
						Params.useModules = true;
						Params.ontoSize = "big";
					}
					else {
						if(testKind == 0)
							continue;
						Params.whole = false;
						Params.useModules = false;
						Params.ontoSize = "small";
					}
					
					fstOnto = elem.getParentFile().getParentFile() 
							+ "/onto/" + Params.largebioOntologies14.get(
							chunks[1]+ontos[0]+ontos[1]);
					sndOnto = elem.getParentFile().getParentFile() 
							+ "/onto/" + Params.largebioOntologies14.get(
							chunks[1]+ontos[1]+ontos[0]);
				}
				
				mappingFile = elem.getAbsolutePath();

				if(mappingFile == null)
					FileUtil.writeErrorLogAndConsole("Mapping pathname is null!");

				if(new File(mappingFile).exists()){
					test(mappingFile,fstOnto,sndOnto,unloadOnto);
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
