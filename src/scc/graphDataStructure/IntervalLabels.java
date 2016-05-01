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
package scc.graphDataStructure;

import java.util.ArrayList;
import java.util.Collection;

import util.FileUtil;
import util.Params;

public class IntervalLabels extends ArrayList<IntervalLabel> {

	private static final long serialVersionUID = 827667631571722019L;

	public IntervalLabels(){
		super(Params.labelDims);
	}
	
	public IntervalLabels(int dimensionsNum){
		super(dimensionsNum);
	}
	
	public IntervalLabels(Collection<IntervalLabel> c){
		super(c);
	}
	
	public IntervalLabels(IntervalLabel ... labels){
		super(labels.length);
		for (IntervalLabel l : labels)
			add(l);
	}
	
	public boolean subsumes(IntervalLabels labels2){
		if(size() != labels2.size())
			throw new IllegalArgumentException("Size for interval labels must " +
					"coincide, found " + size() + " and " + labels2.size());
	
		for (int i = 0; i < labels2.size(); i++){
			FileUtil.writeLogAndConsoleNONL(get(i) + " < " + labels2.get(i));
			boolean r;
			if(r = !get(i).subsumes(labels2.get(i))){
				FileUtil.writeLogAndConsole(" = " + !r);
				return false;
			}
			FileUtil.writeLogAndConsole(" = " + !r);
		}
		return true;
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		int c = 0;
		for (IntervalLabel i : this)
			sb.append((++c) + ": " + i + " ");
		return sb.toString();
	}
}
