/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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

import net.tourbook.common.UI;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.AnimatedToolTipShell;
import net.tourbook.map3.Messages;
import net.tourbook.map3.view.TVIMap3Layer;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerRow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;

public class SlideoutMap3LayerTooltip extends AnimatedToolTipShell {

	private static final int			SHELL_MARGIN			= 5;
	private static final double			HOVERED_SENSITIVE_AREA	= 0.66;

	private Composite					_shellContainer;

	private ContainerCheckedTreeViewer	_layerViewer;
	private Tree						_tree;

	private ViewerRow					_sensitiveRowArea;
	private ViewerRow					_viewerRow;

	private IToolProvider				_defaultToolProvider	= new ToolProvider();

	private TVIMap3Layer				_mapLayer;

	private int							_column0_Width;

	private int							_hoverLeftBorder;

	/*
	 * UI resources
	 */
	private Color						_bgColor;
	private Color						_fgColor;

	private Cursor						_cursorHand;

	private final class ToolProvider extends ToolProviderAdapter {
		@Override
		public void createToolUI(final Composite parent) {
			createToolTipUI(parent);
		}
	}

	public SlideoutMap3LayerTooltip(final ContainerCheckedTreeViewer layerViewer) {

		super(layerViewer.getTree());

		_layerViewer = layerViewer;
		_tree = _layerViewer.getTree();

		final Device display = _tree.getDisplay();

		_bgColor = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
		_fgColor = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);

		_cursorHand = new Cursor(display, SWT.CURSOR_HAND);

	}

	@Override
	protected boolean canShowToolTip() {
		return true;
	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		return createUI(parent);
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

	private Composite createUI(final Composite parent) {

		_shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults()//
				.margins(0, 0)
				.spacing(0, 0)
				.applyTo(_shellContainer);
		{

		}

		return _shellContainer;
	}

	private void createUI_20_Info(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{
			/*
			 * layer name
			 */
			Label label = new Label(container, SWT.NONE);
			label.setText(_mapLayer.name);
			MTFont.setBannerFont(label);

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
	public ViewerRow getHoveredRow() {

		return _sensitiveRowArea;
	}

	@Override
	protected Point getToolTipLocation(final Point tipSize) {

//	protected Point getToolTipLocation(	final Point tipSize,
//										final Point mouseOwnerPosition,
//										final IToolProvider toolProvider) {

		if (_viewerRow == null) {
			return null;
		}

		// try to position the tooltip at the bottom of the cell

		final Rectangle displayBounds = _tree.getDisplay().getBounds();

		final Rectangle rowBounds = _viewerRow.getBounds();
		final int rowWidth = rowBounds.width;
		final int rowHeight = rowBounds.height;

		final int tipWidth = tipSize.x;
		final int tipHeight = tipSize.y;

		final int devX = -tipWidth;
		final int devY = rowBounds.y;// + cellHeight;

		final Point ttDisplayLocation = _tree.toDisplay(devX, devY);

//		if (toolProvider != _defaultToolProvider) {
//
//			// locate tooltip closer that it can be accessed with the mouse
//
//			ttDisplayLocation.x = ttDisplayLocation.x + _hoverLeftBorder + 5;
//		}
//
////		if (ttDisplayLocation.x < 0) {
////
////			if (toolProvider == _defaultToolProvider) {
////
////				ttDisplayLocation.x = ttDisplayLocation.x - tipWidth - _column0_Width;
////
////			} else {
////
////			}
////		}

		if (ttDisplayLocation.y + tipHeight > displayBounds.height) {

			/*
			 * adjust vertical position, it is outside of the display, prevent default repositioning
			 */

			ttDisplayLocation.y = ttDisplayLocation.y - tipHeight - rowHeight;
		}

		return ttDisplayLocation;
	}

	@Override
	protected boolean isInNoHideArea(final Point ownerHoverPosition) {

		// set default values
		_mapLayer = null;
		_viewerRow = null;
		_sensitiveRowArea = null;

		Object ttArea = null;

		final ViewerCell viewerCell = _layerViewer.getCell(ownerHoverPosition);

		if (viewerCell != null) {

			/*
			 * Use the first column as content area, when mouse is hovering other cells in the same
			 * row, the tooltip keeps open
			 */
			_viewerRow = viewerCell.getViewerRow();

			_column0_Width = _tree.getColumn(0).getWidth();
			_hoverLeftBorder = (int) (_column0_Width * HOVERED_SENSITIVE_AREA);
			final int ownerHoveredPositionX = ownerHoverPosition.x;

			if (ownerHoveredPositionX > _hoverLeftBorder && ownerHoveredPositionX < _column0_Width) {

				/*
				 * the info tooltip is opened when the mouse is in the last 1/3 part of the column 0
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
		_layerViewer.getTree().setCursor(_sensitiveRowArea == null ? null : _cursorHand);

		return ttArea != null;
	}

	@Override
	protected void onDispose() {

		_cursorHand = UI.disposeResource(_cursorHand);
	}

	public void setLayerVisibility(final TVIMap3Layer mapLayer, final boolean isUpdateUI) {

		if (mapLayer == null) {
			return;
		}

		final IToolProvider toolProvider = mapLayer.toolProvider;
		if (toolProvider != null) {

			// update UI when a tool provider is set in the layer

			toggleToolVisibility(toolProvider, mapLayer.isLayerVisible, isUpdateUI);
		}
	}
	/**
	 * Show/hide tooltip.
	 * 
	 * @param toolProvider
	 * @param isVisible
	 * @param isUpdateUI
	 */
	protected void toggleToolVisibility(final IToolProvider toolProvider,
										final boolean isVisible,
										final boolean isUpdateUI) {

//		final Object ttArea = toolProvider.getToolTipArea();
//
//		final ToolTip3Tool existingTool = _allTools.get(ttArea);
//
//		if (existingTool != null) {
//
//			if (toolProvider.isFlexTool()) {
//
//				// update tool visibility state
//				existingTool.setToolVisibility(isVisible);
//
//			} else {
//
//				// remove default tool that the flex tool is created
//
//				_allTools.remove(ttArea);
//			}
//		}
//
//		/*
//		 * force to create toggled tooltip
//		 */
//		closeTool(existingTool);
//
//		// UI is only updated when mouse is hovering the tool, otherwise tool is not set or displayed at the wrong location
//		if (isUpdateUI) {
//			onOwnerHovered(ttArea);
//		}
	}

}
