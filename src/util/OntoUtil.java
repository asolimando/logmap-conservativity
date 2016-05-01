/*******************************************************************************
 * Copyright 2016 by the Department of Computer Science (University of Genova and University of Oxford)
 * 
 *    This file is part of LogMapC an extension of LogMap matcher for conservativity principle.
 * 
 *    LogMapC is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 * 
 *    LogMapC is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 * 
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with LogMapC.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import logmap.LogMapWrapper;
import ontology.AxiomExplanation;

import org.mindswap.pellet.PelletOptions;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.elk.owl.exceptions.ElkException;
import org.semanticweb.elk.owlapi.ElkClassExpressionConverter;
import org.semanticweb.elk.owlapi.ElkConverter;
import org.semanticweb.elk.owlapi.ElkReasoner;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.elk.owlapi.proofs.AxiomExpressionWrap;
import org.semanticweb.elk.owlapi.proofs.ElkToOwlProofConverter;
import org.semanticweb.elk.owlapi.proofs.Proofs;
import org.semanticweb.elk.owlapi.wrapper.OwlConverter;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owl.explanation.api.ExplanationGenerator;
import org.semanticweb.owl.explanation.api.ExplanationGeneratorFactory;
import org.semanticweb.owl.explanation.api.ExplanationGeneratorInterruptedException;
import org.semanticweb.owl.explanation.impl.blackbox.Configuration;
import org.semanticweb.owl.explanation.impl.blackbox.EntailmentCheckerFactory;
import org.semanticweb.owl.explanation.impl.blackbox.checker.BlackBoxExplanationGeneratorFactory;
import org.semanticweb.owl.explanation.impl.blackbox.checker.SatisfiabilityEntailmentCheckerFactory;
import org.semanticweb.owl.explanation.impl.laconic.LaconicExplanationGeneratorFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.IRIDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyChangeListener;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubDataPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.UnknownOWLOntologyException;
import org.semanticweb.owlapi.owllink.OWLlinkHTTPXMLReasoner;
import org.semanticweb.owlapi.owllink.OWLlinkHTTPXMLReasonerFactory;
import org.semanticweb.owlapi.owllink.OWLlinkReasoner;
import org.semanticweb.owlapi.owllink.OWLlinkReasonerConfiguration;
import org.semanticweb.owlapi.owllink.server.AbstractOWLlinkReasonerConfiguration;
import org.semanticweb.owlapi.owllink.server.OWLlinkHTTPXMLServer;
import org.semanticweb.owlapi.owllink.server.OWLlinkServer;
import org.semanticweb.owlapi.profiles.OWL2ELProfile;
import org.semanticweb.owlapi.profiles.OWLProfileReport;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.ReasonerInterruptedException;
import org.semanticweb.owlapi.reasoner.TimeOutException;
import org.semanticweb.owlapi.util.DLExpressivityChecker;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.util.InferredAxiomGenerator;
import org.semanticweb.owlapi.util.InferredEquivalentClassAxiomGenerator;
import org.semanticweb.owlapi.util.InferredSubClassAxiomGenerator;
import org.semanticweb.owlapi.util.OWLOntologyWalker;
import org.semanticweb.owlapi.util.OWLOntologyWalkerVisitor;
import org.semanticweb.owlapi.util.SimpleIRIShortFormProvider;
import org.semanticweb.owlapi.util.Version;
import org.semanticweb.owlapitools.proofs.ExplainingOWLReasoner;
import org.semanticweb.owlapitools.proofs.OWLInference;
import org.semanticweb.owlapitools.proofs.exception.ProofGenerationException;
import org.semanticweb.owlapitools.proofs.expressions.ExpressionUtils;
import org.semanticweb.owlapitools.proofs.expressions.OWLAxiomExpression;
import org.semanticweb.owlapitools.proofs.expressions.OWLExpression;

import reasoning.ExtDisjReasoner;
import reasoning.UnsupportedDTHermitReasonerFactory;
import scc.graphAlgo.DFSReachability;
import scc.graphAlgo.NodeReachability;
import scc.graphDataStructure.LightAdjacencyList;
import scc.graphDataStructure.LightNode;
import scc.ontology.ExplanationProgMonitor;
import thread.ConservativityExplanationThread;
import thread.EntailmentExplanationThread;
import thread.EntailmentTracingThread;
import thread.OntoClassificationThread;
import thread.SatExplanationThread;
import thread.UnsatExplanationThread;
import uk.ac.manchester.cs.factplusplus.owlapiv3.FaCTPlusPlusReasonerFactory;
import uk.ac.manchester.cs.owlapi.modularity.ModuleType;
import uk.ac.manchester.cs.owlapi.modularity.SyntacticLocalityModuleExtractor;
import uk.ac.manchester.syntactic_locality.OntologyModuleExtractor;
import uk.ac.manchester.syntactic_locality.OntologyModuleExtractor.TYPEMODULE;
import uk.ac.ox.krr.logmap2.indexing.JointIndexManager;
import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;
import uk.ac.ox.krr.logmap2.utilities.Utilities;
import visitor.disjToConj.ClassificationOverapproximator;
import visitor.disjToConj.OWLAxiomOverapproximationVisitor;
import auxStructures.Pair;

import com.clarkparsia.owlapi.explanation.DefaultExplanationGenerator;
import com.clarkparsia.owlapi.modularity.locality.LocalityClass;
import com.clarkparsia.owlapi.modularity.locality.LocalityEvaluator;
import com.clarkparsia.owlapi.modularity.locality.SemanticLocalityEvaluator;
import com.clarkparsia.owlapi.modularity.locality.SyntacticLocalityEvaluator;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;

import enumerations.OS;
import enumerations.REASONER_KIND;

public class OntoUtil {
	static final String freshClassPrefix = "#Class_";
	static int nextFreshId = 0;
	private static final DateFormat dateFormat = new SimpleDateFormat(
			"yyyyMMdd_HHmmss");
	private static Calendar cal = Calendar.getInstance();
	public static OWLReasonerFactory reasonerFactory;
	private static ExplanationProgMonitor progMonitor = new ExplanationProgMonitor();
	private static SimpleIRIShortFormProvider shortFormProvider = new SimpleIRIShortFormProvider();

	private static Set<OWLReasoner> reasoners = new HashSet<>();
	// private static OWLDataFactory dataFactory = new OWLDataFactoryImpl(false,
	// false);
	private static OWLDataFactory dataFactory = OWLManager.getOWLDataFactory();
	private static LinkedList<OWLOntologyManager> managers = new LinkedList<>();
	private static OWLlinkHTTPXMLReasonerFactory owlLinkFactory = new OWLlinkHTTPXMLReasonerFactory();

	private static Map<REASONER_KIND, Boolean> owlLinkReasonersActive = new HashMap<>(
			REASONER_KIND.values().length);
	private static Map<REASONER_KIND, String> owlLinkReasonersCmd = new HashMap<>(
			REASONER_KIND.values().length);

	private static List<Process> systemProcesses = new LinkedList<>();
	private static List<OWLlinkServer> owlLinkServers = new LinkedList<>();

	static {
		managers.add(OWLManager.createOWLOntologyManager(dataFactory));

		owlLinkReasonersActive.put(REASONER_KIND.KONCLUDE, false);
		owlLinkReasonersCmd.put(REASONER_KIND.KONCLUDE,
				"lib/Konclude-v0.6.0-408-linux64/myKonclude");

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				if (!Params.os.equals(OS.LINUX)) {
					return;
				}

				System.out.println("Running Shutdown Hook");
				int c = 0, excC = 0;
				for (Process p : systemProcesses) {
					// if(p.isAlive()){
					// c++;
					// p.destroyForcibly();
					// }
				}
				ProcessBuilder pb = new ProcessBuilder(new String[]
				// {"pgrep","Konclude"});
				// {"/bin/sh","-c","kill","`pgrep Konclude`"});
						{ "/bin/sh", "-c", "pgrep Konclude | xargs kill" });
				Process proc = null;
				InputStream in = null;
				StringBuilder output = new StringBuilder();

				try {
					pb.redirectErrorStream(true);
					proc = pb.start();
					in = proc.getInputStream();
				} catch (IOException e1) {
					e1.printStackTrace();
				}

				byte[] data = null;
				try {
					data = new byte[in.available()];
					in.read(data);
					output.append(new String(data, "UTF-8"));
					in.close();
				} catch (UnsupportedEncodingException e1) {
					e1.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				System.out.println(output);
				System.out.println(c + " process(es) terminated");

				c = 0;

				for (OWLlinkServer s : owlLinkServers) {
					try {
						s.stop();
					} catch (InterruptedException e) {
						excC++;
					}
					c++;
				}
				System.out.println(c + " server(s) terminated (" + excC
						+ " interrupted)");
			}
		});
	}

	public static OWLEntity getEntityFromName(OWLOntology o, String label){
		
		for (OWLEntity e : o.getSignature()) {
			if(e.isOWLClass() || e.isOWLDataProperty() || e.isOWLObjectProperty() || 
					e.isOWLNamedIndividual()){
				if(label.equals(getIRIShortFragment(e.getIRI())))
					return e;
			}
		}
		
		return null;
	}
	
	static public String getDLName(OWLOntology onto) {
		return new DLExpressivityChecker(Collections.singleton(onto))
				.getDescriptionLogicName();
	}

	private static boolean launchOWLLinkServer(REASONER_KIND rk) {

		int port = Params.getReasonerServerPort(rk);
		if (false) {
			AbstractOWLlinkReasonerConfiguration serverConfiguration = new AbstractOWLlinkReasonerConfiguration();
			OWLlinkServer server = new OWLlinkHTTPXMLServer(
					getReasonerFactory(rk), serverConfiguration, port);
			server.run();

		} else {
			ProcessBuilder pb = new ProcessBuilder(owlLinkReasonersCmd.get(rk));
			Process proc = null;

			try {
				pb.redirectErrorStream(true);
				proc = pb.start();
				systemProcesses.add(proc);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}

		FileUtil.writeLogAndConsole("OWLLinkServer started for " + rk
				+ " on port " + port);

		return true;
	}

	public static OWLOntologyManager getManager(boolean createNew) {
		if (createNew) {
			managers.add(OWLManager.createOWLOntologyManager(dataFactory));
			if (!Params.bufferingReasoner && !Params.incrementalReasoning) {
				managers.getLast().addOntologyChangeListener(
						new OWLOntologyChangeListener() {
							@Override
							public void ontologiesChanged(
									List<? extends OWLOntologyChange> arg0)
									throws OWLException {
							}
						});
			}
		}
		return managers.getLast();
	}

	public static OWLDataFactory getDataFactory() {
		// return manager.getOWLDataFactory();
		return dataFactory;
	}

	public static void disposeReasoners(OWLReasoner... r) {
		for (OWLReasoner reas : r) {
			if (reas != null) {
				reas.interrupt();
				reas.dispose();
				reasoners.remove(reas);
				if (reas instanceof ExtDisjReasoner)
					reasoners.remove(((ExtDisjReasoner) reas).getReasoner());
			}
		}
	}

	public static Set<OWLAxiom> convertAlignmentToAxioms(OWLOntology fstO,
			OWLOntology sndO, Set<MappingObjectStr> mappings) {
		Set<OWLAxiom> alignment = new HashSet<>();
		Set<OWLEntity> sig1 = fstO.getSignature(true), sig2 = sndO
				.getSignature(true);
		for (MappingObjectStr m : mappings) {
			alignment.addAll(OntoUtil.convertMappingToAxiom(sig1, sig2, m));

			// if(OntoUtil.convertMappingToAxiom(sig1,sig2,m).size()
			// != LogMapWrapper.countMappings(m))
			// FileUtil.writeErrorLogAndConsole(
			// m + ": "+
			// OntoUtil.convertMappingToAxiom(sig1,sig2,m).size() +
			// " vs " + LogMapWrapper.countMappings(m));
		}

		if (alignment.size() != LogMapWrapper.countMappings(mappings)) {
			if (Params.oaei)
				FileUtil.writeErrorLogAndConsole(alignment.size()
						+ " mapping(s) but expected "
						+ LogMapWrapper.countMappings(mappings));
			else
				// throw new RuntimeException(alignment.size()
				// + " mapping(s) but expected " +
				// LogMapWrapper.countMappings(mappings));
				FileUtil.writeErrorLogAndConsole(alignment.size()
						+ " mapping(s) but expected "
						+ LogMapWrapper.countMappings(mappings));
		}

		return alignment;
	}

	private static void alterOntologyWithAxioms(OWLOntology o,
			Set<OWLAxiom> axioms, OWLOntologyManager manager, boolean remove,
			boolean suppressOutput) {
		List<OWLOntologyChange> changes = remove ? manager.removeAxioms(o,
				axioms) : manager.addAxioms(o, axioms);
		manager.applyChanges(changes);
		if (!suppressOutput)
			FileUtil.writeLogAndConsole(changes.size() + " axioms "
					+ (remove ? "removed" : "added"));
	}

	public static void addAxiomsToOntology(OWLOntology o,
			OWLOntologyManager manager, Set<OWLAxiom> axioms,
			boolean suppressOutput) {
		alterOntologyWithAxioms(o, axioms, manager, false, suppressOutput);
	}

	public static void removeAxiomsFromOntology(OWLOntology o,
			OWLOntologyManager manager, Set<OWLAxiom> axioms,
			boolean suppressOutput) {
		alterOntologyWithAxioms(o, axioms, manager, true, suppressOutput);
	}

	public static Set<Explanation<OWLAxiom>> getTracingForAxiom(OWLReasoner r,
			OWLAxiom ax, int limit, int timeout, int printEach)
			throws ProofGenerationException, TimeoutException {
		
		List<Set<Explanation<OWLAxiom>>> l;
		try {
			l = Util.runExplanationCallables(TimeUnit.SECONDS, timeout, 
			Collections.singleton(
					new EntailmentTracingThread(ax, r, limit, timeout, 
					Params.suppressFullReasoningOutput, printEach)));
		} catch (InterruptedException | ExecutionException e) {
			FileUtil.writeErrorLogAndConsole(e.getMessage());
			return null;
		} catch (TimeoutException e) {
			FileUtil.writeLogAndConsole("Timeout of " + timeout + 
					"(s) reached while computing the trace");
			throw e;
		}
		
		Set<Explanation<OWLAxiom>> res = new HashSet<>();
		for (Set<Explanation<OWLAxiom>> s : l)
			res.addAll(s);

		return res;
		
//		ExecutorService executor = Executors.newFixedThreadPool(1);
//
//		long time = Util.getMSec();
//		
//		EntailmentTracingThread thread = new EntailmentTracingThread(ax, r,
//				limit, timeout, Params.suppressFullReasoningOutput, printEach);
//
//		Future<Set<Explanation<OWLAxiom>>> f = executor.submit(thread);
//
//		executor.shutdown();
//
//		Set<Explanation<OWLAxiom>> res = null;
//
//		try {
//			res = f.get(timeout, TimeUnit.SECONDS);
//		} catch (InterruptedException | ExecutionException e) {
//			FileUtil.writeErrorLogAndConsole(e.getMessage());
//			return null;
//		} catch (TimeoutException e) {
//			FileUtil.writeLogAndConsole("Timeout of " + timeout
//					+ "(s) reached while computing the trace");
//			f.cancel(true);
//			executor.shutdownNow();
//			try {
//				org.semanticweb.elk.reasoner.Reasoner internalReasoner = 
//						((ElkReasoner) r).getInternalReasoner();
//				if(!internalReasoner.isInterrupted()){
//					FileUtil.writeLogAndConsole("Interrupting ELK");
//					internalReasoner.interrupt();
//					// otherwise next request will fail!
//					internalReasoner.clearInterrupt();
//				}
//				throw e;
//			}
//			catch(ReasonerInterruptedException e1){
//				// expected, do nothing else than printing
//				FileUtil.writeLogAndConsole("Reasoner interrupted after timeout");
//			}
//		} finally {
//			executor.shutdownNow();
//			time = Util.getDiffmsec(time);
//		}
//
//		return res;
	}

	private static OWLReasoner recreateReasonerAndDisposeOld(OWLReasoner rOld) {
		OWLReasoner rNew = getReasoner(rOld.getRootOntology(), 
				REASONER_KIND.getKind(rOld), 
				getManager(false));
		OntoUtil.disposeReasoners(rOld);
		return rNew;
	}

	public static Set<Explanation<OWLAxiom>> getTracingForAxiom(OWLReasoner r,
			OWLAxiom ax, int limit, int timeout, boolean suppressOutput, 
			int printEach) throws ProofGenerationException, 
			InterruptedException, ExecutionException, TimeoutException {
		
		// for comparison only
		boolean just = false;

		boolean proof = false;
		boolean traceJust = true;
		
		boolean fullOutput = false;
		
		// Get the first derivable expression which corresponds to the
		// entailment.
		// "Derivable" means that it can provide access to inferences which
		// directly derived it.

		OWLExpression derived = null;
		try {
			derived = ((ElkReasoner) r).getDerivedExpression(ax);
		}
		
		catch(ProofGenerationException e){
			FileUtil.writeErrorLogAndConsole(REASONER_KIND.ELKTRACE.toString() 
					+ " interrupted while deriving expression");
			return null;
		}
		
		Set<Explanation<OWLAxiom>> res = null;
		
//		return unwindProofs(derived, limit, suppressOutput);

		long time = Util.getMSec();
		
		if(Thread.currentThread().isInterrupted())
			return null;
		
		if(traceJust){
			res = Collections.singleton(new Explanation<OWLAxiom>(ax,
					Proofs.getUsedAxioms((ExplainingOWLReasoner) r,ax,limit>1)));
			
			FileUtil.writeLogAndConsole(Util.getDiffmsec(time) + 
					" (ms) Flatten" + (fullOutput ? (": " + res) : ""));

			if(Thread.currentThread().isInterrupted())
				return null;
			
			if(res.iterator().hasNext()){
				res = OntoUtil.getExplanationForAxiom(ax, 
						res.iterator().next().getAxioms(), limit, reasonerFactory, 
						timeout, Params.laconicJust, printEach);
			
				FileUtil.writeLogAndConsole(Util.getDiffmsec(time) + 
						" (ms) Trace+Just " + ax + (fullOutput ? (": " + res) : ""));
			}
		}
		
		if(Thread.currentThread().isInterrupted())
			return null;
	
		if(just){
			time = Util.getMSec();
			
			res = OntoUtil.getExplanationForAxiom(ax, r.getRootOntology(), limit, 
					reasonerFactory, timeout, Params.laconicJust, printEach);
			
			FileUtil.writeLogAndConsole(Util.getDiffmsec(time) + 
					" (ms) Justifications " + ax + (fullOutput ? (": " + res) : ""));
		}

		if(Thread.currentThread().isInterrupted())
			return null;
		
		if(proof){
			
			time = Util.getMSec();
				
			try {
				res = getProofsAsExplanations(derived, limit, suppressOutput);
				
				FileUtil.writeLogAndConsole(Util.getDiffmsec(time) 
						+ " (ms) Proofs " + ax + (fullOutput ? (": " + res) : ""));
				
	//			for (Explanation<OWLAxiom> explanation : res) 
	//				if(!isEntailed(explanation.getAxioms(), ax))
	//					FileUtil.writeErrorLogAndConsole(explanation + 
	//					" does not entail axiom " + ax);
			}
			catch(NoSuchElementException e){
				e.printStackTrace();
				FileUtil.writeErrorLogAndConsole(e.toString());
				System.exit(1);
			}
		}
		
		return res;
	}
	
	public static boolean isEntailed(Set<OWLAxiom> axioms, OWLAxiom ax) {
		OWLOntologyManager manager = getManager(false); 
		OWLOntology onto = null;
		boolean res = false;
		OWLReasoner r = null;
		try {
			onto = manager.createOntology(axioms);
			r = getReasoner(onto, REASONER_KIND.PELLET, manager);
			res = r.isEntailed(ax);
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
		finally {
			OntoUtil.disposeReasoners(r);
		}
		return res;
	}
	
	private static Set<Explanation<OWLAxiom>> getProofsAsExplanations(
			OWLExpression expression, int limit, boolean suppressOutput)
			throws ProofGenerationException {

		Set<LinkedList<OWLExpression>> completedProofs = new HashSet<>();
		LinkedList<Pair<LinkedList<OWLExpression>>> incompleteProofs = 
				new LinkedList<>();
		
		LinkedList<OWLExpression> tmpProof = new LinkedList<>();
		LinkedList<OWLExpression> toBeExpanded = new LinkedList<>();
		toBeExpanded.add(expression);
		incompleteProofs.add(new Pair<>(toBeExpanded,tmpProof));

		Set<Explanation<OWLAxiom>> res = new HashSet<>();
		
		while (!incompleteProofs.isEmpty()) {
			Pair<LinkedList<OWLExpression>> proof = incompleteProofs.get(0);
			LinkedList<OWLExpression> toExpand = proof.getFirst();

			int branches = 0;

			if(!toExpand.isEmpty()){
			
				LinkedList<OWLExpression> expanded = proof.getSecond();
				OWLExpression tmpExpr = toExpand.removeFirst(); 
				expanded.add(tmpExpr);

				for (OWLInference inf : tmpExpr.getInferences()) {
	
					if(++branches > 1){					
						Pair<LinkedList<OWLExpression>> newProof = 
								new Pair<>(new LinkedList<>(toBeExpanded),		
										new LinkedList<>(expanded));
						
						newProof.getFirst().addAll(inf.getPremises());
						
						incompleteProofs.add(newProof);
					}
					else
						toExpand.addAll(inf.getPremises());
					
					if(limit > 0 && branches >= limit)
						break;
				}
			}
			
			if(toExpand.isEmpty()){
				incompleteProofs.remove(proof);
				completedProofs.add(proof.getSecond());
			}
			
			if(Thread.interrupted())
				break;
		}

		for (LinkedList<OWLExpression> proof : completedProofs) {
			Set<OWLAxiom> just = new HashSet<>();
			for (OWLExpression expr : proof)
				just.add(ExpressionUtils.getAxiom(expr));
			Explanation<OWLAxiom> expl = new Explanation<>(
					ExpressionUtils.getAxiom(expression), just);
			res.add(expl);			
		}

		return res;
	}

	private static Set<Explanation<OWLAxiom>> unwindProofs(
			OWLExpression expression, int limit, boolean suppressOutput)
			throws ProofGenerationException {
		// Start recursive unwinding
		LinkedList<OWLExpression> toDo = new LinkedList<OWLExpression>();
		Set<OWLExpression> done = new HashSet<OWLExpression>();

		toDo.add(expression);
		done.add(expression);

		int numInf = 0;

		Set<Explanation<OWLAxiom>> res = new HashSet<>();
		Set<OWLAxiom> just = new HashSet<>();
		
		while (true) {
			OWLExpression next = toDo.poll();

			if (next == null)
				break;

			if (!suppressOutput)
				FileUtil.writeLogAndConsole("Current expression: " + next);

			for (OWLInference inf : next.getInferences()) {
				if (!suppressOutput)
					FileUtil.writeLogAndConsole("\t\t" + inf);

				// Recursively unwind premise inferences
				for (OWLExpression premise : inf.getPremises()) {
					if (!suppressOutput)
						FileUtil.writeLogAndConsole("\t\t\tPremise: " + premise);

					if (done.add(premise))
						toDo.addFirst(premise);
				}
				if ( // (limit > 0 && ++numInf >= limit) ||
				Thread.interrupted())
					break;
			}
		}

		for (OWLExpression expr : done)
			just.add(ExpressionUtils.getAxiom(expr));

		Explanation<OWLAxiom> expl = new Explanation<>(
				ExpressionUtils.getAxiom(expression), just);

		res.add(expl);
		
		return res;
	}

	public static Set<Explanation<OWLAxiom>> getExplanationForAxiom(
			OWLAxiom ax, OWLOntology alignOnto, int limit,
			OWLReasonerFactory reasonerFactory, int timeout, boolean laconic, 
			int printEach) throws InterruptedException, ExecutionException, TimeoutException {

		return getExplanationForAxiom(ax, alignOnto.getAxioms(), limit, 
				reasonerFactory, timeout, laconic, printEach);
	}
	
	public static Set<Explanation<OWLAxiom>> getExplanationForAxiom(
			OWLAxiom ax, Set<OWLAxiom> axioms, int limit,
			OWLReasonerFactory reasonerFactory, int timeout, boolean laconic, 
			int printEach) throws InterruptedException, ExecutionException, 
			TimeoutException, TimeOutException {

		EntailmentExplanationThread explThread = new EntailmentExplanationThread(
				axioms, ax, reasonerFactory, limit,
				Params.suppressFullReasoningOutput, timeout * 1000, laconic, printEach); 

		List<Set<Explanation<OWLAxiom>>> repairs = null;

//		Set<Explanation<OWLAxiom>> repair = null;
//		repair = explThread.call();
		try {
			repairs = Util.runExplanationCallables(TimeUnit.SECONDS, timeout*limit, 
					Collections.singletonList(explThread));
		}
		catch(ExecutionException e){
			Throwable ee = e.getCause ();
	
		    if (ee instanceof TimeOutException)
		    	throw (TimeOutException) ee;
		    else if(ee instanceof ExplanationGeneratorInterruptedException)
		    	throw new TimeOutException();
		    else
		    	throw e;
	    }
		if(repairs == null || repairs.isEmpty())
			return null;
		
		return repairs.iterator().next();
	}

	public static Set<Explanation<OWLAxiom>> getExplanationForUnsat(OWLClass c,
			OWLOntology alignOnto, int limit,
			OWLReasonerFactory reasonerFactory, int timeout, int printEach) 
					throws InterruptedException, ExecutionException, 
					TimeoutException, TimeOutException {

		if (c.isOWLNothing()) {
			FileUtil.writeLogAndConsole("Bottom class is empty by definition");
			return null;
		}

		UnsatExplanationThread explThread = new UnsatExplanationThread(
				alignOnto, c, reasonerFactory, limit,
				Params.suppressFullReasoningOutput, timeout * 1000, // sec -> msec
				printEach); 

		List<Set<Explanation<OWLAxiom>>> repairs = null;

//		Set<Explanation<OWLAxiom>> repair = null;
//		repair = explThread.call();
		
		try {
			repairs = Util.runExplanationCallables(TimeUnit.SECONDS, timeout*limit, 
					Collections.singletonList(explThread));
		}
		catch(ExecutionException e){
			Throwable ee = e.getCause ();

		    if (ee instanceof TimeOutException)
		    	throw (TimeOutException) ee;
		    else if(ee instanceof ExplanationGeneratorInterruptedException)
		    	throw new TimeOutException();
		    else
		    	throw e;
	    }
		if(repairs == null || repairs.isEmpty())
			return null;
		
		return repairs.iterator().next();
	}

	public static Set<MappingObjectStr> repairUnsatisfiabilitiesFullReasoning(
			Set<OWLClass> unsats, OWLOntology fstO, OWLOntology sndO,
			OWLOntology alignOnto, Set<MappingObjectStr> mappings,
			boolean useELK) {

		Set<MappingObjectStr> repair = new HashSet<>();
		Map<OWLAxiom, Double> mappingsMap = new HashMap<>();
		Set<OWLEntity> sig1 = fstO.getSignature(true);
		Set<OWLEntity> sig2 = sndO.getSignature(true);

		for (MappingObjectStr m : mappings)
			for (OWLAxiom ax : convertMappingToAxiom(sig1, sig2, m))
				mappingsMap.put(ax, m.getConfidence());

		unsats.remove(OntoUtil.getDataFactory().getOWLNothing());

		int d = 0;

		for (OWLClass c : unsats) {
			++d;
			SatExplanationThread explThread = new SatExplanationThread(
					alignOnto, mappingsMap, c, reasonerFactory, 1, useELK,
					Params.suppressFullReasoningOutput,
					Params.timeoutFullRepairExplanation);

			Set<OWLAxiom> locRepair = null;

			try {
				locRepair = explThread.call();
			} catch (org.semanticweb.HermiT.datatypes.UnsupportedDatatypeException e) {
				FileUtil.writeLogAndConsole("Unsupported datatype, switching to Pellet");
				explThread.changeReasonerFactoryPellet();
				reasonerFactory = new PelletReasonerFactory();
				try {
					locRepair = explThread.call();
				} catch (org.semanticweb.owlapi.reasoner.TimeOutException
						| ExplanationGeneratorInterruptedException e1) {
					locRepair = elkExplanationUnsatRepair(alignOnto,
							mappingsMap, c, Params.suppressFullReasoningOutput);
				}

			} catch (org.semanticweb.owlapi.reasoner.TimeOutException
					| ExplanationGeneratorInterruptedException e) {
				locRepair = elkExplanationUnsatRepair(alignOnto, mappingsMap,
						c, Params.suppressFullReasoningOutput);
			}

			if (locRepair == null || locRepair.isEmpty()) {
				FileUtil.writeErrorLogAndConsole("Class " + c
						+ " cannot be repaired");
				continue;
			}

			FileUtil.writeLogAndConsole(d + " repaired class " + c + ": "
					+ locRepair);

			repair.addAll(OntoUtil.convertAxiomsToAlignment(locRepair));

			if (Params.singleClassFullRepairStep)
				break;
		}

		return repair;
	}

	public static Set<OWLAxiom> elkExplanationUnsatRepair(
			OWLOntology alignOnto, Map<OWLAxiom, Double> mappingsMap,
			OWLClass c, boolean suppressOutput) {

		if (reasonerFactory instanceof ElkReasonerFactory)
			return null;

		FileUtil.writeLogAndConsole("Explanation call using "
				+ reasonerFactory.getReasonerName() + " timed out after "
				+ (Params.timeoutFullRepairExplanation / 1000)
				+ " s, switching to ELK");

		SatExplanationThread satThread = new SatExplanationThread(alignOnto,
				mappingsMap, c, reasonerFactory, 1, true, suppressOutput, 0);

		Set<OWLAxiom> locRepair = satThread.call();

		if (locRepair == null || locRepair.isEmpty()) {
			try {
				locRepair = new SatExplanationThread(alignOnto, mappingsMap, c,
						reasonerFactory, 1, false, suppressOutput, 0).call();
			} catch (org.semanticweb.owlapi.reasoner.TimeOutException
					| ExplanationGeneratorInterruptedException e) {
				throw new Error(reasonerFactory.getReasonerName() + " failed");
			}
		}

		return locRepair;
	}

	public static boolean checkDirectViolation(OWLOntology inputOnto,
			OWLOntology alignOnto, Pair<OWLClass> v, boolean useELK,
			OWLReasoner alignR, boolean suppressOutput) {
		return checkDirectViolation(inputOnto, alignOnto, v, reasonerFactory,
				Params.maxExplanationsForDirectViol, useELK, alignR,
				suppressOutput);
	}

	public static boolean checkDirectViolation(OWLOntology inputOnto,
			OWLOntology alignOnto, Pair<OWLClass> v,
			OWLReasonerFactory reasonerFactory, int limit, boolean useELK,
			OWLReasoner alignR, boolean suppressOutput) {

		ConservativityExplanationThread explThread = new ConservativityExplanationThread(
				inputOnto, alignOnto, v, reasonerFactory, limit, true, alignR,
				suppressOutput);

		return explThread.call();
	}

	public static Set<Explanation<OWLAxiom>> computeSubsumptionExplanation(
			OWLOntology o, OWLOntologyManager manager, OWLSubClassOfAxiom ax,
			int limit, boolean useELK) {

		EntailmentCheckerFactory<OWLAxiom> ecf = new SatisfiabilityEntailmentCheckerFactory(
				!useELK ? reasonerFactory : new ElkReasonerFactory());

		ExplanationGeneratorFactory<OWLAxiom> explGenFactory = new LaconicExplanationGeneratorFactory<OWLAxiom>(
				new BlackBoxExplanationGeneratorFactory<OWLAxiom>(
						new Configuration<OWLAxiom>(ecf)));

		ExplanationGenerator<OWLAxiom> exManager = explGenFactory
				.createExplanationGenerator(o);

		return limit == 0 ? exManager.getExplanations(ax) : exManager
				.getExplanations(ax, limit);
	}

	public static Set<Explanation<OWLAxiom>> computeSubsumptionExplanation(
			OWLOntology o, OWLOntologyManager manager, OWLSubClassOfAxiom ax,
			boolean useELK) {

		return computeSubsumptionExplanation(o, manager, ax, 0, useELK);
	}

	public static Set<AxiomExplanation> computeExplanations(
			OWLOntologyManager manager, OWLClass c, OWLReasoner reasoner) {

		DefaultExplanationGenerator exManager = new DefaultExplanationGenerator(
				manager, OntoUtil.reasonerFactory, reasoner.getRootOntology(),
				reasoner, progMonitor);

		Set<Set<OWLAxiom>> explanations = exManager.getExplanations(c,
				Params.explanationsNumber);

		Set<AxiomExplanation> axiomExplanations = new HashSet<>();
		for (Set<OWLAxiom> explanation : explanations)
			axiomExplanations.add(new AxiomExplanation(getDataFactory()
					.getOWLDeclarationAxiom(c), explanation));

		return axiomExplanations;
	}

	static public Set<AxiomExplanation> computeExplanations(
			OWLOntologyManager manager, OWLAxiom a, OWLReasoner reasoner) {

		DefaultExplanationGenerator exManager = new DefaultExplanationGenerator(
				manager, OntoUtil.reasonerFactory, reasoner.getRootOntology(),
				reasoner, progMonitor);

		Set<Set<OWLAxiom>> explanations = exManager.getExplanations(a,
				Params.explanationsNumber);

		Set<AxiomExplanation> axiomExplanations = new HashSet<>();
		for (Set<OWLAxiom> explanation : explanations)
			axiomExplanations.add(new AxiomExplanation(a, explanation));

		return axiomExplanations;
	}

	// public static OWLAxiom createDisjointAxiom(OWLDataFactory dataFactory,
	// OWLClass c1, OWLClass c2){
	// OWLAxiom disjAx = null;
	// if(!c1.isAnonymous() && c1.isClassExpressionLiteral() &&
	// !c2.isAnonymous() && c2.isClassExpressionLiteral()){
	// disjAx = dataFactory.getOWLDisjointClassesAxiom(
	// c1,c2);
	// if(disjAx.getClassesInSignature().size() != 2){
	// FileUtil.writeErrorLogAndConsole(disjAx);
	// return null;
	// }
	// }
	// return disjAx;
	// }
	//
	// public static Set<OWLAxiom> createDisjAxioms(Set<OWLClass> classes,
	// OWLDataFactory dataFac){
	// Set<OWLAxiom> disj = new HashSet<>();
	// for (OWLClass c1 : classes) {
	// for (OWLClass c2 : classes) {
	// if(!c1.equals(c2)){
	// disj.add(dataFac.getOWLDisjointClassesAxiom(c1, c2));
	// }
	// }
	// }
	// return disj;
	// }

	public static void approximateAxiom(OWLAxiom ax) {
		ax.accept(new OWLAxiomOverapproximationVisitor(dataFactory, false));
	}

	// it takes an ontology and returns its overapproximation wrt classification
	public static OWLOntology overApproximateOntologyClassification(
			OWLOntologyManager manager, OWLOntology onto,
			JointIndexManager index) {
		ClassificationOverapproximator.computeApproximation(onto, index);
		return onto;
	}

	public static boolean checkLocality(OWLAxiom ax, Set<OWLEntity> signature,
			OWLOntologyManager manager, OWLReasonerFactory fac) {
		LocalityEvaluator eval = (manager == null) ? new SyntacticLocalityEvaluator(
				LocalityClass.TOP_BOTTOM) : new SemanticLocalityEvaluator(
				manager, fac);
		return eval.isLocal(ax, signature);
	}

	public static void printClassification(Node<OWLClass> parent,
			OWLReasoner reasoner, int depth) {
		// skip bottom
		if (parent.isBottomNode()) {
			return;
		}
		DefaultPrefixManager pm = new DefaultPrefixManager(reasoner
				.getRootOntology().getOntologyID().getOntologyIRI().toString());

		// Print an indent to denote parent-child relationships
		printClassificationIndent(depth);
		// Now print the node (containing the child classes)
		printClassificationNode(pm, parent);
		for (Node<OWLClass> child : reasoner.getSubClasses(
				parent.getRepresentativeElement(), true)) {
			printClassification(child, reasoner, depth + 1);
		}
	}

	private static void printClassificationIndent(int depth) {
		for (int i = 0; i < depth; i++)
			FileUtil.writeLogAndConsole("    ");
	}

	private static void printClassificationNode(DefaultPrefixManager pm,
			Node<OWLClass> node) {
		// Print out a node as a list of class names in curly brackets
		FileUtil.writeLogAndConsoleNONL("{");
		for (Iterator<OWLClass> it = node.getEntities().iterator(); it
				.hasNext();) {
			OWLClass cls = it.next();
			// User a prefix manager to provide a slightly nicer shorter name
			FileUtil.writeLogAndConsoleNONL(pm.getShortForm(cls));
			if (it.hasNext()) {
				FileUtil.writeLogAndConsole(" ");
			}
		}
		FileUtil.writeLogAndConsole("}");
	}

	public static void printClassification(OWLOntology filter,
			Node<OWLClass> parent, OWLReasoner reasoner, int depth) {
		// skip bottom
		if (parent.isBottomNode()) {
			return;
		}
		DefaultPrefixManager pm = new DefaultPrefixManager(reasoner
				.getRootOntology().getOntologyID().getOntologyIRI().toString());

		boolean toFilter = true;
		for (OWLClass owlClass : parent.getEntities())
			if (filter.containsClassInSignature(owlClass.getIRI()))
				toFilter = false;

		if (!toFilter) {
			// Print an indent to denote parent-child relationships
			printClassificationIndent(depth);
			// Now print the node (containing the child classes)
			printClassificationNode(filter, pm, parent);
		}
		depth = toFilter ? depth : depth + 1;

		for (Node<OWLClass> child : reasoner.getSubClasses(
				parent.getRepresentativeElement(), true)) {
			printClassification(filter, child, reasoner, depth);
		}
	}

	public static boolean checkClassification(List<OWLReasoner> reasoners) {
		for (OWLReasoner r : reasoners) {
			if (!checkClassification(r))
				return false;
		}
		return true;
	}

	public static boolean checkClassification(OWLReasoner r) {
		try {
			r.isConsistent();
		}
		catch(ReasonerInterruptedException  e){
			FileUtil.writeErrorLogAndConsole("Reasoner was interrupted, cannot proceed");
			return false;
		}
		catch(NullPointerException e){
			FileUtil.writeErrorLogAndConsole("Reasoner timed out, cannot proceed");
			return false;			
		}
		return isPrecomputed(r, InferenceType.CLASS_HIERARCHY);
	}

	private static void printClassificationNode(OWLOntology filter,
			DefaultPrefixManager pm, Node<OWLClass> node) {
		// Print out a node as a list of class names in curly brackets
		FileUtil.writeLogAndConsoleNONL("{");
		for (Iterator<OWLClass> it = node.getEntities().iterator(); it
				.hasNext();) {
			OWLClass cls = it.next();
			if (!filter.containsClassInSignature(cls.getIRI()))
				continue;
			// User a prefix manager to provide a slightly nicer shorter name
			FileUtil.writeLogAndConsoleNONL(pm.getShortForm(cls));
			if (it.hasNext()) {
				FileUtil.writeLogAndConsoleNONL(" ");
			}
		}
		FileUtil.writeLogAndConsole("}");
	}

	public static long ontologyClassification(boolean alignedOnto,
			boolean printIt, List<OWLReasoner> reasoners, boolean tryPellet,
			boolean useExtended) {
		long time = ontologyClassification(alignedOnto, printIt, reasoners,
				Params.reasonerKind, tryPellet);
		List<OWLReasoner> listCopy = new ArrayList<>(reasoners);
		Iterator<OWLReasoner> itr = listCopy.iterator();
		while (itr.hasNext()) {
			OWLReasoner r = itr.next();
			int index = reasoners.indexOf(r);
			// we use an extended version providing only direct disjoint classes
			// if(alignedOnto || OntoUtil.isELKReasoner(r)){
			if (useExtended)
				r = new ExtDisjReasoner(r);
			reasoners.remove(index);
			reasoners.add(index, r);
			// }
		}

		return time;
	}

	public static long ontologyClassification(boolean alignedOnto,
			boolean printIt, List<OWLReasoner> reasoners, boolean tryPellet) {
		return ontologyClassification(alignedOnto, printIt, reasoners,
				tryPellet, true);
	}

	public static boolean isELKReasoner(OWLReasoner r) {
		if (Params.oaei)
			return r.getClass().getName().toLowerCase().contains("elk");

		return r.getReasonerName() == null
				|| r.getReasonerName().toLowerCase().contains("elk");
	}

	public static boolean isClassificationPrecomputed(OWLReasoner r) {
		return isPrecomputed(r, InferenceType.CLASS_HIERARCHY);
	}

	public static boolean isPrecomputed(OWLReasoner r, InferenceType infType) {
		if (r instanceof OWLlinkReasoner || r instanceof OWLlinkHTTPXMLReasoner)
			return false;
		// if(r instanceof OWLlinkReasoner || r instanceof
		// OWLlinkHTTPXMLReasoner)
		// return ((OWLlinkReasoner)
		// r).isPrecomputed(InferenceType.CLASS_HIERARCHY);
//		try {
		  return r.isPrecomputed(infType);
//		}
//		catch(NullPointerException e){
//			FileUtil.writeErrorLogAndConsole("isPrecomputed fired a NullPointerException, returning false");
//			return false;
//		}
	}

	public static long ontologyClassification(boolean alignedOnto,
			boolean printIt, List<OWLReasoner> reasoners,
			REASONER_KIND currentReasoner, boolean tryPellet) {

		boolean tryPelletL = currentReasoner.equals(REASONER_KIND.PELLET) ? false
				: tryPellet;

		int timeout = alignedOnto ? Params.alignOntoClassificationTimeout
				: Params.inputOntoClassificationTimeout;
		if (timeout == 0)
			timeout = Integer.MAX_VALUE;

		ExecutorService executor = Executors.newFixedThreadPool(alignedOnto ? 1
				: 2);

		List<Future<Boolean>> futures = new ArrayList<>(reasoners.size());
		List<OntoClassificationThread> threads = new ArrayList<>(
				reasoners.size());

		long time = Util.getMSec();

		for (OWLReasoner r : reasoners) {
			if (!isClassificationPrecomputed(r)) {
				OntoClassificationThread t = new OntoClassificationThread(r);
				threads.add(t);
				futures.add(executor.submit(t));
			}
		}

		executor.shutdown();

		try {
			for (Future<Boolean> f : futures) {
				if (!f.get(timeout, TimeUnit.SECONDS)) {
					// inconsistent ontology!
					FileUtil.writeLogAndConsole("\nInconsistent ontology, "
							+ "skipping it");
					return -1;
				}
			}
		} catch (InterruptedException | ExecutionException e) {
			FileUtil.writeErrorLogAndConsole(e.getMessage());
			return -1;
		} catch (TimeoutException e) {

			if (currentReasoner.equals(Params.reasonerAfterTimeout)) {
				FileUtil.writeLogAndConsole("Timeout of " + timeout
						+ "(s) reached with " + currentReasoner
						+ ", skipping this test");

				for (OntoClassificationThread t : threads) {
					OWLReasoner r = t.getReasoner();
					r.interrupt();
					OntoUtil.disposeReasoners(r);
				}
				return -1;
			}

			FileUtil.writeLogAndConsole("Timeout of "
					+ timeout
					+ "(s) reached with "
					+ currentReasoner
					+ ", trying with "
					+ (tryPelletL ? REASONER_KIND.PELLET
							: Params.reasonerAfterTimeout));
			// (alignedOnto ? Params.reasonerBasic
			// : Params.reasonerAfterTimeout));

			for (OntoClassificationThread t : threads) {
				OWLReasoner r = t.getReasoner();
				if (!isClassificationPrecomputed(t.getReasoner())) {
					OWLOntology o = r.getRootOntology();
					int index = reasoners.indexOf(r);
					r.interrupt();
					OntoUtil.disposeReasoners(r);

					if (tryPelletL)
						Params.reasonerKind = REASONER_KIND.PELLET;

					r = OntoUtil.getReasoner(o,
							tryPelletL ? REASONER_KIND.PELLET
									: Params.reasonerAfterTimeout,
							getManager(false));
					// (alignedOnto ? Params.reasonerBasic
					// : Params.reasonerAfterTimeout), manager);
					reasoners.remove(index);
					reasoners.add(index, r);
				}
			}

			// hopefully at this point all the reasoners are interrupted and
			// disposed
			for (Future<Boolean> f : futures)
				f.cancel(true);
			executor.shutdownNow();

			long newTime = ontologyClassification(alignedOnto, printIt,
					reasoners, tryPelletL ? REASONER_KIND.PELLET
							: Params.reasonerAfterTimeout,
					!currentReasoner.equals(REASONER_KIND.PELLET));
			// (alignedOnto ? Params.reasonerBasic
			// : Params.reasonerAfterTimeout));

			// for (OWLReasoner owlReasoner : reasoners)
			// if(!isClassificationPrecomputed(owlReasoner))
			// throw new Error(owlReasoner.getRootOntology().
			// getOntologyID().getOntologyIRI() +
			// " REASONER NOT CLASSIFIED");

			return newTime;
		} finally {
			for (OntoClassificationThread t : threads) {
				if (!isClassificationPrecomputed(t.getReasoner())){
					t.getReasoner().interrupt();
				}
			}

			for (Future<Boolean> f : futures)
				if (!f.isCancelled() && !f.isDone())
					f.cancel(true);

			executor.shutdownNow();
			time = Util.getDiffmsec(time);
		}
		if (printIt)
			for (OWLReasoner r : reasoners)
				if (isClassificationPrecomputed(r))
					OntoUtil.printClassification(r.getTopClassNode(), r, 0);

		// for (OWLReasoner owlReasoner : reasoners)
		// if(!isClassificationPrecomputed(owlReasoner))
		// throw new Error(owlReasoner.getRootOntology().getOntologyID().
		// getOntologyIRI() + " REASONER NOT CLASSIFIED");

		return time;
	}

	public static OWLOntology moduleExtractor(OWLOntology onto,
			Set<OWLEntity> seedSig) {
		OntologyModuleExtractor moduleExt =
		// new OntologyModuleExtractor(onto);
		new OntologyModuleExtractor(OWLManager.createOWLOntologyManager(),
				onto, TYPEMODULE.BOTTOM_LOCALITY);
		OWLOntology module;
		try {
			module = moduleExt.extractAsOntology(seedSig,
					IRI.create("http://module.owl"));
		} catch (OWLOntologyCreationException e) {
			FileUtil.writeErrorLogAndConsole("Error while creating module for "
					+ "signature " + seedSig + ", ontology " + onto);
			return null;
		} finally {
			moduleExt.clearStrutures();
		}

		return module;
	}

	public static OWLOntology extractModule(OWLOntologyManager manager,
			OWLReasoner r, Set<OWLEntity> sig, String IRISuffix, ModuleType type)
			throws OWLOntologyCreationException, OWLOntologyStorageException {

		OWLOntology onto = r.getRootOntology();

		// We now add all subclasses (direct and indirect) of the chosen
		// classes. Ideally, it should be done using a DL reasoner, in order to
		// take inferred subclass relations into account. We are using the
		// structural reasoner of the OWL API for simplicity.
		Set<OWLEntity> seedSig = new HashSet<OWLEntity>(sig);
		for (OWLEntity ent : sig) {
			if (OWLClass.class.isAssignableFrom(ent.getClass())) {
				NodeSet<OWLClass> subClasses = r.getSubClasses((OWLClass) ent,
						false);
				seedSig.addAll(subClasses.getFlattened());
			}
		}
		// Output for debugging purposes
		FileUtil.writeLogAndConsole("Extracting the module for this seed signature:");
		for (OWLEntity ent : seedSig) {
			FileUtil.writeLogAndConsole("  " + ent);
		}
		FileUtil.writeLogAndConsole("\nSome statistics of the original ontology:");
		FileUtil.writeLogAndConsole("  " + onto.getSignature(true).size()
				+ " entities");
		FileUtil.writeLogAndConsole("  " + onto.getLogicalAxiomCount()
				+ " logical axioms");
		FileUtil.writeLogAndConsole("  "
				+ (onto.getAxiomCount() - onto.getLogicalAxiomCount())
				+ " other axioms\n");
		// We now extract a locality-based module. For most reuse purposes, the
		// module type should be STAR -- this yields the smallest possible
		// locality-based module. These modules guarantee that all entailments
		// of the original ontology that can be formulated using only terms from
		// the seed signature or the module will also be entailments of the
		// module. In easier words, the module preserves all knowledge of the
		// ontology about the terms in the seed signature or the module.
		SyntacticLocalityModuleExtractor sme = new SyntacticLocalityModuleExtractor(
				manager, onto, type);
		IRI moduleIRI = IRI.create(manager.getOntologyDocumentIRI(onto)
				.toString() + IRISuffix);
		OWLOntology mod = sme.extractAsOntology(seedSig, moduleIRI);
		// Output for debugging purposes
		FileUtil.writeLogAndConsole("Some statistics of the module:");
		FileUtil.writeLogAndConsole("  " + mod.getSignature(true).size()
				+ " entities");
		FileUtil.writeLogAndConsole("  " + mod.getLogicalAxiomCount()
				+ " logical axioms");
		FileUtil.writeLogAndConsole("  "
				+ (mod.getAxiomCount() - mod.getLogicalAxiomCount())
				+ " other axioms\n");

		// // And we save the module.
		// System.out.println("Saving the module as "
		// + mod.getOntologyID().getOntologyIRI());
		// manager.saveOntology(mod);
		return mod;
	}

	static public void chooseReasoner(String mappingFile, String trackName) {
		if (mappingFile.endsWith("ASE-ekaw-iasted.rdf")
				|| mappingFile.endsWith("MaasMatch-ekaw-iasted.rdf"))
			Params.reasonerKind = REASONER_KIND.HERMIT;
		else if (mappingFile.endsWith("MaasMatch-conference-iasted.rdf"))
			Params.reasonerKind = REASONER_KIND.PELLET;
		// else if(mappingFile.contains("iasted"))
		// Params.reasonerKind = ENUM_REASONER.PELLET;
		else if (trackName.equals("anatomy")
				|| mappingFile.endsWith("MapSSS-edas-ekaw.rdf"))
			Params.reasonerKind = REASONER_KIND.HERMIT;
		else if (trackName.equals("largebio")) {
			// if(mappingFile.contains("SNOMED2NCI"))
			// Params.reasonerKind = ENUM_REASONER.ELK;
			// else
			Params.reasonerKind = REASONER_KIND.HERMIT;
		} else if (trackName.equals("conference"))
			Params.reasonerKind = REASONER_KIND.HERMIT;
		else if (trackName.equals("library"))
			Params.reasonerKind = REASONER_KIND.HERMIT;
		else
			Params.reasonerKind = REASONER_KIND.HERMIT;
	}

	static public void removeDatatypes(OWLOntology onto,
			OWLOntologyManager manager) {
		Set<OWLAxiom> toRemove = new HashSet<>();
		for (OWLDatatype dt : onto.getDatatypesInSignature(true))
			toRemove.addAll(onto.getDatatypeDefinitions(dt));

		removeAxiomsFromOntology(onto, manager, toRemove, true);
	}

	static public OWLReasoner getReasoner(OWLOntology onto,
			REASONER_KIND reasonerKind, OWLOntologyManager manager) {

		OWLReasoner r = null;
		// OWLReasonerConfiguration rc = new SimpleConfiguration(
		// Params.inputOntoClassificationTimeout / 60 * 1000);

		try {
			FileUtil.writeLogAndConsoleNONL(getDLName(onto));

			if (owlLinkReasonersActive.containsKey(reasonerKind)
					&& !owlLinkReasonersActive.get(reasonerKind)) {
				if (launchOWLLinkServer(reasonerKind))
					owlLinkReasonersActive.put(reasonerKind, true);
				else
					return r;
			}

			switch (reasonerKind) {
			case PELLET:
				reasonerFactory = getReasonerFactory(reasonerKind);
				// OWLReasonerConfiguration conf = new SimpleConfiguration();
				if (Params.incrementalReasoning) {
					PelletOptions.USE_COMPLETION_QUEUE = true;
					PelletOptions.USE_INCREMENTAL_CONSISTENCY = true;
					PelletOptions.USE_SMART_RESTORE = false;
					// PelletOptions.USE_TRACING = true;
				}
				// PelletOptions.IGNORE_UNSUPPORTED_AXIOMS = true;
				break;
			case HERMIT:
				// Arrays.toString(
				// ((URLClassLoader)OntoUtil.class.getClassLoader()).getURLs());
				reasonerFactory = getReasonerFactory(reasonerKind);
				if (Params.oaei
						&& onto.getDatatypesInSignature(true).size() > 0) {
					reasonerFactory = new ElkReasonerFactory();
					// reasonerFactory = new
					// org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory();
					// r = reasonerFactory.createReasoner(onto);
					break;
				}
				org.semanticweb.HermiT.Configuration c = new org.semanticweb.HermiT.Configuration();
				c.ignoreUnsupportedDatatypes = true;
				// c.individualTaskTimeout =
				// Params.inputOntoClassificationTimeout / 60 * 1000;
				c.bufferChanges = Params.bufferingReasoner;
				r = new Reasoner(c, onto);
				break;
			case ELKTRACE:
			case ELK:
				reasonerFactory = getReasonerFactory(reasonerKind);
				break;
			case FACT:
				reasonerFactory = getReasonerFactory(reasonerKind);
				// SimpleConfiguration fc = new SimpleConfiguration();
				// BufferingMode.NON_BUFFERING;
				// r = new Reasoner(fc, onto);
				break;
			case KONCLUDE:
				URL url = new URL("http://localhost:8082");// Configure the
															// server end-point
				OWLlinkReasonerConfiguration reasonerConfiguration = new OWLlinkReasonerConfiguration(
						url);
				reasonerFactory = new OWLlinkHTTPXMLReasonerFactory();
				r = reasonerFactory.createNonBufferingReasoner(onto,
						reasonerConfiguration);
				break;
			default:
				reasonerFactory = new org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory();
				break;
			}
			// if(!manager.contains(onto.getOntologyID().getOntologyIRI()))
			// System.out.println(onto);
			if (r == null) {
				r = Params.bufferingReasoner ? reasonerFactory
						.createNonBufferingReasoner(onto)// ,rc)
						: reasonerFactory.createReasoner(onto);// ,rc);
			}
			// else
			// System.out.println(r);
			FileUtil.writeLogAndConsole(", Reasoner: "
					+ getReasonerInfoString(r));
			if (reasonerKind.equals(REASONER_KIND.PELLET)
					&& Params.incrementalReasoning)
				manager.addOntologyChangeListener((OWLOntologyChangeListener) r);

			reasoners.add(r);
			return r;
		} catch (UnknownOWLOntologyException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		reasoners.add(r);
		return r;
	}

	public static String getReasonerInfoString(OWLReasoner r) {

		Version v;
		String name;

		if (Params.oaei) {
			v = new Version(0, 0, 0, 0);
			name = r.getClass().getName();
		} else {
			v = r.getReasonerVersion();
			name = r.getReasonerName();
		}

		return (name != null ? name : "ELK") + " " + v.getMajor() + "."
				+ v.getMinor() + "." + v.getPatch() + " (build " + v.getBuild()
				+ ")";
	}

	public static String extractPrefix(OWLOntology s) {
		// if(s==null)
		// System.out.println();
		return s.toString().indexOf("<") >= 0 ? s.toString().substring(
				s.toString().indexOf("<") + 1, s.toString().indexOf(">")) : 
					s.getOntologyID().toString();
	}

	static public OWLOntology load(String iriString, boolean local,
			OWLOntologyManager manager) throws OWLOntologyCreationException {
		OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration();
		config = config.setLoadAnnotationAxioms(false);
		IRI iri = null;
		if (!local)
			iri = IRI.create(iriString);
		else
			iri = IRI.create(new File(iriString));

		OWLOntologyDocumentSource source = new IRIDocumentSource(iri);

		return manager.loadOntologyFromOntologyDocument(source, config);
	}

	public static String getCurrTime() {
		return dateFormat.format(cal.getTime());
	}

	static public void save(OWLOntology onto, String destFile,
			OWLOntologyManager manager) throws OWLOntologyStorageException,
			OWLOntologyCreationException, IOException {

		// File file = File.createTempFile("owlapiexamples", "saving");
		File file = new File(destFile);
		// manager.saveOntology(onto, IRI.create(file.toURI()));
		// By default ontologies are saved in the format from which they were
		// loaded. In this case the ontology was loaded from an rdf/xml file We
		// can get information about the format of an ontology from its manager

		OWLOntologyFormat format = manager.getOntologyFormat(onto);
		// We can save the ontology in a different format Lets save the ontology
		// in owl/xml format
		OWLXMLOntologyFormat owlxmlFormat = new OWLXMLOntologyFormat();
		// Some ontology formats support prefix names and prefix IRIs. In our
		// case we loaded the pizza ontology from an rdf/xml format, which
		// supports prefixes. When we save the ontology in the new format we
		// will copy the prefixes over so that we have nicely abbreviated IRIs
		// in the new ontology document
		if (format.isPrefixOWLOntologyFormat()) {
			owlxmlFormat.copyPrefixesFrom(format.asPrefixOWLOntologyFormat());
		}
		manager.saveOntology(onto, owlxmlFormat, IRI.create(file.toURI()));
	}

	public void shouldWalkOntology(OWLOntology onto, OWLOntologyManager manager)
			throws OWLOntologyCreationException {
		// This example shows how to use an ontology walker to walk the asserted
		// structure of an ontology. Suppose we want to find the axioms that use
		// a some values from (existential restriction) we can use the walker to
		// do this. We'll use the pizza ontology as an example. Load the
		// ontology from the web:
		IRI ontoIRI = manager.getOntologyDocumentIRI(onto);

		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLOntology ont = man.loadOntologyFromOntologyDocument(ontoIRI);
		// Create the walker. Pass in the pizza ontology - we need to put it
		// into a set though, so we just create a singleton set in this case.
		OWLOntologyWalker walker = new OWLOntologyWalker(
				Collections.singleton(ont));
		// Now ask our walker to walk over the ontology. We specify a visitor
		// who gets visited by the various objects as the walker encounters
		// them. We need to create out visitor. This can be any ordinary
		// visitor, but we will extend the OWLOntologyWalkerVisitor because it
		// provides a convenience method to get the current axiom being visited
		// as we go. Create an instance and override the
		// visit(OWLObjectSomeValuesFrom) method, because we are interested in
		// some values from restrictions.
		OWLOntologyWalkerVisitor<Object> visitor = new OWLOntologyWalkerVisitor<Object>(
				walker) {
			@Override
			public Object visit(OWLObjectSomeValuesFrom desc) {
				// Print out the restriction
				FileUtil.writeLogAndConsole(desc.toString());
				// Print out the axiom where the restriction is used
				FileUtil.writeLogAndConsole("         " + getCurrentAxiom()
						+ "\n");
				// We don't need to return anything here.
				return null;
			}
		};
		// Now ask the walker to walk over the ontology structure using our
		// visitor instance.
		walker.walkStructure(visitor);
	}

	static public OWLClass createFreshClass(OWLOntology onto,
			OWLDataFactory dataFactory, OWLOntologyManager manager) {
		OWLClass cls = dataFactory.getOWLClass(freshClassPrefix + nextFreshId,
				new DefaultPrefixManager(onto.getOntologyID().getOntologyIRI()
						.toString()));

		++nextFreshId;

		return cls;
	}

	static public URI stringToURI(String str) {
		URI uri = null;
		try {
			return new URI(str);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return uri;
	}

	static public String extractOBOOntologyIRI(OWLOntology o) {
		String iriStr = o.getOntologyID().getOntologyIRI().toString();
		// int oboIndex = iriStr.indexOf("/obo/");
		// int fragIndex = iriStr.indexOf('/', oboIndex);
		// iriStr = iriStr.substring(0,oboIndex) + iriStr.substring(fragIndex);
		// System.out.println(iriStr);
		// return iriStr;
		return iriStr.substring(0, iriStr.lastIndexOf('/') + 1);
	}

	static public void unloadAllOntologies(OWLOntologyManager manager) {
		Iterator<OWLOntology> itr = manager.getOntologies().iterator();
		OWLOntology o = null;

		while (itr.hasNext()) {
			o = itr.next();
			manager.removeOntology(o);
		}
		// if(o != null)
		// Util.getUsedMemoryAndClean(1024,250);
	}

	public static Set<MappingObjectStr> convertAxiomsToAlignment(
			Set<OWLAxiom> axioms) {
		Set<MappingObjectStr> align = new HashSet<>();

		Map<OWLEntity, OWLAxiom> entity2Axiom = new HashMap<>();
		Set<AxiomType<?>> types = new HashSet<>();
		types.add(AxiomType.EQUIVALENT_CLASSES);
		types.add(AxiomType.SUBCLASS_OF);
		types.add(AxiomType.EQUIVALENT_DATA_PROPERTIES);
		types.add(AxiomType.SUB_DATA_PROPERTY);
		types.add(AxiomType.EQUIVALENT_OBJECT_PROPERTIES);
		types.add(AxiomType.SUB_OBJECT_PROPERTY);

		Set<OWLAxiom> axs = new HashSet<>();

		for (OWLAxiom ax : axioms) {
			if (types.contains(ax.getAxiomType())) {
				AxiomType<?> type = ax.getAxiomType();

				if (type.equals(AxiomType.SUBCLASS_OF)) {
					OWLSubClassOfAxiom scAx = (OWLSubClassOfAxiom) ax;

					if (!((OWLSubClassOfAxiom) ax).getSubClass().isAnonymous()) {
						entity2Axiom.put(((OWLSubClassOfAxiom) ax)
								.getSubClass().asOWLClass(), ax);
					} else {
						FileUtil.writeErrorLogAndConsole("Cannot convert to mappings axioms involving "
								+ "other than named classes");
						continue;
					}
				} else if (type.equals(AxiomType.SUB_OBJECT_PROPERTY)) {
					OWLSubObjectPropertyOfAxiom sopAx = (OWLSubObjectPropertyOfAxiom) ax;

					if (!sopAx.getSubProperty().isAnonymous()) {
						entity2Axiom.put(sopAx.getSubProperty()
								.asOWLObjectProperty(), ax);
					} else {
						FileUtil.writeErrorLogAndConsole("Cannot convert to mappings axioms involving "
								+ "other than named object properties");
						continue;
					}
				} else if (type.equals(AxiomType.SUB_DATA_PROPERTY)) {
					OWLSubDataPropertyOfAxiom sdpAx = (OWLSubDataPropertyOfAxiom) ax;

					if (!sdpAx.getSubProperty().isAnonymous()) {
						entity2Axiom.put(sdpAx.getSubProperty()
								.asOWLDataProperty(), ax);
					} else {
						FileUtil.writeErrorLogAndConsole("Cannot convert to mappings axioms involving "
								+ "other than named object properties");
						continue;
					}
				}
				axs.add(ax);
			} else {
				FileUtil.writeErrorLogAndConsole("Axiom type "
						+ ax.getAxiomType() + " is not supported, ignoring it");
				continue;
			}

			for (OWLEntity e : ax.getSignature()) {

				AxiomType<?> type = ax.getAxiomType();

				if (type.equals(AxiomType.EQUIVALENT_CLASSES)) {
					if (!e.isOWLClass()) {
						FileUtil.writeErrorLogAndConsole("Cannot convert to mappings "
								+ "axioms involving other than named classes");
						axs.remove(ax);
						break;
					} else {
						if (ax.getAxiomType().equals(
								AxiomType.EQUIVALENT_CLASSES))
							entity2Axiom.put(e.asOWLClass(), ax);
						// already added, nothing to do
					}
				} else if (type.equals(AxiomType.EQUIVALENT_OBJECT_PROPERTIES)) {
					if (!e.isOWLObjectProperty()) {
						FileUtil.writeErrorLogAndConsole("Cannot convert to mappings axioms involving "
								+ "other than named object properties");
						axs.remove(ax);
						break;
					} else {
						if (ax.getAxiomType().equals(
								AxiomType.EQUIVALENT_OBJECT_PROPERTIES))
							entity2Axiom.put(e.asOWLObjectProperty(), ax);
					}
				} else if (type.equals(AxiomType.EQUIVALENT_DATA_PROPERTIES)) {
					if (!e.isOWLDataProperty()) {
						FileUtil.writeErrorLogAndConsole("Cannot convert to mappings axioms involving "
								+ "other than named data properties");
						axs.remove(ax);
						break;
					} else {
						if (ax.getAxiomType().equals(
								AxiomType.EQUIVALENT_DATA_PROPERTIES))
							entity2Axiom.put(e.asOWLDataProperty(), ax);
					}
				}
			}
		}

		for (OWLAxiom ax : axs) {

			OWLEntity[] entities = ax.getSignature().toArray(new OWLEntity[0]);

			if (entities.length != 2) {
				FileUtil.writeErrorLogAndConsole("Method supports exactly 2 classes, not "
						+ entities.length + ", skipping axiom " + ax);
				continue;
			}

			entities = new OWLEntity[2];

			if (ax.getAxiomType().equals(AxiomType.EQUIVALENT_CLASSES)) {

				align.add(new MappingObjectStr(entities[0].getIRI().toString(),
						entities[1].getIRI().toString(), 1, Utilities.EQ,
						Utilities.CLASSES));

				// align.add(new MappingObjectStr(
				// classes[1].getIRI().toString(),
				// classes[0].getIRI().toString(),
				// 1, Utilities.EQ, Utilities.CLASSES));
			} else if (ax.getAxiomType().equals(
					AxiomType.EQUIVALENT_OBJECT_PROPERTIES)) {

				align.add(new MappingObjectStr(entities[0].getIRI().toString(),
						entities[1].getIRI().toString(), 1, Utilities.EQ,
						Utilities.OBJECTPROPERTIES));
			} else if (ax.getAxiomType().equals(
					AxiomType.EQUIVALENT_DATA_PROPERTIES)) {

				align.add(new MappingObjectStr(entities[0].getIRI().toString(),
						entities[1].getIRI().toString(), 1, Utilities.EQ,
						Utilities.DATAPROPERTIES));
			} else if (ax.getAxiomType().equals(AxiomType.SUBCLASS_OF)) {
				if (entity2Axiom.containsKey(entities[1])
						&& entity2Axiom.get(entities[1]).getAxiomType()
								.equals(AxiomType.SUBCLASS_OF)) {
					align.add(new MappingObjectStr(entities[0].getIRI()
							.toString(), entities[1].getIRI().toString(), 1,
							Utilities.EQ, Utilities.CLASSES));

					// align.add(new MappingObjectStr(
					// classes[1].getIRI().toString(),
					// classes[0].getIRI().toString(),
					// 1, Utilities.EQ, Utilities.CLASSES));
				} else {
					entities[0] = ((OWLSubClassOfAxiom) ax).getSubClass()
							.asOWLClass();
					entities[1] = ((OWLSubClassOfAxiom) ax).getSuperClass()
							.asOWLClass();

					align.add(new MappingObjectStr(entities[0].getIRI()
							.toString(), entities[1].getIRI().toString(), 1,
							Utilities.L2R, Utilities.CLASSES));
				}
			} else if (ax.getAxiomType().equals(AxiomType.SUB_OBJECT_PROPERTY)) {
				if (entity2Axiom.containsKey(entities[1])
						&& entity2Axiom.get(entities[1]).getAxiomType()
								.equals(AxiomType.SUB_OBJECT_PROPERTY)) {
					align.add(new MappingObjectStr(entities[0].getIRI()
							.toString(), entities[1].getIRI().toString(), 1,
							Utilities.EQ, Utilities.OBJECTPROPERTIES));
				} else {
					entities[0] = ((OWLSubObjectPropertyOfAxiom) ax)
							.getSubProperty().asOWLObjectProperty();
					entities[1] = ((OWLSubObjectPropertyOfAxiom) ax)
							.getSuperProperty().asOWLObjectProperty();

					align.add(new MappingObjectStr(entities[0].getIRI()
							.toString(), entities[1].getIRI().toString(), 1,
							Utilities.L2R, Utilities.OBJECTPROPERTIES));
				}
			} else if (ax.getAxiomType().equals(AxiomType.SUB_DATA_PROPERTY)) {
				if (entity2Axiom.containsKey(entities[1])
						&& entity2Axiom.get(entities[1]).getAxiomType()
								.equals(AxiomType.SUB_DATA_PROPERTY)) {
					align.add(new MappingObjectStr(entities[0].getIRI()
							.toString(), entities[1].getIRI().toString(), 1,
							Utilities.EQ, Utilities.DATAPROPERTIES));

					// align.add(new MappingObjectStr(
					// classes[1].getIRI().toString(),
					// classes[0].getIRI().toString(),
					// 1, Utilities.EQ, Utilities.CLASSES));
				} else {
					entities[0] = ((OWLSubDataPropertyOfAxiom) ax)
							.getSubProperty().asOWLDataProperty();
					entities[1] = ((OWLSubDataPropertyOfAxiom) ax)
							.getSuperProperty().asOWLDataProperty();

					align.add(new MappingObjectStr(entities[0].getIRI()
							.toString(), entities[1].getIRI().toString(), 1,
							Utilities.L2R, Utilities.DATAPROPERTIES));
				}
			}
		}

		return align;
	}

	public static Set<OWLAxiom> convertMappingToAxiom(Set<OWLEntity> sig1,
			Set<OWLEntity> sig2, MappingObjectStr mapping) {

		Set<OWLAxiom> axioms = new HashSet<>();
		switch (mapping.getTypeOfMapping()) {
		case Utilities.CLASSES:
			OWLClass c1 = dataFactory.getOWLClass(IRI.create(mapping
					.getIRIStrEnt1())),
			c2 = dataFactory.getOWLClass(IRI.create(mapping.getIRIStrEnt2()));
			switch (mapping.getMappingDirection()) {
			case Utilities.EQ:
				if ((!sig1.contains(c1) && !sig1.contains(c2))
						|| (!sig1.contains(c2) && !sig2.contains(c2)))
					break;
				axioms.add(dataFactory.getOWLSubClassOfAxiom(c1, c2));
				axioms.add(dataFactory.getOWLSubClassOfAxiom(c2, c1));
				break;
			case Utilities.L2R:
				if ((!sig1.contains(c1) || !sig2.contains(c2)))
					break;
				axioms.add(dataFactory.getOWLSubClassOfAxiom(c1, c2));
				break;
			case Utilities.R2L:
				if ((!sig1.contains(c1) || !sig2.contains(c2)))
					break;
				axioms.add(dataFactory.getOWLSubClassOfAxiom(c2, c1));
				break;
			default:
				break;
			}
			break;

		case Utilities.DATAPROPERTIES:
			OWLDataProperty dp1 = dataFactory.getOWLDataProperty(IRI
					.create(mapping.getIRIStrEnt1())),
			dp2 = dataFactory.getOWLDataProperty(IRI.create(mapping
					.getIRIStrEnt2()));
			switch (mapping.getMappingDirection()) {
			case Utilities.EQ:
				if ((!sig1.contains(dp1) && !sig1.contains(dp2))
						|| (!sig1.contains(dp2) && !sig2.contains(dp2)))
					break;
				axioms.add(dataFactory.getOWLSubDataPropertyOfAxiom(dp1, dp2));
				axioms.add(dataFactory.getOWLSubDataPropertyOfAxiom(dp2, dp1));
				break;
			case Utilities.L2R:
				if ((!sig1.contains(dp1) || !sig2.contains(dp2)))
					break;
				axioms.add(dataFactory.getOWLSubDataPropertyOfAxiom(dp1, dp2));
				break;
			case Utilities.R2L:
				if ((!sig2.contains(dp1) || !sig1.contains(dp2)))
					break;
				axioms.add(dataFactory.getOWLSubDataPropertyOfAxiom(dp2, dp1));
				break;
			default:
				break;
			}
			break;

		case Utilities.OBJECTPROPERTIES:
			OWLObjectProperty op1 = dataFactory.getOWLObjectProperty(IRI
					.create(mapping.getIRIStrEnt1())),
			op2 = dataFactory.getOWLObjectProperty(IRI.create(mapping
					.getIRIStrEnt2()));
			switch (mapping.getMappingDirection()) {

			case Utilities.EQ:
				if ((!sig1.contains(op1) && !sig1.contains(op2))
						|| (!sig1.contains(op2) && !sig2.contains(op2)))
					break;
				axioms.add(dataFactory.getOWLSubObjectPropertyOfAxiom(op1, op2));
				axioms.add(dataFactory.getOWLSubObjectPropertyOfAxiom(op2, op1));
				break;
			case Utilities.L2R:
				if ((!sig1.contains(op1) || !sig2.contains(op2)))
					break;
				axioms.add(dataFactory.getOWLSubObjectPropertyOfAxiom(op1, op2));
				break;
			case Utilities.R2L:
				if ((!sig2.contains(op1) || !sig1.contains(op2)))
					break;
				axioms.add(dataFactory.getOWLSubObjectPropertyOfAxiom(op2, op1));
				break;
			default:
				break;
			}
		case Utilities.INSTANCES:
			OWLNamedIndividual i1 = dataFactory.getOWLNamedIndividual(IRI
					.create(mapping.getIRIStrEnt1())),
			i2 = dataFactory.getOWLNamedIndividual(IRI.create(mapping
					.getIRIStrEnt2()));

			if (!sig1.contains(i1) || !sig2.contains(i2))
				break;

			axioms.add(dataFactory.getOWLSameIndividualAxiom(i1, i2));
			break;
		case Utilities.UNKNOWN:
			FileUtil.writeErrorLogAndConsole("Unknown mapping kind: " + mapping);
			break;
		default:
			break;
		}
		return axioms;
	}

	public static Pair<List<Pair<OWLClass>>> graphDetectionDirectViolations(
			OWLOntology fstOnto, OWLOntology sndOnto, OWLOntology alignOnto,
			Pair<List<Pair<OWLClass>>> viols) {

		long start = Util.getMSec();
		LightAdjacencyList adj = new LightAdjacencyList(fstOnto, sndOnto,
				alignOnto);

		NodeReachability r = new DFSReachability(adj, false);

		List<Pair<OWLClass>> dirViol1 = new ArrayList<>();
		List<Pair<OWLClass>> dirViol2 = new ArrayList<>();

		List<Pair<OWLClass>> viol1 = viols.getFirst();
		List<Pair<OWLClass>> viol2 = viols.getSecond();

		for (Pair<OWLClass> v : viol1)
			if (r.areReachable(v.getFirst(), v.getSecond()))
				dirViol1.add(v);

		for (Pair<OWLClass> v : viol2)
			if (r.areReachable(v.getFirst(), v.getSecond()))
				dirViol2.add(v);

		FileUtil.writeLogAndConsole("Graph direct violation detection: "
				+ Util.getDiffmsec(start) + " (ms)");

		return new Pair<>(dirViol1, dirViol2);
	}

	public static List<Pair<OWLClass>> explanationDetectionDirectViolations(
			OWLOntology inputOnto, OWLOntology alignOnto,
			List<Pair<Integer>> violations, JointIndexManager index,
			int ontoId, OWLReasoner alignR, boolean suppressOutput) {

		long start = Util.getMSec();

		List<Pair<OWLClass>> owlViols = new ArrayList<>(violations.size());
		for (Pair<Integer> p : violations)
			owlViols.add(LogMapWrapper.getOWLClassFromIndexPair(p, index));

		List<Pair<OWLClass>> res = explanationDetectionDirectViolations(
				inputOnto, alignOnto, owlViols, ontoId, alignR, suppressOutput);

		FileUtil.writeLogAndConsole("Explanation direct violation detection: "
				+ Util.getDiffmsec(start) + " (ms)");

		return res;
	}

	public static boolean compareDirectViolations(List<Pair<OWLClass>> dirR,
			List<Pair<Integer>> dirI, JointIndexManager index) {
		if (dirR.size() != dirI.size())
			return compareDirectViolations(dirR, "explanations",
					LogMapWrapper.getOWLClassFromIndexPair(dirI, index),
					"index");

		return false;
	}

	public static boolean compareDirectViolations(List<Pair<OWLClass>> dirR,
			String dirRLabel, List<Pair<OWLClass>> dirIC, String dirICLabel) {

		// if(dirR.size() != dirIC.size()){

		Set<Pair<OWLClass>> intersection = Util
				.computeIntersection(dirR, dirIC);

		dirR.removeAll(intersection);
		dirIC.removeAll(intersection);

		FileUtil.writeLogAndConsole("Detected by " + dirICLabel
				+ " but not by " + dirRLabel + ": " + dirIC.size() + "\n"
				+ dirIC.toString().replace(">>, ", ">>,\n"));

		FileUtil.writeLogAndConsole("Detected by " + dirRLabel + " but not by "
				+ dirICLabel + ": " + dirR.size() + "\n"
				+ dirR.toString().replace(">>, ", ">>,\n"));
		return true;
		// }
		// return false;
	}

	public static List<Pair<OWLClass>> explanationDetectionDirectViolations(
			OWLOntology inputOnto, OWLOntology alignOnto,
			List<Pair<OWLClass>> violations, int ontoId, OWLReasoner alignR,
			boolean suppressOutput) {

		if (violations.isEmpty())
			return Collections.emptyList();

		List<Pair<OWLClass>> dirViols = new ArrayList<>();

		// Set<OWLClass> classSig = inputOnto.getClassesInSignature();
		// classSig.remove(OntoUtil.getDataFactory().getOWLThing());
		// classSig.remove(OntoUtil.getDataFactory().getOWLNothing());

		FileUtil.writeLogAndConsole("Direct violations for onto " + ontoId
				+ ":");
		int count = 0, processed = 0;
		for (Pair<OWLClass> pair : violations) {
			if (!suppressOutput)
				FileUtil.writeLogAndConsole("\tProcessing violation "
						+ ++processed + ":");
			// if(OntoUtil.isDirectViolation(pair,classSig,false))
			// ++count;
			if (OntoUtil.checkDirectViolation(inputOnto, alignOnto, pair,
					false, alignR, suppressOutput)) {
				dirViols.add(pair);
				++count;
			} else {
				if (!suppressOutput)
					FileUtil.writeLogAndConsole("\tNOT DIRECT: " + pair + "\n");
			}
		}

		FileUtil.writeLogAndConsole("Direct violations: " + count + "/"
				+ violations.size());

		return dirViols;
	}

	// a violation "A isA B" is direct iff there is at least a path in one of
	// its explanations not traversing any element of the same input ontology
	// of A and B
	public static boolean isDirectViolation(OWLOntology o, Pair<OWLClass> v,
			Set<OWLClass> classSig, boolean useELK) {

		Set<Explanation<OWLAxiom>> expls = computeSubsumptionExplanation(o,
				getManager(false), getSubClassOfAxiom(v), useELK);
		FileUtil.writeLogAndConsole("\tFound " + expls.size() + " explanations");
		int count = 0;
		ext: for (Explanation<OWLAxiom> expl : expls) {
			FileUtil.writeLogAndConsole("\t\tProcessing explanation " + ++count);

			OWLClass nextSrc = v.getFirst(), nextDst = v.getSecond();

			if (expl.isJustificationEntailment()) {
				FileUtil.writeLogAndConsole("\t" + expl.toString());
				return true;
			}

			Set<OWLAxiom> axioms = new HashSet<>(expl.getAxioms());
			while (true) {
				OWLAxiom ax;
				Iterator<OWLAxiom> itr = axioms.iterator();

				while (itr.hasNext()) {
					ax = itr.next();
					if (!ax.getAxiomType().equals(AxiomType.SUBCLASS_OF))
						continue ext;

					OWLSubClassOfAxiom subAx = (OWLSubClassOfAxiom) ax;
					OWLClassExpression subCE = subAx.getSubClass(), supCE = subAx
							.getSuperClass();

					if (subCE.isAnonymous()
							|| !subCE.isClassExpressionLiteral()
							|| subCE.isBottomEntity() || subCE.isTopEntity())
						continue ext;
					if (supCE.isAnonymous()
							|| !supCE.isClassExpressionLiteral()
							|| supCE.isBottomEntity() || supCE.isTopEntity())
						continue ext;

					OWLClass subC = subCE.asOWLClass(), supC = supCE
							.asOWLClass();

					if (!v.getFirst().equals(subC) && classSig.contains(subC)
							|| !v.getSecond().equals(supC)
							&& classSig.contains(supC))
						continue ext;

					if (subC.equals(nextSrc)) {
						if (supC.equals(nextDst)) {
							FileUtil.writeLogAndConsole("\t" + expl.toString());
							return true;
						}
						nextSrc = supC;
						itr.remove();
					}

					if (supC.equals(nextDst)) {
						nextDst = subC;
						itr.remove();
					}
				}
			}
		}

		return false;
	}

	public static OWLSubClassOfAxiom getSubClassOfAxiom(Pair<OWLClass> p) {
		return getSubClassOfAxiom(p.getFirst(), p.getSecond());
	}

	public static OWLSubClassOfAxiom getSubClassOfAxiom(OWLClass a, OWLClass b) {
		return dataFactory.getOWLSubClassOfAxiom(a, b);
	}

	public static OWLOntology getAlignedOntology(OWLOntologyManager manager,
			Set<OWLAxiom> alignment, OWLOntology... ontos) {
		OWLOntology o = null;
		try {
			o = manager.createOntology(alignment);
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
		for (OWLOntology onto : ontos)
			alterOntologyWithAxioms(o, onto.getAxioms(), manager, false, true);

		return o;
	}

	public static boolean saveClassificationAxioms(OWLOntology o,
			OWLReasoner r, OWLOntologyManager manager) {
		// try {
		List<InferredAxiomGenerator<? extends OWLAxiom>> gens = new ArrayList<InferredAxiomGenerator<? extends OWLAxiom>>();
		gens.add(new InferredSubClassAxiomGenerator());
		gens.add(new InferredEquivalentClassAxiomGenerator());

		int preAxioms = o.getLogicalAxiomCount();

		Set<OWLAxiom> axioms = new HashSet<>();

		for (InferredAxiomGenerator<? extends OWLAxiom> gen : gens)
			axioms.addAll(gen.createAxioms(manager, r));

		// classified ontology closed by inference would be too big, avoid
		// materialisation
		if (preAxioms > Params.minAxiomsInferenceBlock
				&& preAxioms * Params.maxInferredAxiomsTimes < axioms.size()) {
			FileUtil.writeLogAndConsole("Skip aligned ontology inference materialisation (> "
					+ axioms.size()
					/ preAxioms
					+ "x the original ontology size)");
			return true;
		}

		OntoUtil.addAxiomsToOntology(o, manager, axioms, true);

		// InferredOntologyGenerator iog =
		// new InferredOntologyGenerator(r,gens);
		//
		// iog.fillOntology(manager, o);

		FileUtil.writeLogAndConsole("Logical axioms (pre->post): " + preAxioms
				+ "->" + o.getLogicalAxiomCount());

		// }
		// catch(org.semanticweb.owlapi.reasoner.InconsistentOntologyException
		// e){
		// FileUtil.writeErrorLogAndConsole("Inconsistent ontology cannot be saved!");
		// return false;
		// }
		return true;
	}

	public static OWLAxiom createDisjointAxiom(OWLDataFactory dataFactory,
			OWLClass c1, OWLClass c2) {
		OWLAxiom disjAx = null;
		if (!c1.isAnonymous() && c1.isClassExpressionLiteral()
				&& !c2.isAnonymous() && c2.isClassExpressionLiteral()) {
			disjAx = dataFactory.getOWLDisjointClassesAxiom(c1, c2);
			if (disjAx.getClassesInSignature().size() != 2) {
				FileUtil.writeErrorLogAndConsole(disjAx.toString());
				return null;
			}
		}
		return disjAx;
	}

	public static Set<OWLAxiom> createDisjAxioms(Set<OWLClass> classes,
			OWLDataFactory dataFac) {
		Set<OWLAxiom> disj = new HashSet<>();
		for (OWLClass c1 : classes) {
			for (OWLClass c2 : classes) {
				if (!c1.equals(c2)) {
					disj.add(dataFac.getOWLDisjointClassesAxiom(c1, c2));
				}
			}
		}
		return disj;
	}

	public static void checkActiveReasoners(boolean clean) {
		if (reasoners.size() > 0) {
			FileUtil.writeLogAndConsole(reasoners.size()
					+ " were not disposed " + "at the end of a test");
			if (clean)
				disposeAllReasoners();
		}
	}

	public static void disposeAllReasoners() {
		for (OWLReasoner r : reasoners) {
			r.dispose();
		}
		reasoners.clear();
	}

	public static String getIRIShortFragment(String iri) {
		return shortFormProvider.getShortForm(IRI.create(iri));
	}

	public static String getIRIShortFragment(IRI iri) {
		return getIRIShortFragment(iri.toString());
	}

	public static void unloadAllOntologies() {
		for (OWLOntologyManager manager : managers)
			unloadAllOntologies(manager);
	}

	public static void unloadOntologies(boolean all, OWLOntology... ontos) {
		if (!all)
			unloadOntologies(ontos);

		try {
			for (OWLOntologyManager manager : managers)
				for (OWLOntology o : ontos)
					if (manager.contains(o.getOntologyID().getOntologyIRI()))
						manager.removeOntology(o);
		} catch (UnknownOWLOntologyException e) {
			e.printStackTrace();
		}
	}

	public static void unloadOntologies(OWLOntologyManager manager,
			OWLOntology... ontos) {
		try {
			for (OWLOntology o : ontos)
				if (manager.contains(o.getOntologyID().getOntologyIRI()))
					manager.removeOntology(o);
		} catch (UnknownOWLOntologyException e) {
			e.printStackTrace();
		}
	}

	public static void unloadOntologies(OWLOntology... ontos) {
		try {
			for (OWLOntology o : ontos)
				if (getManager(false).contains(
						o.getOntologyID().getOntologyIRI()))
					getManager(false).removeOntology(o);
		} catch (UnknownOWLOntologyException e) {
			e.printStackTrace();
		}
	}

	public static Pair<OWLClass> getNamedClassesFromSubClassAxiom(
			OWLSubClassOfAxiom subAx, boolean allowsTopBot) {
		Pair<OWLClass> p = null;
		OWLClass c1, c2;
		OWLClassExpression ce1, ce2;

		ce1 = subAx.getSubClass();
		ce2 = subAx.getSuperClass();

		if (!(ce1.isAnonymous() || ce2.isAnonymous())) {
			c1 = ce1.asOWLClass();
			c2 = ce2.asOWLClass();
			if (allowsTopBot
					|| !(c1.isBottomEntity() || c1.isTopEntity()
							|| c2.isBottomEntity() || c2.isTopEntity()))
				p = new Pair<>(c1, c2);
		}

		return p;
	}

	public static String getGraphIRIObjectProperty(OWLObjectProperty role) {
		return "EXISTS" + LightNode.iriProvider.getShortForm(role.getIRI())
				+ "DotThing";
	}

	public static String getGraphIRIObjectProperty(OWLObjectProperty role,
			String prefix) {
		return prefix + "EXISTS"
				+ LightNode.iriProvider.getShortForm(role.getIRI())
				+ "DotThing";
	}

	public static String getGraphIRIObjectProperty(String nodeIRI, String prefix) {
		return prefix + "EXISTS"
				+ LightNode.iriProvider.getShortForm(IRI.create(nodeIRI))
				+ "DotThing";
	}

	public static OWLClass getNamedClassesFromSubClassAxiom(
			OWLClassExpression ce, boolean allowsTopBot) {
		OWLClass c;

		if (!ce.isAnonymous()) {
			c = ce.asOWLClass();

			if (allowsTopBot || !(c.isBottomEntity() || c.isTopEntity()))
				return c;
		}
		return null;
	}

	public static OWLProfileReport checkELProfile(OWLOntology onto) {
		OWL2ELProfile profile = new OWL2ELProfile();
		return profile.checkOntology(onto);
	}

	public static boolean isExtendedReasoner(OWLReasoner r) {
		return r.getReasonerName().toLowerCase().contains("extended");
	}

	public static OWLClass getOWLClassFromIRI(String iriStr) {
		return dataFactory.getOWLClass(IRI.create(iriStr));
	}

	public static Pair<OWLClass> getOWLClassesFromIRIs(String iriStr1,
			String iriStr2) {

		return new Pair<OWLClass>(dataFactory.getOWLClass(IRI.create(iriStr1)),
				dataFactory.getOWLClass(IRI.create(iriStr2)));
	}

	public static void addEntityDeclarationToOntology(OWLOntology o,
			OWLOntologyManager manager, OWLEntity e) {
		alterOntologyWithEntityDeclaration(o, manager, e, true);
	}

	public static void alterOntologyWithEntityDeclaration(OWLOntology o,
			OWLOntologyManager manager, OWLEntity e, boolean add) {

		Set<OWLAxiom> axioms = new HashSet<>();

		axioms.add(getDataFactory().getOWLDeclarationAxiom(e));
		if (add)
			OntoUtil.addAxiomsToOntology(o, manager, axioms, true);
		else
			OntoUtil.removeAxiomsFromOntology(o, manager, axioms, true);
	}

	public static void addEntitiesDeclarationToOntology(OWLOntology o,
			OWLOntologyManager manager, Collection<OWLEntity> c) {
		for (OWLEntity e : c)
			addEntityDeclarationToOntology(o, manager, e);
	}

	public static void removeEntityDeclarationToOntology(OWLOntology o,
			OWLOntologyManager manager, OWLEntity e) {
		alterOntologyWithEntityDeclaration(o, manager, e, false);
	}

	public static void removeEntitiesDeclarationToOntology(OWLOntology o,
			OWLOntologyManager manager, Collection<OWLEntity> c) {
		for (OWLEntity e : c)
			removeEntityDeclarationToOntology(o, manager, e);
	}

	public static OWLReasoner getOWLLinkReasoner(OWLOntologyManager manager,
			OWLOntology o) {
		return owlLinkFactory.createReasoner(o);
	}

	public static OWLReasonerFactory getReasonerFactory(REASONER_KIND rk) {
		switch (rk) {
		case PELLET:
			return new com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory();
		case HERMIT:
			// return new org.semanticweb.HermiT.Reasoner.ReasonerFactory();
			return new UnsupportedDTHermitReasonerFactory();
		case ELK:
		case ELKTRACE:
			return new ElkReasonerFactory();
		case FACT:
			return new FaCTPlusPlusReasonerFactory();
		case KONCLUDE:
			return new OWLlinkHTTPXMLReasonerFactory();
		default:
			return new org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory();
		}
	}

	public static String extractIRIString(OWLOntology onto) {
		return onto.getOntologyID().getOntologyIRI().toString();
	}
}