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
package auxStructures;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import enumerations.VIOL_KIND;

import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;

public class RepairStatus {
	private Set<MappingObjectStr> repair;
	private Map<VIOL_KIND,Integer> violMap;
	private Set<MappingObjectStr> repairedMappings;
	
	public RepairStatus(){
		init();
		repair = new HashSet<>();
	}
	
	public RepairStatus(boolean hasViolations, Set<MappingObjectStr> repair){
		init();
		this.repair = new HashSet<>(repair);
	}
	
	public void setViolations(VIOL_KIND kind, int numViols){
		violMap.put(kind, numViols);
	}

	public int getViolationsNumber(){
		 return getViolationsNumber(Arrays.asList(VIOL_KIND.values()));
	}
	
	public int getViolationsNumber(Collection<VIOL_KIND> violKinds){
		 int violNum = 0;
		 
		 for (VIOL_KIND vk : violKinds)
			violNum += violMap.get(vk);
		 
		 return violNum;
	}

	public boolean hasViolations(){
		return getViolationsNumber() > 0;
	}
	
	public boolean hasViolations(Collection<VIOL_KIND> violKinds){
		return getViolationsNumber(violKinds) > 0;
	}
	
	private void init(){
		violMap = new HashMap<>(VIOL_KIND.values().length);
		
		for (VIOL_KIND kind : VIOL_KIND.values())
			violMap.put(kind, 0);
	}
	
	public Set<MappingObjectStr> getRepair(){
		return Collections.unmodifiableSet(repair);
	}
	
	public void setRepair(Set<MappingObjectStr> repair){
		this.repair = new HashSet<>(repair);
	}

	public void setRepairedMappings(Set<MappingObjectStr> repairedMappings) {
		this.repairedMappings = new HashSet<>(repairedMappings);
	}
	
	public Set<MappingObjectStr> getRepairedMappings() {
		if(repairedMappings == null)
			return Collections.emptySet();
		return repairedMappings;
	}

	public boolean hasValidRepair() {
		return repair != null && !repair.isEmpty();
	}
}
