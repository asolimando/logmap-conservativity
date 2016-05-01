/*******************************************************************************
 * Copyright 2012 by the Department of Computer Science (University of Oxford)
 * 
 *    This file is part of LogMap.
 * 
 *    LogMap is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 * 
 *    LogMap is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 * 
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with LogMap.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package uk.ac.ox.krr.logmap2;


import uk.ac.ox.krr.logmap2.indexing.JointIndexManager;
import uk.ac.ox.krr.logmap2.oaei.reader.MappingsReaderManager;
import uk.ac.ox.krr.logmap2.indexing.OntologyProcessing;
import uk.ac.ox.krr.logmap2.io.LogOutput;
import uk.ac.ox.krr.logmap2.io.OWLAlignmentFormat;
import uk.ac.ox.krr.logmap2.io.OutPutFilesManager;
import uk.ac.ox.krr.logmap2.io.ReadFile;
import uk.ac.ox.krr.logmap2.mappings.CandidateMappingManager;
import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;
import uk.ac.ox.krr.logmap2.reasoning.SatisfiabilityIntegration;
import uk.ac.ox.krr.logmap2.repair.AnchorAssessment;
import uk.ac.ox.krr.logmap2.repair.MappingNumViolationsComparator;
import uk.ac.ox.krr.logmap2.repair.hornSAT.HornClause;
import uk.ac.ox.krr.logmap2.utilities.Utilities;
import uk.ac.ox.krr.logmap2.lexicon.LexicalUtilities;
import uk.ac.ox.krr.logmap2.overlapping.OverlappingExtractor4Mappings;

import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.apibinding.OWLManager;

import com.hp.hpl.jena.rdf.model.impl.Util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;


/**
 * This class will require two OWL ontologies and a set of mappings (see <code>MappingObjectStr<code>)
 * @author Ernesto
 *
 */
public class LogMap2_RepairFacility {


	private long init_global, init, fin;

	private OntologyProcessing onto_process1;
	private OntologyProcessing onto_process2;

	private JointIndexManager index;

	private CandidateMappingManager mapping_manager;

	private AnchorAssessment mapping_assessment;

	private OWLOntology onto1;
	private OWLOntology onto2;
	private Set<MappingObjectStr> input_mappings;	
	private Set<MappingObjectStr> mappingsToKeep;	
	private boolean overlapping;
	private boolean method_optimal;


	private TreeSet<MappingObjectStr> ordered_mappings = new TreeSet<MappingObjectStr>(new MappingComparator());
	private Set<MappingObjectStr> clean_mappings = new HashSet<MappingObjectStr>();


	protected Map<Integer, Set<Integer>> mappings2Review_step2 = new HashMap<Integer, Set<Integer>>();


	private double average_confidence=0;

	public LogMap2_RepairFacility(OWLOntology onto1, OWLOntology onto2, 
			OntologyProcessing op, JointIndexManager index, 
			Set<MappingObjectStr> mappings, boolean overlapping, 
			boolean optimal) {
		this(onto1,onto2,op,index,mappings,overlapping,optimal,
				Collections.<MappingObjectStr> emptySet());
	}		

	public LogMap2_RepairFacility(OWLOntology onto1, OWLOntology onto2, 
			OntologyProcessing op, JointIndexManager index, 
			Set<MappingObjectStr> mappings, boolean overlapping, boolean optimal, 
			Set<MappingObjectStr> mappingsToKeep) {

		this.onto1 = onto1;
		this.onto2 = onto2;
		this.input_mappings = mappings;
		this.overlapping=overlapping;
		this.method_optimal = optimal;
		this.index = index;

		onto_process1 = op;
		onto_process2 = op;

		try {
			init_global = init = Calendar.getInstance().getTimeInMillis();

			//When reading from RDF align there is no type
			associateType2Mappings();

			mapping_manager = new CandidateMappingManager(
					index, onto_process1, onto_process2);

			onto_process1.clearReasoner();
			//onto_process2.clearReasoner();

			//We add mappings to structures
			addMapping2Structures();

			//			index.recreateDisjointIntervalsStructure();

			assessMappings(mappingsToKeep);

			keepRepairedMappings();

			fin = Calendar.getInstance().getTimeInMillis();
			LogOutput.print("TOTAL REPAIR TIME (s): " 
					+ (float)((double)fin-(double)init_global)/1000.0);
		}
		catch (Exception e){
			System.out.println("Error repairing mappings using LogMap repair module: " + e.getMessage());
		}
	}

	/**
	 * Constructor from Java application
	 * @param onto1
	 * @param onto2
	 * @param mappings
	 * @param overlapping If the intersection or overlapping of the ontologies are extracted before the repair
	 * @param optimal If the repair is performed in a two steps process (optimal) or in one cleaning step (more aggressive)
	 */
	public LogMap2_RepairFacility(
			OWLOntology onto1, 
			OWLOntology onto2, 
			Set<MappingObjectStr> mappings, 
			boolean overlapping, 
			boolean optimal){
		this(onto1, onto2, mappings, overlapping, optimal, false, "", 
				Collections.<MappingObjectStr> emptySet());
	}

	/**
	 * Constructor from Java application
	 * @param onto1
	 * @param onto2
	 * @param mappings
	 * @param overlapping If the intersection or overlapping of the ontologies are extracted before the repair
	 * @param optimal If the repair is performed in a two steps process (optimal) or in one cleaning step (more aggressive)
	 */
	public LogMap2_RepairFacility(
			OWLOntology onto1, 
			OWLOntology onto2, 
			Set<MappingObjectStr> mappings, 
			boolean overlapping, 
			boolean optimal, 
			Set<MappingObjectStr> mappingsToKeep,
			boolean repairImmediately){
		
		this.onto1 = onto1;
		this.onto2 = onto2;
		this.input_mappings = mappings;
		this.overlapping=overlapping;
		this.method_optimal = optimal;
		this.mappingsToKeep = mappingsToKeep;
		
		try{

			init_global = init = Calendar.getInstance().getTimeInMillis();

			setUpStructures();
			
			fin = Calendar.getInstance().getTimeInMillis();
			LogOutput.print("SETUP TIME (s): " + (float)((double)fin-(double)init_global)/1000.0);
//			System.out.println("SETUP TIME (s): " + (float)((double)fin-(double)init_global)/1000.0);

			if(repairImmediately)
				computeRepair();
		}
		catch (Exception e){
			System.out.println("Error repairing mappings using LogMap repair module: " + e.getMessage());
		}

	}

	public void computeRepair(){
		init_global = init = Calendar.getInstance().getTimeInMillis();

		//TODO It also includes assessment for properties and instances!			
		assessMappings(mappingsToKeep);

		//Always... at least for testing
		keepRepairedMappings();
		
		fin = Calendar.getInstance().getTimeInMillis();
		LogOutput.print("TOTAL REPAIR TIME (s): " + (float)((double)fin-(double)init_global)/1000.0);
	}
	
	public LogMap2_RepairFacility(OWLOntology onto1, 
			OWLOntology onto2, OntologyProcessing op1,
			OntologyProcessing op2, JointIndexManager index,
			Set<MappingObjectStr> mappings,
			boolean overlapping, boolean optimal) {
		this(onto1,onto2,op1,op2,index,mappings,overlapping,optimal, 
				Collections.<MappingObjectStr> emptySet());
	}

	public LogMap2_RepairFacility(OWLOntology onto1, 
			OWLOntology onto2, OntologyProcessing op1,
			OntologyProcessing op2, JointIndexManager index,
			Set<MappingObjectStr> mappings,
			boolean overlapping, boolean optimal, 
			Set<MappingObjectStr> mappingsToKeep) {

		this.onto1 = onto1;
		this.onto2 = onto2;
		this.input_mappings = mappings;
		this.overlapping=overlapping;
		this.method_optimal = optimal;
		this.index = index;
		//		this.index = new JointIndexManager(index);
		//		if(!index.equals(this.index))
		//			throw new Error("DIFFERENT INDEX");

		onto_process1 = op1;
		//		onto_process1 = new OntologyProcessing(op1);
		//		if(!op1.equals(this.onto_process1))
		//			throw new Error("DIFFERENT ONTO PROC 1");

		onto_process2 = op2;
		//		onto_process2 =	new OntologyProcessing(op2);
		//		if(!op2.equals(this.onto_process2))
		//			throw new Error("DIFFERENT ONTO PROC 2");

		try {
			init_global = init = Calendar.getInstance().getTimeInMillis();

			//setUpStructures();			

			//When reading from RDF align there is no type
			associateType2Mappings();

			/**************************************************************/
			/*
			 * This part has been already carried out outside this methods
			 * */			
			//			//Create Index and new Ontology Index...
			//			index = new JointIndexManager();
			//
			//			onto_process1 = new OntologyProcessing(onto1, index, new LexicalUtilities());
			//			onto_process2 = new OntologyProcessing(onto2, index, new LexicalUtilities());

			/**************************************************************/

			mapping_manager = new CandidateMappingManager(index, onto_process1, onto_process2);

			/**************************************************************/
			/*
			 * This part has been already carried out outside this methods
			 * */			
			//Extracts lexicon
			//			init = Calendar.getInstance().getTimeInMillis();
			//			onto_process1.precessLexicon(false);
			//			onto_process2.precessLexicon(false);
			//			fin = Calendar.getInstance().getTimeInMillis();
			//			LogOutput.print("Time indexing entities (s): " + (float)((double)fin-(double)init)/1000.0);

			/**************************************************************/

			//We add mappings to structures
			addMapping2Structures();

			//			//Extracts Taxonomy
			//			//Also extracts A^B->C
			//			init = Calendar.getInstance().getTimeInMillis();
			//			onto_process1.setTaxonomicData();
			//			onto_process2.setTaxonomicData();
			//			fin = Calendar.getInstance().getTimeInMillis();
			//			LogOutput.print("Time extracting structural information (s): " 
			//					+ (float)((double)fin-(double)init)/1000.0);
			//
			//			onto_process1.clearReasoner();
			//			//onto_process1.getClass2Identifier().clear();
			//
			//			onto_process2.clearReasoner();
			//			//onto_process2.getClass2Identifier().clear();
			//
			////			index.recreateDisjointIntervalsStructure();

			//TODO It also includes assessment for properties and instances!			
			assessMappings(mappingsToKeep);

			//Always... at least for testing
			keepRepairedMappings();

			fin = Calendar.getInstance().getTimeInMillis();
			LogOutput.print("TOTAL REPAIR TIME (s): " 
					+ (float)((double)fin-(double)init_global)/1000.0);
		}
		catch (Exception e){
			System.out.println("Error repairing mappings using LogMap repair module: " + e.getMessage());
		}
	}

	public LogMap2_RepairFacility(
			OWLOntology onto1,
			OWLOntology onto2, 
			Set<MappingObjectStr> mappings, 
			boolean overlapping, 
			boolean optimal, 
			boolean chechSatisfiability,
			String outPutFileName){
		this(onto1,onto2,mappings,overlapping,optimal,chechSatisfiability,outPutFileName,
				Collections.<MappingObjectStr> emptySet());
	}

	/**
	 * Constructor for command line
	 * @param onto1
	 * @param onto2
	 * @param mappings
	 * @param overlapping If the intersection or overlapping of the ontologies are extracted before the repair
	 * @param optimal If the repair is performed in a two steps process (optimal) or in one cleaning step (more aggressive)
	 * @param chechSatisfiability Uses HermiT to check if the repaired mappings lead to unsatisfiable classes
	 * @param outPutFileName
	 */
	public LogMap2_RepairFacility(
			OWLOntology onto1,
			OWLOntology onto2, 
			Set<MappingObjectStr> mappings, 
			boolean overlapping, 
			boolean optimal, 
			boolean chechSatisfiability,
			String outPutFileName,
			Set<MappingObjectStr> mappingsToKeep){

		this.onto1 = onto1;
		this.onto2 = onto2;
		this.input_mappings = mappings;
		this.overlapping=overlapping;
		this.method_optimal = optimal;




		try{

			init_global = init = Calendar.getInstance().getTimeInMillis();

			setUpStructures();

			//TODO It also includes assessment for properties and instances!			
			assessMappings(mappingsToKeep);

			//Always... at least for testing
			keepRepairedMappings();


			if (!outPutFileName.equals("")){
				saveRepairedMappings(outPutFileName);
			}

			if (chechSatisfiability){
				checkSatisfiabilityMappings(clean_mappings);
			}


			fin = Calendar.getInstance().getTimeInMillis();
			LogOutput.print("TOTAL REPAIR TIME (s): " + (float)((double)fin-(double)init_global)/1000.0);


		}
		catch (Exception e){
			System.out.println("Error repairing mappings using LogMap repair module: " + e.getMessage());
		}


	}



	/**
	 * Method to be called from web service
	 * Create a new class??? //TODO
	 * @param onto1
	 * @param onto2
	 * @param fixedmappings
	 * @param mappings2review
	 */
	public LogMap2_RepairFacility(
			OWLOntology onto1,
			OWLOntology onto2, 
			Set<MappingObjectStr> fixedmappings,
			Set<MappingObjectStr> mappings2review){


		this.onto1 = onto1;
		this.onto2 = onto2;
		this.input_mappings = fixedmappings;
		this.overlapping=false; //we already have the modules!
		this.method_optimal = true;


		try{

			init_global = init = Calendar.getInstance().getTimeInMillis();

			setUpStructures();

			//TODO It also includes assessment for properties and instances!			
			assessMappings();

			//Always... at least for testing
			//Clean mapping
			keepRepairedMappings();



			fin = Calendar.getInstance().getTimeInMillis();
			LogOutput.print("TOTAL REPAIR TIME (s): " + (float)((double)fin-(double)init_global)/1000.0);


		}
		catch (Exception e){
			System.out.println("Error repairing mappings using LogMap repair module: " + e.getMessage());
		}




	}


	private void addSubMapping2Mappings2Review(int index1, int index2){

		if (!mappings2Review_step2.containsKey(index1)){
			mappings2Review_step2.put(index1, new HashSet<Integer>());
		}
		mappings2Review_step2.get(index1).add(index2);
	}


	/**
	 * We associate type to mappings in case the object does indicate this.
	 */
	private void associateType2Mappings(){


		//TREAT GIVEN MAPPINGS

		int num_original_class_mappings=0;
		int num_original_dprop_mappings=0;
		int num_original_oprop_mappings=0;
		int num_original_instance_mappings=0;
		int num_mixed_mappings=0;

		double min_confidence = 2.0;
		double max_confidence = 0.0;
		average_confidence = 0.0;


		for (MappingObjectStr map : input_mappings){

			//Check if it contains type? Better to double check

			average_confidence = average_confidence + map.getConfidence();
			if (map.getConfidence()<min_confidence){
				min_confidence = map.getConfidence(); 
			}
			if (map.getConfidence()>max_confidence){
				max_confidence = map.getConfidence(); 
			}


			//Detect the type of mapping: class, property or instance
			//In some cases it might be included
			if (onto1.containsClassInSignature(IRI.create(map.getIRIStrEnt1()), true)
					&& onto2.containsClassInSignature(IRI.create(map.getIRIStrEnt2()), true)) {

				map.setTypeOfMapping(Utilities.CLASSES);

				ordered_mappings.add(map);

				num_original_class_mappings++;


			}
			else if (onto1.containsObjectPropertyInSignature(IRI.create(map.getIRIStrEnt1()), true)
					&& onto2.containsObjectPropertyInSignature(IRI.create(map.getIRIStrEnt2()), true)) {

				map.setTypeOfMapping(Utilities.OBJECTPROPERTIES);

				ordered_mappings.add(map);

				num_original_oprop_mappings++;


			}
			else if (onto1.containsDataPropertyInSignature(IRI.create(map.getIRIStrEnt1()), true)
					&& onto2.containsDataPropertyInSignature(IRI.create(map.getIRIStrEnt2()), true)) {

				map.setTypeOfMapping(Utilities.DATAPROPERTIES);

				ordered_mappings.add(map);

				num_original_dprop_mappings++;

			}

			else if (onto1.containsIndividualInSignature(IRI.create(map.getIRIStrEnt1()), true)
					&& onto2.containsIndividualInSignature(IRI.create(map.getIRIStrEnt2()), true)) {

				map.setTypeOfMapping(Utilities.INSTANCES);

				ordered_mappings.add(map);			

				num_original_instance_mappings++;

			}
			else {
				//System.out.println("Mixed Entities or entities not in signature of ontologies: ");
				//System.out.println("\t" + map.getIRIStrEnt1());
				//System.out.println("\t" + map.getIRIStrEnt2());

				num_mixed_mappings++;

			}


		}

		average_confidence = average_confidence/(double)input_mappings.size();

		LogOutput.printAlways("Num original mappings: " + input_mappings.size());
		LogOutput.print("\tNum original class mappings: " + num_original_class_mappings);
		LogOutput.print("\tNum original object property mappings: " + num_original_oprop_mappings);
		LogOutput.print("\tNum original data property mappings: " + num_original_dprop_mappings);			
		LogOutput.print("\tNum original instance mappings: " + num_original_instance_mappings);
		LogOutput.print("\tNum mixed mappings: " + num_mixed_mappings);
		LogOutput.print("\tMin confidence: " + min_confidence);
		LogOutput.print("\tMax confidence: " + max_confidence);
		LogOutput.print("\tAVERAGE confidence: " + average_confidence);

		//We do not differentiate between reliable mappings and other mappings
		if (!method_optimal)
			average_confidence=-1.0;



	}


	/**
	 * We add mapping to structures in mapping_manager
	 */
	private void addMapping2Structures(){


		mappings2Review_step2.clear();


		Iterator<MappingObjectStr> it = ordered_mappings.descendingIterator();

		MappingObjectStr map;


		//for (MappingObjectStr map : ordered_mappings){
		while (it.hasNext()){

			map = it.next();

			//System.out.println(map + "   " + map.getConfidence());

			if (map.getTypeOfMapping()==Utilities.CLASSES) {

				addClassMapping(map);

			}
			else if (map.getTypeOfMapping()==Utilities.OBJECTPROPERTIES) {

				addObjectPropertyMapping(map);

			}
			else if (map.getTypeOfMapping()==Utilities.DATAPROPERTIES) {

				addDataPropertyMapping(map);

			}

			else if (map.getTypeOfMapping()==Utilities.INSTANCES) {

				addInstanceMapping(map);			

			}
			else {
				//Do nothing			
			}


		}

		//Not necessary any more
		ordered_mappings.clear();


		LogOutput.print("Numb of reliable mappings: " + num_anchors);
		LogOutput.print("Numb of other mappings: " + num_mappings2review);




	}




	private void setUpStructures() throws Exception{


		//try{

		//TODO showOutput!!		
		LogOutput.showOutpuLog(false);

		//init = Calendar.getInstance().getTimeInMillis();
		//PrecomputeIndexCombination.preComputeIdentifierCombination();
		//fin = Calendar.getInstance().getTimeInMillis();
		//LogOutput.print("Time precomputing index combinations (s): " + (float)((double)fin-(double)init)/1000.0);


		//When reading from RDF align there is no type
		associateType2Mappings();


		//Create Index and new Ontology Index...
		index = new JointIndexManager();


		//Extract overlapping if indicated
		if (overlapping){
			OverlappingExtractor4Mappings overlapping = new OverlappingExtractor4Mappings();

			overlapping.createOverlapping(onto1, onto2, input_mappings);

			onto_process1 = new OntologyProcessing(overlapping.getOverlappingOnto1(), index, new LexicalUtilities());
			onto_process2 = new OntologyProcessing(overlapping.getOverlappingOnto2(), index, new LexicalUtilities());
		}
		else{
			onto_process1 = new OntologyProcessing(onto1, index, new LexicalUtilities());
			onto_process2 = new OntologyProcessing(onto2, index, new LexicalUtilities());
		}




		mapping_manager = new CandidateMappingManager(index, onto_process1, onto_process2);



		//Extracts lexicon
		init = Calendar.getInstance().getTimeInMillis();
		onto_process1.precessLexicon(false);
		onto_process2.precessLexicon(false);
		fin = Calendar.getInstance().getTimeInMillis();
		LogOutput.print("Time indexing entities (s): " + (float)((double)fin-(double)init)/1000.0);


		//We add mappings to structures
		addMapping2Structures();



		//Extracts Taxonomy
		//Also extracts A^B->C
		init = Calendar.getInstance().getTimeInMillis();
		onto_process1.setTaxonomicData();
		onto_process2.setTaxonomicData();
		fin = Calendar.getInstance().getTimeInMillis();
		LogOutput.print("Time extracting structural information (s): " + (float)((double)fin-(double)init)/1000.0);




		onto_process1.clearReasoner();
		onto_process1.getClass2Identifier().clear();

		onto_process2.clearReasoner();
		onto_process2.getClass2Identifier().clear();
		//}
		//catch (Exception e){
		//	e.printStackTrace();
		//}


	}


	private void preprocessingHeuristicForRepairPlans(){
		Map<HornClause,Integer> mapping2NumUnsatisfiabilities = 
				mapping_assessment.getMapping2NumUnsatisfiabilities();

		LogOutput.printAlways("\tMapping(s) to be analyzed by the heuristic: " 
				+ mapping2NumUnsatisfiabilities.size());
//		System.out.println("\tMapping(s) to be analyzed by the heuristic: " 
//				+ mapping2NumUnsatisfiabilities.size());

		MappingNumViolationsComparator comp = 
				new MappingNumViolationsComparator(mapping2NumUnsatisfiabilities);

		List<HornClause> mappingsToRemove = new ArrayList<>(
				mapping2NumUnsatisfiabilities.keySet());

		if(mappingsToRemove.isEmpty())
			return;

		Collections.sort(mappingsToRemove, comp);

		LogOutput.printAlways("\tMapping involved in the most number of unsatisfiabilities: " + mappingsToRemove.get(0) 
				+ ", UNSATs: " + mapping2NumUnsatisfiabilities.get(mappingsToRemove.get(0)));
//		System.out.println("\tMapping involved in the most number of unsatisfiabilities: " + mappingsToRemove.get(0) 
//				+ ", UNSATs: " + mapping2NumUnsatisfiabilities.get(mappingsToRemove.get(0)));

		int removed = 0;
		int decreased = 0;

		for (HornClause clause : mappingsToRemove) {
			// nothing to do from this point on
			if(mapping2NumUnsatisfiabilities.get(clause) < Parameters.repair_heuristic_threshold_decrease)
				break;

			if(mapping2NumUnsatisfiabilities.get(clause) >= Parameters.repair_heuristic_threshold_remove){
				removed++;
				//				dgSat.addGeneralLink2Ignore(clausemap.getLeftHS1(), clausemap.getLabel(), clausemap.getRightHS());
				//				dgSat.addGeneralLink2Ignore(clausemap.getRightHS(), clausemap.getLabel(), clausemap.getLeftHS1());
				if (clause.getDirImplication()==HornClause.L2R){
					//						mapping_assessment.addParticularIgnoreLink(
					//								clause.getLeftHS1(), clause.getLabel(), clause.getRightHS());	
					mapping_manager.removeSubMappingFromStructure(clause.getLeftHS1(),clause.getRightHS());
				} else {
					//					mapping_assessment.addParticularIgnoreLink(
					//							clause.getRightHS(), clause.getLabel(), clause.getLeftHS1());
					mapping_manager.removeSubMappingFromStructure(clause.getRightHS(),clause.getLeftHS1());
				}
			}
			else {
				decreased++;
				double oldConf = mapping_manager.getConfidence4Mapping(
						clause.getLeftHS1(), clause.getRightHS());
				mapping_manager.addIsub2Structure(
						clause.getLeftHS1(), clause.getRightHS(), oldConf-oldConf*0.1);
			}
		}

//		System.out.println("\t" + removed + " mapping(s) removed");
//		System.out.println("\t" + decreased + " mapping(s) decreased");

		LogOutput.printAlways("\t" + removed + " mapping(s) removed");
		LogOutput.printAlways("\t" + decreased + " mapping(s) decreased");

		mapping_assessment.getMapping2NumUnsatisfiabilities().clear();
	}

	public int getUnsatsLowerBound(){
		mapping_assessment = new AnchorAssessment(index, mapping_manager);
		return mapping_assessment.CountSatisfiabilityOfIntegration_DandG(
				mapping_manager.getAnchors());
	}

	private void assessMappings(){
		assessMappings(Collections.<MappingObjectStr> emptySet());
	}

	private void assessMappings(Set<MappingObjectStr> mappingsToKeep){

		// Alessandro 24 September 2014: now driven by a global parameter, used 
		// for an additional repair plan heuristic 
		if(Parameters.repair_heuristic){
			//Optional check to count the total unsat classes
			LogOutput.print("INITIAL UNSATISFIABILITY");
			mapping_assessment = new AnchorAssessment(index, mapping_manager);
			mapping_assessment.CountSatisfiabilityOfIntegration_DandG(
					mapping_manager.getAnchors());
			preprocessingHeuristicForRepairPlans();
		}

		//CLASS MAPPINGS ASSESSESMENT
		if (method_optimal)
			assessClassMappings2steps(); //we split between reliable and other mappings
		else
			assessClassMappings1step(mappingsToKeep);
		//			assessClassMappings1step(!Parameters.repair_heuristic);


		//Clean property mappings and individual mappings
		//--------------------------------

		//Assess Property mappings: using index
		if (mapping_manager.getDataPropertyAnchors().size() >0 || mapping_manager.getObjectPropertyAnchors().size() > 0) {
			init = Calendar.getInstance().getTimeInMillis();
			mapping_manager.evaluateCompatibilityDataPropertyMappings();
			mapping_manager.evaluateCompatibilityObjectPropertyMappings();
			fin = Calendar.getInstance().getTimeInMillis();
			LogOutput.print("\tTime assessing property mappings (s): " + (float)((double)fin-(double)init)/1000.0);		
		}



		//Asses Individuals index
		if (mapping_manager.getInstanceMappings().size()>0){

			init = Calendar.getInstance().getTimeInMillis();
			mapping_manager.evaluateCompatibilityInstanceMappings();


			//Asses Individuals D&G
			if (mapping_manager.getInstanceMappings().size()>0){

				init = Calendar.getInstance().getTimeInMillis();

				//We have an specific method since there is not a top-down search. And we first repair classes
				mapping_assessment.CheckSatisfiabilityOfIntegration_DandG_Individuals(
						mapping_manager.getInstanceMappings());

				fin = Calendar.getInstance().getTimeInMillis();
				LogOutput.print("Time cleaning instance mappings D&G (s): " + (float)((double)fin-(double)init)/1000.0);
			}

			fin = Calendar.getInstance().getTimeInMillis();
			LogOutput.print("\tTime assessing instance mappings (s): " + (float)((double)fin-(double)init)/1000.0);
		}


	}

	/**
	 * Clean first the identified anchors and then clean the rest of candidates
	 */
	private void assessClassMappings2steps(){


		int discarded_with_index=0;

		mapping_assessment = new AnchorAssessment(index, mapping_manager);

		//TODO uncomments for general behaviour
		//For SNOMED-NCI cases we need an approximation and for other cases as well
		//init = Calendar.getInstance().getTimeInMillis();
		//mapping_assessment.CountSatisfiabilityOfIntegration_DandG(mapping_manager.getAnchors());
		//fin = Calendar.getInstance().getTimeInMillis();
		//System.out.println("\tTime counting unsat Dowling and Gallier (s): " + (float)((double)fin-(double)init)/1000.0);


		init = Calendar.getInstance().getTimeInMillis();
		mapping_assessment.CheckSatisfiabilityOfIntegration_DandG(mapping_manager.getAnchors());
		fin = Calendar.getInstance().getTimeInMillis();
		LogOutput.print("\tTime cleaning reliable class mappings Dowling and Gallier (s): " + (float)((double)fin-(double)init)/1000.0);
		LogOutput.print("\tRepaired Root Unsat using Dowling and Gallier (aproximation): " + mapping_assessment.getNumRepairedUnsatClasses());

		//After repairing exact
		mapping_manager.setExactAsFixed(true);


		//Interval labelling schema and cleaning of mappings 2 review against anchors
		//------------------------------------------------------------------------------
		try {			

			init = Calendar.getInstance().getTimeInMillis();

			//Index already have the necessary taxonomical information apart from the equiv mappings
			index.setIntervalLabellingIndex(mapping_manager.getFixedAnchors());
			index.clearAuxStructuresforLabellingSchema();

			fin = Calendar.getInstance().getTimeInMillis();
			LogOutput.print("\tTime indexing hierarchy + anchors (ILS) (s): " + (float)((double)fin-(double)init)/1000.0);


			//Asses mappings to review
			for (int ide1 : mappings2Review_step2.keySet()){
				for (int ide2 : mappings2Review_step2.get(ide1)){

					if (index.areDisjoint(ide1, ide2)) {
						//if (mapping_manager.isMappingInConflictWithFixedMappings(ide1, ide2)){
						discarded_with_index++;
					}
					else {
						mapping_manager.addSubMapping2Mappings2Review(ide1, ide2);						
					}
				}				
			}

			LogOutput.print("Discarded with index: " + discarded_with_index);


		}
		catch (Exception e){
			System.out.println("Error creating Interval Labelling index 1: " + e.getMessage());
			e.printStackTrace();
		}

		if (mapping_manager.getMappings2Review().size()>0){

			//Clean D&G mappings 2 review
			init = Calendar.getInstance().getTimeInMillis();
			mapping_assessment.CheckSatisfiabilityOfIntegration_DandG(mapping_manager.getMappings2Review());  //With Fixed mappings!
			fin = Calendar.getInstance().getTimeInMillis();
			LogOutput.print("Time cleaning rest of the mappings using D&G (s): " + (float)((double)fin-(double)init)/1000.0);
			LogOutput.print("\tRepaired Root Unsat using Dowling and Gallier 2 (aproximation): " + mapping_assessment.getNumRepairedUnsatClasses());

			//Move clean to anchors
			mapping_manager.moveMappingsToReview2AnchorList();


			//Interval labelling index with try block with all clean mappings
			//------------------------------
			try{			
				init = Calendar.getInstance().getTimeInMillis();
				index.setIntervalLabellingIndex(mapping_manager.getAnchors());//It also contains mappings 2 review
				index.clearAuxStructuresforLabellingSchema();
				fin = Calendar.getInstance().getTimeInMillis();
				LogOutput.print("Time indexing hierarchy + anchors and candidates I (ILS) (s): " + (float)((double)fin-(double)init)/1000.0);
			}
			catch (Exception e){
				System.out.println("Error creating Interval Labelling index 2: " + e.getMessage());
				e.printStackTrace();
			}
		}


		//Add weakened
		for (int ide1 : mapping_manager.getWeakenedDandGAnchors().keySet()){

			for (int ide2 : mapping_manager.getWeakenedDandGAnchors().get(ide1)){

				if (!mapping_manager.isMappingInConflictWithFixedMappings(ide1, ide2)){

					mapping_manager.addSubMapping2ListOfAnchors(ide1, ide2);

				}
			}
		}


		//Repair all just in case
		///--------------------------
		mapping_manager.setExactAsFixed(false);

		init = Calendar.getInstance().getTimeInMillis();
		mapping_assessment.CheckSatisfiabilityOfIntegration_DandG(mapping_manager.getAnchors());
		fin = Calendar.getInstance().getTimeInMillis();
		LogOutput.print("\tTime cleaning ALL class mappings Dowling and Gallier (s): " + (float)((double)fin-(double)init)/1000.0);
		LogOutput.print("\tRepaired Root Unsat using Dowling and Gallier 3 (aproximation): " + mapping_assessment.getNumRepairedUnsatClasses());

		//New weakened ar not added



	}

	private void assessClassMappings1step(Set<MappingObjectStr> mappingToKeep){
		assessClassMappings1step(mapping_assessment != null, mappingToKeep);
	}

	private void assessClassMappings1step(){
		assessClassMappings1step(mapping_assessment != null);
	}

	/**
	 * Clean the complete set of mappings at once
	 */
	private void assessClassMappings1step(boolean reuseMappingAssessment){
		assessClassMappings1step(reuseMappingAssessment, 
				Collections.<MappingObjectStr> emptySet());
	}

	/**
	 * Clean the complete set of mappings at once
	 */
	private void assessClassMappings1step(boolean reuseMappingAssessment, 
			Set<MappingObjectStr> mappingsToKeep){

		if(!reuseMappingAssessment)
			mapping_assessment = new AnchorAssessment(index, mapping_manager);

		//TODO comments for general behaviour
		//For SNOMED-NCI cases we need an approximation and for other cases as well
		//init = Calendar.getInstance().getTimeInMillis();
		//mapping_assessment.CountSatisfiabilityOfIntegration_DandG(mapping_manager.getAnchors());
		//fin = Calendar.getInstance().getTimeInMillis();
		//System.out.println("\tTime counting unsat Dowling and Gallier (s): " + (float)((double)fin-(double)init)/1000.0);
		//if (true)
		//	return;
		//End to comments


		Set<HornClause> clausesToKeep = new HashSet<>();
		for (MappingObjectStr m : mappingsToKeep) {
			int ide1=onto_process1.getIdentifier4ConceptName(
					Utilities.getEntityLabelFromURI(m.getIRIStrEnt1()));
			int ide2=onto_process2.getIdentifier4ConceptName(
					Utilities.getEntityLabelFromURI(m.getIRIStrEnt2()));

			if(m.getMappingDirection() == Utilities.EQ){
				clausesToKeep.add(new HornClause(ide1, ide2, 0, HornClause.MAP, HornClause.L2R));
				clausesToKeep.add(new HornClause(ide1, ide2, 0, HornClause.MAP, HornClause.R2L));
			}

			else if(m.getMappingDirection() == Utilities.L2R){
				clausesToKeep.add(new HornClause(ide1, ide2, 0, HornClause.MAP, HornClause.L2R));
			}

			else if(m.getMappingDirection() == Utilities.R2L){
				clausesToKeep.add(new HornClause(ide1, ide2, 0, HornClause.MAP, HornClause.R2L));
			}
		}

		init = Calendar.getInstance().getTimeInMillis();
		mapping_assessment.CheckSatisfiabilityOfIntegration_DandG(mapping_manager.getAnchors(),clausesToKeep);
		fin = Calendar.getInstance().getTimeInMillis();
		LogOutput.print("\tTime cleaning class mappings Dowling and Gallier (s): " + (float)((double)fin-(double)init)/1000.0);
		LogOutput.print("\tRepaired Root Unsat using Dowling and Gallier (aproximation): " + mapping_assessment.getNumRepairedUnsatClasses());



		try {

			//Interval labelling schema
			//--------------------------
			init = Calendar.getInstance().getTimeInMillis();

			//Index already have the necessary taxonomical information apart from the equiv mappings
			index.setIntervalLabellingIndex(mapping_manager.getAnchors());
			index.clearAuxStructuresforLabellingSchema();

			fin = Calendar.getInstance().getTimeInMillis();
			LogOutput.print("\tTime indexing hierarchy + anchors (ILS) (s): " + (float)((double)fin-(double)init)/1000.0);
		}
		catch (Exception e){
			System.out.println("Error creating Interval Labelling index: " + e.getMessage());
			e.printStackTrace();
		}


		for (int ide1 : mapping_manager.getWeakenedDandGAnchors().keySet()){

			for (int ide2 : mapping_manager.getWeakenedDandGAnchors().get(ide1)){

				//				//TODO: This is necessary no?
				//				//if (!mapping_manager.isMappingInConflictWithFixedMappings(ide1, ide2)){
				//				mapping_manager.addSubMapping2ListOfAnchors(ide1, ide2);
				//				//}

				// Alessandro 16 June 2014: retracted, now fixed in "AnchorAssessment" class, 
				// method "remove_weaken_ConflictiveMappings2"
				//if(mapping_manager.isMappingInAnchors(ide1, ide2))
				mapping_manager.addSubMapping2ListOfAnchors(ide1, ide2);
			}
		}
	}


	int num_anchors=0;
	int num_mappings2review=0;

	/**
	 * Adds mappings to structures
	 * @param map
	 */
	private void addClassMapping(MappingObjectStr map){



		int ide1;
		int ide2;



		//Translate from mapping 2 index
		ide1=onto_process1.getIdentifier4ConceptName(Utilities.getEntityLabelFromURI(map.getIRIStrEnt1()));
		ide2=onto_process2.getIdentifier4ConceptName(Utilities.getEntityLabelFromURI(map.getIRIStrEnt2()));


		//We only consider classes
		if (ide1<0 || ide2<0){
			LogOutput.print("Classes not found in ontology.");
			LogOutput.print("\t" + ide1 + "  " + map.getIRIStrEnt1());
			LogOutput.print("\t" + ide2 + "  " + map.getIRIStrEnt2());
			return;
		}


		mapping_manager.addIsub2Structure(ide1, ide2, map.getConfidence());
		mapping_manager.addIsub2Structure(ide2, ide1, map.getConfidence());


		//TODO Split mappings in anchors and candidates 2 review
		//Less than half
		if (((2*num_anchors) < ordered_mappings.size()) || !method_optimal){
			//if (map.getConfidence()>=average_confidence){

			num_anchors++;				

			if (map.getMappingDirection()==Utilities.EQ){
				mapping_manager.addSubMapping2ListOfAnchors(ide1, ide2);
				mapping_manager.addSubMapping2ListOfAnchors(ide2, ide1);

			}
			else if (map.getMappingDirection()==Utilities.L2R){
				mapping_manager.addSubMapping2ListOfAnchors(ide1, ide2); //TODO Check this
				//mapping_manager.addIsub2Structure(ide1, ide2, map.getConfidence()); //confidence

				if (method_optimal){//we do not add them to reliable mappings for indexing issues
					addSubMapping2Mappings2Review(ide1, ide2);
				}

			}
			else{
				mapping_manager.addSubMapping2ListOfAnchors(ide2, ide1);

				if (method_optimal){ //we do not add them to reliable mappings for indexing issues
					addSubMapping2Mappings2Review(ide2, ide1);
				}
				//mapping_manager.addIsub2Structure(ide2, ide1, map.getConfidence());
			}

		}
		else {

			num_mappings2review++;

			if (map.getMappingDirection()==Utilities.EQ){
				addSubMapping2Mappings2Review(ide1, ide2); //local method
				addSubMapping2Mappings2Review(ide2, ide1);

			}
			else if (map.getMappingDirection()==Utilities.L2R){
				addSubMapping2Mappings2Review(ide1, ide2);				
			}
			else{				
				addSubMapping2Mappings2Review(ide2, ide1);
			}

		}

	}


	private void addObjectPropertyMapping(MappingObjectStr map){

		int ide1;
		int ide2;

		//Translate from mapping 2 index
		ide1=onto_process1.getIdentifier4ObjectPropName(Utilities.getEntityLabelFromURI(map.getIRIStrEnt1()));
		ide2=onto_process2.getIdentifier4ObjectPropName(Utilities.getEntityLabelFromURI(map.getIRIStrEnt2()));


		//We only consider classes
		if (ide1<0 || ide2<0){
			LogOutput.print("Object properties not found in ontology.");
			LogOutput.print("\t" + ide1 + "  " + map.getIRIStrEnt1());
			LogOutput.print("\t" + ide2 + "  " + map.getIRIStrEnt2());
			return;
		}

		//So far only equivalences are considered
		//if (map.getMappingDirection()==Utilities.EQ){
		mapping_manager.addObjectPropertyAnchor(ide1, ide2);
		mapping_manager.addObjectPropertyAnchorConfidence(ide1, map.getConfidence());
		//}		
	}







	private void addDataPropertyMapping(MappingObjectStr map){

		int ide1;
		int ide2;

		//Translate from mapping 2 index
		ide1=onto_process1.getIdentifier4DataPropName(Utilities.getEntityLabelFromURI(map.getIRIStrEnt1()));
		ide2=onto_process2.getIdentifier4DataPropName(Utilities.getEntityLabelFromURI(map.getIRIStrEnt2()));


		//We only consider classes
		if (ide1<0 || ide2<0){
			LogOutput.print("Data properties not found in ontology.");
			LogOutput.print("\t" + ide1 + "  " + map.getIRIStrEnt1());
			LogOutput.print("\t" + ide2 + "  " + map.getIRIStrEnt2());
			return;
		}

		//So far only equivalences are considered
		//if (map.getMappingDirection()==Utilities.EQ){
		mapping_manager.addDataPropertyAnchor(ide1, ide2);
		mapping_manager.addDataPropertyAnchorConfidence(ide1, map.getConfidence());
		//}		


	}


	private void addInstanceMapping(MappingObjectStr map){


		int ide1;
		int ide2;

		//Translate from mapping 2 index
		ide1=onto_process1.getIdentifier4InstanceName(Utilities.getEntityLabelFromURI(map.getIRIStrEnt1()));
		ide2=onto_process2.getIdentifier4InstanceName(Utilities.getEntityLabelFromURI(map.getIRIStrEnt2()));


		//We only consider classes
		if (ide1<0 || ide2<0){
			LogOutput.print("Individuals not found in ontology.");
			LogOutput.print("\t" + ide1 + "  " + map.getIRIStrEnt1());
			LogOutput.print("\t" + ide2 + "  " + map.getIRIStrEnt2());
			return;
		}

		//So far only equivalences are considered
		//if (map.getMappingDirection()==Utilities.EQ){
		mapping_manager.addInstanceMapping(ide1, ide2);			
		mapping_manager.addInstanceAnchorConfidence(ide1, ide2, map.getConfidence());
		//}		


	}


	private void saveRepairedMappings(String outPutFileName){

		int dirMapping;

		OutPutFilesManager outPutFilesManager = new OutPutFilesManager();

		int num_clean_mappings=0;
		int num_clean_class_mappings=0;
		int num_clean_dprop_mappings=0;
		int num_clean_oprop_mappings=0;
		int num_clean_instance_mappings=0;

		try {
			outPutFilesManager.createOutFiles(
					//logmap_mappings_path + "Output/mappings",
					//path + "/" + file_name,
					//outPutFileName + "/" + "repaired_mappings",
					outPutFileName,
					OutPutFilesManager.AllFormats,
					onto_process1.getOntoIRI(),
					onto_process1.getOntoIRI());

			//if (Parameters.output_class_mappings){

			for (int idea : mapping_manager.getAnchors().keySet()){
				for (int ideb : mapping_manager.getAnchors().get(idea)){

					//This is important to keep compatibility with OAEI and Flat alignment formats
					//The order of mappings is important
					//For OWL output would be the same since mappings are axioms
					if (mapping_manager.isId1SmallerThanId2(idea, ideb)){

						if (mapping_manager.isMappingAlreadyInList(ideb, idea)){
							dirMapping=Utilities.EQ;
						}
						else {
							dirMapping=Utilities.L2R;
						}

						num_clean_mappings++;
						num_clean_class_mappings++;

						outPutFilesManager.addClassMapping2Files(
								index.getIRIStr4ConceptIndex(idea),
								index.getIRIStr4ConceptIndex(ideb),
								dirMapping, 
								mapping_manager.getConfidence4Mapping(idea, ideb));
					}
					else {
						if (mapping_manager.isMappingAlreadyInList(ideb, idea)){
							//Do nothing
						}
						else {

							num_clean_mappings++;
							num_clean_class_mappings++;

							outPutFilesManager.addClassMapping2Files(
									index.getIRIStr4ConceptIndex(ideb),
									index.getIRIStr4ConceptIndex(idea),
									Utilities.R2L, 
									mapping_manager.getConfidence4Mapping(idea, ideb));
						}
					}


				}
			}
			//}

			//if (Parameters.output_prop_mappings){

			for (int ide1 : mapping_manager.getDataPropertyAnchors().keySet()){		

				num_clean_mappings++;
				num_clean_dprop_mappings++;

				outPutFilesManager.addDataPropMapping2Files(
						index.getIRIStr4DataPropIndex(ide1),
						index.getIRIStr4DataPropIndex(mapping_manager.getDataPropertyAnchors().get(ide1)),
						Utilities.EQ,  
						mapping_manager.getConfidence4DataPropertyAnchor(ide1, mapping_manager.getDataPropertyAnchors().get(ide1))//1.0
						);
			}

			for (int ide1 : mapping_manager.getObjectPropertyAnchors().keySet()){

				num_clean_mappings++;
				num_clean_oprop_mappings++;

				outPutFilesManager.addObjPropMapping2Files(
						index.getIRIStr4ObjPropIndex(ide1),
						index.getIRIStr4ObjPropIndex(mapping_manager.getObjectPropertyAnchors().get(ide1)),
						Utilities.EQ, 
						mapping_manager.getConfidence4ObjectPropertyAnchor(ide1, mapping_manager.getObjectPropertyAnchors().get(ide1))//1.0
						);
			}
			//}



			//if (Parameters.perform_instance_matching && Parameters.output_instance_mappings){

			for (int ide1 : mapping_manager.getInstanceMappings().keySet()){
				for (int ide2 : mapping_manager.getInstanceMappings().get(ide1)){

					num_clean_mappings++;
					num_clean_instance_mappings++;

					outPutFilesManager.addInstanceMapping2Files(
							index.getIRIStr4IndividualIndex(ide1), 
							index.getIRIStr4IndividualIndex(ide2), 
							mapping_manager.getConfidence4InstanceMapping(ide1, ide2)
							);

				}

			}
			//}


			//mapping_manager.setStringAnchors();
			LogOutput.printAlways("Num repaired mappings: " + num_clean_mappings);
			LogOutput.print("\tNum repaired class mappings: " + num_clean_class_mappings);
			LogOutput.print("\tNum repaired object property mappings: " + num_clean_oprop_mappings);
			LogOutput.print("\tNum repaired data property mappings: " + num_clean_dprop_mappings);			
			LogOutput.print("\tNum repaired instance mappings: " + num_clean_instance_mappings);

			outPutFilesManager.closeAndSaveFiles();


		}
		catch (Exception e){
			System.err.println("Error saving mappings...");
			e.printStackTrace();
		}


	}



	/**
	 * Returns the set of mappings that have been repaired using LogMap's repair facility
	 * @return
	 */
	public Set<MappingObjectStr> getCleanMappings(){
		return clean_mappings;
	}

	/**
	 * Returns the input set of mappings (NOT repaired)
	 * @return
	 */
	public Set<MappingObjectStr> getInputMappings(){
		return input_mappings;
	}

	/**
	 * Returns the real size of the repair: number of removed clauses
	 * @return
	 */
	public int getSizeOfRepair(){

		int clauses = 0;

		for (int ide1 : mapping_manager.getConflictiveAnchors().keySet()){
			clauses += mapping_manager.getConflictiveAnchors().get(ide1).size();
		}

		return clauses;

	}


	public void checkSatisfiabilityInputMappings() throws Exception {
		checkSatisfiabilityMappings(input_mappings);
	}

	public void checkSatisfiabilityCleanMappings() throws Exception {
		checkSatisfiabilityMappings(clean_mappings);
	}


	public int checkSatisfiabilityMappings(Set<MappingObjectStr> mappings) throws Exception {

		OWLOntology mappins_owl_onto = getOWLOntology4CleanMappings(mappings);


		SatisfiabilityIntegration.setTimeoutClassSatisfiabilityCheck(60);


		SatisfiabilityIntegration sat_checker = new SatisfiabilityIntegration(
				onto1, 
				onto2,
				mappins_owl_onto,
				true,//class sat
				true,//Time_Out_Class
				false); //use factory


		LogOutput.print("Num unsat classes lead by repaired mappings using LogMap: " + sat_checker.getNumUnsatClasses());

		return sat_checker.getNumUnsatClasses();
	}


	/**
	 * Returns the clean mappings as an OWLOntology object.
	 * @return
	 * @throws Exception
	 */
	public OWLOntology getOWLOntology4CleanMappings(Set<MappingObjectStr> mappings) throws Exception {

		OWLAlignmentFormat owlformat = new OWLAlignmentFormat("");


		for (MappingObjectStr mapping : mappings){


			if (mapping.getTypeOfMapping() == Utilities.INSTANCE){

				owlformat.addInstanceMapping2Output(
						mapping.getIRIStrEnt1(),
						mapping.getIRIStrEnt2(),						
						mapping.getConfidence());				
			}


			else if (mapping.getTypeOfMapping() == Utilities.CLASSES){


				owlformat.addClassMapping2Output(
						mapping.getIRIStrEnt1(),
						mapping.getIRIStrEnt2(),
						mapping.getMappingDirection(),
						mapping.getConfidence());
			}

			else if (mapping.getTypeOfMapping() == Utilities.OBJECTPROPERTIES){

				owlformat.addObjPropMapping2Output(
						mapping.getIRIStrEnt1(),
						mapping.getIRIStrEnt2(),
						mapping.getMappingDirection(),
						mapping.getConfidence());
			}

			else if (mapping.getTypeOfMapping() == Utilities.DATAPROPERTIES){

				owlformat.addDataPropMapping2Output(
						mapping.getIRIStrEnt1(),
						mapping.getIRIStrEnt2(),
						mapping.getMappingDirection(),
						mapping.getConfidence());

			}


		}//end for mappings


		return owlformat.getOWLOntology();


	}







	private void keepRepairedMappings(){

		int dirMapping;

		int num_clean_mappings=0;
		int num_clean_class_mappings=0;
		int num_clean_dprop_mappings=0;
		int num_clean_oprop_mappings=0;
		int num_clean_instance_mappings=0;

		clean_mappings.clear();

		try {

			for (int idea : mapping_manager.getAnchors().keySet()){
				for (int ideb : mapping_manager.getAnchors().get(idea)){

					//This is important to keep compatibility with OAEI and Flat alignment formats
					//The order of mappings is important
					//For OWL output would be the same since mappings are axioms
					if (mapping_manager.isId1SmallerThanId2(idea, ideb)){

						if (mapping_manager.isMappingAlreadyInList(ideb, idea)){
							dirMapping=Utilities.EQ;
						}
						else {
							dirMapping=Utilities.L2R;
						}

						num_clean_mappings++;
						num_clean_class_mappings++;

						clean_mappings.add(
								new MappingObjectStr(
										index.getIRIStr4ConceptIndex(idea), 
										index.getIRIStr4ConceptIndex(ideb), 
										mapping_manager.getConfidence4Mapping(idea, ideb), 
										dirMapping,
										Utilities.CLASSES));

					}
					else {
						if (mapping_manager.isMappingAlreadyInList(ideb, idea)){
							//Do nothing
						}
						else {

							num_clean_mappings++;
							num_clean_class_mappings++;

							clean_mappings.add(
									new MappingObjectStr(
											index.getIRIStr4ConceptIndex(ideb),
											index.getIRIStr4ConceptIndex(idea), 
											mapping_manager.getConfidence4Mapping(idea, ideb), 
											Utilities.R2L,
											Utilities.CLASSES));
						}
					}


				}
			}
			//}

			//if (Parameters.output_prop_mappings){

			for (int ide1 : mapping_manager.getDataPropertyAnchors().keySet()){		

				num_clean_mappings++;
				num_clean_dprop_mappings++;



				clean_mappings.add(
						new MappingObjectStr(
								index.getIRIStr4DataPropIndex(ide1), 
								index.getIRIStr4DataPropIndex(mapping_manager.getDataPropertyAnchors().get(ide1)), 
								mapping_manager.getConfidence4DataPropertyAnchor(ide1, mapping_manager.getDataPropertyAnchors().get(ide1)), 
								Utilities.EQ,
								Utilities.DATAPROPERTIES));


			}

			for (int ide1 : mapping_manager.getObjectPropertyAnchors().keySet()){

				num_clean_mappings++;
				num_clean_oprop_mappings++;

				clean_mappings.add(
						new MappingObjectStr(
								index.getIRIStr4ObjPropIndex(ide1),
								index.getIRIStr4ObjPropIndex(mapping_manager.getObjectPropertyAnchors().get(ide1)),								 
								mapping_manager.getConfidence4ObjectPropertyAnchor(ide1, mapping_manager.getObjectPropertyAnchors().get(ide1)),
								Utilities.EQ,
								Utilities.OBJECTPROPERTIES));
			}
			//}



			//if (Parameters.perform_instance_matching && Parameters.output_instance_mappings){

			for (int ide1 : mapping_manager.getInstanceMappings().keySet()){
				for (int ide2 : mapping_manager.getInstanceMappings().get(ide1)){

					num_clean_mappings++;
					num_clean_instance_mappings++;

					clean_mappings.add(
							new MappingObjectStr(
									index.getIRIStr4IndividualIndex(ide1), 
									index.getIRIStr4IndividualIndex(ide2), 
									mapping_manager.getConfidence4InstanceMapping(ide1, ide2),
									Utilities.EQ,
									Utilities.INSTANCES));

				}

			}
			//}


			//mapping_manager.setStringAnchors();
			//TODO uncomment
			/*System.out.println("\tNum clean mappings: " + num_clean_mappings);
			System.out.println("\t\tNum clean class mappings: " + num_clean_class_mappings);
			System.out.println("\t\tNum clean object property mappings: " + num_clean_oprop_mappings);
			System.out.println("\t\tNum clean data property mappings: " + num_clean_dprop_mappings);			
			System.out.println("\t\tNum clean instance mappings: " + num_clean_instance_mappings);
			 */



		}
		catch (Exception e){
			System.err.println("Error keeping mappings...");
			e.printStackTrace();
		}


	}


	double precision = 0.0;
	double recall = 0.0;
	double fmeasure = 0.0;



	protected void getPrecisionAndRecallMappings(String path_gs, Set<MappingObjectStr> mappings) throws Exception{


		loadMappingsGS(path_gs);

		double current_precision = 0.0;
		double current_recall = 0.0;
		double current_fmeasure = 0.0;

		Set <MappingObjectStr> intersection;

		//ALL UMLS MAPPINGS
		intersection=new HashSet<MappingObjectStr>(mappings);
		intersection.retainAll(mappings_gs);


		current_precision = (((double)intersection.size())/((double)mappings.size()));
		current_recall = (((double)intersection.size())/((double)mappings_gs.size()));
		current_fmeasure=((2*current_recall*current_precision)/(current_precision+current_recall));

		//We evaluate how P and R increases from original to clean
		precision = current_precision - precision;
		recall = current_recall - recall;
		fmeasure = current_fmeasure - fmeasure;

		//System.out.println("WRT GS MAPPINGS");
		System.out.println("\n\tPrecision Mappings: " + precision);
		System.out.println("\tRecall Mapping: " + recall);
		System.out.println("\tF measure: " + fmeasure);
		System.out.println("");


	}

	private Set<MappingObjectStr> mappings_gs = new HashSet<MappingObjectStr>();

	/**
	 * Load Gold Standard Mappings
	 * @throws Exception
	 */
	private void loadMappingsGS(String path_gs) throws Exception{

		ReadFile reader = new ReadFile(path_gs);

		mappings_gs.clear();


		String line;
		String[] elements;

		line=reader.readLine();


		while (line!=null) {

			if (line.indexOf("|")<0){
				line=reader.readLine();
				continue;
			}

			elements=line.split("\\|");


			mappings_gs.add(new MappingObjectStr(elements[0], elements[1]));


			line=reader.readLine();
		}		

		reader.closeBuffer();

	}


	private static Set<MappingObjectStr> emptyMappings() throws Exception{

		Set<MappingObjectStr> mappings = new HashSet<MappingObjectStr>();


		mappings.add(new MappingObjectStr(
				"http://csu6325.cs.ox.ac.uk/ontologies/matching_21_05_2012/emptyOntology.owl#lala", 
				"http://csu6325.cs.ox.ac.uk/ontologies/matching_31_05_2012/ontology_31_05_2012__18_53_50_221#lala", 
				1.0,
				Utilities.EQ));

		return mappings;


	}



	/**
	 * 
	 * @deprecated Use instead mappings manager reader
	 * @param file
	 * @return
	 * @throws Exception
	 */
	private static Set<MappingObjectStr> readMappings(String file) throws Exception{


		Set<MappingObjectStr> mappings = new HashSet<MappingObjectStr>();

		ReadFile reader = new ReadFile(file);


		String line;
		String[] elements;

		line=reader.readLine();

		int dir;

		while (line!=null) {

			if (line.indexOf("|")<0){
				line=reader.readLine();
				continue;
			}

			elements=line.split("\\|");

			if (elements.length<4)
				continue;


			if (elements[2].equals(">")){
				dir = Utilities.R2L;
			}
			else if (elements[2].equals("<")){
				dir = Utilities.L2R;
			}
			else {
				dir = Utilities.EQ;
			}


			mappings.add(new MappingObjectStr(elements[0], elements[1], Double.valueOf(elements[3]), dir));

			line=reader.readLine();
		}		

		reader.closeBuffer();


		return mappings;


	}




	private static String getHelpMessage(){
		return "LogMap's repair facility requires 7 parameters:\n" +
				"\t1. IRI ontology 1. e.g.: http://myonto1.owl  or  file:/C://myonto1.owl  or  file:/usr/local/myonto1.owl\n" +
				"\t2. IRI ontology 2. e.g.: http://myonto2.owl  or  file:/C://myonto2.owl  or  file:/usr/local/myonto2.owl\n" +
				"\t3. Format mappings e.g.: OWL  or  RDF  or  TXT\n" +
				"\t4. Full IRI or full Path:\n" +
				"\t\ta. Full IRI of input mappings if OWL format. e.g.: file:/C://mymappings.owl  or  file:/usr/local/mymappings.owl  or http://mymappings.owl\n" +
				"\t\tb. Full path of input mappings if formats RDF or TXT. e.g.: C://mymappings.rdf  or  /usr/local/mymappings.txt\n" +
				"\t5. Full output path for the repaired mappings: e.g. /usr/local/output_path or C://output_path\n" +
				"\t6. Extract modules for repair?: true or false\n" +
				"\t7. Check satisfiability after repair using HermiT? true or false\n";		
		//"\t4. Classify the input ontologies together with the mappings. e.g. true or false";
	}



	private static String path_gs;


	/**
	 * @param args
	 */
	public static void main(String[] args) {



		try {

			String iri_onto1;
			String iri_onto2;
			String format_mappings;
			String input_mappings_path;
			String output_path;
			boolean overlapping;
			boolean satisfiability_check;

			MappingsReaderManager readermanager;
			OntologyLoader loader1;
			OntologyLoader loader2;



			if (args.length==5)
				StatisticsOAEI2012(args[0], Integer.valueOf(args[1]), args[2], Boolean.valueOf(args[3]), Boolean.valueOf(args[4]));
			else
				StatisticsOAEI2012();


			if (true)
				return;

			/*
			if (args.length==1){

				if (args[0].toLowerCase().contains("help")){
					System.out.println("HELP:\n" + getHelpMessage());
					return;
				}

			}
			 */


			if (args.length!=7){
				System.out.println(getHelpMessage());
				return;
			}
			else{				
				iri_onto1=args[0];
				iri_onto2=args[1];
				format_mappings=args[2];
				input_mappings_path=args[3];
				output_path=args[4];
				overlapping=Boolean.valueOf(args[5]);
				satisfiability_check=Boolean.valueOf(args[6]);

			}



			LogOutput.printAlways("Loading ontologies...");
			loader1 = new OntologyLoader(iri_onto1);
			loader2 = new OntologyLoader(iri_onto2);
			LogOutput.printAlways("...Done");


			readermanager = new MappingsReaderManager(input_mappings_path, format_mappings);

			new LogMap2_RepairFacility(
					loader1.getOWLOntology(), 
					loader2.getOWLOntology(), 
					readermanager.getMappingObjects(),
					overlapping,
					true, //always optimal?
					satisfiability_check,
					output_path +  "/" + "mappings_repaired_with_LogMap");




		}
		catch (Exception e){
			e.printStackTrace();
		}


	}


	private static void StatisticsOAEI2012() throws Exception{

		int onto_pair;
		//onto_pair = Utilities.FMA2NCI;
		onto_pair = Utilities.FMA2SNOMED;
		//onto_pair = Utilities.SNOMED2NCI;
		String size;
		//size = "small";
		//size = "big";
		size = "whole";
		boolean overlapping;
		overlapping = false;

		boolean optimal;
		optimal = false;

		StatisticsOAEI2012("/usr/local/data/DataUMLS/UMLS_Onto_Versions/", onto_pair, size, overlapping, optimal);

	}

	/**
	 * Used to extract statistics from OAEI 2012 tool outputs
	 */
	private static void StatisticsOAEI2012(String path_base, int ontoPair, String sizePair, boolean overlapping, boolean optimal) throws Exception{

		long init,fin;

		String format_mappings;

		MappingsReaderManager readermanager;
		OntologyLoader loader1;
		OntologyLoader loader2;

		//String base_path = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/";
		String base_path = path_base;
		int onto_pair = ontoPair;
		String size = sizePair;


		String rootpath;
		String rootpath_fma2nci = base_path + "OAEI_datasets/oaei_2012/fma2nci/";
		String rootpath_fma2snomed = base_path + "OAEI_datasets/oaei_2012/fma2snmd/";
		String rootpath_snomed2nci = base_path + "OAEI_datasets/oaei_2012/snmd2nci/";

		String irirootpath;
		String irirootpath_fma2nci = "file:" + rootpath_fma2nci;
		String irirootpath_fma2snomed = "file:" + rootpath_fma2snomed;
		String irirootpath_snomed2nci = "file:" +  rootpath_snomed2nci;

		String modules="";
		String iterative="";
		if (overlapping)
			modules="_modules";
		if (optimal)
			iterative="_iterative";

		//String path_gs;

		//String irirootpath = "file:/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/fma2nci_dataset/";	
		//String mappings_path = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/OutputAlcomo/";

		String onto1;
		String onto2;
		String pattern;
		String extension;



		//format_mappings = MappingsReaderManager.FlatFormat;
		//extension = "txt";			

		format_mappings = MappingsReaderManager.OAEIFormat;
		extension = "rdf";

		//format_mappings = MappingsReaderManager.OWLFormat;
		//extension = "owl";


		if (onto_pair==Utilities.FMA2NCI){

			//path_gs = "/usr/local/data/DataUMLS/UMLS_source_data/onto_mappings_FMA_NCI_cleantDG.txt";
			//path_gs = "/usr/local/data/DataUMLS/UMLS_source_data/onto_mappings_FMA_NCI_dirty.txt";
			path_gs = base_path + "OAEI_datasets/UMLS_txt_mappings/onto_mappings_FMA_NCI_dirty.txt";


			irirootpath = irirootpath_fma2nci;
			if (size.equals("small")){
				onto1 = irirootpath + "oaei2012_FMA_small_overlapping_nci.owl";
				onto2 = irirootpath + "oaei2012_NCI_small_overlapping_fma.owl";
				pattern = "_small_fma2nci." + extension;
			}
			else if (size.equals("big")){
				onto1 = irirootpath + "oaei2012_FMA_extended_overlapping_nci.owl";
				onto2 = irirootpath + "oaei2012_NCI_extended_overlapping_fma.owl";
				pattern = "_big_fma2nci." + extension;
			}
			else{
				onto1 = irirootpath + "oaei2012_FMA_whole_ontology.owl";
				onto2 = irirootpath + "oaei2012_NCI_whole_ontology.owl";
				pattern = "_whole_fma2nci." + extension;
			}
		}
		else if (onto_pair==Utilities.FMA2SNOMED){

			//path_gs = "/usr/local/data/DataUMLS/UMLS_source_data/onto_mappings_FMA_SNOMED_cleantDG.txt";
			//path_gs = "/usr/local/data/DataUMLS/UMLS_source_data/onto_mappings_FMA_SNOMED_dirty.txt";
			path_gs = base_path + "OAEI_datasets/UMLS_txt_mappings/onto_mappings_FMA_SNOMED_dirty.txt";

			irirootpath = irirootpath_fma2snomed;

			if (size.equals("small")){
				onto1 = irirootpath + "oaei2012_FMA_small_overlapping_snomed.owl";
				onto2 = irirootpath + "oaei2012_SNOMED_small_overlapping_fma.owl";
				pattern = "_small_fma2snomed." + extension;
			}
			else if (size.equals("big")){
				onto1 = irirootpath + "oaei2012_FMA_extended_overlapping_snomed.owl";
				onto2 = irirootpath + "oaei2012_SNOMED_extended_overlapping_fma_nci.owl";
				pattern = "_big_fma2snomed." + extension;
			}
			else{
				onto1 = irirootpath_fma2nci + "oaei2012_FMA_whole_ontology.owl";
				onto2 = irirootpath + "oaei2012_SNOMED_whole_ontology.owl.zip";
				pattern = "_whole2_fma2snomed." + extension;
			}
		}
		else {

			//path_gs = "/usr/local/data/DataUMLS/UMLS_source_data/onto_mappings_SNOMED_NCI_cleantDG.txt";
			//path_gs = "/usr/local/data/DataUMLS/UMLS_source_data/onto_mappings_SNOMED_NCI_dirty.txt";
			path_gs = base_path + "OAEI_datasets/UMLS_txt_mappings/onto_mappings_SNOMED_NCI_dirty.txt";

			irirootpath = irirootpath_snomed2nci;

			if (size.equals("small")){
				onto2 = irirootpath + "oaei2012_NCI_small_overlapping_snomed.owl";
				onto1 = irirootpath + "oaei2012_SNOMED_small_overlapping_nci.owl";
				pattern = "_small_snomed2nci." + extension;
			}
			else if (size.equals("big")){
				onto2 = irirootpath + "oaei2012_NCI_extended_overlapping_snomed.owl";
				onto1 = irirootpath_fma2snomed + "oaei2012_SNOMED_extended_overlapping_fma_nci.owl";
				pattern = "_big_snomed2nci." + extension;
			}
			else{
				onto2 = irirootpath_fma2nci + "oaei2012_NCI_whole_ontology.owl";
				onto1 = irirootpath_fma2snomed + "oaei2012_SNOMED_whole_ontology.owl.zip";
				pattern = "_whole2_snomed2nci." + extension;
			}


		}

		//String mappings_path = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/Mappings_Tools/";
		//String mappings_path = "/usr/local/data/DataUMLS/UMLS_Onto_Versions/OAEI_datasets/Mappings_Tools_2012/";			
		//String irirootpath_mappings = "file:" + mappings_path;
		String mappings_path = base_path + "OAEI_datasets/Mappings_Tools_2012/";






		//onto1 = "http://csu6325.cs.ox.ac.uk/ontologies/matching_21_05_2012/emptyOntology.owl";
		//onto2 = "http://csu6325.cs.ox.ac.uk/ontologies/matching_31_05_2012/ontology_31_05_2012__18_53_50_221";
		//"http://csu6325.cs.ox.ac.uk/ontologies/matching_31_05_2012/ontology_31_05_2012__18_54_00_263";




		//String irirootpath_mappings = "file:" + mappings_path; 

		File directory = new File(mappings_path);
		String filenames[] = directory.list();


		LogOutput.printAlways("Loading ontologies...");
		loader1 = new OntologyLoader(onto1);
		loader2 = new OntologyLoader(onto2);
		LogOutput.printAlways("...Done");

		String fileNameNoExtension;


		//Iterate over mappings
		for(int i=0; i<filenames.length; i++){

			//if (!filenames[i].contains(task) || filenames[i].contains("mapevo") || filenames[i].contains("mappso"))
			//if (!filenames[i].contains("mappso_small.txt"))
			//	continue;
			//if (!filenames[i].equals("oaei2012_FMA2NCI_voted_mappings.txt"))
			//if (!filenames[i].equals("hertuda_small_fma2nci_repaired_with_Alcomo_Hermit.rdf"))
			//if (!filenames[i].equals("gommaBK_small_fma2snomed.txt"))
			//if (!filenames[i].equals("servomap_big_fma2snomed.txt"))
			//if (!filenames[i].contains("wmatch_small_fma2nci.rdf"))
			//if (!filenames[i].contains("whole2_snomed2nci_repaired_with_Alcomo_Hermit.rdf"))
			//if (!filenames[i].equals("logmap2_small_fma2snomed.rdf"))				
			//if (!filenames[i].contains(pattern) || filenames[i].contains("logmap"))
			if (!filenames[i].contains(pattern))
				continue;


			init = Calendar.getInstance().getTimeInMillis();

			fileNameNoExtension = filenames[i].split("\\.")[0];

			System.out.println("Evaluation mappings: " + filenames[i]);

			//Load mappings file				
			readermanager = new MappingsReaderManager(mappings_path + filenames[i], format_mappings);



			LogMap2_RepairFacility repair = new LogMap2_RepairFacility(
					loader1.getOWLOntology(), 
					loader2.getOWLOntology(), 
					readermanager.getMappingObjects(),
					overlapping,
					optimal,
					false,
					mappings_path + fileNameNoExtension + "_repaired_with_LogMap" + modules + iterative);
			//new RepairFacility(loader1.getOWLOntology(), loader2.getOWLOntology(), readMappings(mappings_path + filenames[i]));
			//new RepairFacility(loader1.getOWLOntology(), loader2.getOWLOntology(), emptyMappings());


			//Precision and recall
			//-----------------------
			repair.getPrecisionAndRecallMappings(path_gs, repair.getInputMappings());
			repair.getPrecisionAndRecallMappings(path_gs, repair.getCleanMappings()); //increment

			System.out.println("REMOVED CLAUSES /  size repair: " + repair.getSizeOfRepair());

			fin = Calendar.getInstance().getTimeInMillis();
			LogOutput.print("TOTAL REPAIR TIME (s): " + (float)((double)fin-(double)init)/1000.0);



			//TODO REPAIR with ALCOMO? in SNOMED
			if (false && onto_pair!=Utilities.SNOMED2NCI){ //do not reason with SNOMED and NCI
				//Satisfiability check!
				System.out.println("UNSAT original mappings: ");
				repair.checkSatisfiabilityMappings(repair.getInputMappings());
			}

			System.out.println("\n\n");				




		}//iter files








	}




	/**
	 * Comparator based on the mapping confidence
	 * @author Ernesto
	 *
	 */
	private class MappingComparator implements Comparator<MappingObjectStr> {

		/**
		 * We order by  confidence
		 */
		public int compare(MappingObjectStr m1, MappingObjectStr m2) {


			if (m1.getConfidence()<m2.getConfidence()){
				return -1;					
			}
			else{
				return 1;
			}


		}

	}







}
