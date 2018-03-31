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

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.MtMath;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.TableColumnDefinition;
import net.tourbook.common.util.Util;
import net.tourbook.data.NormalizedGeoData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.TableColumnFactory;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.views.tourCatalog.ReferenceTourManager;
import net.tourbook.ui.views.tourCatalog.SelectionTourCatalogView;
import net.tourbook.ui.views.tourCatalog.TVICatalogComparedTour;
import net.tourbook.ui.views.tourCatalog.TVICompareResultComparedTour;
import net.tourbook.ui.views.tourCatalog.TourCatalogViewComparedTour;
import net.tourbook.ui.views.tourCatalog.TourCatalogViewReferenceTour;
import net.tourbook.ui.views.tourCatalog.TourCompareConfig;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

public class GeoPartView extends ViewPart implements ITourViewer {

// SET_FORMATTING_OFF
	

	public static final String				ID			= "net.tourbook.ui.views.tourCatalog.geo.GeoPartView";	//$NON-NLS-1$
                                            
// SET_FORMATTING_ON

	private static final int				DELAY_BEFORE_STARTING_COMPARE	= 300;
	private static final int				UI_UPDATE_INTERVAL				= 500;

	private static final String				STATE_IS_COMPARE_ENABLED		= "STATE_IS_COMPARE_ENABLED";		//$NON-NLS-1$
	private static final String				STATE_IS_USE_APP_FILTER			= "STATE_IS_USE_APP_FILTER";		//$NON-NLS-1$

	private static final String				STATE_DISTANCE_INTERVAL			= "STATE_DISTANCE_INTERVAL";		//$NON-NLS-1$
	private static final String				STATE_GEO_ACCURACY				= "STATE_GEO_ACCURACY";				//$NON-NLS-1$
	private static final String				STATE_SORT_COLUMN_DIRECTION		= "STATE_SORT_COLUMN_DIRECTION";	//$NON-NLS-1$
	private static final String				STATE_SORT_COLUMN_ID			= "STATE_SORT_COLUMN_ID";			//$NON-NLS-1$

	private static final String				COLUMN_AVG_PULSE				= "avgPulse";						//$NON-NLS-1$
	private static final String				COLUMN_AVG_SPEED				= "avgSpeed";						//$NON-NLS-1$
	private static final String				COLUMN_GEO_DIFF					= "geoDiff";						//$NON-NLS-1$
	private static final String				COLUMN_TOUR_START_DATE			= "tourStartDate";					//$NON-NLS-1$

	private static final IDialogSettings	_state							= TourbookPlugin.getState(ID);

	private final IPreferenceStore			_prefStore						= TourbookPlugin.getPrefStore();

	private IPartListener2					_partListener;
	private SelectionAdapter				_columnSortListener;
	private MouseWheelListener				_defaultMouseWheelListener;
	private SelectionAdapter				_defaultSelectionListener;
	private IPropertyChangeListener			_prefChangeListener;
	private ISelectionListener				_postSelectionListener;
	private ITourEventListener				_tourEventListener;

	private PostSelectionProvider			_postSelectionProvider;
	private int								_lastSelectionHash;

	private AtomicInteger					_workedTours					= new AtomicInteger();
	private AtomicInteger					_runningId						= new AtomicInteger();

	private long							_workerExecutorId;

	private boolean							_isCompareEnabled;
	private boolean							_isInUpdate;
	private long							_lastUIUpdate;

	/**
	 * Comparer items from the last comparison
	 */
	private ArrayList<GeoPartComparerItem>	_comparedTours					= new ArrayList<>();

	private int								_compareData_NumGeoPartTours;
	private TourData						_compareData_TourData;
	private long							_compareData_TourId				= Long.MIN_VALUE;
	private int								_compareData_FirstIndex;
	private int								_compareData_LastIndex;
	private int								_compareData_LastDistanceInterval;
	private int								_compareData_LastGeoAccuracy;
	private int[]							_compareData_GeoGrid;
	private GeoPartItem						_compareData_PreviousGeoPartItem;
	private long							_compareData_RefId;
	private String							_compareData_TourTitle;
	private boolean							_compareData_IsUseAppFilter;

	private ActionAppTourFilter				_actionAppTourFilter;
	private ActionOnOff						_actionOnOff;

	private TableViewer						_geoPartViewer;
	private ColumnManager					_columnManager;
	private CompareResultComparator			_geoPartComparator				= new CompareResultComparator();

	private int								_distanceInterval				= 100;
	private int								_geoAccuracy					= 100_000;

	private PixelConverter					_pc;

	/*
	 * UI controls
	 */
	private Composite						_parent;
	private Composite						_viewerContainer;

	private PageBook						_pageBook;
	private Composite						_pageNoData;
	private Composite						_pageContent;

	private Label							_lblGeoAccuracy;
	private Label							_lblNumGeoGrid;
	private Label							_lblNumSlices;
	private Label							_lblNumTours;
	private Label							_lblProgressBar;
//	private Label							_lblSqlRuntime;
	private Label							_lblTitle;

	private Spinner							_spinnerGeoAccuracy;
	private Spinner							_spinnerDistanceInterval;

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

	private class CompareResultComparator extends ViewerComparator {

		private static final int	ASCENDING		= 0;
		private static final int	DESCENDING		= 1;

		private String				__sortColumnId	= COLUMN_GEO_DIFF;
		private int					__sortDirection	= ASCENDING;

		@Override
		public int compare(final Viewer viewer, final Object e1, final Object e2) {

			final GeoPartComparerItem item1 = (GeoPartComparerItem) e1;
			final GeoPartComparerItem item2 = (GeoPartComparerItem) e2;

			boolean _isSortByTime = true;
			double rc = 0;

			// Determine which column and do the appropriate sort
			switch (__sortColumnId) {

			case COLUMN_GEO_DIFF:

				final long minDiffValue1 = item1.minDiffValue;
				final long minDiffValue2 = item2.minDiffValue;

				if (minDiffValue1 >= 0 && minDiffValue2 >= 0) {

					rc = minDiffValue1 - minDiffValue2;

				} else if (minDiffValue1 >= 0) {

					rc = -Integer.MAX_VALUE;

				} else if (minDiffValue2 >= 0) {

					rc = Integer.MAX_VALUE;

				} else {

					rc = minDiffValue1 - minDiffValue2;
				}

				break;

			case COLUMN_TOUR_START_DATE:

				// sorting by date is already set
				break;

			case COLUMN_AVG_PULSE:
				rc = item1.avgPulse - item2.avgPulse;
				break;

			case COLUMN_AVG_SPEED:
				rc = item1.avgSpeed - item2.avgSpeed;
				break;

			default:
				_isSortByTime = true;
			}

			if (rc == 0 && _isSortByTime) {
				rc = item1.tourStartTimeMS - item2.tourStartTimeMS;
			}

			// if descending order, flip the direction
			if (__sortDirection == DESCENDING) {
				rc = -rc;
			}

			/*
			 * MUST return 1 or -1 otherwise long values are not sorted correctly.
			 */
			return rc > 0 //
					? 1
					: rc < 0 //
							? -1
							: 0;
		}

		@Override
		public boolean isSorterProperty(final Object element, final String property) {

			// force resorting when a name is renamed
			return true;
		}

		public void setSortColumn(final Widget widget) {

			final ColumnDefinition columnDefinition = (ColumnDefinition) widget.getData();
			final String columnId = columnDefinition.getColumnId();

			if (columnId.equals(__sortColumnId)) {

				// Same column as last sort; toggle the direction

				__sortDirection = 1 - __sortDirection;

			} else {

				// New column; do an ascent sorting

				__sortColumnId = columnId;
				__sortDirection = ASCENDING;
			}

			updateUI_SetSortDirection(__sortColumnId, __sortDirection);
		}
	}

	private class CompareResultProvider implements IStructuredContentProvider {

		@Override
		public void dispose() {}

		@Override
		public Object[] getElements(final Object inputElement) {
			return _comparedTours.toArray();
		}

		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	private void addPartListener() {

		_partListener = new IPartListener2() {

			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {

				if (partRef.getPart(false) == GeoPartView.this) {

				}
			}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {

				if (partRef.getPart(false) == GeoPartView.this) {

					saveState();
					setState_CancelComparing();
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

				} else if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					updateUI_GeoAccuracy();
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

				if (isIgnorePart(part)) {
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

				if (isIgnorePart(part)) {
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

	/**
	 * @param tourData
	 * @param leftIndex
	 * @param rightIndex
	 * @param refId
	 *            Reference tour id or <code>-1</code> when not available
	 */
	private void compare_10_Compare(final TourData tourData,
									final int leftIndex,
									final int rightIndex,
									final long refId) {

		if (_isCompareEnabled == false) {

			// ignore slider position
			return;
		}

		/*
		 * !!! This must be very early because the comparison is running with a delay which could
		 * cause that an old is running !!!
		 */
		final int runnableRunningId = _runningId.incrementAndGet();

		/*
		 * Show no data page
		 */
		final double[] latSerie = tourData.latitudeSerie;
		if (latSerie == null) {

			setState_StopComparing();

			_pageBook.showPage(_pageNoData);

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

		final long tourId = tourData.getTourId();

		// skip same data and continue current comparison
		if (_compareData_TourId == tourId
				&& _compareData_FirstIndex == leftIndex
				&& _compareData_LastIndex == rightIndex
				&& _compareData_LastGeoAccuracy == _geoAccuracy
				&& _compareData_LastDistanceInterval == _distanceInterval) {

			return;
		}

		/*
		 * New data should be compared
		 */
		GeoPartTourLoader.stopLoading(_compareData_PreviousGeoPartItem);

		_compareData_TourId = tourId;
		_compareData_TourData = tourData;
		_compareData_RefId = refId;
		_compareData_FirstIndex = firstIndex;
		_compareData_LastIndex = lastIndex;

		updateUI_GeoItem(null);

		// delay tour comparator, moving the slider can occure very often
		_parent.getDisplay().timerExec(DELAY_BEFORE_STARTING_COMPARE, new Runnable() {

			private int __runningId = runnableRunningId;

			@Override
			public void run() {

				if (_parent.isDisposed()) {
					return;
				}

				if (__runningId != _runningId.get()) {

					// a newer runnable is created

					return;
				}

				compare_20_SetupComparing();
			}
		});
	}

	private void compare_20_SetupComparing() {

		// 1. get geo grid from lat/lon first/last index
		_compareData_GeoGrid = _compareData_TourData.computeGeo_Grid(
				_compareData_FirstIndex,
				_compareData_LastIndex);

		if (_compareData_GeoGrid == null) {

			_pageBook.showPage(_pageNoData);

			return;
		}

		_pageBook.showPage(_pageContent);

		/*
		 * Update UI
		 */
		_comparedTours.clear();
		updateUI_Viewer();

		final int numSlices = _compareData_LastIndex - _compareData_FirstIndex;
		_lblNumSlices.setText(Integer.toString(numSlices));
		_lblNumGeoGrid.setText(Integer.toString(_compareData_GeoGrid.length));

		updateUI_Progress(0, 0);

		_compareData_TourTitle = TourManager.getTourTitleDetailed(_compareData_TourData);

		compare_30_StartComparing();
	}

	private void compare_30_StartComparing() {

		/*
		 * Create geo data which should be compared
		 */
		final NormalizedGeoData rasterizedTourPart = _compareData_TourData.computeGeo_NormalizeLatLon(
				_compareData_FirstIndex,
				_compareData_LastIndex,
				_geoAccuracy,
				_distanceInterval);

		// 2. load tour id's in the geo parts
		final GeoPartItem newGeoPartItem = GeoPartTourLoader.loadToursFromGeoParts(
				_compareData_GeoGrid,
				rasterizedTourPart,
				_compareData_IsUseAppFilter,
				_compareData_PreviousGeoPartItem,
				this);

		newGeoPartItem.refId = _compareData_RefId;

		_compareData_PreviousGeoPartItem = newGeoPartItem;
	}

	void compare_40_CompareTours(final GeoPartItem geoPartItem) {

		_compareData_NumGeoPartTours = geoPartItem.tourIds.length;

		if (_compareData_NumGeoPartTours == 0) {

			// update UI
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {

					if (_parent.isDisposed()) {
						return;
					}

					// this can happen when the tour filter is active and no tours are found -> show empty result

					_comparedTours.clear();

					updateUI_Progress(0, 0);
					updateUI_Viewer();

					updateUI_GeoItem(geoPartItem);
				}
			});

			return;
		}

		final long workerExecutorId[] = { 0 };

		_workedTours.set(0);

		_workerExecutorId = geoPartItem.executorId;
		workerExecutorId[0] = _workerExecutorId;

		GeoCompareManager.compareGeoTours(geoPartItem, this);

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

				updateUI_GeoItem(geoPartItem);
			}
		});

	}

	void compare_50_TourIsCompared(final GeoPartComparerItem comparerItem) {

		final GeoPartItem geoPartItem = comparerItem.geoPartItem;

		if (geoPartItem.isCanceled || geoPartItem.executorId != _workerExecutorId) {
			return;
		}

		_comparedTours = geoPartItem.comparedTours;

		final int workedTours = _workedTours.incrementAndGet();

		final long now = System.currentTimeMillis();

		// update UI not too often until comparison is done
		if (now - _lastUIUpdate < UI_UPDATE_INTERVAL && workedTours != _compareData_NumGeoPartTours) {
			return;
		}

		// reset paused time
		_lastUIUpdate = now;

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {

				if (_parent.isDisposed()) {
					return;
				}

				updateUI_Progress(workedTours, _compareData_NumGeoPartTours);
				updateUI_Viewer();

				// set focus to the viewer
//				_geoPartViewer.getTable().setFocus();

				// fire geo part compare result
				TourManager.fireEventWithCustomData(
						TourEventId.GEO_PART_COMPARE,
						comparerItem.geoPartItem,
						GeoPartView.this);

				if (workedTours == _compareData_NumGeoPartTours) {

					// keep state after compare is done

					_compareData_LastGeoAccuracy = _geoAccuracy;
					_compareData_LastDistanceInterval = _distanceInterval;
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

		initUI(parent);
		restoreState_BeforeUI();

		// define all columns for the viewer
		_columnManager = new ColumnManager(this, _state);
		defineAllColumns();

		createUI(parent);
		createActions();

		fillToolbar();

		addPartListener();
		addPrefListener();
		addTourEventListener();
		addSelectionListener();

		// set selection provider
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

		restoreState();

		updateUI_GeoAccuracy();

		_pageBook.showPage(_pageNoData);
	}

	private void createUI(final Composite parent) {

		_pageBook = new PageBook(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_pageBook);

		_pageNoData = UI.createUI_PageNoData(_pageBook, Messages.GeoPart_View_Label_NoTourWithGeoData);

		_pageContent = new Composite(_pageBook, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(_pageContent);
		{
			final Composite container = new Composite(_pageContent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
			{
				createUI_10_Comparator(container);

				_viewerContainer = new Composite(container, SWT.NONE);
				GridDataFactory.fillDefaults().grab(true, true).applyTo(_viewerContainer);
				GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
				{
					createUI_20_TableViewer(_viewerContainer);
				}
			}
		}
	}

	private void createUI_10_Comparator(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory
				.swtDefaults()//
				.numColumns(2)
				//				.spacing(10, 2)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		{
			{
				// tour title

				_lblTitle = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_lblTitle);
			}

			createUI_12_Left(container);
			createUI_14_Right(container);

//			{
//				/*
//				 * SQL runtime
//				 */
//
//				final Label label = new Label(container, SWT.NONE);
//				label.setText("SQL Runtime"); //$NON-NLS-1$
//				GridDataFactory.fillDefaults().applyTo(label);
//
//				_lblSqlRuntime = new Label(container, SWT.NONE);
//				_lblSqlRuntime.setText(UI.EMPTY_STRING);
//				GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblSqlRuntime);
//			}
			{
				/*
				 * Progress bar label
				 */
				_lblProgressBar = new Label(container, SWT.NONE);

				GridDataFactory
						.fillDefaults()
						.span(2, 1)
						.grab(true, false)
						.applyTo(_lblProgressBar);
			}
		}
	}

	private void createUI_12_Left(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			{
				/*
				 * Number of tours
				 */

				final Label label = new Label(container, SWT.NONE);
				label.setText("Tours"); //$NON-NLS-1$

				_lblNumTours = new Label(container, SWT.NONE);
				_lblNumTours.setText(UI.EMPTY_STRING);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblNumTours);
			}
			{
				/*
				 * Number of time slices
				 */

				final Label label = new Label(container, SWT.NONE);
				label.setText("Time Slices"); //$NON-NLS-1$

				_lblNumSlices = new Label(container, SWT.NONE);
				_lblNumSlices.setText(UI.EMPTY_STRING);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblNumSlices);
			}
			{
				/*
				 * Number of geo parts
				 */

				final Label label = new Label(container, SWT.NONE);
				label.setText("Geo Grid"); //$NON-NLS-1$

				_lblNumGeoGrid = new Label(container, SWT.NONE);
				_lblNumGeoGrid.setText(UI.EMPTY_STRING);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblNumGeoGrid);
			}
		}
	}

	private void createUI_14_Right(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		{
			{
				/*
				 * Normalized geo data factor
				 */
				{
					// Label
					final Label label = new Label(container, SWT.NONE);
					label.setText("Geo &Accuracy");
				}
				{
					// Spinner
					_spinnerGeoAccuracy = new Spinner(container, SWT.BORDER);
					_spinnerGeoAccuracy.setMinimum(100);
					_spinnerGeoAccuracy.setMaximum(100_000);
					_spinnerGeoAccuracy.setPageIncrement(100);
					_spinnerGeoAccuracy.addSelectionListener(_defaultSelectionListener);
					_spinnerGeoAccuracy.addMouseWheelListener(_defaultMouseWheelListener);
				}
				{
					// geo distance
					_lblGeoAccuracy = new Label(container, SWT.NONE);
					GridDataFactory
							.fillDefaults()
							.grab(true, false)
							.align(SWT.FILL, SWT.CENTER)
							.applyTo(_lblGeoAccuracy);
				}
			}
		}
		{
			/*
			 * Distance interval
			 */
			{
				// Label
				final Label label = new Label(container, SWT.NONE);
				label.setText("&Distance Interval");
			}
			{
				// Spinner
				_spinnerDistanceInterval = new Spinner(container, SWT.BORDER);
				_spinnerDistanceInterval.setMinimum(10);
				_spinnerDistanceInterval.setMaximum(1_000);
				_spinnerDistanceInterval.setPageIncrement(10);
				_spinnerDistanceInterval.addSelectionListener(_defaultSelectionListener);
				_spinnerDistanceInterval.addMouseWheelListener(_defaultMouseWheelListener);
				GridDataFactory
						.fillDefaults()
						.align(SWT.END, SWT.FILL)
						.applyTo(_spinnerDistanceInterval);
			}
			{
				// Label: Distance unit
				final Label labelDistanceUnit = new Label(container, SWT.NONE);
				labelDistanceUnit.setText("m");
				GridDataFactory
						.fillDefaults()
						.grab(true, false)
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(labelDistanceUnit);
			}
		}
	}

	private void createUI_20_TableViewer(final Composite parent) {

		/*
		 * create table
		 */
		final Table table = new Table(parent, SWT.FULL_SELECTION /* | SWT.MULTI /* | SWT.BORDER */);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

		table.setHeaderVisible(true);
		table.setLinesVisible(false);

		/*
		 * It took a while that the correct listener is set and also the checked item is fired and
		 * not the wrong selection.
		 */
		table.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(final Event event) {
				onGeoPart_Select(event);
			}
		});
		/*
		 * create table viewer
		 */
		_geoPartViewer = new TableViewer(table);

		_columnManager.createColumns(_geoPartViewer);

		_geoPartViewer.setUseHashlookup(true);
		_geoPartViewer.setContentProvider(new CompareResultProvider());
		_geoPartViewer.setComparator(_geoPartComparator);

		_geoPartViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				fireSelection(event.getSelection());
			}
		});

		_geoPartViewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(final DoubleClickEvent event) {
//				onBookmark_Rename(true);
			}
		});

		_geoPartViewer.getTable().addKeyListener(new KeyListener() {

			@Override
			public void keyPressed(final KeyEvent e) {

				switch (e.keyCode) {

				case SWT.DEL:
//					onBookmark_Delete();
					break;

				case SWT.F2:
//					onBookmark_Rename(false);
					break;

				default:
					break;
				}
			}

			@Override
			public void keyReleased(final KeyEvent e) {}
		});

		updateUI_SetSortDirection(//
				_geoPartComparator.__sortColumnId,
				_geoPartComparator.__sortDirection);

		createUI_22_ContextMenu();
	}

	/**
	 * Ceate the view context menus
	 */
	private void createUI_22_ContextMenu() {

		final MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$

		menuMgr.setRemoveAllWhenShown(true);

		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(final IMenuManager manager) {
//				fillContextMenu(manager);
			}
		});

		final Table table = _geoPartViewer.getTable();
		final Menu tableHeaderContextMenu = menuMgr.createContextMenu(table);

		_columnManager.createHeaderContextMenu(table, tableHeaderContextMenu);
	}

	private void defineAllColumns() {

		defineColumn_10_GeoDiff();
		defineColumn_20_Time_TourStartDate();
		defineColumn_30_Motion_AvgSpeed();
		defineColumn_40_Body_AvgPulse();

		defineColumn_80_StartIndex();
		defineColumn_82_EndIndex();
		defineColumn_84_IndexDiff();
	}

	/**
	 * Column: Geo Diff
	 */
	private void defineColumn_10_GeoDiff() {

		final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_GEO_DIFF, SWT.TRAIL);

		colDef.setColumnLabel(Messages.GeoPart_View_Column_GeoDiff_Label);
		colDef.setColumnHeaderText(Messages.GeoPart_View_Column_GeoDiff_Header);
		colDef.setColumnHeaderToolTipText(Messages.GeoPart_View_Column_GeoDiff_Label);

		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(30));

		colDef.setIsDefaultColumn();
		colDef.setCanModifyVisibility(false);
		colDef.setColumnSelectionListener(_columnSortListener);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final GeoPartComparerItem item = (GeoPartComparerItem) cell.getElement();
				final long minDiffValue = item.minDiffValue;

				final String minDiffText = minDiffValue == -2
						? "..."
						: minDiffValue == -1
								? "n/a"
								: Long.toString(minDiffValue);

				cell.setText(minDiffText);
			}
		});
	}

	/**
	 * column: Tour start date
	 */
	private void defineColumn_20_Time_TourStartDate() {

		final ColumnDefinition colDef = TableColumnFactory.TIME_TOUR_START_DATE.createColumn(_columnManager, _pc);

		// overwrite column id to identify the column when table is sorted
		colDef.setColumnId(COLUMN_TOUR_START_DATE);
		colDef.setColumnSelectionListener(_columnSortListener);

		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final GeoPartComparerItem item = (GeoPartComparerItem) cell.getElement();

				final ZonedDateTime tourStartTime = item.tourStartTime;
				cell.setText(
						tourStartTime == null
								? UI.EMPTY_STRING
								: tourStartTime.format(TimeTools.Formatter_Date_S));
			}
		});
	}

	/**
	 * column: Tour start date
	 */
	private void defineColumn_30_Motion_AvgSpeed() {

		final ColumnDefinition colDef = TableColumnFactory.MOTION_AVG_SPEED.createColumn(_columnManager, _pc);

		// overwrite column id to identify the column when table is sorted
		colDef.setColumnId(COLUMN_AVG_SPEED);
		colDef.setColumnSelectionListener(_columnSortListener);

		colDef.setIsDefaultColumn();

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final GeoPartComparerItem item = (GeoPartComparerItem) cell.getElement();

				final double speed = item.avgSpeed / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

				colDef.printDetailValue(cell, speed);
			}
		});
	}

	/**
	 * column: average pulse
	 */
	private void defineColumn_40_Body_AvgPulse() {

		final ColumnDefinition colDef = TableColumnFactory.BODY_AVG_PULSE.createColumn(_columnManager, _pc);

		// overwrite column id to identify the column when table is sorted
		colDef.setColumnId(COLUMN_AVG_PULSE);
		colDef.setColumnSelectionListener(_columnSortListener);

		colDef.setIsDefaultColumn();

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final GeoPartComparerItem item = (GeoPartComparerItem) cell.getElement();

				colDef.printDetailValue(cell, item.avgPulse);
			}
		});
	}

	/**
	 * Column: Start index
	 */
	private void defineColumn_80_StartIndex() {

		final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "startIndex", SWT.TRAIL);

		colDef.setColumnLabel("Start Idx");
		colDef.setColumnHeaderText("Start Idx");

		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(10));

		colDef.setIsDefaultColumn();

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final GeoPartComparerItem item = (GeoPartComparerItem) cell.getElement();

				cell.setText(Integer.toString(item.tourFirstIndex));
			}
		});
	}

	/**
	 * Column: End index
	 */
	private void defineColumn_82_EndIndex() {

		final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "endIndex", SWT.TRAIL);

		colDef.setColumnLabel("End Idx");
		colDef.setColumnHeaderText("End Idx");

		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(10));

		colDef.setIsDefaultColumn();

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final GeoPartComparerItem item = (GeoPartComparerItem) cell.getElement();

				cell.setText(Integer.toString(item.tourLastIndex));
			}
		});
	}

	/**
	 * Column: End index
	 */
	private void defineColumn_84_IndexDiff() {

		final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "indexDiff", SWT.TRAIL);

		colDef.setColumnLabel("Idx ∆");
		colDef.setColumnHeaderText("Idx ∆");

		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(10));

		colDef.setIsDefaultColumn();

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final GeoPartComparerItem item = (GeoPartComparerItem) cell.getElement();

				cell.setText(Integer.toString(item.tourLastIndex - item.tourFirstIndex));
			}
		});
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

		_actionAppTourFilter.setEnabled(_isCompareEnabled);
	}

	private void fillToolbar() {

		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

		tbm.add(_actionOnOff);
		tbm.add(_actionAppTourFilter);

		tbm.update(true);
	}

	private void fireSelection(final ISelection selection) {

		// fire selection for the selected geo part tour
		_postSelectionProvider.setSelection(selection);
	}

	@Override
	public ColumnManager getColumnManager() {
		return _columnManager;
	}

	/**
	 * @param sortColumnId
	 * @return Returns the column widget by it's column id, when column id is not found then the
	 *         first column is returned.
	 */
	private TableColumn getSortColumn(final String sortColumnId) {

		final TableColumn[] allColumns = _geoPartViewer.getTable().getColumns();

		for (final TableColumn column : allColumns) {

			final String columnId = ((ColumnDefinition) column.getData()).getColumnId();

			if (columnId.equals(sortColumnId)) {
				return column;
			}
		}

		return allColumns[0];
	}

	@Override
	public ColumnViewer getViewer() {
		return _geoPartViewer;
	}

	private StructuredSelection getViewerSelection() {

		return (StructuredSelection) _geoPartViewer.getSelection();
	}

	private void initUI(final Composite parent) {

		_parent = parent;

		_pc = new PixelConverter(parent);

		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeUI();
			}
		};

		_defaultMouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
				onChangeUI();
			}
		};

		_columnSortListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelect_SortColumn(e);
			}
		};
	}

	private boolean isIgnorePart(final IWorkbenchPart part) {

		// ignore own part
		if (part == GeoPartView.this) {
			return true;
		}

		// ignore other parts to prevent geo part comparing !!!
		if (part instanceof TourCatalogViewComparedTour || part instanceof TourCatalogViewReferenceTour) {
			return true;
		}

		return false;
	}

	private void onAction_AppFilter(final boolean isSelected) {

		_compareData_IsUseAppFilter = isSelected;

		recompareTours();
	}

	private void onAction_OnOff(final boolean isSelected) {

		_isCompareEnabled = isSelected;

		_actionOnOff.setIcon(_isCompareEnabled);

		if (isSelected) {

			// enable comparing

			recompareTours();

		} else {

			// cancel comparing

			setState_CancelComparing();
		}

		enableControls();
	}

	private void onChangeUI() {

		_geoAccuracy = _spinnerGeoAccuracy.getSelection();
		_distanceInterval = _spinnerDistanceInterval.getSelection();

		if (_compareData_LastGeoAccuracy != _geoAccuracy || _compareData_LastDistanceInterval != _distanceInterval) {

			// accuracy is modified

			updateUI_GeoAccuracy();

			recompareTours();
		}
	}

	private void onGeoPart_Select(final Event event) {
		// TODO Auto-generated method stub
		if (_isInUpdate) {
			return;
		}
	}

	private void onSelect_SortColumn(final SelectionEvent e) {

		_viewerContainer.setRedraw(false);
		{
			// keep selection
			final ISelection selectionBackup = getViewerSelection();
			{
				// update viewer with new sorting
				_geoPartComparator.setSortColumn(e.widget);
				_geoPartViewer.refresh();
			}
			updateUI_SelectCompareItem(selectionBackup);
		}
		_viewerContainer.setRedraw(true);
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

				compare_10_Compare(
						tourData,
						chartInfo.leftSliderValuesIndex,
						chartInfo.rightSliderValuesIndex,
						-1);
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

					compare_10_Compare(
							tourData,
							leftSliderValueIndex,
							rightSliderValueIndex,
							-1);
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

		if (_compareData_GeoGrid != null && _isCompareEnabled) {
			compare_30_StartComparing();
		}
	}

	@Override
	public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

		_viewerContainer.setRedraw(false);
		{
			_geoPartViewer.getTable().dispose();

			createUI_20_TableViewer(_viewerContainer);
			_viewerContainer.layout();

			// update the viewer
			reloadViewer();
		}
		_viewerContainer.setRedraw(true);

		return _geoPartViewer;
	}

	@Override
	public void reloadViewer() {

		updateUI_Viewer();
	}

	private void restoreState() {

		_isCompareEnabled = Util.getStateBoolean(_state, STATE_IS_COMPARE_ENABLED, true);
		_actionOnOff.setIcon(_isCompareEnabled);
		_actionOnOff.setChecked(_isCompareEnabled);

		_compareData_IsUseAppFilter = Util.getStateBoolean(_state, STATE_IS_USE_APP_FILTER, true);
		_actionAppTourFilter.setChecked(_compareData_IsUseAppFilter);

		_geoAccuracy = Util.getStateInt(_state, STATE_GEO_ACCURACY, 100_000);
		_spinnerGeoAccuracy.setSelection(_geoAccuracy);

		_distanceInterval = Util.getStateInt(_state, STATE_DISTANCE_INTERVAL, 100);
		_spinnerDistanceInterval.setSelection(_distanceInterval);

		enableControls();
	}

	private void restoreState_BeforeUI() {

		// sorting
		final String sortColumnId = Util.getStateString(_state, STATE_SORT_COLUMN_ID, COLUMN_GEO_DIFF);
		final int sortDirection = Util.getStateInt(
				_state,
				STATE_SORT_COLUMN_DIRECTION,
				CompareResultComparator.ASCENDING);

		// update comparator
		_geoPartComparator.__sortColumnId = sortColumnId;
		_geoPartComparator.__sortDirection = sortDirection;
	}

	private void saveState() {

		_state.put(STATE_IS_COMPARE_ENABLED, _isCompareEnabled);
		_state.put(STATE_IS_USE_APP_FILTER, _compareData_IsUseAppFilter);
		_state.put(STATE_GEO_ACCURACY, _geoAccuracy);
		_state.put(STATE_DISTANCE_INTERVAL, _distanceInterval);

		_state.put(STATE_SORT_COLUMN_ID, _geoPartComparator.__sortColumnId);
		_state.put(STATE_SORT_COLUMN_DIRECTION, _geoPartComparator.__sortDirection);

		_columnManager.saveState(_state);
	}

	@Override
	public void setFocus() {

		_geoPartViewer.getTable().setFocus();
	}

	private void setState_CancelComparing() {

		_lblProgressBar.setText("Comparing is canceled");

		setState_StopComparing();
	}

	private void setState_StopComparing() {

		GeoPartTourLoader.stopLoading(_compareData_PreviousGeoPartItem);

		// reset last id that the same compare can be restarted
		_compareData_TourId = Long.MIN_VALUE;
	}

	private void showRefTour(final long refId) {

		final TourCompareConfig tourCompareConfig = ReferenceTourManager.getInstance().getTourCompareConfig(refId);

		if (tourCompareConfig == null) {
			return;
		}

		final TourData tourData = tourCompareConfig.getRefTourData();
		if (tourData != null) {

			final TourReference refTour = tourCompareConfig.getRefTour();

			compare_10_Compare(
					tourData,
					refTour.getStartValueIndex(),
					refTour.getEndValueIndex(),
					refTour.getRefId());
		}
	}

	@Override
	public void updateColumnHeader(final ColumnDefinition colDef) {}

	private void updateUI_GeoAccuracy() {

		final double latStart = 0;
		final double latEnd = 1.0 / _geoAccuracy;

		final double lonStart = 0;
		final double lonEnd = 1.0 / _geoAccuracy;

		final double distDiff = MtMath.distanceVincenty(latStart, lonStart, latEnd, lonEnd);

		final double distValue = distDiff / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE_SMALL;

		final String valueFormatting = distValue > 100
				? "%1.0f %s"
				: distValue > 10
						? "%1.1f %s"//
						: "%1.2f %s";

		final String geoDistance = String.format(valueFormatting, distValue, UI.UNIT_LABEL_DISTANCE_SMALL);

		_lblGeoAccuracy.setText(geoDistance);
	}

	private void updateUI_GeoItem(final GeoPartItem geoItem) {

		if (geoItem == null) {

			_lblTitle.setText(UI.EMPTY_STRING);
			_lblNumTours.setText(UI.EMPTY_STRING);
//			_lblSqlRuntime.setText(UI.EMPTY_STRING);

		} else {

			_lblTitle.setText(_compareData_TourTitle);
			_lblNumTours.setText(Integer.toString(geoItem.tourIds.length));
//			_lblSqlRuntime.setText(Long.toString(geoItem.sqlRunningTime) + " ms"); //$NON-NLS-1$
		}
	}

	private void updateUI_Progress(final int workedTours, final int numTours) {

		if (workedTours == numTours) {

			_lblProgressBar.setText("Comparing is done");

		} else {

			_lblProgressBar.setText(NLS.bind("Comparing tours: {0} / {1}", workedTours, numTours)); //$NON-NLS-1$
		}
	}

	/**
	 * Select and reveal a compare item item.
	 * 
	 * @param selection
	 */
	private void updateUI_SelectCompareItem(final ISelection selection) {

		_isInUpdate = true;
		{
			_geoPartViewer.setSelection(selection, true);

			final Table table = _geoPartViewer.getTable();
			table.showSelection();
		}
		_isInUpdate = false;
	}

	/**
	 * Set the sort column direction indicator for a column.
	 * 
	 * @param sortColumnId
	 * @param isAscendingSort
	 */
	private void updateUI_SetSortDirection(final String sortColumnId, final int sortDirection) {

		final Table table = _geoPartViewer.getTable();
		final TableColumn tc = getSortColumn(sortColumnId);

		table.setSortColumn(tc);
		table.setSortDirection(sortDirection == CompareResultComparator.ASCENDING ? SWT.UP : SWT.DOWN);
	}

	private void updateUI_Viewer() {

		_geoPartViewer.setInput(new Object[0]);

//		enableActions();
	}
}
