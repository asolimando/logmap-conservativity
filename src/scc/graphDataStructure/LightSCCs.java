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

import java.util.ArrayList;
import java.util.Collection;

public class LightSCCs extends ArrayList<LightSCC> {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2233833755484063484L;

	public LightSCCs(){
		super();
	}
	
	public LightSCCs clone() {
		LightSCCs e = new LightSCCs();
		for(LightSCC scc : this)
			e.add(scc.clone());
		return e;
	}
	
	@Override 
	public boolean equals(Object cmpSCCs) {
		if (this == cmpSCCs) 
			return true;

		if (!(cmpSCCs instanceof LightSCCs))
			return false;

		// we need to clone so we can safely delete elements later
		LightSCCs cmp = (LightSCCs) ((LightSCCs) cmpSCCs).clone();

		if(cmp.size() != this.size())
			return false;

		boolean toRet = true;
		
		for(int c = 0; c < this.size(); c++){
			LightSCC scc = this.get(c);
			if(cmp.isEmpty()){
				toRet = false;
				break;
			}
			
			if(cmp.contains(scc))
				cmp.remove(scc);
			else{
				toRet = false;
				break;
			}
		}
		return toRet;
	}
	
	public void printProblematicSCCs(LightAdjacencyList adj){
		for (LightSCC scc : this)
			scc.printProblematicSCC(adj);
	}
	
	public static void printProblematicSCCs(Collection<LightSCC> sccs, LightAdjacencyList adj){
		for (LightSCC scc : sccs)
			scc.printProblematicSCC(adj);
	}
}
