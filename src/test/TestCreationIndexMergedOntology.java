/*******************************************************************************
 * Copyright 2016 by the Department of Computer Science (University of Genova and University of Oxford)
 * 
 *    This file is part of LogMapC an extension of LogMap matcher for conservativity principle.
 * 
 *    LogMapC is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 * 
 *    LogMapC is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 * 
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with LogMapC.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import org.semanticweb.owlapi.reasoner.FreshEntityPolicy;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.util.InferredAxiomGenerator;
import org.semanticweb.owlapi.util.InferredEquivalentClassAxiomGenerator;
import org.semanticweb.owlapi.util.InferredOntologyGenerator;
import org.semanticweb.owlapi.util.InferredSubClassAxiomGenerator;

import uk.ac.ox.krr.logmap2.OntologyLoader;
import uk.ac.ox.krr.logmap2.Parameters;

import uk.ac.ox.krr.logmap2.indexing.JointIndexManager;
import uk.ac.ox.krr.logmap2.indexing.OntologyProcessing;
import uk.ac.ox.krr.logmap2.io.LogOutput;
import uk.ac.ox.krr.logmap2.lexicon.LexicalUtilities;
import uk.ac.ox.krr.logmap2.owlapi.SynchronizedOWLManager;

public class TestCreationIndexMergedOntology {
		
	protected OWLOntology mergedOntology;
	protected OWLOntologyManager ontoManager;
	protected OWLDataFactory datafactory;

	protected OWLReasoner reasoner;
	protected OWLReasonerFactory reasonerFactory;
	
	
	String task;
	String onto1;
	String onto2;
	

	
	OntologyLoader loader1;
	OntologyLoader loader2;
	OntologyLoader loaderMapping;


	String reference;
	
	String path_ontos;
	String path_ontos_uri;
	String path_align_uri;
	
	OWLOntology inferredOnt;
	
	
	
	public TestCreationIndexMergedOntology() throws OWLOntologyCreationException{
		
		
		loadMergedOntology();
		
		Logger.getLogger("org.semanticweb.elk").setLevel(Level.OFF);
		
		reasonerFactory = new ElkReasonerFactory();	
		OWLReasonerConfiguration c = new SimpleConfiguration(FreshEntityPolicy.ALLOW,60*3);
		reasoner = reasonerFactory.createReasoner(mergedOntology,c);
		
		createClassificationOntology();
		
		processMergedOntologyAndCreateIndex();
		
		
		
	}
	
	
	private void processMergedOntologyAndCreateIndex() {
		
		Parameters.reasoner = Parameters.hermit;
		
		JointIndexManager index = new JointIndexManager();
		
		OntologyProcessing onto_process = new OntologyProcessing(inferredOnt, index, new LexicalUtilities());
		
		
		onto_process.precessLexicon(false); //we process labels if "useLogMapConfidences"

		onto_process.setTaxonomicData();
		
		index.setIntervalLabellingIndex(new HashMap<Integer, Set<Integer>>());
		
		
		
		
		
	}


	private void loadMergedOntology() throws OWLOntologyCreationException{
		task = "sn1";
		
		path_ontos = "/home/ale/data/oaei2013/largebio/";
		path_ontos_uri= "file:"+path_ontos+"onto/";
		path_align_uri= "file:"+path_ontos+"reference/";
		
		setTestCase();
		
		loader1 = new OntologyLoader(onto1);
		loader2 = new OntologyLoader(onto2);
		loaderMapping = new OntologyLoader(reference);
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
		axioms.addAll(loader1.getOWLOntology().getAxioms());
		axioms.addAll(loader2.getOWLOntology().getAxioms());
		axioms.addAll(loaderMapping.getOWLOntology().getAxioms());
				
		//OWLOntologyManager managerMerged = OWLManager.createOWLOntologyManager();
		OWLOntologyManager managerMerged = SynchronizedOWLManager.createOWLOntologyManager();
		mergedOntology = managerMerged.createOntology(axioms, IRI.create("http://krr.ox.cs.ac.uk/logmap2/integration.owl"));
		
		
		
	}
	
	protected void setTestCase(){
		
		
		if (task.startsWith("fn")){
			
			reference = "oaei2013_FMA2NCI_repaired_UMLS_mappings.owl";
			
			if (task.equals("fn1")){
				onto1 = "oaei2013_FMA_small_overlapping_nci.owl";
				onto2 = "oaei2013_NCI_small_overlapping_fma.owl";
			}
			else{
				onto1 = "oaei2013_FMA_whole_ontology.owl";
				onto2 = "oaei2013_NCI_whole_ontology.owl";
			}
			
		}
		
		else if (task.startsWith("fs")){
			
			reference = "oaei2013_FMA2SNOMED_repaired_UMLS_mappings.owl";
			
			if (task.equals("fs1")){
				onto1 = "oaei2013_FMA_small_overlapping_snomed.owl";
				onto2 = "oaei2013_SNOMED_small_overlapping_fma.owl";
			}
			else{
				onto1 = "oaei2013_FMA_whole_ontology.owl";
				onto2 = "oaei2013_SNOMED_extended_overlapping_fma_nci.owl";
			}
			
		}
		
		else{
			
			reference = "oaei2013_SNOMED2NCI_repaired_UMLS_mappings.owl";
			
			if (task.equals("sn1")){
				onto1 = "oaei2013_SNOMED_small_overlapping_nci.owl";
				onto2 = "oaei2013_NCI_small_overlapping_snomed.owl";
			}
			else{
				onto1 = "oaei2013_SNOMED_extended_overlapping_fma_nci.owl";
				onto2 = "oaei2013_NCI_whole_ontology.owl";
			}
			
		}
		
		
		reference = path_align_uri + reference;
		onto1 = path_ontos_uri + onto1;
		onto2 = path_ontos_uri + onto2;
		
		
	}
	
	
	/**
	 * This closure will involve subclass axioms
	 */
	private void createClassificationOntology(){
				
		try {
			
		       
           //OWLOntologyManager classifiedOntoMan = OWLManager.createOWLOntologyManager();
		   OWLOntologyManager classifiedOntoMan = SynchronizedOWLManager.createOWLOntologyManager();
		   																 
           inferredOnt = classifiedOntoMan.createOntology(mergedOntology.getOntologyID().getOntologyIRI());
           InferredOntologyGenerator ontGen = new InferredOntologyGenerator(
        		   reasoner, new ArrayList<InferredAxiomGenerator<? extends OWLAxiom>>());
           //InferredOntologyGenerator ontGen = new InferredOntologyGenerator(reasoner);
           
           
           ontGen.addGenerator(new InferredEquivalentClassAxiomGenerator());
           ontGen.addGenerator(new InferredSubClassAxiomGenerator());
           
           //Fills inferred onto
           ontGen.fillOntology(classifiedOntoMan, inferredOnt);
         
           
           System.out.println("Closure:\n" + inferredOnt.getAxiomCount() +  "  "  + inferredOnt.getSignature().size());
           
           classifiedOntoMan.saveOntology(inferredOnt, new RDFXMLOntologyFormat(), IRI.create(path_ontos_uri + "SNOMED_NCI_Mappings.owl"));
           
           
   		   //OTHER GENERATORS
   		   //ontGen.addGenerator(new InferredClassAssertionAxiomGenerator());
   		   //ontGen.addGenerator(new InferredPropertyAssertionGenerator());
   		   //Original computational cost is really high! With extension we can extract only eplicit disjointness	   		   
           //ontGen.addGenerator(new InferredDisjointClassesAxiomGenerator());
           
           //ontGen.addGenerator(new InferredDataPropertyCharacteristicAxiomGenerator());	           
           //ontGen.addGenerator(new InferredEquivalentDataPropertiesAxiomGenerator());
           //ontGen.addGenerator(new InferredSubDataPropertyAxiomGenerator());
       
           //ontGen.addGenerator(new InferredEquivalentObjectPropertyAxiomGenerator());
           //ontGen.addGenerator(new InferredInverseObjectPropertiesAxiomGenerator());
           //ontGen.addGenerator(new InferredObjectPropertyCharacteristicAxiomGenerator());
           //ontGen.addGenerator(new InferredSubObjectPropertyAxiomGenerator());
           
	           
	       }
	       catch (Exception e) {
	           e.printStackTrace();
	           //return new ArrayList<OWLAxiom>();
	       }
		
		
		
	}
	
	
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		try {
			new TestCreationIndexMergedOntology();
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}