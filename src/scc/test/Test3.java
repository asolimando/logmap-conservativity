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
package scc.test;

import scc.exception.ClassificationTimeoutException;
import scc.graphDataStructure.Diagnosis;
import scc.graphDataStructure.LightAdjacencyList;
import scc.graphDataStructure.LightEdge;
import scc.graphDataStructure.LightSCC;
import scc.graphDataStructure.LightSCCs;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import scc.util.LegacyFileUtil;
import util.OntoUtil;
import util.Params;
import util.Util;

// class performing UMLS analysis
public class Test3 {
	public static Map<String, Double> stats = new HashMap<>();
	
	public static void main(String args[]) throws OWLOntologyCreationException, 
		IOException{
		
		Map<String, Map<String,Double>> generalStats = new HashMap<>();
		
		OntoUtil.getManager(true);
		
		LegacyFileUtil.createDirPath(Params.test3OutDir);
		
		preTest(2012,generalStats);
		preTest(2013,generalStats);
		preTest(2014,generalStats);
		
		LegacyFileUtil.printStatsTest1ToFile(Params.test3OutDir + "umls.text", 
				generalStats);
	}
	
	private static void preTest(int year, 
			Map<String, Map<String,Double>> generalStats) 
					throws OWLOntologyCreationException, IOException {

		String refDir = Params.dataFolder + "oaei" + year + "/largebio/reference/", 
				ontoDir = Params.dataFolder + "oaei" + year + "/largebio/onto/";
		
		OWLOntology fstO = null, sndO = null;
		String fstOnto = null, sndOnto = null;
		
		Map<String,String> refMap = Params.largebioRef;
		
		if(year == 2013)
			refMap = Params.largebioRef13;
		else if(year == 2014)
			refMap = Params.largebioRef14;
		
		for (String s : refMap.keySet()) {
			if(year!=2014 && !s.endsWith("original"))
				continue;
			boolean fma = s.contains("fma"), nci = s.contains("nci"), 
					snomed = s.contains("snomed");
			
			// oaei2012_NCI_whole_ontology.owl
			// oaei2012_FMA_whole_ontology.owl
			// oaei2012_SNOMED_extended_overlapping_fma_nci.owl
			
			if(fma && snomed){
				fstOnto = "oaei" + year + "_FMA_whole_ontology.owl";
				sndOnto = "oaei" + year + "_SNOMED_extended_overlapping_fma_nci.owl";
			}
			else if(nci && fma){
				fstOnto = "oaei" + year + "_FMA_whole_ontology.owl";
				sndOnto = "oaei" + year + "_NCI_whole_ontology.owl";
			}
			else if(nci && snomed){
				fstOnto = "oaei" + year + "_SNOMED_extended_overlapping_fma_nci.owl";
				sndOnto = "oaei" + year + "_NCI_whole_ontology.owl";
			}
			
			fstOnto = ontoDir + fstOnto;
			sndOnto = ontoDir + sndOnto;

			fstO = OntoUtil.load(fstOnto, true, OntoUtil.getManager(false));
			sndO = OntoUtil.load(sndOnto, true, OntoUtil.getManager(false));
			
			if(test(s + "_" + year, refDir+refMap.get(s), fstO, sndO, true) == null)
				continue;
			generalStats.put(s + " " + year, stats);
			stats = new HashMap<>();
			OntoUtil.disposeAllReasoners();
			OntoUtil.unloadAllOntologies();
		}

	}
	
	public static Diagnosis test(String refKey, String mappingPathname, 
			OWLOntology fstO, OWLOntology sndO, boolean unloadOnto) 
					throws OWLOntologyCreationException, IOException {		

		double totalStartTime = Util.getMSec();
		
		LightAdjacencyList adj;
		try {
			adj = new LightAdjacencyList(fstO, sndO, stats, 
					unloadOnto);
		} catch (ClassificationTimeoutException e) {
			System.err.println(e.getMessage());
			return null;
		}
		Set<LightEdge> mappings = 
				adj.loadMappings(new File(mappingPathname), stats, 
						Params.fullDetection);

		Set<LightSCC> problematicSCCs = new HashSet<>();

		Diagnosis d = adj.computeDiagnosis(new LightSCCs(), mappings, 
					problematicSCCs, stats, totalStartTime);
		
		StringBuilder buf = new StringBuilder();
		buf.append("Global Diagnosis: " + d + "\n\n");
		
		for (LightSCC s : problematicSCCs)
			buf.append("Local Diagnosis: " + s.selectSCCMappings(d) + "\n\n" +
					s.problematicSCCAsString(adj) + "\n\n");
		
		LegacyFileUtil.writeStringToFile(buf.toString(), 
				Params.test3OutDir + refKey + ".text");

		return d;
	}
}