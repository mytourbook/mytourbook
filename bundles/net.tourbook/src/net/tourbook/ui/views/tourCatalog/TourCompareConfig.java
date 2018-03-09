/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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

import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.tourChart.TourChartConfiguration;

/**
 * contains data and configuration for the compared tour
 */
public class TourCompareConfig {

	private TourReference					_refTour;
	private Long							_refTourTourId;

	private TourChartConfiguration			_refTourChartConfig;
	private TourChartConfiguration			_compareTourChartConfig;

	private SelectionChartXSliderPosition	_xSliderPosition;

	TourCompareConfig(	final TourReference refTour,
						final ChartDataModel refChartDataModel,
						final Long refTourTourId,
						final TourChartConfiguration refTourChartConfig,
						final TourChartConfiguration compTourChartConfig) {

		_refTour = refTour;
		_refTourTourId = refTourTourId;

		_refTourChartConfig = refTourChartConfig;
		_compareTourChartConfig = compTourChartConfig;
	}

	TourChartConfiguration getCompTourChartConfig() {
		return _compareTourChartConfig;
	}

	public TourReference getRefTour() {
		return _refTour;
	}

	TourChartConfiguration getRefTourChartConfig() {
		return _refTourChartConfig;
	}

	public TourData getRefTourData() {

		/*
		 * ensure to have the correct tour data, load tour data because tour data in the ref tour
		 * could be changed, this is a wrong concept which could be changed but requires additonal
		 * work
		 */
		return TourManager.getInstance().getTourData(_refTourTourId);
	}

	SelectionChartXSliderPosition getXSliderPosition() {
		return _xSliderPosition;
	}

	void setXSliderPosition(final SelectionChartXSliderPosition sliderPosition) {
		_xSliderPosition = sliderPosition;
	}

}
