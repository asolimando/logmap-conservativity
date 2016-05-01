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

import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLClass;

import uk.ac.ox.krr.logmap2.OntologyLoader;

import uk.ac.ox.krr.logmap2.io.WriteFile;

import java.util.*;


public class LUCADA_processing {

	OntologyLoader loader;
	
	String rdf_label_uri = "http://www.w3.org/2000/01/rdf-schema#label";
	String rdf_comment_uri = "http://www.w3.org/2000/01/rdf-schema#comment";
	String purl_identifier = "http://purl.org/dc/elements/1.1/identifier";
	String purl_desc = "http://purl.org/dc/elements/1.1/description";
	
	
	WriteFile writer;
	
	
	public LUCADA_processing() throws Exception{
		
		loader = new OntologyLoader("file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/LUCADA/LUCADAOntology15September2011.owl");
		
		String iri_annotation;
		String label_value;
		
		Set<String> classes2add=new HashSet<String>();
		Set<String> ids2add=new HashSet<String>();
		
		String ide;
		
		writer = new WriteFile("/usr/local/data/DataUMLS/UMLS_Onto_Versions/LUCADA/references_to_snomed.txt");
		
		System.out.println(loader.getClassesInSignature().size());
		
		for (OWLClass cls : loader.getClassesInSignature()){
		
			//Otherwise We look for label first (if no label we keepID)
			for (OWLAnnotationAssertionAxiom annAx : cls.getAnnotationAssertionAxioms(loader.getOWLOntology())){
				
				iri_annotation = annAx.getAnnotation().getProperty().getIRI().toString();
				
				if (iri_annotation.equals(rdf_label_uri) || 
						iri_annotation.equals(rdf_comment_uri) || 
						iri_annotation.equals(purl_identifier) ||
						iri_annotation.equals(purl_desc)){
					
					
					
					//LogOutput.print(((OWLLiteral)annAx.getAnnotation().getValue()).getLiteral());
					label_value=((OWLLiteral)annAx.getAnnotation().getValue()).getLiteral();//.toLowerCase();
					
					//if (label_value.startsWith("SNOMED-CT Concept ID:") ||
					//		label_value.startsWith("SNOMED ID:")){						
					//	classes2add.add(cls.getIRI().toString());	
					//}
					if (label_value.startsWith("SNOMED-CT Concept ID: ")){
						ide = label_value.split(": ")[1];
						writer.writeLine("http://www.ihtsdo.org/snomed#" + ide + "|" + cls.getIRI().toString());
						
					}
					
				}
	
			}
		}
		
		
		//for (String iri_str : classes2add){
		//	writer.writeLine(iri_str);
		//}
		
		writer.closeBuffer();
		
		
		
	}
	
	
	public static void main(String[] args) {
		try{
			new LUCADA_processing();
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}
	
}
