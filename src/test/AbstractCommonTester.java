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
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import util.FileUtil;
import util.OntoUtil;
import util.Params;
import util.Util;

public abstract class AbstractCommonTester implements ITester {

	protected int count;
	protected static OWLOntologyManager manager = OntoUtil.getManager(false);
	protected static OWLOntology fstO, sndO;
	
	protected int testNumber;
	protected int testKind;
	protected int repetitionsNum = 1;
	
	protected String testOutDir;
	protected String testMappingDir;
	protected String testOntoDir;

	private static void loadOntologies(String fstOnto, String sndOnto){
		OntoUtil.unloadAllOntologies(manager);
		manager = OntoUtil.getManager(true);
		try {
			fstO = OntoUtil.load(fstOnto, true, manager);
			sndO = OntoUtil.load(sndOnto, true, manager);
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
	}

	public void test(String mappingFile, String fstOnto, String sndOnto, 
			boolean unloadOnto) throws OWLOntologyCreationException, IOException {		

		for (int i = 0; i < repetitionsNum; i++) {
			FileUtil.writeLogAndConsole("\nTEST " + (++count) + " START: " 
					+ Util.getCurrTime());

			FileUtil.writeLogAndConsole(mappingFile);

			long totalStartTime = Util.getMSec();

			try {
				loadOntologies(fstOnto, sndOnto);
			
				realTest(mappingFile, fstO, sndO, unloadOnto, 
						Params.rootViolations, Params.fullDisj, 
						Params.useModules, Params.trackName);
			}
			catch(Error | Exception e){
				FileUtil.writeErrorLogAndConsole("Skipping test " + count 
						+ ", reason: " + e.getMessage() 
						+ "\nProblem location: " 
						+ (e.getStackTrace().length > 0 
								? e.getStackTrace()[0] : "N/A"));
				FileUtil.printStackTrace(e, Params.showFullExceptionTrace);
			}
			
			FileUtil.flushDataFile();
			FileUtil.writeLogAndConsole("Total test time: " 
					+ Util.getDiffmsec(totalStartTime));
		}
	}
}