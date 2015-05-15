/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourBook;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ITourViewer3;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.TreeColumnDefinition;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.database.PersonManager;
import net.tourbook.database.TourDatabase;
import net.tourbook.extension.export.ActionExport;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageViewColors;
import net.tourbook.printing.ActionPrint;
import net.tourbook.tag.TagMenuManager;
import net.tourbook.tour.ActionOpenAdjustAltitudeDialog;
import net.tourbook.tour.ActionOpenMarkerDialog;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourDoubleClickState;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TourTypeMenuManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.ITourProviderByID;
import net.tourbook.ui.TreeColumnFactory;
import net.tourbook.ui.action.ActionCollapseAll;
import net.tourbook.ui.action.ActionCollapseOthers;
import net.tourbook.ui.action.ActionComputeDistanceValuesFromGeoposition;
import net.tourbook.ui.action.ActionComputeElevationGain;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.ui.action.ActionEditTour;
import net.tourbook.ui.action.ActionExpandSelection;
import net.tourbook.ui.action.ActionJoinTours;
import net.tourbook.ui.action.ActionModifyColumns;
import net.tourbook.ui.action.ActionOpenTour;
import net.tourbook.ui.action.ActionRefreshView;
import net.tourbook.ui.action.ActionSetAltitudeValuesFromSRTM;
import net.tourbook.ui.action.ActionSetPerson;
import net.tourbook.ui.action.ActionSetTourTypeMenu;
import net.tourbook.ui.views.TourInfoToolTipCellLabelProvider;
import net.tourbook.ui.views.TourInfoToolTipStyledCellLabelProvider;
import net.tourbook.ui.views.TreeViewerTourInfoToolTip;
import net.tourbook.ui.views.rawData.ActionMergeTour;
import net.tourbook.ui.views.rawData.ActionReimportSubMenu;

import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class TourBookView extends ViewPart implements ITourProvider, ITourViewer3, ITourProviderByID {

	static public final String							ID									= "net.tourbook.views.tourListView";						//$NON-NLS-1$

	private static final String							GRAPH_LABEL_HEARTBEAT_UNIT			= net.tourbook.common.Messages.Graph_Label_Heartbeat_Unit;

	private static final String							STATE_CSV_EXPORT_PATH				= "STATE_CSV_EXPORT_PATH";									//$NON-NLS-1$
	private static final String							STATE_SELECTED_YEAR					= "SelectedYear";											//$NON-NLS-1$
	private static final String							STATE_SELECTED_MONTH				= "SelectedMonth";											//$NON-NLS-1$
	private static final String							STATE_SELECTED_TOURS				= "SelectedTours";											//$NON-NLS-1$
	private static final String							STATE_IS_SELECT_YEAR_MONTH_TOURS	= "IsSelectYearMonthTours";								//$NON-NLS-1$
	private static final String							STATE_YEAR_SUB_CATEGORY				= "YearSubCategory";										//$NON-NLS-1$

	private static final String							CSV_HEADER_AVERAGE_CADENCE			= "AvgCadence";											//$NON-NLS-1$
	private static final String							CSV_HEADER_AVERAGE_PACE				= "AvgPace (%s)";											//$NON-NLS-1$
	private static final String							CSV_HEADER_AVERAGE_PULSE			= "AvgPulse (%s)";											//$NON-NLS-1$
	private static final String							CSV_HEADER_AVERAGE_SPEED			= "AvgSpeed (%s)";											//$NON-NLS-1$
	private static final String							CSV_HEADER_AVERAGE_TEMPERATURE		= "AvgTemperature (%s)";									//$NON-NLS-1$
	private static final String							CSV_HEADER_ALTITUDE_DOWN			= "AltitudeDown (%s)";										//$NON-NLS-1$
	private static final String							CSV_HEADER_ALTITUDE_UP				= "AltitudeUp (%s)";										//$NON-NLS-1$
	private static final String							CSV_HEADER_CALORIES					= "Calories";												//$NON-NLS-1$
	private static final String							CSV_HEADER_DAY						= "Day";													//$NON-NLS-1$
	private static final String							CSV_HEADER_DEVICE_START_DISTANCE	= "DeviceStartDistance";									//$NON-NLS-1$
	private static final String							CSV_HEADER_DISTANCE					= "Distance (%s)";											//$NON-NLS-1$
	private static final String							CSV_HEADER_DP_TOLERANCE				= "DPTolerance";											//$NON-NLS-1$
	private static final String							CSV_HEADER_GEAR_FRONT_SHIFT_COUNT	= "FrontShiftCount";										//$NON-NLS-1$
	private static final String							CSV_HEADER_GEAR_REAR_SHIFT_COUNT	= "RearShiftCount";										//$NON-NLS-1$
	private static final String							CSV_HEADER_ISO_DATE_TIME			= "ISO8601";												//$NON-NLS-1$
	private static final String							CSV_HEADER_MOVING_TIME				= "MovingTime (%s)";										//$NON-NLS-1$
	private static final String							CSV_HEADER_NUMBER_OF_MARKER			= "NumberOfMarkers";										//$NON-NLS-1$
	private static final String							CSV_HEADER_NUMBER_OF_PHOTOS			= "NumberOfPhotos";										//$NON-NLS-1$
	private static final String							CSV_HEADER_NUMBER_OF_TOURS			= "NumberOfTours";											//$NON-NLS-1$
	private static final String							CSV_HEADER_WEATHER					= "Weather";												//$NON-NLS-1$
	private static final String							CSV_HEADER_WIND_DIRECTION			= "WindDirection";											//$NON-NLS-1$
	private static final String							CSV_HEADER_WIND_SPEED				= "WindSpeed";												//$NON-NLS-1$
	private static final String							CSV_HEADER_MAX_ALTITUDE				= "MaxAltitude (%s)";										//$NON-NLS-1$
	private static final String							CSV_HEADER_MAX_PULSE				= "MaxPulse";												//$NON-NLS-1$
	private static final String							CSV_HEADER_MAX_SPEED				= "MaxSpeed (%s)";											//$NON-NLS-1$
	private static final String							CSV_HEADER_MONTH					= "Month";													//$NON-NLS-1$
	private static final String							CSV_HEADER_PAUSED_TIME				= "PausedTime (%s)";										//$NON-NLS-1$
	private static final String							CSV_HEADER_PAUSED_TIME_RELATIVE		= "RelativePausedTime (%)";								//$NON-NLS-1$
	private static final String							CSV_HEADER_PERSON					= "Person";												//$NON-NLS-1$
	private static final String							CSV_HEADER_RECORDING_TIME			= "RecordingTime (%s)";									//$NON-NLS-1$
	private static final String							CSV_HEADER_RESTPULSE				= "RestPulse";												//$NON-NLS-1$
	private static final String							CSV_HEADER_TAGS						= "Tags";													//$NON-NLS-1$
	private static final String							CSV_HEADER_TIME						= "Time";													//$NON-NLS-1$
	private static final String							CSV_HEADER_TIME_INTERVAL			= "TimeInterval";											//$NON-NLS-1$
	private static final String							CSV_HEADER_TIME_SLICES				= "TimeSlices";											//$NON-NLS-1$
	private static final String							CSV_HEADER_TITLE					= "Title";													//$NON-NLS-1$
	private static final String							CSV_HEADER_TOUR_TYPE_ID				= "TourTypeId";											//$NON-NLS-1$
	private static final String							CSV_HEADER_TOUR_TYPE_NAME			= "TourTypeName";											//$NON-NLS-1$
	private static final String							CSV_HEADER_WEEK						= "Week";													//$NON-NLS-1$
	private static final String							CSV_HEADER_WEEKDAY					= "Weekday";												//$NON-NLS-1$
	private static final String							CSV_HEADER_WEEK_YEAR				= "WeekYear";												//$NON-NLS-1$
	private static final String							CSV_HEADER_YEAR						= "Year";													//$NON-NLS-1$

	private static final String							CSV_EXPORT_DEFAULT_FILE_NAME		= "TourBook_";												//$NON-NLS-1$
	private static final String							CSV_EXPORT_DURATION_HHH_MM_SS		= "hhh:mm:ss";												//$NON-NLS-1$

	private static int									_yearSubCategory					= TVITourBookItem.ITEM_TYPE_MONTH;

	private final IPreferenceStore						_prefStore							= TourbookPlugin
																									.getPrefStore();
	private final IDialogSettings						_state								= TourbookPlugin
																									.getState(ID);

	private ColumnManager								_columnManager;

	private PostSelectionProvider						_postSelectionProvider;

	private ISelectionListener							_postSelectionListener;
	private IPartListener2								_partListener;
	private ITourEventListener							_tourPropertyListener;
	private IPropertyChangeListener						_prefChangeListener;
	private TVITourBookRoot								_rootItem;

	private final DateTimeFormatter						_dtFormatter;
	private final DateTimeFormatter						_isoFormatter;
	private final NumberFormat							_nf1;
	private final NumberFormat							_nf1_NoGroup;

	{
		_nf1 = NumberFormat.getNumberInstance();
		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);

		_nf1_NoGroup = NumberFormat.getNumberInstance();
		_nf1_NoGroup.setMinimumFractionDigits(1);
		_nf1_NoGroup.setMaximumFractionDigits(1);
		_nf1_NoGroup.setGroupingUsed(false);

		_dtFormatter = DateTimeFormat.forPattern("yyyy-MM-dd_HH-mm-ss"); //$NON-NLS-1$
		_isoFormatter = ISODateTimeFormat.basicDateTimeNoMillis();
	}

	private final Calendar								_calendar							= GregorianCalendar
																									.getInstance();

	private final DateFormat							_timeFormatter						= DateFormat
																									.getTimeInstance(DateFormat.SHORT);
	private static final String[]						_weekDays							= DateFormatSymbols
																									.getInstance()
																									.getShortWeekdays();

	private int											_selectedYear						= -1;

	private int											_selectedYearSub					= -1;
	private final ArrayList<Long>						_selectedTourIds					= new ArrayList<Long>();

	private boolean										_isInStartup;
	private boolean										_isInReload;
	private boolean										_isRecTimeFormat_hhmmss;
	private boolean										_isDriveTimeFormat_hhmmss;

	private boolean										_isToolTipInDate;
	private boolean										_isToolTipInTags;
	private boolean										_isToolTipInTime;
	private boolean										_isToolTipInTitle;
	private boolean										_isToolTipInWeekDay;

	private final TourDoubleClickState					_tourDoubleClickState				= new TourDoubleClickState();
	private TagMenuManager								_tagMenuMgr;
	private TreeViewerTourInfoToolTip					_tourInfoToolTip;

	private ActionCollapseAll							_actionCollapseAll;
	private ActionCollapseOthers						_actionCollapseOthers;
	private ActionComputeDistanceValuesFromGeoposition	_actionComputeDistanceValuesFromGeoposition;
	private ActionComputeElevationGain					_actionComputeElevationGain;
	private ActionEditQuick								_actionEditQuick;
	private ActionExpandSelection						_actionExpandSelection;
	private ActionExport								_actionExportTour;
	private ActionExportViewCSV							_actionExportViewCSV;
	private ActionDeleteTourMenu						_actionDeleteTour;
	private ActionEditTour								_actionEditTour;
	private ActionOpenTour								_actionOpenTour;
	private ActionOpenMarkerDialog						_actionOpenMarkerDialog;
	private ActionOpenAdjustAltitudeDialog				_actionOpenAdjustAltitudeDialog;
	private ActionJoinTours								_actionJoinTours;
	private ActionMergeTour								_actionMergeTour;
	private ActionModifyColumns							_actionModifyColumns;
	private ActionPrint									_actionPrintTour;
	private ActionRefreshView							_actionRefreshView;
	private ActionReimportSubMenu						_actionReimportSubMenu;
	private ActionSelectAllTours						_actionSelectAllTours;
	private ActionSetAltitudeValuesFromSRTM				_actionSetAltitudeFromSRTM;
	private ActionSetTourTypeMenu						_actionSetTourType;
	private ActionSetPerson								_actionSetOtherPerson;
	private ActionToggleMonthWeek						_actionToggleMonthWeek;

	/*
	 * UI controls
	 */
	private Composite									_viewerContainer;

	private TreeViewer									_tourViewer;

	private PixelConverter								_pc;

	private static class ItemComparer implements IElementComparer {

		@Override
		public boolean equals(final Object a, final Object b) {

			if (a == b) {
				return true;
			}

			if (a instanceof TVITourBookYear && b instanceof TVITourBookYear) {

				final TVITourBookYear item1 = (TVITourBookYear) a;
				final TVITourBookYear item2 = (TVITourBookYear) b;
				return item1.tourYear == item2.tourYear;
			}

			if (a instanceof TVITourBookYearSub && b instanceof TVITourBookYearSub) {

				final TVITourBookYearSub item1 = (TVITourBookYearSub) a;
				final TVITourBookYearSub item2 = (TVITourBookYearSub) b;
				return item1.tourYear == item2.tourYear && item1.tourYearSub == item2.tourYearSub;
			}

			if (a instanceof TVITourBookTour && b instanceof TVITourBookTour) {

				final TVITourBookTour item1 = (TVITourBookTour) a;
				final TVITourBookTour item2 = (TVITourBookTour) b;
				return item1.tourId == item2.tourId;
			}

			return false;
		}

		@Override
		public int hashCode(final Object element) {
			return 0;
		}
	}

	private class TourBookContentProvider implements ITreeContentProvider {

		@Override
		public void dispose() {}

		@Override
		public Object[] getChildren(final Object parentElement) {
			return ((TreeViewerItem) parentElement).getFetchedChildrenAsArray();
		}

		@Override
		public Object[] getElements(final Object inputElement) {
			return _rootItem.getFetchedChildrenAsArray();
		}

		@Override
		public Object getParent(final Object element) {
			return ((TreeViewerItem) element).getParentItem();
		}

		@Override
		public boolean hasChildren(final Object element) {
			return ((TreeViewerItem) element).hasChildren();
		}

		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	void actionExportViewCSV() {

		// get selected items
		final ITreeSelection selection = (ITreeSelection) _tourViewer.getSelection();

		if (selection.size() == 0) {
			return;
		}

		final String defaultExportFilePath = _state.get(STATE_CSV_EXPORT_PATH);

		final String defaultExportFileName = CSV_EXPORT_DEFAULT_FILE_NAME
				+ _dtFormatter.print(new DateTime())
				+ UI.SYMBOL_DOT
				+ Util.CSV_FILE_EXTENSION;

		/*
		 * get export filename
		 */
		final FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.SAVE);
		dialog.setText(Messages.dialog_export_file_dialog_text);

		dialog.setFilterPath(defaultExportFilePath);
		dialog.setFilterExtensions(new String[] { Util.CSV_FILE_EXTENSION });
		dialog.setFileName(defaultExportFileName);

		final String selectedFilePath = dialog.open();
		if (selectedFilePath == null) {
			return;
		}

		final File exportFilePath = new Path(selectedFilePath).toFile();

		// keep export path
		_state.put(STATE_CSV_EXPORT_PATH, exportFilePath.getPath());

		if (exportFilePath.exists()) {
			if (net.tourbook.ui.UI.confirmOverwrite(exportFilePath) == false) {
				// don't overwrite file, nothing more to do
				return;
			}
		}

		exportCSV(selection, selectedFilePath);

//		// DEBUGGING: USING DEFAULT PATH
//		final IPath path = new Path(defaultExportFilePath).removeLastSegments(1).append(defaultExportFileName);
//
//		exportCSV(selection, path.toOSString());
	}

	void actionSelectYearMonthTours() {

		if (_actionSelectAllTours.isChecked()) {
			// reselect selection
			_tourViewer.setSelection(_tourViewer.getSelection());
		}
	}

	void actionToggleMonthWeek() {

		if (_yearSubCategory == TVITourBookItem.ITEM_TYPE_WEEK) {

			// toggle to month

			_yearSubCategory = TVITourBookItem.ITEM_TYPE_MONTH;

			_actionToggleMonthWeek.setImageDescriptor(//
					TourbookPlugin.getImageDescriptor(Messages.Image__TourBook_Week));

		} else {

			// toggle to week

			_yearSubCategory = TVITourBookItem.ITEM_TYPE_WEEK;

			_actionToggleMonthWeek.setImageDescriptor(//
					TourbookPlugin.getImageDescriptor(Messages.Image__TourBook_Month));
		}

		reopenFirstSelectedTour();
	}

	private void addPartListener() {

		_partListener = new IPartListener2() {
			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourBookView.this) {
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

				if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {

//					fActivePerson = TourbookPlugin.getDefault().getActivePerson();
//					fActiveTourTypeFilter = TourbookPlugin.getDefault().getActiveTourTypeFilter();

					reloadViewer();

				} else if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

					// update tourbook viewer
					_tourViewer.refresh();

					// redraw must be done to see modified tour type image colors
					_tourViewer.getTree().redraw();

				} else if (property.equals(ITourbookPreferences.VIEW_TOOLTIP_IS_MODIFIED)) {

					updateToolTipState();

				} else if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

//					UI.updateUnits();

					_columnManager.saveState(_state);
					_columnManager.clearColumns();
					defineAllColumns(_viewerContainer);

					_tourViewer = (TreeViewer) recreateViewer(_tourViewer);

				} else if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

					readDisplayFormats();

					_tourViewer.getTree().setLinesVisible(
							_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

					_tourViewer.refresh();

					/*
					 * the tree must be redrawn because the styled text does not show with the new
					 * color
					 */
					_tourViewer.getTree().redraw();
				}
			}
		};

		// register the listener
		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	private void addSelectionListener() {
		// this view part is a selection listener
		_postSelectionListener = new ISelectionListener() {

			@Override
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				if (selection instanceof SelectionDeletedTours) {
					reloadViewer();
				}
			}
		};

		// register selection listener in the page
		getSite().getPage().addPostSelectionListener(_postSelectionListener);
	}

	private void addTourEventListener() {

		_tourPropertyListener = new ITourEventListener() {
			@Override
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (eventId == TourEventId.TOUR_CHANGED || eventId == TourEventId.UPDATE_UI) {

					/*
					 * it is possible when a tour type was modified, the tour can be hidden or
					 * visible in the viewer because of the tour type filter
					 */
					reloadViewer();

				} else if (eventId == TourEventId.TAG_STRUCTURE_CHANGED
						|| eventId == TourEventId.ALL_TOURS_ARE_MODIFIED) {

					reloadViewer();
				}
			}
		};
		TourManager.getInstance().addTourEventListener(_tourPropertyListener);
	}

	private void createActions() {

		_actionCollapseAll = new ActionCollapseAll(this);
		_actionCollapseOthers = new ActionCollapseOthers(this);
		_actionComputeDistanceValuesFromGeoposition = new ActionComputeDistanceValuesFromGeoposition(this);
		_actionComputeElevationGain = new ActionComputeElevationGain(this);
		_actionDeleteTour = new ActionDeleteTourMenu(this);
		_actionEditQuick = new ActionEditQuick(this);
		_actionEditTour = new ActionEditTour(this);
		_actionExpandSelection = new ActionExpandSelection(this);
		_actionExportTour = new ActionExport(this);
		_actionExportViewCSV = new ActionExportViewCSV(this);
		_actionJoinTours = new ActionJoinTours(this);
		_actionOpenMarkerDialog = new ActionOpenMarkerDialog(this, true);
		_actionOpenAdjustAltitudeDialog = new ActionOpenAdjustAltitudeDialog(this);
		_actionMergeTour = new ActionMergeTour(this);
		_actionModifyColumns = new ActionModifyColumns(this);
		_actionOpenTour = new ActionOpenTour(this);
		_actionPrintTour = new ActionPrint(this);
		_actionRefreshView = new ActionRefreshView(this);
		_actionReimportSubMenu = new ActionReimportSubMenu(this);
		_actionSetAltitudeFromSRTM = new ActionSetAltitudeValuesFromSRTM(this);
		_actionSetOtherPerson = new ActionSetPerson(this);
		_actionSetTourType = new ActionSetTourTypeMenu(this);
		_actionSelectAllTours = new ActionSelectAllTours(this);
		_actionToggleMonthWeek = new ActionToggleMonthWeek(this);

		_tagMenuMgr = new TagMenuManager(this, true);

		fillActionBars();
	}

	@Override
	public void createPartControl(final Composite parent) {

		_pc = new PixelConverter(parent);

		// define all columns for the viewer
		_columnManager = new ColumnManager(this, _state);
		defineAllColumns(parent);

		createUI(parent);
		createActions();

		addSelectionListener();
		addPartListener();
		addPrefListener();
		addTourEventListener();

		// set selection provider
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

		readDisplayFormats();
		restoreState();

		enableActions();

		// update the viewer
		_rootItem = new TVITourBookRoot(this);

		// delay loading, that the app filters are initialized
		Display.getCurrent().asyncExec(new Runnable() {
			@Override
			public void run() {

				_tourViewer.setInput(this);

				_isInStartup = true;

				reselectTourViewer();
			}
		});
	}

	private void createUI(final Composite parent) {

		_viewerContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
		{
			createUI_10_TourViewer(_viewerContainer);
		}
	}

	private void createUI_10_TourViewer(final Composite parent) {

		// tour tree
		final Tree tree = new Tree(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FLAT | SWT.FULL_SELECTION | SWT.MULTI);

		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		tree.setHeaderVisible(true);
		tree.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

		_tourViewer = new TreeViewer(tree);
		_columnManager.createColumns(_tourViewer);

		_tourViewer.setContentProvider(new TourBookContentProvider());
		_tourViewer.setComparer(new ItemComparer());
		_tourViewer.setUseHashlookup(true);

		_tourViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				onSelectTreeItem(event);
			}
		});

		_tourViewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(final DoubleClickEvent event) {

				final Object selection = ((IStructuredSelection) _tourViewer.getSelection()).getFirstElement();

				if (selection instanceof TVITourBookTour) {

					TourManager.getInstance().tourDoubleClickAction(TourBookView.this, _tourDoubleClickState);

				} else if (selection != null) {

					// expand/collapse current item

					final TreeViewerItem tourItem = (TreeViewerItem) selection;

					if (_tourViewer.getExpandedState(tourItem)) {
						_tourViewer.collapseToLevel(tourItem, 1);
					} else {
						_tourViewer.expandToLevel(tourItem, 1);
					}
				}
			}
		});

		/*
		 * the context menu must be created after the viewer is created which is also done after the
		 * measurement system has changed
		 */
		createUI_20_ContextMenu();

		// set tour info tooltip provider
		_tourInfoToolTip = new TreeViewerTourInfoToolTip(_tourViewer);
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

		final Tree tree = (Tree) _tourViewer.getControl();
		final Menu treeContextMenu = menuMgr.createContextMenu(tree);
		treeContextMenu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuHidden(final MenuEvent e) {
				_tagMenuMgr.onHideMenu();
			}

			@Override
			public void menuShown(final MenuEvent menuEvent) {
				_tagMenuMgr.onShowMenu(menuEvent, tree, Display.getCurrent().getCursorLocation(), _tourInfoToolTip);
			}
		});

		_columnManager.createHeaderContextMenu(tree, treeContextMenu);
	}

	private void csvField(final StringBuilder sb, final int fieldValue) {

		sb.append(fieldValue);
		sb.append(UI.TAB);
	}

	private void csvField(final StringBuilder sb, final String fieldValue) {

		sb.append(fieldValue);
		sb.append(UI.TAB);
	}

	/**
	 * Defines all columns for the table viewer in the column manager, the sequence defines the
	 * default columns
	 * 
	 * @param parent
	 */
	private void defineAllColumns(final Composite parent) {

		defineColumn_1stColumn_Date();
		defineColumn_WeekDay();
		defineColumn_Time();
		defineColumn_TourTypeImage();
		defineColumn_TourTypeText();

		defineColumn_Distance();
		defineColumn_AltitudeUp();
		defineColumn_TimeDriving();

		defineColumn_WeatherClouds();
		defineColumn_Photos();
		defineColumn_Title();
		defineColumn_Tags();

		defineColumn_Marker();
		defineColumn_Calories();
		defineColumn_RestPulse();

		defineColumn_TimeRecording();
		defineColumn_TimeBreak();
		defineColumn_TimeBreakRelative();

		defineColumn_AltitudeDown();

		defineColumn_MaxAltitude();
		defineColumn_MaxSpeed();
		defineColumn_MaxPulse();

		defineColumn_AvgSpeed();
		defineColumn_AvgPace();
		defineColumn_AvgPulse();
		defineColumn_AvgCadence();
		defineColumn_AvgTemperature();

		defineColumn_WeatherWindSpeed();
		defineColumn_WeatherWindDirection();

		defineColumn_Gear_FrontShiftCount();
		defineColumn_Gear_RearShiftCount();

		defineColumn_WeekNo();
		defineColumn_WeekYear();

		defineColumn_TimeSlices();
		defineColumn_TimeInterval();
		defineColumn_DeviceDistance();
		defineColumn_DPTolerance();

		defineColumn_Person();
	}

	/**
	 * tree column: date
	 */
	private void defineColumn_1stColumn_Date() {

		final TreeColumnDefinition colDef = TreeColumnFactory.DATE.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new TourInfoToolTipStyledCellLabelProvider() {

			@Override
			public Long getTourId(final ViewerCell cell) {

				if (_isToolTipInDate == false) {
					return null;
				}

				final Object element = cell.getElement();
				if ((element instanceof TVITourBookTour)) {
					return ((TVITourBookItem) element).getTourId();
				}

				return null;
			}

			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final TVITourBookItem tourItem = (TVITourBookItem) element;

				if ((element instanceof TVITourBookTour)) {

					// tour item
					cell.setText(tourItem.treeColumn);

				} else {

					// year/month item
					final StyledString styledString = new StyledString();
					styledString.append(tourItem.treeColumn);
					styledString.append(UI.SPACE3);
					styledString.append(Long.toString(tourItem.colCounter), StyledString.QUALIFIER_STYLER);

					if (tourItem instanceof TVITourBookYearSub) {

						cell.setForeground(//
								JFaceResources.getColorRegistry().get(net.tourbook.ui.UI.VIEW_COLOR_SUB_SUB));

					} else {

						cell.setForeground(//
								JFaceResources.getColorRegistry().get(net.tourbook.ui.UI.VIEW_COLOR_SUB));
					}

					cell.setText(styledString.getString());
					cell.setStyleRanges(styledString.getStyleRanges());
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: altitude down (m)
	 */
	private void defineColumn_AltitudeDown() {

		final TreeColumnDefinition colDef = TreeColumnFactory.ALTITUDE_DOWN.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final long dbAltitudeDown = ((TVITourBookItem) element).colAltitudeDown;

				if (dbAltitudeDown == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(Long.toString((long) (-dbAltitudeDown / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE)));
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: altitude up (m)
	 */
	private void defineColumn_AltitudeUp() {

		final TreeColumnDefinition colDef = TreeColumnFactory.ALTITUDE_UP.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final long dbAltitudeUp = ((TVITourBookItem) element).colAltitudeUp;

				if (dbAltitudeUp == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(Long.toString((long) (dbAltitudeUp / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE)));
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: avg cadence
	 */
	private void defineColumn_AvgCadence() {

		final TreeColumnDefinition colDef = TreeColumnFactory.AVG_CADENCE.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final float dbAvgCadence = ((TVITourBookItem) element).colAvgCadence;

				if (dbAvgCadence == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(_nf1.format(dbAvgCadence));
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: avg pace min/km - min/mi
	 */
	private void defineColumn_AvgPace() {

		final TreeColumnDefinition colDef = TreeColumnFactory.AVG_PACE.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final float pace = ((TVITourBookItem) element).colAvgPace * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

				if (pace == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(net.tourbook.ui.UI.format_mm_ss((long) pace));
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: avg pulse
	 */
	private void defineColumn_AvgPulse() {

		final TreeColumnDefinition colDef = TreeColumnFactory.AVG_PULSE.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final float dbAvgPulse = ((TVITourBookItem) element).colAvgPulse;

				if (dbAvgPulse == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(_nf1.format(dbAvgPulse));
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: avg speed km/h - mph
	 */
	private void defineColumn_AvgSpeed() {

		final TreeColumnDefinition colDef = TreeColumnFactory.AVG_SPEED.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				final float speed = ((TVITourBookItem) element).colAvgSpeed / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;
				if (speed == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(_nf1.format(speed));
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: avg temperature
	 */
	private void defineColumn_AvgTemperature() {

		final TreeColumnDefinition colDef = TreeColumnFactory.AVG_TEMPERATURE.createColumn(_columnManager, _pc);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				float temperature = ((TVITourBookItem) element).colAvgTemperature;

				if (temperature == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {

					if (net.tourbook.ui.UI.UNIT_VALUE_TEMPERATURE != 1) {
						temperature = temperature
								* net.tourbook.ui.UI.UNIT_FAHRENHEIT_MULTI
								+ net.tourbook.ui.UI.UNIT_FAHRENHEIT_ADD;
					}

					cell.setText(_nf1.format(temperature));
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: calories
	 */
	private void defineColumn_Calories() {

		final TreeColumnDefinition colDef = TreeColumnFactory.CALORIES.createColumn(_columnManager, _pc);
		//colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final long caloriesSum = ((TVITourBookItem) element).colCalories;

				if (caloriesSum == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(Long.toString(caloriesSum));
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: device distance
	 */
	private void defineColumn_DeviceDistance() {

		final TreeColumnDefinition colDef = TreeColumnFactory.DEVICE_DISTANCE.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVITourBookTour) {

					final long dbStartDistance = ((TVITourBookTour) element).getColumnStartDistance();

					if (dbStartDistance == 0) {
						cell.setText(UI.EMPTY_STRING);
					} else {
						cell.setText(Long.toString((long) (dbStartDistance / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE)));
					}

					setCellColor(cell, element);
				}
			}
		});
	}

	/**
	 * column: distance (km/miles)
	 */
	private void defineColumn_Distance() {

		final TreeColumnDefinition colDef = TreeColumnFactory.DISTANCE.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final float dbDistance = ((TVITourBookItem) element).colDistance;

				if (dbDistance == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(_nf1.format(dbDistance / 1000 / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE));
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * Column: DP tolerance
	 */
	private void defineColumn_DPTolerance() {

		final TreeColumnDefinition colDef = TreeColumnFactory.DP_TOLERANCE.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final int dpTolerance = ((TVITourBookItem) element).colDPTolerance;

				if (dpTolerance == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(_nf1.format(dpTolerance / 10.0));
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * Column: Front shift count.
	 */
	private void defineColumn_Gear_FrontShiftCount() {

		final TreeColumnDefinition colDef = TreeColumnFactory.GEAR_FRONT_SHIFT_COUNT.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final int numberOfShifts = ((TVITourBookItem) element).colFrontShiftCount;

				if (numberOfShifts == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(Integer.toString(numberOfShifts));
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * Column: Rear shift count.
	 */
	private void defineColumn_Gear_RearShiftCount() {

		final TreeColumnDefinition colDef = TreeColumnFactory.GEAR_REAR_SHIFT_COUNT.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final int numberOfShifts = ((TVITourBookItem) element).colRearShiftCount;

				if (numberOfShifts == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(Integer.toString(numberOfShifts));
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: markers
	 */
	private void defineColumn_Marker() {

		final TreeColumnDefinition colDef = TreeColumnFactory.TOUR_MARKERS.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				if (element instanceof TVITourBookTour) {

					final ArrayList<Long> markerIds = ((TVITourBookTour) element).getMarkerIds();
					if (markerIds == null) {
						cell.setText(UI.EMPTY_STRING);
					} else {
						cell.setText(Integer.toString(markerIds.size()));
					}

					setCellColor(cell, element);
				}
			}
		});
	}

	/**
	 * column: max altitude
	 */
	private void defineColumn_MaxAltitude() {

		final TreeColumnDefinition colDef = TreeColumnFactory.MAX_ALTITUDE.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final long dbMaxAltitude = ((TVITourBookItem) element).colMaxAltitude;

				if (dbMaxAltitude == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(Long.toString((long) (dbMaxAltitude / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE)));
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: max pulse
	 */
	private void defineColumn_MaxPulse() {

		final TreeColumnDefinition colDef = TreeColumnFactory.MAX_PULSE.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final long dbMaxPulse = ((TVITourBookItem) element).colMaxPulse;

				if (dbMaxPulse == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(Long.toString(dbMaxPulse));
				}
				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: max speed
	 */
	private void defineColumn_MaxSpeed() {

		final TreeColumnDefinition colDef = TreeColumnFactory.MAX_SPEED.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final float dbMaxSpeed = ((TVITourBookItem) element).colMaxSpeed;

				if (dbMaxSpeed == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(_nf1.format(dbMaxSpeed / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE));
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: person
	 */
	private void defineColumn_Person() {

		final TreeColumnDefinition colDef = TreeColumnFactory.PERSON.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVITourBookTour) {

					final long dbPersonId = ((TVITourBookTour) element).colPersonId;

					cell.setText(PersonManager.getPersonName(dbPersonId));

//					setCellColor(cell, element);
				}
			}
		});
	}

	/**
	 * column: number of photos
	 */
	private void defineColumn_Photos() {

		final TreeColumnDefinition colDef = TreeColumnFactory.TOUR_PHOTOS.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final int numberOfPhotos = ((TVITourBookItem) element).colNumberOfPhotos;

				if (numberOfPhotos == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(Integer.toString(numberOfPhotos));
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: rest pulse
	 */
	private void defineColumn_RestPulse() {

		final TreeColumnDefinition colDef = TreeColumnFactory.RESTPULSE.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {

			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final int restPulse = ((TVITourBookItem) element).colRestPulse;

				if (restPulse == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(Integer.toString(restPulse));
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: tags
	 */
	private void defineColumn_Tags() {

		final TreeColumnDefinition colDef = TreeColumnFactory.TOUR_TAGS.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

			@Override
			public Long getTourId(final ViewerCell cell) {

				if (_isToolTipInTags == false) {
					return null;
				}

				final Object element = cell.getElement();
				if ((element instanceof TVITourBookTour)) {
					return ((TVITourBookTour) element).getTourId();
				}

				return null;
			}

			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVITourBookTour) {

					cell.setText(TourDatabase.getTagNames(((TVITourBookTour) element).getTagIds()));
					setCellColor(cell, element);
				}
			}
		});
	}

	/**
	 * column: time
	 */
	private void defineColumn_Time() {

		final TreeColumnDefinition colDef = TreeColumnFactory.TOUR_START_TIME //
				.createColumn(_columnManager, _pc);

		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

			@Override
			public Long getTourId(final ViewerCell cell) {

				if (_isToolTipInTime == false) {
					return null;
				}

				final Object element = cell.getElement();
				if ((element instanceof TVITourBookTour)) {
					return ((TVITourBookTour) element).getTourId();
				}

				return null;
			}

			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVITourBookTour) {

					final long tourDate = ((TVITourBookTour) element).colTourDate;
					_calendar.setTimeInMillis(tourDate);

					cell.setText(_timeFormatter.format(_calendar.getTime()));
					setCellColor(cell, element);
				}
			}
		});
	}

	/**
	 * column: paused time (h)
	 */
	private void defineColumn_TimeBreak() {

		final TreeColumnDefinition colDef = TreeColumnFactory.PAUSED_TIME.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				/*
				 * display paused time relative to the recording time
				 */

				final Object element = cell.getElement();
				final TVITourBookItem item = (TVITourBookItem) element;

				final long dbPausedTime = item.colPausedTime;

				if (dbPausedTime == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					if (_isDriveTimeFormat_hhmmss) {
						cell.setText(net.tourbook.ui.UI.format_hh_mm_ss(dbPausedTime).toString());
					} else {
						cell.setText(net.tourbook.ui.UI.format_hh_mm(dbPausedTime + 30).toString());
					}
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: relative paused time %
	 */
	private void defineColumn_TimeBreakRelative() {

		final TreeColumnDefinition colDef = TreeColumnFactory.PAUSED_TIME_RELATIVE.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				/*
				 * display paused time relative to the recording time
				 */

				final Object element = cell.getElement();
				final TVITourBookItem item = (TVITourBookItem) element;

				final long dbPausedTime = item.colPausedTime;
				final long dbRecordingTime = item.colRecordingTime;

				final float relativePausedTime = dbRecordingTime == 0 ? 0 : (float) dbPausedTime
						/ dbRecordingTime
						* 100;

				cell.setText(_nf1.format(relativePausedTime));

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: driving time (h)
	 */
	private void defineColumn_TimeDriving() {

		final TreeColumnDefinition colDef = TreeColumnFactory.DRIVING_TIME.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final long drivingTime = ((TVITourBookItem) element).colDrivingTime;

				if (element instanceof TVITourBookTour) {
					if (_isDriveTimeFormat_hhmmss) {
						cell.setText(net.tourbook.ui.UI.format_hh_mm_ss(drivingTime).toString());
					} else {
						cell.setText(net.tourbook.ui.UI.format_hh_mm(drivingTime + 30).toString());
					}
				} else {
					cell.setText(net.tourbook.ui.UI.format_hh_mm(drivingTime + 30).toString());
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: timeinterval
	 */

	private void defineColumn_TimeInterval() {

		final TreeColumnDefinition colDef = TreeColumnFactory.TIME_INTERVAL.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVITourBookTour) {

					final short dbTimeInterval = ((TVITourBookTour) element).getColumnTimeInterval();
					if (dbTimeInterval == 0) {
						cell.setText(UI.EMPTY_STRING);
					} else {
						cell.setText(Long.toString(dbTimeInterval));
					}

					setCellColor(cell, element);
				}
			}
		});
	}

	/**
	 * column: recording time (h)
	 */
	private void defineColumn_TimeRecording() {

		final TreeColumnDefinition colDef = TreeColumnFactory.RECORDING_TIME.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final long recordingTime = ((TVITourBookItem) element).colRecordingTime;

				if (element instanceof TVITourBookTour) {
					if (_isRecTimeFormat_hhmmss) {
						cell.setText(net.tourbook.ui.UI.format_hh_mm_ss(recordingTime).toString());
					} else {
						cell.setText(net.tourbook.ui.UI.format_hh_mm(recordingTime + 30).toString());
					}
				} else {
					cell.setText(net.tourbook.ui.UI.format_hh_mm(recordingTime + 30).toString());
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: number of time slices
	 */
	private void defineColumn_TimeSlices() {

		final TreeColumnDefinition colDef = TreeColumnFactory.TIME_SLICES.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final int numberOfTimeSlices = ((TVITourBookItem) element).colNumberOfTimeSlices;

				if (numberOfTimeSlices == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(Integer.toString(numberOfTimeSlices));
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: title
	 */
	private void defineColumn_Title() {

		final TreeColumnDefinition colDef = TreeColumnFactory.TITLE.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

			@Override
			public Long getTourId(final ViewerCell cell) {

				if (_isToolTipInTitle == false) {
					return null;
				}

				final Object element = cell.getElement();
				if ((element instanceof TVITourBookTour)) {
					return ((TVITourBookTour) element).getTourId();
				}

				return null;
			}

			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVITourBookTour) {

					cell.setText(((TVITourBookTour) element).colTourTitle);
					setCellColor(cell, element);
				}
			}
		});
	}

	/**
	 * column: tour type image
	 */
	private void defineColumn_TourTypeImage() {

		final TreeColumnDefinition colDef = TreeColumnFactory.TOUR_TYPE.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVITourBookTour) {

					final long tourTypeId = ((TVITourBookTour) element).getTourTypeId();
					final Image tourTypeImage = net.tourbook.ui.UI.getInstance().getTourTypeImage(tourTypeId);

					/*
					 * when a tour type image is modified, it will keep the same image resource only
					 * the content is modified but in the rawDataView the modified image is not
					 * displayed compared with the tourBookView which displays the correct image
					 */
					cell.setImage(tourTypeImage);
				}
			}
		});
	}

	/**
	 * column: tour type text
	 */
	private void defineColumn_TourTypeText() {

		final TreeColumnDefinition colDef = TreeColumnFactory.TOUR_TYPE_TEXT.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVITourBookTour) {

					final long tourTypeId = ((TVITourBookTour) element).getTourTypeId();
					cell.setText(net.tourbook.ui.UI.getInstance().getTourTypeLabel(tourTypeId));
				}
			}
		});
	}

	/**
	 * column: clouds
	 */
	private void defineColumn_WeatherClouds() {

		final TreeColumnDefinition colDef = TreeColumnFactory.CLOUDS.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {

			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final String windClouds = ((TVITourBookItem) element).colClouds;

				if (windClouds == null) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					//cell.setText(windClouds);
					final Image img = net.tourbook.common.UI.IMAGE_REGISTRY.get(windClouds);
					if (img != null) {
						cell.setImage(img);
					} else {
						cell.setText(windClouds);
					}
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: wind direction
	 */
	private void defineColumn_WeatherWindDirection() {

		final TreeColumnDefinition colDef = TreeColumnFactory.WIND_DIR.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {

			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final int windDir = ((TVITourBookItem) element).colWindDir;

				if (windDir == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(Integer.toString(windDir));
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: weather
	 */
	private void defineColumn_WeatherWindSpeed() {

		final TreeColumnDefinition colDef = TreeColumnFactory.WIND_SPEED.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {

			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final int windSpeed = (int) (((TVITourBookItem) element).colWindSpd / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE);

				if (windSpeed == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(Integer.toString(windSpeed));
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: week day
	 */
	private void defineColumn_WeekDay() {

		final TreeColumnDefinition colDef = TreeColumnFactory.WEEK_DAY.createColumn(_columnManager, _pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

			@Override
			public Long getTourId(final ViewerCell cell) {

				if (_isToolTipInWeekDay == false) {
					return null;
				}

				final Object element = cell.getElement();
				if ((element instanceof TVITourBookTour)) {
					return ((TVITourBookTour) element).getTourId();
				}

				return null;
			}

			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVITourBookTour) {

					final int weekDay = ((TVITourBookTour) element).colWeekDay;

					cell.setText(_weekDays[weekDay]);
					setCellColor(cell, element);
				}
			}
		});
	}

	/**
	 * column: week
	 */
	private void defineColumn_WeekNo() {

		final TreeColumnDefinition colDef = TreeColumnFactory.WEEK_NO.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {

			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final int week = ((TVITourBookItem) element).colWeekNo;

				if (week == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(Integer.toString(week));
				}

				setCellColor(cell, element);
			}
		});
	}

	/**
	 * column: week year
	 */
	private void defineColumn_WeekYear() {

		final TreeColumnDefinition colDef = TreeColumnFactory.WEEKYEAR.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {

			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final int week = ((TVITourBookItem) element).colWeekYear;

				if (week == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(Integer.toString(week));
				}

				setCellColor(cell, element);
			}
		});
	}

	@Override
	public void dispose() {

		getSite().getPage().removePostSelectionListener(_postSelectionListener);
		getViewSite().getPage().removePartListener(_partListener);
		TourManager.getInstance().removeTourEventListener(_tourPropertyListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	private void enableActions() {

		final ITreeSelection selection = (ITreeSelection) _tourViewer.getSelection();

		/*
		 * count number of selected items
		 */
		int tourItems = 0;

		TVITourBookTour firstTour = null;

		for (final Iterator<?> iter = selection.iterator(); iter.hasNext();) {

			final Object treeItem = iter.next();
			if (treeItem instanceof TVITourBookTour) {
				if (tourItems == 0) {
					firstTour = (TVITourBookTour) treeItem;
				}
				tourItems++;
			}
		}

		final int selectedItems = selection.size();
		final boolean isTourSelected = tourItems > 0;
		final boolean isOneTour = tourItems == 1;
		boolean isDeviceTour = false;

		final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();

		final TVITourBookItem firstElement = (TVITourBookItem) selection.getFirstElement();
		final boolean firstElementHasChildren = firstElement == null ? false : firstElement.hasChildren();
		TourData firstSavedTour = null;

		if (isOneTour) {
			firstSavedTour = TourManager.getInstance().getTourData(firstTour.getTourId());
			isDeviceTour = firstSavedTour.isManualTour() == false;
		}

		/*
		 * enable actions
		 */
		_tourDoubleClickState.canEditTour = isOneTour;
		_tourDoubleClickState.canOpenTour = isOneTour;
		_tourDoubleClickState.canQuickEditTour = isOneTour;
		_tourDoubleClickState.canEditMarker = isOneTour;
		_tourDoubleClickState.canAdjustAltitude = isOneTour;

		_actionComputeDistanceValuesFromGeoposition.setEnabled(isTourSelected);
		_actionComputeElevationGain.setEnabled(true);
		_actionDeleteTour.setEnabled(isTourSelected);
		_actionEditQuick.setEnabled(isOneTour);
		_actionEditTour.setEnabled(isOneTour);
		_actionExportTour.setEnabled(isTourSelected);
		_actionExportViewCSV.setEnabled(selectedItems > 0);
		_actionJoinTours.setEnabled(tourItems > 1);
		_actionMergeTour.setEnabled(isOneTour && isDeviceTour && firstSavedTour.getMergeSourceTourId() != null);
		_actionOpenAdjustAltitudeDialog.setEnabled(isOneTour && isDeviceTour);
		_actionOpenMarkerDialog.setEnabled(isOneTour && isDeviceTour);
		_actionOpenTour.setEnabled(isOneTour);
		_actionPrintTour.setEnabled(isTourSelected);
		_actionReimportSubMenu.setEnabled(isTourSelected);
		_actionSetAltitudeFromSRTM.setEnabled(isTourSelected);
		_actionSetOtherPerson.setEnabled(isTourSelected);
		_actionSetTourType.setEnabled(isTourSelected && tourTypes.size() > 0);

		_actionCollapseOthers.setEnabled(selectedItems == 1 && firstElementHasChildren);
		_actionExpandSelection.setEnabled(firstElement == null //
				? false
				: selectedItems == 1 //
						? firstElementHasChildren
						: true);

		_tagMenuMgr.enableTagActions(isTourSelected, isOneTour, firstTour == null ? null : firstTour.getTagIds());

		TourTypeMenuManager.enableRecentTourTypeActions(isTourSelected, isOneTour
				? firstTour.getTourTypeId()
				: TourDatabase.ENTITY_IS_NOT_SAVED);
	}

	private void exportCSV(final ITreeSelection selection, final String selectedFilePath) {

		/*
		 * Write selected items into a csv file.
		 */
		Writer exportWriter = null;
		try {

			exportWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(selectedFilePath), UI.UTF_8));
			final StringBuilder sb = new StringBuilder();

			exportCSV_10_Header(exportWriter, sb);

			for (final TreePath treePath : selection.getPaths()) {

				// truncate buffer
				sb.setLength(0);

				final int segmentCount = treePath.getSegmentCount();

				for (int segmentIndex = 0; segmentIndex < segmentCount; segmentIndex++) {

					final Object segment = treePath.getSegment(segmentIndex);
					final boolean isTour = segment instanceof TVITourBookTour;

					exportCSV_20_1stColumn(sb, segmentCount, segment, isTour);

					if (segment instanceof TVITourBookItem) {

						final TVITourBookItem tviItem = (TVITourBookItem) segment;

						// output data only for the last segment
						if (segmentCount == 1
								|| (segmentCount == 2 && segmentIndex == 1)
								|| (segmentCount == 3 && segmentIndex == 2)) {

							exportCSV_30_OtherColumns(sb, isTour, tviItem);
						}
					}
				}

				// end of line
				sb.append(net.tourbook.ui.UI.SYSTEM_NEW_LINE);
				exportWriter.write(sb.toString());
			}

		} catch (final IOException e) {
			StatusUtil.showStatus(e);
		} finally {
			Util.closeWriter(exportWriter);
		}
	}

	private void exportCSV_10_Header(final Writer exportWriter, final StringBuilder sb) throws IOException {

		// Year
		csvField(sb, CSV_HEADER_YEAR);

		// Month or Week
		if (isYearSubWeek()) {
			csvField(sb, CSV_HEADER_WEEK);
			csvField(sb, CSV_HEADER_MONTH);
		} else {
			// defaults to month
			csvField(sb, CSV_HEADER_MONTH);
			csvField(sb, CSV_HEADER_WEEK);
		}

		csvField(sb, CSV_HEADER_DAY);
		csvField(sb, CSV_HEADER_WEEKDAY);
		csvField(sb, CSV_HEADER_TIME);
		csvField(sb, CSV_HEADER_ISO_DATE_TIME);
		csvField(sb, CSV_HEADER_NUMBER_OF_TOURS);
		csvField(sb, CSV_HEADER_TOUR_TYPE_ID);
		csvField(sb, CSV_HEADER_TOUR_TYPE_NAME);
		csvField(sb, String.format(CSV_HEADER_DISTANCE, UI.UNIT_LABEL_DISTANCE));
		csvField(sb, String.format(CSV_HEADER_ALTITUDE_UP, UI.UNIT_LABEL_ALTITUDE));
		csvField(sb, String.format(CSV_HEADER_ALTITUDE_DOWN, UI.UNIT_LABEL_ALTITUDE));
		csvField(sb, String.format(CSV_HEADER_RECORDING_TIME, Messages.App_Unit_Seconds_Small));
		csvField(sb, String.format(CSV_HEADER_MOVING_TIME, Messages.App_Unit_Seconds_Small));
		csvField(sb, String.format(CSV_HEADER_PAUSED_TIME, Messages.App_Unit_Seconds_Small));
		csvField(sb, CSV_HEADER_PAUSED_TIME_RELATIVE);
		csvField(sb, String.format(CSV_HEADER_RECORDING_TIME, CSV_EXPORT_DURATION_HHH_MM_SS));
		csvField(sb, String.format(CSV_HEADER_MOVING_TIME, CSV_EXPORT_DURATION_HHH_MM_SS));
		csvField(sb, String.format(CSV_HEADER_PAUSED_TIME, CSV_EXPORT_DURATION_HHH_MM_SS));
		csvField(sb, CSV_HEADER_NUMBER_OF_MARKER);
		csvField(sb, CSV_HEADER_NUMBER_OF_PHOTOS);
		csvField(sb, CSV_HEADER_WEATHER);
		csvField(sb, CSV_HEADER_WIND_SPEED);
		csvField(sb, CSV_HEADER_WIND_DIRECTION);
		csvField(sb, CSV_HEADER_TITLE);
		csvField(sb, CSV_HEADER_TAGS);
		csvField(sb, CSV_HEADER_CALORIES);
		csvField(sb, CSV_HEADER_RESTPULSE);
		csvField(sb, String.format(CSV_HEADER_MAX_ALTITUDE, UI.UNIT_LABEL_ALTITUDE));
		csvField(sb, String.format(CSV_HEADER_MAX_SPEED, UI.UNIT_LABEL_SPEED));
		csvField(sb, CSV_HEADER_MAX_PULSE);
		csvField(sb, String.format(CSV_HEADER_AVERAGE_SPEED, UI.UNIT_LABEL_SPEED));
		csvField(sb, String.format(CSV_HEADER_AVERAGE_PACE, UI.UNIT_LABEL_PACE));
		csvField(sb, CSV_HEADER_AVERAGE_CADENCE);
		csvField(sb, String.format(CSV_HEADER_AVERAGE_PULSE, GRAPH_LABEL_HEARTBEAT_UNIT));
		csvField(sb, String.format(CSV_HEADER_AVERAGE_TEMPERATURE, UI.UNIT_LABEL_TEMPERATURE));
		csvField(sb, CSV_HEADER_WEEK_YEAR);
		csvField(sb, CSV_HEADER_TIME_SLICES);
		csvField(sb, CSV_HEADER_TIME_INTERVAL);
		csvField(sb, CSV_HEADER_DEVICE_START_DISTANCE);
		csvField(sb, CSV_HEADER_DP_TOLERANCE);
		csvField(sb, CSV_HEADER_PERSON);

		csvField(sb, CSV_HEADER_GEAR_FRONT_SHIFT_COUNT);
		csvField(sb, CSV_HEADER_GEAR_REAR_SHIFT_COUNT);

		// end of line
		sb.append(net.tourbook.ui.UI.SYSTEM_NEW_LINE);

		exportWriter.write(sb.toString());
	}

	private void exportCSV_20_1stColumn(final StringBuilder sb,
										final int segmentCount,
										final Object segment,
										final boolean isTour) {

		if (segment instanceof TVITourBookYear) {

			final TVITourBookYear tviYear = (TVITourBookYear) segment;

			// year
			csvField(sb, tviYear.tourYear);

			if (segmentCount == 1) {

				for (int spacerIndex = segmentCount; spacerIndex < 4; spacerIndex++) {
					sb.append(UI.TAB);
				}
			}

		} else if (segment instanceof TVITourBookYearSub) {

			final TVITourBookYearSub tviYearSub = (TVITourBookYearSub) segment;

			// month or week
			csvField(sb, tviYearSub.tourYearSub);

			if (segmentCount == 2) {

				for (int spacerIndex = segmentCount; spacerIndex < 4; spacerIndex++) {
					sb.append(UI.TAB);
				}
			}

		} else if (isTour) {

			final TVITourBookTour tviTour = (TVITourBookTour) segment;

			if (isYearSubWeek()) {

				// month
				csvField(sb, tviTour.tourMonth);

			} else {

				// week
				csvField(sb, tviTour.tourWeek);
			}

			// day
			csvField(sb, tviTour.tourDay);
		}
	}

	private void exportCSV_30_OtherColumns(final StringBuilder sb, final boolean isTour, final TVITourBookItem tviItem) {

		TVITourBookTour tviTour = null;
		if (isTour) {
			tviTour = (TVITourBookTour) tviItem;
		}

		// CSV_HEADER_WEEKDAY
		{
			if (isTour) {
				sb.append(_weekDays[tviItem.colWeekDay]);
			}
			sb.append(UI.TAB);
		}

		// CSV_HEADER_TIME
		{
			if (isTour) {
				_calendar.setTimeInMillis(tviItem.colTourDate);
				sb.append(_timeFormatter.format(_calendar.getTime()));
			}
			sb.append(UI.TAB);
		}

		// CSV_HEADER_ISO_DATE_TIME
		{
			if (isTour) {
				sb.append(_isoFormatter.print(tviItem.colTourDate));
			}
			sb.append(UI.TAB);
		}

		// CSV_HEADER_NUMBER_OF_TOURS
		{
			if (isTour) {
				sb.append(Long.toString(1));
			} else {
				sb.append(Long.toString(tviItem.colCounter));
			}
			sb.append(UI.TAB);
		}

		// CSV_HEADER_TOUR_TYPE_ID
		{
			if (isTour) {
				sb.append(tviTour.getTourTypeId());
			}
			sb.append(UI.TAB);
		}

		// CSV_HEADER_TOUR_TYPE_NAME
		{
			if (isTour) {
				final long tourTypeId = tviTour.getTourTypeId();
				sb.append(net.tourbook.ui.UI.getInstance().getTourTypeLabel(tourTypeId));
			}
			sb.append(UI.TAB);
		}

		// CSV_HEADER_DISTANCE
		{
			final float dbDistance = tviItem.colDistance;
			if (dbDistance != 0) {
				sb.append(_nf1_NoGroup.format(dbDistance / 1000 / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE));
			}
			sb.append(UI.TAB);
		}

		// CSV_HEADER_ALTITUDE_UP
		{
			final long dbAltitudeUp = tviItem.colAltitudeUp;
			if (dbAltitudeUp != 0) {
				sb.append(Long.toString((long) (dbAltitudeUp / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE)));
			}
			sb.append(UI.TAB);
		}

		// CSV_HEADER_ALTITUDE_DOWN
		{
			final long dbAltitudeDown = tviItem.colAltitudeDown;
			if (dbAltitudeDown != 0) {
				sb.append(Long.toString((long) (-dbAltitudeDown / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE)));
			}
			sb.append(UI.TAB);
		}

		// CSV_HEADER_RECORDING_TIME
		{
			final long colRecordingTime = (tviItem).colRecordingTime;
			if (colRecordingTime != 0) {
				sb.append(Long.toString(colRecordingTime));
			}
			sb.append(UI.TAB);
		}

		// CSV_HEADER_MOVING_TIME
		{
			final long colDrivingTime = tviItem.colDrivingTime;
			if (colDrivingTime != 0) {
				sb.append(Long.toString(colDrivingTime));
			}
			sb.append(UI.TAB);
		}

		// CSV_HEADER_PAUSED_TIME
		{
			final long colPausedTime = tviItem.colPausedTime;
			if (colPausedTime != 0) {
				sb.append(Long.toString(colPausedTime));
			}
			sb.append(UI.TAB);
		}

		// CSV_HEADER_RELATIVE_PAUSED_TIME
		{
			final long colPausedTime = tviItem.colPausedTime;
			final long dbPausedTime = colPausedTime;
			final long dbRecordingTime = tviItem.colRecordingTime;
			final float relativePausedTime = dbRecordingTime == 0 //
					? 0
					: (float) dbPausedTime / dbRecordingTime * 100;
			if (relativePausedTime != 0) {
				sb.append(_nf1_NoGroup.format(relativePausedTime));
			}
			sb.append(UI.TAB);
		}

		// CSV_HEADER_RECORDING_TIME hhh:mm:ss
		{
			final long colRecordingTime = (tviItem).colRecordingTime;
			if (colRecordingTime != 0) {
				sb.append(net.tourbook.ui.UI.format_hh_mm_ss(colRecordingTime));
			}
			sb.append(UI.TAB);
		}

		// CSV_HEADER_MOVING_TIME hhh:mm:ss
		{
			final long colDrivingTime = tviItem.colDrivingTime;
			if (colDrivingTime != 0) {
				sb.append(net.tourbook.ui.UI.format_hh_mm_ss(colDrivingTime));
			}
			sb.append(UI.TAB);
		}

		// CSV_HEADER_PAUSED_TIME hhh:mm:ss
		{
			final long colPausedTime = tviItem.colPausedTime;
			if (colPausedTime != 0) {
				sb.append(net.tourbook.ui.UI.format_hh_mm_ss(colPausedTime));
			}
			sb.append(UI.TAB);
		}

		// CSV_HEADER_NUMBER_OF_MARKER
		{
			if (isTour) {
				final ArrayList<Long> markerIds = tviTour.getMarkerIds();
				if (markerIds != null) {
					sb.append(Integer.toString(markerIds.size()));
				}
			}
			sb.append(UI.TAB);
		}

		// CSV_HEADER_NUMBER_OF_PHOTOS
		{
			final int numberOfPhotos = tviItem.colNumberOfPhotos;
			if (numberOfPhotos != 0) {
				sb.append(Integer.toString(numberOfPhotos));
			}

			sb.append(UI.TAB);
		}

		// CSV_HEADER_WEATHER
		{
			if (isTour) {
				final String windClouds = tviTour.colClouds;
				if (windClouds != null) {
					sb.append(windClouds);
				}

			}
			sb.append(UI.TAB);
		}

		// CSV_HEADER_WIND_SPEED
		{
			final int windSpeed = (int) (tviItem.colWindSpd / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE);
			if (windSpeed != 0) {
				sb.append(Integer.toString(windSpeed));
			}
			sb.append(UI.TAB);
		}

		// CSV_HEADER_WIND_DIRECTION
		{
			if (isTour) {
				final int windDir = tviItem.colWindDir;
				if (windDir != 0) {
					sb.append(Integer.toString(windDir));
				}
			}
			sb.append(UI.TAB);
		}

		// CSV_HEADER_TITLE
		{
			final String dbTourTitle = tviItem.colTourTitle;
			if (dbTourTitle != null) {
				sb.append(dbTourTitle);
			}
			sb.append(UI.TAB);
		}

		// CSV_HEADER_TAGS
		{
			if (isTour) {
				sb.append(TourDatabase.getTagNames(tviTour.getTagIds()));
			}
			sb.append(UI.TAB);
		}

		// CSV_HEADER_CALORIES
		{
			final long caloriesSum = tviItem.colCalories;
			if (caloriesSum != 0) {
				sb.append(Long.toString(caloriesSum));
			}
			sb.append(UI.TAB);
		}

		// CSV_HEADER_RESTPULSE
		{
			if (isTour) {
				final int restPulse = tviItem.colRestPulse;
				if (restPulse != 0) {
					sb.append(Integer.toString(restPulse));
				}
			}
			sb.append(UI.TAB);
		}

		// CSV_HEADER_MAX_ALTITUDE
		{
			final long dbMaxAltitude = tviItem.colMaxAltitude;
			if (dbMaxAltitude != 0) {
				sb.append(Long.toString((long) (dbMaxAltitude / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE)));
			}
			sb.append(UI.TAB);
		}

		// CSV_HEADER_MAX_SPEED
		{
			final float dbMaxSpeed = tviItem.colMaxSpeed;
			if (dbMaxSpeed != 0) {
				sb.append(_nf1_NoGroup.format(dbMaxSpeed / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE));
			}
			sb.append(UI.TAB);
		}

		// CSV_HEADER_MAX_PULSE
		{
			if (isTour) {
				final long dbMaxPulse = tviItem.colMaxPulse;
				if (dbMaxPulse != 0) {
					sb.append(Long.toString(dbMaxPulse));
				}
			}
			sb.append(UI.TAB);
		}

		// CSV_HEADER_AVERAGE_SPEED
		{
			final float speed = tviItem.colAvgSpeed / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;
			if (speed != 0) {
				sb.append(_nf1_NoGroup.format(speed));
			}
			sb.append(UI.TAB);
		}

		// CSV_HEADER_AVERAGE_PACE
		{
			final float pace = tviItem.colAvgPace * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;
			if (pace != 0) {
				sb.append(net.tourbook.ui.UI.format_mm_ss((long) pace));
			}
			sb.append(UI.TAB);
		}

		// CSV_HEADER_AVERAGE_CADENCE
		{
			final float dbAvgCadence = tviItem.colAvgCadence;
			if (dbAvgCadence != 0) {
				sb.append(_nf1_NoGroup.format(dbAvgCadence));
			}
			sb.append(UI.TAB);
		}

		// CSV_HEADER_AVERAGE_PULSE
		{
			final float pulse = tviItem.colAvgPulse;
			if (pulse != 0) {
				sb.append(_nf1_NoGroup.format(pulse));
			}
			sb.append(UI.TAB);
		}

		// CSV_HEADER_AVERAGE_TEMPERATURE
		{
			float temperature = tviItem.colAvgTemperature;

			if (temperature != 0) {
				if (net.tourbook.ui.UI.UNIT_VALUE_TEMPERATURE != 1) {
					temperature = temperature
							* net.tourbook.ui.UI.UNIT_FAHRENHEIT_MULTI
							+ net.tourbook.ui.UI.UNIT_FAHRENHEIT_ADD;
				}
				sb.append(_nf1_NoGroup.format(temperature));
			}
			sb.append(UI.TAB);
		}

		// CSV_HEADER_WEEK_YEAR
		{
			if (isTour) {
				final int week = tviItem.colWeekYear;
				if (week != 0) {
					sb.append(Integer.toString(week));
				}
			}
			sb.append(UI.TAB);
		}

		// CSV_HEADER_TIME_SLICES
		{
			final int numberOfTimeSlices = tviItem.colNumberOfTimeSlices;
			if (numberOfTimeSlices != 0) {
				sb.append(Integer.toString(numberOfTimeSlices));
			}
			sb.append(UI.TAB);
		}

		// CSV_HEADER_TIME_INTERVAL
		{
			if (isTour) {
				final short dbTimeInterval = tviTour.getColumnTimeInterval();
				if (dbTimeInterval != 0) {
					sb.append(Long.toString(dbTimeInterval));
				}
			}
			sb.append(UI.TAB);
		}

		// CSV_HEADER_DEVICE_START_DISTANCE
		{
			if (isTour) {
				final long dbStartDistance = tviTour.getColumnStartDistance();
				if (dbStartDistance != 0) {
					sb.append(Long.toString((long) (dbStartDistance / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE)));
				}
			}
			sb.append(UI.TAB);
		}

		// CSV_HEADER_DP_TOLERANCE
		{
			if (isTour) {
				final int dpTolerance = tviItem.colDPTolerance;
				if (dpTolerance != 0) {
					sb.append(_nf1_NoGroup.format(dpTolerance / 10.0));
				}
			}
			sb.append(UI.TAB);
		}

		// CSV_HEADER_PERSON
		{
			if (isTour) {
				final long dbPersonId = tviTour.colPersonId;
				sb.append(PersonManager.getPersonName(dbPersonId));
			}
			sb.append(UI.TAB);
		}

		// CSV_HEADER_GEAR_FRONT_SHIFT_COUNT
		{
			final int shiftCount = tviItem.colFrontShiftCount;
			sb.append(Integer.toString(shiftCount));

			sb.append(UI.TAB);
		}

		// CSV_HEADER_GEAR_REAR_SHIFT_COUNT
		{
			final int shiftCount = tviItem.colRearShiftCount;
			sb.append(Integer.toString(shiftCount));

			sb.append(UI.TAB);
		}
	}

	private void fillActionBars() {

		/*
		 * fill view menu
		 */
		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();

		menuMgr.add(_actionModifyColumns);

		/*
		 * fill view toolbar
		 */
		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

		tbm.add(_actionSelectAllTours);
		tbm.add(_actionToggleMonthWeek);

		tbm.add(new Separator());
		tbm.add(_actionExpandSelection);
		tbm.add(_actionCollapseAll);

		tbm.add(_actionRefreshView);
	}

	private void fillContextMenu(final IMenuManager menuMgr) {

		menuMgr.add(_actionEditQuick);
		menuMgr.add(_actionEditTour);
		menuMgr.add(_actionOpenMarkerDialog);
		menuMgr.add(_actionOpenAdjustAltitudeDialog);
		menuMgr.add(_actionOpenTour);
		menuMgr.add(_actionMergeTour);
		menuMgr.add(_actionJoinTours);

		menuMgr.add(new Separator());
		menuMgr.add(_actionComputeElevationGain);
		menuMgr.add(_actionComputeDistanceValuesFromGeoposition);
		menuMgr.add(_actionSetAltitudeFromSRTM);

		_tagMenuMgr.fillTagMenu(menuMgr);

		// tour type actions
		menuMgr.add(new Separator());
		menuMgr.add(_actionSetTourType);
		TourTypeMenuManager.fillMenuWithRecentTourTypes(menuMgr, this, true);

		menuMgr.add(new Separator());
		menuMgr.add(_actionCollapseOthers);
		menuMgr.add(_actionExpandSelection);
		menuMgr.add(_actionCollapseAll);

		menuMgr.add(new Separator());
		menuMgr.add(_actionExportTour);
		menuMgr.add(_actionExportViewCSV);
		menuMgr.add(_actionReimportSubMenu);
		menuMgr.add(_actionPrintTour);

		menuMgr.add(new Separator());
		menuMgr.add(_actionSetOtherPerson);
		menuMgr.add(_actionDeleteTour);

		enableActions();
	}

	@Override
	public ColumnManager getColumnManager() {
		return _columnManager;
	}

	@Override
	public PostSelectionProvider getPostSelectionProvider() {
		return _postSelectionProvider;
	}

	private void getSelectedTourData(final ArrayList<TourData> selectedTourData, final Set<Long> tourIdSet) {
		for (final Long tourId : tourIdSet) {
			selectedTourData.add(TourManager.getInstance().getTourData(tourId));
		}
	}

	@Override
	public Set<Long> getSelectedTourIDs() {

		final Set<Long> tourIds = new HashSet<Long>();

		final IStructuredSelection selectedTours = ((IStructuredSelection) _tourViewer.getSelection());
		if (selectedTours.size() < 2) {

			// one item is selected

			final Object selectedItem = selectedTours.getFirstElement();
			if (selectedItem instanceof TVITourBookYear) {

				// one year is selected

				if (_actionSelectAllTours.isChecked()) {

					// loop: all months
					for (final TreeViewerItem viewerItem : ((TVITourBookYear) selectedItem).getFetchedChildren()) {
						if (viewerItem instanceof TVITourBookYearSub) {
							getYearSubTourIDs((TVITourBookYearSub) viewerItem, tourIds);
						}
					}
				}

			} else if (selectedItem instanceof TVITourBookYearSub) {

				// one month/week is selected

				if (_actionSelectAllTours.isChecked()) {
					getYearSubTourIDs((TVITourBookYearSub) selectedItem, tourIds);
				}

			} else if (selectedItem instanceof TVITourBookTour) {

				// one tour is selected

				tourIds.add(((TVITourBookTour) selectedItem).getTourId());
			}

		} else {

			// multiple items are selected

			// get all selected tours, ignore year and month items
			for (final Iterator<?> tourIterator = selectedTours.iterator(); tourIterator.hasNext();) {
				final Object viewItem = tourIterator.next();

				if (viewItem instanceof TVITourBookTour) {
					tourIds.add(((TVITourBookTour) viewItem).getTourId());
				}
			}
		}

		return tourIds;
	}

	@Override
	public ArrayList<TourData> getSelectedTours() {

		// get selected tour id's

		final Set<Long> tourIds = getSelectedTourIDs();

		/*
		 * show busyindicator when multiple tours needs to be retrieved from the database
		 */
		final ArrayList<TourData> selectedTourData = new ArrayList<TourData>();

		if (tourIds.size() > 1) {
			BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
				@Override
				public void run() {
					getSelectedTourData(selectedTourData, tourIds);
				}
			});
		} else {
			getSelectedTourData(selectedTourData, tourIds);
		}

		return selectedTourData;
	}

	@Override
	public ColumnViewer getViewer() {
		return _tourViewer;
	}

	public int getYearSub() {
		return _yearSubCategory;
	}

	/**
	 * @param yearSubItem
	 * @param tourIds
	 * @return Return all tours for one yearSubItem
	 */
	private void getYearSubTourIDs(final TVITourBookYearSub yearSubItem, final Set<Long> tourIds) {

		// get all tours for the month item
		for (final TreeViewerItem viewerItem : yearSubItem.getFetchedChildren()) {
			if (viewerItem instanceof TVITourBookTour) {

				final TVITourBookTour tourItem = (TVITourBookTour) viewerItem;
				tourIds.add(tourItem.getTourId());
			}
		}
	}

	/**
	 * @return Returns <code>true</code> when the year subcategory is week, otherwise it is month.
	 */
	private boolean isYearSubWeek() {

		return _yearSubCategory == TVITourBookItem.ITEM_TYPE_WEEK;
	}

	private void onSelectTreeItem(final SelectionChangedEvent event) {

		if (_isInReload) {
			return;
		}

		final boolean isSelectAllChildren = _actionSelectAllTours.isChecked();

		final HashSet<Long> tourIds = new HashSet<Long>();

		boolean isFirstYear = true;
		boolean isFirstYearSub = true;
		boolean isFirstTour = true;

		final IStructuredSelection selectedTours = (IStructuredSelection) (event.getSelection());
		// loop: all selected items
		for (final Iterator<?> itemIterator = selectedTours.iterator(); itemIterator.hasNext();) {

			final Object treeItem = itemIterator.next();

			if (isSelectAllChildren) {

				// get ALL tours from all selected tree items (year/month/tour)

				if (treeItem instanceof TVITourBookYear) {

					// year is selected

					final TVITourBookYear yearItem = ((TVITourBookYear) treeItem);
					if (isFirstYear) {
						// keep selected year
						isFirstYear = false;
						_selectedYear = yearItem.tourYear;
					}

					// get all tours for the selected year
					for (final TreeViewerItem viewerItem : yearItem.getFetchedChildren()) {
						if (viewerItem instanceof TVITourBookYearSub) {
							getYearSubTourIDs((TVITourBookYearSub) viewerItem, tourIds);
						}
					}

				} else if (treeItem instanceof TVITourBookYearSub) {

					// month/week/day is selected

					final TVITourBookYearSub yearSubItem = (TVITourBookYearSub) treeItem;
					if (isFirstYearSub) {
						// keep selected year/month/week/day
						isFirstYearSub = false;
						_selectedYear = yearSubItem.tourYear;
						_selectedYearSub = yearSubItem.tourYearSub;
					}

					// get all tours for the selected month
					getYearSubTourIDs(yearSubItem, tourIds);

				} else if (treeItem instanceof TVITourBookTour) {

					// tour is selected

					final TVITourBookTour tourItem = (TVITourBookTour) treeItem;
					if (isFirstTour) {
						// keep selected tour
						isFirstTour = false;
						_selectedYear = tourItem.tourYear;
						_selectedYearSub = tourItem.tourYearSub;
					}

					tourIds.add(tourItem.getTourId());
				}

			} else {

				// get only selected tours

				if (treeItem instanceof TVITourBookTour) {

					final TVITourBookTour tourItem = (TVITourBookTour) treeItem;

					if (isFirstTour) {
						// keep selected tour
						isFirstTour = false;
						_selectedYear = tourItem.tourYear;
						_selectedYearSub = tourItem.tourYearSub;
					}

					tourIds.add(tourItem.getTourId());
				}
			}
		}

		ISelection selection;
		if (tourIds.size() == 0) {

			// fire selection that nothing is selected

			selection = new SelectionTourIds(new ArrayList<Long>());

		} else {

			// keep selected tour id's
			_selectedTourIds.clear();
			_selectedTourIds.addAll(tourIds);

			selection = tourIds.size() == 1 //
					? new SelectionTourId(_selectedTourIds.get(0))
					: new SelectionTourIds(_selectedTourIds);

		}

		// _postSelectionProvider should be removed when all parts are listening to the TourManager event
		if (_isInStartup) {

			_isInStartup = false;

			// this view can be inactive -> selection is not fired with the SelectionProvider interface

			TourManager.fireEvent(TourEventId.TOUR_SELECTION, selection, this);

		} else {

			_postSelectionProvider.setSelection(selection);
		}

		enableActions();
	}

	private void readDisplayFormats() {

		_isRecTimeFormat_hhmmss = _prefStore.getString(ITourbookPreferences.VIEW_LAYOUT_RECORDING_TIME_FORMAT).equals(
				PrefPageViewColors.VIEW_TIME_LAYOUT_HH_MM_SS);

		_isDriveTimeFormat_hhmmss = _prefStore.getString(ITourbookPreferences.VIEW_LAYOUT_DRIVING_TIME_FORMAT).equals(
				PrefPageViewColors.VIEW_TIME_LAYOUT_HH_MM_SS);
	}

	@Override
	public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

		_viewerContainer.setRedraw(false);
		{
			final Object[] expandedElements = _tourViewer.getExpandedElements();
			final ISelection selection = _tourViewer.getSelection();

			_tourViewer.getTree().dispose();

			createUI_10_TourViewer(_viewerContainer);
			_viewerContainer.layout();

			_tourViewer.setInput(_rootItem = new TVITourBookRoot(this));

			_tourViewer.setExpandedElements(expandedElements);
			_tourViewer.setSelection(selection);
		}
		_viewerContainer.setRedraw(true);

		return _tourViewer;
	}

	@Override
	public void reloadViewer() {

		final Tree tree = _tourViewer.getTree();
		tree.setRedraw(false);
		_isInReload = true;
		{
			final Object[] expandedElements = _tourViewer.getExpandedElements();
			final ISelection selection = _tourViewer.getSelection();

			_tourViewer.setInput(_rootItem = new TVITourBookRoot(this));

			_tourViewer.setExpandedElements(expandedElements);
			_tourViewer.setSelection(selection, true);
		}
		_isInReload = false;
		tree.setRedraw(true);
	}

	void reopenFirstSelectedTour() {

		_selectedYear = -1;
		_selectedYearSub = -1;
		TVITourBookTour selectedTourItem = null;

		final ISelection oldSelection = _tourViewer.getSelection();
		if (oldSelection != null) {

			final Object selection = ((IStructuredSelection) oldSelection).getFirstElement();
			if (selection instanceof TVITourBookTour) {

				selectedTourItem = (TVITourBookTour) selection;

				_selectedYear = selectedTourItem.tourYear;

				if (getYearSub() == TVITourBookItem.ITEM_TYPE_WEEK) {
					_selectedYearSub = selectedTourItem.tourWeek;
				} else {
					_selectedYearSub = selectedTourItem.tourMonth;
				}
			}
		}

		reloadViewer();
		reselectTourViewer();

		final IStructuredSelection newSelection = (IStructuredSelection) _tourViewer.getSelection();
		if (newSelection != null) {

			final Object selection = newSelection.getFirstElement();
			if (selection instanceof TVITourBookTour) {

				selectedTourItem = (TVITourBookTour) selection;

				_tourViewer.collapseAll();
				_tourViewer.expandToLevel(selectedTourItem, 0);
				_tourViewer.setSelection(new StructuredSelection(selectedTourItem), false);
			}
		}
	}

	private void reselectTourViewer() {

		// find the old selected year/[month/week] in the new tour items
		TreeViewerItem reselectYearItem = null;
		TreeViewerItem reselectYearSubItem = null;
		final ArrayList<TreeViewerItem> reselectTourItems = new ArrayList<TreeViewerItem>();

		/*
		 * get the year/month/tour item in the data model
		 */
		final ArrayList<TreeViewerItem> yearItems = _rootItem.getChildren();
		for (final TreeViewerItem yearItem : yearItems) {

			final TVITourBookYear tourBookYear = ((TVITourBookYear) yearItem);
			if (tourBookYear.tourYear == _selectedYear) {

				reselectYearItem = yearItem;

				final Object[] yearSubItems = tourBookYear.getFetchedChildrenAsArray();
				for (final Object yearSub : yearSubItems) {

					final TVITourBookYearSub tourBookYearSub = ((TVITourBookYearSub) yearSub);
					if (tourBookYearSub.tourYearSub == _selectedYearSub) {

						reselectYearSubItem = tourBookYearSub;

						final Object[] tourItems = tourBookYearSub.getFetchedChildrenAsArray();
						for (final Object tourItem : tourItems) {

							final TVITourBookTour tourBookTour = ((TVITourBookTour) tourItem);
							final long treeTourId = tourBookTour.tourId;

							for (final Long tourId : _selectedTourIds) {
								if (treeTourId == tourId) {
									reselectTourItems.add(tourBookTour);
									break;
								}
							}
						}
						break;
					}
				}
				break;
			}
		}

		// select year/month/tour in the viewer
		if (reselectTourItems.size() > 0) {

			_tourViewer.setSelection(new StructuredSelection(reselectTourItems) {}, false);

		} else if (reselectYearSubItem != null) {

			_tourViewer.setSelection(new StructuredSelection(reselectYearSubItem) {}, false);

		} else if (reselectYearItem != null) {

			_tourViewer.setSelection(new StructuredSelection(reselectYearItem) {}, false);

		} else if (yearItems.size() > 0) {

			// the old year was not found, select the newest year

			final TreeViewerItem yearItem = yearItems.get(yearItems.size() - 1);

			_tourViewer.setSelection(new StructuredSelection(yearItem) {}, true);
		}

		// move the horizontal scrollbar to the left border
		final ScrollBar horizontalBar = _tourViewer.getTree().getHorizontalBar();
		if (horizontalBar != null) {
			horizontalBar.setSelection(0);
		}
	}

	private void restoreState() {

		// set tour viewer reselection data
		try {
			_selectedYear = _state.getInt(STATE_SELECTED_YEAR);
		} catch (final NumberFormatException e) {
			_selectedYear = -1;
		}

		try {
			_selectedYearSub = _state.getInt(STATE_SELECTED_MONTH);
		} catch (final NumberFormatException e) {
			_selectedYearSub = -1;
		}

		final String[] selectedTourIds = _state.getArray(STATE_SELECTED_TOURS);
		_selectedTourIds.clear();

		if (selectedTourIds != null) {
			for (final String tourId : selectedTourIds) {
				try {
					_selectedTourIds.add(Long.valueOf(tourId));
				} catch (final NumberFormatException e) {
					// ignore
				}
			}
		}

		_actionSelectAllTours.setChecked(_state.getBoolean(STATE_IS_SELECT_YEAR_MONTH_TOURS));

		/*
		 * Year sub category
		 */
		_yearSubCategory = Util.getStateInt(_state, STATE_YEAR_SUB_CATEGORY, TVITourBookItem.ITEM_TYPE_MONTH);
		_actionToggleMonthWeek.setImageDescriptor(//
				TourbookPlugin.getImageDescriptor(_yearSubCategory == TVITourBookItem.ITEM_TYPE_WEEK
						? Messages.Image__TourBook_Month
						: Messages.Image__TourBook_Week));

		updateToolTipState();
	}

	private void saveState() {

		// save selection in the tour viewer
		_state.put(STATE_SELECTED_YEAR, _selectedYear);
		_state.put(STATE_SELECTED_MONTH, _selectedYearSub);

		// convert tour id's into string
		final ArrayList<String> selectedTourIds = new ArrayList<String>();
		for (final Long tourId : _selectedTourIds) {
			selectedTourIds.add(tourId.toString());
		}
		_state.put(STATE_SELECTED_TOURS, selectedTourIds.toArray(new String[selectedTourIds.size()]));

		// action: select tours for year/yearSub
		_state.put(STATE_IS_SELECT_YEAR_MONTH_TOURS, _actionSelectAllTours.isChecked());

		_state.put(STATE_YEAR_SUB_CATEGORY, _yearSubCategory);

		_columnManager.saveState(_state);
	}

	public void setActiveYear(final int activeYear) {
		_selectedYear = activeYear;
	}

	private void setCellColor(final ViewerCell cell, final Object element) {

		if (element instanceof TVITourBookYear) {
			cell.setForeground(JFaceResources.getColorRegistry().get(net.tourbook.ui.UI.VIEW_COLOR_SUB));
		} else if (element instanceof TVITourBookYearSub) {
			cell.setForeground(JFaceResources.getColorRegistry().get(net.tourbook.ui.UI.VIEW_COLOR_SUB_SUB));
//		} else if (element instanceof TVITourBookTour) {
//			cell.setForeground(JFaceResources.getColorRegistry().get(UI.VIEW_COLOR_TOUR));
		}
	}

	@Override
	public void setFocus() {
		_tourViewer.getControl().setFocus();
	}

	private void updateToolTipState() {

		_isToolTipInDate = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_DATE);
		_isToolTipInTime = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_TIME);
		_isToolTipInWeekDay = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_WEEKDAY);
		_isToolTipInTitle = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_TITLE);
		_isToolTipInTags = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_TAGS);
	}

}
