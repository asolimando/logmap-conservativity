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

import scc.graphDataStructure.LightAdjacencyList;
import scc.graphDataStructure.LightEdge;
import enumerations.VIOL_KIND;
import graphViz.GraphCache;
import scc.gui.OpenFileFilter;
import graphViz.GraphComponentPopupMouseAdapter;
import graphViz.GraphOutlinePopupMouseAdapter;
import graphViz.MappingMouseWheelListener;
import graphViz.myConservativityGraph;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import logmap.LogMapWrapper;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import repair.ConservativityRepairFacility;

import uk.ac.ox.krr.logmap2.mappings.I_Sub;
import util.FileUtil;
import util.OntoUtil;
import util.Params;
import auxStructures.Pair;

import com.google.common.collect.Sets;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.mxGraphOutline;
import com.mxgraph.util.mxResources;

public class VisualConservativity extends JFrame implements WindowListener, 
ActionListener, ListSelectionListener {

	private static final long serialVersionUID = -3748246500598367398L;

	public OWLOntology fstO, sndO;
		
	private ConservativityRepairFacility repairFacility;
	private I_Sub isub = new I_Sub();

	protected myConservativityGraph graph;
	public mxGraphComponent graphComponent;
	public mxGraphOutline graphOutline;
	public MappingPopupMenu menu;
	
	private GraphBuilderTask graphTask;
	private PreferencesDialog prefDialog;
	
	private String previousStatus;

	protected JButton jButtonAnalyze = new JButton("Analyze"),
			jButtonLoadSrcOnto = new JButton("Load Source Ontology"),
			jButtonLoadTrgOnto = new JButton("Load Target Ontology"),
			jButtonLoadMapping = new JButton("Load Mappings"),
			jButtonCheckUncheck = new JButton("Check/Uncheck All"),
			jButtonDiag = new JButton("Compute Diagnosis"),
			jButtonSaveMapping = new JButton("Save Mapping"),
			jButtonPreferences = new JButton("Preferences"),
			jButtonIsDirect = new JButton("Direct?"),
			jButtonConfidence = new JButton("Confidence?");

	private JButton [] buttons = {jButtonAnalyze, jButtonLoadSrcOnto, 
			jButtonLoadTrgOnto, jButtonCheckUncheck, 
			jButtonLoadMapping, jButtonDiag, jButtonSaveMapping, 
			jButtonPreferences, jButtonIsDirect, jButtonConfidence};
	private JButton [] violButtons = {jButtonIsDirect, jButtonConfidence};

	private boolean checkAll = false;
	
	protected List<Pair<OWLClass>> violations = new ArrayList<>();
	private Pair<OWLClass> selectedViol = null;
	private Set<Pair<OWLClass>> truePosViols = Sets.newHashSet();
	private Set<Pair<OWLClass>> removedMappings = Sets.newHashSet();
	protected Map<Pair<OWLClass>, GraphCache> graphCache = new HashMap<>();

	private DefaultListModel<Pair<OWLClass>> listModel = new DefaultListModel<>(); 
	private DefaultListModel<JCheckBox> listSelViolModel = new DefaultListModel<>(); 
	
	private JList<Pair<OWLClass>> listViols = new JList<>(listModel); 	
	private JChkBoxList listSelViols = new JChkBoxList(this); 
	private JScrollPane listScroller;

	private JProgressBar progBar = new JProgressBar(SwingConstants.HORIZONTAL);

	/** Label showing the name of the schema file selected. */
	String baseSelectedLabel = " - (Source Ontology ; Target Ontology ; " +
			"Mapping) -> ",
			defaultStatusLbl = "Select a violation";
	protected JLabel jLabelFilesSelected = new JLabel(baseSelectedLabel),
			statusLabel = new JLabel("Initialization completed");

	protected File srcOnto, trgOnto, mapping;

	private String 
	defSourceOnto =
	//"/home/ale/Dropbox/PHD/Tesi/SW_Tesi/manualOntoExample/o1.owl",
	//"/home/ale/Dropbox/PHD/Tesi/SW_Tesi/manualOntoExample/Food1.owl",
	//Params.dataFolder + "oaei2012/largebio/onto/oaei2012_FMA_small_overlapping_snomed.owl",
	//Params.dataFolder + "oaei2012/largebio/onto/oaei2012_FMA_whole_ontology.owl", 
//	Params.dataFolder + "oaei2013/anatomy/onto/mouse.owl",
	Params.dataFolder + "oaei2013/conference/onto/cmt.owl",
//	Params.dataFolder + "oaei2013/conference/onto/conference.owl",
	defTargetOnto = 
	//"/home/ale/Dropbox/PHD/Tesi/SW_Tesi/manualOntoExample/o2.owl",
	//"/home/ale/Dropbox/PHD/Tesi/SW_Tesi/manualOntoExample/Food2.owl",
	//Params.dataFolder + "oaei2012/largebio/onto/oaei2012_SNOMED_small_overlapping_fma.owl",
	//Params.dataFolder + "oaei2012/largebio/onto/oaei2012_NCI_whole_ontology.owl",
//	Params.dataFolder + "oaei2013/anatomy/onto/human.owl",
	Params.dataFolder + "oaei2013/conference/onto/confof.owl",
//	Params.dataFolder + "oaei2013/conference/onto/edas.owl",
	defMapping =
	//"/home/ale/Dropbox/PHD/Tesi/SW_Tesi/manualOntoExample/m.rdf";
	//"/home/ale/Dropbox/PHD/Tesi/SW_Tesi/manualOntoExample/foodAlign2.rdf";
	//Params.dataFolder + "oaei2012/largebio/alignments/HERTUDA/hertuda_small_fma2snomed.rdf";
	//"/home/ale/svn/Conservativity/Code/test/test7/mappings/mouse-human-light.rdf";
//			Params.dataFolder + "oaei2013/anatomy/reference/mouse-human.rdf";
	Params.dataFolder + "oaei2013/conference/reference/cmt-confof.rdf";
//	Params.dataFolder + "oaei2013/conference/reference/conference-edas.rdf";
	//Params.dataFolder + "oaei2012/largebio/alignments/GOMMA/gomma_small_fma2nci.rdf";

	private OpenFileFilter ontoFilter = new OpenFileFilter(new String[]{"owl"}),
			mappingFilter = new OpenFileFilter(new String[]{"rdf"});	

	private JFileChooser fileChooser = 
			new JFileChooser(System.getProperty("user.dir"));

	private BorderLayout mainWinLayout = new BorderLayout();

	public JSplitPane splitPaneSCCs = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

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
		System.getProperties().setProperty(
				"java.util.Arrays.useLegacyMergeSort", "true");

		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new VisualConservativity().init();
			}
		});
	}

	public VisualConservativity(){
		super("Visual Conservativity Debugger");
	}

	public Pair<OWLClass> getViolation(int index){
		return listViols.getModel().getElementAt(index);
	}
	
	public boolean isViolDisj(Pair<OWLClass> v){
		return truePosViols.contains(v);
	}
	
	public boolean addViolDisj(Pair<OWLClass> v){
		return truePosViols.add(v);
	}
	
	public boolean removeViolDisj(Pair<OWLClass> v){
		return truePosViols.remove(v);
	}
	
	/**
	 * Initialize the gui.
	 */
	public void init() {
		generalParamsInit();
		buttonsInit();
		listsInit();
		listenerInit();
		toolbarInit();
		layoutInit();
	}
	
	private void generalParamsInit(){
		Params.storeViolations = true;
		Params.visualizationGUI = true;
	}

	private void toolbarInit(){
		// toolbar's element insertion
		topToolbar.add(jButtonAnalyze);
		topToolbar.add(jButtonLoadSrcOnto);
		topToolbar.add(jButtonLoadTrgOnto);
		topToolbar.add(jButtonLoadMapping);
		topToolbar.add(jButtonSaveMapping);
		topToolbar.addSeparator();
		topToolbar.add(jButtonCheckUncheck);
		topToolbar.add(jButtonDiag);
		topToolbar.add(jButtonIsDirect);
		topToolbar.add(jButtonConfidence);
		topToolbar.addSeparator();
		topToolbar.add(jButtonPreferences);
	}

	public OWLReasoner getFirstReasoner(){
		return repairFacility.getFirstReasoner();
	}
	
	public void setRepairFacility(ConservativityRepairFacility repairFacility){
		this.repairFacility = repairFacility; 
	}
	
	public OWLReasoner getSecondReasoner(){
		return repairFacility.getSecondReasoner();		
	}
	
	public ConservativityRepairFacility getRepairFacility() {
		return repairFacility;
	}
	
	private void listenerInit(){
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		this.addWindowListener(this);

		statusLabel.addMouseListener(
				new InfoLabelMouseListener(statusLabel,this));
	}

	private void layoutInit(){
		prefDialog = new PreferencesDialog(this);

		progBar.setIndeterminate(false);

		JRootPane rootPane = this.getRootPane();
		rootPane.setDefaultButton(jButtonAnalyze);

		dummyScrollPane.setVerticalScrollBarPolicy(
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

		// layout
		this.getContentPane().setLayout(mainWinLayout);

		splitPaneSCCs.setLeftComponent(listScroller);
		splitPaneSCCs.setRightComponent(dummyScrollPane);
		splitPaneSCCs.setOrientation(JSplitPane.HORIZONTAL_SPLIT);

		this.add(topToolbar, BorderLayout.NORTH);
		this.add(splitPaneSCCs, BorderLayout.CENTER);

		JPanel southPanel = new JPanel(new BorderLayout());
		southPanel.add(statusLabel, BorderLayout.WEST);
		southPanel.add(jLabelFilesSelected, BorderLayout.CENTER);
		southPanel.add(progBar, BorderLayout.EAST);
		this.add(southPanel, BorderLayout.SOUTH);
		this.doLayout();
		this.setExtendedState(this.getExtendedState() | JFrame.MAXIMIZED_BOTH);
		this.pack();
		this.setVisible(true);
		// effective only after setVisible
		//		splitPaneSCCs.setDividerLocation(0.3);
		//		splitPaneMappings.setDividerLocation(0.8);
		splitPaneSCCs.setResizeWeight(0.3);
	}

	private void buttonsInit() {
		setPreferredSize(new Dimension(600, 550));
//		Font arial = new Font("Arial", Font.PLAIN, 12);

		for (int i = 0; i < buttons.length; i++) {
//			buttons[i].setFont(arial);
			buttons[i].addActionListener(this);
		}

		jButtonPreferences.setEnabled(true);
		jButtonSaveMapping.setEnabled(false);		
		setViolButtons(false);
	}

	private void setDiagnosisButtons(){
		jButtonDiag.setEnabled(isAnalysisCompleted());
//		if(selectedViol == null){
//			jButtonDiag.setEnabled(false);
//			return;
//		}
//		boolean en = hasDiagnosis(selectedSCC);
//		jButtonDiag.setEnabled(!en);
	}

	private void setButtonsEnabled(boolean state){
		for (JButton button : buttons)
			button.setEnabled(state);
		setDiagnosisButtons();
	}

	public Set<Pair<OWLClass>> getRemovedMappings(){
		return Collections.unmodifiableSet(removedMappings);
	}
	
	public void addRemovedMappings(
			Collection<? extends Pair<OWLClass>> pairs){
		removedMappings.addAll(pairs);
	}
	
	public void addRemovedMappings(List<Pair<Integer>> pairs){
		removedMappings.addAll(
				LogMapWrapper.getOWLClassFromIndexPair(pairs, 
						getRepairFacility().getAlignIndex()));
	}

	void setViolButtons(boolean state){
		for (JButton button : violButtons)
			button.setEnabled(state);
		setDiagnosisButtons();
	}

	private void uncheckAllViolations(){
		checkAll = false;
		listSelViols.uncheckAllViolations();
		refreshCheckedList();		
	}
	
	private void checkAllViolations(){
		checkAll = true;
		listSelViols.checkAllViolations();
		refreshCheckedList();
	}
	
	protected JList<?> getViolsList(){
		return listViols;
	} 
	
	protected void showViols(final List<Pair<OWLClass>> violations){		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if(Params.verbosity > 1)
					System.out.print("Adding violations to list in the GUI...");
				
				listModel.removeAllElements();
				listSelViolModel.removeAllElements();
				for (Pair<OWLClass> v : violations){
					listModel.addElement(v);
					listSelViolModel.addElement(new JCheckBox());
				}
				if(Params.verbosity > 1)
					System.out.println(" DONE!");		    	
			}
		});
	}

	public void setAdjancencyList(LightAdjacencyList adj){
		repairFacility.setAdjacencyList(adj);
	}

	public LightAdjacencyList getAdjancencyList(){
		return repairFacility == null ? null : repairFacility.getAdjacencyList();
	}

	protected void showSCCMappings(final Set<LightEdge> mappings){		
//		SwingUtilities.invokeLater(new Runnable() {
//			public void run() {
//				listMappings.removeAllElements();
//				for (LightEdge m : mappings)
//					listMappings.addElement(m);
//			}
//		});
	}

	private void setSourceOntologyFile(String src){
		setSourceOntologyFile(new File(src));
	}

	private void setTargetOntologyFile(String trg){
		setTargetOntologyFile(new File(trg));
	}

	private void setSourceOntologyFile(File src){
		srcOnto = src;
		updateFilesLabel();
	}

	private void setTargetOntologyFile(File trg){
		trgOnto = trg;
		updateFilesLabel();
	}

	private void setMappingFile(File mapping){
		this.mapping = mapping;
		updateFilesLabel();
	}

	private void updateFilesLabel(){
		resetListAndPanel(false);
		jLabelFilesSelected.setText(baseSelectedLabel + " ( " 
				+ (srcOnto == null ? " ; " : srcOnto.getName() + " ; ") 
				+ (trgOnto == null ? " ; " : trgOnto.getName() + " ; ") 
				+ (mapping == null ? "" : mapping.getName()) + " ) ");
	}

	protected void resetListAndPanel(boolean loading){
		if(!loading)
			repairFacility = null;
		
		listModel.removeAllElements();
		if(graphComponent!=null){
			graphComponent.removeAll();
			graphComponent.refresh();
		}
		statusLabel.setText(defaultStatusLbl);
		setViolButtons(false);
	}


	public void refreshGraph(){
		if(graph != null)
			graph.refresh();	
	}

	private void refreshCheckedList(){
		listSelViols.updateUI();
	}

	protected void setComputingStatus(boolean status){
		progBar.setIndeterminate(status);
		setButtonsEnabled(!status);
		if(status){
			previousStatus = statusLabel.getText();
			statusLabel.setText("Computing...");
		}
		else
			statusLabel.setText(previousStatus);
		listViols.setEnabled(!status);
		if(graphOutline != null)
			graphOutline.setEnabled(!status);
		if(graph != null)
			graph.setEnabled(!status);
		if(graphComponent != null)
			graphComponent.setEnabled(!status);
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if(e.getSource().equals(listViols)){
			if(isComputing())
				return;
			if (e.getValueIsAdjusting() == false) {
				if (listViols.getSelectedIndex() != -1) {
					setComputingStatus(true);

					selectedViol = listViols.getSelectedValue();
					graphTask = new GraphBuilderTask(selectedViol,this);
				
//					showSCCMappings(selectedViol.extractMappings(adj, false));
					
					graphTask.execute();
					setViolButtons(true);
				}
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JButton source = (JButton) e.getSource();
		if(source.equals(jButtonAnalyze)){
			if(getAdjancencyList() != null){
				int dialogResult = JOptionPane.showConfirmDialog(this, 
						"All the unsaved changes will be lost, " +
								"do you want to continue?",
								"Continue?",JOptionPane.YES_NO_OPTION);
				if(dialogResult == JOptionPane.NO_OPTION)
					return;
			}

			if(srcOnto == null)
				setSourceOntologyFile(defSourceOnto);
			if(trgOnto == null)
				setTargetOntologyFile(defTargetOnto);
			if(mapping == null)
				setMappingFile(new File(defMapping));

			setComputingStatus(true);
			
			selectedViol = null;
			new OntoLoaderTask(this).execute();
			return;
		}
		else if(source.equals(jButtonSaveMapping)){
			int res = fileChooser.showOpenDialog(this);
			switch(res){
			case JFileChooser.CANCEL_OPTION:
				break;
			case JFileChooser.APPROVE_OPTION:
				if(getAdjancencyList() != null){
					
					try {
						LogMapWrapper.writeAlignmentToFile(
								fileChooser.getSelectedFile().getAbsolutePath(),
								repairFacility.getFirstOntologyIRIStr(),
								repairFacility.getSecondOntologyIRIStr(),
								repairFacility.getRepairedMappings());
					} catch (Exception e1) {
						JOptionPane.showMessageDialog(this,
								"Cannot save mappings, exception occurred.",
								"Save failed",
								JOptionPane.ERROR_MESSAGE);
						System.out.println(
								"Cannot save mappings, exception occurred: " + 
										e1.getMessage());
					}
				}
				else {
					JOptionPane.showMessageDialog(this,
							"Cannot save mappings, no repair facility available.",
							"Save failed",
							JOptionPane.ERROR_MESSAGE);
					System.out.println(
							"Cannot save mappings, no repair facility available");
				}
				break;

			case JFileChooser.ERROR_OPTION:
				System.out.println("Error concerning the selected target file");
				break;
			default:
				System.exit(1);
			}
			return;
		}
		else if(source.equals(jButtonCheckUncheck)){
			if(!checkAll)
				checkAllViolations();
			else
				uncheckAllViolations();
		}
		else if(source.equals(jButtonLoadSrcOnto)){
			fileChooser.setFileFilter(ontoFilter);

			int res = fileChooser.showOpenDialog(this);
			switch(res){
			case JFileChooser.CANCEL_OPTION:
				break;
			case JFileChooser.APPROVE_OPTION:
				setSourceOntologyFile(fileChooser.getSelectedFile());
				resetListAndPanel(false);
				break;
			case JFileChooser.ERROR_OPTION:
				System.out.println("Error while opening the file");
				break;
			default:
				System.exit(1);
			}
			return;
		}
		else if(source.equals(jButtonLoadTrgOnto)){
			fileChooser.setFileFilter(ontoFilter);

			int res = fileChooser.showOpenDialog(this);
			switch(res){
			case JFileChooser.CANCEL_OPTION:
				break;
			case JFileChooser.APPROVE_OPTION:
				setTargetOntologyFile(fileChooser.getSelectedFile());
				resetListAndPanel(false);
				break;
			case JFileChooser.ERROR_OPTION:
				System.out.println("Error while opening the file");
				break;
			default:
				System.exit(1);
			}
			return;
		}
		else if(source.equals(jButtonLoadMapping)){			
			fileChooser.setFileFilter(mappingFilter);

			int res = fileChooser.showOpenDialog(this);
			switch(res){
			case JFileChooser.CANCEL_OPTION:
				break;
			case JFileChooser.APPROVE_OPTION:
				mapping = fileChooser.getSelectedFile();
				setMappingFile(mapping);
				resetListAndPanel(false);
				break;
			case JFileChooser.ERROR_OPTION:
				System.out.println("Error while opening the file");
				break;
			default:
				System.exit(1);
			}
			return;
		}
		else if(source.equals(jButtonDiag)){
			if(!isAnalysisCompleted()){
				JOptionPane.showMessageDialog(this,
						"Cannot compute a diagnosis when the analysis has " +
						"not been performed.", "Repair failed", 
						JOptionPane.ERROR_MESSAGE);
				
				FileUtil.writeLogAndConsole("Cannot compute a diagnosis when " +
						"the analysis has not been performed.");
			}				
			setComputingStatus(true);
			
			List<Pair<OWLClass>> selViols = getCheckedViolations();
			
			if(!selViols.isEmpty())
				new DiagnosisTask(this,selViols).execute();
			else {
				List<Pair<Integer>> viols = new ArrayList<>();
				
				if(Params.preComputeDirectViols){
					viols.addAll(getRepairFacility().getDirectViolations(
							true, Params.violKindToShow, getRepairFacility().getRepairStep()));
					viols.addAll(getRepairFacility().getDirectViolations(
							false, Params.violKindToShow, getRepairFacility().getRepairStep()));
				}
				new DiagnosisTask(viols,this).execute();
			}
			return;
		}
		else if(source.equals(jButtonPreferences)){
			prefDialog.setVisible(true);
		}
		else if(source.equals(jButtonIsDirect)){
			boolean direct = getRepairFacility().isDirectViolation(selectedViol);
			JOptionPane.showMessageDialog(this, 
					"The violation is" + (!direct ? " NOT" : "") + " direct.",
					"Is direct violation?", JOptionPane.INFORMATION_MESSAGE);
			if(Params.verbosity > 0)
				FileUtil.writeLogAndConsole(
						"The violation is" + (!direct ? " NOT" : "") + " direct.");
		}
		else if(source.equals(jButtonConfidence)){			
			double conf = isub.score(
					OntoUtil.getIRIShortFragment(selectedViol.getFirst().getIRI().toString()), 
					OntoUtil.getIRIShortFragment(selectedViol.getSecond().getIRI().toString()));
			
			JOptionPane.showMessageDialog(this, 
					"Confidence level: " + conf + ".",
					"Confidence", JOptionPane.INFORMATION_MESSAGE);
			if(Params.verbosity > 0)
				FileUtil.writeLogAndConsole(
						"Confidence level: " + conf + ".");
		}
	}

	private boolean isAnalysisCompleted() {
		return repairFacility != null;
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
	public void windowClosed(WindowEvent e) {}
	@Override
	public void windowIconified(WindowEvent e) {}
	@Override
	public void windowDeiconified(WindowEvent e) {}
	@Override
	public void windowActivated(WindowEvent e) {}
	@Override
	public void windowDeactivated(WindowEvent e) {}

	private void listsInit(){
		listViols.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		//listSCCs.setLayoutOrientation(JList.VERTICAL);
		listViols.setVisibleRowCount(-1);

		JPanel leftPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c1 = new GridBagConstraints(), 
				c2 = new GridBagConstraints();		
		c1.gridx = 0;
		c1.gridy = 0;
		c1.fill = GridBagConstraints.VERTICAL;
		c1.weightx = 0.1;
		
		c2.gridx = 1;
		c2.gridy = 0;
		c2.fill = GridBagConstraints.VERTICAL;

		leftPanel.add(listSelViols,c1);
		leftPanel.add(listViols,c2);
		
//		listScroller = new JScrollPane(listViols);
		listScroller = new JScrollPane(leftPanel);
		//listScroller.setPreferredSize(new Dimension(80, 250));
		listScroller.setMinimumSize(new Dimension(0, 0));

//		listMappings.setVisibleRowCount(-1);
		
		//listMappings.setCellRenderer(new MappingsCellRenderer(this));
		listViols.setCellRenderer(new ViolationsCellRenderer(this));
		listViols.addListSelectionListener(this);

//		mapListScroller = new JScrollPane(listMappings);
//		mapListScroller.setPreferredSize(new Dimension(80, 250));
//		mapListScroller.setMinimumSize(new Dimension(0, 0));
		
		listSelViols.setModel(listSelViolModel);
		listSelViols.setMinimumSize(new Dimension(0, 0));
		listSelViols.setVisibleRowCount(-1);
	}

	boolean isComputing(){
		return progBar.isIndeterminate();
	}


	protected void mouseWheelMoved(MouseWheelEvent e) {
		if (e.getWheelRotation() < 0) {
			graphComponent.zoomIn();
		}
		else {
			graphComponent.zoomOut();
		}
	}

	protected void installListeners() {

		// Installs mouse wheel listener for zooming
		MappingMouseWheelListener mouseWheelHandler = 
				new MappingMouseWheelListener(this);

		// Handles mouse wheel events in the outline and graph component
		graphOutline.addMouseWheelListener(mouseWheelHandler);
		graphComponent.addMouseWheelListener(mouseWheelHandler);

		GraphOutlinePopupMouseAdapter outlinePoputListener = new 
				GraphOutlinePopupMouseAdapter(this);
		
		// Installs the popup menu in the outline
		graphOutline.addMouseListener(outlinePoputListener);

		GraphComponentPopupMouseAdapter componentPoputListener = new 
				GraphComponentPopupMouseAdapter(this);
		
		// Installs the popup menu in the graph component
		graphComponent.getGraphControl().addMouseListener(
				componentPoputListener);
	}

	public void showOutlinePopupMenu(MouseEvent e) {
		Point pt = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(),
				graphComponent);
		JCheckBoxMenuItem item = new JCheckBoxMenuItem(mxResources
				.get("magnifyPage"));
		item.setSelected(graphOutline.isFitPage());

		item.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				graphOutline.setFitPage(!graphOutline.isFitPage());
				graphOutline.repaint();
			}
		});

		JCheckBoxMenuItem item2 = new JCheckBoxMenuItem(mxResources
				.get("showLabels"));
		item2.setSelected(graphOutline.isDrawLabels());

		item2.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				graphOutline.setDrawLabels(!graphOutline.isDrawLabels());
				graphOutline.repaint();
			}
		});

		JCheckBoxMenuItem item3 = new JCheckBoxMenuItem(mxResources
				.get("buffering"));
		item3.setSelected(graphOutline.isTripleBuffered());

		item3.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				graphOutline
				.setTripleBuffered(!graphOutline.isTripleBuffered());
				graphOutline.repaint();
			}
		});

		JPopupMenu menu = new JPopupMenu();
		menu.add(item);
		menu.add(item2);
		menu.add(item3);
		menu.show(graphComponent, pt.x, pt.y);

		e.consume();
	}

	public void showGraphPopupMenu(MouseEvent e) {
		Point pt = SwingUtilities.convertPoint(
				e.getComponent(), e.getPoint(),graphComponent);
		
		if(menu == null)
			menu = new MappingPopupMenu(this);
		
		menu.show(graphComponent, pt.x, pt.y);
		e.consume();
	}

	public List<Pair<OWLClass>> getCheckedViolations(){
		return listSelViols.getCheckedElements();
	}
	
	public Pair<OWLClass> getSelectedViolation() {
		return selectedViol;
	}
	
	protected List<Pair<OWLClass>> getViolations(){
		violations.clear();
		
		violations.addAll(LogMapWrapper.getOWLClassFromIndexPair(
				repairFacility.getViolations(true, Params.violKindToShow, 
						repairFacility.getRepairStep()), 
						repairFacility.getOrigIndex()));

		violations.addAll(LogMapWrapper.getOWLClassFromIndexPair(
				repairFacility.getViolations(false, Params.violKindToShow, 
						repairFacility.getRepairStep()), 
						repairFacility.getOrigIndex()));
		
		return violations;
	}

	public boolean isDeleted(Pair<OWLClass> v){
		return removedMappings.contains(v);
	}

	public boolean isViolationSolved(Pair<OWLClass> v) {
		return !getViolations().contains(v);
	}
}