import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChangeException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.OWLOntologyMerger;

import eu.trowl.owlapi3.rel.tms.model.AxiomPool;
import eu.trowl.owlapi3.rel.tms.reasoner.el.RELReasoner;
import eu.trowl.owlapi3.rel.tms.reasoner.el.RELReasonerFactory;
import eu.trowl.owlapi3.rel.util.Timer;

/**
 * 
 * @author Yuan Ren
 * @version 0.9.0.0
 * created on 2012-May-18
 * This example shows how to use the TrOWL to perform stream reasoning
 * and compute approximate justification for TBox.
 * In this example we assume a dynamic TBox, which is a subset of the Galen ontology.
 */
public class TrOWLTMSTBoxDemo {

	Timer timerR = new Timer("TrOWL TBox stream reasoning using TMS");
	ArrayList<Double> time = new ArrayList<Double>();
	ArrayList<Integer> number = new ArrayList<Integer>();
	ArrayList<Integer> concept = new ArrayList<Integer>();

	public static void main(String args[]) throws OWLOntologyCreationException, OWLOntologyChangeException, IOException
	{		
		TrOWLTMSTBoxDemo reasoner = new TrOWLTMSTBoxDemo();
		reasoner.processOWLFile();
	}

	void processOWLFile() throws OWLOntologyCreationException, OWLOntologyChangeException, IOException
	{

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

		/* 
		 * specify the path to the sub-TBox files, 
		 * the following are just examples.
		 * You can specify your own path.
		 */
		String dir = "D:\\WorkSpace\\CIKM2011\\";
		String dir2 = "D:/WorkSpace/CIKM2011/";

		// total number of sub-TBoxes
		int total = 21;
		// size of active TBox, in terms of number of sub-TBoxes
		int size = 15;
		// create and classify the initial TBox 
		for(int i = 0;i < size; i++)
		{
			/*
			 * specify the file name of the sub-Tboxes,
			 * the following are just examples.
			 * You can specify your own names.
			 */
			String filename = "Fonto"+i+".owl";
			if (!(new File(dir + filename).exists()))
				break;
			manager.loadOntology(IRI.create("file:///"+dir2+filename));
		}
		OWLOntologyMerger merger = new OWLOntologyMerger(manager);
		IRI uri = IRI.create("http://www.abdn.ac.uk/~yren/galen.owl");
		OWLOntology ontology = merger.createMergedOntology(manager, uri);

		timerR.start();
		/*
		 *  create the ontology reasoner.
		 *  Note that in this example we use the galen sub-TBoxes,
		 *  thus we use the EL reasoner.
		 */
		RELReasonerFactory relfactory = new RELReasonerFactory();
		RELReasoner reasoner = relfactory.createReasoner(ontology);		
		timerR.stop();
		time.add(timerR.getTotal());

		OWLDataFactory factory = manager.getOWLDataFactory();

		int num = 0;
		int c = 0;
		for(OWLClass cls:ontology.getClassesInSignature())
		{
			if(cls.equals(factory.getOWLThing()) || cls.equals(factory.getOWLNothing()))
				continue;
			c++;
			num+=reasoner.getSuperClasses(cls, false).getFlattened().size();
		}
		concept.add(c);
		number.add(num);

		// size of update, in terms of number of sub-TBoxes
		int update = 1;
		// starting position of update
		int current = size;
		// update the ontology by removing and adding sub-TBoxes
		for (int i = 0; i < (total-size)/update; i++) {
			Timer timer = new Timer("Round"+(i));
			for (int j = 0; j < update; j++) {
				String filename1 = "Fonto" + (current - size) + ".owl";
				String filename2 = "Fonto" + current + ".owl";
				current++;
				if (!(new File(dir + filename2).exists()))
					break;
				// remove some sub-TBoxes
				OWLOntology toDel = manager.getOntology(IRI.create("http://www.rel.com/"+filename1));				

				// add some sub-TBoxes
				OWLOntology toAdd = manager.loadOntology(IRI.create("file:///"+dir2+filename2));

				timer.start();
				// clean the removal
				reasoner.clean(toDel.getAxioms());
				// load the addition
				reasoner.add(toAdd);
			}
			// reclassify the ontology
			reasoner.reclassify();
			timer.stop();
			time.add(timer.getTotal());

			num = 0;
			c = 0;
			// retrieve the active concepts
			HashSet<OWLClass> clss = new HashSet<OWLClass>();
			for(int k = 0;k < size;k++)
			{
				OWLOntology onto = manager.getOntology(IRI.create("http://www.rel.com/Fonto"+(current-size+k)+".owl"));
				clss.addAll(onto.getClassesInSignature());
			}
			for(OWLClass cls:clss)
			{
				if(cls.equals(factory.getOWLThing()) || cls.equals(factory.getOWLNothing()))
					continue;
				c++;
				/*  we also compute the approximate justifications 
				 * for the subsumptions of Anonymous-126
				 */
				if(cls.getIRI().getFragment().equals("Anonymous-126"))
				{
					if(reasoner.getSuperClasses(cls, false).getFlattened().size()>0)
						System.out.println("Round "+i+":");

					for(OWLClass sup:reasoner.getSuperClasses(cls, false).getFlattened())
					{
						System.out.println(cls.getIRI().getFragment()+" is a subclass of "+sup.getIRI().getFragment()+" because of: ");
						System.out.println("========================");
						// compute one approximate justification for the subsumption
						AxiomPool apool = reasoner.justify(cls, sup);
						// when apool == null, the subsumption is a tautology
						if(apool != null)
							for(OWLLogicalAxiom ax:apool.axioms)
								System.out.println(ax);
						System.out.println();
					}				
				}
				num+=reasoner.getSuperClasses(cls, false).getFlattened().size();
			}
			concept.add(c);
			number.add(num);
		}

		// output the stream reasoning results
		
		// time used for each update
		System.out.println("Time: "+time);
		// number of active concepts in each round
		System.out.println("#CN:  "+concept);
		// number of subsumptions in each round
		System.out.println("#Sub: "+number);

	}
}
