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

import net.tourbook.common.UI;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.map3.Activator;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.part.ViewPart;

public class Map3PropertiesView extends ViewPart {

	public static final String			ID				= "net.tourbook.map3.Map3PropertiesView";					//$NON-NLS-1$

	private ContainerCheckedTreeViewer	_propViewer;

	private Image						_imgLayerProp	= Activator.getImageDescriptor(
																Messages.Image_Map3Property_Layer).createImage();

	private TVIMap3Root					_rootItem		= new TVIMap3Root();

	private boolean						_isTreeChecked;

	private PixelConverter				_pc;

	private PropertyViewerToolTip		_propViewerToolTip;

	private class PropertiesContentProvider implements ITreeContentProvider {

		@Override
		public void dispose() {}

		@Override
		public Object[] getChildren(final Object parentElement) {
			return ((TreeViewerItem) parentElement).getFetchedChildrenAsArray();
		}

		@Override
		public Object[] getElements(final Object inputElement) {
			return _rootItem.getFetchedChildrenAsArray();
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

	@Override
	public void createPartControl(final Composite parent) {

		createUI(parent);

		Map3Manager.setMap3PropertiesView(this);

		_propViewer.setInput(new Object());
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

			tree = new Tree(layoutContainer, SWT.H_SCROLL
					| SWT.V_SCROLL
					| SWT.BORDER
					| SWT.MULTI
					| SWT.FULL_SELECTION
					| SWT.CHECK);

			tree.setHeaderVisible(false);
			tree.setLinesVisible(false);

			/**
			 * Tree selection listener must be set BEVORE the tree viewer is created, this ensures
			 * that this listener is called before the viewer listeners.
			 * <p>
			 * When checking is not handled in this way, a user can check a tree item which is not
			 * selected and the selected tree item visibility is toggled !!!
			 */

			tree.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(final SelectionEvent e) {
					_isTreeChecked = e.detail == SWT.CHECK;

					if (_isTreeChecked) {

						final Object itemData = e.item.getData();
						if (itemData instanceof TVIMap3Layer) {

							final TVIMap3Layer layerItem = (TVIMap3Layer) itemData;
							toggleLayerVisibility(layerItem);
						}

					}
				}
			});

			/*
			 * tree viewer
			 */
			_propViewer = new ContainerCheckedTreeViewer(tree);

			_propViewer.setContentProvider(new PropertiesContentProvider());
			_propViewer.setUseHashlookup(true);

			_propViewer.addSelectionChangedListener(new ISelectionChangedListener() {
				public void selectionChanged(final SelectionChangedEvent event) {

					if (_isTreeChecked) {

						// checking is handled in the check state listener

						return;
					}

					final IStructuredSelection selection = (IStructuredSelection) _propViewer.getSelection();

					final Object firstItem = selection.getFirstElement();
					if (firstItem instanceof TVIMap3Item) {

						final TVIMap3Item item = (TVIMap3Item) firstItem;

						System.out.println(UI.timeStampNano() + " selectionChanged\t" + item);
						// TODO remove SYSTEM.OUT.PRINTLN

						if (item.hasChildren()) {

							// expand collapse item

							if (_propViewer.getExpandedState(firstItem)) {

								_propViewer.collapseToLevel(firstItem, 1);

							} else {

								_propViewer.expandToLevel(firstItem, 1);

								// expand event is not fired, set state manually
								onExpandTree(item);
							}

						} else if (firstItem instanceof TVIMap3Layer) {

							// toggle layer visibility

							final TVIMap3Layer layerItem = (TVIMap3Layer) firstItem;

							final boolean isEnabled = toggleLayerVisibility(layerItem);

							// update viewer
							_propViewer.setChecked(layerItem, isEnabled);
						}
					}
				}
			});

			_propViewer.addTreeListener(new ITreeViewerListener() {

				@Override
				public void treeCollapsed(final TreeExpansionEvent event) {
					// TODO Auto-generated method stub

				}

				@Override
				public void treeExpanded(final TreeExpansionEvent event) {

					System.out.println(UI.timeStampNano() + " treeExpanded\t");
					// TODO remove SYSTEM.OUT.PRINTLN

					onExpandTree((TVIMap3Item) event.getElement());
				}
			});

			_propViewer.addCheckStateListener(new ICheckStateListener() {

				@Override
				public void checkStateChanged(final CheckStateChangedEvent event) {
					onCheckTreeItem(event);
				}
			});

			tree.addKeyListener(new KeyListener() {

				public void keyPressed(final KeyEvent e) {

					/*
					 * toggle the visibility with the space key
					 */
					if (e.keyCode == ' ') {
//						toggleMapVisibility(tree);
					}
				}

				public void keyReleased(final KeyEvent e) {}
			});

		}

		defineAllColumn(treeLayout);

		_propViewerToolTip = new PropertyViewerToolTip(_propViewer);

		return layoutContainer;
	}

	private void defineAllColumn(final TreeColumnLayout treeLayout) {

		defineColumn_CategoryLayer(treeLayout);
		defineColumn_Info(treeLayout);
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
//		tc.setText("");
//		tc.setToolTipText(Messages.Dialog_MapProfile_Column_MapProvider_Tooltip);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				if (element instanceof TVIMap3Item) {

					final TVIMap3Item mapItem = (TVIMap3Item) element;

					cell.setText(mapItem.name);
				}

				if (element instanceof TVIMap3Category) {

					final TVIMap3Category propCategory = (TVIMap3Category) element;

					if (propCategory.id == Map3Manager.WW_DEFAULT_LAYER_ID) {
						cell.setImage(_imgLayerProp);
					} else {
						cell.setImage(null);
					}
				}

			}
		});
		treeLayout.setColumnData(tc, new ColumnWeightData(100, true));
	}

	/**
	 * Create columns for the tree viewer.
	 */
	private void defineColumn_Info(final TreeColumnLayout treeLayout) {

		TreeViewerColumn tvc;
		TreeColumn tc;

		/*
		 * column: tooltip
		 */
		tvc = new TreeViewerColumn(_propViewer, SWT.LEAD);
		tc = tvc.getColumn();
//		tc.setText("i");
//		tc.setToolTipText(Messages.Dialog_MapProfile_Column_MapProvider_Tooltip);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				if (element instanceof TVIMap3Item) {

					final TVIMap3Item mapItem = (TVIMap3Item) element;

					cell.setText(mapItem.name);
				}

				if (element instanceof TVIMap3Category) {

					final TVIMap3Category propCategory = (TVIMap3Category) element;

					if (propCategory.id == Map3Manager.WW_DEFAULT_LAYER_ID) {
						cell.setImage(_imgLayerProp);
					} else {
						cell.setImage(null);
					}
				}

			}
		});
		treeLayout.setColumnData(tc, new ColumnPixelData(_pc.convertWidthInCharsToPixels(11), false));
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

	private void onCheckTreeItem(final CheckStateChangedEvent event) {

		final boolean isChecked = event.getChecked();
		final Object element = event.getElement();

		System.out.println(UI.timeStampNano() + " onCheckTreeItem\t" + element + "\t" + isChecked);
		// TODO remove SYSTEM.OUT.PRINTLN

	}

	private void onExpandTree(final TVIMap3Item element) {

		// ensure check state is set
		if (element instanceof TVIMap3Category) {
			((TVIMap3Category) element).setCheckState();
		}
	}

	@Override
	public void setFocus() {
		_propViewer.getTree().setFocus();
	}

	private boolean toggleLayerVisibility(final TVIMap3Layer layerItem) {

		// toggle state
		final boolean isEnabled = !layerItem.layer.isEnabled();

		// update model
		layerItem.isEnabled = isEnabled;

		// update layer
		layerItem.layer.setEnabled(isEnabled);

		// redraw map
		Map3Manager.getWWCanvas().redraw();

		return isEnabled;
	}
}
