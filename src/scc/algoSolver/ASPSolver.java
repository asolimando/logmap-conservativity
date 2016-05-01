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
package scc.algoSolver;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import enumerations.ENTITY_KIND;
import enumerations.OS;
import scc.exception.UnsatisfiableProblemException;
import scc.util.LegacyFileUtil;
import util.FileUtil;
import util.Params;
import util.Util;
import it.unical.mat.dlv.program.Atom;
import it.unical.mat.dlv.program.Disjunction;
import it.unical.mat.dlv.program.NormalAtom;
import it.unical.mat.dlv.program.Program;
import it.unical.mat.dlv.program.Rule;
import it.unical.mat.dlv.program.SimpleTerm;
import it.unical.mat.wrapper.DLVInputProgram;
import it.unical.mat.wrapper.DLVInputProgramImpl;
import it.unical.mat.wrapper.DLVInvocation;
import it.unical.mat.wrapper.DLVInvocationException;
import it.unical.mat.wrapper.DLVWrapper;
import it.unical.mat.wrapper.Model;
import it.unical.mat.wrapper.ModelBufferedHandler;
import it.unical.mat.wrapper.Predicate;
import it.unical.mat.wrapper.Predicate.ResultLiteral;
import scc.graphDataStructure.Diagnosis;
import scc.graphDataStructure.LightAdjacencyList;
import scc.graphDataStructure.LightEdge;
import scc.graphDataStructure.LightNode;
import scc.graphDataStructure.LightSCC;

public class ASPSolver extends ProblemSolver {

	private boolean customWrapper = true;
	private boolean saveFile;
	private boolean conservativeDiagnosis;
	private ExecutorService executor = Executors.newSingleThreadExecutor();
	private DLVInvocation invocation;
	private int numSCC;
	// process executing DLV
	private Process proc;
	private boolean useDLV;
	private Set<LightEdge> sealedMappings;

	public ASPSolver(LightAdjacencyList adj, LightSCC scc, int numSCC, 
			boolean conservativeDiagnosis, boolean saveFile, boolean useDLV) {
		super(adj, null, scc);
		this.numSCC = numSCC;
		this.conservativeDiagnosis = conservativeDiagnosis;
		this.saveFile = saveFile;
		this.useDLV = useDLV;

		Params.setASPProgramPaths(useDLV);
	}

	public DLVInvocation getInvocation(){
		return invocation;
	}

	public void setSealedMappings(Set<LightEdge> sealedMappings){
		this.sealedMappings = sealedMappings;
	}

	private DLVInputProgram buildAspProgram(){

		DLVInputProgram ipr = new DLVInputProgramImpl();

		Program pr = new Program();
		ArrayList<SimpleTerm> terms = new ArrayList<>();

		// fact for vertices
		for (LightNode n : scc) {
			SimpleTerm vtxName = new SimpleTerm(n.getASPSafeName());
			terms.clear();			
			terms.add(vtxName);
			//			if(newFormat)
			terms.add(new SimpleTerm(n.getOntoId()));

			Atom vtx = new NormalAtom("vtx", terms);
			pr.add(new Rule(new Disjunction(vtx)));
		}

		// fact for edges representing mappings
		for (LightEdge e : scc.extractMappings(adj, true)) {
			SimpleTerm from = new SimpleTerm(e.from.getASPSafeName()), 
					to = new SimpleTerm(e.to.getASPSafeName());
			SimpleTerm conf = new SimpleTerm(Integer.toString((int)(
					e.confidence*Params.confidencePrecisionASP)));
			SimpleTerm isMapping = new SimpleTerm("1");
			terms.clear();

			terms.add(from);
			terms.add(to);
			terms.add(conf);
			terms.add(isMapping);

			NormalAtom edge = new NormalAtom("edge", terms);			
			pr.add(new Rule(new Disjunction(edge)));

			//			if(newFormat)
			//				continue;

			//			NormalAtom mapping = new NormalAtom("mapping");
			//			mapping.addAttribute(edge);
			//			pr.add(new Rule(new Disjunction(mapping)));

			if(sealedMappings != null && sealedMappings.contains(e)){
				NormalAtom notremoved = new NormalAtom("not_removed");
				notremoved.addAttribute(edge);
				pr.add(new Rule(new Disjunction(notremoved)));
			}
		}

		// fact for edges representing axioms
		for (LightEdge e : scc.extractOriginalEdges(adj)) {
			SimpleTerm from = new SimpleTerm(e.from.getASPSafeName()), 
					to = new SimpleTerm(e.to.getASPSafeName());
			SimpleTerm conf = new SimpleTerm("" 
					+ (100 * Params.confidencePrecisionASP)
					);
			SimpleTerm isMapping = new SimpleTerm("0");
			terms.clear();

			terms.add(from);
			terms.add(to);
			terms.add(conf);
			terms.add(isMapping);

			Atom edge = new NormalAtom("edge", terms);
			pr.add(new Rule(new Disjunction(edge)));
		}

		ipr.includeProgram(pr);
		if(useDLV)
			ipr.addFile(conservativeDiagnosis 
					? Params.nonconsAspProgramPath : Params.consAspProgramPath);

		return ipr;
	}

	public Diagnosis computeFiltered() throws InterruptedException, 
	TimeoutException, UnsatisfiableProblemException{
		long startTime = Util.getMSec();
		return aspRunningEngineLinux(buildAspProgram(),startTime,
				Params.filterAspProgramPath);
		//aspRunningEngine(buildAspProgram(),startTime,filterAspProgramPath);
	}

	private Diagnosis aspRunningEngineLinux(DLVInputProgram ipr, long startTime, 
			String progPathname) throws UnsatisfiableProblemException{
		Diagnosis diagnosis = null;
		String name = numSCC + "_" + startTime + "";//Thread.currentThread().getId() + "";
		//System.out.println(numSCC + " " + name);
		String tmpFilename = Params.tmpDir + name + ".dl";
		File tmpProgramFile = new File(tmpFilename);

		tmpProgramFile.getParentFile().mkdirs();

		//				if(tmpProgramFile.exists())
		//					tmpProgramFile.delete();


		// only facts (not merged anymore with program file)
		LegacyFileUtil.writeStringToFile(ipr.getProgram().toString(), tmpFilename);		

		if(saveFile){
			String savedFilename = Params.savedDir + name + ".dl";
			File savedProgramFile = new File(savedFilename);
			savedProgramFile.getParentFile().mkdirs();

			// only facts (not merged anymore with program file)
			LegacyFileUtil.writeStringToFile(ipr.getProgram().toString(), 
					savedFilename);
		}
		tmpProgramFile.deleteOnExit();

		// if CLASP we pass separate fact and program file names
		diagnosis = executeClingoProcess(progPathname + " " + tmpFilename,
				Params.maxAttemptOnError,Params.ASPTimeout);

		return diagnosis;
	}

	private Diagnosis aspRunningEngine(DLVInputProgram ipr, long startTime, 
			String progPathname) 
					throws InterruptedException, TimeoutException, 
					UnsatisfiableProblemException
					{
		ASPThread t = new ASPThread(invocation, ipr, customWrapper, useDLV, 
				progPathname);

		Diagnosis d = null;
		Future<Diagnosis> future = null;

		try {
			future = executor.submit(t);
			d = future.get(Params.ASPTimeout, TimeUnit.SECONDS);

			if(Params.verbosity > 0)
				FileUtil.writeLogAndConsole(numSCC + " ASP diagnosis in " 
						+ (Util.getDiffmsec(startTime)) + " ms");
		} catch (TimeoutException e) {
			//System.out.println("TimeoutException in ASPThread");
			try {
				if(!future.isDone() && !future.isCancelled()){
					future.cancel(true);
					if(!customWrapper){
						invocation.killDlv();
						if(Params.verbosity > 0)
							FileUtil.writeLogAndConsole(numSCC + " DLV killed!");
						throw e;
					}
					else {
						if(!useDLV){
							// if the timeout is so tight that it is not even possible to launch clingo, 
							// no answer sets can be available, the exception is thrown again
							if(proc != null){
								InputStream in = null;
								in = proc.getInputStream();							
								StringBuilder output = new StringBuilder();
								try {
									byte[] data = new byte[in.available()];
									in.read(data);
									output.append(new String(data, "UTF-8"));
								} catch (IOException e1) {
									e1.printStackTrace();
								}

								d = parseAnswerSetsClasp(output.toString(),true,false);
								proc.destroy();
								if(d != null){
									FileUtil.writeLogAndConsole("Partial CLASP diagnosis " + d);
									// if partial, the timeout is reached (convert from sec to msec)
									d.setTime(Params.ASPTimeout * 1000);
									return d;
								}
								else{
									FileUtil.writeLogAndConsole("Impossible to parse partial CLASP diagnosis");

									if(Params.verbosity > 0)
										FileUtil.writeLogAndConsole(output.toString());
								}

							}
						}
						else{
							proc.destroy();
							if(Params.verbosity > 0)
								FileUtil.writeLogAndConsole(numSCC + " CustomDLV killed!");
						}
						throw e;
					}
				}
			} catch (DLVInvocationException e1) {
				e1.printStackTrace();
			}
		} catch (ExecutionException e1) {
			if(e1.getCause() instanceof UnsatisfiableProblemException){
				throw (UnsatisfiableProblemException) e1.getCause();
			}
			else {
				FileUtil.writeLogAndConsole("ExecutionException in ASP Thread");
				e1.printStackTrace();
			}
		} catch (InterruptedException e) {
			try {
				if(Params.verbosity > 0)
					FileUtil.writeLogAndConsoleNONL(numSCC 
							+ " (InterruptedException in ASP Thread) killing DLV... ");
				invocation.killDlv();
				if(Params.verbosity > 0)
					FileUtil.writeLogAndConsole("done!");
			} catch (DLVInvocationException e1) {
				e1.printStackTrace();
			}
		}
		finally {
			if(Params.verbosity > 1)
				FileUtil.writeLogAndConsoleNONL("ASP Thread executor shutdown... ");
			executor.shutdownNow();
			if(Params.verbosity > 1)
				FileUtil.writeLogAndConsole("done!");
		}

		return d;
					} 

	@Override
	public Diagnosis computeDiagnosis() throws InterruptedException, 
	TimeoutException, UnsatisfiableProblemException {
		long startTime = Util.getMSec();
		//		DLVInputProgram ipr = buildAspProgram();

		if(!customWrapper){
			//String dlvPath = "lib/dlv.bin";
			String dlvPath = "lib/dlv.x86-64-linux-elf-static.bin";
			invocation=DLVWrapper.getInstance().createInvocation(dlvPath);
		}
		return aspRunningEngineLinux(buildAspProgram(),startTime,
				conservativeDiagnosis ? Params.nonconsAspProgramPath 
						: Params.consAspProgramPath);
		//		return aspRunningEngine(ipr,startTime,
		//				conservativeDiagnosis ? newAspProgramPath : aspProgramPath);
	}

	class ASPThread implements Callable<Diagnosis> {

		DLVInvocation invocation;
		private DLVInputProgram ipr;
		boolean customWrapper, useDLV;
		String progPathname;

		public ASPThread(DLVInvocation invocation, DLVInputProgram ipr, 
				boolean customWrapper, boolean useDLV, String progPathname){
			this.invocation = invocation;
			this.ipr = ipr;
			this.useDLV = useDLV;
			this.customWrapper = useDLV ? customWrapper : true;
			this.progPathname = progPathname;
		}

		@Override
		public Diagnosis call() throws Exception {
			Diagnosis diagnosis = null;
			if(customWrapper){

				String tmpFilename = Params.tmpDir 
						+ Thread.currentThread().getId() + ".dl";
				File tmpProgramFile = new File(tmpFilename);

				tmpProgramFile.getParentFile().mkdirs();

				//				if(tmpProgramFile.exists())
				//					tmpProgramFile.delete();

				if(useDLV)
					LegacyFileUtil.writeStringToFile(ipr.getProgram().toString() 
							+ "\n\n" + LegacyFileUtil.readStringFromFile(progPathname), 
							tmpFilename);			
				else
					// only facts (not merged anymore with program file)
					LegacyFileUtil.writeStringToFile(ipr.getProgram().toString(), 
							tmpFilename);		

				if(saveFile){
					String savedFilename = Params.savedDir 
							+ Thread.currentThread().getId() + ".dl";
					File savedProgramFile = new File(savedFilename);
					savedProgramFile.getParentFile().mkdirs();

					//					if(savedProgramFile.exists())
					//						savedProgramFile.delete();

					if(useDLV)
						LegacyFileUtil.writeStringToFile(ipr.getProgram().toString() 
								+ "\n\n" + LegacyFileUtil.readStringFromFile(progPathname), 
								savedFilename);			
					else
						// only facts (not merged anymore with program file)
						LegacyFileUtil.writeStringToFile(ipr.getProgram().toString(), 
								savedFilename);
				}
				tmpProgramFile.deleteOnExit();

				// if CLASP we pass separate fact and program file names
				diagnosis = useDLV ? executeDLVProcess(tmpFilename) 
						: executeClingoProcess(progPathname + " " + tmpFilename,
								Params.maxAttemptOnError,0);
			}
			else {
				try {
					//Thread.currentThread().setUncaughtExceptionHandler(new InterrException(this));
					diagnosis = new Diagnosis();

					invocation.setInputProgram(ipr);
					//System.out.println(ipr.getProgram());

					//invocation.addOption("nofacts");
					invocation.setNumberOfModels(1);

					List<String> filters=new ArrayList<>();
					filters.add("removed");

					invocation.setFilter(filters, true);
					ModelBufferedHandler modelBufferedHandler=
							new ModelBufferedHandler(invocation);
					invocation.run();

					while(modelBufferedHandler.hasMoreModels()){
						Model model=modelBufferedHandler.nextModel();
						//				if(model.isBest())
						//					System.out.println("Best model");

						Predicate predicate = model.getPredicate("removed");

						while(predicate.next()){
							ResultLiteral l = predicate.getLiteral();
							diagnosis.add(predicateToMapping(l));
						}
					}

					invocation.waitUntilExecutionFinishes();

					//				List<DLVError> errors = invocation.getErrors();
					//				if(!errors.isEmpty())
					//					System.out.println(errors.toString().replace(",", ",\n"));

				} catch (DLVInvocationException | IOException e2) {
					e2.printStackTrace();
					throw new Error("DLV execution error");
				} catch (IllegalThreadStateException e){
					FileUtil.writeLogAndConsole("IllegalThreadStateException in ASPThread");
				}
			}
			return diagnosis;
		}
	}

	private LightEdge predicateToMapping(ResultLiteral lit){
		String str = lit.toString();
		String edgeStr = str.substring(str.indexOf("edge(")+5, str.length()-2);
		String [] elems = edgeStr.split(",");
		elems[0] = elems[0].trim();
		elems[1] = elems[1].trim();

		LightNode srcNode = aspNameToNode(elems[0], true, false),
				trgNode = aspNameToNode(elems[1], false, false);

		return adj.getEdgeBetweenNodes(srcNode, trgNode);		
	}

	private Diagnosis executeClingoProcess(String filePath,int attemptLeft,
			int timeout) throws UnsatisfiableProblemException {

		String [] paths = filePath.split(" ");
		String[] params = null;

		if(timeout > 0){
			if(Params.os.equals(OS.WIN)){
				params = new String [] {
						"cmd.exe",
						"/c",
						Params.aspBinPath, 
						"--shift",
						"--heuristic="+Params.claspHeuristic,
						paths[0],
						paths[1],
						"&&",
						"timeout","/t","" + timeout,
						"&&",
						"taskkill","/im",Params.aspBinPath,"/f"
				};
			}
			else{
				params = new String [] {
						Params.timeoutBinPath,"--preserve-status","--s","KILL",
						"" + timeout,
						Params.aspBinPath, 
						"--shift",
						"--heuristic="+Params.claspHeuristic,//"Vmtf",//"Vsids",
						//filePath
						paths[0],
						paths[1]
				};
			}
		}
		else{
			if(Params.os.equals(OS.WIN)){
				params = new String [] {
						"cmd.exe",
						"/c",
						Params.aspBinPath, 
						"--shift",
						"--heuristic="+Params.claspHeuristic,
						paths[0],
						paths[1]
				};
			}
			else {
				params = new String [] {
						Params.aspBinPath, 
						"--shift",
						"--heuristic="+Params.claspHeuristic,
						//filePath
						paths[0],
						paths[1]
				};
			}
		}
		//System.out.println(Arrays.toString(params));

		StringBuilder output = new StringBuilder();
		ProcessBuilder pb = new ProcessBuilder(params);
		proc = null;
		boolean partial = false;

		try {
			// redirects stderr on stdout
			pb.redirectErrorStream(true);

			proc = pb.start();
			InputStream in = null;
			in = proc.getInputStream();
			proc.waitFor();

			int exitValue = proc.exitValue();

			String errStr = null;

			switch(exitValue) {			
			case 0: // problem satisfiability unknown, interrupted process 
				//(there may be intermediate models to parse, though)
			case 10: // problem satisfiable, no errors
				break;
			case 1: // problem trivially satisfiable
				FileUtil.writeLogAndConsole("SAT");
				break;
			case 20:
				//errStr = "Unsatisfiable problem, Exit value: " + exitValue;
				throw new UnsatisfiableProblemException();
				//break;
				// sigterm received, if timeout is used it is ok
			case 124:
				if(timeout >= 0){
					partial = true;
					break;
				}
				errStr = "Clingo (ASP): SIGTERM received, " +
						"Exit value: " + exitValue;
				break;
			case 127:
				errStr = "Clingo (ASP): Out of memory error, " +
						"Exit value: " + exitValue;
				attemptLeft = 0; // not a temporary error
				break;
			case 137:
				// when clingo is killed by timeout
				if(timeout >= 0){
					partial = true;
					break;
				}
			default:
				errStr = "Clingo (ASP): Unknown error, Exit value: " 
						+ exitValue + ", ASP file: " + paths[1];
			}
			if(errStr != null){
				FileUtil.writeErrorLogAndConsole(errStr);

				if(attemptLeft > 0){
					FileUtil.writeLogAndConsole("Relaunching, " + --attemptLeft 
							+ " attempt(s) left");
					return executeClingoProcess(filePath, attemptLeft, timeout);
				}
				return null;
			}

			byte[] data = new byte[in.available()];
			in.read(data);
			output.append(new String(data, "UTF-8"));
			in.close();

		} catch (InterruptedException e) {
			//proc.destroy();
			if(Params.verbosity > 0)
				throw new Error("CustomClingo interrupted!");
			//e.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();   
		}

		Diagnosis d = parseAnswerSetsClasp(output.toString(),partial,timeout>=0); 
		
		if(d == null)
			FileUtil.writeLogAndConsole("Null diagnosis, detected OS: " + 
					Params.os + ", process parameters: " + 
						Arrays.toString(params));
		
		return d;
	}

	private Diagnosis executeDLVProcess(String filePath) {

		String[] params = new String [] {
				"lib/dlv.x86-64-linux-elf-static.bin", 
				"-nofacts",
				"-pfilter=removed",
				"-silent",
				"-n=1",
				filePath
		};

		StringBuilder output = new StringBuilder();
		ProcessBuilder pb = new ProcessBuilder(params);
		proc = null;

		try {
			// redirects stderr on stdout
			pb.redirectErrorStream(true);

			proc = pb.start();
			BufferedReader r = new BufferedReader(
					new InputStreamReader(proc.getInputStream()));
			proc.waitFor();

			int exitValue = proc.exitValue();
			if(exitValue != 0){
				if(exitValue != 143)
					FileUtil.writeErrorLogAndConsole("Exit value: " + exitValue);
				else
					return null;
			}
			String str;

			while((str = r.readLine()) != null){
				//System.out.println(str);
				output.append(str);
			}
		} catch (InterruptedException e) {
			proc.destroy();
			if(Params.verbosity > 0)
				throw new Error("CustomDLV interrupted!");
			//e.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();   
		}

		return parseAnswerSets(output.toString());
	}

	private Diagnosis parseAnswerSetsClasp(String out, boolean partial, 
			boolean timeout) throws UnsatisfiableProblemException{
		if(out.startsWith("UNSATISFIABLE"))
			throw new UnsatisfiableProblemException();

		Diagnosis diagnosis = new Diagnosis();

		//System.out.println(out);
		String [] ASets = out.split("\n");

		try {

			// no intermediate models computed
			if(ASets.length < 2 || ASets[1].startsWith("\n*** INTERRUPTED! ***"))
				return null;

			else {
				//Arrays.toString(ASets);

				String model = null;
				int modelOffset = 0;

				if(!partial && ASets[ASets.length-6].contains("yes")){
					diagnosis.setOptimal(true);
					modelOffset = ASets[ASets.length-11].startsWith("Optimization") ? -12 : -11;
				}
				// interrupted
				else if(partial && ASets.length >= 16 &&
						ASets[ASets.length-6].contains("unknown") 
						&& ASets[ASets.length-10].contains("INTERRUPTED") 
						&& ASets[ASets.length-13].startsWith("*** INTERRUPTED! ***")){
					diagnosis.setOptimal(false);
					modelOffset = ASets[ASets.length-15].startsWith("Optimization") ? -16 : -15;
				}
				// unknown state, we try to read a model like for partial
				else
					partial = true;
				//		else if(partial){
				//			if(!ASets[ASets.length-1].contains("Optimization") || ASets.length < 2)
				//				return null;
				//		}
				if(!partial)
					model = (ASets[ASets.length+modelOffset]).trim();
				else if(partial && timeout){
					for (int i = ASets.length-1; i >= 0; --i) {
						model = ASets[i];
						if(model.startsWith("Optimization:") && i>0 
								&& ASets[i-1].startsWith("removed")){
							model = ASets[i-1];
							break;
						}
						// empty diagnosis
						else if("Optimization: 0".equals(ASets[i]))
							return diagnosis;
					}
				}
				else {
					for (int i = ASets.length-2; i >= 0; --i) {
						model = ASets[i];
						if(model.startsWith("removed"))
							break;

						// empty diagnosis
						else if(ASets[i].isEmpty() 
								&& "Optimization: 0".equals(ASets[i+1]))
							return diagnosis;
					}
				}
				if(model == null)
					return null;

				if(!partial){
					//System.out.println(ASets[ASets.length-4]);
					String time = ASets[ASets.length-4].substring(
							ASets[ASets.length-4].indexOf(":")+1).trim();
					//stats.substring(stats.indexOf(":")+1, stats.indexOf("Prepare")-1);
					diagnosis.setTime(new Double(Double.parseDouble(time)*100).longValue());
				}
				else {
					diagnosis.setTime(Params.ASPTimeout*1000);
				}

				// empty diagnosis
				if(model.isEmpty()){
					return diagnosis;
				}

				if(Params.verbosity > 2)
					FileUtil.writeLogAndConsole("Model " + model);

				String [] predicates = model.split(" ");

				String str = null;
				for (int d = 0; d < predicates.length; ++d) {
					str = predicates[d];
					//System.out.println("Pred " + str);

					String edgeStr = str.substring(str.indexOf("edge(")+5, 
							str.length()-2);
					//System.out.println(edgeStr);
					String [] elems = edgeStr.split(",");

					LightNode srcNode = aspNameToNode(elems[0], true, false),
							trgNode = aspNameToNode(elems[1], false, false);

					LightEdge mapping = adj.getEdgeBetweenNodes(srcNode, trgNode);
					//System.out.println(mapping);
					diagnosis.add(mapping);
				}
			}
		}
		catch(java.lang.StringIndexOutOfBoundsException 
				| java.lang.ArrayIndexOutOfBoundsException e){
			FileUtil.writeLogAndConsole(out);
			FileUtil.printStackTrace(e, true);
			return null;
		}

		return diagnosis;
	}

	private LightNode aspNameToNode(String aspName, boolean first, boolean debug){
		if(debug){
			String res = (aspName.startsWith("one") 
					? aspName.replaceFirst("one", "")//"1_") 
							: aspName.replaceFirst("two", ""));//"2_"));

			res.replaceAll("escapeminus", "-");

			return adj.getNodeFromIRI(res,first,ENTITY_KIND.CLASS);
		}
		else {
			return adj.getNodeFromId(aspName.replaceFirst("one", 
					"").replaceFirst("two", ""));
		}
	}

	private Diagnosis parseAnswerSets(String out){
		Diagnosis diagnosis = new Diagnosis();
		//System.out.println(out);
		String model = null;
		if(out.startsWith("Best model")){
			diagnosis.setOptimal(true);
			model = out.substring(out.indexOf('{')+1, out.lastIndexOf('}'));
		}
		//System.out.println(model);
		String [] predicates = model.split("\\),");

		for (String str : predicates) {
			String edgeStr = str.substring(str.indexOf("edge(")+5, str.length()-2);
			//System.out.println(edgeStr);
			String [] elems = edgeStr.split(",");

			LightNode srcNode = aspNameToNode(elems[0], true, false),
					trgNode = aspNameToNode(elems[1], false, false);

			LightEdge mapping = adj.getEdgeBetweenNodes(srcNode, trgNode);
			//System.out.println(mapping);
			diagnosis.add(mapping);
		}		
		return diagnosis;
	}
}
