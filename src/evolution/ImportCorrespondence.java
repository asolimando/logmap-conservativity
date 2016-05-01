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

import java.util.Map;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;

import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;
import uk.ac.ox.krr.logmap2.utilities.Utilities;
import util.OntoUtil;

public class ImportCorrespondence {

	protected float confidence;
	protected int support;
	protected String corr_type;
	protected int nChecked;
	protected String domainObjectType;
	protected String domainSourceName;
	private String domainObjAcc;
	protected String rangeObjectType;
	protected String rangeSourceName;
	private String rangeObjAcc;
	protected Map<String,Object> corrAtts;

	@Override
	public String toString(){
		return domainObjAcc + " " + corr_type + " (" + confidence 
				+ ")" + " " + rangeObjAcc;
	}
	
	public OWLClass getDomainOWLClass(String iri){
		return OntoUtil.getOWLClassFromIRI(iri+domainObjAcc);
	}
	
	public OWLClass getRangeOWLClass(String iri){
		return OntoUtil.getOWLClassFromIRI(iri+rangeObjAcc);
	}
	
	public OWLAxiom toOWLAxiom(String iri1, String iri2){
		OWLClass range = getRangeOWLClass(iri1), domain = getDomainOWLClass(iri2);
		
		switch(corr_type){
		case "<":
			return OntoUtil.getDataFactory().getOWLSubClassOfAxiom(
					range, domain);
		case ">":
			return OntoUtil.getDataFactory().getOWLSubClassOfAxiom(
					domain, range);
		case "is_equivalent_to":
		default:
			return OntoUtil.getDataFactory().getOWLEquivalentClassesAxiom(
					range, domain);
		}				
	}

	public String getDomainObjAcc(){
		return domainObjAcc;
	}

	public String getRangeObjAcc(){
		return rangeObjAcc;
	}

	public void setDomainObjAcc(String str) {
		domainObjAcc = fix(str);
	}
	
	public void setRangeObjAcc(String str) {
		rangeObjAcc = fix(str);
	}
	
	public String fix(String str){
		if(str.startsWith("NCI"))
			return str.replace(":", "#_");
		else if(str.startsWith("UWDA"))
			return str.replace(":", "_");
		else if(str.startsWith("SNOMEDCT"))
			return str.replace(":", "_");
		else {
//			System.out.println(str);
			return str;
		}
	}

	public MappingObjectStr toMappingStrObj(String iri) {
		String defaultIRI = iri == null ? "http://purl.library.org/obo/" : iri;
		int corrType = Utilities.EQ;
		
		return new MappingObjectStr(
				defaultIRI + getDomainObjAcc(), 
				defaultIRI + getRangeObjAcc(),
				confidence);
//		,
//				corrType,
//				Utilities.CLASSES);
	}
}
