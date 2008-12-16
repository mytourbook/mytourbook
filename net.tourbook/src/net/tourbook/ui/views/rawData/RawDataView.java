/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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
import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tag.ActionRemoveAllTags;
import net.tourbook.tag.ActionSetTourTag;
import net.tourbook.tag.TagManager;
import net.tourbook.tour.ActionOpenAdjustAltitudeDialog;
import net.tourbook.tour.ActionOpenMarkerDialog;
import net.tourbook.tour.DialogMergeTours;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.ITourItem;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ColumnDefinition;
import net.tourbook.ui.ColumnManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.ITourViewer;
import net.tourbook.ui.TableColumnFactory;
import net.tourbook.ui.UI;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.ui.action.ActionEditTour;
import net.tourbook.ui.action.ActionMergeTour;
import net.tourbook.ui.action.ActionModifyColumns;
import net.tourbook.ui.action.ActionOpenPrefDialog;
import net.tourbook.ui.action.ActionOpenTour;
import net.tourbook.ui.action.ActionSetTourTypeMenu;
import net.tourbook.util.PixelConverter;
import net.tourbook.util.PostSelectionProvider;
import net.tourbook.util.StringToArrayConverter;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.DeviceResourceException;
import org.eclipse.jface.resource.ImageDescriptor;
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
public class RawDataView extends ViewPart implements ITourProvider, ITourViewer {

	private static final String				FILESTRING_SEPARATOR			= "|";											//$NON-NLS-1$

	public static final String				ID								= "net.tourbook.views.rawData.RawDataView";	//$NON-NLS-1$

	final IDialogSettings					fViewState						= TourbookPlugin.getDefault()
																					.getDialogSettingsSection(ID);

	public static final int					COLUMN_DATE						= 0;
	public static final int					COLUMN_TITLE					= 1;
	public static final int					COLUMN_DATA_FORMAT				= 2;
	public static final int					COLUMN_FILE_NAME				= 3;

	private static final String				MEMENTO_SASH_CONTAINER			= "importview.sash.container.";				//$NON-NLS-1$
	private static final String				MEMENTO_IMPORT_FILENAME			= "importview.raw-data.filename";				//$NON-NLS-1$
	private static final String				MEMENTO_SELECTED_TOUR_INDEX		= "importview.selected-tour-index";			//$NON-NLS-1$

	private static final String				MEMENTO_MERGE_TRACKS			= "importview.action.merge-tracks";			//$NON-NLS-1$
	private static final String				MEMENTO_IS_CHECKSUM_VALIDATION	= "importview.action.is-checksum-validation";	//$NON-NLS-1$

	private TableViewer						fTourViewer;

	private ActionClearView					fActionClearView;
	private ActionModifyColumns				fActionModifyColumns;
	private ActionSaveTourInDatabase		fActionSaveTour;
	private ActionSaveTourInDatabase		fActionSaveTourWithPerson;
	private ActionMergeIntoMenu				fActionMergeIntoTour;
	private ActionAdjustYear				fActionAdjustImportedYear;
	private ActionMergeGPXTours				fActionMergeGPXTours;
	private ActionDisableChecksumValidation	fActionDisableChecksumValidation;
	private ActionSetTourTypeMenu			fActionSetTourType;
	private ActionEditQuick					fActionEditQuick;
	private ActionEditTour					fActionEditTour;
	private ActionMergeTour					fActionMergeTour;
	private ActionSetTourTag				fActionAddTag;
	private ActionSetTourTag				fActionRemoveTag;
	private ActionRemoveAllTags				fActionRemoveAllTags;
	private ActionOpenPrefDialog			fActionOpenTagPrefs;
	private ActionOpenTour					fActionOpenTour;
	private ActionOpenMarkerDialog			fActionOpenMarkerDialog;
	private ActionOpenAdjustAltitudeDialog	fActionOpenAdjustAltitudeDialog;

	private ImageDescriptor					imageDescDatabase;
	private ImageDescriptor					imageDescDatabaseOtherPerson;
	private ImageDescriptor					imageDescDatabaseAssignMergedTour;
	private ImageDescriptor					imageDescDatabasePlaceholder;
	private ImageDescriptor					imageDescDelete;
	private Image							imageDatabase;
	private Image							imageDatabaseOtherPerson;
	private Image							imageDatabaseAssignMergedTour;
	private Image							imageDatabasePlaceholder;
	private Image							imageDelete;

	private PostSelectionProvider			fPostSelectionProvider;
	private IPartListener2					fPartListener;
	private ISelectionListener				fPostSelectionListener;
	private IPropertyChangeListener			fPrefChangeListener;
	private ITourEventListener				fTourEventListener;

	public Calendar							fCalendar						= GregorianCalendar.getInstance();
	private DateFormat						fDateFormatter					= DateFormat.getDateInstance(DateFormat.SHORT);
	private DateFormat						fTimeFormatter					= DateFormat.getTimeInstance(DateFormat.SHORT);
	private NumberFormat					fNumberFormatter				= NumberFormat.getNumberInstance();
	private DateFormat						fDurationFormatter				= DateFormat.getTimeInstance(DateFormat.SHORT,
																					Locale.GERMAN);

	protected TourPerson					fActivePerson;
	protected TourPerson					fNewActivePerson;

	protected boolean						fIsPartVisible					= false;
	protected boolean						fIsViewerPersonDataDirty		= false;

	private ColumnManager					fColumnManager;

	private Composite						fViewerContainer;

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

		fPostSelectionProvider.setSelection(new SelectionDeletedTours());

		// don't throw the selection again
		fPostSelectionProvider.clearSelection();
	}

	void actionMergeTours(final TourData mergeFromTour, final TourData mergeIntoTour) {

		// check if the tour editor contains a modified tour
		if (UI.isTourEditorModified()) {
			return;
		}

		// backup data
		final Long backupMergeFromTourId = mergeIntoTour.getMergeFromTourId();

		// set tour data and tour id from which the tour is merged
		mergeIntoTour.setMergeFromTourId(mergeFromTour.getTourId());
		
		// set temp data, this is required by the dialog because the merge from tour could not be saved
		mergeIntoTour.setMergeFromTour(mergeFromTour);

		if (new DialogMergeTours(Display.getCurrent().getActiveShell(), mergeFromTour, mergeIntoTour).open() != Window.OK) {

			// dialog is canceled, restore modified values

			mergeIntoTour.setMergeFromTourId(backupMergeFromTourId);
		}

		// reset temp tour data
		mergeIntoTour.setMergeFromTour(null);
	}

	void actionSaveTour(final TourPerson person) {

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {

				final ArrayList<TourData> savedTours = new ArrayList<TourData>();

				// get selected tours
				final IStructuredSelection selection = ((IStructuredSelection) fTourViewer.getSelection());

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
		fPartListener = new IPartListener2() {

			public void partActivated(final IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == RawDataView.this) {

//					final TourDataEditorView tourEditor = TourManager.getTourDataEditor();
//					if (tourEditor != null) {
//
//						// hide tours which are originated from this view
//
//						tourEditor.clearEditorContent();
//					}

					saveState();

					// remove all tours
					RawDataManager.getInstance().removeAllTours();

					TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR, null, RawDataView.this);
//
//					// don't throw the selection again
//					fPostSelectionProvider.clearSelection();
				}
			}

			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			public void partHidden(final IWorkbenchPartReference partRef) {
				if (RawDataView.this == partRef.getPart(false)) {
					fIsPartVisible = false;
				}
			}

			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			public void partOpened(final IWorkbenchPartReference partRef) {}

			public void partVisible(final IWorkbenchPartReference partRef) {
				if (RawDataView.this == partRef.getPart(false)) {
					fIsPartVisible = true;
					if (fIsViewerPersonDataDirty || (fNewActivePerson != fActivePerson)) {
						reloadViewer();
						updateViewerPersonData();
						fNewActivePerson = fActivePerson;
						fIsViewerPersonDataDirty = false;
					}
				}
			}
		};
		getViewSite().getPage().addPartListener(fPartListener);
	}

	private void addPrefListener() {

		final Preferences prefStore = TourbookPlugin.getDefault().getPluginPreferences();

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(final Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {
					if (fIsPartVisible) {
						updateViewerPersonData();
					} else {
						// keep new active person until the view is visible
						fNewActivePerson = TourbookPlugin.getDefault().getActivePerson();
					}

				} else if (property.equals(ITourbookPreferences.TOUR_PERSON_LIST_IS_MODIFIED)) {
					fActionSaveTour.resetPeopleList();

				} else if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

					// update tour type in the raw data
					RawDataManager.getInstance().updateTourDataFromDb();

					fTourViewer.refresh();

				} else if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

					UI.updateUnits();

					fColumnManager.saveState(fViewState);
					fColumnManager.clearColumns();
					defineViewerColumns(fViewerContainer);

					fTourViewer = (TableViewer) recreateViewer(fTourViewer);

				} else if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

					fTourViewer.getTable()
							.setLinesVisible(prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

					fTourViewer.refresh();

					/*
					 * the tree must be redrawn because the styled text does not show with the new
					 * color
					 */
					fTourViewer.getTable().redraw();
				}
			}
		};

		prefStore.addPropertyChangeListener(fPrefChangeListener);
	}

	private void addSelectionListener() {

		fPostSelectionListener = new ISelectionListener() {
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				if (part == RawDataView.this) {
					return;
				}

				if (!selection.isEmpty() && selection instanceof SelectionDeletedTours) {

					final SelectionDeletedTours tourSelection = (SelectionDeletedTours) selection;
					final ArrayList<ITourItem> removedTours = tourSelection.removedTours;

					if (removedTours.size() == 0) {
						return;
					}

					removeTours(removedTours);

					if (fIsPartVisible) {

						RawDataManager.getInstance().updateTourDataFromDb();

						// update the table viewer
						reloadViewer();
					} else {
						fIsViewerPersonDataDirty = true;
					}
				}
			}
		};
		getSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

	private void addTourEventListener() {

		fTourEventListener = new ITourEventListener() {
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (part == RawDataView.this) {
					return;
				}

				if (eventId == TourEventId.TOUR_CHANGED && eventData instanceof TourEvent) {

					// update modified tours
					final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
					if (modifiedTours != null) {

						// update model
						RawDataManager.getInstance().updateTourDataModel(modifiedTours);

						// update viewer
						fTourViewer.update(modifiedTours.toArray(), null);

						// remove old selection, old selection can have the same tour but with old data
						fPostSelectionProvider.clearSelection();
					}

				} else if (eventId == TourEventId.TAG_STRUCTURE_CHANGED) {

					RawDataManager.getInstance().updateTourDataFromDb();

					reloadViewer();
				}
			}
		};
		TourManager.getInstance().addTourEventListener(fTourEventListener);
	}

	private void createActions() {

		// context menu
		fActionSaveTour = new ActionSaveTourInDatabase(this, false);
		fActionSaveTourWithPerson = new ActionSaveTourInDatabase(this, true);
		fActionMergeIntoTour = new ActionMergeIntoMenu(this);

		fActionEditTour = new ActionEditTour(this);
		fActionEditQuick = new ActionEditQuick(this);
		fActionMergeTour = new ActionMergeTour(this);
		fActionOpenTour = new ActionOpenTour(this);
		fActionSetTourType = new ActionSetTourTypeMenu(this);

		fActionOpenMarkerDialog = new ActionOpenMarkerDialog(this, true);
		fActionOpenAdjustAltitudeDialog = new ActionOpenAdjustAltitudeDialog(this, true);

		fActionAddTag = new ActionSetTourTag(this, true);
		fActionRemoveTag = new ActionSetTourTag(this, false);
		fActionRemoveAllTags = new ActionRemoveAllTags(this);

		fActionOpenTagPrefs = new ActionOpenPrefDialog(Messages.action_tag_open_tagging_structure,
				ITourbookPreferences.PREF_PAGE_TAGS);

		fActionClearView = new ActionClearView(this);

		// view menu
		fActionModifyColumns = new ActionModifyColumns(this);
		fActionMergeGPXTours = new ActionMergeGPXTours(this);
		fActionAdjustImportedYear = new ActionAdjustYear(this);
		fActionDisableChecksumValidation = new ActionDisableChecksumValidation(this);
	}

	private void createChart() {

		final Object firstElement = ((IStructuredSelection) fTourViewer.getSelection()).getFirstElement();

		if (firstElement != null && firstElement instanceof TourData) {
			TourManager.getInstance().createTour((TourData) firstElement);
		}
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

		final Menu menu = menuMgr.createContextMenu(fTourViewer.getControl());
		fTourViewer.getControl().setMenu(menu);

		getSite().registerContextMenu(menuMgr, fTourViewer);
	}

	@Override
	public void createPartControl(final Composite parent) {

		createResources();

		// define all columns
		fColumnManager = new ColumnManager(this, fViewState);
		defineViewerColumns(parent);

		fViewerContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(fViewerContainer);

		createTourViewer(fViewerContainer);

		createActions();
		fillToolbar();

		addPartListener();
		addSelectionListener();
		addPrefListener();
		addTourEventListener();

		// set this view part as selection provider
		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		fActivePerson = TourbookPlugin.getDefault().getActivePerson();

		restoreState();
	}

	private void createResources() {

		imageDescDatabase = TourbookPlugin.getImageDescriptor(Messages.Image__database);
		imageDescDatabaseOtherPerson = TourbookPlugin.getImageDescriptor(Messages.Image__database_other_person);
		imageDescDatabaseAssignMergedTour = TourbookPlugin.getImageDescriptor(Messages.Image__assignMergedTour);
		imageDescDatabasePlaceholder = TourbookPlugin.getImageDescriptor(Messages.Image__database_placeholder);
		imageDescDelete = TourbookPlugin.getImageDescriptor(Messages.Image__delete);

		try {
			final Display display = Display.getCurrent();
			imageDatabase = (Image) imageDescDatabase.createResource(display);
			imageDatabaseOtherPerson = (Image) imageDescDatabaseOtherPerson.createResource(display);
			imageDatabaseAssignMergedTour = (Image) imageDescDatabaseAssignMergedTour.createResource(display);
			imageDatabasePlaceholder = (Image) imageDescDatabasePlaceholder.createResource(display);
			imageDelete = (Image) imageDescDelete.createResource(display);
		} catch (final DeviceResourceException e) {
			e.printStackTrace();
		}

	}

	/**
	 * @param parent
	 */
	private void createTourViewer(final Composite parent) {

		final Preferences prefStore = TourbookPlugin.getDefault().getPluginPreferences();

		// table
		final Table table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);

		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.setHeaderVisible(true);
		table.setLinesVisible(prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

		fTourViewer = new TableViewer(table);
		fColumnManager.createColumns(fTourViewer);

		// table viewer
		fTourViewer.setContentProvider(new TourDataContentProvider());
		fTourViewer.setSorter(new DeviceImportSorter());

		fTourViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {
				createChart();
			}
		});

		fTourViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				final IStructuredSelection selection = (IStructuredSelection) fTourViewer.getSelection();

				final TourData tourData = (TourData) selection.getFirstElement();

				enableActions();

				if (tourData != null) {
					fPostSelectionProvider.setSelection(new SelectionTourData(null, tourData));
				}
			}
		});

		createContextMenu();
	}

	/**
	 * Defines all columns for the table viewer in the column manager
	 * 
	 * @param parent
	 */
	private void defineViewerColumns(final Composite parent) {

		final PixelConverter pixelConverter = new PixelConverter(parent);
		ColumnDefinition colDef;

		/*
		 * column: database indicator
		 */
		colDef = TableColumnFactory.DB_STATUS.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				// show the database indicator for the person who owns the tour
				cell.setImage(getDbImage((TourData) cell.getElement()));
			}
		});

		/*
		 * column: date
		 */
		colDef = TableColumnFactory.TOUR_DATE.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourData tourData = (TourData) cell.getElement();

				fCalendar.set(tourData.getStartYear(), tourData.getStartMonth() - 1, tourData.getStartDay());
				cell.setText(fDateFormatter.format(fCalendar.getTime()));
			}
		});
		colDef.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((DeviceImportSorter) fTourViewer.getSorter()).doSort(COLUMN_DATE);
				fTourViewer.refresh();
			}
		});

		/*
		 * column: time
		 */
		colDef = TableColumnFactory.TOUR_START_TIME.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourData tourData = (TourData) cell.getElement();
				fCalendar.set(0, 0, 0, tourData.getStartHour(), tourData.getStartMinute(), 0);

				cell.setText(fTimeFormatter.format(fCalendar.getTime()));
			}
		});
		colDef.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((DeviceImportSorter) fTourViewer.getSorter()).doSort(COLUMN_DATE);
				fTourViewer.refresh();
			}
		});

		/*
		 * column: tour type
		 */
		colDef = TableColumnFactory.TOUR_TYPE.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final TourType tourType = ((TourData) cell.getElement()).getTourType();
				if (tourType == null) {
					cell.setImage(null);
				} else {
					cell.setImage(UI.getInstance().getTourTypeImage(tourType.getTypeId()));
				}
			}
		});

		/*
		 * column: recording time
		 */
		colDef = TableColumnFactory.RECORDING_TIME.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final int recordingTime = ((TourData) cell.getElement()).getTourRecordingTime();

				if (recordingTime != 0) {
					fCalendar.set(0,
							0,
							0,
							recordingTime / 3600,
							((recordingTime % 3600) / 60),
							((recordingTime % 3600) % 60));

					cell.setText(fDurationFormatter.format(fCalendar.getTime()));
				}
			}
		});

		/*
		 * column: driving time
		 */
		colDef = TableColumnFactory.DRIVING_TIME.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final int drivingTime = ((TourData) cell.getElement()).getTourDrivingTime();

				if (drivingTime != 0) {
					fCalendar.set(0, 0, 0, drivingTime / 3600, ((drivingTime % 3600) / 60), ((drivingTime % 3600) % 60));

					cell.setText(fDurationFormatter.format(fCalendar.getTime()));
				}
			}
		});

		/*
		 * column: distance (km/mile)
		 */
		colDef = TableColumnFactory.DISTANCE.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final int tourDistance = ((TourData) cell.getElement()).getTourDistance();
				if (tourDistance != 0) {
					fNumberFormatter.setMinimumFractionDigits(3);
					fNumberFormatter.setMaximumFractionDigits(3);
					cell.setText(fNumberFormatter.format(((float) tourDistance) / 1000 / UI.UNIT_VALUE_DISTANCE));
				}
			}
		});

		/*
		 * column: avg speed
		 */
		colDef = TableColumnFactory.AVG_SPEED.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final TourData tourData = ((TourData) cell.getElement());
				final int tourDistance = tourData.getTourDistance();
				final int drivingTime = tourData.getTourDrivingTime();
				if (drivingTime != 0) {
					fNumberFormatter.setMinimumFractionDigits(1);
					fNumberFormatter.setMaximumFractionDigits(1);
					cell.setText(fNumberFormatter.format(((float) tourDistance)
							/ drivingTime
							* 3.6
							/ UI.UNIT_VALUE_DISTANCE));
				}
			}
		});

		/*
		 * column: altitude up
		 */
		colDef = TableColumnFactory.ALTITUDE_UP.createColumn(fColumnManager, pixelConverter);
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

		/*
		 * column: altitude down
		 */
		colDef = TableColumnFactory.ALTITUDE_DOWN.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final int tourAltDown = ((TourData) cell.getElement()).getTourAltDown();
				if (tourAltDown != 0) {
					cell.setText(Long.toString((long) (-tourAltDown / UI.UNIT_VALUE_ALTITUDE)));
				}
			}
		});

		/*
		 * column: tour title
		 */
		colDef = TableColumnFactory.TOUR_TITLE.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final TourData tourData = (TourData) cell.getElement();
				cell.setText(tourData.getTourTitle());
			}
		});
		colDef.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((DeviceImportSorter) fTourViewer.getSorter()).doSort(COLUMN_TITLE);
				fTourViewer.refresh();
			}
		});

		/*
		 * column: tags
		 */
		colDef = TableColumnFactory.TOUR_TAGS.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
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

		/*
		 * column: device name
		 */
		colDef = TableColumnFactory.DEVICE_NAME.createColumn(fColumnManager, pixelConverter);
		colDef.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((DeviceImportSorter) fTourViewer.getSorter()).doSort(COLUMN_DATA_FORMAT);
				fTourViewer.refresh();
			}
		});

		/*
		 * column: device profile
		 */
		TableColumnFactory.DEVICE_PROFILE.createColumn(fColumnManager, pixelConverter);

		/*
		 * column: time interval
		 */
		TableColumnFactory.TIME_INTERVAL.createColumn(fColumnManager, pixelConverter);

		/*
		 * column: import file name
		 */
		colDef = TableColumnFactory.IMPORT_FILE_NAME.createColumn(fColumnManager, pixelConverter);
		colDef.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((DeviceImportSorter) fTourViewer.getSorter()).doSort(COLUMN_FILE_NAME);
				fTourViewer.refresh();
			}
		});

		TableColumnFactory.IMPORT_FILE_PATH.createColumn(fColumnManager, pixelConverter);
	}

	@Override
	public void dispose() {

		if (imageDatabase != null) {
			imageDescDatabase.destroyResource(imageDatabase);
		}
		if (imageDatabaseOtherPerson != null) {
			imageDescDatabaseOtherPerson.destroyResource(imageDatabaseOtherPerson);
		}
		if (imageDatabaseAssignMergedTour != null) {
			imageDescDatabaseAssignMergedTour.destroyResource(imageDatabaseAssignMergedTour);
		}
		if (imageDatabasePlaceholder != null) {
			imageDescDatabasePlaceholder.destroyResource(imageDatabasePlaceholder);
		}
		if (imageDelete != null) {
			imageDescDelete.destroyResource(imageDelete);
		}

		// don't throw the selection again
		fPostSelectionProvider.clearSelection();

		getViewSite().getPage().removePartListener(fPartListener);
		getSite().getPage().removeSelectionListener(fPostSelectionListener);

		TourManager.getInstance().removeTourEventListener(fTourEventListener);

		TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);

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
		final HashMap<Long, TourData> rawDataMap = RawDataManager.getInstance().getTourDataMap();
		for (final TourData tourData : savedTours) {

			final Long tourId = tourData.getTourId();

			rawDataMap.put(tourId, tourData);
			savedToursIds.add(tourId);
		}

		/*
		 * the selection provider can contain old tour data which conflicts with the tour data in
		 * the tour data editor
		 */
		fPostSelectionProvider.clearSelection();

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

		final StructuredSelection selection = (StructuredSelection) fTourViewer.getSelection();

		int savedTours = 0;
		int unsavedTours = 0;
		int selectedValidTours = 0;

		TourData firstSavedTour = null;
		TourData firstValidTour = null;

		for (final Iterator<?> iter = selection.iterator(); iter.hasNext();) {
			final Object treeItem = iter.next();
			if (treeItem instanceof TourData) {

				final TourData tourData = (TourData) treeItem;
				if (tourData.getTourPerson() == null) {

					// tour is not saved

					if (tourData.isTourDeleted == false) {

						// deleted tours are ignored, tour is not deleted

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
		final boolean isOneSavedAndValidTour = selectedValidTours == 1 && savedTours == 1;

		final boolean canMergeIntoTour = selectedValidTours == 1
				&& (firstValidTour == null ? true : firstValidTour.getMergeFromTourId() == null);

		// action: save tour with person
		final TourPerson person = TourbookPlugin.getDefault().getActivePerson();
		if (person != null) {
			fActionSaveTourWithPerson.setText(NLS.bind(Messages.import_data_action_save_tour_with_person,
					person.getName()));
			fActionSaveTourWithPerson.setPerson(person);
		}
		fActionSaveTourWithPerson.setEnabled(person != null && unsavedTours > 0);

		// action: save tour...
		if (selection.size() == 1) {
			fActionSaveTour.setText(Messages.import_data_action_save_tour_for_person);
		} else {
			fActionSaveTour.setText(Messages.import_data_action_save_tours_for_person);
		}
		fActionSaveTour.setEnabled(unsavedTours > 0);

		fActionMergeIntoTour.setEnabled(canMergeIntoTour);
		fActionMergeTour.setEnabled(isOneSavedAndValidTour && firstSavedTour.getMergeFromTourId() != null);

		fActionEditTour.setEnabled(isOneSavedAndValidTour);
		fActionEditQuick.setEnabled(isOneSavedAndValidTour);
		fActionOpenTour.setEnabled(isOneSavedAndValidTour);
		fActionOpenMarkerDialog.setEnabled(isOneSavedAndValidTour);
		fActionOpenAdjustAltitudeDialog.setEnabled(isOneSavedAndValidTour);

		final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();
		fActionSetTourType.setEnabled(isTourSelected && tourTypes.size() > 0);

		fActionAddTag.setEnabled(isTourSelected);

		/*
		 * enable/disable remove actions
		 */
		if (firstSavedTour != null && savedTours == 1) {

			// one tour is selected

			final Set<TourTag> tourTags = firstSavedTour.getTourTags();
			if (tourTags != null && tourTags.size() > 0) {

				// at least one tag is within the tour

				fActionRemoveAllTags.setEnabled(true);
				fActionRemoveTag.setEnabled(true);
			} else {
				// tags are not available
				fActionRemoveAllTags.setEnabled(false);
				fActionRemoveTag.setEnabled(false);
			}
		} else {

			// multiple tours are selected

			fActionRemoveTag.setEnabled(isTourSelected);
			fActionRemoveAllTags.setEnabled(isTourSelected);
		}

		// enable/disable actions for the recent tags
		TagManager.enableRecentTagActions(isTourSelected);

	}

	private void fillContextMenu(final IMenuManager menuMgr) {

		if (TourbookPlugin.getDefault().getActivePerson() != null) {
			menuMgr.add(fActionSaveTourWithPerson);
		}
		menuMgr.add(fActionSaveTour);
		menuMgr.add(fActionMergeIntoTour);

		menuMgr.add(new Separator());
		menuMgr.add(fActionEditQuick);
		menuMgr.add(fActionEditTour);
		menuMgr.add(fActionOpenMarkerDialog);
		menuMgr.add(fActionOpenAdjustAltitudeDialog);
		menuMgr.add(fActionMergeTour);
		menuMgr.add(fActionOpenTour);

		menuMgr.add(new Separator());
		menuMgr.add(fActionSetTourType);
		menuMgr.add(fActionAddTag);
		menuMgr.add(fActionRemoveTag);
		menuMgr.add(fActionRemoveAllTags);
		TagManager.fillRecentTagsIntoMenu(menuMgr, this, true, true);
		menuMgr.add(fActionOpenTagPrefs);

		// add standard group which allows other plug-ins to contribute here
		menuMgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

		enableActions();
	}

	private void fillToolbar() {
		/*
		 * fill view toolbar
		 */
		final IToolBarManager viewTbm = getViewSite().getActionBars().getToolBarManager();

		viewTbm.add(fActionSaveTourWithPerson);
		viewTbm.add(fActionSaveTour);
		viewTbm.add(new Separator());

		// place for import and transfer actions
		viewTbm.add(new GroupMarker("import")); //$NON-NLS-1$
		viewTbm.add(new Separator());

		viewTbm.add(fActionClearView);

		/*
		 * fill view menu
		 */
		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();

		menuMgr.add(fActionMergeGPXTours);
		menuMgr.add(fActionDisableChecksumValidation);
		menuMgr.add(fActionAdjustImportedYear);

		menuMgr.add(new Separator());
		menuMgr.add(fActionModifyColumns);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object getAdapter(final Class adapter) {

		if (adapter == ColumnViewer.class) {
			return fTourViewer;
		}

		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	/**
	 * @return all selected tour this includes tours which are not saved
	 */
	ArrayList<TourData> getAllSelectedTours() {

		final TourManager tourManager = TourManager.getInstance();

		// get selected tours
		final IStructuredSelection selectedTours = ((IStructuredSelection) fTourViewer.getSelection());

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
		return fColumnManager;
	}

	Image getDbImage(final TourData tourData) {
		final TourPerson tourPerson = tourData.getTourPerson();
		final long activePersonId = fActivePerson == null ? -1 : fActivePerson.getPersonId();

		final Image dbImage = tourData.isTourDeleted ? //
				imageDelete
				: tourData.getMergeIntoTourId() != null ? //
						imageDatabaseAssignMergedTour
						: tourPerson == null ? imageDatabasePlaceholder : tourPerson.getPersonId() == activePersonId
								? imageDatabase
								: imageDatabaseOtherPerson;
		return dbImage;
	}

	public ArrayList<TourData> getSelectedTours() {

		final TourManager tourManager = TourManager.getInstance();

		// get selected tours
		final IStructuredSelection selectedTours = ((IStructuredSelection) fTourViewer.getSelection());

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
		return fTourViewer;
	}

	private void importFiles() {

		// restore imported tours
		final String mementoImportedFiles = fViewState.get(MEMENTO_IMPORT_FILENAME);
		final ArrayList<String> notImportedFiles = new ArrayList<String>();

		if (mementoImportedFiles != null) {

			final RawDataManager rawDataMgr = RawDataManager.getInstance();

			rawDataMgr.getTourDataMap().clear();

			final String[] files = StringToArrayConverter.convertStringToArray(mementoImportedFiles,
					FILESTRING_SEPARATOR);
			int importCounter = 0;

			// loop: import all files
			for (final String fileName : files) {

				final File file = new File(fileName);
				if (file.exists()) {
					if (rawDataMgr.importRawData(file, null, false, null)) {
						importCounter++;
					} else {
						notImportedFiles.add(fileName);
					}
				}
			}

			if (importCounter > 0) {

				rawDataMgr.updateTourDataFromDb();
				reloadViewer();

				// restore selected tour
				try {
					final Integer selectedTourIndex = fViewState.getInt(MEMENTO_SELECTED_TOUR_INDEX);

					final Object tourData = fTourViewer.getElementAt(selectedTourIndex);

					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							if (tourData != null) {
								fTourViewer.setSelection(new StructuredSelection(tourData), true);
							}
						}
					});

				} catch (final NumberFormatException e) {}
			}
		}

		if (notImportedFiles.size() > 0) {
			RawDataManager.showMsgBoxInvalidFormat(notImportedFiles);
		}

	}

	public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

		fViewerContainer.setRedraw(false);
		{
			fTourViewer.getTable().dispose();
			createTourViewer(fViewerContainer);
			fViewerContainer.layout();

			// update the viewer
			reloadViewer();
		}
		fViewerContainer.setRedraw(true);

		return fTourViewer;
	}

	public void reloadViewer() {

		// update tour data viewer
		fTourViewer.setInput(RawDataManager.getInstance().getTourDataMap().values().toArray());
	}

	private void removeTours(final ArrayList<ITourItem> removedTours) {

		final HashMap<Long, TourData> tourMap = RawDataManager.getInstance().getTourDataMap();

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
		final boolean isMergeTracks = fViewState.getBoolean(MEMENTO_MERGE_TRACKS);
		fActionMergeGPXTours.setChecked(isMergeTracks);
		rawDataManager.setMergeTracks(isMergeTracks);

		// restore: is checksum validation
		fActionDisableChecksumValidation.setChecked(fViewState.getBoolean(MEMENTO_IS_CHECKSUM_VALIDATION));
		rawDataManager.setIsChecksumValidation(fActionDisableChecksumValidation.isChecked() == false);

		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {
				importFiles();
			}
		});
	}

	private void saveState() {

		// save sash weights
		final Table table = fTourViewer.getTable();

		if (table.isDisposed()) {
			return;
		}

		fViewState.put(MEMENTO_SASH_CONTAINER, table.getSize().x);

		final RawDataManager rawDataMgr = RawDataManager.getInstance();

		// save imported file names
		final HashSet<String> importedFiles = rawDataMgr.getImportedFiles();
		fViewState.put(MEMENTO_IMPORT_FILENAME,
				StringToArrayConverter.convertArrayToString(importedFiles.toArray(new String[importedFiles.size()]),
						FILESTRING_SEPARATOR));

		// save selected tour in the viewer
		fViewState.put(MEMENTO_SELECTED_TOUR_INDEX, table.getSelectionIndex());

		fViewState.put(MEMENTO_MERGE_TRACKS, fActionMergeGPXTours.isChecked());
		fViewState.put(MEMENTO_IS_CHECKSUM_VALIDATION, fActionDisableChecksumValidation.isChecked());

		fColumnManager.saveState(fViewState);
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

		if (tourData.getTourPerson() != null && isForceSave == false) {
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

		final TourData firstTourData = (TourData) fTourViewer.getElementAt(0);
		if (firstTourData != null) {
			fTourViewer.setSelection(new StructuredSelection(firstTourData), true);
		}
	}

	void selectLastTour() {

		final Collection<TourData> tourDataCollection = RawDataManager.getInstance().getTourDataMap().values();

		final TourData[] tourList = tourDataCollection.toArray(new TourData[tourDataCollection.size()]);

		// select the last tour in the viewer
		if (tourList.length > 0) {
			final TourData tourData = tourList[0];
			fTourViewer.setSelection(new StructuredSelection(tourData), true);
		}
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
	public void setFocus() {
		fTourViewer.getControl().setFocus();
	}

	/**
	 * when the active person was modified, the view must be updated
	 */
	private void updateViewerPersonData() {

		fActivePerson = TourbookPlugin.getDefault().getActivePerson();

		// update person in save action
		enableActions();

		// update person in the raw data
		RawDataManager.getInstance().updateTourDataFromDb();

		fTourViewer.refresh();
	}
}
