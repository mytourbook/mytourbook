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

package net.tourbook.ui.tourChart.action;

import net.tourbook.Messages;
import net.tourbook.chart.ChartMarker;
import net.tourbook.chart.ChartXSlider;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.tour.DialogMarker;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.tourChart.TourChart;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

public class ActionCreateMarker extends Action {

	/**
	 * 
	 */
	private final TourChart	fTourChart;
	private boolean			fIsLeftSlider;

	private IMarkerReceiver	fMarkerReceiver;

	public ActionCreateMarker(final TourChart tourChart, final String text, final boolean isLeftSlider) {

		super(text);

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__edit_tour_marker_new));

		fTourChart = tourChart;
		fIsLeftSlider = isLeftSlider;
	}

	/**
	 * Creates a new marker
	 * 
	 * @param tourData
	 * @return
	 */
	private TourMarker createTourMarker(final TourData tourData) {

		final ChartXSlider slider = fIsLeftSlider ? fTourChart.getLeftSlider() : fTourChart.getRightSlider();

		if (slider == null) {
			return null;
		}

		final int serieIndex = slider.getValuesIndex();

		// create a new marker
		final TourMarker tourMarker = new TourMarker(tourData, ChartMarker.MARKER_TYPE_CUSTOM);
		tourMarker.setSerieIndex(serieIndex);
		tourMarker.setDistance(tourData.getMetricDistanceSerie()[serieIndex]);
		tourMarker.setTime(tourData.timeSerie[serieIndex]);
		tourMarker.setLabel(Messages.TourData_Label_new_marker);
		tourMarker.setVisualPosition(ChartMarker.VISUAL_HORIZONTAL_ABOVE_GRAPH_CENTERED);

		return tourMarker;
	}

	@Override
	public void run() {

		final TourData tourData = fTourChart.getTourData();

//		if (UI.isTourModified(tourData)) {
//			return;
//		}

		final TourMarker newTourMarker = createTourMarker(tourData);
		if (newTourMarker == null) {
			return;
		}

		if (fMarkerReceiver != null) {
			fMarkerReceiver.addTourMarker(newTourMarker);
			
			// the marker dialog will not be opened
			return;
		}

		final DialogMarker markerDialog = new DialogMarker(Display.getCurrent().getActiveShell(), tourData, null);

		markerDialog.create();
		markerDialog.addTourMarker(newTourMarker);

		if (markerDialog.open() == Window.OK) {
			TourManager.saveModifiedTour(tourData);
		}

//		} else if (this.fChartContextProvider.fMarkerDialog != null) {
//
//			final TourMarker newTourMarker = createTourMarker(this.fChartContextProvider.fMarkerDialog.getTourData());
//
//			this.fChartContextProvider.fMarkerDialog.addTourMarker(newTourMarker);
//		}
	}

	public void setMarkerReceiver(final IMarkerReceiver markerReceiver) {
		fMarkerReceiver = markerReceiver;
	}
}
