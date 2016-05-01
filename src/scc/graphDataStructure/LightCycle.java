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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.uncommons.maths.binary.BitString;

public class LightCycle extends LinkedList<LightEdge>{

	private boolean cachedHash = false;
	private int hash;
	private BitString b = null;

	@Override
	public int hashCode() {
		if(cachedHash)
			return hash;

		final int prime = 31;
		List<Integer> hashes = new ArrayList<>();

		for (LightEdge e : this)
			hashes.add(e.hashCode());

		//Collections.sort(hashes);

		cachedHash = true;
		return hash = prime * hashes.hashCode();
	}

	public BitString toBitString(Map<LightEdge, Integer> map){
		if(b == null){
			b = new BitString(map.size());
			for (LightEdge e : this) {
				if(e.mapping)
					b.setBit(map.get(e), true);
			}
		}
		return b;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		//		if (!super.equals(obj))
		//			return false;
		if (getClass() != obj.getClass())
			return false;
		LightCycle other = (LightCycle) obj;

		return this.hashCode() == other.hashCode();
	}

	private static final long serialVersionUID = -4301191094433331283L;

	public LightCycle clone(){
		LightCycle e = (LightCycle) super.clone();
		e.clear();
		for(LightEdge p : this)
			e.add(p);
		return e;
	}

	public boolean containsMappings(){
		for (LightEdge e : this)
			if(e.mapping)
				return true;

		return false;
	}

	public boolean isNontrivialSafeCycle(){
		// test if it is trivial
		if(size() <= 2)
			return false;

		LightSCC firstOntoSCC = null, secondOntoSCC = null;

		// the cycle is safe iff all the elements of the same input ontology 
		// belong to the same localSCC 
		for (LightEdge e : this) {
			if(e.from.firstOnto){
				if(firstOntoSCC == null){
					firstOntoSCC = e.from.getLocalSCC();
				}
				else{
					if(!e.from.getLocalSCC().equals(firstOntoSCC))
						return false;
				}
			}
			else {
				if(secondOntoSCC == null){
					secondOntoSCC = e.from.getLocalSCC();
				}
				else{
					if(!e.from.getLocalSCC().equals(secondOntoSCC))
						return false;
				}
			}
		}
		
		return true;
	}

	@Override
	public void add(int index, LightEdge element) {
		cachedHash = false;
		super.add(index, element);
	}

	@Override
	public void addFirst(LightEdge e) {
		cachedHash = false;
		super.addFirst(e);
	}

	@Override
	public void addLast(LightEdge e) {
		cachedHash = false;
		super.addLast(e);
	}

	@Override
	public void clear() {
		cachedHash = false;
		super.clear();
	}

	@Override
	public boolean offer(LightEdge e) {
		cachedHash = false;
		return super.offer(e);
	}

	@Override
	public boolean offerFirst(LightEdge e) {
		cachedHash = false;
		return super.offerFirst(e);
	}

	@Override
	public boolean offerLast(LightEdge e) {
		cachedHash = false;
		return super.offerLast(e);
	}

	@Override
	public LightEdge poll() {
		cachedHash = false;
		return super.poll();
	}

	@Override
	public LightEdge pollFirst() {
		cachedHash = false;
		return super.pollFirst();
	}

	@Override
	public LightEdge pollLast() {
		cachedHash = false;
		return super.pollLast();
	}

	@Override
	public LightEdge pop() {
		cachedHash = false;
		return super.pop();
	}

	@Override
	public void push(LightEdge e) {
		cachedHash = false;
		super.push(e);
	}

	@Override
	public LightEdge remove() {
		cachedHash = false;
		return super.remove();
	}

	@Override
	public LightEdge remove(int index) {
		cachedHash = false;
		return super.remove(index);
	}

	@Override
	public boolean remove(Object o) {
		cachedHash = false;
		return super.remove(o);
	}

	@Override
	public LightEdge removeFirst() {
		cachedHash = false;
		return super.removeFirst();
	}

	@Override
	public boolean removeFirstOccurrence(Object o) {
		cachedHash = false;
		return super.removeFirstOccurrence(o);
	}

	@Override
	public LightEdge removeLast() {
		cachedHash = false;
		return super.removeLast();
	}

	@Override
	public boolean removeLastOccurrence(Object o) {
		cachedHash = false;
		return super.removeLastOccurrence(o);
	}

	@Override
	public LightEdge set(int index, LightEdge element) {
		cachedHash = false;
		return super.set(index, element);
	}

	@Override
	public boolean add(LightEdge e){
		cachedHash = false;
		return super.add(e);
	}

	@Override
	public boolean addAll(Collection<? extends LightEdge> c){
		cachedHash = false;
		return super.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends LightEdge> c){
		cachedHash = false;
		return super.addAll(index,c);
	}

// INCORRECT, IT IS SAFE IFF THE PROJECTIONS ARE SUBSET, NOT NECESSARILY LOCALSSCs
//	public boolean safeCycle(LightSCCs localSCCs) {
//		LightSCC fstProj = new LightSCC(), sndProj = new LightSCC();
//		for (LightEdge e : this) {
//			if(e.from.firstOnto)
//				fstProj.add(e.from);
//			else
//				sndProj.add(e.from);
//
//			if(e.to.firstOnto)
//				fstProj.add(e.to);
//			else
//				sndProj.add(e.to);
//		}
//
//		if(fstProj.size() == 1 && sndProj.size() == 1)
//			return false;
//
//		boolean fstRet = localSCCs.contains(fstProj), 
//				sndRet = localSCCs.contains(sndProj);
//
//		if(fstRet && sndRet)
//		{
//			System.out.println("First Projection is safe: " + fstProj);
//			System.out.println("Second Projection is safe: " + sndProj);
//		}
//
//		return fstRet && sndRet;
//	}
}
