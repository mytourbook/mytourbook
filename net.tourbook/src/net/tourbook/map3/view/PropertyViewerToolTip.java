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

import gov.nasa.worldwind.layers.Layer;
import net.tourbook.common.UI;
import net.tourbook.common.tooltip.IToolProvider;
import net.tourbook.common.tooltip.ToolProviderAdapter;
import net.tourbook.common.tooltip.ToolTip3;
import net.tourbook.map3.Messages;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerRow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;

public class PropertyViewerToolTip extends ToolTip3 {

	/**
	 * Relative horizontal start of the sensitive area in a hovered row.
	 */
	private static final double			HOVERED_SENSITIVE_AREA	= 0.66;

	private ContainerCheckedTreeViewer	_propViewer;

	private Tree						_tree;
	private ViewerRow					_viewerRow;

	private ViewerRow					_sensitiveRowArea;
	private IToolProvider				_defaultToolProvider	= new ToolProvider();

	private TVIMap3Layer				_mapLayer;
	private int							_hoverLeftBorder;

	private int							_columnWidth;

	/*
	 * UI resources
	 */
	private Color						_bgColor;
	private Color						_fgColor;
	private Font						_boldFont;

	private final class ToolProvider extends ToolProviderAdapter {
		@Override
		public void createToolUI(final Composite parent) {
			createToolTipUI(parent);
		}
	}

	public PropertyViewerToolTip(final ContainerCheckedTreeViewer propViewer) {

		super(propViewer.getTree());

		_propViewer = propViewer;

		_tree = propViewer.getTree();
		_tree.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});

		final Device display = _tree.getDisplay();

		_bgColor = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
		_fgColor = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);
		_boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);
	}

	private void createToolTipUI(final Composite parent) {

		/*
		 * shell container is necessary because the margins of the inner container will hide the
		 * tooltip when the mouse is hovered, which is not as it should be.
		 */
		final Composite shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(shellContainer);
//		shellContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		{
			final Composite container = new Composite(shellContainer, SWT.NONE);
			GridLayoutFactory.fillDefaults().margins(SHELL_MARGIN, SHELL_MARGIN).applyTo(container);
			//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
			{
				createUI_20_Info(container);
				UI.setColorForAllChildren(container, _fgColor, _bgColor);
			}
		}
	}

	private void createUI_20_Info(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{
			/*
			 * layer name
			 */
			Label label = new Label(container, SWT.NONE);
			label.setFont(_boldFont);
			label.setText(_mapLayer.name);

			final Layer wwLayer = _mapLayer.wwLayer;

			final double minActiveAltitude = wwLayer.getMinActiveAltitude();
			final double maxActiveAltitude = wwLayer.getMaxActiveAltitude();

			/*
			 * min/max altitude
			 */
			label = new Label(container, SWT.NONE);

			label.setText(UI.FormatDoubleMinMaxElevationMeter(minActiveAltitude)
					+ UI.ELLIPSIS_WITH_SPACE
					+ UI.FormatDoubleMinMaxElevationMeter(maxActiveAltitude));
			label.setToolTipText(Messages.Prop_Viewer_Label_AltitudeRange_Tooltip);
		}
	}

	/**
	 * @return Returns a row which is hovered with the mouse or <code>null</code> when no row is
	 *         hovered or when mouse is not hoverering the sensitive area.
	 *         <p>
	 *         Sensitive area is 1/3 of the right side of the row which is set in
	 *         {@link #HOVERED_SENSITIVE_AREA} = {@value #HOVERED_SENSITIVE_AREA}
	 */
	ViewerRow getHoveredRow() {

		return _sensitiveRowArea;
	}

	@Override
	protected IToolProvider getToolProvider(final Object toolTipArea) {

		if (_mapLayer == null) {
			return _defaultToolProvider;
		}

		final boolean isToolProvider = _mapLayer.toolProvider != null;

		if (isToolProvider) {

			/**
			 * !!! HACK !!!
			 * <p>
			 * Ensure the toolprovider contains the tooltip area. When a layer is hovered the first
			 * time and the tool is set to be visible, then the flex tool should be displayed and
			 * NOT the default tooltip.
			 */
			_mapLayer.toolProvider.setToolTipArea(toolTipArea);
		}

		final boolean isLayerVisible = isToolProvider && _mapLayer.isLayerVisible;

		return isLayerVisible ? _mapLayer.toolProvider : _defaultToolProvider;
	}

	@Override
	protected Object getToolTipArea(final Point ownerHoverPosition) {

		// set default values
		_mapLayer = null;
		_viewerRow = null;
		_sensitiveRowArea = null;

		Object ttArea = null;

		final ViewerCell viewerCell = _propViewer.getCell(ownerHoverPosition);

		if (viewerCell != null) {

			/*
			 * use the whole row as content area that when mouse is hovering other cells in the same
			 * row, the tooltip keeps open
			 */
			_viewerRow = viewerCell.getViewerRow();

			_columnWidth = _tree.getColumn(0).getWidth();

			_hoverLeftBorder = (int) (_columnWidth * HOVERED_SENSITIVE_AREA);

			if (ownerHoverPosition.x > _hoverLeftBorder) {

				/*
				 * the info tooltip is opened when the mouse is in the last 1/3 part of the row
				 */

				_sensitiveRowArea = _viewerRow;

				final Object element = _viewerRow.getElement();

				if (element instanceof TVIMap3Layer) {

					ttArea = _viewerRow;

					final TVIMap3Layer mapLayer = (TVIMap3Layer) element;
					_mapLayer = mapLayer;
				}
			}
		}

		/*
		 * show user that sensitive row area is hovered and actions can be done
		 */
		_propViewer.getTree().setCursor(_sensitiveRowArea == null ? null : getCursorHand());

		return ttArea;
	}

	@Override
	public Point getToolTipLocation(final Point tipSize, final Point mouseOwnerPosition) {

		if (_viewerRow == null) {
			return null;
		}

		// try to position the tooltip at the bottom of the cell

		final Rectangle displayBounds = _tree.getDisplay().getBounds();

		final Rectangle rowBounds = _viewerRow.getBounds();
		final int rowWidth = rowBounds.width;
		final int rowHeight = rowBounds.height;

		final int devX = _columnWidth;
		final int devY = rowBounds.y;// + cellHeight;

		final Point ttDisplayLocation = _tree.toDisplay(devX, devY);
		final int tipSizeWidth = tipSize.x;
		final int tipSizeHeight = tipSize.y;

		if (ttDisplayLocation.x + tipSizeWidth > displayBounds.width) {

			ttDisplayLocation.x = ttDisplayLocation.x - tipSizeWidth - rowWidth;
		}

		if (ttDisplayLocation.y + tipSizeHeight > displayBounds.height) {

			/*
			 * adjust vertical position, it is outside of the display, prevent default repositioning
			 */

			ttDisplayLocation.y = ttDisplayLocation.y - tipSizeHeight - rowHeight;
		}

		return ttDisplayLocation;
	}

	private void onDispose() {

	}

	void setLayerVisibility(final TVIMap3Layer mapLayer, final boolean isUpdateUI) {

		if (mapLayer == null) {
			return;
		}

		final IToolProvider toolProvider = mapLayer.toolProvider;

		if (toolProvider != null) {

			// update UI when a tool provider is set in the layer

			toggleToolVisibility(toolProvider, mapLayer.isLayerVisible, isUpdateUI);
		}
	}

}
