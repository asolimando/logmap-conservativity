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
package uk.ac.ox.krr.logmap2.SIAssessment;

import java.util.HashSet;
import java.util.Set;

import uk.ac.ox.krr.logmap2.indexing.IndexManager;
import uk.ac.ox.krr.logmap2.mappings.MappingManager;

public class InstanceMatchingAssessment {
	
	
	protected IndexManager index;
	protected MappingManager mapping_manager;
	
	
	protected final int EMPTY_TYPES=0;
	protected final int ONE_TYPE_EMPTY=1;
	
	protected final int COMPATIBLE_TYPES=2;
	protected final int INCOMPATIBLE_TYPES=3;
	
	protected final int SAME_TYPES=4;
	protected final int SUB_TYPES=5;
	
	protected int compatibility=0;
	
	public InstanceMatchingAssessment(IndexManager index, MappingManager mapping_manager){
		
		this.index = index;
		this.mapping_manager = mapping_manager;
		
	}
	
	
	
	protected int areInstancesCompatible(int ident1, int ident2){
	
		Set<Integer> types1 = index.getIndividualClassTypes4Identifier(ident1);
		Set<Integer> types2 = index.getIndividualClassTypes4Identifier(ident2);
		
		Set<Integer> mapped_types1=new HashSet<Integer>();
		
		if (types1.isEmpty() && types2.isEmpty()){
			return EMPTY_TYPES;
		}
		if (types1.isEmpty() || types2.isEmpty()){
			return ONE_TYPE_EMPTY;
		}
		
		
		
		for (int cls1 : types1){
			if (mapping_manager.getAnchors().containsKey(cls1)){
				mapped_types1.addAll(mapping_manager.getAnchors().get(cls1));
			}
		}
		
		if (haveSameClassTypes(mapped_types1, types2)){
			return SAME_TYPES;
		}
		
		for (int cls1 : types1){
			for (int cls2 : types2){
				if (mapping_manager.isMappingInConflictWithFixedMappings(cls1, cls2)){
					return INCOMPATIBLE_TYPES;
				}
			}	
		}
		
		
		//Subtypes
		if (areAllSubTypes(types1, types2))
			return SUB_TYPES;
		
		
		
		
		return COMPATIBLE_TYPES; 
		
		
		
		
	}
	
	
	public double getConfidence4Compatibility(int ident1, int ident2){
		
		compatibility = areInstancesCompatible(ident1, ident2);
		
		switch (compatibility) {
	    	case EMPTY_TYPES:
	    		return 0.90; //0.85
	    	case ONE_TYPE_EMPTY:
	    		return 0.90;
	    	case SAME_TYPES:
	    		return 0.75;//0.75
	    	case SUB_TYPES:
	    		return 0.80;
	    	case COMPATIBLE_TYPES:
	    		return 0.85;	
	    	case INCOMPATIBLE_TYPES:
	    		return 2.0; 
	    	default:
	    		return 2.0;
		}
	}
	
	
	public double getCompatibilityFactor(int ident1, int ident2){
		
		//compatibility has been set when calling the above method
		
		switch (compatibility) {
	    	case EMPTY_TYPES:
	    		return 0.50;
	    	case ONE_TYPE_EMPTY:
	    		return 0.50;
	    	case SAME_TYPES:
	    		return 1.0;
	    	case SUB_TYPES:
	    		return 0.90;
	    	case COMPATIBLE_TYPES:
	    		return 0.70;	
	    	case INCOMPATIBLE_TYPES:
	    		return 0.0; //no compatible 
	    	default:
	    		return 0.0;
		}
	}
	
	
	protected boolean haveSameClassTypes(Set<Integer> types1, Set<Integer> types2){
		
		if (types1.size()>0 && types2.size()>0){			
			return types1.equals(types2);
		}
		
		return false;	
	}
	
	
	protected boolean areAllSubTypes(Set<Integer> types1, Set<Integer> types2){
		
		for (int cls1 : types1){
			for (int cls2 : types2){
				if (!index.isSubClassOf(cls1, cls2) && !index.isSubClassOf(cls2, cls1))
					return false;
			}
		}
		
		return true;	
	}
	


}
