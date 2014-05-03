/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
package net.tourbook.map3.ui;

import gov.nasa.worldwind.layers.Layer;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;

import net.tourbook.common.UI;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.map3.Messages;
import net.tourbook.map3.view.Map3Manager;
import net.tourbook.map3.view.TVIMap3Category;
import net.tourbook.map3.view.TVIMap3Item;
import net.tourbook.map3.view.TVIMap3Layer;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ICellEditorListener;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerRow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;

import cop.swt.widgets.viewers.table.celleditors.RangeContent;
import cop.swt.widgets.viewers.table.celleditors.SpinnerCellEditor;

/**
 * UI for the map3 layers, to set visibility and opacity.
 */
public class Map3LayerUI {

	private static final String			OPACITY_CAN_NOT_BE_SET	= "....";

	public static final Double			DEFAULT_OPACITY			= new Double(1.0);

	private ContainerCheckedTreeViewer	_layerViewer;
	private DialogLayerViewerToolTip	_propToolTip;

	private OpacityEditingSupport		_opacityEditingSupport;
	private final RangeContent			_opacityRange			= new RangeContent(0.0, 1.0, 0.01, 100);
	private final NumberFormat			_nf2					= NumberFormat.getNumberInstance();
	{
		_nf2.setMinimumFractionDigits(2);
		_nf2.setMaximumFractionDigits(2);
	}

	/*
	 * UI resources
	 */
	private PixelConverter				_pc;

	/*
	 * UI controls
	 */

	private class LayerContentProvider implements ITreeContentProvider {

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

	private final class OpacityEditingSupport extends EditingSupport {

		private final TreeViewer		_treeViewer;
		private TVIMap3Layer			_currentLayerItem;

		private final SpinnerCellEditor	_cellEditor;

		private OpacityEditingSupport(final TreeViewer treeViewer) {

			super(treeViewer);

			_treeViewer = treeViewer;

			_cellEditor = new SpinnerCellEditor(treeViewer.getTree(), _nf2, _opacityRange, SWT.BORDER);

			_cellEditor.addListener(new ICellEditorListener() {

				@Override
				public void applyEditorValue() {}

				@Override
				public void cancelEditor() {}

				@Override
				public void editorValueChanged(final boolean oldValidState, final boolean newValidState) {
					onSelectOpacity();
				}
			});
		}

		@Override
		protected boolean canEdit(final Object element) {

			if (element instanceof TVIMap3Layer) {

				final TVIMap3Layer layerItem = (TVIMap3Layer) element;

				return layerItem.canSetOpacity();
			}

			return false;
		}

		@Override
		protected CellEditor getCellEditor(final Object element) {
			return _cellEditor;
		}

		@Override
		protected Object getValue(final Object element) {

			if (element instanceof TVIMap3Layer) {

				final TVIMap3Layer layerItem = (TVIMap3Layer) element;

				// keep current layer
				_currentLayerItem = layerItem;

				final Double opacity = layerItem.canSetOpacity() //
						? Double.valueOf(layerItem.getOpacity())
						: DEFAULT_OPACITY;

				return opacity;
			}

			return DEFAULT_OPACITY;
		}

		boolean isEditorActive() {
			return _cellEditor.isActivated();
		}

		/**
		 * This is a very complex hack to get modified spinner values and update the map
		 * immediately.
		 */
		private void onSelectOpacity() {

			final Object editorValue = _cellEditor.getValue();

			updateUIAndModel(_currentLayerItem, editorValue);
		}

		@Override
		protected void setValue(final Object element, final Object value) {

			if (element instanceof TVIMap3Layer) {

				final TVIMap3Layer layerItem = (TVIMap3Layer) element;

				updateUIAndModel(layerItem, value);
			}
		}

		private void updateUIAndModel(final TVIMap3Layer layerItem, final Object value) {

			final double newOpacity = (Double) value;
			final float newOpacityFloat = (float) newOpacity;

			// update model
			layerItem.setOpacity(newOpacityFloat);

			// update UI
			_treeViewer.update(layerItem, null);
			Map3Manager.redrawMap();
		}
	}

	public Map3LayerUI(final Composite parent) {

		initUI(parent);

		createUI(parent);

		// restore layers
		_layerViewer.setInput(new Object());

		restoreState();
	}

	private void createUI(final Composite parent) {

		createUI_10_LayerViewer(parent);

		parent.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});
	}

	private Control createUI_10_LayerViewer(final Composite parent) {

		final TreeColumnLayout treeLayout = new TreeColumnLayout();

		final Composite layoutContainer = new Composite(parent, SWT.NONE);
		layoutContainer.setLayout(treeLayout);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.hint(_pc.convertWidthInCharsToPixels(45), SWT.DEFAULT)
				.applyTo(layoutContainer);

		Tree tree;
		{
			tree = new Tree(layoutContainer, //
					SWT.H_SCROLL //
							| SWT.V_SCROLL
//							| SWT.BORDER
//							| SWT.MULTI
							| SWT.FULL_SELECTION
							| SWT.CHECK);

			tree.setHeaderVisible(true);
			tree.setLinesVisible(false);

//			tree.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));

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
			_layerViewer = new ContainerCheckedTreeViewer(tree);

			_layerViewer.setContentProvider(new LayerContentProvider());
			_layerViewer.setUseHashlookup(true);

			_layerViewer.addDoubleClickListener(new IDoubleClickListener() {
				@Override
				public void doubleClick(final DoubleClickEvent event) {

					final IStructuredSelection selection = (IStructuredSelection) _layerViewer.getSelection();

					final Object firstItem = selection.getFirstElement();
					if (firstItem instanceof TVIMap3Layer) {

						toggleLayerVisibility((TVIMap3Layer) firstItem, true, false);
					}
				}
			});

			_layerViewer.addTreeListener(new ITreeViewerListener() {

				@Override
				public void treeCollapsed(final TreeExpansionEvent event) {}

				@Override
				public void treeExpanded(final TreeExpansionEvent event) {
					onExpandTree((TVIMap3Item) event.getElement());
				}
			});

			_layerViewer.addCheckStateListener(new ICheckStateListener() {
				@Override
				public void checkStateChanged(final CheckStateChangedEvent event) {
					onChangeCheckState(event);
				}
			});
		}

		defineAllColumn(treeLayout);

		// hide default tooltip and display the custom tooltip
		tree.setToolTipText(UI.EMPTY_STRING);

		_propToolTip = new DialogLayerViewerToolTip(_layerViewer);

		return layoutContainer;
	}

	private void defineAllColumn(final TreeColumnLayout treeLayout) {

		defineColumn_CategoryLayer(treeLayout);
		defineColumn_Opacity(treeLayout);
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
		tvc = new TreeViewerColumn(_layerViewer, SWT.LEAD);
		tc = tvc.getColumn();
		tc.setText(Messages.Map3Layer_Viewer_Column_Layer);

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

	/**
	 * column: marker
	 */
	private void defineColumn_Opacity(final TreeColumnLayout treeLayout) {

		final TreeViewerColumn tvc = new TreeViewerColumn(_layerViewer, SWT.CENTER);
		final TreeColumn tc = tvc.getColumn();

		tc.setText(Messages.Map3Layer_Viewer_Column_Opacity);
		tc.setToolTipText(Messages.Map3Layer_Viewer_Column_Opacity_Tooltip);

		_opacityEditingSupport = new OpacityEditingSupport(_layerViewer);
		tvc.setEditingSupport(_opacityEditingSupport);

		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				if (element instanceof TVIMap3Layer) {

					final TVIMap3Layer layerItem = (TVIMap3Layer) element;

					final float opacity = layerItem.getOpacity();

					final String opacityText;
					if (layerItem.canSetOpacity()) {

						if (layerItem.isLayerVisible) {

							if (opacity == 1.0) {
								opacityText = UI.SYMBOL_FULL_BLOCK;
							} else {
								opacityText = _nf2.format(opacity);
							}
						} else {

							// layer is hidden
							opacityText = UI.EMPTY_STRING;
						}
					} else {

						// opacity cannot be set
						opacityText = OPACITY_CAN_NOT_BE_SET;
					}

					cell.setText(opacityText);
				}
			}
		});
		treeLayout.setColumnData(tc, new ColumnPixelData(_pc.convertWidthInCharsToPixels(8), false));
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);
	}

	private void onChangeCheckState(final CheckStateChangedEvent event) {

		final Object viewerItem = event.getElement();

		if (viewerItem instanceof TVIMap3Layer) {

			toggleLayerVisibility((TVIMap3Layer) viewerItem, false, false);

		} else if (viewerItem instanceof TVIMap3Category) {

			final TVIMap3Category tviCategory = (TVIMap3Category) viewerItem;

			final ArrayList<TreeViewerItem> children = tviCategory.getUnfetchedChildren();
			if (children == null) {
				return;
			}

			final boolean isCategoryVisible = event.getChecked();

			boolean isMapModified = false;

			for (final TreeViewerItem childItem : children) {

				if (childItem instanceof TVIMap3Layer) {

					final TVIMap3Layer tviLayer = (TVIMap3Layer) childItem;

					final boolean isLayerVisible = tviLayer.wwLayer.isEnabled();

					if (isCategoryVisible == isLayerVisible) {

						// layer has the correct visibility
						continue;
					}

					// force map redraw
					isMapModified = true;

					// update model
					tviLayer.isLayerVisible = isCategoryVisible;

					// update ww layer
					tviLayer.wwLayer.setEnabled(isCategoryVisible);

					// fire check state listener
					tviLayer.fireCheckStateListener();

					// update tooltip
					_propToolTip.setLayerVisibility(tviLayer, false);

					// update viewer
					_layerViewer.update(tviLayer, null);
				}
			}

			// redraw map
			if (isMapModified) {
				Map3Manager.redrawMap();
			}
		}
	}

	private void onDispose() {

		saveState();
	}

	private void onExpandTree(final TVIMap3Item element) {

		// ensure check state is set
		if (element instanceof TVIMap3Category) {
			((TVIMap3Category) element).setCheckState();
		}
	}

	private void onSelectTreeItem() {

		// ignore mouse when cell editor is active
		if (_opacityEditingSupport.isEditorActive()) {
			return;
		}

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

				if (_layerViewer.getExpandedState(hoveredItem)) {

					_layerViewer.collapseToLevel(hoveredItem, 1);

				} else {

					_layerViewer.expandToLevel(hoveredItem, 1);

					// expand event is not fired, set state manually
					onExpandTree(mapItem);
				}

			} else if (mapItem instanceof TVIMap3Layer) {

				// toggle layer visibility

				toggleLayerVisibility((TVIMap3Layer) mapItem, true, true);
			}
		}

	}

	private void restoreState() {

		// restore UI
		final Object[] uiVisibleLayers = Map3Manager.getUIVisibleLayers();
		final Object[] uiExpandedCategories = Map3Manager.getUIExpandedCategories();

		_layerViewer.setCheckedElements(uiVisibleLayers);
		_layerViewer.setExpandedElements(uiExpandedCategories);

		// inform layer about check state modification
		for (final Object object : uiVisibleLayers) {
			if (object instanceof TVIMap3Layer) {

				((TVIMap3Layer) object).fireCheckStateListener();
			}
		}
	}

	private void saveState() {

		// save/keep UI state
		Map3Manager.saveUIState(_layerViewer.getCheckedElements(), _layerViewer.getExpandedElements());
	}

	public void setLayerVisible(final TVIMap3Layer tviLayer, final boolean isVisible) {

		// update viewer
		_layerViewer.setChecked(tviLayer, isVisible);
	}

	public void setLayerVisible_TourTrack(final TVIMap3Layer tviLayer, final boolean isTrackVisible) {

		setLayerVisible_TourTrack(tviLayer, isTrackVisible, true, false);
	}

	/**
	 * @param tviLayer
	 * @param isLayerVisible
	 * @param isUpdateViewer
	 * @param isUpdateTooltip
	 */
	private void setLayerVisible_TourTrack(	final TVIMap3Layer tviLayer,
											final boolean isLayerVisible,
											final boolean isUpdateViewer,
											final boolean isUpdateTooltip) {
		// update model
		tviLayer.isLayerVisible = isLayerVisible;

		final Layer wwLayer = tviLayer.wwLayer;

		// update ww layer
		wwLayer.setEnabled(isLayerVisible);

		// add/remove layer listener
		tviLayer.fireCheckStateListener();

		// redraw map
		Map3Manager.redrawMap();

		// update tooltip
		_propToolTip.setLayerVisibility(tviLayer, isUpdateTooltip);

		// update viewer
		if (isUpdateViewer) {
			_layerViewer.setChecked(tviLayer, isLayerVisible);
		}

		// check/uncheck actions in the map view
		Map3Manager.enableMap3Actions();
	}

	/**
	 * @param tviLayer
	 * @param isUpdateViewer
	 * @param isUpdateTooltip
	 */
	private void toggleLayerVisibility(	final TVIMap3Layer tviLayer,
										final boolean isUpdateViewer,
										final boolean isUpdateTooltip) {

		// toggle state
		final boolean isLayerVisible = !tviLayer.wwLayer.isEnabled();

		setLayerVisible_TourTrack(tviLayer, isLayerVisible, isUpdateViewer, isUpdateTooltip);

		// update viewer
		_layerViewer.update(tviLayer, null);
	}

	public void updateUI_NewLayer(final ArrayList<TVIMap3Layer> insertedLayers) {

		// get a set of unique parents
		final HashSet<TreeViewerItem> parentItems = new HashSet<TreeViewerItem>();
		for (final TVIMap3Layer tviMap3Layer : insertedLayers) {

			final TreeViewerItem parentItem = tviMap3Layer.getParentItem();

			parentItems.add(parentItem);
		}

		// update parent and all it's children
		for (final TreeViewerItem parentItem : parentItems) {
			_layerViewer.refresh(parentItem, false);
		}
	}

}
