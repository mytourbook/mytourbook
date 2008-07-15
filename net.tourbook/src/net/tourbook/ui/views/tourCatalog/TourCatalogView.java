/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tag.ActionRemoveAllTags;
import net.tourbook.tag.ActionSetTourTag;
import net.tourbook.tag.TagManager;
import net.tourbook.tour.ActionEditQuick;
import net.tourbook.tour.ITourPropertyListener;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TreeViewerItem;
import net.tourbook.ui.ActionCollapseAll;
import net.tourbook.ui.ActionEditTour;
import net.tourbook.ui.ActionModifyColumns;
import net.tourbook.ui.ActionOpenPrefDialog;
import net.tourbook.ui.ActionRefreshView;
import net.tourbook.ui.ActionSetTourType;
import net.tourbook.ui.ColumnManager;
import net.tourbook.ui.ISelectedTours;
import net.tourbook.ui.ITourViewer;
import net.tourbook.ui.TreeColumnDefinition;
import net.tourbook.ui.TreeColumnFactory;
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
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.part.ViewPart;

public class TourCatalogView extends ViewPart implements ITourViewer, ISelectedTours {

	private static final String			MEMENTO_TOUR_CATALOG_ACTIVE_REF_ID	= "tour.catalog.active.ref.id";					//$NON-NLS-1$
	private static final String			MEMENTO_TOUR_CATALOG_LINK_TOUR		= "tour.catalog.link.tour";						//$NON-NLS-1$

	public static final String			ID									= "net.tourbook.views.tourCatalog.TourCatalogView"; //$NON-NLS-1$

	public static final int				COLUMN_LABEL						= 0;
	public static final int				COLUMN_SPEED						= 1;

	/**
	 * This memento allows this view to save and restore state when it is closed and opened within a
	 * session. A different memento is supplied by the platform for persistance at workbench
	 * shutdown.
	 */
	private static IMemento				fSessionMemento						= null;

	private TVICatalogRootItem			fRootItem;

	private Composite					fViewerContainer;
	private TreeViewer					fTourViewer;

	private final NumberFormat			fNf									= NumberFormat.getNumberInstance();

	private ISelectionListener			fPostSelectionListener;
	private IPartListener2				fPartListener;
	private PostSelectionProvider		fPostSelectionProvider;
	private ITourPropertyListener		fCompareTourPropertyListener;
	private IPropertyChangeListener		fPrefChangeListener;
	private ITourPropertyListener		fTourPropertyListener;

	protected int						fRefTourXMarkerValue;

	private ActionRemoveComparedTours	fActionDeleteSelectedTour;
	private ActionRenameRefTour			fActionRenameRefTour;
	private ActionLinkTour				fActionLinkTour;
	private ActionCollapseAll			fActionCollapseAll;
	private ActionRefreshView			fActionRefreshView;
	private ActionModifyColumns			fActionModifyColumns;
	private ActionEditQuick				fActionEditQuick;
	private ActionEditTour				fActionEditTour;
	private ActionSetTourType			fActionSetTourType;
	private ActionSetTourTag			fActionAddTag;
	private ActionRemoveAllTags			fActionRemoveAllTags;
	private ActionOpenPrefDialog		fActionOpenTagPrefs;
	private ActionSetTourTag			fActionRemoveTag;

	/**
	 * tour item which is selected by the link tour action
	 */
	protected TVICatalogComparedTour	fLinkedTour;

	/**
	 * ref id which is currently selected in the tour viewer
	 */
	private long						fActiveRefId;

	/**
	 * flag if actions are added to the toolbar
	 */
	private boolean						fIsToolbarCreated					= false;
	private ColumnManager				fColumnManager;

	class TourContentProvider implements ITreeContentProvider {

		public void dispose() {}

		public Object[] getChildren(final Object parentElement) {
			return ((TreeViewerItem) parentElement).getFetchedChildrenAsArray();
		}

		public Object[] getElements(final Object inputElement) {
			return fRootItem.getFetchedChildrenAsArray();
		}

		public Object getParent(final Object element) {
			return ((TreeViewerItem) element).getParentItem();
		}

		public boolean hasChildren(final Object element) {
			return ((TreeViewerItem) element).hasChildren();
		}

		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	/**
	 * Find the compared tours in the tour map tree viewer<br>
	 * !!! Recursive !!!
	 * 
	 * @param comparedTours
	 * @param parentItem
	 * @param findCompIds
	 *            comp id's which should be found
	 */
	private static void getComparedTours(	final ArrayList<TVICatalogComparedTour> comparedTours,
											final TreeViewerItem parentItem,
											final ArrayList<Long> findCompIds) {

		final ArrayList<TreeViewerItem> unfetchedChildren = parentItem.getUnfetchedChildren();

		if (unfetchedChildren != null) {

			// children are available

			for (final TreeViewerItem tourTreeItem : unfetchedChildren) {

				if (tourTreeItem instanceof TVICatalogComparedTour) {

					final TVICatalogComparedTour ttiCompResult = (TVICatalogComparedTour) tourTreeItem;
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

	public TourCatalogView() {}

	private void addCompareTourPropertyListener() {

		fCompareTourPropertyListener = new ITourPropertyListener() {
			public void propertyChanged(final int propertyId, final Object propertyData) {

				if (propertyId == TourManager.TOUR_PROPERTY_COMPARE_TOUR_CHANGED
						&& propertyData instanceof TourPropertyCompareTourChanged) {

					final TourPropertyCompareTourChanged compareTourProperty = (TourPropertyCompareTourChanged) propertyData;

					// check if the compared tour was saved in the database
					if (compareTourProperty.isDataSaved) {

						final ArrayList<Long> compareIds = new ArrayList<Long>();
						compareIds.add(compareTourProperty.compareId);

						// find the compared tour in the viewer
						final ArrayList<TVICatalogComparedTour> comparedTours = new ArrayList<TVICatalogComparedTour>();

						getComparedTours(comparedTours, fRootItem, compareIds);

						if (comparedTours.size() > 0) {

							final TVICatalogComparedTour comparedTour = comparedTours.get(0);

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

					final ArrayList<TVICompareResultComparedTour> persistedCompareResults = selectionPersisted.persistedCompareResults;

					if (persistedCompareResults.size() > 0) {
						updateTourViewer(persistedCompareResults);
					}

				} else if (selection instanceof SelectionNewRefTours) {

					reloadViewer();

				} else if (selection instanceof SelectionRemovedComparedTours) {

					final SelectionRemovedComparedTours removedCompTours = (SelectionRemovedComparedTours) selection;

					/*
					 * find/remove the removed compared tours in the viewer
					 */

					final ArrayList<TVICatalogComparedTour> comparedTours = new ArrayList<TVICatalogComparedTour>();

					getComparedTours(comparedTours, fRootItem, removedCompTours.removedComparedTours);

					// remove compared tour from the data model
					for (final TVICatalogComparedTour comparedTour : comparedTours) {
						comparedTour.remove();
					}

					// remove compared tour from the tree viewer
					fTourViewer.remove(comparedTours.toArray());
					reloadViewer();

				} else if (selection instanceof StructuredSelection) {

					final StructuredSelection structuredSelection = (StructuredSelection) selection;

					final Object firstElement = structuredSelection.getFirstElement();

					if (firstElement instanceof TVICatalogComparedTour) {

						// select the compared tour in the tour viewer

						final TVICatalogComparedTour linkedTour = (TVICatalogComparedTour) firstElement;

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

		final Preferences prefStore = TourbookPlugin.getDefault().getPluginPreferences();

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(final Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

					UI.updateUnits();

					fColumnManager.saveState(fSessionMemento);

					fColumnManager.resetColumns();
					defineViewerColumns(fViewerContainer);

					recreateViewer();

				} else if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

					// update tourbook viewer
					fTourViewer.refresh();

				} else if (property.equals(ITourbookPreferences.TAG_COLOR_AND_LAYOUT_CHANGED)) {

					fTourViewer.getTree()
							.setLinesVisible(prefStore.getBoolean(ITourbookPreferences.TAG_VIEW_SHOW_LINES));

					fTourViewer.refresh();

					/*
					 * the tree must be redrawn because the styled text does not show with the new
					 * color
					 */
					fTourViewer.getTree().redraw();
				}
			}
		};
		prefStore.addPropertyChangeListener(fPrefChangeListener);
	}

	private void addTourPropertyListener() {

		fTourPropertyListener = new ITourPropertyListener() {
			@SuppressWarnings("unchecked")//$NON-NLS-1$
			public void propertyChanged(final int propertyId, final Object propertyData) {
				if (propertyId == TourManager.TOUR_PROPERTIES_CHANGED) {

					// get a clone of the modified tours because the tours are removed from the list
					final ArrayList<TourData> modifiedTours = (ArrayList<TourData>) ((ArrayList<TourData>) propertyData).clone();

					updateTourViewer(fRootItem, modifiedTours);

				} else if (propertyId == TourManager.TAG_STRUCTURE_CHANGED) {

					reloadViewer();
				}
			}
		};
		TourManager.getInstance().addPropertyListener(fTourPropertyListener);
	}

	private void createActions() {

		fActionDeleteSelectedTour = new ActionRemoveComparedTours(this);
		fActionRenameRefTour = new ActionRenameRefTour(this);
		fActionLinkTour = new ActionLinkTour(this);
		fActionRefreshView = new ActionRefreshView(this);
		fActionCollapseAll = new ActionCollapseAll(this);
		fActionModifyColumns = new ActionModifyColumns(this);

		fActionEditQuick = new ActionEditQuick(this);
		fActionEditTour = new ActionEditTour(this);
		fActionSetTourType = new ActionSetTourType(this);

		fActionAddTag = new ActionSetTourTag(this, true);
		fActionRemoveTag = new ActionSetTourTag(this, false);
		fActionRemoveAllTags = new ActionRemoveAllTags(this);

		fActionOpenTagPrefs = new ActionOpenPrefDialog(Messages.app_action_tag_open_tagging_structure,
				"net.tourbook.preferences.PrefPageTags");
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

		// define all columns for the viewer
		fColumnManager = new ColumnManager(this, fSessionMemento);
		defineViewerColumns(parent);

		fViewerContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(fViewerContainer);

		createTourViewer(fViewerContainer);

		createActions();
		fillViewMenu();

		addPartListener();
		addPostSelectionListener();
		addCompareTourPropertyListener();
		addTourPropertyListener();
		addPrefListener();

		// set selection provider
		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		fTourViewer.setInput(fRootItem = new TVICatalogRootItem(this));

		restoreState(fSessionMemento);
	}

	private void createTourViewer(final Composite parent) {

		// tour tree
		final Tree tree = new Tree(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);

		GridDataFactory.fillDefaults().grab(true, true).applyTo(tree);

		tree.setHeaderVisible(true);
		tree.setLinesVisible(TourbookPlugin.getDefault()
				.getPluginPreferences()
				.getBoolean(ITourbookPreferences.TAG_VIEW_SHOW_LINES));

		fTourViewer = new TreeViewer(tree);
		fColumnManager.createColumns();

		fTourViewer.setContentProvider(new TourContentProvider());
		fTourViewer.setUseHashlookup(true);

		fTourViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				onSelectionChanged((IStructuredSelection) event.getSelection());
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
				if (tourItem instanceof TVICatalogComparedTour) {
					tourId = ((TVICatalogComparedTour) tourItem).getTourId();
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
	}

	private void defineViewerColumns(final Composite parent) {

		final PixelConverter pixelConverter = new PixelConverter(parent);
		TreeColumnDefinition colDef;

		/*
		 * first column: ref tour name/compare tour name /year
		 */
		colDef = TreeColumnFactory.REF_TOUR.createColumn(fColumnManager, pixelConverter);
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new StyledCellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				if ((element instanceof TVICatalogReferenceTour)) {

					// ref tour item

					final TVICatalogReferenceTour refItem = (TVICatalogReferenceTour) element;

					final StyledString styledString = new StyledString();
					styledString.append(refItem.label, UI.TAG_STYLER);

					cell.setText(styledString.getString());
					cell.setStyleRanges(styledString.getStyleRanges());

				} else if (element instanceof TVICatalogYearItem) {

					// year item

					final TVICatalogYearItem yearItem = (TVICatalogYearItem) element;
					final StyledString styledString = new StyledString();
					styledString.append(Integer.toString(yearItem.year), UI.TAG_SUB_STYLER);
					styledString.append("   " + yearItem.tourCounter, StyledString.QUALIFIER_STYLER); //$NON-NLS-1$

					cell.setText(styledString.getString());
					cell.setStyleRanges(styledString.getStyleRanges());

				} else if (element instanceof TVICatalogComparedTour) {

					// compared tour item

					cell.setText(DateFormat.getDateInstance(DateFormat.SHORT)
							.format(((TVICatalogComparedTour) element).getTourDate()));

				}
			}
		});

		/*
		 * column: tour type
		 */
		colDef = TreeColumnFactory.TOUR_TYPE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVICatalogComparedTour) {
					cell.setImage(UI.getInstance().getTourTypeImage(((TVICatalogComparedTour) element).tourTypeId));
				}
			}
		});

		/*
		 * column: title
		 */
		colDef = TreeColumnFactory.TITLE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVICatalogComparedTour) {
					cell.setText(((TVICatalogComparedTour) element).tourTitle);
				}
			}
		});

		/*
		 * column: tags
		 */
		colDef = TreeColumnFactory.TOUR_TAGS.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVICatalogComparedTour) {
					cell.setText(TourDatabase.getTagNames(((TVICatalogComparedTour) element).tagIds));
				}
			}
		});

		/*
		 * column: speed
		 */
		colDef = TreeColumnFactory.SPEED.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				if (element instanceof TVICatalogComparedTour) {

					fNf.setMinimumFractionDigits(1);
					fNf.setMaximumFractionDigits(1);

					final float speed = ((TVICatalogComparedTour) element).getTourSpeed();
					if (speed > 0) {
						cell.setText(fNf.format(speed / UI.UNIT_VALUE_DISTANCE));
					}
				}
			}
		});

	}

	@Override
	public void dispose() {

		getSite().getPage().removePostSelectionListener(fPostSelectionListener);
		getViewSite().getPage().removePartListener(fPartListener);

		TourManager.getInstance().removePropertyListener(fCompareTourPropertyListener);
		TourManager.getInstance().removePropertyListener(fTourPropertyListener);

		TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);

		super.dispose();
	}

	private void enableActions() {

		final ITreeSelection selection = (ITreeSelection) fTourViewer.getSelection();

		int refItems = 0;
		int yearItems = 0;
		int tourItems = 0;
		TVICatalogComparedTour firstTour = null;

		// count how many different items are selected
		for (final Iterator<?> iter = selection.iterator(); iter.hasNext();) {

			final Object treeItem = iter.next();

			if (treeItem instanceof TVICatalogReferenceTour) {
				refItems++;
			} else if (treeItem instanceof TVICatalogComparedTour) {
				if (tourItems == 0) {
					firstTour = (TVICatalogComparedTour) treeItem;
				}
				tourItems++;
			} else if (treeItem instanceof TVICatalogYearItem) {
				yearItems++;
			}
		}
		final boolean isTourSelected = tourItems > 0 && refItems == 0 && yearItems == 0;
		final boolean isOneTour = tourItems == 1 && refItems == 0 && yearItems == 0;

		fActionDeleteSelectedTour.setEnabled(isOneTour);

		// enable remove button when only one type of item is selected
		if (yearItems == 0 && ((refItems > 0 && tourItems == 0) || (refItems == 0 & tourItems > 0))) {
			fActionDeleteSelectedTour.setEnabled(true);
		} else {
			fActionDeleteSelectedTour.setEnabled(false);
		}

		fActionEditTour.setEnabled(isOneTour);
		fActionEditQuick.setEnabled(isOneTour);

		fActionRenameRefTour.setEnabled(refItems == 1 && tourItems == 0 && yearItems == 0);

		final ArrayList<TourType> tourTypes = TourDatabase.getTourTypes();
		fActionSetTourType.setEnabled(isTourSelected && tourTypes.size() > 0);

		fActionAddTag.setEnabled(isTourSelected);

		if (isOneTour) {

			// one tour is selected

			final ArrayList<Long> tagIds = firstTour.tagIds;
			if (tagIds != null && tagIds.size() > 0) {

				// at least one tag is within the tour

				fActionRemoveAllTags.setEnabled(true);
				fActionRemoveTag.setEnabled(true);
			} else {
				// tags are not available
				fActionRemoveAllTags.setEnabled(false);
				fActionRemoveTag.setEnabled(false);
			}
		} else {

			// multiple tours are selected

			fActionRemoveTag.setEnabled(isTourSelected);
			fActionRemoveAllTags.setEnabled(isTourSelected);
		}
		// enable/disable actions for the recent tags
		TagManager.enableRecentTagActions(isTourSelected);

	}

	private void fillContextMenu(final IMenuManager menuMgr) {

		menuMgr.add(fActionAddTag);
		menuMgr.add(fActionRemoveTag);
		menuMgr.add(fActionRemoveAllTags);

		TagManager.fillRecentTagsIntoMenu(menuMgr, this, true);

		menuMgr.add(new Separator());
		menuMgr.add(fActionOpenTagPrefs);

		menuMgr.add(new Separator());
		menuMgr.add(fActionRenameRefTour);
		menuMgr.add(fActionDeleteSelectedTour);

		menuMgr.add(new Separator());
		menuMgr.add(fActionEditQuick);
		menuMgr.add(fActionSetTourType);
		menuMgr.add(fActionEditTour);

		enableActions();
	}

	private void fillToolbar() {

		// check if toolbar is created
		if (fIsToolbarCreated) {
			return;
		}
		fIsToolbarCreated = true;

		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

		tbm.add(fActionLinkTour);
		tbm.add(fActionCollapseAll);
		tbm.add(fActionRefreshView);

		tbm.update(true);
	}

	private void fillViewMenu() {

		/*
		 * fill view menu
		 */
		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();
		menuMgr.add(fActionModifyColumns);
	}

	public void fireSelection(final ISelection selection) {
		fPostSelectionProvider.setSelection(selection);
	}

	public ColumnManager getColumnManager() {
		return fColumnManager;
	}

	public ArrayList<TourData> getSelectedTours() {

		// get selected tours

		final IStructuredSelection selectedTours = ((IStructuredSelection) fTourViewer.getSelection());
		final ArrayList<TourData> selectedTourData = new ArrayList<TourData>();

		// loop: all selected tours
		for (final Iterator<?> iter = selectedTours.iterator(); iter.hasNext();) {

			final Object treeItem = iter.next();

			if (treeItem instanceof TVICatalogComparedTour) {

				final TVICatalogComparedTour tourItem = ((TVICatalogComparedTour) treeItem);

				final TourData tourData = TourManager.getInstance().getTourData(tourItem.getTourId());
				if (tourData != null) {
					selectedTourData.add(tourData);
				}
			}
		}

		return selectedTourData;
	}

	public TreeViewer getTourViewer() {
		return fTourViewer;
	}

	public ColumnViewer getViewer() {
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

	public boolean isFromTourEditor() {
		return false;
	}

	/**
	 * Selection changes in the tour map viewer
	 * 
	 * @param selection
	 */
	private void onSelectionChanged(final IStructuredSelection selection) {

		// show the reference tour chart
		final Object item = selection.getFirstElement();

		ISelection tourCatalogSelection = null;
		boolean selectTourInChart = false;

		if (item instanceof TVICatalogReferenceTour) {

			// reference tour is selected

			final TVICatalogReferenceTour refItem = (TVICatalogReferenceTour) item;

			tourCatalogSelection = new SelectionTourCatalogView(refItem.refId);
			fActiveRefId = refItem.refId;

			((SelectionTourCatalogView) tourCatalogSelection).setRefItem(refItem);

			selectTourInChart = true;

		} else if (item instanceof TVICatalogYearItem) {

			// year item is selected

			final TVICatalogYearItem yearItem = (TVICatalogYearItem) item;

			tourCatalogSelection = new SelectionTourCatalogView(yearItem.refId);
			fActiveRefId = yearItem.refId;

			((SelectionTourCatalogView) tourCatalogSelection).setYearData(yearItem);

			selectTourInChart = true;

		} else if (item instanceof TVICatalogComparedTour) {

			// compared tour is selected

			final TVICatalogComparedTour compItem = (TVICatalogComparedTour) item;

			tourCatalogSelection = new StructuredSelection(compItem);
			fActiveRefId = compItem.getRefId();
		}

		if (tourCatalogSelection != null) {

			// fire selection for the selected tour catalog item
			fPostSelectionProvider.setSelection(tourCatalogSelection);

			if (selectTourInChart) {

				/*
				 * get selection from year statistic view, this selection is set from the previous
				 * fired selection
				 */
				final ISelection selectionInTourChart = getSite().getWorkbenchWindow()
						.getSelectionService()
						.getSelection(TourCatalogViewYearStatistic.ID);

				fPostSelectionProvider.setSelection(selectionInTourChart);
			}
		}
	}

	public void recreateViewer() {

		final Object[] expandedElements = fTourViewer.getExpandedElements();
		final ISelection selection = fTourViewer.getSelection();

		fViewerContainer.setRedraw(false);
		{
			fTourViewer.getTree().dispose();

			createTourViewer(fViewerContainer);
			fViewerContainer.layout();

			fTourViewer.setInput(fRootItem = new TVICatalogRootItem(this));

			fTourViewer.setExpandedElements(expandedElements);
			fTourViewer.setSelection(selection);
		}
		fViewerContainer.setRedraw(true);
	}

	public void reloadViewer() {

		final Tree tree = fTourViewer.getTree();
		tree.setRedraw(false);
		{
			final Object[] expandedElements = fTourViewer.getExpandedElements();
			final ISelection selection = fTourViewer.getSelection();

			fTourViewer.setInput(fRootItem = new TVICatalogRootItem(this));

			fTourViewer.setExpandedElements(expandedElements);
			fTourViewer.setSelection(selection);
		}
		tree.setRedraw(true);
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
		final String mementoRefId = memento.getString(MEMENTO_TOUR_CATALOG_ACTIVE_REF_ID);
		if (mementoRefId != null) {

			try {
				selectRefTour(Long.parseLong(mementoRefId));
			} catch (final NumberFormatException e) {
				// do nothing
			}
		}

		/*
		 * action: link tour with statistics
		 */
		final Integer mementoLinkTour = memento.getInteger(MEMENTO_TOUR_CATALOG_LINK_TOUR);
		if (mementoLinkTour != null) {
			fActionLinkTour.setChecked(mementoLinkTour == 1);
		}
	}

	private void saveSettings() {
		fSessionMemento = XMLMemento.createWriteRoot("TourCatalogView"); //$NON-NLS-1$
		saveState(fSessionMemento);
	}

	@Override
	public void saveState(final IMemento memento) {

		memento.putString(MEMENTO_TOUR_CATALOG_ACTIVE_REF_ID, Long.toString(fActiveRefId));
		memento.putInteger(MEMENTO_TOUR_CATALOG_LINK_TOUR, fActionLinkTour.isChecked() ? 1 : 0);

		fColumnManager.saveState(memento);
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
	private void selectRefTour(final long refId) {

		final Object[] refTourItems = fRootItem.getFetchedChildrenAsArray();

		// search ref tour
		for (final Object refTourItem : refTourItems) {
			if (refTourItem instanceof TVICatalogReferenceTour) {

				final TVICatalogReferenceTour tvtiRefTour = (TVICatalogReferenceTour) refTourItem;
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
	private void updateTourViewer(final ArrayList<TVICompareResultComparedTour> persistedCompareResults) {

		// ref id's which hast new children
		final HashMap<Long, Long> viewRefIds = new HashMap<Long, Long>();

		// get all ref tours which needs to be updated
		for (final TVICompareResultComparedTour compareResult : persistedCompareResults) {

			if (compareResult.getParentItem() instanceof TVICompareResultReferenceTour) {

				final long compResultRefId = ((TVICompareResultReferenceTour) compareResult.getParentItem()).refTour.getRefId();

				viewRefIds.put(compResultRefId, compResultRefId);
			}
		}

		// clear selection
		persistedCompareResults.clear();

		// loop: all ref tours where children have been added
		for (final Iterator<Long> refIdIter = viewRefIds.values().iterator(); refIdIter.hasNext();) {

			final Long refId = refIdIter.next();

			final ArrayList<TreeViewerItem> unfetchedChildren = fRootItem.getUnfetchedChildren();
			if (unfetchedChildren != null) {

				for (final TreeViewerItem rootChild : unfetchedChildren) {
					final TVICatalogReferenceTour mapRefTour = (TVICatalogReferenceTour) rootChild;

					if (mapRefTour.refId == refId) {

						// reload the children for the reference tour
						mapRefTour.fetchChildren();
						fTourViewer.refresh(mapRefTour, true);

						break;
					}
				}
			}
		}
	}

	/**
	 * !!!Recursive !!! update all tour items with new data
	 * 
	 * @param rootItem
	 * @param modifiedTours
	 */
	private void updateTourViewer(final TreeViewerItem parentItem, final ArrayList<TourData> modifiedTours) {

		final ArrayList<TreeViewerItem> children = parentItem.getUnfetchedChildren();

		if (children == null) {
			return;
		}

		// loop: all children
		for (final Object object : children) {
			if (object instanceof TreeViewerItem) {

				final TreeViewerItem treeItem = (TreeViewerItem) object;
				if (treeItem instanceof TVICatalogComparedTour) {

					final TVICatalogComparedTour tourItem = (TVICatalogComparedTour) treeItem;
					final long tourItemId = tourItem.getTourId();

					for (final TourData modifiedTourData : modifiedTours) {
						if (modifiedTourData.getTourId().longValue() == tourItemId) {

							// update tree item

							final TourType tourType = modifiedTourData.getTourType();
							if (tourType != null) {
								tourItem.tourTypeId = tourType.getTypeId();
							}

							// update item title
							tourItem.tourTitle = modifiedTourData.getTourTitle();

							// update item tags
							final Set<TourTag> tourTags = modifiedTourData.getTourTags();
							final ArrayList<Long> tagIds;

							tourItem.tagIds = tagIds = new ArrayList<Long>();
							for (final TourTag tourTag : tourTags) {
								tagIds.add(tourTag.getTagId());
							}

							// update item in the viewer
							fTourViewer.update(tourItem, null);

							break;
						}
					}

				} else {
					// update children
					updateTourViewer(treeItem, modifiedTours);
				}
			}
		}
	}
}
