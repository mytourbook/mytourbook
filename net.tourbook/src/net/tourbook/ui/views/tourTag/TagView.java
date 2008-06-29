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

package net.tourbook.ui.views.tourTag;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.tag.ActionMenuSetAllTagStructures;
import net.tourbook.tag.ActionMenuSetTagStructure;
import net.tourbook.tag.ActionRenameTag;
import net.tourbook.tag.ChangedTags;
import net.tourbook.tour.ITourPropertyListener;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TreeViewerItem;
import net.tourbook.ui.ActionExpandSelection;
import net.tourbook.ui.ActionRefreshView;
import net.tourbook.ui.ColumnManager;
import net.tourbook.ui.ISelectedTours;
import net.tourbook.ui.ITourViewer;
import net.tourbook.ui.TreeColumnDefinition;
import net.tourbook.ui.TreeColumnFactory;
import net.tourbook.ui.UI;
import net.tourbook.ui.views.tourCatalog.ActionCollapseAll;
import net.tourbook.util.PixelConverter;
import net.tourbook.util.PostSelectionProvider;
import net.tourbook.util.StringToArrayConverter;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;

public class TagView extends ViewPart implements ISelectedTours, ITourViewer {

	static public final String				ID							= "net.tourbook.views.tagViewID";	//$NON-NLS-1$

	private static final String				MEMENTO_COLUMN_SORT_ORDER	= "tagview.column_sort_order";		//$NON-NLS-1$
	private static final String				MEMENTO_COLUMN_WIDTH		= "tagview.column_width";			//$NON-NLS-1$

	private IMemento						fSessionMemento;

	private Composite						fViewerContainer;

	private TreeViewer						fTagViewer;
	private DrillDownAdapter				fDrillDownAdapter;
	private TVITagViewRoot					fRootItem;

	private ColumnManager					fColumnManager;

	private PostSelectionProvider			fPostSelectionProvider;
	private ITourPropertyListener			fTourPropertyListener;

	private ActionRefreshView				fActionRefreshView;
	private ActionMenuSetTagStructure		fActionSetTagStructure;
	private ActionMenuSetAllTagStructures	fActionSetAllTagStructures;
	private ActionRenameTag					fActionRenameTag;
	private ActionExpandSelection			fActionExpandSelection;
	private ActionCollapseAll				fActionCollapseAll;
	private Action							fActionOpenTagPrefs;

	private static final Image				fImgTagCategory				= TourbookPlugin.getImageDescriptor(Messages.Image__tag_category)
																				.createImage();
	private static final Image				fImgTag						= TourbookPlugin.getImageDescriptor(Messages.Image__tag)
																				.createImage();
	private static final NumberFormat		fNF							= NumberFormat.getNumberInstance();

	private final class TagComparator extends ViewerComparator {
		@Override
		public int compare(final Viewer viewer, final Object obj1, final Object obj2) {

			if (obj1 instanceof TVITagViewTour && obj2 instanceof TVITagViewTour) {

				// sort tours by date
				final TVITagViewTour tourItem1 = (TVITagViewTour) (obj1);
				final TVITagViewTour tourItem2 = (TVITagViewTour) (obj2);

				return tourItem1.tourDate.compareTo(tourItem2.tourDate);
			}

			return 0;
		}
	}

	private class TagComparer implements IElementComparer {

		public boolean equals(final Object a, final Object b) {

			if (a == b) {

				return true;

			} else if (a instanceof TVITagViewYear && b instanceof TVITagViewYear) {

				final TVITagViewYear year1 = (TVITagViewYear) a;
				final TVITagViewYear year2 = (TVITagViewYear) b;
				return year1.getTagId() == year2.getTagId() && year1.getYear() == year2.getYear();

			} else if (a instanceof TVITagViewMonth && b instanceof TVITagViewMonth) {

				final TVITagViewMonth month1 = (TVITagViewMonth) a;
				final TVITagViewMonth month2 = (TVITagViewMonth) b;
				return month1.getYearItem() == month2.getYearItem() && month1.getMonth() == month2.getMonth();

			} else if (a instanceof TVITagViewTagCategory && b instanceof TVITagViewTagCategory) {

				return ((TVITagViewTagCategory) a).tagCategoryId == ((TVITagViewTagCategory) b).tagCategoryId;

			} else if (a instanceof TVITagViewTag && b instanceof TVITagViewTag) {

				return ((TVITagViewTag) a).getTagId() == ((TVITagViewTag) b).getTagId();

			}

			return false;
		}

		public int hashCode(final Object element) {
			return 0;
		}

	}

	private class TagContentProvider implements ITreeContentProvider {

		public void dispose() {}

		public Object[] getChildren(final Object parentElement) {

			if (parentElement instanceof TVITagViewItem) {
				return ((TVITagViewItem) parentElement).getFetchedChildrenAsArray();
			}

			return new Object[0];
		}

		public Object[] getElements(final Object inputElement) {
			return getChildren(inputElement);
		}

		public Object getParent(final Object element) {
			return ((TreeViewerItem) element).getParentItem();
		}

		public boolean hasChildren(final Object element) {
			return ((TreeViewerItem) element).hasChildren();
		}

		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {

			if (newInput == null) {
				return;
			}

			setTagViewTitle(newInput);
		}
	}

	private void addTourPropertyListener() {

		fTourPropertyListener = new ITourPropertyListener() {
			public void propertyChanged(final int propertyId, final Object propertyData) {

				if (propertyId == TourManager.TOUR_TAGS_CHANGED) {
					if (propertyData instanceof ChangedTags) {

						final ChangedTags changedTags = (ChangedTags) propertyData;

						final boolean isAddMode = changedTags.isAddMode();

						// get a clone of the modified tours/tags because the tours are removed from the list
						updateViewer(fRootItem, //
								new ChangedTags(changedTags.getModifiedTags(),
										changedTags.getModifiedTours(),
										isAddMode),
								isAddMode);
					}

				} else if (propertyId == TourManager.TAG_STRUCTURE_CHANGED) {

					final Tree tree = fTagViewer.getTree();
					final Object[] expandedElements = fTagViewer.getExpandedElements();

					tree.setRedraw(false);
					{
						reloadViewer();
						fTagViewer.setExpandedElements(expandedElements);
					}
					tree.setRedraw(true);
				}
			}
		};

		TourManager.getInstance().addPropertyListener(fTourPropertyListener);
	}

	private void createActions() {

		fActionRefreshView = new ActionRefreshView(this);
		fActionSetTagStructure = new ActionMenuSetTagStructure(this);
		fActionSetAllTagStructures = new ActionMenuSetAllTagStructures(this);
		fActionRenameTag = new ActionRenameTag(fTagViewer);

		fActionExpandSelection = new ActionExpandSelection(fTagViewer);
		fActionCollapseAll = new ActionCollapseAll(fTagViewer);

		fActionOpenTagPrefs = new Action(Messages.app_action_tag_open_tagging_structure) {

			@Override
			public void run() {
				PreferencesUtil.createPreferenceDialogOn(//
				Display.getCurrent().getActiveShell(),
						"net.tourbook.preferences.PrefPageTags", //$NON-NLS-1$
						null,
						null).open();
			}
		};

		fillToolBar();
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

		// add the context menu to the table viewer
		final Control tourViewer = fTagViewer.getControl();
		final Menu menu = menuMgr.createContextMenu(tourViewer);
		tourViewer.setMenu(menu);
	}

	@Override
	public void createPartControl(final Composite parent) {

		fViewerContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(fViewerContainer);

		createTagViewer(fViewerContainer);
		createActions();
		createContextMenu();

		// set selection provider
		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		addTourPropertyListener();

		restoreState(fSessionMemento);
		reloadViewer();
	}

	private Control createTagViewer(final Composite parent) {

		// tour tree
		final Tree tree = new Tree(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FLAT | SWT.FULL_SELECTION | SWT.MULTI);

		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tree.setHeaderVisible(true);
		tree.setLinesVisible(false);

		fTagViewer = new TreeViewer(tree);
		fDrillDownAdapter = new DrillDownAdapter(fTagViewer);

		// define and create all columns
		fColumnManager = new ColumnManager(this);
		createTagViewerColumns(parent);
		fColumnManager.createColumns();

		fTagViewer.setContentProvider(new TagContentProvider());
		fTagViewer.setComparer(new TagComparer());
		fTagViewer.setComparator(new TagComparator());
		fTagViewer.setUseHashlookup(true);

		fTagViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {

				final Object selectedItem = ((IStructuredSelection) (event.getSelection())).getFirstElement();

				if (selectedItem instanceof TVITagViewTour) {
					final TVITagViewTour tourItem = (TVITagViewTour) selectedItem;
					fPostSelectionProvider.setSelection(new SelectionTourId(tourItem.getTourId()));
				}

				enableActions();
			}
		});

		fTagViewer.addDoubleClickListener(new IDoubleClickListener() {

			public void doubleClick(final DoubleClickEvent event) {

				final Object selection = ((IStructuredSelection) fTagViewer.getSelection()).getFirstElement();

				if (selection instanceof TVITagViewTour) {

					// open tour in the tour editor

					TourManager.getInstance().openTourInEditor(((TVITagViewTour) selection).getTourId());

				} else if (selection != null) {

					// expand/collapse current item

					final TreeViewerItem tourItem = (TreeViewerItem) selection;

					if (fTagViewer.getExpandedState(tourItem)) {
						fTagViewer.collapseToLevel(tourItem, 1);
					} else {
						fTagViewer.expandToLevel(tourItem, 1);
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
	private void createTagViewerColumns(final Composite parent) {

		final PixelConverter pixelConverter = new PixelConverter(parent);
		TreeColumnDefinition colDef;

		/*
		 * tree column
		 */
		colDef = TreeColumnFactory.TAG.createColumn(fColumnManager, pixelConverter);
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new StyledCellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final TVITagViewItem viewItem = (TVITagViewItem) element;
				final StyledString styledString = new StyledString();

				if (viewItem instanceof TVITagViewTour) {

					styledString.append(viewItem.treeColumn);

					final TVITagViewTour tourItem = (TVITagViewTour) viewItem;
					cell.setImage(UI.getInstance().getTourTypeImage(tourItem.tourTypeId));

				} else if (viewItem instanceof TVITagViewTag) {

					styledString.append(viewItem.treeColumn, UI.TAG_STYLER);
					styledString.append("   " + viewItem.colItemCounter, StyledString.COUNTER_STYLER); //$NON-NLS-1$
					cell.setImage(fImgTag);

				} else if (viewItem instanceof TVITagViewTagCategory) {

					styledString.append(viewItem.treeColumn, UI.TAG_STYLER);
					cell.setImage(fImgTagCategory);

				} else if (viewItem instanceof TVITagViewYear || viewItem instanceof TVITagViewMonth) {

					styledString.append(viewItem.treeColumn);
					cell.setForeground(JFaceResources.getColorRegistry().get(UI.SUB_TAG_COLOR));

				} else {
					styledString.append(viewItem.treeColumn);
				}

				cell.setText(styledString.getString());
				cell.setStyleRanges(styledString.getStyleRanges());
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

				if (element instanceof TVITagViewTour) {
					cell.setText(((TVITagViewTour) element).tourTitle);
				} else {
					cell.setText(UI.EMPTY_STRING);
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
				if (element instanceof TVITagViewTour) {
					TourDatabase.getInstance();
					cell.setText(TourDatabase.getTagNames(((TVITagViewTour) element).tagIds));
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		/*
		 * column: distance (km/miles)
		 */
		colDef = TreeColumnFactory.DISTANCE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final Object element = cell.getElement();
				final TVITagViewItem treeItem = (TVITagViewItem) element;

				if (element instanceof TVITagViewTagCategory == false) {

					// set distance
					fNF.setMinimumFractionDigits(1);
					fNF.setMaximumFractionDigits(1);
					final String distance = fNF.format(((float) treeItem.colDistance) / 1000 / UI.UNIT_VALUE_DISTANCE);
					cell.setText(distance);

					// set color
					if (element instanceof TVITagViewTag) {
						cell.setForeground(JFaceResources.getColorRegistry().get(UI.TAG_COLOR));
					} else if (element instanceof TVITagViewYear || element instanceof TVITagViewMonth) {
						cell.setForeground(JFaceResources.getColorRegistry().get(UI.SUB_TAG_COLOR));
					}

				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

	}

	@Override
	public void dispose() {

		TourManager.getInstance().removePropertyListener(fTourPropertyListener);

		fImgTagCategory.dispose();
		fImgTag.dispose();

		super.dispose();
	}

	private void enableActions() {

		final StructuredSelection selection = (StructuredSelection) fTagViewer.getSelection();
		final int selectedItems = selection.size();

		boolean isTagSelected = false;
		final TVITagViewItem firstElement = (TVITagViewItem) selection.getFirstElement();
		if (firstElement instanceof TVITagViewTag) {
			isTagSelected = true;
		}

		/*
		 * tree expand type can be set only for one tag
		 */
		fActionSetTagStructure.setEnabled(isTagSelected && selectedItems == 1);
		fActionRenameTag.setEnabled(isTagSelected && selectedItems == 1);

		fActionExpandSelection.setEnabled(firstElement == null ? false : //
				selectedItems == 1 ? firstElement.hasChildren() : //
						true);
	}

	private void fillContextMenu(final IMenuManager menuMgr) {

		menuMgr.add(new Separator());
		menuMgr.add(fActionExpandSelection);
		menuMgr.add(fActionCollapseAll);

		menuMgr.add(new Separator());
		fDrillDownAdapter.addNavigationActions(menuMgr);

		menuMgr.add(new Separator());
		menuMgr.add(fActionSetTagStructure);
		menuMgr.add(fActionSetAllTagStructures);
		menuMgr.add(fActionRenameTag);
		menuMgr.add(fActionOpenTagPrefs);

		enableActions();
	}

	private void fillToolBar() {
		/*
		 * action in the view toolbar
		 */
		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

		fDrillDownAdapter.addNavigationActions(tbm);

		tbm.add(fActionExpandSelection);
		tbm.add(fActionCollapseAll);

		tbm.add(fActionRefreshView);
	}

	@SuppressWarnings("unchecked")//$NON-NLS-1$
	@Override
	public Object getAdapter(final Class adapter) {

		if (adapter == ColumnViewer.class) {
			return fTagViewer;
		}

		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	public ColumnManager getColumnManager() {
		return null;
	}

	public ArrayList<TourData> getSelectedTours() {

		// get selected tours
		final IStructuredSelection selectedTours = ((IStructuredSelection) fTagViewer.getSelection());

		final TourManager tourManager = TourManager.getInstance();
		final ArrayList<TourData> selectedTourData = new ArrayList<TourData>();

		// loop: all selected tours
		for (final Iterator<?> iter = selectedTours.iterator(); iter.hasNext();) {

			final Object treeItem = iter.next();
			if (treeItem instanceof TVITagViewTour) {

				final TVITagViewTour tviTour = (TVITagViewTour) treeItem;
				final TourData tourData = tourManager.getTourData(tviTour.getTourId());

				if (tourData != null) {
					selectedTourData.add(tourData);
				}
			}
		}

		return selectedTourData;
	}

	public TreeViewer getTreeViewer() {
		return fTagViewer;
	}

	@Override
	public void init(final IViewSite site, final IMemento memento) throws PartInitException {
		super.init(site, memento);

		// set the session memento if it's not yet set
		if (fSessionMemento == null) {
			fSessionMemento = memento;
		}
	}

	public boolean isFromTourEditor() {
		return false;
	}

	/**
	 * reload the content of the tag viewer
	 */
	public void reloadViewer() {
		fRootItem = new TVITagViewRoot(this);
		fTagViewer.setInput(fRootItem);
	}

	private void restoreState(final IMemento memento) {

		if (memento != null) {

			/*
			 * restore states from the memento
			 */

			// restore columns sort order
			final String mementoColumnSortOrderIds = memento.getString(MEMENTO_COLUMN_SORT_ORDER);
			if (mementoColumnSortOrderIds != null) {
				fColumnManager.orderColumns(StringToArrayConverter.convertStringToArray(mementoColumnSortOrderIds));
			}

			// restore column width
			final String mementoColumnWidth = memento.getString(MEMENTO_COLUMN_WIDTH);
			if (mementoColumnWidth != null) {
				fColumnManager.setColumnWidth(StringToArrayConverter.convertStringToArray(mementoColumnWidth));
			}
		}
	}

	@Override
	public void saveState(final IMemento memento) {

		// save column sort order
		memento.putString(MEMENTO_COLUMN_SORT_ORDER,
				StringToArrayConverter.convertArrayToString(fColumnManager.getColumnIds()));

		// save columns width
		final String[] columnIdAndWidth = fColumnManager.getColumnIdAndWidth();
		if (columnIdAndWidth != null) {
			memento.putString(MEMENTO_COLUMN_WIDTH, StringToArrayConverter.convertArrayToString(columnIdAndWidth));
		}
	}

	@Override
	public void setFocus() {

	}

	private void setTagViewTitle(final Object newInput) {

		String description = UI.EMPTY_STRING;

		if (newInput instanceof TVITagViewTag) {
			description = "Tag: " + ((TVITagViewTag) newInput).getName();
		} else if (newInput instanceof TVITagViewTagCategory) {
			description = "Category: " + ((TVITagViewTagCategory) newInput).name;
		}

		setContentDescription(description);
	}

	/**
	 * !!! Recursive !!! method to update the tags in the viewer
	 * 
	 * @param rootItem
	 * @param changedTags
	 * @param isAddMode
	 */
	private void updateViewer(final TreeViewerItem parentItem, final ChangedTags changedTags, final boolean isAddMode) {

		final ArrayList<TreeViewerItem> children = parentItem.getUnfetchedChildren();

		if (children == null) {
			return;
		}

		// loop: all children of the current parent item 
		for (final Object object : children) {

			if (object instanceof TVITagViewTag) {

				final TVITagViewTag tagItem = (TVITagViewTag) object;
				final long viewerTagId = tagItem.getTagId();

				final HashMap<Long, TourTag> modifiedTags = changedTags.getModifiedTags();
				final ArrayList<Long> removedIds = new ArrayList<Long>();

				for (final Long modifiedTagId : modifiedTags.keySet()) {
					if (viewerTagId == modifiedTagId.longValue()) {

						/*
						 * current tag was modified
						 */

						// add/remove tours from the tag
						tagItem.refresh(fTagViewer, changedTags.getModifiedTours(), changedTags.isAddMode());

						// update tag totals
						TVITagViewItem.getTagTotals(tagItem);

						// update viewer
						fTagViewer.refresh(tagItem);

						removedIds.add(modifiedTagId);
					}
				}

				/*
				 * modified tag id exists only once in the tree viewer, remove the id's outside of
				 * the foreach loop to avid the exception ConcurrentModificationException
				 */
				for (final Long removedId : removedIds) {
					modifiedTags.remove(removedId);
				}

				// optimize
				if (modifiedTags.size() == 0) {
					return;
				}

			} else {
				if (object instanceof TreeViewerItem) {
					updateViewer((TreeViewerItem) object, changedTags, isAddMode);
				}
			}
		}
	}

}
