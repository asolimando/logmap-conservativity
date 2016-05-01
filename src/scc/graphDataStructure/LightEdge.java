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

import java.io.Serializable;

import org.semanticweb.owlapi.model.OWLAxiom;

import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;
import uk.ac.ox.krr.logmap2.utilities.Utilities;
import util.OntoUtil;

public class LightEdge implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2560470628068115762L;
	public LightNode from, to;
	//public int weight;
	public boolean mapping = false, disjoint = false;
	public double confidence = 1;
	public boolean DAG = false;
	
	private boolean cachedHash = false;
	private int hash;
	
	public LightEdge(LightNode from, LightNode to, boolean mapping, boolean disjoint){
		this.from = from;
		this.to = to;
		
		if(from.equals(to))
			throw new Error("Self-edges are not allowed");
		
		this.mapping = mapping;
		
		this.disjoint = disjoint;
	}
	
	public LightEdge(LightNode to, LightNode from, boolean mapping,
			boolean disjoint, double confidence) {
		this(to, from, mapping, disjoint);
		this.confidence = confidence;
	}
	
	@Override
	public int hashCode() {
		if(cachedHash)
			return hash;
		
		final int prime = 31;
		int result = 1;
		result = prime * result + (disjoint ? 1231 : 1237);
		result = prime * result + ((from == null) ? 0 : from.hashCode());
		result = prime * result + (mapping ? 1231 : 1237);
		result = prime * result + ((to == null) ? 0 : to.hashCode());
		cachedHash = true;
		return hash = result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LightEdge other = (LightEdge) obj;
		if (disjoint != other.disjoint)
			return false;
		if (from == null) {
			if (other.from != null)
				return false;
		} else if (!from.equals(other.from))
			return false;
		if (mapping != other.mapping)
			return false;
		if (to == null) {
			if (other.to != null)
				return false;
		} else if (!to.equals(other.to))
			return false;
		return true;
	}

	public String toString(){
		return from + (disjoint ? " DISJ (" : " -> (") + confidence + ") " + to;
	}

	public OWLAxiom toOWLAxiom() {
		return OntoUtil.getSubClassOfAxiom(from.getOWLClass(), to.getOWLClass());
	}

	public MappingObjectStr toMappingObjectStr() {
		if(!mapping)
			return null;
		return new MappingObjectStr(from.getIRIString(), to.getIRIString(), 
				confidence, Utilities.L2R, Utilities.CLASSES);
	}
}
