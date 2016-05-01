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
package scc.gui;

import scc.graphDataStructure.LightSCC;
import scc.graphViz.SCCGraphCache;
import scc.graphViz.mySCCGraph;
import scc.graphViz.myMappingKeyboardHandler;

import javax.swing.SwingWorker;

import util.Params;

import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.layout.mxEdgeLabelLayout;
import com.mxgraph.layout.mxGraphLayout;
import com.mxgraph.layout.mxParallelEdgeLayout;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.mxGraphOutline;
import com.mxgraph.swing.handler.mxRubberband;

public class GraphBuilderTask extends SwingWorker<Void, Void> {

	private LightSCC scc;
	private VisualDebugger vd;

	public GraphBuilderTask(LightSCC scc, VisualDebugger vd){
		this.scc = scc;
		this.vd = vd;
	}

	@Override
	public Void doInBackground() {
		if(vd.graphCache.containsKey(vd.getSelectedSCC())){
			SCCGraphCache cache = vd.graphCache.get(vd.getSelectedSCC());
			vd.graph = cache.getGraph();
			vd.graphComponent = cache.getGraphComponent();
			vd.graphOutline = cache.getGraphOutline();
		}
		else {
			try {
				vd.graph = new mySCCGraph(vd.getAdjancencyList(), scc, vd);
				vd.graph.getModel().beginUpdate();
				vd.graph.buildGraphFromSCC(null);
				if(vd.localDiagnoses.containsKey(scc))
					vd.graph.markAsDeleted(vd.localDiagnoses.get(scc),
							mySCCGraph.deletedStyle);
				if(vd.localFilters.containsKey(scc))
					vd.graph.markAsDeleted(vd.localFilters.get(scc),
							mySCCGraph.filteredStyle);				
				vd.graph.setCollapseToPreferredSize(false);

				mxGraphLayout graphLayout = new mxCircleLayout(vd.graph);
				//mxGraphLayout graphLayout = new mxFastOrganicLayout(graph);
				mxGraphLayout graphLayoutPar = new mxParallelEdgeLayout(vd.graph);
				mxGraphLayout graphLayoutEdge = new mxEdgeLabelLayout(vd.graph);
				if(Params.verbosity > 0)
					System.out.print("Computing graph layout...");
				graphLayout.execute(vd.graph.getDefaultParent());
				graphLayoutPar.execute(vd.graph.getDefaultParent());
				graphLayoutEdge.execute(vd.graph.getDefaultParent());
				if(Params.verbosity > 0)
					System.out.println(" DONE!");

				if(Params.verbosity > 0)
					System.out.print("Computing graph representation...");
				vd.graphComponent = new mxGraphComponent(vd.graph);
				// prevent creation of new edges
				vd.graphComponent.setConnectable(false);
				vd.graphOutline = new mxGraphOutline(vd.graphComponent);
				vd.installListeners();

				//new mxRubberband(vd.graphComponent);
				new myMappingKeyboardHandler(vd.graphComponent);
				if(Params.verbosity > 0)
					System.out.println(" DONE!");
				
				// cache the graph elements
				vd.graphCache.put(vd.getSelectedSCC(),
						new SCCGraphCache(vd.graph,
								vd.graphComponent,vd.graphOutline));
			}
			finally {
				vd.graph.getModel().endUpdate();
			}
		}
		int divLoc = vd.splitPaneSCCs.getDividerLocation();
		if(vd.splitPaneSCCs.getComponentCount() > 2)
			vd.splitPaneSCCs.remove(2);

		vd.splitPaneSCCs.add(vd.graphComponent);

		vd.splitPaneSCCs.validate();
		vd.splitPaneSCCs.revalidate();

		vd.splitPaneSCCs.setDividerLocation(divLoc);

		return null;
	}

	/*
	 * Executed in event dispatch thread
	 */
	public void done() {
		vd.setComputingStatus(false);
		vd.statusLabel.setText(vd.getSelectedSCC().printDimensions(
				vd.getAdjancencyList()));
	}
}
