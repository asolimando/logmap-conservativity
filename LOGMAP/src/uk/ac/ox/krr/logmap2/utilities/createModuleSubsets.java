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

import java.util.*;


import uk.ac.ox.krr.logmap2.io.ReadFile;

import org.semanticweb.owlapi.model.*;
import uk.ac.ox.krr.logmap2.OntologyLoader;
import uk.ac.manchester.syntactic_locality.*;


/**
 * 
 * @author Ernesto
 *
 */
public class createModuleSubsets {

	String gs_mappings_file;
	
	String onto_file;
	String output_file_path;
	String output_iri;
	
	List<String> signature_str = new ArrayList<String>();
	
	//Set<OWLEntity> entitiesGS = new HashSet<OWLEntity>();
	Map<OWLEntity, Set<OWLEntity>> GS_mappings = new HashMap<OWLEntity, Set<OWLEntity>>();
	
	Set<OWLEntity> signature = new HashSet<OWLEntity>();
	
	
	
	OWLOntology module;
	
	
	int onto_id;
	
	
	ModuleExtractor extractor;
	
	OntologyLoader loader;
	
	
	int num=1110;
	
	
	String ontology = "";
	
	
	//boolean fromMappings=true;
	
	/**
	 * 
	 * @throws Exception
	 */
	public createModuleSubsets() throws Exception{
		
		initNCImodulesFMA();
		//initNCImodulesSNMD();
		//initFMAmodulesNCI();
		

		
		
		System.out.println(onto_file);
		loader = new OntologyLoader(onto_file);
		
		//It also loads mappings to check how many mappings are contained in module 
		loadMappingsGS();	
	
		extractor = new ModuleExtractor(loader.getOWLOntology(), false, false, false, true, false);
		
		//extract subsets of size 50, 100, 200, 300, 400, 500, 1000
		
		//Guided by gold standard
		//extractModules();
		
		
		//Random modules (less coupled)
		signature_str.clear();
		createSignatureFromOntology();
		extractModules();
		
		
		
		
	}
	
	
	
	private void extractModules(){
		//extractModules(1);
		//extractModules(2);
		extractModules(3);
		//extractModules(10);
		//extractModules(10);
		//extractModules(20);
		//extractModules(30);
		//extractModules(40);
		/*extractModules(50);
		extractModules(75);
		extractModules(100);
		extractModules(125);
		extractModules(150);
		extractModules(175);
		extractModules(200);
		extractModules(250);
		extractModules(300);
		extractModules(350);
		extractModules(400);
		extractModules(450);
		extractModules(500);*/
		//extractModules(600);
		//extractModules(700);
		//extractModules(800);
		//extractModules(900);
		//extractModules(1000);
	}
	
	

	
	
	
	private void initNCImodulesFMA(){
				
		//onto=Utilities.FMA;
		onto_id=Utilities.NCI;
		
		//onto_file= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2nci_dataset/oaei2012_FMA_whole_ontology.owl";		
		onto_file= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2nci_dataset/oaei2012_NCI_whole_ontology.owl";
		
		//if (fromMappings)
		output_file_path = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/NCI_Modules_fma/";
		//else
		//	output_file_path = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/NCI_Modules_fma_random/";
		
		output_iri = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";
		
		ontology = "NCI";
		
				
		gs_mappings_file = "/usr/local/data/DataUMLS/UMLS_source_data/onto_mappings_FMA_NCI_cleantDG.txt";
		
		
		
	}
	
	private void initNCImodulesSNMD(){
		onto_id=Utilities.NCI;
				
			
		onto_file= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2nci_dataset/oaei2012_NCI_whole_ontology.owl";
				
		output_file_path = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/NCI_Modules_snmd/";
		output_iri = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";
				
						
		gs_mappings_file = "/usr/local/data/DataUMLS/UMLS_source_data/onto_mappings_SNOMED_NCI_cleantDG.txt";
	}
	
	
	private void initFMAmodulesNCI(){
		
		onto_id=Utilities.FMA;
		
		
		onto_file= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2nci_dataset/oaei2012_FMA_whole_ontology.owl";
		
		
		//if (fromMappings)
		output_file_path = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/FMA_Modules_nci/";
		//else
		//	output_file_path = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/FMA_Modules_nci_random/";
		
		
		
		output_iri = "http://bioontology.org/projects/ontologies/fma/fmaOwlDlComponent_2_0";		
		
		ontology = "FMA";
		
		gs_mappings_file = "/usr/local/data/DataUMLS/UMLS_source_data/onto_mappings_FMA_NCI_cleantDG.txt";
		
		
		
	}
	
	
	/**
	 * Load Gold Standard Mappings
	 * @throws Exception
	 */
	private void createSignatureFromOntology() throws Exception{
	
		int count=0;
		
		for (OWLClass cls : loader.getClassesInSignature()){
			
			count++;
			if (count<15000)
				continue;
			
			if (signature_str.size()>3000)
				break;
				
			signature_str.add(cls.getIRI().toString());
			
		}

	}
	
	
	
	
	/**
	 * Load Gold Standard Mappings
	 * @throws Exception
	 */
	private void loadMappingsGS() throws Exception{
	
		ReadFile reader = new ReadFile(gs_mappings_file);
		
		
		String line;
		String[] elements;
		
		line=reader.readLine();
		
		
		while (line!=null) {
			
			if (line.indexOf("|")<0){
				line=reader.readLine();
				continue;
			}
			
			elements=line.split("\\|");
			
			if (onto_id==Utilities.NCI){			
				signature_str.add(elements[1]);
				//entitiesGS.add(loader.getDataFactory().getOWLClass(IRI.create(elements[1])));
				
				if (!GS_mappings.containsKey(loader.getDataFactory().getOWLClass(IRI.create(elements[1])))){
					GS_mappings.put(loader.getDataFactory().getOWLClass(IRI.create(elements[1])), new HashSet<OWLEntity>());
				}
				GS_mappings.get(loader.getDataFactory().getOWLClass(IRI.create(elements[1]))).add(loader.getDataFactory().getOWLClass(IRI.create(elements[0])));
				
				
			}
			else if (onto_id==Utilities.FMA || onto_id==Utilities.SNOMED){ //snomed can also be second position
				signature_str.add(elements[0]);
				//entitiesGS.add(loader.getDataFactory().getOWLClass(IRI.create(elements[0])));
				
				if (!GS_mappings.containsKey(loader.getDataFactory().getOWLClass(IRI.create(elements[0])))){
					GS_mappings.put(loader.getDataFactory().getOWLClass(IRI.create(elements[0])), new HashSet<OWLEntity>());
				}
				GS_mappings.get(loader.getDataFactory().getOWLClass(IRI.create(elements[0]))).add(loader.getDataFactory().getOWLClass(IRI.create(elements[1])));
				
			}
			
				
			line=reader.readLine();		
			

		}		
		
		reader.closeBuffer();

	}
	
	
	
	
	private void extractModules(int size){
		
		//Extract at most 10 modules of size "X"
		
		int lower;
		int upper;
		
		int mod_size;
		
		int numMappings;
		int numMappingsCateg;
		
		boolean loose_module; //size wrt num of mappings 
		
		
		int max=100;
		int max_tries = 1500;
		int tries = 0;
		
		Set<OWLEntity> lastSignature = new HashSet<OWLEntity>();
		
		for (int i=0; i<max; i++) {
			
			//lower = size*i/2;
			lower = size*i;
			upper = lower+size-1;
			
			tries++;
			
			if (tries > max_tries){
				break;
			}
			
			if (lower>=signature_str.size()){
				break;
			}
			if (upper>=signature_str.size()){
				upper=signature_str.size()-1;
			}
			
			for (String iri_str : signature_str.subList(lower, upper)){
				signature.add(loader.getDataFactory().getOWLClass(IRI.create(iri_str)));
				lastSignature.add(loader.getDataFactory().getOWLClass(IRI.create(iri_str)));
			}
			
			
			//Extract module
			//System.out.println("Creating module: " + output_file_path + "moduleNCI: " + size + "  " +  num + ".owl");
			
			module = extractor.getLocalityModuleForSignatureGroup(signature, output_iri);
			
			mod_size = module.getClassesInSignature().size();
						
			if (mod_size<2000 && mod_size>400){
				
				numMappings = extractNumOfMappingsInModule();			
				
				if (numMappings>0){
					
					loose_module = ((numMappings*4) < mod_size); //the ration size module and mappings is less than more that 5
					
					
					System.out.println("Signature interval: " + lower + "  " + upper + "  " +  size);
				
					numMappingsCateg = extractNumOfMappingsInModuleCategory(numMappings);
						
					System.out.println("\tFILE: "+ output_file_path + 
							"module" + ontology + "_" + loose_module + "_" +  numMappings + "_" + 
							mod_size + "_" + num + ".owl");
					
					if (numMappingsCateg<11){//Save is num mappings < 1500
						
						num++;
						
						//Information: category - numb mappings - module size - id module
						extractor.saveExtractedModule(output_file_path + 
								"module" + ontology + "_" + loose_module + "_" +  numMappings + "_" + 
								mod_size + "_" + num + ".owl");
					}
				}
				else{
					System.out.println("MODULE WITHOUT MAPPINGS");
					max++; //we increment max. We want 30 modules, at least with 1 mapping
				}
				//signature.clear();
				lastSignature.clear();
				
			}
			else{
				System.out.println("MODULE TOO BIG or SMALL: " + mod_size);
				max++; //we increment max. We want 30 modules, at least with 1 mapping
				if (mod_size>2000){
					signature.clear();//if too big clear
					//System.out.println(signature.size() + " - " + lastSignature.size());
					//signature.removeAll(lastSignature);
					//System.out.println(signature.size());
					lastSignature.clear();
				}
				else{
					lastSignature.clear();
				}
			}

			
			
					
		}
		
		
	}
	
	
	
	private int extractNumOfMappingsInModule(){
		
		
		Set<OWLEntity> module_classes = new HashSet<OWLEntity>();
		
		module_classes.addAll(module.getClassesInSignature());
		
		//int mappingsInModule = module_classes.size();
				
		//keep only those in GS
		module_classes.retainAll(GS_mappings.keySet());
		
		//mappingsInModule = mappingsInModule - module_classes.size();				
		//return mappingsInModule;
		
		//Ambiguity
		int numOfMappings=0;
		
		for (OWLEntity ent : module_classes){
			
			numOfMappings+=GS_mappings.get(ent).size(); //We may have mappings with more than 1 correspondence
			
		}
		
		return numOfMappings;	
		
		
		
	}
	
	
	private int extractNumOfMappingsInModuleCategory(int mappingsInModule){
		
		if (mappingsInModule<50){
			return 1;
		}
		else if (mappingsInModule<100){
			return 2;
		}
		else if (mappingsInModule<200){
			return 3;
		}
		else if (mappingsInModule<300){
			return 4;
		}
		else if (mappingsInModule<400){
			return 5;
		}
		else if (mappingsInModule<500){
			return 6;
		}
		else if (mappingsInModule<600){
			return 7;
		}
		else if (mappingsInModule<800){
			return 8;
		}
		else if (mappingsInModule<1000){
			return 9;
		}
		else if (mappingsInModule<1500){
			return 10;
		}
		else {
			return 11;
		}
	}
	
	
	
	public static void main(String[] args) {
		
		try{
			new createModuleSubsets();
		}
		catch (Exception e){
			e.printStackTrace();
		}
		
	}
	
	
	
}
