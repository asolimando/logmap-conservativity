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

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import uk.ac.manchester.syntactic_locality.ModuleExtractor;

public class createModulesOAEI {
	
	String path = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/";
	String fma2nci = "fma2nci_dataset/";
	String fma2snmd = "fma2snmd_dataset/";
	String snmd2nci = "snmd2nci_dataset/";
	
	String out ="oaei_2012/";
	
	private OWLOntologyManager managerOWL1 = OWLManager.createOWLOntologyManager();
	private OWLOntologyManager managerOWL2 = OWLManager.createOWLOntologyManager(); 
	private OWLOntologyManager managerOWL3 = OWLManager.createOWLOntologyManager(); 
	
	
	private OWLOntology onto_whole;
	private OWLOntology onto_sig;
	private OWLOntology onto_sig2;
	

	private OWLOntology module;


	private ModuleExtractor moduleExtractor;

	
	private String str_onto_whole;
	private String str_onto_sig;
	private String str_onto_sig2;
	
	
	private String str_onto_out;
	
	
	public createModulesOAEI(){
		
		try{
			
			//setModuleSmallNCI_snmd();
			//setModuleExtendedNCI_snmd();
			
			//setModuleSmallFMA_snmd();
			//setModuleExtendedFMA_snmd();
			
			setModuleSNOMED_extended();
			
			
			loadOntologies();
			
			setUpModuleExtractors();
			
			extractModules();
			
			saveModules();
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void setModuleSmallNCI_snmd(){
		
		str_onto_whole = path + fma2nci + "oaei2012_NCI_whole_ontology.owl";
		str_onto_sig = path + snmd2nci + "oaei2012_NCI_small_overlapping_snomed.owl";
		str_onto_out = path + out + "oaei2012_NCI_small_overlapping_snomed.owl";
		
	}
	
	public void setModuleExtendedNCI_snmd(){
		
		str_onto_whole = path + fma2nci + "oaei2012_NCI_whole_ontology.owl";
		str_onto_sig = path + snmd2nci + "oaei2012_NCI_big_overlapping_snomed.owl";
		str_onto_out = path + out + "oaei2012_NCI_extended_overlapping_snomed.owl";
		
	}
	
	
	public void setModuleSmallFMA_snmd(){
		
		str_onto_whole = path + fma2nci + "oaei2012_FMA_whole_ontology.owl";
		str_onto_sig = path + fma2snmd + "oaei2012_FMA_small_overlapping_snomed.owl";
		str_onto_out = path + out + "oaei2012_FMA_small_overlapping_snomed.owl";
		
	}
	
	public void setModuleExtendedFMA_snmd(){
		
		str_onto_whole = path + fma2nci + "oaei2012_FMA_whole_ontology.owl";
		str_onto_sig = path + fma2snmd + "oaei2012_FMA_big_overlapping_snomed.owl";
		str_onto_out = path + out + "oaei2012_FMA_extended_overlapping_snomed.owl";
		
	}
	
	
	public void setModuleSNOMED_extended(){
		
		
		str_onto_whole = path + "snomed20090131_replab.owl";
		
		str_onto_sig = path + fma2snmd + "oaei2012_SNOMED_big_overlapping_fma.owl";
		str_onto_sig2 = path + snmd2nci + "oaei2012_SNOMED_small_overlapping_nci.owl";
		
		str_onto_out = path + out + "oaei2012_SNOMED_extended_overlapping_fma_nci.owl";
		
		
	}
	
	
	
	
	
	private void loadOntologies() throws Exception {		
		 onto_whole = managerOWL1.loadOntology(IRI.create(str_onto_whole));
		 onto_sig = managerOWL2.loadOntology(IRI.create(str_onto_sig));
		 onto_sig2 = managerOWL3.loadOntology(IRI.create(str_onto_sig2));
		 
		 //onto1 = managerOWL.loadOntology(IRI.create(onto1uri));
		 //onto2 = managerOWL.loadOntology(IRI.create(onto2uri));
	}
	
	
	
	
	private void saveModules() throws Exception {
		
		managerOWL1.saveOntology(module, new RDFXMLOntologyFormat(), IRI.create(str_onto_out));
		
	}
	
	
	
	private void setUpModuleExtractors(){
		//Bottom locality, but extracting annotations
		moduleExtractor = new ModuleExtractor(onto_whole, false, false, false, true, false);
		
		
	}
	
	
	private void extractModules(){
		
		Set<OWLEntity> signature = new HashSet<OWLEntity>();
		
		signature.addAll(onto_sig.getSignature());
		signature.addAll(onto_sig2.getSignature());
		
		module = moduleExtractor.getLocalityModuleForSignatureGroup(
				signature,//onto_sig.getSignature(), 
				onto_whole.getOntologyID().getOntologyIRI().toString());
		
		System.out.println("Module size. Sig: " + module.getSignature().size());

		System.out.println("Module size. Classes: " + module.getClassesInSignature().size());
	}
	
	
	
	
	
	
	public static void main(String[] args) {
		 
		 new createModulesOAEI();

	 }

}
