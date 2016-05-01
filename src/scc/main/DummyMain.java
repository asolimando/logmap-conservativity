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
package scc.main;

import java.io.File;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import util.Params;

public class DummyMain {

	/**
	 * @param args
	 */
	 public static void main(String[] args) throws OWLOntologyCreationException {
	        OWLOntology o =
	OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(new
	File(Params.dataFolder + "oaei2012/anatomy/onto/mouse.owl"));
	        for (OWLOntology ont : o.getImportsClosure()) {
	            for (OWLClass c : ont.getClassesInSignature()) {
	                if (c.toString().contains("obo")) {
	                    System.out.println("TestAlessandro.main() " + c +
	" \t" + ont.getOntologyID());
	                }
	            }
	        }
	    }


}
