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
package scc.graphAlgo;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import scc.graphDataStructure.LightAdjacencyList;
import scc.graphDataStructure.LightEdge;
import scc.graphDataStructure.LightNode;

public class DFSReachability extends NodeReachability {

	private boolean allowsEdge;

	public DFSReachability(LightAdjacencyList adj, boolean allowsEdge){
		super(adj);		
		this.allowsEdge = allowsEdge;
	}

	@Override
	public boolean areReachable(LightNode src, LightNode dst) {
		Set<LightNode> visited = new HashSet<>();
		Stack<LightNode> toProcess = new Stack<>();
		
		toProcess.add(src);
		LightNode n = null;
		
		while(!toProcess.isEmpty()){
			n = toProcess.pop();
			
			if(visited.contains(n))
				continue;
			
//			if(n.equals(dst))
//				return true;
			
			if(visitAndTest(visited, toProcess, src, dst, n, allowsEdge))
				return true;
		}
		
		return false;
	}
}
