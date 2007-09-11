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
package net.tourbook.ui.views.tourMap;

import javax.persistence.EntityManager;

import net.tourbook.Messages;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ISliderMoveListener;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.IDataModelListener;
import net.tourbook.tour.TourChart;
import net.tourbook.tour.TourChartConfiguration;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.views.TourChartViewPart;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.part.PageBook;

// author: Wolfgang Schramm
// create: 09.07.2007

public class TourMapReferenceTourView extends TourChartViewPart {

	public static final String	ID	= "net.tourbook.views.tourMap.referenceTourView";	//$NON-NLS-1$

	private long				fActiveRefId;

	private PageBook			fPageBook;
	private Label				fPageNoChart;

	@Override
	public void createPartControl(final Composite parent) {

		super.createPartControl(parent);

		fPageBook = new PageBook(parent, SWT.NONE);

		fPageNoChart = new Label(fPageBook, SWT.NONE);
		fPageNoChart.setText(Messages.UI_Label_no_chart_is_selected);

		fCompareTourChart = new TourChart(fPageBook, SWT.FLAT, true);
		fCompareTourChart.setShowZoomActions(true);
		fCompareTourChart.setShowSlider(true);
		fCompareTourChart.setToolBarManager(getViewSite().getActionBars().getToolBarManager(), true);

		fCompareTourChart.addDoubleClickListener(new Listener() {
			public void handleEvent(final Event event) {
				TourManager.getInstance().openTourInEditor(fTourData.getTourId());
			}
		});

		// set chart title
		fCompareTourChart.addDataModelListener(new IDataModelListener() {
			public void dataModelChanged(final ChartDataModel chartDataModel) {
				chartDataModel.setTitle(TourManager.getTourTitleDetailed(fTourData));
			}
		});

		// fire a slider move selection when a slider was moved in the tour chart
		fCompareTourChart.addSliderMoveListener(new ISliderMoveListener() {
			public void sliderMoved(final SelectionChartInfo chartInfoSelection) {
				fPostSelectionProvider.setSelection(chartInfoSelection);
			}
		});

		// show current selected tour
		final ISelection selection = getSite().getWorkbenchWindow()
				.getSelectionService()
				.getSelection();
		if (selection != null) {
			onSelectionChanged(selection);
		} else {
			fPageBook.showPage(fPageNoChart);
		}
	}

	public TourChart getTourChart() {
		return fCompareTourChart;
	}

	@Override
	public void onSelectionChanged(final ISelection selection) {

		if (selection instanceof SelectionComparedTour) {
			showRefTour((SelectionComparedTour) selection);
		}

	}

	private void showRefTour(final SelectionComparedTour selectionComparedTour) {

		// check if the ref tour is already displayed
		final Long refId = selectionComparedTour.getRefId();
		if (refId == null || refId == fActiveRefId) {
			return;
		}

		final TourCompareConfig tourCompareConfig = createTourCompareConfig(refId);

		if (tourCompareConfig == null) {
			return;
		}

		/*
		 * show new ref tour
		 */

		fTourData = tourCompareConfig.getRefTourData();
		fTourChartConfig = tourCompareConfig.getRefTourChartConfig();

		setTourCompareConfig(tourCompareConfig);

		// set active ref id after the configuration is set
		fActiveRefId = refId;

		// ???
		fCompareTourChart.zoomOut(false);

		updateChart();

	}

	/**
	 * @param refId
	 *        Reference Id
	 * @return
	 */
	private TourCompareConfig createTourCompareConfig(final long refId) {

		TourCompareConfig compareConfig = ReferenceTourManager.getInstance()
				.getTourCompareConfig(refId);

		if (compareConfig != null) {
			return compareConfig;
		}

		// load the reference tour from the database
		final EntityManager em = TourDatabase.getInstance().getEntityManager();
		final TourReference refTour = em.find(TourReference.class, refId);
		em.close();

		if (refTour == null) {
			return null;
		} else {

			/*
			 * create a new reference tour configuration
			 */

			final TourData refTourData = refTour.getTourData();
			final TourChartConfiguration refTourChartConfig = TourManager.createTourChartConfiguration();

			final TourChartConfiguration compTourchartConfig = TourManager.createTourChartConfiguration();

			final ChartDataModel chartDataModel = TourManager.getInstance()
					.createChartDataModel(refTourData, refTourChartConfig);

			compareConfig = new TourCompareConfig(refTour,
					chartDataModel,
					refTourData,
					refTourChartConfig,
					compTourchartConfig);

			// keep ref config in the cache
			ReferenceTourManager.getInstance().setTourCompareConfig(refId, compareConfig);
		}

		return compareConfig;
	}

	/**
	 * set the configuration for a reference tour
	 * 
	 * @param compareConfig
	 * @return Returns <code>true</code> then the ref tour changed
	 */
	private void setTourCompareConfig(final TourCompareConfig compareConfig) {

		// save the chart slider positions for the old ref tour
		final TourCompareConfig oldRefTourConfig = ReferenceTourManager.getInstance()
				.getTourCompareConfig(fActiveRefId);

		if (oldRefTourConfig != null) {

			final SelectionChartXSliderPosition oldXSliderPosition = fCompareTourChart.getXSliderPosition();

			oldRefTourConfig.setXSliderPosition(new SelectionChartXSliderPosition(fCompareTourChart,
					oldXSliderPosition.slider1ValueIndex,
					oldXSliderPosition.slider2ValueIndex));
		}

		fCompareTourChart.addDataModelListener(new IDataModelListener() {

			public void dataModelChanged(final ChartDataModel changedChartDataModel) {

				final ChartDataXSerie xData = changedChartDataModel.getXData();
				final TourReference refTour = compareConfig.getRefTour();

				// set marker positions
				xData.setSynchMarkerValueIndex(refTour.getStartValueIndex(),
						refTour.getEndValueIndex());

				// set the value difference of the synch marker
				final int[] xValues = xData.getHighValues()[0];
				final int refTourXMarkerValue = xValues[refTour.getEndValueIndex()]
						- xValues[refTour.getStartValueIndex()];

				TourManager.getInstance()
						.firePropertyChange(TourManager.TOUR_PROPERTY_REFERENCE_TOUR_CHANGED,
								new TourPropertyRefTourChanged(fCompareTourChart,
										refTour.getRefId(),
										refTourXMarkerValue));

				// set title
				changedChartDataModel.setTitle(NLS.bind(Messages.TourMap_Label_chart_title_reference_tour,
						refTour.getLabel(),
						TourManager.getTourTitleDetailed(compareConfig.getRefTourData())));

			}
		});
	}

	@Override
	public void setFocus() {
		fCompareTourChart.setFocus();

//		/*
//		 * fire tour selection
//		 */
//		fPostSelectionProvider.setSelection(new SelectionTourData(fTourChart, fTourData));
	}

	@Override
	public void updateChart() {

		if (fTourData == null) {
			return;
		}

		fCompareTourChart.updateTourChart(fTourData, fTourChartConfig, false);

		fPageBook.showPage(fCompareTourChart);

		// set application window title
		setTitleToolTip(TourManager.getTourDate(fTourData));
	}

}
