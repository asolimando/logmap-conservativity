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

import scc.graphDataStructure.LightEdge;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;

public class JCheckBoxLightEdge extends JCheckBox {

	private static final long serialVersionUID = 2534315383546789055L;
	public static Icon locked = new ImageIcon(((
			new ImageIcon("img/lock_closed.png")).getImage()).getScaledInstance(
					30, 30, java.awt.Image.SCALE_SMOOTH)), 
			unlocked = new ImageIcon(((new ImageIcon("img/lock_open.png")).
					getImage()).getScaledInstance(30, 30, java.awt.Image.SCALE_SMOOTH));	
	
	private LightEdge mapping;
	
	public JCheckBoxLightEdge(LightEdge mapping){
		this.mapping = mapping;
		this.setIcon(unlocked);
	}
	
	public LightEdge getElement(){
		return mapping;
	}
	
	public String toString(){
		return getElement().toString();
	}
	
	
}
