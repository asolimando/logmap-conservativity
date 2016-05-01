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
package util;

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
import java.util.Set;

import ontology.AxiomExplanation;

import org.semanticweb.owlapi.model.OWLAxiom;

public class FileUtil {

	private static FileWriter dataFW, logFW;
	private static PrintWriter dataOutFile, logFile;
	private static boolean dataEnabled = false;
	private static StringBuffer buffer;
	
	public static void enableDataOutFileBuffering() {
		buffer = new StringBuffer();
	}

	public static void flushWriteDataOutFile() {
		if(testDataOutFile()){
			dataOutFile.print(buffer.toString());
			flushDataFile();
		}
		buffer = null;
	}
	
	public static void printStackTrace(Throwable e, boolean consoleAlso) {
		e.printStackTrace(logFile);
		
		if(consoleAlso)
			e.printStackTrace(System.err);
	}
	
	public static void closeFiles() throws IOException {
		dataOutFile.close();
		dataFW.close();
		logFile.close();
		logFW.close();
		dataEnabled = false;
	}
	
	public static void disableDataOutput(){
		dataEnabled = false;
	}
	
	public static void enableDataOutput(){
		dataEnabled = true;
	}
	
	public static void createTestDataFile(String path) throws IOException{
		dataFW = new FileWriter(path);
		dataOutFile = new PrintWriter(dataFW);
		dataEnabled = true;
	}
	
	public static void createLogFile(String path) throws IOException{
		logFW = new FileWriter(path);
		logFile = new PrintWriter(logFW);
		
		FileUtil.writeLogAndConsole("JVM Params: " + Util.getJVMParams());
	}
	
	public static boolean testDataOutFile(){
		return dataOutFile != null && dataEnabled && !dataOutFile.checkError();
	}
	
	public static boolean testLogFile(){
		return logFile != null && !logFile.checkError();
	}
	
	public static void flushDataFile(){
		if(testDataOutFile())
			dataOutFile.flush();
	}
	
	public static void flushLogFile(){
		if(testLogFile())
			logFile.flush();
	}
	
	public static void flushFiles(){
		flushDataFile();
		flushLogFile();
	}
	
	public static void writeDataOutFile(String s, boolean flush){
		if(buffer != null){
			buffer.append(s);
		}
		else {
			if(testDataOutFile()){
				dataOutFile.print(s);
				if(flush)
					flushDataFile();
			}
		}
	}
	
	public static void writeDataOutFileNL(String s){
		writeDataOutFileNL(s, false);
	}
	
	public static void writeDataOutFileNL(String s, boolean flush){
		writeDataOutFile(s+"\n", flush);
	}
	
	public static void writeDataOutFile(String s){
		writeDataOutFile(s, false);
	}
	
	public static void writeErrorLogAndConsole(String s){
		writeLog(s,true,System.err,true);
	}
	
	public static void writeLogAndConsole(String s){
		writeLog(s,true,System.out,true);
	}
	
	public static void writeErrorLogAndConsoleNONL(String s){
		writeLog(s,true,System.err,false);
	}
	
	public static void writeLogAndConsoleNONL(String s){
		writeLog(s,true,System.out,false);
	}

	public static void writeLog(String s){
		writeLog(s,false,System.out,true);
	}
	
	public static void writeErrorLog(String s){
		writeLog(s,false,System.err,true);
	}
	
	public static void writeLog(String s, boolean console, 
			PrintStream consStream, boolean newline){
		if(console && !Params.suppressConsole)
			if(newline)
				consStream.println(s);
			else
				consStream.print(s);
		if(testDataOutFile()){
			if(newline)
				logFile.println(s);
			else
				logFile.print(s);
			logFile.flush();			
		}
	}

	
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

	public static void deleteAllFiles(String dir) {
		File [] tmpFiles = new File(dir).listFiles();
		for (int i = 0; i < tmpFiles.length; i++)
			tmpFiles[i].delete();
	}

	public static String removeExtension(String filename) {
		int lastChar = filename.lastIndexOf('.');
		return filename.substring(0,lastChar > 0 ? lastChar : filename.length());
	}
	
	public static String removePath(String filename) {
		int firstChar = filename.lastIndexOf('/');
		return filename.substring(firstChar > 0 ? firstChar+1 : 0);
	}
	
	public static String getFilenameNoExtensionFromFullPath(String filename) {
		return removePath(removeExtension(filename));
	}
}
