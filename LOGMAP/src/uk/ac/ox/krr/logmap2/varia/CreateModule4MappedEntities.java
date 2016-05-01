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
package uk.ac.ox.krr.logmap2.varia;



//import java.net.URI;


import java.util.Calendar;


import org.semanticweb.owlapi.model.IRI;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLEntity;


import uk.ac.manchester.syntactic_locality.ModuleExtractor;
import uk.ac.ox.krr.logmap2.io.ReadFile;

import java.util.*;

/**
 * This class will extract a module for those entities involved 
 * in the mappings set
 * @author Ernesto Jimenez Ruiz
 * Co-Authors: Bernardo Cuenca Grau, Rafa Berlana Llavori and Ian Horrocks
 * Jaume I University of Castellon and Oxford University
 *
 * 24/09/2009
 */
public class CreateModule4MappedEntities {
	
	
	private static int FMA2NCI=0;
	private static int FMA2SNOMED=1;
	private static int SNOMED2NCI=2;
	private static int NCI2MOUSE=3;
	private static int LUCADA=4;

	
	//String path = "file:/root/Work/ModulesMappings/";
	//String pathuri = "file:/C://Users//ernesto//EclipseWS//DataUMLS/";
	//String path = "C:/Users/ernesto/EclipseWS/DataUMLS/";
	
	String pathuri = "file:/home/ernesto/EclipseWS/DataUMLS/";
	String path = "/home/ernesto/EclipseWS/DataUMLS/";
	
	
	
	/** Files mapping uris from two different ontologies*/
	private String onto_mappingsFile;
	
	private OWLOntologyManager managerOWL = OWLManager.createOWLOntologyManager();
	//private OWLDataFactory datafactory = managerOWL.getOWLDataFactory(); 
	
	private OWLOntology onto1;
	private OWLOntology onto2;
	
	private OWLOntology moduleonto1;
	private OWLOntology moduleonto2;
	
	private String onto1uri;
	private String onto2uri;

	private String moduleonto1uri;
	private String moduleonto2uri;
	
	private String moduleonto1loguri;
	private String moduleonto2loguri;

	private ModuleExtractor moduleExtractor1;
	private ModuleExtractor moduleExtractor2;
	
	long init, fin;
	
	
	private Set<String> signatureStrOnto1 = new HashSet<String>();
	private Set<String> signatureStrOnto2 = new HashSet<String>();
	
	private Set<OWLEntity> signatureOnto1 = new HashSet<OWLEntity>();
	private Set<OWLEntity> signatureOnto2 = new HashSet<OWLEntity>();
	
	
	private ReadFile reader;
	
	
	/**
	 * 
	 *
	 */
	public CreateModule4MappedEntities(int assessmenOntos){
		
		try {
			
			if (assessmenOntos==FMA2NCI)
				setFMA_NCI_Files();
			else if (assessmenOntos==FMA2SNOMED)
				setFMA_SNOMED_Files();
			else if (assessmenOntos==SNOMED2NCI)
				setSNOMED_NCI_Files();
			else  if (assessmenOntos==NCI2MOUSE)
				setNCI_MOUSE_Files();
			else 
				SetLUCADA();
			
			//setFMA_NCI_Files();
			//setFMA_SNOMED_Files();
			//setSNOMED_NCI_Files();
			
			
			init=Calendar.getInstance().getTimeInMillis();
			loadOntologies();
			fin = Calendar.getInstance().getTimeInMillis();
			System.out.println("Time loading ontologies (s): " + (float)((double)fin-(double)init)/1000.0);
			
			init=Calendar.getInstance().getTimeInMillis();
			setUpModuleExtractors(true); //With annotations
			//setUpModuleExtractors(false);
			fin = Calendar.getInstance().getTimeInMillis();
			System.out.println("Time setting up module extractors (s): " + (float)((double)fin-(double)init)/1000.0);
			
			init=Calendar.getInstance().getTimeInMillis();
			getSignature();
			fin = Calendar.getInstance().getTimeInMillis();
			System.out.println("Time getting signature (s): " + (float)((double)fin-(double)init)/1000.0);
			
			init=Calendar.getInstance().getTimeInMillis();
			extractModules();
			fin = Calendar.getInstance().getTimeInMillis();
			System.out.println("Time extracting modules (s): " + (float)((double)fin-(double)init)/1000.0);
			
			init=Calendar.getInstance().getTimeInMillis();
			saveModules();
			fin = Calendar.getInstance().getTimeInMillis();
			System.out.println("Time saving modules (s): " + (float)((double)fin-(double)init)/1000.0);
			
			
		}
		catch (Exception e){
			e.printStackTrace();
		}
		
		
		
	}
	
	
	private void loadOntologies() throws Exception {		
		 onto1 = managerOWL.loadOntology(IRI.create(onto1uri));
		 onto2 = managerOWL.loadOntology(IRI.create(onto2uri));
		 
		 //onto1 = managerOWL.loadOntology(IRI.create(onto1uri));
		 //onto2 = managerOWL.loadOntology(IRI.create(onto2uri));
	}
	
	
	private void saveModules() throws Exception {
		
		managerOWL.saveOntology(moduleonto1, new RDFXMLOntologyFormat(), IRI.create(moduleonto1uri));
		managerOWL.saveOntology(moduleonto2, new RDFXMLOntologyFormat(), IRI.create(moduleonto2uri));
		
	}
	
	
	private void setUpModuleExtractors(boolean extractAnnotation){
		//Bottom locality, but extracting annotations
		moduleExtractor1 = new ModuleExtractor(onto1, false, false, false, extractAnnotation, !extractAnnotation);
		moduleExtractor2 = new ModuleExtractor(onto2, false, false, false, extractAnnotation, !extractAnnotation);
		
	}
	
	
	private void getSignature() throws Exception{
		
		
		//Read from file
		
		reader = new ReadFile(onto_mappingsFile);
		
		String line;
		String[] elements;
		
		line=reader.readLine();
		while (line!=null) {
			
			if (line.indexOf("|")<0){
				line=reader.readLine();
				continue;
			}
				
			elements=line.split("\\|");
			
			signatureStrOnto1.add(elements[0]);
			signatureStrOnto2.add(elements[1]);
			

			line=reader.readLine();
		}		
		
		reader.closeBuffer();
		
		
		//Get entity from ontology
		
		//UNCOMMENT or comment!!
		/**/for (OWLEntity ent : onto1.getSignature()){
			//System.out.println(ent.getIRI().toString());
			if (signatureStrOnto1.contains(ent.getIRI().toString())){
				signatureOnto1.add(ent);
			}
		}
		

		
		for (OWLEntity ent : onto2.getSignature()){
			if (signatureStrOnto2.contains(ent.getIRI().toString())){
				signatureOnto2.add(ent);
			}
		}
		
		
		//Uncomment or comment
		if (!(signatureStrOnto1.size()==signatureOnto1.size()) || 
			!(signatureStrOnto2.size()==signatureOnto2.size())){
			
			System.out.println(signatureStrOnto1.size() +  " " + signatureOnto1.size() +  "\n" +
					signatureStrOnto2.size() +  " " + signatureOnto2.size());
			
			//System.out.println(signatureStrOnto1);
			
		}
		
		
	}
	
	
	
	private void extractModules(){
		
		moduleonto1=moduleExtractor1.getLocalityModuleForSignatureGroup(signatureOnto1, moduleonto1loguri);
		
		moduleonto2=moduleExtractor2.getLocalityModuleForSignatureGroup(signatureOnto2, moduleonto2loguri);
		
		System.out.println("Module sizes. Onto 1: " + moduleonto1.getSignature().size());
		
		System.out.println("Module sizes. Onto 2: " + moduleonto2.getSignature().size());
		
	}
	
	
	
	private void setFMA_NCI_Files(){
		
		onto1uri= pathuri + "UMLS_Onto_Versions/FMADL_cleant_2_0.owl";
		onto2uri= pathuri + "UMLS_Onto_Versions/NCI_Thesaurus_08.05d_cleant.owl";
		
		//Output
		//moduleonto1uri= pathuri + "UMLS_Onto_Versions/FMA_module_mappings_nci.owl";
		//moduleonto2uri= pathuri + "UMLS_Onto_Versions/NCI_module_mappings_fma.owl";
		
		moduleonto1uri= pathuri + "UMLS_Onto_Versions/FMA_module_mappings_nci_revised.owl";
		moduleonto2uri= pathuri + "UMLS_Onto_Versions/NCI_module_mappings_fma_revised.owl";
		
		moduleonto1loguri="http://comlab.ox.ac.uk/modules/FMA_module_mappings_nci.owl";
		moduleonto2loguri="http://comlab.ox.ac.uk/modules/NCI_module_mappings_fma.owl";
		
		//input
		//onto_mappingsFile= path + "UMLS_source_data/onto_mappings_FMA_NCI.txt";
		onto_mappingsFile= path + "Results/resultsFMA2NCI/onto_mappings_FMA_NCI_revised_final.txt";
		
	}
	
	
	private void setSNOMED_NCI_Files(){
		
		//onto1uri= pathuri + "UMLS_Onto_Versions/snomed20090131_cleant.owl";
		//onto2uri= pathuri + "UMLS_Onto_Versions/NCI_Thesaurus_08.05d_cleant.owl";
		//We use the previous module for whole set of mappings
		onto1uri= pathuri + "UMLS_Onto_Versions/SNOMED_module_mappings_nci.owl";
		onto2uri= pathuri + "UMLS_Onto_Versions/NCI_module_mappings_snmd.owl";
		
		//moduleonto1uri= pathuri + "UMLS_Onto_Versions/SNOMED_module_mappings_nci.owl";
		//moduleonto2uri= pathuri + "UMLS_Onto_Versions/NCI_module_mappings_snmd.owl";
		//Revised
		moduleonto1uri= pathuri + "UMLS_Onto_Versions/SNOMED_module_mappings_nci_revised.owl";
		moduleonto2uri= pathuri + "UMLS_Onto_Versions/NCI_module_mappings_snmd_revised.owl";
		
		
		moduleonto1loguri="http://comlab.ox.ac.uk/modules/SNOMED_module_mappings_nci.owl";
		moduleonto2loguri="http://comlab.ox.ac.uk/modules/NCI_module_mappings_snmd.owl";
		
		//onto_mappingsFile=path + "UMLS_source_data/onto_mappings_SNOMED_NCI.txt";
		//Revised mappings
		onto_mappingsFile=path + "Results/resultsSNOMED2NCI/onto_mappings_SNOMED_NCI_revised_final.txt";
		
	}
	
	
	
	private void setFMA_SNOMED_Files(){
		
		
		//onto1uri= pathuri + "UMLS_Onto_Versions/FMADL_cleant_2_0.owl";
		//onto2uri="file:/root/Work/ModulesMappings/UMLS_Onto_Versions/snomed20090131_umls.owl";
		//onto2uri= pathuri + "UMLS_Onto_Versions/snomed20090131_cleant.owl";
		
		//We use the previous module for whole set of mappings
		onto1uri= pathuri + "UMLS_Onto_Versions/FMA_module_mappings_snmd.owl";
		onto2uri= pathuri + "UMLS_Onto_Versions/SNOMED_module_mappings_fma.owl";
		
		//moduleonto1uri= pathuri + "UMLS_Onto_Versions/FMA_module_mappings_snmd.owl";
		//moduleonto2uri= pathuri + "UMLS_Onto_Versions/SNOMED_module_mappings_fma.owl";
		//Revised
		moduleonto1uri= pathuri + "UMLS_Onto_Versions/FMA_module_mappings_snmd_revised.owl";
		moduleonto2uri= pathuri + "UMLS_Onto_Versions/SNOMED_module_mappings_fma_revised.owl";
		
		
		moduleonto1loguri="http://comlab.ox.ac.uk/modules/FMA_module_mappings_snmd.owl";
		moduleonto2loguri="http://comlab.ox.ac.uk/modules/SNOMED_module_mappings_fma.owl";
		
		
		//onto_mappingsFile=path + "UMLS_source_data/onto_mappings_FMA_SNOMED.txt";
		//Revised mappings
		onto_mappingsFile=path + "Results/resultsFMA2SNOMED/onto_mappings_FMA_SNOMED_revised_final.txt";
		
	}
	
	private void setNCI_MOUSE_Files(){
		
		
		//onto1uri= pathuri + "UMLS_Onto_Versions/FMADL_cleant_2_0.owl";
		//onto2uri="file:/root/Work/ModulesMappings/UMLS_Onto_Versions/snomed20090131_umls.owl";
		//onto2uri= pathuri + "UMLS_Onto_Versions/snomed20090131_cleant.owl";
		
		//We use the previous module for whole set of mappings
		onto2uri= pathuri + "UMLS_Onto_Versions/Anatomy/nci_anatomy_2010.owl";
		onto1uri= pathuri + "UMLS_Onto_Versions/Anatomy/mouse_anatomy_2010.owl";
		


		moduleonto2uri= pathuri + "UMLS_Onto_Versions/Anatomy/NCIAn_module_mappings_mouse.owl";
		moduleonto1uri= pathuri + "UMLS_Onto_Versions/Anatomy/MOUSE_module_mappings_ncian.owl";
		
		
		moduleonto2loguri="http://comlab.ox.ac.uk/modules/NCIAn_module_mappings_mouse.owl";
		moduleonto1loguri="http://comlab.ox.ac.uk/modules/MOUSE_module_mappings_ncian.owl";
		
		
				
		onto_mappingsFile=path + "UMLS_Onto_Versions/Anatomy/GSAll_Anatomy_2010.txt";
		
	}
	
	private void SetLUCADA(){
		
		
	
		
		onto1uri= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/LUCADA/TestMarch/LUCADAOntology17January2013.owl";
		onto2uri= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/LUCADA/snomed20110131_replab.owl.zip";
		
		


		moduleonto1uri= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/LUCADA/TestMarch/module_lucada_interact.owl";
		moduleonto2uri= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/LUCADA/TestMarch/module_snomed_interact.owl";
		
		
		moduleonto2loguri="http://www.ihtsdo.org/snomed";
		moduleonto1loguri="http://www.semanticweb.org/ontologies/2011/3/LUCADAOntology.owl";
		
		
				
		//onto_mappingsFile = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/LUCADA/TestMarch/mappings2.txt";
		onto_mappingsFile = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/LUCADA/TestMarch/mappings_interactivity2.txt";
		
	}
	
	
	
	
	
	 public static void main(String[] args) {
		 if (args.length==1){
			 new CreateModule4MappedEntities(Integer.valueOf(args[0]));
		 }
		 else{
			 new CreateModule4MappedEntities(LUCADA);
		 }

	 }

}
