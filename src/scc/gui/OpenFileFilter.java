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
package scc.gui;

import java.io.File;
import java.util.Arrays;

import javax.swing.filechooser.FileFilter;

/**
 * Class representing a filter based on file's extension.
 */
public class OpenFileFilter extends FileFilter {

	String[] formats;

	public OpenFileFilter(String[] strings){
		for (int i = 0; i < strings.length; i++) {
			strings[i] = strings[i].toLowerCase();
		}
		this.formats = strings;
	}

	/**
	 * Methods that accept or reject a file by the use of a filter.
	 *
	 * @param file the file to accept or reject
	 * @return true if the file is acceptable for this filter, false otherwise
	 */
	public boolean accept(File file) {
		if (file.isDirectory()) return true;
		String fname = file.getName().toLowerCase();
		for (int i = 0; i < formats.length; i++) {
			if(fname.endsWith("." + formats[i].toLowerCase()))
				return true;
		}
		return false;
	}

	/**
	 * Methods that return a string representation of the filter.
	 *
	 * @return the description of the filter
	 */
	public String getDescription() {
		return "File of format " + Arrays.toString(formats);
	}
}
