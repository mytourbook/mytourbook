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
package net.tourbook.ui.views.tourCatalog;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import net.tourbook.Messages;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.IChartListener;
import net.tourbook.chart.ISliderMoveListener;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.data.TourCompared;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.tour.IDataModelListener;
import net.tourbook.tour.ITourPropertyListener;
import net.tourbook.tour.SelectionTourChart;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.TourChart;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.views.TourChartViewPart;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.PageBook;

// author: Wolfgang Schramm
// create: 06.09.2007

public class TourCatalogViewComparedTour extends TourChartViewPart implements ISynchedChart {

	public static final String					ID				= "net.tourbook.views.tourCatalog.comparedTourView";	//$NON-NLS-1$

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
	 * key for the {@link TourCompared} instance or <code>-1</code> when it's not saved in the
	 * database
	 */
	private long								fCTCompareId	= -1;

	/**
	 * Tour Id for the displayed compared tour
	 */
	private long								fCTTourId		= -1;

	/**
	 * Reference Id for the displayed compared tour
	 */
	private long								fCTRefId		= -1;

	/**
	 * Reference tour chart for the displayed compared tour, chart is used for the synchronization
	 */
	private TourChart							fCTRefTourChart;

	private PageBook							fPageBook;
	private Label								fPageNoChart;

	private ITourPropertyListener				fRefTourPropertyListener;

	private ActionSynchChartHorizontalByScale	fActionSynchChartsByScale;
	private ActionSynchChartHorizontalBySize	fActionSynchChartsBySize;

	private ActionSaveComparedTour				fActionSaveComparedTour;
	private ActionUndoChanges					fActionUndoChanges;

	private boolean								fIsDataDirty;

	/*
	 * 3 positons for the marker are available: computed, default(saved) and moved
	 */
	private int									fMovedStartIndex;
	private int									fMovedEndIndex;

	private int									fComputedStartIndex;
	private int									fComputedEndIndex;

	private int									fDefaultStartIndex;
	private int									fDefaultEndIndex;

	/**
	 * object for the currently displayed compared tour
	 */
	private Object								fComparedTourItem;

	private class ActionSaveComparedTour extends Action {

		public ActionSaveComparedTour() {

			super(null, AS_PUSH_BUTTON);

			setToolTipText(Messages.tourCatalog_view_action_save_marker);

			setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__save));
			setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__save_disabled));

			setEnabled(false);
		}

		@Override
		public void run() {
			saveComparedTour();
		}
	}

	private class ActionUndoChanges extends Action {

		public ActionUndoChanges() {

			super(null, AS_PUSH_BUTTON);

			setToolTipText(Messages.tourCatalog_view_action_undo_marker_position);

			setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__undo_edit));
			setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__undo_edit_disabled));

			setEnabled(false);
		}

		@Override
		public void run() {
			undoChanges();
		}
	}

	private void addRefTourPropertyListener() {

		fRefTourPropertyListener = new ITourPropertyListener() {
			public void propertyChanged(final int propertyId, final Object propertyData) {

				if (propertyId == TourManager.TOUR_PROPERTY_REFERENCE_TOUR_CHANGED
						&& propertyData instanceof TourPropertyRefTourChanged) {

					/*
					 * reference tour changed
					 */

					final TourPropertyRefTourChanged tourProperty = (TourPropertyRefTourChanged) propertyData;

					fRefTourRefId = tourProperty.refId;
					fRefTourTourChart = tourProperty.refTourChart;
					fRefTourXMarkerValueDifference = tourProperty.xMarkerValue;

					if (updateTourChart() == false) {
						enableSynchronization();
					}
				}
			}
		};

		TourManager.getInstance().addPropertyListener(fRefTourPropertyListener);
	}

	private void createActions() {

		fActionSynchChartsBySize = new ActionSynchChartHorizontalBySize(this);
		fActionSynchChartsByScale = new ActionSynchChartHorizontalByScale(this);

		fActionSaveComparedTour = new ActionSaveComparedTour();
		fActionUndoChanges = new ActionUndoChanges();

		final IToolBarManager tbm = fTourChart.getToolBarManager();

		tbm.add(fActionSaveComparedTour);
		tbm.add(fActionUndoChanges);

		tbm.add(new Separator());
		tbm.add(fActionSynchChartsByScale);
		tbm.add(fActionSynchChartsBySize);

		tbm.update(true);
	}

	@Override
	public void createPartControl(final Composite parent) {

		super.createPartControl(parent);

		fPageBook = new PageBook(parent, SWT.NONE);

		fPageNoChart = new Label(fPageBook, SWT.NONE);
		fPageNoChart.setText(Messages.UI_Label_no_chart_is_selected);

		createTourChart();
		createActions();

		addRefTourPropertyListener();

		// show current selected tour
		final ISelection selection = getSite().getWorkbenchWindow().getSelectionService().getSelection();
		if (selection != null) {
			onSelectionChanged(selection);
		} else {
			fPageBook.showPage(fPageNoChart);
		}

		enableSynchronization();
	}

	private void createTourChart() {

		fTourChart = new TourChart(fPageBook, SWT.FLAT, true);
		fTourChart.setShowZoomActions(true);
		fTourChart.setShowSlider(true);
		fTourChart.setToolBarManager(getViewSite().getActionBars().getToolBarManager(), true);

		fTourChart.addDoubleClickListener(new Listener() {
			public void handleEvent(final Event event) {
				TourManager.getInstance().openTourInEditor(fTourData.getTourId());
			}
		});

		// fire a slider move selection when a slider was moved in the tour chart
		fTourChart.addSliderMoveListener(new ISliderMoveListener() {
			public void sliderMoved(final SelectionChartInfo chartInfoSelection) {
				fPostSelectionProvider.setSelection(chartInfoSelection);
			}
		});

		fTourChart.addDataModelListener(new IDataModelListener() {
			public void dataModelChanged(final ChartDataModel changedChartDataModel) {

				final ChartDataXSerie xData = changedChartDataModel.getXData();

				/*
				 * set synch marker position, this method is also called when a graph is
				 * displayed/removed
				 */
				xData.setSynchMarkerValueIndex(fMovedStartIndex, fMovedEndIndex);

				setRangeMarkers(xData);

				// set chart title
				changedChartDataModel.setTitle(TourManager.getTourTitleDetailed(fTourData));
			}
		});

		fTourChart.addXMarkerDraggingListener(new IChartListener() {

			public int getXMarkerValueDiff() {
				return fRefTourXMarkerValueDifference;
			}

			public void xMarkerMoved(final int movedXMarkerStartValueIndex, final int movedXMarkerEndValueIndex) {
				onMoveSynchedMarker(movedXMarkerStartValueIndex, movedXMarkerEndValueIndex);
			}
		});
	}

	@Override
	public void dispose() {

		saveComparedTourDialog();

		TourManager.getInstance().removePropertyListener(fRefTourPropertyListener);

		super.dispose();
	}

	private void enableSaveAction() {

		final boolean isNotMoved = fDefaultStartIndex == fMovedStartIndex && fDefaultEndIndex == fMovedEndIndex;

		fActionSaveComparedTour.setEnabled(isNotMoved == false || fCTCompareId == -1);
	}

	private void enableSynchronization() {

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
				fCTRefTourChart.synchChart(false, fTourChart, Chart.SYNCH_MODE_NO);
			}
			fActionSynchChartsByScale.setChecked(false);
			fActionSynchChartsBySize.setChecked(false);
		}

		fActionSynchChartsByScale.setEnabled(isSynchEnabled);
		fActionSynchChartsBySize.setEnabled(isSynchEnabled);
	}

	/**
	 * update tour map and compare result view
	 */
	private void fireChangeEvent(final int startIndex, final int endIndex) {

		final float speed = TourManager.computeTourSpeed(fTourData, startIndex, endIndex);

		fireChangeEvent(startIndex, endIndex, speed, false);
	}

	/**
	 * update tour map and compare result view
	 * 
	 * @param startIndex
	 * @param endIndex
	 * @param speed
	 * @param isDataSaved
	 */
	private void fireChangeEvent(final int startIndex, final int endIndex, final float speed, final boolean isDataSaved) {

		TourManager.firePropertyChange(TourManager.TOUR_PROPERTY_COMPARE_TOUR_CHANGED,
				new TourPropertyCompareTourChanged(fCTCompareId,
						startIndex,
						endIndex,
						speed,
						isDataSaved,
						fComparedTourItem));
	}

	private void onMoveSynchedMarker(final int movedValueIndex, final int movedEndIndex) {

		// update the chart
		final ChartDataModel chartDataModel = fTourChart.getChartDataModel();
		final ChartDataXSerie xData = chartDataModel.getXData();

		xData.setSynchMarkerValueIndex(movedValueIndex, movedEndIndex);
		setRangeMarkers(xData);

		fTourChart.updateChart(chartDataModel);

		// keep marker position for saving the tour
		fMovedStartIndex = movedValueIndex;
		fMovedEndIndex = movedEndIndex;

		// check if the data are dirty
		boolean isDataDirty;
		if (fDefaultStartIndex == fMovedStartIndex && fDefaultEndIndex == fMovedEndIndex) {
			isDataDirty = false;
		} else {
			isDataDirty = true;
		}
		setDataDirty(isDataDirty);

		fireChangeEvent(fMovedStartIndex, fMovedEndIndex);
	}

	private void onSelectionChanged(final ISelection selection) {

		if (selection instanceof StructuredSelection) {

			final Object firstElement = ((StructuredSelection) selection).getFirstElement();

			if (firstElement instanceof TVICatalogComparedTour) {

				updateTourChart((TVICatalogComparedTour) firstElement);

			} else if (firstElement instanceof TVICompareResultComparedTour) {

				updateTourChart((TVICompareResultComparedTour) firstElement);
			}
		}
	}

	@Override
	protected void onSelectionChanged(final IWorkbenchPart part, final ISelection selection) {

//		if (fIsDataDirty && part != TourCatalogViewComparedTour.this) {
//			return;
//		}

		onSelectionChanged(selection);
	}

	/**
	 * Persist the compared tours
	 */
	private void persistComparedTour() {

		final EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em != null) {

			final EntityTransaction ts = em.getTransaction();

			try {

				if (fComparedTourItem instanceof TVICompareResultComparedTour) {

					final TVICompareResultComparedTour comparedTourItem = (TVICompareResultComparedTour) fComparedTourItem;

					TourCompareManager.saveComparedTourItem(comparedTourItem, em, ts);

					fCTCompareId = comparedTourItem.compId;

					// update tour map view
					final SelectionPersistedCompareResults persistedCompareResults = new SelectionPersistedCompareResults();
					persistedCompareResults.persistedCompareResults.add(comparedTourItem);

					fPostSelectionProvider.setSelection(persistedCompareResults);
				}

			} catch (final Exception e) {
				e.printStackTrace();
			} finally {
				if (ts.isActive()) {
					ts.rollback();
				}
				em.close();
			}
		}
	}

	private void saveComparedTour() {

//		if (fIsDataDirty == false) {
//			return;
//		}

		if (fCTCompareId == -1) {
			persistComparedTour();
		}

		final EntityManager em = TourDatabase.getInstance().getEntityManager();
		final EntityTransaction ts = em.getTransaction();

		try {
			final TourCompared comparedTour = em.find(TourCompared.class, fCTCompareId);

			if (comparedTour != null) {

				final ChartDataModel chartDataModel = fTourChart.getChartDataModel();

				final float speed = TourManager.computeTourSpeed(fTourData, fMovedStartIndex, fMovedEndIndex);

				// set new data in entity
				comparedTour.setStartIndex(fMovedStartIndex);
				comparedTour.setEndIndex(fMovedEndIndex);
				comparedTour.setTourSpeed(speed);

				// update entity
				ts.begin();
				em.merge(comparedTour);
				ts.commit();

				fCTCompareId = comparedTour.getComparedId();

				setDataDirty(false);

				/*
				 * update chart and viewer with new marker position
				 */
				fDefaultStartIndex = fMovedStartIndex;
				fDefaultEndIndex = fMovedEndIndex;

				final ChartDataXSerie xData = chartDataModel.getXData();
				xData.setSynchMarkerValueIndex(fDefaultStartIndex, fDefaultEndIndex);
				setRangeMarkers(xData);

				fTourChart.updateChart(chartDataModel);
				enableSaveAction();

				fireChangeEvent(fDefaultStartIndex, fDefaultEndIndex, speed, true);
			}
		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			if (ts.isActive()) {
				ts.rollback();
			}
			em.close();
		}
	}

	/**
	 * @return Returns <code>false</code> when the save dialog was canceled
	 */
	private boolean saveComparedTourDialog() {

		if (fCTCompareId == -1) {
			setDataDirty(false);
			return true;
		}

		if (fIsDataDirty == false) {
			return true;
		}

		final MessageBox msgBox = new MessageBox(Display.getDefault().getActiveShell(), SWT.ICON_QUESTION
				| SWT.YES
				| SWT.NO/*
						 * | SWT.CANCEL
						 */);

		msgBox.setText(Messages.tourCatalog_view_dlg_save_compared_tour_title);
		msgBox.setMessage(NLS.bind(Messages.tourCatalog_view_dlg_save_compared_tour_message,
				TourManager.getTourTitleDetailed(fTourData)));

		final int answer = msgBox.open();

		if (answer == SWT.YES) {
			saveComparedTour();
//		} else if (answer == SWT.CANCEL) {
// disabled, pop up for every selection when multiple selections are fired
//			return false;
		} else {
			fireChangeEvent(fComputedStartIndex, fComputedEndIndex);
		}

		setDataDirty(false);

		return true;
	}

	private void setDataDirty(final boolean isDirty) {

		fIsDataDirty = isDirty;

		enableSaveAction();

		fActionUndoChanges.setEnabled(isDirty);
	}

	@Override
	public void setFocus() {
		fTourChart.setFocus();

		fPostSelectionProvider.setSelection(new SelectionTourChart(fTourChart));
	}

	private void setRangeMarkers(final ChartDataXSerie xData) {

		if (fComparedTourItem instanceof TVICatalogComparedTour) {

			xData.setRangeMarkers(new int[] { fDefaultStartIndex }, new int[] { fDefaultEndIndex });

		} else if (fComparedTourItem instanceof TVICompareResultComparedTour) {

			xData.setRangeMarkers(new int[] { fDefaultStartIndex, fComputedStartIndex }, new int[] {
					fDefaultEndIndex,
					fComputedEndIndex });
		}
	}

	public void synchCharts(final boolean isSynched, final int synchMode) {

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

			fCTRefTourChart.synchChart(isSynched, fTourChart, synchMode);
		}
	}

	private void undoChanges() {

		// set synch marker to original position
		final ChartDataModel chartDataModel = fTourChart.getChartDataModel();
		final ChartDataXSerie xData = chartDataModel.getXData();

		fMovedStartIndex = fDefaultStartIndex;
		fMovedEndIndex = fDefaultEndIndex;

		xData.setSynchMarkerValueIndex(fDefaultStartIndex, fDefaultEndIndex);

		fTourChart.updateChart(chartDataModel);

		setDataDirty(false);

		fireChangeEvent(fDefaultStartIndex, fDefaultEndIndex);
	}

	@Override
	protected void updateChart() {

		if (fTourData == null) {
			return;
		}

		fTourChart.updateTourChart(fTourData, fTourChartConfig, false);

		fPageBook.showPage(fTourChart);

		// set application window title
		setTitleToolTip(TourManager.getTourDate(fTourData));
	}

	/**
	 * @return Returns <code>false</code> when the compared tour was not displayed
	 */
	private boolean updateTourChart() {

		// check if the compared tour is displayed
//		if (fVisibleComparedTourId == fCTTourId) {
//			return false;
//		}

		final TourCompareConfig tourCompareConfig = ReferenceTourManager.getInstance().getTourCompareConfig(fCTRefId);

		if (tourCompareConfig != null) {

//			fVisibleComparedTourId = fCTTourId;

			fTourChartConfig = tourCompareConfig.getCompTourChartConfig();

			fTourChartConfig.setMinMaxKeeper(true);
			fTourChartConfig.canShowTourCompareGraph = true;

			updateChart();
			enableSynchronization();
			enableSaveAction();

			/*
			 * fire change event to update tour markers
			 */
			fPostSelectionProvider.setSelection(new SelectionTourData(fTourChart, fTourChart.getTourData()));

			return true;
		}

		return false;
	}

	/**
	 * Shows the compared tour which was selected by the user in the {@link TourCatalogView}
	 * 
	 * @param selectionComparedTour
	 */
	private void updateTourChart(final TVICatalogComparedTour itemComparedTour) {

		if (saveComparedTourDialog() == false) {
			return;
		}

		final Long ctTourId = itemComparedTour.getTourId();

		// check if the compared tour is already displayed
		if (fCTTourId == ctTourId.longValue() && fComparedTourItem instanceof TVICatalogComparedTour) {
			return;
		}

		// load the tourdata of the compared tour from the database
		final TourData compTourData = TourManager.getInstance().getTourData(ctTourId);
		if (compTourData == null) {
			return;
		}

		// set data from the selection
		fCTTourId = ctTourId;
		fCTRefId = itemComparedTour.getRefId();
		fCTCompareId = itemComparedTour.getCompId();

		fTourData = compTourData;

		/*
		 * remove tour compare data (when there are any), but set dummy object to display the action
		 * button
		 */
		fTourData.tourCompareSerie = new int[0];

		fDefaultStartIndex = fMovedStartIndex = fComputedStartIndex = itemComparedTour.getStartIndex();
		fDefaultEndIndex = fMovedEndIndex = fComputedEndIndex = itemComparedTour.getEndIndex();

		fComparedTourItem = itemComparedTour;

		updateTourChart();

		// disable action after the chart was created
		fTourChart.enableGraphAction(TourManager.GRAPH_TOUR_COMPARE, false);
	}

	private void updateTourChart(final TVICompareResultComparedTour compareResultItem) {

		if (saveComparedTourDialog() == false) {
			return;
		}

		final Long ctTourId = compareResultItem.comparedTourData.getTourId();

		// check if the compared tour is already displayed
		if (fCTTourId == ctTourId && fComparedTourItem instanceof TVICompareResultComparedTour) {
			return;
		}

		// load the tourdata of the compared tour from the database
		final TourData compTourData = TourManager.getInstance().getTourData(ctTourId);
		if (compTourData == null) {
			return;
		}

		// keep data from the selected compared tour
		fCTTourId = ctTourId;
		fCTRefId = compareResultItem.refTour.getRefId();
		fCTCompareId = compareResultItem.compId;

		fTourData = compTourData;

		// set tour compare data, this will show the action button to see the graph for this data
		fTourData.tourCompareSerie = compareResultItem.altitudeDiffSerie;

		if (fCTCompareId == -1) {

			// compared tour is not saved

			fDefaultStartIndex = fComputedStartIndex = fMovedStartIndex = compareResultItem.computedStartIndex;
			fDefaultEndIndex = fComputedEndIndex = fMovedEndIndex = compareResultItem.computedEndIndex;

		} else {

			// compared tour is saved

			fDefaultStartIndex = fMovedStartIndex = compareResultItem.dbStartIndex;
			fDefaultEndIndex = fMovedEndIndex = compareResultItem.dbEndIndex;

			fComputedStartIndex = compareResultItem.computedStartIndex;
			fComputedEndIndex = compareResultItem.computedEndIndex;
		}

		fComparedTourItem = compareResultItem;

		updateTourChart();

		// enable action after the chart was created
		fTourChart.enableGraphAction(TourManager.GRAPH_TOUR_COMPARE, true);
	}

}
