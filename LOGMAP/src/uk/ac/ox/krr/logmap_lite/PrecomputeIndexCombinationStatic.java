/*******************************************************************************
 * Copyright 2012 by the Department of Computer Science (University of Oxford)
 * 
 *    This file is part of LogMap.
 * 
 *    LogMap is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 * 
 *    LogMap is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 * 
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with LogMap.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package uk.ac.ox.krr.logmap_lite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Static class: not recommended when using from web service
 * This class will precompute and manage the index combination for:
 * 1. Weak mappings (combination of words to be dropped)
 * 2. Repair plans from a given input of mappings
 *
 * @author Ernesto Jimenez-Ruiz
 * Created: Sep 10, 2011
 *
 */
public class PrecomputeIndexCombinationStatic {
	
	
	private static List<Map<Integer, Set<Set<Integer>>>> precomputedCombinations = new ArrayList<Map<Integer, Set<Set<Integer>>>>();
	
	
	private static Set<Set<Integer>> identifierCombination = new HashSet<Set<Integer>>();
	private static Set<Integer> combination = new HashSet<Integer>();
	
	private static int size_combination=3; //Size of plan or size of words to be dropped
	
	private static int size_input=10; //Eaither size of mappings or size of words
	
	
	
	
	public static Set<Set<Integer>> getIdentifierCombination(int size_object, int size_combo){
		
		//Objects are inserted from position 0!
	
		if (precomputedCombinations.size()<=size_combo-1){
			
			extractIdentifierCombination(size_object, size_combo);
			
			//We are supposed to add in order: e.g. we look for plans of 2 before plans of 3		
			precomputedCombinations.add(size_combo-1, new HashMap<Integer, Set<Set<Integer>>>());
			precomputedCombinations.get(size_combo-1).put(size_object, new HashSet<Set<Integer>>(identifierCombination));
			
		}
		else if (!precomputedCombinations.get(size_combo-1).containsKey(size_object)){
			extractIdentifierCombination(size_object, size_combo);
						
			precomputedCombinations.get(size_combo-1).put(size_object, new HashSet<Set<Integer>>(identifierCombination));
		}
		

		return precomputedCombinations.get(size_combo-1).get(size_object);
		
		
	}
	
	
	public static void setDefaultSizes(int sizeinput, int sizecombination){
		size_input = sizeinput;
		size_combination=sizecombination;
	}
	
	
	
	
	public static void preComputeIdentifierCombination(){
		
		for (int j=1; j<=size_combination; j++){//subgroup
			
			precomputedCombinations.add(j-1, new HashMap<Integer, Set<Set<Integer>>>());
			
			for (int i=2; i<=size_input; i++){//size
		
				extractIdentifierCombination(i, j);
				
				//System.out.println(j + " " + i + " " +identifierCombination);
				
				//We insert in position 0
				
				precomputedCombinations.get(j-1).put(i, new HashSet<Set<Integer>>(identifierCombination));
				
				
			}
			
		}
		
		//System.out.println(precomputedCombinations.size());
		//System.out.println(precomputedCombinations);
		
	}
	
	/**
	 * Creates set of combinations of given size from a set of elements
	 * @param numElements
	 * @param sizeCombinations
	 */
	private static void extractIdentifierCombination(int numElements, int sizeCombinations){
		identifierCombination.clear();
		combination.clear();
		if (numElements==sizeCombinations){
			for (int i=0; i<numElements; i++)
				combination.add(i);
			identifierCombination.add(new HashSet<Integer>(combination));			
		} else {
			extractIdentifierCombination(numElements, sizeCombinations, 0, 0);
		}
	}
	
	/**
	 
	 * @param numElements
	 * @param sizePlan
	 * @param current_number
	 * @param level
	 */
	private static void extractIdentifierCombination(int numElements, int sizeCombinations, int current_number, int level){
		for (int i=current_number; i<numElements; i++){
			if (level==sizeCombinations-1){
				combination.add(i);
				identifierCombination.add(new HashSet<Integer>(combination));
				combination.remove(i);
			} else {
				combination.add(i);
				extractIdentifierCombination(numElements, sizeCombinations, i+1, level+1);
				combination.remove(i);
			}
		}
	}




}

