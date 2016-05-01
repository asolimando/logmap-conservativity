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

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import enumerations.REASONER_KIND;

import util.Params;

class PreferencesDialog extends JDialog implements ActionListener {

	private static final long serialVersionUID = -4348383057307973879L;

	private VisualDebugger vd;

	private JLabel verbosityLabel = new JLabel("Verbosity: "), 
			aspTimeoutLabel = new JLabel("ASP Timeout (sec): ");

	private JTextField verbosityField = new JTextField(), 
			aspTimeoutField = new JTextField();

	private JCheckBox debugMode = new JCheckBox("Debug Mode"),
			useHermit = new JCheckBox("Hermit");

	private JButton applyButton = new JButton("Apply"), 
			cancelButton = new JButton("Cancel");

	public PreferencesDialog(VisualDebugger vd){
		super(vd, "Preferences", true);

		this.vd = vd;

		// this.setPreferredSize(new Dimension(200,100));
		JRootPane rootPane = this.getRootPane();
		// applybutton receive Enter by default
		rootPane.setDefaultButton(applyButton);

		/* add listener */
		applyButton.addActionListener(this);
		cancelButton.addActionListener(this);
		/* end add listener */

		JPanel sizePanel = new JPanel(new GridLayout(3,2));
		sizePanel.setBorder(BorderFactory.createTitledBorder(" Parameters "));

		sizePanel.add(useHermit);
		sizePanel.add(debugMode);
		sizePanel.add(verbosityLabel);
		sizePanel.add(verbosityField);
		sizePanel.add(aspTimeoutLabel);
		sizePanel.add(aspTimeoutField);

		//verbosityField.setPreferredSize(new Dimension(20,20));

		JPanel buttonPanel = new JPanel(new FlowLayout());
		buttonPanel.add(applyButton);
		buttonPanel.add(cancelButton);

		Container mainPanel = getContentPane();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.add(sizePanel);
		mainPanel.add(buttonPanel);
		
		readValuesFromConfiguration();

		this.setResizable(false);
		this.pack();
		setLocationRelativeTo(vd);

		setVisible(false);
	}

	private void readValuesFromConfiguration(){
		debugMode.setSelected(Params.testMode);
		useHermit.setSelected(Params.reasonerKind.equals(REASONER_KIND.HERMIT));
		verbosityField.setText(Params.verbosity + "");
		aspTimeoutField.setText(Params.ASPTimeout + "");
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		JButton button = (JButton) e.getSource();

		if(button == applyButton){
			Params.testMode = debugMode.isSelected();
			try {
				Params.verbosity = Integer.parseInt(verbosityField.getText());
			} catch(NumberFormatException e1){
				JOptionPane.showMessageDialog(this,
						"Malformed Verbosity Level, " +
								"using default value \"0\".",
								"Malformed value",
								JOptionPane.ERROR_MESSAGE);
				Params.verbosity = 0;
			}
			
			Params.reasonerKind = useHermit.isSelected() ? 
					REASONER_KIND.HERMIT : REASONER_KIND.PELLET;
			
			try {
				Params.ASPTimeout = Integer.parseInt(aspTimeoutField.getText());
			} catch(NumberFormatException e1){
				JOptionPane.showMessageDialog(this,
						"Malformed ASP Timeout, " +
								"using default value \"60\" seconds.",
								"Malformed value",
								JOptionPane.ERROR_MESSAGE);
				Params.ASPTimeout = 60;
			}
		}
		else if(button == cancelButton)
			readValuesFromConfiguration();
		
		setVisible(false);
	}
}