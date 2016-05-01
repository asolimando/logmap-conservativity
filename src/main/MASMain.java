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
package main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import logmap.LogMapWrapper;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import repair.ConservativityRepairFacility;

import enumerations.REASONER_KIND;
import enumerations.VIOL_KIND;

import uk.ac.manchester.cs.owlapi.modularity.ModuleType;
import uk.ac.manchester.syntactic_locality.ModuleExtractor;
import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;
import util.OntoUtil;
import util.Params;

public class MASMain {

	/**
	 * @param args
	 * @throws OWLOntologyCreationException 
	 * @throws OWLOntologyStorageException 
	 */
	public static void main(String[] args) throws OWLOntologyCreationException, 
		OWLOntologyStorageException {

//		Params.storeViolations = true;
		
		// add the violations kind you want to check
		List<VIOL_KIND> violKinds = new ArrayList<>();
		violKinds.add(VIOL_KIND.EQONLY);
		violKinds.add(VIOL_KIND.APPROX);
//		violKinds.add(VIOL_KIND.FULL);
		
		OWLOntologyManager managerTmp = OntoUtil.getManager(false), 
				manager = OntoUtil.getManager(true);
		OWLOntology emptyOnto;
		
		String [] ontoPaths = {"/home/ale/data/oaei2013/conference/onto/cmt.owl", 
				"/home/ale/data/oaei2013/conference/onto/confof.owl"};
		
		// load the two ontologies (the other only needed for the signature)
		OWLOntology o1 = OntoUtil.load(ontoPaths[0], true, managerTmp);
		OWLOntology o2 = OntoUtil.load(ontoPaths[1], true, managerTmp);
		
		String align = "/home/ale/data/oaei2013/conference/reference/cmt-confof.rdf";
			
		List<OWLOntology> ontos = new ArrayList<>(2);
		ontos.add(o1);
		ontos.add(o2);
				
		ConservativityRepairFacility repair;
		boolean fstOnto;
		
		for (OWLOntology o : ontos) {
			
			System.out.println("Known ontology IRI: " + 
					OntoUtil.getIRIShortFragment(o.getOntologyID().getOntologyIRI()));
			
			fstOnto = o1.equals(o);

			// create fresh ontology having only the signature 
			// (otherwise the mappings will be discarded by the sanity check) 
			emptyOnto = manager.createOntology(fstOnto ? o2.getOntologyID() : 
				o1.getOntologyID());
			Set<MappingObjectStr> safeMappings = new HashSet<>();

			// load mapping and sort them by confidence (desc)
			List<MappingObjectStr> mappings = 
					new ArrayList<>(LogMapWrapper.getMappings(align, o1, o2));
			Collections.sort(mappings, new Comparator<MappingObjectStr>() {
				   public int compare(MappingObjectStr m1, MappingObjectStr m2) {
				      if(m1.getConfidence() > m2.getConfidence())
				    	  return 1; 
				      if(m1.getConfidence() < m2.getConfidence())
				    	  return -1; 			      
				      return 0;
				   }
				});
			
			Iterator<MappingObjectStr> itr = mappings.iterator();
			MappingObjectStr m = null;
						
			while(itr.hasNext()){
				
				m = itr.next();
				int viols = 0;
				
				OntoUtil.addEntityDeclarationToOntology(emptyOnto, manager, 
						LogMapWrapper.getEntity(!fstOnto, m));
				
				System.out.println("\nTesting mapping: " + m);
				
				System.out.println("Actual signature: " + emptyOnto.getSignature());
				
				System.out.println("Actual size of the set of safe mappings: " + 
						LogMapWrapper.countMappings(safeMappings));

				if(fstOnto)
					repair = new ConservativityRepairFacility(o, emptyOnto, 
							manager, safeMappings, false);
				else
					repair = new ConservativityRepairFacility(emptyOnto, o, 
							manager, safeMappings, false);

				for (VIOL_KIND vk : violKinds)
					viols += repair.getTotalViolationNumber(vk, 
							repair.getRepairStep(), false);
				
				if(viols > 0){
					OntoUtil.removeEntityDeclarationToOntology(emptyOnto, 
							manager, LogMapWrapper.getEntity(!fstOnto, m));
					
					System.out.println("Mapping " + m + " would cause " + 
						viols + " violation(s)");
				}
//				else if(!repair.getRepairedMappings().contains(m))
//					System.out.println("Mapping " + m + 
//							" presents incompatibilities and is discarded");
				else {
					safeMappings.add(m);
					System.out.println("Mapping " + m + 
							" added to the set of safe mappings");
				}
			}
		}
	}	
}
