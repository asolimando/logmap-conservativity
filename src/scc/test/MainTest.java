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
import java.util.Arrays;

import org.semanticweb.owlapi.model.OWLOntologyCreationException;

public class MainTest {

	public static void main(String args[]) 
			throws IOException, OWLOntologyCreationException{
		
		if(args.length < 1)
			throw new Error("Incorrect number of parameters");
		
		int testNum = Integer.parseInt(args[0]), from = Math.min(args.length, 1), 
				to = args.length;
		
		switch (testNum) {
		case 0:
			ExternalMain.main(Arrays.copyOfRange(args, from, to));
			break;
		case 1:
			Test1.main(Arrays.copyOfRange(args, from, to));
			break;
		case 2:
			Test2.main(Arrays.copyOfRange(args, from, to));
			break;
		case 3:
			Test3.main(Arrays.copyOfRange(args, from, to)); 
			break;
		case 4:
			Test4.main(Arrays.copyOfRange(args, from, to));
			break;
		default:
			throw new Error("Unknown test " + testNum);
		}
	}
}