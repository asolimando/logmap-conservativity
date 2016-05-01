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
package uk.ac.ox.krr.logmap2.oaei.eval;

import uk.ac.ox.krr.logmap2.io.*;

import uk.ac.ox.krr.logmap2.lexicon.LexicalUtilitiesStatic;
import uk.ac.ox.krr.logmap2.mappings.I_Sub;
import uk.ac.ox.krr.logmap2.utilities.Utilities;

import java.util.*;


public class MultiFarmTest {

	ReadFile reader;
	
	Set<String> ent1 = new HashSet<String>();
	Set<String> ent2 = new HashSet<String>();
	Map<String,String> gs = new HashMap<String, String>();
	
	
	private double average_isub;
	
	I_Sub isub = new I_Sub();
	
	double isub_score;
	
	public MultiFarmTest() throws Exception{
		
		
		String path = "/home/ernesto/OM_OAEI/MultiFarm/";
		
		
		//readMultiFarmFile(path + "cmt_labels_cz.txt");
		
		readMultiFarmFile(path + "cmt_labels_de.txt");
		readMultiFarmFile(path + "cmt_labels_es.txt");
		readMultiFarmFile(path + "cmt_labels_fr.txt");
		readMultiFarmFile(path + "cmt_labels_nl.txt");
		readMultiFarmFile(path + "cmt_labels_pt.txt");
		
	}
	
	

	private void readMultiFarmFile(String file) throws Exception{
		
		System.out.println("\n\n" + file);
		
		reader = new ReadFile(file);
		
		average_isub = 0.0;
		gs.clear();
		ent1.clear();
		ent2.clear();
		
		int num_entries=0;
		
		
		
		
		String line;
		String[] elements;
		
		line=reader.readLine();
		
		
		while (line!=null) {
			
			if (line.indexOf("\t")<0){
				line=reader.readLine();
				continue;
			}
			
			elements=line.split("\t");
			
			Utilities.splitStringByCapitalLetter(elements[0]);
			Utilities.splitStringByCapitalLetter(elements[0]);
			
			//System.out.println(elements[0] + " - " +elements[1]);
			
			isub_score = getIsub(cleanLabel(elements[0]), cleanLabel(elements[1]));
			
			ent1.add(cleanLabel(elements[0]));
			ent2.add(cleanLabel(elements[1]));
			gs.put(cleanLabel(elements[0]), cleanLabel(elements[1]));
			
			if (isub_score>0.60){
				average_isub+=isub_score;
				num_entries++;
			}
			
				
			/*System.out.println(
					cleanLabel(elements[0]) + " - " + 
							cleanLabel(elements[1]) + " - " + 
							isub_score);
			*/
				
			line=reader.readLine();
		}		
		
		
		System.out.println("\nAverage: " + average_isub/(double)num_entries);
		
		
		reader.closeBuffer();

		
		for (String str1: ent1){
			for (String str2: ent2){
		
				isub_score = getIsub(str1,str2);
				
				if (isub_score>0.40){
					if (inGS(str1, str2)){
						
						System.out.println("GOOD" +
								str1 + " - " + str2 + " - " +isub_score);
					}
					else {
						System.out.println("BADD" +
								str1 + " - " + str2 + " - " +isub_score);
					}
				}
				
			}
			
		}
		
	}
	
	
	private boolean inGS(String str1, String str2){
		
		if (gs.containsKey(str1)){
			if (gs.get(str1).equals(str2))
				return true;
		}
		return false;
		
	}
	
	
	public String cleanLabel(String label){
		
		String[] words;
		label=label.replace(",", "");
		label=label.replace("-", "");
		
		if (label.indexOf("_")>0){ //NCI and SNOMED
			words=label.split("_");
		}
		else if (label.indexOf(" ")>0){ //FMA
			words=label.split(" ");
		}
		//Split capitals...
		else{
			//label_value = Utilities.capitalPrepositions(label_value);
			words=Utilities.splitStringByCapitalLetter(label);
		}

		label="";
		
		for (int i=0; i<words.length; i++){
			
			words[i]=words[i].toLowerCase(); //to lower case
			
			if (words[i].length()>0){
			
				
				if (!LexicalUtilitiesStatic.getStopwordsSet().contains(words[i])){ 
					//For label
					label+=words[i] + "_";
					
				}				
			}			
		}
		
		label = label.substring(0, label.length()-1);
		
				
		return label;

		
		
		
	}
	
	
	public double getIsub(String str1, String str2){
		
		double isub_score = isub.score(str1, str2);
		
		if (isub_score<0){ //Do not return negative isubs...
			isub_score=0.0;
		}
		
		return isub_score;
		
	}

		
	
	public static void main(String[] args) {
		try{
			new MultiFarmTest();
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}
	
	
	
}
