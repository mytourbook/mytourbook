/*******************************************************************************
 * Copyright (C) 2005, 20011  Wolfgang Schramm and Contributors
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
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;

class DialogMarkerTourChartContextProvicer implements IChartContextProvider, IMarkerReceiver {

	private final DialogMarker	_markerDialog;

	private ActionCreateMarker	_actionCreateMarker;
	private ActionCreateMarker	_actionCreateMarkerLeft;
	private ActionCreateMarker	_actionCreateMarkerRight;

	private ChartXSlider		_leftSlider;
	private ChartXSlider		_rightSlider;

	/**
	 * @param markerDialog
	 */
	DialogMarkerTourChartContextProvicer(final DialogMarker markerDialog) {

		_markerDialog = markerDialog;

		_actionCreateMarker = new ActionCreateMarker(this, Messages.tourCatalog_view_action_create_marker, true);

		_actionCreateMarkerLeft = new ActionCreateMarker(
				this,
				Messages.tourCatalog_view_action_create_left_marker,
				true);

		_actionCreateMarkerRight = new ActionCreateMarker(
				this,
				Messages.tourCatalog_view_action_create_right_marker,
				false);

		_actionCreateMarker.setMarkerReceiver(this);
		_actionCreateMarkerLeft.setMarkerReceiver(this);
		_actionCreateMarkerRight.setMarkerReceiver(this);
	}

	public void addTourMarker(final TourMarker tourMarker) {
		_markerDialog.addTourMarker(tourMarker);
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

		_leftSlider = leftSlider;
		_rightSlider = rightSlider;

		if (leftSlider != null || rightSlider != null) {

			// marker actions
			if (leftSlider != null && rightSlider == null) {
				menuMgr.add(_actionCreateMarker);
			} else {
				menuMgr.add(_actionCreateMarkerLeft);
				menuMgr.add(_actionCreateMarkerRight);
			}
		}
	}

	public Chart getChart() {
		return _markerDialog.getTourChart();
	}

	public ChartXSlider getLeftSlider() {
		return _leftSlider;
	}

	public ChartXSlider getRightSlider() {
		return _rightSlider;
	}

	@Override
	public void onHideContextMenu(final MenuEvent menuEvent, final Control menuParentControl) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onShowContextMenu(final MenuEvent menuEvent, final Control menuParentControl) {
		// TODO Auto-generated method stub

	}

	public boolean showOnlySliderContextMenu() {
		return false;
	}

}
