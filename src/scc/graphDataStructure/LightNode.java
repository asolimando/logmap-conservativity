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
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.util.SimpleIRIShortFormProvider;

import util.FileUtil;
import util.Params;

public class LightNode implements Serializable {

	private static final long serialVersionUID = -3308510722849779506L;
	public int index = -1, lowlink;
	private int id, orderId;
	boolean firstOnto;
	private boolean cachedHash = false;
	private int hash;
	private LightSCC localSCC = null;

	// false if the node corresponds to \existsR.T
	private boolean isNamedConcept = true; 

	private OWLClass cls;
	public static SimpleIRIShortFormProvider iriProvider = 
			new SimpleIRIShortFormProvider();
	
	// BFS/DFS mark
	public Set<LightNode> pred = new HashSet<>();
//	private LightNode succ;
	
	private IntervalLabels intLabel;
	
	public LightNode(boolean firstOnto, OWLClass cls, int id){
		this.firstOnto = firstOnto;
		this.cls = cls;
		this.id = id;
		// initially the orderId is set arbitrarily
		this.orderId = id;
	}
	
	public LightNode(boolean first, OWLClass cls, boolean isNamedConcept, 
			int id) {
		this(first,cls,id);
		this.isNamedConcept = isNamedConcept;
	}
	
	public void setIntervalLabel(IntervalLabels intLabel){
		this.intLabel = intLabel;
	}
	
	public IntervalLabels getIntervalLabel(){
		return intLabel;
	}
	
	public boolean hasInstances(LightAdjacencyList adj){
		Set<LightIndividual> inds = adj.getInstances(this); 
		return inds != null && !inds.isEmpty();
	}
	
	public Set<LightIndividual> getIndividuals(LightAdjacencyList adj){
		return adj.getInstances(this);
	}

	public boolean isNamedConcept(){
		return isNamedConcept;
	}
	
	public void setLocalSCC(LightSCC scc){		
		if(localSCC != null)
			throw new Error("Illegal state detected, localSCC for nodes " +
					"should never be overwritten!");
//		else
//			System.out.println("Prima LOCALSCC");

		localSCC = scc;
	}
	
	public LightSCC getLocalSCC(){
		return localSCC;
	}
	
	public String getOntoId(){
		return firstOnto ? "1" : "2";
	}

	public String getName(boolean full){
		return full ? cls.getIRI().toString() 
				: iriProvider.getShortForm(cls.getIRI());
	}
	
	public String getName(){
		return getName(false);
	}
	
	public String getASPSafeName(boolean debug){
		if(debug)
			return (firstOnto ? "one" : "two") 
				+ getName().replaceAll("-", "escapeminus");
		return getASPSafeName();
	}
	
	public String getASPSafeName(){
		return (firstOnto ? "one" : "two") + id;
	}
	
	public String toString(){
		return (firstOnto ? "1_" : "2_") + getName();
	}

	public void resetTarjan() {
		index = -1;
		lowlink = 0;
	}
	
	public boolean getFirstOnto(){
		return firstOnto;
	}

	@Override
	public int hashCode() {
		if(cachedHash)
			return hash;
		
		final int prime = 31;
		int result = 1;
		result = prime * result + ((cls == null) ? 0 : cls.hashCode());
		result = prime * result + (firstOnto ? 1231 : 1237);
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
		LightNode other = (LightNode) obj;
		if (cls == null) {
			if (other.cls != null)
				return false;
		} else if (!cls.equals(other.cls))
			return false;
		if (firstOnto != other.firstOnto)
			return false;
		return true;
	}

	public int getId() {
		return id;
	}

	public int getOrderId(){
		return orderId;
	}
	
	public void setOrderId(int orderId){
		this.orderId = orderId;
	}
	
	public void clearSearchIndexes(){
		index = -1;
		pred.clear();
//		succ = null;
	}
	
	public LightNode getPred(){
		return pred.toArray(new LightNode[0])[0];
	}

	public void setIntervalLabel(int i, int begin, int end) {
		if(intLabel==null)
			intLabel = new IntervalLabels(Params.labelDims);
		intLabel.add(i-1, new IntervalLabel(begin, end));
		if(Params.verbosity > -2)
			FileUtil.writeLogAndConsole("label(" + this + "): " + intLabel.get(i-1));
	}
	
	public boolean reaches(LightNode n){
		if(Params.verbosity > -2)
			FileUtil.writeLogAndConsole(this + " -> " + n);
		if(this.equals(n))
			return true;
		return getIntervalLabel().subsumes(n.getIntervalLabel());
	}

	public OWLClass getOWLClass() {
		return cls;
	}

	public String getIRIString() {
		return getName(true);
	}

	public boolean isTopOrNothing() {
		return cls.isOWLNothing() || cls.isOWLThing();
	}
}
