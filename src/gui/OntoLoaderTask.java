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

import scc.exception.ClassificationTimeoutException;
import scc.graphDataStructure.LightAdjacencyList;

import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import repair.ConservativityRepairFacility;

import util.OntoUtil;


public class OntoLoaderTask extends SwingWorker<Void, Void> {

	private VisualConservativity vc;
	
	public OntoLoaderTask(VisualConservativity vc){
		this.vc = vc;
	}
	
	@Override
	public Void doInBackground() throws OWLOntologyCreationException, 
		ClassificationTimeoutException {
		OWLOntologyManager manager = OntoUtil.getManager(false);
		OntoUtil.unloadAllOntologies(manager);
		manager = OntoUtil.getManager(true);
		try {
			vc.fstO = OntoUtil.load(vc.srcOnto.getPath(), true, manager);
			vc.sndO = OntoUtil.load(vc.trgOnto.getPath(), true, manager);
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
		
		vc.setRepairFacility(new ConservativityRepairFacility(
				vc.fstO, vc.sndO, manager, vc.mapping.getPath()));

		vc.setAdjancencyList(
				new LightAdjacencyList(vc.fstO, vc.sndO, null,false,
						vc.getFirstReasoner(),vc.getSecondReasoner()));
		
		vc.getAdjancencyList().loadMappings(vc.mapping, 
				vc.getRepairFacility().getOriginalMappings());
		
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

		vc.resetListAndPanel(true);
		vc.showViols(vc.getViolations());

		vc.setComputingStatus(false);
		vc.statusLabel.setText("Analysis completed");
		vc.jLabelFilesSelected.setText(" - " + vc.getViolations().size() + 
				" violation(s) detected" + vc.jLabelFilesSelected.getText());
		vc.setViolButtons(false);
	}
}

