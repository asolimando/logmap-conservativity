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
package scc.graphDataStructure;

import scc.graphAlgo.LightTarjan;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClassExpression;

import util.FileUtil;
import util.OntoUtil;
import util.Params;

public class LightSCC extends HashSet<LightNode> implements Serializable {

	private static final long serialVersionUID = -8795710312493993054L;
	//public LightNode [] parents;
	private Set<LightEdge> mappings = null; 
	private boolean cachedHash = false;
	private int hash;
	
	public LightSCC(){
		super();
	}
	
	public Set<LightEdge> selectSCCMappings(Diagnosis d){
		Set<LightEdge> r = new HashSet<>(mappings);
		r.retainAll(d);
		return r;
	}
	
	public boolean isFullyProblematic(LightAdjacencyList adj){
		return isProblematic(adj) || isLogicallyProblematic(adj);
	}
	
	public boolean isLogicallyProblematic(LightAdjacencyList adj){
		LightSCC fstSCC = extract(true), sndSCC = extract(false);
		LightSCCs localSCCs = adj.getLocalSCCs();
		if(size() == 1)
			return false;
		if(!fstSCC.isEmpty() && sndSCC.isEmpty() && !localSCCs.contains(fstSCC))
			return true;
		if(!sndSCC.isEmpty() && fstSCC.isEmpty() && !localSCCs.contains(sndSCC))
			return true;
		if(!sndSCC.isEmpty() && !fstSCC.isEmpty())
			return false;
		//&& extractMappings(adj, false).isEmpty()
		return false;
	}
	
	public boolean isProblematic(LightAdjacencyList adj){
		LightSCC fstSCC, sndSCC;
		LightSCCs localSCCs = adj.getLocalSCCs();
		LightSCCs sccs = new LightTarjan().executeTarjan(adj, 
				adj.removedMappings, this, false);
		
		for (LightSCC scc : sccs){
			fstSCC = scc.extract(true);
			sndSCC = scc.extract(false);

			if(!fstSCC.isEmpty() && !localSCCs.contains(fstSCC))
				return true;
			if(!sndSCC.isEmpty() && !localSCCs.contains(sndSCC))
				return true;
		}
		
		return false;
		
//		return (!fstSCC.isEmpty() && !sndSCC.isEmpty() 
//				&& !(localSCCs.contains(fstSCC) 
//						&& localSCCs.contains(sndSCC)));
	}
		
	public double [] avgInOutDegree(LightAdjacencyList adj){
		
		double [] avgDegree = new double[2];
		
		for (LightNode n : this) {
			avgDegree[0] += adj.getIncomingEdges(n, this, true, false).size();
			avgDegree[1] += adj.getOutgoingEdges(n, this, true, false).size();
		}
		avgDegree[0] /= this.size();
		avgDegree[1] /= this.size();
		
		return avgDegree;
	}
	
	public int [] dimensions(LightAdjacencyList adj){
		int [] dimensions = new int[5];
		dimensions[0] = 0;
		dimensions[1] = 0;
		dimensions[2] = (mappings != null ? mappings.size() : 0);
		dimensions[3] = 0;
		dimensions[4] = 0;
				
		for (LightNode n : this) {
			if(n.firstOnto)
				++dimensions[0];
			else
				++dimensions[1];
		}
		for (LightEdge e : extractOriginalEdges(adj)) {
			if(e.from.firstOnto)
				++dimensions[3];
			else
				++dimensions[4];
		}
 
		return dimensions;
	}
	
	public String printDimensions(LightAdjacencyList adj){
		int [] dimensions = dimensions(adj);
		return "|V| = " + (dimensions[0]+dimensions[1]) + ", |M| = " 
				+ dimensions[2] + ", |E| = " + (dimensions[3]+dimensions[4]);
	}
	
	private BigInteger factorial(int n){
        BigInteger ret = BigInteger.ONE;
        for (int i = 1; i <= n; ++i) 
        	ret = ret.multiply(BigInteger.valueOf(i));
        return ret;
    }
	
	private BigInteger binomialCoefficient(int n, int k){
        return factorial(n).divide(factorial(k).multiply(factorial(n-k)));
    }
	
	public BigInteger getMaxElementaryCyclesNum(){
		BigInteger max = BigInteger.ZERO;
		int n = size();
		
		for (int i = 1; i < n; i++)
			max = max.add(binomialCoefficient(n, n-i+1).multiply(factorial(n-1)));
		
		return max;
	}
		
	@Override
	public boolean add(LightNode e) {
		cachedHash = false;
		return super.add(e);
	}

	@Override
	public void clear() {
		cachedHash = false;
		super.clear();
	}

	@Override
	public boolean remove(Object o) {
		cachedHash = false;
		return super.remove(o);
	}

	@Override
	public int hashCode() {
		if(cachedHash)
			return hash;
		
		final int prime = 31;
		List<Integer> hashes = new ArrayList<>();
		
		for (LightNode n : this)
			hashes.add(n.hashCode());
		
		Collections.sort(hashes);

		cachedHash = true;
		return hash = prime * hashes.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		LightSCC other = (LightSCC) obj;
		
		return this.hashCode() == other.hashCode();
	}
	
	public Set<LightEdge> findBridges(LightAdjacencyList adj, Set<LightEdge> bridges) 
			throws InterruptedException{
		bridges.clear();
		LightTarjan tar = new LightTarjan();
		Set<LightEdge> removed = new HashSet<>(adj.removedMappings);
		for (LightEdge m : extractMappings(adj, true)) {
			if(Thread.interrupted())
				break;
			removed.add(m);
			if(tar.executeTarjan(adj, removed //Collections.singleton(m)
					, this, false).size() > 1)
				bridges.add(m);
			removed.remove(m);
		}
		return bridges;
	}
	
	public LightSCCs SCCexcludingSomeMappings(LightAdjacencyList adj, Set<LightEdge> excludeMappings){
		return new LightTarjan().executeTarjan(adj, excludeMappings, this, false);
	}
	
	public void printProblematicSCC(LightAdjacencyList adj){
		FileUtil.writeLogAndConsole(problematicSCCAsString(adj));
	}
	
	public String problematicSCCAsString(LightAdjacencyList adj){
		return "SCC: " + toString() + 
				"\nMappings: " + extractMappings(adj,true).toString().replace(",", ",\n") +
				"\nAll Mappings: " + extractMappings(adj,false).toString().replace(",", ",\n") +
				"\nEdges: " + extractOriginalEdges(adj).toString().replace(",", ",\n") + "\n";
	}

	public LightSCC clone() {
		LightSCC e = new LightSCC();
		for(LightNode n : this)
			e.add(n);
		return e;
	}

	public void clearTarjanIndexes() {
		for (LightNode node : this)
			node.resetTarjan();
	}
	
	public LightSCC extract(boolean first) {
		LightSCC res = new LightSCC();
		for (LightNode n : this) {
			if(n.firstOnto == first)
				res.add(n);
		}
		return res;
	}
	
	public Set<LightEdge> extractMappings(LightAdjacencyList adj, 
			boolean filterRemoved) {			
		if(mappings == null){
			mappings = new HashSet<>();
			for (LightNode n : this) {
				Set<LightEdge> outEdges = adj.getOutgoingEdges(n, this, true, 
						false);
				for (LightEdge e : outEdges)
					if(e.mapping)
						mappings.add(e);
			}
		}
		
		if(!filterRemoved)
			return mappings;

		Set<LightEdge> unremovedMappings = new HashSet<>();
		for (LightNode n : this) {
			Set<LightEdge> outEdges = adj.getOutgoingEdges(n, this, true, false);
			for (LightEdge e : outEdges)
				if(e.mapping && !adj.removedMappings.contains(e))
					unremovedMappings.add(e);
		}
		return unremovedMappings;
	}

	public Set<LightEdge> extractOriginalEdges(LightAdjacencyList adj) {
		Set<LightEdge> res = new HashSet<>();
		for (LightNode n : this) {
			for (LightEdge e : adj.getOutgoingEdges(n, this, false, true)) {
				if(this.contains(e.to))
					res.add(e);
			}
		}
		return res;
	}

	public void clearSearchIndexes() {
		for (LightNode n : this)
			n.clearSearchIndexes();
	}
	
	/********** Discarded idea ***********/
/*
	public Set<LightCycle> getAllMinimalCycles(LightAdjacencyList adj) {
		return getAllMinimalCycles(adj, true);
	}
	
	public Set<LightCycle> getAllMinimalCycles(LightAdjacencyList adj, 
			boolean allVertices){
		//Map<LightNode, List<LightCycle>> cycles = new HashMap<>();
		List<LightCycle> actualCycles = null;
		Set<LightCycle> cycles = new HashSet<>();
		Set<LightNode> nodes = new HashSet<>();
		
		if(allVertices)
			nodes.addAll(this);
		else 
			nodes.addAll(extract(extract(true).size() < extract(false).size()));
		
		for (LightNode v : nodes) {
			actualCycles = new LinkedList<>();
			BFSSpanningTree(v, adj);
			LightCycle actualCycle = new LightCycle();
			buildCyclesFromSpanningTree(v, adj, v, actualCycle, actualCycles);
			clearBFSIndexes();
			if(!actualCycles.isEmpty())
				cycles.addAll(actualCycles);
		}

		return cycles;
	}
	private void buildCyclesFromSpanningTree(LightNode start, LightAdjacencyList adj, 
			LightNode v, LightCycle cycle, List<LightCycle> cycles){

		for (LightEdge e : adj.getOutgoingEdges(v, this, true, true)) {
			if(v.succ.contains(e.to)){
				LightCycle forkedCycle = cycle.clone();
				forkedCycle.addLast(e);

				if(e.to.equals(start)){
					// otherwise could be a cycle in one of the projections
					// that are legal cycles
					if(forkedCycle.containsMappings())
						cycles.add(forkedCycle);
					continue;
				}
				buildCyclesFromSpanningTree(start,adj,e.to,forkedCycle,cycles);
			}
		}
	}
	
	private void BFSSpanningTree(LightNode v, LightAdjacencyList adj){
		Queue<LightNode> q = new LinkedList<>();
		q.add(v);
		LightNode t = null;

		while(!q.isEmpty()){
			t = q.poll();
			for (LightEdge e : adj.getOutgoingEdges(t, this, true, true)) {				
//				// to check or snd operand
//				if(e.to.pred == null || e.to.equals(v)){
//					e.to.pred = t;
//					t.succ.add(e.to);
//					//to check
//					if(!e.to.equals(v))
//						q.add(e.to);
//				}

				if(!e.to.pred.contains(e.from) || e.to.equals(v)){
					e.to.pred.add(t);
					t.succ.add(e.to);
					//to check
					if(!e.to.equals(v))
						q.add(e.to);
				}
			}
		}
	}
*/

	public LinkedList<LightEdge> BFS(LightNode start, LightNode end, 
			LightAdjacencyList adj, boolean pathsWithMappingsOnly){
		clearSearchIndexes();
		Queue<LightNode> q = new LinkedList<>();
		q.add(start);
		LightNode t = null;
		LinkedList<LightEdge> mappings;
		if(Params.verbosity > 0)
			FileUtil.writeLogAndConsole("\nBFS start: [" + start + "->" + end + "]");
		while(!q.isEmpty()){
			t = q.poll();
			if(Params.verbosity > 0)
				FileUtil.writeLogAndConsole("Actual Node: " + t + ", Queue: " + q);
			
			if(t.equals(end)){
				mappings = getBFSPath(start, end, adj, pathsWithMappingsOnly);
				if(mappings!=null){
					if(Params.verbosity > 0)
						FileUtil.writeLogAndConsole("Mappings: " + mappings);
					return mappings;
				}
				// else: keeps searching, if possible
				//continue;
			}
//			else if(t.equals(start) && !start.pred.isEmpty())
//				break;
			
			for (LightEdge e : adj.getOutgoingEdges(t, this, true, true)) {
				if(!e.to.pred.contains(t)){
					e.to.pred.add(t);
					q.add(e.to);
				}
			}
		}

		return null;
	}

	public LinkedList<LightEdge> getBFSPath(LightNode start, LightNode end, 
			LightAdjacencyList adj, boolean pathsWithMappingsOnly){
		LinkedList<LightEdge> mappings = new LinkedList<>();
		LightNode succ = null, pred = end;
		LightEdge m = null;
		Set<LightNode> visited = new HashSet<>();

		while(pred != start){
			visited.add(succ);
			succ = pred;
			if(Params.verbosity > 0)
				FileUtil.writeLogAndConsole("Actual Node: " + succ + ", Path: " + mappings);
			//pred = succ.pred.toArray(new LightNode[0])[0]; 
			for (LightNode pp : succ.pred) {
				// the node has been reached during BFS
				if((pp == start || !pp.pred.isEmpty()) 
						&& !visited.contains(pp)){ // && !pp.pred.contains(succ)){
					pred = pp;
					break;
				}
			}
			if(pred == succ)
				return null;
			
			m = adj.getEdgeBetweenNodes(pred, succ);
			if(m == null || adj.isRemoved(m)){
				if(Params.verbosity > 0)
					FileUtil.writeLogAndConsole("No arc (" + pred + ", " + succ + ")");
				return null;
			}
			if(m != null && m.mapping)
				mappings.addFirst(m);
		}
		if(pathsWithMappingsOnly)
			return mappings.isEmpty() ? null : mappings;
		
		return mappings.getFirst().from.equals(start) ? mappings : null;
	}
	
	public boolean hasInstances(LightAdjacencyList adj) {
		for (LightNode n : this)
			if(n.hasInstances(adj))
				return true;
		
		return false;
	}

	public boolean canBeProblematic(LightAdjacencyList adj) {
//		if(contains(adj.getNodeFromClass(OntoUtil.getDataFactory().getOWLNothing())))
//			return false;
		
		if(size() > 2)
			return true;

		if(size() == 2){
			LightNode [] nodes = this.toArray(new LightNode[0]);
			return nodes[0].firstOnto == nodes[1].firstOnto;
		}

		return false;
	}

	public Set<? extends OWLClassExpression> getClassSet() {
		Set<OWLClassExpression> set = new HashSet<>();

		for (LightNode n : this)
			set.add(n.getOWLClass());

		return set;
	}
}
