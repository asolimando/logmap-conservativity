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

import scc.graphDataStructure.LightCycle;
import scc.graphDataStructure.LightCycles;
import scc.graphDataStructure.LightEdge;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.uncommons.maths.binary.BitString;

public class BitStringUtils {
	public static boolean isDisjoint(BitString one, BitString two){
		if(one.getLength() != two.getLength())
			throw new Error("BitStrings of different lengths cannot be compared");
		for (int i = 0; i < one.getLength(); i++) {
			if(one.getBit(i) && two.getBit(i))
				return false;
		}
		return true;
	}
	
	public static boolean fastIsDisjoint(BitString b, List<BitString> bs){
		for (BitString bitString : bs)
			if(fastIsDisjoint(b, bitString))
				return true;
		return false;
	}
	
	public static void bitStringToDiagnosis(LightCycles cycles, BitString bitString, 
			Map<LightEdge,Integer> mappingIndex, Random rnd){
		for (LightCycle cycle : cycles) {
			BitString bs = cycle.toBitString(mappingIndex);
			if(BitStringUtils.fastIsDisjoint(bitString, bs)){
				while(true){
					int i = rnd.nextInt(mappingIndex.size());
					if(bs.getBit(i)){
						bitString.setBit(i, true);
						break;
					}
				}
			}
		}
	}

	public static boolean fastIsDisjoint(BitString one, BitString two){
		if(one.getLength() != two.getLength())
			throw new Error("BitStrings of different lengths cannot be compared");
		
		// BEGIN reflection code
		Class rightJavaClass1 = one.getClass(), rightJavaClass2 = one.getClass();

		Field[] fs1 = rightJavaClass1.getDeclaredFields(), 
				fs2 = rightJavaClass2.getDeclaredFields();
		int[] data1 = null; 
		int[] data2 = null;
		
		try {
			Field operands1 = Class.forName(rightJavaClass1.getName()).
					getDeclaredField(fs1[2].getName()), 
					operands2 = Class.forName(rightJavaClass2.getName()).
					getDeclaredField(fs2[2].getName());
			operands1.setAccessible(true);
			operands2.setAccessible(true);

			data1 = (int []) operands1.get(one); 
			data2 = (int []) operands2.get(two);
			operands1.setAccessible(false);
			operands2.setAccessible(false);

		} catch (IllegalArgumentException | IllegalAccessException 
				| NoSuchFieldException | SecurityException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		// END reflection code
		
		for (int i = 0; i < data1.length; i++)
			if((data1[i] & data2[i]) != 0)
				return false;

		return true;
	}
}
