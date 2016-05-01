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

import org.semanticweb.owl.explanation.impl.blackbox.EntailmentChecker;
import org.semanticweb.owl.explanation.impl.blackbox.EntailmentCheckerFactory;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

public class OWLLinkSatisfiabilityEntailmentCheckerFactory implements
		EntailmentCheckerFactory<OWLAxiom> {
    private OWLReasonerFactory reasonerFactory;

    private boolean useModularisation;

    private long entailmentCheckTimeOutMS = Long.MAX_VALUE;

    public OWLLinkSatisfiabilityEntailmentCheckerFactory(
    		OWLReasonerFactory reasonerFactory) {
        this(reasonerFactory, true);
    }

    public OWLLinkSatisfiabilityEntailmentCheckerFactory(
    		OWLReasonerFactory reasonerFactory, long entailmentCheckTimeOutMS) {
        this.reasonerFactory = reasonerFactory;
        this.entailmentCheckTimeOutMS = entailmentCheckTimeOutMS;
        this.useModularisation = true;
    }

    public OWLLinkSatisfiabilityEntailmentCheckerFactory(
    		OWLReasonerFactory reasonerFactory, boolean useModularisation) {
        this.reasonerFactory = reasonerFactory;
        this.useModularisation = useModularisation;
    }

    public OWLLinkSatisfiabilityEntailmentCheckerFactory(
    		OWLReasonerFactory reasonerFactory, boolean useModularisation, long entailmentCheckTimeOutMS) {
        this.reasonerFactory = reasonerFactory;
        this.useModularisation = useModularisation;
        this.entailmentCheckTimeOutMS = entailmentCheckTimeOutMS;
    }

    public EntailmentChecker<OWLAxiom> createEntailementChecker(OWLAxiom entailment) {
        return new OWLLinkSatisfiabilityEntailmentChecker(
        		reasonerFactory, entailment, useModularisation, 
        		entailmentCheckTimeOutMS);
    }
}
