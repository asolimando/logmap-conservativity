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
package uk.ac.ox.krr.logmap2.indexing.labelling_schema;


/**
 * 
 * @author Anton Morant
 */
public class PostNode extends Node {

	private Interval descOrderInterval;
	private Interval ascOrderInterval;

	public PostNode(PostNode n){
		super(n);
		descOrderInterval = new Interval(n.descOrderInterval.leftbound, 
				n.descOrderInterval.rightbound);
		ascOrderInterval = new Interval(n.ascOrderInterval.leftbound, 
				n.ascOrderInterval.rightbound);
//		super(n.classId);
//
//		if(n.parents != null)
//			for (Node p : n.parents)
//				parents.add(new PostNode((PostNode) p));
//
//		if(n.children != null)
//			for (Node p : n.children)
//				children.add(new PostNode((PostNode) p));
//
//		if(n.descTreeChildren != null)
//			for (Node p : n.descTreeChildren)
//				descTreeChildren.add(new PostNode((PostNode) p));
//
//		if(n.ascTreeChildren != null)
//			for (Node p : n.ascTreeChildren)
//				ascTreeChildren.add(new PostNode((PostNode) p));
//
//		if(n.descIntervals != null)
//			for (Interval i : n.descIntervals)
//				descIntervals.add(new Interval(i.leftbound,i.rightbound));		
//
//		if(n.ascIntervals != null)
//			for (Interval i : n.ascIntervals)
//				ascIntervals.add(i);//new Interval(i.leftbound,i.rightbound));
//		
//		if(n.ascTreeParent != null)
//			ascTreeParent = new PostNode((PostNode) n.ascTreeParent);
//		
//		if(n.descTreeParent != null)
//			descTreeParent = new PostNode((PostNode) n.descTreeParent);
	}

	public PostNode(int classId) {
		super(classId);
		descOrderInterval = new Interval(-1,-1);
		descIntervals.add(descOrderInterval);
		ascOrderInterval = new Interval(-1,-1);
		ascIntervals.add(ascOrderInterval);
	}

	@Override
	public void setDescOrder(int postorder) {
		// Alessandro: this update affects the hashcode and would break HashSet
		descIntervals.remove(descOrderInterval);
		descOrderInterval.setRightBound(postorder);
		descIntervals.add(descOrderInterval);
	}

	@Override
	public void setDescChildOrder(int minPostorder) {
		// Alessandro: this update affects the hashcode and would break HashSet
		descIntervals.remove(descOrderInterval);
		descOrderInterval.setLeftBound(minPostorder);
		descIntervals.add(descOrderInterval);
	}

	@Override
	public int getDescOrder() {
		return descOrderInterval.getRightBound();
	}

	@Override
	public int getDescChildOrder() {
		return descOrderInterval.getLeftBound();
	}

	@Override
	public Interval getDescOrderInterval() {
		return descOrderInterval;
	}

	@Override
	public void setAscOrder(int postorder) {
		// Alessandro: this update affects the hashcode and would break HashSet
		ascIntervals.remove(ascOrderInterval);
		ascOrderInterval.setRightBound(postorder);
		ascIntervals.add(ascOrderInterval);
	}

	@Override
	public void setAscChildOrder(int minPostorder) {
		// Alessandro: this update affects the hashcode and would break HashSet
		ascIntervals.remove(ascOrderInterval);
		ascOrderInterval.setLeftBound(minPostorder);
		ascIntervals.add(ascOrderInterval);
	}

	@Override
	public int getAscOrder() {
		return ascOrderInterval.getRightBound();
	}

	@Override
	public int getAscChildOrder() {
		return ascOrderInterval.getLeftBound();
	}

	@Override
	public Interval getAscOrderInterval() {
		return ascOrderInterval;
	}

//	@Override
//	public int hashCode() {
//		final int prime = 31;
//		int result = super.hashCode();
//		result = prime
//				* result
//				+ ((ascOrderInterval == null) ? 0 : ascOrderInterval.hashCode());
//		result = prime
//				* result
//				+ ((descOrderInterval == null) ? 0 : descOrderInterval
//						.hashCode());
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
//		PostNode other = (PostNode) obj;
//		if (ascOrderInterval == null) {
//			if (other.ascOrderInterval != null)
//				return false;
//		} else if (!ascOrderInterval.equals(other.ascOrderInterval))
//			return false;
//		if (descOrderInterval == null) {
//			if (other.descOrderInterval != null)
//				return false;
//		} else if (!descOrderInterval.equals(other.descOrderInterval))
//			return false;
//		return true;
//	}	
}
