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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.xml.sax.helpers.DefaultHandler;

import enumerations.REASONER_KIND;

import auxStructures.Pair;

import util.OntoUtil;
import util.Params;

public class MainEvolution {

	private static boolean snomednci = false;
	private static boolean ncifma = true;
	
	// do not activate both, for ncifma gives error
	private static boolean keepDel = false;
	private static boolean reduced = true;
	
	// tests only adapted mappings (2012)
	private static boolean adaptedOnly = true;
	
	/**
	 * @param args
	 * @throws OWLOntologyCreationException 
	 */
	public static void main(String[] args) throws OWLOntologyCreationException {
		String basePath = Params.dataFolder + "evolution/";
		String baseOntoPath = basePath + "onto/escaped/";
		String baseAdaptedAlign = basePath + "adapted/";
		String baseExtractedAlign = basePath + "extracted/";
		String baseRefAlign = basePath + "reference/";

		String repairedSuffix = "_repaired";
		
//		String [] fstOntos = {"NCI-2010AAesc.obo","NCI-2011AAesc.obo","NCI-2012AAesc.obo"};
//		String [] sndOntos = {"UWDA-2010AAesc.obo","UWDA-2011AAesc.obo","UWDA-2012AAesc.obo"};
		
		Map<String,Pair<String>> mappingToOntoMap = new HashMap<>();
		
		String [] adaptedAligns = {"Adapted_NCI-FMA_2012-05.xml",
				"Adapted_NCI-FMA_2012-05_keepDelObs.xml",
				"Adapted_NCI-FMA_2012-05_keepDelObs_reduced.xml",
				"Adapted_NCI-FMA_2012-05_reduced.xml",
				"Adapted_SNOMEDCT-NCI_2012-05.xml",
				"Adapted_SNOMEDCT-NCI_2012-05_keepDelObs.xml",
				"Adapted_SNOMEDCT-NCI_2012-05_keepDelObs_reduced.xml",
				"Adapted_SNOMEDCT-NCI_2012-05_reduced.xml"};

		if(ncifma){
			Pair<String> ontos = new Pair<String>("NCI-2012AAesc.obo","UWDA-2012AAesc.obo");
			
			if(keepDel && reduced){
				mappingToOntoMap.put(baseAdaptedAlign + 
						"Adapted_NCI-FMA_2012-05_keepDelObs_reduced.xml", ontos);
				
				mappingToOntoMap.put(baseAdaptedAlign + 
						"Adapted_NCI-FMA_2012-05_keepDelObs_reduced" + repairedSuffix + ".xml", ontos);
			}
			else if(keepDel){
				mappingToOntoMap.put(baseAdaptedAlign + 
						"Adapted_NCI-FMA_2012-05_keepDelObs.xml", ontos);
				
				mappingToOntoMap.put(baseAdaptedAlign + 
						"Adapted_NCI-FMA_2012-05_keepDelObs" + repairedSuffix + ".xml", ontos);
			}
			else if(reduced){
				mappingToOntoMap.put(baseAdaptedAlign + 
						"Adapted_NCI-FMA_2012-05_reduced.xml", ontos);
				
				mappingToOntoMap.put(baseAdaptedAlign + 
						"Adapted_NCI-FMA_2012-05_reduced" + repairedSuffix + ".xml", ontos);
			}
			else {
				mappingToOntoMap.put(baseAdaptedAlign + 
						"Adapted_NCI-FMA_2012-05.xml", ontos);
				
				mappingToOntoMap.put(baseAdaptedAlign + 
						"Adapted_NCI-FMA_2012-05" + repairedSuffix + ".xml", ontos);
			}
		}		
		
		if(snomednci){
			Pair<String> ontos = new Pair<String>("SNOMEDCT-2012AAesc.obo","NCI-2012AAesc.obo");
			
			if(keepDel && reduced)
				mappingToOntoMap.put(baseAdaptedAlign + 
						"Adapted_SNOMEDCT-NCI_2012-05_keepDelObs_reduced.xml", ontos);
			else if(keepDel)
				mappingToOntoMap.put(baseAdaptedAlign + 
					"Adapted_SNOMEDCT-NCI_2012-05_keepDelObs.xml", ontos);
			else if(reduced)
				mappingToOntoMap.put(baseAdaptedAlign + 
						"Adapted_SNOMEDCT-NCI_2012-05_reduced.xml", ontos);
			else
				mappingToOntoMap.put(baseAdaptedAlign + 
						"Adapted_SNOMEDCT-NCI_2012-05.xml", ontos);
		}

		String [] extractedAligns = {
			"HealthEntity@NCI[2009-04]-AnatomicalEntity@FoundationalModelofAnatomy[2009-04]_UMLS_based.xml",
			"HealthEntity@NCI[2010-04]-AnatomicalEntity@FoundationalModelofAnatomy[2010-04]_UMLS_based.xml",
			"HealthEntity@NCI[2011-05]-AnatomicalEntity@FoundationalModelofAnatomy[2011-05]_UMLS_based.xml",
			"HealthEntity@NCI[2012-05]-AnatomicalEntity@FoundationalModelofAnatomy[2012-05]_UMLS_based.xml",
			"HealthEntity@SNOMEDCT[2009-04]-HealthEntity@NCI[2009-04]_UMLS_based.xml",
			"HealthEntity@SNOMEDCT[2010-04]-HealthEntity@NCI[2010-04]_UMLS_based.xml",
			"HealthEntity@SNOMEDCT[2011-05]-HealthEntity@NCI[2011-05]_UMLS_based.xml",
			"HealthEntity@SNOMEDCT[2012-05]-HealthEntity@NCI[2012-05]_UMLS_based.xml",
		};
		
		if(ncifma){
			mappingToOntoMap.put(baseExtractedAlign + "HealthEntity@NCI[2009-04]-AnatomicalEntity@FoundationalModelofAnatomy[2009-04]_UMLS_based.xml", 
					new Pair<String>("NCI-2009AAesc.obo","UWDA-2009AAesc.obo"));
//			mappingToOntoMap.put(baseExtractedAlign + "HealthEntity@NCI[2010-04]-AnatomicalEntity@FoundationalModelofAnatomy[2010-04]_UMLS_based.xml", 
//					new Pair<String>("NCI-2010AAesc.obo","UWDA-2010AAesc.obo"));		
//			mappingToOntoMap.put(baseExtractedAlign + "HealthEntity@NCI[2011-05]-AnatomicalEntity@FoundationalModelofAnatomy[2011-05]_UMLS_based.xml", 
//					new Pair<String>("NCI-2011AAesc.obo","UWDA-2011AAesc.obo"));
			mappingToOntoMap.put(baseExtractedAlign + "HealthEntity@NCI[2012-05]-AnatomicalEntity@FoundationalModelofAnatomy[2012-05]_UMLS_based.xml", 
					new Pair<String>("NCI-2012AAesc.obo","UWDA-2012AAesc.obo"));
		}
		
		if(snomednci){
			mappingToOntoMap.put(baseExtractedAlign + "HealthEntity@SNOMEDCT[2009-04]-HealthEntity@NCI[2009-04]_UMLS_based.xml", 
					new Pair<String>("SNOMEDCT-2009AAesc.obo","NCI-2009AAesc.obo"));
//			mappingToOntoMap.put(baseExtractedAlign + "HealthEntity@SNOMEDCT[2010-04]-HealthEntity@NCI[2010-04]_UMLS_based.xml", 
//					new Pair<String>("SNOMEDCT-2010AAesc.obo","NCI-2010AAesc.obo"));		
//			mappingToOntoMap.put(baseExtractedAlign + "HealthEntity@SNOMEDCT[2011-05]-HealthEntity@NCI[2011-05]_UMLS_based.xml", 
//					new Pair<String>("SNOMEDCT-2011AAesc.obo","NCI-2011AAesc.obo"));
			mappingToOntoMap.put(baseExtractedAlign + "HealthEntity@SNOMEDCT[2012-05]-HealthEntity@NCI[2012-05]_UMLS_based.xml", 
					new Pair<String>("SNOMEDCT-2012AAesc.obo","NCI-2012AAesc.obo"));
		}
		
		String [] referenceAligns = {
				"RefMap_NCI-FMA_2012-05.xml",
				"RefMap_NCI-FMA_2012-05_keepDelObs.xml",
				"RefMap_NCI-FMA_2012-05_keepDelObs_reduced.xml",
				"RefMap_NCI-FMA_2012-05_reduced.xml",
				"RefMap_SNOMEDCT-NCI_2012-05.xml",
				"RefMap_SNOMEDCT-NCI_2012-05_keepDelObs.xml",
				"RefMap_SNOMEDCT-NCI_2012-05_keepDelObs_reduced.xml",
				"RefMap_SNOMEDCT-NCI_2012-05_reduced.xml",
		};
		
		if(ncifma){
			Pair<String> ontos = new Pair<String>("NCI-2012AAesc.obo","UWDA-2012AAesc.obo");
			
			if(keepDel && reduced)
				mappingToOntoMap.put(baseRefAlign + 
						"RefMap_NCI-FMA_2012-05_keepDelObs_reduced.xml", ontos); 
			else if(keepDel)
				mappingToOntoMap.put(baseRefAlign + 
						"RefMap_NCI-FMA_2012-05_keepDelObs.xml", ontos); 
			else if(reduced)
				mappingToOntoMap.put(baseRefAlign + 
						"RefMap_NCI-FMA_2012-05_reduced.xml", ontos);
			else 
				mappingToOntoMap.put(baseRefAlign + 
						"RefMap_NCI-FMA_2012-05.xml", ontos);
		}
		
		if(snomednci){
			Pair<String> ontos = new Pair<String>("SNOMEDCT-2012AAesc.obo","NCI-2012AAesc.obo");
			
			if(keepDel && reduced)
				mappingToOntoMap.put(baseRefAlign + 
						"RefMap_SNOMEDCT-NCI_2012-05_keepDelObs_reduced.xml", ontos);
			else if(keepDel)
				mappingToOntoMap.put(baseRefAlign + 
					"RefMap_SNOMEDCT-NCI_2012-05_keepDelObs.xml", ontos); 
			else if(reduced)
				mappingToOntoMap.put(baseRefAlign + 
						"RefMap_SNOMEDCT-NCI_2012-05_reduced.xml", ontos);
			else
				mappingToOntoMap.put(baseRefAlign + 
						"RefMap_SNOMEDCT-NCI_2012-05.xml", ontos);
		}
		
		Params.reasonerKind = REASONER_KIND.STRUCTURAL;
		
//		Params.reasonerKind = REASONER_KIND.ELK;
//		Params.inputOntoClassificationTimeout = 60 * 60;
//		Params.alignOntoClassificationTimeout = 5 * 60 * 60;
		
		for (Entry<String, Pair<String>> e : mappingToOntoMap.entrySet()) {
			if(adaptedOnly && !e.getKey().contains("Adapted"))
				continue;
			
			String fstOnto = baseOntoPath + e.getValue().getFirst();
			String sndOnto = baseOntoPath + e.getValue().getSecond();
			String align = e.getKey();
			
			new EvolDataLoader(fstOnto, sndOnto, align).loadData();
		}
	}

}
