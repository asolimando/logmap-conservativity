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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owl.explanation.api.ExplanationException;
import org.semanticweb.owl.explanation.api.ExplanationGeneratorInterruptedException;
import org.semanticweb.owl.explanation.impl.blackbox.EntailmentChecker;
import org.semanticweb.owl.explanation.impl.blackbox.checker.SatisfiabilityEntailmentChecker.UnsupportedAxiomTypeException;
import org.semanticweb.owl.explanation.telemetry.DefaultTelemetryInfo;
import org.semanticweb.owl.explanation.telemetry.TelemetryInfo;
import org.semanticweb.owl.explanation.telemetry.TelemetryTimer;
import org.semanticweb.owl.explanation.telemetry.TelemetryTransmitter;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLAsymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLAxiomVisitorEx;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDatatypeDefinitionAxiom;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointUnionAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalDataPropertyAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLHasKeyAxiom;
import org.semanticweb.owlapi.model.OWLInverseFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLIrreflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNegativeDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNegativeObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLReflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;
import org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubDataPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;
import org.semanticweb.owlapi.model.OWLSymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLTransitiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.SWRLRule;
import org.semanticweb.owlapi.owllink.OWLlinkReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.FreshEntityPolicy;
import org.semanticweb.owlapi.reasoner.IndividualNodeSetPolicy;
import org.semanticweb.owlapi.reasoner.NullReasonerProgressMonitor;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.reasoner.TimeOutException;

import uk.ac.manchester.cs.owlapi.modularity.ModuleType;
import uk.ac.manchester.cs.owlapi.modularity.SyntacticLocalityModuleExtractor;

public class OWLLinkSatisfiabilityEntailmentChecker implements EntailmentChecker<OWLAxiom> {

	public OWLLinkSatisfiabilityEntailmentChecker(
			OWLReasonerFactory reasonerFactory, OWLAxiom entailment){
	        this(reasonerFactory, entailment, true, Long.MAX_VALUE);
	}
	
	public OWLLinkSatisfiabilityEntailmentChecker(
			OWLReasonerFactory reasonerFactory, OWLAxiom entailment,
			boolean useModularisation, long timeOutMS) {
		
        this.reasonerFactory = reasonerFactory;
        this.axiom = entailment;
        this.useModularisation = useModularisation;
        this.timeOutMS = timeOutMS;
        this.seedSignature = new HashSet<OWLAxiom>();
        this.lastAxioms = new HashSet<OWLAxiom>();
        this.lastEntailingAxioms = new HashSet<OWLAxiom>();
        freshEntities = new HashSet<OWLEntity>();
        man = OWLManager.createOWLOntologyManager();

        if (entailment instanceof OWLSubClassOfAxiom && 
        		((OWLSubClassOfAxiom) entailment).getSuperClass().isOWLNothing()) {
            unsatDesc = ((OWLSubClassOfAxiom) entailment).getSubClass();
        }
        else {
            SatisfiabilityConverter con = new SatisfiabilityConverter();
            unsatDesc = entailment.accept(con);
        }
	}

    private OWLOntologyManager man;

    private OWLAxiom axiom;

    private OWLClassExpression unsatDesc;

    private Set<OWLEntity> freshEntities;

    private OWLReasonerFactory reasonerFactory;

    private Set<OWLAxiom> seedSignature;

    private boolean useModularisation;

    final private Set<OWLAxiom> lastAxioms;

    final private Set<OWLAxiom> lastEntailingAxioms;

    private int counter = 0;

    private ModuleType moduleType = ModuleType.STAR;

    private long timeOutMS = Long.MAX_VALUE;
    
	@Override
    public boolean isEntailed(Set<OWLAxiom> axioms) {

        TelemetryTimer totalTimer = new TelemetryTimer();
        TelemetryTimer moduleTimer = new TelemetryTimer();
        TelemetryTimer entailmentCheckTimer = new TelemetryTimer();
        TelemetryInfo info = new DefaultTelemetryInfo("entailmentcheck", false, totalTimer, moduleTimer, entailmentCheckTimer);
        final TelemetryTransmitter transmitter = TelemetryTransmitter.getTransmitter();

        transmitter.beginTransmission(info);
        boolean entailed = true;
        OWLOntology ont = null;
        try {
//            transmitter.recordObject(info, "entailment", "", getEntailment());
            transmitter.recordMeasurement(info, "input size", axioms.size());
            totalTimer.start();

            lastEntailingAxioms.clear();
            lastAxioms.clear();
            lastAxioms.addAll(axioms);

            if (axioms.contains(axiom)) {
                lastEntailingAxioms.add(axiom);
                return true;
            }



            ont = man.createOntology(axioms);
            // Previously, I had coded the checker so that we broke out if the
            // signature of the unsatDesc was not totally contained in set of axioms.
            // However, if a GCI was in the set of axioms, for example an object
            // property domain checker, then this could cause erronous results.  We
            // now add in the signature using declaration axioms.

            for (OWLEntity ent : unsatDesc.getSignature()) {
                if (!ent.isBuiltIn()) {
                    if (!ont.containsEntityInSignature(ent)) {
                        man.addAxiom(ont, man.getOWLDataFactory().getOWLDeclarationAxiom(ent));
                    }
                }
            }
            String clsName = "Entailment" + System.currentTimeMillis();
            OWLClass namingCls = man.getOWLDataFactory().getOWLClass(IRI.create(clsName));
            OWLAxiom namingAxiom = man.getOWLDataFactory().getOWLSubClassOfAxiom(namingCls, unsatDesc);
            man.addAxiom(ont, namingAxiom);
            for (OWLEntity freshEntity : freshEntities) {
                man.addAxiom(ont, man.getOWLDataFactory().getOWLDeclarationAxiom(freshEntity));
            }

            // Do the actual entailment check
            counter++;
            entailmentCheckTimer.start();
			URL url = null;
			try {
				url = new URL("http://localhost:8082");
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}//Configure the server end-point
            OWLReasoner reasoner = reasonerFactory.createReasoner(ont, 
            		new OWLlinkReasonerConfiguration(url));
            entailed = !reasoner.isSatisfiable(unsatDesc);
            entailmentCheckTimer.stop();


            reasoner.dispose();
            man.removeOntology(ont);
            if (entailed) {
                lastEntailingAxioms.remove(namingAxiom);
                lastEntailingAxioms.addAll(ont.getLogicalAxioms());
            }
            return entailed;
        }
        catch (OWLOntologyCreationException e) {
            throw new ExplanationException(e);
        }
        catch (TimeOutException e) {
            transmitter.recordMeasurement(info, "reasoner time out", true);
            throw e;
        }
        catch (ExplanationGeneratorInterruptedException e) {
            transmitter.recordMeasurement(info, "interrupted", true);
            throw e;
        }
        catch (RuntimeException e) {
            try {
                if (ont != null) {
                    ont.getOWLOntologyManager().saveOntology(ont, new FileOutputStream(new File("/tmp/lasterror.owl")));
                }
            }
            catch (OWLOntologyStorageException e1) {
                e1.printStackTrace();
            }
            catch (FileNotFoundException e1) {
                e1.printStackTrace();
            }
            transmitter.recordException(info, e);
            throw e;
        }
        finally {
            totalTimer.stop();
            transmitter.recordTiming(info, "satisfiability check time", entailmentCheckTimer);
            transmitter.recordMeasurement(info, "entailed", entailed);
            transmitter.recordTiming(info, "time", totalTimer);
            transmitter.endTransmission(info);
        }
    }
	
    private class SatisfiabilityConverter implements OWLAxiomVisitorEx<OWLClassExpression> {

        private OWLDataFactory df = man.getOWLDataFactory();

        public OWLClassExpression visit(OWLAsymmetricObjectPropertyAxiom axiom) {
            throw new UnsupportedAxiomTypeException(axiom);
        }


        public OWLClassExpression visit(OWLClassAssertionAxiom axiom) {
            OWLClassExpression nominal = df.getOWLObjectOneOf(axiom.getIndividual());
            return df.getOWLObjectIntersectionOf(nominal, df.getOWLObjectComplementOf(axiom.getClassExpression()));
        }


        public OWLClassExpression visit(OWLDataPropertyAssertionAxiom axiom) {
            OWLClassExpression nom = df.getOWLObjectOneOf(axiom.getSubject());
            OWLClassExpression hasVal = df.getOWLDataHasValue(axiom.getProperty(), axiom.getObject());
            return df.getOWLObjectIntersectionOf(nom, df.getOWLObjectComplementOf(hasVal));
        }


        public OWLClassExpression visit(OWLDataPropertyDomainAxiom axiom) {
            OWLClassExpression exists = df.getOWLDataSomeValuesFrom(axiom.getProperty(), df.getTopDatatype());
            return df.getOWLObjectIntersectionOf(exists, df.getOWLObjectComplementOf(axiom.getDomain()));
        }


        public OWLClassExpression visit(OWLDataPropertyRangeAxiom axiom) {
            OWLClassExpression forall = df.getOWLDataAllValuesFrom(axiom.getProperty(), axiom.getRange());
            return df.getOWLObjectIntersectionOf(df.getOWLThing(), df.getOWLObjectComplementOf(forall));
        }


        public OWLClassExpression visit(OWLSubDataPropertyOfAxiom axiom) {
            OWLLiteral c = df.getOWLLiteral("x");
            OWLClassExpression subHasValue = df.getOWLDataHasValue(axiom.getSubProperty(), c);
            OWLClassExpression supHasValue = df.getOWLDataHasValue(axiom.getSuperProperty(), c);
            return df.getOWLObjectIntersectionOf(subHasValue, df.getOWLObjectComplementOf(supHasValue));
        }


        public OWLClassExpression visit(OWLDeclarationAxiom axiom) {
            throw new UnsupportedAxiomTypeException(axiom);
        }


        public OWLClassExpression visit(OWLDifferentIndividualsAxiom axiom) {
            throw new UnsupportedAxiomTypeException(axiom);
        }


        public OWLClassExpression visit(OWLDisjointClassesAxiom axiom) {
            return df.getOWLObjectIntersectionOf(axiom.getClassExpressions());
        }


        public OWLClassExpression visit(OWLDisjointDataPropertiesAxiom axiom) {
            throw new UnsupportedAxiomTypeException(axiom);
        }


        public OWLClassExpression visit(OWLDisjointObjectPropertiesAxiom axiom) {
            throw new UnsupportedAxiomTypeException(axiom);
        }


        public OWLClassExpression visit(OWLDisjointUnionAxiom axiom) {
            throw new UnsupportedAxiomTypeException(axiom);
        }


        public OWLClassExpression visit(OWLAnnotationAssertionAxiom axiom) {
            throw new UnsupportedAxiomTypeException(axiom);
        }


        public OWLClassExpression visit(OWLEquivalentClassesAxiom axiom) {
            if (axiom.getClassExpressions().size() != 2) {
                throw new UnsupportedAxiomTypeException(axiom);
            }

            OWLClassExpression[] descs = axiom.getClassExpressions().toArray(new OWLClassExpression[2]);
            OWLClassExpression d1 = descs[0];
            OWLClassExpression d2 = descs[1];

            if (d1.isOWLNothing()) {
                return d2;
            }
            else if (d2.isOWLNothing()) {
                return d1;
            }
            else if (d1.isOWLThing()) {
                return df.getOWLObjectComplementOf(d2);
            }
            else if (d2.isOWLThing()) {
                return df.getOWLObjectComplementOf(d1);
            }
            else {
                return df.getOWLObjectUnionOf(df.getOWLObjectIntersectionOf(d1, df.getOWLObjectComplementOf(d2)), df.getOWLObjectIntersectionOf(df.getOWLObjectComplementOf(d1), d2));
            }
        }


        public OWLClassExpression visit(OWLEquivalentDataPropertiesAxiom axiom) {
            throw new UnsupportedAxiomTypeException(axiom);
        }


        public OWLClassExpression visit(OWLEquivalentObjectPropertiesAxiom axiom) {
            throw new UnsupportedAxiomTypeException(axiom);
        }


        public OWLClassExpression visit(OWLFunctionalDataPropertyAxiom axiom) {
            return axiom.asOWLSubClassOfAxiom().accept(this);
        }


        public OWLClassExpression visit(OWLFunctionalObjectPropertyAxiom axiom) {
            return axiom.asOWLSubClassOfAxiom().accept(this);
        }

        public OWLClassExpression visit(OWLInverseFunctionalObjectPropertyAxiom axiom) {
            return axiom.asOWLSubClassOfAxiom().accept(this);
        }


        public OWLClassExpression visit(OWLInverseObjectPropertiesAxiom axiom) {
            OWLClass clsA = df.getOWLClass(IRI.create("owlapi:explanation:clsA"));
            freshEntities.add(clsA);
            OWLClass clsB = df.getOWLClass(IRI.create("owlapi:explanation:clsB"));
            freshEntities.add(clsB);
            OWLClassExpression subHasValueA = df.getOWLObjectSomeValuesFrom(axiom.getFirstProperty(), clsA);
            OWLClassExpression supHasValueA = df.getOWLObjectSomeValuesFrom(axiom.getSecondProperty().getInverseProperty(), clsA);
            OWLClassExpression subHasValueB = df.getOWLObjectSomeValuesFrom(axiom.getSecondProperty(), clsB);
            OWLClassExpression supHasValueB = df.getOWLObjectSomeValuesFrom(axiom.getFirstProperty().getInverseProperty(), clsB);
            OWLClassExpression ceA = df.getOWLObjectIntersectionOf(subHasValueA, df.getOWLObjectComplementOf(supHasValueA));
            OWLClassExpression ceB = df.getOWLObjectIntersectionOf(subHasValueB, df.getOWLObjectComplementOf(supHasValueB));
            return df.getOWLObjectUnionOf(ceA, ceB);
        }


        public OWLClassExpression visit(OWLIrreflexiveObjectPropertyAxiom axiom) {
            return axiom.asOWLSubClassOfAxiom().accept(this);
        }


        public OWLClassExpression visit(OWLNegativeDataPropertyAssertionAxiom axiom) {
            return axiom.asOWLSubClassOfAxiom().accept(this);
        }


        public OWLClassExpression visit(OWLNegativeObjectPropertyAssertionAxiom axiom) {
            return axiom.asOWLSubClassOfAxiom().accept(this);
        }


        public OWLClassExpression visit(OWLHasKeyAxiom owlHasKeyAxiom) {
            throw new UnsupportedAxiomTypeException(axiom);
        }


        public OWLClassExpression visit(OWLDatatypeDefinitionAxiom owlDatatypeDefinition) {
            throw new UnsupportedAxiomTypeException(axiom);
        }


        public OWLClassExpression visit(OWLSubAnnotationPropertyOfAxiom owlSubAnnotationPropertyOfAxiom) {
            throw new UnsupportedAxiomTypeException(axiom);
        }


        public OWLClassExpression visit(OWLAnnotationPropertyDomainAxiom owlAnnotationPropertyDomainAxiom) {
            throw new UnsupportedAxiomTypeException(axiom);
        }


        public OWLClassExpression visit(OWLAnnotationPropertyRangeAxiom owlAnnotationPropertyRangeAxiom) {
            throw new UnsupportedAxiomTypeException(axiom);
        }


        public OWLClassExpression visit(OWLObjectPropertyAssertionAxiom axiom) {
            OWLClassExpression nom = df.getOWLObjectOneOf(axiom.getSubject());
            OWLClassExpression hasVal = df.getOWLObjectHasValue(axiom.getProperty(), axiom.getObject());
            return df.getOWLObjectIntersectionOf(nom, df.getOWLObjectComplementOf(hasVal));
        }


        public OWLClassExpression visit(OWLSubPropertyChainOfAxiom axiom) {
            throw new UnsupportedAxiomTypeException(axiom);
        }


        public OWLClassExpression visit(OWLObjectPropertyDomainAxiom axiom) {
            OWLClassExpression exists = df.getOWLObjectSomeValuesFrom(axiom.getProperty(), df.getOWLThing());
            return df.getOWLObjectIntersectionOf(exists, df.getOWLObjectComplementOf(axiom.getDomain()));
        }


        public OWLClassExpression visit(OWLObjectPropertyRangeAxiom axiom) {
            OWLClassExpression forall = df.getOWLObjectAllValuesFrom(axiom.getProperty(), axiom.getRange());
            return df.getOWLObjectIntersectionOf(df.getOWLThing(), df.getOWLObjectComplementOf(forall));
        }


        public OWLClassExpression visit(OWLSubObjectPropertyOfAxiom axiom) {
            OWLClass clsA = df.getOWLClass(IRI.create("owlapi:explanation:clsA"));
            freshEntities.add(clsA);
            OWLClassExpression subHasValue = df.getOWLObjectSomeValuesFrom(axiom.getSubProperty(), clsA);
            OWLClassExpression supHasValue = df.getOWLObjectSomeValuesFrom(axiom.getSuperProperty(), clsA);
            return df.getOWLObjectIntersectionOf(subHasValue, df.getOWLObjectComplementOf(supHasValue));
        }


        public OWLClassExpression visit(OWLReflexiveObjectPropertyAxiom axiom) {
            return df.getOWLObjectHasSelf(axiom.getProperty()).getObjectComplementOf();
        }


        public OWLClassExpression visit(OWLSameIndividualAxiom axiom) {
            throw new UnsupportedAxiomTypeException(axiom);
        }


        public OWLClassExpression visit(OWLSubClassOfAxiom axiom) {
            return man.getOWLDataFactory().getOWLObjectIntersectionOf(axiom.getSubClass(), man.getOWLDataFactory().getOWLObjectComplementOf(axiom.getSuperClass()));
        }


        public OWLClassExpression visit(OWLSymmetricObjectPropertyAxiom axiom) {
            throw new UnsupportedAxiomTypeException(axiom);
        }


        public OWLClassExpression visit(OWLTransitiveObjectPropertyAxiom axiom) {
            OWLClass clsA = df.getOWLClass(IRI.create("owlapi:explanation:clsA"));
            freshEntities.add(clsA);
            OWLClassExpression subHasValue = df.getOWLObjectSomeValuesFrom(axiom.getProperty(), df.getOWLObjectSomeValuesFrom(axiom.getProperty(), clsA));
            OWLClassExpression supHasValue = df.getOWLObjectSomeValuesFrom(axiom.getProperty(), clsA);
            return df.getOWLObjectIntersectionOf(subHasValue, df.getOWLObjectComplementOf(supHasValue));
        }


        public OWLClassExpression visit(SWRLRule rule) {
            throw new UnsupportedAxiomTypeException(rule);
        }
    }

    public String getModularisationTypeDescription() {
        return moduleType.toString();
    }

    public boolean isUseModularisation() {
        return useModularisation;
    }

    public OWLAxiom getEntailment() {
        return axiom;
    }


    public Set<OWLEntity> getEntailmentSignature() {
        return axiom.getSignature();
    }


    public int getCounter() {
        return counter;
    }

    public void resetCounter() {
        counter = 0;
    }

    public Set<OWLEntity> getSeedSignature() {
        if (axiom instanceof OWLSubClassOfAxiom) {
            return ((OWLSubClassOfAxiom) axiom).getSubClass().getSignature();
        }
        else {
            return axiom.getSignature();
        }
    }

    public Set<OWLAxiom> getModule(Set<OWLAxiom> axioms) {

        if (useModularisation) {
//            Thread.dumpStack();
            if (axioms.isEmpty()) {
                return Collections.emptySet();
            }
            Set<OWLAxiom> inputAxioms = null;
//            if (getEntailment() instanceof OWLSubClassOfAxiom) {
//                boolean containsNominals = false;
//                AxiomsSplitter splitter = new AxiomsSplitter(axioms.size());
//                for(OWLAxiom ax : axioms) {
//                    ax.accept(splitter);
//                    if(containsNominals(ax)) {
//                        containsNominals = true;
//                        break;
//                    }
//                }
//                if(containsNominals) {
//                    inputAxioms = axioms;
//                }
//                else {
//                    inputAxioms = splitter.tboxAxioms;
//                }
//            }
//            else {
                inputAxioms = axioms;
//            }

            OWLOntologyManager man2 = OWLManager.createOWLOntologyManager();
            moduleType = ModuleType.STAR;
            SyntacticLocalityModuleExtractor extractor = new SyntacticLocalityModuleExtractor(man2, null, inputAxioms, moduleType);
            Set<OWLAxiom> module = extractor.extract(getEntailmentSignature());
            return module;
        }
        else {
            return axioms;
        }
    }
    
    public Set<OWLAxiom> getEntailingAxioms(Set<OWLAxiom> axioms) {
        if (!axioms.equals(lastAxioms)) {
            isEntailed(axioms);
        }
        return lastEntailingAxioms;
    }

}
