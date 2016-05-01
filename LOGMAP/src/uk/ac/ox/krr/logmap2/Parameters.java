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
package uk.ac.ox.krr.logmap2;

import uk.ac.ox.krr.logmap2.io.ReadFile;
import java.util.Set;
import java.util.HashSet;
import java.io.File;

public class Parameters {

	private static String rdf_label_uri = "http://www.w3.org/2000/01/rdf-schema#label";
	public static String rdf_comment_uri = "http://www.w3.org/2000/01/rdf-schema#comment";
	
	private static String skos_label_uri = "http://www.w3.org/2004/02/skos/core#prefLabel";
	private static String skos_altlabel_uri = "http://www.w3.org/2004/02/skos/core#altLabel";
	
	private static String foaf_name_uri = "http://xmlns.com/foaf/0.1/name";
	
	
	private static String hasRelatedSynonym_uri = "http://www.geneontology.org/formats/oboInOwl#hasRelatedSynonym";
	private static String hasExactSynonym_uri   = "http://www.geneontology.org/formats/oboInOwl#hasExactSynonym";
	private static String nci_synonym_uri = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#Synonym";
	private static String fma_synonym_uri="http://bioontology.org/projects/ontologies/fma/fmaOwlDlComponent_2_0#Synonym";
	private static String hasDefinition_uri="http://www.geneontology.org/formats/oboInOwl#hasDefinition";
	private static String xbrl_label_uri="http://www.xbrl.org/2003/role/label";
	


	
	
	
	
		
	private static String name_dprop_im_uri = "http://oaei.ontologymatching.org/2012/IIMBTBOX/name";
	private static String article_dprop_im_uri = "http://oaei.ontologymatching.org/2012/IIMBTBOX/article";
	
	private static String has_value_dprop_im_uri = "http://www.instancematching.org/IIMB2012/ADDONS#has_value";
		
	private static String article_oprop_im_uri = "http://www.instancematching.org/IIMB2012/ADDONS#article";
	private static String name_oprop_im_uri = "http://www.instancematching.org/IIMB2012/ADDONS#name";
	
	
	//2013
	private static String population_dprop_im_uri = "http://dbpedia.org/ontology/populationTotal";	
	private static String birthName_dprop_im_uri = "http://dbpedia.org/ontology/birthName";	
	
	private static String label_oprop_im_uri = "http://www.instancematching.org/label";
	private static String curriculum_oprop_im_uri = "http://www.instancematching.org/curriculum";
	private static String places_oprop_im_uri = "http://www.instancematching.org/places";
	
	//Other
	private static String abstract_dprop_im_uri = "http://dbpedia.org/ontology/abstract";
	private static String label_dprop_im_uri = "http://dbpedia.org/property/label";
	private static String name2_dprop_im_uri = "http://dbpedia.org/property/name";
		
	
	//2010 URIs
	private static String article_oprop_im_uri_2010 = "http://www.instancematching.org/IIMB2010/ADDONS#name";
	private static String name_oprop_im_uri_2010 = "http://www.instancematching.org/IIMB2010/ADDONS#article";
	
	private static String has_value_dprop_im_uri_2010 = "http://www.instancematching.org/IIMB2010/ADDONS#has_value";
	
	private static String name_dprop_im_uri_2010 = "http://oaei.ontologymatching.org/2010/IIMBTBOX/name";
	private static String article_dprop_im_uri_2010 = "http://oaei.ontologymatching.org/2010/IIMBTBOX/article";
	
	
	
	
	
	
	
	
	
	public static boolean print_output = false; //false;
	public static boolean print_output_always = false; //false;
	
	public static double bad_score_scope = 0.0;
	
	public static double good_isub_anchors = 0.98;
	
	public static double good_isub_candidates = 0.95;
	
	public static double good_confidence = 0.50;
	
	public static double min_conf_pro_map = 0.75;
	
	public static double good_sim_coocurrence = 0.08; //Tested one 0.09
	
	public static int max_ambiguity = 4;
	
	public static int good_ambiguity = 2;
	
	//Note that even if overlapping is set to true. It will only applied for big ontologies >15000 
	public static boolean use_overlapping = true;
	
	public static boolean second_chance_conflicts = true;
	
	public static int ratio_second_chance_discarded = 5;
	
	public static int min_size_overlapping = 15000; //5000
	
	//set to false by default?
	public static boolean perform_instance_matching = true; //true;
	public static boolean output_instance_mappings = true; //true;
	
	public static boolean output_instance_mapping_files = true;
	
	public static boolean output_class_mappings = true;
	public static boolean output_prop_mappings = true;
	
	//TODO Now with ignore types it may be solved this issue!
	public static boolean reason_datatypes = true; //with OM client gives error if true
	
	public static String hermit = "HermiT";
	public static String more = "MORe";
	public static String reasoner = hermit;
	//public static String reasoner = more;
	
	//Timeout reasoner
	public static int timeout = 25;
	
	
	public static boolean output_equivalences_only = false;
	
	public static boolean use_umls_lexicon = true;
	
	public static boolean reverse_labels = false;
	
	public static boolean allow_interactivity = false;

	public static boolean repair_heuristic = true;
	public static int repair_heuristic_threshold_decrease = 5;
	public static int repair_heuristic_threshold_remove = 10;

	public static Set<String> accepted_annotation_URIs_for_classes = new HashSet<String>();
	
	public static Set<String> accepted_data_assertion_URIs_for_individuals = new HashSet<String>();
	public static Set<String> accepted_data_assertion_URIs_for_individuals_deep2 = new HashSet<String>();
	public static Set<String> accepted_object_assertion_URIs_for_individuals = new HashSet<String>();
	
	
	//For thresholds file
	private static final String repair_heuristic_str = "repair_heuristic";
	private static final String repair_heuristic_threshold_remove_str = "repair_heuristic_threshold_remove";
	private static final String repair_heuristic_threshold_decrease_str  = "repair_heuristic_threshold_decrease";
	
	private static final String print_output_str = "print_output";
	
	private static final String bad_score_scope_str = "bad_score_scope";
	
	private static final String good_isub_anchors_str = "good_isub_anchors";
	
	private static final String good_isub_candidates_str = "good_isub_candidates";
	
	private static final String good_confidence_str = "good_confidence";
	
	private static final String good_sim_coocurrence_str = "good_sim_coocurrence";
	
	private static final String min_conf_pro_map_str = "min_conf_pro_map";
	
	private static final String max_ambiguity_str = "max_ambiguity";
	
	private static final String good_ambiguity_str = "good_ambiguity";
	 
	private static final String use_overlapping_str = "use_overlapping";
	
	private static final String min_size_overlapping_str = "min_size_overlapping";
	
	private static final String instance_matching_str = "instance_matching";
	
	private static final String annotation_URI_str = "annotation_URI";
	
	private static final String data_assertion_URI_Indiv_str = "data_assertion_URI_Indiv";
	private static final String data_assertion_URI_Indiv_deep2_str = "data_assertion_URI_Indiv_deep2";
	
	private static final String object_assertion_URI_Indiv_str = "object_assertion_URI_Indiv";
	
	private static final String output_class_mappings_str = "output_class_mappings";
	private static final String output_prop_mappings_str = "output_prop_mappings";
	private static final String output_instance_mappings_str = "output_instance_mappings";
	private static final String output_instance_mapping_files_str = "output_instance_mapping_files";
	
	
	
	private static final String reason_datatypes_str = "reason_datatypes";
	
	private static final String second_chance_conflicts_str = "second_chance_conflicts";
	private static final String ratio_second_chance_discarded_str = "ratio_second_chance_discarded";
	
	
	//reasoner|MORe or HermiT
	private static final String reasoner_str = "reasoner";
	private static final String timeout_str = "timeout";
	
	
	
	private static final String output_equivalences_only_str = "output_equivalences_only";
	
	private static final String use_umls_lexicon_str = "use_umls_lexicon";

	private static final String allow_interactivity_str = "allow_interactivity";
	
	
	private static final String reverse_labels_str = "reverse_labels";
	
	
	
	//Init of default accepted annotation/assertion uris
	static {
		//accepted_annotation_URIs = new HashSet<String>();
		accepted_annotation_URIs_for_classes.add(rdf_label_uri);
		accepted_annotation_URIs_for_classes.add(hasExactSynonym_uri);
		accepted_annotation_URIs_for_classes.add(hasRelatedSynonym_uri);
		accepted_annotation_URIs_for_classes.add(nci_synonym_uri);
		accepted_annotation_URIs_for_classes.add(fma_synonym_uri);
		accepted_annotation_URIs_for_classes.add(hasDefinition_uri);
		accepted_annotation_URIs_for_classes.add(xbrl_label_uri);
		
		accepted_annotation_URIs_for_classes.add(skos_label_uri);
		accepted_annotation_URIs_for_classes.add(skos_altlabel_uri);
		accepted_annotation_URIs_for_classes.add(foaf_name_uri);
		
		
		//OAEI IM 2012
		//Data
		accepted_data_assertion_URIs_for_individuals.add(name_dprop_im_uri);
		accepted_data_assertion_URIs_for_individuals.add(article_dprop_im_uri);
		
		//Data deep2
		accepted_data_assertion_URIs_for_individuals_deep2.add(has_value_dprop_im_uri);
		
		//Object
		accepted_object_assertion_URIs_for_individuals.add(name_oprop_im_uri);
		accepted_object_assertion_URIs_for_individuals.add(article_oprop_im_uri);
		
		//OAEI IM 2010
		//Data
		accepted_data_assertion_URIs_for_individuals.add(name_dprop_im_uri_2010);
		accepted_data_assertion_URIs_for_individuals.add(article_dprop_im_uri_2010);
		
		//Data deep2
		accepted_data_assertion_URIs_for_individuals_deep2.add(has_value_dprop_im_uri_2010);
		
		//Object
		accepted_object_assertion_URIs_for_individuals.add(name_oprop_im_uri_2010);
		accepted_object_assertion_URIs_for_individuals.add(article_oprop_im_uri_2010);
		
		
		//oaei 2013
		//Data
		accepted_data_assertion_URIs_for_individuals.add(birthName_dprop_im_uri);
		//It is a number and will be filtered
		//Shoyld be considered for the "role assertion inverted file"
		//accepted_data_assertion_URIs_for_individuals.add(population_dprop_im_uri);
		
		//Data deep2??
		//it is a comment
		
		//Object
		accepted_object_assertion_URIs_for_individuals.add(label_oprop_im_uri);
		accepted_object_assertion_URIs_for_individuals.add(curriculum_oprop_im_uri);
		accepted_object_assertion_URIs_for_individuals.add(places_oprop_im_uri);
		
		
		
		//Other accepted data assertions
		accepted_data_assertion_URIs_for_individuals.add(abstract_dprop_im_uri);
		accepted_data_assertion_URIs_for_individuals.add(label_dprop_im_uri);
		accepted_data_assertion_URIs_for_individuals.add(name2_dprop_im_uri);
		
		
		
	}
	
	
	public static void setMinSize4Overlapping(int size){
		min_size_overlapping = size;
	}
	
	
	public static void readParameters(){
		
		try{
			
			//File file = new File("thresholds.txt");
			//System.out.println(file.getAbsolutePath() + "  " + file.exists());
			
			File file = new File ("parameters.txt");
			
			if (!file.exists()){
				System.err.println("Error reading LogMap parameters. File 'parameters.txt' is not available. Using default parameters.");
				return;
			}
			
			
			//We reinit with URIs in file
			accepted_annotation_URIs_for_classes.clear();
			accepted_data_assertion_URIs_for_individuals.clear();
			
			ReadFile reader = new ReadFile("parameters.txt");
			//ReadFile reader = new ReadFile("/home/ernesto/OM_OAEI/logmap2_package/conf/thresholds.txt");
			
			String line;
			String[] elements;
			
			while ((line = reader.readLine()) != null){
				
				//Ignore commented lines
				if (line.startsWith("#")){
					continue;
				}
				
				if (line.indexOf("|")<0){
					continue;
				}
				//System.out.println(line);
				elements=line.split("\\|");
				
				if (elements[0].equals(print_output_str)){
					print_output = Boolean.valueOf(elements[1]);
				}
				else if (elements[0].equals(repair_heuristic_str )){
					repair_heuristic = Boolean.valueOf(elements[1]);
				}
				else if (elements[0].equals(repair_heuristic_threshold_remove_str)){
					repair_heuristic_threshold_remove = Integer.valueOf(elements[1]);
				}
				else if (elements[0].equals(repair_heuristic_threshold_decrease_str)){
					repair_heuristic_threshold_decrease = Integer.valueOf(elements[1]);
				}
				else if (elements[0].equals(repair_heuristic_str )){
					repair_heuristic = Boolean.valueOf(elements[1]);
				}
				else if (elements[0].equals(bad_score_scope_str)){
					bad_score_scope = Double.valueOf(elements[1]);
				}
				else if (elements[0].equals(good_isub_anchors_str)){
					good_isub_anchors = Double.valueOf(elements[1]);
				}
				else if (elements[0].equals(good_isub_candidates_str)){
					good_isub_candidates=Double.valueOf(elements[1]);
				}
				else if (elements[0].equals(good_confidence_str)){
					good_confidence = Double.valueOf(elements[1]);
				}
				else if (elements[0].equals(good_sim_coocurrence_str)){
					good_sim_coocurrence = Double.valueOf(elements[1]);
				}
				else if (elements[0].equals(min_conf_pro_map_str)){
					min_conf_pro_map = Double.valueOf(elements[1]);
				}
				else if (elements[0].equals(max_ambiguity_str)){
					max_ambiguity = Integer.valueOf(elements[1]);
				}
				else if (elements[0].equals(good_ambiguity_str)){
					good_ambiguity = Integer.valueOf(elements[1]);
				}
				else if (elements[0].equals(use_overlapping_str)){
					use_overlapping = Boolean.valueOf(elements[1]);
				}
				else if (elements[0].equals(min_size_overlapping_str)){
					min_size_overlapping = Integer.valueOf(elements[1]);
				}
				else if (elements[0].equals(instance_matching_str)){
					perform_instance_matching = Boolean.valueOf(elements[1]);
				}
				
				else if (elements[0].equals(output_class_mappings_str)){
					output_class_mappings = Boolean.valueOf(elements[1]);
				}
				else if (elements[0].equals(output_prop_mappings_str)){
					output_prop_mappings = Boolean.valueOf(elements[1]);
				}
				else if (elements[0].equals(output_instance_mappings_str)){
					output_instance_mappings = Boolean.valueOf(elements[1]);
				}
				
				else if (elements[0].equals(output_instance_mapping_files_str)){
					output_instance_mapping_files = Boolean.valueOf(elements[1]);
				}
				
				
				
				else if (elements[0].equals(annotation_URI_str)){
					accepted_annotation_URIs_for_classes.add(elements[1]);
				}
				else if (elements[0].equals(data_assertion_URI_Indiv_str)){
					accepted_data_assertion_URIs_for_individuals.add(elements[1]);
				}
				else if (elements[0].equals(data_assertion_URI_Indiv_deep2_str)){
					accepted_data_assertion_URIs_for_individuals_deep2.add(elements[1]);
				}
				else if (elements[0].equals(object_assertion_URI_Indiv_str)){
					accepted_object_assertion_URIs_for_individuals.add(elements[1]);
				}
				else if (elements[0].equals(reason_datatypes_str)){
					reason_datatypes = Boolean.valueOf(elements[1]);
				}
				else if (elements[0].equals(reasoner_str)){
					reasoner = elements[1];
				}
				else if (elements[0].equals(timeout_str)){
					timeout = Integer.valueOf(elements[1]);
				}
				else if (elements[0].equals(output_equivalences_only_str)){
					output_equivalences_only = Boolean.valueOf(elements[1]);
				}
				
				else if (elements[0].equals(second_chance_conflicts_str)){
					second_chance_conflicts = Boolean.valueOf(elements[1]);
				}
				
				else if (elements[0].equals(ratio_second_chance_discarded_str)){
					ratio_second_chance_discarded = Integer.valueOf(elements[1]);
				}
				
				else if (elements[0].equals(use_umls_lexicon_str)){
					use_umls_lexicon = Boolean.valueOf(elements[1]);
				}
				else if (elements[0].equals(reverse_labels_str)){
					reverse_labels = Boolean.valueOf(elements[1]);
				}
				else if (elements[0].equals(allow_interactivity_str)){
					allow_interactivity = Boolean.valueOf(elements[1]);
				}
				
				
				
				
				
			}
			
			//System.out.println(accepted_annotation_URIs.size());
			//for (String str : accepted_annotation_URIs){
			//	System.out.println("Read: " + str);
			//}
			
			reader.closeBuffer();
		}
		catch (Exception e){
			System.err.println("Error reading LogMap 2 parameters file: " + e.getLocalizedMessage());
		}
		
	}
	
	
}
