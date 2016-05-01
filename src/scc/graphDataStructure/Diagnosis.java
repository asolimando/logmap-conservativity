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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import scc.mapping.LightAlignment;

import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.uncommons.maths.binary.BitString;

import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;
import util.FileUtil;

public class Diagnosis extends HashSet<LightEdge> {

	private static final long serialVersionUID = 6616490127967064347L;
	private double weight;
	private BitString b = null;
	private Map<LightEdge, Integer> mappingIndex = null;
	private long time = -1;
	private boolean optimal = false;
	
	public Diagnosis(){
		super();
	}

	public Diagnosis(Collection<LightEdge> mappings) {
		super(mappings);
//		for (LightEdge m : mappings) {
//			weight += m.confidence;
//		}
	}

	public Diagnosis(Map<LightEdge, Integer> mappingIndex, BitString individual) {		
		this.mappingIndex = mappingIndex;
		
		for (LightEdge e : mappingIndex.keySet()) 
			if(individual.getBit(mappingIndex.get(e)))
				add(e);
	}

	public Diagnosis(BitString individual, Map<Integer, LightEdge> indexMapping) {				
		for (int i = 0; i < individual.getLength(); i++)
			if(individual.getBit(i))
				add(indexMapping.get(i));
	}
	
	public BitString getBitString(Map<LightEdge, Integer> mappingIndex){
		if(b==null){
			this.mappingIndex = mappingIndex;
			b = new BitString(mappingIndex.size());
			for (LightEdge m : this) {
				b.setBit(mappingIndex.get(m), true);
			}
		}
		return b;
	}

//	public int numUnbrokenCycle(LightCycles cycles){
//		LightCycles aux = new LightCycles();
//		aux.addAll(cycles);
//
//		for (LightEdge m : this) {
//
//			Iterator<LightCycle> itr = aux.iterator();
//			LightCycle actCycle = null;
//
//			while(itr.hasNext()){
//				actCycle = itr.next();
//
//				// do not break, mapping could break multiple cycles
//				if(actCycle.contains(m))
//					itr.remove();
//			}
//
//		}
//		return aux.size();
//	}

	public boolean isDiagnosis(LightAdjacencyList adj, LightSCC scc){
		return adj.computeProblematicSCCs(
				scc.SCCexcludingSomeMappings(adj, this)).isEmpty();
	}
	
//	public boolean isDiagnosis(LightCycles cycles){
//		return numUnbrokenCycle(cycles) == 0;
//	}
//	
//	public boolean isDiagnosis(LightCycles cycles, 
//		Map<LightEdge, Integer> mappingIndex){
//		List<BitString> bs = cycles.toBitStrings(mappingIndex);
//		BitString diag = this.getBitString(mappingIndex);
//		
//		for (BitString b : bs)
//			if(!BitStringUtils.fastIsDisjoint(diag, b))
//				return false; 
//
//		return true;
//	}

	@Override
	public void clear(){
		weight = 0;
		if(b != null)
			b = new BitString(b.getLength());
		super.clear();
	}

	@Override
	public boolean add(LightEdge e){
		//weight = -1;
		if(e == null)
			return false;
		
		boolean res = super.add(e);
		if(!res)
			return res;
		
		weight += e.confidence;
		
		if(mappingIndex != null)
			b.setBit(mappingIndex.get(e), true);
		else
			b = null;
		
		return res;
	}

	@Override
	public boolean remove(Object e){
		boolean res = super.remove(e);
		if(!res)
			return res;
		
		//weight = -1;
		weight -= ((LightEdge) e).confidence;
		
		if(mappingIndex != null)
			b.setBit(mappingIndex.get(e), false);
		else
			b = null;
		
		return res;
	}

	@Override
	public boolean removeAll(Collection<?> c){
//		//weight = -1;
//		boolean res = super.removeAll(c);
//		if(!res)
//			return res;
//		
//		for (Object object : c)
//			weight -= ((LightEdge)object).confidence;
//		
//		if(mappingIndex != null)
//			for (Object object : c)
//				b.setBit(mappingIndex.get(object), false);
//		else
//			b = null;
//		
//		return res;
		boolean res = true;
		for (Object o : c)
			res = res && remove(o);
		return res;
	}

	@Override
	public boolean addAll(Collection<? extends LightEdge> c){
//		//weight = -1;
		boolean res = super.addAll(c);
//		if(!res)
//			return res;
//		
//		for (Object object : c)
//			weight += ((LightEdge)object).confidence;
//		
//		if(mappingIndex != null)
//			for (Object object : c)
//				b.setBit(mappingIndex.get(object), true);
//		else
//			b = null;
//		
		return res;
	}

	public boolean retainAll(Collection<?> c){
		boolean res = super.retainAll(c);
		if(!res)
			return res;
		weight = -1;
		weight = getWeight();
		
		if(mappingIndex != null)
			for (LightEdge e : this)
				b.setBit(mappingIndex.get(e), true);
		else
			b = null;
		
		return res;		
	}

	public double getWeight(){
		if(weight != -1)
			return weight;

		double weight = 0;

		for (LightEdge m : this)
			weight += m.confidence;

		return weight;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public boolean isOptimal() {
		return optimal;
	}

	public void setOptimal(boolean optimal) {
		this.optimal = optimal;
	}

	public LightAlignment toAlignment() {
		LightAlignment align = new LightAlignment();

		try {
			for (LightEdge e : this)
				align.addAlignCell(e.from.getOWLClass().getIRI().toURI(), 
						e.to.getOWLClass().getIRI().toURI(), "<", e.confidence);
		} catch (AlignmentException e1) {
			FileUtil.writeErrorLogAndConsole("Cannot convert diagnosis to alignment");
			e1.printStackTrace();
		}	
		return align;
	}

	public Set<LightEdge> getMultipleMappings() {
		Set<LightEdge> multiMappings = new HashSet<>();
		Map<LightNode, List<LightEdge>> mapFrom = new HashMap<>();
		Map<LightNode, List<LightEdge>> mapTo = new HashMap<>();
		LinkedList<LightEdge> list = null;
		
		for (LightEdge m : this) {
			if(!mapFrom.containsKey(m.from)){
				list = new LinkedList<>();
				list.add(m);
				mapFrom.put(m.from, list);
			}
			else
				mapFrom.get(m.from).add(m);
			
			if(!mapTo.containsKey(m.to)){
				list = new LinkedList<>();
				list.add(m);
				mapTo.put(m.to, list);
			}
			else
				mapTo.get(m.to).add(m);
		}
		
		for (List<LightEdge> l : mapFrom.values()){
			if(l.size() > 1)
				multiMappings.addAll(l);
		}
		for (List<LightEdge> l : mapTo.values()){
			if(l.size() > 1)
			multiMappings.addAll(l);
		}
		
		return multiMappings;
	}

	public Set<OWLAxiom> toOWLAxioms() {
		Set<OWLAxiom> res = new HashSet<>();
		
		for (LightEdge e : this)
			res.add(e.toOWLAxiom());
		
		return res;
	}

	public Set<MappingObjectStr> toMappingObjectStr() {
		
		Set<MappingObjectStr> align = new HashSet<>();
		MappingObjectStr m;
		
		for (LightEdge e : this)
			if((m = e.toMappingObjectStr()) != null)
				align.add(m);

		return align;
	}
	
	@Override
	public String toString(){
		return "Size: " + size() + ", w: " + weight + ", getW(): " 
				+ getWeight() + " = " +super.toString();
	}
}
