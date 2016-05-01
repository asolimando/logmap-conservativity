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
package uk.ac.ox.krr.logmap2.indexing.entities;

import java.util.Set;

import java.util.HashSet;


public class DataPropertyIndex extends PropertyIndex {

	Set<String> range = new HashSet<String>();
	
	public DataPropertyIndex(int index){
		super(index, PropertyIndex.DATAPROPERTY);
	}
	
	public DataPropertyIndex(DataPropertyIndex idx) {
		super((PropertyIndex) idx);
		range = new HashSet<String>(idx.range);
	}

	public Set<String> getRangeTypes(){
		return range;
	}
	
	
	public void addRangeType(String dtype){
		range.add(dtype);
	}

//	@Override
//	public int hashCode() {
//		final int prime = 31;
//		int result = super.hashCode();
//		result = prime * result + ((range == null) ? 0 : range.hashCode());
//		return result;
//	}
//
//	@Override
//	public boolean equals(Object obj) {
//		if (this == obj)
//			return true;
//		if (!super.equals(obj))
//			return false;
//		if (getClass() != obj.getClass())
//			return false;
//		DataPropertyIndex other = (DataPropertyIndex) obj;
//		if (range == null) {
//			if (other.range != null)
//				return false;
//		} else if (!range.equals(other.range))
//			return false;
//		return true;
//	}
//	
}
