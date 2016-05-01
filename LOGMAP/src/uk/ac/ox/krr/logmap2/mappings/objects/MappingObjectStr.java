/*******************************************************************************
 * Copyright 2012 by the Department of Computer Science (University of Oxford)
 * 
 *    This file is part of LogMap.
 * 
 *    LogMap is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 * 
 *    LogMap is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 * 
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with LogMap.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package uk.ac.ox.krr.logmap2.mappings.objects;

import java.net.URI;

import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Cell;

import fr.inrialpes.exmo.align.impl.rel.SubsumedRelation;
import uk.ac.ox.krr.logmap2.utilities.Utilities;



public class MappingObjectStr {

	String Iri_ent1_str;
	String Iri_ent2_str;
	double confidence;
	int dir_mappings;
	int typeMappings;//classes, properties, instances



	//DIR IMPLICATION
	public static final int SUB = 0; //L2R=0; //P->Q
	public static final int SUP=-1; //R2L P<-Q
	public static final int EQ=-2; //P<->Q



	//TYPE OF MAPPING
	public static final int CLASSES=0;
	public static final int DATAPROPERTIES=1;
	public static final int OBJECTPROPERTIES=2;
	public static final int INSTANCES=3;
	public static final int UNKNOWN=4;


	public MappingObjectStr(MappingObjectStr m){
		this.Iri_ent1_str = m.Iri_ent1_str;
		this.Iri_ent2_str = m.Iri_ent2_str;
		this.confidence = m.confidence;
		this.dir_mappings = m.dir_mappings;
		this.typeMappings = m.typeMappings;
	}

	public MappingObjectStr(String iri_ent1, String iri_ent2){

		Iri_ent1_str=iri_ent1;
		Iri_ent2_str=iri_ent2;
		confidence=-1;
		dir_mappings=MappingObjectStr.EQ;
		typeMappings = MappingObjectStr.UNKNOWN;
	}

	public MappingObjectStr(String iri_ent1, String iri_ent2, double conf){

		Iri_ent1_str=iri_ent1;
		Iri_ent2_str=iri_ent2;
		confidence=conf;
		dir_mappings=MappingObjectStr.EQ;
		typeMappings = MappingObjectStr.UNKNOWN;

	}

	public MappingObjectStr(String iri_ent1, String iri_ent2, double conf, int dir_mapping){

		Iri_ent1_str=iri_ent1;
		Iri_ent2_str=iri_ent2;
		confidence=conf;
		dir_mappings=dir_mapping;
		typeMappings = MappingObjectStr.UNKNOWN;		
	}

	public MappingObjectStr(String iri_ent1, String iri_ent2, double conf, int dir_mapping, int typeMapping){

		Iri_ent1_str=iri_ent1;
		Iri_ent2_str=iri_ent2;
		confidence=conf;
		dir_mappings=dir_mapping;
		typeMappings = typeMapping;		
	}

	public MappingObjectStr(Cell c, int dir, int typeMapping) throws AlignmentException{				
		this(c.getObject1AsURI().toASCIIString(), 
				c.getObject2AsURI().toASCIIString(), c.getStrength(), 
				dir, typeMapping);
	}

	public int getMappingDirection(){
		return dir_mappings;
	}

	public String getIRIStrEnt1(){
		return Iri_ent1_str;

	}

	public String getIRIStrEnt2(){
		return Iri_ent2_str;

	}

	public double getConfidence(){
		return confidence;

	}


	public int getTypeOfMapping(){
		return typeMappings;

	}

	public void setTypeOfMapping(int type){
		typeMappings = type;

	}

	public void setConfidenceMapping(double conf){
		confidence = conf;

	}




	public boolean equals(Object o){

		if  (o == null)
			return false;
		if (o == this)
			return true;
		if (!(o instanceof MappingObjectStr))
			return false;

		MappingObjectStr i =  (MappingObjectStr)o;

		return equals(i);

	}

	public boolean equals(MappingObjectStr m){

		//TODO: maybe the mapping is in the other from ent2 to ent1
		if (!Iri_ent1_str.equals(m.getIRIStrEnt1()) || !Iri_ent2_str.equals(m.getIRIStrEnt2())){
			return false;
		}
		return true;
	}

	public String toString(){
		String relation = " ? ";

		switch(dir_mappings){
		case Utilities.EQ:
			relation = "<->";
			break;
		case Utilities.L2R:
			relation = "->";
			break;
		case Utilities.R2L:
			relation = "<-";
			break;
		}

		return "<"+Iri_ent1_str+relation+Iri_ent2_str+">";
	}

	public  int hashCode() {
		int code = 10;
		code = 40 * code + Iri_ent1_str.hashCode();
		code = 40 * code + Iri_ent2_str.hashCode();
		return code;
	}

	public void weakenMapping(MappingObjectStr removed){
		if(dir_mappings != Utilities.EQ)
			return;
		else {
			if(Iri_ent1_str.equals(removed.Iri_ent1_str) && 
					Iri_ent2_str.equals(removed.Iri_ent2_str)){
				if(removed.getMappingDirection() == Utilities.L2R)
					this.dir_mappings = Utilities.R2L;
				else if(removed.getMappingDirection() == Utilities.R2L)
					this.dir_mappings = Utilities.L2R;
			}
			else if(Iri_ent1_str.equals(removed.Iri_ent2_str) && 
					Iri_ent2_str.equals(removed.Iri_ent1_str)){
				if(removed.getMappingDirection() == Utilities.L2R)
					this.dir_mappings = Utilities.L2R;
				else if(removed.getMappingDirection() == Utilities.R2L)
					this.dir_mappings = Utilities.R2L;
			}
		}
	}
	
	public static boolean areCompatibleMappings(MappingObjectStr m1, 
			MappingObjectStr m2){
		if(m1.Iri_ent1_str.equals(m2.Iri_ent1_str) && 
				m1.Iri_ent2_str.equals(m2.Iri_ent2_str))
			return true;
		
		if(m1.Iri_ent1_str.equals(m2.Iri_ent2_str) && 
				m1.Iri_ent2_str.equals(m2.Iri_ent1_str))
			return true;
		
		return false;
	}
	
	public static boolean doCoincide(MappingObjectStr m1, 
			MappingObjectStr m2){
		if(m1.getTypeOfMapping() != m2.getTypeOfMapping())
			return false;
		
		if(m1.Iri_ent1_str.equals(m2.Iri_ent1_str) && 
				m1.Iri_ent2_str.equals(m2.Iri_ent2_str) && 
				m1.getMappingDirection() == m2.getMappingDirection())
			return true;

		if(m1.Iri_ent1_str.equals(m2.Iri_ent2_str) && 
				m1.Iri_ent2_str.equals(m2.Iri_ent1_str) && 
				m1.getMappingDirection() == m2.getMappingDirection() && 
				m1.getMappingDirection() == Utilities.EQ)
			return true;
		
		if(m1.Iri_ent1_str.equals(m2.Iri_ent2_str) && 
				m1.Iri_ent2_str.equals(m2.Iri_ent1_str) && 
				haveOppositeDirection(m1, m2))
			return true;
		
		return false;
	}
	
	public static boolean haveOppositeDirection(MappingObjectStr m1, 
			MappingObjectStr m2){
		return (m1.getMappingDirection() == Utilities.L2R &&
				m2.getMappingDirection() == Utilities.R2L)
				||
				(m1.getMappingDirection() == Utilities.R2L &&
				m2.getMappingDirection() == Utilities.L2R);
	}
	
	public static boolean isWeakeningOf(MappingObjectStr m1, 
			MappingObjectStr m2){
		// the matcher elements are different (checks all the combinations)
		if(!areCompatibleMappings(m1, m2))
			return false;

		// identical match (entities and relation)
		if(doCoincide(m1, m2))
			return true;

		// identical entities (same "position")
		if(m1.equals(m2)){
			// redundant, would have matched doCoincide
//			if(m1.dir_mappings == m2.dir_mappings)
//				return true;
			if(m1.dir_mappings == Utilities.EQ)
				return false;
			if((m1.dir_mappings == Utilities.L2R || 
					m1.dir_mappings == Utilities.R2L) && 
					m2.dir_mappings == Utilities.EQ){
				return true;
			}
		}
		
		// redundant 'cause it must be true, otherwise not compatible would have failed (first check)
		else if(m1.Iri_ent1_str.equals(m2.Iri_ent2_str) && 
				m1.Iri_ent2_str.equals(m2.Iri_ent1_str)){
			if((m1.dir_mappings == Utilities.L2R || 
					m1.dir_mappings == Utilities.R2L) && 
					m2.dir_mappings == Utilities.EQ){
				return true;
			}
		}
		
		return false;
	}
}
