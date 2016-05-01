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

import java.io.IOException;

import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import util.Params;

public class Test4 {

	public static void main(String args[]) 
			throws IOException, OWLOntologyCreationException{
		
		if(args.length != 3)
			throw new Error("Invalid params for Test4: " + args.length);
		Params.test4Repetitions = Integer.parseInt(args[0]);
		Params.test2VMGB = Double.parseDouble(args[1]);
		Params.test4Resume = Integer.parseInt(args[2]) == 1;
		
		// default is "runAll", verbosity=0, noRefAnalysis off, filterAnalysis on
		if(args.length != 4){
			System.out.println("Using default values for test4");
			args = new String[]{"6","0","0","1"};
		}
		
		Test1.main(args);
	}
}