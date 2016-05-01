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
package junit;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasoner;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.elk.owlapi.proofs.Proofs;
import org.semanticweb.elk.reasoner.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapitools.proofs.ExplainingOWLReasoner;
import org.semanticweb.owlapitools.proofs.exception.ProofGenerationException;

public class ELKTracingUnitTest {

	private final static OWLDataFactory factory = OWLManager.getOWLDataFactory();
	private final static OWLOntologyManager manager = OWLManager.createOWLOntologyManager(factory);
	
	@Test
	public void inferenceFail() throws InterruptedException, 
		OWLOntologyCreationException, ProofGenerationException {

		String iastedPrefixStr = "iasted:";
		String conferencePrefixStr = "conference:";
		
		DefaultPrefixManager prefManager = new DefaultPrefixManager();
		prefManager.setPrefix(iastedPrefixStr,"http://iasted/");
		prefManager.setPrefix(conferencePrefixStr,"http://conference/");

		OWLClass iastedTutorial = 
				factory.getOWLClass(IRI.create(prefManager.getPrefix(iastedPrefixStr), "Tutorial"));
		OWLClass conferenceTutorial = 
				factory.getOWLClass(IRI.create(prefManager.getPrefix(conferencePrefixStr), "Tutorial"));
		OWLClass conferenceConferencePart = 
				factory.getOWLClass(IRI.create(prefManager.getPrefix(conferencePrefixStr), "Conference_part"));
		OWLClass iastedMoney = 
				factory.getOWLClass(IRI.create(prefManager.getPrefix(iastedPrefixStr), "Money"));
		
		OWLSubClassOfAxiom sub1 = factory.getOWLSubClassOfAxiom(iastedTutorial,conferenceTutorial);
		OWLSubClassOfAxiom sub2 = factory.getOWLSubClassOfAxiom(conferenceTutorial,conferenceConferencePart);
		OWLSubClassOfAxiom sub3 = factory.getOWLSubClassOfAxiom(conferenceConferencePart,iastedMoney);
		
		Set<OWLAxiom> axioms = new HashSet<>();
		axioms.add(sub1);
		axioms.add(sub2);
		axioms.add(sub3);

		OWLOntology onto = manager.createOntology(axioms);

		System.out.println("Proof for <SubClassOf(<http://iasted#Tutorial> <http://iasted#Money>)>");

		ElkReasoner elkReasoner = 
				(ElkReasoner) new ElkReasonerFactory().createReasoner(onto);
		
		OWLSubClassOfAxiom inf1 = factory.getOWLSubClassOfAxiom(iastedTutorial,iastedMoney);
		
		Set<OWLAxiom> singleAxioms = 
				Proofs.getUsedAxioms((ExplainingOWLReasoner) elkReasoner,inf1,false);

		Set<OWLAxiom> allAxioms = 
				Proofs.getUsedAxioms((ExplainingOWLReasoner) elkReasoner,inf1,true);
		
		printSets(singleAxioms, allAxioms, onto.getAxioms());
		
		// expected result:
		// 		Explanation for <SubClassOf(<http://iasted#Tutorial> <http://iasted#Money>)>
		//		SubClassOf("<http://iasted#Tutorial>" "<http://conference#Tutorial>")
		//		SubClassOf("<http://conference#Tutorial>" "<http://conference#Conference_part>")
		//		SubClassOf("<http://conference#Conference_part>" "<http://iasted#Money>")

		elkReasoner.dispose();
		
		// only one proof/justification, they should coincide 
		assertTrue(singleAxioms.containsAll(allAxioms) && allAxioms.containsAll(singleAxioms));
				
		assertTrue(singleAxioms.containsAll(onto.getAxioms()) && onto.getAxioms().containsAll(singleAxioms));

		assertTrue(allAxioms.containsAll(onto.getAxioms()) && onto.getAxioms().containsAll(allAxioms));
	}

	@Test
	public void inferenceFine() throws InterruptedException, 
		OWLOntologyCreationException, ProofGenerationException {

		String cmtPrefixStr = "cmt:";
		String ekawPrefixStr = "ekaw:";

		DefaultPrefixManager prefManager = new DefaultPrefixManager();
		prefManager.setPrefix(cmtPrefixStr,"http://cmt/");
		prefManager.setPrefix(ekawPrefixStr,"http://ekaw/");
		
		OWLClass cmtMetaReviewer = 
				factory.getOWLClass(IRI.create(prefManager.getPrefix(cmtPrefixStr), 
						"Meta-Reviewer"));
		OWLClass ekawPCMember = 
				factory.getOWLClass(IRI.create(prefManager.getPrefix(ekawPrefixStr), 
						"PC_Member"));
		OWLClass ekawPossibleReviewer = 
				factory.getOWLClass(IRI.create(prefManager.getPrefix(ekawPrefixStr), 
						"Possible_Reviewer"));
		OWLClass cmtExternalReviewer = 
				factory.getOWLClass(IRI.create(prefManager.getPrefix(cmtPrefixStr), 
						"ExternalReviewer"));
		OWLClass cmtReviewer = 
				factory.getOWLClass(IRI.create(prefManager.getPrefix(cmtPrefixStr), 
						"Reviewer"));
		
		OWLSubClassOfAxiom sub1 = 
				factory.getOWLSubClassOfAxiom(cmtMetaReviewer,ekawPCMember);
		OWLSubClassOfAxiom sub2 = 
				factory.getOWLSubClassOfAxiom(ekawPCMember,ekawPossibleReviewer);
		OWLSubClassOfAxiom sub3 = 
				factory.getOWLSubClassOfAxiom(ekawPossibleReviewer,cmtExternalReviewer);
		OWLSubClassOfAxiom sub4 = 
				factory.getOWLSubClassOfAxiom(cmtMetaReviewer,cmtReviewer);
		OWLDisjointClassesAxiom disj1 = 
				factory.getOWLDisjointClassesAxiom(cmtMetaReviewer,cmtReviewer);
		
		Set<OWLAxiom> axioms = new HashSet<>();
		axioms.add(sub1);
		axioms.add(sub2);
		axioms.add(sub3);
		axioms.add(sub4);
		axioms.add(disj1);
		OWLOntology onto = manager.createOntology(axioms);

		System.out.println("Proof for <SubClassOf(<http://cmt#Meta-Reviewer> owl:Nothing)>");
		
		ElkReasoner elkReasoner = 
				(ElkReasoner) new ElkReasonerFactory().createReasoner(onto);
		
		OWLSubClassOfAxiom inf1 = 
				factory.getOWLSubClassOfAxiom(cmtMetaReviewer,factory.getOWLNothing());

		Set<OWLAxiom> singleAxioms = 
				Proofs.getUsedAxioms((ExplainingOWLReasoner) elkReasoner,inf1,false);

		Set<OWLAxiom> allAxioms = 
				Proofs.getUsedAxioms((ExplainingOWLReasoner) elkReasoner,inf1,true);

		printSets(singleAxioms, allAxioms, onto.getAxioms());

		// expected result:
		//[Explanation <SubClassOf(<http://cmt#Meta-Reviewer> owl:Nothing)>
		//			SubClassOf(<http://cmt#Meta-Reviewer> <http://ekaw#PC_Member>)
		//			SubClassOf(<http://ekaw#PC_Member> <http://ekaw#Possible_Reviewer>)
		//			SubClassOf(<http://ekaw#Possible_Reviewer> <http://cmt#ExternalReviewer>)
		//			SubClassOf(<http://cmt#Meta-Reviewer> <http://cmt#Reviewer>)
		//			DisjointClasses(<http://cmt#ExternalReviewer> <http://cmt#Reviewer>)
		
		elkReasoner.dispose();
		
		// only one proof/justification, they should coincide 
		assertTrue(singleAxioms.containsAll(allAxioms) && allAxioms.containsAll(singleAxioms));
				
		assertTrue(singleAxioms.containsAll(onto.getAxioms()) && onto.getAxioms().containsAll(singleAxioms));

		assertTrue(allAxioms.containsAll(onto.getAxioms()) && onto.getAxioms().containsAll(allAxioms));
	}	
	
	private static void printSets(Set<OWLAxiom> singleAxioms, Set<OWLAxiom> allAxioms, Set<OWLAxiom> expectedAxioms){
		System.out.println("Single proof axioms: " + singleAxioms.toString().replace("[", "[\n").replace(", ", ",\n") + "\n");
		System.out.println("All proofs axioms: " + allAxioms.toString().replace("[", "[\n").replace(", ", ",\n") + "\n");
		System.out.println("Expected axioms: " + expectedAxioms.toString().replace("[", "[\n").replace(", ", ",\n") + "\n\n");
	}
}
