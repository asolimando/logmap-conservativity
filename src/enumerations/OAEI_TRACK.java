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
package enumerations;

public enum OAEI_TRACK {
	CONFERENCE("Conference Track","conference","conference"),
	ANATOMY("Anatomy Track","anatomy","anatomy"),
	LIBRARY("Library Track","library","library"), 
	LARGEBIOBIG("LargeBio-Big Track","largebio","largebiobig"), 
	LARGEBIOSMALL("LargeBio-Small Track","largebio","largebiosmall")
	;
	
	private String descr, dir, compactName;
	
	private OAEI_TRACK(String descr, String dir, String compactName){
		this.descr = descr;
		this.dir = dir;
		this.compactName = compactName;
	}
	
	public String toString(){
		return descr;
	}
	
	public String getDir(){
		return dir;
	}
	
	public String getCompactName(){
		return compactName;
	}
}
