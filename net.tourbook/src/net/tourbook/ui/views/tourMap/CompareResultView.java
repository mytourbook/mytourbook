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

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import net.tourbook.Messages;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.tour.ITourPropertyListener;
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
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
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

	public static final String			ID							= "net.tourbook.views.tourMap.CompareResultView";				//$NON-NLS-1$

	public static final int				COLUMN_REF_TOUR				= 0;
	public static final int				COLUMN_ALTITUDE_DIFFERENCE	= 1;
	public static final int				COLUMN_SPEED_COMPUTED		= 2;
	public static final int				COLUMN_SPEED_SAVED			= 3;															// speed saved in the database
	public static final int				COLUMN_SPEED_MOVED			= 4;															// speed saved in the database
	public static final int				COLUMN_DISTANCE				= 5;
	public static final int				COLUMN_TIME_INTERVAL		= 6;

	/**
	 * This memento allows this view to save and restore state when it is closed and opened within a
	 * session. A different memento is supplied by the platform for persistance at workbench
	 * shutdown.
	 */
	private static IMemento				fSessionMemento				= null;

	private CheckboxTreeViewer			fTourViewer;

	private ISelectionListener			fPostSelectionListener;
	private IPartListener2				fPartListener;

	private Action						fActionSaveComparedTours;
	private Action						fActionRemoveComparedTourSaveStatus;

	PostSelectionProvider				fPostSelectionProvider;

	/**
	 * resource manager for images
	 */
	private final LocalResourceManager	resManager					= new LocalResourceManager(JFaceResources.getResources());

	private ImageDescriptor				dbImgDescriptor				= TourbookPlugin.getImageDescriptor(Messages.Image_database);

	private final NumberFormat			nf							= NumberFormat.getNumberInstance();

	private ITourPropertyListener		fCompareTourPropertyListener;

	private class ActionSaveComparedTours extends Action {

		ActionSaveComparedTours() {
			setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image_save));
			setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image_save_disabled));

			setText(Messages.Compare_Result_Action_save_checked_tours);
			setToolTipText(Messages.Compare_Result_Action_save_checked_tours_tooltip);
			setEnabled(false);
		}

		@Override
		public void run() {
			saveCheckedTours();
		}
	}

	class ResultContentProvider implements ITreeContentProvider {

		private CompareResultItemRoot	rootItem	= new CompareResultItemRoot();

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

			if (element instanceof CompareResultItemComparedTour) {
				CompareResultItemComparedTour compTour = (CompareResultItemComparedTour) element;
				if (columnIndex == COLUMN_REF_TOUR) {
					if (compTour.compId != -1) {
						return resManager.createImageWithDefault(dbImgDescriptor);
					}
				}
			}

			return null;
		}

		public String getColumnText(Object obj, int index) {

			if (obj instanceof CompareResultItemReferenceTour) {

				CompareResultItemReferenceTour refTour = (CompareResultItemReferenceTour) obj;
				if (index == 0) {
					return refTour.label;
				} else {
					return ""; //$NON-NLS-1$
				}

			} else if (obj instanceof CompareResultItemComparedTour) {

				final CompareResultItemComparedTour result = (CompareResultItemComparedTour) obj;

				switch (index) {
				case COLUMN_REF_TOUR:
					return TourManager.getTourDate(result.comparedTourData);

				case COLUMN_SPEED_COMPUTED:
					nf.setMinimumFractionDigits(1);
					nf.setMaximumFractionDigits(1);
					return nf.format(result.compareSpeed);

				case COLUMN_SPEED_SAVED:
					final float speedSaved = result.dbSpeed;
					if (speedSaved == 0) {
						return ""; //$NON-NLS-1$
					} else {
						nf.setMinimumFractionDigits(1);
						nf.setMaximumFractionDigits(1);
						return nf.format(speedSaved);
					}

				case COLUMN_SPEED_MOVED:
					final float speedMoved = result.movedSpeed;
					if (speedMoved == 0) {
						return ""; //$NON-NLS-1$
					} else {
						nf.setMinimumFractionDigits(1);
						nf.setMaximumFractionDigits(1);
						return nf.format(speedMoved);
					}

				case COLUMN_DISTANCE:
					nf.setMinimumFractionDigits(2);
					nf.setMaximumFractionDigits(2);
					return nf.format(((float) result.compareDistance) / 1000);

				case COLUMN_ALTITUDE_DIFFERENCE:
					return Integer.toString(result.minAltitudeDiff
							* 100
							/ (result.normalizedEndIndex - result.normalizedStartIndex));

				case COLUMN_TIME_INTERVAL:
					return Integer.toString(result.timeIntervall);

				}
			}

			return ""; //$NON-NLS-1$
		}

		public Color getForeground(Object element) {
			if (element instanceof CompareResultItemComparedTour) {
				if (((CompareResultItemComparedTour) (element)).compId != -1) {
					return Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
				}
			}
			return null;
		}
	}

	public CompareResultView() {}

	/**
	 * Recursive method to walk down the tour tree items and find the compared tours
	 * 
	 * @param parentItem
	 * @param CompareIds
	 */
	private static void getComparedTours(	ArrayList<CompareResultItemComparedTour> comparedTours,
											TreeViewerItem parentItem,
											ArrayList<Long> CompareIds) {

		ArrayList<TreeViewerItem> unfetchedChildren = parentItem.getUnfetchedChildren();

		if (unfetchedChildren != null) {

			// children are available

			for (TreeViewerItem tourTreeItem : unfetchedChildren) {

				if (tourTreeItem instanceof CompareResultItemComparedTour) {
					CompareResultItemComparedTour ttiCompResult = (CompareResultItemComparedTour) tourTreeItem;
					long compId = ttiCompResult.compId;
					for (Long removedCompId : CompareIds) {
						if (compId == removedCompId) {
							comparedTours.add(ttiCompResult);
						}
					}
				} else {
					// this is a child which can be the parent for other childs
					getComparedTours(comparedTours, tourTreeItem, CompareIds);
				}
			}
		}
	}

	private void addCompareTourPropertyListener() {

		fCompareTourPropertyListener = new ITourPropertyListener() {
			public void propertyChanged(int propertyId, Object propertyData) {

				if (propertyId == TourManager.TOUR_PROPERTY_COMPARE_TOUR_CHANGED
						&& propertyData instanceof TourPropertyCompareTourChanged) {

					TourPropertyCompareTourChanged compareTourProperty = (TourPropertyCompareTourChanged) propertyData;

					final long compareId = compareTourProperty.compareId;

					ArrayList<Long> compareIds = new ArrayList<Long>();
					compareIds.add(compareId);

					if (compareId == -1) {

						// compare result is not saved

						final Object comparedTourItem = compareTourProperty.comparedTourItem;

						if (comparedTourItem instanceof CompareResultItemComparedTour) {
							CompareResultItemComparedTour resultItem = (CompareResultItemComparedTour) comparedTourItem;

							resultItem.movedStartIndex = compareTourProperty.startIndex;
							resultItem.movedEndIndex = compareTourProperty.endIndex;
							resultItem.movedSpeed = compareTourProperty.speed;

							// update viewer
							fTourViewer.update(comparedTourItem, null);
						}

					} else {

						// compare result is saved

						// find compared tour in the viewer
						ArrayList<CompareResultItemComparedTour> comparedTours = new ArrayList<CompareResultItemComparedTour>();
						final TreeViewerItem rootItem = ((ResultContentProvider) fTourViewer.getContentProvider()).getRootItem();
						getComparedTours(comparedTours, rootItem, compareIds);

						if (comparedTours.size() > 0) {

							CompareResultItemComparedTour compareTourItem = comparedTours.get(0);

							if (compareTourProperty.isDataSaved) {

								// compared tour was saved

								compareTourItem.dbStartIndex = compareTourProperty.startIndex;
								compareTourItem.dbEndIndex = compareTourProperty.endIndex;
								compareTourItem.dbSpeed = compareTourProperty.speed;

							} else {

								compareTourItem.movedStartIndex = compareTourProperty.startIndex;
								compareTourItem.movedEndIndex = compareTourProperty.endIndex;
								compareTourItem.movedSpeed = compareTourProperty.speed;
							}

							// update viewer
							fTourViewer.update(compareTourItem, null);
						}
					}
				}
			}
		};

		TourManager.getInstance().addPropertyListener(fCompareTourPropertyListener);
	}

	/**
	 * set the part listener to save the view settings, the listeners are called before the controls
	 * are disposed
	 */
	private void addPartListeners() {

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
	}

	/**
	 * Listen to post selections
	 */
	private void addSelectionListeners() {

		fPostSelectionListener = new ISelectionListener() {
			public void selectionChanged(IWorkbenchPart part, ISelection selection) {

				SelectionRemovedComparedTours oldSelection = null;

				if (selection instanceof SelectionRemovedComparedTours) {

					SelectionRemovedComparedTours tourSelection = (SelectionRemovedComparedTours) selection;
					ArrayList<Long> removedTourCompareIds = tourSelection.removedComparedTours;

					/*
					 * return when there are no removed tours or when the selection has not changed
					 */
					if (removedTourCompareIds.size() == 0 || tourSelection == oldSelection) {
						return;
					}

					oldSelection = tourSelection;

					/*
					 * find/update the removed compared tours in the viewer
					 */

					ArrayList<CompareResultItemComparedTour> comparedTours = new ArrayList<CompareResultItemComparedTour>();

					getComparedTours(comparedTours,
							((ResultContentProvider) fTourViewer.getContentProvider()).getRootItem(),
							removedTourCompareIds);

					// reset entity for the removed compared tours
					for (CompareResultItemComparedTour removedCompTour : comparedTours) {

						removedCompTour.compId = -1;

						removedCompTour.dbStartIndex = -1;
						removedCompTour.dbEndIndex = -1;
						removedCompTour.dbSpeed = 0;

						removedCompTour.movedStartIndex = -1;
						removedCompTour.movedEndIndex = -1;
						removedCompTour.movedSpeed = 0;
					}

					// update viewer
					fTourViewer.update(comparedTours.toArray(), null);

				} else if (selection instanceof SelectionPersistedCompareResults) {

					final SelectionPersistedCompareResults selectionPersisted = (SelectionPersistedCompareResults) selection;

					final ArrayList<CompareResultItemComparedTour> persistedCompareResults = selectionPersisted.persistedCompareResults;

					if (persistedCompareResults.size() > 0) {

						CompareResultItemComparedTour comparedTourItem = persistedCompareResults.get(0);

						// uncheck persisted tours
						fTourViewer.setChecked(comparedTourItem, false);

						// update changed item
						fTourViewer.update(comparedTourItem, null);

					}
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

		addPartListeners();
		addSelectionListeners();
		addCompareTourPropertyListener();

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
		tc.setText(Messages.Compare_Result_Column_tour);
		tc.setWidth(pixelConverter.convertWidthInCharsToPixels(25) + 16);

		// column: altitude difference
		tc = new TreeColumn(tree, SWT.TRAIL);
		tc.setText(Messages.Compare_Result_Column_diff);
		tc.setToolTipText(Messages.Compare_Result_Column_diff_tooltip);
		tc.setWidth(pixelConverter.convertWidthInCharsToPixels(8));

		// column: speed computed
		tc = new TreeColumn(tree, SWT.TRAIL);
		tc.setText(Messages.Compare_Result_Column_kmh);
		tc.setToolTipText(Messages.Compare_Result_Column_kmh_tooltip);
		tc.setWidth(pixelConverter.convertWidthInCharsToPixels(8));

		// column: speed saved
		tc = new TreeColumn(tree, SWT.TRAIL);
		tc.setText(Messages.Compare_Result_Column_kmh);
		tc.setToolTipText(Messages.Compare_Result_Column_kmh_db_tooltip);
		tc.setWidth(pixelConverter.convertWidthInCharsToPixels(8));

		// column: speed moved
		tc = new TreeColumn(tree, SWT.TRAIL);
		tc.setText(Messages.Compare_Result_Column_kmh);
		tc.setToolTipText(Messages.Compare_Result_Column_kmh_moved_tooltip);
		tc.setWidth(pixelConverter.convertWidthInCharsToPixels(8));

		// column: distance
		tc = new TreeColumn(tree, SWT.TRAIL);
		tc.setText(Messages.Compare_Result_Column_km);
		tc.setToolTipText(Messages.Compare_Result_Column_km_tooltip);
		tc.setWidth(pixelConverter.convertWidthInCharsToPixels(8));

		tc = new TreeColumn(tree, SWT.TRAIL);
		tc.setText(Messages.Compare_Result_time_interval);
		tc.setToolTipText(Messages.Compare_Result_time_interval_tooltip);
		tc.setWidth(pixelConverter.convertWidthInCharsToPixels(8));

		fTourViewer = new ContainerCheckedTreeViewer(tree);
		fTourViewer.setContentProvider(new ResultContentProvider());
		fTourViewer.setLabelProvider(new ViewLabelProvider());
		fTourViewer.setUseHashlookup(true);

		fTourViewer.setSorter(new ViewerSorter() {
			@Override
			public int compare(Viewer viewer, Object obj1, Object obj2) {

				if (obj1 instanceof CompareResultItemComparedTour) {
					return ((CompareResultItemComparedTour) obj1).minAltitudeDiff
							- ((CompareResultItemComparedTour) obj2).minAltitudeDiff;
				}

				return 0;
			}
		});

		fTourViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				onSelectionChanged(event);
			}
		});

		fTourViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {

				// expand/collapse current item

				final Object treeItem = ((IStructuredSelection) event.getSelection()).getFirstElement();

				if (fTourViewer.getExpandedState(treeItem)) {
					fTourViewer.collapseToLevel(treeItem, 1);
				} else {
					fTourViewer.expandToLevel(treeItem, 1);
				}
			}
		});

		fTourViewer.getTree().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent keyEvent) {
				if (keyEvent.keyCode == SWT.DEL) {
					removeComparedTour();
				}
			}
		});

		fTourViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {

				if (event.getElement() instanceof CompareResultItemComparedTour) {
					CompareResultItemComparedTour compareResult = (CompareResultItemComparedTour) event.getElement();
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

//		fTourViewer.addTreeListener(new ITreeViewerListener() {
//
//			public void treeCollapsed(TreeExpansionEvent event) {
//			// fTourViewer.getTree().layout(true,true);
//			}
//
//			public void treeExpanded(TreeExpansionEvent event) {}
//		});

		return tree;
	}

	@Override
	public void dispose() {

		getSite().getPage().removePostSelectionListener(fPostSelectionListener);
		getSite().getPage().removePartListener(fPartListener);
		TourManager.getInstance().removePropertyListener(fCompareTourPropertyListener);

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
		if (selection.size() == 1
				&& selection.getFirstElement() instanceof CompareResultItemComparedTour) {

			CompareResultItemComparedTour tviCompResult = (CompareResultItemComparedTour) (selection.getFirstElement());

			fActionRemoveComparedTourSaveStatus.setEnabled(tviCompResult.compId != -1);
		} else {
			fActionRemoveComparedTourSaveStatus.setEnabled(false);
		}
	}

	private void fillContextMenu(IMenuManager menuMgr) {

		menuMgr.add(fActionSaveComparedTours);
		final StructuredSelection selection = (StructuredSelection) fTourViewer.getSelection();

		selection.getFirstElement();
		if (!selection.isEmpty()) {
			menuMgr.add(new Action(Messages.Compare_Result_Action_check_selected_tours) {
				@Override
				public void run() {
					// check all selected compared tours which are not yet
					// stored
					Object[] selectedTours = selection.toArray();
					for (Object tour : selectedTours) {
						if (tour instanceof CompareResultItemComparedTour) {
							CompareResultItemComparedTour comparedTour = (CompareResultItemComparedTour) tour;
							if (comparedTour.compId == -1) {
								fTourViewer.setChecked(tour, true);
							}
						}
					}
					enableActions();
				}
			});

			menuMgr.add(new Action(Messages.Compare_Result_Action_uncheck_selected_tours) {
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

	private void onSelectionChanged(SelectionChangedEvent event) {

		IStructuredSelection selection = (IStructuredSelection) event.getSelection();

		Object treeItem = selection.getFirstElement();

		if (treeItem instanceof CompareResultItemReferenceTour) {

			CompareResultItemReferenceTour refItem = (CompareResultItemReferenceTour) treeItem;

			fPostSelectionProvider.setSelection(new SelectionTourMapView(refItem.refTour.getRefId()));

		} else if (treeItem instanceof CompareResultItemComparedTour) {

			final CompareResultItemComparedTour resultItem = (CompareResultItemComparedTour) treeItem;

			fPostSelectionProvider.setSelection(new StructuredSelection(resultItem));
		}
	}

	/**
	 * Remove compared tour from the database
	 */
	void removeComparedTour() {

		// enable/disable action: remove save status
		final StructuredSelection selection = (StructuredSelection) fTourViewer.getSelection();

		/*
		 * currently only one tour is supported to remove the save status
		 */
		if (selection.size() == 1
				&& selection.getFirstElement() instanceof CompareResultItemComparedTour) {

			CompareResultItemComparedTour compareResult = (CompareResultItemComparedTour) selection.getFirstElement();

			if (TourCompareManager.removeComparedTourFromDb(compareResult.compId)) {

				// update tour map view
				SelectionRemovedComparedTours removedCompareTours = new SelectionRemovedComparedTours();
				removedCompareTours.removedComparedTours.add(compareResult.compId);

				fPostSelectionProvider.setSelection(removedCompareTours);
			}
		}
	}

	private void restoreSettings(IMemento memento) {

		if (memento != null) {

		}
	}

	/**
	 * Persist the compared tours which are checked in the viewer
	 */
	private void saveCheckedTours() {

		Object[] checkedTours = fTourViewer.getCheckedElements();

		EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em != null) {

			EntityTransaction ts = em.getTransaction();

			try {

				SelectionPersistedCompareResults persistedCompareResults = new SelectionPersistedCompareResults();

				for (Object checkedTour : checkedTours) {
					if (checkedTour instanceof CompareResultItemComparedTour) {

						CompareResultItemComparedTour comparedTourItem = (CompareResultItemComparedTour) checkedTour;

						TourCompareManager.saveComparedTourItem(comparedTourItem, em, ts);

						// uncheck the compared tour and make the persisted instance visible
						fTourViewer.setChecked(comparedTourItem, false);

						persistedCompareResults.persistedCompareResults.add(comparedTourItem);
					}
				}

				// uncheck/disable the persisted tours
				fTourViewer.update(checkedTours, null);

				// update tour map view
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

	/**
	 * Update the viewer by providing new data
	 */
	public void updateViewer() {
		fTourViewer.setContentProvider(new ResultContentProvider());
	}

}
