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
package uk.ac.ox.krr.logmap2.indexing.entities;


import java.lang.reflect.InvocationTargetException;
import java.util.Collections;


import java.util.List;
import java.util.Set;
import java.util.HashSet;

import uk.ac.ox.krr.logmap2.indexing.labelling_schema.Node;
import uk.ac.ox.krr.logmap2.indexing.labelling_schema.PreNode;
import uk.ac.ox.krr.logmap2.mappings.I_Sub;
import uk.ac.ox.krr.logmap2.utilities.Lib;


public class ClassIndex extends EntityIndex{
	
	
	private Set<String> alternativeLabels;// = new HashSet<String>();
	
	//private List<String> stemmed_label_list;
	
	//Structural Indexing
	private Set<Integer> disjointClasses;	
	private Set<Integer> equivalentClasses;	
	private Set<Integer> directSubclasses;	
	private Set<Integer> directSuperclasses;
	private int hierarchyLevel=-1;
	
	
	
	//Scope
	private Set<Integer> scope4Score;
	private Set<Integer> scope4Exploration;
	
	//Root classes for the class 
	private Set<Integer> roots;
	
	public ClassIndex(int i){
		index=i;
		node = new PreNode(i);
	}
	
	public ClassIndex(ClassIndex idx) {
		super((EntityIndex) idx);
		try {
			node = idx.node.getClass().getConstructor(
					idx.node.getClass()).newInstance((
							idx.node.getClass().cast(idx.node)));
		} catch (InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}			
//				new PreNode((PreNode) idx.node);
		if(idx.alternativeLabels != null)
			alternativeLabels = new HashSet<String>(idx.getAlternativeLabels());
		if(idx.disjointClasses != null)
			disjointClasses = new HashSet<Integer>(idx.getDisjointClasses());
		if(idx.equivalentClasses != null)
			equivalentClasses = new HashSet<Integer>(idx.getEquivalentClasses());	
		if(idx.directSubclasses != null)
			directSubclasses = new HashSet<Integer>(idx.getDirectSubclasses());
		if(idx.directSuperclasses != null)
			directSuperclasses = new HashSet<Integer>(idx.getDirectSuperclasses());
		hierarchyLevel=idx.hierarchyLevel;
		if(idx.scope4Score != null)
			scope4Score = new HashSet<Integer>(idx.getScope4Scores());
		if(idx.scope4Exploration != null)
			scope4Exploration = new HashSet<Integer>(idx.getScope4Exploration());
		if(idx.roots != null)
			roots = new HashSet<Integer>(idx.getRoots());
	}

	public void addAlternativeLabel(String altLabel){
		
		if (alternativeLabels==null)
			alternativeLabels=new HashSet<String>();
		
		alternativeLabels.add(altLabel);
		
	}
	
	public void setAlternativeLabels(Set<String> altLabels){
		
		alternativeLabels=new HashSet<String>(altLabels);
		
	}
	
	
	public void setEmptyAlternativeLabels(){
		
		alternativeLabels=new HashSet<String>();
		
	}
	
	
	
	
	/**
	 * Set of alternative labels or synonyms
	 * @return
	 */
	public Set<String> getAlternativeLabels(){
		if (alternativeLabels==null)
			return Collections.emptySet();
		
		return alternativeLabels;
	}
	
	public boolean hasAlternativeLabels(){
		if (alternativeLabels==null)
			return false;
		return true;		
	}


	
	/**
	 * @param node the node to set
	 */
	public void setNode(Node node) {
		this.node = node;
	}


	/**
	 * @return the node
	 */
	public Node getNode() {
		return node;
	}
	
	
	
	public boolean hasDirectDisjointClasses(){
		if (disjointClasses==null)
			return false;
		return true;		
	}


	/**
	 * @param disjointClasses the disjointClasses to set
	 */
	public void setDisjointClasses(Set<Integer> disjointClasses) {
		this.disjointClasses = disjointClasses;
	}
	
	
	/**
	 *
	 */
	public void setEmptyDisjointClasses() {
		if (disjointClasses==null)
			this.disjointClasses = new HashSet<Integer>();
		else
			this.disjointClasses.clear();
	}

	
	/**
	 * Add disjoint class
	 */
	public void addDisjointClass(int disjident) {
		if (disjointClasses==null)
			this.disjointClasses = new HashSet<Integer>();
		
		this.disjointClasses.add(disjident);		
	}
	
	public void removeDisjointClass(int disjident){
		if (disjointClasses==null)
			return;
		
		this.disjointClasses.remove(disjident);		
	}
	
	/**
	 * Add all disjoint classes
	 */
	public void addAllDisjointClasses(Set<Integer> disjclasses) {
		if (disjointClasses==null)
			this.disjointClasses = new HashSet<Integer>();
		
		this.disjointClasses.addAll(disjclasses);
		
	}

	/**
	 * @return the disjointClasses
	 */
	public Set<Integer> getDisjointClasses() {
		return disjointClasses;
	}

	
	public boolean hasEquivalentClasses(){
		if (equivalentClasses==null)
			return false;
		return true;		
	}

	/**
	 * @param equivalentClasses the equivalentClasses to set
	 */
	public void setEquivalentClasses(Set<Integer> equivalentClasses) {
		this.equivalentClasses = equivalentClasses;
	}

	
	/**
	 *
	 */
	public void setEmptyEquivalentClasses() {
		if (equivalentClasses==null)
			this.equivalentClasses = new HashSet<Integer>();
		else
			this.equivalentClasses.clear();
	}

	
	/**
	 * Add equivalent class
	 */
	public void addEquivalentClass(int disjident) {
		if (equivalentClasses==null)
			this.equivalentClasses = new HashSet<Integer>();
		
		this.equivalentClasses.add(disjident);
		
	}
	

	/**
	 * @return the equivalentClasses
	 */
	public Set<Integer> getEquivalentClasses() {
		return equivalentClasses;
	}


	public boolean hasDirectSubClasses(){
		if (directSubclasses==null)
			return false;
		return true;		
	}
	
	
	/**
	 *
	 */
	public void setEmptyDirectSubClasses() {
		if (directSubclasses==null)
			this.directSubclasses = new HashSet<Integer>();
		else
			this.directSubclasses.clear();
	}

	
	/**
	 * Add direct sub class
	 */
	public void addDirectSubClass(int disjident) {
		if (directSubclasses==null)
			this.directSubclasses = new HashSet<Integer>();
		
		this.directSubclasses.add(disjident);
		
	}
	
	/**
	 * @param directSubclasses the directSubclasses to set
	 */
	public void setDirectSubclasses(Set<Integer> directSubclasses) {
		this.directSubclasses = directSubclasses;
	}


	/**
	 * @return the directSubclasses
	 */
	public Set<Integer> getDirectSubclasses() {
		return directSubclasses;
	}


	
	
	public boolean hasDirectSuperClasses(){
		if (directSuperclasses==null)
			return false;
		return true;		
	}
	
	
	/**
	 *
	 */
	public void setEmptyDirectSuperClasses() {
		if (directSuperclasses==null)
			this.directSuperclasses = new HashSet<Integer>();
		else
			this.directSuperclasses.clear();
	}

	
	/**
	 * Add direct super class
	 */
	public void addDirectSuperClass(int disjident) {
		if (directSuperclasses==null)
			this.directSuperclasses = new HashSet<Integer>();
		
		this.directSuperclasses.add(disjident);
		
	}
	
	
	/**
	 * @param directSuperclasses the directSuperclasses to set
	 */
	public void setDirectSuperclasses(Set<Integer> directSuperclasses) {
		this.directSuperclasses = directSuperclasses;
	}


	/**
	 * @return the directSuperclasses
	 */
	public Set<Integer> getDirectSuperclasses() {
		return directSuperclasses;
	}



	
	public boolean hasScope4Scores(){
		if (scope4Score==null)
			return false;
		return true;		
	}


	/**
	 * @param scope the scope to set
	 */
	public void setScope4Scores(Set<Integer> scope) {
		//this.scope4Score = new HashSet<Integer>(scope);
		this.scope4Score = scope;
	}


	/**
	 * @return the scope
	 */
	public Set<Integer> getScope4Scores() {
		return scope4Score;
	}
	
	
	
	public boolean hasScope4Exploration(){
		if (scope4Exploration==null)
			return false;
		return true;		
	}
	
	
	/**
	 * @param scope the scope to set
	 */
	public void setScope4Exploration(Set<Integer> scope) {
		this.scope4Exploration = scope;
		//this.scope4Exploration = new HashSet<Integer>(scope);
	}


	/**
	 * @return the scope
	 */
	public Set<Integer> getScope4Exploration() {
		return scope4Exploration;
	}
	
	
	
	public boolean hasRoots(){
		if (roots==null)
			return false;
		return true;		
	}


	/**
	 * @param roots the roots to set
	 */
	public void setRoots(Set<Integer> roots) {
		this.roots = roots;
	}


	/**
	 * @return the roots
	 */
	public Set<Integer> getRoots() {
		return roots;
	}


	/**
	 * @param hierarchyLevel the hierarchyLevel to set
	 */
	public void setHierarchyLevel(int hierarchyLevel) {
		this.hierarchyLevel = hierarchyLevel;
	}


	/**
	 * @return the hierarchyLevel
	 */
	public int getHierarchyLevel() {
		return hierarchyLevel;
	}




	/*
	 * @param stemmed_label_list the stemmed_label_list to set
	 *
	public void setStemmedLabelList(List<String> stemmed_label_list) {
		this.stemmed_label_list = stemmed_label_list;
	}*/






	/**
	 * @return the stemmed_label_list
	 *
	public List<String> getStemmedLabelList() {
		return stemmed_label_list;
	}*/
	
	private Set<String> stemmedAltLabels;
	
	public String findSimilarStemmedAltLable(ClassIndex that)
	{
		if (stemmedAltLabels == null || that.stemmedAltLabels == null)
			return null;
		
		int combo = -1, left = -1, temp_c, temp_l;
		String ret = null;
		String[] words2;
		
		for (String lab1 : stemmedAltLabels)
			for (String lab2 : that.stemmedAltLabels)
			{
				temp_l = -1;
				if (((temp_c = getCommonWordsNumber(lab1, (words2 = lab2.split("_")))) > combo) ||
						temp_c == combo && (temp_l = lab1.split("_").length + words2.length) < left)
				{
					combo = temp_c;
					left = temp_l == -1 ? lab1.split("_").length + words2.length : temp_l;
					ret = lab1 + " " + lab2;
				}
			}
		
		words2 = null;
		return ret;
	}
	
	private int getCommonWordsNumber(String str, String[] words)
	{
		int ret = 0;
		for (String word : words)
			if (!word.isEmpty() && str.contains(word))		//word is stemmed
				++ret;
		return ret;
	}
	
	public String findStemmedAltLabel(Set<String> words)
	{
		if (stemmedAltLabels == null)
		{
			Lib.debuginfo("The class named " + name4Entitity + " has no stemmed alt labels.");
			return null;
		}
			
		String label = "";
		int maxScore = 0, score;
		for (String l : stemmedAltLabels)
			if (maxScore < (score = getCommonWordsNumber(l, words)))
			{
				maxScore = score;
				label = l;
			}
			else if (maxScore == score && l.length() < label.length())
				label = l;
		
		return label;
	}
	
	private int getCommonWordsNumber(String str, Set<String> words)
	{
		int ret = 0;
		for (String word : words)
			if (str.contains(word))		//word is stemmed
				++ret;
		return ret;
	}
	
	public void addStemmedAltLabel(String label)
	{
		if (stemmedAltLabels == null)
			stemmedAltLabels = new HashSet<String>();
/*
		boolean sub = false;
		for (String lab : stemmedAltLabels)
		{
			sub = true;
			for (String word : label.split("_"))
				if (!lab.contains(word))
				{
					sub = false;
					break;
				}
		}
		
		if (!sub)
*/			stemmedAltLabels.add(label);
	}

	public Set<String> getStemmedAltLabels()
	{
		return stemmedAltLabels;
	}
	
	
	public boolean hasStemmedAlternativeLabels(){
		if (stemmedAltLabels==null)
			return false;
		return true;		
	}
	
	
	public void deleteAltStemmedLabels(){
		stemmedAltLabels.clear();
	}
	
	public void deleteAltLabels(){
		alternativeLabels.clear();
	}

//	@Override
//	public int hashCode() {
//		final int prime = 31;
//		int result = super.hashCode();
//		result = prime
//				* result
//				+ ((alternativeLabels == null) ? 0 : alternativeLabels
//						.hashCode());
//		result = prime
//				* result
//				+ ((directSubclasses == null) ? 0 : directSubclasses.hashCode());
//		result = prime
//				* result
//				+ ((directSuperclasses == null) ? 0 : directSuperclasses
//						.hashCode());
//		result = prime * result
//				+ ((disjointClasses == null) ? 0 : disjointClasses.hashCode());
//		result = prime
//				* result
//				+ ((equivalentClasses == null) ? 0 : equivalentClasses
//						.hashCode());
//		result = prime * result + hierarchyLevel;
//		result = prime * result + ((roots == null) ? 0 : roots.hashCode());
//		result = prime
//				* result
//				+ ((scope4Exploration == null) ? 0 : scope4Exploration
//						.hashCode());
//		result = prime * result
//				+ ((scope4Score == null) ? 0 : scope4Score.hashCode());
//		result = prime
//				* result
//				+ ((stemmedAltLabels == null) ? 0 : stemmedAltLabels.hashCode());
//		return result;
//	}
//
//	@Override
//	public boolean equals(Object obj) {
//		if (this == obj)
//			return true;
//		if (!super.equals(obj))
//			return false;
//		if (getClass() != obj.getClass())
//			return false;
//		ClassIndex other = (ClassIndex) obj;
//		if (alternativeLabels == null) {
//			if (other.alternativeLabels != null)
//				return false;
//		} else if (!alternativeLabels.equals(other.alternativeLabels))
//			return false;
//		if (directSubclasses == null) {
//			if (other.directSubclasses != null)
//				return false;
//		} else if (!directSubclasses.equals(other.directSubclasses))
//			return false;
//		if (directSuperclasses == null) {
//			if (other.directSuperclasses != null)
//				return false;
//		} else if (!directSuperclasses.equals(other.directSuperclasses))
//			return false;
//		if (disjointClasses == null) {
//			if (other.disjointClasses != null)
//				return false;
//		} else if (!disjointClasses.equals(other.disjointClasses))
//			return false;
//		if (equivalentClasses == null) {
//			if (other.equivalentClasses != null)
//				return false;
//		} else if (!equivalentClasses.equals(other.equivalentClasses))
//			return false;
//		if (hierarchyLevel != other.hierarchyLevel)
//			return false;
//		if (roots == null) {
//			if (other.roots != null)
//				return false;
//		} else if (!roots.equals(other.roots))
//			return false;
//		if (scope4Exploration == null) {
//			if (other.scope4Exploration != null)
//				return false;
//		} else if (!scope4Exploration.equals(other.scope4Exploration))
//			return false;
//		if (scope4Score == null) {
//			if (other.scope4Score != null)
//				return false;
//		} else if (!scope4Score.equals(other.scope4Score))
//			return false;
//		if (stemmedAltLabels == null) {
//			if (other.stemmedAltLabels != null)
//				return false;
//		} else if (!stemmedAltLabels.equals(other.stemmedAltLabels))
//			return false;
//		return true;
//	}

}
