package uk.ac.ox.krr.logmap2.indexing.labelling_schema;

public class IntervalForNode extends Interval {

	private int nodeId;
	
	public IntervalForNode(int leftb, int rightb) {
		super(leftb, rightb);
		this.nodeId = -2;
	}
	
	public IntervalForNode(int leftb, int rightb, int nodeId) {
		super(leftb, rightb);
		this.nodeId = nodeId;
	}

	public int getNodeId(){
		return nodeId;
	}
}
