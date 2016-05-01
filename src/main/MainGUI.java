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
package main;

import java.io.IOException;
import java.util.Arrays;

import scc.gui.VisualDebugger;

import gui.VisualConservativity;

public class MainGUI {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String [] dummyArgs = new String[0];
		if(args.length == 0){
			VisualConservativity.main(dummyArgs);
		}
		else {
			int kind = Integer.parseInt(args[0]);
			switch(kind){
				case 0: 
					VisualConservativity.main(dummyArgs);
					break;
				case 1: 
					VisualDebugger.main(dummyArgs);
					break;
				default: 
					VisualConservativity.main(dummyArgs);
			}
		}
	}
}
