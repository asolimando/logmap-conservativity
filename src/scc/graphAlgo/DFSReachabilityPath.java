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

import scc.graphDataStructure.LightAdjacencyList;
import scc.graphDataStructure.LightEdge;
import scc.graphDataStructure.LightNode;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import org.semanticweb.owlapi.model.OWLClass;

public class DFSReachabilityPath {

	boolean allowsEdge;
	boolean srcFstOnto;
	LightAdjacencyList adj;
	Stack<LightNode> s = new Stack<>();
	Set<LightEdge> discovery = new HashSet<>();
	Set<LightEdge> back = new HashSet<>();
	Set<LightNode> visited = new HashSet<>();
	LightNode src, dst;
	
	public DFSReachabilityPath(LightAdjacencyList adj, boolean allowsEdge, 
			OWLClass src, OWLClass dst){
		this.allowsEdge = allowsEdge;
		this.adj = adj;
		this.src = adj.getNodeFromClass(src);
		this.dst = adj.getNodeFromClass(dst);
	}

	public void clear(){
		s.clear();
		discovery.clear();
		back.clear();
		visited.clear();
	}

	private boolean isUnexplored(LightEdge e){
		return !(discovery.contains(e) || back.contains(e));
	}

	private void setDiscovery(LightEdge e){
		discovery.add(e);
	}

	private void setBack(LightEdge e){
		back.add(e);
	}

	//	Algorithm pathDFS(G, v, z)
	//		setLabel(v, VISITED)
	//		S.push(v)
	//		if v = z
	//			return S.elements()
	//		for all e ∈ G.incidentEdges(v)
	//			if getLabel(e) = UNEXPLORED
	//				w ← opposite(v, e)
	//				if getLabel(w) = UNEXPLORED
	//					setLabel(e, DISCOVERY)
	//					S.push(e)
	//					pathDFS(G, w, z)
	//					S.pop() { e gets popped }
	//				else
	//					setLabel(e, BACK)
	//		S.pop() { v gets popped }
	
	public Stack<LightNode> getPath() {
		s.push(src);
		return getPath(src);
	}
	
	public Stack<LightNode> getPath(LightNode n) {					
		visited.add(n);

		if(n.equals(dst))
			return s;

//		s.push(n);

		for (LightEdge e : adj.getAdjacent(n)) {
			if(!allowsEdge && (e.to.getFirstOnto() == src.getFirstOnto()))
				continue;

			if(isUnexplored(e)){
				if(!visited.contains(e.to)){
					setDiscovery(e);
					s.push(e.to);
					if(getPath(e.to) != null)
						return s;
					s.pop();
				}
			}
			else
				setBack(e);
		}
//		s.pop();

		return null;
	}
}
