/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourCatalog;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ISliderMoveListener;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;
import net.tourbook.tour.IDataModelListener;
import net.tourbook.tour.SelectionTourChart;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourChartViewer;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.tourChart.TourChartContextProvider;
import net.tourbook.ui.tourChart.TourChartViewPart;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.PageBook;

// author: Wolfgang Schramm
// create: 09.07.2007

public class TourCatalogViewReferenceTour extends TourChartViewPart implements ITourChartViewer {

	public static final String	ID				= "net.tourbook.views.tourCatalog.referenceTourView";	//$NON-NLS-1$

	private long				_activeRefId	= -1;

	private PageBook			_pageBook;
	private Label				_pageNoChart;

	@Override
	public void createPartControl(final Composite parent) {

		super.createPartControl(parent);

		_pageBook = new PageBook(parent, SWT.NONE);

		_pageNoChart = new Label(_pageBook, SWT.NONE);
		_pageNoChart.setText(Messages.UI_Label_no_chart_is_selected);

		_tourChart = new TourChart(_pageBook, SWT.FLAT, getSite().getPart());
		_tourChart.setShowZoomActions(true);
		_tourChart.setShowSlider(true);
		_tourChart.setToolBarManager(getViewSite().getActionBars().getToolBarManager(), true);
		_tourChart.setContextProvider(new TourChartContextProvider(this));
		_tourChart.setTourInfoActionsEnabled(true);

		// set chart title
		_tourChart.addDataModelListener(new IDataModelListener() {
			@Override
			public void dataModelChanged(final ChartDataModel chartDataModel) {

				if (_tourData == null) {
					return;
				}

				chartDataModel.setTitle(TourManager.getTourTitleDetailed(_tourData));
			}
		});

		// fire a slider move selection when a slider was moved in the tour chart
		_tourChart.addSliderMoveListener(new ISliderMoveListener() {
			@Override
			public void sliderMoved(final SelectionChartInfo chartInfo) {

				TourManager.fireEventWithCustomData(
						TourEventId.SLIDER_POSITION_CHANGED,
						chartInfo,
						TourCatalogViewReferenceTour.this);
			}
		});

		_pageBook.showPage(_pageNoChart);

		// show current selected tour
		final ISelection selection = getSite().getWorkbenchWindow().getSelectionService().getSelection();
		if (selection != null) {
			onSelectionChanged(selection);
		}
	}

	@Override
	public ArrayList<TourData> getSelectedTours() {

		final ArrayList<TourData> selectedTour = new ArrayList<TourData>();

		if (_tourData != null) {

			selectedTour.add(_tourData);
		}

		return selectedTour;
	}

	@Override
	public TourChart getTourChart() {
		return _tourChart;
	}

	private void onSelectionChanged(final ISelection selection) {

		if (selection instanceof SelectionTourCatalogView) {

			showRefTour(((SelectionTourCatalogView) selection).getRefId());

		} else if (selection instanceof StructuredSelection) {

			final Object firstElement = ((StructuredSelection) selection).getFirstElement();

			if (firstElement instanceof TVICatalogComparedTour) {

				showRefTour(((TVICatalogComparedTour) firstElement).getRefId());

			} else if (firstElement instanceof TVICompareResultComparedTour) {

				showRefTour(((TVICompareResultComparedTour) firstElement).refTour.refId);
			}
		}
	}

	@Override
	protected void onSelectionChanged(final IWorkbenchPart part, final ISelection selection) {

		if (part == TourCatalogViewReferenceTour.this) {
			return;
		}

		onSelectionChanged(selection);
	}

	@Override
	public void setFocus() {

		_tourChart.setFocus();

		_postSelectionProvider.setSelection(new SelectionTourChart(_tourChart));
	}

	/**
	 * set the configuration for a reference tour
	 * 
	 * @param compareConfig
	 * @return Returns <code>true</code> then the ref tour changed
	 */
	private void setTourCompareConfig(final TourCompareConfig compareConfig) {

		// save the chart slider positions for the old ref tour
		final TourCompareConfig oldRefTourConfig = ReferenceTourManager
				.getInstance()
				.getTourCompareConfig(_activeRefId);

		if (oldRefTourConfig != null) {

			final SelectionChartXSliderPosition oldXSliderPosition = _tourChart.getXSliderPosition();

			oldRefTourConfig.setXSliderPosition(
					new SelectionChartXSliderPosition(
							_tourChart,
							oldXSliderPosition
									.getLeftSliderValueIndex(),
							oldXSliderPosition.getRightSliderValueIndex()));
		}

		_tourChart.addDataModelListener(new IDataModelListener() {

			@Override
			public void dataModelChanged(final ChartDataModel changedChartDataModel) {

				if (_tourData == null) {
					return;
				}

				final ChartDataXSerie xData = changedChartDataModel.getXData();
				final TourReference refTour = compareConfig.getRefTour();

				// set marker positions
				xData.setSynchMarkerValueIndex(refTour.getStartValueIndex(), refTour.getEndValueIndex());

				// set the value difference of the synch marker
				final double[] xValues = xData.getHighValuesDouble()[0];
				final double refTourXMarkerValue = xValues[refTour.getEndValueIndex()]
						- xValues[refTour.getStartValueIndex()];

				TourManager.fireEventWithCustomData(
						TourEventId.REFERENCE_TOUR_CHANGED, //
						new TourPropertyRefTourChanged(_tourChart, refTour.getRefId(), refTourXMarkerValue),
						TourCatalogViewReferenceTour.this);

				// set title
				changedChartDataModel.setTitle(
						NLS.bind(
								Messages.tourCatalog_view_label_chart_title_reference_tour,
								refTour.getLabel(),
								TourManager.getTourTitleDetailed(_tourData)));

			}
		});
	}

	private void showRefTour(final long refId) {

		// check if the ref tour is already displayed
		if (refId == _activeRefId) {
			return;
		}

		final TourCompareConfig tourCompareConfig = ReferenceTourManager.getInstance().getTourCompareConfig(refId);
		if (tourCompareConfig == null) {
			return;
		}

		/*
		 * show new ref tour
		 */

		_tourData = tourCompareConfig.getRefTourData();
		_tourChartConfig = tourCompareConfig.getRefTourChartConfig();

		setTourCompareConfig(tourCompareConfig);

		// set active ref id after the configuration is set
		_activeRefId = refId;

		// ???
		_tourChart.onExecuteZoomOut(false, 1.0);

		updateChart();

	}

	@Override
	public void updateChart() {

		if (_tourData == null) {
			_activeRefId = -1;
			_pageBook.showPage(_pageNoChart);
			return;
		}

		_tourChart.updateTourChart(_tourData, _tourChartConfig, false);

		_pageBook.showPage(_tourChart);

		// set application window title
		setTitleToolTip(TourManager.getTourDateShort(_tourData));
	}

}
