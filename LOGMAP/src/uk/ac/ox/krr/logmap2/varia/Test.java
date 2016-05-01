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
package uk.ac.ox.krr.logmap2.varia;

import java.util.HashSet;


import java.util.Set;

import uk.ac.ox.krr.logmap2.lexicon.NormalizeNumbers;
import uk.ac.ox.krr.logmap2.utilities.Utilities;
import uk.ac.ox.krr.logmap2.io.ReadFile;
import uk.ac.ox.krr.logmap2.io.WriteFile;

public class Test {
	
	
	public Test(int m, int r) {
		
		//getCandidateRepairPlans(m, r, 0, 0);
		
		//System.out.println(currentPlans);
		
		String[] elements = "lalala".split("_");
		
		System.out.println(elements.length + "  " + elements[0]);
		
		//loadMappingsUMLS();
		
	}
	
	
	
	/**
	 * UMLS mappings will be our gold standard. Candidate entities from ontologies should be sismilar to the mapped entities with UMLS
	 * @throws Exception
	 */
	private void loadMappingsUMLS(){
	
		String file_mappings;
		String rootPath = "/usr/local/data/DataUMLS/";
		int i;
		
		//file_mappings = rootPath +  "UMLS_source_data/onto_mappings_SNOMED_NCI.txt";
		//i=0;
		
		file_mappings = rootPath +  "UMLS_source_data/onto_mappings_FMA_SNOMED.txt";
		i=1;
		
		try {
		ReadFile reader = new ReadFile(file_mappings);
		WriteFile writer = new WriteFile(file_mappings + "2");
		
		
		String line;
		String[] elements;
		
		line=reader.readLine();
		
		
		String changed;
		
		while (line!=null) {
			
			if (line.indexOf("|")<0){
				line=reader.readLine();
				continue;
			}
			
			
			
			elements=line.split("\\|");
			
			//changed = elements[i];
			changed=Utilities.getEntityLabelFromURI(elements[i]);
			
			
			
			
			/*changed = elements[i].replaceAll(",", "_");
			changed = changed.replaceAll("__", "_");
			
			changed = changed.replaceAll(";", "_");
			changed = changed.replaceAll("__", "_");
			
			changed = changed.replaceAll("'", "_");
			changed = changed.replaceAll("__", "_");
			
			changed = changed.replaceAll("\"", "_");
			changed = changed.replaceAll("__", "_");
			
			changed = changed.replaceAll("-", "_");
			changed = changed.replaceAll("__", "_");
			changed = changed.replaceAll("__", "_");*/
			
			changed = changed.replaceAll(".*[^a-zA-Z0-9_].*", "_");			
			changed = changed.replaceAll("__", "_");
			changed = changed.replaceAll("__", "_");
			
			changed = Utilities.getNameSpaceFromURI(elements[i]) + "#" + changed;
			
			
			
			if (i==0){
				writer.writeLine(changed + "|" + elements[1]);
			}
			else
				writer.writeLine(elements[0] + "|" + changed);
			
			
			
			
				
			line=reader.readLine();
		}		
		
		reader.closeBuffer();
		writer.closeBuffer();
		
		}
		catch (Exception e){
			e.printStackTrace();
		}
				
		
	}
	
	
	Set<Set<Integer>> currentPlans = new HashSet<Set<Integer>>();
	Set<Integer> currentPlan = new HashSet<Integer>();
	
	private void getCandidateRepairPlans(int numElements, int sizePlan, int current_number, int level){
		
		if (numElements==sizePlan){
			for (int i=0; i<numElements; i++)
				currentPlan.add(i);
			currentPlans.add(new HashSet<Integer>(currentPlan));
			currentPlan.clear();
		}
		
		else {
			for (int i=current_number; i<numElements; i++){
				
				if (level==sizePlan-1){
					currentPlan.add(i);
					currentPlans.add(new HashSet<Integer>(currentPlan));
					currentPlan.remove(i);
				}
				else{
					currentPlan.add(i);
					getCandidateRepairPlans(numElements, sizePlan, i+1, level+1);
					currentPlan.remove(i);
				}
				
			}
		}
		
		//if (currentPlan.size()>=sizePlan) //Not necessary more recursivity for this plan
		//	currentPlan.clear();???
		
	}
	
	
	public static void main(String[] args) {
		
		String line="#Palabra";
		System.out.println(line.substring(0));
		System.out.println(line.substring(1));
		System.out.println(line.substring(2));
		
		String s = "thisIsMyString";
		String[] r = s.split("(?=\\p{Upper})");
		
		for (int i=0; i<r.length; i++){
			System.out.println("w " +r[i]);
		}
		
		System.out.println("");
		
		s = "Sss222Thisis333Mystring222";
		s = "Ssssssss";
		r = Utilities.splitStringByCapitalLetter(s);
		
		for (int i=0; i<r.length; i++){
			System.out.println("w " +r[i] + " " + r[i].length());
		}
		
		new Test(4, 2);
		new Test(4, 1);
		new Test(4, 3);
		new Test(4, 4);
		
		new Test(5, 1);
		new Test(5, 2);
		new Test(5, 3);
		new Test(5, 4);
		
		new Test(30, 5);
		
		System.out.println("1  "+ NormalizeNumbers.getRomanNormalization("1"));
		System.out.println("11  "+ NormalizeNumbers.getRomanNormalization("11"));
		System.out.println("kk  "+ NormalizeNumbers.getRomanNormalization("kaka"));
		System.out.println("v  "+ NormalizeNumbers.getRomanNormalization("v"));
		
		System.out.println("2nd  "+ NormalizeNumbers.getRomanNormalization("2nd"));
		
		System.out.println("three  "+ NormalizeNumbers.getRomanNormalization("three"));
		System.out.println("fourth  "+ NormalizeNumbers.getRomanNormalization("fourth"));
		
		
	}

}
