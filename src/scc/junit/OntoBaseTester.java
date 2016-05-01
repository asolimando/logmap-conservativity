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

import static org.junit.Assert.*;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import scc.graphDataStructure.Diagnosis;
import scc.graphDataStructure.LightAdjacencyList;
import scc.graphDataStructure.LightEdge;
import scc.graphDataStructure.LightSCC;
import scc.graphDataStructure.LightSCCs;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntology;
import scc.util.LegacyFileUtil;
import util.OntoUtil;
import util.Params;
import util.Util;

public abstract class OntoBaseTester {

	private String onto1, onto2, align, baseSCCPath = "test_reference/", 
			sccPath;
	private OWLOntology fstO,sndO;
	private int numMappings, diagSize;
	
	public OntoBaseTester(String onto1, String onto2, String align, 
			int numMappings, int diagSize){
		this.onto1 = onto1;
		this.onto2 = onto2;
		this.align = align;
		this.sccPath = baseSCCPath + align.substring(0, 
				align.lastIndexOf('.')).substring(align.lastIndexOf('/')+1) + "/";
		this.numMappings = numMappings;
		this.diagSize = diagSize;
	}
	
	@Test
	public void test() {
		try {
			OntoUtil.getManager(true);
			System.out.println("\nPHASE 0: loading input ontologies");
			fstO = OntoUtil.load(onto1, true, OntoUtil.getManager(false));
			sndO = OntoUtil.load(onto2, true, OntoUtil.getManager(false));
			LightAdjacencyList adj = new LightAdjacencyList(fstO, sndO, null, 
					true);
			
			if(adj.checkDataStructureConsistency())
				fail("Data structures are inconsistent");

			System.out.println("\nPHASE 1: loading alignment");
//			LightOAEIMappingHandler alignH = adj.getOAEIHandler();
			Set<LightEdge> mappings = adj.loadMappings(new File(align), null, 
					Params.fullDetection);
			
			if(adj.checkDataStructureConsistency())
				fail("Data structures are inconsistent");
			
			if(mappings.size() != numMappings)
				fail("Expected # mappings was " + numMappings + ", found " 
						+ mappings.size());
			
			System.out.println("\nPHASE 2: diagnosis computation");
			Set<LightSCC> problematicSCCs = new HashSet<>();
			Diagnosis d = adj.computeDiagnosis(new LightSCCs(), mappings, 
					problematicSCCs, null, Util.getMSec());
			
			int i=0;
			File sccDir = new File(sccPath), sccFile;
			
			if(!sccDir.exists() || !sccDir.isDirectory())
				if(!sccDir.mkdirs())
					fail("Impossible to create scc dir " + sccDir.getName());
			
			for (LightSCC scc : problematicSCCs){
				sccFile = new File(sccPath + i + ".scc");
				if(sccFile.exists()){
					LightSCC scc2 = (LightSCC) LegacyFileUtil.deserializeObject(
							sccFile.getAbsolutePath());
					if(!scc.equals(scc2))
						fail("SCCs are different\n" + scc + "\n" + scc2);
				}
				else{
					LegacyFileUtil.serializeObject(sccFile.getAbsolutePath(), scc);
					if(!sccFile.exists())
						fail("Impossible to create file " + sccFile.getName());
				}
				++i;
			}
			
			if(d.size() != diagSize)
				fail("Expected diagnosis size was " + diagSize 
						+ ", found " + d.size());
			
			System.out.println("\nPHASE 3: testing diagnosis correctness");
			problematicSCCs.clear();
			adj.computeGlobalSCCsAndProblematicMappings(problematicSCCs, null);
			
			if(!problematicSCCs.isEmpty())
				fail("Diagnosis left unsolved " + problematicSCCs.size() 
						+ " problematic SCCs");
			
			System.out.println("Test succesfully ended");
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}