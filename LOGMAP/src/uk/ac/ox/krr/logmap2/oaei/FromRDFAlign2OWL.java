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
package uk.ac.ox.krr.logmap2.oaei;

import java.io.File;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;
import uk.ac.ox.krr.logmap2.oaei.reader.RDFAlignReader;
import uk.ac.ox.krr.logmap2.utilities.Utilities;
import uk.ac.ox.krr.logmap2.io.LogOutput;
import uk.ac.ox.krr.logmap2.io.WriteFile;


/**
 * This class transforms a RDF alignment (XML) file into OWL format and plain text format.
 * @author root
 *
 */
public class FromRDFAlign2OWL {
	
	private RDFAlignReader RDF_mappings_reader;
	
	private OWLOntologyManager mappings_ontologyManager;
	private OWLOntology ontology1;
	private OWLOntology ontology2;
	
	private OWLOntology mappings_ontology; //mappings out
	
	private OWLDataFactory datafactory;
	
	
	
	private List<OWLOntologyChange> owl_changes = new ArrayList<OWLOntologyChange>();
	
	private WriteFile writer;
	
		
	private String mappings_rdf_file_name;
	private String mappings_owl_file_name;
	private String mappings_txt_file_name;
	
	
	private String mappings_IRI="http://www.cs.ox.ac.uk/ontologies/oaei/owl/mappings.owl";
	
	
	/**
	 * Constructors for reference alignment only involving instances
	 * @param mappings_file_name
	 * @throws Exception
	 */
	public FromRDFAlign2OWL(String mappings_file_name) throws Exception{
		this(null, null, mappings_file_name, true);		
	}
	
	
	
	
	public FromRDFAlign2OWL(String onto_iri1, String onto_iri2, String mappings_file_name) throws Exception{		
		this(loadOntologyStatic(onto_iri1), loadOntologyStatic(onto_iri2), mappings_file_name, false);		
	}
	
	
	public FromRDFAlign2OWL(OWLOntology onto1, OWLOntology onto2, String mappings_file_name) throws Exception{
		this(onto1, onto2, mappings_file_name, false);
	}
	
	
	public FromRDFAlign2OWL(OWLOntology onto1, OWLOntology onto2, String mappings_file_name, boolean only_individuals) throws Exception{
		
		//Init file names
		String mappings_file;
		
		mappings_file = mappings_file_name;		
		mappings_rdf_file_name = mappings_file_name;
		
				
		//indexOf
		if (mappings_rdf_file_name.lastIndexOf(".")>0){			
			 mappings_file = mappings_rdf_file_name.split("\\.")[0];
		}
		
		mappings_owl_file_name = "file:" + mappings_file + ".owl";
		mappings_txt_file_name = mappings_file + ".txt";
				
		
		
		//Ontologies
		ontology1 = onto1;
		ontology2 = onto2;
		
		
		//Init writer text file mappings
		writer = new WriteFile(mappings_txt_file_name);
		
		
		//Read mappings
		RDF_mappings_reader =  new RDFAlignReader(mappings_file_name);		
		RDF_mappings_reader.getMappingObjects();
		
		
		//Init output mappings ontology
		initOWLMappingsFile();
		
		
		
		//Convert mappings to OWL axioms: populate "owl_changes"
		convertMappings2OWL(only_individuals);
	
		
		
		//close writer text file mappings
		LogOutput.printAlways("Saving file: " + mappings_txt_file_name);
		writer.closeBuffer();
		
		
		//Save output owl mappings: save owl_changes
		LogOutput.printAlways("Saving file: " + mappings_owl_file_name);
		saveOWLMappingsFile();
		
		
		
		
	}
	
	private void initOWLMappingsFile() throws Exception {
		mappings_ontologyManager = OWLManager.createOWLOntologyManager();		
		mappings_ontology = mappings_ontologyManager.createOntology(IRI.create(mappings_IRI));
		
		datafactory=mappings_ontologyManager.getOWLDataFactory();
		
	}
	
	
	private void saveOWLMappingsFile() throws Exception {

		mappings_ontologyManager.applyChanges(owl_changes);		
		mappings_ontologyManager.saveOntology(mappings_ontology, new RDFXMLOntologyFormat(), IRI.create(mappings_owl_file_name));
		
	}
	
	
	
	private void addAxiom2Changes(OWLAxiom ax){
		
		owl_changes.add(new AddAxiom(mappings_ontology, ax));
		
	}
	
	

	
	private void convertMappings2OWL(boolean only_individuals){
		
		
		String iri_measure_str = mappings_IRI + "#measure";
		String iri_entity1_str = mappings_IRI + "#entity1";
		String iri_entity2_str = mappings_IRI + "#entity2";
		String iri_relation_str = mappings_IRI + "#relation";
		
		OWLLiteral confidence_literal;
		OWLAnnotation ann_measure;
		OWLAnnotationProperty ann_property;
		
		OWLLiteral ent1_literal;
		OWLAnnotation ann_ent1;
		OWLAnnotationProperty ann_property_ent1;

		OWLLiteral ent2_literal;
		OWLAnnotation ann_ent2;
		OWLAnnotationProperty ann_property_ent2;
		
		OWLLiteral relation_literal;
		OWLAnnotation ann_relation;
		OWLAnnotationProperty ann_property_relation;
		
		
		Set<OWLAnnotation> annSet = new HashSet<OWLAnnotation>();
		
		
		int type_mapping;
		String type_mapping_str;
		
		OWLAxiom ax;
		IRI iri_ent1;
		IRI iri_ent2;
		
		String relation_str;
		
		
		Set<OWLNamedIndividual> indivSet = new HashSet<OWLNamedIndividual>();
		
		
		
		
		for (MappingObjectStr mapping : RDF_mappings_reader.getMappingObjects()){
			
			
			iri_ent1 = IRI.create(mapping.getIRIStrEnt1());
			iri_ent2 = IRI.create(mapping.getIRIStrEnt2());
			
			
			//RELATION
			//default
			relation_str = "=";
			
			if (mapping.getMappingDirection() == Utilities.EQ){
				relation_str = "=";
			}
			else if (mapping.getMappingDirection() == Utilities.L2R){								
				relation_str = "<";					
			}
			else if (mapping.getMappingDirection() == Utilities.R2L){
				relation_str = ">";					
			}
			
			
			
			if (only_individuals){
				type_mapping = Utilities.INSTANCES;
				type_mapping_str = Utilities.INSTANCES_STR;
			}
			else{
				
				//TYPE MAPPING
				if (ontology1.containsClassInSignature(IRI.create(mapping.getIRIStrEnt1()), true) 
						&& ontology2.containsClassInSignature(IRI.create(mapping.getIRIStrEnt2()), true)){
					
					type_mapping = Utilities.CLASSES;
					type_mapping_str = Utilities.CLASSES_STR;
					
				}
				else if (ontology1.containsObjectPropertyInSignature(IRI.create(mapping.getIRIStrEnt1()), true) 
						&& ontology2.containsObjectPropertyInSignature(IRI.create(mapping.getIRIStrEnt2()), true)){
					
					type_mapping = Utilities.OBJECTPROPERTIES;
					type_mapping_str = Utilities.OBJECTPROPERTIES_STR;
				}
				else if (ontology1.containsDataPropertyInSignature(IRI.create(mapping.getIRIStrEnt1()), true) 
						&& ontology2.containsDataPropertyInSignature(IRI.create(mapping.getIRIStrEnt2()), true)){
					
					type_mapping = Utilities.DATAPROPERTIES;
					type_mapping_str = Utilities.DATAPROPERTIES_STR;
				}
				else if (ontology1.containsIndividualInSignature(IRI.create(mapping.getIRIStrEnt1()), true) 
						&& ontology2.containsIndividualInSignature(IRI.create(mapping.getIRIStrEnt2()), true)){
					
					type_mapping = Utilities.INSTANCES;
					type_mapping_str = Utilities.INSTANCES_STR;
					relation_str = "=";
				}
				else {
					System.err.println("Wrong mapping mixing entities ");
					System.err.println("\t" + mapping.getIRIStrEnt1());
					System.err.println("\t" + mapping.getIRIStrEnt2());				
					type_mapping = Utilities.UNKNOWN;
					continue; // with next mapping
				}
			}
			
			
			
			//Plain Text mappings
			if (type_mapping != Utilities.UNKNOWN){
				writer.writeLine(mapping.getIRIStrEnt1() + "|" + mapping.getIRIStrEnt2() + "|" + relation_str + "|" + mapping.getConfidence() + "|" + type_mapping_str);
			}
			
			
			
			//ANNOTATIONS
			ann_property = datafactory.getOWLAnnotationProperty(IRI.create(iri_measure_str));			
			confidence_literal = datafactory.getOWLLiteral(mapping.getConfidence());			
			ann_measure = datafactory.getOWLAnnotation(ann_property, confidence_literal);
			
			ann_property_ent1 = datafactory.getOWLAnnotationProperty(IRI.create(iri_entity1_str));			
			ent1_literal = datafactory.getOWLLiteral(mapping.getIRIStrEnt1());			
			ann_ent1 = datafactory.getOWLAnnotation(ann_property_ent1, ent1_literal);
			
			ann_property_ent2 = datafactory.getOWLAnnotationProperty(IRI.create(iri_entity2_str));			
			ent2_literal = datafactory.getOWLLiteral(mapping.getIRIStrEnt2());			
			ann_ent2 = datafactory.getOWLAnnotation(ann_property_ent2, ent2_literal);
			
			ann_property_relation = datafactory.getOWLAnnotationProperty(IRI.create(iri_relation_str));			
			relation_literal = datafactory.getOWLLiteral(relation_str);			
			ann_relation = datafactory.getOWLAnnotation(ann_property_relation, relation_literal);
			
			
			
			//Annotations are added to the axiom
			annSet.clear();
			annSet.add(ann_measure);
			annSet.add(ann_ent1);
			annSet.add(ann_ent2);
			annSet.add(ann_relation);
			
			
			
			
			if (type_mapping == Utilities.INSTANCE){
													
			
				//We only consider a kind of axioms between instances
				
				indivSet.clear();					
				indivSet.add(datafactory.getOWLNamedIndividual(iri_ent1));
				indivSet.add(datafactory.getOWLNamedIndividual(iri_ent2));
				
				addAxiom2Changes(
						datafactory.getOWLSameIndividualAxiom(
								indivSet,
								annSet)								
						);				
				
								
			}
			
			
			else if (type_mapping == Utilities.CLASSES){
				
				
				if (mapping.getMappingDirection() == Utilities.EQ){					
				
					addAxiom2Changes(
							datafactory.getOWLEquivalentClassesAxiom(
									datafactory.getOWLClass(iri_ent1),
									datafactory.getOWLClass(iri_ent2),
									annSet)								
							);
					
				}
				else if (mapping.getMappingDirection() == Utilities.L2R){
				
					addAxiom2Changes(
							datafactory.getOWLSubClassOfAxiom(
									datafactory.getOWLClass(iri_ent1),
									datafactory.getOWLClass(iri_ent2),
									annSet)								
							);
					
				}
				else if (mapping.getMappingDirection() == Utilities.R2L){
				
					addAxiom2Changes(
							datafactory.getOWLSubClassOfAxiom(
									datafactory.getOWLClass(iri_ent2),
									datafactory.getOWLClass(iri_ent1),
									annSet)								
							);
										
				}
				
				
			}
			
			else if (type_mapping == Utilities.OBJECTPROPERTIES){
				
				if (mapping.getMappingDirection() == Utilities.EQ){
					addAxiom2Changes(
							datafactory.getOWLEquivalentObjectPropertiesAxiom(
									datafactory.getOWLObjectProperty(iri_ent1),
									datafactory.getOWLObjectProperty(iri_ent2),
									annSet)								
							);
										
				}
				else if (mapping.getMappingDirection() == Utilities.L2R){
				
					addAxiom2Changes(
							datafactory.getOWLSubObjectPropertyOfAxiom(
									datafactory.getOWLObjectProperty(iri_ent1),
									datafactory.getOWLObjectProperty(iri_ent2),
									annSet)								
							);
					
										
				}
				else if (mapping.getMappingDirection() == Utilities.R2L){
				
					addAxiom2Changes(
							datafactory.getOWLSubObjectPropertyOfAxiom(
									datafactory.getOWLObjectProperty(iri_ent2),
									datafactory.getOWLObjectProperty(iri_ent1),
									annSet)								
							);
															
				}
				
			}
			else if (type_mapping == Utilities.DATAPROPERTIES){
				
				if (mapping.getMappingDirection() == Utilities.EQ){
								
					addAxiom2Changes(
							datafactory.getOWLEquivalentDataPropertiesAxiom(
									datafactory.getOWLDataProperty(iri_ent1),
									datafactory.getOWLDataProperty(iri_ent2),
									annSet)								
							);
										
				}
				else if (mapping.getMappingDirection() == Utilities.L2R){
				
					addAxiom2Changes(
							datafactory.getOWLSubDataPropertyOfAxiom(
									datafactory.getOWLDataProperty(iri_ent1),
									datafactory.getOWLDataProperty(iri_ent2),
									annSet)								
							);
					
										
				}
				else if (mapping.getMappingDirection() == Utilities.R2L){
				
					addAxiom2Changes(
							datafactory.getOWLSubDataPropertyOfAxiom(
									datafactory.getOWLDataProperty(iri_ent2),
									datafactory.getOWLDataProperty(iri_ent1),
									annSet)								
							);
					
										
				}
				
			}
			//else {//UNNOWN do nothing}
			
			
			
			
			
			
			
		}
		
		
	}
	
	
	
	
	
	private static OWLOntology loadOntologyStatic(String uri) throws Exception{
		System.out.println("\tLoading ontology " + uri);
		return loadOntologyStatic(IRI.create(uri));
	}
	
	private static OWLOntology loadOntologyStatic(IRI uri) throws Exception{
		OWLOntologyManager ontologyManager=OWLManager.createOWLOntologyManager();
		return ontologyManager.loadOntology(uri);
		
	}
	
	
	
	
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		//	Wrong mapping mixing entities 
		///	http://bioontology.org/projects/ontologies/fma/fmaOwlDlComponent_2_0#Modified_by
		//	http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#Gene_Product_Encoded_By_Gene

		String base_path;
		int onto_pair;
		
		
		if (args.length==2){
			base_path = args[0];
			onto_pair = Integer.valueOf(args[1]);
		}
		else{
			base_path = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/";
			//onto_pair = Utilities.FMA2NCI;
			//onto_pair = Utilities.FMA2SNOMED;
			onto_pair = Utilities.SNOMED2NCI;
			onto_pair = Utilities.MOUSE2HUMAN;
		}
		
		
		//String irirootpath = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/oaei_2012/fma2nci/";
		String irirootpath = "file:" + base_path + "OAEI_datasets/oaei_2012/fma2nci/";
		String irirootpath2 = "file:" + base_path + "OAEI_datasets/oaei_2012/fma2snmd/";
		
		//String irirootpath = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/Mappings_Tools_2012/";
		
		//String mappings_path = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/Mappings_Tools/";
		//String mappings_path = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/OutputAlcomo/";
		
		//Original path
		//String mappings_path = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/Mappings_Tools_2012/";
		String mappings_path = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/Mappings_Tools_2012/Top7_rdf/";
		//String mappings_path = base_path + "OAEI_datasets/Mappings_Tools_2012/";
		
		//String mappings_path = base_path + "OAEI_datasets/oaei_2012/fma2nci/";
		//String mappings_path = base_path + "OAEI_datasets/oaei_2012/fma2snmd/";
		//String mappings_path = base_path + "OAEI_datasets/oaei_2012/snmd2nci/";
		
		
		
		int max_folder=80;  //11 sandbox  //80 iimb
		//mappings_path = "/usr/local/data/Instance/sandbox/";
		//mappings_path = "/usr/local/data/Instance/iimb/";
		//mappings_path = "/usr/local/data/Instance/iimb_large/";
		
		
		String irirootpath_mappings = "file:" + mappings_path; 

		
	
		
		String onto1;
		String onto2;
		String pattern;
		if (onto_pair==Utilities.FMA2NCI){
			//FMA2NCI
			onto1 = irirootpath + "oaei2012_FMA_whole_ontology.owl";
			//onto1 = irirootpath + "oaei2012_FMA_small_overlapping_nci.owl";
			onto2 = irirootpath + "oaei2012_NCI_whole_ontology.owl";
			//onto2 = irirootpath + "oaei2012_NCI_small_overlapping_fma.owl";
			pattern="fma2nci.rdf";
		}
		else if (onto_pair==Utilities.FMA2SNOMED){
			//onto1 = irirootpath + "oaei2012_FMA_whole_ontology.owl";
			//onto2 = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/snomed20090131_replab.owl.zip";
			onto1 = irirootpath + "oaei2012_FMA_whole_ontology.owl";
			onto2 = irirootpath2 + "oaei2012_SNOMED_whole_ontology.owl.zip";
			pattern="fma2snomed.rdf";
		}
		else if (onto_pair==Utilities.SNOMED2NCI){
			//onto1 = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/snomed20090131_replab.owl.zip";
			//onto2 = irirootpath + "oaei2012_NCI_whole_ontology.owl";
			onto2 = irirootpath + "oaei2012_NCI_whole_ontology.owl";
			onto1 = irirootpath2 + "oaei2012_SNOMED_whole_ontology.owl.zip";
			pattern="snomed2nci.rdf";
		}
		else { //mouse
			onto1= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/Anatomy/2012/mouse2012.owl";
			onto2= "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/Anatomy/2012/human2012.owl";
			//TODO to be completed...
		}
		
			
		
		
		
		String mappings_file;
		String gold_rdf;	
		String gold_owl;
		String gold_txt;
		OWLOntology OWLonto1;
		OWLOntology OWLonto2;
		
		try {
			
			/*System.out.println("Loading ontologies...");
			OWLonto1 = loadOntologyStatic(onto1);
			OWLonto2 = loadOntologyStatic(onto2); 
			System.out.println("...Done");
			new FromRDFAlign2OWL(
					OWLonto1, OWLonto2, 
					mappings_path + "logmap_small_fma2nci_new.rdf"); 
					
			if (true)
			return;
			*/
			
			if (args.length==3) {
				
				//TODO should be in a different place
				new FromRDFAlign2OWL(args[0], args[1], args[2]);
			
			}
			
			else {
				
				
				File directory = new File(mappings_path);
				String filenames[] = directory.list();
				
				
				System.out.println("Loading ontologies...");
				OWLonto1 = loadOntologyStatic(onto1);
				OWLonto2 = loadOntologyStatic(onto2); 
				System.out.println("...Done");

				for(int i=0; i<filenames.length; i++){
					//if (!filenames[i].contains("oaei2012_FMA2SNMD_repaired_UMLS_mappings_alcomo.rdf"))
					//if (!filenames[i].contains("oaei2012_FMA2NCI_repaired_UMLS_mappings_alcomo.rdf"))
					//if (!filenames[i].contains("refalign.rdf"))
					//if (!filenames[i].contains(".rdf"))
					//if (!filenames[i].contains(pattern) || !(filenames[i].contains("gomma")))
					//if (!filenames[i].contains(pattern))
					if (!filenames[i].contains("servomap_whole2_snomed2nci_repaired_with_Alcomo_Hermit.rdf"))
						continue;
					
					System.out.println("Converting " + filenames[i] + "...");
					
					mappings_file = filenames[i].split("\\.")[0];
					
					
					gold_rdf = mappings_path + filenames[i];	
					//gold_owl = irirootpath_mappings + mappings_file + ".owl";
					//gold_txt = mappings_path + mappings_file + ".txt";
				
					
					new FromRDFAlign2OWL(OWLonto1, OWLonto2, gold_rdf); //true for instance matching track
					
					System.out.println("...Done");
					
				}
					
					
				
				/*  INSTANCE
				for (int folder=1; folder<=max_folder;folder++){//instance
				
				File directory = new File(mappings_path + convert2ThreeDigitStrNumber(folder) + "/");
				//File directory = new File(mappings_path);
				String filenames[] = directory.list();
				

				for(int i=0; i<filenames.length; i++){
					if (!filenames[i].contains("refalign.rdf"))	
						continue;
					
					mappings_file = filenames[i].split("\\.")[0];
					
					gold_rdf = irirootpath_mappings + convert2ThreeDigitStrNumber(folder) + "/" + filenames[i];	
					//gold_owl = irirootpath_mappings + convert2ThreeDigitStrNumber(folder) + "/" + mappings_file + ".owl";					
					//gold_txt = mappings_path + convert2ThreeDigitStrNumber(folder) + "/" + mappings_file + ".txt";
														
					new FromRDFAlign2OWL(gold_rdf); //true for instance matching track
				}
				
				}//end for 4 instance
				*/
			}
		}
		catch (Exception e){
			e.printStackTrace();
		}

	}
	
	
	
	
	
	
	
	
}
