package uk.ac.ox.krr.logmap2.varia;

import java.util.*;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;


public class ExperimentLUCADA {

	String iri_patientScenario;
	OWLClass cls_PatientScenario;
	
	String ontology;
	String reasonerChoice;
	String iri_onto_guidelines;
	String iri_ontology;
	
	List<OWLAxiom> listGuidelinesAxioms = new ArrayList<OWLAxiom>();
	//Random subset of the scenarios or guidelines
	Set<OWLAxiom> subSetScenariosAxiom = new HashSet<OWLAxiom>();
	
	
	
	//Fixed we only 
	OWLOntologyManager lucada_manager;
	OWLOntology lucada_onto;
	 
	
	public ExperimentLUCADA(){
		
		ontology = "LUCADA";
		//ontology = "Integrated LUCADA";
		reasonerChoice = "Hermit";
		
		if (ontology.equals("LUCADA")){
			
			//Contains different URI for PatientScenario!
			iri_patientScenario = "http://www.semanticweb.org/ontologies/2011/3/LUCADAOntology.owl#PatientScenario";
			
			
			//Ontology containing 40 guidelines.
			iri_onto_guidelines = 
					"file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/LUCADA/ReasoningTests/GuidelineRulePerformanceExperiment/LUCADAOntology_With_one_to_forty_rules/LUCADAOntology_40.owl";


			//without guidelines
			iri_ontology = 
					"file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/LUCADA/ReasoningTests/GuidelineRulePerformanceExperiment/LUCADAOntology_With_one_to_forty_rules/LUCADAOntology.owl";
			
			
		
			
		}
		else{
			//Contains different URI for PatientScenario!
			iri_patientScenario = "http://csu6325.cs.ox.ac.uk/output/matching_26_03_2013__17_26_04_545/integratedOntology_WithSmallModules.owl#PatientScenario";
			
			//Ontology containing 40 guidelines.
			iri_onto_guidelines = 
					"file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/LUCADA/ReasoningTests/GuidelineRulePerformanceExperiment/IntegratedLUCADA-SNOMED-CT_With_one_to_forty_rules/integrated_with40_guidelines.owl";
			
			//without guidelines
			iri_ontology = 
					"file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/LUCADA/ReasoningTests/GuidelineRulePerformanceExperiment/IntegratedLUCADA-SNOMED-CT_With_one_to_forty_rules/Ernesto_final_with_interactivity_integrated_result_17Apr13.owl";

					
		}
		
		cls_PatientScenario = OWLManager.getOWLDataFactory().getOWLClass(IRI.create(iri_patientScenario));
		loadGuidelines();
		
		loadLucadaOntology();
		
		
		int MAX_REPETITIONS = 10;
		
		//Size scenarios from 1 to 40
		for (int size_scenarios = 1; size_scenarios<=40; size_scenarios++){
			
			for (int rep = 1; rep <= MAX_REPETITIONS; rep++){
				
				getRandomSubsetOfGuidelines(size_scenarios);
				
				//Add axioms to ontology
				addAxiomsToLucada(subSetScenariosAxiom);
								
				//Do reasoning and store times
				//Dispose reasoner in each iteration!!
				
				
				//Add axioms to ontology
				removeAxiomsFromLucada(subSetScenariosAxiom);
				
				
			}
			
		}
	
		
		
		//runExperiment();
		
		
		
	}
	
	
	
	public void loadLucadaOntology(){
		
		try {
			lucada_manager = OWLManager.createOWLOntologyManager();
			
			lucada_manager.setSilentMissingImportsHandling(true);
								
			lucada_onto = lucada_manager.loadOntology(IRI.create(iri_ontology));
		}
		catch(Exception e){
			System.err.println("Error loading OWL ontology: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	
	//We add new guidelines to ontology
	public void addAxiomsToLucada(Set<OWLAxiom> axioms){
		lucada_manager.addAxioms(lucada_onto, axioms);
	}
	

	//We add new guidelines to ontology
	public void removeAxiomsFromLucada(Set<OWLAxiom> axioms){
		lucada_manager.removeAxioms(lucada_onto, axioms);
	}
	
	
	//Load guidelines or patient scenario
	public void loadGuidelines() {		

		try {
			OWLOntologyManager managerOnto = OWLManager.createOWLOntologyManager();
			
			managerOnto.setSilentMissingImportsHandling(true);
								
			OWLOntology onto = managerOnto.loadOntology(IRI.create(iri_onto_guidelines));
			
			System.out.println("Subclasses of  cls_PatientScenario: " + onto.getSubClassAxiomsForSuperClass(cls_PatientScenario).size());
			
			//For each subclass of "cls_PatientScenario"
			for (OWLSubClassOfAxiom sub_ax : onto.getSubClassAxiomsForSuperClass(cls_PatientScenario)){
				
				//We add equivalence axioms
				listGuidelinesAxioms.addAll(onto.getEquivalentClassesAxioms(sub_ax.getSubClass().asOWLClass()));
				
				
			}	
			
			System.out.println("Size guidelines: " + listGuidelinesAxioms.size());
			
			//for (OWLAxiom ax : listGuidelines){
			//	System.out.println("\t"+ ax);
			//}
			
			
		}
		catch(Exception e){
			System.err.println("Error loading OWL ontology: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	
	
	
	
	private Set<OWLAxiom> getRandomSubsetOfGuidelines(int size){
		
		subSetScenariosAxiom.clear();
		
		
		Random randomGenerator = new Random(Calendar.getInstance().getTimeInMillis());
		
		while (subSetScenariosAxiom.size()<size){
		      int randomInt = randomGenerator.nextInt(listGuidelinesAxioms.size());
		      //System.out.println("Generated : " + randomInt);
		      subSetScenariosAxiom.add(listGuidelinesAxioms.get(randomInt));
		}
		
		//System.out.println("Scenarios: " + setScenarios.size());
		
		return subSetScenariosAxiom;
		
	}
	
	
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		new ExperimentLUCADA();

	}
	
	
	
	
	
	
	
	
	
	
	
	

}
