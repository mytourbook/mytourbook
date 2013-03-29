/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
package net.tourbook.map3.view;

import java.util.ArrayList;
import java.util.HashSet;

import net.tourbook.common.UI;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.map3.Activator;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerRow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.part.ViewPart;

public class Map3PropertiesView extends ViewPart {

	public static final String			ID				= "net.tourbook.map3.Map3PropertiesView";					//$NON-NLS-1$

	private final IDialogSettings		_state			= Activator.getDefault().getDialogSettingsSection(ID);

	private ContainerCheckedTreeViewer	_propViewer;

	private Image						_imgLayerProp	= Activator.getImageDescriptor(
																Messages.Image_Map3_PropertyLayer).createImage();

	private PixelConverter				_pc;

	private IPartListener2				_partListener;

	private PropertyViewerToolTip		_propToolTip;

	private class PropertiesContentProvider implements ITreeContentProvider {

		@Override
		public void dispose() {}

		@Override
		public Object[] getChildren(final Object parentElement) {
			return ((TreeViewerItem) parentElement).getFetchedChildrenAsArray();
		}

		@Override
		public Object[] getElements(final Object inputElement) {
			return Map3Manager.getRootItem().getFetchedChildrenAsArray();
		}

		@Override
		public Object getParent(final Object element) {
			return ((TreeViewerItem) element).getParentItem();
		}

		@Override
		public boolean hasChildren(final Object element) {
			return ((TreeViewerItem) element).hasChildren();
		}

		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	public Map3PropertiesView() {}

	private void addPartListener() {

		_partListener = new IPartListener2() {
			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == Map3PropertiesView.this) {
					saveState();
				}
			}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {}

			@Override
			public void partVisible(final IWorkbenchPartReference partRef) {}
		};
		getViewSite().getPage().addPartListener(_partListener);
	}

	@Override
	public void createPartControl(final Composite parent) {

		createUI(parent);

		addPartListener();

		Map3Manager.setMap3PropertiesView(this);

		// restore layers
		_propViewer.setInput(new Object());

		restoreState();
	}

	private void createUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{
			createUI_10_PropViewer(container);
		}
	}

	private Control createUI_10_PropViewer(final Composite parent) {

		final TreeColumnLayout treeLayout = new TreeColumnLayout();

		final Composite layoutContainer = new Composite(parent, SWT.NONE);
		layoutContainer.setLayout(treeLayout);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(layoutContainer);
		Tree tree;
		{

			tree = new Tree(layoutContainer, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER
//					| SWT.MULTI
					| SWT.FULL_SELECTION
					| SWT.CHECK);

			tree.setHeaderVisible(false);
			tree.setLinesVisible(false);

			/**
			 * Tree selection listener must be set BEVORE the tree viewer is created, this ensures
			 * that this listener is called before the viewer listeners.
			 * <p>
			 * When checking is not handled in this way, a user can check a tree item which is not
			 * selected and the selected tree item layer visibility is toggled !!!
			 */

			tree.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseDown(final MouseEvent e) {
					onSelectTreeItem();
				}
			});

			/*
			 * tree viewer
			 */
			_propViewer = new ContainerCheckedTreeViewer(tree);

			_propViewer.setContentProvider(new PropertiesContentProvider());
			_propViewer.setUseHashlookup(true);

			_propViewer.addDoubleClickListener(new IDoubleClickListener() {
				@Override
				public void doubleClick(final DoubleClickEvent event) {

					final IStructuredSelection selection = (IStructuredSelection) _propViewer.getSelection();

					final Object firstItem = selection.getFirstElement();
					if (firstItem instanceof TVIMap3Layer) {

						toggleLayerVisibility((TVIMap3Layer) firstItem, true);
					}
				}
			});

			_propViewer.addTreeListener(new ITreeViewerListener() {

				@Override
				public void treeCollapsed(final TreeExpansionEvent event) {}

				@Override
				public void treeExpanded(final TreeExpansionEvent event) {
					onExpandTree((TVIMap3Item) event.getElement());
				}
			});

			_propViewer.addCheckStateListener(new ICheckStateListener() {

				@Override
				public void checkStateChanged(final CheckStateChangedEvent event) {

					final Object itemData = event.getElement();
					if (itemData instanceof TVIMap3Layer) {
						toggleLayerVisibility((TVIMap3Layer) itemData, false);
					}
				}
			});
		}

		defineAllColumn(treeLayout);

		// hide default tooltip and display the custom tooltip
		tree.setToolTipText(UI.EMPTY_STRING);

		_propToolTip = new PropertyViewerToolTip(_propViewer);

		return layoutContainer;
	}

	private void defineAllColumn(final TreeColumnLayout treeLayout) {

		defineColumn_CategoryLayer(treeLayout);
//		defineColumn_Info(treeLayout);
	}

	/**
	 * Create columns for the tree viewer.
	 */
	private void defineColumn_CategoryLayer(final TreeColumnLayout treeLayout) {

		TreeViewerColumn tvc;
		TreeColumn tc;

		/*
		 * column: category/layer
		 */
		tvc = new TreeViewerColumn(_propViewer, SWT.LEAD);
		tc = tvc.getColumn();
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				if (element instanceof TVIMap3Item) {

					final TVIMap3Item mapItem = (TVIMap3Item) element;

					cell.setText(mapItem.name);
				}
			}
		});
		treeLayout.setColumnData(tc, new ColumnWeightData(100, true));
	}

	@Override
	public void dispose() {

		_imgLayerProp.dispose();

		Map3Manager.setMap3PropertiesView(null);

		super.dispose();
	}

	ContainerCheckedTreeViewer getPropertiesViewer() {
		return _propViewer;
	}

	private void onExpandTree(final TVIMap3Item element) {

		// ensure check state is set
		if (element instanceof TVIMap3Category) {
			((TVIMap3Category) element).setCheckState();
		}
	}

	private void onSelectTreeItem() {

		/*
		 * the following actions will only be done, when the sensitive area of the row is hovered
		 * with the mouse
		 */
		final ViewerRow hoveredRow = _propToolTip.getHoveredRow();

		if (hoveredRow == null) {
			return;
		}

		final Object hoveredItem = hoveredRow.getElement();

		if (hoveredItem instanceof TVIMap3Item) {

			final TVIMap3Item mapItem = (TVIMap3Item) hoveredItem;

			if (mapItem.hasChildren()) {

				// expand collapse item

				if (_propViewer.getExpandedState(hoveredItem)) {

					_propViewer.collapseToLevel(hoveredItem, 1);

				} else {

					_propViewer.expandToLevel(hoveredItem, 1);

					// expand event is not fired, set state manually
					onExpandTree(mapItem);
				}

			} else if (mapItem instanceof TVIMap3Layer) {

				// toggle layer visibility

				toggleLayerVisibility((TVIMap3Layer) mapItem, true);
			}
		}

	}

	private void restoreState() {

		// restore UI
		final Object[] uiEnabledLayers = Map3Manager.getUIEnabledLayers();
		final Object[] uiExpandedCategories = Map3Manager.getUIExpandedCategories();

		_propViewer.setCheckedElements(uiEnabledLayers);
		_propViewer.setExpandedElements(uiExpandedCategories);

		// inform layer about check state modification
		for (final Object object : uiEnabledLayers) {
			if (object instanceof TVIMap3Layer) {
				final TVIMap3Layer tviLayer = (TVIMap3Layer) object;
				tviLayer.onSetCheckState();
			}
		}
	}

	private void saveState() {

		// save/keep UI state
		Map3Manager.saveUIState(_propViewer.getCheckedElements(), _propViewer.getExpandedElements());
	}

	@Override
	public void setFocus() {
		_propViewer.getTree().setFocus();
	}

	private void toggleLayerVisibility(final TVIMap3Layer tviLayer, final boolean isUpdateViewer) {

		// toggle state
		final boolean isLayerVisible = !tviLayer.wwLayer.isEnabled();

		// update model
		tviLayer.isLayerVisible = isLayerVisible;

		// update layer
		tviLayer.wwLayer.setEnabled(isLayerVisible);

		// add/remove layer listener
		tviLayer.onSetCheckState();

		// redraw map
		Map3Manager.getWWCanvas().redraw();

		// update tooltip
		_propToolTip.setLayerVisibility(tviLayer);

		if (isUpdateViewer) {

			// update viewer
			_propViewer.setChecked(tviLayer, isLayerVisible);
		}
	}

	void updateUINewLayer(final ArrayList<TVIMap3Layer> insertedLayers) {

		// get a set of unique parents
		final HashSet<TreeViewerItem> parentItems = new HashSet<TreeViewerItem>();
		for (final TVIMap3Layer tviMap3Layer : insertedLayers) {

			final TreeViewerItem parentItem = tviMap3Layer.getParentItem();

			parentItems.add(parentItem);
		}

		// update parent and all it's children
		for (final TreeViewerItem parentItem : parentItems) {
			_propViewer.refresh(parentItem, false);
		}
	}
}
