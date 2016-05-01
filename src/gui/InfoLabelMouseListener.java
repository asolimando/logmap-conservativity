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

import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

public class InfoLabelMouseListener implements MouseListener {

	private JLabel infoLabel;
	private VisualConservativity vc;
	
	public InfoLabelMouseListener(JLabel infoLabel, VisualConservativity vc){
		this.infoLabel = infoLabel;
		this.vc = vc;
	}
	
	@Override
	public void mouseClicked(MouseEvent arg0) {
		if(arg0.getSource() == infoLabel){
			
			if(vc.getSelectedViolation() == null)
				return;
		    
		    JTextArea textArea = new JTextArea("");					
			textArea.setLineWrap(true);
		    textArea.setEditable(false);
		    textArea.setVisible(true);
		    
			JScrollPane sp = new JScrollPane(textArea);
			
			sp.setPreferredSize(new Dimension(800,600));
			sp.setHorizontalScrollBarPolicy(
					ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			sp.setVerticalScrollBarPolicy(
					ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
			
//			Window window = SwingUtilities.getWindowAncestor(textArea);
//			if (window instanceof Dialog) {
//				Dialog dialog = (Dialog) window;
//				if (!dialog.isResizable())
//					dialog.setResizable(true);
//			}
			
			JOptionPane.showMessageDialog(vc, sp, "Information",
				    JOptionPane.INFORMATION_MESSAGE);
		}
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
	}

}
