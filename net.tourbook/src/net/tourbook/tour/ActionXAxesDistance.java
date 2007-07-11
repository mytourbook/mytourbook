/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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
package net.tourbook.tour;

import net.tourbook.Messages;
import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.jface.action.Action;

public class ActionXAxesDistance extends Action {

	private TourChart	tourChart;

	public ActionXAxesDistance(TourChart tourChart) {

		super(Messages.Tour_Action_show_distance_on_x_axis, AS_RADIO_BUTTON);

		this.tourChart = tourChart;

		setToolTipText(Messages.Tour_Action_show_distance_on_x_axis_tooltip);

		setChecked(!tourChart.fTourChartConfig.showTimeOnXAxis);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image_show_distance_on_x_axis));
	}

	public void run() {

		if (isChecked()) {

			// show distance on x axes

			TourChartConfiguration chartConfig = tourChart.fTourChartConfig;
			
			chartConfig.showTimeOnXAxis = !chartConfig.showTimeOnXAxis;
			chartConfig.showTimeOnXAxisBackup = chartConfig.showTimeOnXAxis;

			tourChart.switchSlidersTo2ndXData();
			tourChart.updateChart(true);

			tourChart.fActionOptions.actionStartTimeOption.setEnabled(false);

		} else {
			// this action got unchecked
			tourChart.fActionOptions.actionStartTimeOption.setEnabled(true);
		}
	}
}
