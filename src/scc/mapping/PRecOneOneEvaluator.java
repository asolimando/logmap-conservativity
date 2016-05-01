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
import java.util.Properties;
import java.util.Set;

import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Cell;

import fr.inrialpes.exmo.align.impl.eval.PRecEvaluator;

public class PRecOneOneEvaluator extends PRecEvaluator {

	LightAlignment lalign1 = null, 
			lalign2 = null;

	public PRecOneOneEvaluator(LightAlignment arg0, LightAlignment arg1)
			throws AlignmentException {
		super(arg0, arg1);
		lalign1 = arg0;
		lalign2 = arg1;
	}

	public int getOneOneAlign1(){
		return lalign1.getOneOneMappingNumber();
	}

	public int getOneOneAlign2(){
		return lalign2.getOneOneMappingNumber();
	}

//	/**
//	 *
//	 * The formulas are standard:
//	 * given a reference alignment A
//	 * given an obtained alignment B
//	 * which are sets of cells (linking one entity of ontology O to another of ontolohy O').
//	 *
//	 * P = |A inter B| / |B|
//	 * R = |A inter B| / |A|
//	 * F = 2PR/(P+R)
//	 * with inter = set intersection and |.| cardinal.
//	 *
//	 * In the implementation |B|=nbfound, |A|=nbexpected and |A inter B|=nbcorrect.
//	 */
//	@Override
//	public double eval(Properties params) throws AlignmentException {
//		init();
//		nbfound = lalign2.nbCells();
//		boolean relsensitive = false, confSensitive = false;
//		if ( params != null){
//			if(params.getProperty("relations") != null ) 
//				relsensitive = true;
//			if(params.getProperty("confidence") != null )
//				confSensitive = true;
//		}
//
//		for ( Cell c1 : lalign1 ) {
//			URI uri1 = c1.getObject2AsURI();
//			nbexpected++;
//			Set<Cell> s2 = align2.getAlignCells1( c1.getObject1() );
//			if( s2 != null ){
//				for( Cell c2 : s2 ) {
//					URI uri2 = c2.getObject2AsURI();	
//					if ( uri1.equals( uri2 )
//							&& ( !relsensitive || c1.getRelation().equals( c2.getRelation() ) ) 
//							&& ( !confSensitive || c1.getStrength() == c2.getStrength() ) ) {
//						nbcorrect++;
//						break;
//					}
//				}
//			}
//		}
//		// What is the definition if:
//		// nbfound is 0 (p is 1., r is 0)
//		// nbexpected is 0 [=> nbcorrect is 0] (r=1, p=0)
//		// precision+recall is 0 [= nbcorrect is 0]
//		// precision is 0 [= nbcorrect is 0]
//		if ( nbfound != 0 ) precision = (double) nbcorrect / (double) nbfound;
//		if ( nbexpected != 0 ) recall = (double) nbcorrect / (double) nbexpected;
//		return computeDerived();
//	}
}
