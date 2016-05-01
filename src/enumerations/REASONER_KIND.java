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
package enumerations;

import org.semanticweb.owlapi.reasoner.OWLReasoner;

import util.OntoUtil;
import util.Params;

public enum REASONER_KIND {
	KONCLUDE("Konclude Reasoner"),
	FACT("FaCT++ Reasoner"),
	PELLET("Pellet Reasoner"), 
	HERMIT("HermiT Reasoner"), 
	ELK("ELK Reasoner"),
	STRUCTURAL("Structural Reasoner"),
	ELKTRACE("ELK Reasoner with Tracing"),
//	MORE("MORe Reasoner"),
//	CHAINSAW("Chainsaw Reasoner"),
	UNKNOWN("Unknown Reasoner")
	;
	
	private String descr;
	
	private REASONER_KIND(String descr){
		this.descr = descr;
	}
	
	public String toString(){
		return descr;
	}
	
	public static REASONER_KIND getKind(OWLReasoner r){
		if(OntoUtil.isELKReasoner(r))
			return ELK;
		
		for (REASONER_KIND k : REASONER_KIND.values()){
			if(Params.oaei){
				if(k.getClass().getName().contains(
							k.descr.split(" ")[0].toLowerCase())){
					return k;
				}
			}
			else {
				if(r.getReasonerName().toLowerCase().contains(
						k.descr.split(" ")[0].toLowerCase())){
					return k;
				}
			}
		}
		
		return UNKNOWN;
	}
}
