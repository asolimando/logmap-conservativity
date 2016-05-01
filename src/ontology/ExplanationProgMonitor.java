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
package ontology;

import java.util.Set;

import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owl.explanation.api.ExplanationGenerator;
import org.semanticweb.owl.explanation.api.ExplanationProgressMonitor;
import org.semanticweb.owlapi.model.OWLAxiom;
import util.FileUtil;
import util.Util;

public class ExplanationProgMonitor implements ExplanationProgressMonitor<OWLAxiom> {

	private int found, maxExpl, printEach = 10;
	private boolean suppressOutput;
	private boolean stop;

	public ExplanationProgMonitor() {
		this.suppressOutput = false;
	}

	public ExplanationProgMonitor(int maxExpl) {
		this();
		this.maxExpl = maxExpl;
	}

	public ExplanationProgMonitor(int maxExpl, boolean suppressOutput) {
		this.suppressOutput = suppressOutput;
		this.maxExpl = maxExpl;
	}

	public ExplanationProgMonitor(boolean suppressOutput) {
		this.suppressOutput = suppressOutput;
	}

	public ExplanationProgMonitor(int maxExpl, boolean suppressOutput, int printEach) {
		this.suppressOutput = suppressOutput;
		this.maxExpl = maxExpl;
		this.printEach = printEach;
	}

	public ExplanationProgMonitor(int maxExpl, int printEach) {
		this.maxExpl = maxExpl;
		this.printEach = printEach;
	}

	public ExplanationProgMonitor(boolean suppressOutput, int printEach) {
		this.suppressOutput = suppressOutput;
		this.printEach = printEach;
	}

	@Override
	public boolean isCancelled() {
		return stop;
	}

	@Override
	public void foundExplanation(ExplanationGenerator<OWLAxiom> generator,
			Explanation<OWLAxiom> explanation,
			Set<Explanation<OWLAxiom>> allFoundExplanations) {
		++found;
		if(found % printEach == 0 && !suppressOutput)
			FileUtil.writeLogAndConsole("Found " + found + 
					" explanation(s): " + Util.getCurrTime());
		if(found == maxExpl)
			stop = true;
	}

	public void resetCounter(){
		found = 0;
	}

	public void cancel() {
		stop = true;
	}
}
