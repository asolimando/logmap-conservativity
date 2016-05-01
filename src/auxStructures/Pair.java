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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import uk.ac.ox.krr.logmap2.indexing.JointIndexManager;

public class Pair<T> {
	
	private T first, second;
	
	public Pair(T first, T second){
		this.first = first;
		this.second = second;
	}
	
	public T getFirst(){
		return first;
	}
	
	public T getSecond(){
		return second;
	}
	
	// do not use if Pair is in hash-based data structure!
	public void setFirst(T first){
		this.first = first;
	}
	public void setSecond(T second){
		this.second = second;
	}

	public Set<T> asSet(){
		Set<T> ret = new HashSet<>();
		ret.add(first);
		ret.add(second);
		return ret;
	}
	
	public List<T> asList(){
		List<T> ret = new ArrayList<>(2);
		ret.add(first);
		ret.add(second);
		return ret;		
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((first == null) ? 0 : first.hashCode());
		result = prime * result + ((second == null) ? 0 : second.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Pair other = (Pair) obj;
		if (first == null) {
			if (other.first != null)
				return false;
		} else if (!first.equals(other.first))
			return false;
		if (second == null) {
			if (other.second != null)
				return false;
		} else if (!second.equals(other.second))
			return false;
		return true;
	}

	@Override
	public String toString(){
		return "<" + first + ", " + second + ">"; 
	}
	
	public boolean subsumes(Pair pair, JointIndexManager idx){
		return idx.isSubClassOf(((Integer) first), ((Integer) pair.getFirst())) 
				&& idx.isSubClassOf(((Integer) pair.getSecond()), ((Integer) second));
	}
}
