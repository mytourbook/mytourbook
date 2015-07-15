/*******************************************************************************
 * Copyright (C) 2005, 2014 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.tourChart.action;

import net.tourbook.chart.Chart;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.tourChart.TourChart;

import org.eclipse.jface.action.Action;

public class ActionConvertIntoSharedMarker_DISABLED extends Action {

	private TourChart	_tourChart;
	private TourMarker	_tourMarker;

	public ActionConvertIntoSharedMarker_DISABLED(final TourChart tourChart) {

//		super(Messages.Action_SharedMarker_ConvertFromTourMarker);

		_tourChart = tourChart;
	}

	@Override
	public void run() {

		TourData tourData = null;
		final Object tourId = _tourChart.getChartDataModel().getCustomData(Chart.CUSTOM_DATA_TOUR_ID);
		if (tourId instanceof Long) {
			tourData = TourManager.getInstance().getTourData((Long) tourId);
		}

		// check required data
		if (tourData == null || tourData.latitudeSerie == null) {
			return;
		}

		final int tourMarkerSerieIndex = _tourMarker.getSerieIndex();

//		final SharedMarker sharedMarker = new SharedMarker();
//
//		sharedMarker.setName(_tourMarker.getLabel());
//		sharedMarker.setDescription(_tourMarker.getDescription());
//
//		sharedMarker.setUrlAddress(_tourMarker.getUrlAddress());
//		sharedMarker.setUrlText(_tourMarker.getUrlText());
//
//		sharedMarker.setLatitude(tourData.latitudeSerie[tourMarkerSerieIndex]);
//		sharedMarker.setLongitude(tourData.longitudeSerie[tourMarkerSerieIndex]);
//
//
//		if (tourData.altitudeSerie != null) {
//			sharedMarker.setAltitude(tourData.altitudeSerie[tourMarkerSerieIndex]);
//		}
//
//		final SharedMarker savedSharedMarker = TourDatabase.saveEntity(//
//				sharedMarker,
//				sharedMarker.getId(),
//				SharedMarker.class);
//
//		TourManager.fireEvent(TourEventId.TOUR_CHANGED);
	}

	public void setTourMarker(final TourMarker tourMarker) {
		_tourMarker = tourMarker;
	}
}
