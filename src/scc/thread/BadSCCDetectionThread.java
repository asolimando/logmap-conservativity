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
package scc.thread;

import scc.graphDataStructure.LightSCC;
import scc.graphDataStructure.LightSCCs;

import java.util.concurrent.Callable;

import util.Params;

public class BadSCCDetectionThread implements Callable<LightSCCs>{

	private LightSCCs localSCCs;
	private LightSCC scc;

	public BadSCCDetectionThread(LightSCCs localSCCs, LightSCC scc){
		this.localSCCs = localSCCs;
		this.scc = scc;
	}

	@Override
	public LightSCCs call() throws Exception {
		LightSCC fstSCC = scc.extract(true), sndSCC = scc.extract(false);
		LightSCCs problematicSCCs = new LightSCCs();

		if(Params.fullDetection){
			if( (!fstSCC.isEmpty() && !localSCCs.contains(fstSCC) )
					|| (!sndSCC.isEmpty() && !localSCCs.contains(sndSCC))
					) 
				problematicSCCs.add(scc);
		}
		else {
			if(!fstSCC.isEmpty() && !sndSCC.isEmpty() 
					&& !(localSCCs.contains(fstSCC) 
							&& localSCCs.contains(sndSCC)))
				problematicSCCs.add(scc);		
		}
		return problematicSCCs;
	}
}
