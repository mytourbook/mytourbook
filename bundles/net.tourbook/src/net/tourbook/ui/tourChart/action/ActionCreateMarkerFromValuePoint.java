/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.IChartContextProvider;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.tour.DialogMarker;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.tourChart.ChartLabel;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

public class ActionCreateMarkerFromValuePoint extends Action {

	/**
	 * 
	 */
	private final IChartContextProvider	_chartContextProvider;

	private IMarkerReceiver				_markerReceiver;
	private int							_vpIndex	= -1;

	public ActionCreateMarkerFromValuePoint(final IChartContextProvider chartContextProvider, final String text) {

		super(text);

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__edit_tour_marker_new));
		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__edit_tour_marker_new_disabled));

		_chartContextProvider = chartContextProvider;
	}

	/**
	 * Creates a new marker
	 * 
	 * @param tourData
	 * @return
	 */
	private TourMarker createTourMarker(final TourData tourData) {

		if (tourData.timeSerie == null) {
			return null;
		}

		final int serieIndex = _vpIndex;
		final int relativeTourTime = tourData.timeSerie[serieIndex];
		final float[] altitudeSerie = tourData.altitudeSerie;
		final float[] distSerie = tourData.getMetricDistanceSerie();
		final double[] latitudeSerie = tourData.latitudeSerie;
		final double[] longitudeSerie = tourData.longitudeSerie;

		// create a new marker
		final TourMarker tourMarker = new TourMarker(tourData, ChartLabel.MARKER_TYPE_CUSTOM);
		tourMarker.setSerieIndex(serieIndex);
		tourMarker.setLabel(Messages.TourData_Label_new_marker);
		tourMarker.setTime(relativeTourTime, tourData.getTourStartTimeMS() + (relativeTourTime * 1000));

		if (altitudeSerie != null) {
			tourMarker.setAltitude(altitudeSerie[serieIndex]);
         tourMarker.setDescription("#alti: " + (int)altitudeSerie[serieIndex] + " m");
		}

		if (distSerie != null) {
			tourMarker.setDistance(distSerie[serieIndex]);
		}

		if (latitudeSerie != null) {
			tourMarker.setGeoPosition(latitudeSerie[serieIndex], longitudeSerie[serieIndex]);
		}

		return tourMarker;
	}

	@Override
	public void run() {

		final Chart chart = _chartContextProvider.getChart();

		TourData tourData = null;
		final Object tourId = chart.getChartDataModel().getCustomData(Chart.CUSTOM_DATA_TOUR_ID);
		if (tourId instanceof Long) {
			tourData = TourManager.getInstance().getTourData((Long) tourId);
		}

		if (tourData == null) {
			return;
		}

		final TourMarker newTourMarker = createTourMarker(tourData);

		// check if a tour marker could be created
		if (newTourMarker == null) {
			return;
		}

		if (_markerReceiver != null) {

			_markerReceiver.addTourMarker(newTourMarker);

			// the marker dialog will not be opened
			return;
		}

		final DialogMarker markerDialog = new DialogMarker(Display.getCurrent().getActiveShell(), tourData, null);

		markerDialog.create();
		markerDialog.addTourMarker(newTourMarker);

		if (markerDialog.open() == Window.OK) {
			TourManager.saveModifiedTour(tourData);
		}
	}

	public void setMarkerReceiver(final IMarkerReceiver markerReceiver) {
		_markerReceiver = markerReceiver;
	}

	public void setValuePointIndex(final int vpIndex) {
		_vpIndex = vpIndex;
	}
}
