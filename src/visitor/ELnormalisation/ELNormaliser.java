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
package visitor.ELnormalisation;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.semanticweb.owlapi.AmalgamateSubClassAxioms;
import org.semanticweb.owlapi.ConvertEquivalentClassesToSuperClasses;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.profiles.OWL2ELProfile;
import org.semanticweb.owlapi.profiles.OWL2Profile;
import org.semanticweb.owlapi.profiles.OWLProfileReport;
import org.semanticweb.owlapi.profiles.OWLProfileViolation;

import uk.ac.manchester.cs.owl.owlapi.OWLDeclarationAxiomImpl;
import util.OntoUtil;
import util.Params;
import util.Util;

public class ELNormaliser {

	public static OWLOntologyManager manager = OntoUtil.getManager(false);
	public static OWLDataFactory dataFactory = OntoUtil.getDataFactory();
	public OWLOntology normalisedOnto;

	private Map<OWLClassExpression, OWLClass> map;
	private Set<OWLClass> classesLHS;

	private Set<OWLAxiom> axBuffer;

	public ELNormaliser(){
		map = new HashMap<OWLClassExpression, OWLClass>();
		classesLHS = new HashSet<>();
		axBuffer = new HashSet<>();
	}

	/**
	 * @param args
	 * @throws OWLOntologyCreationException 
	 * @throws IOException 
	 * @throws OWLOntologyStorageException 
	 */
	public static void main(String[] args) throws OWLOntologyCreationException, 
	OWLOntologyStorageException, IOException {

		String ontoFile = Params.dataFolder + "oaei2012/conference/onto/confious.owl";
//		String ontoFile = Params.dataFolder + 
//						"oaei2012/largebio/onto/oaei2012_SNOMED_extended_overlapping_fma_nci.owl";
//		String ontoFile = Params.dataFolder + 
//				"oaei2012/largebio/onto/oaei2012_FMA_extended_overlapping_snomed.owl";
//		String ontoFile = Params.dataFolder + 
//				"oaei2012/anatomy/onto/human.owl";
//		String ontoFile = Params.dataFolder + 
//				"oaei2012/anatomy/onto/mouse.owl";
		boolean direct = false;
		boolean forComparison = false;

		if(Params.verbosity > 1)
			System.out.println("Processing " + ontoFile);

		OWLOntology onto = OntoUtil.load("file://" + ontoFile, direct, manager);
		//		if(!forComparison)
		//			OntoUtil.save(onto, ontoFile.replace("/", "/reduced/"), manager);

		ELNormaliser norm = new ELNormaliser();
		long start = Util.getMSec();
		OWLOntology normOnto = norm.applyELNormalisation(onto);
		System.out.println(normOnto + " in " + Util.getDiffmsec(start) + "(ms)");

		if(norm.checkNormalisation(normOnto))
			norm.isUnfoldable(normOnto);
		else
			System.out.println("Normalisation error");

		//		OntoUtil.save(onto, ontoFile.replace("/", "/reduced/").replace(".","_red."), 
		//				manager);
	}

	public OWLOntology applyELNormalisation(OWLOntology onto){

		OWLProfileReport report;

		//		report = OntoUtil.checkELProfile(onto);
		//		Set<OWLAxiom> invalidAxioms = new HashSet<>();
		//		
		//		if(!report.isInProfile())
		//			for (OWLProfileViolation viol : report.getViolations()) {
		//				invalidAxioms.add(viol.getAxiom());
		//				System.out.println(viol);
		//			}

		try {
			normalisedOnto = manager.createOntology(IRI.create(
					onto.getOntologyID().getOntologyIRI().toString() + "/EL"));
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
			return null;
		}

		Set<OWLAxiom> normAxioms = new HashSet<>();

		OWLAxiom tmpAx = null;

		AmalgamateSubClassAxioms amalgSubChange = new AmalgamateSubClassAxioms(
				Collections.singleton(onto),dataFactory);
		System.out.println("Subclass amalgamation changes: " + 
				manager.applyChanges(amalgSubChange.getChanges()).size());

		boolean isNormOk = false;

		for (OWLAxiom ax : onto.getAxioms()) {

			//			if(invalidAxioms.contains(ax)){
			//				System.out.println("Invalid, skip it");
			//				continue;
			//			}

			if(ax.isOfType(AxiomType.EQUIVALENT_CLASSES)){
				ax = ax.getNNF();

				for (OWLAxiom eqAx : 
					((OWLEquivalentClassesAxiom)ax).asPairwiseAxioms()) {
					tmpAx = normaliseAxiom(eqAx);
					if(isNormOk = tmpAx != null)
						normAxioms.add(tmpAx);

					flushBufferizedAxioms(isNormOk);
				}
			}
			else if(ax.isOfType(AxiomType.SUBCLASS_OF)){				
				ax = ax.getNNF();
				OWLSubClassOfAxiom subAx = (OWLSubClassOfAxiom) ax;

				// combine A isA C_1, ..., A isA C_n => A isA C_1 and ... and C_n
				if(!subAx.isGCI()){
					if(classesLHS.contains(subAx.getSubClass().asOWLClass()))
						continue;

					Set<OWLClassExpression> operands = new HashSet<>();

					for (OWLSubClassOfAxiom otherSubAx : 
						onto.getSubClassAxiomsForSubClass(
								subAx.getSubClass().asOWLClass()))
						operands.add(otherSubAx.getSuperClass());

					classesLHS.add(subAx.getSubClass().asOWLClass());

					ax = dataFactory.getOWLSubClassOfAxiom(subAx.getSubClass(), 
							dataFactory.getOWLObjectIntersectionOf(operands));
				}

				tmpAx = normaliseAxiom(ax);
				if(isNormOk = tmpAx != null)
					normAxioms.add(tmpAx);

				flushBufferizedAxioms(isNormOk);
			}
			else if(ax.isOfType(AxiomType.DECLARATION)){
				OWLEntity e = ((OWLDeclarationAxiom)ax).getEntity();
				if(e.isOWLClass() || e.isOWLObjectProperty() 
						|| e.isOWLDataProperty())
					normAxioms.add(ax);
				//				else
				//					System.out.println(e.isBuiltIn() + " " + ax);
			}
		}

		OntoUtil.addAxiomsToOntology(normalisedOnto, manager, normAxioms, false);

		System.out.println("\nNormalised " + normAxioms.size() + " over " + 
				onto.getTBoxAxioms(true).size() + " axioms\n");

		report = OntoUtil.checkELProfile(normalisedOnto);

		if(!report.isInProfile())
			System.out.println(
					report.getViolations().toString().replace(", ", ",\n"));

		return normalisedOnto;
	}

	public OWLAxiom normaliseAxiom(OWLAxiom ax){
		OWLAxiomELNormVisitor axVisitor = new OWLAxiomELNormVisitor(this);
		OWLAxiom tmpAx = null;

		if(Params.verbosity > 1)
			System.out.print("\n" + ax + "\n  -->\n");
		tmpAx = ax.accept(axVisitor);

		if(Params.verbosity > 1){
			if(tmpAx != null)
				System.out.println(tmpAx);
			else
				System.out.println("discarded");
		}
		return tmpAx;
	}

	public boolean checkNormalisation(OWLOntology o){
		// check if atomic concepts are unique in LHS

		int count, violations = 0;
		Params.verbosity = 1;
		for (OWLClass c : o.getClassesInSignature(true)) {
			if((count = o.getSubClassAxiomsForSubClass(c).size()) > 1){
				if(Params.verbosity > 0)
					System.out.println(c + " appears as LHS in " + count 
							+ " subclassof axioms");
				violations++;
			}
			if((count = o.getEquivalentClassesAxioms(c).size()) > 1){
				if(Params.verbosity > 0)
					System.out.println(c + " appears in " + count 
							+ " equivalent classes axioms");
				violations++;
			}
		}

		for (OWLSubClassOfAxiom subAx : o.getAxioms(AxiomType.SUBCLASS_OF))
			if(subAx.isGCI())
				violations++;

		Params.verbosity = 0;
		if(violations > 0)
			System.out.println(violations + " violation(s) detected");

		return violations == 0;
	}

	public boolean isUnfoldable(OWLOntology o){
		
		Set<OWLClass> cycles = new HashSet<>();
		
		for (OWLClass c : o.getClassesInSignature(true)) {
			System.out.println(c);
			Set<OWLClassExpression> visited = new HashSet<>();
			Queue<OWLAxiom> q = new LinkedList<>();
			q.addAll(o.getAxioms(c));
			
			visited.add(c);
			OWLAxiom ax;
			
			while(!q.isEmpty()){
				ax = q.poll();
				
				if(//!ax.isOfType(AxiomType.SUBCLASS_OF) && 
						!ax.isOfType(AxiomType.EQUIVALENT_CLASSES))
					continue;

				System.out.println("\t"+ax);
					
				for (OWLClass d : ax.getClassesInSignature()) {
					if(visited.contains(d)){
						cycles.add(d);
					}
					else {
						visited.add(d);
						q.addAll(o.getDeclarationAxioms(d));
					}
				}
			}
		}
		
		if(!cycles.isEmpty())
			System.out.println(cycles);
		
		return cycles.isEmpty();
	}

	public OWLClassExpression introduceFreshNamedConcept(
			OWLClassExpression ce){
		OWLClass newClass; 

		if(ce.isClassExpressionLiteral() && !map.containsKey(ce))
			map.put(ce, ce.asOWLClass());

		newClass= map.containsKey(ce) ? map.get(ce) : 
			OntoUtil.createFreshClass(normalisedOnto, dataFactory, manager);

		OWLAxiom declAxiom = new OWLDeclarationAxiomImpl(newClass, 
				Collections.<OWLAnnotation> emptySet());

		OWLAxiom eqAx = dataFactory.getOWLEquivalentClassesAxiom(newClass, ce);

		axBuffer.add(declAxiom);
		axBuffer.add(eqAx);

		if(Params.verbosity > 1)
			System.out.println("\tFresh class: " + eqAx);

		return newClass;
	}

	private void flushBufferizedAxioms(boolean add){
		if(add)
			OntoUtil.addAxiomsToOntology(normalisedOnto, manager, axBuffer, true);
		axBuffer.clear();
	} 
}
