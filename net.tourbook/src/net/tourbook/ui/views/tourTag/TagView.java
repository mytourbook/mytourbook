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

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourTagCategory;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tag.TVIPrefTag;
import net.tourbook.tag.TVIPrefTagCategory;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TreeViewerItem;
import net.tourbook.ui.ActionExpandSelection;
import net.tourbook.ui.ActionRefreshView;
import net.tourbook.ui.ActionSetTreeExpandType;
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
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

public class TagView extends ViewPart implements ISelectedTours, ITourViewer {

	static public final String		ID							= "net.tourbook.views.tagViewID";				//$NON-NLS-1$

	private static final String		MEMENTO_COLUMN_SORT_ORDER	= "tagview.column_sort_order";					//$NON-NLS-1$
	private static final String		MEMENTO_COLUMN_WIDTH		= "tagview.column_width";						//$NON-NLS-1$

	public static final String		STATEMENT_TOURDATA_TOURTAG	= "tourData_tourTag";

	private static IMemento			fSessionMemento;

	private NumberFormat			fNF							= NumberFormat.getNumberInstance();
	private DateFormat				fDF							= DateFormat.getDateInstance(DateFormat.SHORT);

	private Composite				fViewerContainer;

	private TreeViewer				fTagViewer;
	private ColumnManager			fColumnManager;

	private PostSelectionProvider	fPostSelectionProvider;
	private IPropertyChangeListener	fPrefChangeListener;

	TVITagViewRoot					fRootItem;

	private ActionRefreshView		fActionRefreshView;
	private ActionSetTreeExpandType	fActionSetTreeExpandType;

	private Image					fImgTagCategory				= TourbookPlugin.getImageDescriptor(Messages.Image__tag_category)
																		.createImage();
	private Image					fImgTag						= TourbookPlugin.getImageDescriptor(Messages.Image__tag)
																		.createImage();

	private class TagContentProvider implements ITreeContentProvider {

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
	 * Sort tags and categories
	 */
	private class TagViewerSorter extends ViewerSorter {

		@Override
		public int compare(final Viewer viewer, final Object obj1, final Object obj2) {

			if (obj1 instanceof TVIPrefTag && obj2 instanceof TVIPrefTag) {

				final TourTag tourTag1 = ((TVIPrefTag) (obj1)).getTourTag();
				final TourTag tourTag2 = ((TVIPrefTag) (obj2)).getTourTag();

				return tourTag1.getTagName().compareTo(tourTag2.getTagName());

			} else if (obj1 instanceof TVIPrefTag && obj2 instanceof TVIPrefTagCategory) {

				return 1;

			} else if (obj2 instanceof TVIPrefTag && obj1 instanceof TVIPrefTagCategory) {

				return -1;

			} else if (obj1 instanceof TVIPrefTagCategory && obj2 instanceof TVIPrefTagCategory) {

				final TourTagCategory tourTagCat1 = ((TVIPrefTagCategory) (obj1)).getTourTagCategory();
				final TourTagCategory tourTagCat2 = ((TVIPrefTagCategory) (obj2)).getTourTagCategory();

				return tourTagCat1.getCategoryName().compareTo(tourTagCat2.getCategoryName());
			}

			return 0;
		}
	}

	private void addPrefListener() {

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(final Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {

					reloadViewer();

				}
			}
		};

		// register the listener
		TourbookPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(fPrefChangeListener);
	}

	private void createActions() {

		fActionRefreshView = new ActionRefreshView(this);
		fActionSetTreeExpandType = new ActionSetTreeExpandType(this);

		/*
		 * action in the view toolbar
		 */
		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

		tbm.add(new ActionExpandSelection(fTagViewer));
		tbm.add(new ActionCollapseAll(fTagViewer));

		tbm.add(fActionRefreshView);
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

		addPrefListener();

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

		// define and create all columns
		fColumnManager = new ColumnManager(this);
		createTagViewerColumns(parent);
		fColumnManager.createColumns();

		fTagViewer.setContentProvider(new TagContentProvider());
		fTagViewer.setUseHashlookup(true);
		fTagViewer.setSorter(new TagViewerSorter());

		fTagViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {

				final Object selectedItem = ((IStructuredSelection) (event.getSelection())).getFirstElement();

				if (selectedItem instanceof TVITagViewTour) {
					final TVITagViewTour tourItem = (TVITagViewTour) selectedItem;
					fPostSelectionProvider.setSelection(new SelectionTourId(tourItem.tourId));
				}

				enableActions();
			}
		});

		fTagViewer.addDoubleClickListener(new IDoubleClickListener() {

			public void doubleClick(final DoubleClickEvent event) {

				final Object selection = ((IStructuredSelection) fTagViewer.getSelection()).getFirstElement();

				if (selection instanceof TVITagViewTour) {

					// open tour in the tour editor

					TourManager.getInstance().openTourInEditor(((TVITagViewTour) selection).tourId);

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
		 * first column
		 */
		colDef = TreeColumnFactory.TAG.createColumn(fColumnManager, pixelConverter);
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final TVITagViewItem viewItem = (TVITagViewItem) element;

				if (viewItem instanceof TVITagViewTour) {

					final TVITagViewTour tagItem = (TVITagViewTour) viewItem;
					cell.setText(fDF.format(tagItem.tourDate.toDate()));
					cell.setImage(UI.getInstance().getTourTypeImage(tagItem.tourTypeId));

				} else if (viewItem instanceof TVITagViewTag) {

					cell.setText(viewItem.treeColumn);
					cell.setImage(fImgTag);

				} else if (viewItem instanceof TVITagViewTagCategory) {

					cell.setText(viewItem.treeColumn);
					cell.setImage(fImgTagCategory);
					
				} else {
					cell.setText(viewItem.treeColumn);
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

				if (element instanceof TVITagViewTour) {
					cell.setText(((TVITagViewTour) element).tourTitle);
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
				fNF.setMinimumFractionDigits(1);
				fNF.setMaximumFractionDigits(1);
				cell.setText(fNF.format(((float) treeItem.colDistance) / 1000 / UI.UNIT_VALUE_DISTANCE));
			}
		});

	}

	@Override
	public void dispose() {

		TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);

		fImgTagCategory.dispose();
		fImgTag.dispose();

		super.dispose();
	}

	private void enableActions() {

		final StructuredSelection selection = (StructuredSelection) fTagViewer.getSelection();
		final int selectedItems = selection.size();

		boolean isTagSelected = false;
		if (selection.getFirstElement() instanceof TVITagViewTag) {
			isTagSelected = true;
		}

		/*
		 * tree expand type can be set only for one tag
		 */
		fActionSetTreeExpandType.setEnabled(isTagSelected && selectedItems == 1);
	}

	private void fillContextMenu(final IMenuManager menuMgr) {

		menuMgr.add(new Separator());
		menuMgr.add(fActionSetTreeExpandType);

		enableActions();
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
				final TourData tourData = tourManager.getTourData(tviTour.tourId);

				if (tourData != null) {
					selectedTourData.add(tourData);
				}
			}
		}

		return selectedTourData;
	}

//	public PreparedStatement getStatement(final String statementTourdataTourtag) {
//		return fStatements.get(statementTourdataTourtag);
//	}

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
		fTagViewer.setInput(this);
	}

//	private void saveSettings() {
//		fSessionMemento = XMLMemento.createWriteRoot("TagView"); //$NON-NLS-1$
//		saveState(fSessionMemento);
//	}

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

//	public void setStatement(final String statementTourdataTourtag, final PreparedStatement statement) {
//		fStatements.put(statementTourdataTourtag, statement);
//	}

}
