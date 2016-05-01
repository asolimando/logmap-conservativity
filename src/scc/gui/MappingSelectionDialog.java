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

import scc.graphDataStructure.LightEdge;
import scc.graphDataStructure.LightSCC;
import scc.graphViz.mySCCGraph;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;

import util.Params;

import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.layout.mxEdgeLabelLayout;
import com.mxgraph.layout.mxGraphLayout;
import com.mxgraph.layout.mxParallelEdgeLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.mxGraphOutline;

class MappingSelectionDialog extends JDialog implements ActionListener {

	private static final long serialVersionUID = -4348383057307973879L;

	private VisualDebugger vd;	
	private JButton deleteButton = new JButton("Delete"), 
			cancelButton = new JButton("Cancel");
	private mySCCGraph graph;
	public mxGraphComponent graphComponent;
	public mxGraphOutline graphOutline;

	public MappingSelectionDialog(InstancesDialog id, 
			VisualDebugger vd, 
			Set<LightEdge> visibleMappings){

		super(id, "Delete mapping", true);
		this.vd = vd;

		// this.setPreferredSize(new Dimension(200,100));
		JRootPane rootPane = this.getRootPane();
		// applybutton receive Enter by default
		rootPane.setDefaultButton(deleteButton);

		/* add listener */
		deleteButton.addActionListener(this);
		cancelButton.addActionListener(this);
		/* end add listener */

		JPanel sizePanel = new JPanel(new GridLayout(1,2));
		sizePanel.setBorder(BorderFactory.createTitledBorder(" Parameters "));

		buildGraph(vd.getSelectedSCC(), visibleMappings);
		sizePanel.add(graphComponent);

		JPanel buttonPanel = new JPanel(new FlowLayout());
		buttonPanel.add(deleteButton);
		buttonPanel.add(cancelButton);

		Container mainPanel = getContentPane();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.add(sizePanel);
		mainPanel.add(buttonPanel);		

		this.setResizable(false);
		this.pack();
		setLocationRelativeTo(vd);
		setVisible(true);
	}

	private void buildGraph(LightSCC scc, Set<LightEdge> visibleMapping){
		try {
			graph = new mySCCGraph(vd.getAdjancencyList(), scc, vd);
			graph.getModel().beginUpdate();
			graph.buildGraphFromSCC(visibleMapping);
			graph.setCollapseToPreferredSize(false);

			mxGraphLayout graphLayout = new mxCircleLayout(graph);
			mxGraphLayout graphLayoutPar = new mxParallelEdgeLayout(graph);
			mxGraphLayout graphLayoutEdge = new mxEdgeLabelLayout(graph);
			if(Params.verbosity > 0)
				System.out.print("Computing graph layout...");
			graphLayout.execute(graph.getDefaultParent());
			graphLayoutPar.execute(graph.getDefaultParent());
			graphLayoutEdge.execute(graph.getDefaultParent());
			if(Params.verbosity > 0)
				System.out.println(" DONE!");

			if(Params.verbosity > 0)
				System.out.print("Computing graph representation...");
			graphComponent = new mxGraphComponent(graph);
			// prevent creation of new edges
			graphComponent.setConnectable(false);
			graphOutline = new mxGraphOutline(graphComponent);

			if(Params.verbosity > 0)
				System.out.println(" DONE!");
		}
		finally {
			graph.getModel().endUpdate();
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		Object source = e.getSource();
		JButton button = (JButton) source;

		if(button == deleteButton){
			mxCell sel = (mxCell) graph.getSelectionCell();
			if(sel == null || sel.isVertex() || !graph.isMapping(sel)){
				JOptionPane.showMessageDialog(vd, 
						"Mapping not valid for deletion", 
						"Deletion impossible",
					    JOptionPane.ERROR_MESSAGE);
				return;
			}
			else if(vd.isSealed(vd.getSelectedSCC(), graph.getEdgeFromCell(sel))){
				JOptionPane.showMessageDialog(vd, 
						"The mapping is sealed, if you want to delete it, " +
						"unseal it first.", 
						"Deletion impossible",
					    JOptionPane.ERROR_MESSAGE);
				return;
			}
			else
				vd.graph.manualMappingDeletion(
						vd.graph.getCellFromEdge(graph.getEdgeFromCell(sel)), 
						true);
		}
//		else if(button == cancelButton) {}
		dispose();
	}
}