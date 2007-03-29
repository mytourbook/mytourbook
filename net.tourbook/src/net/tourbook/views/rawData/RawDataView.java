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

package net.tourbook.views.rawData;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
import net.tourbook.ui.ViewerDetailForm;
import net.tourbook.util.PixelConverter;
import net.tourbook.util.PostSelectionProvider;
import net.tourbook.views.tourBook.DrawingColors;
import net.tourbook.views.tourBook.SelectionRemovedTours;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.DeviceResourceException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
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

public class RawDataView extends ViewPart {

	public static final String			ID							= "net.tourbook.views.rawData.RawDataView"; //$NON-NLS-1$

	private static final String			TOUR_TYPE_PREFIX			= "tourType";								//$NON-NLS-1$

	public static final int				COLUMN_DATE					= 0;
	public static final int				COLUMN_START_TIME			= 1;
	public static final int				COLUMN_RECORDING_TIME		= 2;
	public static final int				COLUMN_DRIVING_TIME			= 3;
	public static final int				COLUMN_DISTANCE				= 4;
	public static final int				COLUMN_AVG_SPEED			= 5;
	public static final int				COLUMN_ALTITUDE				= 6;
	public static final int				COLUMN_DEVICE_PROFILE		= 7;
	public static final int				COLUMN_TIME_INTERVAL		= 8;

	private static final String			MEMENTO_SASH_CONTAINER		= "importview.sash.container.";			//$NON-NLS-1$
	private static final String			MEMENTO_IMPORT_FILENAME		= "importview.raw-data.filename";			//$NON-NLS-1$
	private static final String			MEMENTO_SELECTED_TOUR_INDEX	= "importview.selected-tour-index";		//$NON-NLS-1$
	private static final String			MEMENTO_IS_CHART_VISIBLE	= "importview.is-chart-visible";			//$NON-NLS-1$

	private static IMemento				fSessionMemento;

	private ViewerDetailForm			fViewerDetailForm;
	private TableViewer					fTourViewer;

	private TourChart					fTourChart;
	private TourChartConfiguration		fTourChartConfig;

	private ActionSaveRawData			actionSave;
	private ActionImportFromFile		fActionImportTourFromFile;
	private ActionSaveTourInDatabase	actionSaveTour;
	private ActionSaveTourInDatabase	actionSaveTourWithPerson;
	private ActionShowViewDetails		fActionShowTourChart;

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

	// protected TourEditorPart currentTourEditor;

	public Calendar						calendar;
	public DateFormat					dateInstance;
	public DateFormat					timeInstance;
	private DateFormat					durationInstance;
	private NumberFormat				numberInstance;

	private ToolBarManager				fTbm;

	private ViewForm					tourForm;

	/**
	 * status if the tour chart is displayed
	 */
	private Label						fLblRawDataSource;

	protected TourPerson				fActivePerson;
	protected TourPerson				fNewActivePerson;

	protected boolean					fIsPartVisible				= false;
	protected boolean					fIsViewerPersonDataDirty	= false;

	private HashMap<String, Image>		fImages						= new HashMap<String, Image>();
	private ColorCache					fColorCache					= new ColorCache();

	private int							fColorImageHeight			= -1;
	private int							fColorImageWidth;

	/**
	 * device which was used to import the data, it's set to <code>null</code>
	 * when the import was not successful
	 */
	private TourbookDevice				fImportDevice;

	private class TourDataContentProvider implements IStructuredContentProvider {

		public TourDataContentProvider() {}
		public void dispose() {}
		public Object[] getElements(Object parent) {
			return (Object[]) (parent);
		}

		public void inputChanged(Viewer v, Object oldInput, Object newInput) {}
	}

	private class TourDataLabelProvider extends LabelProvider implements ITableLabelProvider {

		public Image getColumnImage(Object obj, int index) {

			TourData tourData = ((TourData) obj);

			long activePersonId = fActivePerson == null ? -1 : fActivePerson.getPersonId();

			switch (index) {
			case COLUMN_DATE:
				// show the database indicator, which person owns the tour
				TourPerson tourPerson = tourData.getTourPerson();

				return tourPerson == null
						? imageDatabasePlaceholder
						: tourPerson.getPersonId() == activePersonId
								? imageDatabase
								: imageDatabaseOtherPerson;

			case COLUMN_START_TIME:
				return getTourTypeImage(tourData);
			}

			return null;
		}

		public String getColumnText(Object obj, int index) {

			TourData tourData = ((TourData) obj);

			final int drivingTime = tourData.getTourDrivingTime();
			final int tourDistance = tourData.getTourDistance();

			switch (index) {
			case COLUMN_DATE:
				calendar.set(tourData.getStartYear(), tourData.getStartMonth() - 1, tourData
						.getStartDay());
				return dateInstance.format(calendar.getTime());

			case COLUMN_START_TIME:
				calendar.set(0, 0, 0, tourData.getStartHour(), tourData.getStartMinute(), 0);
				return timeInstance.format(calendar.getTime());

			case COLUMN_RECORDING_TIME:
				int recordingTime = tourData.getTourRecordingTime();

				if (recordingTime == 0) {
					return ""; //$NON-NLS-1$
				} else {
					calendar.set(
							0,
							0,
							0,
							recordingTime / 3600,
							((recordingTime % 3600) / 60),
							((recordingTime % 3600) % 60));

					return durationInstance.format(calendar.getTime());
				}

			case COLUMN_DRIVING_TIME:
				if (drivingTime == 0) {
					return ""; //$NON-NLS-1$
				} else {
					calendar.set(
							0,
							0,
							0,
							drivingTime / 3600,
							((drivingTime % 3600) / 60),
							((drivingTime % 3600) % 60));

					return durationInstance.format(calendar.getTime());
				}

			case COLUMN_DISTANCE:
				if (tourDistance == 0) {
					return ""; //$NON-NLS-1$
				} else {
					numberInstance.setMinimumFractionDigits(2);
					numberInstance.setMaximumFractionDigits(2);
					return numberInstance.format(((float) tourDistance) / 1000);
				}

			case COLUMN_AVG_SPEED:

				if (drivingTime == 0) {
					return ""; //$NON-NLS-1$
				} else {
					numberInstance.setMinimumFractionDigits(1);
					numberInstance.setMaximumFractionDigits(1);
					return numberInstance.format(((float) tourDistance) / drivingTime * 3.6);
				}

			case COLUMN_ALTITUDE:
				numberInstance.setMinimumFractionDigits(0);
				return numberInstance.format(tourData.getTourAltUp());

			case COLUMN_TIME_INTERVAL:
				return Integer.toString(tourData.getDeviceTimeInterval());

			case COLUMN_DEVICE_PROFILE:
				if (fImportDevice == null) {
					return ""; //$NON-NLS-1$
				} else {
					return fImportDevice.getDeviceModeName(tourData.getDeviceMode());
				}

			default:
				break;
			}

			return (getText(obj));
		}
	}

	private void addPrefListener() {
		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				/*
				 * set a new chart configuration when the preferences has
				 * changed
				 */
				if (property.equals(ITourbookPreferences.GRAPH_VISIBLE)
						|| property.equals(ITourbookPreferences.GRAPH_X_AXIS)
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
					actionSaveTour.resetPeopleList();
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
	private void ensureImageSize(Display display) {

		if (fColorImageHeight == -1) {

			Table table = fTourViewer.getTable();

			// fColorImageHeight = table.getItemHeight();
			// fColorImageWidth = 10;

			fColorImageWidth = 16;
			fColorImageHeight = table.getItemHeight();
		}
	}

	private Image getTourTypeImage(TourData tourData) {

		TourType tourType = tourData.getTourType();

		if (tourType == null) {
			return null;
		}

		long typeId = tourType.getTypeId();

		String colorId = TOUR_TYPE_PREFIX + typeId;
		Image image = fImages.get(colorId);

		if (image == null) {

			Display display = Display.getCurrent();
			ensureImageSize(display);
			image = new Image(display, fColorImageWidth, fColorImageHeight);

			GC gc = new GC(image);
			{
				int arcSize = 4;

				DrawingColors drawingColors = getTourTypeDrawingColors(display, typeId);

				Color colorBright = drawingColors.colorBright;

				if (colorBright != null) {

					gc.setForeground(colorBright);
					gc.setBackground(drawingColors.colorDark);

//					gc.setAlpha(0x0);
//					gc.fillRectangle(0, 0, fColorImageWidth, fColorImageHeight);

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
	private DrawingColors getTourTypeDrawingColors(Display display, long tourTypeId) {

		String colorIdBright = TOUR_TYPE_PREFIX + "bright" + tourTypeId; //$NON-NLS-1$
		String colorIdDark = TOUR_TYPE_PREFIX + "dark" + tourTypeId; //$NON-NLS-1$
		String colorIdLine = TOUR_TYPE_PREFIX + "line" + tourTypeId; //$NON-NLS-1$

		DrawingColors drawingColors = new DrawingColors();

		Color colorBright = fColorCache.get(colorIdBright);
		Color colorDark = fColorCache.get(colorIdDark);
		Color colorLine = fColorCache.get(colorIdLine);

		if (colorBright == null) {

			ArrayList<TourType> tourTypes = TourbookPlugin.getDefault().getTourTypes();

			TourType tourTypeColors = null;

			for (TourType tourType : tourTypes) {
				if (tourType.getTypeId() == tourTypeId) {
					tourTypeColors = tourType;
				}
			}

			if (tourTypeColors == null
					|| tourTypeColors.getTypeId() == TourType.TOUR_TYPE_ID_NOT_DEFINED) {

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
			public void selectionChanged(IWorkbenchPart part, ISelection selection) {

				if (!selection.isEmpty() && selection instanceof SelectionRemovedTours) {

					SelectionRemovedTours tourSelection = (SelectionRemovedTours) selection;
					ArrayList<ITourItem> removedTours = tourSelection.removedTours;

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
			public void partActivated(IWorkbenchPartReference partRef) {

				disableTourChartSelection();

				// IWorkbenchPart part = partRef.getPart(false);
				// if (part instanceof TourEditorPart && part !=
				// currentTourEditor)
				// {
				// currentTourEditor = (TourEditorPart) part;
				// selectTourInView();
				// }
			}
			public void partBroughtToTop(IWorkbenchPartReference partRef) {}
			public void partClosed(IWorkbenchPartReference partRef) {
				if (ID.equals(partRef.getId()))
					saveSettings();

				RawDataManager.getInstance().resetData();
			}
			public void partDeactivated(IWorkbenchPartReference partRef) {
				if (ID.equals(partRef.getId()))
					saveSettings();
			}
			public void partHidden(IWorkbenchPartReference partRef) {
				if (RawDataView.this == partRef.getPart(false)) {
					fIsPartVisible = false;
				}
			}
			public void partInputChanged(IWorkbenchPartReference partRef) {}
			public void partOpened(IWorkbenchPartReference partRef) {}
			public void partVisible(IWorkbenchPartReference partRef) {
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
		actionSave = new ActionSaveRawData(this);
		fActionImportTourFromFile = new ActionImportFromFile();
		actionSaveTour = new ActionSaveTourInDatabase(this);
		actionSaveTourWithPerson = new ActionSaveTourInDatabase(this);
		fActionShowTourChart = new ActionShowViewDetails(this);

		fTbm.add(actionSave);
		fTbm.add(fActionImportTourFromFile);

		fTbm.update(true);

		// view actions
		IToolBarManager viewTbm = getViewSite().getActionBars().getToolBarManager();
		viewTbm.add(fActionShowTourChart);
	}

	/**
	 * create the views context menu
	 */
	private void createContextMenu() {

		MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(fTourViewer.getControl());
		fTourViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, fTourViewer);
	}

	private void createDeviceData(Composite parent) {

		Composite container = new Composite(parent, SWT.NONE);
		container.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

		GridLayout gridLayout = new GridLayout();
		gridLayout.marginHeight = 1;
		gridLayout.marginWidth = 2;
		container.setLayout(gridLayout);

		fLblRawDataSource = new Label(container, SWT.NONE);
		fLblRawDataSource.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		fLblRawDataSource.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				// recalculate the label
				updateDeviceData();
			}
		});

		// container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));

	}

	public void createPartControl(Composite parent) {

		createResources();

		// device data / tour viewer
		tourForm = new ViewForm(parent, SWT.NONE);

		// create the left toolbar
		ToolBar tbmLeft = new ToolBar(tourForm, SWT.FLAT | SWT.WRAP);
		fTbm = new ToolBarManager(tbmLeft);

		Composite partComposite = new Composite(tourForm, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		gridLayout.verticalSpacing = 0;
		partComposite.setLayout(gridLayout);

		tourForm.setTopLeft(tbmLeft);
		tourForm.setContent(partComposite);

		createDeviceData(partComposite);
		createTourViewer(partComposite);

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
			public void sliderMoved(SelectionChartInfo chartInfoSelection) {
				fPostSelectionProvider.setSelection(chartInfoSelection);
			}
		});

		fActivePerson = TourbookPlugin.getDefault().getActivePerson();
		restoreState(fSessionMemento);
	}

	/*
	 * public void createPartControlDISABLED(Composite parent) {
	 * createResources(); fPartSash = new SashForm(parent, SWT.VERTICAL);
	 * fPartSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	 * fPartSash.setOrientation(SWT.HORIZONTAL); // device data / tour viewer
	 * tourForm = new ViewForm(fPartSash, SWT.NONE); // create the left toolbar
	 * ToolBar tbmLeft = new ToolBar(tourForm, SWT.FLAT | SWT.WRAP); fTbm = new
	 * ToolBarManager(tbmLeft); Composite partComposite = new
	 * Composite(tourForm, SWT.NONE); GridLayout gridLayout = new GridLayout();
	 * gridLayout.marginWidth = 0; gridLayout.marginHeight = 0;
	 * gridLayout.verticalSpacing = 0; partComposite.setLayout(gridLayout);
	 * tourForm.setTopLeft(tbmLeft); tourForm.setContent(partComposite);
	 * createDeviceData(partComposite); createTourViewer(partComposite); // tour
	 * chart fTourChart = new TourChart(fPartSash, SWT.NONE, false);
	 * fTourChart.setShowZoomActions(true); fTourChart.setShowSlider(true);
	 * fTourChartConfig = TourManager.createTourChartConfiguration();
	 * fTourChartConfig.setMinMaxKeeper(false); // actions createActions();
	 * createContextMenu(); addPartListener(); addSelectionListener();
	 * addPrefListener(); // set this view part as selection provider
	 * getSite().setSelectionProvider(fPostSelectionProvider = new
	 * PostSelectionProvider()); // fire a slider move selection when a slider
	 * was moved in the tour // chart fTourChart.addSliderMoveListener(new
	 * ISliderMoveListener() { public void sliderMoved(SelectionChartInfo
	 * chartInfoSelection) {
	 * fPostSelectionProvider.setSelection(chartInfoSelection); } });
	 * fActivePerson = TourbookPlugin.getDefault().getActivePerson();
	 * restoreState(fSessionMemento); }
	 */
	private void createResources() {
		imageDatabaseDescriptor = TourbookPlugin.getImageDescriptor(Messages.Image_database);
		imageDatabaseOtherPersonDescriptor = TourbookPlugin
				.getImageDescriptor(Messages.Image_database_other_person);
		imageDatabasePlaceholderDescriptor = TourbookPlugin
				.getImageDescriptor(Messages.Image_database_placeholder);

		try {
			Display display = Display.getCurrent();
			imageDatabase = (Image) imageDatabaseDescriptor.createResource(display);
			imageDatabaseOtherPerson = (Image) imageDatabaseOtherPersonDescriptor
					.createResource(display);
			imageDatabasePlaceholder = (Image) imageDatabasePlaceholderDescriptor
					.createResource(display);
		} catch (DeviceResourceException e) {
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
	private void createTourViewer(Composite parent) {

		// parent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

		// table
		Table table = new Table(parent, SWT.H_SCROLL
				| SWT.V_SCROLL
				| SWT.FULL_SELECTION
				| SWT.MULTI
				| SWT.BORDER);

		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		PixelConverter pixelConverter = new PixelConverter(table);

		// columns
		TableColumn tc;

		tc = new TableColumn(table, SWT.TRAIL);
		tc.setText(Messages.RawData_Colum_date);
		tc.setToolTipText(Messages.RawData_Column_date_tooltip);
		tc.setWidth(pixelConverter.convertWidthInCharsToPixels(16));

		tc.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				((DeviceImportSorter) fTourViewer.getSorter()).doSort(COLUMN_DATE);
				fTourViewer.refresh();
			}
		});

		tc = new TableColumn(table, SWT.TRAIL);
		tc.setText(Messages.RawData_Column_time);
		tc.setToolTipText(Messages.RawData_Column_time_tooltip);
		tc.setWidth(pixelConverter.convertWidthInCharsToPixels(16));

		tc = new TableColumn(table, SWT.TRAIL);
		tc.setText(Messages.RawData_Column_recording_time);
		tc.setToolTipText(Messages.RawData_Column_recording_time_tooltip);
		tc.setWidth(pixelConverter.convertWidthInCharsToPixels(8));

		tc = new TableColumn(table, SWT.TRAIL);
		tc.setText(Messages.RawData_Column_driving_time);
		tc.setToolTipText(Messages.RawData_Column_driving_time_tooltip);
		tc.setWidth(pixelConverter.convertWidthInCharsToPixels(8));

		tc = new TableColumn(table, SWT.TRAIL);
		tc.setText(Messages.RawData_Column_distance);
		tc.setToolTipText(Messages.RawData_Column_distance_tooltip);
		tc.setWidth(pixelConverter.convertWidthInCharsToPixels(10));

		tc = new TableColumn(table, SWT.TRAIL);
		tc.setText(Messages.RawData_Column_speed);
		tc.setToolTipText(Messages.RawData_Column_speed_tooltip);
		tc.setWidth(pixelConverter.convertWidthInCharsToPixels(9));

		tc = new TableColumn(table, SWT.TRAIL);
		tc.setText(Messages.RawData_Column_altitude_up);
		tc.setToolTipText(Messages.RawData_Column_altitude_up_tooltip);
		tc.setWidth(pixelConverter.convertWidthInCharsToPixels(8));

		tc = new TableColumn(table, SWT.LEAD);
		tc.setText(Messages.RawData_Column_profile);
		tc.setToolTipText(Messages.RawData_Column_profile_tooltip);
		tc.setWidth(pixelConverter.convertWidthInCharsToPixels(10));

		tc = new TableColumn(table, SWT.TRAIL);
		tc.setText(Messages.RawData_Column_time_interval);
		tc.setToolTipText(Messages.RawData_Column_time_interval_tooltip);
		tc.setWidth(pixelConverter.convertWidthInCharsToPixels(8));

		// table viewer
		fTourViewer = new TableViewer(table);

		fTourViewer.setContentProvider(new TourDataContentProvider());
		fTourViewer.setLabelProvider(new TourDataLabelProvider());
		fTourViewer.setSorter(new DeviceImportSorter());

		fTourViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				createChart(false);
			}
		});

		fTourViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {

				StructuredSelection selection = (StructuredSelection) event.getSelection();

				selectTour(selection);
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
		for (Iterator i = fImages.values().iterator(); i.hasNext();) {
			((Image) i.next()).dispose();
		}
		fImages.clear();

		fColorCache.dispose();
	}

	private void fillContextMenu(IMenuManager menuMgr) {

		IStructuredSelection tourSelection = (IStructuredSelection) fTourViewer.getSelection();

		if (!tourSelection.isEmpty()) {
			// menuMgr.add(new Action("Open the Tour") {
			// public void run() {
			// createChart(true);
			// }
			// });
		}

		if (tourSelection.isEmpty() == false) {

			int unsavedTours = 0;
			for (Iterator iter = tourSelection.iterator(); iter.hasNext();) {
				TourData tourData = (TourData) iter.next();
				if (tourData.getTourPerson() == null) {
					unsavedTours++;
				}
			}

			TourPerson person = TourbookPlugin.getDefault().getActivePerson();
			if (person != null) {
				actionSaveTourWithPerson.setText(NLS.bind(
						Messages.RawData_Action_save_tour_with_person,
						person.getName()));
				actionSaveTourWithPerson.setPerson(person);
				actionSaveTourWithPerson.setEnabled(unsavedTours > 0);
				menuMgr.add(actionSaveTourWithPerson);
			}

			if (tourSelection.size() == 1) {
				actionSaveTour.setText(Messages.RawData_Action_save_tour_for_person);
			} else {
				actionSaveTour.setText(Messages.RawData_Action_save_tours_for_person);
			}
			actionSaveTour.setEnabled(unsavedTours > 0);
			menuMgr.add(actionSaveTour);

		}

		// add standard group which allows other plug-ins to contribute here
		menuMgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	public void fireSelectionEvent(ISelection selection) {
		fPostSelectionProvider.setSelection(selection);
	}

	public TableViewer getTourViewer() {
		return fTourViewer;
	}

	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);

		// set the session memento if it's not yet set
		if (fSessionMemento == null) {
			fSessionMemento = memento;
		}
	}

	private void restoreState(IMemento memento) {

		if (memento != null) {

			// restore viewer/detail weights
			fViewerDetailForm.setViewerWidth(memento.getInteger(MEMENTO_SASH_CONTAINER));

			// show/hide chart
			Integer isMementoChartVisible = memento.getInteger(MEMENTO_IS_CHART_VISIBLE);
			boolean isChartVisible = isMementoChartVisible == null
					? true
					: (isMementoChartVisible == 1 ? true : false);
			showViewerDetails(isChartVisible);
			fActionShowTourChart.setChecked(isChartVisible);

			// restore imported tours
			String importFilename = memento.getString(MEMENTO_IMPORT_FILENAME);

			if (importFilename != null) {

				RawDataManager rawDataManager = RawDataManager.getInstance();
				TourbookDevice rawDataDevice = rawDataManager.getDevice();

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
		}
	}
	private void saveSettings() {
		fSessionMemento = XMLMemento.createWriteRoot("DeviceImportView"); //$NON-NLS-1$
		saveState(fSessionMemento);
	}

	public void saveState(IMemento memento) {

		// save sash weights
		memento.putInteger(MEMENTO_SASH_CONTAINER, fTourViewer.getTable().getSize().x);

		// save: is chart visible
		memento.putInteger(MEMENTO_IS_CHART_VISIBLE, fActionShowTourChart.isChecked() ? 1 : 0);

		RawDataManager rawDataMgr = RawDataManager.getInstance();
		// save import file name
		TourbookDevice rawDataDevice = rawDataMgr.getDevice();
		if (rawDataDevice != null) {
			memento.putString(MEMENTO_IMPORT_FILENAME, rawDataMgr.getImportFileName());
		} else {
			memento.putString(MEMENTO_IMPORT_FILENAME, null);
		}

		// save selected tour in the viewer
		memento.putInteger(MEMENTO_SELECTED_TOUR_INDEX, fTourViewer.getTable().getSelectionIndex());
	}

	void selectLastTour() {

		ArrayList<TourData> tourList = RawDataManager.getInstance().getTourData();

		// select the last tour in the viewer
		if (tourList.size() > 0) {
			TourData tourData = tourList.get(0);
			fTourViewer.setSelection(new StructuredSelection(tourData), true);
		}
	}

	private void selectTour(StructuredSelection selection) {

		TourData tourData = (TourData) selection.getFirstElement();

		if (tourData != null) {

			/*
			 * action "Store in Db" is enabled if the tour was not yet saved
			 */
			// if (selection.size() == 1) {
			// boolean isEnabled = tourData != null
			// && tourData.fIsTourSavedInDb == false;
			// actionSaveTour.setEnabled(isEnabled);
			// actionSaveTourWithPerson.setEnabled(isEnabled);
			// } else {
			// actionSaveTour.setEnabled(true);
			// actionSaveTourWithPerson.setEnabled(true);
			// }
			showTourChart(tourData);
		}
	}

	// /**
	// * select a tour in the table viewer
	// */
	// private void selectTourInView() {
	// if (currentTourEditor == null) {
	// return;
	// }
	//
	// fTourViewer.setSelection(new
	// StructuredSelection(currentTourEditor.getTourData()), true);
	// }

	/**
	 * enable/disable the save action
	 * 
	 * @param enabled
	 */
	public void setActionSaveEnabled(boolean enabled) {
		actionSave.setEnabled(enabled);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		fTourViewer.getControl().setFocus();
	}

	public void showViewerDetails(boolean isVisible) {
		fViewerDetailForm.setMaximizedControl(isVisible ? null : tourForm);
	}

	private void createChart(boolean useNormalizedData) {

		Object selObject = ((IStructuredSelection) fTourViewer.getSelection()).getFirstElement();

		if (selObject != null && selObject instanceof TourData) {
			TourManager.getInstance().createTour((TourData) selObject, useNormalizedData);
		}
	}

	private void showTourChart(final TourData tourData) {

		// show the tour chart

		IDataModelListener dataModelListener = new IDataModelListener() {

			public void dataModelChanged(ChartDataModel chartDataModel) {

				// set title
				chartDataModel.setTitle(NLS.bind(Messages.RawData_Chart_title, TourManager
						.getTourDate(tourData)));
			}
		};

		fTourChart.addDataModelListener(dataModelListener);
		fTourChart.updateChart(tourData, fTourChartConfig, false);

		disableTourChartSelection();
	}

	/**
	 * prevent the marker viewer to show the markers by setting the tour chart
	 * parameter to null
	 */
	private void disableTourChartSelection() {
		fPostSelectionProvider.setSelection(new SelectionTourChart(null));
	}

	private void updateDeviceData() {

		RawDataManager importer = RawDataManager.getInstance();

		// update source label
		TourbookDevice device = importer.getDevice();

		if (device == null) {
			fLblRawDataSource.setText(Messages.RawData_Lable_import_no_data);
		} else {

			String rawDataSource;
			if (importer.isDeviceImport()) {
				rawDataSource = device.visibleName + Messages.RawData_Lable_import_from_device;
			} else {
				rawDataSource = device.visibleName
						+ Messages.RawData_Lable_import_from_file
						+ importer.getImportFileName();
			}
			fLblRawDataSource.setText(Dialog.shortenText(rawDataSource, fLblRawDataSource));
		}
	}

	public void updateViewer() {

		RawDataManager rawDataManager = RawDataManager.getInstance();
		fImportDevice = rawDataManager.getDevice();

		// update tour data from the raw data manager
		if (fImportDevice != null) {

			// update tour data viewer
			fTourViewer.setInput(rawDataManager.getTourData().toArray());

			updateDeviceData();

			fTourViewer.getTable().setEnabled(true);

		} else {
			fTourViewer.getTable().setEnabled(false);
		}
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
