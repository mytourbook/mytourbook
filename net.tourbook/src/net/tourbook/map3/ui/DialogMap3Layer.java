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

import java.util.ArrayList;
import java.util.HashSet;

import net.tourbook.common.UI;
import net.tourbook.common.tooltip.AnimatedToolTipShell;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.map3.view.Map3Manager;
import net.tourbook.map3.view.TVIMap3Category;
import net.tourbook.map3.view.TVIMap3Item;
import net.tourbook.map3.view.TVIMap3Layer;

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
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;

/**
 * Map3 tour track layer properties dialog.
 */
public class DialogMap3Layer extends AnimatedToolTipShell {

	private static final int			SHELL_MARGIN		= 0;

	// initialize with default values which are (should) never be used
	private Rectangle					_toolTipItemBounds	= new Rectangle(0, 0, 50, 50);

	private final WaitTimer				_waitTimer			= new WaitTimer();

	private boolean						_canOpenToolTip;
	private boolean						_isWaitTimerStarted;

	private ContainerCheckedTreeViewer	_layerViewer;

	private DialogPropertyViewerToolTip	_propToolTip;

	private PixelConverter				_pc;

	/*
	 * UI resources
	 */

	/*
	 * UI controls
	 */
	private Composite					_shellContainer;


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

	private final class WaitTimer implements Runnable {
		@Override
		public void run() {
			open_Runnable();
		}
	}

	public DialogMap3Layer(final Control ownerControl, final ToolBar toolBar) {

		super(ownerControl);

		addListener(ownerControl, toolBar);

		setToolTipCreateStyle(AnimatedToolTipShell.TOOLTIP_STYLE_KEEP_CONTENT);
		setBehaviourOnMouseOver(AnimatedToolTipShell.MOUSE_OVER_BEHAVIOUR_IGNORE_OWNER);
		setIsKeepShellOpenWhenMoved(false);
		setFadeInSteps(1);
		setFadeOutSteps(10);
		setFadeOutDelaySteps(1);
	}

	private void addListener(final Control ownerControl, final ToolBar toolBar) {

		toolBar.addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseExit(final MouseEvent e) {

				// prevent to open the tooltip
				_canOpenToolTip = false;
			}
		});
	}

	@Override
	protected void beforeHideToolTip() {

	}

	@Override
	protected boolean canCloseToolTip() {

		/*
		 * Do not hide this dialog when the color selector dialog or other dialogs are opened
		 * because it will lock the UI completely !!!
		 */

		return true;
	}

	@Override
	protected boolean canShowToolTip() {
		return true;
	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		initUI(parent);

		final Composite container = createUI(parent);

		Map3Manager.setMap3LayerDialog(this);

		// restore layers
		_layerViewer.setInput(new Object());

		restoreState();

		return container;
	}

	private Composite createUI(final Composite parent) {

		_shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults()//
				.margins(SHELL_MARGIN, SHELL_MARGIN)
				.spacing(0, 0)
				.applyTo(_shellContainer);
		{
			createUI_10_LayerViewer(_shellContainer);
		}

		_shellContainer.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});

		return _shellContainer;
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

			tree = new Tree(layoutContainer, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER
//					| SWT.MULTI
					| SWT.FULL_SELECTION
					| SWT.CHECK);

			tree.setHeaderVisible(false);
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

			_layerViewer.setContentProvider(new PropertiesContentProvider());
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

		_propToolTip = new DialogPropertyViewerToolTip(_layerViewer);

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
		tvc = new TreeViewerColumn(_layerViewer, SWT.LEAD);
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

	public Shell getShell() {

		if (_shellContainer == null) {
			return null;
		}

		return _shellContainer.getShell();
	}

	@Override
	public Point getToolTipLocation(final Point tipSize) {

//		final int tipWidth = tipSize.x;
//
//		final int itemWidth = _toolTipItemBounds.width;
		final int itemHeight = _toolTipItemBounds.height;

		// center horizontally
		final int devX = _toolTipItemBounds.x;// + itemWidth / 2 - tipWidth / 2;
		final int devY = _toolTipItemBounds.y + itemHeight + 0;

		return new Point(devX, devY);
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);

	}

	@Override
	protected Rectangle noHideOnMouseMove() {

		return _toolTipItemBounds;
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
				}
			}

			// redraw map
			if (isMapModified) {
				Map3Manager.redrawMap();
			}

		}
	}

	public void onDispose() {

		saveState();

		Map3Manager.setMap3LayerDialog(null);
	}

	private void onExpandTree(final TVIMap3Item element) {

		// ensure check state is set
		if (element instanceof TVIMap3Category) {
			((TVIMap3Category) element).setCheckState();
		}
	}

	@Override
	protected void onMouseMoveInToolTip(final MouseEvent mouseEvent) {

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

	/**
	 * @param toolTipItemBounds
	 * @param isOpenDelayed
	 */
	public void open(final Rectangle toolTipItemBounds, final boolean isOpenDelayed) {

		if (isToolTipVisible()) {

//			System.out.println((net.tourbook.common.UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//					+ ("\tisToolTipVisible: true"));
//			// TODO remove SYSTEM.OUT.PRINTLN

			return;
		}

//		System.out.println((net.tourbook.common.UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//				+ ("\tisToolTipVisible: false"));
//		// TODO remove SYSTEM.OUT.PRINTLN

		if (isOpenDelayed == false) {

			if (toolTipItemBounds != null) {

				_toolTipItemBounds = toolTipItemBounds;

				showToolTip();
			}

		} else {

			if (toolTipItemBounds == null) {

				// item is not hovered any more

				_canOpenToolTip = false;

				return;
			}

			_toolTipItemBounds = toolTipItemBounds;
			_canOpenToolTip = true;

			if (_isWaitTimerStarted == false) {

				_isWaitTimerStarted = true;

				Display.getCurrent().timerExec(50, _waitTimer);
			}
		}
	}

	private void open_Runnable() {

		_isWaitTimerStarted = false;

		if (_canOpenToolTip) {
			showToolTip();
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
				final TVIMap3Layer tviLayer = (TVIMap3Layer) object;
				tviLayer.fireCheckStateListener();
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
