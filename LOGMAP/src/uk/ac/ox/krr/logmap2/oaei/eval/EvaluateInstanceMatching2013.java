package uk.ac.ox.krr.logmap2.oaei.eval;

import java.io.File;
import java.util.Calendar;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLOntology;

import uk.ac.ox.krr.logmap2.LogMap2Core;
import uk.ac.ox.krr.logmap2.OntologyLoader;
import uk.ac.ox.krr.logmap2.Parameters;
import uk.ac.ox.krr.logmap2.io.LogOutput;
import uk.ac.ox.krr.logmap2.io.WriteFile;
import uk.ac.ox.krr.logmap2.reasoning.ReasonerAccess;
import uk.ac.ox.krr.logmap2.reasoning.ReasonerManager;
import uk.ac.ox.krr.logmap_lite.LogMap_Lite;

public class EvaluateInstanceMatching2013 {

	private long init, fin, init_intermediate;
	
	static final int PLAY=0;
	static final int RDFT=1;

	boolean logmap=true;
	//logmap lite
	
	//int test = PLAY;
	int test = RDFT;
	
	boolean training = false;
	
	int num_tests;
	
	String base_path;
	String relative_path;
	String base_iri;
	
	
	//Onto 1 is fixed
	String iri_onto1;
	
	
	String str2digits;
	
	String gs_text_file;
	
	String outputfolderpath;
	
	//Onto 2 variable
	String iri_onto2;
	String ontology2match;
	
	String mapping_rel_file;
	
	LogMap2Core logmap2;
	LogMap_Lite logmap_lite;
	
	
	int cases_incons = 0;
	
	double avg_num_mappings=0;
	
	
	double avearge_P=0.0;
	double avearge_R=0.0;
	double avearge_F=0.0;
	
	
	
	public EvaluateInstanceMatching2013(){
		
		
		
		init = Calendar.getInstance().getTimeInMillis();
		
		initStructures();
		
		//LogOutput.showOutpuLog(false);
		//LogOutput.showOutpuLogAlways(false);
		
		Parameters.print_output_always=true;
		
		Parameters.output_class_mappings=false;
		Parameters.output_prop_mappings=false;
		Parameters.output_instance_mappings=true;
		Parameters.perform_instance_matching=true;
		Parameters.reasoner = Parameters.hermit;
		
		try{
			
			String output;
		
			File directory;
			
			for (int folder=1; folder<=num_tests;folder++){//instance
				
				str2digits = convertInteger2TwoDigitString(folder);
				
				iri_onto2 = base_iri + relative_path + "testcase" + str2digits + ontology2match;
				
				//TODO if matching contest check at least if one side is mapped
				//Or compare number of mappings
				gs_text_file = base_path + relative_path + "testcase" + str2digits + mapping_rel_file;
								
				
				System.out.println("Ontology test " + str2digits);
				System.out.println("--------------------------------------------------");
				
				output = outputfolderpath + "/testcase" + str2digits + "/";
				
				directory = new File(output + "/");
				
				if (!directory.exists())
					directory.mkdirs();
				
				if (logmap){
					logmap2 = new LogMap2Core(
							iri_onto1,
							iri_onto2,
							output,
							gs_text_file,
							false); //eval impact
					
					
					
					if (!isIntegratedOntologyConsistent(iri_onto1, iri_onto2, logmap2.getOWLOntology4Mappings()))
						cases_incons++;
					
					
					//TODO Generate output
					if (!training){
						storeMappingsIMFormat(logmap2.getLogMapMappings(), outputfolderpath + "/testcase" + str2digits + "_mappings2.tsv");
					}
					
					
					avg_num_mappings += logmap2.getOWLOntology4Mappings().getABoxAxioms(false).size();
					
					LogOutput.printAlways("\nNUMBER OF INSTANCE MAPPINGS: " + logmap2.getOWLOntology4Mappings().getABoxAxioms(false).size());
					
					avearge_P += logmap2.getPrecision();
					avearge_R += logmap2.getRecall();
					avearge_F += logmap2.getFmeasure();
					
					logmap2.clearIndexStructures();
					logmap2 = null;
					
					System.out.println("\n");
				}
				else{//logmaplite
					
					logmap_lite = new LogMap_Lite(
							iri_onto1,
							iri_onto2,
							gs_text_file,
							output
							); 
					
					
					if (!isIntegratedOntologyConsistent(iri_onto1, iri_onto2, logmap_lite.getOWLMappingsOntology()))
						cases_incons++;
					
					
					//TODO Generate output
					if (!training){
						storeMappingsIMFormat_Lite(logmap_lite.getLogMapLiteMappings(), outputfolderpath + "/testcase" + str2digits + "_mappings2.tsv");
					}
					
					avg_num_mappings += logmap_lite.getOWLMappingsOntology().getABoxAxioms(false).size();
					
					LogOutput.printAlways("\nNUMBER OF INSTANCE MAPPINGS: " + logmap_lite.getOWLMappingsOntology().getABoxAxioms(false).size() + "\n\n");
					
					avearge_P += logmap_lite.getPrecision();
					avearge_R += logmap_lite.getRecall();
					avearge_F += logmap_lite.getFmeasure();
					
					logmap_lite = null;
					
				}
				
			}
			
			
			avearge_P = (double)avearge_P/((double)num_tests);
			avearge_R = (double)avearge_R/((double)num_tests);
			avearge_F = (double)avearge_F/((double)num_tests);
			
			avg_num_mappings = (double)avg_num_mappings/((double)num_tests);
			
			
			
		}
		catch (Exception e){
			e.printStackTrace();
		}
		
		System.out.println("\n\nAverage mappings: " + avg_num_mappings);
		System.out.println("Cases with inconcistency: " + cases_incons);
		System.out.println("Average precision: " + avearge_P);
		System.out.println("Average recall: " + avearge_R);
		System.out.println("Average F-measure: " + avearge_F);
		System.out.println("F-measure from average: " + (2*avearge_R*avearge_P)/(avearge_P+avearge_R));
		
		fin = Calendar.getInstance().getTimeInMillis();
		System.out.println("Done, Time (s): " + (float)((double)fin-(double)init)/1000.0);
		
		
		
	}
	
	
	
	private void initStructures(){
		
		num_tests = 5;
		
		base_path = "/usr/local/data/Instance/InstanceMatching2013/";
		base_iri = "file:" + base_path; //for onto 2
		
		if (training)
			ontology2match="/training/training.owl";
		else
			ontology2match="/contest/contest.owl";
		
		
		if (test==PLAY){
			relative_path = "RDFT_PLAYGROUND_2013/";
			 mapping_rel_file = "/training/mappings.tsv";
		}
		else if (test==RDFT){
			relative_path = "RDFT_DATASET_2013/";
			//mapping_rel_file = "/training/refined_mappings.tsv";
			mapping_rel_file = "/contest/reference.tsv";
		}
		
		
		iri_onto1= base_iri + relative_path + "original.owl";
		
		if (logmap)
			outputfolderpath= base_path + relative_path + "logmap_play";
		else
			outputfolderpath= base_path + relative_path + "logmap_lite_play";
		
	}
	
	
	
	private boolean isIntegratedOntologyConsistent(String iri1, String iri2, OWLOntology M) throws Exception{
		
		OntologyLoader loader1 = new OntologyLoader(iri1);
		OntologyLoader loader2 = new OntologyLoader(iri2);
				 		 
	    //Setting up reasoner
		ReasonerAccess reasonerAccess = ReasonerManager.getMergedOntologyReasoner(
				ReasonerManager.HERMIT, loader1.getOWLOntology(), loader2.getOWLOntology(), M, false);

		
		init_intermediate=Calendar.getInstance().getTimeInMillis();
		boolean isconsistent = reasonerAccess.isConsistent();
		fin=Calendar.getInstance().getTimeInMillis();
					
		LogOutput.printAlways("Time checking consitency (s): " + (float)((double)fin-(double)init_intermediate)/1000.0);
		
		return isconsistent;
		
	}
	
	
	
	
	private String convertInteger2TwoDigitString(int number){
		
		String two_digits = String.valueOf(number);
		
		if (two_digits.length()==1)
			two_digits="0" + two_digits;
		
		return two_digits;
		
	}
	
	
	private void storeMappingsIMFormat(Set<uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr> mappings, String file){
		
		WriteFile writer = new WriteFile(file);
		
		for (uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr mapping : mappings){
			
			//These cases are not included in the reference although represent correct mappings
			//if (mapping.getIRIStrEnt1().contains("http://dbpedia.org/resource/")||mapping.getIRIStrEnt2().contains("http://dbpedia.org/resource/"))
			//	continue;
			
			writer.writeLine(mapping.getIRIStrEnt1() + "\t" + mapping.getIRIStrEnt2() + "\t" + mapping.getConfidence());
		
		}
		
		writer.closeBuffer();
		
	}
	
	private void storeMappingsIMFormat_Lite(Set<uk.ac.ox.krr.logmap_lite.MappingObjectStr> mappings, String file){
		
		WriteFile writer = new WriteFile(file);
		
		for (uk.ac.ox.krr.logmap_lite.MappingObjectStr mapping : mappings){
			
			writer.writeLine(mapping.getIRIStrEnt1() + "\t" + mapping.getIRIStrEnt2() + "\t" + mapping.getConfidence());
			
		}
		
		writer.closeBuffer();
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new EvaluateInstanceMatching2013();
	}

}
