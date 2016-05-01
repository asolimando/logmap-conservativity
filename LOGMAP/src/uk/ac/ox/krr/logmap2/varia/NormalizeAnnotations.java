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

import java.util.HashSet;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.apibinding.OWLManager;

import uk.ac.ox.krr.logmap2.lexicon.LexicalUtilities;
import uk.ac.ox.krr.logmap2.OntologyLoader;




/**
 * 
 * 
 * 
 * @author Ernesto Jimenez Ruiz
 *
 */
public class NormalizeAnnotations {
	
	
	//IRIS of alternative labels annotations 
	private String rdf_label_uri = "http://www.w3.org/2000/01/rdf-schema#label";
	private String hasRelatedSynonym_uri = "http://www.geneontology.org/formats/oboInOwl#hasRelatedSynonym";
		
	private String nci_synonym_uri = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#Synonym";
	//private String nci_umls_cui_uri = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#UMLS_CUI";
		
	private String nci_pref_name_uri = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#Preferred_Name";
	
	private String fma_synonym_uri = "http://bioontology.org/projects/ontologies/fma/fmaOwlDlComponent_2_0#Synonym";	
	private String fma_pref_name_uri = "http://bioontology.org/projects/ontologies/fma/fmaOwlDlComponent_2_0#Preferred_name";
	private String fma_non_en_eq_uri = "http://bioontology.org/projects/ontologies/fma/fmaOwlDlComponent_2_0#Non_English_equivalent";
	
	//For fma annotations
	private String fma_name_uri="http://bioontology.org/projects/ontologies/fma/fmaOwlDlComponent_2_0#name";
	private String fma_lang_uri="http://bioontology.org/projects/ontologies/fma/fmaOwlDlComponent_2_0#Language";
		
	//IRIS 2 Ignore 
	private String oboinowl = "http://www.geneontology.org/formats/oboInOwl";
	private String synonym ="http://oaei.ontologymatching.org/annotations#synonym";
	
	private OWLAnnotationProperty labelAnnProp;
	private OWLDataProperty fma_name_prop;
	private OWLDataProperty fma_lang_prop;
	
	private OntologyLoader loader; 
	
	private List<OWLOntologyChange> listchanges;
	
	private Set<String> labels = new HashSet<String>();
	
	
	/**
	 * 
	 * @param phy_iri_onto
	 * @param phy_iri_onto_norm
	 */
	public NormalizeAnnotations(String phy_iri_onto, String phy_iri_onto_norm){
	
		try {
		
				//Load ontology
			loader = new OntologyLoader(phy_iri_onto);
			
			labelAnnProp = loader.getDataFactory().getOWLAnnotationProperty(IRI.create(rdf_label_uri));			
			//synonymAnnProp = loader.getDataFactory().getOWLAnnotationProperty(IRI.create(synonym));
			
			fma_name_prop = loader.getDataFactory().getOWLDataProperty(IRI.create(fma_name_uri));
			fma_lang_prop = loader.getDataFactory().getOWLDataProperty(IRI.create(fma_lang_uri));
					
			listchanges = new ArrayList<OWLOntologyChange>();
			
			for (OWLClass cls : loader.getClassesInSignature()){
				normalizeAlternateLabels4OWLCls(cls);
				//normalizeAlternateLabels4OWLCls_only1label(cls);
			}
			
			for (OWLDataProperty dProp : loader.getOWLOntology().getDataPropertiesInSignature()){
				normalizeLabelsProperty(dProp);
			}
			
			for (OWLObjectProperty oProp : loader.getOWLOntology().getObjectPropertiesInSignature()){
				normalizeLabelsProperty(oProp);
			}
			
			//Remove assertion axioms!
			for (OWLAxiom ax : loader.getOWLOntology().getABoxAxioms(false)){
				removeAxiom(ax);
			}
			
			for (OWLNamedIndividual namedIndiv : loader.getOWLOntology().getIndividualsInSignature()){
				//We remove individual
				removeAxiom(loader.getDataFactory().getOWLDeclarationAxiom(namedIndiv));
			}
			
			//Remove individuals??		
			loader.applyChanges(listchanges);
			loader.saveOntology(phy_iri_onto_norm);
			
		}
		catch (Exception e){
			e.printStackTrace();
		}

	}

	
	
	
	
	private OWLAnnotationAssertionAxiom createClassSynonymAnnotation(IRI cls_iri, String value, String lang){
		
		return loader.getDataFactory().getOWLAnnotationAssertionAxiom(
				labelAnnProp, 
				cls_iri, 
				loader.getDataFactory().getOWLLiteral(value, lang));
		
		
	}
	
	private void removeAxiom(OWLAxiom ax){
		
		listchanges.add(new RemoveAxiom(loader.getOWLOntology(), ax));
		
	}
	
	
	
	
	/**
	 * Return alternate labels
	 * They could be represented using different annotation properties
	 * @param cls
	 * @return
	 */
	private void normalizeAlternateLabels4OWLCls(OWLClass cls){
		
		String language="";
		String label_value="";
		OWLAnonymousIndividual geneid_value;
		
		OWLNamedIndividual namedIndiv=null;
		IRI namedIndivIRI;
		
		
		labels.clear();
		
		
		
		for (OWLAnnotationAssertionAxiom clsAnnAx : cls.getAnnotationAssertionAxioms(loader.getOWLOntology())){
			
			//We remove all annotation and will keep labels
			removeAxiom(clsAnnAx);
			label_value="";
			
			
			if (clsAnnAx.getAnnotation().getProperty().getIRI().toString().equals(rdf_label_uri)){				
				
				label_value=((OWLLiteral)clsAnnAx.getAnnotation().getValue()).getLiteral().toString();//.toLowerCase();
				
				labels.add(label_value);
				
			}			
						
			//Annotations in Mouse Anatomy and NCI Anatomy
			//---------------------------------------------
			else if (clsAnnAx.getAnnotation().getProperty().getIRI().toString().equals(hasRelatedSynonym_uri)){
									
				//It is an individual
				geneid_value=((OWLAnonymousIndividual)clsAnnAx.getAnnotation().getValue()).asOWLAnonymousIndividual();//.getID()
				
				for (OWLAnnotationAssertionAxiom annGeneidAx : loader.getOWLOntology().getAnnotationAssertionAxioms(geneid_value)){

					//remove ann axioms genei id
					removeAxiom(annGeneidAx);
					
					if (annGeneidAx.getAnnotation().getProperty().getIRI().toString().equals(rdf_label_uri)){
						
						label_value=((OWLLiteral)annGeneidAx.getAnnotation().getValue()).getLiteral().toString();//.toLowerCase();
						
						labels.add(label_value);

						
					}
				}
				
				
			}
			
			//Annotations in NCI Full
			//---------------------------------------------
			else if (clsAnnAx.getAnnotation().getProperty().getIRI().toString().equals(nci_synonym_uri) ||
					clsAnnAx.getAnnotation().getProperty().getIRI().toString().equals(nci_pref_name_uri)){
				
				
				
				label_value=((OWLLiteral)clsAnnAx.getAnnotation().getValue()).getLiteral().toString();//.toLowerCase();
				
				labels.add(label_value);
				
			}
			
			//Annotations in FMA DL 2.0
			//----------------------------
			else if (clsAnnAx.getAnnotation().getProperty().getIRI().toString().equals(fma_synonym_uri) ||
					clsAnnAx.getAnnotation().getProperty().getIRI().toString().equals(fma_pref_name_uri) ||
					clsAnnAx.getAnnotation().getProperty().getIRI().toString().equals(fma_non_en_eq_uri)){
									
				//It is an individual
				namedIndivIRI=(IRI)clsAnnAx.getAnnotation().getValue();
					
				namedIndiv=loader.getDataFactory().getOWLNamedIndividual(namedIndivIRI);
					
					
				//Synonym name is in data property assertion, not in annotation!!
					
				
				if (namedIndiv==null)
					continue;
					
				
				label_value = "";
				language = "";
				//We get the language
				if (clsAnnAx.getAnnotation().getProperty().getIRI().toString().equals(fma_non_en_eq_uri)){
					
					for (OWLDataPropertyAssertionAxiom ax : loader.getOWLOntology().getDataPropertyAssertionAxioms(namedIndiv)){
						if (!ax.getProperty().isAnonymous()){
							if (ax.getProperty().asOWLDataProperty().equals(fma_name_prop)){
								
								label_value = ax.getObject().getLiteral().toString();
								
							}
							else if (ax.getProperty().asOWLDataProperty().equals(fma_lang_prop)){
								if (ax.getObject().getLiteral().toString().toLowerCase().equals("latin")){ //only latin
									language = "lat";//ax.getObject().getLiteral().toString();
								}
							}
						}
					}
					
					//we add now
					if (!label_value.equals("") && !language.equals("")){
						listchanges.add(new AddAxiom(
								loader.getOWLOntology(), 
								createClassSynonymAnnotation(cls.getIRI(), label_value, language)));
					}
					
				}
				else{
					for (OWLLiteral literal_syn : namedIndiv.getDataPropertyValues(fma_name_prop, loader.getOWLOntology())){
				
						label_value = literal_syn.getLiteral().toString();//.toLowerCase();
					
						//we add later as English label
						labels.add(label_value);
						
					}
				}
					
			}
			
			//SNOMED has only rdf_labels annotations
			
		}//end class ann axioms
		
		
		for (String label : labels){ //This way we avoid duplicate labels
			listchanges.add(new AddAxiom(
					loader.getOWLOntology(), 
					createClassSynonymAnnotation(cls.getIRI(), label, "en")));
		}
		
		
		
	}
	
	
	
	
	/**
	 * Test method
	 * @param cls
	 * @return
	 */
	private void normalizeLabelsProperty(OWLProperty prop){
		
		//System.out.println(prop);
	
		
		for (OWLAnnotationAssertionAxiom propAnnAx : prop.getAnnotationAssertionAxioms(loader.getOWLOntology())){
			
			//System.out.println(propAnnAx);
			
			if (propAnnAx.getAnnotation().getProperty().getIRI().toString().equals(rdf_label_uri)){				
				continue;
			}			
				
			//We remove all annotation and will keep labels
			removeAxiom(propAnnAx);
								
			
			
		}		
		
		
		
	}
	
	
	/**
	 * Test method
	 * @param cls
	 * @return
	 */
	private void normalizeAlternateLabels4OWLCls_only1label(OWLClass cls){
		
	
		OWLAnonymousIndividual geneid_value;
		
		
		
		for (OWLAnnotationAssertionAxiom clsAnnAx : cls.getAnnotationAssertionAxioms(loader.getOWLOntology())){
			
			
			
			if (clsAnnAx.getAnnotation().getProperty().getIRI().toString().equals(rdf_label_uri)){				
				
				//do nothing
				
			}			
						
			//Annotations in Mouse Anatomy and NCI Anatomy
			//---------------------------------------------
			else if (clsAnnAx.getAnnotation().getProperty().getIRI().toString().equals(hasRelatedSynonym_uri)){
				
				//We remove all annotation and will keep labels
				removeAxiom(clsAnnAx);
									
				//It is an individual
				geneid_value=((OWLAnonymousIndividual)clsAnnAx.getAnnotation().getValue()).asOWLAnonymousIndividual();//.getID()
				
				for (OWLAnnotationAssertionAxiom annGeneidAx : loader.getOWLOntology().getAnnotationAssertionAxioms(geneid_value)){

					//remove ann axioms genei id
					removeAxiom(annGeneidAx);
					
				}
				
				
			}
			
			//Annotations in NCI Full
			//---------------------------------------------
			else if (clsAnnAx.getAnnotation().getProperty().getIRI().toString().equals(nci_synonym_uri) ||
					clsAnnAx.getAnnotation().getProperty().getIRI().toString().equals(nci_pref_name_uri)){
				
				//We remove all annotation and will keep labels
				removeAxiom(clsAnnAx);
				
				
				
				
			}
			
			//Annotations in FMA DL 2.0
			//----------------------------
			else if (clsAnnAx.getAnnotation().getProperty().getIRI().toString().equals(fma_synonym_uri) ||
					clsAnnAx.getAnnotation().getProperty().getIRI().toString().equals(fma_pref_name_uri) ||
					clsAnnAx.getAnnotation().getProperty().getIRI().toString().equals(fma_non_en_eq_uri)){
									
				
				//We remove all annotation and will keep labels
				removeAxiom(clsAnnAx);
				
					
			}
			else {
				//We remove all annotation and will keep labels
				removeAxiom(clsAnnAx);
			}
				
			
			//SNOMED has only rdf_labels annotations
			
		}//end class ann axioms
		
		
		
		
		
	}
	
	
	public static void main(String[] args) {
		
		String iri_in = "";
		String iri_out = "";
		
		//MOUSE
		iri_in = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/Anatomy/mouse_anatomy_2010.owl";
		iri_out = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/Anatomy/mouse_anatomy_2010_norm.owl";
		
		iri_in = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/Anatomy/nci_anatomy_2010.owl";
		iri_out = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/Anatomy/nci_anatomy_2010_norm.owl";
		
		
		//FMA
		//iri_in ="file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/FMADL_2_0_with_synonyms.owl";
		//iri_out ="file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2nci_dataset/oaei2012_FMA_whole_ontology.owl";
		
		//FMA2NCI
		//iri_in ="file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2nci_dataset/FMA_bigoverlapping_nci.owl";
		//iri_out ="file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2nci_dataset/oaei2012_FMA_big_overlapping_nci.owl";
		
		//iri_in ="file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2nci_dataset/FMA_overlapping_nci.owl";
		//iri_out ="file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2nci_dataset/oaei2012_FMA_small_overlapping_nci.owl";
		//iri_out ="file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2nci_dataset/oaei2012_FMA_small_overlapping_1label_nci.owl";
		
		//FMA2SNOMED		
		//iri_in ="file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2snmd_dataset/FMA_bigoverlapping_snmd.owl";
		//iri_out ="file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2snmd_dataset/oaei2012_FMA_big_overlapping_snomed.owl";
				
		//iri_in ="file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2snmd_dataset/FMA_overlapping_snmd.owl";
		//iri_out ="file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2snmd_dataset/oaei2012_FMA_small_overlapping_snomed.owl";
					
		
		
				
		//NCI
		//iri_in = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/NCI_Thesaurus_08.05d_with_synonyms.owl";
		//iri_out = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2nci_dataset/oaei2012_NCI_whole_ontology.owl";
		
		//FMA2NCI
		//iri_in ="file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2nci_dataset/NCI_bigoverlapping_fma.owl";
		//iri_out ="file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2nci_dataset/oaei2012_NCI_big_overlapping_fma.owl";
		
		//iri_in ="file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2nci_dataset/NCI_overlapping_fma.owl";
		//iri_out ="file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2nci_dataset/oaei2012_NCI_small_overlapping_fma.owl";
		//iri_out ="file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2nci_dataset/oaei2012_NCI_small_overlapping_1label_fma.owl";
		
		//NCI2SNOMED
		
		//iri_in ="file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/snmd2nci_dataset/NCI_bigoverlapping_snmd.owl";
		//iri_out ="file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/snmd2nci_dataset/oaei2012_NCI_big_overlapping_snomed.owl";
				
		//iri_in ="file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/snmd2nci_dataset/NCI_overlapping_snmd.owl";
		//iri_out ="file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/snmd2nci_dataset/oaei2012_NCI_small_overlapping_snomed.owl";
		
		new NormalizeAnnotations(iri_in, iri_out);
		
		
	}
	
	
	
	
}
