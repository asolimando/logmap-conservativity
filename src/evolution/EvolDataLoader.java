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
package evolution;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import logmap.LogMapWrapper;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.xml.sax.helpers.DefaultHandler;

import repair.ConservativityRepairFacility;

import enumerations.REASONER_KIND;
import enumerations.VIOL_KIND;

import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;
import util.FileUtil;
import util.OntoUtil;
import util.Params;
import util.Util;

public class EvolDataLoader {
	
	private String onto1Path, onto2Path;
	private String alignPath;
	private Set<OWLAxiom> mappings = new HashSet<>();
	
	public EvolDataLoader(String onto1Path, String onto2Path, String alignPath){
		this.onto1Path = onto1Path;
		this.onto2Path = onto2Path;
		this.alignPath = alignPath;
	}
	
	public void loadData(){
		
		if(onto1Path == null || onto2Path == null || alignPath == null)
			return;
		
		System.out.println("Onto1: " + onto1Path);
		System.out.println("Onto2: " + onto2Path);
		System.out.println("Alignment: " + alignPath);
		
		System.out.println("START: " + Util.getCurrTime());
		
		try {
			// Use an instance of ourselves as the SAX event handler
			DefaultHandler handler = new MappingParser(alignPath);
			// Parse the input with the default (non-validating) parser
			SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
			saxParser.parse( new File(alignPath), handler );

//			System.out.println(((MappingParser)handler).mappingToImport);
			OWLOntologyManager manager = OntoUtil.getManager(true);
			OWLOntology o1 = OntoUtil.load(onto1Path, true, manager), 
					o2 = OntoUtil.load(onto2Path, true, manager);

			Set<OWLClass> globalSignature = new HashSet<>(o1.getClassesInSignature(true));
			globalSignature.addAll(o2.getClassesInSignature(true));
			
			int probl = 0;
			String oboIRI = OntoUtil.extractOBOOntologyIRI(o1);
			OWLAxiom ax = null;
			
			for (ImportCorrespondence corr : 
				((MappingParser)handler).mappingToImport.correspondences) {
				ax = corr.toOWLAxiom(oboIRI, oboIRI);
				mappings.add(ax);
				
				if(!globalSignature.containsAll(ax.getSignature())){
					System.out.println(o1.getClassesInSignature(false).iterator().next());
					probl++;
					System.out.println(corr.toOWLAxiom(oboIRI, oboIRI));
				}
			}

			System.out.println(((MappingParser)handler).mappingToImport.correspondences.size() 
					+ " mapping(s) loaded");
			if(probl > 0)
				System.out.println(probl + " mapping(s) involving unknown entities");
						
			ConservativityRepairFacility repairFac = new ConservativityRepairFacility(o1, o2, manager, 
					((MappingParser)handler).mappingToImport.convertToAlignment(oboIRI));
			
			repairFac.setMappingFile(alignPath);
//			repairFac.enableSaveOntologies();
			repairFac.enableSaveMappings();
			repairFac.repair(false);
			
			System.out.println("Number of initial mapping(s): " + 
					LogMapWrapper.countMappings(repairFac.getOriginalMappings()));
			System.out.println("Number of removed mapping(s): " + 
					LogMapWrapper.countMappings(repairFac.getRepair()));
			System.out.println("Number of final mapping(s): " + 
					LogMapWrapper.countMappings(repairFac.getRepairedMappings()));
			
			System.out.print("Total violation(s) approximated notion: ");
			System.out.print(
					repairFac.getTotalInitialViolationNumber(VIOL_KIND.APPROX, false) + " -> ");
			System.out.println(
					repairFac.getTotalActualStepViolationNumber(VIOL_KIND.APPROX, false));

			System.out.print("Total violation(s) full notion: ");
			System.out.print(
					repairFac.getTotalInitialViolationNumber(VIOL_KIND.FULL, false) + " -> ");
			System.out.println(
					repairFac.getTotalActualStepViolationNumber(VIOL_KIND.FULL, false));

			System.out.print("Total violation(s) for equivalence only: ");
			System.out.print(
					repairFac.getTotalInitialViolationNumber(VIOL_KIND.EQONLY, false) + " -> ");
			System.out.println(
					repairFac.getTotalActualStepViolationNumber(VIOL_KIND.EQONLY, false));
			Set<MappingObjectStr> mappings = repairFac.getRepairedMappings();
			
			String outDir = "test/testEvo/outputMapping/";
			FileUtil.createDirPath(outDir);
			
			ExportMapping expMapping = new ExportMapping(outDir + 
					alignPath.substring(alignPath.lastIndexOf("/")+1), mappings);
			
			expMapping.export("2014-05");
			
			OntoUtil.disposeAllReasoners();
			OntoUtil.unloadAllOntologies(manager);
			
		} catch( Throwable t ) {
			t.printStackTrace();
			OntoUtil.disposeAllReasoners();
			OntoUtil.unloadAllOntologies();
		}
		
		System.out.println("END: " + Util.getCurrTime());
	}
}
