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
 * contains data/configuration for the current ref chart
 */
public class RefTourChartData {

	private TourReference			refTour;
	private ChartDataModel			chartDataModel;
	private TourData				refTourData;

	private TourChartConfiguration	refTourChartConfig;
	private TourChartConfiguration	compTourChartConfig;

	private SelectionChartXSliderPosition	xSliderPosition;

	RefTourChartData(TourReference refTour, ChartDataModel chartDataModel, TourData refTourData,
			TourChartConfiguration refTourChartConfig,
			TourChartConfiguration compTourChartConfig) {

		this.refTour = refTour;
		this.chartDataModel = chartDataModel;
		this.refTourData = refTourData;
		this.refTourChartConfig = refTourChartConfig;
		this.compTourChartConfig = compTourChartConfig;
	}

	SelectionChartXSliderPosition getXSliderPosition() {
		return xSliderPosition;
	}

	void setXSliderPosition(SelectionChartXSliderPosition sliderPosition) {
		xSliderPosition = sliderPosition;
	}

	TourChartConfiguration getRefTourChartConfig() {
		return refTourChartConfig;
	}

	TourChartConfiguration getCompTourChartConfig() {
		return compTourChartConfig;
	}

	ChartDataModel getChartDataModel() {
		return chartDataModel;
	}

	TourReference getRefTour() {
		return refTour;
	}

	TourData getRefTourData() {
		return refTourData;
	}

}
