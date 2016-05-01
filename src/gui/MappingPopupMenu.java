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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import com.mxgraph.model.mxCell;

public class MappingPopupMenu extends JPopupMenu implements ActionListener {

	private static final long serialVersionUID = -3132749140550242191L;
	private JMenuItem 
//	sealItem = new JMenuItem("Seal mapping"),
//	unsealItem = new JMenuItem("Unseal mapping"),
	delItem = new JMenuItem("Delete mapping"),
	undelItem = new JMenuItem("Restore mapping");
	private VisualConservativity vc;

	public MappingPopupMenu(VisualConservativity vc) {
		this.vc = vc;

//		add(sealItem);
//		add(unsealItem);
//		addSeparator();
		add(delItem);
		add(undelItem);
		pack();

//		sealItem.addActionListener(this);
//		unsealItem.addActionListener(this);
		delItem.addActionListener(this);
		undelItem.addActionListener(this);
	}

	@Override
	public void show(Component invoker, int x, int y){

		if(vc.graphComponent.getGraph().isSelectionEmpty())
			return;

		mxCell cell = (mxCell) vc.graphComponent.getGraph().getSelectionCell();
		if(!cell.isEdge() || vc.graph.isAxiom(cell))
			return;

		disableAllItems();
		boolean del = vc.graph.isDeleted(cell);
//		boolean seal = vc.graph.isSealed(cell);

		super.show(invoker,x,y);
	}

	public void hide(){
		this.setVisible(false);
	}

	private void disableAllItems(){
//		sealItem.setEnabled(false);
//		unsealItem.setEnabled(false);
		delItem.setEnabled(false);
		undelItem.setEnabled(false);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JMenuItem s = (JMenuItem) e.getSource();
		mxCell cell = 
				(mxCell) vc.graphComponent.getGraph().getSelectionCell();

		if(!cell.isEdge() || vc.graph.isAxiom(cell))
			return;

//		if(s.equals(delItem)){
//			vc.graph.manualMappingDeletion(cell, true);
//		}
//		else if(s.equals(undelItem)){
//			vc.graph.retractMappingDeletion(cell, true);
//		}
//		else if(s.equals(sealItem)){
//			vc.graph.sealMapping(cell);
//		}
//		else if(s.equals(unsealItem)){
//			vc.graph.unsealMapping(cell);
//		}

		hide();
	}
}