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
package net.tourbook.ui.views.tourMap;

import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;
import net.tourbook.tour.TourChartConfiguration;

/**
 * contains data and configuration for the compared tour
 */
public class TourCompareConfig {

	private TourReference					fRefTour;
	private TourData						fRefTourData;
	private TourChartConfiguration			fRefTourChartConfig;

	private TourChartConfiguration			fCompareTourChartConfig;

	private SelectionChartXSliderPosition	xSliderPosition;

	TourCompareConfig(TourReference refTour, ChartDataModel refChartDataModel,
			TourData refTourData, TourChartConfiguration refTourChartConfig,
			TourChartConfiguration compTourChartConfig) {

		fRefTour = refTour;
		fRefTourData = refTourData;

		fRefTourChartConfig = refTourChartConfig;
		fCompareTourChartConfig = compTourChartConfig;
	}

	SelectionChartXSliderPosition getXSliderPosition() {
		return xSliderPosition;
	}

	void setXSliderPosition(SelectionChartXSliderPosition sliderPosition) {
		xSliderPosition = sliderPosition;
	}

	TourChartConfiguration getRefTourChartConfig() {
		return fRefTourChartConfig;
	}

	TourChartConfiguration getCompTourChartConfig() {
		return fCompareTourChartConfig;
	}

	TourReference getRefTour() {
		return fRefTour;
	}

	TourData getRefTourData() {
		return fRefTourData;
	}

}
