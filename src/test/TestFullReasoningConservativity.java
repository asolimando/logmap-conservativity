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
package test;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import logmap.LogMapWrapper;

import org.semanticweb.elk.owlapi.ElkReasoner;
import org.semanticweb.elk.owlapi.ElkReasonerConfiguration;
import org.semanticweb.elk.reasoner.config.ReasonerConfiguration;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.owllink.OWLlinkHTTPXMLReasonerFactory;
import org.semanticweb.owlapi.owllink.OWLlinkReasonerRuntimeException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.reasoner.TimeOutException;

import auxStructures.HeterogeneousTuple;
import auxStructures.Pair;

import reasoning.KoncludeReasoning;
import repair.ConservativityRepairFacility;

import enumerations.OAEI_TRACK;
import enumerations.REASONER_KIND;
import enumerations.VIOL_KIND;

import uk.ac.manchester.cs.owl.owlapi.OWLSubClassOfAxiomImpl;
import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;
import util.FileUtil;
import util.OntoUtil;
import util.Params;
import util.Util;

public class TestFullReasoningConservativity {

	private static List<String> aligns = new ArrayList<>();
	private static OWLOntology fstO, sndO, alignOnto;
	private static Set<OWLAxiom> mappings;
	private static OWLReasoner reasoner;
//	private static ConservativityRepairFacility repairFac;

	private static final boolean conservativityAlso = true;
	
	private static OAEI_TRACK [] testOrderTracks = {
//		OAEI_TRACK.CONFERENCE,
//		OAEI_TRACK.LARGEBIOSMALL,
//		OAEI_TRACK.LARGEBIOBIG,
		OAEI_TRACK.LIBRARY,
//		OAEI_TRACK.ANATOMY,
	};
	
	private static REASONER_KIND [] reasoners = {
//		REASONER_KIND.KONCLUDE, 
//		REASONER_KIND.PELLET, 
//		REASONER_KIND.HERMIT,
		REASONER_KIND.ELK,
//		REASONER_KIND.ELKTRACE,
////		REASONER_KIND.FACT
		};
	
	private static int classesLimit = 50;
	private static int justificationsNumLimit = 50;
	private static int singleJustificationTimeout = 60;
	private static int allJustificationsTimeoutSingleJust = 60;

	private static int alignedOntoClassificationTimeout = 5 * 60;

	private static int violationsNumLimit = 50;
	private static int violationsJustNumLimit = 50;
	private static int violationsSingleJustificationTimeout = 60;
	private static int violationsAllJustificationsTimeoutSingleJust = 60;

	private static Map<OAEI_TRACK,Integer> classesLimitMap = new HashMap<>();
	private static Map<OAEI_TRACK,Integer> justificationsNumLimitMap = new HashMap<>();
	private static Map<OAEI_TRACK,Integer> singleJustificationTimeoutMap = new HashMap<>();
	private static Map<OAEI_TRACK,Integer> allJustificationsTimeoutSingleJustMap = new HashMap<>();
	private static Map<OAEI_TRACK,Integer> alignedOntoClassificationTimeoutMap = new HashMap<>();
	
	private static Map<OAEI_TRACK,Integer> violationsNumLimitMap = new HashMap<>();
	private static Map<OAEI_TRACK,Integer> violationsJustNumLimitMap = new HashMap<>();
	private static Map<OAEI_TRACK,Integer> violationsSingleJustificationTimeoutMap = new HashMap<>();
	private static Map<OAEI_TRACK,Integer> violationsAllJustificationsTimeoutSingleJustMap = new HashMap<>();
	
	private static int printEach = 2;

	static {
		Params.storeViolations = conservativityAlso;
		
		aligns.add("");
		
		classesLimitMap.put(OAEI_TRACK.CONFERENCE, classesLimit);
		classesLimitMap.put(OAEI_TRACK.ANATOMY, classesLimit);
		classesLimitMap.put(OAEI_TRACK.LIBRARY, classesLimit);
		classesLimitMap.put(OAEI_TRACK.LARGEBIOBIG, classesLimit);
		classesLimitMap.put(OAEI_TRACK.LARGEBIOSMALL, classesLimit);
		
		justificationsNumLimitMap.put(OAEI_TRACK.CONFERENCE, justificationsNumLimit);
		justificationsNumLimitMap.put(OAEI_TRACK.ANATOMY, justificationsNumLimit);
		justificationsNumLimitMap.put(OAEI_TRACK.LIBRARY, justificationsNumLimit);
		justificationsNumLimitMap.put(OAEI_TRACK.LARGEBIOBIG, justificationsNumLimit);
		justificationsNumLimitMap.put(OAEI_TRACK.LARGEBIOSMALL, justificationsNumLimit);

		singleJustificationTimeoutMap.put(OAEI_TRACK.CONFERENCE, singleJustificationTimeout);
		singleJustificationTimeoutMap.put(OAEI_TRACK.ANATOMY, singleJustificationTimeout);
		singleJustificationTimeoutMap.put(OAEI_TRACK.LIBRARY, singleJustificationTimeout);
		singleJustificationTimeoutMap.put(OAEI_TRACK.LARGEBIOBIG, singleJustificationTimeout);
		singleJustificationTimeoutMap.put(OAEI_TRACK.LARGEBIOSMALL, singleJustificationTimeout);

		allJustificationsTimeoutSingleJustMap.put(OAEI_TRACK.CONFERENCE, allJustificationsTimeoutSingleJust);
		allJustificationsTimeoutSingleJustMap.put(OAEI_TRACK.ANATOMY, allJustificationsTimeoutSingleJust);
		allJustificationsTimeoutSingleJustMap.put(OAEI_TRACK.LIBRARY, allJustificationsTimeoutSingleJust);
		allJustificationsTimeoutSingleJustMap.put(OAEI_TRACK.LARGEBIOBIG, allJustificationsTimeoutSingleJust);
		allJustificationsTimeoutSingleJustMap.put(OAEI_TRACK.LARGEBIOSMALL, allJustificationsTimeoutSingleJust);
		
		alignedOntoClassificationTimeoutMap.put(OAEI_TRACK.CONFERENCE, 10 * 60);
		alignedOntoClassificationTimeoutMap.put(OAEI_TRACK.ANATOMY, 10 * 60);
		alignedOntoClassificationTimeoutMap.put(OAEI_TRACK.LIBRARY, 20 * 60);
		alignedOntoClassificationTimeoutMap.put(OAEI_TRACK.LARGEBIOBIG, 60 * 60);
		alignedOntoClassificationTimeoutMap.put(OAEI_TRACK.LARGEBIOSMALL, 60 * 60);
		
		violationsNumLimitMap.put(OAEI_TRACK.CONFERENCE, violationsNumLimit);
		violationsNumLimitMap.put(OAEI_TRACK.ANATOMY, violationsNumLimit);
		violationsNumLimitMap.put(OAEI_TRACK.LIBRARY, violationsNumLimit);
		violationsNumLimitMap.put(OAEI_TRACK.LARGEBIOBIG, violationsNumLimit);
		violationsNumLimitMap.put(OAEI_TRACK.LARGEBIOSMALL, violationsNumLimit);
		
		violationsJustNumLimitMap.put(OAEI_TRACK.CONFERENCE, violationsJustNumLimit);
		violationsJustNumLimitMap.put(OAEI_TRACK.ANATOMY, violationsJustNumLimit);
		violationsJustNumLimitMap.put(OAEI_TRACK.LIBRARY, violationsJustNumLimit);
		violationsJustNumLimitMap.put(OAEI_TRACK.LARGEBIOBIG, violationsJustNumLimit);
		violationsJustNumLimitMap.put(OAEI_TRACK.LARGEBIOSMALL, violationsJustNumLimit);
		
		violationsSingleJustificationTimeoutMap.put(
				OAEI_TRACK.CONFERENCE, violationsSingleJustificationTimeout);
		violationsSingleJustificationTimeoutMap.put(
				OAEI_TRACK.ANATOMY, violationsSingleJustificationTimeout);
		violationsSingleJustificationTimeoutMap.put(
				OAEI_TRACK.LIBRARY, violationsSingleJustificationTimeout);
		violationsSingleJustificationTimeoutMap.put(
				OAEI_TRACK.LARGEBIOBIG, violationsSingleJustificationTimeout);
		violationsSingleJustificationTimeoutMap.put(
				OAEI_TRACK.LARGEBIOSMALL, violationsSingleJustificationTimeout);
		
		violationsAllJustificationsTimeoutSingleJustMap.put(
				OAEI_TRACK.CONFERENCE, violationsAllJustificationsTimeoutSingleJust);
		violationsAllJustificationsTimeoutSingleJustMap.put(
				OAEI_TRACK.ANATOMY, violationsAllJustificationsTimeoutSingleJust);
		violationsAllJustificationsTimeoutSingleJustMap.put(
				OAEI_TRACK.LIBRARY, violationsAllJustificationsTimeoutSingleJust);
		violationsAllJustificationsTimeoutSingleJustMap.put(
				OAEI_TRACK.LARGEBIOBIG, violationsAllJustificationsTimeoutSingleJust);
		violationsAllJustificationsTimeoutSingleJustMap.put(
				OAEI_TRACK.LARGEBIOSMALL, violationsAllJustificationsTimeoutSingleJust);
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		OWLOntologyManager manager = OntoUtil.getManager(false);
		
		Params.saveOnto = true;
		
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		String startTime = sdf.format(Calendar.getInstance().getTime());
		
		String testOutDir = Params.testOutDir + "FullReasoning/";
//		String testMappingDir = testOutDir + "mappings/";
		String testOntoDir = testOutDir + "ontologies/";
		
		FileUtil.createDirPath(testOutDir);
//		FileUtil.createDirPath(testMappingDir);
		FileUtil.createDirPath(testOntoDir);
		
		try {
			FileUtil.createTestDataFile(testOutDir + "data_" + Util.getCompactCurrTime() + ".txt");
			FileUtil.createLogFile(testOutDir + "log_" + Util.getCompactCurrTime() + ".txt");
		} catch (IOException e) {
			System.err.println("Error creating log and data files, aborting test");
			System.exit(-1);
		}

		FileUtil.writeLogAndConsole("Start: " + startTime);
		
		String prefix = "/home/ale/data/";
		Map<OAEI_TRACK, List<String>> alignMap = new HashMap<>();

		String [] conference = {
			prefix + "oaei2013/conference/alignments/AMLbk-cmt-conference.rdf",
			prefix + "oaei2013/conference/alignments/AMLbk-cmt-confof.rdf",
			prefix + "oaei2013/conference/alignments/AMLbk-cmt-edas.rdf",
			prefix + "oaei2013/conference/alignments/AMLbk-cmt-ekaw.rdf",
			prefix + "oaei2013/conference/alignments/AMLbk-cmt-iasted.rdf",
			prefix + "oaei2013/conference/alignments/AMLbk-cmt-sigkdd.rdf",
			prefix + "oaei2013/conference/alignments/AMLbk-conference-confof.rdf",
			prefix + "oaei2013/conference/alignments/AMLbk-conference-edas.rdf",
			prefix + "oaei2013/conference/alignments/AMLbk-conference-ekaw.rdf",
			prefix + "oaei2013/conference/alignments/AMLbk-conference-iasted.rdf",
			prefix + "oaei2013/conference/alignments/AMLbk-conference-sigkdd.rdf",
			prefix + "oaei2013/conference/alignments/AMLbk-confof-edas.rdf",
			prefix + "oaei2013/conference/alignments/AMLbk-confof-ekaw.rdf",
			prefix + "oaei2013/conference/alignments/AMLbk-confof-iasted.rdf",
			prefix + "oaei2013/conference/alignments/AMLbk-confof-sigkdd.rdf",
			prefix + "oaei2013/conference/alignments/AMLbk-edas-ekaw.rdf",
			prefix + "oaei2013/conference/alignments/AMLbk-edas-iasted.rdf",
			prefix + "oaei2013/conference/alignments/AMLbk-edas-sigkdd.rdf",
			prefix + "oaei2013/conference/alignments/AMLbk-ekaw-iasted.rdf",
			prefix + "oaei2013/conference/alignments/AMLbk-ekaw-sigkdd.rdf",
			prefix + "oaei2013/conference/alignments/AMLbk-iasted-sigkdd.rdf",
			prefix + "oaei2013/conference/alignments/XMapGen-cmt-conference.rdf",
			prefix + "oaei2013/conference/alignments/XMapGen-cmt-confof.rdf",
			prefix + "oaei2013/conference/alignments/XMapGen-cmt-edas.rdf",
				
			prefix + "oaei2013/conference/alignments/XMapGen-cmt-ekaw.rdf", // timeout
				
			prefix + "oaei2013/conference/alignments/XMapGen-cmt-iasted.rdf",
			prefix + "oaei2013/conference/alignments/XMapGen-cmt-sigkdd.rdf",
			prefix + "oaei2013/conference/alignments/XMapGen-conference-confof.rdf",
			prefix + "oaei2013/conference/alignments/XMapGen-conference-edas.rdf",
			prefix + "oaei2013/conference/alignments/XMapGen-conference-ekaw.rdf",
			prefix + "oaei2013/conference/alignments/XMapGen-conference-iasted.rdf",
			prefix + "oaei2013/conference/alignments/XMapGen-conference-sigkdd.rdf",
			prefix + "oaei2013/conference/alignments/XMapGen-confof-edas.rdf",
			prefix + "oaei2013/conference/alignments/XMapGen-confof-ekaw.rdf",
			prefix + "oaei2013/conference/alignments/XMapGen-confof-iasted.rdf",
			prefix + "oaei2013/conference/alignments/XMapGen-confof-sigkdd.rdf",
			prefix + "oaei2013/conference/alignments/XMapGen-edas-ekaw.rdf",
			prefix + "oaei2013/conference/alignments/XMapGen-edas-iasted.rdf",
			prefix + "oaei2013/conference/alignments/XMapGen-edas-sigkdd.rdf",
			prefix + "oaei2013/conference/alignments/XMapGen-ekaw-iasted.rdf",
			prefix + "oaei2013/conference/alignments/XMapGen-ekaw-sigkdd.rdf",
			prefix + "oaei2013/conference/alignments/XMapGen-iasted-sigkdd.rdf",
			prefix + "oaei2013/conference/alignments/MaasMatch-cmt-conference.rdf",
			prefix + "oaei2013/conference/alignments/MaasMatch-cmt-confof.rdf",
			prefix + "oaei2013/conference/alignments/MaasMatch-cmt-edas.rdf",
			prefix + "oaei2013/conference/alignments/MaasMatch-cmt-ekaw.rdf",
			prefix + "oaei2013/conference/alignments/MaasMatch-cmt-iasted.rdf",
			prefix + "oaei2013/conference/alignments/MaasMatch-cmt-sigkdd.rdf",
			prefix + "oaei2013/conference/alignments/MaasMatch-conference-confof.rdf",
			prefix + "oaei2013/conference/alignments/MaasMatch-conference-edas.rdf",
			prefix + "oaei2013/conference/alignments/MaasMatch-conference-ekaw.rdf",
			prefix + "oaei2013/conference/alignments/MaasMatch-conference-iasted.rdf",
			prefix + "oaei2013/conference/alignments/MaasMatch-conference-sigkdd.rdf",
			prefix + "oaei2013/conference/alignments/MaasMatch-confof-edas.rdf",
			prefix + "oaei2013/conference/alignments/MaasMatch-confof-ekaw.rdf",
			prefix + "oaei2013/conference/alignments/MaasMatch-confof-iasted.rdf",
			prefix + "oaei2013/conference/alignments/MaasMatch-confof-sigkdd.rdf",
			prefix + "oaei2013/conference/alignments/MaasMatch-edas-ekaw.rdf",
			prefix + "oaei2013/conference/alignments/MaasMatch-edas-iasted.rdf",
			prefix + "oaei2013/conference/alignments/MaasMatch-edas-sigkdd.rdf",
			prefix + "oaei2013/conference/alignments/MaasMatch-ekaw-iasted.rdf",
			prefix + "oaei2013/conference/alignments/MaasMatch-ekaw-sigkdd.rdf",
			prefix + "oaei2013/conference/alignments/MaasMatch-iasted-sigkdd.rdf",
			prefix + "oaei2014/conference/alignments/MaasMatch-cmt-conference.rdf",
			prefix + "oaei2014/conference/alignments/MaasMatch-cmt-confof.rdf",
			prefix + "oaei2014/conference/alignments/MaasMatch-cmt-edas.rdf",
			prefix + "oaei2014/conference/alignments/MaasMatch-cmt-ekaw.rdf",
			prefix + "oaei2014/conference/alignments/MaasMatch-cmt-iasted.rdf",
			prefix + "oaei2014/conference/alignments/MaasMatch-cmt-sigkdd.rdf",
			prefix + "oaei2014/conference/alignments/MaasMatch-conference-confof.rdf",
			prefix + "oaei2014/conference/alignments/MaasMatch-conference-edas.rdf",
			prefix + "oaei2014/conference/alignments/MaasMatch-conference-ekaw.rdf",
			prefix + "oaei2014/conference/alignments/MaasMatch-conference-iasted.rdf",
			prefix + "oaei2014/conference/alignments/MaasMatch-conference-sigkdd.rdf",
			prefix + "oaei2014/conference/alignments/MaasMatch-confof-edas.rdf",
			prefix + "oaei2014/conference/alignments/MaasMatch-confof-ekaw.rdf",
			prefix + "oaei2014/conference/alignments/MaasMatch-confof-iasted.rdf",
			prefix + "oaei2014/conference/alignments/MaasMatch-confof-sigkdd.rdf",
			prefix + "oaei2014/conference/alignments/MaasMatch-edas-ekaw.rdf",
			prefix + "oaei2014/conference/alignments/MaasMatch-edas-iasted.rdf",
			prefix + "oaei2014/conference/alignments/MaasMatch-edas-sigkdd.rdf",
			prefix + "oaei2014/conference/alignments/MaasMatch-ekaw-iasted.rdf",
			prefix + "oaei2014/conference/alignments/MaasMatch-ekaw-sigkdd.rdf",
			prefix + "oaei2014/conference/alignments/MaasMatch-iasted-sigkdd.rdf",
			prefix + "oaei2013/conference/alignments/YAM++-cmt-conference.rdf",
			prefix + "oaei2013/conference/alignments/YAM++-cmt-confof.rdf",
			prefix + "oaei2013/conference/alignments/YAM++-cmt-edas.rdf",
			prefix + "oaei2013/conference/alignments/YAM++-cmt-ekaw.rdf",
			prefix + "oaei2013/conference/alignments/YAM++-cmt-iasted.rdf",
			prefix + "oaei2013/conference/alignments/YAM++-cmt-sigkdd.rdf",
			prefix + "oaei2013/conference/alignments/YAM++-conference-confof.rdf",
			prefix + "oaei2013/conference/alignments/YAM++-conference-edas.rdf",
			prefix + "oaei2013/conference/alignments/YAM++-conference-ekaw.rdf",
			prefix + "oaei2013/conference/alignments/YAM++-conference-iasted.rdf",
			prefix + "oaei2013/conference/alignments/YAM++-conference-sigkdd.rdf",
			prefix + "oaei2013/conference/alignments/YAM++-confof-edas.rdf",
			prefix + "oaei2013/conference/alignments/YAM++-confof-ekaw.rdf",
			prefix + "oaei2013/conference/alignments/YAM++-confof-iasted.rdf",
			prefix + "oaei2013/conference/alignments/YAM++-confof-sigkdd.rdf",
			prefix + "oaei2013/conference/alignments/YAM++-edas-ekaw.rdf",
			prefix + "oaei2013/conference/alignments/YAM++-edas-iasted.rdf",
			prefix + "oaei2013/conference/alignments/YAM++-edas-sigkdd.rdf",
			prefix + "oaei2013/conference/alignments/YAM++-ekaw-iasted.rdf",
			prefix + "oaei2013/conference/alignments/YAM++-ekaw-sigkdd.rdf",
			prefix + "oaei2013/conference/alignments/YAM++-iasted-sigkdd.rdf",
			prefix + "oaei2014/conference/alignments/AML-cmt-conference.rdf",
			prefix + "oaei2014/conference/alignments/AML-cmt-confof.rdf",
			prefix + "oaei2014/conference/alignments/AML-cmt-edas.rdf",
			prefix + "oaei2014/conference/alignments/AML-cmt-ekaw.rdf",
			prefix + "oaei2014/conference/alignments/AML-cmt-iasted.rdf",
			prefix + "oaei2014/conference/alignments/AML-cmt-sigkdd.rdf",
			prefix + "oaei2014/conference/alignments/AML-conference-confof.rdf",
			prefix + "oaei2014/conference/alignments/AML-conference-edas.rdf",
			prefix + "oaei2014/conference/alignments/AML-conference-ekaw.rdf",
			prefix + "oaei2014/conference/alignments/AML-conference-iasted.rdf",
			prefix + "oaei2014/conference/alignments/AML-conference-sigkdd.rdf",
			prefix + "oaei2014/conference/alignments/AML-confof-edas.rdf",
			prefix + "oaei2014/conference/alignments/AML-confof-ekaw.rdf",
			prefix + "oaei2014/conference/alignments/AML-confof-iasted.rdf",
			prefix + "oaei2014/conference/alignments/AML-confof-sigkdd.rdf",
			prefix + "oaei2014/conference/alignments/AML-edas-ekaw.rdf",
			prefix + "oaei2014/conference/alignments/AML-edas-iasted.rdf",
			prefix + "oaei2014/conference/alignments/AML-edas-sigkdd.rdf",
			prefix + "oaei2014/conference/alignments/AML-ekaw-iasted.rdf",
			prefix + "oaei2014/conference/alignments/AML-ekaw-sigkdd.rdf",
			prefix + "oaei2014/conference/alignments/AML-iasted-sigkdd.rdf",
			prefix + "oaei2013/conference/alignments/XMapSiG1_4-cmt-conference.rdf",
			prefix + "oaei2013/conference/alignments/XMapSiG1_4-cmt-confof.rdf",
			prefix + "oaei2013/conference/alignments/XMapSiG1_4-cmt-edas.rdf",
			prefix + "oaei2013/conference/alignments/XMapSiG1_4-cmt-ekaw.rdf",
			prefix + "oaei2013/conference/alignments/XMapSiG1_4-cmt-iasted.rdf",
			prefix + "oaei2013/conference/alignments/XMapSiG1_4-cmt-sigkdd.rdf",
			prefix + "oaei2013/conference/alignments/XMapSiG1_4-conference-confof.rdf",
			prefix + "oaei2013/conference/alignments/XMapSiG1_4-conference-edas.rdf",
			prefix + "oaei2013/conference/alignments/XMapSiG1_4-conference-ekaw.rdf",
			prefix + "oaei2013/conference/alignments/XMapSiG1_4-conference-iasted.rdf",
			prefix + "oaei2013/conference/alignments/XMapSiG1_4-conference-sigkdd.rdf",
			prefix + "oaei2013/conference/alignments/XMapSiG1_4-confof-edas.rdf",
			prefix + "oaei2013/conference/alignments/XMapSiG1_4-confof-ekaw.rdf",
			prefix + "oaei2013/conference/alignments/XMapSiG1_4-confof-iasted.rdf",
			prefix + "oaei2013/conference/alignments/XMapSiG1_4-confof-sigkdd.rdf",
			prefix + "oaei2013/conference/alignments/XMapSiG1_4-edas-ekaw.rdf",
			prefix + "oaei2013/conference/alignments/XMapSiG1_4-edas-iasted.rdf",
			prefix + "oaei2013/conference/alignments/XMapSiG1_4-edas-sigkdd.rdf",
			prefix + "oaei2013/conference/alignments/XMapSiG1_4-ekaw-iasted.rdf",
			prefix + "oaei2013/conference/alignments/XMapSiG1_4-ekaw-sigkdd.rdf",
			prefix + "oaei2013/conference/alignments/XMapSiG1_4-iasted-sigkdd.rdf",
			prefix + "oaei2014/conference/alignments/AOTL-cmt-conference.rdf",
			prefix + "oaei2014/conference/alignments/AOTL-cmt-confof.rdf",
			prefix + "oaei2014/conference/alignments/AOTL-cmt-edas.rdf",
			prefix + "oaei2014/conference/alignments/AOTL-cmt-ekaw.rdf",
			prefix + "oaei2014/conference/alignments/AOTL-cmt-iasted.rdf",
			prefix + "oaei2014/conference/alignments/AOTL-cmt-sigkdd.rdf",
			prefix + "oaei2014/conference/alignments/AOTL-conference-confof.rdf",
			prefix + "oaei2014/conference/alignments/AOTL-conference-edas.rdf",
			prefix + "oaei2014/conference/alignments/AOTL-conference-ekaw.rdf",
			prefix + "oaei2014/conference/alignments/AOTL-conference-iasted.rdf",
			prefix + "oaei2014/conference/alignments/AOTL-conference-sigkdd.rdf",
			prefix + "oaei2014/conference/alignments/AOTL-confof-edas.rdf",
			prefix + "oaei2014/conference/alignments/AOTL-confof-ekaw.rdf",
			prefix + "oaei2014/conference/alignments/AOTL-confof-iasted.rdf",
			prefix + "oaei2014/conference/alignments/AOTL-confof-sigkdd.rdf",
			prefix + "oaei2014/conference/alignments/AOTL-edas-ekaw.rdf",
			prefix + "oaei2014/conference/alignments/AOTL-edas-iasted.rdf",
			prefix + "oaei2014/conference/alignments/AOTL-edas-sigkdd.rdf",
			prefix + "oaei2014/conference/alignments/AOTL-ekaw-iasted.rdf",
			prefix + "oaei2014/conference/alignments/AOTL-ekaw-sigkdd.rdf",
			prefix + "oaei2014/conference/alignments/AOTL-iasted-sigkdd.rdf",
			
			prefix + "oaei2014/conference/reference/cmt-conference.rdf",
			prefix + "oaei2014/conference/reference/cmt-confof.rdf",
			prefix + "oaei2014/conference/reference/cmt-edas.rdf",
			prefix + "oaei2014/conference/reference/cmt-ekaw.rdf",
			prefix + "oaei2014/conference/reference/cmt-iasted.rdf",
			prefix + "oaei2014/conference/reference/cmt-sigkdd.rdf",
			prefix + "oaei2014/conference/reference/conference-confof.rdf",
			prefix + "oaei2014/conference/reference/conference-edas.rdf",
			prefix + "oaei2014/conference/reference/conference-ekaw.rdf",
			prefix + "oaei2014/conference/reference/conference-iasted.rdf",
			prefix + "oaei2014/conference/reference/conference-sigkdd.rdf",
			prefix + "oaei2014/conference/reference/confof-edas.rdf",
			prefix + "oaei2014/conference/reference/confof-ekaw.rdf",
			prefix + "oaei2014/conference/reference/confof-iasted.rdf",
			prefix + "oaei2014/conference/reference/confof-sigkdd.rdf",
			prefix + "oaei2014/conference/reference/edas-ekaw.rdf",
			prefix + "oaei2014/conference/reference/edas-iasted.rdf",
			prefix + "oaei2014/conference/reference/edas-sigkdd.rdf",
			prefix + "oaei2014/conference/reference/ekaw-iasted.rdf",
			prefix + "oaei2014/conference/reference/ekaw-sigkdd.rdf",
			prefix + "oaei2014/conference/reference/iasted-sigkdd.rdf",
		};
		
		String [] largebiosmall = {
				prefix + "oaei2013/largebio/alignments/GOMMA_small_fma2nci.rdf",
				prefix + "oaei2013/largebio/alignments/GOMMA_small_fma2snomed.rdf",
				prefix + "oaei2013/largebio/alignments/GOMMA_small_snomed2nci.rdf",
				prefix + "oaei2013/largebio/alignments/IAMA_small_fma2nci.rdf",
				prefix + "oaei2013/largebio/alignments/IAMA_small_fma2snomed.rdf",
				prefix + "oaei2013/largebio/alignments/IAMA_small_snomed2nci.rdf",
				prefix + "oaei2014/largebio/alignments/AML_small_fma2nci.rdf",
				prefix + "oaei2014/largebio/alignments/AML_small_fma2snomed.rdf",
				prefix + "oaei2014/largebio/alignments/AML_small_snomed2nci.rdf",
				prefix + "oaei2014/largebio/alignments/LogMapBio_small_fma2nci.rdf",
				prefix + "oaei2014/largebio/alignments/LogMapBio_small_fma2snomed.rdf",
				prefix + "oaei2014/largebio/alignments/LogMapBio_small_snomed2nci.rdf",				
				prefix + "oaei2013/largebio/alignments/YAM++_small_fma2nci.rdf",
				prefix + "oaei2013/largebio/alignments/YAM++_small_fma2snomed.rdf",
				prefix + "oaei2013/largebio/alignments/YAM++_small_snomed2nci.rdf",
				prefix + "oaei2014/largebio/alignments/OMReasoner_small_fma2nci.rdf",
				prefix + "oaei2014/largebio/alignments/OMReasoner_small_fma2snomed.rdf",
				prefix + "oaei2014/largebio/alignments/OMReasoner_small_snomed2nci.rdf",
				prefix + "oaei2014/largebio/alignments/MaasMatch_small_fma2nci.rdf",
				prefix + "oaei2014/largebio/alignments/MaasMatch_small_fma2snomed.rdf",
				prefix + "oaei2014/largebio/alignments/MaasMatch_small_snomed2nci.rdf",
				
				prefix + "oaei2013/largebio/reference/oaei2013_FMA2NCI_original_UMLS_mappings_with_confidence.rdf",
				prefix + "oaei2013/largebio/reference/oaei2013_FMA2SNOMED_original_UMLS_mappings_with_confidence.rdf",
				prefix + "oaei2013/largebio/reference/oaei2013_SNOMED2NCI_original_UMLS_mappings_with_confidence.rdf",
		};
		
		String [] largebiobig = {
				prefix + "oaei2013/largebio/alignments/GOMMA_big_fma2nci.rdf",
				prefix + "oaei2013/largebio/alignments/GOMMA_big_fma2snomed.rdf",
				prefix + "oaei2013/largebio/alignments/GOMMA_big_snomed2nci.rdf",
				prefix + "oaei2013/largebio/alignments/IAMA_big_fma2nci.rdf",
				prefix + "oaei2013/largebio/alignments/IAMA_big_fma2snomed.rdf",
				prefix + "oaei2013/largebio/alignments/IAMA_big_snomed2nci.rdf",
				prefix + "oaei2014/largebio/alignments/AML_big_fma2nci.rdf",
				prefix + "oaei2014/largebio/alignments/AML_big_fma2snomed.rdf",
				prefix + "oaei2014/largebio/alignments/AML_big_snomed2nci.rdf",
				prefix + "oaei2014/largebio/alignments/LogMapBio_big_fma2nci.rdf",
				prefix + "oaei2014/largebio/alignments/LogMapBio_big_fma2snomed.rdf",
				prefix + "oaei2014/largebio/alignments/LogMapBio_big_snomed2nci.rdf",				
				prefix + "oaei2013/largebio/alignments/YAM++_big_fma2nci.rdf",
				prefix + "oaei2013/largebio/alignments/YAM++_big_fma2snomed.rdf",
				prefix + "oaei2013/largebio/alignments/YAM++_big_snomed2nci.rdf",
				prefix + "oaei2014/largebio/alignments/OMReasoner_big_fma2nci.rdf",

				prefix + "oaei2013/largebio/reference/oaei2013_FMA2NCI_original_UMLS_mappings_with_confidence.rdf",
				prefix + "oaei2013/largebio/reference/oaei2013_FMA2SNOMED_original_UMLS_mappings_with_confidence.rdf",
				prefix + "oaei2013/largebio/reference/oaei2013_SNOMED2NCI_original_UMLS_mappings_with_confidence.rdf",
		};
		
		String [] library = {
				prefix + "oaei2014/library/alignments/AML.rdf",
				prefix + "oaei2013/library/alignments/xmapSig.rdf",
				prefix + "oaei2014/library/alignments/RSDLWB.rdf",
				prefix + "oaei2013/library/alignments/LogMap.rdf",
				prefix + "oaei2013/library/alignments/IAMA.rdf",
				prefix + "oaei2013/library/alignments/Hertuda.rdf",
//				prefix + "oaei2013/library/alignments/xmapGen.rdf", 12GB not enough
				prefix + "oaei2014/library/alignments/MaasMatch.rdf",
				prefix + "oaei2014/library/alignments/XMap.rdf",
				prefix + "oaei2013/library/alignments/AML.rdf",
				prefix + "oaei2013/library/alignments/YAM++.rdf",
				prefix + "oaei2013/library/alignments/ODGOMS.rdf",
				prefix + "oaei2013/library/alignments/StringsAuto.rdf",				
				prefix + "oaei2014/library/alignments/LogMapC.rdf",
				
				prefix + "oaei2014/library/reference/stw-thesoz.rdf"
		};

		String [] anatomy = {
				prefix + "oaei2013/anatomy/alignments/IAMA.rdf",
				prefix + "oaei2013/anatomy/alignments/WikiMatch.rdf",
				prefix + "oaei2013/anatomy/alignments/ODGOMS.rdf",
				prefix + "oaei2013/anatomy/alignments/AMLbk.rdf",
				prefix + "oaei2013/anatomy/alignments/GOMMAbk.rdf",
				prefix + "oaei2013/anatomy/alignments/YAM++.rdf",
				prefix + "oaei2013/anatomy/alignments/WeSeE.rdf",
				prefix + "oaei2013/anatomy/alignments/MaasMatch.rdf",
				
				prefix + "oaei2014/anatomy/alignments/RSDLWB.rdf",
				prefix + "oaei2014/anatomy/alignments/LogMapC.rdf",
				prefix + "oaei2014/anatomy/alignments/AOT.rdf",
				prefix + "oaei2014/anatomy/alignments/AOTL.rdf",
				prefix + "oaei2014/anatomy/alignments/LogMapBio.rdf",
				prefix + "oaei2014/anatomy/alignments/AML.rdf",

				prefix + "oaei2014/anatomy/reference/mouse-human.rdf"
		};
		
		alignMap.put(OAEI_TRACK.ANATOMY, Arrays.asList(anatomy));
		
		alignMap.put(OAEI_TRACK.LARGEBIOSMALL, Arrays.asList(largebiosmall));

		alignMap.put(OAEI_TRACK.LARGEBIOBIG, Arrays.asList(largebiobig));
		
		alignMap.put(OAEI_TRACK.LIBRARY, Arrays.asList(library));

		alignMap.put(OAEI_TRACK.CONFERENCE, Arrays.asList(conference));
		
		int rowNum = 1, pickRow = 150;
		
		for (OAEI_TRACK track : testOrderTracks) {
		
			if(!alignMap.containsKey(track))
				continue;

			classesLimit = classesLimitMap.get(track);
			justificationsNumLimit = justificationsNumLimitMap.get(track);
			singleJustificationTimeout = singleJustificationTimeoutMap.get(track);
			allJustificationsTimeoutSingleJust = allJustificationsTimeoutSingleJustMap.get(track);
			alignedOntoClassificationTimeout = alignedOntoClassificationTimeoutMap.get(track);
			
			violationsNumLimit = violationsNumLimitMap.get(track);
			violationsJustNumLimit = violationsJustNumLimitMap.get(track);
			violationsSingleJustificationTimeout = violationsSingleJustificationTimeoutMap.get(track);
			violationsAllJustificationsTimeoutSingleJust = violationsAllJustificationsTimeoutSingleJustMap.get(track);
			
			for (String alignPath : alignMap.get(track)) {
			
				String year = (alignPath.contains("2013") ? "2013" : "2014");
				String matcher = "reference";
				
				String fstOnto = "", 
				sndOnto = "",
				alignOntoPath = "",
				mappingDir = prefix + "oaei" + year + "/" + track.getDir() + "/";
	
				boolean isRefAlign = alignPath.contains("/reference/");
				
				if(track.equals(OAEI_TRACK.ANATOMY)){
					fstOnto = mappingDir + "onto/mouse.owl";
					sndOnto = mappingDir + "onto/human.owl";
					if(!isRefAlign)
						matcher = alignPath.substring(alignPath.lastIndexOf("/")+1, 
							alignPath.length()-4);
				}
				else if(track.equals(OAEI_TRACK.LIBRARY)){
					fstOnto = mappingDir + "onto/stw.owl";
					sndOnto = mappingDir + "onto/thesoz.owl";
					if(!isRefAlign)
						matcher = alignPath.substring(alignPath.lastIndexOf("/")+1, 
							alignPath.length()-4);
				}
				else if(track.equals(OAEI_TRACK.CONFERENCE)){
					String [] ontos = alignPath.substring(
							alignPath.lastIndexOf("/")+1, 
							alignPath.length()-4).split("-");
					
					int offset = isRefAlign ? 1 : 0;
					
					fstOnto = mappingDir + "onto/" + ontos[1-offset] + ".owl";
					sndOnto = mappingDir + "onto/" + ontos[2-offset] + ".owl";
					if(!isRefAlign)
						matcher = ontos[0];
				}
				else if(track.equals(OAEI_TRACK.LARGEBIOSMALL) || 
						track.equals(OAEI_TRACK.LARGEBIOBIG)){
					Map<String,String> largebioOntologies = Params.largebioOntologies13;
					if(year.equals("2014"))
						largebioOntologies = Params.largebioOntologies14;

					if(!isRefAlign){
	//					AML_big_fma2nci
						String [] chunks = alignPath.substring(
								alignPath.lastIndexOf("/")+1, 
								alignPath.length()-4).split("_");
						String [] ontos = chunks[2].split("2");
						matcher = chunks[0];
												
						fstOnto = mappingDir + "onto/" + 
								largebioOntologies.get(chunks[1]+ontos[0]+ontos[1]);
						sndOnto = mappingDir + "onto/" + 
								largebioOntologies.get(chunks[1]+ontos[1]+ontos[0]);
					}
					else {
						String [] ontoNames = alignPath.substring(
								alignPath.lastIndexOf("/")+1, 
								alignPath.length()-4).split("_")[1].split("2");
						
						ontoNames[0] = ontoNames[0].toLowerCase();
						ontoNames[1] = ontoNames[1].toLowerCase();

						String ontosize = 
								track.equals(OAEI_TRACK.LARGEBIOSMALL) ? 
										"small" : "big";
						
						fstOnto = mappingDir + "onto/" + largebioOntologies.get(
								ontosize + ontoNames[0] + ontoNames[1]);
						sndOnto = mappingDir + "onto/" + largebioOntologies.get(
								ontosize + ontoNames[1] + ontoNames[0]);
					}
				}
				
				FileUtil.writeLogAndConsole("\n" + Util.getCurrTime() + ": " + alignPath);
				File fileAlign = new File(alignPath);
								
				if(!fileAlign.exists()){
					FileUtil.writeErrorLogAndConsole(
							"Mapping filename does not exist!");
					continue;
				}
				
				loadOntologies(fstOnto, sndOnto, manager, true);
		
				Set<MappingObjectStr> align = 
						LogMapWrapper.getMappings(alignPath, fstO , sndO);
				
				mappings = OntoUtil.convertAlignmentToAxioms(fstO, sndO, align);
				
				alignOnto = OntoUtil.getAlignedOntology(manager, mappings, fstO, sndO);
		
				if(Params.saveOnto){
					FileUtil.writeLogAndConsole("Saving starting aligned ontology");
					String ontoName = alignPath.substring(
							alignPath.lastIndexOf('/')+1, 
							alignPath.lastIndexOf('.'));
					alignOntoPath = testOntoDir + track.getCompactName() 
							+ "-" + ontoName + "-original.owl";
					try {
						OntoUtil.save(alignOnto, alignOntoPath, manager);
					} catch (OWLOntologyStorageException | OWLOntologyCreationException
							| IOException e) {
						e.printStackTrace();
					}
				}
				
//				FileUtil.enableDataOutFileBuffering();
				
				long classTime = 0;
				List<OWLClass> unsatClasses = null;
				int numUnsatClasses = 0;
				int numViolations = 0;
				List<OWLSubClassOfAxiom> violsAxioms = null;
				
				for (REASONER_KIND rk : reasoners) {
					
//					if(pickRow > 0 && rowNum < pickRow){
//						rowNum++;
//						continue;
//					}
					try{
						FileUtil.writeLogAndConsole(rk + ": " + Util.getCurrTime());
						
						Params.alignOntoClassificationTimeout = alignedOntoClassificationTimeout;
						// in this way it does not try other reasoners if timeout is reached
						Params.reasonerKind = rk;
						Params.reasonerAfterTimeout = rk;
						
						boolean isKonclude = rk.equals(REASONER_KIND.KONCLUDE);
						
						int ret = isKonclude ? 0 : setupReasoner(manager, alignOnto, rk);
						
						// reuse that of ELK
						if(ret == 0 && !rk.equals(REASONER_KIND.ELKTRACE))
							classTime = isKonclude ? 
							KoncludeReasoning.computeClassification(alignOntoPath, 
									Params.alignOntoClassificationTimeout) 
									: classificationTask();
						else if (ret < 0)
							classTime = ret;
						
						if(classTime == -100){
							FileUtil.writeErrorLogAndConsole("Unsat aligned onto, skipping alignment");
							break;
						}
							
						long timeSingleJustAVG = -1, timeAllJustAVG = -1;
						long timeSingleJustTOT = -1, timeAllJustTOT = -1;
						int numSingleJustOk = -1, numSingleJustTimeout = -1, numSingleJustError = -1;
						int numAllJustOk = -1, numAllJustTimeout = -1, numAllJustError = -1;
						
						HeterogeneousTuple timeSingleJustTuple = null,
								timeAllJustTuple = null;
						
						if(classTime < -1){
							FileUtil.writeErrorLogAndConsole(rk + " failed");
	//						continue;
						}
						else if(classTime == -1){
							FileUtil.writeErrorLogAndConsole(rk + " timed out");						
						}
						else
							FileUtil.writeLogAndConsole(rk + " classification time: " + classTime);
						
						Set<Explanation<OWLAxiom>> expls = new HashSet<>();
						
						long timeSingleJustViolAVG = -1, timeAllJustViolAVG = -1;
						long timeSingleJustViolTOT = -1, timeAllJustViolTOT = -1;
						int numSingleJustViolOk = -1, numSingleJustViolTimeout = -1, 
								numSingleJustViolError = -1;
						int numAllJustViolOk = -1, numAllJustViolTimeout = -1, 
								numAllJustViolError = -1;
	
						Set<Explanation<OWLAxiom>> explsViols = new HashSet<>();
						
						if(!isKonclude && classTime > -1){
							
							if(!rk.equals(REASONER_KIND.ELKTRACE)){
								// detect unsat classes, keep only the max allowed number after random shufflings
								unsatClasses = new LinkedList<>(getUnsatClasses());
								if(classesLimit < (numUnsatClasses = unsatClasses.size())){
									Collections.shuffle(unsatClasses);
									unsatClasses = unsatClasses.subList(0, classesLimit);
								}
							}
							
							timeSingleJustTuple = singleJustificationTask(
									unsatClasses, alignOnto, rk, 
									singleJustificationTimeout);
							timeSingleJustAVG = (Long) timeSingleJustTuple.get(0);
							timeSingleJustTOT = (Long) timeSingleJustTuple.get(1);
							numSingleJustOk = (Integer) timeSingleJustTuple.get(2);
							numSingleJustTimeout = (Integer) timeSingleJustTuple.get(3);
							numSingleJustError = (Integer) timeSingleJustTuple.get(4);
	
							if(timeSingleJustAVG < 0 || timeSingleJustTOT < 0){
								FileUtil.writeErrorLogAndConsole(rk + " failed");
				//				continue;
							}
							FileUtil.writeLogAndConsole(rk + " single justifications time (AVG,TOT): " 
									+ timeSingleJustAVG + ", " + timeSingleJustTOT);
	
							FileUtil.writeLogAndConsole(
									rk + " single justification (ok,timeout,error): " + 
											numSingleJustOk + ", " + 
											numSingleJustTimeout + ", " + 
											numSingleJustError);
							
							timeAllJustTuple = allJustificationsTask(unsatClasses, 
									alignOnto, rk, justificationsNumLimit, 
									allJustificationsTimeoutSingleJust, expls);
							timeAllJustAVG = (Long) timeAllJustTuple.get(0);
							timeAllJustTOT = (Long) timeAllJustTuple.get(1);
							numAllJustOk = (Integer) timeAllJustTuple.get(2);
							numAllJustTimeout = (Integer) timeAllJustTuple.get(3);
							numAllJustError = (Integer) timeAllJustTuple.get(4);
							
							if(timeAllJustAVG < 0 || timeAllJustTOT < 0){
								FileUtil.writeErrorLogAndConsole(rk + " failed");
				//				continue;
							}
							FileUtil.writeLogAndConsole(rk + " all justifications time (AVG,TOT): " 
									+ timeAllJustAVG + ", " + timeAllJustTOT);
						
							FileUtil.writeLogAndConsole(
									rk + " all " + expls.size() + " justifications (ok,timeout,error): " + 
											numAllJustOk + ", " + numAllJustTimeout + ", " + 
											numAllJustError);
							
							boolean failed = false;
							
							if(conservativityAlso){
								
								if(!rk.equals(REASONER_KIND.ELKTRACE)){
									ConservativityRepairFacility repairFac = 
											new ConservativityRepairFacility(true, 
													fstO, sndO, manager, align);
									
									if(repairFac.hasFailed()){
										FileUtil.writeErrorLogAndConsole("Conservativity " +
												"facility failed, skipping the test");
										repairFac.dispose();
										failed = true;
									}
									else {
										repairFac.detectViolations(false,false);
										List<Pair<Integer>> viols = 
												new ArrayList<Pair<Integer>>(
														repairFac.getViolations(true, VIOL_KIND.FULL, 
														repairFac.getRepairStep()));
										viols.addAll(repairFac.getViolations(false, VIOL_KIND.FULL, 
														repairFac.getRepairStep()));
										
										if(violationsNumLimit < (numViolations = viols.size())){
											Collections.shuffle(viols);
											viols = viols.subList(0, violationsNumLimit);
										}
			
										violsAxioms = new ArrayList<>(viols.size());
										
										for (Pair<OWLClass> v : 
											LogMapWrapper.getOWLClassFromIndexPair(viols, 
													repairFac.getAlignIndex())) {
											violsAxioms.add(OntoUtil.getSubClassOfAxiom(v));
										}
										
										viols.clear();
										
										repairFac.dispose();
									}
								}
								
								if(failed){
									timeSingleJustTuple = new HeterogeneousTuple(-1l,-1l,-1,-1,-1);;
								}
								else {
									timeSingleJustTuple = singleJustificationViolTask(
											violsAxioms, alignOnto, rk, 
											violationsSingleJustificationTimeout);
								}
								
								timeSingleJustViolAVG = (Long) timeSingleJustTuple.get(0);
								timeSingleJustViolTOT = (Long) timeSingleJustTuple.get(1);
								numSingleJustViolOk = (Integer) timeSingleJustTuple.get(2);
								numSingleJustViolTimeout = (Integer) timeSingleJustTuple.get(3);
								numSingleJustViolError = (Integer) timeSingleJustTuple.get(4);
								
								if(timeSingleJustViolAVG < 0 || timeSingleJustViolTOT < 0){
									FileUtil.writeErrorLogAndConsole(rk + " failed");
								}
								FileUtil.writeLogAndConsole(rk + 
										" single justifications violations time (AVG,TOT): " 
										+ timeSingleJustViolAVG + ", " + timeSingleJustViolTOT);
	
								FileUtil.writeLogAndConsole(
										rk + " single justification violations (ok,timeout,error): " + 
												numSingleJustViolOk + ", " + 
												numSingleJustViolTimeout + ", " + 
												numSingleJustViolError);
								
								if(failed){
									timeAllJustTuple = new HeterogeneousTuple(-1l,-1l,-1,-1,-1);;
								}
								else {
									timeAllJustTuple = allJustificationsViolTask(violsAxioms, 
										alignOnto, rk, violationsJustNumLimit, 
										violationsAllJustificationsTimeoutSingleJust, explsViols);
								}
								timeAllJustViolAVG = (Long) timeAllJustTuple.get(0);
								timeAllJustViolTOT = (Long) timeAllJustTuple.get(1);
								numAllJustViolOk = (Integer) timeAllJustTuple.get(2);
								numAllJustViolTimeout = (Integer) timeAllJustTuple.get(3);
								numAllJustViolError = (Integer) timeAllJustTuple.get(4);
								
								if(timeAllJustViolAVG < 0 || timeAllJustViolTOT < 0){
									FileUtil.writeErrorLogAndConsole(rk + " failed");
								}
								FileUtil.writeLogAndConsole(rk + 
										" all justifications violations time (AVG,TOT): " 
										+ timeAllJustViolAVG + ", " + timeAllJustViolTOT);
							
								FileUtil.writeLogAndConsole(
										rk + " all " + explsViols.size() + 
										" justifications violations (ok,timeout,error): " + 
												numAllJustViolOk + ", " + 
												numAllJustViolTimeout + ", " + 
												numAllJustViolError);
							}
						}
						
						/****** COLUMN 0 = alignment (matcher + ontologies) *********/
						String mappingName = 
								alignPath.substring(
										alignPath.lastIndexOf("/")+1, 
										alignPath.length()-4);
						FileUtil.writeDataOutFile(mappingName + " ");
			
						/****** COLUMN 1 = align year *********/
						FileUtil.writeDataOutFile(year + " ");
	
						/****** COLUMN 2 = matcher align *********/
						FileUtil.writeDataOutFile(matcher + " ");
	
						/****** COLUMN 3 = align size *********/
						FileUtil.writeDataOutFile(LogMapWrapper.countMappings(align) + " ");
						
						/****** COLUMN 4 = num unsats *********/
						FileUtil.writeDataOutFile(numUnsatClasses + " ");					
						
						/****** COLUMN 5 = track *********/
						FileUtil.writeDataOutFile(track.getCompactName() + " ");
						
						/****** COLUMN 6 = reasoner *********/
						FileUtil.writeDataOutFile(rk.name() + " ");
			
						// -1 => timeout!
						/****** COLUMN 7 = classificationTime *********/
						FileUtil.writeDataOutFile(classTime + " ");
			
						/****** COLUMN 8 = singleJustTimeAVG *********/
						FileUtil.writeDataOutFile(timeSingleJustAVG + " ");
			
						/****** COLUMN 9 = singleJustTimeTOT *********/
						FileUtil.writeDataOutFile(timeSingleJustTOT + " ");
	
						/****** COLUMN 10 = numSingleJustOk *********/
						FileUtil.writeDataOutFile(numSingleJustOk + " ");
						
						/****** COLUMN 11 = numSingleJustTimeout *********/
						FileUtil.writeDataOutFile(numSingleJustTimeout + " ");
						
						/****** COLUMN 12 = numSingleJustError *********/
						FileUtil.writeDataOutFile(numSingleJustError + " ");
											
						/****** COLUMN 13 = allJustTimeAVG *********/
						FileUtil.writeDataOutFile(timeAllJustAVG + " ");
						
						/****** COLUMN 14 = allJustTimeTOT *********/
						FileUtil.writeDataOutFile(timeAllJustTOT + " ");
	
						/****** COLUMN 15 = numAllJustOk *********/
						FileUtil.writeDataOutFile(numAllJustOk + " ");
	
						/****** COLUMN 16 = numAllJustTimeout *********/
						FileUtil.writeDataOutFile(numAllJustTimeout + " ");
	
						/****** COLUMN 17 = numAllJustError *********/
						FileUtil.writeDataOutFile(numAllJustError + " ");
						
						if(!conservativityAlso){
							/****** COLUMN 18 = allJustNum *********/
							FileUtil.writeDataOutFileNL(expls.size() + "");
						}
						else {
							/****** COLUMN 18 = allJustNum *********/
							FileUtil.writeDataOutFile(expls.size() + " ");
							
							/****** COLUMN 19 = singleJustViolTimeAVG *********/
							FileUtil.writeDataOutFile(timeSingleJustViolAVG + " ");
				
							/****** COLUMN 20 = singleJustViolTimeTOT *********/
							FileUtil.writeDataOutFile(timeSingleJustViolTOT + " ");
	
							/****** COLUMN 21 = numSingleJustViolOk *********/
							FileUtil.writeDataOutFile(numSingleJustViolOk + " ");
							
							/****** COLUMN 22 = numSingleJustViolTimeout *********/
							FileUtil.writeDataOutFile(numSingleJustViolTimeout + " ");
							
							/****** COLUMN 23 = numSingleJustViolError *********/
							FileUtil.writeDataOutFile(numSingleJustViolError + " ");
												
							/****** COLUMN 24 = allJustViolTimeAVG *********/
							FileUtil.writeDataOutFile(timeAllJustViolAVG + " ");
							
							/****** COLUMN 25 = allJustViolTimeTOT *********/
							FileUtil.writeDataOutFile(timeAllJustViolTOT + " ");
	
							/****** COLUMN 26 = numAllJustViolOk *********/
							FileUtil.writeDataOutFile(numAllJustViolOk + " ");
	
							/****** COLUMN 27 = numAllJustViolTimeout *********/
							FileUtil.writeDataOutFile(numAllJustViolTimeout + " ");
	
							/****** COLUMN 28 = numAllJustViolError *********/
							FileUtil.writeDataOutFile(numAllJustViolError + " ");
							
							/****** COLUMN 29 = allJustViolNum *********/
							FileUtil.writeDataOutFile(explsViols.size() + " ");
							
							/****** COLUMN 30 = numViolations *********/
							FileUtil.writeDataOutFileNL(numViolations + "");
						}
					}catch(Throwable e){
						Util.logAndPrintThrowable(e);
					}
					finally {
						OntoUtil.checkActiveReasoners(true);
	//					OntoUtil.disposeAllReasoners();
						Util.getUsedMemoryAndClean(5 * 1024 * 1024 * 1024, 2000);
						FileUtil.writeLogAndConsole("Number of active threads: " + java.lang.Thread.activeCount());
						rowNum++;
					}
				}
				
				OntoUtil.checkActiveReasoners(true);				
				OntoUtil.unloadAllOntologies();
			}		
		}

		FileUtil.writeLogAndConsole("End: " 
				+ new SimpleDateFormat("HH:mm:ss").format(
						Calendar.getInstance().getTime()));
		FileUtil.closeFiles();
		FileUtil.flushLogFile();
	}
	
	private static HeterogeneousTuple allJustificationsViolTask(
			List<OWLSubClassOfAxiom> violsAxioms, OWLOntology alignOnto,
			REASONER_KIND rk, int limit, int timeout, 
			Set<Explanation<OWLAxiom>> expls) {
		FileUtil.writeLogAndConsole("All justifications violations computation");		
		long start = Util.getMSec();
		OWLReasonerFactory reasonerFactory = OntoUtil.getReasonerFactory(rk);

		HeterogeneousTuple dummyRes = new HeterogeneousTuple(-1l,-1l,-1,-1,-1);
		
		if(!supportsJustifications(reasonerFactory))
			return dummyRes;

		int okJust = 0, numTimeouts = 0, numErrors = 0;

		for (OWLAxiom v : violsAxioms) {
			long locStart = Util.getMSec();
			FileUtil.writeLogAndConsole("Violation " + v + ": " + Util.getCurrTime());
			
			try {
				Set<Explanation<OWLAxiom>> tmpExpls = null;
				
				if(rk.equals(REASONER_KIND.ELKTRACE))
					tmpExpls = OntoUtil.getTracingForAxiom(reasoner, 
							v, limit, timeout, printEach);
				else
					tmpExpls = OntoUtil.getExplanationForAxiom(v, alignOnto, 
						limit, reasonerFactory, timeout, Params.laconicJust, 
						printEach);

				if(tmpExpls == null)
					return dummyRes;
					
				if(!isExplanationsSetValid(tmpExpls))
					throw new IllegalStateException("Explanation cannot be empty: " + v);
				
				expls.addAll(tmpExpls);
				
				FileUtil.writeLogAndConsole(expls.size() + " justification(s) in " + 
						Util.getDiffmsec(locStart) + " ms");
				okJust++;
			}
			catch(TimeOutException | TimeoutException e){
				FileUtil.writeErrorLogAndConsole("Timeout reached, giving up");
				numTimeouts++;
			}
			catch(Throwable e){
				logAndPrintThrowable(e);
				numErrors++;
			}
		}
		
		long tot = Util.getDiffmsec(start);
		long avg = violsAxioms.size() == 0 ? tot : tot / violsAxioms.size();
		
		return new HeterogeneousTuple(avg,tot,okJust,numTimeouts,numErrors);
	}
	
	private static void logAndPrintThrowable(Throwable e){
		String msg = "Reasoner failed: " + e.getClass() + " ";
		boolean details = !msg.contains("org.semanticweb");
		FileUtil.writeErrorLogAndConsole(msg + ( 
					e.getMessage() != null ? e.getMessage() : "" ) + 
					(details ? "\n" + Arrays.toString(e.getStackTrace()).
								replace("[", "").
								replace("]", "").
								replace(", ", ",\n") 
								: ""));
	}

	private static HeterogeneousTuple singleJustificationViolTask(
			List<OWLSubClassOfAxiom> violsAxioms, OWLOntology alignOnto,
			REASONER_KIND rk, int timeout) {
		FileUtil.writeLogAndConsole("Single justification violations computation");		
		long start = Util.getMSec();
		OWLReasonerFactory reasonerFactory = OntoUtil.getReasonerFactory(rk);
		
		HeterogeneousTuple dummyRes = new HeterogeneousTuple(-1l,-1l,-1,-1,-1);
		
		if(!supportsJustifications(reasonerFactory))
			return dummyRes;

		int okJust = 0, numTimeouts = 0, numErrors = 0;
			
		for (OWLSubClassOfAxiom v : violsAxioms) {
			long locStart = Util.getMSec();
			FileUtil.writeLogAndConsole("Violation " + v + ": " + Util.getCurrTime());
			try {
				Set<Explanation<OWLAxiom>> tmpExpls = null;

				if(rk.equals(REASONER_KIND.ELKTRACE))
					tmpExpls = OntoUtil.getTracingForAxiom(reasoner, v, 1, 
							timeout, printEach);
				else
					tmpExpls = OntoUtil.getExplanationForAxiom(v, alignOnto, 1, 
							reasonerFactory, timeout, Params.laconicJust, 
							printEach);

				if(tmpExpls == null)
					return dummyRes;
				
				if(!isExplanationsSetValid(tmpExpls))
					throw new IllegalStateException("Explanation cannot be empty: " + v);

				FileUtil.writeLogAndConsole("Done in " + Util.getDiffmsec(locStart) + " ms");
				okJust++;
			}
			catch(TimeOutException | TimeoutException e){
				FileUtil.writeErrorLogAndConsole("Timeout reached, giving up");
				numTimeouts++;
			}
			catch(Throwable e){
				logAndPrintThrowable(e);
				numErrors++;			
			}
		}

		long tot = Util.getDiffmsec(start);
		long avg = violsAxioms.size() == 0 ? tot : tot / violsAxioms.size();
		
		return new HeterogeneousTuple(avg,tot,okJust,numTimeouts,numErrors);
	}
	
	private static HeterogeneousTuple singleJustificationTask(List<OWLClass> unsatClasses, 
			OWLOntology alignOnto, REASONER_KIND rk, int timeout){
		FileUtil.writeLogAndConsole("Single justification computation");		
		long start = Util.getMSec();
		OWLReasonerFactory reasonerFactory = OntoUtil.getReasonerFactory(rk);
		
		HeterogeneousTuple dummyRes = new HeterogeneousTuple(-1l,-1l,-1,-1,-1);
		
		if(!supportsJustifications(reasonerFactory))
			return dummyRes;

		int okJust = 0, numTimeouts = 0, numErrors = 0;
			
		for (OWLClass c : unsatClasses) {
			long locStart = Util.getMSec();
			FileUtil.writeLogAndConsole("Unsat class " + c + ": " + Util.getCurrTime());
			
			Set<Explanation<OWLAxiom>> tmpExpls = null;
			
			try {	
				if(rk.equals(REASONER_KIND.ELKTRACE)){
					tmpExpls = OntoUtil.getTracingForAxiom(reasoner, 
							new OWLSubClassOfAxiomImpl(c, 
									OntoUtil.getDataFactory().getOWLNothing(), 
									Collections.EMPTY_SET), 1, timeout, printEach);
				}
				else 
					tmpExpls = OntoUtil.getExplanationForUnsat(c, alignOnto, 1, 
							reasonerFactory, timeout, printEach);
				
				if(tmpExpls == null)
					return dummyRes;
				
				if(!isExplanationsSetValid(tmpExpls))
					throw new IllegalStateException("Explanation cannot be empty: " + c);
				
				FileUtil.writeLogAndConsole("Done in " + Util.getDiffmsec(locStart) + " ms");
				okJust++;
			}
			catch(TimeOutException | TimeoutException e){
				FileUtil.writeErrorLogAndConsole("Timeout reached, giving up");
				numTimeouts++;
			}
			catch(Throwable e){
				logAndPrintThrowable(e);
				numErrors++;
			}
		}

		long tot = Util.getDiffmsec(start);
		long avg = unsatClasses.size() == 0 ? tot : tot / unsatClasses.size();
		
		return new HeterogeneousTuple(avg,tot,okJust,numTimeouts,numErrors);
	}
	
	private static boolean isExplanationsSetValid(Set<Explanation<OWLAxiom>> tmpExpls){		
		if(tmpExpls.isEmpty())
			return false;
		
		Explanation<OWLAxiom> expl = tmpExpls.iterator().next();
		
		return !(expl == null || expl.isEmpty());
	}
	
	private static HeterogeneousTuple allJustificationsTask(
			List<OWLClass> unsatClasses, OWLOntology alignOnto, 
			REASONER_KIND rk, int limit, int timeout, 
			Set<Explanation<OWLAxiom>> expls){
		FileUtil.writeLogAndConsole("All justifications computation");		
		long start = Util.getMSec();
		OWLReasonerFactory reasonerFactory = OntoUtil.getReasonerFactory(rk);

		HeterogeneousTuple dummyRes = new HeterogeneousTuple(-1l,-1l,-1,-1,-1);

		if(!supportsJustifications(reasonerFactory))
			return dummyRes;

		int okJust = 0, numTimeouts = 0, numErrors = 0;

		for (OWLClass c : unsatClasses) {
			long locStart = Util.getMSec();
			FileUtil.writeLogAndConsole("Unsat class " + c + ": " + Util.getCurrTime());
			
			try {
				Set<Explanation<OWLAxiom>> tmpExpls = null;
				if(rk.equals(REASONER_KIND.ELKTRACE)){
					tmpExpls = OntoUtil.getTracingForAxiom(reasoner, 
							new OWLSubClassOfAxiomImpl(c, 
									OntoUtil.getDataFactory().getOWLNothing(), 
									Collections.EMPTY_SET), limit, timeout, 
									printEach);					
				}
				else
					tmpExpls = OntoUtil.getExplanationForUnsat(c, alignOnto, 
						limit, reasonerFactory, timeout, printEach);
				
				if(tmpExpls == null)
					return dummyRes;
				
				if(!isExplanationsSetValid(tmpExpls))
					throw new IllegalStateException("Explanation cannot be empty: " + c);
				
				expls.addAll(tmpExpls);
				
				FileUtil.writeLogAndConsole(expls.size() + 
						" justification(s) in " + 
						Util.getDiffmsec(locStart) + " ms");
				okJust++;
			}
			catch(TimeOutException | TimeoutException e){
				FileUtil.writeErrorLogAndConsole("Timeout reached, giving up");
				numTimeouts++;
			}
			catch(Throwable e){
				logAndPrintThrowable(e);
				numErrors++;
			}
		}
		
		long tot = Util.getDiffmsec(start);
		long avg = unsatClasses.size() == 0 ? tot : tot / unsatClasses.size();
		
		return new HeterogeneousTuple(avg,tot,okJust,numTimeouts,numErrors);
	}	

	private static Set<OWLClass> getUnsatClasses(){
		Set<OWLClass> unsatClasses = 
				reasoner.getUnsatisfiableClasses().getEntitiesMinusBottom();
		int unsats = unsatClasses.size();
		
		FileUtil.writeLogAndConsole("Unsatisfiable classes: " + unsats);
		
		return unsatClasses;
	}

	private static void loadOntologies(String fstOnto, String sndOnto, 
			OWLOntologyManager manager, boolean local){
		try {
			fstO = OntoUtil.load(fstOnto, local, manager);
			// this should be fine because that exception is only thrown for identical ontologies
			try {
				sndO = OntoUtil.load(sndOnto, local, manager);
			}
			catch(OWLOntologyAlreadyExistsException e){
				sndO = fstO;
			}
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
	}

	private static boolean supportsJustifications(OWLReasonerFactory reasonerFactory){
		return !(reasonerFactory instanceof OWLlinkHTTPXMLReasonerFactory);
	}
	
	private static int setupReasoner(OWLOntologyManager manager, 
			OWLOntology alignOnto, REASONER_KIND rk){	
		try {
			reasoner = OntoUtil.getReasoner(alignOnto, rk, manager);
		}
		catch(OWLlinkReasonerRuntimeException e){
			FileUtil.writeErrorLogAndConsole("OWLink parsing error on " +
					"aligned ontology");
			return -2;
		}
		catch(Exception | Error e){
			FileUtil.writeErrorLogAndConsole("Reasoner failed while loading " +
					"the ontology: " + e.getMessage());
			return -3;
		}
		
		return 0;
	}
	
	private static long classificationTask(){
		long start = Util.getMSec();

		if(reasoner == null){
			String errStr = "Cannot classify due to null reasoner";
			FileUtil.writeErrorLog(errStr);
			throw new IllegalStateException(errStr);
		}
		
		List<OWLReasoner> reasoners = new LinkedList<>();
		reasoners.add(reasoner);

		long alignClassifTime = -1;
		try {
			alignClassifTime = OntoUtil.ontologyClassification(
				true, false, reasoners, false, false);
		}
		catch(Exception | Error e){
			FileUtil.writeErrorLogAndConsole("Classification failed: " + e.getMessage());
			return -4;
		}
		
		reasoner = reasoners.get(0);

		if(alignClassifTime != -1){
		
			if(!reasoner.getRootOntology().equals(alignOnto)){
				FileUtil.writeErrorLogAndConsole("Aligned ontology for reasoner mismatch");
				return -5;
			}
			
			if(!OntoUtil.checkClassification(reasoners)){ 
				FileUtil.writeErrorLogAndConsole("Classification failed!");
				OntoUtil.disposeAllReasoners();
				return -6;
			}
		
//		OntoUtil.printClassification(reasoner.getTopClassNode(), reasoner, 0);
		
		}
		return alignClassifTime;
	}
}
