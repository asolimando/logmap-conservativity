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
package uk.ac.ox.krr.logmap2.utilities;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Statistics {
	
	private Collection<Double> values;
	private double mean;
	private double sd;
	private boolean readyMean;
	private boolean readySd;
	
	public Statistics(Collection<Double> values) {
		if(values.size() == 0)
			throw new IllegalArgumentException("Statistics: Cannot accept empty collection");
		this.values = values;
		readyMean = false;
		readySd = false;
	}
	
	public double getMean() {
		if(!readyMean) {
			mean = 0;
			for(double value : values) {
				mean += value;
			}
			mean = mean / values.size();
			readyMean = true;
		}
		return mean;
	}
	
	public double getStandardDeviation() {
		if(!readySd) {
			double mean = getMean();
			sd = 0;
			for(double value : values) {
				sd += (value - mean) * (value - mean);
			}
			sd = Math.sqrt(sd / values.size());
		}
		return sd;
	}
	
	public static void main(String[] args) {
		//For values: mean=50, sd=10
		Set<Double> values = new HashSet<Double>();
		values.add(50d);
		values.add(60d);
		values.add(40d);
		values.add(50+Math.sqrt(150));
		values.add(50-Math.sqrt(150));

		Set<Double> values2 = new HashSet<Double>();
		values2.add(40d);
		values2.add(35d);
		values2.add(15d);
		values2.add(60d);
		values2.add(48d);
		values2.add(84d);
		values2.add(55d);
		values2.add(45d);
		values2.add(24d);
		values2.add(33d);
		values2.add(16d);
		values2.add(74d);
		values2.add(68d);
		
		Statistics s = new Statistics(values2);
		System.out.println(s.getMean() + "\t" + s.getStandardDeviation());
		
		for(double value : values2) {
			if(value >= s.getMean() + s.getStandardDeviation())
				System.out.println("\t" + value);
			else
				System.out.println(value);
		}
	}
	
}
