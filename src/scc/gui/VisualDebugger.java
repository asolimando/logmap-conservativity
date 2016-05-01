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
import scc.graphDataStructure.LightAdjacencyList;
import scc.graphDataStructure.LightEdge;
import scc.graphDataStructure.LightSCC;
import scc.graphViz.SCCGraphCache;
import scc.graphViz.GraphComponentPopupMouseAdapter;
import scc.graphViz.GraphOutlinePopupMouseAdapter;
import scc.graphViz.mySCCGraph;
import scc.graphViz.MappingMouseWheelListener;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.DefaultListModel;
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
import util.Params;
import util.Util;
import scc.algoSolver.ASPSolver;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.mxGraphOutline;
import com.mxgraph.util.mxResources;

public class VisualDebugger extends JFrame implements WindowListener, 
ActionListener, ListSelectionListener { //PropertyChangeListener

	private static final long serialVersionUID = -3748246500598367398L;

	private LightAdjacencyList adj;

	protected mySCCGraph graph;
	public mxGraphComponent graphComponent;
	public mxGraphOutline graphOutline;
	//	private MouseHandler mouseHandler;
	public MappingPopupMenu menu;
	
	private GraphBuilderTask graphTask;
	private PreferencesDialog prefDialog;
	//private InstancesDialog instDialog;
	
	private String previousStatus;

	protected JButton jButtonAnalyze = new JButton("Analyze"),
			jButtonLoadSrcOnto = new JButton("Load Source Ontology"),
			jButtonLoadTrgOnto = new JButton("Load Target Ontology"),
			jButtonLoadMapping = new JButton("Load Mapping"),
			jButtonDiag = new JButton("Compute Diagnosis"),
			jButtonFilter = new JButton("Un/filter multiple"),
			jButtonSaveMapping = new JButton("Save Mapping"),
			jButtonRetractDiagnosis = new JButton("Retract Local Diagnosis"),
			jButtonPreferences = new JButton("Preferences"),
			jButtonIsSafe = new JButton("SCC Safe?"),
			jButtonIndiv = new JButton("Instances"),
			jButtonResetSCC = new JButton("Reset SCC");

	private JButton [] buttons = {jButtonAnalyze, jButtonLoadSrcOnto, 
			jButtonLoadTrgOnto, jButtonLoadMapping, jButtonDiag, jButtonFilter, 
			jButtonSaveMapping, jButtonPreferences, jButtonIsSafe, jButtonIndiv,
			jButtonResetSCC, jButtonRetractDiagnosis};
	private JButton [] sccButtons = {jButtonDiag, jButtonFilter,
			jButtonRetractDiagnosis, jButtonIsSafe, jButtonIndiv,
			jButtonResetSCC};

	protected Set<LightSCC> problematicSCCs = new HashSet<>();
	private LightSCC selectedSCC = null;
	protected Map<LightSCC, Diagnosis> localDiagnoses = new HashMap<>();
	protected Map<LightSCC, Diagnosis> localFilters = new HashMap<>();
	protected Map<LightSCC, Diagnosis> localDeletions = new HashMap<>();
	protected Map<LightSCC, Set<LightEdge>> sealedMappings = new HashMap<>();
	protected Map<LightSCC, SCCGraphCache> graphCache = new HashMap<>();

	private DefaultListModel<LightSCC> listModel = new DefaultListModel<>(); 
	//private DefaultListModel<LightEdge> listModelMap = new DefaultListModel<>();  

	private JList<LightSCC> listSCCs = new JList<>(listModel); 
	//private JList<LightEdge> listMappings = new JList<>(listModelMap);
	private JCheckBoxList listMappings = new JCheckBoxList(this); 

	private JScrollPane listScroller, mapListScroller;

	private JProgressBar progBar = new JProgressBar(SwingConstants.HORIZONTAL);

	/** Label showing the name of the schema file selected. */
	String baseSelectedLabel = " - (Source Ontology ; Target Ontology ; " +
			"Mapping) -> ",
			defaultStatusLbl = "Select an SCC";
	protected JLabel jLabelFilesSelected = new JLabel(baseSelectedLabel),
			statusLabel = new JLabel("Initialization completed");

	protected File srcOnto, trgOnto, mapping;

	private String 
	defSourceOnto =
	//"/home/ale/Dropbox/PHD/Tesi/SW_Tesi/manualOntoExample/o1.owl",
	//"/home/ale/Dropbox/PHD/Tesi/SW_Tesi/manualOntoExample/Food1.owl",
	//Params.dataFolder + "oaei2012/largebio/onto/oaei2012_FMA_small_overlapping_snomed.owl",
	//Params.dataFolder + "oaei2012/largebio/onto/oaei2012_FMA_whole_ontology.owl", 
	Params.dataFolder + "oaei2013/anatomy/onto/mouse.owl",
	defTargetOnto = 
	//"/home/ale/Dropbox/PHD/Tesi/SW_Tesi/manualOntoExample/o2.owl",
	//"/home/ale/Dropbox/PHD/Tesi/SW_Tesi/manualOntoExample/Food2.owl",
	//Params.dataFolder + "oaei2012/largebio/onto/oaei2012_SNOMED_small_overlapping_fma.owl",
	//Params.dataFolder + "oaei2012/largebio/onto/oaei2012_NCI_whole_ontology.owl",
	Params.dataFolder + "oaei2013/anatomy/onto/human.owl",
	defMapping =
	//"/home/ale/Dropbox/PHD/Tesi/SW_Tesi/manualOntoExample/m.rdf";
	//"/home/ale/Dropbox/PHD/Tesi/SW_Tesi/manualOntoExample/foodAlign2.rdf";
	//Params.dataFolder + "oaei2012/largebio/alignments/HERTUDA/hertuda_small_fma2snomed.rdf";
	//"/home/ale/svn/Conservativity/Code/test/test7/mappings/mouse-human-light.rdf";
	"/home/ale/bzr/ontoMapping.bzr/DirectedLoopDetection/test/test0/mappings/mouse-human.rdf";
	//Params.dataFolder + "oaei2012/largebio/alignments/GOMMA/gomma_small_fma2nci.rdf";

	private OpenFileFilter ontoFilter = new OpenFileFilter(new String[]{"owl"}),
			mappingFilter = new OpenFileFilter(new String[]{"rdf"});	

	private JFileChooser fileChooser = 
			new JFileChooser(System.getProperty("user.dir"));

	private BorderLayout mainWinLayout = new BorderLayout();

	public JSplitPane 
	splitPaneSCCs = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

	private JSplitPane splitPaneMappings = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

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
				new VisualDebugger().init();
			}
		});
	}

	public VisualDebugger(){
		super("Visual Cycle Breaker");
	}

	/**
	 * Initialize the gui.
	 */
	public void init() {
		buttonsInit();
		listsInit();
		listenerInit();
		toolbarInit();
		layoutInit();
	}

	private void toolbarInit(){
		// toolbar's element insertion
		topToolbar.add(jButtonAnalyze);
		topToolbar.add(jButtonLoadSrcOnto);
		topToolbar.add(jButtonLoadTrgOnto);
		topToolbar.add(jButtonLoadMapping);
		topToolbar.add(jButtonSaveMapping);
		topToolbar.addSeparator();
		topToolbar.add(jButtonDiag);
		topToolbar.add(jButtonRetractDiagnosis);
		topToolbar.add(jButtonFilter);
		topToolbar.add(jButtonIsSafe);
		topToolbar.add(jButtonIndiv);
		topToolbar.add(jButtonResetSCC);
		topToolbar.addSeparator();
		topToolbar.add(jButtonPreferences);
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
		//dummyScrollPane.setPreferredSize(new Dimension(100, 900));
		//		dummyScrollPane.setMinimumSize(new Dimension(0, 0));

		// layout
		this.getContentPane().setLayout(mainWinLayout);

		splitPaneSCCs.setLeftComponent(listScroller);
		splitPaneSCCs.setRightComponent(dummyScrollPane);
		splitPaneSCCs.setOrientation(JSplitPane.HORIZONTAL_SPLIT);

		splitPaneMappings.setLeftComponent(splitPaneSCCs);
		splitPaneMappings.setRightComponent(mapListScroller);
		splitPaneMappings.setOrientation(JSplitPane.HORIZONTAL_SPLIT);

		this.add(topToolbar, BorderLayout.NORTH);
		this.add(splitPaneMappings, BorderLayout.CENTER);
		//this.add(splitPaneSCCs, BorderLayout.CENTER);
		//this.add(mapListScroller, BorderLayout.EAST);

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
		splitPaneSCCs.setResizeWeight(0.1);
		splitPaneMappings.setResizeWeight(0.9);
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
		setSCCButtons(false);
	}

	private void setDiagnosisButtons(){
		if(selectedSCC == null){
			jButtonDiag.setEnabled(false);
			jButtonRetractDiagnosis.setEnabled(false);
			return;
		}
		boolean en = hasDiagnosis(selectedSCC);
		jButtonDiag.setEnabled(!en);
		jButtonRetractDiagnosis.setEnabled(en && !hasFiltering(selectedSCC));
	}

	public void retractManualMappingDeletions(Collection<LightEdge> mappings){
		for (LightEdge m : mappings)
			retractManualMappingDeletion(m);
	}

	public void retractManualMappingDeletion(LightEdge mapping){
		String baseError = "Impossible to retract manual mapping deletion, ";
		if(!localDeletions.containsKey(selectedSCC))
			throw new Error(baseError 
					+ "no mappings where deleted for the selected SCC.");
		if(!localDeletions.get(selectedSCC).remove(mapping))
			throw new Error(baseError + "the mapping was not deleted.");
		adj.retractRemovedMappings(mapping);
	}

	public void manualMappingDeletions(Collection<LightEdge> mappings){
		for (LightEdge m : mappings)
			manualMappingDeletion(m);
	}

	public void manualMappingDeletion(LightEdge mapping){
		Diagnosis dels = null;
		
		if(isSealed(selectedSCC, mapping))
			throw new Error("Cannot delete a sealed mapping");
		
		if(!localDeletions.containsKey(selectedSCC))
			localDeletions.put(selectedSCC, new Diagnosis());

		dels = localDeletions.get(selectedSCC);
		dels.add(mapping);
		adj.removeMapping(mapping);
	}

	private void setButtonsEnabled(boolean state){
		for (JButton button : buttons)
			button.setEnabled(state);
		setDiagnosisButtons();
	}

	void setSCCButtons(boolean state){
		for (JButton button : sccButtons)
			button.setEnabled(state);
		setDiagnosisButtons();
	}

	protected void showSCCs(final Set<LightSCC> problematicSCCs){		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if(Params.verbosity > 1)
					System.out.print("Adding SCCs to list in the GUI...");
				listModel.removeAllElements();
				for (LightSCC lightSCC : problematicSCCs){
					listModel.addElement(lightSCC);
					if(Params.verbosity > 1)
						lightSCC.printProblematicSCC(adj);
				}
				if(Params.verbosity > 1)
					System.out.println(" DONE!");		    	
			}
		});
	}

	public boolean isMappingSealed(LightSCC scc, LightEdge m){
		if(!sealedMappings.containsKey(scc))
			return false;
		return sealedMappings.get(scc).contains(m);
	}
	
	public void unsealMapping(LightSCC scc, LightEdge m) {
		if(!sealedMappings.containsKey(scc) || 
				!sealedMappings.get(scc).contains(m))
			throw new Error("Mapping was not sealed, cannot unseal it");
		sealedMappings.get(scc).remove(m);
		listMappings.setSealStatus(m,false);
		refreshMappingList();
	}
	
	public void sealMapping(LightSCC scc, LightEdge m) {
		Set<LightEdge> sealedSet;
		if(!sealedMappings.containsKey(scc)){
			sealedSet = new HashSet<>();
			sealedMappings.put(scc, sealedSet);
		}
		sealedMappings.get(scc).add(m);
		listMappings.setSealStatus(m,true);
		refreshMappingList();
	}

	public void setAdjancencyList(LightAdjacencyList adj){
		this.adj = adj;
	}

	public LightAdjacencyList getAdjancencyList(){
		return adj;
	}

	protected void showSCCMappings(final Set<LightEdge> mappings){		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				listMappings.removeAllElements();
				for (LightEdge m : mappings)
					listMappings.addElement(m);
			}
		});
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
		resetListAndPanel();
		jLabelFilesSelected.setText(baseSelectedLabel + " ( " 
				+ (srcOnto == null ? " ; " : srcOnto.getName() + " ; ") 
				+ (trgOnto == null ? " ; " : trgOnto.getName() + " ; ") 
				+ (mapping == null ? "" : mapping.getName()) + " ) ");
	}

	protected void resetListAndPanel(){
		listModel.removeAllElements();
		if(graphComponent!=null){
			graphComponent.removeAll();
			graphComponent.refresh();
		}
		statusLabel.setText(defaultStatusLbl);
		setSCCButtons(false);
	}


	private void refreshGraph(){
		graph.refresh();	
	}
	private void refreshMappingList(){
		listMappings.updateUI();
	}
	public void refreshGraphAndMappingList(){
		refreshGraph();
		refreshMappingList();
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
		listSCCs.setEnabled(!status);
		if(graphOutline != null)
			graphOutline.setEnabled(!status);
		if(graph != null)
			graph.setEnabled(!status);
		if(graphComponent != null)
			graphComponent.setEnabled(!status);
		
		if(adj == null || selectedSCC == null || !selectedSCC.hasInstances(adj))
			jButtonIndiv.setEnabled(false);
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if(e.getSource().equals(listSCCs)){
			if(isComputing())
				return;
			if (e.getValueIsAdjusting() == false) {
				if (listSCCs.getSelectedIndex() != -1) {
					setComputingStatus(true);

					selectedSCC = listSCCs.getSelectedValue();
					if(Params.verbosity > 0)
						selectedSCC.printProblematicSCC(adj);
					graphTask = new GraphBuilderTask(selectedSCC,this);
					//graphTask.addPropertyChangeListener(this);

					showSCCMappings(selectedSCC.extractMappings(adj, false));
					
					graphTask.execute();
					setSCCButtons(true);
					
					if(!selectedSCC.hasInstances(adj))
						jButtonIndiv.setEnabled(false);
				}
			}
		}
	}

	private void printDiagnosisOnLabel(final Diagnosis d){ 
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if(d != null)
					statusLabel.setText(
							selectedSCC.printDimensions(adj) 
							+ ", Diagnosis (w=" + d.getWeight() 
							+ "): " + d + " (" + d.getTime() + "ms)");
				else	
					statusLabel.setText(
							selectedSCC.printDimensions(adj) 
							+ ", Unsatisfiable Problem");
			}
		});
	}

	protected void clearDiagnosesFilters(){
		localDiagnoses.clear();
		localFilters.clear();
		jButtonRetractDiagnosis.setEnabled(false);
	}

	private void retractAll(LightSCC scc){
		retractDiagnosis(scc);
		retractDeletions(scc);
		retractFilter(scc);
	}

	private void retractFilter(LightSCC scc){
		if(localFilters.containsKey(scc)){				
			Diagnosis diag = localFilters.get(scc);
			adj.retractRemovedMappings(diag);
			localFilters.remove(scc);
			// redraw the SCC
			graph.markAsDeleted(diag,mySCCGraph.mappingStyle);
			refreshGraphAndMappingList();
		}
	}

	private void retractDeletions(LightSCC scc){
		if(localDeletions.containsKey(scc)){				
			Diagnosis diag = localDeletions.get(scc);
			adj.retractRemovedMappings(diag);
			localDeletions.remove(scc);
			// redraw the SCC
			graph.markAsDeleted(diag,mySCCGraph.mappingStyle);
			refreshGraphAndMappingList();
		}
	}

	private void retractDiagnosis(LightSCC scc){
		if(localDiagnoses.containsKey(scc)){				
			Diagnosis diag = localDiagnoses.get(scc);
			adj.retractRemovedMappings(diag);
			localDiagnoses.remove(scc);
			// redraw the SCC
			graph.markAsDeleted(diag,mySCCGraph.mappingStyle);
			refreshGraphAndMappingList();
		}
		jButtonDiag.setEnabled(true);
		jButtonRetractDiagnosis.setEnabled(false);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JButton source = (JButton) e.getSource();
		if(source.equals(jButtonAnalyze)){
			if(adj != null){
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
			
			selectedSCC = null;
			new OntoLoaderTask(this).execute();
			return;
		}
		else if(source.equals(jButtonSaveMapping)){
			int res = fileChooser.showOpenDialog(this);
			switch(res){
			case JFileChooser.CANCEL_OPTION:
				break;
			case JFileChooser.APPROVE_OPTION:
				if(adj != null){
					Set<LightEdge> repairedAlign = 
							new HashSet<LightEdge>(adj.getOriginalMappings());
					repairedAlign.removeAll(adj.removedMappings);
					adj.getOAEIHandler().writeMappings(
							fileChooser.getSelectedFile().getAbsolutePath(), 
							repairedAlign, 
							adj.getOntoPrefix(true), 
							adj.getOntoPrefix(false)
							);
				}
				else {
					JOptionPane.showMessageDialog(this,
							"Cannot save mappings, no adjacency list available.",
							"Save failed",
							JOptionPane.ERROR_MESSAGE);
					System.out.println(
							"Cannot save mappings, no adjacency list available");
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
		else if(source.equals(jButtonRetractDiagnosis)){
			retractDiagnosis(selectedSCC);
		}
		else if(source.equals(jButtonLoadSrcOnto)){
			fileChooser.setFileFilter(ontoFilter);

			int res = fileChooser.showOpenDialog(this);
			switch(res){
			case JFileChooser.CANCEL_OPTION:
				break;
			case JFileChooser.APPROVE_OPTION:
				setSourceOntologyFile(fileChooser.getSelectedFile());
				resetListAndPanel();
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
				resetListAndPanel();
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
				resetListAndPanel();
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
			if(selectedSCC == null){
				JOptionPane.showMessageDialog((Component) e.getSource(),
						"No SCC selected.",
						"No SCC warning",
						JOptionPane.WARNING_MESSAGE);
				return;
			}
			ASPSolver aspSolver = new ASPSolver(adj, selectedSCC, 1, 
					Params.conservativeDiagnosis, false, false);

			//			aspSolver.setSealedMappings(
			//					new HashSet<LightEdge>(
			//							listMappings.getSelectedValuesList()));			
			aspSolver.setSealedMappings(listMappings.getCheckedElems());

			setComputingStatus(true);
			new DiagnosisTask(aspSolver,this).execute();

			return;
		}
		else if(source.equals(jButtonFilter)){

			Set<LightEdge> filtered = null;

			if(!localFilters.containsKey(selectedSCC) 
					&& (hasDiagnosis(selectedSCC) || hasDeletions(selectedSCC))){
				//				if(!localDiagnoses.get(selectedSCC).isEmpty() || 
				//						!localDeletions.get(selectedSCC).isEmpty()){
				int dialogResult = JOptionPane.showConfirmDialog(this, 
						"All the removed mappings for this SCC will be " +
								"reset, do you want to continue?",
								"Continue?",JOptionPane.YES_NO_OPTION);
				if(dialogResult == JOptionPane.NO_OPTION)
					return;
				//}
				// invalidate diagnosis and manual deletions
				retractDiagnosis(selectedSCC);
				retractDeletions(selectedSCC);
			}

			if(localFilters.containsKey(selectedSCC)){
				filtered = localFilters.get(selectedSCC);
				adj.retractRemovedMappings(localFilters.get(selectedSCC));
				localFilters.remove(selectedSCC);
				graph.markAsDeleted(filtered, mySCCGraph.mappingStyle);
				if(!jButtonDiag.isEnabled())
					jButtonDiag.setEnabled(true);
			}
			else {
				Diagnosis sealedMappings = new Diagnosis(
						listMappings.getCheckedElems());
				Set<LightEdge> clashMappings = 
						sealedMappings.getMultipleMappings();  
				if(clashMappings.size() > 0){
					JOptionPane.showMessageDialog(this, clashMappings.size() + 
							" invalid sealed mappings for 1-1 filtering, " +
							"remove one or more mapping sharing the same " +
							"source or target vertex:\n" + 
							clashMappings.toString().replaceAll(",", ",\n"),
							"Invalid Sealed Mappings",
							JOptionPane.WARNING_MESSAGE);
					return;
				}

				filtered = adj.filterMultipleCorrespondences(selectedSCC, true);
				Set<LightEdge> intersection = Util.computeIntersection(
						sealedMappings, filtered);

				if(!intersection.isEmpty()){
					Set<LightEdge> keptMappings = selectedSCC.extractMappings(
							adj, true);
					// we update the filtered set
					adj.retractRemovedMappings(filtered);
					filtered.removeAll(sealedMappings);

					for (LightEdge k : keptMappings) {
						for (LightEdge i : intersection) {
							if(k.to.equals(i.to) || k.from.equals(i.from)){
								filtered.add(k);
								break;
							}
						}
					}
					adj.removeMappings(filtered);
				}

				localFilters.put(selectedSCC, new Diagnosis(filtered));
				if(Params.verbosity > 0){
					System.out.println("Filtered mappings: " + filtered);
					selectedSCC.printProblematicSCC(adj);
				}

				// check if it is still problematic after filtering
				if(!selectedSCC.isProblematic(adj)){
					JOptionPane.showMessageDialog(this, 
							"The SCC is NOT problematic after filtering.",
							"Filtering solved!",
							JOptionPane.INFORMATION_MESSAGE);
					if(Params.verbosity > 0)
						System.out.println("The SCC is NOT problematic " +
								"after filtering");

					// we insert an empty diagnosis and disable diag button
					localDiagnoses.put(selectedSCC, new Diagnosis());
					jButtonDiag.setEnabled(false);
				}

				graph.markAsDeleted(filtered, mySCCGraph.filteredStyle);
			}

			selectedSCC = listSCCs.getSelectedValue();

			refreshGraphAndMappingList();
		}
		else if(source.equals(jButtonPreferences)){
			prefDialog.setVisible(true);
		}
		else if(source.equals(jButtonIsSafe)){
			boolean probl = !selectedSCC.isProblematic(adj);
			JOptionPane.showMessageDialog(this, 
					"The SCC is" + (probl ? " NOT" : "") + " problematic.",
					"SCC status", !probl ? JOptionPane.WARNING_MESSAGE 
							: JOptionPane.INFORMATION_MESSAGE);
			if(Params.verbosity > 0)
				System.out.println("The SCC is" + (probl ? " NOT" : "") 
						+ " problematic.");
		}
		else if(source.equals(jButtonResetSCC)){
			int dialogResult = JOptionPane.showConfirmDialog(this, 
					"All the removed mappings for this SCC will be " +
							"reset, do you want to continue?",
							"Continue?",JOptionPane.YES_NO_OPTION);
			if(dialogResult == JOptionPane.NO_OPTION)
				return;

			retractAll(selectedSCC);
		}
		else if(source.equals(jButtonIndiv)){
			new InstancesDialog(this);
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
		listSCCs.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		//listSCCs.setLayoutOrientation(JList.VERTICAL);
		listSCCs.setVisibleRowCount(-1);

		listScroller = new JScrollPane(listSCCs);
		//listScroller.setPreferredSize(new Dimension(80, 250));
		listScroller.setMinimumSize(new Dimension(0, 0));

		//		listMappings.setSelectionMode(
		//				ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		listMappings.setVisibleRowCount(-1);
		//listMappings.setCellRenderer(new MappingsCellRenderer(this));
		listSCCs.setCellRenderer(new SCCsCellRenderer(this));
		listSCCs.addListSelectionListener(this);

		mapListScroller = new JScrollPane(listMappings);
		mapListScroller.setPreferredSize(new Dimension(80, 250));
		mapListScroller.setMinimumSize(new Dimension(0, 0));
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

	public boolean isFiltered(LightSCC scc, LightEdge m) {
		if(localFilters.containsKey(scc))
			if(localFilters.get(scc).contains(m))
				return true;
		return false;
	}
	public boolean isManuallyRemoved(LightSCC scc, LightEdge m){
		if(localDeletions.containsKey(scc))
			if(localDeletions.get(scc).contains(m))
				return true;
		return false;
	}
	public boolean isRemoved(LightSCC scc, LightEdge m){
		if(localDiagnoses.containsKey(scc))
			if(localDiagnoses.get(scc).contains(m))
				return true;
		return false;
	}
	public boolean isDeleted(LightSCC scc, LightEdge m) {
		return isManuallyRemoved(scc, m) || isRemoved(scc, m) || isFiltered(scc, m);
	}
	public boolean isSealed(LightSCC scc, LightEdge m){
		return sealedMappings.containsKey(scc) 
				&& sealedMappings.get(scc).contains(m);
	}

	public boolean hasDeletions(LightSCC scc){
		return localDeletions.containsKey(scc)
				&& !localDeletions.get(scc).isEmpty();
	}
	public boolean hasDiagnosis(LightSCC scc){
		return localDiagnoses.containsKey(scc) 
				&& !localDiagnoses.get(scc).isEmpty();
	}
	public boolean hasFiltering(LightSCC scc){
		return localFilters.containsKey(scc) 
				&& !localFilters.get(scc).isEmpty();
	}
	public boolean isModified(LightSCC scc) {
		return hasDiagnosis(scc) || hasFiltering(scc) || hasDeletions(scc);
	}

	public LightSCC getSelectedSCC() {
		return selectedSCC;
	}

	//	@Override
	//	public void propertyChange(PropertyChangeEvent evt) {		
	//		if ("state".equals(evt.getPropertyName())) {
	//			if(StateValue.DONE == ((StateValue)evt.getNewValue())){
	//				setComputingStatus();
	//				graphTask = new GraphBuilderTask();
	//				graphTask.addPropertyChangeListener(this);
	//				graphTask.execute();
	//			}
	//		}
	//	}
	//
	//	public void workerRepainter() {
	//		graphTask = new GraphBuilderTask();
	//		graphTask.addPropertyChangeListener(this);
	//		graphTask.execute();
	//	}
}