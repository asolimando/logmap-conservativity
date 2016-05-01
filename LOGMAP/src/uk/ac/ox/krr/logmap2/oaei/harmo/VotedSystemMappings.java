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
package uk.ac.ox.krr.logmap2.oaei.harmo;

import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import uk.ac.ox.krr.logmap2.io.ReadFile;


public class VotedSystemMappings {
	
	Map<String, Map<String, Double>> mappings2weight = new HashMap<String, Map<String, Double>>();
	

	public VotedSystemMappings(){
		
		//No properties
		
	}
	
	
	public void addMappings(String pathMappings, double weight){
		
		try {
			ReadFile reader = new ReadFile(pathMappings);
			
			String line;
			
			String[] elements;
			
			while ((line = reader.readLine()) != null){
				
				if (line.contains("|")){
					elements=line.split("\\|");
					
					addMapping(elements[0], elements[1], weight);
					
				}	
			}
			
			reader.closeBuffer();
		}
		catch (Exception e){
			e.printStackTrace();
		}
		
	}
	
	
	private void addMapping(String uri1, String uri2, double weight){
		
		if (!mappings2weight.containsKey(uri1)){
			mappings2weight.put(uri1, new HashMap<String, Double>());
		}
		
			
		if (mappings2weight.get(uri1).containsKey(uri2)){
				
			//We keep the highest
			if (weight > mappings2weight.get(uri1).get(uri2)){
					
				mappings2weight.get(uri1).put(uri2, weight);//replace previous value
				
			}	
		}
		else{
			//No entry for uri2
			mappings2weight.get(uri1).put(uri2, weight);
		}
				
	}
	
	
	
	private double getWeightMapping(String uri1, String uri2){
		
		//return 0 if mapping does not exist
		
		if (mappings2weight.containsKey(uri1)){
			if (mappings2weight.get(uri1).containsKey(uri2)){
				
				return mappings2weight.get(uri1).get(uri2);
			
			}
			else{
				return 0.0;
			}
		}
		else {
			return 0.0;
		}
		
	}
	
	
	public Map<String, Map<String, Double>> getMappings2Weight(){
		return mappings2weight;
	}
	
	
	
	
}
