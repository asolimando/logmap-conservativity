package uk.ac.ox.krr.logmap2.reasoning;


import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import org.semanticweb.more.MOReReasoner;
import org.semanticweb.more.MOReReasonerFactory;

import uk.ac.ox.krr.logmap2.Parameters;


public class MOReAccess extends ReasonerAccessImpl {

	private int timeout = Parameters.timeout;
	
	public MOReAccess(OWLOntologyManager ontoManager, OWLOntology onto, boolean useFactory, int timeout) throws Exception{		
		super(ontoManager, onto, useFactory);
		this.timeout=timeout;
		
	}
	
	public MOReAccess(OWLOntologyManager ontoManager, OWLOntology onto, boolean useFactory) throws Exception{		
		super(ontoManager, onto, useFactory);		
	}
	
	
	protected void setUpReasoner(boolean useFactory) throws Exception{			
		
		//, new SimpleConfiguration()); //BufferingMode.NON_BUFFERING
		
		//if (useFactory){	 
		//	reasonerFactory = new MOReReasonerFactory();
		//	reasoner = reasonerFactory.createReasoner(ontoBase);
		//}
		//else{
			//reasoner=new MORe_adapted(ontoBase, timeout);
		reasoner=new MOReReasoner(ontoBase);
		//}
		
		//((MOReReasoner)reasoner).showAnyOutPut(Parameters.print_output || Parameters.print_output_always);
		
		reasonerName = "MORe";//reasoner.getReasonerName();
		
	}
	
	

	
	
	
}
