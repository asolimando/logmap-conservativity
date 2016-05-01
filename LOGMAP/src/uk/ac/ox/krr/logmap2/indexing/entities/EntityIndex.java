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

import uk.ac.ox.krr.logmap2.indexing.labelling_schema.Node;

public abstract class EntityIndex {

	
	protected int onto_index;
	
	protected int index;
	
	//Interval labelling index
	protected Node node;
	
	protected String namespace; 
	
	
	//Lexical Indexation
	protected String name4Entitity;	
	protected String label4Entity;
	
	public EntityIndex(){
	}
	
	public EntityIndex(EntityIndex idx) {
		onto_index = idx.onto_index;
		index = idx.index;
		namespace = idx.namespace;
		name4Entitity = idx.name4Entitity;
		label4Entity = idx.label4Entity;
	}

	/**
	 * In order to identify ontology of entity
	 * @param entityName
	 */
	public void setOntologyId(int ontoindex){
		onto_index=ontoindex;
	}
	
	public int getOntologyId(){
		return onto_index;
	}
	
	public boolean equals(String entityname)
	{
		return name4Entitity.equals(entityname);
	}
	
	public void setEntityName(String entityName){
		name4Entitity=entityName;
	}
	
	/**
	 * The class name in the URI			
	 * @return
	 */
	public String getEntityName(){
		return name4Entitity;
	}
	
	
	public void setLabel(String label){
		label4Entity=label;
	}
	
	/**
	 * Usually represents the preferred class name
	 * @return
	 */
	public String getLabel(){
		return label4Entity;
	}
	
	
	/**
	 * Given IRI within the ontology
	 * @param baseIRI
	 * @return
	 */
	public String getIRI(String baseIRI){
		if (hasDifferentNamespace()){
			if (namespace.equals(name4Entitity)){
				return namespace; //Cases in which uri has not '#'
			}
			else {
				//For URIS like http://ontology.dumontierlab.com/hasReference
				if (namespace.endsWith("/"))
					return namespace + name4Entitity;
				else
					return namespace + "#" + name4Entitity;
			}
		}
		//
		if (baseIRI.endsWith("/")){
			return baseIRI +  name4Entitity;
		}
		else {
			return baseIRI + "#" + name4Entitity;
		}
	}
	
	
	/**
	 * @param namespace the namespace to set
	 */
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}
	
	


	/**
	 * @return the namespace of the entity if it has one
	 */
	public String getNamespace() {
		if (hasDifferentNamespace())
			return namespace;
		else
			return "";
	}
	
	
	/**
	 * If the namespace of the entity is different from the ontology namespace we store ir
	 * @return
	 */
	public boolean hasDifferentNamespace(){
		if (namespace==null || namespace.equals(""))
			return false;
		return true;		
	}

//	@Override
//	public int hashCode() {
//		final int prime = 31;
//		int result = 1;
//		result = prime * result + index;
//		result = prime * result
//				+ ((label4Entity == null) ? 0 : label4Entity.hashCode());
//		result = prime * result
//				+ ((name4Entitity == null) ? 0 : name4Entitity.hashCode());
//		result = prime * result
//				+ ((namespace == null) ? 0 : namespace.hashCode());
//		result = prime * result + ((node == null) ? 0 : node.hashCode());
//		result = prime * result + onto_index;
//		return result;
//	}
//
//	@Override
//	public boolean equals(Object obj) {
//		if (this == obj)
//			return true;
//		if (obj == null)
//			return false;
//		if (getClass() != obj.getClass())
//			return false;
//		EntityIndex other = (EntityIndex) obj;
//		if (index != other.index)
//			return false;
//		if (label4Entity == null) {
//			if (other.label4Entity != null)
//				return false;
//		} else if (!label4Entity.equals(other.label4Entity))
//			return false;
//		if (name4Entitity == null) {
//			if (other.name4Entitity != null)
//				return false;
//		} else if (!name4Entitity.equals(other.name4Entitity))
//			return false;
//		if (namespace == null) {
//			if (other.namespace != null)
//				return false;
//		} else if (!namespace.equals(other.namespace))
//			return false;
//		if (node == null) {
//			if (other.node != null)
//				return false;
//		} else if (!node.equals(other.node))
//			return false;
//		if (onto_index != other.onto_index)
//			return false;
//		return true;
//	}
	
	
	

	
}
