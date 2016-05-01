package uk.ac.ox.krr.logmap2.repair;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import uk.ac.ox.krr.logmap2.indexing.IndexManager;
import uk.ac.ox.krr.logmap2.repair.hornSAT.DowlingGallierHornSAT;
import uk.ac.ox.krr.logmap2.repair.hornSAT.HornClause;

public class DandGCounterThread implements Callable<Set<Integer>> {
	
	private boolean satisfiable = false, useProjection;
	private Set<Integer> allSubClasses = new HashSet<>();
	private Set<Integer> SATvisited, unSATvisited;
	private Map<HornClause,Integer> mapping2NumUnsatisfiabilities;
	private Integer cls;
	private DowlingGallierHornSAT dgSat;
	private IndexManager index;
	
	public DandGCounterThread(
			Map<HornClause, Integer> mapping2NumUnsatisfiabilities,
			Set<Integer> sATvisited, Set<Integer> unSATvisited, int cls,
			IndexManager index, boolean useProjection,
			DowlingGallierHornSAT dgSat) {
		this.mapping2NumUnsatisfiabilities = mapping2NumUnsatisfiabilities;
		this.SATvisited = sATvisited;
		this.unSATvisited = unSATvisited;
		this.cls = cls;
		this.index = index;
		this.useProjection = useProjection;
		this.dgSat = new DowlingGallierHornSAT(dgSat);
	}

	@Override
	public Set<Integer> call() {
		
		//Already visited
//		synchronized(unSATvisited){
//			synchronized(SATvisited){
				if (unSATvisited.contains(cls) || SATvisited.contains(cls)) 
					return Collections.emptySet();
//			}
//		}

		satisfiable=dgSat.isSatisfiable(cls);				

		if (satisfiable){
//			synchronized(SATvisited){
				SATvisited.add(cls);
//			}
			// new threads will be called for these classes
			return index.getDirectSubClasses4Identifier(cls, useProjection);
		}
		else { //UNSAT
			
			getSubclasses4Identifiers(index.getDirectSubClasses4Identifier(cls, useProjection));
	
			//Alessandro: extension for additional map -- START
			for (HornClause clause : dgSat.getMappingsInvolvedInError()) {
//				synchronized(mapping2NumUnsatisfiabilities){
					if(mapping2NumUnsatisfiabilities.containsKey(clause)){
						int updatedVal = 1+mapping2NumUnsatisfiabilities.get(clause)+allSubClasses.size();
						mapping2NumUnsatisfiabilities.put(clause, updatedVal);
					}
					else
						mapping2NumUnsatisfiabilities.put(clause, 
								1+allSubClasses.size());				
//				}
			}
			// -- END
			
//			synchronized(unSATvisited){
				unSATvisited.add(cls);
				unSATvisited.addAll(allSubClasses);
//			}

			return Collections.emptySet();
		}
	}
	
	private void getSubclasses4Identifiers(Set<Integer> classes){
		
		allSubClasses.addAll(classes);
		
		if (classes.size()<1) //exit condition
			return;
		
		Set<Integer> subClasses = new HashSet<Integer>();
		
		for (int ide : classes){
			subClasses.addAll(index.getDirectSubClasses4Identifier(ide, false));
		}
		
		getSubclasses4Identifiers(subClasses);		
	}
}