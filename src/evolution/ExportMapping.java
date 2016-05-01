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

import java.net.URI;
import java.util.GregorianCalendar;
import java.util.Set;

import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;
import uk.ac.ox.krr.logmap2.utilities.Utilities;
import util.FileUtil;

public class ExportMapping {
	
//	protected boolean isExternalImport;
//	protected String accessClass;
	protected String baseName;
	protected String versionName;
	protected String mappingClass = "ontology_mapping";
	protected String mappingType = "corresponds_to";
	protected String mappingMethod = "";
	protected String mappingTool = "N/A";
	protected GregorianCalendar mappingTimestamp;
	protected boolean isInstanceMap = false;
	protected float minConfidence = 0;
	protected int minSupport = 0;
//	protected String accessLocation;
//	protected Set<ImportCorrespondence> correspondences = new HashSet<>();
//	protected Map<String,String> accessProps;
//	protected Map<String,String> accessParams;

	private Set<MappingObjectStr> mappings;
	private String outFilePath;
	
	public ExportMapping(String filename, Set<MappingObjectStr> mappings){
		this.mappings = mappings;
		this.outFilePath = filename;
		int idx = filename.lastIndexOf(".");
		baseName = idx > 0 ? filename.substring(0, filename.lastIndexOf(".")) 
				: filename;
		versionName = baseName;
		mappingMethod = baseName;
	}
	
	private void addRow(StringBuilder str, String s){
		str.append(s + "\n");
	}
	
	private void addMapping(StringBuilder str, MappingObjectStr m){
		String corr = "is_equivalent_to";
		
		if(m.getMappingDirection() == Utilities.L2R)
			corr = "is_less_general_than";
		else if(m.getMappingDirection() == Utilities.R2L)
			corr = "is_more_general_than";
		
		addRow(str,"<correspondence support=\"0\" confidence=\"" 
				+ m.getConfidence() 
				+ "\" user_checked=\"0\" status=\"N/A\" corr_type=\"" 
				+ corr + "\" />");
		addRow(str,"<domain_objects>");
		addObject(str,m.getIRIStrEnt1());
		addRow(str,"</domain_objects>");
		addRow(str,"<range_objects>");
		addObject(str,m.getIRIStrEnt2());
		addRow(str,"</range_objects>");
		addRow(str,"</correspondence>");
	}
	
	private void addObject(StringBuilder str, String uri){
		boolean fma = uri.contains("UWDA");
		boolean nci = uri.contains("NCI");
		
		String abbrName = URI.create(uri).getFragment();
		
		if(nci)
			abbrName = abbrName.replaceFirst("_", "NCI:");
		if(fma) {
			abbrName = uri.substring(uri.lastIndexOf('/')+1);
			abbrName = abbrName.replaceFirst("UWDA_", "UWDA:");
		}
		addRow(str, "<object accession=\"" + abbrName + "\" objecttype=\"" + 
		(fma? "AnatomicalEntity" : "HealthEntity") + "\" source_name=\"" + 
				(fma ? "FoundationalModelofAnatomy" : "NCI") + "\"/>");
	}
	
	private void addMappingInfo(StringBuilder str, String YYM){
		addRow(str,"<mapping baseName=\"" + baseName + "\" versionName=\"" + 
				versionName + "\" timestamp=\"" + YYM + "-07" + "\" " +
						"is_instance_map=\"false\" mapping_class=\"ontology_mapping\" " +
						"mapping_type=\"corresponds_to\" mapping_tool=\"N/A\" " +
						"mapping_method=\"" + mappingMethod + "\">");

	}
	
	public void export(String YYM){
		System.out.println("Exporting file to: " + outFilePath);
		
		StringBuilder str = new StringBuilder(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		
		addMappingInfo(str, YYM);
		
		addRow(str,"<metadata minConfidence=\"0.0\" minSupport=\"0\">");
		addRow(str,"<domain_sources>");
		addRow(str,"<source objecttype=\"HealthEntity\" name=\"NCI\" " +
				"timestamp=\"" + YYM + "-07\" " + "version=\"" + YYM + 
				"\"is_ontology=\"yes\" structural_type=\"directed_acyclic\" " +
				"url=\"\" />");
		addRow(str,"</domain_sources>");
		addRow(str,"<range_sources>");
		addRow(str,"<source objecttype=\"AnatomicalEntity\" " +
				"name=\"FoundationalModelofAnatomy\" timestamp=\"" + YYM + "-07\" " +
				"version=\"" + YYM + "\" is_ontology=\"yes\" " +
				"structural_type=\"directed_acyclic\" url=\"\" />");
		addRow(str,"</range_sources>");
		addRow(str,"</metadata>");

		addRow(str,"<correspondences>");
		
		for (MappingObjectStr m : mappings)
			addMapping(str, m);
		
		addRow(str,"</correspondences>");
		addRow(str,"</mapping>");
		
		FileUtil.writeStringToFile(str.toString(), outFilePath);
	}
}
