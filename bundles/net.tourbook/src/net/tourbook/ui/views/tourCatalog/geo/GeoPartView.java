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
package net.tourbook.ui.views.tourCatalog.geo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.data.NormalizedGeoData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.views.tourCatalog.ReferenceTourManager;
import net.tourbook.ui.views.tourCatalog.SelectionTourCatalogView;
import net.tourbook.ui.views.tourCatalog.TVICatalogComparedTour;
import net.tourbook.ui.views.tourCatalog.TVICompareResultComparedTour;
import net.tourbook.ui.views.tourCatalog.TourCompareConfig;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.ProgressMonitorPart;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

public class GeoPartView extends ViewPart {

// SET_FORMATTING_OFF
	
	public static final String		ID									= "net.tourbook.ui.views.tourCatalog.geo.GeoPartView";	//$NON-NLS-1$

	private static final String		STATE_IS_LISTEN_TO_SLIDER_POSITION	= "STATE_IS_LISTEN_TO_SLIDER_POSITION";		//$NON-NLS-1$
	private static final String		STATE_IS_USE_APP_FILTER				= "STATE_IS_USE_APP_FILTER";				//$NON-NLS-1$
	
// SET_FORMATTING_ON

	private static final IDialogSettings	_state								= TourbookPlugin.getState(ID);

	private final IPreferenceStore			_prefStore							= TourbookPlugin.getPrefStore();

	private IPartListener2					_partListener;

	private ITourEventListener				_tourEventListener;
	private SelectionAdapter				_defaultSelectionListener;
	private IPropertyChangeListener			_prefChangeListener;
	private ISelectionListener				_postSelectionListener;
	private int								_lastSelectionHash;

	private long							_lastTourId							= Long.MIN_VALUE;
	private int								_lastLeftIndex;
	private int								_lastRightIndex;
	private int[]							_geoParts;

	private int								_geoPartTours;
	private NormalizedGeoData				_normalizedTourPart;
	private AtomicInteger					_workedTours						= new AtomicInteger();
	private AtomicInteger					_runningId							= new AtomicInteger();

	private long							_workerExecutorId;

	private boolean							_isListenToSliderPosition;
	private boolean							_isUseAppFilter;

	private GeoPartItem						_previousGeoPartItem;
	private int								_workedDiff;
	private long							_lastUIUpdate;

	private ActionAppTourFilter				_actionAppTourFilter;
	private ActionOnOff						_actionOnOff;

	/*
	 * UI controls
	 */
	private Composite						_parent;

	private Label							_lblNumGeoParts;

	private Label							_lblNumSlices;
	private Label							_lblNumTours;
	private Label							_lblSqlRuntime;

	private ProgressMonitorPart				_progMonitor;

	private class ActionAppTourFilter extends Action {

		public ActionAppTourFilter() {

			super(null, AS_CHECK_BOX);

			setToolTipText(Messages.GeoPart_View_Action_AppFilter_Tooltip);
			setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Filter));
			setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Filter_Disabled));
		}

		@Override
		public void run() {
			onAction_AppFilter(isChecked());
		}
	}

	private class ActionOnOff extends Action {

		public ActionOnOff() {

			super(null, AS_CHECK_BOX);

			setToolTipText(Messages.GeoPart_View_Action_OnOff_Tooltip);
			setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Turn_On));
		}

		@Override
		public void run() {
			onAction_OnOff(isChecked());
		}

		private void setIcon(final boolean isSelected) {

			// switch icon
			if (isSelected) {
				setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Turn_On));
			} else {
				setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Turn_Off));
			}
		}

	}

	private void addPartListener() {

		_partListener = new IPartListener2() {

			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == GeoPartView.this) {
					saveState();
					onCancelProgress();
				}
			}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == GeoPartView.this) {}
			}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {}

			@Override
			public void partVisible(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == GeoPartView.this) {}
			}
		};
		getViewSite().getPage().addPartListener(_partListener);
	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {

					recompareTours();
				}
			}
		};

		// register the listener
		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		_postSelectionListener = new ISelectionListener() {
			@Override
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				if (part == GeoPartView.this) {
					return;
				}

				onSelectionChanged(selection);
			}
		};
		getSite().getPage().addPostSelectionListener(_postSelectionListener);
	}

	private void addTourEventListener() {

		_tourEventListener = new ITourEventListener() {
			@Override
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (part == GeoPartView.this) {
					return;
				}

				if ((eventId == TourEventId.TOUR_SELECTION) && eventData instanceof ISelection) {

					onSelectionChanged((ISelection) eventData);

				} else if (eventId == TourEventId.SLIDER_POSITION_CHANGED && eventData instanceof ISelection) {

					onSelectionChanged((ISelection) eventData);
				}
			}
		};

		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	private void compare_10_LoadGeoParts(final TourData tourData, final int firstIndex, final int lastIndex) {

		// 1. get geo partitions from lat/lon first/last index
		_geoParts = tourData.computeGeo_Partitions(firstIndex, lastIndex);

		if (_geoParts == null) {
			return;
		}

		// update UI
		_lblNumSlices.setText(Integer.toString(lastIndex - firstIndex));
		_lblNumGeoParts.setText(Integer.toString(_geoParts.length));

		/*
		 * Create geo data which should be compared
		 */
		_normalizedTourPart = tourData.computeGeo_NormalizedLatLon(firstIndex, lastIndex);

		// 2. load tour id's in the geo parts
		_previousGeoPartItem = GeoPartTourLoader.loadToursFromGeoParts(
				_geoParts,
				_normalizedTourPart,
				_isUseAppFilter,
				_previousGeoPartItem);
	}

	void compare_20_CompareTours(final GeoPartItem loaderItem) {

		_geoPartTours = loaderItem.tourIds.length;

		final long workerExecutorId[] = { 0 };

		if (_geoPartTours > 0) {

			_workedTours.set(0);

			_workerExecutorId = loaderItem.executorId;
			workerExecutorId[0] = _workerExecutorId;

			GeoPartTourComparer.compareGeoTours(loaderItem);
		}

		// update UI
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {

				if (_parent.isDisposed()) {
					return;
				}

				if (workerExecutorId[0] != _workerExecutorId) {
					// skip old tasks
					return;
				}

				if (_geoPartTours > 0) {

					// enable cancel button
					_progMonitor.attachToCancelComponent(null);

					_progMonitor.beginTask(NLS.bind("Comparing tours", _geoPartTours), _geoPartTours); //$NON-NLS-1$
				}

				updateUI(loaderItem);
			}
		});

	}

	void compare_30_TourIsCompared(final GeoPartComparerItem comparerItem) {

		final GeoPartItem geoPartItem = comparerItem.geoPartItem;

		if (geoPartItem.isCanceled || geoPartItem.executorId != _workerExecutorId) {
			return;
		}

		final int workedTours = _workedTours.incrementAndGet();

		_workedDiff++;
		final int workedDiff = _workedDiff;

		final long now = System.currentTimeMillis();

		// update UI every 100 ms or when comparation is done
		if (now - _lastUIUpdate < 100 && workedTours != _geoPartTours) {
			return;
		}

		_lastUIUpdate = now;
		_workedDiff = 1;

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {

				if (_parent.isDisposed()) {
					return;
				}

				_progMonitor.subTask(NLS.bind("{0} / {1}", workedTours, _geoPartTours)); //$NON-NLS-1$
				_progMonitor.worked(workedDiff);

				// fire geo part compare result
				TourManager.fireEventWithCustomData(
						TourEventId.GEO_PART_COMPARE,
						comparerItem.geoPartItem,
						GeoPartView.this);

				if (workedTours == _geoPartTours) {
					logCompareResult(geoPartItem);
				}
			}
		});
	}

	private void createActions() {

		_actionAppTourFilter = new ActionAppTourFilter();
		_actionOnOff = new ActionOnOff();
	}

	@Override
	public void createPartControl(final Composite parent) {

		_parent = parent;

		initUI();

		createUI(parent);
		createActions();

		fillToolbar();

		addPartListener();
		addPrefListener();
		addTourEventListener();
		addSelectionListener();

		GeoPartTourLoader.geoPartView = this;
		GeoPartTourComparer.geoPartView = this;

		restoreState();
	}

	private void createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory
				.swtDefaults()//
				.numColumns(2)
				//				.spacing(10, 2)
				.applyTo(container);
		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		{
			{
				/*
				 * Number of time slices
				 */

				final Label label = new Label(container, SWT.NONE);
				label.setText("Time Slices"); //$NON-NLS-1$
				GridDataFactory.fillDefaults().applyTo(label);

				_lblNumSlices = new Label(container, SWT.NONE);
				_lblNumSlices.setText(UI.EMPTY_STRING);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblNumSlices);
			}
			{
				/*
				 * Number of geo parts
				 */

				final Label label = new Label(container, SWT.NONE);
				label.setText("Geo Parts"); //$NON-NLS-1$
				GridDataFactory.fillDefaults().applyTo(label);

				_lblNumGeoParts = new Label(container, SWT.NONE);
				_lblNumGeoParts.setText(UI.EMPTY_STRING);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblNumGeoParts);
			}
			{
				/*
				 * Number of tours
				 */

				final Label label = new Label(container, SWT.NONE);
				label.setText("Part Tours"); //$NON-NLS-1$
				GridDataFactory.fillDefaults().applyTo(label);

				_lblNumTours = new Label(container, SWT.NONE);
				_lblNumTours.setText(UI.EMPTY_STRING);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblNumTours);
			}
			{
				/*
				 * SQL runtime
				 */

				final Label label = new Label(container, SWT.NONE);
				label.setText("SQL Runtime"); //$NON-NLS-1$
				GridDataFactory.fillDefaults().applyTo(label);

				_lblSqlRuntime = new Label(container, SWT.NONE);
				_lblSqlRuntime.setText(UI.EMPTY_STRING);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblSqlRuntime);
			}
			{
				/*
				 * Progress monitor with cancel button
				 */
				_progMonitor = new ProgressMonitorPart(container, new GridLayout(), true) {

					@Override
					public void setCanceled(final boolean isCanceled) {

						onCancelProgress();

						super.setCanceled(isCanceled);
					}
				};

				GridDataFactory.fillDefaults().span(2, 1).applyTo(_progMonitor);
			}
		}
	}

	@Override
	public void dispose() {

		getSite().getPage().removePostSelectionListener(_postSelectionListener);
		getSite().getPage().removePartListener(_partListener);

		TourManager.getInstance().removeTourEventListener(_tourEventListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	private void enableControls() {

		_actionAppTourFilter.setEnabled(_isListenToSliderPosition);
	}

	private void fillToolbar() {

		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

		tbm.add(_actionOnOff);
		tbm.add(_actionAppTourFilter);

		tbm.update(true);
	}

	private void initUI() {

		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				recompareTours();
			}
		};
	}

	private void logCompareResult(final GeoPartItem geoPartItem) {

		final ArrayList<GeoPartComparerItem> comparedTours = geoPartItem.comparedTours;

		// sort by compare min diff value
		Collections.sort(comparedTours, new Comparator<GeoPartComparerItem>() {

			@Override
			public int compare(final GeoPartComparerItem compItem1, final GeoPartComparerItem compItem2) {

				if (compItem1 == null || compItem1.tourLatLonDiff == null) {
					return Integer.MAX_VALUE;
				}
				if (compItem2 == null || compItem2.tourLatLonDiff == null) {
					return Integer.MAX_VALUE;
				}

				final int minIndex1 = compItem1.tourMinDiffIndex;
				final int minIndex2 = compItem2.tourMinDiffIndex;

				final long diff1 = minIndex1 < 0 ? Integer.MAX_VALUE : compItem1.tourLatLonDiff[minIndex1];
				final long diff2 = minIndex2 < 0 ? Integer.MAX_VALUE : compItem2.tourLatLonDiff[minIndex2];

//				return (int) (diff1 - diff2); // smallest first
				return (int) (diff2 - diff1); // smallest last
			}
		});

		System.out.println("updateUI_CompareResult()  execId " + geoPartItem.executorId);

		for (final GeoPartComparerItem comparerItem : comparedTours) {

			if (comparerItem == null) {

				System.out.println("comparerItem == null"); //$NON-NLS-1$
				continue;
			}

			if (comparerItem.tourLatLonDiff == null) {

				System.out.println("tourLatLonDiff == null"); //$NON-NLS-1$
				continue;
			}

			final int tourMinDiffIndex = comparerItem.tourMinDiffIndex;
			final long[] tourLatLonDiff = comparerItem.tourLatLonDiff;

			System.out.println(
					String.format(

							"tourId %-20s" //$NON-NLS-1$
									+ "   idx %5d" //$NON-NLS-1$
									+ "   diff %12d" //$NON-NLS-1$

									+ "   speed %5.1f" //$NON-NLS-1$
									+ "   pulse %6.1f" //$NON-NLS-1$

							,

							comparerItem.tourId,

							tourMinDiffIndex,
							tourMinDiffIndex < 0 ? -1 : tourLatLonDiff[tourMinDiffIndex],

							comparerItem.speed,
							comparerItem.avgPulse

					));
// TODO remove SYSTEM.OUT.PRINTLN
		}
	}

	private void onAction_AppFilter(final boolean isSelected) {

		_isUseAppFilter = isSelected;

		recompareTours();
	}

	private void onAction_OnOff(final boolean isSelected) {

		_isListenToSliderPosition = isSelected;

		_isListenToSliderPosition = isSelected;
		_actionOnOff.setIcon(_isListenToSliderPosition);

		if (isSelected) {

			// enable comparing

			recompareTours();

		} else {

			// cancel comparing

			onCancelProgress();
		}

		enableControls();
	}

	private void onCancelProgress() {

		_progMonitor.beginTask("Comparing is canceled", 1);
//		_progMonitor.setTaskName("Comparing is canceled");
//		_progMonitor.subTask(UI.EMPTY_STRING);
		_progMonitor.worked(0);

//		_progMonitor.setEnabled(false);

		GeoPartTourLoader.stopLoading(_previousGeoPartItem);
	}

	private void onMoveSlider(	final TourData tourData,
								final int leftIndex,
								final int rightIndex) {

		if (_isListenToSliderPosition == false) {

			// ignore slider position
			return;
		}

		final double[] latSerie = tourData.latitudeSerie;
		if (latSerie == null) {
			return;
		}

		/*
		 * Validate first/last indices
		 */
		int firstIndex = leftIndex < rightIndex ? leftIndex : rightIndex;
		int lastIndex = leftIndex > rightIndex ? leftIndex : rightIndex;
		if (firstIndex < 0) {
			firstIndex = 0;
		}
		if (lastIndex > latSerie.length) {
			lastIndex = latSerie.length;
		}

		// skip same data
		if (tourData.getTourId() == _lastTourId //
				&& leftIndex == _lastLeftIndex
				&& rightIndex == _lastRightIndex) {
			return;
		}

		_lastTourId = tourData.getTourId();
		_lastLeftIndex = leftIndex;
		_lastRightIndex = rightIndex;

		final int firstIndexFinal = firstIndex;
		final int lastIndexFinal = lastIndex;

		GeoPartTourLoader.stopLoading(_previousGeoPartItem);

		// delay tour comparator, moving the slider can occure very often
		_parent.getDisplay().timerExec(100, new Runnable() {

			private int __runningId = _runningId.incrementAndGet();

			@Override
			public void run() {

				if (_parent.isDisposed()) {
					return;
				}

				if (__runningId != _runningId.get()) {

					// a newer runnable is created

					return;
				}

				compare_10_LoadGeoParts(tourData, firstIndexFinal, lastIndexFinal);
			}
		});
	}

	private void onSelectionChanged(final ISelection selection) {

		final int selectionHash = selection.hashCode();
		if (_lastSelectionHash == selectionHash) {

			/*
			 * Last selection has not changed, this can occure when the app lost the focus and got
			 * the focus again.
			 */
			return;
		}

		_lastSelectionHash = selectionHash;

		if (selection instanceof SelectionChartInfo) {

			TourData tourData = null;

			final SelectionChartInfo chartInfo = (SelectionChartInfo) selection;

			final Chart chart = chartInfo.getChart();
			if (chart instanceof TourChart) {

				final TourChart tourChart = (TourChart) chart;
				tourData = tourChart.getTourData();
			}

			if (tourData != null && tourData.isMultipleTours()) {

				// multiple tours are selected

			} else {

				// use old behaviour

				final ChartDataModel chartDataModel = chartInfo.chartDataModel;
				if (chartDataModel != null) {

					final Object tourId = chartDataModel.getCustomData(Chart.CUSTOM_DATA_TOUR_ID);
					if (tourId instanceof Long) {

						tourData = TourManager.getInstance().getTourData((Long) tourId);
						if (tourData == null) {

							// tour is not in the database, try to get it from the raw data manager

							final HashMap<Long, TourData> rawData = RawDataManager.getInstance().getImportedTours();
							tourData = rawData.get(tourId);
						}
					}
				}
			}

			if (tourData != null) {

				onMoveSlider(
						tourData,
						chartInfo.leftSliderValuesIndex,
						chartInfo.rightSliderValuesIndex);
			}

		} else if (selection instanceof SelectionChartXSliderPosition) {

			final SelectionChartXSliderPosition xSliderPos = (SelectionChartXSliderPosition) selection;
			final Chart chart = xSliderPos.getChart();
			if (chart == null) {
				return;
			}

			final ChartDataModel chartDataModel = chart.getChartDataModel();

			final Object tourId = chartDataModel.getCustomData(Chart.CUSTOM_DATA_TOUR_ID);
			if (tourId instanceof Long) {

				final TourData tourData = TourManager.getInstance().getTourData((Long) tourId);
				if (tourData != null) {

					final int leftSliderValueIndex = xSliderPos.getLeftSliderValueIndex();
					int rightSliderValueIndex = xSliderPos.getRightSliderValueIndex();

					rightSliderValueIndex =
							rightSliderValueIndex == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION
									? leftSliderValueIndex
									: rightSliderValueIndex;

					onMoveSlider(
							tourData,
							leftSliderValueIndex,
							rightSliderValueIndex);
				}
			}

		} else if (selection instanceof SelectionTourCatalogView) {

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

	private void recompareTours() {

		if (_geoParts != null && _isListenToSliderPosition) {

			_previousGeoPartItem = GeoPartTourLoader.loadToursFromGeoParts(
					_geoParts,
					_normalizedTourPart,
					_isUseAppFilter,
					_previousGeoPartItem);
		}

	}

	private void restoreState() {

		_isListenToSliderPosition = Util.getStateBoolean(_state, STATE_IS_LISTEN_TO_SLIDER_POSITION, true);
		_actionOnOff.setIcon(_isListenToSliderPosition);
		_actionOnOff.setChecked(_isListenToSliderPosition);

		_isUseAppFilter = Util.getStateBoolean(_state, STATE_IS_USE_APP_FILTER, true);
		_actionAppTourFilter.setChecked(_isUseAppFilter);

		enableControls();
	}

	private void saveState() {

		_state.put(STATE_IS_LISTEN_TO_SLIDER_POSITION, _isListenToSliderPosition);
		_state.put(STATE_IS_USE_APP_FILTER, _isUseAppFilter);
	}

	@Override
	public void setFocus() {}

	private void showRefTour(final long refId) {
		// TODO Auto-generated method stub

		final TourCompareConfig tourCompareConfig = ReferenceTourManager.getInstance().getTourCompareConfig(refId);

		if (tourCompareConfig == null) {
			return;
		}

		final TourData tourData = tourCompareConfig.getRefTourData();
		if (tourData != null) {

			final TourReference refTour = tourCompareConfig.getRefTour();

			final int leftSliderValueIndex = refTour.getStartValueIndex();
			final int rightSliderValueIndex = refTour.getEndValueIndex();

			onMoveSlider(
					tourData,
					leftSliderValueIndex,
					rightSliderValueIndex);
		}

	}

	private void updateUI(final GeoPartItem loaderItem) {

		_lblNumTours.setText(Integer.toString(loaderItem.tourIds.length));
		_lblSqlRuntime.setText(Long.toString(loaderItem.sqlRunningTime) + " ms"); //$NON-NLS-1$
	}

}
