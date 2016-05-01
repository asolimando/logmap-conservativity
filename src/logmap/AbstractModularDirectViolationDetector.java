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
package logmap;

import scc.graphAlgo.DFSReachability;
import scc.graphAlgo.NodeReachability;
import scc.graphDataStructure.LightAdjacencyList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;

import com.google.common.collect.Sets;

import comparator.AbstractViolationComparator;

import uk.ac.ox.krr.logmap2.indexing.JointIndexManager;
import uk.ac.ox.krr.logmap2.indexing.OntologyProcessing;
import util.FileUtil;
import util.OntoUtil;
import util.Util;
import auxStructures.Pair;

public abstract class AbstractModularDirectViolationDetector {

	private OWLOntology fstOnto, sndOnto, alignOnto; 
	private Pair<List<Pair<Integer>>> viols;
	protected JointIndexManager origIdx;
	protected JointIndexManager alignIdx;
	private List<Pair<Integer>> dirViol1 = new ArrayList<>();
	private List<Pair<Integer>> dirViol2 = new ArrayList<>();

	private int computedModules;
	protected List<Pair<Integer>> violId1;
	protected List<Pair<Integer>> violId2;

	protected AbstractViolationComparator vc;
	
	protected OntologyProcessing alignProc;
	
	public AbstractModularDirectViolationDetector(OWLOntology fstOnto, 
			OWLOntology sndOnto, OWLOntology alignOnto, 
			Pair<List<Pair<Integer>>> viols, JointIndexManager origIdx, 
			JointIndexManager alignIdx, OntologyProcessing alignProc){
		
		this.fstOnto = fstOnto;
		this.sndOnto = sndOnto;
		this.alignOnto = alignOnto;
		this.viols = viols;
		this.origIdx = origIdx;
		this.alignIdx = alignIdx;
		this.alignProc = alignProc;
		
		violId1 = viols.getFirst();
		violId2 = viols.getSecond();
	}
	
	protected void sortViolations(){
		if(vc != null){
			Collections.sort(violId1, vc);
			Collections.sort(violId2, vc);
		}
	}

	protected Pair<OWLClass> getOWLClass(Pair<Integer> p){
		return LogMapWrapper.getOWLClassFromIndexPair(p, origIdx);
	}
	
	protected abstract boolean canReuseModule(int id, int moduleIdx);

	protected abstract int nextModuleIndex(int id);
	
	protected abstract boolean sameModule(int id, int moduleIdx);
	
	protected abstract Set<OWLClass> getSeedForModule(int moduleIdx, 
			Pair<OWLClass> v);
	
	public List<Pair<Integer>> detectDirectViolations(
			List<Pair<Integer>> dirViol, List<Pair<Integer>> violId){
		
		OWLOntology module = null;
		
		// build the graph for the needed module only		
		LightAdjacencyList adj = null;
		NodeReachability r = null;

		boolean reuseModule;
		int c = 0, moduleIdx = -2;
		
		for (Pair<Integer> vi : violId){
			Pair<OWLClass> v = getOWLClass(vi);
			
			if(c == 0 || !sameModule(violId.get(c).getFirst(), moduleIdx)){
				reuseModule = c > 0 && 
						canReuseModule(violId.get(c).getFirst(), moduleIdx);
				
				if(!reuseModule){
					++computedModules;
					moduleIdx = nextModuleIndex(violId.get(c).getFirst());
					module = OntoUtil.moduleExtractor(alignOnto, 
							Sets.<OWLEntity>newHashSet(
									getSeedForModule(moduleIdx, v)));

					adj = new LightAdjacencyList(fstOnto, sndOnto, module);
					r = new DFSReachability(adj, false);
				}
			}

			if(c > 0 && c % 10000 == 0)
				FileUtil.writeLogAndConsole(c + "/" + violId.size() + 
//						" (" + computedModules + " modules)" +
								" - " + Util.getCurrTime());

			try {
				if(r.areReachable(v.getFirst(), v.getSecond()))
					dirViol.add(violId.get(c));
//				System.out.println(v);
			}
			catch(NullPointerException e){
				FileUtil.writeErrorLogAndConsole(v + " caused an error");

				computedModules++;
				
				r = new DFSReachability(adj = 
						new LightAdjacencyList(fstOnto, sndOnto, 
								module = 
						OntoUtil.moduleExtractor(alignOnto, 
						Collections.singleton((OWLEntity) v.getFirst()))), 
						false);
				if(r.areReachable(v.getFirst(), v.getSecond()))
					dirViol.add(violId.get(c));
				
				moduleIdx = vi.getFirst();
			}
			++c;
		}
		
		return dirViol;
	}
	
	public Pair<List<Pair<Integer>>> detectDirectViolations(){

		sortViolations();
		
		detectDirectViolations(dirViol1, violId1);
		detectDirectViolations(dirViol2, violId2);
		
		FileUtil.writeLogAndConsole("Computed modules: " + 
				computedModules + "/" + (violId1.size()+violId2.size()));

		return new Pair<>(dirViol1,dirViol2);
	}
}
