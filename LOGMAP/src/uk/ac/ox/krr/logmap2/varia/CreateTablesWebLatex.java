package uk.ac.ox.krr.logmap2.varia;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import uk.ac.ox.krr.logmap2.io.ReadFile;


public class CreateTablesWebLatex {

	
	String file;

	TreeSet<ResultObject> orderedObjectsScores = 
			new TreeSet<ResultObject>(new ResultObjectComparator());
	
	TreeSet<ResultObjectTimes> orderedObjectsTime = 
			new TreeSet<ResultObjectTimes>(new ResultObjectComparatorTimes());
	
	
	Set<String> files = new HashSet<String>();
	
	Map<String, ResultObject> tools2object = new HashMap<String, ResultObject>();
	
	
	
	CreateTablesWebLatex() throws Exception{
		
		int output;
		
		output=3;
		
		if (output==0){ //scores
			//Tables with scores
			//file = "/home/ernesto/OM_OAEI/data_tables_results_2013/fma2nci_1.txt";
			//file = "/home/ernesto/OM_OAEI/data_tables_results_2013/fma2nci_2.txt";
			
			//file = "/home/ernesto/OM_OAEI/data_tables_results_2013/fma2snomed_1.txt";					
			//file = "/home/ernesto/OM_OAEI/data_tables_results_2013/fma2snomed_2.txt";
			
			//file = "/home/ernesto/OM_OAEI/data_tables_results_2013/snomed2nci_1.txt";
			file = "/home/ernesto/OM_OAEI/data_tables_results_2013/snomed2nci_2.txt";
			
			loadObjectsScores();
			printObjectsHTML_Scores();
		}
		else if (output==1){ //times
			//Table runtimes
			file = "/home/ernesto/OM_OAEI/data_tables_results_2013/runtimes.txt";
			loadObjectsTime();
			printObjectsHTML_Time();
		}
		else if (output==3){ //totals
			
			files.add("/home/ernesto/OM_OAEI/data_tables_results_2013/fma2nci_1.txt");
			files.add("/home/ernesto/OM_OAEI/data_tables_results_2013/fma2nci_2.txt");
			
			files.add("/home/ernesto/OM_OAEI/data_tables_results_2013/fma2snomed_1.txt");					
			files.add("/home/ernesto/OM_OAEI/data_tables_results_2013/fma2snomed_2.txt");
			
			files.add("/home/ernesto/OM_OAEI/data_tables_results_2013/snomed2nci_1.txt");
			files.add("/home/ernesto/OM_OAEI/data_tables_results_2013/snomed2nci_2.txt");			
			
			loadObjectsScores_All();
			
			printObjectsHTML_AllAvg();
			
		}
		
	}
	
	
	private void loadObjectsScores_All() throws Exception{
		
		for (String f : files){
			
			ReadFile reader = new ReadFile(f);
						
			String line;
			String[] elements;
			
			line=reader.readLine();
			
			
			while (line!=null) {
				
				if (line.startsWith("#")){
					line=reader.readLine();
					continue;
				}	
				
				if (line.indexOf("|")<0 && line.indexOf("\t")<0){
					line=reader.readLine();
					continue;
				}
				
				if (line.indexOf("|")>=0)
					elements=line.split("\\|");
				else { // if (line.indexOf("\t")>=0){
					elements=line.split("\\t");
				}
				
				String tool = elements[0];
				
				if (!tools2object.containsKey(tool))
					tools2object.put(tool, new ResultObject(tool));
				
				//0 Tool	1 Time (ms)	2 Precision	3 Recall	4 F-score	5 Time (s) 6 mappings 7 unsat? 8 degree? 9 reasoner
				tools2object.get(tool).updateAverage(
						Long.valueOf(elements[5]), 
						Double.valueOf(elements[2]),
						Double.valueOf(elements[3]),
						Double.valueOf(elements[4]), 
						Integer.valueOf(elements[6]),
						Long.valueOf(elements[7]),
						Double.valueOf(elements[8]));
				
				
				
				line=reader.readLine();
			}		
			
			reader.closeBuffer();
			
			
		}//for files
		
		
		for (String tool : tools2object.keySet()){
			
			if (tools2object.get(tool).n4avg<6)
				continue;
			
			tools2object.get(tool).precision = tools2object.get(tool).precision / (double)tools2object.get(tool).n4avg;
			tools2object.get(tool).recall = tools2object.get(tool).recall / (double)tools2object.get(tool).n4avg;
			tools2object.get(tool).fscore = tools2object.get(tool).fscore / (double)tools2object.get(tool).n4avg;
			//tools2object.get(tool).mappings = tools2object.get(tool).mappings / (double)tools2object.get(tool).n4avg;
			//tools2object.get(tool).unsat = tools2object.get(tool).unsat / (double)tools2object.get(tool).n4avg;
			tools2object.get(tool).degreee = tools2object.get(tool).degreee / (double)tools2object.get(tool).n4avg;
		
			
			orderedObjectsScores.add(tools2object.get(tool));
			
			
		}
		
		
			
			
			
		
		
		
		
		
	}
	
	
	private void loadObjectsScores() throws Exception{
	
		ReadFile reader = new ReadFile(file);
		
		
		String line;
		String[] elements;
		
		line=reader.readLine();
		
		
		while (line!=null) {
			
			if (line.startsWith("#")){
				line=reader.readLine();
				continue;
			}	
			
			if (line.indexOf("|")<0 && line.indexOf("\t")<0){
				line=reader.readLine();
				continue;
			}
			
			if (line.indexOf("|")>=0)
				elements=line.split("\\|");
			else { // if (line.indexOf("\t")>=0){
				elements=line.split("\\t");
			}
			//0 Tool	1 Time (ms)	2 Precision	3 Recall	4 F-score	5 Time (s) 6 mappings 7 unsat? 8 degree? 9 reasoner 
			
			if (elements.length==9){
				orderedObjectsScores.add(
						new ResultObject(
							elements[0], 
							Long.valueOf(elements[5]), 
							Double.valueOf(elements[2]),
							Double.valueOf(elements[3]),
							Double.valueOf(elements[4]), 
							Integer.valueOf(elements[6]),
							Long.valueOf(elements[7]),
							Double.valueOf(elements[8]))
						);
			}
			else if (elements.length==10){
					orderedObjectsScores.add(
							new ResultObject(
								elements[0], 
								Long.valueOf(elements[5]), 
								Double.valueOf(elements[2]),
								Double.valueOf(elements[3]),
								Double.valueOf(elements[4]), 
								Integer.valueOf(elements[6]),
								Long.valueOf(elements[7]),
								Double.valueOf(elements[8]),
								Boolean.valueOf(elements[9]))
							);
			}
			else{
				orderedObjectsScores.add(
						new ResultObject(
							elements[0], 
							Long.valueOf(elements[5]), 
							Double.valueOf(elements[2]) ,
							Double.valueOf(elements[3]),
							Double.valueOf(elements[4]))
						);
			}
				
			line=reader.readLine();
		}		
		
		reader.closeBuffer();
		

	}
	
	
	private void loadObjectsTime() throws Exception{
		
		ReadFile reader = new ReadFile(file);
		
		
		String line;
		String[] elements;
		
		line=reader.readLine();
		
		
		List<Integer> task_times = new ArrayList<Integer>(); 
		
		
		while (line!=null) {
			
			if (line.startsWith("#")){
				line=reader.readLine();
				continue;
			}	
			
			if (line.indexOf("|")<0 && line.indexOf("\t")<0){
				line=reader.readLine();
				continue;
			}
			
			if (line.indexOf("|")>=0)
				elements=line.split("\\|");
			else { // if (line.indexOf("\t")>=0){
				elements=line.split("\\t");
			}
			
			
			for (int i=1; i< elements.length; i++){
				if (Integer.valueOf(elements[i])>=0)
					task_times.add(Integer.valueOf(elements[i])/1000);
				else
					task_times.add(Integer.valueOf(elements[i]));
			}
			
			//create object
			orderedObjectsTime.add(
					new ResultObjectTimes(
							elements[0], //tool
							task_times   //times
							));
			
			task_times.clear();;
			line=reader.readLine();
		}		
		
		reader.closeBuffer();
		

	}
	
	
	
	private void printObjectsHTML_AllAvg(){
		
		Iterator<ResultObject> it = orderedObjectsScores.iterator();
		
		ResultObject object;
		
		//odd or even
		String type_line;
		
		
		int i=1;
		
		
		
		System.out.println("<table cellpadding=\"4\" cellspacing=\"0\">");


		System.out.println("<tr class=\"header\">");
		System.out.println("<td  class=\"header\" rowspan=\"2\" colspan=\"1\"> System </td>");
		System.out.println("<td  class=\"header\" rowspan=\"2\" colspan=\"1\"> Total Time (s) </td>");
		System.out.println("<td  class=\"header\" colspan=\"4\"> Average </td>"); 
		System.out.println("</tr>");


		System.out.println("<tr class=\"header\">");
		System.out.println("<td  class=\"header\"> Precision </td>");
		System.out.println("<td  class=\"header\"> &nbsp;Recall&nbsp; </td>"); 
		System.out.println("<td  class=\"header\"> F-measure </td>");
		System.out.println("<td class=\"header\"> Incoherence</td>");
		System.out.println("</tr>");

		
		
		
		//Compute averages
		while (it.hasNext()){
			object = it.next();
			
			if ( i % 2 == 0 ) { type_line="even"; } else { type_line="odd"; }
		
			
			
			if (object.tool.contains("SBK")){
				type_line="blue";
			}
			
			 
			System.out.println("<tr class=\""+ type_line + "\">");
			if (object.tool.contains("Average"))
				System.out.println("<td class=\"header\">"+object.tool+"</td>");
			else {
				if (object.tool.contains("GOMMA")){
					System.out.println("<td class=\"text\">"+object.tool+"<sub>2012</sub></td>");
				}
				else{
					System.out.println("<td class=\"text\">"+object.tool+"</td>");
				}
			
			}
			
			
			System.out.format("<td> %,d </td>", object.time);
			
			
			System.out.format("<td> %.3f </td> ", object.precision);			
			System.out.format("<td> %.3f </td> ", object.recall);			
			System.out.format("<td> %.3f </td>%n", object.fscore);
			
			
			if (object.unsat>=0){
				//System.out.format("<td> %,d </td>", object.unsat);
				
				if (object.degreee<0.1)
					
					//System.out.println("<td class=\"right\">" + object.degreee + "&#37</td> ");
					System.out.format("<td class=\"right\"> %.3f&#37</td>", object.degreee);
				else
					System.out.format("<td class=\"right\"> %.1f&#37</td>", object.degreee);
			}
			
			
			
			System.out.println("</tr>");
			
			System.out.println("\n");
			
			i++;
		}
		
		
		System.out.println("</table>");

		
	}
	
	
	
	private void printObjectsHTML_Scores(){
		
		Iterator<ResultObject> it = orderedObjectsScores.iterator();
		
		ResultObject object;
		
		//odd or even
		String type_line;
		
		
		int i=1;
		
		
		String prefix;
		
		long avg_time = 0;
		int avg_map = 0;
		double avg_p = 0.0;
		double avg_r = 0.0;
		double avg_f = 0.0;
		long avg_u = 0;
		double avg_d = 0.0;
		
		//Compute averages
		while (it.hasNext()){
			object = it.next();
			
			avg_time += object.time;
			avg_map += object.mappings;
			avg_p += object.precision;
			avg_r += object.recall;
			avg_f += object.fscore;
			avg_u += object.unsat;
			avg_d += object.degreee;
			
			
			i++;
		
		}
		
		//Averages
		avg_time = avg_time /  i;
		avg_map = avg_map /  i;
		avg_p = avg_p / (double) i;
		avg_r = avg_r / (double) i;
		avg_f = avg_f / (double) i;
		avg_u = avg_u /  i;
		avg_d = avg_d / (double) i;
		
		
		orderedObjectsScores.add(
				new ResultObject(
					"Average", 
					avg_time, 
					avg_p,
					avg_r,
					avg_f, 
					avg_map,
					avg_u,
					avg_d)
				);
			
		
		i=1;
		it = orderedObjectsScores.iterator();
		
		while (it.hasNext()){
			
			
			object = it.next();
			
			
			if ( i % 2 == 0 ) { type_line="even"; } else { type_line="odd"; }
			
			if (object.tool.contains("Average")){//object.tool.equals("LogMapLt") || 
				type_line="base";
			}
			
			if (object.tool.contains("SBK")){
				type_line="blue";
			}
			
			 
			System.out.println("<tr class=\""+ type_line + "\">");
			if (object.tool.contains("Average"))
				System.out.println("<td class=\"header\">"+object.tool+"</td>");
			else {
				if (object.tool.contains("GOMMA")){
					System.out.println("<td class=\"text\">"+object.tool+"<sub>2012</sub></td>");
				}
				else{
					System.out.println("<td class=\"text\">"+object.tool+"</td>");
				}
			
			}
			//System.out.println("<td>" + object.time + "</td> ");
			//http://docs.oracle.com/javase/tutorial/java/data/numberformat.html
			
			System.out.format("<td> %,d </td>", object.time);
			//System.out.print("<td>" + object.mappings + "</td> ");
			if (object.mappings>0)
				System.out.format("<td> %,d </td>%n", object.mappings);
			else
				System.out.println("<td> - </td>");
			//System.out.print("<td>" + object.precision + "</td> ");
			System.out.format("<td> %.3f </td> ", object.precision);
			//System.out.print("<td>" + object.recall + "</td> ");
			System.out.format("<td> %.3f </td> ", object.recall);
			//System.out.println("<td class=\"bold\">" + object.fscore + "</td> ");
			System.out.format("<td class=\"bold\"> %.3f </td>%n", object.fscore);
			
			//System.out.println("<td>" + object.unsat + "</td> ");
			
			prefix = "";
			if (!object.complete_reasoner){
				prefix = "&ge;";
			}
			
			
			if (object.unsat>=0){
				System.out.format("<td> "+prefix+"%,d </td>", object.unsat);
				//System.out.println("<td class=\"right\">" + object.degreee + "&#37</td> ");
				if (object.degreee<0.1)
					//System.out.format("<td class=\"right\"> %.f&#37</td>", object.degreee);
					System.out.println("<td class=\"right\">" + prefix + object.degreee + "&#37</td> ");
				else
					System.out.format("<td class=\"right\"> " + prefix + "%.1f&#37</td>", object.degreee);
			}
			else{
				System.out.print("<td> - </td>");
				System.out.println("<td> - </td>");
			}
			System.out.println("</tr>");
			
			System.out.println("\n");
			
			i++;
			
			
			
			/*
			<tr class="odd">
			<td class="text">Tool</td> 
			<td>1,304</td> <td> 2,738 </td> 
			<td>0.907</td> <td>0.821</td> <td class="bold">0.862</td> 
			<td>50,550</td> <td class="right">28.56&#37</td> 
			</tr>
			*/
		}
		
		
		
		
		
		/*System.out.println("<tr class=\"base\">");
		System.out.println("<td class=\"header\">Average #</td>");
		
		//http://docs.oracle.com/javase/tutorial/java/data/numberformat.html
		
		System.out.format("<td class=\"header\"> %,d </td>", avg_time);
		if (avg_map>0)
			System.out.format("<td class=\"header\"> %,d </td>%n", avg_map);
		else
			System.out.println("<td class=\"header\"> - </td>");
		
		System.out.format("<td class=\"header\"> %.3f </td> ", avg_p);
		System.out.format("<td class=\"header\"> %.3f </td> ", avg_r);
		System.out.format("<td class=\"header\"> %.3f </td>%n", avg_f);
		
		
		if (avg_u>0){
			System.out.format("<td class=\"header\"> %,d </td>", avg_u);
			System.out.println("<td class=\"header\">" + avg_d + "&#37</td> ");
		}
		else{
			System.out.print("<td class=\"header\"> - </td>");
			System.out.println("<td class=\"header\"> - </td>");
		}
		
		System.out.println("\n");
		*/
		
		
	}
	
	
	private void printObjectsLatex_Scores(){
		
	}
	
	private void printObjectsLatex_Time(){
		
	}
	
	private void printObjectsHTML_Time(){
	
			
		Iterator<ResultObjectTimes> it = orderedObjectsTime.iterator();
		
		ResultObjectTimes object;
		
		int i=1;
		
		String type_line;
		
		
		
		
		
		while (it.hasNext()){
			object = it.next();
			
			
			if ( i % 2 == 0 ) { type_line="even"; } else { type_line="odd"; }
			
			
			System.out.println("<tr class=\""+ type_line + "\">");
			
			if (object.tool.contains("GOMMA")){
				System.out.println("<td class=\"text\">"+object.tool+"<sub>2012</sub></td>");
			}
			else if (object.tool.contains("SPHeRe")){
				System.out.println("<td class=\"text\">"+object.tool+" (*)");
			}
			else{
				System.out.println("<td class=\"text\">"+object.tool+"</td>");
			}
			
			for (int time : object.task_times){
				
				if (object.tool.equals("SPHeRe")){
					System.out.print("<td class=\"italic\">");
				}
				else{
					System.out.print("<td>");
				}
				if (time>0)
					System.out.format("%,d </td>%n", time);
				else
					System.out.println("-  </td>");
			}
			
			
			//System.out.println("<td class=\"header\">"+object.average_time+"</td>");
			System.out.format("<td class=\"header\"> %,.0f </td>%n", object.average_time);
			
			System.out.println("<td class=\"header\">"+object.completed+"</td>");
			
			System.out.println("</tr>");
			System.out.println("\n");
			
			i++;
		}
		
		
		
		
		//Add summary row?
		double average_total = 0.0;
		int completed_total = 0; 
		int[] num_complete_task = new int[6];
		for (int j=0; j<num_complete_task.length; j++){
			num_complete_task[j]=0;
		}
		
		for (ResultObjectTimes objTimes : orderedObjectsTime){
			average_total += objTimes.average_time;
			completed_total += objTimes.completed;
			
			for (int j=0; j<objTimes.task_times.size(); j++){
				//num_complete_task.
				if (objTimes.task_times.get(j)>=0){
					num_complete_task[j]++;
				}
			}
		}
		
		average_total = average_total / (double) orderedObjectsTime.size();
		
		System.out.println("<tr class=\"base\">");
		System.out.println("<td class=\"header\"> # Systems </td> ");
		
		//System.out.println("<td class=\"header\">"+object.average_time+"</td>");
		
		for (int j=0; j<num_complete_task.length; j++){
			System.out.format("<td class=\"header\">%,d </td>%n", num_complete_task[j]);
		}
		
		System.out.format("<td class=\"header\"> %,.0f </td>%n", average_total);
		System.out.println("<td class=\"header\">"+completed_total+"</td>");
		System.out.println("</tr>");
		System.out.println("\n");
		
		
		
		/*
		 * 
		 * 
		 * <tr class="base">	

<td class="header"> # Systems </td> 

<td class="header"> 23 </td> <td class="header"> - </td>
	<td class="header"> 21 </td> <td class="header"> - </td> 
	<td class="header"> - </td> <td class="header"> - </td>
	<td class="header"> - </td> 

</tr>
		 * 
		 * 
		 */
		
		
		
		//Put cursiva to GOMMA??
		
		
		
	}
	
	
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		try {
			new CreateTablesWebLatex();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	
	
	
	
	/**
	 * Comparator fscore
	 * @author Ernesto
	 *
	 */
	private class ResultObjectComparator implements Comparator<ResultObject> {
		
		
		/**

		 * @param m1
		 * @param m2
		 * @return
		 */
		public int compare(ResultObject r1, ResultObject r2) {

			if (r1.fscore < r2.fscore){
				return 1;
			}
			else if (r1.fscore == r2.fscore){
				
				//if (r1.precision < r2.precision){ //do it with unsat?
				if (r1.unsat > r2.unsat){ //do it with unsat?
					return 1;						
				}
				else{
					return -1;
				}
				
			}
			else{
				return -1;
			}
			
				
		}
		
		
	
	}
	
	
	/**
	 * Comparator fscore
	 * @author Ernesto
	 *
	 */
	private class ResultObjectComparatorTimes implements Comparator<ResultObjectTimes> {
		
		
		/**

		 * @param m1
		 * @param m2
		 * @return
		 */
		public int compare(ResultObjectTimes r1, ResultObjectTimes r2) {

			if (r1.completed < r2.completed){
				return 1;
			}
			else if (r1.completed == r2.completed){
				
				if (r1.average_time > r2.average_time){ 
					return 1;						
				}
				else{
					return -1;
				}
				
			}
			else{
				return -1;
			}
			
				
		}
		
		
	
	}
	
	
	
	private class ResultObject  {
	
		public String tool;
		int mappings;
		public long time;
		public double precision;
		public double recall;
		public double fscore;
		public long unsat;
		public double degreee;
		public boolean complete_reasoner;
		
		public int n4avg;
		
		
		ResultObject(String tool){
			
			this.tool = tool;
			mappings = 0;
			time = 0;
			precision = 0.0;
			recall = 0.0;
			fscore = 0.0;
			unsat = 0;
			degreee = 0.0;
			complete_reasoner=true;
			
			n4avg = 0;
			
		}
		
		
		ResultObject(String tool, long t, double p, double r, double f){
			this(tool, t, p, r, f, -1, -1, -1.0);
			
		}
		ResultObject(String tool, long t, double p, double r, double f, int m, long u, double d){
			this(tool, t, p, r, f, m, u, d, true);
		}
		
		ResultObject(String tool, long t, double p, double r, double f, int m, long u, double d, boolean owl2reasoner){
			
			this.tool = tool;
			mappings = m;
			time = t;
			precision =p;
			recall = r;
			fscore = f;
			unsat = u;
			degreee = d;
			complete_reasoner = owl2reasoner;
			
		}
		
		
		public void updateAverage(long t, double p, double r, double f, double m, long u, double d){
			
			mappings += m;
			time += t;
			precision +=p;
			recall += r;
			fscore += f;
			unsat += u;
			degreee += d;
			
			n4avg++;
			
		}
		
		
		
	}
	
	
	private class ResultObjectTimes  {
		
		public String tool;
		
		//Time for each task
		public List<Integer> task_times;
		public double average_time;
		public int completed;
		
		
		ResultObjectTimes(
				String tool, 
				List<Integer> times){
			
			this.tool = tool;
			task_times = new ArrayList<Integer>(times);
			
			completed = 0;
			average_time = 0.0;
			for (int t : task_times){
				if (t>=0){
					completed++;
					average_time += t;
				}
			}
			
			average_time = average_time / (double) completed;
			
			
		}
		
		
	}
	
	

}
