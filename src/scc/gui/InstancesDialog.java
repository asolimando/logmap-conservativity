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

import scc.graphDataStructure.LightAdjacencyList;
import scc.graphDataStructure.LightEdge;
import scc.graphDataStructure.LightIndividual;
import scc.graphDataStructure.LightNode;
import scc.graphDataStructure.LightSCC;

import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;

class InstancesDialog extends JDialog implements ActionListener {

	private static final long serialVersionUID = -4348383057307973879L;

	private VisualDebugger vd;

	//	private JLabel verbosityLabel = new JLabel("Verbosity: "), 
	//			aspTimeoutLabel = new JLabel("ASP Timeout (sec): ");
	//
	//	private JTextField verbosityField = new JTextField(), 
	//			aspTimeoutField = new JTextField();	
	private ComboBoxModel<LightIndividual> comboModel;
	private ComboBoxModel<LightNode> otherModel = new DefaultComboBoxModel<>();

	private JComboBox<LightIndividual> indBox;
	private JComboBox<LightNode> otherBox = new JComboBox<>(otherModel);

	private JButton debugButton = new JButton("Debug"), 
			cancelButton = new JButton("Cancel");
	private Map<LightNode, Map<LightNode, LinkedList<LightEdge>>> 
	nodeToNodeReversePathMap = new HashMap<>(); 

	public InstancesDialog(VisualDebugger vd){
		super(vd, "Instances", true);

		this.vd = vd;

		// this.setPreferredSize(new Dimension(200,100));
		JRootPane rootPane = this.getRootPane();
		// applybutton receive Enter by default
		rootPane.setDefaultButton(debugButton);

		/* add listener */
		debugButton.addActionListener(this);
		cancelButton.addActionListener(this);
		/* end add listener */

		JPanel sizePanel = new JPanel(new GridLayout(1,2));
		sizePanel.setBorder(BorderFactory.createTitledBorder(" Parameters "));

		setupIndivComboBox();
		sizePanel.add(indBox);
		sizePanel.add(otherBox);

		JPanel buttonPanel = new JPanel(new FlowLayout());
		buttonPanel.add(debugButton);
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

	private void setupIndivComboBox(){
		LightAdjacencyList adj = vd.getAdjancencyList();
		LightSCC scc = vd.getSelectedSCC();
		Set<LightIndividual> inds = new HashSet<>();
		Set<LightIndividual> tmpInd;

		for (LightNode n : scc){
			tmpInd = n.getIndividuals(adj);
			if(tmpInd != null && !tmpInd.isEmpty()){				
				Set<LightNode> otherNodes = new HashSet<>(scc);
				otherNodes.remove(n);
				Map<LightNode, LinkedList<LightEdge>> nodePathMap = 
						new HashMap<LightNode, LinkedList<LightEdge>>();
				nodeToNodeReversePathMap.put(n, nodePathMap);
				LinkedList<LightEdge> path = null;
				for (LightNode on : otherNodes) {
					LightEdge e = adj.getEdgeBetweenNodes(n, on); 
					if(e != null){
						if(e.mapping){
							path = new LinkedList<>();
							path.add(e);
						}
						// if "e" is an axiom we assume the subsumption is correct 
						// (the input ontologies are considered as correct)
						
						continue;
					}
						
					path = scc.BFS(n, on, adj, true);
					if(path != null){
						nodePathMap.put(on, path);
					}
				}
				// no reachable nodes, "n" is useless and is removed
				if(nodePathMap.isEmpty())
					nodeToNodeReversePathMap.remove(n);
				else
					inds.addAll(tmpInd);
			}			
		}

		comboModel = new DefaultComboBoxModel<>(
				inds.toArray(new LightIndividual[0]));
		
		if(comboModel.getSize() == 0){
			JOptionPane.showMessageDialog(null,
					"The actual diagnosis already solved all the paths " +
					"debuggable using individuals.",
							"No debuggable paths",
							JOptionPane.INFORMATION_MESSAGE);
			dispose();
			return;
		}
		
		indBox = new JComboBox<>(comboModel);
		indBox.addActionListener(this);
		indBox.setSelectedIndex(-1);
		indBox.setSelectedIndex(0);
	}

	public LightIndividual getSelectedInstance(){
		if(indBox == null || indBox.getSelectedIndex() < 0)
			return null;
		return (LightIndividual) indBox.getSelectedItem();
	}

	public LightNode getSelectedTargetNode(){
		if(otherBox == null || otherBox.getSelectedIndex() < 0)
			return null;
		return (LightNode) otherBox.getSelectedItem();
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		Object source = e.getSource();

		if(source instanceof JButton){
			JButton button = (JButton) source;

			if(button == debugButton){
				LightNode start = getSelectedInstance().getNode(),
						end = getSelectedTargetNode();
				LightAdjacencyList adj = vd.getAdjancencyList();

				LinkedList<LightEdge> path = 
						vd.getSelectedSCC().BFS(start, end, adj, true); 
				
				for (LightEdge m : path)
					if(!m.mapping)
						path.remove(m);
				
				new MappingSelectionDialog(this,vd,
						new HashSet<LightEdge>(path));
			}
			//			else if(button == cancelButton) {}
			dispose();
		}
		else {
			JComboBox cb = (JComboBox) source;
			if(cb.getSelectedIndex() == -1)
				return;
			LightIndividual ind = (LightIndividual) cb.getSelectedItem();
			LightSCC scc = vd.getSelectedSCC();
			//	        Set<LightNode> otherNodes = new HashSet<>(scc);
			//	        otherNodes.remove(ind.getNode());
			//	        otherModel = new DefaultComboBoxModel<LightNode>(
			//	        		otherNodes.toArray(new LightNode[0]));
			otherModel = new DefaultComboBoxModel<LightNode>(
					nodeToNodeReversePathMap.get(
							ind.getNode()).keySet().toArray(new LightNode[0]));
			otherBox.setModel(otherModel);
			otherBox.updateUI();
		}
	}
}