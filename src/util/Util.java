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

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import enumerations.OS;
import enumerations.REASONER_KIND;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.uncommons.maths.random.MersenneTwisterRNG;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import thread.ExplanationExecutorService;
import thread.OntoClassificationThread;

public final class Util {

	static Runtime runtime = Runtime.getRuntime();
	private static final DateFormat dateFormat = 
			new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	private static final int gcIterations = 2;
	//	private static final int gcWaitTime = 250;
	
	public static List<String> getJVMParams(){
		RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
		List<String> arguments = runtimeMxBean.getInputArguments();
		
		return arguments;
	}
	
	public static int getNumberOfCores(boolean multiThreaded){
		return Runtime.getRuntime().availableProcessors();
	}
	
	public static void logAndPrintThrowable(Throwable e){
		String msg = "Raised exception: " + e.getClass() + "\n";
		FileUtil.writeErrorLogAndConsole("Message: " + msg + ( 
					e.getMessage() != null ? e.getMessage() : "" ) + 
					"\nTrace: " + Arrays.toString(e.getStackTrace()).
								replace("[", "").
								replace("]", "").
								replace(", ", ",\n") 
								);
	}

	public static <T> String prettyPrint(Collection<T> c){
		return c.toString().replace("[", "[\n").replace("]", "\n]").replace(", ", ",\n");
	}
	
	public static <T> List<T> runExplanationCallables(TimeUnit unit, int timeout, 
			Collection<? extends Callable<T>> tasks) 
					throws InterruptedException, ExecutionException, TimeoutException {
		return runCallables(unit, timeout, tasks, 
				new ExplanationExecutorService(getNumberOfCores(false)));
	}

	public static <T> List<T> runCallables(TimeUnit unit, int timeout, 
			Collection<? extends Callable<T>> tasks) 
					throws InterruptedException, ExecutionException, TimeoutException {
		return runCallables(unit, timeout, tasks, 
				Executors.newFixedThreadPool(getNumberOfCores(false)));
	}
		
	public static <T> List<T> runCallables(TimeUnit unit, int timeout, 
			Collection<? extends Callable<T>> tasks, boolean multiThreaded) 
					throws InterruptedException, ExecutionException, TimeoutException {
		return runCallables(unit, timeout, tasks, 
				Executors.newFixedThreadPool(getNumberOfCores(multiThreaded)));
	}

	public static <T> List<T> runCallables(TimeUnit unit, int timeout, 
			Collection<? extends Callable<T>> tasks, ExecutorService executor) 
					throws InterruptedException, ExecutionException, TimeoutException {

		if (timeout <= 0)
			timeout = Integer.MAX_VALUE;

		List<Future<T>> futures = new ArrayList<>(tasks.size());
		List<T> res = new ArrayList<>(futures.size());
				
		try {
			futures.addAll(executor.invokeAll(tasks, timeout, unit));
		
			executor.shutdown();

			for (Future<T> f : futures)
				res.add(f.get(timeout, unit));
			
		} finally {
			executor.shutdownNow();
		}
		return res;
	}
	
	public static <T> List<T> selectPercentage(
			List<T> c, int percentage){
		Collections.shuffle(c);
		return c.subList(0, c.size() * percentage/100);
	}
	
	public static <T> List<T> randomSample(List<T> items, int percentage){
		Random rnd = new MersenneTwisterRNG();
		int m = items.size() * percentage / 100;
	    List<T> res = new ArrayList<T>(m);
	    int n = items.size();
	    for(int i=n-m;i<n;i++){
	        int pos = rnd.nextInt(i+1);
	        T item = items.get(pos);
	        if (res.contains(item))
	            res.add(items.get(i));
	        else
	            res.add(item);
	    }
	    return res;
	}
	
	public static void filterOutliers(ArrayList<Double> x, double avg){
		double stdDev = Util.getStdDev(avg, x);
		//, double median = getMedian(x);
		int preSize = x.size();

		for (int i = 0; i < x.size(); i++)
			if(x.get(i) > (2.7*stdDev) || x.get(i) < (-2.7*stdDev) )
				x.remove(i);

		if(x.size() < preSize)
			FileUtil.writeLogAndConsole(preSize - x.size() + " outlier(s) filtered");
	}

	public static <T extends Object> float getPrecision(Collection<T> comp, Collection<T> exp){
		float num = computeIntersection(comp,exp).size();
		return num / comp.size();
	}

	public static <T extends Object> float getRecall(Collection<T> comp, Collection<T> exp){
		float num = computeIntersection(comp,exp).size();
		return num / exp.size();
	}

	public static <T extends Object> float getFMeasure(Collection<T> comp, Collection<T> exp){
		float prec = getPrecision(comp,exp);
		float recall = getRecall(comp,exp);
		return (2 * prec * recall) / (prec + recall);
	}

	public static <T extends Object> boolean equivalent(Collection<T> a, 
			Collection<T> b){
		return a.containsAll(b) && b.containsAll(a);
	}
	
	public static double getPercentage(double a, double b){
		return a/b*100;
	}

	public static <T extends Object> Set<T> computeIntersection(
			Collection<T> a, Collection<T> b){
		Set<T> res = new HashSet<>();

		for (T object : b) {
			if(a.contains(object))
				res.add(object);
		}
//		for (T object : a) {
//			if(b.contains(object))
//				res.add(object);
//		}		
		return res;
	}

	//	public static double getMedian(ArrayList<Double> x) {
	//		Collections.sort(x);
	//		
	//	    int factor = x.length - 1;
	//	    double[] first = new double[(double) factor / 2];
	//	    double[] last = new double[first.length];
	//	    double[] middleNumbers = new double[1];
	//	    for (int i = 0; i < first.length; i++) {
	//	        first[i] = x.get(i);
	//	    }
	//	    for (int i = x.size(); i > last.length; i--) {
	//	        last[i] = x.get(i);
	//	    }
	//	    for (int i = 0; i <= x.size(); i++) {
	//	        if (x.get(i) != first[i] || x.get(i) != last[i]) middleNumbers[i] = x.get(i);
	//	    }
	//	    if (x.size() % 2 == 0) {
	//	        double total = middleNumbers[0] + middleNumbers[1];
	//	        return total / 2;
	//	    } else {
	//	        return middleNumbers[0];
	//	    }
	//	}

	public static double getAvg(ArrayList<Double> x){
		double tot = 0;
		for (int i = 0; i < x.size(); i++)
			tot += x.get(i);
		return tot/x.size();
	}

	public static double getStdDev(double avg, ArrayList<Double> x){
		double sd = 0;
		for (int i=0; i<x.size();i++)
			sd += Math.pow(x.get(i) - avg, 2);
		return Math.sqrt(sd/(x.size()-1));
	}

	public static double getUsedMemory(boolean callGC, boolean pre, int sleepMs){
		if(callGC == false)
			return getUsedMemory();

		if(pre){
			for (int i = 0; i < gcIterations; i++) {
				System.gc();
				try {
					Thread.sleep(sleepMs);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		double mem = (runtime.totalMemory() - runtime.freeMemory())/1024/1024;

		if(!pre){
			for (int i = 0; i < gcIterations; i++) {
				System.gc();
				try {
					Thread.sleep(sleepMs);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		return mem;
	}

	public static double getUsedMemory(){
		return (runtime.totalMemory() - runtime.freeMemory())/1024/1024;
	}

	public static long getNanoSec(){
		return System.nanoTime();
	}

	// you always need nanoTime because it measures elapsed-time (no 
	// skew corrections), currentTimeMillis measures wall-clock time!
	public static long getMSec(){
		return getNanoSec()/1000/1000;
		//return System.currentTimeMillis();
	}

	public static long getDiffmsec(long start){
		//return ((System.nanoTime() - start)/1000);
		return getMSec() - start;
	}

	public static String getCurrTime(){
		return dateFormat.format(Calendar.getInstance().getTime());
	}

	public static String getCompactCurrTime(){
		return new SimpleDateFormat("yyyyMMdd_HHmmss").format(
				Calendar.getInstance().getTime());
	}
	
	public static int getPercentageFreeMem(){
		return new Long(runtime.freeMemory() / runtime.maxMemory()).intValue();
	}

	public static void getUsedMemoryAndCleanLight(int minMB, int sleepMs) {
		if(getUsedMemory() < minMB)
			return;

		FileUtil.writeLogAndConsole("Used Memory (pre): " + getUsedMemory());
		FileUtil.writeLogAndConsole("Used Memory (post): " + getUsedMemory(true,true,sleepMs));
	}

	public static void getUsedMemoryAndClean(int minMB, int sleepMs) {
		if(getUsedMemory() < minMB)
			return;

		FileUtil.writeLogAndConsole("Used Memory (pre): " + getUsedMemory(false,false,sleepMs));
		FileUtil.writeLogAndConsole("Used Memory (post): " + getUsedMemory(true,true,sleepMs));
	}

	public static double getDiffmsec(double startTime) {
		return getDiffmsec(new Double(startTime).longValue());
	}

	/**
	 * From http://en.algoritmy.net/article/40549/Counting-sort
	 * Counting sort with complexity in O(range + n), suited for small ranges
	 * @param array array to be sorted
	 * @return array sorted in ascending order
	 */
	public static int[] countingSort(int[] array) {
		// array to be sorted in, this array is necessary
		// when we sort object datatypes, if we don't, 
		// we can sort directly into the input array     
		int[] aux = new int[array.length];

		// find the smallest and the largest value
		int min = array[0];
		int max = array[0];
		for (int i = 1; i < array.length; i++) {
			if (array[i] < min) min = array[i];
			else if (array[i] > max) max = array[i];
		}

		// init array of frequencies
		int[] counts = new int[max - min + 1];

		// init the frequencies
		for (int i = 0;  i < array.length; i++) {
			counts[array[i] - min]++;
		}

		// recalculate the array - create the array of occurences
		counts[0]--;
		for (int i = 1; i < counts.length; i++) {
			counts[i] = counts[i] + counts[i-1];
		}

		// Sort the array right to the left
		// 1) look up in the array of occurences the last occurence of the given value
		// 2) place it into the sorted array
		// 3) decrement the index of the last occurence of the given value
		// 4) continue with the previous value of the input array (goto: 1), terminate if all values were already sorted
		for (int i = array.length - 1; i >= 0; i--) {
			aux[counts[array[i] - min]--] = array[i];
		}

		return aux;
	}
		 
	public static OS getOS() {
		
		String osStr = System.getProperty("os.name", 
				"generic").toLowerCase(Locale.ENGLISH);
		
		OS os = OS.UNKNOWN;

		if (osStr.contains("mac") || osStr.contains("darwin"))
			os = OS.MACOS;
		else if (osStr.contains("win"))
			os = OS.WIN;
		else if (osStr.contains("nux"))
			os = OS.LINUX;
				
		if(os.equals(OS.UNKNOWN)){// || os.equals(OS.WIN)){
			System.err.println("Sorry but the detected OS (" 
					+ os.toString() + ") is not supported at the moment");
			System.exit(-1);
		}
		
		return os;
	}
}
