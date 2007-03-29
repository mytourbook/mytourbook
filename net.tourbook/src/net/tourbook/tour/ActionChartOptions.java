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
import org.eclipse.swt.widgets.ToolBar;

public class ActionChartOptions extends Action implements IMenuCreator {

	private Menu				fMenu	= null;

	ActionStartTimeOption		actionStartTimeOption;
	ActionCanScrollZoomedChart	actionCanScrollZoomedChart;
	ActionCanAutoZoomToSlider	actionCanAutoZoomToSlider;

	private TourChart			tourChart;

	private ToolBarManager	fTBM;

	class ActionStartTimeOption extends Action {

		public ActionStartTimeOption() {
			super(Messages.Tour_Action_show_start_time_on_x_axis, AS_CHECK_BOX);
		}

		public void run() {
			tourChart.fTourChartConfig.isStartTime = isChecked();
			tourChart.updateChart();
		}
	}

	class ActionCanScrollZoomedChart extends Action {

		public ActionCanScrollZoomedChart() {
			super(Messages.Tour_Action_scroll_zoomed_chart, AS_CHECK_BOX);
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
			super(Messages.Tour_Action_auto_zoom_to_slider_position, AS_CHECK_BOX);
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

	public ActionChartOptions(TourChart tourChart, ToolBarManager tbm) {

		super(null, Action.AS_DROP_DOWN_MENU);
		
		this.tourChart = tourChart;
		fTBM=tbm;

		setToolTipText(Messages.Tour_Action_chart_options_tooltip);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image_chart_options));

		actionStartTimeOption = new ActionStartTimeOption();
		actionCanScrollZoomedChart = new ActionCanScrollZoomedChart();
		actionCanAutoZoomToSlider = new ActionCanAutoZoomToSlider();

		setMenuCreator(this);
	}

	public void runWithEvent(Event event) {
		
		ToolBar tb = fTBM.getControl();
		Menu ddMenu = getMenuCreator().getMenu(tb);
		
//		tb.getItem(0).getControl();
//		
//		for (ToolItem toolItem : tb.getItems()) {
//			toolItem.get
//		}
		
		// position drop down menu
//        Rectangle rect = tb.getItem(0).getBounds();
//        Point pt = new Point(rect.x, rect.y + rect.height);
//        pt = tb.toDisplay(pt);
//        ddMenu.setLocation(pt.x, pt.y);

		// show the drop-down menu, this only works in the runWithEvent not in the run method
		ddMenu.setVisible(true);
		
        
//		Widget wi = event.widget;
////        Menu m = getMenu(event.widget);
////        if (m != null) {
////            // position the menu below the drop down item
////            Rectangle b = ti.getBounds();
////            Point p = ti.getParent().toDisplay(
////                    new Point(b.x, b.y + b.height));
////            m.setLocation(p.x, p.y); // waiting for SWT 0.42
////            m.setVisible(true);
////            return; // we don't fire the action
////        }
//		run(event);
	}

	public void dispose() {
		if (fMenu != null) {
			fMenu.dispose();
			fMenu = null;
		}
	}

	public Menu getMenu(Control parent) {

//		if (fMenu == null) {
			fMenu = new Menu(parent);

			addItem(actionStartTimeOption);
			(new Separator()).fill(fMenu, -1);

			addItem(actionCanScrollZoomedChart);
			addItem(actionCanAutoZoomToSlider);
//		}
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
