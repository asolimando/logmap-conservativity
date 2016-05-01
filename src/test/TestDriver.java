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
import java.util.Arrays;

import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import enumerations.DISJ_CHECK;
import enumerations.REPAIR_STRATEGY;

import util.Params;

public class TestDriver {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws OWLOntologyCreationException 
	 */
	public static void main(String[] args) 
			throws OWLOntologyCreationException, IOException {

		int testFamily;
//		int testKind, repetitionsNum;
//		boolean fullDisj;

		if(args.length < 5)
			throw new IllegalArgumentException(
					"At least five arguments are needed");
		
		// disable extra checks, too expensive for long test batch
		Params.reasonerViolationsCheck = false;
		Params.reasonerValidateIndex = false;
		Params.indexSanityCheck = false;
		
		Params.disjCheckStrategy = DISJ_CHECK.SEMINDEX; 
		Params.repairStrategy = REPAIR_STRATEGY.CONSIST_THEN_CONSERV;
		
		Params.saveOnto = false;
		Params.saveMappings = true;
		
		testFamily = Integer.parseInt(args[args.length-1]);

		String [] newargs = Arrays.copyOfRange(args, 0, args.length-1);

		switch(testFamily) {
		case 6:
			Test6.main(newargs);
			break;
		case 7: 
			Test7.main(newargs);
			break;
		case 8:
			CombinedMatcher12Test.main(newargs);
			break;
		case 9:
			CombinedGoldTest13.main(newargs);
			break;
		case 10:
			CombinedMatcher13Test.main(newargs);
			break;
		case 11:
			CombinedGoldTest14.main(newargs);
			break;
		case 12:
			CombinedMatcher14Test.main(newargs);
			break;
		case 13:
			SelectiveRepairTest.main(newargs);
			break;			
		default:
			throw new Error("Invalid test family number: " + testFamily);
		}
	}

}
