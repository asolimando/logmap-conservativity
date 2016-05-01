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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import auxStructures.Pair;

import enumerations.VIOL_KIND;

import repair.ConservativityRepairFacility;

import util.FileUtil;
import util.Params;
import util.Util;

public class SelectiveRepairTest extends AbstractMatcherTest2014 {

	private int percentage = 100;
	
	public SelectiveRepairTest(String [] args, int testNumber){
		super(args,testNumber);
	}
	
	public static void main(String [] args) 
			throws OWLOntologyCreationException, IOException {
		
		Params.storeViolations = true;
		args[3] = "direct";

		SelectiveRepairTest selTest = 
				new SelectiveRepairTest(args.length > 5 ? 
						Arrays.copyOfRange(args, 0, args.length-1) : args, 13);

		if(args.length > 5)
			selTest.percentage = Integer.parseInt(args[5]);		
			
		selTest.trackTest();
	}
	
	public void realTest(String mappingFile, 
			OWLOntology fstO, OWLOntology sndO, boolean unloadOnto, 
			boolean rootViolations, boolean fullDisj, boolean useModules, 
			String trackName){
		
		FileUtil.writeLogAndConsole(percentage + " % of violations to be solved");
		
		ConservativityRepairFacility repair = 
				new ConservativityRepairFacility(fstO, sndO, manager, 
						mappingFile, rootViolations);
		
//		repair.detectViolations(true, rootViolations);
		
		List<Pair<Integer>> violations1 = repair.getViolations(true, 
				VIOL_KIND.APPROX, repair.getRepairStep(), rootViolations);
		List<Pair<Integer>> violations2 = repair.getViolations(false, 
				VIOL_KIND.APPROX, repair.getRepairStep(), rootViolations);
		
		List<Pair<Integer>> viols = new ArrayList<>();
//		viols.addAll(Util.selectPercentage(violations1,percentage));
//		viols.addAll(Util.selectPercentage(violations2,percentage));
		
		viols.addAll(Util.randomSample(violations1,percentage));
		viols.addAll(Util.randomSample(violations2,percentage));
		
		repair.repair(viols);
		
		double [] stats = repair.getSelectiveRepairStats(viols);
		
		FileUtil.writeLogAndConsole(
				"Precision: " + stats[0] + 
				"\nRecall: " + stats[1] + 
				"\nF-Measure: " + stats[2]);
	}
}
