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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import auxStructures.RepairStatus;

import repair.MASRepair;
import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;
import util.OntoUtil;

import enumerations.REPAIR_METHOD;
import enumerations.VIOL_KIND;

public class TestMASRepairDetection1 {

	@Test
	public void test() throws Exception {
		
		System.out.println("Test 1 start");
		
		List<VIOL_KIND> violKinds = new ArrayList<>();
		violKinds.add(VIOL_KIND.EQONLY);
		violKinds.add(VIOL_KIND.APPROX);
//		violKinds.add(VIOL_KIND.FULL);

		String [] ontoPaths = new String[2];
		String alignPath = null;

		ontoPaths[0] = "testdataset/onto/cmt.owl"; 
		ontoPaths[1] = "testdataset/onto/confof.owl";
		alignPath = "testdataset/align/cmt-confof.rdf";
		
		OWLOntologyManager manager = OntoUtil.getManager(true);
				
		OWLOntology o1 = null, o2 = null;
		try {
			o1 = OntoUtil.load(ontoPaths[0], true, manager);
			o2 = OntoUtil.load(ontoPaths[1], true, manager);
		} catch (OWLOntologyCreationException e) {
			fail("Exception while loading input ontologies: " + e.getMessage());
		}

		boolean fstOnto = true;
		boolean repairAlso = true;

		MASRepair masRepair = null;
		try {
			masRepair = new MASRepair(fstOnto ? o1 : o2, 
					fstOnto ? o2 : o1, repairAlso, fstOnto);
		} catch (OWLOntologyCreationException e) {
			fail("Exception while creating the \"hidden\" ontology: " + e.getMessage());
		}

		List<MappingObjectStr> mappings = masRepair.loadMappings(alignPath);

		// no violations
		Set<MappingObjectStr> actualMappings = new HashSet<>(mappings.subList(0, 6));
		
		RepairStatus rs = masRepair.assessMapping(manager, actualMappings, 
				mappings.get(7), false, REPAIR_METHOD.SUB, violKinds);
		
		if(rs.hasViolations())
			fail("Violations should not have been detected");
		
		System.out.println("Test 1 end");
	}
}
