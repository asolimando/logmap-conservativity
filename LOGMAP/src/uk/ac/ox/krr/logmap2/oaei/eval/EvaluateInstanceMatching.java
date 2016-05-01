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
package uk.ac.ox.krr.logmap2.oaei.eval;

import java.io.File;
import java.util.Calendar;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import uk.ac.ox.krr.logmap2.LogMap2Core;
import uk.ac.ox.krr.logmap_lite.LogMap_Lite;
import uk.ac.ox.krr.logmap2.io.LogOutput;

public class EvaluateInstanceMatching {
	
	static final int SANDBOX=0;
	static final int IIMB=1;
	static final int IIMB_LARGE=2;
	
	private long init, fin;
	
	
	int max_folder=11;  //11 sandbox  //80 iimb
	String mappings_path = "/usr/local/data/Instance/sandbox/";
	//String mappings_path = "/usr/local/data/Instance/iimb/";
	String irirootpath_mappings = "file:" + mappings_path; //for onto 2
	
	
	//Onto 1 is fixed
	//String iri_onto1="file:/usr/local/data/Instance/iimb/onto.owl";
	String iri_onto1="file:/usr/local/data/Instance/sandbox/sandbox.owl";
	
	String gs_text_file;
	
	String outputfolderpath;
	
	//Onto 2 variable
	String iri_onto2;
	String shortOntoName;
	
	LogMap2Core logmap2;
	
	LogMap_Lite logmap_lite;
	
	int cases_unsat = 0;
	int num_unsat = 0;
	
	//int test = EvaluateInstanceMatching.IIMB_LARGE;
	//int test = EvaluateInstanceMatching.IIMB;
	int test = EvaluateInstanceMatching.SANDBOX;
	
	double avearge_P=0.0;
	double avearge_R=0.0;
	double avearge_F=0.0;
	
	String str3digits;
	
	boolean logmap=false;
	//logmap lite
	
	
	public EvaluateInstanceMatching(){
		
		init = Calendar.getInstance().getTimeInMillis();
		
		initStructures();
		
		
		LogOutput.showOutpuLog(false);
		
		try{
			
			String output;
		
			File directory;
			
			for (int folder=0; folder<=max_folder;folder++){//instance
				
				str3digits = convert2ThreeDigitStrNumber(folder) + "/";
				
				iri_onto2 = irirootpath_mappings + str3digits + shortOntoName;
				
				gs_text_file = mappings_path + str3digits + "refalign.txt";
				
				System.out.println("Ontology test " + str3digits);
				System.out.println("--------------------------------------------------");
				
				output = outputfolderpath + "/" + convert2ThreeDigitStrNumber(folder);
				
				directory = new File(output + "/");
				
				if (!directory.exists())
					directory.mkdirs();
				
				if (logmap){
					logmap2 = new LogMap2Core(
							iri_onto1,
							iri_onto2,
							output,
							gs_text_file,
							false); //eval impact
					
					if (logmap2.hasUnsatClasses()){
						cases_unsat++;
						
						num_unsat += logmap2.getNumUnsatClasses();
						
					}
					
					if (logmap2.getPrecision()>0.0){//avoid first case without GS
						avearge_P += logmap2.getPrecision();
						avearge_R += logmap2.getRecall();
						avearge_F += logmap2.getFmeasure();
					}
					logmap2.clearIndexStructures();
					logmap2 = null;
					
					System.out.println("\n");
				}
				else{//logmaplite
					
					logmap_lite = new LogMap_Lite(
							iri_onto1,
							iri_onto2,
							gs_text_file,
							output
							); 
					
					if (logmap_lite.getPrecision()>0.0){//avoid first case without GS
						avearge_P += logmap_lite.getPrecision();
						avearge_R += logmap_lite.getRecall();
						avearge_F += logmap_lite.getFmeasure();
					}
					logmap_lite = null;
					
				}
				
			}
			
			
			avearge_P = (double)avearge_P/((double)max_folder);
			avearge_R = (double)avearge_R/((double)max_folder);
			avearge_F = (double)avearge_F/((double)max_folder);
			
			
			
		}
		catch (Exception e){
			e.printStackTrace();
		}
		
		System.out.println("Cases with unsatisfiability: " + cases_unsat  + " (total unsat: " + num_unsat + ").");
		System.out.println("Average precision: " + avearge_P);
		System.out.println("Average recall: " + avearge_R);
		System.out.println("Average F-measure: " + avearge_F);
		System.out.println("F-measure from average: " + (2*avearge_R*avearge_P)/(avearge_P+avearge_R));
		
		fin = Calendar.getInstance().getTimeInMillis();
		System.out.println("Done, Time (s): " + (float)((double)fin-(double)init)/1000.0);
		
		
		
	}
	
	private void initStructures(){
		
		if (test==EvaluateInstanceMatching.SANDBOX){
			max_folder=11;
			mappings_path = "/usr/local/data/Instance/sandbox/";
			shortOntoName="sandbox.owl";
			iri_onto1="file:/usr/local/data/Instance/sandbox/sandbox.owl";
			if (logmap)
				outputfolderpath="/usr/local/data/Instance/sandbox/logmap_sandbox";
			else
				outputfolderpath="/usr/local/data/Instance/sandbox/logmap_lite_sandbox";
				
		}
		else if (test==EvaluateInstanceMatching.IIMB){
			max_folder=80;
			mappings_path = "/usr/local/data/Instance/iimb/";
			shortOntoName="onto.owl";
			iri_onto1="file:/usr/local/data/Instance/iimb/onto.owl";
			if (logmap)
				outputfolderpath="/usr/local/data/Instance/iimb/logmap_iimb";
			else
				outputfolderpath="/usr/local/data/Instance/iimb/logmap_lite_iimb";
		}
		else if (test==EvaluateInstanceMatching.IIMB_LARGE){
			max_folder=80;
			mappings_path = "/usr/local/data/Instance/iimb_large/";
			shortOntoName="onto.owl";
			iri_onto1="file:/usr/local/data/Instance/iimb_large/onto.owl";
			if (logmap)
				outputfolderpath="/usr/local/data/Instance/iimb_large/logmap_iimb_large";
			else
				outputfolderpath="/usr/local/data/Instance/iimb_large/logmap_lite_iimb_large";
		}
		irirootpath_mappings = "file:" + mappings_path; //for onto 2
	}
	
	
	private String convert2ThreeDigitStrNumber(int number){
		
		String three_digits = String.valueOf(number);
		
		if (three_digits.length()==1)
			three_digits="00" + three_digits;
		else if (three_digits.length()==2)
			three_digits="0" + three_digits;
		
		return three_digits;
		
	}
	
	
	public static void main(String[] args) {
		try{
			new EvaluateInstanceMatching();
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}

}
