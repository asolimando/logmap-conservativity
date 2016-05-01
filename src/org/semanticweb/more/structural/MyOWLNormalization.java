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
package org.semanticweb.more.structural;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.HermiT.structural.OWLAxioms;
import org.semanticweb.HermiT.structural.OWLNormalization;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitor;
import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
import org.semanticweb.owlapi.model.OWLDataExactCardinality;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataMaxCardinality;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataOneOf;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owlapi.model.OWLDisjointDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalDataPropertyAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLInverseFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLNegativeDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNegativeObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLObjectHasSelf;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubDataPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;
import org.semanticweb.owlapi.model.OWLTransitiveObjectPropertyAxiom;

public class MyOWLNormalization extends OWLNormalization{

	/* modified version of OWLNormalization class in HermiT project
	 * 
	 * this normalizer produces axioms of the following kind:
	 *	
	 *	GCIs with 
	 *
	 *		AND_i A_i -> OR_j B_j
	 *
	 *		where each A_i is an atomic class or an existential restriction with an atomic filler
	 *		and each B_j is an atomic class, or an existential/universal restriction with an atomic filler, or an atMost restriction with a literal filler, or a nominal expression
	 *
	 *
	 *	Domain axioms are rewritten as GCIs
	 *	Range axioms are either rewritten as GCIs or integrated inside other GCIs 
	 *
	 *		for now only dealing with SHOIQ
	 *
	 *	Transitive properties axioms are, in principle, encoded away in GCIs, but I have allowed the option not to do it to debug the rest of the code
	 *
	 *	It seems to split A equiv {a,b,c} into A -> {a,b,c} and A(a), A(b), A(c).
	 *
	 *
	 *	simple ObjectProperty inclusions are kept as given
	 *
	 *	reflexive
	 *	irreflexive
	 *	symmetric
	 *	asymmetric
	 *	functional
	 *	disjoint
	 *	ObjectProperty(ies) axioms
	 * 
	 */
	

	
	
	
	protected OWLOntology o;
	protected Set<OWLAxiom> axiomsNormalizedO;
	protected Set<OWLClassExpression[]> gcis;
	
	protected ObjectPropertyManager objectPropertyManager = new ObjectPropertyManager();
	boolean integrateRangesInRhsExistentials = false;
	boolean encodeTransitivity = false;
	
	
	public MyOWLNormalization(OWLOntology o) {
		this(o,false, false);
	}
	
	public MyOWLNormalization(OWLOntology o, boolean integrateRanges, boolean encodeTransitivity) {
		super(o.getOWLOntologyManager().getOWLDataFactory(), new OWLAxioms(), 0);
		this.o = o;
		this.encodeTransitivity = encodeTransitivity;
		integrateRangesInRhsExistentials = integrateRanges;
		axiomsNormalizedO = new HashSet<OWLAxiom>();
	}
	
	public Set<OWLAxiom> getNormalizedOntology(){
		if (axiomsNormalizedO.isEmpty()){
			//      String ontologyIRI=o.getOntologyID().getDefaultDocumentIRI()==null ? "urn:hermit:kb" : o.getOntologyID().getDefaultDocumentIRI().toString();
			Collection<OWLOntology> importClosure=o.getImportsClosure();
			//with the mappings constructed by the AxiomVisitor, make this process them properly and then reprocess all axioms to integrate ranges
			for (OWLOntology ontology : importClosure)
				processOntology(ontology);

			List<OWLClassExpression[]> conceptInclusionsCopy = new ArrayList<OWLClassExpression[]>(m_axioms.m_conceptInclusions);
			m_axioms.m_conceptInclusions.clear();
			normalizeInclusions(objectPropertyManager.handleRangesDomainsAndTransitivity(conceptInclusionsCopy, integrateRangesInRhsExistentials, encodeTransitivity)); 
			if (!encodeTransitivity)
				axiomsNormalizedO.addAll(objectPropertyManager.getTransitivityAxioms());

			rearrangeClassExpressionInclusions();
		}
		return axiomsNormalizedO;
	}
	
	protected void rearrangeClassExpressionInclusions(){
		NormalizedAxiomRearranger rearranger = new NormalizedAxiomRearranger();
		for (OWLClassExpression[] inclusion : m_axioms.m_conceptInclusions)
			axiomsNormalizedO.add(rearranger.rearrange(inclusion));
	}

	
	@Override
	public void processOntology(OWLOntology ontology) {
//        // Each entry in the inclusions list represents a disjunction of
//        // concepts -- that is, each OWLClassExpression in an entry contributes a
//        // disjunct. It is thus not really inclusions, but rather a disjunction
//        // of concepts that represents an inclusion axiom.
//        m_axioms.m_classes.addAll(ontology.getClassesInSignature(true));
//        m_axioms.m_objectProperties.addAll(ontology.getObjectPropertiesInSignature(true));
//        m_axioms.m_dataProperties.addAll(ontology.getDataPropertiesInSignature(true));
//        m_axioms.m_namedIndividuals.addAll(ontology.getIndividualsInSignature(true));
        processAxioms(ontology.getLogicalAxioms());
    }
	
	
	public int getDefinitionsSize(){
		return m_definitions.size();
	}
	
	
	public void processAxioms(Collection<? extends OWLAxiom> axioms) {
        MyAxiomVisitor axiomVisitor=new MyAxiomVisitor();
        for (OWLAxiom axiom : axioms){
            axiom.accept(axiomVisitor);
        }
        // now all axioms are in NNF and converted into disjunctions wherever possible
        // exact cardinalities are rewritten into at least and at most cardinalities etc //this was so in HermiT, we are not considering exact cardinalities here for now
        // Rules with multiple head atoms are rewritten into several rules (Lloyd-Topor transformation)

//        // normalize rules, this might add new concept and data range inclusions
//        // in case a rule atom uses a complex concept or data range
//        // we keep this inclusions separate because they are only applied to named individuals
//        RuleNormalizer ruleNormalizer=new RuleNormalizer(m_axioms.m_rules,axiomVisitor.m_classExpressionInclusionsAsDisjunctions,axiomVisitor.m_dataRangeInclusionsAsDisjunctions);
//        for (SWRLRule rule : axiomVisitor.m_rules)
//            ruleNormalizer.visit(rule);

        // in normalization, we now simplify the disjuncts where possible (eliminate
        // unnecessary conjuncts/disjuncts) and introduce fresh atomic concepts for complex
        // concepts m_axioms.m_conceptInclusions contains the normalized axioms after the normalization
        normalizeInclusions(axiomVisitor.getClassExpressionInclusionsAsDisjunctions());
    }
	
	protected void normalizeInclusions(List<OWLClassExpression[]> inclusions) {
        MyClassExpressionNormalizer classExpressionNormalizer=new MyClassExpressionNormalizer(inclusions);
//        ClassExpressionNormalizer classExpressionNormalizer=new ClassExpressionNormalizer(inclusions,dataRangeInclusions);
        // normalize all class expression inclusions
        while (!inclusions.isEmpty()) {
            OWLClassExpression simplifiedDescription=m_expressionManager.getNNF(m_expressionManager.getSimplified(m_factory.getOWLObjectUnionOf(inclusions.remove(inclusions.size()-1))));
            
            if (!simplifiedDescription.isOWLThing()) {
                if (simplifiedDescription instanceof OWLObjectUnionOf) {
                    OWLObjectUnionOf objectOr=(OWLObjectUnionOf)simplifiedDescription;
                    OWLClassExpression[] descriptions=new OWLClassExpression[objectOr.getOperands().size()];
                    objectOr.getOperands().toArray(descriptions);
                    if (!distributeUnionOverAnd(descriptions,inclusions) && !optimizedNegativeOneOfTranslation(descriptions,m_axioms.m_facts)) {
                        for (int index=0;index<descriptions.length;index++)
                            descriptions[index]=descriptions[index].accept(classExpressionNormalizer);
                        m_axioms.m_conceptInclusions.add(descriptions);
                    }
                }
                else if (simplifiedDescription instanceof OWLObjectIntersectionOf) {
                    OWLObjectIntersectionOf objectAnd=(OWLObjectIntersectionOf)simplifiedDescription;
                    for (OWLClassExpression conjunct : objectAnd.getOperands())
                        inclusions.add(new OWLClassExpression[] { conjunct });
                }
                else {
                    OWLClassExpression normalized=simplifiedDescription.accept(classExpressionNormalizer);
                    m_axioms.m_conceptInclusions.add(new OWLClassExpression[] { normalized });
                }
            }
        }
        
    }
	
//	@Override
//	protected boolean distributeUnionOverAnd(OWLClassExpression[] descriptions,List<OWLClassExpression[]> inclusions) {
//		//the original code in hermit only performs the distribution if there is a single intersection.
//		//In cases where there is more than one, like when we consider axiom {some R. C1 or some R. C2 -> A and B}
//		//this leads to the introduction of unnecessary nondeterminism, furthermore, in this particular case it leads to the introduction
//		//of an axiom top -> X1 or X2, which completely kills MORe.
//		//I have modified this method here so that, if one intersection is encountered, it is distributed as thought it was the only intersection,
//		//and the corresponding new inclusions
//		
//        int andIndex = -1;
//        int index = 0;
//        while (andIndex < 0 && index<descriptions.length) {
//            OWLClassExpression description=descriptions[index];
//            if (description instanceof OWLObjectIntersectionOf) 
//            	andIndex=index;
//            index++;
//        }
//        if (andIndex==-1)
//            return false;
//        OWLObjectIntersectionOf objectAnd=(OWLObjectIntersectionOf)descriptions[andIndex];
//        for (OWLClassExpression description : objectAnd.getOperands()) {
//            OWLClassExpression[] newDescriptions=descriptions.clone();
//            newDescriptions[andIndex]=description;
//            inclusions.add(newDescriptions);
//        }
//        return true;
//    }
	
//	protected boolean willStayOnRhs(OWLClassExpression c){
//		if (c instanceof OWLClass)
//	        return true;
//	    if (c instanceof OWLObjectIntersectionOf) 
//	        throw new IllegalStateException("Internal error: invalid normal form.");
//	    if (c instanceof OWLObjectUnionOf)
//	        throw new IllegalStateException("Internal error: invalid normal form.");
//	    if (c instanceof OWLObjectComplementOf) {
//	        OWLClassExpression description = ((OWLObjectComplementOf) c).getOperand();
//	        if (description instanceof OWLClass || description instanceof OWLObjectHasSelf)
//	        	return false;
//	        else
//	            throw new IllegalStateException("Internal error: invalid normal form.");
//	    }
//	    if (c instanceof OWLObjectSomeValuesFrom)
//	    	return true;
//	    if (c instanceof OWLObjectAllValuesFrom) {
//	        OWLClassExpression filler = ((OWLObjectAllValuesFrom) c).getFiller();
//	        if (filler instanceof OWLClass && !filler.isOWLNothing())
//	            return true;
//	        else if ((filler instanceof OWLObjectComplementOf && ((OWLObjectComplementOf) filler).getOperand() instanceof OWLClass) || (filler.isOWLNothing()))
//	        	return false;
//	        else
//	            throw new IllegalStateException("Internal error: invalid normal form: " + c.toString());
//	    }
//	    throw new IllegalStateException("Internal error: invalid normal form.");
//	}
	
	
    
	@Override
	protected OWLClassExpression getDefinitionFor(OWLClassExpression description,boolean[] alreadyExists) {
		if (description instanceof OWLObjectAllValuesFrom)
			return getDefinitionFor(description,alreadyExists,false);
//		else if (description instanceof OWLObjectSomeValuesFrom)
//			return getDefinitionFor(description,alreadyExists,false);
		else
			return getDefinitionFor(description,alreadyExists,true);
//        return getDefinitionFor(description,alreadyExists,false);
    }
	
	public OWLClassExpression getDefinition(OWLClassExpression description,boolean[] alreadyExists, boolean forcePositive) {
		return getDefinitionFor(description, alreadyExists, forcePositive);
    }
	
	protected OWLClass getFreshClass() {
        OWLClass definition = m_factory.getOWLClass(IRI.create("internal:def#"+(m_definitions.size()+m_firstReplacementIndex)));
        m_definitions.put(definition,definition);
        return definition;
    }
    
	
	
	protected class MyAxiomVisitor extends AxiomVisitor {

		public MyAxiomVisitor() {
			super();
		}


		public List<OWLClassExpression[]> getClassExpressionInclusionsAsDisjunctions(){
			return m_classExpressionInclusionsAsDisjunctions;
		}


		public void visit(OWLSubClassOfAxiom axiom) {
			OWLClassExpression[] inclusion = new OWLClassExpression[] { negative(axiom.getSubClass()),positive(axiom.getSuperClass()) };
			m_classExpressionInclusionsAsDisjunctions.add(inclusion);
		}
		public void visit(OWLEquivalentClassesAxiom axiom) {
            if (axiom.getClassExpressions().size()>1) {
                Iterator<OWLClassExpression> iterator=axiom.getClassExpressions().iterator();
                OWLClassExpression first=iterator.next();
                OWLClassExpression last=first;
                while (iterator.hasNext()) {
                    OWLClassExpression next=iterator.next();
                    m_factory.getOWLSubClassOfAxiom(last, next).accept(this);
//                    m_classExpressionInclusionsAsDisjunctions.add(new OWLClassExpression[] { negative(last),positive(next) });
                    last=next;
                }
                m_factory.getOWLSubClassOfAxiom(last, first).accept(this);
//                m_classExpressionInclusionsAsDisjunctions.add(new OWLClassExpression[] { negative(last),positive(first) });
            }
        }

		// Object property axioms

		public void visit(OWLSubObjectPropertyOfAxiom axiom) {

			if (!axiom.getSubProperty().isOWLBottomObjectProperty() && !axiom.getSuperProperty().isOWLTopObjectProperty()){
				axiomsNormalizedO.add(axiom);
//				addInclusion(axiom.getSubProperty(),axiom.getSuperProperty());
//			m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(axiom.getSubProperty().getNamedProperty());
//			m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(axiom.getSuperProperty().getNamedProperty());

				objectPropertyManager.registerPropertyInclusion(axiom.getSubProperty(), axiom.getSuperProperty());
			}
			
		}

		public void visit(OWLSubPropertyChainOfAxiom axiom) {
			List<OWLObjectPropertyExpression> subPropertyChain=axiom.getPropertyChain();
			if (!containsBottomObjectProperty(subPropertyChain) && !axiom.getSuperProperty().isOWLTopObjectProperty()) {
				OWLObjectPropertyExpression superObjectPropertyExpression=axiom.getSuperProperty().getSimplified();
				if (subPropertyChain.size()==1){
					axiomsNormalizedO.add(m_factory.getOWLSubObjectPropertyOfAxiom(subPropertyChain.get(0),superObjectPropertyExpression));
//					addInclusion(subPropertyChain.get(0),superObjectPropertyExpression);
					//it's a normal subproperty axiom - need to add it's info to the roleHierarchy map

					objectPropertyManager.registerPropertyInclusion(subPropertyChain.get(0), superObjectPropertyExpression);

				}
				else if (subPropertyChain.size()==2 && subPropertyChain.get(0).equals(superObjectPropertyExpression) && subPropertyChain.get(1).equals(superObjectPropertyExpression))
//					makeTransitive(axiom.getSuperProperty());
//					objectPropertyManager.registerTransitiveObjectProperty(axiom.getSuperProperty());
					objectPropertyManager.registerTransitivityAxiom(m_factory.getOWLTransitiveObjectPropertyAxiom(axiom.getSuperProperty()));
				else if (subPropertyChain.size()==0)
					throw new IllegalArgumentException("Error: In OWL 2 DL, an empty property chain in property chain axioms is not allowed, but the ontology contains an axiom that the empty chain is a subproperty of "+superObjectPropertyExpression+".");
				else {
					throw new IllegalArgumentException("we are not supposed to accept complex role inclusions yet!");
					
//					OWLObjectPropertyExpression[] subObjectProperties=new OWLObjectPropertyExpression[subPropertyChain.size()];
//					subPropertyChain.toArray(subObjectProperties);
//					addInclusion(subObjectProperties,superObjectPropertyExpression);
//					
//					System.out.println("complex role inclusion");
				}
			}
//			for (OWLObjectPropertyExpression objectPropertyExpression : subPropertyChain)
//				m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(objectPropertyExpression.getNamedProperty());
//			m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(axiom.getSuperProperty().getNamedProperty());
		}
		public void visit(OWLTransitiveObjectPropertyAxiom axiom) {
//            makeTransitive(axiom.getProperty());
//            m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(axiom.getProperty().getNamedProperty());
            objectPropertyManager.registerTransitivityAxiom(axiom);
        }
		public void visit(OWLEquivalentObjectPropertiesAxiom axiom) {
			Set<OWLObjectPropertyExpression> objectPropertyExpressions=axiom.getProperties();
			if (objectPropertyExpressions.size()>1) {
				Iterator<OWLObjectPropertyExpression> iterator=objectPropertyExpressions.iterator();
				OWLObjectPropertyExpression first=iterator.next();
				OWLObjectPropertyExpression last=first;
				while (iterator.hasNext()) {
					OWLObjectPropertyExpression next=iterator.next();
//					addInclusion(last,next);
					axiomsNormalizedO.add(m_factory.getOWLSubObjectPropertyOfAxiom(last, next));
					last=next;
				}
//				addInclusion(last,first);
				axiomsNormalizedO.add(m_factory.getOWLSubObjectPropertyOfAxiom(last, first));
			}
//			for (OWLObjectPropertyExpression objectPropertyExpression : objectPropertyExpressions)
//				m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(objectPropertyExpression.getNamedProperty());
		}

		public void visit(OWLDisjointObjectPropertiesAxiom axiom) {// with only two properties each // are we taking this yet as in SHOIQ?
			Set<OWLObjectPropertyExpression> disjointProperties = axiom.getProperties(); 
			List<OWLObjectPropertyExpression> disjointPropertiesSimplified=new ArrayList<OWLObjectPropertyExpression>(disjointProperties.size());
			for (OWLObjectPropertyExpression p : disjointProperties)
				disjointPropertiesSimplified.add(p.getSimplified());
			for (int i = 0 ; i < disjointPropertiesSimplified.size() ; i++)
				for (int j = i+1 ; j < disjointPropertiesSimplified.size() ; j++){
					OWLDisjointObjectPropertiesAxiom ax = m_factory.getOWLDisjointObjectPropertiesAxiom(disjointPropertiesSimplified.get(i), disjointPropertiesSimplified.get(j));
					axiomsNormalizedO.add(ax);		
				}
		}
//		public void visit(OWLDisjointObjectPropertiesAxiom axiom) {
//			OWLObjectPropertyExpression[] objectPropertyExpressions=new OWLObjectPropertyExpression[axiom.getProperties().size()];
//			axiom.getProperties().toArray(objectPropertyExpressions);
//			for (int i=0;i<objectPropertyExpressions.length;i++) {
//				objectPropertyExpressions[i]=objectPropertyExpressions[i].getSimplified();
//				m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(objectPropertyExpressions[i].getNamedProperty());
//			}
//			m_axioms.m_disjointObjectProperties.add(objectPropertyExpressions);
//		}
		public void visit(OWLInverseObjectPropertiesAxiom axiom) {
			OWLObjectPropertyExpression first=axiom.getFirstProperty().getSimplified();
			OWLObjectPropertyExpression second=axiom.getSecondProperty().getSimplified();
//			addInclusion(first,second.getInverseProperty().getSimplified());
//			addInclusion(second,first.getInverseProperty().getSimplified());
			axiomsNormalizedO.add(m_factory.getOWLSubObjectPropertyOfAxiom(first,second.getInverseProperty().getSimplified()));
			axiomsNormalizedO.add(m_factory.getOWLSubObjectPropertyOfAxiom(second,first.getInverseProperty().getSimplified()));
//			m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(first.getNamedProperty());
//			m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(second.getNamedProperty());

			objectPropertyManager.registerPropertyInclusion(first, second.getInverseProperty());
			objectPropertyManager.registerPropertyInclusion(second, first.getInverseProperty());
			
		}

		@Override
		//  public void visit(OWLObjectPropertyRangeAxiom axiom) {
		//      OWLObjectAllValuesFrom allPropertyRange = m_factory.getOWLObjectAllValuesFrom(
		//      		axiom.getProperty().getSimplified(),
		//      		positive(axiom.getRange()));
		//      m_classExpressionInclusionsAsDisjunctionsMap.put(new OWLClassExpression[] { allPropertyRange }, axiom);
		//  }
		//  @Override
		//  public void visit(OWLObjectPropertyDomainAxiom axiom) {
		//  	OWLObjectAllValuesFrom allPropertyDomain = m_factory.getOWLObjectAllValuesFrom(
		//				axiom.getProperty().getInverseProperty().getSimplified(), 
		//				positive(axiom.getDomain())); 
		//  	m_classExpressionInclusionsAsDisjunctionsMap.put(new OWLClassExpression[] {allPropertyDomain}, axiom);
		//  }
		//  
		public void visit(OWLObjectPropertyDomainAxiom axiom) {
//			OWLObjectAllValuesFrom allPropertyNothing=m_factory.getOWLObjectAllValuesFrom(axiom.getProperty().getSimplified(),m_factory.getOWLNothing());
//			m_classExpressionInclusionsAsDisjunctions.add(new OWLClassExpression[] { positive(axiom.getDomain()),allPropertyNothing });
////			m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(axiom.getProperty().getNamedProperty());
			
			OWLClassExpression domain = axiom.getDomain();
			OWLObjectPropertyExpression p = axiom.getProperty();
			if (domain instanceof OWLClass)
				objectPropertyManager.registerDomain(p, (OWLClass) domain);
			else{
				domain = positive(domain);
				OWLClass def = (OWLClass) getDefinitionFor(domain, m_alreadyExists, true);
				m_classExpressionInclusionsAsDisjunctions.add(new OWLClassExpression[]{def.getComplementNNF(), domain});
				objectPropertyManager.registerDomain(p, def);
			}
		}
		public void visit(OWLObjectPropertyRangeAxiom axiom) {
////			if (integrateRangesInRhsExistentials)
////				objectPropertyManager.registerRangeAxiom(axiom);
////			else{
//				OWLObjectAllValuesFrom allPropertyRange=m_factory.getOWLObjectAllValuesFrom(axiom.getProperty().getSimplified(),positive(axiom.getRange()));
//				m_classExpressionInclusionsAsDisjunctions.add(new OWLClassExpression[] { allPropertyRange });
////			}
			OWLClassExpression range = axiom.getRange();
			OWLObjectPropertyExpression p = axiom.getProperty();
			if (range instanceof OWLClass)
				objectPropertyManager.registerRange(p, (OWLClass) range);
			else{
				range = positive(range);
				OWLClass def = (OWLClass) getDefinitionFor(range, m_alreadyExists, true);
				m_classExpressionInclusionsAsDisjunctions.add(new OWLClassExpression[]{def.getComplementNNF(), range});
				objectPropertyManager.registerRange(p, def);
			}
		}
		public void visit(OWLFunctionalObjectPropertyAxiom axiom) {
			m_classExpressionInclusionsAsDisjunctions.add(new OWLClassExpression[] { m_factory.getOWLObjectMaxCardinality(1,axiom.getProperty().getSimplified()) });
//			m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(axiom.getProperty().getNamedProperty());
		}
		public void visit(OWLInverseFunctionalObjectPropertyAxiom axiom) {
			m_classExpressionInclusionsAsDisjunctions.add(new OWLClassExpression[] { m_factory.getOWLObjectMaxCardinality(1,axiom.getProperty().getSimplified().getInverseProperty()) });
//			m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(axiom.getProperty().getNamedProperty());
		}



		// Data property axioms
		//we ignore data properties for now

		public void visit(OWLSubDataPropertyOfAxiom axiom) {
			//        OWLDataPropertyExpression subDataProperty=axiom.getSubProperty();
			//        checkTopDataPropertyUse(subDataProperty,axiom);
			//        OWLDataPropertyExpression superDataProperty=axiom.getSuperProperty();
			//        if (!subDataProperty.isOWLBottomDataProperty() && !superDataProperty.isOWLTopDataProperty())
			//            addInclusion(subDataProperty,superDataProperty);
//			axiomsNormalizedO.add(axiom);
			System.out.println("DATA PROPERTY!?!?!?!");
		}
		public void visit(OWLEquivalentDataPropertiesAxiom axiom) {
			//        for (OWLDataPropertyExpression dataPropertyExpression : axiom.getProperties())
			//            checkTopDataPropertyUse(dataPropertyExpression,axiom);
			//        if (axiom.getProperties().size()>1) {
			//            Iterator<OWLDataPropertyExpression> iterator=axiom.getProperties().iterator();
			//            OWLDataPropertyExpression first=iterator.next();
			//            OWLDataPropertyExpression last=first;
			//            while (iterator.hasNext()) {
			//                OWLDataPropertyExpression next=iterator.next();
			//                addInclusion(last,next);
			//                last=next;
			//            }
			//            addInclusion(last,first);
			//        }
//			axiomsNormalizedO.add(axiom);
			System.out.println("DATA PROPERTY!?!?!?!");
		}
		public void visit(OWLDisjointDataPropertiesAxiom axiom) {
			//        OWLDataPropertyExpression[] dataProperties=new OWLDataPropertyExpression[axiom.getProperties().size()];
			//        axiom.getProperties().toArray(dataProperties);
			//        for (OWLDataPropertyExpression dataProperty : dataProperties)
			//            checkTopDataPropertyUse(dataProperty,axiom);
			//        m_axioms.m_disjointDataProperties.add(dataProperties);
//			axiomsNormalizedO.add(axiom);
			System.out.println("DATA PROPERTY!?!?!?!");
		}
		public void visit(OWLDataPropertyDomainAxiom axiom) {
			//        OWLDataPropertyExpression dataProperty=axiom.getProperty();
			//        checkTopDataPropertyUse(dataProperty,axiom);
			//        OWLDataRange dataNothing=m_factory.getOWLDataComplementOf(m_factory.getTopDatatype());
			//        OWLDataAllValuesFrom allPropertyDataNothing=m_factory.getOWLDataAllValuesFrom(dataProperty,dataNothing);
			//        m_classExpressionInclusionsAsDisjunctions.add(new OWLClassExpression[] { positive(axiom.getDomain()),allPropertyDataNothing });
//			axiomsNormalizedO.add(axiom);
			System.out.println("DATA PROPERTY!?!?!?!");
		}
		public void visit(OWLDataPropertyRangeAxiom axiom) {
			//        OWLDataPropertyExpression dataProperty=axiom.getProperty();
			//        checkTopDataPropertyUse(dataProperty,axiom);
			//        OWLDataAllValuesFrom allPropertyRange=m_factory.getOWLDataAllValuesFrom(dataProperty,positive(axiom.getRange()));
			//        m_classExpressionInclusionsAsDisjunctions.add(new OWLClassExpression[] { allPropertyRange });
//			axiomsNormalizedO.add(axiom);
			System.out.println("DATA PROPERTY!?!?!?!");
		}
		public void visit(OWLFunctionalDataPropertyAxiom axiom) {
			//        OWLDataPropertyExpression dataProperty=axiom.getProperty();
			//        checkTopDataPropertyUse(dataProperty,axiom);
			//        m_classExpressionInclusionsAsDisjunctions.add(new OWLClassExpression[] { m_factory.getOWLDataMaxCardinality(1,dataProperty) });
//			axiomsNormalizedO.add(axiom);
			System.out.println("DATA PROPERTY!?!?!?!");
		}
		protected void checkTopDataPropertyUse(OWLDataPropertyExpression dataPropertyExpression,OWLAxiom axiom) {
			//        if (dataPropertyExpression.isOWLTopDataProperty())
			//            throw new IllegalArgumentException("Error: In OWL 2 DL, owl:topDataProperty is only allowed to occur in the super property position of SubDataPropertyOf axioms, but the ontology contains an axiom "+axiom+" that violates this condition.");
		}

		// Assertions

		public void visit(OWLSameIndividualAxiom axiom) {
			if (axiom.containsAnonymousIndividuals())
				throw new IllegalArgumentException("The axiom "+axiom+" contains anonymous individuals, which is not allowed in OWL 2. ");
			axiomsNormalizedO.add(axiom);
		}
		public void visit(OWLDifferentIndividualsAxiom axiom) {
			if (axiom.containsAnonymousIndividuals())
				throw new IllegalArgumentException("The axiom "+axiom+" contains anonymous individuals, which is not allowed in OWL 2. ");
			axiomsNormalizedO.add(axiom);
		}
		public void visit(OWLClassAssertionAxiom axiom) {
			OWLClassExpression classExpression=axiom.getClassExpression();
			if (classExpression instanceof OWLDataHasValue) {
//				OWLDataHasValue hasValue=(OWLDataHasValue)classExpression;
//				addFact(m_factory.getOWLDataPropertyAssertionAxiom(hasValue.getProperty(), axiom.getIndividual(), hasValue.getValue()));
//				return;
				System.out.println("DATA PROPERTY!?!?!?!");
			}
			if (classExpression instanceof OWLDataSomeValuesFrom) {
//				OWLDataSomeValuesFrom someValuesFrom=(OWLDataSomeValuesFrom)classExpression;
//				OWLDataRange dataRange=someValuesFrom.getFiller();
//				if (dataRange instanceof OWLDataOneOf) {
//					OWLDataOneOf oneOf=(OWLDataOneOf)dataRange;
//					if (oneOf.getValues().size()==1) {
//						addFact(m_factory.getOWLDataPropertyAssertionAxiom(someValuesFrom.getProperty(),axiom.getIndividual(),oneOf.getValues().iterator().next()));
//						return;
//					}
//				}
				System.out.println("DATA PROPERTY!?!?!?!");
			}
			classExpression=positive(classExpression);
			if (!isSimple(classExpression)) {
				OWLClassExpression definition=getDefinitionFor(classExpression,m_alreadyExists);
				if (!m_alreadyExists[0])
					m_classExpressionInclusionsAsDisjunctions.add(new OWLClassExpression[] { negative(definition),classExpression });
				classExpression=definition;
			}
			axiomsNormalizedO.add(m_factory.getOWLClassAssertionAxiom(classExpression,axiom.getIndividual()));
		}
		public void visit(OWLObjectPropertyAssertionAxiom axiom) {
			axiomsNormalizedO.add(m_factory.getOWLObjectPropertyAssertionAxiom(axiom.getProperty().getSimplified(),axiom.getSubject(),axiom.getObject()));
//			m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(axiom.getProperty().getNamedProperty());
		}
		public void visit(OWLNegativeObjectPropertyAssertionAxiom axiom) {
			if (axiom.containsAnonymousIndividuals())
				throw new IllegalArgumentException("The axiom "+axiom+" contains anonymous individuals, which is not allowed in OWL 2 DL. ");
			axiomsNormalizedO.add(m_factory.getOWLNegativeObjectPropertyAssertionAxiom(axiom.getProperty().getSimplified(),axiom.getSubject(),axiom.getObject()));
//			m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(axiom.getProperty().getNamedProperty());
		}
		public void visit(OWLDataPropertyAssertionAxiom axiom) {
			System.out.println("DATA PROPERTY!?!?!?!");
//			checkTopDataPropertyUse(axiom.getProperty(),axiom);
//			addFact(axiom);
		}
		public void visit(OWLNegativeDataPropertyAssertionAxiom axiom) {
			System.out.println("DATA PROPERTY!?!?!?!");
//			checkTopDataPropertyUse(axiom.getProperty(),axiom);
//			if (axiom.containsAnonymousIndividuals())
//				throw new IllegalArgumentException("The axiom "+axiom+" contains anonymous individuals, which is not allowed in OWL 2 DL. ");
//			addFact(axiom);
		}

	}

	protected class MyClassExpressionNormalizer extends ClassExpressionNormalizer{
		//        protected final Collection<OWLClassExpression[]> m_newInclusions;
		//        protected final Collection<OWLDataRange[]> m_newDataRangeInclusions;
		//        protected final boolean[] m_alreadyExists;

		public MyClassExpressionNormalizer(Collection<OWLClassExpression[]> newInclusions) {
			super(newInclusions, new ArrayList<OWLDataRange[]>());
		}

		public OWLClassExpression visit(OWLObjectSomeValuesFrom object) {
//			m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(object.getProperty().getNamedProperty());
			OWLClassExpression filler=object.getFiller();
			//            if (isSimple(filler) || isNominal(filler))
			if (filler instanceof OWLClass || isNominal(filler))
				return object;
			else {
				//            	OWLClassExpression definition=getDefinitionFor(filler,m_alreadyExists);
				OWLClassExpression definition=getDefinitionFor(filler,m_alreadyExists,true);//forcePositive=true
				if (!m_alreadyExists[0])
					m_newInclusions.add(new OWLClassExpression[] { negative(definition),filler });
				return m_factory.getOWLObjectSomeValuesFrom(object.getProperty(),definition);
			}
		}
		public OWLClassExpression visit(OWLObjectAllValuesFrom object) {
//            m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(object.getProperty().getNamedProperty());
            OWLClassExpression filler=object.getFiller();
            if (isSimple(filler))// || isNominal(filler) || isNegatedOneNominal(filler))
//                // The nominal cases are optimizations.
                return object;
            else {
                OWLClassExpression definition=getDefinitionFor(filler, m_alreadyExists, false);
                if (!m_alreadyExists[0])
                    m_newInclusions.add(new OWLClassExpression[] { negative(definition),filler });
                return m_factory.getOWLObjectAllValuesFrom(object.getProperty(),definition);
            }
        }
        public OWLClassExpression visit(OWLObjectMinCardinality object) {
//			m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(object.getProperty().getNamedProperty());
			OWLClassExpression filler=object.getFiller();
			//want to transform this into existential restrictions
			if (object.getCardinality() > 1){
				OWLClass[] disjointClasses = new OWLClass[object.getCardinality()];
				OWLClass freshClass = getFreshClass();
				for (int i = 0 ; i < disjointClasses.length ; i++){
					disjointClasses[i] = getFreshClass();
					m_newInclusions.add(
							new OWLClassExpression[]{
									freshClass.getComplementNNF(),
									m_factory.getOWLObjectSomeValuesFrom(object.getProperty().getSimplified(), disjointClasses[i])});
				}
				for (int i = 0 ; i < disjointClasses.length ; i++)
					for (int j = i+1 ; j < disjointClasses.length ; j++)
						m_newInclusions.add(
								new OWLClassExpression[]{
										disjointClasses[i].getComplementNNF(),
										disjointClasses[j].getComplementNNF()});

				for (int i = 0 ; i < disjointClasses.length ; i++)
					m_newInclusions.add(
							new OWLClassExpression[]{
									disjointClasses[i].getComplementNNF(),
									filler});

				return freshClass;
			} 
			else if (object.getCardinality() == 1){
				if (isSimple(filler))
					return m_factory.getOWLObjectSomeValuesFrom(object.getProperty().getSimplified(),filler);
				else {
					OWLClassExpression definition=getDefinitionFor(filler,m_alreadyExists, true);
					if (!m_alreadyExists[0])
						m_newInclusions.add(new OWLClassExpression[] { negative(definition),filler });
					return m_factory.getOWLObjectSomeValuesFrom(object.getProperty().getSimplified(),definition);
				}
			} 
			else
				return m_factory.getOWLThing();
		}
		public OWLClassExpression visit(OWLObjectMaxCardinality object) {
//			m_axioms.m_objectPropertiesOccurringInOWLAxioms.add(object.getProperty().getNamedProperty());
			OWLClassExpression filler=object.getFiller();
			if (object.getCardinality() == 0){
				filler = filler.getComplementNNF();
				OWLClassExpression c = m_factory.getOWLObjectAllValuesFrom(object.getProperty().getSimplified(), filler);
				return c.accept(this);
			}
			else
				if (isSimple(filler))
					return object;
				else {//not 100% about this case...
					OWLClassExpression complementDescription=m_expressionManager.getComplementNNF(filler);
					OWLClassExpression definition=getDefinitionFor(complementDescription,m_alreadyExists);
					if (!m_alreadyExists[0])
						m_newInclusions.add(new OWLClassExpression[] { negative(definition),complementDescription });
					return m_factory.getOWLObjectMaxCardinality(object.getCardinality(),object.getProperty(),m_expressionManager.getComplementNNF(definition));
				}
		}

	}

	protected class NormalizedAxiomRearranger implements OWLClassExpressionVisitor {
		protected Set<OWLClassExpression> m_lhsDisjuncts;//make this a list so that you can order it
//		protected final ArrayList<OWLClassExpression> m_lhsDisjuncts;//make this a list so that you can order it
		protected Set<OWLClassExpression> m_rhsConjuncts;

		//need to save these
		
		public NormalizedAxiomRearranger() {
			m_lhsDisjuncts=new HashSet<OWLClassExpression>();
			m_rhsConjuncts=new HashSet<OWLClassExpression>();
		}

		public OWLAxiom rearrange(OWLClassExpression[] inclusion){
			m_lhsDisjuncts.clear();
			m_rhsConjuncts.clear();
			
			for (OWLClassExpression description : inclusion)
				description.accept(this);

//			OWLAxiom ax = m_factory.getOWLSubClassOfAxiom(getLhsAsIntersection(), getRhsAsUnion());
			return m_factory.getOWLSubClassOfAxiom(getLhsAsIntersection(), getRhsAsUnion());
		}

		private OWLClassExpression getLhsAsIntersection(){
			if (m_lhsDisjuncts.isEmpty())
				return m_factory.getOWLThing();
			else if (m_lhsDisjuncts.size() == 1)
				return m_lhsDisjuncts.iterator().next();
			else 
				return m_factory.getOWLObjectIntersectionOf(m_lhsDisjuncts);
		}

		private OWLClassExpression getRhsAsUnion(){
			if (m_rhsConjuncts.isEmpty())
				return m_factory.getOWLNothing();
			else if (m_rhsConjuncts.size() == 1)
				return m_rhsConjuncts.iterator().next();
			else 
				return m_factory.getOWLObjectUnionOf(m_rhsConjuncts);
		}


		// Various types of descriptions

		public void visit(OWLClass object) {
			m_rhsConjuncts.add(object);
		}
		public void visit(OWLObjectIntersectionOf object) {
			throw new IllegalStateException("Internal error: invalid normal form.");
		}
		public void visit(OWLObjectUnionOf object) {
			throw new IllegalStateException("Internal error: invalid normal form.");
		}
		public void visit(OWLObjectComplementOf object) {
			OWLClassExpression description=object.getOperand();
			if (!(description instanceof OWLClass))
				throw new IllegalStateException("Internal error: invalid normal form.");
			else{
				m_lhsDisjuncts.add(description);
			}
		}
		public void visit(OWLObjectOneOf object) {    
			m_rhsConjuncts.add(object);
		}
		public void visit(OWLObjectSomeValuesFrom object) {
			m_rhsConjuncts.add(object);
		}
		public void visit(OWLObjectAllValuesFrom object) {
			OWLClassExpression filler=object.getFiller();
			if (filler instanceof OWLClass && !(filler.isOWLNothing())){
				m_rhsConjuncts.add(object);
			}
			else if ( filler.isOWLNothing()){
				OWLObjectSomeValuesFrom some = m_factory.getOWLObjectSomeValuesFrom(object.getProperty().getSimplified(), m_factory.getOWLThing()); 
				m_lhsDisjuncts.add(some);
			}
			else if (filler instanceof OWLObjectComplementOf){
				OWLObjectSomeValuesFrom some = m_factory.getOWLObjectSomeValuesFrom(object.getProperty().getSimplified(), object.getFiller().getComplementNNF()); 
				m_lhsDisjuncts.add(some);
			}
			else
				throw new IllegalStateException("Internal error: invalid normal form: " + object.toString());
		}
		public void visit(OWLObjectHasSelf object) {
			//what happens if this occurred on the lhs of some axiom??
			//how does it get treated in the first place when finding the NNF??
			m_rhsConjuncts.add(object);
		}
		public void visit(OWLObjectMinCardinality object) {
//			//have I forgotten to treat this when its cardinality is 1 in all other methods in the class??
//			m_rhsConjuncts.add(object);
			throw new IllegalStateException("min cardinality restrictions should have been rewritten away!");
		}
		public void visit(OWLObjectMaxCardinality object) {
			m_rhsConjuncts.add(object);
		}
		public void visit(OWLObjectHasValue object) {
			m_rhsConjuncts.add(object);
		}
		public void visit(OWLObjectExactCardinality object) {
			System.out.println("oops OWLObjectExactCardinality");
		}
		public void visit(OWLDataSomeValuesFrom object) {
			System.out.println("oops OWLDataSomeValuesFrom");
		}
		public void visit(OWLDataAllValuesFrom object) {
			System.out.println("oops OWLDataAllValuesFrom");
		}
		public void visit(OWLDataHasValue object) {
			System.out.println("oops OWLDataHasValue");
		}
		public void visit(OWLDataMinCardinality object) {
			System.out.println("oops OWLDataMinCardinality");
		}
		public void visit(OWLDataMaxCardinality object) {
			System.out.println("oops OWLDataMaxCardinality");
		}
		public void visit(OWLDataExactCardinality object) {
			System.out.println("oops OWLDataExactCardinality");
		}
	}
	
	
}