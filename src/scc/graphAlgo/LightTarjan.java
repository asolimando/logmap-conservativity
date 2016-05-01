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
import scc.graphDataStructure.LightSCC;
import scc.graphDataStructure.LightSCCs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LightTarjan implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3317618668787472814L;
	private int index = 0;
	private ArrayList<LightNode> stack = new ArrayList<LightNode>();
	private LightSCCs SCCs = new LightSCCs();

//	private static void buildEdge(Map<LightNode, List<LightEdge>> adjList, LightAdjacencyList list, 
//			LightNode left, LightNode right, String edgeLabel, boolean mapping){
//
//		adjList.get(left).add(new LightEdge(left, right));
//	}

	public LightSCCs executeLocalTarjan(LightAdjacencyList graph, LightSCCs 
			localSCC, LightNode deletedVertex){
		LightSCC invalidSCC = null;

		for (LightSCC scc : localSCC){
			if(scc.contains(deletedVertex)){
				invalidSCC = scc;
				break;
			}
		}
		
		//System.out.println("Adapted graph: " + graph);

		localSCC.remove(invalidSCC);
		invalidSCC.remove(deletedVertex);
		invalidSCC.clearTarjanIndexes();
		stack.clear();
		// index is the next available
		for (LightNode node : invalidSCC) {
			if(node.index == -1)
				localTarjan(node, graph, localSCC);
		}

		return localSCC;
	}

	private LightSCCs localTarjan(LightNode v, LightAdjacencyList list, 
			LightSCCs localSCCs){

		v.index = index;
		v.lowlink = index;
		++index;
		stack.add(0, v);
		for(LightEdge e : list.getAdjacent(v)){
			LightNode n = e.to;

			if(n.index == -1){
				localTarjan(n, list, localSCCs);
				v.lowlink = Math.min(v.lowlink, n.lowlink);
			}else if(stack.contains(n)){
				v.lowlink = Math.min(v.lowlink, n.index);
			}
		}
		if(v.lowlink == v.index){
			LightNode n;
			LightSCC component = new LightSCC();
			do{
				n = stack.remove(0);
				component.add(n);
			}while(n != v);
			localSCCs.add(component);
		}
		return localSCCs;
	}

	private LightSCCs tarjan(LightNode v, LightAdjacencyList list, 
			Set<LightEdge> m, LightSCC lightSCC, boolean inputOntologies){
		v.index = index;
		v.lowlink = index;
		++index;
		stack.add(0, v);
		
		Set<LightEdge> excludedMappings = new HashSet<>(m);
		//excludedMappings.addAll(list.removedMappings);
		
		for(LightEdge e : list.getAdjacent(v,excludedMappings)){
			LightNode n = e.to;	
			
			// if an SCC is given, we compute locally to it
			if(lightSCC != null && !lightSCC.contains(n))
				continue;
				
			if(n.index == -1){
				tarjan(n, list, excludedMappings, lightSCC, inputOntologies);
				v.lowlink = Math.min(v.lowlink, n.lowlink);
			}else if(stack.contains(n)){
				v.lowlink = Math.min(v.lowlink, n.index);
			}
		}
		if(v.lowlink == v.index){
			LightNode n;
			LightSCC component = new LightSCC();
			do{
				n = stack.remove(0);
				component.add(n);
				// we associate to the node its localSCC (in the input ontology)
				if(inputOntologies)
					n.setLocalSCC(component);
			}while(n != v);
			SCCs.add(component);
		}
		
		return SCCs;
	}
	
	public LightSCCs executeTarjan(LightAdjacencyList graph,
			Set<LightEdge> excludeMappings, LightSCC lightSCC, 
			boolean inputOntologies) {
		SCCs.clear();
		index = 0;
		stack.clear();
		if(graph != null){
			if(lightSCC != null){
				// we only compute in a single SCC
				lightSCC.clearTarjanIndexes();
				for (LightNode node : lightSCC)
				{
					if(node.index == -1)
					{
						tarjan(node, graph, excludeMappings, lightSCC,inputOntologies);
					}
				}
			}
			else {
				graph.clearTarjanIndexes();
				List<LightNode> nodeList = new ArrayList<>(graph.getSourceNodeSet());
				for (LightNode node : nodeList)
				{
					if(node.index == -1)
					{
						tarjan(node, graph, excludeMappings, lightSCC,inputOntologies);
					}
				}
			}
		}
		return SCCs;
	}
	
	public LightSCCs executeTarjan(LightAdjacencyList graph,
			Set<LightEdge> excludeMappings, boolean inputOntologies) {

		return executeTarjan(graph, excludeMappings, null, inputOntologies);
	}
	
	public LightSCCs executeTarjan(LightAdjacencyList graph, 
			boolean inputOntologies){
		return executeTarjan(graph, null, null, inputOntologies);
	}
}
