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
package net.tourbook.ui.views.rawData;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ITourViewer3;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.data.TourWayPoint;
import net.tourbook.database.TourDatabase;
import net.tourbook.extension.export.ActionExport;
import net.tourbook.importdata.DeviceImportLauncher;
import net.tourbook.importdata.DeviceImportManager;
import net.tourbook.importdata.DeviceImportState;
import net.tourbook.importdata.DialogDeviceImportConfig;
import net.tourbook.importdata.ImportConfig;
import net.tourbook.importdata.ImportState;
import net.tourbook.importdata.OSFile;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.importdata.SpeedTourType;
import net.tourbook.importdata.TourTypeConfig;
import net.tourbook.photo.ImageUtils;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageImport;
import net.tourbook.tag.TagMenuManager;
import net.tourbook.tour.ActionOpenAdjustAltitudeDialog;
import net.tourbook.tour.ActionOpenMarkerDialog;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.ITourItem;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourDoubleClickState;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TourTypeMenuManager;
import net.tourbook.ui.ITourProviderAll;
import net.tourbook.ui.TableColumnFactory;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.ui.action.ActionEditTour;
import net.tourbook.ui.action.ActionJoinTours;
import net.tourbook.ui.action.ActionModifyColumns;
import net.tourbook.ui.action.ActionOpenTour;
import net.tourbook.ui.action.ActionSetTourTypeMenu;
import net.tourbook.ui.views.TableViewerTourInfoToolTip;
import net.tourbook.ui.views.TourInfoToolTipCellLabelProvider;
import net.tourbook.web.WEB;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.DeviceResourceException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

/**
 *
 */
public class RawDataView extends ViewPart implements ITourProviderAll, ITourViewer3 {

	public static final String				ID											= "net.tourbook.views.rawData.RawDataView"; //$NON-NLS-1$
	//
	private static final String				WEB_RESOURCE_TITLE_FONT						= "Nunito-Bold.ttf";						//$NON-NLS-1$
//	private static final String				WEB_RESOURCE_TITLE_FONT						= "NothingYouCouldDo.ttf";					//$NON-NLS-1$
	private static final String				WEB_RESOURCE_TOUR_IMPORT_BG_IMAGE			= "mytourbook-icon.svg";					//$NON-NLS-1$
	private static final String				WEB_RESOURCE_TOUR_IMPORT_CSS				= "tour-import.css";						//$NON-NLS-1$
	private static final String				WEB_RESOURCE_TOUR_IMPORT_CSS3				= "tour-import-css3.css";					//$NON-NLS-1$
	//
	private static final String				CSS_IMPORT_BACKGROUND						= "div.import-background";					//$NON-NLS-1$
	private static final String				CSS_IMPORT_TILE								= "a.import-tile";							//$NON-NLS-1$
	//
	public static final int					COLUMN_DATE									= 0;
	public static final int					COLUMN_TITLE								= 1;
	public static final int					COLUMN_DATA_FORMAT							= 2;
	public static final int					COLUMN_FILE_NAME							= 3;
	//
	private static final String				STATE_IMPORTED_FILENAMES					= "importedFilenames";						//$NON-NLS-1$
	private static final String				STATE_SELECTED_TOUR_INDICES					= "SelectedTourIndices";					//$NON-NLS-1$
	private static final String				STATE_IS_REMOVE_TOURS_WHEN_VIEW_CLOSED		= "STATE_IS_REMOVE_TOURS_WHEN_VIEW_CLOSED"; //$NON-NLS-1$
	public static final String				STATE_IS_MERGE_TRACKS						= "isMergeTracks";							//$NON-NLS-1$
	public static final String				STATE_IS_CHECKSUM_VALIDATION				= "isChecksumValidation";					//$NON-NLS-1$
	public static final String				STATE_IS_CONVERT_WAYPOINTS					= "STATE_IS_CONVERT_WAYPOINTS";			//$NON-NLS-1$
	public static final String				STATE_IS_CREATE_TOUR_ID_WITH_TIME			= "isCreateTourIdWithTime";				//$NON-NLS-1$
	public static final boolean				STATE_IS_MERGE_TRACKS_DEFAULT				= false;
	public static final boolean				STATE_IS_CHECKSUM_VALIDATION_DEFAULT		= true;
	public static final boolean				STATE_IS_CONVERT_WAYPOINTS_DEFAULT			= true;
	public static final boolean				STATE_IS_CREATE_TOUR_ID_WITH_TIME_DEFAULT	= false;
	//
	private static final String				HREF_TOKEN									= "#";										//$NON-NLS-1$
	private static final String				PAGE_ABOUT_BLANK							= "about:blank";							//$NON-NLS-1$

	/**
	 * This is necessary otherwise XULrunner in Linux do not fire a location change event.
	 */
	private static final String				HTTP_DUMMY									= "http://dummy";							//$NON-NLS-1$

	private static String					ACTION_DEVICE_IMPORT						= "DeviceImport";							//$NON-NLS-1$
	private static final String				ACTION_IMPORT_FROM_FILES					= "ImportFromFiles";						//$NON-NLS-1$
	private static final String				ACTION_SERIAL_PORT_CONFIGURED				= "SerialPortConfigured";					//$NON-NLS-1$
	private static final String				ACTION_SERIAL_PORT_DIRECTLY					= "SerialPortDirectly";					//$NON-NLS-1$
	private static final String				ACTION_SETUP_DEVICE_IMPORT					= "SetupDeviceImport";						//$NON-NLS-1$

	private static final String				DOM_ID_DEVICE_STATE							= "deviceState";							//$NON-NLS-1$

	private static String					HREF_DEVICE_IMPORT;
	private static String					HREF_IMPORT_FROM_FILES;
	private static String					HREF_SERIAL_PORT_CONFIGURED;
	private static String					HREF_SERIAL_PORT_DIRECTLY;
	private static String					HREF_SETUP_DEVICE_IMPORT;

	static {
		HREF_DEVICE_IMPORT = HREF_TOKEN + ACTION_DEVICE_IMPORT;
		HREF_IMPORT_FROM_FILES = HREF_TOKEN + ACTION_IMPORT_FROM_FILES;
		HREF_SERIAL_PORT_CONFIGURED = HREF_TOKEN + ACTION_SERIAL_PORT_CONFIGURED;
		HREF_SERIAL_PORT_DIRECTLY = HREF_TOKEN + ACTION_SERIAL_PORT_DIRECTLY;
		HREF_SETUP_DEVICE_IMPORT = HREF_TOKEN + ACTION_SETUP_DEVICE_IMPORT + HREF_TOKEN;
	}

	public static final int					TILE_SIZE_DEFAULT							= 80;
	public static final int					TILE_SIZE_MIN								= 20;
	public static final int					TILE_SIZE_MAX								= 300;
	public static final int					NUM_HORIZONTAL_TILES_DEFAULT				= 5;
	public static final int					NUM_HORIZONTAL_TILES_MIN					= 1;
	public static final int					NUM_HORIZONTAL_TILES_MAX					= 50;

	//
	private final IPreferenceStore			_prefStore									= TourbookPlugin.getPrefStore();
	private final IDialogSettings			_state										= TourbookPlugin.getState(ID);
	//
	private RawDataManager					_rawDataMgr									= RawDataManager.getInstance();
	//
	private PostSelectionProvider			_postSelectionProvider;
	private IPartListener2					_partListener;
	private ISelectionListener				_postSelectionListener;
	private IPropertyChangeListener			_prefChangeListener;
	private ITourEventListener				_tourEventListener;
	//
	// context menu actions
	private ActionClearView					_actionClearView;
	private ActionExport					_actionExportTour;
	private ActionEditQuick					_actionEditQuick;
	private ActionEditTour					_actionEditTour;
	private ActionJoinTours					_actionJoinTours;
	private ActionMergeIntoMenu				_actionMergeIntoTour;
	private ActionMergeTour					_actionMergeTour;
	private ActionModifyColumns				_actionModifyColumns;
	private ActionOpenTour					_actionOpenTour;
	private ActionOpenMarkerDialog			_actionOpenMarkerDialog;
	private ActionOpenAdjustAltitudeDialog	_actionOpenAdjustAltitudeDialog;
	private ActionOpenPrefDialog			_actionEditImportPreferences;
	private ActionReimportSubMenu			_actionReimportSubMenu;
	private ActionRemoveTour				_actionRemoveTour;
	private ActionRemoveToursWhenClosed		_actionRemoveToursWhenClosed;
	private ActionSaveTourInDatabase		_actionSaveTour;
	private ActionSaveTourInDatabase		_actionSaveTourWithPerson;
	private ActionSetupImport				_actionSetupImport;
	private ActionSetTourTypeMenu			_actionSetTourType;
	//
	protected TourPerson					_activePerson;
	protected TourPerson					_newActivePerson;
	//
	private boolean							_isRunAnimation								= true;
	private boolean							_isInUIStartup								= true;
	protected boolean						_isPartVisible								= false;
	protected boolean						_isViewerPersonDataDirty					= false;
	//
	private ColumnManager					_columnManager;
	//
	private final Calendar					_calendar									= GregorianCalendar
																								.getInstance();
	private final DateFormat				_dateFormatter								= DateFormat
																								.getDateInstance(DateFormat.SHORT);
	private final DateFormat				_timeFormatter								= DateFormat
																								.getTimeInstance(DateFormat.SHORT);
	private final DateFormat				_durationFormatter							= DateFormat.getTimeInstance(
																								DateFormat.SHORT,
																								Locale.GERMAN);
	private final NumberFormat				_nf1										= NumberFormat
																								.getNumberInstance();
	private final NumberFormat				_nf3										= NumberFormat
																								.getNumberInstance();
	{
		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);
		_nf3.setMinimumFractionDigits(3);
		_nf3.setMaximumFractionDigits(3);
	}
	//
	private boolean							_isToolTipInDate;
	private boolean							_isToolTipInTime;
	private boolean							_isToolTipInTitle;
	private boolean							_isToolTipInTags;
	//
	private TagMenuManager					_tagMenuMgr;
	private TourDoubleClickState			_tourDoubleClickState						= new TourDoubleClickState();
	//
	private Thread							_devicePollingThread;
	private Thread							_watchFolderThread;
	private WatchService					_watcher;
	private WatchKey						_watchKey;
	private boolean							_isStopPollingThread;
	private boolean							_isCancelWatchFolder;
	private AtomicBoolean					_canPollImportFiles							= new AtomicBoolean();
	private AtomicBoolean					_browserTask_DeviceState					= new AtomicBoolean();
	//
	private HashMap<Long, Image>			_configImages								= new HashMap<>();
	private HashMap<Long, Integer>			_configImageHash							= new HashMap<>();
	//
	private boolean							_isBrowserCompleted;
	//
	private String							_cssFonts;
	private String							_cssFromFile;
	//
	private String							_imageUrl_AppStateError;
	private String							_imageUrl_AppStateOK;
	private String							_imageUrl_DeviceFolder_OK;
	private String							_imageUrl_DeviceFolder_NotAvailable;
	private String							_imageUrl_ImportFromFile;
	private String							_imageUrl_SerialPort_Configured;
	private String							_imageUrl_SerialPort_Directly;
	//
	private PixelConverter					_pc;

	/*
	 * resources
	 */
	private ImageDescriptor					_imageDescDatabase;
	private ImageDescriptor					_imageDescDatabaseOtherPerson;
	private ImageDescriptor					_imageDescDatabaseAssignMergedTour;
	private ImageDescriptor					_imageDescDatabasePlaceholder;
	private ImageDescriptor					_imageDescDelete;
	//
	private Image							_imageDatabase;
	private Image							_imageDatabaseOtherPerson;
	private Image							_imageDatabaseAssignMergedTour;
	private Image							_imageDatabasePlaceholder;
	private Image							_imageDelete;

	/*
	 * UI controls
	 */
	private PageBook						_topPageBook;
	private Composite						_topPage_Startup;
	private Composite						_topPage_Dashboard;
	private Composite						_topPage_ImportViewer;

	private PageBook						_dashboard_PageBook;
	private Composite						_dashboardPage_NoBrowser;
	private Composite						_dashboardPage_WithBrowser;

	private Composite						_parent;
	private Text							_txtNoBrowser;

	private TableViewer						_tourViewer;
	private TableViewerTourInfoToolTip		_tourInfoToolTip;

	private Browser							_browser;

	private class TourDataContentProvider implements IStructuredContentProvider {

		public TourDataContentProvider() {}

		@Override
		public void dispose() {}

		@Override
		public Object[] getElements(final Object parent) {
			return (Object[]) (parent);
		}

		@Override
		public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {}
	}

	void actionClearView() {

		// remove all tours
		_rawDataMgr.removeAllTours();

		reloadViewer();

		_postSelectionProvider.setSelection(new SelectionDeletedTours());

		// don't throw the selection again
		_postSelectionProvider.clearSelection();
	}

	void actionMergeTours(final TourData mergeFromTour, final TourData mergeIntoTour) {

		// check if the tour editor contains a modified tour
		if (TourManager.isTourEditorModified()) {
			return;
		}

		// backup data
		final Long backupMergeSourceTourId = mergeIntoTour.getMergeSourceTourId();
		final Long backupMergeTargetTourId = mergeIntoTour.getMergeTargetTourId();

		// set tour data and tour id from which the tour is merged
		mergeIntoTour.setMergeSourceTourId(mergeFromTour.getTourId());
		mergeIntoTour.setMergeTargetTourId(null);

		// set temp data, this is required by the dialog because the merge from tour could not be saved
		mergeIntoTour.setMergeSourceTour(mergeFromTour);

		if (new DialogMergeTours(Display.getCurrent().getActiveShell(), mergeFromTour, mergeIntoTour).open() != Window.OK) {

			// dialog is canceled, restore modified values

			mergeIntoTour.setMergeSourceTourId(backupMergeSourceTourId);
			mergeIntoTour.setMergeTargetTourId(backupMergeTargetTourId);
		}

		// reset temp tour data
		mergeIntoTour.setMergeSourceTour(null);
	}

	/**
	 * Remove all tours from the raw data view which are selected
	 */
	void actionRemoveTour() {

		final IStructuredSelection selection = ((IStructuredSelection) _tourViewer.getSelection());
		if (selection.size() == 0) {
			return;
		}

		/*
		 * convert selection to array
		 */
		final Object[] selectedItems = selection.toArray();
		final TourData[] selectedTours = new TourData[selection.size()];
		for (int i = 0; i < selectedItems.length; i++) {
			selectedTours[i] = (TourData) selectedItems[i];
		}

		_rawDataMgr.removeTours(selectedTours);

		_postSelectionProvider.clearSelection();

		TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR, null, RawDataView.this);

		// update the table viewer
		reloadViewer();
	}

	void actionSaveTour(final TourPerson person) {

		final ArrayList<TourData> savedTours = new ArrayList<TourData>();

		// get selected tours, this must be outside of the runnable !!!
		final IStructuredSelection selection = ((IStructuredSelection) _tourViewer.getSelection());

		final IRunnableWithProgress saveRunnable = new IRunnableWithProgress() {
			@Override
			public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

				int saveCounter = 0;
				final int selectionSize = selection.size();

				monitor.beginTask(Messages.Tour_Data_SaveTour_Monitor, selectionSize);

				// loop: all selected tours, selected tours can already be saved
				for (final Iterator<?> iter = selection.iterator(); iter.hasNext();) {

					monitor.subTask(NLS.bind(Messages.Tour_Data_SaveTour_MonitorSubtask, ++saveCounter, selectionSize));

					final Object selObject = iter.next();
					if (selObject instanceof TourData) {
						saveTour((TourData) selObject, person, savedTours, false);
					}

					monitor.worked(1);
				}
			}
		};

		try {
			new ProgressMonitorDialog(Display.getCurrent().getActiveShell()).run(true, false, saveRunnable);
		} catch (final InvocationTargetException e) {
			StatusUtil.showStatus(e);
		} catch (final InterruptedException e) {
			StatusUtil.showStatus(e);
		}

		doSaveTourPostActions(savedTours);
	}

	void actionSetupImport() {

		final Shell shell = Display.getDefault().getActiveShell();

		final ImportConfig importConfig = getImportConfig();

		final DialogDeviceImportConfig dialog = new DialogDeviceImportConfig(shell, importConfig, this);

		boolean isOK = false;

		if (dialog.open() == Window.OK) {
			isOK = true;
		}

		final ImportConfig modifiedConfig = dialog.getModifiedConfig();

		if (isOK) {

			// keep none live update values

			importConfig.deviceImportLaunchers.clear();
			importConfig.deviceImportLaunchers.addAll(modifiedConfig.deviceImportLaunchers);

			updateModel_ImportConfig_LiveUpdate(dialog, false);
			updateModel_ImportConfig_AndSave(dialog);

			updateDeviceState();

			setupThread_WatchDeviceFolder();
		}

		updateUI_Dashboard();
	}

	private void addPartListener() {
		_partListener = new IPartListener2() {

			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == RawDataView.this) {

					saveState();

					// remove all tours
					_rawDataMgr.removeAllTours();

					TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR, null, RawDataView.this);
				}
			}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {
				if (RawDataView.this == partRef.getPart(false)) {
					_isPartVisible = false;
				}
			}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {}

			@Override
			public void partVisible(final IWorkbenchPartReference partRef) {
				if (RawDataView.this == partRef.getPart(false)) {
					_isPartVisible = true;
					if (_isViewerPersonDataDirty || (_newActivePerson != _activePerson)) {

//						// run animation after recreation
//						_isAnimationRun = false;

						reloadViewer();
						updateViewerPersonData();
						_newActivePerson = _activePerson;
						_isViewerPersonDataDirty = false;
					}
				}
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
					if (_isPartVisible) {
						updateViewerPersonData();
					} else {
						// keep new active person until the view is visible
						_newActivePerson = TourbookPlugin.getActivePerson();
					}

				} else if (property.equals(ITourbookPreferences.TOUR_PERSON_LIST_IS_MODIFIED)) {

					_actionSaveTour.resetPeopleList();

				} else if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

					// update tour type in the raw data
					_rawDataMgr.updateTourData_InImportView_FromDb(null);

					_tourViewer.refresh();

				} else if (property.equals(ITourbookPreferences.VIEW_TOOLTIP_IS_MODIFIED)) {

					updateToolTipState();

				} else if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

					_columnManager.saveState(_state);
					_columnManager.clearColumns();
					defineAllColumns();

					_tourViewer = (TableViewer) recreateViewer(_tourViewer);

					updateUI_Dashboard();

				} else if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

					_tourViewer.getTable().setLinesVisible(
							_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

					_tourViewer.refresh();

					/*
					 * the tree must be redrawn because the styled text does not show with the new
					 * color
					 */
					_tourViewer.getTable().redraw();
				}
			}
		};

		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	private void addSelectionListener() {

		_postSelectionListener = new ISelectionListener() {
			@Override
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				if (part == RawDataView.this) {
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

				if (part == RawDataView.this) {
					return;
				}

				if ((eventId == TourEventId.TOUR_CHANGED) && (eventData instanceof TourEvent)) {

					// update modified tours
					final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
					if (modifiedTours != null) {

						// update model
						_rawDataMgr.updateTourDataModel(modifiedTours);

						// update viewer
						_tourViewer.update(modifiedTours.toArray(), null);

						// remove old selection, old selection can have the same tour but with old data
						_postSelectionProvider.clearSelection();
					}

				} else if (eventId == TourEventId.ALL_TOURS_ARE_MODIFIED) {

					// save imported file names
					final HashSet<String> importedFiles = _rawDataMgr.getImportedFiles();
					_state.put(STATE_IMPORTED_FILENAMES, importedFiles.toArray(new String[importedFiles.size()]));

					reimportAllImportFiles(false);

				} else if (eventId == TourEventId.TAG_STRUCTURE_CHANGED) {

					_rawDataMgr.updateTourData_InImportView_FromDb(null);

					reloadViewer();
				}
			}
		};
		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	/**
	 * Thread is not interrupted it could cause SQL exceptions.
	 */
	private void cancelThread_PollingStores() {

		_isStopPollingThread = true;
	}

	/**
	 * Thread is not interrupted it could cause SQL exceptions.
	 */
	private void cancelThread_WatchDeviceFolder() {

		if (_watchFolderThread != null) {

			try {

				if (_watchKey != null) {
					_watchKey.cancel();
					_watchKey = null;
				}

				if (_watcher != null) {
					try {
						_watcher.close();
					} catch (final IOException e) {
						StatusUtil.log(e);
					} finally {
						_watcher = null;
					}
				}

			} catch (final Exception e) {
				StatusUtil.log(e);
			} finally {
				_watchFolderThread = null;
			}
		}
	}

	private void createActions() {

		_actionClearView = new ActionClearView(this);
		_actionEditImportPreferences = new ActionOpenPrefDialog(
				Messages.Import_Data_Action_EditImportPreferences,
				PrefPageImport.ID);
		_actionEditTour = new ActionEditTour(this);
		_actionEditQuick = new ActionEditQuick(this);
		_actionExportTour = new ActionExport(this);
		_actionJoinTours = new ActionJoinTours(this);
		_actionMergeIntoTour = new ActionMergeIntoMenu(this);
		_actionMergeTour = new ActionMergeTour(this);
		_actionModifyColumns = new ActionModifyColumns(this);
		_actionOpenTour = new ActionOpenTour(this);
		_actionOpenMarkerDialog = new ActionOpenMarkerDialog(this, true);
		_actionOpenAdjustAltitudeDialog = new ActionOpenAdjustAltitudeDialog(this);
		_actionReimportSubMenu = new ActionReimportSubMenu(this);
		_actionRemoveTour = new ActionRemoveTour(this);
		_actionRemoveToursWhenClosed = new ActionRemoveToursWhenClosed();
		_actionSaveTour = new ActionSaveTourInDatabase(this, false);
		_actionSaveTourWithPerson = new ActionSaveTourInDatabase(this, true);
		_actionSetupImport = new ActionSetupImport(this);
		_actionSetTourType = new ActionSetTourTypeMenu(this);

		_tagMenuMgr = new TagMenuManager(this, true);
	}

	/**
	 * Create css from the import configurations.
	 * 
	 * @return
	 */
	private String createCSS_Custom() {

		/*
		 * Import background image
		 */
		final ImportConfig importConfig = getImportConfig();
		final int itemSize = importConfig.tileSize;
		final int opacity = importConfig.backgroundOpacity;
		final int animationDuration = importConfig.animationDuration;
		final int crazyFactor = importConfig.animationCrazinessFactor;

		String bgImage = UI.EMPTY_STRING;
		if (opacity > 0) {

			/*
			 * Show image only when it is visible, opacity > 0
			 */

			File webFile;
			try {

				webFile = WEB.getResourceFile(WEB_RESOURCE_TOUR_IMPORT_BG_IMAGE);
				final String webContent = Util.readContentFromFile(webFile.getAbsolutePath());
				final String base64Encoded = Base64.getEncoder().encodeToString(webContent.getBytes());

				bgImage = (CSS_IMPORT_BACKGROUND + "\n")// //$NON-NLS-1$
						+ "{\n" //$NON-NLS-1$
						+ ("	background:				url('data:image/svg+xml;base64," + base64Encoded + "');\n") //$NON-NLS-1$ //$NON-NLS-2$
						+ ("	background-repeat:		no-repeat;\n") //$NON-NLS-1$
						+ ("	background-size: 		contain;\n") //$NON-NLS-1$
						+ ("	background-position: 	center center;\n") //$NON-NLS-1$
						+ ("	opacity:				" + (float) opacity / 100 + ";\n") //$NON-NLS-1$ //$NON-NLS-2$
						+ "}\n"; //$NON-NLS-1$

			} catch (IOException | URISyntaxException e) {
				StatusUtil.log(e);
			}
		}

		String animation = UI.EMPTY_STRING;
		if (_isRunAnimation && animationDuration > 0 && UI.IS_WIN) {

			// run animation only once
			_isRunAnimation = false;

			final double rotateX = Math.random() * 10 * crazyFactor;
			final double rotateY = Math.random() * 10 * crazyFactor;

			animation = ""// //$NON-NLS-1$

					+ ("body\n") //$NON-NLS-1$
					+ ("{\n") //$NON-NLS-1$
					+ ("	animation:					fadeinBody;\n") //$NON-NLS-1$
					+ ("	animation-duration:			" + _nf1.format(animationDuration / 10.0) + "s;\n") //$NON-NLS-1$ //$NON-NLS-2$
					+ ("	animation-timing-function:	ease;\n") //$NON-NLS-1$
					+ ("}\n") //$NON-NLS-1$

					+ ("@keyframes fadeinBody												\n") //$NON-NLS-1$
					+ ("{																	\n") //$NON-NLS-1$

					+ ("	from															\n") //$NON-NLS-1$
					+ ("	{																\n") //$NON-NLS-1$
					+ ("		opacity:				0.0;								\n") //$NON-NLS-1$
					+ ("		background-color:		ButtonFace;							\n") //$NON-NLS-1$
					+ ("		transform:				rotateX(" + (int) rotateX + "deg) rotateY(" + (int) rotateY + "deg);	\n") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					+ ("		xtransform-origin: 		50% 200%;							\n") //$NON-NLS-1$
					+ ("	}																\n") //$NON-NLS-1$
//		transform:				rotateX(-80deg) rotateY(-80deg);
//		transform-origin: 		50% 200%;

					+ ("	to																\n") //$NON-NLS-1$
					+ ("	{																\n") //$NON-NLS-1$
					+ ("		opacity:				0.9;								\n") //$NON-NLS-1$
					+ ("	}																\n") //$NON-NLS-1$
					+ ("}																	\n"); //$NON-NLS-1$
		}

		/*
		 * Tile size
		 */
		final String tileSize = "" // //$NON-NLS-1$
				//
				+ (CSS_IMPORT_TILE + "\n") //$NON-NLS-1$
				+ ("{\n") //$NON-NLS-1$
				+ ("	min-height: " + itemSize + "px;\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("	max-height: " + itemSize + "px;\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("	min-width: " + itemSize + "px;\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("	max-width: " + itemSize + "px;\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("}\n"); //$NON-NLS-1$

		/*
		 * CSS
		 */
		final String customCSS = "" // //$NON-NLS-1$

				+ "<style>\n" // //$NON-NLS-1$
				+ animation
				+ bgImage
				+ tileSize
				+ "</style>\n"; //$NON-NLS-1$

		return customCSS;
	}

	private void createDefaultDeviceImportLauncher() {

		final ImportConfig importConfig = getImportConfig();

		final DeviceImportLauncher defaultLauncher = new DeviceImportLauncher();
		importConfig.deviceImportLaunchers.add(defaultLauncher);

		defaultLauncher.name = Messages.Import_Data_Default_DeviceImportLauncher_Name;
		defaultLauncher.description = Messages.Import_Data_Default_DeviceImportLauncher_Description;
	}

	private String createHTML() {

//		Force Internet Explorer to not use compatibility mode. Internet Explorer believes that websites under
//		several domains (including "ibm.com") require compatibility mode. You may see your web application run
//		normally under "localhost", but then fail when hosted under another domain (e.g.: "ibm.com").
//		Setting "IE=Edge" will force the latest standards mode for the version of Internet Explorer being used.
//		This is supported for Internet Explorer 8 and later. You can also ease your testing efforts by forcing
//		specific versions of Internet Explorer to render using the standards mode of previous versions. This
//		prevents you from exploiting the latest features, but may offer you compatibility and stability. Lookup
//		the online documentation for the "X-UA-Compatible" META tag to find which value is right for you.

		final String html = "" // //$NON-NLS-1$
				+ "<!DOCTYPE html>\n" // ensure that IE is using the newest version and not the quirk mode //$NON-NLS-1$
				+ "<html style='height: 100%; width: 100%; margin: 0px; padding: 0px;'>\n" //$NON-NLS-1$
				+ ("<head>\n" + createHTML_10_Head() + "\n</head>\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("<body>\n" + createHTML_20_Body() + "\n</body>\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ "</html>"; //$NON-NLS-1$

		return html;
	}

	private String createHTML_10_Head() {

		final String html = ""// //$NON-NLS-1$
				+ "	<meta http-equiv='Content-Type' content='text/html; charset=UTF-8' />\n" //$NON-NLS-1$
				+ "	<meta http-equiv='X-UA-Compatible' content='IE=edge' />\n" //$NON-NLS-1$
				+ _cssFonts
				+ _cssFromFile
				+ createCSS_Custom()
				+ "\n"; //$NON-NLS-1$

		return html;
	}

	private String createHTML_20_Body() {

		final StringBuilder sb = new StringBuilder();

		sb.append("<div class='import-container'>\n"); //$NON-NLS-1$
		{
			/*
			 * Very tricky: When a parent has an opacity, a child cannot modify it. Therefore the
			 * different divs with position relative/absolute. It took me some time to
			 * find/implement this tricky but simple solution.
			 */
			sb.append("<div class='import-background'></div>\n"); //$NON-NLS-1$

			sb.append("<div class='import-content'>\n"); //$NON-NLS-1$
			{
				/*
				 * Device Import
				 */
				createHTML_50_DeviceImport_Header(sb);
				createHTML_52_DeviceImport_Tiles(sb);

				/*
				 * Get Tours
				 */
				sb.append("<div class='get-tours-title title'>\n"); //$NON-NLS-1$
				sb.append("	" + Messages.Import_Data_HTML_GetTours + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
				sb.append("</div>\n"); //$NON-NLS-1$

				createHTML_60_GetTours(sb);
			}
			sb.append("</div>\n"); //$NON-NLS-1$
		}
		sb.append("</div>\n"); //$NON-NLS-1$

		return sb.toString();
	}

	private void createHTML_50_DeviceImport_Header(final StringBuilder sb) {

		final String html = "" // //$NON-NLS-1$

				+ "<div class='auto-import-header'>\n" //$NON-NLS-1$
				+ ("	<table><tbody><tr>\n") //$NON-NLS-1$

				// device import
				+ ("		<td class='title'>" + Messages.Import_Data_HTML_DeviceImport + "</td>\n") //$NON-NLS-1$ //$NON-NLS-2$

				// device state icon
				+ ("		<td>\n") //$NON-NLS-1$
				+ ("			<div id='" + DOM_ID_DEVICE_STATE + "' style='padding-left:20px;'>" + createHTML_DeviceState() + "</div>\n") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ ("		</td>\n") //$NON-NLS-1$

				+ "	</tr></tbody></table>\n" // //$NON-NLS-1$
				+ "</div>\n"; //$NON-NLS-1$

		sb.append(html);
	}

	private void createHTML_52_DeviceImport_Tiles(final StringBuilder sb) {

		final ImportConfig importConfig = getImportConfig();

		final int numHorizontalTiles = importConfig.numHorizontalTiles;

		final ArrayList<DeviceImportLauncher> configItems = importConfig.deviceImportLaunchers;
		if (configItems.size() == 0) {

			/*
			 * Make life easier and create a default import launcher.
			 */
			createDefaultDeviceImportLauncher();
		}

		boolean isTrOpen = false;
		int tileIndex = 0;

		sb.append("<table class='import-tiles'><tbody>\n"); //$NON-NLS-1$

		for (int configIndex = 0; configIndex < configItems.size(); configIndex++) {

			final DeviceImportLauncher configItem = configItems.get(configIndex);

			if (configItem.isShowInDashboard) {

				if (tileIndex % numHorizontalTiles == 0) {
					sb.append("<tr>\n"); //$NON-NLS-1$
					isTrOpen = true;
				}

				// enforce equal column width
				sb.append("<td style='width:" + 100 / numHorizontalTiles + "%' class='import-tile'>\n"); //$NON-NLS-1$ //$NON-NLS-2$
				sb.append(createHTML_54_DeviceImport_Tile(configItem));
				sb.append("</td>\n"); //$NON-NLS-1$

				if (tileIndex % numHorizontalTiles == numHorizontalTiles - 1) {
					sb.append("</tr>\n"); //$NON-NLS-1$
					isTrOpen = false;
				}

				tileIndex++;
			}
		}

		if (isTrOpen) {
			sb.append("	</tr>\n"); //$NON-NLS-1$
		}

		sb.append("</tbody></table>\n"); //$NON-NLS-1$
	}

	private String createHTML_54_DeviceImport_Tile(final DeviceImportLauncher importTile) {

		/*
		 * Tooltip
		 */
		final String tooltip = createHTML_TileTooltip(importTile);

		/*
		 * Tile image
		 */
		final Image tileImage = getImportConfigImage(importTile);
		String htmlImage = UI.EMPTY_STRING;
		if (tileImage != null) {

			final byte[] pngImageData = ImageUtils.formatImage(tileImage, SWT.IMAGE_PNG);
			final String base64Encoded = Base64.getEncoder().encodeToString(pngImageData);

			htmlImage = "<img src='data:image/png;base64," + base64Encoded + "'>"; //$NON-NLS-1$ //$NON-NLS-2$
		}

		/*
		 * Tile HTML
		 */
		final String href = HTTP_DUMMY + HREF_DEVICE_IMPORT + HREF_TOKEN + importTile.getId();

		final String html = "" //$NON-NLS-1$

				+ ("<a href='" + href + "' title='" + tooltip + "' class='import-tile'>\n") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ ("	<div class='import-tile-image'>" + htmlImage + "</div>\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("	<div class='import-tile-name'>" + importTile.name + "</div>\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("</a>\n") //$NON-NLS-1$
		;

		return html;
	}

	private void createHTML_60_GetTours(final StringBuilder sb) {

		sb.append("<div class='get-tours-items'>\n"); //$NON-NLS-1$
		sb.append("	<table><tbody><tr>\n"); //$NON-NLS-1$
		{
			createHTML_TileAction(
					sb,
					Messages.Import_Data_HTML_ImportFromFiles_Action,
					Messages.Import_Data_HTML_ImportFromFiles_ActionTooltip,
					(HTTP_DUMMY + HREF_IMPORT_FROM_FILES),
					_imageUrl_ImportFromFile);

			createHTML_TileAction(
					sb,
					Messages.Import_Data_HTML_ReceiveFromSerialPort_ConfiguredAction,
					Messages.Import_Data_HTML_ReceiveFromSerialPort_ConfiguredLink,
					(HTTP_DUMMY + HREF_SERIAL_PORT_CONFIGURED),
					_imageUrl_SerialPort_Configured);

			createHTML_TileAction(
					sb,
					Messages.Import_Data_HTML_ReceiveFromSerialPort_DirectlyAction,
					Messages.Import_Data_HTML_ReceiveFromSerialPort_DirectlyLink,
					(HTTP_DUMMY + HREF_SERIAL_PORT_DIRECTLY),
					_imageUrl_SerialPort_Directly);
		}
		sb.append("	</tr></tbody></table>\n"); // //$NON-NLS-1$
		sb.append("</div>\n"); //$NON-NLS-1$

	}

	private String createHTML_BgImage(final String imageUrl) {

		final String htmlImage = "style='" // //$NON-NLS-1$

				+ ("background-image:	url(" + imageUrl + ");") //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ "'"; //$NON-NLS-1$

		return htmlImage;
	}

	private String createHTML_DeviceState() {

		final ImportConfig importConfig = getImportConfig();
		final int numDeviceFiles = importConfig.numDeviceFiles;
		final String deviceOSFolder = importConfig.getDeviceOSFolder();
		final boolean isDeviceFolderOK = isOSFolderValid(deviceOSFolder);

		final StringBuilder sb = new StringBuilder();

		boolean isFolderOK = true;

		/*
		 * Backup folder
		 */
		final boolean isCreateBackup = importConfig.isCreateBackup;
		if (isCreateBackup) {

			final String htmlBackupFolder = importConfig.backupFolder.replace(//
					UI.SYMBOL_BACKSLASH,
					UI.SYMBOL_HTML_BACKSLASH);

			// check OS folder
			final String backupOSFolder = importConfig.getBackupOSFolder();
			final boolean isBackupFolderOK = isOSFolderValid(backupOSFolder);
			isFolderOK &= isBackupFolderOK;

			final String folderTitle = Messages.Import_Data_HTML_BackupFolder;

			/*
			 * Show back folder info only when device folder is OK because they are related
			 * together.
			 */
			final String folderInfo = isDeviceFolderOK //
					? NLS.bind(
							Messages.Import_Data_HTML_NotBackedUpFiles,
							importConfig.notBackedUpFiles.size(),
							numDeviceFiles) //
					: null;

			createHTML_FolderState(//
					sb,
					htmlBackupFolder,
					isBackupFolderOK,
					UI.EMPTY_STRING,
					folderTitle,
					folderInfo);
		}

		/*
		 * Device folder
		 */
		final String htmlDeviceFolder = importConfig.deviceFolder.replace(//
				UI.SYMBOL_BACKSLASH,
				UI.SYMBOL_HTML_BACKSLASH);

		final String folderTitle = Messages.Import_Data_HTML_DeviceFolder;
		final String folderInfo = NLS.bind(Messages.Import_Data_HTML_NotImportedFiles,//
				importConfig.notImportedFiles.size(),
				numDeviceFiles);

		final String paddingTop = importConfig.isCreateBackup ? "padding-top:10px;" : UI.EMPTY_STRING; //$NON-NLS-1$

		createHTML_FolderState(//
				sb,
				htmlDeviceFolder,
				isDeviceFolderOK,
				paddingTop,
				folderTitle,
				folderInfo);

		isFolderOK &= isDeviceFolderOK;

		// create html list with not imported files
		final ArrayList<OSFile> notImportedFiles = importConfig.notImportedFiles;
		final int numNotImported = notImportedFiles.size();
		if (numNotImported > 0) {
			createHTML_NotImportedFiles(sb, notImportedFiles);
		}

		final String htmlTooltip = sb.toString();

		/*
		 * Different html
		 */
		final String hrefAction = HTTP_DUMMY + HREF_SETUP_DEVICE_IMPORT;
		final String imageUrl = isFolderOK ? _imageUrl_DeviceFolder_OK : _imageUrl_DeviceFolder_NotAvailable;
		final String stateImage = createHTML_BgImage(imageUrl);
		final String stateIconValue = isDeviceFolderOK ? Integer.toString(numNotImported) : UI.EMPTY_STRING;

		/*
		 * Show overflow scrollbar ONLY when more than 10 entries are available because it looks
		 * ugly.
		 */
		final String cssOverflow = numNotImported > 10 ? "style='overflow-y: scroll;'" : UI.EMPTY_STRING; //$NON-NLS-1$

		final String html = ""// //$NON-NLS-1$

				+ "<a class='importState'" // //$NON-NLS-1$
				+ (" href='" + hrefAction + "'") //$NON-NLS-1$ //$NON-NLS-2$
				+ ">" //$NON-NLS-1$

				+ ("<div class='stateIcon' " + stateImage + ">") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("   <div class='stateIconValue'>" + stateIconValue + "</div>") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("</div>") //$NON-NLS-1$
				+ ("<div class='stateTooltip' " + cssOverflow + ">" + htmlTooltip + "</div>") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

				+ "</a>"; //$NON-NLS-1$

//		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//				+ ("createHTML_DeviceState\n\nhtmlDeviceState:\n" + htmlActionImportConfig + "\n"));
//		// TODO remove SYSTEM.OUT.PRINTLN

		return html;
	}

	private void createHTML_FolderState(final StringBuilder sb,
										final String folderLocation,
										final boolean isOSFolderValid,
										final String style,
										final String folderTitle,
										final String folderInfo) {

		String htmlErrorState;
		String htmlFolderInfo;

		if (isOSFolderValid) {

			htmlErrorState = UI.EMPTY_STRING;
			htmlFolderInfo = folderInfo == null //
					? UI.EMPTY_STRING
					: "<span class='folderInfo'>" + folderInfo + "</span>"; //$NON-NLS-1$ //$NON-NLS-2$

		} else {

			htmlErrorState = "<div class='folderError'>" + Messages.Import_Data_HTML_FolderIsNotAvailable + "</div>"; //$NON-NLS-1$ //$NON-NLS-2$
			htmlFolderInfo = UI.EMPTY_STRING;
		}

		final String imageUrl = isOSFolderValid ? _imageUrl_AppStateOK : _imageUrl_AppStateError;
		final String folderStateIcon = "<img src='" //$NON-NLS-1$
				+ imageUrl
				+ "' style='padding-left:5px; vertical-align:text-bottom;'>"; //$NON-NLS-1$

		sb.append("<div class='folderContainer' style='" + style + "'>"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("<span class='folderTitle'>" + folderTitle + "</span>"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(htmlFolderInfo);
		sb.append("<div class='folderLocation'>" + folderLocation + folderStateIcon + "</div>"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(htmlErrorState);
		sb.append("</div>"); //$NON-NLS-1$
	}

	private void createHTML_NotImportedFiles(final StringBuilder sb, final ArrayList<OSFile> notImportedFiles) {

		sb.append("<table class='deviceList'><tbody>"); //$NON-NLS-1$

		for (final OSFile deviceFile : notImportedFiles) {

			sb.append("<tr>"); //$NON-NLS-1$

			sb.append("<td class='column name'>"); //$NON-NLS-1$
			sb.append(deviceFile.fileName);
			sb.append("</td>"); //$NON-NLS-1$

			sb.append("<td class='right column'>"); //$NON-NLS-1$
			sb.append(deviceFile.size);
			sb.append("</td>"); //$NON-NLS-1$

			sb.append("<td class='right column'>"); //$NON-NLS-1$
			sb.append(_dateFormatter.format(deviceFile.modifiedTime));
			sb.append("</td>"); //$NON-NLS-1$

			sb.append("<td class='right'>"); //$NON-NLS-1$
			sb.append(_timeFormatter.format(deviceFile.modifiedTime));
			sb.append("</td>"); //$NON-NLS-1$

			sb.append("</tr>"); //$NON-NLS-1$
		}

		sb.append("</tbody></table>"); //$NON-NLS-1$
	}

	private void createHTML_TileAction(	final StringBuilder sb,
										final String name,
										final String tooltip,
										final String href,
										final String imageUrl) {

		final String htmlImage = "style='" // //$NON-NLS-1$

				+ ("background-image:	url(" + imageUrl + ");\n") //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ "'"; //$NON-NLS-1$

		final String html = "" //$NON-NLS-1$

				+ ("<td>") //$NON-NLS-1$
				+ ("<a href='" + href + "' title='" + tooltip + "' class='import-tile'>\n") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ ("	<div class='import-tile-image action-button' " + htmlImage + "></div>\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("	<div class='import-tile-name'>" + name + "</div>\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("</a>\n") //$NON-NLS-1$
				+ ("</td>") //$NON-NLS-1$
		;

		sb.append(html);
	}

	private String createHTML_TileTooltip(final DeviceImportLauncher importTile) {

		boolean isTextAdded = false;

		final StringBuilder sb = new StringBuilder();

		final String tileName = importTile.name.trim();
		final String tileDescription = importTile.description.trim();

		final StringBuilder ttText = new StringBuilder();
		final Enum<TourTypeConfig> ttConfig = importTile.tourTypeConfig;

		if (TourTypeConfig.TOUR_TYPE_CONFIG_BY_SPEED.equals(ttConfig)) {

			final ArrayList<SpeedTourType> speedTourTypes = importTile.speedTourTypes;
			boolean isSpeedAdded = false;

			for (final SpeedTourType speedTT : speedTourTypes) {

				if (isSpeedAdded) {
					ttText.append(UI.NEW_LINE);
				}

				final long tourTypeId = speedTT.tourTypeId;
				final double avgSpeed = (speedTT.avgSpeed / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE) + 0.0001;

				ttText.append((int) avgSpeed);
				ttText.append(UI.SPACE);
				ttText.append(UI.UNIT_LABEL_SPEED);
				ttText.append(UI.SPACE2);
				ttText.append(net.tourbook.ui.UI.getInstance().getTourTypeLabel(tourTypeId));

				isSpeedAdded = true;
			}

		} else if (TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL.equals(ttConfig)) {

			final TourType oneTourType = importTile.oneTourType;
			if (oneTourType != null) {

				final String ttName = oneTourType.getName();

				// show this text only when the name is different
				if (!tileName.equals(ttName)) {
					ttText.append(ttName);
				}
			}
		}

		// tour type name
		if (tileName.length() > 0) {

			sb.append(tileName);
			isTextAdded = true;
		}

		// tour type description
		if (tileDescription.length() > 0) {

			if (isTextAdded) {
				sb.append(UI.NEW_LINE2);
			}

			sb.append(tileDescription);
			isTextAdded = true;
		}

		// tour type text
		if (ttText.length() > 0) {

			if (isTextAdded) {
				sb.append(UI.NEW_LINE2);
			}

			sb.append(ttText);
		}

		return sb.toString();
	}

	@Override
	public void createPartControl(final Composite parent) {

		initUI(parent);

		// define all columns
		_columnManager = new ColumnManager(this, _state);
		defineAllColumns();

		createUI(parent);
		createActions();

		fillToolbar();

		addPartListener();
		addSelectionListener();
		addPrefListener();
		addTourEventListener();

		// set this view part as selection provider
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

		_activePerson = TourbookPlugin.getActivePerson();

		enableActions();
		restoreState();

		updateUI_TopPage();
	}

	private void createResources_Image() {

		_imageDescDatabase = TourbookPlugin.getImageDescriptor(Messages.Image__database);
		_imageDescDatabaseOtherPerson = TourbookPlugin.getImageDescriptor(Messages.Image__database_other_person);
		_imageDescDatabaseAssignMergedTour = TourbookPlugin.getImageDescriptor(Messages.Image__assignMergedTour);
		_imageDescDatabasePlaceholder = TourbookPlugin.getImageDescriptor(Messages.Image__icon_placeholder);
		_imageDescDelete = TourbookPlugin.getImageDescriptor(Messages.Image__delete);

		try {

			final Display display = Display.getCurrent();

			_imageDatabase = (Image) _imageDescDatabase.createResource(display);
			_imageDatabaseOtherPerson = (Image) _imageDescDatabaseOtherPerson.createResource(display);
			_imageDatabaseAssignMergedTour = (Image) _imageDescDatabaseAssignMergedTour.createResource(display);
			_imageDatabasePlaceholder = (Image) _imageDescDatabasePlaceholder.createResource(display);
			_imageDelete = (Image) _imageDescDelete.createResource(display);

		} catch (final DeviceResourceException e) {
			StatusUtil.log(e);
		}

	}

	private void createResources_Web() {

		try {

			/*
			 * Font css must be on the top
			 */
			final File fontFile = WEB.getResourceFile(WEB_RESOURCE_TITLE_FONT);

			final Path path = Paths.get(fontFile.getAbsolutePath());
			final byte[] data = Files.readAllBytes(path);
			final String base64Encoded = Base64.getEncoder().encodeToString(data);

			_cssFonts = "<style>\n" // //$NON-NLS-1$

					+ ("@font-face\n") //$NON-NLS-1$
					+ ("{\n") //$NON-NLS-1$
//					+ ("	font-family:	'NothingYouCouldDo';\n") //$NON-NLS-1$
					+ ("	font-family:	'Nunito-Bold';\n") //$NON-NLS-1$
					+ ("	font-weight:	700;\n") //$NON-NLS-1$
					+ ("	font-style:		bold;\n") //$NON-NLS-1$
					+ ("	src:			url(data:font/truetype;charset=utf-8;base64," + base64Encoded + ") format('truetype');") //$NON-NLS-1$ //$NON-NLS-2$
					+ ("}\n") //$NON-NLS-1$

					+ "</style>\n"; //$NON-NLS-1$

			/*
			 * Webpage css
			 */
			File webFile = WEB.getResourceFile(WEB_RESOURCE_TOUR_IMPORT_CSS);
			final String css = Util.readContentFromFile(webFile.getAbsolutePath());

			String css3 = UI.EMPTY_STRING;

			/*
			 * Eclipse 3.8 with Linux has only a limited css3 support -> DISABLED
			 */
			if (UI.IS_WIN) {

				webFile = WEB.getResourceFile(WEB_RESOURCE_TOUR_IMPORT_CSS3);
				css3 = Util.readContentFromFile(webFile.getAbsolutePath());
			}

			_cssFromFile = ""// //$NON-NLS-1$
					+ "<style>\n" //$NON-NLS-1$
					+ css
					+ css3
					+ "</style>\n"; //$NON-NLS-1$

			/*
			 * Image urls
			 */
			_imageUrl_AppStateOK = net.tourbook.ui.UI.getIconUrl(Messages.Image__App_State_OK);
			_imageUrl_AppStateError = net.tourbook.ui.UI.getIconUrl(Messages.Image__App_State_Error);
			_imageUrl_ImportFromFile = net.tourbook.ui.UI.getIconUrl(Messages.Image__RawData_Import);
			_imageUrl_SerialPort_Configured = net.tourbook.ui.UI.getIconUrl(Messages.Image__RawData_Transfer);
			_imageUrl_SerialPort_Directly = net.tourbook.ui.UI.getIconUrl(Messages.Image__RawData_TransferDirect);
			_imageUrl_DeviceFolder_OK = net.tourbook.ui.UI.getIconUrl(Messages.Image__RawData_DeviceFolder);
			_imageUrl_DeviceFolder_NotAvailable = net.tourbook.ui.UI
					.getIconUrl(Messages.Image__RawData_DeviceFolder_NotDefined);

		} catch (final IOException | URISyntaxException e) {
			StatusUtil.showStatus(e);
		}
	}

	private void createUI(final Composite parent) {

		_topPageBook = new PageBook(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_topPageBook);

		_topPage_Startup = createUI_10_Page_Startup(_topPageBook);
		_topPage_Dashboard = createUI_20_Page_Dashboard(_topPageBook);
		_topPage_ImportViewer = createUI_90_Page_TourViewer(_topPageBook);

		_topPageBook.showPage(_topPage_Startup);
	}

	/**
	 * This page is displayed until the first page of the browser is loaded.
	 * 
	 * @param parent
	 * @return
	 */
	private Composite createUI_10_Page_Startup(final Composite parent) {

		return new Composite(parent, SWT.NONE);
	}

	private Composite createUI_20_Page_Dashboard(final Composite parent) {

		_dashboard_PageBook = new PageBook(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_dashboard_PageBook);

		_dashboardPage_NoBrowser = new Composite(_dashboard_PageBook, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_dashboardPage_NoBrowser);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(_dashboardPage_NoBrowser);
		_dashboardPage_NoBrowser.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		{
			_txtNoBrowser = new Text(_dashboardPage_NoBrowser, SWT.WRAP | SWT.READ_ONLY);
			GridDataFactory.fillDefaults()//
					.grab(true, true)
					.align(SWT.FILL, SWT.BEGINNING)
					.applyTo(_txtNoBrowser);
			_txtNoBrowser.setText(Messages.UI_Label_BrowserCannotBeCreated);
		}

		_dashboardPage_WithBrowser = new Composite(_dashboard_PageBook, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(_dashboardPage_WithBrowser);
		{
			createUI_22_Browser(_dashboardPage_WithBrowser);
		}

		return _dashboard_PageBook;
	}

	private void createUI_22_Browser(final Composite parent) {

		try {

			try {

				// use default browser
				_browser = new Browser(parent, SWT.NONE);

				// initial setup
				_browser.setRedraw(false);

			} catch (final Exception e) {

				/*
				 * Use mozilla browser, this is necessary for Linux when default browser fails
				 * however the XULrunner needs to be installed.
				 */
				_browser = new Browser(parent, SWT.MOZILLA);
			}

			GridDataFactory.fillDefaults().grab(true, true).applyTo(_browser);

			_browser.addLocationListener(new LocationAdapter() {
				@Override
				public void changing(final LocationEvent event) {
					onBrowser_LocationChanging(event);
				}
			});

			_browser.addProgressListener(new ProgressAdapter() {
				@Override
				public void completed(final ProgressEvent event) {
					onBrowser_Completed(event);
				}
			});

		} catch (final SWTError e) {

			_txtNoBrowser.setText(NLS.bind(Messages.UI_Label_BrowserCannotBeCreated_Error, e.getMessage()));
		}
	}

	private Composite createUI_90_Page_TourViewer(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(container);
		{
			createUI_92_TourViewer(container);
		}

		return container;
	}

	/**
	 * @param parent
	 */
	private void createUI_92_TourViewer(final Composite parent) {

		// table
		final Table table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);

		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.setHeaderVisible(true);
		table.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

		_tourViewer = new TableViewer(table);
		_columnManager.createColumns(_tourViewer);

		// table viewer
		_tourViewer.setContentProvider(new TourDataContentProvider());
		_tourViewer.setSorter(new DeviceImportSorter());

		_tourViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(final DoubleClickEvent event) {

				final Object firstElement = ((IStructuredSelection) _tourViewer.getSelection()).getFirstElement();

				if ((firstElement != null) && (firstElement instanceof TourData)) {
					TourManager.getInstance().tourDoubleClickAction(RawDataView.this, _tourDoubleClickState);
				}
			}
		});

		_tourViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				fireSelectedTour();
			}
		});

		// set tour info tooltip provider
		_tourInfoToolTip = new TableViewerTourInfoToolTip(_tourViewer);

		createUI_94_ContextMenu();
	}

	/**
	 * create the views context menu
	 */
	private void createUI_94_ContextMenu() {

		final Table table = (Table) _tourViewer.getControl();

		final MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(final IMenuManager manager) {
				fillContextMenu(manager);
			}
		});

		final Menu tableContextMenu = menuMgr.createContextMenu(table);
		tableContextMenu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuHidden(final MenuEvent e) {
				_tagMenuMgr.onHideMenu();
			}

			@Override
			public void menuShown(final MenuEvent menuEvent) {
				_tagMenuMgr.onShowMenu(menuEvent, table, Display.getCurrent().getCursorLocation(), _tourInfoToolTip);
			}
		});

		getSite().registerContextMenu(menuMgr, _tourViewer);

		_columnManager.createHeaderContextMenu(table, tableContextMenu);
	}

	private void deactivateBackgroundTask() {

		// deactivate background task

		_canPollImportFiles.set(false);

		cancelThread_WatchDeviceFolder();
	}

	/**
	 * Defines all columns for the table viewer in the column manager, the sequenze defines the
	 * default columns
	 * 
	 * @param parent
	 */
	private void defineAllColumns() {

		defineColumnDatabase();
		defineColumnDate();
		defineColumnTime();
		defineColumnTourType();
		defineColumnTourTypeText();
		defineColumnRecordingTime();
		defineColumnDrivingTime();
		defineColumnCalories();
		defineColumnDistance();
		defineColumnAvgSpeed();
		defineColumnAvgPace();
		defineColumnAltitudeUp();
		defineColumnAltitudeDown();
		defineColumnWeatherClouds();
		defineColumnTitle();
		defineColumnTags();
		defineColumnDeviceName();
		defineColumnDeviceProfile();
		defineColumnMarker();
		defineColumnTimeInterval();
		defineColumnImportFileName();
		defineColumnImportFilePath();
	}

	/**
	 * column: altitude down
	 */
	private void defineColumnAltitudeDown() {

		final ColumnDefinition colDef = TableColumnFactory.ALTITUDE_DOWN_SUMMARIZED_BORDER.createColumn(
				_columnManager,
				_pc);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final int tourAltDown = ((TourData) cell.getElement()).getTourAltDown();
				if (tourAltDown != 0) {
					cell.setText(Long.toString((long) (-tourAltDown / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE)));
				}
			}
		});
	}

	/**
	 * column: altitude up
	 */
	private void defineColumnAltitudeUp() {

		final ColumnDefinition colDef = TableColumnFactory.ALTITUDE_UP_SUMMARIZED_BORDER.createColumn(
				_columnManager,
				_pc);

		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final int tourAltUp = ((TourData) cell.getElement()).getTourAltUp();
				if (tourAltUp != 0) {
					cell.setText(Long.toString((long) (tourAltUp / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE)));
				}
			}
		});
	}

	/**
	 * column: average pace
	 */
	private void defineColumnAvgPace() {

		final ColumnDefinition colDef = TableColumnFactory.AVG_PACE.createColumn(_columnManager, _pc);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourData tourData = (TourData) cell.getElement();

				final float tourDistance = tourData.getTourDistance();
				final long drivingTime = tourData.getTourDrivingTime();

				final float pace = tourDistance == 0 ? //
						0
						: drivingTime * 1000 / tourDistance * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

				cell.setText(net.tourbook.ui.UI.format_mm_ss((long) pace));
			}
		});
	}

	/**
	 * column: avg speed
	 */
	private void defineColumnAvgSpeed() {

		final ColumnDefinition colDef = TableColumnFactory.AVG_SPEED.createColumn(_columnManager, _pc);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourData tourData = ((TourData) cell.getElement());
				final float tourDistance = tourData.getTourDistance();
				final long drivingTime = tourData.getTourDrivingTime();

				double speed = 0;

				if (drivingTime != 0) {
					speed = tourDistance / drivingTime * 3.6 / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;
				}

				cell.setText(speed == 0.0 ? UI.EMPTY_STRING : _nf1.format(speed));
			}
		});
	}

	/**
	 * column: calories (cal)
	 */
	private void defineColumnCalories() {

		final ColumnDefinition colDef = TableColumnFactory.CALORIES.createColumn(_columnManager, _pc);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final int tourCalories = ((TourData) cell.getElement()).getCalories();

				cell.setText(tourCalories == 0 ? UI.EMPTY_STRING : Integer.toString(tourCalories));
			}
		});
	}

	/**
	 * column: database indicator
	 */
	private void defineColumnDatabase() {

		final ColumnDefinition colDef = TableColumnFactory.DB_STATUS.createColumn(_columnManager, _pc);

		colDef.setIsDefaultColumn();
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				// show the database indicator for the person who owns the tour
				cell.setImage(getDbImage((TourData) cell.getElement()));
			}
		});
	}

	/**
	 * column: date
	 */
	private void defineColumnDate() {

		final ColumnDefinition colDef = TableColumnFactory.TOUR_DATE.createColumn(_columnManager, _pc);

		colDef.setIsDefaultColumn();
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

			@Override
			public Long getTourId(final ViewerCell cell) {

				if (_isToolTipInDate == false) {
					return null;
				}

				return ((TourData) cell.getElement()).getTourId();
			}

			@Override
			public void update(final ViewerCell cell) {

				final TourData tourData = (TourData) cell.getElement();

				cell.setText(_dateFormatter.format(tourData.getTourStartTimeMS()));
			}
		});

		// sort column
		colDef.setColumnSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((DeviceImportSorter) _tourViewer.getSorter()).doSort(COLUMN_DATE);
				_tourViewer.refresh();
			}
		});
	}

	/**
	 * column: device name
	 */
	private void defineColumnDeviceName() {

		final ColumnDefinition colDef = TableColumnFactory.DEVICE_NAME.createColumn(_columnManager, _pc);

		colDef.setColumnSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((DeviceImportSorter) _tourViewer.getSorter()).doSort(COLUMN_DATA_FORMAT);
				_tourViewer.refresh();
			}
		});
	}

	/**
	 * column: device profile
	 */
	private void defineColumnDeviceProfile() {

		TableColumnFactory.DEVICE_PROFILE.createColumn(_columnManager, _pc);
	}

	/**
	 * column: distance (km/mile)
	 */
	private void defineColumnDistance() {

		final ColumnDefinition colDef = TableColumnFactory.DISTANCE.createColumn(_columnManager, _pc);

		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final float tourDistance = ((TourData) cell.getElement()).getTourDistance();
				if (tourDistance == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(_nf3.format(tourDistance / 1000 / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE));
				}
			}
		});
	}

	/**
	 * column: driving time
	 */
	private void defineColumnDrivingTime() {

		final ColumnDefinition colDef = TableColumnFactory.DRIVING_TIME.createColumn(_columnManager, _pc);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final int drivingTime = (int) ((TourData) cell.getElement()).getTourDrivingTime();

				if (drivingTime != 0) {
					_calendar
							.set(0, 0, 0, drivingTime / 3600, ((drivingTime % 3600) / 60), ((drivingTime % 3600) % 60));

					cell.setText(_durationFormatter.format(_calendar.getTime()));
				}
			}
		});
	}

	/**
	 * column: import file name
	 */
	private void defineColumnImportFileName() {

		final ColumnDefinition colDef = TableColumnFactory.IMPORT_FILE_NAME.createColumn(_columnManager, _pc);

		colDef.setColumnSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((DeviceImportSorter) _tourViewer.getSorter()).doSort(COLUMN_FILE_NAME);
				_tourViewer.refresh();
			}
		});
	}

	/**
	 * column: import file path
	 */
	private void defineColumnImportFilePath() {
		TableColumnFactory.IMPORT_FILE_PATH.createColumn(_columnManager, _pc);
	}

	/**
	 * column: markers
	 */
	private void defineColumnMarker() {

		final ColumnDefinition colDef = TableColumnFactory.TOUR_MARKERS.createColumn(_columnManager, _pc);

		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourData tourData = (TourData) cell.getElement();

				final Set<TourMarker> tourMarker = tourData.getTourMarkers();
				final Set<TourWayPoint> wayPoints = tourData.getTourWayPoints();

				if (tourMarker == null && wayPoints == null) {
					cell.setText(UI.EMPTY_STRING);
				} else {

					int size = 0;
					if (tourMarker != null) {
						size = tourMarker.size();
					}
					if (wayPoints != null) {
						size += wayPoints.size();
					}
					cell.setText(size == 0 ? UI.EMPTY_STRING : Integer.toString(size));
				}
			}
		});
	}

	/**
	 * column: recording time
	 */
	private void defineColumnRecordingTime() {

		final ColumnDefinition colDef = TableColumnFactory.RECORDING_TIME.createColumn(_columnManager, _pc);

		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final int recordingTime = (int) ((TourData) cell.getElement()).getTourRecordingTime();

				if (recordingTime != 0) {
					_calendar.set(
							0,
							0,
							0,
							recordingTime / 3600,
							((recordingTime % 3600) / 60),
							((recordingTime % 3600) % 60));

					cell.setText(_durationFormatter.format(_calendar.getTime()));
				}
			}
		});
	}

	/**
	 * column: tags
	 */
	private void defineColumnTags() {

		final ColumnDefinition colDef = TableColumnFactory.TOUR_TAGS.createColumn(_columnManager, _pc);

		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

			@Override
			public Long getTourId(final ViewerCell cell) {

				if (_isToolTipInTags == false) {
					return null;
				}

				return ((TourData) cell.getElement()).getTourId();
			}

			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final TourData tourData = (TourData) element;

				final Set<TourTag> tourTags = tourData.getTourTags();

				if (tourTags.size() == 0) {

					// the tags could have been removed, set empty field

					cell.setText(UI.EMPTY_STRING);

				} else {

					// convert the tags into a list of tag ids

					cell.setText(TourDatabase.getTagNames(tourTags));
				}
			}
		});
	}

	/**
	 * column: time
	 */
	private void defineColumnTime() {

		final ColumnDefinition colDef = TableColumnFactory.TOUR_START_TIME.createColumn(_columnManager, _pc);

		colDef.setIsDefaultColumn();
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

			@Override
			public Long getTourId(final ViewerCell cell) {

				if (_isToolTipInTime == false) {
					return null;
				}

				return ((TourData) cell.getElement()).getTourId();
			}

			@Override
			public void update(final ViewerCell cell) {

				final TourData tourData = (TourData) cell.getElement();

				cell.setText(_timeFormatter.format(tourData.getTourStartTimeMS()));
			}
		});

		// sort column
		colDef.setColumnSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((DeviceImportSorter) _tourViewer.getSorter()).doSort(COLUMN_DATE);
				_tourViewer.refresh();
			}
		});
	}

	/**
	 * column: time interval
	 */
	private void defineColumnTimeInterval() {

		TableColumnFactory.TIME_INTERVAL.createColumn(_columnManager, _pc);
	}

	/**
	 * column: tour title
	 */
	private void defineColumnTitle() {

		final ColumnDefinition colDef = TableColumnFactory.TOUR_TITLE.createColumn(_columnManager, _pc);

		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

			@Override
			public Long getTourId(final ViewerCell cell) {

				if (_isToolTipInTitle == false) {
					return null;
				}

				return ((TourData) cell.getElement()).getTourId();
			}

			@Override
			public void update(final ViewerCell cell) {
				final TourData tourData = (TourData) cell.getElement();
				cell.setText(tourData.getTourTitle());
			}
		});
		colDef.setColumnSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((DeviceImportSorter) _tourViewer.getSorter()).doSort(COLUMN_TITLE);
				_tourViewer.refresh();
			}
		});
	}

	/**
	 * column: tour type image
	 */
	private void defineColumnTourType() {

		final ColumnDefinition colDef = TableColumnFactory.TOUR_TYPE.createColumn(_columnManager, _pc);

		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final net.tourbook.ui.UI ui = net.tourbook.ui.UI.getInstance();

				final TourType tourType = ((TourData) cell.getElement()).getTourType();

				if (tourType == null) {
					cell.setImage(ui.getTourTypeImage(TourDatabase.ENTITY_IS_NOT_SAVED));
				} else {

					final long tourTypeId = tourType.getTypeId();
					final Image tourTypeImage = ui.getTourTypeImage(tourTypeId);

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
	private void defineColumnTourTypeText() {

		final ColumnDefinition colDef = TableColumnFactory.TOUR_TYPE_TEXT.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourType tourType = ((TourData) cell.getElement()).getTourType();
				if (tourType == null) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(tourType.getName());
				}
			}
		});
	}

	/**
	 * column: clouds
	 */
	private void defineColumnWeatherClouds() {

		final ColumnDefinition colDef = TableColumnFactory.CLOUDS.createColumn(_columnManager, _pc);

		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {

			@Override
			public void update(final ViewerCell cell) {

				final String weatherCloudId = ((TourData) cell.getElement()).getWeatherClouds();
				if (weatherCloudId == null) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					final Image img = UI.IMAGE_REGISTRY.get(weatherCloudId);
					if (img != null) {
						cell.setImage(img);
					} else {
						cell.setText(weatherCloudId);
					}
				}
			}
		});
	}

	@Override
	public void dispose() {

		cancelThread_WatchDeviceFolder();
		cancelThread_PollingStores();
		DeviceImportManager.getInstance().reset();

		Util.disposeResource(_imageDatabase);
		Util.disposeResource(_imageDatabaseOtherPerson);
		Util.disposeResource(_imageDatabaseAssignMergedTour);
		Util.disposeResource(_imageDatabasePlaceholder);
		Util.disposeResource(_imageDelete);

		// don't throw the selection again
		_postSelectionProvider.clearSelection();

		getViewSite().getPage().removePartListener(_partListener);
		getSite().getPage().removeSelectionListener(_postSelectionListener);

		TourManager.getInstance().removeTourEventListener(_tourEventListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		disposeConfigImages();

		super.dispose();
	}

	private void disposeConfigImages() {

		for (final Image configImage : _configImages.values()) {

			if (configImage != null) {
				configImage.dispose();
			}
		}

		_configImages.clear();
		_configImageHash.clear();
	}

	public void doLiveUpdate(final DialogDeviceImportConfig dialogImportConfig) {

		updateModel_ImportConfig_LiveUpdate(dialogImportConfig, true);

		updateUI_Dashboard();
	}

	/**
	 * After tours are saved, the internal structures and ui viewers must be updated
	 * 
	 * @param savedTours
	 *            contains the saved {@link TourData}
	 */
	private void doSaveTourPostActions(final ArrayList<TourData> savedTours) {

		// update viewer, fire selection event
		if (savedTours.size() == 0) {
			return;
		}

		final ArrayList<Long> savedToursIds = new ArrayList<Long>();

		// update raw data map with the saved tour data
		final HashMap<Long, TourData> rawDataMap = _rawDataMgr.getImportedTours();
		for (final TourData tourData : savedTours) {

			final Long tourId = tourData.getTourId();

			rawDataMap.put(tourId, tourData);
			savedToursIds.add(tourId);
		}

		/*
		 * the selection provider can contain old tour data which conflicts with the tour data in
		 * the tour data editor
		 */
		_postSelectionProvider.clearSelection();

		// update import viewer
		reloadViewer();

		enableActions();

		/*
		 * notify all views, it is not checked if the tour data editor is dirty because newly saved
		 * tours can not be modified in the tour data editor
		 */
		TourManager.fireEventWithCustomData(TourEventId.UPDATE_UI, new SelectionTourIds(savedToursIds), this);
	}

	private void enableActions() {

		final Object[] rawData = _rawDataMgr.getImportedTours().values().toArray();
		final boolean isTourAvailable = rawData.length > 0;

		final StructuredSelection selection = (StructuredSelection) _tourViewer.getSelection();

		int savedTours = 0;
		int unsavedTours = 0;
		int selectedTours = 0;

		// contains all tours which are selected and not deleted
		int selectedNotDeleteTours = 0;

		TourData firstSavedTour = null;
		TourData firstValidTour = null;

		for (final Iterator<?> iter = selection.iterator(); iter.hasNext();) {
			final Object treeItem = iter.next();
			if (treeItem instanceof TourData) {

				selectedTours++;

				final TourData tourData = (TourData) treeItem;
				if (tourData.getTourPerson() == null) {

					// tour is not saved

					if (tourData.isTourDeleted == false) {

						// tour is not deleted, deleted tours are ignored

						unsavedTours++;
						selectedNotDeleteTours++;
					}

				} else {

					if (savedTours == 0) {
						firstSavedTour = tourData;
					}

					savedTours++;
					selectedNotDeleteTours++;
				}

				if (selectedNotDeleteTours == 1) {
					firstValidTour = tourData;
				}
			}
		}

		final boolean isSavedTourSelected = savedTours > 0;
		final boolean isOneSavedAndNotDeleteTour = (selectedNotDeleteTours == 1) && (savedTours == 1);

		final boolean isOneSelectedNotDeleteTour = selectedNotDeleteTours == 1;

		// action: save tour with person
		final TourPerson person = TourbookPlugin.getActivePerson();
		if (person != null) {
			_actionSaveTourWithPerson.setText(NLS.bind(
					Messages.import_data_action_save_tour_with_person,
					person.getName()));
			_actionSaveTourWithPerson.setPerson(person);
		}
		_actionSaveTourWithPerson.setEnabled((person != null) && (unsavedTours > 0));

		// action: save tour...
		if (selection.size() == 1) {
			_actionSaveTour.setText(Messages.import_data_action_save_tour_for_person);
		} else {
			_actionSaveTour.setText(Messages.import_data_action_save_tours_for_person);
		}
		_actionSaveTour.setEnabled(unsavedTours > 0);

		// action: merge tour ... into ...
		if (isOneSelectedNotDeleteTour) {

			final StringBuilder sb = new StringBuilder().append(UI.EMPTY_STRING)//
					.append(TourManager.getTourDateShort(firstValidTour))
					.append(UI.DASH_WITH_SPACE)
					.append(TourManager.getTourTimeShort(firstValidTour))
					.append(UI.DASH_WITH_SPACE)
					.append(firstValidTour.getDeviceName());

			_actionMergeIntoTour.setText(NLS.bind(Messages.import_data_action_assignMergedTour, sb.toString()));

		} else {
			// tour cannot be merged, display default text
			_actionMergeIntoTour.setText(Messages.import_data_action_assignMergedTour_default);
		}
		_actionMergeIntoTour.setEnabled(isOneSelectedNotDeleteTour);

		_actionMergeTour.setEnabled(isOneSavedAndNotDeleteTour && (firstSavedTour.getMergeSourceTourId() != null));
		_actionReimportSubMenu.setEnabled(selectedTours > 0);
		_actionRemoveTour.setEnabled(selectedTours > 0);
		_actionExportTour.setEnabled(selectedNotDeleteTours > 0);
		_actionJoinTours.setEnabled(selectedNotDeleteTours > 1);

		_actionEditTour.setEnabled(isOneSavedAndNotDeleteTour);
		_actionEditQuick.setEnabled(isOneSavedAndNotDeleteTour);
		_actionOpenTour.setEnabled(isOneSavedAndNotDeleteTour);
		_actionOpenMarkerDialog.setEnabled(isOneSavedAndNotDeleteTour);
		_actionOpenAdjustAltitudeDialog.setEnabled(isOneSavedAndNotDeleteTour);

		// set double click state
		_tourDoubleClickState.canEditTour = isOneSavedAndNotDeleteTour;
		_tourDoubleClickState.canQuickEditTour = isOneSavedAndNotDeleteTour;
		_tourDoubleClickState.canEditMarker = isOneSavedAndNotDeleteTour;
		_tourDoubleClickState.canAdjustAltitude = isOneSavedAndNotDeleteTour;
		_tourDoubleClickState.canOpenTour = isOneSelectedNotDeleteTour;

		final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();
		_actionSetTourType.setEnabled(isSavedTourSelected && (tourTypes.size() > 0));

//		_actionAddTag.setEnabled(isTourSelected);

		final ArrayList<Long> existingTagIds = new ArrayList<Long>();
		long existingTourTypeId = TourDatabase.ENTITY_IS_NOT_SAVED;
		boolean isOneTour;

		if ((firstSavedTour != null) && (savedTours == 1)) {

			// one tour is selected

			isOneTour = true;

			final TourType tourType = firstSavedTour.getTourType();
			existingTourTypeId = tourType == null ? TourDatabase.ENTITY_IS_NOT_SAVED : tourType.getTypeId();

			final Set<TourTag> existingTags = firstSavedTour.getTourTags();
			if ((existingTags != null) && (existingTags.size() > 0)) {

				// tour contains at least one tag
				for (final TourTag tourTag : existingTags) {
					existingTagIds.add(tourTag.getTagId());
				}
			}
		} else {

			// multiple tours are selected

			isOneTour = false;
		}

		// enable/disable actions for tags/tour types
		_tagMenuMgr.enableTagActions(isSavedTourSelected, isOneTour, existingTagIds);
		TourTypeMenuManager.enableRecentTourTypeActions(isSavedTourSelected, existingTourTypeId);

		/*
		 * Action: Setup import
		 */
		_actionSetupImport.setEnabled(!isTourAvailable);
		_actionClearView.setEnabled(isTourAvailable);
	}

	private void fillContextMenu(final IMenuManager menuMgr) {

		// hide tour info tooltip, this is displayed when the mouse context menu should be created
		_tourInfoToolTip.hide();

		if (TourbookPlugin.getActivePerson() != null) {
			menuMgr.add(_actionSaveTourWithPerson);
		}
		menuMgr.add(_actionSaveTour);
		menuMgr.add(_actionMergeIntoTour);
		menuMgr.add(_actionJoinTours);

		menuMgr.add(new Separator());
		menuMgr.add(_actionExportTour);
		menuMgr.add(_actionReimportSubMenu);
		menuMgr.add(_actionEditImportPreferences);
		menuMgr.add(_actionRemoveTour);

		menuMgr.add(new Separator());
		menuMgr.add(_actionEditQuick);
		menuMgr.add(_actionEditTour);
		menuMgr.add(_actionOpenMarkerDialog);
		menuMgr.add(_actionOpenAdjustAltitudeDialog);
		menuMgr.add(_actionMergeTour);
		menuMgr.add(_actionOpenTour);

		// tour type actions
		menuMgr.add(new Separator());
		menuMgr.add(_actionSetTourType);
		TourTypeMenuManager.fillMenuWithRecentTourTypes(menuMgr, this, true);

		// tour tag actions
		_tagMenuMgr.fillTagMenu(menuMgr);

		// add standard group which allows other plug-ins to contribute here
		menuMgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

		enableActions();
	}

	private void fillToolbar() {

		/*
		 * fill view toolbar
		 */
		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

		tbm.add(_actionSaveTourWithPerson);
		tbm.add(_actionSaveTour);
		tbm.add(new Separator());

		// place for import and transfer actions
		tbm.add(new GroupMarker("import")); //$NON-NLS-1$
		tbm.add(new Separator());

		tbm.add(_actionClearView);
		tbm.add(_actionSetupImport);

		/*
		 * fill view menu
		 */
		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();

		menuMgr.add(_actionRemoveToursWhenClosed);
		menuMgr.add(_actionEditImportPreferences);

		menuMgr.add(new Separator());
		menuMgr.add(_actionModifyColumns);
	}

	private void fireSelectedTour() {

		final IStructuredSelection selection = (IStructuredSelection) _tourViewer.getSelection();
		final TourData tourData = (TourData) selection.getFirstElement();

		enableActions();

		if (tourData != null) {
			_postSelectionProvider.setSelection(new SelectionTourData(null, tourData));
		}
	}

	@Override
	public ArrayList<TourData> getAllSelectedTours() {

		final TourManager tourManager = TourManager.getInstance();

		// get selected tours
		final IStructuredSelection selectedTours = ((IStructuredSelection) _tourViewer.getSelection());

		final ArrayList<TourData> selectedTourData = new ArrayList<TourData>();

		// loop: all selected tours
		for (final Iterator<?> iter = selectedTours.iterator(); iter.hasNext();) {

			final Object tourItem = iter.next();

			if (tourItem instanceof TourData) {

				final TourData tourData = (TourData) tourItem;

				if (tourData.isTourDeleted) {
					// skip deleted tour
					continue;
				}

				if (tourData.getTourPerson() == null) {

					// tour is not saved
					selectedTourData.add(tourData);

				} else {
					/*
					 * get the data from the database because the tag names could be changed and
					 * this is not reflected in the tours which are displayed in the raw data view
					 */
					final TourData tourDataInDb = tourManager.getTourData(tourData.getTourId());
					if (tourDataInDb != null) {
						selectedTourData.add(tourDataInDb);
					}
				}
			}
		}

		return selectedTourData;
	}

	@Override
	public ColumnManager getColumnManager() {
		return _columnManager;
	}

	Image getDbImage(final TourData tourData) {
		final TourPerson tourPerson = tourData.getTourPerson();
		final long activePersonId = _activePerson == null ? -1 : _activePerson.getPersonId();

		final Image dbImage = tourData.isTourDeleted ? //
				_imageDelete
				: tourData.getMergeTargetTourId() != null ? //
						_imageDatabaseAssignMergedTour
						: tourPerson == null ? _imageDatabasePlaceholder : tourPerson.getPersonId() == activePersonId
								? _imageDatabase
								: _imageDatabaseOtherPerson;
		return dbImage;
	}

	private ImportConfig getImportConfig() {

		return DeviceImportManager.getInstance().getDeviceImportConfig();
	}

	public Image getImportConfigImage(final DeviceImportLauncher importConfig) {

		final int imageWidth = importConfig.imageWidth;

		if (imageWidth == 0) {
			return null;
		}

		final long configId = importConfig.getId();
		Image image = _configImages.get(configId);

		if (isConfigImageValid(image, importConfig)) {
			return image;
		}

		final Display display = _parent.getDisplay();

		final Enum<TourTypeConfig> tourTypeConfig = importConfig.tourTypeConfig;
		final net.tourbook.ui.UI ui = net.tourbook.ui.UI.getInstance();

		if (TourTypeConfig.TOUR_TYPE_CONFIG_BY_SPEED.equals(tourTypeConfig)) {

			final ArrayList<SpeedTourType> speedVertices = importConfig.speedTourTypes;
			final int imageSize = TourType.TOUR_TYPE_IMAGE_SIZE;

			final Image tempImage = new Image(display, imageWidth, imageSize);
			{
				final GC gcImage = new GC(tempImage);
				final Color colorTransparent = new Color(display, TourType.TRANSPARENT_COLOR);
				{
					// fill with transparent color
					gcImage.setBackground(colorTransparent);
					gcImage.fillRectangle(0, 0, imageWidth, imageSize);

					for (int imageIndex = 0; imageIndex < speedVertices.size(); imageIndex++) {

						final SpeedTourType vertex = speedVertices.get(imageIndex);

						final Image ttImage = ui.getTourTypeImage(vertex.tourTypeId);

						gcImage.drawImage(ttImage, //
								0,
								0,
								imageSize,
								imageSize,

								imageSize * imageIndex,
								0,
								imageSize,
								imageSize);
					}
				}
				gcImage.dispose();
				colorTransparent.dispose();

				/*
				 * set transparency
				 */
				final ImageData imageData = tempImage.getImageData();
				imageData.transparentPixel = imageData.getPixel(0, 0);

				image = new Image(display, imageData);
			}
			tempImage.dispose();

		} else if (TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL.equals(tourTypeConfig)) {

			final TourType tourType = importConfig.oneTourType;

			if (tourType != null) {

				// create a copy because the copied image can be disposed
				final Image tempImage = new Image(display, TourType.TOUR_TYPE_IMAGE_SIZE, TourType.TOUR_TYPE_IMAGE_SIZE);
				{

					final GC gcImage = new GC(tempImage);
					final Color colorTransparent = new Color(display, TourType.TRANSPARENT_COLOR);
					{
						// fill with transparent color
						gcImage.setBackground(colorTransparent);
						gcImage.fillRectangle(0, 0, TourType.TOUR_TYPE_IMAGE_SIZE, TourType.TOUR_TYPE_IMAGE_SIZE);

						final Image ttImage = ui.getTourTypeImage(tourType.getTypeId());
						gcImage.drawImage(ttImage, 0, 0);
					}
					gcImage.dispose();
					colorTransparent.dispose();

					/*
					 * set transparency
					 */
					final ImageData imageData = tempImage.getImageData();
					imageData.transparentPixel = imageData.getPixel(0, 0);

					image = new Image(display, imageData);

				}
				tempImage.dispose();
			}

		} else {

			// this is the default or TourTypeConfig.TOUR_TYPE_CONFIG_NOT_USED

		}

		// keep image in the cache
		final Image oldImage = _configImages.put(configId, image);

		Util.disposeResource(oldImage);

		_configImageHash.put(configId, importConfig.imageHash);

		return image;
	}

	@Override
	public PostSelectionProvider getPostSelectionProvider() {
		return _postSelectionProvider;
	}

	@Override
	public ArrayList<TourData> getSelectedTours() {

		final TourManager tourManager = TourManager.getInstance();

		// get selected tours
		final IStructuredSelection selectedTours = ((IStructuredSelection) _tourViewer.getSelection());

		final ArrayList<TourData> selectedTourData = new ArrayList<TourData>();

		// loop: all selected tours
		for (final Iterator<?> iter = selectedTours.iterator(); iter.hasNext();) {

			final Object tourItem = iter.next();

			if (tourItem instanceof TourData) {

				final TourData tourData = (TourData) tourItem;

				/*
				 * only tours are added which are saved in the database
				 */
				if (tourData.getTourPerson() != null) {

					/*
					 * get the data from the database because the tag names could be changed and
					 * this is not reflected in the tours which are displayed in the raw data view
					 */
					final TourData tourDataInDb = tourManager.getTourData(tourData.getTourId());
					if (tourDataInDb != null) {
						selectedTourData.add(tourDataInDb);
					}
				}
			}
		}

		return selectedTourData;
	}

	@Override
	public ColumnViewer getViewer() {
		return _tourViewer;
	}

	private void initUI(final Composite parent) {

		_parent = parent;
		_pc = new PixelConverter(parent);

		createResources_Image();
		createResources_Web();

		setupThread_PollingStores();
	}

	/**
	 * @param image
	 * @param importConfig
	 * @return Returns <code>true</code> when the image is valid, returns <code>false</code> when
	 *         the profile image must be created,
	 */
	private boolean isConfigImageValid(final Image image, final DeviceImportLauncher importConfig) {

		if (image == null || image.isDisposed()) {

			return false;
		}

		final Integer imageHash = _configImageHash.get(importConfig.getId());

		if (imageHash == null || imageHash != importConfig.imageHash) {

			image.dispose();

			return false;
		}

		return true;
	}

	private boolean isOSFolderValid(final String osFolder) {

		try {

			if (osFolder != null && osFolder.trim().length() > 0 && Files.exists(Paths.get(osFolder))) {

				// device folder exists
				return true;
			}

		} catch (final Exception e) {}

		return false;
	}

	private void onBrowser_Completed(final ProgressEvent event) {

		if (_isInUIStartup = true) {

			_isInUIStartup = false;

			_topPageBook.showPage(_topPage_Dashboard);

			_browser.setRedraw(true);
		}

//		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//				+ ("\tonBrowser_Completed: " + event));
//		// TODO remove SYSTEM.OUT.PRINTLN

//		if (_reloadedTourMarkerId == null) {
//			return;
//		}
//
//		// get local copy
//		final long reloadedTourMarkerId = _reloadedTourMarkerId;
//
//		/*
//		 * This must be run async otherwise an endless loop will happen
//		 */
//		_browser.getDisplay().asyncExec(new Runnable() {
//			@Override
//			public void run() {
//
//				final String href = "location.href='" + createHtml_MarkerName(reloadedTourMarkerId) + "'"; //$NON-NLS-1$ //$NON-NLS-2$
//
//				_browser.execute(href);
//			}
//		});
//
//		_reloadedTourMarkerId = null;

		if (_browserTask_DeviceState.getAndSet(false)) {

			updateUI_DeviceState_Task();
		}

		_isBrowserCompleted = true;
	}

	private void onBrowser_LocationChanging(final LocationEvent event) {

		final String location = event.location;

//		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//				+ ("\tonBrowser_LocationChanging: " + location));
//		// TODO remove SYSTEM.OUT.PRINTLN

		final String[] locationParts = location.split(HREF_TOKEN);

		if (locationParts.length > 1) {

			// end loading of the browser page and start a new action
			_browser.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					onBrowser_LocationChanging_Runnable(locationParts);
				}
			});
		}

		// prevent to load a new url
		if (location.equals(PAGE_ABOUT_BLANK) == false) {

			// about:blank is the initial page

			event.doit = false;
		}
	}

	private void onBrowser_LocationChanging_Runnable(final String[] locationParts) {

		final String hrefAction = locationParts[1];

		if (ACTION_DEVICE_IMPORT.equals(hrefAction)) {

			final long tileId = Long.parseLong(locationParts[2]);

			onSelectImportTile(tileId);

		} else if (ACTION_IMPORT_FROM_FILES.equals(hrefAction)) {

			_rawDataMgr.actionImportFromFile();

		} else if (ACTION_SERIAL_PORT_CONFIGURED.equals(hrefAction)) {

			_rawDataMgr.actionImportFromDevice();

		} else if (ACTION_SERIAL_PORT_DIRECTLY.equals(hrefAction)) {

			_rawDataMgr.actionImportFromDeviceDirect();

		} else if (ACTION_SETUP_DEVICE_IMPORT.equals(hrefAction)) {

			actionSetupImport();
		}
	}

	private void onBrowser_SetText(final String html) {

		_isBrowserCompleted = false;

		_browser.setText(html);
	}

	private void onSelectImportTile(final long tileId) {

		final ImportConfig importConfig = getImportConfig();

		for (final DeviceImportLauncher importTile : importConfig.deviceImportLaunchers) {

			if (importTile.getId() == tileId) {

				final ImportState importState = DeviceImportManager.getInstance().runImport(importTile);

				// open import config dialog to solve problems
				if (importState.isOpenSetup) {

					_parent.getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							actionSetupImport();
						}
					});
				}

				break;
			}
		}
	}

	private void onSelectionChanged(final ISelection selection) {

		if (!selection.isEmpty() && (selection instanceof SelectionDeletedTours)) {

			_postSelectionProvider.clearSelection();

			final SelectionDeletedTours tourSelection = (SelectionDeletedTours) selection;
			final ArrayList<ITourItem> removedTours = tourSelection.removedTours;

			if (removedTours.size() == 0) {
				return;
			}

			removeTours(removedTours);

			if (_isPartVisible) {

				_rawDataMgr.updateTourData_InImportView_FromDb(null);

				// update the table viewer
				reloadViewer();
			} else {
				_isViewerPersonDataDirty = true;
			}
		}
	}

	@Override
	public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

		_topPage_ImportViewer.setRedraw(false);
		{
			_tourViewer.getTable().dispose();
			createUI_92_TourViewer(_topPage_ImportViewer);
			_topPage_ImportViewer.layout();

			// update the viewer
			reloadViewer();
		}
		_topPage_ImportViewer.setRedraw(true);

		return _tourViewer;
	}

	/**
	 * Update {@link TourData} from the database for all imported tours, displays a progress dialog.
	 * 
	 * @param canCancelable
	 */
	private void reimportAllImportFiles(final boolean canCancelable) {

		final String[] prevImportedFiles = _state.getArray(STATE_IMPORTED_FILENAMES);
		if ((prevImportedFiles == null) || (prevImportedFiles.length == 0)) {
			return;
		}

//		if (prevImportedFiles.length < 5) {
//			reimportAllImportFilesTask(null, prevImportedFiles);
//		} else {

		try {
			new ProgressMonitorDialog(Display.getDefault().getActiveShell()).run(
					true,
					canCancelable,
					new IRunnableWithProgress() {

						@Override
						public void run(final IProgressMonitor monitor) throws InvocationTargetException,
								InterruptedException {

							reimportAllImportFiles_Runnable(monitor, prevImportedFiles, canCancelable);
						}
					});

		} catch (final Exception e) {
			StatusUtil.log(e);
		}
	}

	/**
	 * reimport previous imported tours
	 * 
	 * @param monitor
	 * @param importedFiles
	 * @param canCancelable
	 */
	private void reimportAllImportFiles_Runnable(	final IProgressMonitor monitor,
													final String[] importedFiles,
													final boolean canCancelable) {

		int workedDone = 0;
		final int workedAll = importedFiles.length;

		if (monitor != null) {
			monitor.beginTask(Messages.import_data_importTours_task, workedAll);
		}

		final ArrayList<String> notImportedFiles = new ArrayList<String>();

		_rawDataMgr.getImportedTours().clear();
		_rawDataMgr.setImportId();

		int importedFileCounter = 0;

		// loop: import all files
		for (final String fileName : importedFiles) {

			if (monitor != null) {
				monitor.worked(1);
				monitor.subTask(NLS.bind(Messages.import_data_importTours_subTask, //
						new Object[] { workedDone++, workedAll, fileName }));
			}

			final File file = new File(fileName);
			if (file.exists()) {
				if (_rawDataMgr.importRawData(file, null, false, null, true)) {
					importedFileCounter++;
				} else {
					notImportedFiles.add(fileName);
				}
			}

			if (canCancelable && monitor.isCanceled()) {
				// stop importing but process imported tours
				break;
			}
		}

		if (importedFileCounter > 0) {

			_rawDataMgr.updateTourData_InImportView_FromDb(monitor);

			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {

					reloadViewer();

					/*
					 * restore selected tour
					 */
					final String[] viewerIndices = _state.getArray(STATE_SELECTED_TOUR_INDICES);

					if (viewerIndices != null) {

						final ArrayList<Object> viewerTourData = new ArrayList<Object>();

						for (final String viewerIndex : viewerIndices) {

							Object tourData = null;

							try {
								final int index = Integer.parseInt(viewerIndex);
								tourData = _tourViewer.getElementAt(index);
							} catch (final NumberFormatException e) {
								// just ignore
							}

							if (tourData != null) {
								viewerTourData.add(tourData);
							}
						}

						if (viewerTourData.size() > 0) {
							_tourViewer.setSelection(new StructuredSelection(viewerTourData.toArray()), true);
						}
					}
				}
			});
		}

		if (notImportedFiles.size() > 0) {
			RawDataManager.showMsgBoxInvalidFormat(notImportedFiles);
		}
	}

	@Override
	public void reloadViewer() {

		updateUI_TopPage();

		// update tour data viewer
		final Object[] rawData = _rawDataMgr.getImportedTours().values().toArray();
		_tourViewer.setInput(rawData);

		enableActions();
	}

	private void removeTours(final ArrayList<ITourItem> removedTours) {

		final HashMap<Long, TourData> tourMap = _rawDataMgr.getImportedTours();

		for (final ITourItem tourItem : removedTours) {

			final TourData tourData = tourMap.get(tourItem.getTourId());
			if (tourData != null) {

				// when a tour was deleted the person in the tour data must be removed
				tourData.setTourPerson(null);

				// remove tour properties
				tourData.setTourType(null);
				tourData.setTourTitle(UI.EMPTY_STRING);
				tourData.setTourTags(new HashSet<TourTag>());

				/**
				 * when a remove tour is saved again, this will cause the exception: <br>
				 * detached entity passed to persist: net.tourbook.data.TourMarker<br>
				 * I didn't find a workaround, so this tour cannot be saved again until it is
				 * reloaded from the file
				 */
				tourData.isTourDeleted = true;
			}
		}
	}

	private void restoreState() {

		_actionRemoveToursWhenClosed.setChecked(Util.getStateBoolean(
				_state,
				STATE_IS_REMOVE_TOURS_WHEN_VIEW_CLOSED,
				true));

		// restore: set merge tracks status before the tours are imported
		final boolean isMergeTracks = _state.getBoolean(STATE_IS_MERGE_TRACKS);
		_rawDataMgr.setMergeTracks(isMergeTracks);

		// restore: set merge tracks status before the tours are imported
		final boolean isCreateTourIdWithTime = _state.getBoolean(STATE_IS_CREATE_TOUR_ID_WITH_TIME);
		_rawDataMgr.setCreateTourIdWithTime(isCreateTourIdWithTime);

		// restore: is checksum validation
		final boolean isValidation = _state.getBoolean(STATE_IS_CHECKSUM_VALIDATION);
		_rawDataMgr.setIsChecksumValidation(isValidation);

		updateToolTipState();

		Display.getCurrent().asyncExec(new Runnable() {
			@Override
			public void run() {
				reimportAllImportFiles(true);
			}
		});
	}

	private void saveState() {

		// check if UI is disposed
		final Table table = _tourViewer.getTable();
		if (table.isDisposed()) {
			return;
		}

		/*
		 * save imported file names
		 */
		final boolean isRemoveToursWhenClosed = _actionRemoveToursWhenClosed.isChecked();
		String[] stateImportedFiles;
		if (isRemoveToursWhenClosed) {
			stateImportedFiles = new String[] {};
		} else {
			final HashSet<String> importedFiles = _rawDataMgr.getImportedFiles();
			stateImportedFiles = importedFiles.toArray(new String[importedFiles.size()]);
		}
		_state.put(STATE_IMPORTED_FILENAMES, stateImportedFiles);
		_state.put(STATE_IS_REMOVE_TOURS_WHEN_VIEW_CLOSED, isRemoveToursWhenClosed);

		// keep selected tours
		Util.setState(_state, STATE_SELECTED_TOUR_INDICES, table.getSelectionIndices());

		_columnManager.saveState(_state);
	}

	/**
	 * @param tourData
	 *            {@link TourData} which is not yet saved
	 * @param person
	 *            person for which the tour is being saved
	 * @param savedTours
	 *            the saved tour is added to this list
	 */
	private void saveTour(	final TourData tourData,
							final TourPerson person,
							final ArrayList<TourData> savedTours,
							final boolean isForceSave) {

		// workaround for hibernate problems
		if (tourData.isTourDeleted) {
			return;
		}

		if ((tourData.getTourPerson() != null) && (isForceSave == false)) {
			/*
			 * tour is already saved, resaving cannot be done in the import view it can be done in
			 * the tour editor
			 */
			return;
		}

		tourData.setTourPerson(person);
		tourData.setBikerWeight(person.getWeight());
		tourData.setTourBike(person.getTourBike());

		final TourData savedTour = TourDatabase.saveTour(tourData, true);
		if (savedTour != null) {
			savedTours.add(savedTour);
		}
	}

	/**
	 * select first tour in the viewer
	 */
	public void selectFirstTour() {

		final TourData firstTourData = (TourData) _tourViewer.getElementAt(0);
		if (firstTourData != null) {
			_tourViewer.setSelection(new StructuredSelection(firstTourData), true);
		}
	}

	void selectLastTour() {

		final Collection<TourData> tourDataCollection = _rawDataMgr.getImportedTours().values();

		final TourData[] tourList = tourDataCollection.toArray(new TourData[tourDataCollection.size()]);

		// select the last tour in the viewer
		if (tourList.length > 0) {
			final TourData tourData = tourList[0];
			_tourViewer.setSelection(new StructuredSelection(tourData), true);
		}
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
	public void setFocus() {

		_tourViewer.getControl().setFocus();

		if (_postSelectionProvider.getSelection() == null) {

			// fire a selected tour when the selection provider was cleared sometime before
			Display.getCurrent().asyncExec(new Runnable() {
				@Override
				public void run() {
					fireSelectedTour();
				}
			});
		}
	}

	private void setupThread_PollingStores() {

		_devicePollingThread = new Thread("WatchingStores") { //$NON-NLS-1$
			@Override
			public void run() {

				while (!isInterrupted()) {

					try {

						Thread.sleep(1000);

						if (_isStopPollingThread) {
							break;
						}

						if (_canPollImportFiles.get()) {

							final DeviceImportState importState = DeviceImportManager.getInstance().checkImportedFiles(
									false);

							if (importState.areTheSameStores == false) {

								// stores has changed, update the folder watcher

								setupThread_WatchDeviceFolder();
							}

							if (importState.areFilesRetrieved) {

								// import files have been retrieved, update the UI

								updateUI_DeviceState();
							}
						}

					} catch (final InterruptedException e) {
						interrupt();
					} catch (final Exception e) {
						StatusUtil.log(e);
					}
				}
			}
		};

		_devicePollingThread.setDaemon(true);
		_devicePollingThread.start();
	}

	private void setupThread_WatchDeviceFolder() {

		// cancel previous watcher
		cancelThread_WatchDeviceFolder();

		try {

			// check new folder
			final String deviceFolder = getImportConfig().getDeviceOSFolder();
			if (deviceFolder == null) {
				return;
			}

			// check file
			final Path watchFolder = Paths.get(deviceFolder);
			if (!Files.exists(watchFolder)) {
				return;
			}

			final Runnable runnable = new Runnable() {
				@Override
				public void run() {

					try {

						_watcher = FileSystems.getDefault().newWatchService();

						watchFolder.register(_watcher, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);

						// wait for the next event
						_watchKey = _watcher.take();

						while (_watchKey != null) {

							if (_isCancelWatchFolder) {
								break;
							}

							/*
							 * Events MUST be polled otherwise this will be an endless loop.
							 */
							@SuppressWarnings("unused")
							final List<WatchEvent<?>> polledEvents = _watchKey.pollEvents();

// log events, they are not used
//							for (final WatchEvent<?> event : polledEvents) {
//
//								final WatchEvent.Kind<?> kind = event.kind();
//
//								System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//										+ (String.format("Event: %s\tFile: %s", kind, event.context())));
//								// TODO remove SYSTEM.OUT.PRINTLN
//							}

							updateDeviceState();

							if (_watchKey != null && _watchKey.reset()) {

								// wait for the next event
								_watchKey = _watcher.take();

							} else {

								break;
							}
						}

					} catch (final InterruptedException e) {
						//
					} catch (final ClosedWatchServiceException e) {
						//
					} catch (final Exception e) {
						StatusUtil.log(e);
					} finally {

						if (_watchKey != null) {
							_watchKey.cancel();
						}

						if (_watcher != null) {
							try {
								_watcher.close();
							} catch (final IOException e) {
								StatusUtil.log(e);
							}
						}
					}
				}
			};

			_watchFolderThread = new Thread(runnable, "WatchingDeviceFolder: " + deviceFolder); //$NON-NLS-1$
			_watchFolderThread.setDaemon(true);
			_watchFolderThread.start();

		} catch (final Exception e) {
			StatusUtil.log(e);
		}
	}

	/**
	 * Retrieve files from the device folder and update the UI.
	 */
	private void updateDeviceState() {

		DeviceImportManager.getInstance().checkImportedFiles(true);
		updateUI_DeviceState();
	}

	/**
	 * Keep none live values.
	 */
	private void updateModel_ImportConfig_AndSave(final DialogDeviceImportConfig dialog) {

		final ImportConfig modifiedConfig = dialog.getModifiedConfig();
		final ImportConfig importConfig = getImportConfig();

		importConfig.isCreateBackup = modifiedConfig.isCreateBackup;
		importConfig.backupFolder = modifiedConfig.backupFolder;
		importConfig.deviceFolder = modifiedConfig.deviceFolder;

		DeviceImportManager.getInstance().saveImportConfig(importConfig);
	}

	/**
	 * Keep live update values, other values MUST already have been set.
	 * 
	 * @param isSaveConfig
	 */
	private void updateModel_ImportConfig_LiveUpdate(final DialogDeviceImportConfig dialog, final boolean isSaveConfig) {

		final ImportConfig modifiedConfig = dialog.getModifiedConfig();
		final ImportConfig importConfig = getImportConfig();

		if (importConfig.animationDuration != modifiedConfig.animationDuration
				|| importConfig.animationCrazinessFactor != modifiedConfig.animationCrazinessFactor) {

			// run animation only when it was modified
			_isRunAnimation = true;
		}
		importConfig.animationCrazinessFactor = modifiedConfig.animationCrazinessFactor;
		importConfig.animationDuration = modifiedConfig.animationDuration;

		importConfig.backgroundOpacity = modifiedConfig.backgroundOpacity;
		importConfig.isLiveUpdate = modifiedConfig.isLiveUpdate;
		importConfig.numHorizontalTiles = modifiedConfig.numHorizontalTiles;
		importConfig.tileSize = modifiedConfig.tileSize;

		if (isSaveConfig) {
			DeviceImportManager.getInstance().saveImportConfig(importConfig);
		}
	}

	private void updateToolTipState() {

		_isToolTipInDate = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_DATE);
		_isToolTipInTime = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_TIME);
		_isToolTipInTitle = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_TITLE);
		_isToolTipInTags = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_TAGS);
	}

	/**
	 * Set/create dashboard page.
	 */
	private void updateUI_Dashboard() {

		final boolean isBrowserAvailable = _browser != null;

		// set dashboard page
		_dashboard_PageBook.showPage(isBrowserAvailable//
				? _dashboardPage_WithBrowser
				: _dashboardPage_NoBrowser);

		if (!isBrowserAvailable) {
			return;
		}

		final String html = createHTML();

		onBrowser_SetText(html);
	}

	private void updateUI_DeviceState() {

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {

				if (_browser.isDisposed()) {
					// this occured
					return;
				}

				if (_isBrowserCompleted) {
					updateUI_DeviceState_Task();
				} else {
					_browserTask_DeviceState.set(true);
				}
			}
		});
	}

	private void updateUI_DeviceState_Task() {

		final String deviceState = createHTML_DeviceState();
		final String jsHTML = deviceState.replace("\"", "\\\""); //$NON-NLS-1$ //$NON-NLS-2$

		final String js = "\n" //$NON-NLS-1$
				+ ("var htmlDeviceState =\"" + jsHTML + "\";\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("document.getElementById(\"" + DOM_ID_DEVICE_STATE + "\").innerHTML = htmlDeviceState;\n"); //$NON-NLS-1$ //$NON-NLS-2$

		_browser.execute(js);

//		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//				+ ("updateUI_DeviceState_Task\n" + js));
//		//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//		// TODO remove SYSTEM.OUT.PRINTLN
	}

	/**
	 * Set top page.
	 */
	private void updateUI_TopPage() {

		/*
		 * When imported tours are available then the import viewer page will ALLWAYS be displayed.
		 */
		final int numImportedTours = _rawDataMgr.getImportedTours().size();
		if (numImportedTours > 0) {

			deactivateBackgroundTask();

			_topPageBook.showPage(_topPage_ImportViewer);

		} else {

			/*
			 * Run async that the first page in the top pagebook is visible and to prevent
			 * flickering when the view toolbar is first drawn on the left side of the view !!!
			 */

			_parent.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {

					if (_isInUIStartup) {

						// dashboard is not yet created
						updateUI_Dashboard();

					} else {

						_topPageBook.showPage(_topPage_Dashboard);
					}

					if (_browser != null) {

						// dashboard is visible, activate background task

						_browser.setFocus();

						_canPollImportFiles.set(true);

						setupThread_WatchDeviceFolder();

						updateDeviceState();

					} else {

						// deactivate background task

						deactivateBackgroundTask();
					}
				}
			});
		}
	}

	/**
	 * when the active person was modified, the view must be updated
	 */
	private void updateViewerPersonData() {

		_activePerson = TourbookPlugin.getActivePerson();

		// update person in save action
		enableActions();

		// update person in the raw data
		_rawDataMgr.updateTourData_InImportView_FromDb(null);

		_tourViewer.refresh();
	}
}
