/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

import net.tourbook.Messages;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ColorCache;
import net.tourbook.chart.ISliderMoveListener;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.importdata.TourbookDevice;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.IDataModelListener;
import net.tourbook.tour.ITourItem;
import net.tourbook.tour.SelectionTourChart;
import net.tourbook.tour.TourChart;
import net.tourbook.tour.TourChartConfiguration;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ActionModifyColumns;
import net.tourbook.ui.ColumnDefinition;
import net.tourbook.ui.ColumnManager;
import net.tourbook.ui.ViewerDetailForm;
import net.tourbook.ui.views.tourBook.DrawingColors;
import net.tourbook.ui.views.tourBook.SelectionRemovedTours;
import net.tourbook.util.PixelConverter;
import net.tourbook.util.PostSelectionProvider;
import net.tourbook.util.StringToArrayConverter;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.resource.DeviceResourceException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CellLabelProvider;
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
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.ToolBar;
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
 * @author user081647
 */
public class RawDataView extends ViewPart {

	public static final String				ID							= "net.tourbook.views.rawData.RawDataView"; //$NON-NLS-1$

	private static final String				TOUR_TYPE_PREFIX			= "tourType";								//$NON-NLS-1$

	public static final int					COLUMN_DATE					= 0;

	public static final int					COLUMN_ID_DATE				= 10;
	public static final int					COLUMN_ID_START_TIME		= 20;
	public static final int					COLUMN_ID_RECORDING_TIME	= 30;
	public static final int					COLUMN_ID_DRIVING_TIME		= 40;
	public static final int					COLUMN_ID_DISTANCE			= 50;
	public static final int					COLUMN_ID_SPEED				= 60;
	public static final int					COLUMN_ID_ALTITUDE			= 70;
	public static final int					COLUMN_ID_DEVICE_PROFILE	= 80;
	public static final int					COLUMN_ID_TIME_INTERVAL		= 90;
	private static final int				COLUMN_ID_DB_ICON			= 100;
	private static final int				COLUMN_ID_TOUR_TYPE			= 110;
	private static final int				COLUMN_ID_IMPORT_FILE		= 120;
	private static final int				COLUMN_ID_DEVICE_NAME		= 130;
	private static final int                COLUMN_ID_TOUR_TITLE		= 140;

	private static final String				MEMENTO_SASH_CONTAINER		= "importview.sash.container.";			//$NON-NLS-1$
	private static final String				MEMENTO_IMPORT_FILENAME		= "importview.raw-data.filename";			//$NON-NLS-1$
	private static final String				MEMENTO_SELECTED_TOUR_INDEX	= "importview.selected-tour-index";		//$NON-NLS-1$
	private static final String				MEMENTO_IS_CHART_VISIBLE	= "importview.is-chart-visible";			//$NON-NLS-1$
	private static final String				MEMENTO_COLUMN_SORT_ORDER	= "importview.column_sort_order";
	private static final String				MEMENTO_COLUMN_WIDTH		= "importview.column_width";

	private static IMemento					fSessionMemento;

	private ViewerDetailForm				fViewerDetailForm;
	private TableViewer						fTourViewer;

	private TourChart						fTourChart;
	private TourChartConfiguration			fTourChartConfig;

	private ActionSaveRawData				fActionSaveRawDataFile;
	private ActionImportFromFile			fActionImportTourFromFile;
	private ActionClearView					fActionClearView;
	private ActionModifyColumns				fActionModifyColumns;

	private ActionSaveTourInDatabase		fActionSaveTour;
	private ActionSaveTourInDatabase		fActionSaveTourWithPerson;

	private ActionShowViewDetails			fActionShowTourChart;

	private ImageDescriptor					imageDatabaseDescriptor;
	private ImageDescriptor					imageDatabaseOtherPersonDescriptor;
	private ImageDescriptor					imageDatabasePlaceholderDescriptor;
	private Image							imageDatabase;
	private Image							imageDatabaseOtherPerson;
	private Image							imageDatabasePlaceholder;

	private IPartListener2					fPartListener;
	private ISelectionListener				fPostSelectionListener;
	private IPropertyChangeListener			fPrefChangeListener;
	private PostSelectionProvider			fPostSelectionProvider;

	// protected TourEditorPart currentTourEditor;

	public Calendar							calendar;
	public DateFormat						dateInstance;
	public DateFormat						timeInstance;
	private DateFormat						durationInstance;
	private NumberFormat					numberInstance;

	private ToolBarManager					fTbm;

	private ViewForm						tourForm;

	/**
	 * status if the tour chart is displayed
	 */
// private Label fLblRawDataSource;
	protected TourPerson					fActivePerson;
	protected TourPerson					fNewActivePerson;

	protected boolean						fIsPartVisible				= false;
	protected boolean						fIsViewerPersonDataDirty	= false;

	private final HashMap<String, Image>	fImages						= new HashMap<String, Image>();
	private final ColorCache				fColorCache					= new ColorCache();

	private int								fColorImageHeight			= -1;
	private int								fColorImageWidth;

	/**
	 * device which was used to import the data, it's set to <code>null</code> when the import was
	 * not successful
	 */
	private TourbookDevice					fImportDevice;

	private ColumnManager					fColumnManager;

	private class TourDataContentProvider implements IStructuredContentProvider {

		public TourDataContentProvider() {}

		public void dispose() {}

		public Object[] getElements(final Object parent) {
			return (Object[]) (parent);
		}

		public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {}
	}

	private void addPrefListener() {
		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(final Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				/*
				 * set a new chart configuration when the preferences has changed
				 */
				if (property.equals(ITourbookPreferences.GRAPH_VISIBLE) || property
						.equals(ITourbookPreferences.GRAPH_X_AXIS)
						|| property.equals(ITourbookPreferences.GRAPH_X_AXIS_STARTTIME)) {

					fTourChartConfig = TourManager.createTourChartConfiguration();
				}

				if (property.equals(ITourbookPreferences.APP_NEW_DATA_FILTER)) {
					if (fIsPartVisible) {
						updateViewerPersonData();
					} else {
						// keep new active person until the view is visible
						fNewActivePerson = TourbookPlugin.getDefault().getActivePerson();
					}
				}

				if (property.equals(ITourbookPreferences.TOUR_PERSON_LIST_IS_MODIFIED)) {
					fActionSaveTour.resetPeopleList();
				}

				if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

					// update tour type in the raw data
					RawDataManager.getInstance().updatePersonInRawData();

					disposeImages();
					fTourViewer.refresh();
				}

			}
		};
		TourbookPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(
				fPrefChangeListener);
	}

	/**
	 * Set the size for the tour type images
	 * 
	 * @param display
	 * @return
	 */
	private void ensureImageSize(final Display display) {

		if (fColorImageHeight == -1) {

			final Table table = fTourViewer.getTable();

			// fColorImageHeight = table.getItemHeight();
			// fColorImageWidth = 10;

			fColorImageWidth = 16;
			fColorImageHeight = table.getItemHeight();
		}
	}

	private Image getTourTypeImage(final TourData tourData) {

		final TourType tourType = tourData.getTourType();

		if (tourType == null) {
			return null;
		}

		final long typeId = tourType.getTypeId();

		final String colorId = TOUR_TYPE_PREFIX + typeId;
		Image image = fImages.get(colorId);

		if (image == null) {

			final Display display = Display.getCurrent();
			ensureImageSize(display);
			image = new Image(display, fColorImageWidth, fColorImageHeight);

			final GC gc = new GC(image);
			{
				final int arcSize = 4;

				final DrawingColors drawingColors = getTourTypeDrawingColors(display, typeId);

				final Color colorBright = drawingColors.colorBright;

				if (colorBright != null) {

					gc.setForeground(colorBright);
					gc.setBackground(drawingColors.colorDark);

					// gc.setAlpha(0x0);
					// gc.fillRectangle(0, 0, fColorImageWidth, fColorImageHeight);

					gc.setAlpha(0xff);
					gc.fillGradientRectangle(4, 1, 8, fColorImageHeight - 3, false);

					gc.setForeground(drawingColors.colorLine);
					gc.drawRoundRectangle(4, 0, 7, fColorImageHeight - 2, arcSize, arcSize);

				}
			}
			gc.dispose();

			fImages.put(colorId, image);
		}

		return image;
	}

	/**
	 * @param display
	 * @param graphColor
	 * @return return the color for the graph
	 */
	private DrawingColors getTourTypeDrawingColors(final Display display, final long tourTypeId) {

		final String colorIdBright = TOUR_TYPE_PREFIX + "bright" + tourTypeId; //$NON-NLS-1$
		final String colorIdDark = TOUR_TYPE_PREFIX + "dark" + tourTypeId; //$NON-NLS-1$
		final String colorIdLine = TOUR_TYPE_PREFIX + "line" + tourTypeId; //$NON-NLS-1$

		final DrawingColors drawingColors = new DrawingColors();

		Color colorBright = fColorCache.get(colorIdBright);
		Color colorDark = fColorCache.get(colorIdDark);
		Color colorLine = fColorCache.get(colorIdLine);

		if (colorBright == null) {

			final ArrayList<TourType> tourTypes = TourbookPlugin.getDefault().getTourTypes();

			TourType tourTypeColors = null;

			for (final TourType tourType : tourTypes) {
				if (tourType.getTypeId() == tourTypeId) {
					tourTypeColors = tourType;
				}
			}

			if (tourTypeColors == null || tourTypeColors.getTypeId() == TourType.TOUR_TYPE_ID_NOT_DEFINED) {

				// tour type was not found

				colorBright = display.getSystemColor(SWT.COLOR_WHITE);
				colorDark = display.getSystemColor(SWT.COLOR_WHITE);
				colorLine = display.getSystemColor(SWT.COLOR_DARK_GRAY);

			} else {

				colorBright = fColorCache.put(colorIdBright, tourTypeColors.getRGBBright());

				colorDark = fColorCache.put(colorIdDark, tourTypeColors.getRGBDark());
				colorLine = fColorCache.put(colorIdLine, tourTypeColors.getRGBLine());
			}
		}

		drawingColors.colorBright = colorBright;
		drawingColors.colorDark = colorDark;
		drawingColors.colorLine = colorLine;

		return drawingColors;
	}

	private void addSelectionListener() {
		// set the selection listener
		fPostSelectionListener = new ISelectionListener() {
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				if (!selection.isEmpty() && selection instanceof SelectionRemovedTours) {

					final SelectionRemovedTours tourSelection = (SelectionRemovedTours) selection;
					final ArrayList<ITourItem> removedTours = tourSelection.removedTours;

					if (removedTours.size() == 0) {
						return;
					}

					if (fIsPartVisible) {

						RawDataManager.getInstance().updatePersonInRawData();

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

	private void addPartListener() {
		fPartListener = new IPartListener2() {
			public void partActivated(final IWorkbenchPartReference partRef) {

				disableTourChartSelection();

				// IWorkbenchPart part = partRef.getPart(false);
				// if (part instanceof TourEditorPart && part !=
				// currentTourEditor)
				// {
				// currentTourEditor = (TourEditorPart) part;
				// selectTourInView();
				// }
			}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (ID.equals(partRef.getId()))
					saveSettings();

				RawDataManager.getInstance().resetData();
			}

			public void partDeactivated(final IWorkbenchPartReference partRef) {
				if (ID.equals(partRef.getId()))
					saveSettings();
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
						updateViewerPersonData();
						fNewActivePerson = fActivePerson;
						fIsViewerPersonDataDirty = false;
					}
				}
			}
		};
		getViewSite().getPage().addPartListener(fPartListener);
	}

	private void createActions() {

		// toolbar: left side
		fActionSaveRawDataFile = new ActionSaveRawData(this);
		fActionClearView = new ActionClearView(this);
		fActionModifyColumns = new ActionModifyColumns(fColumnManager);
		fActionImportTourFromFile = new ActionImportFromFile();
		fActionSaveTour = new ActionSaveTourInDatabase(this);
		fActionSaveTourWithPerson = new ActionSaveTourInDatabase(this);
		fActionShowTourChart = new ActionShowViewDetails(this);

		fTbm.add(fActionSaveRawDataFile);
		fTbm.add(fActionImportTourFromFile);
		fTbm.add(fActionClearView);
		fTbm.add(new Separator());
		fTbm.add(fActionModifyColumns);

		fTbm.update(true);

		// view actions
		final IToolBarManager viewTbm = getViewSite().getActionBars().getToolBarManager();
		viewTbm.add(fActionShowTourChart);
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

	private void createDeviceData(final Composite parent) {

// final Composite deviceContainer = new Composite(parent, SWT.NONE);
// deviceContainer.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
//
// final GridLayout gl = new GridLayout();
// gl.marginHeight = 1;
// gl.marginWidth = 2;
// deviceContainer.setLayout(gl);
//
// fLblRawDataSource = new Label(deviceContainer, SWT.NONE);
// fLblRawDataSource.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
// fLblRawDataSource.addControlListener(new ControlAdapter() {
// public void controlResized(final ControlEvent e) {
	// recalculate the label
// updateDeviceData();
// fLblRawDataSource.pack(true);
// }
// });

// deviceContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));

	}

	public void createPartControl(final Composite parent) {

		createResources();

		// device data / tour viewer
		tourForm = new ViewForm(parent, SWT.NONE);

		// create the left toolbar
		final ToolBar tbmLeft = new ToolBar(tourForm, SWT.FLAT | SWT.WRAP);
		fTbm = new ToolBarManager(tbmLeft);

		final Composite contentContainer = new Composite(tourForm, SWT.NONE);
		final GridLayout gl = new GridLayout();
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		gl.verticalSpacing = 0;
		contentContainer.setLayout(gl);

		tourForm.setTopLeft(tbmLeft);
		tourForm.setContent(contentContainer);

		createDeviceData(contentContainer);
		createTourViewer(contentContainer);

		final Sash sash = new Sash(parent, SWT.VERTICAL);

		// tour chart
		fTourChart = new TourChart(parent, SWT.NONE, false);
		fTourChart.setShowZoomActions(true);
		fTourChart.setShowSlider(true);

		fTourChartConfig = TourManager.createTourChartConfiguration();
		fTourChartConfig.setMinMaxKeeper(false);

		fViewerDetailForm = new ViewerDetailForm(parent, tourForm, sash, fTourChart);

		// actions
		createActions();
		createContextMenu();

		addPartListener();
		addSelectionListener();
		addPrefListener();

		// set this view part as selection provider
		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		// fire a slider move selection when a slider was moved in the tour
		// chart
		fTourChart.addSliderMoveListener(new ISliderMoveListener() {
			public void sliderMoved(final SelectionChartInfo chartInfoSelection) {
				fPostSelectionProvider.setSelection(chartInfoSelection);
			}
		});

		fActivePerson = TourbookPlugin.getDefault().getActivePerson();

		restoreState(fSessionMemento);
	}

	private void createResources() {

		imageDatabaseDescriptor = TourbookPlugin.getImageDescriptor(Messages.Image_database);
		imageDatabaseOtherPersonDescriptor = TourbookPlugin
				.getImageDescriptor(Messages.Image_database_other_person);
		imageDatabasePlaceholderDescriptor = TourbookPlugin
				.getImageDescriptor(Messages.Image_database_placeholder);

		try {
			final Display display = Display.getCurrent();
			imageDatabase = (Image) imageDatabaseDescriptor.createResource(display);
			imageDatabaseOtherPerson = (Image) imageDatabaseOtherPersonDescriptor
					.createResource(display);
			imageDatabasePlaceholder = (Image) imageDatabasePlaceholderDescriptor
					.createResource(display);
		} catch (final DeviceResourceException e) {
			e.printStackTrace();
		}

		calendar = GregorianCalendar.getInstance();
		dateInstance = DateFormat.getDateInstance(DateFormat.SHORT);
		timeInstance = DateFormat.getTimeInstance(DateFormat.SHORT);
		durationInstance = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.GERMAN);
		numberInstance = NumberFormat.getNumberInstance();
	}

	/**
	 * @param parent
	 */
	private void createTourViewer(final Composite parent) {

		// parent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

		// table
		final Table table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL
				| SWT.FULL_SELECTION
				| SWT.MULTI);

		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		final PixelConverter pixelConverter = new PixelConverter(table);

		fTourViewer = new TableViewer(table);
		fColumnManager = new ColumnManager(fTourViewer);

		ColumnDefinition colDef;

		/*
		 * column: database indicator
		 */
		colDef = new ColumnDefinition(fColumnManager, COLUMN_ID_DB_ICON, SWT.CENTER);
		colDef.setLabel("Datenbank Status");
		colDef.setToolTipText(Messages.RawData_Column_db_status_tooltip);
		colDef.setWidth(20);
		colDef.setColumnResizable(false);
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {

				final TourData tourData = (TourData) cell.getElement();

				// show the database indicator this shows which person owns the tour
				final TourPerson tourPerson = tourData.getTourPerson();
				final long activePersonId = fActivePerson == null ? -1 : fActivePerson
						.getPersonId();

				cell
						.setImage(tourPerson == null ? imageDatabasePlaceholder : tourPerson
								.getPersonId() == activePersonId
								? imageDatabase
								: imageDatabaseOtherPerson);
			}
		});

		/*
		 * column: date
		 */
		colDef = new ColumnDefinition(fColumnManager, COLUMN_ID_DATE, SWT.TRAIL);
		colDef.setText(Messages.RawData_Column_date);
		colDef.setLabel(Messages.RawData_Column_date_label);
		colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(12));
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {

				final TourData tourData = (TourData) cell.getElement();

				calendar.set(tourData.getStartYear(), tourData.getStartMonth() - 1, tourData
						.getStartDay());
				cell.setText(dateInstance.format(calendar.getTime()));
			}
		});
		colDef.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent event) {
				((DeviceImportSorter) fTourViewer.getSorter()).doSort(COLUMN_DATE);
				fTourViewer.refresh();
			}
		});

		/*
		 * column: time
		 */
		colDef = new ColumnDefinition(fColumnManager, COLUMN_ID_START_TIME, SWT.TRAIL);
		colDef.setLabel(Messages.RawData_Column_time_label);
		colDef.setText(Messages.RawData_Column_time);
		colDef.setToolTipText(Messages.RawData_Column_time_tooltip);
		colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(8));
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				final TourData tourData = (TourData) cell.getElement();
				calendar.set(0, 0, 0, tourData.getStartHour(), tourData.getStartMinute(), 0);

				cell.setText(timeInstance.format(calendar.getTime()));
			}
		});

		/*
		 * column: tour type
		 */
		colDef = new ColumnDefinition(fColumnManager, COLUMN_ID_TOUR_TYPE, SWT.TRAIL);
		colDef.setLabel(Messages.RawData_Column_tour_type_label);
		colDef.setToolTipText(Messages.RawData_Column_tour_type_tooltip);
		colDef.setWidth(18);
		colDef.setColumnResizable(false);
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				cell.setImage(getTourTypeImage((TourData) cell.getElement()));
			}
		});

		/*
		 * column: tour title
		 */
		colDef = new ColumnDefinition(fColumnManager, COLUMN_ID_TOUR_TITLE, SWT.LEAD);
		colDef.setLabel("Title");
		colDef.setText("Title");
		colDef.setToolTipText("Title of the tour");
		colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(20));
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				cell.setText(((TourData) cell.getElement()).getTourTitle());
			}
		});

		/*
		 * column: recording time
		 */
		colDef = new ColumnDefinition(fColumnManager, COLUMN_ID_RECORDING_TIME, SWT.TRAIL);
		colDef.setLabel(Messages.RawData_Column_recording_time_label);
		colDef.setText(Messages.RawData_Column_recording_time);
		colDef.setToolTipText(Messages.RawData_Column_recording_time_tooltip);
		colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(8));
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				final int recordingTime = ((TourData) cell.getElement()).getTourRecordingTime();

				if (recordingTime == 0) {
					cell.setText(""); //$NON-NLS-1$
				} else {
					calendar.set(
							0,
							0,
							0,
							recordingTime / 3600,
							((recordingTime % 3600) / 60),
							((recordingTime % 3600) % 60));

					cell.setText(durationInstance.format(calendar.getTime()));
				}
			}
		});

		/*
		 * column: driving time
		 */
		colDef = new ColumnDefinition(fColumnManager, COLUMN_ID_DRIVING_TIME, SWT.TRAIL);
		colDef.setLabel(Messages.RawData_Column_driving_time_label);
		colDef.setText(Messages.RawData_Column_driving_time);
		colDef.setToolTipText(Messages.RawData_Column_driving_time_tooltip);
		colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(8));
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				final int drivingTime = ((TourData) cell.getElement()).getTourDrivingTime();
				if (drivingTime == 0) {
					cell.setText(""); //$NON-NLS-1$
				} else {
					calendar.set(
							0,
							0,
							0,
							drivingTime / 3600,
							((drivingTime % 3600) / 60),
							((drivingTime % 3600) % 60));

					cell.setText(durationInstance.format(calendar.getTime()));
				}
			}
		});

		/*
		 * column: distance
		 */
		colDef = new ColumnDefinition(fColumnManager, COLUMN_ID_DISTANCE, SWT.TRAIL);
		colDef.setLabel(Messages.RawData_Column_distance_label);
		colDef.setText(Messages.RawData_Column_distance);
		colDef.setToolTipText(Messages.RawData_Column_distance_tooltip);
		colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(10));
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				final int tourDistance = ((TourData) cell.getElement()).getTourDistance();
				if (tourDistance == 0) {
					cell.setText(""); //$NON-NLS-1$
				} else {
					numberInstance.setMinimumFractionDigits(2);
					numberInstance.setMaximumFractionDigits(2);
					cell.setText(numberInstance.format(((float) tourDistance) / 1000));
				}
			}
		});

		/*
		 * column: speed
		 */
		colDef = new ColumnDefinition(fColumnManager, COLUMN_ID_SPEED, SWT.TRAIL);
		colDef.setLabel(Messages.RawData_Column_speed_label);
		colDef.setText(Messages.RawData_Column_speed);
		colDef.setToolTipText(Messages.RawData_Column_speed_tooltip);
		colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(9));
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				final TourData tourData = ((TourData) cell.getElement());
				final int tourDistance = tourData.getTourDistance();
				final int drivingTime = tourData.getTourDrivingTime();
				if (drivingTime == 0) {
					cell.setText(""); //$NON-NLS-1$
				} else {
					numberInstance.setMinimumFractionDigits(1);
					numberInstance.setMaximumFractionDigits(1);
					cell.setText(numberInstance.format(((float) tourDistance) / drivingTime * 3.6));
				}
			}
		});

		/*
		 * column: altitude up
		 */
		colDef = new ColumnDefinition(fColumnManager, COLUMN_ID_ALTITUDE, SWT.TRAIL);
		colDef.setLabel(Messages.RawData_Column_altitude_up_label);
		colDef.setText(Messages.RawData_Column_altitude_up);
		colDef.setToolTipText(Messages.RawData_Column_altitude_up_tooltip);
		colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(8));
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				final TourData tourData = (TourData) cell.getElement();
				numberInstance.setMinimumFractionDigits(0);
				cell.setText(numberInstance.format(tourData.getTourAltUp()));
			}
		});

		/*
		 * column: profile
		 */
		colDef = new ColumnDefinition(fColumnManager, COLUMN_ID_DEVICE_PROFILE, SWT.LEAD);
		colDef.setLabel(Messages.RawData_Column_profile_label);
		colDef.setText(Messages.RawData_Column_profile);
		colDef.setToolTipText(Messages.RawData_Column_profile_tooltip);
		colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(10));
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				final TourData tourData = (TourData) cell.getElement();
				if (fImportDevice == null) {
					cell.setText(""); //$NON-NLS-1$
				} else {
					cell.setText(fImportDevice.getDeviceModeName(tourData.getDeviceMode()));
				}
			}
		});

		/*
		 * column: interval
		 */
		colDef = new ColumnDefinition(fColumnManager, COLUMN_ID_TIME_INTERVAL, SWT.TRAIL);
		colDef.setLabel(Messages.RawData_Column_time_interval_label);
		colDef.setText(Messages.RawData_Column_time_interval);
		colDef.setToolTipText(Messages.RawData_Column_time_interval_tooltip);
		colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(8));
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				cell.setText(Integer.toString(((TourData) cell.getElement())
						.getDeviceTimeInterval()));
			}
		});

		/*
		 * column: device name
		 */
		colDef = new ColumnDefinition(fColumnManager, COLUMN_ID_DEVICE_NAME, SWT.LEAD);
		colDef.setLabel("Device/Dataformat");
		colDef.setText("Device");
		colDef.setToolTipText("Device which was used to import the tour");
		colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(10));
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				cell.setText(((TourData) cell.getElement()).getDeviceId());
			}
		});

		/*
		 * column: import file
		 */
		colDef = new ColumnDefinition(fColumnManager, COLUMN_ID_IMPORT_FILE, SWT.LEAD);
		colDef.setLabel("Import File");
		colDef.setText("Import File");
		colDef.setToolTipText("Filename from where the tour was imported");
		colDef.setWidth(pixelConverter.convertWidthInCharsToPixels(20));
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				cell.setText(((TourData) cell.getElement()).importRawDataFile);
			}
		});

		// create all columns
		fColumnManager.createColumns();

		// table viewer
		fTourViewer.setContentProvider(new TourDataContentProvider());
		fTourViewer.setSorter(new DeviceImportSorter());

		fTourViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {
				createChart(false);
			}
		});

		fTourViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				selectTour((StructuredSelection) event.getSelection());
			}
		});
	}

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

		TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(
				fPrefChangeListener);

		getSite().setSelectionProvider(null);

		disposeImages();

		super.dispose();
	}

	private void disposeImages() {
		for (final Iterator<Image> i = fImages.values().iterator(); i.hasNext();) {
			i.next().dispose();
		}
		fImages.clear();

		fColorCache.dispose();
	}

	@SuppressWarnings("unchecked")//$NON-NLS-1$
	private void fillContextMenu(final IMenuManager menuMgr) {

		final IStructuredSelection tourSelection = (IStructuredSelection) fTourViewer
				.getSelection();

		if (!tourSelection.isEmpty()) {
			// menuMgr.add(new Action("Open the Tour") {
			// public void run() {
			// createChart(true);
			// }
			// });
		}

		if (tourSelection.isEmpty() == false) {

			int unsavedTours = 0;
			for (final Iterator<TourData> iter = tourSelection.iterator(); iter.hasNext();) {
				final TourData tourData = iter.next();
				if (tourData.getTourPerson() == null) {
					unsavedTours++;
				}
			}

			final TourPerson person = TourbookPlugin.getDefault().getActivePerson();
			if (person != null) {
				fActionSaveTourWithPerson.setText(NLS.bind(
						Messages.RawData_Action_save_tour_with_person,
						person.getName()));
				fActionSaveTourWithPerson.setPerson(person);
				fActionSaveTourWithPerson.setEnabled(unsavedTours > 0);
				menuMgr.add(fActionSaveTourWithPerson);
			}

			if (tourSelection.size() == 1) {
				fActionSaveTour.setText(Messages.RawData_Action_save_tour_for_person);
			} else {
				fActionSaveTour.setText(Messages.RawData_Action_save_tours_for_person);
			}
			fActionSaveTour.setEnabled(unsavedTours > 0);
			menuMgr.add(fActionSaveTour);

		}

		// add standard group which allows other plug-ins to contribute here
		menuMgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	public void fireSelectionEvent(final ISelection selection) {
		fPostSelectionProvider.setSelection(selection);
	}

	public TableViewer getTourViewer() {
		return fTourViewer;
	}

	public void init(final IViewSite site, final IMemento memento) throws PartInitException {

		super.init(site, memento);

		// set the session memento
		if (fSessionMemento == null) {
			fSessionMemento = memento;
		}
	}

	private void restoreState(final IMemento memento) {

		if (memento != null) {

			// restore viewer/detail weights
			fViewerDetailForm.setViewerWidth(memento.getInteger(MEMENTO_SASH_CONTAINER));

			// show/hide chart
			final Integer isMementoChartVisible = memento.getInteger(MEMENTO_IS_CHART_VISIBLE);
			final boolean isChartVisible = isMementoChartVisible == null
					? true
					: (isMementoChartVisible == 1 ? true : false);
			showViewerDetails(isChartVisible);
			fActionShowTourChart.setChecked(isChartVisible);

			/*
			 * sort table columns
			 */
			final String mementoColumnSortOrderIds = memento.getString(MEMENTO_COLUMN_SORT_ORDER);
			if (mementoColumnSortOrderIds != null) {
				fColumnManager.orderColumns(convertStringToIntArray(mementoColumnSortOrderIds));
			}

			/*
			 * create table columns
			 */
//			final String mementoVisibleColumnIds = memento.getString(MEMENTO_VISIBLE_COLUMNS);
//			if (mementoVisibleColumnIds == null) {
//
////				// create all columns
////				fColumnManager.createColumns();
//
//			} else {
//
////				fColumnManager.createColumns(convertStringToIntArray(mementoVisibleColumnIds));
//			}
			// restore column width
			final String mementoColumnWidth = memento.getString(MEMENTO_COLUMN_WIDTH);
			if (mementoColumnWidth != null) {

//				fColumnManager.setColumnWidth(convertStringToIntArray(mementoColumnWidth));
			}

			// restore imported tours
			final String importFilename = memento.getString(MEMENTO_IMPORT_FILENAME);
			if (importFilename != null) {

				final RawDataManager rawDataManager = RawDataManager.getInstance();
				final TourbookDevice rawDataDevice = rawDataManager.getDevice();

				if (rawDataDevice == null) {
					rawDataManager.importRawData(importFilename);
				}

				updateViewer();
				setActionSaveEnabled(RawDataManager.getInstance().isDeviceImport());

				// restore selected tour
				fTourViewer.getTable().select(memento.getInteger(MEMENTO_SELECTED_TOUR_INDEX));
				fTourViewer.getTable().showSelection();

				selectTour((StructuredSelection) fTourViewer.getSelection());
			}
		} else {
			// create all viewer columns
			fColumnManager.createColumns();
		}
	}

	private int[] convertStringToIntArray(final String mementoColumnWidth) {
		String[] columnIdAndWidth = StringToArrayConverter.convertStringToArray(mementoColumnWidth);
		int[] columnIds = new int[columnIdAndWidth.length];

		for (int columnIdx = 0; columnIdx < columnIds.length; columnIdx++) {
			columnIds[columnIdx] = Integer.valueOf(columnIdAndWidth[columnIdx]);
		}
		return columnIds;
	}

	private void saveSettings() {
		fSessionMemento = XMLMemento.createWriteRoot("DeviceImportView"); //$NON-NLS-1$
		saveState(fSessionMemento);
	}

	public void saveState(final IMemento memento) {

		// save sash weights
		memento.putInteger(MEMENTO_SASH_CONTAINER, fTourViewer.getTable().getSize().x);

		// save: is chart visible
		memento.putInteger(MEMENTO_IS_CHART_VISIBLE, fActionShowTourChart.isChecked() ? 1 : 0);

		// final RawDataManager rawDataMgr = RawDataManager.getInstance();

		// save import file name
		// final TourbookDevice rawDataDevice = rawDataMgr.getDevice();
		// if (rawDataDevice != null) {
		// memento.putString(MEMENTO_IMPORT_FILENAME, rawDataMgr.getImportFileName());
		// } else {
		memento.putString(MEMENTO_IMPORT_FILENAME, null);
		// }

		// save selected tour in the viewer
		memento.putInteger(MEMENTO_SELECTED_TOUR_INDEX, fTourViewer.getTable().getSelectionIndex());

		saveColumns(memento);
	}

	/**
	 * save all visible columns in the memento
	 */
	private void saveColumns(IMemento memento) {

		// get columnId for all visible columns
		ArrayList<String> columnIds = new ArrayList<String>();

		/*
		 * save all columns in the current order
		 */
		columnIds.clear();
		for (ColumnDefinition colDef : fColumnManager.getColumns()) {
			columnIds.add(Integer.toString(colDef.getColumnId()));
		}
		memento.putString(MEMENTO_COLUMN_SORT_ORDER, StringToArrayConverter
				.convertArrayToString(columnIds.toArray()));

		/*
		 * save columns width in the format: id/width
		 */
		columnIds.clear();
		Table table = fTourViewer.getTable();
		for (TableColumn column : table.getColumns()) {
			columnIds.add(Integer.toString(((ColumnDefinition) column.getData()).getColumnId()));
			columnIds.add(Integer.toString(column.getWidth()));
		}
		memento.putString(MEMENTO_COLUMN_WIDTH, StringToArrayConverter
				.convertArrayToString(columnIds.toArray()));
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

		final Collection<TourData> tourDataCollection = RawDataManager
				.getInstance()
				.getTourData()
				.values();

		final TourData[] tourList = tourDataCollection.toArray(new TourData[tourDataCollection
				.size()]);

		// select the last tour in the viewer
		if (tourList.length > 0) {
			final TourData tourData = tourList[0];
			fTourViewer.setSelection(new StructuredSelection(tourData), true);
		}
	}

	private void selectTour(final StructuredSelection selection) {

		final TourData tourData = (TourData) selection.getFirstElement();

		if (tourData != null) {

			showTourChart(tourData);

// updateDataSource(tourData);
		}
	}

	/**
	 * enable/disable the save action
	 * 
	 * @param enabled
	 */
	public void setActionSaveEnabled(final boolean enabled) {
		fActionSaveRawDataFile.setEnabled(enabled);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		fTourViewer.getControl().setFocus();
	}

	public void showViewerDetails(final boolean isVisible) {
		fViewerDetailForm.setMaximizedControl(isVisible ? null : tourForm);
	}

	private void createChart(final boolean useNormalizedData) {

		final Object selObject = ((IStructuredSelection) fTourViewer.getSelection())
				.getFirstElement();

		if (selObject != null && selObject instanceof TourData) {
			TourManager.getInstance().createTour((TourData) selObject, useNormalizedData);
		}
	}

	private void showTourChart(final TourData tourData) {

		// show the tour chart

		final IDataModelListener dataModelListener = new IDataModelListener() {

			public void dataModelChanged(ChartDataModel chartDataModel) {

				// set title
				chartDataModel.setTitle(NLS.bind(Messages.RawData_Chart_title, TourManager
						.getTourTitleDetailed(tourData)));
			}
		};

		fTourChart.addDataModelListener(dataModelListener);
		fTourChart.updateChart(tourData, fTourChartConfig, false);

		disableTourChartSelection();
	}

	/**
	 * prevent the marker viewer to show the markers by setting the tour chart parameter to null
	 */
	private void disableTourChartSelection() {
		fPostSelectionProvider.setSelection(new SelectionTourChart(null));
	}

//	/**
//	 * update data source label
//	 */
//	private void updateDataSource(final TourData tourData) {
//
//		final String rawDataFile = tourData.importRawDataFile;
//		String rawDataLabel;
//
//		if (rawDataFile == null) {
//			rawDataLabel = Messages.RawData_Lable_import_no_data;
//
//		} else {
//			final String tempDataFileName = RawDataManager.getTempDataFileName();
//			if (rawDataFile.equalsIgnoreCase(tempDataFileName)) {
//				rawDataLabel = Messages.RawData_Lable_import_from_device;
//
//			} else {
//
//			}
//			rawDataLabel = Messages.RawData_Lable_import_from_file + tourData.importRawDataFile;
//		}
//
//		// fLblRawDataSource.setText(Dialog.shortenText(rawDataLabel, fLblRawDataSource));
//		// fLblRawDataSource.pack(true);
//	}

	public void updateViewer() {

		final RawDataManager rawDataManager = RawDataManager.getInstance();

		// update tour data viewer
		fTourViewer.setInput(rawDataManager.getTourData().values().toArray());

//		fImportDevice = rawDataManager.getDevice();
//
//		// update tour data from the raw data manager
//		if (fImportDevice != null) {
//
//
//			fTourViewer.getTable().setEnabled(true);
//
//		} else {
//			fTourViewer.getTable().setEnabled(false);
//		}
	}

	/**
	 * when the active person was modified, the view must be updated
	 */
	private void updateViewerPersonData() {

		fActivePerson = TourbookPlugin.getDefault().getActivePerson();

		// update person in the raw data
		RawDataManager.getInstance().updatePersonInRawData();

		fTourViewer.refresh();
	}

}
