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

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import org.semanticweb.owlapi.model.OWLClass;

import auxStructures.Pair;

public class JChkBoxList extends JList<JCheckBox>
{
	private static final long serialVersionUID = 5146051145811651618L;
	protected static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);
	private VisualConservativity vc;
	
	public JChkBoxList(final VisualConservativity vc){
		setCellRenderer(new CellRenderer(vc));
		this.vc = vc;
		setFixedCellHeight(15);

		addMouseListener(new MouseAdapter()
		{
			public void mousePressed(MouseEvent e)
			{
				if(vc.isComputing())
					return;

				int index = locationToIndex(e.getPoint());

				if (index != -1) {
					JCheckBox checkbox = 
							getModel().getElementAt(index);
					Pair<OWLClass> v = vc.getViolation(index);
					checkbox.setSelected(!checkbox.isSelected());
					if(checkbox.isSelected())
						vc.addViolDisj(v);
					else
						vc.removeViolDisj(v);
					repaint();
				}
			}
		}
				);

		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	}

	public void addElement(Pair<OWLClass> m) {
		ListModel<JCheckBox> currentList = this.getModel();
		JCheckBox elem = new JCheckBox();
		
		if(vc.isViolDisj(m))
			elem.setSelected(true);
		
		JCheckBox[] newList = new JCheckBox[currentList.getSize() + 1];
		
		for (int i = 0; i < currentList.getSize(); i++)
			newList[i] = currentList.getElementAt(i);
		
		newList[newList.length - 1] = elem;
		setListData(newList);
	}

	public void removeAllElements() {
		setListData(new JCheckBox[0]);
	}
	
	public void uncheckAllViolations() {
		setAllViolations(false);
	}
	
	public void checkAllViolations() {
		setAllViolations(true);
	}
	
	private void setAllViolations(boolean b) {
		ListModel<JCheckBox> currentList = this.getModel();
		for (int i = 0; i < currentList.getSize(); i++) {
			currentList.getElementAt(i).setSelected(b);
		}
		System.out.println("SET " + b);
	}
	
	protected List<Pair<OWLClass>> getCheckedElements(){
		List<Pair<OWLClass>> res = new ArrayList<>();
		
		ListModel<JCheckBox> currentList = this.getModel();
		
		for (int i = 0; i < currentList.getSize(); i++)
			if(currentList.getElementAt(i).isSelected())
				res.add(vc.getViolation(i));
		
		return res;
	}

	protected class CellRenderer extends JCheckBox implements 
													ListCellRenderer<Object>{
		private static final long serialVersionUID = 7861380354090460679L;
		private VisualConservativity vc;

		public CellRenderer(VisualConservativity vc){
			setOpaque(true);
			this.vc = vc;
		}

		public Component getListCellRendererComponent(
				JList<?> list, Object value, int index,
				boolean isSelected, boolean cellHasFocus){

			Color background = Color.WHITE;
			Color foreground = Color.BLACK;
//			Pair<OWLClass> v = (Pair<OWLClass>) vc.getViolsList().getModel().getElementAt(index);
//			
//			// check if this cell represents the current DnD drop location
//			if (vc.getViolsList().getSelectedIndex() == index) {
//				background = Color.GRAY;
//				foreground = Color.WHITE;
//			}
//			else {
//				if(v != null){
//					if(vc.getRepairFacility().isDirectViolation(v))
//						background = Color.ORANGE;
//					else if(vc.isViolationSolved(v))
//						background = Color.green;
//				}
//			}
			
			setBackground(background);
			setForeground(foreground);
			setFont(new JList().getFont());
			setEnabled(isEnabled());
			setFocusPainted(false);
			setBorderPainted(true);
			setSelected(((JCheckBox) value).isSelected());
			return this;
		}
	}
}