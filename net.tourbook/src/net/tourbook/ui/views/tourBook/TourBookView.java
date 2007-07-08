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
package net.tourbook.ui.views.tourBook;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Iterator;

import net.tourbook.Messages;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ISliderMoveListener;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.statistic.StatisticContainer;
import net.tourbook.tour.IDataModelListener;
import net.tourbook.tour.ITourChartSelectionListener;
import net.tourbook.tour.SelectionTourChart;
import net.tourbook.tour.Tour;
import net.tourbook.tour.TourChart;
import net.tourbook.tour.TourChartConfiguration;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TreeViewerItem;
import net.tourbook.ui.ActionModifyColumns;
import net.tourbook.ui.ColumnManager;
import net.tourbook.ui.ITourChartViewer;
import net.tourbook.ui.TreeColumnDefinition;
import net.tourbook.ui.TreeColumnFactory;
import net.tourbook.ui.UI;
import net.tourbook.ui.ViewerDetailForm;
import net.tourbook.ui.views.SelectionTourSegmentLayer;
import net.tourbook.ui.views.rawData.SelectionRawData;
import net.tourbook.util.PixelConverter;
import net.tourbook.util.PostSelectionProvider;
import net.tourbook.util.StringToArrayConverter;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

public class TourBookView extends ViewPart implements ITourChartViewer {

	static public final String			ID									= "net.tourbook.views.tourListView";			//$NON-NLS-1$

	private static final String			MEMENTO_VIEWER_WIDTH				= "tourbookview.sash-weight-container.";		//$NON-NLS-1$
	private static final String			MEMENTO_SASH_WEIGHT_DETAIL			= "tourbookview.sash-weight-detail.";			//$NON-NLS-1$
	private static final String			MEMENTO_VISIBLE_STATUS_CONTAINER	= "tourbookview.visible-status-container";		//$NON-NLS-1$
	private static final String			MEMENTO_VISIBLE_STATUS_DETAIL		= "tourbookview.visible-status-detail";		//$NON-NLS-1$
	private static final String			MEMENTO_TOURVIEWER_SELECTED_YEAR	= "tourbookview.tourviewer.selected-year";		//$NON-NLS-1$
	private static final String			MEMENTO_TOURVIEWER_SELECTED_MONTH	= "tourbookview.tourviewer.selected-month";	//$NON-NLS-1$
	private static final String			MEMENTO_LAST_SELECTED_TOUR_TYPE_ID	= "tourbookview.last-selected-tour-type-id";	//$NON-NLS-1$
	private static final String			MEMENTO_COLUMN_SORT_ORDER			= "tourbookview.column_sort_order";			//$NON-NLS-1$
	private static final String			MEMENTO_COLUMN_WIDTH				= "tourbookview.column_width";					//$NON-NLS-1$

	private static IMemento				fSessionMemento;

	private ViewerDetailForm			fViewerDetailForm;
	private SashForm					fSashDetail;
	private PageBook					fPageBookDetailChart;

	private PageBook					fPageBookDetailStatistic;
	private Label						fPageDetailNoChart;

	private Label						fDetailNoStatistic;
	private TreeViewer					fTourViewer;

	private ColumnManager				fColumnManager;

	private StatisticContainer			fStatistics;
	private TourChart					fTourChart;
	private Tour						fTour;
	private TourChartConfiguration		fTourChartConfig;

	private TourData					fTourChartTourData;

	private PostSelectionProvider		fPostSelectionProvider;
	private ISelectionListener			fPostSelectionListener;
	private IPartListener2				fPartListener;

	private IPropertyChangeListener		fPrefChangeListener;

	TVITourBookRoot						fRootItem;
	TourPerson							fActivePerson;

	long								fActiveTourTypeId;

	private boolean						fCanTourChartBeVisible;

	public NumberFormat					fNF									= NumberFormat
																					.getNumberInstance();

	private RGB							fRGBYearFg							= new RGB(255, 255, 255);
	private RGB							fRGBMonthFg							= new RGB(128, 64, 0);
	private RGB							fRGBTourFg							= new RGB(0, 0, 128);

	private RGB							fRGBYearBg							= new RGB(111, 130, 197);
	private RGB							fRGBMonthBg							= new RGB(220, 220, 255);
	private RGB							fRGBTourBg							= new RGB(240, 240, 255);

	private Color						fColorYearFg;
	private Color						fColorMonthFg;
	private Color						fColorTourFg;

	private Color						fColorYearBg;
	private Color						fColorMonthBg;
	private Color						fColorTourBg;

	public Font							fFontNormal;
	public Font							fFontBold;

	private ActionEditTour				fActionEditTour;
	private ActionDeleteTour			fActionDeleteTour;
	private ActionSetTourType			fActionSetTourType;
	private ActionSetTourType			fActionSetLastTourType;
	private ActionModifyColumns			fActionModifyColumns;

	private ActionShowViewDetails		fActionShowViewDetailsBoth;
	private ActionShowViewDetailsViewer	fActionShowViewDetailsViewer;
	private ActionShowViewDetailsDetail	fActionShowViewDetailsDetail;

	private ActionShowStatistic			fActionShowDetailStatistic;
	private ActionShowTourChart			fActionShowDetailTourChart;

	private int							fStatisticYear;

	private int							fTourViewerSelectedYear				= -1;
	private int							fTourViewerSelectedMonth			= -1;

	protected Long						fActiveTourId;
	private int							fLastSelectedTourTypeId;

	private int							fViewerWidth;

	private class TourBookContentProvider implements ITreeContentProvider {

		public void dispose() {}

		public Object[] getChildren(Object parentElement) {
			return ((TreeViewerItem) parentElement).getFetchedChildren();
		}

		public Object[] getElements(Object inputElement) {
			return fRootItem.getFetchedChildren();
		}

		public Object getParent(Object element) {
			return ((TreeViewerItem) element).getParentItem();
		}

		public boolean hasChildren(Object element) {
			return ((TreeViewerItem) element).hasChildren();
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
	}

	private void addPartListener() {

		// set the part listener
		fPartListener = new IPartListener2() {
			public void partActivated(IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(IWorkbenchPartReference partRef) {}

			public void partClosed(IWorkbenchPartReference partRef) {
				if (ID.equals(partRef.getId()))
					saveSettings();
			}

			public void partDeactivated(IWorkbenchPartReference partRef) {
				if (ID.equals(partRef.getId())) {
					// saveSettings();
				}
			}

			public void partHidden(IWorkbenchPartReference partRef) {}

			public void partInputChanged(IWorkbenchPartReference partRef) {}

			public void partOpened(IWorkbenchPartReference partRef) {}

			public void partVisible(IWorkbenchPartReference partRef) {}
		};
		// register the listener in the page
		getViewSite().getPage().addPartListener(fPartListener);
	}

	private void addPrefListener() {

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				/*
				 * set a new chart configuration when the preferences has changed
				 */
				if (property.equals(ITourbookPreferences.GRAPH_VISIBLE)
						|| property.equals(ITourbookPreferences.GRAPH_X_AXIS)
						|| property.equals(ITourbookPreferences.GRAPH_X_AXIS_STARTTIME)) {

					fTourChartConfig = TourManager.createTourChartConfiguration();
				}

				if (property.equals(ITourbookPreferences.APP_NEW_DATA_FILTER)) {

					fActivePerson = TourbookPlugin.getDefault().getActivePerson();
					fActiveTourTypeId = TourbookPlugin.getDefault().getActiveTourType().getTypeId();

					refreshTourViewer();
					refreshStatistics();
				}

				if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

					// update tourbook viewer
					UI.getInstance().disposeTourTypeImages();
					fTourViewer.refresh();

					// update statistics
					refreshStatistics();
				}
			}
		};

		// register the listener
		TourbookPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(
				fPrefChangeListener);
	}

	private void createActions() {

		fActionEditTour = new ActionEditTour(this);
		fActionDeleteTour = new ActionDeleteTour(this);
		fActionSetLastTourType = new ActionSetTourType(this, false);
		fActionSetTourType = new ActionSetTourType(this, true);
		fActionModifyColumns = new ActionModifyColumns(fColumnManager);

		fActionShowViewDetailsBoth = new ActionShowViewDetails(this);
		fActionShowViewDetailsViewer = new ActionShowViewDetailsViewer(this);
		fActionShowViewDetailsDetail = new ActionShowViewDetailsDetail(this);

		fActionShowDetailStatistic = new ActionShowStatistic(this);
		fActionShowDetailTourChart = new ActionShowTourChart(this);

		/*
		 * fill toolbar
		 */
		IToolBarManager viewTbm = getViewSite().getActionBars().getToolBarManager();

		viewTbm.add(fActionShowViewDetailsViewer);
		viewTbm.add(fActionShowViewDetailsBoth);
		viewTbm.add(fActionShowViewDetailsDetail);
		viewTbm.add(new Separator());
		viewTbm.add(fActionShowDetailStatistic);
		viewTbm.add(fActionShowDetailTourChart);
	}

	/**
	 * create the views context menu
	 */
	private void createContextMenu() {

		MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				TourBookView.this.fillContextMenu(manager);
			}
		});

		// add the context menu to the table viewer
		Control tourViewer = fTourViewer.getControl();
		Menu menu = menuMgr.createContextMenu(tourViewer);
		tourViewer.setMenu(menu);
	}

	private Control createDetail(Composite parent) {

		// sash for statistics and tour chart
		fSashDetail = new SashForm(parent, SWT.VERTICAL);
		fSashDetail.setOrientation(SWT.VERTICAL);

		/*
		 * pagebook: statistic
		 */
		fPageBookDetailStatistic = new PageBook(fSashDetail, SWT.NONE);
		fPageBookDetailStatistic.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		fStatistics = new StatisticContainer(this, fPageBookDetailStatistic, SWT.NONE);

		fDetailNoStatistic = new Label(fPageBookDetailStatistic, SWT.NONE);
		fDetailNoStatistic.setText(Messages.TourBook_Lable_no_statistic_is_selected);

		/*
		 * pagebook: chart
		 */
		fPageBookDetailChart = new PageBook(fSashDetail, SWT.NONE);
		fPageBookDetailChart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		fPageDetailNoChart = new Label(fPageBookDetailChart, SWT.NONE);
		fPageDetailNoChart.setText(Messages.TourBook_Label_no_tour_is_selected);

		fTour = new Tour(fPageBookDetailChart, SWT.FLAT);
		fTour.restoreState(fSessionMemento);
		fTour.setFont(parent.getFont());
//		fTour.addTourChangedListener(new ITourChangeListener() {
//			public void tourChanged(TourChangeEvent event) {
//				TourbookPlugin.getDefault().getPreferenceStore().setValue(
//						ITourbookPreferences.APP_NEW_DATA_FILTER,
//						Math.random());
//			}
//		});

		fTourChart = fTour.getTourChart();
		fTourChart.setShowZoomActions(true);
		fTourChart.setShowSlider(true);
		fTourChart.setContextProvider(new TourChartContextProvider(this));

		// fire a slider move selection when a slider was moved in the tour
		// chart
		fTourChart.addSliderMoveListener(new ISliderMoveListener() {
			public void sliderMoved(SelectionChartInfo chartInfoSelection) {
				fPostSelectionProvider.setSelection(chartInfoSelection);
			}
		});

		fTourChart.addTourChartListener(new ITourChartSelectionListener() {
			public void selectedTourChart(SelectionTourChart tourChart) {
				firePostSelection(tourChart);
			}
		});

		fTourChartConfig = TourManager.createTourChartConfiguration();
		fTourChartConfig.setMinMaxKeeper(true);

		return fSashDetail;
	}

	public void createPartControl(Composite parent) {

		createResources();

		final Control tree = createTourViewer(parent);
		final Sash sash = new Sash(parent, SWT.VERTICAL);
		final Control detail = createDetail(parent);

		fViewerDetailForm = new ViewerDetailForm(parent, tree, sash, detail);

		createActions();
		createContextMenu();

		// set selection provider
		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		setPostSelectionListener();
		addPartListener();
		addPrefListener();

		fActivePerson = TourbookPlugin.getDefault().getActivePerson();
		fActiveTourTypeId = TourbookPlugin.getDefault().getActiveTourType().getTypeId();
		restoreState(fSessionMemento);

		// update the viewer
		fRootItem = new TVITourBookRoot(this);
		fTourViewer.setInput(this);

		reselectTourViewer();
	}

	private void createResources() {

		Display display = Display.getCurrent();

		fColorYearFg = new Color(display, fRGBYearFg);
		fColorYearBg = new Color(display, fRGBYearBg);
		fColorMonthFg = new Color(display, fRGBMonthFg);
		fColorMonthBg = new Color(display, fRGBMonthBg);
		fColorTourFg = new Color(display, fRGBTourFg);
		fColorTourBg = new Color(display, fRGBTourBg);

		fFontNormal = JFaceResources.getFontRegistry().get(JFaceResources.DIALOG_FONT);
		fFontBold = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);
	}

	private Control createTourViewer(Composite parent) {

		// tour tree
		final Tree tree = new Tree(parent, SWT.H_SCROLL
				| SWT.V_SCROLL
				| SWT.BORDER
				| SWT.FLAT
				| SWT.FULL_SELECTION
				| SWT.MULTI);

		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tree.setHeaderVisible(true);
		tree.setLinesVisible(false);

		fTourViewer = new TreeViewer(tree);

		// define and create all columns
		fColumnManager = new ColumnManager(fTourViewer);
		defineAllColumns(parent);
		fColumnManager.createColumns();

		fTourViewer.setContentProvider(new TourBookContentProvider());
		fTourViewer.setUseHashlookup(true);

		fTourViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {

				Object selectedItem = ((IStructuredSelection) (event.getSelection()))
						.getFirstElement();

				if (selectedItem instanceof TVITourBookYear) {

					// year is selected

					fStatisticYear = ((TVITourBookYear) selectedItem).fTourYear;

					fStatistics.refreshStatistic(
							fActivePerson,
							fActiveTourTypeId,
							fStatisticYear,
							false);

					fCanTourChartBeVisible = false;
					manageDetailVisibility();

				} else if (selectedItem instanceof TVITourBookMonth) {

					// month is selected

					TVITourBookMonth monthItem = (TVITourBookMonth) selectedItem;
					fStatisticYear = ((TVITourBookYear) (monthItem.getParentItem())).fTourYear;

					fStatistics.refreshStatistic(
							fActivePerson,
							fActiveTourTypeId,
							fStatisticYear,
							false);

					fStatistics.selectMonth(monthItem.fTourDate);

					fCanTourChartBeVisible = false;
					manageDetailVisibility();

				} else if (selectedItem instanceof TVITourBookTour) {

					// tour is selected

					TVITourBookTour tourItem = (TVITourBookTour) selectedItem;

					// show the statistic for the year in the tour
					TreeViewerItem yearItem = tourItem.getParentItem().getParentItem();
					if (yearItem instanceof TVITourBookYear) {
						TVITourBookYear tviYear = (TVITourBookYear) yearItem;
						if (fStatisticYear != tviYear.fTourYear) {
							fStatisticYear = tviYear.fTourYear;
							fStatistics.refreshStatistic(
									fActivePerson,
									fActiveTourTypeId,
									fStatisticYear,
									false);
						}
					}

					fStatistics.selectDay(tourItem.fTourDate);

					fActiveTourId = tourItem.getTourId();

					fStatistics.selectTour(fActiveTourId);

					if (fActionShowDetailTourChart.isChecked() == false) {
						fActionShowDetailTourChart.setChecked(true);
					}
					if (fActionShowDetailTourChart.isChecked()) {

						showTourChartInView(fActiveTourId);

						fCanTourChartBeVisible = true;
						manageDetailVisibility();
					}
				}

				enableActions();
			}
		});

		fTourViewer.addDoubleClickListener(new IDoubleClickListener() {

			public void doubleClick(DoubleClickEvent event) {

				Object selection = ((IStructuredSelection) fTourViewer.getSelection())
						.getFirstElement();

				if (selection instanceof TVITourBookTour) {

					// open tour in editor

					TVITourBookTour tourItem = (TVITourBookTour) selection;
					TourManager.getInstance().openTourInEditor(tourItem.getTourId());

				} else if (selection != null) {

					// expand/collapse current item

					TreeViewerItem tourItem = (TreeViewerItem) selection;

					if (fTourViewer.getExpandedState(tourItem)) {
						fTourViewer.collapseToLevel(tourItem, 1);
					} else {
						fTourViewer.expandToLevel(tourItem, 1);
					}
				}
			}
		});

		return tree;
	}

	/**
	 * Defines all columns for the table viewer in the column manager
	 * 
	 * @param parent
	 */
	private void defineAllColumns(Composite parent) {

		PixelConverter pixelConverter = new PixelConverter(parent);
		TreeColumnDefinition colDef;

		/*
		 * column: date
		 */
		colDef = TreeColumnFactory.DATE.createColumn(fColumnManager, pixelConverter);
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				final Object element = cell.getElement();
				TourBookTreeViewerItem tourItem = (TourBookTreeViewerItem) element;
				cell.setText(Long.toString(tourItem.fFirstColumn));
				setCellColor(cell, element);
			}
		});

		/*
		 * column: title
		 */
		colDef = TreeColumnFactory.TITLE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				final Object element = cell.getElement();
				TourBookTreeViewerItem tourItem = (TourBookTreeViewerItem) element;
				if (element instanceof TVITourBookTour) {
					cell.setText(tourItem.fTourTitle);
				} else {
					setCellColor(cell, element);
				}
			}
		});

		/*
		 * column: distance (km)
		 */
		colDef = TreeColumnFactory.DISTANCE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				final Object element = cell.getElement();
				TourBookTreeViewerItem tourItem = (TourBookTreeViewerItem) element;
				fNF.setMinimumFractionDigits(1);
				fNF.setMaximumFractionDigits(1);
				cell.setText(fNF.format(((float) tourItem.fColumnDistance) / 1000));
				setCellColor(cell, element);
			}
		});

		/*
		 * column: tour type
		 */
		colDef = TreeColumnFactory.TOUR_TYPE.createColumn(fColumnManager, pixelConverter);
//		colDef.setColumnResizable(false);
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVITourBookTour) {
					cell.setImage(UI.getInstance().getTourTypeImage(
							((TVITourBookTour) element).getTourTypeId()));
				} else {
					setCellColor(cell, element);
				}
			}
		});

		/*
		 * column: recording time (h)
		 */
		colDef = TreeColumnFactory.RECORDING_TIME.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {

				final Object element = cell.getElement();
				long recordingTime = ((TourBookTreeViewerItem) element).fColumnRecordingTime;

				cell.setText(new Formatter().format(
						Messages.Format_hhmm,
						(recordingTime / 3600),
						((recordingTime % 3600) / 60)).toString());
				setCellColor(cell, element);
			}
		});

		/*
		 * column: driving time (h)
		 */
		colDef = TreeColumnFactory.DRIVING_TIME.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {

				final Object element = cell.getElement();
				long drivingTime = ((TourBookTreeViewerItem) element).fColumnDrivingTime;

				cell.setText(new Formatter().format(
						Messages.Format_hhmm,
						(drivingTime / 3600),
						((drivingTime % 3600) / 60)).toString());
				setCellColor(cell, element);
			}
		});

		/*
		 * column: altitude up (m)
		 */
		colDef = TreeColumnFactory.ALTITUDE_UP.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				final Object element = cell.getElement();
				TourBookTreeViewerItem tourItem = (TourBookTreeViewerItem) element;
				cell.setText(Long.toString(tourItem.fColumnAltitudeUp));
				setCellColor(cell, element);
			}
		});

		/*
		 * column: number of tours
		 */
		colDef = TreeColumnFactory.TOUR_COUNTER.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				final Object element = cell.getElement();
				if ((element instanceof TVITourBookTour) == false) {
					cell.setText(Long.toString(((TourBookTreeViewerItem) element).fColumnCounter));
				}
				setCellColor(cell, element);
			}
		});

		/*
		 * column: device distance
		 */
		colDef = TreeColumnFactory.DEVICE_DISTANCE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVITourBookTour) {
					cell.setText(Long
							.toString(((TVITourBookTour) element).getColumnStartDistance()));
				}
				setCellColor(cell, element);
			}
		});

		colDef = TreeColumnFactory.TIME_INTERVAL.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVITourBookTour) {
					cell
							.setText(Long.toString(((TVITourBookTour) element)
									.getColumnTimeInterval()));
				}
				setCellColor(cell, element);
			}
		});

		colDef = TreeColumnFactory.MAX_SPEED.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				final Object element = cell.getElement();
				TourBookTreeViewerItem tourItem = (TourBookTreeViewerItem) element;
				fNF.setMinimumFractionDigits(1);
				fNF.setMaximumFractionDigits(1);
				cell.setText(fNF.format(tourItem.fColumnMaxSpeed));
				setCellColor(cell, element);
			}
		});

		colDef = TreeColumnFactory.AVG_SPEED.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				final Object element = cell.getElement();
				TourBookTreeViewerItem tourItem = (TourBookTreeViewerItem) element;
				fNF.setMinimumFractionDigits(1);
				fNF.setMaximumFractionDigits(1);
				cell.setText(fNF.format(tourItem.fColumnAvgSpeed));
				setCellColor(cell, element);
			}
		});

		colDef = TreeColumnFactory.MAX_ALTITUDE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				final Object element = cell.getElement();
				TourBookTreeViewerItem tourItem = (TourBookTreeViewerItem) element;
				cell.setText(Long.toString(tourItem.fColumnMaxAltitude));
				setCellColor(cell, element);
			}
		});

		colDef = TreeColumnFactory.MAX_PULSE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				final Object element = cell.getElement();
				TourBookTreeViewerItem tourItem = (TourBookTreeViewerItem) element;
				cell.setText(Long.toString(tourItem.fColumnMaxPulse));
				setCellColor(cell, element);
			}
		});

		colDef = TreeColumnFactory.AVG_PULSE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				final Object element = cell.getElement();
				TourBookTreeViewerItem tourItem = (TourBookTreeViewerItem) element;
				cell.setText(Long.toString(tourItem.fColumnAvgPulse));
				setCellColor(cell, element);
			}
		});

		colDef = TreeColumnFactory.AVG_CADENCE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				final Object element = cell.getElement();
				TourBookTreeViewerItem tourItem = (TourBookTreeViewerItem) element;
				cell.setText(Long.toString(tourItem.fColumnAvgCadence));
				setCellColor(cell, element);
			}
		});

		colDef = TreeColumnFactory.AVG_TEMPERATURE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			public void update(ViewerCell cell) {
				final Object element = cell.getElement();
				TourBookTreeViewerItem tourItem = (TourBookTreeViewerItem) element;
				cell.setText(Long.toString(tourItem.fColumnAvgTemperature));
				setCellColor(cell, element);
			}
		});

		// TableColumnFactory.DEVICE_PROFILE.createColumn(fColumnManager,
		// pixelConverter);
// TableColumnFactory.DEVICE_NAME.createColumn(fColumnManager, pixelConverter);

	}

	public void dispose() {

		getSite().getPage().removePostSelectionListener(fPostSelectionListener);
		getViewSite().getPage().removePartListener(fPartListener);

		TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(
				fPrefChangeListener);

		fColorYearFg.dispose();
		fColorYearBg.dispose();
		fColorMonthFg.dispose();
		fColorMonthBg.dispose();
		fColorTourFg.dispose();
		fColorTourBg.dispose();

		super.dispose();
	}

	@SuppressWarnings("unchecked")//$NON-NLS-1$
	private void enableActions() {

		ITreeSelection selection = (ITreeSelection) fTourViewer.getSelection();

		// number ob selected tour items
		int tourItems = 0;

		// count how many tour items are selected
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			if (iter.next() instanceof TVITourBookTour) {
				tourItems++;
			}
		}

		fActionEditTour.setEnabled(tourItems == 1);

		// enable the delete button when only tours are selected
		if (tourItems > 0 && selection.size() == tourItems) {
			fActionDeleteTour.setEnabled(true);
		} else {
			fActionDeleteTour.setEnabled(false);
		}

		fActionSetTourType.setEnabled(tourItems > 0);
		fActionSetLastTourType.setEnabled(tourItems > 0);
	}

	private void fillContextMenu(IMenuManager menuMgr) {

		TourType selectedTourType;
		if ((selectedTourType = fActionSetTourType.getSelectedTourType()) != null) {

			fActionSetLastTourType.setSelectedTourType(selectedTourType);
			fActionSetLastTourType.setText(NLS.bind(
					Messages.TourBook_Action_set_tour_type,
					selectedTourType.getName()));
			fActionSetLastTourType.setEnabled(true);
			menuMgr.add(fActionSetLastTourType);
		} else {
			fActionSetLastTourType.setEnabled(false);
		}

		menuMgr.add(fActionSetTourType);

		menuMgr.add(new Separator());
		menuMgr.add(fActionEditTour);
		menuMgr.add(fActionDeleteTour);

		menuMgr.add(new Separator());
		menuMgr.add(fActionModifyColumns);

		enableActions();
	}

	void firePostSelection(ISelection selection) {

		if (selection instanceof SelectionRemovedTours) {
			refreshStatistics();
		}

		fPostSelectionProvider.setSelection(selection);
	}

	TourChart getTourChart() {
		return fTourChart;
	}

	TreeViewer getTourViewer() {
		return fTourViewer;
	}

	/**
	 * @param initializeYearMonth
	 *        reset the selected year/month when set to <code>true</code>
	 */
	private void getTourViewerSelection(boolean initializeYearMonth) {

		if (initializeYearMonth) {
			fTourViewerSelectedYear = -1;
			fTourViewerSelectedMonth = -1;
		}

		ITreeSelection selectedItems = (ITreeSelection) fTourViewer.getSelection();
		TreePath[] treePaths = selectedItems.getPaths();

		// get selected year/month
		for (TreePath treePath : treePaths) {
			for (int segmentIndex = 0; segmentIndex < treePath.getSegmentCount(); segmentIndex++) {
				Object treeItem = treePath.getSegment(segmentIndex);
				if (treeItem instanceof TVITourBookYear) {
					fTourViewerSelectedYear = ((TVITourBookYear) treeItem).fTourYear;
				} else if (treeItem instanceof TVITourBookMonth) {
					fTourViewerSelectedMonth = ((TVITourBookMonth) treeItem).fTourMonth;
				}
			}

			// currently only the first selected entry is supported
			break;
		}
	}

	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);

		// set the session memento if it's not yet set
		if (fSessionMemento == null) {
			fSessionMemento = memento;
		}
	}

	public void manageDetailVisibility() {

		boolean isStatVisible = fActionShowDetailStatistic.isChecked();
		boolean isTourChartVisible = fActionShowDetailTourChart.isChecked();

		if (fCanTourChartBeVisible) {
			fPageBookDetailChart.showPage(fTour);
			fActionShowDetailTourChart.setChecked(true);
			isTourChartVisible = true;
		} else {
			fPageBookDetailChart.showPage(fPageDetailNoChart);
		}

		if (isStatVisible) {
			fPageBookDetailStatistic.showPage(fStatistics);
		} else {
			fPageBookDetailStatistic.showPage(fDetailNoStatistic);
		}

		fSashDetail.setMaximizedControl(isStatVisible && isTourChartVisible ? null : isStatVisible
				? fPageBookDetailStatistic
				: fPageBookDetailChart);
	}

	public void manageDetailVisibility(IAction action) {

		if (action == fActionShowDetailStatistic) {

			// action: statistics

			if (!action.isChecked() && !fActionShowDetailTourChart.isChecked()) {

				// reckeck current button
				action.setChecked(true);
			}

		} else if (action == fActionShowDetailTourChart) {

			// action: tour chart

			if (action.isChecked() == false && !fActionShowDetailStatistic.isChecked()) {

				// force the tour chart button be checked when statistics is
				// hidden
				action.setChecked(true);

			} else if (action.isChecked() == false) {

				// hide the tour chart
				fCanTourChartBeVisible = false;

				// fStatistics.resetSelection();
			} else if (action.isChecked() && fActiveTourId == null) {

				action.setChecked(false);

			} else if (action.isChecked() && fActiveTourId != null) {

				showTourChartInView(fActiveTourId);

				fCanTourChartBeVisible = true;
			}
		}

		manageDetailVisibility();
	}

	/**
	 * @param action
	 *        action which was enable/disabled
	 */
	public void manageViewerVisibility(IAction action) {

		// keep the width of the tree viewer
		int viewerWidth = fTourViewer.getTree().getSize().x;
		if (viewerWidth > 0) {
			fViewerWidth = viewerWidth;
		}

		if (action == fActionShowViewDetailsBoth) {

			// show viewer and details

			if (fActionShowViewDetailsBoth.isChecked() == false) {
				// recheck the button
				fActionShowViewDetailsBoth.setChecked(true);
			} else {

				fActionShowViewDetailsViewer.setChecked(false);
				fActionShowViewDetailsDetail.setChecked(false);

				fStatistics.setYearComboVisibility(false);

				fViewerDetailForm.setMaximizedControl(null);
			}

		} else if (action == fActionShowViewDetailsViewer) {

			// show only the viewer

			if (fActionShowViewDetailsViewer.isChecked() == false) {
				// recheck the button
				fActionShowViewDetailsViewer.setChecked(true);
			} else {

				fActionShowViewDetailsBoth.setChecked(false);
				fActionShowViewDetailsDetail.setChecked(false);

				fStatistics.setYearComboVisibility(false);

				fViewerDetailForm.setMaximizedControl(fTourViewer.getTree());
			}

		} else {

			// show only the details

			if (fActionShowViewDetailsDetail.isChecked() == false) {
				// recheck the button
				fActionShowViewDetailsDetail.setChecked(true);
			} else {

				fActionShowViewDetailsBoth.setChecked(false);
				fActionShowViewDetailsViewer.setChecked(false);

				fStatistics.setYearComboVisibility(true);

				fViewerDetailForm.setMaximizedControl(fSashDetail);
			}
		}

		manageDetailVisibility();
	}

	public void openTourChart(long tourId) {
	// TourManager.getInstance().openTourInEditor(tourId);
	}

	void refreshStatistics() {
		fStatistics.refreshStatistic(fActivePerson, fActiveTourTypeId);
	}

	void refreshTour(TourData tourData) {
		fTourChart.updateChart(fTourChartTourData, fTourChartConfig, false);
		fTour.refreshTourData(tourData);
	}

	private void refreshTourViewer() {

		getTourViewerSelection(true);

		// refresh the tree viewer
		fRootItem.fetchChildren();
		fTourViewer.refresh();

		if (fTourViewerSelectedYear == -1) {
			return;
		}

		reselectTourViewer();
	}

	private void reselectTourViewer() {

		// find the old selected year/month in the new tour items
		TreeViewerItem newYearItem = null;
		TreeViewerItem newMonthItem = null;
		ArrayList<TreeViewerItem> yearItems = fRootItem.getChildren();

		/*
		 * get the year and month item in the data model
		 */
		for (TreeViewerItem yearItem : yearItems) {
			TVITourBookYear tourBookYear = ((TVITourBookYear) yearItem);
			if (tourBookYear.fTourYear == fTourViewerSelectedYear) {
				newYearItem = yearItem;

				Object[] monthItems = tourBookYear.getFetchedChildren();
				for (Object monthItem : monthItems) {
					TVITourBookMonth tourBookMonth = ((TVITourBookMonth) monthItem);
					if (tourBookMonth.fTourMonth == fTourViewerSelectedMonth) {
						newMonthItem = tourBookMonth;
						break;
					}
				}
				break;
			}
		}

		// select year/month in the viewer
		if (newMonthItem != null) {
			fTourViewer.setSelection(new StructuredSelection(newMonthItem) {}, false);
		} else if (newYearItem != null) {
			fTourViewer.setSelection(new StructuredSelection(newYearItem) {}, false);
		} else if (yearItems.size() > 0) {

			// the old year was not found, select the newest year

			TreeViewerItem yearItem = yearItems.get(yearItems.size() - 1);

			fTourViewer.setSelection(new StructuredSelection(yearItem) {}, true);
		} else {
			/*
			 * years are not available, force the chart the be displayed
			 */
			fStatistics.refreshStatistic(fActivePerson, fActiveTourTypeId, fStatisticYear, true);
		}
	}

	private void restoreState(IMemento memento) {

		if (memento != null) {

			/*
			 * restore states from the memento
			 */

			fViewerDetailForm.setViewerWidth(fSessionMemento.getInteger(MEMENTO_VIEWER_WIDTH));

			// restore sash weights
			UI.restoreSashWeight(fSashDetail, memento, MEMENTO_SASH_WEIGHT_DETAIL, new int[] {
					50,
					50 });

			// viewer/detail visibility
			Integer containerVisibleStatus = memento.getInteger(MEMENTO_VISIBLE_STATUS_CONTAINER);

			if (containerVisibleStatus == null || containerVisibleStatus == 0) {
				fActionShowViewDetailsBoth.setChecked(true);
				manageViewerVisibility(fActionShowViewDetailsBoth);
			} else if (containerVisibleStatus == 1) {
				fActionShowViewDetailsViewer.setChecked(true);
				manageViewerVisibility(fActionShowViewDetailsViewer);

			} else {
				fActionShowViewDetailsDetail.setChecked(true);
				manageViewerVisibility(fActionShowViewDetailsDetail);
			}

			// show/hide tour chart
			Integer detailVisibleStatus = memento.getInteger(MEMENTO_VISIBLE_STATUS_DETAIL);

			fActionShowDetailStatistic.setChecked(detailVisibleStatus == null
					|| detailVisibleStatus == 0
					|| detailVisibleStatus == 1);
			fActionShowDetailTourChart.setChecked(detailVisibleStatus == null
					|| detailVisibleStatus == 0
					|| detailVisibleStatus == 2);

			// set tour viewer reselection data
			Integer selectedYear = memento.getInteger(MEMENTO_TOURVIEWER_SELECTED_YEAR);
			Integer selectedMonth = memento.getInteger(MEMENTO_TOURVIEWER_SELECTED_MONTH);
			fTourViewerSelectedYear = selectedYear == null ? -1 : selectedYear;
			fTourViewerSelectedMonth = selectedMonth == null ? -1 : selectedMonth;

			// restore columns sort order
			final String mementoColumnSortOrderIds = memento.getString(MEMENTO_COLUMN_SORT_ORDER);
			if (mementoColumnSortOrderIds != null) {
				fColumnManager.orderColumns(StringToArrayConverter
						.convertStringToArray(mementoColumnSortOrderIds));
			}

			// restore column width
			final String mementoColumnWidth = memento.getString(MEMENTO_COLUMN_WIDTH);
			if (mementoColumnWidth != null) {
				fColumnManager.setColumnWidth(StringToArrayConverter
						.convertStringToArray(mementoColumnWidth));
			}

		} else {

			fActionShowViewDetailsBoth.setChecked(true);
			manageViewerVisibility(fActionShowViewDetailsBoth);

			fActionShowDetailStatistic.setChecked(true);
			fActionShowDetailTourChart.setChecked(false);
		}

		manageDetailVisibility();
		fStatistics.restoreStatistics(memento);
	}

	private void saveSettings() {
		fSessionMemento = XMLMemento.createWriteRoot("DeviceImportView"); //$NON-NLS-1$
		saveState(fSessionMemento);
	}

	public void saveState(IMemento memento) {

		// keep viewer width
		int viewerWidth = fTourViewer.getTree().getSize().x;
		memento.putInteger(MEMENTO_VIEWER_WIDTH, viewerWidth > 0 ? viewerWidth : fViewerWidth);

		// save sash weights
		UI.saveSashWeight(fSashDetail, memento, MEMENTO_SASH_WEIGHT_DETAIL);

		// viewer status:
		// -------------
		// 0: viewer + detail
		// 1: only viewer
		// 2: only detail
		int viewerStatus = fActionShowViewDetailsBoth.isChecked()
				? 0
				: fActionShowViewDetailsViewer.isChecked() ? 1 : 2;
		memento.putInteger(MEMENTO_VISIBLE_STATUS_CONTAINER, viewerStatus);

		// detail status:
		// -------------
		// 0: stat + chart
		// 1: only stat
		// 2: only chart
		boolean isStatVisible = fActionShowDetailStatistic.isChecked();
		boolean isChartVisible = fActionShowDetailTourChart.isChecked();
		memento.putInteger(MEMENTO_VISIBLE_STATUS_DETAIL, isStatVisible && isChartVisible
				? 0
				: isStatVisible ? 1 : 2);

		// save selection in the tour viewer
		if (viewerStatus != 2) {
			// statistic is not visible
			getTourViewerSelection(false);
			memento.putInteger(MEMENTO_TOURVIEWER_SELECTED_YEAR, fTourViewerSelectedYear);
			memento.putInteger(MEMENTO_TOURVIEWER_SELECTED_MONTH, fTourViewerSelectedMonth);
		} else {
			// only the statistic is visible and the year combo box
			memento.putInteger(MEMENTO_TOURVIEWER_SELECTED_YEAR, fStatistics.getActiveYear());
			memento.putInteger(MEMENTO_TOURVIEWER_SELECTED_MONTH, -1);
		}

		memento.putInteger(MEMENTO_LAST_SELECTED_TOUR_TYPE_ID, fLastSelectedTourTypeId);

		// save column sort order
		memento.putString(MEMENTO_COLUMN_SORT_ORDER, StringToArrayConverter
				.convertArrayToString(fColumnManager.getColumnIds()));

		// save columns width
		memento.putString(MEMENTO_COLUMN_WIDTH, StringToArrayConverter
				.convertArrayToString(fColumnManager.getColumnIdAndWidth()));

		fStatistics.saveState(memento);
		fTour.saveState(memento);
	}

	public void setActiveYear(int activeYear) {
		fTourViewerSelectedYear = activeYear;
	}

	private void setCellColor(ViewerCell cell, final Object element) {

		if (element instanceof TVITourBookMonth) {
			cell.setBackground(fColorMonthBg);
		}
		if (element instanceof TVITourBookYear) {
			cell.setForeground(fColorYearFg);
			cell.setBackground(fColorYearBg);
		}
	}

	public void setFocus() {
		fTourViewer.getControl().setFocus();
	}

	private void setPostSelectionListener() {
		// this view part is a selection listener
		fPostSelectionListener = new ISelectionListener() {

			public void selectionChanged(IWorkbenchPart part, ISelection selection) {

				if (!selection.isEmpty() && selection instanceof SelectionRawData) {
					refreshTourViewer();
					refreshStatistics();
				}

				if (selection instanceof SelectionTourSegmentLayer) {
					fTourChart
							.updateSegmentLayer(((SelectionTourSegmentLayer) selection).isLayerVisible);
				}

				if (selection instanceof SelectionChartXSliderPosition) {
					fTourChart.setXSliderPosition((SelectionChartXSliderPosition) selection);
				}
			}
		};

		// register selection listener in the page
		getSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

	public void showTourChart(boolean isVisible) {

		if (fActionShowDetailTourChart.isChecked()) {

			// show the chart when action is checked

			fCanTourChartBeVisible = isVisible;
			manageDetailVisibility();
		}
	}

	public void showTourChart(long tourId) {

		if (tourId == -1) {

			// hide the tour chart

			fCanTourChartBeVisible = false;

		} else {

			// show the tour

			fActiveTourId = tourId;

			// select the tour in the statistic
			fStatistics.selectTour(tourId);

			fActionShowDetailTourChart.setChecked(true);

			if (fActionShowDetailTourChart.isChecked()) {

				// show the chart when action is checked

				showTourChartInView(tourId);

				fSashDetail.setMaximizedControl(null);

				fActionShowDetailStatistic.setEnabled(true);
				fActionShowDetailTourChart.setEnabled(true);

				fActionShowDetailStatistic.setChecked(true);
				fActionShowDetailTourChart.setChecked(true);

				fCanTourChartBeVisible = true;
			}
		}

		manageDetailVisibility();
	}

	private void showTourChartInView(long tourId) {

		// load the tourdata from the database
		fTourChartTourData = TourDatabase.getTourDataByTourId(tourId);

		if (fTourChartTourData != null) {

			// show the tour chart

			fTourChart.addDataModelListener(new IDataModelListener() {
				public void dataModelChanged(ChartDataModel changedChartDataModel) {

					// set title
					changedChartDataModel.setTitle(NLS.bind(
							Messages.TourBook_Label_chart_title,
							TourManager.getTourTitleDetailed(fTourChartTourData)));
				}
			});

			fTourChart.updateChart(fTourChartTourData, fTourChartConfig, false);
			fTour.refreshTourData(fTourChartTourData);

			firePostSelection(new SelectionTourChart(fTourChart));
		}
	}

}
