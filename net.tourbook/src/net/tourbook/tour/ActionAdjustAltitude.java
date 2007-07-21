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
package net.tourbook.tour;

import net.tourbook.Messages;
import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;

public class ActionAdjustAltitude extends Action {

	private TourChart				fTourChart;

	private TreeViewer				fTreeViewer;

	private AdjustAltitudeDialog	fDialog;

	public ActionAdjustAltitude(TourChart tourChart) {

		super("AdjustAltitude", AS_PUSH_BUTTON); //$NON-NLS-1$

		fTourChart = tourChart;

		setText(Messages.Tour_Action_adjust_tour_altitude);
		setToolTipText(Messages.Tour_Action_adjust_tour_altitude_tooltip);
		//
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image_adjust_altitude));
	}

	public ActionAdjustAltitude(TreeViewer treeViewer, TourChart treeTourChart) {
		setText(Messages.Tour_Action_adjust_tour_altitude);
		fTreeViewer = treeViewer;
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

		fDialog.create();
		fDialog.init();

		if (fDialog.open() == Window.OK) {
			fTourChart.updateChart(true);
		} else {
			fDialog.restoreOriginalAltitudeValues();
		}
	}

//	private void adjustTreeViewerAltitude() {
//
//		IStructuredSelection treeSelection = (IStructuredSelection) fTreeViewer.getSelection();
//
//		boolean firstTour = false;
//
//		// loop: all selections in the tree
//		for (Iterator iter = treeSelection.iterator(); iter.hasNext();) {
//
//			Object item = iter.next();
//
//			if (item instanceof TreeViewerTourItem) {
//				TreeViewerTourItem tti = (TreeViewerTourItem) item;
//
//				TourData tourData = TourManager.getInstance().getTourData(tti.getTourId());
//
//				if (tourData != null) {
//
//					// adjustAltitude(tourData);
//
//					/*
//					 * set the tour id for the chart which will be updated
//					 */
//					if (firstTour == false) {
//						firstTour = true;
//						fTreeTourId = tourData.getTourId();
//					}
//				}
//			}
//		}
//	}

//	private void adjustChartAltitude() {
//
//		ArrayList<ChartDataYSerie> yDataList = fTourChart.getChartDataModel().getYData();
//		int[][] altitude = null;
//
//		// get altitude data from all y-data
//		for (ChartDataYSerie yData : yDataList) {
//			Integer yDataInfo = (Integer) yData.getCustomData(ChartDataYSerie.YDATA_INFO);
//			if (yDataInfo == TourManager.GRAPH_ALTITUDE) {
//				altitude = yData.getHighValues();
//			}
//		}
//
//		if (altitude == null || altitude.length == 0) {
//			return;
//		}
//
//		// adjustAltitude(fTourChart.fTourData);
//	}

}
