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
package net.tourbook.ui.views.rawData;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.data.TourWayPoint;
import net.tourbook.database.TourDatabase;
import net.tourbook.export.ActionExport;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tag.ActionRemoveAllTags;
import net.tourbook.tag.ActionSetTourTag;
import net.tourbook.tag.TagManager;
import net.tourbook.tour.ActionOpenAdjustAltitudeDialog;
import net.tourbook.tour.ActionOpenMarkerDialog;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.ITourItem;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TourTypeMenuManager;
import net.tourbook.ui.ITourProviderAll;
import net.tourbook.ui.TableColumnFactory;
import net.tourbook.ui.UI;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.ui.action.ActionEditTour;
import net.tourbook.ui.action.ActionJoinTours;
import net.tourbook.ui.action.ActionModifyColumns;
import net.tourbook.ui.action.ActionOpenPrefDialog;
import net.tourbook.ui.action.ActionOpenTour;
import net.tourbook.ui.action.ActionSetTourTypeMenu;
import net.tourbook.ui.views.TableViewerTourInfoToolTip;
import net.tourbook.ui.views.TourInfoToolTipCellLabelProvider;
import net.tourbook.util.ColumnDefinition;
import net.tourbook.util.ColumnManager;
import net.tourbook.util.ITourViewer;
import net.tourbook.util.PixelConverter;
import net.tourbook.util.PostSelectionProvider;
import net.tourbook.util.Util;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.GridLayoutFactory;
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
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

/**
 * 
 */
public class RawDataView extends ViewPart implements ITourProviderAll, ITourViewer {

	public static final String				ID									= "net.tourbook.views.rawData.RawDataView"; //$NON-NLS-1$

	public static final int					COLUMN_DATE							= 0;
	public static final int					COLUMN_TITLE						= 1;
	public static final int					COLUMN_DATA_FORMAT					= 2;
	public static final int					COLUMN_FILE_NAME					= 3;

	private static final String				STATE_IMPORTED_FILENAMES			= "importedFilenames";						//$NON-NLS-1$
	private static final String				STATE_SELECTED_TOUR_INDICES			= "SelectedTourIndices";					//$NON-NLS-1$

	private static final String				STATE_IS_MERGE_TRACKS				= "isMergeTracks";							//$NON-NLS-1$
	private static final String				STATE_IS_CHECKSUM_VALIDATION		= "isChecksumValidation";					//$NON-NLS-1$
	private static final String				STATE_IS_CREATE_TOUR_ID_WITH_TIME	= "isCreateTourIdWithTime";				//$NON-NLS-1$

	private final IPreferenceStore			_prefStore							= TourbookPlugin
																						.getDefault()
																						.getPreferenceStore();

	private final IDialogSettings			_state								= TourbookPlugin
																						.getDefault()
																						.getDialogSettingsSection(ID);

	private PostSelectionProvider			_postSelectionProvider;
	private IPartListener2					_partListener;
	private ISelectionListener				_postSelectionListener;
	private IPropertyChangeListener			_prefChangeListener;
	private ITourEventListener				_tourEventListener;

	protected TourPerson					_activePerson;
	protected TourPerson					_newActivePerson;

	protected boolean						_isPartVisible						= false;
	protected boolean						_isViewerPersonDataDirty			= false;

	private ColumnManager					_columnManager;

	private final Calendar					_calendar							= GregorianCalendar.getInstance();

	private final NumberFormat				_nf									= NumberFormat.getNumberInstance();
	private final DateFormat				_dateFormatter						= DateFormat
																						.getDateInstance(DateFormat.SHORT);
	private final DateFormat				_timeFormatter						= DateFormat
																						.getTimeInstance(DateFormat.SHORT);
	private final DateFormat				_durationFormatter					= DateFormat.getTimeInstance(
																						DateFormat.SHORT,
																						Locale.GERMAN);

	/*
	 * resources
	 */
	private ImageDescriptor					_imageDescDatabase;
	private ImageDescriptor					_imageDescDatabaseOtherPerson;
	private ImageDescriptor					_imageDescDatabaseAssignMergedTour;
	private ImageDescriptor					_imageDescDatabasePlaceholder;
	private ImageDescriptor					_imageDescDelete;

	private Image							_imageDatabase;
	private Image							_imageDatabaseOtherPerson;
	private Image							_imageDatabaseAssignMergedTour;
	private Image							_imageDatabasePlaceholder;
	private Image							_imageDelete;

	/*
	 * UI controls
	 */
	private Composite						_viewerContainer;
	private TableViewer						_tourViewer;

	private ActionClearView					_actionClearView;
	private ActionModifyColumns				_actionModifyColumns;
	private ActionSaveTourInDatabase		_actionSaveTour;
	private ActionSaveTourInDatabase		_actionSaveTourWithPerson;
	private ActionMergeIntoMenu				_actionMergeIntoTour;
	private ActionReimportTour				_actionReimportTour;
	private ActionRemoveTour				_actionRemoveTour;
	private ActionAdjustYear				_actionAdjustImportedYear;
	private ActionMergeGPXTours				_actionMergeGPXTours;
	private ActionCreateTourIdWithTime		_actionCreateTourIdWithTime;
	private ActionDisableChecksumValidation	_actionDisableChecksumValidation;
	private ActionSetTourTypeMenu			_actionSetTourType;
	private ActionEditQuick					_actionEditQuick;
	private ActionEditTour					_actionEditTour;
	private ActionMergeTour					_actionMergeTour;
	private ActionSetTourTag				_actionAddTag;
	private ActionSetTourTag				_actionRemoveTag;
	private ActionRemoveAllTags				_actionRemoveAllTags;
	private ActionOpenPrefDialog			_actionOpenTagPrefs;
	private ActionOpenTour					_actionOpenTour;
	private ActionOpenMarkerDialog			_actionOpenMarkerDialog;
	private ActionOpenAdjustAltitudeDialog	_actionOpenAdjustAltitudeDialog;
	private ActionExport					_actionExportTour;
	private ActionJoinTours					_actionJoinTours;

	private TableViewerTourInfoToolTip		_tourInfoToolTip;

	private class TourDataContentProvider implements IStructuredContentProvider {

		public TourDataContentProvider() {}

		public void dispose() {}

		public Object[] getElements(final Object parent) {
			return (Object[]) (parent);
		}

		public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {}
	}

	void actionClearView() {

		// remove all tours
		RawDataManager.getInstance().removeAllTours();

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

	void actionReimportTour() {

		// check if the tour editor contains a modified tour
		if (TourManager.isTourEditorModified()) {
			return;
		}

		if (MessageDialog.openConfirm(
				Display.getCurrent().getActiveShell(),
				Messages.import_data_dlg_reimport_title,
				Messages.Import_Data_Dialog_ConfirmReimport_Message) == false) {
			return;
		}

		boolean isTourImported = false;

		// prevent async error in save tour method, cleanup environment
		TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR, null, null);
		_postSelectionProvider.clearSelection();

		// get selected tours
		final IStructuredSelection selection = ((IStructuredSelection) _tourViewer.getSelection());

		final ArrayList<String> notImportedFiles = new ArrayList<String>();

		final RawDataManager rawDataMgr = RawDataManager.getInstance();
		final HashMap<Long, TourData> importedTours = rawDataMgr.getImportedTours();

		final TourData[] firstTourData = new TourData[1];

		for (final Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {

			final Object element = iterator.next();
			if (element instanceof TourData) {

				final TourData tourData = (TourData) element;

				// get import file name
				String importFilePathName = tourData.importRawDataFile;
				if (importFilePathName == null) {

					if (tourData.isTourImportFilePathAvailable()) {

						importFilePathName = tourData.getTourImportFilePath();

					} else {

						MessageDialog.openInformation(
								Display.getCurrent().getActiveShell(),
								Messages.import_data_dlg_reimport_title,
								Messages.import_data_dlg_reimport_message);

						continue;
					}
				}

				// check import file
				final File file = new File(importFilePathName);
				if (file.exists() == false) {
					MessageDialog.openInformation(
							Display.getCurrent().getActiveShell(),
							Messages.import_data_dlg_reimport_title,
							NLS.bind(Messages.import_data_dlg_reimport_invalid_file_message, importFilePathName));
					continue;
				}

				final Long tourId = tourData.getTourId();

				// remove old tour data
				importedTours.remove(tourId);

				if (rawDataMgr.importRawData(file, null, false, null)) {

					isTourImported = true;

					// keep first tour
					if (firstTourData[0] == null) {
						firstTourData[0] = tourData;
					}

					final TourPerson tourPerson = tourData.getTourPerson();
					if (tourPerson != null) {

						// resave tour when the reimported tour was saved before

						// get reimported tour
						final TourData mapTourData = importedTours.get(tourId);
						if (mapTourData == null) {
							System.err.println("reimported tour was not found in map");//$NON-NLS-1$
						} else {

							mapTourData.setTourPerson(tourPerson);
							final TourData savedTourData = TourManager.saveModifiedTour(mapTourData);

							// replace tour in map
							importedTours.put(savedTourData.getTourId(), savedTourData);
						}
					}

				} else {
					notImportedFiles.add(importFilePathName);
				}
			}
		}

		if (notImportedFiles.size() > 0) {
			RawDataManager.showMsgBoxInvalidFormat(notImportedFiles);
		}

		if (isTourImported) {

			//
			rawDataMgr.updateTourDataFromDb(null);

			reloadViewer();

			if (firstTourData != null) {

				// reselect tours
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						_tourViewer.setSelection(selection, true);
					}
				});
			}
		}
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

		RawDataManager.getInstance().removeTours(selectedTours);

		TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR, null, RawDataView.this);

		// update the table viewer
		reloadViewer();
	}

	void actionSaveTour(final TourPerson person) {

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {

				final ArrayList<TourData> savedTours = new ArrayList<TourData>();

				// get selected tours
				final IStructuredSelection selection = ((IStructuredSelection) _tourViewer.getSelection());

				// loop: all selected tours, selected tours can already be saved
				for (final Iterator<?> iter = selection.iterator(); iter.hasNext();) {

					final Object selObject = iter.next();
					if (selObject instanceof TourData) {
						saveTour((TourData) selObject, person, savedTours, false);
					}
				}

				doSaveTourPostActions(savedTours);
			}
		});
	}

	private void addPartListener() {
		_partListener = new IPartListener2() {

			public void partActivated(final IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == RawDataView.this) {

					saveState();

					// remove all tours
					RawDataManager.getInstance().removeAllTours();

					TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR, null, RawDataView.this);
				}
			}

			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			public void partHidden(final IWorkbenchPartReference partRef) {
				if (RawDataView.this == partRef.getPart(false)) {
					_isPartVisible = false;
				}
			}

			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			public void partOpened(final IWorkbenchPartReference partRef) {}

			public void partVisible(final IWorkbenchPartReference partRef) {
				if (RawDataView.this == partRef.getPart(false)) {
					_isPartVisible = true;
					if (_isViewerPersonDataDirty || (_newActivePerson != _activePerson)) {
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
					RawDataManager.getInstance().updateTourDataFromDb(null);

					_tourViewer.refresh();

				} else if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

					UI.updateUnits();

					_columnManager.saveState(_state);
					_columnManager.clearColumns();
					defineAllColumns(_viewerContainer);

					_tourViewer = (TableViewer) recreateViewer(_tourViewer);

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
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (part == RawDataView.this) {
					return;
				}

				if ((eventId == TourEventId.TOUR_CHANGED) && (eventData instanceof TourEvent)) {

					// update modified tours
					final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
					if (modifiedTours != null) {

						// update model
						RawDataManager.getInstance().updateTourDataModel(modifiedTours);

						// update viewer
						_tourViewer.update(modifiedTours.toArray(), null);

						// remove old selection, old selection can have the same tour but with old data
						_postSelectionProvider.clearSelection();
					}

				} else if (eventId == TourEventId.ALL_TOURS_ARE_MODIFIED) {

					// save imported file names
					final HashSet<String> importedFiles = RawDataManager.getInstance().getImportedFiles();
					_state.put(STATE_IMPORTED_FILENAMES, importedFiles.toArray(new String[importedFiles.size()]));

					reimportAllImportFiles();

				} else if (eventId == TourEventId.TAG_STRUCTURE_CHANGED) {

					RawDataManager.getInstance().updateTourDataFromDb(null);

					reloadViewer();
				}
			}
		};
		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	private void createActions() {

		// context menu
		_actionSaveTour = new ActionSaveTourInDatabase(this, false);
		_actionSaveTourWithPerson = new ActionSaveTourInDatabase(this, true);
		_actionMergeIntoTour = new ActionMergeIntoMenu(this);
		_actionReimportTour = new ActionReimportTour(this);
		_actionRemoveTour = new ActionRemoveTour(this);
		_actionExportTour = new ActionExport(this);
		_actionJoinTours = new ActionJoinTours(this);

		_actionEditTour = new ActionEditTour(this);
		_actionEditQuick = new ActionEditQuick(this);
		_actionMergeTour = new ActionMergeTour(this);
		_actionOpenTour = new ActionOpenTour(this);
		_actionSetTourType = new ActionSetTourTypeMenu(this);

		_actionOpenMarkerDialog = new ActionOpenMarkerDialog(this, true);
		_actionOpenAdjustAltitudeDialog = new ActionOpenAdjustAltitudeDialog(this);

		_actionAddTag = new ActionSetTourTag(this, true);
		_actionRemoveTag = new ActionSetTourTag(this, false);
		_actionRemoveAllTags = new ActionRemoveAllTags(this);

		_actionOpenTagPrefs = new ActionOpenPrefDialog(
				Messages.action_tag_open_tagging_structure,
				ITourbookPreferences.PREF_PAGE_TAGS);

		// view toolbar
		_actionClearView = new ActionClearView(this);
//		fActionRefreshView = new ActionRefreshView(this);

		// view menu
		_actionModifyColumns = new ActionModifyColumns(this);
		_actionMergeGPXTours = new ActionMergeGPXTours(this);
		_actionCreateTourIdWithTime = new ActionCreateTourIdWithTime(this);
		_actionAdjustImportedYear = new ActionAdjustYear(this);
		_actionDisableChecksumValidation = new ActionDisableChecksumValidation(this);
	}

	/**
	 * create the views context menu
	 */
	private void createContextMenu() {

		final MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager manager) {
				fillContextMenu(manager);
			}
		});

		final Menu menu = menuMgr.createContextMenu(_tourViewer.getControl());
		_tourViewer.getControl().setMenu(menu);

		getSite().registerContextMenu(menuMgr, _tourViewer);
	}

	@Override
	public void createPartControl(final Composite parent) {

		createResources();

		// define all columns
		_columnManager = new ColumnManager(this, _state);
		defineAllColumns(parent);

		_viewerContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);

		createTourViewer(_viewerContainer);

		createActions();
		fillToolbar();

		addPartListener();
		addSelectionListener();
		addPrefListener();
		addTourEventListener();

		// set this view part as selection provider
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider());

		_activePerson = TourbookPlugin.getActivePerson();

		restoreState();
	}

	private void createResources() {

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
			e.printStackTrace();
		}

	}

	/**
	 * @param parent
	 */
	private void createTourViewer(final Composite parent) {

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
			public void doubleClick(final DoubleClickEvent event) {
				onOpenTourInEditor();
			}
		});

		_tourViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				fireSelectedTour();
			}
		});

		createContextMenu();

		// set tour info tooltip provider
		_tourInfoToolTip = new TableViewerTourInfoToolTip(_tourViewer);
	}

	/**
	 * Defines all columns for the table viewer in the column manager, the sequenze defines the
	 * default columns
	 * 
	 * @param parent
	 */
	private void defineAllColumns(final Composite parent) {

		final PixelConverter pc = new PixelConverter(parent);

		defineColumnDatabase(pc);
		defineColumnDate(pc);
		defineColumnTime(pc);
		defineColumnTourType(pc);
		defineColumnRecordingTime(pc);
		defineColumnDrivingTime(pc);
		defineColumnCalories(pc);
		defineColumnDistance(pc);
		defineColumnAvgSpeed(pc);
		defineColumnAvgPace(pc);
		defineColumnAltitudeUp(pc);
		defineColumnAltitudeDown(pc);
		defineColumnTitle(pc);
		defineColumnTags(pc);
		defineColumnDeviceName(pc);
		defineColumnDeviceProfile(pc);
		defineColumnMarker(pc);
		defineColumnTimeInterval(pc);
		defineColumnImportFileName(pc);
		defineColumnImportFilePath(pc);
	}

	/**
	 * column: altitude down
	 */
	private void defineColumnAltitudeDown(final PixelConverter pc) {

		final ColumnDefinition colDef = TableColumnFactory.ALTITUDE_DOWN_SUMMARIZED_BORDER.createColumn(
				_columnManager,
				pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final int tourAltDown = ((TourData) cell.getElement()).getTourAltDown();
				if (tourAltDown != 0) {
					cell.setText(Long.toString((long) (-tourAltDown / UI.UNIT_VALUE_ALTITUDE)));
				}
			}
		});
	}

	/**
	 * column: altitude up
	 */
	private void defineColumnAltitudeUp(final PixelConverter pc) {

		final ColumnDefinition colDef = TableColumnFactory.ALTITUDE_UP_SUMMARIZED_BORDER.createColumn(
				_columnManager,
				pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final int tourAltUp = ((TourData) cell.getElement()).getTourAltUp();
				if (tourAltUp != 0) {
					cell.setText(Long.toString((long) (tourAltUp / UI.UNIT_VALUE_ALTITUDE)));
				}
			}
		});
	}

	/**
	 * column: average pace
	 */
	private void defineColumnAvgPace(final PixelConverter pc) {

		final ColumnDefinition colDef = TableColumnFactory.AVG_PACE.createColumn(_columnManager, pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourData tourData = (TourData) cell.getElement();

				final int tourDistance = tourData.getTourDistance();
				final int drivingTime = tourData.getTourDrivingTime();

				final float pace = tourDistance == 0 ? //
						0
						: (float) drivingTime * 1000 / tourDistance * UI.UNIT_VALUE_DISTANCE;

				cell.setText(UI.format_mm_ss((long) pace));
			}
		});
	}

	/**
	 * column: avg speed
	 */
	private void defineColumnAvgSpeed(final PixelConverter pc) {

		final ColumnDefinition colDef = TableColumnFactory.AVG_SPEED.createColumn(_columnManager, pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final TourData tourData = ((TourData) cell.getElement());
				final int tourDistance = tourData.getTourDistance();
				final int drivingTime = tourData.getTourDrivingTime();
				if (drivingTime != 0) {
					_nf.setMinimumFractionDigits(1);
					_nf.setMaximumFractionDigits(1);

					cell.setText(_nf.format(((float) tourDistance) / drivingTime * 3.6 / UI.UNIT_VALUE_DISTANCE));
				}
			}
		});
	}

	/**
	 * column: calories (cal)
	 */
	private void defineColumnCalories(final PixelConverter pc) {

		final ColumnDefinition colDef = TableColumnFactory.CALORIES.createColumn(_columnManager, pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final int tourCalories = ((TourData) cell.getElement()).getCalories();
				cell.setText(Integer.toString(tourCalories));
			}
		});
	}

	/**
	 * column: database indicator
	 */
	private void defineColumnDatabase(final PixelConverter pc) {

		final ColumnDefinition colDef = TableColumnFactory.DB_STATUS.createColumn(_columnManager, pc);
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
	private void defineColumnDate(final PixelConverter pc) {

		final ColumnDefinition colDef = TableColumnFactory.TOUR_DATE.createColumn(_columnManager, pc);
		colDef.setIsDefaultColumn();
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

			@Override
			public Long getTourId(final ViewerCell cell) {
				return ((TourData) cell.getElement()).getTourId();
			}

			@Override
			public void update(final ViewerCell cell) {

				final TourData tourData = (TourData) cell.getElement();

				_calendar.set(tourData.getStartYear(), tourData.getStartMonth() - 1, tourData.getStartDay());
				cell.setText(_dateFormatter.format(_calendar.getTime()));
			}
		});

		// sort column
		colDef.addSelectionListener(new SelectionAdapter() {
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
	private void defineColumnDeviceName(final PixelConverter pc) {

		final ColumnDefinition colDef = TableColumnFactory.DEVICE_NAME.createColumn(_columnManager, pc);
		colDef.addSelectionListener(new SelectionAdapter() {
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
	private void defineColumnDeviceProfile(final PixelConverter pc) {

		TableColumnFactory.DEVICE_PROFILE.createColumn(_columnManager, pc);
	}

	/**
	 * column: distance (km/mile)
	 */
	private void defineColumnDistance(final PixelConverter pc) {

		final ColumnDefinition colDef = TableColumnFactory.DISTANCE.createColumn(_columnManager, pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final int tourDistance = ((TourData) cell.getElement()).getTourDistance();
				if (tourDistance == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					_nf.setMinimumFractionDigits(3);
					_nf.setMaximumFractionDigits(3);
					cell.setText(_nf.format(((float) tourDistance) / 1000 / UI.UNIT_VALUE_DISTANCE));
				}
			}
		});
	}

	/**
	 * column: driving time
	 */
	private void defineColumnDrivingTime(final PixelConverter pc) {

		final ColumnDefinition colDef = TableColumnFactory.DRIVING_TIME.createColumn(_columnManager, pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final int drivingTime = ((TourData) cell.getElement()).getTourDrivingTime();

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
	private void defineColumnImportFileName(final PixelConverter pc) {

		final ColumnDefinition colDef = TableColumnFactory.IMPORT_FILE_NAME.createColumn(_columnManager, pc);
		colDef.addSelectionListener(new SelectionAdapter() {
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
	private void defineColumnImportFilePath(final PixelConverter pc) {
		TableColumnFactory.IMPORT_FILE_PATH.createColumn(_columnManager, pc);
	}

	/**
	 * column: markers
	 */
	private void defineColumnMarker(final PixelConverter pixelConverter) {

		final ColumnDefinition colDef = TableColumnFactory.TOUR_MARKERS.createColumn(_columnManager, pixelConverter);
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
						size = wayPoints.size();
					}
					cell.setText(Integer.toString(size));
				}
			}
		});
	}

	/**
	 * column: recording time
	 */
	private void defineColumnRecordingTime(final PixelConverter pc) {

		final ColumnDefinition colDef = TableColumnFactory.RECORDING_TIME.createColumn(_columnManager, pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final int recordingTime = ((TourData) cell.getElement()).getTourRecordingTime();

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
	private void defineColumnTags(final PixelConverter pc) {

		final ColumnDefinition colDef = TableColumnFactory.TOUR_TAGS.createColumn(_columnManager, pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

			@Override
			public Long getTourId(final ViewerCell cell) {
				return ((TourData) cell.getElement()).getTourId();
			}

			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final Set<TourTag> tourTags = ((TourData) element).getTourTags();

				if (tourTags.size() == 0) {

					// the tags could have been removed, set empty field

					cell.setText(UI.EMPTY_STRING);

				} else {

					// convert the tags into a list of tag ids
					final ArrayList<Long> tagIds = new ArrayList<Long>();
					for (final TourTag tourTag : tourTags) {
						tagIds.add(tourTag.getTagId());
					}

					cell.setText(TourDatabase.getTagNames(tagIds));
				}
			}
		});
	}

	/**
	 * column: time
	 */
	private void defineColumnTime(final PixelConverter pc) {

		final ColumnDefinition colDef = TableColumnFactory.TOUR_START_TIME.createColumn(_columnManager, pc);
		colDef.setIsDefaultColumn();
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

			@Override
			public Long getTourId(final ViewerCell cell) {
				return ((TourData) cell.getElement()).getTourId();
			}

			@Override
			public void update(final ViewerCell cell) {

				final TourData tourData = (TourData) cell.getElement();
				_calendar.set(0, 0, 0, tourData.getStartHour(), tourData.getStartMinute(), 0);

				cell.setText(_timeFormatter.format(_calendar.getTime()));
			}
		});

		// sort column
		colDef.addSelectionListener(new SelectionAdapter() {
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
	private void defineColumnTimeInterval(final PixelConverter pc) {

		TableColumnFactory.TIME_INTERVAL.createColumn(_columnManager, pc);
	}

	/**
	 * column: tour title
	 */
	private void defineColumnTitle(final PixelConverter pc) {

		final ColumnDefinition colDef = TableColumnFactory.TOUR_TITLE.createColumn(_columnManager, pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

			@Override
			public Long getTourId(final ViewerCell cell) {
				return ((TourData) cell.getElement()).getTourId();
			}

			@Override
			public void update(final ViewerCell cell) {
				final TourData tourData = (TourData) cell.getElement();
				cell.setText(tourData.getTourTitle());
			}
		});
		colDef.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((DeviceImportSorter) _tourViewer.getSorter()).doSort(COLUMN_TITLE);
				_tourViewer.refresh();
			}
		});
	}

	/**
	 * column: tour type
	 */
	private void defineColumnTourType(final PixelConverter pc) {

		final ColumnDefinition colDef = TableColumnFactory.TOUR_TYPE.createColumn(_columnManager, pc);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourType tourType = ((TourData) cell.getElement()).getTourType();
				if (tourType == null) {
					cell.setImage(UI.getInstance().getTourTypeImage(TourDatabase.ENTITY_IS_NOT_SAVED));
				} else {

					final long tourTypeId = tourType.getTypeId();
					final Image tourTypeImage = UI.getInstance().getTourTypeImage(tourTypeId);

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

	@Override
	public void dispose() {

		if (_imageDatabase != null) {
			_imageDescDatabase.destroyResource(_imageDatabase);
		}
		if (_imageDatabaseOtherPerson != null) {
			_imageDescDatabaseOtherPerson.destroyResource(_imageDatabaseOtherPerson);
		}
		if (_imageDatabaseAssignMergedTour != null) {
			_imageDescDatabaseAssignMergedTour.destroyResource(_imageDatabaseAssignMergedTour);
		}
		if (_imageDatabasePlaceholder != null) {
			_imageDescDatabasePlaceholder.destroyResource(_imageDatabasePlaceholder);
		}
		if (_imageDelete != null) {
			_imageDescDelete.destroyResource(_imageDelete);
		}

		// don't throw the selection again
		_postSelectionProvider.clearSelection();

		getViewSite().getPage().removePartListener(_partListener);
		getSite().getPage().removeSelectionListener(_postSelectionListener);

		TourManager.getInstance().removeTourEventListener(_tourEventListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
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
		final HashMap<Long, TourData> rawDataMap = RawDataManager.getInstance().getImportedTours();
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
		TourManager.fireEvent(TourEventId.UPDATE_UI, new SelectionTourIds(savedToursIds));
	}

	void enableActions() {

		final StructuredSelection selection = (StructuredSelection) _tourViewer.getSelection();

		int savedTours = 0;
		int unsavedTours = 0;
		int selectedTours = 0;

		// contains all tours which are selected and not deleted
		int selectedValidTours = 0;

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
						selectedValidTours++;
					}

				} else {

					if (savedTours == 0) {
						firstSavedTour = tourData;
					}

					savedTours++;
					selectedValidTours++;
				}

				if (selectedValidTours == 1) {
					firstValidTour = tourData;
				}
			}
		}

		final boolean isTourSelected = savedTours > 0;
		final boolean isOneSavedAndValidTour = (selectedValidTours == 1) && (savedTours == 1);

		final boolean canMergeIntoTour = selectedValidTours == 1;

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
		if (canMergeIntoTour) {

			final Calendar calendar = GregorianCalendar.getInstance();
			calendar.set(
					firstValidTour.getStartYear(),
					firstValidTour.getStartMonth() - 1,
					firstValidTour.getStartDay(),
					firstValidTour.getStartHour(),
					firstValidTour.getStartMinute());

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
		_actionMergeIntoTour.setEnabled(canMergeIntoTour);

		_actionMergeTour.setEnabled(isOneSavedAndValidTour && (firstSavedTour.getMergeSourceTourId() != null));
		_actionReimportTour.setEnabled(selectedTours > 0);
		_actionRemoveTour.setEnabled(selectedTours > 0);
		_actionExportTour.setEnabled(selectedValidTours > 0);
		_actionJoinTours.setEnabled(selectedValidTours > 1);

		_actionEditTour.setEnabled(isOneSavedAndValidTour);
		_actionEditQuick.setEnabled(isOneSavedAndValidTour);
		_actionOpenTour.setEnabled(isOneSavedAndValidTour);
		_actionOpenMarkerDialog.setEnabled(isOneSavedAndValidTour);
		_actionOpenAdjustAltitudeDialog.setEnabled(isOneSavedAndValidTour);

		final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();
		_actionSetTourType.setEnabled(isTourSelected && (tourTypes.size() > 0));

		_actionAddTag.setEnabled(isTourSelected);

		Set<TourTag> existingTags = null;
		long existingTourTypeId = TourDatabase.ENTITY_IS_NOT_SAVED;

		if ((firstSavedTour != null) && (savedTours == 1)) {

			// one tour is selected

			final TourType tourType = firstSavedTour.getTourType();

			existingTags = firstSavedTour.getTourTags();
			existingTourTypeId = tourType == null ? TourDatabase.ENTITY_IS_NOT_SAVED : tourType.getTypeId();

			if ((existingTags != null) && (existingTags.size() > 0)) {

				// at least one tag is within the tour

				_actionRemoveAllTags.setEnabled(true);
				_actionRemoveTag.setEnabled(true);
			} else {
				// tags are not available
				_actionRemoveAllTags.setEnabled(false);
				_actionRemoveTag.setEnabled(false);
			}
		} else {

			// multiple tours are selected

			_actionRemoveTag.setEnabled(isTourSelected);
			_actionRemoveAllTags.setEnabled(isTourSelected);
		}

		// enable/disable actions for tags/tour types
		TagManager.enableRecentTagActions(isTourSelected, existingTags);
		TourTypeMenuManager.enableRecentTourTypeActions(isTourSelected, existingTourTypeId);
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
		menuMgr.add(_actionReimportTour);
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
		TourTypeMenuManager.fillMenuRecentTourTypes(menuMgr, this, true);

		// tour tag actions
		menuMgr.add(new Separator());
		menuMgr.add(_actionAddTag);
		TagManager.fillMenuRecentTags(menuMgr, this, true, true);
		menuMgr.add(_actionRemoveTag);
		menuMgr.add(_actionRemoveAllTags);
		menuMgr.add(_actionOpenTagPrefs);

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
//		tbm.add(fActionRefreshView);

		/*
		 * fill view menu
		 */
		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();

		menuMgr.add(_actionMergeGPXTours);
		menuMgr.add(_actionCreateTourIdWithTime);
		menuMgr.add(_actionDisableChecksumValidation);
		menuMgr.add(_actionAdjustImportedYear);

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

	public ColumnViewer getViewer() {
		return _tourViewer;
	}

	private void onOpenTourInEditor() {

		final Object firstElement = ((IStructuredSelection) _tourViewer.getSelection()).getFirstElement();

		if ((firstElement != null) && (firstElement instanceof TourData)) {
//			TourManager.getInstance().openTourInEditor(((TourData) firstElement).getTourId());
			TourManager.openTourEditor(true);
		}
	}

	private void onSelectionChanged(final ISelection selection) {

		if (!selection.isEmpty() && (selection instanceof SelectionDeletedTours)) {

			final SelectionDeletedTours tourSelection = (SelectionDeletedTours) selection;
			final ArrayList<ITourItem> removedTours = tourSelection.removedTours;

			if (removedTours.size() == 0) {
				return;
			}

			removeTours(removedTours);

			if (_isPartVisible) {

				RawDataManager.getInstance().updateTourDataFromDb(null);

				// update the table viewer
				reloadViewer();
			} else {
				_isViewerPersonDataDirty = true;
			}
		}
	}

	public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

		_viewerContainer.setRedraw(false);
		{
			_tourViewer.getTable().dispose();
			createTourViewer(_viewerContainer);
			_viewerContainer.layout();

			// update the viewer
			reloadViewer();
		}
		_viewerContainer.setRedraw(true);

		return _tourViewer;
	}

	/**
	 * update {@link TourData} from the database for all imported tours, displays a progress dialog
	 */
	public void reimportAllImportFiles() {

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
					false,
					new IRunnableWithProgress() {

						public void run(final IProgressMonitor monitor) throws InvocationTargetException,
								InterruptedException {

							reimportAllImportFilesRunnable(monitor, prevImportedFiles);
						}
					});

		} catch (final InvocationTargetException e) {
			e.printStackTrace();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * reimport previous imported tours
	 * 
	 * @param monitor
	 * @param importedFiles
	 */
	private void reimportAllImportFilesRunnable(final IProgressMonitor monitor, final String[] importedFiles) {

		int workedDone = 0;
		final int workedAll = importedFiles.length;

		if (monitor != null) {
			monitor.beginTask(Messages.import_data_importTours_task, workedAll);
		}

		final ArrayList<String> notImportedFiles = new ArrayList<String>();

		final RawDataManager rawDataMgr = RawDataManager.getInstance();

		rawDataMgr.getImportedTours().clear();
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
				if (rawDataMgr.importRawData(file, null, false, null)) {
					importedFileCounter++;
				} else {
					notImportedFiles.add(fileName);
				}
			}
		}

		if (importedFileCounter > 0) {

			rawDataMgr.updateTourDataFromDb(monitor);

			Display.getDefault().asyncExec(new Runnable() {
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

	public void reloadViewer() {

		// update tour data viewer
		_tourViewer.setInput(RawDataManager.getInstance().getImportedTours().values().toArray());
	}

	private void removeTours(final ArrayList<ITourItem> removedTours) {

		final HashMap<Long, TourData> tourMap = RawDataManager.getInstance().getImportedTours();

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

		final RawDataManager rawDataManager = RawDataManager.getInstance();

		// restore: set merge tracks status before the tours are imported
		final boolean isMergeTracks = _state.getBoolean(STATE_IS_MERGE_TRACKS);
		_actionMergeGPXTours.setChecked(isMergeTracks);
		rawDataManager.setMergeTracks(isMergeTracks);

		// restore: set merge tracks status before the tours are imported
		final boolean isCreateTourIdWithTime = _state.getBoolean(STATE_IS_CREATE_TOUR_ID_WITH_TIME);
		_actionCreateTourIdWithTime.setChecked(isCreateTourIdWithTime);
		rawDataManager.setCreateTourIdWithTime(isCreateTourIdWithTime);

		// restore: is checksum validation
		_actionDisableChecksumValidation.setChecked(_state.getBoolean(STATE_IS_CHECKSUM_VALIDATION));
		rawDataManager.setIsChecksumValidation(_actionDisableChecksumValidation.isChecked() == false);

		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {
				reimportAllImportFiles();
			}
		});
	}

	private void saveState() {

		// check if UI is disposed
		final Table table = _tourViewer.getTable();
		if (table.isDisposed()) {
			return;
		}

		// save imported file names
		final HashSet<String> importedFiles = RawDataManager.getInstance().getImportedFiles();
		_state.put(STATE_IMPORTED_FILENAMES, importedFiles.toArray(new String[importedFiles.size()]));

		// keep selected tours
		Util.setState(_state, STATE_SELECTED_TOUR_INDICES, table.getSelectionIndices());

		_state.put(STATE_IS_MERGE_TRACKS, _actionMergeGPXTours.isChecked());
		_state.put(STATE_IS_CHECKSUM_VALIDATION, _actionDisableChecksumValidation.isChecked());
		_state.put(STATE_IS_CREATE_TOUR_ID_WITH_TIME, _actionCreateTourIdWithTime.isChecked());

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

		final TourData savedTour = TourDatabase.saveTour(tourData);
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

		final Collection<TourData> tourDataCollection = RawDataManager.getInstance().getImportedTours().values();

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
				public void run() {
					fireSelectedTour();
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
		RawDataManager.getInstance().updateTourDataFromDb(null);

		_tourViewer.refresh();
	}
}
