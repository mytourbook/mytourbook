/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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
package net.tourbook.views.tourBook;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;

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
import net.tourbook.tour.ITourChartListener;
import net.tourbook.tour.SelectionTourChart;
import net.tourbook.tour.Tour;
import net.tourbook.tour.TourChart;
import net.tourbook.tour.TourChartConfiguration;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TreeViewerItem;
import net.tourbook.tour.Tour.ITourChangeListener;
import net.tourbook.tour.Tour.TourChangeEvent;
import net.tourbook.ui.ColorCache;
import net.tourbook.ui.ITourChartViewer;
import net.tourbook.ui.UI;
import net.tourbook.ui.ViewerDetailForm;
import net.tourbook.util.PixelConverter;
import net.tourbook.util.PostSelectionProvider;
import net.tourbook.views.SelectionTourSegmentLayer;
import net.tourbook.views.rawData.SelectionRawData;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
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

	private static final String			TOUR_TYPE_PREFIX					= "tourType";

	static public final String			ID									= "net.tourbook.views.tourListView";

	private static final String			MEMENTO_VIEWER_WIDTH				= "tourbookview.sash-weight-container.";
	private static final String			MEMENTO_SASH_WEIGHT_DETAIL			= "tourbookview.sash-weight-detail.";

	private static final String			MEMENTO_VISIBLE_STATUS_CONTAINER	= "tourbookview.visible-status-container";
	private static final String			MEMENTO_VISIBLE_STATUS_DETAIL		= "tourbookview.visible-status-detail";

	private static final String			MEMENTO_TOURVIEWER_SELECTED_YEAR	= "tourbookview.tourviewer.selected-year";
	private static final String			MEMENTO_TOURVIEWER_SELECTED_MONTH	= "tourbookview.tourviewer.selected-month";

	private static final String			MEMENTO_LAST_SELECTED_TOUR_TYPE_ID	= "tourbookview.last-selected-tour-type-id";

	private static final int			COLUMN_DATE							= 0;
	private static final int			COLUMN_DISTANCE						= 1;
	private static final int			COLUMN_RECORDING					= 2;
	private static final int			COLUMN_DRIVING						= 3;
	private static final int			COLUMN_UP							= 4;
	private static final int			COLUMN_DEVICE_DISTANCE				= 5;

	private ViewerDetailForm			fViewerDetailForm;

	private SashForm					fSashDetail;
	private PageBook					fPageBookDetailChart;
	private PageBook					fPageBookDetailStatistic;

	private Label						fPageDetailNoChart;
	private Label						fDetailNoStatistic;
	private TreeViewer					fTourViewer;

	private StatisticContainer			fStatistics;
	private Composite					fViewerContainer;

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

	// private boolean fIsDetailVisible;
	private boolean						fCanTourChartBeVisible;

	private static IMemento				fSessionMemento;

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

	private ActionDeleteTour			fActionDeleteSelectedTour;
	private ActionSetTourType			fActionSetTourType;
	private ActionSetTourType			fActionSetLastTourType;

	private ActionShowViewDetails		fActionShowViewDetailsBoth;
	private ActionShowViewDetailsViewer	fActionShowViewDetailsViewer;
	private ActionShowViewDetailsDetail	fActionShowViewDetailsDetail;

	private ActionShowStatistic			fActionShowDetailStatistic;
	private ActionShowTourChart			fActionShowDetailTourChart;

	private int							fStatisticYear;

	private int							fTourViewerSelectedYear				= -1;
	private int							fTourViewerSelectedMonth			= -1;

	private HashMap<String, Image>		fImages								= new HashMap<String, Image>();
	private ColorCache					fColorCache							= new ColorCache();

	private int							fColorImageHeight					= -1;
	private int							fColorImageWidth;

	protected Long						fActiveTourId;

	private int							fLastSelectedTourTypeId;

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

	private class TourBookLabelProvider extends LabelProvider implements ITableLabelProvider,
			ITableColorProvider, ITableFontProvider {

		public String getColumnText(Object obj, int index) {

			TourBookTreeViewerItem tourItem = (TourBookTreeViewerItem) obj;

			switch (index) {
			case TourBookView.COLUMN_DATE:
				return Long.toString(tourItem.fFirstColumn);

			case TourBookView.COLUMN_DISTANCE:
				fNF.setMinimumFractionDigits(1);
				fNF.setMaximumFractionDigits(1);
				return fNF.format(((float) tourItem.fColumnDistance) / 1000);

			case TourBookView.COLUMN_RECORDING:

				long recordingTime = tourItem.fColumnRecordingTime;

				return new Formatter().format(
						"%d:%02d",
						(recordingTime / 3600),
						((recordingTime % 3600) / 60)).toString();

			case TourBookView.COLUMN_DRIVING:

				long drivingTime = tourItem.fColumnDrivingTime;

				return new Formatter().format(
						"%d:%02d",
						(drivingTime / 3600),
						((drivingTime % 3600) / 60)).toString();

			case TourBookView.COLUMN_UP:
				return Long.toString(tourItem.fColumnAltitudeUp);

			case TourBookView.COLUMN_DEVICE_DISTANCE:

				if (obj instanceof TVITourBookTour) {
					return Long.toString(((TVITourBookTour) obj).getColumnStartDistance());
				} else {
					return Long.toString(tourItem.fColumnCounter);
				}

			default:
				return (getText(obj));
			}

		}

		public Image getColumnImage(Object element, int columnIndex) {

			if (columnIndex == TourBookView.COLUMN_DISTANCE && element instanceof TVITourBookTour) {
				return getTourTypeImage((TVITourBookTour) element);
			}

			return null;
		}

		public Color getBackground(Object element, int columnIndex) {
			// if (columnIndex != 0 && element instanceof TVITourBookTour) {
			// return fColorTourBg;
			// }
			if (/* columnIndex != 0 && */element instanceof TVITourBookMonth) {
				return fColorMonthBg;
			}
			if (/* columnIndex != 0 && */element instanceof TVITourBookYear) {
				return fColorYearBg;
			}

			return null;
		}

		public Color getForeground(Object element, int columnIndex) {
			// if (element instanceof TVITourBookTour) {
			// return fColorTourFg;
			// }
			// if (columnIndex != 0 && element instanceof TVITourBookMonth) {
			// return fColorMonthFg;
			// }
			if (/* columnIndex != 0 && */element instanceof TVITourBookYear) {
				return fColorYearFg;
			}
			return null;
		}

		public Font getFont(Object element, int columnIndex) {
			// if (element instanceof TVITourBookTour) {
			// return fFontBold;
			// }
			if (columnIndex == 0) {

				// if (element instanceof TVITourBookMonth) {
				// return fFontBold;
				// }
				// if (element instanceof TVITourBookYear) {
				// return fFontBold;
				// }
			}

			return fFontNormal;
		}
	}

	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);

		// set the session memento if it's not yet set
		if (fSessionMemento == null) {
			fSessionMemento = memento;
		}
	}

	/**
	 * Set the size for the tour type images
	 * 
	 * @param display
	 * @return
	 */
	private void ensureImageSize(Display display) {

		if (fColorImageHeight == -1) {

			Tree tree = fTourViewer.getTree();

			fColorImageHeight = tree.getItemHeight();
			// fUsableImageHeight = Math.max(1, fColorImageHeight - 3);

			// fColorImageWidth = tree.getItemHeight()/3;
			fColorImageWidth = 8;
			// fUsableColorImageWidth = Math.max(1, fColorImageWidth - 4);
		}
	}

	/**
	 * create the image for the tour type
	 * 
	 * @param tourItem
	 * @return Returns the tour type image which is displayed in the viewer
	 */
	public Image getTourTypeImage(TVITourBookTour tourItem) {

		long typeId = tourItem.fTourTypeId;

		String colorId = TOUR_TYPE_PREFIX + typeId;
		Image image = fImages.get(colorId);

		if (image == null) {

			Display display = Display.getCurrent();
			ensureImageSize(display);
			image = new Image(display, fColorImageWidth, fColorImageHeight);

			GC gc = new GC(image);
			{

				// Control treeControl = fTourViewer.getControl();
				// gc.setBackground(treeControl.getBackground());
				// gc.setForeground(treeControl.getBackground());
				// gc.drawRectangle(0, 0, fColorImageWidth - 1,
				// fColorImageHeight - 1);

				// gc.setForeground(treeControl.getForeground());

				int arcSize = 4;
				int offsetWidth = 0;
				int offsetHeight = 1;

				DrawingColors drawingColors = getTourTypeDrawingColors(display, typeId);

				Color colorBright = drawingColors.colorBright;

				if (colorBright != null) {

					gc.setForeground(colorBright);
					gc.setBackground(drawingColors.colorDark);

					gc.fillGradientRectangle(
							offsetWidth + 0,
							offsetHeight + 0,
							fColorImageWidth-1,
							fColorImageHeight - 2,
							false);

					// gc.setAntialias(SWT.ON);
					// gc.fillRoundRectangle(
					// offsetWidth + 0,
					// offsetHeight + 0,
					// fColorImageWidth,
					// fColorImageHeight - 2,
					// arcSize,
					// arcSize);
					// gc.setAntialias(SWT.OFF);

					gc.setForeground(drawingColors.colorLine);
					gc.drawRoundRectangle(
							offsetWidth,
							offsetHeight,
							fColorImageWidth - 1,
							fColorImageHeight - 3,
							arcSize,
							arcSize);
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

		String colorIdBright = TOUR_TYPE_PREFIX + "bright" + tourTypeId;
		String colorIdDark = TOUR_TYPE_PREFIX + "dark" + tourTypeId;
		String colorIdLine = TOUR_TYPE_PREFIX + "line" + tourTypeId;

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
		fDetailNoStatistic.setText("No statistic");

		/*
		 * pagebook: chart
		 */
		fPageBookDetailChart = new PageBook(fSashDetail, SWT.NONE);
		fPageBookDetailChart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		fPageDetailNoChart = new Label(fPageBookDetailChart, SWT.NONE);
		fPageDetailNoChart.setText("A tour is not selected");

		fTour = new Tour(fPageBookDetailChart, SWT.FLAT);
		fTour.restoreState(fSessionMemento);
		fTour.setFont(parent.getFont());
		fTour.addTourChangedListener(new ITourChangeListener() {
			public void tourChanged(TourChangeEvent event) {
				TourbookPlugin.getDefault().getPreferenceStore().setValue(
						ITourbookPreferences.APP_NEW_DATA_FILTER,
						Math.random());
			}
		});

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

		fTourChart.addTourChartListener(new ITourChartListener() {
			public void selectedTourChart(SelectionTourChart tourChart) {
				firePostSelection(tourChart);
			}
		});

		fTourChartConfig = TourManager.createTourChartConfiguration();
		fTourChartConfig.setMinMaxKeeper(true);

		return fSashDetail;
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

	private void createActions() {

		fActionDeleteSelectedTour = new ActionDeleteTour(this);
		fActionSetLastTourType = new ActionSetTourType(this, false);
		fActionSetTourType = new ActionSetTourType(this, true);

		fActionShowViewDetailsBoth = new ActionShowViewDetails(this);
		fActionShowViewDetailsViewer = new ActionShowViewDetailsViewer(this);
		fActionShowViewDetailsDetail = new ActionShowViewDetailsDetail(this);

		fActionShowDetailStatistic = new ActionShowStatistic(this);
		fActionShowDetailTourChart = new ActionShowTourChart(this);

		IToolBarManager viewTbm = getViewSite().getActionBars().getToolBarManager();

		viewTbm.add(fActionShowViewDetailsViewer);
		viewTbm.add(fActionShowViewDetailsBoth);
		viewTbm.add(fActionShowViewDetailsDetail);
		viewTbm.add(new Separator());
		viewTbm.add(fActionShowDetailStatistic);
		viewTbm.add(fActionShowDetailTourChart);
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

	private void addPartListener() {

		// set the part listener
		fPartListener = new IPartListener2() {
			public void partClosed(IWorkbenchPartReference partRef) {
				if (ID.equals(partRef.getId()))
					saveSettings();
			}
			public void partDeactivated(IWorkbenchPartReference partRef) {
				if (ID.equals(partRef.getId())) {
					// saveSettings();
				}
			}
			public void partActivated(IWorkbenchPartReference partRef) {}
			public void partBroughtToTop(IWorkbenchPartReference partRef) {}
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
				 * set a new chart configuration when the preferences has
				 * changed
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
					disposeImages();
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

	public void setFocus() {
		fTourViewer.getControl().setFocus();
	}

	/**
	 * create the views context menu
	 */
	private void createContextMenu() {

		MenuManager menuMgr = new MenuManager("#PopupMenu");
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

	private void fillContextMenu(IMenuManager menuMgr) {

		TourType selectedTourType;
		if ((selectedTourType = fActionSetTourType.getSelectedTourType()) != null) {

			fActionSetLastTourType.setSelectedTourType(selectedTourType);
			fActionSetLastTourType.setText("Set tour type to: " + selectedTourType.getName());
			fActionSetLastTourType.setEnabled(true);
			menuMgr.add(fActionSetLastTourType);
		} else {
			fActionSetLastTourType.setEnabled(false);
		}

		menuMgr.add(fActionSetTourType);

		menuMgr.add(new Separator());
		menuMgr.add(fActionDeleteSelectedTour);

		enableActions();
	}
	private void enableActions() {

		ITreeSelection selection = (ITreeSelection) fTourViewer.getSelection();

		int tourItemCounter = 0;

		// count how many tour items are selected
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			if (iter.next() instanceof TVITourBookTour) {
				tourItemCounter++;
			}
		}

		// enable the delete button when only tours are selected
		if (tourItemCounter > 0 && selection.size() == tourItemCounter) {
			fActionDeleteSelectedTour.setEnabled(true);
		} else {
			fActionDeleteSelectedTour.setEnabled(false);
		}

		fActionSetTourType.setEnabled(tourItemCounter > 0);
		fActionSetLastTourType.setEnabled(tourItemCounter > 0);
	}

	private Control createTourViewer(Composite parent) {

		// tour tree
		final Tree tree = new Tree(parent, SWT.H_SCROLL
				| SWT.V_SCROLL
				| SWT.BORDER
				| SWT.FLAT
				| SWT.FULL_SELECTION
				| SWT.MULTI);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		tree.setLayoutData(gridData);

		tree.setHeaderVisible(true);
		tree.setLinesVisible(false);

		fViewerContainer = tree;

		// tree columns
		TreeColumn tc;
		PixelConverter pixelConverter = new PixelConverter(tree);

		tc = new TreeColumn(tree, SWT.NONE);
		tc.setText("Date");
		tc.setWidth(pixelConverter.convertWidthInCharsToPixels(16));

		tc = new TreeColumn(tree, SWT.TRAIL);
		tc.setText("km");
		tc.setToolTipText("Distance");
		tc.setWidth(pixelConverter.convertWidthInCharsToPixels(10));

		tc = new TreeColumn(tree, SWT.TRAIL);
		tc.setText("h");
		tc.setToolTipText("Recording time");
		tc.setWidth(pixelConverter.convertWidthInCharsToPixels(10));

		tc = new TreeColumn(tree, SWT.TRAIL);
		tc.setText("h");
		tc.setToolTipText("Driving time");
		tc.setWidth(pixelConverter.convertWidthInCharsToPixels(10));

		tc = new TreeColumn(tree, SWT.TRAIL);
		tc.setText("hm");
		tc.setToolTipText("Altitude up");
		tc.setWidth(pixelConverter.convertWidthInCharsToPixels(10));

		tc = new TreeColumn(tree, SWT.TRAIL);
		tc.setToolTipText("Number of tours / device start distance");
		tc.setWidth(pixelConverter.convertWidthInCharsToPixels(10));

		fTourViewer = new TreeViewer(tree);
		fTourViewer.setContentProvider(new TourBookContentProvider());
		fTourViewer.setLabelProvider(new TourBookLabelProvider());
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
//					TourManager.getInstance().openTourInEditor(tourItem.getTourId());

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

		return fViewerContainer;
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
					changedChartDataModel.setTitle("Tour: "
							+ TourManager.getTourDate(fTourChartTourData));
				}
			});

			fTourChart.updateChart(fTourChartTourData, fTourChartConfig, false);
			fTour.refreshTourData(fTourChartTourData);

			firePostSelection(new SelectionTourChart(fTourChart));
		}
	}

	private void saveSettings() {
		fSessionMemento = XMLMemento.createWriteRoot("DeviceImportView"); //$NON-NLS-1$
		saveState(fSessionMemento);
	}

	public void saveState(IMemento memento) {

		memento.putInteger(MEMENTO_VIEWER_WIDTH, fTourViewer.getTree().getSize().x);

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
		fStatistics.saveState(memento);
		fTour.saveState(memento);
	}

	private void restoreState(IMemento memento) {

		if (memento != null) {

			fViewerDetailForm.setViewerWidth(fSessionMemento.getInteger(MEMENTO_VIEWER_WIDTH));

			// restore sash weights
			UI.restoreSashWeight(fSashDetail, memento, MEMENTO_SASH_WEIGHT_DETAIL, new int[] {
					50,
					50 });

			// viewer/detail visibility
			Integer containerVisibleStatus = memento.getInteger(MEMENTO_VISIBLE_STATUS_CONTAINER);

			if (containerVisibleStatus == null || containerVisibleStatus == 0) {
				fActionShowViewDetailsBoth.setChecked(true);
				manageVisibility(fActionShowViewDetailsBoth);
			} else if (containerVisibleStatus == 1) {
				fActionShowViewDetailsViewer.setChecked(true);
				manageVisibility(fActionShowViewDetailsViewer);

			} else {
				fActionShowViewDetailsDetail.setChecked(true);
				manageVisibility(fActionShowViewDetailsDetail);
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

		} else {

			fActionShowViewDetailsBoth.setChecked(true);
			manageVisibility(fActionShowViewDetailsBoth);

			fActionShowDetailStatistic.setChecked(true);
			fActionShowDetailTourChart.setChecked(false);
		}

		manageDetailVisibility();
		fStatistics.restoreStatistics(memento);
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
	public void manageVisibility(IAction action) {

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

				fViewerDetailForm.setMaximizedControl(fViewerContainer);
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

	void firePostSelection(ISelection selection) {

		if (selection instanceof SelectionRemovedTours) {
			refreshStatistics();
		}

		fPostSelectionProvider.setSelection(selection);
	}

	void refreshStatistics() {
		fStatistics.refreshStatistic(fActivePerson, fActiveTourTypeId);
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

	public void setActiveYear(int activeYear) {
		fTourViewerSelectedYear = activeYear;
	}

	public void openTourChart(long tourId) {
//		TourManager.getInstance().openTourInEditor(tourId);
	}

	TreeViewer getTourViewer() {
		return fTourViewer;
	}

	void refreshTour(TourData tourData) {
		fTour.refreshTourData(tourData);
	}

	TourChart getTourChart() {
		return fTourChart;
	}

}
