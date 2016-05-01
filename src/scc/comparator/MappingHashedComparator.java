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
package scc.comparator;

import scc.graphDataStructure.MappingHashed;
import java.util.Comparator;

public class MappingHashedComparator implements Comparator<MappingHashed> {

	private boolean source;

	public MappingHashedComparator(boolean source){
		this.source = source;
	}

	@Override
	public int compare(MappingHashed a, MappingHashed b) {

		int res = source ? Integer.compare(a.src, b.src) 
				: Integer.compare(a.dest, b.dest);
		
//		if(res == 0)
//			res = source ? Integer.compare(a.dest, b.dest) 
//					: Integer.compare(a.src, b.src);
		if(res == 0)
			res = Double.compare(b.mapping.confidence, a.mapping.confidence);
		
		return res;
	}
}
