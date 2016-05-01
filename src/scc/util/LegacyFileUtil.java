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
package scc.util;

import scc.graphDataStructure.Diagnosis;
import scc.graphDataStructure.LightAdjacencyList;
import scc.graphDataStructure.LightEdge;
import scc.graphDataStructure.LightSCC;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ontology.AxiomExplanation;

import org.semanticweb.owlapi.model.OWLAxiom;

import scc.mapping.PRecOneOneEvaluator;
import scc.test.ResultASPSGA;
import util.FileUtil;
import util.Params;

public class LegacyFileUtil {

	public static int countLines(String filename) throws IOException {
	    InputStream is = new BufferedInputStream(new FileInputStream(filename));
	    try {
	        byte[] c = new byte[1024];
	        int count = 0;
	        int readChars = 0;
	        boolean endsWithoutNewLine = false;
	        while ((readChars = is.read(c)) != -1) {
	            for (int i = 0; i < readChars; ++i) {
	                if (c[i] == '\n')
	                    ++count;
	            }
	            endsWithoutNewLine = (c[readChars - 1] != '\n');
	        }
	        if(endsWithoutNewLine) {
	            ++count;
	        } 
	        return count;
	    } finally {
	        is.close();
	    }
	}
	
	public static void createDirPath(String path){
		File outDir = new File(path);
		if(!outDir.exists() && !outDir.mkdirs())
			throw new Error("Error while creating test output directory: " 
					+ path);
	}

	public static void writeStringToFile(String str, String filePath){
		PrintWriter tmpASP;
		try {
			tmpASP = new PrintWriter(filePath);
			tmpASP.print(str);
			tmpASP.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static String readStringFromFile(String filePath){
		BufferedReader br = null;
		StringBuilder sb = null;
		try {
			br = new BufferedReader(new FileReader(new File(filePath)));
			sb = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				sb.append(line);
				sb.append('\n');
				line = br.readLine();
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	public static void serializeObject(String filepath, Object obj){
		try {
			FileOutputStream fileOut = new FileOutputStream(
					filepath);
			ObjectOutputStream objOut = new ObjectOutputStream(fileOut);
			objOut.writeObject(obj);
			objOut.close();
			fileOut.close();			
		}catch(IOException i) {
			i.printStackTrace();
		}
	}

	public static void printExplanations(Set<AxiomExplanation> explanations, 
			PrintWriter out){
		if(out != null){			
			for (AxiomExplanation set : explanations) {
				out.println(set.getAxiom() + " -> ");
				for (OWLAxiom owlAxiom : set)
					out.println(owlAxiom);
				out.println("\n");
			}
		}
		else {
			for (AxiomExplanation set : explanations) {
				FileUtil.writeLogAndConsole(set.getAxiom() + " -> ");
				for (OWLAxiom owlAxiom : set)
					FileUtil.writeLogAndConsole(owlAxiom.toString());
				FileUtil.writeLogAndConsole("\n");
			}
		}
	}
	
	public static void writeFileAndConsole(PrintWriter out, String s){
		out.println(s);
		out.flush();
		System.out.println(s);
	}
	
	public static Object deserializeObject(String filepath){
		FileInputStream fileIn;
		Object obj = null;
		try {
			fileIn = new FileInputStream(new File(filepath));
			ObjectInputStream in = new ObjectInputStream(fileIn);
			obj = in.readObject();
			in.close();
			fileIn.close();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		return obj;
	}	
	
	public static String [] getJVMOptions(double giga){
		if(giga < 1)
			return new String[]{
				"-Xmx" + new Double(Math.ceil(giga*1024)).intValue() + "M",
				"-Xms" + new Double(Math.ceil(giga*1024)).intValue() + "M"};
		if(giga < 16)
			return new String[]{
				"-Xmx" + new Double(giga).intValue() + "G",
				"-Xms" + new Double(giga).intValue() + "G"};
		else
			return new String[]{
				"-Xmx" + new Double(giga).intValue() + "M",
				"-Xms" + new Double(giga).intValue() + "M"};
	}
	
	public static String [] getJarString(int testType, long startTime, 
			int timeout, int repNum){
		return getJarString(testType, startTime, timeout, -1, repNum);
	}
	
	public static String [] getJarString(int testType, long startTime, 
			int timeout, double target, int repNum){
		String [] jvm = getJVMOptions(Params.test2VMGB);
		ArrayList<String> res = new ArrayList<>(9);
		res.add("java");
		res.add(jvm[0]);
		res.add(jvm[1]);
		res.add("-jar");
		res.add("cycleBreaker.jar");
		res.add(""+0);
		res.add(""+testType);
		res.add(""+startTime);
		res.add(""+timeout);
		if(target >= 0)
			res.add(""+target);
		res.add(""+repNum);
		//System.out.println(res);
		return res.toArray(new String[0]);
	}
	
	// 0 = diag_time, 1 = diag_weight, 2 = diag_card (optional)
	public static double [] parseOutput(String line){
		double [] stats;
		
		String [] elems = line.split(";");
		
		if(elems[0].startsWith("U"))
			return new double[]{-2,-2,-2};
		
		stats = new double[elems.length-1];
		
		for (int i = 0; i < stats.length; i++)
			stats[i] = Double.parseDouble(elems[i+1]);
		
		return stats;
	}
	
	public static double [] launchJar(String [] params){		
		//StringBuilder output = new StringBuilder();
		ProcessBuilder pb = new ProcessBuilder(params);
		Process proc = null;

		double [] stats = null;
		
		try {
			// redirects stderr on stdout
			pb.redirectErrorStream(true);
			proc = pb.start();
			proc.waitFor();
			
			BufferedReader r = new BufferedReader(
					new InputStreamReader(proc.getInputStream()));

			int exitValue = proc.exitValue();
			if(exitValue != 0)
				FileUtil.writeLogAndConsole("Error, exit value: " + exitValue);
			
			try {
				String line = null;
				while(r.ready()){
					line = r.readLine();
					FileUtil.writeLogAndConsole(line);
					if(line.startsWith("EXT;") || line.startsWith("T;") 
							|| line.startsWith("U;")){
						stats = parseOutput(line);
					}
				}
				r.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} catch (InterruptedException | IOException e) {
			e.printStackTrace();
		}
		
		return stats;
	}
	
	public static String stringInfoTest4Bis(LightAdjacencyList adj, 
			LightSCC scc, double cASP, double wASP, double aspTime, 
			double cHeur, double wHeur, double hTime){
		// vtx1, vtx2, M, edges1, edges2, aspWeight, heurWeight, aspTime, heurTime
		
		int [] dim = scc.dimensions(adj);
		Set<LightEdge> m = scc.extractMappings(adj, false);
		double totW = new Diagnosis(m).getWeight();

		String line = dim[0] + " " + dim[1] + " " + dim[2] + " " + dim[3] 
				+ " " + dim[4] + " " + totW + " " + cASP + " " + cHeur + " " 
				+ wASP + " " + wHeur + " " + aspTime + " " + hTime + "\n";
		
		if(dim[2] < cASP || dim[2] < cHeur || totW < wASP || totW < wHeur)
			FileUtil.writeLogAndConsoleNONL("INVALID: " + line);
		
		return line;
	}
	
	public static String stringInfoTest4(LightAdjacencyList adj, LightSCC scc, 
			Diagnosis aspD, long aspTime, Diagnosis hD, long hTime){
		// vtx1, vtx2, M, edges1, edges2, aspWeight, heurWeight, aspTime, heurTime

		//System.out.println("ASP (" + aspD.getTime() + ") = " + aspD + "\nHeur = " + hD);
		double aspDWeight = aspD == null ? -1 : aspD.getWeight();
		long aspDTime = aspD == null ? -1 : aspD.getTime();
		
		int [] dim = scc.dimensions(adj); 
		return dim[0] + " " + dim[1] + " " + dim[2] + " " + dim[3] + " " + 
		dim[4] + " " + aspDWeight + " " + hD.getWeight() + " " + 
		aspDTime + " " + hTime + "\n";
	}

	public static void printAnalysisExp1aToFile(String matcher, 
			PRecOneOneEvaluator...evals){
		try {
			File file = new File(Params.test1Exp1OutPathname);
			//boolean printHeader = !file.exists();
			PrintWriter out = new PrintWriter(new FileWriter(file, true));
			//			if(printHeader)				
			//				out.print(Params.test1Exp1Header);

			out.print(matcher);
			for (PRecOneOneEvaluator eval : evals) {
				out.print(" " + eval.getFound() + 
						" " + eval.getExpected() + 
						" " + eval.getPrecision() + 
						" " + eval.getRecall() +
						" " + eval.getFmeasure() + 
						" " + eval.getOneOneAlign1() +
						" " + eval.getOneOneAlign2()
						);
			}
			out.print("\n");
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void printStatsTest1ToFile(String filename, Map<String, 
			Map<String,Double>> stats) throws IOException{

		// do not output other data when testing for reference, otherwise 
		// reliable data could be overwritten by unreliable ones (with overheads)
		if(Params.test1Reference)
			return;

		String key = stats.keySet().toArray(new String[0])[0];
		String [] keyStats = stats.get(key).keySet().toArray(new String[0]);

		FileWriter outFile = new FileWriter(filename);
		PrintWriter out = new PrintWriter(outFile);

		for (int i = 0; i < keyStats.length; i++)
			out.print(keyStats[i] + (i == keyStats.length-1 ? "\n" : " "));

		for (String align : stats.keySet()) {
			out.print(align + " ");
			for (int i = 0; i < keyStats.length; i++)
				out.print(stats.get(align).get(keyStats[i]) 
						+ (i == keyStats.length-1 ? "\n" : " "));
		}

		out.close();
		outFile.close();
	}

	public static void printStatsTest2ToFile(String filename, 
			List<ResultASPSGA> generalStats) throws IOException{

		FileWriter outFile = new FileWriter(filename);
		PrintWriter out = new PrintWriter(outFile);

		for (ResultASPSGA r : generalStats)
			out.print(r.toString());		

		out.close();
		outFile.close();
	}

	public static void deleteAnalysisTest1() {
		File exp1 = new File(Params.test1Exp1OutPathname);

		if(exp1.exists())
			exp1.delete();
	}

	public static void deleteAllFiles(String dir) {
		File [] tmpFiles = new File(dir).listFiles();
		for (int i = 0; i < tmpFiles.length; i++)
			tmpFiles[i].delete();
	}
}
