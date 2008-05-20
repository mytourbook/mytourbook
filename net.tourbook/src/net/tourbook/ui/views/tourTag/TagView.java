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

import net.tourbook.data.TourData;
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

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
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

	static public final String		ID	= "net.tourbook.views.tourListView";	//$NON-NLS-1$

	private static IMemento			fSessionMemento;

	public NumberFormat				fNF	= NumberFormat.getNumberInstance();

	private Composite				fViewerContainer;

	private TreeViewer				fTagViewer;
	private ColumnManager			fColumnManager;

	private PostSelectionProvider	fPostSelectionProvider;

	TVITagRoot						fRootItem;

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

	private void createContextMenu() {

	}

	@Override
	public void createPartControl(final Composite parent) {

		fViewerContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(fViewerContainer);

		createTourViewer(fViewerContainer);
//		createActions();

		// set selection provider
		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		// update the viewer
		fRootItem = new TVITagRoot(this);
		fTagViewer.setInput(this);
	}

	private Control createTourViewer(final Composite parent) {

		// tour tree
		final Tree tree = new Tree(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FLAT | SWT.FULL_SELECTION | SWT.MULTI);

		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tree.setHeaderVisible(true);
		tree.setLinesVisible(false);

		fTagViewer = new TreeViewer(tree);

		// define and create all columns
		fColumnManager = new ColumnManager(this);
		defineAllColumns(parent);
		fColumnManager.createColumns();

		fTagViewer.setContentProvider(new TagContentProvider());
		fTagViewer.setUseHashlookup(true);

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
	private void defineAllColumns(final Composite parent) {

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
				final TVITagItem tagItem = (TVITagItem) element;
				cell.setText(tagItem.fTreeColumn);
			}
		});

//		/*
//		 * column: date
//		 */
//		colDef = TreeColumnFactory.DATE.createColumn(fColumnManager, pixelConverter);
//		colDef.setCanModifyVisibility(false);
//		colDef.setLabelProvider(new CellLabelProvider() {
//			@Override
//			public void update(final ViewerCell cell) {
//				final Object element = cell.getElement();
//				final TourBookTreeViewerItem tourItem = (TourBookTreeViewerItem) element;
//				cell.setText(Long.toString(tourItem.fFirstColumn));
//			}
//		});
//		
//		/*
//		 * column: distance (km/miles)
//		 */
//		colDef = TreeColumnFactory.DISTANCE.createColumn(fColumnManager, pixelConverter);
//		colDef.setLabelProvider(new CellLabelProvider() {
//			@Override
//			public void update(final ViewerCell cell) {
//				final Object element = cell.getElement();
//				final TourBookTreeViewerItem tourItem = (TourBookTreeViewerItem) element;
//				fNF.setMinimumFractionDigits(1);
//				fNF.setMaximumFractionDigits(1);
//				cell.setText(fNF.format(((float) tourItem.fColumnDistance) / 1000 / UI.UNIT_VALUE_DISTANCE));
//			}
//		});
//
//		/*
//		 * column: tour type
//		 */
//		colDef = TreeColumnFactory.TOUR_TYPE.createColumn(fColumnManager, pixelConverter);
////		colDef.setColumnResizable(false);
//		colDef.setLabelProvider(new CellLabelProvider() {
//			@Override
//			public void update(final ViewerCell cell) {
//				final Object element = cell.getElement();
//				if (element instanceof TVITourBookTour) {
//					cell.setImage(UI.getInstance().getTourTypeImage(((TVITourBookTour) element).getTourTypeId()));
//				}
//			}
//		});

	}

	protected void enableActions() {

	}

	public ColumnManager getColumnManager() {
		return null;
	}

	public ArrayList<TourData> getSelectedTours() {
		return null;
	}

	public TreeViewer getTreeViewer() {
		return null;
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

	@Override
	public void setFocus() {

	}

}
