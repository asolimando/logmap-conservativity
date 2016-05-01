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
import java.util.*;

import org.semanticweb.owlapi.apibinding.OWLManager;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyManager;



public class TestFrancisco {

	OWLOntology onto;
	OWLDataFactory factory;
	OWLOntologyManager managerOnto;
	
	
	public TestFrancisco(){
		
		loadOWLOntology("file:/home/ernesto/ontologies/pizza.owl");
		print();
	}
	
	
	public void loadOWLOntology(String phy_iri_onto) {		

		try {
			managerOnto = OWLManager.createOWLOntologyManager();
			
			managerOnto.setSilentMissingImportsHandling(true);
			
			factory = managerOnto.getOWLDataFactory();
			
			onto = managerOnto.loadOntology(IRI.create(phy_iri_onto));
			
			
			
		}
		catch(Exception e){
			System.err.println("Error loading OWL ontology: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	public void print(){
		
		

		Set<OWLAxiom> intersection = new HashSet<OWLAxiom>(onto.getTBoxAxioms(true));
		intersection.retainAll(onto.getRBoxAxioms(true));
		
		//for (OWLAxiom ax : intersection){
		//	System.out.println(ax);
		//}
		
		System.out.println(onto.getOntologyID().getOntologyIRI().toString());
				System.out.println("Axioms: " + onto.getAxiomCount());
				System.out.println("Axioms: " + onto.getAxioms().size());
				System.out.println("Tbox Axioms: " + onto.getTBoxAxioms(true).size());
				System.out.println("Abox Axioms: " + onto.getABoxAxioms(true).size());
				System.out.println("RBox Axioms: " + onto.getRBoxAxioms(true).size());
				System.out.println("Logical Axioms: " + onto.getLogicalAxiomCount());
				System.out.println("Logical Axioms: " + onto.getLogicalAxioms().size());
				System.out.println("Annotations: " + onto.getAnnotations().size());
				
				int annotations=0;
				int ref_axioms=0;
				Set<OWLAxiom> refaxioms = new HashSet<OWLAxiom>();
				Set<OWLAxiom> annotationsSet = new HashSet<OWLAxiom>();
				for (OWLEntity ent : onto.getSignature()){
					annotations+=ent.getAnnotationAssertionAxioms(onto).size();
					annotationsSet.addAll(ent.getAnnotationAssertionAxioms(onto));
					refaxioms.addAll(onto.getReferencingAxioms(ent));
					ref_axioms+=onto.getReferencingAxioms(ent).size();
				}
				int nothing=0;
				int thing=0;
				for (OWLAxiom ax : refaxioms){
					
					if (ax.getSignature().contains(factory.getOWLNothing())){
						nothing++;
					}
					if (ax.getSignature().contains(factory.getOWLThing())){
						thing++;
					}
				}
				
				
				Set<OWLAxiom> declaration = new HashSet<OWLAxiom>(onto.getAxioms());
				declaration.removeAll(onto.getLogicalAxioms());
				declaration.removeAll(annotationsSet);
				System.out.println("Declaration Axioms: " + declaration.size());
				for (OWLAxiom ax : declaration){
					System.out.println(ax);
				}
				
				
				System.out.println("Annotation Axioms: " + annotations);
				System.out.println("Referenced Axioms: " + ref_axioms);
				System.out.println("Referenced Axioms 2: " + refaxioms.size());
				System.out.println("Axioms with nothing: " + nothing);
				System.out.println("Axioms with thing: " + thing);
				
				
				//We delete logical and annotation axioms
				//refaxioms.removeAll(onto.getTBoxAxioms(true));
				//refaxioms.removeAll(onto.getRBoxAxioms(true));
				//refaxioms.removeAll(onto.getABoxAxioms(true));
				//refaxioms.removeAll(onto.getLogicalAxioms());
				//refaxioms.removeAll(annotationsSet);
				
				//System.out.println("?? Axioms: " + refaxioms.size());
				//for (OWLAxiom ax : refaxioms){
				//	System.out.println(ax);
				//}
	}
	
	public static void main(String[] args) {
		
		new TestFrancisco();
		
	}
	
	
	
}
