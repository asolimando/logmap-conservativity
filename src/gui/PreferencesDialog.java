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

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import enumerations.REASONER_KIND;
import enumerations.VIOL_KIND;

import util.Params;

class PreferencesDialog extends JDialog implements ActionListener {

	private static final long serialVersionUID = -4348383057307973879L;

	private VisualConservativity vc;

	private JLabel verbosityLabel = new JLabel("Verbosity: "), 
			violKindLabel = new JLabel("Violations kind: "),
			reasonerLabel = new JLabel("Reasoner: ");

	private JTextField verbosityField = new JTextField();
	
	@SuppressWarnings("unchecked")
	private JComboBox<VIOL_KIND> violKindList;
	private JComboBox<REASONER_KIND> reasonerList;
	
	private JCheckBox debugMode = new JCheckBox("Debug Mode");
	private JCheckBox preCompDirViols = new JCheckBox("Precompute Direct Violations");
	
	private JButton applyButton = new JButton("Apply"), 
			cancelButton = new JButton("Cancel");

	public PreferencesDialog(VisualConservativity vc){
		super(vc, "Preferences", true);

		this.vc = vc;

		VIOL_KIND [] violkinds = {VIOL_KIND.APPROX, VIOL_KIND.FULL};
		REASONER_KIND [] reaskinds = {REASONER_KIND.HERMIT, REASONER_KIND.PELLET, 
				REASONER_KIND.ELK, REASONER_KIND.STRUCTURAL};
		reasonerList = new JComboBox(reaskinds);
		violKindList = new JComboBox(violkinds);
		
		// this.setPreferredSize(new Dimension(200,100));
		JRootPane rootPane = this.getRootPane();
		// applybutton receive Enter by default
		rootPane.setDefaultButton(applyButton);

		/* add listener */
		applyButton.addActionListener(this);
		cancelButton.addActionListener(this);
		/* end add listener */

		JPanel sizePanel = new JPanel(new GridLayout(4,2));
		sizePanel.setBorder(BorderFactory.createTitledBorder(" Parameters "));

		sizePanel.add(reasonerLabel);
		sizePanel.add(reasonerList);
		sizePanel.add(debugMode);
		sizePanel.add(preCompDirViols);
		sizePanel.add(verbosityLabel);
		sizePanel.add(verbosityField);
		sizePanel.add(violKindLabel);
		sizePanel.add(violKindList);

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
		setLocationRelativeTo(vc);

		setVisible(false);
	}

	private void readValuesFromConfiguration(){
		debugMode.setSelected(Params.testMode);
		verbosityField.setText(Params.verbosity + "");
		reasonerList.setSelectedItem(Params.reasonerKind);
		violKindList.setSelectedItem(Params.violKindToShow);
		preCompDirViols.setSelected(Params.preComputeDirectViols);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		JButton button = (JButton) e.getSource();

		if(button == applyButton){
			Params.testMode = debugMode.isSelected();
			Params.preComputeDirectViols = preCompDirViols.isSelected();
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
			
			Params.reasonerKind = (REASONER_KIND) reasonerList.getSelectedItem();
			Params.violKindToShow = (VIOL_KIND) violKindList.getSelectedItem();		
		}
		else if(button == cancelButton)
			readValuesFromConfiguration();
		
		setVisible(false);
	}
}