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
package scc.graphViz;

import scc.graphDataStructure.LightAdjacencyList;
import scc.graphDataStructure.LightEdge;
import scc.graphDataStructure.LightNode;
import scc.graphDataStructure.LightSCC;
import scc.gui.VisualDebugger;

import java.awt.Color;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import util.Params;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxICell;
import com.mxgraph.util.mxUtils;
import com.mxgraph.view.mxGraph;

public class mySCCGraph extends mxGraph {

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
	private LightSCC scc;
	private VisualDebugger vd;

	public mySCCGraph(LightAdjacencyList adj, LightSCC scc, VisualDebugger vd){
		this.adj = adj;
		this.scc = scc;
		this.vd = vd;

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

	public boolean isMapping(mxCell c){
		return cellEdgeMap.containsKey(c) && cellEdgeMap.get(c).mapping;
	}
	
	@Override
	public void cellLabelChanged(Object cell, Object value, boolean autoSize) {
		mxCell cl = (mxCell) cell;
		// vertex label can not be changed, while edge labels can
		if(cl.isVertex())
			return;
		
		if(vd.isModified(vd.getSelectedSCC())){
			JOptionPane.showMessageDialog(vd,
					"Impossible to change confidence values on a modified " +
					"SCC, reset it first.",
				    "Change not possible!",
				    JOptionPane.ERROR_MESSAGE);
			return;
		}
		
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
			JOptionPane.showMessageDialog(vd,
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
			vd.refreshGraphAndMappingList();
		}
	}

	public void buildGraphFromSCC(Set<LightEdge> visibleMapping){
		for (LightNode n : scc) {
			if(visibleMapping != null){
				boolean found = false;
				for (LightEdge e : visibleMapping) {
					if(e.from.equals(n) || e.to.equals(n)){
						found = true;
						break;
					}
				}
				if(!found)
					continue;
			}
			mxCell v = insertVertex(n.toString(), 
					n.getFirstOnto() ? vertexStyle1 : vertexStyle2);
			cellNodeMap.put(v, n);
			nodeCellMap.put(n, v);
		}

		for (LightEdge e : scc.extractOriginalEdges(adj)) {
			if(visibleMapping != null){
				if(!visibleMapping.contains(e))
					continue;
			}
			mxCell arc = insertEdge(nodeCellMap.get(e.from), 
					nodeCellMap.get(e.to), Double.toString(e.confidence), 
					e.mapping);
			cellEdgeMap.put(arc, e);
			edgeCellMap.put(e, arc);
		}

		for (LightEdge e : scc.extractMappings(adj, false)) {
			if(visibleMapping != null){
				if(!visibleMapping.contains(e))
					continue;
			}
			mxCell arc = insertEdge(nodeCellMap.get(e.from), 
					nodeCellMap.get(e.to), Double.toString(e.confidence), 
					e.mapping);
			cellEdgeMap.put(arc, e);
			edgeCellMap.put(e, arc);
		}
		
//		if(visibleMapping != null && !visibleMapping.isEmpty()){			
//			for (LightEdge k : edgeCellMap.keySet())
//				if(!visibleMapping.contains(k))
//					removeCells(new Object[]{edgeCellMap.get(k)});
//		}

		setCellsDeletable(false);
		//computeGroups();
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
	
	public void manualMappingDeletions(Collection<mxCell> cells){
		for (mxCell mxCell : cells)
			manualMappingDeletion(mxCell,false);
		vd.refreshGraphAndMappingList();
	}
	
	public void manualMappingDeletion(mxCell cell, boolean refresh){
		LightEdge m = cellEdgeMap.get(cell);
		
		if(vd.isSealed(vd.getSelectedSCC(), m))
			throw new Error("Cannot delete a sealed mapping");
		
		vd.manualMappingDeletion(m);
		markAsDeleted(m, manualDeletedStyle, refresh);
	}

	public void markAsDeleted(LightEdge m, String style, boolean refresh){
		mxCell e = edgeCellMap.get(m);
		e.setStyle(style);
		//this.removeCells(new Object[]{e});
		if(refresh)
			vd.refreshGraphAndMappingList();
	}
	
	public void markAsDeleted(Collection<LightEdge> ms, String style){
		for (LightEdge m : ms)
			markAsDeleted(m,style,false);
		vd.refreshGraphAndMappingList();
	}

	private void computeGroups(){

		//		if(reset){
		//			ungroupCells(new Object[] {groups[0], groups[1]});
		//			groups[0] = groups[1] = null;
		//		}

		List<Object> fstVtx = new LinkedList<>(),
				sndVtx = new LinkedList<>();

				for (LightNode n : nodeCellMap.keySet()) {
					if(n.getFirstOnto())
						fstVtx.add(nodeCellMap.get(n));
					else
						sndVtx.add(nodeCellMap.get(n));
				}

				groups[0] = (mxCell) groupCells(null, 100, fstVtx.toArray());
				groups[1] = (mxCell) groupCells(null, 100, sndVtx.toArray());

				//group.setCollapsed(true);
				//group.setConnectable(true);
	}

	public mxCell insertVertex(String desc, String style){
		mxCell node = (mxCell) insertVertex(getDefaultParent(), 
				Integer.toString(vertexID++), desc, 0, 0, 80, 30, style);
		return node;
	}

	public mxCell insertEdge(mxICell source, mxICell target, String edgeLabel, 
			boolean isMapping){

		//		if(source.equals(target))
		//			throw new Error("Self-loops not allowed: edge " + cellToString((mxCell)source));

		mxCell edge = null;		

		if(isMapping)
			edge = (mxCell) insertEdge(getDefaultParent(), null, 
					edgeLabel, source, target, mappingStyle);
		else
			edge = (mxCell) insertEdge(getDefaultParent(), null, 
					edgeLabel, source, target, axiomStyle);

		if(Params.testMode)
			System.out.println("Edge created " + cellToString(edge));

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

	public boolean isManuallyRemovable(mxCell c){
		if(!cellEdgeMap.containsKey(c))
			return false;
		
		LightEdge e = cellEdgeMap.get(c);
		return !(!e.mapping 
				|| vd.isSealed(vd.getSelectedSCC(), e) 
				|| vd.isRemoved(vd.getSelectedSCC(), e) 
				|| vd.isFiltered(vd.getSelectedSCC(), e)
		);
	}
	
	public boolean isManuallyDeleted(mxCell c){
		return vd.isManuallyRemoved(vd.getSelectedSCC(), cellEdgeMap.get(c));
	}
	
	public boolean isDeleted(mxCell c) {
		return vd.isDeleted(vd.getSelectedSCC(), cellEdgeMap.get(c));
	}
	
	public boolean isAxiom(mxCell c){
		return cellEdgeMap.containsKey(c) && !cellEdgeMap.get(c).mapping; 
	}

	public void retractMappingDeletion(mxCell c, boolean refresh) {
		LightEdge e = cellEdgeMap.get(c);
		vd.retractManualMappingDeletion(e);
		markAsDeleted(Collections.singleton(e),mappingStyle);
		if(refresh)
			vd.refreshGraphAndMappingList();
	}
	
	public void retractMappingDeletions(Collection<mxCell> cs) {
		for (mxCell c : cs)
			retractMappingDeletion(c, false);
		vd.refreshGraphAndMappingList();
	}
	
	public void sealMapping(mxCell c){
		vd.sealMapping(vd.getSelectedSCC(), cellEdgeMap.get(c));
	}
	
	public void unsealMapping(mxCell c){
		vd.unsealMapping(vd.getSelectedSCC(), cellEdgeMap.get(c));
	}

	public boolean isSealed(mxCell c) {
		return vd.isSealed(vd.getSelectedSCC(), cellEdgeMap.get(c));
	}

	public LightEdge getEdgeFromCell(mxCell c) {
		return cellEdgeMap.containsKey(c) ? cellEdgeMap.get(c) : null; 
	}
	
	public mxCell getCellFromEdge(LightEdge e) {
		return edgeCellMap.containsKey(e) ? edgeCellMap.get(e) : null; 
	}

	//	public void buildEdge(String left, String right, boolean reverse){
	//		Node leftNode = null, rightNode;
	//		
	//		String leftLabel = reverse ? right : left,
	//				rightLabel = reverse ? left : right;
	//		
	//		Map<OWLClass, Node> leftNodes = reverse ? classNodeSecond : classNodeFirst,
	//				rightNodes = reverse ? classNodeFirst : classNodeSecond;
	//
	//		for (Node node : leftNodes.values()) {
	//			if(node.getDesc().compareTo(leftLabel) == 0){
	//				leftNode = node;
	//				break;
	//			}
	//		}
	//
	//		if(leftNode == null)
	//			return;
	//
	//		for (Node node : rightNodes.values()) {
	//			if(node.getDesc().compareTo(rightLabel) == 0){
	//				rightNode = node;
	//				mxCell edge = insertEdge(leftNode.getVertex(), rightNode.getVertex());
	////				mxCell edge = (mxCell) insertEdge(getDefaultParent(), null, edgeLabel, 
	////						leftNode.getVertex(), rightNode.getVertex(), mappingStyle);
	//				
	////				insertEdge(getDefaultParent(), null, edgeLabel, rightNode.getVertex(), leftNode.getVertex());
	//				Edge graphEdge = new Edge(leftNode, rightNode, edge);
	//				edgeMap.put(edge, graphEdge);
	//				adjList.get(leftNode).add(graphEdge);
	////				adjList.get(rightNode).add(new Edge(rightNode, rightNode));
	//			}
	//		}
	//	}
}
