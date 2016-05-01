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

import java.util.HashSet;
import java.util.Iterator;
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

public class ViolationExplanationProgressMonitor implements ExplanationProgressMonitor<OWLAxiom> {

	private Pair<OWLClass> v;
	private Set<OWLClass> classSig;
	private int count;
	private boolean isDirect;
	private Set<OWLAxiom> subAxioms;
	private boolean suppressOutput;
	
	public ViolationExplanationProgressMonitor(OWLOntology inputOnto, 
			Pair<OWLClass> v, Set<OWLClass> classSig, boolean suppressOutput){
		this.classSig = classSig;
		this.v = v;
		this.subAxioms = new HashSet<>();
		this.suppressOutput = suppressOutput;
		
		for (OWLEquivalentClassesAxiom eqAx : 
			inputOnto.getAxioms(AxiomType.EQUIVALENT_CLASSES))
				subAxioms.addAll(eqAx.asOWLSubClassOfAxioms());
		
		subAxioms.addAll(inputOnto.getAxioms(AxiomType.SUBCLASS_OF));
		
		if(subAxioms.contains(OntoUtil.getSubClassOfAxiom(v)))
			isDirect = true;
	}

	@Override
	public boolean isCancelled() {
		return isDirect;
	}

	@Override
	public void foundExplanation(ExplanationGenerator<OWLAxiom> generator,
			Explanation<OWLAxiom> expl, Set<Explanation<OWLAxiom>> allFoundExplanations) {

		if(!suppressOutput)
			FileUtil.writeLogAndConsole("\t\tProcessing explanation " + ++count 
				+ " (size " + expl.getSize() + ")");

		OWLClass nextSrc =  v.getFirst(), 
				nextDst = v.getSecond();

		if(expl.getSize() == 1 || expl.isJustificationEntailment()){
			// otherwise it was in the inference closure, not valid 
			
//			if(subAxioms.contains(expl.getEntailment())){
				if(!suppressOutput)
					FileUtil.writeLogAndConsole("\t" + expl.toString());
				isDirect = true;
				return;
//			}
//			return;
		}

		Set<OWLAxiom> axioms = new HashSet<>(expl.getAxioms());
		ext : 
			while(true){
				OWLAxiom ax;
				Iterator<OWLAxiom> itr = axioms.iterator();

				while(itr.hasNext()){
					ax = itr.next();
					if(!ax.getAxiomType().equals(AxiomType.SUBCLASS_OF))
						break ext;

					OWLSubClassOfAxiom subAx = (OWLSubClassOfAxiom) ax;
					OWLClassExpression subCE = subAx.getSubClass(),
							supCE = subAx.getSuperClass();

					if(subCE.isAnonymous() || !subCE.isClassExpressionLiteral() || 
							subCE.isBottomEntity() || subCE.isTopEntity())
						break ext;
					if(supCE.isAnonymous() || !supCE.isClassExpressionLiteral() 
							|| supCE.isBottomEntity() || supCE.isTopEntity())
						break ext;

					OWLClass subC = subCE.asOWLClass(), 
							supC = supCE.asOWLClass();

					if(!v.getFirst().equals(subC) 
							&& classSig.contains(subC) 
							|| !v.getSecond().equals(supC) 
							&& classSig.contains(supC))
						break ext;				

//					if(!classSig.contains(subC) 
//							&& !classSig.contains(supC) 
//								&& !subAxioms.contains(subAx))
//						break ext;
					
					if(subC.equals(nextSrc)){
						if(supC.equals(nextDst)){
							if(!suppressOutput)
								FileUtil.writeLogAndConsole("\t" 
										+ expl.toString());
							isDirect = true;
							return;
						}
						nextSrc = supC;
						itr.remove();
					}

					if(supC.equals(nextDst)){
						nextDst = subC;
						itr.remove();
					}
				}
			}
//		if(count == Params.maxExplanationsForDirectViol)
//			isDirect = true;
	}

	public boolean isDirect() {
		return isDirect;
	}
}