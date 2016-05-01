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
package scc.evol;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import scc.graphDataStructure.Diagnosis;
import scc.graphDataStructure.LightCycle;
import scc.graphDataStructure.LightCycles;
import scc.graphDataStructure.LightEdge;

import org.uncommons.maths.binary.BitString;
import org.uncommons.watchmaker.framework.factories.BitStringFactory;

import util.Params;

public class BitStringCycleFactory extends BitStringFactory {
	private int length, candidateNum; 
	private LightCycles cycles;
	private Map<LightEdge,Integer> mappingIndex;
	private List<Diagnosis> firstInds;
	
	public BitStringCycleFactory(int length, LightCycles cycles, 
			Map<LightEdge,Integer> map, List<Diagnosis> heur) {
		super(length);
		this.length = length;
		this.cycles = cycles;
		this.mappingIndex = map;
		this.firstInds = heur;
//		for (Entry<LightEdge, Integer> e : map.entrySet()) {
//			System.out.println(e.getValue() + " -> " + e.getKey());
//		}
	}
	
	public BitString generateRandomCandidate(Random rng){
		BitString b = new BitString(length);
		
//		if(candidateNum == 0){
////			List<MappingCycles> ranking = cycles.generateMappingRanking();
////			int c = 0;
////			while(true){
////				b.setBit(mappingIndex.get(ranking.get(c++).mapping), true);
////				if(!BitStringUtils.fastIsDisjoint(b, cycles.toBitStrings(mappingIndex)))
////					break;
////				if(c >= ranking.size())
////					BitStringUtils.bitStringToDiagnosis(cycles, b, mappingIndex, rng);
////			}
////			return b;
//			return firstInds.getBitString(mappingIndex);
//		}
		
		if(candidateNum < firstInds.size())
			return firstInds.get(candidateNum).getBitString(mappingIndex);
		
		for (LightCycle cycle : cycles) {
			BitString c = cycle.toBitString(mappingIndex);
			while(true){
				int i = rng.nextInt(length);
				if(c.getBit(i)){
					b.setBit(i, true);
					break;
				}
			}
		}
		
		//Diagnosis d;
		//if(!(d = new Diagnosis(map,b)).isDiagnosis(cycles))
		if(Params.testMode && BitStringUtils.fastIsDisjoint(b, 
				cycles.toBitStrings(mappingIndex)))
			throw new Error(b + " is an invalid diagnosis " 
				+ new Diagnosis(mappingIndex,b));
//			throw new Error(b + " is an invalid diagnosis " + d);
		
		return b;
	}
}
