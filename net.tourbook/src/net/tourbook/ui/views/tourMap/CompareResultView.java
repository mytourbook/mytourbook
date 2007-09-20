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

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import net.tourbook.Messages;
import net.tourbook.data.TourCompared;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TreeViewerItem;
import net.tourbook.util.PixelConverter;
import net.tourbook.util.PostSelectionProvider;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
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
import org.eclipse.ui.part.ViewPart;

public class CompareResultView extends ViewPart {

	public static final String			ID						= "net.tourbook.views.tourMap.CompareResultView";				//$NON-NLS-1$

	public static final int				COLUMN_REF_TOUR			= 0;
	public static final int				COLUMN_DIFFERENCE		= 1;
	public static final int				COLUMN_SPEED			= 2;
	public static final int				COLUMN_DISTANCE			= 3;
	public static final int				COLUMN_TIME_INTERVAL	= 4;

	/**
	 * This memento allows this view to save and restore state when it is closed and opened within a
	 * session. A different memento is supplied by the platform for persistance at workbench
	 * shutdown.
	 */
	private static IMemento				fSessionMemento			= null;

	private CheckboxTreeViewer			fTourViewer;

	private ISelectionListener			fPostSelectionListener;
	private IPartListener2				fPartListener;

	private Action						fActionSaveComparedTours;
	private Action						fActionRemoveComparedTourSaveStatus;

	PostSelectionProvider				fPostSelectionProvider;

	/**
	 * resource manager for images
	 */
	private final LocalResourceManager	resManager				= new LocalResourceManager(JFaceResources.getResources());

	private ImageDescriptor				dbImgDescriptor			= TourbookPlugin.getImageDescriptor(Messages.Image_database);

	private final NumberFormat			nf						= NumberFormat.getNumberInstance();

	private class ActionSaveComparedTours extends Action {

		ActionSaveComparedTours() {
			setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image_save_raw_data_to_file));
			setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image_save_raw_data_to_file_disabled));

			setText(Messages.CompareResult_Action_save_checked_tours);
			setToolTipText(Messages.CompareResult_Action_save_checked_tours_tooltip);
			setEnabled(false);
		}

		@Override
		public void run() {
			saveComparedTours();
		}
	}

	class ResultContentProvider implements ITreeContentProvider {

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

	public CompareResultView() {}

	private void addListeners() {

		/*
		 * set the part listener to save the view settings, the listeners are called before the
		 * controls are disposed
		 */
		fPartListener = new IPartListener2() {
			public void partActivated(IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(IWorkbenchPartReference partRef) {}

			public void partClosed(IWorkbenchPartReference partRef) {
				if (ID.equals(partRef.getId())) {
					saveSettings();
				}
			}

			public void partDeactivated(IWorkbenchPartReference partRef) {
				if (ID.equals(partRef.getId())) {
					saveSettings();
				}
			}

			public void partHidden(IWorkbenchPartReference partRef) {}

			public void partInputChanged(IWorkbenchPartReference partRef) {}

			public void partOpened(IWorkbenchPartReference partRef) {}

			public void partVisible(IWorkbenchPartReference partRef) {}
		};
		getViewSite().getPage().addPartListener(fPartListener);

		// this view part is a selection listener
		fPostSelectionListener = new ISelectionListener() {

			ArrayList<TVICompareResult>	fTTIRemovedComparedTours;

			/**
			 * Recursive method to walk down the tour tree items and find the compared tours which
			 * have been removed
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

			public void selectionChanged(IWorkbenchPart part, ISelection selection) {

				SelectionRemovedComparedTours oldSelection = null;

				if (selection instanceof SelectionRemovedComparedTours) {

					SelectionRemovedComparedTours tourSelection = (SelectionRemovedComparedTours) selection;
					ArrayList<Long> removedComparedTourCompIds = tourSelection.removedComparedTours;

					/*
					 * return when there are no removed tours or when the selection has not changed
					 */
					if (removedComparedTourCompIds.size() == 0 || tourSelection == oldSelection) {
						return;
					}

					oldSelection = tourSelection;

					/*
					 * find/update the removed compared tours in the viewer
					 */

					fTTIRemovedComparedTours = new ArrayList<TVICompareResult>();

					findTTICompareResult(((ResultContentProvider) fTourViewer.getContentProvider()).getRootItem(),
							removedComparedTourCompIds);

					// set status for removed compared tours
					for (TVICompareResult removedCompTour : fTTIRemovedComparedTours) {
						removedCompTour.compId = -1;
					}

					// update the viewer
					fTourViewer.update(fTTIRemovedComparedTours.toArray(), null);
				}
			}

		};

		// register selection listener in the page
		getSite().getPage().addPostSelectionListener(fPostSelectionListener);

	}

	private void createActions() {

		fActionSaveComparedTours = new ActionSaveComparedTours();
		fActionRemoveComparedTourSaveStatus = new ActionRemoveComparedTourSaveStatus(this);
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

	@Override
	public void createPartControl(final Composite parent) {

		createTourViewer(parent);

		restoreSettings(fSessionMemento);

		addListeners();

		createActions();
		createContextMenu();

		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		fTourViewer.setInput(((ResultContentProvider) fTourViewer.getContentProvider()).getRootItem());
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
		tc.setText(net.tourbook.ui.Messages.ColumnFactory_time_interval);
		tc.setToolTipText(net.tourbook.ui.Messages.ColumnFactory_time_interval_tooltip);
		tc.setWidth(pixelConverter.convertWidthInCharsToPixels(8));

		fTourViewer = new ContainerCheckedTreeViewer(tree);
		fTourViewer.setContentProvider(new ResultContentProvider());
		fTourViewer.setLabelProvider(new ViewLabelProvider());
		fTourViewer.setUseHashlookup(true);

		fTourViewer.setSorter(new ViewerSorter() {
			@Override
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
						 * uncheck elements which are already stored for the reftour, it would be
						 * better to disable them, but this is not possible because this is a
						 * limitation by the OS
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

	@Override
	public void dispose() {

		getSite().getPage().removePostSelectionListener(fPostSelectionListener);
		getSite().getPage().removePartListener(fPartListener);

		resManager.dispose();

		super.dispose();
	}

	private void enableActions() {

		// enable/disable save button
		fActionSaveComparedTours.setEnabled(fTourViewer.getCheckedElements().length > 0);

		// enable/disable action: remove save status
		final StructuredSelection selection = (StructuredSelection) fTourViewer.getSelection();

		/*
		 * currently we only support one tour item were the save status can be removed
		 */
		if (selection.size() == 1 && selection.getFirstElement() instanceof TVICompareResult) {

			TVICompareResult ttiCompResult = (TVICompareResult) (selection.getFirstElement());

			fActionRemoveComparedTourSaveStatus.setEnabled(ttiCompResult.compId != -1);
		} else {
			fActionRemoveComparedTourSaveStatus.setEnabled(false);
		}
	}

	private void fillContextMenu(IMenuManager menuMgr) {

		menuMgr.add(fActionSaveComparedTours);
		final StructuredSelection selection = (StructuredSelection) fTourViewer.getSelection();

		selection.getFirstElement();
		if (!selection.isEmpty()) {
			menuMgr.add(new Action(Messages.CompareResult_Action_check_selected_tours) {
				@Override
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
				@Override
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
		menuMgr.add(fActionRemoveComparedTourSaveStatus);

		enableActions();
	}

	/**
	 * @return Returns the fCompareTourViewer.
	 */
	public CheckboxTreeViewer getRefTourViewer() {
		return fTourViewer;
	}

	@Override
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

		}
	}

	/**
	 * Persist checked tours in the viewer
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
						comparedTour.setRefTourId(compareResult.refTour.getRefId());

						Calendar calendar = GregorianCalendar.getInstance();
						calendar.set(tourData.getStartYear(),
								tourData.getStartMonth() - 1,
								tourData.getStartDay());

						comparedTour.setTourDate(calendar.getTimeInMillis());
						comparedTour.setStartYear(tourData.getStartYear());

						float speed = TourManager.computeTourSpeed(tourData.distanceSerie,
								tourData.timeSerie,
								compareResult.compareIndexStart,
								compareResult.compareIndexEnd);

						comparedTour.setTourSpeed(speed);

						em.persist(comparedTour);

						ts.commit();

						/*
						 * uncheck the compared tour and make the persisted instance visible
						 */
						compareResult.compId = comparedTour.getComparedId();
						fTourViewer.setChecked(compareResult, false);

						persistedCompareResults.persistedCompareResults.add(compareResult);

						// update the chart
//						fCompTourChart.setBackgroundColor(Display.getCurrent()
//								.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
//						fCompTourChart.redrawChart();

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

	@Override
	public void saveState(IMemento memento) {

	}

	@Override
	public void setFocus() {
		fTourViewer.getControl().setFocus();
	}

	private void showSelectedTour(SelectionChangedEvent event) {

		IStructuredSelection selection = (IStructuredSelection) event.getSelection();

		Object treeItem = selection.getFirstElement();

		if (treeItem instanceof TVICompareResultReference) {

			TVICompareResultReference refItem = (TVICompareResultReference) treeItem;

			fPostSelectionProvider.setSelection(new SelectionComparedTour(fTourViewer,
					refItem.refTour.getRefId()));

		} else if (treeItem instanceof TVICompareResult) {

			final TVICompareResult resultItem = (TVICompareResult) treeItem;

			final SelectionComparedTour selectionCompTour = new SelectionComparedTour(fTourViewer,
					resultItem.refTour.getRefId());

			selectionCompTour.setTourCompareData(resultItem.compId,
					resultItem.compTour.getTourId(),
					resultItem.compareIndexStart,
					resultItem.compareIndexEnd);

			fPostSelectionProvider.setSelection(selectionCompTour);
		}
	}

	/**
	 * Update the viewer by providing new data
	 */
	public void updateViewer() {
		fTourViewer.setContentProvider(new ResultContentProvider());
	}

}
