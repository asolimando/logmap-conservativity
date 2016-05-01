package uk.ac.ox.krr.logmap2.indexing.labelling_schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The Node class contains the interval tree information for one single node
 * Adapted from https://github.com/kevinjdolan/intervaltree
 * @author Kevin Dolan (original author)
 */
public class IntervalNode {

	private SortedMap<IntervalForNode, List<IntervalForNode>> intervals;
	private int center;
	private IntervalNode leftNode;
	private IntervalNode rightNode;

	public IntervalNode() {
		intervals = new TreeMap<>();
		center = 0;
		leftNode = null;
		rightNode = null;
	}

	public IntervalNode(List<IntervalForNode> intervalList) {

		intervals = new TreeMap<>();

		SortedSet<Integer> endpoints = new TreeSet<>();

		for(IntervalForNode interval: intervalList) {
			endpoints.add(interval.getLeftBound());
			endpoints.add(interval.getRightBound());
		}

		int median = getMedian(endpoints);
		center = median;

		List<IntervalForNode> left = new ArrayList<>();
		List<IntervalForNode> right = new ArrayList<>();

		for(IntervalForNode interval : intervalList) {
			if(interval.getRightBound() < median)
				left.add(interval);
			else if(interval.getLeftBound() > median)
				right.add(interval);
			else {
				List<IntervalForNode> posting = intervals.get(interval);
				if(posting == null) {
					posting = new ArrayList<>();
					intervals.put(interval, posting);
				}
				posting.add(interval);
			}
		}

		if(left.size() > 0)
			leftNode = new IntervalNode(left);
		if(right.size() > 0)
			rightNode = new IntervalNode(right);
	}

	/**
	 * Perform a stabbing query on the node
	 * @param index the value to query at
	 * @return	   all intervals containing time
	 */
	public List<IntervalForNode> stab(int index) {		
		List<IntervalForNode> result = new ArrayList<>();

		for(Entry<IntervalForNode, List<IntervalForNode>> entry : intervals.entrySet()) {
			if(entry.getKey().containsIndex(index))
				for(IntervalForNode interval : entry.getValue())
					result.add(interval);
			else if(entry.getKey().getLeftBound() > index)
				break;
		}

		if(index < center && leftNode != null)
			result.addAll(leftNode.stab(index));
		else if(index > center && rightNode != null)
			result.addAll(rightNode.stab(index));
		return result;
	}

	/**
	 * Perform an interval intersection query on the node
	 * @param target the interval to intersect
	 * @return		   all intervals containing time
	 */
	public List<IntervalForNode> query(IntervalForNode target) {
		List<IntervalForNode> result = new ArrayList<>();

		for(Entry<IntervalForNode, List<IntervalForNode>> entry : intervals.entrySet()) {
			if(entry.getKey().hasNonEmptyIntersectionWith(target))
				for(IntervalForNode interval : entry.getValue())
					result.add(interval);
			else if(entry.getKey().getLeftBound() > target.getRightBound())
				break;
		}

		if(target.getLeftBound() < center && leftNode != null)
			result.addAll(leftNode.query(target));
		if(target.getRightBound() > center && rightNode != null)
			result.addAll(rightNode.query(target));
		return result;
	}

	public int getCenter() {
		return center;
	}

	public void setCenter(int center) {
		this.center = center;
	}

	public IntervalNode getLeft() {
		return leftNode;
	}

	public void setLeft(IntervalNode left) {
		this.leftNode = left;
	}

	public IntervalNode getRight() {
		return rightNode;
	}

	public void setRight(IntervalNode right) {
		this.rightNode = right;
	}

	/**
	 * @param set the set to look on
	 * @return	  the median of the set, not interpolated
	 */
	private Integer getMedian(SortedSet<Integer> set) {
		int i = 0;
		int middle = set.size() / 2;
		for(Integer point : set) {
			if(i == middle)
				return point;
			i++;
		}
		return null;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(center + ": ");
		for(Entry<IntervalForNode, List<IntervalForNode>> entry : intervals.entrySet()) {
			sb.append("[" + entry.getKey().getLeftBound() + "," + entry.getKey().getRightBound() + "]:{");
			for(IntervalForNode interval : entry.getValue()) {
				sb.append("("+interval.getLeftBound()+","+interval.getRightBound()+")");
			}
			sb.append("} ");
		}
		return sb.toString();
	}
}
