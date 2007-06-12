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
package net.tourbook.ui.views.tourMap;

import java.sql.Date;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import net.tourbook.Messages;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.IBarSelectionListener;
import net.tourbook.chart.IChartListener;
import net.tourbook.chart.ISliderMoveListener;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.colors.GraphColors;
import net.tourbook.data.TourCompared;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.tour.ActionAdjustAltitude;
import net.tourbook.tour.IDataModelListener;
import net.tourbook.tour.ITourChartListener;
import net.tourbook.tour.SelectionTourChart;
import net.tourbook.tour.TourChart;
import net.tourbook.tour.TourChartConfiguration;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TreeViewerItem;
import net.tourbook.ui.UI;
import net.tourbook.ui.ViewerDetailForm;
import net.tourbook.ui.views.SelectionTourSegmentLayer;
import net.tourbook.util.PixelConverter;
import net.tourbook.util.PostSelectionProvider;
import net.tourbook.util.TreeColumnLayout;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Color;
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

public class TourMapView extends SynchedChartView {

	public static final String						ID						= "net.tourbook.views.tourMap.TourMapView"; //$NON-NLS-1$

	private static final String						MEMENTO_SASH_CONTAINER	= "tourmapview.sash.container.";			//$NON-NLS-1$
	private static final String						MEMENTO_SASH_CHART		= "tourmapview.sash.chart.";				//$NON-NLS-1$

	public static final int							COLUMN_LABEL			= 0;
	public static final int							COLUMN_SPEED			= 1;

	/**
	 * This memento allows this view to save and restore state when it is closed and opened within a
	 * session. A different memento is supplied by the platform for persistance at workbench
	 * shutdown.
	 */
	private static IMemento							fSessionMemento			= null;

	private ViewerDetailForm						fViewerDetailForm;

	TVITourMapRoot									fRootItem				= new TVITourMapRoot();

	private TourChart								fRefTourChart;
	private Chart									fYearChart;
	private TourChart								fCompTourChart;

	private RefTourInfo								fRefTourInfo;

	private final HashMap<Long, RefTourChartData>	fRefChartDataCache		= new HashMap<Long, RefTourChartData>();

	private TreeViewer								fTourViewer;

	private SashForm								fSashCharts;

	final NumberFormat								nf						= NumberFormat
																					.getNumberInstance();
	/**
	 * active tourId for the reference tour
	 */
	private long									fActiveRefTourId		= -1;
	private RefTourChartData						fActiveRefTourChartData;

	/**
	 * active tourId for the compared tour
	 */
	// private long fActiveCompTourId = -1;
	private TVTITourMapComparedTour					fActiveComparedTour;

	private long									fYearChartRefId			= -1;
	private int										fYearChartYear			= -1;

	private ISelectionListener						fPostSelectionListener;
	private IPartListener2							fPartListener;
	// PostSelectionProvider fPostSelectionProvider;

	private PageBook								fPageBookYearChart;
	private PageBook								fPageBookCompTourChart;

	private Label									fNoSelectedTour;

	protected int									fRefTourXMarkerValue;

	private ArrayList<TVTITourMapComparedTour>		fComparedToursFindResult;
	private ActionDeleteTourFromMap					fActionDeleteSelectedTour;

	private ActionSynchChartHorizontal				fActionSynchCharts;
	private ActionRenameRefTour						fActionRenameRefTour;

	private ActionAdjustAltitude					fActionAdjustAltitude;

	private final RGB								fRGBYearFg				= new RGB(255, 255, 255);
	private final RGB								fRGBMonthFg				= new RGB(128, 64, 0);
	private final RGB								fRGBTourFg				= new RGB(0, 0, 128);

	private final RGB								fRGBYearBg				= new RGB(111, 130, 197);
	private final RGB								fRGBMonthBg				= new RGB(220, 220, 255);
	private final RGB								fRGBTourBg				= new RGB(240, 240, 255);

	private Color									fColorYearFg;
	private Color									fColorMonthFg;
	private Color									fColorTourFg;

	private Color									fColorYearBg;
	private Color									fColorMonthBg;
	private Color									fColorTourBg;

	/**
	 * contains TVTITourMapComparedTour objects, these are the tours for the selected year
	 */
	private Object[]								fYearMapTours;

	PostSelectionProvider							fPostSelectionProvider;

	/**
	 * tour chart which has currently the focus
	 */
	private TourChart								fActiveTourChart;

	private class ActionRenameRefTour extends Action {

		public ActionRenameRefTour() {
			super(Messages.TourMap_Action_rename_reference_tour);
		}

		public void run() {

			final Object selectedItem = (((ITreeSelection) fTourViewer.getSelection())
					.getFirstElement());

			if (selectedItem instanceof TVTITourMapReferenceTour) {

				final TVTITourMapReferenceTour ttiRefTour = (TVTITourMapReferenceTour) selectedItem;

				// ask for the reference tour name
				final InputDialog dialog = new InputDialog(
						getSite().getShell(),
						Messages.TourMap_Dlg_rename_reference_tour_title,
						Messages.TourMap_Dlg_rename_reference_tour_msg,
						ttiRefTour.label,
						null);

				if (dialog.open() != Window.OK) {
					return;
				}

				// get the ref tour from the database
				final EntityManager em = TourDatabase.getInstance().getEntityManager();
				final TourReference refTour = em.find(TourReference.class, ttiRefTour.refId);

				if (refTour != null) {

					final EntityTransaction ts = em.getTransaction();

					// persist the changed ref tour
					try {
						// change the label
						refTour.setLabel(dialog.getValue());

						ts.begin();
						em.merge(refTour);
						ts.commit();
					} catch (final Exception e) {
						e.printStackTrace();
					} finally {
						if (ts.isActive()) {
							ts.rollback();
						} else {

							// refresh the tree viewer and resort the ref tours
							fRootItem.fetchChildren();
							fTourViewer.refresh();
						}
						em.close();
					}
				}
			}
		}
	}

	private class TourContentProvider implements ITreeContentProvider {

		public void dispose() {}

		public Object[] getChildren(final Object parentElement) {
			return ((TreeViewerItem) parentElement).getFetchedChildren();
		}

		public Object[] getElements(final Object inputElement) {
			return fRootItem.getFetchedChildren();
		}

		public Object getParent(final Object element) {
			return ((TreeViewerItem) element).getParentItem();
		}

		public TreeViewerItem getRootItem() {
			return fRootItem;
		}
		public boolean hasChildren(final Object element) {
			return ((TreeViewerItem) element).hasChildren();
		}

		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	private class TourLabelProvider extends LabelProvider implements ITableLabelProvider,
			ITableColorProvider {

		public Color getBackground(final Object element, final int columnIndex) {
			if (/* columnIndex != 0 && */element instanceof TVTITourMapReferenceTour) {
				return fColorYearBg;
			}
			if (/* columnIndex != 0 && */element instanceof TVITourMapYear) {
				return fColorMonthBg;
			}

			return null;
		}

		public Image getColumnImage(final Object element, final int columnIndex) {
			return null;
		}

		public String getColumnText(final Object obj, final int index) {

			if (obj instanceof TVTITourMapReferenceTour) {

				final TVTITourMapReferenceTour refTour = (TVTITourMapReferenceTour) obj;
				switch (index) {
				case COLUMN_LABEL:
					return refTour.label;
				}
				return ""; //$NON-NLS-1$

			} else if (obj instanceof TVITourMapYear) {

				final TVITourMapYear yearItem = (TVITourMapYear) obj;
				switch (index) {
				case COLUMN_LABEL:
					return Integer.toString(yearItem.year);
				}
				return ""; //$NON-NLS-1$

			} else if (obj instanceof TVTITourMapComparedTour) {

				final TVTITourMapComparedTour compTour = (TVTITourMapComparedTour) obj;
				switch (index) {
				case COLUMN_LABEL:
					return DateFormat.getDateInstance(DateFormat.SHORT).format(
							compTour.getTourDate());

				case COLUMN_SPEED:
					nf.setMinimumFractionDigits(1);
					nf.setMaximumFractionDigits(1);

					final float speed = compTour.getTourSpeed();
					if (speed == 0) {
						return ""; //$NON-NLS-1$
					} else {
						return nf.format(speed);
					}
				}

			}
			return (getText(obj));
		}

		public Color getForeground(final Object element, final int columnIndex) {
			if (/* columnIndex != 0 && */element instanceof TVTITourMapReferenceTour) {
				return fColorYearFg;
			}
			// if (element instanceof TVITourBookTour) {
			// return fColorTourFg;
			// }
			// if (columnIndex != 0 && element instanceof TVITourBookMonth) {
			// return fColorMonthFg;
			// }
			return null;
		}
	}

	public TourMapView() {}

	private void createActions() {

		fActionDeleteSelectedTour = new ActionDeleteTourFromMap(this);
		fActionRenameRefTour = new ActionRenameRefTour();
		fActionAdjustAltitude = new ActionAdjustAltitude(fTourViewer, fCompTourChart);

		fActionSynchCharts = new ActionSynchChartHorizontal(this);

		final IToolBarManager tbm = fCompTourChart.getToolbarManager();
		tbm.add(fActionSynchCharts);
	}

	private void createContainer(final Composite parent) {

		/*
		 * viewer layout
		 */
		final Control tree = createTourViewer(parent);
		final Sash sash = new Sash(parent, SWT.VERTICAL);

		fSashCharts = new SashForm(parent, SWT.VERTICAL);
		fSashCharts.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		fSashCharts.setOrientation(SWT.VERTICAL);

		fViewerDetailForm = new ViewerDetailForm(parent, tree, sash, fSashCharts);

		/*
		 * sash content
		 */
		// ref tour chart
		fRefTourChart = new TourChart(fSashCharts, SWT.NONE, true);
		fRefTourChart.setShowZoomActions(true);
		fRefTourChart.setShowSlider(true);
		fRefTourChart.addTourChartListener(new ITourChartListener() {
			public void selectedTourChart(SelectionTourChart tourChart) {
				fActiveTourChart = fRefTourChart;
				fPostSelectionProvider.setSelection(tourChart);
			}
		});

		// fire a slider move selection when a slider was moved in the tour
		// chart
		fRefTourChart.addSliderMoveListener(new ISliderMoveListener() {
			public void sliderMoved(SelectionChartInfo chartInfoSelection) {
				fPostSelectionProvider.setSelection(chartInfoSelection);
			}
		});

		/*
		 * year map pagebook
		 */
		fPageBookYearChart = new PageBook(fSashCharts, SWT.NONE);
		fPageBookYearChart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// year chart
		fYearChart = new Chart(fPageBookYearChart, SWT.NONE);
		fYearChart.addSelectionChangedListener(new IBarSelectionListener() {
			public void selectionChanged(final int serieIndex, final int valueIndex) {

				TVTITourMapComparedTour tourMapComparedTour = (TVTITourMapComparedTour) fYearMapTours[valueIndex];

				// show the compared tour
				updateCompTourChart(tourMapComparedTour);
				fPageBookCompTourChart.showPage(fCompTourChart);

				// select the tour in the tour viewer
				fTourViewer.setSelection(new StructuredSelection(tourMapComparedTour), true);
			}
		});

		fRefTourInfo = new RefTourInfo(fPageBookYearChart, SWT.NONE);

		/*
		 * comp tour pagebook
		 */
		fPageBookCompTourChart = new PageBook(fSashCharts, SWT.NONE);
		fPageBookCompTourChart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// compared tour chart
		fCompTourChart = new TourChart(fPageBookCompTourChart, SWT.NONE, true);
		fCompTourChart.setShowZoomActions(true);
		fCompTourChart.setShowSlider(true);

		fCompTourChart.addXMarkerDraggingListener(new IChartListener() {

			public int getXMarkerValueDiff() {
				return fRefTourXMarkerValue;
			}

			public void xMarkerMoved(	final int movedXMarkerStartValueIndex,
										final int movedXMarkerEndValueIndex) {
				xMarkerMovedInCompTourChart(movedXMarkerStartValueIndex, movedXMarkerEndValueIndex);
			}
		});

		fCompTourChart.addTourChartListener(new ITourChartListener() {
			public void selectedTourChart(SelectionTourChart tourChart) {
				fActiveTourChart = fCompTourChart;
				fPostSelectionProvider.setSelection(tourChart);
			}
		});

		// fire a slider move selection when a slider was moved in the tour
		// chart
		fCompTourChart.addSliderMoveListener(new ISliderMoveListener() {
			public void sliderMoved(SelectionChartInfo chartInfoSelection) {
				fPostSelectionProvider.setSelection(chartInfoSelection);
			}
		});

		fNoSelectedTour = new Label(fPageBookCompTourChart, SWT.NONE);
		fNoSelectedTour.setText(Messages.TourMap_Label_a_tour_is_not_selected);
	}

	/**
	 * create the views context menu
	 */
	private void createContextMenu() {

		final MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager manager) {
				TourMapView.this.fillContextMenu(manager);
			}
		});

		// add the context menu to the table viewer
		final Control tourViewer = fTourViewer.getControl();
		final Menu menu = menuMgr.createContextMenu(tourViewer);
		tourViewer.setMenu(menu);
	}

	public void createPartControl(final Composite parent) {

		final Display display = parent.getDisplay();

		fColorYearFg = new Color(display, fRGBYearFg);
		fColorYearBg = new Color(display, fRGBYearBg);
		fColorMonthFg = new Color(display, fRGBMonthFg);
		fColorMonthBg = new Color(display, fRGBMonthBg);
		fColorTourFg = new Color(display, fRGBTourFg);
		fColorTourBg = new Color(display, fRGBTourBg);

		createContainer(parent);
		createActions();
		createContextMenu();

		restoreSettings(fSessionMemento);

		setPartListener();
		setPostSelectionListener();

		// set selection provider
		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		fTourViewer
				.setInput(((TourContentProvider) fTourViewer.getContentProvider()).getRootItem());
	}

	private Control createTourViewer(final Composite parent) {

		// viewer container
		final Composite treeContainer = new Composite(parent, SWT.NONE);
		final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		treeContainer.setLayoutData(gridData);

		final TreeColumnLayout treeLayouter = new TreeColumnLayout();
		treeContainer.setLayout(treeLayouter);

		// tour tree
		final Tree tree = new Tree(treeContainer, SWT.H_SCROLL
				| SWT.V_SCROLL
				| SWT.BORDER
				| SWT.MULTI
				| SWT.FULL_SELECTION);

		tree.setHeaderVisible(true);
		// tree.setLinesVisible(true);

		// tree columns
		TreeColumn tc;
		PixelConverter pixelConverter = new PixelConverter(tree);

		tc = new TreeColumn(tree, SWT.NONE);
		tc.setText(Messages.TourMap_Column_tour);
		treeLayouter.addColumnData(new ColumnWeightData(18, true));

		tc = new TreeColumn(tree, SWT.TRAIL);
		tc.setText(Messages.TourMap_Column_kmh);
		treeLayouter.addColumnData(new ColumnPixelData(pixelConverter
				.convertWidthInCharsToPixels(9), false));

		// tour viewer
		fTourViewer = new TreeViewer(tree);
		fTourViewer.setContentProvider(new TourContentProvider());
		fTourViewer.setLabelProvider(new TourLabelProvider());
		fTourViewer.setUseHashlookup(true);

		fTourViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				showTourMapItem((IStructuredSelection) event.getSelection());
				enableActions();
			}
		});

		fTourViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {

				final IStructuredSelection selection = (IStructuredSelection) event.getSelection();

				final Object tourItem = selection.getFirstElement();

				/*
				 * get tour id
				 */
				long tourId = -1;
				if (tourItem instanceof TVTITourMapComparedTour) {
					tourId = ((TVTITourMapComparedTour) tourItem).getTourId();
				}

				if (tourId != -1) {
					// TourManager.getInstance().openTourInEditor(tourId);
				} else {
					// expand/collapse current item
					if (fTourViewer.getExpandedState(tourItem)) {
						fTourViewer.collapseToLevel(tourItem, 1);
					} else {
						fTourViewer.expandToLevel(tourItem, 1);
					}
				}
			}
		});

		return treeContainer;
	}

	public void dispose() {

		getSite().getPage().removePostSelectionListener(fPostSelectionListener);
		getViewSite().getPage().removePartListener(fPartListener);

		fColorYearFg.dispose();
		fColorMonthFg.dispose();
		fColorTourFg.dispose();
		fColorYearBg.dispose();
		fColorMonthBg.dispose();
		fColorTourBg.dispose();

		super.dispose();
	}

	private void enableActions() {

		final ITreeSelection selection = (ITreeSelection) fTourViewer.getSelection();

		int refItemCounter = 0;
		int tourItemCounter = 0;
		int yearItemCounter = 0;

		// count how many different items are selected
		for (final Iterator iter = selection.iterator(); iter.hasNext();) {
			final Object item = (Object) iter.next();

			if (item instanceof TVTITourMapReferenceTour) {
				refItemCounter++;
			} else if (item instanceof TVTITourMapComparedTour) {
				tourItemCounter++;
			} else if (item instanceof TVITourMapYear) {
				yearItemCounter++;
			}
		}

		// enable: delete button when only one type is selected
		if (yearItemCounter == 0
				&& ((refItemCounter > 0 && tourItemCounter == 0) || (refItemCounter == 0 & tourItemCounter > 0))) {
			fActionDeleteSelectedTour.setEnabled(true);
		} else {
			fActionDeleteSelectedTour.setEnabled(false);
		}

		// enable: rename ref tour
		fActionRenameRefTour.setEnabled(refItemCounter == 1
				&& tourItemCounter == 0
				&& yearItemCounter == 0);

		fActionAdjustAltitude.setEnabled(tourItemCounter > 0);
	}

	private void fillContextMenu(final IMenuManager menuMgr) {

		menuMgr.add(fActionAdjustAltitude);
		menuMgr.add(fActionRenameRefTour);
		menuMgr.add(new Separator());
		menuMgr.add(fActionDeleteSelectedTour);

		enableActions();
	}

	/**
	 * Recursive !!! method to walk down the tour tree items and find the compared tours
	 * 
	 * @param parentItem
	 * @param findCompIds
	 *        comp id's which should be found
	 */
	private void findComparedTours(	final TreeViewerItem parentItem,
									final ArrayList<Long> findCompIds) {

		final ArrayList<TreeViewerItem> unfetchedChildren = parentItem.getUnfetchedChildren();

		if (unfetchedChildren != null) {

			// children are available

			for (final TreeViewerItem tourTreeItem : unfetchedChildren) {

				if (tourTreeItem instanceof TVTITourMapComparedTour) {

					final TVTITourMapComparedTour ttiCompResult = (TVTITourMapComparedTour) tourTreeItem;
					final long ttiCompId = ttiCompResult.getCompId();

					for (final Long compId : findCompIds) {
						if (ttiCompId == compId) {
							fComparedToursFindResult.add(ttiCompResult);
						}
					}

				} else {
					// this is a child which can be the parent for other
					// childs
					findComparedTours(tourTreeItem, findCompIds);
				}
			}
		}
	}

	/**
	 * fRefChartData is set from the cache or from a new instance
	 * 
	 * @param refId
	 */
	private RefTourChartData getRefChartData(final long refId) {

		// get the reference chart from the cache
		final RefTourChartData refTourChartData = fRefChartDataCache.get(refId);

		if (refTourChartData != null) {
			return refTourChartData;
		}

		// create a new reference tour chart

		// load the reference tour from the database
		final EntityManager em = TourDatabase.getInstance().getEntityManager();
		final TourReference refTour = em.find(TourReference.class, refId);
		em.close();

		if (refTour == null) {
			fRefTourChart.setChartDataModel(null);
			return null;
		} else {

			// get tour data from the database
			final TourData refTourData = refTour.getTourData();

			// set visible graphs: altitude
			final TourChartConfiguration refTourChartConfig = TourManager
					.createTourChartConfiguration();
			// refTourChartConfig.setKeepMinMaxValues(true);

			final TourChartConfiguration compTourchartConfig = TourManager
					.createTourChartConfiguration();
			// compTourchartConfig.setKeepMinMaxValues(true);

			final ChartDataModel chartDataModel = TourManager.getInstance().createChartDataModel(
					refTourData,
					refTourChartConfig);

			return new RefTourChartData(
					refTour,
					chartDataModel,
					refTourData,
					refTourChartConfig,
					compTourchartConfig);
		}
	}

	public TreeViewer getTourViewer() {
		return fTourViewer;
	}

	public void init(final IViewSite site, final IMemento memento) throws PartInitException {
		super.init(site, memento);

		// set the session memento if it's net yet set
		if (fSessionMemento == null) {
			fSessionMemento = memento;
		}
	}

	private void restoreSettings(final IMemento memento) {

		if (memento != null) {

			fViewerDetailForm.setViewerWidth(fSessionMemento.getInteger(MEMENTO_SASH_CONTAINER));

			// restore sash weights
			UI.restoreSashWeight(fSashCharts, memento, MEMENTO_SASH_CHART, new int[] { 20, 40 });

			// restore tab index
			// final Integer upperIndex = memento.getInteger(MEMENTO_UPPER_TAB);
			// fUpperTabs.setSelection(upperIndex == null ? 0 : upperIndex);
		}
	}
	private void saveSettings() {
		fSessionMemento = XMLMemento.createWriteRoot("TourMapView"); //$NON-NLS-1$
		saveState(fSessionMemento);
	}

	public void saveState(final IMemento memento) {

		memento.putInteger(MEMENTO_SASH_CONTAINER, fTourViewer.getTree().getSize().x);

		// save sash weights
		UI.saveSashWeight(fSashCharts, memento, MEMENTO_SASH_CHART);

		// save selected tabs
		// memento.putInteger(MEMENTO_UPPER_TAB,
		// fUpperTabs.getSelectionIndex());
	}

	/**
	 * select the tour in the year map chart
	 * 
	 * @param selectedTourId
	 *        tour id which should be selected
	 */
	private void selectYearMapTour(long selectedTourId) {

		final int tourLength = fYearMapTours.length;
		boolean[] selectedTours = new boolean[tourLength];

		for (int tourIndex = 0; tourIndex < tourLength; tourIndex++) {
			final TVTITourMapComparedTour comparedItem = (TVTITourMapComparedTour) fYearMapTours[tourIndex];
			if (comparedItem.getTourId() == selectedTourId) {
				selectedTours[tourIndex] = true;
			}
		}

		fYearChart.setSelectedBars(selectedTours);
	}

	public void setFocus() {
		fTourViewer.getTree().setFocus();
	}

	private void setPartListener() {
		fPartListener = new IPartListener2() {
			public void partActivated(final IWorkbenchPartReference partRef) {}
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}
			public void partClosed(final IWorkbenchPartReference partRef) {
				if (ID.equals(partRef.getId()))
					saveSettings();
			}
			public void partDeactivated(final IWorkbenchPartReference partRef) {
				if (ID.equals(partRef.getId()))
					saveSettings();
			}
			public void partHidden(final IWorkbenchPartReference partRef) {}
			public void partInputChanged(final IWorkbenchPartReference partRef) {}
			public void partOpened(final IWorkbenchPartReference partRef) {}
			public void partVisible(final IWorkbenchPartReference partRef) {}
		};
		getViewSite().getPage().addPartListener(fPartListener);
	}

	/**
	 * show the reference tour if it's not yet displayed
	 * 
	 * @param refId
	 * @return Returns <code>true</code> then the ref tour changed
	 */
	private boolean setRefTourChartData(final long refId) {

		if (fActiveRefTourId == refId) {
			// the ref tour is already displayed
			return false;
		}

		// replace the old ref tour with a new one

		// save the chart slider positions for the old ref chart
		final RefTourChartData oldRefChartData = fRefChartDataCache.get(fActiveRefTourId);
		if (oldRefChartData != null) {

			final SelectionChartXSliderPosition oldXSliderPosition = fRefTourChart
					.getXSliderPosition();

			oldRefChartData.setXSliderPosition(new SelectionChartXSliderPosition(
					fRefTourChart,
					oldXSliderPosition.slider1ValueIndex,
					oldXSliderPosition.slider2ValueIndex));
		}

		// create the ref chart data for the new ref tour
		final RefTourChartData refTourChartData = getRefChartData(refId);

		fRefTourChart.addDataModelListener(new IDataModelListener() {

			public void dataModelChanged(final ChartDataModel changedChartDataModel) {

				final ChartDataXSerie xData = changedChartDataModel.getXData();
				final TourReference refTour = refTourChartData.getRefTour();

				// set the marker positions
				xData.setMarkerValueIndex(refTour.getStartValueIndex(), refTour.getEndValueIndex());

				// set the x-marker value difference
				final int[] xValues = xData.getHighValues()[0];
				fRefTourXMarkerValue = xValues[refTour.getEndValueIndex()]
						- xValues[refTour.getStartValueIndex()];

				// set title
				changedChartDataModel.setTitle(NLS.bind(
						Messages.TourMap_Label_chart_title_reference_tour,
						refTour.getLabel(),
						TourManager.getTourTitleDetailed(refTourChartData.getRefTourData())));

			}
		});

		// keep data for the current ref tour
		fRefChartDataCache.put(refId, refTourChartData);
		fActiveRefTourId = refId;
		fActiveRefTourChartData = refTourChartData;

		return true;
	}

	private void setPostSelectionListener() {

		// this view part is a selection listener
		fPostSelectionListener = new ISelectionListener() {

			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				// update the view when a new tour reference was created
				if (selection instanceof SelectionPersistedCompareResults) {

					final SelectionPersistedCompareResults tourMapUpdate = (SelectionPersistedCompareResults) selection;

					final ArrayList<TVICompareResult> persistedCompareResults = tourMapUpdate.persistedCompareResults;

					if (persistedCompareResults.size() > 0) {
						updateCompareResults(persistedCompareResults);
					}
				}

				if (selection instanceof SelectionNewRefTours) {

					final SelectionNewRefTours tourSelection = (SelectionNewRefTours) selection;
					final ArrayList<TourReference> newRefTours = tourSelection.newRefTours;

					if (newRefTours.size() > 0) {

						// refresh the tree viewer and resort the ref tours
						fRootItem.fetchChildren();
						fTourViewer.refresh();
					}
				}

				if (selection instanceof SelectionRemovedComparedTours) {

					final SelectionRemovedComparedTours removedCompTours = (SelectionRemovedComparedTours) selection;

					/*
					 * find/remove the removed compared tours in the viewer
					 */
					fComparedToursFindResult = new ArrayList<TVTITourMapComparedTour>();

					findComparedTours(((TourContentProvider) fTourViewer.getContentProvider())
							.getRootItem(), removedCompTours.removedComparedTours);

					// remove compared tour from the fDataModel
					for (final TVTITourMapComparedTour comparedTour : fComparedToursFindResult) {
						comparedTour.remove();
					}

					// remove compared tour from the tree viewer
					fTourViewer.remove(fComparedToursFindResult.toArray());
				}

				if (fActiveTourChart != null) {

					/*
					 * listen for x-slider position changes which can be done in the marker view
					 */
					if (selection instanceof SelectionChartXSliderPosition) {
						fActiveTourChart
								.setXSliderPosition((SelectionChartXSliderPosition) selection);
					}

					if (selection instanceof SelectionTourSegmentLayer) {
						fActiveTourChart
								.updateSegmentLayer(((SelectionTourSegmentLayer) selection).isLayerVisible);
					}
				}

			}

		};

		// register selection listener in the page
		getSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

	private void showRefTourChart(final RefTourChartData refTourChartData) {

		fRefTourChart.zoomOut(false);
		fRefTourChart.updateChart(refTourChartData.getRefTourData(), refTourChartData
				.getRefTourChartConfig(), false);
	}

	private void showTourMapItem(final IStructuredSelection selection) {

		// show the reference tour chart
		final Object item = selection.getFirstElement();

		long refId = -1;

		if (item instanceof TVTITourMapReferenceTour) {

			final TVTITourMapReferenceTour refItem = (TVTITourMapReferenceTour) item;

			refId = refItem.refId;

			fRefTourInfo.updateInfo(getRefChartData(refId));

			fPageBookYearChart.showPage(fRefTourInfo);
			fPageBookCompTourChart.showPage(fNoSelectedTour);

			// force the next compare tour to be recomputed
			fActiveComparedTour = null;

		} else if (item instanceof TVITourMapYear) {

			final TVITourMapYear yearItem = (TVITourMapYear) item;

			if (fYearChartRefId != yearItem.refId || fYearChartYear != yearItem.year) {
				updateYearChart(yearItem);
			}

			refId = yearItem.refId;

			fPageBookYearChart.showPage(fYearChart);
			fPageBookCompTourChart.showPage(fNoSelectedTour);

		} else if (item instanceof TVTITourMapComparedTour) {

			final TVTITourMapComparedTour compItem = (TVTITourMapComparedTour) item;

			// show the chart for the compared tour if it is not yet displayed
			if (fActiveComparedTour != compItem) {

				/*
				 * show the ref tour and synch the marked area in the ref chart with the compare
				 * tour chart
				 */
				refId = compItem.getRefId();

				updateCompTourChart(compItem);

				updateYearChart((TVITourMapYear) compItem.getParentItem());

				selectYearMapTour(compItem.getTourId());
			}

			fPageBookCompTourChart.showPage(fCompTourChart);
		}

		if (refId != -1) {
			// show the ref tour chart
			if (setRefTourChartData(refId)) {
				showRefTourChart(fActiveRefTourChartData);
			}
		}
	}

	private void showYearChart(final Chart yearChart, final TVITourMapYear yearItem) {

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		fYearMapTours = yearItem.getFetchedChildren();
		final int tourLength = fYearMapTours.length;

		final int[] tourDateValues = new int[tourLength];
		final int[] tourSpeed = new int[tourLength];
		final Calendar calendar = GregorianCalendar.getInstance();

		for (int tourIndex = 0; tourIndex < tourLength; tourIndex++) {
			final TVTITourMapComparedTour comparedItem = (TVTITourMapComparedTour) fYearMapTours[tourIndex];

			final Date tourDate = comparedItem.getTourDate();
			calendar.setTime(tourDate);

			tourDateValues[tourIndex] = calendar.get(Calendar.DAY_OF_YEAR) - 1;
			tourSpeed[tourIndex] = (int) (comparedItem.getTourSpeed() * 10);
		}

		final ChartDataModel chartModel = new ChartDataModel(ChartDataModel.CHART_TYPE_BAR);

		final ChartDataXSerie xData = new ChartDataXSerie(tourDateValues);
		xData.setAxisUnit(ChartDataXSerie.AXIS_UNIT_YEAR);
		chartModel.setXData(xData);

		// set the bar low/high data
		final ChartDataYSerie yData = new ChartDataYSerie(ChartDataModel.CHART_TYPE_BAR, tourSpeed);

		yData.setValueDivisor(10);
		TourManager.setChartColors(prefStore, yData, GraphColors.PREF_GRAPH_SPEED);

		/*
		 * set/restore min/max values
		 */
		final TVTITourMapReferenceTour refItem = yearItem.getRefItem();
		final int minValue = yData.getMinValue();
		final int maxValue = yData.getMaxValue();

		final int dataMinValue = minValue - (minValue / 10);
		final int dataMaxValue = maxValue;// + (maxValue / 30);

		if (refItem.yearMapMinValue == Integer.MIN_VALUE) {

			// min/max values have not yet been saved

			/*
			 * set the min value 10% below the computed so that the lowest value is not at the
			 * bottom
			 */
			yData.setMinValue(dataMinValue);
			yData.setMaxValue(dataMaxValue);

			refItem.yearMapMinValue = dataMinValue;
			refItem.yearMapMaxValue = dataMaxValue;

		} else {

			/*
			 * restore min/max values, but make sure min/max values for the current graph are
			 * visible and not outside of the chart
			 */

			refItem.yearMapMinValue = Math.min(refItem.yearMapMinValue, dataMinValue);
			refItem.yearMapMaxValue = Math.max(refItem.yearMapMaxValue, dataMaxValue);

			yData.setMinValue(refItem.yearMapMinValue);
			yData.setMaxValue(refItem.yearMapMaxValue);
		}

		yData.setYTitle(Messages.TourMap_Label_year_chart_title);
		yData.setUnitLabel(Messages.TourMap_Label_year_chart_unit);
		// yData.setMinValue(0);

		chartModel.addYData(yData);

		// set title
		chartModel.setTitle(NLS.bind(Messages.TourMap_Label_chart_title_year_map, yearItem.year));

		// set graph minimum width, this is the number of days in the
		// fSelectedYear
		calendar.set(yearItem.year, 11, 31);
		final int yearDays = calendar.get(Calendar.DAY_OF_YEAR);
		chartModel.setChartMinWidth(yearDays);

		// show the data fDataModel in the chart
		yearChart.setChartDataModel(chartModel);
	}

	void synchCharts(final boolean isSynched) {
		fRefTourChart.setZoomMarkerPositionListener(isSynched, fCompTourChart);
	}

	private void updateCompareResults(final ArrayList<TVICompareResult> persistedCompareResults) {
		/*
		 * mapViewRefIds contains all ref id's which children changed
		 */
		final HashMap<Long, Long> mapViewRefIds = new HashMap<Long, Long>();

		// loop: get all ref tours which needs to be updated
		for (final TVICompareResult compareResult : persistedCompareResults) {

			if (compareResult.getParentItem() instanceof TVICompareResultReference) {

				final long compResultRefId = ((TVICompareResultReference) compareResult
						.getParentItem()).refTour.getGeneratedId();

				mapViewRefIds.put(compResultRefId, compResultRefId);
			}
		}

		// remove all compare results that another getSelection call
		// does n
		persistedCompareResults.clear();

		// loop: all ref tours where children has been added
		for (final Iterator refIdIter = mapViewRefIds.values().iterator(); refIdIter.hasNext();) {

			final Long refId = (Long) refIdIter.next();

			final ArrayList<TreeViewerItem> unfetchedChildren = fRootItem.getUnfetchedChildren();
			if (unfetchedChildren != null) {

				for (final TreeViewerItem rootChild : unfetchedChildren) {
					final TVTITourMapReferenceTour mapRefTour = (TVTITourMapReferenceTour) rootChild;

					if (mapRefTour.refId == refId) {
						// reload the children for the reference tour
						mapRefTour.fetchChildren();
						fTourViewer.refresh(mapRefTour, false);

						break;
					}
				}
			}
		}
	}

	private void updateCompTourChart(final TVTITourMapComparedTour compItem) {

		// load the tourdata for the compared tour from the database
		final TourData selectedCompTourData = TourDatabase
				.getTourDataByTourId(compItem.getTourId());

		if (selectedCompTourData == null) {
			return;
		} else {

			// show the compared tour chart

			final IDataModelListener dataModelListener = new IDataModelListener() {

				public void dataModelChanged(ChartDataModel changedChartDataModel) {

					ChartDataXSerie xData = changedChartDataModel.getXData();

					// set marker
					xData.setMarkerValueIndex(compItem.getStartIndex(), compItem.getEndIndex());

					// set title
					changedChartDataModel.setTitle(NLS.bind(
							Messages.TourMap_Label_chart_title_compared_tour,
							TourManager.getTourTitleDetailed(selectedCompTourData)));
				}
			};

			fCompTourChart.addDataModelListener(dataModelListener);

			RefTourChartData refTourChartData = fRefChartDataCache.get(fActiveRefTourId);

			if (refTourChartData != null) {
				fCompTourChart.updateChart(selectedCompTourData, refTourChartData
						.getCompTourChartConfig());
			}

			/*
			 * fire the change event so that the tour markers updated
			 */
			fPostSelectionProvider.setSelection(new SelectionTourChart(fCompTourChart));

			fActiveComparedTour = compItem;

			fActiveTourChart = fCompTourChart;
		}
	}

	private void updateYearChart(final TVITourMapYear yearItem) {

		showYearChart(fYearChart, yearItem);

		fYearChartRefId = yearItem.refId;
		fYearChartYear = yearItem.year;
	}

	private void xMarkerMovedInCompTourChart(	final int movedXMarkerStartValueIndex,
												final int movedXMarkerEndValueIndex) {

		final EntityManager em = TourDatabase.getInstance().getEntityManager();
		final EntityTransaction ts = em.getTransaction();
		float tourSpeed = 0;

		try {
			final TourCompared compTour = em.find(TourCompared.class, fActiveComparedTour
					.getCompId());

			if (compTour != null) {

				// update the changed x-marker index
				compTour.setStartIndex(movedXMarkerStartValueIndex);
				compTour.setEndIndex(movedXMarkerEndValueIndex);

				// update the changed tour speed
				final ChartDataModel chartDataModel = fCompTourChart.getChartDataModel();

				final int[] distanceValues = ((ChartDataXSerie) chartDataModel
						.getCustomData(TourManager.CUSTOM_DATA_DISTANCE)).getHighValues()[0];

				final int[] timeValues = ((ChartDataXSerie) chartDataModel
						.getCustomData(TourManager.CUSTOM_DATA_TIME)).getHighValues()[0];

				final int distance = distanceValues[movedXMarkerEndValueIndex]
						- distanceValues[movedXMarkerStartValueIndex];
				int time = timeValues[movedXMarkerEndValueIndex]
						- timeValues[movedXMarkerStartValueIndex];

				// adjust the time by removing the breaks
				final int timeInterval = timeValues[1] - timeValues[0];
				int ignoreTimeSlices = TourManager.getInstance().getIgnoreTimeSlices(
						timeValues,
						movedXMarkerStartValueIndex,
						movedXMarkerEndValueIndex,
						10 / timeInterval);
				time = time - (ignoreTimeSlices * timeInterval);

				tourSpeed = compTour.setTourSpeed(distance, time);

				// update the entity
				ts.begin();
				em.merge(compTour);
				ts.commit();
			}
		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			if (ts.isActive()) {
				ts.rollback();
			}
			em.close();
		}

		// find the changed compared tour
		fComparedToursFindResult = new ArrayList<TVTITourMapComparedTour>();
		final ArrayList<Long> findCompIds = new ArrayList<Long>();
		findCompIds.add(fActiveComparedTour.getCompId());
		findComparedTours(
				((TourContentProvider) fTourViewer.getContentProvider()).getRootItem(),
				findCompIds);

		// update the data in the data model
		final TVTITourMapComparedTour ttiComparedTour = fComparedToursFindResult.get(0);
		ttiComparedTour.setStartIndex(movedXMarkerStartValueIndex);
		ttiComparedTour.setEndIndex(movedXMarkerEndValueIndex);
		ttiComparedTour.setTourSpeed(tourSpeed);

		// update the chart
		final ChartDataModel chartDataModel = fCompTourChart.getChartDataModel();
		final ChartDataXSerie xData = chartDataModel.getXData();
		xData.setMarkerValueIndex(movedXMarkerStartValueIndex, movedXMarkerEndValueIndex);
		fCompTourChart.setChartDataModel(chartDataModel);

		// update the tour viewer
		fTourViewer.update(fComparedToursFindResult.toArray(), null);

		// force the year chart to be refreshed
		fYearChartYear = -1;

		// reset the min/max size in the year view
		if (ttiComparedTour.getParentItem() instanceof TVITourMapYear) {
			final TVITourMapYear ttiTourMapYear = (TVITourMapYear) ttiComparedTour.getParentItem();
			final TVTITourMapReferenceTour refItem = ttiTourMapYear.getRefItem();
			refItem.yearMapMinValue = Integer.MIN_VALUE;

			updateYearChart(ttiTourMapYear);
		}
	}

}
