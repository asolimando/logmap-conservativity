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
package scc.mapping;

import enumerations.ENTITY_KIND;
import scc.exception.ClassificationTimeoutException;
import fr.inrialpes.exmo.align.impl.eval.PRecEvaluator;
import fr.inrialpes.exmo.align.parser.AlignmentParser;
import scc.graphDataStructure.Diagnosis;
import scc.graphDataStructure.LightAdjacencyList;
import scc.graphDataStructure.LightEdge;
import scc.graphDataStructure.LightNode;
import scc.io.OAEIAlignmentOutput;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owlapi.model.OWLOntology;

import scc.util.LegacyFileUtil;
import util.FileUtil;
import util.Params;
import util.Util;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;


public class LightOAEIMappingHandler implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9030426886132305092L;


	public static void printPREval(PRecEvaluator e, LightAlignment mapping, 
			String mappingLabel, LightAlignment ref, String refLabel){
		FileUtil.writeLogAndConsole(mappingLabel + " = " + mapping.nbCells() 
				+ " " + refLabel + " = " + ref.nbCells()
				+ " Precision: " + e.getPrecision() 
				+ " Recall: " + e.getRecall() 
				+ " F-Measure: " + e.getFmeasure());
	}
	
	public static void PRAnalysis(LightAlignment original, 
			LightAlignment diagnosis, LightAlignment reference, 
			LightAlignment diagnosisRef, LightAlignment multiDiagnosis, 
			LightAlignment multiDiagnosisRef, 
			String matcher, String year, String track, String mappingName){
		
		if(original.nbCells() == 0){
			FileUtil.writeLogAndConsole("Empty alignment, skipping analysis");
			return;
		}
		
		if(mappingName.contains("/"))
			mappingName = mappingName.substring(mappingName.lastIndexOf('/')+1);
		
		FileUtil.writeLogAndConsole("\n" + matcher + " (" + year + "): " + 
				track + "(" + mappingName + ")");
		
		try {
			LightAlignment debugged = new LightAlignment(original);
			LightAlignment multiDebugged = new LightAlignment(original);
			LightAlignment refDebugged = new LightAlignment(reference);
			LightAlignment multiRefDebugged = new LightAlignment(reference);
			
//			System.out.println("|Original| = " + original.nbCells());
			debugged = debugged.diff(diagnosis);
			multiDebugged = multiDebugged.diff(multiDiagnosis);
//			System.out.println("|Debugged\\Diagnosis| = " + debugged.nbCells());
//			System.out.println("|Reference| = " + reference.nbCells());
			refDebugged = refDebugged.diff(diagnosisRef);
			multiRefDebugged = multiRefDebugged.diff(multiDiagnosisRef);
//			System.out.println("|Debugged\\DiagnosisRef| = " + refDebugged.nbCells());

			PRecOneOneEvaluator 
			evalOrig = new PRecOneOneEvaluator(reference, original),
			evalDbg = new PRecOneOneEvaluator(reference, debugged),
			multiEvalDbg = new PRecOneOneEvaluator(reference, multiDebugged),
			evalDbg2 = new PRecOneOneEvaluator(refDebugged, debugged),
			multiEvalDbg2a = new PRecOneOneEvaluator(multiRefDebugged, debugged),
			multiEvalDbg2b = new PRecOneOneEvaluator(refDebugged, multiDebugged),
			multiEvalDbg2c = new PRecOneOneEvaluator(multiRefDebugged, multiDebugged),
			evalDbg3 = new PRecOneOneEvaluator(refDebugged, original),
			multiEvalDbg3 = new PRecOneOneEvaluator(multiRefDebugged, original);
			
			// evaluates over all properties (relations)
			evalOrig.eval(new Properties());
			printPREval(evalOrig, original, "|M|", reference, "|R|");

			evalDbg.eval(new Properties());
			printPREval(evalDbg, debugged, "|M\\Diag|", reference, "|R|");

			multiEvalDbg.eval(new Properties());
			printPREval(multiEvalDbg, multiDebugged, "|M\\mDiag|", reference, 
					"|R|");

			evalDbg2.eval(new Properties());
			printPREval(evalDbg2, debugged, "|M\\Diag|", refDebugged, 
					"|R\\RDiag|");
			
			multiEvalDbg2a.eval(new Properties());
			printPREval(evalDbg2, debugged, "|M\\Diag|", multiRefDebugged, 
					"|R\\mRDiag|");
			
			multiEvalDbg2b.eval(new Properties());
			printPREval(evalDbg2, multiDebugged, "|M\\mDiag|", refDebugged, 
					"|R\\RDiag|");
			
			multiEvalDbg2c.eval(new Properties());
			printPREval(evalDbg2, multiDebugged, "|M\\mDiag|", multiRefDebugged, 
					"|R\\mRDiag|");
			
			evalDbg3.eval(new Properties());
			printPREval(evalDbg3, original, "|M|", refDebugged, "|R\\RDiag|");
			
			multiEvalDbg3.eval(new Properties());
			printPREval(evalDbg3, original, "|M|", multiRefDebugged, 
					"|R\\mRDiag|");
			
			LegacyFileUtil.printAnalysisExp1aToFile(matcher.toLowerCase()+" "+track
					+" "+mappingName+" "+year, evalOrig, evalDbg, multiEvalDbg, 
					evalDbg2, multiEvalDbg2a, multiEvalDbg2b, multiEvalDbg2c, 
					evalDbg3, multiEvalDbg3);
			
		} catch (AlignmentException e) {
			e.printStackTrace();
		}
	}

	public void checkMappings(LightAdjacencyList adj, 
			String [] fileMappings) throws IOException{

		OWLOntology fstO = adj.getOntology(0), sndO = adj.getOntology(1);

		//		List<Set<LightEdge>> listMappings = 
		//				new ArrayList<Set<LightEdge>>(fileMappings.length);

		LightAdjacencyList mine, alcomo, logmap;
		try {
			mine = new LightAdjacencyList(fstO,sndO,null,false);
			alcomo = new LightAdjacencyList(fstO,sndO,null,false);
			logmap = new LightAdjacencyList(fstO,sndO,null,true);
		} catch (ClassificationTimeoutException e) {
			FileUtil.writeErrorLogAndConsole(e.getMessage());
			return;
		}

		FileUtil.writeLogAndConsole("MIO: " + LightAdjacencyList.computeProblematicSCCs(
				mine, fileMappings[1], null).size() + " violations");

		FileUtil.writeLogAndConsole("ALCOMO: " + LightAdjacencyList.computeProblematicSCCs(
				alcomo, fileMappings[2], null).size() + " violations");

		FileUtil.writeLogAndConsole("LOGMAP: " + LightAdjacencyList.computeProblematicSCCs(
				logmap, fileMappings[3], null).size() + " violations");
	}

	public void compareMappings(LightAdjacencyList adj, String [] fileMappings) throws IOException{

		List<Set<LightEdge>> listMappings = new ArrayList<Set<LightEdge>>(fileMappings.length);

		for (String s : fileMappings) {
			listMappings.add(MappingDiffer.loadMappings(new File(s), adj));
		}

		Set<LightEdge> removedAlcomo = new HashSet<LightEdge>(listMappings.get(0)),
				removedLogmap = new HashSet<LightEdge>(listMappings.get(0));

		removedAlcomo.removeAll(listMappings.get(2));
		removedLogmap.removeAll(listMappings.get(3));

		Set<LightEdge> myMappings1 = new HashSet<LightEdge>(listMappings.get(1));
		Set<LightEdge> myMappings2 = new HashSet<LightEdge>(listMappings.get(1));

		myMappings1.removeAll(removedAlcomo);
		myMappings2.removeAll(removedLogmap);

		FileUtil.writeLogAndConsole("\nAlcomo additional remove: " + removedAlcomo.size());
		FileUtil.writeLogAndConsole("LogMap additional remove: " + removedLogmap.size());

		listMappings.get(2).removeAll(listMappings.get(1));
		listMappings.get(3).removeAll(listMappings.get(1));

		FileUtil.writeLogAndConsole("Shared with Alcomo: " + myMappings1.size() + "/" + listMappings.get(1).size());
		FileUtil.writeLogAndConsole("Shared with LogMap: " + myMappings2.size() + "/" + listMappings.get(1).size());

		FileUtil.writeLogAndConsole("Alcomo not detected: " + listMappings.get(2).size());
		FileUtil.writeLogAndConsole("LogMap not detected: " + listMappings.get(3).size());
	}

	public void writeMappings(String mappingFilename, Set<LightEdge> mappings, 
			String onto1Prefix, String onto2Prefix){
		try {
			OAEIAlignmentOutput mappingOut = new OAEIAlignmentOutput(
					mappingFilename, 
					onto1Prefix, onto2Prefix);
			LightEdge oppositeEdge = null;
			Set<LightEdge> alreadyAdded = new HashSet<LightEdge>();

			for (LightEdge m : mappings) {
				if(m == null || alreadyAdded.contains(m))
					continue;

				oppositeEdge = new LightEdge(m.to, m.from, m.mapping, m.disjoint, m.confidence);

				if(mappings.contains(oppositeEdge)){
					mappingOut.addClassMapping2Output(
							onto1Prefix + "#" + m.from.getName(), 
							onto2Prefix + "#" + m.to.getName(), 
							OAEIAlignmentOutput.EQ, 
							m.confidence);
					alreadyAdded.add(oppositeEdge);
				}
				else
					mappingOut.addClassMapping2Output(
							onto1Prefix + "#" + m.from.getName(), 
							onto2Prefix + "#" +  m.to.getName(), 
							OAEIAlignmentOutput.L2R, 
							m.confidence);
			}
			mappingOut.saveOutputFile();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public LightAlignment parseAlignAPI(File mapping, boolean custom) throws IOException{
		FileUtil.writeLogAndConsole("Loading RDF mapping file " + mapping.getName() 
				+ (custom ? " (custom parser)" : " (alignAPI parser)"));
		long start = Util.getMSec();

		LightAlignment lAlign = null;

		if(!custom){
			AlignmentParser aparser = new AlignmentParser(0);
			Alignment align = null;

			try {
				align = aparser.parse(mapping.toURI());
			} catch (AlignmentException e) {
				FileUtil.writeLogAndConsole("AlignAPI parser failed");
				return parseAlignAPI(mapping, true);
			}
			lAlign = new LightAlignment(align);
		}
		else {
			lAlign = new LightAlignment();
			Model model = rdfMappingToModel(mapping);

			String URIbaseNoSharp = "http://knowledgeweb.semanticweb.org/heterogeneity/alignment";
			String URIbase = model.getNsPrefixMap().containsKey("align") 
					? model.getNsPrefixMap().get("align") : model.getNsPrefixMap().get("");

			if(URIbase == null)
				URIbase = "http://knowledgeweb.semanticweb.org/heterogeneity/alignment#";

			String relation = null, src = null, trg = null;
			double measure = 1;

			StmtIterator s = model.listStatements();

			while(s.hasNext()){
				Statement st = s.next();
				Triple triple = st.asTriple();

				if(triple.getPredicate().hasURI(URIbase + "relation") 
						|| triple.getPredicate().hasURI(URIbaseNoSharp + "relation"))
					relation = triple.getObject().toString();
				else if(triple.getPredicate().hasURI(URIbase + "entity2") 
						|| triple.getPredicate().hasURI(URIbaseNoSharp + "entity2")){
					trg = triple.getObject().toString();
					//(trg = triple.getObject().toString()).substring(trg.lastIndexOf('#')+1);
				}
				else if(triple.getPredicate().hasURI(URIbase + "measure") 
						|| triple.getPredicate().hasURI(URIbaseNoSharp + "measure")){
					try {
						measure = Double.parseDouble(
								triple.getObject().getLiteralLexicalForm());//.getLiteralValue().toString());
					}catch(NumberFormatException exc){
						exc.printStackTrace();
					}
				}
				else if(triple.getPredicate().hasURI(URIbase + "entity1") 
						|| triple.getPredicate().hasURI(URIbaseNoSharp + "entity1")){
					src = triple.getObject().toString();
							//(src = triple.getObject().toString()).substring(src.lastIndexOf('#')+1);
					try {
						URI srcURI = new URI(src), trgURI = new URI(trg);
						
						if(relation.substring(1,2).compareTo("<") == 0){
							lAlign.addAlignCell(srcURI, trgURI, "<", measure);
						}
						else if(relation.substring(1,2).compareTo(">") == 0){
							lAlign.addAlignCell(trgURI, srcURI, "<", measure);
						}
						else if(relation.substring(1, 2).compareTo("=") == 0 
								|| relation.compareToIgnoreCase("\"fr.inrialpes.exmo.align.impl.rel.EquivRelation@50\"") == 0){
							lAlign.addAlignCell(srcURI, trgURI, "<", measure);
							lAlign.addAlignCell(trgURI, srcURI, "<", measure);
						}
					} catch (AlignmentException e) {
						e.printStackTrace();
					} catch (URISyntaxException e) {
						e.printStackTrace();
					}
				}
			}
		}

		FileUtil.writeLogAndConsole(lAlign.nbCells() + " mappings loaded in " 
				+ (Util.getDiffmsec(start)) + "ms");

		return lAlign;
	}

	public Set<LightEdge> parseAlignAPI(File mapping, LightAdjacencyList adjList, 
			boolean loadOnly) throws IOException {
		FileUtil.writeLogAndConsole("Loading RDF mapping file " + mapping.getName());
		long start = Util.getMSec();
		int nulls = 0;
		
		Set<LightEdge> mappings = new HashSet<LightEdge>();

		AlignmentParser aparser = new AlignmentParser(0);
		Alignment align;
		try {
			align = aparser.parse(mapping.toURI());

			Iterator<Cell> itr = align.iterator();
			Cell cell = null;

			String relation = null, src = null, trg = null;
			double measure = 1;
			LightNode source = null, target = null, sourceRev = null, targetRev = null;
			
			while(itr.hasNext()){
				LightEdge newMapping1 = null, newMapping2 = null;

				cell = itr.next();
//				src = cell.getObject1AsURI().getFragment();
//				trg = cell.getObject2AsURI().getFragment();
				
				src = cell.getObject1AsURI().toString();
				trg = cell.getObject2AsURI().toString();

				measure = cell.getStrength();
				relation = cell.getRelation().getRelation();

//				source = adjList.getNodeFromName("1_" + src);
//				target = adjList.getNodeFromName("2_" + trg);
//				sourceRev = adjList.getNodeFromName("2_" + src);
//				targetRev = adjList.getNodeFromName("1_" + trg);
				
				source = adjList.tryToGetNodeFromIRI(
						cell.getObject1AsURI().toString(),true,ENTITY_KIND.CLASS);
				target = adjList.tryToGetNodeFromIRI(
						cell.getObject2AsURI().toString(),false,ENTITY_KIND.CLASS);
				if(source == null)
					sourceRev = adjList.tryToGetNodeFromIRI(
							cell.getObject1AsURI().toString(),false,
							ENTITY_KIND.CLASS);
				if(target == null)
					targetRev = adjList.tryToGetNodeFromIRI(
							cell.getObject2AsURI().toString(),true,
							ENTITY_KIND.CLASS);

				if(source == null || target == null){
					source = sourceRev;
					target = targetRev;
				}

				if(!checkElements(src,trg,source,target,sourceRev,targetRev,adjList))
					continue;	

				if(relation.equals("<")){
					if(!loadOnly){
						newMapping1 = adjList.insertEdge(
								src, trg, true, false, measure, false);
					}
					else {
						newMapping1 = new LightEdge(
								source, target, true, false, measure);
					}
					nulls = addMappingIfNotNull(mappings, newMapping1, nulls);
				}	
				else if(relation.equals(">")){
					if(!loadOnly){
						newMapping1 = adjList.insertEdge(
								trg, src, true, false, measure, true);
					}
					else {
						newMapping1 = new LightEdge(
								target, source, true, false, measure);
					}
					nulls = addMappingIfNotNull(mappings, newMapping1, nulls);
				}
				else if(relation.equals("=")){
					if(!loadOnly){
						newMapping1 = adjList.insertEdge(
								src, trg, true, false, measure, false);
						newMapping2 = adjList.insertEdge(
								trg, src, true, false, measure, true);
					}
					else {
						newMapping1 = new LightEdge(
								source, target, true, false, measure);
						newMapping2 = new LightEdge(
								target, source, true, false, measure);
					}
					if(newMapping1 != null && newMapping2 != null){
						nulls = addMappingIfNotNull(mappings, newMapping1, nulls);
						nulls = addMappingIfNotNull(mappings, newMapping2, nulls);
					}
				}
				else if(relation.equals("%") // unsupported relations 
						|| relation.equals("HasInstance") 
						|| relation.equals("InstanceOf"))
					continue;

				else if(Params.testMode)
					throw new Error("Unknown relation: " + relation);
			}

			FileUtil.writeLogAndConsole(mappings.size() + " mappings (" 
					+ align.nbCells() + " in RDF) loaded in " 
					+ (Util.getDiffmsec(start)) + "ms");

		} catch (AlignmentException e) {
			FileUtil.writeLogAndConsole("AlignAPI parser failed, using custom parser\n");
			mappings = parse(mapping, adjList, loadOnly);
		}

		if(Params.testMode)
			adjList.checkDataStructureConsistency();

		return mappings;
	}
	
	private int addMappingIfNotNull(Set<LightEdge> mappings, LightEdge m, int nulls){
		if(m != null){
			mappings.add(m);
			return nulls;
		}
		else
			return nulls+1;
	}

	private boolean checkElements(String src, String trg,
			LightNode source, LightNode target, 
			LightNode sourceRev, LightNode targetRev, 
			LightAdjacencyList adjList){

		if(source == null){						
			if(Params.testMode && !adjList.datatypesProp.contains(src))
				throw new Error("Unknown entity " + src + " mapped with " + trg);
			else
				return false;
		}					
		if(target == null){
			if(Params.testMode && !adjList.datatypesProp.contains(trg))
				throw new Error("Unknown entity " + trg + " mapped with " + src);
			else
				return false;
		}

		return true;
	}

	private Model rdfMappingToModel(File mapping){
		// create an empty model
		Model model = ModelFactory.createDefaultModel();

		// use the FileManager to find the input file
		InputStream in = FileManager.get().open(mapping.getAbsolutePath());
		if (in == null) {
			throw new IllegalArgumentException(
					"File: " + mapping.getAbsolutePath() + " not found");
		}

		// read the RDF/XML file
		String URIbaseNoSharp = "http://knowledgeweb.semanticweb.org/heterogeneity/alignment";
		model.read(in, URIbaseNoSharp);

		return model;
	}


	public Set<LightEdge> parse(File mapping, LightAdjacencyList adjList, 
			boolean loadOnly) throws IOException {
		FileUtil.writeLogAndConsole("Loading RDF mapping file " + mapping.getName());
		//		if(mapping.getName().compareTo("CODI-anatomy-track1.rdf") == 0)
		//			System.out.println();
		long start = Util.getMSec();

		if(adjList == null)
			loadOnly = true;

		Set<LightEdge> mappings = new HashSet<LightEdge>();

		Model model = rdfMappingToModel(mapping);

		String URIbaseNoSharp = "http://knowledgeweb.semanticweb.org/heterogeneity/alignment";
		String URIbase = model.getNsPrefixMap().containsKey("align") 
				? model.getNsPrefixMap().get("align") : model.getNsPrefixMap().get("");

				if(URIbase == null)
					URIbase = "http://knowledgeweb.semanticweb.org/heterogeneity/alignment#";

				String relation = null, src = null, trg = null;
				double measure = 1;

				StmtIterator s = model.listStatements();
				int nulls = 0;
				
				while(s.hasNext()){
					LightEdge m1 = null, m2 = null;
					Statement st = s.next();
					Triple triple = st.asTriple();

					if(triple.getPredicate().hasURI(URIbase + "relation") 
							|| triple.getPredicate().hasURI(URIbaseNoSharp + "relation"))
						relation = triple.getObject().toString();
					else if(triple.getPredicate().hasURI(URIbase + "entity2") 
							|| triple.getPredicate().hasURI(URIbaseNoSharp + "entity2")){
						//trg = (trg = triple.getObject().toString()).substring(trg.lastIndexOf('#')+1);
						trg = triple.getObject().getURI().toString();
					}
					else if(triple.getPredicate().hasURI(URIbase + "measure") 
							|| triple.getPredicate().hasURI(URIbaseNoSharp + "measure")){
						try {
							measure = Double.parseDouble(triple.getObject().getLiteralLexicalForm());//.getLiteralValue().toString());
						}catch(NumberFormatException exc){
							exc.printStackTrace();
						}
					}
					else if(triple.getPredicate().hasURI(URIbase + "entity1") 
							|| triple.getPredicate().hasURI(URIbaseNoSharp + "entity1")){
						
						//src = (src = triple.getObject().toString()).substring(src.lastIndexOf('#')+1);
						src = (triple.getObject().getURI().toString());
						
						//				System.out.println(src + " " + relation + " " + trg);
						LightNode source = null, target = null, sourceRev = null, targetRev = null;

						if(adjList != null){
//							source = adjList.getNodeFromName("1_" + src);
//							target = adjList.getNodeFromName("2_" + trg);
//							sourceRev = adjList.getNodeFromName("2_" + src);
//							targetRev = adjList.getNodeFromName("1_" + trg);

							source = adjList.tryToGetNodeFromIRI(src,true,
									ENTITY_KIND.CLASS);
							target = adjList.tryToGetNodeFromIRI(trg,false,
									ENTITY_KIND.CLASS);
							sourceRev = adjList.tryToGetNodeFromIRI(src,false,
									ENTITY_KIND.CLASS);
							targetRev = adjList.tryToGetNodeFromIRI(trg,true,
									ENTITY_KIND.CLASS);
							
							if(source == null || target == null){
								source = sourceRev;
								target = targetRev;
							}

							if(!checkElements(src,trg,source,target,sourceRev,targetRev,adjList))
								continue;
						}

						if(relation.substring(1,2).compareTo("<") == 0){
							if(!loadOnly)
								m1 = adjList.insertEdge(src, trg, true, false, measure, false);
							else 						
								m1 = new LightEdge(source, target, true, false, measure);
							nulls = addMappingIfNotNull(mappings, m1, nulls);
						}
						else if(relation.substring(1,2).compareTo(">") == 0){
							if(!loadOnly)
								m1 = adjList.insertEdge(trg, src, true, false, measure, true);
							else 
								m1 = new LightEdge(target, source, true, false, measure);

							nulls = addMappingIfNotNull(mappings, m1, nulls);
						}
						else if(relation.substring(1, 2).compareTo("=") == 0 
								|| relation.compareToIgnoreCase("\"fr.inrialpes.exmo.align.impl.rel.EquivRelation@50\"") == 0){
							if(!loadOnly){
								m1 = adjList.insertEdge(src, trg, true, false, measure, false);
								m2 = adjList.insertEdge(trg, src, true, false, measure, true);
							}
							else {
								m1 = new LightEdge(source, target, true, false, measure);
								m2 = new LightEdge(target, source, true, false, measure);
							}
							if(m1 != null && m2 != null){
								addMappingIfNotNull(mappings, m1, nulls);
								addMappingIfNotNull(mappings, m2, nulls);
							}
						}
					}
				}

				//System.out.println("Printing whole RDF file");
				//model.write(System.out);

				FileUtil.writeLogAndConsole(mappings.size() + " mappings loaded in " 
						+ (Util.getDiffmsec(start)) + "ms");

				if(Params.testMode && adjList != null)
					adjList.checkDataStructureConsistency();

				return mappings;
	}

}
