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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import net.tourbook.Messages;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartIsEmptyException;
import net.tourbook.chart.IChartListener;
import net.tourbook.chart.ISliderMoveListener;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.data.TourCompared;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.tour.IDataModelListener;
import net.tourbook.tour.SelectionTourChart;
import net.tourbook.tour.TourChart;
import net.tourbook.tour.TourChartConfiguration;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TreeViewerItem;
import net.tourbook.ui.UI;
import net.tourbook.ui.ViewerDetailForm;
import net.tourbook.util.PixelConverter;
import net.tourbook.util.PostSelectionProvider;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
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
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.part.PageBook;

public class CompareResultView extends SynchedChartView {

	public static final String				ID						= "net.tourbook.views.tourMap.CompareResultView";		//$NON-NLS-1$

	public static final int					COLUMN_REF_TOUR			= 0;
	public static final int					COLUMN_DIFFERENCE		= 1;
	public static final int					COLUMN_SPEED			= 2;
	public static final int					COLUMN_DISTANCE			= 3;
	public static final int					COLUMN_TIME_INTERVAL	= 4;

	private static final String				MEMENTO_SASH_CONTAINER	= "resultview.container.";								//$NON-NLS-1$
	private static final String				MEMENTO_SASH_CHART		= "resultview.chart.";									//$NON-NLS-1$

	/**
	 * This memento allows this view to save and restore state when it is closed
	 * and opened within a session. A different memento is supplied by the
	 * platform for persistance at workbench shutdown.
	 */
	private static IMemento					fSessionMemento			= null;

	private ViewerDetailForm				fViewerDetailForm;
	CheckboxTreeViewer						fTourViewer;

	private SashForm						fSashChart;
	private TourChart						fRefTourChart;
	private TourChart						fCompTourChart;

	private TourReference					fCurrentRefTour;

	private ISelectionListener				selectionListener;
	private IPartListener2					fPartListener;

	private Action							actionSaveComparedTours;
	private Action							actionSynchChartsHorizontal;
	private Action							actionRemoveComparedTourSaveStatus;

	PostSelectionProvider					fPostSelectionProvider;

	private HashMap<Long, RefTourChartData>	fRefChartDataCache		= new HashMap<Long, RefTourChartData>();

	/**
	 * resource manager for images
	 */
	private final LocalResourceManager		resManager				= new LocalResourceManager(
																			JFaceResources
																					.getResources());

	private ImageDescriptor					dbImgDescriptor			= TourbookPlugin
																			.getImageDescriptor(Messages.Image_database);

	private PageBook						fLowerPageBook;

	private RefTourInfo						fRefTourInfo;

	private int								fRefTourXMarkerValue;

	private TVICompareResult				fCurrentTTICompareResult;

	private final NumberFormat				nf						= NumberFormat
																			.getNumberInstance();

	public CompareResultView() {}

	private class ActionSaveComparedTours extends Action {

		ActionSaveComparedTours() {
			setImageDescriptor(TourbookPlugin
					.getImageDescriptor(Messages.Image_save_raw_data_to_file));
			setDisabledImageDescriptor(TourbookPlugin
					.getImageDescriptor(Messages.Image_save_raw_data_to_file_disabled));

			setText(Messages.CompareResult_Action_save_checked_tours);
			setToolTipText(Messages.CompareResult_Action_save_checked_tours_tooltip);
			setEnabled(false);
		}

		public void run() {
			saveComparedTours();
		}
	}

	private class ResultContentProvider implements ITreeContentProvider {

		private TVICompareResultRoot	rootItem	= new TVICompareResultRoot();

		public void dispose() {}

		public Object[] getChildren(Object parentElement) {
			return ((TreeViewerItem) parentElement).getFetchedChildren();
		}

		public Object[] getElements(Object inputElement) {
			return rootItem.getFetchedChildren();
		}

		public Object getParent(Object element) {
			return ((TreeViewerItem) element).getParentItem();
		}

		public TreeViewerItem getRootItem() {
			return rootItem;
		}
		public boolean hasChildren(Object element) {
			return ((TreeViewerItem) element).hasChildren();
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
	}

	private class ViewLabelProvider extends LabelProvider implements ITableLabelProvider,
			IColorProvider {

		public Color getBackground(Object element) {
			return null;
		}

		public Image getColumnImage(Object element, int columnIndex) {

			if (element instanceof TVICompareResult) {
				TVICompareResult compTour = (TVICompareResult) element;
				if (columnIndex == COLUMN_REF_TOUR) {
					if (compTour.compId != -1) {
						return resManager.createImageWithDefault(dbImgDescriptor);
					}
				}
			}

			return null;
		}

		public String getColumnText(Object obj, int index) {

			if (obj instanceof TVICompareResultReference) {

				TVICompareResultReference refTour = (TVICompareResultReference) obj;
				if (index == 0) {
					return refTour.label;
				} else {
					return ""; //$NON-NLS-1$
				}

			} else if (obj instanceof TVICompareResult) {

				final TVICompareResult result = (TVICompareResult) obj;

				switch (index) {
				case COLUMN_REF_TOUR:
					return TourManager.getTourDate(result.compTour);

				case COLUMN_SPEED:
					nf.setMinimumFractionDigits(1);
					nf.setMaximumFractionDigits(1);
					return nf.format(((float) result.compareDistance) / result.compareTime * 3.6);

				case COLUMN_DISTANCE:
					nf.setMinimumFractionDigits(2);
					nf.setMaximumFractionDigits(2);
					return nf.format(((float) result.compareDistance) / 1000);

				case COLUMN_DIFFERENCE:
					return Integer.toString(result.altitudeDiff
							* 100
							/ (result.normIndexEnd - result.normIndexStart));

				case COLUMN_TIME_INTERVAL:
					return Integer.toString(result.timeIntervall);

				}
			}

			return ""; //$NON-NLS-1$
		}

		public Color getForeground(Object element) {
			if (element instanceof TVICompareResult) {
				if (((TVICompareResult) (element)).compId != -1) {
					return Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
				}
			}
			return null;
		}
	}

	private void addListeners() {

		/*
		 * set the part listener to save the view settings, the listeners are
		 * called before the controls are disposed
		 */
		fPartListener = new IPartListener2() {
			public void partActivated(IWorkbenchPartReference partRef) {}
			public void partBroughtToTop(IWorkbenchPartReference partRef) {}
			public void partClosed(IWorkbenchPartReference partRef) {
				if (ID.equals(partRef.getId()))
					saveSettings();
			}
			public void partDeactivated(IWorkbenchPartReference partRef) {
				if (ID.equals(partRef.getId()))
					saveSettings();
			}
			public void partHidden(IWorkbenchPartReference partRef) {}
			public void partInputChanged(IWorkbenchPartReference partRef) {}
			public void partOpened(IWorkbenchPartReference partRef) {}
			public void partVisible(IWorkbenchPartReference partRef) {}
		};
		getViewSite().getPage().addPartListener(fPartListener);

		// this view part is a selection listener
		selectionListener = new ISelectionListener() {

			ArrayList<TVICompareResult>	fTTIRemovedComparedTours;

			public void selectionChanged(IWorkbenchPart part, ISelection selection) {

				SelectionRemovedComparedTours oldSelection = null;

				if (selection instanceof SelectionRemovedComparedTours) {

					SelectionRemovedComparedTours tourSelection = (SelectionRemovedComparedTours) selection;
					ArrayList<Long> removedComparedTourCompIds = tourSelection.removedComparedTours;

					/*
					 * return when there are no removed tours or when the
					 * selection has not changed
					 */
					if (removedComparedTourCompIds.size() == 0 || tourSelection == oldSelection) {
						return;
					}

					oldSelection = tourSelection;

					/*
					 * find/update the removed compared tours in the viewer
					 */

					fTTIRemovedComparedTours = new ArrayList<TVICompareResult>();

					findTTICompareResult(((ResultContentProvider) fTourViewer.getContentProvider())
							.getRootItem(), removedComparedTourCompIds);

					// set status for removed compared tours
					for (TVICompareResult removedCompTour : fTTIRemovedComparedTours) {
						removedCompTour.compId = -1;
					}

					// update the viewer
					fTourViewer.update(fTTIRemovedComparedTours.toArray(), null);
				}
			}

			/**
			 * Recursive method to walk down the tour tree items and find the
			 * compared tours which have been removed
			 * 
			 * @param parentItem
			 * @param removedComparedTourCompIds
			 */
			private void findTTICompareResult(	TreeViewerItem parentItem,
												ArrayList<Long> removedComparedTourCompIds) {

				ArrayList<TreeViewerItem> unfetchedChildren = parentItem.getUnfetchedChildren();

				if (unfetchedChildren != null) {

					// children are available

					for (TreeViewerItem tourTreeItem : unfetchedChildren) {

						if (tourTreeItem instanceof TVICompareResult) {
							TVICompareResult ttiCompResult = (TVICompareResult) tourTreeItem;
							long compId = ttiCompResult.compId;
							for (Long removedCompId : removedComparedTourCompIds) {
								if (compId == removedCompId) {
									fTTIRemovedComparedTours.add(ttiCompResult);
								}
							}
						} else {
							// this is a child which can be the parent for other
							// childs
							findTTICompareResult(tourTreeItem, removedComparedTourCompIds);
						}
					}
				}
			}

		};

		// register selection listener in the page
		getSite().getPage().addPostSelectionListener(selectionListener);

	}
	private void createActions() {

		actionSaveComparedTours = new ActionSaveComparedTours();
		actionRemoveComparedTourSaveStatus = new ActionRemoveComparedTourSaveStatus(this);
		actionSynchChartsHorizontal = new ActionSynchChartHorizontal(this);

		// extend the toolbar int eht compared tour
		IToolBarManager tbm = fCompTourChart.getToolbarManager();
		tbm.add(actionSynchChartsHorizontal);
	}

	/**
	 * create the views context menu
	 */
	private void createContextMenu() {

		MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				CompareResultView.this.fillContextMenu(manager);
			}
		});

		// add the context menu to the table viewer
		Control tourViewer = fTourViewer.getControl();
		Menu menu = menuMgr.createContextMenu(tourViewer);
		tourViewer.setMenu(menu);
	}

	public void createPartControl(final Composite parent) {

		final Control tree = createTourViewer(parent);
		final Sash sash = new Sash(parent, SWT.VERTICAL);
		final Control charts = createCharts(parent);

		fViewerDetailForm = new ViewerDetailForm(parent, tree, sash, charts);

		// -----------------------------------------------------------

		restoreSettings(fSessionMemento);

		addListeners();

		createActions();
		createContextMenu();

		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		fTourViewer.setInput(((ResultContentProvider) fTourViewer.getContentProvider())
				.getRootItem());
	}

	private Control createCharts(Composite parent) {

		fSashChart = new SashForm(parent, SWT.VERTICAL);

		/*
		 * reference tour chart
		 */
		fRefTourChart = new TourChart(fSashChart, SWT.NONE, true);
		fRefTourChart.setShowZoomActions(true);
		fRefTourChart.setShowSlider(true);
		fRefTourChart.setBackgroundColor(parent.getDisplay().getSystemColor(
				SWT.COLOR_WIDGET_BACKGROUND));

		fRefTourChart.addSliderMoveListener(new ISliderMoveListener() {
			public void sliderMoved(SelectionChartInfo chartInfoSelection) {
				fPostSelectionProvider.setSelection(chartInfoSelection);
			}
		});

		// lower part
		fLowerPageBook = new PageBook(fSashChart, SWT.NONE);
		fLowerPageBook.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));

		/*
		 * compared tour chart
		 */
		fCompTourChart = new TourChart(fLowerPageBook, SWT.NONE, true);
		fCompTourChart.setShowZoomActions(true);
		fCompTourChart.setShowSlider(true);

		fCompTourChart.addSliderMoveListener(new ISliderMoveListener() {
			public void sliderMoved(SelectionChartInfo chartInfoSelection) {
				fPostSelectionProvider.setSelection(chartInfoSelection);
			}
		});

		fCompTourChart.addXMarkerDraggingListener(new IChartListener() {

			public void xMarkerMoved(int movedXMarkerStartValueIndex, int movedXMarkerEndValueIndex) {
				xMarkerMovedInCompTourChart(movedXMarkerStartValueIndex, movedXMarkerEndValueIndex);
			}

			public int getXMarkerValueDiff() {
				return fRefTourXMarkerValue;
			}
		});

		// ref tour info
		fRefTourInfo = new RefTourInfo(fLowerPageBook, SWT.NONE);

		return fSashChart;
	}

	private Control createTourViewer(Composite parent) {

		final Tree tree = new Tree(parent, SWT.H_SCROLL
				| SWT.V_SCROLL
				| SWT.BORDER
				| SWT.MULTI
				| SWT.FULL_SELECTION
				| SWT.CHECK);

		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);

		// tree columns
		TreeColumn tc;
		PixelConverter pixelConverter = new PixelConverter(tree);

		// column: reference tour/date
		tc = new TreeColumn(tree, SWT.NONE);
		tc.setText(Messages.CompareResult_Column_tour);
		tc.setWidth(pixelConverter.convertWidthInCharsToPixels(25) + 16);

		// column: altitude difference
		tc = new TreeColumn(tree, SWT.TRAIL);
		tc.setText(Messages.CompareResult_Column_diff);
		tc.setToolTipText(Messages.CompareResult_Column_diff_tooltip);
		tc.setWidth(pixelConverter.convertWidthInCharsToPixels(8));

		// column: speed
		tc = new TreeColumn(tree, SWT.TRAIL);
		tc.setText(Messages.CompareResult_Column_kmh);
		tc.setToolTipText(Messages.CompareResult_Column_kmh_tooltip);
		tc.setWidth(pixelConverter.convertWidthInCharsToPixels(9));

		// column: distance
		tc = new TreeColumn(tree, SWT.TRAIL);
		tc.setText(Messages.CompareResult_Column_km);
		tc.setToolTipText(Messages.CompareResult_Column_km_tooltip);
		tc.setWidth(pixelConverter.convertWidthInCharsToPixels(8));

		tc = new TreeColumn(tree, SWT.TRAIL);
		tc.setText(Messages.RawData_Column_time_interval);
		tc.setToolTipText(Messages.RawData_Column_time_interval_tooltip);
		tc.setWidth(pixelConverter.convertWidthInCharsToPixels(8));

		fTourViewer = new ContainerCheckedTreeViewer(tree);
		fTourViewer.setContentProvider(new ResultContentProvider());
		fTourViewer.setLabelProvider(new ViewLabelProvider());
		fTourViewer.setUseHashlookup(true);

		fTourViewer.setSorter(new ViewerSorter() {
			public int compare(Viewer viewer, Object obj1, Object obj2) {

				if (obj1 instanceof TVICompareResult) {
					return ((TVICompareResult) obj1).altitudeDiff
							- ((TVICompareResult) obj2).altitudeDiff;
				}

				return 0;
			}
		});

		fTourViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				showSelectedTour(event);
			}
		});

		fTourViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				openComparedTour(event);
			}
		});

		fTourViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {

				if (event.getElement() instanceof TVICompareResult) {
					TVICompareResult compareResult = (TVICompareResult) event.getElement();
					if (event.getChecked() && compareResult.compId != -1) {
						/*
						 * uncheck elements which are already stored for the
						 * reftour, it would be better to disable them, but this
						 * is not possible because this is a limitation by the
						 * OS
						 */
						fTourViewer.setChecked(compareResult, false);
					} else {
						enableActions();
					}
				} else {
					// uncheck all other tree items
					fTourViewer.setChecked(event.getElement(), false);
				}
			}
		});

		fTourViewer.addTreeListener(new ITreeViewerListener() {

			public void treeCollapsed(TreeExpansionEvent event) {
			// fTourViewer.getTree().layout(true,true);
			}

			public void treeExpanded(TreeExpansionEvent event) {}
		});

		return tree;
	}

	public void dispose() {

		getSite().getPage().removePostSelectionListener(selectionListener);
		getSite().getPage().removePartListener(fPartListener);

		resManager.dispose();

		super.dispose();
	}

	private void enableActions() {

		// enable/disable save button
		actionSaveComparedTours.setEnabled(fTourViewer.getCheckedElements().length > 0);

		// enable/disable action: remove save status
		final StructuredSelection selection = (StructuredSelection) fTourViewer.getSelection();

		/*
		 * currently we only support one tour item were the save status can be
		 * removed
		 */
		if (selection.size() == 1 && selection.getFirstElement() instanceof TVICompareResult) {

			TVICompareResult ttiCompResult = (TVICompareResult) (selection.getFirstElement());

			actionRemoveComparedTourSaveStatus.setEnabled(ttiCompResult.compId != -1);
		} else {
			actionRemoveComparedTourSaveStatus.setEnabled(false);
		}
	}

	private void fillContextMenu(IMenuManager menuMgr) {

		menuMgr.add(actionSaveComparedTours);
		final StructuredSelection selection = (StructuredSelection) fTourViewer.getSelection();

		selection.getFirstElement();
		if (!selection.isEmpty()) {
			menuMgr.add(new Action(Messages.CompareResult_Action_check_selected_tours) {
				public void run() {
					// check all selected compared tours which are not yet
					// stored
					Object[] selectedTours = selection.toArray();
					for (Object tour : selectedTours) {
						if (tour instanceof TVICompareResult) {
							TVICompareResult comparedTour = (TVICompareResult) tour;
							if (comparedTour.compId == -1) {
								fTourViewer.setChecked(tour, true);
							}
						}
					}
					enableActions();
				}
			});

			menuMgr.add(new Action(Messages.CompareResult_Action_uncheck_selected_tours) {
				public void run() {
					// uncheck all selected tours
					Object[] selectedTours = selection.toArray();
					for (Object tour : selectedTours) {
						fTourViewer.setChecked(tour, false);
					}
					enableActions();
				}
			});
		}

		menuMgr.add(new Separator());
		menuMgr.add(actionRemoveComparedTourSaveStatus);

		enableActions();
	}

	/**
	 * @return Returns the fCompareTourViewer.
	 */
	public CheckboxTreeViewer getRefTourViewer() {
		return fTourViewer;
	}

	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);

		// set the session memento if it's net yet set
		if (fSessionMemento == null) {
			fSessionMemento = memento;
		}
	}

	private void openComparedTour(DoubleClickEvent event) {
		IStructuredSelection selection = (IStructuredSelection) event.getSelection();

		TVICompareResult result = (TVICompareResult) selection.getFirstElement();

		if (result != null) {
			// TourManager.getInstance().openTourInEditor(result.compTour.getTourId());
		}
	}

	private void restoreSettings(IMemento memento) {

		if (memento != null) {

			fViewerDetailForm.setViewerWidth(fSessionMemento.getInteger(MEMENTO_SASH_CONTAINER));

			UI.restoreSashWeight(fSashChart, memento, MEMENTO_SASH_CHART, new int[] { 20, 40 });
		}
	}

	/**
	 * Persist the checked tours
	 */
	private void saveComparedTours() {

		Object[] checkedTours = fTourViewer.getCheckedElements();

		EntityManager em = TourDatabase.getInstance().getEntityManager();

		SelectionPersistedCompareResults persistedCompareResults = new SelectionPersistedCompareResults();

		if (em != null) {

			EntityTransaction ts = em.getTransaction();

			try {

				for (Object checkedTour : checkedTours) {

					if (checkedTour instanceof TVICompareResult) {

						ts.begin();

						TVICompareResult compareResult = (TVICompareResult) checkedTour;
						TourData tourData = compareResult.compTour;

						TourCompared comparedTour = new TourCompared();

						comparedTour.setStartIndex(compareResult.compareIndexStart);
						comparedTour.setEndIndex(compareResult.compareIndexEnd);
						comparedTour.setTourId(tourData.getTourId());
						comparedTour.setRefTourId(compareResult.refTour.getGeneratedId());

						Calendar calendar = GregorianCalendar.getInstance();
						calendar.set(
								tourData.getStartYear(),
								tourData.getStartMonth() - 1,
								tourData.getStartDay());

						comparedTour.setTourDate(calendar.getTimeInMillis());
						comparedTour.setStartYear(tourData.getStartYear());

						comparedTour.setTourSpeed(
								compareResult.compareDistance,
								compareResult.compareTime);

						em.persist(comparedTour);

						ts.commit();

						/*
						 * uncheck the compared tour and make the persisted
						 * instance visible
						 */
						compareResult.compId = comparedTour.getComparedId();
						fTourViewer.setChecked(compareResult, false);

						persistedCompareResults.persistedCompareResults.add(compareResult);

						// update the chart
						fCompTourChart.setBackgroundColor(Display.getCurrent().getSystemColor(
								SWT.COLOR_WIDGET_BACKGROUND));
						fCompTourChart.redrawChart();

					}
				}

				// uncheck/disable the persisted tours
				fTourViewer.update(checkedTours, null);

				// fire selection: update the tourmap view
				fPostSelectionProvider.setSelection(persistedCompareResults);

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (ts.isActive()) {
					ts.rollback();
				}
				em.close();
			}
		}
	}
	private void saveSettings() {
		fSessionMemento = XMLMemento.createWriteRoot("CompareResultView"); //$NON-NLS-1$
		saveState(fSessionMemento);
	}

	public void saveState(IMemento memento) {

		memento.putInteger(MEMENTO_SASH_CONTAINER, fTourViewer.getTree().getSize().x);

		UI.saveSashWeight(fSashChart, memento, MEMENTO_SASH_CHART);
	}

	public void setFocus() {
		fTourViewer.getControl().setFocus();
	}

	private void showRefTour(final TourReference refTour) throws ChartIsEmptyException {

		// show the reference tour if it is not yet displayed
		if (refTour == fCurrentRefTour) {
			return;
		}

		final RefTourChartData refTourChartData = getRefChartData(refTour);

		fRefTourChart.addDataModelListener(new IDataModelListener() {

			public void dataModelChanged(ChartDataModel changedChartDataModel) {

				ChartDataXSerie xData = changedChartDataModel.getXData();

				// set marker
				xData.setMarkerValueIndex(refTour.getStartValueIndex(), refTour.getEndValueIndex());

				// set the x-marker value difference
				int[] xValues = xData.getHighValues()[0];
				fRefTourXMarkerValue = xValues[refTour.getEndValueIndex()]
						- xValues[refTour.getStartValueIndex()];

				// System.out.println(fRefTourXMarkerValue);

				// set title

				changedChartDataModel.setTitle(

				NLS.bind(
						Messages.CompareResult_Chart_title_reference_tour,
						refTour.getLabel(),
						TourManager.getTourDate(refTourChartData.getRefTourData())));
			}
		});

		fRefTourChart.updateChart(refTourChartData.getRefTourData(), refTourChartData
				.getRefTourChartConfig());

		// keep data for current ref tour in the cache
		fRefChartDataCache.put(refTour.getGeneratedId(), refTourChartData);
		fCurrentRefTour = refTour;
	}

	private RefTourChartData getRefChartData(TourReference refTour) {

		// get the reference chart from the cache
		RefTourChartData refTourChartData = fRefChartDataCache.get(refTour.getGeneratedId());

		if (refTourChartData != null) {
			return refTourChartData;
		}

		// get tour data from the database
		TourData tourData = refTour.getTourData();

		// set visible graphs: altitude
		TourChartConfiguration refTourChartConfig = new TourChartConfiguration();
		refTourChartConfig.addVisibleGraph(TourManager.GRAPH_ALTITUDE);
		// refTourChartConfig.setKeepMinMaxValues(true);

		TourChartConfiguration compTourChartConfig = new TourChartConfiguration();
		compTourChartConfig.addVisibleGraph(TourManager.GRAPH_ALTITUDE);
		// compTourChartConfig.setKeepMinMaxValues(true);

		ChartDataModel chartDataModel = TourManager.getInstance().createChartDataModel(
				tourData,
				refTourChartConfig);

		return new RefTourChartData(
				refTour,
				chartDataModel,
				tourData,
				refTourChartConfig,
				compTourChartConfig);
	}

	private void showSelectedTour(SelectionChangedEvent event) {

		try {

			IStructuredSelection selection = (IStructuredSelection) event.getSelection();

			Object treeItem = selection.getFirstElement();

			if (treeItem instanceof TVICompareResult) {

				final TVICompareResult result = (TVICompareResult) treeItem;

				/*
				 * show the reference tour chart, this must be done before the
				 * compared tour chart is displayed, because it set's the
				 * current ref tour
				 */
				showRefTour(result.refTour);

				// show the compared tour chart
				showCompTour(result);

				fPostSelectionProvider.setSelection(new SelectionTourChart(fCompTourChart));

			} else if (treeItem instanceof TVICompareResultReference) {

				TVICompareResultReference refTour = (TVICompareResultReference) treeItem;

				// show the reference tour chart
				showRefTour(refTour.refTour);

				showRefTourInfo(refTour.refTour.getGeneratedId());

				fPostSelectionProvider.setSelection(new SelectionTourChart(fRefTourChart));
			}

		} catch (ChartIsEmptyException e) {
			e.printStackTrace();
		}
	}

	private void showRefTourInfo(long refId) {

		RefTourChartData refTourChartData = fRefChartDataCache.get(refId);

		fRefTourInfo.updateInfo(refTourChartData);

		fLowerPageBook.showPage(fRefTourInfo);
	}

	private void showCompTour(final TVICompareResult compareResult) {

		IDataModelListener dataModelListener = new IDataModelListener() {

			public void dataModelChanged(ChartDataModel changedChartDataModel) {

				ChartDataXSerie xData = changedChartDataModel.getXData();

				// set the x-marker
				xData.setMarkerValueIndex(
						compareResult.compareIndexStart,
						compareResult.compareIndexEnd);

				// set title
				changedChartDataModel.setTitle(NLS.bind(
						Messages.CompareResult_Chart_title_compared_tour,
						TourManager.getTourDate(compareResult.compTour)));
			}
		};

		// set current selected compared tour
		fCurrentTTICompareResult = compareResult;

		// show the compared tour chart
		fCompTourChart.addDataModelListener(dataModelListener);

		// get the tour chart configuration
		RefTourChartData refTourChartData = fRefChartDataCache
				.get(fCurrentRefTour.getGeneratedId());
		TourChartConfiguration compTourChartConfig = refTourChartData.getCompTourChartConfig();

		fCompTourChart.setBackgroundColor(Display.getCurrent().getSystemColor(
				compareResult.compId == -1 ? SWT.COLOR_WHITE : SWT.COLOR_WIDGET_BACKGROUND));

		fCompTourChart.updateChart(compareResult.compTour, compTourChartConfig);

		fLowerPageBook.showPage(fCompTourChart);
	}
	/**
	 * Update the viewer by providing new data
	 */
	public void updateViewer() {
		fTourViewer.setContentProvider(new ResultContentProvider());
	}

	void synchCharts(boolean isSynched) {

		fRefTourChart.setZoomMarkerPositionListener(isSynched, fCompTourChart);

		// show the compared chart in full size
		fCompTourChart.zoomOut(false);
		;
		fCompTourChart.updateChart(false);

	}

	void removeComparedTour() {

		// enable/disable action: remove save status
		final StructuredSelection selection = (StructuredSelection) fTourViewer.getSelection();

		/*
		 * currently we only support one tour item were the save status can be
		 * removed
		 */
		if (selection.size() == 1 && selection.getFirstElement() instanceof TVICompareResult) {

			SelectionRemovedComparedTours removedCompareTours = new SelectionRemovedComparedTours();

			TVICompareResult compareResult = (TVICompareResult) selection.getFirstElement();

			if (TourCompareManager.removeComparedTourFromDb(compareResult.compId)) {

				// update the chart
				fCompTourChart.setBackgroundColor(Display.getCurrent().getSystemColor(
						SWT.COLOR_WHITE));
				fCompTourChart.redrawChart();

				// create and fire the selection
				removedCompareTours.removedComparedTours.add(compareResult.compId);
				fPostSelectionProvider.setSelection(removedCompareTours);

			}
		}
	}

	private void xMarkerMovedInCompTourChart(	int movedXMarkerStartValueIndex,
												int movedXMarkerEndValueIndex) {

		/*
		 * update the moved x-marker in the tour viewer data fDataModel
		 */
		fCurrentTTICompareResult.compareIndexStart = movedXMarkerStartValueIndex;
		fCurrentTTICompareResult.compareIndexEnd = movedXMarkerEndValueIndex;

		// update the changed tour distance/time
		ChartDataModel chartDataModel = fCompTourChart.getChartDataModel();

		int[] distanceValues = ((ChartDataXSerie) chartDataModel
				.getCustomData(TourManager.CUSTOM_DATA_DISTANCE)).getHighValues()[0];
		int[] timeValues = ((ChartDataXSerie) chartDataModel
				.getCustomData(TourManager.CUSTOM_DATA_TIME)).getHighValues()[0];

		fCurrentTTICompareResult.compareDistance = (distanceValues[movedXMarkerEndValueIndex] - distanceValues[movedXMarkerStartValueIndex]);

		int time = (timeValues[movedXMarkerEndValueIndex] - timeValues[movedXMarkerStartValueIndex]);

		// adjust the time by removing the breaks
		int timeInterval = timeValues[1] - timeValues[0];
		int ignoreTimeSlices = TourManager.getInstance().getIgnoreTimeSlices(
				timeValues,
				movedXMarkerStartValueIndex,
				movedXMarkerEndValueIndex,
				10 / timeInterval);
		fCurrentTTICompareResult.compareTime = time - (ignoreTimeSlices * timeInterval);

		// update the chart
		ChartDataXSerie xData = chartDataModel.getXData();
		xData.setMarkerValueIndex(movedXMarkerStartValueIndex, movedXMarkerEndValueIndex);
		fCompTourChart.setChartDataModel(chartDataModel);

		// update the tour viewer
		fTourViewer.update(fCurrentTTICompareResult, null);
	}

}
