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

		fTourChart = new TourChart(fPageBook, SWT.FLAT, true);
		fTourChart.setShowZoomActions(true);
		fTourChart.setShowSlider(true);
		fTourChart.setToolBarManager(getViewSite().getActionBars().getToolBarManager(), true);

		fTourChart.addDoubleClickListener(new Listener() {
			public void handleEvent(final Event event) {
				TourManager.getInstance().openTourInEditor(fTourData.getTourId());
			}
		});

		// set chart title
		fTourChart.addDataModelListener(new IDataModelListener() {
			public void dataModelChanged(final ChartDataModel chartDataModel) {
				chartDataModel.setTitle(TourManager.getTourTitleDetailed(fTourData));
			}
		});

		// fire a slider move selection when a slider was moved in the tour chart
		fTourChart.addSliderMoveListener(new ISliderMoveListener() {
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
		return fTourChart;
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

		final CompareTourConfig refTourConfig = createRefTourConfig(refId);

		if (refTourConfig == null) {
			return;
		}

		/*
		 * show new ref tour
		 */

		fTourData = refTourConfig.getRefTourData();
		fTourChartConfig = refTourConfig.getRefTourChartConfig();

		setRefTourConfig(refTourConfig);

		// set active ref id after the configuration is set
		fActiveRefId = refId;

		// ???
		fTourChart.zoomOut(false);

		updateChart();

	}

	/**
	 * @param refId
	 *        Reference Id
	 * @return
	 */
	private CompareTourConfig createRefTourConfig(final long refId) {

		CompareTourConfig refTourConfig = ReferenceTourManager.getInstance()
				.getCompareTourConfig(refId);

		if (refTourConfig != null) {
			return refTourConfig;
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

			refTourConfig = new CompareTourConfig(refTour,
					chartDataModel,
					refTourData,
					refTourChartConfig,
					compTourchartConfig);

			// keep ref config in the cache
			ReferenceTourManager.getInstance().setRefTourConfig(refId, refTourConfig);
		}

		return refTourConfig;
	}

	/**
	 * set the configuration for a reference tour
	 * 
	 * @param refTourConfig
	 * @return Returns <code>true</code> then the ref tour changed
	 */
	private void setRefTourConfig(final CompareTourConfig refTourConfig) {

		// save the chart slider positions for the old ref tour
		final CompareTourConfig oldRefTourConfig = ReferenceTourManager.getInstance()
				.getCompareTourConfig(fActiveRefId);

		if (oldRefTourConfig != null) {

			final SelectionChartXSliderPosition oldXSliderPosition = fTourChart.getXSliderPosition();

			oldRefTourConfig.setXSliderPosition(new SelectionChartXSliderPosition(fTourChart,
					oldXSliderPosition.slider1ValueIndex,
					oldXSliderPosition.slider2ValueIndex));
		}

		fTourChart.addDataModelListener(new IDataModelListener() {

			public void dataModelChanged(final ChartDataModel changedChartDataModel) {

				final ChartDataXSerie xData = changedChartDataModel.getXData();
				final TourReference refTour = refTourConfig.getRefTour();

				// set marker positions
				xData.setSynchMarkerValueIndex(refTour.getStartValueIndex(),
						refTour.getEndValueIndex());

				// set the value difference of the synch marker
				final int[] xValues = xData.getHighValues()[0];
				final int refTourXMarkerValue = xValues[refTour.getEndValueIndex()]
						- xValues[refTour.getStartValueIndex()];

				TourManager.getInstance()
						.firePropertyChange(TourManager.TOUR_PROPERTY_REFERENCE_TOUR_CHANGED,
								new TourPropertyRefTourChanged(fTourChart,
										refTour.getRefId(),
										refTourXMarkerValue));

				// set title
				changedChartDataModel.setTitle(NLS.bind(Messages.TourMap_Label_chart_title_reference_tour,
						refTour.getLabel(),
						TourManager.getTourTitleDetailed(refTourConfig.getRefTourData())));

			}
		});
	}

	@Override
	public void setFocus() {
		fTourChart.setFocus();

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

		fTourChart.updateTourChart(fTourData, fTourChartConfig, false);

		fPageBook.showPage(fTourChart);

		// set application window title
		setTitleToolTip(TourManager.getTourDate(fTourData));
	}

}
