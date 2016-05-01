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

import scc.graphDataStructure.Diagnosis;
import scc.graphDataStructure.LightCycles;
import java.util.List;
import java.util.Random;
import org.uncommons.maths.binary.BitString;
import org.uncommons.watchmaker.framework.EvolutionaryOperator;

import scc.algoSolver.SGASolver;

public class BitStringToDiagnosis implements EvolutionaryOperator<BitString> {

	private SGASolver sga; 
	
	public BitStringToDiagnosis(SGASolver sga){
		this.sga = sga;
	}
	
	private void removeMappings(BitString bs){
		for (int idx : sga.removedIndexes)
			bs.setBit(idx, true);
	}
	
	@Override
	public List<BitString> apply(List<BitString> list, Random rnd) {
		
//		for (Entry<LightEdge, Integer> e : sga.mappingIndex.entrySet()) {
//			System.out.println(e.getValue() + " -> " + e.getKey());
//		}
				
		for (BitString bitString : list) {

			// remove mappings that are asserted as removed
			removeMappings(bitString);
			
//			System.out.println("Pre: " + bitString);
			
			//Diagnosis d = new Diagnosis(bitString,sga.indexMapping);
			
			//if(d.isDiagnosis(sga.getCycles()))
			if(!BitStringUtils.fastIsDisjoint(bitString, 
					sga.getCycles().toBitStrings(sga.mappingIndex)))
				continue;
			
			LightCycles cycles = new LightCycles();
			cycles.addAll(sga.getCycles());

			BitStringUtils.bitStringToDiagnosis(cycles, bitString, 
					sga.mappingIndex, rnd);
			
//			Diagnosis dia;
//			if(!(dia = new Diagnosis(sga.mappingIndex,bitString)).isDiagnosis(cycles))
//				throw new Error("Invalid diagnosis " + dia);
			if(BitStringUtils.fastIsDisjoint(bitString, 
					cycles.toBitStrings(sga.mappingIndex)))
				throw new Error(bitString + " invalid diagnosis " 
					+ new Diagnosis(sga.mappingIndex,bitString));
			
//			System.out.println("Post: " + bitString + " ");						
		}
		
		return list;
	}
}
