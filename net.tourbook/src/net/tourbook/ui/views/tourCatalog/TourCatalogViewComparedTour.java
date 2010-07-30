/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.IChartListener;
import net.tourbook.chart.ISliderMoveListener;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.data.TourCompared;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.IDataModelListener;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionTourChart;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourChartViewer;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.tourChart.TourChartContextProvicer;
import net.tourbook.ui.tourChart.TourChartViewPart;

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

public class TourCatalogViewComparedTour extends TourChartViewPart implements ISynchedChart, ITourChartViewer {

	public static final String					ID				= "net.tourbook.views.tourCatalog.comparedTourView";	//$NON-NLS-1$

	/*
	 * keep data from the reference tour view
	 */
	private long								_refTourRefId	= -1;
	private TourChart							_refTourTourChart;
	private int									_refTourXMarkerValueDifference;

	/*
	 * CT ... (c)ompared (t)our which is displayed in this view
	 */

	/**
	 * key for the {@link TourCompared} instance or <code>-1</code> when it's not saved in the
	 * database
	 */
	private long								_ctCompareId	= -1;

	/**
	 * Tour Id for the displayed compared tour
	 */
	private long								_ctTourId		= -1;

	/**
	 * Reference Id for the displayed compared tour
	 */
	private long								_ctRefId		= -1;

	/**
	 * Reference tour chart for the displayed compared tour, chart is used for the synchronization
	 */
	private TourChart							_ctRefTourChart;

	private PageBook							_pageBook;
	private Label								_pageNoChart;

	private ITourEventListener					_refTourPropertyListener;

	private ActionSynchChartHorizontalByScale	_actionSynchChartsByScale;
	private ActionSynchChartHorizontalBySize	_actionSynchChartsBySize;

	private ActionSaveComparedTour				_actionSaveComparedTour;
	private ActionUndoChanges					_actionUndoChanges;

	private boolean								_isDataDirty;

	/*
	 * 3 positons for the marker are available: computed, default(saved) and moved
	 */
	private int									_movedStartIndex;
	private int									_movedEndIndex;

	private int									_computedStartIndex;
	private int									_computedEndIndex;

	private int									_defaultStartIndex;
	private int									_defaultEndIndex;

	/**
	 * object for the currently displayed compared tour
	 */
	private Object								_comparedTourItem;

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

		_refTourPropertyListener = new ITourEventListener() {
			public void tourChanged(final IWorkbenchPart part, final TourEventId propertyId, final Object propertyData) {

				if (propertyId == TourEventId.REFERENCE_TOUR_CHANGED
						&& propertyData instanceof TourPropertyRefTourChanged) {

					/*
					 * reference tour changed
					 */

					final TourPropertyRefTourChanged tourProperty = (TourPropertyRefTourChanged) propertyData;

					_refTourRefId = tourProperty.refId;
					_refTourTourChart = tourProperty.refTourChart;
					_refTourXMarkerValueDifference = tourProperty.xMarkerValue;

					if (updateTourChart() == false) {
						enableSynchronization();
					}
				}
			}
		};

		TourManager.getInstance().addTourEventListener(_refTourPropertyListener);
	}

	private void createActions() {

		_actionSynchChartsBySize = new ActionSynchChartHorizontalBySize(this);
		_actionSynchChartsByScale = new ActionSynchChartHorizontalByScale(this);

		_actionSaveComparedTour = new ActionSaveComparedTour();
		_actionUndoChanges = new ActionUndoChanges();

		final IToolBarManager tbm = _tourChart.getToolBarManager();

		tbm.add(_actionSaveComparedTour);
		tbm.add(_actionUndoChanges);

		tbm.add(new Separator());
		tbm.add(_actionSynchChartsByScale);
		tbm.add(_actionSynchChartsBySize);

		tbm.update(true);
	}

	@Override
	public void createPartControl(final Composite parent) {

		super.createPartControl(parent);

		_pageBook = new PageBook(parent, SWT.NONE);

		_pageNoChart = new Label(_pageBook, SWT.NONE);
		_pageNoChart.setText(Messages.UI_Label_no_chart_is_selected);

		createTourChart();
		createActions();

		addRefTourPropertyListener();

		_pageBook.showPage(_pageNoChart);

		// show current selected tour
		final ISelection selection = getSite().getWorkbenchWindow().getSelectionService().getSelection();
		if (selection != null) {
			onSelectionChanged(selection);
		}

		enableSynchronization();
	}

	private void createTourChart() {

		_tourChart = new TourChart(_pageBook, SWT.FLAT, true);
		_tourChart.setShowZoomActions(true);
		_tourChart.setShowSlider(true);
		_tourChart.setToolBarManager(getViewSite().getActionBars().getToolBarManager(), true);
		_tourChart.setContextProvider(new TourChartContextProvicer(this));
		_tourChart.setTourInfoActionsEnabled(true);

		_tourChart.addDoubleClickListener(new Listener() {
			public void handleEvent(final Event event) {
				TourManager.getInstance().openTourInEditorArea(_tourData.getTourId());
			}
		});

		// fire a slider move selection when a slider was moved in the tour chart
		_tourChart.addSliderMoveListener(new ISliderMoveListener() {
			public void sliderMoved(final SelectionChartInfo chartInfoSelection) {
				_postSelectionProvider.setSelection(chartInfoSelection);
			}
		});

		_tourChart.addDataModelListener(new IDataModelListener() {
			public void dataModelChanged(final ChartDataModel changedChartDataModel) {

				if (_tourData == null) {
					return;
				}

				final ChartDataXSerie xData = changedChartDataModel.getXData();

				/*
				 * set synch marker position, this method is also called when a graph is
				 * displayed/removed
				 */
				xData.setSynchMarkerValueIndex(_movedStartIndex, _movedEndIndex);

				setRangeMarkers(xData);

				// set chart title
				changedChartDataModel.setTitle(TourManager.getTourTitleDetailed(_tourData));
			}
		});

		_tourChart.addXMarkerDraggingListener(new IChartListener() {

			public int getXMarkerValueDiff() {
				return _refTourXMarkerValueDifference;
			}

			public void xMarkerMoved(final int movedXMarkerStartValueIndex, final int movedXMarkerEndValueIndex) {
				onMoveSynchedMarker(movedXMarkerStartValueIndex, movedXMarkerEndValueIndex);
			}
		});
	}

	@Override
	public void dispose() {

		saveComparedTourDialog();

		TourManager.getInstance().removeTourEventListener(_refTourPropertyListener);

		super.dispose();
	}

	private void enableSaveAction() {

		final boolean isNotMoved = _defaultStartIndex == _movedStartIndex && _defaultEndIndex == _movedEndIndex;

		_actionSaveComparedTour.setEnabled(isNotMoved == false || _ctCompareId == -1);
	}

	private void enableSynchronization() {

		// check initial value
		if (_ctRefId == -1) {
			_actionSynchChartsByScale.setEnabled(false);
			_actionSynchChartsBySize.setEnabled(false);
			return;
		}

		boolean isSynchEnabled = false;

		if (_ctRefId == _refTourRefId) {

			// reference tour for the compared chart is displayed

			if (_ctRefTourChart != _refTourTourChart) {
				_ctRefTourChart = _refTourTourChart;
			}

			isSynchEnabled = true;

		} else {

			// another ref tour is displayed, disable synchronization

			if (_ctRefTourChart != null) {
				_ctRefTourChart.synchChart(false, _tourChart, Chart.SYNCH_MODE_NO);
			}
			_actionSynchChartsByScale.setChecked(false);
			_actionSynchChartsBySize.setChecked(false);
		}

		_actionSynchChartsByScale.setEnabled(isSynchEnabled);
		_actionSynchChartsBySize.setEnabled(isSynchEnabled);
	}

	/**
	 * update tour map and compare result view
	 */
	private void fireChangeEvent(final int startIndex, final int endIndex) {

		final float speed = TourManager.computeTourSpeed(_tourData, startIndex, endIndex);

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

		TourManager.fireEvent(TourEventId.COMPARE_TOUR_CHANGED, new TourPropertyCompareTourChanged(
				_ctCompareId,
				startIndex,
				endIndex,
				speed,
				isDataSaved,
				_comparedTourItem), this);
	}

	public ArrayList<TourData> getSelectedTours() {
		final ArrayList<TourData> selectedTours = new ArrayList<TourData>();
		selectedTours.add(_tourData);
		return selectedTours;
	}

	public TourChart getTourChart() {
		return _tourChart;
	}

	private void onMoveSynchedMarker(final int movedValueIndex, final int movedEndIndex) {

		// update the chart
		final ChartDataModel chartDataModel = _tourChart.getChartDataModel();
		final ChartDataXSerie xData = chartDataModel.getXData();

		xData.setSynchMarkerValueIndex(movedValueIndex, movedEndIndex);
		setRangeMarkers(xData);

		_tourChart.updateChart(chartDataModel, true);

		// keep marker position for saving the tour
		_movedStartIndex = movedValueIndex;
		_movedEndIndex = movedEndIndex;

		// check if the data are dirty
		boolean isDataDirty;
		if (_defaultStartIndex == _movedStartIndex && _defaultEndIndex == _movedEndIndex) {
			isDataDirty = false;
		} else {
			isDataDirty = true;
		}
		setDataDirty(isDataDirty);

		fireChangeEvent(_movedStartIndex, _movedEndIndex);
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

		if (part == TourCatalogViewComparedTour.this) {
			return;
		}

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

				if (_comparedTourItem instanceof TVICompareResultComparedTour) {

					final TVICompareResultComparedTour comparedTourItem = (TVICompareResultComparedTour) _comparedTourItem;

					TourCompareManager.saveComparedTourItem(comparedTourItem, em, ts);

					_ctCompareId = comparedTourItem.compId;

					// update tour map view
					final SelectionPersistedCompareResults persistedCompareResults = new SelectionPersistedCompareResults();
					persistedCompareResults.persistedCompareResults.add(comparedTourItem);

					_postSelectionProvider.setSelection(persistedCompareResults);
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

		if (_ctCompareId == -1) {
			persistComparedTour();
		}

		final EntityManager em = TourDatabase.getInstance().getEntityManager();
		final EntityTransaction ts = em.getTransaction();

		try {
			final TourCompared comparedTour = em.find(TourCompared.class, _ctCompareId);

			if (comparedTour != null) {

				final ChartDataModel chartDataModel = _tourChart.getChartDataModel();

				final float speed = TourManager.computeTourSpeed(_tourData, _movedStartIndex, _movedEndIndex);

				// set new data in entity
				comparedTour.setStartIndex(_movedStartIndex);
				comparedTour.setEndIndex(_movedEndIndex);
				comparedTour.setTourSpeed(speed);

				// update entity
				ts.begin();
				em.merge(comparedTour);
				ts.commit();

				_ctCompareId = comparedTour.getComparedId();

				setDataDirty(false);

				/*
				 * update chart and viewer with new marker position
				 */
				_defaultStartIndex = _movedStartIndex;
				_defaultEndIndex = _movedEndIndex;

				final ChartDataXSerie xData = chartDataModel.getXData();
				xData.setSynchMarkerValueIndex(_defaultStartIndex, _defaultEndIndex);
				setRangeMarkers(xData);

				_tourChart.updateChart(chartDataModel, true);
				enableSaveAction();

				fireChangeEvent(_defaultStartIndex, _defaultEndIndex, speed, true);
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

		if (_ctCompareId == -1) {
			setDataDirty(false);
			return true;
		}

		if (_isDataDirty == false) {
			return true;
		}

		final MessageBox msgBox = new MessageBox(Display.getDefault().getActiveShell(), //
				SWT.ICON_QUESTION | SWT.YES | SWT.NO);

		msgBox.setText(Messages.tourCatalog_view_dlg_save_compared_tour_title);
		msgBox.setMessage(NLS.bind(
				Messages.tourCatalog_view_dlg_save_compared_tour_message,
				TourManager.getTourTitleDetailed(_tourData)));

		final int answer = msgBox.open();

		if (answer == SWT.YES) {
			saveComparedTour();
//		} else if (answer == SWT.CANCEL) {
// disabled, pops up for every selection when multiple selections are fired
//			return false;
		} else {
			fireChangeEvent(_computedStartIndex, _computedEndIndex);
		}

		setDataDirty(false);

		return true;
	}

	private void setDataDirty(final boolean isDirty) {

		_isDataDirty = isDirty;

		enableSaveAction();

		_actionUndoChanges.setEnabled(isDirty);
	}

	@Override
	public void setFocus() {
		_tourChart.setFocus();

		_postSelectionProvider.setSelection(new SelectionTourChart(_tourChart));
	}

	private void setRangeMarkers(final ChartDataXSerie xData) {

		if (_comparedTourItem instanceof TVICatalogComparedTour) {

			xData.setRangeMarkers(new int[] { _defaultStartIndex }, new int[] { _defaultEndIndex });

		} else if (_comparedTourItem instanceof TVICompareResultComparedTour) {

			xData.setRangeMarkers(new int[] { _defaultStartIndex, _computedStartIndex }, new int[] {
					_defaultEndIndex,
					_computedEndIndex });
		}
	}

	public void synchCharts(final boolean isSynched, final int synchMode) {

		if (_ctRefTourChart != null) {

			// uncheck other synch mode
			switch (synchMode) {
			case Chart.SYNCH_MODE_BY_SCALE:
				_actionSynchChartsBySize.setChecked(false);
				break;

			case Chart.SYNCH_MODE_BY_SIZE:
				_actionSynchChartsByScale.setChecked(false);
				break;

			default:
				break;
			}

			_ctRefTourChart.synchChart(isSynched, _tourChart, synchMode);
		}
	}

	private void undoChanges() {

		// set synch marker to original position
		final ChartDataModel chartDataModel = _tourChart.getChartDataModel();
		final ChartDataXSerie xData = chartDataModel.getXData();

		_movedStartIndex = _defaultStartIndex;
		_movedEndIndex = _defaultEndIndex;

		xData.setSynchMarkerValueIndex(_defaultStartIndex, _defaultEndIndex);

		_tourChart.updateChart(chartDataModel, true);

		setDataDirty(false);

		fireChangeEvent(_defaultStartIndex, _defaultEndIndex);
	}

	@Override
	protected void updateChart() {

		if (_tourData == null) {

			_refTourRefId = -1;

			_ctTourId = -1;
			_ctRefId = -1;
			_ctCompareId = -1;

			_pageBook.showPage(_pageNoChart);

			return;
		}

		_tourChart.updateTourChart(_tourData, _tourChartConfig, false);

		_pageBook.showPage(_tourChart);

		// set application window title
		setTitleToolTip(TourManager.getTourDateShort(_tourData));
	}

	/**
	 * @return Returns <code>false</code> when the compared tour was not displayed
	 */
	private boolean updateTourChart() {

		// check if the compared tour is displayed
//		if (fVisibleComparedTourId == fCTTourId) {
//			return false;
//		}

		final TourCompareConfig tourCompareConfig = ReferenceTourManager.getInstance().getTourCompareConfig(_ctRefId);

		if (tourCompareConfig != null) {

//			fVisibleComparedTourId = fCTTourId;

			_tourChartConfig = tourCompareConfig.getCompTourChartConfig();

			_tourChartConfig.setMinMaxKeeper(true);
			_tourChartConfig.canShowTourCompareGraph = true;

			updateChart();
			enableSynchronization();
			enableSaveAction();

			/*
			 * fire change event to update tour markers
			 */
			_postSelectionProvider.setSelection(new SelectionTourData(_tourChart, _tourChart.getTourData()));

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
		if (_ctTourId == ctTourId.longValue() && _comparedTourItem instanceof TVICatalogComparedTour) {
			return;
		}

		// load the tourdata of the compared tour from the database
		final TourData compTourData = TourManager.getInstance().getTourData(ctTourId);
		if (compTourData == null) {
			return;
		}

		// set data from the selection
		_ctTourId = ctTourId;
		_ctRefId = itemComparedTour.getRefId();
		_ctCompareId = itemComparedTour.getCompId();

		_tourData = compTourData;

		/*
		 * remove tour compare data (when there are any), but set dummy object to display the action
		 * button
		 */
		_tourData.tourCompareSerie = new int[0];

		_defaultStartIndex = _movedStartIndex = _computedStartIndex = itemComparedTour.getStartIndex();
		_defaultEndIndex = _movedEndIndex = _computedEndIndex = itemComparedTour.getEndIndex();

		_comparedTourItem = itemComparedTour;

		updateTourChart();

		// disable action after the chart was created
		_tourChart.enableGraphAction(TourManager.GRAPH_TOUR_COMPARE, false);
	}

	private void updateTourChart(final TVICompareResultComparedTour compareResultItem) {

		if (saveComparedTourDialog() == false) {
			return;
		}

		final Long ctTourId = compareResultItem.comparedTourData.getTourId();

		// check if the compared tour is already displayed
		if (_ctTourId == ctTourId && _comparedTourItem instanceof TVICompareResultComparedTour) {
			return;
		}

		// load the tourdata of the compared tour from the database
		final TourData compTourData = TourManager.getInstance().getTourData(ctTourId);
		if (compTourData == null) {
			return;
		}

		// keep data from the selected compared tour
		_ctTourId = ctTourId;
		_ctRefId = compareResultItem.refTour.getRefId();
		_ctCompareId = compareResultItem.compId;

		_tourData = compTourData;

		// set tour compare data, this will show the action button to see the graph for this data
		_tourData.tourCompareSerie = compareResultItem.altitudeDiffSerie;

		if (_ctCompareId == -1) {

			// compared tour is not saved

			_defaultStartIndex = _computedStartIndex = _movedStartIndex = compareResultItem.computedStartIndex;
			_defaultEndIndex = _computedEndIndex = _movedEndIndex = compareResultItem.computedEndIndex;

		} else {

			// compared tour is saved

			_defaultStartIndex = _movedStartIndex = compareResultItem.dbStartIndex;
			_defaultEndIndex = _movedEndIndex = compareResultItem.dbEndIndex;

			_computedStartIndex = compareResultItem.computedStartIndex;
			_computedEndIndex = compareResultItem.computedEndIndex;
		}

		_comparedTourItem = compareResultItem;

		updateTourChart();

		// enable action after the chart was created
		_tourChart.enableGraphAction(TourManager.GRAPH_TOUR_COMPARE, true);
	}

}
