/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
 *  
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software 
 * Foundation version 2 of the License.
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with 
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA    
 *******************************************************************************/
package net.tourbook.chart;

import org.eclipse.jface.action.Action;

public class ActionMouseMode extends Action {

	private Chart	fChart;

	public ActionMouseMode(final Chart chart) {

		super(Messages.Action_toggle_mouse, Action.AS_CHECK_BOX);
		
		fChart = chart;

		setToolTipText(Messages.Action_toggle_mouse_tooltip);
		setImageDescriptor(Activator.getImageDescriptor(Messages.Image_toggle_mouse));
	}

	@Override
	public void run() {
		fChart.onExecuteMouseMode(isChecked());
	}

}
