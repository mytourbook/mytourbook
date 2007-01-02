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
package net.tourbook.tour;

import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

public class ActionChartOptions extends Action implements IMenuCreator {

	private Menu				fMenu	= null;

	ActionStartTimeOption		actionStartTimeOption;
	ActionCanScrollZoomedChart	actionCanScrollZoomedChart;
	ActionCanAutoZoomToSlider	actionCanAutoZoomToSlider;

	private TourChart			tourChart;

	class ActionStartTimeOption extends Action {

		public ActionStartTimeOption() {
			super("Show Starttime on X-Axis", AS_CHECK_BOX);
		}

		public void run() {
			tourChart.fTourChartConfig.isStartTime = isChecked();
			tourChart.updateChart();
		}
	}

	class ActionCanScrollZoomedChart extends Action {

		public ActionCanScrollZoomedChart() {
			super("Zoomed Chart can be scrolled", AS_CHECK_BOX);
		}

		public void run() {

			tourChart.setCanScrollZoomedChart(isChecked());

			// update the chart
			if (isChecked()) {
				tourChart.zoomInWithSlider();
			} else {
				tourChart.zoomOut(true);
			}

			updateActionsZoomOptions();
		}
	}

	class ActionCanAutoZoomToSlider extends Action {

		public ActionCanAutoZoomToSlider() {
			super("Auto-Zoom to Slider Position", AS_CHECK_BOX);
		}

		public void run() {

			tourChart.setCanAutoZoomToSlider(isChecked());

			// update the chart
			if (isChecked()) {
				tourChart.zoomInWithSlider();
			} else {
				tourChart.zoomOut(true);
			}

			updateActionsZoomOptions();
		}
	}

	public ActionChartOptions(TourChart tourChart) {

		this.tourChart = tourChart;

		setToolTipText("Chart Options");
		setImageDescriptor(TourbookPlugin.getImageDescriptor("options.gif"));

		actionStartTimeOption = new ActionStartTimeOption();
		actionCanScrollZoomedChart = new ActionCanScrollZoomedChart();
		actionCanAutoZoomToSlider = new ActionCanAutoZoomToSlider();

		setMenuCreator(this);
	}

	public void run() {}

	public void dispose() {
		if (fMenu != null) {
			fMenu.dispose();
			fMenu = null;
		}
	}

	public Menu getMenu(Control parent) {

		if (fMenu == null) {
			fMenu = new Menu(parent);

			addItem(actionStartTimeOption);
			(new Separator()).fill(fMenu, -1);

			addItem(actionCanScrollZoomedChart);
			addItem(actionCanAutoZoomToSlider);
		}
		return fMenu;
	}

	private void addItem(Action action) {
		ActionContributionItem item = new ActionContributionItem(action);
		item.fill(fMenu, -1);
	}

	public Menu getMenu(Menu parent) {
		return null;
	}

	/**
	 * the chart decides if the scroll/auto zoom options are available
	 */
	private void updateActionsZoomOptions() {
		actionCanScrollZoomedChart.setChecked(tourChart.getCanScrollZoomedChart());
		actionCanAutoZoomToSlider.setChecked(tourChart.getCanAutoZoomToSlider());
	}
}
