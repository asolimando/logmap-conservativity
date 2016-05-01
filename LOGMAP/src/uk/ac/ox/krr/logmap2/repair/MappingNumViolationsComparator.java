package uk.ac.ox.krr.logmap2.repair;

import java.util.Comparator;
import java.util.Map;

import uk.ac.ox.krr.logmap2.repair.hornSAT.HornClause;

public class MappingNumViolationsComparator implements Comparator<HornClause> {

	private Map<HornClause,Integer> mapping2NumUnsatisfiabilities;
	
	public MappingNumViolationsComparator(
			Map<HornClause,Integer> mapping2NumUnsatisfiabilities){
		this.mapping2NumUnsatisfiabilities = mapping2NumUnsatisfiabilities;
	}

	@Override
	public int compare(HornClause h1, HornClause h2) {
		int v1 = mapping2NumUnsatisfiabilities.containsKey(h1) 
				? mapping2NumUnsatisfiabilities.get(h1) : 0;
		int v2 = mapping2NumUnsatisfiabilities.containsKey(h2) 
				? mapping2NumUnsatisfiabilities.get(h2) : 0;
		return Integer.compare(v2, v1);
	}
}