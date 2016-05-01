import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChangeException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.OWLOntologyMerger;

import eu.trowl.owlapi3.rel.tms.model.AxiomPool;
import eu.trowl.owlapi3.rel.tms.reasoner.dl.RELReasoner;
import eu.trowl.owlapi3.rel.tms.reasoner.dl.RELReasonerFactory;
import eu.trowl.owlapi3.rel.util.Timer;

/**
 * 
 * @author Yuan Ren
 * @version 0.9.0.0
 * created on 2012-May-18
 * This example shows how to use the TrOWL to perform stream reasoning
 * and compute approximate justification for ABox.
 * In this example, we assume a static TBox, which is the LUBM ontology.
 */
public class TrOWLTMSABoxDemo {

	Timer timerR = new Timer("TrOWL ABox stream reasoning using TMS");
	List<Double> time = new ArrayList<Double>();
	List<Integer> Cnums = new ArrayList<Integer>();
	List<Integer> Rnums = new ArrayList<Integer>();
	List<Integer> cs = new ArrayList<Integer>();

	public static void main(String args[]) throws OWLOntologyCreationException, OWLOntologyChangeException, IOException
	{
		TrOWLTMSABoxDemo reasoner = new TrOWLTMSABoxDemo();
		reasoner.processOWLFile();
	}
	
	void processOWLFile() throws OWLOntologyCreationException, OWLOntologyChangeException, IOException{
		
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		// load the TBox
		manager.loadOntology(IRI.create("file:univ-bench.owl"));
		/* 
		 * specify the path to the sub-ABox files, 
		 * the following are just examples.
		 * You can specify your own path.
		 */
		String dir = "D:\\WorkSpace\\RELIWOD2010\\";
		String dir2 = "D:/WorkSpace/RELIWOD2010/";
		
		// total number of active ABox, in terms of number of sub-ABoxes
		int size = 5;
//		int update = 1;
		// create and classify the initial ontology
			for (int i = 0; i < size; i++) {
				String filename = "LUBMonto" + i + ".owl";
				if (!(new File(dir + filename).exists()))
					break;				
				manager.loadOntology(IRI.create("file:///"+dir2+filename));
			}
		OWLOntologyMerger merger = new OWLOntologyMerger(manager);
		IRI uri = IRI.create("http://www.abdn.ac.uk/~yren/LUBMSR.owl");
		OWLOntology ontology = merger.createMergedOntology(manager, uri);
		
		timerR.start();
		/*
		 *  create the ontology reasoner.
		 *  Note that in this example we use the LUBM TBoxe,
		 *  thus we use the DL reasoner.
		 */
		RELReasonerFactory relfactory = new RELReasonerFactory();
		RELReasoner reasoner = relfactory.createReasoner(ontology);
		timerR.stop();
		System.out.println(timerR);
		
		int num = 0;
		int rum = 0;
		int c = 0;
		for(OWLNamedIndividual indi:ontology.getIndividualsInSignature())
		{
			c++;
			num+= reasoner.getTypes(indi.asOWLNamedIndividual(), false).getFlattened().size();
			for(OWLObjectProperty prop:ontology.getObjectPropertiesInSignature())
				rum+= reasoner.getObjectPropertyValues(indi, prop).getFlattened().size();
				
		}
		Cnums.add(num);
		Rnums.add(rum);
		cs.add(c);
		// total number of sub-ABoxes
		int total = 15;
			for (int i = size; i < total; i++) {
				String filename2 = "LUBMonto" + i + ".owl";
				

				if (!(new File(dir + filename2).exists()))
					break;
				
				// remove the sub-ABox
				OWLOntology toDel = manager.getOntology(IRI.create("http://www.rel.com/onto"+(i-size)+".owl"));
				// add new sub-ABox
				OWLOntology toAdd = manager.loadOntology(IRI.create("file:///"+dir2+filename2));
				
				Timer timer = new Timer("Round"+(i-size+1));
				timer.start();
				
				// clean the removal
				reasoner.clean(toDel.getAxioms());
				// load the addition
				reasoner.add(toAdd);
				
				// reclassify the ontology
				reasoner.reclassify();
				timer.stop();
				time.add(timer.getTotal());
				
				num = 0;
				c = 0;
				rum = 0;
				// retrieve the active individuals
				HashSet<OWLNamedIndividual> indis = new HashSet<OWLNamedIndividual>();
				for(int j = 1;j<size+1;j++)
				{
					OWLOntology onto = manager.getOntology(IRI.create("http://www.rel.com/onto"+(i-size+j)+".owl"));
					indis.addAll(onto.getIndividualsInSignature());
				}
				for(OWLNamedIndividual indi:indis)
				{
					c++;
					/* we also compute the approximate justifications
					 * for why http://www.Department1.University0.edu/GraduateStudent66
					 * being a Person
					 */					
					if(indi.getIRI().toString().contains("http://www.Department1.University0.edu/GraduateStudent66"))
					{
						if(reasoner.getTypes(indi, false).getFlattened().size()>0)
							System.out.println("Round "+(i-size+1)+": ");
						for(OWLClass cls:reasoner.getTypes(indi, false).getFlattened())
						{
							if(cls.getIRI().getFragment().contains("Person"))
							{
							System.out.println(indi.getIRI()+" is an instance of "+cls.getIRI().getFragment()+" because of: ");
							System.out.println("========================");
							// compute one approximate justification for the classification
							AxiomPool apool = reasoner.justify(indi, cls);
							// when apool == null, the classification is a tautology
							if(apool != null)
							for(OWLLogicalAxiom ax:apool.axioms)
								System.out.println(ax);
							System.out.println();
							}

						}
					}
					num+= reasoner.getTypes(indi, false).getFlattened().size();
					for(OWLObjectProperty prop:ontology.getObjectPropertiesInSignature())
						rum+= reasoner.getObjectPropertyValues(indi, prop).getFlattened().size();

				}
				Cnums.add(num);
				Rnums.add(rum);
				cs.add(c);

			}
			// output the stream reasoning results
			
			// time used for each update
			System.out.println("Time: "+time);
			// number of active individuals in each round
			System.out.println("#IN:  "+cs);
			// number of classifications in each round
			System.out.println("#CA:  "+Cnums);
			// number of relations in each round
			System.out.println("#RA:  "+Rnums);

	}
}
