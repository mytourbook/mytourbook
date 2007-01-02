/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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
package net.tourbook.views.tourMap;

import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.jface.action.Action;

public class ActionSynchChartHorizontal extends Action {

	private SynchedChartView	resultView;

	public ActionSynchChartHorizontal(SynchedChartView resultView) {

		super(null, AS_CHECK_BOX);

		this.resultView = resultView;

		setToolTipText("Keep the marked part of the compared tour in synch with the reference tour chart");
		setImageDescriptor(TourbookPlugin
				.getImageDescriptor("synch-graph-horizontal.gif"));
	}

	public void run() {
		resultView.synchCharts(isChecked());
	}
}
