/*******************************************************************************
 * Copyright (C) 2005, 2014 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourMarker;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.SQL;
import net.tourbook.common.util.TableColumnDefinition;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ActionOpenMarkerDialog;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.SelectionTourMarker;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.Messages;
import net.tourbook.ui.TableColumnFactory;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.ui.action.ActionEditTour;
import net.tourbook.ui.action.ActionModifyColumns;
import net.tourbook.ui.action.ActionOpenTour;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

/**
 * This view displays all available {@link TourMarker}.
 */
public class TourMarkerAllView extends ViewPart implements ITourProvider, ITourViewer {

	public static final String			ID									= "net.tourbook.ui.views.TourMarkerAllView";	//$NON-NLS-1$
	//
	private static final String			COLUMN_ALTITUDE						= "Altitude";									//$NON-NLS-1$
	private static final String			COLUMN_DATE							= "Date";										//$NON-NLS-1$
	private static final String			COLUMN_DESCRIPTION					= "Description";								//$NON-NLS-1$
	private static final String			COLUMN_LATITUDE						= "Latitude";									//$NON-NLS-1$
	private static final String			COLUMN_LONGITUDE					= "Longitude";									//$NON-NLS-1$
	private static final String			COLUMN_MARKER_ID					= "MarkerId";									//$NON-NLS-1$
	private static final String			COLUMN_NAME							= "Name";										//$NON-NLS-1$
	private static final String			COLUMN_TOUR_ID						= "TourId";									//$NON-NLS-1$
	private static final String			COLUMN_TIME							= "Time";										//$NON-NLS-1$
	private static final String			COLUMN_URL_ADDRESS					= "UrlAddress";								//$NON-NLS-1$
	private static final String			COLUMN_URL_LABEL					= "UrlLabel";									//$NON-NLS-1$
	//
	static final String					STATE_GEO_FILTER_AREA				= "STATE_GEO_FILTER_AREA";						//$NON-NLS-1$
	private static final String			STATE_GPS_FILTER					= "STATE_GPS_FILTER";							//$NON-NLS-1$
	static final String					STATE_IS_LAT_LON_DIGITS_ENABLED		= "STATE_IS_LAT_LON_DIGITS_ENABLED";			//$NON-NLS-1$
	static final String					STATE_LAT_LON_DIGITS				= "STATE_LAT_LON_DIGITS";						//$NON-NLS-1$
	private static final String			STATE_SELECTED_MARKER_ITEM			= "STATE_SELECTED_MARKER_ITEM";				//$NON-NLS-1$
	private static final String			STATE_SORT_COLUMN_DIRECTION			= "STATE_SORT_COLUMN_DIRECTION";				//$NON-NLS-1$
	private static final String			STATE_SORT_COLUMN_ID				= "STATE_SORT_COLUMN_ID";						//$NON-NLS-1$

	static final double					DEFAULT_GEO_FILTER_AREA				= 0.05;
	static final boolean				DEFAULT_IS_LAT_LON_DIGITS_ENABLED	= true;
	static final int					DEFAULT_LAT_LON_DIGITS				= 5;
	//
	private static int					GPS_MARKER_FILTER_IS_DISABLED		= 0;
	private static int					GPS_MARKER_FILTER_WITH_GPS			= 1;
	private static int					GPS_MARKER_FILTER_WITHOUT_GPS		= 2;
	//
	private final IPreferenceStore		_prefStore							= TourbookPlugin.getPrefStore();
	private final IDialogSettings		_state								= TourbookPlugin.getState(ID);
	//
	private PostSelectionProvider		_postSelectionProvider;
	//
	private IPartListener2				_partListener;
	private IPropertyChangeListener		_prefChangeListener;
	private ITourEventListener			_tourEventListener;
	//
	private ActionEditTour				_actionEditTour;
	private ActionModifyColumns			_actionModifyColumns;
	private ActionOpenMarkerDialog		_actionOpenMarkerDialog;
	private ActionOpenTour				_actionOpenTour;
	private ActionEditQuick				_actionQuickEdit;
	private ActionTourMarkerFilter		_actionTourMarkerFilter;
	private ActionMarkerFilterWithGPS	_actionTourFilterWithGPS;
	private ActionMarkerFilterWithNoGPS	_actionTourFilterWithoutGPS;
	//
	private CheckboxTableViewer			_markerViewer;
	private MarkerComparator			_markerComparator					= new MarkerComparator();
	private ColumnManager				_columnManager;
	private SelectionAdapter			_columnSortListener;

	private ArrayList<TourMarkerItem>	_allMarkerItems						= new ArrayList<TourMarkerItem>();

	private int							_gpsMarkerFilter					= GPS_MARKER_FILTER_IS_DISABLED;

	/**
	 * Number of digits for the lat/lon columns.
	 */
	private int							_latLonDigits;
	private boolean						_isLatLonDigitsEnabled;

	/**
	 * Is <code>true</code> when markers are filtered with the geo filter.
	 */
	private boolean						_isGeoFilterActive;
	private double						_geoFilterLat;
	private double						_geoFilterLon;
	private double						_geoFilterMaxDiff;

	private boolean						_isInUpdate;

	private final DateFormat			_dateFormatter						= DateFormat
																					.getDateInstance(DateFormat.SHORT);
	private final DateFormat			_timeFormatter						= DateFormat
																					.getTimeInstance(DateFormat.SHORT);
	//
	private final NumberFormat			_nf1								= NumberFormat.getNumberInstance();
	private final NumberFormat			_nf3								= NumberFormat.getNumberInstance();
	private final NumberFormat			_nfLatLon							= NumberFormat.getNumberInstance();
	{
		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);
		_nf3.setMinimumFractionDigits(3);
		_nf3.setMaximumFractionDigits(3);
	}

	/*
	 * UI controls
	 */
	private PixelConverter				_pc;
	private Composite					_uiParent;
	private Composite					_viewerContainer;
	private TourMarkerItem				_markerFilter;

	private class MarkerComparator extends ViewerComparator {

		private static final int	ASCENDING		= 0;
		private static final int	DESCENDING		= 1;

		private String				__sortColumnId	= COLUMN_TOUR_ID;
		private int					__sortDirection;

		@Override
		public int compare(final Viewer viewer, final Object e1, final Object e2) {

			final TourMarkerItem m1 = (TourMarkerItem) e1;
			final TourMarkerItem m2 = (TourMarkerItem) e2;

			boolean _isSortByTime = false;
			double rc = 0;

			// Determine which column and do the appropriate sort
			switch (__sortColumnId) {
			case COLUMN_ALTITUDE:
				rc = m1.altitude - m2.altitude;
				break;

			case COLUMN_DESCRIPTION:
				rc = m1.description.compareTo(m2.description);
				break;

			case COLUMN_LATITUDE:
				rc = m1.latitude - m2.latitude;
				if (rc == 0) {
					rc = m1.longitude - m2.longitude;
				}
				_isSortByTime = true;
				break;

			case COLUMN_LONGITUDE:
				rc = m1.longitude - m2.longitude;
				if (rc == 0) {
					rc = m1.latitude - m2.latitude;
				}
				_isSortByTime = true;
				break;

			case COLUMN_MARKER_ID:
				rc = m1.markerId - m2.markerId;
				break;

			case COLUMN_TOUR_ID:
				rc = m1.tourId - m2.tourId;
				break;

			case COLUMN_DATE:
			case COLUMN_TIME:
				rc = m1.time - m2.time;
				break;

			case COLUMN_URL_ADDRESS:
				rc = m1.urlAddress.compareTo(m2.urlAddress);
				break;

			case COLUMN_URL_LABEL:
				rc = m1.urlLabel.compareTo(m2.urlLabel);
				break;

			case COLUMN_NAME:
			default:
				rc = m1.label.compareTo(m2.label);
				_isSortByTime = true;
			}

			if (rc == 0 && _isSortByTime) {
				rc = m1.time - m2.time;
			}

			// If descending order, flip the direction
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

	private class MarkerContentProvider implements IStructuredContentProvider {

		@Override
		public void dispose() {}

		@Override
		public Object[] getElements(final Object inputElement) {
			return _allMarkerItems.toArray();
		}

		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	public class MarkerItemFilter extends ViewerFilter {

		@Override
		public boolean select(final Viewer viewer, final Object parentElement, final Object element) {

			final TourMarkerItem markerItem = (TourMarkerItem) element;

			final boolean isTourWithGPS = markerItem.latitude != TourDatabase.DEFAULT_DOUBLE;
			final boolean isTourWithoutGPS = markerItem.latitude == TourDatabase.DEFAULT_DOUBLE;

			boolean isVisible;

			/*
			 * Check GPS filter
			 */
			if (_gpsMarkerFilter == GPS_MARKER_FILTER_IS_DISABLED) {

				isVisible = true;

			} else {

				isVisible = _gpsMarkerFilter == GPS_MARKER_FILTER_WITH_GPS ? isTourWithGPS : isTourWithoutGPS;
			}

			if (isVisible == false) {
				return false;
			}

			/*
			 * Check geo filter
			 */
			if (_isGeoFilterActive == false) {
				return true;
			}

			if (isTourWithoutGPS) {
				return false;
			}

			// Lat filter
			final double lat = markerItem.latitude;
			final double latDiff = _geoFilterLat - lat;
			if (latDiff > 0) {

				if (latDiff > _geoFilterMaxDiff) {
					return false;
				}

			} else {

				if (-latDiff > _geoFilterMaxDiff) {
					return false;
				}
			}

			// Lon filter
			final double lon = markerItem.longitude;
			final double lonDiff = _geoFilterLon - lon;
			if (lonDiff > 0) {

				if (lonDiff > _geoFilterMaxDiff) {
					return false;
				}
			} else {

				if (-lonDiff > _geoFilterMaxDiff) {
					return false;
				}
			}

			return true;
		}
	}

	class TourMarkerItem {

		public long		markerId;

		public long		tourId;
		public String	label;
		public String	description;
		public String	urlLabel;
		public String	urlAddress;
		public double	latitude;
		public double	longitude;
		public float	altitude;
		public long		time;

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof TourMarkerItem)) {
				return false;
			}
			final TourMarkerItem other = (TourMarkerItem) obj;
			if (!getOuterType().equals(other.getOuterType())) {
				return false;
			}
			if (markerId != other.markerId) {
				return false;
			}
			return true;
		}

		private TourMarkerAllView getOuterType() {
			return TourMarkerAllView.this;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + (int) (markerId ^ (markerId >>> 32));
			return result;
		}

		@Override
		public String toString() {
			return "TourMarkerItem [_label=" + label + "]"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	public TourMarkerAllView() {
		super();
	}

	void actionMarkerFilterGPS(final Action actionTourFilter) {

		// toggle with/without GPS actions

		if (actionTourFilter == _actionTourFilterWithGPS) {

			// action with GPS is hit

			if (_gpsMarkerFilter != GPS_MARKER_FILTER_WITH_GPS) {

				_gpsMarkerFilter = GPS_MARKER_FILTER_WITH_GPS;

			} else {

				_gpsMarkerFilter = GPS_MARKER_FILTER_IS_DISABLED;
			}

			_actionTourFilterWithoutGPS.setChecked(false);

		} else {

			// action without GPS is hit

			if (_gpsMarkerFilter != GPS_MARKER_FILTER_WITHOUT_GPS) {

				_gpsMarkerFilter = GPS_MARKER_FILTER_WITHOUT_GPS;

			} else {

				_gpsMarkerFilter = GPS_MARKER_FILTER_IS_DISABLED;
			}

			_actionTourFilterWithGPS.setChecked(false);
		}

		refilterViewer();

		_markerViewer.getTable().setFocus();
	}

	private void addPartListener() {

		_partListener = new IPartListener2() {

			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourMarkerAllView.this) {
					saveState();
				}
			}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {}

			@Override
			public void partVisible(final IWorkbenchPartReference partRef) {}
		};

		getViewSite().getPage().addPartListener(_partListener);
	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

					_columnManager.saveState(_state);
					_columnManager.clearColumns();

					defineAllColumns();

					_markerViewer = (CheckboxTableViewer) recreateViewer(_markerViewer);

				} else if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

					_markerViewer.getTable().setLinesVisible(
							_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

					_markerViewer.refresh();

					/*
					 * the tree must be redrawn because the styled text does not show with the new
					 * color
					 */
					_markerViewer.getTable().redraw();

				}
			}
		};
		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	private void addTourEventListener() {

		_tourEventListener = new ITourEventListener() {
			@Override
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (part == TourMarkerAllView.this) {
					return;
				}

				if (_isInUpdate) {
					return;
				}

				if ((eventId == TourEventId.TOUR_CHANGED) && (eventData instanceof TourEvent)) {

					final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
					if (modifiedTours != null) {

						// update modified tour

						reloadViewer();
					}

				} else if (eventId == TourEventId.MARKER_SELECTION) {

					onTourEvent_TourMarker(eventData);

				} else if (eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

					reloadViewer();
				}
			}
		};

		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	private void createActions() {

		_actionEditTour = new ActionEditTour(this);
		_actionModifyColumns = new ActionModifyColumns(this);
		_actionOpenMarkerDialog = new ActionOpenMarkerDialog(this, true);
		_actionOpenTour = new ActionOpenTour(this);
		_actionQuickEdit = new ActionEditQuick(this);
		_actionTourFilterWithGPS = new ActionMarkerFilterWithGPS(this);
		_actionTourFilterWithoutGPS = new ActionMarkerFilterWithNoGPS(this);
		_actionTourMarkerFilter = new ActionTourMarkerFilter(this, _uiParent);
	}

	@Override
	public void createPartControl(final Composite parent) {

		_uiParent = parent;

		initUI(parent);

		restoreState_BeforeUI();

		// define all columns for the viewer
		_columnManager = new ColumnManager(this, _state);
		defineAllColumns();

		createUI(parent);

		addTourEventListener();
		addPrefListener();
		addPartListener();

		// set selection provider
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

		createActions();
		fillToolbar();

		// load marker and display them
//		parent.getDisplay().asyncExec(new Runnable() {
//			public void run() {
//
//			}
//		});
		BusyIndicator.showWhile(parent.getDisplay(), new Runnable() {
			@Override
			public void run() {

				loadAllMarker();

				updateUI_SetViewerInput();

				restoreState_WithUI();
			}
		});
	}

	private void createUI(final Composite parent) {

		_viewerContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
		{
			createUI_10_MarkerViewer(_viewerContainer);
		}
	}

	private void createUI_10_MarkerViewer(final Composite parent) {

		/*
		 * create table
		 */
		final Table table = new Table(parent, SWT.FULL_SELECTION | SWT.MULTI | SWT.CHECK);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

		table.setHeaderVisible(true);
		table.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

		/*
		 * It took a while that the correct listener is set and also the checked item is fired and
		 * not the wrong selection.
		 */
		table.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(final Event event) {
				onSelect_TourMarker(event);
			}
		});
		;

		/*
		 * create table viewer
		 */
		_markerViewer = new CheckboxTableViewer(table);

		_columnManager.createColumns(_markerViewer);

		_markerViewer.setUseHashlookup(true);
		_markerViewer.setContentProvider(new MarkerContentProvider());
		_markerViewer.addFilter(new MarkerItemFilter());
		_markerViewer.setComparator(_markerComparator);

		_markerViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(final DoubleClickEvent event) {
				onTourMarker_DoubleClick();
			}
		});

		_markerViewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(final CheckStateChangedEvent event) {
				onTourMarker_StateChanged(event);
			}
		});

		updateUI_SetSortDirection(//
				_markerComparator.__sortColumnId,
				_markerComparator.__sortDirection);

		createUI_20_ContextMenu();
	}

	/**
	 * create the views context menu
	 */
	private void createUI_20_ContextMenu() {

		final MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(final IMenuManager manager) {
				fillContextMenu(manager);
			}
		});

		final Table table = (Table) _markerViewer.getControl();
		final Menu tableContextMenu = menuMgr.createContextMenu(table);

		_columnManager.createHeaderContextMenu(table, tableContextMenu);
	}

	private void defineAllColumns() {

		defineColumn_Name();

		defineColumn_Date();
		defineColumn_Time();

		defineColumn_Altitude();
		defineColumn_Latitude();
		defineColumn_Longitude();

		defineColumn_Description();
		defineColumn_UrlLabel();
		defineColumn_UrlAddress();

		defineColumn_MarkerId();
		defineColumn_TourId();
	}

	/**
	 * Column: Altitude
	 */
	private void defineColumn_Altitude() {

		final ColumnDefinition colDef = TableColumnFactory.ALTITUDE_ALTITUDE.createColumn(_columnManager, _pc);

		// overwrite column id to identify the column when table is sorted
		colDef.setColumnId(COLUMN_ALTITUDE);
		colDef.setColumnSelectionListener(_columnSortListener);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				String valueText;
				final float altitude = ((TourMarkerItem) cell.getElement()).altitude;

				if (altitude == TourDatabase.DEFAULT_FLOAT) {
					valueText = UI.EMPTY_STRING;
				} else {
					valueText = _nf1.format(altitude / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE);
				}

				cell.setText(valueText);
			}
		});
	}

	/**
	 * Column: Date
	 */
	private void defineColumn_Date() {

		final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_DATE, SWT.TRAIL);

		colDef.setColumnName(Messages.ColumnFactory_Waypoint_Date);

		colDef.setIsDefaultColumn();
		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(12));
		colDef.setColumnSelectionListener(_columnSortListener);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourMarkerItem tourMarkerItem = (TourMarkerItem) cell.getElement();

				cell.setText(_dateFormatter.format(tourMarkerItem.time));
			}
		});

	}

	/**
	 * Column: Description
	 */
	private void defineColumn_Description() {

		final ColumnDefinition colDef = TableColumnFactory.WAYPOINT_DESCRIPTION.createColumn(_columnManager, _pc);

		colDef.setIsDefaultColumn();

		// overwrite column id to identify the column when table is sorted
		colDef.setColumnId(COLUMN_DESCRIPTION);
		colDef.setColumnSelectionListener(_columnSortListener);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourMarkerItem marker = (TourMarkerItem) cell.getElement();
				cell.setText(marker.description);
			}
		});
	}

	/**
	 * Column: Latitude
	 */
	private void defineColumn_Latitude() {

		final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_LATITUDE, SWT.TRAIL);

		colDef.setColumnName(Messages.ColumnFactory_latitude);
		colDef.setIsDefaultColumn();

		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(11));
		colDef.setColumnSelectionListener(_columnSortListener);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				String valueText;
				final double latitude = ((TourMarkerItem) cell.getElement()).latitude;

				if (latitude == TourDatabase.DEFAULT_DOUBLE) {
					valueText = UI.EMPTY_STRING;
				} else {
					if (_isLatLonDigitsEnabled) {
						valueText = _nfLatLon.format(latitude);
					} else {
						valueText = Double.toString(latitude);
					}
				}

				cell.setText(valueText);
			}
		});
	}

	/**
	 * Column: Longitude
	 */
	private void defineColumn_Longitude() {

		final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_LONGITUDE, SWT.TRAIL);

		colDef.setColumnName(Messages.ColumnFactory_longitude);
		colDef.setIsDefaultColumn();

		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(11));
		colDef.setColumnSelectionListener(_columnSortListener);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				String valueText;
				final double longitude = ((TourMarkerItem) cell.getElement()).longitude;

				if (longitude == TourDatabase.DEFAULT_DOUBLE) {
					valueText = UI.EMPTY_STRING;
				} else {
					if (_isLatLonDigitsEnabled) {
						valueText = _nfLatLon.format(longitude);
					} else {
						valueText = Double.toString(longitude);
					}
				}

				cell.setText(valueText);
			}
		});
	}

	/**
	 * Column: Marker ID
	 */
	private void defineColumn_MarkerId() {

		final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_MARKER_ID, SWT.LEAD);

		colDef.setColumnName(Messages.ColumnFactory_TourMarkerId);

		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(12));
		colDef.setColumnSelectionListener(_columnSortListener);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				cell.setText(Long.toString(((TourMarkerItem) cell.getElement()).markerId));
			}
		});
	}

	/**
	 * Column: Name
	 */
	private void defineColumn_Name() {

		final ColumnDefinition colDef = TableColumnFactory.WAYPOINT_NAME.createColumn(_columnManager, _pc);

		colDef.setIsDefaultColumn();

		// overwrite column id to identify the column when table is sorted
		colDef.setColumnId(COLUMN_NAME);
		colDef.setColumnSelectionListener(_columnSortListener);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourMarkerItem marker = (TourMarkerItem) cell.getElement();
				cell.setText(marker.label);
			}
		});
	}

	/**
	 * Column: Time
	 */
	private void defineColumn_Time() {

		final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_TIME, SWT.TRAIL);

		colDef.setColumnName(Messages.ColumnFactory_tour_time_label_hhmmss);

		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(12));

		/*
		 * a marker cannot be sorted by time only by date
		 */
//		colDef.setColumnSelectionListener(_columnSortListener);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourMarkerItem tourMarkerItem = (TourMarkerItem) cell.getElement();

				cell.setText(_timeFormatter.format(tourMarkerItem.time));
			}
		});

	}

	/**
	 * Column: TourID
	 */
	private void defineColumn_TourId() {

		final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_TOUR_ID, SWT.LEAD);

		colDef.setColumnName(Messages.ColumnFactory_TourId);

		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(22));
		colDef.setColumnSelectionListener(_columnSortListener);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				cell.setText(Long.toString(((TourMarkerItem) cell.getElement()).tourId));
			}
		});

	}

	/**
	 * Column: Url address
	 */
	private void defineColumn_UrlAddress() {

		final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_URL_ADDRESS, SWT.LEAD);

		colDef.setColumnName(Messages.ColumnFactory_InetAddress_Url);

		colDef.setIsDefaultColumn();
		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(20));
		colDef.setColumnSelectionListener(_columnSortListener);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final TourMarkerItem markerItem = (TourMarkerItem) cell.getElement();
				cell.setText(markerItem.urlAddress);
			}
		});
	}

	/**
	 * Column: Url label
	 */
	private void defineColumn_UrlLabel() {

		final ColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_URL_LABEL, SWT.LEAD);

		colDef.setColumnName(Messages.ColumnFactory_InetAddress_Label);

		colDef.setIsDefaultColumn();
		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(20));
		colDef.setColumnSelectionListener(_columnSortListener);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final TourMarkerItem markerItem = (TourMarkerItem) cell.getElement();
				cell.setText(markerItem.urlLabel);
			}
		});
	}

	@Override
	public void dispose() {

		TourManager.getInstance().removeTourEventListener(_tourEventListener);

		getViewSite().getPage().removePartListener(_partListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	private void enableActions() {

		final TourMarker tourMarker = getSelectedTourMarker();
		final boolean isMarkerSelected = tourMarker != null;
		final boolean isGeoFilterDeactivated = _isGeoFilterActive == false;

		_actionEditTour.setEnabled(isMarkerSelected);
		_actionOpenMarkerDialog.setEnabled(isMarkerSelected);
		_actionOpenTour.setEnabled(isMarkerSelected);
		_actionQuickEdit.setEnabled(isMarkerSelected);

		_actionTourFilterWithGPS.setEnabled(isGeoFilterDeactivated);
		_actionTourFilterWithoutGPS.setEnabled(isGeoFilterDeactivated);
	}

	private void fillContextMenu(final IMenuManager menuMgr) {

		final TourMarker tourMarker = getSelectedTourMarker();

		/*
		 * update actions
		 */
		_actionOpenMarkerDialog.setTourMarker(tourMarker);

		/*
		 * fill menu
		 */
		menuMgr.add(_actionOpenMarkerDialog);

		menuMgr.add(new Separator());
		menuMgr.add(_actionQuickEdit);
		menuMgr.add(_actionEditTour);
		menuMgr.add(_actionOpenTour);

		enableActions();
	}

	private void fillToolbar() {

		final IActionBars actionBars = getViewSite().getActionBars();

		/*
		 * Fill view menu
		 */
		final IMenuManager menuMgr = actionBars.getMenuManager();

		menuMgr.add(_actionModifyColumns);

		/*
		 * Fill view toolbar
		 */
		final IToolBarManager tbm = actionBars.getToolBarManager();

		tbm.add(_actionTourFilterWithGPS);
		tbm.add(_actionTourFilterWithoutGPS);
		tbm.add(_actionTourMarkerFilter);
	}

	private void fireSelection(final StructuredSelection selection) {

		// get unique tour id's
		final HashSet<Long> tourIds = new HashSet<Long>();
		for (final Iterator<?> selectedItem = selection.iterator(); selectedItem.hasNext();) {
			tourIds.add(((TourMarkerItem) selectedItem.next()).tourId);
		}

		final SelectionTourIds selectionTourIds = new SelectionTourIds(new ArrayList<>(tourIds));

		final int uniqueTourIds = tourIds.size();

		if (uniqueTourIds == 1) {

			// only one unique tour is selected, fire a marker selection

			final TourMarkerItem firstMarkerItem = (TourMarkerItem) selection.getFirstElement();

			// get tour by id
			final TourData tourData = TourManager.getInstance().getTourData(firstMarkerItem.tourId);
			if (tourData == null) {
				return;
			}

			final ArrayList<TourMarker> selectedTourMarkers = new ArrayList<TourMarker>();

			for (final Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {

				final long selectedMarkerId = ((TourMarkerItem) iterator.next()).markerId;

				// get marker by id
				for (final TourMarker tourMarker : tourData.getTourMarkers()) {
					if (tourMarker.getMarkerId() == selectedMarkerId) {
						selectedTourMarkers.add(tourMarker);
						break;
					}
				}
			}

			_isInUpdate = true;
			{
				/*
				 * Set selection in the selection provider that the part selection provider is in
				 * sync with the marker selection, otherwise a selection listener, e.g. tour editor
				 * can display the wrong tour. This happened !!!
				 */
				_postSelectionProvider.setSelectionNoFireEvent(selectionTourIds);

				TourManager.fireEventWithCustomData(//
						TourEventId.MARKER_SELECTION,
						new SelectionTourMarker(tourData, selectedTourMarkers),
						getSite().getPart());
			}
			_isInUpdate = false;

		} else if (uniqueTourIds > 1) {

			// multiple tours are selected, fire a part selection

			_isInUpdate = true;
			{
				_postSelectionProvider.setSelection(selectionTourIds);
			}
			_isInUpdate = false;
		}
	}

	@Override
	public ColumnManager getColumnManager() {
		return _columnManager;
	}

	/**
	 * @return Returns the tour marker when the marker filter is active otherwise it returns
	 *         <code>null</code>.
	 */
	TourMarkerItem getFilterMarker() {
		return _markerFilter;
	}

	public Object getMarkerViewer() {
		return _markerViewer;
	}

	private TourMarker getSelectedTourMarker() {

		final TourMarkerItem tourMarkerItem = getSelectedTourMarkerItem();
		if (tourMarkerItem == null) {
			return null;
		}

		// get tour by id
		final TourData tourData = TourManager.getInstance().getTourData(tourMarkerItem.tourId);
		if (tourData == null) {
			return null;
		}

		final long markerId = tourMarkerItem.markerId;

		// get marker by id
		for (final TourMarker tourMarker : tourData.getTourMarkers()) {
			if (tourMarker.getMarkerId() == markerId) {
				return tourMarker;
			}
		}

		return null;
	}

	private TourMarkerItem getSelectedTourMarkerItem() {

		final StructuredSelection selection = getViewerSelection();

		if (selection.getFirstElement() instanceof TourMarkerItem) {
			return (TourMarkerItem) selection.getFirstElement();
		}

		return null;
	}

	@Override
	public ArrayList<TourData> getSelectedTours() {

		final ArrayList<TourData> selectedTours = new ArrayList<TourData>();

		final StructuredSelection selection = getViewerSelection();

		for (final Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {

			final Object element = iterator.next();

			if (element instanceof TourMarkerItem) {

				// get TourData from the tour marker item

				final TourMarkerItem tourMarkerItem = (TourMarkerItem) element;

				// get tour by id
				final TourData tourData = TourManager.getInstance().getTourData(tourMarkerItem.tourId);

				if (tourData != null) {
					selectedTours.add(tourData);
				}
			}
		}

		return selectedTours;
	}

	/**
	 * @param sortColumnId
	 * @return Returns the column widget by it's column id, when column id is not found then the
	 *         first column is returned.
	 */
	private TableColumn getSortColumn(final String sortColumnId) {

		final TableColumn[] allColumns = _markerViewer.getTable().getColumns();

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
		return _markerViewer;
	}

	private StructuredSelection getViewerSelection() {

		return (StructuredSelection) _markerViewer.getSelection();
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		_columnSortListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelect_SortColumn(e);
			}
		};
	}

	boolean isGeoFilterActive() {
		return _isGeoFilterActive;
	}

	boolean isLatLonDigits() {
		return _isLatLonDigitsEnabled;
	}

	private void loadAllMarker() {

//		final long start = System.nanoTime();

		_allMarkerItems.clear();

//		final double latLonFactor = Math.pow(10, _latLonDigits);

		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet result = null;

		try {

			conn = TourDatabase.getInstance().getConnection();

			final String tblTourMarker = TourDatabase.TABLE_TOUR_MARKER;
			final String tourKey = TourDatabase.KEY_TOUR;

			final String sql = "SELECT " // //$NON-NLS-1$
					+ "MarkerID, " //						// 1 //$NON-NLS-1$
					+ (tourKey + ", ") //					// 2 //$NON-NLS-1$
					+ "Label, " //							// 3 //$NON-NLS-1$
					+ "description, " //					// 4 //$NON-NLS-1$
					+ "urlText, " //						// 5 //$NON-NLS-1$
					+ "urlAddress, " //						// 6 //$NON-NLS-1$
					+ "latitude, " //						// 7 //$NON-NLS-1$
					+ "longitude, " //						// 8 //$NON-NLS-1$
					+ "altitude, " //						// 9 //$NON-NLS-1$
					+ "tourTime " //						// 10 //$NON-NLS-1$
					//
					+ UI.NEW_LINE
					//
					+ (" FROM " + tblTourMarker + UI.NEW_LINE) //$NON-NLS-1$
			//
			;

			statement = conn.prepareStatement(sql);
			result = statement.executeQuery();

			while (result.next()) {

				final TourMarkerItem markerItem = new TourMarkerItem();
				_allMarkerItems.add(markerItem);

				final String dbLabel = result.getString(3);
				final String dbDescription = result.getString(4);
				final String dbUrlLabel = result.getString(5);
				final String dbUrlAddress = result.getString(6);

				/*
				 * Reduce accuracy that markers sorted by lat/lon are near together
				 */
				final double dbLatitude = result.getDouble(7);
				final double dbLongitude = result.getDouble(8);
				if (dbLatitude == TourDatabase.DEFAULT_DOUBLE) {

					markerItem.latitude = dbLatitude;
					markerItem.longitude = dbLongitude;

				} else {

//					if (_isLatLonDigitsEnabled) {
//
//						// set lat/lon accuracy
//
//						final long longLat = (long) (dbLatitude * latLonFactor);
//						final long longLon = (long) (dbLongitude * latLonFactor);
//
//						markerItem.latitude = longLat / latLonFactor;
//						markerItem.longitude = longLon / latLonFactor;
//
//					} else {
//
					markerItem.latitude = dbLatitude;
					markerItem.longitude = dbLongitude;
//
//					}
				}

				markerItem.markerId = result.getLong(1);
				markerItem.tourId = result.getLong(2);
				markerItem.label = dbLabel == null ? UI.EMPTY_STRING : dbLabel;
				markerItem.description = dbDescription == null ? UI.EMPTY_STRING : dbDescription;
				markerItem.urlLabel = dbUrlLabel == null ? UI.EMPTY_STRING : dbUrlLabel;
				markerItem.urlAddress = dbUrlAddress == null ? UI.EMPTY_STRING : dbUrlAddress;
				markerItem.altitude = result.getFloat(9);
				markerItem.time = result.getLong(10);
			}

		} catch (final SQLException e) {
			SQL.showException(e);
		} finally {
			Util.closeSql(conn);
			Util.closeSql(statement);
			Util.closeSql(result);
		}

//		System.out.println((UI.timeStampNano() + " " + this.getClass().getName() + " \t")
//				+ (((float) (System.nanoTime() - start) / 1000000) + " ms"));
//		// TODO remove SYSTEM.OUT.PRINTLN
	}

	private void onSelect_SortColumn(final SelectionEvent e) {

		_viewerContainer.setRedraw(false);
		{
			// keep selection
			final ISelection selectionBackup = getViewerSelection();
			{
				// update viewer with new sorting
				_markerComparator.setSortColumn(e.widget);
				_markerViewer.refresh();
			}
			updateUI_SelectTourMarker(selectionBackup, null);
		}
		_viewerContainer.setRedraw(true);
	}

	private void onSelect_TourMarker(final Event event) {

		if (_isInUpdate) {
			return;
		}

		StructuredSelection selection;

		if (event.detail == SWT.CHECK) {

			// checkbox is hit

			/**
			 * !!! VERY important !!!
			 * <p>
			 * Table.getSelection() returns the wrong selection when the user clicked in a checkbox.
			 */

			final TableItem item = (TableItem) event.item;
			final Object selectedMarker = item.getData();

			selection = new StructuredSelection(selectedMarker);

		} else {

			// checkbox is not hit

			selection = (StructuredSelection) _markerViewer.getSelection();
		}

		fireSelection(selection);
	}

	private void onTourEvent_TourMarker(final Object eventData) {

		if (eventData instanceof SelectionTourMarker) {

			// select the tour marker item in the view

			final SelectionTourMarker selection = (SelectionTourMarker) eventData;
			final ArrayList<TourMarker> allTourMarker = selection.getSelectedTourMarker();

			final long selectedTourMarkerId = allTourMarker.get(0).getMarkerId();

			// find tour marker in marker items
			TourMarkerItem selectedMarkerItem = null;
			for (final TourMarkerItem tourMarker : _allMarkerItems) {
				if (tourMarker.markerId == selectedTourMarkerId) {
					selectedMarkerItem = tourMarker;
					break;
				}
			}

			if (selectedMarkerItem != null) {

				final StructuredSelection markerViewerSelection = new StructuredSelection(selectedMarkerItem);

				updateUI_SelectTourMarker(markerViewerSelection, null);
			}
		}
	}

	private void onTourMarker_DoubleClick() {

		final TourMarker tourMarker = getSelectedTourMarker();

		if (tourMarker == null) {
			return;
		}

		_actionOpenMarkerDialog.setTourMarker(tourMarker);
		_actionOpenMarkerDialog.run();
	}

	private void onTourMarker_StateChanged(final CheckStateChangedEvent event) {

		final boolean isChecked = event.getChecked();
		final Object checkedElement = event.getElement();
		final Object[] checkedElements = _markerViewer.getCheckedElements();

		final TourMarkerItem markerItem = (TourMarkerItem) checkedElement;
		final boolean isMarkerWithGPS = markerItem.latitude != TourDatabase.DEFAULT_DOUBLE;

		Object checkedMarker = null;

		_isGeoFilterActive = false;
		_markerFilter = null;

		if (isMarkerWithGPS) {

			if (isChecked) {

				_isGeoFilterActive = true;
				_markerFilter = markerItem;

				/*
				 * Ensure that the checked marker is also selected. It is possible that a selected
				 * and checked marker gets unchecked and another marker is checked that it is not
				 * selected.
				 */

				boolean isCheckAndSelected = false;

				final TableItem[] seletedItems = _markerViewer.getTable().getSelection();
				for (final TableItem tableItem : seletedItems) {
					if (tableItem.getData() == checkedElement) {
						isCheckAndSelected = true;
						break;
					}
				}

				if (isCheckAndSelected == false) {

					updateUI_SelectTourMarker(new StructuredSelection(checkedElement), null);
				}
			}

		} else {

			// prevent to check tour marker without GPS

			_markerViewer.setGrayed(checkedElement, true);

			checkedMarker = checkedElement;
		}

		if (isChecked && checkedElements.length > 1) {

			// only 1 marker can be checked

			checkedMarker = checkedElement;
		}

		if (checkedMarker != null) {

			for (final Object element : checkedElements) {
				if (element != checkedMarker) {
					_markerViewer.setChecked(element, false);
				}
			}

			updateUI_SelectTourMarker(new StructuredSelection(checkedMarker), null);

		}

		if (isMarkerWithGPS) {
			updateUI_GeoFilter(markerItem);
		}

		enableActions();
	}

	@Override
	public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

		_viewerContainer.setRedraw(false);
		{
			// keep selection
			final ISelection selectionBackup = getViewerSelection();
			final Object[] checkedElements = _markerViewer.getCheckedElements();
			{
				_markerViewer.getTable().dispose();

				createUI_10_MarkerViewer(_viewerContainer);

				// update UI
				_viewerContainer.layout();

				// update the viewer
				updateUI_SetViewerInput();
			}
			updateUI_SelectTourMarker(selectionBackup, checkedElements);
		}
		_viewerContainer.setRedraw(true);

		_markerViewer.getTable().setFocus();

		return _markerViewer;
	}

	private void refilterViewer() {

		BusyIndicator.showWhile(_viewerContainer.getDisplay(), new Runnable() {
			@Override
			public void run() {

				_viewerContainer.setRedraw(false);
				{
					// keep selection
					final ISelection selectionBackup = getViewerSelection();
					final Object[] checkedElements = _markerViewer.getCheckedElements();
					{
						_markerViewer.refresh(false);
					}
					updateUI_SelectTourMarker(selectionBackup, checkedElements);
				}
				_viewerContainer.setRedraw(true);
			}
		});
	}

	@Override
	public void reloadViewer() {

		loadAllMarker();

		_viewerContainer.setRedraw(false);
		{
			// keep selection
			final ISelection selectionBackup = getViewerSelection();
			final Object[] checkedElements = _markerViewer.getCheckedElements();
			{
				updateUI_SetViewerInput();
			}
			updateUI_SelectTourMarker(selectionBackup, checkedElements);
		}
		_viewerContainer.setRedraw(true);
	}

	private void restoreState_BeforeUI() {

		// GPS tour filter
		_gpsMarkerFilter = Util.getStateInt(_state, STATE_GPS_FILTER, GPS_MARKER_FILTER_IS_DISABLED);

		// lat/lon digits
		_isLatLonDigitsEnabled = Util.getStateBoolean(
				_state,
				TourMarkerAllView.STATE_IS_LAT_LON_DIGITS_ENABLED,
				TourMarkerAllView.DEFAULT_IS_LAT_LON_DIGITS_ENABLED);

		_latLonDigits = Util.getStateInt(
				_state,
				TourMarkerAllView.STATE_LAT_LON_DIGITS,
				TourMarkerAllView.DEFAULT_LAT_LON_DIGITS);
		setup_LatLonDigits();

		// geo filter accuracy
		_geoFilterMaxDiff = Util.getStateDouble(_state, STATE_GEO_FILTER_AREA, DEFAULT_GEO_FILTER_AREA);

		// sorting
		final String sortColumnId = Util.getStateString(_state, STATE_SORT_COLUMN_ID, COLUMN_NAME);
		final int sortDirection = Util.getStateInt(_state, STATE_SORT_COLUMN_DIRECTION, MarkerComparator.ASCENDING);

		// update comparator
		_markerComparator.__sortColumnId = sortColumnId;
		_markerComparator.__sortDirection = sortDirection;
	}

	private void restoreState_WithUI() {

		// GPS tour filter
		_actionTourFilterWithGPS.setChecked(_gpsMarkerFilter == GPS_MARKER_FILTER_WITH_GPS);
		_actionTourFilterWithoutGPS.setChecked(_gpsMarkerFilter == GPS_MARKER_FILTER_WITHOUT_GPS);

		/*
		 * select marker item
		 */
		final long stateMarkerId = Util.getStateLong(_state,//
				STATE_SELECTED_MARKER_ITEM,
				TourDatabase.ENTITY_IS_NOT_SAVED);

		if (stateMarkerId != TourDatabase.ENTITY_IS_NOT_SAVED) {

			// select marker item by it's ID
			for (final TourMarkerItem markerItem : _allMarkerItems) {
				if (markerItem.markerId == stateMarkerId) {

					updateUI_SelectTourMarker(new StructuredSelection(markerItem), null);

					return;
				}
			}
		}

		enableActions();
	}

	private void saveState() {

		_columnManager.saveState(_state);

		_state.put(STATE_SORT_COLUMN_ID, _markerComparator.__sortColumnId);
		_state.put(STATE_SORT_COLUMN_DIRECTION, _markerComparator.__sortDirection);

		_state.put(STATE_GPS_FILTER, _gpsMarkerFilter);

		/*
		 * selected marker item
		 */
		long markerId = TourDatabase.ENTITY_IS_NOT_SAVED;
		final StructuredSelection selection = getViewerSelection();
		final Object firstItem = selection.getFirstElement();

		if (firstItem instanceof TourMarkerItem) {
			final TourMarkerItem markerItem = (TourMarkerItem) firstItem;
			markerId = markerItem.markerId;
		}
		_state.put(STATE_SELECTED_MARKER_ITEM, markerId);
	}

	@Override
	public void setFocus() {
		_markerViewer.getTable().setFocus();
	}

	/**
	 * Validates geo position accuracy and setup's the position formatter.
	 */
	private void setup_LatLonDigits() {

		_nfLatLon.setMinimumFractionDigits(_latLonDigits);
		_nfLatLon.setMaximumFractionDigits(_latLonDigits);
	}

	@Override
	public void updateColumnHeader(final ColumnDefinition colDef) {
		// TODO Auto-generated method stub
		
	}

	void updateUI_GeoFilter() {

		// geo filter accuracy
		_geoFilterMaxDiff = Util.getStateDouble(_state, STATE_GEO_FILTER_AREA, DEFAULT_GEO_FILTER_AREA);

		refilterViewer();
	}

	private void updateUI_GeoFilter(final TourMarkerItem markerItem) {

		_geoFilterLat = markerItem.latitude;
		_geoFilterLon = markerItem.longitude;

		_viewerContainer.setRedraw(false);
		{
			_markerViewer.refresh(false);

			/*
			 * Select and reveal marker item
			 */
			final Table table = _markerViewer.getTable();
			final TableItem[] tableItems = table.getItems();

			int checkedIndex = 0;

			for (; checkedIndex < tableItems.length; checkedIndex++) {
				if (tableItems[checkedIndex].getData() == markerItem) {
					break;
				}
			}

			if (checkedIndex < tableItems.length) {
				table.setSelection(checkedIndex);
			}

		}
		_viewerContainer.setRedraw(true);

	}

	void updateUI_LatLonDigits(final boolean isEnabled, final int selectedDigits) {

		if (isEnabled == _isLatLonDigitsEnabled && selectedDigits == _latLonDigits) {
			// nothing has changed
			return;
		}

		_isLatLonDigitsEnabled = isEnabled;
		_latLonDigits = selectedDigits;

		setup_LatLonDigits();

		_viewerContainer.setRedraw(false);
		{
			// keep selection
			final ISelection selectionBackup = getViewerSelection();
			final Object[] checkedElements = _markerViewer.getCheckedElements();
			{
				_markerViewer.refresh();
			}
			updateUI_SelectTourMarker(selectionBackup, checkedElements);
		}
		_viewerContainer.setRedraw(true);

	}

	/**
	 * Select and reveal tour marker item.
	 * 
	 * @param selection
	 * @param checkedElements
	 */
	private void updateUI_SelectTourMarker(final ISelection selection, final Object[] checkedElements) {

		_isInUpdate = true;
		{
			_markerViewer.setSelection(selection, true);

			if (checkedElements != null && checkedElements.length > 0) {
				_markerViewer.setCheckedElements(checkedElements);
			}

			final Table table = _markerViewer.getTable();
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

		final Table table = _markerViewer.getTable();
		final TableColumn tc = getSortColumn(sortColumnId);

		table.setSortColumn(tc);
		table.setSortDirection(sortDirection == MarkerComparator.ASCENDING ? SWT.UP : SWT.DOWN);
	}

	private void updateUI_SetViewerInput() {

		_isInUpdate = true;
		{
			_markerViewer.setInput(new Object[0]);
		}
		_isInUpdate = false;
	}

}
