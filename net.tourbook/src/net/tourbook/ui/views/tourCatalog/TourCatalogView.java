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
package net.tourbook.ui.views.tourCatalog;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import net.tourbook.Messages;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourPropertyListener;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TreeViewerItem;
import net.tourbook.ui.ColumnManager;
import net.tourbook.ui.ITourViewer;
import net.tourbook.ui.UI;
import net.tourbook.util.PixelConverter;
import net.tourbook.util.PostSelectionProvider;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
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
import org.eclipse.ui.part.ViewPart;

public class TourCatalogView extends ViewPart implements ITourViewer {

	private static final String			MEMENTO_TOUR_MAP_ACTIVE_REF_ID	= "tour.map.active.ref.id";				//$NON-NLS-1$
	private static final String			MEMENTO_TOUR_MAP_LINK_TOUR		= "tour.map.link.tour";					//$NON-NLS-1$

	public static final String			ID								= "net.tourbook.views.tourMap.TourMapView"; //$NON-NLS-1$

	public static final int				COLUMN_LABEL					= 0;
	public static final int				COLUMN_SPEED					= 1;

	/**
	 * This memento allows this view to save and restore state when it is closed and opened within a
	 * session. A different memento is supplied by the platform for persistance at workbench
	 * shutdown.
	 */
	private static IMemento				fSessionMemento					= null;

	TourCatalogItemRoot						fRootItem						= new TourCatalogItemRoot();

	private Composite					fViewerContainer;
	private TreeViewer					fTourViewer;

	final NumberFormat					nf								= NumberFormat.getNumberInstance();

	private ISelectionListener			fPostSelectionListener;
	private IPartListener2				fPartListener;
	PostSelectionProvider				fPostSelectionProvider;
	private ITourPropertyListener		fCompareTourPropertyListener;
	private IPropertyChangeListener		fPrefChangeListener;

	protected int						fRefTourXMarkerValue;

	private ActionDeleteTourFromMap		fActionDeleteSelectedTour;
	private ActionRenameRefTour			fActionRenameRefTour;
	private ActionLinkTour				fActionLinkTour;
	private ActionCollapseAll			fActionCollapseAll;
	private ActionRefreshView			fActionRefreshView;

	private final RGB					fRGBRefFg						= new RGB(0, 0, 0);
	private final RGB					fRGBRefBg						= new RGB(255, 220, 153);

	private final RGB					fRGBYearFg						= new RGB(255, 255, 255);
	private final RGB					fRGBYearBg						= new RGB(255, 233, 191);

	private final RGB					fRGBTourFg						= new RGB(0, 0, 0);
	private final RGB					fRGBTourBg						= new RGB(255, 255, 255);

	private Color						fColorRefFg;
	private Color						fColorRefBg;

	private Color						fColorYearFg;
	private Color						fColorYearBg;

	private Color						fColorTourFg;
	private Color						fColorTourBg;

	/**
	 * tour item which is selected by the link tour action
	 */
	protected TourCatalogItemComparedTour	fLinkedTour;

	/**
	 * ref id which is currently selected in the tour viewer
	 */
	private long						fActiveRefId;

	/**
	 * flag if actions are added to the toolbar
	 */
	private boolean						fIsToolbarCreated				= false;

	class TourContentProvider implements ITreeContentProvider {

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

	private class TourLabelProvider extends LabelProvider implements ITableLabelProvider, ITableColorProvider {

		public Color getBackground(final Object element, final int columnIndex) {
			if (/* columnIndex == 0 && */element instanceof TourCatalogItemReferenceTour) {
				return fColorRefBg;
			}
			if (/* columnIndex == 0 && */element instanceof TourCatalogItemYear) {
				return fColorYearBg;
			}
			if (columnIndex == 0 && element instanceof TourCatalogItemComparedTour) {
				return fColorTourBg;
			}

			return null;
		}

		public Image getColumnImage(final Object element, final int columnIndex) {
			return null;
		}

		public String getColumnText(final Object obj, final int index) {

			if (obj instanceof TourCatalogItemReferenceTour) {

				final TourCatalogItemReferenceTour refTour = (TourCatalogItemReferenceTour) obj;
				switch (index) {
				case COLUMN_LABEL:
					return refTour.label;
				}
				return ""; //$NON-NLS-1$

			} else if (obj instanceof TourCatalogItemYear) {

				final TourCatalogItemYear yearItem = (TourCatalogItemYear) obj;
				switch (index) {
				case COLUMN_LABEL:
					return Integer.toString(yearItem.year);
				}
				return ""; //$NON-NLS-1$

			} else if (obj instanceof TourCatalogItemComparedTour) {

				final TourCatalogItemComparedTour compTour = (TourCatalogItemComparedTour) obj;
				switch (index) {
				case COLUMN_LABEL:
					return DateFormat.getDateInstance(DateFormat.SHORT).format(compTour.getTourDate());

				case COLUMN_SPEED:

					nf.setMinimumFractionDigits(1);
					nf.setMaximumFractionDigits(1);

					final float speed = compTour.getTourSpeed();
					if (speed == 0) {
						return ""; //$NON-NLS-1$
					} else {
						return nf.format(speed / UI.UNIT_VALUE_DISTANCE);
					}
				}

			}
			return (getText(obj));
		}

		public Color getForeground(final Object element, final int columnIndex) {
//			if (/* columnIndex != 0 && */element instanceof TVTITourMapReferenceTour) {
//				return fColorRefFg;
//			}
			return fColorRefFg;
			// if (element instanceof TVITourBookTour) {
			// return fColorTourFg;
			// }
			// if (columnIndex != 0 && element instanceof TVITourBookMonth) {
			// return fColorMonthFg;
			// }
//			return null;
		}
	}

	public TourCatalogView() {}

	/**
	 * Find the compared tours in the tour map tree viewer<br>
	 * !!! Recursive !!!
	 * 
	 * @param comparedTours
	 * @param parentItem
	 * @param findCompIds
	 *        comp id's which should be found
	 */
	private static void getComparedTours(	ArrayList<TourCatalogItemComparedTour> comparedTours,
											final TreeViewerItem parentItem,
											final ArrayList<Long> findCompIds) {

		final ArrayList<TreeViewerItem> unfetchedChildren = parentItem.getUnfetchedChildren();

		if (unfetchedChildren != null) {

			// children are available

			for (final TreeViewerItem tourTreeItem : unfetchedChildren) {

				if (tourTreeItem instanceof TourCatalogItemComparedTour) {

					final TourCatalogItemComparedTour ttiCompResult = (TourCatalogItemComparedTour) tourTreeItem;
					final long ttiCompId = ttiCompResult.getCompId();

					for (final Long compId : findCompIds) {
						if (ttiCompId == compId) {
							comparedTours.add(ttiCompResult);
						}
					}

				} else {
					// this is a child which can be the parent for other childs
					getComparedTours(comparedTours, tourTreeItem, findCompIds);
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

					// check if the compared tour was saved in the database
					if (compareTourProperty.isDataSaved) {

						ArrayList<Long> compareIds = new ArrayList<Long>();
						compareIds.add(compareTourProperty.compareId);

						// find the compared tour in the viewer
						ArrayList<TourCatalogItemComparedTour> comparedTours = new ArrayList<TourCatalogItemComparedTour>();
						final TreeViewerItem rootItem = ((TourContentProvider) fTourViewer.getContentProvider()).getRootItem();

						getComparedTours(comparedTours, rootItem, compareIds);

						if (comparedTours.size() > 0) {

							TourCatalogItemComparedTour comparedTour = comparedTours.get(0);

							// update entity
							comparedTour.setStartIndex(compareTourProperty.startIndex);
							comparedTour.setEndIndex(compareTourProperty.endIndex);
							comparedTour.setTourSpeed(compareTourProperty.speed);

							// update the viewer
							fTourViewer.update(comparedTour, null);
						}
					}
				}
			}
		};

		TourManager.getInstance().addPropertyListener(fCompareTourPropertyListener);
	}

	private void addPartListener() {

		fPartListener = new IPartListener2() {
			public void partActivated(final IWorkbenchPartReference partRef) {}

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

			public void partHidden(final IWorkbenchPartReference partRef) {}

			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			public void partOpened(final IWorkbenchPartReference partRef) {
				/*
				 * add the actions in the part open event so they are appended AFTER the actions
				 * which are defined in the plugin.xml
				 */
				fillToolbar();
			}

			public void partVisible(final IWorkbenchPartReference partRef) {}
		};

		getViewSite().getPage().addPartListener(fPartListener);
	}

	private void addPostSelectionListener() {

		// this view part is a selection listener
		fPostSelectionListener = new ISelectionListener() {

			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				// update the view when a new tour reference was created
				if (selection instanceof SelectionPersistedCompareResults) {

					final SelectionPersistedCompareResults selectionPersisted = (SelectionPersistedCompareResults) selection;

					final ArrayList<CompareResultItemComparedTour> persistedCompareResults = selectionPersisted.persistedCompareResults;

					if (persistedCompareResults.size() > 0) {
						updateTourViewer(persistedCompareResults);
					}

				} else if (selection instanceof SelectionNewRefTours) {

					refreshViewer();

//					final SelectionNewRefTours tourSelection = (SelectionNewRefTours) selection;
//					final ArrayList<TourReference> newRefTours = tourSelection.newRefTours;
//
//					if (newRefTours.size() > 0) {
//						refreshView();
//					}

				} else if (selection instanceof SelectionRemovedComparedTours) {

					final SelectionRemovedComparedTours removedCompTours = (SelectionRemovedComparedTours) selection;

					/*
					 * find/remove the removed compared tours in the viewer
					 */

					ArrayList<TourCatalogItemComparedTour> comparedTours = new ArrayList<TourCatalogItemComparedTour>();
					final TreeViewerItem rootItem = ((TourContentProvider) fTourViewer.getContentProvider()).getRootItem();

					getComparedTours(comparedTours, rootItem, removedCompTours.removedComparedTours);

					// remove compared tour from the data model
					for (final TourCatalogItemComparedTour comparedTour : comparedTours) {
						comparedTour.remove();
					}

					// remove compared tour from the tree viewer
					fTourViewer.remove(comparedTours.toArray());

				} else if (selection instanceof StructuredSelection) {

					StructuredSelection structuredSelection = (StructuredSelection) selection;

					Object firstElement = structuredSelection.getFirstElement();

					if (firstElement instanceof TourCatalogItemComparedTour) {

						// select the compared tour in the tour viewer

						final TourCatalogItemComparedTour linkedTour = (TourCatalogItemComparedTour) firstElement;

						// check if the linked tour is already set, prevent recursion
						if (fLinkedTour != linkedTour) {
							fLinkedTour = linkedTour;
							selectLinkedTour();
						}
					}
				}
			}
		};

		// register selection listener in the page
		getSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

	private void addPrefListener() {

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(final Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

					UI.updateUnits();

					saveSettings();

					// dispose viewer
					Control[] children = fViewerContainer.getChildren();
					for (int childIndex = 0; childIndex < children.length; childIndex++) {
						children[childIndex].dispose();
					}

					createTourViewer(fViewerContainer);
					fViewerContainer.layout();

					// update the viewer
					fTourViewer.setInput(((TourContentProvider) fTourViewer.getContentProvider()).getRootItem());

					restoreState(fSessionMemento);
				}
			}
		};
		TourbookPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(fPrefChangeListener);
	}

	private void createActions() {

		fActionDeleteSelectedTour = new ActionDeleteTourFromMap(this);
		fActionRenameRefTour = new ActionRenameRefTour(this);
		fActionLinkTour = new ActionLinkTour(this);
		fActionRefreshView = new ActionRefreshView(this);
		fActionCollapseAll = new ActionCollapseAll(this);
	}

	/**
	 * create the views context menu
	 */
	private void createContextMenu() {

		final MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager manager) {
				TourCatalogView.this.fillContextMenu(manager);
			}
		});

		// add the context menu to the table viewer
		final Control tourViewer = fTourViewer.getControl();
		final Menu menu = menuMgr.createContextMenu(tourViewer);
		tourViewer.setMenu(menu);
	}

	@Override
	public void createPartControl(final Composite parent) {

		final Display display = parent.getDisplay();

		fColorRefFg = new Color(display, fRGBRefFg);
		fColorRefBg = new Color(display, fRGBRefBg);
		fColorYearFg = new Color(display, fRGBYearFg);
		fColorYearBg = new Color(display, fRGBYearBg);
		fColorTourFg = new Color(display, fRGBTourFg);
		fColorTourBg = new Color(display, fRGBTourBg);

		fViewerContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(fViewerContainer);
		createTourViewer(fViewerContainer);

		createActions();

		addPartListener();
		addPostSelectionListener();
		addCompareTourPropertyListener();
		addPrefListener();

		// set selection provider
		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		fTourViewer.setInput(((TourContentProvider) fTourViewer.getContentProvider()).getRootItem());

		restoreState(fSessionMemento);
	}

	private Control createTourViewer(final Composite parent) {

		final TreeColumnLayout treeLayout = new TreeColumnLayout();

		Composite layoutContainer = new Composite(parent, SWT.NONE);
		layoutContainer.setLayout(treeLayout);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(layoutContainer);

		// tour tree
		final Tree tree = new Tree(layoutContainer, SWT.H_SCROLL
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
		tc.setText(Messages.Tour_Map_Column_tour);
		treeLayout.setColumnData(tc, new ColumnWeightData(18, true));

		tc = new TreeColumn(tree, SWT.TRAIL);
		tc.setText(UI.UNIT_LABEL_SPEED);
		treeLayout.setColumnData(tc, new ColumnPixelData(pixelConverter.convertWidthInCharsToPixels(10), false));

		// tour viewer
		fTourViewer = new TreeViewer(tree);
		fTourViewer.setContentProvider(new TourContentProvider());
		fTourViewer.setLabelProvider(new TourLabelProvider());
		fTourViewer.setUseHashlookup(true);

		fTourViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				onSelectionChanged((IStructuredSelection) event.getSelection());
//				enableActions();
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
				if (tourItem instanceof TourCatalogItemComparedTour) {
					tourId = ((TourCatalogItemComparedTour) tourItem).getTourId();
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

		createContextMenu();

		return layoutContainer;
	}

	@Override
	public void dispose() {

		getSite().getPage().removePostSelectionListener(fPostSelectionListener);
		getViewSite().getPage().removePartListener(fPartListener);
		TourManager.getInstance().removePropertyListener(fCompareTourPropertyListener);

		TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);

		fColorRefFg.dispose();
		fColorYearFg.dispose();
		fColorTourFg.dispose();
		fColorRefBg.dispose();
		fColorYearBg.dispose();
		fColorTourBg.dispose();

		super.dispose();
	}

	private void enableActions() {

		final ITreeSelection selection = (ITreeSelection) fTourViewer.getSelection();

		int refItemCounter = 0;
		int tourItemCounter = 0;
		int yearItemCounter = 0;

		// count how many different items are selected
		for (final Iterator<?> iter = selection.iterator(); iter.hasNext();) {

			final Object item = (Object) iter.next();

			if (item instanceof TourCatalogItemReferenceTour) {
				refItemCounter++;
			} else if (item instanceof TourCatalogItemComparedTour) {
				tourItemCounter++;
			} else if (item instanceof TourCatalogItemYear) {
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
		fActionRenameRefTour.setEnabled(refItemCounter == 1 && tourItemCounter == 0 && yearItemCounter == 0);
//
//		fActionAdjustAltitude.setEnabled(tourItemCounter > 0);
	}

	private void fillContextMenu(final IMenuManager menuMgr) {

//		menuMgr.add(fActionAdjustAltitude);
		menuMgr.add(fActionRenameRefTour);
		menuMgr.add(new Separator());
		menuMgr.add(fActionDeleteSelectedTour);

		enableActions();
	}

	private void fillToolbar() {

		// check if toolbar is created
		if (fIsToolbarCreated) {
			return;
		}
		fIsToolbarCreated = true;

		IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

		tbm.add(fActionLinkTour);
		tbm.add(fActionCollapseAll);
		tbm.add(fActionRefreshView);

		tbm.update(true);
	}

	public ColumnManager getColumnManager() {
		return null;
	}

	public TreeViewer getTourViewer() {
		return fTourViewer;
	}

	public TreeViewer getTreeViewer() {
		return fTourViewer;
	}

	@Override
	public void init(final IViewSite site, final IMemento memento) throws PartInitException {
		super.init(site, memento);

		// set the session memento if it's net yet set
		if (fSessionMemento == null) {
			fSessionMemento = memento;
		}
	}

	/**
	 * Selection changes in the tour map viewer
	 * 
	 * @param selection
	 */
	private void onSelectionChanged(IStructuredSelection selection) {

		// show the reference tour chart
		final Object item = selection.getFirstElement();

		ISelection tourMapSelection = null;

		if (item instanceof TourCatalogItemReferenceTour) {

			final TourCatalogItemReferenceTour refItem = (TourCatalogItemReferenceTour) item;

			tourMapSelection = new SelectionTourMapView(refItem.refId);
			fActiveRefId = refItem.refId;

		} else if (item instanceof TourCatalogItemYear) {

			final TourCatalogItemYear yearItem = (TourCatalogItemYear) item;

			tourMapSelection = new SelectionTourMapView(yearItem.refId);
			fActiveRefId = yearItem.refId;

			((SelectionTourMapView) tourMapSelection).setYearData(yearItem);

		} else if (item instanceof TourCatalogItemComparedTour) {

			final TourCatalogItemComparedTour compItem = (TourCatalogItemComparedTour) item;

			tourMapSelection = new StructuredSelection(compItem);
			fActiveRefId = compItem.getRefId();
		}

		if (tourMapSelection != null) {
			fPostSelectionProvider.setSelection(tourMapSelection);
		}
	}

	public void refreshViewer() {
		// refresh the tree viewer and resort the ref tours
		fRootItem.fetchChildren();
		fTourViewer.refresh();
	}

	/**
	 * Restore settings from the last session
	 * 
	 * @param memento
	 */
	private void restoreState(final IMemento memento) {

		if (memento == null) {
			return;
		}

		/*
		 * select ref tour in tour viewer
		 */
		String mementoRefId = memento.getString(MEMENTO_TOUR_MAP_ACTIVE_REF_ID);
		if (mementoRefId != null) {

			try {
				selectRefTour(Long.parseLong(mementoRefId));
			} catch (NumberFormatException e) {
				// do nothing
			}
		}

		/*
		 * action: link tour with statistics
		 */
		Integer mementoLinkTour = memento.getInteger(MEMENTO_TOUR_MAP_LINK_TOUR);
		if (mementoLinkTour != null) {
			fActionLinkTour.setChecked(mementoLinkTour == 1);
		}
	}

	private void saveSettings() {
		fSessionMemento = XMLMemento.createWriteRoot("TourMapView"); //$NON-NLS-1$
		saveState(fSessionMemento);
	}

	@Override
	public void saveState(final IMemento memento) {

		memento.putString(MEMENTO_TOUR_MAP_ACTIVE_REF_ID, Long.toString(fActiveRefId));
		memento.putInteger(MEMENTO_TOUR_MAP_LINK_TOUR, fActionLinkTour.isChecked() ? 1 : 0);

	}

	/**
	 * select the tour which was selected in the year chart
	 */
	void selectLinkedTour() {
		if (fActionLinkTour.isChecked()) {
			fTourViewer.setSelection(new StructuredSelection((fLinkedTour)), true);
		}
	}

	/**
	 * Select the reference tour in the tour viewer
	 * 
	 * @param refId
	 */
	private void selectRefTour(long refId) {

		Object[] refTourItems = fRootItem.getFetchedChildren();

		// search ref tour
		for (Object refTourItem : refTourItems) {
			if (refTourItem instanceof TourCatalogItemReferenceTour) {

				TourCatalogItemReferenceTour tvtiRefTour = (TourCatalogItemReferenceTour) refTourItem;
				if (tvtiRefTour.refId == refId) {

					// select ref tour
					fTourViewer.setSelection(new StructuredSelection(tvtiRefTour), true);
					break;
				}
			}
		}
	}

	@Override
	public void setFocus() {
		fTourViewer.getTree().setFocus();
	}

	/**
	 * Update viewer with new saved compared tours
	 * 
	 * @param persistedCompareResults
	 */
	private void updateTourViewer(final ArrayList<CompareResultItemComparedTour> persistedCompareResults) {

		// ref id's which hast new children
		final HashMap<Long, Long> viewRefIds = new HashMap<Long, Long>();

		// get all ref tours which needs to be updated
		for (final CompareResultItemComparedTour compareResult : persistedCompareResults) {

			if (compareResult.getParentItem() instanceof CompareResultItemReferenceTour) {

				final long compResultRefId = ((CompareResultItemReferenceTour) compareResult.getParentItem()).refTour.getRefId();

				viewRefIds.put(compResultRefId, compResultRefId);
			}
		}

		// remove all compare results that from the selection
		persistedCompareResults.clear();

		// loop: all ref tours where children has been added
		for (final Iterator<Long> refIdIter = viewRefIds.values().iterator(); refIdIter.hasNext();) {

			final Long refId = (Long) refIdIter.next();

			final ArrayList<TreeViewerItem> unfetchedChildren = fRootItem.getUnfetchedChildren();
			if (unfetchedChildren != null) {

				for (final TreeViewerItem rootChild : unfetchedChildren) {
					final TourCatalogItemReferenceTour mapRefTour = (TourCatalogItemReferenceTour) rootChild;

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
}
