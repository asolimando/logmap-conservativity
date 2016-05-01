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
package ontology;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owl.explanation.api.ExplanationGenerator;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import util.FileUtil;
import util.OntoUtil;
import util.Params;

import auxStructures.Pair;

import org.semanticweb.owl.explanation.api.ExplanationProgressMonitor;

public class UnsatExplanationProgressMonitor implements ExplanationProgressMonitor<OWLAxiom> {

	private int count;
	private Map<OWLAxiom, Double> mappingsMap;
	private boolean suppressOutput;
	private OWLAxiom repair;
	private double minConf = Float.MAX_VALUE;
		
	public UnsatExplanationProgressMonitor(Map<OWLAxiom, Double> mappingsMap, 
			boolean suppressOutput){
		this.mappingsMap = new HashMap<>(mappingsMap);
		this.suppressOutput = suppressOutput;
	}
	
	@Override
	public boolean isCancelled(){
//		System.out.println("Test cancelled: " + repair);
		return repair != null;
	}
	
	@Override
	public void foundExplanation(ExplanationGenerator<OWLAxiom> generator,
			Explanation<OWLAxiom> expl, Set<Explanation<OWLAxiom>> allFoundExplanations) {

		if(!suppressOutput)
			FileUtil.writeLogAndConsole("\tProcessing explanation " + ++count 
				+ " (size " + expl.getSize() + "):\n\t\t" + expl);

		Set<OWLAxiom> overlap = new HashSet<>();
		
		for (OWLAxiom ax : expl.getAxioms()) {
			if(ax.getAxiomType().equals(AxiomType.EQUIVALENT_CLASSES)){
				overlap.addAll(((OWLEquivalentClassesAxiom) ax).asPairwiseAxioms());
			}
			else { //if(ax.getAxiomType().equals(AxiomType.SUBCLASS_OF){
				overlap.add(ax);
			}
		}
		
		overlap.retainAll(mappingsMap.keySet());
		
		for (OWLAxiom ax : overlap) {
			if(minConf > mappingsMap.get(ax)){
				repair = ax;
				minConf = mappingsMap.get(ax);
			}
		}
	}
	
	public OWLAxiom getRepair(){
		return repair;
	}
	
	public double getRepairCost(){
		return minConf;
	}
}