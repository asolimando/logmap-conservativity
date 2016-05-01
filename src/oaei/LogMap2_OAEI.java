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
package oaei;

import java.net.URL;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Set;

import logmap.LogMapWrapper;

import org.semanticweb.owlapi.model.OWLOntology;

import repair.ConservativityRepairFacility;

import uk.ac.ox.krr.logmap2.LogMap2Core;
import uk.ac.ox.krr.logmap2.Parameters;
import uk.ac.ox.krr.logmap2.mappings.objects.MappingObject;
import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;
import uk.ac.ox.krr.logmap2.oaei.Oraculo;
import uk.ac.ox.krr.logmap2.utilities.Utilities;
import uk.ac.ox.krr.logmap2.io.LogOutput;
import uk.ac.ox.krr.logmap2.io.OAEIAlignmentOutput;
import util.FileUtil;

/**
 * This classes manages the required wrapper of LogMap 2 in order to be accept and provided the required input and output data. 
 * @author root
 *
 */
public class LogMap2_OAEI {
	
	private long init_tot, fin;
	private double total_time=0.0;
	
	LogMap2Core logmap2;
	
	OAEIAlignmentOutput alignment_output;
	
	ConservativityRepairFacility consRepair;
	
	Set<MappingObjectStr> repairedMappings;
	
	public LogMap2_OAEI(){
		
		//LogOutput.showOutpuLog(false);
		//Oraculo.unsetStatusOraculo();
		
	}
	
	public void align(URL source, URL target) throws Exception{
		
		init_tot = Calendar.getInstance().getTimeInMillis();
		
		Parameters.readParameters();
		
		Oraculo.allowOracle(Parameters.allow_interactivity);
		
		logmap2 = new LogMap2Core(source.toURI().toString(), target.toURI().toString());

		Set<MappingObjectStr> origMappings = logmap2.getLogMapMappings();
		logmap2.clearIndexStructures();
		
		try {
			consRepair = new ConservativityRepairFacility(source.toURI().toString(), 
							target.toURI().toString(), false, 
							origMappings);
			consRepair.repair(false);
			repairedMappings = consRepair.getRepairedMappings();
		}
		catch(Exception | Error e){
			FileUtil.writeErrorLogAndConsole("Repair failed, using the computed alignment: " + 
					e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()));
			repairedMappings = origMappings;
		}
		
		if(repairedMappings.isEmpty() && !origMappings.isEmpty()){
			FileUtil.writeErrorLogAndConsole("Empty repaired alignment, using the original one");
			repairedMappings = origMappings;
		}
			
		fin = Calendar.getInstance().getTimeInMillis();
		
		//System.out.println("Matching Time (s): " + (float)((double)fin-(double)init_tot)/1000.0);
		total_time = (float)((double)fin-(double)init_tot)/1000.0;
		//total_time = total_time - time_loading;
		//System.out.println("Time loading ontos (s): " + time_loading);
		//System.out.println("Is Oracle active? " + Oraculo.isActive() + "  " + Oraculo.getStatusOraculo());
		if (Oraculo.isActive()){
			FileUtil.writeLogAndConsole("\tNumber of questions to oracle: " + Oraculo.getNumberOfQuestions());
		}
		FileUtil.writeLogAndConsole("LogMap 2 Total Matching Time (s): " + total_time);
		
	}
	
	
	public URL returnAlignmentFile() throws Exception{

		return LogMapWrapper.saveTemporaryMappings("alignment", logmap2.getIRIOntology1(), 
				logmap2.getIRIOntology2(), repairedMappings);
	}

}
