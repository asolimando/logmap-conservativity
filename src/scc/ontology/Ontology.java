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
package scc.ontology;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLOntologyCreationIOException;
import org.semanticweb.owlapi.io.OWLParser;
import org.semanticweb.owlapi.io.OWLParserException;
import org.semanticweb.owlapi.io.UnparsableOntologyException;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.UnloadableImportException;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;

public class Ontology {
	
	private File pathName;
	private OWLOntology ontology;
	private OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	private Set<OWLClass> classes;
	private Set<OWLEquivalentClassesAxiom> equiv = new HashSet<OWLEquivalentClassesAxiom>();
	private Set<OWLSubClassOfAxiom> subsumption = new HashSet<OWLSubClassOfAxiom>();
	
	public Ontology(File pathName){
		this.pathName = pathName;
		loadOntology();
		extractInfo();
	}
	
	public int getNodesNumber(){
		return classes.size();
	}
	
	public String getID(){
		return ontology.getOntologyID().toString();
	}
	
	private void extractInfo(){
		if(classes == null){
			classes = new HashSet<OWLClass>();
			Set<OWLClass> allClasses = ontology.getClassesInSignature(); 
			
			for (OWLClass cls : allClasses)
				if(acceptableClass(cls))
					classes.add(cls);
		}
	}
	
	private boolean isAcceptableClassExpression(OWLClassExpression clsExpr){
		if(clsExpr.isClassExpressionLiteral() && !clsExpr.isOWLNothing() && !clsExpr.isOWLThing())
			if(!Collections.disjoint(clsExpr.getClassesInSignature(), ontology.getClassesInSignature()))
				return true;
		return false;
	}
	
	public static String getShortClassname(OWLClass cls){
		return cls.toStringID().substring( Math.max(cls.toStringID().lastIndexOf("/"), cls.toStringID().lastIndexOf("#"))+1);
	}
	
	private boolean acceptableClass(OWLClass cls){
		Set<OWLEquivalentClassesAxiom> eq;
		Set<OWLSubClassOfAxiom> sub;
		boolean res = false, localRes = false;
		OWLClass tmpClass;
		
		if(getShortClassname(cls).compareTo("EntityAnnotation") == 0)
			System.out.println("");
		
		eq = ontology.getEquivalentClassesAxioms(cls);
		for (OWLEquivalentClassesAxiom owlEqClass : eq) {
			localRes = false;
			for (OWLClassExpression owlCE : owlEqClass.getClassExpressionsAsList()) {
				if(isAcceptableClassExpression(owlCE)){
					tmpClass = (OWLClass) owlCE.getClassesInSignature().toArray()[0];
					if(!classes.contains(tmpClass))
						classes.add(tmpClass);
				}
				else
					localRes = false;
			}
			if(localRes){
				res = true;
				equiv.addAll(eq);
			}
		}
		
		 // cls will be the "from" in edge representation
		sub = ontology.getSubClassAxiomsForSubClass(cls);
		for (OWLSubClassOfAxiom owlSubClassOfAxiom : sub) {
			if(owlSubClassOfAxiom.getSubClass().equals(owlSubClassOfAxiom.getSuperClass()))
				continue;

			if(isAcceptableClassExpression(owlSubClassOfAxiom.getSuperClass())){
				tmpClass = (OWLClass) owlSubClassOfAxiom.getSuperClass();
				if(!classes.contains(tmpClass))
					classes.add(tmpClass);
				
				subsumption.addAll(sub);
				res = true;
			}
		}

		return !classes.contains(cls) && res;
	}
	
	private void loadOntology(){
		 try {
	            //IRI iri = IRI.create("http://www.co-ode.org/ontologies/pizza/pizza.owl");
	            IRI iri = IRI.create(pathName);
	            ontology = manager.loadOntologyFromOntologyDocument(iri);
	            System.out.println("Loaded ontology: " + ontology);
	            
//	            IRI documentIRI = manager.getOntologyDocumentIRI(ontology);
//	            System.out.println("    from: " + documentIRI);
	            
	        } catch (OWLOntologyCreationIOException e) {
	            // IOExceptions during loading get wrapped in an
	            // OWLOntologyCreationIOException
	            IOException ioException = e.getCause();
	            if (ioException instanceof FileNotFoundException) {
	                System.out.println("Could not load ontology. File not found: "
	                        + ioException.getMessage());
	            } else if (ioException instanceof UnknownHostException) {
	                System.out.println("Could not load ontology. Unknown host: "
	                        + ioException.getMessage());
	            } else {
	                System.out.println("Could not load ontology: "
	                        + ioException.getClass().getSimpleName() + " "
	                        + ioException.getMessage());
	            }
	        } catch (UnparsableOntologyException e) {
	     
	            System.out.println("Could not parse the ontology: " + e.getMessage());
	            // A map of errors can be obtained from the exception
	            Map<OWLParser, OWLParserException> exceptions = e.getExceptions();
	            // The map describes which parsers were tried and what the errors
	            // were
	            for (OWLParser parser : exceptions.keySet()) {
	                System.out.println("Tried to parse the ontology with the "
	                        + parser.getClass().getSimpleName() + " parser");
	                System.out.println("Failed because: "
	                        + exceptions.get(parser).getMessage());
	            }
	        } catch (UnloadableImportException e) {
	            // If our ontology contains imports and one or more of the imports
	            // could not be loaded then an
	            // UnloadableImportException will be thrown (depending on the
	            // missing imports handling policy)
	            System.out.println("Could not load import: " + e.getImportsDeclaration());
	            // The reason for this is specified and an
	            // OWLOntologyCreationException
	            OWLOntologyCreationException cause = e.getOntologyCreationException();
	            System.out.println("Reason: " + cause.getMessage());
	        } catch (OWLOntologyCreationException e) {
	            System.out.println("Could not load ontology: " + e.getMessage());
	        }
	}

	public Set<OWLEquivalentClassesAxiom> getEquiv() {
		return equiv;
	}

	public void setEquiv(Set<OWLEquivalentClassesAxiom> equiv) {
		this.equiv = equiv;
	}

	public Set<OWLSubClassOfAxiom> getSubsumption() {
		return subsumption;
	}

	public void setSubsumption(Set<OWLSubClassOfAxiom> subsumption) {
		this.subsumption = subsumption;
	}

	public Set<OWLClass> getClasses() {
		return classes;
	}

	public void setClasses(Set<OWLClass> classes) {
		this.classes = classes;
	}
}
