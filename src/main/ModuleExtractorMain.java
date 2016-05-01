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
package main;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import enumerations.REASONER_KIND;

import uk.ac.manchester.cs.owlapi.modularity.ModuleType;
import uk.ac.manchester.syntactic_locality.ModuleExtractor;
import util.OntoUtil;
import util.Util;

public class ModuleExtractorMain {

	/**
	 * @param args
	 * @throws OWLOntologyCreationException 
	 * @throws OWLOntologyStorageException 
	 */
	public static void main(String[] args) throws OWLOntologyCreationException, OWLOntologyStorageException {
		OWLOntologyManager manager = OntoUtil.getManager(false);
		OWLOntology o1 = OntoUtil.load("/home/ale/svn/Conservativity/Papers/DL2014/example/onto1.owl", true, manager);
		OWLOntology o2 = OntoUtil.load("/home/ale/svn/Conservativity/Papers/DL2014/example/onto2.owl", true, manager);
		
		OWLReasoner r1 = OntoUtil.getReasoner(o1, REASONER_KIND.HERMIT, manager);
		OWLReasoner r2 = OntoUtil.getReasoner(o2, REASONER_KIND.HERMIT, manager);
		List<OWLReasoner> reasoners = new LinkedList<>();
		
		reasoners.add(r1);
		reasoners.add(r2);
		
		OntoUtil.ontologyClassification(false, false, reasoners, false);
		r1 = reasoners.get(0);
		r2 = reasoners.get(1);
		
		OntoUtil.saveClassificationAxioms(o1, r1, manager);
		OntoUtil.saveClassificationAxioms(o2, r2, manager);
		
		String path = "/home/ale/svn/Conservativity/Papers/DL2014/example/aligned_noimports.owl";
		OWLOntology o = OntoUtil.load(path, true, manager);
		OntoUtil.addAxiomsToOntology(o, manager, o1.getAxioms(), true);
		OntoUtil.addAxiomsToOntology(o, manager, o2.getAxioms(), true);

		Set<OWLEntity> seed = new HashSet<>();
		List<OWLClass> list = new LinkedList<>();
		
		list.add(OntoUtil.getDataFactory().getOWLClass(IRI.create("http://onto1.owl#Company")));
//		list.add(OntoUtil.getDataFactory().getOWLClass(IRI.create("http://onto2.owl#Company")));
//		list.add(OntoUtil.getDataFactory().getOWLClass(IRI.create("http://onto2.owl#Field_operator")));
//		list.add(OntoUtil.getDataFactory().getOWLClass(IRI.create("http://onto1.owl#AppraisalWellBore")));
//		list.add(OntoUtil.getDataFactory().getOWLClass(IRI.create("http://onto2.owl#Exploration_borehole")));
		
		System.out.println("Classes in Signature: " + list);
		OWLReasoner r = OntoUtil.getReasoner(o, REASONER_KIND.HERMIT, manager);
		
		for (int c = 0; c < list.size(); c++) {
			seed.add(list.get(c));
			extractPrintOWLAPI(manager,r,seed,1000+c);
			seed.clear();
		}
		
////		ModuleExtractor extractor = 
////				new ModuleExtractor(o, true, true, false, false, true);
////		extractPrint(extractor, seed);
//		seed.clear();
//		list = new LinkedList<>(o.getClassesInSignature());
//		
//		int i = 0;
//		while(list.size() > 1){
//			System.out.println("Iteration " + ++i);
//			seed.addAll(list = Util.selectPercentage(list, 50));			
////			seed.addAll(list = Util.randomSample(list,10));
////			extractPrint(extractor, seed);
//			extractPrintOWLAPI(manager,r,seed,i);
//			seed.clear();
//		}
	}
	
	private static void extractPrintOWLAPI(OWLOntologyManager manager, OWLReasoner r, Set<OWLEntity> seed, int i) throws OWLOntologyCreationException, OWLOntologyStorageException{
		System.out.println("Seed (size " + seed.size() + "): " + seed);
		OWLOntology mod = OntoUtil.extractModule(manager, r, seed, "-"+i, ModuleType.BOT);
		
		for (OWLAxiom ax : mod.getLogicalAxioms()) {
			System.out.println(ax);
		}
	}
	
	private static void extractPrint(ModuleExtractor extractor, Set<OWLEntity> seed){
		System.out.println("Seed (size " + seed.size() + "): " + seed);
		Set<OWLAxiom> module = extractor.extractModuleAxiomsForGroupSignature(seed);		
		for (OWLAxiom ax : module) {
			if(!ax.getAxiomType().equals(AxiomType.DECLARATION))
				System.out.println(ax);
		}
	}
}
