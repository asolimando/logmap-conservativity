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
import scc.graphDataStructure.LightEdge;

import java.util.List;
import java.util.Map;

import org.uncommons.maths.binary.BitString;
import org.uncommons.watchmaker.framework.FitnessEvaluator;

public class DiagnosisFitnessEvaluator implements FitnessEvaluator<BitString> {

	private Map<Integer,LightEdge> map;
	
	public DiagnosisFitnessEvaluator(Map<Integer,LightEdge> map){
		this.map = map;
	}
	
	@Override
	public double getFitness(BitString elem, 
			List<? extends BitString> population) {
		return new Diagnosis(elem, map).getWeight();
	}

	@Override
	public boolean isNatural() {
		return false;
	}

}
