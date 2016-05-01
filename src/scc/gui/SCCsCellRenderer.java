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

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

public class SCCsCellRenderer extends JLabel implements ListCellRenderer<Object> {

	private VisualDebugger vd;	
	private static final long serialVersionUID = 1L;
	public SCCsCellRenderer(VisualDebugger vd) {
		setOpaque(true);
		this.vd = vd;
	}
	@Override
	public Component getListCellRendererComponent(
			JList<?> list, Object value, int index,
			boolean isSelected, boolean cellHasFocus){

		setText(index + " - " + value.toString());

		Color background;
		Color foreground;

		// check if this cell represents the current DnD drop location
		if (isSelected) {
			background = Color.GRAY;
			foreground = Color.WHITE;

			// unselected, and not the DnD drop location
		} else {
			if(!((LightSCC) value).isProblematic(vd.getAdjancencyList()))
				background = Color.green;
			else if(vd.isModified((LightSCC) value))
				background = Color.ORANGE;
			else 
				background = Color.WHITE;

			foreground = Color.BLACK;
		};

		setBackground(background);
		setForeground(foreground);

		return this;
	}
}
