/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views;

import net.tourbook.chart.ChartDataModel;
import net.tourbook.data.TourData;
import net.tourbook.tour.IDataModelListener;
import net.tourbook.tour.TourChart;
import net.tourbook.tour.TourChartConfiguration;
import net.tourbook.tour.TourChartSelection;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

// author: Wolfgang Schramm
// create: 09.07.2007

public class TourChartView extends ViewPart {

	public static final String		ID	= "net.tourbook.ui.views.TourChartView";	//$NON-NLS-1$

	private TourChart				fTourChart;
	private TourChartConfiguration	fTourChartConfig;
	private TourData				fTourData;

	private ISelectionListener		fPostSelectionListener;

	public void createPartControl(Composite parent) {

		fTourChart = new TourChart(parent, SWT.FLAT, true);

		fTourChart.setShowZoomActions(true);
		fTourChart.setShowSlider(true);

		fTourChartConfig = TourManager.createTourChartConfiguration();

		final IDataModelListener dataModelListener = new IDataModelListener() {

			public void dataModelChanged(ChartDataModel chartDataModel) {

				// set title
				chartDataModel.setTitle(TourManager.getTourTitleDetailed(fTourData));
			}
		};

		fTourChart.addDataModelListener(dataModelListener);

		addSelectionListener();
		
		// show current selected chart if there are any
		ISelection selection = getSite().getWorkbenchWindow().getSelectionService().getSelection();
		if (selection != null) {
			updateChart(selection);
		}
	}

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		fPostSelectionListener = new ISelectionListener() {
			public void selectionChanged(IWorkbenchPart part, ISelection selection) {
				updateChart(selection);
			}
		};
		getSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

	public void setFocus() {
		fTourChart.setFocus();
	}

	public void dispose() {
		
		getSite().getPage().removePostSelectionListener(fPostSelectionListener);
		
		super.dispose();
	}

	private void updateChart(ISelection selection) {
		
		if (selection instanceof TourChartSelection) {

			// a tour was selected show the chart

			TourChart tourChart = ((TourChartSelection) selection).getTourChart();

			if (tourChart != null) {
				fTourData = tourChart.getTourData();
				fTourChart.updateChart(fTourData, fTourChartConfig, false);
			}
		}
	}

}
