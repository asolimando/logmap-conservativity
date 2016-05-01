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
package scc.mapping;

import java.net.URI;

import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owl.align.Relation;

import fr.inrialpes.exmo.align.impl.ObjectCell;
import fr.inrialpes.exmo.align.impl.URICell;
import fr.inrialpes.exmo.align.impl.rel.EquivRelation;
import fr.inrialpes.exmo.align.impl.rel.SubsumeRelation;
import fr.inrialpes.exmo.align.impl.rel.SubsumedRelation;

public class LightCell extends URICell {

	private static int c;
	
	public LightCell(String arg0, URI arg1, URI arg2, Relation arg3,
			double arg4) throws AlignmentException {
		super(arg0, arg1, arg2, arg3, arg4);
	}
	
	public LightCell(URI arg1, URI arg2, Relation arg3,
			double arg4) throws AlignmentException {
		super(c++ + "", arg1, arg2, arg3, arg4);
	}
	
	public LightCell(Cell c) throws AlignmentException {
		super(c.getId(), c.getObject1AsURI(), c.getObject2AsURI(), 
				c.getRelation(), c.getStrength());
	}
	
	@Override
	public int compareTo(Cell c2){
		return Integer.compare(hashCode(), c2.hashCode());
	}
	
	@Override
	public boolean equals(Object object){
		if(object == null)
			return false;
		if (object == this) 
			return true;
		if(!getClass().equals(object.getClass()))
			return false;
		LightCell c2 = (LightCell) object;
		boolean res = this.getObject1().equals(c2.getObject1()) && 
					this.getObject2().equals(c2.getObject2()) &&
					this.getRelation().equals(c2.getRelation());
					//&& this.getStrength() == c2.getStrength();
		
		if(!res)
			System.out.println();
		
		return res;
	}

	@Override
	public int hashCode(){
		int hash = 5;
		hash = 37 * hash + (getObject1() != null ? getObject1().hashCode() : 0);
		hash = 37 * hash + (getObject2() != null ? getObject2().hashCode() : 0);
		hash = 37 * hash + (getRelation() != null ? getRelation().hashCode() : 0);		
//		hash = 37 * hash + new Double(getStrength()).hashCode();
		return hash;
	}
	
	@Override
	public String toString(){
		String res = null, rel = null;
		Relation relation = this.getRelation();

		if(relation instanceof SubsumedRelation)
			rel = "<";
		else if(relation instanceof SubsumeRelation)
			rel = ">";
		else if(relation instanceof EquivRelation)
			rel = "=";
		else
			throw new Error("Unknown relation: " + rel);

		try {
			res = this.getObject1AsURI() + " " + rel + " (" 
					+ this.getStrength() + ") " + this.getObject2AsURI();
		} catch (AlignmentException e) {
			e.printStackTrace();
		}
		return res;
	}
}
