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

import scc.exception.UnsatisfiableProblemException;
import scc.graphDataStructure.Diagnosis;
import graphViz.myConservativityGraph;

import java.util.concurrent.TimeoutException;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import util.Params;
import scc.algoSolver.ASPSolver;

public class DiagnosisTask extends SwingWorker<Diagnosis, Void>{

	private ASPSolver aspSolver;
	private Diagnosis d;
	private VisualDebugger vd;

	public DiagnosisTask(ASPSolver aspSolver, VisualDebugger vd) {
		this.aspSolver = aspSolver;
		this.vd = vd;
	}

	@Override
	public Diagnosis doInBackground() {
		try {
			d = aspSolver.computeDiagnosis();

			if(Params.verbosity > 0)
				System.out.println("Diagnosis: " + d);

		} catch (InterruptedException | TimeoutException e1) {
			e1.printStackTrace();
			System.exit(1);
		} catch (UnsatisfiableProblemException e1) {
			JOptionPane.showMessageDialog(vd,
					"The problem is unsatisfiable with the current set " +
							"of sealed mappings.",
							e1.getMessage(),
							JOptionPane.WARNING_MESSAGE);
			System.err.println(e1.getMessage());
			//printDiagnosisOnLabel(null);
		}
		return d;
	}

	/*
	 * Executed in event dispatch thread
	 */
	public void done() {
		vd.setComputingStatus(false);
		if(d != null){
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					//printDiagnosisOnLabel(d);
					if(!d.isEmpty()){
						vd.graph.markAsDeleted(d,myConservativityGraph.deletedStyle);
						vd.localDiagnoses.put(vd.getSelectedSCC(), d);
						vd.jButtonRetractDiagnosis.setEnabled(true);
						vd.getAdjancencyList().removeMappings(d);
						vd.refreshGraphAndMappingList();
					}
				}
			});
			vd.jButtonDiag.setEnabled(false);
			vd.jButtonRetractDiagnosis.setEnabled(true);
		}
	}
} 
