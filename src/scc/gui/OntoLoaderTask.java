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

import scc.exception.ClassificationTimeoutException;
import scc.graphDataStructure.LightAdjacencyList;

import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import util.OntoUtil;
import util.Params;

public class OntoLoaderTask extends SwingWorker<Void, Void> {

	private VisualDebugger vd;
	
	public OntoLoaderTask(VisualDebugger vd){
		this.vd = vd;
	}
	
	@Override
	public Void doInBackground() throws OWLOntologyCreationException, 
		ClassificationTimeoutException {
		LightAdjacencyList adj = null;
		try {
			OntoUtil.unloadAllOntologies();
			vd.setAdjancencyList(
					adj = new LightAdjacencyList(
							vd.srcOnto,vd.trgOnto,null,true,true,true));
		} catch (OWLOntologyCreationException e1) {
			e1.printStackTrace();
			throw new Error("Error while processing selected ontologies.");
		}
		adj.loadMappings(vd.mapping, null, Params.fullDetection);
		adj.computeGlobalSCCsAndProblematicMappings(vd.problematicSCCs, null);
		return null;
	}

	/*
	 * Executed in event dispatch thread
	 */
	public void done() {
		try {
			get();
		} catch (InterruptedException | ExecutionException e1) {
			e1.printStackTrace();
			System.exit(1);
		}

		vd.resetListAndPanel();
		vd.showSCCs(vd.problematicSCCs);

		vd.setComputingStatus(false);
		vd.statusLabel.setText("Analysis completed");
		vd.jLabelFilesSelected.setText(" - " + vd.problematicSCCs.size() + 
				" SCCs detected" + vd.jLabelFilesSelected.getText());
		vd.jButtonSaveMapping.setEnabled(true);
		vd.setSCCButtons(false);
		vd.clearDiagnosesFilters();
	}
}

