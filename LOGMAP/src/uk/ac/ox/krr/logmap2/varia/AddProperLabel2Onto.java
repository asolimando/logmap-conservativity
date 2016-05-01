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

import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.AddAxiom;

import uk.ac.ox.krr.logmap2.utilities.Utilities;


public class AddProperLabel2Onto {

	
	
	private OWLOntologyManager managerOnto;
	private OWLDataFactory factory;
	private OWLOntology onto;
	
	String onto_iri_new;
	String onto_iri;
	
	
	private String rdf_label_uri = "http://www.w3.org/2000/01/rdf-schema#label";
	
	
	
	public AddProperLabel2Onto(String iri_in, String iri_out) throws Exception {
		
		onto_iri=iri_in;
		onto_iri_new=iri_out;
		
		loadOntology();
		
		processClassNames();
		
		saveOntology();
		
		
	}
	
	
	private void saveOntology() throws Exception{
		managerOnto.saveOntology(onto, new RDFXMLOntologyFormat(), IRI.create(onto_iri_new));
	}
	
	
	
	private void loadOntology() throws Exception {		

		managerOnto = OWLManager.createOWLOntologyManager();
		
		factory = managerOnto.getOWLDataFactory();
	
		//If import cannot be loaded
		managerOnto.setSilentMissingImportsHandling(true);
		
		onto = managerOnto.loadOntology(IRI.create(onto_iri));
		
		 
		//signatureOnto = onto.getClassesInSignature();
		//signatureOnto1.add(factory.getOWLNothing());
		 
	}
	
	
	
	//Process class names and creates proper labels
	private void processClassNames(){
		
		List<OWLOntologyChange> listchanges= new ArrayList<OWLOntologyChange>();
		
		String cls_name;
		String final_label;
		
		int charInit;
		
		boolean lowercase;
		
		for (OWLClass cls : onto.getClassesInSignature()){
			
			cls_name = Utilities.getEntityLabelFromURI(cls.getIRI().toString());
			
			if (cls_name.indexOf("_Class")>0){
				
				cls_name=cls_name.split("_Class")[0];
			}
			
			
			charInit=0;
			final_label="";
			
			lowercase=false;
			for (int i=1; i<cls_name.length(); i++){
				
				if (cls_name.getBytes()[i]>=97 && cls_name.getBytes()[i]<=122) //lowercase, we force lowecase in the middle
					lowercase=true;
				
				if (cls_name.getBytes()[i]>=65 && cls_name.getBytes()[i]<=90 && lowercase){ //Uppercase, avoid acronyms
					
					final_label+=cls_name.substring(charInit, i) + "_";
					charInit=i;
					lowercase=false;
					
				}
				
			}
			
			
			final_label+=cls_name.substring(charInit); //From there to the end
			
			//System.out.println(cls_name);
			//System.out.println(final_label);
			
			
			//factory.getOWLAnnotationProperty(IRI.create(rdf_label_uri)
			//factory.getOWLAnnotationAssertionAxiom(factory.getRDFSLabel(), 
			//				clsgetIRI(), factory.getOWLLiteral(cls_name));
			
			listchanges.add(new AddAxiom(onto,
					factory.getOWLAnnotationAssertionAxiom(
							cls.getIRI(), 
							factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(final_label)))
					));
			
		}
		
		
		managerOnto.applyChanges(listchanges);
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		try {
			new AddProperLabel2Onto(
					"file:/c://Users/ernesto/EclipseWS/DataUMLS/UMLS_Onto_Versions/OntosRobotics/ChemoSupportv2.owl",
					"file:/c://Users/ernesto/EclipseWS/DataUMLS/UMLS_Onto_Versions/OntosRobotics/ChemoSupportv2_labels.owl");
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}

}
