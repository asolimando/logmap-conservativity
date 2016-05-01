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
package org.semanticweb.more.structural;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLTransitiveObjectPropertyAxiom;

import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

public class ObjectPropertyManager{

//	Set<OWLObjectPropertyExpression> allRoles = new HashSet<OWLObjectPropertyExpression>();
	
	
	//We will use this class to compute the transitive reflexive closure of the role hierarchy 
	//and to compute the domain and range of all roles once that hierarchy is taken onto account
	
	Map<OWLObjectPropertyExpression, Set<OWLClass>> domainMap = new HashMap<OWLObjectPropertyExpression, Set<OWLClass>>();
	Map<OWLObjectPropertyExpression, Set<OWLClass>> rangeMap = new HashMap<OWLObjectPropertyExpression, Set<OWLClass>>();
//	Map<OWLObjectPropertyExpression, OWLClass> domainMap = new HashMap<OWLObjectPropertyExpression, OWLClass>();
//	Map<OWLObjectPropertyExpression, OWLClass> rangeMap = new HashMap<OWLObjectPropertyExpression, OWLClass>();
//	Set<OWLObjectPropertyDomainAxiom> domainAxioms = new HashSet<OWLObjectPropertyDomainAxiom>();
//	Set<OWLObjectPropertyRangeAxiom> rangeAxioms = new HashSet<OWLObjectPropertyRangeAxiom>();
	Map<OWLObjectPropertyExpression, Set<OWLObjectPropertyExpression>> roleHierarchy = new HashMap<OWLObjectPropertyExpression, Set<OWLObjectPropertyExpression>>();
	boolean transitiveClosureOfRoleHierarchyComputed = false;
	
	Set<OWLObjectPropertyExpression> generatingRoles = new HashSet<OWLObjectPropertyExpression>();
	Set<OWLTransitiveObjectPropertyAxiom> transitivityAxioms = new HashSet<OWLTransitiveObjectPropertyAxiom>();
	
	Map<OWLObjectPropertyExpression, Map<OWLClass, OWLClass>> classesToEncodeTransitivityStandard = new HashMap<OWLObjectPropertyExpression, Map<OWLClass,OWLClass>>();
	Map<OWLObjectPropertyExpression, Map<OWLClass, OWLClass>> classesToEncodeTransitivityDisjointness = new HashMap<OWLObjectPropertyExpression, Map<OWLClass,OWLClass>>();//for cases  {some R. A  and  B   ->   bot}
	Map<OWLObjectPropertyExpression, Map<OWLClass, OWLClass>> classesDefUniv = new HashMap<OWLObjectPropertyExpression, Map<OWLClass,OWLClass>>();
	Map<OWLObjectPropertyExpression, Map<OWLClass, OWLClass>> classesDefExist = new HashMap<OWLObjectPropertyExpression, Map<OWLClass,OWLClass>>();
	int freshClassCounter = 0;
	//find a way to register  generating roles and then add domain axioms for those roles which are generating
	
	OWLDataFactoryImpl factory = new OWLDataFactoryImpl();
	
	
	public void registerPropertyInclusion(OWLObjectPropertyExpression subP, OWLObjectPropertyExpression superP){
		 subP = subP.getSimplified();
		 superP = superP.getSimplified();
		
		if (roleHierarchy.containsKey(subP)){//then it must also contain inv(subP) as a key because we add both at the same time
			roleHierarchy.get(subP).add(superP);
			roleHierarchy.get(subP.getInverseProperty().getSimplified()).add(superP.getInverseProperty().getSimplified());
		}
		else{
			Set<OWLObjectPropertyExpression> superRoles = new HashSet<OWLObjectPropertyExpression>();
			superRoles.add(subP);//necessary??
			superRoles.add(superP);
			roleHierarchy.put(subP, superRoles);

			Set<OWLObjectPropertyExpression> superRolesInv = new HashSet<OWLObjectPropertyExpression>();
			superRolesInv.add(subP.getInverseProperty().getSimplified());//necessary??
			superRolesInv.add(superP.getInverseProperty().getSimplified());
			roleHierarchy.put(subP.getInverseProperty().getSimplified(), superRolesInv);
		}
	}

	public void registerDomain(OWLObjectPropertyExpression p, OWLClass domain){
		p = p.getSimplified();
		if (domainMap.containsKey(p))
			domainMap.get(p).add(domain);
		else{
			Set<OWLClass> aux = new HashSet<OWLClass>();
			aux.add(domain);
			domainMap.put(p, aux);
		}
//		if (domainMap.containsKey(p))
//			throw new IllegalArgumentException("more than one domain axiom for the same property!?!?");
//		else
//			domainMap.put(p, domain);
	}

	public void registerRange(OWLObjectPropertyExpression p, OWLClass range){
//		OWLObjectPropertyExpression pInv = p.getInverseProperty().getSimplified();
//		if (!domainMap.containsKey(pInv))
//			domainMap.get(pInv).add(range);
//		else{
//			Set<OWLClass> aux = new HashSet<OWLClass>();
//			aux.add(range);
//			domainMap.put(pInv, aux);
//		}
		p = p.getSimplified();
		if (rangeMap.containsKey(p))
			rangeMap.get(p).add(range);
		else{
			Set<OWLClass> aux = new HashSet<OWLClass>();
			aux.add(range);
			rangeMap.put(p, aux);
		}
//		if (rangeMap.containsKey(p))
//			throw new IllegalArgumentException("more than one range axiom for the same property!?!?");
//		else
//			domainMap.put(p, range);
	}
	
//	public void registerTransitiveObjectProperty(OWLObjectPropertyExpression p){
//		
//	}

	public void registerTransitivityAxiom(OWLTransitiveObjectPropertyAxiom ax){
		transitivityAxioms.add(ax);
	}

	public Set<OWLTransitiveObjectPropertyAxiom> getTransitivityAxioms(){
		return transitivityAxioms;
	}
	
	protected OWLClassExpression getIntersection(OWLClassExpression c1, OWLClassExpression c2){
		Set<OWLClassExpression> operands = new HashSet<OWLClassExpression>();
		if (c1 instanceof OWLObjectIntersectionOf)
			operands.addAll(((OWLObjectIntersectionOf) c1).getOperands());
		else 
			operands.add(c1);
		if (c2 instanceof OWLObjectIntersectionOf)
			operands.addAll(((OWLObjectIntersectionOf) c2).getOperands());
		else 
			operands.add(c2);
		
		return factory.getOWLObjectIntersectionOf(operands);
	}
	
	
	public List<OWLClassExpression[]> handleRangesDomainsAndTransitivity(
			List<OWLClassExpression[]> conceptInclusions,
			boolean integrateRangesInRhsExistentials, boolean encodeTransitivity) {
		
		if (encodeTransitivity)
			return handleRangeAndDomainAxioms(encodeTransitivity(conceptInclusions), integrateRangesInRhsExistentials);
		else 
			return handleRangeAndDomainAxioms(conceptInclusions, integrateRangesInRhsExistentials);
	}
	
	public List<OWLClassExpression[]> encodeTransitivity(List<OWLClassExpression[]> inclusions){
		
		List<OWLClassExpression[]> updatedInclusions = new ArrayList<OWLClassExpression[]>();
		
		Set<OWLObjectPropertyExpression> transitiveRoles = new HashSet<OWLObjectPropertyExpression>();
		for (OWLTransitiveObjectPropertyAxiom ax : transitivityAxioms){
			OWLObjectPropertyExpression p = ax.getProperty();
			transitiveRoles.add(p.getSimplified());
			transitiveRoles.add(p.getInverseProperty().getSimplified());
		}
		if (!transitiveRoles.isEmpty()){
			
			//first of all we need to locate the roles that are superroles of some transitive object property
			//for that we first need to compute the transitive closure of the role hierarachy
			if (!transitiveClosureOfRoleHierarchyComputed)
				computeTransitiveClosureOfRoleHierarchy();
			Map<OWLObjectPropertyExpression, Set<OWLObjectPropertyExpression>> rolesWithTransitiveSubroles = new HashMap<OWLObjectPropertyExpression, Set<OWLObjectPropertyExpression>>();
			for (OWLObjectPropertyExpression p : transitiveRoles){
				Set<OWLObjectPropertyExpression> superroles = roleHierarchy.get(p);
				if (superroles != null)
					for (OWLObjectPropertyExpression q : superroles)
						if (rolesWithTransitiveSubroles.containsKey(q))
							rolesWithTransitiveSubroles.get(q).add(p);
						else{
							Set<OWLObjectPropertyExpression> aux = new HashSet<OWLObjectPropertyExpression>();
							aux.add(p);
							rolesWithTransitiveSubroles.put(q, aux);
						}
				if (rolesWithTransitiveSubroles.containsKey(p))
					rolesWithTransitiveSubroles.get(p).add(p);
				else{
					Set<OWLObjectPropertyExpression> aux = new HashSet<OWLObjectPropertyExpression>();
					aux.add(p);
					rolesWithTransitiveSubroles.put(p, aux);
				}
			}


			while (!inclusions.isEmpty()){

				OWLClassExpression[] inclusion = inclusions.remove(inclusions.size()-1); 
				
				//indexes for univ with atomic filler if we have to isolate them
				//indexes for univ with negative filler if we have to isolate them
				//#negative atoms other than univ with atomic filler - this inlcudes negated atoms, univ with negative filler and at most restrictions -- this will help decide if we have to isolate the universals or not
				//#atomic classes -- this will help decide if we have 
				
				Integer[] indexesForPositiveUniversalsWithTransitiveSubrole = new Integer[inclusion.length];
				int nPositiveUniversals = 0;
				Integer[] indexesForNegativeUniversalsWithTransitiveSubrole = new Integer[inclusion.length];
				int nNegativeUniversals = 0;
				int nOtherNegativeDisjuncts = 0;//anything that would put stuff on the body of a datalog rule other than a universal restriction with atomic filler and with a transitive subrole
				int nAtomicClasses = 0;
				int nNegatedAtomicClasses = 0;
				
				for (int i = 0 ; i<inclusion.length ; i++){
					if (inclusion[i] instanceof OWLObjectAllValuesFrom){
						OWLObjectPropertyExpression p = ((OWLObjectAllValuesFrom) inclusion[i]).getProperty().getSimplified();
						if (rolesWithTransitiveSubroles.containsKey(p)){
							OWLClassExpression filler = ((OWLObjectAllValuesFrom) inclusion[i]).getFiller();
							if (filler instanceof OWLClass && !filler.isOWLNothing()){
								indexesForPositiveUniversalsWithTransitiveSubrole[nPositiveUniversals] = i;
								nPositiveUniversals++;
							}
							else if (((OWLObjectAllValuesFrom) inclusion[i]).getFiller().getComplementNNF() instanceof OWLClass || ((OWLObjectAllValuesFrom) inclusion[i]).getFiller().isOWLNothing()){
								indexesForNegativeUniversalsWithTransitiveSubrole[nNegativeUniversals] = i;
								nNegativeUniversals++;
								nOtherNegativeDisjuncts++;
							}
							else
								throw new IllegalStateException("filler in this universal restriction is not ok: " + inclusion[i].toString());
						}
						else nOtherNegativeDisjuncts++;
					}
					else if (inclusion[i] instanceof OWLClass){
						nAtomicClasses++;
					}
					else if (inclusion[i].getComplementNNF() instanceof OWLClass){
						nNegatedAtomicClasses++;
						nOtherNegativeDisjuncts++;
					}
				}
				
//				if (nPositiveUniversals > 0 && nNegativeUniversals > 0)
//					System.out.println("HERE'S AN INTERESTING CASE!!");
				
				int[] nNewAtomicClasses = new int[1];
				updatedInclusions.addAll(treatRhsUniversalsForTransitivity(
						inclusion, 
						indexesForPositiveUniversalsWithTransitiveSubrole, 
						nPositiveUniversals, 
						nOtherNegativeDisjuncts,
						nNegatedAtomicClasses, 
						rolesWithTransitiveSubroles,
						nNewAtomicClasses));
				nAtomicClasses = nAtomicClasses + nNewAtomicClasses[0];
				
				
				updatedInclusions.addAll(treatLhsExistentialsForTransitivity(
						inclusion,
						indexesForNegativeUniversalsWithTransitiveSubrole,
						nNegativeUniversals,
						nAtomicClasses,
						nNegatedAtomicClasses,
						rolesWithTransitiveSubroles));
				
			}
		}
		else
			updatedInclusions = inclusions;
		
		return updatedInclusions;

	}
	
	protected Set<OWLClassExpression[]> treatLhsExistentialsForTransitivity(
			OWLClassExpression[] inclusion,
			Integer[] indexesForNegativeUniversalsWithTransitiveSubrole,
			int nNegativeUniversals,
			int nAtomicClasses,
			int nNegatedAtomicClasses,
			Map<OWLObjectPropertyExpression, Set<OWLObjectPropertyExpression>> rolesWithTransitiveSubroles) {

		Set<OWLClassExpression[]> finalInclusions = new HashSet<OWLClassExpression[]>();
		boolean anyLhsExistentialsTreated = false;
		
		//isolate universal restrictions with negated atomic fillers or not
		//the only cases where we don't isolate them is when we have {top -> all R. not A  or  B} or {top -> all R. not A  or  not B}
		//{top -> all R. not A  or  B}
		if (inclusion.length == 2 && nAtomicClasses == 1 && nNegativeUniversals == 1){
			//the original axiom stays
			finalInclusions.add(inclusion);

			OWLObjectPropertyExpression p = ((OWLObjectAllValuesFrom) inclusion[indexesForNegativeUniversalsWithTransitiveSubrole[0]]).getProperty().getSimplified();
			OWLClassExpression filler = ((OWLObjectAllValuesFrom) inclusion[indexesForNegativeUniversalsWithTransitiveSubrole[0]]).getFiller();//must be negated atomic class or BOT
			
			if (!filler.isOWLNothing()){
				//o.w. it's jusr a domain axiom and we can just leave it as it is, no need to add anything extra
				OWLClass atomicClass = null;
				for (int i = 0 ; i<inclusion.length ; i++)
					if (inclusion[i] instanceof OWLClass)
						atomicClass = (OWLClass) inclusion[i];

				for (OWLObjectPropertyExpression t : rolesWithTransitiveSubroles.get(p)){
					
					anyLhsExistentialsTreated = true;
					
					boolean[] alreadyExists = new boolean[1];
					OWLClassExpression[] copyOfInclusion = new OWLClassExpression[2];
					OWLClass c_invT_atomiClass = getClassForTransitivity(t.getInverseProperty().getSimplified(), atomicClass, alreadyExists);
					copyOfInclusion[0] = c_invT_atomiClass;
					copyOfInclusion[1] = factory.getOWLObjectAllValuesFrom(t, filler);
					finalInclusions.add(copyOfInclusion);

					if (!alreadyExists[0]){
						finalInclusions.add(new OWLClassExpression[]{c_invT_atomiClass, factory.getOWLObjectAllValuesFrom(t, c_invT_atomiClass.getComplementNNF())});
						finalInclusions.add(new OWLClassExpression[]{c_invT_atomiClass.getComplementNNF(), atomicClass});
					}
				}
			}
		}
		//{top -> all R. not A  or  not B}
		else if (inclusion.length == 2 && nNegatedAtomicClasses == 1 && nNegativeUniversals == 1){
			// if the filler is BOT then maybe it's worth introducing an abbreviation for the negated atomic class 
			//and treating it as a domain axiom than adding all the extra aximos to encode transitivity
			
			//the original axiom stays
			finalInclusions.add(inclusion);

			OWLObjectPropertyExpression p = ((OWLObjectAllValuesFrom) inclusion[indexesForNegativeUniversalsWithTransitiveSubrole[0]]).getProperty().getSimplified();
			OWLClassExpression filler = ((OWLObjectAllValuesFrom) inclusion[indexesForNegativeUniversalsWithTransitiveSubrole[0]]).getFiller();//must be negated atomic class
			OWLObjectComplementOf negatedAtomicClass = null;
			for (int i = 0 ; i<inclusion.length ; i++)
				if (inclusion[i].getComplementNNF() instanceof OWLClass)
					negatedAtomicClass = (OWLObjectComplementOf) inclusion[i];

			for (OWLObjectPropertyExpression t : rolesWithTransitiveSubroles.get(p)){
				
				anyLhsExistentialsTreated = true;
				
				boolean[] alreadyExists = new boolean[1];
				OWLClassExpression[] copyOfInclusion = new OWLClassExpression[2];
				OWLClass c_invT_atomiClass = getClassForTransitivityDisjointness(t.getInverseProperty().getSimplified(), (OWLClass) negatedAtomicClass.getComplementNNF(), alreadyExists);
				copyOfInclusion[0] = c_invT_atomiClass;
				copyOfInclusion[1] = factory.getOWLObjectAllValuesFrom(t, filler);
				finalInclusions.add(copyOfInclusion);

				if (!alreadyExists[0]){
					finalInclusions.add(new OWLClassExpression[]{c_invT_atomiClass, factory.getOWLObjectAllValuesFrom(t, c_invT_atomiClass.getComplementNNF())});
					finalInclusions.add(new OWLClassExpression[]{c_invT_atomiClass.getComplementNNF(), negatedAtomicClass});
				}
			}
		}
		else{
			
//			////
//			if (nNegativeUniversals > 0)
//				System.out.println("look here!");
//			////
			
			//probably also in this case if the filler of the universal restriction is BOT we can turn it into a "domain" axiom that doesn't need anything done to encode transitivity
			
			for (int j = 0 ; j<nNegativeUniversals ; j++){
				if (inclusion[indexesForNegativeUniversalsWithTransitiveSubrole[j]] instanceof OWLObjectAllValuesFrom){
					OWLObjectPropertyExpression p = ((OWLObjectAllValuesFrom) inclusion[indexesForNegativeUniversalsWithTransitiveSubrole[j]]).getProperty().getSimplified();
					OWLClassExpression filler = ((OWLObjectAllValuesFrom) inclusion[indexesForNegativeUniversalsWithTransitiveSubrole[j]]).getFiller();
					boolean[] alreadyExists = new boolean[1];
					OWLClass c_p_filler = null;
					if (filler.isOWLNothing())
						c_p_filler = getDefinitionForExist(p.getInverseProperty().getSimplified(), factory.getOWLThing(), alreadyExists);
					else 
						c_p_filler = getDefinitionForExist(p.getInverseProperty().getSimplified(), (OWLClass) filler.getComplementNNF(), alreadyExists);
					if (!alreadyExists[0]){

						anyLhsExistentialsTreated = true;
						
						OWLClassExpression[] aux = new OWLClassExpression[]{c_p_filler, inclusion[indexesForNegativeUniversalsWithTransitiveSubrole[j]]}; 
						finalInclusions.add(aux);

						for (OWLObjectPropertyExpression t : rolesWithTransitiveSubroles.get(p)){
							OWLClass c_invT_cpfiller = getClassForTransitivity(t.getInverseProperty().getSimplified(), c_p_filler, alreadyExists);
							aux = new OWLClassExpression[]{c_invT_cpfiller, factory.getOWLObjectAllValuesFrom(t, filler)};
							finalInclusions.add(aux);
							if (!alreadyExists[0]){
								aux = new OWLClassExpression[]{c_invT_cpfiller, factory.getOWLObjectAllValuesFrom(t, c_invT_cpfiller.getComplementNNF())};
								finalInclusions.add(aux);
								aux = new OWLClassExpression[]{c_invT_cpfiller.getComplementNNF(), c_p_filler};
								finalInclusions.add(aux);
							}
						}
						inclusion[indexesForNegativeUniversalsWithTransitiveSubrole[j]] = c_p_filler.getComplementNNF();
					}
				}
				else{
					throw new IllegalStateException("WHAT?? this was supposed to be a universal restriction!!");
				}
			}
			finalInclusions.add(inclusion);
			
		}
		
//		//////
//		if (anyLhsExistentialsTreated){
//			System.out.println();
//			for (OWLClassExpression c : inclusion)
//				System.out.print(c.toString() + " ");
//			System.out.println();
//			System.out.print("becomes");
//			for (OWLClassExpression[] newInclusion : finalInclusions){
//				System.out.println();
//				System.out.print("    ");
//				for (OWLClassExpression c : newInclusion)
//					System.out.print(c.toString() + " ");
//			}			
//		}
//		//////
		
		return finalInclusions;
	}

	protected Set<OWLClassExpression[]> treatRhsUniversalsForTransitivity(
			OWLClassExpression[] inclusion,
			Integer[] indexesForPositiveUniversalsWithTransitiveSubrole,
			int nPositiveUniversals, int nOtherNegativeDisjuncts, 
			int nNegatedAtomicClasses,
			Map<OWLObjectPropertyExpression, Set<OWLObjectPropertyExpression>> rolesWithTransitiveSubroles, int[] nNewAtomicClasses) {

		Set<OWLClassExpression[]> finalInclusions = new HashSet<OWLClassExpression[]>();
		nNewAtomicClasses = new int[]{0};
		boolean anyRhsUniversalsTreated = false;
		
		//isolate universal restrictions with atomic fillers or not
		if (nPositiveUniversals == 1 && ((nNegatedAtomicClasses == 1 && inclusion.length == 2) || inclusion.length == 1)){
			//then not necessary to isolate because it's already in that form, either  {A->all R.B}  or  {top->all R.B}
			
			finalInclusions.add(inclusion);
			
			OWLObjectPropertyExpression p = ((OWLObjectAllValuesFrom) inclusion[indexesForPositiveUniversalsWithTransitiveSubrole[0]]).getProperty().getSimplified();
			OWLClass filler = (OWLClass) ((OWLObjectAllValuesFrom) inclusion[indexesForPositiveUniversalsWithTransitiveSubrole[0]]).getFiller();
			if (inclusion.length == 1){
				//then it's a range axiom and we don't need to add any extra axioms
				
//				for (OWLObjectPropertyExpression t : rolesWithTransitiveSubroles.get(p)){
//					
//					anyRhsUniversalsTreated = true;
//					
//					boolean[] alreadyExists = new boolean[1];
//					OWLClass c_t_filler = getClassForTransitivity(t,(OWLClass) filler, alreadyExists);
//					finalInclusions.add(new OWLClassExpression[]{factory.getOWLObjectAllValuesFrom(t, c_t_filler)});
//					if (!alreadyExists[0]){
//						finalInclusions.add(new OWLClassExpression[]{c_t_filler.getComplementNNF(), factory.getOWLObjectAllValuesFrom(t, c_t_filler)});
//						finalInclusions.add(new OWLClassExpression[]{c_t_filler.getComplementNNF(), filler});
//					}
//				}
			}
			else{ //inclusion.length == 2
				OWLObjectComplementOf negatedAtomicClass = null;
				for (int i = 0 ; i<inclusion.length ; i++)
					if (inclusion[i].getComplementNNF() instanceof OWLClass)
						negatedAtomicClass = (OWLObjectComplementOf) inclusion[i];
				
				for (OWLObjectPropertyExpression t : rolesWithTransitiveSubroles.get(p)){
					
					anyRhsUniversalsTreated = true;
					
					boolean[] alreadyExists = new boolean[1];
					OWLClass c_t_filler = getClassForTransitivity(t,(OWLClass) filler, alreadyExists);
					finalInclusions.add(new OWLClassExpression[]{negatedAtomicClass, factory.getOWLObjectAllValuesFrom(t, c_t_filler)});
					if (!alreadyExists[0]){
						finalInclusions.add(new OWLClassExpression[]{c_t_filler.getComplementNNF(), factory.getOWLObjectAllValuesFrom(t, c_t_filler)});
						finalInclusions.add(new OWLClassExpression[]{c_t_filler.getComplementNNF(), filler});
					}
				}	
			}
			
		}
		else if (nOtherNegativeDisjuncts > 0){//then we isolate all the rhs universals that involve superroles of a transitive role
			for (int j = 0 ; j<nPositiveUniversals ; j++)//TODO do not isolate if we already have A->allR.B!!
				if (inclusion[indexesForPositiveUniversalsWithTransitiveSubrole[j]] instanceof OWLObjectAllValuesFrom){
					OWLObjectPropertyExpression p = ((OWLObjectAllValuesFrom) inclusion[indexesForPositiveUniversalsWithTransitiveSubrole[j]]).getProperty().getSimplified();
					OWLClassExpression filler = ((OWLObjectAllValuesFrom) inclusion[indexesForPositiveUniversalsWithTransitiveSubrole[j]]).getFiller();
					boolean[] alreadyExists = new boolean[1];
					OWLClass c_p_filler = getDefinitionForUniv(p, (OWLClass) filler, alreadyExists);
					if (!alreadyExists[0]){

						anyRhsUniversalsTreated = true;
						
						finalInclusions.add(new OWLClassExpression[]{c_p_filler.getComplementNNF(), inclusion[indexesForPositiveUniversalsWithTransitiveSubrole[j]]});

						for (OWLObjectPropertyExpression t : rolesWithTransitiveSubroles.get(p)){
							OWLClass c_t_filler = getClassForTransitivity(t,(OWLClass) filler, alreadyExists);
							finalInclusions.add(new OWLClassExpression[]{c_p_filler.getComplementNNF(), factory.getOWLObjectAllValuesFrom(t, c_t_filler)});
							if (!alreadyExists[0]){
								finalInclusions.add(new OWLClassExpression[]{c_t_filler.getComplementNNF(), factory.getOWLObjectAllValuesFrom(t, c_t_filler)});
								finalInclusions.add(new OWLClassExpression[]{c_t_filler.getComplementNNF(), filler});
							}

						}
						inclusion[indexesForPositiveUniversalsWithTransitiveSubrole[j]] = c_p_filler;
						nNewAtomicClasses[0]++;
						//for each of these expressions that we find and replace by an atomic class we can now count as if that class had been there from the start 
						//when we have to decide later whether to rewrite lhs existentials or not
					}
					//if the abbreviation for {all P. Filler} had already been created then all the transitivity propagating axioms 
					//for all its transitive subroles for Filler must have already been created as well

				}
				else{
					throw new IllegalStateException("WHAT?? this was supposed to be a universal restriction!!");
				}
		}
		else{ //then we isolate all but one
			//for now let's just choose the last one to stay
			//we could choose the one with fewer transitive subroles so that we have to add as few aioms with union as possible
			for (int j = 0 ; j<nPositiveUniversals-1 ; j++)
				if (inclusion[indexesForPositiveUniversalsWithTransitiveSubrole[j]] instanceof OWLObjectAllValuesFrom){
					OWLObjectPropertyExpression p = ((OWLObjectAllValuesFrom) inclusion[indexesForPositiveUniversalsWithTransitiveSubrole[j]]).getProperty().getSimplified();
					OWLClassExpression filler = ((OWLObjectAllValuesFrom) inclusion[indexesForPositiveUniversalsWithTransitiveSubrole[j]]).getFiller();
					boolean[] alreadyExists = new boolean[1];
					OWLClass c_p_filler = getDefinitionForUniv(p, (OWLClass) filler, alreadyExists);
					if (!alreadyExists[0]){

						anyRhsUniversalsTreated = true;
						
						finalInclusions.add(new OWLClassExpression[]{c_p_filler.getComplementNNF(), inclusion[indexesForPositiveUniversalsWithTransitiveSubrole[j]]});

						for (OWLObjectPropertyExpression t : rolesWithTransitiveSubroles.get(p)){
							OWLClass c_t_filler = getClassForTransitivity(t,(OWLClass) filler, alreadyExists);
							finalInclusions.add(new OWLClassExpression[]{c_p_filler.getComplementNNF(), factory.getOWLObjectAllValuesFrom(t, c_t_filler)});
							if (!alreadyExists[0]){
								finalInclusions.add(new OWLClassExpression[]{c_t_filler.getComplementNNF(), factory.getOWLObjectAllValuesFrom(t, c_t_filler)});
								finalInclusions.add(new OWLClassExpression[]{c_t_filler.getComplementNNF(), filler});
							}

						}
						inclusion[indexesForPositiveUniversalsWithTransitiveSubrole[j]] = c_p_filler;
						nNewAtomicClasses[0]++;
					}
				}
				else{
					throw new IllegalStateException("WHAT?? this was supposed to be a universal restriction!!");
				}
			//now we handle the one we haven't isolated
			if (nPositiveUniversals > 0){
				//			if (inclusion[indexesForPositiveUniversalsWithTransitiveSubrole[nPositiveUniversals-1]] instanceof OWLObjectAllValuesFrom)
				OWLObjectPropertyExpression p = ((OWLObjectAllValuesFrom) inclusion[indexesForPositiveUniversalsWithTransitiveSubrole[nPositiveUniversals-1]]).getProperty().getSimplified();
				OWLClassExpression filler = ((OWLObjectAllValuesFrom) inclusion[indexesForPositiveUniversalsWithTransitiveSubrole[nPositiveUniversals-1]]).getFiller();

				for (OWLObjectPropertyExpression t : rolesWithTransitiveSubroles.get(p)){
					
					anyRhsUniversalsTreated = true;
					
					OWLClassExpression[] copyOfInclusion = new OWLClassExpression[inclusion.length];
					boolean[] alreadyExists = new boolean[1];
					for (int j = 0 ; j<inclusion.length ; j++)
						copyOfInclusion[j] = inclusion[j];
					OWLClass c_t_filler = getClassForTransitivity(t,(OWLClass) filler, alreadyExists);
					copyOfInclusion[indexesForPositiveUniversalsWithTransitiveSubrole[nPositiveUniversals-1]] = factory.getOWLObjectAllValuesFrom(t, c_t_filler);
					finalInclusions.add(copyOfInclusion);

					if (!alreadyExists[0]){
						finalInclusions.add(new OWLClassExpression[]{c_t_filler.getComplementNNF(), factory.getOWLObjectAllValuesFrom(t, c_t_filler)});
						finalInclusions.add(new OWLClassExpression[]{c_t_filler.getComplementNNF(), filler});
					}

				}

			}
		}
		
//		//////
//		if (anyRhsUniversalsTreated){
//			System.out.println();
//			for (OWLClassExpression c : inclusion)
//				System.out.print(c.toString() + " ");
//			System.out.println();
//			System.out.print("becomes");
//			for (OWLClassExpression[] newInclusion : finalInclusions){
//				System.out.println();
//				System.out.print("    ");
//				for (OWLClassExpression c : newInclusion)
//					System.out.print(c.toString() + " ");
//			}
//		}
//		//////
		
		return finalInclusions;
	}
	
	protected OWLClass getDefinitionForExist(
			OWLObjectPropertyExpression p, OWLClass negatedFiller,
			boolean[] alreadyExists) {
		Map<OWLClass, OWLClass> aux = classesDefExist.get(p.getSimplified()); 
		if (aux != null){
			OWLClass c = aux.get(negatedFiller);
			if (c != null){
				alreadyExists = new boolean[]{true};
				return c;
			}
			else{
				c = getFreshClass();
				aux.put(negatedFiller, c);
				alreadyExists = new boolean[]{false};
				return c;
			}
		}
		else{
			aux = new HashMap<OWLClass, OWLClass>();
			OWLClass c = getFreshClass();
			aux.put(negatedFiller, c);
			classesDefExist.put(p.getSimplified(), aux);
			alreadyExists = new boolean[]{false};
			return c;
		}
	}
	protected OWLClass getDefinitionForUniv(
			OWLObjectPropertyExpression p, OWLClass negatedFiller,
			boolean[] alreadyExists) {
		Map<OWLClass, OWLClass> aux = classesDefUniv.get(p.getSimplified()); 
		if (aux != null){
			OWLClass c = aux.get(negatedFiller);
			if (c != null){
				alreadyExists = new boolean[]{true};
				return c;
			}
			else{
				c = getFreshClass();
				aux.put(negatedFiller, c);
				alreadyExists = new boolean[]{false};
				return c;
			}
		}
		else{
			aux = new HashMap<OWLClass, OWLClass>();
			OWLClass c = getFreshClass();
			aux.put(negatedFiller, c);
			classesDefUniv.put(p.getSimplified(), aux);
			alreadyExists = new boolean[]{false};
			return c;
		}
	}
	protected OWLClass getFreshClass(){
		freshClassCounter++;
		return factory.getOWLClass(IRI.create("internal:transencodingDef#"+(freshClassCounter)));
	}
	protected OWLClass getClassForTransitivity(
			OWLObjectPropertyExpression p, OWLClass filler,
			boolean[] alreadyExists) {
		Map<OWLClass, OWLClass> aux = classesToEncodeTransitivityStandard.get(p.getSimplified()); 
		if (aux != null){
			OWLClass c = aux.get(filler);
			if (c != null){
				alreadyExists = new boolean[]{true};
				return c;
			}
			else{
				c = getFreshClass();
				aux.put(filler, c);
				alreadyExists = new boolean[]{false};
				return c;
			}
		}
		else{
			aux = new HashMap<OWLClass, OWLClass>();
			OWLClass c = getFreshClass();
			aux.put(filler, c);
			classesToEncodeTransitivityStandard.put(p.getSimplified(), aux);
			alreadyExists = new boolean[]{false};
			return c;
		}
	}
	protected OWLClass getClassForTransitivityDisjointness(
			OWLObjectPropertyExpression p, OWLClass filler,
			boolean[] alreadyExists) {
		Map<OWLClass, OWLClass> aux = classesToEncodeTransitivityStandard.get(p.getSimplified()); 
		if (aux != null){
			OWLClass c = aux.get(filler);
			if (c != null){
				alreadyExists = new boolean[]{true};
				return c;
			}
			else{
				c = getFreshClass();
				aux.put(filler, c);
				alreadyExists = new boolean[]{false};
				return c;
			}
		}
		else{
			aux = new HashMap<OWLClass, OWLClass>();
			OWLClass c = getFreshClass();
			aux.put(filler, c);
			classesToEncodeTransitivityStandard.put(p.getSimplified(), aux);
			alreadyExists = new boolean[]{false};
			return c;
		}
	}
	

//	public List<OWLClassExpression[]> integrateDomainsAndRanges(Collection<OWLClassExpression[]> inclusions){
//		
////		completeDomainMapWithRoleHierarchy();
////		
////		//at this point all expressions in inclusions are normalized
////		List<OWLClassExpression[]> integratedInclusions = new ArrayList<OWLClassExpression[]>();
////		for (OWLClassExpression[] description : inclusions){
////			OWLClassExpression[] newDescription = new OWLClassExpression[description.length]; 
////			for (int i = 0 ; i < description.length ; i++)
////				if (description[i] instanceof OWLObjectSomeValuesFrom){
////					OWLObjectPropertyExpression p = ((OWLObjectSomeValuesFrom) description[i]).getProperty();
////					if (generatingRoles.add(p) && roleHierarchy.containsKey(p))
////						generatingRoles.addAll(roleHierarchy.get(p));
////					if (domainMap.containsKey(p.getInverseProperty().getSimplified()))
////						newDescription[i] = factory.getOWLObjectSomeValuesFrom(
////								p, 
////								getIntersection(((OWLObjectSomeValuesFrom) description[i]).getFiller(), domainMap.get(p.getInverseProperty().getSimplified())));
////					else
////						newDescription[i] = description[i];
////				}
////				else
////					newDescription[i] = description[i];
////			
////			integratedInclusions.add(newDescription);
////		}
////		
////		
////		
////		for (OWLObjectPropertyExpression p : generatingRoles)
////			if (domainMap.containsKey(p))
////				integratedInclusions.add(
////						new OWLClassExpression[]{
////								domainMap.get(p),
////								factory.getOWLObjectAllValuesFrom(
////										p,
////										factory.getOWLNothing())
////						});
////		
////		return integratedInclusions;
//	}
	
	protected void computeTransitiveClosureOfRoleHierarchy(){
		
		boolean flag = true;
		while (flag) {
			flag = false;
			for (OWLObjectPropertyExpression p : roleHierarchy.keySet()) {
				Set<OWLObjectPropertyExpression> superRolesCopy = new HashSet<OWLObjectPropertyExpression>(roleHierarchy.get(p));
				for (OWLObjectPropertyExpression s : superRolesCopy)
					if (roleHierarchy.containsKey(s) && roleHierarchy.get(p).addAll(roleHierarchy.get(s)))
						flag = true;
			}
		}
//		/////
//		for (OWLObjectPropertyExpression p : roleHierarchy.keySet()){
//			System.out.println(p.toString());
//			for (OWLObjectPropertyExpression q : roleHierarchy.get(p))
//				System.out.println("	" + q.toString());
//		}
//		/////
		transitiveClosureOfRoleHierarchyComputed = true;
	}
	
	protected int freshRangeIntegClassCounter = -1;
	int nRecycled = 0;
	protected Map<OWLObjectPropertyExpression, Map<OWLClassExpression, OWLClass>> rangeIntegrationClassMap = new HashMap<OWLObjectPropertyExpression, Map<OWLClassExpression,OWLClass>>();
	protected OWLClass getFreshRangeIntegrationClass(OWLObjectPropertyExpression p , OWLClassExpression filler, boolean[] alreadyExists){
		Map<OWLClassExpression, OWLClass> aux = rangeIntegrationClassMap.get(p);
		if (aux != null){
			OWLClass def = aux.get(filler); 
			if (def != null){
				nRecycled++;
				alreadyExists[0] = true;
				return def;
			}
			else{
				freshRangeIntegClassCounter++;
				def = factory.getOWLClass(IRI.create("internal:rangeIntegClass#" + freshRangeIntegClassCounter));
				aux.put(filler, def);
				alreadyExists[0] = false;
				return def;
			}
		}
		else{
			freshRangeIntegClassCounter++;
			OWLClass def = factory.getOWLClass(IRI.create("internal:rangeIntegClass#" + freshRangeIntegClassCounter));
			aux = new HashMap<OWLClassExpression, OWLClass>();
			aux.put(filler, def);
			rangeIntegrationClassMap.put(p, aux);
			alreadyExists[0] = false;
			return def;
		}
	}
//	protected int freshDomainClassCounter = -1;
//	protected OWLClass getFreshDomainClass(){
//		freshDomainClassCounter++;
//		return factory.getOWLClass(IRI.create("internal: domainClass#" + freshDomainClassCounter));
//	}
	
	protected List<OWLClassExpression[]> completeRangeMapWithRoleHierarchy() {
		
		//when registering domain and range axioms we have stored all the info in the domainMap
		//now we will transfer this info to the rangeMap as well, and here we will also get the information that the role hierarchy gives
		
		//we are going to keep a copy of everything domain-wise, and also range-wise
		//the difference between the two will be that, for the domain, we will just keep, for each propertyExpression, a list of the 
		//classes stated as their domain or as the range of their inverse - without using the transitive closure of the role hierarchy;
		//for the range, we take the list gathered for the domain of the inverse, and if it contains more than one class
		//we will introduce an alias X_r specific for role r, and axioms X_r -> A for each class A in the corresponding list. 
		List<OWLClassExpression[]> additionalInclusions = new LinkedList<OWLClassExpression[]>();
		
		for (OWLObjectPropertyExpression p : rangeMap.keySet()){
			OWLObjectPropertyExpression pInv = p.getInverseProperty().getSimplified();
			if (domainMap.containsKey(pInv))
				domainMap.get(pInv).addAll(rangeMap.get(p));
			else
				domainMap.put(pInv, rangeMap.get(p));
		}
		
		if (!transitiveClosureOfRoleHierarchyComputed)
			computeTransitiveClosureOfRoleHierarchy();
		
		for (OWLObjectPropertyExpression p : rangeMap.keySet()){
			Set<OWLObjectPropertyExpression> aux = roleHierarchy.get(p);
			if (aux != null)
				for (OWLObjectPropertyExpression q : aux){
					Set<OWLClass> aux2 = rangeMap.get(q);
					if (aux2 != null)
						rangeMap.get(p).addAll(aux2);
				}
		}
		//Instead of adding a fresh class for the range that points to all the ranges, let's just keep the whole list, and only introduce fresh classes when we actually replace the filler in the rhs existential restriction
//		for (OWLObjectPropertyExpression p : rangeMap.keySet()){
//			Set<OWLClass> rangeSet = rangeMap.get(p);
//			if (rangeSet.size() > 1){
//				OWLClass freshRangeClass = getFreshRangeClass();
//				for (OWLClass c : rangeSet)
//					additionalInclusions.add(new OWLClassExpression[]{freshRangeClass.getComplementNNF(), c});
//				rangeSet.clear();
//				rangeSet.add(freshRangeClass);
//
//				////
//				System.out.println(freshRangeClass.toString());
//				System.out.println("should be the same as");
//				for (OWLClass c : rangeMap.get(p))
//					System.out.println(c.toString());
//				////
//			}
//		}
		return additionalInclusions;
	}

	public List<OWLClassExpression[]> handleRangeAndDomainAxioms(
			List<OWLClassExpression[]> conceptInclusions,
			boolean integrateRangesInRhsExistentials) {
		List<OWLClassExpression[]> updatedInclusions = new LinkedList<OWLClassExpression[]>();
		if (integrateRangesInRhsExistentials){
			updatedInclusions = completeRangeMapWithRoleHierarchy();
			/////NOT ANYMORE ->//all the values in rangeMap at this point should be singleton sets
			
			//loop through all the given inclusions adding the necessary information for ranges
			//and find generating roles as we go
			while (!conceptInclusions.isEmpty()){
				OWLClassExpression[] inclusion = conceptInclusions.remove(0);
				OWLClassExpression[] newInclusion = new OWLClassExpression[inclusion.length];
				for (int i = 0 ; i < inclusion.length ; i++)
					//for each existential restriction for a role that has a range, we will get a fresh class associated to the role and filler - which should by now be atomic, 
					//since the class normalizer should have been applied to the inclusions before they get sent here
					if (inclusion[i] instanceof OWLObjectSomeValuesFrom){
						OWLObjectPropertyExpression p = ((OWLObjectSomeValuesFrom) inclusion[i]).getProperty().getSimplified();
						OWLClassExpression filler = ((OWLObjectSomeValuesFrom) inclusion[i]).getFiller();
						if (generatingRoles.add(p) && roleHierarchy.containsKey(p))
							generatingRoles.addAll(roleHierarchy.get(p));
						
						
						
//						/////
//						if (p.toString().equals("<http://bioonto.de/ro2.owl#realized-by>"))
//							System.out.println(inclusion[i]);
//						if (roleHierarchy.get(p) != null)
//							for (OWLObjectPropertyExpression q : roleHierarchy.get(p))
//								if (q.toString().equals("<http://bioonto.de/ro2.owl#realized-by>")){
//									System.out.println(inclusion[i]);
//								}
//						/////
						
						
						
						Set<OWLClass> aux = rangeMap.get(p);
						if (aux != null){
							boolean[] alreadyExists = new boolean[1];
							OWLClass def = getFreshRangeIntegrationClass(p, filler, alreadyExists);
							newInclusion[i] = factory.getOWLObjectSomeValuesFrom(p, def);
							if (!alreadyExists[0]){
								updatedInclusions.add(new OWLClassExpression[]{def.getComplementNNF(), filler});//add inclusions of def in every class in aux and also the filler
								for (OWLClass c : aux)
									updatedInclusions.add(new OWLClassExpression[]{def.getComplementNNF(), c});//add inclusions of def in every class in aux and also the filler
							}
						}
						else
							newInclusion[i] = inclusion[i];
					}
					else
						newInclusion[i] = inclusion[i];
				updatedInclusions.add(newInclusion);
			}
			
			
//			// superroles of generating roles are also generating
//			Set<OWLObjectPropertyExpression> aux = new HashSet<OWLObjectPropertyExpression>();
//			//TODO
//			!!!
			
			//finally, we'll add the domain axioms  -  (for the roles that turn out to be generating)? or for all of them?  
			for (OWLObjectPropertyExpression p : generatingRoles){
				Set<OWLClass> domainSet = domainMap.get(p);
				if (domainSet != null)
					for (OWLClass dom : domainSet){
						OWLObjectAllValuesFrom allPropertyNothing = factory.getOWLObjectAllValuesFrom(p, factory.getOWLNothing());
						updatedInclusions.add(new OWLClassExpression[] { dom, allPropertyNothing });
					}
			}
		}
		else{
			updatedInclusions = new ArrayList<OWLClassExpression[]>(conceptInclusions);
			for (OWLObjectPropertyExpression p : domainMap.keySet()){
				for (OWLClass domain : domainMap.get(p)){
					OWLObjectAllValuesFrom allPropertyNothing = factory.getOWLObjectAllValuesFrom(p, factory.getOWLNothing());
					updatedInclusions.add(new OWLClassExpression[] { domain, allPropertyNothing });
				}
			}
			for (OWLObjectPropertyExpression p : rangeMap.keySet()){
				for (OWLClass range : rangeMap.get(p)){
					OWLObjectAllValuesFrom allPropertyRange = factory.getOWLObjectAllValuesFrom(p,range);
					updatedInclusions.add(new OWLClassExpression[] { allPropertyRange });
				}
			}
		}
		
		
		System.out.println(nRecycled +  " times we recycled a previously created def for some rhs existential restriction");
		
		
//		System.out.println(" generating roles:");
//		for (OWLObjectPropertyExpression p : generatingRoles)
//			System.out.println(p.toString());
//		System.out.println();
		
		return updatedInclusions;
	}

	
}

