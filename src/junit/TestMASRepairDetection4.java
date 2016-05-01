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

import logmap.LogMapWrapper;

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

public class TestMASRepairDetection4 {

	@Test
	public void test() throws Exception {

		System.out.println("Test 4 start");

		List<VIOL_KIND> violKinds = new ArrayList<>();
		violKinds.add(VIOL_KIND.EQONLY);

		String [] ontoPaths = new String[2];
		String alignPath = null;

		ontoPaths[0] = "testdataset/onto/confof.owl"; 
		ontoPaths[1] = "testdataset/onto/ekaw.owl";
		alignPath = "testdataset/align/confof-ekaw.rdf";
		
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
		
		System.out.println("SIZE: " + mappings.size());
		
		int id = 19;
		
		// 1 eq violation!
		Set<MappingObjectStr> actualMappings = new HashSet<>(mappings.subList(0, id+1));
		
		RepairStatus rs = masRepair.assessMapping(manager, actualMappings, 
				mappings.get(id), false, REPAIR_METHOD.EQ, violKinds);
				
		if(rs.getViolationsNumber(violKinds) != 1)
			fail("1 EQ violation expected, got " + 
					rs.getViolationsNumber(violKinds));

//		System.out.println("Repair of size " + 
//				LogMapWrapper.countMappings(rs.getRepair()) + 
//					": " + rs.getRepair());
		
//		if(LogMapWrapper.isContained(mappings.get(id), rs.getRepairedMappings()))
//			fail("Incompatible mapping should have been discarded: " + mappings.get(id));

		if(rs.getRepair().size() != 1)
			fail("Expected repair size is 1, found " + rs.getRepair().size());
		
		MappingObjectStr m = rs.getRepair().iterator().next();
		MappingObjectStr expM = null;		

		String javaVersion = System.getProperty("java.specification.version");
//		System.out.println("Java version JVM: " + javaVersion);

		// the solution is not unique!
		if(javaVersion.equals("1.7")){
			expM = 	new MappingObjectStr("http://confOf#Student", 
					"http://ekaw#Student", 1.0, MappingObjectStr.SUP, 
					MappingObjectStr.CLASSES);
		}
		else if(javaVersion.equals("1.8")){
			expM = 	new MappingObjectStr("http://confOf#Scholar", 
					"http://ekaw#Student", 1.0, MappingObjectStr.SUP, 
					MappingObjectStr.CLASSES);					
		}
		
		if(expM != null && 
				(!MappingObjectStr.doCoincide(m, expM) 
						|| m.getConfidence() != expM.getConfidence()))
			fail("Expected removed mapping is " + expM + ", found\n" + m);

		System.out.println("Test 4 end");
	}
}
