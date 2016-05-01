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
package scc.mapping;

import scc.graphDataStructure.LightAdjacencyList;
import scc.graphDataStructure.LightEdge;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class MappingDiffer {

	private static LightOAEIMappingHandler parser = new LightOAEIMappingHandler();
	
	public static Set<LightEdge> loadMappings(File mapping, LightAdjacencyList adjList) throws IOException{
		return parser.parse(mapping, adjList, true);
	}

}
