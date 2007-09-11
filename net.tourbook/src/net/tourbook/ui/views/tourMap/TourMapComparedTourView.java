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

import net.tourbook.Messages;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.IChartListener;
import net.tourbook.data.TourData;
import net.tourbook.tour.IDataModelListener;
import net.tourbook.tour.ITourPropertyListener;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.TourChart;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.views.TourChartViewPart;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.part.PageBook;

// author: Wolfgang Schramm
// create: 06.09.2007

public class TourMapComparedTourView extends TourChartViewPart implements ISynchedChart {

	public static final String					ID				= "net.tourbook.views.tourMap.comparedTourView";	//$NON-NLS-1$

	/*
	 * keep data from the reference tour view
	 */
	private long								fRefTourRefId	= -1;
	private TourChart							fRefTourTourChart;
	private int									fRefTourXMarkerValueDifference;

	/*
	 * CT ... (c)ompared (t)our which is displayed in this view
	 */

	/**
	 * Tour Id for the displayed compared tour
	 */
	private long								fCTTourId		= -1;

	/**
	 * Reference Id for the displayed compared tour
	 */
	private long								fCTRefId		= -1;

	/**
	 * Reference tour chart for the displayed compared tour
	 */
	private TourChart							fCTRefTourChart;

	private PageBook							fPageBook;
	private Label								fPageNoChart;

	private ITourPropertyListener				fRefTourPropertyListener;

	private ActionSynchChartHorizontalByScale	fActionSynchChartsByScale;
	private ActionSynchChartHorizontalBySize	fActionSynchChartsBySize;

	private void addRefTourPropertyListener() {

		fRefTourPropertyListener = new ITourPropertyListener() {
			public void propertyChanged(int propertyId, Object propertyData) {

				if (propertyId == TourManager.TOUR_PROPERTY_REFERENCE_TOUR_CHANGED
						&& propertyData instanceof TourPropertyRefTourChanged) {

					TourPropertyRefTourChanged tourProperty = (TourPropertyRefTourChanged) propertyData;

					fRefTourRefId = tourProperty.refId;
					fRefTourTourChart = tourProperty.refTourChart;
					fRefTourXMarkerValueDifference = tourProperty.xMarkerValue;

					synchCharts();
				}
			}
		};

		TourManager.getInstance().addPropertyListener(fRefTourPropertyListener);
	}

	private void createActions() {

		fActionSynchChartsBySize = new ActionSynchChartHorizontalBySize(this);
		fActionSynchChartsByScale = new ActionSynchChartHorizontalByScale(this);

		final IToolBarManager tbm = fCompareTourChart.getToolBarManager();
		tbm.add(fActionSynchChartsByScale);
		tbm.add(fActionSynchChartsBySize);

		tbm.update(true);
	}

	@Override
	public void createPartControl(Composite parent) {

		super.createPartControl(parent);

		fPageBook = new PageBook(parent, SWT.NONE);

		fPageNoChart = new Label(fPageBook, SWT.NONE);
		fPageNoChart.setText(Messages.UI_Label_no_chart_is_selected);

		fCompareTourChart = new TourChart(fPageBook, SWT.FLAT, true);
		fCompareTourChart.setShowZoomActions(true);
		fCompareTourChart.setShowSlider(true);
		fCompareTourChart.setToolBarManager(getViewSite().getActionBars().getToolBarManager(), true);

		fCompareTourChart.addDoubleClickListener(new Listener() {
			public void handleEvent(Event event) {
				TourManager.getInstance().openTourInEditor(fTourData.getTourId());
			}
		});

		createActions();

		addRefTourPropertyListener();

		// show current selected tour
		ISelection selection = getSite().getWorkbenchWindow().getSelectionService().getSelection();
		if (selection != null) {
			onSelectionChanged(selection);
		} else {
			fPageBook.showPage(fPageNoChart);
		}

		synchCharts();
	}

	@Override
	public void dispose() {

		TourManager.getInstance().removePropertyListener(fRefTourPropertyListener);

		super.dispose();
	}

	@Override
	protected void onSelectionChanged(ISelection selection) {
		if (selection instanceof SelectionComparedTour) {
			showCompTour((SelectionComparedTour) selection);
		}
	}

	@Override
	public void setFocus() {
		fCompareTourChart.setFocus();
	}

	/**
	 * Shows the compared tour which was selected by the user in the {@link TourMapView}
	 * 
	 * @param selectionComparedTour
	 */
	private void showCompTour(final SelectionComparedTour selectionComparedTour) {

		final Long ctTourId = selectionComparedTour.getCompTourId();

		// check if the ref tour is already displayed
		if (ctTourId == null || fCTTourId == ctTourId) {
			return;
		}

		// load the tourdata of the compared tour from the database
		final TourData compTourData = TourManager.getInstance().getTourData(ctTourId);
		if (compTourData == null) {
			return;
		}

		// set active id's 
		fCTTourId = ctTourId;
		fCTRefId = selectionComparedTour.getRefId();

		fCompareTourChart.addDataModelListener(new IDataModelListener() {
			public void dataModelChanged(ChartDataModel changedChartDataModel) {

				ChartDataXSerie xData = changedChartDataModel.getXData();

				// set synch marker data
				xData.setSynchMarkerValueIndex(selectionComparedTour.getCompareStartIndex(),
						selectionComparedTour.getCompareEndIndex());

				// set title
				changedChartDataModel.setTitle(NLS.bind(Messages.TourMap_Label_chart_title_compared_tour,
						TourManager.getTourTitleDetailed(compTourData)));
			}
		});

		fCompareTourChart.addXMarkerDraggingListener(new IChartListener() {

			public int getXMarkerValueDiff() {
				return fRefTourXMarkerValueDifference;
			}

			public void xMarkerMoved(	final int movedXMarkerStartValueIndex,
										final int movedXMarkerEndValueIndex) {
				xMarkerMovedInCompTourChart(movedXMarkerStartValueIndex, movedXMarkerEndValueIndex);
			}
		});

		TourCompareConfig tourCompareConfig = ReferenceTourManager.getInstance()
				.getTourCompareConfig(selectionComparedTour.getRefId());

		if (tourCompareConfig != null) {

			fTourData = compTourData;
			fTourChartConfig = tourCompareConfig.getCompTourChartConfig();
			fTourChartConfig.setMinMaxKeeper(true);

			/*
			 * fire the change event so that the tour markers updated
			 */
			fPostSelectionProvider.setSelection(new SelectionTourData(fCompareTourChart,
					fCompareTourChart.getTourData()));

			updateChart();

			synchCharts();
		}

	}

	private void synchCharts() {

		// check initial value
		if (fCTRefId == -1) {
			fActionSynchChartsByScale.setEnabled(false);
			fActionSynchChartsBySize.setEnabled(false);
			return;
		}

		boolean isSynchEnabled = false;

		if (fCTRefId == fRefTourRefId) {

			// reference tour for the compared chart is displayed

			if (fCTRefTourChart != fRefTourTourChart) {

				fCTRefTourChart = fRefTourTourChart;
			}

			isSynchEnabled = true;

		} else {

			// another ref tour is displayed, disable synchronization

			if (fCTRefTourChart != null) {
				fCTRefTourChart.synchChart(false, fCompareTourChart, Chart.SYNCH_MODE_NO);
			}
			fActionSynchChartsByScale.setChecked(false);
			fActionSynchChartsBySize.setChecked(false);
		}

		fActionSynchChartsByScale.setEnabled(isSynchEnabled);
		fActionSynchChartsBySize.setEnabled(isSynchEnabled);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.tourbook.ui.views.tourMap.ISynchedChart#synchCharts(boolean, int)
	 */
	public void synchCharts(boolean isSynched, int synchMode) {
		if (fCTRefTourChart != null) {

			// uncheck other synch mode
			switch (synchMode) {
			case Chart.SYNCH_MODE_BY_SCALE:
				fActionSynchChartsBySize.setChecked(false);
				break;

			case Chart.SYNCH_MODE_BY_SIZE:
				fActionSynchChartsByScale.setChecked(false);
				break;

			default:
				break;
			}

			fCTRefTourChart.synchChart(isSynched, fCompareTourChart, synchMode);
		}
	}

	@Override
	protected void updateChart() {

		if (fTourData == null) {
			return;
		}

		fCompareTourChart.updateTourChart(fTourData, fTourChartConfig, false);

		fPageBook.showPage(fCompareTourChart);

		// set application window title
		setTitleToolTip(TourManager.getTourDate(fTourData));
	}

	private void xMarkerMovedInCompTourChart(	final int movedXMarkerStartValueIndex,
												final int movedXMarkerEndValueIndex) {

//		final EntityManager em = TourDatabase.getInstance().getEntityManager();
//		final EntityTransaction ts = em.getTransaction();
//		float tourSpeed = 0;
//
//		try {
//			final TourCompared compTour = em.find(TourCompared.class,
//					fActiveComparedTour.getCompId());
//
//			if (compTour != null) {
//
//				// update the changed x-marker index
//				compTour.setStartIndex(movedXMarkerStartValueIndex);
//				compTour.setEndIndex(movedXMarkerEndValueIndex);
//
//				// update the changed tour speed
//				final ChartDataModel chartDataModel = fTourChart.getChartDataModel();
//
//				final int[] distanceValues = ((ChartDataXSerie) chartDataModel.getCustomData(TourManager.CUSTOM_DATA_DISTANCE)).getHighValues()[0];
//
//				final int[] timeValues = ((ChartDataXSerie) chartDataModel.getCustomData(TourManager.CUSTOM_DATA_TIME)).getHighValues()[0];
//
//				final int distance = distanceValues[movedXMarkerEndValueIndex]
//						- distanceValues[movedXMarkerStartValueIndex];
//				int time = timeValues[movedXMarkerEndValueIndex]
//						- timeValues[movedXMarkerStartValueIndex];
//
//				// adjust the time by removing the breaks
//				final int timeInterval = timeValues[1] - timeValues[0];
//				int ignoreTimeSlices = TourManager.getInstance().getIgnoreTimeSlices(timeValues,
//						movedXMarkerStartValueIndex,
//						movedXMarkerEndValueIndex,
//						10 / timeInterval);
//				time = time - (ignoreTimeSlices * timeInterval);
//
//				tourSpeed = compTour.setTourSpeed(distance, time);
//
//				// update the entity
//				ts.begin();
//				em.merge(compTour);
//				ts.commit();
//			}
//		} catch (final Exception e) {
//			e.printStackTrace();
//		} finally {
//			if (ts.isActive()) {
//				ts.rollback();
//			}
//			em.close();
//		}
//
//		// find the changed compared tour
//		fComparedToursFindResult = new ArrayList<TVTITourMapComparedTour>();
//		final ArrayList<Long> findCompIds = new ArrayList<Long>();
//		findCompIds.add(fActiveComparedTour.getCompId());
//		findComparedTours(((TourContentProvider) fTourViewer.getContentProvider()).getRootItem(),
//				findCompIds);
//
//		// update the data in the data model
//		final TVTITourMapComparedTour ttiComparedTour = fComparedToursFindResult.get(0);
//		ttiComparedTour.setStartIndex(movedXMarkerStartValueIndex);
//		ttiComparedTour.setEndIndex(movedXMarkerEndValueIndex);
//		ttiComparedTour.setTourSpeed(tourSpeed);
//
//		// update the chart
//		final ChartDataModel chartDataModel = fTourChart.getChartDataModel();
//		final ChartDataXSerie xData = chartDataModel.getXData();
//		xData.setMarkerValueIndex(movedXMarkerStartValueIndex, movedXMarkerEndValueIndex);
//		fTourChart.updateChart(chartDataModel);
//
//		// update the tour viewer
////		fTourViewer.update(fComparedToursFindResult.toArray(), null);
//
//		// force the year chart to be refreshed
////		fYearChartYear = -1;
//
//		// reset the min/max size in the year view
////		if (ttiComparedTour.getParentItem() instanceof TVITourMapYear) {
////			final TVITourMapYear ttiTourMapYear = (TVITourMapYear) ttiComparedTour.getParentItem();
////			final TVTITourMapReferenceTour refItem = ttiTourMapYear.getRefItem();
////			refItem.yearMapMinValue = Integer.MIN_VALUE;
////
////			updateYearBarChart(ttiTourMapYear);
////		}
	}

}
