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
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;

public class ActionChartOptions extends Action implements IMenuCreator {

	private Menu				fMenu	= null;

	ActionStartTimeOption		fActionShowStartTime;
	ActionCanScrollZoomedChart	fActionCanScrollZoomedChart;
	ActionCanAutoZoomToSlider	fActionCanAutoZoomToSlider;

	private TourChart			fTourChart;

	private ToolBarManager		fTBM;

	class ActionStartTimeOption extends Action {

		public ActionStartTimeOption() {
			super(Messages.Tour_Action_show_start_time_on_x_axis, AS_CHECK_BOX);
		}

		public void run() {
			fTourChart.fTourChartConfig.isStartTime = isChecked();
			fTourChart.updateTourChart(true);
		}
	}

	class ActionCanScrollZoomedChart extends Action {

		public ActionCanScrollZoomedChart() {
			super(Messages.Tour_Action_scroll_zoomed_chart, AS_CHECK_BOX);
		}

		public void run() {

			fTourChart.setCanScrollZoomedChart(isChecked());

			// update the chart
			if (isChecked()) {
				fTourChart.zoomInWithSlider();
			} else {
				fTourChart.zoomOut(true);
			}

			updateZoomOptions();
		}
	}

	class ActionCanAutoZoomToSlider extends Action {

		public ActionCanAutoZoomToSlider() {
			super(Messages.Tour_Action_auto_zoom_to_slider_position, AS_CHECK_BOX);
		}

		public void run() {

			fTourChart.setCanAutoZoomToSlider(isChecked());

			// update the chart
			if (isChecked()) {
				fTourChart.zoomInWithSlider();
			} else {
				fTourChart.zoomOut(true);
			}

			updateZoomOptions();
		}
	}

	public ActionChartOptions(TourChart tourChart, ToolBarManager tbm) {

		super(null, Action.AS_DROP_DOWN_MENU);

		this.fTourChart = tourChart;
		fTBM = tbm;

		setToolTipText(Messages.Tour_Action_chart_options_tooltip);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image_chart_options));

		fActionShowStartTime = new ActionStartTimeOption();
		fActionCanScrollZoomedChart = new ActionCanScrollZoomedChart();
		fActionCanAutoZoomToSlider = new ActionCanAutoZoomToSlider();

		setMenuCreator(this);
	}

	public void runWithEvent(Event event) {

		// show the drop-down menu, this only works in the runWithEvent not in the run method
		getMenuCreator().getMenu(fTBM.getControl()).setVisible(true);
	}

	public void dispose() {
		if (fMenu != null) {
			fMenu.dispose();
			fMenu = null;
		}
	}

	public Menu getMenu(Control parent) {

		fMenu = new Menu(parent);

		addItem(fActionShowStartTime);
		(new Separator()).fill(fMenu, -1);

		addItem(fActionCanScrollZoomedChart);
		addItem(fActionCanAutoZoomToSlider);

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
	private void updateZoomOptions() {
		fActionCanScrollZoomedChart.setChecked(fTourChart.getCanScrollZoomedChart());
		fActionCanAutoZoomToSlider.setChecked(fTourChart.getCanAutoZoomToSlider());
	}
}
