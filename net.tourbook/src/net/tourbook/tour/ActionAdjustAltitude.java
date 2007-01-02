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
package net.tourbook.tour;

import java.util.ArrayList;
import java.util.Iterator;

import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;

public class ActionAdjustAltitude extends Action {

	private TourChart				fTourChart;

	private TreeViewer				fTreeViewer;
	private TourChart				fTreeTourChart;

	private AdjustAltitudeDialog	fDialog;

	private long					fTreeTourId;

	public ActionAdjustAltitude(TourChart tourChart) {

		super("AdjustAltitude", AS_PUSH_BUTTON);

		this.fTourChart = tourChart;

		setText("Adjust tour altitude");
		setToolTipText("Adjust the tour altitude");
		//
		setImageDescriptor(TourbookPlugin.getImageDescriptor("adjust-altitude.gif"));
	}

	public ActionAdjustAltitude(TreeViewer treeViewer, TourChart treeTourChart) {
		setText("Adjust tour altitude");
		fTreeViewer = treeViewer;
		fTreeTourChart = treeTourChart;
	}

	public void run() {

		// open the dialog to adjust the altitude

		if (fTourChart != null) {
			fDialog = new AdjustAltitudeDialog(fTourChart.getShell(), fTourChart);
		} else if (fTreeViewer != null) {
			fDialog = new AdjustAltitudeDialog(
					fTreeViewer.getTree().getShell(),
					((IStructuredSelection) fTreeViewer.getSelection()));
		} else {
			return;
		}

		if (fDialog.open() == Window.CANCEL) {
			return;
		}

		if (fTourChart != null) {

			adjustChartAltitude();
			fTourChart.updateChart();

		} else if (fTreeViewer != null) {

			adjustTreeViewerAltitude();
			fTreeTourChart.updateChart(fTreeTourId);

		} else {
			return;
		}
	}

	private void adjustTreeViewerAltitude() {

		IStructuredSelection treeSelection = (IStructuredSelection) fTreeViewer.getSelection();

		boolean firstTour = false;

		// loop: all selections in the tree
		for (Iterator iter = treeSelection.iterator(); iter.hasNext();) {
			
			Object item = iter.next();

			if (item instanceof TreeViewerTourItem) {
				TreeViewerTourItem tti = (TreeViewerTourItem) item;

				TourData tourData = TourDatabase.getTourDataByTourId(tti.getTourId());

				if (tourData != null) {

					adjustAltitude(tourData);

					/*
					 * set the tour id for the chart which will be updated
					 */
					if (firstTour == false) {
						firstTour = true;
						fTreeTourId = tourData.getTourId();
					}
				}
			}
		}
	}

	private void adjustChartAltitude() {

		ArrayList<ChartDataYSerie> yDataList = fTourChart.getChartDataModel().getYData();
		int[][] altitude = null;

		// get altitude data from all y-data
		for (ChartDataYSerie yData : yDataList) {
			Integer yDataInfo = (Integer) yData.getCustomData(ChartDataYSerie.YDATA_INFO);
			if (yDataInfo == TourManager.GRAPH_ALTITUDE) {
				altitude = yData.getHighValues();
			}
		}

		if (altitude == null || altitude.length == 0) {
			return;
		}

		adjustAltitude(fTourChart.fTourData);
	}

	private void adjustAltitude(TourData tourData) {

		if (fDialog.fSelectedAdjustment == AdjustAltitudeDialog.ALTITUDE_ADJUSTMENT_ALL) {

			// adjust evenly
			adjustEvenly(tourData);

		} else if (fDialog.fSelectedAdjustment == AdjustAltitudeDialog.ALTITUDE_ADJUSTMENT_END) {
			// adjust end
			adjustEndAltitude(tourData);

		} else if (fDialog.fSelectedAdjustment == AdjustAltitudeDialog.ALTITUDE_ADJUSTMENT_MAX_HEIGHT) {

			// adjust start
			adjustMaxHeightAltitude(tourData);
		}

		TourDatabase.saveTour(tourData);
	}

	private void adjustMaxHeightAltitude(TourData tourData) {

		int newAltitude = fDialog.fNewAltitude;
		int[] altitudeSerie = tourData.altitudeSerie;
		int startAltitude = altitudeSerie[0];

		// calculate max altitude
		int maxHeight = altitudeSerie[0];
		for (int altitude : altitudeSerie) {
			if (altitude > maxHeight) {
				maxHeight = altitude;
			}
		}

		// adjust altitude
		int maxHeight0 = maxHeight - startAltitude;
		int newHeight0 = newAltitude - startAltitude;
		float heightDiff = (float) maxHeight0 / (float) newHeight0;

		for (int serieIndex = 0; serieIndex < altitudeSerie.length; serieIndex++) {

			int altitude = altitudeSerie[serieIndex];
			int altitude0 = altitude - startAltitude;
			altitude0 = (int) (altitude0 / heightDiff);

			altitudeSerie[serieIndex] = altitude0 + startAltitude;
		}
	}

	private void adjustEndAltitude(TourData tourData) {

		int[] altitudeSerie = tourData.altitudeSerie;
		int[] distanceSerie = tourData.distanceSerie;
		int newAltitude = fDialog.fNewAltitude;
		int endDiff = newAltitude - altitudeSerie[altitudeSerie.length - 1];
		float tourDistance = distanceSerie[distanceSerie.length - 1];

		// adjust every altitude with the same difference
		for (int serieIndex = 0; serieIndex < altitudeSerie.length; serieIndex++) {
			float distance = distanceSerie[serieIndex];
			int altitudeDiff = (int) (distance / tourDistance * endDiff);
			altitudeSerie[serieIndex] = altitudeSerie[serieIndex] + altitudeDiff;
		}
	}

	private void adjustEvenly(TourData tourData) {

		int newAltitude = fDialog.fNewAltitude;
		int[] altitudeSerie = tourData.altitudeSerie;
		int altitudeDiff = newAltitude - altitudeSerie[0];

		// adjust every altitude with the same difference
		for (int altIndex = 0; altIndex < altitudeSerie.length; altIndex++) {
			altitudeSerie[altIndex] = altitudeSerie[altIndex] + altitudeDiff;
		}

	}
}
