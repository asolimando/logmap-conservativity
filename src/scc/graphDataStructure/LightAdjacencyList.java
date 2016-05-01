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
package scc.graphDataStructure;

import enumerations.ENTITY_KIND;
import scc.exception.ClassificationTimeoutException;
import scc.graphAlgo.Johnson;
import scc.graphAlgo.LightTarjan;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import logmap.LogMapWrapper;
import scc.mapping.LightOAEIMappingHandler;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.uncommons.maths.random.MersenneTwisterRNG;

import auxStructures.Pair;

import scc.thread.BadSCCDetectionThread;
import scc.thread.DiagnosisThread;
import uk.ac.manchester.cs.owl.owlapi.OWLDeclarationAxiomImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLEquivalentClassesAxiomImpl;
import uk.ac.ox.krr.logmap2.indexing.JointIndexManager;
import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;
import uk.ac.ox.krr.logmap2.utilities.Utilities;
import util.FileUtil;
import scc.util.LegacyFileUtil;
import util.OntoUtil;
import util.Params;
import util.Util;

import scc.comparator.ConfidenceComparator;
import scc.comparator.MappingHashedComparator;

public class LightAdjacencyList implements Serializable {

	private static final long serialVersionUID = 1883472482148354599L;

	public static IRI alignIRI = IRI.create("http://www.align.org/align#");
	//	private static OWLDataFactory dataFactory = OWLManager.getOWLDataFactory();
	//	public static OWLOntologyManager manager = 
	//			OWLManager.createOWLOntologyManager();
	private static ExecutorService executor = 
			Executors.newFixedThreadPool(Params.NTHREADS);
	private Map<LightNode, Map<LightNode,LightEdge>> adjList = null;
	private Map<LightNode, Map<LightNode,LightEdge>> reverseAdjList = 
			new HashMap<>(); 

	private static int edgesNum, vtxNum;

	private static boolean handleProp = false;

	public Set<String> datatypesProp = new HashSet<>();
	public Set<String> objectProp = new HashSet<>();

	private JointIndexManager preIndex;//, postIndex;

	private Map<String, ArrayList<LightNode>> nodes = new HashMap<>();
	//	private Map<String, LightNode> iri2NodeMap = new HashMap<>();

	private String mappingPathname;
	// mappings removed by the program (aligned ontology diagnosis)
	public Set<LightEdge> removedMappings = //new HashSet<>();
			Collections.synchronizedSet(new HashSet<LightEdge>());

	private Set<LightEdge> problematicMappings = new HashSet<>();
	private Set<LightEdge> originalMappings = new HashSet<>();
	private LightOAEIMappingHandler oaeiHandler = new LightOAEIMappingHandler();

	// the dual map is implicitly stored as node id
	private Map<Integer,LightNode> id2Node = new HashMap<>();

	private LightSCCs sccs = null;
	private LightSCCs localSCCs = null; 
	private LightTarjan tarjan = new LightTarjan();
	private String onto1Prefix = null,
			onto2Prefix = null;
	private Map<LightNode, Set<LightIndividual>> indivMap = new HashMap<>(); 

	private OWLOntology fstOnto, sndOnto;

	/** Gets the instances of a class expression parsed from a string.
	 * @param cls 
	 * 			  The OWL class for which we ask the individuals. 
	 * @param direct
	 *            Specifies whether direct instances should be returned or not.
	 * @return The instances of the specified class expression */
	private void loadInstances(OWLOntology o, LightNode n){
		OWLClass cls = n.getOWLClass();

		if(!n.isNamedConcept())
			return;

		if(indivMap.containsKey(n))
			throw new Error("Individuals already loaded for node " + n);

		Set<OWLClassAssertionAxiom> assAx = o.getClassAssertionAxioms(cls);    	
		Set<LightIndividual> inds = new HashSet<>();

		for (OWLClassAssertionAxiom ax : assAx)
			if(ax.getIndividual().isNamed())
				inds.add(new LightIndividual(n, 
						ax.getIndividual().asOWLNamedIndividual()));

		indivMap.put(n, Collections.unmodifiableSet(inds));
	}

	public void buildOntologyIndex(OWLOntology onto1, OWLOntology onto2, 
			String mapping){
		preIndex = LogMapWrapper.buildOntologiesIndex(onto1, onto2, mapping);
	}

	public Set<LightIndividual> getInstances(LightNode n){
		return indivMap.get(n);
	} 

	public Set<LightEdge> getOriginalMappings(){
		return Collections.unmodifiableSet(originalMappings);
	}

	public boolean isFirstNodeAdjacentToSecondNode(LightNode first, 
			LightNode second){
		return reverseAdjList.get(first).containsKey(second);
	}

	public String getOntoPrefix(boolean first){
		if(first)
			return onto1Prefix;
		return onto2Prefix;
	}

	public URI getOntoURI(boolean first){
		if(first)
			return OntoUtil.stringToURI(onto1Prefix);
		return OntoUtil.stringToURI(onto2Prefix);
	}

	public LightNode getNodeFromClass(OWLClass cls){
		ArrayList<LightNode> ns = nodes.get(cls.getIRI().toString());
		if(ns != null)
			for (LightNode n : ns)
				return n;
		return null;
	}

	public LightNode tryToGetNodeFromIRI(String nodeIRI, boolean first, 
			ENTITY_KIND kind){
		if(nodes.containsKey(nodeIRI) || objectProp.contains(nodeIRI))
			return getNodeFromIRI(nodeIRI,first,kind);
		return null;
	}

	public LightNode getNodeFromClass(OWLClass cls, boolean first){
		//		for (LightNode n : nodes.get(cls.getIRI().toString()))
		//			if(n.firstOnto == first)
		//				return n;
		//
		//		throw new Error("Node of ontology " + (first ? "1" : "2" ) 
		//				+ " associated with class " + cls + " does not exist!");
		return getNodeFromIRI(cls.getIRI().toString(),first,ENTITY_KIND.CLASS);
	}

	public LightNode getNodeFromIRI(String nodeIRI, boolean first, 
			ENTITY_KIND kind){
		if(Params.testMode 
				&& !datatypesProp.contains(nodeIRI) 
				&& !objectProp.contains(nodeIRI)
				&& !nodes.containsKey(nodeIRI))
			throw new Error("Unknown node with IRI " + nodeIRI);
		
		switch (kind) {
		case CLASS:
			if(!nodes.containsKey(nodeIRI)){
				if(Params.testMode){
					FileUtil.writeErrorLogAndConsole(nodeIRI + " unknown");

					for (OWLEntity e : fstOnto.getSignature(true)) {
						if(e.getIRI().toString().equals(nodeIRI))
							FileUtil.writeLogAndConsole(nodeIRI + " in onto 1");
					}

					for (OWLEntity e : sndOnto.getSignature(true)) {
						if(e.getIRI().toString().equals(nodeIRI))
							FileUtil.writeLogAndConsole(nodeIRI + " in onto 2");
					}
				}
				else 
					return null;
			}

			for (LightNode n : nodes.get(nodeIRI))
				if(n.firstOnto == first && n.getIRIString().equals(nodeIRI))
					return n;

			if(Params.testMode)
				throw new Error("None of the nodes of ontology " + 
						(first? "1" : "2") + "associated with " + nodeIRI 
						+ " has the right IRI");
			
			return null;
			
		case OBJPROP:
			if(objectProp.contains(nodeIRI)){
				if(!handleProp)
					return null;

				nodeIRI = OntoUtil.getGraphIRIObjectProperty(nodeIRI, 
						(first ? onto1Prefix : onto2Prefix) + "#");
			}
			else {
				FileUtil.writeErrorLogAndConsole("Unknown " + kind + 
						" with IRI " + nodeIRI);
			}
			break;
			
//		case DATAPROP:
//			break;
			
		default:
			throw new IllegalArgumentException("Illegal entity kind " 
					+ kind + " for node retrival given an IRI");
		}
		
		return null;
	}

	public LightNode getNodeFromId(String nodeID) {
		int id;
		try {
			id = Integer.parseInt(nodeID);
		}
		catch(NumberFormatException e){
			throw new Error(nodeID + " is not a valid numeric identifier");
		}

		if(Params.testMode && !id2Node.containsKey(id))
			throw new Error("Unknown node with id " + id);

		LightNode n = id2Node.get(id);

		if(n == null)
			return null;

		if(Params.testMode && !nodes.containsKey(n))
			throw new Error("Unknown node " + n 
					+ " associated to id " + id);

		return n;
	}

	public String toString(){
		StringBuilder str = new StringBuilder();

		return adjList.toString() + "\n" + str.toString();
	}

	// this method is meant to be used on aligned ontologies closed by 
	// logical inference w.r.t. subsumption/equivalence axioms
	public LightAdjacencyList(OWLOntology fstOnto, OWLOntology sndOnto, 
			OWLOntology alignedOnto){
		super();
		commonInit();

		this.fstOnto = fstOnto;
		this.sndOnto = sndOnto;

		Set<OWLClass> sigFst = fstOnto.getClassesInSignature(true), 
				sigSnd = sndOnto.getClassesInSignature(true);
		
		for (OWLSubClassOfAxiom subAx : 
				alignedOnto.getAxioms(AxiomType.SUBCLASS_OF))
			insertSubClassOfAxiom(sigFst, sigSnd, subAx);
		
		for (OWLEquivalentClassesAxiom eqAx 
				: alignedOnto.getAxioms(AxiomType.EQUIVALENT_CLASSES)){
			Iterator<OWLClassExpression> eqSigItr1 = 
					eqAx.getClassExpressions().iterator();
			OWLClass subC, supC;
			
			while(eqSigItr1.hasNext()){
				Iterator<OWLClassExpression> eqSigItr2 = 
						eqAx.getClassExpressions().iterator();
				
				if((subC = OntoUtil.getNamedClassesFromSubClassAxiom(
						eqSigItr1.next(), true)) == null)
					continue;
				
				while(eqSigItr2.hasNext()){
					if((supC = OntoUtil.getNamedClassesFromSubClassAxiom(
							eqSigItr2.next(), true)) == null)
						continue;
					
					insertSubClassOfAxiom(sigFst, sigSnd, 
							new Pair<OWLClass>(subC,supC));
				}
			}
		}
	}

	private LightEdge insertSubClassOfAxiom(Set<OWLClass> sigFst, 
			Set<OWLClass> sigSnd, OWLSubClassOfAxiom subAx){
		return insertSubClassOfAxiom(sigFst, sigSnd, 
				OntoUtil.getNamedClassesFromSubClassAxiom(subAx, true));
	}
	
	private LightEdge insertSubClassOfAxiom(Set<OWLClass> sigFst, 
			Set<OWLClass> sigSnd, Pair<OWLClass> p){

		OWLClass c1,c2;
		LightNode n1,n2;

		if(p == null)
			return null;

		c1 = p.getFirst();
		c2 = p.getSecond();

		if(c1 == null || c2 == null)
			return null;

		if(c1.equals(c2))
			return null;

		boolean fstElemFirstOnto = sigFst.contains(c1);
		boolean sndElemFirstOnto = sigFst.contains(c2);

		boolean isMapping = fstElemFirstOnto != sndElemFirstOnto;
		//			boolean isMapping = !(sigFst.contains(c1) && sigFst.contains(c2) 
		//					|| sigSnd.contains(c1) && sigSnd.contains(c2));

		n1 = getNodeFromClass(c1,fstElemFirstOnto);
		n2 = getNodeFromClass(c2,sndElemFirstOnto);

		if(n1 == null)
			n1 = createVertex(c1, true, fstElemFirstOnto);
		if(n2 == null)
			n2 = createVertex(c2, true, sndElemFirstOnto);

		//			if(n1 == null)
		//				System.out.println();
		//			if(n2 == null)
		//				System.out.println();

		if(getEdgeBetweenNodes(n1, n2) == null)
			return insertEdge(n1,n2,isMapping,false);
		
		return null;
	}
	
	@Deprecated
	private void insertSubClassOfAxioms(Set<OWLClass> sigFst, 
			Set<OWLClass> sigSnd, Set<OWLSubClassOfAxiom> subClassOfAxioms){

		for (OWLSubClassOfAxiom subAx : subClassOfAxioms)
			insertSubClassOfAxiom(sigFst, sigSnd, subAx);
	}
	
	@Deprecated
	public LightAdjacencyList(Set<LightEdge> mappings, 
			int numVertices1, int numVertices2, 
			double connPerc1, double connPerc2, double connPercMappings){
		super();
		commonInit();

		vtxNum = numVertices1 + numVertices2;

		//		if(LightAdjacencyList.manager == null)
		//			LightAdjacencyList.manager = OWLManager.createOWLOntologyManager();

		IRI iri1 = IRI.create("http://www.test.org/test1.owl"),
				iri2 = IRI.create("http://www.test.org/test2.owl");

		// first ontology
		for (int i = 0; i < numVertices1; i++)
			createVertex(OntoUtil.getDataFactory().getOWLClass(
					IRI.create(iri1 + "#" + i)), true);

		// second ontology
		for (int i = 0; i < numVertices2; i++)
			createVertex(OntoUtil.getDataFactory().getOWLClass(
					IRI.create(iri2 + "#" + i)), false);

		int fullConnectivity1 = (int) (Math.pow(numVertices1, 2) - numVertices1);
		int fullConnectivity2 = (int) (Math.pow(numVertices2, 2) - numVertices2);
		int alignedFullConnectivity = (int) (Math.pow(numVertices1+numVertices2, 2) 
				- (numVertices1+numVertices2));

		Random rnd = new MersenneTwisterRNG();

		FileUtil.writeLogAndConsole("First onto creation...");
		OWLClass cls = null;
		LightNode src = null, dst = null;

		Map<LightNode, Integer> occ = new HashMap<>();

		// first onto axioms
		for (int i = 0; i < ((int) fullConnectivity1 * connPerc1); i++) {
			if(Params.verbosity > 0 && i%10000 == 0)
				FileUtil.writeLogAndConsole(i + "/" 
						+ ((int) fullConnectivity1 * connPerc1));
			while(true){		
				while(true){
					cls = nodes.keySet().toArray(
							new OWLClass[0])[rnd.nextInt(nodes.size())];
					src = getNodeFromClass(cls, true);
					if(src != null && src.firstOnto){
						break;
					}
				}

				while(true){
					cls = nodes.keySet().toArray(
							new OWLClass[0])[rnd.nextInt(nodes.size())];
					dst = getNodeFromClass(cls, true);
					if(dst != null && dst.firstOnto){
						break;
					}
				}
				if(!src.equals(dst) && !adjList.get(src).containsKey(dst)){
					insertEdge(src, dst, false, false);
					int valS = 1, valD = 1;
					if(occ.containsKey(src))
						valS += occ.get(src);
					occ.put(src, valS);

					if(occ.containsKey(dst))
						valD += occ.get(dst);
					occ.put(dst, valD);

					break;
				}
			}
		}

		FileUtil.writeLogAndConsole(occ.toString());

		FileUtil.writeLogAndConsoleNONL(" done!\nSecond onto creation...");

		// second onto axioms
		for (int i = 0; i < ((int) fullConnectivity2 * connPerc2); i++) {
			if(Params.verbosity > 0 && i%10000 == 0)
				FileUtil.writeLogAndConsole(i + "/" 
						+ ((int) fullConnectivity2 * connPerc2));
			while(true){
				while(true){
					cls = nodes.keySet().toArray(
							new OWLClass[0])[rnd.nextInt(nodes.size())];
					src = getNodeFromClass(cls, false);
					if(src != null && !src.firstOnto)
						break;
				}

				while(true){
					cls = nodes.keySet().toArray(
							new OWLClass[0])[rnd.nextInt(nodes.size())];
					dst = getNodeFromClass(cls, false);
					if(dst != null && !dst.firstOnto)
						break;
				}
				if(!src.equals(dst) && !adjList.get(src).containsKey(dst)){
					insertEdge(src, dst, false, false);
					break;
				}
			}
		}
		localSCCs = computeLocalSCCs(null);

		FileUtil.writeLogAndConsoleNONL(" done!\nMappings...");

		// mappings
		for (int i = 0; 
				i < ((int) alignedFullConnectivity * connPercMappings); i++) {			
			if(Params.verbosity > 0 && i%10000 == 0)
				FileUtil.writeLogAndConsole(i + "/" 
						+ ((int) alignedFullConnectivity * connPercMappings));
			while(true){
				while(true){
					cls = nodes.keySet().toArray(
							new OWLClass[0])[rnd.nextInt(nodes.size())];
					src = nodes.get(cls).get(0);
					cls = nodes.keySet().toArray(
							new OWLClass[0])[rnd.nextInt(nodes.size())];
					dst = nodes.get(cls).get(0);
					if(src != null && dst != null 
							&& src.firstOnto != dst.firstOnto)
						break;
				}

				if(!adjList.get(src).containsKey(dst)){
					mappings.add(insertEdge(src, dst, true, false, 
							rnd.nextDouble()));
					break;
				}
			}
		}		
		FileUtil.writeLogAndConsole(" done!");
	}

	public LightAdjacencyList(String filePath, Set<LightEdge> mappings) 
			throws IOException{
		super();
		commonInit();

		//		LightAdjacencyList.manager = manager;

		onto1Prefix = "A";
		onto2Prefix = "B";

		FileUtil.writeLogAndConsole("Processing:\n" + filePath);

		FileInputStream fstream = new FileInputStream(filePath);
		// Get the object of DataInputStream
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;
		//Read File Line By Line
		String [] elems = null;

		IRI iri1 = IRI.create("http://www.test.org/test1.owl"),
				iri2 = IRI.create("http://www.test.org/test2.owl");

		// first ontology
		strLine = br.readLine();
		elems = strLine.split(";");
		for (String string : elems)
			if(!string.isEmpty())
				createVertex(OntoUtil.getDataFactory().getOWLClass(
						IRI.create(iri1 + "#" + string)), true);

		// second ontology
		strLine = br.readLine();
		elems = strLine.split(";");
		for (String string : elems)
			if(!string.isEmpty())
				createVertex(OntoUtil.getDataFactory().getOWLClass(
						IRI.create(iri2 + "#" + string)), false);

		// axioms
		strLine = br.readLine();
		elems = strLine.split(";");
		for (String string : elems){
			if(string.trim().isEmpty())
				continue;
			string = string.substring(1, string.length()-1).trim();
			String [] parts = string.split(",");
			insertAxiom(parts[0], parts[1], false, false, 
					Double.parseDouble(parts[3]), 
					Boolean.parseBoolean(parts[2]));
		}

		localSCCs = computeLocalSCCs(null);

		// mappings
		strLine = br.readLine();
		elems = strLine.split(";");
		for (String string : elems){
			if(string.trim().isEmpty())
				continue;
			string = string.substring(1, string.length()-1).trim();
			String [] parts = string.split(",");
			mappings.add(insertEdge(parts[0], parts[1], true, false, 
					Double.parseDouble(parts[3]), 
					Boolean.parseBoolean(parts[2])));
		}

		//Close the input stream
		in.close();
	}

	public LightAdjacencyList(File onto1, File onto2, Map<String, Double> stats, 
			boolean unloadOnto, boolean loadIndividuals, boolean unloadOnFailure) 
					throws OWLOntologyCreationException, ClassificationTimeoutException{
		super();
		commonInit();

		OWLOntology s = OntoUtil.load(onto1.getAbsolutePath(), true, OntoUtil.getManager(false)),
				t = OntoUtil.load(onto2.getAbsolutePath(), true, OntoUtil.getManager(false));

		this.fstOnto = s;
		this.sndOnto = t;

		onto1Prefix = OntoUtil.extractPrefix(s);
		onto2Prefix = OntoUtil.extractPrefix(t);

		FileUtil.writeLogAndConsole("Processing:\n" + s + "\n" + t);

		extractDatatypeIRIs(s);
		extractDatatypeIRIs(t);

		extractObjectPropertiesIRIs(s);
		extractObjectPropertiesIRIs(t);

		long start = Util.getMSec();;
		computeVertices(s, true, stats);
		computeVertices(t, false, stats);

		FileUtil.writeLogAndConsole("Vertices loading in: " 
				+ (Util.getDiffmsec(start)) + " ms");

		OWLReasoner rs = OntoUtil.getReasoner(s, Params.reasonerKind, 
				OntoUtil.getManager(false)), 
				rt = OntoUtil.getReasoner(t, Params.reasonerKind, 
						OntoUtil.getManager(false));
		List<OWLReasoner> reasoners = new ArrayList<>(2);
		reasoners.add(rs);
		reasoners.add(rt);

		long classTime = 0;
		if((classTime = classifyInputOntologies(reasoners)) == -1){
			if(unloadOnto)
				OntoUtil.unloadOntologies(s, t);
			throw new ClassificationTimeoutException();
		}

		rs = reasoners.get(0);
		rt = reasoners.get(1);

		start = Util.getMSec();
		computeEdges(s, rs, true, stats);
		computeEdges(t, rt, false, stats);
		FileUtil.writeLogAndConsole("Edges loading (+ classification) in: " 
				+ (Util.getDiffmsec(start)+classTime) + " ms");

		if(loadIndividuals)
			for (LightNode n : adjList.keySet())
				loadInstances(n.firstOnto ? s : t, n);

		//		if(unloadOnto)
		//			unloadOntologies(s,t);

		OntoUtil.disposeReasoners(rs,rt);

		localSCCs = computeLocalSCCs(stats);
	}

	//	public void unloadOntologies(OWLOntology s, OWLOntology t){
	//		unloadOntology(s);
	//		unloadOntology(t);
	//	}
	//
	//	public void unloadOntology(OWLOntology o){
	//		try {
	//			if(manager.contains(o.getOntologyID()))
	//				manager.removeOntology(o.getOntologyID());
	//		}
	//		catch(UnknownOWLOntologyException e){
	//			e.printStackTrace();
	//		}
	//	}

	public LightAdjacencyList(OWLOntology s, OWLOntology t, 
			Map<String, Double> stats, boolean unloadOnto) throws ClassificationTimeoutException{
		this(s,t,stats,unloadOnto,null,null);
	}
	
	public LightAdjacencyList(OWLOntology s, OWLOntology t, 
			Map<String, Double> stats, boolean unloadOnto, OWLReasoner rs, 
			OWLReasoner rt) 
					throws ClassificationTimeoutException{
		super();
		commonInit();

		onto1Prefix = OntoUtil.extractPrefix(s);
		onto2Prefix = OntoUtil.extractPrefix(t);

		this.fstOnto = s;
		this.sndOnto = t;

		FileUtil.writeLogAndConsole("Processing:\n" + s + "\n" + t);

		extractDatatypeIRIs(s);
		extractDatatypeIRIs(t);

		extractObjectPropertiesIRIs(s);
		extractObjectPropertiesIRIs(t);

		double start = Util.getMSec();
		computeVertices(s, true, stats);		
		if(stats != null)
			stats.put("VtxLoadTime1", Util.getDiffmsec(start));
		computeVertices(t, false, stats);
		if(stats != null)
			stats.put("VtxLoadTime2", (Util.getDiffmsec(start))
					-stats.get("VtxLoadTime1"));

		FileUtil.writeLogAndConsole("Vertices loading in: " 
				+ (Util.getDiffmsec(start)) + " ms");

		boolean classifNeeded = (rs == null || rt == null || 
				!OntoUtil.checkClassification(rs) || 
				!OntoUtil.checkClassification(rt));
		if(rs == null)
			rs = OntoUtil.getReasoner(s, Params.reasonerKind, 
				OntoUtil.getManager(false));
		if(rt == null)
			rt = OntoUtil.getReasoner(t, Params.reasonerKind, 
				OntoUtil.getManager(false));

		long classTime = 0;
				
		if(classifNeeded){
			List<OWLReasoner> reasoners = new ArrayList<>(2);
			reasoners.add(rs);
			reasoners.add(rt);
	
			if((classTime = classifyInputOntologies(reasoners)) == -1){
				if(unloadOnto)
					OntoUtil.unloadOntologies(s, t);
				throw new ClassificationTimeoutException();
			}
			rs = reasoners.get(0);
			rt = reasoners.get(1);
		}

		FileUtil.writeLogAndConsole("Classification in: " + classTime + " ms");
		
		start = Util.getMSec();
		computeEdges(s, rs, true, stats);
		if(stats != null){
			stats.put("EdgesLoadTime1", (Util.getDiffmsec(start)+classTime/2) );
			start = Util.getMSec();
		}
		computeEdges(t, rt, false, stats);
		if(stats != null)
			stats.put("EdgesLoadTime2", (Util.getDiffmsec(start)+classTime/2) );

		FileUtil.writeLogAndConsole("Edges loading in: " + 
				(stats == null ? 
				Util.getDiffmsec(start) :
				(stats.get("EdgesLoadTime1")+stats.get("EdgesLoadTime2")))
				+ " ms");

		localSCCs = computeLocalSCCs(stats);

		// we need them to classify the aligned ontology
		//		if(unloadOnto)
		//			unloadOntologies(s,t);

		OntoUtil.disposeReasoners(rs,rt);
	}

	private void extractDatatypeIRIs(OWLOntology onto) {
		for (OWLDataProperty dt : onto.getDataPropertiesInSignature(true))
			datatypesProp.add(dt.getIRI().toString());
		//datatypesProp.add(LightNode.iriProvider.getShortForm(dt.getIRI()));		
	}

	private void extractObjectPropertiesIRIs(OWLOntology onto) {
		for (OWLObjectProperty op : onto.getObjectPropertiesInSignature(true))
			objectProp.add(op.getIRI().toString());		
	}

	private LightSCCs computeLocalSCCs(Map<String, Double> stats){
		localSCCs = loopDetection(true, stats, true);
		FileUtil.writeLogAndConsole("#LocalSCC = " + localSCCs.size());
		if(stats != null)
			stats.put("NLocalSCCs", new Double(localSCCs.size()));
		return localSCCs;
	}

	public LightSCCs getLocalSCCs(){
		return localSCCs;
	}

	public Set<LightEdge> loadMappings(File mappingFile, 
			Set<MappingObjectStr> axioms){
		Set<LightEdge> mappings = new HashSet<>();

		mappingPathname = mappingFile != null ? mappingFile.getAbsolutePath() : "";

		int preEdges = edgesNum;
		int notClassMappings = 0;
		
		LightNode src,trg;
		for (MappingObjectStr m : axioms) {
			if(m.getTypeOfMapping() != Utilities.CLASSES){
				notClassMappings += LogMapWrapper.countMappings(m);
				continue;
			}
				
			String iri1 = m.getIRIStrEnt1(), 
					iri2 = m.getIRIStrEnt2();

			if(m.getMappingDirection() == Utilities.EQ){

				boolean swapped = false;
				src = tryToGetNodeFromIRI(iri1,true,ENTITY_KIND.CLASS);
				if(src == null){
					src = tryToGetNodeFromIRI(iri1,false,ENTITY_KIND.CLASS);
					swapped = true;
				}

				trg = tryToGetNodeFromIRI(iri2,swapped,ENTITY_KIND.CLASS);
				if(swapped && trg == null)
					throw new Error("Not a valid mapping because they do not " +
							"belong to different ontologies:\nIRI 1 = " + iri1 
							+ "\nIRI 2 = " + iri2);
				
				if(src == null)
					FileUtil.writeLogAndConsole("NULL VTX " +  
							tryToGetNodeFromIRI(iri1,swapped,ENTITY_KIND.CLASS));
				
				if(trg == null)
					FileUtil.writeLogAndConsole("NULL VTX " +  
							tryToGetNodeFromIRI(iri2,swapped,ENTITY_KIND.CLASS));
				
				mappings.add(insertEdge(src, trg, true, false, 
						m.getConfidence()));
				mappings.add(insertEdge(trg, src, true, false, 
						m.getConfidence()));
			}
			
			else if(m.getMappingDirection() == Utilities.L2R){
				src = tryToGetNodeFromIRI(iri1,true,ENTITY_KIND.CLASS);
				trg = tryToGetNodeFromIRI(iri2,false,ENTITY_KIND.CLASS);
				
				mappings.add(insertEdge(src, trg, true, false, 
							m.getConfidence()));
			}
			
			else if(m.getMappingDirection() == Utilities.R2L){
				src = tryToGetNodeFromIRI(iri1,true,ENTITY_KIND.CLASS);
				trg = tryToGetNodeFromIRI(iri2,false,ENTITY_KIND.CLASS);
				
				mappings.add(insertEdge(trg, src, true, false, 
							m.getConfidence()));
			}
			
			else 
				throw new IllegalArgumentException("Invalid mapping direction " +
						"for mapping " + m);
		}

		FileUtil.writeLogAndConsole((edgesNum-preEdges) + 
				" mappings loaded out of " + 
					LogMapWrapper.countMappings(axioms) + " processed");
		if(notClassMappings > 0)
			FileUtil.writeLogAndConsole(notClassMappings 
					+ " mapping(s) not for classes");
		
		return originalMappings = Collections.unmodifiableSet(mappings);
	}

//	private Boolean inferFirstOntologyFromIRI(String iri){
//		if(iri.startsWith(onto1Prefix))
//			return true;
//		else if(iri.startsWith(onto2Prefix))
//			return false;
//		return null;
//	}

	public Set<LightEdge> loadMappings(File mappingFile, 
			Map<String, Double> stats, boolean fullDetection){
		try {
			mappingPathname = mappingFile.getAbsolutePath();
			Set<LightEdge> mappings = null;
			mappings = oaeiHandler.parseAlignAPI(mappingFile, this, false);
			edgesNum += mappings.size();
			
			if(mappings.contains(null))
				throw new Error("Null mapping loaded!");
			
			if(stats != null){
				Diagnosis d = new Diagnosis(mappings);
				stats.put("NumM", new Double(mappings.size()));
				stats.put("wM", new Double(d.getWeight()));
				stats.put("1M", 
						new Double(d.toAlignment().getOneOneMappingNumber()));
			}

			if(fullDetection){
				OWLOntology alignedOnto = null;
				try {
					alignedOnto = OntoUtil.getManager(false).createOntology(alignIRI);
				} catch (OWLOntologyCreationException e) {
					e.printStackTrace();
				}

				Set<OWLAxiom> alignAxioms = new HashSet<>();
				alignAxioms.addAll(getOntology(0).getAxioms());
				alignAxioms.addAll(getOntology(1).getAxioms());

				for (LightEdge m : mappings){
					//FileUtil.writeLogAndConsole(m);
					alignAxioms.add(m.toOWLAxiom());
				}
				OntoUtil.addAxiomsToOntology(alignedOnto, 
						OntoUtil.getManager(false), alignAxioms, true);
			}

			return originalMappings = mappings;

		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("Error while loading mappings from file " 
					+ mappingFile.getName());
		}
	}

	public Diagnosis computeDiagnosis(LightSCCs globalSCCs,  
			Set<LightEdge> mappings, 
			Set<LightSCC> problematicSCCs, 
			Map<String, Double> stats, 
			double totalStartTime){

		double diagTimeStart = Util.getMSec();

		FileUtil.writeLogAndConsole("#V (aligned onto) = " + getNodes().size());

		// now pre-computed when creating the adjacency list
		//		LightTarjan tarjan = new LightTarjan();
		//		localSCCs.clear();
		//		localSCCs.addAll(tarjan.executeTarjan(this,mappings,false));
		//		FileUtil.writeLogAndConsole("#LocalSCCs = " + localSCCs.size());

		int originalProblemsNum = problematicSCCs.size();
		int numSCC = 0;

		globalSCCs.addAll(computeGlobalSCCsAndProblematicMappings(
				problematicSCCs, stats));

		int numProblematicMappings = problematicMappings.size();

		if(stats != null){
			stats.put("NumProblSCCs", new Double(
					originalProblemsNum = problematicSCCs.size()));
			int totV = 0;
			for (LightSCC l : problematicSCCs)
				totV += l.size();

			stats.put("NumVSCCs", new Double(totV));
		}
		if(Params.verbosity > 1 && problematicSCCs.size() > 0){
			FileUtil.writeLogAndConsole(problematicSCCs.toString());
			FileUtil.writeLogAndConsole(problematicSCCs.size() 
					+ " problematic SCCs (and their " + 
					problematicMappings.size() + " mappings):");

			if(Params.verbosity > 2)
				for (LightSCC scc : problematicSCCs)
					scc.printProblematicSCC(this);
		}

		if(stats!=null){
			stats.put("NumPM", new Double(problematicMappings.size()));
			stats.put("wPM", new Double(
					new Diagnosis(problematicMappings).getWeight()));
		}

		boolean filtered = false;

		if(!Params.disableFilterMultiple && Params.alwaysFilterMultiple){
			this.filterMultipleCorrespondences(problematicSCCs, true);
			filtered = true;
		}

		List<Future<Diagnosis>> handles = new ArrayList<Future<Diagnosis>>();
		Future<Diagnosis> future;
		if(executor.isShutdown())
			executor = Executors.newFixedThreadPool(Params.NTHREADS);

		for (LightSCC scc : problematicSCCs) {
			if(Params.verbosity > 1)
				FileUtil.writeLogAndConsole("SCC " + (numSCC++) 
						+ " (" + scc.printDimensions(this) + ")");

			boolean subOptimalD = false;

			if(!filtered && !Params.disableFilterMultiple){
				if(Params.alwaysFilterMultiple 
						|| Params.filterMultipleIfBigSCC){
					if(Params.alwaysFilterMultiple || 
							(scc.extractMappings(this, true).size() 
									> Params.mappingsMaxThresholdFilter 
									|| (
											scc.extractOriginalEdges(this).size()
											> (Params.arcsNoMappingMaxThresholdFilter 
													- Params.mappingsMaxThresholdFilter)
											)
									)
							){
						if(!this.filterMultipleCorrespondences(scc,true).isEmpty());
							subOptimalD = true;

						// check if it is still problematic after filtering
						if(!scc.isProblematic(this))
							continue;
					}
				}
			}
			future = executor.submit(new DiagnosisThread(this,scc, numSCC++, 
					subOptimalD));
			handles.add(future);
		}

		int numPartialDiag = 0;

		for (Future<Diagnosis> h : handles) {
			try {
				Diagnosis d = h.get();
				if(d == null)
					throw new Error("Null diagnosis");

				if(!d.isOptimal())
					++numPartialDiag;

				this.removedMappings.addAll(d);
			} catch (ExecutionException | InterruptedException e) {
				e.printStackTrace();
			}
		}

		executor.shutdownNow();

		if(stats != null){
			stats.put("DiagTime", Util.getDiffmsec(diagTimeStart));
			stats.put("TotTime", Util.getDiffmsec(totalStartTime));
			stats.put("NumSubDiag", new Double(numPartialDiag));
		}

		FileUtil.writeLogAndConsole("DiagnosisTime: " 
				+ (Util.getDiffmsec(diagTimeStart)) + " (ms)");

		FileUtil.writeLogAndConsole("TotalTime: " 
				+ (Util.getDiffmsec(totalStartTime)) + " (ms)\n");

		LegacyFileUtil.deleteAllFiles(Params.tmpDir);

		if(Params.alwaysTestDiagnosis || Params.testMode){
			//			Diagnosis cycleDiagnosis = computeDiagnosisOnCycles(problematicSCCs);
			//			removeMappings(cycleDiagnosis);			
			computeGlobalSCCsAndProblematicMappings(problematicSCCs, null);

			if(problematicSCCs.size() > 0){
				FileUtil.writeLogAndConsole(problematicSCCs.toString());
				FileUtil.writeLogAndConsole(problematicSCCs.size() 
						+ " problematic SCCs (and their " 
						+ problematicMappings.size() + " mappings):");

				if(Params.verbosity > -1)
					for (LightSCC scc : problematicSCCs)
						scc.printProblematicSCC(this);

				FileUtil.writeErrorLogAndConsole("FAILED TO FIX " + problematicSCCs.size() 
						+ " OVER " + originalProblemsNum);
			}
		}

		if(stats != null){
			stats.put("NumD", new Double(removedMappings.size()));
			stats.put("wD", new Double(new Diagnosis(removedMappings).getWeight()));
		}

		FileUtil.writeLogAndConsole("Diagnosis size/ProblM/M: " 
				+ removedMappings.size() + "/" 
				+ numProblematicMappings +"/" 
				+ mappings.size());		

		return new Diagnosis(removedMappings);		
	}

	public void removeMapping(LightEdge mapping){
		removedMappings.add(mapping);
		problematicMappings.remove(mapping);
	}

	public void removeMappings(Collection<LightEdge> mappings){
		removedMappings.addAll(mappings);
		problematicMappings.removeAll(mappings);
	}

	public void retractRemovedMappings(Collection<LightEdge> mappings) {
		removedMappings.removeAll(mappings);
		problematicMappings.addAll(mappings);
	}

	public void retractRemovedMappings(LightEdge mapping) {
		removedMappings.remove(mapping);
		problematicMappings.add(mapping);
	}

	public boolean edgesNotEmpty(List<LightNode> nodes){
		boolean res = false;

		for (LightNode from : nodes) {
			for (LightNode to : nodes) {
				if(reverseAdjList.get(from).containsKey(to) 
						|| reverseAdjList.get(to).containsKey(from))
					return true;
			}
		}
		return res;
	}

	public LightEdge getEdgeBetweenNodes(LightNode from, LightNode to){
//		LightEdge e = null;
//		Map<LightNode,LightEdge> m; 
//		if(adjList.containsKey(from))
		return adjList.get(from).get(to);
		
//		return e;
	}

	public Diagnosis computeDiagnosisOnCycles(Set<LightSCC> problematicSCCs, 
			boolean optCard){

		LightCycles cycles = null;
		Diagnosis diagnosis = new Diagnosis();
		int numSCC = 1;

		for (LightSCC scc : problematicSCCs) {
			long startDiagnosisTime = Util.getMSec();
			//if(numSCC == 93){
			FileUtil.writeLogAndConsole("SCC " + numSCC + " (" 
					+ scc.printDimensions(this) + ")\nMaxCycles: " 
					+ scc.getMaxElementaryCyclesNum());
			if((scc.extractMappings(this, true).size() 
					+ scc.extractOriginalEdges(this).size()) 
					> Params.arcsMaxThresholdFilter){
				filterMultipleCorrespondences(scc, true);

				// check if it is still problematic after filtering
				if(!scc.isProblematic(this))
					continue;
			}

			cycles = new Johnson().findElementaryCycles(this,scc,localSCCs);
			FileUtil.writeLogAndConsole(Util.getDiffmsec(startDiagnosisTime) + " ms");
			//cycles.addAll(scc.getAllMinimalCycles(this,true));

			//scc.printProblematicSCC(this);
			FileUtil.writeLogAndConsole(cycles.size() + " cycles");
			//System.out.println(cycles.toString().replace("],", "],\n"));
			List<MappingCycles> rank = cycles.generateMappingRanking(optCard);
			FileUtil.writeLogAndConsole("Ranking generated");
			FileUtil.writeLogAndConsole(Util.getDiffmsec(startDiagnosisTime) + " ms");

			//System.out.println(rank.toString().replace("],", "],\n"));
			diagnosis.addAll(greedyDiagnosisHeuristic(cycles, rank));
			FileUtil.writeLogAndConsole("SCC diagnosis computed");
			FileUtil.writeLogAndConsole(Util.getDiffmsec(startDiagnosisTime) + " ms");
			cycles.clear();
			//}
			//			else {
			//				diagnosis.addAll(scc.extractMappings(this, false));
			//			}
			numSCC++;
		}
		//FileUtil.writeLogAndConsole("DIAG " + diagnosis);
		return diagnosis;
	}

	public Diagnosis computeDiagnosisOnCycles(LightCycles cycles, LightSCC scc, 
			boolean cardOpt){
		long startTime = Util.getMSec();

		List<MappingCycles> rank = cycles.generateMappingRanking(cardOpt);
		Diagnosis d =greedyDiagnosisHeuristic(cycles, rank);
		d.setTime(Util.getDiffmsec(startTime));
		return d;
	}

	public Set<LightEdge> filterMultipleCorrespondences(LightSCC scc, 
			boolean applyChanges) {

		Set<LightEdge> filteredMappings = new HashSet<>();
		Set<LightEdge> localProblematicMappings = scc.extractMappings(this, true);
		int oldMappingNum = localProblematicMappings.size(), sourceFilteredMappings;
		long multipleFilterStartTime = Util.getMSec();
		Set<LightEdge> newProblemsFlattened = new HashSet<>();

		newProblemsFlattened = filterMultipleCorrespondences(
				localProblematicMappings, filteredMappings, true);
		sourceFilteredMappings = newProblemsFlattened.size();
		localProblematicMappings.clear();
		localProblematicMappings.addAll(filterMultipleCorrespondences(
				newProblemsFlattened, filteredMappings, false));

		if(Params.verbosity >= 0)
			FileUtil.writeLogAndConsole("Removed (" + (oldMappingNum - sourceFilteredMappings) 
					+ ";" + (sourceFilteredMappings - localProblematicMappings.size()) 
					+ ") (source;target) multiple-occurrences in " 
					+ (Util.getDiffmsec(multipleFilterStartTime)) + " ms");
		else if(!Params.alwaysFilterMultiple)
			FileUtil.writeLogAndConsole("Multiple-occurrences filtering in SCC");

		if(applyChanges)
			removeMappings(filteredMappings);

		return filteredMappings;
	}

	private void filterMultipleCorrespondences(Set<LightSCC> problematicSCCs, 
			boolean applyChanges) {
		int oldMappingNum = problematicMappings.size(), sourceFilteredMappings;
		long multipleFilterStartTime = Util.getMSec();
		Set<LightEdge> newProblemsFlattened;
		Set<LightEdge> filteredMappings = new HashSet<>();

		newProblemsFlattened = filterMultipleCorrespondences(
				problematicMappings,filteredMappings, true);
		sourceFilteredMappings = newProblemsFlattened.size();
		problematicMappings.clear();
		problematicMappings.addAll(filterMultipleCorrespondences(
				newProblemsFlattened,filteredMappings, false));

		if(Params.verbosity > 0)
			FileUtil.writeLogAndConsole("Removed (" + (oldMappingNum - sourceFilteredMappings) 
					+ ";" + (sourceFilteredMappings - problematicMappings.size()) 
					+ ") (source;target) multiple-occurrences in " 
					+ (Util.getDiffmsec(multipleFilterStartTime)) + " ms");
		else
			FileUtil.writeLogAndConsole("Multiple-occurrences filtering in all SCCs");

		if(applyChanges)
			removedMappings.addAll(filteredMappings);
	}

	private Diagnosis greedyDiagnosisHeuristic(LightCycles cycles, 
			List<MappingCycles> rank){
		Diagnosis diagnosis = new Diagnosis();

		LightCycles solvedCycles = new LightCycles();
		int i = 0;

		while(solvedCycles.size() < cycles.size()){
			if(!solvedCycles.containsAll(rank.get(i).cycles)){
				solvedCycles.addAll(rank.get(i).cycles);
				diagnosis.add(rank.get(i).mapping);
			}
			++i;
		}

		if(Params.testMode && !solvedCycles.containsAll(cycles) || 
				!cycles.containsAll(solvedCycles))
			throw new Error("Set of cycles to break and that of broken cycles " +
					"do not coincide");

		return diagnosis;
	}

	public LightSCCs computeGlobalSCCs(Map<String, Double> stats){
		LightSCCs globalSCCs = loopDetection(true, stats, false);
		FileUtil.writeLogAndConsole("#GlobalSCC = " + globalSCCs.size());

		int globNontrivial = 0;
		for (LightSCC s : globalSCCs)
			if(s.size() > 2)
				++globNontrivial;

		FileUtil.writeLogAndConsole("#NontrivialGlobalSCC = " + globNontrivial + "\n");

		if(stats != null){
			stats.put("NumGlobalSCCs", new Double(globalSCCs.size()));
			stats.put("NumNontrivGlobalSCCs", new Double(globNontrivial));
		}

		return globalSCCs;		
	}

	public LightSCCs computeGlobalSCCsAndProblematicMappings(
			Set<LightSCC> problematicSCCs, Map<String, Double> stats) {

		LightSCCs globalSCCs = computeGlobalSCCs(stats);

		double problemStartTime = Util.getMSec();
		FileUtil.writeLogAndConsoleNONL("Problematic mapping detection: ");
		problematicSCCs.clear();

		problematicSCCs.addAll(computeProblematicMappings(globalSCCs));

		FileUtil.writeLogAndConsole(Util.getDiffmsec(problemStartTime) + " (ms)");
		if(stats != null)
			stats.put("ProblMapppingsDetTime", 
					Util.getDiffmsec(problemStartTime));

		FileUtil.writeLogAndConsole(problematicSCCs.size() 
				+ " problematic SCCs (and their " 
				+ problematicMappings.size() 
				+ " mappings):");

		if(Params.verbosity > 1)
			for (LightSCC scc : problematicSCCs)
				scc.printProblematicSCC(this);

		return globalSCCs;
	}

	public LightSCCs filterMultipleCorrespondencesAndRecomputeGlobalSCCs(
			Set<LightSCC> problematicSCCs, Map<String, Double> stats) {

		filterMultipleCorrespondences(problematicSCCs, true);
		return computeGlobalSCCsAndProblematicMappings(problematicSCCs, stats);
	}

	private Set<LightEdge> filterMultipleCorrespondences(
			Set<LightEdge> mappings, Set<LightEdge> filteredMappings, 
			boolean sortOnSource){
		if(mappings.size() < 2)
			return mappings;

		Set<LightEdge> unique = new HashSet<>();

		List<MappingHashed> list = new LinkedList<>();
		for (LightEdge e : mappings)
			list.add(new MappingHashed(e));

		Collections.sort(list, new MappingHashedComparator(sortOnSource));

		LinkedList<LightEdge> buffer = new LinkedList<>();

		// if same confidence and an exact mapping (case insensitive) exists, we keep it
		MappingHashed exactMapping = null;

		Iterator<MappingHashed> itr = list.listIterator();
		MappingHashed prev = itr.next();
		if(Params.useExactMapping 
				&& prev.mapping.from.getName().equalsIgnoreCase(
						prev.mapping.to.getName()))
			exactMapping = prev;
		else
			buffer.addLast(prev.mapping);

		while (itr.hasNext()) {
			MappingHashed el = itr.next();

			// still scanning elements with same src (resp. dest) element
			if(sortOnSource ? prev.src == el.src : prev.dest == el.dest){
				if(Params.useExactMapping && exactMapping == null 
						&& el.mapping.from.getName().equalsIgnoreCase(
								el.mapping.to.getName()))
					exactMapping = el;
				else
					buffer.addLast(el.mapping);
			}
			// different src (resp. dest) element, we take only one element of the 
			// buffer before flushing it
			else{
				//				if(buffer.size() > 1)
				//					Collections.sort(buffer,new ConfidenceComparator());

				// we use the exact mapping because the buffer is empty
				if(exactMapping != null && buffer.isEmpty())
					unique.add(exactMapping.mapping);

				// we keep the exact mapping, it has higher confidence
				else if(exactMapping != null 
						&& buffer.get(0).confidence <= exactMapping.mapping.confidence){
					unique.add(exactMapping.mapping);

					// we mark as filtered the other elements of the buffer 
					for (LightEdge e : buffer) {
						if(!exactMapping.mapping.equals(e))
							filteredMappings.add(e);
						//removedMappings.add(e);
					}
				}
				// no exact mapping
				else {
					unique.add(buffer.get(0));

					for (int i = 1; i < buffer.size(); i++)
						filteredMappings.add(buffer.get(i));
					//removedMappings.add(buffer.get(i));

					// if it exists is useless
					if(exactMapping != null)
						filteredMappings.add(exactMapping.mapping);
				}				
				buffer.clear();
				exactMapping = null;
				prev = el;
				if(el.mapping.from.getName().equalsIgnoreCase(el.mapping.to.getName()))
					exactMapping = el;
				else
					buffer.addLast(el.mapping);
			}
		}

		if(exactMapping != null || !buffer.isEmpty()){
			//			if(buffer.size() > 1)
			//				Collections.sort(buffer,new ConfidenceComparator());

			if(exactMapping != null && buffer.isEmpty())
				unique.add(exactMapping.mapping);

			else if(exactMapping != null 
					&& buffer.get(0).confidence <= exactMapping.mapping.confidence){
				unique.add(exactMapping.mapping);

				for (LightEdge e : buffer) {
					if(!exactMapping.mapping.equals(e))
						filteredMappings.add(e);
					//removedMappings.add(e);
				}
			}
			else {
				unique.add(buffer.get(0));

				for (int i = 1; i < buffer.size(); i++)
					filteredMappings.add(buffer.get(i));
				//removedMappings.add(buffer.get(i));

				// if it exists is useless
				if(exactMapping != null)
					filteredMappings.add(exactMapping.mapping);
			}				
		}

		//removeEdges(removedMappings);
		return unique;
	}

	public Diagnosis bruteforceDiagnosis(Set<LightSCC> problematicSCCs, 
			LightSCCs globalSCCs, Map<String, Double> stats){
		Diagnosis diagnosis, minDiagnosis = null;
		int bitmask = 1;
		double diagnosisMinCost = Double.MAX_VALUE;

		do {
			Set<LightSCC> problSCCs = new HashSet<>(problematicSCCs);
			diagnosis = nextHypothesis(bitmask);
			++bitmask;

			this.removedMappings.clear();
			this.removedMappings.addAll(diagnosis);
			computeGlobalSCCsAndProblematicMappings(problSCCs, stats);

			if(problSCCs.size() == 0 
					&& (diagnosis.getWeight() < diagnosisMinCost 
							|| (diagnosis.getWeight() == diagnosisMinCost 
							&& diagnosis.size() < minDiagnosis.size())) ){
				minDiagnosis = diagnosis; 
				diagnosisMinCost = diagnosis.getWeight();
			}
		}
		while(!diagnosis.isEmpty());

		return (minDiagnosis != null) ? minDiagnosis 
				: new Diagnosis(problematicMappings);
	}

	public Diagnosis computeDiagnosis(Set<LightSCC> problematicSCCs, 
			boolean filterDeleted, Map<String, Double> stats){
		Diagnosis computedDiagnosis = new Diagnosis();
		Set<LightEdge> localProblematicMappings = new HashSet<>(problematicMappings);
		Set<LightSCC> localProblematicSCCs = new HashSet<>(problematicSCCs);

		int c = 1;
		do {
			FileUtil.writeLogAndConsole("ITERATION " + c++);

			computeGlobalSCCsAndProblematicMappings(localProblematicSCCs, stats);

			List<LightEdge> bridges = computeBridgeMappings(localProblematicSCCs, 
					stats, false);

			FileUtil.writeLogAndConsole(bridges.size() + " bridges");

			if(Params.verbosity > 1)
				FileUtil.writeLogAndConsole("\n\nBridges:\n" + 
						bridges.toString().replace(",", "\n"));

			bridges = selectBridges(bridges,localProblematicSCCs,filterDeleted);

			FileUtil.writeLogAndConsole(bridges.size() + " selected bridges");

			if(Params.verbosity > 1)
				FileUtil.writeLogAndConsole("Selected bridges:\n" + 
						bridges.toString().replace(",", "\n"));

			//adj.removedMappings.addAll(bridges);
			//adj.removeEdges(bridges);

			if(bridges.isEmpty()){
				if(!localProblematicSCCs.isEmpty())
					throw new Error("NO BRIDGES");

				/*				filterMultipleCorrespondencesAndRecomputeGlobalSCCs(
						localProblematicMappings, localProblematicSCCs);

				FileUtil.writeLogAndConsole("Filtered mappings:\n" + removedMappings.toString().replace(",", "\n"));
				 */

				/*				
				adj.computeBridgeMappings(problematicSCCs);

				FileUtil.writeLogAndConsole("\n\nBridges:\n" + 
						bridges.toString().replace(",", "\n") + "\n");
				 */
			}
			else {
				removedMappings.add(bridges.get(0));
				//adj.removeEdge(bridges.get(0));
			}
			FileUtil.writeLogAndConsole("Removed Mappings: " 
					+ removedMappings.toString().replace(",", "\n"));
		}
		while(!localProblematicSCCs.isEmpty());

		computedDiagnosis.addAll(removedMappings);

		return computedDiagnosis;
	}

	public boolean compareDiagnoses(Diagnosis diag1, Diagnosis diag2){
		if(diag1.size() != diag2.size())
			return false;

		for (LightEdge e : diag1)
			if(!diag2.contains(e))
				return false;

		return true;
	}

	private Diagnosis nextHypothesis(int bitmask){
		Diagnosis hyp = new Diagnosis();
		int actualIdx = -1;
		List<LightEdge> mappings = new ArrayList<LightEdge>(problematicMappings);

		if(Integer.bitCount(bitmask) == mappings.size())
			return new Diagnosis();

		String binaryRepr = new StringBuilder(Integer.toBinaryString(bitmask)).reverse().toString();

		while(binaryRepr.contains("1"))
		{
			actualIdx = binaryRepr.indexOf("1");
			hyp.add(mappings.get(actualIdx));
			binaryRepr = binaryRepr.replaceFirst("1", "0");
		}

		return hyp;
	}

	public boolean isVertexASourceOrSink(LightNode n, LightSCC scc, 
			boolean filterDeleted, boolean source){

		Set<LightEdge> edges = source 
				? getIncomingEdges(n, scc, false, filterDeleted) : 
					getOutgoingEdges(n, scc, false, filterDeleted);

				for (LightSCC localSCC : localSCCs) {
					if(localSCC.contains(n))
						return true;
				}

				return edges.size() == 0;
	}

	public List<LightEdge> selectBridges(Collection<LightEdge> bridges, 
			Set<LightSCC> localProblematicSCCs, boolean filterDeleted){
		List<LightEdge> selBridges = new LinkedList<>();

		for (LightEdge b : bridges){
			for (LightSCC scc : localProblematicSCCs) {
				if(scc.contains(b.from))
					if(isVertexASourceOrSink(b.from,scc,filterDeleted,false) 
							&& isVertexASourceOrSink(b.to,scc,filterDeleted,true))
						selBridges.add(b);
			}

		}		
		Collections.sort(selBridges,new ConfidenceComparator());

		return selBridges;
	}

	public void removeEdges(Collection<LightEdge> removedEdges) {
		for (LightEdge e : removedEdges)
			removeEdge(e);
		//removedEdges.clear();
	}

	private boolean isBridgeDeletionUseful(LightSCC scc, 
			Set<LightEdge> deletedMappings){
		LightTarjan tar = new LightTarjan();

		// we do no consider previously filtered mappings as well
		//Set<LightEdge> delMappings = new HashSet<LightEdge>(deletedMappings);
		//delMappings.addAll(removedMappings);

		for (LightSCC s : tar.executeTarjan(this, deletedMappings, scc, false)){
			LightSCC fstSCC = s.extract(true),
					sndSCC = s.extract(false);

			if((fstSCC.size() > 1 && !localSCCs.contains(fstSCC)) 
					&& (sndSCC.size() > 1 && !localSCCs.contains(sndSCC))){
				FileUtil.writeLogAndConsole(fstSCC.toString());
				FileUtil.writeLogAndConsole(sndSCC.toString());
				FileUtil.writeLogAndConsole("Edges: " + s.extractOriginalEdges(this));
				FileUtil.writeLogAndConsole("Mappings: " + s.extractMappings(this,true));
				FileUtil.writeLogAndConsole("DeletedMapping: " + deletedMappings);
				FileUtil.writeLogAndConsole(s.toString());
				return false;
			}
		}
		return true;
	}

	private int [] sccEnumerator(LightSCC scc, Set<LightEdge> deletedMappings){
		int [] res = {0,0};
		LightTarjan tar = new LightTarjan();
		res[0] = tar.executeTarjan(this, new HashSet<LightEdge>(), scc, false).size();
		res[1] = tar.executeTarjan(this, deletedMappings, scc, false).size();
		return res;
	}

	public static Set<LightSCC> computeProblematicSCCs(LightAdjacencyList adj, 
			String mapping, Map<String, Double> stats) throws IOException{ 

		Set<LightSCC> problematicSCCs = new HashSet<>();
		LightOAEIMappingHandler parser = new LightOAEIMappingHandler();
		Set<LightEdge> mappings = parser.parse(new File(mapping), adj, false);

		if(stats != null)
			stats.put("MappingsNum", new Double(mappings.size()));

		adj.computeGlobalSCCsAndProblematicMappings(problematicSCCs, stats);

		return problematicSCCs;
	}

	public LightSCCs computeProblematicSCCs(LightSCCs SCCs){
		return computeProblematicMappings(SCCs);
	}

	public OWLOntology getOntology(int index){
		if(index < 0 || index >= 2)
			throw new Error("Invalid ontology index in the manager");
		return index == 0 ? fstOnto : sndOnto;

		//		if(index < 0 || index >= OntoUtil.getManager(false).getOntologies().toArray(new OWLOntology[0]).length)
		//			throw new Error("Invalid ontology index in the manager");
		//
		//		return OntoUtil.getManager(false).getOntologies().toArray(new OWLOntology[0])[index]; 
	}

	private LightSCCs computeProblematicMappings(LightSCCs globSCCs){

		LightSCCs problematicSCCs = new LightSCCs();
		List<Future<LightSCCs>> handles = new ArrayList<Future<LightSCCs>>();
		Future<LightSCCs> future;

		problematicMappings.clear();

		if(executor.isShutdown())
			executor = Executors.newFixedThreadPool(Params.NTHREADS);

		for (LightSCC scc : globSCCs) {
			if(scc.size() > 2){
				future = executor.submit(
						new BadSCCDetectionThread(localSCCs,scc));
				handles.add(future);
			}
		}

		LightSCCs tmpSCCs = null;
		for (Future<LightSCCs> h : handles) {
			try {
				tmpSCCs = h.get();
				problematicSCCs.addAll(tmpSCCs);
				for (LightSCC lightSCC : tmpSCCs)
					problematicMappings.addAll(
							lightSCC.extractMappings(this, true));

			} catch (ExecutionException | InterruptedException e) {
				e.printStackTrace();
			}
		}

		executor.shutdownNow();

		//		int c = 0;
		//		for (LightSCC scc : problematicSCCs)
		//			if(scc.extractMappings(this, true).isEmpty())
		//				++c;
		//		
		//		FileUtil.writeLogAndConsole(c + "/" + problematicSCCs.size() + " unsolvable violations");

		return problematicSCCs;
	}

	public List<LightEdge> computeBridgeMappings(Set<LightSCC> problematicSCCs, 
			Map<String, Double> stats, boolean filter){

		double bridgeStartTime = Util.getMSec();
		FileUtil.writeLogAndConsoleNONL("\n\nBridge mapping detection: ");
		List<LightEdge> bridgeMappings = new LinkedList<>();
		Set<LightEdge> bridges;
		for (LightSCC scc : problematicSCCs) {
			bridges = new HashSet<>();
			if(filter)
				filterMultipleCorrespondences(scc, true);
			try {
				scc.findBridges(this,bridges);
			}
			catch (InterruptedException e){
				FileUtil.writeLogAndConsole("\nBridges timedout, partial result");
			}
			finally {
				bridgeMappings.addAll(bridges);
			}
		}
		if(stats != null)
			stats.put("computeBridgesTime", Util.getDiffmsec(bridgeStartTime));
		FileUtil.writeLogAndConsole((Util.getDiffmsec(bridgeStartTime)) + " ms");

		double sortTime = Util.getMSec();
		FileUtil.writeLogAndConsoleNONL("Sorting bridges: ");
		Collections.sort(bridgeMappings, new ConfidenceComparator());
		if(stats != null)
			stats.put("sortBridgesTime", Util.getDiffmsec(sortTime));
		FileUtil.writeLogAndConsole((Util.getDiffmsec(sortTime)) + " ms");

		return bridgeMappings;
	}

	//	private void convertToSNAPFile(Set<LightSCC> problematicSCCs, 
	//			String graphname, boolean ignoreRemoved){
	//
	//		BufferedWriter writer = null;
	//		try {
	//			try {
	//				FileUtil.writeLogAndConsole("Converting...");
	//
	//				File file =  new File(graphname+"_snap.gz");
	//				GZIPOutputStream zip = 
	//						new GZIPOutputStream(new FileOutputStream(file));
	//				writer = new BufferedWriter(
	//						new OutputStreamWriter(zip, "UTF-8"));
	//
	//				if(problematicSCCs.size() > 1)
	//					writer.append("#\n#\n# Nodes: "+this.nodes.size()+" Edges: "+
	//							(edgesNum-(ignoreRemoved ? 0 : removedMappings.size()))
	//							+"\n#\n");
	//				else{
	//					LightSCC scc = problematicSCCs.toArray(new LightSCC[0])[0];
	//					writer.append("#\n#\n# Nodes: " + scc.size() +" Edges: " +
	//							(scc.extractOriginalEdges(this).size() + 
	//									scc.extractMappings(this, ignoreRemoved).size()) 
	//									+ "\n#\n");
	//				}
	//
	//				for (LightSCC scc : problematicSCCs)
	//					for (LightNode n : scc)
	//						for (LightEdge e : adjList.get(n)){
	//							if((!ignoreRemoved || !removedMappings.contains(e)) 
	//									&& scc.contains(e.to))
	//								writer.append(n.getId()+"\t"+e.to.getId()+"\n");
	//						}
	//				FileUtil.writeLogAndConsole("...done.");
	//			} finally{           
	//				if(writer != null){
	//					writer.close();
	//				}
	//			}
	//		} catch (IOException e) {
	//			e.printStackTrace();
	//		}
	//	}

	//	public Set<LightEdge> computeBridgesLinear(Set<LightSCC> problematicSCCs, 
	//			boolean filter){
	//		//, Map<String, Long> stats
	//		boolean verbose = Params.verbosity > 0 || true;
	//		FileUtil.createDirPath(Params.outDirSB);
	//		convertToSNAPFile(problematicSCCs, Params.outGraphSB, filter);
	//		SF_GraphFactory.convertSNAPgraph2WebGraph(Params.outGraphSB, 
	//				Params.outDirSB, verbose);
	//		SF_Graph graph = SF_GraphFactory.loadSF_Graph(Params.outGraphSB, 
	//				true, verbose);
	//		SF_StrongX_Report<SF_Edge> bridgesRep = 
	//				SF_StrongBridges.computeSBs(graph, 
	//						Params.algoSB, 0, 3, verbose);
	//
	//		Set<LightEdge> bridges = new HashSet<>();
	//
	//		// flattening
	//		for (List<LightEdge> l : SBtoLightEdges(bridgesRep.StrongXsets))
	//			bridges.addAll(l);
	//
	//		return bridges;
	//	}

	//	private List<List<LightEdge>> SBtoLightEdges(LinkedList<Set<SF_Edge>> 
	//	strongSets){
	//		List<List<LightEdge>> sets = new LinkedList<>();
	//		for (Set<SF_Edge> set : strongSets) {
	//			List<LightEdge> sccSet = new LinkedList<>();
	//			for (SF_Edge sfe : set) {
	//				sccSet.add(SF_EdgeToLightEdge(sfe));
	//			}
	//			sets.add(sccSet);
	//		}
	//		return sets;
	//	}

	//	private LightEdge SF_EdgeToLightEdge(SF_Edge sfe){
	//		LightNode from = id2Node.get(sfe.from), to = id2Node.get(sfe.to);		
	//		return getEdgeBetweenNodes(from, to);
	//	}

	public Set<LightEdge> getIncomingEdges(LightNode node, LightSCC scc, 
			boolean mappingAlso, boolean filterDeleted) {
		Set<LightEdge> list = getIncomingEdges(node,mappingAlso,filterDeleted);
		Iterator<LightEdge> itr = list.iterator();
		LightEdge edge = null;

		while(itr.hasNext()){
			edge = itr.next();

			if(!scc.contains(edge.from))
				itr.remove();
		}

		return list;
	}

	public Set<LightEdge> getOutgoingEdges(LightNode node, 
			Set<LightNode> nodeSet, boolean mappingAlso, 
			boolean filterDeleted) {
		Set<LightEdge> list = getOutgoingEdges(node,mappingAlso,filterDeleted);
		Iterator<LightEdge> itr = list.iterator();
		LightEdge edge = null;

		while(itr.hasNext()){
			edge = itr.next();
			if(!nodeSet.contains(edge.to))
				itr.remove();
		}

		return list;
	}

	private void commonInit(){
		edgesNum = 0;
		vtxNum = 0;
		adjList = new HashMap<LightNode, Map<LightNode,LightEdge>>();
	}

	private void createVertex(OWLOntology onto, OWLObjectProperty role, 
			boolean first) {

		OWLObjectSomeValuesFrom existsRoleThing = 
				OntoUtil.getDataFactory().getOWLObjectSomeValuesFrom(role, 
						OntoUtil.getDataFactory().getOWLThing());

		OWLClass cls = OntoUtil.getDataFactory().getOWLClass(
				OntoUtil.getGraphIRIObjectProperty(role), 
				new DefaultPrefixManager(
						(first ? onto1Prefix : onto2Prefix) + "#")
				);	

		if(nodes.containsKey(cls.getIRI()))
			return;

		OWLDeclarationAxiom declAxiom = 
				new OWLDeclarationAxiomImpl(cls, 
						Collections.<OWLAnnotation> emptySet());
		OWLEquivalentClassesAxiom eqClAxiom = 
				new OWLEquivalentClassesAxiomImpl(
						new HashSet<>(Arrays.asList(cls, existsRoleThing)), 
						Collections.<OWLAnnotation> emptySet()
						);
		Set<OWLAxiom> axioms = new HashSet<>();
		axioms.add(declAxiom);
		axioms.add(eqClAxiom);
		OntoUtil.addAxiomsToOntology(onto, OntoUtil.getManager(false), 
				axioms, true);

		createVertex(cls,false,first);
	}

	private LightNode createVertex(OWLClass cls, boolean namedConcept, boolean first){
		return createVertex(cls,first,namedConcept,false);
	}
	private LightNode createVertex(OWLClass cls, boolean first){
		return createVertex(cls,first,true,false);
	}

	private LightNode createVertex(OWLClass cls, boolean first, 
			boolean namedConcept, boolean fullName){
		LightNode node = new LightNode(first, cls, namedConcept, vtxNum++);
		id2Node.put(vtxNum-1, node);

		//		String nodeName = (first ? "1_" : "2_") + node.getName(fullName);
		String nodeIRI = node.getIRIString();

//		if(nodes.containsKey(nodeIRI)){
//			if(!cls.isClassExpressionLiteral() 
//					//&& nodes.get(nodeIRI).isNamedConcept() 
//					&& !cls.equals(nodes.get(nodeIRI)))
//				throw new Error("Request to give to OWLClass " + cls + " IRI " 
//						+ nodeIRI + " but it has already been given to node " 
//						+ getNodeFromIRI(nodeIRI,first));
//			return getNodeFromIRI(nodeIRI,first);
//		} 

		if(nodes.containsKey(nodeIRI)){
			nodes.get(nodeIRI).add(node);
		}
		else {
			ArrayList<LightNode> nodeList = new ArrayList<>(2);
			nodeList.add(node);
			nodes.put(nodeIRI, nodeList);
		}
		adjList.put(node, new HashMap<LightNode,LightEdge>());
		reverseAdjList.put(node, new HashMap<LightNode,LightEdge>());

		return node;
	}

	private void computeVertices(OWLOntology onto, boolean first, 
			Map<String, Double> stats){
		int nodesNum = nodes.size();
		OWLClass cls;
		OWLOntology o = onto;
		//		for (OWLOntology o : onto.getImportsClosure()) {
		// computes nodes from concepts
		Set<OWLClass> classes = o.getClassesInSignature(true);
//		classes.remove(OntoUtil.getDataFactory().getOWLNothing());
//		classes.remove(OntoUtil.getDataFactory().getOWLThing());

		if(classes.isEmpty()){
			FileUtil.writeLogAndConsole("Ontology " + o.getOntologyID().getOntologyIRI() 
					+ " has no classes");
			return;
		}

		Iterator<OWLClass> itr = classes.iterator();
//		LightNode n;
		while(itr.hasNext()){
			cls = itr.next();
//			if(cls.getIRI().toString().toLowerCase().endsWith("subset"))
//				FileUtil.writeLogAndConsole(cls.toString());
			createVertex(cls, first);			
		}

		if(handleProp){
			// compute nodes from roles
			Set<OWLObjectProperty> roles = o.getObjectPropertiesInSignature(true);
			Iterator<OWLObjectProperty> itrR = roles.iterator();
			OWLObjectProperty role;
			while(itrR.hasNext()){
				role = itrR.next();
				createVertex(o, role, first);
			}
			//		}			
		}

		cls = onto.getOWLOntologyManager().getOWLDataFactory().getOWLThing();
		createVertex(cls, first);

		cls = onto.getOWLOntologyManager().getOWLDataFactory().getOWLNothing();
		createVertex(cls, first);

		if(stats != null){
			if(nodesNum == 0)
				stats.put("NumVtx1", new Double(nodes.size()));
			else
				stats.put("NumVtx2", new Double(nodes.size() - nodesNum));
		}
		//FileUtil.writeLogAndConsole(classNode.size() + " valid classes out of " + classes.size());
	}

	private void clearEdges(){
		for (LightNode n : adjList.keySet())
			adjList.get(n).clear();

		for (LightNode n : adjList.keySet())
			reverseAdjList.get(n).clear();

		edgesNum = 0;
	}

	public Diagnosis fromLogMapRepairToDiagnosis(
			Set<MappingObjectStr> repaired_mappings){
		Diagnosis d = new Diagnosis();

		for (MappingObjectStr mappingObjectStr : repaired_mappings) {
			LightEdge e = getAlignmentBetweenIRIs(
					IRI.create(mappingObjectStr.getIRIStrEnt1()), 
					IRI.create(mappingObjectStr.getIRIStrEnt2()), 
					mappingObjectStr.getTypeOfMapping() == Utilities.OBJECTPROPERTIES);
			if(e != null)
				d.add(e);
			else
				FileUtil.writeLogAndConsole(mappingObjectStr + " not found (type " 
						+ mappingObjectStr.getTypeOfMapping() + ")");
		}
		if(d.size() != repaired_mappings.size())
			FileUtil.writeLogAndConsole("Found only " + d.size() + "/" 
					+ repaired_mappings.size() + " mappings");
		return d;
	}

	public LightSCCs detectProblematicSCCs(List<OWLReasoner> reasoner, OWLOntology o){
		if(nodes.size() == 0)
			throw new Error("computeVertices must be called before computeEdges");

		double reasoningTime = Util.getMSec();

		FileUtil.writeLogAndConsoleNONL("Align Onto Classification: ");
		LightSCCs problematicSCCs = new LightSCCs();

		if(OntoUtil.ontologyClassification(true, false, reasoner, Params.tryPellet) == -1)
			return null;

		FileUtil.writeLogAndConsole(Util.getDiffmsec(reasoningTime) + " ms");

		int c = 0, d = 0, tot = o.getClassesInSignature(true).size();
		Iterator<OWLClass> itr = o.getClassesInSignature(true).iterator();
		OWLClass cls;
		Set<OWLClass> processed = new HashSet<>();

		while(itr.hasNext()){
			++d;
			cls = itr.next();

			if(d % 1000 == 0)
				FileUtil.writeLogAndConsole(d + "/" + tot 
						+ " vertices processed");

			if(processed.contains(cls))
				continue;
			processed.add(cls);

			LightNode clsNode = getNodeFromClass(cls);

			if(clsNode == null){
				FileUtil.writeLogAndConsole(cls + " node not found");
				continue;
			}

			LightSCC scc = new LightSCC();
			scc.add(clsNode);

			Set<OWLClass> equivClasses = 
					reasoner.get(0).getEquivalentClasses(cls).getEntitiesMinus(cls);

			for (OWLClass eq : equivClasses){
				scc.add(getNodeFromClass(eq));
				processed.add(eq);
			}

			if(scc.canBeProblematic(this)) 
				problematicSCCs.add(scc);
		}

		FileUtil.writeLogAndConsole("Total time for graph update: " 
				+ Util.getDiffmsec(reasoningTime) + " ms");

		return problematicSCCs;
	}

	public LightEdge getAlignmentBetweenIRIs(IRI from, IRI to, boolean role){
		for (LightEdge m : originalMappings) { 
			if(role){
				if(m.from.isNamedConcept() || m.to.isNamedConcept())
					continue;
				if(m.from.getOWLClass().getIRI().toString().contains(from.getFragment()) 
						&& m.to.getOWLClass().getIRI().toString().contains(to.getFragment()) ||
						m.to.getOWLClass().getIRI().toString().contains(from.getFragment()) 
						&& m.from.getOWLClass().getIRI().toString().contains(to.getFragment()) 
						)
					return m;
			}
			else {
				if(m.from.getOWLClass().getIRI().equals(from) 
						&& m.to.getOWLClass().getIRI().equals(to) ||
						m.to.getOWLClass().getIRI().equals(from) 
						&& m.from.getOWLClass().getIRI().equals(to) 
						)
					return m;
			}
		}
		//FileUtil.writeLogAndConsole(originalMappings);
		return null;
	}

	public LightSCCs detectUnsolvableProblematicSCCs(OWLReasoner reasoner, 
			LightSCCs sccs){
		int c = 0;
		LightSCCs unsolvProblSCCs = new LightSCCs();

		for (LightSCC scc : sccs) {			
			if(scc.isLogicallyProblematic(this)) {
				//if(Params.verbosity > 1)
				FileUtil.writeLogAndConsole(scc.problematicSCCAsString(this));
				++c;
				unsolvProblSCCs.add(scc);
			}			
		}
		//if(Params.verbosity > 0)
		FileUtil.writeLogAndConsole(c + "/" + sccs.size() + " unsolvable violations");
		return unsolvProblSCCs;
	}

	private long classifyInputOntologies(List<OWLReasoner> reasoners){

		FileUtil.writeLogAndConsoleNONL("Classification: ");
		double reasoningTime = Util.getMSec();

		//		if(Params.lightDisj || !Params.disj){
		return OntoUtil.ontologyClassification(false, false, 
				reasoners, Params.tryPellet);
		//		}
		//		else {
		//			rs.precomputeInferences(InferenceType.CLASS_HIERARCHY, 
		//					InferenceType.DISJOINT_CLASSES);
		//			rt.precomputeInferences(InferenceType.CLASS_HIERARCHY, 
		//					InferenceType.DISJOINT_CLASSES);
		//		}

//		FileUtil.writeLogAndConsole(Util.getDiffmsec(reasoningTime) + " ms");
//		return true;
	}

	private void computeEdges(OWLOntology o, OWLReasoner reasoner, 
			boolean first, Map<String, Double> stats){

		if(nodes.size() == 0)
			throw new Error("computeVertices must be called before computeEdges");

		int preEdgesNum = edgesNum;

		Set<OWLClass> classes = o.getClassesInSignature(true);
//		classes.remove(OntoUtil.getDataFactory().getOWLNothing());
//		classes.remove(OntoUtil.getDataFactory().getOWLThing());

		for (OWLClass cls : classes) {

			LightNode clsNode = getNodeFromClass(cls, first);

//			if(clsNode == null)
//				FileUtil.writeLogAndConsole(getNodeFromClass(cls, first).toString());
			
			if(handleProp){
				Set<OWLSubClassOfAxiom> subAxioms = 
						new HashSet<>(o.getSubClassAxiomsForSubClass(cls));

						for (OWLEquivalentClassesAxiom eqAx : o.getEquivalentClassesAxioms(cls))
							subAxioms.addAll(eqAx.asOWLSubClassOfAxioms());

						for (OWLSubClassOfAxiom subAx : subAxioms) {
							if(subAx.getSubClass().equals(cls) 
									&& subAx.getSuperClass().getClassExpressionType().equals(
											ClassExpressionType.OBJECT_SOME_VALUES_FROM)){
								OWLObjectPropertyExpression op = 
										((OWLObjectSomeValuesFrom) subAx.getSuperClass()).getProperty();
								if(op.isObjectPropertyExpression()){
									LightNode opNode = getNodeFromIRI(
											op.asOWLObjectProperty().getIRI().toString(), 
											first,ENTITY_KIND.OBJPROP);
									if(!clsNode.equals(opNode))
										insertEdge(clsNode,opNode,false,false);
								}
							}
							else if(subAx.getSuperClass().equals(cls) && 
									subAx.getSubClass().getClassExpressionType().equals(
											ClassExpressionType.OBJECT_SOME_VALUES_FROM)){
								OWLObjectPropertyExpression op = 
										((OWLObjectSomeValuesFrom) subAx.getSubClass()).getProperty();
								if(op.isObjectPropertyExpression() && 
										((OWLObjectSomeValuesFrom) 
												subAx.getSubClass()).getFiller().isOWLThing()){

									LightNode opNode = getNodeFromIRI(
											op.asOWLObjectProperty().getIRI().toString(), 
											first,ENTITY_KIND.OBJPROP);
									if(!clsNode.equals(opNode))
										insertEdge(opNode,clsNode,false,false);
								}
							}
						}
			}

			for (OWLClass sup : reasoner.getSuperClasses(cls, true).getFlattened()) {
				if(!classes.contains(sup))
					continue;
				LightNode supNode = getNodeFromClass(sup,first);
				insertEdge(clsNode, supNode, false, false);
			}

			for (OWLClass eq : reasoner.getEquivalentClasses(cls).getEntitiesMinus(cls)) {
				if(!classes.contains(eq))
					continue;
				LightNode eqNode = getNodeFromClass(eq, first);
				insertEdge(clsNode, eqNode, false, false);
				insertEdge(eqNode, clsNode, false, false);
			}

			//			if(Params.disj && !Params.lightDisj){
			//				for (OWLClass disj : reasoner.getDisjointClasses(cls).getFlattened()) {
			//					LightNode disjNode = getNodeFromClass(disj, first);
			//					insertEdge(clsNode, disjNode, false, true);
			//					insertEdge(disjNode, clsNode, false, true);				
			//				}
			//			}
		}

		FileUtil.writeLogAndConsole("SUB/EQ");

		//		if(Params.disj && Params.lightDisj){			
		//			for (OWLDisjointClassesAxiom disjAxiom : o.getAxioms(AxiomType.DISJOINT_CLASSES)) {
		//				List<OWLClassExpression> ceDisj = disjAxiom.getClassExpressionsAsList();
		//				for (int i = 0; i < ceDisj.size(); ++i) {
		//					OWLClassExpression ce = ceDisj.get(i);
		//
		//					if(!ce.isClassExpressionLiteral() && !ce.isAnonymous())
		//						continue;
		//
		//					LightNode ceNode = getNodeFromClass(ce.asOWLClass(), first);
		//					for (int j = i+1; j < ceDisj.size(); ++j) {
		//						OWLClassExpression oce = ceDisj.get(j);
		//						if(!oce.isClassExpressionLiteral() 
		//								&& !oce.isAnonymous())
		//							continue;
		//
		//						LightNode oceNode = getNodeFromClass(oce.asOWLClass(), first);
		//						insertEdge(ceNode, oceNode, false, true);
		//						insertEdge(oceNode, ceNode, false, true);
		//					}	
		//				}
		//			}
		//		}

		if(Params.disj)
			FileUtil.writeLogAndConsole("DISJ " 
					+ o.getAxiomCount(AxiomType.DISJOINT_CLASSES));

		if(stats != null){
			if(first)
				stats.put("NumEdges1", new Double(edgesNum));
			else
				stats.put("NumEdges2", new Double(edgesNum - preEdgesNum));
		}
	}

	public void updateTarjan(LightNode v){
		if(v != null){
			//			LightSCCs localSCCs = tarjan.executeLocalTarjan(this, (LightSCCs) sccs.clone(), v);
			//			FileUtil.writeLogAndConsole("Local Tarjan = " + localSCCs);
			//			FileUtil.writeLogAndConsole("Are Local and Global Tarjan equal? " + localSCCs.equals(tarjan.executeTarjan(this)) + "!");				
		}
	}

	public LightSCC loopDetection(LightNode v){
		if(sccs == null)
			sccs = tarjan.executeTarjan(this,false);
		FileUtil.writeLogAndConsole("Tarjan = " + sccs + "\nGraph = " + this);

		for (LightSCC component : sccs) {
			if(component.contains(v))
				return component;
		}

		return null;
	}

	public LightSCCs loopDetection(boolean ret, Map<String, Double> stats, 
			boolean inputOntologies){
		double start = Util.getMSec();

		//if(sccs == null)
		this.clearTarjanIndexes();

		sccs = tarjan.executeTarjan(this, removedMappings, inputOntologies);
		if(stats != null){
			if(ret && !stats.containsKey("GlobalSCCsNum") 
					&& stats.containsKey("MappingsNum")){
				//if(stats.containsKey("TarjanTime") && removedMappings.isEmpty())
				//	throw new Error("multiple tarjan measurements!");
				if(!stats.containsKey("TarjanTime"))
					stats.put("TarjanTime", Util.getDiffmsec(start));
			}
		}
		FileUtil.writeLogAndConsole((inputOntologies ? "Local" : "Global") 
				+ " Tarjan ended in " + (Util.getDiffmsec(start)) 
				+ " ms");

		if(!ret){
			boolean cycle = false;
			for (LightSCC component : sccs) {
				if(component.size() > 1)
				{
					cycle = true;
					break;
				}
			}
			FileUtil.writeLogAndConsole(cycle + " = " + sccs + "\n" + this);
		}
		if(ret)
			return sccs.clone();
		return sccs;
	}

	private void removeVertexTarjan(LightNode node,boolean filterDeleted){

		// remove its incoming edges		
		for (LightEdge inEdge : getIncomingEdges(node,true,filterDeleted)) {
			adjList.get(inEdge.from).remove(inEdge);
			reverseAdjList.remove(node);
		}

		// remove its outgoing edges
		for (LightEdge e : getOutgoingEdges(node,true,filterDeleted)) {
			reverseAdjList.get(e.to).remove(node);
		}

		adjList.remove(node);
		nodes.remove(node);
	}

	/*	private void removeEdgeTarjan(mxCell inEdge) {
		Edge edge = getEdge(inEdge);
		for (Entry<Node, List<Edge>> entry : adjList.entrySet())
			if(entry.getValue().remove(edge))
				break;

	}*/

	private Set<LightEdge> getIncomingEdges(LightNode node, boolean mappingAlso, 
			boolean filterDeleted) {
		Set<LightEdge> list = new HashSet<LightEdge>();
		for (LightEdge e : reverseAdjList.get(node).values()) {
			if(!mappingAlso && e.mapping)
				continue;
			if(filterDeleted && removedMappings.contains(e))
				continue;
			if(e.to.equals(node))
				list.add(e);
		}
		return list;
	}

	private Set<LightEdge> getOutgoingEdges(LightNode node, boolean mappingAlso, 
			boolean filterDeleted) {
		Set<LightEdge> outEdges = new HashSet<LightEdge>();
		for (LightEdge e : adjList.get(node).values()) {
			if(!mappingAlso && e.mapping)
				continue;
			if(filterDeleted && removedMappings.contains(e))
				continue;
			outEdges.add(e);
		}
		return outEdges;
	}

	public int getVertexNumber(){
		return vtxNum;
	}

	public void deleteVertices(List<LightNode> vertices){
		for (LightNode node : vertices)
			forgetVertex(node);
	}

	public boolean checkDataStructureConsistency(){
		String errorDescr = "Inconsistencies detected:\n";
		boolean error = false;

		FileUtil.writeLogAndConsole("Data structure consistency check:");

		//		if(nodes.size() != adjList.size() || nodes.size() != reverseAdjList.size() 
		//				|| adjList.size() != reverseAdjList.size()){
		//			error = true;
		//			errorDescr += "#nodes = " + nodes.size() 
		//					+ ", #adjList = "  + adjList.size() 
		//					+ ", #revAdjList = " + reverseAdjList.size() + "\n";
		//			Set<LightNode> setNodes = adjList.keySet();
		//			setNodes.removeAll(nodes.values());
		//			throw new Error(setNodes.toString());
		//		}

		for (LightNode n : adjList.keySet()) {
			for (LightEdge e : adjList.get(n).values()) {
				if(!reverseAdjList.get(e.to).containsKey(e.from)){
					error = true;
					errorDescr += "Edge " + e + " but no entry in reverse list\n";
				}
			}

			for (LightNode revNode : reverseAdjList.get(n).keySet()) {
				boolean localError = true;
				for (LightEdge edge : adjList.get(revNode).values()) {
					if(edge.to.equals(n)){
						localError = false;
						break;
					}
				}

				if(localError){
					error = true;
					errorDescr += "Reverse entry " + n + "->" 
							+ revNode + " but not in AdjList\n";
				}
			}
		}

		if(error){
			String err = errorDescr + "Graph data structure = " + this + "\n";
			FileUtil.writeErrorLogAndConsole(err);
			//throw new Error();
		}
		else
			FileUtil.writeLogAndConsole("Data structures are consistent");

		return error;
	}

	public Map<LightNode, Map<LightNode,LightEdge>> getAdjList(){
		return adjList;
	}

	public Map<LightNode, Map<LightNode,LightEdge>> getReverseAdjList(){
		return reverseAdjList;
	}

	public void forgetVertex(LightNode v){
		long start = Util.getMSec();;

		if(v == null){
			FileUtil.writeLogAndConsole("Trying to delete null node");
			return;
		}

		FileUtil.writeLogAndConsole("Deleting node " + v);

		Set<LightEdge> inEdges = getIncomingEdges(v,true,true),
				outEdges = getOutgoingEdges(v,true,true);
		LightEdge newEdge;
		LightNode substitute;
		Set<LightEdge> deletable = new HashSet<>();
		// in the end it must be deleted
		//deletable.add(vertex);

		// case 1: one of the other vertices in the loop inherits all the edges of v
		LightSCC cycle = loopDetection(v);
		int cycleSize = cycle.size();

		if(cycleSize > 1) {

			Params.testMode = true;
			FileUtil.writeLogAndConsole("CASE 1: Cycle detected\nIN-ARCS\n");

			LightNode subNode;
			LightEdge modEdge;
			Set<LightNode> exclude = new HashSet<LightNode>();
			exclude.add(v);
			//substitute = getSubstitute(cycle, exclude, vertex);

			for (LightEdge edge : inEdges) {				
				if(Params.testMode)
					FileUtil.writeLogAndConsole("Adapting edge " + edge);

				// to avoid self-edge
				exclude.add(edge.from);

				// to avoid dublicate edges
				Set<LightEdge> tmpOutEdges = getOutgoingEdges(edge.from,true,true);
				for (LightEdge tmpOutEdge : tmpOutEdges) {
					exclude.add(tmpOutEdge.to);
				}

				substitute = getSubstitute(cycle, exclude, v);

				deletable.add(edge);

				if(substitute != null){ // we replace vertex with substitute

					if(Params.testMode)
						FileUtil.writeLogAndConsole("New target selected = " + substitute);

					newEdge = insertEdge(edge.from, substitute, 
							edge.from.firstOnto != substitute.firstOnto,
							false);

					if(newEdge == null){
						if(Params.testMode)
							FileUtil.writeLogAndConsole("Edge already adapted!");
						continue;
					}
				}
				else{ // no substitution possible, we drop the edge
					if(Params.testMode)
						FileUtil.writeLogAndConsole("Adaptation for edge " + edge + " not possible");
					removeEdge(edge);
				}
				exclude.clear();
				exclude.add(v);
			}

			FileUtil.writeLogAndConsole("\nOUT-ARCS\n");

			for (LightEdge edge : outEdges) {

				if(Params.testMode)
					FileUtil.writeLogAndConsole("Adapting edge " + edge);
				// to avoid self-edge
				exclude.add(edge.to);

				// to avoid dublicate edges
				for (LightEdge tmpInEdge : getIncomingEdges(edge.to,true,true)) {
					exclude.add(tmpInEdge.to);
				}

				substitute = getSubstitute(cycle, exclude, v);

				deletable.add(edge);

				if(substitute != null){ // we replace vertex with substitute
					if(Params.testMode)
						FileUtil.writeLogAndConsole("New source selected = " + substitute);

					newEdge = insertEdge(substitute, edge.to, 
							substitute.firstOnto != edge.to.firstOnto, false);

					if(newEdge == null){
						if(Params.testMode)
							FileUtil.writeLogAndConsole("Edge already adapted!");
						continue;
					}

					removeEdge(edge);					
				}
				else{ // no substitution possible, we drop the edge
					if(Params.testMode)
						FileUtil.writeLogAndConsole("Adaptation for edge " + edge + " not possible");
					removeEdge(edge);
				}

				exclude.clear();
				exclude.add(v);
			}

			//delete vertex
			removeVertexTarjan(v,true);

			//			if(testMode){
			//				StringBuilder buf = new StringBuilder();
			//				for (mxCell mxCell : deletable) {
			//					buf.append(cellToString(mxCell) + "\n");
			//				}
			//				FileUtil.writeLogAndConsole("Elements removed =\n" + buf.toString());
			//			}

			removeVertexTarjan(v,true);

			return;
		}

		/* case 2: we choose an edge (s,v), for each edge (v,t) we 
		 * add a new edge (s,t), we delete (v,t); similarly for incoming edges */
		if(inEdges.size() > 0 && outEdges.size() > 0){
			LightEdge candidateInEdge, candidateOutEdge;

			FileUtil.writeLogAndConsole("CASE 2: arcs merging\n\nOUT-ARCS:\n");

			for (LightEdge outEdge : outEdges) {

				candidateInEdge = getMergingEdge(inEdges, outEdge, false, true);
				if(!deletable.contains(outEdge))
					deletable.add(outEdge);

				if(candidateInEdge != null){
					FileUtil.writeLogAndConsole("Merging edge " + candidateInEdge + " and " + outEdge);

					newEdge = insertEdge(candidateInEdge.from, outEdge.to, 
							candidateInEdge.from.firstOnto != outEdge.to.firstOnto,
							false);

					if(newEdge == null){
						if(Params.testMode)
							FileUtil.writeLogAndConsole("Edge " + outEdge + " already merged!");
						continue;
					}

					removeEdge(candidateInEdge);
				}
			}


			FileUtil.writeLogAndConsole("\nIN-ARCS:\n");

			for (LightEdge inEdge : inEdges) {

				candidateOutEdge = getMergingEdge(outEdges, inEdge, true, true);
				if(!deletable.contains(inEdge))
					deletable.add(inEdge);

				if(candidateOutEdge != null){
					FileUtil.writeLogAndConsole("Merging edge " + inEdge + " and " + candidateOutEdge);

					newEdge = insertEdge(inEdge.from, candidateOutEdge.to,
							inEdge.from.firstOnto != candidateOutEdge.to.firstOnto,
							false);

					if(newEdge == null){
						if(Params.testMode)
							FileUtil.writeLogAndConsole("Edge " + inEdge + " already merged!");
						continue;
					}

					removeEdge(candidateOutEdge);
				}
			}

			//			StringBuilder buf = new StringBuilder();
			//			for (mxCell mxCell : deletable) {
			//				buf.append(cellToString(mxCell) + "\n");
			//			}
			//			FileUtil.writeLogAndConsole("Elements removed =\n" + buf.toString());

			// remove from Tarjan's data structures the old arcs
			removeVertexTarjan(v,true);
			//removeMergedEdges(deletable);
			//removeCells(deletable.toArray(), false); // remove old arcs and vertex v

			return;
		}

		/* case 3: if exist only out-edges or in-edges for vertex v, 
		 * we delete v and all its edges  */
		FileUtil.writeLogAndConsole("CASE 3: no adaptation");

		removeVertexTarjan(v,true);

		FileUtil.writeLogAndConsole(" Done ! ("+(Util.getDiffmsec(start))+" ms)");
	}

	public void removeEdge(LightEdge edge) {
		adjList.get(edge.from).remove(edge);
		reverseAdjList.get(edge.to).remove(edge.from);
	}

	private LightEdge insertMergedEdge(LightEdge first, LightEdge second) {
		//FileUtil.writeLogAndConsole(getNode(first.getSource()) + " " + getNode(second.getTarget()) + "\n" + cellToString(merged));
		LightEdge edge = new LightEdge(first.from, second.to, 
				first.mapping || second.mapping, first.disjoint);
		adjList.get(edge.from).put(edge.to,edge);
		return edge;
	}
	public LightEdge insertEdge(LightNode source, LightNode target, 
			boolean mapping, boolean disjoint, double measure){
		LightEdge e = insertEdge(source, target, mapping, disjoint);
		e.confidence = measure;
		return e;
	}

	public LightEdge insertEdge(LightNode source, LightNode target, 
			boolean mapping, boolean disjoint){

		if(source == null || target == null)
			throw new Error("Edge must present non null vertices");

		if(source.equals(target))
			throw new Error("Self-loops not allowed: edge " + source);

		if(Params.testMode && testDuplicateEdge(source, target))
			return null;

		LightEdge edge = new LightEdge(source, target, mapping, disjoint);
		if(Params.testMode){
			if(!getNodeFromClass(source.getOWLClass(), 
					source.firstOnto).equals(source))
				throw new Error("source " + source);

			if(!getNodeFromClass(target.getOWLClass(), 
					target.firstOnto).equals(target))
				throw new Error("target " + target);
		}		
		adjList.get(source).put(target,edge);
		reverseAdjList.get(target).put(source,edge);

		if(Params.verbosity > 3)
			FileUtil.writeLogAndConsole("Edge created " + edge);

		++edgesNum;

		return edge;
	}

	private boolean testDuplicateEdge(LightNode source, LightNode substitute) {
		return adjList.get(source).containsKey(substitute);
	}

	private LightEdge getMergingEdge(Set<LightEdge> candidateEdges, 
			LightEdge toMerge, boolean outgoingWanted, boolean filterDeleted){
		Set<LightNode> excluded = new HashSet<LightNode>();

		if(outgoingWanted){
			for (LightEdge edge : this.getOutgoingEdges(toMerge.to,true,
					filterDeleted)) {
				excluded.add(edge.from);
			}
			for (LightEdge edge : candidateEdges) {
				if(edge.to != toMerge.from && !excluded.contains(edge.to))
					return edge;
			}
		}
		else {
			for (LightEdge edge : this.getIncomingEdges(toMerge.from,true,
					filterDeleted)) {
				excluded.add(edge.to);
			}
			for (LightEdge edge : candidateEdges) {
				if(edge.from != toMerge.to && !excluded.contains(edge.from))
					return edge;
			}
		}
		return null;
	}

	private LightNode getSubstitute(LightSCC cycle, Set<LightNode> exclude, 
			LightNode vertex) {
		List<LightNode> valid = new ArrayList<LightNode>();

		for (LightNode node : cycle) {
			valid.add(node);
		}		
		valid.removeAll(exclude);
		if(valid.isEmpty()){
			exclude.remove(vertex);
			return null;
		}

		return valid.get(0);
	}

	public Set<LightEdge> getAdjacent(LightNode v, 
			Collection<LightEdge> excludedMappings) {
		Set<LightEdge> res = new HashSet<>();
		res.addAll(adjList.get(v).values());

		if(excludedMappings != null && !excludedMappings.isEmpty())
			res.removeAll(excludedMappings);

		return res;
	}

	public Set<LightEdge> getAdjacent(LightNode v) {
		return getAdjacent(v, null);
	}

	public Collection<LightNode> getSourceNodeSet() {
		Set<LightNode> res = new HashSet<>();
		for (ArrayList<LightNode> l : nodes.values())
			res.addAll(l);
		return res;
	}

	public void clearTarjanIndexes(){
		for (LightNode node : getSourceNodeSet())
			node.resetTarjan();
	}

	public LightEdge insertAxiom(String src, String trg, boolean mapping, 
			boolean disjoint, double measure, boolean first) {
		//		LightNode source = getNodeFromName((first ? "1_" : "2_") + src), 
		//				target = getNodeFromName((first ? "1_" : "2_") + trg);

		LightNode source = getNodeFromIRI(src,first,ENTITY_KIND.CLASS), 
				target = getNodeFromIRI(trg,first,ENTITY_KIND.CLASS);

		if(source == null){
			if(Params.testMode)
				throw new Error("Unknown entity " + src + " mapped with " + trg);
			else
				return null;
		}
		if(target == null){
			if(Params.testMode)
				throw new Error("Unknown entity " + trg + " mapped with " + src);
			else
				return null;
		}

		return insertEdge(source, target, mapping, disjoint, measure);
	}

	public LightEdge insertEdge(String src, String trg, boolean mapping, boolean disjoint, 
			double measure, boolean flippedArgs) {
		//		LightNode source = getNodeFromName((flippedArgs ? "2_" : "1_") + src),
		//				target = getNodeFromName((flippedArgs ? "1_" : "2_") + trg);

		LightNode source = getNodeFromIRI(src,!flippedArgs,ENTITY_KIND.CLASS),
				target = getNodeFromIRI(trg,flippedArgs,ENTITY_KIND.CLASS);

		if(source == null){
			if(Params.testMode && !datatypesProp.contains(src))
				throw new Error("Unknown entity " + src + " mapped with " + trg);
			else
				return null;
		}
		if(target == null){
			if(Params.testMode && !datatypesProp.contains(trg))
				throw new Error("Unknown entity " + trg + " mapped with " + src);
			else
				return null;
		}

		return insertEdge(source, target, mapping, disjoint, measure);
	}

	public List<LightNode> getAdjacentNodes(LightNode v, LightSCC scc, 
			boolean directed, boolean mappingAlso, boolean filterDeleted) {
		List<LightNode> list = new LinkedList<LightNode>();
		for (LightEdge e : this.getOutgoingEdges(v, scc, mappingAlso, 
				filterDeleted))
			list.add(e.to);

		if(directed)
			return list;

		for (LightEdge e : this.getIncomingEdges(v, scc, mappingAlso, 
				filterDeleted))
			list.add(e.from);

		return list;
	}

	public Map<String, ArrayList<LightNode>> getNodes() {
		return nodes;
	}

	public LightOAEIMappingHandler getOAEIHandler() {
		return oaeiHandler;
	}

	public boolean isRemoved(LightEdge e) {
		return removedMappings != null && removedMappings.contains(e);
	}

	public Map<Integer, LightNode> getIdMap() {
		return id2Node;
	}

	//	public static OWLOntologyManager getOntologyManager() {
	//		return LightAdjacencyList.manager;
	//	}
	//
	//	public static OWLDataFactory getDataFactory() {
	//		return dataFactory;
	//	}

	//	public static void setDataFactory(OWLDataFactory dataFactory) {
	//		LightAdjacencyList.dataFactory = dataFactory;
	//	}

	public String getMappingPathname() {
		return mappingPathname;
	}

	public void setMappingPathname(String mappingPathname) {
		this.mappingPathname = mappingPathname;
	}
}
