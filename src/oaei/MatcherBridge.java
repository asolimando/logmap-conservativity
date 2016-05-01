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
package oaei;

import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;

import org.semanticweb.owlapi.model.IRI;

import uk.ac.ox.krr.logmap2.Parameters;
import util.FileUtil;

import eu.sealsproject.platform.res.domain.omt.IOntologyMatchingToolBridge;
import eu.sealsproject.platform.res.tool.api.ToolBridgeException;
import eu.sealsproject.platform.res.tool.api.ToolException;
import eu.sealsproject.platform.res.tool.api.ToolType;
import eu.sealsproject.platform.res.tool.impl.AbstractPlugin;


public class MatcherBridge extends AbstractPlugin implements IOntologyMatchingToolBridge {

	/**
	* LogMap 2 aligns two ontologies specified via their URL and returns the 
	* URL of the resulting alignment, which should be stored locally.
	* 
	*/
	public URL align(URL source, URL target) throws ToolBridgeException, ToolException {
		LogMap2_OAEI logmap;
		URL url_alignment;
		long init, fin;
		
		init = Calendar.getInstance().getTimeInMillis();
		
		try {
		
			logmap = new LogMap2_OAEI();
		
			logmap.align(source, target);
			
			url_alignment = logmap.returnAlignmentFile(); //Local-temporary file
			
			FileUtil.writeLogAndConsole("URL mapping: " + url_alignment);
			
			fin = Calendar.getInstance().getTimeInMillis();
			//System.out.println("Total time LogMap (s): " + (float)((double)fin-(double)init)/1000.0);
		
			return url_alignment;
			
		}
		
		catch (IOException e) {
			throw new ToolBridgeException("Cannot create file for resulting alignment", e);
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new ToolException("Error extracting/cleaning/storing mappings with LogMap: ");
			
		}
			
		
	}

	/**
	* Not considered in LogMap
	*/
	public URL align(URL source, URL target, URL inputAlignment) throws ToolBridgeException, ToolException {
		throw new ToolException("functionality of called method is not supported");
	}

	/**
	* No pre-resiquites in LogMap
	*/
	public boolean canExecute() {
		return true;
	}

	/**
	* The LogMap is an ontology matching tool. 
	*/
	public ToolType getType() {
		return ToolType.OntologyMatchingTool;
	}
	
	
	
	public static void main(String[] args) {
		MatcherBridge m = new MatcherBridge();
		
		//Parameters.print_output=true;
		
//		String uri1 = "file:/home/ale/data/oaei2013/conference/onto/cmt.owl";
//		String uri2 = "file:/home/ale/data/oaei2013/conference/onto/ekaw.owl";
		
		String uri1 = "file:/home/ale/data/oaei2013/anatomy/onto/mouse.owl";
		String uri2 = "file:/home/ale/data/oaei2013/anatomy/onto/human.owl";
		
//		String uri1 = "file:/home/ale/data/oaei2013/library/onto/stw.owl";
//		String uri2 = "file:/home/ale/data/oaei2013/library/onto/thesoz.owl";

//		String uri1 = "file:/home/ale/data/oaei2013/largebio/onto/oaei2013_SNOMED_extended_overlapping_fma_nci.owl";
//		String uri2 = "file:/home/ale/data/oaei2013/largebio/onto/oaei2013_NCI_whole_ontology.owl";
		
//		String uri1 = "file:/home/ale/data/oaei2013/largebio/onto/oaei2013_FMA_whole_ontology.owl";
//		String uri2 = "file:/home/ale/data/oaei2013/largebio/onto/oaei2013_SNOMED_extended_overlapping_fma_nci.owl";
		
		try {
			m.align(IRI.create(uri1).toURI().toURL(), IRI.create(uri2).toURI().toURL());
		} catch (ToolBridgeException | MalformedURLException e) {
			e.printStackTrace();
		}
	}
	
}
