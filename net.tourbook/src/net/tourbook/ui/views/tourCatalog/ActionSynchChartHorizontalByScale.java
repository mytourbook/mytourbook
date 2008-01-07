/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourCatalog;

import net.tourbook.Messages;
import net.tourbook.chart.Chart;
import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.jface.action.Action;

public class ActionSynchChartHorizontalByScale extends Action {

	private ISynchedChart	synchChart;

	public ActionSynchChartHorizontalByScale(ISynchedChart resultView) {

		super(null, AS_CHECK_BOX);

		this.synchChart = resultView;

		setToolTipText(Messages.tourCatalog_view_action_synch_charts_byScale_tooltip);

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__synch_graph_byScale));
		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__synch_graph_byScale_disabled));
	}

	@Override
	public void run() {
		synchChart.synchCharts(isChecked(), Chart.SYNCH_MODE_BY_SCALE);
	}
}
