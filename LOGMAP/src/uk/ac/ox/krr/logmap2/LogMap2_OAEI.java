/*******************************************************************************
 * Copyright 2012 by the Department of Computer Science (University of Oxford)
 * 
 *    This file is part of LogMap.
 * 
 *    LogMap is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 * 
 *    LogMap is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 * 
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with LogMap.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package uk.ac.ox.krr.logmap2;

import java.net.URL;

import java.util.Calendar;

import uk.ac.ox.krr.logmap2.oaei.Oraculo;
import uk.ac.ox.krr.logmap2.utilities.Utilities;
import uk.ac.ox.krr.logmap2.io.LogOutput;
import uk.ac.ox.krr.logmap2.io.OAEIAlignmentOutput;

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
	
	
	public LogMap2_OAEI(){
		
		//LogOutput.showOutpuLog(false);
		//Oraculo.unsetStatusOraculo();
		
		
	}
	
	public void align(URL source, URL target) throws Exception{
		
		init_tot = Calendar.getInstance().getTimeInMillis();
		
		Parameters.readParameters();
		
		Oraculo.allowOracle(Parameters.allow_interactivity);
		
		logmap2 = new LogMap2Core(source.toURI().toString(), target.toURI().toString());
		
		fin = Calendar.getInstance().getTimeInMillis();
		
		//System.out.println("Matching Time (s): " + (float)((double)fin-(double)init_tot)/1000.0);
		total_time = (float)((double)fin-(double)init_tot)/1000.0;
		//total_time = total_time - time_loading;
		//System.out.println("Time loading ontos (s): " + time_loading);
		//System.out.println("Is Oracle active? " + Oraculo.isActive() + "  " + Oraculo.getStatusOraculo());
		if (Oraculo.isActive()){
			System.out.println("\tNumber of questions to oracle: " + Oraculo.getNumberOfQuestions());
		}
		System.out.println("LogMap 2 Total Matching Time (s): " + total_time);
		
	}
	
	
	public URL returnAlignmentFile() throws Exception{

		
		alignment_output = new OAEIAlignmentOutput("alignment", logmap2.getIRIOntology1(), logmap2.getIRIOntology2());
		
		int dir_mapping;
		
		if (Parameters.output_class_mappings){
		
			for (int ide1 : logmap2.getClassMappings().keySet()){
				for (int ide2 : logmap2.getClassMappings().get(ide1)){
					
					dir_mapping = logmap2.getDirClassMapping(ide1, ide2);
					
					if (dir_mapping!=Utilities.NoMap){
						
						if (dir_mapping!=Utilities.R2L){						
						
							//GSs in OAIE only contains, in general, equivalence mappings
							if (Parameters.output_equivalences_only){
								dir_mapping=Utilities.EQ;
							}
								
							alignment_output.addClassMapping2Output(
									logmap2.getIRI4ConceptIdentifier(ide1),
									logmap2.getIRI4ConceptIdentifier(ide2),
									dir_mapping,
									logmap2.getConfidence4ConceptMapping(ide1, ide2)
									);
						}
						else{
							
							if (Parameters.output_equivalences_only){
								dir_mapping=Utilities.EQ;
							}
							
							alignment_output.addClassMapping2Output(								
									logmap2.getIRI4ConceptIdentifier(ide2),
									logmap2.getIRI4ConceptIdentifier(ide1),
									dir_mapping,
									logmap2.getConfidence4ConceptMapping(ide1, ide2)
									);
						}
					}
				}
			}
		}
		
		
		if (Parameters.output_prop_mappings){
		
			for (int ide1 : logmap2.getDataPropMappings().keySet()){							
				alignment_output.addDataPropMapping2Output(
							logmap2.getIRI4DataPropIdentifier(ide1),
							logmap2.getIRI4DataPropIdentifier(logmap2.getDataPropMappings().get(ide1)),
							Utilities.EQ,  
							logmap2.getConfidence4DataPropConceptMapping(ide1, logmap2.getDataPropMappings().get(ide1))//1.0
							);
			}
			
			for (int ide1 : logmap2.getObjectPropMappings().keySet()){
					
				alignment_output.addObjPropMapping2Output(
							logmap2.getIRI4ObjectPropIdentifier(ide1),
							logmap2.getIRI4ObjectPropIdentifier(logmap2.getObjectPropMappings().get(ide1)),
							Utilities.EQ, 
							logmap2.getConfidence4ObjectPropConceptMapping(ide1, logmap2.getObjectPropMappings().get(ide1))//1.0
							);
			}
		}
		

		//Output for individuals
		if (Parameters.perform_instance_matching && Parameters.output_instance_mappings){
			
			for (int ide1 : logmap2.getInstanceMappings().keySet()){
				for (int ide2 : logmap2.getInstanceMappings().get(ide1)){
				
					alignment_output.addInstanceMapping2Output(
							logmap2.getIRI4InstanceIdentifier(ide1), 
							logmap2.getIRI4InstanceIdentifier(ide2), 
							logmap2.getConfidence4InstanceMapping(ide1, ide2)
						);
					
				}
			}
			
			
		}
		
		
		alignment_output.saveOutputFile();
		
		logmap2.clearIndexStructures();
		
		return alignment_output.returnAlignmentFile();
		
	}

}
