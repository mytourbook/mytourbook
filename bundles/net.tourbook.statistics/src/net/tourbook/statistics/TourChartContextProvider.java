/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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
/**
 * 
 */
package net.tourbook.statistics;

import java.util.ArrayList;

import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartXSlider;
import net.tourbook.chart.IChartContextProvider;
import net.tourbook.data.TourData;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.ui.action.ActionEditTour;
import net.tourbook.ui.action.ActionOpenTour;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;

/**
 * provides the fill menu methods for the chart context menu
 */
class TourChartContextProvider implements IChartContextProvider, ITourProvider {

	private final Chart					_chart;
	private final IBarSelectionProvider	_barSelectionProvider;

	private final ActionEditQuick		_actionEditQuick;
	private final ActionEditTour		_actionEditTour;
	private final ActionOpenTour		_actionOpenTour;

	public TourChartContextProvider(final Chart chart, final IBarSelectionProvider barSelectionProvider) {

		_chart = chart;
		_barSelectionProvider = barSelectionProvider;

		_actionEditQuick = new ActionEditQuick(this);
		_actionEditTour = new ActionEditTour(this);
		_actionOpenTour = new ActionOpenTour(this);
	}

	private void enableActions(final boolean isTourHovered) {

		_actionEditQuick.setEnabled(isTourHovered);
		_actionEditTour.setEnabled(isTourHovered);
		_actionOpenTour.setEnabled(isTourHovered);
	}

	public void fillBarChartContextMenu(final IMenuManager menuMgr,
										final int hoveredBarSerieIndex,
										final int hoveredBarValueIndex) {

		menuMgr.add(_actionEditQuick);
		menuMgr.add(_actionEditTour);
		menuMgr.add(_actionOpenTour);

		enableActions(hoveredBarSerieIndex != -1);
	}

	public void fillContextMenu(final IMenuManager menuMgr,
								final int mouseDownDevPositionX,
								final int mouseDownDevPositionY) {}

	public void fillXSliderContextMenu(	final IMenuManager menuMgr,
										final ChartXSlider leftSlider,
										final ChartXSlider rightSlider) {}

	public Chart getChart() {
		return _chart;
	}

	public ChartXSlider getLeftSlider() {
		return null;
	}

	public ChartXSlider getRightSlider() {
		return null;
	}

	public ArrayList<TourData> getSelectedTours() {

		final Long selectedTourId = _barSelectionProvider.getSelectedTourId();
		if (selectedTourId != null) {

			final TourData tourData = TourManager.getInstance().getTourData(selectedTourId);
			if (tourData != null) {

				final ArrayList<TourData> selectedTourData = new ArrayList<TourData>();
				selectedTourData.add(tourData);

				return selectedTourData;
			}
		}

		return null;
	}

	@Override
	public void onHideContextMenu(final MenuEvent menuEvent, final Control menuParentControl) {}

	@Override
	public void onShowContextMenu(final MenuEvent menuEvent, final Control menuParentControl) {}

	public boolean showOnlySliderContextMenu() {
		return false;
	}
}
