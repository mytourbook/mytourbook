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

	private TourReference					fRefTour;
	private TourChartConfiguration			fRefTourChartConfig;

	private TourChartConfiguration			fCompareTourChartConfig;

	private SelectionChartXSliderPosition	xSliderPosition;
	private Long							fRefTourTourId;

	TourCompareConfig(	final TourReference refTour,
						final ChartDataModel refChartDataModel,
						final Long refTourTourId,
						final TourChartConfiguration refTourChartConfig,
						final TourChartConfiguration compTourChartConfig) {

		fRefTour = refTour;
		fRefTourTourId = refTourTourId;

		fRefTourChartConfig = refTourChartConfig;
		fCompareTourChartConfig = compTourChartConfig;
	}

	TourChartConfiguration getCompTourChartConfig() {
		return fCompareTourChartConfig;
	}

	TourReference getRefTour() {
		return fRefTour;
	}

	TourChartConfiguration getRefTourChartConfig() {
		return fRefTourChartConfig;
	}

	TourData getRefTourData() {
		
		/*
		 * ensure to have the correct tour data, load tour data because tour data in the ref tour
		 * could be changed, this is a wrong concept which could be changed but requires additonal
		 * work
		 */
		return TourManager.getInstance().getTourData(fRefTourTourId);
	}

	SelectionChartXSliderPosition getXSliderPosition() {
		return xSliderPosition;
	}

	void setXSliderPosition(final SelectionChartXSliderPosition sliderPosition) {
		xSliderPosition = sliderPosition;
	}

}
