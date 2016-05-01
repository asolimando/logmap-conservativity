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

import scc.graphDataStructure.Diagnosis;
import scc.graphDataStructure.LightAdjacencyList;
import scc.graphDataStructure.LightCycle;
import scc.graphDataStructure.LightCycles;
import scc.graphDataStructure.LightSCC;

public class ResultASPSGA {
	private int numCycles, vtx1, vtx2, edges1, edges2, mappings;
	private long aspCTime, aspNCTime, sgaTime, greedyCTime, greedyWTime, 
	filterTime, cycleTime;
	private double aspCWeight, aspNCWeight, sgaWeight, 
	greedyCWeight, greedyWWeight, filterWeight, 
	avgCycleLen, avgInDegree, avgOutDegree, 
	aspCSize, aspNCSize, sgaSize, greedyCSize, greedyWSize, filterSize;
	
	public static String printResult(LightAdjacencyList adj, LightSCC scc, 
			LightCycles cycles, long cycleTime, long aspCTime, 
			long aspNCTime, long sgaTime, long greedyCTime, 
			long greedyWTime, long filterTime,
			double aspCWeight, double aspNCWeight, 
			double sgaWeight, double greedyCWeight, double greedyWWeight,
			double filterWeight, double aspCSize, double aspNCSize, 
			double sgaSize, double greedyCSize, double greedyWSize, 
			double filterSize){
		
		int [] dims = scc.dimensions(adj);
		
		Diagnosis d = new Diagnosis(scc.extractMappings(adj, false));
		double alignWeight = d.getWeight();
		
		double avgCycleLen = -1;

		if(cycles != null){
			avgCycleLen = 0;
			for (LightCycle c : cycles)
				avgCycleLen += c.size();
			avgCycleLen /= cycles.size();
		}
		
		double [] avgDegree = scc.avgInOutDegree(adj);
		
		StringBuilder str = new StringBuilder();
		str.append((cycles != null ? cycles.size() : -1) + " ");
		str.append(avgCycleLen + " ");
		str.append(cycleTime + " ");
		str.append(dims[0] + " ");
		str.append(dims[1] + " ");
		str.append(dims[2] + " ");
		str.append(dims[3] + " ");
		str.append(dims[4] + " ");
		str.append(avgDegree[0] + " ");
		str.append(avgDegree[1] + " ");
		
		str.append(alignWeight + " ");
		
		str.append(aspCTime + " ");
		str.append(aspNCTime + " ");
		str.append(sgaTime + " ");
		str.append(greedyCTime + " ");
		str.append(greedyWTime + " ");
		str.append(filterTime + " ");
		
		str.append(aspCWeight + " ");
		str.append(aspNCWeight + " ");
		str.append(sgaWeight + " ");
		str.append(greedyCWeight + " ");
		str.append(greedyWWeight + " ");
		str.append(filterWeight + " ");
		
		str.append(aspCSize + " ");
		str.append(aspNCSize + " ");
		str.append(sgaSize + " ");
		str.append(greedyCSize + " ");
		str.append(greedyWSize + " ");
		str.append(filterSize + "\n");
		
		return str.toString();
	}
	
	public ResultASPSGA(LightAdjacencyList adj, LightSCC scc, 
			LightCycles cycles, long cycleTime, long aspCTime, 
			long aspNCTime, long sgaTime, long greedyCTime, long greedyWTime, 
			long filterTime,
			double aspCWeight, double aspNCWeight, 
			double sgaWeight, double greedyCWeight, double greedyWWeight,
			double filterWeight, double aspCSize, double aspNCSize, 
			double sgaSize, double greedyCSize, double greedyWSize, 
			double filterSize) 
	{
		this.numCycles = cycles == null ? -1 : cycles.size();
		this.cycleTime = cycleTime;
		this.aspCTime = aspCTime;
		this.aspNCTime = aspNCTime;
		this.sgaTime = sgaTime;
		this.greedyCTime = greedyCTime;
		this.greedyWTime = greedyWTime;
		this.filterTime = filterTime;
		this.aspCWeight = aspCWeight;
		this.aspNCWeight = aspNCWeight;
		this.sgaWeight = sgaWeight;
		this.greedyCWeight = greedyCWeight;
		this.greedyWWeight = greedyWWeight;
		this.filterWeight = filterWeight;
		this.aspCSize = aspCSize;
		this.aspNCSize = aspNCSize;
		this.sgaSize = sgaSize;
		this.greedyCSize = greedyCSize;
		this.greedyWSize = greedyWSize;
		this.filterSize = filterSize;
		
		int [] dims = scc.dimensions(adj);
		vtx1 = dims[0];
		vtx2 = dims[1];
		mappings = dims[2];
		edges1 = dims[3];
		edges2 = dims[4];
		
		if(cycles == null)
			avgCycleLen = -1;
		else {
			for (LightCycle c : cycles)
				avgCycleLen += c.size();
			avgCycleLen /= cycles.size();
		}
		
		double [] avgDegree = scc.avgInOutDegree(adj);
		avgInDegree = avgDegree[0];
		avgOutDegree = avgDegree[1];
		
		System.out.println(this);
	}
	
	@Override
	public String toString(){
		StringBuilder str = new StringBuilder();
		str.append(numCycles + " ");
		str.append(avgCycleLen + " ");
		str.append(cycleTime + " ");
		str.append(vtx1 + " ");
		str.append(vtx2 + " ");
		str.append(mappings + " ");
		str.append(edges1 + " ");
		str.append(edges2 + " ");
		str.append(avgInDegree + " ");
		str.append(avgOutDegree + " ");
		
		str.append(aspCTime + " ");
		str.append(aspNCTime + " ");
		str.append(sgaTime + " ");
		str.append(greedyCTime + " ");
		str.append(greedyWTime + " ");
		str.append(filterTime + " ");
		
		str.append(aspCWeight + " ");
		str.append(aspNCWeight + " ");
		str.append(sgaWeight + " ");
		str.append(greedyCWeight + " ");
		str.append(greedyWWeight + " ");
		str.append(filterWeight + " ");
		
		str.append(aspCSize + " ");
		str.append(aspNCSize + " ");
		str.append(sgaSize + " ");
		str.append(greedyCSize + " ");
		str.append(greedyWSize + " ");
		str.append(filterSize + "\n");
		
		return str.toString();
	}
}
