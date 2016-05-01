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
package scc.graphViz;

import java.awt.Container;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

public class MouseHandler {
	public MouseHandler(final mxGraphComponent graphComponent, final Container top)
	{
		graphComponent.getGraphControl().addMouseListener(new MouseAdapter()
		{

			public void mouseReleased(MouseEvent e)
			{
				Object cell = graphComponent.getCellAt(e.getX(), e.getY());

				if (cell != null)
				{
					mxGraph graph = graphComponent.getGraph();
					//System.out.println(((mxCell) cell).getId() + " " +cell + " = " + graph.getLabel(cell));
				}
			}
		});
	}
}
