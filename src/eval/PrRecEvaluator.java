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
package eval;

import java.util.Set;

import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;

public class PrRecEvaluator {
	Set<MappingObjectStr> align1;
	Set<MappingObjectStr> align2;
	boolean useRelation, useConfidence;
	double prec, recall, fmeas;
	int expNum, foundNum, correctNum;
    
	public PrRecEvaluator(Set<MappingObjectStr> align1, 
			Set<MappingObjectStr> align2){
		this.align1 = align1;
		this.align2 = align2;
	}
	
	public void setConsiderRelation(boolean useRelation){
		this.useRelation = useRelation;
	}
	
	public void setConsiderConfidence(boolean useConfidence){
		this.useConfidence = useConfidence;
	}
	
	public void eval(){
		foundNum = align1.size();
		for (MappingObjectStr m2 : align2) {
			expNum++;
			for (MappingObjectStr m1 : align1) {
				if(MappingObjectStr.doCoincide(m1,m2)){
					if(!useConfidence || m2.getConfidence() == m1.getConfidence()){
						correctNum++;
						break;
					}
				}
				else if(MappingObjectStr.isWeakeningOf(m1, m2)){					
					if(!useConfidence || m2.getConfidence() == m1.getConfidence()){
						correctNum++;
						break;
					}
				}
			}
		}
		computeValues();
	}
	
	public void computeValues(){
		if ( foundNum != 0 ) 
			prec = (double) correctNum / (double) foundNum;
		if ( expNum != 0 ) 
			recall = (double) correctNum / (double) expNum;

		if ( prec != 0. )
		    fmeas = 2 * prec * recall / (prec + recall);
	}
	
	public double getPrecision(){
		return prec;
	}
	
	public double getRecall(){
		return recall;
	}
	
	public double getFMeasure(){
		return fmeas;
	}

	public int getFound() {
		return foundNum;
	}
	
	public int getExpected() {
		return expNum;
	}
	
	public int getCorrect() {
		return correctNum;
	}
}
