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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import com.mxgraph.model.mxCell;

public class MappingPopupMenu extends JPopupMenu implements ActionListener {

	private static final long serialVersionUID = -3132749140550242191L;
	private JMenuItem 
	sealItem = new JMenuItem("Seal mapping"),
	unsealItem = new JMenuItem("Unseal mapping"),
	delItem = new JMenuItem("Delete mapping"),
	undelItem = new JMenuItem("Restore mapping");
	private VisualDebugger vd;

	public MappingPopupMenu(VisualDebugger vd) {
		this.vd = vd;

		add(sealItem);
		add(unsealItem);
		addSeparator();
		add(delItem);
		add(undelItem);
		pack();

		sealItem.addActionListener(this);
		unsealItem.addActionListener(this);
		delItem.addActionListener(this);
		undelItem.addActionListener(this);
	}

	@Override
	public void show(Component invoker, int x, int y){

		if(vd.graphComponent.getGraph().isSelectionEmpty())
			return;

		mxCell cell = (mxCell) vd.graphComponent.getGraph().getSelectionCell();
		if(!cell.isEdge() || vd.graph.isAxiom(cell))
			return;

		disableAllItems();
		boolean del = vd.graph.isDeleted(cell);
		boolean seal = vd.graph.isSealed(cell);

		if(!(vd.hasDiagnosis(vd.getSelectedSCC()) || 
				vd.hasFiltering(vd.getSelectedSCC()))){
			delItem.setEnabled(!del && !seal);
			undelItem.setEnabled(del);
			if(!del){
				unsealItem.setEnabled(seal);
				sealItem.setEnabled(!seal);
			}
		}
		super.show(invoker,x,y);
	}

	public void hide(){
		this.setVisible(false);
	}

	private void disableAllItems(){
		sealItem.setEnabled(false);
		unsealItem.setEnabled(false);
		delItem.setEnabled(false);
		undelItem.setEnabled(false);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JMenuItem s = (JMenuItem) e.getSource();
		mxCell cell = 
				(mxCell) vd.graphComponent.getGraph().getSelectionCell();

		if(!cell.isEdge() || vd.graph.isAxiom(cell))
			return;

		if(s.equals(delItem)){
			vd.graph.manualMappingDeletion(cell, true);
		}
		else if(s.equals(undelItem)){
			vd.graph.retractMappingDeletion(cell, true);
		}
		else if(s.equals(sealItem)){
			vd.graph.sealMapping(cell);
		}
		else if(s.equals(unsealItem)){
			vd.graph.unsealMapping(cell);
		}

		hide();
	}
}