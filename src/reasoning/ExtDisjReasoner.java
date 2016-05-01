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
package reasoning;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.elk.owlapi.ElkReasoner;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.owllink.OWLlinkHTTPXMLReasoner;
import org.semanticweb.owlapi.owllink.OWLlinkReasoner;
import org.semanticweb.owlapi.reasoner.AxiomNotInProfileException;
import org.semanticweb.owlapi.reasoner.BufferingMode;
import org.semanticweb.owlapi.reasoner.ClassExpressionNotInProfileException;
import org.semanticweb.owlapi.reasoner.FreshEntitiesException;
import org.semanticweb.owlapi.reasoner.FreshEntityPolicy;
import org.semanticweb.owlapi.reasoner.InconsistentOntologyException;
import org.semanticweb.owlapi.reasoner.IndividualNodeSetPolicy;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.ReasonerInterruptedException;
import org.semanticweb.owlapi.reasoner.ReasonerProgressMonitor;
import org.semanticweb.owlapi.reasoner.TimeOutException;
import org.semanticweb.owlapi.reasoner.UnsupportedEntailmentTypeException;
import org.semanticweb.owlapi.reasoner.impl.OWLClassNode;
import org.semanticweb.owlapi.reasoner.impl.OWLClassNodeSet;
import org.semanticweb.owlapi.util.Version;

import enumerations.REASONER_KIND;

import util.FileUtil;
import util.OntoUtil;
import util.Params;

public class ExtDisjReasoner implements OWLReasoner {

	private Version v; 
	private OWLReasoner reasoner;
	private REASONER_KIND kind;
	private Map<OWLClassExpression, Set<Node<OWLClass>>> disjointClasses = new HashMap<>();

	public ExtDisjReasoner(OWLReasoner reasoner) {
		this.reasoner = reasoner;
		
		setReasonerDetails();

		OWLOntology o = reasoner.getRootOntology();
		int disjAxNum = 0;
		for (OWLDisjointClassesAxiom disjAx : o.getAxioms(AxiomType.DISJOINT_CLASSES)) {
			++disjAxNum;
			for (OWLClassExpression ce : disjAx.getClassExpressionsAsList()) {
				if(ce.isAnonymous() || ce.isBottomEntity() || ce.isTopEntity())
					continue;

				if(!disjointClasses.containsKey(ce))
					disjointClasses.put(ce, new HashSet<Node<OWLClass>>());

				for (OWLClassExpression ce2 : disjAx.getClassExpressionsMinus(ce)) {
					if(ce2.isAnonymous() || ce2.isBottomEntity() || ce2.isTopEntity())
						continue;

					OWLClassNode clsNode = new OWLClassNode();
					clsNode.add(ce.asOWLClass());
					disjointClasses.get(ce).add(clsNode);
				}
				
				// only class expressions, we remove the empty set...
				if(disjointClasses.get(ce).isEmpty())
					disjointClasses.remove(ce);
			}
		}
		FileUtil.writeLogAndConsole("Extended " + kind + " disjoint info: " 
				+ disjointClasses.size() + " using " + disjAxNum + " axioms");
	}

	public String getReasonerName() {
		return "Extended " + kind;
	}

	public Version getReasonerVersion() {
		return v;
	}
	
	private void setReasonerDetails(){
		kind = REASONER_KIND.getKind(reasoner);
		
		if(Params.oaei)
			v = new Version(0,0,0,0);
		else {
			if(kind.equals(REASONER_KIND.ELK) || kind.equals(REASONER_KIND.ELKTRACE))
				v = new Version(0,4,1,0);
			
			else if(kind.equals(REASONER_KIND.UNKNOWN) 
					&& reasoner.getReasonerVersion() == null)
				v = new Version(0,0,0,0);
			
			else
				v = reasoner.getReasonerVersion();
		}
	}

	// we reply with the direct disjoint axioms
	public NodeSet<OWLClass> getDisjointClasses(OWLClassExpression ce)
			throws ReasonerInterruptedException, TimeOutException,
			FreshEntitiesException, InconsistentOntologyException {
		if(disjointClasses.containsKey(ce))
			return new OWLClassNodeSet(disjointClasses.get(ce));
		return new OWLClassNodeSet();
	}


	public Node<OWLObjectPropertyExpression> getTopObjectPropertyNode() {
		return reasoner.getTopObjectPropertyNode();
	}


	public Node<OWLObjectPropertyExpression> getBottomObjectPropertyNode() {
		return reasoner.getBottomObjectPropertyNode();
	}


	public NodeSet<OWLObjectPropertyExpression> getSubObjectProperties(
			OWLObjectPropertyExpression pe, boolean direct)
					throws InconsistentOntologyException, FreshEntitiesException,
					ReasonerInterruptedException, TimeOutException {
		return reasoner.getSubObjectProperties(pe, direct);
	}


	public NodeSet<OWLObjectPropertyExpression> getSuperObjectProperties(
			OWLObjectPropertyExpression pe, boolean direct)
					throws InconsistentOntologyException, FreshEntitiesException,
					ReasonerInterruptedException, TimeOutException {
		return reasoner.getSuperObjectProperties(pe, direct);
	}


	public Node<OWLObjectPropertyExpression> getEquivalentObjectProperties(
			OWLObjectPropertyExpression pe)
					throws InconsistentOntologyException, FreshEntitiesException,
					ReasonerInterruptedException, TimeOutException {
		return reasoner.getEquivalentObjectProperties(pe);
	}


	public NodeSet<OWLObjectPropertyExpression> getDisjointObjectProperties(
			OWLObjectPropertyExpression pe)
					throws InconsistentOntologyException, FreshEntitiesException,
					ReasonerInterruptedException, TimeOutException {
		return reasoner.getDisjointObjectProperties(pe);
	}


	public Node<OWLObjectPropertyExpression> getInverseObjectProperties(
			OWLObjectPropertyExpression pe)
					throws InconsistentOntologyException, FreshEntitiesException,
					ReasonerInterruptedException, TimeOutException {
		return reasoner.getInverseObjectProperties(pe);
	}


	public NodeSet<OWLClass> getObjectPropertyDomains(
			OWLObjectPropertyExpression pe, boolean direct)
					throws InconsistentOntologyException, FreshEntitiesException,
					ReasonerInterruptedException, TimeOutException {
		return reasoner.getObjectPropertyDomains(pe, direct);
	}


	public NodeSet<OWLClass> getObjectPropertyRanges(
			OWLObjectPropertyExpression pe, boolean direct)
					throws InconsistentOntologyException, FreshEntitiesException,
					ReasonerInterruptedException, TimeOutException {
		return reasoner.getObjectPropertyRanges(pe, direct);
	}


	public Node<OWLDataProperty> getTopDataPropertyNode() {
		return reasoner.getTopDataPropertyNode();
	}


	public Node<OWLDataProperty> getBottomDataPropertyNode() {
		return reasoner.getBottomDataPropertyNode();
	}


	public NodeSet<OWLDataProperty> getSubDataProperties(OWLDataProperty pe,
			boolean direct) throws InconsistentOntologyException,
			FreshEntitiesException, ReasonerInterruptedException,
			TimeOutException {
		return reasoner.getSubDataProperties(pe, direct);
	}


	public NodeSet<OWLDataProperty> getSuperDataProperties(OWLDataProperty pe,
			boolean direct) throws InconsistentOntologyException,
			FreshEntitiesException, ReasonerInterruptedException,
			TimeOutException {
		return reasoner.getSuperDataProperties(pe, direct);
	}


	public Node<OWLDataProperty> getEquivalentDataProperties(OWLDataProperty pe)
			throws InconsistentOntologyException, FreshEntitiesException,
			ReasonerInterruptedException, TimeOutException {
		return reasoner.getEquivalentDataProperties(pe);
	}


	public NodeSet<OWLDataProperty> getDisjointDataProperties(
			OWLDataPropertyExpression pe) throws InconsistentOntologyException,
			FreshEntitiesException, ReasonerInterruptedException,
			TimeOutException {
		return reasoner.getDisjointDataProperties(pe);
	}


	public NodeSet<OWLClass> getDataPropertyDomains(OWLDataProperty pe,
			boolean direct) throws InconsistentOntologyException,
			FreshEntitiesException, ReasonerInterruptedException,
			TimeOutException {
		return reasoner.getDataPropertyDomains(pe, direct);
	}


	public NodeSet<OWLClass> getTypes(OWLNamedIndividual ind, boolean direct)
			throws InconsistentOntologyException, FreshEntitiesException,
			ReasonerInterruptedException, TimeOutException {
		return reasoner.getTypes(ind, direct);
	}


	public NodeSet<OWLNamedIndividual> getInstances(OWLClassExpression ce,
			boolean direct) throws InconsistentOntologyException,
			ClassExpressionNotInProfileException, FreshEntitiesException,
			ReasonerInterruptedException, TimeOutException {
		return reasoner.getInstances(ce, direct);
	}


	public NodeSet<OWLNamedIndividual> getObjectPropertyValues(
			OWLNamedIndividual ind, OWLObjectPropertyExpression pe)
					throws InconsistentOntologyException, FreshEntitiesException,
					ReasonerInterruptedException, TimeOutException {
		return reasoner.getObjectPropertyValues(ind, pe);
	}


	public Set<OWLLiteral> getDataPropertyValues(OWLNamedIndividual ind,
			OWLDataProperty pe) throws InconsistentOntologyException,
			FreshEntitiesException, ReasonerInterruptedException,
			TimeOutException {
		return reasoner.getDataPropertyValues(ind, pe);
	}


	public Node<OWLNamedIndividual> getSameIndividuals(OWLNamedIndividual ind)
			throws InconsistentOntologyException, FreshEntitiesException,
			ReasonerInterruptedException, TimeOutException {
		return reasoner.getSameIndividuals(ind);
	}


	public NodeSet<OWLNamedIndividual> getDifferentIndividuals(
			OWLNamedIndividual ind) throws InconsistentOntologyException,
			FreshEntitiesException, ReasonerInterruptedException,
			TimeOutException {
		return reasoner.getDifferentIndividuals(ind);
	}


	public long getTimeOut() {
		return reasoner.getTimeOut();
	}


	public FreshEntityPolicy getFreshEntityPolicy() {
		return reasoner.getFreshEntityPolicy();
	}


	public IndividualNodeSetPolicy getIndividualNodeSetPolicy() {
		return reasoner.getIndividualNodeSetPolicy();
	}

	public void dispose() {
		reasoner.dispose();
	}

	@Override
	public BufferingMode getBufferingMode() {
		return reasoner.getBufferingMode();
	}

	@Override
	public void flush() {
		reasoner.flush();
	}

	@Override
	public List<OWLOntologyChange> getPendingChanges() {
		return reasoner.getPendingChanges();
	}

	@Override
	public Set<OWLAxiom> getPendingAxiomAdditions() {
		return reasoner.getPendingAxiomAdditions();
	}

	@Override
	public Set<OWLAxiom> getPendingAxiomRemovals() {
		return reasoner.getPendingAxiomRemovals();
	}

	@Override
	public OWLOntology getRootOntology() {
		return reasoner.getRootOntology();
	}

	@Override
	public void interrupt() {
		reasoner.interrupt();
	}

	@Override
	public void precomputeInferences(InferenceType... inferenceTypes)
			throws ReasonerInterruptedException, TimeOutException,
			InconsistentOntologyException {
		reasoner.precomputeInferences(inferenceTypes);
	}

	@Override
	public boolean isPrecomputed(InferenceType inferenceType) {
//		return reasoner.isPrecomputed(inferenceType);
		if(reasoner instanceof OWLlinkReasoner || reasoner instanceof OWLlinkHTTPXMLReasoner)
			return true;
		return OntoUtil.isClassificationPrecomputed(reasoner);
	}

	@Override
	public Set<InferenceType> getPrecomputableInferenceTypes() {
		return reasoner.getPrecomputableInferenceTypes();
	}

	@Override
	public boolean isConsistent() throws ReasonerInterruptedException,
	TimeOutException {
		return reasoner.isConsistent();
	}

	@Override
	public boolean isSatisfiable(OWLClassExpression classExpression)
			throws ReasonerInterruptedException, TimeOutException,
			ClassExpressionNotInProfileException, FreshEntitiesException,
			InconsistentOntologyException {
		return reasoner.isSatisfiable(classExpression);
	}

	@Override
	public Node<OWLClass> getUnsatisfiableClasses()
			throws ReasonerInterruptedException, TimeOutException,
			InconsistentOntologyException {
		return reasoner.getUnsatisfiableClasses();
	}

	@Override
	public boolean isEntailed(OWLAxiom axiom)
			throws ReasonerInterruptedException,
			UnsupportedEntailmentTypeException, TimeOutException,
			AxiomNotInProfileException, FreshEntitiesException,
			InconsistentOntologyException {
		return reasoner.isEntailed(axiom);
	}

	@Override
	public boolean isEntailed(Set<? extends OWLAxiom> axioms)
			throws ReasonerInterruptedException,
			UnsupportedEntailmentTypeException, TimeOutException,
			AxiomNotInProfileException, FreshEntitiesException,
			InconsistentOntologyException {
		return reasoner.isEntailed(axioms);
	}

	@Override
	public boolean isEntailmentCheckingSupported(AxiomType<?> axiomType) {
		return reasoner.isEntailmentCheckingSupported(axiomType);
	}

	@Override
	public Node<OWLClass> getTopClassNode() {
		return reasoner.getTopClassNode();
	}

	@Override
	public Node<OWLClass> getBottomClassNode() {
		return reasoner.getBottomClassNode();
	}

	@Override
	public NodeSet<OWLClass> getSubClasses(OWLClassExpression ce, boolean direct)
			throws ReasonerInterruptedException, TimeOutException,
			FreshEntitiesException, InconsistentOntologyException,
			ClassExpressionNotInProfileException {
		return reasoner.getSubClasses(ce, direct);
	}

	@Override
	public NodeSet<OWLClass> getSuperClasses(OWLClassExpression ce,
			boolean direct) throws InconsistentOntologyException,
			ClassExpressionNotInProfileException, FreshEntitiesException,
			ReasonerInterruptedException, TimeOutException {
		return reasoner.getSuperClasses(ce, direct);
	}

	@Override
	public Node<OWLClass> getEquivalentClasses(OWLClassExpression ce)
			throws InconsistentOntologyException,
			ClassExpressionNotInProfileException, FreshEntitiesException,
			ReasonerInterruptedException, TimeOutException {
		return reasoner.getEquivalentClasses(ce);
	}

	public OWLReasoner getReasoner() {
		return reasoner;
	}
}
