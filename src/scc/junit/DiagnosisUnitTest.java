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
package scc.junit;

import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;

import scc.graphDataStructure.Diagnosis;
import scc.graphDataStructure.LightEdge;
import scc.graphDataStructure.LightNode;
import util.OntoUtil;

public class DiagnosisUnitTest {
	
	public DiagnosisUnitTest(){
		
	}
	
	@Test
	public void test() {
		String prefixIRI = "http://diagnosis.com#";
		try {
			LightNode n1 = new LightNode(true,
					OntoUtil.getDataFactory().getOWLClass(IRI.create(prefixIRI + "src")),0);
			
			LightNode n2 = new LightNode(true,
					OntoUtil.getDataFactory().getOWLClass(IRI.create(prefixIRI + "dst")),1);
			
			Diagnosis d = new Diagnosis();
			testWeight(d,0);		
			testSize(d,0);	
			
			LightEdge e1 = new LightEdge(n1, n2, true, false, 0.1);
			LightEdge e2 = new LightEdge(n2, n1, true, false, 1);
			
			Set<LightEdge> set = new HashSet<>();
			set.add(e1);
			set.add(e2);
			
			d.add(e1);
			testWeight(d,0.1);
			testSize(d,1);	
			
			d.remove(e1);
			testWeight(d,0);		
			testSize(d,0);
			
			Diagnosis d2 = new Diagnosis(set);
			d.addAll(set);
			testWeight(d,d2.getWeight());		
			testSize(d,d2.size());
			
			d.removeAll(Collections.singleton(e1));
			testWeight(d,1);
			testSize(d,1);
			
			d.retainAll(Collections.singleton(e2));
			testWeight(d,1);
			testSize(d,1);
			
			d.add(null);
			testWeight(d,1);
			testSize(d,1);

			d.addAll(Collections.singleton(e1));
			testWeight(d,1.1);
			testSize(d,2);
			
			d.addAll(Collections.singleton(e1));			
			testWeight(d,1.1);
			testSize(d,2);
			
			d.retainAll(Collections.singleton(e2));
			testWeight(d,1);
			testSize(d,1);
			
			d.add(e2);
			testWeight(d,1);
			testSize(d,1);
			
			d.remove(null);
			testWeight(d,1);
			testSize(d,1);
			
			d.add(e1);
			testWeight(d,1.1);
			testSize(d,2);
			
			d.clear();
			testWeight(d,0);
			testSize(d,0);
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	private void testWeight(Diagnosis d, double expected){
		if(d.getWeight() != expected)
			fail("Expected diagnosis weight was " + expected 
					+ ", found " + d.getWeight());
	}
	
	private void testSize(Diagnosis d, int expected){
		if(d.size() != expected)
			fail("Expected diagnosis size was " + expected 
					+ ", found " + d.size());
	}
}