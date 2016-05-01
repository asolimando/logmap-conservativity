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
package gui;

import java.util.List;

import graphViz.GraphCache;
import graphViz.myConservativityGraph;

import javax.swing.SwingWorker;

import org.semanticweb.owlapi.model.OWLClass;

import auxStructures.Pair;

import logmap.LogMapWrapper;
import scc.graphViz.SCCGraphCache;
import util.FileUtil;

public class DiagnosisTask extends SwingWorker<Void, Void>{

	private VisualConservativity vc;
	private List<Pair<OWLClass>> disj;
	private List<Pair<Integer>> disjIdx;
	
	public DiagnosisTask(VisualConservativity vc, List<Pair<OWLClass>> disj) {
		this.vc = vc;
		this.disj = disj;
	}

	public DiagnosisTask(List<Pair<Integer>> viols, VisualConservativity vc) {
		this.vc = vc;
		this.disjIdx = viols;
	}

	@Override
	public Void doInBackground() {
		if(disjIdx != null && !disjIdx.isEmpty())
			vc.getRepairFacility().repair(disjIdx);
		else if(disj == null || disj.isEmpty())
			vc.getRepairFacility().repair();
		else 
			vc.getRepairFacility().repair(
					LogMapWrapper.getPairsOfIdentifiersFromPairsOfClasses(
							disj, vc.getRepairFacility().getAlignOntoProc()));		
		return null;
	}

	/*
	 * Executed in event dispatch thread
	 */
	public void done() {
		vc.addRemovedMappings(LogMapWrapper.getOWLClassesFromMappings(
				vc.getRepairFacility().getRepair()));
		vc.setComputingStatus(false);
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				for (GraphCache cache : vc.graphCache.values())
					cache.getGraph().markAsDeleted(vc.getRemovedMappings(),
							myConservativityGraph.deletedStyle);
//				vc.graph.markAsDeleted(vc.getRemovedMappings(),
//						myGraph.deletedStyle);
				vc.refreshGraph();
			}
		});
//		vc.jButtonDiag.setEnabled(false);
		vc.jButtonSaveMapping.setEnabled(true);
		
		FileUtil.writeLogAndConsole(vc.getRemovedMappings().size() + 
				" removed mapping(s): " + vc.getRemovedMappings().toString());
	}
} 
