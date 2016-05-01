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

import scc.graphDataStructure.Diagnosis;

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
	private VisualDebugger vd;
	
	public InfoLabelMouseListener(JLabel infoLabel, VisualDebugger vd){
		this.infoLabel = infoLabel;
		this.vd = vd;
	}
	
	@Override
	public void mouseClicked(MouseEvent arg0) {
		if(arg0.getSource() == infoLabel){
			
			if(vd.getSelectedSCC() == null)
				return;
			
		    Diagnosis manual = vd.localDeletions.get(vd.getSelectedSCC()), 
		    		diag = vd.localDiagnoses.get(vd.getSelectedSCC()),
		    		filter = vd.localFilters.get(vd.getSelectedSCC());
		    
		    if(manual == null)
		    	manual = new Diagnosis();
		    if(diag == null)
		    	diag = new Diagnosis();
		    if(filter == null)
		    	filter = new Diagnosis();
		    
		    JTextArea textArea = new JTextArea(

					vd.getSelectedSCC().problematicSCCAsString(
					vd.getAdjancencyList()) +  
		    		"\n\nManual Deletion (w = " + manual.getWeight() + "):\n" 
		    				+ manual 
		    		+ "\nComputed Diagnosis (w = " + diag.getWeight() + "):\n" 
		    				+ diag 
		    		+ "\nFiltered (w = " + filter.getWeight() + "):\n" 
		    				+ filter + "\n\n\nThe SCC is" 
		    				+ (vd.getSelectedSCC().isProblematic(vd.getAdjancencyList()) 
		    						? "" : " NOT") 
		    				+ " problematic with the actual diagnosis!");
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
			
			JOptionPane.showMessageDialog(vd, sp, "Information",
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
