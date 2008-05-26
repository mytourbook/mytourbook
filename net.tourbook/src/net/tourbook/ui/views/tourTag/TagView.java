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

import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourTagCategory;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tag.TVITourTag;
import net.tourbook.tag.TVITourTagCategory;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TreeViewerItem;
import net.tourbook.ui.ColumnManager;
import net.tourbook.ui.ISelectedTours;
import net.tourbook.ui.ITourViewer;
import net.tourbook.ui.TreeColumnDefinition;
import net.tourbook.ui.TreeColumnFactory;
import net.tourbook.ui.views.tourBook.TVITourBookTour;
import net.tourbook.util.PixelConverter;
import net.tourbook.util.PostSelectionProvider;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

public class TagView extends ViewPart implements ISelectedTours, ITourViewer {

	static public final String		ID	= "net.tourbook.views.tagViewID";				//$NON-NLS-1$

	private static IMemento			fSessionMemento;

	private NumberFormat			fNF	= NumberFormat.getNumberInstance();
	private DateFormat				fDF	= DateFormat.getDateInstance(DateFormat.SHORT);

	private Composite				fViewerContainer;

	private TreeViewer				fTagViewer;
	private ColumnManager			fColumnManager;

	private PostSelectionProvider	fPostSelectionProvider;
	private IPropertyChangeListener	fPrefChangeListener;

	TVITagViewRoot					fRootItem;

	private ActionRefreshView		fActionRefreshView;

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
	 * Sort the tags and categories
	 */
	private class TagViewerSorter extends ViewerSorter {

		@Override
		public int compare(final Viewer viewer, final Object obj1, final Object obj2) {

			if (obj1 instanceof TVITourTag && obj2 instanceof TVITourTag) {

				final TourTag tourTag1 = ((TVITourTag) (obj1)).getTourTag();
				final TourTag tourTag2 = ((TVITourTag) (obj2)).getTourTag();

				return tourTag1.getTagName().compareTo(tourTag2.getTagName());

			} else if (obj1 instanceof TVITourTag && obj2 instanceof TVITourTagCategory) {

				return 1;

			} else if (obj2 instanceof TVITourTag && obj1 instanceof TVITourTagCategory) {

				return -1;

			} else if (obj1 instanceof TVITourTagCategory && obj2 instanceof TVITourTagCategory) {

				final TourTagCategory tourTagCat1 = ((TVITourTagCategory) (obj1)).getTourTagCategory();
				final TourTagCategory tourTagCat2 = ((TVITourTagCategory) (obj2)).getTourTagCategory();

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

					reloadTagViewer();

				}
			}
		};

		// register the listener
		TourbookPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(fPrefChangeListener);
	}

	private void createActions() {

		fActionRefreshView = new ActionRefreshView(this);

		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

		tbm.add(fActionRefreshView);
	}

	private void createContextMenu() {

	}

	@Override
	public void createPartControl(final Composite parent) {

		fViewerContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(fViewerContainer);

		createTagViewer(fViewerContainer);
		createActions();

		// set selection provider
		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		addPrefListener();

		reloadTagViewer();
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

				enableActions();
			}
		});

		fTagViewer.addDoubleClickListener(new IDoubleClickListener() {

			public void doubleClick(final DoubleClickEvent event) {

				final Object selection = ((IStructuredSelection) fTagViewer.getSelection()).getFirstElement();

				if (selection instanceof TVITourBookTour) {

					// open tour in editor

					final TVITourBookTour tourItem = (TVITourBookTour) selection;
					TourManager.getInstance().openTourInEditor(tourItem.getTourId());

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

		createContextMenu();

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
		 * column: tag
		 */
		colDef = TreeColumnFactory.TAG.createColumn(fColumnManager, pixelConverter);
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final TVITagViewItem tagItem = (TVITagViewItem) element;

				if (tagItem instanceof TVITagViewTour) {
					cell.setText(fDF.format(((TVITagViewTour) tagItem).tourDate.toDate()));
//					cell.setText(tagItem.treeColumn);
				} else {
					cell.setText(tagItem.treeColumn);
				}
			}
		});

	}

	@Override
	public void dispose() {

		TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);

		super.dispose();
	}

	protected void enableActions() {

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

		final ArrayList<TourData> selectedTourData = new ArrayList<TourData>();

//		// loop: all selected tours
//		for (final Iterator<?> iter = selectedTours.iterator(); iter.hasNext();) {
//
//			final Object treeItem = iter.next();
//
//			if (treeItem instanceof TVITourBookTour) {
//
//				final TVITourBookTour tviTour = ((TVITourBookTour) treeItem);
//
//				final TourData tourData = TourManager.getInstance().getTourData(tviTour.getTourId());
//
//				if (tourData != null) {
//					selectedTourData.add(tourData);
//				}
//			}
//		}

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
	void reloadTagViewer() {
		fRootItem = new TVITagViewRoot(this);
		fTagViewer.setInput(this);
	}

	@Override
	public void setFocus() {

	}

}
