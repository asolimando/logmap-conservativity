package uk.ac.ox.krr.logmap2.indexing.labelling_schema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
* Adapted from https://github.com/kevinjdolan/intervaltree
* @author Kevin Dolan (original author)
*/
public class IntervalTree {

	private IntervalNode head;
	private List<IntervalForNode> intervalList;
	private boolean inSync;
	private int size;

	/**
	 * Instantiate a new interval tree with no intervals
	 */
	public IntervalTree() {
		this.head = new IntervalNode();
		this.intervalList = new ArrayList<>();
		this.inSync = true;
		this.size = 0;
	}

	/**
	 * Instantiate and build an interval tree with a preset list of intervals
	 * @param intervalList the list of intervals to use
	 */
	public IntervalTree(List<IntervalForNode> intervalList) {
		this.head = new IntervalNode(intervalList);
		this.intervalList = new ArrayList<>();
		this.intervalList.addAll(intervalList);
		this.inSync = true;
		this.size = intervalList.size();
	}

	public IntervalTree(IntervalTree tree){
		this.head = new IntervalNode(tree.intervalList);
		this.intervalList = new ArrayList<>();
		this.intervalList.addAll(this.intervalList);
		this.inSync = tree.inSync;
		this.size = this.intervalList.size();
	}
	
	public List<IntervalForNode> getIntervalList(){
		return intervalList;
	}
	
	/**
	 * Perform a stabbing query, returning the associated data
	 * Will rebuild the tree if out of sync
	 * @param time the time to stab
	 * @return	   the data associated with all intervals that contain time
	 */
	public List<Integer> get(int id) {
		List<IntervalForNode> intervals = getIntervals(id);
		List<Integer> result = new ArrayList<>();
		for(IntervalForNode interval : intervals)
			result.add(interval.getNodeId());
		return result;
	}

	/**
	 * Perform a stabbing query, returning the interval objects
	 * Will rebuild the tree if out of sync
	 * @param index the index to stab
	 * @return	   all intervals that contain index
	 */
	public List<IntervalForNode> getIntervals(int index) {
		build();
		return head.stab(index);
	}

	public boolean hasIntervalsContaining(int index){
		return !getIntervals(index).isEmpty();
	}
	
	public boolean hasIntervalsContaining(int start, int end){
		return !getIntervals(start,end).isEmpty();
	}
	
	public boolean hasIntervalsContaining(IntervalForNode i){
		return !getIntervals(i).isEmpty();
	}
	
	/**
	 * Perform an interval query, returning the associated data
	 * Will rebuild the tree if out of sync
	 * @param start the start of the interval to check
	 * @param end	the end of the interval to check
	 * @return	  	the data associated with all intervals that intersect target
	 */
	public List get(int start, int end) {
		List<IntervalForNode> intervals = getIntervals(start, end);
		List<Integer> result = new ArrayList<>();
		for(IntervalForNode interval : intervals)
			result.add(interval.getNodeId());
		return result;
	}

	/**
	 * Perform an interval query, returning the interval objects
	 * Will rebuild the tree if out of sync
	 * @param start the start of the interval to check
	 * @param end	the end of the interval to check
	 * @return	  	all intervals that intersect target
	 */
	public List<IntervalForNode> getIntervals(int start, int end) {
		build();
		return head.query(new IntervalForNode(start, end));
	}
	
	public List<IntervalForNode> getIntervals(IntervalForNode i) {
		build();
		return head.query(i);
	}

	/**
	 * Add an interval object to the interval tree's list
	 * Will not rebuild the tree until the next query or call to build
	 * @param interval the interval object to add
	 */
	public void addInterval(IntervalForNode interval) {
		intervalList.add(interval);
		inSync = false;
	}

	/**
	 * Add a collection of intervals to the interval tree's list
	 * Will not rebuild the tree until the next query or call to build
	 * @param interval the interval object to add
	 */
	public void addIntervals(Collection<? extends IntervalForNode> intervals) {
		for (IntervalForNode interval : intervals)
			intervalList.add(interval);
		inSync = false;
	}
	
	/**
	 * Add an interval object to the interval tree's list
	 * Will not rebuild the tree until the next query or call to build
	 * @param begin the beginning of the interval
	 * @param end	the end of the interval
	 * @param data	the data to associate
	 */
	public void addInterval(int begin, int end, int nodeId) {
		intervalList.add(new IntervalForNode(begin, end, nodeId));
		inSync = false;
	}

	/**
	 * Determine whether this interval tree is currently a reflection of all intervals in the interval list
	 * @return true if no changes have been made since the last build
	 */
	public boolean inSync() {
		return inSync;
	}

	/**
	 * Build the interval tree to reflect the list of intervals,
	 * Will not run if this is currently in sync
	 */
	public void build() {
		if(!inSync) {
			head = new IntervalNode(intervalList);
			inSync = true;
			size = intervalList.size();
		}
	}

	/**
	 * @return the number of entries in the currently built interval tree
	 */
	public int currentSize() {
		return size;
	}

	/**
	 * @return the number of entries in the interval list, equal to .size() if inSync()
	 */
	public int listSize() {
		return intervalList.size();
	}

	@Override
	public String toString() {
		return nodeString(head,0);
	}

	private String nodeString(IntervalNode node, int level) {		
		if(node == null)
			return "";

		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < level; i++)
			sb.append("\t");
		sb.append(node + "\n");
		sb.append(nodeString(node.getLeft(), level + 1));
		sb.append(nodeString(node.getRight(), level + 1));
		return sb.toString();
	}
}