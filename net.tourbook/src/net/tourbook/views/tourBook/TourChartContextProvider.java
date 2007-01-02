/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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
package net.tourbook.views.tourBook;

import net.tourbook.chart.ChartContextProvider;
import net.tourbook.chart.ChartXSlider;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourReference;
import net.tourbook.tour.TourChart;
import net.tourbook.views.tourMap.ReferenceTourManager;
import net.tourbook.views.tourMap.SelectionNewRefTours;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

class TourChartContextProvider implements ChartContextProvider {

	private TourBookView	fView;

	/**
	 * add a new reference tour to all reference tours
	 */
	class ActionAddTourReference extends Action {

		ActionAddTourReference() {
			setText("Create Reference Tour");
		}

		public void run() {

			TourReference refTour = ReferenceTourManager.getInstance().addReferenceTour(
					fView.getTourChart());

			if (refTour != null) {

				SelectionNewRefTours selection = new SelectionNewRefTours();
				selection.newRefTours.add(refTour);

				fView.firePostSelection(selection);
			}
		}
	}

	public TourChartContextProvider(TourBookView view) {
		fView = view;
	}

	public void fillBarChartContextMenu(IMenuManager menuMgr) {}

	class SliderAction extends Action {

		private ChartXSlider	fSlider;

		SliderAction(String text, ChartXSlider slider) {
			super(text);

			fSlider = slider;
		}

		public void run() {

			// create a new marker

			// get the marker name
			InputDialog dialog = new InputDialog(
					Display.getCurrent().getActiveShell(),
					"Add Marker",
					"Enter Marker name:",
					"",
					null);

			if (dialog.open() != Window.OK) {
				return;
			}

			TourChart tourChart = fView.getTourChart();
			TourData tourData = tourChart.getTourData();

			TourMarker tourMarker = new TourMarker(tourData, TourMarker.MARKER_TYPE_CUSTOM);

			int serieIndex = fSlider.getValuesIndex();
			tourMarker.setSerieIndex(serieIndex);
			tourMarker.setDistance(tourData.distanceSerie[serieIndex]);
			tourMarker.setTime(tourData.timeSerie[serieIndex]);
			tourMarker.setLabel(dialog.getValue().trim());
			tourMarker.setVisualPosition(TourMarker.VISUAL_HORIZONTAL_ABOVE_GRAPH_CENTERED);
			 
			// add new marker to the marker list
			tourData.getTourMarkers().add(tourMarker);

			tourChart.setTourDirty();

			tourChart.updateMarkerLayer(true);

			// update marker list or other listener
			tourChart.fireSelectionTourChart();
		}
	}

	public void fillXSliderContextMenu(	IMenuManager menuMgr,
										ChartXSlider leftSlider,
										ChartXSlider rightSlider) {

		if (leftSlider != null || rightSlider != null) {

			// marker menu
			if (leftSlider != null && rightSlider == null) {
				menuMgr.add(new SliderAction("Add &Marker", leftSlider));
			} else {
				menuMgr.add(new SliderAction("Add &Left Marker", leftSlider));
				menuMgr.add(new SliderAction("Add &Right Marker", rightSlider));
			}

			// add to chart reference
			menuMgr.add(new ActionAddTourReference());
		}
	}

}
