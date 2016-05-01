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

import graphViz.GraphCache;
import scc.graphViz.myMappingKeyboardHandler;
import graphViz.myConservativityGraph;

import javax.swing.SwingWorker;

import org.semanticweb.owlapi.model.OWLClass;

import util.Params;
import auxStructures.Pair;

import com.mxgraph.layout.mxGraphLayout;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.mxGraphOutline;

public class GraphBuilderTask extends SwingWorker<Void, Void> {

	private Pair<OWLClass> v;
	private VisualConservativity vc;

	public GraphBuilderTask(Pair<OWLClass> v, VisualConservativity vc){
		this.v = v;
		this.vc = vc;
	}

	@Override
	public Void doInBackground() {
		if(vc.graphCache.containsKey(vc.getSelectedViolation())){
			GraphCache cache = vc.graphCache.get(vc.getSelectedViolation());
			vc.graph = cache.getGraph();
			vc.graphComponent = cache.getGraphComponent();
			vc.graphOutline = cache.getGraphOutline();
		}
		else {
			try {
				vc.graph = new myConservativityGraph(vc.getAdjancencyList(), v, vc);
				vc.graph.getModel().beginUpdate();
//				vc.graph.buildModuleGraph();
				vc.graph.buildGraph();
				vc.graph.markAsDeleted(vc.getRemovedMappings(), myConservativityGraph.deletedStyle);
				vc.graph.setCollapseToPreferredSize(false);

				//mxGraphLayout graphLayout = new mxCircleLayout(vc.graph);
				mxGraphLayout graphLayout = new mxHierarchicalLayout(vc.graph);
//				mxGraphLayout graphLayoutPar = new mxParallelEdgeLayout(vc.graph);
//				mxGraphLayout graphLayoutEdge = new mxEdgeLabelLayout(vc.graph);
				if(Params.verbosity > 0)
					System.out.print("Computing graph layout...");
				graphLayout.execute(vc.graph.getDefaultParent());
//				graphLayoutPar.execute(vc.graph.getDefaultParent());
//				graphLayoutEdge.execute(vc.graph.getDefaultParent());
				if(Params.verbosity > 0)
					System.out.println(" DONE!");

				if(Params.verbosity > 0)
					System.out.print("Computing graph representation...");
				vc.graphComponent = new mxGraphComponent(vc.graph);
				// prevent creation of new edges
				vc.graphComponent.setConnectable(false);
				vc.graphOutline = new mxGraphOutline(vc.graphComponent);
				vc.installListeners();

				//new mxRubberband(vd.graphComponent);
				new myMappingKeyboardHandler(vc.graphComponent);
				if(Params.verbosity > 0)
					System.out.println(" DONE!");
				
				// cache the graph elements
				vc.graphCache.put(vc.getSelectedViolation(),
						new GraphCache(vc.graph,
								vc.graphComponent,vc.graphOutline));
			}
			finally {
				vc.graph.getModel().endUpdate();
			}
		}
		int divLoc = vc.splitPaneSCCs.getDividerLocation();
		if(vc.splitPaneSCCs.getComponentCount() > 2)
			vc.splitPaneSCCs.remove(2);

		vc.splitPaneSCCs.add(vc.graphComponent);

		vc.splitPaneSCCs.validate();
		vc.splitPaneSCCs.revalidate();

		vc.splitPaneSCCs.setDividerLocation(divLoc);

		return null;
	}

	/*
	 * Executed in event dispatch thread
	 */
	public void done() {
		vc.setComputingStatus(false);
		vc.statusLabel.setText(vc.getSelectedViolation().toString());
	}
}
