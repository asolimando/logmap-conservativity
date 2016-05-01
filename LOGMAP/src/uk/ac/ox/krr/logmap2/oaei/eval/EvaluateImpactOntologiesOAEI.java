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

import java.net.URI;
import java.util.Calendar;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.IRI;

import uk.ac.ox.krr.logmap2.LogMap2Core;
import uk.ac.ox.krr.logmap2.io.LogOutput;
//import uk.ac.ox.krr.logmap2.reasoning.deprecated.SatisfiabilityIntegration;
import uk.ac.ox.krr.logmap2.oaei.Oraculo;
import uk.ac.ox.krr.logmap2.reasoning.SatisfiabilityIntegration;
import uk.ac.ox.krr.logmap2.utilities.Utilities;
import uk.ac.ox.krr.logmap_lite.LogMap_Lite;


public class EvaluateImpactOntologiesOAEI {

	
	private long init, fin;
		
	
	
	
	private OWLOntologyManager ontology1Manager;
	private OWLOntologyManager ontology2Manager;	

		
	private OWLOntology ontology1;
	private OWLOntology ontology2;
	private OWLOntology mappingsOntology;
	
	
	//FMA2NCI mappings (tools oaei 2011.5)
	/**
	 * Impact TOOLS OAEI 2012 Large BioMed
	 * @throws Exception
	 */
	public EvaluateImpactOntologiesOAEI() throws Exception{
		
		
		//FMA2NCI
		String irirootpath;
		String irirootpath_fma2nci = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/oaei_2012/fma2nci/";
		String irirootpath_fma2snomed = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/oaei_2012/fma2snmd/";
		String irirootpath_snomed2nci = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/oaei_2012/snmd2nci/";
		//String irirootpath = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2nci_dataset/";		
		
		String onto1;
		String onto2;
		String pattern;
		
		int onto_pair;
		onto_pair = Utilities.FMA2NCI;
		//onto_pair = Utilities.FMA2SNOMED;
		//onto_pair = Utilities.SNOMED2NCI;
		String size;
		//size = "small";
		//size = "big";
		size = "whole";
		
		
		if (onto_pair==Utilities.FMA2NCI){
			irirootpath = irirootpath_fma2nci;
			if (size.equals("small")){
				onto1 = irirootpath + "oaei2012_FMA_small_overlapping_nci.owl";
				onto2 = irirootpath + "oaei2012_NCI_small_overlapping_fma.owl";
				pattern = "_small_fma2nci.owl";
			}
			else if (size.equals("big")){
				onto1 = irirootpath + "oaei2012_FMA_extended_overlapping_nci.owl";
				onto2 = irirootpath + "oaei2012_NCI_extended_overlapping_fma.owl";
				pattern = "_big_fma2nci.owl";
			}
			else{
				onto1 = irirootpath + "oaei2012_FMA_whole_ontology.owl";
				onto2 = irirootpath + "oaei2012_NCI_whole_ontology.owl";
				pattern = "_whole_fma2nci.owl";
			}
		}
		else if (onto_pair==Utilities.FMA2SNOMED){
			
			irirootpath = irirootpath_fma2snomed;
			
			if (size.equals("small")){
				onto1 = irirootpath + "oaei2012_FMA_small_overlapping_snomed.owl";
				onto2 = irirootpath + "oaei2012_SNOMED_small_overlapping_fma.owl";
				pattern = "_small_fma2snomed.owl";
			}
			else if (size.equals("big")){
				onto1 = irirootpath + "oaei2012_FMA_extended_overlapping_snomed.owl";
				onto2 = irirootpath + "oaei2012_SNOMED_extended_overlapping_fma_nci.owl";
				pattern = "_big_fma2snomed.owl";
			}
			else{
				onto1 = irirootpath_fma2nci + "oaei2012_FMA_whole_ontology.owl";
				onto2 = irirootpath + "oaei2012_SNOMED_whole_ontology.owl";
				pattern = "_whole2_fma2snomed.owl";
			}
		}
		else {
			
			
			irirootpath = irirootpath_snomed2nci;
			
			if (size.equals("small")){
				onto1 = irirootpath + "oaei2012_NCI_small_overlapping_snomed.owl";
				onto2 = irirootpath + "oaei2012_SNOMED_small_overlapping_nci.owl";
				pattern = "_small_snomed2nci.owl";
			}
			else if (size.equals("big")){
				onto1 = irirootpath + "oaei2012_NCI_extended_overlapping_snomed.owl";
				onto2 = irirootpath_fma2snomed + "oaei2012_SNOMED_extended_overlapping_fma_nci.owl";
				pattern = "_big_snomed2nci.owl";
			}
			else{
				onto1 = irirootpath_fma2nci + "oaei2012_NCI_whole_ontology.owl";
				onto2 = irirootpath_fma2snomed + "oaei2012_SNOMED_whole_ontology.owl";
				pattern = "_whole2_snomed2nci.owl";
			}
			
			
		}
		
				
		
		
		
		
		//String mappings_path = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/Mappings_Tools/";
		//String mappings_path = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/Mappings_Tools_2012/";

		String base_path = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/";
		String mappings_path = base_path +  "OAEI_datasets/oaei_2013/reference_alignment/repaired_alignments/";
		
		String irirootpath_mappings = "file:" + mappings_path; 
		
		File directory = new File(mappings_path);
		String filenames[] = directory.list();
		
		System.out.println("Loading ontologies...");
		loadOntology1(onto1);
		loadOntology2(onto2);
		System.out.println("...Done");
		
		pattern = "oaei2013_FMA2NCI_repaired_UMLS_mappings.owl";
		
		for(int i=0; i<filenames.length; i++){
			
			if (!filenames[i].contains(pattern)) 
					//||	!(filenames[i].contains("servomap") || filenames[i].contains("yam") || filenames[i].contains("logmaplt")))
			//if (!filenames[i].contains(task) || !filenames[i].contains(".owl"))
			//if (!filenames[i].contains(task) || !filenames[i].contains(".owl") || !(filenames[i].contains("hertuda")|| filenames[i].contains("hotmatch")))
				continue;
			
			mappingsOntology = loadOntology(irirootpath_mappings + filenames[i]);
			
			System.out.println("Evaluation mappings: " + filenames[i]);
			System.out.println(irirootpath_mappings + filenames[i]);
			
			SatisfiabilityIntegration.setTimeoutClassification(7200);
			SatisfiabilityIntegration.setTimeoutClassSatisfiabilityCheck(10);
			
			SatisfiabilityIntegration satIntegration = new SatisfiabilityIntegration(ontology1, ontology2, mappingsOntology, true, true, false);
			
			satIntegration.clear();
			satIntegration=null;
			
			System.out.println("\n\n");
		
		}
		
		
		
	}
	
	
	public OWLOntology loadOntology(String uri) throws Exception{		
		return loadOntology(IRI.create(uri));
	}
	
	public OWLOntology loadOntology(IRI uri) throws Exception{
		OWLOntologyManager ontologyManager=OWLManager.createOWLOntologyManager();
		return ontologyManager.loadOntology(uri);
		
	}
	
	
	/**
	 * Eval Impact 
	 * Benchmark or conference
	 * 
	 **/
	public EvaluateImpactOntologiesOAEI(boolean conference, boolean logmap, boolean eval_impact, boolean oraculo){
		
		init = Calendar.getInstance().getTimeInMillis();
		
		LogOutput.showOutpuLog(true);
		
		String path;
		//String base_path;
		if (conference){
			path = "/usr/local/data/ConfOntosOAEI/";
			//base_path = "/usr/local/data/MappingsConferenceBenchmark/";
			//path = base_path = "ontologies/";
		}
		else
			path = "/usr/local/data/Multifarm/";
		
		File directory = new File(path);
		String filenames[] = directory.list();
		
		LogMap2Core logmap2;
		LogMap_Lite logmap_lite;
		
		int cases_unsat = 0;
		int num_unsat = 0;
		
		
		Oraculo.setLocalOracle(oraculo);
		//Oraculo.setActive(false);
		Oraculo.loadOraculoConference();
		
		
		
		try{
			//init = Calendar.getInstance().getTimeInMillis();
		
			int pairs = 0;
			
			for(int i=0; i<filenames.length-1; i++){
				
				if (filenames[i].contains("txt") || filenames[i].contains("xml") || (filenames[i].contains("cmt") && filenames[i].contains("_")))
					continue;
				
								
				//if (!filenames[i].contains("cmt.owl"))
				//	continue;
				
				for(int j=i+1; j<filenames.length; j++){
					
					if (filenames[j].contains("txt") || filenames[j].contains("xml") || (filenames[j].contains("cmt") && filenames[j].contains("_")))
						continue;
					
					//if (!filenames[j].contains("cocus.owl"))
					//	continue;
					

					//if (!filenames[j].contains("MyReview.owl"))
					//	continue;
					
					pairs++;
					
					System.out.println("Ontology pair " + pairs + ": " + filenames[i] + " - " + filenames[j]);
					System.out.println("--------------------------------------------------");
					
					if (logmap){
						logmap2 = new LogMap2Core(
								"file:" + path + filenames[i],
								"file:" + path + filenames[j], 
								eval_impact); //eval impact
						
						if (logmap2.hasUnsatClasses()){
							cases_unsat++;
							
							num_unsat += logmap2.getNumUnsatClasses();
							
						}
						
						
						logmap2.clearIndexStructures();
						logmap2 = null;
					}
					else {//logmap_lite
						
						logmap_lite = new LogMap_Lite(
								"file:" + path + filenames[i],
								"file:" + path + filenames[j], 
								"",
								false,
								false,
								eval_impact); //eval impact
						
						if (logmap_lite.hasUnsatClasses()){
							cases_unsat++;
							
							num_unsat += logmap_lite.getNumUnsatClasses();
							
						}
						
						logmap_lite = null;
						

					}
					System.out.println("\n");
					
				}
				//break;
			}
		}
		catch (Exception e){
			e.printStackTrace();
		}
		
		System.out.println("Cases with unsatisfiability: " + cases_unsat  + " (total unsat: " + num_unsat + ").");
		fin = Calendar.getInstance().getTimeInMillis();
		System.out.println("Done, Time (s): " + (float)((double)fin-(double)init)/1000.0);
	}
	
	
	
	
	public EvaluateImpactOntologiesOAEI(boolean conf) throws Exception{
		
		
		init = Calendar.getInstance().getTimeInMillis();
		
		boolean eval_impact;
		
		eval_impact=true;
		//eval_impact=false;
		
		String base_path = "/usr/local/data/MappingsConferenceBenchmark/";
		
		String path_alignments = base_path + "reference-alignment-subset2012/";
		
		String iri_path_alignments = "file:" + base_path + "reference-alignment-subset2012/";
		
		String iri_path_ontologies = "file:" + base_path + "ontologies/";
		
		
		//String pattern=".rdf";
		String pattern=".txt";
		//pattern = "edas-ekaw.txt";
		
		

		File directory = new File(path_alignments);
		String filenames[] = directory.list();
		
		String[] elements;
		
		
		LogMap2Core logmap2;
		//LogOutput.showOutpuLog(true);
		//Oraculo.setActive(true);
		Oraculo.setLocalOracle(true);
		Oraculo.loadOraculoConference();
		
		
		//System.out.println(Oraculo.isMappingValid("http://edas#ConferenceEvent", "http://ekaw#Event"));
		//System.out.println(Oraculo.isMappingValid("http://edas#ConferenceEvent", "http://ekaw#Conference"));
		
		
		int pairs=0;
		int cases_unsat = 0;
		int num_unsat = 0;
		
		for(int i=0; i<filenames.length; i++){
			
			if (!filenames[i].contains(pattern)) 
				continue;
			
			elements = filenames[i].split("-|\\.");
			
			pairs++;
			
			//System.out.println("Ontology pair " + pairs + ": " + elements[0] + " - " + elements[1]);
			//System.out.println("--------------------------------------------------");
			
			String onto1_str = iri_path_ontologies + elements[0] + ".owl";
			String onto2_str = iri_path_ontologies + elements[1] + ".owl";
			String mappings_str = path_alignments + filenames[i];
			
			//System.out.println("Loading ontologies...");
			//System.out.println("\t" + iri_path_ontologies + elements[0] + ".owl");
			//ontology1 = loadOntology(iri_path_ontologies + elements[0] + ".owl");
			//System.out.println("\t" + iri_path_ontologies + elements[1] + ".owl");
			//ontology2 = loadOntology(iri_path_ontologies + elements[1] + ".owl");
			//System.out.println("\t" + iri_path_alignments + filenames[i]);
			//mappingsOntology = loadOntology(iri_path_alignments + filenames[i]);
			//System.out.println("...Done");
			
			logmap2 = new LogMap2Core(
					onto1_str,
					onto2_str, 
					eval_impact, //eval impact
					mappings_str); 
			
			if (logmap2.hasUnsatClasses()){
				cases_unsat++;
				
				num_unsat += logmap2.getNumUnsatClasses();
				
			}
			
			System.out.println(elements[0] + "-" + elements[1] + "\t" + logmap2.getPrecision()  + "\t" + logmap2.getRecall()  + "\t" + logmap2.getFmeasure() + "\t" + logmap2.getNumUnsatClasses());
			
			
			//System.out.println("NUmber of questions oraculo: " + Oraculo.getNumberOfQuestions());
			
			logmap2.clearIndexStructures();
			logmap2 = null;
			
			//System.out.println("\n\n");
			
		}
		
		System.out.println("\n\n");
		System.out.println("Number of questions oraculo: " + Oraculo.getNumberOfQuestions());
		System.out.println("Cases with unsatisfiability: " + cases_unsat  + " (total unsat: " + num_unsat + ").");
		fin = Calendar.getInstance().getTimeInMillis();
		System.out.println("Done, Time (s): " + (float)((double)fin-(double)init)/1000.0);
		//System.out.println("\n\n");
		
		
	}
	
	
	
	
	
	public static void main(String[] args) {
		try{
			//new EvaluateImpactOntologiesOAEI(true);
			//
			new EvaluateImpactOntologiesOAEI(true, false, false, false);//conf and logmap
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}
	
	
	
	//Ontology 1
	public void loadOntology1(String uri) throws Exception{		
		loadOntology1(IRI.create(uri));
	}
	
	public void loadOntology1(IRI uri) throws Exception{
		ontology1Manager=OWLManager.createOWLOntologyManager();
		ontology1 = ontology1Manager.loadOntology(uri);
		
	
	}
	
	
	//Ontology 2
	public void loadOntology2(String uri) throws Exception{		
		loadOntology2(IRI.create(uri));
	}
	
	public void loadOntology2(IRI uri) throws Exception{
		ontology2Manager=OWLManager.createOWLOntologyManager();
		ontology2 = ontology2Manager.loadOntology(uri);
	
	}
	
	
	
}
