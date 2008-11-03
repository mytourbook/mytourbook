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

package net.tourbook.ui.tourChart;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartXSlider;
import net.tourbook.chart.IChartContextProvider;
import net.tourbook.data.TourData;
import net.tourbook.tour.ActionEditTourMarker;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.ui.action.ActionEditTour;
import net.tourbook.ui.action.ActionOpenTour;
import net.tourbook.ui.tourChart.action.ActionCreateMarker;
import net.tourbook.ui.tourChart.action.ActionCreateRefTour;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;

class TourChartViewContextProvicer implements IChartContextProvider, ITourProvider {

	private final TourChartView		fTourChartView;

	private ActionEditQuick			fActionQuickEdit;
	private ActionEditTour			fActionEditTour;
	private ActionOpenTour			fActionOpenTour;

	private ActionCreateRefTour		fActionCreateRefTour;
	private ActionCreateMarker		fActionCreateMarker;
	private ActionCreateMarker		fActionCreateMarkerLeft;
	private ActionCreateMarker		fActionCreateMarkerRight;
	private ActionEditTourMarker	fActionEditTourMarkers;

	private ChartXSlider			fLeftSlider;

	private ChartXSlider			fRightSlider;

	/**
	 * @param tourChartView
	 */
	TourChartViewContextProvicer(final TourChartView tourChartView) {

		fTourChartView = tourChartView;

		fActionQuickEdit = new ActionEditQuick(fTourChartView);
		fActionEditTour = new ActionEditTour(fTourChartView);
		fActionOpenTour = new ActionOpenTour(fTourChartView);

		final TourChart tourChart = fTourChartView.getTourChart();

		fActionCreateRefTour = new ActionCreateRefTour(tourChart);

		fActionCreateMarker = new ActionCreateMarker(this, //
				Messages.tourCatalog_view_action_create_marker,
				true);
		
		fActionCreateMarkerLeft = new ActionCreateMarker(this,
				Messages.tourCatalog_view_action_create_left_marker,
				true);

		fActionCreateMarkerRight = new ActionCreateMarker(this,
				Messages.tourCatalog_view_action_create_right_marker,
				false);

		fActionEditTourMarkers = new ActionEditTourMarker(this, true);
		fActionEditTourMarkers.setEnabled(true);
	}

	public void fillBarChartContextMenu(final IMenuManager menuMgr,
										final int hoveredBarSerieIndex,
										final int hoveredBarValueIndex) {}

	public void fillContextMenu(final IMenuManager menuMgr) {

		menuMgr.add(fActionQuickEdit);
		menuMgr.add(fActionEditTour);
		menuMgr.add(fActionOpenTour);

		menuMgr.add(new Separator());
		menuMgr.add(fActionEditTourMarkers);

		/*
		 * enable actions
		 */
		final boolean isDataAvailable = fTourChartView.fTourData != null
				&& fTourChartView.fTourData.getTourPerson() != null;

		fActionQuickEdit.setEnabled(isDataAvailable);
		fActionEditTour.setEnabled(isDataAvailable);
	}

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

			menuMgr.add(fActionCreateRefTour);
			menuMgr.add(new Separator());

			// action: create reference tour
			final TourData tourData = fTourChartView.getTourChart().getTourData();
			final boolean canCreateRefTours = tourData.altitudeSerie != null && tourData.distanceSerie != null;

			fActionCreateRefTour.setEnabled(canCreateRefTours);

		}

	}

	public Chart getChart() {
		return fTourChartView.getTourChart();
	}

	public ChartXSlider getLeftSlider() {
		return fLeftSlider;
	}

	public ChartXSlider getRightSlider() {
		return fRightSlider;
	}

	public ArrayList<TourData> getSelectedTours() {

		final ArrayList<TourData> tourList = new ArrayList<TourData>();
		tourList.add(fTourChartView.fTourData);

		return tourList;
	}
}
