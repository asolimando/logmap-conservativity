package uk.ac.ox.krr.logmap2.reasoning;

import org.semanticweb.owlapi.model.OWLOntology;

public class MORe_adapted extends MOReReasoner_withTimeout{

	public MORe_adapted(OWLOntology ontlgy, int timeout) {
		super(ontlgy, timeout);
	}

	
	/*
	 * Already implemented like that in MORe
	 * 
	 * 
	 * Getting all disjoint classes is costly. We get only explicit disjointness.
	 * We will complete with questions (A intersection B) later  if necessary
	 *
	public NodeSet<OWLClass> getDisjointClasses(OWLClassExpression ce) {
        //super.ensurePrepared();
        OWLClassNodeSet nodeSet = new OWLClassNodeSet();
        if (!ce.isAnonymous()) {
            for (OWLOntology ontology : getRootOntology().getImportsClosure()) {
                for (OWLDisjointClassesAxiom ax : ontology.getDisjointClassesAxioms(ce.asOWLClass())) {
                    for (OWLClassExpression op : ax.getClassExpressions()) {
                        if (!op.isAnonymous() && !op.equals(ce)) { //Op must be differnt to ce
                            nodeSet.addNode(getEquivalentClasses(op));
                        }
                    }
                }
            }
        }        
        return nodeSet;
    }*/
	
	
}
