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
package visitor.structuralreduction;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitor;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import uk.ac.manchester.cs.owl.owlapi.OWLDataPropertyDomainAxiomImpl;
import util.OntoUtil;
import util.Params;
import util.Util;

public class StructuralReducer {

	public static OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	public static OWLDataFactory dataFactory = OWLManager.getOWLDataFactory();
	public static Map<OWLClassExpression, OWLClass> map = new HashMap<OWLClassExpression, OWLClass>();
	public static Stack<OWLClassExpression> stack = new Stack<OWLClassExpression>();
	public static Set<OWLAxiom> toDelete = new HashSet<OWLAxiom>();
	public static Set<OWLAxiom> toAdd = new HashSet<OWLAxiom>();
	public static OWLOntology onto = null;
	
//	/**
//	 * @param args
//	 * @throws OWLOntologyCreationException 
//	 * @throws IOException 
//	 * @throws OWLOntologyStorageException 
//	 */
//	public static void main(String[] args) throws OWLOntologyCreationException, 
//		OWLOntologyStorageException, IOException {
//		String ontoFile = "data/cton.owl";
//		boolean direct = true;
//		boolean forComparison = false;
//		
//		if(Params.verbosity > 1)
//			System.out.println("Processing " + ontoFile);
//		
//		OWLOntology onto = OntoUtil.load(ontoFile, direct, manager);
//		if(!forComparison)
//			OntoUtil.save(onto, ontoFile.replace("/", "/reduced/"), manager);
//		
//		long start = System.nanoTime();
//		applyStructuralReduction(onto);
//		System.out.println("Took " + (System.nanoTime() - start) / 1000000 + "ms");
//		OntoUtil.save(onto, ontoFile.replace("/", "/reduced/").replace(".","_red."), 
//				manager);
//	}
	
	public static void applyStructuralReduction(OWLOntology onto){
		StructuralReducer.onto = onto;
		OWLAxiomReducerVisitor axVisitor = new OWLAxiomReducerVisitor();
		
		for (OWLAxiom ax : onto.getTBoxAxioms(false)) {
			System.out.println("\n" + ax);
			ax.accept(axVisitor);									
		}

		System.out.println(toAdd.size() + "/" + onto.getTBoxAxioms(false).size());
		
		OWLClassExpression ce = null;
		OWLClassExpressionVisitor visitor = new OWLClassExpressioneReducerVisitor();
		int b = 0;
		System.out.println("Initial stack " + stack.size());
		
		while(!stack.isEmpty()){
			ce = stack.pop();
			ce.accept(visitor);
			OWLClass newClass = map.containsKey(ce) ? map.get(ce) : 
				OntoUtil.createFreshClass(onto, dataFactory, manager);
			
			toAdd.add(dataFactory.getOWLEquivalentClassesAxiom(newClass, ce));
			++b;
		}
		System.out.println(b);
		
		List<OWLOntologyChange> changes = manager.removeAxioms(onto, toDelete);
		changes.addAll(manager.addAxioms(onto, toAdd));
		manager.applyChanges(changes);
	}
}
