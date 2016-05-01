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
package graphViz;

import gui.VisualConservativity;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;

import scc.graphDataStructure.LightAdjacencyList;
import scc.graphDataStructure.LightEdge;
import scc.graphDataStructure.LightNode;
import util.OntoUtil;
import util.Params;
import auxStructures.Pair;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxICell;
import com.mxgraph.util.mxUtils;
import com.mxgraph.view.mxGraph;

public class myConservativityGraph extends mxGraph {

	public static final String 
	vertexStyle1 = "fillColor=" + mxUtils.getHexColorString(Color.cyan) 
	+ ";editable=false",
	vertexStyle2 = "fillColor=" + mxUtils.getHexColorString(Color.green) 
	+ ";editable=false",
	mappingStyle = "strokeWidth=2;strokeColor=#000000",
	deletedStyle = "strokeWidth=2;strokeColor=#FF0000",
	filteredStyle = "strokeWidth=2;strokeColor=#FF00FF",
	manualDeletedStyle = "strokeWidth=2;strokeColor=" 
			+ mxUtils.getHexColorString(Color.orange),
	axiomStyle = "editable=false";

	public mxCell [] groups = new mxCell[2];
	private int vertexID = 0;

	private Map<mxCell, LightEdge> cellEdgeMap = new HashMap<mxCell, LightEdge>();
	private Map<mxCell, LightNode> cellNodeMap = new HashMap<mxCell, LightNode>();
	private Map<LightEdge, mxCell> edgeCellMap = new HashMap<LightEdge, mxCell>();
	private Map<LightNode, mxCell> nodeCellMap = new HashMap<LightNode, mxCell>();

	private LightAdjacencyList adj;
	private VisualConservativity vc;
	private Pair<OWLClass> v;

//	private Stack<LightNode> s;
	
	public myConservativityGraph(LightAdjacencyList adj, Pair<OWLClass> v, VisualConservativity vc){
		this.adj = adj;
		this.vc = vc;
		this.v = v;
		
//		s = new DFSReachabilityPath(adj,true,v.getFirst(),v.getSecond()).getPath();
		
		setDisconnectOnMove(false);
		setDropEnabled(false);
		setAllowNegativeCoordinates(false);
		setSwimlaneNesting(true);
		setCellsCloneable(false);
		setCellsDisconnectable(false);
		setAllowLoops(false);
		setSplitEnabled(false);
		setAllowDanglingEdges(false);
		setCellsDeletable(false);
		setEdgeLabelsMovable(true);
	}

//	public Stack<LightNode> getNodes(){
//		return s;
//	}
	
	public boolean isMapping(mxCell c){
		return cellEdgeMap.containsKey(c) && cellEdgeMap.get(c).mapping;
	}
	
	@Override
	public void cellLabelChanged(Object cell, Object value, boolean autoSize) {
		mxCell cl = (mxCell) cell;
		// vertex label can not be changed, while edge labels can
		if(cl.isVertex())
			return;
		
//		if(vc.isModified(vc.getSelectedSCC())){
//			JOptionPane.showMessageDialog(vd,
//					"Impossible to change confidence values on a modified " +
//					"SCC, reset it first.",
//				    "Change not possible!",
//				    JOptionPane.ERROR_MESSAGE);
//			return;
//		}
		
		double val = 0;
		String errorStr = null;
		try {
			val = Double.parseDouble((String) value);
			if(val <= 0 || val > 1)
				errorStr = "Confidence value must be in real range (0,1].";
		} catch(NumberFormatException e1){
			errorStr = "Malformed confidence value " + value;
		}
		
		if(errorStr != null){
			JOptionPane.showMessageDialog(vc,
					errorStr,
					"Malformed value",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		model.beginUpdate();
		try {
			getModel().setValue(cell, value);
			
			if (autoSize)
				cellSizeUpdated(cell, false);
		}
		finally {
			model.endUpdate();
			cellEdgeMap.get(cl).confidence = val;
			vc.refreshGraph();
		}
	}
	
	public void buildModuleGraph(){
		OWLOntology fstO = adj.getOntology(0), 
				sndO = adj.getOntology(1), 
				module, onto;
		
		onto = fstO.getClassesInSignature(true).contains(v.getFirst()) 
				? fstO : sndO;
		
		module = OntoUtil.moduleExtractor(onto, 
				new HashSet<OWLEntity>(v.asSet()));
		
		for (OWLClass cls : module.getClassesInSignature()) {
			LightNode n = adj.getNodeFromClass(cls);
			insertVertex(n);
			
			List<LightEdge> neighbours = new ArrayList<>(adj.getAdjacent(n));
			neighbours.addAll(adj.getReverseAdjList().get(n).values());
			
			for (LightEdge e : neighbours)
				if(!edgeCellMap.containsKey(e) || 
						!(e.from.isTopOrNothing() || e.to.isTopOrNothing()))
					insertEdge(e);
		}
		
		setCellsDeletable(false);
		adjustVertices();
	}

	public void buildGraph(){
		for (OWLClass cls : v.asList()) {
			LightNode n = adj.getNodeFromClass(cls);
			insertVertex(n);
			
			List<LightEdge> neighbours = new ArrayList<>(adj.getAdjacent(n));
			neighbours.addAll(adj.getReverseAdjList().get(n).values());
			
			for (LightEdge e : neighbours)
				if(!(e.from.isTopOrNothing() || e.to.isTopOrNothing()))
					insertEdge(e);
		}
		
		setCellsDeletable(false);
		adjustVertices();
	}

	private void adjustVertices(){
		for (mxCell cell : cellNodeMap.keySet())
			updateCellSize(cell);
	}

	public mxCell getGroupCell(int index){
		if(index >= 0 && index < groups.length)
			return groups[index];
		else
			return null;
	}
	
	public void markAsDeleted(LightEdge m, String style, boolean refresh){
		mxCell e = edgeCellMap.get(m);
		
		if(e == null)
			return;
		
		e.setStyle(style);
		
		if(refresh)
			vc.refreshGraph();
	}
	
	public void markAsDeleted(Collection<Pair<OWLClass>> ms, String style){
		for (Pair<OWLClass> m : ms)
			markAsDeleted(adj.getEdgeBetweenNodes(
					adj.getNodeFromClass(m.getFirst()), 
					adj.getNodeFromClass(m.getSecond())),style,false);
		vc.refreshGraph();
	}
	
//	public void markAsDeleted(Collection<LightEdge> ms, String style){
//		for (LightEdge m : ms)
//			markAsDeleted(m,style,false);
//		vc.refreshGraphAndMappingList();
//	}

	public mxCell insertVertex(LightNode n){
		mxCell node = (mxCell) insertVertex(getDefaultParent(), 
				Integer.toString(vertexID++), n.toString(), 0, 0, 80, 30, 
					n.getFirstOnto() ? vertexStyle1 : vertexStyle2);
		
		cellNodeMap.put(node, n);
		nodeCellMap.put(n, node);
		
		return node;
	}

	public mxCell insertEdge(LightEdge e){

		if(!nodeCellMap.containsKey(e.from))
			insertVertex(e.from);
		if(!nodeCellMap.containsKey(e.to))
			insertVertex(e.to);
		
		mxICell source = nodeCellMap.get(e.from);
		mxICell target = nodeCellMap.get(e.to);
		
		mxCell edge = (mxCell) insertEdge(getDefaultParent(), null, 
					e.confidence, source, target, e.mapping ? 
							mappingStyle : axiomStyle);
		
		if(Params.testMode)
			System.out.println("Edge created " + cellToString(edge));
		
		cellEdgeMap.put(edge, e);
		edgeCellMap.put(e, edge);

		return edge;
	}

	public static String mxCellDetailFormatter(mxCell cell){
		if(cell == null)
			return "";

		if(cell.isVertex())
			return cell.getId() + ": " + cell.getValue();
		return cell.getId() + ": " + cell.getSource().getValue() + " -> " 
		+ cell.getTarget().getValue();
	}

	private String cellToString(mxCell cell){
		if(cell == null)
			return "";

		if(cell.isVertex())
			return cell.getId() + ": " + cell.getValue();
		return cell.getId() + ": " + cell.getSource().getValue() + " -> " 
		+ cell.getTarget().getValue();
	}
	
	public boolean isDeleted(mxCell c) {
		return vc.isDeleted(new Pair<OWLClass>(
				cellEdgeMap.get(c).from.getOWLClass(), 
				cellEdgeMap.get(c).to.getOWLClass()));
	}
	
	public boolean isAxiom(mxCell c){
		return cellEdgeMap.containsKey(c) && !cellEdgeMap.get(c).mapping; 
	}

	public LightEdge getEdgeFromCell(mxCell c) {
		return cellEdgeMap.containsKey(c) ? cellEdgeMap.get(c) : null; 
	}
	
	public mxCell getCellFromEdge(LightEdge e) {
		return edgeCellMap.containsKey(e) ? edgeCellMap.get(e) : null; 
	}
}
