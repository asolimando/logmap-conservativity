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
package reasoning;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import util.FileUtil;
import util.Params;

public class KoncludeReasoning {
	
	public static long computeClassification(String ontoPath, int timeout){
		ProcessBuilder pb = new ProcessBuilder(new String [] 
//		{"/bin/sh","-c","/usr/bin/time -f \"%E\" lib/Konclude-v0.6.0-408-linux64/Binaries/Konclude classification -w AUTO -i " + ontoPath});
		{"/bin/sh","-c","/usr/bin/time -f \"%E\" " + Params.timeoutBinPath 
				+ " --preserve-status --s TERM " + timeout +
				" lib/Konclude-v0.6.0-408-linux64/Binaries/Konclude classification -w AUTO -i " + ontoPath});
		Process proc = null;
		InputStream in = null;
		StringBuilder output = new StringBuilder();
		long time = -1;
		byte[] data = null;
		
		int exitValue = -1;
		
		try {
			pb.redirectErrorStream(true);
			proc = pb.start();
			proc.waitFor();
			exitValue = proc.exitValue();
			in = proc.getInputStream();
			data = new byte[in.available()];
			in.read(data);
			output.append(new String(data, "UTF-8"));
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		String outStr = output.toString();
		String [] outStrs = outStr.split("\n");
		String failureStr = outStrs[outStrs.length-2];
		String timeStr = outStrs[outStrs.length-1];
		FileUtil.writeLogAndConsole("Konclude Output: " + outStr);

		// timeout!
		if(exitValue == 143){
//			FileUtil.writeErrorLogAndConsole(
//					"Konclude timedout with " + timeout + "(s)");
			return -1;
		}
		
		// usually signal 9 when killed for memory saturation
		if(failureStr.startsWith("Command terminated by signal"))
			return -2;
		
		// unsat ontology
		if(failureStr.contains("Requirements for ontology") && 
				failureStr.contains("not satisfied."))
			return -100;
			
//		[minutes:]seconds.centofsecs
		int columnId = timeStr.indexOf(':');
		int dotId = timeStr.indexOf('.');
		
		// something went wrong, we record the failure
		if(columnId == -1 || dotId == -1)
			return -3;
		
		String min = timeStr.substring(0, columnId), 
				sec = timeStr.substring(columnId+1, dotId),
				csec = timeStr.substring(dotId+1, timeStr.length());

		try {
			time = Integer.valueOf(csec) * 10;
			time += Integer.valueOf(sec) * 1000;
			time += Integer.valueOf(min) * 60 * 1000;
		}
		catch(NumberFormatException e){
			FileUtil.writeErrorLogAndConsole("Invalid time!");
			return -4;
		}
//		System.out.println(min + ":" + sec + "." + csec);
//
//		System.out.println(time);

		return time;
	}
}
