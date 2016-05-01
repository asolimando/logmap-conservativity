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

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;

import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.handler.mxKeyboardHandler;
import com.mxgraph.swing.util.mxGraphActions;

public class myMappingKeyboardHandler extends mxKeyboardHandler {
	
	public myMappingKeyboardHandler(mxGraphComponent graphComponent) {
		super(graphComponent);
	}
	
	protected ActionMap createActionMap(){
		ActionMap map = super.createActionMap();
		map.put("delete", new AbstractAction() {
			
			private static final long serialVersionUID = -5263639405885263507L;

			@Override
			public void actionPerformed(ActionEvent e) {
				mySCCGraph graph = (mySCCGraph) mxGraphActions.getGraph(e);
				
				if (graph != null){
					if(((mxCell)graph.getSelectionCell()).isEdge() 
							&& graph.getSelectionCount() == 1){
						mxCell c = (mxCell)graph.getSelectionCell();
						if(!graph.isManuallyRemovable(c))
							return;
							
						if(!graph.isManuallyDeleted(c))
							graph.manualMappingDeletion(c,true);
						else
							graph.retractMappingDeletion(c,true);
					}
				}
			}
		});
		return map;
	}
}
