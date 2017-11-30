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
package uk.ac.ox.krr.logmap2.indexing;

import java.util.ArrayList;


import java.util.HashSet;


import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Map.Entry;

//import org.apache.commons.lang3.StringUtils;


import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.impl.OWLClassNodeSet;

import uk.ac.ox.krr.logmap2.Parameters;
import uk.ac.ox.krr.logmap2.indexing.entities.ClassIndex;
import uk.ac.ox.krr.logmap2.io.LogOutput;
import uk.ac.ox.krr.logmap2.lexicon.LexicalUtilities;
import uk.ac.ox.krr.logmap2.lexicon.NormalizeDate;
import uk.ac.ox.krr.logmap2.lexicon.NormalizeNumbers;
import uk.ac.ox.krr.logmap2.owlapi.SynchronizedOWLManager;
import uk.ac.ox.krr.logmap2.reasoning.ELKAccess;
import uk.ac.ox.krr.logmap2.reasoning.MOReAccess;
import uk.ac.ox.krr.logmap2.reasoning.StructuralReasonerExtended;
//import uk.ac.ox.krr.logmap2.reasoning.deprecated.HermiTReasonerAccess;
import uk.ac.ox.krr.logmap2.reasoning.HermiTAccess;
import uk.ac.ox.krr.logmap2.utilities.Lib;
import uk.ac.ox.krr.logmap2.utilities.PrecomputeIndexCombination;
import uk.ac.ox.krr.logmap2.utilities.Utilities;

/**
 * This class will extract the lexicon and structure of the given ontology
 * 
 *
 * @author Ernesto Jimenez-Ruiz
 * Created: Sep 12, 2011
 *
 */
public class OntologyProcessing {

	long init, fin;

	private OWLOntology onto;

	private IndexManager index;

	/*Exact match entries*/
	//protected Map<String, Integer> extact_occurrence_entries = new HashMap<String, Integer>();


	/**Exact match inverted file*/
	protected Map<Set<String>, Set<Integer>> invertedFileExact = new HashMap<Set<String>, Set<Integer>>();

	/**Exact match inverted file for data properties*/
	protected Map<Set<String>, Integer> invertedFileExactDataProp = new HashMap<Set<String>, Integer>();

	/**Exact match inverted file for object properties*/
	protected Map<Set<String>, Integer> invertedFileExactObjProp = new HashMap<Set<String>, Integer>();

	protected Map<Set<String>, Set<Integer>> invertedFileIndividuals = new HashMap<Set<String>, Set<Integer>>();
	//Only one word IF weak
	protected Map<String, Set<Integer>> invertedFileWeakIndividuals = new HashMap<String, Set<Integer>>();


	//Will contain a lexicon of the role assertions in indiv
	protected Map<String, Set<Integer>> invertedFileRoleassertions = new HashMap<String, Set<Integer>>();


	protected Map<Set<String>, Set<Integer>> invertedFileWeakLabelsStemming = new HashMap<Set<String>, Set<Integer>>();

	protected Map<Set<String>, Set<Integer>> invertedFileWeakLabels = new HashMap<Set<String>, Set<Integer>>();
	//protected Map<Set<String>, Set<Integer>> invertedFileWeakLabelsL2 = new HashMap<Set<String>, Set<Integer>>();
	//protected Map<Set<String>, Set<Integer>> invertedFileWeakLabelsL3 = new HashMap<Set<String>, Set<Integer>>();



	/**To index class names: necessary when loading GS and repairing mappings*/
	protected Map<String, Integer> className2Identifier = new HashMap<String, Integer>();

	/**To index data prop names: necessary when loading GS and repairing mappings*/
	protected Map<String, Integer> dataPropName2Identifier = new HashMap<String, Integer>();

	/**To index object prop names: necessary when loading GS and repairing mappings*/
	protected Map<String, Integer> objectPropName2Identifier = new HashMap<String, Integer>();

	/**To index instance names: necessary when loading GS and repairing mappings*/
	protected Map<String, Integer> individualName2Identifier = new HashMap<String, Integer>();


	/**USed when extracting taxonomy*/
	private Map<OWLClass, Integer> class2identifier = new HashMap<OWLClass, Integer>();
	private Map<String, Integer> classIri2identifier = new HashMap<String, Integer>();
	
	//For lexicon extraction. Indiv are related one to the other
	private Map<OWLNamedIndividual, Integer> inidividual2identifier = new HashMap<OWLNamedIndividual, Integer>();

	private Map<Integer, OWLClass> identifier2class = new HashMap<Integer, OWLClass>();

	//In this set we should include those classes we should not map
	//e.g. classes that are equivalent to Top (see cocus ontology)
	private Set<Integer> dangerousClasses = new HashSet<Integer>();


	//private HashMap<Integer, Set<Integer>> identifier2directkids= new HashMap<Integer, Set<Integer>>();	
	//private Map<Integer, Set<Integer>> identifier2directparents= new HashMap<Integer, Set<Integer>>();

	/**This set will be used to propagate equivalences (entities store equivalents)*/
	//TODO this set will need to be enriched with anchors... or at leats considered after indexing
	//In index
	//private Set<Integer> representativeNodes = new HashSet<Integer>();

	/* Importnat for a ordered assessment Now in index*/
	//protected Set<Integer> RootIdentifiers = new HashSet<Integer>();


	/*A^B->C axioms Now in Index*/
	//private Map<Set<Integer>, Integer> generalHornAxioms= new HashMap<Set<Integer>, Integer>();



	/** We use this structure to create weak labels*/
	//private Map<Integer, List<String>> identifier2stemmedlabel = new HashMap<Integer, List<String>>();
	//We also consider alternative labels
	private Map<Integer, Set<List<String>>> identifier2stemmedlabels = new HashMap<Integer, Set<List<String>>>();
	//Must be kept outside index to be removed after use and to attached to onto (i.e ontoprocessing object)

	/**
	 * Meaningful roots 
	 * @deprecated
	 */
	protected Set<Integer> MeaningfulRootIdentifiers = new HashSet<Integer>();
	/**Real roots 
	 * @deprecated
	 */
	protected Set<Integer> TaxRootIdentifiers = new HashSet<Integer>(); //The real ones


	//IRIS of alternative labels annotations 
	private String rdf_label_uri = "http://www.w3.org/2000/01/rdf-schema#label";
	private String hasRelatedSynonym_uri = "http://www.geneontology.org/formats/oboInOwl#hasRelatedSynonym";
	private String hasExactSynonym_uri   = "http://www.geneontology.org/formats/oboInOwl#hasExactSynonym";
	private String nci_synonym_uri = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#Synonym";
	//private String nci_umls_cui_uri = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#UMLS_CUI";
	private String fma_synonym_uri="http://bioontology.org/projects/ontologies/fma/fmaOwlDlComponent_2_0#Synonym";
	private String fma_name_uri="http://bioontology.org/projects/ontologies/fma/fmaOwlDlComponent_2_0#name";


	//IRIS 2 Ignore 
	private String oboinowl = "http://www.geneontology.org/formats/oboInOwl";

	private String iri_onto = "http://krono.act.uji.es/ontology.owl";

	public String getOntoIRI() {return iri_onto; }

	private int id_onto;


	int num_syn=0;
	int toohigh_synset_cases=0;	



	private OWLReasoner reasoner;
	private int reasonerIdentifier;


	//TODO Ernesto: to avoid using all labels for weak mappings
	boolean use_all_labels_for_weak_mappings=false;


	private LexicalUtilities lexicalUtilities;

	private PrecomputeIndexCombination precomputeIndexCombination = new PrecomputeIndexCombination();

	private ExtractStringFromAnnotationAssertionAxiom annotationExtractor = new ExtractStringFromAnnotationAssertionAxiom();

	public ExtractAcceptedLabelsFromRoleAssertions roleAssertionLabelsExtractor = new ExtractAcceptedLabelsFromRoleAssertions();


	public OntologyProcessing(OntologyProcessing op){
		this.onto = op.onto;
		this.index = op.index;
		//this.lexicalUtilities = op.lexicalUtilities;
		this.id_onto = op.id_onto;
		this.reasonerIdentifier = op.reasonerIdentifier;
		this.toohigh_synset_cases = op.toohigh_synset_cases;
		this.num_syn = op.num_syn;
		this.iri_onto = op.iri_onto;
		this.oboinowl = op.oboinowl;
		TaxRootIdentifiers = new HashSet<Integer>(op.TaxRootIdentifiers);
		MeaningfulRootIdentifiers = new HashSet<Integer>(op.MeaningfulRootIdentifiers);
		this.minNumberOfRoots = op.minNumberOfRoots;
		this.ausxSetOfClasses = new HashSet<Integer>(op.ausxSetOfClasses);
		this.labels_set = new HashSet<String>(op.labels_set);
		this.lexiconValues4individual = new HashSet<String>(op.lexiconValues4individual);
		this.fma_name_uri = op.fma_name_uri;
		this.fma_synonym_uri = op.fma_synonym_uri;

		// TODO Alessandro: is it needed for repairing?
		filteredInvertedIndex = new HashMap<Set<String>, Set<Integer>>();
		for (Entry<Set<String>, Set<Integer>> e 
				: op.filteredInvertedIndex.entrySet()) {

			filteredInvertedIndex.put(new HashSet<>(e.getKey()), 
					new HashSet<>(e.getValue()));
		}

		identifier2stemmedlabels = new HashMap<Integer, Set<List<String>>>();
		for (Entry<Integer, Set<List<String>>> e 
				: op.identifier2stemmedlabels.entrySet()) {
			Set<List<String>> set = new HashSet<>();
			for (List<String> l : e.getValue())
				set.add(new ArrayList<>(l));

			identifier2stemmedlabels.put(e.getKey(), set);
		}

		invertedFileExact = new HashMap<Set<String>, Set<Integer>>();
		for (Entry<Set<String>, Set<Integer>> e 
				: op.invertedFileExact.entrySet()) {
			invertedFileExact.put(new HashSet<>(e.getKey()), 
					new HashSet<>(e.getValue()));
		}

		invertedFileExactDataProp = new HashMap<Set<String>, Integer>();
		for (Entry<Set<String>, Integer> e 
				: op.invertedFileExactDataProp.entrySet()) {
			invertedFileExactDataProp.put(new HashSet<>(e.getKey()), 
					e.getValue());
		}

		invertedFileExactObjProp = new HashMap<Set<String>, Integer>();
		for (Entry<Set<String>, Integer> e 
				: op.invertedFileExactObjProp.entrySet()) {
			invertedFileExactObjProp.put(new HashSet<>(e.getKey()), 
					e.getValue());
		}

		invertedFileIndividuals = new HashMap<Set<String>, Set<Integer>>();
		for (Entry<Set<String>, Set<Integer>> e 
				: op.invertedFileIndividuals.entrySet()) {
			invertedFileIndividuals.put(new HashSet<>(e.getKey()), 
					new HashSet<>(e.getValue()));
		}

		invertedFileWeakIndividuals = new HashMap<String, Set<Integer>>();
		for (Entry<String, Set<Integer>> e 
				: op.invertedFileWeakIndividuals.entrySet()) {
			invertedFileWeakIndividuals.put(e.getKey(), 
					new HashSet<>(e.getValue()));
		}

		invertedFileRoleassertions = new HashMap<String, Set<Integer>>();
		for (Entry<String, Set<Integer>> e 
				: op.invertedFileRoleassertions.entrySet()) {
			invertedFileRoleassertions.put(e.getKey(), 
					new HashSet<>(e.getValue()));
		}

		invertedFileWeakLabelsStemming = new HashMap<Set<String>, Set<Integer>>();
		for (Entry<Set<String>, Set<Integer>> e 
				: op.invertedFileWeakLabelsStemming.entrySet()) {
			invertedFileWeakLabelsStemming.put(new HashSet<>(e.getKey()), 
					new HashSet<>(e.getValue()));
		}

		invertedFileWeakLabels = new HashMap<Set<String>, Set<Integer>>();
		for (Entry<Set<String>, Set<Integer>> e 
				: op.invertedFileWeakLabels.entrySet()) {
			invertedFileWeakLabels.put(new HashSet<>(e.getKey()), 
					new HashSet<>(e.getValue()));
		}
		// TODO Alessandro: is it needed for repairing? (END)

		className2Identifier = new HashMap<String, Integer>(op.className2Identifier);
		classIri2identifier = new HashMap<String, Integer>(op.classIri2identifier);
		dataPropName2Identifier = new HashMap<String, Integer>(op.dataPropName2Identifier);
		objectPropName2Identifier = new HashMap<String, Integer>(op.objectPropName2Identifier);
		individualName2Identifier = new HashMap<String, Integer>(op.individualName2Identifier);
		class2identifier = new HashMap<OWLClass, Integer>(op.class2identifier);
		inidividual2identifier = new HashMap<OWLNamedIndividual, Integer>(op.inidividual2identifier);
		identifier2class = new HashMap<Integer, OWLClass>(op.identifier2class);
		dangerousClasses = new HashSet<Integer>(op.dangerousClasses);
		
//		reasoner = op.reasoner;
	}

	/**
	 * 
	 * @param onto
	 * @param index
	 * @param lexicalUtilities
	 */
	public OntologyProcessing(OWLOntology onto, IndexManager index, LexicalUtilities lexicalUtilities){

		this.onto=onto;
		this.index=index;

		this.lexicalUtilities=lexicalUtilities;


		//We precompute indexes
		//precomputeIndexCombination.clearCombinations(); //Old calls
		precomputeIndexCombination.preComputeIdentifierCombination();


		//The preclassification with condor has no ontology id
		if (onto.getOntologyID().getOntologyIRI()!=null){
			iri_onto=onto.getOntologyID().getOntologyIRI().toString();
		}

		this.id_onto = this.index.addNewOntologyEntry(iri_onto);



	}

	public OntologyProcessing(OWLOntology onto, IndexManager index, LexicalUtilities lexicalUtilities, OWLReasoner reasoner){
		this(onto,index,lexicalUtilities);		
		this.reasoner = reasoner;
	}

	public void clearOntologyRelatedInfo(){
		//TODO After extracting tax
		onto=null;
		class2identifier.clear();
		classIri2identifier.clear();
		clearReasoner();
	}

	public Map<OWLClass, Integer> getClass2Identifier(){
		return class2identifier;
	}

	public void clearInvertedFilesExact(){
		invertedFileExact.clear();
		//invertedFileExactDataProp.clear();
		//invertedFileExactObjProp.clear();	
	}

	public void clearInvertedFileStemming(){

		invertedFileWeakLabelsStemming.clear();
	}

	public void clearInvertedFileWeak(){

		invertedFileWeakLabels.clear();		

	}


	public void clearInvertedFilesStemmingAndWeak(){

		invertedFileWeakLabels.clear();		
		invertedFileWeakLabelsStemming.clear();
	}

	public void clearStemmedLabels(){
		//identifier2stemmedlabel.clear();
		identifier2stemmedlabels.clear();
	}

	public void clearInvertedFiles4properties(){
		invertedFileExactDataProp.clear();
		invertedFileExactObjProp.clear();
	}

	public void clearInvertedFiles4Individuals(){
		invertedFileIndividuals.clear();
		invertedFileWeakIndividuals.clear();
		invertedFileRoleassertions.clear();
	}





	/**
	 * @deprecated
	 * 
	 */
	private void clearTaxonomy(){
		//TODO after indexing or the global id2kids has been created 
		//identifier2directkids.clear();
		//identifier2directparents.clear();
		//representativeNodes.clear();
		//generalHornAxioms.clear();
	}


	public void clearReasoner(){
		//TODO after extracting tax!
		if(reasoner != null){
			reasoner.dispose();
			reasoner=null;
		}
	}



	public void precessLexicon() {
		precessLexicon(true);
	}

	/**
	 * We create a class identifier for each class, we also create inverted files
	 * 
	 */
	public void precessLexicon(boolean extractLabels) {


		init=Calendar.getInstance().getTimeInMillis();



		//LogOutput.print(onto.getClassesInSignature().size());

		//CLASSES				
		LogOutput.print("\nCLASSES: " + onto.getClassesInSignature(true).size());

		//TODO
		//We need to add something
		//Was already fixed in a different place?
		//if (onto.getClassesInSignature(true).size()==0){
		//	OWLClass cls = SynchronizedOWLManager.createOWLDataFactory().getOWLClass(IRI.create("http://logmap.cs.ox.ac.uk/ontologies#TopClass"));
		//	precessLexiconClasses(cls, extractLabels);			
		//}
		//else {

		for (OWLClass cls : onto.getClassesInSignature(true)){ //we also add imports


			if (!cls.isTopEntity() && !cls.isBottomEntity()){// && !ns_ent.equals(oboinowl)){

				processLexiconClasses(cls, extractLabels);

			}

		}//end For classes

		//System.out.println("NUM RDF LABEL: "+num_labels);


		//}//end If size classes


		//DATA PROPERTIES
		processLexiconDataProperties(extractLabels);



		//OBJECT PROPERTIES
		processLexiconObjectProperties(extractLabels);



		//INDIVIDUALS
		if (Parameters.perform_instance_matching){
			processNamedIndividuals(extractLabels);
		}




		LogOutput.print("Number of classes/labels: " + index.getSizeIndexClasses()); //Ojo it is a total
		LogOutput.print("\tNumber of labels + syn: " + num_syn);
		LogOutput.print("\tCases with huge combination of synonyms: " + toohigh_synset_cases);
		LogOutput.print("\tNumber of entries inverted file (exact): " + invertedFileExact.size());

		LogOutput.print("Number of dProp: " + index.getSizeDataProperties()); //Ojo it is a total
		LogOutput.print("\tNumber of dProp inverted file: " + invertedFileExactDataProp.size());

		LogOutput.print("Number of oProp: " + index.getSizeObjectProperties()); //Ojo it is a total
		LogOutput.print("\tNumber of oProp inverted file: " + invertedFileExactObjProp.size());

		LogOutput.print("Number of Indiv: " + index.getSizeIndexIndividuals()); //Ojo it is a total
		LogOutput.print("\tNumber of Indiv inverted file: " + invertedFileIndividuals.size());
		LogOutput.print("\tNumber of Indiv weak inverted file: " + invertedFileWeakIndividuals.size());
		LogOutput.print("\tNumber of Indiv Role assertions inverted file: " + invertedFileRoleassertions.size());




		/*int exactIFAmb=0;
		for (Set<String> setstr : invertedFileExact.keySet()){
			//LogOutput.print(setstr);
			if (invertedFileExact.get(setstr).size()>1)
				exactIFAmb++;
		}
		LogOutput.print("Ambiguity IF exact: " + exactIFAmb);*/


		//Labels are already indexed so we can safely remove annotations form onto
		//managerOnto.applyChanges(listchanges);


		/*for (int i=1900; i<2000; i++){
			if (identifier2ClassIndex.get(i).getAlternativeLabels().size()>1){
				LogOutput.print(i);
				LogOutput.print("\t"+identifier2ClassIndex.get(i).getLabel());
				LogOutput.print("\t"+identifier2ClassIndex.get(i).getEntityName());
				LogOutput.print("\t"+identifier2ClassIndex.get(i).getAlternativeLabels());
			}
		}*/


		fin = Calendar.getInstance().getTimeInMillis();
		LogOutput.print("Time setting labels and inverted files (s): " + (float)((double)fin-(double)init)/1000.0);




	}



	private void processLexiconClasses(OWLClass cls, boolean extractLabels) {

		int ident;

		String ns_ent;
		String name;

		ns_ent=Utilities.getNameSpaceFromURI(cls.getIRI().toString());

		ident = index.addNewClassEntry();

		index.setOntologyId4Class(ident, id_onto);

		name = Utilities.getEntityLabelFromURI(cls.getIRI().toString());

		//if (name.equals("Mouse_Coccyx"))
		//	LogOutput.print("Here");

		index.setClassName(ident, name);

		//We store ns only if it is different to the ontology ns
		if (!ns_ent.equals("") && !ns_ent.equals(iri_onto)){
			index.setClassNamespace(ident, ns_ent);
		}


		/*LogOutput.print("\nENT:");
		LogOutput.print(iri_onto);
		LogOutput.print(cls.getIRI().toString());
		LogOutput.print(identifier2ClassIndex.get(ident).getNamespace());
		LogOutput.print(identifier2ClassIndex.get(ident).getEntityName());*/


		//We may need this structure for the taxonomy extraction...
		class2identifier.put(cls, ident);
		identifier2class.put(ident, cls);
		className2Identifier.put(name, ident);

		// Alessandro 21 April 2014
		classIri2identifier.put(cls.getIRI().toString(), ident);

		//Extract labels and alternative labels and create IFs
		if (extractLabels)
			createEntryInLexicalInvertedFiles4ClassLabels(cls, ident);

		//if (ident>30)
		//	break;


	}


	private void processLexiconDataProperties() {
		processLexiconDataProperties(true);
	}


	private void processLexiconDataProperties(boolean extractLabels) {


		List<String> cleanWords;
		String label;

		int ident;

		String ns_ent;
		String name;

		//DATA PROPERTIES
		for (OWLDataProperty dProp : onto.getDataPropertiesInSignature(true)){ //also imports

			ns_ent=Utilities.getNameSpaceFromURI(dProp.getIRI().toString());

			ident = index.addNewDataPropertyEntry();	

			index.setOntologyId4DataProp(ident, id_onto);

			name = Utilities.getEntityLabelFromURI(dProp.getIRI().toString());

			index.setDataPropName(ident, name);

			dataPropName2Identifier.put(name, ident);


			//LogOutput.print(name);

			//We store ns only if it is different to the ontology ns
			if (!ns_ent.equals("") && !ns_ent.equals(iri_onto)){
				index.setDataPropNamespace(ident, ns_ent);
			}

			//TODO: labels might be in an annotation!!
			//Extract labels and alternative labels and create IFs
			cleanWords = processLabel(name);
			if (cleanWords.size()>0){
				if (extractLabels){
					invertedFileExactDataProp.put(new HashSet<String>(cleanWords), ident);
				}
			}

			label="";
			for (String word : cleanWords){
				label=label + word;				
			}			
			//label without spaces
			index.setDataPropLabel(ident, label);
			index.addAlternativeDataPropertyLabel(ident, label);
			cleanWords.clear();


			//Create alternative labels
			//--------------------------------
			List<String> cleanWordsAlternative = createAlternativeLabel(name);
			if (cleanWordsAlternative.size()>0){

				//Note that we do not add alternative label to inverted file
				//Inverted file only contains exact entries from main labels

				label=""; //label without spaces
				for (String word : cleanWordsAlternative){
					label=label + word;				
				}
				index.addAlternativeDataPropertyLabel(ident, label);
			}
			cleanWordsAlternative.clear();


			//LogOutput.print(name);

			//Process domain
			//--------------------------------
			for (OWLClassExpression clsexp : dProp.getDomains(onto)){
				if (!clsexp.isAnonymous()){
					if (class2identifier.containsKey(clsexp.asOWLClass())){
						index.addDomainClass4DataProperty(ident, class2identifier.get(clsexp.asOWLClass()));
					}
				}
				else if (clsexp.getClassExpressionType()==ClassExpressionType.OBJECT_UNION_OF){									
					for (OWLClassExpression clsexpunion : clsexp.asDisjunctSet()){
						if (!clsexpunion.isAnonymous()){
							if (class2identifier.containsKey(clsexpunion.asOWLClass())){
								index.addDomainClass4DataProperty(ident, class2identifier.get(clsexpunion.asOWLClass()));
							}
						}
					}
				}
			}

			String range_type;

			//Process data ranges
			//--------------------------------
			for (OWLDataRange type : dProp.getRanges(onto)){
				if (type.isDatatype()){
					//LogOutput.print(type.asOWLDatatype() + " " + type.asOWLDatatype().getIRI() + " " + type.asOWLDatatype().isBuiltIn() +"  " +
					//		Utilities.getEntityLabelFromURI(type.asOWLDatatype().getIRI().toString()));

					try{
						if (type.asOWLDatatype().isBuiltIn()){
							range_type = type.asOWLDatatype().getBuiltInDatatype().getShortName();
						}
						else{//we extract name from iri
							range_type =  Utilities.getEntityLabelFromURI(Utilities.getEntityLabelFromURI(type.asOWLDatatype().getIRI().toString()));
						}
					}
					catch (Exception e){ //In some cases the datatype is not built in an rises an error
						range_type =  Utilities.getEntityLabelFromURI(Utilities.getEntityLabelFromURI(type.asOWLDatatype().getIRI().toString()));
					}
					index.addRangeType4DataProperty(ident, range_type);
				}

			}


			//LogOutput.print("\tDOMAINS: " + identifier2DataPropIndex.get(ident).getDomainClassIndexes());
			//LogOutput.print("\tRANGES: " + identifier2DataPropIndex.get(ident).getRangeTypes());


			ident++;			

		}


	}

	private void processLexiconObjectProperties(){
		processLexiconObjectProperties(true);
	}

	private void processLexiconObjectProperties(boolean extractLabels){

		List<String> cleanWords;
		String label;

		int ident;

		String ns_ent;
		String name;


		//OBJECT PROPERTIES
		for (OWLObjectProperty oProp : onto.getObjectPropertiesInSignature(true)){//also imports

			ns_ent=Utilities.getNameSpaceFromURI(oProp.getIRI().toString());


			ident = index.addNewObjectPropertyEntry();

			index.setOntologyId4ObjectProp(ident, id_onto);

			name = Utilities.getEntityLabelFromURI(oProp.getIRI().toString());

			index.setObjectPropName(ident, name);

			objectPropName2Identifier.put(name, ident);


			//identifier2ObjPropIndex.get(ident).setLabel(name);

			//LogOutput.print(name);


			//We store ns only if it is different to the ontology ns
			if (!ns_ent.equals("") && !ns_ent.equals(iri_onto)){
				index.setObjectPropNamespace(ident, ns_ent);
			}

			//TODO Labels might be in an annotation!!
			//Extract labels and alternative labels and create IFs
			cleanWords = processLabel(name);
			if (cleanWords.size()>0){
				if (extractLabels){
					invertedFileExactObjProp.put(new HashSet<String>(cleanWords), ident);
				}
			}

			label=""; //label without spaces
			for (String word : cleanWords){
				label=label + word;				
			}
			index.setObjectPropLabel(ident, label);
			index.addAlternativeObjectPropertyLabel(ident, label);
			cleanWords.clear();



			//Create alternative labels
			//--------------------------------
			List<String> cleanWordsAlternative = createAlternativeLabel(name);
			if (cleanWordsAlternative.size()>0){

				//Note that we do not add alternative label to inverted file
				//Inverted file only contains exact entries from main labels

				label=""; //label without spaces
				for (String word : cleanWordsAlternative){
					label=label + word;				
				}
				index.addAlternativeObjectPropertyLabel(ident, label);
			}
			cleanWordsAlternative.clear();


			//Process Inverse properties and extend labels
			//--------------------------------------------
			String inverse_name;
			List<String> cleanWordsInverse;
			for (OWLObjectPropertyExpression propexp : oProp.getInverses(onto)){
				if (!propexp.isAnonymous()){
					inverse_name = Utilities.getEntityLabelFromURI(
							propexp.asOWLObjectProperty().getIRI().toString());

					//Reuse name of inverse property to create an alternative label for the property
					cleanWordsInverse = processInverseLabel(inverse_name);

					if (cleanWordsInverse.size()>0){

						//Note that we do not add alternative label to inverted file
						//Inverted file only contains exact entries from main labels


						label=""; //label without spaces
						for (String word : cleanWordsInverse){
							label=label + word;				
						}
						index.addAlternativeObjectPropertyLabel(ident, label);
					}
					cleanWordsInverse.clear();
				}
			}


			//Process domains
			//--------------------------------------------
			for (OWLClassExpression clsexp : oProp.getDomains(onto)){

				if (!clsexp.isAnonymous()){
					if (class2identifier.containsKey(clsexp.asOWLClass())){
						index.addDomainClass4ObjectProperty(ident, class2identifier.get(clsexp.asOWLClass()));
					}
				}
				else if (clsexp.getClassExpressionType()==ClassExpressionType.OBJECT_UNION_OF){									
					for (OWLClassExpression clsexpunion : clsexp.asDisjunctSet()){
						if (!clsexpunion.isAnonymous()){
							if (class2identifier.containsKey(clsexpunion.asOWLClass())){
								index.addDomainClass4ObjectProperty(ident, class2identifier.get(clsexpunion.asOWLClass()));
							}
						}
					}
				}

			}


			//Process ranges
			//--------------------------------------------
			for (OWLClassExpression clsexp : oProp.getRanges(onto)){
				if (!clsexp.isAnonymous()){
					if (class2identifier.containsKey(clsexp.asOWLClass())){
						index.addRangeClass4ObjectProperty(ident, class2identifier.get(clsexp.asOWLClass()));
					}
				}
				else if (clsexp.getClassExpressionType()==ClassExpressionType.OBJECT_UNION_OF){									
					for (OWLClassExpression clsexpunion : clsexp.asDisjunctSet()){
						if (!clsexpunion.isAnonymous()){
							if (class2identifier.containsKey(clsexpunion.asOWLClass())){
								index.addRangeClass4ObjectProperty(ident, class2identifier.get(clsexpunion.asOWLClass()));
							}
						}
					}
				}
			}

			//LogOutput.print(name);
			//LogOutput.print("\tDOMAINS: " + identifier2ObjPropIndex.get(ident).getDomainClassIndexes());
			//LogOutput.print("\tRANGES: " + identifier2ObjPropIndex.get(ident).getRangeClassIndexes());

			ident++;	//Is it necessary?	

		}

	}


	private void processNamedIndividuals(){
		processNamedIndividuals(true);
	}

	private void processNamedIndividuals(boolean extractLabels){

		Set<String> cleanWords = new HashSet<String>();
		List<String> cleanWordsList;

		String label;

		int ident;

		String ns_ent;
		String name;


		Set<String> altLabels = new HashSet<String>(); 


		String longestALabel;


		int num_dummy_indiv = 0;
		Set<OWLNamedIndividual> dummyIndividualsSet = new HashSet<OWLNamedIndividual>();



		for (OWLNamedIndividual indiv : onto.getIndividualsInSignature(true)){//also imports

			ns_ent=Utilities.getNameSpaceFromURI(indiv.getIRI().toString());

			ident = index.addNewIndividualEntry();

			index.setOntologyId4Individual(ident, id_onto);

			inidividual2identifier.put(indiv, ident);


			//Name in URI
			name = Utilities.getEntityLabelFromURI(indiv.getIRI().toString());
			index.setIndividualName(ident, name);
			//We add a better label below if there are alternative labels
			index.setIndividualLabel(ident, name);

			individualName2Identifier.put(name, ident);

			//We store ns only if it is different to the ontology ns
			if (!ns_ent.equals("") && !ns_ent.equals(iri_onto)){
				index.setIndividualNamespace(ident, ns_ent);
			}


			//IF DUMMY INDIVIDUAL THEN DO NOT ADD ALTERNATIVE LABELS
			//In OAEI 2013 the ones that are referenced from property "http://www.instancematching.org/label"
			if (roleAssertionLabelsExtractor.isDummyIndividual(indiv)){
				LogOutput.print("DUMMY individual: " + indiv.getIRI().toString());
				num_dummy_indiv++;
				dummyIndividualsSet.add(indiv);
				continue;
			}




			//EXTRACT ALTERNATIVE LABELS FOR INDIVIDUAL
			///-------------------------------------------
			altLabels.add(name);


			//We add accepted assertions associated to the individual 
			//altLabels.addAll(extractNameFromDataAssertion(indiv));
			altLabels.addAll(roleAssertionLabelsExtractor.extractLexiconFromRoleAssertions(indiv)); //new


			//We add accepted annotations associated to the individual
			altLabels.addAll(extractAnnotations4Infividual(indiv));


			//add weak entries

			longestALabel="";

			//We treat Alt labels for individual: so far datatype assertion name and URI
			for (String alabel : altLabels){

				if (!isLabelAnIdentifier(alabel)){

					if (alabel.length()>longestALabel.length()){
						longestALabel=alabel;
					}

					index.addAlternativeIndividualLabel(ident, alabel.toLowerCase()); //lower case? I guess it is the same...

					cleanWordsList = processLabel(alabel, true); //sopwords!! //and lowecase

					//Add altlabels with order changed
					changeOrderAltLabelWords(ident, cleanWordsList);

					//wE do not want order in IFs entries
					cleanWords.addAll(cleanWordsList);


					//Weak IF
					String stemmedWord;
					for (String word : cleanWords){

						if (extractLabels){

							if (!invertedFileWeakIndividuals.containsKey(word))
								invertedFileWeakIndividuals.put(word, new HashSet<Integer>());

							invertedFileWeakIndividuals.get(word).add(ident);

							//Add stemmed word
							stemmedWord = lexicalUtilities.getStemming4Word(word);
							if (stemmedWord.length()>2){ //minimum 3 characters

								if (!invertedFileWeakIndividuals.containsKey(stemmedWord))
									invertedFileWeakIndividuals.put(stemmedWord, new HashSet<Integer>());

								invertedFileWeakIndividuals.get(stemmedWord).add(ident);

							}
						}

					}


					//Exact IF
					if (cleanWords.size()>0){

						if (extractLabels){
							if (!invertedFileIndividuals.containsKey(cleanWords))
								invertedFileIndividuals.put(new HashSet<String>(cleanWords), new HashSet<Integer>());

							//LogOutput.print(invertedFileExactIndividuals.get(cleanWords) + " "+cleanWords + " "+ ident);
							invertedFileIndividuals.get(cleanWords).add(ident);
						}

						cleanWords.clear();
						cleanWordsList.clear();//not really necessary

					}

				}//if ident

			}//end for alabels

			//Deprecated: seems to be wrong
			//We add an alternative label including all alternative labels
			//This will only be used to extract isub score
			//-------------------------------
			/*String new_alabel="";
			for (String altLabel : index.getAlternativeLabels4IndividualIndex(ident)){
				new_alabel += altLabel + " ";
			}
			new_alabel = new_alabel.trim();
			if (new_alabel.length()>3)
				index.addAlternativeIndividualLabel(ident, new_alabel);
			 */



			//We add a better label
			if (!longestALabel.equals("")){
				index.setIndividualLabel(ident, longestALabel);
			}


			//altLabels.clear();


			//Extract class types.
			//We only extract direct types. Inferred types will be extracted later when setting up the taxonomic data.					
			for (OWLClassExpression clsexp : indiv.getTypes(onto)){

				if (!clsexp.isAnonymous()){
					if (class2identifier.containsKey(clsexp.asOWLClass())){
						index.addType4Individual(ident, class2identifier.get(clsexp.asOWLClass()));
					}
				}
				else if (clsexp.getClassExpressionType()==ClassExpressionType.OBJECT_UNION_OF){									
					for (OWLClassExpression clsexpunion : clsexp.asDisjunctSet()){
						if (!clsexpunion.isAnonymous()){
							if (class2identifier.containsKey(clsexpunion.asOWLClass())){
								index.addType4Individual(ident, class2identifier.get(clsexpunion.asOWLClass()));
							}
						}
					}
				}
				//other not considered yet

			}





			//In main extract instances and assess....

			//LogOutput.print("INDIVIDUALS " + id_onto + ", " +ident);
			/*
			//LogOutput.print("\t" + indiv.getIRI().toString());
			//LogOutput.print("\t" + index.getName4IndividualIndex(ident));
			//LogOutput.print("\t" + index.getLabel4IndividualIndex(ident));
			LogOutput.print("\t" + index.getIRIStr4IndividualIndex(ident));
			LogOutput.print("\t" + altLabels);

			LogOutput.print("\tTYPES: " + index.getIndividualClassTypes4Identifier(ident));
			LogOutput.print("\tIF size: " + invertedFileExactIndividuals.size());
			if (invertedFileExactIndividuals.size()<5){
				LogOutput.print("\tIF size: " + invertedFileExactIndividuals);
			}*/

			altLabels.clear();


		}//end for individuals



		//Another for to extract lexicon from other labels (relationships with other indiv or data)
		//Must be after wars since we require "inidividual2identifier"

		if (extractLabels){

			for (OWLNamedIndividual indiv : onto.getIndividualsInSignature(true)){//also imports

				//We do not want to match dummy individuals!
				if (dummyIndividualsSet.contains(indiv))
					continue;

				for (String str_label : roleAssertionLabelsExtractor.extractExtendedLexiconFromRoleAssertions(indiv)){


					if (!invertedFileRoleassertions.containsKey(str_label))
						invertedFileRoleassertions.put(str_label, new HashSet<Integer>());

					invertedFileRoleassertions.get(str_label).add(inidividual2identifier.get(indiv));

				}			

			}
		}

		//not necessary any more
		inidividual2identifier.clear();
		dummyIndividualsSet.clear();

		LogOutput.print("NUMBER OF DUMMY INDIVIDUALS: " + num_dummy_indiv);
		//LogOutput.printAlways("NUMBER OF DUMMY INDIVIDUALS: " + num_dummy_indiv);


		LogOutput.print("MAX SIZE ANNOTATIONS: " + roleAssertionLabelsExtractor.max_size_name_label);
		LogOutput.print("MIN SIZE ANNOTATIONS: " + roleAssertionLabelsExtractor.min_size_name_label);

	}


	/**
	 * Changes order of alternative words labels
	 */
	private void changeOrderAltLabelWords(int ident, List<String> cleanWordsList){

		String original="";
		String changed="";

		//Add same altlabel without 
		if (cleanWordsList.size()>0){

			original = cleanWordsList.get(0);
			for (int i=1; i<cleanWordsList.size(); i++) {
				original += " " + cleanWordsList.get(i);
			}

			changed = cleanWordsList.get(cleanWordsList.size()-1);
			for (int i=cleanWordsList.size()-2; i>-1; i--) {
				changed += " " + cleanWordsList.get(i);
			}

			index.addAlternativeIndividualLabel(ident, original);
			index.addAlternativeIndividualLabel(ident, changed);

		}


	}




	Set<String> lexiconValues4individual = new HashSet<String>();

	/**
	 * See Class @ExtractAcceptedLabelsFromRoleAssertions
	 * @deprecated
	 */
	private Set<String> extractNameFromDataAssertion(OWLNamedIndividual indiv){

		lexiconValues4individual.clear();

		String label_value;


		//roleAsserationExtractor

		//TODO
		//ceate class

		for (String uri_indiv_ann : Parameters.accepted_data_assertion_URIs_for_individuals){

			for (OWLLiteral assertion_value : indiv.getDataPropertyValues(
					index.getFactory().getOWLDataProperty(IRI.create(uri_indiv_ann)), onto)){

				label_value = assertion_value.getLiteral().toLowerCase();

				if (label_value.length()>2){
					lexiconValues4individual.add(label_value);
				}

			}
		}

		return lexiconValues4individual;


	}


	private Set<String> extractAnnotations4Infividual(OWLNamedIndividual indiv){

		lexiconValues4individual.clear();

		String label_value;

		for (OWLAnnotationAssertionAxiom indivAnnAx : indiv.getAnnotationAssertionAxioms(onto)){

			label_value = annotationExtractor.getAnntotationString(indivAnnAx);

			if (label_value.length()>2){
				lexiconValues4individual.add(label_value);
			}

		}//end class ann axioms

		return lexiconValues4individual;
	}




	private List<String> processLabel(String label){
		return processLabel(label, false);
	}


	/**
	 * This processes the property label and alternative labels, and create inverted files
	 * Used for properties and individuals. Classes have a more sophisticaed processing
	 * @param label
	 * @param ident
	 */
	private List<String> processLabel(String label, boolean filterStopwords){

		String label_value;
		List<String> cleanWords = new ArrayList<String>();
		String[] words;

		label_value=label.replace(",", "");
		label_value=label.replace("-", "");

		if (label_value.indexOf("_")>0){
			words=label_value.split("_");
		}
		else if (label_value.indexOf(" ")>0){ 
			words=label_value.split(" ");
		}
		//Split capitals...
		else{
			words=Utilities.splitStringByCapitalLetter(label_value);
		}

		//shift=1;				

		label_value="";

		for (int i=0; i<words.length; i++){

			words[i]=words[i].toLowerCase(); //to lower case

			//We optionally filter stopwords
			if (words[i].length()>0 && (!filterStopwords || !lexicalUtilities.getStopwordsSet().contains(words[i]))){ 
				cleanWords.add(words[i]);
			}			
		}		






		return cleanWords;

	}

	/**
	 * This class processes the label of P2 (an inverse property of P1) and
	 * returns a candidate alternative label for P1.
	 * e.g: hasPart -> partOf
	 * e.g: authorOf -> hasAuthor 
	 * e.g. writtenBy -> written (would be similar to writes)
	 * e.g. isGivenBy ->given (would be closer to gives) 
	 * @param label
	 * @return
	 */
	private List<String> processInverseLabel(String label){

		List<String> words = processLabel(label);

		int lastIndex = words.size()-1;

		String firstWord = words.get(0).toLowerCase();
		String lastWord = words.get(lastIndex).toLowerCase();

		//LogOutput.printAlways(words.toString());

		if (firstWord.equals("has")){
			words.remove(0);
			words.add("of");
		}
		else if (lastWord.equals("of")){
			words.remove(lastIndex);
			words.add(0, "has");
		}
		else if (lastWord.equals("by")){
			words.remove(lastIndex);
			if (firstWord.equals("is")){
				words.remove(0);
			}
		}
		else{
			words.clear();
		}

		//LogOutput.printAlways(words.toString());

		return words;

	}


	/**
	 * Create alternative label for a given property
	 * e.g. hasName -> name
	 * e.g. isReviewedBy - > hasReviewer
	 * @param label
	 * @return
	 */
	private List<String> createAlternativeLabel(String label){

		List<String> words = processLabel(label);

		int lastIndex = words.size()-1;

		String firstWord = words.get(0).toLowerCase();
		String lastWord = words.get(lastIndex).toLowerCase();

		//LogOutput.printAlways(words.toString());

		if (firstWord.equals("has")){
			words.remove(0);
		}
		else if (lastWord.equals("by")){
			words.remove(lastIndex);
			if (firstWord.equals("is")){
				words.remove(0);
			}
			words.add(0, "has");
		}
		else{
			words.clear();
		}

		//LogOutput.printAlways(words.toString());

		return words;



	}







	/**
	 * This methid extracts and processes the class label and alternative labels, and create inverted files
	 * @param cls
	 */
	private void createEntryInLexicalInvertedFiles4ClassLabels(OWLClass cls, int ident){


		Set<String> cleanWords = extractCleanLabel4OWLCls(cls, ident);
		Set<String> stemmed_words=new HashSet<String>();


		String[] words;

		String cleanAltLabel;

		String cleanReverseAltLabel;

		String stemmedWord;



		//From label
		//we also add cases one word in case they should be matched to combo
		if (cleanWords.size()>0){
			if (!invertedFileExact.containsKey(cleanWords))
				invertedFileExact.put(new HashSet<String>(cleanWords), new HashSet<Integer>());

			invertedFileExact.get(cleanWords).add(ident);

		}

		//lala

		for (String str : cleanWords){

			stemmedWord = lexicalUtilities.getStemming4Word(str);
			if (!stemmedWord.isEmpty()){
				stemmed_words.add(stemmedWord);
			}
			//if (lexicalUtilities.getStemming4Word(str).equals("")){
			//	LogOutput.print(str + "->  stemming: '" + lexicalUtilities.getStemming4Word(str) +"'");
			//}

		}

		if (!invertedFileWeakLabelsStemming.containsKey(stemmed_words))
			invertedFileWeakLabelsStemming.put(new HashSet<String>(stemmed_words), new HashSet<Integer>());

		invertedFileWeakLabelsStemming.get(stemmed_words).add(ident);

		//We add to internal structure
		//identifier2stemmedlabel.put(ident, new ArrayList<String>(stemmed_words));
		identifier2stemmedlabels.put(ident, new HashSet<List<String>>());
		identifier2stemmedlabels.get(ident).add(new ArrayList<String>(stemmed_words));

		stemmed_words.clear();		
		cleanWords.clear();

		String stemmedAltLabel;

		Set<Integer> temp;

		//ALTERNATE LABELS
		for (String altlabel_value : extractAlternateLabels4OWLCls(cls, ident)){

			//TODO Lower case altlabel_value?? And not to lower case in annotation extraction??
			//TODO REvise this with time...


			cleanAltLabel = "";
			cleanReverseAltLabel = "";
			stemmedAltLabel = "";

			if (altlabel_value.length()>2){

				words=altlabel_value.split("_");  //Already pre-processed to be '_'

				for (int i=0; i<words.length; i++){

					if (!lexicalUtilities.getStopwordsSet().contains(words[i]) && words[i].length()>0){ //words[i].length()>2 &&  Not for exact if: it may contain important numbers

						cleanWords.add(words[i]);

						if (cleanAltLabel.length()==0){
							cleanAltLabel = words[i];
							cleanReverseAltLabel = words[i];
						}
						else {
							cleanAltLabel+= "_" + words[i];
							cleanReverseAltLabel = words[i] + "_" + cleanReverseAltLabel;
						}

						stemmedWord = lexicalUtilities.getStemming4Word(words[i]);
						if (stemmedWord.isEmpty())
							continue;

						stemmedAltLabel += "_" + stemmedWord;
						/*
						if (frequency4words.containsKey(words[i]))
							frequency4words.put(words[i], 1);
						else 
							frequency4words.put(words[i], frequency4words.get(words[i]) + 1);
						 */

						index.addWordOccurrence(stemmedWord, ident);

					}
				}//end words

				if (cleanWords.size()>0){
					if (!invertedFileExact.containsKey(cleanWords))
						invertedFileExact.put(new HashSet<String>(cleanWords), new HashSet<Integer>());

					invertedFileExact.get(cleanWords).add(ident);



					//We add to altrnative labels
					index.addAlternativeClassLabel(ident, cleanAltLabel);
					if (Parameters.reverse_labels){ //isub score slightly changes if labels are reversed
						index.addAlternativeClassLabel(ident, cleanReverseAltLabel);
					}
					if (!stemmedAltLabel.isEmpty()){
						//System.out.println(stemmedAltLabel);
						index.addStemmedAltClassLabel(ident, (stemmedAltLabel = stemmedAltLabel.substring(1)));
					}
				}


				//STEMMING
				for (String str : cleanWords){

					stemmedWord = lexicalUtilities.getStemming4Word(str);
					if (!stemmedWord.isEmpty()){
						stemmed_words.add(stemmedWord);
					}
					//if (lexicalUtilities.getStemming4Word(str).equals("")){
					//	LogOutput.print(str + "->  stemming: '" + lexicalUtilities.getStemming4Word(str) + "'" + stemmedWord.isEmpty());
					//}

				}

				if (!invertedFileWeakLabelsStemming.containsKey(stemmed_words))
					invertedFileWeakLabelsStemming.put(new HashSet<String>(stemmed_words), new HashSet<Integer>());

				invertedFileWeakLabelsStemming.get(stemmed_words).add(ident);

				//We add stemmin also for weak
				if (use_all_labels_for_weak_mappings)
					identifier2stemmedlabels.get(ident).add(new ArrayList<String>(stemmed_words));


				stemmed_words.clear();		
				cleanWords.clear();
			}

		}//Alt labels






	}


	/**
	 * Do not consider class names if they are identifiers
	 * @return
	 */
	private boolean isLabelAnIdentifier(String label_value){

		return label_value.matches(".+[0-9][0-9][0-9]+") 
				|| label_value.matches("[0-9][0-9][0-9][0-9][0-9]+-[0-9]+") //library ontologies
				//|| label_value.matches("[0-9]+\\.[0-9]+(\\.[0-9]+)+");//library ontologies
				|| label_value.matches("[0-9]+(\\.[0-9]+)+")
				|| label_value.matches(".+[0-9]+.+[0-9]+.+[0-9]+.+");//instance matching ontologies

	}


	/**
	 * Creates entry in exact occurrences map and adds label to class index
	 * @param cls
	 * @param ident
	 * @return
	 */
	private Set<String> extractCleanLabel4OWLCls(OWLClass cls, int ident){

		String label_value="";

		String entry4ifexact;
		Set<String> cleanWords=new HashSet<String>();

		String[] words;

		int ambiguity_ident=1;

		//System.out.println(cls);
		//System.out.println(cls.getIRI().toString());

		String ann_label;

		//Check if concept name is meaningful (not an ID)
		label_value = index.getIdentifier2ClassIndexMap().get(ident).getEntityName(); 
		//if (label_value.matches(".+[0-9][0-9][0-9]+")){

		if (isLabelAnIdentifier(label_value)){

			//Otherwise We look for first non empty label (if no label we keepID)
			//---------------------------------------------------------------------
			for (OWLAnnotationAssertionAxiom annAx : cls.getAnnotationAssertionAxioms(onto)){

				//listchanges.add(new RemoveAxiom(onto, annAx)); //We remove all annotations

				if (annAx.getAnnotation().getProperty().getIRI().toString().equals(rdf_label_uri)){

					//LogOutput.print(((OWLLiteral)annAx.getAnnotation().getValue()).getLiteral());
					ann_label=((OWLLiteral)annAx.getAnnotation().getValue()).getLiteral();//.toLowerCase();

					if (!ann_label.equals("") && ann_label!=null){
						label_value=ann_label;
					}

					//LogOutput.print("Label: " + label_value);
					break;

				}

			}

		}




		//If it doesn't exist then we use entity name
		//if (label_value.equals("")){
		//	label_value=index.getIdentifier2ClassIndexMap().get(ident).getEntityName();
		//}


		//System.out.println(label_value);

		label_value=label_value.replace(",", "");


		if (label_value.startsWith("_")){
			label_value = label_value.substring(1, label_value.length());
		}
		if (label_value.endsWith("_")){
			label_value = label_value.substring(0, label_value.length()-1);
		}


		if (label_value.indexOf("_")>0){ //NCI and SNOMED
			words=label_value.split("_");
		}
		else if (label_value.indexOf(" ")>0){ //FMA
			words=label_value.split(" ");
		}
		//Split capitals...
		else{
			//label_value = Utilities.capitalPrepositions(label_value);
			words=Utilities.splitStringByCapitalLetter(label_value);
		}
		//else {
		//	words=new String[1];
		//	words[0]=label_value;
		//}

		//To lowercase




		//shift=1;				

		label_value="";
		entry4ifexact="";
		for (int i=0; i<words.length; i++){

			words[i]=words[i].toLowerCase(); //to lower case

			if (words[i].length()>0){

				//For IF
				entry4ifexact+=words[i];

				//For label
				label_value+=words[i] + "_";

				if (!lexicalUtilities.getStopwordsSet().contains(words[i])){ 
					//words[i].length()>2 &&  Not for exact IF: it may contain important numbers					
					cleanWords.add(words[i]);
				}				
			}			
		}


		//Check length!! or if it contains "_"
		if (label_value.length()>0){

			label_value = label_value.substring(0, label_value.length()-1);

			//Add to class index
			index.setClassLabel(ident, label_value);
		}
		else{
			//we add the whole IRI
			index.setClassLabel(ident, cls.getIRI().toString());
		}

		//System.out.println(cls.getIRI());
		//System.out.println(label_value);
		//System.out.println(cleanWords);

		return cleanWords;

		//Add to IF: not used any more
		/*if (entry4ifexact.length()>2){//Min 3 characteres

			//Ambiguity in labels of ontology (use concept name)
			if (extact_occurrence_entries.containsKey(entry4ifexact)){ 

				ambiguity_ident=extact_occurrence_entries.get(entry4ifexact);
				//We remove previous one
				extact_occurrence_entries.remove(entry4ifexact);

				//We create two news
				extact_occurrence_entries.put(getProcessedName4ConceptIndex(ambiguity_ident), ambiguity_ident);
				extact_occurrence_entries.put(getProcessedName4ConceptIndex(ident), ident);



			}
			else{
				extact_occurrence_entries.put(entry4ifexact, ident);
			}
		}*/



	}







	private Set<String> labels_set = new HashSet<String>();

	private void considerLabel(String label_value){

		labels_set.addAll(extendAlternativeLabel(label_value));

		//Expand with UMLS Lex Spelling variants
		if (lexicalUtilities.hasSpellingVariants(label_value)){
			//if (spelling_variants_map.containsKey(label_value)){
			for (String variant : lexicalUtilities.getSpellingVariants(label_value)){
				//for (String variant : spelling_variants_map.get(label_value)){
				labels_set.addAll(extendAlternativeLabel(variant));
			}
		}
	}





	/**
	 * Return alternate labels
	 * They could be represented using different annotation properties
	 * @param cls
	 * @return
	 */
	private Set<String> extractAlternateLabels4OWLCls(OWLClass cls, int ident){


		String label_value="";
		OWLAnonymousIndividual geneid_value;

		OWLNamedIndividual namedIndiv=null;
		IRI namedIndivIRI;


		//REINIT for each label
		labels_set.clear();


		//Use concept name as well!! Some times it is different to label
		//Avoid identifiers as concept names
		label_value = index.getIdentifier2ClassIndexMap().get(ident).getEntityName().toLowerCase();

		//if (!label_value.matches(".+[0-9][0-9][0-9]+")){
		if (!isLabelAnIdentifier(label_value)){

			//And extend with variants
			considerLabel(label_value);

			//LogOutput.print("name2Label: " + labels_set);

		}



		//LogOutput.print("GENIEID");

		for (OWLAnnotationAssertionAxiom clsAnnAx : cls.getAnnotationAssertionAxioms(onto)){

			//listchanges.add(new RemoveAxiom(onto, clsAnnAx)); //We remove all annotations


			label_value = annotationExtractor.getAnntotationString(clsAnnAx);

			if (label_value.length()>2){
				considerLabel(label_value);
			}

		}//end class ann axioms

		//LogOutput.print("\t" + labels_set.size());
		num_syn=num_syn+labels_set.size();

		return labels_set;



	}


	//int num_labels=0;

	/**
	 * 
	 * @deprecated
	 * 
	 */
	private Set<String> extractAlternateLabels4OWLCls2(OWLClass cls, int ident){

		Set<String> labels_set = new HashSet<String>();
		String label_value="";
		OWLAnonymousIndividual geneid_value;

		OWLNamedIndividual namedIndiv=null;
		IRI namedIndivIRI;

		//OWLNamedIndividual geneid_value;
		//String[] words;

		//int shift=1;

		//Use concept name as well!! Some times it is different to label
		//Avoid identifiers as concept names
		label_value = index.getIdentifier2ClassIndexMap().get(ident).getEntityName().toLowerCase(); 

		if (!isLabelAnIdentifier(label_value)){
			//if (!label_value.matches(".+[0-9][0-9][0-9]+")){

			labels_set.addAll(extendAlternativeLabel(label_value));
			//LogOutput.print(label_value + " " + labels_set.size());
			//LogOutput.print("\t" + labels_set);


			//Expand with UMLS Lex Spelling variants
			if (lexicalUtilities.hasSpellingVariants(label_value)){
				//if (spelling_variants_map.containsKey(label_value)){
				for (String variant : lexicalUtilities.getSpellingVariants(label_value)){
					//for (String variant : spelling_variants_map.get(label_value)){
					labels_set.addAll(extendAlternativeLabel(variant));
				}
			}

			//LogOutput.print("name2Label: " + labels_set);

		}



		//LogOutput.print("GENIEID");

		for (OWLAnnotationAssertionAxiom clsAnnAx : cls.getAnnotationAssertionAxioms(onto)){

			//listchanges.add(new RemoveAxiom(onto, clsAnnAx)); //We remove all annotations

			//num_labels++;

			if (clsAnnAx.getAnnotation().getProperty().getIRI().toString().equals(rdf_label_uri)){


				//LogOutput.print(((OWLLiteral)annAx.getAnnotation().getValue()).getLiteral());
				label_value=((OWLLiteral)clsAnnAx.getAnnotation().getValue()).getLiteral().toLowerCase();

				//LogOutput.print("Label: " + label_value);

				if (label_value.length()>2){
					//labels_set.add(label_value);
					labels_set.addAll(extendAlternativeLabel(label_value));
					//LogOutput.print(label_value + " " + labels_set.size());
					//LogOutput.print("\t" + labels_set);


					//Expand with UMLS Lex Spelling variants
					if (lexicalUtilities.hasSpellingVariants(label_value)){
						//if (spelling_variants_map.containsKey(label_value)){
						for (String variant : lexicalUtilities.getSpellingVariants(label_value)){
							//for (String variant : spelling_variants_map.get(label_value)){
							labels_set.addAll(extendAlternativeLabel(variant));
						}
					}

				}

			}


			//Deal with these cases
			//TODO Create a new class where can be easily extended the was annotations are extracted!!
			//Currently in new class
			//<oboInOwl:hasRelatedSynonym rdf:datatype="http://www.w3.org/2001/XMLSchema#string">HGE</oboInOwl:hasRelatedSynonym>
			//<oboInOwl:hasExactSynonym rdf:datatype="http://www.w3.org/2001/XMLSchema#string">human granulocytic ehrlichiosis</oboInOwl:hasExactSynonym>


			//Annotations in MOuse Anatomy and NCI Anatomy
			if (clsAnnAx.getAnnotation().getProperty().getIRI().toString().equals(hasRelatedSynonym_uri)||
					clsAnnAx.getAnnotation().getProperty().getIRI().toString().equals(hasExactSynonym_uri)){

				try {
					//LogOutput.print("GENIEID");
					//LogOutput.print(clsAnnAx.getAnnotation());
					//LogOutput.print(clsAnnAx.getAnnotation().getValue());
					//It is an individual
					geneid_value=((OWLAnonymousIndividual)clsAnnAx.getAnnotation().getValue()).asOWLAnonymousIndividual();//.getID()

					for (OWLAnnotationAssertionAxiom annGeneidAx : onto.getAnnotationAssertionAxioms(geneid_value)){

						//listchanges.add(new RemoveAxiom(onto, annGeneidAx)); //We remove all annotations

						if (annGeneidAx.getAnnotation().getProperty().getIRI().toString().equals(rdf_label_uri)){

							label_value=((OWLLiteral)annGeneidAx.getAnnotation().getValue()).getLiteral().toLowerCase();

							if (label_value.length()>2){


								//labels_set.add(label_value);
								labels_set.addAll(extendAlternativeLabel(label_value));
								//LogOutput.print(label_value + " " + labels_set.size());
								//LogOutput.print("\t" + labels_set);

								//Expand with UMLS Lex Spelling variants
								if (lexicalUtilities.hasSpellingVariants(label_value)){
									for (String variant : lexicalUtilities.getSpellingVariants(label_value)){
										labels_set.addAll(extendAlternativeLabel(variant));
									}
								}

							}


						}
					}
				}
				catch (Exception e){
					LogOutput.printAlways("Error accessing annotation: hasRelatedSynonym_uri or hasExactSynonym_uri");
				}

			}

			//Annotations in NCI Full
			else if (clsAnnAx.getAnnotation().getProperty().getIRI().toString().equals(nci_synonym_uri)){

				label_value=((OWLLiteral)clsAnnAx.getAnnotation().getValue()).getLiteral().toLowerCase();

				if (label_value.length()>2){

					//LogOutput.print("\tSynonym nci: " + label_value);

					//LogOutput.print("\t" + label_value);
					//labels_set.add(label_value);
					labels_set.addAll(extendAlternativeLabel(label_value));
					//LogOutput.print(label_value + " " + labels_set.size());
					//LogOutput.print("\t" + labels_set);

					//Expand with UMLS Lex Spelling variants
					if (lexicalUtilities.hasSpellingVariants(label_value)){
						for (String variant : lexicalUtilities.getSpellingVariants(label_value)){
							labels_set.addAll(extendAlternativeLabel(variant));
						}
					}

				}					


			}

			//Annotations in FMA DL 2.0
			else if (clsAnnAx.getAnnotation().getProperty().getIRI().toString().equals(fma_synonym_uri)){

				//It is an individual
				namedIndivIRI=(IRI)clsAnnAx.getAnnotation().getValue();

				namedIndiv=index.getFactory().getOWLNamedIndividual(namedIndivIRI);


				//Synonym name is in data property assertion, not in annotation!!


				/*LogOutput.print("\tcls: " + cls.getIRI());
					LogOutput.print("\tindiv: " + namedIndivIRI);
					LogOutput.print("\tindiv: " + namedIndiv.getIRI());
					LogOutput.print("\tann: " + namedIndiv.getAnnotations(onto));
					LogOutput.print("\tdprop: " + namedIndiv.getDataPropertyValues(factory.getOWLDataProperty(IRI.create(fma_name_uri)), onto));
				 */
				if (namedIndiv==null)
					continue;



				//for (OWLAnnotation indivAnn : namedIndiv.getAnnotations(onto)){
				for (OWLLiteral literal_syn : namedIndiv.getDataPropertyValues(index.getFactory().getOWLDataProperty(IRI.create(fma_name_uri)), onto)){


					//listchanges.add(new RemoveAxiom(onto, indivAnnAx)); //We remove all annotations


					//LogOutput.print(literal_syn);

					label_value = literal_syn.getLiteral().toLowerCase();

					//if (indivAnn.getProperty().getIRI().toString().equals(fma_name_uri)){
					//label_value=((OWLLiteral)indivAnn.getValue()).getLiteral().toLowerCase();
					//}


					//LogOutput.print("\tSynonym FMA: " + label_value);

					if (label_value.length()>2){



						//labels_set.add(label_value);
						labels_set.addAll(extendAlternativeLabel(label_value));
						//LogOutput.print(label_value + " " + labels_set.size());
						//LogOutput.print("\t" + labels_set);

						//Expand with UMLS Lex Spelling variants
						if (lexicalUtilities.hasSpellingVariants(label_value)){
							for (String variant : lexicalUtilities.getSpellingVariants(label_value)){
								labels_set.addAll(extendAlternativeLabel(variant));
							}
						}

					}



				}

			}
		}//end class ann axioms

		//LogOutput.print("\t" + labels_set.size());
		num_syn=num_syn+labels_set.size();

		return labels_set;



	}






	/**
	 * Process given label (synonym) and extendes with wordnet synonyms
	 * @param label_value
	 */
	private Set<String> extendAlternativeLabel(String label_value){

		Set<String> set_syn = new HashSet<String>();

		List<Set<String>> wordi2syn = new ArrayList<Set<String>>(); 

		String[] words;
		int shift=1;

		String roman;

		//Replace "/" by _
		label_value=label_value.replaceAll("/", "_");

		if (label_value.indexOf(" ")>0){ //Synonyms
			words=label_value.split(" ");
		}
		else if (label_value.indexOf("_")>0){ //just in case
			words=label_value.split("_");
		}
		//Split capitals...
		else{
			words=Utilities.splitStringByCapitalLetter(label_value); //quite unlikely for alt labels
		}
		//else {
		//	words=new String[1];
		//	words[0]=label_value;
		//}


		shift=1;

		for (int i=0; i<words.length; i++){
			set_syn.add(words[i].replace(",", ""));

			//Synonym from wordnet: they are a bit noisy
			//if (IndexingUtilities.getLabel2wordnetsyn().containsKey(words[i])){
			//	set_syn.addAll(IndexingUtilities.getLabel2wordnetsyn().get(words[i]));
			//}

			//We use normalization from UMLS Specialist Lexicon 
			if (lexicalUtilities.hasNormalization(words[i])){
				//if (normalization_map.containsKey(words[i])){
				set_syn.addAll(lexicalUtilities.getNormalization(words[i]));
				//set_syn.addAll(normalization_map.get(words[i]));
			}


			//*****EXTEND WITH STEMMING
			//if(IndexingUtilities.getStemmingMap().containsKey(words[i])) {
			//set_syn.add(IndexingUtilities.getStemmingMap().get(words[i]));
			//}			
			else if (lexicalUtilities.isStemmingUp()){//Only if no normalization??
				set_syn.add(lexicalUtilities.getStemming4Word(words[i]));
			}

			//We normaliza numbers
			roman = lexicalUtilities.getRomanNormalization4Number(words[i]);
			if (!roman.equals("")){
				//LogOutput.print("\tROMAN   " +words[i] +"  " +  roman);
				set_syn.add(roman); 
			}



			wordi2syn.add(new HashSet<String>(set_syn));
			set_syn.clear();
		}


		long comb=1;

		//Too many combinations.... (max=50)
		for (Set<String> set : wordi2syn){
			comb=comb*set.size();
		}

		//LogOutput.print(comb);

		String label;
		if (comb>50 || comb<0){

			toohigh_synset_cases++;

			label="";
			for (int i=0; i<words.length-shift; i++){
				label+=words[i] + "_";
			}

			label+=words[words.length-shift];
			//LogOutput.print("\t" + label_value);
			set_syn.add(label);
			return set_syn;
		}				
		else { //We get combinations

			if (wordi2syn.size()==1){
				return wordi2syn.get(0);
			}

			return combineWordSynonyms(wordi2syn, wordi2syn.get(0), 1); 

		}		

	}





	private Set<String> combineWordSynonyms(List<Set<String>> wordi2syn, Set<String> currentSet, int index){

		Set<String> newSet = new HashSet<String>();

		for (String clabel: currentSet){

			for (String syn: wordi2syn.get(index)){

				newSet.add(clabel+ "_"+syn);
				//LogOutput.print(clabel+ "_"+syn);

			}
		}

		if (wordi2syn.size()<=index+1){
			return newSet;
		}
		else{
			return combineWordSynonyms(wordi2syn, newSet, index+1);
		}


	}



	/**
	 * Entries not matched in IF for stemmed labels
	 * @param entries
	 */
	public void addEntries2InvertedFileWeakLabels(Map<Set<String>, Set<Integer>> entries){

		invertedFileWeakLabels.putAll(entries);


	}


	public void setInvertedFileWeakLabels(){

		//List<String> list_words;

		//TODO: Default values
		int max_size_labels=8;
		int max_size_list_words_missing=3;


		//for (int ident : identifier2stemmedlabel.keySet()){
		for (int ident : identifier2stemmedlabels.keySet()){

			//list_words = identifier2stemmedlabel.get(ident);
			for (List<String> list_words : identifier2stemmedlabels.get(ident)){

				if (list_words.size()>max_size_labels)
					continue;

				if (list_words.size()>1){ //Smaller case 1 out of 2

					createWeakLabels4Identifier(list_words, ident, 1);// 1 missing word

					if (list_words.size()>3 && max_size_list_words_missing>1){ //Smaller case 2 out of 4

						createWeakLabels4Identifier(list_words, ident, 2);

						if (list_words.size()>5 && max_size_list_words_missing>2){ //Smaller case 3 out of 6

							createWeakLabels4Identifier(list_words, ident, 3);

							if (list_words.size()>7 && max_size_list_words_missing>3){ //Smaller case 4 out of 8

								createWeakLabels4Identifier(list_words, ident, 4);
							}
						}
					}
				}
			}
		}
	}


	/**
	 * Considers all labels from IF stemmed
	 * @deprecated review... something is wrong...
	 */
	public void setFullInvertedFileWeakLabels(){

		List<String> list_words = new ArrayList<String>();
		Set<Integer> identifiers;


		//TODO: Default values
		int max_size_labels=8;
		int max_size_list_words_missing=3;


		for (Set <String> stemmed_set : invertedFileWeakLabelsStemming.keySet()){

			if (stemmed_set.size()>max_size_labels)
				continue;

			list_words.addAll(stemmed_set);

			identifiers=invertedFileWeakLabelsStemming.get(stemmed_set);

			if (list_words.size()>1){ //Smaller case 1 out of 2

				createWeakLabels4Identifier(list_words, identifiers, 1);// 1 missing word

				if (list_words.size()>3 && max_size_list_words_missing>1){ //Smaller case 2 out of 4

					createWeakLabels4Identifier(list_words, identifiers, 2);

					if (list_words.size()>5 && max_size_list_words_missing>2){ //Smaller case 3 out of 6

						createWeakLabels4Identifier(list_words, identifiers, 3);

						if (list_words.size()>7 && max_size_list_words_missing>3){ //Smaller case 4 out of 8

							createWeakLabels4Identifier(list_words, identifiers, 4);
						}
					}
				}
			}
			list_words.clear();
		}
	}



	/**
	 * Combines the words in given list with 'x' missing words and stores the results in IF
	 * 
	 * @param cleanWords Clean label of concept
	 * @param ident Identifier of concepts
	 * @param missing_words Number of words to be discarded 
	 */
	private void createWeakLabels4Identifier(List<String> cleanWords, int ident, int missing_words){

		Set<String> combo = new HashSet<String>();

		//Fills identifierCombination

		//Set<Set<Integer>> combination_set = getIdentifierCombination(cleanWords.size(), missing_words);
		Set<Set<Integer>> combination_set = precomputeIndexCombination.getIdentifierCombination(cleanWords.size(), missing_words);

		for(Set<Integer> toExclude : combination_set){

			for (int pos=0; pos<cleanWords.size(); pos++){
				if (!toExclude.contains(pos))
					combo.add(cleanWords.get(pos));

			}

			//TODO: evaluate if set of words of combo are meaningful
			//are they too frequent??
			if (!invertedFileWeakLabels.containsKey(combo))
				invertedFileWeakLabels.put(new HashSet<String>(combo), new HashSet<Integer>());

			invertedFileWeakLabels.get(combo).add(ident);

			combo.clear();

		}


	}


	/**
	 * Combines the words in given list with 'x' missing words and stores the results in IF
	 * 
	 * @param cleanWords Clean label of concept
	 * @param ident Identifier of concepts
	 * @param missing_words Number of words to be discarded 
	 */
	private void createWeakLabels4Identifier(List<String> cleanWords, Set<Integer> identifiers, int missing_words){

		Set<String> combo = new HashSet<String>();

		//Fills identifierCombination

		//Set<Set<Integer>> combination_set = getIdentifierCombination(cleanWords.size(), missing_words);
		Set<Set<Integer>> combination_set = precomputeIndexCombination.getIdentifierCombination(cleanWords.size(), missing_words);

		for(Set<Integer> toExclude : combination_set){

			for (int pos=0; pos<cleanWords.size(); pos++){
				if (!toExclude.contains(pos))
					combo.add(cleanWords.get(pos));

			}

			if (!invertedFileWeakLabels.containsKey(combo))
				invertedFileWeakLabels.put(new HashSet<String>(combo), new HashSet<Integer>());

			invertedFileWeakLabels.get(combo).addAll(identifiers);

			combo.clear();
		}
	}

	public int getIdentifier4ConceptIRI(String iri){
		if (classIri2identifier.containsKey(iri))
			return classIri2identifier.get(iri);
		else
			return -1;
	}

	public int getIdentifier4ConceptName(String name){ 
		if (className2Identifier.containsKey(name))
			return className2Identifier.get(name);
		else
			return -1;
	}

	public int getIdentifier4ObjectPropName(String name){ 
		if (objectPropName2Identifier.containsKey(name))
			return objectPropName2Identifier.get(name);
		else
			return -1;
	}

	public int getIdentifier4DataPropName(String name){ 
		if (dataPropName2Identifier.containsKey(name))
			return dataPropName2Identifier.get(name);
		else
			return -1;
	}

	public int getIdentifier4InstanceName(String name){ 
		if (individualName2Identifier.containsKey(name))
			return individualName2Identifier.get(name);
		else
			return -1;
	}



	public Map<Set<String>, Set<Integer>> getInvertedFileExactMatching(){
		return invertedFileExact;
	}

	public Map<Set<String>, Integer> getInvertedFileExactMatching4DataProp(){
		return invertedFileExactDataProp;
	}

	public Map<Set<String>, Integer> getInvertedFileExactMatching4ObjProp(){
		return invertedFileExactObjProp;
	}


	public Map<Set<String>, Set<Integer>> getInvertedFileMatching4Individuals(){
		return invertedFileIndividuals;
	}

	public Map<String, Set<Integer>> getInvertedFileRoleAssertions(){
		return invertedFileRoleassertions;
	}

	//Weak IF
	public Map<String, Set<Integer>> getInvertedFileWeakMatching4Individuals(){
		return invertedFileWeakIndividuals;
	}


	public Map<Set<String>, Set<Integer>> getInvertedFileWeakLabelsStemming(){	
		return invertedFileWeakLabelsStemming;
	}

	public Map<Set<String>, Set<Integer>> getInvertedFileWeakLabels(){	
		return invertedFileWeakLabels;
	}

	public void setTaxonomicData() {
		setTaxonomicData(null);
	}
	
	/**
	 * Extract taxonomies
	 */
	public void setTaxonomicData(OWLReasoner r) {

		//TODO
		//Only SNOMED has been pre-classified with Condor
		//Note that in the classification we just have information about the hierarchy
		/*if (isOntoPreclassified){
			init=Calendar.getInstance().getTimeInMillis();
			unloadOntology();
			loadOWLOntology(physical_iri_class);
			fin = Calendar.getInstance().getTimeInMillis();
			LogOutput.print("Time unloading Ontology and loading pre-classified ontology (s): " + (float)((double)fin-(double)init)/1000.0);
		}*/


		//init=Calendar.getInstance().getTimeInMillis();
		
		// for externally pre-classified ontologies we use the reasoner
		if(r != null){
			try {
				reasoner = r;
				LogOutput.printAlways("Reusing " + r.getReasonerName());
//				System.out.println("Reusing " + r.getReasonerName());
////				reasoner = getIncompleteReasoner();
////				reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
			}
			catch(Exception e){
				System.err.println("Error setting up Structural reasoner: " 
						+ e.getMessage());
			}
		}
		else {
			if (Parameters.reasoner.equals(Parameters.hermit)){
				setUpReasoner_HermiT();
			}
			else{ //MORe
				setUpReasoner_MORe();
			}
		}
		//fin = Calendar.getInstance().getTimeInMillis();
		//LogOutput.print("Time classifying ontology (s): " + (float)((double)fin-(double)init)/1000.0);

		extractDangerousClasses();

		//init=Calendar.getInstance().getTimeInMillis();
		extractStringTaxonomiesAndDisjointness();
		//fin = Calendar.getInstance().getTimeInMillis();
		//LogOutput.print("Time Extracting Taxonomy (s): " + (float)((double)fin-(double)init)/1000.0);

		init = Calendar.getInstance().getTimeInMillis();
		extractGeneralHornAxioms(); //A^...^B->C
		fin = Calendar.getInstance().getTimeInMillis();
		LogOutput.print("Extracting General Axioms: " + (float)((double)fin-(double)init)/1000.0);



		//Extract
		if (Parameters.perform_instance_matching){			
			//We need the inferred types
			extractInferredTypes4Individuals();

		}

	}






	/**
	 * MORe or Structural reasoner
	 */
	private void setUpReasoner_MORe() {

		MOReAccess moreAccess=null;

		//Try first with Hermit if classifies in more than x seconds or error then structural  

		try {


			moreAccess = new MOReAccess(
					SynchronizedOWLManager.createOWLOntologyManager(),
					onto, false); //with factory i got problems


			//Timeout is not properly handled with MORe
			//moreAccess.classifyOntology_withTimeout_throws_Exception(17); 
			//We then do the timeout internally in MORe only over HermiT (or the OWL 2 reasoner)
			//MORe access invokes a version or MORe with timeout
			//moreAccess.classifyOntology(false); //no props
			moreAccess.classifyOntology_withTimeout_throws_Exception(Parameters.timeout); //in case of error or timeout


			if (moreAccess.isOntologyClassified()){
				reasoner = moreAccess.getReasoner();				
			}
			else {				
				LogOutput.print("Onto not classified with MORe. Using 'structural' reasoner instead.");
				reasoner = getIncompleteReasoner();
			}

			if (reasoner==null){
				LogOutput.print("Reasoner was null. Using 'structural' reasoner instead.");
				reasoner = getIncompleteReasoner();
			}


		}
		catch(Exception e){

			try {
				LogOutput.print("Error/timeout setting up MORe reasoner. Using 'structural' reasoner instead.");//\n\n" + e.getMessage() + "\n");

				reasoner = getIncompleteReasoner();
			}
			catch(Exception e2){
				System.err.println("Error setting up Structural reasoner: " + e2.getMessage());
				e2.printStackTrace();
			}

		}

		//if (reasoner.getReasonerName()!=null){
		//LogOutput.print("Reasoner name: " + reasoner.isConsistent() + ".\n");
		//LogOutput.print("Reasoner name: " + reasoner + ".\n");
		//LogOutput.print("Reasoner name: " + reasoner.getReasonerName() + ".\n");
		//}
	}



	/**
	 * HermiT or Structural reasoner
	 */
	private void setUpReasoner_HermiT() {

		HermiTAccess hermitAccess;

		//Try first with Hermit if classifies in more than x seconds or error then structural  

		try {

			if (Parameters.reason_datatypes || onto.getDatatypesInSignature(true).size()==0){
				//if (true){
				//hermitAccess = new HermiTReasonerAccess(onto, false);
				hermitAccess = new HermiTAccess(
						SynchronizedOWLManager.createOWLOntologyManager(),
						onto, false); //with factory i got problems

				//hermitAccess.classifyOntology_withTimeout(15);
				hermitAccess.classifyOntology_withTimeout_throws_Exception(Parameters.timeout); //in case of error or timeout

				if (hermitAccess.isOntologyClassified()){
					reasoner = hermitAccess.getReasoner();				
				}
				else {				
					LogOutput.print("Onto not classified with HermT. Using 'structural' reasoner instead.");
					//reasoner = new StructuralReasonerExtended(onto);
					reasoner = getIncompleteReasoner();
				}
			}
			else { //With OMT client gives an error :-(
				LogOutput.print("Ontology with datatypes. Using 'structural' reasoner instead.");
				//reasoner = new StructuralReasonerExtended(onto);
				reasoner = getIncompleteReasoner();
			}

			if (reasoner==null){
				LogOutput.print("Reasoner was null. Using 'structural' reasoner instead.");
				//reasoner = new StructuralReasonerExtended(onto);
				reasoner = getIncompleteReasoner();
			}


		}
		catch(Exception e){

			try {
				LogOutput.print("Error/timeout setting up HermiT reasoner. Using 'structural' reasoner instead.\n\n" + e.getMessage() + "\n");
				//reasoner = new StructuralReasonerExtended(onto);
				reasoner = getIncompleteReasoner();
			}
			catch(Exception e2){
				System.err.println("Error setting up Structural reasoner: " + e2.getMessage());
			}

		}

		//if (reasoner.getReasonerName()!=null){
		//LogOutput.print("Reasoner name: " + reasoner.isConsistent() + ".\n");
		//LogOutput.print("Reasoner name: " + reasoner + ".\n");
		//LogOutput.print("Reasoner name: " + reasoner.getReasonerName() + ".\n");
		//}
	}


	private OWLReasoner getIncompleteReasoner() throws Exception{
		return new StructuralReasonerExtended(onto);
		//ELKAccess elk = new ELKAccess(
		//		SynchronizedOWLManager.createOWLOntologyManager(),
		//		onto, true);
		//return elk.getReasoner();
	}



	/**
	 * Dangerous class such that the ones equivalent to Top
	 */
	private void extractDangerousClasses(){

		for (OWLClass cls : reasoner.getTopClassNode().getEntitiesMinusTop()){
			//System.out.println(cls.toString());
			dangerousClasses.add(class2identifier.get(cls));
			index.addDangerousClasses(class2identifier.get(cls));
		}

		for (int ide : dangerousClasses){
			LogOutput.print("DANGEROUS CLASS == TOP: "+ index.getName4ConceptIndex(ide));
		}
	}

	public Set<Integer> getDangerousClasses(){
		return dangerousClasses;
	}



	/**
	 * Create taxonomy (for interval labeling), and disjointness and equivalence (for internal control)
	 * @throws Exception
	 */
	private void extractStringTaxonomiesAndDisjointness() {

		// enables the fix for handling unsatisfiable classes
		// do not enable because the unsat classes will behave like bottom (subclass of every other class)
		boolean handleBottom = false;

		//onto.

		int bignode=0;

		int equiv=0;
		int disj=0;

		//For all nodes
		Map<Node<OWLClass>,Integer> node2identifier = new HashMap<Node<OWLClass>,Integer>();

		int identRepresentative;
		int ident1;
		int ident2;
		//String identRepresentativeStr;
		OWLClass clsRepresentative; 

		//TOP CONCEPTS 
		NodeSet<OWLClass> topClasses = reasoner.getSubClasses(reasoner.getTopClassNode().getRepresentativeElement(), true);
		//Real root nodes
		//RootIdentifiers.clear();
		for (OWLClass cls : topClasses.getFlattened()){
			if (class2identifier.containsKey(cls)){
				//RootIdentifiers.add(class2identifier.get(cls));
				index.addRoot2Structure(class2identifier.get(cls));
			}
		}

		// Alessandro: 20 April 2014
		// BOTTOM CONCEPTS
		Node<OWLClass> bottomNode = reasoner.getBottomClassNode();
		OWLClass bottomReprentative = null;
		if(bottomNode.getSize() > 1){
			if(handleBottom) 
				bottomReprentative = bottomNode.getEntitiesMinusBottom().iterator().next();
			else {
				for (OWLClass cls : bottomNode.getEntitiesMinusBottom())
					if(class2identifier.containsKey(cls))
						index.addBottomIdentifier(class2identifier.get(cls));
			}
		}
		// END BOTTOM CONCEPTS
		
		LogOutput.printAlways("Reasoner = " + reasoner.getReasonerName());
		LogOutput.printAlways("PRECOMPUTED? " + reasoner.isPrecomputed(InferenceType.CLASS_HIERARCHY));
		LogOutput.printAlways("#Classes = " + reasoner.getRootOntology().getClassesInSignature().size());
		
//		System.out.println("Reasoner = " + reasoner.getReasonerName());
//		System.out.println("PRECOMPUTED? " + reasoner.isPrecomputed(InferenceType.CLASS_HIERARCHY));
//		System.out.println("#Classes = " + reasoner.getRootOntology().getClassesInSignature().size());
		
		//All Subclasses
		NodeSet<OWLClass> nodeSet = reasoner.getSubClasses(reasoner.getTopClassNode().getRepresentativeElement(), false);

		//LogOutput.print(reasoner.getTopClassNode().getRepresentativeElement() + " " + nodeSet.getNodes().size());


		//We first check node size
		for (Node<OWLClass> node : nodeSet){

			if (node.isTopNode() || (node.isBottomNode() && bottomReprentative == null))
				continue;

			if(node.isBottomNode())
				clsRepresentative = bottomReprentative;
			else
				clsRepresentative=node.getRepresentativeElement();

			//Important to avoid non-class nodes like "DIRECTED-BINARY-RELATION"
			if (!class2identifier.containsKey(clsRepresentative)){
				//LogOutput.print("NO: " + clsRepresentative);
				continue;
			}

			identRepresentative=class2identifier.get(clsRepresentative);
			//WE WANT TO GUARANTEE THAT REPRESENTATIVE IS ALWAYS THE SAME
			node2identifier.put(node,identRepresentative);

			//Only for 'big' nodes
			if (node.getEntities().size()>1){
				bignode++;
				//representativeNodes.add(identRepresentative);
				index.addRepresentativeNode(identRepresentative);
			}
		}




		//TODO Meaningful roots and level are candidates to be deprecated
		//MEANINGFUL ROOTS: to avoid cases like SNOMED where there is an unique root apart from THING
		//This method should be used only for big ontologies!!
		//extractMeaningfulRoots();
		//LogOutput.print("Number of roots: " + MeaningfulRootIdentifiers.size());


		//HIERARCHY LEVELS FOR ENTITIES
		//extractHierarchyLevel(reasoner.getSubClasses(reasoner.getTopClassNode().getRepresentativeElement(), true), 1);


		for (Node<OWLClass> node : nodeSet){

			if ((node.isBottomNode() && bottomReprentative == null) || node.isTopNode())
				continue;

			if (!node2identifier.containsKey(node))
				continue;

			identRepresentative = node2identifier.get(node);
			//clsRepresentative = identifier2class.get(identRepresentative);
			if(node.isBottomNode())
				clsRepresentative = bottomReprentative;
			else
				clsRepresentative = node.getRepresentativeElement();

			//Add root to class index (only representative?) Deprecated
			//index.getClassIndex(identRepresentative).setRoots(getMeaningfulRootsForIdentifier(identRepresentative));

//			if(node.getRepresentativeElement().getIRI().toString().equals(
//					"http://conference#Paper") || 
//				node.getRepresentativeElement().getIRI().toString().equals(
//						"http://http://edas#RejectedPaper") || 
//				node.getRepresentativeElement().getIRI().toString().equals(
//						"http://edas#Paper") || 
//				node.getRepresentativeElement().getIRI().toString().equals(
//						"http://conference#Reviewed_contribution"))
//				System.out.println(node.getRepresentativeElement().getIRI());

			//SUBCLASSES: TAXONOMY
			if (!reasoner.getSubClasses(clsRepresentative, true).isEmpty()){

				//identifier2directkids.put(identRepresentative, new HashSet<Integer>());
				//Only representatives??
				index.getClassIndex(identRepresentative).setEmptyDirectSubClasses();

				for (Node<OWLClass> nodeSub : reasoner.getSubClasses(clsRepresentative, true).getNodes()){
					if (nodeSub.isTopNode() || (nodeSub.isBottomNode() && bottomReprentative == null))
						continue;

					//identifier2directkids.get(identRepresentative).add(node2identifier.get(nodeSub));//will give us direct kid identifiers
					index.getClassIndex(identRepresentative).addDirectSubClass(node2identifier.get(nodeSub));


				}
			}


			//SUPERCLASSES: REVERSE TAXONOMY
			if (!reasoner.getSuperClasses(clsRepresentative, true).isEmpty()){

				//identifier2directparents.put(identRepresentative, new HashSet<Integer>());
				index.getClassIndex(identRepresentative).setEmptyDirectSuperClasses();

				for (Node<OWLClass> nodeSup : reasoner.getSuperClasses(clsRepresentative, true).getNodes()){
					if (nodeSup.isTopNode() || (nodeSup.isBottomNode()))// && bottomReprentative == null))
						continue;

					//identifier2directparents.get(identRepresentative).add(node2identifier.get(nodeSup));  //direct parents
					//System.out.println(identRepresentative + "  " + nodeSup + "  " + node2identifier.get(nodeSup));
					index.getClassIndex(identRepresentative).addDirectSuperClass(node2identifier.get(nodeSup));


				}
			}


			//WE STORE DISJ AND EQUIVALENCES FOR ALL NODE ENTITIES

			//DISJOINTNESS
			for (OWLClass nodeClass : node.getEntities()){ //for all node classes

				//Avoid top or nothing
				if (node.isTopNode() || (node.isBottomNode() && bottomReprentative == null))
					continue;

				//We check for each class. differnt disjoint axioms may affect different classes of same node
				//The reasoner has been adapted to extract only explicit disjointness
				//since implicit (all) disjointness is time conuming
//				if(reasoner.getDisjointClasses(nodeClass) == null){
//					System.out.println("NO DISJ FOR " + nodeClass);
//					continue;
//				}
				
				if (!reasoner.getDisjointClasses(nodeClass).isEmpty()){

					ident1=class2identifier.get(nodeClass);

					index.getClassIndex(ident1).setEmptyDisjointClasses();
					disj++;

					//LogOutput.print(nodeClass);
					//LogOutput.print("\t" + reasoner.getDisjointClasses(nodeClass).getNodes());

					for (Node<OWLClass> nodeDisj : reasoner.getDisjointClasses(nodeClass).getNodes()){

						for (OWLClass disjcls : nodeDisj.getEntities()){ //We add all

							//Avoid top or nothing
							if (disjcls.isTopEntity() || disjcls.isBottomEntity())
								continue;

							//TODO To avoid classes being disjoint with themselves
							if (ident1!=class2identifier.get(disjcls)){ 
								index.getClassIndex(ident1).addDisjointClass(class2identifier.get(disjcls));
								//Add both sides??
								//TODO:  //Not sure if neccessary
								index.getClassIndex(class2identifier.get(disjcls)).addDisjointClass(ident1);
							}
						}
					}					
				}
			}

			//EQUIVELNCE
			if (node.getEntities().size()>1){
				OWLClass[] nodeClasses= new OWLClass[node.getEntitiesMinusBottom().size()];
				nodeClasses = node.getEntitiesMinusBottom().toArray(nodeClasses);
				//LogOutput.print(nodeClasses[0]);

				for (int i=0; i<nodeClasses.length; i++){
					ident1=class2identifier.get(nodeClasses[i]);
//					// otherwise we loose the effects of the SCC fix
//					if(!index.getClassIndex(ident1).hasEquivalentClasses())
					index.getClassIndex(ident1).setEmptyEquivalentClasses();
					equiv++;

					for (int j=0; j<nodeClasses.length; j++){
						if (i==j)
							continue;

						ident2=class2identifier.get(nodeClasses[j]);

						index.getClassIndex(ident1).addEquivalentClass(ident2);

						//Propagation of disjointness
						if (index.getClassIndex(ident1).hasDirectDisjointClasses()){

							if (!index.getClassIndex(ident2).hasDirectDisjointClasses()){
								index.getClassIndex(ident2).setEmptyDisjointClasses();
							}

							index.getClassIndex(ident2).addAllDisjointClasses(index.getClassIndex(ident1).getDisjointClasses());
						}

					}
				}


			}
		}



		node2identifier.clear();

		LogOutput.print("Representatives (aggregated): " + index.getRepresentativeNodes().size());
		LogOutput.print("Disjoint: " + disj);
		LogOutput.print("Equivalences: " + equiv);
		LogOutput.print("Big nodes: " + bignode);	
		//LogOutput.print("Taxonomy: " + Taxonomy);

	}


	/**
	 * Extract axioms of the form A^...^B- > C
	 * from subclassof or equivalence axioms
	 */

	Set<Integer> ausxSetOfClasses = new HashSet<Integer>();

	private void extractGeneralHornAxioms(){

		int idecls;

		//for (OWLClass cls: onto.getClassesInSignature()){
		for (OWLClass cls: class2identifier.keySet()){
			//LogOutput.print(cls.getIRI().toString());

			idecls=class2identifier.get(cls);

			for (OWLEquivalentClassesAxiom ax: onto.getEquivalentClassesAxioms(cls)){

				for (OWLClassExpression exp_equiv : ax.getClassExpressions()){

					addOWLClassExpresion2GeneralHornAxiom(idecls, exp_equiv);

				}//For expressions

			}//For equiv axioms

			for (OWLSubClassOfAxiom ax: onto.getSubClassAxiomsForSuperClass(cls)){

				addOWLClassExpresion2GeneralHornAxiom(idecls, ax.getSubClass());

			}//For subclass axioms

		}//For classes


		LogOutput.print("->General HORN Axioms: " + index.getGeneralHornAxiom().size());

		/*int i=0;
		String horn="";
		for (Set<Integer> body : generalHornAxioms.keySet()){
			i++;


			for (int b : body){
				horn+=index.getClassIndex(b).getLabel() + " ^ ";
			}

			horn+=" -> " + index.getClassIndex(generalHornAxioms.get(body)).getLabel();

			LogOutput.print(body + "->" + generalHornAxioms.get(body));
			LogOutput.print(horn);

			horn="";

			if (i>10){
				break;
			}

		}*/

	}


	/**
	 * We extract inferred class types for the individuals
	 */
	private void extractInferredTypes4Individuals(){

		for (int identIndiv : index.getIndividuaIdentifierSet()){

			for (Node<OWLClass> node_cls : reasoner.getTypes(
					index.getOWLNamedIndividual4IndividualIndex(identIndiv), true)){

				for (OWLClass cls : node_cls.getEntitiesMinusTop()){

					index.addType4Individual(identIndiv, class2identifier.get(cls));

				}				
			}
		}		
	}





	/**
	 * Checks if the expresion is valid for general HORN axioms
	 */
	private void addOWLClassExpresion2GeneralHornAxiom(int idecls, OWLClassExpression exp){


		ausxSetOfClasses.clear();

		//if (exp.isClassExpressionLiteral()){
		//	return;
		//}
		if (exp instanceof OWLObjectIntersectionOf){

			for (OWLClassExpression exp_intersect : ((OWLObjectIntersectionOf) exp).getOperands()){

				//if (exp_intersect.isClassExpressionLiteral()){ //A and notA are considered class expression literals
				if (!exp_intersect.isAnonymous()){
					ausxSetOfClasses.add(class2identifier.get(exp_intersect.asOWLClass()));
				}
				else{
					ausxSetOfClasses.clear();
					return;//do nothing
				}

			}

		}
		else{
			return; //do nothing
		}

		if (ausxSetOfClasses.size()>1){ //At least Two in the intersection 
			//class2identifier

			//if (!generalHornAxioms.containsKey(ausxSetOfClasses)){
			//	generalHornAxioms.put(new HashSet<Integer>(ausxSetOfClasses), idecls);
			//}
			index.addGeneralHornAxiom2Structure(ausxSetOfClasses, idecls);
		}

	}


	/*
	 * Set with representative for equivalence nodes
	 * Will be required after interval labelling indexing
	 * @return

	public Set<Integer> getRepresentativeNodes(){
		return representativeNodes;
	}*/


	/*
	 * Important for the cleaning process
	 * @return
	 *
	public Set<Integer> getRootNodes(){
		return RootIdentifiers;
	}*/






	/*private Set<Integer> getMeaningfulRootsForIdentifier(int ident){

		Set<Integer> set =  new HashSet<Integer>();

		for (int ideroot : MeaningfulRootIdentifiers){
			if (isSubClassOf(ident, ideroot)){
				set.add(ideroot);
			}
		}

		return set;

	}*/




	int minNumberOfRoots=0;

	/**
	 * @deprecated
	 */
	private void extractMeaningfulRoots(){

		//TOP CONCEPTS 
		//(avoids possible top elements like in snomed)
		//Create here roots and discard Top concepts!
		//By name or by number? --> if == 1 then split
		NodeSet<OWLClass> topClasses = reasoner.getSubClasses(reasoner.getTopClassNode().getRepresentativeElement(), true);


		//Real root nodes
		TaxRootIdentifiers.clear();
		for (Node<OWLClass> node : topClasses.getNodes()){
			if (class2identifier.containsKey(node.getRepresentativeElement())){
				TaxRootIdentifiers.add(class2identifier.get(node.getRepresentativeElement()));
			}
		}



		//Meaningful roots. Might be different to the real ones
		if (onto.getClassesInSignature(true).size()<500)
			minNumberOfRoots=4;
		else
			minNumberOfRoots=8;

		if (topClasses.getNodes().size()<minNumberOfRoots){
			MeaningfulRootIdentifiers.addAll(extractMeaningfulRoots(topClasses, 1));
			if (MeaningfulRootIdentifiers.size()>topClasses.getNodes().size())
				return;
		}

		//Level 0 roots
		MeaningfulRootIdentifiers.clear();
		for (Node<OWLClass> node : topClasses.getNodes()){
			if (class2identifier.containsKey(node.getRepresentativeElement())){
				MeaningfulRootIdentifiers.add(class2identifier.get(node.getRepresentativeElement()));
			}
		}



		//LogOutput.print("Number of roots: " + RootIdentifiers.size());


	}

	/**
	 * @deprecated
	 */
	private Set<Integer> extractMeaningfulRoots(NodeSet<OWLClass> nodes, int level){

		Set<Integer> mroots=new HashSet<Integer>();
		OWLClassNodeSet mrootsClass =  new OWLClassNodeSet();

		for (Node<OWLClass> node : nodes.getNodes()){

			for (Node<OWLClass> topNode : reasoner.getSubClasses(node.getRepresentativeElement(), true)){
				mrootsClass.addNode(topNode);
				if (class2identifier.containsKey(topNode.getRepresentativeElement())){
					mroots.add(class2identifier.get(topNode.getRepresentativeElement()));
				}
			}
		}
		if (mroots.size()>=minNumberOfRoots || level==3){ //we want to avoid infinite recursion
			return mroots;

		}
		else {
			return extractMeaningfulRoots(mrootsClass, level+1);
		}

	}


	/**
	 * @deprecated
	 */
	private void extractHierarchyLevel(NodeSet<OWLClass> classes, int level){

		//TOP CONCEPTS		
		//NodeSet<OWLClass> topClasses = reasoner.getSubClasses(reasoner.getTopClassNode().getRepresentativeElement(), true);

		int ident;

		for (Node<OWLClass> node : classes.getNodes()){
			if (class2identifier.containsKey(node.getRepresentativeElement())){
				ident = class2identifier.get(node.getRepresentativeElement());
				//We add the deeper level
				if (index.getClassIndex(ident).getHierarchyLevel()<level){ 
					index.getClassIndex(ident).setHierarchyLevel(level);
				}
			}

			extractHierarchyLevel(reasoner.getSubClasses(node.getRepresentativeElement(), true), level+1);

		}


	}


	public Set<Integer> getMeaningfulRoots(){
		return MeaningfulRootIdentifiers;
	}

	public Set<Integer> getRaelRoots(){
		return TaxRootIdentifiers;
	}



	public void clearFrequencyRelatedStructures(){
		singleWordInvertedIndex.clear();
		filteredInvertedIndex.clear();
	}


	private Map<String, Set<Integer>> singleWordInvertedIndex = new HashMap<String, Set<Integer>>();

	public Integer getFrequency(String word)
	{
		if (singleWordInvertedIndex.containsKey(word))
			return singleWordInvertedIndex.get(word).size();
		return 0;
	}

	private Map<Set<String>, Set<Integer>> filteredInvertedIndex = new HashMap<Set<String>, Set<Integer>>();

	public Map<Set<String>, Set<Integer>> getFilteredInvertedIndex()
	{
		return filteredInvertedIndex;
	}

	public void clearFilteredInvertedIndex()
	{
		filteredInvertedIndex.clear();
	}

	public void buildFilteredInvertedIndex(Set<String> stopWords)
	{
		ClassIndex cls;
		String [] words;
		Set<String> key, labels;

		/*
		Set<String> specialKey = new HashSet<String>();
		specialKey.add("vas");
		specialKey.add("va");
		specialKey.add("def");
		 */	

		for (Integer id : className2Identifier.values())
		{
			cls = index.getClassIndex(id);
			labels = cls.getStemmedAltLabels();

			if (labels == null)
			{
				Lib.debuginfo("A class named " + cls.getEntityName() + " has no labels!");
				continue;
			}

			for (String lab : labels)
			{
				key = new HashSet<String>();
				words = lab.split("_");
				for (String word : words)
					if (!stopWords.contains(word))
						key.add(word);

				if (!filteredInvertedIndex.containsKey(key))
				{
					filteredInvertedIndex.put(key, new HashSet<Integer>());
				}

				filteredInvertedIndex.get(key).add(id);
			}
		}
		/*		
		Set<Entry<Set<String>,Set<Integer>>> temp = filteredInvertedIndex.entrySet();

		LogOutput.print(filteredInvertedIndex.size() + " " + temp.size());

		Entry<Set<String>, Set<Integer>> key1 = null, key2;
		for (Entry<Set<String>, Set<Integer>> entry : temp)
		{
			Lib.logInfo("- " + entry.getKey().toString());
			if (entry.getKey().equals(specialKey))
			{
				if (key1 == null)
					LogOutput.print(key1 = entry);
				else 
				{
					LogOutput.print(key2 = entry);
					LogOutput.print(key1.equals(key2) + " " + key1.hashCode() + " " + key2.hashCode());
					LogOutput.print(key1 + " "+ key2);
				}
			}

		}

		Lib.closeLog();
		 */	}

	public Set<String> getSuperClass(int id, int TOTAL) 
	{
		Set<String> ret = new HashSet<String>();
		Set<Integer> superclasses;
		Set<Integer> visited = new HashSet<Integer>();
		ClassIndex cls;

		Queue<ClassIndex> q = new LinkedList<ClassIndex>();
		q.add(index.getClassIndex(id));
		visited.add(id);

		while (!q.isEmpty() && ret.size() < TOTAL)
		{
			superclasses = q.remove().getDirectSuperclasses();
			//TODO: Yujiao - check the OWLClassExpression

			for (int i: superclasses)
				if (!visited.contains(i))
				{
					q.add(cls = index.getClassIndex(i));
					visited.add(i);
					addStemmedAltLabels(ret, cls);
				}
		}

		return ret;
	}

	private void addStemmedAltLabels(Set<String> labels, ClassIndex cls) 
	{
		Set<String> newLabs = cls.getStemmedAltLabels();

		if (newLabs == null) return ;
		for (String lab : newLabs)
			labels.add(lab);
	}












	/**
	 * 
	 * Manages the extraction of the lexicon associated to an Individual that is presented as data asserion axioms 
	 * or object asssertion axioms. Currently is based on the IM track of the OAEI 2012
	 * 
	 * @author Ernesto
	 *
	 */
	private class ExtractAcceptedLabelsFromRoleAssertions{


		Set<String> lexiconValues4individual = new HashSet<String>();
		String label_value;

		int max_size_name_label=0;
		int min_size_name_label=5000;


		ExtractAcceptedLabelsFromRoleAssertions(){

		}


		protected boolean isDummyIndividual(OWLNamedIndividual indiv){

			OWLObjectPropertyAssertionAxiom opaa;
			String prop_uri;


			//Check for oject property assertions deep 1 referenceing given individual
			//-------------------------------------------------------------------------
			//If referenced it is a dummy individual which should not be considered in the matching				
			for (OWLAxiom refAx : onto.getReferencingAxioms(indiv, true)){

				if (refAx instanceof OWLObjectPropertyAssertionAxiom){

					opaa = (OWLObjectPropertyAssertionAxiom)refAx;

					//Not the searched individual
					if (opaa.getObject().isAnonymous())
						continue;

					//Not the searched individual as assertion object
					if (!indiv.equals(opaa.getObject().asOWLNamedIndividual()))
						continue; //with next axiom

					//Check if object property is the used for dummy individuals 
					if (!opaa.getProperty().isAnonymous()){

						prop_uri = opaa.getProperty().asOWLObjectProperty().getIRI().toString();

						for (String op4indiv : Parameters.accepted_object_assertion_URIs_for_individuals){

							//The property is the one
							if (prop_uri.equals(op4indiv)){
								return true;
							}

						}	

					}


				}

			}

			return false;



		}

		protected Set<String> extractLexiconFromRoleAssertions(OWLNamedIndividual indiv){				

			lexiconValues4individual.clear();



			//We also add from rdfs:comments
			//-----------------------------------------
			//Since the comments may be long we need to pre-process them
			for (OWLAnnotationAssertionAxiom indivAnnAx : indiv.getAnnotationAssertionAxioms(onto)){


				String uri_ann = indivAnnAx.getAnnotation().getProperty().getIRI().toString();


				if (Parameters.rdf_comment_uri.equals(uri_ann)){

					try{
						label_value = processLabel(
								((OWLLiteral)indivAnnAx.getAnnotation().getValue()).getLiteral().toLowerCase());
					}
					catch (Exception e){
						//In case of error.
						label_value =  "";
					}

					//Statistics
					if (label_value.length() > max_size_name_label){
						max_size_name_label=label_value.length();
					}

					if (label_value.length()>0 && label_value.length() < min_size_name_label){
						min_size_name_label=label_value.length();
					}

					if (label_value.length()>2){
						lexiconValues4individual.add(label_value);
					}


				}

			}//end extraction of comments




			//Datatype assertion
			for (String uri_indiv_ann : Parameters.accepted_data_assertion_URIs_for_individuals){

				for (OWLLiteral assertion_value : indiv.getDataPropertyValues(
						index.getFactory().getOWLDataProperty(IRI.create(uri_indiv_ann)), onto)){


					//LogOutput.print(indiv.getIRI().toString());

					label_value = processLabel(
							assertion_value.getLiteral().toLowerCase());

					//Statistics
					if (label_value.length() > max_size_name_label){
						max_size_name_label=label_value.length();
					}

					if (label_value.length()>0 && label_value.length() < min_size_name_label){
						min_size_name_label=label_value.length();
					}

					if (label_value.length()>2){
						lexiconValues4individual.add(label_value);
					}

				}
			}

			//OBject property assertions deep 1  (level 1 references a dummy individual)
			//-------------------------------------
			for (String uri_indiv_ann_deep1 : Parameters.accepted_object_assertion_URIs_for_individuals){

				for (OWLIndividual assertion_value_indiv : indiv.getObjectPropertyValues(
						index.getFactory().getOWLObjectProperty(IRI.create(uri_indiv_ann_deep1)), onto)){

					//We only consider named individuals
					if (assertion_value_indiv.isNamed()){

						//Datatype assertion deep 2: has_value and others
						//----------------------------------------
						for (String uri_indiv_ann_deep2 : Parameters.accepted_data_assertion_URIs_for_individuals_deep2){

							for (OWLLiteral assertion_value_deep2 : assertion_value_indiv.asOWLNamedIndividual().getDataPropertyValues(
									index.getFactory().getOWLDataProperty(IRI.create(uri_indiv_ann_deep2)), onto)){


								label_value = processLabel(
										assertion_value_deep2.getLiteral().toLowerCase());

								//Statistics
								if (label_value.length() > max_size_name_label){
									max_size_name_label=label_value.length();
								}

								if (label_value.length()>2){
									lexiconValues4individual.add(label_value);
								}

							}
						}//end for data assertion level 2

						//Extract comment level 2
						//---------------------
						for (OWLAnnotationAssertionAxiom indivAnnAx_level2 : assertion_value_indiv.asOWLNamedIndividual().getAnnotationAssertionAxioms(onto)){


							String uri_ann = indivAnnAx_level2.getAnnotation().getProperty().getIRI().toString();


							if (Parameters.rdf_comment_uri.equals(uri_ann)){

								try{
									label_value = processLabel(
											((OWLLiteral)indivAnnAx_level2.getAnnotation().getValue()).getLiteral().toLowerCase());
								}
								catch (Exception e){
									//In case of error.
									label_value =  "";
								}

								//Statistics
								if (label_value.length() > max_size_name_label){
									max_size_name_label=label_value.length();
								}

								if (label_value.length()>0 && label_value.length() < min_size_name_label){
									min_size_name_label=label_value.length();
								}

								if (label_value.length()>2){
									lexiconValues4individual.add(label_value);
								}


							}

						}//end extraction of comments

					}

				}
			}


			return lexiconValues4individual;
		}


		/**
		 * This method extracts lexicon from other properties. Like "lives in X". 
		 * In the end we will only keep those relationships that uniquely identifies an instance
		 * @param indiv
		 * @return
		 */
		protected Set<String> extractExtendedLexiconFromRoleAssertions(OWLNamedIndividual indiv){

			lexiconValues4individual.clear();

			//look forclean lexicon in data prop

			String label_name;

			Map<OWLDataPropertyExpression, Set<OWLLiteral>> dataProp2values = indiv.getDataPropertyValues(onto);


			//We estract a data props lecicon like "date_12_12_2012", "age_18", etc...
			for (OWLDataPropertyExpression dataprop : dataProp2values.keySet()){

				if (dataprop.isAnonymous())
					continue;

				//we avoid properties already extracted
				if (Parameters.accepted_data_assertion_URIs_for_individuals.contains(
						dataprop.asOWLDataProperty().getIRI().toString())
						||
						Parameters.accepted_data_assertion_URIs_for_individuals_deep2.contains( //hasv_alue
								dataprop.asOWLDataProperty().getIRI().toString()))
					continue;


				//Init label with name of the property
				label_name = Utilities.getEntityLabelFromURI(dataprop.asOWLDataProperty().getIRI().toString());

				for (OWLLiteral literal : dataProp2values.get(dataprop)){

					//if (isGoodLabel(literal.getLiteral().toString())) //not useful in this case
					//We also normalize dates in case the give string is within one of the acceptred date formats
					lexiconValues4individual.add(label_name + "_" + NormalizeDate.normalize(literal.getLiteral().toString()));

				}

			}//for dtata prop

			Map<OWLObjectPropertyExpression, Set<OWLIndividual>> objProp2values = indiv.getObjectPropertyValues(onto);
			int ident;

			//We estract a data props lecicon like "lives_in_city", "spoken_in_lesotho"
			for (OWLObjectPropertyExpression objprop : objProp2values.keySet()){

				if (objprop.isAnonymous())
					continue;

				//we avoid properties already extracted
				//if (Parameters.accepted_object_assertion_URIs_for_individuals.contains(
				//		objprop.asOWLObjectProperty().getIRI().toString()))
				//	continue;

				//Init label with name of the property
				label_name = Utilities.getEntityLabelFromURI(objprop.asOWLObjectProperty().getIRI().toString());

				for (OWLIndividual indiv_deep2 : objProp2values.get(objprop)){

					if (indiv_deep2.isAnonymous())
						continue;


					if (!inidividual2identifier.containsKey(indiv_deep2.asOWLNamedIndividual())){
						continue;
					}

					ident = inidividual2identifier.get(indiv_deep2.asOWLNamedIndividual());

					//If has alternative labels, otherwise we cannot do much (since label will probably be a "randomly" generated identifier)
					//Dummy indiv has not alternative labels!!
					if (index.hasIndividualAlternativeLabels(ident)){  
						//we know it is a good label
						lexiconValues4individual.add(label_name + "_" + index.getLabel4IndividualIndex(ident));
					}
					else{

						//Deep 2
						///
						dataProp2values = indiv_deep2.getDataPropertyValues(onto);

						//We estract a data props lecicon like "date_12_12_2012", "age_18", etc...
						//in OAEI 2013 I this is not used much... since deep2 obj properties substitute another obj propertyes
						for (OWLDataPropertyExpression dataprop : dataProp2values.keySet()){

							if (dataprop.isAnonymous())
								continue;

							label_name = Utilities.getEntityLabelFromURI(dataprop.asOWLDataProperty().getIRI().toString());

							for (OWLLiteral literal : dataProp2values.get(dataprop)){

								//if (isGoodLabel(literal.getLiteral().toString()))
								lexiconValues4individual.add(label_name + "_" + NormalizeDate.normalize(literal.getLiteral().toString()));

							}

						}

						Map<OWLObjectPropertyExpression, Set<OWLIndividual>> objProp2values_deep2 = indiv_deep2.getObjectPropertyValues(onto);

						for (OWLObjectPropertyExpression objectprop2 : objProp2values_deep2.keySet()){

							if (objectprop2.isAnonymous())
								continue;

							label_name = Utilities.getEntityLabelFromURI(objectprop2.asOWLObjectProperty().getIRI().toString());

							for (OWLIndividual indiv_deep3 : objProp2values_deep2.get(objectprop2)){

								int ident2 = inidividual2identifier.get(indiv_deep3.asOWLNamedIndividual());

								if (index.hasIndividualAlternativeLabels(ident2)){  
									lexiconValues4individual.add(label_name + "_" + index.getLabel4IndividualIndex(ident2));
								}							
							}

						}

					}




				}


			}//for obj prop

			return lexiconValues4individual;


		}


		/**
		 * 
		 * @param value
		 * @return Empty string if it is not valid
		 */
		public String processLabel(String value){

			String processedLabel="";

			//Reg expression to split text: //Split up to "&", ".", "(", "," ";", is, was, are, were, est/fut (french)
			//In this way we split long comments and we only get the important part as synonym
			//String reg_ex_split = "[&\\.,;(]";
			String reg_ex_split="[&\\,;(/\\[]|(\\s)is(\\s)|(\\s)are(\\s)|(\\s)was(\\s)|(\\s)were(\\s)|(\\s)est(\\s)|(\\s)fut(\\s)|(\\s)un(\\s)|(\\s)a(\\s)|(\\s)an(\\s)";
			//No filter by point : St. Georges or St. John

			//Removing annoying acronyms
			//order of the British empire
			//Never inside a word 
			processedLabel = value.replaceAll(" obe ", "");
			processedLabel = processedLabel.replaceAll(" obe", "");
			processedLabel = processedLabel.replaceAll("obe ", "");
			//fellow royal society
			processedLabel = processedLabel.replaceAll(" frs ", "");
			processedLabel = processedLabel.replaceAll(" frs", "");
			processedLabel = processedLabel.replaceAll("frs ", "");

			int manegeable_lenght = 65;

			//To remove non ascii spaces
			processedLabel =  processedLabel.replaceAll(String.valueOf((char) 160), " ");

			//short data assertion strings
			if (processedLabel.length()<=manegeable_lenght && !processedLabel.contains("<p>") && !processedLabel.contains("</p>")){

				//We still want to split and trim
				//we keep/split string up to the given character (twice in case mroe than one character)
				processedLabel = processedLabel.split(reg_ex_split)[0];
				processedLabel = processedLabel.split(reg_ex_split)[0];
				//we remove white spaces at the end and begining of label
				processedLabel = processedLabel.trim();
				//processedLabel = StringUtils.trim(processedLabel);


				if (!isGoodLabel(processedLabel)){
					//LogOutput.printAlways("Filtered: "+ processedLabel);
					return ""; //bad label
				}
				else{
					//LogOutput.print("GOOD: "+ processedLabel);
					return processedLabel;
				}

			}
			//Text with several paragraphs -> filter
			else {

				//Detect if it starts with <p> if not then it has been split 
				//For iimb
				if (processedLabel.startsWith("<p>")){

					processedLabel = processedLabel.split("<p>")[1];

					//we keep/split string up to the given character (twice in case mroe than one character)
					processedLabel = processedLabel.split(reg_ex_split)[0];
					processedLabel = processedLabel.split(reg_ex_split)[0];

					//we remove white spaces ate the endand beginning oflabel
					processedLabel = processedLabel.trim();
					//processedLabel = StringUtils.trim(processedLabel);

					if (processedLabel.length()<=manegeable_lenght){

						if (isGoodLabel(processedLabel)){
							//LogOutput.print("GOOD: "+ processedLabel);
							return processedLabel;
						}
					}

					LogOutput.print("Filtered: "+ processedLabel);
					return "";

				}
				else {

					//For comments in RDFT oaei 2013. They do not contain <p>

					//we keep/split string up to the given character (twice in case mroe than one character)
					processedLabel = processedLabel.split(reg_ex_split)[0];
					processedLabel = processedLabel.split(reg_ex_split)[0];

					//we remove white spaces at the end and begining of label
					//System.out.println("'" + processedLabel + "'");
					processedLabel = processedLabel.trim();
					//processedLabel = StringUtils.trim(processedLabel);
					//System.out.println("'" + processedLabel + "'");


					if (processedLabel.length()<=manegeable_lenght){

						if (isGoodLabel(processedLabel)){
							//LogOutput.printAlways("GOOD: "+ processedLabel);
							return processedLabel;
						}
						else{
							//LogOutput.printAlways("BAD: "+ processedLabel);
						}
					}
					else{

						processedLabel = processedLabel.substring(0, manegeable_lenght);

						if (isGoodLabel(processedLabel)){
							LogOutput.print("REDUCED label 0-"+manegeable_lenght+": "+ processedLabel);
							return processedLabel;
						}

					}

					LogOutput.print("Filtered: "+ processedLabel);



					return "";
				}


			}//if lenght


		}








		private boolean isGoodLabel(String label){

			//REGULAR EXPRESSIONS

			//we do not consider "y" as consonant for filtering purposes. After all "y" may have vowel phonetics.
			String consonant_regex = "[b-df-hj-np-tv-xz]";
			String more3_consonants_regex = consonant_regex + consonant_regex + consonant_regex + consonant_regex + "+";
			String more5_consonants_regex = consonant_regex + consonant_regex + consonant_regex + consonant_regex + consonant_regex + consonant_regex + "+";
			String vowel_regex = "[aeiou]"; //accented?? so far we are considering only "english"
			String more3_vowels_regex =  vowel_regex + vowel_regex + vowel_regex + vowel_regex + "+";
			String same_character_3_times = ".*(.)\\1\\1.*";
			//String space_character_3_times = ".*([ \t])\\1\\1.*";
			String space_character_3_times = ".*(\\s)\\1\\1.*";


			String[] words;

			//Detect if label has been randomly generated

			if (label.length()<3){ //very short strings
				return false;
			}

			if (label.contains("!") || label.contains("?"))
				return false;


			if (label.matches(space_character_3_times))
				return false;



			//Split in words
			words = label.split(" ");

			//At least one word with size >1
			boolean has_min_size_word = false;

			//has_min_size_word=true;

			for (String word : words){

				word = word.toLowerCase();

				//Accept roman numbers
				if (NormalizeNumbers.getRomanNumbers10().contains(word)){ 
					continue;
				}

				//to also avoid : or other characteres after a rman number
				if (word.length()>1 && NormalizeNumbers.getRomanNumbers10().contains(word.substring(0, word.length()-1))){					
					//LogOutput.print(word + " " + word.substring(0, word.length()-1) + " " + word.substring(0, word.length()-2));
					continue;
				}

				if (word.equals("st") || word.equals("dr")) //st johns dr john
					continue;

				//if (word.equals("f")) //st johns
				//	continue;

				//Single characters different from "a"
				if (word.length()<2){ //if any of the contained words is a single character
					if (word.equals("a")){
						//LogOutput.print(word);
						continue;
					}
					return false;					
				}
				else{
					has_min_size_word=true;
				}



				if (word.matches(same_character_3_times)){ //Any character occurring three or more times together
					return false;
				}



				//Strange words
				//In english it is very rare to have more than 4 consonant together (e.g. angsts). In compounds words 5 it is possible (e.g. handspring)
				//Starting the word cluster is three consonants is the maximum number, as in "split".
				//More than 3 vowels together is also strange
				if (!word.startsWith("mc") && word.matches(more3_consonants_regex + ".*")){ //Starting with more thn 3 cons. //Mc surname like McGregor include
					return false;
				}
				if (word.matches(".*" + more5_consonants_regex + ".*")){ //more than 5 inside word
					return false;
				}
				if (word.matches(".*" + more3_vowels_regex + ".*")){ //more than three vowels
					return false;
				}
				//only consonants or only vowels
				if (word.matches(consonant_regex + "+") || word.matches(vowel_regex + "+")){
					return false;
				}				
			}

			if (!has_min_size_word)
				return false;


			return true; 		

		}			



	}//end class








	/**
	 * 
	 * This class manages the annotation that LogMap currently accepts.
	 * These annotation may appear as direct strings, anonymous/named individulas or even as data assertions.
	 * 
	 * @author Ernesto
	 *
	 */
	private class ExtractStringFromAnnotationAssertionAxiom {

		private OWLAnonymousIndividual geneid_value;
		private OWLNamedIndividual namedIndiv;
		private IRI namedIndivIRI;


		ExtractStringFromAnnotationAssertionAxiom(){

		}


		protected String getAnntotationString(OWLAnnotationAssertionAxiom entityAnnAx){

			String label_value="";

			String uri_ann = entityAnnAx.getAnnotation().getProperty().getIRI().toString();


			//Accepted URIs
			if (Parameters.accepted_annotation_URIs_for_classes.contains(uri_ann)){

				if (!(label_value=asDirectValue(entityAnnAx)).equals("")){
					return processLongLabels(label_value);
				}
				if (!(label_value=asAnonymousIndividual(entityAnnAx)).equals("")){
					return processLongLabels(label_value);
				}
				if (!(label_value=asNamedIndividual(entityAnnAx)).equals("")){
					return processLongLabels(label_value);
				}
				if (!(label_value=asNamedIndividualFMA(entityAnnAx)).equals("")){
					return processLongLabels(label_value);
				}

			}

			return ""; //empty value







		}


		private String asDirectValue(OWLAnnotationAssertionAxiom entityAnnAx){
			try	{
				//LogOutput.print(((OWLLiteral)annAx.getAnnotation().getValue()).getLiteral());
				return ((OWLLiteral)entityAnnAx.getAnnotation().getValue()).getLiteral().toLowerCase();

			}
			catch (Exception e){
				//In case of error. Accessing an object in an expected way
				return "";
			}
		}


		/**
		 * As in Mouse and NCI anatomy. Annotations al rdf:labels in anonymous individuals
		 * It seems also GO ontology (to be checked)
		 * @param entityAnnAx
		 * @return
		 */
		private String asAnonymousIndividual(OWLAnnotationAssertionAxiom entityAnnAx){
			try {
				geneid_value=((OWLAnonymousIndividual)entityAnnAx.getAnnotation().getValue()).asOWLAnonymousIndividual();//.getID()
				for (OWLAnnotationAssertionAxiom annGeneidAx : onto.getAnnotationAssertionAxioms(geneid_value)){

					if (annGeneidAx.getAnnotation().getProperty().getIRI().toString().equals(rdf_label_uri)){

						return ((OWLLiteral)annGeneidAx.getAnnotation().getValue()).getLiteral().toLowerCase();
					}
				}
				return "";
			}
			catch (Exception e){
				//In case of error. Accessing an object in an expected way
				return "";
			}
		}


		/**
		 * In some OBO like ontologies
		 * @param entityAnnAx
		 * @return
		 */
		private String asNamedIndividual(OWLAnnotationAssertionAxiom entityAnnAx){
			try {
				//It is an individual
				namedIndivIRI=(IRI)entityAnnAx.getAnnotation().getValue();				
				namedIndiv=index.getFactory().getOWLNamedIndividual(namedIndivIRI);


				for (OWLAnnotationAssertionAxiom annIdiv : namedIndiv.getAnnotationAssertionAxioms(onto)){


					if (annIdiv.getAnnotation().getProperty().getIRI().toString().equals(rdf_label_uri)){

						return ((OWLLiteral)annIdiv.getAnnotation().getValue()).getLiteral().toLowerCase();
					}
				}
				return "";


			}
			catch (Exception e){
				//In case of error. Accessing an object in an expected way
				return "";
			}

		}

		/**
		 * FMA originalannotations annotations appear as datatype assertions
		 * @param entityAnnAx
		 * @return
		 */
		private String asNamedIndividualFMA(OWLAnnotationAssertionAxiom entityAnnAx){

			try{
				//It is an individual
				namedIndivIRI=(IRI)entityAnnAx.getAnnotation().getValue();

				namedIndiv=index.getFactory().getOWLNamedIndividual(namedIndivIRI);

				//for (OWLAnnotation indivAnn : namedIndiv.getAnnotations(onto)){
				for (OWLLiteral literal_syn : namedIndiv.getDataPropertyValues(index.getFactory().getOWLDataProperty(IRI.create(fma_name_uri)), onto)){

					return literal_syn.getLiteral().toLowerCase();
				}

				return "";

			}
			catch (Exception e){
				//In case of error. Accessing an object in an expected way
				return "";
			}

		}


		/**
		 * We deal with some definitions. Long ones are discarded
		 * @param def
		 * @return
		 */
		private String processLongLabels(String label){

			String words[];

			if (label.indexOf(".")<0){ //It is not a definition
				return label;
			}

			if (label.length()<15)
				return label;
			//LogOutput.print("\nDEF 1: " + label);

			label = label.split("\\.")[0];

			//LogOutput.print("\nDEF 2: " + label);

			words = label.split(" ");

			if (words.length>12){
				return "";
			}

			//LogOutput.print("\nDEF 3: " + label);

			return label;



		}




	}


	OntologyProcessing(){

	}

	public static void main(String[] args) {

		OntologyProcessing p = new OntologyProcessing();
		p.roleAssertionLabelsExtractor.processLabel("Vicenza  , a city in north-eastern Italy, is the capital of the eponymous province in the Veneto region, at the northern base of the Monte Berico, straddling the Bacchiglione. Vicenza is approximately 60 km west of Venice and 200 km east of Milan. Vicenza is a thriving and cosmopolitan city, with a rich history and culture, and many museums, art galleries, piazzas, villas, churches and elegant Renaissance palazzi.");

		//System.out.println(StringUtils.trim("Vicenza  ")+"'");
		System.out.println("Vicenza  ".trim()+"'");
		System.out.println("Vicenza  , a city in north-eastern Italy, is the capital of the eponymous province in the Veneto region, at the northern base of the Monte Berico, straddling the Bacchiglione. Vicenza is approximately 60 km west of Venice and 200 km east of Milan. Vicenza is a thriving and cosmopolitan city, with a rich history and culture, and many museums, art galleries, piazzas, villas, churches and elegant Renaissance palazzi.".trim());
	}

	//	@Override
	//	public int hashCode() {
	//		final int prime = 31;
	//		int result = 1;
	//		result = prime
	//				* result
	//				+ ((MeaningfulRootIdentifiers == null) ? 0
	//						: MeaningfulRootIdentifiers.hashCode());
	//		result = prime
	//				* result
	//				+ ((TaxRootIdentifiers == null) ? 0 : TaxRootIdentifiers
	//						.hashCode());
	//		result = prime
	//				* result
	//				+ ((ausxSetOfClasses == null) ? 0 : ausxSetOfClasses.hashCode());
	//		result = prime
	//				* result
	//				+ ((class2identifier == null) ? 0 : class2identifier.hashCode());
	//		result = prime
	//				* result
	//				+ ((className2Identifier == null) ? 0 : className2Identifier
	//						.hashCode());
	//		result = prime
	//				* result
	//				+ ((dangerousClasses == null) ? 0 : dangerousClasses.hashCode());
	//		result = prime
	//				* result
	//				+ ((dataPropName2Identifier == null) ? 0
	//						: dataPropName2Identifier.hashCode());
	//		result = prime
	//				* result
	//				+ ((filteredInvertedIndex == null) ? 0 : filteredInvertedIndex
	//						.hashCode());
	//		result = prime * result
	//				+ ((fma_name_uri == null) ? 0 : fma_name_uri.hashCode());
	//		result = prime * result
	//				+ ((fma_synonym_uri == null) ? 0 : fma_synonym_uri.hashCode());
	//		result = prime
	//				* result
	//				+ ((hasExactSynonym_uri == null) ? 0 : hasExactSynonym_uri
	//						.hashCode());
	//		result = prime
	//				* result
	//				+ ((hasRelatedSynonym_uri == null) ? 0 : hasRelatedSynonym_uri
	//						.hashCode());
	//		result = prime * result + id_onto;
	//		result = prime
	//				* result
	//				+ ((identifier2class == null) ? 0 : identifier2class.hashCode());
	//		result = prime
	//				* result
	//				+ ((identifier2stemmedlabels == null) ? 0
	//						: identifier2stemmedlabels.hashCode());
	//		result = prime * result + ((index == null) ? 0 : index.hashCode());
	//		result = prime
	//				* result
	//				+ ((individualName2Identifier == null) ? 0
	//						: individualName2Identifier.hashCode());
	//		result = prime
	//				* result
	//				+ ((inidividual2identifier == null) ? 0
	//						: inidividual2identifier.hashCode());
	//		result = prime
	//				* result
	//				+ ((invertedFileExact == null) ? 0 : invertedFileExact
	//						.hashCode());
	//		result = prime
	//				* result
	//				+ ((invertedFileExactDataProp == null) ? 0
	//						: invertedFileExactDataProp.hashCode());
	//		result = prime
	//				* result
	//				+ ((invertedFileExactObjProp == null) ? 0
	//						: invertedFileExactObjProp.hashCode());
	//		result = prime
	//				* result
	//				+ ((invertedFileIndividuals == null) ? 0
	//						: invertedFileIndividuals.hashCode());
	//		result = prime
	//				* result
	//				+ ((invertedFileRoleassertions == null) ? 0
	//						: invertedFileRoleassertions.hashCode());
	//		result = prime
	//				* result
	//				+ ((invertedFileWeakIndividuals == null) ? 0
	//						: invertedFileWeakIndividuals.hashCode());
	//		result = prime
	//				* result
	//				+ ((invertedFileWeakLabels == null) ? 0
	//						: invertedFileWeakLabels.hashCode());
	//		result = prime
	//				* result
	//				+ ((invertedFileWeakLabelsStemming == null) ? 0
	//						: invertedFileWeakLabelsStemming.hashCode());
	//		result = prime * result
	//				+ ((iri_onto == null) ? 0 : iri_onto.hashCode());
	//		result = prime * result
	//				+ ((labels_set == null) ? 0 : labels_set.hashCode());
	//		result = prime
	//				* result
	//				+ ((lexiconValues4individual == null) ? 0
	//						: lexiconValues4individual.hashCode());
	//		result = prime * result + minNumberOfRoots;
	//		result = prime * result
	//				+ ((nci_synonym_uri == null) ? 0 : nci_synonym_uri.hashCode());
	//		result = prime * result + num_syn;
	//		result = prime
	//				* result
	//				+ ((objectPropName2Identifier == null) ? 0
	//						: objectPropName2Identifier.hashCode());
	//		result = prime * result
	//				+ ((oboinowl == null) ? 0 : oboinowl.hashCode());
	//		result = prime * result
	//				+ ((rdf_label_uri == null) ? 0 : rdf_label_uri.hashCode());
	//		result = prime * result + reasonerIdentifier;
	//		result = prime
	//				* result
	//				+ ((singleWordInvertedIndex == null) ? 0
	//						: singleWordInvertedIndex.hashCode());
	//		result = prime * result + toohigh_synset_cases;
	//		result = prime * result
	//				+ (use_all_labels_for_weak_mappings ? 1231 : 1237);
	//		return result;
	//	}
	//
	//	@Override
	//	public boolean equals(Object obj) {
	//		if (this == obj)
	//			return true;
	//		if (obj == null)
	//			return false;
	//		if (getClass() != obj.getClass())
	//			return false;
	//		OntologyProcessing other = (OntologyProcessing) obj;
	//		if (MeaningfulRootIdentifiers == null) {
	//			if (other.MeaningfulRootIdentifiers != null)
	//				return false;
	//		} else if (!MeaningfulRootIdentifiers
	//				.equals(other.MeaningfulRootIdentifiers))
	//			return false;
	//		if (TaxRootIdentifiers == null) {
	//			if (other.TaxRootIdentifiers != null)
	//				return false;
	//		} else if (!TaxRootIdentifiers.equals(other.TaxRootIdentifiers))
	//			return false;
	//		if (ausxSetOfClasses == null) {
	//			if (other.ausxSetOfClasses != null)
	//				return false;
	//		} else if (!ausxSetOfClasses.equals(other.ausxSetOfClasses))
	//			return false;
	//		if (class2identifier == null) {
	//			if (other.class2identifier != null)
	//				return false;
	//		} else if (!class2identifier.equals(other.class2identifier))
	//			return false;
	//		if (className2Identifier == null) {
	//			if (other.className2Identifier != null)
	//				return false;
	//		} else if (!className2Identifier.equals(other.className2Identifier))
	//			return false;
	//		if (dangerousClasses == null) {
	//			if (other.dangerousClasses != null)
	//				return false;
	//		} else if (!dangerousClasses.equals(other.dangerousClasses))
	//			return false;
	//		if (dataPropName2Identifier == null) {
	//			if (other.dataPropName2Identifier != null)
	//				return false;
	//		} else if (!dataPropName2Identifier
	//				.equals(other.dataPropName2Identifier))
	//			return false;
	//		if (filteredInvertedIndex == null) {
	//			if (other.filteredInvertedIndex != null)
	//				return false;
	//		} else if (!filteredInvertedIndex.equals(other.filteredInvertedIndex))
	//			return false;
	//		if (fma_name_uri == null) {
	//			if (other.fma_name_uri != null)
	//				return false;
	//		} else if (!fma_name_uri.equals(other.fma_name_uri))
	//			return false;
	//		if (fma_synonym_uri == null) {
	//			if (other.fma_synonym_uri != null)
	//				return false;
	//		} else if (!fma_synonym_uri.equals(other.fma_synonym_uri))
	//			return false;
	//		if (hasExactSynonym_uri == null) {
	//			if (other.hasExactSynonym_uri != null)
	//				return false;
	//		} else if (!hasExactSynonym_uri.equals(other.hasExactSynonym_uri))
	//			return false;
	//		if (hasRelatedSynonym_uri == null) {
	//			if (other.hasRelatedSynonym_uri != null)
	//				return false;
	//		} else if (!hasRelatedSynonym_uri.equals(other.hasRelatedSynonym_uri))
	//			return false;
	//		if (id_onto != other.id_onto)
	//			return false;
	//		if (identifier2class == null) {
	//			if (other.identifier2class != null)
	//				return false;
	//		} else if (!identifier2class.equals(other.identifier2class))
	//			return false;
	//		if (identifier2stemmedlabels == null) {
	//			if (other.identifier2stemmedlabels != null)
	//				return false;
	//		} else if (!identifier2stemmedlabels
	//				.equals(other.identifier2stemmedlabels))
	//			return false;
	//		if (index == null) {
	//			if (other.index != null)
	//				return false;
	//		} else if (!index.equals(other.index))
	//			return false;
	//		if (individualName2Identifier == null) {
	//			if (other.individualName2Identifier != null)
	//				return false;
	//		} else if (!individualName2Identifier
	//				.equals(other.individualName2Identifier))
	//			return false;
	//		if (inidividual2identifier == null) {
	//			if (other.inidividual2identifier != null)
	//				return false;
	//		} else if (!inidividual2identifier.equals(other.inidividual2identifier))
	//			return false;
	//		if (invertedFileExact == null) {
	//			if (other.invertedFileExact != null)
	//				return false;
	//		} else if (!invertedFileExact.equals(other.invertedFileExact))
	//			return false;
	//		if (invertedFileExactDataProp == null) {
	//			if (other.invertedFileExactDataProp != null)
	//				return false;
	//		} else if (!invertedFileExactDataProp
	//				.equals(other.invertedFileExactDataProp))
	//			return false;
	//		if (invertedFileExactObjProp == null) {
	//			if (other.invertedFileExactObjProp != null)
	//				return false;
	//		} else if (!invertedFileExactObjProp
	//				.equals(other.invertedFileExactObjProp))
	//			return false;
	//		if (invertedFileIndividuals == null) {
	//			if (other.invertedFileIndividuals != null)
	//				return false;
	//		} else if (!invertedFileIndividuals
	//				.equals(other.invertedFileIndividuals))
	//			return false;
	//		if (invertedFileRoleassertions == null) {
	//			if (other.invertedFileRoleassertions != null)
	//				return false;
	//		} else if (!invertedFileRoleassertions
	//				.equals(other.invertedFileRoleassertions))
	//			return false;
	//		if (invertedFileWeakIndividuals == null) {
	//			if (other.invertedFileWeakIndividuals != null)
	//				return false;
	//		} else if (!invertedFileWeakIndividuals
	//				.equals(other.invertedFileWeakIndividuals))
	//			return false;
	//		if (invertedFileWeakLabels == null) {
	//			if (other.invertedFileWeakLabels != null)
	//				return false;
	//		} else if (!invertedFileWeakLabels.equals(other.invertedFileWeakLabels))
	//			return false;
	//		if (invertedFileWeakLabelsStemming == null) {
	//			if (other.invertedFileWeakLabelsStemming != null)
	//				return false;
	//		} else if (!invertedFileWeakLabelsStemming
	//				.equals(other.invertedFileWeakLabelsStemming))
	//			return false;
	//		if (iri_onto == null) {
	//			if (other.iri_onto != null)
	//				return false;
	//		} else if (!iri_onto.equals(other.iri_onto))
	//			return false;
	//		if (labels_set == null) {
	//			if (other.labels_set != null)
	//				return false;
	//		} else if (!labels_set.equals(other.labels_set))
	//			return false;
	//		if (lexiconValues4individual == null) {
	//			if (other.lexiconValues4individual != null)
	//				return false;
	//		} else if (!lexiconValues4individual
	//				.equals(other.lexiconValues4individual))
	//			return false;
	//		if (minNumberOfRoots != other.minNumberOfRoots)
	//			return false;
	//		if (nci_synonym_uri == null) {
	//			if (other.nci_synonym_uri != null)
	//				return false;
	//		} else if (!nci_synonym_uri.equals(other.nci_synonym_uri))
	//			return false;
	//		if (num_syn != other.num_syn)
	//			return false;
	//		if (objectPropName2Identifier == null) {
	//			if (other.objectPropName2Identifier != null)
	//				return false;
	//		} else if (!objectPropName2Identifier
	//				.equals(other.objectPropName2Identifier))
	//			return false;
	//		if (oboinowl == null) {
	//			if (other.oboinowl != null)
	//				return false;
	//		} else if (!oboinowl.equals(other.oboinowl))
	//			return false;
	//		if (rdf_label_uri == null) {
	//			if (other.rdf_label_uri != null)
	//				return false;
	//		} else if (!rdf_label_uri.equals(other.rdf_label_uri))
	//			return false;
	//		if (reasonerIdentifier != other.reasonerIdentifier)
	//			return false;
	//		if (singleWordInvertedIndex == null) {
	//			if (other.singleWordInvertedIndex != null)
	//				return false;
	//		} else if (!singleWordInvertedIndex
	//				.equals(other.singleWordInvertedIndex))
	//			return false;
	//		if (toohigh_synset_cases != other.toohigh_synset_cases)
	//			return false;
	//		if (use_all_labels_for_weak_mappings != other.use_all_labels_for_weak_mappings)
	//			return false;
	//		return true;
	//	}



}
