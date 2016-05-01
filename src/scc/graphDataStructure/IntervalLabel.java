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
package scc.graphDataStructure;

public class IntervalLabel {
	private int begin, end;

	public IntervalLabel(int begin, int end){
		sanityCheck(begin, end);
		this.begin = begin;
		this.end = end;
	}
	
	public String toString(){
		return "[" + begin + ", " + end + "]";
	}
	
	public int getBegin(){
		return begin;
	}
	
	public int getEnd(){
		return end;
	}
	
	public void setBegin(int begin){
		sanityCheck(begin, end);
		this.begin = begin;
	}
	
	public void setEnd(int end){
		sanityCheck(begin, end);
		this.end = end;
	}
	
	private void sanityCheck(int begin, int end){
		if(begin > end)
			throw new IllegalArgumentException(
					"Begin cannot be strictly greater than end: begin = " 
							+ begin + ", end = " + end);
	}
	
	// [b1,e1] \subsumes [b2,e2] iff b2 >= b1 && e2 <= e1
	public boolean subsumes(IntervalLabel l2){
		return l2.getBegin() >= begin && l2.getEnd() <= end;
	}
}
