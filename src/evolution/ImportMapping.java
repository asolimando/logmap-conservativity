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
package evolution;

import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;
import uk.ac.ox.krr.logmap2.utilities.Utilities;

import auxStructures.Pair;

public class ImportMapping {

	protected boolean isExternalImport;
	protected String accessClass;
	protected String baseName;
	protected String versionName;
	protected String mappingClass;
	protected String mappingType;
	protected String mappingMethod;
	protected String mappingTool;
	protected GregorianCalendar mappingTimestamp;
	protected boolean isInstanceMap;
	protected float minConfidence;
	protected int minSupport;
	protected String accessLocation;
	protected Set<ImportCorrespondence> correspondences = new HashSet<>();
	protected Map<String,String> accessProps;
	protected Map<String,String> accessParams;
	
	public void addCorrespondence(ImportCorrespondence currentCorrespondence) {
		correspondences.add(currentCorrespondence);
	}

	public void addParameter(String string, String string2) {
		accessParams.put(string, string2);
	}

	@Override
	public String toString(){
		return correspondences.toString();
	}
	
	public Set<MappingObjectStr> convertToAlignment(String iri){
		Set<MappingObjectStr> align = new HashSet<>();
				
		for (ImportCorrespondence corr : correspondences)
			align.add(corr.toMappingStrObj(iri));
		
		return align;
	}
}
