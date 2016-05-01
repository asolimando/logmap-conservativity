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

import java.util.Collection;

import org.semanticweb.owlapi.model.OWLClass;

import scc.graphDataStructure.LightAdjacencyList;
import scc.graphDataStructure.LightEdge;
import scc.graphDataStructure.LightNode;

public abstract class NodeReachability {
	
	protected LightAdjacencyList adj;
	
	public NodeReachability(LightAdjacencyList adj){
		this.adj = adj;
	}
	
	public abstract boolean areReachable(LightNode src, LightNode dst);	
	
	public boolean areReachable(OWLClass src, OWLClass dst){
		
		LightNode srcN = adj.getNodeFromClass(src), 
				dstN = adj.getNodeFromClass(dst);
		
		return areReachable(srcN, dstN);
	}
	
	public boolean visitAndTest(Collection<LightNode> visited, 
			Collection<LightNode> toProcess, LightNode src, LightNode dst, 
			LightNode n, boolean allowsEdge){
		visited.add(n);
		
		for (LightEdge e : adj.getAdjacent(n)) {
			if(e.to.equals(dst))
				return true;
			
			if(!allowsEdge && (e.to.getFirstOnto() == src.getFirstOnto()))
				continue;
			
			toProcess.add(e.to);
		}
		
		return false;
	}
}
