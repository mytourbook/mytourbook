/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartXSlider;
import net.tourbook.chart.IChartContextProvider;
import net.tourbook.data.TourMarker;
import net.tourbook.ui.tourChart.action.ActionCreateMarker;
import net.tourbook.ui.tourChart.action.IMarkerReceiver;

import org.eclipse.jface.action.IMenuManager;

class DialogMarkerTourChartContextProvicer implements IChartContextProvider, IMarkerReceiver {

	private final DialogMarker	fMarkerDialog;

	private ActionCreateMarker	fActionCreateMarker;
	private ActionCreateMarker	fActionCreateMarkerLeft;
	private ActionCreateMarker	fActionCreateMarkerRight;

	private ChartXSlider		fLeftSlider;
	private ChartXSlider		fRightSlider;

	/**
	 * @param markerDialog
	 */
	DialogMarkerTourChartContextProvicer(final DialogMarker markerDialog) {

		fMarkerDialog = markerDialog;

		fActionCreateMarker = new ActionCreateMarker(this, Messages.tourCatalog_view_action_create_marker, true);

		fActionCreateMarkerLeft = new ActionCreateMarker(this,
				Messages.tourCatalog_view_action_create_left_marker,
				true);

		fActionCreateMarkerRight = new ActionCreateMarker(this,
				Messages.tourCatalog_view_action_create_right_marker,
				false);

		fActionCreateMarker.setMarkerReceiver(this);
		fActionCreateMarkerLeft.setMarkerReceiver(this);
		fActionCreateMarkerRight.setMarkerReceiver(this);
	}

	public void addTourMarker(final TourMarker tourMarker) {
		fMarkerDialog.addTourMarker(tourMarker);
	}

	public void fillBarChartContextMenu(final IMenuManager menuMgr,
										final int hoveredBarSerieIndex,
										final int hoveredBarValueIndex) {}

	public void fillContextMenu(final IMenuManager menuMgr,
								final int mouseDownDevPositionX,
								final int mouseDownDevPositionY) {}

	public void fillXSliderContextMenu(	final IMenuManager menuMgr,
										final ChartXSlider leftSlider,
										final ChartXSlider rightSlider) {

		fLeftSlider = leftSlider;
		fRightSlider = rightSlider;

		if (leftSlider != null || rightSlider != null) {

			// marker actions
			if (leftSlider != null && rightSlider == null) {
				menuMgr.add(fActionCreateMarker);
			} else {
				menuMgr.add(fActionCreateMarkerLeft);
				menuMgr.add(fActionCreateMarkerRight);
			}
		}
	}

	public Chart getChart() {
		return fMarkerDialog.getTourChart();
	}

	public ChartXSlider getLeftSlider() {
		return fLeftSlider;
	}

	public ChartXSlider getRightSlider() {
		return fRightSlider;
	}

}
