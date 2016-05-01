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

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JCheckBoxList extends JList<JCheckBoxLightEdge>
{
	private static final long serialVersionUID = 5146051145811651618L;
	protected static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);
	private VisualDebugger vd;
	
	public JCheckBoxList(final VisualDebugger vd){
		setCellRenderer(new CellRenderer(vd));
		this.vd = vd;
		
		addMouseListener(new MouseAdapter()
		{
			public void mousePressed(MouseEvent e)
			{
				if(vd.isComputing())
					return;

				int index = locationToIndex(e.getPoint());

				if (index != -1) {
					if(vd.isModified(vd.getSelectedSCC())){
						JOptionPane.showMessageDialog(vd,
								"Sealing mappings is not possible when there " +
								"are already removed mappings.",
							    "Sealing not possible!",
							    JOptionPane.ERROR_MESSAGE);
						return;
					}

					JCheckBoxLightEdge checkbox = 
							getModel().getElementAt(index);
					checkbox.setSelected(!checkbox.isSelected());
					if(checkbox.isSelected())
						vd.sealMapping(vd.getSelectedSCC(),
								checkbox.getElement());
					else
						vd.unsealMapping(vd.getSelectedSCC(),
								checkbox.getElement());
					repaint();
				}
			}
		}
				);

		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	}

	public Set<LightEdge> getCheckedElems(){
		Set<LightEdge> ret = new HashSet<>();
		JCheckBoxLightEdge cb;
		for (int i = 0; i < getModel().getSize(); i++){
			cb = getModel().getElementAt(i);
			if(cb.isSelected())
				ret.add(cb.getElement());
		}
		return ret;
	}

	public void addElement(LightEdge m) {
		ListModel currentList = this.getModel();
		JCheckBoxLightEdge elem = new JCheckBoxLightEdge(m);
		
		if(vd.isMappingSealed(vd.getSelectedSCC(), m))
			elem.setSelected(true);
		
		JCheckBoxLightEdge[] newList = 
				new JCheckBoxLightEdge[currentList.getSize() + 1];
		for (int i = 0; i < currentList.getSize(); i++) {
			newList[i] = (JCheckBoxLightEdge) currentList.getElementAt(i);
		}
		newList[newList.length - 1] = elem;
		setListData(newList);
	}

	public void removeAllElements() {
		setListData(new JCheckBoxLightEdge[0]);
	}

	protected class CellRenderer extends JCheckBox implements 
													ListCellRenderer<Object>{
		private static final long serialVersionUID = 7861380354090460679L;
		private VisualDebugger vd;

		public CellRenderer(VisualDebugger vd){
			setOpaque(true);
			this.vd = vd;
		}

		public Component getListCellRendererComponent(
				JList<?> list, Object value, int index,
				boolean isSelected, boolean cellHasFocus){

			Color background = Color.WHITE, foreground;

			if(vd.isFiltered(vd.getSelectedSCC(), ((JCheckBoxLightEdge) value).getElement() ))
				foreground = Color.MAGENTA;
			else if(vd.isRemoved(vd.getSelectedSCC(), ((JCheckBoxLightEdge) value).getElement() ))
				foreground = Color.RED;
			else if(vd.isManuallyRemoved(vd.getSelectedSCC(), 
					((JCheckBoxLightEdge) value).getElement() ))
				foreground = Color.orange;
			else
				foreground = Color.BLACK;

			setBackground(background);
			setForeground(foreground);

			setEnabled(isEnabled());
			setText(value.toString());
			setFocusPainted(false);
			setBorderPainted(true);
			setSelected(((JCheckBoxLightEdge) value).isSelected());
			setIcon(isSelected() ? JCheckBoxLightEdge.locked 
					: JCheckBoxLightEdge.unlocked);

			//			setBorder(isSelected ?
					//	          UIManager.getBorder(
			//	           "List.focusCellHighlightBorder") : noFocusBorder);

			return this;
		}
	}

	public void setSealStatus(LightEdge m, boolean sealed) {
		JCheckBoxLightEdge cb;
		for (int i = 0; i < getModel().getSize(); i++){
			cb = getModel().getElementAt(i);
			if(cb.getElement().equals(m)){
				cb.setSelected(sealed);
				break;
			}
		}
	}
}