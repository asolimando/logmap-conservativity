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
package visitor.disjToConj;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import uk.ac.ox.krr.logmap2.indexing.JointIndexManager;
import util.FileUtil;
import util.OntoUtil;
import util.Params;
import util.Util;

public class ClassificationOverapproximator {

	public static OWLOntologyManager manager = OntoUtil.getManager(true);
	public static Map<OWLClassExpression, OWLClassExpression> map = new HashMap<>();
	public static Set<OWLAxiom> toAdd = new HashSet<>();
	public static Set<OWLAxiom> toDelete = new HashSet<>();
	public static Set<OWLAxiom> toRetract = new HashSet<>();
	public static OWLOntology onto = null;
	private boolean fullApprox = false;
	
	public static void clearStructures(){
		map.clear();
		toDelete.clear();
		toAdd.clear();
		toRetract.clear();
		manager = OntoUtil.getManager(true);
		onto = null;
	}
	
	/**
	 * @param args
	 * @throws OWLOntologyCreationException 
	 * @throws IOException 
	 * @throws OWLOntologyStorageException 
	 */
	public static void main(String[] args) throws OWLOntologyCreationException, 
		OWLOntologyStorageException, IOException {
		String ontoFile = "data/cton.owl";
		boolean direct = true;
		boolean forComparison = false;
		
		if(Params.verbosity > 1)
			FileUtil.writeLogAndConsole("Processing " + ontoFile);
		
		OWLOntology onto = OntoUtil.load(ontoFile, direct, manager);
		if(!forComparison)
			OntoUtil.save(onto, ontoFile.replace("/", "/reduced/"), manager);
		
		computeApproximation(onto, null);
		
		OntoUtil.save(onto, ontoFile.replace("/", "/reduced/").replace(".","_red."), 
				manager);
	}
	
	public static void computeApproximation(OWLOntology onto, 
			JointIndexManager index){
		
		clearStructures();
		long start = Util.getMSec();
		Params.verbosity = 0;
		ClassificationOverapproximator.onto = onto;
		
		OWLAxiomOverapproximationVisitor axVisitor = 
				new OWLAxiomOverapproximationVisitor(
						manager.getOWLDataFactory(),false);
		
		for (OWLAxiom ax : onto.getTBoxAxioms(true)) {
			OWLAxiom newAx = ax.accept(axVisitor);
			if(!ax.equals(newAx)){
				toDelete.add(ax);
				toAdd.add(newAx);
				if(Params.verbosity > 0){
					FileUtil.writeLogAndConsole("\nPre: " + ax);
					FileUtil.writeLogAndConsole("\nPost: " + newAx);
				}
			}
		}
		// all the (now) unsafe disjoint axioms
		toDelete.addAll(toRetract);
		//TODO: to be tested
		// we also enforce this for superclasses of disjoint axioms
//		if(index != null){
//			for (OWLAxiom ax : toRetract) {
//				OWLDisjointClassesAxiom disjAx = (OWLDisjointClassesAxiom) ax;
//				List<OWLClassExpression> exps = disjAx.getClassExpressionsAsList();
//				int id1 = -1, id2 = -1;
//				for (Integer i : index.getClassIdentifierSet()) {
//					if(index.getOWLClass4ConceptIndex(i).equals(exps.get(0))){
//						id1 = i;
//					}
//					else if(index.getOWLClass4ConceptIndex(i).equals(exps.get(1))){
//						id2 = i;
//					}
//					if(id1 != -1 && id2 != -1)
//						break;
//				}
//				
//				if(id1 == -1){
//					//System.err.println(exps.get(0) + " not found");
//					continue;
//				}
//				else if(id2 == -1){
//					//System.err.println(exps.get(1) + " not found");
//					continue;					
//				}
//				
//				OWLClass supC1, supC2;
//				for (Integer sup1 : index.getSubsetOfSuperClasses4Identifier(id1)) {
//					supC1 = index.getOWLClass4ConceptIndex(sup1);
//					for (Integer sup2 : index.getSubsetOfSuperClasses4Identifier(id2)) {
//						supC2 = index.getOWLClass4ConceptIndex(sup2);
//						for (OWLDisjointClassesAxiom djA : onto.getDisjointClassesAxioms(supC1)) {
//							if(djA.getClassExpressions().size() != 2){
//								ClassificationOverapproximator.toDelete.add(djA);
//								for (OWLDisjointClassesAxiom axDPair : djA.asPairwiseAxioms()) {
//									if(!axDPair.contains(supC2)){
//										ClassificationOverapproximator.toAdd.add(axDPair);
//									}
//								}
//							}
//							else {
//								if(!djA.contains(supC2)){
//									ClassificationOverapproximator.toDelete.add(djA);
//								}
//							}
//						}
//					}
//				}
//			}
//		}
		
		FileUtil.writeLogAndConsole(toAdd.size() + "/" + onto.getTBoxAxioms(true).size());
		FileUtil.writeLogAndConsole(toAdd.toString().replace(", ", ",\n"));
		List<OWLOntologyChange> changes = manager.removeAxioms(onto, toDelete);
		changes.addAll(manager.addAxioms(onto, toAdd));
		manager.applyChanges(changes);
		
		FileUtil.writeLogAndConsole("Approximation Time: " + Util.getDiffmsec(start) + "ms");
	}
}
