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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.uncommons.maths.binary.BitString;

import util.FileUtil;

public class LightCycles extends HashSet<LightCycle> implements Serializable {

	private static final long serialVersionUID = -5016759277938114455L;
	public LightSCC scc;
	private List<MappingCycles> rankingCache = null;
	private List<BitString> bitStrings = null;
	
	public LightCycles(LightSCC scc){
		this.scc = scc;
	}
	
	public LightCycles() {
		super();
	}
	
	public int removeNontrivialSafeCycles(){
		int a = 0;
		Map<LightEdge, HashSet<LightCycle>> map = new HashMap<>();
		Map<LightCycle, Boolean> mapRemovable = new HashMap<>();
		
		for (LightCycle c : this) {
			if(c.isNontrivialSafeCycle()){
				mapRemovable.put(c, true);
				for (LightEdge e : c) {
					if(map.containsKey(e)){
						map.get(e).add(c);
						mapRemovable.put(c, false);
					}
					else {
						HashSet<LightCycle> cycleSet = new HashSet<>(); 
						cycleSet.add(c);
						map.put(e, cycleSet);
					}
				}
			}
			else
				mapRemovable.put(c, false);
		}
		
		Iterator<LightCycle> itr = iterator();
		
		while(itr.hasNext()){
			LightCycle c = itr.next();
			if(mapRemovable.get(c)){
				FileUtil.writeLogAndConsole("Removed: " + c);
				itr.remove();
				++a;
			}
		}
		
		return a;
	}
	
	public boolean nontrivialSafeCycles(){
		int a = 0;
		for (LightCycle c : this) {
			if(c.isNontrivialSafeCycle()){
				FileUtil.writeLogAndConsole("Nontrivial Safe Cycle: " + c);
				++a;
			}
		}
		if(a > 0)
			FileUtil.writeLogAndConsole(a + " nontrivial safe cycles out of " + this.size());
		return a > 0;
	}
	
// INCORRECT, see the safeCycle for LightCycle
//	public boolean safeCycle(LightSCCs localSCCs){
//		int a = 0;
//		for (LightCycle c : this) {
//			if(c.safeCycle(localSCCs)){
//				System.out.println("Safe Cycle: " + c);
//				++a;
//			}
//		}
//		if(a > 0)
//			System.out.println(a + " safe out of " + this.size());
//		return a > 0;
//	}
	
	@Override
	public void clear(){
		rankingCache = null;
		bitStrings = null;
		super.clear();
	}
	
	@Override
	public boolean add(LightCycle e){
		boolean res = super.add(e);
		if(!res)
			return res;
		
		rankingCache = null;
		bitStrings = null;
		
		return res;
	}
	
	@Override
	public boolean addAll(Collection<? extends LightCycle> c){
		boolean res = super.addAll(c);
		if(!res)
			return res;
		
		rankingCache = null;
		bitStrings = null;

		return res;
	}
	
	@Override
	public boolean remove(Object o){
		boolean res = super.remove(o);
		if(!res)
			return res;
		
		rankingCache = null;
		bitStrings = null;

		return res;
	}
	
	@Override
	public boolean removeAll(Collection<?> o){
		boolean res = super.removeAll(o);
		if(!res)
			return res;
		
		rankingCache = null;
		bitStrings = null;

		return res;
	}
	
	@Override
	public boolean retainAll(Collection<?> c){
		boolean res = super.retainAll(c);
		if(!res)
			return res;
		
		rankingCache = null;
		bitStrings = null;

		return res;
	}

	private List<MappingCycle> generateMappingList(){
		List<MappingCycle> res = new LinkedList<>();
		
		for (LightCycle cycle : this) {
			for (LightEdge e : cycle) {
				if(e.mapping)
					res.add(new MappingCycle(e,cycle));
			}
		}
		
		Collections.sort(res, new MappingCycleComparator());
		return res;
	}
	
	public List<MappingCycles> generateMappingRanking(boolean cardOpt){
		if(rankingCache != null)
			return rankingCache;
		
		List<MappingCycles> ranking = new LinkedList<>(); 
		List<MappingCycle> mappingList = generateMappingList();
		
		LightEdge prev = null;
		MappingCycles actualEl = null;
		
		Iterator<MappingCycle> itr = mappingList.iterator();
		MappingCycle c = null;
		
		while(itr.hasNext()){
			c = itr.next();
			
			if(c.mapping.equals(prev)){
				actualEl.cycles.add(c.cycle);
				if(!itr.hasNext())
					ranking.add(actualEl);
			}
			else{
				if(actualEl != null)
					ranking.add(actualEl);
				
				actualEl = new MappingCycles(c);
				prev = c.mapping;
			}
		}
		
		Collections.sort(ranking, new MappingCyclesComparator(cardOpt));
		
		return rankingCache = ranking;
	}
	
	class MappingCyclesComparator implements Comparator<MappingCycles> {

		boolean optCard;
		
		public MappingCyclesComparator(boolean optCard){
			this.optCard = optCard;
		}
		
		@Override
		public int compare(MappingCycles a, MappingCycles b) {

			int rank = Integer.compare(b.rank(), a.rank()), 
					weight = Double.compare(a.mapping.confidence, 
							b.mapping.confidence);
			
			int res = optCard ? rank : weight;
			//int res = Integer.compare(b.rank(), a.rank());

			if(res == 0)
				res = optCard ? weight : rank;
				//res = Double.compare(a.mapping.confidence, b.mapping.confidence);
			
			if(res == 0)
				res = Integer.compare(b.mapping.hashCode(), 
						a.mapping.hashCode());
			
			return res;
		}
	}
	
	class MappingCycleComparator implements Comparator<MappingCycle> {

		@Override
		public int compare(MappingCycle a, MappingCycle b) {

			int res = Integer.compare(a.mapping.hashCode(), b.mapping.hashCode());

			if(res == 0)
				res = Integer.compare(a.cycle.hashCode(), b.cycle.hashCode());
			return res;
		}
	}

	public List<BitString> toBitStrings(Map<LightEdge, Integer> mappingIndex) {
		if(bitStrings != null)
			return bitStrings;
		
		bitStrings = new ArrayList<BitString>(size());
		for (LightCycle c : this)
			bitStrings.add(c.toBitString(mappingIndex));

		return bitStrings;
	}
}
