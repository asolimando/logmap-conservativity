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
import java.util.Set;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Cell;

import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.rel.EquivRelation;
import fr.inrialpes.exmo.align.impl.rel.SubsumeRelation;
import fr.inrialpes.exmo.align.impl.rel.SubsumedRelation;

public class LightAlignment extends URIAlignment {

	public LightAlignment(){
		super();
	}

	@Override
	public void addCell(Cell c) throws AlignmentException{
		super.addCell(c);
	}
	
	public Cell addAlignCell(URI o1, URI o2, String rel, double conf) throws AlignmentException{
		Cell c;
		addCell(c = new LightCell(o1,o2,SubsumedRelation.createRelation(rel),conf));
		return c;
	}
	
	public LightAlignment(Alignment align){
		super();
		if(align != null){

			for (Cell cell : align) {
				try {
					if(cell.getRelation() instanceof EquivRelation){
						this.addAlignCell(cell.getObject1AsURI(), 
								cell.getObject2AsURI(), "<", cell.getStrength());
						this.addAlignCell(cell.getObject2AsURI(),
								cell.getObject1AsURI(), "<", cell.getStrength());
					}
					else if(cell.getRelation() instanceof SubsumedRelation){
						this.addAlignCell(cell.getObject2AsURI(),
								cell.getObject1AsURI(), "<", cell.getStrength());
					}
					else if(cell.getRelation() instanceof SubsumeRelation){
						this.addAlignCell(cell.getObject1AsURI(),
								cell.getObject2AsURI(), "<", cell.getStrength());
					}
					else
						throw new Error("Unknown relation: " + cell);
//					else
//						this.addCell(cell);
				} catch (AlignmentException e) {
					e.printStackTrace();
				}
			}
		}
	}

//	public String printCell(Cell c){
//		String res = null, rel = null;
//		Relation relation = c.getRelation();
//
//		if(relation instanceof SubsumedRelation)
//			rel = "<";
//		else if(relation instanceof SubsumeRelation)
//			rel = ">";
//		else if(relation instanceof EquivRelation)
//			rel = "=";
//		else
//			throw new Error("Unknown relation: " + c);
//
//		try {
//			res = c.getObject1AsURI() + " " + rel + " (" 
//					+ c.getStrength() + ") " + c.getObject2AsURI();
//		} catch (AlignmentException e) {
//			e.printStackTrace();
//		}
//		return res;
//	}

	@Override
	public LightAlignment diff(Alignment align) throws AlignmentException {
		LightAlignment result = new LightAlignment();
		for ( Cell c1 : this ) {
			boolean found = false;
			Set<Cell> s2 = align.getAlignCells1( c1.getObject1() );
			if ( s2 != null ){
				for ( Cell c2 : s2 ){
					if ( c1.equals( c2 ) ) {
//					if(areEqual((LightCell)c1, (LightCell)c2)){
						found = true;
						break;
					}
//					else{
//						System.out.print(c1.equals( c2 ));
//					}		
				}
			}
			if ( !found ) result.addCell( c1 );
		}
		return result;
	}
	
	private static boolean areEqual(LightCell c1, LightCell c2){
		
		if(c1 == c2 || c1 == null && c2 == null)
			return true;
		if(c1 == null && c2 != null)
			return false;
		if(c2 == null && c1 != null)
			return false;
		boolean res = c1.getObject1().equals(c2.getObject1()) && 
				c1.getObject2().equals(c2.getObject2()) &&
				c1.getRelation().equals(c2.getRelation());
		
		return res;
	}
	
	public int getOneOneMappingNumber() {
		int n = 0;
		for (Cell c : this) {
			try {
				Set<Cell> clashOnSrc = getAlignCells1(c.getObject1());
				Set<Cell> clashOnTrg = getAlignCells2(c.getObject2());
				if(clashOnSrc.size() == 1 && clashOnTrg.size() == 1)
					++n;
			} catch (AlignmentException e) {
				e.printStackTrace();
			}			
		}
		return n;
	}
	
	@Override
	public String toString(){
		StringBuilder strB = new StringBuilder();
		for (Cell c : this){
			strB.append("\t"+c+"\n");//printCell(c)+"\n");
		}
		return strB.toString();
	}
}
