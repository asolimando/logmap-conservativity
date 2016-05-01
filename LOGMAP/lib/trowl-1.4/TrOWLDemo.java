
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import eu.trowl.owlapi3.rel.reasoner.dl.RELReasoner;
import eu.trowl.owlapi3.rel.reasoner.dl.RELReasonerFactory;
import eu.trowl.owlapi3.rel.util.Timer;

public class TrOWLDemo {

	/**
	 * @param args
	 */
	Timer timerR;
	int count;
	public static void main(String[] args) throws IOException, OWLOntologyCreationException {
		// TODO Auto-generated method stub
		
		// load ontology file with OWL API
		String file = "file:Food.owl";
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		IRI physicalURI = IRI.create(file);
		OWLOntology ontology = manager.loadOntologyFromOntologyDocument(physicalURI);
		
	    OWLDataFactory factory = manager.getOWLDataFactory();
	    String prefix = "http://TrOWL.eu/NBox/Food.owl#";

		/* create reasoner, depending on the language used in the ontology,
		 * choose between EL reasoner or DL reasoner. If not sure about the
		 * language, use DL reasoner. 
		 */
		RELReasonerFactory reasonerFactory = new RELReasonerFactory();
		RELReasoner reasoner = reasonerFactory.createReasoner(ontology);			
			
		// check ontology consistency	
		if(reasoner.isConsistent())
			System.out.println("This ontology is consistent.");
		
		System.out.println("=========================================");
			
		// get super concepts of a concept,
		OWLClass MinorSpicyFood = factory.getOWLClass(IRI.create(prefix+"MinorSpicyFood"));
		System.out.print(MinorSpicyFood.getIRI().getFragment()+" has super concepts: ");
		/* the second parameter "false" indicates that indirect 
		 * superconcepts will also be returned.
		 */
		for(OWLClass superconcept:reasoner.getSuperClasses(MinorSpicyFood, false).getFlattened())
			System.out.print(superconcept.getIRI().getFragment()+", ");
		System.out.println();
		
		// get types of an individual
		OWLNamedIndividual PorkCurry = factory.getOWLNamedIndividual(IRI.create(prefix+"pork_curry"));
		System.out.print(PorkCurry.getIRI().getFragment()+" has types: ");
		/* the second parameter "false" indicates that indirect 
		 * types will also be returned.
		 * NOTE: in the current release of REL, we do not support retrieval of direct instance yet.
		 */
		for(OWLClass type:reasoner.getTypes(PorkCurry, false).getFlattened())
			System.out.print(type.getIRI().getFragment()+", ");
		System.out.println();
		
		// get relations of an individual
		OWLNamedIndividual Salad = factory.getOWLNamedIndividual(IRI.create(prefix+"salad"));
		OWLObjectProperty Order = factory.getOWLObjectProperty(IRI.create(prefix+"order"));
		System.out.print("In the original ontology, "+Salad.getIRI().getFragment()+" is ordered by: ");
		// in our example we actually don't have anyone orders salad, yet.
		for(OWLNamedIndividual indi:reasoner.getObjectPropertyValues(Salad, Order.getInverseProperty()).getFlattened())
			System.out.print(indi.getIRI().getFragment()+", ");
		if(reasoner.getObjectPropertyValues(Salad, Order.getInverseProperty()).getFlattened().size() == 0)
			System.out.print("no one.");
		System.out.println();
		
		/* now we close the concept Vegetarian Food
		 * no role is closed
		 */
		OWLClass VegetarianFood = factory.getOWLClass(IRI.create(prefix+"VegetarianFood"));
		Set<OWLClassExpression> closedConcepts = new HashSet<OWLClassExpression>();
		closedConcepts.add(VegetarianFood);
		reasoner.close(closedConcepts, new HashSet<OWLObjectPropertyExpression>());
		
		// then we check again the individuals that orders salad
		System.out.print("In the closed ontology, "+Salad.getIRI().getFragment()+" is ordered by: ");
		/* This time, due to the closure of Vegetarian food,
		 * we will find out that Yuting is ordering Vegetarian food.
		 */
		for(OWLNamedIndividual indi:reasoner.getObjectPropertyValues(Salad, Order.getInverseProperty()).getFlattened())
			System.out.print(indi.getIRI().getFragment()+", ");
		System.out.println();	
		/* Furthermore, concept Vegetarian will also be implicitly closed,
		 * making Yuting the one and only Vegetarian.
		 * Thus non-Vegetarian will contain all the other customers. 
		 */
		OWLClass Vegetarian = factory.getOWLClass(IRI.create(prefix+"Vegetarian"));
		System.out.print("In the closed ontology, non-Vegetarian includes: ");
		for(OWLNamedIndividual indi:reasoner.getInstances(factory.getOWLObjectComplementOf(Vegetarian), false).getFlattened())
			System.out.print(indi.getIRI().getFragment()+", ");
		System.out.println();
		
		System.out.println("=========================================");
		/* we can also assert the closure of concepts and properties
		 * in an ontology with annotation properties
		 */
		// we create a new ontology, and copy all axioms from the Food ontology
		OWLOntology newOnto = manager.createOntology(IRI.create("http://TrOWL.eu/NBox/newFood.owl"));
		for(OWLLogicalAxiom axiom:ontology.getLogicalAxioms())
			manager.addAxiom(newOnto, axiom);
		/* we add an annotation property http://TrOWL.eu/REL#NBox
		 * with value close@en, and use it to annotate VegetarianFood
		 */
		OWLAnnotationProperty ano = factory.getOWLAnnotationProperty(IRI.create("http://TrOWL.eu/REL#NBox"));
		OWLAnnotation close = factory.getOWLAnnotation(ano, factory.getOWLLiteral("close", "en"));
		OWLAxiom axiom = factory.getOWLAnnotationAssertionAxiom(VegetarianFood.getIRI(), close);
		manager.addAxiom(newOnto, axiom);
		
		/* now we load the new ontology,
		 * and directly check the order relation
		 * and instances of non-Vegetarian.
		 * Results can be directly computed without further closure
		 */
		System.out.print("In the pre-closed ontology, "+Salad.getIRI().getFragment()+" is ordered by: ");
		for(OWLNamedIndividual indi:reasoner.getObjectPropertyValues(Salad, Order.getInverseProperty()).getFlattened())
			System.out.print(indi.getIRI().getFragment()+", ");
		System.out.println();	

		System.out.print("In the pre-closed ontology, non-Vegetarian includes: ");
		for(OWLNamedIndividual indi:reasoner.getInstances(factory.getOWLObjectComplementOf(Vegetarian), false).getFlattened())
			System.out.print(indi.getIRI().getFragment()+", ");
		System.out.println();
	}

}