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

package net.tourbook.ui.views.rawData;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourItem;
import net.tourbook.tour.ITourPropertyListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ActionModifyColumns;
import net.tourbook.ui.ActionSetTourType;
import net.tourbook.ui.ColumnManager;
import net.tourbook.ui.ISelectedTours;
import net.tourbook.ui.ITourViewer;
import net.tourbook.ui.TableColumnDefinition;
import net.tourbook.ui.TableColumnFactory;
import net.tourbook.ui.UI;
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
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.part.ViewPart;

/**
 * 
 */
public class RawDataView extends ViewPart implements ISelectedTours, ITourViewer {

	private static final String			FILESTRING_SEPARATOR		= "|";											//$NON-NLS-1$

	public static final String			ID							= "net.tourbook.views.rawData.RawDataView";	//$NON-NLS-1$

	public static final int				COLUMN_DATE					= 0;
	public static final int				COLUMN_TITLE				= 1;
	public static final int				COLUMN_DATA_FORMAT			= 2;

	private static final String			MEMENTO_SASH_CONTAINER		= "importview.sash.container.";				//$NON-NLS-1$
	private static final String			MEMENTO_IMPORT_FILENAME		= "importview.raw-data.filename";				//$NON-NLS-1$
	private static final String			MEMENTO_SELECTED_TOUR_INDEX	= "importview.selected-tour-index";			//$NON-NLS-1$
	private static final String			MEMENTO_COLUMN_SORT_ORDER	= "importview.column_sort_order";				//$NON-NLS-1$
	private static final String			MEMENTO_COLUMN_WIDTH		= "importview.column_width";					//$NON-NLS-1$

	private static IMemento				fSessionMemento;

	private TableViewer					fTourViewer;

	private ActionClearView				fActionClearView;
	private ActionModifyColumns			fActionModifyColumns;
	private ActionSaveTourInDatabase	fActionSaveTour;
	private ActionSaveTourInDatabase	fActionSaveTourWithPerson;
	private ActionAdjustYear			fActionAdjustImportedYear;
	private ActionSetTourType			fActionSetTourType;

	private ImageDescriptor				imageDatabaseDescriptor;
	private ImageDescriptor				imageDatabaseOtherPersonDescriptor;
	private ImageDescriptor				imageDatabasePlaceholderDescriptor;
	private Image						imageDatabase;
	private Image						imageDatabaseOtherPerson;
	private Image						imageDatabasePlaceholder;

	private IPartListener2				fPartListener;
	private ISelectionListener			fPostSelectionListener;
	private IPropertyChangeListener		fPrefChangeListener;
	private PostSelectionProvider		fPostSelectionProvider;
	private ITourPropertyListener		fTourPropertyListener;

	public Calendar						fCalendar					= GregorianCalendar.getInstance();
	private DateFormat					fDateFormatter				= DateFormat.getDateInstance(DateFormat.SHORT);
	private DateFormat					fTimeFormatter				= DateFormat.getTimeInstance(DateFormat.SHORT);
	private NumberFormat				fNumberFormatter			= NumberFormat.getNumberInstance();
	private DateFormat					fDurationFormatter			= DateFormat.getTimeInstance(DateFormat.SHORT,
																			Locale.GERMAN);

	protected TourPerson				fActivePerson;
	protected TourPerson				fNewActivePerson;

	protected boolean					fIsPartVisible				= false;
	protected boolean					fIsViewerPersonDataDirty	= false;

	private ColumnManager				fColumnManager;

	private Composite					fViewerContainer;

	private class TourDataContentProvider implements IStructuredContentProvider {

		public TourDataContentProvider() {}

		public void dispose() {}

		public Object[] getElements(final Object parent) {
			return (Object[]) (parent);
		}

		public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {}
	}

	private void addPartListener() {
		fPartListener = new IPartListener2() {
			public void partActivated(final IWorkbenchPartReference partRef) {
//				disableTourChartSelection();
			}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (ID.equals(partRef.getId())) {
					saveSettings();
				}
			}

			public void partDeactivated(final IWorkbenchPartReference partRef) {
				if (ID.equals(partRef.getId())) {
					saveSettings();
				}
			}

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
						updateViewer();
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

					saveSettings();

					fTourViewer.getTable().dispose();
					createTourViewer(fViewerContainer);
					fViewerContainer.layout();

					restoreState(fSessionMemento);

					// update the viewer
					updateViewer();
				}
			}
		};
		TourbookPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(fPrefChangeListener);
	}

	private void addSelectionListener() {

		fPostSelectionListener = new ISelectionListener() {
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				if (!selection.isEmpty() && selection instanceof SelectionDeletedTours) {

					final SelectionDeletedTours tourSelection = (SelectionDeletedTours) selection;
					final ArrayList<ITourItem> removedTours = tourSelection.removedTours;

					if (removedTours.size() == 0) {
						return;
					}

					if (fIsPartVisible) {

						RawDataManager.getInstance().updateTourDataFromDb();

						// update the table viewer
						updateViewer();
					} else {
						fIsViewerPersonDataDirty = true;
					}
				}
			}
		};
		getSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

	private void addTourPropertyListener() {

		fTourPropertyListener = new ITourPropertyListener() {
			@SuppressWarnings("unchecked") //$NON-NLS-1$
			public void propertyChanged(int propertyId, Object propertyData) {
				if (propertyId == TourManager.TOUR_PROPERTY_TOUR_TYPE_CHANGED) {

					// update modified tours
					ArrayList<TourData> modifiedTours = (ArrayList<TourData>) propertyData;

					fTourViewer.update(modifiedTours.toArray(), null);
				}
			}
		};
		TourManager.getInstance().addPropertyListener(fTourPropertyListener);
	}

	private void createActions() {

		fActionClearView = new ActionClearView(this);
		fActionModifyColumns = new ActionModifyColumns(this);
		fActionSaveTour = new ActionSaveTourInDatabase(this);
		fActionSaveTourWithPerson = new ActionSaveTourInDatabase(this);
		fActionSetTourType = new ActionSetTourType(this);
		fActionAdjustImportedYear = new ActionAdjustYear(this);

		/*
		 * fill view toolbar
		 */
		IToolBarManager viewTbm = getViewSite().getActionBars().getToolBarManager();

		viewTbm.add(new GroupMarker("import")); //$NON-NLS-1$

		viewTbm.add(fActionClearView);

		/*
		 * fill view menu
		 */
		IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();
		menuMgr.add(fActionModifyColumns);

		menuMgr.add(new Separator());
		menuMgr.add(fActionAdjustImportedYear);

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

		fViewerContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(fViewerContainer);

		createTourViewer(fViewerContainer);

		createActions();

		addPartListener();
		addSelectionListener();
		addPrefListener();
		addTourPropertyListener();

		// set this view part as selection provider
		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		fActivePerson = TourbookPlugin.getDefault().getActivePerson();

		restoreState(fSessionMemento);
	}

	private void createResources() {

		imageDatabaseDescriptor = TourbookPlugin.getImageDescriptor(Messages.Image__database);
		imageDatabaseOtherPersonDescriptor = TourbookPlugin.getImageDescriptor(Messages.Image__database_other_person);
		imageDatabasePlaceholderDescriptor = TourbookPlugin.getImageDescriptor(Messages.Image__database_placeholder);

		try {
			final Display display = Display.getCurrent();
			imageDatabase = (Image) imageDatabaseDescriptor.createResource(display);
			imageDatabaseOtherPerson = (Image) imageDatabaseOtherPersonDescriptor.createResource(display);
			imageDatabasePlaceholder = (Image) imageDatabasePlaceholderDescriptor.createResource(display);
		} catch (final DeviceResourceException e) {
			e.printStackTrace();
		}

	}

	/**
	 * @param parent
	 */
	private void createTourViewer(final Composite parent) {

		// parent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

		// table
		final Table table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);

		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		fTourViewer = new TableViewer(table);

		// define and create all columns
		fColumnManager = new ColumnManager(this);
		createTourViewerColumns(parent);
		fColumnManager.createColumns();

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
				IStructuredSelection selection = (IStructuredSelection) fTourViewer.getSelection();

				final TourData tourData = (TourData) selection.getFirstElement();

				if (tourData == null) {
					return;
				}

				fPostSelectionProvider.setSelection(new SelectionTourData(null, tourData));
			}
		});

		createContextMenu();
	}

	/**
	 * Defines all columns for the table viewer in the column manager
	 * 
	 * @param parent
	 */
	private void createTourViewerColumns(final Composite parent) {

		final PixelConverter pixelConverter = new PixelConverter(parent);

		TableColumnDefinition colDef;

		/*
		 * column: database indicator
		 */
		colDef = TableColumnFactory.DB_STATUS.createColumn(fColumnManager, pixelConverter);
//		colDef.setColumnResizable(false);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {

				// show the database indicator for the person who owns the tour
				final TourPerson tourPerson = ((TourData) cell.getElement()).getTourPerson();
				final long activePersonId = fActivePerson == null ? -1 : fActivePerson.getPersonId();

				cell.setImage(tourPerson == null
						? imageDatabasePlaceholder
						: tourPerson.getPersonId() == activePersonId ? imageDatabase : imageDatabaseOtherPerson);
			}
		});

		/*
		 * column: date
		 */
		colDef = TableColumnFactory.TOUR_DATE.createColumn(fColumnManager, pixelConverter);
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {

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
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {

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
//		colDef.setColumnResizable(false);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				final TourType tourType = ((TourData) cell.getElement()).getTourType();
				if (tourType != null) {
					cell.setImage(UI.getInstance().getTourTypeImage(tourType.getTypeId()));
				}
			}
		});

		/*
		 * column: recording time
		 */
		colDef = TableColumnFactory.RECORDING_TIME.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {

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
			public void update(ViewerCell cell) {

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
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				final int tourDistance = ((TourData) cell.getElement()).getTourDistance();
				if (tourDistance != 0) {
					fNumberFormatter.setMinimumFractionDigits(2);
					fNumberFormatter.setMaximumFractionDigits(2);
					cell.setText(fNumberFormatter.format(((float) tourDistance) / 1000 / UI.UNIT_VALUE_DISTANCE));
				}
			}
		});

		/*
		 * column: speed
		 */
		colDef = TableColumnFactory.SPEED.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
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
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
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
			public void update(ViewerCell cell) {
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
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
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

		colDef = TableColumnFactory.DEVICE_NAME.createColumn(fColumnManager, pixelConverter);
		colDef.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((DeviceImportSorter) fTourViewer.getSorter()).doSort(COLUMN_DATA_FORMAT);
				fTourViewer.refresh();
			}
		});

		TableColumnFactory.DEVICE_PROFILE.createColumn(fColumnManager, pixelConverter);
		TableColumnFactory.TIME_INTERVAL.createColumn(fColumnManager, pixelConverter);
		TableColumnFactory.IMPORT_FILE_NAME.createColumn(fColumnManager, pixelConverter);
		TableColumnFactory.IMPORT_FILE_PATH.createColumn(fColumnManager, pixelConverter);
	}

	@Override
	public void dispose() {

		if (imageDatabase != null) {
			imageDatabaseDescriptor.destroyResource(imageDatabase);
		}
		if (imageDatabaseOtherPerson != null) {
			imageDatabaseOtherPersonDescriptor.destroyResource(imageDatabaseOtherPerson);
		}
		if (imageDatabasePlaceholder != null) {
			imageDatabasePlaceholderDescriptor.destroyResource(imageDatabasePlaceholder);
		}

		getViewSite().getPage().removePartListener(fPartListener);
		getSite().getPage().removeSelectionListener(fPostSelectionListener);
		TourManager.getInstance().removePropertyListener(fTourPropertyListener);

		TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);

		super.dispose();
	}

	@SuppressWarnings("unchecked") //$NON-NLS-1$
	private void fillContextMenu(final IMenuManager menuMgr) {

		final IStructuredSelection tourSelection = (IStructuredSelection) fTourViewer.getSelection();

		if (!tourSelection.isEmpty()) {
			// menuMgr.add(new Action("Open the Tour") {
			// public void run() {
			// createChart(true);
			// }
			// });
		}

		if (tourSelection.isEmpty() == false) {

			int unsavedTours = 0;
			int savedTours = 0;
			for (final Iterator<TourData> iter = tourSelection.iterator(); iter.hasNext();) {
				final TourData tourData = iter.next();
				if (tourData.getTourPerson() == null) {
					unsavedTours++;
				} else {
					savedTours++;
				}
			}

			final TourPerson person = TourbookPlugin.getDefault().getActivePerson();
			if (person != null) {
				fActionSaveTourWithPerson.setText(NLS.bind(Messages.Raw_Data_Action_save_tour_with_person,
						person.getName()));
				fActionSaveTourWithPerson.setPerson(person);
				fActionSaveTourWithPerson.setEnabled(unsavedTours > 0);
				menuMgr.add(fActionSaveTourWithPerson);
			}

			if (tourSelection.size() == 1) {
				fActionSaveTour.setText(Messages.Raw_Data_Action_save_tour_for_person);
			} else {
				fActionSaveTour.setText(Messages.Raw_Data_Action_save_tours_for_person);
			}
			fActionSaveTour.setEnabled(unsavedTours > 0);
			menuMgr.add(fActionSaveTour);

			ArrayList<TourType> tourTypes = TourDatabase.getTourTypes();
			fActionSetTourType.setEnabled(savedTours > 0 && tourTypes.size() > 0);
			menuMgr.add(fActionSetTourType);
		}

		menuMgr.add(new Separator());
		menuMgr.add(fActionModifyColumns);

		// add standard group which allows other plug-ins to contribute here
		menuMgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	void fireSelectionEvent(final ISelection selection) {
		fPostSelectionProvider.setSelection(selection);
	}

	@SuppressWarnings("unchecked") //$NON-NLS-1$
	@Override
	public Object getAdapter(Class adapter) {

		if (adapter == ColumnViewer.class) {
			return fTourViewer;
		}

		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	public ColumnManager getColumnManager() {
		return fColumnManager;
	}

	public ArrayList<TourData> getSelectedTours() {

		// get selected tours
		final IStructuredSelection selectedTours = ((IStructuredSelection) fTourViewer.getSelection());

		ArrayList<TourData> selectedTourData = new ArrayList<TourData>();

		// loop: all selected tours
		for (Iterator<?> iter = selectedTours.iterator(); iter.hasNext();) {

			Object tourItem = iter.next();

			if (tourItem instanceof TourData) {

				TourData tourData = (TourData) tourItem;

				if (tourData.getTourPerson() != null) {
					selectedTourData.add(tourData);
				}
			}
		}

		return selectedTourData;
	}

	public TableViewer getTourViewer() {
		return fTourViewer;
	}

	public TreeViewer getTreeViewer() {
		return null;
	}

	@Override
	public void init(final IViewSite site, final IMemento memento) throws PartInitException {

		super.init(site, memento);

		// set the session memento
		if (fSessionMemento == null) {
			fSessionMemento = memento;
		}
	}

	public boolean isFromTourEditor() {
		return false;
	}

	private void restoreState(final IMemento memento) {

		if (memento != null) {

			// restore table columns sort order
			final String mementoColumnSortOrderIds = memento.getString(MEMENTO_COLUMN_SORT_ORDER);
			if (mementoColumnSortOrderIds != null) {
				fColumnManager.orderColumns(StringToArrayConverter.convertStringToArray(mementoColumnSortOrderIds));
			}

			// restore column width
			final String mementoColumnWidth = memento.getString(MEMENTO_COLUMN_WIDTH);
			if (mementoColumnWidth != null) {
				fColumnManager.setColumnWidth(StringToArrayConverter.convertStringToArray(mementoColumnWidth));
			}

			// restore imported tours
			final String mementoImportedFiles = memento.getString(MEMENTO_IMPORT_FILENAME);
			if (mementoImportedFiles != null) {

				final RawDataManager rawDataManager = RawDataManager.getInstance();

				rawDataManager.getTourDataMap().clear();

				String[] files = StringToArrayConverter.convertStringToArray(mementoImportedFiles, FILESTRING_SEPARATOR);
				int importCounter = 0;

				// loop: import all files
				for (String fileName : files) {

					if (rawDataManager.importRawData(fileName)) {
						importCounter++;
					}
				}

				if (importCounter > 0) {

					rawDataManager.updateTourDataFromDb();
					updateViewer();

					// restore selected tour
					final Integer selectedTourIndex = memento.getInteger(MEMENTO_SELECTED_TOUR_INDEX);

					Object tourData = fTourViewer.getElementAt(selectedTourIndex);
					if (tourData != null) {
						fTourViewer.setSelection(new StructuredSelection(tourData), true);
					}
				}
			}
		}
	}

	private void saveSettings() {
		fSessionMemento = XMLMemento.createWriteRoot("DeviceImportView"); //$NON-NLS-1$
		saveState(fSessionMemento);
	}

	@Override
	public void saveState(final IMemento memento) {

		// save sash weights
		final Table table = fTourViewer.getTable();

		if (table.isDisposed()) {
			return;
		}

		memento.putInteger(MEMENTO_SASH_CONTAINER, table.getSize().x);

		final RawDataManager rawDataMgr = RawDataManager.getInstance();

		// save imported file names
		final HashSet<String> importedFiles = rawDataMgr.getImportedFiles();
		memento.putString(MEMENTO_IMPORT_FILENAME,
				StringToArrayConverter.convertArrayToString(importedFiles.toArray(new String[importedFiles.size()]),
						FILESTRING_SEPARATOR));

		// save selected tour in the viewer
		memento.putInteger(MEMENTO_SELECTED_TOUR_INDEX, table.getSelectionIndex());

		// save column sort order
		memento.putString(MEMENTO_COLUMN_SORT_ORDER,
				StringToArrayConverter.convertArrayToString(fColumnManager.getColumnIds()));

		// save columns width
		final String[] columnIdAndWidth = fColumnManager.getColumnIdAndWidth();
		if (columnIdAndWidth != null) {
			memento.putString(MEMENTO_COLUMN_WIDTH, StringToArrayConverter.convertArrayToString(columnIdAndWidth));
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

	public void updateViewer() {

		// update tour data viewer
		fTourViewer.setInput(RawDataManager.getInstance().getTourDataMap().values().toArray());
	}

	/**
	 * when the active person was modified, the view must be updated
	 */
	private void updateViewerPersonData() {

		fActivePerson = TourbookPlugin.getDefault().getActivePerson();

		// update person in the raw data
		RawDataManager.getInstance().updateTourDataFromDb();

		fTourViewer.refresh();
	}
}
