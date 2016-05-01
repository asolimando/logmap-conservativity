package uk.ac.ox.krr.logmap2.oaei.eval;

import java.io.File;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import uk.ac.ox.krr.logmap2.oaei.FromRDFAlign2OWL;

import uk.ac.ox.krr.logmap2.reasoning.SatisfiabilityIntegration;
import uk.ac.ox.krr.logmap2.reasoning.ReasonerManager;

public class EvalImpactGSConferenceOAEI {
	
	private OWLOntology ontology1;
	private OWLOntology ontology2;
	private OWLOntology mappingsOntology;

	
	String base_path = "/usr/local/data/MappingsConferenceBenchmark/";
	
	String path_alignments = base_path + "reference-alignment-subset2012/";
	
	String iri_path_alignments = "file:" + base_path + "reference-alignment-subset2012/";
	
	String iri_path_ontologies = "file:" + base_path + "ontologies/";
	
	
	//String pattern=".rdf";
	String pattern=".owl";
	FromRDFAlign2OWL fromrdf2owl;
	
	
	/**
	 * This class evaluates the impact of reasoning with the OAEI conference ontologies and the given reference alignments
	 */
	public EvalImpactGSConferenceOAEI() throws Exception{
		
		
		File directory = new File(path_alignments);
		String filenames[] = directory.list();
		
		String[] elements;
		
		
		for(int i=0; i<filenames.length; i++){
			
			if (!filenames[i].contains(pattern)) 
				continue;
			
			elements = filenames[i].split("-|\\.");
			
			System.out.println("Loading ontologies...");
			System.out.println("\t" + iri_path_ontologies + elements[0] + ".owl");
			ontology1 = loadOntology(iri_path_ontologies + elements[0] + ".owl");
			System.out.println("\t" + iri_path_ontologies + elements[1] + ".owl");
			ontology2 = loadOntology(iri_path_ontologies + elements[1] + ".owl");
			System.out.println("\t" + iri_path_alignments + filenames[i]);
			mappingsOntology = loadOntology(iri_path_alignments + filenames[i]);
			System.out.println("...Done");
			
			/*fromrdf2owl = new FromRDFAlign2OWL(
					iri_path_ontologies + elements[0] + ".owl",
					iri_path_ontologies + elements[1] + ".owl",
					path_alignments + filenames[i]);*/
			
			SatisfiabilityIntegration.setTimeoutClassification(7200);
			SatisfiabilityIntegration.setTimeoutClassSatisfiabilityCheck(10);
			
			SatisfiabilityIntegration satIntegration = 
					new SatisfiabilityIntegration(ontology1, ontology2, mappingsOntology, false, true, true);
			
			

		}
			System.out.println("\n\n");
		
		
		
	}
		
	
	public OWLOntology loadOntology(String uri) throws Exception{		
		return loadOntology(IRI.create(uri));
	}
	
	public OWLOntology loadOntology(IRI uri) throws Exception{
		OWLOntologyManager ontologyManager=OWLManager.createOWLOntologyManager();
		return ontologyManager.loadOntology(uri);
		
	}
	

	
	
	public static void main(String[] args) {
		try{
			new EvalImpactGSConferenceOAEI();
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}
			
	
	
}
