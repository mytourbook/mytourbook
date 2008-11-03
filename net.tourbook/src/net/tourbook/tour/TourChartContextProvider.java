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
package net.tourbook.tour;

import java.util.ArrayList;

import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartXSlider;
import net.tourbook.chart.IChartContextProvider;
import net.tourbook.data.TourData;
import net.tourbook.ui.ITourProvider;

import org.eclipse.jface.action.IMenuManager;

public class TourChartContextProvider implements IChartContextProvider, ITourProvider {

	public ChartXSlider		fSlider;

//	TourEditor		fTourEditor;
//	DialogMarker	fMarkerDialog;

//	private ActionEditQuick	fActionQuickEdit;

	public TourChartContextProvider(final DialogMarker markerDialog) {
//		fMarkerDialog = markerDialog;
	}

	public TourChartContextProvider(final TourEditor tourEditor) {

//		fTourEditor = tourEditor;

//		fActionQuickEdit = new ActionEditQuick(this);
	}

//	private void createMarkerMenu(	final IMenuManager menuMgr,
//									final ChartXSlider leftSlider,
//									final ChartXSlider rightSlider) {
//
////		if (leftSlider != null || rightSlider != null) {
////
////			// marker menu
////			if (leftSlider != null && rightSlider == null) {
////				menuMgr.add(new ActionCreateMarker(this, Messages.tourCatalog_view_action_create_marker, leftSlider));
////			} else {
////				menuMgr.add(new ActionCreateMarker(this, Messages.tourCatalog_view_action_create_left_marker, leftSlider));
////				menuMgr.add(new ActionCreateMarker(this, Messages.tourCatalog_view_action_create_right_marker, rightSlider));
////			}
////		}
//	}

//	/**
//	 * enable actions for the tour editor
//	 */
//	private void enableActions() {
//
////		final TourChart tourChart = fTourEditor.getTourChart();
////		final TourData tourData = tourChart.getTourData();
////
////		final boolean isDataAvailable = tourData != null && tourData.getTourPerson() != null;
////
////		fActionQuickEdit.setEnabled(isDataAvailable);
//	}

	public void fillBarChartContextMenu(final IMenuManager menuMgr,
										final int hoveredBarSerieIndex,
										final int hoveredBarValueIndex) {}

	public void fillContextMenu(final IMenuManager menuMgr) {

//		if (fTourEditor != null) {
//
//			menuMgr.add(new Separator());
//			menuMgr.add(fActionQuickEdit);
//
//			enableActions();
//		}
	}

	public void fillXSliderContextMenu(	final IMenuManager menuMgr,
										final ChartXSlider leftSlider,
										final ChartXSlider rightSlider) {

//		if (fTourEditor != null) {
//
//			// context dialog for the tour editor
//
//			if (leftSlider != null || rightSlider != null) {
//
//				createMarkerMenu(menuMgr, leftSlider, rightSlider);
//
//				// action: create chart reference
//				final TourData tourData = fTourEditor.getTourChart().getTourData();
//				final boolean isEnabled = tourData.altitudeSerie != null && tourData.distanceSerie != null;
//
//				final ActionCreateRefTour actionAddTourReference = new ActionCreateRefTour();
//				actionAddTourReference.setEnabled(isEnabled);
//
//				menuMgr.add(actionAddTourReference);
//			}
//
//		} else if (fMarkerDialog != null) {
//
//			// context menu for the marker dialog
//
//			createMarkerMenu(menuMgr, leftSlider, rightSlider);
//		}

	}

	public Chart getChart() {
		// TODO Auto-generated method stub
		return null;
	}

	public ChartXSlider getLeftSlider() {
		// TODO Auto-generated method stub
		return null;
	}

	public ChartXSlider getRightSlider() {
		// TODO Auto-generated method stub
		return null;
	}

	public ArrayList<TourData> getSelectedTours() {

		final ArrayList<TourData> selectedTourData = new ArrayList<TourData>();
		selectedTourData.add(fTourEditor.getTourChart().getTourData());
		return selectedTourData;

	}

}
