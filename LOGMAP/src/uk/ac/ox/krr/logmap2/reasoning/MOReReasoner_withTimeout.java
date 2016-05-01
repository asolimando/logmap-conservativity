package uk.ac.ox.krr.logmap2.reasoning;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.semanticweb.more.MOReReasoner;
import org.semanticweb.more.OWL2ReasonerManager;
import org.semanticweb.more.io.LogOutput;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.util.DLExpressivityChecker;

import uk.ac.ox.krr.logmap2.owlapi.SynchronizedOWLManager;


public class MOReReasoner_withTimeout extends MOReReasoner{

	/*Default timeout*/
	private int timeoutSeconds = 100;
	private boolean isOWL2ReasonerDone=false;
	
	
	
	
	public MOReReasoner_withTimeout(OWLOntology ontlgy, int timeoutSecs) {
		super(ontlgy);
		setTimeout(timeoutSecs);
	}
	
	public void setTimeout(int timeoutSecs){
		timeoutSeconds=timeoutSecs;
	}
	
	
	
	public void classifyClasses(int timeoutSecs) {
		setTimeout(timeoutSecs);
		classifyClasses();
	}
	
	public void classifyClasses()  {

		flushChangesIfRequired();

		if (classified == notYetClassified) {

			try {
				// Unload hermit and lreasoner ontologies and dispose reasoners
				// If incremental reasoning this may not be necessary
				unloadOntologyFragmentsFromManager();

				if (isMonitorUp) {
					
					String reasoner_name = OWL2ReasonerManager.getCurrentReasonerName(OWL2REASONERID);
					
					configuration.getProgressMonitor().reasonerTaskStarted(
							"Classifying the ontology with MORe A " + reasoner_name + " " + getReasonerVersionStr() + "...");
					configuration.getProgressMonitor().reasonerTaskBusy();
				}

				long tTotal = System.currentTimeMillis();				
				findLsignature();

				// Step 1, if we know the concrete number of steps
				// if (isMonitorUp){
				// configuration.getProgressMonitor().reasonerTaskProgressChanged(1,
				// 3);
				// }
				computePartialClassificationWithHermiT();
				if (!isOWL2ReasonerDone)
					throw new Exception(); //we finish

				if (!lSignature.isEmpty()) {
					
					//completeClassificationWithLreasoner();
				} else {
					//classified = classifiedWithOWL2Reasoner;
					LogOutput
							.print("Because the computed Lsignature is empty, no other reasoner was used");
				}

				tTotal = System.currentTimeMillis() - tTotal;
				LogOutput.print("Whole classification took " + tTotal
						+ " milliseconds in total.");

				if (!isConsistent())
					LogOutput.print("The input ontology is inconsistent.");
			} 
			catch (Exception e){
				classified = notYetClassified;
				//e.printStackTrace();
			}
			finally {
				// Final step
				if (isMonitorUp) {
					configuration.getProgressMonitor().reasonerTaskStopped();
				}
			}
			
			//if (classified == notYetClassified){
				//We throw exception so that from ReasonerAccessImpl.classifyOntology we detect the ontology was not classified
				//throw new Exception("The classification using HermiT did not finished");  //Not classified!!
			//}
		}
	}
	
	
	
	protected void computePartialClassificationWithHermiT() {
		try {
			Set<OWLAxiom> compModule = extractComplementModule();
			// We keep ontology given to HermiT in order to remove it from
			// mamnager when necessary
			// If incremental reasoning this can be done in a different way
			compmodule_onto = manager.createOntology(compModule,
					IRI.create(iri_compmodule_ontology));

			// /////////////
			// manager.setOntologyDocumentIRI(compmodule_onto,
			// IRI.create("file:/D:/Users/aarmas/Documents/Ontologies/compModule_CCOv2.01.owl"));
			// // manager.setOntologyFormat(normalisedCompModule, new
			// OWLFunctionalSyntaxOntologyFormat());
			// try {
			// manager.saveOntology(compmodule_onto);
			// } catch (OWLOntologyStorageException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }
			// ////////////

			LogOutput.print(compmodule_onto.getAxiomCount()
					+ " axioms in comp module");
			
			
			//Setting OWL 2 Reasoner
			//----------------------------
			//owl2reasoner = OWL2ReasonerManager.createOWL2ReasonerInstance(
			//		compmodule_onto, OWL2REASONERID);
			//String reasoner_name = OWL2ReasonerManager.getCurrentReasonerName(OWL2REASONERID);
			HermiTAccess hermitAccess = new HermiTAccess(
					SynchronizedOWLManager.createOWLOntologyManager(),
					compmodule_onto, false); //with factory i got problems
			
			long towl2reasoner = System.currentTimeMillis();
			//hermitAccess.classifyOntology_withTimeout_throws_Exception(timeoutSeconds);
			hermitAccess.classifyOntology(false);
			
			String reasoner_name="";
			if (hermitAccess.isOntologyClassified()){
				owl2reasoner = hermitAccess.getReasoner();
				reasoner_name="HermiT";
			}
			else{
				LogOutput.print("Onto not classified with HermiT. Using 'structural' reasoner instead.");
				owl2reasoner = new StructuralReasonerExtended(compmodule_onto);
			}
			
			isOWL2ReasonerDone=true;
			//owl2reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
			//classificationWithHermiT_withTimeout(timeoutSeconds);

			
			towl2reasoner = System.currentTimeMillis() - towl2reasoner;
			LogOutput.print(reasoner_name + " took " + towl2reasoner
					+ " milliseconds"); // to classify a module of size
										// " + compModule.size() + " axioms");

		} catch (Exception e) {
			isOWL2ReasonerDone=false;
			System.err.println("Error classifying the DL module with HermiT (from MORe): " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	
	
	
	
	/**
	 * 
	 * @param timeoutSecs
	 * @throws Exception
	 */
    public void classificationWithHermiT_withTimeout(int timeoutSecs) {

    	ExecutorService executor = Executors.newFixedThreadPool(1);
    	
        //set the executor thread working
        final Future<?> future = executor.submit(new Runnable() {
            public void run() {
                try {
                	task();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

        //check the outcome of the executor thread and limit the time allowed for it to complete
        try {
            future.get(timeoutSecs, TimeUnit.SECONDS);
            future.cancel(true);
            executor.shutdown();
        }
        catch (TimeoutException e) {
        	
        	isOWL2ReasonerDone=false;
        	
        	owl2reasoner.interrupt();
        	owl2reasoner.dispose();
        	
            //interrupts the worker thread if necessary
            future.cancel(true);
            executor.shutdown();
            LogOutput.print("Timeout classifying ontology with " + owl2reasoner.getReasonerName());
            //throw new TimeoutException();
            e.printStackTrace();
    	}
        catch (Exception e) {
        	
        	isOWL2ReasonerDone=false;
        	
        	owl2reasoner.dispose();
        	
        	//interrupts the worker thread if necessary
            future.cancel(true);
            executor.shutdown();
        	
            //e.printStackTrace();
            LogOutput.print("Error classifying ontology with " + owl2reasoner.getReasonerName() + "\n" + e.getMessage()+ "\n" + e.getLocalizedMessage());
        	//throw new Exception();
            e.printStackTrace();
    	
        }        
    }
    
   
    
    
    private void task() throws Exception{
    	isOWL2ReasonerDone=false;
    	try {
    		Set<OWLOntology> importsClosure = new HashSet<OWLOntology>();
			importsClosure.add(owl2reasoner.getRootOntology());
			DLExpressivityChecker checker = new DLExpressivityChecker(importsClosure);
	        //System.out.println("Expressivity Ontology: " + checker.getDescriptionLogicName());
			
			LogOutput.print("\nClassifying '" + checker.getDescriptionLogicName() + "' Ontology with MORe-HermiT... ");
			owl2reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
	    	isOWL2ReasonerDone=true;
    	}
    	catch (Exception e){
    		LogOutput.print("Error classifying ontology with MORe-HermiT\n" + e.getMessage() + "\n" + e.getLocalizedMessage());
			e.printStackTrace();
			throw new Exception();
    	}
    	
    }
    
    
    
    
	
	
	

}
