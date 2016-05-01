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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import util.OntoUtil;

public class VisualStats extends JFrame implements WindowListener, 
ActionListener {

	private static final long serialVersionUID = -3748246500598367398L;

	/** The jbutton analyze. */
	private JButton jButtonLoadSrcOnto = new JButton("Load Ontology");

	private JButton [] buttons = {jButtonLoadSrcOnto}; 

	private JProgressBar progBar = new JProgressBar(SwingConstants.HORIZONTAL);

	/** Label showing the name of the schema file selected. */
	String baseSelectedLabel = " - (Source Ontology ; Target Ontology ; " +
			"Mapping) -> ",
			defaultStatusLbl = "Select an SCC";
	private JLabel jLabelFilesSelected = new JLabel(baseSelectedLabel),
			statusLabel = new JLabel("Initialization completed");

	private File srcOnto;
	private OWLOntology onto = null;

	private OpenFileFilter ontoFilter = new OpenFileFilter(new String[]{"owl"});	

	private JFileChooser fileChooser = 
			new JFileChooser(System.getProperty("user.dir"));

	/** The border layout1. */
	private BorderLayout borderLayout1 = new BorderLayout();

	/** Split pane for the main components (textarea for textual representation 
	 * and graphical representation of the graph). */
	private JSplitPane splitPaneForTextAreas = 
			new JSplitPane(JSplitPane.VERTICAL_SPLIT);

	/** Scroll area for text area representing the textual representation 
	 * of the graph. */
	private JScrollPane dummyScrollPane = new JScrollPane();

	/** Top toolbar with the main buttons. */
	private JToolBar topToolbar = new JToolBar(JToolBar.HORIZONTAL);

	/**
	 * The main method.
	 *
	 * @param args the arguments of the main method
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// allows comparators to keep working in java 7
		System.getProperties().setProperty("java.util.Arrays.useLegacyMergeSort", "true");

		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new VisualStats().init();
			}
		});
	}

	private void setSourceOntologyFile(File src){
		srcOnto = src;
		updateFilesLabel();
	}

	private void updateFilesLabel(){
		jLabelFilesSelected.setText(baseSelectedLabel + " ( " 
				+ (srcOnto == null ? " ; " : srcOnto.getName() + " ; "));
	}

	/**
	 * Initialize the gui.
	 */
	public void init() {
		jbInit();
	}

	public VisualStats(){
		super("Stats Visualizer");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JButton source = (JButton) e.getSource();
		if(source.equals(jButtonLoadSrcOnto)){
			fileChooser.setFileFilter(ontoFilter);

			int res = fileChooser.showOpenDialog(this);
			switch(res){
			case JFileChooser.CANCEL_OPTION:
				break;
			case JFileChooser.APPROVE_OPTION:
				setSourceOntologyFile(fileChooser.getSelectedFile());
				try {
					onto = OntoUtil.load(srcOnto.getAbsolutePath(), true, 
							OntoUtil.getManager(false));

					System.out.println(
							onto + "\n"
									+ "Classes:" + onto.getClassesInSignature().size()
									+"\nDatatype Properties: " + onto.getDatatypesInSignature().size() 
									+"\nObject Properties: " + onto.getObjectPropertiesInSignature().size());
					OntoUtil.unloadAllOntologies();
				} catch (OWLOntologyCreationException e1) {
					e1.printStackTrace();
					throw new Error("Error while processing selected ontologies.");
				}
				break;
			case JFileChooser.ERROR_OPTION:
				System.out.println("Error while opening the file");
				break;
			default:
				System.exit(1);
			}
			return;
		}
	}
	@Override
	public void windowOpened(WindowEvent e) {
		this.setExtendedState(JFrame.MAXIMIZED_BOTH);  
	}

	@Override
	public void windowClosing(WindowEvent arg0) {
		String message = "Do you really want to exit?";
		int answer = JOptionPane.showConfirmDialog(
				this, message,"Exit",JOptionPane.YES_NO_OPTION);
		if (answer == JOptionPane.YES_OPTION)
			System.exit(0);
	}

	@Override
	public void windowClosed(WindowEvent e) {
	}

	@Override
	public void windowIconified(WindowEvent e) {
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
	}

	@Override
	public void windowActivated(WindowEvent e) {
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
	}

	private void jbInit() {
		progBar.setIndeterminate(false);
		this.setPreferredSize(new Dimension(600, 550));
		Font arial = new Font("Arial", Font.PLAIN, 12);

		jButtonLoadSrcOnto.setFont(arial);		
		jLabelFilesSelected.setFont(arial);
		statusLabel.setFont(arial);

		jButtonLoadSrcOnto.addActionListener(this);

		JRootPane rootPane = this.getRootPane();

		dummyScrollPane.setVerticalScrollBarPolicy(
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

		// toolbar's element insertion

		topToolbar.add(jButtonLoadSrcOnto);

		// layout
		this.getContentPane().setLayout(borderLayout1);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		this.addWindowListener(this);

		splitPaneForTextAreas.add(dummyScrollPane);
		splitPaneForTextAreas.setOrientation(JSplitPane.HORIZONTAL_SPLIT);

		this.add(topToolbar, BorderLayout.NORTH);
		this.add(splitPaneForTextAreas, BorderLayout.CENTER);
		JPanel southPanel = new JPanel(new BorderLayout());
		southPanel.add(statusLabel, BorderLayout.WEST);
		southPanel.add(jLabelFilesSelected, BorderLayout.CENTER);
		southPanel.add(progBar, BorderLayout.EAST);
		this.add(southPanel, BorderLayout.SOUTH);
		this.doLayout();
		this.setExtendedState(this.getExtendedState() | JFrame.MAXIMIZED_BOTH);
		this.pack();
		splitPaneForTextAreas.setDividerLocation(0.3);
		this.setVisible(true);	
	}
}

class OntoFileFilter extends FileFilter {

	String[] formats;

	public OntoFileFilter(String[] strings){
		for (int i = 0; i < strings.length; i++) {
			strings[i] = strings[i].toLowerCase();
		}
		this.formats = strings;
	}

	/**
	 * Methods that accept or reject a file by the use of a filter.
	 *
	 * @param file the file to accept or reject
	 * @return true if the file is acceptable for this filter, false otherwise
	 */
	public boolean accept(File file) {
		if (file.isDirectory()) return true;
		String fname = file.getName().toLowerCase();
		for (int i = 0; i < formats.length; i++) {
			if(fname.endsWith("." + formats[i].toLowerCase()))
				return true;
		}
		return false;
	}

	/**
	 * Methods that return a string representation of the filter.
	 *
	 * @return the description of the filter
	 */
	public String getDescription() {
		return "File of format " + Arrays.toString(formats);
	}
}