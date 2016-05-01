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
package comparator;

import java.util.Comparator;

import uk.ac.ox.krr.logmap2.indexing.JointIndexManager;
import auxStructures.Pair;

public class ViolationComparatorSubClass extends AbstractViolationComparator {

	private JointIndexManager idx;
	
	public ViolationComparatorSubClass(JointIndexManager idx){
		this.idx = idx;
	}

	@Override
	public int compare(Pair<Integer> o1, Pair<Integer> o2) {
		int res = 0;
		Integer fst1 = o1.getFirst(), fst2 = o2.getFirst();
		
//		if(idx.areEquivalentClasses(fst1, fst2) || 
//				idx.areEquivalentClasses(fst2, fst1) || 
//				(idx.isSubClassOf(fst1, fst2) && 
//						idx.isSubClassOf(fst2, fst1)))
//			return 0;

		if(idx.areConceptsSharingDescendants(fst1, fst2))
			res = 0;
		else
			return Integer.compare(fst1, fst2);
		
//		if(idx.isSubClassOf(fst1, fst2))
//			res = 1;
//		else if(idx.isSubClassOf(fst2, fst1))
//			res = -1;
		
		return res;
	}
}