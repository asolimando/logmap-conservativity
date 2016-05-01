/*******************************************************************************
 * Copyright 2012 by the Department of Computer Science (Universit	y of Oxford)
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

//import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.DLExpressivityChecker;

import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.AddAxiom;

import org.semanticweb.owlapi.profiles.OWLProfileReport;
import org.semanticweb.owlapi.profiles.OWLProfileViolation;


import uk.ac.ox.krr.logmap2.io.LogOutput;
import uk.ac.ox.krr.logmap2.owlapi.SynchronizedOWLManager;
import uk.ac.ox.krr.logmap2.reasoning.profiles.CheckOWL2Profile;
import uk.ac.ox.krr.logmap2.utilities.Utilities;

import java.util.Calendar;
import java.util.List;
import java.util.HashSet;
import java.util.Set;



/**
 * This class will manage the loaded ontology
 * 
 *
 * @author Ernesto Jimenez-Ruiz
 * Created: Sep 6, 2011
 *
 */
public class OntologyLoader {
	
	
	protected OWLDataFactory dataFactory;
	protected OWLOntologyManager managerOnto;
	protected OWLOntology onto;
	
	protected String iri_onto_str;
	
	protected int size_signature;
	protected int size_classes;
	//protected int size_axioms;
	
	protected Set<OWLAxiom> axiomSet = new HashSet<OWLAxiom>();
	
	private String DLNameOnto;
	
	private CheckOWL2Profile profileChecker;
	
	private boolean inOWL2DL;
	private boolean inOWL2EL;
	
	//We only report violations for OWL 2 DL
	//private List<OWLProfileViolation> listViolatiosnOWL2DL;
	private OWLProfileReport owl2DLProfileReport;
	
	
	
	public OntologyLoader(String phy_iri_onto) throws OWLOntologyCreationException{
		//managerOnto = OWLManager.createOWLOntologyManager();
		managerOnto = SynchronizedOWLManager.createOWLOntologyManager();
		dataFactory=managerOnto.getOWLDataFactory();
		loadOWLOntology(phy_iri_onto);		
	}
	
	
	public OntologyLoader(OWLOntology given_onto) throws OWLOntologyCreationException{
		//managerOnto = OWLManager.createOWLOntologyManager();
		managerOnto = SynchronizedOWLManager.createOWLOntologyManager();
		dataFactory=managerOnto.getOWLDataFactory();
		setOWLOntology(given_onto);		
	}

	
	
	private String getURIFromClasses(){
		for (OWLClass cls : onto.getClassesInSignature()){
			return Utilities.getNameSpaceFromURI(cls.getIRI().toString());
		}
		
		//Just in case we return default IRI
		return "http://logmap.cs.ox.ac.uk/ontology.owl";
		
	}
	
	
	/**
	 * Sets the given OWLOntology to be used by LogMap 
	 * @param given_onto
	 * @throws OWLOntologyCreationException
	 */
	public void setOWLOntology(OWLOntology given_onto) throws OWLOntologyCreationException{
	
		try {
			
			
			managerOnto.setSilentMissingImportsHandling(true);
									
			onto = managerOnto.createOntology(given_onto.getAxioms());
			
			if (onto.getOntologyID().getOntologyIRI()!=null){
				iri_onto_str=onto.getOntologyID().getOntologyIRI().toString(); //Give this iri to module
			}
			else {
				iri_onto_str=getURIFromClasses();
			}
			
			
			LogOutput.print("IRI: " + iri_onto_str);
			
					
			
			
			size_signature = onto.getSignature(true).size();
			size_classes = onto.getClassesInSignature(true).size();
			
			
			//We add dummy axiom
			if (size_classes==0){
				addDummyAxiom2Ontology();
			}
		}	
		
		catch(Exception e){
			System.err.println("Error creating OWL ontology 4 LogMap: " + e.getMessage());
			//e.printStackTrace();
			throw new OWLOntologyCreationException();
		}
		
		
	}
	
	
	public void loadOWLOntology(String phy_iri_onto) throws OWLOntologyCreationException{		

		try {
			
			//If import cannot be loaded
			//TODO: deprecated??
			managerOnto.setSilentMissingImportsHandling(true);
									
			onto = managerOnto.loadOntology(IRI.create(phy_iri_onto));
			
			
			//The preclassification with condor has no ontology id
			if (onto.getOntologyID().getOntologyIRI()!=null){
				iri_onto_str=onto.getOntologyID().getOntologyIRI().toString(); //Give this iri to module
			}
			else {
				iri_onto_str=getURIFromClasses();
			}
			
			
			LogOutput.print("IRI: " + iri_onto_str);
			
					
			
			
			size_signature = onto.getSignature(true).size();
			size_classes = onto.getClassesInSignature(true).size();
			
			
			//We add dummy axiom
			if (size_classes==0){
				addDummyAxiom2Ontology();
			}
			
			
			//LogOutput.print(iri_onto);
			
			//EXPRESSIVITY ontology			
			//Used in webservice
			try{
				Set<OWLOntology> importsClosure = managerOnto.getImportsClosure(onto);        
		        DLExpressivityChecker checker = new DLExpressivityChecker(importsClosure);
		        DLNameOnto = checker.getDescriptionLogicName();
	
				profileChecker = new CheckOWL2Profile(onto);
				
				owl2DLProfileReport = profileChecker.getReport4OWL2DL();
				
				if (!owl2DLProfileReport.isInProfile()){
					inOWL2DL=false;
					inOWL2EL=false;
				}
				else{
					inOWL2DL=true;
					//In DL but may be not in EL
					inOWL2EL = profileChecker.getReport4OWL2EL().isInProfile();
				}
			}
			catch (Exception e){
				LogOutput.printError("Error checking DL expressivity: " + e.getMessage());
			}
			
			
			
		}
		catch(Exception e){
			System.err.println("Error loading OWL ontology: " + e.getMessage());
			//e.printStackTrace();
			throw new OWLOntologyCreationException();
		}
	}
	
	
	public String getDLNameOntology(){
		return DLNameOnto;
	}
	
	
	public boolean isOntologyInOWL2DL(){
		return inOWL2DL;
	}
	
	public boolean isOntologyInOWL2EL(){
		return inOWL2EL;
	}
	
	public List<OWLProfileViolation> getOWL2DLProfileViolation(){
		return owl2DLProfileReport.getViolations();
	}
	
	
	
	public void addDummyAxiom2Ontology(){
		
		OWLClass dummycls = dataFactory.getOWLClass(IRI.create("http://logmap.cs.ox.ac.uk/ontologies#TopClass"));
		
		
		managerOnto.applyChange(				
				new AddAxiom(
						onto, 
						dataFactory.getOWLDeclarationAxiom(dummycls)));
		
		
		managerOnto.applyChange(				
				new AddAxiom(
						onto, 
						dataFactory.getOWLSubClassOfAxiom(
								dummycls,
								dataFactory.getOWLThing())));
	}
	
	
	
	
	public void createAxiomSet(){
		
		//TODO Do not delete any of these lines		
		axiomSet.addAll(onto.getAxioms());  //Add All axioms. This line also includes annotations.
		axiomSet.addAll(onto.getTBoxAxioms(true));//also imports closure...
		axiomSet.addAll(onto.getABoxAxioms(true));
		axiomSet.addAll(onto.getRBoxAxioms(true));
		
		//Original size
		//size_axioms = axiomSet.size();
		
	}
	
	public void clearAxiomSet(){
		axiomSet.clear();
	}
	
	public Set<OWLAxiom> getAxiomSet(){
		return axiomSet;
	}
	
	
	public void clearOntology(){
		
		managerOnto.removeOntology(onto);
		onto=null;
		managerOnto=null;
	}
	
	
	public OWLOntologyManager getOWLOntologyManager(){
		return managerOnto;
	}
	
	public OWLOntology getOWLOntology(){
		return onto;
	}
	
	public String getOntologyIRIStr(){
		return iri_onto_str;
	}
	
	public IRI getOntologyIRI(){
		return IRI.create(iri_onto_str);
	}
	
	
	public Set<OWLClass> getClassesInSignature(){
		return onto.getClassesInSignature(true);//With imports!!
	}
	
	public int getSignatureSize(){
		return size_signature;
	}
	
	public int getClassesInSignatureSize(){
		return size_classes;
	}
	
	public void applyChanges(List<OWLOntologyChange> listchanges){
		managerOnto.applyChanges(listchanges);
	}
	
	public OWLDataFactory getDataFactory(){
		return dataFactory;
	}
	
	
	public void saveOntology(String phy_iri_onto) throws Exception{
		
		managerOnto.saveOntology(onto, new RDFXMLOntologyFormat(), IRI.create(phy_iri_onto));
		
	}
	
	
	
}
