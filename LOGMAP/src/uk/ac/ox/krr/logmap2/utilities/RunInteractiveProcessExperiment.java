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
package uk.ac.ox.krr.logmap2.utilities;

import java.io.File;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

import uk.ac.ox.krr.logmap2.LogMap2Core;
import uk.ac.ox.krr.logmap2.interactive.objects.MappingObjectInteractivity;
import uk.ac.ox.krr.logmap2.io.WriteFile;
import uk.ac.ox.krr.logmap2.statistics.StatisticsManager;


public class RunInteractiveProcessExperiment {

	String path;
	
	String output_file;
	
	LogMap2Core logmap2;
	
	
	int experiment;
	
	int LOOSE=0;
	int COUPLED=1;
	int ALL=2;
	
	
	
	TreeSet<ResultObject> orderedResults = new TreeSet<ResultObject>(new ResultObjectComparator());
	
	public int getSizeModule(String filename){
		
		return Integer.valueOf(filename.split("_")[3]);
		
	}
	
	public boolean isLooseModule(String filename){
		
		return Boolean.valueOf(filename.split("_")[1]);
		
	}
	
	public int getSizeMappingsGS(String filename){
		
		return Integer.valueOf(filename.split("_")[2]);
		
	}
	
	double precision;
	double recall;
	double fmeasure;
	
	double precision_anc;
	double recall_anc;
	double fmeasure_anc;
	
	
	public void getPrecisionAndRecall(String fileName){
		
		int numGS = getSizeMappingsGS(fileName);
		
		
		//precision=((double)logmap2.getIntersection())/((double)logmap2.getNumMappings());
		//recall=((double)logmap2.getIntersection())/((double)numGS);
		//DO not use StatisticsManager.getMMissing(); since contains all, and the module may contain less
		precision=((double)StatisticsManager.getGoodMFinal())/((double)StatisticsManager.getMFinal());
		recall=((double)StatisticsManager.getGoodMFinal())/((double)numGS);
		
		
		fmeasure = (2.0*precision*recall)/(precision+recall);
		
		
		//FOR ANCHORS
		
		precision_anc=((double)StatisticsManager.Manchors_ok)/((double)StatisticsManager.Manchors);
		recall_anc=((double)StatisticsManager.Manchors_ok)/((double)numGS);
		
		
		fmeasure_anc = (2.0*precision_anc*recall_anc)/(precision_anc+recall_anc);
		
		
		
		precision = Math.round(precision*1000.0)/1000.0;
		recall = Math.round(recall*1000.0)/1000.0;
		fmeasure = Math.round(fmeasure*1000.0)/1000.0;
		
		precision_anc = Math.round(precision_anc*1000.0)/1000.0;
		recall_anc = Math.round(recall_anc*1000.0)/1000.0;
		fmeasure_anc = Math.round(fmeasure_anc*1000.0)/1000.0;
		
		
		
	}
	
	
	
	int n25=0;
	int n50=0;
	int n100=0;
	int n150=0;
	int n200=0;
	int n300=0;
	int n400=0;
	int n500=0;
	int n600=0;
	int n700=0;
	int n800=0;
	int n900=0;
	int n1000=0;
	
	int l25=0;
	int l50=0;
	int l100=0;
	int l150=0;
	int l200=0;
	int l300=0;
	int l400=0;
	int l500=0;
	int l600=0;
	int l700=0;
	int l800=0;
	int l900=0;
	int l1000=0;
	
	private void characterizeTest(String filename){
		
		int size = getSizeModule(filename);
		boolean loose = isLooseModule(filename);
		
		if (size<25){
			n25++;
			if(loose)
				l25++;
		}
		else if (size<50){
			n50++;
			if(loose)
				l50++;
		}
		else if (size<100){
			n100++;
			if(loose)
				l100++;
		}
		else if (size<150){
			n150++;
			if(loose)
				l150++;
		}
		else if (size<200){
			n200++;
			if(loose)
				l200++;
		}
		else if (size<300){
			n300++;
			if(loose)
				l300++;
		}
		else if (size<400){
			n400++;
			if(loose)
				l400++;
		}
		else if (size<500){
			n500++;
			if(loose)
				l500++;
		}
		else if (size<600){
			n600++;
			if(loose)
				l600++;
		}
		else if (size<700){
			n700++;
			if(loose)
				l700++;
		}
		else if (size<800){
			n800++;
			if(loose)
				l800++;
		}
		else if (size<900){
			n900++;
			if(loose)
				l900++;
		}
		else{
			n1000++;
			if(loose)
				l1000++;
		}
		
	}
	
	
	private void printStatistics(){
		
		System.out.println("Modules <25: "+ n25 + " / " + l25);
		System.out.println("Modules <50: "+ n50 + " / " + l50);
		System.out.println("Modules <100: "+ n100 + " / " + l100);
		System.out.println("Modules <150: "+ n150 + " / " + l150);
		System.out.println("Modules <200: "+ n200 + " / " + l200);
		System.out.println("Modules <300: "+ n300 + " / " + l300);
		System.out.println("Modules <400: "+ n400 + " / " + l400);
		System.out.println("Modules <500: "+ n500 + " / " + l500);
		System.out.println("Modules <600: "+ n600 + " / " + l600);
		System.out.println("Modules <700: "+ n700 + " / " + l700);
		System.out.println("Modules <800: "+ n800 + " / " + l800);
		System.out.println("Modules <900: "+ n900 + " / " + l900);
		System.out.println("Modules >900: "+ n1000 + " / " + l1000);
		
	}
	
	
	
	
	
	boolean useInteractivity=false;
	int error_user=0;
	boolean useHeuristics =false;
	
	boolean ask_everything;
	//boolean useHeuristics=false;
	//boolean orderQuestions=false;
	
	public RunInteractiveProcessExperiment(String path, int experiment, boolean useInteractivity, int error_user, boolean ask_everything, boolean useHeuristics){
		
		this.path=path;
		this.useInteractivity=useInteractivity;
		this.error_user=error_user;
		this.useHeuristics=useHeuristics;
		
		this.ask_everything=ask_everything;
		
		
		File directory = new File(path);
		String filenames[] = directory.list();
		
		
		this.experiment = experiment;
		
		String onto1;
		String onto2;
		
		
		try{
			//init = Calendar.getInstance().getTimeInMillis();
		
			for(int i=0; i<filenames.length; i++){
			//for(int i=0; i<3; i++){
				
				System.out.println("file " + i + " : "  + path + filenames[i]);
				
				
				if (false){
					characterizeTest(filenames[i]);
					continue;
				}
				
				if (experiment==Utilities.NCI){
					//Modules NCI
					onto1 = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2nci_dataset/oaei2012_FMA_whole_ontology.owl"; 
					onto2 = "file:" + path + filenames[i];
					output_file = "/usr/local/data/DataUMLS/InteractiveProcess/ExpWithModules/FMA2NCIModules_";
				}
				else{
					//Modules FMS 
					onto1 = "file:" + path + filenames[i];
					onto2 = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2nci_dataset/oaei2012_NCI_whole_ontology.owl.owl";
					output_file="/usr/local/data/DataUMLS/InteractiveProcess/ExpWithModules/FMAModules2NCI_";
				}
				
				
				
				
				
				logmap2 = new LogMap2Core(
					onto1,
					onto2,
					"", 
					"", 
					"/usr/local/data/DataUMLS/UMLS_source_data/onto_mappings_FMA_NCI_cleantDG.txt", 
					"",
					useInteractivity,
					useHeuristics,
					false,
					error_user,
					ask_everything,
					false,
					false
					);
				
				/*
				 * 
				String iri1_str, 
				String iri2_str, 
				String iri1_str_out, 
				String iri2_str_out, 
				String gs_mappings, 
				String logmap_mappings_path,
				boolean useInteractivity,
				boolean useHeuristics, //should be an input parameter
				boolean orderQuestions,
				int error_user,
				boolean ask_everything,
				boolean record_interactivity,
				boolean overlapping) throws Exception{
				 * 
				 */
			
				
				
				getPrecisionAndRecall(filenames[i]);
				
				
			
				orderedResults.add(
						new ResultObject(
								getSizeModule(filenames[i]),
								getSizeMappingsGS(filenames[i]),
								isLooseModule(filenames[i]),
								StatisticsManager.Manchors,
								StatisticsManager.Manchors_ok,
								precision_anc,
								recall_anc,
								fmeasure_anc,
								StatisticsManager.Mask,
								StatisticsManager.Mask_ok,
								StatisticsManager.Mask_heur,
								StatisticsManager.Mdisc+StatisticsManager.Mharddisc,
								StatisticsManager.Mdisc_ok+StatisticsManager.Mharddisc_ok,
								StatisticsManager.MFinal,
								StatisticsManager.MFinal_ok,
								precision,
								recall,
								fmeasure
								)
						);
				
				
				
				System.err.println(
						getSizeModule(filenames[i]) + "\t" +
						getSizeMappingsGS(filenames[i]) + "\t" +
						isLooseModule(filenames[i])  + "\t" +
						StatisticsManager.Manchors + "\t" +
						StatisticsManager.Manchors_ok + "\t" +
						precision_anc + "\t" +
						recall_anc + "\t" +
						fmeasure_anc + "\t" +
						StatisticsManager.Mask + "\t" +
						StatisticsManager.Mask_ok + "\t" +
						StatisticsManager.Mask_heur + "\t" +
						(StatisticsManager.Mdisc+StatisticsManager.Mharddisc) + "\t" +
						(StatisticsManager.Mdisc_ok+StatisticsManager.Mharddisc_ok) + "\t" +
						StatisticsManager.MFinal + "\t" +
						StatisticsManager.MFinal_ok  + "\t" +
						precision + "\t" +
						recall + "\t" +
						fmeasure
						);
				
				
				
				
			}
			
			//printStatistics();
			printOrdering(LOOSE);
			printOrdering(COUPLED);
			printOrdering(ALL);
			
			
		
			//fin = Calendar.getInstance().getTimeInMillis();
			//LogOutput.print("TOTAL TIME (s): " + (float)((double)fin-(double)init)/1000.0);
		}
		catch (Exception e){
			e.printStackTrace();
		}
		
		
	}
	
	
	
	private void printOrdering(int type){
		
		String typestr;
		
		String num_questions="";
		
		if (type==LOOSE)
			typestr="LOOSE";
		else if (type==COUPLED)
			typestr="COUPLED";
		else 
			typestr="ALL";
		
		if (ask_everything)
			num_questions="askAll";
		
		
		if (useHeuristics)
			num_questions="Interact_Heuristics";
		
		
		WriteFile writer;
		
		if (useInteractivity){		
			writer = new WriteFile(output_file + "User_" + error_user + "_" + typestr + "_" + num_questions + ".txt");
		}
		else{
			writer = new WriteFile(output_file + "LogMap2" + "_" + typestr + "_" + num_questions + ".txt");
		}
		
		Iterator<ResultObject> it = orderedResults.iterator();
		
		ResultObject resultObject;
		
		int num=0;
		
		writer.writeLine(
				"TAM_M" + "\t" +
				"NUM_M" + "\t" +
				"LOOSE" + "\t" +
				"ANC" + "\t" +
				"ANCOK" + "\t" +
				"Panc" + "\t" +
				"Ranc" + "\t" +
				"Fanc" + "\t" +
				"ASK" + "\t" +
				"ASKOK" + "\t" +
				"FINAL" + "\t" +
				"FINALOK"  + "\t" +
				"P" + "\t" +
				"R" + "\t" +
				"F" + "\t" +
				"DISC" + "\t" +
				"DISCOK" + "\t" +
				"ASK_HEUR"
				);
		
		while (it.hasNext()){
			
			resultObject = it.next();
			
			if (resultObject.fmeasure<0.01)
				continue; //avoid cases with 1-2 mappings and no candidates
			
			
			if ((type==LOOSE && !resultObject.loose) ||
				(type==COUPLED && resultObject.loose)){
				continue; //Do not add entry
			}
			
			
			writer.writeLine(
					resultObject.tam_module + "\t" +
					resultObject.tam_mappings + "\t" +
					resultObject.loose + "\t" +
					resultObject.anchors + "\t" +
					resultObject.anchors_ok + "\t" +
					resultObject.precision_anc + "\t" +
					resultObject.recall_anc  + "\t" +
					resultObject.fmeasure_anc  + "\t" +
					resultObject.toask + "\t" +
					resultObject.toask_ok + "\t" +
					resultObject.final_all + "\t" +
					resultObject.final_ok + "\t" +
					resultObject.precision + "\t" +
					resultObject.recall  + "\t" +
					resultObject.fmeasure + "\t" +
					resultObject.disc  + "\t" +
					resultObject.disc_ok + "\t" +
					resultObject.toask_heur
					);
			
			
		
			num++;
			
		}
		
		writer.closeBuffer();
		System.out.println("Num entries: " + num);
		
		
	}
	
	
	
	
	
	
	private String[] getAllFileNames(){
		File directory = new File(path);
		String filenames[] = directory.list();
		
		/*for(int i=0; i<filenames.length; i++){
			System.out.println(filenames[i]);
		}*/
		
		System.out.println(filenames.length);
		
		return filenames;
	}
	
	
	
	public static void main(String[] args) {
		
		try{
			if (args.length!=6){
				new RunInteractiveProcessExperiment(
						"/usr/local/data/DataUMLS/UMLS_Onto_Versions/NCI_Modules_fma/", 
						Utilities.NCI, 
						true, //interactivity
						10, //error user if interactivity
						true, //ask everything
						false); //heuristics
				//new RunInteractiveProcessExperiment("/usr/local/data/DataUMLS/UMLS_Onto_Versions/NCI_Modules_snmd/");

			}
			else{
				new RunInteractiveProcessExperiment(
						args[0], Integer.valueOf(args[1]), Boolean.valueOf(args[2]), 
						Integer.valueOf(args[3]), Boolean.valueOf(args[4]), Boolean.valueOf(args[5]));
				//NCI 1
			}
		}
		catch (Exception e){
			e.printStackTrace();
		}
		
	}
	
	
	
	public class ResultObject{
		
		int tam_module;
		int tam_mappings;
		boolean loose;
		
		int anchors;
		int anchors_ok;
		
		double precision_anc;
		double recall_anc;
		double fmeasure_anc;
		
		int toask;
		int toask_ok;
		int toask_heur;
		
		int disc;
		int disc_ok;
		
		int final_all;
		int final_ok;
		
		
		double precision;
		double recall;
		double fmeasure;
		
		
		public ResultObject(
				int tam_module,
				int tam_mappings,
				boolean loose,
				int anchors,
				int anchors_ok,				
				double precision_anc,
				double recall_anc,
				double fmeasure_anc,
				int toask,
				int toask_ok,
				int toask_heur,
				int disc,
				int disc_ok,
				int final_all,
				int final_ok,
				double precision,
				double recall,
				double fmeasure
				){

			this.tam_module=tam_module;
			this.tam_mappings=tam_mappings;
			this.loose = loose;
			
			this.anchors=anchors;
			this.anchors_ok=anchors_ok;
			
			
			this.precision_anc=precision_anc;
			this.recall_anc=recall_anc;
			this.fmeasure_anc=fmeasure_anc;
			
			this.toask=toask;
			this.toask_ok=toask_ok;
			this.toask_heur=toask_heur;
			
			this.disc=disc;
			this.disc_ok=disc_ok;
			
			this.final_all=final_all;
			this.final_ok=final_ok;
			
			this.precision=precision;
			this.recall=recall;
			this.fmeasure=fmeasure;
			
		}
		
		
	}
	
	/**
	 * No comparator. for no order of mappings to ask
	 * @author Ernesto
	 *
	 */
	private class ResultObjectComparator implements Comparator<ResultObject> {
		
		public int compare(ResultObject r1, ResultObject r2) {
			
			//if (r1.tam_mappings<r2.tam_mappings){
			if (r1.tam_module<r2.tam_module){
				return -1;
			}
			else{
				return 1;
			}
			
		}
		
	}
	
	
	
	
	
}
