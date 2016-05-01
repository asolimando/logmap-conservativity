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
import scc.graphDataStructure.LightCycle;
import scc.graphDataStructure.LightCycles;
import scc.graphDataStructure.LightEdge;
import scc.graphDataStructure.LightNode;
import scc.graphDataStructure.LightSCC;
import scc.graphDataStructure.LightSCCs;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import util.FileUtil;
import util.Params;

public class Johnson {

	private LightCycles cycles;
	private Set<LightNode> blocked;
	private Map<LightNode,Set<LightNode>> b;
	private LinkedList<LightNode> stack;
	
	// the order between nodes is imposed by their position in the list
	private List<LightNode> nodes = new LinkedList<>();
	private int nodeIdx;
	private int circuitCall;
	
	private void reset(LightSCC scc){
		if(cycles != null)
			cycles.clear();
		else
			cycles = new LightCycles();

		if(blocked != null)
			blocked.clear();
		else
			blocked = new HashSet<>();
		if(stack != null)
			stack.clear();
		else
			stack = new LinkedList<LightNode>();
		
		nodes.clear();
		nodes.addAll(scc);
		nodeIdx = 0;
		if(b != null)
			b.clear();
		else
			b = new HashMap<LightNode, Set<LightNode>>();
	}
	
    private LightSCC leastSCC(LightAdjacencyList adj, LightSCC scc){
	    LightTarjan t = new LightTarjan();
		List<LightSCC> sccs = t.executeTarjan(adj, 
				Collections.<LightEdge> emptySet(), scc, false);
		
	    int minIdx = nodes.size();
	    LightSCC minSCC = null;
	    
	    for (LightSCC lightSCC : sccs) {
			for (LightNode n : lightSCC) {
				if(nodes.indexOf(n) < minIdx){
					minSCC = lightSCC;
					minIdx = nodes.indexOf(n);
				}
			}
		}
	    return minSCC;
	}
    
    private LightNode leastNode(LightSCC leastSCC){
    	LightNode leastNode = null;
    	int minIdx = nodes.size();
    	
    	for (LightNode n : leastSCC) {
			if(nodes.indexOf(n) < minIdx){
				leastNode = n;
				minIdx = nodes.indexOf(n);
			}
		}
    	
    	return leastNode;
    }
    
    private LightSCC computeInducedSubGraph(LightAdjacencyList adj, LightNode s, List<LightNode> list){
    	LightSCC scc = new LightSCC();
    	
    	for (LightNode m : list) {
			for (LightNode n : list) {
				if(n.equals(m))
					continue;
				//adj.reverseAdjList.get(n).contains(m)
				if(adj.isFirstNodeAdjacentToSecondNode(n,m)){
					scc.add(m);
					break;
				}
			}
		}
    	return scc;
    }
	
	public LightCycles findElementaryCycles(LightAdjacencyList adj, LightSCC scc, LightSCCs localSCCs){		
		reset(scc);
		
		for (LightNode n : scc)
			b.put(n, new HashSet<LightNode>());
		
		while(nodeIdx < nodes.size()){
			//if(nodeIdx % 1 == 0)
				//System.out.println("Node index " + nodeIdx + "/" + nodes.size());
			LightSCC reducedSCC = computeInducedSubGraph(adj, 
					nodes.get(nodeIdx),nodes.subList(nodeIdx,nodes.size()));
			//new LightSCC();
			//reducedSCC.addAll(nodes.subList(nodeIdx,nodes.size()));
			LightSCC leastSCC = leastSCC(adj, reducedSCC);

			if(leastSCC != null && !leastSCC.isEmpty()){
				LightNode leastNode = leastNode(leastSCC);
				for (LightNode i : leastSCC) {
					blocked.remove(i);
					b.get(i).clear();
				}
				circuit(adj,leastSCC,leastNode,cycles,leastNode);
				nodeIdx++;
			}
			else
				break;
		}
		
		if(Params.removeNontrivialSafeCycles){
			int remNum = cycles.removeNontrivialSafeCycles();
			if(Params.verbosity >= 0 && remNum > 0)
				FileUtil.writeLogAndConsole("Removed " + remNum + " nontrivial safe cycles");
		}
		
		return cycles;
	}
	
	private boolean circuit(LightAdjacencyList adj, LightSCC scc, 
			LightNode v, LightCycles cycles, LightNode s){
		boolean f = false;
		
		if(++circuitCall % 500000 == 0)
			FileUtil.writeLogAndConsole(circuitCall + "");
		
		if(circuitCall % 2000000 == 0){
			FileUtil.writeLogAndConsole("Blocked " + blocked.size() + "\nCycles " + cycles.size());
		}
		//FileUtil.writeLogAndConsole(indexes[(nodes.indexOf(v)+1)] + " " + indexes[(nodes.indexOf(s)+1)]);
		
		stack.push(v);
		blocked.add(v);
		
		//FileUtil.writeLogAndConsole(stack.size() + " " + v);
		
		for (LightNode w : adj.getAdjacentNodes(v, scc, true, true, true)) {
			if(w.equals(s)){
				LightCycle c = cycleFromStack(adj,stack,v,s);
				if(c.containsMappings())
					cycles.add(c);
				//FileUtil.writeLogAndConsole("#="+cycles.size());
				//FileUtil.writeLogAndConsole(""+c);
				f = true;
			}
			else if(!blocked.contains(w)){
				if(circuit(adj,scc,w,cycles,s))
					f = true;
			}
		}
		
		if(f)
			unblock(v);
		else {
			for (LightNode w : adj.getAdjacentNodes(v, scc, true, true, true)) {
				if(!b.get(w).contains(v))
					b.get(w).add(v);
			}
		}
		stack.pop();
		
		return f;
	}
	
	private LightCycle cycleFromStack(LightAdjacencyList adj, 
			LinkedList<LightNode> stack, LightNode v, LightNode s){
		Iterator<LightNode> itr = stack.iterator();
		LightNode act = itr.next(), next;

		LightCycle cycle = new LightCycle();
		while(itr.hasNext()){
			next = itr.next();
			cycle.addFirst(adj.getEdgeBetweenNodes(next, act));
			act = next;
		}
		cycle.addLast(adj.getEdgeBetweenNodes(v,s));
		
		return cycle;
	}
	
	private void unblock(LightNode u){
		blocked.remove(u);
		Iterator<LightNode> itr = b.get(u).iterator();
		LightNode w = null;
		
		while(itr.hasNext()){
			w = itr.next();
			//FileUtil.writeLogAndConsole("W = " + indexes[(nodes.indexOf(w)+1)]);
			itr.remove();
			if(blocked.contains(w))
				unblock(w);
		}
	}
}
